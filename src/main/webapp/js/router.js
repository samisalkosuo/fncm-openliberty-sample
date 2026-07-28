// router.js — view switching, app entry, logout
import { session } from './session.js';

export function showView(name) {
  document.querySelectorAll('.view').forEach(v => v.classList.remove('active'));
  document.getElementById('view-' + name).classList.add('active');
}

export function enterApp() {
  document.getElementById('user-name').textContent = session.username;
  document.getElementById('user-info').classList.remove('hidden');
  showView('app');
}

export function logout() {
  session.clear();
  document.getElementById('user-info').classList.add('hidden');
  document.getElementById('username').value = '';
  document.getElementById('password').value = '';
  showView('login');
}
