package io.github.filenet.graphql.examples;

import io.github.filenet.graphql.FileNetClient;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Command line entry point.
 *
 * <pre>
 * mvn -q compile exec:java -Dexec.args="introspection [TypeName]"
 * mvn -q compile exec:java -Dexec.args="repository"
 * mvn -q compile exec:java -Dexec.args="fetch document|folder &lt;path-or-id&gt;"
 * mvn -q compile exec:java -Dexec.args="search [where] [class]"
 * mvn -q compile exec:java -Dexec.args="folder create|rename|delete ..."
 * mvn -q compile exec:java -Dexec.args="upload &lt;file&gt; &lt;folder&gt; [name] [mime]"
 * mvn -q compile exec:java -Dexec.args="download &lt;document&gt; &lt;output-file&gt;"
 * mvn -q compile exec:java -Dexec.args="properties &lt;document&gt; &lt;new-name&gt; &lt;title&gt;"
 * mvn -q compile exec:java -Dexec.args="versioning checkout|checkin|cancel|versions &lt;document&gt;"
 * mvn -q compile exec:java -Dexec.args="security &lt;document&gt;"
 * </pre>
 */
public final class Main {

    private Main() {}

    public static void main(String[] args) throws Exception {
        if (args.length == 0) usage();
        Examples ex = new Examples(FileNetClient.fromEnvironment());
        Object result = switch (args[0]) {
            case "introspection" -> args.length > 1 ? ex.typeDetails(args[1]) : ex.schemaOverview();
            case "repository" -> ex.repository();
            case "fetch" -> {
                need(args, 3);
                yield args[1].equals("document") ? ex.document(args[2]) : ex.folder(args[2]);
            }
            case "search" -> ex.searchDocuments(args.length > 1 ? args[1] : null, args.length > 2 ? args[2] : "Document");
            case "folder" -> {
                need(args, 3);
                yield switch (args[1]) {
                    case "create" -> ex.createFolder(args[2], args.length > 3 ? args[3] : "/");
                    case "rename" -> ex.renameFolder(args[2], need(args, 4)[3]);
                    case "delete" -> ex.deleteFolder(args[2]);
                    default -> usage();
                };
            }
            case "upload" -> {
                need(args, 3);
                Path file = Path.of(args[1]);
                String name = args.length > 3 ? args[3] : file.getFileName().toString();
                yield ex.createDocument(name, args[2], Files.readAllBytes(file), args.length > 4 ? args[4] : "text/plain");
            }
            case "download" -> {
                need(args, 3);
                byte[] bytes = ex.downloadContent(args[1]);
                Files.write(Path.of(args[2]), bytes);
                yield "Saved " + args[2] + " (" + bytes.length + " bytes)";
            }
            case "properties" -> {
                need(args, 4);
                yield ex.updateDocument(args[1], args[2], args[3]);
            }
            case "versioning" -> {
                need(args, 3);
                yield switch (args[1]) {
                    case "checkout" -> ex.checkout(args[2]);
                    case "checkin" -> ex.checkin(args[2]);
                    case "cancel" -> ex.cancelCheckout(args[2]);
                    case "versions" -> ex.versions(args[2]);
                    default -> usage();
                };
            }
            case "security" -> {
                need(args, 2);
                yield ex.permissions(args[1]);
            }
            default -> usage();
        };
        System.out.println(result);
    }

    private static String[] need(String[] args, int count) {
        if (args.length < count) usage();
        return args;
    }

    private static <T> T usage() {
        System.err.println("Unknown or incomplete command. See the usage list in Main.java and docs/examples/.");
        System.exit(2);
        return null;
    }
}
