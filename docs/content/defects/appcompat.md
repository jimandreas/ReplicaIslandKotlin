---
title: "Theme.AppCompat Issue"
date: 2025-01-01
draft: false
---

# Theme.AppCompat theme (or descendant).

This error check appears in the emulator - smells like the beginning of enforcement of
a theme when using AppCompatTextView.

In Replica Island this usage is below:

```
class TypewriterTextView : androidx.appcompat.widget.AppCompatTextView {
```

The check happens here in the Android system code:
```
public AppCompatTextView(
            @NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(TintContextWrapper.wrap(context), attrs, defStyleAttr);

        ThemeUtils.checkAppCompatTheme(this, getContext());
```

And here is the check:

```
    /**
     * Checks that the specific view (which should be an AppCompat widget) is
     * using a {@link Context} that is an AppCompat theme or its descendant.
     */
    public static void checkAppCompatTheme(@NonNull View view, @NonNull Context context) {
        TypedArray a = context.obtainStyledAttributes(R.styleable.AppCompatTheme);

        try {
            // Same check as in AppCompatDelegateImpl - do not allow using AppCompat widgets
            // without a top-level AppCompat theme (or its descendant). For now flag this as
            // an error-level log message.
            if (!a.hasValue(R.styleable.AppCompatTheme_windowActionBar)) {
                android.util.Log.e(TAG, "View " + view.getClass()
                        + " is an AppCompat widget that can only be used with a "
                        + "Theme.AppCompat theme (or descendant).");
```

Solution: build the theme that the app needs:

```
    <style name="Theme.AppCompat.NoTitleBar.Fullscreen" parent="Theme.AppCompat">
        <item name="android:windowNoTitle">true</item>
        <item name="android:windowFullscreen">true</item>
    </style>
```
