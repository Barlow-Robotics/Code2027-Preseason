(() => {
  let chosen = 'Elevator';
  try { chosen = localStorage.getItem('tuning-mechanism') || chosen; } catch {}
  if (!['Elevator','Arm'].includes(chosen)) chosen='Elevator';
  const select = value => {
    document.querySelectorAll('[data-mechanism]').forEach(el => el.hidden = el.dataset.mechanism !== value);
    document.querySelectorAll('input[name="tuning-mechanism"]').forEach(el => el.checked = el.value === value);
    try {localStorage.setItem('tuning-mechanism',value);} catch {}
  };
  document.querySelectorAll('input[name="tuning-mechanism"]').forEach(el => el.addEventListener('change',()=>select(el.value)));
  select(chosen);
})();
