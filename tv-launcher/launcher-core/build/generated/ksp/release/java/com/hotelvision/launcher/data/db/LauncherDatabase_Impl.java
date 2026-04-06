package com.hotelvision.launcher.data.db;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.hotelvision.launcher.data.db.dao.LauncherDao;
import com.hotelvision.launcher.data.db.dao.LauncherDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class LauncherDatabase_Impl extends LauncherDatabase {
  private volatile LauncherDao _launcherDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(2) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `room_info` (`id` INTEGER NOT NULL, `roomNumber` TEXT NOT NULL, `guestName` TEXT, `checkoutTime` TEXT, `status` TEXT NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `launcher_rows` (`id` INTEGER NOT NULL, `rowType` TEXT NOT NULL, `title` TEXT NOT NULL, `titleBn` TEXT, `sortOrder` INTEGER NOT NULL, `isVisible` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `apps_config` (`id` INTEGER NOT NULL, `packageName` TEXT NOT NULL, `label` TEXT NOT NULL, `iconUrl` TEXT, `rowId` INTEGER NOT NULL, `sortOrder` INTEGER NOT NULL, `isVisible` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `menu_items` (`id` INTEGER NOT NULL, `name` TEXT NOT NULL, `nameBn` TEXT, `price` REAL NOT NULL, `category` TEXT NOT NULL, `imageUrl` TEXT, `sortOrder` INTEGER NOT NULL, `isAvailable` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `services` (`id` INTEGER NOT NULL, `name` TEXT NOT NULL, `nameBn` TEXT, `icon` TEXT NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `sync_meta` (`id` INTEGER NOT NULL, `lastSyncedAt` TEXT NOT NULL, `serverVersion` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '2877003f7cad91f5f99b3c8e77b36321')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `room_info`");
        db.execSQL("DROP TABLE IF EXISTS `launcher_rows`");
        db.execSQL("DROP TABLE IF EXISTS `apps_config`");
        db.execSQL("DROP TABLE IF EXISTS `menu_items`");
        db.execSQL("DROP TABLE IF EXISTS `services`");
        db.execSQL("DROP TABLE IF EXISTS `sync_meta`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsRoomInfo = new HashMap<String, TableInfo.Column>(5);
        _columnsRoomInfo.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRoomInfo.put("roomNumber", new TableInfo.Column("roomNumber", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRoomInfo.put("guestName", new TableInfo.Column("guestName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRoomInfo.put("checkoutTime", new TableInfo.Column("checkoutTime", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRoomInfo.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysRoomInfo = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesRoomInfo = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoRoomInfo = new TableInfo("room_info", _columnsRoomInfo, _foreignKeysRoomInfo, _indicesRoomInfo);
        final TableInfo _existingRoomInfo = TableInfo.read(db, "room_info");
        if (!_infoRoomInfo.equals(_existingRoomInfo)) {
          return new RoomOpenHelper.ValidationResult(false, "room_info(com.hotelvision.launcher.data.db.entities.RoomInfoEntity).\n"
                  + " Expected:\n" + _infoRoomInfo + "\n"
                  + " Found:\n" + _existingRoomInfo);
        }
        final HashMap<String, TableInfo.Column> _columnsLauncherRows = new HashMap<String, TableInfo.Column>(6);
        _columnsLauncherRows.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLauncherRows.put("rowType", new TableInfo.Column("rowType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLauncherRows.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLauncherRows.put("titleBn", new TableInfo.Column("titleBn", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLauncherRows.put("sortOrder", new TableInfo.Column("sortOrder", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLauncherRows.put("isVisible", new TableInfo.Column("isVisible", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysLauncherRows = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesLauncherRows = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoLauncherRows = new TableInfo("launcher_rows", _columnsLauncherRows, _foreignKeysLauncherRows, _indicesLauncherRows);
        final TableInfo _existingLauncherRows = TableInfo.read(db, "launcher_rows");
        if (!_infoLauncherRows.equals(_existingLauncherRows)) {
          return new RoomOpenHelper.ValidationResult(false, "launcher_rows(com.hotelvision.launcher.data.db.entities.LauncherRowEntity).\n"
                  + " Expected:\n" + _infoLauncherRows + "\n"
                  + " Found:\n" + _existingLauncherRows);
        }
        final HashMap<String, TableInfo.Column> _columnsAppsConfig = new HashMap<String, TableInfo.Column>(7);
        _columnsAppsConfig.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppsConfig.put("packageName", new TableInfo.Column("packageName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppsConfig.put("label", new TableInfo.Column("label", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppsConfig.put("iconUrl", new TableInfo.Column("iconUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppsConfig.put("rowId", new TableInfo.Column("rowId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppsConfig.put("sortOrder", new TableInfo.Column("sortOrder", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppsConfig.put("isVisible", new TableInfo.Column("isVisible", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysAppsConfig = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesAppsConfig = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoAppsConfig = new TableInfo("apps_config", _columnsAppsConfig, _foreignKeysAppsConfig, _indicesAppsConfig);
        final TableInfo _existingAppsConfig = TableInfo.read(db, "apps_config");
        if (!_infoAppsConfig.equals(_existingAppsConfig)) {
          return new RoomOpenHelper.ValidationResult(false, "apps_config(com.hotelvision.launcher.data.db.entities.AppConfigEntity).\n"
                  + " Expected:\n" + _infoAppsConfig + "\n"
                  + " Found:\n" + _existingAppsConfig);
        }
        final HashMap<String, TableInfo.Column> _columnsMenuItems = new HashMap<String, TableInfo.Column>(8);
        _columnsMenuItems.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMenuItems.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMenuItems.put("nameBn", new TableInfo.Column("nameBn", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMenuItems.put("price", new TableInfo.Column("price", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMenuItems.put("category", new TableInfo.Column("category", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMenuItems.put("imageUrl", new TableInfo.Column("imageUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMenuItems.put("sortOrder", new TableInfo.Column("sortOrder", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMenuItems.put("isAvailable", new TableInfo.Column("isAvailable", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysMenuItems = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesMenuItems = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoMenuItems = new TableInfo("menu_items", _columnsMenuItems, _foreignKeysMenuItems, _indicesMenuItems);
        final TableInfo _existingMenuItems = TableInfo.read(db, "menu_items");
        if (!_infoMenuItems.equals(_existingMenuItems)) {
          return new RoomOpenHelper.ValidationResult(false, "menu_items(com.hotelvision.launcher.data.db.entities.MenuItemEntity).\n"
                  + " Expected:\n" + _infoMenuItems + "\n"
                  + " Found:\n" + _existingMenuItems);
        }
        final HashMap<String, TableInfo.Column> _columnsServices = new HashMap<String, TableInfo.Column>(4);
        _columnsServices.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServices.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServices.put("nameBn", new TableInfo.Column("nameBn", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServices.put("icon", new TableInfo.Column("icon", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysServices = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesServices = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoServices = new TableInfo("services", _columnsServices, _foreignKeysServices, _indicesServices);
        final TableInfo _existingServices = TableInfo.read(db, "services");
        if (!_infoServices.equals(_existingServices)) {
          return new RoomOpenHelper.ValidationResult(false, "services(com.hotelvision.launcher.data.db.entities.ServiceEntity).\n"
                  + " Expected:\n" + _infoServices + "\n"
                  + " Found:\n" + _existingServices);
        }
        final HashMap<String, TableInfo.Column> _columnsSyncMeta = new HashMap<String, TableInfo.Column>(3);
        _columnsSyncMeta.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSyncMeta.put("lastSyncedAt", new TableInfo.Column("lastSyncedAt", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSyncMeta.put("serverVersion", new TableInfo.Column("serverVersion", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSyncMeta = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSyncMeta = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSyncMeta = new TableInfo("sync_meta", _columnsSyncMeta, _foreignKeysSyncMeta, _indicesSyncMeta);
        final TableInfo _existingSyncMeta = TableInfo.read(db, "sync_meta");
        if (!_infoSyncMeta.equals(_existingSyncMeta)) {
          return new RoomOpenHelper.ValidationResult(false, "sync_meta(com.hotelvision.launcher.data.db.entities.SyncMetaEntity).\n"
                  + " Expected:\n" + _infoSyncMeta + "\n"
                  + " Found:\n" + _existingSyncMeta);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "2877003f7cad91f5f99b3c8e77b36321", "1ba229aa44560cf834094a638ad89f45");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "room_info","launcher_rows","apps_config","menu_items","services","sync_meta");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `room_info`");
      _db.execSQL("DELETE FROM `launcher_rows`");
      _db.execSQL("DELETE FROM `apps_config`");
      _db.execSQL("DELETE FROM `menu_items`");
      _db.execSQL("DELETE FROM `services`");
      _db.execSQL("DELETE FROM `sync_meta`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(LauncherDao.class, LauncherDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public LauncherDao launcherDao() {
    if (_launcherDao != null) {
      return _launcherDao;
    } else {
      synchronized(this) {
        if(_launcherDao == null) {
          _launcherDao = new LauncherDao_Impl(this);
        }
        return _launcherDao;
      }
    }
  }
}
