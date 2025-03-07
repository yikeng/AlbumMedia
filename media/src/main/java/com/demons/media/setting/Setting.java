package com.demons.media.setting;

import android.view.View;

import androidx.annotation.IntDef;

import com.demons.media.constant.Type;
import com.demons.media.engine.ImageEngine;
import com.demons.media.models.album.entity.Photo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * AlbumMedia的设置值
 */
public class Setting {
    public static int minWidth = 1;
    public static int minHeight = 1;
    public static long minSize = 1;
    public static int count = 1;

    public static int videoMaxSize = 500 * 1048576;
    public static int photoMaxSize = 64 * 1048576;

    public static WeakReference<View> photosAdView = null;
    public static WeakReference<View> albumItemsAdView = null;
    public static boolean photoAdIsOk = false;
    public static boolean albumItemsAdIsOk = false;
    public static boolean useWidth = false;
    public static ArrayList<Photo> selectedPhotos = new ArrayList<>();
    public static boolean showOriginalMenu = false;
    public static boolean originalMenuUsable = false;
    public static String originalMenuUnusableHint = "";
    public static boolean selectedOriginal = false;
    public static String fileProviderAuthority = null;
    public static boolean isShowCamera = false;
    public static int cameraLocation = 1;
    public static boolean onlyStartCamera = false;
    public static boolean showPuzzleMenu = true;
    public static List<String> filterTypes = new ArrayList<>();
    public static boolean showGif = false;
    public static boolean showVideo = false;
    public static boolean showCleanMenu = true;
    public static long videoMinSecond = 0L;
    public static long videoMaxSecond = Long.MAX_VALUE;
    public static ImageEngine imageEngine = null;

    public static final int LIST_FIRST = 0;
    public static final int BOTTOM_RIGHT = 1;

    public static boolean complexSelector = false;
    public static boolean complexSingleType = false;
    public static int complexVideoCount = 0;
    public static int complexPictureCount = 0;
    //当传入已选中图片时，是否按照之前选中的顺序排序
    public static boolean isSequentialSelectedPhotos = true;
    public static boolean showBottomMenu = true;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {LIST_FIRST, BOTTOM_RIGHT})
    public @interface Location {

    }

    public static void clear() {
        minWidth = 1;
        minHeight = 1;
        minSize = 1;
        count = 1;

        photosAdView = null;
        albumItemsAdView = null;
        photoAdIsOk = false;
        albumItemsAdIsOk = false;
        selectedPhotos.clear();
        showOriginalMenu = false;
        originalMenuUsable = false;
        originalMenuUnusableHint = "";
        selectedOriginal = false;
        cameraLocation = BOTTOM_RIGHT;
        isShowCamera = false;
        onlyStartCamera = false;
        showPuzzleMenu = true;
        filterTypes = new ArrayList<>();
        showGif = false;
        showVideo = false;
        showCleanMenu = true;
        videoMinSecond = 0L;
        videoMaxSecond = Long.MAX_VALUE;
        complexSelector = false;
        complexSingleType = false;
        complexVideoCount = 0;
        complexPictureCount = 0;
        isSequentialSelectedPhotos = true;
    }

    public static boolean isFilter(String type) {
        type = type.toLowerCase();
        for (String filterType : Setting.filterTypes) {
            if (type.contains(filterType)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isOnlyVideo() {
        return filterTypes.size() == 1 && filterTypes.get(0).equals(Type.VIDEO);
    }

    public static boolean hasPhotosAd() {
        return photosAdView != null && photosAdView.get() != null;
    }

    public static boolean hasAlbumItemsAd() {
        return albumItemsAdView != null && albumItemsAdView.get() != null;
    }

    public static boolean isBottomRightCamera() {
        return cameraLocation == BOTTOM_RIGHT;
    }
}
