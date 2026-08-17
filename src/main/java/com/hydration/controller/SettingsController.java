package com.hydration.controller;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import com.hydration.model.UserSettings;
import com.hydration.service.DatabaseService;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.stage.Stage;

public class SettingsController {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    private Spinner<Integer> goalSpinner;
    @FXML
    private Spinner<Integer> intervalSpinner;
    @FXML
    private ComboBox<String> activeFromCombo;
    @FXML
    private ComboBox<String> activeToCombo;
    @FXML
    private CheckBox quietModeCheck;
    @FXML
    private ComboBox<String> quietFromCombo;
    @FXML
    private ComboBox<String> quietToCombo;

    private DatabaseService db;
    private Runnable onSaved;

    /** Inyectado desde MainController al abrir la ventana. */
    public void init(DatabaseService db, Runnable onSaved) {
        this.db = db;
        this.onSaved = onSaved;

        goalSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(500, 6000, 2000, 100));
        intervalSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 240, 60, 5));

        List<String> timeSlots = buildTimeSlots();
        for (ComboBox<String> combo : List.of(activeFromCombo, activeToCombo, quietFromCombo, quietToCombo)) {
            combo.setItems(FXCollections.observableArrayList(timeSlots));
        }

        Executors.newVirtualThreadPerTaskExecutor().submit(() -> {
            UserSettings settings = db.loadSettings();
            Platform.runLater(() -> populateFields(settings));
        });
    }

    private List<String> buildTimeSlots() {
        List<String> slots = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            for (int m = 0; m < 60; m += 30) {
                slots.add("%02d:%02d".formatted(h, m));
            }
        }
        return slots;
    }

    private void populateFields(UserSettings s) {
        goalSpinner.getValueFactory().setValue(s.dailyGoalMl());
        intervalSpinner.getValueFactory().setValue(s.reminderIntervalMinutes());
        activeFromCombo.setValue(s.activeFrom().format(TIME_FORMAT));
        activeToCombo.setValue(s.activeTo().format(TIME_FORMAT));
        quietModeCheck.setSelected(s.quietModeEnabled());
        quietFromCombo.setValue(s.quietFrom().format(TIME_FORMAT));
        quietToCombo.setValue(s.quietTo().format(TIME_FORMAT));
    }

    @FXML
    private void save() {
        UserSettings updated = new UserSettings(
                goalSpinner.getValue(),
                intervalSpinner.getValue(),
                LocalTime.parse(activeFromCombo.getValue(), TIME_FORMAT),
                LocalTime.parse(activeToCombo.getValue(), TIME_FORMAT),
                quietModeCheck.isSelected(),
                LocalTime.parse(quietFromCombo.getValue(), TIME_FORMAT),
                LocalTime.parse(quietToCombo.getValue(), TIME_FORMAT));

        Executors.newVirtualThreadPerTaskExecutor().submit(() -> {
            db.saveSettings(updated);
            Platform.runLater(() -> {
                if (onSaved != null)
                    onSaved.run();
                closeWindow();
            });
        });
    }

    @FXML
    private void cancel() {
        closeWindow();
    }

    private void closeWindow() {
        ((Stage) goalSpinner.getScene().getWindow()).close();
    }
}
