
package com.nexora.launcher;


import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;
import javafx.scene.control.ComboBox;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import javafx.application.Platform;
import net.raphimc.minecraftauth.msa.model.MsaDeviceCode;
import net.raphimc.minecraftauth.msa.model.MsaApplicationConfig;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.java.JavaAuthManager;
import net.raphimc.minecraftauth.msa.service.impl.DeviceCodeMsaAuthService;
import net.lenni0451.commons.httpclient.HttpClient;
import net.raphimc.minecraftauth.msa.data.MsaConstants;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.control.Alert;
import javafx.scene.control.Hyperlink;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Main extends Application {

    private JavaAuthManager authManager;
    private static final String RED = "#ff303b";
    private static final String BG = "#0b0b0f";
    private static final String PANEL = "#14141b";
    private static final String MUTED = "#92929f";

    private Label status;

    private Button navButton(String text, boolean selected) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setPadding(new Insets(13, 16, 13, 16));
        button.setStyle(
                "-fx-background-color: " +
                        (selected ? "#30151b" : "transparent") + ";" +
                        "-fx-text-fill: " + (selected ? RED : "#c9c9d2") + ";" +
                        "-fx-font-size: 14px;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;"
        );
        return button;
    }

    private Label label(String text, int size, String color) {
        Label label = new Label(text);
        label.setFont(Font.font("Arial", size));
        label.setTextFill(Color.web(color));
        return label;
    }

    @Override
    public void start(Stage stage) {
        // Sidebar
        Label logo = label("NEXORA", 25, RED);
        logo.setStyle("-fx-font-weight: bold;");

        Label edition = label("GAMING CLIENT", 10, MUTED);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.NEVER);

        Button home = navButton("⌂    Home", true);
        Button mods = navButton("⚙    Mods", false);
        Button settings = navButton("☷    Settings", false);

        Label menuTitle = label("WORKSPACE", 10, MUTED);

        VBox sidebar = new VBox(
                10, logo, edition, new Region(), menuTitle,
                home, mods, settings
        );
        sidebar.setPadding(new Insets(28, 18, 24, 18));
        sidebar.setPrefWidth(205);
        sidebar.setStyle("-fx-background-color: #101015;");

        // Header
        Label pageTitle = label("Overview", 24, "#ffffff");
        Label subtitle = label(
                "Welcome back to your gaming space.", 12, MUTED
        );

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        Label profile = label("N  PLAYER", 12, "#eeeeF4");

        HBox header = new HBox(
                16, new VBox(5, pageTitle, subtitle), headerSpacer, profile
        );
        header.setAlignment(Pos.CENTER_LEFT);

        // Main hero panel
        Image bannerImage = new Image(
                getClass().getResource("/nexora-banner.png").toExternalForm()
        );


        Label eyebrow = label("YOUR NEXT ADVENTURE STARTS HERE", 11, RED);
        Label heroTitle = label("Ready to play?", 32, "#ffffff");
        heroTitle.setStyle("-fx-font-weight: bold;");

        Label heroText = label(
                "Your Minecraft world is waiting.", 14, "#b5b5c0"
        );


        Label versionNumber = label("Version: Not configured", 14, "#ffffff");

ComboBox<String> mcVersion = new ComboBox<>();


        mcVersion.getItems().addAll(
                "Latest Release",
                "26.2",
                "Fabric 26.2"
        );

        mcVersion.setValue("Fabric 26.2");
        mcVersion.setMaxWidth(220);
        mcVersion.setStyle("-fx-background-color: #25232b;");

        Button play = new Button("▶    PLAY NOW");
        play.setPrefWidth(200);
        play.setPrefHeight(48);
        play.setStyle(
                "-fx-background-color: " + RED + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 15px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;"
        );


        Button modsButton = new Button("MODS");

        modsButton.setStyle(
                "-fx-background-color: #1565C0;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 15px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 10 25;"
        );

        modsButton.setOnAction(event -> {
            javafx.stage.Stage modsWindow = new javafx.stage.Stage();

            modsWindow.setTitle("Nexora - Mod Manager");

            javafx.scene.control.Label title =
                    new javafx.scene.control.Label("NEXORA MOD MANAGER");

            title.setStyle(
                    "-fx-text-fill: white;" +
                            "-fx-font-size: 22px;" +
                            "-fx-font-weight: bold;"
            );

            javafx.scene.control.Button modrinthButton =
                    new javafx.scene.control.Button("Search on Modrinth");

            javafx.scene.control.Button jarButton =
                    new javafx.scene.control.Button("Add .JAR Mod");

            modrinthButton.setOnAction(e -> {
                getHostServices().showDocument(
                        "https://modrinth.com/mods"
                );
            });

            jarButton.setOnAction(e -> {
                javafx.stage.FileChooser chooser =
                        new javafx.stage.FileChooser();

                chooser.setTitle("Selecteer een Minecraft-mod");

                chooser.getExtensionFilters().add(
                        new javafx.stage.FileChooser.ExtensionFilter(
                                "Minecraft Mods (*.jar)", "*.jar"
                        )
                );

                java.io.File selected =
                        chooser.showOpenDialog(modsWindow);

                if (selected != null) {
                    try {
                        java.io.File folder = new java.io.File(
                                System.getenv("APPDATA"),
                                ".minecraft/mods"
                        );

                        java.nio.file.Files.createDirectories(
                                folder.toPath()
                        );

                        java.nio.file.Files.copy(
                                selected.toPath(),
                                folder.toPath().resolve(selected.getName()),
                                java.nio.file.StandardCopyOption.REPLACE_EXISTING
                        );

                        title.setText(
                                "Installed: " + selected.getName()
                        );

                    } catch (Exception ex) {
                        title.setText("Mod installation failed!");
                        ex.printStackTrace();
                    }
                }
            });

            javafx.scene.layout.VBox layout =
                    new javafx.scene.layout.VBox(
                            20, title, modrinthButton, jarButton
                    );

            layout.setAlignment(
                    javafx.geometry.Pos.CENTER
            );

            layout.setStyle(
                    "-fx-background-color: #0C1425;" +
                            "-fx-padding: 30;"
            );

            javafx.scene.Scene scene =
                    new javafx.scene.Scene(layout, 500, 350);

            modsWindow.setScene(scene);
            modsWindow.show();
        });



        status = label("●  Launcher interface ready", 12, "#77df9b");

// Microsoft-login-knop
        Button microsoftLogin = new Button("MICROSOFT LOGIN");
        microsoftLogin.setPrefWidth(200);
        microsoftLogin.setPrefHeight(42);
        microsoftLogin.setStyle(
                "-fx-background-color: #25232b;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 13px;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;"
        );

        microsoftLogin.setOnAction(event -> startMicrosoftLogin());

        play.setOnAction(event -> {
            if ("Fabric 26.2".equals(mcVersion.getValue())) {

                if (authManager == null) {
                    status.setText("● Log eerst in met Microsoft.");
                    return;
                }

                try {
                    var minecraftProfile = authManager
                            .getMinecraftProfile()
                            .getUpToDate();

                    String accessToken = authManager
                            .getMinecraftToken()
                            .getUpToDate()
                            .getToken();

                    status.setText("● Minecraft wordt voorbereid...");

                    MinecraftLauncher.launchMinecraft(
                            minecraftProfile.getName(),
                            minecraftProfile.getId(),
                            accessToken
                    );

                    status.setText("● Minecraft-startcontrole uitgevoerd!");

                } catch (Exception e) {
                    status.setText("● Minecraft voorbereiden mislukt.");
                    System.out.println(
                            "Startcontrole mislukt: " + e.getMessage()
                    );
                }

            } else {
                status.setText("● Kies voorlopig Fabric 26.2.");
            }
        });
        ImageView banner = new ImageView(bannerImage);
        banner.setPreserveRatio(true);
        banner.setSmooth(true);

        StackPane hero = new StackPane();
        hero.setMinSize(0, 0);

        banner.fitWidthProperty().bind(hero.widthProperty());
        banner.fitHeightProperty().bind(hero.heightProperty());

// HIER KOMT DE OVERLAY
        Rectangle overlay = new Rectangle();
        overlay.widthProperty().bind(hero.widthProperty());
        overlay.heightProperty().bind(hero.heightProperty());
        overlay.setFill(Color.rgb(5, 5, 10, 0.55));

// DAARNA JE BESTAANDE VBox heroContent

        VBox heroContent = new VBox(
                17, eyebrow, heroTitle, heroText,
                new Region(), versionNumber, mcVersion,
                play, modsButton, microsoftLogin, status
        );

        heroContent.setPadding(new Insets(30));
        heroContent.setAlignment(Pos.CENTER_LEFT);
        heroContent.setMaxWidth(Double.MAX_VALUE);

        VBox.setVgrow(heroContent.getChildren().get(3), Priority.ALWAYS);

        hero.getChildren().addAll(banner, overlay, heroContent);
        hero.setMinHeight(365);
        hero.setStyle(
                "-fx-background-radius: 16;" +
                        "-fx-border-color: #ff303b;" +
                        "-fx-border-radius: 16;"
        );
        hero.setClip(new Rectangle(2000, 2000));


        // Bottom information cards
        VBox clientCard = new VBox(
                9,
                label("CLIENT STATUS", 10, MUTED),
                label("Nexora is running", 15, "#ffffff"),
                label("Interface loaded successfully", 11, "#77df9b")
        );

        VBox updateCard = new VBox(
                9,
                label("UPDATES", 10, MUTED),
                label("Version 1.0.0", 15, "#ffffff"),
                label("Update service not configured", 11, "#c4a0a5")
        );

        for (VBox card : new VBox[]{clientCard, updateCard}) {
            card.setPadding(new Insets(18));
            card.setStyle(
                    "-fx-background-color: " + PANEL + ";" +
                            "-fx-background-radius: 12;" +
                            "-fx-border-color: #272730;" +
                            "-fx-border-radius: 12;"
            );
            HBox.setHgrow(card, Priority.ALWAYS);
        }

        HBox cards = new HBox(14, clientCard, updateCard);

        VBox content = new VBox(25, header, hero, cards);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color: " + BG + ";");
        VBox.setVgrow(hero, Priority.ALWAYS);

        BorderPane root = new BorderPane();
        root.setLeft(sidebar);
        root.setCenter(content);

// Home-pagina
        home.setOnAction(event -> {
            root.setCenter(content);
        });

// Mods-pagina
        mods.setOnAction(event -> {
            root.setCenter(ModManager.createPage());
        });

        Scene scene = new Scene(root, 1000, 680);
        stage.setTitle("Nexora Launcher");
        stage.setMinWidth(800);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.show();
        loadSavedLogin();
    }


    private void startMicrosoftLogin() {
        status.setText("● Microsoft-login wordt gestart...");

        new Thread(() -> {
            try {
                HttpClient httpClient = MinecraftAuth.createHttpClient();



                Consumer<MsaDeviceCode> callback = deviceCode ->
                        Platform.runLater(() -> {
                            String url = deviceCode.getDirectVerificationUri();
                            String code = deviceCode.getUserCode();

                            Hyperlink loginLink = new Hyperlink(url);
                            loginLink.setOnAction(event -> {
                                try {
                                    java.awt.Desktop.getDesktop().browse(
                                            java.net.URI.create(url)
                                    );
                                } catch (Exception e) {
                                    status.setText("Browser openen mislukt.");
                                }
                            });

                            Button copyLink = new Button("Link kopiëren");
                            copyLink.setOnAction(event -> {
                                ClipboardContent content = new ClipboardContent();
                                content.putString(url);
                                Clipboard.getSystemClipboard().setContent(content);
                                status.setText("Microsoft-link gekopieerd!");
                            });

                            Button copyCode = new Button("Code kopiëren");
                            copyCode.setOnAction(event -> {
                                ClipboardContent content = new ClipboardContent();
                                content.putString(code);
                                Clipboard.getSystemClipboard().setContent(content);
                                status.setText("Microsoft-code gekopieerd!");
                            });

                            VBox loginBox = new VBox(
                                    10,
                                    new Label("Klik op de link om Microsoft te openen:"),
                                    loginLink,
                                    copyLink,
                                    new Label("Verificatiecode: " + code),
                                    copyCode
                            );
                            loginBox.setPadding(new Insets(15));

                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle("Nexora - Microsoft Login");
                            alert.setHeaderText("Aanmelden bij Microsoft");
                            alert.getDialogPane().setContent(loginBox);
                            alert.show();
                        });


                this.authManager =
                        JavaAuthManager.create(httpClient)
                                .login(DeviceCodeMsaAuthService::new, callback);

                try {
                    java.nio.file.Path accountDir =
                            java.nio.file.Paths.get(
                                    System.getenv("APPDATA"), ".nexora"
                            );

                    java.nio.file.Files.createDirectories(accountDir);

                    java.nio.file.Path accountFile =
                            accountDir.resolve("account.json");

                    java.nio.file.Files.writeString(
                            accountFile,
                            JavaAuthManager.toJson(authManager).toString(),
                            java.nio.charset.StandardCharsets.UTF_8
                    );

                    Platform.runLater(() ->
                            status.setText("● Login opgeslagen voor Nexora!")
                    );

                } catch (Exception e) {
                    Platform.runLater(() ->
                            status.setText("● Login opslaan mislukt.")
                    );
                }

                try {
                    String playerName = authManager
                            .getMinecraftProfile()
                            .getUpToDate()
                            .getName();

                    java.util.UUID playerId = authManager
                            .getMinecraftProfile()
                            .getUpToDate()
                            .getId();

                    System.out.println("Minecraft profiel-ID gevonden: " + (playerId != null));

                    status.setText("● Klaar om Minecraft te starten als " + playerName);

                } catch (java.io.IOException e) {
                    status.setText("● Profiel ophalen mislukt: " + e.getMessage());
                }

            } catch (Exception e) {
                Platform.runLater(() ->
                        status.setText("● Login mislukt: " + e.getMessage())
                );
            }
        }).start();
    }


    private void loadSavedLogin() {
        new Thread(() -> {
            try {
                java.nio.file.Path accountFile =
                        java.nio.file.Paths.get(
                                System.getenv("APPDATA"),
                                ".nexora",
                                "account.json"
                        );

                if (!java.nio.file.Files.exists(accountFile)) {
                    Platform.runLater(() ->
                            status.setText("● Log in met Microsoft")
                    );
                    return;
                }

                String json = java.nio.file.Files.readString(
                        accountFile,
                        java.nio.charset.StandardCharsets.UTF_8
                );

                HttpClient httpClient = MinecraftAuth.createHttpClient();

                this.authManager =
                        JavaAuthManager.fromJson(
                                httpClient,
                                com.google.gson.JsonParser.parseString(json)
                                        .getAsJsonObject()
                        );

                String playerName = authManager
                        .getMinecraftProfile()
                        .getUpToDate()
                        .getName();

                Platform.runLater(() ->
                        status.setText("● Ingelogd als " + playerName)
                );

            } catch (Exception e) {
                Platform.runLater(() ->
                        status.setText("● Log opnieuw in met Microsoft")
                );
            }
        }).start();
    }

    private void launchFabric() {
        status.setText("● Minecraft 26.2 met Fabric voorbereiden...");

        if (authManager == null) {
            status.setText("● Log eerst in met Microsoft!");
            return;
        }

        String minecraftDir =
                System.getenv("APPDATA") + "\\.minecraft";

        java.io.File fabricJson = new java.io.File(
                minecraftDir,
                "versions/fabric-loader-0.19.5-26.2/fabric-loader-0.19.5-26.2.json"
        );

        java.io.File baseJson = new java.io.File(
                minecraftDir,
                "versions/26.2/26.2.json"
        );

        if (!fabricJson.exists() || !baseJson.exists()) {
            status.setText("● Minecraft- of Fabric-configuratie ontbreekt.");
            return;
        }

        try {
            String playerName = authManager
                    .getMinecraftProfile()
                    .getUpToDate()
                    .getName();

            try (java.io.FileReader reader =
                         new java.io.FileReader(fabricJson)) {

                com.google.gson.JsonObject fabricConfig =
                        com.google.gson.JsonParser
                                .parseReader(reader)
                                .getAsJsonObject();

                String mainClass =
                        fabricConfig.get("mainClass").getAsString();

                status.setText(
                        "● Fabric gevonden! " + playerName
                                + " — " + mainClass
                );
            }

        } catch (Exception e) {
            status.setText(
                    "● Fabric starten voorbereiden mislukt: "
                            + e.getMessage()
            );
        }
    }


    public static void main(String[] args) {

        launch(args);
    }
}
