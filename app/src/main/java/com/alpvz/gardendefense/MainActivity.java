package com.alpvz.gardendefense;

import android.app.Activity;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.*;
import android.media.*;
import android.view.*;
import android.widget.FrameLayout;
import android.widget.VideoView;
import android.net.Uri;
import java.util.*;

public class MainActivity extends Activity {

    private GameView game;
    private FrameLayout root;
    private SoundPool soundPool;
    private int peaShootSound;
    private MediaPlayer plantFoodPlayer;
    private SharedPreferences save;

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
        initSound();

        game = new GameView();
        setContentView(game);
    }

    private void initSound() {
        try {
            AudioAttributes a = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            soundPool = new SoundPool.Builder()
                    .setAudioAttributes(a)
                    .setMaxStreams(8)
                    .build();

            peaShootSound = soundPool.load(this, R.raw.peashoot, 1);
        } catch (Exception ignored) {
        }
    }

    private void playPeaSound() {
        try {
            if (soundPool != null) {
                soundPool.play(peaShootSound, 1f, 1f, 1, 0, 1f);
            }
        } catch (Exception ignored) {
        }
    }

    private void playPlantFoodSound() {
        try {
            if (plantFoodPlayer != null) {
                plantFoodPlayer.release();
                plantFoodPlayer = null;
            }

            plantFoodPlayer = MediaPlayer.create(
                    MainActivity.this,
                    R.raw.peashootplantfood
            );

            if (plantFoodPlayer != null) {
                plantFoodPlayer.setOnCompletionListener(mp -> {
                    mp.release();
                    plantFoodPlayer = null;
                });
                plantFoodPlayer.start();
            }
        } catch (Exception ignored) {
        }
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

    @Override
    protected void onDestroy() {
        try {
            if (plantFoodPlayer != null) {
                plantFoodPlayer.release();
                plantFoodPlayer = null;
            }
            if (soundPool != null) {
                soundPool.release();
                soundPool = null;
            }
        } catch (Exception ignored) {
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (game == null) {
            super.onBackPressed();
            return;
        }

        if (game.screen == GameView.PLAY) {
            game.screen = GameView.PAUSE;
        } else if (game.screen == GameView.PAUSE) {
            game.screen = GameView.PLAY;
        } else if (game.screen != GameView.HOME) {
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

        static final int HOME = 0;
        static final int LEVEL = 1;
        static final int PLAY = 2;
        static final int PAUSE = 3;
        static final int WIN = 4;
        static final int LOSE = 5;

        static final int TOOL_NONE = 0;
        static final int TOOL_SHOVEL = 1;
        static final int TOOL_PLANTFOOD = 2;

        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Random random = new Random();

        final Plant[][] plants = new Plant[ROWS][COLS];
        final ArrayList<Zombie> zombies = new ArrayList<>();
        final ArrayList<Pea> peas = new ArrayList<>();
        final ArrayList<SunDrop> suns = new ArrayList<>();
        final Mower[] mowers = new Mower[ROWS];

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
        Bitmap zombieNormalScaled;
        Bitmap zombieBossScaled;
        Bitmap bulletImg;

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
        int food = 3;

        int killed = 0;
        int total = 0;
        int spawned = 0;

        int speed = 1;

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

            level = save.getInt("level", 1);
            unlocked = save.getInt("unlocked", 1);
            sun = save.getInt("sun", 500);
            coins = save.getInt("coins", 99999);
            food = save.getInt("food", 3);

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
                return BitmapFactory.decodeResource(getResources(), id);
            } catch (Exception e) {
                return null;
            }
        }

        private void loadImages() {
            sunflowerImg = loadImage("sun");
            peashooterImg = loadImage("peashoot");
            giganutImg = loadImage("giganut");
            chomperImg = loadImage("chomper");
            repeaterImg = loadImage("repeater");
            mineImg = loadImage("min");
            peaFoodImg = loadImage("peashootplantfood");
            repeaterFoodImg = loadImage("repeaterplantfood");
            gigaFoodImg = loadImage("giganutplantfood");
            zombieImg = loadImage("zomplatz");
            bulletImg = loadImage("gigapea");
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldW, int oldH) {
            left = w * 0.18f;
            top = h * 0.25f;
            cellW = (w * 0.78f) / COLS;
            cellH = (h * 0.70f) / ROWS;
            try {
                if (zombieImg != null) {
                    zombieNormalScaled = Bitmap.createScaledBitmap(
                            zombieImg, Math.max(1,(int)(cellW*.68f)),
                            Math.max(1,(int)(cellH*.82f)), true);
                    zombieBossScaled = Bitmap.createScaledBitmap(
                            zombieImg, Math.max(1,(int)(cellW*.95f)),
                            Math.max(1,(int)(cellH*.95f)), true);
                }
            } catch (Throwable ignored) {
                zombieNormalScaled = null;
                zombieBossScaled = null;
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
                postInvalidateDelayed(33);
            }
        }

        private void drawHome(Canvas c) {
            c.drawColor(Color.rgb(25, 65, 30));

            drawText(
                    c,
                    "GARDEN DEFENSE",
                    getWidth() / 2f,
                    getHeight() * 0.22f,
                    42,
                    Color.WHITE,
                    Paint.Align.CENTER
            );

            drawText(
                    c,
                    "☀ " + sun + "     🪙 " + coins + "     PF " + food,
                    getWidth() / 2f,
                    getHeight() * 0.31f,
                    22,
                    Color.YELLOW,
                    Paint.Align.CENTER
            );

            button(c, .30f, .40f, .70f, .51f, "CHƠI");
            button(c, .30f, .56f, .70f, .67f, "CHỌN MÀN");
        }

        private void drawLevels(Canvas c) {
            c.drawColor(Color.rgb(20, 55, 25));

            drawText(
                    c,
                    "CHỌN MÀN",
                    getWidth() / 2f,
                    getHeight() * .10f,
                    32,
                    Color.WHITE,
                    Paint.Align.CENTER
            );

            for (int i = 1; i <= 9; i++) {
                int col = (i - 1) % 3;
                int row = (i - 1) / 3;

                float x = .18f + col * .22f;
                float y = .18f + row * .19f;

                p.setColor(
                        i <= unlocked
                                ? Color.rgb(65, 145, 70)
                                : Color.rgb(70, 70, 70)
                );

                c.drawRoundRect(
                        getWidth() * x,
                        getHeight() * y,
                        getWidth() * (x + .17f),
                        getHeight() * (y + .13f),
                        14,
                        14,
                        p
                );

                drawText(
                        c,
                        i <= unlocked ? "MÀN " + i : "KHÓA",
                        getWidth() * (x + .085f),
                        getHeight() * (y + .082f),
                        20,
                        Color.WHITE,
                        Paint.Align.CENTER
                );
            }

            button(c, .04f, .84f, .20f, .94f, "QUAY LẠI");
        }

        private void drawGame(Canvas c) {
            c.drawColor(Color.rgb(92, 155, 70));

            p.setColor(Color.rgb(38, 78, 40));
            c.drawRect(0, 0, getWidth(), top, p);

            drawText(c, "☀ " + sun, 14, 34, 23, Color.YELLOW, Paint.Align.LEFT);
            drawText(c, "MÀN " + level, getWidth() * .27f, 34, 22, Color.WHITE, Paint.Align.LEFT);
            drawText(c, "ZOM " + killed + "/" + total, getWidth() * .47f, 34, 20, Color.WHITE, Paint.Align.LEFT);
            drawText(c, "🪙 " + coins, getWidth() * .68f, 34, 20, Color.YELLOW, Paint.Align.LEFT);
            drawText(c, "PF " + food, getWidth() * .86f, 34, 20, Color.WHITE, Paint.Align.LEFT);

            drawCards(c);
            drawBoard(c);
            drawPlants(c);
            drawPeas(c);
            drawZombies(c);
            drawSuns(c);
            drawMowers(c);

            button(c, .91f, .075f, .99f, .15f, speed == 2 ? "×2" : "▶");
            button(c, .82f, .075f, .90f, .15f, "Ⅱ");

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
            int[] types = {
                    SUNFLOWER,
                    PEASHOOTER,
                    GIGANUT,
                    CHOMPER,
                    REPEATER,
                    MINE
            };

            for (int i = 0; i < types.length; i++) {
                float x = getWidth() * (.01f + i * .075f);
                float y = getHeight() * .075f;
                float w = getWidth() * .065f;
                float h = getHeight() * .09f;

                p.setColor(
                        selected == types[i] && tool == TOOL_NONE
                                ? Color.YELLOW
                                : Color.rgb(45, 85, 45)
                );

                c.drawRoundRect(x, y, x + w, y + h, 8, 8, p);

                if (isPlantUnlocked(types[i])) {
                    drawPlant(c, types[i], x + w / 2f, y + h / 2f, Math.min(w, h) * .72f);
                } else {
                    drawText(
                            c,
                            "🔒",
                            x + w / 2f,
                            y + h * .62f,
                            18,
                            Color.LTGRAY,
                            Paint.Align.CENTER
                    );
                }
            }

            // Shovel
            p.setColor(tool == TOOL_SHOVEL ? Color.YELLOW : Color.rgb(55, 80, 55));
            c.drawRoundRect(
                    getWidth() * .465f,
                    getHeight() * .075f,
                    getWidth() * .525f,
                    getHeight() * .15f,
                    8,
                    8,
                    p
            );
            drawText(
                    c,
                    "XẺNG",
                    getWidth() * .495f,
                    getHeight() * .122f,
                    12,
                    Color.WHITE,
                    Paint.Align.CENTER
            );

            // Plant food
            p.setColor(tool == TOOL_PLANTFOOD ? Color.YELLOW : Color.rgb(55, 80, 55));
            c.drawRoundRect(
                    getWidth() * .535f,
                    getHeight() * .075f,
                    getWidth() * .63f,
                    getHeight() * .15f,
                    8,
                    8,
                    p
            );
            drawText(
                    c,
                    "PF " + food,
                    getWidth() * .582f,
                    getHeight() * .122f,
                    12,
                    Color.WHITE,
                    Paint.Align.CENTER
            );
        }

        private void drawBoard(Canvas c) {
            for (int r = 0; r < ROWS; r++) {
                for (int col = 0; col < COLS; col++) {
                    p.setColor(
                            (r + col) % 2 == 0
                                    ? Color.rgb(103, 166, 78)
                                    : Color.rgb(91, 153, 67)
                    );

                    c.drawRect(
                            left + col * cellW,
                            top + r * cellH,
                            left + (col + 1) * cellW,
                            top + (r + 1) * cellH,
                            p
                    );
                }
            }

            p.setColor(Color.rgb(135, 95, 55));
            c.drawRect(0, top, left, top + ROWS * cellH, p);
        }

        private void drawPlants(Canvas c) {
            for (int r = 0; r < ROWS; r++) {
                for (int col = 0; col < COLS; col++) {
                    Plant plant = plants[r][col];
                    if (plant == null) continue;

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

                    if (plant.type == MINE &&
                            plant.exploded &&
                            System.currentTimeMillis() < plant.explodeUntil) {
                        p.setColor(Color.rgb(255,165,0));
                        c.drawCircle(x,y,Math.min(cellW,cellH)*.55f,p);
                        p.setColor(Color.YELLOW);
                        c.drawCircle(x,y,Math.min(cellW,cellH)*.30f,p);
                    }
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

        private void drawPlant(Canvas c, int type, float x, float y, float size, Plant plant) {
            Bitmap food = null;
            if (plant != null && plant.foodUsed) {
                if (type == PEASHOOTER) food = peaFoodImg;
                else if (type == REPEATER) food = repeaterFoodImg;
                else if (type == GIGANUT) food = gigaFoodImg;
            }
            if (food != null) {
                c.drawBitmap(food, null, new RectF(
                        x - size / 2f, y - size / 2f,
                        x + size / 2f, y + size / 2f), p);
                return;
            }
            drawPlant(c, type, x, y, size);
        }

        private void drawPlant(Canvas c, int type, float x, float y, float size) {
            Bitmap b = null;

            if (type == SUNFLOWER) b = sunflowerImg;
            else if (type == PEASHOOTER) b = peashooterImg;
            else if (type == GIGANUT) b = giganutImg;
            else if (type == CHOMPER) b = chomperImg;
            else if (type == REPEATER) b = repeaterImg;
            else if (type == MINE) b = mineImg;

            if (b != null) {
                c.drawBitmap(
                        b,
                        null,
                        new RectF(
                                x - size / 2f,
                                y - size / 2f,
                                x + size / 2f,
                                y + size / 2f
                        ),
                        p
                );
                return;
            }

            if (type == SUNFLOWER) p.setColor(Color.YELLOW);
            else if (type == GIGANUT) p.setColor(Color.rgb(145, 95, 55));
            else if (type == CHOMPER) p.setColor(Color.rgb(145, 70, 180));
            else p.setColor(Color.rgb(55, 175, 70));

            c.drawCircle(x, y, size * .35f, p);
        }

        private void drawZombies(Canvas c) {
            for (Zombie z : zombies) {
                float w = z.boss ? cellW * .95f : cellW * .68f;
                float h = z.boss ? cellH * .95f : cellH * .82f;
                Bitmap cached = z.boss ? zombieBossScaled : zombieNormalScaled;

                if (cached != null) {
                    c.drawBitmap(cached, z.x - w / 2f, z.y - h * .55f, p);
                } else if (zombieImg != null) {
                    c.drawBitmap(zombieImg, null,
                            new RectF(z.x - w / 2f, z.y - h * .55f,
                                    z.x + w / 2f, z.y + h * .45f), p);
                } else {
                    p.setColor(Color.rgb(80,80,80));
                    c.drawOval(new RectF(z.x-w/2f,z.y-h/2f,z.x+w/2f,z.y+h/2f),p);
                }
                drawHp(c,z.x-w/2f,z.y-h*.65f,w,z.hp,z.maxHp);
            }
        }

        private void drawPeas(Canvas c) {
            for (Pea q : peas) {
                float size = q.big ? 18 : 10;

                if (bulletImg != null) {
                    c.drawBitmap(
                            bulletImg,
                            null,
                            new RectF(
                                    q.x - size,
                                    q.y - size,
                                    q.x + size,
                                    q.y + size
                            ),
                            p
                    );
                } else {
                    p.setColor(Color.GREEN);
                    c.drawCircle(q.x, q.y, size * .7f, p);
                }
            }
        }

        private void drawSuns(Canvas c) {
            for (SunDrop s : suns) {
                if (sunflowerImg != null) {
                    c.drawBitmap(
                            sunflowerImg,
                            null,
                            new RectF(
                                    s.x - 18,
                                    s.y - 18,
                                    s.x + 18,
                                    s.y + 18
                            ),
                            p
                    );
                } else {
                    p.setColor(Color.YELLOW);
                    c.drawCircle(s.x, s.y, 16, p);
                }
            }
        }

        private void drawMowers(Canvas c) {
            for (Mower m : mowers) {
                float y = top + m.row * cellH + cellH * .72f;

                p.setColor(
                        m.used
                                ? Color.DKGRAY
                                : Color.rgb(190, 70, 40)
                );

                c.drawRoundRect(
                        m.x - cellW * .30f,
                        y - cellH * .18f,
                        m.x + cellW * .30f,
                        y,
                        8,
                        8,
                        p
                );
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
            long now = System.currentTimeMillis();

            float dt = Math.min(
                    .08f,
                    (now - lastUpdate) / 1000f
            ) * speed;

            lastUpdate = now;

            // Random sun drops.
            if (now - lastSun >= 7000) {
                lastSun = now;

                int col = random.nextInt(COLS);

                suns.add(
                        new SunDrop(
                                left + col * cellW + cellW / 2f,
                                top - 30
                        )
                );
            }

            updatePlants(now);
            updatePeas(dt);
            updateZombies(now, dt);
            updateMowers(dt);

            removeDead();

            if (spawned < total &&
                    now - spawnClock >= spawnDelay()) {

                spawnZombie();
                spawned++;
                spawnClock = now;
            }

            if (spawned >= total && zombies.isEmpty() && killed >= total) {
                winLevel();
            }
        }

        private void updatePlants(long now) {
            for (int r = 0; r < ROWS; r++) {
                for (int col = 0; col < COLS; col++) {
                    Plant a = plants[r][col];
                    if (a != null && a.foodUsed &&
                            a.type != GIGANUT &&
                            a.plantFoodUntil > 0 &&
                            now >= a.plantFoodUntil) {
                        a.foodUsed = false;
                        a.plantFoodUntil = 0;
                    }
                }
                for (int col = 0; col < COLS; col++) {
                    Plant a = plants[r][col];
                    if (a == null) continue;

                    boolean plantFoodActive = now < a.plantFoodUntil;

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
                        }
                    }

                    if (a.type == REPEATER) {
                        long cd = plantFoodActive ? 700 : 1500;

                        if (now - a.last >= cd && rowHasZombie(r)) {
                            fire(r, col, 30, false);
                            fireDelayed(r, col, 30, 250);
                            a.last = now;
                        }
                    }

                    if (a.type == CHOMPER) {
                        long cd = plantFoodActive ? 1800 : 3500;

                        if (now - a.last >= cd) {
                            Zombie z = nearestZombie(r, col, cellW * 1.5f);

                            if (z != null) {
                                z.hp = 0;
                                a.last = now;
                            }
                        }
                    }

                    if (a.type == MINE) {
                        if (!a.armed && now >= a.armTime) {
                            a.armed = true;
                        }
                        if (a.armed && !a.exploded) {
                            Zombie hit = nearestZombie(r, col, cellW * 1.05f);
                            if (hit != null) {
                                float centerX = left + col * cellW + cellW / 2f;
                                for (Zombie z : zombies) {
                                    if (z.row == r &&
                                            Math.abs(z.x - centerX) <= cellW * 1.35f) {
                                        z.hp -= 1800;
                                    }
                                }
                                a.exploded = true;
                                a.explodeUntil = now + 450;
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

        private void updatePeas(float dt) {
            Iterator<Pea> it = peas.iterator();

            while (it.hasNext()) {
                Pea q = it.next();

                q.x += q.direction * cellW * 8.5f * dt;

                Zombie hit = null;

                for (Zombie z : zombies) {
                    if (z.row == q.row &&
                            Math.abs(z.x - q.x) < cellW * .30f) {
                        hit = z;
                        break;
                    }
                }

                if (hit != null) {
                    hit.hp -= q.damage;
                    it.remove();
                } else if (q.x > getWidth() + 40) {
                    it.remove();
                }
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
                    } else {
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
                    if (z.row == m.row &&
                            Math.abs(z.x - m.x) < cellW * .55f) {
                        z.hp = 0;
                    }
                }

                if (m.x > getWidth() + cellW) {
                    m.active = false;
                }
            }
        }

        private void spawnZombie() {
            int row = random.nextInt(ROWS);

            boolean boss =
                    level == 9 &&
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
            Iterator<Zombie> it = zombies.iterator();

            while (it.hasNext()) {
                Zombie z = it.next();

                if (z.hp <= 0) {
                    it.remove();
                    killed++;
                    coins += z.boss ? 100 : 5;
                }
            }

            for (int r = 0; r < ROWS; r++) {
                for (int col = 0; col < COLS; col++) {
                    if (plants[r][col] != null &&
                            plants[r][col].hp <= 0) {
                        plants[r][col] = null;
                    }
                }
            }
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
            level = lv;
            screen = PLAY;

            killed = 0;
            spawned = 0;

            total = lv <= 2 ? 8 :
                    lv <= 4 ? 10 :
                    lv <= 8 ? 12 :
                    15;

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

            tool = TOOL_NONE;
        }

        private long spawnDelay() {
            if (level <= 2) return 4200;
            if (level <= 4) return 3600;
            if (level <= 8) return 3100;
            return 2600;
        }

        private boolean isPlantUnlocked(int type) {
            if (type == PEASHOOTER) return true;
            if (type == SUNFLOWER) return level >= 1;
            if (type == GIGANUT) return level >= 2;
            if (type == MINE) return level >= 3;
            if (type == CHOMPER) return level >= 4;
            if (type == REPEATER) return level >= 5;
            return false;
        }

        private int plantCost(int type) {
            if (type == SUNFLOWER) return 50;
            if (type == PEASHOOTER) return 100;
            if (type == GIGANUT) return 125;
            if (type == CHOMPER) return 150;
            if (type == REPEATER) return 200;
            if (type == MINE) return 50;
            return 99999;
        }

        private void winLevel() {
            screen = WIN;
            if (level < 9) {
                unlocked = Math.max(unlocked, level + 1);
            } else {
                showFinalVideo();
            }
            saveGame();
        }

        private void usePlantFood(Plant a) {
            if (a == null) return;

            long now = System.currentTimeMillis();

            playPlantFoodSound();

            if (a.type == GIGANUT) {
                a.maxHp = 8000;
                a.hp = 8000;
                a.foodUsed = true;
            } else if (a.type == SUNFLOWER ||
                    a.type == PEASHOOTER ||
                    a.type == REPEATER) {
                a.plantFoodUntil = now + 10000;
                a.foodUsed = true;
                a.last = now - 2000;
            } else if (a.type == CHOMPER) {
                Zombie z = nearestZombie(a.row, a.col, cellW * 2f);
                if (z != null) z.hp = 0;
            } else if (a.type == MINE) {
                for (Zombie z : zombies) {
                    if (z.row == a.row &&
                            Math.abs(
                                    z.x -
                                    (left + a.col * cellW + cellW / 2f)
                            ) < cellW * 2.2f) {
                        z.hp -= 1800;
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

        private void button(
                Canvas c,
                float x1,
                float y1,
                float x2,
                float y2,
                String label
        ) {
            p.setColor(Color.rgb(65, 135, 70));

            c.drawRoundRect(
                    getWidth() * x1,
                    getHeight() * y1,
                    getWidth() * x2,
                    getHeight() * y2,
                    14,
                    14,
                    p
            );

            drawText(
                    c,
                    label,
                    getWidth() * ((x1 + x2) / 2f),
                    getHeight() * ((y1 + y2) / 2f) + 8,
                    19,
                    Color.WHITE,
                    Paint.Align.CENTER
            );
        }

        private void drawText(
                Canvas c,
                String s,
                float x,
                float y,
                float size,
                int color,
                Paint.Align align
        ) {
            text.setTextAlign(align);
            text.setTextSize(size);
            text.setColor(color);
            c.drawText(s, x, y, text);
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            if (e.getAction() != MotionEvent.ACTION_UP) {
                return true;
            }

            float x = e.getX();
            float y = e.getY();

            // HOME
            if (screen == HOME) {
                if (inside(x, y, .30f, .40f, .70f, .51f)) {
                    startLevel(level);
                } else if (inside(x, y, .30f, .56f, .70f, .67f)) {
                    screen = LEVEL;
                }

                invalidate();
                return true;
            }

            // LEVEL SELECT
            if (screen == LEVEL) {
                if (inside(x, y, .04f, .84f, .20f, .94f)) {
                    screen = HOME;
                    invalidate();
                    return true;
                }

                for (int i = 1; i <= 9; i++) {
                    int col = (i - 1) % 3;
                    int row = (i - 1) / 3;

                    float bx = .18f + col * .22f;
                    float by = .18f + row * .19f;

                    if (inside(
                            x, y,
                            bx, by,
                            bx + .17f, by + .13f
                    ) && i <= unlocked) {
                        startLevel(i);
                        invalidate();
                        return true;
                    }
                }

                return true;
            }

            // PAUSE
            if (screen == PAUSE) {
                if (inside(x, y, .32f, .42f, .68f, .52f)) {
                    screen = PLAY;
                } else if (inside(x, y, .32f, .55f, .68f, .65f)) {
                    startLevel(level);
                } else if (inside(x, y, .32f, .68f, .68f, .78f)) {
                    screen = HOME;
                }

                invalidate();
                return true;
            }

            // WIN
            if (screen == WIN) {
                if (inside(x, y, .32f, .42f, .68f, .52f)) {
                    if (level < 9) {
                        startLevel(level + 1);
                    } else {
                        screen = HOME;
                    }
                } else if (inside(x, y, .32f, .55f, .68f, .65f)) {
                    startLevel(level);
                } else if (inside(x, y, .32f, .68f, .68f, .78f)) {
                    screen = HOME;
                }

                invalidate();
                return true;
            }

            // LOSE
            if (screen == LOSE) {
                if (inside(x, y, .32f, .42f, .68f, .52f)) {
                    startLevel(level);
                } else if (inside(x, y, .32f, .68f, .68f, .78f)) {
                    screen = HOME;
                }

                invalidate();
                return true;
            }

            // Pause button
            if (inside(x, y, .82f, .075f, .90f, .15f)) {
                screen = PAUSE;
                invalidate();
                return true;
            }

            // Speed
            if (inside(x, y, .91f, .075f, .99f, .15f)) {
                speed = speed == 1 ? 2 : 1;
                invalidate();
                return true;
            }

            // Collect sun
            Iterator<SunDrop> sunIt = suns.iterator();

            while (sunIt.hasNext()) {
                SunDrop s = sunIt.next();

                if (Math.hypot(x - s.x, y - s.y) <
                        Math.max(45, getHeight() * .055f)) {

                    sun += 25;
                    sunIt.remove();
                    saveGame();
                    invalidate();
                    return true;
                }
            }

            // Plant cards
            float cardY = getHeight() * .075f;
            float cardH = getHeight() * .09f;

            if (y >= cardY && y <= cardY + cardH) {
                int[] types = {
                        SUNFLOWER,
                        PEASHOOTER,
                        GIGANUT,
                        CHOMPER,
                        REPEATER,
                        MINE
                };

                for (int i = 0; i < types.length; i++) {
                    float bx = getWidth() * (.01f + i * .075f);

                    if (x >= bx &&
                            x <= bx + getWidth() * .065f) {

                        if (isPlantUnlocked(types[i])) {
                            selected = types[i];
                            tool = TOOL_NONE;
                        }

                        invalidate();
                        return true;
                    }
                }

                // Shovel
                if (inside(x, y, .465f, .075f, .525f, .15f)) {
                    tool = tool == TOOL_SHOVEL
                            ? TOOL_NONE
                            : TOOL_SHOVEL;
                    invalidate();
                    return true;
                }

                // Plant food
                if (inside(x, y, .535f, .075f, .63f, .15f)) {
                    if (food > 0) {
                        tool = tool == TOOL_PLANTFOOD
                                ? TOOL_NONE
                                : TOOL_PLANTFOOD;
                    }

                    invalidate();
                    return true;
                }
            }

            // Buy plant food with coins
            if (inside(x, y, .76f, .075f, .81f, .15f)) {
                if (coins >= 100) {
                    coins -= 100;
                    food++;
                    saveGame();
                }

                invalidate();
                return true;
            }

            // Board
            if (x >= left &&
                    x < left + COLS * cellW &&
                    y >= top &&
                    y < top + ROWS * cellH) {

                int col = (int) ((x - left) / cellW);
                int row = (int) ((y - top) / cellH);

                if (row < 0 || row >= ROWS ||
                        col < 0 || col >= COLS) {
                    return true;
                }

                if (tool == TOOL_SHOVEL) {
                    plants[row][col] = null;
                    tool = TOOL_NONE;
                } else if (tool == TOOL_PLANTFOOD) {
                    if (plants[row][col] != null && food > 0) {
                        usePlantFood(plants[row][col]);
                        food--;
                    }
                    tool = TOOL_NONE;
                } else {
                    if (plants[row][col] == null &&
                            isPlantUnlocked(selected)) {

                        int cost = plantCost(selected);

                        if (sun >= cost) {
                            sun -= cost;
                            plants[row][col] =
                                    new Plant(selected, row, col);
                        }
                    }
                }

                saveGame();
                invalidate();
                return true;
            }

            return true;
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

            boolean armed;
            boolean foodUsed;
            boolean exploded;
            long explodeUntil;

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
                    maxHp = 5000;
                    speed = cellW * .08f;
                    damage = 45;
                } else if (type == 1) {
                    maxHp = 900 + level * 100;
                    speed = cellW * .12f;
                    damage = 32;
                } else if (type == 2) {
                    maxHp = 450 + level * 60;
                    speed = cellW * .30f;
                    damage = 18;
                } else {
                    maxHp = 650 + level * 75;
                    speed = cellW * .19f;
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
            float x;
            float y;

            SunDrop(float x, float y) {
                this.x = x;
                this.y = y;
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
                        
