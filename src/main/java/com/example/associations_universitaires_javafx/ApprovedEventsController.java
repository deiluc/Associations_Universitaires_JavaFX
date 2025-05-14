package com.example.associations_universitaires_javafx;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Callback;

import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class ApprovedEventsController {
    private static final Logger LOGGER = Logger.getLogger(ApprovedEventsController.class.getName());

    @FXML private TableView<Event> eventsTable;
    @FXML private TableColumn<Event, String> statusColumn;
    @FXML private TableColumn<Event, String> associationColumn;
    @FXML private TableColumn<Event, String> titleColumn;
    @FXML private TableColumn<Event, String> dateRangeColumn;
    @FXML private TableColumn<Event, String> timeRangeColumn;
    @FXML private TableColumn<Event, String> locationColumn;
    @FXML private TableColumn<Event, Void> detailsColumn;

    public static class Event {
        private final SimpleStringProperty association;
        private final SimpleStringProperty title;
        private final SimpleStringProperty dateRange;
        private final SimpleStringProperty timeRange;
        private final SimpleStringProperty location;
        private final SimpleStringProperty status;
        private final int eventId;

        public Event(int eventId, String association, String title, String dateRange, String timeRange, String location, String status) {
            this.eventId = eventId;
            this.association = new SimpleStringProperty(association);
            this.title = new SimpleStringProperty(title);
            this.dateRange = new SimpleStringProperty(dateRange);
            this.timeRange = new SimpleStringProperty(timeRange);
            this.location = new SimpleStringProperty(location);
            this.status = new SimpleStringProperty(status);
        }

        public int getEventId() { return eventId; }
        public String getAssociation() { return association.get(); }
        public String getTitle() { return title.get(); }
        public String getDateRange() { return dateRange.get(); }
        public String getTimeRange() { return timeRange.get(); }
        public String getLocation() { return location.get(); }
        public String getStatus() { return status.get(); }
    }

    public void loadApprovedEvents() {
        eventsTable.getItems().clear();
        String sql = "SELECT e.event_id, a.name AS association_name, e.title, e.event_date, e.end_date, e.start_time, e.end_time, e.location, e.status " +
                "FROM events e JOIN associations a ON e.association_id = a.association_id " +
                "WHERE e.status IN ('APPROVED', 'CANCELED')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            int count = 0;
            while (rs.next()) {
                int eventId = rs.getInt("event_id");
                String association = rs.getString("association_name") != null ? rs.getString("association_name") : "Unknown";
                String title = rs.getString("title") != null ? rs.getString("title") : "No title";
                String dateRange = rs.getTimestamp("event_date") != null && rs.getTimestamp("end_date") != null ?
                        rs.getTimestamp("event_date").toLocalDateTime().format(dateFormatter) + " to " +
                                rs.getTimestamp("end_date").toLocalDateTime().format(dateFormatter) : "No dates";
                String timeRange = rs.getTime("start_time") != null && rs.getTime("end_time") != null ?
                        rs.getTime("start_time").toLocalTime().format(timeFormatter) + " - " +
                                rs.getTime("end_time").toLocalTime().format(timeFormatter) : "Not specified";
                String location = rs.getString("location") != null ? rs.getString("location") : "No location";
                String status = rs.getString("status") != null ? rs.getString("status") : "UNKNOWN";

                LOGGER.info("Loaded event: EventID=" + eventId + ", Association=" + association + ", Title=" + title +
                        ", DateRange=" + dateRange + ", TimeRange=" + timeRange + ", Location=" + location + ", Status=" + status);
                eventsTable.getItems().add(new Event(eventId, association, title, dateRange, timeRange, location, status));
                count++;
            }
            LOGGER.info("Loaded " + count + " approved or canceled events");

            // Configure columns
            associationColumn.setCellValueFactory(cellData -> cellData.getValue().association);
            titleColumn.setCellValueFactory(cellData -> cellData.getValue().title);
            dateRangeColumn.setCellValueFactory(cellData -> cellData.getValue().dateRange);
            timeRangeColumn.setCellValueFactory(cellData -> cellData.getValue().timeRange);
            locationColumn.setCellValueFactory(cellData -> cellData.getValue().location);
            statusColumn.setCellValueFactory(cellData -> cellData.getValue().status);

            // Customize status column with color
            statusColumn.setCellFactory(new Callback<>() {
                @Override
                public TableCell<Event, String> call(TableColumn<Event, String> param) {
                    return new TableCell<>() {
                        @Override
                        protected void updateItem(String item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty || item == null) {
                                setText(null);
                                setStyle("");
                            } else {
                                setText(item);
                                if ("APPROVED".equals(item)) {
                                    setTextFill(Color.GREEN);
                                } else if ("CANCELED".equals(item)) {
                                    setTextFill(Color.RED);
                                } else {
                                    setTextFill(Color.BLACK);
                                }
                            }
                        }
                    };
                }
            });

            // Configure details column with a button
            detailsColumn.setCellFactory(new Callback<>() {
                @Override
                public TableCell<Event, Void> call(TableColumn<Event, Void> param) {
                    return new TableCell<>() {
                        private final Button detailsButton = new Button("Details");

                        {
                            detailsButton.setOnAction(event -> {
                                Event evt = getTableView().getItems().get(getIndex());
                                showEventDetails(evt);
                            });
                        }

                        @Override
                        protected void updateItem(Void item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty) {
                                setGraphic(null);
                            } else {
                                setGraphic(detailsButton);
                            }
                        }
                    };
                }
            });
        } catch (SQLException e) {
            LOGGER.severe("Failed to load approved or canceled events: " + e.getMessage() + ", SQL State: " + e.getSQLState());
            showAlert("Error", "Failed to load approved or canceled events: " + e.getMessage());
        }
    }

    private void showEventDetails(Event event) {
        String description = fetchDescription(event.getEventId());
        if (description == null) {
            description = "No description";
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Event Details");
        dialog.setHeaderText("Details for Event: " + event.getTitle());

        VBox content = new VBox(10);
        content.setStyle("-fx-padding: 10;");

        // Add each field with wrapping if necessary
        addWrappedLabel(content, "Status: ", event.getStatus());
        addWrappedLabel(content, "Association: ", event.getAssociation());
        addWrappedLabel(content, "Title: ", event.getTitle());
        addWrappedLabel(content, "Date Range: ", event.getDateRange());
        addWrappedLabel(content, "Time Range: ", event.getTimeRange());
        addWrappedLabel(content, "Location: ", event.getLocation());
        addWrappedLabel(content, "Description: ", description);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private void addWrappedLabel(VBox content, String labelPrefix, String text) {
        // Create a label for the field name
        HBox fieldBox = new HBox(5);
        Label fieldLabel = new Label(labelPrefix);
        fieldBox.getChildren().add(fieldLabel);

        // Calculate the alignment padding based on the longest label prefix
        double indent = 120; // Approximate pixel width for alignment (adjust if needed)
        fieldLabel.setMinWidth(indent);

        // Split the text into chunks of 100 characters or less, respecting word boundaries
        List<String> lines = wrapText(text, 100);

        // Add the first line next to the label
        Label firstLineLabel = new Label(lines.get(0));
        fieldBox.getChildren().add(firstLineLabel);
        content.getChildren().add(fieldBox);

        // Add remaining lines, indented to align with the first line's content
        for (int i = 1; i < lines.size(); i++) {
            HBox indentedLine = new HBox();
            Label indentLabel = new Label("");
            indentLabel.setMinWidth(indent);
            Label lineLabel = new Label(lines.get(i));
            indentedLine.getChildren().addAll(indentLabel, lineLabel);
            content.getChildren().add(indentedLine);
        }
    }

    private List<String> wrapText(String text, int maxLength) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            lines.add("");
            return lines;
        }

        int start = 0;
        while (start < text.length()) {
            if (start + maxLength >= text.length()) {
                lines.add(text.substring(start));
                break;
            }

            // Find the last space or special character before maxLength
            int end = start + maxLength;
            if (!Character.isWhitespace(text.charAt(end)) && !isSpecialCharacter(text.charAt(end))) {
                // Move backwards to find the last space or special character
                int lastSpace = text.lastIndexOf(' ', end);
                int lastSpecial = findLastSpecialCharacter(text, start, end);
                int breakPoint = Math.max(lastSpace, lastSpecial);
                if (breakPoint <= start) {
                    // No space or special character found, force break at maxLength
                    breakPoint = end;
                }
                end = breakPoint;
            }

            // Add the line (trim to avoid extra spaces)
            String line = text.substring(start, end).trim();
            lines.add(line);
            start = end + 1;

            // Skip any leading spaces for the next line
            while (start < text.length() && Character.isWhitespace(text.charAt(start))) {
                start++;
            }
        }

        return lines;
    }

    private int findLastSpecialCharacter(String text, int start, int end) {
        for (int i = end; i > start; i--) {
            if (isSpecialCharacter(text.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private boolean isSpecialCharacter(char c) {
        return c == '-' || c == ',' || c == '.' || c == '!' || c == '?' || c == ';';
    }

    private String fetchDescription(int eventId) {
        String sql = "SELECT description FROM events WHERE event_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, eventId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("description");
            }
        } catch (SQLException e) {
            LOGGER.severe("Failed to fetch description for event ID " + eventId + ": " + e.getMessage());
            showAlert("Error", "Failed to fetch event description: " + e.getMessage());
        }
        return null;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}