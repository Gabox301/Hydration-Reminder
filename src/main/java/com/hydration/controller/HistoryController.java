package com.hydration.controller;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Executors;

import com.hydration.model.UserSettings;
import com.hydration.service.DatabaseService;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class HistoryController {

    private static final int DAYS_TO_SHOW = 14;
    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("dd/MM");

    @FXML
    private LineChart<String, Number> historyChart;
    @FXML
    private CategoryAxis dateAxis;
    @FXML
    private NumberAxis mlAxis;
    @FXML
    private Label averageLabel;
    @FXML
    private Label bestDayLabel;
    @FXML
    private Label streakLabel;

    private DatabaseService db;

    public void init(DatabaseService db) {
        this.db = db;
        historyChart.getStylesheets().add(getClass().getResource("/styles/history-chart.css").toExternalForm());
        loadData();
    }

    private void loadData() {
        Executors.newVirtualThreadPerTaskExecutor().submit(() -> {
            List<DatabaseService.DailyTotal> totals = db.getDailyTotals(DAYS_TO_SHOW);
            UserSettings settings = db.loadSettings();
            int streak = db.getCurrentStreakDays(settings.dailyGoalMl());

            int total = totals.stream().mapToInt(DatabaseService.DailyTotal::totalMl).sum();
            int average = totals.isEmpty() ? 0 : total / totals.size();
            int best = totals.stream().mapToInt(DatabaseService.DailyTotal::totalMl).max().orElse(0);

            Platform.runLater(() -> {
                XYChart.Series<String, Number> consumptionSeries = new XYChart.Series<>();
                consumptionSeries.setName("Consumo diario");

                XYChart.Series<String, Number> goalSeries = new XYChart.Series<>();
                goalSeries.setName("Meta");

                for (DatabaseService.DailyTotal t : totals) {
                    String label = t.date().format(DAY_FORMAT);
                    consumptionSeries.getData().add(new XYChart.Data<>(label, t.totalMl()));
                    goalSeries.getData().add(new XYChart.Data<>(label, settings.dailyGoalMl()));
                }

                historyChart.getData().clear();
                historyChart.getData().add(consumptionSeries);
                historyChart.getData().add(goalSeries);

                averageLabel.setText(average + " ml");
                bestDayLabel.setText(best + " ml");
                streakLabel.setText(streak + (streak == 1 ? " día" : " días"));
            });
        });
    }

    @FXML
    private void close() {
        ((Stage) historyChart.getScene().getWindow()).close();
    }
}
