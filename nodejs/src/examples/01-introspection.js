// Usage: node src/examples/01-introspection.js [TypeName]
import { gql, runIfMain } from '../client.js';

export const schemaOverview = () => gql('01-introspection/schema-overview.graphql');
export const typeDetails = (name) => gql('01-introspection/type-details.graphql', { name });

runIfMain(import.meta.url, ([typeName]) => (typeName ? typeDetails(typeName) : schemaOverview()));
