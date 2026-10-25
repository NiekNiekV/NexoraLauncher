
package com.nexora.launcher;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Updater {

    private static final String RELEASE_URL =
            "https://api.github.com/repos/NiekNiekV/NexoraLauncher/releases/latest";

    public static String checkForUpdates() {
        try {

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(10))
                    .build();


            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(RELEASE_URL))
                    .header("Accept", "application/vnd.github+json")
                    .timeout(java.time.Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() == 404) {
                return "Nog geen officiële updates beschikbaar.";
            }

            if (response.statusCode() != 200) {
                return "Updatecontrole mislukt: HTTP "
                        + response.statusCode();
            }

            String json = response.body();

            java.util.regex.Matcher matcher =
                    java.util.regex.Pattern.compile(
                            "\"tag_name\"\\s*:\\s*\"([^\"]+)\""
                    ).matcher(json);

            if (matcher.find()) {
                return "Nieuwste officiële release: " + matcher.group(1);
            }

            return "Geen versienummer gevonden.";

        } catch (Exception e) {
            return "Updatecontrole mislukt: " + e.getMessage();
        }
    }
}
