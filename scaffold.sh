#!/usr/bin/env bash
# scaffold.sh — developer artifact generator for fncm-openliberty-sample
#
# Usage:
#   ./scaffold.sh [--css <theme|list>] [--card <slug>] [--java <FeatureName>]
#   ./scaffold.sh --feature <slug>
#   ./scaffold.sh --remove-feature <slug>
#
# Multiple arguments can be combined in one call:
#   ./scaffold.sh --css my-theme --card my-feature --java MyFeature
#
# Run ./scaffold.sh --help for full usage.

set -euo pipefail

# ── Constants ─────────────────────────────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCAFFOLD_DIR="${SCRIPT_DIR}/scaffold"
TEMPLATES_DIR="${SCAFFOLD_DIR}/templates"
BACKUPS_DIR="${SCAFFOLD_DIR}/backups"
WEBAPP_CSS_DIR="${SCRIPT_DIR}/src/main/webapp/css"
CARDS_DIR="${SCRIPT_DIR}/src/main/webapp/js/cards"
MAIN_JS="${SCRIPT_DIR}/src/main/webapp/js/main.js"
LAYOUT_CONFIG="${SCRIPT_DIR}/src/main/webapp/js/layout-config.js"
JAVA_ROOT="${SCRIPT_DIR}/src/main/java/dev/fncm"

# ── Colour output helpers ──────────────────────────────────────────────────────
if [[ -t 1 ]]; then
  _RESET='\033[0m'; _BOLD='\033[1m'
  _BLUE='\033[0;34m'; _GREEN='\033[0;32m'
  _YELLOW='\033[0;33m'; _RED='\033[0;31m'
else
  _RESET=''; _BOLD=''; _BLUE=''; _GREEN=''; _YELLOW=''; _RED=''
fi

info()    { echo -e "${_BLUE}  →${_RESET} $*"; }
success() { echo -e "${_GREEN}  ✓${_RESET} $*"; }
warn()    { echo -e "${_YELLOW}  !${_RESET} $*"; }
error()   { echo -e "${_RED}  ✗ ERROR:${_RESET} $*" >&2; }
header()  { echo -e "\n${_BOLD}${_BLUE}── $* ──${_RESET}"; }

die() { error "$*"; exit 1; }

# ── Usage ─────────────────────────────────────────────────────────────────────
usage() {
  cat <<EOF

${_BOLD}scaffold.sh${_RESET} — generate project artifacts

${_BOLD}USAGE${_RESET}
  ./scaffold.sh [OPTIONS]

${_BOLD}OPTIONS${_RESET}
  --css list               List available CSS themes
  --css <theme>            Apply a CSS theme (backs up current CSS first)
  --card <slug>            Generate a new JavaScript card (kebab-case slug)
  --java <FeatureName>     Generate a Java vertical slice (PascalCase name)
  --feature <slug>         Generate a full feature: Java slice + JS card + API entry (kebab-case slug)
  --remove-feature <slug>  Remove all files created by --feature and undo main.js / api.js changes
  --help                   Show this help and exit

${_BOLD}EXAMPLES${_RESET}
  ./scaffold.sh --css list
  ./scaffold.sh --css default
  ./scaffold.sh --card my-feature
  ./scaffold.sh --java MyFeature
  ./scaffold.sh --card my-feature --java MyFeature --css my-theme
  ./scaffold.sh --feature my-feature
  ./scaffold.sh --remove-feature my-feature

${_BOLD}NOTES${_RESET}
  • CSS themes live in scaffold/templates/css/<theme>/
  • Card templates live in scaffold/templates/card/
  • Java templates live in scaffold/templates/java/
  • CSS backups are written to scaffold/backups/css-TIMESTAMP/
  • --feature derives PascalCase (Java) and camelCase (JS) from the kebab-case slug
  • --remove-feature is the exact inverse of --feature; it only removes what --feature created
  • See scaffold/README.md for full details.

EOF
}

# ── Sub-Task 3: CSS theme command ─────────────────────────────────────────────
cmd_css() {
  local theme="$1"
  local themes_dir="${TEMPLATES_DIR}/css"

  # --css list
  if [[ "$theme" == "list" ]]; then
    header "Available CSS themes"
    local found=0
    for d in "${themes_dir}"/*/; do
      [[ -d "$d" ]] || continue
      echo "  $(basename "$d")"
      found=1
    done
    [[ $found -eq 1 ]] || warn "No themes found in ${themes_dir}/"
    return 0
  fi

  # --css <theme>
  local theme_dir="${themes_dir}/${theme}"
  [[ -d "$theme_dir" ]] || die "CSS theme '${theme}' not found. Run --css list to see available themes."

  header "Applying CSS theme: ${theme}"

  # Backup current CSS
  local ts
  ts="$(date +%Y%m%d-%H%M%S)"
  local backup_dir="${BACKUPS_DIR}/css-${ts}"
  mkdir -p "$backup_dir"
  info "Backing up current CSS to ${backup_dir}/"
  for f in "${WEBAPP_CSS_DIR}"/*.css; do
    [[ -f "$f" ]] || continue
    cp "$f" "$backup_dir/"
    info "  backed up $(basename "$f")"
  done
  success "Backup complete → ${backup_dir}/"

  # Apply theme files
  info "Copying theme files to ${WEBAPP_CSS_DIR}/"
  for f in "${theme_dir}"/*.css; do
    [[ -f "$f" ]] || continue
    cp "$f" "${WEBAPP_CSS_DIR}/$(basename "$f")"
    info "  applied $(basename "$f")"
  done
  success "CSS theme '${theme}' applied."
  echo ""
  echo "  Backup: ${backup_dir}/"
  echo "  Applied from: ${theme_dir}/"
}

# ── Sub-Task 4: Card generation command ───────────────────────────────────────
cmd_card() {
   local slug="$1"

   # Validate slug is kebab-case (letters, digits, hyphens)
   if [[ ! "$slug" =~ ^[a-z][a-z0-9-]*$ ]]; then
     die "Card slug must be kebab-case (lowercase letters, digits, hyphens). Got: '${slug}'"
   fi

   header "Generating card: ${slug}"

   # slug → camelCase filename  (my-feature → myFeature)
   local camel
   camel="$(echo "$slug" | sed -E 's/-([a-z])/\U\1/g')"

   # slug → Title Case heading  (my-feature → My Feature)
   local title
   title="$(echo "$slug" | awk -F'-' '{for(i=1;i<=NF;i++) $i=toupper(substr($i,1,1)) substr($i,2); print}' OFS=' ')"

   local out_file="${CARDS_DIR}/${camel}.js"
   [[ ! -f "$out_file" ]] || die "File already exists: ${out_file}. Aborting to avoid overwrite."

   local template="${TEMPLATES_DIR}/card/_template.js"
   [[ -f "$template" ]] || die "Card template not found: ${template}"

   # Replace template placeholders
   local content
   content="$(cat "$template")"
   content="${content//my-feature/$slug}"
   content="${content//My Feature/$title}"

   printf '%s\n' "$content" > "$out_file"
   success "Created card: ${out_file}"

   # Insert import into main.js before the marker comment
   local import_line="import './cards/${camel}.js';"
   local marker="// To add a new card:"

   if grep -qF "$import_line" "$MAIN_JS"; then
     warn "Import already present in main.js — skipping: ${import_line}"
   else
     # Use sed to insert the import line before the marker line
     sed -i.bak "s|${marker}|${import_line}\n${marker}|" "$MAIN_JS"
     rm -f "${MAIN_JS}.bak"
     success "Inserted import into main.js: ${import_line}"
   fi

   # Add card entry to layout-config.js (find highest row, add to end)
   # Card IDs in config are usually kebab-case or camelCase, matching the card definition's 'id' property
   # We'll use the slug as the card ID (developer can adjust if their card.id is different)
   if grep -qF "'${slug}'" "$LAYOUT_CONFIG"; then
     warn "Card '${slug}' already present in layout-config.js — skipping."
   else
     # Build the card entry with proper line breaks
     # We use a temp file and sed to insert multi-line content BEFORE the closing },
     local temp_entry=$(mktemp)
     cat > "$temp_entry" <<EOF
   '${slug}': {
     row: 99,  // FIXME: Update row/column to position this card in the grid
     column: 1,
     size: 'normal',
   },
EOF
     # Insert the temp file before the closing }, using sed with the 'e' flag to execute insertion
     # The -i'' syntax works on both GNU and BSD sed (macOS)
     sed -i.bak "/^  },$/e cat ${temp_entry}" "$LAYOUT_CONFIG"
     rm -f "$temp_entry" "${LAYOUT_CONFIG}.bak"
     success "Added card to layout-config.js: '${slug}' (row: 99, FIXME: adjust position)"
   fi

   echo ""
   echo "  New file      : ${out_file}"
   echo "  main.js       : added → ${import_line}"
   echo "  layout-config : added → '${slug}' (row: 99, needs manual positioning)"
   echo ""
   echo "  Next steps:"
   echo "    1. Edit ${out_file} — replace the placeholder API call / HTML"
   echo "    2. Add the REST endpoint to src/main/webapp/js/api.js if needed"
   echo "    3. Edit layout-config.js and set row/column for '${slug}' (currently row: 99 is a placeholder)"
}

# ── Full-feature command: Java slice + JS card + API entry ────────────────────
cmd_feature() {
  local slug="$1"

  # Validate slug is kebab-case (letters, digits, hyphens)
  if [[ ! "$slug" =~ ^[a-z][a-z0-9-]*$ ]]; then
    die "Feature slug must be kebab-case (lowercase letters, digits, hyphens). Got: '${slug}'"
  fi

  # ── Derive naming variants ─────────────────────────────────────────────
  # camelCase  (my-feature → myFeature)
  local camel
  camel="$(echo "$slug" | sed -E 's/-([a-z])/\U\1/g')"

  # PascalCase (my-feature → MyFeature)
  local pascal
  pascal="$(echo "$camel" | sed -E 's/^([a-z])/\U\1/')"

  # Title Case (my-feature → My Feature)
  local title
  title="$(echo "$slug" | awk -F'-' '{for(i=1;i<=NF;i++) $i=toupper(substr($i,1,1)) substr($i,2); print}' OFS=' ')"

  # lowercase path segment (MyFeature → myfeature)
  local path_seg
  path_seg="$(echo "$pascal" | tr '[:upper:]' '[:lower:]')"

  header "Generating full feature: ${slug} (Java: ${pascal}, JS: ${camel}, path: /api/${path_seg})"

  # ── 1. Java vertical slice ─────────────────────────────────────────────
  local f_resource="${JAVA_ROOT}/resource/${pascal}Resource.java"
  local f_model="${JAVA_ROOT}/model/${pascal}Result.java"
  local f_service="${JAVA_ROOT}/service/${pascal}Service.java"
  local f_operation="${JAVA_ROOT}/service/javaapi/service/${pascal}Operation.java"

  for f in "$f_resource" "$f_model" "$f_service" "$f_operation"; do
    [[ ! -f "$f" ]] || die "File already exists: ${f}. Aborting to avoid overwrite."
  done

  local tpl_dir="${TEMPLATES_DIR}/java"

  _apply_tpl_feature() {
    local tpl_file="$1"
    local out_file="$2"
    [[ -f "$tpl_file" ]] || die "Java template not found: ${tpl_file}"
    local content
    content="$(cat "$tpl_file")"
    content="${content//__NAME__/$pascal}"
    content="${content//__PATH__/$path_seg}"
    printf '%s\n' "$content" > "$out_file"
    success "Created: ${out_file}"
  }

  _apply_tpl_feature "${tpl_dir}/Resource.java.tpl"  "$f_resource"
  _apply_tpl_feature "${tpl_dir}/Model.java.tpl"     "$f_model"
  _apply_tpl_feature "${tpl_dir}/Service.java.tpl"   "$f_service"
  _apply_tpl_feature "${tpl_dir}/Operation.java.tpl" "$f_operation"

  # ── 2. JS card ────────────────────────────────────────────────────────
  local out_file="${CARDS_DIR}/${camel}.js"
  [[ ! -f "$out_file" ]] || die "File already exists: ${out_file}. Aborting to avoid overwrite."

  local card_template="${TEMPLATES_DIR}/card/_template.js"
  [[ -f "$card_template" ]] || die "Card template not found: ${card_template}"

  local card_content
  card_content="$(cat "$card_template")"
  card_content="${card_content//my-feature/$slug}"
  card_content="${card_content//My Feature/$title}"
  # Wire the card to the generated API key instead of the placeholder
  card_content="${card_content//API.myEndpoint/API.${camel}}"

  printf '%s\n' "$card_content" > "$out_file"
  success "Created card: ${out_file}"

  # Insert import into main.js before the marker comment
  local import_line="import './cards/${camel}.js';"
  local marker="// ── Add new card imports above this line ──"

  if grep -qF "$import_line" "$MAIN_JS"; then
    warn "Import already present in main.js — skipping: ${import_line}"
  else
    sed -i.bak "s|${marker}|${import_line}\n${marker}|" "$MAIN_JS"
    rm -f "${MAIN_JS}.bak"
    success "Inserted import into main.js: ${import_line}"
  fi

  # ── 3. API entry in api.js ────────────────────────────────────────────
  local API_JS="${SCRIPT_DIR}/src/main/webapp/js/api.js"
  local api_key="${camel}"
  local api_path="/api/${path_seg}"
  local api_entry="  ${api_key}:$(printf '%*s' $((36 - ${#api_key})) '')\'${api_path}\',"

  if grep -qF "'${api_path}'" "$API_JS"; then
    warn "API path '${api_path}' already present in api.js — skipping."
  else
    # Insert the new entry before the first closing }; (the API object)
    sed -i.bak "0,/^};$/{s|^};$|${api_entry}\n};|}" "$API_JS"
    rm -f "${API_JS}.bak"
    success "Added API entry to api.js: ${api_key}: '${api_path}'"
  fi

  # ── 4. Card entry in layout-config.js ──────────────────────────────────
  # Use kebab-case slug as card ID (matching the card's id in registerCard)
  # Most cards in layout-config use kebab-case IDs (e.g., 'connection-test', 'list-folders')
  if grep -qF "'${slug}'" "$LAYOUT_CONFIG"; then
    warn "Card '${slug}' already present in layout-config.js — skipping."
  else
    # Build the card entry with proper line breaks
    # We use a temp file to generate the multi-line entry, then use sed to insert it BEFORE the closing },
    local temp_entry=$(mktemp)
    cat > "$temp_entry" <<EOF
    '${slug}': {
      row: 99,  // FIXME: Update row/column to position this card in the grid
      column: 1,
      size: 'normal',
    },
EOF
    # Insert the temp file before the closing }, using sed with the 'e' flag
    # The -i'' syntax works on both GNU and BSD sed (macOS)
    sed -i.bak "/^  },$/e cat ${temp_entry}" "$LAYOUT_CONFIG"
    rm -f "$temp_entry" "${LAYOUT_CONFIG}.bak"
    success "Added card to layout-config.js: '${slug}' (row: 99, FIXME: adjust position)"
  fi

  # ── Summary ───────────────────────────────────────────────────────────
  echo ""
  echo "  Java files:"
  echo "    ${f_resource}"
  echo "    ${f_model}"
  echo "    ${f_service}"
  echo "    ${f_operation}"
  echo ""
  echo "  JS card       : ${out_file}"
  echo "  main.js       : added → ${import_line}"
  echo "  api.js        : added → ${api_key}: '${api_path}'"
  echo "  layout-config : added → '${slug}' (row: 99, needs manual positioning)"
  echo ""
  echo "  Next steps:"
  echo "    1. Edit ${pascal}Result.java — add record components for your data"
  echo "    2. Edit ${pascal}Operation.java — implement the FileNet logic"
  echo "    3. Edit ${out_file} — customise the card HTML / rendering"
  echo "    4. Edit layout-config.js and set row/column for '${slug}' (currently row: 99 is a placeholder)"
}

# ── Sub-Task 5: Java vertical-slice command ────────────────────────────────────
cmd_java() {
  local name="$1"

  # Validate PascalCase
  if [[ ! "$name" =~ ^[A-Z][A-Za-z0-9]*$ ]]; then
    die "Java name must be PascalCase (start with uppercase letter). Got: '${name}'"
  fi

  header "Generating Java vertical slice: ${name}"

  # Derive lowercase @Path segment  (MyFeature → myfeature)
  local path_seg
  path_seg="$(echo "$name" | tr '[:upper:]' '[:lower:]')"

  # Output file paths
  local f_resource="${JAVA_ROOT}/resource/${name}Resource.java"
  local f_model="${JAVA_ROOT}/model/${name}Result.java"
  local f_service="${JAVA_ROOT}/service/${name}Service.java"
  local f_operation="${JAVA_ROOT}/service/javaapi/service/${name}Operation.java"

  # Guard: abort if any target file already exists
  for f in "$f_resource" "$f_model" "$f_service" "$f_operation"; do
    [[ ! -f "$f" ]] || die "File already exists: ${f}. Aborting to avoid overwrite."
  done

  local tpl_dir="${TEMPLATES_DIR}/java"

  # Helper: apply placeholder substitution and write a file
  _apply_tpl() {
    local tpl_file="$1"
    local out_file="$2"
    [[ -f "$tpl_file" ]] || die "Java template not found: ${tpl_file}"
    local content
    content="$(cat "$tpl_file")"
    content="${content//__NAME__/$name}"
    content="${content//__PATH__/$path_seg}"
    printf '%s\n' "$content" > "$out_file"
    success "Created: ${out_file}"
  }

  _apply_tpl "${tpl_dir}/Resource.java.tpl"  "$f_resource"
  _apply_tpl "${tpl_dir}/Model.java.tpl"     "$f_model"
  _apply_tpl "${tpl_dir}/Service.java.tpl"   "$f_service"
  _apply_tpl "${tpl_dir}/Operation.java.tpl" "$f_operation"

  echo ""
  echo "  Files created:"
  echo "    ${f_resource}"
  echo "    ${f_model}"
  echo "    ${f_service}"
  echo "    ${f_operation}"
  echo ""
  echo "  Next steps:"
  echo "    1. Edit ${name}Result.java — add record components for your data"
  echo "    2. Edit ${name}Operation.java — implement the FileNet logic"
  echo "    3. Add the API endpoint to src/main/webapp/js/api.js"
  echo "    4. Run ./scaffold.sh --card ${path_seg} to generate the matching UI card"
}

# ── Remove-feature command: undo everything --feature created ─────────────────
cmd_remove_feature() {
  local slug="$1"

  # Validate slug is kebab-case
  if [[ ! "$slug" =~ ^[a-z][a-z0-9-]*$ ]]; then
    die "Feature slug must be kebab-case (lowercase letters, digits, hyphens). Got: '${slug}'"
  fi

  # ── Derive the same naming variants as cmd_feature ────────────────────
  local camel
  camel="$(echo "$slug" | sed -E 's/-([a-z])/\U\1/g')"
  local pascal
  pascal="$(echo "$camel" | sed -E 's/^([a-z])/\U\1/')"
  local path_seg
  path_seg="$(echo "$pascal" | tr '[:upper:]' '[:lower:]')"

  header "Removing feature: ${slug} (Java: ${pascal}, JS: ${camel}, path: /api/${path_seg})"

  local API_JS="${SCRIPT_DIR}/src/main/webapp/js/api.js"
  local removed_any=0

  # ── 1. Delete Java files ───────────────────────────────────────────────
  local f_resource="${JAVA_ROOT}/resource/${pascal}Resource.java"
  local f_model="${JAVA_ROOT}/model/${pascal}Result.java"
  local f_service="${JAVA_ROOT}/service/${pascal}Service.java"
  local f_operation="${JAVA_ROOT}/service/javaapi/service/${pascal}Operation.java"

  for f in "$f_resource" "$f_model" "$f_service" "$f_operation"; do
    if [[ -f "$f" ]]; then
      rm "$f"
      success "Deleted: ${f}"
      removed_any=1
    else
      warn "Not found (skipping): ${f}"
    fi
  done

  # ── 2. Delete JS card file ─────────────────────────────────────────────
  local card_file="${CARDS_DIR}/${camel}.js"
  if [[ -f "$card_file" ]]; then
    rm "$card_file"
    success "Deleted card: ${card_file}"
    removed_any=1
  else
    warn "Not found (skipping): ${card_file}"
  fi

  # ── 3. Remove import line from main.js ────────────────────────────────
  local import_line="import './cards/${camel}.js';"
  if grep -qF "$import_line" "$MAIN_JS"; then
    sed -i.bak "\|${import_line}|d" "$MAIN_JS"
    rm -f "${MAIN_JS}.bak"
    success "Removed import from main.js: ${import_line}"
    removed_any=1
  else
    warn "Import not found in main.js (skipping): ${import_line}"
  fi

  # ── 4. Remove API entry from api.js ───────────────────────────────────
  local api_path="/api/${path_seg}"
  if grep -qF "'${api_path}'" "$API_JS"; then
    # Delete the entire line containing this path value
    sed -i.bak "\|'${api_path}'|d" "$API_JS"
    rm -f "${API_JS}.bak"
    success "Removed API entry from api.js: ${camel}: '${api_path}'"
    removed_any=1
  else
    warn "API path '${api_path}' not found in api.js (skipping)."
  fi

  # ── 5. Remove card entry from layout-config.js ─────────────────────────
  # Remove the entire card entry for this card from layoutConfig.cards
  # Use kebab-case slug to match the card ID in layout-config
  if grep -qF "'${slug}'" "$LAYOUT_CONFIG"; then
    # Use sed to remove the multi-line entry
    # Find the line with '${slug}': and delete from there until the closing },
    # Using \s* to handle variable indentation
    sed -i.bak "/^[[:space:]]*'${slug}':/,/^[[:space:]]*},/d" "$LAYOUT_CONFIG"
    rm -f "${LAYOUT_CONFIG}.bak"
    success "Removed card entry from layout-config.js: '${slug}'"
    removed_any=1
  else
    warn "Card '${slug}' not found in layout-config.js (skipping)."
  fi

  # ── Summary ───────────────────────────────────────────────────────────
  echo ""
  if [[ $removed_any -eq 1 ]]; then
    success "Feature '${slug}' removed."
  else
    warn "Nothing was removed — was '${slug}' ever created with --feature?"
  fi
}

# ── Argument parsing & dispatch ────────────────────────────────────────────────
main() {
  if [[ $# -eq 0 ]]; then
    usage
    exit 0
  fi

  # Collect all options first, then dispatch
  local OPT_CSS=""
  local OPT_CARD=""
  local OPT_JAVA=""
  local OPT_FEATURE=""
  local OPT_REMOVE_FEATURE=""

  while [[ $# -gt 0 ]]; do
    case "$1" in
      --help|-h)
        usage; exit 0 ;;
      --css)
        [[ $# -ge 2 ]] || die "--css requires a value (e.g. --css default or --css list)"
        OPT_CSS="$2"; shift 2 ;;
      --card)
        [[ $# -ge 2 ]] || die "--card requires a value (e.g. --card my-feature)"
        OPT_CARD="$2"; shift 2 ;;
      --java)
        [[ $# -ge 2 ]] || die "--java requires a value (e.g. --java MyFeature)"
        OPT_JAVA="$2"; shift 2 ;;
      --feature)
        [[ $# -ge 2 ]] || die "--feature requires a value (e.g. --feature my-feature)"
        OPT_FEATURE="$2"; shift 2 ;;
      --remove-feature)
        [[ $# -ge 2 ]] || die "--remove-feature requires a value (e.g. --remove-feature my-feature)"
        OPT_REMOVE_FEATURE="$2"; shift 2 ;;
      *)
        die "Unknown argument: '$1'. Run ./scaffold.sh --help for usage." ;;
    esac
  done

  # Dispatch each requested command
  [[ -z "$OPT_CSS"            ]] || cmd_css            "$OPT_CSS"
  [[ -z "$OPT_CARD"           ]] || cmd_card           "$OPT_CARD"
  [[ -z "$OPT_JAVA"           ]] || cmd_java           "$OPT_JAVA"
  [[ -z "$OPT_FEATURE"        ]] || cmd_feature        "$OPT_FEATURE"
  [[ -z "$OPT_REMOVE_FEATURE" ]] || cmd_remove_feature "$OPT_REMOVE_FEATURE"

  echo ""
  success "scaffold.sh done."
}

main "$@"
