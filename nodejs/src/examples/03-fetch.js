// Usage: node src/examples/03-fetch.js document|folder <path-or-id>
import { config, gql, runIfMain } from '../client.js';

export const document = (identifier) =>
  gql('03-fetch/document.graphql', { repositoryIdentifier: config().objectStore, identifier });
export const folder = (identifier) =>
  gql('03-fetch/folder.graphql', { repositoryIdentifier: config().objectStore, identifier });

runIfMain(import.meta.url, async ([kind, identifier]) => {
  if (!['document', 'folder'].includes(kind) || !identifier) {
    throw new Error('usage: 03-fetch.js document|folder <path-or-id>');
  }
  return kind === 'document' ? document(identifier) : folder(identifier);
});
