
package com.nexora.launcher;

import com.google.gson.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;

public class ModrinthInstaller {

    private static final String MC_VERSION = "26.2";

    public static String install(String projectId)
            throws Exception {

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        String url = "https://api.modrinth.com/v2/project/"
                + URLEncoder.encode(projectId, "UTF-8")
                + "/version?loaders=%5B%22fabric%22%5D"
                + "&game_versions=%5B%22" + MC_VERSION + "%22%5D";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "NexoraLauncher/1.0")
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) {
            throw new Exception(
                    "Modrinth API error: " + response.statusCode()
            );
        }

        JsonArray versions = JsonParser.parseString(
                response.body()
        ).getAsJsonArray();

        if (versions.isEmpty()) {
            throw new Exception(
                    "Geen Fabric-versie gevonden voor " + MC_VERSION
            );
        }

        JsonObject version = versions.get(0).getAsJsonObject();
        JsonArray files = version.getAsJsonArray("files");

        JsonObject chosenFile = null;

        for (JsonElement element : files) {
            JsonObject file = element.getAsJsonObject();

            if (file.get("filename").getAsString()
                    .toLowerCase().endsWith(".jar")) {

                if (chosenFile == null ||
                        file.get("primary").getAsBoolean()) {
                    chosenFile = file;
                }

                if (file.get("primary").getAsBoolean()) {
                    break;
                }
            }
        }

        if (chosenFile == null) {
            throw new Exception("Geen JAR-bestand gevonden.");
        }

        String filename = chosenFile.get("filename").getAsString();
        String downloadUrl = chosenFile.get("url").getAsString();

        Path modsFolder = Paths.get(
                System.getenv("APPDATA"),
                ".minecraft",
                "mods"
        );

        Files.createDirectories(modsFolder);

        Path destination = modsFolder.resolve(filename).normalize();

        if (!destination.getParent().equals(modsFolder)) {
            throw new Exception("Ongeldige bestandsnaam.");
        }

        if (Files.exists(destination)) {
            throw new Exception("Deze mod is al geïnstalleerd.");
        }

        HttpRequest downloadRequest = HttpRequest.newBuilder()
                .uri(URI.create(downloadUrl))
                .header("User-Agent", "NexoraLauncher/1.0")
                .timeout(Duration.ofMinutes(2))
                .GET()
                .build();

        Path tempFile = Files.createTempFile(
                modsFolder, "nexora-", ".tmp"
        );

        try {
            HttpResponse<Path> downloadResponse = client.send(
                    downloadRequest,
                    HttpResponse.BodyHandlers.ofFile(tempFile)
            );

            if (downloadResponse.statusCode() != 200) {
                throw new Exception("Download mislukt.");
            }

            Files.move(tempFile, destination);
        } finally {
            Files.deleteIfExists(tempFile);
        }

        return filename;
    }
}
