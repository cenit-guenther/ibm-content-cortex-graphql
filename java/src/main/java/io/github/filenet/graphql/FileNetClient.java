package io.github.filenet.graphql;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Minimal FileNet GraphQL client based on java.net.http (Java 17). The only dependency is Jackson.
 *
 * <p>Configuration is read from environment variables (see .env.example); a {@code .env} file in
 * the repository root is used as a fallback. Custom CA certificates are configured through the
 * standard JVM truststore options ({@code -Djavax.net.ssl.trustStore=...}).
 */
public final class FileNetClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String url;
    private final String authorization;
    private final String objectStore;
    private final String testFolder;
    private final Path graphqlDir;
    private final HttpClient http = HttpClient.newHttpClient();

    public FileNetClient(Map<String, String> env, Path graphqlDir) {
        List<String> missing = new java.util.ArrayList<>();
        for (String name : List.of("FILENET_GRAPHQL_URL", "FILENET_OBJECT_STORE")) {
            if (isBlank(env.get(name))) missing.add(name);
        }
        if (isBlank(env.get("FILENET_TOKEN"))) {
            for (String name : List.of("FILENET_USER", "FILENET_PASSWORD")) {
                if (isBlank(env.get(name))) missing.add(name);
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Missing environment variable(s): " + String.join(", ", missing));
        }
        this.url = env.get("FILENET_GRAPHQL_URL");
        this.objectStore = env.get("FILENET_OBJECT_STORE");
        this.testFolder = isBlank(env.get("FILENET_TEST_FOLDER")) ? "/" : env.get("FILENET_TEST_FOLDER");
        this.graphqlDir = graphqlDir;
        if (!isBlank(env.get("FILENET_TOKEN"))) {
            this.authorization = "Bearer " + env.get("FILENET_TOKEN");
        } else {
            String credentials = env.get("FILENET_USER") + ":" + env.get("FILENET_PASSWORD");
            this.authorization = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        }
    }

    /** Creates a client from the process environment, with {@code .env} as a fallback. */
    public static FileNetClient fromEnvironment() {
        Path root = findRepositoryRoot();
        Map<String, String> env = new java.util.HashMap<>(readDotEnv(root.resolve(".env")));
        env.putAll(System.getenv());
        return new FileNetClient(env, root.resolve("graphql"));
    }

    /** Returns true when enough environment variables are set to contact a server. */
    public static boolean isConfigured() {
        try {
            fromEnvironment();
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    public String objectStore() { return objectStore; }

    public String testFolder() { return testFolder; }

    /** Executes a query or mutation from a shared .graphql file and returns the {@code data} node. */
    public JsonNode execute(String file, Map<String, Object> variables) throws IOException, InterruptedException {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("query", loadQuery(file));
        body.set("variables", MAPPER.valueToTree(variables));
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", authorization)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)))
                .build();
        return parse(http.send(request, HttpResponse.BodyHandlers.ofString()));
    }

    /**
     * Executes a mutation with a single file upload. FileNet expects a multipart request in which
     * the variable {@code uploadVariable} holds the NAME of a file part ("file1") and that part
     * carries the bytes.
     */
    public JsonNode executeUpload(String file, Map<String, Object> variables, String uploadVariable,
                                  byte[] content, String contentType, String fileName)
            throws IOException, InterruptedException {
        Map<String, Object> vars = new java.util.HashMap<>(variables);
        vars.put(uploadVariable, "file1");
        ObjectNode operations = MAPPER.createObjectNode();
        operations.put("query", loadQuery(file));
        operations.set("variables", MAPPER.valueToTree(vars));

        String boundary = "----filenet" + UUID.randomUUID();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeTextPart(out, boundary, "operations", MAPPER.writeValueAsString(operations));
        out.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file1\"; filename=\"" + fileName
                + "\"\r\nContent-Type: " + contentType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(content);
        out.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", authorization)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(out.toByteArray()))
                .build();
        return parse(http.send(request, HttpResponse.BodyHandlers.ofString()));
    }

    /** Downloads the bytes behind a URL (for example a content element downloadUrl). */
    public byte[] download(String downloadUrl) throws IOException, InterruptedException {
        // FileNet returns a path relative to the context path (e.g. "/content?..."): resolve it
        // against the GraphQL URL without its last segment.
        if (downloadUrl.startsWith("/")) {
            downloadUrl = url.substring(0, url.lastIndexOf('/')) + downloadUrl;
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(downloadUrl))
                .header("Authorization", authorization)
                .GET()
                .build();
        HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("HTTP " + response.statusCode() + " while downloading " + downloadUrl);
        }
        return response.body();
    }

    private static void writeTextPart(ByteArrayOutputStream out, String boundary, String name, String value)
            throws IOException {
        out.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + name + "\"\r\n\r\n" + value
                + "\r\n").getBytes(StandardCharsets.UTF_8));
    }

    private static JsonNode parse(HttpResponse<String> response) throws IOException {
        if (response.statusCode() / 100 != 2) {
            throw new IOException("HTTP " + response.statusCode() + ": " + response.body());
        }
        JsonNode root = MAPPER.readTree(response.body());
        if (root.path("errors").isArray() && !root.path("errors").isEmpty()) {
            throw new IOException("GraphQL errors: " + root.get("errors"));
        }
        return root.get("data");
    }

    /** Loads a shared query file and strips comment lines. */
    String loadQuery(String file) throws IOException {
        return Files.readAllLines(graphqlDir.resolve(file)).stream()
                .filter(line -> !line.trim().startsWith("#"))
                .collect(Collectors.joining("\n"));
    }

    private static Path findRepositoryRoot() {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null) {
            if (Files.isDirectory(dir.resolve("graphql")) && Files.isDirectory(dir.resolve("java"))) return dir;
            dir = dir.getParent();
        }
        throw new IllegalStateException("Cannot find the repository root (directory containing graphql/ and java/)");
    }

    private static Map<String, String> readDotEnv(Path file) {
        Map<String, String> values = new java.util.HashMap<>();
        if (!Files.isRegularFile(file)) return values;
        try {
            for (String line : Files.readAllLines(file)) {
                String trimmed = line.trim();
                int eq = trimmed.indexOf('=');
                if (trimmed.isEmpty() || trimmed.startsWith("#") || eq < 1) continue;
                String value = trimmed.substring(eq + 1).trim();
                if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                values.put(trimmed.substring(0, eq).trim(), value);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + file, e);
        }
        return values;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
