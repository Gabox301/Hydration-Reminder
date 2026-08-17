package com.hydration.controller;

import java.util.concurrent.Executors;

import com.hydration.model.HydrationEntry;
import com.hydration.model.UserSettings;
import com.hydration.service.DatabaseService;
import com.hydration.util.AppIcons;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class MainController {

    @FXML
    private ProgressBar progressBar;
    @FXML
    private Label progressLabel;
    @FXML
    private Label progressSublabel;
    @FXML
    private Label remainingLabel;
    @FXML
    private HBox streakPill;
    @FXML
    private Label streakLabel;

    private DatabaseService db;
    private Runnable onSettingsSaved;

    /** Inyectado desde HydrationApp tras cargar el FXML. */
    public void init(DatabaseService db, Runnable onSettingsSaved) {
        this.db = db;
        this.onSettingsSaved = onSettingsSaved;
        refreshProgress();
    }

    @FXML
    private void logSmallGlass() {
        logAmount(200);
    }

    @FXML
    private void logMediumGlass() {
        logAmount(300);
    }

    @FXML
    private void logLargeGlass() {
        logAmount(500);
    }

    private void logAmount(int ml) {
        Executors.newVirtualThreadPerTaskExecutor().submit(() -> {
            db.addEntry(HydrationEntry.of(ml));
            Platform.runLater(this::refreshProgress);
        });
    }

    /**
     * Recalcula y actualiza el progreso del día. Seguro de llamar desde cualquier
     * hilo.
     */
    public void refreshProgress() {
        Executors.newVirtualThreadPerTaskExecutor().submit(() -> {
            int total = db.getTodayTotalMl();
            UserSettings settings = db.loadSettings();
            int streak = db.getCurrentStreakDays(settings.dailyGoalMl());

            Platform.runLater(() -> {
                double ratio = Math.min(1.0, total / (double) settings.dailyGoalMl());
                progressBar.setProgress(ratio);
                progressLabel.setText(total + " ml");
                progressSublabel.setText("de " + settings.dailyGoalMl() + " ml hoy");

                int remaining = Math.max(0, settings.dailyGoalMl() - total);
                remainingLabel.setText(remaining == 0
                        ? "¡Meta del día cumplida!"
                        : "Faltan " + remaining + " ml");

                boolean showStreak = streak > 0;
                streakPill.setVisible(showStreak);
                streakPill.setManaged(showStreak);
                streakLabel.setText(streak + (streak == 1 ? " día" : " días"));
            });
        });
    }

    @FXML
    private void openHistory() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/history.fxml"));
            Parent root = loader.load();
            HistoryController controller = loader.getController();
            controller.init(db);

            Stage stage = new Stage();
            stage.initModality(Modality.NONE);
            stage.setTitle("Historial — Hydration Reminder");
            stage.getIcons().addAll(AppIcons.loadAll(getClass()));
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            throw new RuntimeException("No se pudo abrir el historial", e);
        }
    }

    @FXML
    private void openSettings() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/settings.fxml"));
            Parent root = loader.load();
            SettingsController controller = loader.getController();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Configuración — Hydration Reminder");
            stage.getIcons().addAll(AppIcons.loadAll(getClass()));
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
            stage.setScene(scene);

            controller.init(db, () -> {
                refreshProgress();
                if (onSettingsSaved != null)
                    onSettingsSaved.run();
            });

            stage.showAndWait();
        } catch (Exception e) {
            throw new RuntimeException("No se pudo abrir la configuración", e);
        }
    }
}
