package edu.buffalo.cse.cse486586.groupmessenger;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class GroupMessengerDataBase extends SQLiteOpenHelper  
{

	private static final String DATABASE_NAME = "groupMessenger.db";
	private static final int DATABASE_VERSION = 1;
	
	private static final String TABLE_NAME = "provider";
	private static final String COLUMN_KEY = "key";
	private static final String COLUMN_VALUE = "value";
	
	private static final String TABLE_CREATION_STRING = "create table " + TABLE_NAME
														+ "("+ COLUMN_KEY + " text primary key,"
														+ COLUMN_VALUE +" text);" ;
	
	public GroupMessengerDataBase(Context context) 
	{
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase database) 
	{
		database.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
		database.execSQL(TABLE_CREATION_STRING);		
	}

	@Override
	public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) 
	{
		Log.v(GroupMessengerDataBase.class.getName(),
		      "Upgrading database from version " + oldVersion + " to "
		      + newVersion + ", which will destroy all old data");
		
		//database.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
		onCreate(database);
	}

}
