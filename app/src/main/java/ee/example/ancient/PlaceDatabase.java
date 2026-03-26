package ee.example.ancient;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ee.example.ancient.model.Collection;
import ee.example.ancient.model.Note;

//功能：管理与古诗相关的 SQLite 数据库。
//主要功能：
//创建和管理古诗数据表，支持插入、查询和删除操作。
//提供方法来添加默认古诗数据，支持根据主题或ID查询古诗。

//发现页面 - 数据库
public class PlaceDatabase extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "poetry.db";
    private static final String SHIJING_ASSET_FILE = "shijing.json";
    private static final String SOURCE_DEFAULT = "default";
    private static final String SOURCE_SHIJING = "shijing";
    private static final String SOURCE_TANG = "tang";
    private static final String SOURCE_SONG = "song";
    private static final String SOURCE_YUAN = "yuan";
    private static final ExecutorService IMPORT_EXECUTOR = Executors.newSingleThreadExecutor();
    private static volatile boolean IMPORT_SCHEDULED = false;
    public static final String POETRY_TABLE = "tb_plave";
    public static final String COLLECTIONS_TABLE = "collections";
    public static final String USERS_TABLE = "users";
    public static final String NOTES_TABLE = "notes";
    private static final int DATABASE_VERSION = 8; // 升级到版本8，导入唐诗/宋词/元曲资产数据
    private final Context appContext;

    public PlaceDatabase(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, DATABASE_NAME, factory, DATABASE_VERSION);
        appContext = context == null ? null : context.getApplicationContext();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        ensureCoreSchema(db);

        addDefaultPoetry(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d("PlaceDatabase", "升级数据库从版本 " + oldVersion + " 到 " + newVersion);
        ensureCoreSchema(db);

        // 逐版本升级
        if (oldVersion < 3) {
            upgradeToVersion3(db);
        }

        if (oldVersion < 4) {
            upgradeToVersion4(db);
        }

        if (oldVersion < 5) {
            upgradeToVersion5(db);
        }

        if (oldVersion < 6) {
            // 版本6：添加更多分类古诗
            addDefaultPoetry(db);
        }

        if (oldVersion < 7) {
            // 版本7：添加更多占位古诗
            addDefaultPoetry(db);
        }

        // 大体量诗词数据改为后台导入，避免阻塞前台UI
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        ensureCoreSchema(db);
    }

    /**
     * 后台预热导入（诗经/唐诗/宋词/元曲），避免阻塞前台 UI。
     * 进程内只会调度一次。
     */
    public static void preloadPoetryInBackground(Context context) {
        if (context == null) {
            return;
        }
        synchronized (PlaceDatabase.class) {
            if (IMPORT_SCHEDULED) {
                return;
            }
            IMPORT_SCHEDULED = true;
        }

        final Context appCtx = context.getApplicationContext();
        IMPORT_EXECUTOR.execute(() -> {
            PlaceDatabase helper = null;
            SQLiteDatabase db = null;
            try {
                helper = new PlaceDatabase(appCtx, DATABASE_NAME, null, 1);
                db = helper.getWritableDatabase();
                helper.ensureCoreSchema(db);
                helper.importShijingIfNeeded(db);
                helper.importAdditionalPoetryIfNeeded(db);
                Log.d("PlaceDatabase", "后台导入任务执行完成");
            } catch (Exception e) {
                Log.e("PlaceDatabase", "后台导入任务失败", e);
            } finally {
                try {
                    if (db != null && db.isOpen()) {
                        db.close();
                    }
                    if (helper != null) {
                        helper.close();
                    }
                } catch (Exception ignored) {
                }
            }
        });
    }

    /**
     * 核心表结构自愈：即使数据库文件异常存在，也保证关键表可用。
     */
    private void ensureCoreSchema(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + USERS_TABLE +
                " (id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT, " +
                "password TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + COLLECTIONS_TABLE +
                " (id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER, " +
                "poetry_id TEXT, " +
                "title TEXT, " +
                "content TEXT)");

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

        db.execSQL("CREATE TABLE IF NOT EXISTS " + POETRY_TABLE + " (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT," +
                "title TEXT," +
                "content TEXT," +
                "pic TEXT," +
                "theme TEXT," +
                "translation TEXT," +
                "poet_info TEXT," +
                "source TEXT DEFAULT ''" +
                ")");

        Cursor cursor = null;
        try {
            cursor = db.query(USERS_TABLE, new String[]{"id"}, "username=?", new String[]{"1"}, null, null, null);
            if (cursor == null || cursor.getCount() == 0) {
                ContentValues values = new ContentValues();
                values.put("username", "1");
                values.put("password", "1");
                db.insert(USERS_TABLE, null, values);
            }
        } catch (Exception e) {
            Log.e("PlaceDatabase", "初始化默认用户失败", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
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
     * 升级到版本5：诗词表新增source列
     */
    private void upgradeToVersion5(SQLiteDatabase db) {
        try {
            addColumnIfNotExists(db, POETRY_TABLE, "source", "TEXT DEFAULT ''");
            Log.d("PlaceDatabase", "升级到版本5完成");
        } catch (Exception e) {
            Log.e("PlaceDatabase", "升级到版本5失败", e);
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
        // 清空诗词表，确保只添加一次
        db.execSQL("DELETE FROM " + POETRY_TABLE + " WHERE source = '" + SOURCE_DEFAULT + "'");
        
        ContentValues values = new ContentValues();

        // 添加第一首诗
        values.put("name", "《江南春》");
        values.put("title", "《江南春》·【唐】杜牧");
        values.put("content", "千里莺啼绿映红，\n水村山郭酒旗风。\n南朝四百八十寺，\n多少楼台烟雨中。\n");
        values.put("pic", "p1");
        values.put("theme", "其他");
        values.put("translation", "辽阔的江南，到处莺歌燕舞，绿树红花相映，水边村寨山麓城郭处处酒旗飘动。南朝遗留下的许多座古寺，如今有多少笼罩在这蒙胧烟雨之中。");
        values.put("poet_info", "杜牧(公元803-约852年),字牧之,号樊川居士,汉族,京兆万年(今陕西西安)人,唐代诗人。");
        values.put("source", SOURCE_DEFAULT);
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
        values.put("source", SOURCE_DEFAULT);
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
        values.put("source", SOURCE_DEFAULT);
        db.insert(POETRY_TABLE, null, values);

        // 添加第四首诗
        values.clear();
        values.put("name", "《春望》");
        values.put("title", "《春望》·【唐】杜甫");
        values.put("content", "国破山河在，\n城春草木深。\n感时花溅泪，\n恨别鸟惊心。\n");
        values.put("pic", "p6");
        values.put("theme", "爱国");
        values.put("translation", "国家虽然破败，山河依旧存在，城中春意浓郁，草木茂盛深邃。感伤时事，花开时也洒下泪水，怨恨分离，鸟鸣声也令人心惊。");
        values.put("poet_info", "杜甫(712年-770年),字子美,自号少陵野老,世称杜工部、杜少陵等,汉族,河南巩县(今河南省巩义市)人,唐代伟大的现实主义诗人。");
        values.put("source", SOURCE_DEFAULT);
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
        values.put("source", SOURCE_DEFAULT);
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
        values.put("source", SOURCE_DEFAULT);
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
        values.put("source", SOURCE_DEFAULT);
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
        values.put("source", SOURCE_DEFAULT);
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
        values.put("source", SOURCE_DEFAULT);
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
        values.put("source", SOURCE_DEFAULT);
        db.insert(POETRY_TABLE, null, values);

        // ===== 新增：思乡分类古诗 =====
        // 真实古诗1
        values.clear();
        values.put("name", "《九月九日忆山东兄弟》");
        values.put("title", "《九月九日忆山东兄弟》·【唐】王维");
        values.put("content", "独在异乡为异客，\n每逢佳节倍思亲。\n遥知兄弟登高处，\n遍插茱萸少一人。\n");
        values.put("pic", "p12");
        values.put("theme", "思乡");
        values.put("translation", "独自远离家乡难免总有一点凄凉，每到重阳佳节倍加思念远方的亲人。远远想到兄弟们身佩茱萸登上高处，也会因为少我一人而生遗憾之情。");
        values.put("poet_info", "王维(701年-761年),字摩诘,号摩诘居士,汉族,河东蒲州(今山西运城)人,祖籍山西祁县,唐朝诗人,有诗佛之称。");
        values.put("source", SOURCE_DEFAULT);
        db.insert(POETRY_TABLE, null, values);

        // 真实古诗2
        values.clear();
        values.put("name", "《泊船瓜洲》");
        values.put("title", "《泊船瓜洲》·【宋】王安石");
        values.put("content", "京口瓜洲一水间，\n钟山只隔数重山。\n春风又绿江南岸，\n明月何时照我还？\n");
        values.put("pic", "p13");
        values.put("theme", "思乡");
        values.put("translation", "京口和瓜洲不过一水之遥，钟山也只隔着几重青山。温柔的春风又吹绿了大江南岸，天上的明月呀，你什么时候才能够照着我回家呢？");
        values.put("poet_info", "王安石(1021年-1086年),字介甫,号半山,汉族,临川(今江西抚州市临川区)人,北宋著名思想家、政治家、文学家、改革家。");
        values.put("source", SOURCE_DEFAULT);
        db.insert(POETRY_TABLE, null, values);

        // 真实古诗3
        values.clear();
        values.put("name", "《回乡偶书》");
        values.put("title", "《回乡偶书》·【唐】贺知章");
        values.put("content", "少小离家老大回，\n乡音无改鬓毛衰。\n儿童相见不相识，\n笑问客从何处来。\n");
        values.put("pic", "p14");
        values.put("theme", "思乡");
        values.put("translation", "我在年少时离开家乡，到了迟暮之年才回来。我的乡音虽未改变，但鬓角的毛发却已经疏落。儿童们看见我，没有一个认识的。他们笑着询问：这客人是从哪里来的呀？");
        values.put("poet_info", "贺知章(约659年-约744年),字季真,晚年自号四明狂客,汉族,唐代著名诗人、书法家,越州永兴(今浙江萧山)人。");
        values.put("source", SOURCE_DEFAULT);
        db.insert(POETRY_TABLE, null, values);

        // 占位古诗
        values.clear();
        values.put("name", "《望月怀远》");
        values.put("title", "《望月怀远》·【唐】张九龄");
        values.put("content", "海上生明月，\n天涯共此时。\n情人怨遥夜，\n竟夕起相思。\n");
        values.put("pic", "p1");
        values.put("theme", "思乡");
        values.put("translation", "");
        values.put("poet_info", "");
        values.put("source", SOURCE_DEFAULT);
        db.insert(POETRY_TABLE, null, values);

        values.clear();
        values.put("name", "《秋思》");
        values.put("title", "《秋思》·【唐】张籍");
        values.put("content", "洛阳城里见秋风，\n欲作家书意万重。\n复恐匆匆说不尽，\n行人临发又开封。\n");
        values.put("pic", "p2");
        values.put("theme", "思乡");
        values.put("translation", "");
        values.put("poet_info", "");
        values.put("source", SOURCE_DEFAULT);
        db.insert(POETRY_TABLE, null, values);

        // 新增占位古诗
        values.clear();
        values.put("name", "《次北固山下》");
        values.put("title", "《次北固山下》·【唐】王湾");
        values.put("content", "客路青山外，行舟绿水前。\n潮平两岸阔，风正一帆悬。\n海日生残夜，江春入旧年。\n乡书何处达？归雁洛阳边。\n");
        values.put("pic", "p3");
        values.put("theme", "思乡");
        values.put("translation", "");
        values.put("poet_info", "");
        values.put("source", SOURCE_DEFAULT);
        db.insert(POETRY_TABLE, null, values);

        values.clear();
        values.put("name", "《商山早行》");
        values.put("title", "《商山早行》·【唐】温庭筠");
        values.put("content", "晨起动征铎，客行悲故乡。\n鸡声茅店月，人迹板桥霜。\n槲叶落山路，枳花明驿墙。\n因思杜陵梦，凫雁满回塘。\n");
        values.put("pic", "p4");
        values.put("theme", "思乡");
        values.put("translation", "");
        values.put("poet_info", "");
        values.put("source", SOURCE_DEFAULT);
        db.insert(POETRY_TABLE, null, values);

        values.clear();
        values.put("name", "《逢入京使》");
        values.put("title", "《逢入京使》·【唐】岑参");
        values.put("content", "故园东望路漫漫，双袖龙钟泪不干。\n马上相逢无纸笔，凭君传语报平安。\n");
        values.put("pic", "p5");
        values.put("theme", "思乡");
        values.put("translation", "");
        values.put("poet_info", "");
        values.put("source", SOURCE_DEFAULT);
        db.insert(POETRY_TABLE, null, values);

        // ===== 新增：爱国分类古诗 =====
        // 真实古诗1
        values.clear();
        values.put("name", "《过零丁洋》");
        values.put("title", "《过零丁洋》·【宋】文天祥");
        values.put("content", "辛苦遭逢起一经，\n干戈寥落四周星。\n山河破碎风飘絮，\n身世浮沉雨打萍。\n惶恐滩头说惶恐，\n零丁洋里叹零丁。\n人生自古谁无死？\n留取丹心照汗青。\n");
        values.put("pic", "p5");
        values.put("theme", "爱国");
        values.put("translation", "回想我早年由科举入仕历尽辛苦，如今战火消歇已熬过了四个年头。国家危在旦夕恰如狂风中的柳絮，个人又哪堪言说似骤雨里的浮萍。惶恐滩的惨败让我至今依然惶恐，零丁洋身陷元虏可叹我孤苦零丁。人生自古以来有谁能够长生不死？我要留一片爱国的丹心映照史册。");
        values.put("poet_info", "文天祥(1236年-1283年),初名云孙,字宋瑞,又字履善。道号浮休道人、文山。汉族,吉州庐陵(今江西吉安县)人,南宋末大臣,文学家,民族英雄。");
        values.put("source", SOURCE_DEFAULT);
        db.insert(POETRY_TABLE, null, values);

        // 真实古诗2
        values.clear();
        values.put("name", "《满江红》");
        values.put("title", "《满江红》·【宋】岳飞");
        values.put("content", "怒发冲冠，凭栏处、潇潇雨歇。抬望眼、仰天长啸，壮怀激烈。三十功名尘与土，八千里路云和月。莫等闲、白了少年头，空悲切。\n靖康耻，犹未雪。臣子恨，何时灭。驾长车，踏破贺兰山缺。壮志饥餐胡虏肉，笑谈渴饮匈奴血。待从头、收拾旧山河，朝天阙。\n");
        values.put("pic", "p6");
        values.put("theme", "爱国");
        values.put("translation", "我愤怒得头发竖了起来，帽子被顶飞了。独自登高凭栏远眺，骤急的风雨刚刚停歇。抬头远望天空，禁不住仰天长啸，一片报国之心充满心怀。三十多年来虽已建立一些功名，但如同尘土微不足道，南北转战八千里，经过多少风云人生。好男儿，要抓紧时间为国建功立业，不要空空将青春消磨，等年老时徒自悲切。靖康之变的耻辱，至今仍然没有被雪洗。作为国家臣子的愤恨，何时才能泯灭！我要驾着战车向贺兰山进攻，连贺兰山也要踏为平地。我满怀壮志，打仗饿了就吃敌人的肉，谈笑渴了就喝敌人的鲜血。待我重新收复旧日山河，再带着捷报向国家报告胜利的消息！");
        values.put("poet_info", "岳飞(1103年-1142年),字鹏举,宋相州汤阴县(今河南汤阴县)人,抗金名将,中国历史上著名军事家、战略家、书法家、诗人,民族英雄,位列南宋中兴四将之首。");
        values.put("source", SOURCE_DEFAULT);
        db.insert(POETRY_TABLE, null, values);

        // 真实古诗3
        values.clear();
        values.put("name", "《示儿》");
        values.put("title", "《示儿》·【宋】陆游");
        values.put("content", "死去元知万事空，\n但悲不见九州同。\n王师北定中原日，\n家祭无忘告乃翁。\n");
        values.put("pic", "p7");
        values.put("theme", "爱国");
        values.put("translation", "我本来知道，当我死后，人间的一切就都和我无关了；但唯一使我痛心的，就是我没能亲眼看到祖国的统一。因此，当大宋军队收复了中原失地的那一天到来之时，你们举行家祭，千万别忘把这好消息告诉你们的父亲！");
        values.put("poet_info", "陆游(1125年-1210年),字务观,号放翁,汉族,越州山阴(今浙江绍兴)人,南宋文学家、史学家、爱国诗人。");
        values.put("source", SOURCE_DEFAULT);
        db.insert(POETRY_TABLE, null, values);

        // 占位古诗
        values.clear();
        values.put("name", "《正气歌》");
        values.put("title", "《正气歌》·【宋】文天祥");
        values.put("content", "天地有正气，杂然赋流形。下则为河岳，上则为日星。\n于人曰浩然，沛乎塞苍冥。皇路当清夷，含和吐明庭。\n");
        values.put("pic", "p1");
        values.put("theme", "爱国");
        values.put("translation", "");
        values.put("poet_info", "");
        values.put("source", SOURCE_DEFAULT);
        db.insert(POETRY_TABLE, null, values);

        values.clear();
        values.put("name", "《金错刀行》");
        values.put("title", "《金错刀行》·【宋】陆游");
        values.put("content", "楚虽三户能亡秦，岂有堂堂中国空无人。\n王师北定中原日，家祭无忘告乃翁。\n");
        values.put("pic", "p2");
        values.put("theme", "爱国");
        values.put("translation", "");
        values.put("poet_info", "");
        values.put("source", SOURCE_DEFAULT);
        db.insert(POETRY_TABLE, null, values);

        // 新增占位古诗
        values.clear();
        values.put("name", "《满江红·写怀》");
        values.put("title", "《满江红·写怀》·【宋】岳飞");
        values.put("content", "怒发冲冠，凭栏处、潇潇雨歇。抬望眼、仰天长啸，壮怀激烈。三十功名尘与土，八千里路云和月。莫等闲、白了少年头，空悲切。\n靖康耻，犹未雪。臣子恨，何时灭。驾长车，踏破贺兰山缺。壮志饥餐胡虏肉，笑谈渴饮匈奴血。待从头、收拾旧山河，朝天阙。\n");
        values.put("pic", "p3");
        values.put("theme", "爱国");
        values.put("translation", "");
        values.put("poet_info", "");
        values.put("source", SOURCE_DEFAULT);
        db.insert(POETRY_TABLE, null, values);

        values.clear();
        values.put("name", "《小重山·昨夜寒蛩不住鸣》");
        values.put("title", "《小重山·昨夜寒蛩不住鸣》·【宋】岳飞");
        values.put("content", "昨夜寒蛩不住鸣。惊回千里梦，已三更。起来独自绕阶行。人悄悄，帘外月胧明。\n白首为功名。旧山松竹老，阻归程。欲将心事付瑶琴。知音少，弦断有谁听？\n");
        values.put("pic", "p4");
        values.put("theme", "爱国");
        values.put("translation", "");
        values.put("poet_info", "");
        values.put("source", SOURCE_DEFAULT);
        db.insert(POETRY_TABLE, null, values);

        values.clear();
        values.put("name", "《病起书怀》");
        values.put("title", "《病起书怀》·【宋】陆游");
        values.put("content", "病骨支离纱帽宽，孤臣万里客江干。\n位卑未敢忘忧国，事定犹须待阖棺。\n天地神灵扶庙社，京华父老望和銮。\n出师一表通今古，夜半挑灯更细看。\n");
        values.put("pic", "p5");
        values.put("theme", "爱国");
        values.put("translation", "");
        values.put("poet_info", "");
        values.put("source", SOURCE_DEFAULT);
        db.insert(POETRY_TABLE, null, values);

        // ===== 新增：离别分类古诗 =====
        // 真实古诗1
        values.clear();
        values.put("name", "《送元二使安西》");
        values.put("title", "《送元二使安西》·【唐】王维");
        values.put("content", "渭城朝雨浥轻尘，\n客舍青青柳色新。\n劝君更尽一杯酒，\n西出阳关无故人。\n");
        values.put("pic", "p8");
        values.put("theme", "离别");
        values.put("translation", "清晨的微雨湿润了渭城地面的灰尘，馆驿青堂瓦舍柳树的枝叶翠嫩一新。真诚地奉劝我的朋友再干一杯美酒，向西出了阳关就难以遇到故旧亲人。");
        values.put("poet_info", "王维(701年-761年),字摩诘,号摩诘居士,汉族,河东蒲州(今山西运城)人,祖籍山西祁县,唐朝诗人,有诗佛之称。");
        values.put("source", SOURCE_DEFAULT);
        db.insert(POETRY_TABLE, null, values);

        // 真实古诗2
        values.clear();
        values.put("name", "《别董大》");
        values.put("title", "《别董大》·【唐】高适");
        values.put("content", "千里黄云白日曛，\n北风吹雁雪纷纷。\n莫愁前路无知己，\n天下谁人不识君。\n");
        values.put("pic", "p9");
        values.put("theme", "离别");
        values.put("translation", "黄昏的落日使千里浮云变得暗黄；北风劲吹，大雪纷纷，雁儿南飞。不要担心前方的路上没有知己，普天之下还有谁不知道您呢？");
        values.put("poet_info", "高适(704年-765年),字达夫,一字仲武,渤海蓨(今河北景县)人,后迁居宋州宋城(今河南商丘睢阳)。唐代著名的边塞诗人,曾任刑部侍郎、散骑常侍、渤海县候,世称高常侍。");
        values.put("source", SOURCE_DEFAULT);
        db.insert(POETRY_TABLE, null, values);

        // 真实古诗3
        values.clear();
        values.put("name", "《赋得古原草送别》");
        values.put("title", "《赋得古原草送别》·【唐】白居易");
        values.put("content", "离离原上草，一岁一枯荣。\n野火烧不尽，春风吹又生。\n远芳侵古道，晴翠接荒城。\n又送王孙去，萋萋满别情。\n");
        values.put("pic", "p10");
        values.put("theme", "离别");
        values.put("translation", "长长的原上草是多么茂盛，每年秋冬枯黄春来草色浓。无情的野火只能烧掉干叶，春风吹来大地又是绿茸茸。野草野花蔓延着淹没古道，艳阳下草地尽头是你征程。我又一次送走知心的好友，茂密的青草代表我的深情。");
        values.put("poet_info", "白居易(772年-846年),字乐天,号香山居士,又号醉吟先生,祖籍太原,到其曾祖父时迁居下邽,生于河南新郑。是唐代伟大的现实主义诗人,唐代三大诗人之一。");
        values.put("source", SOURCE_DEFAULT);
        db.insert(POETRY_TABLE, null, values);

        // 占位古诗
        values.clear();
        values.put("name", "《送杜少府之任蜀州》");
        values.put("title", "《送杜少府之任蜀州》·【唐】王勃");
        values.put("content", "城阙辅三秦，风烟望五津。\n与君离别意，同是宦游人。\n海内存知己，天涯若比邻。\n无为在歧路，儿女共沾巾。\n");
        values.put("pic", "p1");
        values.put("theme", "离别");
        values.put("translation", "");
        values.put("poet_info", "");
        values.put("source", SOURCE_DEFAULT);
        db.insert(POETRY_TABLE, null, values);

        values.clear();
        values.put("name", "《雨霖铃》");
        values.put("title", "《雨霖铃》·【宋】柳永");
        values.put("content", "寒蝉凄切，对长亭晚，骤雨初歇。都门帐饮无绪，留恋处，兰舟催发。执手相看泪眼，竟无语凝噎。念去去，千里烟波，暮霭沉沉楚天阔。\n");
        values.put("pic", "p2");
        values.put("theme", "离别");
        values.put("translation", "");
        values.put("poet_info", "");
        values.put("source", SOURCE_DEFAULT);
        db.insert(POETRY_TABLE, null, values);

        // 新增占位古诗
        values.clear();
        values.put("name", "《送孟浩然之广陵》");
        values.put("title", "《送孟浩然之广陵》·【唐】李白");
        values.put("content", "故人西辞黄鹤楼，\n烟花三月下扬州。\n孤帆远影碧空尽，\n唯见长江天际流。\n");
        values.put("pic", "p3");
        values.put("theme", "离别");
        values.put("translation", "");
        values.put("poet_info", "");
        values.put("source", SOURCE_DEFAULT);
        db.insert(POETRY_TABLE, null, values);

        values.clear();
        values.put("name", "《送友人》");
        values.put("title", "《送友人》·【唐】李白");
        values.put("content", "青山横北郭，\n白水绕东城。\n此地一为别，\n孤蓬万里征。\n浮云游子意，\n落日故人情。\n挥手自兹去，\n萧萧班马鸣。\n");
        values.put("pic", "p4");
        values.put("theme", "离别");
        values.put("translation", "");
        values.put("poet_info", "");
        values.put("source", SOURCE_DEFAULT);
        db.insert(POETRY_TABLE, null, values);

        values.clear();
        values.put("name", "《别董大》");
        values.put("title", "《别董大》·【唐】高适");
        values.put("content", "千里黄云白日曛，\n北风吹雁雪纷纷。\n莫愁前路无知己，\n天下谁人不识君。\n六翮飘飖私自怜，\n一离京洛十余年。\n丈夫贫贱应未足，\n今日相逢无酒钱。\n");
        values.put("pic", "p5");
        values.put("theme", "离别");
        values.put("translation", "");
        values.put("poet_info", "");
        values.put("source", SOURCE_DEFAULT);
        db.insert(POETRY_TABLE, null, values);

        // ===== 新增：友情分类古诗 =====
        // 真实古诗1
        values.clear();
        values.put("name", "《黄鹤楼送孟浩然之广陵》");
        values.put("title", "《黄鹤楼送孟浩然之广陵》·【唐】李白");
        values.put("content", "故人西辞黄鹤楼，\n烟花三月下扬州。\n孤帆远影碧空尽，\n唯见长江天际流。\n");
        values.put("pic", "p9");
        values.put("theme", "友情");
        values.put("translation", "老朋友在黄鹤楼与我告别西行，在烟雨迷蒙的暮春三月去扬州。孤帆远去在碧空尽头消失，只见长江水向天际奔流。");
        values.put("poet_info", "李白(701年-762年),字太白,号青莲居士,唐代伟大的浪漫主义诗人,被后人誉为诗仙。");
        values.put("source", SOURCE_DEFAULT);
        db.insert(POETRY_TABLE, null, values);

        // 真实古诗2
        values.clear();
        values.put("name", "《别董大》");
        values.put("title", "《别董大》·【唐】高适");
        values.put("content", "千里黄云白日曛，\n北风吹雁雪纷纷。\n莫愁前路无知己，\n天下谁人不识君。\n");
        values.put("pic", "p9");
        values.put("theme", "友情");
        values.put("translation", "黄昏的落日使千里浮云变得暗黄；北风劲吹，大雪纷纷，雁儿南飞。不要担心前方的路上没有知己，普天之下还有谁不知道您呢？");
        values.put("poet_info", "高适(704年-765年),字达夫,一字仲武,渤海蓨(今河北景县)人,后迁居宋州宋城(今河南商丘睢阳)。唐代著名的边塞诗人,曾任刑部侍郎、散骑常侍、渤海县候,世称高常侍。");
        values.put("source", SOURCE_DEFAULT);
        db.insert(POETRY_TABLE, null, values);

        // 真实古诗3
        values.clear();
        values.put("name", "《闻王昌龄左迁龙标遥有此寄》");
        values.put("title", "《闻王昌龄左迁龙标遥有此寄》·【唐】李白");
        values.put("content", "杨花落尽子规啼，\n闻道龙标过五溪。\n我寄愁心与明月，\n随君直到夜郎西。\n");
        values.put("pic", "p11");
        values.put("theme", "友情");
        values.put("translation", "在杨花落完，子规啼鸣的时候，我听说您被贬为龙标尉，龙标地方偏远要经过五溪。我把我忧愁的心思寄托给明月，希望能一直陪着你到夜郎以西。");
        values.put("poet_info", "李白(701年-762年),字太白,号青莲居士,唐代伟大的浪漫主义诗人,被后人誉为诗仙。");
        values.put("source", SOURCE_DEFAULT);
        db.insert(POETRY_TABLE, null, values);

        // 占位古诗
        values.clear();
        values.put("name", "《寄李十二白二十韵》");
        values.put("title", "《寄李十二白二十韵》·【唐】杜甫");
        values.put("content", "笔落惊风雨，诗成泣鬼神。\n声名从此大，汩没一朝伸。\n文彩承殊渥，流传必绝伦。\n\n");
        values.put("pic", "p1");
        values.put("theme", "友情");
        values.put("translation", "");
        values.put("poet_info", "");
        values.put("source", SOURCE_DEFAULT);
        db.insert(POETRY_TABLE, null, values);

        values.clear();
        values.put("name", "《赠孟浩然》");
        values.put("title", "《赠孟浩然》·【唐】李白");
        values.put("content", "吾爱孟夫子，风流天下闻。\n红颜弃轩冕，白首卧松云。\n醉月频中圣，迷花不事君。\n高山安可仰，徒此揖清芬。\n");
        values.put("pic", "p2");
        values.put("theme", "友情");
        values.put("translation", "");
        values.put("poet_info", "");
        values.put("source", SOURCE_DEFAULT);
        db.insert(POETRY_TABLE, null, values);

        // 新增占位古诗
        values.clear();
        values.put("name", "《黄鹤楼送孟浩然之广陵》");
        values.put("title", "《黄鹤楼送孟浩然之广陵》·【唐】李白");
        values.put("content", "故人西辞黄鹤楼，\n烟花三月下扬州。\n孤帆远影碧空尽，\n唯见长江天际流。\n");
        values.put("pic", "p3");
        values.put("theme", "友情");
        values.put("translation", "");
        values.put("poet_info", "");
        values.put("source", SOURCE_DEFAULT);
        db.insert(POETRY_TABLE, null, values);

        values.clear();
        values.put("name", "《别董大》");
        values.put("title", "《别董大》·【唐】高适");
        values.put("content", "千里黄云白日曛，\n北风吹雁雪纷纷。\n莫愁前路无知己，\n天下谁人不识君。\n");
        values.put("pic", "p4");
        values.put("theme", "友情");
        values.put("translation", "");
        values.put("poet_info", "");
        values.put("source", SOURCE_DEFAULT);
        db.insert(POETRY_TABLE, null, values);

        values.clear();
        values.put("name", "《闻王昌龄左迁龙标遥有此寄》");
        values.put("title", "《闻王昌龄左迁龙标遥有此寄》·【唐】李白");
        values.put("content", "杨花落尽子规啼，\n闻道龙标过五溪。\n我寄愁心与明月，\n随君直到夜郎西。\n");
        values.put("pic", "p5");
        values.put("theme", "友情");
        values.put("translation", "");
        values.put("poet_info", "");
        values.put("source", SOURCE_DEFAULT);
        db.insert(POETRY_TABLE, null, values);

        // ===== 新增：励志分类古诗 =====
        // 真实古诗1
        values.clear();
        values.put("name", "《行路难》");
        values.put("title", "《行路难》·【唐】李白");
        values.put("content", "金樽清酒斗十千，玉盘珍羞直万钱。\n停杯投箸不能食，拔剑四顾心茫然。\n欲渡黄河冰塞川，将登太行雪满山。\n闲来垂钓碧溪上，忽复乘舟梦日边。\n行路难！行路难！多歧路，今安在？\n长风破浪会有时，直挂云帆济沧海。\n");
        values.put("pic", "p12");
        values.put("theme", "励志");
        values.put("translation", "金杯里装的名酒，每斗要价十千；玉盘中盛的精美菜肴，收费万钱。胸中郁闷啊，我停杯投箸吃不下；拔剑环顾四周，我心里委实茫然。想渡黄河，冰雪堵塞了这条大川；要登太行，莽莽的风雪早已封山。象吕尚垂钓溪，闲待东山再起；又象伊尹做梦，他乘船经过日边。世上行路呵多么艰难，多么艰难；眼前歧路这么多，我该向北向南？相信总有一天，能乘长风破万里浪；高高挂起云帆，在沧海中勇往直前！");
        values.put("poet_info", "李白(701年-762年),字太白,号青莲居士,唐代伟大的浪漫主义诗人,被后人誉为诗仙。");
        values.put("source", SOURCE_DEFAULT);
        db.insert(POETRY_TABLE, null, values);

        // 真实古诗2
        values.clear();
        values.put("name", "《望岳》");
        values.put("title", "《望岳》·【唐】杜甫");
        values.put("content", "岱宗夫如何？齐鲁青未了。\n造化钟神秀，阴阳割昏晓。\n荡胸生曾云，决眦入归鸟。\n会当凌绝顶，一览众山小。\n");
        values.put("pic", "p11");
        values.put("theme", "励志");
        values.put("translation", "泰山是如此雄伟，青翠的山色望不到边际。大自然在这里凝聚了一切钟灵神秀，山南山北如同被分割为黄昏与白昼。望着山中冉冉升起的云霞，荡涤着我的心灵，极目追踪那暮归的鸟儿隐入了山林。我一定要登上泰山的顶峰，俯瞰那众山，而众山就会显得极为渺小。");
        values.put("poet_info", "杜甫(712年-770年),字子美,自号少陵野老,世称杜工部、杜少陵等,汉族,河南巩县(今河南省巩义市)人,唐代伟大的现实主义诗人。");
        values.put("source", SOURCE_DEFAULT);
        db.insert(POETRY_TABLE, null, values);

        // 真实古诗3
        values.clear();
        values.put("name", "《竹石》");
        values.put("title", "《竹石》·【清】郑燮");
        values.put("content", "咬定青山不放松，\n立根原在破岩中。\n千磨万击还坚劲，\n任尔东西南北风。\n");
        values.put("pic", "p13");
        values.put("theme", "励志");
        values.put("translation", "竹子抓住青山一点也不放松，它的根牢牢地扎在岩石缝中。经历成千上万次的折磨和打击，它依然那么坚强，不管是酷暑的东南风，还是严冬的西北风，它都能经受得住，还会依然坚韧挺拔。");
        values.put("poet_info", "郑燮(1693年-1765年),字克柔,号理庵,又号板桥,人称板桥先生,江苏兴化人,祖籍苏州。清代书画家、文学家。");
        values.put("source", SOURCE_DEFAULT);
        db.insert(POETRY_TABLE, null, values);

        // 占位古诗
        values.clear();
        values.put("name", "《劝学》");
        values.put("title", "《劝学》·【唐】颜真卿");
        values.put("content", "三更灯火五更鸡，\n正是男儿读书时。\n黑发不知勤学早，\n白首方悔读书迟。\n");
        values.put("pic", "p1");
        values.put("theme", "励志");
        values.put("translation", "");
        values.put("poet_info", "");
        values.put("source", SOURCE_DEFAULT);
        db.insert(POETRY_TABLE, null, values);

        values.clear();
        values.put("name", "《浪淘沙》");
        values.put("title", "《浪淘沙》·【唐】刘禹锡");
        values.put("content", "千淘万漉虽辛苦，\n吹尽狂沙始到金。\n");
        values.put("pic", "p2");
        values.put("theme", "励志");
        values.put("translation", "");
        values.put("poet_info", "");
        values.put("source", SOURCE_DEFAULT);
        db.insert(POETRY_TABLE, null, values);

        // 新增占位古诗
        values.clear();
        values.put("name", "《长歌行》");
        values.put("title", "《长歌行》·【汉】乐府诗集");
        values.put("content", "青青园中葵，朝露待日晞。\n阳春布德泽，万物生光辉。\n常恐秋节至，焜黄华叶衰。\n百川东到海，何时复西归？\n少壮不努力，老大徒伤悲。\n");
        values.put("pic", "p3");
        values.put("theme", "励志");
        values.put("translation", "");
        values.put("poet_info", "");
        values.put("source", SOURCE_DEFAULT);
        db.insert(POETRY_TABLE, null, values);

        values.clear();
        values.put("name", "《竹石》");
        values.put("title", "《竹石》·【清】郑燮");
        values.put("content", "咬定青山不放松，\n立根原在破岩中。\n千磨万击还坚劲，\n任尔东西南北风。\n");
        values.put("pic", "p4");
        values.put("theme", "励志");
        values.put("translation", "");
        values.put("poet_info", "");
        values.put("source", SOURCE_DEFAULT);
        db.insert(POETRY_TABLE, null, values);

        values.clear();
        values.put("name", "《石灰吟》");
        values.put("title", "《石灰吟》·【明】于谦");
        values.put("content", "千锤万凿出深山，\n烈火焚烧若等闲。\n粉骨碎身浑不怕，\n要留清白在人间。\n");
        values.put("pic", "p5");
        values.put("theme", "励志");
        values.put("translation", "");
        values.put("poet_info", "");
        values.put("source", SOURCE_DEFAULT);
        db.insert(POETRY_TABLE, null, values);
    }

    /**
     * 幂等导入《诗经》数据：
     * 1) 若已存在source=shijing的数据则跳过
     * 2) 否则从assets/shijing.json导入
     */
    private void importShijingIfNeeded(SQLiteDatabase db) {
        if (!isTableExists(db, POETRY_TABLE)) {
            Log.e("PlaceDatabase", "诗词表不存在，跳过诗经导入");
            return;
        }
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT COUNT(*) FROM " + POETRY_TABLE + " WHERE source = ?", new String[]{SOURCE_SHIJING});
            if (cursor.moveToFirst() && cursor.getInt(0) > 0) {
                return;
            }
        } catch (Exception e) {
            Log.e("PlaceDatabase", "检查诗经导入状态失败", e);
            return;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        importShijingFromAssets(db);
    }

    private void importShijingFromAssets(SQLiteDatabase db) {
        InputStream inputStream = null;
        ByteArrayOutputStream outputStream = null;
        try {
            if (appContext == null) {
                Log.e("PlaceDatabase", "Context为空，无法导入诗经");
                return;
            }

            inputStream = appContext.getAssets().open(SHIJING_ASSET_FILE);
            outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
            String json = outputStream.toString("UTF-8");

            JSONArray array = new JSONArray(json);
            db.beginTransaction();
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.optJSONObject(i);
                if (item == null) {
                    continue;
                }

                String title = item.optString("title", "").trim();
                if (title.isEmpty()) {
                    continue;
                }
                String chapter = item.optString("chapter", "").trim();
                String section = item.optString("section", "").trim();
                JSONArray contentArray = item.optJSONArray("content");
                StringBuilder contentBuilder = new StringBuilder();
                if (contentArray != null) {
                    for (int j = 0; j < contentArray.length(); j++) {
                        if (j > 0) {
                            contentBuilder.append('\n');
                        }
                        contentBuilder.append(contentArray.optString(j, ""));
                    }
                }

                ContentValues values = new ContentValues();
                values.put("name", title);
                values.put("title", "《" + title + "》·《诗经》");
                values.put("content", contentBuilder.toString());
                values.put("pic", "p1");
                values.put("theme", chapter + (section.isEmpty() ? "" : ("-" + section)));
                values.put("translation", "");
                values.put("poet_info", "《诗经》");
                values.put("source", SOURCE_SHIJING);
                db.insert(POETRY_TABLE, null, values);
            }
            db.setTransactionSuccessful();
            Log.d("PlaceDatabase", "诗经导入完成");
        } catch (Exception e) {
            Log.e("PlaceDatabase", "导入诗经失败", e);
        } finally {
            if (db.inTransaction()) {
                db.endTransaction();
            }
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 幂等导入全唐诗/宋词/元曲目录。
     * 通过source字段判断是否已导入，避免重复写入。
     */
    private void importAdditionalPoetryIfNeeded(SQLiteDatabase db) {
        long startTime = System.currentTimeMillis();
        Log.d("PlaceDatabase", "开始检查并导入扩展诗词数据（唐诗/宋词/元曲）");
        importPoetryFromAssetsFolder(db, "poetry/tang", SOURCE_TANG, "唐诗");
        importPoetryFromAssetsFolder(db, "poetry/song", SOURCE_SONG, "宋词");
        importPoetryFromAssetsFolder(db, "poetry/yuan", SOURCE_YUAN, "元曲");
        long cost = System.currentTimeMillis() - startTime;
        Log.d("PlaceDatabase", "扩展诗词导入检查完成，耗时 " + cost + "ms");
    }

    private void importPoetryFromAssetsFolder(SQLiteDatabase db, String folderPath, String source, String theme) {
        if (!isTableExists(db, POETRY_TABLE)) {
            Log.e("PlaceDatabase", "诗词表不存在，跳过导入: " + source);
            return;
        }
        Cursor cursor = null;
        int existingCount = 0;
        try {
            cursor = db.rawQuery("SELECT COUNT(*) FROM " + POETRY_TABLE + " WHERE source = ?", new String[]{source});
            if (cursor.moveToFirst()) {
                existingCount = cursor.getInt(0);
            }
            if (existingCount >= getMinimumImportedCount(source)) {
                return;
            }
        } catch (Exception e) {
            Log.e("PlaceDatabase", "检查 " + source + " 导入状态失败", e);
            return;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        if (appContext == null) {
            return;
        }

        InputStream inputStream = null;
        ByteArrayOutputStream outputStream = null;
        boolean transactionStarted = false;
        long startTime = System.currentTimeMillis();
        int importedCount = 0;
        int processedFiles = 0;
        int jsonFiles = 0;
        try {
            String[] files = appContext.getAssets().list(folderPath);
            if (files == null || files.length == 0) {
                return;
            }
            for (String fileName : files) {
                if (fileName.endsWith(".json")) {
                    jsonFiles++;
                }
            }
            Log.d("PlaceDatabase", "开始导入 " + source + "，目录=" + folderPath + "，json文件数=" + jsonFiles);

            if (existingCount > 0) {
                db.delete(POETRY_TABLE, "source = ?", new String[]{source});
                Log.d("PlaceDatabase", source + " 已有部分数据(" + existingCount + ")，先清理后重导");
            }

            db.beginTransaction();
            transactionStarted = true;

            for (String fileName : files) {
                if (!fileName.endsWith(".json")) {
                    continue;
                }
                processedFiles++;
                String assetPath = folderPath + "/" + fileName;
                try {
                    inputStream = appContext.getAssets().open(assetPath);
                    outputStream = new ByteArrayOutputStream();
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, len);
                    }
                    String json = outputStream.toString("UTF-8").trim();
                    if (json.isEmpty()) {
                        continue;
                    }
                    if (!json.startsWith("[")) {
                        Log.d("PlaceDatabase", "跳过非数组JSON文件: " + assetPath);
                        continue;
                    }
                    JSONArray array = new JSONArray(json);

                    for (int i = 0; i < array.length(); i++) {
                        JSONObject item = array.optJSONObject(i);
                        if (item == null) {
                            continue;
                        }

                        JSONArray paragraphs = item.optJSONArray("paragraphs");
                        if (paragraphs == null || paragraphs.length() == 0) {
                            continue;
                        }

                        String rawTitle = item.optString("title", "").trim();
                        if (rawTitle.isEmpty()) {
                            rawTitle = item.optString("rhythmic", "").trim();
                        }
                        if (rawTitle.isEmpty()) {
                            continue;
                        }

                        String author = item.optString("author", "佚名").trim();
                        if (author.isEmpty()) {
                            author = "佚名";
                        }

                        StringBuilder contentBuilder = new StringBuilder();
                        for (int j = 0; j < paragraphs.length(); j++) {
                            if (j > 0) {
                                contentBuilder.append('\n');
                            }
                            contentBuilder.append(paragraphs.optString(j, ""));
                        }

                        ContentValues values = new ContentValues();
                        values.put("name", rawTitle);
                        values.put("title", "《" + rawTitle + "》·" + author);
                        values.put("content", contentBuilder.toString());
                        values.put("pic", "p1");
                        values.put("theme", theme);
                        values.put("translation", "");
                        values.put("poet_info", author);
                        values.put("source", source);
                        db.insert(POETRY_TABLE, null, values);
                        importedCount++;
                    }
                } catch (Exception fileError) {
                    Log.e("PlaceDatabase", "导入文件失败，已跳过: " + assetPath, fileError);
                } finally {
                    try {
                        if (inputStream != null) {
                            inputStream.close();
                        }
                        if (outputStream != null) {
                            outputStream.close();
                        }
                    } catch (Exception ignored) {
                    }
                    inputStream = null;
                    outputStream = null;
                }

                if (processedFiles % 20 == 0 || processedFiles == jsonFiles) {
                    Log.d("PlaceDatabase", "导入进度 " + source + ": 文件 " + processedFiles + "/" + jsonFiles + "，已写入 " + importedCount + " 条");
                }
            }

            db.setTransactionSuccessful();
            long cost = System.currentTimeMillis() - startTime;
            Log.d("PlaceDatabase", "导入完成: " + source + "，写入 " + importedCount + " 条，耗时 " + cost + "ms");
        } catch (Exception e) {
            Log.e("PlaceDatabase", "导入目录失败: " + folderPath, e);
        } finally {
            if (transactionStarted) {
                db.endTransaction();
            }
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (Exception ignored) {
            }
        }
    }

    private int getMinimumImportedCount(String source) {
        if (SOURCE_TANG.equals(source)) {
            return 30000;
        }
        if (SOURCE_SONG.equals(source)) {
            return 10000;
        }
        if (SOURCE_YUAN.equals(source)) {
            return 1000;
        }
        return 1;
    }

    // 搜索诗词
    public List<PlaceBean> find(String key) {
        return find(key, 0);
    }

    // 搜索诗词（支持limit，limit<=0表示不限制）
    public List<PlaceBean> find(String key, int limit) {
        List<PlaceBean> list = new ArrayList<>();
        String safeKey = key == null ? "" : key.trim();
        String selection = safeKey.isEmpty() ? null : "name LIKE ? OR title LIKE ? OR content LIKE ? OR theme LIKE ? OR poet_info LIKE ?";
        String[] selectionArgs = safeKey.isEmpty()
                ? null
                : new String[]{"%" + safeKey + "%", "%" + safeKey + "%", "%" + safeKey + "%", "%" + safeKey + "%", "%" + safeKey + "%"};
        String limitArg = limit > 0 ? String.valueOf(limit) : null;

        try {
            SQLiteDatabase database = this.getReadableDatabase();
            Cursor cursor = database.query(POETRY_TABLE, null, selection, selectionArgs, null, null, "_id DESC", limitArg);

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

    // 检查是否存在相同标题的笔记
    public boolean isNoteExists(int userId, String title) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            String query = "SELECT * FROM " + NOTES_TABLE + " WHERE user_id = ? AND title = ?";
            cursor = db.rawQuery(query, new String[]{String.valueOf(userId), title});
            return cursor.getCount() > 0;
        } catch (Exception e) {
            Log.e("PlaceDatabase", "Error checking note existence: " + e.getMessage());
            return false;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
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
