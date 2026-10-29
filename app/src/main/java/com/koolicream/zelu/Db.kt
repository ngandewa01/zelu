package com.koolicream.zelu

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

// The payments read from messages, in an SQLite database on the phone. Adding one payment writes one row instead of
// rewriting every payment ever read. Your decisions about them (categories, names, splits) stay in their own files.
// Amounts are kept as whole paise, so adding them up can never drift by a fraction of a rupee.
@Entity(tableName = "txn", indices = [Index("date")])
data class TxnEntity(
    @PrimaryKey val id: String,
    val date: String,
    @ColumnInfo(name = "amount_paise") val amountPaise: Long,
    val direction: String,
    val counterparty: String?,
    val ref: String?,
    val raw: String,
    val source: String,
    val account: String,
    val kind: String,
    val category: String,
    val ms: Long,
) {
    fun toRow() = Row(id, date, amountPaise, Dir.of(direction), counterparty, ref, raw, source, account, Kind.of(kind), category, ms)

    companion object {
        fun of(r: Row) = TxnEntity(r.id, r.date, r.paise, r.direction.key, r.counterparty, r.ref, r.raw,
            r.source, r.account, r.kind.key, r.category, r.ms)
    }
}

@Dao
interface TxnDao {
    @Query("SELECT * FROM txn")
    fun all(): List<TxnEntity>

    @Query("SELECT id FROM txn")
    fun ids(): List<String>

    /** Adds the payments not stored yet; a stored one with the same id is kept. Returns -1 for each one skipped. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertNew(rows: List<TxnEntity>): List<Long>

    @Query("DELETE FROM txn")
    fun deleteAll()
}

// Your decisions that are one value per key, as the key/value files kept them (overrides, payee rules, renames,
// titles, accounts...): one row per key, so changing one decision writes one row. [file] is the file's old name.
@Entity(tableName = "kv", primaryKeys = ["file", "k"])
data class KvEntity(val file: String, val k: String, val a: String, val b: String)

// Everything else Zelu saved as a small file (reminders, splits, budgets, balances, plan...), under the file's old name
// and with the same text, so backups and older versions read it unchanged.
@Entity(tableName = "doc")
data class DocEntity(@PrimaryKey val name: String, val body: String)

@Dao
interface StoreDao {
    @Query("SELECT * FROM kv WHERE file = :file")
    fun kv(file: String): List<KvEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun putKv(rows: List<KvEntity>)

    @Query("DELETE FROM kv WHERE file = :file AND k = :k")
    fun removeKv(file: String, k: String)

    @Query("DELETE FROM kv WHERE file = :file")
    fun clearKv(file: String)

    @Query("SELECT body FROM doc WHERE name = :name")
    fun doc(name: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun putDoc(doc: DocEntity)

    @Query("DELETE FROM doc WHERE name = :name")
    fun removeDoc(name: String)

    @Query("DELETE FROM kv")
    fun clearAllKv()

    @Query("DELETE FROM doc")
    fun clearAllDocs()
}

// Version 2 adds the kv and doc tables; payments are untouched.
@Database(entities = [TxnEntity::class, KvEntity::class, DocEntity::class], version = 2, exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)])
abstract class ZeluDb : RoomDatabase() {
    abstract fun txns(): TxnDao
    abstract fun store(): StoreDao

    companion object {
        @Volatile private var real: ZeluDb? = null
        @Volatile private var demo: ZeluDb? = null

        // Sample data has its own database, as it has its own files. It is opened on the application context, never on
        // the screen that asked (DemoContext wraps the activity), so the database never holds on to a closed screen.
        // Storage reads payments on whichever thread asks, as it did with the file, so main-thread reads are allowed.
        fun get(context: Context): ZeluDb {
            val isDemo = context is DemoContext
            (if (isDemo) demo else real)?.let { return it }
            return synchronized(this) {
                (if (isDemo) demo else real) ?: run {
                    val app = if (context is DemoContext) context.baseContext.applicationContext else context.applicationContext
                    Room.databaseBuilder(app, ZeluDb::class.java, if (isDemo) "zelu-demo.db" else "zelu.db")
                        .allowMainThreadQueries()
                        .build()
                        .also { if (isDemo) demo = it else real = it }
                }
            }
        }
    }
}
