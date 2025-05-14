package com.example.associations_universitaires_javafx;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.logging.Logger;

public class EventController {
    private static final Logger LOGGER = Logger.getLogger(EventController.class.getName());

    @FXML private ComboBox<String> associationComboBox;
    @FXML private TextField titleField;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private TextField startTimeField;
    @FXML private TextField endTimeField;
    @FXML private TextField locationField;
    @FXML private TextArea descriptionField;
    @FXML private Button createEventBtn;
    @FXML private TableView<Event> eventsTable;
    @FXML private TableColumn<Event, String> titleColumn;
    @FXML private TableColumn<Event, String> dateRangeColumn;
    @FXML private TableColumn<Event, String> timeRangeColumn;
    @FXML private TableColumn<Event, String> locationColumn;
    @FXML private TableColumn<Event, String> statusColumn;
    @FXML private TableColumn<Event, Void> actionColumn;

    private String associationName;
    private String currentUserEmail;
    private String currentUserRole;
    private int currentUserId;
    private Event editingEvent; // Track the event being edited

    public static class Event {
        private final SimpleStringProperty title;
        private final SimpleStringProperty dateRange;
        private final SimpleStringProperty timeRange;
        private final SimpleStringProperty location;
        private final SimpleStringProperty status;
        private final int eventId;

        public Event(int eventId, String title, String dateRange, String timeRange, String location, String status) {
            this.eventId = eventId;
            this.title = new SimpleStringProperty(title);
            this.dateRange = new SimpleStringProperty(dateRange);
            this.timeRange = new SimpleStringProperty(timeRange);
            this.location = new SimpleStringProperty(location);
            this.status = new SimpleStringProperty(status);
        }

        public String getTitle() { return title.get(); }
        public String getDateRange() { return dateRange.get(); }
        public String getTimeRange() { return timeRange.get(); }
        public String getLocation() { return location.get(); }
        public String getStatus() { return status.get(); }
        public int getEventId() { return eventId; }
    }

    public void initializeData(String associationName, String currentUserEmail, String currentUserRole, int currentUserId, boolean isHomePageContext) {
        this.associationName = associationName;
        this.currentUserEmail = currentUserEmail;
        this.currentUserRole = currentUserRole;
        this.currentUserId = currentUserId;

        setupTable();
        loadEvents();
        loadAssociations(); // Always load associations for manual selection

        boolean isAdmin = "admin".equals(currentUserRole);
        boolean isLeader = checkIfLeader();
        createEventBtn.setVisible(isAdmin || isLeader);
    }

    private void loadAssociations() {
        ObservableList<String> associations = FXCollections.observableArrayList();
        String sql = "SELECT name FROM associations";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                associations.add(rs.getString("name"));
            }
            associationComboBox.setItems(associations);
            // Pre-select the association if coming from association context
            if (associationName != null) {
                associationComboBox.setValue(associationName);
            } else if (!associations.isEmpty()) {
                associationComboBox.setValue(associations.get(0));
            }
        } catch (SQLException e) {
            LOGGER.severe("Failed to load associations: " + e.getMessage());
            showAlert("Error", "Failed to load associations: " + e.getMessage());
        }
    }

    private void setupTable() {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        dateRangeColumn.setCellValueFactory(new PropertyValueFactory<>("dateRange"));
        timeRangeColumn.setCellValueFactory(new PropertyValueFactory<>("timeRange"));
        locationColumn.setCellValueFactory(new PropertyValueFactory<>("location"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        Callback<TableColumn<Event, Void>, TableCell<Event, Void>> cellFactory = param -> new TableCell<>() {
            private final Button approveBtn = new Button("Approve");
            private final Button rejectBtn = new Button("Reject");
            private final Button editBtn = new Button("Edit");
            private final Button cancelBtn = new Button("Cancel");

            {
                approveBtn.setOnAction(event -> {
                    Event evt = getTableView().getItems().get(getIndex());
                    if (!"APPROVED".equals(evt.getStatus())) {
                        updateEventStatus(evt.getEventId(), "APPROVED");
                    }
                });
                rejectBtn.setOnAction(event -> {
                    Event evt = getTableView().getItems().get(getIndex());
                    if (!"REJECTED".equals(evt.getStatus())) {
                        updateEventStatus(evt.getEventId(), "REJECTED");
                    }
                });
                editBtn.setOnAction(event -> {
                    Event evt = getTableView().getItems().get(getIndex());
                    prepareEditEvent(evt);
                });
                cancelBtn.setOnAction(event -> {
                    Event evt = getTableView().getItems().get(getIndex());
                    cancelEvent(evt.getEventId());
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Event evt = getTableView().getItems().get(getIndex());
                    HBox buttons = new HBox(5);
                    boolean isAdmin = "admin".equals(currentUserRole);
                    boolean isLeader = checkIfLeader();
                    if (isAdmin && !"APPROVED".equals(evt.getStatus()) && !"REJECTED".equals(evt.getStatus())) {
                        buttons.getChildren().addAll(approveBtn, rejectBtn);
                    }
                    if (isAdmin || isLeader) {
                        buttons.getChildren().addAll(editBtn, cancelBtn);
                    }
                    setGraphic(buttons.getChildren().isEmpty() ? null : buttons);
                }
            }
        };
        actionColumn.setCellFactory(cellFactory);
    }

    private void loadEvents() {
        eventsTable.getItems().clear();
        String sql;
        if (associationName == null) {
            // Load all events if no specific association (e.g., from home page)
            sql = "SELECT event_id, title, event_date, end_date, start_time, end_time, location, status " +
                    "FROM events";
        } else {
            // Load events for the specific association
            sql = "SELECT event_id, title, event_date, end_date, start_time, end_time, location, status " +
                    "FROM events WHERE association_id = (SELECT association_id FROM associations WHERE name = ?)";
        }
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (associationName != null) {
                stmt.setString(1, associationName);
            }
            LOGGER.info("Executing loadEvents with association: " + (associationName != null ? associationName : "All"));
            ResultSet rs = stmt.executeQuery();
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
                String status = rs.getString("status") != null ? rs.getString("status") : "PENDING";
                eventsTable.getItems().add(new Event(
                        rs.getInt("event_id"),
                        rs.getString("title"),
                        dateRange,
                        timeRange,
                        location,
                        status
                ));
                count++;
            }
            LOGGER.info("Loaded " + count + " events for association: " + (associationName != null ? associationName : "All"));
        } catch (SQLException e) {
            LOGGER.severe("Failed to load events: " + e.getMessage() + ", SQL State: " + e.getSQLState());
            showAlert("Error", "Failed to load events: " + e.getMessage());
        }
    }

    @FXML
    private void handleCreateEvent() {
        if (editingEvent != null) {
            handleUpdateEvent();
            return;
        }

        String title = titleField.getText().trim();
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();
        String startTime = startTimeField.getText().trim();
        String endTime = endTimeField.getText().trim();
        String location = locationField.getText().trim();
        String description = descriptionField.getText().trim();
        String selectedAssociation = associationComboBox.getValue();

        // Validation
        if (title.isEmpty() || startDate == null || endDate == null || selectedAssociation == null) {
            showAlert("Error", "Title, start date, end date, and association are required!");
            return;
        }
        if (endDate.isBefore(startDate)) {
            showAlert("Error", "End date cannot be before start date!");
            return;
        }

        LocalTime startLocalTime = null;
        LocalTime endLocalTime = null;
        if (!startTime.isEmpty() || !endTime.isEmpty()) {
            String timeRegex = "^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$";
            if (!startTime.matches(timeRegex) || !endTime.matches(timeRegex)) {
                showAlert("Error", "Time must be in HH:mm format (e.g., 09:00, 17:30)!");
                return;
            }
            try {
                startLocalTime = LocalTime.parse(startTime, DateTimeFormatter.ofPattern("HH:mm"));
                endLocalTime = LocalTime.parse(endTime, DateTimeFormatter.ofPattern("HH:mm"));
                if (!endLocalTime.isAfter(startLocalTime)) {
                    showAlert("Error", "End time must be after start time!");
                    return;
                }
            } catch (DateTimeParseException e) {
                showAlert("Error", "Invalid time format: " + e.getMessage());
                return;
            }
        }

        Integer associationId = getAssociationId(selectedAssociation);
        if (associationId == null) {
            showAlert("Error", "No association found for name: " + selectedAssociation);
            LOGGER.severe("No association found for name: " + selectedAssociation);
            return;
        }

        if (!isValidUserId(currentUserId)) {
            showAlert("Error", "Invalid user ID: " + currentUserId);
            LOGGER.severe("Invalid user ID: " + currentUserId);
            return;
        }

        String sql = "INSERT INTO events (association_id, title, description, event_date, end_date, start_time, end_time, location, created_by, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, associationId);
            stmt.setString(2, title);
            stmt.setString(3, description.isEmpty() ? null : description);
            stmt.setTimestamp(4, Timestamp.valueOf(startDate.atStartOfDay()));
            stmt.setTimestamp(5, Timestamp.valueOf(endDate.atStartOfDay()));
            if (startLocalTime != null) {
                stmt.setTime(6, Time.valueOf(startLocalTime));
            } else {
                stmt.setNull(6, Types.TIME);
            }
            if (endLocalTime != null) {
                stmt.setTime(7, Time.valueOf(endLocalTime));
            } else {
                stmt.setNull(7, Types.TIME);
            }
            stmt.setString(8, location.isEmpty() ? null : location);
            stmt.setInt(9, currentUserId);
            stmt.setString(10, "PENDING");
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int eventId = generatedKeys.getInt(1);
                    LOGGER.info("Created event with ID: " + eventId + ", Title: " + title + ", Association ID: " + associationId + ", Created by: " + currentUserId + ", Status: PENDING");
                }
                loadEvents();
                clearForm();
                showAlert("Success", "Event created successfully and is pending approval!");
            } else {
                LOGGER.warning("No rows affected when creating event: " + title);
                showAlert("Error", "Event creation failed: No rows affected.");
            }
        } catch (SQLException e) {
            LOGGER.severe("Failed to create event: " + e.getMessage() + ", SQL State: " + e.getSQLState());
            showAlert("Error", "Failed to create event: " + e.getMessage() + " (SQL State: " + e.getSQLState() + ")");
        }
    }

    private void prepareEditEvent(Event event) {
        editingEvent = event;
        titleField.setText(event.getTitle());

        String[] dateRangeParts = event.getDateRange().split(" to ");
        if (dateRangeParts.length == 2) {
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
            try {
                startDatePicker.setValue(LocalDate.parse(dateRangeParts[0], dateFormatter));
                endDatePicker.setValue(LocalDate.parse(dateRangeParts[1], dateFormatter));
            } catch (DateTimeParseException e) {
                LOGGER.warning("Failed to parse dates for editing: " + event.getDateRange());
            }
        }

        String timeRange = event.getTimeRange();
        if (!"Not specified".equals(timeRange)) {
            String[] timeRangeParts = timeRange.split(" - ");
            if (timeRangeParts.length == 2) {
                startTimeField.setText(timeRangeParts[0]);
                endTimeField.setText(timeRangeParts[1]);
            }
        } else {
            startTimeField.clear();
            endTimeField.clear();
        }

        locationField.setText(event.getLocation());
        // Fetch description from the database
        String sql = "SELECT description FROM events WHERE event_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, event.getEventId());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String description = rs.getString("description");
                descriptionField.setText(description != null ? description : "");
            }
        } catch (SQLException e) {
            LOGGER.severe("Failed to load description for event " + event.getEventId() + ": " + e.getMessage());
        }

        // Fetch association name
        String assocName = getAssociationNameForEvent(event.getEventId());
        if (assocName != null) {
            associationComboBox.setValue(assocName);
        }

        createEventBtn.setText("Update Event");
    }

    private void handleUpdateEvent() {
        String title = titleField.getText().trim();
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();
        String startTime = startTimeField.getText().trim();
        String endTime = endTimeField.getText().trim();
        String location = locationField.getText().trim();
        String description = descriptionField.getText().trim();
        String selectedAssociation = associationComboBox.getValue();

        // Validation
        if (title.isEmpty() || startDate == null || endDate == null || selectedAssociation == null) {
            showAlert("Error", "Title, start date, end date, and association are required!");
            return;
        }
        if (endDate.isBefore(startDate)) {
            showAlert("Error", "End date cannot be before start date!");
            return;
        }

        LocalTime startLocalTime = null;
        LocalTime endLocalTime = null;
        if (!startTime.isEmpty() || !endTime.isEmpty()) {
            String timeRegex = "^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$";
            if (!startTime.matches(timeRegex) || !endTime.matches(timeRegex)) {
                showAlert("Error", "Time must be in HH:mm format (e.g., 09:00, 17:30)!");
                return;
            }
            try {
                startLocalTime = LocalTime.parse(startTime, DateTimeFormatter.ofPattern("HH:mm"));
                endLocalTime = LocalTime.parse(endTime, DateTimeFormatter.ofPattern("HH:mm"));
                if (!endLocalTime.isAfter(startLocalTime)) {
                    showAlert("Error", "End time must be after start time!");
                    return;
                }
            } catch (DateTimeParseException e) {
                showAlert("Error", "Invalid time format: " + e.getMessage());
                return;
            }
        }

        Integer associationId = getAssociationId(selectedAssociation);
        if (associationId == null) {
            showAlert("Error", "No association found for name: " + selectedAssociation);
            LOGGER.severe("No association found for name: " + selectedAssociation);
            return;
        }

        String sql = "UPDATE events SET association_id = ?, title = ?, description = ?, event_date = ?, end_date = ?, start_time = ?, end_time = ?, location = ? WHERE event_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, associationId);
            stmt.setString(2, title);
            stmt.setString(3, description.isEmpty() ? null : description);
            stmt.setTimestamp(4, Timestamp.valueOf(startDate.atStartOfDay()));
            stmt.setTimestamp(5, Timestamp.valueOf(endDate.atStartOfDay()));
            if (startLocalTime != null) {
                stmt.setTime(6, Time.valueOf(startLocalTime));
            } else {
                stmt.setNull(6, Types.TIME);
            }
            if (endLocalTime != null) {
                stmt.setTime(7, Time.valueOf(endLocalTime));
            } else {
                stmt.setNull(7, Types.TIME);
            }
            stmt.setString(8, location.isEmpty() ? null : location);
            stmt.setInt(9, editingEvent.getEventId());
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                LOGGER.info("Updated event with ID: " + editingEvent.getEventId());
                loadEvents();
                clearForm();
                showAlert("Success", "Event updated successfully!");
            } else {
                LOGGER.warning("No rows affected when updating event ID: " + editingEvent.getEventId());
                showAlert("Error", "Event update failed: No rows affected.");
            }
        } catch (SQLException e) {
            LOGGER.severe("Failed to update event: " + e.getMessage() + ", SQL State: " + e.getSQLState());
            showAlert("Error", "Failed to update event: " + e.getMessage());
        }
    }

    private void cancelEvent(int eventId) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Cancel Event");
        alert.setHeaderText("Are you sure you want to cancel this event?");
        alert.setContentText("This action cannot be undone.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String sql = "UPDATE events SET status = 'CANCELED' WHERE event_id = ?";
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, eventId);
                    int rowsAffected = stmt.executeUpdate();
                    if (rowsAffected > 0) {
                        LOGGER.info("Successfully updated event ID: " + eventId + " to CANCELED status");
                        loadEvents();
                        clearForm();
                        showAlert("Success", "Event status changed to CANCELED!");
                    } else {
                        LOGGER.warning("No rows affected when updating event ID: " + eventId + " to CANCELED status");
                        showAlert("Error", "Event cancellation failed: No rows affected. Check if event exists.");
                    }
                } catch (SQLException e) {
                    LOGGER.severe("Failed to cancel event ID: " + eventId + " - SQL Error: " + e.getMessage() + ", SQL State: " + e.getSQLState());
                    showAlert("Error", "Failed to cancel event: " + e.getMessage() + " (SQL State: " + e.getSQLState() + ")");
                }
            }
        });
    }

    private String getAssociationNameForEvent(int eventId) {
        String sql = "SELECT a.name FROM associations a JOIN events e ON a.association_id = e.association_id WHERE e.event_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, eventId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("name");
            }
        } catch (SQLException e) {
            LOGGER.severe("Failed to get association name for event " + eventId + ": " + e.getMessage());
        }
        return null;
    }

    private Integer getAssociationId(String assocName) {
        String sql = "SELECT association_id FROM associations WHERE name = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, assocName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("association_id");
            }
        } catch (SQLException e) {
            LOGGER.severe("Failed to get association_id for " + assocName + ": " + e.getMessage());
        }
        return null;
    }

    private boolean isValidUserId(int userId) {
        String sql = "SELECT user_id FROM users WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            LOGGER.severe("Failed to validate user_id " + userId + ": " + e.getMessage());
            return false;
        }
    }

    private void updateEventStatus(int eventId, String status) {
        String sql = "UPDATE events SET status = ? WHERE event_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, eventId);
            stmt.executeUpdate();
            loadEvents();
            showAlert("Success", "Event " + (status.equals("APPROVED") ? "approved" : "rejected") + " successfully!");
        } catch (SQLException e) {
            LOGGER.severe("Failed to update event status: " + e.getMessage());
            showAlert("Error", "Failed to update event status: " + e.getMessage());
        }
    }

    private boolean checkIfLeader() {
        if (associationName == null) return false;
        String sql = "SELECT leader_id FROM associations WHERE name = ? AND leader_id = (SELECT user_id FROM users WHERE email = ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, associationName);
            stmt.setString(2, currentUserEmail);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            LOGGER.severe("Failed to check leader status: " + e.getMessage());
            showAlert("Error", "Failed to check leader status: " + e.getMessage());
            return false;
        }
    }

    private void clearForm() {
        editingEvent = null;
        titleField.clear();
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
        startTimeField.clear();
        endTimeField.clear();
        locationField.clear();
        descriptionField.clear();
        associationComboBox.setValue(associationName != null ? associationName : null);
        createEventBtn.setText("Create Event");
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}