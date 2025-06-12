package com.google.mlkit.vision.demo.java.viewmanager;

import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.google.mlkit.vision.demo.java.facedetector.FaceClassifierProcessor;
import com.google.mlkit.vision.demo.java.posedetector.PoseDetectorProcessor;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.pose.PoseLandmark;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KETIDetector {
    private static final String TAG = "KETIDetector";
    private static Queue<String> emotion_queue = new LinkedList<>();
    private static Queue<String> action_queue = new LinkedList<>();
    private static Queue<String> pose_queue = new LinkedList<>();
    private static String preEmo = null;

    private String curEmo = null;
    private String curPose = null;
    private int curReps = 0;
    private boolean isHumanDetected = false;
    private boolean isPoseDetected = false;

    private Handler handler;

    private Handler timerHandler;
    private Runnable runnable;
    private int intervalMillis;
    private boolean isEmotionRegular = true;
    private boolean isEmotionEvent = true;
    private boolean isActionEvent = true;
    private boolean isPoseRegular = false;
    private VisionImageProcessor face_processor;
    private VisionImageProcessor pose_processor;
    private VisionImageProcessor action_processor;
    //Venus
    private int pre_repetition  = -1;

    private int action_event_timer = 0;

    class PoseExercise{
        String excercise;
        int repetition;
        String pose;
        float socre;
    }
    public KETIDetector(Handler handler)
    {
        this.handler = handler;

        //for timer
        this.intervalMillis = 1000;
        this.timerHandler = new Handler(Looper.getMainLooper());
        this.runnable = new Runnable() {
            @Override
            public void run() {
                // 1초에 한 번 실행할 코드 작성

                if(curEmo != null && isEmotionRegular && isHumanDetected)
                {
                    Message message = handler.obtainMessage();
                    message.what = KETIDetectorConstants.EMOTION_REGULAR;
                    message.obj = curEmo;
                    handler.sendMessage(message);
                }

                if(curPose != null)
                {
                    Message message = handler.obtainMessage();
                    message.what = KETIDetectorConstants.POSE_REGULAR;
                    message.obj = curPose;
                    handler.sendMessage(message);
                }

                //사람 검출되었는지 확인 메세지
                Message message_human = handler.obtainMessage();
                message_human.what = KETIDetectorConstants.HUMAN_DETECTED;
                message_human.obj = isHumanDetected;
                handler.sendMessage(message_human);

                // 다음 실행 예약
                timerHandler.postDelayed(this, intervalMillis);

                //Action Event Timer
                if(action_event_timer != 0)
                    action_event_timer -= 1;
            }
        };
        timerHandler.postDelayed(runnable, intervalMillis);
    }

    public void initializeProcessors(Map<Integer, VisionImageProcessor> frameProcessors) {
        face_processor = frameProcessors.get(KETIDetectorConstants.FACE_DETECTOR_PROCESSOR);
        pose_processor = frameProcessors.get(KETIDetectorConstants.POSE_DETECTOR_PROCESSOR);
        action_processor = frameProcessors.get(KETIDetectorConstants.ACTION_DETECTOR_PROCESSOR_TMP);
    }
    

    public void collectResult(Map<Integer, VisionImageProcessor> frameProcessors)
    {
        FaceClassifierProcessor.EmotionResult faceResult = null;
        List<DetectedObject> actionResult = null;
        PoseDetectorProcessor.PoseWithClassification poseResult = null;

        //1. 감정인식 처리 (스켈레톤 유무와 상관없음)
        faceResult = (FaceClassifierProcessor.EmotionResult)face_processor.getKETIResult();
        if (faceResult != null)
        {

            String emotionResult = SlidingWindow(faceResult.getEmotionLabel(), emotion_queue, KETIDetectorConstants.FACE_DETECTOR_SLIDING_WINDOW_SIZE);
            curEmo = emotionResult;

            if (emotionResult != null) {
                if (!emotionResult.equals(preEmo)) {
                    //System.out.println("감정변화: " + preEmo + "에서 " + emotionResult + "로 변화");
                    if(!emotionResult.equals("Disgust") && !emotionResult.equals("Neutral") && !emotionResult.equals("")){
                        if(isEmotionEvent & isHumanDetected)
                        {
                            Message message = handler.obtainMessage();
                            message.what = KETIDetectorConstants.EMOTION_EVENT;
                            message.obj = emotionResult;
                            handler.sendMessage(message);
                        }
                    }
                    preEmo = emotionResult;
                }
            }
        }
        else
        {
            //이미지에서 얼굴이 인식 안되는 경우, 라벨 넣지 않고 처리
            String emotionResult = SlidingWindow("", emotion_queue, KETIDetectorConstants.FACE_DETECTOR_SLIDING_WINDOW_SIZE);
            curEmo = emotionResult;
        }



        //2. 포즈 인식 처리
        poseResult = (PoseDetectorProcessor.PoseWithClassification)pose_processor.getKETIResult();
        //valid 한 사람인지 확인 하기 위해 faceResult 값 확인함
        if (poseResult != null) {

            PoseExercise result = getPoseResult(poseResult);

            if(result != null)
            {
                //1) 운동이 인식 되었다면 (repetition 증가)
                if(pre_repetition != result.repetition)
                {
                    pre_repetition = result.repetition;
                    Message message = handler.obtainMessage();
                    message.what = KETIDetectorConstants.POSE_EVENT;
                    message.obj = result.excercise;
                    handler.sendMessage(message);
                }

                //2) 현재포즈 (슬라이드윈도우 사용)
                String pose;
                if(result.socre >= 0.5)      // confidence가 높은 포즈가 인식되었다면
                {
                    //stand, sit, lie, squat_down, squat_up, push_up, push_down
                    //stand, squat_up | sit, squat_down | lie, push_up, push_down
                    //결과가 운동으로 나올 때, pose로 맵핑

                    if(result.pose.contains("squat_up"))
                        pose = "standing";
                    else if(result.pose.contains("squat_down"))
                        pose = "sit";
                    else if(result.pose.contains("push"))
                        pose = "lie";
                    else
                        pose = result.pose;

                    curPose = SlidingWindow(pose, pose_queue, KETIDetectorConstants.POSE_DETECTOR_SLIDING_WINDOW_SIZE);
                }
                else // confidence가 낮으면, 포즈 인식이 안된것으로 처리
                {
                    pose = "";
                    curPose = SlidingWindow(pose, pose_queue, KETIDetectorConstants.POSE_DETECTOR_SLIDING_WINDOW_SIZE);
                }
            }
            else {
                curPose = SlidingWindow("", pose_queue, KETIDetectorConstants.POSE_DETECTOR_SLIDING_WINDOW_SIZE);
            }

    /*
           if (poseEvent != null) {
                isPoseDetected = true;
                if (!isPoseRegular)      //운동이면
                {
                    Message message = handler.obtainMessage();
                    message.what = KETIDetectorConstants.POSE_EVENT;
                    message.obj = poseEvent;
                    handler.sendMessage(message);
                } else {
                    curPose = poseEvent;
                }
            } else
                isPoseDetected = false;
    */
        }
        else
        {
            //이미지에서 스켈레톤이 인식 안되는 경우, 라벨 넣지 않고 처리
            curPose = SlidingWindow("", pose_queue, KETIDetectorConstants.POSE_DETECTOR_SLIDING_WINDOW_SIZE);
        }

        //3. 행동 인식 처리
        actionResult = (List<DetectedObject>)action_processor.getKETIResult();
        if(actionResult != null)
        {
            if(actionResult.size() == 1  && (action_event_timer == 0))
            {
                DetectedObject action = actionResult.get(0);
                String actionClass = action.getLabels().get(0).getText();
                float confidence = action.getLabels().get(0).getConfidence();

                if(actionClass.equals("nothing"))
                {
                    //nothing는 confidence 고려 안함, queue에 넣긴 넣어야 함
                    SlidingWindow(actionClass, action_queue, KETIDetectorConstants.ACTION_DETECTOR_SLIDING_WINDOW_SIZE);
                }
                else if(actionClass.equals("reading") | actionClass.equals("drinking"))
                {
                    if(confidence > 0.8)
                    {
                        String actionEvent = SlidingWindow(actionClass, action_queue, KETIDetectorConstants.ACTION_DETECTOR_SLIDING_WINDOW_SIZE);
                        if(actionEvent != null && !actionEvent.equals("") && !(actionEvent.equals("nothing"))/* && !(prevAction.equals(actionEvent))*/)
                        {
                            Message message = handler.obtainMessage();
                            message.what = KETIDetectorConstants.ACTION_EVENT;
                            message.obj = actionEvent;
                            handler.sendMessage(message);
                            //queue 비우기
                            action_queue.clear();
                            //5초 이내에 재인식 못하게 함
                            action_event_timer = 5;
                            //prevAction = actionEvent;
                         }
                    }
                }
            }
            else if(actionResult.size() >= 2)
                Log.d(TAG, "check actionResult size!!");
        }

        //사람 검출 확인
        if (poseResult != null && faceResult != null)
        {
            if (isValidHuman(faceResult, poseResult)) {
                isHumanDetected = true;
            } else
                isHumanDetected = false;
        }
    }

    // 감정인식으로 검출한 얼굴과, Pose 인식으로 검출한 얼굴의 위치가 같은지 확인함
    public boolean isValidHuman(FaceClassifierProcessor.EmotionResult faceResult, PoseDetectorProcessor.PoseWithClassification poseResult){
        if(faceResult != null && poseResult != null){
            RectF faceBoundingBox = new RectF(faceResult.getFaceCoord_X(), faceResult.getFaceCoord_Y(),faceResult.getFaceCoord_X() + faceResult.getFaceCoord_W(),faceResult.getFaceCoord_Y() + faceResult.getFaceCoord_H());
            PoseLandmark nose = poseResult.getPose().getPoseLandmark(PoseLandmark.NOSE);
            PoseLandmark left_sholder = poseResult.getPose().getPoseLandmark(PoseLandmark.LEFT_SHOULDER);
            PoseLandmark right_sholder = poseResult.getPose().getPoseLandmark(PoseLandmark.RIGHT_SHOULDER);
            PoseLandmark left_mouth = poseResult.getPose().getPoseLandmark(PoseLandmark.LEFT_MOUTH);
            PoseLandmark right_mouth = poseResult.getPose().getPoseLandmark(PoseLandmark.RIGHT_MOUTH);

            if (nose != null && left_mouth != null && right_mouth != null && right_sholder != null && left_sholder != null){
                if (faceBoundingBox.contains(nose.getPosition3D().getX(), nose.getPosition3D().getY())
                        && faceBoundingBox.contains(left_mouth.getPosition3D().getX(), left_mouth.getPosition3D().getY())
                        && faceBoundingBox.contains(right_mouth.getPosition3D().getX(), right_mouth.getPosition3D().getY())
                        && !faceBoundingBox.contains(left_sholder.getPosition3D().getX(), left_sholder.getPosition3D().getY())
                        && !faceBoundingBox.contains(right_sholder.getPosition3D().getX(), right_sholder.getPosition3D().getY())){
                    return true;
                }
            }
        }
        return false;
    }


    // 태현 0119 새로 검출된 action 정보를 appending 하고 지정된 사이즈를 넘기면 첫 번째 원소 제거
    public ArrayList<String> addActionAndMaintainSize(ArrayList<String> Actions, String new_action){

        if (Actions.size() >= 99){
            Actions.remove(0);
        }
        Actions.add(new_action);
        return Actions;
    }

    // 태현 0119 Actions 들 중 최빈 action을 검출
    public String findMostCommonAction(ArrayList<String> Actions) {
        Map<String, Integer> frequencyMap = new HashMap<>();

        for (String action : Actions) {
            frequencyMap.put(action, frequencyMap.getOrDefault(action, 0) + 1);
        }

        String mostCommon = null;
        int maxCount = 0;
        for (Map.Entry<String, Integer> entry : frequencyMap.entrySet()) {
            if (entry.getValue() > maxCount) {
                mostCommon = entry.getKey();
                maxCount = entry.getValue();
            }
        }

        return mostCommon;
    }

    public PoseExercise getPoseResult(PoseDetectorProcessor.PoseWithClassification poseResult){

        PoseExercise result = new PoseExercise();
        //String poseClass = null;

        float leftShoulder_p = poseResult.getPose().getPoseLandmark(PoseLandmark.LEFT_SHOULDER).getInFrameLikelihood();
        float rightShoulder_p = poseResult.getPose().getPoseLandmark(PoseLandmark.RIGHT_SHOULDER).getInFrameLikelihood();
        float leftElbow_p = poseResult.getPose().getPoseLandmark(PoseLandmark.LEFT_ELBOW).getInFrameLikelihood();
        float rightElbow_p = poseResult.getPose().getPoseLandmark(PoseLandmark.RIGHT_ELBOW).getInFrameLikelihood();
        float leftHip_p = poseResult.getPose().getPoseLandmark(PoseLandmark.LEFT_HIP).getInFrameLikelihood();
        float rightHip_p = poseResult.getPose().getPoseLandmark(PoseLandmark.RIGHT_HIP).getInFrameLikelihood();
        float leftKnee_p = poseResult.getPose().getPoseLandmark(PoseLandmark.LEFT_KNEE).getInFrameLikelihood();
        float rightKnee_p = poseResult.getPose().getPoseLandmark(PoseLandmark.RIGHT_KNEE).getInFrameLikelihood();

        if(leftShoulder_p >= 0.2 && rightShoulder_p >= 0.2 && leftElbow_p >= 0.2 && rightElbow_p >= 0.2 && leftHip_p >= 0.2 && rightHip_p >= 0.2 && leftKnee_p >= 0.2 && rightKnee_p >= 0.2)
        {
            String input = poseResult.getClassificationResult().toString();
            //인식이 되지 않으면 input은 []] 임. String 값 없을 때 처리 추가
            //Log.d(TAG,  input);
            if(!input.equals("[]"))
            {
                input = input.replaceAll("^\\s*\\[|\\]\\s*$", "");  // 양쪽 대괄호와 공백 제거
                input = input.replaceAll("\\s+", "");  // 내부의 모든 공백 제거
                String[] parts = input.split(",");
                //parts[0]은 운동 반복횟수 정보, parts[1]는 현재 포즈 정보

                //1) 운동 반복 들어왔는지 확인
                if(!parts[0].equals(""))
                {
                    String exercise[] = parts[0].split(":");
                    if(exercise[0].contains("squat"))
                        result.excercise = "squat";
                    else if(exercise[0].contains("push"))
                        result.excercise = "push up";

                    //숫자찾기
                    Pattern pattern = Pattern.compile("\\d+");
                    Matcher matcher = pattern.matcher(exercise[1]);
                    if (matcher.find()) {
                        String tmp = matcher.group();
                        result.repetition = Integer.parseInt(tmp);
                    }
                    else
                        Log.d(TAG, "Value error. Reputation check");
                }

                //2) 포즈인식 들어왔는지 확인
                if(!parts[1].equals(""))
                {
                    String pose[] = parts[1].split(":");
                    result.pose = pose[0];

                    //숫자찾기
                    Pattern pattern = Pattern.compile("\\d+\\.\\d+");
                    Matcher matcher = pattern.matcher(pose[1]);
                    if (matcher.find()) {
                        String tmp = matcher.group();
                        result.socre = Float.parseFloat(tmp);
                    }
                    else
                        Log.d(TAG, "Value error. Score check");
                }

/*
                //org
                //if(isPoseExercise)
                //{
                Pattern pattern = Pattern.compile("\\d+");
                Matcher matcher = pattern.matcher(parts[1]);
                if (matcher.find()) {
                    String conf = matcher.group();
                    if (conf == null) {
                        return null;
                    } else {
                        double confidence = Integer.valueOf(conf);
                        if (confidence < 0.6)
                            return null;

                        String tmp[] = parts[1].split(":");
                        if(!tmp[0].contains("push"))
                        {
                            if(tmp[0].contains("squat_down"))
                                poseClass = "sit";
                            else
                                poseClass = tmp[0];
                            isPoseRegular = true;
                        }
                    }
                }

                //Reputation 추출
                pattern = Pattern.compile("\\d+");
                matcher = pattern.matcher(parts[0]);
                if (matcher.find()) {
                    String rep = matcher.group();
                    if (rep == null) {
                        return null;
                    }
                    else {

                        int t = Integer.valueOf(rep);
                        if (pre_repetition != t) {
                            pre_repetition = t;
                            String tmp[] = parts[0].split(":");
                            if (tmp[0].contains("squat")){
                                poseClass = "squat";
                            }

                            else if (tmp[0].contains("push"))
                            {
                                poseClass = "pushup";
                            }
                            isPoseRegular = false;
                        }
                    }
                }

 */
            }
        }
        else
            result = null;
        return result;
    }
    public String SlidingWindow (String label, Queue<String> queue, int sliding_size){

        String result = "";
        queue.offer(label);

        // 프레임 수가 모이지 않으면 사용 불가
        if (sliding_size > queue.size()) {
            return null;
        }
        else if(queue.size()-1>=sliding_size) {
            queue.poll();

            // 최빈값 메소드 호출
            result = SortingWindow(queue);

        }
        return result;
    }

    public String SortingWindow(Queue<String> windows) {
        Map<String, Integer> keyMap = new HashMap<>(); // 단어의 빈도를 저장할 맵

        // 리스트의 단어를 맵에 추가하고 빈도수 기록
        for (String window : windows) {
            keyMap.put(window, keyMap.getOrDefault(window, 0) + 1);
        }

        // 최대값 value 찾기
        int maxValue = Integer.MIN_VALUE;
        String maxKey = null;

        for (Map.Entry<String, Integer> entry : keyMap.entrySet()) {
            if (entry.getValue() >= maxValue) {
                maxValue = entry.getValue();
                maxKey = entry.getKey();
            }
        }

        return maxKey;
    }

    public void setMode(int mode)
    {
        switch(mode)
        {
            case KETIDetectorConstants.EMOTION_REGULAR_OFF:
                isEmotionRegular = false;
                break;
            case KETIDetectorConstants.EMOTION_REGULAR_ON:
                isEmotionRegular = true;
                break;
            case KETIDetectorConstants.EMOTION_EVENT_OFF:
                isEmotionEvent = false;
                break;
            case KETIDetectorConstants.EMOTION_EVENT_ON:
                isEmotionEvent = true;
                break;
            case KETIDetectorConstants.ACTION_EVENT_OFF:
                isActionEvent = false;
                break;
            case KETIDetectorConstants.ACTION_EVENT_ON:
                isActionEvent = true;
                break;
                //todo 여기 버그
            case KETIDetectorConstants.POSE_REGULAR_ON:
                //isPoseRegular = true;
                //isPoseExercise = false;
                break;
            case KETIDetectorConstants.POSE_EXERCISE_ON:
                //isPoseRegular = false;
                //isPoseExercise = true;
                break;
        }
    }
}
