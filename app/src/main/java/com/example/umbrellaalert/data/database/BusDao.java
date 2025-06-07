package com.example.umbrellaalert.data.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.umbrellaalert.data.model.RegisteredBus;

import java.util.ArrayList;
import java.util.List;

/**
 * 등록된 버스 정보 데이터 액세스 객체
 */
public class BusDao {
    
    private static final String TAG = "BusDao";
    private final DatabaseHelper dbHelper;

    public BusDao(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    /**
     * 버스 등록
     */
    public long insertRegisteredBus(RegisteredBus bus) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(DatabaseHelper.COLUMN_NODE_ID, bus.getNodeId());
        values.put(DatabaseHelper.COLUMN_NODE_NAME, bus.getNodeName());
        values.put(DatabaseHelper.COLUMN_ROUTE_ID, bus.getRouteId());
        values.put(DatabaseHelper.COLUMN_ROUTE_NO, bus.getRouteNo());
        values.put(DatabaseHelper.COLUMN_ROUTE_TYPE, bus.getRouteType());
        values.put(DatabaseHelper.COLUMN_DIRECTION_NAME, bus.getDirectionName());
        values.put(DatabaseHelper.COLUMN_CITY_CODE, bus.getCityCode());
        values.put(DatabaseHelper.COLUMN_CREATED_AT, bus.getCreatedAt());
        values.put(DatabaseHelper.COLUMN_IS_ACTIVE, bus.isActive() ? 1 : 0);
        values.put(DatabaseHelper.COLUMN_ALIAS, bus.getAlias());
        
        long id = db.insert(DatabaseHelper.TABLE_REGISTERED_BUS, null, values);
        Log.d(TAG, "버스 등록 완료: ID=" + id + ", 노선=" + bus.getRouteNo());
        
        return id;
    }

    /**
     * 모든 등록된 버스 조회
     */
    public List<RegisteredBus> getAllRegisteredBuses() {
        List<RegisteredBus> buses = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        String query = "SELECT * FROM " + DatabaseHelper.TABLE_REGISTERED_BUS + 
                      " WHERE " + DatabaseHelper.COLUMN_IS_ACTIVE + " = 1" +
                      " ORDER BY " + DatabaseHelper.COLUMN_CREATED_AT + " DESC";
        
        Cursor cursor = db.rawQuery(query, null);
        
        if (cursor.moveToFirst()) {
            do {
                RegisteredBus bus = createRegisteredBusFromCursor(cursor);
                if (bus != null) {
                    buses.add(bus);
                }
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        Log.d(TAG, "등록된 버스 조회 완료: " + buses.size() + "개");
        
        return buses;
    }

    /**
     * 특정 버스 조회 (ID로)
     */
    public RegisteredBus getRegisteredBusById(int id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        String selection = DatabaseHelper.COLUMN_BUS_ID + " = ?";
        String[] selectionArgs = {String.valueOf(id)};
        
        Cursor cursor = db.query(
            DatabaseHelper.TABLE_REGISTERED_BUS,
            null,
            selection,
            selectionArgs,
            null,
            null,
            null
        );
        
        RegisteredBus bus = null;
        if (cursor.moveToFirst()) {
            bus = createRegisteredBusFromCursor(cursor);
        }
        
        cursor.close();
        return bus;
    }

    /**
     * 버스 정보 업데이트
     */
    public int updateRegisteredBus(RegisteredBus bus) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(DatabaseHelper.COLUMN_NODE_NAME, bus.getNodeName());
        values.put(DatabaseHelper.COLUMN_ROUTE_TYPE, bus.getRouteType());
        values.put(DatabaseHelper.COLUMN_DIRECTION_NAME, bus.getDirectionName());
        values.put(DatabaseHelper.COLUMN_IS_ACTIVE, bus.isActive() ? 1 : 0);
        values.put(DatabaseHelper.COLUMN_ALIAS, bus.getAlias());
        
        String whereClause = DatabaseHelper.COLUMN_BUS_ID + " = ?";
        String[] whereArgs = {String.valueOf(bus.getId())};
        
        int rowsAffected = db.update(DatabaseHelper.TABLE_REGISTERED_BUS, values, whereClause, whereArgs);
        Log.d(TAG, "버스 정보 업데이트 완료: ID=" + bus.getId() + ", 영향받은 행=" + rowsAffected);
        
        return rowsAffected;
    }

    /**
     * 버스 삭제 (비활성화)
     */
    public int deleteRegisteredBus(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_IS_ACTIVE, 0);
        
        String whereClause = DatabaseHelper.COLUMN_BUS_ID + " = ?";
        String[] whereArgs = {String.valueOf(id)};
        
        int rowsAffected = db.update(DatabaseHelper.TABLE_REGISTERED_BUS, values, whereClause, whereArgs);
        Log.d(TAG, "버스 삭제(비활성화) 완료: ID=" + id + ", 영향받은 행=" + rowsAffected);
        
        return rowsAffected;
    }

    /**
     * 중복 버스 확인
     */
    public boolean isDuplicateBus(String nodeId, String routeId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        String selection = DatabaseHelper.COLUMN_NODE_ID + " = ? AND " +
                          DatabaseHelper.COLUMN_ROUTE_ID + " = ? AND " +
                          DatabaseHelper.COLUMN_IS_ACTIVE + " = 1";
        String[] selectionArgs = {nodeId, routeId};
        
        Cursor cursor = db.query(
            DatabaseHelper.TABLE_REGISTERED_BUS,
            new String[]{DatabaseHelper.COLUMN_BUS_ID},
            selection,
            selectionArgs,
            null,
            null,
            null
        );
        
        boolean isDuplicate = cursor.getCount() > 0;
        cursor.close();
        
        return isDuplicate;
    }

    /**
     * 커서에서 RegisteredBus 객체 생성
     */
    private RegisteredBus createRegisteredBusFromCursor(Cursor cursor) {
        try {
            RegisteredBus bus = new RegisteredBus();
            
            bus.setId(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_BUS_ID)));
            bus.setNodeId(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NODE_ID)));
            bus.setNodeName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NODE_NAME)));
            bus.setRouteId(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ROUTE_ID)));
            bus.setRouteNo(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ROUTE_NO)));
            bus.setRouteType(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ROUTE_TYPE)));
            bus.setDirectionName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DIRECTION_NAME)));
            bus.setCityCode(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CITY_CODE)));
            bus.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CREATED_AT)));
            bus.setActive(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_IS_ACTIVE)) == 1);
            bus.setAlias(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ALIAS)));
            
            return bus;
        } catch (Exception e) {
            Log.e(TAG, "RegisteredBus 객체 생성 실패", e);
            return null;
        }
    }

    /**
     * 등록된 버스 개수 조회
     */
    public int getRegisteredBusCount() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        String query = "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_REGISTERED_BUS + 
                      " WHERE " + DatabaseHelper.COLUMN_IS_ACTIVE + " = 1";
        
        Cursor cursor = db.rawQuery(query, null);
        int count = 0;
        
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        
        cursor.close();
        return count;
    }
}
