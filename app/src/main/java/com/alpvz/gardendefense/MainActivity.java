package com.alpvz.gardendefense;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.VideoView;

public class MainActivity extends Activity {

    private GameView game;
    private SharedPreferences save;
    private FrameLayout root;
    private VideoView winVideo;

    // Âm thanh: mỗi loại chỉ có 1 player, luôn được dừng/giải phóng đúng lúc
    private MediaPlayer pfPlayer, binu1Player, binu2Player;
    boolean binu1Done = true;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        hideSystemUi();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        save = getSharedPreferences("garden_defense_save", MODE_PRIVATE);
        try {
            game = new GameView(this);
            root = new FrameLayout(this);
            root.setBackgroundColor(Color.BLACK);
            root.addView(game, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            setContentView(root);
        } catch (Throwable e) {
            game = null;
            TextView t = new TextView(this);
            t.setText("Garden Defense không thể khởi động.\n" + e);
            t.setTextColor(Color.WHITE);
            t.setGravity(Gravity.CENTER);
            t.setBackgroundColor(Color.rgb(25, 65, 30));
            setContentView(t);
        }
    }

    private void hideSystemUi() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemUi();
    }

    SharedPreferences prefs() { return save; }

    // ---------------- Plant Food sound ----------------
    void playPf() {
        stopPf();
        try {
            pfPlayer = MediaPlayer.create(this, R.raw.peashootplantfood);
            if (pfPlayer != null) {
                final MediaPlayer mine = pfPlayer;
                pfPlayer.setOnCompletionListener(mp -> {
                    try { mp.release(); } catch (Exception ignored) { }
                    if (pfPlayer == mine) pfPlayer = null;
                });
                pfPlayer.start();
            }
        } catch (Exception ignored) { pfPlayer = null; }
    }

    void stopPf() {
        MediaPlayer m = pfPlayer;
        pfPlayer = null;
        if (m == null) return;
        try { m.stop(); } catch (Exception ignored) { }
        try { m.release(); } catch (Exception ignored) { }
    }

    // ---------------- BINU sounds ----------------
    void playBinu1() {
        stopBinu1();
        binu1Done = false;
        try {
            int id = getResources().getIdentifier("binusound1", "raw", getPackageName());
            if (id == 0) { binu1Done = true; return; }
            binu1Player = MediaPlayer.create(this, id);
            if (binu1Player == null) { binu1Done = true; return; }
            try {
                if (Build.VERSION.SDK_INT >= 23)
                    binu1Player.setPlaybackParams(binu1Player.getPlaybackParams().setSpeed(1.5f));
            } catch (Exception ignored) { }
            final MediaPlayer mine = binu1Player;
            binu1Player.setOnCompletionListener(mp -> {
                binu1Done = true;
                try { mp.release(); } catch (Exception ignored) { }
                if (binu1Player == mine) binu1Player = null;
            });
            binu1Player.start();
        } catch (Exception e) { binu1Done = true; binu1Player = null; }
    }

    void playBinu2() {
        stopBinu2();
        try {
            int id = getResources().getIdentifier("binusound2", "raw", getPackageName());
            if (id == 0) return;
            binu2Player = MediaPlayer.create(this, id);
            if (binu2Player == null) return;
            final MediaPlayer mine = binu2Player;
            binu2Player.setOnCompletionListener(mp -> {
                try { mp.release(); } catch (Exception ignored) { }
                if (binu2Player == mine) binu2Player = null;
            });
            binu2Player.start();
        } catch (Exception ignored) { }
    }

    void stopBinu1() {
        MediaPlayer m = binu1Player;
        binu1Player = null;
        binu1Done = true;
        if (m == null) return;
        try { m.stop(); } catch (Exception ignored) { }
        try { m.release(); } catch (Exception ignored) { }
    }

    void stopBinu2() {
        MediaPlayer m = binu2Player;
        binu2Player = null;
        if (m == null) return;
        try { m.stop(); } catch (Exception ignored) { }
        try { m.release(); } catch (Exception ignored) { }
    }

    void stopAllSounds() {
        stopPf();
        stopBinu1();
        stopBinu2();
    }

    // ---------------- video chiến thắng ----------------
    void hideFinalVideo() {
        try {
            if (winVideo != null) {
                winVideo.stopPlayback();
                if (root != null) root.removeView(winVideo);
                winVideo = null;
            }
        } catch (Exception ignored) { }
    }

    void showFinalVideo() {
        hideFinalVideo();
        try {
            if (root == null || game == null) return;
            int id = getResources().getIdentifier("win", "raw", getPackageName());
            if (id == 0) { game.onVideoDone(); return; }
            winVideo = new VideoView(this);
            winVideo.setBackgroundColor(Color.BLACK);
            winVideo.setVideoURI(Uri.parse("android.resource://" + getPackageName() + "/" + id));
            root.addView(winVideo, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            winVideo.setOnCompletionListener(mp -> { hideFinalVideo(); if (game != null) game.onVideoDone(); });
            winVideo.setOnErrorListener((mp, what, extra) -> { hideFinalVideo(); if (game != null) game.onVideoDone(); return true; });
            winVideo.start();
        } catch (Exception ignored) {
            hideFinalVideo();
            if (game != null) game.onVideoDone();
        }
    }

    // ---------------- vòng đời ----------------
    @Override
    protected void onPause() {
        if (game != null) { game.persist(); game.onActivityPause(); }
        stopAllSounds();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        stopAllSounds();
        hideFinalVideo();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (winVideo != null) { hideFinalVideo(); if (game != null) game.onVideoDone(); return; }
        if (game == null || !game.onBack()) super.onBackPressed();
    }
}
