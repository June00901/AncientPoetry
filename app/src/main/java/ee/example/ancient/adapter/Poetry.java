package ee.example.ancient.adapter;

//功能：定义古诗词的数据模型。
//主要功能：
//包含古诗词的属性，如 id、title、content、theme 和 time。
//提供构造函数和 getter/setter 方法，方便创建和访问古诗词对象的属性。

public class Poetry {
    private Long id;
    private String title;
    private String content;
    private String theme;
    private String time;

    // 构造函数
    public Poetry() {
    }

    public Poetry(Long id, String title, String content, String theme, String time) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.theme = theme;
        this.time = time;
    }

    // Getter 和 Setter 方法
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
