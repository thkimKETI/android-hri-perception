/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.mlkit.vision.demo.java.viewmanager;

import static java.lang.Math.max;
import static java.lang.Math.min;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build.VERSION_CODES;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageProxy;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskExecutors;
import com.google.android.gms.tasks.Tasks;
import com.google.android.odml.image.BitmapMlImageBuilder;
import com.google.android.odml.image.ByteBufferMlImageBuilder;
import com.google.android.odml.image.MediaMlImageBuilder;
import com.google.android.odml.image.MlImage;
import com.google.mlkit.common.MlKitException;
import com.google.mlkit.vision.common.InputImage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Abstract base class for vision frame processors. Subclasses need to implement {@link
 * #onSuccess(Object, GraphicOverlay)} to define what they want to with the detection results and
 * {@link #detectInImage(InputImage)} to specify the detector object.
 *
 * @param <T> The type of the detected feature.
 */
public abstract class VisionProcessorBase<T> implements VisionImageProcessor {

  protected static final String MANUAL_TESTING_LOG = "LogTagForTest";
  private static final String TAG = "VisionProcessorBase";

  private final ActivityManager activityManager;
  private final Timer fpsTimer = new Timer();
  private final ScopedExecutor executor;

  // Whether this processor is already shut down
  private boolean isShutdown;

  // Used to calculate latency, running in the same thread, no sync needed.
  private int numRuns = 0;
  private long totalFrameMs = 0;
  private long maxFrameMs = 0;
  private long minFrameMs = Long.MAX_VALUE;
  private long totalDetectorMs = 0;
  private long maxDetectorMs = 0;
  private long minDetectorMs = Long.MAX_VALUE;

  // Frame count that have been processed so far in an one second interval to calculate FPS.
  private int frameProcessedInOneSecondInterval = 0;
  private int framesPerSecond = 0;

  // To keep the latest images and its metadata.
  @GuardedBy("this")
  private ByteBuffer latestImage;

  @GuardedBy("this")
  private FrameMetadata latestImageMetaData;
  // To keep the images and metadata in process.
  @GuardedBy("this")
  private ByteBuffer processingImage;

  @GuardedBy("this")
  private FrameMetadata processingMetaData;

  protected VisionProcessorBase(Context context) {
    activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    executor = new ScopedExecutor(TaskExecutors.MAIN_THREAD);
    fpsTimer.scheduleAtFixedRate(
        new TimerTask() {
          @Override
          public void run() {
            framesPerSecond = frameProcessedInOneSecondInterval;
            frameProcessedInOneSecondInterval = 0;
          }
        },
        /* delay= */ 0,
        /* period= */ 1000);
  }

  // -----------------Code for processing single still image----------------------------------------
  @Override
  public void processBitmap(Bitmap bitmap, final GraphicOverlay graphicOverlay, int wo) {
    long frameStartMs = SystemClock.elapsedRealtime();

    if (isMlImageEnabled(graphicOverlay.getContext())) {
      MlImage mlImage = new BitmapMlImageBuilder(bitmap).build();
      requestDetectInImage(
          mlImage,
          graphicOverlay,
          /* originalCameraImage= */ null,
          /* shouldShowFps= */ false,
          frameStartMs,
              wo);
      mlImage.close();

      return;
    }

    requestDetectInImage(
        InputImage.fromBitmap(bitmap, 0),
        graphicOverlay,
        /* originalCameraImage= */ null,
        /* shouldShowFps= */ false,
        frameStartMs,
            wo);
  }

  // -----------------Code for processing live preview frame from Camera1 API-----------------------
  @Override
  public synchronized void processByteBuffer(
      ByteBuffer data, final FrameMetadata frameMetadata, final GraphicOverlay graphicOverlay, int wo) {
    latestImage = data;
    latestImageMetaData = frameMetadata;
    if (processingImage == null && processingMetaData == null) {
      processLatestImage(graphicOverlay, wo);
    }
  }

  private synchronized void processLatestImage(final GraphicOverlay graphicOverlay, int wo) {
    processingImage = latestImage;
    processingMetaData = latestImageMetaData;
    latestImage = null;
    latestImageMetaData = null;
    if (processingImage != null && processingMetaData != null && !isShutdown) {
      processImage(processingImage, processingMetaData, graphicOverlay, wo);
    }
  }

  private void processImage(
      ByteBuffer data, final FrameMetadata frameMetadata, final GraphicOverlay graphicOverlay, int wo) {
    long frameStartMs = SystemClock.elapsedRealtime();

    // If live viewport is on (that is the underneath surface view takes care of the camera preview
    // drawing), skip the unnecessary bitmap creation that used for the manual preview drawing.
    Bitmap bitmap =
        PreferenceUtils.isCameraLiveViewportEnabled(graphicOverlay.getContext())
            ? null
            : BitmapUtils.getBitmap(data, frameMetadata);

    if (isMlImageEnabled(graphicOverlay.getContext())) {
      MlImage mlImage =
          new ByteBufferMlImageBuilder(
                  data,
                  frameMetadata.getWidth(),
                  frameMetadata.getHeight(),
                  MlImage.IMAGE_FORMAT_NV21)
              .setRotation(frameMetadata.getRotation())
              .build();

      requestDetectInImage(mlImage, graphicOverlay, bitmap, /* shouldShowFps= */ true, frameStartMs, wo)
              .addOnSuccessListener(executor, results -> processLatestImage(graphicOverlay, wo));
      // This is optional. Java Garbage collection can also close it eventually.
      mlImage.close();
      return;
    }

      requestDetectInImage(
              InputImage.fromByteBuffer(
                      data,
                      frameMetadata.getWidth(),
                      frameMetadata.getHeight(),
                      frameMetadata.getRotation(),
                      InputImage.IMAGE_FORMAT_NV21),
              graphicOverlay,
              bitmap,
              true,
              frameStartMs, wo)
              .addOnSuccessListener(executor, results -> processLatestImage(graphicOverlay, wo));
  }

  // -----------------Code for processing live preview frame from CameraX API-----------------------
  @Override
  @RequiresApi(VERSION_CODES.LOLLIPOP)
  @ExperimentalGetImage
  public void processImageProxy(ImageProxy image, GraphicOverlay graphicOverlay, int wo) {
    long frameStartMs = SystemClock.elapsedRealtime();
    if (isShutdown) {
      image.close();
      return;
    }

    Bitmap bitmap = null;
    if (!PreferenceUtils.isCameraLiveViewportEnabled(graphicOverlay.getContext())) {
      bitmap = BitmapUtils.getBitmap(image);
    }

    if (isMlImageEnabled(graphicOverlay.getContext())) {
      MlImage mlImage =
          new MediaMlImageBuilder(image.getImage())
              .setRotation(image.getImageInfo().getRotationDegrees())
              .build();

      requestDetectInImage(
              mlImage,
              graphicOverlay,
              /* originalCameraImage= */ bitmap,
              /* shouldShowFps= */ true,
              frameStartMs, wo)
          // When the image is from CameraX analysis use case, must call image.close() on received
          // images when finished using them. Otherwise, new images may not be received or the
          // camera may stall.
          // Currently MlImage doesn't support ImageProxy directly, so we still need to call
          // ImageProxy.close() here.
          .addOnCompleteListener(results -> image.close());
      return;
    }

    requestDetectInImage(
            InputImage.fromMediaImage(image.getImage(), image.getImageInfo().getRotationDegrees()),
            graphicOverlay,
            /* originalCameraImage= */ bitmap,
            /* shouldShowFps= */ true,
            frameStartMs, wo)
        // When the image is from CameraX analysis use case, must call image.close() on received
        // images when finished using them. Otherwise, new images may not be received or the camera
        // may stall.
        .addOnCompleteListener(results -> image.close());
  }

  // -----------------Common processing logic-------------------------------------------------------
  private Task<T> requestDetectInImage(
      final InputImage image,
      final GraphicOverlay graphicOverlay,
      @Nullable final Bitmap originalCameraImage,
      boolean shouldShowFps,
      long frameStartMs,
      int wo) {
    return setUpListener(
        detectInImage(image), graphicOverlay, originalCameraImage, shouldShowFps, frameStartMs, wo);
  }

  private Task<T> requestDetectInImage(
      final MlImage image,
      final GraphicOverlay graphicOverlay,
      @Nullable final Bitmap originalCameraImage,
      boolean shouldShowFps,
      long frameStartMs,
      int wo) {
    return setUpListener(
        detectInImage(image), graphicOverlay, originalCameraImage, shouldShowFps, frameStartMs, wo);
  }

  private Task<T> setUpListener(
      Task<T> task,
      final GraphicOverlay graphicOverlay,
      @Nullable final Bitmap originalCameraImage,
      boolean shouldShowFps,
      long frameStartMs,
      int wo) {
    final long detectorStartMs = SystemClock.elapsedRealtime();
    return task.addOnSuccessListener(
            executor,
            results -> {
              /*long endMs = SystemClock.elapsedRealtime();
              long currentFrameLatencyMs = endMs - frameStartMs;
              long currentDetectorLatencyMs = endMs - detectorStartMs;
              if (numRuns >= 500) {
                resetLatencyStats();
              }
              numRuns++;
              frameProcessedInOneSecondInterval++;
              totalFrameMs += currentFrameLatencyMs;
              maxFrameMs = max(currentFrameLatencyMs, maxFrameMs);
              minFrameMs = min(currentFrameLatencyMs, minFrameMs);
              totalDetectorMs += currentDetectorLatencyMs;
              maxDetectorMs = max(currentDetectorLatencyMs, maxDetectorMs);
              minDetectorMs = min(currentDetectorLatencyMs, minDetectorMs);

              // Only log inference info once per second. When frameProcessedInOneSecondInterval is
              // equal to 1, it means this is the first frame processed during the current second.
              if (frameProcessedInOneSecondInterval == 1) {
                Log.d(TAG, "Num of Runs: " + numRuns);
                Log.d(TAG, "Frame latency: max=" + maxFrameMs + ", min="+ minFrameMs + ", avg="+ totalFrameMs / numRuns);
                Log.d(TAG, "Detector latency: max=" + maxDetectorMs + ", min="+ minDetectorMs + ", avg=" + totalDetectorMs / numRuns);
                MemoryInfo mi = new MemoryInfo();
                activityManager.getMemoryInfo(mi);
                long availableMegs = mi.availMem / 0x100000L;
                Log.d(TAG, "Memory available in system: " + availableMegs + " MB");
                temperatureMonitor.logTemperature();
              }*/

              //graphicOverlay.clear();
              //1016 아래 코드 필요없는듯..

              if (wo== KETIDetectorConstants.FACE_DETECTOR_PROCESSOR){
              graphicOverlay.removeAllFaceGraphics();
              }

              else if (wo == KETIDetectorConstants.ACTION_DETECTOR_PROCESSOR_TMP){
                graphicOverlay.removeAllObjectGraphics();
              }

              else if (wo== KETIDetectorConstants.POSE_DETECTOR_PROCESSOR){
                graphicOverlay.removeAllPoseGraphics();
              }


              if (originalCameraImage != null) {
                graphicOverlay.add(new CameraImageGraphic(graphicOverlay, originalCameraImage));
              }

              try {
                VisionProcessorBase.this.onSuccess(results, graphicOverlay);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }

              //if (!PreferenceUtils.shouldHideDetectionInfo(graphicOverlay.getContext())) {graphicOverlay.add(new InferenceInfoGraphic(graphicOverlay,currentFrameLatencyMs,currentDetectorLatencyMs,shouldShowFps ? framesPerSecond : null));}

              graphicOverlay.postInvalidate();

            })
        .addOnFailureListener(
            executor,
            e -> {
              graphicOverlay.clear();
              graphicOverlay.postInvalidate();
              String error = "Failed to process. Error: " + e.getLocalizedMessage();
              Toast.makeText(graphicOverlay.getContext(),error + "\nCause: " + e.getCause(), Toast.LENGTH_SHORT).show();
              Log.d(TAG, error);
              e.printStackTrace();
              VisionProcessorBase.this.onFailure(e);
            });
  }

  @Override
  public void stop() {
    executor.shutdown();
    isShutdown = true;
    resetLatencyStats();
    fpsTimer.cancel();
  }

  private void resetLatencyStats() {
    numRuns = 0;
    totalFrameMs = 0;
    maxFrameMs = 0;
    minFrameMs = Long.MAX_VALUE;
    totalDetectorMs = 0;
    maxDetectorMs = 0;
    minDetectorMs = Long.MAX_VALUE;
  }

  protected abstract Task<T> detectInImage(InputImage image);

  protected Task<T> detectInImage(MlImage image) {
    return Tasks.forException(
        new MlKitException(
            "MlImage is currently not demonstrated for this feature",
            MlKitException.INVALID_ARGUMENT));
  }

  protected abstract void onSuccess(@NonNull T results, @NonNull GraphicOverlay graphicOverlay) throws IOException;

  protected abstract void onFailure(@NonNull Exception e);

  protected boolean isMlImageEnabled(Context context) {
    return false;
  }
}
