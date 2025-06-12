package com.google.mlkit.vision.demo.java.posedetector;
import android.util.Log;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseLandmark;

public class SkeletonTracker {
    private static final float DISTANCE_THRESHOLD = 200.0f; // 임계값으로 프레임 간 유사성 판단

    private PoseLandmark previousLeftShoulder;
    private PoseLandmark previousRightShoulder;
    private PoseLandmark previousLeftHip;
    private PoseLandmark previousRightHip;

    private float tmp;
    public boolean isSameSkeleton(Pose currentPose) {
        PoseLandmark currentLeftShoulder = currentPose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER);
        PoseLandmark currentRightShoulder = currentPose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER);
        PoseLandmark currentLeftHip = currentPose.getPoseLandmark(PoseLandmark.LEFT_HIP);
        PoseLandmark currentRightHip = currentPose.getPoseLandmark(PoseLandmark.RIGHT_HIP);

        if (currentLeftShoulder == null || currentRightShoulder == null || currentLeftHip == null || currentRightHip == null) {
            return false;
        }

        if (previousLeftShoulder == null || previousRightShoulder == null || previousLeftHip == null || previousRightHip == null) {
            saveCurrentPoseLandmarks(currentLeftShoulder, currentRightShoulder, currentLeftHip, currentRightHip);
            return true;
        }

        boolean isLeftShoulderSimilar = calculateDistance(currentLeftShoulder, previousLeftShoulder) <= DISTANCE_THRESHOLD;
        boolean isRightShoulderSimilar = calculateDistance(currentRightShoulder, previousRightShoulder) <= DISTANCE_THRESHOLD;
        boolean isLeftHipSimilar = calculateDistance(currentLeftHip, previousLeftHip) <= DISTANCE_THRESHOLD;
        boolean isRightHipSimilar = calculateDistance(currentRightHip, previousRightHip) <= DISTANCE_THRESHOLD;

        saveCurrentPoseLandmarks(currentLeftShoulder, currentRightShoulder, currentLeftHip, currentRightHip);

        return isLeftShoulderSimilar && isRightShoulderSimilar && isLeftHipSimilar && isRightHipSimilar;
    }

    private void saveCurrentPoseLandmarks(PoseLandmark leftShoulder, PoseLandmark rightShoulder, PoseLandmark leftHip, PoseLandmark rightHip) {
        this.previousLeftShoulder = leftShoulder;
        this.previousRightShoulder = rightShoulder;
        this.previousLeftHip = leftHip;
        this.previousRightHip = rightHip;
    }

    private float calculateDistance(PoseLandmark point1, PoseLandmark point2) {
        float dx = point1.getPosition().x - point2.getPosition().x;
        float dy = point1.getPosition().y - point2.getPosition().y;
        tmp = (float) Math.sqrt(dx * dx + dy * dy);
        return tmp;
    }
}



