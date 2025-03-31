// src/main/java/com/example/associations_universitaires_javafx/models/Announcement.java
package com.example.associations_universitaires_javafx;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Announcement {
    private String title;
    private String content;
    private String author;
    private LocalDateTime datePosted;

    public Announcement(String title, String content, String author, LocalDateTime datePosted) {
        this.title = title;
        this.content = content;
        this.author = author;
        this.datePosted = datePosted;
    }

    public Announcement(String title, String content, String author, String dateString) {
        this(title, content, author, LocalDateTime.parse(dateString));
    }

    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getAuthor() { return author; }
    public LocalDateTime getDatePosted() { return datePosted; }

    public String getFormattedDate() {
        return datePosted.format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"));
    }

    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
}