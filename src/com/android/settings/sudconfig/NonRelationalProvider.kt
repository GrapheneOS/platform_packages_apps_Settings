package com.android.settings.sudconfig

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri

/**
 * Useful for implementing content provider interfaces that are exclusively for non-relational
 * (non-tabular) models. This reduces the boilerplate code by providing empty implementations for
 * methods which are unneeded in non-relational models.
 *
 * @see [ContentProvider.call]
 */
abstract class NonRelationalProvider : ContentProvider() {

    final override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        throw UnsupportedOperationException()
    }

    final override fun getType(uri: Uri): String? {
        throw UnsupportedOperationException()
    }

    final override fun insert(uri: Uri, values: ContentValues?): Uri? {
        throw UnsupportedOperationException()
    }

    final override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        throw UnsupportedOperationException()
    }

    final override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        throw UnsupportedOperationException()
    }
}
