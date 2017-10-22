package com.exemple.eac2_2017s1;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by BlueStorm on 14/10/2017.
 */

public class DBInterface {
    public static final String _ID = "_id";
    public static final String TITULO = "titulo";
    public static final String DESCRIPCION = "descripcion";
    public static final String ENLACE = "enlace";
    public static final String AUTOR = "autor";
    public static final String FECHA = "fecha";
    public static final String CATEGORIA = "categoria";
    public static final String IMAGEN = "imagen";

    public static final String DB_NAME = "marca";
    public static final String TABLE_NAME = "noticias";

    public static final String TAG = "DBInterface";
    public static final int VERSIO = 1;

    public static final String DB_CREATE =
            "create table " + TABLE_NAME + "( " + _ID + " integer primary key autoincrement, " +
                    TITULO + ", " + DESCRIPCION + ", " + ENLACE + ", " + AUTOR + ", " + FECHA +
                    ", " + CATEGORIA + ", " + IMAGEN + ");";

    private final Context context;
    private DBHelper dbHelper;
    private SQLiteDatabase db;

    public DBInterface(Context con){
        this.context = con;
        dbHelper = new DBHelper(context);
    }

    //Obre la BD

    public DBInterface open() throws SQLException {
        db = dbHelper.getWritableDatabase();
        return this;
    }

//Tanca la BD

    public void close() {
        dbHelper.close();
    }


    public long insert(String titulo, String descripcion, String enlace, String autor, String fecha, String categoria, String imagen){
        ContentValues initialValues = new ContentValues();
        initialValues.put(TITULO, titulo);
        initialValues.put(DESCRIPCION, descripcion);
        initialValues.put(ENLACE, enlace);
        initialValues.put(AUTOR, autor);
        initialValues.put(FECHA, fecha);
        initialValues.put(CATEGORIA, categoria);
        initialValues.put(IMAGEN, imagen);

        return db.insert(TABLE_NAME ,null, initialValues);
    }

    public Cursor getAll(){
        return db.query(TABLE_NAME, new String[] {TITULO, DESCRIPCION, ENLACE, AUTOR, FECHA, CATEGORIA, IMAGEN}, null, null, null, null, null);
    }

    //Destruye la tabla y la rehace
    public void dropAndRecreateTable(){
        open();
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        db.execSQL(DB_CREATE);
        close();
    }
    private static class DBHelper extends SQLiteOpenHelper {
        DBHelper(Context con) {
            super(con, DB_NAME, null, VERSIO);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            try {
                db.execSQL(DB_CREATE);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int VersioAntiga, int VersioNova) {
            Log.w(TAG, "Actualitzant Base de dades de la versió" + VersioAntiga + " a " + VersioNova + ". Destruirà totes les dades");
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);

            onCreate(db);
        }
    }
}
