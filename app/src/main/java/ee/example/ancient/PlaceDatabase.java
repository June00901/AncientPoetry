package ee.example.ancient;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import ee.example.ancient.model.Collection;
import ee.example.ancient.model.Note;

//功能：管理与古诗相关的 SQLite 数据库。
//主要功能：
//创建和管理古诗数据表，支持插入、查询和删除操作。
//提供方法来添加默认古诗数据，支持根据主题或ID查询古诗。

//发现页面 - 数据库
public class PlaceDatabase extends SQLiteOpenHelper {

    public static final String POETRY_TABLE = "tb_plave";
    public static final String COLLECTIONS_TABLE = "collections";
    public static final String USERS_TABLE = "users";
    public static final String NOTES_TABLE = "notes";
    private static final int DATABASE_VERSION = 4; // 升级到版本4以修复表结构

    public PlaceDatabase(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 创建用户表
        db.execSQL("CREATE TABLE IF NOT EXISTS " + USERS_TABLE +
                " (id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT, " +
                "password TEXT)");

        // 添加默认用户（用户名：1，密码：1）
        ContentValues values = new ContentValues();
        values.put("username", "1");
        values.put("password", "1");
        db.insert(USERS_TABLE, null, values);

        // 创建收藏表
        db.execSQL("CREATE TABLE IF NOT EXISTS " + COLLECTIONS_TABLE +
                " (id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER, " +
                "poetry_id TEXT, " +
                "title TEXT, " +
                "content TEXT)");

        // 创建笔记表（确保所有列都存在）
        String createNotesTable = "CREATE TABLE IF NOT EXISTS " + NOTES_TABLE + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "user_id INTEGER,"
                + "title TEXT,"
                + "content TEXT,"
                + "poetry_content TEXT,"
                + "poetry_translation TEXT,"
                + "poet_info TEXT,"
                + "theme TEXT,"
                + "created_at DATETIME DEFAULT CURRENT_TIMESTAMP,"
                + "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP"
                + ")";
        db.execSQL(createNotesTable);

        // 创建诗词表
        db.execSQL("CREATE TABLE IF NOT EXISTS " + POETRY_TABLE + " (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT," +
                "title TEXT," +
                "content TEXT," +
                "pic TEXT," +
                "theme TEXT," +
                "translation TEXT," +
                "poet_info TEXT" +
                ")");

        addDefaultPoetry(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d("PlaceDatabase", "升级数据库从版本 " + oldVersion + " 到 " + newVersion);

        // 逐版本升级
        if (oldVersion < 3) {
            upgradeToVersion3(db);
        }

        if (oldVersion < 4) {
            upgradeToVersion4(db);
        }
    }

    /**
     * 升级到版本3：修复笔记表结构
     */
    private void upgradeToVersion3(SQLiteDatabase db) {
        try {
            // 检查notes表是否存在
            if (isTableExists(db, NOTES_TABLE)) {
                // 备份旧数据
                db.execSQL("ALTER TABLE " + NOTES_TABLE + " RENAME TO notes_backup");

                // 创建新表
                String createNotesTable = "CREATE TABLE " + NOTES_TABLE + " ("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + "user_id INTEGER,"
                        + "title TEXT,"
                        + "content TEXT,"
                        + "poetry_content TEXT,"
                        + "poetry_translation TEXT,"
                        + "poet_info TEXT,"
                        + "theme TEXT,"
                        + "created_at DATETIME DEFAULT CURRENT_TIMESTAMP,"
                        + "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP"
                        + ")";
                db.execSQL(createNotesTable);

                // 恢复数据（只恢复存在的列）
                try {
                    db.execSQL("INSERT INTO " + NOTES_TABLE
                            + " (id, user_id, title, content, poetry_content, poetry_translation, poet_info, theme, created_at, updated_at)"
                            + " SELECT id, user_id, title, content, "
                            + "COALESCE(poetry_content, ''), "
                            + "COALESCE(poetry_translation, ''), "
                            + "COALESCE(poet_info, ''), "
                            + "COALESCE(theme, ''), "
                            + "COALESCE(created_at, CURRENT_TIMESTAMP), "
                            + "COALESCE(updated_at, CURRENT_TIMESTAMP)"
                            + " FROM notes_backup");
                } catch (Exception e) {
                    Log.e("PlaceDatabase", "恢复数据失败", e);
                }

                // 删除备份表
                db.execSQL("DROP TABLE IF EXISTS notes_backup");
            }
        } catch (Exception e) {
            Log.e("PlaceDatabase", "升级到版本3失败", e);
        }
    }

    /**
     * 升级到版本4：确保所有必要的列都存在
     */
    private void upgradeToVersion4(SQLiteDatabase db) {
        try {
            // 检查并添加可能缺失的列
            addColumnIfNotExists(db, NOTES_TABLE, "poetry_content", "TEXT DEFAULT ''");
            addColumnIfNotExists(db, NOTES_TABLE, "poetry_translation", "TEXT DEFAULT ''");
            addColumnIfNotExists(db, NOTES_TABLE, "poet_info", "TEXT DEFAULT ''");
            addColumnIfNotExists(db, NOTES_TABLE, "theme", "TEXT DEFAULT ''");
            addColumnIfNotExists(db, NOTES_TABLE, "created_at", "DATETIME DEFAULT CURRENT_TIMESTAMP");
            addColumnIfNotExists(db, NOTES_TABLE, "updated_at", "DATETIME DEFAULT CURRENT_TIMESTAMP");

            Log.d("PlaceDatabase", "升级到版本4完成");
        } catch (Exception e) {
            Log.e("PlaceDatabase", "升级到版本4失败", e);
        }
    }

    /**
     * 检查表是否存在
     */
    private boolean isTableExists(SQLiteDatabase db, String tableName) {
        Cursor cursor = db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                new String[]{tableName});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    /**
     * 安全地添加列（如果不存在）
     */
    private void addColumnIfNotExists(SQLiteDatabase db, String tableName, String columnName, String columnType) {
        try {
            // 检查列是否存在
            Cursor cursor = db.rawQuery("PRAGMA table_info(" + tableName + ")", null);
            boolean columnExists = false;

            while (cursor.moveToNext()) {
                String name = cursor.getString(1);
                if (name.equals(columnName)) {
                    columnExists = true;
                    break;
                }
            }
            cursor.close();

            // 如果列不存在，添加它
            if (!columnExists) {
                db.execSQL("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnType);
                Log.d("PlaceDatabase", "添加列 " + columnName + " 到表 " + tableName);
            }
        } catch (Exception e) {
            Log.e("PlaceDatabase", "添加列 " + columnName + " 失败", e);
        }
    }

    private void addDefaultPoetry(SQLiteDatabase db) {
        ContentValues values = new ContentValues();

        // 添加第一首诗
        values.put("name", "《江南春》");
        values.put("title", "《江南春》·【唐】杜牧");
        values.put("content", "千里莺啼绿映红，\n水村山郭酒旗风。\n南朝四百八十寺，\n多少楼台烟雨中。\n");
        values.put("pic", "p1");
        values.put("theme", "其他");
        values.put("translation", "辽阔的江南，到处莺歌燕舞，绿树红花相映，水边村寨山麓城郭处处酒旗飘动。南朝遗留下的许多座古寺，如今有多少笼罩在这蒙胧烟雨之中。");
        values.put("poet_info", "杜牧(公元803-约852年),字牧之,号樊川居士,汉族,京兆万年(今陕西西安)人,唐代诗人。");
        db.insert(POETRY_TABLE, null, values);

        // 添加第二首诗
        values.clear();
        values.put("name", "《登高》");
        values.put("title", "《登高》·【唐】杜甫");
        values.put("content", "风急天高猿啸哀，\n渚清沙白鸟飞回。\n无边落木萧萧下，\n不尽长江滚滚来。\n");
        values.put("pic", "p2");
        values.put("theme", "山水");
        values.put("translation", "秋风劲急，天空高远，猿声凄厉。江渚清澈，沙洲洁白，鸟儿在空中盘旋。无边的落叶在萧萧飘落，滚滚长江水不停地奔流而来。");
        values.put("poet_info", "杜甫(712年-770年),字子美,自号少陵野老,世称杜工部、杜少陵等,汉族,河南巩县(今河南省巩义市)人,唐代伟大的现实主义诗人。");
        db.insert(POETRY_TABLE, null, values);

        // 添加第三首诗
        values.clear();
        values.put("name", "《望岳》");
        values.put("title", "《望岳》·【唐】杜甫");
        values.put("content", "岱宗夫如何？\n齐鲁青未了。\n造化钟神秀，\n阴阳割昏晓。\n");
        values.put("pic", "p11");
        values.put("theme", "山水");
        values.put("translation", "泰山是一座怎样的山？它横跨齐鲁之地，青色连绵不绝。它钟灵毓秀，天地造化，山势雄伟，日夜分明。");
        values.put("poet_info", "杜甫(712年-770年),字子美,自号少陵野老,世称杜工部、杜少陵等,汉族,河南巩县(今河南省巩义市)人,唐代伟大的现实主义诗人。");
        db.insert(POETRY_TABLE, null, values);

        // 添加第四首诗
        values.clear();
        values.put("name", "《春望》");
        values.put("title", "《春望》·【唐】杜甫");
        values.put("content", "国破山河在，\n城春草木深。\n感时花溅泪，\n恨别鸟惊心。\n");
        values.put("pic", "p6");
        values.put("theme", "忧国");
        values.put("translation", "国家虽然破败，山河依旧存在，城中春意浓郁，草木茂盛深邃。感伤时事，花开时也洒下泪水，怨恨分离，鸟鸣声也令人心惊。");
        values.put("poet_info", "杜甫(712年-770年),字子美,自号少陵野老,世称杜工部、杜少陵等,汉族,河南巩县(今河南省巩义市)人,唐代伟大的现实主义诗人。");
        db.insert(POETRY_TABLE, null, values);

        // 添加第五首诗
        values.clear();
        values.put("name", "《赠汪伦》");
        values.put("title", "《赠汪伦》·【唐】李白");
        values.put("content", "李白乘舟将欲行，\n忽闻岸上踏歌声。\n桃花潭水深千尺，\n不及汪伦送我情。\n");
        values.put("pic", "p3");
        values.put("theme", "友情");
        values.put("translation", "我李白正要乘船离开，忽然听见岸上有人踏歌而来。桃花潭水虽然深达千尺，也比不上汪伦送别我的深情厚谊。");
        values.put("poet_info", "李白(701年-762年),字太白,号青莲居士,唐代伟大的浪漫主义诗人,被后人誉为诗仙。");
        db.insert(POETRY_TABLE, null, values);

        // 添加第六首诗
        values.clear();
        values.put("name", "《早发白帝城》");
        values.put("title", "《早发白帝城》·【唐】李白");
        values.put("content", "朝辞白帝彩云间，\n千里江陵一日还。\n两岸猿声啼不住，\n轻舟已过万重山。\n");
        values.put("pic", "p15");
        values.put("theme", "山水");
        values.put("translation", "清晨告别云雾缭绕的白帝城，千里之遥的江陵一天就能到达。两岸的猿声不住地啼叫，小船已经驶过了万重青山。");
        values.put("poet_info", "李白(701年-762年),字太白,号青莲居士,唐代伟大的浪漫主义诗人,被后人誉为诗仙。");
        db.insert(POETRY_TABLE, null, values);

        // 添加第七首诗
        values.clear();
        values.put("name", "《望天门山》");
        values.put("title", "《望天门山》·【唐】李白");
        values.put("content", "天门中断楚江开，\n碧水东流至此回。\n两岸青山相对出，\n孤帆一片日边来。\n");
        values.put("pic", "p4");
        values.put("theme", "山水");
        values.put("translation", "天门山中间断开，楚江从中间流过。碧绿的江水自东流来到这里转弯。两岸青山相对挺立，一叶孤帆从夕阳下驶来。");
        values.put("poet_info", "李白(701年-762年),字太白,号青莲居士,唐代伟大的浪漫主义诗人,被后人誉为诗仙。");
        db.insert(POETRY_TABLE, null, values);

        // 添加第八首诗
        values.clear();
        values.put("name", "《送友人》");
        values.put("title", "《送友人》·【唐】李白");
        values.put("content", "青山横北郭，\n白水绕东城。\n此地一为别，\n孤蓬万里征。\n");
        values.put("pic", "p8");
        values.put("theme", "离别");
        values.put("translation", "青山横亘在北郊，白水环绕着东城。在这里一别之后，你将像孤蓬一样飘泊万里。");
        values.put("poet_info", "李白(701年-762年),字太白,号青莲居士,唐代伟大的浪漫主义诗人,被后人誉为诗仙。");
        db.insert(POETRY_TABLE, null, values);

        // 添加第九首诗
        values.clear();
        values.put("name", "《黄鹤楼送孟浩然之广陵》");
        values.put("title", "《黄鹤楼送孟浩然之广陵》·【唐】李白");
        values.put("content", "故人西辞黄鹤楼，\n烟花三月下扬州。\n孤帆远影碧空尽，\n唯见长江天际流。\n");
        values.put("pic", "p9");
        values.put("theme", "离别");
        values.put("translation", "老朋友在黄鹤楼与我告别西行，在烟雨迷蒙的暮春三月去扬州。孤帆远去在碧空尽头消失，只见长江水向天际奔流。");
        values.put("poet_info", "李白(701年-762年),字太白,号青莲居士,唐代伟大的浪漫主义诗人,被后人誉为诗仙。");
        db.insert(POETRY_TABLE, null, values);

        // 添加第十首诗
        values.clear();
        values.put("name", "《静夜思》");
        values.put("title", "《静夜思》·【唐】李白");
        values.put("content", "床前明月光，\n疑是地上霜。\n举头望明月，\n低头思故乡。\n");
        values.put("pic", "p10");
        values.put("theme", "思乡");
        values.put("translation", "床前明亮的月光，好像地上的霜。抬头看看这明亮的月亮，低下头来想起故乡。");
        values.put("poet_info", "李白(701年-762年),字太白,号青莲居士,唐代伟大的浪漫主义诗人,被后人誉为诗仙。");
        db.insert(POETRY_TABLE, null, values);
    }

    // 搜索诗词
    public List<PlaceBean> find(String key) {
        List<PlaceBean> list = new ArrayList<>();
        SQLiteDatabase database = this.getReadableDatabase();

        String selection = key.isEmpty() ? null : "name LIKE ? OR title LIKE ? OR content LIKE ?";
        String[] selectionArgs = key.isEmpty() ? null : new String[]{"%" + key + "%", "%" + key + "%", "%" + key + "%"};

        try {
            Cursor cursor = database.query(POETRY_TABLE, null, selection, selectionArgs, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    PlaceBean data = new PlaceBean();

                    // 安全地获取列索引
                    int idIndex = cursor.getColumnIndex("_id");
                    int nameIndex = cursor.getColumnIndex("name");
                    int titleIndex = cursor.getColumnIndex("title");
                    int contentIndex = cursor.getColumnIndex("content");
                    int picIndex = cursor.getColumnIndex("pic");
                    int themeIndex = cursor.getColumnIndex("theme");
                    int translationIndex = cursor.getColumnIndex("translation");
                    int poetInfoIndex = cursor.getColumnIndex("poet_info");

                    // 只有在索引有效时才获取值
                    if (idIndex >= 0) data.setId(cursor.getString(idIndex));
                    if (nameIndex >= 0) data.setName(cursor.getString(nameIndex));
                    if (titleIndex >= 0) data.setTitle(cursor.getString(titleIndex));
                    if (contentIndex >= 0) data.setContent(cursor.getString(contentIndex));
                    if (picIndex >= 0) data.setPic(cursor.getString(picIndex));
                    if (themeIndex >= 0) data.setTheme(cursor.getString(themeIndex));
                    if (translationIndex >= 0) data.setTranslation(cursor.getString(translationIndex));
                    if (poetInfoIndex >= 0) data.setPoetInfo(cursor.getString(poetInfoIndex));

                    list.add(data);
                } while (cursor.moveToNext());
            }
            if (cursor != null) {
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<PlaceBean> findById(String key) {
        List<PlaceBean> list = new ArrayList<>();
        SQLiteDatabase database = this.getWritableDatabase();
        Cursor cursor = database.query(POETRY_TABLE, null, "_id like ?", new String[]{key}, null, null, null);

        while (cursor.moveToNext()) {
            PlaceBean data = new PlaceBean();

            int idIndex = cursor.getColumnIndex("_id");
            int nameIndex = cursor.getColumnIndex("name");
            int titleIndex = cursor.getColumnIndex("title");
            int contentIndex = cursor.getColumnIndex("content");
            int picIndex = cursor.getColumnIndex("pic");
            int poetInfoIndex = cursor.getColumnIndex("poet_info");
            int translationIndex = cursor.getColumnIndex("translation");

            if (idIndex >= 0) data.setId(String.valueOf(cursor.getInt(idIndex)));
            if (nameIndex >= 0) data.setName(cursor.getString(nameIndex));
            if (titleIndex >= 0) data.setTitle(cursor.getString(titleIndex));
            if (contentIndex >= 0) data.setContent(cursor.getString(contentIndex));
            if (picIndex >= 0) data.setPic(cursor.getString(picIndex));
            if (poetInfoIndex >= 0) data.setPoetInfo(cursor.getString(poetInfoIndex));
            if (translationIndex >= 0) data.setTranslation(cursor.getString(translationIndex));

            list.add(data);
        }

        cursor.close();
        return list;
    }

    public List<PlaceBean> findByTheme(String theme) {
        List<PlaceBean> list = new ArrayList<>();
        SQLiteDatabase database = this.getWritableDatabase();

        try {
            Log.d("PlaceDatabase", "Searching for theme: " + theme);

            Cursor cursor = database.query(POETRY_TABLE, null, "theme=?", new String[]{theme}, null, null, null);
            Log.d("PlaceDatabase", "Found " + cursor.getCount() + " poems");

            while (cursor.moveToNext()) {
                PlaceBean data = new PlaceBean();

                int idIndex = cursor.getColumnIndex("_id");
                int nameIndex = cursor.getColumnIndex("name");
                int titleIndex = cursor.getColumnIndex("title");
                int contentIndex = cursor.getColumnIndex("content");
                int picIndex = cursor.getColumnIndex("pic");
                int poetInfoIndex = cursor.getColumnIndex("poet_info");
                int translationIndex = cursor.getColumnIndex("translation");

                if (idIndex >= 0) data.setId(cursor.getString(idIndex));
                if (nameIndex >= 0) data.setName(cursor.getString(nameIndex));
                if (titleIndex >= 0) data.setTitle(cursor.getString(titleIndex));
                if (contentIndex >= 0) data.setContent(cursor.getString(contentIndex));
                if (picIndex >= 0) data.setPic(cursor.getString(picIndex));
                if (poetInfoIndex >= 0) data.setPoetInfo(cursor.getString(poetInfoIndex));
                if (translationIndex >= 0) data.setTranslation(cursor.getString(translationIndex));

                list.add(data);

                Log.d("PlaceDatabase", "Added poem: " + data.getTitle());
            }
            cursor.close();
        } catch (Exception e) {
            Log.e("PlaceDatabase", "Error finding poems by theme", e);
        }

        return list;
    }

    public long addCollection(int userId, String poetryId, String title, String content) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("user_id", userId);
        values.put("poetry_id", poetryId);
        values.put("title", title);
        values.put("content", content);
        return db.insert(COLLECTIONS_TABLE, null, values);
    }

    public List<Collection> getAllCollections(int userId) {
        List<Collection> collections = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(COLLECTIONS_TABLE, null,
                "user_id = ?", new String[]{String.valueOf(userId)},
                null, null, null);

        while (cursor.moveToNext()) {
            Collection collection = new Collection();

            int idIndex = cursor.getColumnIndex("id");
            int userIdIndex = cursor.getColumnIndex("user_id");
            int poetryIdIndex = cursor.getColumnIndex("poetry_id");
            int titleIndex = cursor.getColumnIndex("title");
            int contentIndex = cursor.getColumnIndex("content");

            if (idIndex >= 0) collection.setId(cursor.getInt(idIndex));
            if (userIdIndex >= 0) collection.setUserId(cursor.getInt(userIdIndex));
            if (poetryIdIndex >= 0) collection.setPoetryId(cursor.getString(poetryIdIndex));
            if (titleIndex >= 0) collection.setTitle(cursor.getString(titleIndex));
            if (contentIndex >= 0) collection.setContent(cursor.getString(contentIndex));

            collections.add(collection);
        }
        cursor.close();
        return collections;
    }

    public boolean isCollected(int userId, String poetryId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(COLLECTIONS_TABLE, new String[]{"id"},
                "user_id = ? AND poetry_id = ?",
                new String[]{String.valueOf(userId), poetryId},
                null, null, null);

        boolean isCollected = cursor.getCount() > 0;
        cursor.close();
        return isCollected;
    }

    public int removeCollection(int userId, String poetryId) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(COLLECTIONS_TABLE,
                "user_id = ? AND poetry_id = ?",
                new String[]{String.valueOf(userId), poetryId});
    }

    // 用户注册
    public long registerUser(String username, String password) {
        SQLiteDatabase db = this.getWritableDatabase();

        // 检查用户名是否已存在
        Cursor cursor = db.query(USERS_TABLE, null,
                "username = ?", new String[]{username},
                null, null, null);

        if (cursor.getCount() > 0) {
            cursor.close();
            return -1; // 用户名已存在
        }
        cursor.close();

        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("password", password);
        return db.insert(USERS_TABLE, null, values);
    }

    // 用户登录
    public int loginUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(USERS_TABLE, new String[]{"id"},
                "username = ? AND password = ?",
                new String[]{username, password},
                null, null, null);

        if (cursor.moveToFirst()) {
            int idIndex = cursor.getColumnIndex("id");
            if (idIndex >= 0) {
                int userId = cursor.getInt(idIndex);
                cursor.close();
                return userId;
            }
        }
        cursor.close();
        return -1; // 登录失败
    }

    // 修改密码
    public boolean changePassword(int userId, String oldPassword, String newPassword) {
        SQLiteDatabase db = this.getWritableDatabase();

        // 验证旧密码
        Cursor cursor = db.query(USERS_TABLE, null,
                "id = ? AND password = ?",
                new String[]{String.valueOf(userId), oldPassword},
                null, null, null);

        if (cursor.getCount() == 0) {
            cursor.close();
            return false; // 旧密码错误
        }
        cursor.close();

        // 更新密码
        ContentValues values = new ContentValues();
        values.put("password", newPassword);

        int rows = db.update(USERS_TABLE, values,
                "id = ?", new String[]{String.valueOf(userId)});
        return rows > 0;
    }

    // 添加笔记
    public long addNote(int userId, String title, String content, String poetryId,
                        String poetryContent, String poetryTranslation, String poetInfo, String theme) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        try {
            values.put("user_id", userId);
            values.put("title", title);
            values.put("content", content);
            values.put("poetry_content", poetryContent);
            values.put("poetry_translation", poetryTranslation);
            values.put("poet_info", poetInfo);
            values.put("theme", theme);
            values.put("created_at", System.currentTimeMillis());
            values.put("updated_at", System.currentTimeMillis());

            long result = db.insert(NOTES_TABLE, null, values);
            Log.d("PlaceDatabase", "Note added with id: " + result);
            return result;
        } catch (Exception e) {
            Log.e("PlaceDatabase", "Error adding note: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }

    // 获取用户的所有笔记（完全修复版本）
    public List<Note> getUserNotes(int userId) {
        List<Note> notes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        try {
            String query = "SELECT * FROM " + NOTES_TABLE +
                    " WHERE user_id = ? ORDER BY updated_at DESC";

            Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});
            Log.d("PlaceDatabase", "Found " + cursor.getCount() + " notes");

            while (cursor.moveToNext()) {
                Note note = new Note();

                // 安全地获取每个字段 - 先检查列索引，再获取值
                int idIndex = cursor.getColumnIndex("id");
                if (idIndex >= 0) {
                    note.setId(cursor.getInt(idIndex));
                }

                int userIdIndex = cursor.getColumnIndex("user_id");
                if (userIdIndex >= 0) {
                    note.setUserId(cursor.getInt(userIdIndex));
                }

                int titleIndex = cursor.getColumnIndex("title");
                if (titleIndex >= 0) {
                    note.setTitle(cursor.getString(titleIndex));
                }

                int contentIndex = cursor.getColumnIndex("content");
                if (contentIndex >= 0) {
                    note.setContent(cursor.getString(contentIndex));
                }

                // 可选字段 - 使用try-catch或检查索引
                int poetryContentIndex = cursor.getColumnIndex("poetry_content");
                if (poetryContentIndex >= 0) {
                    note.setPoetryContent(cursor.getString(poetryContentIndex));
                }

                int poetryTranslationIndex = cursor.getColumnIndex("poetry_translation");
                if (poetryTranslationIndex >= 0) {
                    note.setPoetryTranslation(cursor.getString(poetryTranslationIndex));
                }

                int poetInfoIndex = cursor.getColumnIndex("poet_info");
                if (poetInfoIndex >= 0) {
                    note.setPoetInfo(cursor.getString(poetInfoIndex));
                }

                int themeIndex = cursor.getColumnIndex("theme");
                if (themeIndex >= 0) {
                    note.setTheme(cursor.getString(themeIndex));
                }

                int createdAtIndex = cursor.getColumnIndex("created_at");
                if (createdAtIndex >= 0) {
                    note.setCreatedAt(cursor.getString(createdAtIndex));
                }

                notes.add(note);
            }
            cursor.close();
        } catch (Exception e) {
            Log.e("PlaceDatabase", "Error getting notes: " + e.getMessage());
            e.printStackTrace();
        }

        return notes;
    }

    // 删除笔记
    public int deleteNote(int userId, int noteId) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(NOTES_TABLE,
                "id = ? AND user_id = ?",
                new String[]{String.valueOf(noteId), String.valueOf(userId)});
    }

    // 更新笔记
    public int updateNote(int userId, int noteId, String title, String content) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("content", content);
        values.put("updated_at", System.currentTimeMillis());

        try {
            int result = db.update(NOTES_TABLE, values,
                    "id = ? AND user_id = ?",
                    new String[]{String.valueOf(noteId), String.valueOf(userId)});
            Log.d("PlaceDatabase", "Updated note: " + result + " rows affected");
            return result;
        } catch (Exception e) {
            Log.e("PlaceDatabase", "Error updating note: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }

    // 获取诗词总数
    public int getPoetryCount() {
        SQLiteDatabase database = this.getReadableDatabase();
        int count = 0;
        Cursor cursor = null;

        try {
            cursor = database.rawQuery("SELECT COUNT(*) FROM " + POETRY_TABLE, null);
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
        } catch (Exception e) {
            Log.e("PlaceDatabase", "获取诗词数量失败", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return count;
    }
}