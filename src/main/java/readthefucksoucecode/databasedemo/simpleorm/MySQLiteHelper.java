package readthefucksoucecode.databasedemo.simpleorm;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.lang.reflect.Field;

/**
public long insert(Object obj)；插入数据
public List findAll(Class clazz)；查询所有数据
public List findByArgs(Class clazz, String select, String[] selectArgs) ；根据指定条件查询满足条件数据
public T findById(Class clazz, int id)；根据id查询一条记录
public void deleteById(Class)
 Created by Administrator on 2016/4/21.
 */
public class MySQLiteHelper extends SQLiteOpenHelper {
    private static final String TAG = "MySqLiteHelper";

    private Class mClazz;

    public MySQLiteHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    public MySQLiteHelper(Context context, String db_name, int db_version, Class clazz) {
        this(context, db_name, null, db_version);
        this.mClazz = clazz;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS" + DBUtils.getTableName(mClazz));
        createTable(db);
    }

    /**
     * 根据制定类名创建表
     */
    private void createTable(SQLiteDatabase db) {
        db.execSQL(getCreateTableSql(mClazz));
    }

    /**
     * 得到建表语句
     *
     * @param clazz 指定类
     * @return sql语句
     */
    private String getCreateTableSql(Class<?> clazz) {
        StringBuilder sb = new StringBuilder();
        String tabName = DBUtils.getTableName(clazz);
        sb.append("create table ").append(tabName).append(" (id  INTEGER PRIMARY KEY AUTOINCREMENT, ");
        Field[] fields = clazz.getDeclaredFields();
        for (Field fd : fields) {
            String fieldName = fd.getName();
            String fieldType = fd.getType().getName();
            if (fieldName.equalsIgnoreCase("_id") || fieldName.equalsIgnoreCase("id")) {
                continue;
            } else {
                sb.append(fieldName).append(DBUtils.getColumnType(fieldType)).append(", ");
            }
        }
        int len = sb.length();
        sb.replace(len - 2, len, ")");
        Log.d(TAG, "the result is " + sb.toString());
        return sb.toString();
    }

}
