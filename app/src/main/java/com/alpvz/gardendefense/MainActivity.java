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

    private void showFinalVideo() {
        hideFinalVideo();
        try {
            if(root==null||game==null)return;
            int id=getResources().getIdentifier("win","raw",getPackageName());
            if(id==0){Toast.makeText(this,"Không tìm thấy video win.mp4",Toast.LENGTH_SHORT).show();return;}
            game.screen=GameView.HOME;
            winVideo=new VideoView(MainActivity.this);winVideo.setBackgroundColor(Color.BLACK);
            winVideo.setVideoURI(Uri.parse("android.resource://"+getPackageName()+"/"+id));
            root.addView(winVideo,new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,FrameLayout.LayoutParams.MATCH_PARENT));
            winVideo.setOnCompletionListener(mp->{hideFinalVideo();if(game!=null){game.coins=Math.min(99999999,game.coins+100);saveGame();game.screen=GameView.HOME;game.invalidate();Toast.makeText(MainActivity.this,"+100 xu",Toast.LENGTH_SHORT).show();}});
            winVideo.setOnErrorListener((mp,what,extra)->{hideFinalVideo();if(game!=null){game.screen=GameView.HOME;game.invalidate();}Toast.makeText(MainActivity.this,"Không phát được video",Toast.LENGTH_SHORT).show();return true;});
            winVideo.start();
        }catch(Exception ignored){hideFinalVideo();if(game!=null)game.invalidate();}
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
        Bitmap[] peaFrames = new Bitmap[6], sunflowerFrames = new Bitmap[6], chomperFrames = new Bitmap[6], repeaterFrames = new Bitmap[6];
        Bitmap[] mineFrames = new Bitmap[3];
        Bitmap gardenBackground;
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

        MediaPlayer binuSound1Player;
        MediaPlayer binuSound2Player;

        long lastUpdate;
        long spawnClock;
        long lastSun;

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
            coins = Math.max(0, save.getInt("coins", 99999));
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
            repeaterFrames = makeRepeaterFrames(peaFrames);
            gardenBackground = loadBase64Bitmap("garden_bg");
            sunflowerImg = sunflowerFrames[0] != null ? sunflowerFrames[0] : oldSun;
            peashooterImg = peaFrames[0] != null ? peaFrames[0] : oldPea;
            chomperImg = chomperFrames[0] != null ? chomperFrames[0] : oldChomper;
            repeaterImg = repeaterFrames[0] != null ? repeaterFrames[0] : oldRepeater;
            mineImg = mineFrames[0] != null ? mineFrames[0] : oldMine;
            peaFoodImg = peaFrames[5] != null ? peaFrames[5] : loadImage("peashootplantfood");
            repeaterFoodImg = repeaterFrames[5] != null ? repeaterFrames[5] : loadImage("repeaterplantfood");
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
            for (int r = 0; r < ROWS; r++) {
                if (mowers[r] != null && !mowers[r].active) {
                    mowers[r].x = left - cellW * .40f;
                }
            }
        }

        @Override
        protected void onDraw(Canvas c) {
            super.onDraw(c);

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

        private void drawHome(Canvas c) {
            drawBackdrop(c, 72);
            float w = getWidth(), h = getHeight();
            p.setColor(Color.argb(205, 36, 28, 20));
            c.drawRoundRect(w*.59f,h*.12f,w*.97f,h*.94f,22,22,p);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(3f);
            p.setColor(Color.rgb(232,184,91));
            c.drawRoundRect(w*.59f,h*.12f,w*.97f,h*.94f,22,22,p);
            p.setStyle(Paint.Style.FILL);
            drawText(c,"GARDEN",w*.29f,h*.30f,40,Color.WHITE,Paint.Align.CENTER);
            drawText(c,"DEFENSE",w*.29f,h*.43f,40,Color.rgb(255,214,105),Paint.Align.CENTER);
            drawText(c,"15 MÀN  •  3 BOSS",w*.29f,h*.54f,17,Color.WHITE,Paint.Align.CENTER);
            drawText(c,"☀ "+sun+"    🪙 "+coins+"    PF "+food,w*.29f,h*.65f,16,Color.YELLOW,Paint.Align.CENTER);
            button(c,.64f,.25f,.94f,.35f,"CHƠI TIẾP");
            button(c,.64f,.385f,.94f,.485f,"CHỌN BẢN ĐỒ");
            button(c,.64f,.52f,.94f,.62f,"MINI GAME");
            button(c,.64f,.655f,.94f,.755f,"ADMIN PANEL");
            button(c,.64f,.79f,.94f,.89f,"XEM VIDEO +100 XU");
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

        private void drawGame(Canvas c) {
            c.drawColor(Color.rgb(92, 155, 70));

            p.setColor(Color.rgb(77,45,25));c.drawRect(0,0,getWidth(),top,p);
            p.setColor(Color.rgb(137,83,39));c.drawRect(0,0,getWidth(),top*.24f,p);
            p.setColor(Color.rgb(231,183,89));c.drawRect(0,top-2,getWidth(),top,p);

            drawText(c, "☀ " + sun, 14, 34, 23, Color.YELLOW, Paint.Align.LEFT);
            drawText(c, isBossLevel(level) ? "MÀN " + level + " • BOSS" : "MÀN " + level,
                    getWidth() * .27f, 34, 20, isBossLevel(level) ? Color.YELLOW : Color.WHITE, Paint.Align.LEFT);
            drawText(c, "ZOM " + killed + "/" + total, getWidth() * .47f, 34, 20, Color.WHITE, Paint.Align.LEFT);
            drawText(c, "🪙 " + coins, getWidth() * .68f, 34, 20, Color.YELLOW, Paint.Align.LEFT);
            drawText(c, "PF " + food, getWidth() * .86f, 34, 20, Color.WHITE, Paint.Align.LEFT);

            drawCards(c);
            drawBoard(c);
            drawPlants(c);
            drawBinu(c);
            drawPeas(c);
            drawZombies(c);
            drawSuns(c);
            drawMowers(c);

            button(c, .94f, .075f, .99f, .15f, speed == 2 ? "×2" : "▶");
            button(c, .88f, .075f, .93f, .15f, "Ⅱ");

            drawProgress(c);
        }

        private void drawProgress(Canvas c) {
            float x = getWidth() * .18f;
            float y = getHeight() * .215f;
            float w = getWidth() * .64f;
            float h = 10;

            p.setColor(Color.DKGRAY);
            c.drawRoundRect(x, y, x + w, y + h, 8, 8, p);

            float progress = total <= 0 ? 0 : killed / (float) total;

            p.setColor(Color.GREEN);
            c.drawRoundRect(
                    x,
                    y,
                    x + w * Math.min(1f, progress),
                    y + h,
                    8,
                    8,
                    p
            );
        }

        private void drawCards(Canvas c) {
            for(int i=0;i<cardTypes.length;i++){
                int type=cardTypes[i];
                float x=getWidth()*(.01f+i*.075f),y=getHeight()*.075f;
                float w=getWidth()*.065f,h=getHeight()*.09f;
                p.setColor(Color.rgb(83,49,28));c.drawRoundRect(x,y,x+w,y+h,8,8,p);
                p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2f);
                p.setColor(selected==type&&tool==TOOL_NONE?Color.YELLOW:Color.rgb(221,179,99));
                c.drawRoundRect(x,y,x+w,y+h,8,8,p);p.setStyle(Paint.Style.FILL);
                p.setColor(Color.rgb(225,203,151));c.drawRoundRect(x+2,y+2,x+w-2,y+h-2,6,6,p);
                if(isPlantUnlocked(type))drawPlant(c,type,x+w/2f,y+h/2f,Math.min(w,h)*.72f);
                else drawText(c,"🔒",x+w/2f,y+h*.62f,18,Color.DKGRAY,Paint.Align.CENTER);
            }
            p.setColor(tool==TOOL_SHOVEL?Color.YELLOW:Color.rgb(91,54,30));
            c.drawRoundRect(getWidth()*.82f,getHeight()*.075f,getWidth()*.87f,getHeight()*.15f,8,8,p);
            p.setStyle(Paint.Style.STROKE);p.setColor(Color.rgb(232,184,91));
            c.drawRoundRect(getWidth()*.82f,getHeight()*.075f,getWidth()*.87f,getHeight()*.15f,8,8,p);p.setStyle(Paint.Style.FILL);
            drawText(c,"XẺNG",getWidth()*.845f,getHeight()*.122f,12,Color.WHITE,Paint.Align.CENTER);
            p.setColor(tool==TOOL_PLANTFOOD?Color.YELLOW:Color.rgb(91,54,30));
            c.drawRoundRect(getWidth()*.535f,getHeight()*.075f,getWidth()*.63f,getHeight()*.15f,8,8,p);
            p.setStyle(Paint.Style.STROKE);p.setColor(Color.rgb(232,184,91));
            c.drawRoundRect(getWidth()*.535f,getHeight()*.075f,getWidth()*.63f,getHeight()*.15f,8,8,p);p.setStyle(Paint.Style.FILL);
            drawText(c,"PF "+food,getWidth()*.582f,getHeight()*.122f,12,Color.WHITE,Paint.Align.CENTER);
            p.setColor(Color.rgb(91,54,30));c.drawRoundRect(getWidth()*.64f,getHeight()*.075f,getWidth()*.81f,getHeight()*.15f,8,8,p);
            p.setStyle(Paint.Style.STROKE);p.setColor(Color.rgb(232,184,91));
            c.drawRoundRect(getWidth()*.64f,getHeight()*.075f,getWidth()*.81f,getHeight()*.15f,8,8,p);p.setStyle(Paint.Style.FILL);
            drawText(c,"MUA PF",getWidth()*.725f,getHeight()*.105f,11,Color.WHITE,Paint.Align.CENTER);
            drawText(c,"100 xu",getWidth()*.725f,getHeight()*.138f,10,Color.YELLOW,Paint.Align.CENTER);
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
            if(type==GIGANUT) b=plant!=null&&plant.foodUsed?gigaFoodImg:giganutImg;
            else if(type==BINU) b=binuImg;
            else {
                Bitmap[] frames=type==SUNFLOWER?sunflowerFrames:type==PEASHOOTER?peaFrames:
                        type==CHOMPER?chomperFrames:type==REPEATER?repeaterFrames:type==MINE?mineFrames:null;
                int frame=0;
                if(plant!=null){
                    if((type==PEASHOOTER||type==REPEATER)&&(plant.foodUsed||now<plant.shootUntil)) frame=5;
                    else if(type==SUNFLOWER&&plant.foodUsed&&now<plant.plantFoodUntil) frame=5;
                    else if(type==CHOMPER&&now<plant.shootUntil) frame=5;
                    else if(type==MINE&&plant.armed) frame=plant.animFrame%3;
                    else if(type==MINE) frame=0;
                    else frame=Math.max(0,Math.min(4,plant.animFrame));
                }
                if(frames!=null&&frame<frames.length)b=frames[frame];
                if(b==null)b=type==SUNFLOWER?sunflowerImg:type==PEASHOOTER?peashooterImg:
                        type==CHOMPER?chomperImg:type==REPEATER?repeaterImg:type==MINE?mineImg:null;
            }
            if(b!=null){
                float kick=plant!=null&&(type==PEASHOOTER||type==REPEATER)&&now<plant.shootUntil?1.055f:1f;
                spriteRect.set(x-size*kick/2f,y-size/kick/2f,x+size*kick/2f,y+size/kick/2f);
                c.drawBitmap(b,null,spriteRect,p); return;
            }
            p.setColor(type==SUNFLOWER?Color.YELLOW:type==GIGANUT?Color.rgb(145,95,55):
                    type==CHOMPER?Color.rgb(145,70,180):Color.rgb(55,175,70));
            c.drawCircle(x,y,size*.42f,p);
        }

        private void drawZombies(Canvas c) {
            for(Zombie z:zombies){float w=z.boss?cellW*1.20f:cellW*.68f,h=z.boss?cellH*1.20f:cellH*.82f;
                if(zombieImg!=null){spriteRect.set(z.x-w/2f,z.y-h*.55f,z.x+w/2f,z.y+h*.45f);c.drawBitmap(zombieImg,null,spriteRect,p);}
                else{p.setColor(Color.rgb(80,80,80));spriteRect.set(z.x-w/2f,z.y-h/2f,z.x+w/2f,z.y+h/2f);c.drawOval(spriteRect,p);}
                drawHp(c,z.x-w/2f,z.y-h*.65f,w,z.hp,z.maxHp);
            }
        }

        private void drawPeas(Canvas c) {
            for(Pea q:peas){float size=q.big?18:10;if(bulletImg!=null){spriteRect.set(q.x-size,q.y-size,q.x+size,q.y+size);c.drawBitmap(bulletImg,null,spriteRect,p);}
                else{p.setColor(Color.GREEN);c.drawCircle(q.x,q.y,size*.7f,p);}}
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

        private void updateGame() {
            long now=System.currentTimeMillis();
            float dt=Math.min(.033333f,Math.max(0f,(now-lastUpdate)/1000f))*speed;
            lastUpdate=now;
            if(now-lastSun>=20000L){
                lastSun=now;int col=random.nextInt(COLS),row=random.nextInt(ROWS);
                suns.add(new SunDrop(left+col*cellW+cellW/2f,top-24f,top+row*cellH+cellH*.62f));
            }
            updatePlants(now);updatePlantAnimations(now);updateBinu(now);checkBinu(now);
            updatePeas(dt);updateZombies(now,dt);updateMowers(dt);updateSunDrops(dt);removeDead();
            if(spawned<total&&now-spawnClock>=spawnDelay()){spawnZombie();spawned++;spawnClock=now;}
            if(spawned>=total&&zombies.isEmpty()&&killed>=total)winLevel();
        }

        private void updateSunDrops(float dt) {
            float fall=Math.max(1f,cellH*1.45f)*dt;
            for(SunDrop drop:suns)if(drop.falling&&drop.y<drop.targetY){
                drop.y=Math.min(drop.targetY,drop.y+fall);
                if(drop.y>=drop.targetY)drop.falling=false;
            }
        }

        private void updatePlants(long now) {
            for (int r = 0; r < ROWS; r++) {
                for (int col = 0; col < COLS; col++) {
                    Plant a = plants[r][col];
                    if (a == null) continue;

                    if (a.foodUsed && a.type != GIGANUT &&
                            a.plantFoodUntil > 0 &&
                            now >= a.plantFoodUntil) {                        a.foodUsed = false;
                        a.plantFoodUntil = 0;
                    }

                    boolean plantFoodActive =
                            a.type == GIGANUT || now < a.plantFoodUntil;

                    if (a.type == SUNFLOWER) {
                        long cd = plantFoodActive ? 1800 : 7000;

                        if (now - a.last >= cd) {
                            suns.add(
                                    new SunDrop(
                                            left + col * cellW + cellW / 2f,
                                            top + r * cellH + cellH * .22f
                                    )
                            );
                            a.last = now;
                        }
                    }

                    if (a.type == PEASHOOTER) {
                        long cd = plantFoodActive ? 700 : 1500;

                        if (now - a.last >= cd && rowHasZombie(r)) {
                            fire(r, col, 30, false);
                            a.last = now;
                            a.animFrame = 5;
                            a.animClock = now;
                            a.shootUntil = now + 220L;
                        }
                    }

                    if (a.type == REPEATER) {
                        long cd = plantFoodActive ? 700 : 1500;

                        if (now - a.last >= cd && rowHasZombie(r)) {
                            fire(r, col, 30, false);
                            fireDelayed(r, col, 30, 500);
                            a.last = now;
                            a.animFrame = 5;
                            a.animClock = now;
                            a.shootUntil = now + 220L;
                        }
                    }

                    if (a.type == CHOMPER) {
                        long cd = plantFoodActive ? 1800 : 3500;

                        if (now - a.last >= cd) {
                            Zombie z = nearestZombie(r, col, cellW * 1.5f);

                            if (z != null) {
                                z.hp = 0;
                                a.last = now;
                                a.animFrame = 3;
                                a.shootUntil = now + 500L;
                            }
                        }
                    }

                    if (a.type == MINE) {
                        if (!a.armed && now >= a.armTime) {
                            a.armed = true;
                        }

                        if (a.armed) {
                            Zombie z = zombieOnCell(r, col);

                            if (z != null) {
                                z.hp = 0;
                                a.hp = 0;
                            }
                        }
                    }
                }
            }

            // Delayed repeater shots.
            for (int r = 0; r < ROWS; r++) {
                for (int col = 0; col < COLS; col++) {
                    Plant a = plants[r][col];

                    if (a != null && a.secondShot > 0 && now >= a.secondShot) {
                        fire(r, col, 30, false);
                        a.secondShot = 0;
                    }
                }
            }
        }

        private void fire(int row, int col, int damage, boolean big) {
            peas.add(
                    new Pea(
                            left + col * cellW + cellW * .56f,
                            top + row * cellH + cellH * .50f,
                            row,
                            damage,
                            big,
                            false,
                            1
                    )
            );
        }

        private void fireDelayed(
                int row,
                int col,
                int damage,
                long delay
        ) {
            Plant a = plants[row][col];
            if (a != null) {
                a.secondShot = System.currentTimeMillis() + delay;
            }
        }

        private void updatePlantAnimations(long now) {
            for(int r=0;r<ROWS;r++)for(int col=0;col<COLS;col++){
                Plant a=plants[r][col];
                if(a==null||a.type==GIGANUT||a.type==BINU||now<a.shootUntil)continue;
                long interval=(a.type==PEASHOOTER||a.type==REPEATER)?115L:190L;
                if(now-a.animClock>=interval){a.animClock=now;a.animFrame=(a.animFrame+1)%(a.type==MINE?3:5);}
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

        private void checkBinu(long now) {
            if (binuWaiting || binuJumping) return;
            for (int r = 0; r < ROWS; r++) {
                for (int col = 0; col < COLS; col++) {
                    Plant a = plants[r][col];
                    if (a == null || a.type != BINU) continue;
                    float bx = left + col * cellW + cellW / 2f;
                    float by = top + r * cellH + cellH / 2f;
                    Zombie nearest = null;
                    float best = Float.MAX_VALUE;
                    for (Zombie z : zombies) {
                        if (z.hp <= 0 || z.row != r) continue;                        float d = Math.abs(z.x - bx);
                        if (d <= cellW * 1.30f && d < best) {
                            nearest = z;
                            best = d;
                        }
                    }
                    if (nearest != null) {
                        binuRow = r;
                        binuCol = col;
                        binuWaiting = true;
                        binuDetectAt = now + 2000L;
                        binuX = bx;
                        binuY = by;
                        return;
                    }
                }
            }
        }

        private void startBinuJump(long now) {
            if (!binuWaiting || binuRow < 0 || binuCol < 0) return;
            float startX = left + binuCol * cellW + cellW / 2f;
            float startY = top + binuRow * cellH + cellH / 2f;
            Zombie target = null;
            float best = Float.MAX_VALUE;
            for (Zombie z : zombies) {
                if (z.hp <= 0 || z.row != binuRow) continue;
                float d = Math.abs(z.x - startX);
                if (d <= cellW * 1.80f && d < best) {
                    target = z;
                    best = d;
                }
            }
            if (target == null) {
                binuWaiting = false;
                return;
            }
            binuStartX = startX;
            binuStartY = startY;
            binuTargetX = target.x;
            binuTargetY = target.y;
            binuX = startX;
            binuY = startY;
            plants[binuRow][binuCol] = null;
            binuJumping = true;
            binuFrame = 1;
            binuClock = now;
            playBinuSound2();
        }

        private void smashBinu() {
            if (binuRow < 0) return;
            float centerX = binuTargetX;
            float range = cellW * 1.65f;
            for (Zombie z : zombies) {
                if (z.hp <= 0 || z.row != binuRow) continue;
                if (Math.abs(z.x - centerX) <= range) {
                    if (z.boss) z.hp = Math.max(1f, z.hp * .50f);
                    else z.hp = 0;
                }
            }
        }

        private void updatePeas(float dt) {
            for(int i=peas.size()-1;i>=0;i--){
                Pea q=peas.get(i);
                q.x+=q.direction*cellW*8.5f*dt;
                Zombie hit=null;
                for(Zombie z:zombies)if(z.hp>0&&z.row==q.row&&Math.abs(z.x-q.x)<cellW*.30f){hit=z;break;}
                if(hit!=null){hit.hp-=q.damage;peas.remove(i);}
                else if(q.x>getWidth()+40)peas.remove(i);
            }
        }

        private void updateZombies(long now, float dt) {
            for (Zombie z : zombies) {
                if (z.hp <= 0) continue;

                Plant plant = plantInFront(z);

                if (plant != null) {
                    if (now - z.lastAttack >= 800) {
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
                        mower.x = left - cellW * .40f;
                    } else if (!mower.active) {
                        z.hp = 0;
                        screen = LOSE;
                        return;
                    }
                }
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

        private void removeDead() {
            boolean earned=false;
            for(int i=zombies.size()-1;i>=0;i--){
                Zombie z=zombies.get(i);
                if(z.hp<=0){
                    zombies.remove(i);killed++;
                    int reward=z.boss?60+random.nextInt(141):3+random.nextInt(18);
                    coins=Math.min(99999999,coins+reward);earned=true;
                }
            }
            for(int r=0;r<ROWS;r++)for(int col=0;col<COLS;col++)
                if(plants[r][col]!=null&&plants[r][col].hp<=0)plants[r][col]=null;
            if(earned)saveGame();
        }

        private boolean rowHasZombie(int row) {
            for (Zombie z : zombies) {
                if (z.row == row && z.x > left) {
                    return true;
                }
            }
            return false;
        }

        private Plant plantInFront(Zombie z) {
            int col = (int) ((z.x - left) / cellW);

            if (col < 0 || col >= COLS) {
                return null;
            }

            return plants[z.row][col];
        }

        private Zombie zombieOnCell(int row, int col) {
            float x = left + col * cellW + cellW / 2f;

            for (Zombie z : zombies) {
                if (z.row == row &&
                        Math.abs(z.x - x) < cellW * .50f) {
                    return z;
                }
            }

            return null;
        }

        private Zombie nearestZombie(
                int row,
                int col,
                float range
        ) {
            float x = left + col * cellW + cellW / 2f;

            Zombie best = null;
            float bestDistance = Float.MAX_VALUE;

            for (Zombie z : zombies) {
                if (z.row != row) continue;

                float d = Math.abs(z.x - x);

                if (d <= range && d < bestDistance) {
                    best = z;
                    bestDistance = d;
                }
            }

            return best;
        }

        private void startLevel(int lv) {
            level = Math.max(1, Math.min(MAX_LEVEL, lv));
            screen = PLAY;
            sun = 500;

            killed = 0;
            spawned = 0;

            total = isBossLevel(level)
                    ? 15 + (level / 5) * 2
                    : 8 + (level - 1) / 2;

            clearBoard();

            long now = System.currentTimeMillis();
            lastUpdate = now;
            spawnClock = now;
            lastSun = now;

            saveGame();
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

        private boolean isPlantUnlocked(int type) {
            if (type == PEASHOOTER) return true;
            if (type == SUNFLOWER) return level >= 1;
            if (type == GIGANUT) return level >= 2;
            if (type == MINE) return level >= 3;
            if (type == CHOMPER) return level >= 4;
            if (type == REPEATER) return level >= 5;
            if (type == BINU) return true;
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

        private void usePlantFood(Plant a) {
            if (a == null || a.type == BINU || a.foodUsed || food <= 0) return;
            long now = System.currentTimeMillis();
            food--;
            a.foodUsed = true;
            if (a.type == PEASHOOTER || a.type == REPEATER) playPlantFoodSound();
            for (Zombie z : zombies) {
                if (z.hp > 0 && !z.boss) {
                    z.hp = Math.max(0f, z.hp * (2f / 3f));
                }
            }
            if (a.type == GIGANUT) {
                a.maxHp = 8000;
                a.hp = 8000;
                a.plantFoodUntil = Long.MAX_VALUE;
            } else if (a.type == SUNFLOWER) {
                a.plantFoodUntil = now + 5000L;
                a.last = now - 2000L;
            } else if (a.type == PEASHOOTER || a.type == REPEATER) {
                a.plantFoodUntil = now + 5000L;
                a.last = now - 1000L;
            } else if (a.type == CHOMPER) {
                a.animFrame = 4;
                a.shootUntil = now + 650L;
                Zombie z = nearestZombie(a.row, a.col, cellW * 2f);
                if (z != null && !z.boss) z.hp = 0;
            } else if (a.type == MINE) {
                for (Zombie z : zombies) {
                    if (z.row == a.row && !z.boss &&
                            Math.abs(z.x - (left + a.col * cellW + cellW / 2f)) < cellW * 2.2f) {
                        z.hp = Math.max(0f, z.hp - 1800f);
                    }
                }
                a.hp = 0;
            }
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
            if (e.getAction() != MotionEvent.ACTION_UP) return true;

            float x = e.getX();
            float y = e.getY();

            if (screen == HOME) {
                if (inside(x,y,.64f,.25f,.94f,.35f)) startLevel(level);
                else if (inside(x,y,.64f,.385f,.94f,.485f)) screen=LEVEL;
                else if (inside(x,y,.64f,.52f,.94f,.62f)) screen=MINIGAME_MENU;
                else if (inside(x,y,.64f,.655f,.94f,.755f)) screen=ADMIN;
                else if (inside(x,y,.64f,.79f,.94f,.89f)) MainActivity.this.showFinalVideo();
                invalidate(); return true;
            }

            if (screen == LEVEL) {
                if (inside(x, y, .04f, .84f, .20f, .94f)) {
                    screen = HOME;
                    invalidate();
                    return true;
                }

                for (int i = 1; i <= MAX_LEVEL; i++) {
                    int col = (i - 1) % 5;
                    int row = (i - 1) / 5;
                    float bx = .08f + col * .17f;
                    float by = .20f + row * .19f;
                    if (inside(x, y, bx, by, bx + .14f, by + .12f) &&
                            i <= unlocked) {
                        startLevel(i);
                        invalidate();
                        return true;
                    }
                }
                return true;
            }

            if (screen == MINIGAME_MENU) {
                if (inside(x, y, .04f, .84f, .20f, .94f)) {
                    screen = HOME;
                } else if (inside(x, y, .18f, .34f, .48f, .61f)) {
                    startMiniGame(1);
                } else if (inside(x, y, .52f, .34f, .82f, .61f)) {
                    startMiniGame(2);
                }
                invalidate();
                return true;
            }

            if (screen == MINIGAME_PLAY) {
                if (inside(x, y, .88f, .04f, .98f, .15f)) {
                    screen = MINIGAME_MENU;
                    miniTargets.clear();
                    invalidate();
                    return true;
                }

                Iterator<MiniTarget> it = miniTargets.iterator();
                while (it.hasNext()) {
                    MiniTarget target = it.next();
                    if (Math.hypot(x - target.x, y - target.y) <= target.radius * 1.35f) {
                        miniScore += miniMode == 1 ? 10 : 20;
                        it.remove();
                        invalidate();
                        return true;
                    }
                }
                return true;
            }

            if (screen == MINIGAME_RESULT) {
                if (inside(x, y, .30f, .52f, .70f, .62f)) {
                    startMiniGame(miniMode);
                } else if (inside(x, y, .30f, .67f, .70f, .77f)) {
                    screen = MINIGAME_MENU;
                }
                invalidate();
                return true;
            }

            if (screen == ADMIN) {
                if (inside(x, y, .04f, .84f, .20f, .94f)) {
                    screen = HOME;
                } else if (inside(x, y, .30f, .46f, .70f, .57f)) {
                    showAdminCommandDialog();
                } else if (inside(x, y, .30f, .61f, .70f, .72f)) {
                    runAdminCommand("/unlock_all_maps");
                    Toast.makeText(MainActivity.this,
                            "Đã mở khóa 15 bản đồ", Toast.LENGTH_SHORT).show();
                }
                invalidate();
                return true;
            }

            if (screen == PAUSE) {
                if (inside(x, y, .32f, .42f, .68f, .52f)) {
                    screen = PLAY;
                    lastUpdate = System.currentTimeMillis();
                } else if (inside(x, y, .32f, .55f, .68f, .65f)) {
                    startLevel(level);
                } else if (inside(x, y, .32f, .68f, .68f, .78f)) {
                    screen = HOME;
                }
                invalidate();
                return true;
            }

            if (screen == WIN) {
                if (inside(x, y, .32f, .42f, .68f, .52f)) {
                    if (level < MAX_LEVEL) startLevel(level + 1);
                    else screen = HOME;
                } else if (inside(x, y, .32f, .55f, .68f, .65f)) {
                    startLevel(level);
                } else if (inside(x, y, .32f, .68f, .68f, .78f)) {
                    screen = HOME;
                }
                invalidate();
                return true;
            }

            if (screen == LOSE) {
                if (inside(x, y, .32f, .42f, .68f, .52f)) {
                    startLevel(level);
                } else if (inside(x, y, .32f, .68f, .68f, .78f)) {
                    screen = HOME;
                }
                invalidate();
                return true;
            }

            if (inside(x, y, .88f, .075f, .93f, .15f)) {
                screen = PAUSE;
                invalidate();
                return true;
            }

            if (inside(x, y, .94f, .075f, .99f, .15f)) {
                speed = speed == 1 ? 2 : 1;
                invalidate();
                return true;
            }

            Iterator<SunDrop> sunIt = suns.iterator();
            while (sunIt.hasNext()) {
                SunDrop s = sunIt.next();
                if (Math.hypot(x - s.x, y - s.y) <
                        Math.max(45, getHeight() * .055f)) {
                    sun += 100;
                    sunIt.remove();
                    saveGame();
                    invalidate();
                    return true;
                }
            }

            float cardY = getHeight() * .075f;
            float cardH = getHeight() * .09f;
            if (y >= cardY && y <= cardY + cardH) {
                int[] types = {SUNFLOWER, PEASHOOTER, GIGANUT, CHOMPER, REPEATER, MINE, BINU};
                for (int i = 0; i < types.length; i++) {
                    float bx = getWidth() * (.01f + i * .075f);
                    if (x >= bx && x <= bx + getWidth() * .065f) {
                        if (isPlantUnlocked(types[i])) {
                            selected = types[i];
                            tool = TOOL_NONE;
                        }
                        invalidate();
                        return true;
                    }
                }

                if (inside(x, y, .82f, .075f, .87f, .15f)) {
                    tool = tool == TOOL_SHOVEL ? TOOL_NONE : TOOL_SHOVEL;
                    invalidate();
                    return true;
                }

                if (inside(x, y, .535f, .075f, .63f, .15f)) {
                    if (food > 0) tool = tool == TOOL_PLANTFOOD ? TOOL_NONE : TOOL_PLANTFOOD;
                    invalidate();
                    return true;
                }
            }

            if (inside(x, y, .64f, .075f, .81f, .15f)) {
                if (coins >= 100) {
                    coins -= 100;
                    food++;
                    saveGame();
                }
                invalidate();
                return true;
            }

            if (x >= left && x < left + COLS * cellW &&
                    y >= top && y < top + ROWS * cellH) {
                int col = (int) ((x - left) / cellW);
                int row = (int) ((y - top) / cellH);

                if (row < 0 || row >= ROWS || col < 0 || col >= COLS) return true;
                if (!activeRow(row)) return true;

                if (tool == TOOL_SHOVEL) {
                    plants[row][col] = null;
                    tool = TOOL_NONE;
                } else if (tool == TOOL_PLANTFOOD) {
                    if (plants[row][col] != null &&
                            plants[row][col].type != BINU && food > 0) {
                        usePlantFood(plants[row][col]);
                    }
                    tool = TOOL_NONE;
                } else if (plants[row][col] == null &&
                        isPlantUnlocked(selected)) {
                    int cost = plantCost(selected);
                    if (sun >= cost) {
                        sun -= cost;
                        plants[row][col] = new Plant(selected, row, col);
                        if (selected == BINU) playBinuSound1();
                    }
                }

                saveGame();
                invalidate();
                return true;
            }

            return true;
        }

        private void startMiniGame(int mode) {
            miniMode = mode;
            miniScore = 0;
            miniTargets.clear();
            miniStartedAt = System.currentTimeMillis();
            miniLastSpawn = miniStartedAt;
            miniLastFrame = miniStartedAt;
            screen = MINIGAME_PLAY;
        }

        private void updateMiniGame() {
            long now = System.currentTimeMillis();
            if (now - miniStartedAt >= 30000L) {
                screen = MINIGAME_RESULT;
                coins += miniScore / 10;
                saveGame();
                return;
            }

            float dt = Math.min(.05f, Math.max(0f, (now - miniLastFrame) / 1000f));
            miniLastFrame = now;

            long spawnInterval = miniMode == 1 ? 620L : 820L;
            if (now - miniLastSpawn >= spawnInterval) {
                miniLastSpawn = now;
                float radius = Math.max(18f, Math.min(getWidth(), getHeight()) * .035f);
                if (miniMode == 1) {
                    miniTargets.add(new MiniTarget(
                            getWidth() * (.10f + random.nextFloat() * .80f),
                            -radius, radius, 1, getHeight() * .42f));
                } else {
                    miniTargets.add(new MiniTarget(
                            getWidth() + radius,
                            getHeight() * (.24f + random.nextFloat() * .65f),
                            radius, 2, getWidth() * .33f));
                }
            }

            Iterator<MiniTarget> it = miniTargets.iterator();
            while (it.hasNext()) {
                MiniTarget target = it.next();
                if (miniMode == 1) {
                    target.y += target.speed * dt;
                    if (target.y > getHeight() + target.radius) it.remove();
                } else {
                    target.x -= target.speed * dt;
                    if (target.x < -target.radius) it.remove();
                }
            }
        }

        private void drawMiniGameMenu(Canvas c) {
            drawBackdrop(c, 90);
            drawText(c, "MINI GAME", getWidth() / 2f, getHeight() * .16f,
                    34, Color.WHITE, Paint.Align.CENTER);
            drawText(c, "Chơi 30 giây để nhận xu thưởng",
                    getWidth() / 2f, getHeight() * .24f, 17,
                    Color.YELLOW, Paint.Align.CENTER);
            button(c, .18f, .34f, .48f, .61f, "SUN RUSH");
            button(c, .52f, .34f, .82f, .61f, "ZOMBIE TAP");
            drawText(c, "SUN RUSH: chạm mặt trời đang rơi",
                    getWidth() * .33f, getHeight() * .68f, 14,
                    Color.WHITE, Paint.Align.CENTER);
            drawText(c, "ZOMBIE TAP: chạm zombie để ghi điểm",
                    getWidth() * .67f, getHeight() * .68f, 14,
                    Color.WHITE, Paint.Align.CENTER);
            button(c, .04f, .84f, .20f, .94f, "QUAY LẠI");
        }

        private void drawMiniGame(Canvas c) {
            drawBackdrop(c, 65);
            long remaining = Math.max(0L, 30000L - (System.currentTimeMillis() - miniStartedAt));
            drawText(c, miniMode == 1 ? "SUN RUSH" : "ZOMBIE TAP",
                    getWidth() * .06f, getHeight() * .10f, 23,
                    Color.WHITE, Paint.Align.LEFT);
            drawText(c, "ĐIỂM " + miniScore,
                    getWidth() * .50f, getHeight() * .10f, 22,
                    Color.YELLOW, Paint.Align.CENTER);
            drawText(c, "GIÂY " + (remaining / 1000L),
                    getWidth() * .82f, getHeight() * .10f, 20,
                    Color.WHITE, Paint.Align.CENTER);
            button(c, .88f, .04f, .98f, .15f, "THOÁT");

            for (MiniTarget target : miniTargets) {
                Bitmap image = target.kind == 1 ? sunflowerImg : zombieImg;
                if (image != null) {
                    spriteRect.set(target.x-target.radius,target.y-target.radius,target.x+target.radius,target.y+target.radius);
                    c.drawBitmap(image,null,spriteRect,p);
                } else {
                    p.setColor(target.kind == 1 ? Color.YELLOW : Color.GRAY);
                    c.drawCircle(target.x, target.y, target.radius, p);
                }
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
            long shootUntil;
            int animFrame;

            boolean armed;
            boolean foodUsed;

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
                    armTime = last + 30000;
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

            long lastAttack;

            Zombie(int row, int type, boolean boss) {
                this.row = row;
                this.type = type;
                this.boss = boss;

                x = left + COLS * cellW + cellW;
                y = top + row * cellH + cellH * .58f;

                if (boss) {
                    maxHp = 5500f + Math.max(0, (level / 5) - 1) * 3500f;
                    speed = cellW * .053333f;
                    damage = 45 + (level / 5) * 5;
                } else if (type == 1) {
                    maxHp = 900 + level * 100;
                    speed = cellW * .08f;
                    damage = 32;
                } else if (type == 2) {
                    maxHp = 450 + level * 60;
                    speed = cellW * .20f;
                    damage = 18;
                } else {
                    maxHp = 650 + level * 75;
                    speed = cellW * .126667f;
                    damage = 24;
                }

                hp = maxHp;
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
            float x,y,targetY; boolean falling;
            SunDrop(float x,float y){this.x=x;this.y=y;this.targetY=y;this.falling=false;}
            SunDrop(float x,float y,float targetY){this.x=x;this.y=y;this.targetY=targetY;this.falling=true;}
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