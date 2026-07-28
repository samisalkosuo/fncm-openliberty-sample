// util.js — shared DOM helpers
export function showAlert(el, msg) {
  el.textContent = msg;
  el.classList.remove('hidden');
}

export function esc(str) {
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}
