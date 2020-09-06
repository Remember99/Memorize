package stdbay.memorize.model;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import stdbay.memorize.db.MemorizeOpenHelper;

public class MemorizeDB {

    private static final String DB_NAME="memorize";
    private static final int VERSION=1;
    private static final int NO_FATHER =0;

    private static MemorizeDB memorizeDB;
    private SQLiteDatabase db;

    private MemorizeDB(Context context){
        MemorizeOpenHelper helper=new MemorizeOpenHelper(context,DB_NAME,null,VERSION);
        db=helper.getWritableDatabase();
    }

    private List<BaseItem>list= new ArrayList<>();

    private Cursor cursor;

    //获取MemorizeDB实例
    public synchronized static MemorizeDB getInstance(Context context){
        if(memorizeDB==null){
            memorizeDB=new MemorizeDB(context);
        }
        return memorizeDB;
    }

    @SuppressLint("Recycle")
    public List<BaseItem>loadData(BaseItem nowItem){
        list.clear();
        if(nowItem==null){
            cursor=db.rawQuery("select*from subject where fatherId is null",null);
            queryFromCursorToList(BaseItem.SUBJECT_TYPE);
        }else
        switch(nowItem.getType()){
            case BaseItem.SUBJECT_TYPE:
                cursor=db.rawQuery("select*from subject where fatherId=?",new String[]{String.valueOf(nowItem.getId())});
                queryFromCursorToList(BaseItem.SUBJECT_TYPE);
                cursor=db.rawQuery("select*from problem_set where fatherId is null and subId=?"
                        ,new String[]{String.valueOf(nowItem.getId())});
                queryFromCursorToList(BaseItem.PROBLEM_SET_TYPE);
                break;
            case BaseItem.PROBLEM_SET_TYPE:
                break;
        }
        cursor.close();
        return list;
    }

    public void addItem(final BaseItem father, final String name, int type, final callBackListener listener){
        switch(type){
            case BaseItem.SUBJECT_TYPE:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if(father==null)
                                db.execSQL("insert into subject (name)values(?)",
                                new String[]{name});
                            else
                                db.execSQL("insert into subject (name,fatherId)values(?,?)",
                                        new String[]{name, String.valueOf(father.getId())});
                            if(listener!=null)
                                listener.onFinished();
                        }catch (SQLException e) {
                            Log.d("error", Objects.requireNonNull(e.getMessage()));
                            listener.onError(e);
                        }
                    }
                }).start();
                break;
            case BaseItem.PROBLEM_SET_TYPE:
                new Thread(new Runnable() {
                    @SuppressLint("Recycle")
                    @Override
                    public void run() {
                        try{
                            if(father.getType()==BaseItem.SUBJECT_TYPE){
                                db.execSQL("insert into problem_set (name,subId,createTime,viewTimes,grade,totalGrade)" +
                                        "values (?,?,(select date('now')),0,0,0)",new String[]{name, String.valueOf(father.getId())});
                            }else if(father.getType()==BaseItem.PROBLEM_SET_TYPE){
                                int subId=0;
                                cursor =db.rawQuery("select*from problem_set where id=?",new String[]{String.valueOf(father.getId())}); //子节点的subId和父节点相同
                                if (cursor.moveToFirst()) {
                                    subId=cursor.getInt(cursor.getColumnIndex("fatherId"));
                                }
                                cursor.close();
                                db.execSQL("insert into problem_set (name,subId,fatherId,createTime,viewTimes,grade,totalGrade)" +
                                        "values(?,?,?,(select date('now')),0,0,0)",
                                        new String[]{name, String.valueOf(father.getId()), String.valueOf(subId)});
                            }
                            if(listener!=null)
                                listener.onFinished();
                        } catch (Exception e) {
                            if(listener!=null)
                                listener.onError(e);
                        }
                    }
                }).start();
        }
    }

    @SuppressLint("Recycle")
    public BaseItem findBackItem(BaseItem nowItem){
        if(nowItem==null)return null;
        switch (nowItem.getType()){
            case BaseItem.SUBJECT_TYPE:
                if(nowItem.getFatherId()==NO_FATHER)return null;
                cursor=db.rawQuery("select*from subject where id=?",new String[]{String.valueOf(nowItem.getFatherId())});
                break;
            case BaseItem.PROBLEM_SET_TYPE:
                if(nowItem.getFatherId()==NO_FATHER){//习题集没有父亲的话,它的上一级就是科目
                    cursor=db.rawQuery("select*from problem_set where id=?",new String[]{String.valueOf(nowItem.getId())});
                    //因为nowitem不含subId字段,因此要先从数据库中查出来
                    if(cursor.moveToFirst()){
                        int subId=cursor.getInt(cursor.getColumnIndex("subId"));
                        cursor= db.rawQuery("select*from subject where id=?",new String[]{String.valueOf(subId)});
                    }

                }else{
                    cursor=db.rawQuery("select*from problem_set where id=?",new String[]{String.valueOf(nowItem.getFatherId())});
                }
                break;
        }
        BaseItem rtn=new BaseItem();
        if(cursor.moveToFirst()){
            rtn.setType(BaseItem.SUBJECT_TYPE);
            rtn.setName(cursor.getString(cursor.getColumnIndex("name")));
            rtn.setFatherId(cursor.getInt(cursor.getColumnIndex("fatherId")));
            rtn.setId(cursor.getInt(cursor.getColumnIndex("id")));
        }
        cursor.close();
        return rtn;

    }


    @SuppressLint("Recycle")
    private void queryFromCursorToList(int type){
        if(cursor.moveToFirst()) do {
            BaseItem item = new BaseItem();
            item.setType(type);
            item.setId(cursor.getInt(cursor.getColumnIndex("id")));
            item.setName(cursor.getString(cursor.getColumnIndex("name")));

            //查询子项
            Cursor csr=null;
            Map<String, Integer>map=new HashMap<>();
            switch(type){
                case BaseItem.SUBJECT_TYPE:
                    csr=db.rawQuery("select id from subject where fatherId=?",new String[]{String.valueOf(item.getId())});
                    csr.moveToFirst();
                    map.put("子科目",csr.getCount());
                    csr=db.rawQuery("select id from problem_set where subId=?",new String[]{String.valueOf(item.getId())});
                    csr.moveToFirst();
                    map.put("子习题集",csr.getCount());
                    break;
                case BaseItem.PROBLEM_SET_TYPE:
                    csr=db.rawQuery("select id from problem_set where fatherId=?",new String[]{String.valueOf(item.getId())});
                    csr.moveToFirst();
                    map.put("子习题集",csr.getCount());
                    csr=db.rawQuery("select id from problem where probSetId=?",new String[]{String.valueOf(item.getId())});
                    csr.moveToFirst();
                    map.put("子习题",csr.getCount());
                    break;
            }
            item.setChildrenData(map);
            list.add(item);
        } while (cursor.moveToNext());
        cursor.close();
    };

    public void deleteItem(BaseItem item,callBackListener listener){
        String sql="delete from ";
        switch(item.getType()){
            case BaseItem.SUBJECT_TYPE:
                sql+="subject";
                break;
            case BaseItem.PROBLEM_SET_TYPE:
                sql+="problem_set";
                break;
        }
        sql+=" where id=?";
        try {
            db.execSQL(sql, new String[]{String.valueOf(item.getId())});
            if(listener!=null)
                listener.onFinished();
        } catch (SQLException e) {
            if(listener!=null)
                listener.onError(e);
        }
    }

    public void reName(BaseItem item,String newName,callBackListener listener){
        try{
            String sql="update ";
            switch(item.getType()){
                case BaseItem.SUBJECT_TYPE:
                    sql+="subject";
                    break;
                case BaseItem.PROBLEM_SET_TYPE:
                    sql+="problem_set";
                    break;
            }
            sql+=" set name=? where id=?";
            db.execSQL(sql,new String[]{newName, String.valueOf(item.getId())});
            if(listener!=null)
                listener.onFinished();
        } catch (SQLException e) {
            if(listener!=null)
                listener.onError(e);
        }

    }

    public interface callBackListener{
        void onFinished();
        void onError(Exception e);
    }
}
