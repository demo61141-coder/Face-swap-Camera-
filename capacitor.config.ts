import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.example.facecam',
  appName: 'FaceSwap Cam',
  webDir: 'dist',
  server: {
    androidScheme: 'https',
  },
};

export default config;
