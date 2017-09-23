package com.vs_unusedappremover.data

import android.content.ContentResolver
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.net.Uri

class SelectQuery {

    private var from: Any? = null
    private var columns: Array<String>? = null
    private var selection: String? = null
    private var selectArgs: Array<String>? = null
    private var orderBy: String? = null

    fun select(vararg columns: String): SelectQuery {
        this.columns = Array(columns.size, { i -> columns[i]})
        return this
    }

    fun from(table: String): SelectQuery {
        this.from = table
        return this
    }

    fun from(uri: Uri): SelectQuery {
        this.from = uri
        return this
    }

    fun where(condition: String, vararg args: Any): SelectQuery {
        this.selection = condition
        selectArgs = Array(args.size, {i -> args[i].toString()})
        return this
    }

    fun orderByAscending(column: String): SelectQuery {
        orderBy = column + " asc"
        return this
    }

    fun orderByDescending(column: String): SelectQuery {
        orderBy = column + " desc"
        return this
    }

    fun execute(db: SQLiteDatabase): Cursor {
        return db.query(from as String?, columns, selection, selectArgs, null, null, orderBy) ?: throw SQLiteException("null cursor")
    }

    fun execute(resolver: ContentResolver): Cursor {
        return resolver.query((from as Uri?)!!, columns, selection, selectArgs, orderBy) ?: throw SQLiteException("null cursor")
    }
}
