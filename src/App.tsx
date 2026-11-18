import React, { useEffect, useRef, useState } from 'react';
import {
  FaceLandmarker,
  FilesetResolver,
  type NormalizedLandmark,
} from '@mediapipe/tasks-vision';
import {
  BookOpen,
  Camera,
  CheckCircle2,
  FlipHorizontal,
  ImagePlus,
  RefreshCw,
  Settings,
  Sparkles,
  Video,
  X,
  XCircle,
} from 'lucide-react';
import { DEMO_FACES, type DemoFaceMeta } from './data/demoFaces';
import { buildFaceMeshTriangles } from './utils/faceMeshTriangulation';
import { FaceWarpGlRenderer } from './utils/faceWarpGl';
import { LandmarkOneEuroSmoother } from './utils/oneEuroFilter';

type ResolutionPreset = '480p' | '720p' | '1080p';

interface SettingsState {
  facingMode: 'user' | 'environment';
  smoothing: number;
  opacity: number;
  resolution: ResolutionPreset;
  mirrored: boolean;
}

interface LoadedFace extends DemoFaceMeta {
  image: HTMLImageElement;
  landmarks: NormalizedLandmark[];
}

const RESOLUTION_MAP: Record<ResolutionPreset, { width: number; height: number }> = {
  '480p': { width: 854, height: 480 },
  '720p': { width: 1280, height: 720 },
  '1080p': { width: 1920, height: 1080 },
};

function formatTime(ms: number): string {
  const sec = Math.floor(ms / 1000);
  const m = String(Math.floor(sec / 60)).padStart(2, '0');
  const s = String(sec % 60).padStart(2, '0');
  return `${m}:${s}`;
}

export default function App() {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  const rendererRef = useRef<FaceWarpGlRenderer | null>(null);
  const videoLandmarkerRef = useRef<FaceLandmarker | null>(null);
  const imageLandmarkerRef = useRef<FaceLandmarker | null>(null);
  const smootherRef = useRef(new LandmarkOneEuroSmoother());
  const streamRef = useRef<MediaStream | null>(null);
  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const recordedChunksRef = useRef<Blob[]>([]);

  const [faces, setFaces] = useState<DemoFaceMeta[]>(DEMO_FACES);
  const loadedFacesRef = useRef<Map<string, LoadedFace>>(new Map());
  const [selectedFaceId, setSelectedFaceId] = useState<string | null>(DEMO_FACES[0].id);
  const [settings, setSettings] = useState<SettingsState>({
    facingMode: 'user',
    smoothing: 0.65,
    opacity: 0.92,
    resolution: '720p',
    mirrored: true,
  });

  const [isTracking, setIsTracking] = useState(false);
  const [isRecording, setIsRecording] = useState(false);
  const [recordingMs, setRecordingMs] = useState(0);
  const [showSettings, setShowSettings] = useState(false);
  const [showGuide, setShowGuide] = useState(false);
  const [statusError, setStatusError] = useState<string | null>(null);
  const [statusErrorKind, setStatusErrorKind] = useState<'camera' | 'model' | null>(null);
  const [cameraAttempt, setCameraAttempt] = useState(0);
  const [modelReady, setModelReady] = useState(false);

  const settingsRef = useRef(settings);
  settingsRef.current = settings;
  const selectedFaceIdRef = useRef(selectedFaceId);
  selectedFaceIdRef.current = selectedFaceId;

  useEffect(() => {
    let active = true;
    async function initMediaPipe() {
      try {
        // The face model + wasm runtime are bundled inside the app
        // (public/wasm and public/models), so the app works offline.
        // CDN is only a fallback. GPU is tried first, CPU if GPU fails.
        const combos: Array<{
          wasm: string;
          model: string;
          delegate: 'GPU' | 'CPU';
        }> = [
          { wasm: 'wasm', model: 'models/face_landmarker.task', delegate: 'GPU' },
          { wasm: 'wasm', model: 'models/face_landmarker.task', delegate: 'CPU' },
          {
            wasm: 'https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@0.10.18/wasm',
            model:
              'https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/1/face_landmarker.task',
            delegate: 'GPU',
          },
          {
            wasm: 'https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@0.10.18/wasm',
            model:
              'https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/1/face_landmarker.task',
            delegate: 'CPU',
          },
        ];

        async function tryCreate(
          combo: (typeof combos)[number],
          runningMode: 'VIDEO' | 'IMAGE',
          blendshapes: boolean,
        ) {
          const vision = await FilesetResolver.forVisionTasks(combo.wasm);
          return FaceLandmarker.createFromOptions(vision, {
            baseOptions: { modelAssetPath: combo.model, delegate: combo.delegate },
            runningMode,
            numFaces: 1,
            ...(blendshapes ? { outputFaceBlendshapes: true } : {}),
          });
        }

        let videoLm: FaceLandmarker | null = null;
        let imgLm: FaceLandmarker | null = null;
        let workingCombo: (typeof combos)[number] | null = null;
        for (const combo of combos) {
          try {
            videoLm = await tryCreate(combo, 'VIDEO', true);
            workingCombo = combo;
            break;
          } catch {
            videoLm = null;
          }
        }
        if (videoLm && workingCombo) {
          try {
            imgLm = await tryCreate(workingCombo, 'IMAGE', false);
          } catch {
            imgLm = null;
          }
          if (!imgLm) {
            for (const combo of combos) {
              if (combo === workingCombo) continue;
              try {
                imgLm = await tryCreate(combo, 'IMAGE', false);
                break;
              } catch {
                imgLm = null;
              }
            }
          }
        }

        if (!videoLm || !imgLm) {
          videoLm?.close();
          imgLm?.close();
          if (!active) return;
          setStatusErrorKind('model');
          setStatusError(
            'Failed to load face model • ফেস মডেল লোড করা যায়নি — ইন্টারনেট চালু করে অ্যাপ আবার খুলুন',
          );
          return;
        }

        if (!active) {
          videoLm.close();
          imgLm.close();
          return;
        }
        videoLandmarkerRef.current = videoLm;
        imageLandmarkerRef.current = imgLm;

        const triangles = buildFaceMeshTriangles(FaceLandmarker.FACE_LANDMARKS_TESSELATION);
        rendererRef.current?.setTriangulation(triangles);

        // Pre-detect landmarks for the 6 demo faces
        for (const meta of DEMO_FACES) {
          try {
            const img = await loadImage(meta.src);
            const res = imgLm.detect(img);
            if (res.faceLandmarks.length > 0) {
              loadedFacesRef.current.set(meta.id, {
                ...meta,
                image: img,
                landmarks: res.faceLandmarks[0],
              });
              if (selectedFaceIdRef.current === meta.id) {
                rendererRef.current?.setCharacterFace(img, res.faceLandmarks[0]);
              }
            }
          } catch {
            // Skip individual preload errors cleanly
          }
        }
        setModelReady(true);
      } catch {
        if (!active) return;
        setStatusErrorKind('model');
        setStatusError(
          'Failed to load face model • ফেস মডেল লোড করা যায়নি — ইন্টারনেট চালু করে অ্যাপ আবার খুলুন',
        );
      }
    }
    initMediaPipe();

    return () => {
      active = false;
      videoLandmarkerRef.current?.close();
      imageLandmarkerRef.current?.close();
    };
  }, []);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const resize = () => {
      canvas.width = window.innerWidth;
      canvas.height = window.innerHeight;
    };
    resize();
    window.addEventListener('resize', resize);
    rendererRef.current = new FaceWarpGlRenderer(canvas);
    return () => window.removeEventListener('resize', resize);
  }, []);

  useEffect(() => {
    let cancelled = false;
    async function startCamera() {
      streamRef.current?.getTracks().forEach((t) => t.stop());
      const res = RESOLUTION_MAP[settings.resolution];
      try {
        const stream = await navigator.mediaDevices.getUserMedia({
          video: {
            facingMode: settings.facingMode,
            width: { ideal: res.width },
            height: { ideal: res.height },
          },
          audio: false,
        });
        if (cancelled) {
          stream.getTracks().forEach((t) => t.stop());
          return;
        }
        streamRef.current = stream;
        if (videoRef.current) {
          videoRef.current.srcObject = stream;
          await videoRef.current.play();
        }
        setStatusErrorKind(null);
        setStatusError(null);
      } catch {
        setStatusErrorKind('camera');
        setStatusError(
          'Camera access denied • ক্যামেরার অনুমতি প্রয়োজন',
        );
      }
    }
    startCamera();
    return () => {
      cancelled = true;
    };
  }, [settings.facingMode, settings.resolution, cameraAttempt]);

  useEffect(() => {
    let rafId = 0;
    let lastVideoTime = -1;

    const tick = () => {
      const video = videoRef.current;
      const canvas = canvasRef.current;
      const renderer = rendererRef.current;
      const lm = videoLandmarkerRef.current;

      if (video && canvas && renderer) {
        let smoothed: Array<{ x: number; y: number; z: number }> | null = null;
        let jawOpen = 0;

        if (lm && video.readyState >= 2 && video.currentTime !== lastVideoTime) {
          lastVideoTime = video.currentTime;
          const nowMs = performance.now();
          const result = lm.detectForVideo(video, nowMs);
          if (result.faceLandmarks.length > 0) {
            setIsTracking(true);
            smoothed = smootherRef.current.smooth(
              result.faceLandmarks[0],
              nowMs,
              settingsRef.current.smoothing,
            );
            const shapes = result.faceBlendshapes?.[0]?.categories;
            jawOpen = shapes?.find((c) => c.categoryName === 'jawOpen')?.score ?? 0;
          } else {
            setIsTracking(false);
            smootherRef.current.reset();
          }
        }

        renderer.renderFrame(
          video,
          canvas.width,
          canvas.height,
          settingsRef.current.mirrored,
          smoothed,
          jawOpen,
          settingsRef.current.opacity,
        );
      }
      rafId = requestAnimationFrame(tick);
    };

    rafId = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(rafId);
  }, []);

  useEffect(() => {
    if (!isRecording) {
      setRecordingMs(0);
      return;
    }
    const start = Date.now();
    const timer = window.setInterval(() => {
      setRecordingMs(Date.now() - start);
    }, 200);
    return () => window.clearInterval(timer);
  }, [isRecording]);

  function handleSelectFace(id: string | null) {
    setSelectedFaceId(id);
    if (!id) {
      rendererRef.current?.clearCharacterFace();
      return;
    }
    const loaded = loadedFacesRef.current.get(id);
    if (loaded) {
      rendererRef.current?.setCharacterFace(loaded.image, loaded.landmarks);
    }
  }

  async function handleUploadFile(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file) return;

    const imgLm = imageLandmarkerRef.current;
    if (!imgLm) {
      setStatusError('Face model still loading • মডেল লোড হচ্ছে, একটু অপেক্ষা করুন');
      return;
    }

    const url = URL.createObjectURL(file);
    try {
      const img = await loadImage(url);
      const res = imgLm.detect(img);
      if (res.faceLandmarks.length === 0) {
        URL.revokeObjectURL(url);
        setStatusError(
          'No face detected in uploaded image • আপলোড করা ছবিতে কোনো মুখ পাওয়া যায়নি',
        );
        return;
      }

      const id = `upload_${Date.now()}`;
      const meta: DemoFaceMeta = {
        id,
        nameEn: 'Custom Face',
        nameBn: 'কাস্টম মুখ',
        styleEn: 'Gallery',
        styleBn: 'গ্যালারি',
        src: url,
        isUploaded: true,
      };
      loadedFacesRef.current.set(id, {
        ...meta,
        image: img,
        landmarks: res.faceLandmarks[0],
      });
      setFaces((prev) => [...prev, meta]);
      setSelectedFaceId(id);
      rendererRef.current?.setCharacterFace(img, res.faceLandmarks[0]);
      setStatusError(null);
    } catch {
      URL.revokeObjectURL(url);
      setStatusError('Could not read selected image • ছবিটি পড়া যায়নি');
    }
  }

  async function handleToggleRecord() {
    if (isRecording) {
      mediaRecorderRef.current?.stop();
      setIsRecording(false);
      return;
    }
    const canvas = canvasRef.current;
    if (!canvas) return;
    const canvasStream = canvas.captureStream(30);

    let micStream: MediaStream | null = null;
    try {
      micStream = await navigator.mediaDevices.getUserMedia({ audio: true });
    } catch {
      micStream = null;
    }

    const audioTracks = micStream?.getAudioTracks() ?? [];
    const combined = new MediaStream([...canvasStream.getVideoTracks(), ...audioTracks]);

    recordedChunksRef.current = [];
    const mimeType = MediaRecorder.isTypeSupported('video/mp4')
      ? 'video/mp4'
      : MediaRecorder.isTypeSupported('video/webm;codecs=vp9,opus')
        ? 'video/webm;codecs=vp9,opus'
        : 'video/webm';

    const ext = mimeType.includes('mp4') ? 'mp4' : 'webm';
    const recorder = new MediaRecorder(combined, { mimeType });
    recorder.ondataavailable = (ev) => {
      if (ev.data.size > 0) recordedChunksRef.current.push(ev.data);
    };
    recorder.onstop = () => {
      micStream?.getTracks().forEach((t) => t.stop());
      const blob = new Blob(recordedChunksRef.current, { type: mimeType });
      const file = new File([blob], `FaceSwapCam_${Date.now()}.${ext}`, { type: mimeType });
      if (navigator.canShare && navigator.canShare({ files: [file] })) {
        navigator.share({ files: [file], title: 'FaceSwap Cam' }).catch(() => {});
      }
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = file.name;
      a.click();
      setTimeout(() => URL.revokeObjectURL(url), 5000);
    };

    mediaRecorderRef.current = recorder;
    recorder.start(250);
    setIsRecording(true);
  }

  return (
    <div className="relative w-full h-full bg-[#090B10] overflow-hidden">
      <video ref={videoRef} playsInline muted className="hidden" />
      <canvas ref={canvasRef} className="absolute inset-0 w-full h-full block" />

      {/* Top Glass Header */}
      <header
        style={{
          position: 'absolute',
          top: 0,
          left: 0,
          right: 0,
          padding: '16px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          background: 'linear-gradient(to bottom, rgba(9,11,16,0.85), rgba(9,11,16,0))',
          zIndex: 20,
        }}
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Sparkles size={18} color="#00E5C3" />
            <span className="font-display" style={{ fontWeight: 700, fontSize: '16px' }}>
              FaceSwap Cam
            </span>
            <span style={{ fontSize: '12px', color: '#9CA3B8' }}>• ফেসসোয়াপ ক্যাম</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '6px',
                padding: '3px 10px',
                borderRadius: '999px',
                fontSize: '11px',
                background: 'rgba(20, 24, 36, 0.82)',
                border: `1px solid ${isTracking ? 'rgba(0,229,195,0.45)' : 'rgba(255,255,255,0.12)'}`,
                color: isTracking ? '#00E5C3' : '#9CA3B8',
              }}
            >
              <span
                style={{
                  width: 7,
                  height: 7,
                  borderRadius: '50%',
                  background: isTracking ? '#00E5C3' : '#64748B',
                }}
              />
              {isTracking ? 'Tracking • ট্র্যাকিং' : modelReady ? 'Find face • মুখ খুঁজছে' : 'Loading • লোড হচ্ছে'}
            </span>
            {isRecording && (
              <span
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '6px',
                  padding: '3px 10px',
                  borderRadius: '999px',
                  fontSize: '11px',
                  fontWeight: 700,
                  background: 'rgba(255,59,92,0.2)',
                  border: '1px solid #FF3B5C',
                  color: '#FF3B5C',
                }}
              >
                REC {formatTime(recordingMs)}
              </span>
            )}
          </div>
        </div>

        <div style={{ display: 'flex', gap: '8px' }}>
          <IconBtn
            label="Flip • ফ্লিপ"
            onClick={() =>
              setSettings((s) => ({
                ...s,
                facingMode: s.facingMode === 'user' ? 'environment' : 'user',
                mirrored: s.facingMode !== 'user',
              }))
            }
          >
            <RefreshCw size={18} />
          </IconBtn>
          <IconBtn label="Guide • গাইড" onClick={() => setShowGuide(true)}>
            <BookOpen size={18} />
          </IconBtn>
          <IconBtn label="Settings • সেটিংস" onClick={() => setShowSettings(true)}>
            <Settings size={18} />
          </IconBtn>
        </div>
      </header>

      {/* Error Toast */}
      {statusError && (
        <div
          style={{
            position: 'absolute',
            top: 84,
            left: 16,
            right: 16,
            zIndex: 30,
            background: 'rgba(42, 12, 20, 0.94)',
            border: '1px solid #FF3B5C',
            borderRadius: 14,
            padding: '12px 14px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: 12,
          }}
        >
          <span style={{ fontSize: 13, color: '#FFE4E8' }}>{statusError}</span>
          <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexShrink: 0 }}>
            {statusErrorKind && (
              <button
                onClick={() => {
                  setStatusError(null);
                  setStatusErrorKind(null);
                  if (statusErrorKind === 'camera') {
                    setCameraAttempt((a) => a + 1);
                  } else {
                    window.location.reload();
                  }
                }}
                style={{
                  background: '#00E5C3',
                  border: 'none',
                  color: '#06231E',
                  fontWeight: 700,
                  borderRadius: 10,
                  padding: '8px 12px',
                  cursor: 'pointer',
                  fontSize: 12,
                  whiteSpace: 'nowrap',
                }}
              >
                আবার চেষ্টা করুন
              </button>
            )}
            <button
              onClick={() => {
                setStatusError(null);
                setStatusErrorKind(null);
              }}
              style={{ background: 'none', border: 'none', color: '#FF3B5C', cursor: 'pointer' }}
            >
              <X size={18} />
            </button>
          </div>
        </div>
      )}

      {/* Bottom Carousel + Record Deck */}
      <footer
        style={{
          position: 'absolute',
          bottom: 0,
          left: 0,
          right: 0,
          padding: '16px 12px 28px',
          background: 'linear-gradient(to top, rgba(9,11,16,0.92), rgba(9,11,16,0))',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          gap: '16px',
          zIndex: 20,
        }}
      >
        <div
          className="no-scrollbar"
          style={{
            width: '100%',
            display: 'flex',
            gap: '10px',
            overflowX: 'auto',
            padding: '4px 6px',
          }}
        >
          <button
            onClick={() => handleSelectFace(null)}
            style={{
              minWidth: 68,
              height: 82,
              borderRadius: 16,
              background: 'rgba(20,24,36,0.85)',
              border: selectedFaceId === null ? '2px solid #00E5C3' : '1px solid rgba(255,255,255,0.12)',
              color: '#F8FAFC',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
              gap: 4,
              cursor: 'pointer',
              flexShrink: 0,
            }}
          >
            <Camera size={20} color={selectedFaceId === null ? '#00E5C3' : '#9CA3B8'} />
            <span style={{ fontSize: 10, fontWeight: 600 }}>Original</span>
            <span style={{ fontSize: 9, color: '#9CA3B8' }}>আসল</span>
          </button>

          {faces.map((face) => {
            const active = selectedFaceId === face.id;
            return (
              <button
                key={face.id}
                onClick={() => handleSelectFace(face.id)}
                style={{
                  minWidth: 70,
                  height: 82,
                  borderRadius: 16,
                  padding: 4,
                  background: active ? 'rgba(0,229,195,0.16)' : 'rgba(20,24,36,0.85)',
                  border: active ? '2px solid #00E5C3' : '1px solid rgba(255,255,255,0.12)',
                  color: '#F8FAFC',
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  cursor: 'pointer',
                  flexShrink: 0,
                }}
              >
                <img
                  src={face.src}
                  alt={face.nameEn}
                  style={{ width: 46, height: 46, borderRadius: 12, objectFit: 'cover' }}
                />
                <span style={{ fontSize: 10, fontWeight: 600, lineHeight: 1.1 }}>{face.nameEn}</span>
                <span style={{ fontSize: 9, color: '#9CA3B8', lineHeight: 1.1 }}>{face.nameBn}</span>
              </button>
            );
          })}

          <button
            onClick={() => fileInputRef.current?.click()}
            style={{
              minWidth: 72,
              height: 82,
              borderRadius: 16,
              background: 'rgba(20,24,36,0.85)',
              border: '1px dashed rgba(0,229,195,0.55)',
              color: '#00E5C3',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
              gap: 4,
              cursor: 'pointer',
              flexShrink: 0,
            }}
          >
            <ImagePlus size={20} />
            <span style={{ fontSize: 10, fontWeight: 600 }}>Upload</span>
            <span style={{ fontSize: 9, color: '#9CA3B8' }}>আপলোড</span>
          </button>
          <input
            ref={fileInputRef}
            type="file"
            accept="image/*"
            className="hidden"
            onChange={handleUploadFile}
          />
        </div>

        {/* Record Shutter Button */}
        <button
          onClick={handleToggleRecord}
          aria-label="Record Video"
          style={{
            width: 72,
            height: 72,
            borderRadius: '50%',
            border: '3px solid #F8FAFC',
            background: 'rgba(20,24,36,0.65)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            cursor: 'pointer',
          }}
        >
          <span
            style={{
              width: isRecording ? 28 : 54,
              height: isRecording ? 28 : 54,
              borderRadius: isRecording ? 8 : '50%',
              background: '#FF3B5C',
              transition: 'all 180ms ease',
            }}
          />
        </button>
      </footer>

      {/* Settings Modal */}
      {showSettings && (
        <ModalSheet
          title="Settings • সেটিংস"
          onClose={() => setShowSettings(false)}
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
            <RowToggle
              icon={<RefreshCw size={18} color="#00E5C3" />}
              label="Camera Lens • ক্যামেরা"
              valueLabel={settings.facingMode === 'user' ? 'Front • সামনে' : 'Rear • পিছনে'}
              onClick={() =>
                setSettings((s) => ({
                  ...s,
                  facingMode: s.facingMode === 'user' ? 'environment' : 'user',
                  mirrored: s.facingMode !== 'user',
                }))
              }
            />

            <RowToggle
              icon={<FlipHorizontal size={18} color="#00E5C3" />}
              label="Mirror Mode • মিরর মোড"
              valueLabel={settings.mirrored ? 'ON • চালু' : 'OFF • বন্ধ'}
              onClick={() => setSettings((s) => ({ ...s, mirrored: !s.mirrored }))}
            />

            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 13, marginBottom: 6 }}>
                <span>Smoothing (One Euro) • স্মুথিং মাত্রা</span>
                <span style={{ color: '#00E5C3', fontWeight: 700 }}>
                  {Math.round(settings.smoothing * 100)}%
                </span>
              </div>
              <input
                type="range"
                min={0}
                max={1}
                step={0.05}
                value={settings.smoothing}
                onChange={(e) => setSettings((s) => ({ ...s, smoothing: Number(e.target.value) }))}
                style={{ width: '100%', accentColor: '#00E5C3' }}
              />
            </div>

            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 13, marginBottom: 6 }}>
                <span>Face Blend / Opacity • ব্লেন্ড ও স্বচ্ছতা</span>
                <span style={{ color: '#00E5C3', fontWeight: 700 }}>
                  {Math.round(settings.opacity * 100)}%
                </span>
              </div>
              <input
                type="range"
                min={0.2}
                max={1}
                step={0.05}
                value={settings.opacity}
                onChange={(e) => setSettings((s) => ({ ...s, opacity: Number(e.target.value) }))}
                style={{ width: '100%', accentColor: '#00E5C3' }}
              />
            </div>

            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 13, marginBottom: 8 }}>
                <Video size={16} color="#00E5C3" />
                <span>Resolution • রেজোলিউশন</span>
              </div>
              <div style={{ display: 'flex', gap: 8 }}>
                {(['480p', '720p', '1080p'] as ResolutionPreset[]).map((res) => (
                  <button
                    key={res}
                    onClick={() => setSettings((s) => ({ ...s, resolution: res }))}
                    style={{
                      flex: 1,
                      padding: '10px 0',
                      borderRadius: 12,
                      border:
                        settings.resolution === res
                          ? '1px solid #00E5C3'
                          : '1px solid rgba(255,255,255,0.14)',
                      background:
                        settings.resolution === res ? 'rgba(0,229,195,0.18)' : 'rgba(255,255,255,0.04)',
                      color: settings.resolution === res ? '#00E5C3' : '#F8FAFC',
                      fontWeight: 600,
                      cursor: 'pointer',
                    }}
                  >
                    {res}
                  </button>
                ))}
              </div>
            </div>
          </div>
        </ModalSheet>
      )}

      {/* Guide Modal */}
      {showGuide && (
        <ModalSheet title="Face Guide • ফেস গাইড" onClose={() => setShowGuide(false)}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
            <div
              style={{
                padding: 14,
                borderRadius: 14,
                background: 'rgba(0,229,195,0.08)',
                border: '1px solid rgba(0,229,195,0.3)',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, color: '#00E5C3', fontWeight: 700, marginBottom: 8 }}>
                <CheckCircle2 size={18} />
                <span>Works Best • সেরা ফলাফলের জন্য</span>
              </div>
              <ul style={{ margin: 0, paddingLeft: 18, fontSize: 13, lineHeight: 1.65, color: '#E2E8F0' }}>
                <li>Front-facing portrait looking straight ahead • সরাসরি সামনের দিকে তাকানো ছবি</li>
                <li>Clear, even face lighting without harsh shadows • পরিষ্কার ও সমান আলো</li>
                <li>No sunglasses or face coverings • সানগ্লাস বা মাস্ক ছাড়া মুখ</li>
                <li>Realistic proportions (photos, realistic 3D) • বাস্তবসম্মত মুখের অনুপাত</li>
              </ul>
            </div>

            <div
              style={{
                padding: 14,
                borderRadius: 14,
                background: 'rgba(255,59,92,0.08)',
                border: '1px solid rgba(255,59,92,0.3)',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, color: '#FF3B5C', fontWeight: 700, marginBottom: 8 }}>
                <XCircle size={18} />
                <span>Avoid • যা এড়িয়ে চলবেন</span>
              </div>
              <ul style={{ margin: 0, paddingLeft: 18, fontSize: 13, lineHeight: 1.65, color: '#E2E8F0' }}>
                <li>Side profiles or tilted heads • একপাশ থেকে তোলা বা বাঁকা মাথা</li>
                <li>Sunglasses, hands, or hair blocking eyes • চোখ ঢাকা সানগ্লাস বা চুল</li>
                <li>Extreme cartoon proportions or blurry photos • অতিরিক্ত কার্টুন বা ঝাপসা ছবি</li>
              </ul>
            </div>

            <div
              style={{
                padding: 14,
                borderRadius: 14,
                background: 'rgba(96,165,250,0.08)',
                border: '1px solid rgba(96,165,250,0.3)',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, color: '#60A5FA', fontWeight: 700, marginBottom: 8 }}>
                <Camera size={18} />
                <span>Camera Permission • ক্যামেরার অনুমতি</span>
              </div>
              <ul style={{ margin: 0, paddingLeft: 18, fontSize: 13, lineHeight: 1.65, color: '#E2E8F0' }}>
                <li>প্রথমবার ক্যামেরা চালু করলে অনুমতি চাইবে — Allow দিন</li>
                <li>ভুল করে Deny করলে: Settings → Apps → FaceSwap Cam → Permissions → Camera → Allow করুন</li>
                <li>মাইকের অনুমতিও দিন — নইলে রেকর্ড করা ভিডিওতে শব্দ আসবে না</li>
                <li>ফেস মডেল ফোনের ভেতরেই থাকে — ইন্টারনেট ছাড়াও ফেস-সোয়াপ চলবে</li>
              </ul>
            </div>
          </div>
        </ModalSheet>
      )}
    </div>
  );
}

function IconBtn({
  label,
  onClick,
  children,
}: {
  label: string;
  onClick: () => void;
  children: React.ReactNode;
}) {
  return (
    <button
      onClick={onClick}
      aria-label={label}
      title={label}
      style={{
        minWidth: 44,
        minHeight: 44,
        borderRadius: 14,
        background: 'rgba(20, 24, 36, 0.82)',
        border: '1px solid rgba(255,255,255,0.14)',
        color: '#F8FAFC',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        cursor: 'pointer',
      }}
    >
      {children}
    </button>
  );
}

function ModalSheet({
  title,
  onClose,
  children,
}: {
  title: string;
  onClose: () => void;
  children: React.ReactNode;
}) {
  return (
    <div
      style={{
        position: 'absolute',
        inset: 0,
        zIndex: 40,
        background: 'rgba(0,0,0,0.65)',
        display: 'flex',
        alignItems: 'flex-end',
        justifyContent: 'center',
      }}
      onClick={onClose}
    >
      <div
        onClick={(e) => e.stopPropagation()}
        style={{
          width: '100%',
          maxWidth: 540,
          maxHeight: '85vh',
          overflowY: 'auto',
          background: '#131722',
          borderTopLeftRadius: 24,
          borderTopRightRadius: 24,
          borderTop: '1px solid rgba(255,255,255,0.12)',
          padding: '20px 20px 32px',
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 18 }}>
          <h2 className="font-display" style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>
            {title}
          </h2>
          <button
            onClick={onClose}
            style={{
              background: 'rgba(255,255,255,0.08)',
              border: 'none',
              color: '#F8FAFC',
              borderRadius: 10,
              padding: 8,
              cursor: 'pointer',
            }}
          >
            <X size={18} />
          </button>
        </div>
        {children}
      </div>
    </div>
  );
}

function RowToggle({
  icon,
  label,
  valueLabel,
  onClick,
}: {
  icon: React.ReactNode;
  label: string;
  valueLabel: string;
  onClick: () => void;
}) {
  return (
    <button
      onClick={onClick}
      style={{
        width: '100%',
        padding: '12px 14px',
        borderRadius: 14,
        background: 'rgba(255,255,255,0.04)',
        border: '1px solid rgba(255,255,255,0.1)',
        color: '#F8FAFC',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        cursor: 'pointer',
      }}
    >
      <span style={{ display: 'flex', alignItems: 'center', gap: 10, fontSize: 13 }}>{icon}{label}</span>
      <span style={{ color: '#00E5C3', fontWeight: 700, fontSize: 12 }}>{valueLabel}</span>
    </button>
  );
}

function loadImage(src: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.crossOrigin = 'anonymous';
    img.onload = () => resolve(img);
    img.onerror = reject;
    img.src = src;
  });
}
