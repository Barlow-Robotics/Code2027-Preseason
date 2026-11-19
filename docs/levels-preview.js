/* Preview the final lesson: each press selects an adjacent destination. */
document.querySelectorAll('[data-levels-preview]').forEach(demo => {
  const levels = [{name:'BOTTOM', height:0}, {name:'MID', height:0.35}, {name:'TOP', height:0.65}];
  const carriage = demo.querySelector('[data-level-carriage]');
  const cable = demo.querySelector('[data-level-cable]');
  const status = demo.querySelector('[data-level-status]');
  const reduced = window.matchMedia('(prefers-reduced-motion: reduce)');
  const markers = demo.querySelectorAll('[data-level-marker]');
  let selected = 0, height = 0, frame = null, previous = null;
  function draw() {
    const lift = height / 0.65 * 80;
    carriage.setAttribute('transform', `translate(0 ${-lift})`);
    cable.setAttribute('d', `M82.5 30V${135 - lift}`);
    markers.forEach(marker => marker.classList.toggle('is-target', Number(marker.dataset.levelMarker) === selected));
  }
  function stop() {
    if (frame !== null) cancelAnimationFrame(frame);
    frame = null; previous = null;
  }
  function tick(time) {
    frame = null;
    const step = previous === null ? 0 : Math.min(time - previous, 100) / 1000 * 0.1;
    previous = time;
    const distance = levels[selected].height - height;
    height += Math.sign(distance) * Math.min(Math.abs(distance), step);
    draw();
    if (Math.abs(levels[selected].height - height) < 0.000001) {
      status.textContent = `Target: ${levels[selected].name} · Holding`;
      previous = null;
    } else frame = requestAnimationFrame(tick);
  }
  function select(direction) {
    selected = Math.max(0, Math.min(levels.length - 1, selected + direction));
    if (reduced.matches) height = levels[selected].height;
    const arrived = Math.abs(levels[selected].height - height) < 0.000001;
    status.textContent = `Target: ${levels[selected].name} · ${arrived ? 'Holding' : 'Moving'}`;
    draw();
    if (!arrived && frame === null) frame = requestAnimationFrame(tick);
  }
  demo.querySelector('[data-level-up]').addEventListener('click', () => select(1));
  demo.querySelector('[data-level-down]').addEventListener('click', () => select(-1));
  window.addEventListener('keydown', event => {
    const key = event.key.toLowerCase();
    if (!['y','a'].includes(key) || event.repeat || event.isComposing
        || event.ctrlKey || event.altKey || event.metaKey
        || event.target?.closest?.('input, textarea, select, [contenteditable]:not([contenteditable="false"])')) return;
    event.preventDefault(); select(key === 'y' ? 1 : -1);
  });
  demo.querySelector('[data-level-reset]').addEventListener('click', () => {
    stop(); selected = 0; height = 0;
    status.textContent = 'Target: BOTTOM · Holding'; draw();
  });
  reduced.addEventListener('change', () => {
    if (reduced.matches) { stop(); height = levels[selected].height; select(0); }
  });
  draw();
});
