(() => {
  'use strict';
  const root = document.querySelector('[data-control-lab]'); if (!root) return;
  const model = new ControlLab.ArmModel();
  const stages = [
    {name:'Correct the error', concept:'kP is the proportional gain. It applies voltage based on position error: how far the arm is from its target. A larger error or a larger kP means more voltage toward the target.', text:'Set kP = 8, then 40. Press Reset and Run if paused. Does the arm stop exactly at 65°? P needs some error to keep pushing against gravity.', show:['p'], config:{p:8,d:0,g:0,v:0,a:0,profile:false}},
    {name:'Support the weight', concept:'kG adds voltage to counter gravity. For an arm, that voltage changes with its angle. This is feedforward: predicting the effort needed instead of waiting for an error.', text:'Raise kG toward 1.2 V. Does the arm hold closer to the target? Then change the destination angle and try again.', show:['p','g'], config:{p:8,d:0,g:0,v:0,a:0,profile:false}},
    {name:'Plan the movement', concept:'A trajectory is a plan for position over time. A motion profile moves the target gradually, limiting speed and acceleration. The controller follows that moving target instead of aiming straight at the final angle.', text:'Press Run. Watch the orange Profile target move toward the destination. The lower plot shows how far the arm falls behind. Note the largest error, then move to experiment 4.', show:['p','g','maxV','maxA'], config:{p:8,d:0,g:1.2,v:0,a:0,profile:true}},
    {name:'Help it follow', concept:'kV adds voltage for the profile’s planned speed. kA adds voltage for its planned acceleration—how quickly speed changes. These feedforward terms predict the effort needed; P corrects the remaining position error.', text:'Start with the same settings as experiment 3. Set kV to 4 and kA to 0.31, then Reset. Compare the lower error plot and largest error: does the line stay closer to zero?', show:['p','g','v','a','maxV','maxA'], config:{p:8,d:0,g:1.2,v:0,a:0,profile:true}},
    {name:'Add damping when needed', concept:'kD is the derivative gain. It adds voltage based on the difference between target velocity and actual velocity. At a fixed target it resists motion, helping the arm settle instead of swinging past the target.', text:'This experiment starts with strong P. Compare kD = 0 and 4, using Reset each time. Watch for overshoot. Then try the profile checkbox.', show:['p','d','g','v','a','maxV','maxA'], config:{p:80,d:0,g:1.2,v:0,a:0,profile:false}}
  ];
  let stage = 0, running = false, samples = [model.sample()], previous = null, accumulator = 0, lastRender = 0, peakError = 0;
  const canvas = root.querySelector('canvas'), ctx = canvas.getContext('2d');
  const errorPanel = root.querySelector('#tracking-detail');
  const errorCanvas = root.querySelector('#tracking-plot'), errorCtx = errorCanvas.getContext('2d');
  const controls = [...root.querySelectorAll('[data-gain]')];
  const profile = root.querySelector('#profile-mode');
  function sync() {
    controls.forEach(input => {
      input.value = model.config[input.dataset.gain];
      input.closest('label').querySelector('output').textContent = Number(input.value).toFixed(input.dataset.gain === 'target' ? 0 : 2);
    });
    profile.checked = model.config.profile;
  }
  function reset() { model.reset(); samples = [model.sample()]; previous = null; accumulator = 0; peakError = 0; render(); }
  function setStage(n) {
    stage = Math.max(0, Math.min(stages.length-1,n));
    Object.assign(model.config, stages[stage].config, {target:65,maxV:.18,maxA:.35});
    root.querySelector('#experiment-title').textContent = `${stage+1}. ${stages[stage].name}`;
    root.querySelector('#experiment-concept').textContent = stages[stage].concept;
    root.querySelector('#experiment-task').textContent = stages[stage].text;
    root.querySelector('#stage-back').disabled = stage===0;
    root.querySelector('#stage-next').disabled = stage===stages.length-1;
    root.querySelector('#experiment-progress').textContent = `${stage+1} of ${stages.length}`;
    controls.forEach(input => input.closest('label').hidden = input.dataset.gain!=='target' && !stages[stage].show.includes(input.dataset.gain));
    root.querySelector('#profile-label').hidden = stage<2;
    errorPanel.hidden = stage !== 2 && stage !== 3;
    sync(); running = false; root.querySelector('#lab-play').textContent='Run'; reset();
  }
  controls.forEach(input => input.addEventListener('input', () => {
    model.config[input.dataset.gain]=Number(input.value); sync(); render();
  }));
  profile.addEventListener('change', () => {model.config.profile=profile.checked; reset();});
  root.querySelector('#lab-play').addEventListener('click', () => {
    running=!running; previous=null; root.querySelector('#lab-play').textContent=running?'Pause':'Run';
  });
  root.querySelector('#lab-restart').addEventListener('click', reset);
  root.querySelector('#stage-back').addEventListener('click', () => setStage(stage-1));
  root.querySelector('#stage-next').addEventListener('click', () => setStage(stage+1));
  root.querySelector('#lab-defaults').addEventListener('click', () => {
    for (const gain of ['p','d','g','v','a']) model.config[gain]=stages[stage].config[gain];
    sync(); render();
  });
  document.addEventListener('visibilitychange',()=>{previous=null;});
  function graph() {
    const width = canvas.clientWidth, height = canvas.clientHeight;
    const scale = window.devicePixelRatio || 1;
    if(canvas.width!==Math.round(width*scale)||canvas.height!==Math.round(height*scale)){
      canvas.width=Math.round(width*scale);canvas.height=Math.round(height*scale);
    }
    ctx.setTransform(scale,0,0,scale,0,0);ctx.clearRect(0,0,width,height);
    const left=37,top=15,w=width-left-12,h=height-top-25;
    const start=Math.max(0,model.time-10), end=start+10;
    // Keep ordinary trials comparable; expand only to avoid hiding an excursion.
    let low=0, high=80;
    for(const s of samples) for(const key of ['target','reference','actual']) {
      low=Math.min(low,Math.floor(s[key]/20)*20); high=Math.max(high,Math.ceil(s[key]/20)*20);
    }
    const x=t=>left+(t-start)/10*w,y=a=>top+(high-a)/(high-low)*h;
    ctx.font='11px Segoe UI';ctx.lineWidth=1;ctx.setLineDash([]);
    for(let a=low;a<=high;a+=20){ctx.strokeStyle='#dce4df';ctx.beginPath();ctx.moveTo(left,y(a));ctx.lineTo(width-12,y(a));ctx.stroke();ctx.fillStyle='#52635e';ctx.fillText(`${a}°`,2,y(a)+4);}
    for(let t=Math.ceil(start/2)*2;t<=end;t+=2){ctx.fillStyle='#52635e';ctx.fillText(`${t}s`,x(t)-6,height-3);}
    ctx.save();ctx.beginPath();ctx.rect(left,top,w,h);ctx.clip();
    for(const [key,color,dash] of [['target','#59657c',[5,4]],['reference','#a04e00',[2,3]],['actual','#16634d',[]]]){
      ctx.strokeStyle=color;ctx.lineWidth=key==='actual'?2.5:1.7;ctx.setLineDash(dash);ctx.beginPath();
      samples.forEach((s,i)=>i?ctx.lineTo(x(s.time),y(s[key])):ctx.moveTo(x(s.time),y(s[key])));ctx.stroke();
    }
    ctx.restore();ctx.setLineDash([]);
  }
  function errorGraph() {
    if (errorPanel.hidden) return;
    const width=errorCanvas.clientWidth, height=errorCanvas.clientHeight, scale=window.devicePixelRatio||1;
    if(errorCanvas.width!==Math.round(width*scale)||errorCanvas.height!==Math.round(height*scale)) {
      errorCanvas.width=Math.round(width*scale);errorCanvas.height=Math.round(height*scale);
    }
    const c=errorCtx,left=37,top=10,w=width-left-12,h=height-top-22;
    const start=Math.max(0,model.time-10),x=t=>left+(t-start)/10*w,y=e=>top+(30-e)/60*h;
    c.setTransform(scale,0,0,scale,0,0);c.clearRect(0,0,width,height);c.font='11px Segoe UI';c.lineWidth=1;
    for(const e of [-30,0,30]) {
      c.strokeStyle=e===0?'#52635e':'#dce4df';c.beginPath();c.moveTo(left,y(e));c.lineTo(width-12,y(e));c.stroke();
      c.fillStyle='#52635e';c.fillText(`${e>0?'+':''}${e}°`,0,y(e)+4);
    }
    for(let t=Math.ceil(start/2)*2;t<=start+10;t+=2){c.fillText(`${t}s`,x(t)-6,height-2);}
    c.save();c.beginPath();c.rect(left,top,w,h);c.clip();c.strokeStyle='#16634d';c.lineWidth=2.5;c.beginPath();
    samples.forEach((s,i)=>i?c.lineTo(x(s.time),y(s.reference-s.actual)):c.moveTo(x(s.time),y(s.reference-s.actual)));
    c.stroke();c.restore();
    root.querySelector('#tracking-peak').textContent=`Largest error this run: ${peakError.toFixed(2)}°`;
    root.querySelector('#tracking-summary').textContent=`Tracking error: ${(model.sample().reference-model.sample().actual).toFixed(2)} degrees. Largest absolute error this run: ${peakError.toFixed(2)} degrees. Plot scale: minus 30 to plus 30 degrees.`;
  }
  function render() {
    const s=model.sample();
    root.querySelector('#actual-arm').setAttribute('transform',`rotate(${-s.actual} 80 155)`);
    root.querySelector('#target-arm').setAttribute('transform',`rotate(${-s.target} 80 155)`);
    root.querySelector('#profile-arm').setAttribute('transform',`rotate(${-s.reference} 80 155)`);
    root.querySelector('#arm-readout').textContent=`Actual ${s.actual.toFixed(1)}° · Destination ${s.target.toFixed(0)}°`;
    root.querySelector('#voltage-readout').textContent=`Motor ${s.voltage.toFixed(2)} V${s.limited?' · at voltage limit':''}`;
    root.querySelector('#error-readout').textContent=`Tracking error ${(s.reference-s.actual).toFixed(1)}° · ${s.time.toFixed(1)} s`;
    root.querySelector('#graph-summary').textContent=`At ${s.time.toFixed(1)} seconds: destination ${s.target.toFixed(1)} degrees, controller reference ${s.reference.toFixed(1)} degrees, actual ${s.actual.toFixed(1)} degrees.`;
    graph(); errorGraph();
  }
  function frame(now){
    if(running && !document.hidden){
      if(previous!==null)accumulator+=Math.min((now-previous)/1000,.05);
      while(accumulator>=.002){const sample=model.step(.002);peakError=Math.max(peakError,Math.abs(sample.reference-sample.actual));accumulator-=.002;if(Math.round(model.time/.002)%10===0)samples.push(sample);}
      // Retain one point before the visible window so the trace reaches its left edge.
      while(samples.length>2 && samples[1].time<model.time-10)samples.shift();
      if(now-lastRender>30){render();lastRender=now;}
    }
    previous=now;requestAnimationFrame(frame);
  }
  new ResizeObserver(render).observe(canvas);
  new ResizeObserver(render).observe(errorCanvas);
  setStage(0);requestAnimationFrame(frame);
})();
