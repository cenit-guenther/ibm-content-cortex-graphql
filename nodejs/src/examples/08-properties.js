// Usage: node src/examples/08-properties.js <document-id-or-path> <new-name> <title>
import { config, gql, runIfMain } from '../client.js';

export const updateDocument = (identifier, name, title) =>
  gql('08-properties/update-document.graphql', { repositoryIdentifier: config().objectStore, identifier, name, title });

runIfMain(import.meta.url, async ([identifier, name, title]) => {
  if (!identifier || !name || !title) throw new Error('usage: 08-properties.js <document> <new-name> <title>');
  return updateDocument(identifier, name, title);
});
