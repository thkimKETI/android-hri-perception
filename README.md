# 🤖 KETI Human Robot Interaction (HRI) System

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![API](https://img.shields.io/badge/API-26%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=26)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

An Android application for Human-Robot Interaction research that provides comprehensive human behavior analysis through **Pose Detection**, **Action Recognition**, and **Emotion Analysis** using Google ML Kit and custom object detection models.

## 📸 Demo Screenshots

<div align="center">
  <img src="demo_1.png" width="600" alt="Emotion Recognition Demo"/>
  <br>
  <em>Real-time Emotion Recognition</em>
  <br><br>
  <img src="demo_2.png" width="600" alt="Action and Pose Recognition Demo"/>
  <br>
  <em>Action and Pose Recognition</em>
</div>

## 🎯 Core Features

### 🤸 Pose Detection & Exercise Recognition
Real-time human pose estimation with exercise classification and repetition counting.

**Supported Poses (7 types):**
- `standing` - Standing upright position
- `sit` - Sitting position  
- `lie` - Lying down position
- `pushup_up` - Push-up upper position
- `pushup_down` - Push-up lower position
- `squat_up` - Squat upper position  
- `squat_down` - Squat lower position

**Exercise Recognition:**
- **Push-ups**: Automatic detection and repetition counting
- **Squats**: Automatic detection and repetition counting
- Real-time form analysis with confidence scoring
- Audio feedback on successful repetitions

### 🏃 Action Recognition
Object detection-based recognition of daily activities with high accuracy filtering.

**Supported Actions (2 types):**
- `reading` - Reading books or documents
- `drinking` - Drinking water or beverages

**Recognition Features:**
- Confidence threshold: 80% for reliable detection
- Sliding window processing (50 frames) for stability
- 5-second cooldown to prevent duplicate detections
- Background state handling with `nothing` classification

### 😊 Emotion Analysis
Real-time facial emotion recognition with continuous monitoring and event detection.

**Emotion Detection:**
- Continuous emotion state tracking
- Event-based emotion change alerts
- Integration with facial landmark detection
- Timestamp logging for emotion events

## 🏗️ Architecture

### System Overview
```
Camera Input → Parallel Processing → HRI Analysis → Real-time Output
     ↓              ↓                    ↓              ↓
Live Stream → [Face Detection]    → [Emotion] → UI Display
     ↓        [Pose Detection]    → [Exercise] → Event Triggers  
     ↓        [Object Detection]  → [Action]   → Data Logging
```

### Core Components
```
├── LivePreviewActivity          # Main HRI interface
├── viewmanager/
│   ├── KETIDetector            # Central processing coordinator
│   ├── CameraSource            # Camera stream management
│   ├── GraphicOverlay          # Visual overlay system
│   └── KETIDetectorConstants   # System constants and modes
├── facedetector/
│   ├── FaceDetectorProcessor   # Face detection processing
│   └── FaceClassifierProcessor # Emotion classification
├── posedetector/
│   ├── PoseDetectorProcessor   # Pose detection & analysis
│   └── PoseClassifierProcessor # Exercise classification
└── objectdetector/
    └── ObjectDetectorProcessor # Action recognition
```

### Processing Pipeline
1. **Camera Input** → Real-time video stream capture
2. **Parallel ML Processing** → Simultaneous face, pose, and object detection
3. **HRI Analysis** → KETIDetector coordinates and processes all detection results
4. **Sliding Window Filtering** → Stabilizes recognition results over time
5. **Event Generation** → Triggers for emotion changes, exercise reps, and actions
6. **Output** → Real-time UI updates and data logging

## 🚀 Getting Started

### Prerequisites
- Android Studio Arctic Fox or later
- Android device with API 26+ (Android 8.0+)
- Camera permission for real-time detection
- Minimum 4GB RAM for optimal ML processing performance

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-username/keti-hri-android.git
   cd keti-hri-android
   ```

2. **Open in Android Studio**
   - Import the project
   - Sync Gradle dependencies
   - Build and run on device

### Key Dependencies
```gradle
// ML Kit for core functionality
implementation 'com.google.mlkit:face-detection:16.1.5'
implementation 'com.google.mlkit:pose-detection:18.0.0-beta3'
implementation 'com.google.mlkit:pose-detection-accurate:18.0.0-beta3'
implementation 'com.google.mlkit:object-detection:17.0.0'
implementation 'com.google.mlkit:object-detection-custom:17.0.0'

// Camera framework
implementation "androidx.camera:camera-camera2:1.0.0-SNAPSHOT"
implementation "androidx.camera:camera-lifecycle:1.0.0-SNAPSHOT"
```

## 📱 Usage

### Interface Controls

#### Detection Mode Toggles
- **Emotion Regular**: Continuous emotion monitoring (1-second intervals)
- **Emotion Event**: Event-triggered emotion change alerts
- **Pose Regular**: General pose classification (standing, sitting, lying)
- **Exercise Mode**: Exercise recognition with repetition counting

#### Visual Indicators
- **Human Detection**: Icon changes color when human is detected
- **Real-time Results**: Live display of current pose, emotion, and action
- **Timestamps**: When each detection/event occurs
- **Camera Toggle**: Switch between front/rear camera

### Detection Outputs

#### Pose & Exercise Recognition
```java
// Pose classification results
"standing : 0.95 confidence"
"sit : 0.87 confidence"

// Exercise repetition counting
"pushup_down : 5 reps"
"squat_down : 12 reps"
```

#### Action Recognition
```java
// Action detection with high confidence
"reading : 0.85 confidence"
"drinking : 0.92 confidence"
```

#### Emotion Analysis
```java
// Emotion states with timestamps
"happy : 14:30:25"
"surprised : 14:30:47"
```

## ⚙️ Configuration

### ML Kit Settings
```java
// Pose detection configuration
PoseDetectorOptions poseOptions = new PoseDetectorOptions.Builder()
    .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
    .build();

// Face detection for emotion analysis
FaceDetectorOptions faceOptions = new FaceDetectorOptions.Builder()
    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
    .build();

// Object detection for actions
ObjectDetectorOptions objectOptions = new ObjectDetectorOptions.Builder()
    .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
    .enableClassification()
    .build();
```

### Recognition Parameters
```java
// Sliding window sizes for stability
FACE_DETECTOR_SLIDING_WINDOW_SIZE = 50;
ACTION_DETECTOR_SLIDING_WINDOW_SIZE = 50;
POSE_DETECTOR_SLIDING_WINDOW_SIZE = 20;

// Confidence thresholds
ACTION_CONFIDENCE_THRESHOLD = 0.8f;
POSE_CONFIDENCE_THRESHOLD = 0.5f;

// Event cooldown timers
ACTION_EVENT_COOLDOWN = 5; // seconds
```

## 📊 Technical Specifications

### Supported Platforms
- **Android API 26+** (Android 8.0 and above)
- **Target SDK**: API 31 (Android 12)
- **Architecture**: ARM64, ARMv7

### Performance Metrics
- **Detection Latency**: <100ms for real-time processing
- **Frame Rate**: 30 FPS camera input
- **Memory Usage**: Optimized for mobile devices with 4GB+ RAM

### ML Models & Data
- **Face Detection**: ML Kit lightweight model
- **Pose Detection**: 33-point BlazePose model
- **Action Recognition**: Custom trained object detection model
- **Training Data**: 
  - Pose samples: `pose_all.csv` (7 pose types)
  - Exercise samples: `exercise_pose.csv` (push-up/squat variations)

## 🛠️ Development

### Key Implementation Details

#### KETIDetector Class
Central processing coordinator that handles:
- Multi-modal detection result integration
- Sliding window filtering for stability
- Event generation and timing control
- Real-time data processing and output

#### Detection Processors
- **FaceDetectorProcessor**: Emotion analysis from facial features
- **PoseDetectorProcessor**: Pose classification and exercise recognition
- **ObjectDetectorProcessor**: Action recognition from object detection

#### Data Models
```java
// Pose exercise result structure
class PoseExercise {
    String exercise;     // Exercise type (pushup/squat)
    int repetition;      // Current repetition count
    String pose;         // Current pose classification
    float score;         // Confidence score
}
```

### Customization Options
- Adjust confidence thresholds for different accuracy requirements
- Modify sliding window sizes for responsiveness vs. stability
- Add new pose classifications by updating CSV training data
- Extend action recognition with additional object classes

## 🔧 Training Data Format

### Pose Classification Data
```csv
# Format: image_name, pose_class, landmark_coordinates...
image1.jpg,standing,x1,y1,z1,x2,y2,z2,...
image2.jpg,sit,x1,y1,z1,x2,y2,z2,...
```

### Supported Pose Classes
- **Basic Poses**: `standing`, `sit`, `lie`
- **Exercise Poses**: `pushup_up`, `pushup_down`, `squat_up`, `squat_down`

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/new-feature`)
3. Follow existing code style and architecture patterns
4. Test thoroughly on physical devices
5. Submit a pull request with detailed description

### Development Guidelines
- Maintain real-time performance requirements
- Follow Android development best practices
- Ensure proper camera resource management
- Add appropriate error handling and logging
- Update training data when adding new recognition classes

## 📝 License

```
Copyright 2024 KETI (Korea Electronics Technology Institute)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## 🙏 Acknowledgments

- **Google ML Kit Team** for providing robust computer vision APIs
- **KETI Research Team** for HRI system design and implementation
- **Android CameraX Team** for modern camera framework support

## 📞 Support

- **Issues**: Use GitHub Issues for bug reports and feature requests
- **Documentation**: Check project wiki for detailed implementation guides
- **Contact**: [contact@keti.re.kr](mailto:contact@keti.re.kr)
- **Website**: [KETI Official Website](https://www.keti.re.kr)

---

<div align="center">
  <strong>Comprehensive Human Behavior Analysis</strong><br>
  <em>Pose Detection • Action Recognition • Emotion Analysis</em><br>
  <sub>Developed by KETI for Advanced Human-Robot Interaction Research</sub>
</div>

