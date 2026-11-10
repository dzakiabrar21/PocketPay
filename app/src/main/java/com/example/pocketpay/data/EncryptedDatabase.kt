package com.example.pocketpay.data

import android.content.Context
import android.util.Base64
import com.example.pocketpay.presentation.security.Vault
import net.zetetic.database.sqlcipher.SQLiteDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File
import java.security.SecureRandom

/**
 * Opens the app database with SQLCipher (AES-256). The passphrase is random, generated once, and
 * stored only in encrypted form (Android Keystore, see [Vault]) - it never exists on disk in clear.
 */
object EncryptedDatabase {
    private const val PREFS = "pocketpay_db_key"
    private const val KEY_PASSPHRASE = "passphrase"
    private val SQLITE_MAGIC = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)

    private var libraryLoaded = false

    private fun ensureLibrary() {
        if (!libraryLoaded) {
            System.loadLibrary("sqlcipher")
            libraryLoaded = true
        }
    }

    /** Returns the passphrase, creating it on first use. Null means an old one existed but is unreadable. */
    private fun passphrase(context: Context, dbFile: File): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getString(KEY_PASSPHRASE, null)?.let { token ->
            try {
                return String(Vault.cipher.decrypt(token), Charsets.US_ASCII)
            } catch (e: Exception) {
                // The Keystore key is gone (e.g. app data restored on another device). The encrypted
                // database can never be read again, so start clean rather than crash-looping.
                deleteDatabaseFiles(dbFile)
            }
        }
        val fresh = Base64.encodeToString(ByteArray(32).also { SecureRandom().nextBytes(it) }, Base64.NO_WRAP)
        prefs.edit().putString(KEY_PASSPHRASE, Vault.cipher.encrypt(fresh.toByteArray(Charsets.US_ASCII))).commit()
        return fresh
    }

    private fun deleteDatabaseFiles(dbFile: File) {
        listOf("", "-wal", "-shm", "-journal").forEach { File(dbFile.path + it).delete() }
    }

    private fun isPlaintextSqlite(file: File): Boolean {
        if (!file.exists() || file.length() < SQLITE_MAGIC.size) return false
        val header = ByteArray(SQLITE_MAGIC.size)
        file.inputStream().use { it.read(header) }
        return header.contentEquals(SQLITE_MAGIC)
    }

    /**
     * One-time upgrade for installs that created a plaintext database in an earlier version.
     * Reads the old file with the framework SQLite (it understands WAL) and rebuilds schema + rows
     * inside a new SQLCipher file in one transaction, then replaces the original.
     * (SQLCipher's ATTACH/sqlcipher_export route silently did nothing on WAL databases here.)
     */
    private fun encryptExistingPlaintext(dbFile: File, passphrase: String) {
        val tmp = File(dbFile.path + ".enc")
        deleteDatabaseFiles(tmp)
        val source = android.database.sqlite.SQLiteDatabase.openDatabase(
            dbFile.path, null, android.database.sqlite.SQLiteDatabase.OPEN_READONLY
        )
        var target: SQLiteDatabase? = null
        try {
            val version = source.version
            target = SQLiteDatabase.openDatabase(
                tmp.path, passphrase, null, SQLiteDatabase.CREATE_IF_NECESSARY, null, null
            )
            target.beginTransaction()
            try {
                // Tables first (so rows can be inserted), indices afterwards.
                val ddl = mutableListOf<Pair<String, String>>() // type to sql
                source.rawQuery(
                    "SELECT type, name, sql FROM sqlite_master WHERE sql IS NOT NULL " +
                        "AND name NOT LIKE 'sqlite_%' AND name != 'android_metadata'", null
                ).use { c ->
                    while (c.moveToNext()) ddl += c.getString(0) to c.getString(2)
                }
                ddl.filter { it.first == "table" }.forEach { target.execSQL(it.second) }

                val tables = mutableListOf<String>()
                source.rawQuery(
                    "SELECT name FROM sqlite_master WHERE type = 'table' " +
                        "AND name NOT LIKE 'sqlite_%' AND name != 'android_metadata'", null
                ).use { c -> while (c.moveToNext()) tables += c.getString(0) }

                for (table in tables) {
                    source.rawQuery("SELECT * FROM \"$table\"", null).use { c ->
                        val cols = c.columnNames
                        val sql = "INSERT INTO \"$table\" (${cols.joinToString { "\"$it\"" }}) " +
                            "VALUES (${cols.joinToString { "?" }})"
                        while (c.moveToNext()) {
                            val args = Array<Any?>(cols.size) { i ->
                                when (c.getType(i)) {
                                    android.database.Cursor.FIELD_TYPE_NULL -> null
                                    android.database.Cursor.FIELD_TYPE_INTEGER -> c.getLong(i)
                                    android.database.Cursor.FIELD_TYPE_FLOAT -> c.getDouble(i)
                                    android.database.Cursor.FIELD_TYPE_BLOB -> c.getBlob(i)
                                    else -> c.getString(i)
                                }
                            }
                            target.execSQL(sql, args)
                        }
                    }
                }
                ddl.filter { it.first != "table" }.forEach { target.execSQL(it.second) }
                target.setTransactionSuccessful()
            } finally {
                target.endTransaction()
            }
            // Room decides whether to run migrations from user_version, so carry it over.
            target.version = version
        } catch (e: Exception) {
            runCatching { target?.close() }
            source.close()
            deleteDatabaseFiles(tmp)
            throw e
        }
        target.close()
        source.close()
        deleteDatabaseFiles(dbFile)
        check(tmp.renameTo(dbFile)) { "Could not replace the database with its encrypted copy" }
    }

    fun openHelperFactory(context: Context, dbName: String): SupportOpenHelperFactory {
        ensureLibrary()
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()
        val pass = passphrase(context, dbFile)
        if (isPlaintextSqlite(dbFile)) encryptExistingPlaintext(dbFile, pass)
        return SupportOpenHelperFactory(pass.toByteArray(Charsets.US_ASCII))
    }
}
