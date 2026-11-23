package org.example.triangular_number;

import javafx.beans.binding.Bindings;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class TriangularController {
    @FXML private TextField upperLimitField;
    @FXML private TextField threadCountField;
    @FXML private Button startButton;
    @FXML private Button stopButton;
    @FXML private Label errorLabel;
    @FXML private Label statusLabel;
    @FXML private FlowPane workerPane;
    @FXML private TextArea resultsArea;
    @FXML private Label triangularCountLabel;
    @FXML private Label nonTriangularCountLabel;
    @FXML private Label totalCheckedLabel;

    private SearchRun activeRun;

    private static final class SearchRun {
        final int limit;
        final List<Task<List<Integer>>> tasks = new ArrayList<>();
        int finished;

        SearchRun(int limit) {
            this.limit = limit;
        }

        String finishedCount() {
            return "Finished threads: " + finished + " of " + tasks.size() + ".";
        }
    }

    @FXML
    private void initialize() {
        errorLabel.visibleProperty().bind(errorLabel.textProperty().isNotEmpty());
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());
        upperLimitField.textProperty().addListener(observable -> errorLabel.setText(""));
        threadCountField.textProperty().addListener(observable -> errorLabel.setText(""));
        clearResults();
        setRunning(false);
    }

    @FXML
    private void startSearch() {
        if (activeRun != null) {
            return;
        }
        TriangularSearch.Inputs inputs;
        try {
            inputs = TriangularSearch.parseInputs(upperLimitField.getText(), threadCountField.getText());
        } catch (IllegalArgumentException ex) {
            errorLabel.setText(ex.getMessage());
            return;
        }

        errorLabel.setText("");
        workerPane.getChildren().clear();
        clearResults();
        SearchRun run = new SearchRun(inputs.upperLimit());
        activeRun = run;
        setRunning(true);
        for (var range : TriangularSearch.partition(run.limit, inputs.threadCount())) {
            addWorker(run, range);
        }
        statusLabel.setText("Running. " + run.finishedCount());
        for (int i = 0; i < run.tasks.size(); i++) {
            Thread thread = new Thread(run.tasks.get(i), "triangular-worker-" + (i + 1));
            thread.setDaemon(true);
            thread.start();
        }
    }

    private void addWorker(SearchRun run, TriangularSearch.Range range) {
        int number = run.tasks.size() + 1;
        Task<List<Integer>> task = new Task<>() {
            @Override
            protected List<Integer> call() {
                return TriangularSearch.scan(range, this::isCancelled,
                        checked -> updateProgress(checked, range.size()));
            }
        };
        run.tasks.add(task);

        Label title = new Label("Thread " + number);
        ProgressIndicator progress = new ProgressIndicator(0);
        progress.setPrefSize(50, 50);
        progress.setAccessibleText("Thread " + number + " progress");
        progress.progressProperty().bind(Bindings.max(0, task.progressProperty()));
        Label interval = new Label(range.start() + " to " + range.end());
        interval.setWrapText(true);
        interval.setMaxWidth(Double.MAX_VALUE);
        Label count = new Label(range.size() + (range.size() == 1 ? " number" : " numbers"));
        Label state = new Label("Queued");
        VBox worker = new VBox(6, title, progress, interval, count, state);
        worker.setPrefWidth(150);
        worker.setAlignment(Pos.TOP_CENTER);
        workerPane.getChildren().add(worker);

        task.setOnRunning(event -> state.setText("Running"));
        task.setOnSucceeded(event -> {
            if (activeRun != run) {
                return;
            }
            state.setText("Done");
            run.finished++;
            if (run.finished == run.tasks.size()) {
                finish(run);
            } else {
                statusLabel.setText("Running. " + run.finishedCount());
            }
        });
        task.setOnFailed(event -> {
            if (activeRun == run) {
                cancelRun();
                statusLabel.setText("Search failed.");
            }
        });
    }

    private void finish(SearchRun run) {
        StringJoiner numbers = new StringJoiner(", ");
        int matches = 0;
        for (var task : run.tasks) {
            List<Integer> values = task.getValue();
            matches += values.size();
            for (int value : values) {
                numbers.add(Integer.toString(value));
            }
        }
        resultsArea.setText(numbers.toString());
        setTotals(matches, run.limit);
        statusLabel.setText("Complete. Checked all " + run.limit + " numbers from 1 to "
                + run.limit + ". " + run.finishedCount());
        activeRun = null;
        setRunning(false);
    }

    @FXML
    private void stopSearch() {
        if (activeRun != null) {
            cancelRun();
            statusLabel.setText("Stopped.");
        }
    }

    void shutdown() {
        if (activeRun != null) {
            cancelRun();
        }
    }

    private void cancelRun() {
        SearchRun cancelled = activeRun;
        activeRun = null;
        for (var task : cancelled.tasks) {
            task.cancel(true);
        }
        workerPane.getChildren().clear();
        clearResults();
        setRunning(false);
    }

    private void clearResults() {
        resultsArea.clear();
        setTotals(0, 0);
    }

    private void setTotals(int matches, int checked) {
        triangularCountLabel.setText("Triangular numbers: " + matches);
        nonTriangularCountLabel.setText("Non-triangular numbers: " + (checked - matches));
        totalCheckedLabel.setText("Total checked: " + checked);
    }

    private void setRunning(boolean running) {
        upperLimitField.setDisable(running);
        threadCountField.setDisable(running);
        startButton.setDisable(running);
        stopButton.setDisable(!running);
    }
}
