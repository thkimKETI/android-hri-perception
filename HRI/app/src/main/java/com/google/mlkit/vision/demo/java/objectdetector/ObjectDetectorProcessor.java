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

package com.google.mlkit.vision.demo.java.objectdetector;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.demo.java.viewmanager.GraphicOverlay;
import com.google.mlkit.vision.demo.java.viewmanager.VisionProcessorBase;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.ObjectDetectorOptionsBase;
import java.util.List;

/** A processor to run object detector. */
public class ObjectDetectorProcessor extends VisionProcessorBase<List<DetectedObject>> {

  private static final String TAG = "ObjectDetectorProcessor";

  private final ObjectDetector detector;

  private List<DetectedObject> actionResult;
  private long startTime = 0;
  private int frameCount = 0;
  public ObjectDetectorProcessor(Context context, ObjectDetectorOptionsBase options) {
    super(context);
    detector = ObjectDetection.getClient(options);
  }

  @Override
  public void stop() {
    super.stop();
    detector.close();
  }

  @Override
  public Object getKETIResult() {
    return actionResult;
  }

  @Override
  protected Task<List<DetectedObject>> detectInImage(InputImage image) {
    long frameStartTime = System.currentTimeMillis();
    /*
    // FPS 계산을 위해 시간과 프레임 카운트 업데이트
    if (startTime == 0) {
      startTime = frameStartTime;
    }
    frameCount++;
    */
    return detector.process(image);
  }

  @Override
  protected void onSuccess(
      @NonNull List<DetectedObject> results, @NonNull GraphicOverlay graphicOverlay) {

    //Venus: 검출 결과 접근
    actionResult = results;


    /*
    long currentTime = System.currentTimeMillis();
    double duration = (currentTime - startTime) / 1000.0; // 초 단위

    if (duration >= 1.0) {
      double fps = frameCount / duration;
      Log.d("FPS", "Average CUSTOM OB FPS: " + fps);
      // 시작 시간과 프레임 카운트 리셋
      startTime = currentTime;
      frameCount = 0;
    }
    */

    //Venus: Custom Detection 결과 그리기 삭제
    //for (DetectedObject object : results) {
    //  graphicOverlay.add(new ObjectGraphic(graphicOverlay, object));
    //}
  }

  @Override
  protected void onFailure(@NonNull Exception e) {
    Log.e(TAG, "Object detection failed!", e);
  }
}
