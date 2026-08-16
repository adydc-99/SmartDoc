# DSH Wallpaper Plugin Design

## Objective

Build `dsh-wallpaper`, an independently installable Cordis plugin for the official DeepSeek Harness Web UI. The plugin gives users a persistent wallpaper library covering the entire Web interface and lets DeepSeek safely select an existing wallpaper or adjust its presentation through plugin-contributed tools.

The first release targets the official `deepseek-ai/deepseek-harness` developer preview and will document the compatible Harness version because its plugin APIs may change incompatibly.

## Scope

The first release supports:

- Local JPG, PNG, WebP, GIF, MP4, and WebM files.
- Remote HTTP and HTTPS image, GIF, and video URLs.
- A wallpaper library with previews, selection, deletion, and enable/disable controls.
- Cover, contain, stretch, center, and tile presentation modes where applicable.
- Opacity, brightness, blur, overlay color, video mute, and video playback-speed controls.
- One wallpaper configuration shared across the complete DSH Web UI, including chat, settings, and plugin pages.
- Persistent plugin-private files and settings.
- Model-callable tools for listing existing wallpapers, selecting one, and changing safe display settings.

The first release excludes an online wallpaper marketplace, automatic web discovery or downloading, schedules, playlists, and wallpaper changes driven by agent lifecycle state.

## Architecture

The plugin is one Cordis package with four isolated parts:

1. **Plugin manifest and bootstrap** declare the compatible DSH range, permissions, UI contribution, routes, storage, and model tools.
2. **Wallpaper service** owns validation, metadata, configuration, and plugin-private file operations. UI routes and model tools call this service rather than accessing storage directly.
3. **Wallpaper settings UI** manages the library and presentation controls.
4. **Global renderer** mounts once at the Web application root and renders behind all application content with pointer events disabled.

No separate MCP server is required. MCP is intended for exposing external tools and services to models; this feature already lives inside the DSH plugin host and can register its narrow tool surface directly.

## Data Flow

### Manual control

1. The user uploads a local file or registers a remote URL in the wallpaper settings page.
2. The plugin route validates the request and asks the wallpaper service to store metadata and, for uploads, the file.
3. The service atomically writes the updated configuration.
4. A plugin event notifies the global renderer, which switches wallpaper without reloading the page.

### Conversational control

1. The user asks DeepSeek to list wallpapers, select an existing wallpaper, or adjust display settings.
2. DeepSeek calls a plugin-contributed tool.
3. The tool validates its structured arguments and delegates to the wallpaper service.
4. The service updates configuration and emits the same renderer event used by manual control.

## UI Design

The plugin contributes a `Wallpaper` settings page containing:

- A responsive card library with a preview, name, source type, media type, active state, and delete action.
- An upload action and an `Add URL` action.
- Controls for enable/disable, fit mode, opacity, brightness, blur, overlay color and strength, and video-specific mute and speed.
- A reset action that restores presentation defaults without deleting wallpapers.

The renderer uses a fixed, viewport-sized layer below DSH content. Images and GIFs use an image element or CSS background according to fit mode; videos use a muted, looping, inline video element. Application content remains interactive because the wallpaper layer never receives pointer events. DSH surfaces receive a configurable translucent treatment so text remains readable.

Accessibility requirements:

- Every control has a label and keyboard support.
- Active state is not communicated by color alone.
- Reduced-motion preference pauses animated GIF/video presentation or replaces it with a static first-frame/fallback presentation when supported by the browser.
- The default overlay preserves readable contrast, and reset always returns to that safe default.

## Storage Model

The service stores uploaded media in the plugin-private data directory and stores configuration separately as versioned JSON. Each wallpaper record contains:

- Stable generated ID.
- User-visible name.
- Source type: `upload` or `url`.
- Stored relative path or remote URL.
- Validated media type.
- Creation timestamp.

Global presentation configuration contains the selected wallpaper ID, enabled state, fit mode, opacity, brightness, blur radius, overlay settings, mute state, and playback speed.

Writes use a temporary file followed by atomic replacement. Startup performs schema validation and falls back to disabled/default presentation if configuration is corrupt. Deleting the active wallpaper disables it before removing its file and record.

## Security and Permissions

- Local uploads are limited to 100 MB per file.
- Accepted extensions, declared MIME type, and file signature must agree.
- Uploaded filenames are never used as storage paths; the service generates IDs and fixed extensions.
- All resolved upload paths must remain inside the plugin-private media directory.
- Remote sources must use HTTP or HTTPS. They are loaded by the browser and are not downloaded by the server in version one.
- The manifest declares only the UI, private storage, local route, and model-tool permissions required by the feature.
- Model tools cannot upload media, add URLs, download files, or delete records.

Remote URLs can disclose the user's IP address and browser metadata to their host. The UI must warn about this before saving a remote URL.

## Model Tool Surface

The plugin registers two tools:

### `wallpaper_list`

Returns IDs, names, media types, source types, and active state. It does not return local absolute paths.

### `wallpaper_apply`

Accepts an existing wallpaper ID or the current selection plus an optional bounded display-settings patch. It may enable or disable the wallpaper and adjust fit, opacity, brightness, blur, overlay, mute, and playback speed. Unknown IDs, invalid values, and unsupported combinations return structured errors without changing configuration.

These tools are plugin-native; they are not an MCP server. A future MCP adapter may wrap the same wallpaper service if control from other harnesses becomes a requirement.

## Error Handling

- Invalid uploads remain unstored and return a localized validation error.
- Failed remote media and decode/playback failures display a non-blocking warning and temporarily fall back to the default background.
- A broken wallpaper never prevents DSH navigation or chat from rendering.
- Configuration write failures leave the previous valid configuration active.
- Tool failures return concise structured errors suitable for model recovery.
- Unsupported DSH versions fail plugin activation with a clear compatibility message.

## Testing

Unit tests cover schema defaults and migrations, parameter bounds, file signature validation, path containment, atomic configuration behavior, deletion rules, and model-tool authorization.

UI component tests cover library states, control updates, video-only controls, reset behavior, keyboard operation, and reduced-motion handling.

Integration tests cover upload-to-render, URL-to-render, image/GIF/video switching, persistence across restart, live synchronization after model-tool calls, corrupt configuration recovery, and media-load fallback.

A compatibility smoke test installs the packed plugin into the supported DSH version, launches `dsh web`, verifies plugin activation and the settings contribution, and confirms that uninstalling the plugin leaves the Harness Web UI functional.

## Distribution

The GitHub repository and package name are `dsh-wallpaper`. The repository uses the `dsh-plugin` GitHub topic and includes:

- English and Chinese README files.
- Installation, update, uninstall, permission, and privacy instructions.
- Screenshots or short media demonstrations.
- A compatibility table mapping plugin versions to DSH versions.
- A license and release notes.

The intended installation experience is the official DSH plugin command once the package manifest and registry conventions are confirmed against the pinned Harness version.

## Acceptance Criteria

The design is complete when a user can install the plugin without modifying the DeepSeek Harness repository, add any supported local or remote wallpaper, apply it across the entire Web UI, persist settings across restarts, and ask DeepSeek to select an existing wallpaper or safely adjust its display settings. Invalid or failed media must not break the underlying Harness interface.
