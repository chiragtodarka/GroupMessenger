package edu.buffalo.cse.cse486586.groupmessenger;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

public class GroupMessengerProvider extends ContentProvider 
{
	private GroupMessengerDataBase groupMessengerDataBase;
	private static final String TABLE_NAME = "provider";
	private static final String COLUMN_KEY = "key";

	@Override
	public boolean onCreate() 
	{
		groupMessengerDataBase = new GroupMessengerDataBase(getContext());
		return true;
	}
	
	@Override
	public Uri insert(Uri uri, ContentValues values) 
	{
		SQLiteDatabase sqlDB = groupMessengerDataBase.getWritableDatabase();
		//sqlDB.insert(TABLE_NAME, null, values);
		sqlDB.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
		return uri;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) 
	{
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		queryBuilder.setTables(TABLE_NAME);
		SQLiteDatabase db = groupMessengerDataBase.getReadableDatabase();
		Cursor cursor = queryBuilder.query(db, null, TABLE_NAME+"."+COLUMN_KEY+"='"+selection+"'", null, null, null, null);
	    cursor.setNotificationUri(getContext().getContentResolver(), uri);
	    
	    return cursor;
		/*SQLiteDatabase db = groupMessengerDataBase.getReadableDatabase();    // Database is the class that extends   SQLiteHelper
		String[] _projection = {COLUMN_KEY,"value"};
		Cursor cursor = db.query("provider", _projection, COLUMN_KEY+"='"+selection + "'", null, null, null, null);
		return cursor;*/
	}
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

}
