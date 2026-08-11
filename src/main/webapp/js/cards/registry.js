// cards/registry.js — card registration and mounting
//
// Cards call registerCard(definition) at module load time.
// main.js calls mountAllCards(gridEl, layoutConfig) once the DOM is ready.
// Display order and visibility are controlled by layout-config.js (not import order).

// Array of registered card definitions
// Cards may include explicit row/column positioning info which will be applied during mounting.
// Position precedence: layoutConfig.cards[cardId] > card.row/card.column > auto-flow
const _cards = [];

/**
 * Register a card definition. Called by each card module.
 * @param {{ id: string, size?: string, row?: number, column?: number, html: () => string, init: () => void, runAfterLogin?: boolean, run?: () => void | Promise<void> }} definition
 *   id            — feature slug, e.g. "connection-test"
 *   size          — optional grid span: "wide" | "tall" | "large" | "full" (default: normal)
 *   row           — optional explicit grid row (1-indexed); overridden by layoutConfig if present
 *   column        — optional explicit grid column (1-indexed); overridden by layoutConfig if present
 *   html          — function returning the card's inner HTML string
 *   init          — function that wires up event listeners (called after the HTML is in the DOM)
 *   runAfterLogin — optional flag to auto-run the card after authentication is ready
 *   run           — optional shared action callable by both lifecycle hooks and UI events
 */
export function registerCard(definition) {
  _cards.push(definition);
}

/**
 * Inject all registered card HTML into gridEl, then run each card's init().
 * Applies explicit positioning from layoutConfig if provided.
 * @param {HTMLElement} gridEl
 * @param {Object} layoutConfig - Layout configuration with card positions and visibility
 *   layoutConfig.cards[cardId] = { row, column, size }
 *   Only cards present in layoutConfig.cards are mounted and displayed.
 *   layoutConfig.cards determines both which cards are shown and where they are positioned.
 */
export function mountAllCards(gridEl, layoutConfig = {}) {
  for (const card of _cards) {
    // Check if card is in layout config — if not, skip it (don't mount)
    if (!layoutConfig.cards || !layoutConfig.cards[card.id]) {
      // Card is not in config: skip mounting (effectively hiding it)
      continue;
    }

    const wrapper = document.createElement('div');
    wrapper.innerHTML = card.html().trim();
    const cardEl = wrapper.firstElementChild;

    // Get position and size from layout config (card is guaranteed to be in config at this point)
    const configEntry = layoutConfig.cards[card.id];
    const position = {
      row: configEntry.row,
      column: configEntry.column,
    };

    // Apply explicit positioning and sizing via inline styles.
    // gridColumnStart/gridRowStart set the starting cell.
    // gridColumn/gridRow with span values set the span (how many cells).
    cardEl.style.gridRowStart = position.row;
    cardEl.style.gridColumnStart = position.column;

    // Apply size modifier from layoutConfig
    const size = configEntry.size || card.size || 'normal';
    if (size && size !== 'normal') {
      // Apply the span directly via inline styles so it works with explicit positioning
      // Size modifiers: wide=span 2, tall=span 2 rows, large=span 2×2, full=all columns
      if (size === 'wide') {
        cardEl.style.gridColumn = 'span 2';
      } else if (size === 'tall') {
        cardEl.style.gridRow = 'span 2';
      } else if (size === 'large') {
        cardEl.style.gridColumn = 'span 2';
        cardEl.style.gridRow = 'span 2';
      } else if (size === 'full') {
        cardEl.style.gridColumn = '1 / -1';
      }
      // Also set data-size for CSS styling (though span is now via inline styles)
      cardEl.dataset.size = size;
    }

    gridEl.appendChild(cardEl);
    card.init();
  }
}

export function runPostLoginCards() {
  for (const card of _cards) {
    if (card.runAfterLogin && card.run) {
      card.run();
    }
  }
}
