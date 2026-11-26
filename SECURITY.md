# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| Latest release (main branch) | ✅ Yes |
| Previous release | ⚠️ Security fixes only |
| Older versions | ❌ No longer supported |

We recommend always using the latest release from the main branch.

## Reporting a Vulnerability

**Please do NOT report security vulnerabilities via public GitHub issues.**

Instead, please report them via one of these channels:

1. **Email**: security@hypercraft.org (PGP key available on request)
2. **GitHub Security Advisories**: Use the "Report a vulnerability" tab in the Security section of this repository
3. **Direct Message**: Contact maintainers via Discord/Telegram (links in README)

### What to Include
- Description of the vulnerability
- Steps to reproduce (if applicable)
- Affected versions/components
- Potential impact
- Any suggested mitigations

### Response Timeline
- **Acknowledgment**: Within 48 hours
- **Initial Assessment**: Within 7 days
- **Fix Development**: Depends on severity (target: 30 days for critical)
- **Disclosure**: Coordinated with reporter after fix is released

## Security Model

### Threat Model
HyperCraft is an Xposed module that hooks into system processes (SystemUI, Home, Settings, etc.) on Xiaomi HyperOS/MIUI devices. The security model assumes:

1. **User Intent**: The user explicitly installs and enables the module via LSPosed/KernelSU
2. **Root Access**: The module runs with elevated privileges (via Xposed framework)
3. **System Modification**: Hooks modify system behavior at runtime
4. **No Network**: The module itself makes no network requests (except optional update checks)

### Security Boundaries
- **Hook Isolation**: Each feature hook is independently wrapped; failures don't cascade
- **Safe Mode**: Automatic disable of hooks causing repeated crashes
- **Signature Verification**: Release builds verify module signature at runtime
- **Debug Mode**: Explicit opt-in for development; not for production use

### Known Risks
- **SystemUI Bootloop**: Malformed hooks can crash SystemUI repeatedly → mitigated by Safe Mode
- **Hidden API Usage**: Reflection on internal APIs may break on OS updates → mitigated by version checks and fallbacks
- **Root Exposure**: Module requires root/Xposed → user must trust the source

## Secure Development Practices

### For Contributors
- Never commit secrets (keystore, API keys, tokens)
- Use `HiddenApiBypass` only via `ReflectionUtils.safeInvoke`
- Add version guards for all reflection/hook points
- Test on multiple ROM versions before submitting
- Run `./gradlew detektCheck` - security rules are enabled

### For Maintainers
- Release signing keys stored in GitHub Secrets (not in repo)
- Dependency verification enabled (`gradle/verification-metadata.xml`)
- CI builds on macOS runners with ephemeral keys
- No `mavenLocal()` in CI - all deps from verified repositories

## Disclosure Policy

We follow **Coordinated Vulnerability Disclosure**:
1. Reporter submits vulnerability privately
2. We acknowledge and assess
3. We develop and test fix
4. We release fix (patch version)
5. We publish advisory (after reasonable time for users to update)
6. Credit given to reporter (unless anonymous requested)

## Security-Related Features

| Feature | Purpose |
|---------|---------|
| Safe Mode | Auto-disables hooks after repeated crashes |
| Signature Verification | Detects tampered/unofficial builds |
| Debug Mode Guard | Prevents accidental debug builds in production |
| Hook Isolation | Try-catch per feature prevents cascade failures |
| Version Gating | ROM/Framework version checks before hooking |

## Contact

For security questions or concerns not covered here:
- **Email**: security@hypercraft.org
- **PGP**: Available on request via email

We appreciate responsible disclosure and will credit reporters in our security advisories.