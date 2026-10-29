package com.koolicream.zelu

import android.content.Context
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
    fun toRow() = Row(id, date, amountPaise / 100.0, direction, counterparty, ref, raw, source, account, kind, category, ms)

    companion object {
        fun of(r: Row) = TxnEntity(r.id, r.date, Math.round(r.amount * 100), r.direction, r.counterparty, r.ref, r.raw,
            r.source, r.account, r.kind, r.category, r.ms)
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

@Database(entities = [TxnEntity::class], version = 1, exportSchema = false)
abstract class ZeluDb : RoomDatabase() {
    abstract fun txns(): TxnDao

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
