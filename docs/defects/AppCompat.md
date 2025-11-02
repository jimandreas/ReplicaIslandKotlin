

Yes, there is an AppCompat theme equivalent to Theme.Black.NoTitleBar.Fullscreen 
in Android. The theme Theme.AppCompat.NoTitleBar.Fullscreen provides the same 
functionality: it's a dark (black) theme based on Theme.AppCompat (the dark variant), 
with no title bar (or action bar) and fullscreen mode enabled via windowFullscreen 
set to true.


		android:theme="@android:style/Theme.AppCompat.NoTitleBar.Fullscreen"
// Optional: For additional resource handling on older Android versions
implementation "androidx.appcompat:appcompat-resources:$appcompat_version"