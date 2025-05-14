package com.example.associations_universitaires_javafx;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Announcement {
    private int id;
    private String title;
    private String content;
    private int authorId;
    private String authorName;
    private LocalDateTime datePosted;

    public Announcement(int id, String title, String content, int authorId, String authorName, LocalDateTime datePosted) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.authorId = authorId;
        this.authorName = authorName;
        this.datePosted = datePosted;
    }

    public Announcement(String title, String content, int authorId, String authorName, LocalDateTime datePosted) {
        this(0, title, content, authorId, authorName, datePosted);
    }

    public String getFormattedDate() {
        return datePosted.format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"));
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public int getAuthorId() { return authorId; }
    public String getAuthorName() { return authorName; }
    public LocalDateTime getDatePosted() { return datePosted; }

    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
    public void setAuthorId(int authorId) { this.authorId = authorId; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
}