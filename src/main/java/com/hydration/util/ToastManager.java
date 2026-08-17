package com.hydration.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Background;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * Muestra notificaciones de recordatorio como toasts propios de JavaFX con la
 * estética "Marea nocturna" de la app: ventana sin decorar y translúcida,
 * esquinas redondeadas, borde cian, tipografía Manrope y animación de entrada
 * desde la esquina inferior derecha.
 *
 * <p>
 * Características: apilado vertical (las toasts nuevas empujan a las
 * existentes hacia arriba, usando la altura real de cada una), auto-cierre a
 * los 8 segundos (se pausa al pasar el mouse por encima), clic en el cuerpo
 * para abrir la app y una fila de botones de acción opcional (p. ej. los
 * tamaños de vaso). Todo debe ejecutarse en el hilo de JavaFX; si se invoca
 * desde otro hilo, la llamada se reprograma con {@link Platform#runLater}.
 */
public class ToastManager {

    /**
     * Acción de un botón de la toast: la etiqueta visible y el handler a
     * ejecutar al pulsarlo (la toast se cierra automáticamente tras pulsar).
     */
    public record ToastAction(String label, Runnable action) {
    }

    private static final double CARD_WIDTH = 320;
    private static final double EDGE_MARGIN = 16;
    private static final double GAP = 12;
    private static final Duration SLIDE_DURATION = Duration.millis(260);
    private static final Duration VISIBLE_TIME = Duration.seconds(8);

    /**
     * Margen de la ventana alrededor de la tarjeta para que el dropshadow
     * quede contenido dentro de la ventana: con el pipeline de software, un
     * blur que excede los bordes de la ventana translúcida se muestra como un
     * fondo blanco. Debe ser mayor que offset + radio del shadow (8 + 22).
     */
    private static final double SHADOW_PADDING = 34;

    /** Toasts visibles, de abajo hacia arriba (el último es el más nuevo). */
    private final List<ActiveToast> active = new ArrayList<>();
    private final Map<Stage, PauseTransition> timers = new HashMap<>();

    private static final class ActiveToast {
        final Stage stage;
        final double height;

        ActiveToast(Stage stage, double height) {
            this.stage = stage;
            this.height = height;
        }
    }

    public void show(String title, String message, String iconPath,
            Runnable onOpen, List<ToastAction> actions) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> show(title, message, iconPath, onOpen, actions));
            return;
        }

        Stage stage = buildStage(title, message, iconPath, onOpen, actions);
        stage.show();
        stage.sizeToScene();
        active.add(new ActiveToast(stage, stage.getHeight()));
        position(stage);
        slideIn(stage);
        scheduleHide(stage);
        relayout();
        SoundPlayer.playNotification();
    }

    private Stage buildStage(String title, String message, String iconPath,
            Runnable onOpen, List<ToastAction> actions) {
        Stage stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setAlwaysOnTop(true);
        stage.setResizable(false);
        stage.getIcons().addAll(AppIcons.loadAll(ToastManager.class));

        VBox card = new VBox(8);
        card.getStyleClass().add("toast-card");
        card.setMaxWidth(CARD_WIDTH);
        card.setAlignment(Pos.CENTER_LEFT);

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        if (iconPath != null) {
            ImageView icon = new ImageView(loadImage(iconPath));
            icon.setFitWidth(40);
            icon.setFitHeight(40);
            icon.setPreserveRatio(true);
            header.getChildren().add(icon);
        }

        VBox texts = new VBox(3);
        texts.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(texts, Priority.ALWAYS);
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("toast-title");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(CARD_WIDTH - 120);
        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("toast-message");
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(CARD_WIDTH - 120);
        texts.getChildren().addAll(titleLabel, messageLabel);
        header.getChildren().add(texts);
        card.getChildren().add(header);

        if (actions != null && !actions.isEmpty()) {
            HBox actionRow = new HBox(6);
            actionRow.setAlignment(Pos.CENTER_RIGHT);
            for (ToastAction action : actions) {
                Button button = new Button(action.label());
                button.getStyleClass().add("toast-action");
                button.setOnAction(e -> {
                    action.action().run();
                    hide(stage);
                });
                actionRow.getChildren().add(button);
            }
            card.getChildren().add(actionRow);
        }

        card.setOnMouseEntered(e -> pauseHide(stage));
        card.setOnMouseExited(e -> resumeHide(stage));
        if (onOpen != null) {
            card.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && !(e.getTarget() instanceof Button)) {
                    onOpen.run();
                    hide(stage);
                }
            });
        }

        StackPane wrapper = new StackPane(card);
        wrapper.setPadding(new Insets(SHADOW_PADDING));
        wrapper.setBackground(Background.EMPTY);
        Scene scene = new Scene(wrapper, Color.TRANSPARENT);
        scene.getStylesheets().add(
                ToastManager.class.getResource("/styles/toast.css").toExternalForm());
        stage.setScene(scene);
        return stage;
    }

    private static Image loadImage(String classpathPath) {
        try (InputStream in = ToastManager.class.getResourceAsStream(classpathPath)) {
            if (in == null) {
                return null; // degrada con gracia: la toast se muestra sin ícono
            }
            return new Image(in);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo cargar el ícono " + classpathPath, e);
        }
    }

    private void position(Stage stage) {
        Rectangle2D vb = Screen.getPrimary().getVisualBounds();
        stage.setX(vb.getMaxX() - stage.getWidth() - EDGE_MARGIN);
        stage.setY(vb.getMaxY() - stage.getHeight() - EDGE_MARGIN);
    }

    /**
     * Reacomoda todas las toasts de abajo hacia arriba: la más nueva (última de
     * la lista) en el fondo y cada anterior un paso más arriba.
     */
    private void relayout() {
        Rectangle2D vb = Screen.getPrimary().getVisualBounds();
        double y = vb.getMaxY() - EDGE_MARGIN;
        for (int i = active.size() - 1; i >= 0; i--) {
            ActiveToast t = active.get(i);
            double targetY = y - t.height;
            if (Math.abs(t.stage.getY() - targetY) > 0.5) {
                animateY(t.stage, targetY);
            }
            y = targetY - GAP;
        }
    }

    private void animateY(Stage stage, double targetY) {
        Node root = stage.getScene().getRoot();
        double currentVisualY = stage.getY() + root.getTranslateY();
        stage.setY(targetY);
        // Mantiene la posición visual actual mientras se anima la translación a 0.
        root.setTranslateY(currentVisualY - stage.getY());
        TranslateTransition move = new TranslateTransition(SLIDE_DURATION, root);
        move.setToY(0);
        move.setOnFinished(e -> root.setTranslateY(0));
        move.play();
    }

    private void slideIn(Stage stage) {
        Node root = stage.getScene().getRoot();
        root.setTranslateY(24);
        root.setOpacity(0);
        FadeTransition fade = new FadeTransition(SLIDE_DURATION, root);
        fade.setToValue(1);
        TranslateTransition move = new TranslateTransition(SLIDE_DURATION, root);
        move.setToY(0);
        new ParallelTransition(root, fade, move).play();
    }

    private void scheduleHide(Stage stage) {
        PauseTransition timer = new PauseTransition(VISIBLE_TIME);
        timer.setOnFinished(e -> hide(stage));
        timers.put(stage, timer);
        timer.play();
    }

    private void pauseHide(Stage stage) {
        PauseTransition timer = timers.get(stage);
        if (timer != null) {
            timer.stop();
        }
    }

    private void resumeHide(Stage stage) {
        PauseTransition timer = timers.get(stage);
        if (timer != null) {
            timer.stop();
            timer.play();
        }
    }

    private void hide(Stage stage) {
        PauseTransition timer = timers.remove(stage);
        if (timer != null) {
            timer.stop();
        }
        active.removeIf(t -> t.stage == stage);
        relayout();

        Node root = stage.getScene().getRoot();
        FadeTransition fade = new FadeTransition(SLIDE_DURATION, root);
        fade.setToValue(0);
        TranslateTransition move = new TranslateTransition(SLIDE_DURATION, root);
        move.setByY(12);
        ParallelTransition out = new ParallelTransition(root, fade, move);
        out.setOnFinished(e -> stage.hide());
        out.play();
    }
}
