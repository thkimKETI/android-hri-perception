package com.google.mlkit.vision.demo.java.facedetector;

import java.io.Serializable;


/**
 * Created by avsavchenko.
 */
public class EmotionData implements ClassifierResult,Serializable {
    public float[] emotionScores=null;

    public String emotion_label = "";
    public float emotion_score = 0.0f;

    public EmotionData(){

    }
    public EmotionData(float[] emotionScores){
        this.emotionScores = new float[emotionScores.length];
        System.arraycopy(emotionScores, 0, this.emotionScores, 0, emotionScores.length);

        getEmotion(emotionScores);
    }

    private static String[] emotions={"","Anger", "Disgust", "Fear", "Happiness", "Neutral", "Sadness", "Surprise"};
    public String getEmotion(float[] emotionScores){
        int bestInd=-1;
        if (emotionScores!=null){
            float maxScore=0;
            for(int i=0;i<emotionScores.length;++i){
                if(maxScore<emotionScores[i]){
                    maxScore=emotionScores[i];
                    bestInd=i;
                }
            }
        }
        this.emotion_label = emotions[bestInd+1];
        this.emotion_score = emotionScores[bestInd];
        return emotions[bestInd+1];
    }
    public String toString(){
        return getEmotion(emotionScores);
    }
}
