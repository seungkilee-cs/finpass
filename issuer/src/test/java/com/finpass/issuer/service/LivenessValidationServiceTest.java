package com.finpass.issuer.service;

import com.finpass.issuer.dto.LivenessProof;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Liveness Validation Service
 */
@ExtendWith(MockitoExtension.class)
class LivenessValidationServiceTest {
    
    private LivenessValidationService service;
    
    @BeforeEach
    void setUp() {
        service = new LivenessValidationService();
    }
    
    @Test
    void testValidateLivenessProof_Success() {
        // Arrange
        LivenessProof proof = createValidLivenessProof();
        
        // Act
        LivenessValidationService.ValidationResult result = service.validateLivenessProof(proof);
        
        // Assert
        assertTrue(result.isValid(), "Validation should succeed");
        assertEquals("Liveness proof is valid", result.getMessage());
    }
    
    @Test
    void testValidateLivenessProof_NullProof() {
        // Act
        LivenessValidationService.ValidationResult result = service.validateLivenessProof(null);
        
        // Assert
        assertFalse(result.isValid(), "Validation should fail for null proof");
        assertTrue(result.getMessage().contains("null"));
    }
    
    @Test
    void testValidateLivenessProof_MissingFields() {
        // Arrange
        LivenessProof proof = new LivenessProof();
        // Missing required fields
        
        // Act
        LivenessValidationService.ValidationResult result = service.validateLivenessProof(proof);
        
        // Assert
        assertFalse(result.isValid(), "Validation should fail for missing fields");
        assertTrue(result.getMessage().contains("Missing required fields"));
    }
    
    @Test
    void testValidateLivenessProof_LowScore() {
        // Arrange
        LivenessProof proof = createValidLivenessProof();
        proof.setScore(0.5); // Below minimum threshold
        
        // Act
        LivenessValidationService.ValidationResult result = service.validateLivenessProof(proof);
        
        // Assert
        assertFalse(result.isValid(), "Validation should fail for low score");
        assertTrue(result.getMessage().contains("below minimum threshold"));
    }
    
    @Test
    void testValidateLivenessProof_NotLive() {
        // Arrange
        LivenessProof proof = createValidLivenessProof();
        proof.setIsLive(false);
        
        // Act
        LivenessValidationService.ValidationResult result = service.validateLivenessProof(proof);
        
        // Assert
        assertFalse(result.isValid(), "Validation should fail for non-live result");
        assertTrue(result.getMessage().contains("not live"));
    }
    
    @Test
    void testValidateLivenessProof_LowConfidence() {
        // Arrange
        LivenessProof proof = createValidLivenessProof();
        proof.setConfidence(0.5); // Below minimum threshold
        
        // Act
        LivenessValidationService.ValidationResult result = service.validateLivenessProof(proof);
        
        // Assert
        assertFalse(result.isValid(), "Validation should fail for low confidence");
        assertTrue(result.getMessage().contains("below minimum threshold"));
    }
    
    @Test
    void testValidateLivenessProof_OldTimestamp() {
        // Arrange
        LivenessProof proof = createValidLivenessProof();
        proof.setTimestamp(Instant.now().minusSeconds(600).toEpochMilli()); // 10 minutes ago
        
        // Act
        LivenessValidationService.ValidationResult result = service.validateLivenessProof(proof);
        
        // Assert
        assertFalse(result.isValid(), "Validation should fail for old timestamp");
        assertTrue(result.getMessage().contains("too old"));
    }
    
    @Test
    void testValidateLivenessProof_FutureTimestamp() {
        // Arrange
        LivenessProof proof = createValidLivenessProof();
        proof.setTimestamp(Instant.now().plusSeconds(120).toEpochMilli()); // 2 minutes in future
        
        // Act
        LivenessValidationService.ValidationResult result = service.validateLivenessProof(proof);
        
        // Assert
        assertFalse(result.isValid(), "Validation should fail for future timestamp");
        assertTrue(result.getMessage().contains("future"));
    }
    
    @Test
    void testValidateLiveness_NoFrames() {
        // Arrange
        LivenessProof proof = createValidLivenessProof();
        proof.setFrames(new ArrayList<>());
        
        // Act
        LivenessValidationService.ValidationResult result = service.validateLivenessProof(proof);
        
        // Assert
        assertFalse(result.isValid(), "Validation should fail for no frames");
        assertTrue(result.getMessage().contains("No frames"));
    }
    
    @Test
    void testValidateLiveness_InsufficientFrames() {
        // Arrange
        LivenessProof proof = createValidLivenessProof();
        proof.setFrames(List.of(createValidFrame())); // Only 1 frame
        
        // Act
        LivenessValidationService.ValidationResult result = service.validateLivenessProof(proof);
        
        // Assert
        assertFalse(result.isValid(), "Validation should fail for insufficient frames");
        assertTrue(result.getMessage().contains("Insufficient frames"));
    }
    
    @Test
    void testValidateLiveness_TooManyFrames() {
        // Arrange
        LivenessProof proof = createValidLivenessProof();
        List<LivenessProof.LivenessFrame> frames = new ArrayList<>();
        for (int i = 0; i < 15; i++) { // 15 frames (exceeds max)
            frames.add(createValidFrame());
        }
        proof.setFrames(frames);
        
        // Act
        LivenessValidationService.ValidationResult result = service.validateLivenessProof(proof);
        
        // Assert
        assertFalse(result.isValid(), "Validation should fail for too many frames");
        assertTrue(result.getMessage().contains("Too many frames"));
    }
    
    @Test
    void testValidateLiveness_InvalidFrameData() {
        // Arrange
        LivenessProof proof = createValidLivenessProof();
        LivenessProof.LivenessFrame invalidFrame = createValidFrame();
        invalidFrame.setImageData("invalid-base64-data");
        
        List<LivenessProof.LivenessFrame> frames = new ArrayList<>();
        frames.add(invalidFrame);
        frames.add(createValidFrame());
        frames.add(createValidFrame());
        
        proof.setFrames(frames);
        
        // Act
        LivenessValidationService.ValidationResult result = service.validateLivenessProof(proof);
        
        // Assert
        assertFalse(result.isValid(), "Validation should fail for invalid frame data");
        assertTrue(result.getMessage().contains("not valid base64"));
    }
    
    @Test
    void testValidateLiveness_LowFaceConfidence() {
        // Arrange
        LivenessProof proof = createValidLivenessProof();
        
        LivenessProof.LivenessFrame lowConfidenceFrame = createValidFrame();
        lowConfidenceFrame.setFaceDetected(true);
        lowConfidenceFrame.setFaceConfidence(0.3); // Below threshold
        
        List<LivenessProof.LivenessFrame> frames = new ArrayList<>();
        frames.add(lowConfidenceFrame);
        frames.add(createValidFrame());
        frames.add(createValidFrame());
        
        proof.setFrames(frames);
        
        // Act
        LivenessValidationService.ValidationResult result = service.validateLivenessProof(proof);
        
        // Assert
        assertFalse(result.isValid(), "Validation should fail for low face confidence");
        assertTrue(result.getMessage().contains("confidence is too low"));
    }
    
    @Test
    void testValidateLiveness_SuspiciousUserAgent() {
        // Arrange
        LivenessProof proof = createValidLivenessProof();
        
        LivenessProof.DeviceInfo deviceInfo = new LivenessProof.DeviceInfo();
        deviceInfo.setUserAgent("Googlebot/2.1");
        proof.setDeviceInfo(deviceInfo);
        
        // Act
        LivenessValidationService.ValidationResult result = service.validateLivenessProof(proof);
        
        // Assert
        assertFalse(result.isValid(), "Validation should fail for suspicious user agent");
        assertTrue(result.getMessage().contains("Suspicious user agent"));
    }
    
    @Test
    void testValidateLiveness_SuccessWithMotion() {
        // Arrange
        LivenessProof proof = createValidLivenessProof();
        
        // Add motion vectors to frames
        List<LivenessProof.LivenessFrame> frames = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            LivenessProof.LivenessFrame frame = createValidFrame();
            frame.setMotionVector(new LivenessProof.MotionVector(i * 2.0, i * 1.5));
            frames.add(frame);
        }
        proof.setFrames(frames);
        
        // Act
        LivenessValidationService.ValidationResult result = service.validateLivenessProof(proof);
        
        // Assert
        assertTrue(result.isValid(), "Validation should succeed with motion");
    }
    
    // Helper methods
    private LivenessProof createValidLivenessProof() {
        LivenessProof proof = new LivenessProof();
        proof.setScore(0.85);
        proof.setIsLive(true);
        proof.setConfidence(0.9);
        proof.setTimestamp(System.currentTimeMillis());
        
        List<LivenessProof.LivenessFrame> frames = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            frames.add(createValidFrame());
        }
        proof.setFrames(frames);
        
        LivenessProof.LivenessDetails details = new LivenessProof.LivenessDetails();
        details.setFaceDetected(true);
        details.setMotionDetected(true);
        details.setBlinkDetected(true);
        details.setFrameCount(5);
        details.setAverageConfidence(0.9);
        proof.setDetails(details);
        
        LivenessProof.DeviceInfo deviceInfo = new LivenessProof.DeviceInfo();
        deviceInfo.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        deviceInfo.setScreenResolution("1920x1080");
        proof.setDeviceInfo(deviceInfo);
        
        return proof;
    }
    
    private LivenessProof.LivenessFrame createValidFrame() {
        LivenessProof.LivenessFrame frame = new LivenessProof.LivenessFrame();
        frame.setTimestamp(System.currentTimeMillis());
        frame.setImageData(createValidBase64Image());
        frame.setFaceDetected(true);
        frame.setFaceConfidence(0.95);
        
        LivenessProof.BoundingBox bbox = new LivenessProof.BoundingBox();
        bbox.setX(100.0);
        bbox.setY(100.0);
        bbox.setWidth(200.0);
        bbox.setHeight(250.0);
        frame.setBoundingBox(bbox);
        
        return frame;
    }
    
    private String createValidBase64Image() {
        // Create a minimal valid base64 encoded PNG
        return "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==";
    }
}
