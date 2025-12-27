import React, { useState, useRef, useEffect, useCallback } from 'react';
import { LivenessDetection, LivenessResult } from '../services/liveness';

interface LivenessCheckProps {
  onLivenessComplete: (result: LivenessResult) => void;
  onCancel: () => void;
}

export const LivenessCheck: React.FC<LivenessCheckProps> = ({
  onLivenessComplete,
  onCancel
}) => {
  const [isInitialized, setIsInitialized] = useState(false);
  const [isChecking, setIsChecking] = useState(false);
  const [progress, setProgress] = useState(0);
  const [currentStep, setCurrentStep] = useState('initializing');
  const [error, setError] = useState<string | null>(null);
  const [cameraPermission, setCameraPermission] = useState<'granted' | 'denied' | 'prompt'>('prompt');

  const videoRef = useRef<HTMLVideoElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const livenessRef = useRef<LivenessDetection | null>(null);

  // Initialize liveness detection
  useEffect(() => {
    const initialize = async () => {
      try {
        setCurrentStep('initializing');
        livenessRef.current = new LivenessDetection();
        await livenessRef.current.loadModels();
        setIsInitialized(true);
        setCurrentStep('ready');
      } catch (err) {
        console.error('Failed to initialize liveness detection:', err);
        setError('Failed to initialize face detection. Please refresh the page.');
      }
    };

    initialize();

    return () => {
      // Cleanup
      if (streamRef.current) {
        streamRef.current.getTracks().forEach(track => track.stop());
      }
    };
  }, []);

  // Start camera
  const startCamera = useCallback(async () => {
    try {
      setCurrentStep('requesting_camera');
      
      const stream = await navigator.mediaDevices.getUserMedia({
        video: {
          width: { ideal: 640 },
          height: { ideal: 480 },
          facingMode: 'user'
        },
        audio: false
      });

      streamRef.current = stream;
      
      if (videoRef.current) {
        videoRef.current.srcObject = stream;
        await videoRef.current.play();
      }

      setCameraPermission('granted');
      setCurrentStep('ready');
    } catch (err) {
      console.error('Camera access denied:', err);
      setCameraPermission('denied');
      setError('Camera access is required for liveness verification. Please allow camera access.');
    }
  }, []);

  // Perform liveness check
  const performLivenessCheck = useCallback(async () => {
    if (!isInitialized || !videoRef.current || !canvasRef.current || !livenessRef.current) {
      setError('Liveness detection not ready');
      return;
    }

    try {
      setIsChecking(true);
      setError(null);
      setCurrentStep('checking');
      setProgress(0);

      const result = await livenessRef.current.performLivenessCheck(
        videoRef.current,
        canvasRef.current,
        (progress) => {
          setProgress(progress);
          
          // Update step based on progress
          if (progress < 0.3) {
            setCurrentStep('position_face');
          } else if (progress < 0.6) {
            setCurrentStep('detect_motion');
          } else if (progress < 0.9) {
            setCurrentStep('detect_blink');
          } else {
            setCurrentStep('finalizing');
          }
        }
      );

      onLivenessComplete(result);
    } catch (err) {
      console.error('Liveness check failed:', err);
      setError('Liveness check failed. Please try again.');
      setCurrentStep('ready');
    } finally {
      setIsChecking(false);
    }
  }, [isInitialized, onLivenessComplete]);

  // Get step message
  const getStepMessage = () => {
    switch (currentStep) {
      case 'initializing':
        return 'Initializing face detection...';
      case 'requesting_camera':
        return 'Requesting camera access...';
      case 'ready':
        return 'Position your face in the camera';
      case 'position_face':
        return 'Keep your face centered in the frame';
      case 'detect_motion':
        return 'Turn your head slightly to the left and right';
      case 'detect_blink':
        return 'Please blink naturally';
      case 'finalizing':
        return 'Finalizing verification...';
      case 'checking':
        return 'Analyzing...';
      default:
        return 'Preparing...';
    }
  };

  // Get step icon
  const getStepIcon = () => {
    switch (currentStep) {
      case 'initializing':
      case 'requesting_camera':
        return '⏳';
      case 'ready':
        return '📷';
      case 'position_face':
        return '😊';
      case 'detect_motion':
        return '🔄';
      case 'detect_blink':
        return '👁️';
      case 'finalizing':
      case 'checking':
        return '✅';
      default:
        return '⏳';
    }
  };

  return (
    <div className="liveness-check">
      <div className="liveness-check__header">
        <h2>Liveness Verification</h2>
        <p>We need to verify that you're a real person</p>
      </div>

      {error && (
        <div className="liveness-check__error">
          <span className="error-icon">⚠️</span>
          <span>{error}</span>
        </div>
      )}

      <div className="liveness-check__camera">
        <video
          ref={videoRef}
          className="liveness-check__video"
          autoPlay
          playsInline
          muted
        />
        <canvas
          ref={canvasRef}
          className="liveness-check__canvas"
          style={{ display: 'none' }}
        />
        
        {/* Camera overlay */}
        <div className="liveness-check__overlay">
          <div className="liveness-check__frame">
            <div className="liveness-check__corner top-left"></div>
            <div className="liveness-check__corner top-right"></div>
            <div className="liveness-check__corner bottom-left"></div>
            <div className="liveness-check__corner bottom-right"></div>
          </div>
        </div>
      </div>

      <div className="liveness-check__status">
        <div className="liveness-check__icon">
          <span className="icon-large">{getStepIcon()}</span>
        </div>
        <div className="liveness-check__message">
          <h3>{getStepMessage()}</h3>
          {isChecking && (
            <div className="liveness-check__progress">
              <div 
                className="liveness-check__progress-bar"
                style={{ width: `${progress * 100}%` }}
              />
              <span>{Math.round(progress * 100)}%</span>
            </div>
          )}
        </div>
      </div>

      <div className="liveness-check__actions">
        {!isInitialized ? (
          <div className="loading-spinner">
            <div className="spinner"></div>
            <span>Loading face detection models...</span>
          </div>
        ) : cameraPermission === 'prompt' ? (
          <button
            className="btn btn-primary"
            onClick={startCamera}
          >
            Enable Camera
          </button>
        ) : cameraPermission === 'denied' ? (
          <div className="permission-denied">
            <p>Camera access was denied. Please enable camera access in your browser settings and refresh the page.</p>
            <button className="btn btn-secondary" onClick={onCancel}>
              Cancel
            </button>
          </div>
        ) : (
          <>
            {!isChecking ? (
              <div className="action-buttons">
                <button
                  className="btn btn-primary"
                  onClick={performLivenessCheck}
                >
                  Start Verification
                </button>
                <button
                  className="btn btn-secondary"
                  onClick={onCancel}
                >
                  Cancel
                </button>
              </div>
            ) : (
              <button
                className="btn btn-secondary"
                onClick={() => {
                  setIsChecking(false);
                  setCurrentStep('ready');
                  setProgress(0);
                }}
              >
                Cancel Check
              </button>
            )}
          </>
        )}
      </div>

      <style>{`
        .liveness-check {
          max-width: 600px;
          margin: 0 auto;
          padding: 20px;
          text-align: center;
        }

        .liveness-check__header {
          margin-bottom: 20px;
        }

        .liveness-check__header h2 {
          margin: 0 0 10px 0;
          color: #333;
        }

        .liveness-check__header p {
          margin: 0;
          color: #666;
        }

        .liveness-check__error {
          background: #fee;
          border: 1px solid #fcc;
          border-radius: 8px;
          padding: 12px;
          margin-bottom: 20px;
          display: flex;
          align-items: center;
          gap: 8px;
        }

        .error-icon {
          color: #c33;
        }

        .liveness-check__camera {
          position: relative;
          width: 100%;
          max-width: 400px;
          margin: 0 auto 20px;
          aspect-ratio: 4/3;
          background: #000;
          border-radius: 12px;
          overflow: hidden;
        }

        .liveness-check__video {
          width: 100%;
          height: 100%;
          object-fit: cover;
        }

        .liveness-check__overlay {
          position: absolute;
          top: 0;
          left: 0;
          right: 0;
          bottom: 0;
          pointer-events: none;
        }

        .liveness-check__frame {
          position: absolute;
          top: 50%;
          left: 50%;
          transform: translate(-50%, -50%);
          width: 60%;
          height: 70%;
          border: 2px solid #4CAF50;
          border-radius: 12px;
        }

        .liveness-check__corner {
          position: absolute;
          width: 20px;
          height: 20px;
          border: 3px solid #4CAF50;
        }

        .liveness-check__corner.top-left {
          top: -3px;
          left: -3px;
          border-right: none;
          border-bottom: none;
          border-radius: 8px 0 0 0;
        }

        .liveness-check__corner.top-right {
          top: -3px;
          right: -3px;
          border-left: none;
          border-bottom: none;
          border-radius: 0 8px 0 0;
        }

        .liveness-check__corner.bottom-left {
          bottom: -3px;
          left: -3px;
          border-right: none;
          border-top: none;
          border-radius: 0 0 0 8px;
        }

        .liveness-check__corner.bottom-right {
          bottom: -3px;
          right: -3px;
          border-left: none;
          border-top: none;
          border-radius: 0 0 8px 0;
        }

        .liveness-check__status {
          display: flex;
          align-items: center;
          gap: 16px;
          margin-bottom: 20px;
          padding: 16px;
          background: #f8f9fa;
          border-radius: 8px;
        }

        .liveness-check__icon {
          flex-shrink: 0;
        }

        .icon-large {
          font-size: 32px;
        }

        .liveness-check__message {
          flex: 1;
        }

        .liveness-check__message h3 {
          margin: 0 0 8px 0;
          color: #333;
        }

        .liveness-check__progress {
          display: flex;
          align-items: center;
          gap: 8px;
        }

        .liveness-check__progress-bar {
          flex: 1;
          height: 4px;
          background: #ddd;
          border-radius: 2px;
          overflow: hidden;
        }

        .liveness-check__progress-bar {
          background: #4CAF50;
          transition: width 0.3s ease;
        }

        .liveness-check__actions {
          display: flex;
          flex-direction: column;
          gap: 12px;
        }

        .action-buttons {
          display: flex;
          gap: 12px;
          justify-content: center;
        }

        .btn {
          padding: 12px 24px;
          border: none;
          border-radius: 6px;
          font-size: 16px;
          cursor: pointer;
          transition: all 0.2s ease;
        }

        .btn-primary {
          background: #4CAF50;
          color: white;
        }

        .btn-primary:hover {
          background: #45a049;
        }

        .btn-secondary {
          background: #6c757d;
          color: white;
        }

        .btn-secondary:hover {
          background: #5a6268;
        }

        .loading-spinner {
          display: flex;
          align-items: center;
          justify-content: center;
          gap: 12px;
          color: #666;
        }

        .spinner {
          width: 20px;
          height: 20px;
          border: 2px solid #ddd;
          border-top: 2px solid #4CAF50;
          border-radius: 50%;
          animation: spin 1s linear infinite;
        }

        @keyframes spin {
          0% { transform: rotate(0deg); }
          100% { transform: rotate(360deg); }
        }

        .permission-denied {
          color: #666;
        }

        .permission-denied p {
          margin-bottom: 16px;
        }
      `}</style>
    </div>
  );
};
