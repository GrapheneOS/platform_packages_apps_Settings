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
        uri: Uri?,
        strings: Array<String?>?,
        s: String?,
        strings1: Array<String?>?,
        s1: String?
    ): Cursor? {
        throw UnsupportedOperationException()
    }

    final override fun getType(uri: Uri?): String? {
        throw UnsupportedOperationException()
    }

    final override fun insert(uri: Uri?, contentValues: ContentValues?): Uri? {
        throw UnsupportedOperationException()
    }

    final override fun delete(uri: Uri?, s: String?, strings: Array<String?>?): Int {
        throw UnsupportedOperationException()
    }

    final override fun update(
        uri: Uri?,
        contentValues: ContentValues?,
        s: String?,
        strings: Array<String?>?
    ): Int {
        throw UnsupportedOperationException()
    }
}
