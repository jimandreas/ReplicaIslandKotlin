---
title: "Logcat Attribution Tag"
date: 2025-01-01
draft: false
---

# attributionTag  not declared in manifest of

## SO advice:  https://stackoverflow.com/a/79780410/7061237

I've investigated the problem deeper, found the source code and it turns out that the error log message is constructed as:

msg = "attributionTag " + attributionTag + " not declared in manifest of "

So, it is easy to see that in case of @de11833 like in my case, the attributionTag searched was actually an empty string.

Finally, after one full day of fighting I've added both tags to the manifest (under the <manifest> tag, not <application> or <activity>)
```
<attribution android:tag="@string/empty" android:label="@string/description" />
<attribution android:tag="audioPlayback" android:label="@string/description" />
```

The trick is that simple empty tag "" or "@null" or omitted tag are not accepted during compile time or after run, so it is tricky to provide @string/empty as empty string using string resources.

Only after that the error in logcat is finally disappeared...

P.S. Of course I've created and properly used the attribution context by

createAttributionContext(context, "audioPlayback");

but it didn't help. So anyway I leave all the code as is and also keep "audioPlayback" tag in the manifest for the moment when this bug is fixed at the platform level

## My fix:

In my case, the tag was a "space" e.g.: attributionTag  not declared in manifest of

Here is my fix:

manifest:

```
<attribution android:tag="@string/space" android:label="@string/description" />
```

strings.xml:
```
<!-- bug fixes -->
<string name="description" translatable="false">logcat workaround, see defects/logcatAttributionTag.md</string>
<string name="space" translatable="false"> </string>
```
