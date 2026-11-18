class LowPassFilter {
  private y = 0;
  private s = 0;
  private initialized = false;
  private alpha: number;

  constructor(alpha: number) {
    this.alpha = alpha;
  }

  filter(value: number, alpha: number): number {
    this.alpha = alpha;
    if (!this.initialized) {
      this.s = value;
      this.initialized = true;
    } else {
      this.s = this.alpha * value + (1 - this.alpha) * this.s;
    }
    this.y = value;
    return this.s;
  }

  lastRaw(): number {
    return this.y;
  }

  reset() {
    this.initialized = false;
  }
}

export class OneEuroFilter {
  private freq: number;
  private minCutoff: number;
  private beta: number;
  private dCutoff: number;
  private xFilter: LowPassFilter;
  private dxFilter: LowPassFilter;
  private lastTime: number | null = null;

  constructor(freq = 30, minCutoff = 1.0, beta = 0.05, dCutoff = 1.0) {
    this.freq = freq;
    this.minCutoff = minCutoff;
    this.beta = beta;
    this.dCutoff = dCutoff;
    this.xFilter = new LowPassFilter(this.alpha(minCutoff));
    this.dxFilter = new LowPassFilter(this.alpha(dCutoff));
  }

  setParams(minCutoff: number, beta: number) {
    this.minCutoff = minCutoff;
    this.beta = beta;
  }

  private alpha(cutoff: number): number {
    const te = 1.0 / this.freq;
    const tau = 1.0 / (2 * Math.PI * cutoff);
    return 1.0 / (1.0 + tau / te);
  }

  filter(value: number, timestampSec: number): number {
    if (this.lastTime !== null && timestampSec > this.lastTime) {
      this.freq = Math.max(1, 1.0 / (timestampSec - this.lastTime));
    }
    this.lastTime = timestampSec;

    const prev = this.xFilter.lastRaw();
    const dx = this.lastTime === null ? 0 : (value - prev) * this.freq;
    const edx = this.dxFilter.filter(dx, this.alpha(this.dCutoff));
    const cutoff = this.minCutoff + this.beta * Math.abs(edx);
    return this.xFilter.filter(value, this.alpha(cutoff));
  }

  reset() {
    this.lastTime = null;
    this.xFilter.reset();
    this.dxFilter.reset();
  }
}

export class LandmarkOneEuroSmoother {
  private filters: Array<[OneEuroFilter, OneEuroFilter, OneEuroFilter]> = [];

  smooth(
    landmarks: Array<{ x: number; y: number; z: number }>,
    timestampMs: number,
    smoothingStrength: number,
  ): Array<{ x: number; y: number; z: number }> {
    const t = timestampMs / 1000.0;
    // Higher smoothingStrength => lower minCutoff (less jitter) and gentler beta
    const minCutoff = 2.6 - smoothingStrength * 2.15; // 2.6 down to 0.45 Hz
    const beta = 0.08 - smoothingStrength * 0.065;    // 0.08 down to 0.015

    if (this.filters.length !== landmarks.length) {
      this.filters = landmarks.map(() => [
        new OneEuroFilter(30, minCutoff, beta, 1.0),
        new OneEuroFilter(30, minCutoff, beta, 1.0),
        new OneEuroFilter(30, minCutoff, beta, 1.0),
      ]);
    }

    return landmarks.map((lm, idx) => {
      const [fx, fy, fz] = this.filters[idx];
      fx.setParams(minCutoff, beta);
      fy.setParams(minCutoff, beta);
      fz.setParams(minCutoff, beta);
      return {
        x: fx.filter(lm.x, t),
        y: fy.filter(lm.y, t),
        z: fz.filter(lm.z, t),
      };
    });
  }

  reset() {
    this.filters.forEach(([fx, fy, fz]) => {
      fx.reset();
      fy.reset();
      fz.reset();
    });
  }
}
