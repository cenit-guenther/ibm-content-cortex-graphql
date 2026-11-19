// Usage: node src/examples/07-download.js <document-id-or-path> <output-file>
import { writeFile } from 'node:fs/promises';
import { config, download, gql, runIfMain } from '../client.js';

/** Reads the download URL via GraphQL and then fetches the bytes with the same credentials. */
export async function downloadContent(identifier) {
  const data = await gql('07-download/document-content.graphql', { repositoryIdentifier: config().objectStore, identifier });
  const url = data.document.contentElements?.[0]?.downloadUrl;
  if (!url) throw new Error('The document has no content element');
  return download(url);
}

runIfMain(import.meta.url, async ([identifier, output]) => {
  if (!identifier || !output) throw new Error('usage: 07-download.js <document> <output-file>');
  const bytes = await downloadContent(identifier);
  await writeFile(output, bytes);
  return { saved: output, bytes: bytes.length };
});
