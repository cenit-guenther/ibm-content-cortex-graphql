// Usage: node src/examples/10-security.js <document-id-or-path>
import { config, gql, runIfMain } from '../client.js';

export const permissions = (identifier) =>
  gql('10-security/permissions.graphql', { repositoryIdentifier: config().objectStore, identifier });

runIfMain(import.meta.url, async ([identifier]) => {
  if (!identifier) throw new Error('usage: 10-security.js <document-id-or-path>');
  return permissions(identifier);
});
