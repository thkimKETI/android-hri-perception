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

package com.google.mlkit.vision.demo.java;

import android.graphics.Color;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import com.google.android.gms.common.annotation.KeepName;
import com.google.mlkit.common.model.LocalModel;

import com.google.mlkit.vision.demo.java.viewmanager.CameraSource;
import com.google.mlkit.vision.demo.java.viewmanager.CameraSourcePreview;
import com.google.mlkit.vision.demo.java.viewmanager.GraphicOverlay;
import com.google.mlkit.vision.demo.java.viewmanager.KETIDetectorConstants;
import com.google.mlkit.vision.demo.java.viewmanager.PreferenceUtils;
import com.google.mlkit.vision.demo.R;
import com.google.mlkit.vision.demo.java.facedetector.FaceDetectorProcessor;
import com.google.mlkit.vision.demo.java.objectdetector.ObjectDetectorProcessor;
import com.google.mlkit.vision.demo.java.posedetector.PoseDetectorProcessor;
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions;
import com.google.mlkit.vision.pose.PoseDetectorOptionsBase;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@KeepName
public final class LivePreviewActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {
  private static final String HUMAN_ROBOT_INTERACTION = "Human Robot Interaction";

  private static final String TAG = "LivePreviewActivity";
  private CameraSource cameraSource = null;
  private CameraSourcePreview preview;
  private GraphicOverlay graphicOverlay;
  private String selectedModel = HUMAN_ROBOT_INTERACTION;
  TextView textview_emotion_regular;
  TextView textview_emotion_event;
  TextView textview_pose_regular;
  TextView textview_pose_exercise;
  ImageButton imageButtonHuman;
  int poseMode = KETIDetectorConstants.POSE_REGULAR_ON;;

  Handler handler = new Handler(Looper.getMainLooper()) {
    @Override
    public void handleMessage(Message msg) {
      super.handleMessage(msg);

      Date currentDate = new Date();
      SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
      String formattedDateTime = dateFormat.format(currentDate);

      switch(msg.what){
        case KETIDetectorConstants.EMOTION_REGULAR:
          String emotion_regular = (String) msg.obj;
          if(!emotion_regular.equals(""))
            textview_emotion_regular.setText(emotion_regular + "\n" + formattedDateTime);
          else
            textview_emotion_regular.setText("");
          break;

        case KETIDetectorConstants.EMOTION_EVENT:
          String emotion_event = (String) msg.obj;
          changeTextWithBlink(emotion_event+ "\n" + formattedDateTime, textview_emotion_event);
          break;

        case KETIDetectorConstants.ACTION_EVENT:
          String action_event = (String) msg.obj;
          showToast(action_event);
          break;

        case KETIDetectorConstants.POSE_REGULAR:
          String pose_regular = (String) msg.obj;
          //textview_pose_regular.setTextColor(Color.BLUE);
          if(!pose_regular.equals(""))
            textview_pose_regular.setText(pose_regular + "\n" + formattedDateTime);
          else
            textview_pose_regular.setText("");
          break;
        case KETIDetectorConstants.POSE_EVENT:
          String pose_event = (String) msg.obj;
          //textview_pose_regular.setTextColor(Color.RED);
          changeTextWithBlink(pose_event + "\n" + formattedDateTime, textview_pose_exercise);
          break;
        case KETIDetectorConstants.HUMAN_DETECTED:
          boolean isHumanDetected = (boolean)msg.obj;
          if(!isHumanDetected)
          {
            textview_emotion_regular.setText("");
            textview_emotion_event.setText("");
            imageButtonHuman.setBackgroundResource(R.drawable.human_g);
          }
          else
          {
            imageButtonHuman.setBackgroundResource(R.drawable.human);
          }
          //아이콘 바꾸고 텍스트 바꾸기
          break;
        /*
        case KETIDetectorConstants.POSE_DETECTED:
          boolean isPoseDetected = (boolean)msg.obj;
          if(!isPoseDetected)
          {
            imageButtonSkeleton.setBackgroundResource(R.drawable.skeleton_g);
            textview_pose_regular.setText("");
          }
          else
          {
            imageButtonSkeleton.setBackgroundResource(R.drawable.skeleton);
          }
          break;
        */
      }
    }
  };
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(TAG, "onCreate");

    setContentView(R.layout.activity_vision_live_preview);

    preview = findViewById(R.id.preview_view);
    if (preview == null) {
      Log.d(TAG, "Preview is null");
    }
    graphicOverlay = findViewById(R.id.graphic_overlay);
    if (graphicOverlay == null) {
      Log.d(TAG, "graphicOverlay is null");
    }


    ToggleButton facingSwitch = findViewById(R.id.facing_switch);
    facingSwitch.setOnCheckedChangeListener(this);


    ToggleButton emotion_regular_switch = findViewById(R.id.emotion_regular);
    emotion_regular_switch.setOnCheckedChangeListener(this);
    ToggleButton emotion_event_switch = findViewById(R.id.emotion_event);
    emotion_event_switch.setOnCheckedChangeListener(this);
    ToggleButton action_event_switch = findViewById(R.id.action_event);
    action_event_switch.setOnCheckedChangeListener(this);
    ToggleButton pose_regular_switch = findViewById(R.id.pose_regular);
    pose_regular_switch.setOnCheckedChangeListener(this);

    textview_emotion_regular = findViewById(R.id.text_emotion_regular);
    textview_emotion_event = findViewById(R.id.text_emotion_event);
    textview_pose_regular = findViewById(R.id.text_pose_regular);
    textview_pose_exercise = findViewById(R.id.text_pose_exercise);

    imageButtonHuman = findViewById(R.id.imagebutton_human);

    //createCameraSource(selectedModel);

  }

  @Override
  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    switch (buttonView.getId()) {
      case R.id.facing_switch:
        if (cameraSource != null) {
          if (isChecked) {
            cameraSource.setFacing(CameraSource.CAMERA_FACING_FRONT);
          } else {
            cameraSource.setFacing(CameraSource.CAMERA_FACING_BACK);
          }
        }
        preview.stop();
        startCameraSource();
        break;
      case R.id.emotion_regular:
        if (isChecked) {
          cameraSource.setModeOnOff(KETIDetectorConstants.EMOTION_REGULAR_ON);
        } else {
          cameraSource.setModeOnOff(KETIDetectorConstants.EMOTION_REGULAR_OFF);
          textview_emotion_regular.setText("");
        }
        break;
      case R.id.emotion_event:
        if (isChecked) {
          cameraSource.setModeOnOff(KETIDetectorConstants.EMOTION_EVENT_ON);
        } else {
          cameraSource.setModeOnOff(KETIDetectorConstants.EMOTION_EVENT_OFF);
          textview_emotion_event.setText("");
        }
        break;
      case R.id.action_event:
        if (isChecked) {
          cameraSource.setModeOnOff(KETIDetectorConstants.ACTION_EVENT_ON);
        } else {
          cameraSource.setModeOnOff(KETIDetectorConstants.ACTION_EVENT_OFF);
        }
        break;
      case R.id.pose_regular:
        if (isChecked) {
          poseMode = KETIDetectorConstants.POSE_REGULAR_ON;
          cameraSource.setModeOnOff(KETIDetectorConstants.POSE_REGULAR_ON);
          textview_pose_regular.setText("");
        } else {
          poseMode = KETIDetectorConstants.POSE_EXERCISE_ON;
          cameraSource.setModeOnOff(KETIDetectorConstants.POSE_EXERCISE_ON);
          textview_pose_regular.setText("");
        }
        break;
    }

  }

  private void createCameraSource(String model) {
    // If there's no existing cameraSource, create one.
    if (cameraSource == null) {
      cameraSource = new CameraSource(this, graphicOverlay, handler);

      cameraSource.setFacing(CameraSource.CAMERA_FACING_FRONT);

      try {
        //설정 바꾸고 싶으면 변수 수정하면 됨.

        //1. 감정인식

        Log.i(TAG, "Using Face Detector Processor");
        cameraSource.addMachineLearningFrameProcessor(new FaceDetectorProcessor(this), KETIDetectorConstants.FACE_DETECTOR_PROCESSOR);

        //2. 포즈인식
        PoseDetectorOptionsBase poseDetectorOptions = PreferenceUtils.getPoseDetectorOptionsForLivePreview(this);
        Log.i(TAG, "Using Pose Detector with options " + poseDetectorOptions);
        boolean shouldShowInFrameLikelihood = PreferenceUtils.shouldShowPoseDetectionInFrameLikelihoodLivePreview(this);
        boolean visualizeZ = PreferenceUtils.shouldPoseDetectionVisualizeZ(this);
        boolean rescaleZ = PreferenceUtils.shouldPoseDetectionRescaleZForVisualization(this);
        boolean runClassification = PreferenceUtils.shouldPoseDetectionRunClassification(this);
        cameraSource.addMachineLearningFrameProcessor(new PoseDetectorProcessor(this, poseDetectorOptions, shouldShowInFrameLikelihood, visualizeZ, rescaleZ, runClassification, true), KETIDetectorConstants.POSE_DETECTOR_PROCESSOR);

        //3. 행동인식
        Log.i(TAG, "Using Custom Object Detector Processor");
        LocalModel localModel = new LocalModel.Builder().setAssetFilePath("custom_models/action_model.tflite").build();
        CustomObjectDetectorOptions customObjectDetectorOptions = PreferenceUtils.getCustomObjectDetectorOptionsForLivePreview(this, localModel);
        cameraSource.addMachineLearningFrameProcessor(new ObjectDetectorProcessor(this, customObjectDetectorOptions), KETIDetectorConstants.ACTION_DETECTOR_PROCESSOR_TMP);

        //add 후 init
        cameraSource.initFrameProcessor();

      } catch (RuntimeException e) {
        Log.e(TAG, "Can not create image processor: " + model, e);
        Toast.makeText(
                        getApplicationContext(),
                        "Can not create image processor: " + e.getMessage(),
                        Toast.LENGTH_LONG)
                .show();
      }
    }
  }

  /**
   * Starts or restarts the camera source, if it exists. If the camera source doesn't exist yet
   * (e.g., because onResume was called before the camera source was created), this will be called
   * again when the camera source is created.
   */
  private void startCameraSource() {
    if (cameraSource != null) {
      try {
        if (preview == null) {
          Log.d(TAG, "resume: Preview is null");
        }
        if (graphicOverlay == null) {
          Log.d(TAG, "resume: graphOverlay is null");
        }
        preview.start(cameraSource, graphicOverlay);
      } catch (IOException e) {
        Log.e(TAG, "Unable to start camera source.", e);
        cameraSource.release();
        cameraSource = null;
      }
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    Log.d(TAG, "onResume");
    createCameraSource(selectedModel);
    startCameraSource();
  }

  /**
   * Stops the camera.
   */
  @Override
  protected void onPause() {
    super.onPause();
    preview.stop();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (cameraSource != null) {
      cameraSource.release();
    }
  }

  //Emotion Event textviw animation 함수
  private void changeTextWithBlink(String newText, TextView textView) {
    // 텍스트 변경 전에 반짝임 애니메이션 적용
    AlphaAnimation blinkAnimation = new AlphaAnimation(1.0f, 0.0f);
    blinkAnimation.setDuration(2000); // 반짝임 지속 시간 (0.3초)
    blinkAnimation.setRepeatMode(AlphaAnimation.REVERSE);
    //blinkAnimation.setRepeatCount(1); // 반복 횟수 (1번)

    // 반짝임 애니메이션 리스너 설정 (애니메이션이 끝날 때 새로운 텍스트 설정)
    blinkAnimation.setAnimationListener(new AlphaAnimation.AnimationListener() {
      @Override
      public void onAnimationStart(Animation animation) {
          textView.setText(newText);
      }

      @Override
      public void onAnimationEnd(Animation animation) {
          textView.setText("");
      }

      @Override
      public void onAnimationRepeat(Animation animation) {

      }
    });

    // 텍스트 변경 전에 반짝임 애니메이션 시작
    textView.startAnimation(blinkAnimation);
  }

  public void showToast(String message)
  {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
  }
}

