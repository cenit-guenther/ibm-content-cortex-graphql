// Usage: node src/examples/04-search.js [where-clause] [class]
// Example: node src/examples/04-search.js "DocumentTitle LIKE 'Invoice%'"
import { config, gql, runIfMain } from '../client.js';

export const searchDocuments = (where = null, from = 'Document', pageSize = 10) =>
  gql('04-search/search-documents.graphql', { repositoryIdentifier: config().objectStore, from, where, pageSize });

runIfMain(import.meta.url, ([where, from]) => searchDocuments(where || null, from || 'Document'));
