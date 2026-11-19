// Usage: node src/examples/02-repository.js
import { config, gql, runIfMain } from '../client.js';

export const repositories = () => gql('02-repository/repositories.graphql');
export const repository = (repositoryIdentifier) => gql('02-repository/repository.graphql', { repositoryIdentifier });

runIfMain(import.meta.url, async () => ({
  all: await repositories(),
  configured: await repository(config().objectStore),
}));
