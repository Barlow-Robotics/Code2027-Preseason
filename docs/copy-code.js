/* Copy only the added code, preserving indentation and line breaks. */
document.querySelectorAll('[data-copy-code]').forEach((button) => {
  button.addEventListener('click', async () => {
    const toolbar = button.closest('.code-copy');
    const status = toolbar.querySelector('[role="status"]');
    const text = button.dataset.copyCode;
    toolbar.querySelector('.copy-manual')?.remove();
    status.textContent = '';
    button.disabled = true;
    let restoreFocus = false;
    try {
      if (!navigator.clipboard?.writeText) throw new Error('Clipboard unavailable');
      await navigator.clipboard.writeText(text);
      status.textContent = 'Copied!';
    } catch {
      // Local HTML files and browser settings may block the Clipboard API.
      const field = document.createElement('textarea');
      field.className = 'copy-manual';
      field.value = text;
      field.readOnly = true;
      field.setAttribute('aria-label', 'Added code ready to copy');
      toolbar.append(field);
      field.focus();
      field.select();
      let copied = false;
      try { copied = document.execCommand('copy'); } catch { /* Keep manual selection. */ }
      if (copied) {
        field.remove();
        restoreFocus = true;
        status.textContent = 'Copied!';
      } else {
        status.textContent = 'Press Ctrl+C (Mac: ⌘C) to copy the selected code below.';
      }
    } finally {
      button.disabled = false;
      if (restoreFocus) button.focus();
    }
  });
});

document.querySelectorAll('[data-copy-practice]').forEach((practice) => {
  const input = practice.querySelector('textarea');
  const status = practice.querySelector('[data-practice-status]');
  const expected = practice.dataset.expectedCode;
  const original = input.defaultValue;
  // Trailing spaces are harmless; leading spaces are the point of this exercise.
  const normalize = text => text.replace(/\r\n?/g, '\n').replace(/[ \t]+$/gm, '').replace(/\n$/, '');
  practice.querySelector('[data-practice-reset]').addEventListener('click', () => {
    input.value = original;
    status.textContent = '';
    input.focus();
    const position = original.indexOf('\n', original.indexOf('\n') + 1);
    input.setSelectionRange(position, position);
  });
  input.addEventListener('input', () => {
    const pasted = normalize(input.value);
    if (pasted === normalize(original)) {
      status.textContent = '';
    } else if (pasted === expected) {
      status.textContent = 'It worked! The new lines fit, and the existing code and indentation are intact.';
    } else if (pasted.split('\n').map(line => line.trim()).join('\n')
        === expected.split('\n').map(line => line.trim()).join('\n')) {
      status.textContent = 'The words match, but the spacing is different. Reset the practice code, then paste at the beginning of the blank line.';
    } else {
      status.textContent = 'Keep the existing code and add only the green lines on the blank line. Use Reset practice code to try again.';
    }
  });
});
