// eventBus.js — lightweight publish/subscribe event bus for inter-card communication
//
// Usage:
//   import { publish, subscribe, unsubscribe, TOPICS } from './eventBus.js';
//
//   // Subscribe (returns a convenience unsubscribe function):
//   const off = subscribe(TOPICS.DOCUMENT_SELECTED, ({ id, name }) => { ... });
//   off(); // unsubscribe when no longer needed
//
//   // Publish:
//   publish(TOPICS.DOCUMENT_SELECTED, { id: '123', name: 'Contract.pdf', className: 'Document' });
//
// Logging:
//   All bus activity is logged via console.debug under the [EventBus] prefix.
//   Enable "Verbose" in the browser DevTools console to see these messages.

// ── Topic registry ───────────────────────────────────────────────────────────
//
// All topic name strings live here. Import TOPICS rather than using raw strings
// so that typos are caught at module load time and all topics are discoverable
// in one place.
//
// Naming convention:
//   Key   : SCREAMING_SNAKE_CASE   (e.g. DOCUMENT_SELECTED)
//   Value : 'fncmopenlibertysample:<noun>:<verb>'   (e.g. 'fncmopenlibertysample:document:selected')

const TOPIC_PREFIX = 'fncmopenlibertysample';

export const TOPICS = {
  DOCUMENT_SELECTED: `${TOPIC_PREFIX}:document:selected`,
  FOLDER_SELECTED: `${TOPIC_PREFIX}:folder:selected`,
  DOCUMENT_ID: `${TOPIC_PREFIX}:document:id`,
  DOCUMENT_CLEARED: `${TOPIC_PREFIX}:document:cleared`,
};

// ── Internal state ───────────────────────────────────────────────────────────

/** @type {Map<string, Set<Function>>} */
const _subscribers = new Map();

// ── Public API ───────────────────────────────────────────────────────────────

/**
 * Subscribe to a topic.
 *
 * @param {string}   topic   — one of the TOPICS values
 * @param {Function} handler — called with (payload) on each publish
 * @returns {Function} unsubscribe — call this to remove the subscription
 */
export function subscribe(topic, handler) {
  if (!_subscribers.has(topic)) {
    _subscribers.set(topic, new Set());
  }
  _subscribers.get(topic).add(handler);
  console.debug(`[EventBus] subscribe  "${topic}" — ${_subscribers.get(topic).size} listener(s)`);

  return () => unsubscribe(topic, handler);
}

/**
 * Unsubscribe a previously registered handler from a topic.
 *
 * @param {string}   topic
 * @param {Function} handler — must be the same function reference passed to subscribe
 */
export function unsubscribe(topic, handler) {
  const handlers = _subscribers.get(topic);
  if (!handlers) return;
  handlers.delete(handler);
  console.debug(`[EventBus] unsubscribe "${topic}" — ${handlers.size} listener(s) remaining`);
}

/**
 * Publish a payload to all subscribers of a topic.
 * Handlers are called synchronously in subscription order.
 *
 * @param {string} topic   — one of the TOPICS values
 * @param {*}      payload — data passed to every subscriber handler
 */
export function publish(topic, payload) {
  console.debug(`[EventBus] publish    "${topic}"`, payload);
  const handlers = _subscribers.get(topic);
  if (!handlers || handlers.size === 0) return;
  for (const handler of handlers) {
    try {
      handler(payload);
    } catch (err) {
      console.error(`[EventBus] handler error on topic "${topic}":`, err);
    }
  }
}
