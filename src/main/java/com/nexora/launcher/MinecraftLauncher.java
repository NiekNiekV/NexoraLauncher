
package com.nexora.launcher;

import com.google.gson.*;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Pattern;

public class MinecraftLauncher {

    private static final String VERSION = "26.2";
    private static final String FABRIC =
            "fabric-loader-0.19.5-26.2";

    public static void checkInstallation() {
        File mc = minecraftDir();
        System.out.println("Minecraft-map: " + mc.isDirectory());
        System.out.println("Libraries: "
                + new File(mc, "libraries").isDirectory());
        System.out.println("Assets: "
                + new File(mc, "assets").isDirectory());
    }

    public static void launchMinecraft(
            String playerName,
            UUID playerId,
            String accessToken
    ) {
        if (playerName == null || playerId == null
                || accessToken == null || accessToken.isBlank()) {
            System.out.println("Log eerst in met Microsoft.");
            return;
        }

        try {
            File mc = minecraftDir();
            File libraries = new File(mc, "libraries");
            File assets = new File(mc, "assets");
            File natives = new File(
                    mc, "versions/" + FABRIC + "/natives"
            );

            File baseFile = new File(
                    mc, "versions/" + VERSION + "/" + VERSION + ".json"
            );
            File fabricFile = new File(
                    mc, "versions/" + FABRIC + "/" + FABRIC + ".json"
            );
            File gameJar = new File(
                    mc, "versions/" + VERSION + "/" + VERSION + ".jar"
            );

            File javaExe = new File(
                    "C:\\Program Files\\Amazon Corretto\\jdk25.0.4_7\\bin\\java.exe"
            );

            if (!javaExe.isFile() || !gameJar.isFile()
                    || !baseFile.isFile() || !fabricFile.isFile()
                    || !natives.isDirectory()) {
                throw new IllegalStateException(
                        "Java, Minecraft, Fabric of natives-map ontbreekt."
                );
            }

            JsonObject base = readJson(baseFile);
            JsonObject fabric = readJson(fabricFile);

            List<String> jars = new ArrayList<>();

            addLibraries(
                    base.getAsJsonArray("libraries"),
                    libraries, jars
            );
            addLibraries(
                    fabric.getAsJsonArray("libraries"),
                    libraries, jars
            );

            jars.add(gameJar.getAbsolutePath());

            String classpath = String.join(
                    File.pathSeparator, jars
            );

            String assetIndex = base
                    .getAsJsonObject("assetIndex")
                    .get("id").getAsString();

            File assetIndexFile = new File(
                    assets, "indexes/" + assetIndex + ".json"
            );

            if (!assetIndexFile.isFile()) {
                throw new IllegalStateException(
                        "Asset-index ontbreekt: " + assetIndex
                );
            }

            String mainClass = fabric
                    .get("mainClass").getAsString();

            Map<String, String> values = new HashMap<>();

            values.put("auth_player_name", playerName);
            values.put("auth_uuid",
                    playerId.toString().replace("-", ""));
            values.put("auth_access_token", accessToken);
            values.put("auth_xuid", "");
            values.put("clientid", "");
            values.put("user_type", "msa");
            values.put("version_name", FABRIC);
            values.put("version_type", "release");
            values.put("game_directory", mc.getAbsolutePath());
            values.put("assets_root", assets.getAbsolutePath());
            values.put("game_assets", assets.getAbsolutePath());
            values.put("assets_index_name", assetIndex);
            values.put("natives_directory", natives.getAbsolutePath());
            values.put("library_directory", libraries.getAbsolutePath());
            values.put("classpath", classpath);
            values.put("classpath_separator", File.pathSeparator);
            values.put("launcher_name", "Nexora");
            values.put("launcher_version", "1.0.0");
            values.put("resolution_width", "1280");
            values.put("resolution_height", "720");
            values.put("user_properties", "{}");
            values.put("profile_properties", "{}");

            List<String> command = new ArrayList<>();
            command.add(javaExe.getAbsolutePath());

            // Geheugen voor Minecraft
            command.add("-Xmx4G");

            // Windows-native DLL-bestanden
            command.add(
                    "-Djava.library.path=" + natives.getAbsolutePath()
            );
            command.add(
                    "-Dorg.lwjgl.librarypath=" + natives.getAbsolutePath()
            );

            // JVM-argumenten uit Minecraft JSON
            addArguments(
                    base.getAsJsonObject("arguments")
                            .getAsJsonArray("jvm"),
                    command, values
            );

            // De Fabric-startklasse
            command.add(mainClass);

            // Minecraft game-argumenten
            addArguments(
                    base.getAsJsonObject("arguments")
                            .getAsJsonArray("game"),
                    command, values
            );

            // Eventuele extra Fabric-argumenten
            if (fabric.has("arguments")) {
                JsonObject fabricArgs =
                        fabric.getAsJsonObject("arguments");

                if (fabricArgs.has("game")) {
                    addArguments(
                            fabricArgs.getAsJsonArray("game"),
                            command, values
                    );
                }
            }

            System.out.println("Minecraft starten...");
            System.out.println("Speler: " + playerName);
            System.out.println("Fabric: " + FABRIC);
            System.out.println("Classpath-bestanden: " + jars.size());
            System.out.println("Java: " + javaExe.getAbsolutePath());

            // Toon de volledige opdracht niet:
            // hierin staat je geheime toegangstoken.
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.directory(mc);
            builder.redirectErrorStream(true);

            File logDir = new File(mc, "logs");
            Files.createDirectories(logDir.toPath());

            // Bewaar speluitvoer in een lokaal logbestand.
            File logFile = new File(logDir, "nexora-launch.log");
            builder.redirectOutput(
                    ProcessBuilder.Redirect.to(logFile)
            );

            Process process = builder.start();

            System.out.println(
                    "Minecraft-proces gestart! PID: " + process.pid()
            );
            System.out.println(
                    "Als het spel niet opent, controleer het logbestand:"
            );
            System.out.println(logFile.getAbsolutePath());

        } catch (Exception error) {
            error.printStackTrace();

            javafx.application.Platform.runLater(() -> {
                javafx.scene.control.Alert alert =
                        new javafx.scene.control.Alert(
                                javafx.scene.control.Alert.AlertType.ERROR
                        );

                alert.setTitle("Nexora Launcher");
                alert.setHeaderText("Minecraft starten mislukt!");
                alert.setContentText(error.toString());
                alert.showAndWait();
            });
        }
    }

    private static File minecraftDir() {
        return new File(
                System.getenv("APPDATA"), ".minecraft"
        );
    }

    private static JsonObject readJson(File file) throws Exception {
        try (FileReader reader = new FileReader(file)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static void addLibraries(
            JsonArray libraries,
            File librariesDir,
            List<String> classpath
    ) {
        for (JsonElement element : libraries) {
            JsonObject lib = element.getAsJsonObject();

            if (!allowed(lib)) {
                continue;
            }

            String name = lib.get("name").getAsString();

            // Native JARs horen niet in de gewone classpath.
            if (name.contains(":natives-")) {
                continue;
            }

            String path;

            if (lib.has("downloads")
                    && lib.getAsJsonObject("downloads")
                    .has("artifact")) {
                path = lib.getAsJsonObject("downloads")
                        .getAsJsonObject("artifact")
                        .get("path").getAsString();
            } else {
                String[] parts = name.split(":");

                if (parts.length < 3) {
                    continue;
                }

                path = parts[0].replace('.', '/')
                        + "/" + parts[1]
                        + "/" + parts[2]
                        + "/" + parts[1] + "-" + parts[2]
                        + ".jar";
            }

            File jar = new File(librariesDir, path);

            if (!jar.isFile()) {
                throw new IllegalStateException(
                        "Library ontbreekt: " + path
                );
            }

            if (!classpath.contains(jar.getAbsolutePath())) {
                classpath.add(jar.getAbsolutePath());
            }
        }
    }

    private static boolean allowed(JsonObject item) {
        if (!item.has("rules")) {
            return true;
        }

        boolean allowed = false;

        for (JsonElement element : item.getAsJsonArray("rules")) {
            JsonObject rule = element.getAsJsonObject();

            boolean matches = true;

            if (rule.has("os")) {
                JsonObject os = rule.getAsJsonObject("os");

                if (os.has("name")) {
                    matches = "windows".equals(
                            os.get("name").getAsString()
                    );
                }

                if (os.has("arch")) {
                    String arch = System.getProperty("os.arch");
                    matches &= Pattern.compile(
                            os.get("arch").getAsString()
                    ).matcher(arch).find();
                }

                if (os.has("version")) {
                    matches &= Pattern.compile(
                            os.get("version").getAsString()
                    ).matcher(
                            System.getProperty("os.version")
                    ).find();
                }
            }

            // Optionele features zoals demo en quick play
            // staan standaard uit.
            if (rule.has("features")) {
                JsonObject features =
                        rule.getAsJsonObject("features");

                for (Map.Entry<String, JsonElement> feature :
                        features.entrySet()) {
                    boolean actual = false;
                    boolean expected =
                            feature.getValue().getAsBoolean();

                    if (actual != expected) {
                        matches = false;
                    }
                }
            }

            if (matches) {
                allowed = "allow".equals(
                        rule.get("action").getAsString()
                );
            }
        }

        return allowed;
    }

    private static void addArguments(
            JsonArray args,
            List<String> command,
            Map<String, String> values
    ) {
        for (JsonElement element : args) {
            if (element.isJsonPrimitive()) {
                command.add(replace(
                        element.getAsString(), values
                ));
                continue;
            }

            JsonObject object = element.getAsJsonObject();

            if (!allowed(object)) {
                continue;
            }

            JsonElement value = object.get("value");

            if (value.isJsonArray()) {
                for (JsonElement part : value.getAsJsonArray()) {
                    command.add(replace(
                            part.getAsString(), values
                    ));
                }
            } else {
                command.add(replace(
                        value.getAsString(), values
                ));
            }
        }
    }

    private static String replace(
            String input,
            Map<String, String> values
    ) {
        String result = input;

        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace(
                    "${" + entry.getKey() + "}",
                    entry.getValue()
            );
        }

        if (result.contains("${")) {
            throw new IllegalStateException(
                    "Onbekend Minecraft-startargument."
            );
        }

        return result;
    }
}
