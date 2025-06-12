package com.google.mlkit.vision.demo.java.facedetector;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class FaceClassifierProcessor {

    private static final String TAG = "FaceClassifierProcessor";
    private Bitmap frame;
    private Face face;
    private FaceGraphic faceGraphic;
    private ImageLabeler emotionClassifier;
    private int x, y, width, height;

    Context context;

    private EmotionTfLiteClassifier emotionClassifierTfLite =null;
    public static class EmotionResult
    {
        String label;
        float score;
        int x;
        int y;
        int width;
        int height;

        public EmotionResult(String label, float score, int x, int y, int width, int height)
        {
            this.label = label;
            this.score = score;
            this.x = x ;
            this.y = y;
            this.width = width;
            this.height = height;

        }

        public int getFaceCoord_X() {return this.x;}
        public int getFaceCoord_Y() {return this.y;}
        public int getFaceCoord_W() {return this.width;}
        public int getFaceCoord_H() {return this.height;}
        public String getEmotionLabel(){ return this.label; }
        public float getEmotionScore() { return this.score;}

    }

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }
    public FaceClassifierProcessor(Context context, Bitmap frame, Face face) throws IOException {
        this.frame = frame;
        this.face = face;
        this.context = context;


        emotionClassifierTfLite =new EmotionTfLiteClassifier(context);

    }

    public Bitmap extractFace()
    {
        /*
        float x = faceGraphic.translateX(face.getBoundingBox().centerX());
        float y = faceGraphic.translateY(face.getBoundingBox().centerY());

        float left = x - faceGraphic.scale(face.getBoundingBox().width() / 2.0f);
        float top = y - faceGraphic.scale(face.getBoundingBox().height() / 2.0f);
        float right = x + faceGraphic.scale(face.getBoundingBox().width() / 2.0f);
        float bottom = y + faceGraphic.scale(face.getBoundingBox().height() / 2.0f);

        if (left < 0)
            left = 0;
        if (top < 0)
            top = 0;
        */
        int startX = face.getBoundingBox().left;
        int startY = face.getBoundingBox().top;
        int width = face.getBoundingBox().width();
        int height = face.getBoundingBox().height();

        if (startX < 0)
            startX = 0;
        if (startX > frame.getWidth())
            startX = frame.getWidth();
        if (startY < 0)
            startY = 0;
        if(startY > frame.getHeight())
            startY = frame.getHeight();

        if (startX + width > frame.getWidth())
            width = frame.getWidth() - startX;
        if (startY + height > frame.getHeight())
            height = frame.getHeight() - startY;

        float size = width * height;
        float ratio = (size / (frame.getWidth() * frame.getHeight())) * 100.f;
        Log.d("FACE", width + ", " + height + " [" + ratio + "]");
        this.x = startX;
        this.y = startY;
        this.width = width;
        this.height = height;

        Bitmap croppedFace = Bitmap.createBitmap(frame, startX, startY, width, height);
        return croppedFace;

        //for test
        //String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/crop_img" + "/cropped_image.png";
        //saveBitmapToFile(croppedFace, filePath);
    }
    public EmotionResult classifyEmotion()
    {
        Bitmap faceImg = extractFace();
        //test1. resize
        Bitmap resultBitmap = Bitmap.createScaledBitmap(faceImg, emotionClassifierTfLite.getImageSizeX(), emotionClassifierTfLite.getImageSizeY(), false);
        ClassifierResult res = emotionClassifierTfLite.classifyFrame(resultBitmap);

        EmotionData emotion = (EmotionData) res;
        EmotionResult result = new EmotionResult(emotion.emotion_label, emotion.emotion_score, x, y, width, height);

        return result;
    }
    public void classifyEmotion2(Callback<EmotionResult> callback)
    {
        Bitmap faceImg = extractFace();
        InputImage emotionImage = InputImage.fromBitmap(faceImg, 0);
        emotionClassifier.process(emotionImage).addOnSuccessListener(emotion -> {
            List<ImageLabel> imageLabels = emotion;
            Log.d("VENUS", "Len: " + imageLabels.size());
            String emotion_label = "";
            float confidence = 0;

            for (ImageLabel imageLabel : imageLabels) {
                emotion_label = imageLabel.getText();
                confidence = imageLabel.getConfidence();
                confidence = sigmoid(confidence);

               // Log.d("VENUS", "emotion_label: "  + emotion_label + "[" + confidence +"]");
            }
            EmotionResult result = new EmotionResult(emotion_label, confidence, x, y, width, height);
            callback.onSuccess(result);
        }).addOnFailureListener(e -> {
            callback.onError(e);
        });
    }

    // Bitmap을 파일로 저장하는 메서드
    public static void saveBitmapToFile(Bitmap bitmap, String filePath) {
        File file = new File(filePath);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static float sigmoid(float x){
        return (float) (1 / (1 + Math.exp(-x)));
    }
}
