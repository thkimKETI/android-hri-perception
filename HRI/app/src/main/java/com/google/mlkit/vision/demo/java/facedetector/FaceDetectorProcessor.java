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

package com.google.mlkit.vision.demo.java.facedetector;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.demo.java.viewmanager.BitmapUtils;
import com.google.mlkit.vision.demo.java.viewmanager.FrameMetadata;
import com.google.mlkit.vision.demo.java.viewmanager.GraphicOverlay;
import com.google.mlkit.vision.demo.java.viewmanager.VisionProcessorBase;
import com.google.mlkit.vision.demo.java.viewmanager.PreferenceUtils;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Locale;

/** Face Detector Demo. */
public class FaceDetectorProcessor extends VisionProcessorBase<List<Face>> {

  private static final String TAG = "FaceDetectorProcessor";

  private final FaceDetector detector;
  Context context;
  Bitmap orgFrame;
  private FaceClassifierProcessor.EmotionResult emotionResult;

  private long startTime = 0;
  private int frameCount = 0;
  public FaceDetectorProcessor(Context context) {
    super(context);
    this.context = context;
    FaceDetectorOptions faceDetectorOptions = PreferenceUtils.getFaceDetectorOptions(context);
    Log.v(MANUAL_TESTING_LOG, "Face detector options: " + faceDetectorOptions);
    detector = FaceDetection.getClient(faceDetectorOptions);
  }

  @Override
  public void stop() {
    super.stop();
    detector.close();
  }

  @Override
  public Object getKETIResult() {
    return emotionResult;
  }

  @Override
  protected Task<List<Face>> detectInImage(InputImage image) {

    long frameStartTime = System.currentTimeMillis();
    //Venus: image로부터 Venus 원본 이미지 가져와 orgFrame에 저장
    ByteBuffer tmpBuf = image.getByteBuffer();
    FrameMetadata frameMetadata = new FrameMetadata.Builder()
            .setWidth(image.getWidth())
            .setHeight(image.getHeight())
            .setRotation(image.getRotationDegrees())
            .build();

    orgFrame = BitmapUtils.getBitmap(tmpBuf, frameMetadata);

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
  protected void onSuccess(@NonNull List<Face> faces, @NonNull GraphicOverlay graphicOverlay) throws IOException {
    //Venus: 검출 결과 접근

    if(faces.size() == 0)   //얼굴이 검출되지 않았다면, emotionResult null로 변경
      emotionResult = null;
    for (Face face : faces) {
      //Venus: 얼굴 그리기 관련 UI 삭제
      //graphicOverlay.add(new FaceGraphic(graphicOverlay, face));
      //logExtrasForTesting(face);

	  //Venus : 원본이미지에서 인식된 Face의 영역만 Crop 및 Classification
      FaceClassifierProcessor faceClassifierProcessor = new FaceClassifierProcessor(context, orgFrame, face);
      emotionResult = faceClassifierProcessor.classifyEmotion();
    }

    /*
    long currentTime = System.currentTimeMillis();
    double duration = (currentTime - startTime) / 1000.0; // 초 단위

    if (duration >= 1.0) {
      double fps = frameCount / duration;
      Log.d("FPS", "Average FACE FPS: " + fps);
      // 시작 시간과 프레임 카운트 리셋
      startTime = currentTime;
      frameCount = 0;
    }
    */
  }

  private static void logExtrasForTesting(Face face) {
    if (face != null) {
      Log.v(MANUAL_TESTING_LOG, "face bounding box: " + face.getBoundingBox().flattenToString());
      Log.v(MANUAL_TESTING_LOG, "face Euler Angle X: " + face.getHeadEulerAngleX());
      Log.v(MANUAL_TESTING_LOG, "face Euler Angle Y: " + face.getHeadEulerAngleY());
      Log.v(MANUAL_TESTING_LOG, "face Euler Angle Z: " + face.getHeadEulerAngleZ());

      // All landmarks
      int[] landMarkTypes =
          new int[] {
            FaceLandmark.MOUTH_BOTTOM,
            FaceLandmark.MOUTH_RIGHT,
            FaceLandmark.MOUTH_LEFT,
            FaceLandmark.RIGHT_EYE,
            FaceLandmark.LEFT_EYE,
            FaceLandmark.RIGHT_EAR,
            FaceLandmark.LEFT_EAR,
            FaceLandmark.RIGHT_CHEEK,
            FaceLandmark.LEFT_CHEEK,
            FaceLandmark.NOSE_BASE
          };
      String[] landMarkTypesStrings =
          new String[] {
            "MOUTH_BOTTOM",
            "MOUTH_RIGHT",
            "MOUTH_LEFT",
            "RIGHT_EYE",
            "LEFT_EYE",
            "RIGHT_EAR",
            "LEFT_EAR",
            "RIGHT_CHEEK",
            "LEFT_CHEEK",
            "NOSE_BASE"
          };
      for (int i = 0; i < landMarkTypes.length; i++) {
        FaceLandmark landmark = face.getLandmark(landMarkTypes[i]);
        if (landmark == null) {
          Log.v(
              MANUAL_TESTING_LOG,
              "No landmark of type: " + landMarkTypesStrings[i] + " has been detected");
        } else {
          PointF landmarkPosition = landmark.getPosition();
          String landmarkPositionStr =
              String.format(Locale.US, "x: %f , y: %f", landmarkPosition.x, landmarkPosition.y);
          Log.v(
              MANUAL_TESTING_LOG,
              "Position for face landmark: "
                  + landMarkTypesStrings[i]
                  + " is :"
                  + landmarkPositionStr);
        }
      }
      Log.v(MANUAL_TESTING_LOG, "face left eye open probability: " + face.getLeftEyeOpenProbability());
      Log.v(MANUAL_TESTING_LOG, "face right eye open probability: " + face.getRightEyeOpenProbability());
      Log.v(MANUAL_TESTING_LOG, "face smiling probability: " + face.getSmilingProbability());
      Log.v(MANUAL_TESTING_LOG, "face tracking id: " + face.getTrackingId());
    }
  }

  @Override
  protected void onFailure(@NonNull Exception e) {
    Log.e(TAG, "Face detection failed " + e);
    emotionResult = null;
  }
}
