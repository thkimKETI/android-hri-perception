package com.google.mlkit.vision.demo.java.posedetector;

import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseLandmark;

public class PoseValidator {

    private static final float DISTANCE_THRESHOLD = 0.1f; // 스켈레톤에서 몸통의 비율 임계값
    private static final float SLOPE_THRESHOLD = 1.0f; // 기울기 임계값 (1.0이면 약 45도)

    public static boolean isPoseValid(Pose pose, int imageWidth, int imageHeight) {
        if (pose == null) {
            return false;
        }

        PoseLandmark rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER);
        PoseLandmark rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP);
        PoseLandmark leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER);
        PoseLandmark leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP);

        if (rightShoulder == null || rightHip == null || leftShoulder == null || leftHip == null) {
            return false;
        }

        // 물구나무 자세 확인: 어깨가 엉덩이보다 아래쪽(y 좌표가 큼)일 경우 물구나무 자세
        if (rightShoulder.getPosition().y > rightHip.getPosition().y && leftShoulder.getPosition().y > leftHip.getPosition().y) {
            System.out.println("TT: Handstand detected - Pose is invalid.");
            return false;
        }

        // 오른쪽 어깨와 오른쪽 엉덩이 사이의 거리 계산
        float rightDistance = calculateDistance(rightShoulder, rightHip);

        // 왼쪽 어깨와 왼쪽 엉덩이 사이의 거리 계산
        float leftDistance = calculateDistance(leftShoulder, leftHip);

        // 오른쪽 어깨와 엉덩이의 기울기 계산
        float rightSlope = calculateSlope(rightShoulder, rightHip);

        // 왼쪽 어깨와 엉덩이의 기울기 계산
        float leftSlope = calculateSlope(leftShoulder, leftHip);

        // 서있는지 누워있는지 판단 (기울기가 SLOPE_THRESHOLD보다 크면 서 있는 것으로 판단)
        boolean isStanding = Math.abs(rightSlope) > SLOPE_THRESHOLD && Math.abs(leftSlope) > SLOPE_THRESHOLD;

        // 비율 계산
        float rightRatio = isStanding ? rightDistance / imageHeight : rightDistance / imageWidth;
        float leftRatio = isStanding ? leftDistance / imageHeight : leftDistance / imageWidth;

        // 비율 출력
        System.out.println("TT: Right Ratio: " + rightRatio + ", Left Ratio: " + leftRatio);
        System.out.println("TT: Right Slope: " + rightSlope + ", Left Slope: " + leftSlope);
        System.out.println("TT: Is Standing: " + isStanding);
        System.out.println("TT: -----------------------------------------");


        return rightRatio > DISTANCE_THRESHOLD && leftRatio > DISTANCE_THRESHOLD;
    }

    private static float calculateDistance(PoseLandmark point1, PoseLandmark point2) {
        float dx = point1.getPosition().x - point2.getPosition().x;
        float dy = point1.getPosition().y - point2.getPosition().y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private static float calculateSlope(PoseLandmark point1, PoseLandmark point2) {
        float dx = point1.getPosition().x - point2.getPosition().x;
        float dy = point1.getPosition().y - point2.getPosition().y;
        return dy / dx;
    }
}