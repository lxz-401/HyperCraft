# Style Features Roadmap - HyperCraft

## Overview

Current state: 86 SystemUI hooks across 6 categories
- Control Center: 26
- Status Bar: 25
- Lock Screen: 14
- Plugin: 13
- Other: 7
- Navigation: 1

Recent work: Volume panel style (One UI capsule), media seekbar, ambient color

## Missing Features (Priority Order)

### HIGH IMPACT / HIGH VALUE

#### 1. Status Bar Clock Customization
**Target files**: `statusbar/clock/StatusBarClockNew.kt`

Current: Basic clock display
Missing:
- [ ] Clock format (12/24h, HH:mm:ss, custom)
- [ ] Clock position (center, left, right)
- [ ] Font selection (system, custom TTF)
- [ ] Font size adjustment
- [ ] Color override (auto, white, black, custom)
- [ ] Show/hide seconds
- [ ] Am/pm display

**Complexity**: Medium
**Dependencies**: TextFont hook, ClockView resolution
**Estimate**: 3-5 days

#### 2. Battery Percentage Text
**Target files**: `statusbar/battery/BatteryDetailIndicator.kt`, `plugin/systemui/NewBrightnessPct.kt`

Current: Battery icon indicators
Missing:
- [ ] Show battery % in status bar
- [ ] Position (left of icon, right of icon, center)
- [ ] Color (match icon, custom, auto)
- [ ] Format (10%, 9, 0%)
- [ ] Show charging %
- [ ] Low battery warning color

**Complexity**: Low-Medium
**Dependencies**: BatteryLevelView hook
**Estimate**: 2-3 days

#### 3. Notification Shade Customization
**Target files**: New - `rules/systemui/notificationshade/`

Missing (entirely new):
- [ ] Status bar icon count limit
- [ ] Notification layout (compact, expanded, list)
- [ ] Notification swipe actions
- [ ] Quick settings row count
- [ ] QS tile layout (grid, list)
- [ ] Notification swipe to clear
- [ ] Long-press actions
- [ ] Per-app notification settings

**Complexity**: High
**Dependencies**: NotificationPanel, QuickSettingsPanel hooks
**Estimate**: 5-7 days

#### 4. Quick Settings Grid Customization
**Target files**: `controlcenter/QSGrid.kt`, `controlcenter/QQSGrid.kt`

Current: Basic grid control
Missing:
- [ ] Grid columns (2, 3, 4, 5)
- [ ] Grid rows (3, 4, 5, 6)
- [ ] Tile size
- [ ] Tile shape (circle, rounded, square)
- [ ] Tile labels (show/hide)
- [ ] Tile reordering
- [ ] Custom tile icons
- [ ] Tile colors

**Complexity**: Medium
**Dependencies**: QuickSettingsTile hooks
**Estimate**: 3-4 days

### MEDIUM IMPACT / MEDIUM VALUE

#### 5. Dynamic Color (Material You)
**Target files**: `other/MonetThemeOverlay.kt`, `controlcenter/media3/AmbientColorResolverOS4.kt`

Current: Basic ambient color
Missing:
- [ ] Wallpaper color extraction
- [ ] Live wallpaper support
- [ ] Per-app color themes
- [ ] Color intensity adjustment
- [ ] Dark mode auto-adjust
- [ ] Color lock (prevent changes)

**Complexity**: Medium
**Dependencies**: ColorPalette, WallpaperManager hooks
**Estimate**: 4-5 days

#### 6. Icon Pack Support
**Target files**: New - `rules/systemui/iconpack/`

Missing (entirely new):
- [ ] Icon pack detection
- [ ] Icon shape
- [ ] Icon mask (circle, squircle, rounded)
- [ ] Icon size
- [ ] Adaptive icon support
- [ ] Custom icon for specific apps

**Complexity**: High
**Dependencies**: IconLoader framework, PackageManager hooks
**Estimate**: 5-7 days

#### 7. Animation Customization
**Target files**: New - `rules/systemui/animation/`

Missing (entirely new):
- [ ] Activity transition style
- [ ] Transition duration
- [ ] Overscroll glow
- [ ] Notification animation
- [ ] QS expand animation
- [ ] Lock screen animation

**Complexity**: Medium-High
**Dependencies**: ActivityTransition, WindowAnimation hooks
**Estimate**: 4-6 days

### LOW IMPACT / LOW VALUE

#### 8. Always-On Display (AOD) Customization
**Target files**: `plugin/aod/AodBlurButton.kt` (extend)

Current: Basic AOD blur
Missing:
- [ ] AOD layout
- [ ] AOD clock style
- [ ] AOD notification display
- [ ] AOD trigger
- [ ] AOD wallpaper

**Complexity**: High
**Dependencies**: AlwaysOnDisplay hooks
**Estimate**: 6-8 days

#### 9. Screenshot/Screen Recording UI
**Target files**: New - `rules/systemui/screenshot/`

Missing (entirely new):
- [ ] Screenshot style
- [ ] Screenshot sound
- [ ] Screenshot preview position
- [ ] Screen recording UI
- [ ] Long screenshot

**Complexity**: Medium
**Dependencies**: ScreenshotService hooks
**Estimate**: 3-4 days

## Architecture Improvements

### Refactor Style Settings UI

**Current issues**:
1. `StylesPageFragment.java` is minimal - only hosts `VolumeStyleSettings`
2. No organized categories in UI
3. Preferences are scattered across multiple XML files
4. No preview functionality (except volume panel)
5. No import/export of style presets

**Proposed structure**:
```
app/src/main/java/com/sevtinge/hypercraft/style/
├── StylesPageFragment.java (updated)
├── StylesViewModel.kt (new - state management)
├── StylesRepository.kt (new - settings access)
└── adapter/
    ├── StyleCategoryAdapter.kt (new)
    └── StyleItemAdapter.kt (new)

library/libhook/src/main/java/com/sevtinge/hypercraft/libhook/rules/systemui/
├── style/ (new - unified style manager)
│   ├── StyleConfig.kt (new - data class)
│   ├── StylePreset.kt (new - preset definitions)
│   └── StylePreview.kt (new - preview generation)
└── [existing hooks unchanged]
```

### New Settings UI Features

1. **Style Presets**
   - [ ] Built-in presets (Material, One UI, Custom)
   - [ ] Save/load user presets
   - [ ] Share presets (JSON export/import)

2. **Live Preview**
   - [ ] Screenshot-based preview
   - [ ] Real-time preview in settings
   - [ ] Compare before/after

3. **Style Validation**
   - [ ] Detect incompatible combinations
   - [ ] Auto-fix conflicts
   - [ ] Safe mode integration

## Implementation Plan

### Phase 1 (2-3 weeks)
1. Status bar clock customization
2. Battery percentage text
3. Quick settings grid (columns/rows)
4. Refactor style settings UI

### Phase 2 (3-4 weeks)
5. Notification shade customization
6. Dynamic color (Material You)
7. Animation customization

### Phase 3 (4-6 weeks)
8. Icon pack support
9. AOD customization
10. Screenshot UI

### Phase 4 (Ongoing)
- Style presets (save/load/share)
- Live preview system
- Per-app style overrides

## Technical Considerations

### Xposed API Limitations
- Hidden API usage requires `HiddenApiBypass`
- Some classes may not be accessible on certain ROM versions
- Version gating required (HyperOS vs MIUI)

### Performance
- Avoid heavy work in UI thread
- Use `Handler` for deferred rendering
- Cache expensive operations

### Testing
- Each feature needs ROM version testing
- Safe mode integration required
- Crash isolation (use `HookIsolation`)

## Success Metrics

- [ ] User satisfaction (feature requests)
- [ ] Crash rate (target < 1%)
- [ ] Performance impact (< 5ms per frame)
- [ ] Code coverage (> 80% for new hooks)

## Related Files

- `app/src/main/java/com/sevtinge/hypercraft/style/StylesPageFragment.java`
- `library/libhook/src/main/java/com/sevtinge/hypercraft/libhook/rules/systemui/`
- `app/src/main/res/xml/prefs_*.xml`
- `docs/hook-diagnostics.md`

## Links

- [Xposed API Docs](https://xposed-api.lsposed.org/)
- [LSPosed Docs](https://docs.lsposed.org/)
- [Android SystemUI Architecture](https://developer.android.com/develop/ui/systemui)