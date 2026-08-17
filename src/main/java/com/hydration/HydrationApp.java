package com.hydration;

import java.util.Optional;
import java.util.concurrent.Executors;

import com.hydration.controller.MainController;
import com.hydration.model.HydrationEntry;
import com.hydration.service.DatabaseService;
import com.hydration.service.NotificationService;
import com.hydration.service.ReminderScheduler;
import com.hydration.util.AppIcons;
import com.hydration.util.Fonts;
import com.hydration.util.TrayIconManager;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

public class HydrationApp extends Application {

    private DatabaseService db;
    private ReminderScheduler scheduler;
    private MainController mainController;
    private TrayIconManager trayIconManager;
    private Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        Platform.setImplicitExit(false); // la app sigue viva en el tray al cerrar la ventana

        Fonts.loadAll(getClass());

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
        this.primaryStage = stage;

        trayIconManager = new TrayIconManager(
                getClass().getResourceAsStream("/icons/tray-icon.png"),
                this::showMainWindow,
                this::quickLog,
                this::togglePause,
                this::requestExit);

        scheduler = new ReminderScheduler(db, new NotificationService(), db::loadSettings);
        scheduler.start();
    }

    /** Reabre (o trae al frente) la ventana principal desde la bandeja. */
    private void showMainWindow() {
        if (!primaryStage.isShowing()) {
            primaryStage.show();
        }
        primaryStage.toFront();
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

    /**
     * Pide confirmación antes de salir: cerrar la app detiene los recordatorios,
     * y esa decisión debe ser explícitamente del usuario (no dejar el proceso
     * colgado ni cerrarlo por accidente).
     */
    private void requestExit() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Salir de HydrationReminder");
        alert.setHeaderText("¿Cerrar la app?");
        alert.setContentText(
                "Si cerrás HydrationReminder no vas a recibir más recordatorios de hidratación hasta que la vuelvas a abrir.");

        ButtonType closeApp = new ButtonType("Cerrar", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(closeApp, cancel);

        // El Alert es una ventana propia: no hereda los íconos del Stage principal,
        // así que se asigna el logo como gráfico y como ícono de la barra de título.
        Image appIcon = AppIcons.load(getClass(), 64);
        if (appIcon != null) {
            alert.setGraphic(new ImageView(appIcon));
        }
        alert.setOnShown(e -> {
            Stage window = (Stage) alert.getDialogPane().getScene().getWindow();
            window.getIcons().addAll(AppIcons.loadAll(getClass()));
        });

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == closeApp) {
            exitApp();
        }
    }

    private void exitApp() {
        if (scheduler != null)
            scheduler.shutdown();
        if (trayIconManager != null)
            trayIconManager.remove();
        Platform.exit();
        // Force-exit: hilos no-daemon de librerías de terceros (dorkbox.notify)
        // podrían impedir que el JVM termine solo con Platform.exit().
        System.exit(0);
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
