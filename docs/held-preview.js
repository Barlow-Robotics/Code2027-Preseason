/* A behavior illustration, not the robot's physics simulator. */
document.querySelectorAll('[data-held-preview]').forEach((demo) => {
  const button = demo.querySelector('[data-hold-y]');
  const reset = demo.querySelector('[data-preview-reset]');
  const reduced = window.matchMedia('(prefers-reduced-motion: reduce)');
  const panels = ['before', 'after'].map((name) => {
    const panel = demo.querySelector(`[data-preview-panel="${name}"]`);
    return {panel, height: 0, carriage: panel.querySelector('[data-carriage]'),
      cable: panel.querySelector('[data-cable]'), status: panel.querySelector('[role="status"]')};
  });
  let held = false, requested = false, frame = null, previous = null;
  const inputs = new Set();
  function render() {
    panels.forEach(({height, carriage, cable}) => {
      carriage.setAttribute('transform', `translate(0 ${-height * 80})`);
      cable.setAttribute('d', `M82.5 30V${135 - height * 80}`);
    });
    button.classList.toggle('is-held', held);
    button.setAttribute('aria-pressed', String(held));
  }
  function stopFrame() {
    if (frame !== null) cancelAnimationFrame(frame);
    frame = null; previous = null;
  }
  function tick(time) {
    frame = null;
    const step = previous === null ? 0 : Math.min(time - previous, 100) / 6500;
    previous = time;
    panels.forEach((p, index) => {
      if ((index === 0 ? requested : held) && p.height < 1) {
        p.height = Math.min(1, p.height + step);
        if (p.height === 1) p.status.textContent = 'At the top';
      }
    });
    render();
    if ((requested && panels[0].height < 1) || (held && panels[1].height < 1)) {
      frame = requestAnimationFrame(tick);
    } else previous = null;
  }
  function start() {
    if (held) return;
    held = true; requested = true;
    panels.forEach(p => { p.status.textContent = p.height >= 1 ? 'At the top' : 'Moving up'; });
    if (reduced.matches) {
      panels.forEach(p => { p.height = Math.max(p.height, 0.4); });
    } else if (frame === null) frame = requestAnimationFrame(tick);
    render();
  }
  function release() {
    if (!held) return;
    held = false;
    if (reduced.matches) panels[0].height = 1;
    panels[0].status.textContent = panels[0].height >= 1 ? 'At the top' : 'Still moving up';
    panels[1].status.textContent = panels[1].height >= 1 ? 'Holding at the top' : 'Stopped — holding here';
    render();
  }
  function press(source) { inputs.add(source); start(); }
  function lift(source) { inputs.delete(source); if (inputs.size === 0) release(); }
  function releaseAll() { inputs.clear(); release(); }
  button.addEventListener('pointerdown', event => {
    if (event.button !== 0) return;
    event.preventDefault(); button.focus(); button.setPointerCapture(event.pointerId); press('pointer');
  });
  ['pointerup', 'pointercancel', 'lostpointercapture'].forEach(type => button.addEventListener(type, () => lift('pointer')));
  button.addEventListener('keydown', event => {
    if (event.key === ' ' || event.key === 'Enter') { event.preventDefault(); press(event.key); }
  });
  button.addEventListener('keyup', event => {
    if (event.key === ' ' || event.key === 'Enter') { event.preventDefault(); lift(event.key); }
  });
  window.addEventListener('keydown', event => {
    if (event.key.toLowerCase() !== 'y' || event.repeat || event.isComposing
        || event.ctrlKey || event.metaKey || event.altKey
        || event.target?.closest?.('input, textarea, select, [contenteditable]:not([contenteditable="false"])')) return;
    event.preventDefault(); press('y');
  });
  window.addEventListener('keyup', event => {
    if (event.key.toLowerCase() === 'y') lift('y');
  });
  // Assistive-technology clicks have no preceding key/pointer events.
  button.addEventListener('click', event => {
    if (event.detail === 0 && !held) { start(); release(); }
  });
  button.addEventListener('blur', () => { lift(' '); lift('Enter'); });
  window.addEventListener('blur', releaseAll);
  reset.addEventListener('click', () => {
    stopFrame(); inputs.clear(); held = false; requested = false;
    panels.forEach(p => { p.height = 0; p.status.textContent = 'Ready'; }); render();
  });
  reduced.addEventListener('change', () => { stopFrame(); releaseAll(); });
  render();
});
