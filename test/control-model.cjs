const assert=require('node:assert/strict');
const {ArmModel}=require('../docs/control-model.js');
function run(c){let m=new ArmModel(c),peak=-Infinity,error=0,maxVelocity=0,maxAcceleration=0;for(let i=0;i<5000;i++){let s=m.step();assert(Number.isFinite(s.actual));assert(Math.abs(s.voltage)<=12);assert(s.actual>=-20-1e-8&&s.actual<=95+1e-8);peak=Math.max(peak,s.actual);error+=Math.abs(s.actual-s.reference);maxVelocity=Math.max(maxVelocity,Math.abs(m.referenceVelocity));maxAcceleration=Math.max(maxAcceleration,Math.abs(m.referenceAcceleration));}return {m,peak,error:error/5000,maxVelocity,maxAcceleration};}
let p=run({p:8}),g=run({p:8,g:1.2});assert(p.m.sample().actual<60);assert(Math.abs(g.m.sample().actual-65)<.1);
let undamped=run({p:80,g:1.2}),damped=run({p:80,g:1.2,d:4});assert(undamped.peak>70);assert(damped.peak<67);
let feedback=run({p:8,g:1.2,profile:true}),ff=run({p:8,g:1.2,v:4,a:.314,profile:true});assert(ff.error<feedback.error/10);assert(Math.abs(ff.m.reference-65*Math.PI/180)<.001);assert(ff.maxVelocity<=.18*2*Math.PI+.001);assert(ff.maxAcceleration<=.35*2*Math.PI+.001);
for(const target of [15,40,80])run({target,p:100,d:12,g:2,v:8,a:.8,profile:true,maxV:.35,maxA:.8});
let m=new ArmModel({profile:true,g:1.2,p:40,v:4,a:.314});for(let i=0;i<300;i++)m.step();m.config.target=20;for(let i=0;i<4000;i++)m.step();assert(Math.abs(m.sample().actual-20)<.1);m.reset();assert.equal(m.time,0);assert(Math.abs(m.sample().actual-15)<.001);
console.log('PASS: gravity offset, feedforward tracking, D damping, profile speed/acceleration bounds, mid-motion retarget, saturation, travel limits, reset');
