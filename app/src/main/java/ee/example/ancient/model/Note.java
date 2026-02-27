package ee.example.ancient.model;

public class Note {
    private int id;
    private int userId;
    private String title;
    private String content;
    private String poetryContent;
    private String poetryTranslation;
    private String poetInfo;
    private String theme;
    private String createdAt;

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getPoetryContent() {
        return poetryContent;
    }

    public void setPoetryContent(String poetryContent) {
        this.poetryContent = poetryContent;
    }

    public String getPoetryTranslation() {
        return poetryTranslation;
    }

    public void setPoetryTranslation(String poetryTranslation) {
        this.poetryTranslation = poetryTranslation;
    }

    public String getPoetInfo() {
        return poetInfo;
    }

    public void setPoetInfo(String poetInfo) {
        this.poetInfo = poetInfo;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
} 