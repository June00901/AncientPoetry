package ee.example.ancient;

import java.util.List;

//功能：定义一个数据模型类，用于承载古诗的详细信息。
//主要功能：
//包含属性如 id、name、title、content、pic、poetInfo 和 translation，用于存储古诗的详细信息。
//提供 getter 和 setter 方法，方便访问和修改这些属性。

public class PlaceBean {

    private String id;
    private String name;

    private String title;
    private String content;
    private String pic;
    private String poetInfo;
    private String translation;
    private String theme;

    public PlaceBean() {
    }

    public PlaceBean(String id, String name, String title, String content, String pic) {
        this.id = id;
        this.name = name;
        this.title = title;
        this.content = content;
        this.pic = pic;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getPic() {
        return pic;
    }

    public void setPic(String pic) {
        this.pic = pic;
    }

    public String getPoetInfo() {
        return poetInfo;
    }

    public void setPoetInfo(String poetInfo) {
        this.poetInfo = poetInfo;
    }

    public String getTranslation() {
        return translation;
    }

    public void setTranslation(String translation) {
        this.translation = translation;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }
}
