/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.demons.album.codec;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.provider.MediaStore;

import androidx.annotation.NonNull;

import java.io.File;

public class TransformationUtil {

    private TransformationUtil() {}

    @NonNull
    public static File getTargetFileDirectory(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return context.getNoBackupFilesDir();
        } else {
            return context.getFilesDir();
        }
    }

    @NonNull
    public static String getDisplayName(@NonNull Context context, @NonNull Uri uri) {
        String name = Long.toString(SystemClock.elapsedRealtime());

        String[] projection = { MediaStore.Video.Media.DISPLAY_NAME};
        Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                name = cursor.getString(0);
            }
            cursor.close();
        }
        return name;
    }
}
