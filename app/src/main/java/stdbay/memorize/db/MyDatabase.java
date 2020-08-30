package stdbay.memorize.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MyDatabase extends SQLiteOpenHelper {
    private static  final String CREATE_SUBJECT_TABLE="create table subject(" +//科目表
            "name varchar not null,"+
            "id int primary key autoincrement," +
            "fatherId int," +
            "foreign key(fatherId)references subject(id))";
    private static final String CREATE_PROBLEMSET_TABLE="create table problemSet(" +//习题集表
            "name varchar not null," +
            "id int primary key autoincrement," +
            "subId int not null," +
            "fatherId int," +
            "createTime text," +//创建时间
            "viewTimes int," +//查看次数
            "grade real," +//得分
            "totalGrade real," +//总分
            "FOREIGN KEY(subId)REFERENCES subject(id)," +
            "foreign key(fatherId)references problemSet(id))";

    private static final String CREATE_PROBLEM_TABLE="create table problem(" +//习题表
            "name varchar not null," +
            "id int primary key autoincrement," +
            "probSetId int not null," +
            "subId int not null," +
            "number int," +
            "createTime text," +
            "summary text," +
            "viewTimes int," +
            "grade real," +
            "totalGrade real," +
            "foreign key(subId)references subject(id)," +
            "foreign key(probSetId)references problemSet(id))";

    private static final String CREATE_PROB_PIC_TABLE="create table prob_pic(" +//习题_图片表
            "probId int," +
            "picPosition text," +//图片保存地址
            "foreign key(probId)references problem(id))";

    private static final String CREATE_KNOWLEDGE_TABLE="create table knowledge(" +//知识点表
            "id int primary key autoincrement," +
            "name varchar," +
            "subId int," +
            "annotation text," +//注解
            "foreign key(subId) references subject(id))";

    private static final String CREATE_KNOW_LEVEL_TABLE="create table knowLevel(" +//知识点层次表
            "fatherId int," +
            "sonId int," +
            "foreign key (fatherId) references knowledge(id)," +
            "foreign key (sonId) references knowledge(id))";

    MyDatabase(Context context, String name, SQLiteDatabase.CursorFactory factory, int version){
        super(context,name,factory,version);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
            // Enable foreign key constraints 开启外键约束
            db.execSQL("PRAGMA foreign_keys=ON;");
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_SUBJECT_TABLE);
        db.execSQL(CREATE_PROBLEMSET_TABLE);
        db.execSQL(CREATE_PROBLEM_TABLE);
        db.execSQL(CREATE_PROB_PIC_TABLE);
        db.execSQL(CREATE_KNOWLEDGE_TABLE);
        db.execSQL(CREATE_KNOW_LEVEL_TABLE);
//        db.execSQL("insert into subject (name)values(`数学`)");
//        db.execSQL("insert into subject (name)values(`英语`)");
//        db.execSQL("insert into subject (name)values(`政治`)");
//        db.execSQL("insert into subject (name)values(`专业课`)");
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {

    }

}
