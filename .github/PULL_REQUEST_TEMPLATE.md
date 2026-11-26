# Pull Request

## Description
<!-- Describe what this PR does and why -->

## Type of Change
- [ ] Bug fix (non-breaking change which fixes an issue)
- [ ] New feature (non-breaking change which adds functionality)
- [ ] Breaking change (fix or feature that would cause existing functionality to not work as expected)
- [ ] Documentation update
- [ ] CI/Build improvement
- [ ] Refactoring / Code quality
- [ ] Translation / Locale update

## Component(s) Affected
- [ ] Settings / UI (app module)
- [ ] Home / Launcher hooks (libhook)
- [ ] SystemUI hooks (libhook)
- [ ] Native / C++ hooks (app/src/main/cpp)
- [ ] Safe mode / Crash handling
- [ ] Backup & Restore
- [ ] Scope management
- [ ] Logging / Diagnostics
- [ ] Build / CI / Gradle
- [ ] Tests
- [ ] Documentation
- [ ] Other: ___________

## Testing
<!-- How has this been tested? -->
- [ ] Unit tests pass (`./gradlew testDebugUnitTest`)
- [ ] Lint passes (`./gradlew lintDebug`)
- [ ] Detekt passes (`./gradlew detektCheck`)
- [ ] Ktlint passes (`./gradlew ktlintCheck`)
- [ ] Native host tests pass (`./gradlew :app:hostNativeTests`)
- [ ] Manual testing on device (specify ROM/device):
  - ROM: 
  - Device: 
  - Xposed: 

## Screenshots / Recordings
<!-- If UI changes, add screenshots or screen recordings -->

## Checklist
- [ ] My code follows the project's coding standards
- [ ] I have performed a self-review of my code
- [ ] I have commented my code, particularly in hard-to-understand areas
- [ ] I have made corresponding changes to the documentation
- [ ] My changes generate no new warnings
- [ ] I have added tests that prove my fix is effective or that my feature works
- [ ] New and existing unit tests pass locally with my changes
- [ ] Any dependent changes have been merged and published in downstream modules

## Related Issues
<!-- Link to related issues using "Fixes #123" or "Relates to #123" -->
Fixes #