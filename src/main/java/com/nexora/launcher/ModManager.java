
package com.nexora.launcher;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class ModManager {

    public static VBox createPage() {

        // Minecraft mods-map
        File modsFolder = new File(
                System.getenv("APPDATA"),
                ".minecraft/mods"
        );

        // Titel
        Label title = new Label("NEXORA MOD MANAGER");
        title.setStyle(
                "-fx-text-fill: white;" +
                        "-fx-font-size: 26px;" +
                        "-fx-font-weight: bold;"
        );

        Label description = new Label(
                "Manage your Minecraft Fabric mods"
        );
        description.setStyle("-fx-text-fill: #aaaaaa;");

        // Modlijst
        ObservableList<String> mods =
                FXCollections.observableArrayList();

        ListView<String> modList = new ListView<>(mods);
        modList.setPrefHeight(320);

        // Statusmelding
        Label status = new Label("Ready");
        status.setStyle("-fx-text-fill: #aaaaaa;");

        // Mods laden
        Runnable refreshMods = () -> {
            mods.clear();

            File[] files = modsFolder.listFiles();

            if (files != null) {
                for (File file : files) {
                    String name = file.getName().toLowerCase();

                    if (file.isFile() &&
                            (name.endsWith(".jar") ||
                                    name.endsWith(".jar.disabled"))) {
                        mods.add(file.getName());
                    }
                }
            }

            FXCollections.sort(mods);

            status.setText(
                    "Installed mods: " + mods.size()
            );
        };

        // Knoppen
        Button addJar = new Button("+ ADD .JAR MOD");
        Button refresh = new Button("REFRESH");
        Button browse = new Button("BROWSE MODRINTH");


        TextField searchField = new TextField();
        searchField.setPromptText("Search mods on Modrinth...");

        Button searchButton = new Button("SEARCH MODS");


        Button installButton = new Button("INSTALL SELECTED MOD");

        installButton.setStyle(
                "-fx-background-color: #ff303b;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;"
        );

        ListView<ModrinthSearch.ModResult> searchResults =
                new ListView<>();

        searchResults.setPrefHeight(220);

        searchResults.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(
                    ModrinthSearch.ModResult mod,
                    boolean empty
            ) {
                super.updateItem(mod, empty);

                if (empty || mod == null) {
                    setText(null);
                } else {
                    setText(mod.title() + "\n" + mod.description());
                }
            }
        });

        searchButton.setOnAction(event -> {
            String query = searchField.getText().trim();

            if (query.isEmpty()) {
                status.setText("Typ eerst een modnaam.");
                return;
            }

            searchButton.setDisable(true);
            status.setText("Searching Modrinth...");

            javafx.concurrent.Task<
                    java.util.List<ModrinthSearch.ModResult>
                    > task = new javafx.concurrent.Task<>() {

                @Override
                protected java.util.List<ModrinthSearch.ModResult> call()
                        throws Exception {
                    return ModrinthSearch.search(query);
                }
            };

            task.setOnSucceeded(e -> {
                searchResults.getItems().setAll(task.getValue());
                status.setText(
                        "Found " + task.getValue().size() + " mods."
                );
                searchButton.setDisable(false);
            });

            task.setOnFailed(e -> {
                status.setText(
                        "Search failed: "
                                + task.getException().getMessage()
                );
                searchButton.setDisable(false);
            });

            Thread thread = new Thread(task, "modrinth-search");
            thread.setDaemon(true);
            thread.start();
        });


        installButton.setOnAction(event -> {

            ModrinthSearch.ModResult selected =
                    searchResults.getSelectionModel().getSelectedItem();

            if (selected == null) {
                status.setText("Selecteer eerst een mod.");
                return;
            }

            installButton.setDisable(true);
            status.setText("Installing " + selected.title() + "...");

            javafx.concurrent.Task<String> installTask =
                    new javafx.concurrent.Task<>() {

                        @Override
                        protected String call() throws Exception {
                            return ModrinthInstaller.install(
                                    selected.projectId()
                            );
                        }
                    };

            installTask.setOnSucceeded(e -> {
                refreshMods.run();
                status.setText(
                        "Installed: " + installTask.getValue()
                );
                installButton.setDisable(false);
            });

            installTask.setOnFailed(e -> {
                status.setText(
                        "Installation failed: "
                                + installTask.getException().getMessage()
                );
                installButton.setDisable(false);
            });

            Thread thread = new Thread(
                    installTask, "modrinth-install"
            );
            thread.setDaemon(true);
            thread.start();
        });


        Button enable = new Button("ENABLE");
        Button disable = new Button("DISABLE");
        Button delete = new Button("DELETE");

        enable.setOnAction(event -> {
            String selected = modList.getSelectionModel().getSelectedItem();

            if (selected == null) {
                status.setText("Selecteer eerst een mod.");
                return;
            }

            if (!selected.endsWith(".jar.disabled")) {
                status.setText("Deze mod staat al aan.");
                return;
            }

            try {
                File source = new File(modsFolder, selected);
                File target = new File(
                        modsFolder,
                        selected.substring(0, selected.length() - ".disabled".length())
                );

                if (target.exists()) {
                    status.setText("Er bestaat al een ingeschakelde versie.");
                    return;
                }

                Files.move(source.toPath(), target.toPath());
                refreshMods.run();
                status.setText("Mod ingeschakeld: " + target.getName());

            } catch (Exception ex) {
                status.setText("Inschakelen mislukt: " + ex.getMessage());
            }
        });

        disable.setOnAction(event -> {
            String selected = modList.getSelectionModel().getSelectedItem();

            if (selected == null) {
                status.setText("Selecteer eerst een mod.");
                return;
            }

            if (!selected.endsWith(".jar")) {
                status.setText("Deze mod staat al uit.");
                return;
            }

            try {
                File source = new File(modsFolder, selected);
                File target = new File(modsFolder, selected + ".disabled");

                if (target.exists()) {
                    status.setText("Er bestaat al een uitgeschakelde versie.");
                    return;
                }

                Files.move(source.toPath(), target.toPath());
                refreshMods.run();
                status.setText("Mod uitgeschakeld: " + selected);

            } catch (Exception ex) {
                status.setText("Uitschakelen mislukt: " + ex.getMessage());
            }
        });

        delete.setOnAction(event -> {
            String selected = modList.getSelectionModel().getSelectedItem();

            if (selected == null) {
                status.setText("Selecteer eerst een mod.");
                return;
            }

            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Nexora - Mod verwijderen");
            confirmation.setHeaderText("Mod verwijderen?");
            confirmation.setContentText(selected);

            confirmation.showAndWait().ifPresent(result -> {
                if (result == ButtonType.OK) {
                    try {
                        Files.delete(
                                new File(modsFolder, selected).toPath()
                        );
                        refreshMods.run();
                        status.setText("Mod verwijderd: " + selected);

                    } catch (Exception ex) {
                        status.setText("Verwijderen mislukt: " + ex.getMessage());
                    }
                }
            });
        });


        addJar.setStyle(
                "-fx-background-color: #ff303b;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;"
        );

        refresh.setStyle(
                "-fx-background-color: #252936;" +
                        "-fx-text-fill: white;"
        );

        browse.setStyle(
                "-fx-background-color: #252936;" +
                        "-fx-text-fill: white;"
        );

        // Eigen JAR-mod toevoegen
        addJar.setOnAction(event -> {

            FileChooser chooser = new FileChooser();
            chooser.setTitle("Selecteer een Minecraft-mod");

            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter(
                            "Minecraft Mods (*.jar)",
                            "*.jar"
                    )
            );

            File selected = chooser.showOpenDialog(
                    addJar.getScene().getWindow()
            );

            if (selected == null) {
                return;
            }

            try {
                Files.createDirectories(
                        modsFolder.toPath()
                );

                File destination = new File(
                        modsFolder,
                        selected.getName()
                );

                // Voorkom onbedoeld overschrijven
                if (destination.exists()) {
                    status.setText(
                            "Mod bestaat al: " + selected.getName()
                    );
                    return;
                }

                Files.copy(
                        selected.toPath(),
                        destination.toPath(),
                        StandardCopyOption.COPY_ATTRIBUTES
                );

                refreshMods.run();

                status.setText(
                        "Installed: " + selected.getName()
                );

            } catch (Exception ex) {
                status.setText(
                        "Installation failed: " + ex.getMessage()
                );
            }
        });

        // Modlijst opnieuw laden
        refresh.setOnAction(event -> refreshMods.run());

        // Modrinth openen
        browse.setOnAction(event -> {
            try {
                java.awt.Desktop.getDesktop().browse(
                        java.net.URI.create(
                                "https://modrinth.com/mods"
                        )
                );
            } catch (Exception ex) {
                status.setText(
                        "Cannot open Modrinth: " + ex.getMessage()
                );
            }
        });

        Label installedTitle = new Label("INSTALLED MODS");
        installedTitle.setStyle(
                "-fx-text-fill: white;" +
                        "-fx-font-size: 18px;" +
                        "-fx-font-weight: bold;"
        );

        HBox buttons = new HBox(
                12, addJar, refresh, browse
        );

        HBox manageButtons = new HBox(
                12, enable, disable, delete
        );

        manageButtons.setAlignment(Pos.CENTER_LEFT);

        buttons.setAlignment(Pos.CENTER_LEFT);

        VBox page = new VBox(
                18,
                title,
                description,
                buttons,
                searchField,
                searchButton,
                searchResults,
                installButton,
                installedTitle,
                modList,
                manageButtons,
                status
        );

        page.setPadding(new Insets(30));
        page.setStyle(
                "-fx-background-color: #0D0F17;"
        );

        VBox.setVgrow(modList, Priority.ALWAYS);

        // Eerste keer mods inladen
        refreshMods.run();

        return page;
    }
}
