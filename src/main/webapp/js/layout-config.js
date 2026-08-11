// layout-config.js — Centralized card layout configuration
//
// This file defines:
//   1. Which cards are visible/hidden (only cards in layoutConfig.cards are shown)
//   2. The grid structure and explicit positioning for all cards
//
// Developers can modify this file to change layout without editing main.js or card definitions.
//
// Key Concepts:
//   - gridRows: Total number of rows in the grid (use 'auto' for unlimited auto-flow)
//   - gridColumns: Number of columns (overrides CSS token if needed)
//   - cards: Object mapping card IDs to position and size info
//          ONLY cards listed here will be displayed. Comment/remove a card entry to hide it.
//
// Position Numbering (1-indexed):
//   - row: 1-based row number where card starts
//   - column: 1-based column number where card starts
//   - size: 'normal' (default), 'wide', 'tall', 'large', 'full'
//
// Visibility Control:
//   - Cards in layoutConfig.cards are displayed
//   - Cards NOT in layoutConfig.cards are HIDDEN (not mounted in the UI)
//   - To hide a card: comment it out or remove it from the cards object
//   - To show a card: add it to the cards object with row/column/size properties
//
// Example: To move a card without changing import order, just update its row/column here.
// Example: To hide a card, comment it out (/* ... */) or delete its entry.
// Example: To create a gap (placeholder), simply don't assign a card to that cell range.

export const layoutConfig = {
  // Grid dimensions
  gridRows: 'auto',      // 'auto' = unlimited (CSS Grid auto-flow); or set a number like 5
  gridColumns: 3,        // Override CSS variable --grid-columns if needed (default: 3)

  // Card positions: maps card ID → { row, column, size }
  // NOTE: Card IDs must match exactly the 'id' property in each card's registerCard() call
  // sizes:'normal' | 'wide' | 'tall' | 'large' | 'full'  
  cards: {
    'connection-test': {
      row: 1,
      column: 1,
      size: 'normal',
    },
    'addbuildinginspectiondocs': {
      row: 1,
      column: 2,
      size: 'normal',
    },
    'filebuildinginspectiondocs': {
      row: 1,
      column: 3,
      size: 'normal',
    },
    // row 2
    'folder-tree': {
      row: 2,
      column: 1,
    },
    'documents': {
      row: 2,
      column: 2,
      size: 'normal',
    },
    'document-details': {
      row: 2,
      column: 3,
      size: 'normal',
    },
    // row 3
    'graphql': {
      row: 3,
      column: 1,
      size: 'wide',
    },
    'list-document-classes': {
      row: 3,
      column: 3,
      size: 'normal',
    },
    // row 4
    'create-document': {
      row: 4,
      column: 1,
      size: 'normal',
    },
    'user-groups': {
      row: 4,
      column: 2,
      size: 'normal',
    },
    'list-folders': {
      row: 4,
      column: 3,
      size: 'normal',
    },
    // row 4
    'list-documents-in-folder': {
      row: 5,
      column: 1,
      size: 'wide',  // 2 cols × 1 row
    },
    'folder-details': {
      row: 5,
      column: 3,
      size: 'normal',
    },
  },
};

// ─────────────────────────────────────────────────────────────────────────────
// HOW TO MODIFY THE LAYOUT
// ─────────────────────────────────────────────────────────────────────────────
//
// 1. HIDE A CARD (don't show in UI):
//    - Comment out or delete the card entry from layoutConfig.cards
//    - Example: Comment out lines for 'document-details', 'list-folders', etc.
//    - The card will still load in memory but won't be mounted/displayed
//
// 2. SHOW A CARD (make visible again):
//    - Uncomment or re-add the card entry to layoutConfig.cards
//    - Example: Uncomment the 'document-details' entry to make it visible
//
// 3. MOVE A CARD:
//    - Update the row/column values in the cards map
//    - No need to change import order in main.js
//    - Example: move 'documents' from (row 2, column 3) to (row 1, column 3)
//
// 4. CHANGE CARD SIZE:
//    - Update the size property: 'normal', 'wide', 'tall', 'large', or 'full'
//    - Example: 'documents': { row: 1, column: 3, size: 'wide' }
//
// 5. ADD A NEW CARD:
//    - Create your card in src/main/webapp/js/cards/myFeature.js
//    - Import it in src/main/webapp/js/main.js (import order no longer controls display)
//    - Add entry to layoutConfig.cards with its ID and desired row/column:
//      'my-feature': { row: 2, column: 1, size: 'normal' }
//
// 6. CREATE INTENTIONAL GAPS (PLACEHOLDERS):
//    - Simply don't assign any card to a row/column range
//    - The grid will leave those cells empty
//    - Useful for reserving space for future features
//    - Example: Rows 1-3 fully populated, row 4 left empty, row 5 starts with content
//
// 7. CHANGE GRID DIMENSIONS:
//    - Modify gridColumns to switch between 2, 3, 4 columns (or other values)
//    - Modify gridRows to set fixed height or 'auto' for unlimited
//    - Reassign card positions to fit the new grid shape
//
// ─────────────────────────────────────────────────────────────────────────────
// CURRENT LAYOUT VISUALIZATION (visible cards, 3 columns)
// ─────────────────────────────────────────────────────────────────────────────
//
// Row 1: [connection-test]           [addbuilding...]    [filebuilding...]
//
// Row 2: [folder-tree]               [documents]         [document-details]
//
// Row 3: [         graphql (wide: 2 cols)          ]     (empty)
//
// Hidden cards (commented out in this config):
//   - list-folders, list-document-classes, user-groups, list-documents-in-folder
//   - folder-details, create-document
//
// To show/hide cards: comment/uncomment entries above, don't change import order in main.js
// ─────────────────────────────────────────────────────────────────────────────
