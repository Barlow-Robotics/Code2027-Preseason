
const mechanismInputs = document.querySelectorAll('input[name="mechanism"]');
function selectMechanism(mechanism) {
  document.querySelectorAll('[data-track]').forEach(element => {
    element.hidden = element.dataset.track !== mechanism;
  });
  document.querySelectorAll('[data-elevator][data-arm]').forEach(element => {
    element.textContent = element.dataset[mechanism];
  });
  const status = document.getElementById('selection-status');
  if (status) status.textContent =
    `Showing the ${mechanism === 'elevator' ? 'Elevator' : 'Arm'} instructions below.`;
  document.querySelectorAll('[data-lesson-link]').forEach(link => {
    const url = new URL(link.getAttribute('href'), window.location.href);
    url.searchParams.set('mechanism', mechanism);
    link.setAttribute('href', url.pathname.split('/').pop() + url.search + url.hash);
  });
}
mechanismInputs.forEach(input => input.addEventListener('change', () => selectMechanism(input.value)));
const requestedMechanism = new URLSearchParams(window.location.search).get('mechanism');
const initialMechanism = requestedMechanism === 'arm' ? 'arm' : 'elevator';
mechanismInputs.forEach(input => { input.checked = input.value === initialMechanism; });
selectMechanism(initialMechanism);

// Each illustration uses one shared amount of motor rotation for all its moving parts.
const gearDemos = [];
const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
document.querySelectorAll('.gear-demo').forEach(figure => {
  const arm = figure.dataset.mechanism === 'arm';
  const ratio = arm ? 80 : 12;
  const maxMotorTurns = arm ? 20 : 12;
  const slider = figure.querySelector('[data-part="slider"]');
  const play = figure.querySelector('[data-action="play"]');
  const part = name => figure.querySelector(`[data-part="${name}"]`);
  const value = name => figure.querySelector(`[data-value="${name}"]`);
  let progress = 0, frame = null, previousTime = null;
  function render() {
    const motorTurns = progress * maxMotorTurns;
    const outputTurns = motorTurns / ratio;
    const height = outputTurns * 2 * Math.PI * 0.025;
    // Every meshing pair reverses direction. Gears on one shaft share one angle.
    const shaftTurns = arm
      ? [motorTurns, -motorTurns / 4, motorTurns / 16, -motorTurns / 80]
      : [motorTurns, -motorTurns / 4, motorTurns / 12];
    figure.querySelectorAll('[data-shaft]').forEach(gear => {
      gear.setAttribute('transform', `rotate(${shaftTurns[Number(gear.dataset.shaft)] * 360})`);
    });
    figure.querySelectorAll('[data-turns]').forEach(counter => {
      counter.textContent = Math.abs(shaftTurns[Number(counter.dataset.turns)]).toFixed(2);
    });
    part('output').setAttribute('transform', `rotate(${shaftTurns[shaftTurns.length - 1] * 360})`);
    if (!arm) {
      const lift = height * 500; // 12.5 px drum radius represents 0.025 meters.
      part('carriage').setAttribute('transform', `translate(0 ${-lift})`);
      part('cable').setAttribute('d', `M82.5 30V${135 - lift}`);
      part('height-line').setAttribute('d', `M152 135V${135 - lift}`);
    }
    value('motor').textContent = motorTurns.toFixed(2);
    value('output').textContent = outputTurns.toFixed(3);
    value('position').textContent = arm ? (outputTurns * 360).toFixed(1) : height.toFixed(3);
    slider.value = Math.round(progress * 1000);
    slider.setAttribute('aria-valuetext', `${motorTurns.toFixed(2)} motor turns; ${arm ? (outputTurns * 360).toFixed(1) + ' degrees' : height.toFixed(3) + ' meters'}`);
  }
  function pause() {
    if (frame !== null) cancelAnimationFrame(frame);
    frame = null; previousTime = null; play.textContent = reducedMotion.matches ? 'Advance 10%' : 'Play';
  }
  function tick(time) {
    if (previousTime !== null) progress = Math.min(1, progress + Math.min(time - previousTime, 100) / 16000);
    previousTime = time;
    render();
    if (progress >= 1) { pause(); return; }
    frame = requestAnimationFrame(tick);
  }
  play.addEventListener('click', () => {
    if (frame !== null) { pause(); return; }
    if (progress >= 1) progress = 0;
    // Reduced-motion users advance in deliberate steps, without automatic rotation.
    if (reducedMotion.matches) { progress = Math.min(1, progress + 0.1); render(); return; }
    play.textContent = 'Pause';
    frame = requestAnimationFrame(tick);
  });
  figure.querySelector('[data-action="reset"]').addEventListener('click', () => { pause(); progress = 0; render(); });
  slider.addEventListener('input', () => { pause(); progress = Number(slider.value) / 1000; render(); });
  function labelControls() { pause(); play.textContent = reducedMotion.matches ? 'Advance 10%' : 'Play'; }
  reducedMotion.addEventListener('change', labelControls);
  if ('IntersectionObserver' in window) {
    new IntersectionObserver(entries => { if (!entries[0].isIntersecting) pause(); }).observe(figure);
  }
  gearDemos.push({ pause });
  labelControls(); render();
});
mechanismInputs.forEach(input => input.addEventListener('change', () => gearDemos.forEach(demo => demo.pause())));
document.addEventListener('visibilitychange', () => { if (document.hidden) gearDemos.forEach(demo => demo.pause()); });
