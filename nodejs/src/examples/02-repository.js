// Usage: node src/examples/02-repository.js
import { config, gql, runIfMain } from '../client.js';

export const repositoryObjects = (repositoryIdentifier) =>
  gql('02-repository/repository-objects.graphql', { repositoryIdentifier });
export const repository = (repositoryIdentifier) => gql('02-repository/repository.graphql', { repositoryIdentifier });

runIfMain(import.meta.url, async () => ({
  objectStore: await repository(config().objectStore),
  folders: await repositoryObjects(config().objectStore),
}));
