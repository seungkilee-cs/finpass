package com.finpass.issuer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Liveness proof for credential issuance
 * Contains frames and analysis results from liveness detection
 */
public class LivenessProof {
    
    @JsonProperty("score")
    private Double score;
    
    @JsonProperty("is_live")
    private Boolean isLive;
    
    @JsonProperty("confidence")
    private Double confidence;
    
    @JsonProperty("timestamp")
    private Long timestamp;
    
    @JsonProperty("frames")
    private List<LivenessFrame> frames;
    
    @JsonProperty("details")
    private LivenessDetails details;
    
    @JsonProperty("device_info")
    private DeviceInfo deviceInfo;
    
    // Constructors
    public LivenessProof() {}
    
    public LivenessProof(Double score, Boolean isLive, Double confidence) {
        this.score = score;
        this.isLive = isLive;
        this.confidence = confidence;
        this.timestamp = System.currentTimeMillis();
    }
    
    // Getters and Setters
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
    
    public Boolean getIsLive() { return isLive; }
    public void setIsLive(Boolean isLive) { this.isLive = isLive; }
    
    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
    
    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    
    public List<LivenessFrame> getFrames() { return frames; }
    public void setFrames(List<LivenessFrame> frames) { this.frames = frames; }
    
    public LivenessDetails getDetails() { return details; }
    public void setDetails(LivenessDetails details) { this.details = details; }
    
    public DeviceInfo getDeviceInfo() { return deviceInfo; }
    public void setDeviceInfo(DeviceInfo deviceInfo) { this.deviceInfo = deviceInfo; }
    
    /**
     * Individual frame data
     */
    public static class LivenessFrame {
        
        @JsonProperty("timestamp")
        private Long timestamp;
        
        @JsonProperty("image_data")
        private String imageData; // base64 encoded
        
        @JsonProperty("face_detected")
        private Boolean faceDetected;
        
        @JsonProperty("face_confidence")
        private Double faceConfidence;
        
        @JsonProperty("bounding_box")
        private BoundingBox boundingBox;
        
        @JsonProperty("motion_vector")
        private MotionVector motionVector;
        
        // Constructors
        public LivenessFrame() {}
        
        public LivenessFrame(Long timestamp, String imageData) {
            this.timestamp = timestamp;
            this.imageData = imageData;
        }
        
        // Getters and Setters
        public Long getTimestamp() { return timestamp; }
        public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
        
        public String getImageData() { return imageData; }
        public void setImageData(String imageData) { this.imageData = imageData; }
        
        public Boolean getFaceDetected() { return faceDetected; }
        public void setFaceDetected(Boolean faceDetected) { this.faceDetected = faceDetected; }
        
        public Double getFaceConfidence() { return faceConfidence; }
        public void setFaceConfidence(Double faceConfidence) { this.faceConfidence = faceConfidence; }
        
        public BoundingBox getBoundingBox() { return boundingBox; }
        public void setBoundingBox(BoundingBox boundingBox) { this.boundingBox = boundingBox; }
        
        public MotionVector getMotionVector() { return motionVector; }
        public void setMotionVector(MotionVector motionVector) { this.motionVector = motionVector; }
    }
    
    /**
     * Face bounding box
     */
    public static class BoundingBox {
        
        @JsonProperty("x")
        private Double x;
        
        @JsonProperty("y")
        private Double y;
        
        @JsonProperty("width")
        private Double width;
        
        @JsonProperty("height")
        private Double height;
        
        // Constructors
        public BoundingBox() {}
        
        public BoundingBox(Double x, Double y, Double width, Double height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
        
        // Getters and Setters
        public Double getX() { return x; }
        public void setX(Double x) { this.x = x; }
        
        public Double getY() { return y; }
        public void setY(Double y) { this.y = y; }
        
        public Double getWidth() { return width; }
        public void setWidth(Double width) { this.width = width; }
        
        public Double getHeight() { return height; }
        public void setHeight(Double height) { this.height = height; }
    }
    
    /**
     * Motion vector between frames
     */
    public static class MotionVector {
        
        @JsonProperty("x")
        private Double x;
        
        @JsonProperty("y")
        private Double y;
        
        @JsonProperty("magnitude")
        private Double magnitude;
        
        // Constructors
        public MotionVector() {}
        
        public MotionVector(Double x, Double y) {
            this.x = x;
            this.y = y;
            this.magnitude = Math.sqrt(x * x + y * y);
        }
        
        // Getters and Setters
        public Double getX() { return x; }
        public void setX(Double x) { this.x = x; }
        
        public Double getY() { return y; }
        public void setY(Double y) { this.y = y; }
        
        public Double getMagnitude() { return magnitude; }
        public void setMagnitude(Double magnitude) { this.magnitude = magnitude; }
    }
    
    /**
     * Liveness detection details
     */
    public static class LivenessDetails {
        
        @JsonProperty("face_detected")
        private Boolean faceDetected;
        
        @JsonProperty("motion_detected")
        private Boolean motionDetected;
        
        @JsonProperty("blink_detected")
        private Boolean blinkDetected;
        
        @JsonProperty("frame_count")
        private Integer frameCount;
        
        @JsonProperty("average_confidence")
        private Double averageConfidence;
        
        @JsonProperty("processing_time_ms")
        private Long processingTimeMs;
        
        // Constructors
        public LivenessDetails() {}
        
        public LivenessDetails(Boolean faceDetected, Boolean motionDetected, Boolean blinkDetected) {
            this.faceDetected = faceDetected;
            this.motionDetected = motionDetected;
            this.blinkDetected = blinkDetected;
        }
        
        // Getters and Setters
        public Boolean getFaceDetected() { return faceDetected; }
        public void setFaceDetected(Boolean faceDetected) { this.faceDetected = faceDetected; }
        
        public Boolean getMotionDetected() { return motionDetected; }
        public void setMotionDetected(Boolean motionDetected) { this.motionDetected = motionDetected; }
        
        public Boolean getBlinkDetected() { return blinkDetected; }
        public void setBlinkDetected(Boolean blinkDetected) { this.blinkDetected = blinkDetected; }
        
        public Integer getFrameCount() { return frameCount; }
        public void setFrameCount(Integer frameCount) { this.frameCount = frameCount; }
        
        public Double getAverageConfidence() { return averageConfidence; }
        public void setAverageConfidence(Double averageConfidence) { this.averageConfidence = averageConfidence; }
        
        public Long getProcessingTimeMs() { return processingTimeMs; }
        public void setProcessingTimeMs(Long processingTimeMs) { this.processingTimeMs = processingTimeMs; }
    }
    
    /**
     * Device information for verification
     */
    public static class DeviceInfo {
        
        @JsonProperty("user_agent")
        private String userAgent;
        
        @JsonProperty("screen_resolution")
        private String screenResolution;
        
        @JsonProperty("camera_capabilities")
        private CameraCapabilities cameraCapabilities;
        
        @JsonProperty("timestamp")
        private Long timestamp;
        
        // Constructors
        public DeviceInfo() {}
        
        public DeviceInfo(String userAgent, String screenResolution) {
            this.userAgent = userAgent;
            this.screenResolution = screenResolution;
            this.timestamp = System.currentTimeMillis();
        }
        
        // Getters and Setters
        public String getUserAgent() { return userAgent; }
        public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
        
        public String getScreenResolution() { return screenResolution; }
        public void setScreenResolution(String screenResolution) { this.screenResolution = screenResolution; }
        
        public CameraCapabilities getCameraCapabilities() { return cameraCapabilities; }
        public void setCameraCapabilities(CameraCapabilities cameraCapabilities) { this.cameraCapabilities = cameraCapabilities; }
        
        public Long getTimestamp() { return timestamp; }
        public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    }
    
    /**
     * Camera capabilities
     */
    public static class CameraCapabilities {
        
        @JsonProperty("width")
        private Integer width;
        
        @JsonProperty("height")
        private Integer height;
        
        @JsonProperty("facing_mode")
        private String facingMode;
        
        @JsonProperty("frame_rate")
        private Double frameRate;
        
        // Constructors
        public CameraCapabilities() {}
        
        public CameraCapabilities(Integer width, Integer height, String facingMode) {
            this.width = width;
            this.height = height;
            this.facingMode = facingMode;
        }
        
        // Getters and Setters
        public Integer getWidth() { return width; }
        public void setWidth(Integer width) { this.width = width; }
        
        public Integer getHeight() { return height; }
        public void setHeight(Integer height) { this.height = height; }
        
        public String getFacingMode() { return facingMode; }
        public void setFacingMode(String facingMode) { this.facingMode = facingMode; }
        
        public Double getFrameRate() { return frameRate; }
        public void setFrameRate(Double frameRate) { this.frameRate = frameRate; }
    }
}
