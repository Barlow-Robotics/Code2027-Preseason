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
