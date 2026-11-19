package io.github.filenet.graphql.examples;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.filenet.graphql.FileNetClient;
import java.io.IOException;
import java.util.Map;

/** One method per example topic. Each method returns the {@code data} node of the GraphQL response. */
public final class Examples {

    private final FileNetClient client;

    public Examples(FileNetClient client) {
        this.client = client;
    }

    private Map<String, Object> vars(Object... keyValues) {
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("repositoryIdentifier", client.objectStore());
        for (int i = 0; i < keyValues.length; i += 2) map.put((String) keyValues[i], keyValues[i + 1]);
        return map;
    }

    // 01 - introspection
    public JsonNode schemaOverview() throws IOException, InterruptedException {
        return client.execute("01-introspection/schema-overview.graphql", Map.of());
    }

    public JsonNode typeDetails(String name) throws IOException, InterruptedException {
        return client.execute("01-introspection/type-details.graphql", Map.of("name", name));
    }

    // 02 - repositories
    public JsonNode repositories() throws IOException, InterruptedException {
        return client.execute("02-repository/repositories.graphql", Map.of());
    }

    public JsonNode repository() throws IOException, InterruptedException {
        return client.execute("02-repository/repository.graphql", vars());
    }

    // 03 - fetch by id or path
    public JsonNode document(String identifier) throws IOException, InterruptedException {
        return client.execute("03-fetch/document.graphql", vars("identifier", identifier));
    }

    public JsonNode folder(String identifier) throws IOException, InterruptedException {
        return client.execute("03-fetch/folder.graphql", vars("identifier", identifier));
    }

    // 04 - search
    public JsonNode searchDocuments(String where, String from) throws IOException, InterruptedException {
        return client.execute("04-search/search-documents.graphql", vars("from", from, "where", where, "pageSize", 10));
    }

    // 05 - folders
    public JsonNode createFolder(String name, String parent) throws IOException, InterruptedException {
        return client.execute("05-folders/create-folder.graphql", vars("name", name, "parent", parent));
    }

    public JsonNode renameFolder(String identifier, String name) throws IOException, InterruptedException {
        return client.execute("05-folders/update-folder.graphql", vars("identifier", identifier, "name", name));
    }

    public JsonNode deleteFolder(String identifier) throws IOException, InterruptedException {
        return client.execute("05-folders/delete-folder.graphql", vars("identifier", identifier));
    }

    // 06 - upload
    public JsonNode createDocument(String name, String folder, byte[] content, String contentType)
            throws IOException, InterruptedException {
        return client.executeUpload("06-upload/create-document.graphql",
                vars("name", name, "folder", folder, "contentType", contentType),
                "file", content, contentType, name);
    }

    public JsonNode deleteDocument(String identifier) throws IOException, InterruptedException {
        return client.execute("06-upload/delete-document.graphql", vars("identifier", identifier));
    }

    // 07 - download: read the download URL via GraphQL, then fetch the bytes with the same credentials
    public byte[] downloadContent(String identifier) throws IOException, InterruptedException {
        JsonNode data = client.execute("07-download/document-content.graphql", vars("identifier", identifier));
        JsonNode url = data.path("document").path("contentElements").path(0).path("downloadUrl");
        if (url.isMissingNode() || url.isNull()) throw new IOException("The document has no content element");
        return client.download(url.asText());
    }

    // 08 - properties
    public JsonNode updateDocument(String identifier, String name, String title) throws IOException, InterruptedException {
        return client.execute("08-properties/update-document.graphql",
                vars("identifier", identifier, "name", name, "title", title));
    }

    // 09 - versioning
    public JsonNode checkout(String identifier) throws IOException, InterruptedException {
        return client.execute("09-versioning/checkout-document.graphql", vars("identifier", identifier));
    }

    public JsonNode checkin(String identifier) throws IOException, InterruptedException {
        return client.execute("09-versioning/checkin-document.graphql", vars("identifier", identifier));
    }

    public JsonNode cancelCheckout(String identifier) throws IOException, InterruptedException {
        return client.execute("09-versioning/cancel-checkout.graphql", vars("identifier", identifier));
    }

    public JsonNode versions(String identifier) throws IOException, InterruptedException {
        return client.execute("09-versioning/versions.graphql", vars("identifier", identifier));
    }

    // 10 - security
    public JsonNode permissions(String identifier) throws IOException, InterruptedException {
        return client.execute("10-security/permissions.graphql", vars("identifier", identifier));
    }
}
