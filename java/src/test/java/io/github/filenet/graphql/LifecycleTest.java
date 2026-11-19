package io.github.filenet.graphql;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.filenet.graphql.examples.Examples;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIf;

/**
 * Integration test against a real FileNet server; skipped when the FILENET_* variables are not set.
 * Creates a uniquely named folder under FILENET_TEST_FOLDER and removes everything it created.
 */
@EnabledIf("io.github.filenet.graphql.FileNetClient#isConfigured")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LifecycleTest {

    private static Examples ex;
    private static FileNetClient client;
    private static final String SUFFIX = System.currentTimeMillis() + "-" + ProcessHandle.current().pid();
    private static final String TEXT = "hello FileNet GraphQL " + SUFFIX;
    private static String folderId;
    private static String docId;

    @BeforeAll
    static void setUp() throws Exception {
        client = FileNetClient.fromEnvironment();
        ex = new Examples(client);
        folderId = ex.createFolder("gql-example-" + SUFFIX, client.testFolder()).path("createFolder").path("id").asText();
        assertFalse(folderId.isEmpty());
        docId = ex.createDocument("hello-" + SUFFIX + ".txt", folderId, TEXT.getBytes(StandardCharsets.UTF_8), "text/plain")
                .path("createDocument").path("id").asText();
        assertFalse(docId.isEmpty());
    }

    @AfterAll
    static void tearDown() {
        if (docId != null && !docId.isEmpty()) {
            try {
                for (JsonNode v : ex.versions(docId).at("/document/versionSeries/versions/versionables")) {
                    try { ex.deleteDocument(v.path("id").asText()); } catch (Exception ignored) { }
                }
            } catch (Exception ignored) { }
        }
        if (folderId != null && !folderId.isEmpty()) {
            try { ex.deleteFolder(folderId); } catch (Exception ignored) { }
        }
    }

    @Test
    @Order(1)
    void introspectionAndRepository() throws Exception {
        assertFalse(ex.schemaOverview().at("/__schema/queryType/name").asText().isEmpty());
        assertFalse(ex.repository().at("/objectStore/id").asText().isEmpty());
    }

    @Test
    @Order(2)
    void fetchDocumentAndFolder() throws Exception {
        assertEquals("hello-" + SUFFIX + ".txt", ex.document(docId).at("/document/name").asText());
        boolean listed = false;
        for (JsonNode d : ex.folder(folderId).at("/folder/containedDocuments/documents")) {
            listed |= d.path("id").asText().equals(docId);
        }
        assertTrue(listed);
    }

    @Test
    @Order(3)
    void search() throws Exception {
        boolean found = false;
        for (JsonNode d : ex.searchDocuments("This INFOLDER '" + folderId + "'", "Document").at("/documents/documents")) {
            found |= d.path("id").asText().equals(docId);
        }
        assertTrue(found);
    }

    @Test
    @Order(4)
    void download() throws Exception {
        assertArrayEquals(TEXT.getBytes(StandardCharsets.UTF_8), ex.downloadContent(docId));
    }

    @Test
    @Order(5)
    void permissions() throws Exception {
        assertTrue(ex.permissions(docId).at("/document/permissions").isArray());
    }

    // The following tests modify the document, so they run after the read-only tests.
    @Test
    @Order(100)
    void updateProperties() throws Exception {
        String newName = "renamed-" + SUFFIX + ".txt";
        assertEquals(newName, ex.updateDocument(docId, newName, "Title " + SUFFIX).at("/updateDocument/name").asText());
    }

    @Test
    @Order(101)
    void checkoutAndCheckin() throws Exception {
        String reservationId = ex.checkout(docId).at("/checkoutDocument/reservation/id").asText();
        assertFalse(reservationId.isEmpty());
        assertFalse(ex.checkin(reservationId).at("/checkinDocument/id").asText().isEmpty());
    }
}
