/* Teaching model, not a Phoenix emulator. SI physics; gains use mechanism turns. */
(function (root) {
  'use strict';
  const TAU = 2 * Math.PI;
  const clamp = (v, lo, hi) => Math.max(lo, Math.min(hi, v));
  const defaults = {p: 8, d: 0, g: 0, v: 0, a: 0, maxV: .18, maxA: .35, target: 65, profile: false};
  class ArmModel {
    constructor(config = {}) { this.config = {...defaults, ...config}; this.reset(); }
    reset() {
      this.time = 0; this.angle = 15 * Math.PI / 180; this.velocity = 0;
      this.reference = this.angle; this.referenceVelocity = 0; this.referenceAcceleration = 0;
      this.voltage = 0; this.terms = {p:0,d:0,g:0,v:0,a:0}; this.limited = false;
    }
    step(dt = .002) {
      const c = this.config, goal = c.target * Math.PI / 180;
      if (c.profile) {
        // Trapezoidal profile: choose a velocity that can brake before the goal.
        const error = goal - this.reference;
        const maxA = c.maxA * TAU, maxV = c.maxV * TAU;
        const desiredVelocity = Math.sign(error) * Math.min(maxV, Math.sqrt(2 * maxA * Math.abs(error)));
        const previousVelocity = this.referenceVelocity;
        this.referenceVelocity += clamp(desiredVelocity - previousVelocity, -maxA * dt, maxA * dt);
        const next = this.reference + this.referenceVelocity * dt;
        if (Math.abs(error) < maxA * dt * dt && Math.abs(previousVelocity) <= maxA * dt) {
          this.reference = goal; this.referenceVelocity = 0;
        } else { this.reference = next; }
        this.referenceAcceleration = (this.referenceVelocity - previousVelocity) / dt;
      } else {
        this.reference = goal; this.referenceVelocity = 0; this.referenceAcceleration = 0;
      }
      this.terms = {
        p: c.p * (this.reference - this.angle) / TAU,
        d: c.d * (this.referenceVelocity - this.velocity) / TAU,
        g: c.g * Math.cos(this.angle),
        v: c.v * this.referenceVelocity / TAU,
        a: c.a * this.referenceAcceleration / TAU
      };
      const requested = Object.values(this.terms).reduce((a,b) => a+b, 0);
      this.voltage = clamp(requested, -12, 12); this.limited = Math.abs(requested) > 12;
      // DC-motor back EMF supplies damping with voltage control. Gravity acts at all times.
      // Physical model: kG=1.2 V, kV=4 V/(turn/s), kA=2*pi/20 V/(turn/s^2).
      const acceleration = 20 * (this.voltage - 4 * this.velocity / TAU - 1.2 * Math.cos(this.angle));
      this.velocity += acceleration * dt;
      this.angle += this.velocity * dt;
      const bounded = clamp(this.angle, -20*Math.PI/180, 95*Math.PI/180);
      if (bounded !== this.angle) { this.angle = bounded; this.velocity = 0; }
      this.time += dt;
      return this.sample();
    }
    sample() { return {time:this.time, actual:this.angle*180/Math.PI, reference:this.reference*180/Math.PI,
      target:this.config.target, voltage:this.voltage, velocity:this.velocity, limited:this.limited}; }
  }
  const api = {ArmModel, defaults};
  if (typeof module !== 'undefined' && module.exports) module.exports = api;
  else root.ControlLab = api;
})(typeof window !== 'undefined' ? window : globalThis);
