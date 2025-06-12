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

package com.google.mlkit.vision.demo.java.posedetector;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.android.gms.tasks.Task;
import com.google.android.odml.image.MlImage;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.demo.java.viewmanager.GraphicOverlay;
import com.google.mlkit.vision.demo.java.viewmanager.KETIDetectorConstants;
import com.google.mlkit.vision.demo.java.viewmanager.VisionProcessorBase;
import com.google.mlkit.vision.demo.java.posedetector.classification.PoseClassifierProcessor;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.PoseDetectorOptionsBase;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/** A processor to run pose detector. */
public class PoseDetectorProcessor
    extends VisionProcessorBase<PoseDetectorProcessor.PoseWithClassification> {
  private static final String TAG = "PoseDetectorProcessor";

  private final PoseDetector detector;

  private final boolean showInFrameLikelihood;
  private final boolean visualizeZ;
  private final boolean rescaleZForVisualization;
  private final boolean runClassification;
  private final boolean isStreamMode;
  private final Context context;
  private final Executor classificationExecutor;

  private PoseWithClassification poseResult;
  private PoseClassifierProcessor poseClassifierProcessor;

  //Venus : 운동용, 일상용 Classifier 두개 생성
  private PoseClassifierProcessor poseRegularClassifierProcessor;
  private PoseClassifierProcessor poseExerciseClassifierProcessor;
  private int poseType = KETIDetectorConstants.POSE_REGULAR_ON;
  /** Internal class to hold Pose and classification results. */

  //0805: Tracker 추가하여, 이전프레임과 동일한 Skeleton인지 확인
  SkeletonTracker skeletonTracker = new SkeletonTracker();

  private PoseWithClassification poseWithClassificationInstance;

  private long startTime = 0;
  private int frameCount = 0;
  public static class PoseWithClassification {
    private Pose pose;
    private List<String> classificationResult;
    private boolean isValid;

    public PoseWithClassification(Pose pose, List<String> classificationResult, boolean isValid) {
      this.pose = pose;
      this.classificationResult = classificationResult;
      this.isValid = isValid;
    }

    public boolean isValid() {
      return isValid;
    }
    public Pose getPose() {
      return pose;
    }

    public void update(Pose pose, List<String> classificationResult, boolean isValid)
    {
      this.pose = pose;
      this.classificationResult = classificationResult;
      this.isValid = isValid;
    }

    public List<String> getClassificationResult() {
      return classificationResult;
    }
  }

  public PoseDetectorProcessor(
      Context context,
      PoseDetectorOptionsBase options,
      boolean showInFrameLikelihood,
      boolean visualizeZ,
      boolean rescaleZForVisualization,
      boolean runClassification,
      boolean isStreamMode) {
    super(context);
    this.showInFrameLikelihood = showInFrameLikelihood;
    this.visualizeZ = visualizeZ;
    this.rescaleZForVisualization = rescaleZForVisualization;
    detector = PoseDetection.getClient(options);
    this.runClassification = runClassification;
    this.isStreamMode = isStreamMode;
    this.context = context;
    classificationExecutor = Executors.newSingleThreadExecutor();

  }

  @Override
  public void stop() {
    super.stop();
    detector.close();
  }

  @Override
  public Object getKETIResult() {
    if(poseResult != null &&poseResult.isValid())
      return poseResult;
    else
      return null;
  }

  public void setPoseType(int type)
  {
    poseRegularClassifierProcessor = null ;
    poseType = type;
  }
  //Venus: 사용되지 않음
  @Override
  protected Task<PoseWithClassification> detectInImage(InputImage image) {
    return detector
        .process(image)
        .continueWith(
            classificationExecutor,
            task -> {
              Pose pose = task.getResult();
              List<String> classificationResult = new ArrayList<>();
              if (runClassification) {
                if (poseClassifierProcessor == null) {
                  poseClassifierProcessor = new PoseClassifierProcessor(context, isStreamMode);
                }
                classificationResult = poseClassifierProcessor.getPoseResult(pose);
              }
              return new PoseWithClassification(pose, classificationResult, true);
            });
  }

  @Override
  protected Task<PoseWithClassification> detectInImage(MlImage image) {

    return detector
        .process(image)
        .continueWith(
            classificationExecutor,
            task -> {
              long frameStartTime = System.currentTimeMillis();
              Pose pose = task.getResult();

              //0730: skeleton의 유효성 검증
              //사람으로 판단되는 skeleton만 포즈인식 추정, 너무 작은 스켈레톤, skip
              boolean isValid = PoseValidator.isPoseValid(pose, image.getWidth(), image.getHeight());
              if(!isValid)
                return getReusablePoseWithClassification(null, null, false); // 유효하지 않은 상태 반환

              // Skeleton 추적
              boolean isSameSkeleton = skeletonTracker.isSameSkeleton(pose);
              if (!isSameSkeleton) {
                Log.d("POSE", "Different skeleton detected!!");
                return getReusablePoseWithClassification(null, null, false); // 유효하지 않은 상태 반환
              }

              List<String> classificationResult = new ArrayList<>();
              if (runClassification) {
                //if (poseClassifierProcessor == null) {
                //  poseClassifierProcessor = new PoseClassifierProcessor(context, isStreamMode);
                //}
                if(poseType == KETIDetectorConstants.POSE_REGULAR_ON)
                {
                  if(poseRegularClassifierProcessor == null){
                    poseRegularClassifierProcessor = new PoseClassifierProcessor(context, isStreamMode);
                    poseRegularClassifierProcessor.setCSVFile(KETIDetectorConstants.POSE_REGULAR_ON);
                  }
                  classificationResult = poseRegularClassifierProcessor.getPoseResult(pose);
                }

                else if(poseType == KETIDetectorConstants.POSE_EXERCISE_ON)
                {
                  if(poseExerciseClassifierProcessor == null)
                  {
                    poseExerciseClassifierProcessor = new PoseClassifierProcessor(context, isStreamMode);
                    poseExerciseClassifierProcessor.setCSVFile(KETIDetectorConstants.POSE_EXERCISE_ON);

                  }
                  classificationResult = poseExerciseClassifierProcessor.getPoseResult(pose);
                }

              }

              /*
              // FPS 계산을 위해 시간과 프레임 카운트 업데이트
              if (startTime == 0) {
                startTime = frameStartTime;
              }
              frameCount++;
              */
              return getReusablePoseWithClassification(pose, classificationResult, true);
            });
  }

  @Override
  protected void onSuccess(
      @NonNull PoseWithClassification poseWithClassification,
      @NonNull GraphicOverlay graphicOverlay) {

    if(!poseWithClassification.isValid())
      return;

    //poseWithClassificationInstance
    poseResult = poseWithClassification;
    //Venus: Skeleton 그리는 부분 삭제

    /*
    // 현재 시간과 시작 시간 사이의 차이 계산
    long currentTime = System.currentTimeMillis();
    double duration = (currentTime - startTime) / 1000.0; // 초 단위

    if (duration >= 1.0) {
      double fps = frameCount / duration;
      Log.d("FPS", "Average POSE FPS: " + fps);
      // 시작 시간과 프레임 카운트 리셋
      startTime = currentTime;
      frameCount = 0;
    }
  */
    graphicOverlay.add(
        new PoseGraphic(
            graphicOverlay,
            poseWithClassification.pose,
            showInFrameLikelihood,
            visualizeZ,
            rescaleZForVisualization,
            poseWithClassification.classificationResult));

  }


  @Override
  protected void onFailure(@NonNull Exception e) {
    poseResult = null;
    Log.e(TAG, "Pose detection failed!", e);
  }

  @Override
  protected boolean isMlImageEnabled(Context context) {
    // Use MlImage in Pose Detection by default, change it to OFF to switch to InputImage.
    return true;
  }

  // 재사용 가능한 PoseWithClassification을 반환
  private PoseWithClassification getReusablePoseWithClassification(Pose pose, List<String> classificationResult, boolean isValid) {
    if (poseWithClassificationInstance == null) {
      poseWithClassificationInstance = new PoseWithClassification(pose, classificationResult, isValid);
    } else {
      poseWithClassificationInstance.update(pose, classificationResult, isValid);
    }
    return poseWithClassificationInstance;
  }
}
