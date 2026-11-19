// Usage: node src/examples/09-versioning.js checkout|checkin|cancel|versions <document-id>
import { config, gql, runIfMain } from '../client.js';

const call = (file) => (identifier) => gql(file, { repositoryIdentifier: config().objectStore, identifier });

export const checkout = call('09-versioning/checkout-document.graphql');
export const checkin = call('09-versioning/checkin-document.graphql');
export const cancelCheckout = call('09-versioning/cancel-checkout.graphql');
export const versions = call('09-versioning/versions.graphql');

runIfMain(import.meta.url, async ([action, identifier]) => {
  const actions = { checkout, checkin, cancel: cancelCheckout, versions };
  if (!actions[action] || !identifier) throw new Error('usage: 09-versioning.js checkout|checkin|cancel|versions <id>');
  return actions[action](identifier);
});
