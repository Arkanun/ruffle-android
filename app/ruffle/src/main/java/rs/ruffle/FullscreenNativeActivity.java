package rs.ruffle;


import com.google.androidgamesdk.GameActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;


import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import android.content.pm.PackageManager;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

public class FullscreenNativeActivity extends GameActivity {

    static {
        // load the native activity
        System.loadLibrary("ruffle_android");
    }
    protected static byte[] SWF_BYTES;

    protected byte[] getSwfBytes() {
        return SWF_BYTES;
    }

    private static native void keydown(byte key_code, char key_char);
    private static native void keyup(byte key_code, char key_char);


    private static <T> List<T> gatherAllDescendantsOfType(View v, Class t) {
        List<T> result = new ArrayList<T>();
        if (t.isInstance(v))
            result.add((T)v);
        if (v instanceof  ViewGroup) {
            ViewGroup vg = (ViewGroup)v;
            for (int i = 0; i < vg.getChildCount(); ++i) {
                result.addAll(gatherAllDescendantsOfType(vg.getChildAt(i), t));
            }
        }
        return result;
    }

    @Override
    protected void onCreateSurfaceView() {
        this.mSurfaceView = new InputEnabledSurfaceView(this);

        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setLayoutDirection(LinearLayout.LAYOUT_DIRECTION_LTR);
        this.contentViewId = ViewCompat.generateViewId();
        linearLayout.setId(this.contentViewId);
        linearLayout.addView(this.mSurfaceView);

        mSurfaceView.getHolder().setFixedSize(1000, 1500);

        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.keyboard, null);

        List<Button> l = gatherAllDescendantsOfType(v, Button.class);

        Log.println(Log.WARN, "ruffle", "got " + l.size() + " buttons");
        for (Button b : l) {
            b.setOnTouchListener((View view, MotionEvent motionEvent) -> {
                String tag = (String)view.getTag();

                if (tag != null) {
                    String[] spl = tag.split(" ", 2);
                    byte by = Byte.parseByte(spl[0]);
                    char c = spl.length > 1 ? spl[1].charAt(0) : 0;

                    if (motionEvent.getAction() == MotionEvent.ACTION_DOWN)
                        keydown(by, c);
                    if (motionEvent.getAction() == MotionEvent.ACTION_UP)
                        keyup(by, c);
                }
                return false;
            });
        }
        linearLayout.addView(v);

        this.setContentView(linearLayout);

        linearLayout.requestFocus();
        this.mSurfaceView.getHolder().addCallback(this);
        ViewCompat.setOnApplyWindowInsetsListener(this.mSurfaceView, this);
    }

    private void hideSystemUI() {
        // This will put the game behind any cutouts and waterfalls on devices which have
        // them, so the corresponding insets will be non-zero.
        if (VERSION.SDK_INT >= VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode
                    = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        }
        // From API 30 onwards, this is the recommended way to hide the system UI, rather than
        // using View.setSystemUiVisibility.
        View decorView = getWindow().getDecorView();
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(),
                decorView);
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.hide(WindowInsetsCompat.Type.displayCutout());
        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);


    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // When true, the app will fit inside any system UI windows.
        // When false, we render behind any system UI windows.
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        hideSystemUI();
        // You can set IME fields here or in native code using GameActivity_setImeEditorInfoFields.
        // We set the fields in native_engine.cpp.
        // super.setImeEditorInfoFields(InputType.TYPE_CLASS_TEXT,
        //     IME_ACTION_NONE, IME_FLAG_NO_FULLSCREEN );
        requestNoStatusBarFeature();
        super.onCreate(savedInstanceState);
    }

    public boolean isGooglePlayGames() {
        PackageManager pm = getPackageManager();
        return pm.hasSystemFeature("com.google.android.play.feature.HPE_EXPERIENCE");
    }

    private void requestNoStatusBarFeature() {
        //Hiding the status bar this way makes it see through when pulled down
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }
}
