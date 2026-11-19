// Integration test against a real FileNet server. Skipped when the environment is not configured.
// Creates a uniquely named folder under FILENET_TEST_FOLDER and removes everything it created.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { config, isConfigured } from '../src/client.js';
import { schemaOverview } from '../src/examples/01-introspection.js';
import { repository } from '../src/examples/02-repository.js';
import { document, folder } from '../src/examples/03-fetch.js';
import { searchDocuments } from '../src/examples/04-search.js';
import { createFolder, deleteFolder } from '../src/examples/05-folders.js';
import { createDocument, deleteDocument } from '../src/examples/06-upload.js';
import { downloadContent } from '../src/examples/07-download.js';
import { updateDocument } from '../src/examples/08-properties.js';
import { checkin, checkout, versions } from '../src/examples/09-versioning.js';
import { permissions } from '../src/examples/10-security.js';

const skip = isConfigured() ? false : 'FILENET_* environment variables are not set';

test('FileNet GraphQL lifecycle', { skip }, async (t) => {
  const suffix = `${Date.now()}-${process.pid}`;
  const text = `hello FileNet GraphQL ${suffix}`;
  let folderId;
  let docId;

  t.after(async () => {
    if (docId) {
      try {
        const data = await versions(docId);
        for (const v of data.document.versionSeries.versions.versions) {
          await deleteDocument(v.id).catch(() => {});
        }
      } catch { /* best effort cleanup */ }
    }
    if (folderId) await deleteFolder(folderId).catch(() => {});
  });

  await t.test('introspection', async () => {
    const data = await schemaOverview();
    assert.ok(data.__schema.queryType.name);
  });

  await t.test('repository', async () => {
    const data = await repository(config().objectStore);
    assert.ok(data.repository.id);
  });

  await t.test('create folder', async () => {
    const data = await createFolder(`gql-example-${suffix}`, config().testFolder);
    folderId = data.createFolder.id;
    assert.ok(folderId);
  });

  await t.test('upload document', async () => {
    const data = await createDocument(`hello-${suffix}.txt`, folderId, Buffer.from(text));
    docId = data.createDocument.id;
    assert.ok(docId);
  });

  await t.test('fetch document and folder', async () => {
    assert.equal((await document(docId)).document.name, `hello-${suffix}.txt`);
    const names = (await folder(folderId)).folder.containedDocuments.documents.map((d) => d.name);
    assert.ok(names.includes(`hello-${suffix}.txt`));
  });

  await t.test('search', async () => {
    const data = await searchDocuments(`This INFOLDER '${folderId}'`);
    assert.ok(data.documents.documents.some((d) => d.id === docId));
  });

  await t.test('download', async () => {
    assert.equal((await downloadContent(docId)).toString(), text);
  });

  await t.test('update properties', async () => {
    const data = await updateDocument(docId, `renamed-${suffix}.txt`, `Title ${suffix}`);
    assert.equal(data.updateDocument.name, `renamed-${suffix}.txt`);
  });

  await t.test('permissions', async () => {
    const data = await permissions(docId);
    assert.ok(Array.isArray(data.document.permissions));
  });

  await t.test('checkout and checkin', async () => {
    const out = await checkout(docId);
    const reservationId = out.checkoutDocument.id;
    const inn = await checkin(reservationId);
    assert.ok(inn.checkinDocument.id);
  });
});
