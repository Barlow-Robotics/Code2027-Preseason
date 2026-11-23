/* Behavior illustration: preset edits never change a movement already requested. */
document.querySelectorAll('[data-target-preview]').forEach(demo => {
  const elevator = demo.dataset.targetPreview === 'Elevator';
  const names = elevator ? ['BOTTOM', 'MID', 'TOP'] : ['LOW', 'HIGH'];
  const defaults = elevator ? [0, .35, .65] : [0, 65];
  const fields = [...demo.querySelectorAll('[data-preset]')];
  const status = demo.querySelector('[data-preview-status]');
  const reduced = matchMedia('(prefers-reduced-motion: reduce)');
  let presets = [...defaults], selected = 0, actual = defaults[0], goal = actual;
  let frame = null, previous = null;
  const value = n => `${n.toFixed(elevator ? 2 : 0)}${elevator ? ' m' : '°'}`;
  function draw() {
    if (elevator) {
      const y = n => 135 - n / .9 * 105;
      demo.querySelector('[data-level-carriage]').setAttribute('transform', `translate(0 ${y(actual)-135})`);
      demo.querySelector('[data-level-cable]').setAttribute('d', `M82.5 30V${y(actual)}`);
      demo.querySelectorAll('[data-level-marker]').forEach(marker => {
        const i = Number(marker.dataset.levelMarker);
        marker.setAttribute('transform', `translate(0 ${y(presets[i])})`);
        marker.querySelector('text').textContent = `${names[i]} · ${value(presets[i])}`;
        marker.classList.toggle('is-target', i === selected);
      });
    } else {
      demo.querySelector('[data-preview-arm]').setAttribute('transform', `rotate(${-actual} 80 155)`);
      demo.querySelectorAll('[data-preset-line]').forEach(line => {
        const i = Number(line.dataset.presetLine);
        line.setAttribute('transform', `rotate(${-presets[i]} 80 155)`);
        line.classList.toggle('is-target', i === selected);
      });
    }
    demo.querySelector('[data-preview-readout]').textContent = `Actual ${value(actual)} · Commanded ${value(goal)}`;
  }
  function stop() { if (frame !== null) cancelAnimationFrame(frame); frame = null; previous = null; }
  function tick(time) {
    frame = null;
    if (document.hidden || demo.closest('[data-mechanism]').hidden) { previous = null; return; }
    const dt = previous === null ? 0 : Math.min(time-previous, 50)/1000;
    previous = time;
    actual += Math.sign(goal-actual)*Math.min(Math.abs(goal-actual), dt*(elevator ? .22 : 35));
    draw();
    if (Math.abs(goal-actual) > 1e-6) frame=requestAnimationFrame(tick);
    else { previous=null; status.textContent=`Holding ${names[selected]} at ${value(goal)}.`; }
  }
  function move(up) {
    selected=elevator ? Math.max(0,Math.min(2,selected+(up?1:-1))) : (up?1:0);
    goal=presets[selected]; stop();
    if (reduced.matches) actual=goal;
    status.textContent=`${actual===goal?'Holding':'Moving to'} ${names[selected]} at ${value(goal)}.`;
    draw(); if (actual!==goal) frame=requestAnimationFrame(tick);
  }
  fields.forEach((field,i)=>field.addEventListener('input',()=>{
    const n=field.valueAsNumber;
    if (!field.validity.valid || !Number.isFinite(n)) {
      status.textContent=`Enter a number from ${field.min} to ${field.max}.`; return;
    }
    presets[i]=n; draw();
    status.textContent=`${names[i]} saved at ${value(n)}. Commanded position stays ${value(goal)} until a button press.`;
  }));
  demo.querySelector('[data-preview-up]').addEventListener('click',()=>move(true));
  demo.querySelector('[data-preview-down]').addEventListener('click',()=>move(false));
  demo.querySelector('[data-target-reset]').addEventListener('click',()=>{
    stop(); presets=[...defaults]; selected=0; actual=goal=defaults[0];
    fields.forEach((field,i)=>field.value=presets[i]); draw();
    status.textContent='Edit a preset, then press a controller button.';
  });
  window.addEventListener('keydown',event=>{
    if (demo.closest('[data-mechanism]').hidden || event.repeat || event.isComposing || event.ctrlKey || event.metaKey || event.altKey
      || event.target?.closest?.('input,textarea,select,[contenteditable]')) return;
    const key=event.key.toLowerCase(), up=elevator?'y':'b', down=elevator?'a':'x';
    if (key===up || key===down) {event.preventDefault();move(key===up);}
  });
  function resume() { if (!document.hidden && !demo.closest('[data-mechanism]').hidden && frame===null && actual!==goal) frame=requestAnimationFrame(tick); }
  document.addEventListener('visibilitychange',resume);
  document.querySelectorAll('[name="tuning-mechanism"]').forEach(input=>input.addEventListener('change',resume));
  reduced.addEventListener('change',()=>{if(reduced.matches){stop();actual=goal;draw();status.textContent=`Holding ${names[selected]} at ${value(goal)}.`;}});
  draw();
});

