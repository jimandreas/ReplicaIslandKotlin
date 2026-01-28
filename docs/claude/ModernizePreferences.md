# Preferences Modernization Plan - ReplicaIslandKotlin

## Summary
Modernize the preferences system from deprecated `PreferenceActivity` to `PreferenceFragmentCompat` and migrate from `SharedPreferences` to Jetpack DataStore. The migration is split into 4 testable phases.

---

## Phase 1: Fragment Migration (Critical - PreferenceActivity deprecated)

### Dependencies to Add

**gradle/libs.versions.toml:**
```toml
preference = "1.2.1"
```
```toml
androidx-preference-ktx = { group = "androidx.preference", name = "preference-ktx", version.ref = "preference" }
```

**app/build.gradle.kts:** Add `implementation(libs.androidx.preference.ktx)`

### Files to Create

| File | Purpose |
|------|---------|
| `ui/SettingsFragment.kt` | PreferenceFragmentCompat hosting preferences |
| `res/layout/activity_settings.xml` | FrameLayout container for fragment |
| `ui/YesNoDialogPreferenceCompat.kt` | AndroidX version of YesNoDialogPreference |
| `ui/YesNoDialogFragment.kt` | DialogFragment for erase confirmation |
| `ui/SliderPreferenceCompat.kt` | Extends SeekBarPreference with min/max labels |
| `res/layout/slider_preference_compat.xml` | Layout for slider with labels |
| `ui/KeyboardConfigDialogPreferenceCompat.kt` | AndroidX version with attribute handling |
| `ui/KeyboardConfigDialogFragment.kt` | DialogFragment with key capture logic |

### Files to Modify

| File | Changes |
|------|---------|
| `ui/SetPreferencesActivity.kt` | Change base class to `AppCompatActivity`, host `SettingsFragment` |
| `res/xml/preferences.xml` | Update class references to `*Compat` versions, use `SwitchPreferenceCompat` |

### Key Changes to SetPreferencesActivity.kt
```kotlin
class SetPreferencesActivity : AppCompatActivity(), YesNoDialogPreferenceCompat.YesNoDialogListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        // ... fullscreen setup ...

        if (savedInstanceState == null) {
            val fragment = SettingsFragment()
            if (intent.getBooleanExtra("controlConfig", false)) {
                fragment.arguments = Bundle().apply {
                    putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, "controlConfigScreen")
                }
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.settings_container, fragment)
                .commit()
        }
    }
}
```

### Testing Phase 1
- [ ] Preferences screen displays all categories
- [ ] Sound toggle saves and persists
- [ ] Control config screen navigates correctly
- [ ] Slider preferences work and save
- [ ] Keyboard config dialog captures keys and saves
- [ ] Erase game dialog works and clears data
- [ ] "controlConfig" intent navigates to controls screen

---

## Phase 2: DataStore Infrastructure

### Dependencies to Add

**gradle/libs.versions.toml:**
```toml
datastore = "1.1.1"
```
```toml
androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
```

### Files to Create

| File | Purpose |
|------|---------|
| `data/GamePreferences.kt` | DataStore keys and GamePreferencesRepository |
| `data/PreferencesMigrationHelper.kt` | One-time migration from SharedPreferences |
| `data/PreferencesManager.kt` | Facade for dual-write during transition |

### Key Pattern: Dual-Write Strategy
- Reads from SharedPreferences (for PreferenceFragmentCompat compatibility)
- Writes to BOTH SharedPreferences AND DataStore
- Ensures data consistency during transition

### Testing Phase 2
- [ ] DataStore initializes without errors
- [ ] Migration helper copies existing preferences
- [ ] Dual-write works for all preference types
- [ ] Game state persists correctly

---

## Phase 3: Consumer Migration

### Files to Modify

| File | Changes |
|------|---------|
| `AndouKun.kt` | Replace SharedPreferences access with PreferencesManager |
| `ui/MainMenuActivity.kt` | Replace SharedPreferences access with PreferencesManager |
| `ui/GameOverActivity.kt` | Replace SharedPreferences reads with PreferencesManager |
| `ui/ExtrasMenuActivity.kt` | Replace SharedPreferences reads with PreferencesManager |
| `ui/SetPreferencesActivity.kt` | Update onDialogClosed() to use PreferencesManager |

### Migration Pattern
Before:
```kotlin
val prefs = getSharedPreferences(PreferenceConstants.PREFERENCE_NAME, MODE_PRIVATE)
val soundEnabled = prefs.getBoolean(PreferenceConstants.PREFERENCE_SOUND_ENABLED, true)
```

After:
```kotlin
val prefsManager = PreferencesManager.getInstance(this)
val soundEnabled = prefsManager.getSoundEnabled()
```

### Testing Phase 3
- [ ] Game launches and plays normally
- [ ] All controls work (keyboard, screen, gamepad)
- [ ] Level progress saves and loads
- [ ] Game statistics display on game over
- [ ] Extras unlock after completion

---

## Phase 4: SharedPreferences Removal

### Files to Create

| File | Purpose |
|------|---------|
| `data/DataStorePreferenceAdapter.kt` | Connects PreferenceFragmentCompat to DataStore |

### Files to Modify
- `ui/SettingsFragment.kt` - Use `preferenceManager.preferenceDataStore` instead of SharedPreferences
- `data/PreferencesManager.kt` - Remove dual-write, read from DataStore only

### Files to Delete (after verification)
- `ui/YesNoDialogPreference.kt`
- `ui/KeyboardConfigDialogPreference.kt`
- `ui/SliderPreference.kt`

### Testing Phase 4
- [ ] Fresh install works correctly
- [ ] Upgrade from Phase 3 preserves all data
- [ ] No SharedPreferences file created for new users

---

## Critical Files Reference

| File | Purpose |
|------|---------|
| `app/src/main/java/com/replica/replicaisland/ui/SetPreferencesActivity.kt` | Main activity to migrate |
| `app/src/main/java/com/replica/replicaisland/ui/PreferenceConstants.kt` | 26 preference keys |
| `app/src/main/res/xml/preferences.xml` | Preference UI definitions |
| `app/src/main/java/com/replica/replicaisland/ui/KeyboardConfigDialogPreference.kt` | Most complex custom preference |
| `app/src/main/java/com/replica/replicaisland/AndouKun.kt` | Primary preference consumer |
| `gradle/libs.versions.toml` | Dependency versions |

---

## Verification Strategy

After each phase:
1. Build the app: `./gradlew assembleDebug`
2. Install on device/emulator
3. Run through the testing checklist
4. Test on both API 24 (min SDK) and API 36 (target SDK)
5. Test upgrade path from previous version
6. Test fresh install scenario

---

## Risk Mitigation

- **Data Loss Prevention**: Dual-write in Phase 2-3 ensures data in both storage systems
- **Rollback**: Each phase is independently deployable; can revert if issues found
- **Gradual Migration**: SharedPreferences retained until Phase 4 verification complete
