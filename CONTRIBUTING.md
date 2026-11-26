# Contributing to HyperCraft

Thank you for your interest in contributing to HyperCraft! This document provides guidelines for contributing to the project.

## Table of Contents
- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Making Changes](#making-changes)
- [Testing](#testing)
- [Submitting Changes](#submitting-changes)
- [Code Style](#code-style)
- [Architecture Overview](#architecture-overview)

## Code of Conduct
This project adheres to the [Contributor Covenant Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code.

## Getting Started

### Prerequisites
- **JDK 21** (LTS) - required for building
- **Android SDK** (API 37 / Android 17)
- **Android NDK** (r28+)
- **Gradle** (via wrapper)
- **Kotlin 2.4+**
- **Python 3.10+** (for test scripts)
- **CMake 3.22+** and **Ninja** (for native builds)
- **clang/clang++** (for native code)

### Fork and Clone
```bash
git clone https://github.com/YOUR_USERNAME/HyperCraft.git
cd HyperCraft
git remote add upstream https://github.com/ReChronoRain/HyperCraft.git
```

## Development Setup

### 1. Configure Gradle Properties
Create `local.properties` (or use `signing.properties` for release):
```properties
# SDK/NDK paths (optional - will be downloaded if missing)
sdk.dir=/path/to/android-sdk
ndk.dir=/path/to/android-ndk
```

### 2. Build the Project
```bash
# Debug build (no signing required)
./gradlew assembleDebug

# Run lint checks
./gradlew lintDebug

# Run static analysis
./gradlew detektCheck ktlintCheck

# Run native host tests (no device needed)
./gradlew :app:hostNativeTests
```

### 3. IDE Setup
- **Android Studio Ladybug+** or **IntelliJ IDEA 2024+**
- Install **Kotlin** plugin
- Enable **Annotation Processing** (Settings > Build > Compiler > Annotation Processors)
- Import project as **Gradle** project

## Making Changes

### Branch Naming
- `feature/<short-description>` - new features
- `fix/<short-description>` - bug fixes
- `refactor/<short-description>` - code improvements
- `docs/<short-description>` - documentation
- `ci/<short-description>` - CI/build changes

### Commit Messages
Follow [Conventional Commits](https://www.conventionalcommits.org/):
```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

Types: `feat`, `fix`, `refactor`, `docs`, `ci`, `chore`, `test`, `perf`, `style`

Examples:
```
feat(home): add OS4 capsule volume panel support
fix(safe-mode): prevent SystemUI bootloop on hook crash
refactor(settings): extract PreferencesKeys constants
docs: add CONTRIBUTING.md
```

### Code Changes

#### Kotlin/Java (app, libhook, common, core, etc.)
- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use `detekt` and `ktlint` for style enforcement
- Prefer **coroutines** over callbacks
- Avoid `GlobalScope`, `runBlocking` on main thread
- Use **structured concurrency** (`coroutineScope`, `supervisorScope`)
- Wrap all reflection/`HiddenApiBypass` calls in `ReflectionUtils.safeInvoke`

#### C++ (app/src/main/cpp)
- C++20 standard
- Follow existing code style (4 spaces, no tabs)
- Use `-Wall -Wextra -Werror` (see CMakeLists.txt)
- Prefer `std::optional`, `std::variant`, RAII
- No raw `new`/`delete` - use smart pointers
- JNI: check exceptions after every call, use `ScopedLocalRef`/`ScopedUtfChars`

#### XML (layouts, prefs, values)
- Use `tools:` attributes for design-time data
- Keep layout depth minimal
- Use `ConstraintLayout` over nested `LinearLayout`
- String resources: always in `values/strings.xml` first, then translated

### Hook Development Guidelines
1. **Isolation**: Every feature hook must be wrapped in try-catch
2. **Safe Mode**: Report failures to `SafeModeHandler` for auto-disable
3. **Version Checks**: Use `RomVersion` abstraction, not raw `Build.VERSION`
4. **Hidden API**: Always use `ReflectionUtils.safeInvoke` wrapper
5. **Logging**: Use `XposedLog` with appropriate level (d/w/e)
6. **Performance**: Avoid allocations in hot paths (before/after hooks)

## Testing

### Required Before Submitting
```bash
# Unit tests
./gradlew testDebugUnitTest

# Lint
./gradlew lintDebug

# Static analysis
./gradlew detektCheck ktlintCheck

# Native tests
./gradlew :app:hostNativeTests
```

### Test Categories
| Test Type | Command | When |
|-----------|---------|------|
| Unit tests | `./gradlew testDebugUnitTest` | Every PR |
| Instrumented | `./gradlew connectedAndroidTest` | Device required |
| Native host | `./gradlew :app:hostNativeTests` | Every PR |
| Lint | `./gradlew lintDebug` | Every PR |
| Detekt | `./gradlew detektCheck` | Every PR |
| Ktlint | `./gradlew ktlintCheck` | Every PR |

### Adding Tests
- **Unit tests**: `app/src/test/java/...`, `library/*/src/test/...`
- **Instrumented**: `app/src/androidTest/...`
- **Native**: `app/src/main/cpp/nativehook/` + `tests/run_host_native_tests.sh`
- **Integration**: `tests/<feature>/run.py` (requires ROM data)

## Submitting Changes

### Pull Request Process
1. Ensure all checks pass locally
2. Push to your fork
3. Open PR against `main` branch
4. Fill out the PR template completely
5. Link related issues: `Fixes #123`
6. Wait for CI to pass
7. Address review comments
8. Maintainers will merge after approval

### PR Requirements
- [ ] All CI checks pass (build, lint, detekt, ktlint, native tests)
- [ ] No new warnings introduced
- [ ] Changes are minimal and focused
- [ ] Tests added for new functionality
- [ ] Documentation updated if needed
- [ ] Commit messages follow convention

## Code Style

### Kotlin
- `ktlint` with standard rules
- `detekt` with project config (see `detekt.yml`)
- Max line length: 120 chars
- Indentation: 4 spaces

### Java
- Follow Google Java Style (enforced by lint)
- Migrate to Kotlin when touching files

### C++
- clang-format (see `.clang-format` if present)
- C++20 features encouraged
- No exceptions (use `std::expected`/`Result` pattern)

### XML
- 4-space indentation
- Attributes on separate lines for readability
- `tools:` namespace for preview data

## Architecture Overview

```
HyperCraft (Xposed Module)
├── app (UI + Settings)
│   ├── SettingsFragment → migrating to Kotlin + ViewModel
│   ├── Backup/Restore → service layer
│   └── Scope management
├── library/
│   ├── libhook (385 .kt) - Hook Engine Core
│   │   ├── rules/ - Feature hooks (each isolated)
│   │   ├── safecrash/ - CrashMonitor, SafeModeHandler
│   │   └── utils/hookapi/ - ReflectionUtils, HiddenApiBypass wrapper
│   ├── core/common/provision - Shared utilities
│   └── hidden-api - Bypass implementation
├── app/src/main/cpp - Native (C++20, ARM64)
│   ├── nativehook/ - Runtime infrastructure
│   └── targets/home/ - Home/Launcher hooks
├── tests/ - Regression/verification scripts (40+)
└── .github/workflows/ - CI/CD
```

## Getting Help
- **Discord**: [HyperCraft Community](https://discord.gg/hypercraft)
- **Telegram**: @HyperCraftChat
- **Issues**: Use the bug report/feature request templates

## License
HyperCraft is licensed under **GNU AGPL v3**. By contributing, you agree that your contributions will be licensed under the same license.