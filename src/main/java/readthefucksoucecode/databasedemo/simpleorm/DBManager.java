package readthefucksoucecode.databasedemo.simpleorm;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * from http://www.codeceo.com/article/write-own-android-database.html#comments
 * https://github.com/xujinping/DatabaseDemo/blob/master/app/src/main/java/com/xjp/databasedemo/DBManager.java#L115
 * http://www.codeceo.com/article/java-regular-usage.html
 * Created by Administrator on 2016/4/25.
 */
public class DBManager {
    private static final String TAG = DBManager.class.getSimpleName();
    public static Context mContext;
    private final String db_name;
    SQLiteDatabase db;
    MySQLiteHelper mHelper;

    public DBManager(Context context, String db_name, int db_version, Class<?> clazz) {
        mHelper = new MySQLiteHelper(context, db_name, db_version,clazz);
        db = mHelper.getWritableDatabase();
        this.mContext = context;
        this.db_name = db_name;
    }

    /**
     * 关闭数据库
     */
    public void closeDataBase() {
        db.close();
        mHelper = null;
        db = null;
    }


    /**
     * 得到建立表的语句
     *
     * @param clazz
     * @return
     */
    private String getCreateTableSql(Class<?> clazz) {
        StringBuilder sb = new StringBuilder();
        //讲类名作为表名
        String tabName = DBUtils.getTableName(clazz);
        sb.append("create table ").append(tabName)
                .append("( " +
                        "id integer primary key autoincrement ,");

        //得到类中的所有属性对象数组
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);//可以访问类的私有属性

            String fieldName = field.getName();
            String fieldType = field.getType().getName();

            if (fieldName.equalsIgnoreCase("_id") || fieldName.equalsIgnoreCase("id")) {
                continue;
            } else {
                //eg :name text ,
                sb.append(fieldName).append(DBUtils.getColumnType(fieldType)).append(", ");
            }
            int len = sb.length();
            sb.replace(len - 2, len, ")");
            Log.d(TAG, "the result is " + sb.toString());

        }
        return sb.toString();
    }

    /**
     * 利用反射拼接insert sql
     * 实现对数据库插入操作 SQLite提供了如下方法
     * <p/>
     * public long insert(String table, String nullColumnHack, ContentValues values)
     * 可以看到，第一个参数是table 表示表名，第二个参数通常用不到，传入null即可，第三个参数将数据以 ContentValues键值对的形式存储。比如我们在数据库中插入一条人Person的信息代码如下：
     * <p/>
     * public void insert(Person person){
     * ContentValues values = new ContentValues();
     * values.put("name",person.getName());
     * values.put("age",person.getAge());
     * values.put("flag",person.getFlag());
     * db.insert("Person",null,values);
     * }
     *
     * @param obj 插入数据库的数据 eg:Person
     * @return
     */
    public long insert(Object obj) {
        Class<?> modeClass = obj.getClass();
        Field[] fields = modeClass.getDeclaredFields();

        ContentValues values = new ContentValues();

        for (Field field : fields) {
            field.setAccessible(true);
            String fieldName = field.getName();//age
            //剔除主键id值得保存，由于框架默认设置id为主键自动增长
            if (fieldName.equalsIgnoreCase("id") || fieldName.equalsIgnoreCase("_id")) {
                continue;
            }
            putValues(values, field, obj);
        }
        return db.insert(DBUtils.getTableName(modeClass), null, values);
    }

    /**
     * 查询数据库中所有的数据
     * eg: List<Person> list = dbManager.findAll(Person.class);
     * @param clazz
     * @param <T>   以 List的形式返回数据库中所有数据
     * @return 返回list集合
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws NoSuchMethodException
     **/
    public <T> List<T> findAll(Class<T> clazz) {
        Cursor cursor = db.query(clazz.getSimpleName(), null, null, null, null, null, null);
        return getEntity(cursor, clazz);
    }

    /**
     * 根据指定条件返回满足条件的记录
     *
     * @param clazz      类
     * @param select     条件语句 ：（"id>？"）
     * @param selectArgs 条件(new String[]{"0"}) 查询id=0的记录
     * @param <T>        类型
     * @return 返回满足条件的list集合
     */
    public <T> List<T> findByArgs(Class<T> clazz, String select, String[] selectArgs) {
        Cursor cursor = db.query(clazz.getSimpleName(), null, select, selectArgs, null, null, null);
        return getEntity(cursor, clazz);
    }

    /**
     * 通过id查找制定数据
     *
     * @param clazz 指定类
     * @param id    条件id
     * @param <T>   类型
     * @return 返回满足条件的对象
     */
    public <T> T findById(Class<T> clazz, int id) {
        Cursor cursor = db.query(clazz.getSimpleName(), null, "id=" + id, null, null, null, null);
        List<T> list = getEntity(cursor, clazz);
        return list.get(0);
    }

    /**
     * 删除记录一条记录
     *
     * @param clazz 需要删除的类名
     * @param id    需要删除的 id索引
     */
    public void deleteById(Class<?> clazz, long id) {
        db.delete(DBUtils.getTableName(clazz), "id=" + id, null);
    }


    /**
     * 删除数据库中指定的表
     *
     * @param clazz
     */
    public void deleteTable(Class<?> clazz) {
        db.execSQL("DROP TABLE IF EXISTS" + DBUtils.getTableName(clazz));
    }

    /**
     * 更新一条记录
     *
     * @param clazz  类
     * @param values 更新对象
     * @param id     更新id索引
     */
    public void updateById(Class<?> clazz, ContentValues values, long id) {
        db.update(clazz.getSimpleName(), values, "id=" + id, null);
    }
    /**
     * 查询
     * Cursor cursor = db.query("Person", null, "age = ?", new String[]{"18"}, null, null, null);
     * List<Person> list = new ArrayList<>();
     * if (cursor != null && cursor.moveToFirst()) {
     * do {
     * Person person = new Person();
     * int id = cursor.getInt(cursor.getColumnIndex("id"));
     * String name = cursor.getString(cursor.getColumnIndex("name"));
     * String age = cursor.getString(cursor.getColumnIndex("age"));
     * boolean flag = cursor.getInt(cursor.getColumnIndex("flag")) == 1 ? true : false;
     * person.setId(id);
     * person.setName(name);
     * person.setAge(age);
     * person.setFlag(flag);
     * list.add(person);
     * } while (cursor.moveToNext());
     * }
     * 从数据库得到实体类
     */
    private <T> List<T> getEntity(Cursor cursor, Class<T> clazz) {
        List<T> list = new ArrayList<>();
        try {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Field[] fields = clazz.getDeclaredFields();

                    T beaninstance = clazz.newInstance();//eg:Person person = new Person();
                    for (Field field : fields) {
                        Class<?> cursorClass = cursor.getClass();
                        //int id = cursor.getInt(cursor.getColumnIndex("id"));
                        String columnMethodName = getColumnMethodName(field.getType());//getInt() ...
                        //cursor.getInt  getString ?  这边为什么用int  也可能是getString 明白了  参数二是
                        Method cursorMethod = cursorClass.getMethod(columnMethodName, int.class);
//                        cursor.getString(int columnIndex)

                        //获取数据库中每列的值
                        Object value = cursorMethod.invoke(cursor, cursor.getColumnIndex(field.getName()));//int id = cursor.getInt(cursor.getColumnIndex("id"));

                        //将数据库中数据转换成bean中数据
                        if (field.getType() == boolean.class || field.getType() == Boolean.class) {
                            if ("0".equals(String.valueOf(value))) {
                                value = false;
                            } else if ("1".equals(String.valueOf(value))) {
                                value = true;
                            }
                        } else if (field.getType() == char.class || field.getType() == Character.class) {
                            value = ((String) value).charAt(0);
                        } else if (field.getType() == Date.class) {
                            long date = (Long) value;
                            if (date <= 0) {
                                value = null;
                            } else {
                                value = new Date(date);
                            }
                        }

                        //person.setId(id);
                        String methodName = makeSetterMethodName(field);
                        Method method = clazz.getDeclaredMethod(methodName, field.getType());
                        method.invoke(beaninstance, value);
                    }
                    list.add(beaninstance);

                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return list;
    }

    private boolean isPrimitiveBooleanType(Field field) {
        Class<?> fieldType = field.getType();
        if ("boolean".equals(fieldType.getName())) {
            return true;
        }
        return false;
    }



    private String makeSetterMethodName(Field field) {
       String setMethodName;
        String setMethodPrefix="set";
        // ^匹配行首   . 表示任意字符。* 表示0或多次  $ 匹配行尾
        if(isPrimitiveBooleanType(field) && field.getName().matches("^is[A-Z]{1}.*$")){
            //
            setMethodName=setMethodPrefix+field.getName().substring(2);
        }else if(field.getName().matches("^[a-z]{1}[A-Z]{1}.*")){
            setMethodName=setMethodPrefix+field.getName();
        }else{
            setMethodName = setMethodPrefix + DBUtils.capitalize(field.getName());
        }
        return setMethodName;
    }


    private String getColumnMethodName(Class<?> fieldType) {
        String typeName;
        if (fieldType.isPrimitive()) {
            typeName = DBUtils.capitalize(fieldType.getName());
        } else {
            typeName = fieldType.getSimpleName();
        }
        String methodName = "get" + typeName;
        if ("getBoolean".equals(methodName)) {
            methodName = "getInt";
        } else if ("getChar".equals(methodName) || "getCharacter".equals(methodName)) {
            methodName = "getString";
        } else if ("getDate".equals(methodName)) {
            methodName = "getLong";
        } else if ("getInteger".equals(methodName)) {
            methodName = "getInt";
        }
        return methodName;
    }

    /**
     * 完成 values.put("key",value);
     *
     * @param values
     * @param field
     * @param obj
     */
    private void putValues(ContentValues values, Field field, Object obj) {
        try {
            //不能这么作 不知道put的 field.get(obj) 类型
            //field.get(obj) Returns the value of the field in the specified object.
            // values.put(field.getName(),field.get(obj));
            Class<?> class_ContentValues = values.getClass();
            //获得  属性名称和 属性值
            Object[] parameters = new Object[]{field.getName(), field.get(obj)};
            Class<?>[] parameterTypes = getParameterTypes(field, field.get(obj), parameters);

            //ContentValues.put("","..");
            Method method = class_ContentValues.getDeclaredMethod("put", parameterTypes);
            method.setAccessible(true);
            method.invoke(values, parameters);
        } catch (Exception e) {
            e.printStackTrace();
        }
        /**
         Class<?> clazz = values.getClass();
         try {
         Object[] parameters = new Object[]{fd.getName(), fd.get(obj)};
         Class<?>[] parameterTypes = getParameterTypes(fd, fd.get(obj), parameters);
         Method method = clazz.getDeclaredMethod("put", parameterTypes);
         method.setAccessible(true);
         method.invoke(values, parameters);
         } catch (NoSuchMethodException e) {
         e.printStackTrace();
         } catch (InvocationTargetException e) {
         e.printStackTrace();
         } catch (IllegalAccessException e) {
         e.printStackTrace();
         }
         */

    }

    private Class<?>[] getParameterTypes(Field field, Object fieldValue, Object[] parameters) {
        Class<?>[] parameterTypes;
        if (isCharType(field)) {
            //属性值
            parameters[1] = String.valueOf(fieldValue);
            parameterTypes = new Class[]{String.class, String.class};
        } else {
            if (field.getType().isPrimitive()) {
                parameterTypes = new Class[]{String.class, getObjectType(field.getType())};
            } else if ("java.util.Date".equals(field.getType().getName())) {
                parameterTypes = new Class[]{String.class, Long.class};
            } else {
                parameterTypes = new Class[]{String.class, field.getType()};
            }

        }
        return parameterTypes;
    }


    /**
     * 是否是字符串类型
     *
     * @param field
     * @return
     */
    private boolean isCharType(Field field) {
        String type = field.getType().getName();
        return type.equalsIgnoreCase("char") || type.equalsIgnoreCase("character");
    }

    /**
     * 得到对象的类型
     *
     * @param primitiveType
     * @return
     */
    private Class<?> getObjectType(Class<?> primitiveType) {
        if (primitiveType != null) {
            if (primitiveType.isPrimitive()) {
                String basicTypeName = primitiveType.getName();
                if ("int".equals(basicTypeName)) {
                    return Integer.class;
                } else if ("short".equals(basicTypeName)) {
                    return Short.class;
                } else if ("long".equals(basicTypeName)) {
                    return Long.class;
                } else if ("float".equals(basicTypeName)) {
                    return Float.class;
                } else if ("double".equals(basicTypeName)) {
                    return Double.class;
                } else if ("boolean".equals(basicTypeName)) {
                    return Boolean.class;
                } else if ("char".equals(basicTypeName)) {
                    return Character.class;
                }
            }
        }
        return null;
    }
}
