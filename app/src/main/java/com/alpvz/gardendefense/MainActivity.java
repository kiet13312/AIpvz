package com.alpvz.gardendefense;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.*;
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
                .apply();
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

        static final int HOME = 0;
        static final int LEVEL = 1;
        static final int PLAY = 2;
        static final int PAUSE = 3;
        static final int WIN = 4;
        static final int LOSE = 5;

        static final int TOOL_NONE = 0;
        static final int TOOL_SHOVEL = 1;
        static final int TOOL_PLANTFOOD = 2;

        static final int MAX_LEVEL = 15;
        static final int MINIGAME_MENU = 6;
        static final int MINIGAME_PLAY = 7;
        static final int MINIGAME_RESULT = 8;
        static final int ADMIN = 9;

        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        final RectF spriteRect = new RectF();
        final Random random = new Random();

        final Plant[][] plants = new Plant[ROWS][COLS];
        final ArrayList<Zombie> zombies = new ArrayList<>();
        final ArrayList<Pea> peas = new ArrayList<>();
        final ArrayList<SunDrop> suns = new ArrayList<>();
        final Mower[] mowers = new Mower[ROWS];
        final int[] cardTypes = {SUNFLOWER, PEASHOOTER, GIGANUT, CHOMPER, REPEATER, MINE, BINU};

        Bitmap sunflowerImg;
        Bitmap peashooterImg;
        Bitmap giganutImg;
        Bitmap chomperImg;
        Bitmap repeaterImg;
        Bitmap mineImg;
        Bitmap peaFoodImg;
        Bitmap repeaterFoodImg;
        Bitmap gigaFoodImg;
        Bitmap zombieImg;
        Bitmap bulletImg;
        Bitmap[] peaAnim = new Bitmap[10];
        Bitmap[] peaFrames = new Bitmap[6], sunflowerFrames = new Bitmap[6], chomperFrames = new Bitmap[6];
        Bitmap[] mineFrames = new Bitmap[3];
        Bitmap gardenBackground;
        Bitmap zombieNormalScaled, zombieBossScaled, bulletSmallScaled, bulletBigScaled;
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
            sunflowerFrames = loadFrames("plant_sun", 6);
            chomperFrames = loadFrames("plant_chomper", 6);
            mineFrames = loadFrames("plant_mine", 3);
            gardenBackground = loadBase64Bitmap("garden_bg");
            sunflowerImg = sunflowerFrames[0] != null ? sunflowerFrames[0] : oldSun;
            peashooterImg = peaFrames[0] != null ? peaFrames[0] : oldPea;
            chomperImg = chomperFrames[0] != null ? chomperFrames[0] : oldChomper;
            repeaterImg = oldRepeater;
            mineImg = mineFrames[0] != null ? mineFrames[0] : oldMine;
            peaFoodImg = loadImage("peashootplantfood");
            repeaterFoodImg = loadImage("repeaterplantfood");
            giganutImg = loadImage("giganut");
            gigaFoodImg = loadImage("giganutplantfood");
            zombieImg = loadImage("zomplatz");
            bulletImg = loadImage("gigapea");
            binuImg = loadImage("binu"); binu1Img = loadImage("binu1");
            binu2Img = loadImage("binu2"); binu3Img = loadImage("binu3"); binu4Img = loadImage("binu4");
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldW, int oldH) {
            left = w * 0.18f;
            top = h * 0.25f;
            cellW = (w * 0.78f) / COLS;
            cellH = (h * 0.70f) / ROWS;
            float plantSize = Math.max(1f, Math.min(cellW, cellH));
            zombieNormalScaled = scaleBitmap(zombieImg, cellW * .68f, cellH * .82f);
            zombieBossScaled = scaleBitmap(zombieImg, cellW * 1.20f, cellH * 1.20f);
            bulletSmallScaled = scaleBitmap(bulletImg, 20f, 20f);
            bulletBigScaled = scaleBitmap(bulletImg, 36f, 36f);
            gigaFieldScaled = scaleBitmap(giganutImg, plantSize * .74f, plantSize * .74f);
            gigaCardScaled = scaleBitmap(giganutImg, 44f, 44f);
            gigaFoodFieldScaled = scaleBitmap(gigaFoodImg, plantSize * .74f, plantSize * .74f);
            gigaFoodCardScaled = scaleBitmap(gigaFoodImg, 44f, 44f);
            for (int r = 0; r < ROWS; r++) {
                if (mowers[r] != null && !mowers[r].active) mowers[r].x = left - cellW * .40f;
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

            if (screen == LEVEL) {
                drawLevels(c);
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

                private void drawHome(Canvas c){
            drawBackdrop(c,54);
            float w=getWidth(),h=getHeight();
            p.setColor(Color.argb(218,30,28,20));c.drawRoundRect(w*.57f,h*.105f,w*.97f,h*.93f,24,24,p);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3f);p.setColor(Color.rgb(238,190,92));
            c.drawRoundRect(w*.57f,h*.105f,w*.97f,h*.93f,24,24,p);p.setStyle(Paint.Style.FILL);
            drawText(c,"GARDEN",w*.29f,h*.27f,46,Color.WHITE,Paint.Align.CENTER);
            drawText(c,"DEFENSE",w*.29f,h*.40f,46,Color.rgb(255,211,91),Paint.Align.CENTER);
            drawText(c,"PVZ-STYLE  •  15 MÀN  •  3 BOSS",w*.29f,h*.52f,15,Color.WHITE,Paint.Align.CENTER);
            drawText(c,"☀ "+sun+"    🪙 "+coins,w*.29f,h*.64f,17,Color.YELLOW,Paint.Align.CENTER);
            drawText(c,"PLANT FOOD: "+food,w*.29f,h*.71f,14,Color.rgb(210,255,195),Paint.Align.CENTER);
            button(c,.63f,.17f,.92f,.255f,"CHƠI TIẾP");
            button(c,.63f,.285f,.92f,.37f,"CHỌN BẢN ĐỒ");
            button(c,.63f,.40f,.92f,.485f,"SANDBOX");
            button(c,.63f,.515f,.92f,.60f,"MINI GAME");
            button(c,.63f,.63f,.92f,.715f,"ADMIN PANEL");
            button(c,.63f,.745f,.92f,.83f,"VIDEO +100 XU");
        }

        private void drawLevels(Canvas c) {
            drawBackdrop(c, 105);

            drawText(c, "CHỌN BẢN ĐỒ", getWidth() / 2f,
                    getHeight() * .095f, 31, Color.WHITE, Paint.Align.CENTER);
            drawText(c, "Mở khóa: " + unlocked + "/" + MAX_LEVEL,
                    getWidth() / 2f, getHeight() * .145f, 16,
                    Color.YELLOW, Paint.Align.CENTER);

            for (int i = 1; i <= MAX_LEVEL; i++) {
                int col = (i - 1) % 5;
                int row = (i - 1) / 5;
                float x = .08f + col * .17f;
                float y = .20f + row * .19f;
                boolean open = i <= unlocked;
                boolean boss = i % 5 == 0;

                p.setColor(!open ? Color.rgb(62, 67, 64)
                        : boss ? Color.rgb(153, 91, 37)
                        : Color.rgb(65, 145, 70));
                c.drawRoundRect(getWidth() * x, getHeight() * y,
                        getWidth() * (x + .14f), getHeight() * (y + .12f),
                        12, 12, p);

                if (open) {
                    drawText(c, boss ? "BOSS " + i : "MÀN " + i,
                            getWidth() * (x + .07f),
                            getHeight() * (y + .052f), 17,
                            Color.WHITE, Paint.Align.CENTER);
                    drawText(c, boss ? "TRÙM CUỐI" : worldName(i),
                            getWidth() * (x + .07f),
                            getHeight() * (y + .092f), 9,
                            Color.rgb(255, 238, 180), Paint.Align.CENTER);
                } else {
                    drawText(c, "KHÓA", getWidth() * (x + .07f),
                            getHeight() * (y + .073f), 14,
                            Color.LTGRAY, Paint.Align.CENTER);
                }
            }

            button(c, .04f, .84f, .20f, .94f, "QUAY LẠI");
        }

        private String worldName(int lv) {
            if (lv <= 5) return "BÃI CỎ";
            if (lv <= 10) return "ĐÊM TỐI";
            return "VƯỜN MÁI NHÀ";
        }

        private boolean isBossLevel(int lv) {
            return lv == 5 || lv == 10 || lv == 15;
        }

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
                float x=getWidth()*(.01f+i*.075f),y=getHeight()*.105f;
                float w=getWidth()*.065f,h=getHeight()*.105f;
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
            if (gardenBackground != null) {
                spriteRect.set(0, top, getWidth(), top + ROWS * cellH);
                c.drawBitmap(gardenBackground, null, spriteRect, p);
                for (int r=0;r<ROWS;r++) if (!activeRow(r)) {
                    p.setColor(Color.argb(95,30,35,30));
                    c.drawRect(left,top+r*cellH,left+COLS*cellW,top+(r+1)*cellH,p);
                }
                return;
            }
            int world=(level-1)/5;
            int a=world==0?Color.rgb(111,177,78):world==1?Color.rgb(65,112,81):Color.rgb(157,139,83);
            int b=world==0?Color.rgb(96,160,68):world==1?Color.rgb(53,96,74):Color.rgb(139,119,70);
            for(int r=0;r<ROWS;r++) for(int col=0;col<COLS;col++){
                p.setColor(!activeRow(r)?Color.rgb(55,78,54):((r+col)%2==0?a:b));
                c.drawRect(left+col*cellW,top+r*cellH,left+(col+1)*cellW,top+(r+1)*cellH,p);
            }
        }

        private void drawPlants(Canvas c) {
            for (int r = 0; r < ROWS; r++) {
                for (int col = 0; col < COLS; col++) {
                    Plant plant = plants[r][col];
                    if (plant == null) continue;
                    if (plant.type == BINU && (binuWaiting || binuJumping)) continue;

                    float x = left + col * cellW + cellW / 2f;
                    float y = top + r * cellH + cellH / 2f;

                    drawPlant(
                            c,
                            plant.type,
                            x,
                            y,
                            Math.min(cellW, cellH) * .74f,
                            plant
                    );

                    drawHp(
                            c,
                            x - cellW * .30f,
                            y + cellH * .32f,
                            cellW * .60f,
                            plant.hp,
                            plant.maxHp
                    );
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

                private void drawPlant(Canvas c,int type,float x,float y,float size,Plant plant) {
            Bitmap b=null; long now=System.currentTimeMillis();
            if(type==GIGANUT){
                boolean card=size<Math.min(cellW,cellH)*.55f;
                b=plant!=null&&plant.foodUsed?(card&&gigaFoodCardScaled!=null?gigaFoodCardScaled:gigaFoodFieldScaled):(card&&gigaCardScaled!=null?gigaCardScaled:gigaFieldScaled);
                if(b==null)b=plant!=null&&plant.foodUsed?gigaFoodImg:giganutImg;
            }else if(type==BINU)b=binuImg;
            else if(type==REPEATER)b=plant!=null&&plant.foodUsed&&now<plant.plantFoodUntil?repeaterFoodImg:repeaterImg;
            else{
                Bitmap[] frames=type==SUNFLOWER?sunflowerFrames:type==PEASHOOTER?peaFrames:type==CHOMPER?chomperFrames:type==MINE?mineFrames:null;
                int frame=0;
                if(plant!=null){
                    if(type==PEASHOOTER){if(plant.foodUsed&&now<plant.plantFoodUntil)frame=5;else if(now<plant.shootUntil)frame=Math.max(1,Math.min(4,plant.animFrame));}
                    else if(type==SUNFLOWER&&now<plant.shootUntil)frame=5;
                    else if(type==CHOMPER&&now<plant.shootUntil)frame=Math.max(1,Math.min(5,plant.animFrame));
                    else if(type==MINE)frame=plant.armed?1:0;
                }
                if(frames!=null&&frame<frames.length)b=frames[frame];
                if(b==null)b=type==SUNFLOWER?sunflowerImg:type==PEASHOOTER?peashooterImg:type==CHOMPER?chomperImg:type==MINE?mineImg:null;
            }
            if(b!=null){
                float kick=plant!=null&&now<plant.shootUntil&&(type==PEASHOOTER||type==REPEATER||type==CHOMPER)?1.07f:1f;
                spriteRect.set(x-size*kick/2f,y-size/kick/2f,x+size*kick/2f,y+size/kick/2f);c.drawBitmap(b,null,spriteRect,p);
                if(plant!=null&&plant.foodUsed&&now<plant.plantFoodUntil){
                    p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(2f,size*.035f));p.setColor(Color.argb(210,255,221,90));c.drawCircle(x,y,size*.49f,p);p.setStyle(Paint.Style.FILL);
                }return;
            }
            p.setColor(type==SUNFLOWER?Color.YELLOW:type==GIGANUT?Color.rgb(145,95,55):type==CHOMPER?Color.rgb(145,70,180):Color.rgb(55,175,70));c.drawCircle(x,y,size*.42f,p);
        }

                private void drawZombies(Canvas c){
            for(Zombie z:zombies){
                float w=z.boss?cellW*1.20f:cellW*.68f,h=z.boss?cellH*1.20f:cellH*.82f;
                Bitmap image=z.boss&&zombieBossScaled!=null?zombieBossScaled:zombieNormalScaled!=null?zombieNormalScaled:zombieImg;
                if(image!=null){spriteRect.set(z.x-w/2f,z.y-h*.55f,z.x+w/2f,z.y+h*.45f);c.drawBitmap(image,null,spriteRect,p);}
                else{p.setColor(Color.rgb(80,80,80));spriteRect.set(z.x-w/2f,z.y-h/2f,z.x+w/2f,z.y+h/2f);c.drawOval(spriteRect,p);}
                if(z.dummy)drawText(c,"∞ HP",z.x,z.y-h*.66f,13,Color.YELLOW,Paint.Align.CENTER);
                else drawHp(c,z.x-w/2f,z.y-h*.65f,w,z.hp,z.maxHp);
            }
        }

        private void drawPeas(Canvas c) {
            for (Pea q : peas) {
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
            for (Mower m : mowers) {
                float y = top + m.row * cellH + cellH * .72f;
                float bodyL = m.x - cellW * .26f;
                float bodyR = m.x + cellW * .26f;
                float bodyT = y - cellH * .17f;
                float wheelR = Math.max(3f, cellH * .045f);
                p.setColor(m.used ? Color.DKGRAY : Color.rgb(75, 125, 75));
                c.drawRoundRect(bodyL, bodyT, bodyR, y, 8, 8, p);
                p.setColor(Color.DKGRAY);
                c.drawCircle(bodyL + cellW * .10f, y + wheelR, wheelR, p);
                c.drawCircle(bodyR - cellW * .10f, y + wheelR, wheelR, p);
                p.setStrokeWidth(Math.max(3f, cellW * .025f));
                c.drawLine(bodyR - cellW * .04f, bodyT,
                        bodyR + cellW * .18f, bodyT - cellH * .27f, p);
                c.drawLine(bodyR + cellW * .18f, bodyT - cellH * .27f,
                        bodyR + cellW * .28f, bodyT - cellH * .27f, p);
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
                    if(now-a.last>=7000L){if(!sandboxMode)addSunDrop(new SunDrop(left+col*cellW+cellW/2f,top+r*cellH+cellH*.22f));a.last=now;a.shootUntil=now+500L;a.animFrame=5;}
                }else if(a.type==PEASHOOTER){
                    if(a.foodUsed&&a.foodShotsRemaining>0){int n=0;while(a.foodShotsRemaining>0&&now>=a.nextFoodShotAt&&n<5){fire(r,col,20,false);a.foodShotsRemaining--;a.nextFoodShotAt+=50L;n++;}}
                    else if(!a.foodUsed&&now-a.last>=1500L&&rowHasZombie(r)){fire(r,col,25,false);a.last=now;a.animFrame=1;a.animClock=now;a.shootUntil=now+360L;}
                }else if(a.type==REPEATER){
                    if(a.foodUsed&&a.foodShotsRemaining>0){int n=0;while(a.foodShotsRemaining>0&&now>=a.nextFoodShotAt&&n<6){fire(r,col,20,false);a.foodShotsRemaining--;a.nextFoodShotAt+=34L;n++;}
                        if(a.foodShotsRemaining==0&&!a.foodGiantShotDone){fire(r,col,600,true);a.foodGiantShotDone=true;a.shootUntil=now+450L;}}
                    else if(!a.foodUsed&&now-a.last>=1500L&&rowHasZombie(r)){firePair(r,col,20);a.last=now;a.animFrame=1;a.animClock=now;a.shootUntil=now+300L;}
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

        private void firePair(int row,int col,int damage){
            float x=left+col*cellW+cellW*.56f,y=top+row*cellH+cellH*.50f;
            if(peas.size()>118)peas.subList(0,peas.size()-118).clear();
            peas.add(new Pea(x,y-cellH*.055f,row,damage,false,false,1));
            peas.add(new Pea(x,y+cellH*.055f,row,damage,false,false,1));
        }

                private void updatePlantAnimations(long now){
            for(int r=0;r<ROWS;r++)for(int col=0;col<COLS;col++){
                Plant a=plants[r][col];if(a==null||a.type==GIGANUT||a.type==BINU||a.type==MINE)continue;
                if(a.type==PEASHOOTER){if(now<a.shootUntil&&now-a.animClock>=75L){a.animClock=now;a.animFrame=Math.min(4,a.animFrame+1);}else if(now>=a.shootUntil)a.animFrame=0;}
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
            c.drawBitmap(b,null,spriteRect,p);
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
            for(int i=peas.size()-1;i>=0;i--){Pea q=peas.get(i);q.x+=q.direction*cellW*8.5f*dt;Zombie hit=null;
                for(Zombie z:zombies)if(z.hp>0&&z.row==q.row&&Math.abs(z.x-q.x)<cellW*.30f){hit=z;break;}
                if(hit!=null){if(!hit.dummy)hit.hp-=q.damage;peas.remove(i);}else if(q.x>getWidth()+40)peas.remove(i);}
        }

                private void updateZombies(long now,float dt){
            for(Zombie z:zombies){if(z.hp<=0||z.dummy)continue;Plant plant=plantInFront(z);
                if(plant!=null){if(now-z.lastAttack>=800L){plant.hp-=z.damage;z.lastAttack=now;}}
                else z.x-=z.speed*dt;
                if(z.x<=left-cellW*.45f){Mower mower=mowers[z.row];if(!mower.used){mower.used=true;mower.active=true;mower.x=left-cellW*.40f;}
                    else if(!mower.active){z.hp=0;screen=LOSE;return;}}
            }
        }

        private void updateMowers(float dt) {
            for (Mower m : mowers) {
                if (!m.active) continue;

                m.x += cellW * 17f * dt;

                for (Zombie z : zombies) {
                    if (z.hp > 0 &&
                            z.row == m.row &&
                            Math.abs(z.x - m.x) < cellW * .70f) {
                        z.hp = 0;
                    }
                }

                if (m.x > getWidth() + cellW) {
                    m.active = false;
                }
            }
        }

        private void spawnZombie() {
            int n = activeRows();
            int first = (ROWS - n) / 2;
            int row = first + random.nextInt(n);

            boolean boss =
                    isBossLevel(level) &&
                    spawned == total - 1;

            zombies.add(
                    new Zombie(
                            row,
                            random.nextInt(3),
                            boss
                    )
            );
        }

                private void removeDead(){
            boolean earned=false;
            for(int i=zombies.size()-1;i>=0;i--){Zombie z=zombies.get(i);if(!z.dummy&&z.hp<=0){zombies.remove(i);killed++;
                int reward=z.boss?60+random.nextInt(141):3+random.nextInt(18);coins=Math.min(99999999,coins+reward);earned=true;}}
            for(int r=0;r<ROWS;r++)for(int col=0;col<COLS;col++)if(plants[r][col]!=null&&plants[r][col].hp<=0)plants[r][col]=null;
            if(earned)saveGame();
        }

                private boolean rowHasZombie(int row){
            for(Zombie z:zombies)if(!z.dummy&&z.hp>0&&z.row==row&&z.x>left)return true;
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

                private void startLevel(int lv){
            sandboxMode=false;binuFoodBoost=false;speed=1;
            level=Math.max(1,Math.min(MAX_LEVEL,lv));screen=PLAY;sun=500;killed=0;spawned=0;
            total=isBossLevel(level)?15+(level/5)*2:8+(level-1)/2;
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
            if (level == 1) return 1;
            if (level <= 3) return 3;
            return 5;
        }

        private boolean activeRow(int row) {
            int n = activeRows();
            int first = (ROWS - n) / 2;
            return row >= first && row < first + n;
        }
        private long spawnDelay() {
            if (level <= 2) return 4200;
            if (level <= 4) return 3600;
            if (level <= 5) return 3100;
            if (level <= 10) return 2800;
            return 2450;
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
            return false;
        }

        private int plantCost(int type) {
            if (type == SUNFLOWER) return 50;
            if (type == PEASHOOTER) return 100;
            if (type == GIGANUT) return 125;
            if (type == CHOMPER) return 150;
            if (type == REPEATER) return 200;
            if (type == MINE) return 50;
            if (type == BINU) return 500;
            return 99999;
        }

        private void winLevel() { screen=WIN; if(level<MAX_LEVEL)unlocked=Math.max(unlocked,level+1); saveGame(); }

                private void usePlantFood(Plant a){
            if(a==null||a.foodUsed||food<=0)return;
            long now=System.currentTimeMillis();food--;a.foodUsed=true;
            if(a.type==PEASHOOTER||a.type==REPEATER)playPlantFoodSound();
            if(a.type==GIGANUT){a.maxHp=12000f;a.hp=12000f;a.plantFoodUntil=Long.MAX_VALUE;}
            else if(a.type==SUNFLOWER){sun=Math.min(999999,sun+150);a.plantFoodUntil=now+900L;a.shootUntil=now+900L;a.animFrame=5;}
            else if(a.type==PEASHOOTER){a.foodShotsRemaining=60;a.nextFoodShotAt=now;a.plantFoodUntil=now+3600L;a.shootUntil=now+3600L;a.animFrame=5;}
            else if(a.type==REPEATER){a.foodShotsRemaining=90;a.foodGiantShotDone=false;a.nextFoodShotAt=now;a.plantFoodUntil=now+3700L;a.shootUntil=now+3700L;}
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
                @Override
        public boolean onTouchEvent(MotionEvent e){
            if(e.getAction()!=MotionEvent.ACTION_UP)return true;
            float x=e.getX(),y=e.getY();
            if(screen==HOME){
                if(inside(x,y,.63f,.17f,.92f,.255f))startLevel(level);
                else if(inside(x,y,.63f,.285f,.92f,.37f))screen=LEVEL;
                else if(inside(x,y,.63f,.40f,.92f,.485f))startSandbox();
                else if(inside(x,y,.63f,.515f,.92f,.60f))screen=MINIGAME_MENU;
                else if(inside(x,y,.63f,.63f,.92f,.715f))screen=ADMIN;
                else if(inside(x,y,.63f,.745f,.92f,.83f))MainActivity.this.showFinalVideo();
                invalidate();return true;
            }
            if(screen==LEVEL){
                if(inside(x,y,.04f,.84f,.20f,.94f)){screen=HOME;invalidate();return true;}
                for(int i=1;i<=MAX_LEVEL;i++){int col=(i-1)%5,row=(i-1)/5;float bx=.08f+col*.17f,by=.20f+row*.19f;
                    if(inside(x,y,bx,by,bx+.14f,by+.12f)&&i<=unlocked){startLevel(i);invalidate();return true;}}
                return true;
            }
            if(screen==MINIGAME_MENU){
                if(inside(x,y,.04f,.84f,.20f,.94f))screen=HOME;
                else if(inside(x,y,.07f,.34f,.33f,.62f))startMiniGame(1);
                else if(inside(x,y,.37f,.34f,.63f,.62f))startMiniGame(2);
                else if(inside(x,y,.67f,.34f,.93f,.62f))startMiniGame(3);
                invalidate();return true;
            }
            if(screen==MINIGAME_PLAY){
                if(inside(x,y,.88f,.04f,.98f,.15f)){screen=MINIGAME_MENU;miniTargets.clear();invalidate();return true;}
                Iterator<MiniTarget> it=miniTargets.iterator();
                while(it.hasNext()){MiniTarget t=it.next();if(Math.hypot(x-t.x,y-t.y)<=t.radius*1.35f){
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
                else if(inside(x,y,.30f,.61f,.70f,.72f)){runAdminCommand("/unlock_all_maps");Toast.makeText(MainActivity.this,"Đã mở khóa 15 bản đồ",Toast.LENGTH_SHORT).show();}
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
            Iterator<SunDrop> si=suns.iterator();
            while(si.hasNext()){SunDrop drop=si.next();if(Math.hypot(x-drop.x,y-drop.y)<Math.max(45,getHeight()*.055f)){
                if(!sandboxMode)sun+=100;si.remove();saveGame();invalidate();return true;}}
            float cy=getHeight()*.105f,ch=getHeight()*.105f;
            if(y>=cy&&y<=cy+ch){
                int[] types={SUNFLOWER,PEASHOOTER,GIGANUT,CHOMPER,REPEATER,MINE,BINU};
                for(int i=0;i<types.length;i++){float bx=getWidth()*(.01f+i*.075f);
                    if(x>=bx&&x<=bx+getWidth()*.065f){if(isPlantUnlocked(types[i])){selected=types[i];tool=TOOL_NONE;}invalidate();return true;}}
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

                private void startMiniGame(int mode){
            miniMode=Math.max(1,Math.min(3,mode));miniScore=0;miniTargets.clear();
            miniStartedAt=System.currentTimeMillis();miniLastSpawn=miniStartedAt;miniLastFrame=miniStartedAt;screen=MINIGAME_PLAY;
        }

                private void updateMiniGame(){
            long now=System.currentTimeMillis();
            if(now-miniStartedAt>=30000L){screen=MINIGAME_RESULT;coins+=miniScore/10;saveGame();return;}
            float dt=Math.min(.05f,Math.max(0f,(now-miniLastFrame)/1000f));miniLastFrame=now;
            long interval=miniMode==1?620L:miniMode==2?820L:540L;
            if(now-miniLastSpawn>=interval&&miniTargets.size()<20){
                miniLastSpawn=now;float rad=Math.max(18f,Math.min(getWidth(),getHeight())*.035f);
                if(miniMode==1)miniTargets.add(new MiniTarget(getWidth()*(.10f+random.nextFloat()*.80f),-rad,rad,1,getHeight()*.42f));
                else if(miniMode==2)miniTargets.add(new MiniTarget(getWidth()+rad,getHeight()*(.24f+random.nextFloat()*.65f),rad,2,getWidth()*.33f));
                else miniTargets.add(new MiniTarget(-rad,getHeight()*(.22f+random.nextFloat()*.65f),rad,3,getWidth()*.48f));
            }
            Iterator<MiniTarget> it=miniTargets.iterator();
            while(it.hasNext()){MiniTarget t=it.next();
                if(miniMode==1){t.y+=t.speed*dt;if(t.y>getHeight()+t.radius)it.remove();}
                else if(miniMode==2){t.x-=t.speed*dt;if(t.x< -t.radius)it.remove();}
                else{t.x+=t.speed*dt;t.y+=t.speed*dt*.10f;if(t.x>getWidth()+t.radius)it.remove();}
            }
        }

                private void drawMiniGameMenu(Canvas c){
            drawBackdrop(c,72);drawText(c,"MINI GAMES",getWidth()/2f,getHeight()*.16f,34,Color.WHITE,Paint.Align.CENTER);
            drawText(c,"Chơi 30 giây • Điểm càng cao, xu thưởng càng nhiều",getWidth()/2f,getHeight()*.24f,15,Color.YELLOW,Paint.Align.CENTER);
            button(c,.07f,.34f,.33f,.62f,"SUN RUSH");button(c,.37f,.34f,.63f,.62f,"ZOMBIE TAP");button(c,.67f,.34f,.93f,.62f,"PEA RUSH");
            drawText(c,"Hứng nắng",getWidth()*.20f,getHeight()*.69f,13,Color.WHITE,Paint.Align.CENTER);
            drawText(c,"Chạm mục tiêu",getWidth()*.50f,getHeight()*.69f,13,Color.WHITE,Paint.Align.CENTER);
            drawText(c,"Chạm đậu bay",getWidth()*.80f,getHeight()*.69f,13,Color.WHITE,Paint.Align.CENTER);
            button(c,.04f,.84f,.20f,.94f,"QUAY LẠI");
        }

                private void drawMiniGame(Canvas c){
            drawBackdrop(c,55);long remain=Math.max(0L,30000L-(System.currentTimeMillis()-miniStartedAt));
            String label=miniMode==1?"SUN RUSH":miniMode==2?"ZOMBIE TAP":"PEA RUSH";
            drawText(c,label,getWidth()*.06f,getHeight()*.10f,23,Color.WHITE,Paint.Align.LEFT);
            drawText(c,"ĐIỂM "+miniScore,getWidth()*.50f,getHeight()*.10f,22,Color.YELLOW,Paint.Align.CENTER);
            drawText(c,"GIÂY "+(remain/1000L),getWidth()*.82f,getHeight()*.10f,20,Color.WHITE,Paint.Align.CENTER);
            button(c,.88f,.04f,.98f,.15f,"THOÁT");
            for(MiniTarget t:miniTargets){Bitmap image=t.kind==1?sunflowerImg:t.kind==2?zombieImg:bulletImg;
                if(image!=null){spriteRect.set(t.x-t.radius,t.y-t.radius,t.x+t.radius,t.y+t.radius);c.drawBitmap(image,null,spriteRect,p);}
                else{p.setColor(t.kind==1?Color.YELLOW:t.kind==2?Color.GRAY:Color.GREEN);c.drawCircle(t.x,t.y,t.radius,p);}}
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
            button(c, .30f, .61f, .70f, .72f, "MỞ KHÓA 15 MAP");
            button(c, .04f, .84f, .20f, .94f, "QUAY LẠI");
        }

        private String runAdminCommand(String raw) {
            if (raw == null) return "Lệnh trống";
            String command = raw.trim().toLowerCase(Locale.ROOT);
            try {
                if (command.equals("/unlock_all_maps")) {
                    unlocked = MAX_LEVEL;
                    saveGame();
                    return "Đã mở khóa toàn bộ 15 bản đồ";
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
                    return "/unlock_all_maps, /give_sun 5000, /give_pf 10, /give_coins 1000, /set_level 15, /reset_maps";
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

            Zombie(int row,int type,boolean boss){
                this.row=row;this.type=type;this.boss=boss;x=left+COLS*cellW+cellW;y=top+row*cellH+cellH*.58f;
                if(boss){maxHp=2800f+Math.max(0,(level/5)-1)*900f;speed=cellW*.048f;damage=34+(level/5)*3;}
                else if(type==1){maxHp=420f+level*18f;speed=cellW*.075f;damage=25;}
                else if(type==2){maxHp=180f+level*10f;speed=cellW*.17f;damage=16;}
                else{maxHp=260f+level*15f;speed=cellW*.12f;damage=20;}hp=maxHp;
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
                this.x = left - cellW * .40f;
            }
        }
    }
}