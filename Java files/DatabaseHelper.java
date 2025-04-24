package com.gaurav.geomapmarker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "Markers.db";
    private static final int DATABASE_VERSION = 2;

    private static final String TABLE_MARKERS = "Markers";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_LAT = "latitude";
    private static final String COLUMN_LNG = "longitude";
    private static final String COLUMN_CATEGORY = "category";
    private static final String COLUMN_DESCRIPTION = "description";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_MARKERS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_NAME + " TEXT, " +
                COLUMN_LAT + " REAL, " +
                COLUMN_LNG + " REAL, " +
                COLUMN_CATEGORY + " TEXT, " +
                COLUMN_DESCRIPTION + " TEXT)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_MARKERS + " ADD COLUMN " + COLUMN_DESCRIPTION + " TEXT");
        }
    }

    public void addMarker(String name, LatLng latLng, String category, String description) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_LAT, latLng.latitude);
        values.put(COLUMN_LNG, latLng.longitude);
        values.put(COLUMN_CATEGORY, category);
        values.put(COLUMN_DESCRIPTION, description);

        db.insert(TABLE_MARKERS, null, values);
        db.close();
    }

    public List<String[]> getMarkers() {
        List<String[]> markers = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_MARKERS, null, null, null, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME));
                double lat = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LAT));
                double lng = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LNG));
                String category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY));
                String description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION));

                markers.add(new String[]{name, String.valueOf(lat), String.valueOf(lng), category, description});
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return markers;
    }

    public void deleteMarker(String name, LatLng latLng) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_MARKERS, COLUMN_NAME + " = ? AND " + COLUMN_LAT + " = ? AND " + COLUMN_LNG + " = ?",
                new String[]{name, String.valueOf(latLng.latitude), String.valueOf(latLng.longitude)});
        db.close();
    }

    public void updateMarker(String oldName, String newName, LatLng latLng, String category, String description) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, newName);
        values.put(COLUMN_LAT, latLng.latitude);
        values.put(COLUMN_LNG, latLng.longitude);
        values.put(COLUMN_CATEGORY, category);
        values.put(COLUMN_DESCRIPTION, description);

        db.update(TABLE_MARKERS, values, COLUMN_NAME + " = ?", new String[]{oldName});
        db.close();
    }

    public List<String[]> searchMarkers(String query) {
        List<String[]> markers = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_MARKERS, null,
                COLUMN_NAME + " LIKE ?", new String[]{"%" + query + "%"},
                null, null, null);

        if (cursor.moveToFirst()) {
            do {
                String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME));
                double lat = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LAT));
                double lng = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LNG));
                String category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY));
                String description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION));

                markers.add(new String[]{name, String.valueOf(lat), String.valueOf(lng), category, description});
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();

        return markers;
    }

    public List<String> getAllCategories() {
        List<String> categories = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT DISTINCT category FROM " + TABLE_MARKERS, null);
        if (cursor.moveToFirst()) {
            do {
                categories.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return categories;
    }

    public void addCategory(String category) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("category_name", category);
        db.insert("categories", null, values);
        db.close();
    }

    public boolean categoryExists(String category) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM categories WHERE category_name = ?", new String[]{category});
        boolean exists = (cursor.getCount() > 0);
        cursor.close();
        return exists;
    }
}
