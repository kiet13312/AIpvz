package com.alpvz.gardendefense;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.media.*;
import android.view.*;
import android.widget.TextView;
import android.widget.FrameLayout;
import android.widget.VideoView;
import android.widget.EditText;
import android.widget.Toast;
import android.net.Uri;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;

public class MainActivity extends Activity {

    private GameView game;
    private MediaPlayer plantFoodPlayer;
    private SharedPreferences save;
    private FrameLayout root;
    private VideoView winVideo;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        save = getSharedPreferences("garden_defense_save", MODE_PRIVATE);
        try {
            game = new GameView();
            root = new FrameLayout(MainActivity.this);
            root.setBackgroundColor(Color.BLACK);
            root.addView(game, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            setContentView(root);
            saveGame();
        } catch (Throwable e) {
            game = null;
            TextView t = new TextView(this);
            t.setText("Garden Defense không thể khởi động.");
            t.setTextColor(Color.WHITE);
            t.setGravity(Gravity.CENTER);
            t.setBackgroundColor(Color.rgb(25, 65, 30));
            setContentView(t);
        }
    }

    private void playPlantFoodSound() {
        try {
            if (plantFoodPlayer != null) {
                plantFoodPlayer.release();
                plantFoodPlayer = null;
            }

            final MediaPlayer player = MediaPlayer.create(
                    MainActivity.this,
                    R.raw.peashootplantfood
            );

            plantFoodPlayer = player;
            if (player != null) {
                player.setOnCompletionListener(mp -> {
                    try { mp.release(); } catch (Exception ignored) {}
                    if (plantFoodPlayer == mp) {
                        plantFoodPlayer = null;
                    }
                });
                player.start();
            }
        } catch (Exception ignored) {
        }
    }

    private void showAdminCommandDialog() {
        if (game == null) return;
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setTextSize(16f);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        input.setHint("/unlock_all_maps");
        new AlertDialog.Builder(this)
                .setTitle("Lệnh ADMIN")
                .setMessage("Nhập /help để xem danh sách lệnh.")
                .setView(input)
                .setPositiveButton("CHẠY", (dialog, which) -> {
                    if (game == null) return;
                    String result = game.runAdminCommand(input.getText().toString());
                    Toast.makeText(this, result, Toast.LENGTH_LONG).show();
                    game.invalidate();
                })
                .setNegativeButton("HỦY", null)
                .show();
    }

    private void saveGame() {
        if (game == null) return;

        save.edit()
                .putInt("level", game.level)
                .putInt("unlocked", game.unlocked)
                .putInt("sun", game.sun)
                .putInt("coins", game.coins)
                .putInt("food", game.food)
                .putString("dailyDate", game.dailyDate)
                .putInt("dailyStreak", game.dailyStreak)
                .putInt("dailyWins", game.dailyWins)
                .putInt("dailySuns", game.dailySuns)
                .putInt("dailyKills", game.dailyKills)
                .putBoolean("dailyQuestClaimed", game.dailyQuestClaimed)
                .putBoolean("dailyRewardClaimed", game.dailyRewardClaimed)
                .apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (game != null) {
            game.refreshDaily();
            saveGame();
            game.invalidate();
        }
    }

    @Override
    protected void onPause() {
        saveGame();
        super.onPause();
    }

    private void hideFinalVideo() {
        try {
            if (winVideo != null) {
                winVideo.stopPlayback();
                if (root != null) root.removeView(winVideo);
                winVideo = null;
            }
        } catch (Exception ignored) {}
    }

        private void showFinalVideo(){
        hideFinalVideo();
        if(root==null||game==null)return;
        int id=getResources().getIdentifier("win","raw",getPackageName());
        if(id==0){Toast.makeText(this,"Không tìm thấy video win.mp4",Toast.LENGTH_SHORT).show();return;}
        try{
            game.screen=GameView.HOME;
            winVideo=new VideoView(MainActivity.this);
            winVideo.setBackgroundColor(Color.BLACK);
            winVideo.setZOrderOnTop(true);
            winVideo.setOnPreparedListener(mp->{
                try{mp.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT);winVideo.start();}
                catch(Exception ex){hideFinalVideo();Toast.makeText(MainActivity.this,"Không hiển thị được khung hình video",Toast.LENGTH_SHORT).show();}
            });
            winVideo.setOnCompletionListener(mp->{
                hideFinalVideo();
                if(game!=null){game.coins=Math.min(99999999,game.coins+100);saveGame();game.screen=GameView.HOME;game.invalidate();
                    Toast.makeText(MainActivity.this,"+100 xu",Toast.LENGTH_SHORT).show();}
            });
            winVideo.setOnErrorListener((mp,what,extra)->{
                hideFinalVideo();if(game!=null){game.screen=GameView.HOME;game.invalidate();}
                Toast.makeText(MainActivity.this,"Lỗi giải mã video ("+what+"/"+extra+")",Toast.LENGTH_LONG).show();return true;
            });
            root.addView(winVideo,new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,FrameLayout.LayoutParams.MATCH_PARENT));
            winVideo.setVideoURI(Uri.parse("android.resource://"+getPackageName()+"/"+id));
        }catch(Exception ex){
            hideFinalVideo();if(game!=null)game.invalidate();
            Toast.makeText(this,"Không mở được video",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        try {
            if (plantFoodPlayer != null) {
                plantFoodPlayer.release();
                plantFoodPlayer = null;
            }
            hideFinalVideo();
            if (game != null) {
                game.releaseBinuSounds();
            }
        } catch (Exception ignored) {
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (winVideo != null) { hideFinalVideo(); if(game!=null){game.screen=GameView.HOME;game.invalidate();} return; }
        if (game == null) {
            super.onBackPressed();
            return;
        }

        if (game.screen == GameView.PLAY) {
            game.screen = GameView.PAUSE;
        } else if (game.screen == GameView.PAUSE) {
            game.screen = GameView.PLAY;
        } else if (game.screen == GameView.MINIGAME_PLAY) {
            game.screen = GameView.MINIGAME_MENU;
        } else if (game.screen == GameView.MINIGAME_MENU ||
                   game.screen == GameView.MINIGAME_RESULT ||
                   game.screen == GameView.ADMIN ||
                   game.screen == GameView.LEVEL ||
                   game.screen == GameView.WORLD ||
                   game.screen == GameView.DAILY_QUEST ||
                   game.screen == GameView.DAILY_REWARDS ||
                   game.screen == GameView.WIN ||
                   game.screen == GameView.LOSE) {
            game.screen = GameView.HOME;
        } else {
            super.onBackPressed();
            return;
        }

        game.invalidate();
    }

    class GameView extends View {

        static final int ROWS = 5;
        static final int COLS = 9;

        static final int SUNFLOWER = 1;
        static final int PEASHOOTER = 2;
        static final int GIGANUT = 3;
        static final int CHOMPER = 4;
        static final int REPEATER = 5;
        static final int MINE = 6;
        static final int BINU = 7;
        static final int CABBAGE_PULT = 8;
        static final int KERNEL_PULT = 9;
        static final int MELON_PULT = 10;

        static final int HOME = 0;
        static final int LEVEL = 1;
        static final int PLAY = 2;
        static final int PAUSE = 3;
        static final int WIN = 4;
        static final int LOSE = 5;

        static final int TOOL_NONE = 0;
        static final int TOOL_SHOVEL = 1;
        static final int TOOL_PLANTFOOD = 2;

        static final int MAX_LEVEL = 200;
        static final int LEVELS_PER_WORLD = 20;
        static final int WORLD_COUNT = 10;
        static final int MINIGAME_MENU = 6;
        static final int MINIGAME_PLAY = 7;
        static final int MINIGAME_RESULT = 8;
        static final int ADMIN = 9;
        static final int DAILY_QUEST = 10;
        static final int DAILY_REWARDS = 11;
        static final int WORLD = 12;

        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint spritePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        // Isolated paint for plant art so board dimming, mower rendering and other UI draws cannot tint it.
        final Paint plantPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        final RectF spriteRect = new RectF();
        final Random random = new Random();

        final Plant[][] plants = new Plant[ROWS][COLS];
        final ArrayList<Zombie> zombies = new ArrayList<>();
        final ArrayList<Pea> peas = new ArrayList<>();
        final ArrayList<SunDrop> suns = new ArrayList<>();
        final Mower[] mowers = new Mower[ROWS];
        final int[] cardTypes = {SUNFLOWER, PEASHOOTER, GIGANUT, CHOMPER, REPEATER, MINE, BINU, CABBAGE_PULT, KERNEL_PULT, MELON_PULT};

        Bitmap sunflowerImg;
        Bitmap peashooterImg;
        Bitmap giganutImg;
        Bitmap chomperImg;
        Bitmap repeaterImg;
        Bitmap mowerImg;
        Bitmap cabbagePultImg, kernelPultImg, melonPultImg;
        Bitmap cabbagePultFoodImg, kernelPultFoodImg, melonPultFoodImg;
        Bitmap mineImg;
        Bitmap peaFoodImg;
        Bitmap repeaterFoodImg;
        Bitmap gigaFoodImg;
        Bitmap zombieImg;
        Bitmap zombieAltImg;
        Bitmap bulletImg;
        Bitmap[] peaAnim = new Bitmap[10];
        Bitmap[] peaFrames = new Bitmap[6], repeaterFrames = new Bitmap[6],
                sunflowerFrames = new Bitmap[6], chomperFrames = new Bitmap[6];
        Bitmap[] mineFrames = new Bitmap[3];
        Bitmap gardenBackground;
        Bitmap zombieNormalScaled, zombieBossScaled, zombieVinhHungScaled, bulletSmallScaled, bulletBigScaled;
        final ColorFilter zombieArmorFilter = new PorterDuffColorFilter(Color.rgb(210, 166, 104), PorterDuff.Mode.MULTIPLY);
        final ColorFilter zombieShadowFilter = new PorterDuffColorFilter(Color.rgb(155, 132, 190), PorterDuff.Mode.MULTIPLY);
        final ColorFilter zombieStunFilter = new PorterDuffColorFilter(Color.rgb(125, 205, 255), PorterDuff.Mode.MULTIPLY);
        Bitmap gigaFieldScaled, gigaCardScaled, gigaFoodFieldScaled, gigaFoodCardScaled;
        Bitmap binuImg;
        Bitmap binu1Img;
        Bitmap binu2Img;
        Bitmap binu3Img;
        Bitmap binu4Img;

        float left;
        float top;
        float cellW;
        float cellH;

        int screen = HOME;
        int level = 1;
        int unlocked = 1;
        int selectedWorld = 0;
        int worldPage = 0;

        int selected = PEASHOOTER;
        int tool = TOOL_NONE;

        int sun = 500;
        int coins = 99999;
        int food = 10000;

        int killed = 0;
        int total = 0;
        int spawned = 0;

        int speed = 1;
        boolean sandboxMode = false;
        String dailyDate = "";
        int dailyStreak = 1;
        int dailyWins = 0;
        int dailySuns = 0;
        int dailyKills = 0;
        boolean dailyQuestClaimed = false;
        boolean dailyRewardClaimed = false;

        final ArrayList<MiniTarget> miniTargets = new ArrayList<>();
        int miniMode = 1;
        int miniScore = 0;
        long miniStartedAt = 0L;
        long miniLastSpawn = 0L;
        long miniLastFrame = 0L;

        int binuRow = -1;
        int binuCol = -1;
        int binuFrame = 0;
        long binuClock = 0L;
        long binuDetectAt = 0L;
        boolean binuWaiting = false;
        boolean binuJumping = false;
        boolean binuSound1Finished = false;
        float binuX = 0f;
        float binuY = 0f;
        float binuStartX = 0f;
        float binuStartY = 0f;
        float binuTargetX = 0f;
        float binuTargetY = 0f;
        boolean binuFoodBoost = false;

        MediaPlayer binuSound1Player;
        MediaPlayer binuSound2Player;

        long lastUpdate;
        long spawnClock;
        long lastSun;
        long fpsWindowStart = System.nanoTime();
        int fpsFrames;
        int fpsLast;

        GameView() {
            super(MainActivity.this);
            setFocusable(true);

            loadImages();

            for (int r = 0; r < ROWS; r++) {
                mowers[r] = new Mower(r);
            }

            level = Math.max(1, Math.min(MAX_LEVEL, save.getInt("level", 1)));
            unlocked = Math.max(1, Math.min(MAX_LEVEL, save.getInt("unlocked", 1)));
            unlocked = Math.max(unlocked, level);
            selectedWorld = Math.min(WORLD_COUNT - 1, (level - 1) / LEVELS_PER_WORLD);
            dailyDate = save.getString("dailyDate", "");
            dailyStreak = Math.max(1, save.getInt("dailyStreak", 1));
            dailyWins = Math.max(0, save.getInt("dailyWins", 0));
            dailySuns = Math.max(0, save.getInt("dailySuns", 0));
            dailyKills = Math.max(0, save.getInt("dailyKills", 0));
            dailyQuestClaimed = save.getBoolean("dailyQuestClaimed", false);
            dailyRewardClaimed = save.getBoolean("dailyRewardClaimed", false);
            refreshDaily();
            sun = 500;
            coins = Math.max(0, save.getInt("coins", 0));
            food = Math.max(10000, save.getInt("food", 10000));

            lastUpdate = System.currentTimeMillis();
            spawnClock = lastUpdate;
            lastSun = lastUpdate;
        }

        private Bitmap loadImage(String name) {
            int id = getResources().getIdentifier(
                    name,
                    "drawable",
                    getPackageName()
            );

            if (id == 0) return null;

            try {
                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inPreferredConfig = Bitmap.Config.RGB_565;
                o.inScaled = true;
                return BitmapFactory.decodeResource(getResources(), id, o);
            } catch (Throwable e) {
                return null;
            }
        }
        private Bitmap loadVectorImage(String name, int width, int height) {
            int id = getResources().getIdentifier(name, "drawable", getPackageName());
            if (id == 0) return null;
            try {
                Drawable drawable = getResources().getDrawable(id);
                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                drawable.setBounds(0, 0, width, height);
                drawable.draw(canvas);
                return bitmap;
            } catch (Throwable ignored) {
                return null;
            }
        }

        private Bitmap loadBase64Bitmap(String name) {
            int id = getResources().getIdentifier(name, "raw", getPackageName());
            if (id == 0) return null;
            try (InputStream in = getResources().openRawResource(id);
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buf = new byte[2048]; int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                byte[] data = Base64.decode(out.toByteArray(), Base64.DEFAULT);
                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inPreferredConfig = Bitmap.Config.ARGB_8888; o.inScaled = false;
                return BitmapFactory.decodeByteArray(data, 0, data.length, o);
            } catch (Throwable ignored) { return null; }
        }

        private Bitmap[] loadFrames(String name, int count) {
            Bitmap strip = loadBase64Bitmap(name);
            Bitmap[] frames = new Bitmap[count];
            if (strip == null || count <= 0) return frames;
            int fw = strip.getWidth() / count;
            if (fw <= 0) return frames;
            for (int i = 0; i < count; i++)
                frames[i] = Bitmap.createBitmap(strip, i * fw, 0, fw, strip.getHeight());
            strip.recycle();
            return frames;
        }

        private Bitmap[] makeRepeaterFrames(Bitmap[] source) {
            Bitmap[] out = new Bitmap[6];
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
            for (int i = 0; i < 6; i++) {
                Bitmap frame = source != null && i < source.length ? source[i] : null;
                if (frame == null) continue;
                Bitmap twin = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888);
                Canvas cv = new Canvas(twin);
                cv.drawBitmap(frame, null, new RectF(0, 5, 29, 45), paint);
                cv.drawBitmap(frame, null, new RectF(19, 5, 48, 45), paint);
                out[i] = twin;
            }
            return out;
        }

        private void loadImages() {
            Bitmap oldSun = loadImage("sun"), oldPea = loadImage("peashoot");
            Bitmap oldChomper = loadImage("chomper"), oldRepeater = loadImage("repeater");
            Bitmap oldMine = loadImage("min");
            peaFrames = loadFrames("plant_pea", 6);
            repeaterFrames = makeRepeaterFrames(peaFrames);
            sunflowerFrames = loadFrames("plant_sun", 6);
            chomperFrames = loadFrames("plant_chomper", 6);
            mineFrames = loadFrames("plant_mine", 3);
            gardenBackground = loadBase64Bitmap("garden_bg");
            sunflowerImg = sunflowerFrames[0] != null ? sunflowerFrames[0] : oldSun;
            peashooterImg = peaFrames[0] != null ? peaFrames[0] : oldPea;
            chomperImg = chomperFrames[0] != null ? chomperFrames[0] : oldChomper;
            cabbagePultImg = loadVectorImage("cabbage_pult", 96, 96);
            kernelPultImg = loadVectorImage("kernel_pult", 96, 96);
            melonPultImg = loadVectorImage("melon_pult", 96, 96);
            cabbagePultFoodImg = loadVectorImage("cabbage_pult_food", 96, 96);
            kernelPultFoodImg = loadVectorImage("kernel_pult_food", 96, 96);
            melonPultFoodImg = loadVectorImage("melon_pult_food", 96, 96);
            repeaterImg = loadVectorImage("repeater_new", 96, 96);
            if (repeaterImg == null) repeaterImg = oldRepeater;
            mowerImg = loadVectorImage("lawn_mower", 160, 96);
            mineImg = mineFrames[0] != null ? mineFrames[0] : oldMine;
            peaFoodImg = loadImage("peashootplantfood");
            repeaterFoodImg = loadImage("repeaterplantfood");
            giganutImg = loadImage("giganut");
            gigaFoodImg = loadImage("giganutplantfood");
            zombieImg = loadImage("zomplatz");
            zombieAltImg = loadImage("zomvinhhung");
            bulletImg = loadImage("gigapea");
            binuImg = loadImage("binu"); binu1Img = loadImage("binu1");
            binu2Img = loadImage("binu2"); binu3Img = loadImage("binu3"); binu4Img = loadImage("binu4");
            loadDownloadedPultArtwork();
        }


        private Bitmap loadDownloadedPng(String name) {
            int id = getResources().getIdentifier(name, "drawable", getPackageName());
            if (id == 0) return null;
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                options.inScaled = false;
                return BitmapFactory.decodeResource(getResources(), id, options);
            } catch (Throwable ignored) {
                return null;
            }
        }

        private Bitmap cropArtwork(Bitmap sheet, float leftPart, float topPart, float rightPart, float bottomPart) {
            if (sheet == null || sheet.isRecycled()) return null;
            int leftPx = Math.max(0, Math.min(sheet.getWidth() - 1, Math.round(sheet.getWidth() * leftPart)));
            int topPx = Math.max(0, Math.min(sheet.getHeight() - 1, Math.round(sheet.getHeight() * topPart)));
            int rightPx = Math.max(leftPx + 1, Math.min(sheet.getWidth(), Math.round(sheet.getWidth() * rightPart)));
            int bottomPx = Math.max(topPx + 1, Math.min(sheet.getHeight(), Math.round(sheet.getHeight() * bottomPart)));
            try {
                return Bitmap.createBitmap(sheet, leftPx, topPx, rightPx - leftPx, bottomPx - topPx);
            } catch (Throwable ignored) {
                return null;
            }
        }

        private Bitmap makePlantFoodArtwork(Bitmap normal) {
            if (normal == null) return null;
            try {
                Bitmap foodArt = Bitmap.createBitmap(normal.getWidth(), normal.getHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(foodArt);
                Paint tint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
                tint.setColorFilter(new LightingColorFilter(Color.rgb(255, 255, 175), 0));
                canvas.drawBitmap(normal, 0f, 0f, tint);
                Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
                glow.setColor(Color.argb(70, 255, 215, 70));
                canvas.drawCircle(foodArt.getWidth() * .5f, foodArt.getHeight() * .5f,
                        Math.min(foodArt.getWidth(), foodArt.getHeight()) * .48f, glow);
                canvas.drawBitmap(normal, 0f, 0f, tint);
                return foodArt;
            } catch (Throwable ignored) {
                return normal;
            }
        }

        private void loadDownloadedPultArtwork() {
            Bitmap vegetables = loadDownloadedPng("pult_vegetables_sheet");
            Bitmap cereals = loadDownloadedPng("pult_cereals_sheet");
            Bitmap fruits = loadDownloadedPng("pult_fruits_sheet");

            // ScratchIO's CC0 vegetable sheet: cabbage is the leafy round sprite in the lower-right area.
            Bitmap cabbage = cropArtwork(vegetables, .60f, .53f, .96f, .80f);
            // The CC0 cereal sheet is five 32px cereal sprites; the first slot is corn.
            Bitmap corn = null;
            if (cereals != null) {
                if (cereals.getWidth() >= cereals.getHeight()) {
                    corn = cropArtwork(cereals, 0f, 0f, .20f, 1f);
                } else {
                    corn = cropArtwork(cereals, 0f, 0f, 1f, .20f);
                }
            }
            // ScratchIO's CC0 fruit sheet: crop the whole watermelon from its lower-left fruit slot.
            Bitmap melon = cropArtwork(fruits, .05f, .50f, .43f, .79f);

            if (cabbage != null) {
                cabbagePultImg = cabbage;
                cabbagePultFoodImg = makePlantFoodArtwork(cabbage);
            }
            if (corn != null) {
                kernelPultImg = corn;
                kernelPultFoodImg = makePlantFoodArtwork(corn);
            }
            if (melon != null) {
                melonPultImg = melon;
                melonPultFoodImg = makePlantFoodArtwork(melon);
            }

            if (vegetables != null && !vegetables.isRecycled()) vegetables.recycle();
            if (cereals != null && !cereals.isRecycled()) cereals.recycle();
            if (fruits != null && !fruits.isRecycled()) fruits.recycle();
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldW, int oldH) {
            // The lawn in garden_bg.webp starts at x=55 of a 384px image.
            // Match the 9x5 logical grid to that exact visible lawn rectangle.
            left = w * (55f / 384f);
            top = h * 0.25f;
            cellW = (w - left) / COLS;
            cellH = (h * 0.70f) / ROWS;
            float plantSize = Math.max(1f, Math.min(cellW, cellH));
            zombieNormalScaled = scaleBitmap(zombieImg, cellW * .68f, cellH * .82f);
            zombieBossScaled = scaleBitmap(zombieImg, cellW * 1.20f, cellH * 1.20f);
            zombieVinhHungScaled = scaleBitmap(zombieAltImg, cellW * .72f, cellH * .86f);
            bulletSmallScaled = scaleBitmap(bulletImg, 20f, 20f);
            bulletBigScaled = scaleBitmap(bulletImg, 36f, 36f);
            gigaFieldScaled = scaleBitmap(giganutImg, plantSize * .74f, plantSize * .74f);
            gigaCardScaled = scaleBitmap(giganutImg, 44f, 44f);
            gigaFoodFieldScaled = scaleBitmap(gigaFoodImg, plantSize * .74f, plantSize * .74f);
            gigaFoodCardScaled = scaleBitmap(gigaFoodImg, 44f, 44f);
            for (int r = 0; r < ROWS; r++) {
                if (mowers[r] != null && !mowers[r].active) mowers[r].x = left - cellW * .30f;
            }
        }

        private Bitmap scaleBitmap(Bitmap src, float width, float height) {
            if (src == null || width <= 0f || height <= 0f) return null;
            try {
                return Bitmap.createScaledBitmap(src, Math.max(1, Math.round(width)),
                        Math.max(1, Math.round(height)), true);
            } catch (Throwable ignored) {
                return src;
            }
        }

        @Override
        protected void onDraw(Canvas c) {
            super.onDraw(c);
            recordFrameRate();

            if (screen == HOME) {
                drawHome(c);
                return;
            }

            if (screen == WORLD) {
                drawWorlds(c);
                return;
            }
            if (screen == LEVEL) {
                drawLevels(c);
                return;
            }
            if (screen == DAILY_QUEST) {
                drawDailyQuest(c);
                return;
            }
            if (screen == DAILY_REWARDS) {
                drawDailyRewards(c);
                return;
            }

            if (screen == MINIGAME_MENU) {
                drawMiniGameMenu(c);
                return;
            }

            if (screen == MINIGAME_PLAY) {
                drawMiniGame(c);
                updateMiniGame();
                postInvalidateOnAnimation();
                return;
            }

            if (screen == MINIGAME_RESULT) {
                drawMiniGameResult(c);
                return;
            }

            if (screen == ADMIN) {
                drawAdmin(c);
                return;
            }

            drawGame(c);

            if (screen == PAUSE) {
                drawOverlay(c, "TẠM DỪNG", "TIẾP TỤC", "CHƠI LẠI", "THOÁT");
            } else if (screen == WIN) {
                drawOverlay(c, "CHIẾN THẮNG!", "MÀN TIẾP", "CHƠI LẠI", "VỀ MENU");
            } else if (screen == LOSE) {
                drawOverlay(c, "ZOMBIE ĐÃ VÀO NHÀ!", "CHƠI LẠI", "", "VỀ MENU");
            }

            if (screen == PLAY) {
                updateGame();
                postInvalidateOnAnimation();
            }
        }

        private void recordFrameRate() {
            long now = System.nanoTime();
            fpsFrames++;
            long elapsed = now - fpsWindowStart;
            if (elapsed >= 1_000_000_000L) {
                fpsLast = Math.round(fpsFrames * 1_000_000_000f / elapsed);
                fpsFrames = 0;
                fpsWindowStart = now;
            }
        }

                        private void drawHome(Canvas c) {
            drawBackdrop(c,18);
            float w=getWidth(),h=getHeight();
            p.setColor(Color.argb(230,35,53,27));c.drawRoundRect(w*.012f,h*.018f,w*.30f,h*.15f,18,18,p);
            p.setColor(Color.rgb(255,211,97));c.drawCircle(w*.065f,h*.083f,h*.055f,p);
            drawText(c,"GARDEN DEFENSE",w*.125f,h*.065f,19,Color.WHITE,Paint.Align.LEFT);
            drawText(c,"Cấp "+Math.max(1,(unlocked+3)/4)+"  •  "+unlocked+"/"+MAX_LEVEL+" màn",w*.125f,h*.113f,13,Color.rgb(235,245,198),Paint.Align.LEFT);
            drawText(c,"☀ "+sun,w*.36f,h*.083f,19,Color.YELLOW,Paint.Align.CENTER);
            drawText(c,"🪙 "+coins,w*.49f,h*.083f,19,Color.rgb(255,214,74),Paint.Align.CENTER);
            drawText(c,"PF "+food,w*.61f,h*.083f,17,Color.WHITE,Paint.Align.CENTER);
            button(c,.70f,.025f,.79f,.135f,"NHIỆM VỤ");
            button(c,.805f,.025f,.895f,.135f,"QUÀ NGÀY");
            button(c,.91f,.025f,.99f,.135f,"ADMIN");
            button(c,.012f,.22f,.135f,.315f,"CHIẾN DỊCH");
            button(c,.012f,.335f,.135f,.43f,"MINI GAME");
            button(c,.012f,.45f,.135f,.545f,"SANDBOX");
            button(c,.012f,.565f,.135f,.66f,"CỬA HÀNG");
            button(c,.012f,.68f,.135f,.775f,"XEM VIDEO");
            button(c,.012f,.795f,.135f,.89f,"THÀNH TÍCH");
            drawText(c,"CHỌN THẾ GIỚI",w*.47f,h*.205f,24,Color.WHITE,Paint.Align.CENTER);
            int[] colors={Color.rgb(82,151,61),Color.rgb(190,143,60),Color.rgb(48,142,165),Color.rgb(77,71,117)};
            int start=worldPage*4;
            for(int i=0;i<4;i++){
                int world=start+i;if(world>=WORLD_COUNT)break;
                float x=.17f+i*.16f,y=.25f,right=x+.145f,bottom=.57f;
                int first=world*LEVELS_PER_WORLD+1;boolean open=unlocked>=first;
                p.setColor(Color.argb(232,28,37,30));c.drawRoundRect(w*x,h*y,w*right,h*bottom,18,18,p);
                p.setColor(colors[world%colors.length]);c.drawRoundRect(w*(x+.008f),h*(y+.012f),w*(right-.008f),h*(y+.19f),14,14,p);
                drawText(c,"WORLD "+(world+1),w*(x+.0725f),h*(y+.075f),15,Color.WHITE,Paint.Align.CENTER);
                drawText(c,worldName(first),w*(x+.0725f),h*(y+.145f),12,Color.WHITE,Paint.Align.CENTER);
                drawText(c,open?"MỞ":"KHÓA",w*(x+.0725f),h*(y+.235f),14,open?Color.rgb(255,222,100):Color.LTGRAY,Paint.Align.CENTER);
                drawText(c,Math.max(0,Math.min(20,unlocked-first+1))+"/20",w*(x+.0725f),h*(y+.285f),13,Color.WHITE,Paint.Align.CENTER);
                if(world==selectedWorld){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);p.setColor(Color.YELLOW);
                    c.drawRoundRect(w*x,h*y,w*right,h*bottom,18,18,p);p.setStyle(Paint.Style.FILL);}
            }
            button(c,.17f,.59f,.22f,.66f,"◀");button(c,.72f,.59f,.77f,.66f,"▶");
            drawText(c,"Thế giới "+(start+1)+"–"+Math.min(WORLD_COUNT,start+4)+" / "+WORLD_COUNT,w*.47f,h*.635f,12,Color.WHITE,Paint.Align.CENTER);
            p.setColor(Color.argb(225,45,35,23));c.drawRoundRect(w*.80f,h*.18f,w*.99f,h*.63f,16,16,p);
            p.setColor(Color.rgb(230,183,91));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);c.drawRoundRect(w*.80f,h*.18f,w*.99f,h*.63f,16,16,p);p.setStyle(Paint.Style.FILL);
            drawText(c,"NHIỆM VỤ NGÀY",w*.895f,h*.235f,16,Color.YELLOW,Paint.Align.CENTER);
            drawText(c,"Vượt 1 màn  ("+dailyWins+"/1)",w*.895f,h*.32f,12,Color.WHITE,Paint.Align.CENTER);
            drawText(c,"Nhặt 500 SUN  ("+Math.min(500,dailySuns)+"/500)",w*.895f,h*.405f,12,Color.WHITE,Paint.Align.CENTER);
            drawText(c,"Hạ 20 zombie  ("+Math.min(20,dailyKills)+"/20)",w*.895f,h*.49f,12,Color.WHITE,Paint.Align.CENTER);
            button(c,.83f,.535f,.96f,.605f,"XEM NHIỆM VỤ");
            button(c,.18f,.72f,.34f,.90f,"CHIẾN DỊCH");
            button(c,.36f,.72f,.52f,.90f,"MINI GAME");
            button(c,.54f,.72f,.70f,.90f,"SANDBOX");
            button(c,.72f,.72f,.88f,.90f,"QUÀ HẰNG NGÀY");
            drawText(c,"10 thế giới • 200 bản đồ • Boss mỗi 5 màn",w*.47f,h*.965f,12,Color.WHITE,Paint.Align.CENTER);
        }

        private void drawWorlds(Canvas c) {
            drawBackdrop(c,22);
            drawText(c,"BẢN ĐỒ THẾ GIỚI",getWidth()/2f,getHeight()*.095f,30,Color.WHITE,Paint.Align.CENTER);
            drawText(c,"10 thế giới • 20 màn mỗi thế giới • Tổng 200 màn",getWidth()/2f,getHeight()*.15f,15,Color.YELLOW,Paint.Align.CENTER);
            int[] colors={Color.rgb(82,151,61),Color.rgb(190,143,60),Color.rgb(48,142,165),Color.rgb(77,71,117),
                    Color.rgb(155,190,218),Color.rgb(66,145,160),Color.rgb(116,78,150),Color.rgb(89,133,65),
                    Color.rgb(85,58,100),Color.rgb(48,68,120)};
            for(int world=0;world<WORLD_COUNT;world++){
                int col=world%5,row=world/5;float x=.035f+col*.19f,y=.23f+row*.34f;
                float right=x+.17f,bottom=y+.27f;int first=world*LEVELS_PER_WORLD+1;boolean open=unlocked>=first;
                p.setColor(open?Color.argb(235,35,45,29):Color.argb(220,40,40,40));
                c.drawRoundRect(getWidth()*x,getHeight()*y,getWidth()*right,getHeight()*bottom,18,18,p);
                p.setColor(colors[world]);c.drawRoundRect(getWidth()*(x+.006f),getHeight()*(y+.006f),
                        getWidth()*(right-.006f),getHeight()*(y+.13f),14,14,p);
                drawText(c,"THẾ GIỚI "+(world+1),getWidth()*(x+.085f),getHeight()*(y+.06f),15,Color.WHITE,Paint.Align.CENTER);
                drawText(c,worldName(first),getWidth()*(x+.085f),getHeight()*(y+.185f),13,Color.WHITE,Paint.Align.CENTER);
                drawText(c,open?("MÀN "+first+"–"+(first+19)):"CHƯA MỞ KHÓA",
                        getWidth()*(x+.085f),getHeight()*(y+.235f),11,open?Color.YELLOW:Color.LTGRAY,Paint.Align.CENTER);
                if(world==selectedWorld){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);p.setColor(Color.YELLOW);
                    c.drawRoundRect(getWidth()*x,getHeight()*y,getWidth()*right,getHeight()*bottom,18,18,p);p.setStyle(Paint.Style.FILL);}
            }
            button(c,.035f,.90f,.18f,.98f,"VỀ SẢNH");
        }

                private void drawLevels(Canvas c) {
            drawBackdrop(c,30);
            int first=selectedWorld*LEVELS_PER_WORLD+1;
            drawText(c,"THẾ GIỚI "+(selectedWorld+1)+" • "+worldName(first),getWidth()/2f,getHeight()*.095f,27,Color.WHITE,Paint.Align.CENTER);
            drawText(c,"MÀN ĐÃ MỞ: "+Math.max(0,Math.min(20,unlocked-first+1))+"/20",getWidth()/2f,getHeight()*.15f,15,Color.YELLOW,Paint.Align.CENTER);
            for(int local=1;local<=LEVELS_PER_WORLD;local++){
                int number=first+local-1,col=(local-1)%5,row=(local-1)/5;
                float x=.07f+col*.18f,y=.21f+row*.16f;
                boolean open=number<=unlocked,boss=isBossLevel(number);
                p.setColor(!open?Color.rgb(64,68,63):boss?Color.rgb(167,92,31):Color.rgb(53,135,67));
                c.drawRoundRect(getWidth()*x,getHeight()*y,getWidth()*(x+.15f),getHeight()*(y+.115f),12,12,p);
                if(open){
                    drawText(c,boss?"BOSS "+local:"MÀN "+local,getWidth()*(x+.075f),getHeight()*(y+.048f),16,Color.WHITE,Paint.Align.CENTER);
                    drawText(c,"MAP "+number,getWidth()*(x+.075f),getHeight()*(y+.088f),10,Color.rgb(255,238,180),Paint.Align.CENTER);
                }else drawText(c,"🔒",getWidth()*(x+.075f),getHeight()*(y+.076f),18,Color.LTGRAY,Paint.Align.CENTER);
            }
            button(c,.035f,.88f,.18f,.97f,"THẾ GIỚI");button(c,.82f,.88f,.965f,.97f,"VỀ SẢNH");
        }

                private String worldName(int levelNumber) {
            String[] names={"KHU VƯỜN NHÀ","AI CẬP CỔ ĐẠI","BIỂN CƯỚP BIỂN","MIỀN TÂY HOANG DÃ",
                    "KỶ BĂNG HÀ","THÀNH PHỐ TƯƠNG LAI","ĐÊM NEON","RỪNG KHỦNG LONG","ĐẢO BÓNG TỐI","VŨ TRỤ HỖN LOẠN"};
            int index=Math.max(0,Math.min(WORLD_COUNT-1,(levelNumber-1)/LEVELS_PER_WORLD));
            return names[index];
        }

                private boolean isBossLevel(int lv) { return lv > 0 && lv % 5 == 0; }

                private void drawGame(Canvas c){
            c.drawColor(Color.rgb(92,155,70));
            p.setColor(Color.rgb(77,45,25));c.drawRect(0,0,getWidth(),top,p);
            p.setColor(Color.rgb(137,83,39));c.drawRect(0,0,getWidth(),top*.24f,p);
            p.setColor(Color.rgb(231,183,89));c.drawRect(0,top-2,getWidth(),top,p);
            drawText(c,sandboxMode?"☀ ∞":"☀ "+sun,14,getHeight()*.055f,16,Color.YELLOW,Paint.Align.LEFT);
            drawText(c,sandboxMode?"SANDBOX • THỬ CÂY":(isBossLevel(level)?"MÀN "+level+" • BOSS":"MÀN "+level),
                    getWidth()*.22f,getHeight()*.055f,15,Color.WHITE,Paint.Align.LEFT);
            drawText(c,sandboxMode?"ZOMBIE ∞ HP":"ZOM "+killed+"/"+total,getWidth()*.39f,getHeight()*.055f,15,Color.WHITE,Paint.Align.LEFT);
            drawText(c,"🪙 "+coins,getWidth()*.56f,getHeight()*.055f,15,Color.YELLOW,Paint.Align.LEFT);
            drawText(c,"PF "+food,getWidth()*.72f,getHeight()*.055f,15,Color.WHITE,Paint.Align.LEFT);
            drawText(c,fpsLast+" FPS",getWidth()*.97f,getHeight()*.055f,13,Color.WHITE,Paint.Align.RIGHT);
            drawCards(c);drawBoard(c);drawPlants(c);drawBinu(c);drawPeas(c);drawZombies(c);drawSuns(c);drawMowers(c);
            button(c,.94f,.105f,.99f,.21f,speed==2?"×2":"▶");
            button(c,.88f,.105f,.93f,.21f,"Ⅱ");
            drawProgress(c);
        }

                private void drawProgress(Canvas c){
            float x=getWidth()*.18f,y=getHeight()*.225f,w=getWidth()*.64f;
            if(sandboxMode){drawText(c,"ZOMBIE KIỂM TRA • ĐỨNG YÊN • SUN VÔ HẠN",getWidth()*.50f,y+8f,11,Color.WHITE,Paint.Align.CENTER);return;}
            p.setColor(Color.DKGRAY);c.drawRoundRect(x,y,x+w,y+4,8,8,p);
            float progress=total<=0?0:killed/(float)total;
            p.setColor(Color.GREEN);c.drawRoundRect(x,y,x+w*Math.min(1f,progress),y+4,8,8,p);
        }

        private void drawCards(Canvas c) {
            for(int i=0;i<cardTypes.length;i++){
                int type=cardTypes[i];
                float x=getWidth()*(.01f+i*.052f),y=getHeight()*.105f;
                float w=getWidth()*.047f,h=getHeight()*.105f;
                p.setColor(Color.rgb(83,49,28));c.drawRoundRect(x,y,x+w,y+h,8,8,p);
                p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2f);
                p.setColor(selected==type&&tool==TOOL_NONE?Color.YELLOW:Color.rgb(221,179,99));
                c.drawRoundRect(x,y,x+w,y+h,8,8,p);p.setStyle(Paint.Style.FILL);
                p.setColor(Color.rgb(225,203,151));c.drawRoundRect(x+2,y+2,x+w-2,y+h-2,6,6,p);
                if(isPlantUnlocked(type))drawPlant(c,type,x+w/2f,y+h/2f,Math.min(w,h)*.72f);
                else drawText(c,"🔒",x+w/2f,y+h*.62f,18,Color.DKGRAY,Paint.Align.CENTER);
            }
            float y=getHeight()*.105f, bottom=getHeight()*.21f;
            p.setColor(tool==TOOL_SHOVEL?Color.YELLOW:Color.rgb(91,54,30));
            c.drawRoundRect(getWidth()*.82f,y,getWidth()*.87f,bottom,8,8,p);
            p.setStyle(Paint.Style.STROKE);p.setColor(Color.rgb(232,184,91));
            c.drawRoundRect(getWidth()*.82f,y,getWidth()*.87f,bottom,8,8,p);p.setStyle(Paint.Style.FILL);
            drawText(c,"XẺNG",getWidth()*.845f,getHeight()*.168f,12,Color.WHITE,Paint.Align.CENTER);
            p.setColor(tool==TOOL_PLANTFOOD?Color.YELLOW:Color.rgb(91,54,30));
            c.drawRoundRect(getWidth()*.535f,y,getWidth()*.63f,bottom,8,8,p);
            p.setStyle(Paint.Style.STROKE);p.setColor(Color.rgb(232,184,91));
            c.drawRoundRect(getWidth()*.535f,y,getWidth()*.63f,bottom,8,8,p);p.setStyle(Paint.Style.FILL);
            drawText(c,"PF "+food,getWidth()*.582f,getHeight()*.168f,12,Color.WHITE,Paint.Align.CENTER);
            p.setColor(Color.rgb(91,54,30));
            c.drawRoundRect(getWidth()*.64f,y,getWidth()*.81f,bottom,8,8,p);
            p.setStyle(Paint.Style.STROKE);p.setColor(Color.rgb(232,184,91));
            c.drawRoundRect(getWidth()*.64f,y,getWidth()*.81f,bottom,8,8,p);p.setStyle(Paint.Style.FILL);
            drawText(c,"MUA PF",getWidth()*.725f,getHeight()*.15f,11,Color.WHITE,Paint.Align.CENTER);
            drawText(c,"100 xu",getWidth()*.725f,getHeight()*.185f,10,Color.YELLOW,Paint.Align.CENTER);
        }

                private void drawBoard(Canvas c) {
            int world=(level-1)/LEVELS_PER_WORLD;
            if(gardenBackground!=null&&world==0){
                spriteRect.set(0,top,getWidth(),top+ROWS*cellH);
                spritePaint.setAlpha(255);spritePaint.setColorFilter(null);
                c.drawBitmap(gardenBackground,null,spriteRect,spritePaint);
                for(int row=0;row<ROWS;row++)if(!activeRow(row)){
                    p.setColor(Color.argb(80,30,35,30));c.drawRect(left,top+row*cellH,left+COLS*cellW,top+(row+1)*cellH,p);
                }
                return;
            }
            int[] light={Color.rgb(111,177,78),Color.rgb(207,166,85),Color.rgb(79,173,190),Color.rgb(123,92,146),
                    Color.rgb(207,227,237),Color.rgb(93,166,175),Color.rgb(106,78,133),Color.rgb(112,159,75),
                    Color.rgb(80,63,104),Color.rgb(64,87,145)};
            int[] dark={Color.rgb(96,160,68),Color.rgb(184,139,61),Color.rgb(60,145,167),Color.rgb(89,66,116),
                    Color.rgb(178,201,218),Color.rgb(65,134,153),Color.rgb(75,55,105),Color.rgb(86,131,62),
                    Color.rgb(57,47,80),Color.rgb(46,68,119)};
            for(int row=0;row<ROWS;row++)for(int col=0;col<COLS;col++){
                p.setColor(!activeRow(row)?Color.rgb(48,60,48):((row+col)%2==0?light[world]:dark[world]));
                c.drawRect(left+col*cellW,top+row*cellH,left+(col+1)*cellW,top+(row+1)*cellH,p);
            }
        }

                private void drawPlants(Canvas c) {
            for (int row = 0; row < ROWS; row++) {
                for (int col = 0; col < COLS; col++) {
                    Plant plant = plants[row][col];
                    if (plant == null) continue;
                    if (plant.type == BINU && (binuWaiting || binuJumping)) continue;
                    // Plant art, HP bar and tap target all share the exact cell center.
                    float x = left + (col + 0.5f) * cellW;
                    float y = top + (row + 0.5f) * cellH;
                    drawPlant(c, plant.type, x, y, Math.min(cellW, cellH) * .74f, plant);
                    drawHp(c, x - cellW * .30f, y + cellH * .32f,
                            cellW * .60f, plant.hp, plant.maxHp);
                }
            }
        }

        private void drawPlant(
                Canvas c,
                int type,
                float x,
                float y,
                float size
        ) {
            drawPlant(c, type, x, y, size, null);
        }

                        private void drawPlantBitmap(Canvas c, Bitmap bitmap, RectF bounds) {
            drawPlantBitmap(c, bitmap, bounds, false);
        }

        private void drawPlantBitmap(Canvas c, Bitmap bitmap, RectF bounds, boolean foodTint) {
            // Keep plant rendering isolated from the shared board and sprite paints.
            plantPaint.setColor(Color.WHITE);
            plantPaint.setAlpha(255);
            plantPaint.setColorFilter(foodTint
                    ? new LightingColorFilter(Color.rgb(255, 255, 175), 0) : null);
            plantPaint.setShader(null);
            plantPaint.setXfermode(null);
            plantPaint.setStyle(Paint.Style.FILL);
            plantPaint.setFilterBitmap(true);
            plantPaint.setDither(true);
            c.drawBitmap(bitmap, null, bounds, plantPaint);
            plantPaint.setColorFilter(null);
        }

        private void drawPlant(Canvas c, int type, float x, float y, float size, Plant plant) {
            Bitmap bitmap = null;
            long now = System.currentTimeMillis();
            boolean foodActive = plant != null && plant.foodUsed &&
                    (type == GIGANUT || now < plant.plantFoodUntil || plant.foodShotsRemaining > 0);
            boolean tintFood = false;

            if (type == GIGANUT) {
                boolean card = size < Math.min(cellW, cellH) * .55f;
                bitmap = foodActive
                        ? (card && gigaFoodCardScaled != null ? gigaFoodCardScaled : gigaFoodFieldScaled)
                        : (card && gigaCardScaled != null ? gigaCardScaled : gigaFieldScaled);
                if (bitmap == null) bitmap = foodActive ? gigaFoodImg : giganutImg;
            } else if (type == BINU) {
                bitmap = binuImg;
                tintFood = foodActive;
            } else if (type == SUNFLOWER) {
                bitmap = sunflowerImg;
                tintFood = foodActive;
            } else if (type == PEASHOOTER) {
                if (foodActive && peaFoodImg != null) {
                    bitmap = peaFoodImg;
                } else {
                    int frame = plant != null && now < plant.shootUntil
                            ? Math.max(1, Math.min(4, plant.animFrame)) : 0;
                    if (peaFrames != null && frame < peaFrames.length) bitmap = peaFrames[frame];
                    if (bitmap == null) bitmap = peashooterImg;
                }
                tintFood = foodActive && peaFoodImg == null;
            } else if (type == REPEATER) {
                if (foodActive && repeaterFoodImg != null) {
                    bitmap = repeaterFoodImg;
                } else if (plant != null && now < plant.shootUntil &&
                        repeaterFrames != null && plant.animFrame < repeaterFrames.length &&
                        repeaterFrames[Math.max(0, plant.animFrame)] != null) {
                    bitmap = repeaterFrames[Math.max(0, plant.animFrame)];
                } else {
                    bitmap = repeaterImg;
                }
                tintFood = foodActive && repeaterFoodImg == null;
            } else if (type == CABBAGE_PULT) {
                bitmap = foodActive && cabbagePultFoodImg != null ? cabbagePultFoodImg : cabbagePultImg;
                tintFood = foodActive && cabbagePultFoodImg == null;
            } else if (type == KERNEL_PULT) {
                bitmap = foodActive && kernelPultFoodImg != null ? kernelPultFoodImg : kernelPultImg;
                tintFood = foodActive && kernelPultFoodImg == null;
            } else if (type == MELON_PULT) {
                bitmap = foodActive && melonPultFoodImg != null ? melonPultFoodImg : melonPultImg;
                tintFood = foodActive && melonPultFoodImg == null;
            } else if (type == CHOMPER || type == MINE) {
                Bitmap[] frames = type == CHOMPER ? chomperFrames : mineFrames;
                int frame = 0;
                if (plant != null) {
                    if (type == CHOMPER && now < plant.shootUntil)
                        frame = Math.max(1, Math.min(5, plant.animFrame));
                    else if (type == MINE) frame = plant.armed ? 1 : 0;
                }
                if (frames != null && frame < frames.length) bitmap = frames[frame];
                if (bitmap == null) bitmap = type == CHOMPER ? chomperImg : mineImg;
                tintFood = foodActive;
            }

            if (bitmap != null) {
                float pulse = 0f;
                boolean attacking = plant != null && now < plant.shootUntil;
                if (attacking) {
                    long duration = Math.max(1L, plant.shootUntil - plant.animClock);
                    float progress = Math.max(0f, Math.min(1f,
                            (now - plant.animClock) / (float) duration));
                    pulse = (float) Math.sin(progress * Math.PI);
                }
                float kick = 1f;
                float angle = 0f;
                if (type == PEASHOOTER || type == REPEATER || type == CHOMPER) {
                    kick += pulse * .075f;
                    angle = -3.5f * pulse;
                } else if (type == CABBAGE_PULT || type == KERNEL_PULT || type == MELON_PULT) {
                    kick += pulse * .035f;
                    angle = -17f * pulse;
                } else if (type == SUNFLOWER && foodActive) {
                    kick += .025f * (float) Math.sin(now / 90.0);
                }

                spriteRect.set(x - size * kick / 2f, y - size / kick / 2f,
                        x + size * kick / 2f, y + size / kick / 2f);
                c.save();
                if (angle != 0f) c.rotate(angle, x, y);
                drawPlantBitmap(c, bitmap, spriteRect, tintFood);
                c.restore();

                if (foodActive) {
                    p.setStyle(Paint.Style.STROKE);
                    p.setStrokeWidth(Math.max(2f, size * .035f));
                    p.setColor(Color.argb(210, 255, 221, 90));
                    c.drawCircle(x, y, size * (.49f + .015f * (float) Math.sin(now / 110.0)), p);
                    p.setStyle(Paint.Style.FILL);
                }
                return;
            }
            p.setColor(type == SUNFLOWER ? Color.YELLOW : type == GIGANUT ? Color.rgb(145, 95, 55)
                    : type == CHOMPER ? Color.rgb(145, 70, 180) : Color.rgb(55, 175, 70));
            c.drawCircle(x, y, size * .42f, p);
        }

                private void drawZombies(Canvas c){
            long now = System.currentTimeMillis();
            for (Zombie z : zombies) {
                float widthScale = z.boss ? 1.20f : z.type == 4 ? .88f : z.type == 5 ? .61f : .68f;
                float heightScale = z.boss ? 1.20f : z.type == 4 ? 1.02f : z.type == 5 ? .72f : .82f;
                float w = cellW * widthScale;
                float h = cellH * heightScale;
                Bitmap image = z.boss && zombieBossScaled != null ? zombieBossScaled
                        : z.type == 3 && zombieVinhHungScaled != null ? zombieVinhHungScaled
                        : zombieNormalScaled != null ? zombieNormalScaled : zombieImg;

                float phase = (now - z.bornAt) / 145.0f + z.row * .55f + z.type;
                float bob = z.dummy ? 0f : (float) Math.sin(phase) * h * .025f;
                boolean biting = z.lastAttack > 0L && now - z.lastAttack < 280L && plantInFront(z) != null;
                boolean hitFlash = z.lastHitAt > 0L && now - z.lastHitAt < 135L;
                float lean = biting ? -8f : hitFlash ? 6f : (float) Math.sin(phase * .45f) * 2.2f;
                ColorFilter filter = now < z.stunnedUntil ? zombieStunFilter
                        : z.type == 4 ? zombieArmorFilter
                        : z.type == 5 ? zombieShadowFilter : null;

                if (image != null) {
                    spriteRect.set(z.x - w / 2f, z.y + bob - h * .55f,
                            z.x + w / 2f, z.y + bob + h * .45f);
                    c.save();
                    c.rotate(lean, z.x, z.y + bob);
                    spritePaint.setColor(Color.WHITE);
                    spritePaint.setAlpha(hitFlash ? 205 : 255);
                    spritePaint.setColorFilter(filter);
                    spritePaint.setFilterBitmap(true);
                    c.drawBitmap(image, null, spriteRect, spritePaint);
                    c.restore();
                    spritePaint.setAlpha(255);
                    spritePaint.setColorFilter(null);
                } else {
                    p.setColor(z.type == 4 ? Color.rgb(130, 91, 55) :
                            z.type == 5 ? Color.rgb(100, 75, 145) : Color.rgb(80, 80, 80));
                    spriteRect.set(z.x - w / 2f, z.y + bob - h / 2f,
                            z.x + w / 2f, z.y + bob + h / 2f);
                    c.drawOval(spriteRect, p);
                }

                if (z.type == 4 && !z.boss) {
                    p.setStyle(Paint.Style.STROKE);
                    p.setStrokeWidth(Math.max(2f, cellW * .025f));
                    p.setColor(Color.rgb(255, 204, 110));
                    c.drawCircle(z.x, z.y + bob - h * .04f, Math.min(w, h) * .43f, p);
                    p.setStyle(Paint.Style.FILL);
                }
                if (now < z.stunnedUntil) {
                    p.setColor(Color.argb(145, 115, 220, 255));
                    c.drawCircle(z.x + w * .33f, z.y + bob - h * .42f, 3.5f, p);
                    c.drawCircle(z.x - w * .25f, z.y + bob - h * .52f, 2.5f, p);
                }

                if (z.dummy) {
                    drawText(c, "∞ HP", z.x, z.y + bob - h * .66f, 13, Color.YELLOW, Paint.Align.CENTER);
                } else {
                    drawHp(c, z.x - w / 2f, z.y + bob - h * .65f, w, z.hp, z.maxHp);
                    if (z.type == 3 || z.type == 4 || z.type == 5) {
                        String label = z.type == 3 ? "VĨNH HÙNG" : z.type == 4 ? "GIÁP" : "BÓNG MA";
                        drawText(c, label, z.x, z.y + bob - h * .76f, 8,
                                z.type == 4 ? Color.rgb(255, 220, 120) :
                                z.type == 5 ? Color.rgb(205, 185, 255) : Color.WHITE,
                                Paint.Align.CENTER);
                    }
                }
            }
        }

                private void drawPeas(Canvas c) {
            for (Pea q : peas) {
                if(q.kind>0){
                    p.setColor(q.kind==1?Color.rgb(111,173,56):q.kind==2?Color.rgb(255,196,48):q.kind==3?Color.rgb(255,225,125):Color.rgb(40,155,57));
                    float radius=q.kind==4?14f:q.kind==3?8f:7f;
                    c.drawCircle(q.x,q.y,radius,p);
                    if(q.kind==4){p.setColor(Color.rgb(30,75,30));c.drawLine(q.x-6,q.y-4,q.x+6,q.y+4,p);c.drawLine(q.x-5,q.y+5,q.x+5,q.y-5,p);}
                    continue;
                }
                float size = q.big ? 18 : 10;
                Bitmap image = q.big && bulletBigScaled != null ? bulletBigScaled
                        : bulletSmallScaled != null ? bulletSmallScaled : bulletImg;
                if (image != null) {
                    spriteRect.set(q.x - size, q.y - size, q.x + size, q.y + size);
                    c.drawBitmap(image, null, spriteRect, p);
                } else {
                    p.setColor(Color.GREEN);
                    c.drawCircle(q.x, q.y, size * .7f, p);
                }
            }
        }

        private void drawSuns(Canvas c) {
            for(SunDrop drop:suns){float x=drop.x,y=drop.y;p.setColor(Color.rgb(255,194,35));
                for(int i=0;i<8;i++){double a=i*Math.PI/4.0;c.drawLine(x+(float)Math.cos(a)*12f,y+(float)Math.sin(a)*12f,x+(float)Math.cos(a)*18f,y+(float)Math.sin(a)*18f,p);}
                p.setColor(Color.YELLOW);c.drawCircle(x,y,12f,p);p.setColor(Color.rgb(255,238,135));c.drawCircle(x-3f,y-3f,4f,p);
            }
        }

                                private void drawMowers(Canvas c) {
            for (Mower mower : mowers) {
                if (mower == null || (mower.used && !mower.active && mower.x > getWidth())) continue;
                float y=top+(mower.row+.80f)*cellH;
                if(mowerImg!=null){
                    float width=cellW*.94f,height=cellH*.62f;
                    spriteRect.set(mower.x-width*.5f,y-height*.55f,mower.x+width*.5f,y+height*.45f);
                    spritePaint.setAlpha(255);spritePaint.setColorFilter(null);spritePaint.setFilterBitmap(true);
                    c.drawBitmap(mowerImg,null,spriteRect,spritePaint);
                    continue;
                }
                float l=mower.x-cellW*.37f,r=mower.x+cellW*.37f,t=y-cellH*.19f,b=y+cellH*.025f;
                float wheel=Math.max(4f,cellH*.065f);
                p.setColor(Color.rgb(25,27,27));c.drawCircle(l+cellW*.17f,y+wheel*.45f,wheel,p);
                c.drawCircle(r-cellW*.15f,y+wheel*.45f,wheel,p);
                p.setColor(Color.rgb(205,48,35));c.drawRoundRect(l,t,r,b,7f,7f,p);
                p.setColor(Color.rgb(44,137,60));c.drawRoundRect(l+cellW*.14f,t-cellH*.10f,r-cellW*.12f,y-cellH*.015f,6f,6f,p);
            }
        }

        private void drawHp(
                Canvas c,
                float x,
                float y,
                float width,
                float hp,
                float maxHp
        ) {
            p.setColor(Color.DKGRAY);
            c.drawRect(x, y, x + width, y + 7, p);

            if (maxHp > 0) {
                p.setColor(Color.GREEN);
                c.drawRect(
                        x,
                        y,
                        x + width * Math.max(0, Math.min(1f, hp / maxHp)),
                        y + 7,
                        p
                );
            }
        }

                private void updateGame(){
            long now=System.currentTimeMillis();
            float dt=Math.min(.033333f,Math.max(0f,(now-lastUpdate)/1000f))*speed;lastUpdate=now;
            if(!sandboxMode&&now-lastSun>=20000L){lastSun=now;int col=random.nextInt(COLS),row=random.nextInt(ROWS);
                addSunDrop(new SunDrop(left+col*cellW+cellW/2f,top-24f,top+row*cellH+cellH*.62f));}
            updatePlants(now);updatePlantAnimations(now);updateBinu(now);checkBinu(now);
            updatePeas(dt);updateZombies(now,dt);updateMowers(dt);updateSunDrops(dt);removeDead();
            if(!sandboxMode&&spawned<total&&now-spawnClock>=spawnDelay()){spawnZombie();spawned++;spawnClock=now;}
            if(!sandboxMode&&spawned>=total&&zombies.isEmpty()&&killed>=total)winLevel();
        }

        private void updateSunDrops(float dt) {
            long now = System.currentTimeMillis();
            float fall = Math.max(1f, cellH * 1.45f) * dt;
            for (int i = suns.size() - 1; i >= 0; i--) {
                SunDrop drop = suns.get(i);
                if (now >= drop.expiresAt) {
                    suns.remove(i);
                    continue;
                }
                if (drop.falling && drop.y < drop.targetY) {
                    drop.y = Math.min(drop.targetY, drop.y + fall);
                    if (drop.y >= drop.targetY) drop.falling = false;
                }
            }
        }

        private void addSunDrop(SunDrop drop) {
            // Cap on-screen drops so uncollected suns cannot grow the render list forever.
            if (suns.size() >= 24) suns.remove(0);
            suns.add(drop);
        }

                private void updatePlants(long now){
            for(int r=0;r<ROWS;r++)for(int col=0;col<COLS;col++){
                Plant a=plants[r][col];if(a==null)continue;
                if(a.foodUsed&&a.type!=GIGANUT&&a.plantFoodUntil>0&&now>=a.plantFoodUntil&&a.foodShotsRemaining<=0){a.foodUsed=false;a.plantFoodUntil=0;a.foodGiantShotDone=false;}
                if(a.type==SUNFLOWER){
                    if(now-a.last>=7000L){
                        if(!sandboxMode)addSunDrop(new SunDrop(left+col*cellW+cellW/2f,top+r*cellH+cellH*.22f));
                        a.last=now;a.shootUntil=now+500L;a.animClock=now;a.animFrame=1;
                    }
                }else if(a.type==PEASHOOTER){
                    if(a.foodUsed&&a.foodShotsRemaining>0){
                        int n=0;
                        while(a.foodShotsRemaining>0&&now>=a.nextFoodShotAt&&n<5){
                            fire(r,col,20,false);a.foodShotsRemaining--;a.nextFoodShotAt+=55L;n++;
                        }
                        if(n>0)animatePlantShot(a,now,145L);
                    }else if(!a.foodUsed&&now-a.last>=1500L&&rowHasZombie(r,col)){
                        fire(r,col,20,false);a.last=now;animatePlantShot(a,now,360L);
                    }
                }else if(a.type==REPEATER){
                    if(a.foodUsed&&a.foodShotsRemaining>0){
                        int n=0;
                        while(a.foodShotsRemaining>0&&now>=a.nextFoodShotAt&&n<6){
                            fire(r,col,20,false);a.foodShotsRemaining--;a.nextFoodShotAt+=34L;n++;
                        }
                        if(n>0)animatePlantShot(a,now,145L);
                        if(a.foodShotsRemaining==0&&!a.foodGiantShotDone){
                            fire(r,col,400,true);a.foodGiantShotDone=true;animatePlantShot(a,now,450L);
                        }
                    }else if(!a.foodUsed&&now-a.last>=1500L&&rowHasZombie(r,col)){
                        firePair(r,col,20);a.last=now;animatePlantShot(a,now,330L);
                    }
                }else if(a.type==CABBAGE_PULT){
                    if(!a.foodUsed&&now-a.last>=3000L&&rowHasZombie(r,col)&&fireLobbed(r,col,40,1,0,0L)){
                        a.last=now;animatePlantShot(a,now,560L);
                    }
                }else if(a.type==KERNEL_PULT){
                    if(!a.foodUsed&&now-a.last>=3000L&&rowHasZombie(r,col)){
                        boolean butter=random.nextFloat()<.25f;
                        if(fireLobbed(r,col,butter?40:20,butter?3:2,0,butter?5000L:0L)){
                            a.last=now;animatePlantShot(a,now,560L);
                        }
                    }
                }else if(a.type==MELON_PULT){
                    if(!a.foodUsed&&now-a.last>=3000L&&rowHasZombie(r,col)&&fireLobbed(r,col,80,4,40,0L)){
                        a.last=now;animatePlantShot(a,now,680L);
                    }
                }else if(a.type==CHOMPER){
                    if(a.chompTarget!=null&&now>=a.attackResolveAt){if(!a.chompTarget.dummy)a.chompTarget.hp=0;a.chompTarget=null;}
                    if(!a.foodUsed&&now>=a.nextAttackAt&&now>=a.shootUntil){Zombie z=nearestZombie(r,col,cellW*1.5f);if(z!=null){a.chompTarget=z;a.attackResolveAt=now+420L;a.nextAttackAt=now+17000L;a.animFrame=1;a.animClock=now;a.shootUntil=now+1100L;}}
                }else if(a.type==MINE){
                    if(!a.armed&&now>=a.armTime)a.armed=true;
                    if(a.armed){Zombie z=zombieOnCell(r,col);if(z!=null){z.hp-=1800f;a.hp=0;}}
                }
            }
        }

                private void fire(int row,int col,int damage,boolean big){
            if(peas.size()>=120)peas.remove(0);
            peas.add(new Pea(left+col*cellW+cellW*.56f,top+row*cellH+cellH*.50f,row,damage,big,false,1));
        }

        private boolean fireLobbed(int row,int col,int damage,int kind,int splash,long stunMs){
            float startX=left+(col+.56f)*cellW,startY=top+(row+.50f)*cellH;
            Zombie target=null;float nearest=Float.MAX_VALUE;
            for(Zombie z:zombies)if(!z.dummy&&z.hp>0&&z.row==row&&z.x>startX&&z.x-startX<nearest){target=z;nearest=z.x-startX;}
            if(target==null)return false;
            if(peas.size()>=120)peas.remove(0);
            peas.add(new Pea(startX,startY,row,damage,kind,splash,stunMs,target.x,target.y));
            return true;
        }

        private void firePair(int row,int col,int damage){
            float x=left+col*cellW+cellW*.56f,y=top+row*cellH+cellH*.50f;
            if(peas.size()>118)peas.subList(0,peas.size()-118).clear();
            peas.add(new Pea(x,y-cellH*.055f,row,damage,false,false,1));
            peas.add(new Pea(x,y+cellH*.055f,row,damage,false,false,1));
        }

        private void animatePlantShot(Plant plant, long now, long durationMs) {
            plant.animFrame = 1;
            plant.animClock = now;
            plant.shootUntil = now + durationMs;
        }

                private void updatePlantAnimations(long now){
            for(int r=0;r<ROWS;r++)for(int col=0;col<COLS;col++){
                Plant a=plants[r][col];if(a==null||a.type==GIGANUT||a.type==BINU||a.type==MINE)continue;
                if(a.type==PEASHOOTER){if(now<a.shootUntil&&now-a.animClock>=75L){a.animClock=now;a.animFrame=Math.min(4,a.animFrame+1);}else if(now>=a.shootUntil)a.animFrame=0;}
                else if(a.type==REPEATER){if(now<a.shootUntil&&now-a.animClock>=65L){a.animClock=now;a.animFrame=Math.min(5,a.animFrame+1);}else if(now>=a.shootUntil)a.animFrame=0;}
                else if(a.type==CHOMPER){if(now<a.shootUntil&&now-a.animClock>=155L){a.animClock=now;a.animFrame=Math.min(5,a.animFrame+1);}else if(now>=a.shootUntil)a.animFrame=0;}
                else if(a.type==SUNFLOWER&&now>=a.shootUntil)a.animFrame=0;
            }
        }

        private void drawBinu(Canvas c) {
            if (!binuWaiting && !binuJumping) return;
            if (binuRow < 0 || binuCol < 0) return;
            Bitmap b = binuImg;
            if (binuJumping) {
                if (binuFrame == 1) b = binu1Img;
                else if (binuFrame == 2) b = binu2Img;
                else if (binuFrame == 3) b = binu3Img;
                else if (binuFrame >= 4) b = binu4Img;
            }
            if (b == null) return;
            float x = binuJumping ? binuX : left + binuCol * cellW + cellW / 2f;
            float y = binuJumping ? binuY : top + binuRow * cellH + cellH / 2f;
            spriteRect.set(x-cellW*.37f,y-cellH*.42f,x+cellW*.37f,y+cellH*.42f);
            drawPlantBitmap(c, b, spriteRect, binuFoodBoost);
        }

        private void playBinuSound1() {
            stopBinuSound1();

            binuSound1Finished = false;

            try {
                int id = getResources().getIdentifier(
                        "binusound1",
                        "raw",
                        getPackageName()
                );

                if (id == 0) {
                    binuSound1Finished = true;
                    return;
                }

                binuSound1Player = MediaPlayer.create(
                        MainActivity.this,
                        id
                );

                if (binuSound1Player == null) {
                    binuSound1Finished = true;
                    return;
                }

                // Sound 1 chạy nhanh 1.5x. BINU vẫn chỉ nhảy sau khi sound 1 phát xong.
                try {
                    if (android.os.Build.VERSION.SDK_INT >= 23) {
                        binuSound1Player.setPlaybackParams(
                                binuSound1Player.getPlaybackParams().setSpeed(1.5f)
                        );
                    }
                } catch (Exception ignored) {
                }

                binuSound1Player.setOnCompletionListener(
                        mp -> {
                            binuSound1Finished = true;
                            try {
                                mp.release();
                            } catch (Exception ignored) {
                            }
                            if (binuSound1Player == mp) {
                                binuSound1Player = null;
                            }
                        }
                );

                binuSound1Player.start();
            } catch (Exception e) {
                binuSound1Finished = true;
                binuSound1Player = null;
            }
        }

        private void playBinuSound2() {
            stopBinuSound2();

            try {
                int id = getResources().getIdentifier(
                        "binusound2",
                        "raw",
                        getPackageName()
                );

                if (id == 0) return;

                binuSound2Player = MediaPlayer.create(
                        MainActivity.this,
                        id
                );

                if (binuSound2Player == null) return;

                binuSound2Player.setOnCompletionListener(
                        mp -> {
                            try {
                                mp.release();
                            } catch (Exception ignored) {
                            }
                            if (binuSound2Player == mp) {
                                binuSound2Player = null;
                            }
                        }
                );

                binuSound2Player.start();
            } catch (Exception ignored) {
            }
        }

        private void stopBinuSound1() {
            try {
                if (binuSound1Player != null) {
                    binuSound1Player.stop();
                    binuSound1Player.release();
                    binuSound1Player = null;
                }
            } catch (Exception ignored) {
                binuSound1Player = null;
            }
        }

        private void stopBinuSound2() {
            try {
                if (binuSound2Player != null) {
                    binuSound2Player.stop();
                    binuSound2Player.release();
                    binuSound2Player = null;
                }
            } catch (Exception ignored) {
                binuSound2Player = null;
            }
        }

        private void updateBinu(long now) {
            if (binuWaiting && !binuJumping && now >= binuDetectAt && binuSound1Finished) {
                startBinuJump(now);
            }
            if (!binuJumping) return;
            long elapsed = now - binuClock;
            float t = Math.max(0f, Math.min(1f, elapsed / 480f));
            float smooth = t * t * (3f - 2f * t);
            binuX = binuStartX + (binuTargetX - binuStartX) * smooth;
            binuY = binuStartY + (binuTargetY - binuStartY) * smooth;
            binuFrame = Math.max(1, Math.min(4, (int)(elapsed / 120L) + 1));
            if (elapsed >= 480L) {
                binuX = binuTargetX;
                binuY = binuTargetY;
                smashBinu();
                binuJumping = false;
                binuWaiting = false;
                binuFrame = 0;
                binuRow = -1;
                binuCol = -1;
            }
        }

                        private void checkBinu(long now){
            if(binuWaiting||binuJumping)return;
            for(int r=0;r<ROWS;r++)for(int col=0;col<COLS;col++){
                Plant a=plants[r][col];if(a==null||a.type!=BINU)continue;
                float bx=left+col*cellW+cellW/2f,by=top+r*cellH+cellH/2f;
                boolean boost=a.foodUsed&&now<a.plantFoodUntil;
                Zombie nearest=null;float best=Float.MAX_VALUE;
                for(Zombie z:zombies){if(z.hp<=0||z.dummy||z.row!=r)continue;float d=Math.abs(z.x-bx);
                    if(d<=cellW*(boost?2.25f:1.30f)&&d<best){nearest=z;best=d;}}
                if(nearest!=null){binuRow=r;binuCol=col;binuWaiting=true;binuFoodBoost=boost;binuDetectAt=now+(boost?350L:2000L);binuX=bx;binuY=by;
                    if(boost)binuSound1Finished=true;return;}
            }
        }

                        private void startBinuJump(long now){
            if(!binuWaiting||binuRow<0||binuCol<0)return;
            float sx=left+binuCol*cellW+cellW/2f,sy=top+binuRow*cellH+cellH/2f;
            Zombie target=null;float best=Float.MAX_VALUE;
            for(Zombie z:zombies){if(z.hp<=0||z.dummy||z.row!=binuRow)continue;float d=Math.abs(z.x-sx);
                if(d<=cellW*(binuFoodBoost?2.7f:1.8f)&&d<best){target=z;best=d;}}
            if(target==null){binuWaiting=false;binuFoodBoost=false;return;}
            binuStartX=sx;binuStartY=sy;binuTargetX=target.x;binuTargetY=target.y;binuX=sx;binuY=sy;
            plants[binuRow][binuCol]=null;binuJumping=true;binuFrame=1;binuClock=now;playBinuSound2();
        }

                        private void smashBinu(){
            if(binuRow<0)return;
            float cx=binuTargetX,range=cellW*(binuFoodBoost?3f:1.65f);
            for(Zombie z:zombies){if(z.hp<=0||z.dummy||z.row!=binuRow)continue;
                if(Math.abs(z.x-cx)<=range){if(z.boss)z.hp=Math.max(1f,z.hp*(binuFoodBoost?.25f:.50f));else z.hp=0;}}
            binuFoodBoost=false;
        }

                private void updatePeas(float dt){
            for(int i=peas.size()-1;i>=0;i--){Pea q=peas.get(i);
                if(q.kind>0){
                    q.progress=Math.min(1f,q.progress+dt/q.duration);
                    q.x=q.startX+(q.targetX-q.startX)*q.progress;
                    q.y=q.startY+(q.targetY-q.startY)*q.progress-(float)Math.sin(q.progress*Math.PI)*cellH*.78f;
                    if(q.progress>=1f){
                        Zombie hit=null;float best=Float.MAX_VALUE;
                        for(Zombie z:zombies)if(z.hp>0&&!z.dummy&&z.row==q.row&&Math.abs(z.x-q.targetX)<best){hit=z;best=Math.abs(z.x-q.targetX);}
                        if(hit!=null){
                            long hitAt=System.currentTimeMillis();
                            hit.hp-=q.damage;hit.lastHitAt=hitAt;
                            if(q.stunMs>0)hit.stunnedUntil=hitAt+q.stunMs;
                            if(q.splashDamage>0)for(Zombie z:zombies)
                                if(z.hp>0&&!z.dummy&&Math.abs(z.row-q.row)<=1&&Math.abs(z.x-q.targetX)<=cellW*.85f&&z!=hit){
                                    z.hp-=q.splashDamage;z.lastHitAt=hitAt;
                                }
                        }
                        peas.remove(i);
                    }
                    continue;
                }
                q.x+=q.direction*cellW*8.5f*dt;Zombie hit=null;
                for(Zombie z:zombies)if(z.hp>0&&z.row==q.row&&Math.abs(z.x-q.x)<cellW*.30f){hit=z;break;}
                if(hit!=null){if(!hit.dummy){hit.hp-=q.damage;hit.lastHitAt=System.currentTimeMillis();}peas.remove(i);}else if(q.x>getWidth()+40)peas.remove(i);
            }
        }

                        private void updateZombies(long now, float dt) {
            for (Zombie z : zombies) {
                if (z.hp <= 0 || z.dummy) continue;
                if(now<z.stunnedUntil)continue;
                Plant plant = plantInFront(z);
                if (plant != null) {
                    if (now - z.lastAttack >= 800L) {
                        plant.hp -= z.damage;
                        z.lastAttack = now;
                    }
                } else {
                    z.x -= z.speed * dt;
                }
                if (z.x <= left - cellW * .45f) {
                    Mower mower = mowers[z.row];
                    if (!mower.used) {
                        mower.used = true;
                        mower.active = true;
                        mower.x = left - cellW * .30f;
                    } else if (!mower.active) {
                        z.hp = 0;
                        screen = LOSE;
                        return;
                    }
                }
            }
        }

                private void updateMowers(float dt) {
            for (Mower mower : mowers) {
                if (mower == null || !mower.active) continue;
                float previousX = mower.x;
                mower.x += cellW * 17f * dt;
                float minX = Math.min(previousX, mower.x) - cellW * .66f;
                float maxX = Math.max(previousX, mower.x) + cellW * .66f;
                for (Zombie z : zombies) {
                    if (z.dummy || z.hp <= 0 || z.row != mower.row) continue;
                    // Swept collision prevents a fast mower skipping over a zombie between frames.
                    if (z.x >= minX && z.x <= maxX) z.hp = 0;
                }
                if (mower.x > getWidth() + cellW) mower.active = false;
            }
        }

        private void spawnZombie() {
            int n = activeRows();
            int first = (ROWS - n) / 2;
            int row = first + random.nextInt(n);

            boolean boss =
                    isBossLevel(level) &&
                    spawned == total - 1;

            zombies.add(new Zombie(row, boss ? 0 : chooseZombieType(), boss));
        }

        private int chooseZombieType() {
            int roll = random.nextInt(100);
            if (level >= 16 && roll < 10) return 5;       // Bóng ma: rất nhanh, ít máu.
            if (level >= 11 && roll < 25) return 4;       // Zombie giáp: chậm, nhiều máu.
            if (level >= 6 && roll < 43) return 3;        // Vĩnh Hùng: sprite riêng, cân bằng.
            if (level >= 3 && roll < 62) return 2;        // Zombie chạy.
            if (roll < 82) return 1;                      // Zombie trâu.
            return 0;                                     // Zombie thường.
        }

                        private void removeDead() {
            boolean earned=false;
            for(int i=zombies.size()-1;i>=0;i--){
                Zombie zombie=zombies.get(i);
                if(!zombie.dummy&&zombie.hp<=0){
                    zombies.remove(i);killed++;dailyKills=Math.min(9999,dailyKills+1);
                    int reward=zombie.boss?60+random.nextInt(141):3+random.nextInt(18);
                    coins=Math.min(99999999,coins+reward);earned=true;
                }
            }
            for(int row=0;row<ROWS;row++)for(int col=0;col<COLS;col++)
                if(plants[row][col]!=null&&plants[row][col].hp<=0)plants[row][col]=null;
            if(earned)saveGame();
        }

                private boolean rowHasZombie(int row, int col){
            float muzzleX = left + (col + .56f) * cellW;
            for(Zombie z:zombies)
                if(!z.dummy&&z.hp>0&&z.row==row&&z.x>muzzleX+cellW*.12f)return true;
            return false;
        }

        private Plant plantInFront(Zombie z) {
            int col = (int) ((z.x - left) / cellW);

            if (col < 0 || col >= COLS) {
                return null;
            }

            return plants[z.row][col];
        }

                private Zombie zombieOnCell(int row,int col){
            float x=left+col*cellW+cellW/2f;
            for(Zombie z:zombies)if(!z.dummy&&z.hp>0&&z.row==row&&Math.abs(z.x-x)<cellW*.50f)return z;return null;
        }

                private Zombie nearestZombie(int row,int col,float range){
            float x=left+col*cellW+cellW/2f;Zombie best=null;float dist=Float.MAX_VALUE;
            for(Zombie z:zombies){if(z.dummy||z.hp<=0||z.row!=row)continue;float d=Math.abs(z.x-x);if(d<=range&&d<dist){best=z;dist=d;}}
            return best;
        }

                        private void startLevel(int lv) {
            sandboxMode=false;binuFoodBoost=false;speed=1;
            level=Math.max(1,Math.min(MAX_LEVEL,lv));selectedWorld=(level-1)/LEVELS_PER_WORLD;
            screen=PLAY;sun=500;killed=0;spawned=0;
            int local=((level-1)%LEVELS_PER_WORLD)+1;
            total=isBossLevel(level)?18+selectedWorld*2:8+(local-1)/2;
            clearBoard();long now=System.currentTimeMillis();lastUpdate=now;spawnClock=now;lastSun=now;saveGame();
        }

        private void startSandbox(){
            sandboxMode=true;binuFoodBoost=false;speed=1;screen=PLAY;sun=999999;killed=0;spawned=0;total=0;
            clearBoard();long now=System.currentTimeMillis();lastUpdate=now;spawnClock=now;lastSun=now;
            Zombie dummy=new Zombie(ROWS/2,0,false);
            dummy.dummy=true;dummy.hp=Float.POSITIVE_INFINITY;dummy.maxHp=Float.POSITIVE_INFINITY;
            dummy.speed=0f;dummy.damage=0;dummy.x=left+cellW*(COLS*.5f);
            dummy.y=top+(ROWS/2f)*cellH+cellH*.58f;zombies.add(dummy);
        }

        private String dayKey(int offset) {
            Calendar calendar=Calendar.getInstance();calendar.add(Calendar.DAY_OF_YEAR,offset);
            return calendar.get(Calendar.YEAR)+"-"+(calendar.get(Calendar.MONTH)+1)+"-"+calendar.get(Calendar.DAY_OF_MONTH);
        }

        private void refreshDaily() {
            String today=dayKey(0);
            if(!today.equals(dailyDate)){
                boolean consecutive=dailyDate.equals(dayKey(-1));
                dailyStreak=dailyDate.isEmpty()?1:(consecutive?Math.min(7,dailyStreak+1):1);
                dailyDate=today;dailyWins=0;dailySuns=0;dailyKills=0;
                dailyQuestClaimed=false;dailyRewardClaimed=false;
            }
        }

        private void claimDailyQuest() {
            refreshDaily();
            if(dailyQuestClaimed||dailyWins<1||dailySuns<500||dailyKills<20)return;
            dailyQuestClaimed=true;coins=Math.min(99999999,coins+600);food=Math.min(99999,food+3);saveGame();
        }

        private void claimDailyReward() {
            refreshDaily();
            if(dailyRewardClaimed)return;
            dailyRewardClaimed=true;coins=Math.min(99999999,coins+200+((dailyStreak-1)%7)*50);
            if(dailyStreak%7==0)food=Math.min(99999,food+1);
            saveGame();
        }

        private void drawDailyQuest(Canvas c) {
            drawBackdrop(c,60);
            drawText(c,"NHIỆM VỤ HẰNG NGÀY",getWidth()/2f,getHeight()*.12f,30,Color.WHITE,Paint.Align.CENTER);
            drawText(c,"Làm mới mỗi ngày • Hoàn thành cả 3 nhiệm vụ để nhận thưởng",getWidth()/2f,getHeight()*.19f,14,Color.YELLOW,Paint.Align.CENTER);
            String[] names={"Hoàn thành 1 màn chơi","Thu thập 500 SUN","Đánh bại 20 zombie"};
            int[] values={Math.min(1,dailyWins),Math.min(500,dailySuns),Math.min(20,dailyKills)};
            int[] goals={1,500,20};
            for(int i=0;i<3;i++){
                float y=.28f+i*.17f;
                p.setColor(Color.argb(225,50,40,26));c.drawRoundRect(getWidth()*.18f,getHeight()*y,getWidth()*.82f,getHeight()*(y+.125f),13,13,p);
                drawText(c,names[i],getWidth()*.23f,getHeight()*(y+.05f),16,Color.WHITE,Paint.Align.LEFT);
                drawText(c,values[i]+"/"+goals[i],getWidth()*.77f,getHeight()*(y+.05f),15,Color.YELLOW,Paint.Align.RIGHT);
                p.setColor(Color.DKGRAY);c.drawRoundRect(getWidth()*.23f,getHeight()*(y+.075f),getWidth()*.77f,getHeight()*(y+.095f),8,8,p);
                p.setColor(Color.rgb(70,190,83));c.drawRoundRect(getWidth()*.23f,getHeight()*(y+.075f),
                        getWidth()*(.23f+.54f*values[i]/(float)goals[i]),getHeight()*(y+.095f),8,8,p);
            }
            boolean done=dailyWins>=1&&dailySuns>=500&&dailyKills>=20;
            drawText(c,dailyQuestClaimed?"ĐÃ NHẬN THƯỞNG":done?"ĐÃ HOÀN THÀNH!":"PHẦN THƯỞNG: 600 XU + 3 PF",
                    getWidth()/2f,getHeight()*.82f,16,Color.YELLOW,Paint.Align.CENTER);
            button(c,.35f,.86f,.65f,.96f,dailyQuestClaimed?"ĐÃ NHẬN":done?"NHẬN THƯỞNG":"CHƯA ĐỦ");
            button(c,.035f,.86f,.19f,.96f,"QUAY LẠI");
        }

        private void drawDailyRewards(Canvas c) {
            drawBackdrop(c,55);
            drawText(c,"QUÀ ĐĂNG NHẬP HẰNG NGÀY",getWidth()/2f,getHeight()*.13f,28,Color.WHITE,Paint.Align.CENTER);
            drawText(c,"Chuỗi đăng nhập: "+dailyStreak+" ngày",getWidth()/2f,getHeight()*.21f,17,Color.YELLOW,Paint.Align.CENTER);
            for(int i=0;i<7;i++){
                float x=.08f+i*.12f;
                p.setColor(i==(dailyStreak-1)%7?Color.rgb(190,126,38):Color.argb(225,45,45,45));
                c.drawRoundRect(getWidth()*x,getHeight()*.34f,getWidth()*(x+.105f),getHeight()*.60f,12,12,p);
                drawText(c,"NGÀY "+(i+1),getWidth()*(x+.0525f),getHeight()*.405f,12,Color.WHITE,Paint.Align.CENTER);
                drawText(c,(200+i*50)+" xu",getWidth()*(x+.0525f),getHeight()*.49f,13,Color.YELLOW,Paint.Align.CENTER);
                if(i==6)drawText(c,"+1 PF",getWidth()*(x+.0525f),getHeight()*.55f,11,Color.GREEN,Paint.Align.CENTER);
            }
            drawText(c,dailyRewardClaimed?"ĐÃ NHẬN QUÀ HÔM NAY":"Nhận quà mỗi ngày để tăng chuỗi",
                    getWidth()/2f,getHeight()*.72f,16,Color.WHITE,Paint.Align.CENTER);
            button(c,.35f,.80f,.65f,.91f,dailyRewardClaimed?"ĐÃ NHẬN":"NHẬN QUÀ");
            button(c,.035f,.86f,.19f,.96f,"QUAY LẠI");
        }

        private void clearBoard() {
            for (int r = 0; r < ROWS; r++) {
                for (int col = 0; col < COLS; col++) {
                    plants[r][col] = null;
                }

                mowers[r] = new Mower(r);
            }

            zombies.clear();
            peas.clear();
            suns.clear();

            binuRow = -1;
            binuCol = -1;
            binuFrame = 0;
            binuDetectAt = 0L;
            binuWaiting = false;
            binuJumping = false;
            binuSound1Finished = false;
            binuX = binuY = binuStartX = binuStartY = binuTargetX = binuTargetY = 0f;
            stopBinuSound1();
            stopBinuSound2();

            tool = TOOL_NONE;
        }

                private int activeRows() {
            int local=((level-1)%LEVELS_PER_WORLD)+1;
            if(local==1)return 1;
            if(local<=3)return 3;
            return 5;
        }

        private boolean activeRow(int row) {
            int n = activeRows();
            int first = (ROWS - n) / 2;
            return row >= first && row < first + n;
        }
                private long spawnDelay() {
            int world=(level-1)/LEVELS_PER_WORLD,local=((level-1)%LEVELS_PER_WORLD)+1;
            if(local<=2)return 4200L-world*90L;
            if(local<=5)return 3600L-world*80L;
            if(local<=10)return 3100L-world*70L;
            return Math.max(1700L,2700L-world*90L);
        }

                private boolean isPlantUnlocked(int type){
            if(sandboxMode)return true;
            if(type==PEASHOOTER)return true;
            if(type==SUNFLOWER)return level>=1;
            if(type==GIGANUT)return level>=2;
            if(type==MINE)return level>=3;
            if(type==CHOMPER)return level>=4;
            if(type==REPEATER)return level>=5;
            if(type==BINU)return true;
            if(type==CABBAGE_PULT)return level>=6;
            if(type==KERNEL_PULT)return level>=8;
            if(type==MELON_PULT)return level>=12;
            return false;
        }

                private int plantCost(int type) {
            if (type == SUNFLOWER) return 50;
            if (type == PEASHOOTER) return 100;
            if (type == GIGANUT) return 175;
            if (type == CHOMPER) return 150;
            if (type == REPEATER) return 200;
            if (type == MINE) return 25;
            if (type == BINU) return 450;
            if (type == CABBAGE_PULT) return 100;
            if (type == KERNEL_PULT) return 100;
            if (type == MELON_PULT) return 325;
            return 99999;
        }

                private void winLevel() {
            screen=WIN;if(level<MAX_LEVEL)unlocked=Math.max(unlocked,level+1);
            if(!sandboxMode)dailyWins=Math.min(99,dailyWins+1);saveGame();
        }

                private void usePlantFood(Plant a){
            if(a==null||a.foodUsed||food<=0)return;
            long now=System.currentTimeMillis();food--;a.foodUsed=true;
            if(a.type==PEASHOOTER||a.type==REPEATER)playPlantFoodSound();
            if(a.type==GIGANUT){a.maxHp=12000f;a.hp=12000f;a.plantFoodUntil=Long.MAX_VALUE;}
            else if(a.type==SUNFLOWER){sun=Math.min(999999,sun+150);a.plantFoodUntil=now+900L;a.shootUntil=now+900L;a.animFrame=5;}
            else if(a.type==PEASHOOTER){a.foodShotsRemaining=60;a.nextFoodShotAt=now;a.plantFoodUntil=now+3600L;a.shootUntil=now+3600L;a.animFrame=5;}
            else if(a.type==REPEATER){a.foodShotsRemaining=60;a.foodGiantShotDone=false;a.nextFoodShotAt=now;a.plantFoodUntil=now+3700L;a.shootUntil=now+3700L;}
            else if(a.type==CHOMPER){
                a.plantFoodUntil=now+1400L;a.shootUntil=now+1200L;a.animFrame=1;a.animClock=now;float cx=left+a.col*cellW+cellW/2f;
                for(int pass=0;pass<3;pass++){Zombie best=null;float dist=Float.MAX_VALUE;
                    for(Zombie z:zombies){if(z.hp<=0||z.dummy||z.row!=a.row)continue;float d=Math.abs(z.x-cx);if(d<=cellW*3.2f&&d<dist){best=z;dist=d;}}
                    if(best==null)break;if(best.boss)best.hp=Math.max(0f,best.hp-1000f);else best.hp=0;best.x+=cellW*.55f;}
            }else if(a.type==MINE){
                a.armed=true;a.armTime=now;a.plantFoodUntil=now+1200L;
                int[][] offsets={{0,1},{0,-1},{-1,0},{1,0},{0,2},{0,-2},{-1,1},{1,1}};int made=0;
                for(int[] off:offsets){int rr=a.row+off[0],cc=a.col+off[1];if(made>=2)break;
                    if(rr<0||rr>=ROWS||cc<0||cc>=COLS||!activeRow(rr)||plants[rr][cc]!=null)continue;
                    Plant clone=new Plant(MINE,rr,cc);clone.armed=true;clone.armTime=now;plants[rr][cc]=clone;made++;}
            }else if(a.type==BINU){a.plantFoodUntil=now+5000L;binuFoodBoost=true;binuSound1Finished=true;}
            else if(a.type==CABBAGE_PULT){a.plantFoodUntil=now+1800L;a.shootUntil=now+1800L;for(Zombie z:zombies)if(z.hp>0&&!z.dummy)z.hp-=80f;}
            else if(a.type==KERNEL_PULT){a.plantFoodUntil=now+1800L;a.shootUntil=now+1800L;for(Zombie z:zombies)if(z.hp>0&&!z.dummy){z.hp-=40f;z.stunnedUntil=now+8000L;}}
            else if(a.type==MELON_PULT){a.plantFoodUntil=now+1800L;a.shootUntil=now+1800L;for(Zombie z:zombies)if(z.hp>0&&!z.dummy){z.hp-=200f;for(Zombie other:zombies)if(other!=z&&other.hp>0&&!other.dummy&&Math.abs(other.row-z.row)<=1&&Math.abs(other.x-z.x)<=cellW*.85f)other.hp-=20f;}}
            saveGame();
        }

        private void drawOverlay(
                Canvas c,
                String title,
                String first,
                String second,
                String third
        ) {
            p.setColor(Color.argb(215, 0, 0, 0));
            c.drawRect(0, 0, getWidth(), getHeight(), p);

            drawText(
                    c,
                    title,
                    getWidth() / 2f,
                    getHeight() * .32f,
                    34,
                    Color.WHITE,
                    Paint.Align.CENTER
            );

            if (!first.isEmpty()) {
                button(c, .32f, .42f, .68f, .52f, first);
            }

            if (!second.isEmpty()) {
                button(c, .32f, .55f, .68f, .65f, second);
            }

            if (!third.isEmpty()) {
                button(c, .32f, .68f, .68f, .78f, third);
            }
        }

        private void button(Canvas c,float x1,float y1,float x2,float y2,String label) {
            float l=getWidth()*x1,t=getHeight()*y1,r=getWidth()*x2,b=getHeight()*y2;
            p.setColor(Color.rgb(83,47,25));c.drawRoundRect(l,t,r,b,13,13,p);
            p.setColor(Color.rgb(151,94,43));c.drawRoundRect(l+2,t+2,r-2,t+(b-t)*.48f,11,11,p);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2.2f);p.setColor(Color.rgb(241,193,95));
            c.drawRoundRect(l,t,r,b,13,13,p);p.setStyle(Paint.Style.FILL);
            drawText(c,label,getWidth()*((x1+x2)/2f),getHeight()*((y1+y2)/2f)+7,17,Color.WHITE,Paint.Align.CENTER);
        }

        private void drawBackdrop(Canvas c,int shade) {
            if(gardenBackground!=null){spriteRect.set(0,0,getWidth(),getHeight());c.drawBitmap(gardenBackground,null,spriteRect,p);}
            else c.drawColor(Color.rgb(28,70,38));
            p.setColor(Color.argb(shade,0,0,0));c.drawRect(0,0,getWidth(),getHeight(),p);
        }

        private void drawText(Canvas c,String s,float x,float y,float size,int color,Paint.Align align) {
            text.setTextAlign(align);text.setTextSize(size);text.setColor(color);c.drawText(s,x,y,text);
        }

        

        private void releaseBinuSounds() {
            stopBinuSound1();
            stopBinuSound2();
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            if(e.getAction()!=MotionEvent.ACTION_UP)return true;
            refreshDaily();
            float x=e.getX(),y=e.getY();
            if(screen==HOME){
                if(inside(x,y,.70f,.025f,.79f,.135f))screen=DAILY_QUEST;
                else if(inside(x,y,.83f,.535f,.96f,.605f))screen=DAILY_QUEST;
                else if(inside(x,y,.805f,.025f,.895f,.135f))screen=DAILY_REWARDS;
                else if(inside(x,y,.91f,.025f,.99f,.135f))screen=ADMIN;
                else if(inside(x,y,.012f,.22f,.135f,.315f)||inside(x,y,.18f,.72f,.34f,.90f))screen=WORLD;
                else if(inside(x,y,.012f,.335f,.135f,.43f)||inside(x,y,.36f,.72f,.52f,.90f))screen=MINIGAME_MENU;
                else if(inside(x,y,.012f,.45f,.135f,.545f)||inside(x,y,.54f,.72f,.70f,.90f))startSandbox();
                else if(inside(x,y,.012f,.565f,.135f,.66f)){if(coins>=100){coins-=100;food++;saveGame();}}
                else if(inside(x,y,.012f,.68f,.135f,.775f))MainActivity.this.showFinalVideo();
                else if(inside(x,y,.012f,.795f,.135f,.89f))screen=DAILY_REWARDS;
                else if(inside(x,y,.17f,.59f,.22f,.66f))worldPage=Math.max(0,worldPage-1);
                else if(inside(x,y,.72f,.59f,.77f,.66f))worldPage=Math.min(2,worldPage+1);
                else{
                    int index=worldPage*4;
                    for(int i=0;i<4;i++){float bx=.17f+i*.16f;if(inside(x,y,bx,.25f,bx+.145f,.57f)){
                        int world=index+i;
                        if(world<WORLD_COUNT){selectedWorld=world;int first=world*LEVELS_PER_WORLD+1;
                            if(unlocked>=first)screen=LEVEL;else Toast.makeText(MainActivity.this,"Thế giới này chưa mở khóa",Toast.LENGTH_SHORT).show();}
                        break;
                    }}
                }
                saveGame();invalidate();return true;
            }
            if(screen==WORLD){
                if(inside(x,y,.035f,.90f,.18f,.98f)){screen=HOME;invalidate();return true;}
                for(int world=0;world<WORLD_COUNT;world++){
                    int col=world%5,row=world/5;float bx=.035f+col*.19f,by=.23f+row*.34f;
                    if(inside(x,y,bx,by,bx+.17f,by+.27f)){
                        int first=world*LEVELS_PER_WORLD+1;
                        if(unlocked>=first){selectedWorld=world;screen=LEVEL;}
                        else Toast.makeText(MainActivity.this,"Hoàn thành thế giới trước để mở khóa",Toast.LENGTH_SHORT).show();
                        invalidate();return true;
                    }
                }
                return true;
            }
            if(screen==LEVEL){
                if(inside(x,y,.035f,.88f,.18f,.97f)){screen=WORLD;invalidate();return true;}
                if(inside(x,y,.82f,.88f,.965f,.97f)){screen=HOME;invalidate();return true;}
                int first=selectedWorld*LEVELS_PER_WORLD+1;
                for(int local=1;local<=LEVELS_PER_WORLD;local++){
                    int col=(local-1)%5,row=(local-1)/5;float bx=.07f+col*.18f,by=.21f+row*.16f;
                    if(inside(x,y,bx,by,bx+.15f,by+.115f)){
                        int number=first+local-1;
                        if(number<=unlocked)startLevel(number);
                        else Toast.makeText(MainActivity.this,"Màn này đang khóa",Toast.LENGTH_SHORT).show();
                        invalidate();return true;
                    }
                }
                return true;
            }
            if(screen==DAILY_QUEST){
                if(inside(x,y,.035f,.86f,.19f,.96f))screen=HOME;
                else if(inside(x,y,.35f,.86f,.65f,.96f))claimDailyQuest();
                invalidate();return true;
            }
            if(screen==DAILY_REWARDS){
                if(inside(x,y,.035f,.86f,.19f,.96f))screen=HOME;
                else if(inside(x,y,.35f,.80f,.65f,.91f))claimDailyReward();
                invalidate();return true;
            }
            if(screen==MINIGAME_MENU){
                if(inside(x,y,.035f,.87f,.19f,.97f))screen=HOME;
                else for(int i=0;i<6;i++){int col=i%3,row=i/3;float bx=.12f+col*.26f,by=.23f+row*.30f;
                    if(inside(x,y,bx,by,bx+.22f,by+.22f)){startMiniGame(i+1);break;}}
                invalidate();return true;
            }
            if(screen==MINIGAME_PLAY){
                if(inside(x,y,.88f,.04f,.98f,.15f)){screen=MINIGAME_MENU;miniTargets.clear();invalidate();return true;}
                Iterator<MiniTarget> it=miniTargets.iterator();
                while(it.hasNext()){MiniTarget target=it.next();if(Math.hypot(x-target.x,y-target.y)<=target.radius*1.35f){
                    miniScore+=miniMode==1?10:miniMode==2?20:15;it.remove();invalidate();return true;}}
                return true;
            }
            if(screen==MINIGAME_RESULT){
                if(inside(x,y,.30f,.52f,.70f,.62f))startMiniGame(miniMode);
                else if(inside(x,y,.30f,.67f,.70f,.77f))screen=MINIGAME_MENU;
                invalidate();return true;
            }
            if(screen==ADMIN){
                if(inside(x,y,.04f,.84f,.20f,.94f))screen=HOME;
                else if(inside(x,y,.30f,.46f,.70f,.57f))showAdminCommandDialog();
                else if(inside(x,y,.30f,.61f,.70f,.72f)){runAdminCommand("/unlock_all_maps");Toast.makeText(MainActivity.this,"Đã mở khóa 200 bản đồ",Toast.LENGTH_SHORT).show();}
                invalidate();return true;
            }
            if(screen==PAUSE){
                if(inside(x,y,.32f,.42f,.68f,.52f)){screen=PLAY;lastUpdate=System.currentTimeMillis();}
                else if(inside(x,y,.32f,.55f,.68f,.65f)){if(sandboxMode)startSandbox();else startLevel(level);}
                else if(inside(x,y,.32f,.68f,.68f,.78f)){screen=HOME;sandboxMode=false;}
                invalidate();return true;
            }
            if(screen==WIN){
                if(inside(x,y,.32f,.42f,.68f,.52f)){if(level<MAX_LEVEL)startLevel(level+1);else screen=HOME;}
                else if(inside(x,y,.32f,.55f,.68f,.65f))startLevel(level);
                else if(inside(x,y,.32f,.68f,.68f,.78f))screen=HOME;
                invalidate();return true;
            }
            if(screen==LOSE){
                if(inside(x,y,.32f,.42f,.68f,.52f))startLevel(level);
                else if(inside(x,y,.32f,.68f,.68f,.78f))screen=HOME;
                invalidate();return true;
            }
            if(inside(x,y,.88f,.105f,.93f,.21f)){screen=PAUSE;invalidate();return true;}
            if(inside(x,y,.94f,.105f,.99f,.21f)){speed=speed==1?2:1;invalidate();return true;}
            Iterator<SunDrop> sunIt=suns.iterator();
            while(sunIt.hasNext()){SunDrop drop=sunIt.next();if(Math.hypot(x-drop.x,y-drop.y)<Math.max(45,getHeight()*.055f)){
                if(!sandboxMode){sun+=100;dailySuns=Math.min(999999,dailySuns+100);}
                sunIt.remove();saveGame();invalidate();return true;}}
            float cy=getHeight()*.105f,ch=getHeight()*.105f;
            if(y>=cy&&y<=cy+ch){
                for(int i=0;i<cardTypes.length;i++){float bx=getWidth()*(.01f+i*.052f);
                    if(x>=bx&&x<=bx+getWidth()*.047f){if(isPlantUnlocked(cardTypes[i])){selected=cardTypes[i];tool=TOOL_NONE;}invalidate();return true;}}
                if(inside(x,y,.82f,.105f,.87f,.21f)){tool=tool==TOOL_SHOVEL?TOOL_NONE:TOOL_SHOVEL;invalidate();return true;}
                if(inside(x,y,.535f,.105f,.63f,.21f)){if(food>0)tool=tool==TOOL_PLANTFOOD?TOOL_NONE:TOOL_PLANTFOOD;invalidate();return true;}
            }
            if(inside(x,y,.64f,.105f,.81f,.21f)){if(coins>=100){coins-=100;food++;saveGame();}invalidate();return true;}
            if(x>=left&&x<left+COLS*cellW&&y>=top&&y<top+ROWS*cellH){
                int col=(int)((x-left)/cellW),row=(int)((y-top)/cellH);
                if(row<0||row>=ROWS||col<0||col>=COLS||!activeRow(row))return true;
                if(tool==TOOL_SHOVEL){plants[row][col]=null;tool=TOOL_NONE;}
                else if(tool==TOOL_PLANTFOOD){if(plants[row][col]!=null&&food>0)usePlantFood(plants[row][col]);tool=TOOL_NONE;}
                else if(plants[row][col]==null&&isPlantUnlocked(selected)){int cost=plantCost(selected);
                    if(sandboxMode||sun>=cost){if(!sandboxMode)sun-=cost;plants[row][col]=new Plant(selected,row,col);if(selected==BINU)playBinuSound1();}}
                saveGame();invalidate();return true;
            }
            return true;
        }

                        private void startMiniGame(int mode) {
            miniMode=Math.max(1,Math.min(6,mode));miniScore=0;miniTargets.clear();
            miniStartedAt=System.currentTimeMillis();miniLastSpawn=miniStartedAt;miniLastFrame=miniStartedAt;screen=MINIGAME_PLAY;
        }

                        private void updateMiniGame() {
            long now=System.currentTimeMillis();
            if(now-miniStartedAt>=30000L){screen=MINIGAME_RESULT;coins=Math.min(99999999,coins+miniScore/10);saveGame();return;}
            float dt=Math.min(.05f,Math.max(0f,(now-miniLastFrame)/1000f));miniLastFrame=now;
            long interval=miniMode==1?620L:miniMode==2?820L:miniMode==3?540L:miniMode==4?720L:miniMode==5?650L:500L;
            if(now-miniLastSpawn>=interval&&miniTargets.size()<24){
                miniLastSpawn=now;float radius=Math.max(18f,Math.min(getWidth(),getHeight())*.035f);
                if(miniMode==1)miniTargets.add(new MiniTarget(getWidth()*(.10f+random.nextFloat()*.80f),-radius,radius,1,getHeight()*.42f));
                else if(miniMode==2)miniTargets.add(new MiniTarget(getWidth()+radius,getHeight()*(.24f+random.nextFloat()*.65f),radius,2,getWidth()*.33f));
                else if(miniMode==3)miniTargets.add(new MiniTarget(-radius,getHeight()*(.22f+random.nextFloat()*.65f),radius,3,getWidth()*.48f));
                else if(miniMode==4)miniTargets.add(new MiniTarget(getWidth()*(.08f+random.nextFloat()*.84f),-radius,radius,4,getHeight()*.34f));
                else if(miniMode==5)miniTargets.add(new MiniTarget(getWidth()+radius,getHeight()*(.24f+random.nextFloat()*.65f),radius,5,getWidth()*.40f));
                else miniTargets.add(new MiniTarget(random.nextBoolean()?-radius:getWidth()+radius,
                        getHeight()*(.18f+random.nextFloat()*.62f),radius,random.nextInt(5)+1,getWidth()*.55f));
            }
            Iterator<MiniTarget> it=miniTargets.iterator();
            while(it.hasNext()){
                MiniTarget target=it.next();
                if(miniMode==1||miniMode==4){target.y+=target.speed*dt;if(target.y>getHeight()+target.radius)it.remove();}
                else if(miniMode==2||miniMode==5){target.x-=target.speed*dt;if(target.x< -target.radius)it.remove();}
                else if(miniMode==3){target.x+=target.speed*dt;if(target.x>getWidth()+target.radius)it.remove();}
                else{target.x+=target.x<0?target.speed*dt:-target.speed*dt;target.y+=target.speed*dt*.12f;
                    if(target.x< -target.radius||target.x>getWidth()+target.radius||target.y>getHeight()+target.radius)it.remove();}
            }
        }

                        private void drawMiniGameMenu(Canvas c) {
            drawBackdrop(c,45);
            drawText(c,"VƯỜN MINI GAME",getWidth()/2f,getHeight()*.10f,29,Color.WHITE,Paint.Align.CENTER);
            drawText(c,"6 thử thách ngắn • Chạm mục tiêu để kiếm xu",getWidth()/2f,getHeight()*.16f,14,Color.YELLOW,Paint.Align.CENTER);
            String[] labels={"SUN RUSH","ZOMBIE TAP","PEA RUSH","NUT CATCH","PF HUNT","MIXED RUSH"};
            for(int i=0;i<6;i++){int col=i%3,row=i/3;float x=.12f+col*.26f,y=.23f+row*.30f;button(c,x,y,x+.22f,y+.22f,labels[i]);}
            button(c,.035f,.87f,.19f,.97f,"QUAY LẠI");
        }

                        private void drawMiniGame(Canvas c) {
            drawBackdrop(c,35);
            long remain=Math.max(0L,30000L-(System.currentTimeMillis()-miniStartedAt));
            String[] labels={"","SUN RUSH","ZOMBIE TAP","PEA RUSH","NUT CATCH","PF HUNT","MIXED RUSH"};
            drawText(c,labels[miniMode],getWidth()*.06f,getHeight()*.10f,22,Color.WHITE,Paint.Align.LEFT);
            drawText(c,"ĐIỂM "+miniScore,getWidth()*.50f,getHeight()*.10f,20,Color.YELLOW,Paint.Align.CENTER);
            drawText(c,"GIÂY "+(remain/1000L),getWidth()*.82f,getHeight()*.10f,19,Color.WHITE,Paint.Align.CENTER);
            button(c,.88f,.04f,.98f,.15f,"THOÁT");
            for(MiniTarget target:miniTargets){
                Bitmap image=target.kind==1?sunflowerImg:target.kind==2?zombieImg:target.kind==3?bulletImg:
                        target.kind==4?giganutImg:peaFoodImg;
                if(image!=null){spriteRect.set(target.x-target.radius,target.y-target.radius,target.x+target.radius,target.y+target.radius);
                    spritePaint.setAlpha(255);spritePaint.setColorFilter(null);c.drawBitmap(image,null,spriteRect,spritePaint);}
                else{p.setColor(target.kind==1?Color.YELLOW:target.kind==2?Color.GRAY:Color.GREEN);c.drawCircle(target.x,target.y,target.radius,p);}
            }
        }

        private void drawMiniGameResult(Canvas c) {
            drawBackdrop(c, 105);
            drawText(c, "HOÀN THÀNH MINI GAME",
                    getWidth() / 2f, getHeight() * .30f, 30,
                    Color.WHITE, Paint.Align.CENTER);
            drawText(c, "Điểm: " + miniScore + "   •   Xu nhận: " + (miniScore / 10),
                    getWidth() / 2f, getHeight() * .42f, 20,
                    Color.YELLOW, Paint.Align.CENTER);
            button(c, .30f, .52f, .70f, .62f, "CHƠI LẠI");
            button(c, .30f, .67f, .70f, .77f, "MENU MINI GAME");
        }

        private void drawAdmin(Canvas c) {
            drawBackdrop(c, 125);
            drawText(c, "ADMIN PANEL", getWidth() / 2f,
                    getHeight() * .17f, 33, Color.YELLOW, Paint.Align.CENTER);
            drawText(c, "Lệnh nhanh và công cụ kiểm tra tiến trình",
                    getWidth() / 2f, getHeight() * .27f, 16,
                    Color.WHITE, Paint.Align.CENTER);
            drawText(c, "Lệnh mở toàn bộ bản đồ: /unlock_all_maps",
                    getWidth() / 2f, getHeight() * .38f, 17,
                    Color.rgb(170, 230, 190), Paint.Align.CENTER);
            drawText(c, "FPS khi chơi gần nhất: " + fpsLast + " / 60",
                    getWidth() / 2f, getHeight() * .87f, 15,
                    fpsLast >= 58 ? Color.GREEN : Color.YELLOW, Paint.Align.CENTER);
            button(c, .30f, .46f, .70f, .57f, "NHẬP LỆNH ADMIN");
            button(c, .30f, .61f, .70f, .72f, "MỞ KHÓA 200 MAP");
            button(c, .04f, .84f, .20f, .94f, "QUAY LẠI");
        }

        private String runAdminCommand(String raw) {
            if (raw == null) return "Lệnh trống";
            String command = raw.trim().toLowerCase(Locale.ROOT);
            try {
                if (command.equals("/unlock_all_maps")) {
                    unlocked = MAX_LEVEL;saveGame();return "Đã mở khóa toàn bộ 200 bản đồ";
                }
                if (command.equals("/reset_maps")) {
                    level = 1;
                    unlocked = 1;
                    saveGame();
                    return "Đã đặt lại tiến trình bản đồ";
                }
                if (command.startsWith("/give_sun ")) {
                    sun = Math.min(999999, sun + Math.max(0, Integer.parseInt(command.substring(10).trim())));
                    saveGame();
                    return "Đã cộng SUN";
                }
                if (command.startsWith("/give_pf ")) {
                    food = Math.min(99999, food + Math.max(0, Integer.parseInt(command.substring(9).trim())));
                    saveGame();
                    return "Đã cộng Plant Food";
                }
                if (command.startsWith("/give_coins ")) {
                    coins = Math.min(99999999, coins + Math.max(0, Integer.parseInt(command.substring(12).trim())));
                    saveGame();
                    return "Đã cộng xu";
                }
                if (command.startsWith("/set_level ")) {
                    level = Math.max(1, Math.min(MAX_LEVEL,
                            Integer.parseInt(command.substring(11).trim())));
                    unlocked = Math.max(unlocked, level);
                    saveGame();
                    return "Đã đặt màn hiện tại: " + level;
                }
                if (command.equals("/help")) {
                    return "/unlock_all_maps, /give_sun 5000, /give_pf 10, /give_coins 1000, /set_level 200, /reset_maps";
                }
            } catch (NumberFormatException ignored) {
                return "Giá trị lệnh không hợp lệ";
            }
            return "Không nhận diện lệnh. Dùng /help";
        }

        private boolean inside(
                float x,
                float y,
                float x1,
                float y1,
                float x2,
                float y2
        ) {
            return x >= getWidth() * x1 &&
                    x <= getWidth() * x2 &&
                    y >= getHeight() * y1 &&
                    y <= getHeight() * y2;
        }

        class MiniTarget {
            float x;
            float y;
            float radius;
            int kind;
            float speed;

            MiniTarget(float x, float y, float radius, int kind, float speed) {
                this.x = x;
                this.y = y;
                this.radius = radius;
                this.kind = kind;
                this.speed = speed;
            }
        }



        class Plant {
            int type;
            int row;
            int col;

            float hp;
            float maxHp;

            long last;
            long secondShot;
            long plantFoodUntil;
            long armTime;
            long animClock;
            long shootUntil,nextFoodShotAt,nextAttackAt,attackResolveAt;
            int animFrame,foodShotsRemaining;
            Zombie chompTarget;
            boolean foodGiantShotDone,armed,foodUsed;

            Plant(int type, int row, int col) {
                this.type = type;
                this.row = row;
                this.col = col;

                maxHp =
                        type == GIGANUT ? 4000 :
                        type == MINE ? 120 :
                        500;

                hp = maxHp;

                last = System.currentTimeMillis();
                animFrame = 0;
                animClock = last;

                if (type == MINE) {
                    armTime = last + 14000;
                    armed = false;
                } else {
                    armTime = Long.MAX_VALUE;
                    armed = true;
                }
            }
        }

        class Zombie {
            float x;
            float y;

            float speed;
            float hp;
            float maxHp;
            int row;
            int type;
            int damage;

            boolean boss;
            boolean dummy;

            long lastAttack;
            long stunnedUntil;
            long bornAt;
            long lastHitAt;

            Zombie(int row,int type,boolean boss){
                this.row=row;this.type=type;this.boss=boss;
                x=left+COLS*cellW+cellW;y=top+row*cellH+cellH*.58f;
                bornAt=System.currentTimeMillis();
                if(boss){
                    maxHp=2200f+Math.max(0,(level/5)-1)*600f;
                    speed=cellW*.052f;damage=32+(level/5)*2;
                }else if(type==1){
                    maxHp=340f+level*14f;speed=cellW*.070f;damage=22;
                }else if(type==2){
                    maxHp=130f+level*8f;speed=cellW*.155f;damage=14;
                }else if(type==3){
                    maxHp=380f+level*16f;speed=cellW*.084f;damage=22;
                }else if(type==4){
                    maxHp=700f+level*28f;speed=cellW*.048f;damage=36;
                }else if(type==5){
                    maxHp=190f+level*10f;speed=cellW*.190f;damage=15;
                }else{
                    maxHp=220f+level*13f;speed=cellW*.105f;damage=18;
                }
                hp=maxHp;
            }
        }

        class Pea {
            float x;
            float y;

            int row;
            int damage;
            int direction;

            boolean big;
            boolean fromZombie;
            int kind,splashDamage;
            long stunMs;
            float startX,startY,targetX,targetY,progress,duration=.72f;

            Pea(
                    float x,
                    float y,
                    int row,
                    int damage,
                    boolean big,
                    boolean fromZombie,
                    int direction
            ) {
                this.x = x;
                this.y = y;
                this.row = row;
                this.damage = damage;
                this.big = big;
                this.fromZombie = fromZombie;
                this.direction = direction;
            }

            Pea(float x,float y,int row,int damage,int kind,int splashDamage,long stunMs,float targetX,float targetY){
                this.x=x;this.y=y;this.row=row;this.damage=damage;this.kind=kind;this.splashDamage=splashDamage;this.stunMs=stunMs;
                this.startX=x;this.startY=y;this.targetX=targetX;this.targetY=targetY;this.big=kind==4;
            }
        }

        class SunDrop {
            float x, y, targetY;
            long expiresAt;
            boolean falling;
            SunDrop(float x, float y) {
                this.x=x; this.y=y; this.targetY=y; this.falling=false;
                this.expiresAt=System.currentTimeMillis()+12000L;
            }
            SunDrop(float x, float y, float targetY) {
                this.x=x; this.y=y; this.targetY=targetY; this.falling=true;
                this.expiresAt=System.currentTimeMillis()+20000L;
            }
        }

        class Mower {
            int row;
            float x;

            boolean used;
            boolean active;

            Mower(int row) {
                this.row = row;
                this.x = left - cellW * .30f;
            }
        }
    }
}