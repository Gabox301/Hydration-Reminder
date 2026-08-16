package com.hydration;

import java.util.concurrent.Executors;

import com.hydration.controller.MainController;
import com.hydration.model.HydrationEntry;
import com.hydration.service.DatabaseService;
import com.hydration.service.NotificationService;
import com.hydration.service.ReminderScheduler;
import com.hydration.util.AppIcons;
import com.hydration.util.TrayIconManager;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class HydrationApp extends Application {

    private DatabaseService db;
    private ReminderScheduler scheduler;
    private MainController mainController;

    @Override
    public void start(Stage stage) throws Exception {
        Platform.setImplicitExit(false); // la app sigue viva en el tray al cerrar la ventana

        db = new DatabaseService();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/main.fxml"));
        Parent root = loader.load();
        MainController controller = loader.getController();
        controller.init(db, this::restartScheduler);
        this.mainController = controller;

        Scene scene = new Scene(root, 360, 520);
        scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());

        stage.setTitle("HydrationReminder");
        stage.getIcons().addAll(AppIcons.loadAll(getClass()));
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> {
            e.consume();
            stage.hide(); // minimiza a la bandeja en vez de cerrar
        });
        stage.show();

        NotificationService notifier = new NotificationService(
                new TrayIconManager(
                        getClass().getResource("/icons/tray-icon.png").toExternalForm(),
                        stage::show,
                        this::quickLog,
                        this::togglePause,
                        this::onExit).getSystemTray());

        scheduler = new ReminderScheduler(db, notifier, db::loadSettings);
        scheduler.start();
    }

    private void quickLog(int ml) {
        Executors.newVirtualThreadPerTaskExecutor().submit(() -> {
            db.addEntry(HydrationEntry.of(ml));
            // Refresca la vista principal para reflejar el vaso registrado desde la
            // bandeja.
            if (mainController != null) {
                mainController.refreshProgress();
            }
        });
    }

    private boolean paused = false;

    private void togglePause() {
        paused = !paused;
        if (paused)
            scheduler.pause();
        else
            scheduler.resume();
    }

    private void onExit() {
        if (scheduler != null)
            scheduler.shutdown();
    }

    /**
     * Reinicia el scheduler para que tome el nuevo intervalo/horario tras guardar
     * Configuración.
     */
    private void restartScheduler() {
        if (scheduler != null)
            scheduler.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
