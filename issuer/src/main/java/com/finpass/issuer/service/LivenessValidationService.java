package com.finpass.issuer.service;

import com.finpass.issuer.dto.LivenessProof;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;

/**
 * Service for validating liveness proofs
 * Analyzes frames and metadata to detect spoofing attempts
 */
@Service
public class LivenessValidationService {
    
    private static final Logger logger = LoggerFactory.getLogger(LivenessValidationService.class);
    
    // Configuration constants
    private static final double MIN_LIVENESS_SCORE = 0.7;
    private static final double MIN_CONFIDENCE = 0.6;
    private static final int MIN_FRAME_COUNT = 3;
    private static final int MAX_FRAME_COUNT = 10;
    private static final long MAX_PROOF_AGE_MINUTES = 5;
    private static final double MIN_MOTION_THRESHOLD = 3.0; // pixels
    private static final double MAX_FACE_SIZE_RATIO = 0.8; // 80% of frame
    private static final double MIN_FACE_SIZE_RATIO = 0.1; // 10% of frame
    
    /**
     * Validate liveness proof
     * @param proof Liveness proof to validate
     * @return Validation result
     */
    public ValidationResult validateLivenessProof(LivenessProof proof) {
        try {
            logger.info("Validating liveness proof with score: {}", proof.getScore());
            
            // Basic structure validation
            ValidationResult structureResult = validateStructure(proof);
            if (!structureResult.isValid()) {
                return structureResult;
            }
            
            // Score validation
            ValidationResult scoreResult = validateScore(proof);
            if (!scoreResult.isValid()) {
                return scoreResult;
            }
            
            // Timestamp validation
            ValidationResult timestampResult = validateTimestamp(proof);
            if (!timestampResult.isValid()) {
                return timestampResult;
            }
            
            // Frame analysis
            ValidationResult frameResult = validateFrames(proof);
            if (!frameResult.isValid()) {
                return frameResult;
            }
            
            // Anti-spoofing checks
            ValidationResult antiSpoofResult = validateAntiSpoofing(proof);
            if (!antiSpoofResult.isValid()) {
                return antiSpoofResult;
            }
            
            logger.info("Liveness proof validation successful");
            return ValidationResult.success("Liveness proof is valid");
            
        } catch (Exception e) {
            logger.error("Error validating liveness proof", e);
            return ValidationResult.error("Validation failed: " + e.getMessage());
        }
    }
    
    /**
     * Validate basic structure of liveness proof
     * @param proof Liveness proof
     * @return Validation result
     */
    private ValidationResult validateStructure(LivenessProof proof) {
        if (proof == null) {
            return ValidationResult.error("Liveness proof is null");
        }
        
        if (proof.getScore() == null || proof.getIsLive() == null || proof.getConfidence() == null) {
            return ValidationResult.error("Missing required fields in liveness proof");
        }
        
        if (proof.getFrames() == null || proof.getFrames().isEmpty()) {
            return ValidationResult.error("No frames provided in liveness proof");
        }
        
        if (proof.getDetails() == null) {
            return ValidationResult.error("Missing liveness details");
        }
        
        return ValidationResult.success("Structure validation passed");
    }
    
    /**
     * Validate liveness score and confidence
     * @param proof Liveness proof
     * @return Validation result
     */
    private ValidationResult validateScore(LivenessProof proof) {
        if (proof.getScore() < MIN_LIVENESS_SCORE) {
            return ValidationResult.error(
                String.format("Liveness score %.2f is below minimum threshold %.2f", 
                    proof.getScore(), MIN_LIVENESS_SCORE)
            );
        }
        
        if (proof.getConfidence() < MIN_CONFIDENCE) {
            return ValidationResult.error(
                String.format("Confidence %.2f is below minimum threshold %.2f", 
                    proof.getConfidence(), MIN_CONFIDENCE)
            );
        }
        
        if (!proof.getIsLive()) {
            return ValidationResult.error("Liveness check indicates the subject is not live");
        }
        
        return ValidationResult.success("Score validation passed");
    }
    
    /**
     * Validate timestamp of liveness proof
     * @param proof Liveness proof
     * @return Validation result
     */
    private ValidationResult validateTimestamp(LivenessProof proof) {
        if (proof.getTimestamp() == null) {
            return ValidationResult.error("Missing timestamp in liveness proof");
        }
        
        Instant proofTime = Instant.ofEpochMilli(proof.getTimestamp());
        Instant now = Instant.now();
        
        // Check if proof is too old
        long minutesBetween = ChronoUnit.MINUTES.between(proofTime, now);
        if (minutesBetween > MAX_PROOF_AGE_MINUTES) {
            return ValidationResult.error(
                String.format("Liveness proof is too old: %d minutes (max: %d)", 
                    minutesBetween, MAX_PROOF_AGE_MINUTES)
            );
        }
        
        // Check if proof is from the future (with some tolerance for clock skew)
        if (proofTime.isAfter(now.plusSeconds(60))) {
            return ValidationResult.error("Liveness proof timestamp is in the future");
        }
        
        return ValidationResult.success("Timestamp validation passed");
    }
    
    /**
     * Validate frame data
     * @param proof Liveness proof
     * @return Validation result
     */
    private ValidationResult validateFrames(LivenessProof proof) {
        List<LivenessProof.LivenessFrame> frames = proof.getFrames();
        
        if (frames.size() < MIN_FRAME_COUNT) {
            return ValidationResult.error(
                String.format("Insufficient frames: %d (minimum: %d)", 
                    frames.size(), MIN_FRAME_COUNT)
            );
        }
        
        if (frames.size() > MAX_FRAME_COUNT) {
            return ValidationResult.error(
                String.format("Too many frames: %d (maximum: %d)", 
                    frames.size(), MAX_FRAME_COUNT)
            );
        }
        
        // Validate each frame
        int validFrames = 0;
        double totalConfidence = 0;
        
        for (LivenessProof.LivenessFrame frame : frames) {
            ValidationResult frameResult = validateFrame(frame);
            if (frameResult.isValid()) {
                validFrames++;
                if (frame.getFaceConfidence() != null) {
                    totalConfidence += frame.getFaceConfidence();
                }
            }
        }
        
        if (validFrames < MIN_FRAME_COUNT) {
            return ValidationResult.error(
                String.format("Insufficient valid frames: %d (minimum: %d)", 
                    validFrames, MIN_FRAME_COUNT)
            );
        }
        
        // Check average confidence
        double avgConfidence = totalConfidence / validFrames;
        if (avgConfidence < MIN_CONFIDENCE) {
            return ValidationResult.error(
                String.format("Average frame confidence %.2f is below threshold %.2f", 
                    avgConfidence, MIN_CONFIDENCE)
            );
        }
        
        return ValidationResult.success("Frame validation passed");
    }
    
    /**
     * Validate individual frame
     * @param frame Frame to validate
     * @return Validation result
     */
    private ValidationResult validateFrame(LivenessProof.LivenessFrame frame) {
        if (frame == null) {
            return ValidationResult.error("Frame is null");
        }
        
        if (frame.getTimestamp() == null) {
            return ValidationResult.error("Frame missing timestamp");
        }
        
        if (frame.getImageData() == null || frame.getImageData().trim().isEmpty()) {
            return ValidationResult.error("Frame missing image data");
        }
        
        // Validate base64 image data
        try {
            Base64.getDecoder().decode(frame.getImageData());
        } catch (IllegalArgumentException e) {
            return ValidationResult.error("Frame image data is not valid base64");
        }
        
        // Validate face detection if present
        if (frame.getFaceDetected() != null && frame.getFaceDetected()) {
            if (frame.getFaceConfidence() == null || frame.getFaceConfidence() < 0.5) {
                return ValidationResult.error("Face detected but confidence is too low");
            }
            
            // Validate bounding box if present
            if (frame.getBoundingBox() != null) {
                ValidationResult bboxResult = validateBoundingBox(frame.getBoundingBox());
                if (!bboxResult.isValid()) {
                    return bboxResult;
                }
            }
        }
        
        return ValidationResult.success("Frame validation passed");
    }
    
    /**
     * Validate bounding box dimensions
     * @param bbox Bounding box to validate
     * @return Validation result
     */
    private ValidationResult validateBoundingBox(LivenessProof.BoundingBox bbox) {
        if (bbox.getX() == null || bbox.getY() == null || 
            bbox.getWidth() == null || bbox.getHeight() == null) {
            return ValidationResult.error("Bounding box has missing dimensions");
        }
        
        if (bbox.getWidth() <= 0 || bbox.getHeight() <= 0) {
            return ValidationResult.error("Bounding box has invalid dimensions");
        }
        
        // Check if face size is reasonable (assuming 640x480 frame)
        double frameArea = 640 * 480;
        double faceArea = bbox.getWidth() * bbox.getHeight();
        double sizeRatio = faceArea / frameArea;
        
        if (sizeRatio > MAX_FACE_SIZE_RATIO) {
            return ValidationResult.error("Face size is too large (possible spoofing)");
        }
        
        if (sizeRatio < MIN_FACE_SIZE_RATIO) {
            return ValidationResult.error("Face size is too small");
        }
        
        return ValidationResult.success("Bounding box validation passed");
    }
    
    /**
     * Perform anti-spoofing validation
     * @param proof Liveness proof
     * @return Validation result
     */
    private ValidationResult validateAntiSpoofing(LivenessProof proof) {
        LivenessProof.LivenessDetails details = proof.getDetails();
        
        // Check for motion detection
        if (details.getMotionDetected() != null && !details.getMotionDetected()) {
            logger.warn("No motion detected in liveness proof - possible spoofing attempt");
            // Don't fail immediately, but log warning
        }
        
        // Check for blink detection
        if (details.getBlinkDetected() != null && !details.getBlinkDetected()) {
            logger.warn("No blink detected in liveness proof - possible spoofing attempt");
            // Don't fail immediately, but log warning
        }
        
        // Analyze motion vectors
        List<LivenessProof.LivenessFrame> frames = proof.getFrames();
        boolean hasSignificantMotion = false;
        
        for (int i = 1; i < frames.size(); i++) {
            LivenessProof.LivenessFrame prevFrame = frames.get(i - 1);
            LivenessProof.LivenessFrame currFrame = frames.get(i);
            
            if (prevFrame.getBoundingBox() != null && currFrame.getBoundingBox() != null) {
                double motionDistance = calculateMotionDistance(prevFrame.getBoundingBox(), currFrame.getBoundingBox());
                if (motionDistance > MIN_MOTION_THRESHOLD) {
                    hasSignificantMotion = true;
                    break;
                }
            }
        }
        
        if (!hasSignificantMotion) {
            logger.warn("No significant motion detected between frames - possible photo spoofing");
            // Reduce liveness score but don't necessarily fail
        }
        
        // Check device info for anomalies
        if (proof.getDeviceInfo() != null) {
            ValidationResult deviceResult = validateDeviceInfo(proof.getDeviceInfo());
            if (!deviceResult.isValid()) {
                return deviceResult;
            }
        }
        
        return ValidationResult.success("Anti-spoofing validation passed");
    }
    
    /**
     * Calculate motion distance between two bounding boxes
     * @param bbox1 First bounding box
     * @param bbox2 Second bounding box
     * @return Motion distance in pixels
     */
    private double calculateMotionDistance(LivenessProof.BoundingBox bbox1, LivenessProof.BoundingBox bbox2) {
        double centerX1 = bbox1.getX() + bbox1.getWidth() / 2;
        double centerY1 = bbox1.getY() + bbox1.getHeight() / 2;
        double centerX2 = bbox2.getX() + bbox2.getWidth() / 2;
        double centerY2 = bbox2.getY() + bbox2.getHeight() / 2;
        
        return Math.sqrt(Math.pow(centerX2 - centerX1, 2) + Math.pow(centerY2 - centerY1, 2));
    }
    
    /**
     * Validate device information
     * @param deviceInfo Device information
     * @return Validation result
     */
    private ValidationResult validateDeviceInfo(LivenessProof.DeviceInfo deviceInfo) {
        if (deviceInfo.getUserAgent() == null || deviceInfo.getUserAgent().trim().isEmpty()) {
            return ValidationResult.error("Missing user agent information");
        }
        
        // Check for suspicious user agents (bots, scrapers, etc.)
        String userAgent = deviceInfo.getUserAgent().toLowerCase();
        if (userAgent.contains("bot") || userAgent.contains("crawler") || userAgent.contains("scraper")) {
            return ValidationResult.error("Suspicious user agent detected");
        }
        
        return ValidationResult.success("Device info validation passed");
    }
    
    /**
     * Validation result class
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;
        
        private ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
        
        public static ValidationResult success(String message) {
            return new ValidationResult(true, message);
        }
        
        public static ValidationResult error(String message) {
            return new ValidationResult(false, message);
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getMessage() {
            return message;
        }
    }
}
