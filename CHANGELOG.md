ReplicaIslandKotlin Change Log
===============================

Version K.0.4 *(2026-01-22)*
---------------------------------
* Convert dialog Activities to modern DialogFragment architecture
  - DiaryDialogFragment: Replaces DiaryActivity for diary entry display
  - ConversationDialogFragment: Replaces ConversationDialogActivity with typewriter text effect
  - LevelSelectDialogFragment: Replaces LevelSelectActivity using Fragment Result API
* Extract TypewriterTextView as standalone reusable component
* Convert AndouKun from Activity to AppCompatActivity for fragment support
* Add test infrastructure with Espresso and fragment-testing dependencies
* Add instrumented tests for all three DialogFragments (10 test cases)
* Deprecate original dialog activities (kept for rollback option)

Version K.0.3 *(2025-12)*
---------------------------------
* Migrate preferences from PreferenceActivity to PreferenceFragmentCompat
* Add DataStore infrastructure with dual-write migration strategy
* Connect Settings UI directly to DataStore
* Migrate activities to use PreferencesManager facade

Version K.0.2 *(2025-10-28)*
---------------------------------
* Convert to KTS build files, update versions

Version K.0.1 *(2021-05-17)
---------------------------------
* Updated versions

Version K.0.0 *(2020-09-21)
---------------------------------

* Change: convert to Kotlin complete
* Fix: all lint errors are suppressed or fixed
Ran lint on variant debug: 0 issues found
Ran lint on variant release: 0 issues found


