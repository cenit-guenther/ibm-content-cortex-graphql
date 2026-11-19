// Usage: node src/examples/06-upload.js <local-file> <folder-id-or-path> [document-name] [mime-type]
import { readFile } from 'node:fs/promises';
import { basename } from 'node:path';
import { config, gqlUpload, gql, runIfMain } from '../client.js';

export const createDocument = (name, folder, content, contentType = 'text/plain') =>
  gqlUpload(
    '06-upload/create-document.graphql',
    { repositoryIdentifier: config().objectStore, name, folder, contentType },
    'file',
    content,
    contentType,
    name,
  );

export const deleteDocument = (identifier) =>
  gql('06-upload/delete-document.graphql', { repositoryIdentifier: config().objectStore, identifier });

runIfMain(import.meta.url, async ([file, folder, name, mime]) => {
  if (!file || !folder) throw new Error('usage: 06-upload.js <local-file> <folder> [name] [mime-type]');
  return createDocument(name || basename(file), folder, await readFile(file), mime || 'text/plain');
});
