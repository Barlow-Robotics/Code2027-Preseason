(() => {
  'use strict';
  const root = document.querySelector('[data-control-lab]'); if (!root) return;
  const model = new ControlLab.ArmModel();
  const stages = [
    {name:'Correct the error', text:'Set kP to 8, then try 40. Press Restart motion each time. Does the arm stop exactly at 65°? P needs some error to keep pushing against gravity.', show:['p'], config:{p:8,d:0,g:0,v:0,a:0,profile:false}},
    {name:'Support the weight', text:'Raise kG toward 1.2 V. The arm can now hold near the target without needing a large position error. Try a different target angle: gravity compensation changes with the arm’s angle.', show:['p','g'], config:{p:8,d:0,g:0,v:0,a:0,profile:false}},
    {name:'Plan the movement', text:'The orange reference now travels toward the destination instead of jumping there. Change the speed and acceleration limits. The motor still needs output to follow that reference.', show:['p','g','maxV','maxA'], config:{p:8,d:0,g:1.2,v:0,a:0,profile:true}},
    {name:'Help it follow', text:'Raise kV toward 4, then kA toward 0.31. Compare green Actual with orange Profile target. Feedforward supplies the expected effort; kP corrects the remaining position error.', show:['p','g','v','a','maxV','maxA'], config:{p:8,d:0,g:1.2,v:0,a:0,profile:true}},
    {name:'Add damping when needed', text:'This experiment starts with strong P and an immediate target. Try kD = 0, then 4. D opposes velocity error. It can reduce overshoot; too much can make the response sluggish. Then try the profile checkbox.', show:['p','d','g','v','a','maxV','maxA'], config:{p:80,d:0,g:1.2,v:0,a:0,profile:false}}
  ];
  let stage = 0, running = false, samples = [model.sample()], previous = null, accumulator = 0, lastRender = 0;
  const canvas = root.querySelector('canvas'), ctx = canvas.getContext('2d');
  const controls = [...root.querySelectorAll('[data-gain]')];
  const profile = root.querySelector('#profile-mode');
  function sync() {
    controls.forEach(input => {
      input.value = model.config[input.dataset.gain];
      input.closest('label').querySelector('output').textContent = Number(input.value).toFixed(input.dataset.gain === 'target' ? 0 : 2);
    });
    profile.checked = model.config.profile;
  }
  function reset() { model.reset(); samples = [model.sample()]; previous = null; accumulator = 0; render(); }
  function setStage(n) {
    stage = Math.max(0, Math.min(stages.length-1,n));
    Object.assign(model.config, stages[stage].config, {target:65,maxV:.18,maxA:.35});
    root.querySelector('#experiment-title').textContent = `${stage+1}. ${stages[stage].name}`;
    root.querySelector('#experiment-task').textContent = stages[stage].text;
    root.querySelector('#stage-back').disabled = stage===0;
    root.querySelector('#stage-next').disabled = stage===stages.length-1;
    root.querySelector('#experiment-progress').textContent = `${stage+1} of ${stages.length}`;
    controls.forEach(input => input.closest('label').hidden = input.dataset.gain!=='target' && !stages[stage].show.includes(input.dataset.gain));
    root.querySelector('#profile-label').hidden = stage<2;
    sync(); running = false; root.querySelector('#lab-play').textContent='Run experiment'; reset();
  }
  controls.forEach(input => input.addEventListener('input', () => {
    model.config[input.dataset.gain]=Number(input.value); sync(); render();
  }));
  profile.addEventListener('change', () => {model.config.profile=profile.checked; reset();});
  root.querySelector('#lab-play').addEventListener('click', () => {
    if (model.time>=10) reset();
    running=!running; previous=null; root.querySelector('#lab-play').textContent=running?'Pause':'Resume';
  });
  root.querySelector('#lab-restart').addEventListener('click', () => {reset();running=true;root.querySelector('#lab-play').textContent='Pause';});
  root.querySelector('#stage-back').addEventListener('click', () => setStage(stage-1));
  root.querySelector('#stage-next').addEventListener('click', () => setStage(stage+1));
  root.querySelector('#lab-defaults').addEventListener('click', () => setStage(stage));
  document.addEventListener('visibilitychange',()=>{previous=null;});
  function graph() {
    const width = canvas.clientWidth, height = canvas.clientHeight;
    const scale = window.devicePixelRatio || 1;
    if(canvas.width!==Math.round(width*scale)||canvas.height!==Math.round(height*scale)){
      canvas.width=Math.round(width*scale);canvas.height=Math.round(height*scale);
    }
    ctx.setTransform(scale,0,0,scale,0,0);ctx.clearRect(0,0,width,height);
    const left=37,top=15,w=width-left-12,h=height-top-25;
    const x=t=>left+t/10*w,y=a=>top+(100-a)/120*h;
    ctx.font='11px Segoe UI';ctx.lineWidth=1;ctx.setLineDash([]);
    for(const a of [-20,0,30,60,90]){ctx.strokeStyle='#dce4df';ctx.beginPath();ctx.moveTo(left,y(a));ctx.lineTo(width-12,y(a));ctx.stroke();ctx.fillStyle='#52635e';ctx.fillText(`${a}°`,2,y(a)+4);}
    for(const t of [0,2,4,6,8,10]){ctx.fillStyle='#52635e';ctx.fillText(`${t}s`,x(t)-6,height-3);}
    for(const [key,color,dash] of [['target','#59657c',[5,4]],['reference','#a04e00',[2,3]],['actual','#16634d',[]]]){
      ctx.strokeStyle=color;ctx.lineWidth=key==='actual'?2.5:1.7;ctx.setLineDash(dash);ctx.beginPath();
      samples.forEach((s,i)=>i?ctx.lineTo(x(s.time),y(s[key])):ctx.moveTo(x(s.time),y(s[key])));ctx.stroke();
    }
    ctx.setLineDash([]);
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
    graph();
  }
  function frame(now){
    if(running && !document.hidden){
      if(previous!==null)accumulator+=Math.min((now-previous)/1000,.05);
      while(accumulator>=.002 && model.time<10){model.step(.002);accumulator-=.002;if(Math.round(model.time/.002)%10===0)samples.push(model.sample());}
      if(model.time>=10){running=false;root.querySelector('#lab-play').textContent='Run again';}
      if(now-lastRender>30){render();lastRender=now;}
    }
    previous=now;requestAnimationFrame(frame);
  }
  new ResizeObserver(render).observe(canvas);
  setStage(0);requestAnimationFrame(frame);
})();
