package dev.horizon.android;

/** A regular Android window plays the intro before NativeActivity takes over its window surface. */
public final class IntroActivity extends android.app.Activity implements android.view.SurfaceHolder.Callback {
    private android.widget.FrameLayout root;
    private android.view.SurfaceView video;
    private android.media.MediaPlayer player;
    private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
    private boolean resumed, prepared, transitioning, failed;
    private int videoWidth = 1920, videoHeight = 1080;
    private final Runnable watchdog = () -> {
        if (resumed && !transitioning) {
            android.util.Log.w("VoltIntro", "Intro timed out; opening the launcher");
            openLauncher();
        }
    };

    @Override protected void onCreate(android.os.Bundle state) {
        super.onCreate(state);
        getWindow().getDecorView().setSystemUiVisibility(
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                | android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        root = new android.widget.FrameLayout(this);
        root.setBackgroundColor(android.graphics.Color.BLACK);
        video = new android.view.SurfaceView(this);
        root.addView(video, new android.widget.FrameLayout.LayoutParams(-1, -1, android.view.Gravity.CENTER));
        setContentView(root);
        root.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, oright, ob) -> fitVideo());
        video.getHolder().addCallback(this);
    }

    private void fitVideo() {
        if (root.getWidth() <= 0 || root.getHeight() <= 0 || videoWidth <= 0 || videoHeight <= 0) return;
        float scale = Math.min((float) root.getWidth() / videoWidth, (float) root.getHeight() / videoHeight);
        int w = Math.max(1, Math.round(videoWidth * scale));
        int h = Math.max(1, Math.round(videoHeight * scale));
        android.widget.FrameLayout.LayoutParams lp = (android.widget.FrameLayout.LayoutParams) video.getLayoutParams();
        if (lp.width != w || lp.height != h) {
            lp.width = w; lp.height = h; lp.gravity = android.view.Gravity.CENTER;
            video.setLayoutParams(lp);
        }
    }

    @Override public void surfaceCreated(android.view.SurfaceHolder holder) {
        if (transitioning) return;
        releasePlayer();
        try {
            player = new android.media.MediaPlayer();
            try (android.content.res.AssetFileDescriptor fd = getAssets().openFd("intro.mp4")) {
                player.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
            }
            player.setSurface(holder.getSurface());
            player.setOnVideoSizeChangedListener((mp, width, height) -> {
                if (width > 0 && height > 0) { videoWidth = width; videoHeight = height; fitVideo(); }
            });
            player.setOnPreparedListener(mp -> { prepared = true; if (resumed && !transitioning) mp.start(); });
            player.setOnInfoListener((mp, what, extra) -> {
                if (what == android.media.MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START)
                    android.util.Log.i("VoltIntro", "Intro video rendering started");
                return false;
            });
            player.setOnCompletionListener(mp -> openLauncher());
            player.setOnErrorListener((mp, what, extra) -> {
                android.util.Log.e("VoltIntro", "Video playback failed: " + what + ", " + extra);
                failed = true;
                if (resumed) openLauncher();
                return true;
            });
            player.prepareAsync();
        } catch (Exception e) {
            android.util.Log.e("VoltIntro", "Could not open intro", e);
            failed = true;
            if (resumed) openLauncher();
        }
    }
    @Override public void surfaceChanged(android.view.SurfaceHolder holder, int format, int width, int height) { }
    @Override public void surfaceDestroyed(android.view.SurfaceHolder holder) { releasePlayer(); }

    @Override protected void onResume() {
        super.onResume();
        resumed = true;
        if (failed) { openLauncher(); return; }
        if (prepared && player != null) player.start();
        handler.removeCallbacks(watchdog);
        handler.postDelayed(watchdog, 12000);
    }
    @Override protected void onPause() {
        resumed = false;
        handler.removeCallbacks(watchdog);
        if (prepared && player != null) {
            try { if (player.isPlaying()) player.pause(); } catch (IllegalStateException ignored) { }
        }
        super.onPause();
    }
    @Override protected void onDestroy() {
        handler.removeCallbacks(watchdog);
        releasePlayer();
        super.onDestroy();
    }
    @Override public void onBackPressed() { openLauncher(); }

    private void releasePlayer() {
        prepared = false;
        if (player != null) { player.release(); player = null; }
    }
    private void openLauncher() {
        if (transitioning) return;
        transitioning = true;
        handler.removeCallbacks(watchdog);
        releasePlayer();
        startActivity(new android.content.Intent(this, HorizonActivity.class));
        overridePendingTransition(0, 0);
        finish();
    }
}
