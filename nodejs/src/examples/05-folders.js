// Usage: node src/examples/05-folders.js create <name> [parent=/] | rename <id> <new-name> | delete <id>
import { config, gql, runIfMain } from '../client.js';

const repositoryIdentifier = () => config().objectStore;

export const createFolder = (name, parent = '/') =>
  gql('05-folders/create-folder.graphql', { repositoryIdentifier: repositoryIdentifier(), name, parent });
export const renameFolder = (identifier, name) =>
  gql('05-folders/update-folder.graphql', { repositoryIdentifier: repositoryIdentifier(), identifier, name });
export const deleteFolder = (identifier) =>
  gql('05-folders/delete-folder.graphql', { repositoryIdentifier: repositoryIdentifier(), identifier });

runIfMain(import.meta.url, async ([action, a, b]) => {
  switch (action) {
    case 'create': return createFolder(a, b);
    case 'rename': return renameFolder(a, b);
    case 'delete': return deleteFolder(a);
    default: throw new Error('usage: 05-folders.js create|rename|delete ...');
  }
});
