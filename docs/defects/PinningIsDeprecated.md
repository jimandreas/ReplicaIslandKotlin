
Hitting this error:

    ashmem                  com.replica.replicaisland            E  Pinning is deprecated since Android Q. Please use trim or other methods.

https://android.googlesource.com/platform/system/core/+/refs/heads/main/libcutils/ashmem-dev.cpp?hl=ar%2F%2F%2F%2F%2F%2F%2F%2F%2F%2F%2F%2F
```
static int do_pin(int op, int fd, size_t offset, size_t length) {
    static bool already_warned_about_pin_deprecation = false;
    if (!already_warned_about_pin_deprecation || debug_log) {
        ALOGE("Pinning is deprecated since Android Q. Please use trim or other methods.");
        already_warned_about_pin_deprecation = true;
    }
```

Digging into this - ashmem is a low layer function.  This diagram is helpful to understand the stack:

https://charleszblog.wordpress.com/wp-content/uploads/2014/02/ashmem1.jpg

as part of this writeup in charleszblog:

https://charleszblog.wordpress.com/2014/02/18/understanding-android-internals-ashmem/

Drilling further - maybe the error is triggered by the "newInstance" call into native code:

C:\a\sdk\sources\android-36\java\lang\Class.java

```
private transient volatile Constructor<T> cachedConstructor;
    */
    @FastNative
    @Deprecated(since="9")
    public native T newInstance() throws InstantiationException, IllegalAccessException;
```

In the game code - this is "suspicious"

```
class GameComponentPool : TObjectPool<GameComponent?> {

override fun fill() {
        if (objectClass != null) {
            for (x in 0 until fetchSize()) {
                try {
                    fetchAvailable()!!.add(objectClass!!.newInstance())
                } catch (e: IllegalAccessException) {
                    // TODO Auto-generated catch block
                    e.printStackTrace()
                } catch (e: InstantiationException) {
                    // TODO Auto-generated catch block
                    e.printStackTrace()
                }
            }
        }
    }
```
Here the "newInstance()" call in the GameComponentPool.kt module is deprecated. (as shown above)

Gemini suggests:

```
try {
    // Replaces: val myObject = objectClass!!.newInstance()
    val myObject = objectClass.getDeclaredConstructor().newInstance()
} catch (e: InstantiationException) {
    // Handle exceptions
} catch (e: IllegalAccessException) {
    // Handle exceptions
} catch (e: NoSuchMethodException) {
    // Handle exceptions
} catch (e: java.lang.reflect.InvocationTargetException) {
    // Handle exceptions
}
```

Looks like the bug is deep in Android code

```
/**
 * Convenience function for retrieving a single secure settings value
 * as an integer.  Note that internally setting values are always
 * stored as strings; this function converts the string to an integer
 * for you.  The default value will be returned if the setting is
 * not defined or not an integer.
 *
 * @param cr The ContentResolver to access.
 * @param name The name of the setting to retrieve.
 * @param def Value to return if the setting is not defined.
 *
 * @return The setting's current value, or 'def' if it is not defined
 * or not a valid integer.
 */
public static int getInt(ContentResolver cr, String name, int def) {
    String v = getString(cr, name);
    return parseIntSettingWithDefault(v, def);
}
```

And then here

```
@UnsupportedAppUsage
public String getStringForUser(ContentResolver cr, String name, final int userHandle) {
    final boolean isSelf = (userHandle == UserHandle.myUserId());
    final boolean useCache = isSelf && !isInSystemServer();
    boolean needsGenerationTracker = false;
```

and then here

```
// This method also updates the obsolete generation code stored locally
public boolean isGenerationChanged() {
    final int currentGeneration = readCurrentGeneration();
    if (currentGeneration >= 0) {
        if (currentGeneration == mCurrentGeneration) {
            return false;
        }
        mCurrentGeneration = currentGeneration;
    }
    return true;
}
```

and the error is tossed by 

android/util/MemoryIntArray.java

```
    /**
     * Gets the value at a given index.
     *
     * @param index The index.
     * @return The value at this index.
     * @throws IOException If an error occurs while accessing the shared memory.
     */
    public int get(int index) throws IOException {
        enforceNotClosed();
        enforceValidIndex(index);
        return nativeGet(mFd, mMemoryAddr, index);  <<  HERE
    }
```

Traceback:

```
at android.util.MemoryIntArray.get(MemoryIntArray.java:114)
at android.provider.Settings$GenerationTracker.readCurrentGeneration(Settings.java:3396)
at android.provider.Settings$GenerationTracker.isGenerationChanged(Settings.java:3380)
at android.provider.Settings$NameValueCache.getStringForUser(Settings.java:3751)
- locked <0x7558> (a android.provider.Settings$NameValueCache)
at android.provider.Settings$Global.getStringForUser(Settings.java:18676)
at android.provider.Settings$Global.getString(Settings.java:18659)
at android.provider.Settings$Global.getInt(Settings.java:18876)
at com.android.internal.policy.PhoneWindow.<init>(PhoneWindow.java:408)
at com.android.internal.policy.PhoneWindow.<init>(PhoneWindow.java:420)
at android.app.Activity.attach(Activity.java:9047)
at android.app.ActivityThread.performLaunchActivity(ActivityThread.java:4231)
at android.app.ActivityThread.handleLaunchActivity(ActivityThread.java:4467)
at android.app.servertransaction.LaunchActivityItem.execute(LaunchActivityItem.java:222)
at android.app.servertransaction.TransactionExecutor.executeNonLifecycleItem(TransactionExecutor.java:133)
at android.app.servertransaction.TransactionExecutor.executeTransactionItems(TransactionExecutor.java:103)
at android.app.servertransaction.TransactionExecutor.execute(TransactionExecutor.java:80)
at android.app.ActivityThread$H.handleMessage(ActivityThread.java:2823)
at android.os.Handler.dispatchMessage(Handler.java:110)
at android.os.Looper.loopOnce(Looper.java:248)
```

Found this from a google search:

```
https://cs.android.com/android/platform/superproject/+/master:system/core/libcutils/ashmem-dev.cpp;bpv=1;bpt=0

int ashmem_pin_region(int fd, size_t offset, size_t len)
{
    if (!pin_deprecation_warn || debug_log) {
        ALOGE("Pinning is deprecated since Android Q. Please use trim or other methods.\n");
        pin_deprecation_warn = true;
    }

    if (has_memfd_support() && !memfd_is_ashmem(fd)) {
        return 0;
    }

    // TODO: should LP64 reject too-large offset/len?
    ashmem_pin pin = { static_cast<uint32_t>(offset), static_cast<uint32_t>(len) };
    return __ashmem_check_failure(fd, TEMP_FAILURE_RETRY(ioctl(fd, ASHMEM_PIN, &pin)));
}


```

Further testing shows that this error is emitted by template apps.

* Discontinue further investigation on this error message for now *
