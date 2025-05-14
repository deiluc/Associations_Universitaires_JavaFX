package com.example.associations_universitaires_javafx;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

public class EventStatusController {
    private static final Logger LOGGER = Logger.getLogger(EventStatusController.class.getName());

    @FXML private TableView<Event> eventsTable;
    @FXML private TableColumn<Event, String> associationColumn;
    @FXML private TableColumn<Event, String> titleColumn;
    @FXML private TableColumn<Event, String> dateRangeColumn;
    @FXML private TableColumn<Event, String> timeRangeColumn;
    @FXML private TableColumn<Event, String> locationColumn;
    @FXML private TableColumn<Event, Void> actionColumn;

    public static class Event {
        private final SimpleStringProperty association;
        private final SimpleStringProperty title;
        private final SimpleStringProperty dateRange;
        private final SimpleStringProperty timeRange;
        private final SimpleStringProperty location;
        private final int eventId;

        public Event(int eventId, String association, String title, String dateRange, String timeRange, String location) {
            this.eventId = eventId;
            this.association = new SimpleStringProperty(association);
            this.title = new SimpleStringProperty(title);
            this.dateRange = new SimpleStringProperty(dateRange);
            this.timeRange = new SimpleStringProperty(timeRange);
            this.location = new SimpleStringProperty(location);
        }

        public String getAssociation() { return association.get(); }
        public String getTitle() { return title.get(); }
        public String getDateRange() { return dateRange.get(); }
        public String getTimeRange() { return timeRange.get(); }
        public String getLocation() { return location.get(); }
        public int getEventId() { return eventId; }
    }

    public void loadPendingEvents() {
        eventsTable.getItems().clear();
        String sql = "SELECT e.event_id, a.name AS association_name, e.title, e.event_date, e.end_date, e.start_time, e.end_time, e.location " +
                "FROM events e JOIN associations a ON e.association_id = a.association_id " +
                "WHERE e.status = 'PENDING'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            int count = 0;
            while (rs.next()) {
                String dateRange = rs.getTimestamp("event_date") != null && rs.getTimestamp("end_date") != null ?
                        rs.getTimestamp("event_date").toLocalDateTime().format(dateFormatter) + " to " +
                                rs.getTimestamp("end_date").toLocalDateTime().format(dateFormatter) : "No dates";
                String timeRange = rs.getTime("start_time") != null && rs.getTime("end_time") != null ?
                        rs.getTime("start_time").toLocalTime().format(timeFormatter) + " - " +
                                rs.getTime("end_time").toLocalTime().format(timeFormatter) : "Not specified";
                String location = rs.getString("location") != null ? rs.getString("location") : "No location";
                eventsTable.getItems().add(new Event(
                        rs.getInt("event_id"),
                        rs.getString("association_name"),
                        rs.getString("title"),
                        dateRange,
                        timeRange,
                        location
                ));
                count++;
            }
            LOGGER.info("Loaded " + count + " pending events");
        } catch (SQLException e) {
            LOGGER.severe("Failed to load pending events: " + e.getMessage() + ", SQL State: " + e.getSQLState());
            showAlert("Error", "Failed to load pending events: " + e.getMessage());
        }

        associationColumn.setCellValueFactory(cellData -> cellData.getValue().association);
        titleColumn.setCellValueFactory(cellData -> cellData.getValue().title);
        dateRangeColumn.setCellValueFactory(cellData -> cellData.getValue().dateRange);
        timeRangeColumn.setCellValueFactory(cellData -> cellData.getValue().timeRange);
        locationColumn.setCellValueFactory(cellData -> cellData.getValue().location);

        Callback<TableColumn<Event, Void>, TableCell<Event, Void>> cellFactory = param -> new TableCell<>() {
            private final Button approveBtn = new Button("Approve");
            private final Button rejectBtn = new Button("Reject");

            {
                approveBtn.setOnAction(event -> {
                    Event evt = getTableView().getItems().get(getIndex());
                    updateEventStatus(evt.getEventId(), "APPROVED");
                });
                rejectBtn.setOnAction(event -> {
                    Event evt = getTableView().getItems().get(getIndex());
                    updateEventStatus(evt.getEventId(), "REJECTED");
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox buttons = new HBox(5, approveBtn, rejectBtn);
                    setGraphic(buttons);
                }
            }
        };
        actionColumn.setCellFactory(cellFactory);
    }

    private void updateEventStatus(int eventId, String status) {
        String sql = "UPDATE events SET status = ? WHERE event_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, eventId);
            stmt.executeUpdate();
            loadPendingEvents();
            showAlert("Success", "Event " + (status.equals("APPROVED") ? "approved" : "rejected") + " successfully!");
        } catch (SQLException e) {
            LOGGER.severe("Failed to update event status: " + e.getMessage());
            showAlert("Error", "Failed to update event status: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}