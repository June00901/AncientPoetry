package ee.example.ancient;

//功能：定义一个数据模型类，用于承载古诗的相关信息。
//主要功能：
//包含属性如 id、content、title、theme 和 time，用于存储古诗的基本信息。
//提供 getter 和 setter 方法，方便访问和修改这些属性。

//实体类  数据承载
public class NewsBean {
    private String id;                  //id
    private String content;   //内容
    private String title;       //保存标题
    private String theme;    //主题
    private String time;     //时间
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getTime() {
        return time;
    }
    public void setTime(String time) {
        this.time = time;
    }
    public String getTheme() {
        return theme;
    }
    public void setTheme(String theme) {
        this.theme = theme;
    }
}
