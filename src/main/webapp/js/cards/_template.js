// cards/_template.js — copy and rename for each new card
//
// Naming convention: use the feature slug as prefix, e.g. "my-feature"
//   <div class="card" id="card-my-feature">
//     <button id="my-feature-btn">
//     <div   id="my-feature-spinner">
//     <div   id="my-feature-result">
//
// Steps to add a new card:
//   1. Add a <div class="card" id="card-my-feature"> block in index.html
//   2. Copy this file → cards/myFeature.js, replace "my-feature" with your slug
//   3. Import and call init() from main.js
import { apiFetch, API } from '../api.js';
import { esc } from '../util.js';

export function init() {
  document.getElementById('my-feature-btn').addEventListener('click', async () => {
    const spinner   = document.getElementById('my-feature-spinner');
    const container = document.getElementById('my-feature-result');
    spinner.classList.remove('hidden');
    container.innerHTML = '';

    try {
      // 1. call endpoint
      const res = await apiFetch(API.myEndpoint);
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      // 2. render result
      const data = await res.text();
      container.innerHTML = `<pre>${esc(data)}</pre>`;
    } catch (err) {
      container.innerHTML = `<div class="alert alert-error">${esc(err.message)}</div>`;
    } finally {
      // 3. hide spinner
      spinner.classList.add('hidden');
    }
  });
}
