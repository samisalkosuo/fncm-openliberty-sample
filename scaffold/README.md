# scaffold/

Developer scaffolding support directory for `scaffold.sh`.

## Directory layout

```
scaffold/
  templates/
    css/
      default/          # built-in CSS theme (seeded from src/main/webapp/css/)
      <my-theme>/       # add a new directory here to create a custom CSS theme
    card/
      _template.js      # source template for new JavaScript cards
    java/
      Resource.java.tpl   # JAX-RS resource template
      Model.java.tpl      # Java record result model template
      Service.java.tpl    # ApplicationScoped service template
      Operation.java.tpl  # FileNetOperation implementation template
  backups/
    css-YYYYMMDD-HHMMSS/  # auto-created before each --css apply
```

## Adding a custom CSS theme

1. Create a new directory under `scaffold/templates/css/`, e.g. `scaffold/templates/css/dark/`
2. Add five CSS files with the same names as the default theme:
   - `app.css`
   - `tokens.css`
   - `layout.css`
   - `card.css`
   - `components.css`
3. Apply the theme: `./scaffold.sh --css dark`

## Usage

```bash
# List available CSS themes
./scaffold.sh --css list

# Apply a CSS theme (backs up current CSS first)
./scaffold.sh --css default

# Generate a new JavaScript card
./scaffold.sh --card my-feature

# Generate a Java vertical slice (Resource + Model + Service + Operation)
./scaffold.sh --java MyFeature

# Combine any number of commands in one call
./scaffold.sh --css my-theme --card my-feature --java MyFeature
```
