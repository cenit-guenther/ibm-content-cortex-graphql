// Minimal FileNet GraphQL client built on the global fetch API (Node.js >= 20.12, no dependencies).
import { readFileSync, existsSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..', '..');

// Load <repo>/.env without overriding variables that are already set.
const envFile = join(ROOT, '.env');
if (existsSync(envFile) && typeof process.loadEnvFile === 'function') {
  const before = { ...process.env };
  process.loadEnvFile(envFile);
  Object.assign(process.env, before);
}

/** Reads the connection settings from the environment and validates them. */
export function config() {
  const env = process.env;
  const missing = ['FILENET_GRAPHQL_URL', 'FILENET_OBJECT_STORE'].filter((n) => !env[n]);
  if (!env.FILENET_TOKEN) {
    missing.push(...['FILENET_USER', 'FILENET_PASSWORD'].filter((n) => !env[n]));
  }
  if (missing.length) {
    throw new Error(`Missing environment variable(s): ${missing.join(', ')}`);
  }
  return {
    url: env.FILENET_GRAPHQL_URL,
    objectStore: env.FILENET_OBJECT_STORE,
    testFolder: env.FILENET_TEST_FOLDER || '/',
  };
}

/** Returns true when the environment contains enough settings to talk to a server. */
export function isConfigured() {
  try {
    config();
    return true;
  } catch {
    return false;
  }
}

/** Builds the Authorization header: Bearer when FILENET_TOKEN is set, otherwise Basic. */
export function authHeaders() {
  const { FILENET_TOKEN, FILENET_USER, FILENET_PASSWORD } = process.env;
  if (FILENET_TOKEN) return { Authorization: `Bearer ${FILENET_TOKEN}` };
  const basic = Buffer.from(`${FILENET_USER}:${FILENET_PASSWORD}`).toString('base64');
  return { Authorization: `Basic ${basic}` };
}

/** Loads a shared query file from <repo>/graphql/ and strips comment lines. */
export function loadQuery(relativePath) {
  return readFileSync(join(ROOT, 'graphql', relativePath), 'utf8')
    .split('\n')
    .filter((line) => !line.trim().startsWith('#'))
    .join('\n');
}

async function parse(response) {
  const text = await response.text();
  if (!response.ok) throw new Error(`HTTP ${response.status}: ${text}`);
  const body = JSON.parse(text);
  if (body.errors?.length) throw new Error(`GraphQL errors: ${JSON.stringify(body.errors)}`);
  return body.data;
}

/** Executes a query or mutation from a shared .graphql file and returns `data`. */
export async function gql(file, variables = {}) {
  const { url } = config();
  const response = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify({ query: loadQuery(file), variables }),
  });
  return parse(response);
}

/**
 * Executes a mutation with one file upload using the GraphQL multipart request specification.
 * `uploadVariable` is the name of the variable of type Upload (set to null in `variables`).
 */
export async function gqlUpload(file, variables, uploadVariable, content, contentType, fileName) {
  const { url } = config();
  const form = new FormData();
  form.append('operations', JSON.stringify({ query: loadQuery(file), variables: { ...variables, [uploadVariable]: null } }));
  form.append('map', JSON.stringify({ 0: [`variables.${uploadVariable}`] }));
  form.append('0', new Blob([content], { type: contentType }), fileName);
  // fetch sets the multipart Content-Type header including the boundary.
  const response = await fetch(url, { method: 'POST', headers: authHeaders(), body: form });
  return parse(response);
}

/** Downloads the bytes behind a URL (for example a content element downloadUrl). */
export async function download(downloadUrl) {
  const response = await fetch(downloadUrl, { headers: authHeaders() });
  if (!response.ok) throw new Error(`HTTP ${response.status} while downloading ${downloadUrl}`);
  return Buffer.from(await response.arrayBuffer());
}

/** Runs `main` and prints its JSON result when the module is executed directly. */
export function runIfMain(importMetaUrl, main) {
  if (process.argv[1] && importMetaUrl === pathToFileURL(process.argv[1]).href) {
    main(process.argv.slice(2))
      .then((result) => result !== undefined && console.log(JSON.stringify(result, null, 2)))
      .catch((error) => {
        console.error(error.message);
        process.exit(1);
      });
  }
}
