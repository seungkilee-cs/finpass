import * as faceapi from 'face-api.js';

export interface FaceDetection {
  boundingBox: {
    x: number;
    y: number;
    width: number;
    height: number;
  };
  landmarks: faceapi.FaceLandmarks68;
  confidence: number;
}

export interface LivenessFrame {
  timestamp: number;
  imageData: string; // base64 encoded image
  faceDetection?: FaceDetection;
  motionVector?: {
    x: number;
    y: number;
  };
}

export interface LivenessResult {
  score: number; // 0-1, higher means more likely to be live
  isLive: boolean;
  confidence: number;
  details: {
    faceDetected: boolean;
    motionDetected: boolean;
    blinkDetected: boolean;
    frameCount: number;
    averageConfidence: number;
  };
  frames: LivenessFrame[];
}

export class LivenessDetection {
  private modelLoaded: boolean = false;
  private readonly MIN_LIVENESS_SCORE = 0.7;
  private readonly REQUIRED_FRAMES = 5;
  private readonly FRAME_INTERVAL = 500; // ms between frames

  constructor() {
    this.loadModels();
  }

  /**
   * Load face detection models
   */
  async loadModels(): Promise<void> {
    try {
      console.log('Loading face detection models...');
      
      // Load the models from the public directory
      const MODEL_URL = '/models';
      
      await Promise.all([
        faceapi.nets.tinyFaceDetector.loadFromUri(MODEL_URL),
        faceapi.nets.faceLandmark68Net.loadFromUri(MODEL_URL),
        faceapi.nets.faceRecognitionNet.loadFromUri(MODEL_URL),
        faceapi.nets.faceExpressionNet.loadFromUri(MODEL_URL)
      ]);
      
      this.modelLoaded = true;
      console.log('Face detection models loaded successfully');
    } catch (error) {
      console.error('Failed to load face detection models:', error);
      throw new Error('Failed to initialize liveness detection');
    }
  }

  /**
   * Detect face in a video frame
   * @param videoElement HTML video element
   * @param canvasElement HTML canvas element for processing
   * @returns Face detection result
   */
  async detectFace(videoElement: HTMLVideoElement, canvasElement: HTMLCanvasElement): Promise<FaceDetection | null> {
    if (!this.modelLoaded) {
      throw new Error('Face detection models not loaded');
    }

    try {
      // Get video dimensions
      const displaySize = { width: videoElement.videoWidth, height: videoElement.videoHeight };
      
      // Resize canvas to match video
      faceapi.matchDimensions(canvasElement, displaySize);
      
      // Detect faces
      const detections = await faceapi
        .detectAllFaces(videoElement, new faceapi.TinyFaceDetectorOptions())
        .withFaceLandmarks()
        .withFaceExpressions();

      if (detections.length === 0) {
        return null;
      }

      // Get the first (and most likely) face detection
      const detection = detections[0];
      const box = detection.detection.box;

      return {
        boundingBox: {
          x: box.x,
          y: box.y,
          width: box.width,
          height: box.height
        },
        landmarks: detection.landmarks,
        confidence: detection.detection.score
      };
    } catch (error) {
      console.error('Face detection failed:', error);
      return null;
    }
  }

  /**
   * Capture frame from video
   * @param videoElement HTML video element
   * @param canvasElement HTML canvas element
   * @returns Base64 encoded image data
   */
  captureFrame(videoElement: HTMLVideoElement, canvasElement: HTMLCanvasElement): string {
    const context = canvasElement.getContext('2d');
    if (!context) {
      throw new Error('Could not get canvas context');
    }

    // Draw video frame to canvas
    canvasElement.width = videoElement.videoWidth;
    canvasElement.height = videoElement.videoHeight;
    context.drawImage(videoElement, 0, 0, canvasElement.width, canvasElement.height);

    // Convert to base64
    return canvasElement.toDataURL('image/jpeg', 0.8);
  }

  /**
   * Calculate motion between two face detections
   * @param frame1 Previous frame
   * @param frame2 Current frame
   * @returns Motion vector
   */
  calculateMotion(frame1: LivenessFrame, frame2: LivenessFrame): { x: number; y: number } | null {
    if (!frame1.faceDetection || !frame2.faceDetection) {
      return null;
    }

    const box1 = frame1.faceDetection.boundingBox;
    const box2 = frame2.faceDetection.boundingBox;

    // Calculate center points
    const center1 = {
      x: box1.x + box1.width / 2,
      y: box1.y + box1.height / 2
    };

    const center2 = {
      x: box2.x + box2.width / 2,
      y: box2.y + box2.height / 2
    };

    return {
      x: center2.x - center1.x,
      y: center2.y - center1.y
    };
  }

  /**
   * Detect blink using facial landmarks
   * @param faceDetection Face detection with landmarks
   * @returns True if blink detected
   */
  detectBlink(faceDetection: FaceDetection): boolean {
    const landmarks = faceDetection.landmarks;
    
    // Get eye landmarks
    const leftEye = landmarks.getLeftEye();
    const rightEye = landmarks.getRightEye();
    
    // Calculate eye aspect ratio (EAR)
    const leftEAR = this.calculateEyeAspectRatio(leftEye);
    const rightEAR = this.calculateEyeAspectRatio(rightEye);
    const averageEAR = (leftEAR + rightEAR) / 2;
    
    // Blink threshold (typically < 0.25 indicates closed eyes)
    return averageEAR < 0.25;
  }

  /**
   * Calculate eye aspect ratio for blink detection
   * @param eye Eye landmarks
   * @returns Eye aspect ratio
   */
  private calculateEyeAspectRatio(eye: faceapi.Point[]): number {
    // Calculate vertical distances
    const A = this.distance(eye[1], eye[5]);
    const B = this.distance(eye[2], eye[4]);
    
    // Calculate horizontal distance
    const C = this.distance(eye[0], eye[3]);
    
    // Eye aspect ratio
    return (A + B) / (2 * C);
  }

  /**
   * Calculate distance between two points
   * @param p1 Point 1
   * @param p2 Point 2
   * @returns Distance
   */
  private distance(p1: faceapi.Point, p2: faceapi.Point): number {
    return Math.sqrt(Math.pow(p2.x - p1.x, 2) + Math.pow(p2.y - p1.y, 2));
  }

  /**
   * Perform liveness check by capturing and analyzing multiple frames
   * @param videoElement HTML video element
   * @param canvasElement HTML canvas element
   * @param onProgress Progress callback (0-1)
   * @returns Liveness detection result
   */
  async performLivenessCheck(
    videoElement: HTMLVideoElement,
    canvasElement: HTMLCanvasElement,
    onProgress?: (progress: number) => void
  ): Promise<LivenessResult> {
    if (!this.modelLoaded) {
      throw new Error('Face detection models not loaded');
    }

    const frames: LivenessFrame[] = [];
    let faceDetectedCount = 0;
    let motionDetected = false;
    let blinkDetected = false;
    let totalConfidence = 0;

    console.log(`Starting liveness check with ${this.REQUIRED_FRAMES} frames...`);

    // Capture frames
    for (let i = 0; i < this.REQUIRED_FRAMES; i++) {
      const timestamp = Date.now();
      const imageData = this.captureFrame(videoElement, canvasElement);
      
      // Detect face
      const faceDetection = await this.detectFace(videoElement, canvasElement);
      
      const frame: LivenessFrame = {
        timestamp,
        imageData,
        faceDetection: faceDetection || undefined
      };

      // Calculate motion if we have previous frame
      if (frames.length > 0 && faceDetection) {
        const motion = this.calculateMotion(frames[frames.length - 1], frame);
        if (motion) {
          frame.motionVector = motion;
          
          // Check if motion is significant enough
          const motionMagnitude = Math.sqrt(motion.x * motion.x + motion.y * motion.y);
          if (motionMagnitude > 5) { // 5 pixels threshold
            motionDetected = true;
          }
        }
      }

      // Check for blink
      if (faceDetection) {
        faceDetectedCount++;
        totalConfidence += faceDetection.confidence;
        
        if (this.detectBlink(faceDetection)) {
          blinkDetected = true;
        }
      }

      frames.push(frame);

      // Update progress
      if (onProgress) {
        onProgress((i + 1) / this.REQUIRED_FRAMES);
      }

      // Wait before next frame
      if (i < this.REQUIRED_FRAMES - 1) {
        await new Promise(resolve => setTimeout(resolve, this.FRAME_INTERVAL));
      }
    }

    // Calculate liveness score
    const score = this.calculateLivenessScore(frames, {
      faceDetectedCount,
      motionDetected,
      blinkDetected,
      totalConfidence
    });

    const result: LivenessResult = {
      score,
      isLive: score >= this.MIN_LIVENESS_SCORE,
      confidence: Math.min(faceDetectedCount / this.REQUIRED_FRAMES, 1),
      details: {
        faceDetected: faceDetectedCount > 0,
        motionDetected,
        blinkDetected,
        frameCount: frames.length,
        averageConfidence: faceDetectedCount > 0 ? totalConfidence / faceDetectedCount : 0
      },
      frames
    };

    console.log('Liveness check completed:', result);
    return result;
  }

  /**
   * Calculate liveness score based on various factors
   * @param frames Captured frames
   * @param metrics Detection metrics
   * @returns Liveness score (0-1)
   */
  private calculateLivenessScore(
    frames: LivenessFrame[],
    metrics: {
      faceDetectedCount: number;
      motionDetected: boolean;
      blinkDetected: boolean;
      totalConfidence: number;
    }
  ): number {
    let score = 0;

    // Face detection score (40% weight)
    const faceScore = metrics.faceDetectedCount / this.REQUIRED_FRAMES;
    score += faceScore * 0.4;

    // Motion detection score (30% weight)
    if (metrics.motionDetected) {
      score += 0.3;
    }

    // Blink detection score (20% weight)
    if (metrics.blinkDetected) {
      score += 0.2;
    }

    // Average confidence score (10% weight)
    const avgConfidence = metrics.faceDetectedCount > 0 ? 
      metrics.totalConfidence / metrics.faceDetectedCount : 0;
    score += avgConfidence * 0.1;

    return Math.min(score, 1);
  }

  /**
   * Check if models are loaded
   * @returns True if models are loaded
   */
  isReady(): boolean {
    return this.modelLoaded;
  }

  /**
   * Get minimum liveness score threshold
   * @returns Minimum score threshold
   */
  getMinLivenessScore(): number {
    return this.MIN_LIVENESS_SCORE;
  }
}
