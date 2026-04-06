package com.hotelvision.launcher.data.db.dao;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.hotelvision.launcher.data.db.entities.AppConfigEntity;
import com.hotelvision.launcher.data.db.entities.LauncherRowEntity;
import com.hotelvision.launcher.data.db.entities.MenuItemEntity;
import com.hotelvision.launcher.data.db.entities.RoomInfoEntity;
import com.hotelvision.launcher.data.db.entities.ServiceEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class LauncherDao_Impl implements LauncherDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<RoomInfoEntity> __insertionAdapterOfRoomInfoEntity;

  private final EntityInsertionAdapter<LauncherRowEntity> __insertionAdapterOfLauncherRowEntity;

  private final EntityInsertionAdapter<AppConfigEntity> __insertionAdapterOfAppConfigEntity;

  private final EntityInsertionAdapter<MenuItemEntity> __insertionAdapterOfMenuItemEntity;

  private final EntityInsertionAdapter<ServiceEntity> __insertionAdapterOfServiceEntity;

  public LauncherDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfRoomInfoEntity = new EntityInsertionAdapter<RoomInfoEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `room_info` (`id`,`roomNumber`,`guestName`,`checkoutTime`,`status`) VALUES (?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final RoomInfoEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getRoomNumber());
        if (entity.getGuestName() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getGuestName());
        }
        if (entity.getCheckoutTime() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getCheckoutTime());
        }
        statement.bindString(5, entity.getStatus());
      }
    };
    this.__insertionAdapterOfLauncherRowEntity = new EntityInsertionAdapter<LauncherRowEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `launcher_rows` (`id`,`rowType`,`title`,`titleBn`,`sortOrder`,`isVisible`) VALUES (?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final LauncherRowEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getRowType());
        statement.bindString(3, entity.getTitle());
        if (entity.getTitleBn() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getTitleBn());
        }
        statement.bindLong(5, entity.getSortOrder());
        final int _tmp = entity.isVisible() ? 1 : 0;
        statement.bindLong(6, _tmp);
      }
    };
    this.__insertionAdapterOfAppConfigEntity = new EntityInsertionAdapter<AppConfigEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `apps_config` (`id`,`packageName`,`label`,`iconUrl`,`rowId`,`sortOrder`,`isVisible`) VALUES (?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AppConfigEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getPackageName());
        statement.bindString(3, entity.getLabel());
        if (entity.getIconUrl() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getIconUrl());
        }
        statement.bindLong(5, entity.getRowId());
        statement.bindLong(6, entity.getSortOrder());
        final int _tmp = entity.isVisible() ? 1 : 0;
        statement.bindLong(7, _tmp);
      }
    };
    this.__insertionAdapterOfMenuItemEntity = new EntityInsertionAdapter<MenuItemEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `menu_items` (`id`,`name`,`nameBn`,`price`,`category`,`imageUrl`,`sortOrder`,`isAvailable`) VALUES (?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MenuItemEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        if (entity.getNameBn() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getNameBn());
        }
        statement.bindDouble(4, entity.getPrice());
        statement.bindString(5, entity.getCategory());
        if (entity.getImageUrl() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getImageUrl());
        }
        statement.bindLong(7, entity.getSortOrder());
        final int _tmp = entity.isAvailable() ? 1 : 0;
        statement.bindLong(8, _tmp);
      }
    };
    this.__insertionAdapterOfServiceEntity = new EntityInsertionAdapter<ServiceEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `services` (`id`,`name`,`nameBn`,`icon`) VALUES (?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ServiceEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        if (entity.getNameBn() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getNameBn());
        }
        statement.bindString(4, entity.getIcon());
      }
    };
  }

  @Override
  public Object insertRoomInfo(final RoomInfoEntity roomInfo,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfRoomInfoEntity.insert(roomInfo);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertLauncherRows(final List<LauncherRowEntity> rows,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfLauncherRowEntity.insert(rows);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertApps(final List<AppConfigEntity> apps,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfAppConfigEntity.insert(apps);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertMenuItems(final List<MenuItemEntity> items,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfMenuItemEntity.insert(items);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertServices(final List<ServiceEntity> services,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfServiceEntity.insert(services);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<RoomInfoEntity> getRoomInfoFlow() {
    final String _sql = "SELECT * FROM room_info WHERE id = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"room_info"}, new Callable<RoomInfoEntity>() {
      @Override
      @Nullable
      public RoomInfoEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfRoomNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "roomNumber");
          final int _cursorIndexOfGuestName = CursorUtil.getColumnIndexOrThrow(_cursor, "guestName");
          final int _cursorIndexOfCheckoutTime = CursorUtil.getColumnIndexOrThrow(_cursor, "checkoutTime");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final RoomInfoEntity _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpRoomNumber;
            _tmpRoomNumber = _cursor.getString(_cursorIndexOfRoomNumber);
            final String _tmpGuestName;
            if (_cursor.isNull(_cursorIndexOfGuestName)) {
              _tmpGuestName = null;
            } else {
              _tmpGuestName = _cursor.getString(_cursorIndexOfGuestName);
            }
            final String _tmpCheckoutTime;
            if (_cursor.isNull(_cursorIndexOfCheckoutTime)) {
              _tmpCheckoutTime = null;
            } else {
              _tmpCheckoutTime = _cursor.getString(_cursorIndexOfCheckoutTime);
            }
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            _result = new RoomInfoEntity(_tmpId,_tmpRoomNumber,_tmpGuestName,_tmpCheckoutTime,_tmpStatus);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<LauncherRowEntity>> getLauncherRowsFlow() {
    final String _sql = "SELECT * FROM launcher_rows WHERE isVisible = 1 ORDER BY sortOrder ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"launcher_rows"}, new Callable<List<LauncherRowEntity>>() {
      @Override
      @NonNull
      public List<LauncherRowEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfRowType = CursorUtil.getColumnIndexOrThrow(_cursor, "rowType");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfTitleBn = CursorUtil.getColumnIndexOrThrow(_cursor, "titleBn");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final int _cursorIndexOfIsVisible = CursorUtil.getColumnIndexOrThrow(_cursor, "isVisible");
          final List<LauncherRowEntity> _result = new ArrayList<LauncherRowEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LauncherRowEntity _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpRowType;
            _tmpRowType = _cursor.getString(_cursorIndexOfRowType);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpTitleBn;
            if (_cursor.isNull(_cursorIndexOfTitleBn)) {
              _tmpTitleBn = null;
            } else {
              _tmpTitleBn = _cursor.getString(_cursorIndexOfTitleBn);
            }
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            final boolean _tmpIsVisible;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsVisible);
            _tmpIsVisible = _tmp != 0;
            _item = new LauncherRowEntity(_tmpId,_tmpRowType,_tmpTitle,_tmpTitleBn,_tmpSortOrder,_tmpIsVisible);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<AppConfigEntity>> getAppsForRowFlow(final int rowId) {
    final String _sql = "SELECT * FROM apps_config WHERE rowId = ? AND isVisible = 1 ORDER BY sortOrder ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, rowId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"apps_config"}, new Callable<List<AppConfigEntity>>() {
      @Override
      @NonNull
      public List<AppConfigEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPackageName = CursorUtil.getColumnIndexOrThrow(_cursor, "packageName");
          final int _cursorIndexOfLabel = CursorUtil.getColumnIndexOrThrow(_cursor, "label");
          final int _cursorIndexOfIconUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "iconUrl");
          final int _cursorIndexOfRowId = CursorUtil.getColumnIndexOrThrow(_cursor, "rowId");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final int _cursorIndexOfIsVisible = CursorUtil.getColumnIndexOrThrow(_cursor, "isVisible");
          final List<AppConfigEntity> _result = new ArrayList<AppConfigEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AppConfigEntity _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpPackageName;
            _tmpPackageName = _cursor.getString(_cursorIndexOfPackageName);
            final String _tmpLabel;
            _tmpLabel = _cursor.getString(_cursorIndexOfLabel);
            final String _tmpIconUrl;
            if (_cursor.isNull(_cursorIndexOfIconUrl)) {
              _tmpIconUrl = null;
            } else {
              _tmpIconUrl = _cursor.getString(_cursorIndexOfIconUrl);
            }
            final int _tmpRowId;
            _tmpRowId = _cursor.getInt(_cursorIndexOfRowId);
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            final boolean _tmpIsVisible;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsVisible);
            _tmpIsVisible = _tmp != 0;
            _item = new AppConfigEntity(_tmpId,_tmpPackageName,_tmpLabel,_tmpIconUrl,_tmpRowId,_tmpSortOrder,_tmpIsVisible);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<AppConfigEntity>> getAllAppsFlow() {
    final String _sql = "SELECT * FROM apps_config WHERE isVisible = 1 ORDER BY rowId ASC, sortOrder ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"apps_config"}, new Callable<List<AppConfigEntity>>() {
      @Override
      @NonNull
      public List<AppConfigEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPackageName = CursorUtil.getColumnIndexOrThrow(_cursor, "packageName");
          final int _cursorIndexOfLabel = CursorUtil.getColumnIndexOrThrow(_cursor, "label");
          final int _cursorIndexOfIconUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "iconUrl");
          final int _cursorIndexOfRowId = CursorUtil.getColumnIndexOrThrow(_cursor, "rowId");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final int _cursorIndexOfIsVisible = CursorUtil.getColumnIndexOrThrow(_cursor, "isVisible");
          final List<AppConfigEntity> _result = new ArrayList<AppConfigEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AppConfigEntity _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpPackageName;
            _tmpPackageName = _cursor.getString(_cursorIndexOfPackageName);
            final String _tmpLabel;
            _tmpLabel = _cursor.getString(_cursorIndexOfLabel);
            final String _tmpIconUrl;
            if (_cursor.isNull(_cursorIndexOfIconUrl)) {
              _tmpIconUrl = null;
            } else {
              _tmpIconUrl = _cursor.getString(_cursorIndexOfIconUrl);
            }
            final int _tmpRowId;
            _tmpRowId = _cursor.getInt(_cursorIndexOfRowId);
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            final boolean _tmpIsVisible;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsVisible);
            _tmpIsVisible = _tmp != 0;
            _item = new AppConfigEntity(_tmpId,_tmpPackageName,_tmpLabel,_tmpIconUrl,_tmpRowId,_tmpSortOrder,_tmpIsVisible);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<MenuItemEntity>> getMenuItemsFlow() {
    final String _sql = "SELECT * FROM menu_items WHERE isAvailable = 1 ORDER BY sortOrder ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"menu_items"}, new Callable<List<MenuItemEntity>>() {
      @Override
      @NonNull
      public List<MenuItemEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfNameBn = CursorUtil.getColumnIndexOrThrow(_cursor, "nameBn");
          final int _cursorIndexOfPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "price");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfImageUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "imageUrl");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final int _cursorIndexOfIsAvailable = CursorUtil.getColumnIndexOrThrow(_cursor, "isAvailable");
          final List<MenuItemEntity> _result = new ArrayList<MenuItemEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MenuItemEntity _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpNameBn;
            if (_cursor.isNull(_cursorIndexOfNameBn)) {
              _tmpNameBn = null;
            } else {
              _tmpNameBn = _cursor.getString(_cursorIndexOfNameBn);
            }
            final double _tmpPrice;
            _tmpPrice = _cursor.getDouble(_cursorIndexOfPrice);
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final String _tmpImageUrl;
            if (_cursor.isNull(_cursorIndexOfImageUrl)) {
              _tmpImageUrl = null;
            } else {
              _tmpImageUrl = _cursor.getString(_cursorIndexOfImageUrl);
            }
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            final boolean _tmpIsAvailable;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsAvailable);
            _tmpIsAvailable = _tmp != 0;
            _item = new MenuItemEntity(_tmpId,_tmpName,_tmpNameBn,_tmpPrice,_tmpCategory,_tmpImageUrl,_tmpSortOrder,_tmpIsAvailable);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<ServiceEntity>> getServicesFlow() {
    final String _sql = "SELECT * FROM services";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"services"}, new Callable<List<ServiceEntity>>() {
      @Override
      @NonNull
      public List<ServiceEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfNameBn = CursorUtil.getColumnIndexOrThrow(_cursor, "nameBn");
          final int _cursorIndexOfIcon = CursorUtil.getColumnIndexOrThrow(_cursor, "icon");
          final List<ServiceEntity> _result = new ArrayList<ServiceEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ServiceEntity _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpNameBn;
            if (_cursor.isNull(_cursorIndexOfNameBn)) {
              _tmpNameBn = null;
            } else {
              _tmpNameBn = _cursor.getString(_cursorIndexOfNameBn);
            }
            final String _tmpIcon;
            _tmpIcon = _cursor.getString(_cursorIndexOfIcon);
            _item = new ServiceEntity(_tmpId,_tmpName,_tmpNameBn,_tmpIcon);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
