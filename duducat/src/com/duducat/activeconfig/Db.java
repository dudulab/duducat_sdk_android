package com.duducat.activeconfig;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class Db extends SQLiteOpenHelper {
	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "duducat";
	private static final String TABLE_NAME = "ActiveConfig";

	private static final String TABLE_CREATE = "CREATE TABLE " + TABLE_NAME
			+ "(" + "ID INTEGER PRIMARY KEY AUTOINCREMENT," + "Key TEXT,"
			+ "Type INTEGER," + "Value TEXT," 
			+ "MD5 TEXT," + "Status TEXT," + "EndTime INTEGER,"
			+ "UNIQUE(Key,Type))";

	Db(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(TABLE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE " + TABLE_NAME);
		db.execSQL(TABLE_CREATE);
	}
	
	public void clear(){
		getReadableDatabase().execSQL("DROP TABLE " + TABLE_NAME);
		getReadableDatabase().execSQL(TABLE_CREATE);
	}

	void save(ConfigItem model) {
		String sql = "REPLACE INTO ActiveConfig (Key, Type, Value, MD5, Status, EndTime) VALUES (?,?,?,?,?,?)";
		getWritableDatabase().execSQL(
				sql,
				new Object[] { model.key, model.type, model.value, 
						model.md5, model.status, model.expireTime, });
	}

	ConfigItem get(String key, int type) {
		ConfigItem model = null;
		String sql = "SELECT Key, Type, Value, MD5, Status, EndTime FROM ActiveConfig WHERE Key = ? AND Type = ?";
		Cursor cursor = getReadableDatabase().rawQuery(sql,
				new String[] { key, String.valueOf(type) });
		if (cursor.getCount() == 1) {
			cursor.moveToFirst();
			model = new ConfigItem();
			model.key = cursor.getString(0);
			model.type = cursor.getInt(1);
			model.value = cursor.getString(2);
			model.md5 = cursor.getString(3);
			model.status = cursor.getString(4);
			model.expireTime = new Date(cursor.getInt(5) * 1000);
		} else {
			Logger.i(String.format("Query data error, key = %s, count = %d",
					key, cursor.getCount()));
		}
		return model;
	}

	List<String> getAllKeys() {
		String sql = "SELECT Key,MD5,Type FROM " + TABLE_NAME;
		Cursor cursor = getReadableDatabase().rawQuery(sql, null);
		List<String> values = new ArrayList<String>();
		while (cursor.moveToNext()) {
			String key = cursor.getString(0);
			String md5 = cursor.getString(1);
			int type = cursor.getInt(2);
			if (key != null) {
				values.add(key);
				values.add(",");
				values.add(md5);
				values.add(",");
				values.add(String.valueOf(type));
				values.add(";");
			}
		}
		if (values.size() > 0) {
			// remove the last semicolon
			values.remove(values.size() - 1);
		}
		return values;
	}
}