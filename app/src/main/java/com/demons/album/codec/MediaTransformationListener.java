/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.demons.album.codec;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.linkedin.android.litr.TransformationListener;
import com.linkedin.android.litr.analytics.TrackTransformationInfo;

import java.util.List;

public class MediaTransformationListener implements TransformationListener {

    private final Context context;
    private final String requestId;
    private final TransformationState transformationState;
    private final TargetMedia targetMedia;

    @VisibleForTesting
    public MediaTransformationListener(@NonNull Context context,
                                       @NonNull String requestId,
                                       @NonNull TransformationState transformationState,
                                       @NonNull TargetMedia targetMedia) {
        this.context = context;
        this.requestId = requestId;
        this.transformationState = transformationState;
        this.targetMedia = targetMedia;
    }

    @Override
    public void onStarted(@NonNull String id) {
        if (TextUtils.equals(requestId, id)) {
            transformationState.setState(TransformationState.STATE_RUNNING);
            Log.e(
                    "TranscoderUtils",
                    "转码开始"
            );
        }
    }

    @Override
    public void onProgress(@NonNull String id, float progress) {
        if (TextUtils.equals(requestId, id)) {
            transformationState.setProgress((int) (progress * TransformationState.MAX_PROGRESS));
            Log.e(
                    "TranscoderUtils",
                    "转码进度:" + transformationState.progress
            );
        }
    }

    @Override
    public void onCompleted(@NonNull String id, @Nullable List<TrackTransformationInfo> trackTransformationInfos) {
        if (TextUtils.equals(requestId, id)) {
            transformationState.setState(TransformationState.STATE_COMPLETED);
            transformationState.setProgress(TransformationState.MAX_PROGRESS);
//            transformationState.setStats(TrackMetadataUtil.printTransformationStats(context, trackTransformationInfos));
//            publisher.publish(targetMedia.targetFile, false, (file, contentUri) -> targetMedia.setContentUri(contentUri));
            Log.e(
                    "TranscoderUtils",
                    "转码成功,文件地址:" + targetMedia.targetFile.getPath()
            );
        }
    }

    @Override
    public void onCancelled(@NonNull String id, @Nullable List<TrackTransformationInfo> trackTransformationInfos) {
        if (TextUtils.equals(requestId, id)) {
            transformationState.setState(TransformationState.STATE_CANCELLED);
//            transformationState.setStats(TrackMetadataUtil.printTransformationStats(context, trackTransformationInfos));
            Log.e(
                    "TranscoderUtils",
                    "转码取消"
            );
        }
    }

    @Override
    public void onError(@NonNull String id,
                        @Nullable Throwable cause,
                        @Nullable List<TrackTransformationInfo> trackTransformationInfos) {
        if (TextUtils.equals(requestId, id)) {
            transformationState.setState(TransformationState.STATE_ERROR);
//            transformationState.setStats(TrackMetadataUtil.printTransformationStats(context, trackTransformationInfos));
            Log.e(
                    "TranscoderUtils",
                    "转码失败"
            );
        }
    }
}
