# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Gradle dependency verification (`gradle/verification-metadata.xml`)
- Centralized preference keys in `PreferencesKeys.kt`
- Missing locale files: Korean (ko-KR), French (fr-FR)
- All locale files now have complete string coverage (168 strings each)
- GitHub issue templates (bug report, feature request)
- Pull request template
- CONTRIBUTING.md, SECURITY.md, CODE_OF_CONDUCT.md
- CHANGELOG.md
- ReflectionUtils for safe HiddenApiBypass calls
- RomVersion abstraction for version checking
- HookIsolation utility for feature-level try-catch
- SafeModeHandler.reportCrash() method
- Unit tests for AppSettingsStore, ReflectionUtils, RomVersion
- Locale sync script (`scripts/sync_strings.py`)
- detekt static analysis configuration

### Changed
- CI workflows: Removed `.github/**` from paths-ignore to catch workflow changes
- CI secret handling: Environment variables instead of writing `signing.properties` file
- SettingsFragment: Migrated to use `PreferencesKeys` constants
- All build files: Use centralized version catalog for SDK/NDK/JDK versions
- CMake: Replaced `-Wno-everything` with targeted warning suppression
- Build: JDK 21 LTS instead of JDK 25 EA
- Build: Removed non-reproducible BuildConfig fields (build time, user, OS)

### Fixed
- Build reproducibility: Dependency verification enabled
- Locale completeness: All 18 locales now have all 168 strings
- restartApp(): Removed `Process.killProcess` for proper lifecycle handling
- CMake: Fixed `-Wno-everything` hiding UB (std::string to %s)

### Security
- Dependency verification metadata committed
- Secrets no longer written to files in CI
- clang-tidy integration for native code

---

## [2.11.170] - 2026-11-26

### Added
- Interactive live volume studio preview
- Dedicated Style tab in bottom navigation
- Samsung One UI capsule volume panel implementation
- Volume Style settings section with preset theming
- Miuix support classes retained for release startup

### Fixed
- Parallel runtime issues
- SystemUI freeze and soft-reboot prevention (VolumeColumnRes routing)
- Preference theme text colors in live preview layout
- Build review findings

### Changed
- OS4 native back gesture improvements
- Share APK and shortcut menu keys registered in OS4 adaptation gate

---

## [2.11.169] - 2026-11-20

### Added
- OS4 home grid protocol support
- Folder close landing animation
- Back gesture icon scale improvements

### Fixed
- Folder auto-close on scroll (OS4)
- Dock style refresh issues
- Weather native bridge

---

## [2.11.168] - 2026-11-15

### Added
- Experimental OS4 popsicle support
- Hook diagnostics documentation
- ARM64 emulation test infrastructure

### Fixed
- Back gesture complete implementation
- Capture differential scripts

---

## Older Releases

For changes prior to v2.11.168, see the Git history and release tags.

---

## Release Types

| Type | Description | Version Bump |
|------|-------------|--------------|
| **Major** | Breaking changes, API incompatibility | X.0.0 |
| **Minor** | New features, backward compatible | x.Y.0 |
| **Patch** | Bug fixes, backward compatible | x.y.Z |
| **Canary** | Daily builds from main (`-canary-<hash>`) | - |
| **Beta** | Pre-release for testing (`-beta-<date>`) | - |

## Links

- [GitHub Releases](https://github.com/ReChronoRain/HyperCeiler/releases)
- [Compare v2.11.170...main](https://github.com/ReChronoRain/HyperCeiler/compare/v2.11.170...main)