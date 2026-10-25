
package com.nexora.launcher;

import com.google.gson.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class ModrinthSearch {

    public record ModResult(
            String title,
            String description,
            String projectId
    ) {}

    public static List<ModResult> search(String query) throws Exception {

        String encodedQuery = URLEncoder.encode(
                query, StandardCharsets.UTF_8
        );

        // Alleen mods voor Fabric en Minecraft 26.2
        String facets = URLEncoder.encode(
                "[[\"project_type:mod\"]," +
                        "[\"categories:fabric\"]," +
                        "[\"versions:26.2\"]]",
                StandardCharsets.UTF_8
        );

        String url = "https://api.modrinth.com/v2/search"
                + "?query=" + encodedQuery
                + "&facets=" + facets
                + "&limit=15";

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "NexoraLauncher/1.0")
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) {
            throw new IllegalStateException(
                    "Modrinth HTTP " + response.statusCode()
            );
        }

        JsonObject data = JsonParser.parseString(
                response.body()
        ).getAsJsonObject();

        List<ModResult> results = new ArrayList<>();

        for (JsonElement element : data.getAsJsonArray("hits")) {
            JsonObject mod = element.getAsJsonObject();

            results.add(new ModResult(
                    mod.get("title").getAsString(),
                    mod.get("description").getAsString(),
                    mod.get("project_id").getAsString()
            ));
        }

        return results;
    }
}
