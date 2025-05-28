package com.example.associations_universitaires_javafx;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HomeControllerTest {

    private HomeController homeController;

    @BeforeAll
    static void initJFX() {
        // Initialize JavaFX toolkit
        Platform.startup(() -> {});
    }

    @BeforeEach
    void setUp() throws Exception {
        homeController = new HomeController();
        // Initialize FXML fields using reflection
        setField("welcomeLabel", new Label());
        setField("addAsociatieItem", new MenuItem());
        setField("addAnnouncementItem", new MenuItem());
        setField("manageEventsItem", new MenuItem());
        setField("viewApprovedEventsItem", new MenuItem());
        setField("manageEventStatusItem", new MenuItem());
        setField("profAllocationItem", new MenuItem());
        setField("administrativeMenuBtn", new MenuButton());
        // Set default role to null
        setField("currentUserRole", null);
    }

    @Test
    void testConfigureMenuForUserRole_AdminRole() throws Exception {
        // Arrange
        setField("currentUserRole", "admin");

        // Act
        invokeConfigureMenuForUserRole();

        // Assert
        assertTrue(getFieldValue("administrativeMenuBtn", MenuButton.class).isVisible(), "Administrative menu should be visible for admin");
        assertTrue(getFieldValue("profAllocationItem", MenuItem.class).isVisible(), "Professor allocation item should be visible for admin");
        assertTrue(getFieldValue("addAsociatieItem", MenuItem.class).isVisible(), "Add association item should be visible for admin");
        assertTrue(getFieldValue("addAnnouncementItem", MenuItem.class).isVisible(), "Add announcement item should be visible for admin");
        assertTrue(getFieldValue("manageEventsItem", MenuItem.class).isVisible(), "Manage events item should be visible for admin");
        assertTrue(getFieldValue("manageEventStatusItem", MenuItem.class).isVisible(), "Manage event status item should be visible for admin");
        assertTrue(getFieldValue("viewApprovedEventsItem", MenuItem.class).isVisible(), "View approved events item should be visible");
    }

    @Test
    void testConfigureMenuForUserRole_UserRole() throws Exception {
        // Arrange
        setField("currentUserRole", "user");

        // Act
        invokeConfigureMenuForUserRole();

        // Assert
        assertFalse(getFieldValue("administrativeMenuBtn", MenuButton.class).isVisible(), "Administrative menu should be hidden for user");
        assertFalse(getFieldValue("profAllocationItem", MenuItem.class).isVisible(), "Professor allocation item should be hidden for user");
        assertFalse(getFieldValue("addAsociatieItem", MenuItem.class).isVisible(), "Add association item should be hidden for user");
        assertFalse(getFieldValue("addAnnouncementItem", MenuItem.class).isVisible(), "Add announcement item should be hidden for user");
        assertFalse(getFieldValue("manageEventsItem", MenuItem.class).isVisible(), "Manage events item should be hidden for user");
        assertFalse(getFieldValue("manageEventStatusItem", MenuItem.class).isVisible(), "Manage event status item should be hidden for user");
        assertTrue(getFieldValue("viewApprovedEventsItem", MenuItem.class).isVisible(), "View approved events item should be visible");
    }

    private void invokeConfigureMenuForUserRole() throws Exception {
        Method method = HomeController.class.getDeclaredMethod("configureMenuForUserRole");
        method.setAccessible(true);
        method.invoke(homeController);
        method.setAccessible(false);
    }

    private void setField(String fieldName, Object value) {
        try {
            Field field = HomeController.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(homeController, value);
            field.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }

    private <T> T getFieldValue(String fieldName, Class<T> type) {
        try {
            Field field = HomeController.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            T value = type.cast(field.get(homeController));
            field.setAccessible(false);
            return value;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to get field: " + fieldName, e);
        }
    }
}