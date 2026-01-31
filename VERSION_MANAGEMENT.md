# Version Management & Upgrade Strategy

## Overview

This document describes how versioning and app upgrades work across all platforms.

## Version Management

### Current Version

All version information is managed in a single place: **`gradle.properties`**

```properties
app.version.code=3        # Android only (Play Store increment)
app.version.name=1.0.2    # All platforms (semantic versioning)
```

### Build System Reference

- **Android** (`composeApp/build.gradle.kts`):
  ```kotlin
  versionCode = project.property("app.version.code").toString().toInt()
  versionName = project.property("app.version.name").toString()
  ```

- **Desktop** (`composeApp/build.gradle.kts`):
  ```kotlin
  packageVersion = project.property("app.version.name").toString()
  // Also uses upgradeUuid = "a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d" for MSI upgrades
  ```

---

## Platform-Specific Upgrade Behavior

### Android

**Automatic Upgrade (No Manual Uninstall):**
- Prerequisite: Same `applicationId` + same signing key
- APK with same or higher `versionCode` overwrites previous version
- **Settings preserved:** SharedPreferences data (AppPreferences)

**Steps:**
1. User installs new APK
2. Old version is automatically replaced
3. Settings (folderUri, thumbnailSize, etc.) remain intact

**Implementation:**
- Build signing is configured for Play Store (set in `build.gradle.kts`)
- For local testing: `adb install -r app.apk`

---

### Windows (Desktop)

**Automatic Upgrade (Managed Uninstall):**
- Prerequisite: Same `upgradeUuid` in MSI configuration
- Gradient Compose automatically generates MSI with upgrade support
- The MSI installer performs the uninstall/reinstall automatically

**Settings Preserved:**
- AppData/Roaming directory (java.util.prefs) is NOT touched during upgrade
- User configuration persists across versions

**Alternative (Manual Helper Script):**
For cases where MSI upgrade fails or needs manual control:

```bash
scripts/upgrade.bat "path\to\OrganizePhotos-1.0.3.msi"
```

This script:
1. Terminates running OrganizePhotos process
2. Uninstalls previous version via MSI
3. Installs new version
4. User settings in AppData are preserved

**Installation:**
- User downloads MSI and runs it (or uses helper script)
- Existing application is upgraded automatically
- Settings preserved

---

### Linux (Desktop)

**Automatic Upgrade (Package Manager):**
- Package name: `organize-photos`
- Upgrade via package manager:
  ```bash
  sudo apt-get install organize-photos  # Handles upgrade automatically
  ```

**Settings Preserved:**
- Configuration in `~/.config/` or `~/.local/share/` remains intact
- DEB package respects existing configurations

---

## Versioning Workflow

### For Development

No manual version bumps needed during development. Just commit to `main`.

### For Release (Manual)

**Option A: GitHub Actions (Recommended)**

1. Go to **Actions → Release - Bump Version**
2. Click **Run workflow**
3. Choose bump type:
   - `patch`: 1.0.2 → 1.0.3 (bug fixes)
   - `minor`: 1.0.2 → 1.1.0 (new features)
   - `major`: 1.0.2 → 2.0.0 (breaking changes)

**What happens:**
- ✓ gradle.properties updated
- ✓ versionCode incremented automatically
- ✓ Git commit created
- ✓ Git tag created (`v1.0.3`)
- ✓ GitHub Release created
- ✓ Tag pushed to GitHub
- ✓ All CI builds triggered automatically

**Option B: Manual (For hotfixes)**

```bash
# Edit gradle.properties
app.version.code=4
app.version.name=1.0.3

# Commit and tag
git add gradle.properties
git commit -m "chore: bump version to 1.0.3"
git tag -a v1.0.3 -m "Release v1.0.3"
git push origin main v1.0.3
```

---

## CI/CD Triggers

### Automatic Builds

Builds are triggered on:
- `push` to `main` or `feature/*` branches
- Explicit tag push (`v*`)
- Manual `workflow_dispatch` (Actions dashboard)

### Release Build Artifacts

When a tag like `v1.0.3` is pushed:
- ✓ Windows MSI/EXE generated
- ✓ Android APK generated
- ✓ Linux DEB generated
- ✓ All artifacts uploaded to GitHub Actions Artifacts
- ✓ GitHub Release page shows artifacts

---

## User Update Procedure

### Android

**From Previous APK:**
```
1. Download new APK
2. Install new APK
3. Settings automatically preserved ✓
```

**From Google Play Store:**
```
1. Update appears in Play Store
2. Tap "Update"
3. Settings automatically preserved ✓
```

### Windows

**MSI Installer:**
```
1. Download OrganizePhotos-1.0.3.msi
2. Double-click to run
3. Let installer handle uninstall/upgrade
4. Settings automatically preserved ✓
```

**Alternative with helper script:**
```
1. Download OrganizePhotos-1.0.3.msi
2. Run: scripts/upgrade.bat "path\to\msi"
3. Wait for completion
4. Settings automatically preserved ✓
```

### Linux

```bash
sudo apt-get update
sudo apt-get install organize-photos  # Auto-upgrades existing
# Settings automatically preserved ✓
```

---

## Troubleshooting

### "Cannot upgrade - different signature"

**Android:**
- Cause: APK signed with different key
- Fix: Use same release keystore or install fresh APK

**Windows:**
- Cause: `upgradeUuid` mismatch
- Fix: Run `scripts/upgrade.bat` or manual uninstall + fresh install

### "Settings lost after upgrade"

**Android:**
- Cause: Uninstalled via Settings instead of upgrade
- Fix: Restore from backup if available

**Windows:**
- Cause: Installed to different location or cleared AppData
- Fix: Check AppData/Roaming folder exists

**Linux:**
- Cause: Custom config location not preserved
- Fix: Manual backup before upgrade

---

## Version History

| Version | Date | Notes |
|---------|------|-------|
| 1.0.0   | 2026-01-15 | Initial release |
| 1.0.1   | 2026-01-20 | Bug fixes |
| 1.0.2   | 2026-01-28 | Android v1.0, UI improvements |
| 1.0.3   | TBD | Version management framework |

---

## References

- `gradle.properties` - Central version configuration
- `composeApp/build.gradle.kts` - Android/Desktop build settings
- `.github/workflows/release.yml` - Automatic version bumping CI
- `scripts/upgrade.bat` - Windows upgrade helper
- `scripts/bump-version.sh` - Manual version bumping script
