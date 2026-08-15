package com.alpvz.gardendefense;

import android.app.Activity;
import android.os.Bundle;
import android.content.pm.ActivityInfo;
import android.graphics.*;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.view.*;
import java.util.*;

public class MainActivity extends Activity {
    private ToneGenerator sound;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        sound = new ToneGenerator(AudioManager.STREAM_MUSIC, 55);
        setContentView(new GameView());
    }

    void beep(int tone) {
        try {
            if (sound != null) sound.startTone(tone, 60);
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onDestroy() {
        try {
            if (sound != null) sound.release();
        } catch (Exception ignored) {
        }
        super.onDestroy();
    }

    class GameView extends View {
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Random random = new Random();

        final int ROWS = 5;
        final int COLS = 9;
        final int MAX_LEVEL = 9;

        final int SUNFLOWER = 1;
        final int PEASHOOTER = 2;
        final int GIGANUT = 3;
        final int CHOMPER = 4;
        final int REPEATER = 5;
        final int MINE = 6;

        final int MODE_NONE = 0;
        final int MODE_PLANT_FOOD = 10;
        final int MODE_BOMB = 11;
        final int MODE_FIRE = 12;
        final int MODE_SHOVEL = 13;

        final int SCREEN_WORLD = 0;
        final int SCREEN_PLAY = 1;
        final int SCREEN_PAUSE = 2;
        final int SCREEN_LOADING = 3;
        final int SCREEN_WIN = 4;
        final int SCREEN_HOME_LOSE = 5;
        final int SCREEN_ZEN = 6;
        final int SCREEN_MINIGAME = 7;

        Bitmap sunImg, peaImg, gigaImg, chompImg;
        Bitmap repeaterImg, mineImg, zombieImg, vinhImg, bulletImg;

        final Plant[][] plants = new Plant[ROWS][COLS];
        final ArrayList<Zombie> zombies = new ArrayList<>();
        final ArrayList<Pea> peas = new ArrayList<>();
        final ArrayList<Bomb> bombs = new ArrayList<>();
        final ArrayList<Drop> drops = new ArrayList<>();
        final Mower[] mowers = new Mower[ROWS];

        float left, top, cellW, cellH;
        int sun = 500;
        int coins = 9999;
        int plantFood = 3;

        int currentLevel = 0;      // 0 = tutorial
        int maxUnlocked = 0;
        int selected = PEASHOOTER;
        int mode = MODE_NONE;
        int screen = SCREEN_WORLD;

        int wave = 0;
        int totalWaves = 3;
        int waveSpawned = 0;
        int spawned = 0;
        int killed = 0;

        boolean levelWon = false;
        boolean paused = false;
        boolean homeLose = false;
        Zombie homeZombie;

        long last;
        long spawnClock;
        long waveClock;
        long loadStart;
        long failFadeStart;

        boolean repeaterUnlocked = false;
        boolean sunflowerUnlocked = false;
        boolean gigaUnlocked = false;
        boolean mineUnlocked = false;
        boolean chomperUnlocked = false;

        int zenSeeds = 0;
        int miniGameType = 0;
        boolean miniGameActive = false;
        float miniTimer = 0;
        int miniKills = 0;
        int miniNeed = 8;
        int[] miniPlants = new int[3];
        int vinhSpawnedThisLevel = 0;

        GameView() {
            super(MainActivity.this);
            setFocusable(true);

            sunImg = load("sun");
            peaImg = load("peashoot");
            gigaImg = load("giganut");
            chompImg = load("chomper");
            repeaterImg = load("repeater");   // optional until file exists
            mineImg = load("min");            // optional until file exists
            zombieImg = load("zomplatz");
            vinhImg = load("zomvinhhung");
            bulletImg = load("gigapea");

            resetMowers();
            last = System.currentTimeMillis();
            spawnClock = last;
            waveClock = last;
        }

        Bitmap load(String n) {
            int id = getResources().getIdentifier(n, "drawable", getPackageName());
            return id == 0 ? null : BitmapFactory.decodeResource(getResources(), id);
        }

        @Override
        protected void onDraw(Canvas c) {
            layout();

            if (screen == SCREEN_WORLD) {
                drawWorld(c);
                return;
            }
            if (screen == SCREEN_ZEN) {
                drawZenGarden(c);
                return;
            }
            if (screen == SCREEN_MINIGAME) {
                drawMiniGame(c);
                updateMiniGame();
                postInvalidateDelayed(30);
                return;
            }
            if (screen == SCREEN_LOADING) {
                drawLoading(c);
                if (System.currentTimeMillis() - loadStart > 900) {
                    if (postExitWorld) {
                        postExitWorld = false;
                        screen = SCREEN_WORLD;
                    } else {
                        screen = SCREEN_PLAY;
                        last = System.currentTimeMillis();
                        spawnClock = last;
                        waveClock = last;
                    }
                }
                postInvalidateDelayed(30);
                return;
            }
            if (screen == SCREEN_HOME_LOSE) {
                drawHomeLose(c);
                postInvalidateDelayed(30);
                return;
            }

            drawGame(c);

            if (screen == SCREEN_PLAY) {
                update();
                postInvalidateDelayed(30);
            } else if (screen == SCREEN_PAUSE) {
                drawPause(c);
                postInvalidateDelayed(30);
            } else if (screen == SCREEN_WIN) {
                drawWin(c);
                postInvalidateDelayed(30);
            }
        }

        void layout() {
            left = 58f;
            top = getHeight() * 0.25f;
            cellW = (getWidth() - left - 12f) / COLS;
            cellH = (getHeight() - top - 8f) / ROWS;
        }

        int activeRows() {
            if (currentLevel == 0 || currentLevel == 1) return 1;
            if (currentLevel == 2 || currentLevel == 3) return 3;
            return 5;
        }

        int baseZombieHp() {
            return currentLevel <= 3 ? 550 : 1100;
        }

        int zombieHpFor(boolean giga, boolean vinh, boolean zomGiga) {
            if (vinh) return 3000;
            if (zomGiga) return currentLevel <= 3 ? 1100 : 2200;
            if (giga) return currentLevel <= 3 ? 850 : 1700;
            return baseZombieHp();
        }

        int totalWavesForLevel() {
            if (currentLevel == 0) return 2;
            return 3 + Math.min(2, currentLevel / 4);
        }

        int totalZombiesForWave() {
            if (currentLevel == 0) return 4;
            return 4 + currentLevel + wave * 2;
        }

        int totalLevelZombies() {
            int total = 0;
            for (int w = 0; w < totalWaves; w++) total += totalZombiesForWaveAt(w);
            return total;
        }

        int totalZombiesForWaveAt(int w) {
            if (currentLevel == 0) return 4;
            return 4 + currentLevel + w * 2;
        }

        int currentThemeColor() {
            int[] colors = {
                    Color.rgb(75, 145, 70),
                    Color.rgb(78, 156, 76),
                    Color.rgb(190, 160, 72),
                    Color.rgb(132, 88, 55),
                    Color.rgb(62, 132, 150),
                    Color.rgb(84, 74, 155),
                    Color.rgb(165, 75, 125),
                    Color.rgb(55, 120, 105),
                    Color.rgb(125, 90, 52),
                    Color.rgb(46, 92, 145)
            };
            return colors[Math.min(currentLevel, colors.length - 1)];
        }

        void drawWorld(Canvas c) {
            c.drawColor(Color.rgb(20, 35, 30));
            text(c, "GARDEN DEFENSE", getWidth() / 2f, 60, Color.WHITE, 34, Paint.Align.CENTER);
            text(c, "WORLD", getWidth() / 2f, 92, Color.LTGRAY, 18, Paint.Align.CENTER);

            int cols = 5;
            for (int i = 0; i <= MAX_LEVEL; i++) {
                int row = i / cols;
                int col = i % cols;
                float x = 70 + col * ((getWidth() - 140) / 5f);
                float y = 140 + row * 90;
                boolean unlocked = i <= maxUnlocked;
                p.setColor(unlocked ? Color.rgb(60, 150, 70) : Color.rgb(70, 70, 70));
                c.drawRoundRect(new RectF(x, y, x + 95, y + 60), 10, 10, p);
                text(c, i == 0 ? "HƯỚNG DẪN" : "MÀN " + i,
                        x + 47.5f, y + 36, Color.WHITE, i == 0 ? 12 : 18, Paint.Align.CENTER);
            }

            button(c, getWidth() - 180, getHeight() - 70, getWidth() - 95, getHeight() - 25, "ZEN", Color.rgb(45, 130, 85));
            text(c, "MẦM: " + zenSeeds, getWidth() - 175, getHeight() - 82, Color.WHITE, 14, Paint.Align.LEFT);
            text(c, "Đã mở khóa: " + maxUnlocked + "/" + MAX_LEVEL,
                    20, getHeight() - 35, Color.LTGRAY, 14, Paint.Align.LEFT);
        }

        void drawZenGarden(Canvas c) {
            c.drawColor(Color.rgb(74, 145, 76));
            text(c, "ZEN GARDEN", 25, 45, Color.WHITE, 28, Paint.Align.LEFT);
            text(c, "Mầm cây: " + zenSeeds, 25, 75, Color.WHITE, 16, Paint.Align.LEFT);
            button(c, getWidth() - 120, 20, getWidth() - 20, 62, "QUAY LẠI", Color.DKGRAY);

            int[] types = {PEASHOOTER, SUNFLOWER, GIGANUT, CHOMPER, REPEATER, MINE};
            String[] names = {"PEA", "SUN", "GIGA", "CHOMP", "REPEAT", "MIN"};
            Bitmap[] imgs = {peaImg, sunImg, gigaImg, chompImg, repeaterImg, mineImg};

            for (int i = 0; i < types.length; i++) {
                float x = 30 + (i % 3) * 180;
                float y = 110 + (i / 3) * 180;
                p.setColor(Color.argb(120, 20, 80, 30));
                c.drawRoundRect(new RectF(x, y, x + 150, y + 145), 12, 12, p);
                if (imgs[i] != null) {
                    c.drawBitmap(imgs[i], null, new RectF(x + 35, y + 10, x + 115, y + 90), p);
                }
                text(c, names[i], x + 75, y + 112, Color.WHITE, 16, Paint.Align.CENTER);
                text(c, "TRỒNG", x + 75, y + 135, Color.YELLOW, 13, Paint.Align.CENTER);
            }
        }

        void drawLoading(Canvas c) {
            c.drawColor(Color.BLACK);
            text(c, "ĐANG TẢI...", getWidth() / 2f, getHeight() / 2f,
                    Color.WHITE, 28, Paint.Align.CENTER);
        }

        void drawHomeLose(Canvas c) {
            long elapsed = System.currentTimeMillis() - failFadeStart;
            float alpha = Math.min(255f, elapsed / 900f * 255f);
            p.setColor(Color.argb((int) alpha, 0, 0, 0));
            c.drawRect(0, 0, getWidth(), getHeight(), p);

            if (homeZombie != null) {
                float w = homeZombie.zomGiga ? 78 : (homeZombie.giga ? 68 : 58);
                float h = homeZombie.zomGiga ? 115 : (homeZombie.giga ? 105 : 90);
                Bitmap img = homeZombie.vinh ? vinhImg : zombieImg;
                if (img != null) {
                    c.drawBitmap(img, null, new RectF(
                            getWidth() / 2f - w / 2f,
                            getHeight() / 2f - h / 2f,
                            getWidth() / 2f + w / 2f,
                            getHeight() / 2f + h / 2f), p);
                } else {
                    p.setColor(Color.GRAY);
                    c.drawRect(getWidth() / 2f - w / 2f, getHeight() / 2f - h / 2f,
                            getWidth() / 2f + w / 2f, getHeight() / 2f + h / 2f, p);
                }
                // Đứng im đúng lúc đã vào nhà.
                text(c, "...", getWidth() / 2f + 22, getHeight() / 2f - 55,
                        Color.WHITE, 20, Paint.Align.LEFT);
            }

            button(c, getWidth() / 2f - 90, getHeight() - 95,
                    getWidth() / 2f + 90, getHeight() - 40,
                    "CHƠI LẠI", Color.rgb(50, 140, 65));
        }

        void drawGame(Canvas c) {
            c.drawColor(currentThemeColor());
            drawTop(c);
            drawBoard(c);
            drawMowers(c);
            drawPlants(c);
            drawDrops(c);
            drawPeas(c);
            drawBombs(c);
            drawZombies(c);
        }

        void drawTop(Canvas c) {
            p.setColor(Color.rgb(28, 55, 32));
            c.drawRect(0, 0, getWidth(), top - 7, p);

            text(c, "MÀN " + currentLevel + "   WAVE " + Math.min(wave + 1, totalWaves) + "/" + totalWaves,
                    8, 22, Color.WHITE, 14, Paint.Align.LEFT);
            text(c, "☀ " + sun + "   🪙 " + coins + "   PF " + plantFood,
                    getWidth() / 2f, 22, Color.WHITE, 14, Paint.Align.CENTER);

            button(c, getWidth() - 48, 5, getWidth() - 8, 32, "Ⅱ", Color.DKGRAY);

            float w = getWidth() / 7f;
            card(c, 0, "SUN", SUNFLOWER, sunImg, w, sunflowerUnlocked);
            card(c, w, "PEA", PEASHOOTER, peaImg, w, true);
            card(c, w * 2, "GIGA", GIGANUT, gigaImg, w, gigaUnlocked);
            card(c, w * 3, "CHOMP", CHOMPER, chompImg, w, chomperUnlocked);
            card(c, w * 4, "REPEAT", REPEATER, repeaterImg, w, repeaterUnlocked);
            card(c, w * 5, "MINE", MINE, mineImg, w, mineUnlocked);
            card(c, w * 6, "UTIL", MODE_NONE, null, w, true);

            // Thông tin màn nhỏ ngay gần khay plant.
            text(c, "M" + currentLevel + " • W" + (wave + 1), getWidth() - 75, top - 13,
                    Color.WHITE, 11, Paint.Align.RIGHT);
        }

        void card(Canvas c, float x, String name, int type, Bitmap img, float w, boolean unlocked) {
            boolean active = selected == type && mode == MODE_NONE;
            p.setColor(!unlocked ? Color.rgb(95, 95, 95) : (active ? Color.YELLOW : Color.WHITE));
            c.drawRoundRect(new RectF(x + 2, 32, x + w - 2, top - 10), 8, 8, p);
            if (img != null && unlocked) {
                c.drawBitmap(img, null, new RectF(x + 5, 38, x + 52, top - 17), p);
            }
            text(c, unlocked ? name : "LOCK", x + w / 2f + 14, 64,
                    unlocked ? Color.DKGRAY : Color.LTGRAY, 10, Paint.Align.CENTER);
        }

        void drawBoard(Canvas c) {
            int rows = activeRows();
            for (int r = 0; r < ROWS; r++) {
                for (int col = 0; col < COLS; col++) {
                    float x = left + col * cellW;
                    float y = top + r * cellH;
                    if (r >= rows) {
                        p.setColor(Color.rgb(38, 72, 48));
                    } else {
                        p.setColor((r + col) % 2 == 0 ?
                                Color.rgb(111, 188, 68) : Color.rgb(101, 178, 61));
                    }
                    c.drawRect(x, y, x + cellW - 2, y + cellH - 2, p);
                }
            }
        }

        void drawPlants(Canvas c) {
            for (int r = 0; r < ROWS; r++) {
                for (int col = 0; col < COLS; col++) {
                    Plant a = plants[r][col];
                    if (a == null) continue;

                    Bitmap img = null;
                    if (a.type == SUNFLOWER) img = sunImg;
                    else if (a.type == PEASHOOTER) img = peaImg;
                    else if (a.type == GIGANUT) img = gigaImg;
                    else if (a.type == CHOMPER) img = chompImg;
                    else if (a.type == REPEATER) img = repeaterImg;
                    else if (a.type == MINE) img = mineImg;

                    if (img != null) {
                        c.drawBitmap(img, null,
                                new RectF(a.x + 3, a.y + 3, a.x + a.w - 3, a.y + a.h - 3), p);
                    } else if (a.type == MINE) {
                        p.setColor(a.mineReady ? Color.rgb(70, 70, 70) : Color.rgb(95, 70, 40));
                        c.drawOval(new RectF(a.x + 12, a.y + a.h * .45f,
                                a.x + a.w - 12, a.y + a.h * .80f), p);
                    } else if (a.type == REPEATER) {
                        p.setColor(Color.rgb(65, 180, 65));
                        c.drawCircle(a.x + a.w * .5f, a.y + a.h * .4f, 22, p);
                    }

                    if (a.type != MINE) {
                        bar(c, a.x + 7, a.y + 4, a.w - 14, a.hp, a.maxHp);
                    }
                    if (a.pfTime > 0 || a.pfPermanent) {
                        text(c, "PF", a.x + a.w * .42f, a.y + 20,
                                Color.MAGENTA, 11, Paint.Align.LEFT);
                    }
                    if (a.type == MINE && !a.mineReady) {
                        text(c, String.valueOf((int) Math.ceil(a.mineTimer)), a.x + a.w / 2f,
                                a.y + a.h * .55f, Color.WHITE, 13, Paint.Align.CENTER);
                    }
                }
            }
        }

        void drawDrops(Canvas c) {
            for (Drop d : drops) {
                if (d.type == Drop.SUN) {
                    p.setColor(Color.YELLOW);
                    c.drawCircle(d.x, d.y, 14, p);
                    p.setColor(Color.rgb(255, 180, 0));
                    c.drawCircle(d.x, d.y, 7, p);
                } else if (d.type == Drop.COIN) {
                    p.setColor(Color.rgb(255, 205, 40));
                    c.drawCircle(d.x, d.y, 13, p);
                    text(c, "$", d.x, d.y + 5, Color.rgb(90, 60, 0), 14, Paint.Align.CENTER);
                } else {
                    p.setColor(Color.rgb(80, 185, 90));
                    c.drawRoundRect(new RectF(d.x - 17, d.y - 10, d.x + 17, d.y + 10), 7, 7, p);
                    text(c, "SEED", d.x, d.y + 4, Color.WHITE, 9, Paint.Align.CENTER);
                }
            }
        }

        void drawPeas(Canvas c) {
            for (Pea b : peas) {
                if (bulletImg != null) {
                    c.drawBitmap(bulletImg, null,
                            new RectF(b.x - 10, b.y - 10, b.x + 10, b.y + 10), p);
                } else {
                    p.setColor(Color.GREEN);
                    c.drawCircle(b.x, b.y, 7, p);
                }
            }
        }

        void drawBombs(Canvas c) {
            for (Bomb b : bombs) {
                p.setColor(b.enemy ? Color.RED : Color.rgb(55, 55, 55));
                c.drawCircle(b.x, b.y, 9, p);
            }
        }

        void drawZombies(Canvas c) {
            for (Zombie z : zombies) {
                if (z.dead) continue;
                float w = z.vinh ? 62 : (z.zomGiga ? 98 : (z.giga ? 86 : 70));
                float h = z.vinh ? 82 : (z.zomGiga ? 140 : (z.giga ? 122 : 100));
                Bitmap img = z.vinh ? vinhImg : zombieImg;
                if (img != null) {
                    c.drawBitmap(img, null,
                            new RectF(z.x - w / 2f, z.y - h / 2f,
                                        z.x + w / 2f, z.y + h / 2f), p);
                } else {
                    p.setColor(z.vinh ? Color.DKGRAY : Color.GRAY);
                    c.drawRect(z.x - w / 2f, z.y - h / 2f,
                            z.x + w / 2f, z.y + h / 2f, p);
                }
                bar(c, z.x - 28, z.y - h / 2f - 7, 56, z.hp, z.maxHp);
            }
        }

        void drawMowers(Canvas c) {
            for (Mower m : mowers) {
                if (!m.active) {
                    if (!m.used) {
                        float y = top + m.row * cellH + cellH * .65f;
                        p.setColor(Color.RED);
                        c.drawRect(m.x - 25, y - 13, m.x + 25, y + 13, p);
                        p.setColor(Color.BLACK);
                        c.drawCircle(m.x - 14, y + 14, 7, p);
                        c.drawCircle(m.x + 14, y + 14, 7, p);
                    }
                    continue;
                }
                float y = top + m.row * cellH + cellH * .65f;
                p.setColor(Color.RED);
                c.drawRect(m.x - 25, y - 13, m.x + 25, y + 13, p);
                p.setColor(Color.BLACK);
                c.drawCircle(m.x - 14, y + 14, 7, p);
                c.drawCircle(m.x + 14, y + 14, 7, p);
            }
        }

        void drawPause(Canvas c) {
            p.setColor(0xaa000000);
            c.drawRect(0, 0, getWidth(), getHeight(), p);
            text(c, "TẠM DỪNG", getWidth() / 2f, getHeight() / 2f - 45,
                    Color.WHITE, 32, Paint.Align.CENTER);
            button(c, getWidth() / 2f - 135, getHeight() / 2f,
                    getWidth() / 2f - 15, getHeight() / 2f + 55,
                    "CHƠI LẠI", Color.rgb(50, 140, 65));
            button(c, getWidth() / 2f + 15, getHeight() / 2f,
                    getWidth() / 2f + 135, getHeight() / 2f + 55,
                    "THOÁT", Color.rgb(120, 80, 70));
        }

        void drawWin(Canvas c) {
            p.setColor(0xcc000000);
            c.drawRect(0, 0, getWidth(), getHeight(), p);
            text(c, currentLevel == 0 ? "HƯỚNG DẪN HOÀN TẤT!" : "CHIẾN THẮNG!",
                    getWidth() / 2f, getHeight() / 2f - 55,
                    Color.WHITE, 28, Paint.Align.CENTER);
            String reward = "";
            if (currentLevel == 1) reward = "Mở khóa Sunflower";
            if (currentLevel == 2) reward = "Mở khóa Giganut + Zomto";
            if (currentLevel == 3) reward = "Mở khóa Mìn Khoai Tây + ZomGiga";
            if (currentLevel == 4) reward = "Mở khóa Chomper + 2 hàng";
            if (currentLevel == 5) reward = "Mở khóa Repeater";
            if (!reward.isEmpty()) text(c, reward, getWidth() / 2f, getHeight() / 2f - 10,
                    Color.YELLOW, 17, Paint.Align.CENTER);

            button(c, getWidth() / 2f - 160, getHeight() / 2f + 20,
                    getWidth() / 2f - 20, getHeight() / 2f + 72,
                    "NEXT LEVEL", Color.rgb(45, 140, 65));
            button(c, getWidth() / 2f + 20, getHeight() / 2f + 20,
                    getWidth() / 2f + 160, getHeight() / 2f + 72,
                    "EXIT", Color.rgb(105, 80, 70));
        }

        void drawMiniGame(Canvas c) {
            c.drawColor(Color.rgb(28, 70, 35));
            String title = miniGameType == 1 ? "MINIGAME: ĐẬP ZOMBIE" :
                    miniGameType == 2 ? "MINIGAME: RÒNG RỌC" :
                            "MINIGAME: CHẠY & ĐẶT PLANT";
            text(c, title, getWidth() / 2f, 42, Color.WHITE, 24, Paint.Align.CENTER);
            text(c, "Mục tiêu: " + miniKills + "/" + miniNeed,
                    getWidth() / 2f, 72, Color.YELLOW, 16, Paint.Align.CENTER);

            if (miniGameType == 1) {
                for (int i = 0; i < 6; i++) {
                    float x = 80 + (i * 125) % Math.max(100, getWidth() - 160);
                    float y = 130 + ((i * 70) % Math.max(80, getHeight() - 230));
                    p.setColor(Color.rgb(100, 140, 100));
                    c.drawCircle(x, y, 28, p);
                    text(c, "Z", x, y + 8, Color.WHITE, 24, Paint.Align.CENTER);
                }
                text(c, "Chạm zombie để đập", getWidth() / 2f, getHeight() - 45,
                        Color.LTGRAY, 16, Paint.Align.CENTER);
            } else if (miniGameType == 2) {
                p.setColor(Color.rgb(120, 80, 40));
                c.drawRect(80, 120, getWidth() - 80, 138, p);
                float railX = 120 + (float) ((System.currentTimeMillis() / 8) % Math.max(1, getWidth() - 240));
                p.setColor(Color.YELLOW);
                c.drawCircle(railX, 129, 18, p);
                text(c, "Chạm thanh ròng rọc để nhận xu", getWidth() / 2f, getHeight() - 45,
                        Color.LTGRAY, 16, Paint.Align.CENTER);
            } else {
                for (int i = 0; i < 3; i++) {
                    int t = miniPlants[i];
                    Bitmap img = plantImage(t);
                    float x = 120 + i * 180;
                    if (img != null) {
                        c.drawBitmap(img, null, new RectF(x - 45, 130, x + 45, 220), p);
                    }
                    text(c, plantName(t), x, 245, Color.WHITE, 14, Paint.Align.CENTER);
                }
                text(c, "Chạm mầm để nhận rồi đặt xuống ô", getWidth() / 2f, getHeight() - 45,
                        Color.LTGRAY, 16, Paint.Align.CENTER);
            }

            if (miniTimer > 0) {
                text(c, "Thời gian: " + (int) Math.ceil(miniTimer),
                        getWidth() - 25, 30, Color.WHITE, 14, Paint.Align.RIGHT);
            }
        }

        void updateMiniGame() {
            miniTimer -= .03f;
            if (miniTimer <= 0 || miniKills >= miniNeed) {
                if (miniKills >= miniNeed) {
                    coins += 150;
                    if (random.nextFloat() < .45f) zenSeeds++;
                }
                miniGameActive = false;
                screen = SCREEN_PLAY;
                last = System.currentTimeMillis();
            }
        }

        void bar(Canvas c, float x, float y, float w, int hp, int max) {
            p.setColor(Color.RED);
            c.drawRect(x, y, x + w, y + 5, p);
            p.setColor(Color.GREEN);
            float q = Math.max(0f, Math.min(1f, hp / (float) Math.max(1, max)));
            c.drawRect(x, y, x + w * q, y + 5, p);
        }

        void text(Canvas c, String s, float x, float y, int color, float size, Paint.Align align) {
            p.setColor(color);
            p.setTextSize(size);
            p.setTextAlign(align);
            c.drawText(s, x, y, p);
        }

        void button(Canvas c, float l, float t, float r, float b, String s, int color) {
            p.setColor(color);
            c.drawRoundRect(new RectF(l, t, r, b), 8, 8, p);
            text(c, s, (l + r) / 2f, (t + b) / 2f + 6, Color.WHITE, 14, Paint.Align.CENTER);
        }

        Bitmap plantImage(int type) {
            if (type == SUNFLOWER) return sunImg;
            if (type == PEASHOOTER) return peaImg;
            if (type == GIGANUT) return gigaImg;
            if (type == CHOMPER) return chompImg;
            if (type == REPEATER) return repeaterImg;
            if (type == MINE) return mineImg;
            return null;
        }

        String plantName(int type) {
            if (type == SUNFLOWER) return "Sunflower";
            if (type == PEASHOOTER) return "Peashooter";
            if (type == GIGANUT) return "Giganut";
            if (type == CHOMPER) return "Chomper";
            if (type == REPEATER) return "Repeater";
            if (type == MINE) return "Mìn";
            return "Plant";
        }

        void startLevel(int level) {
            currentLevel = Math.max(0, Math.min(MAX_LEVEL, level));
            selected = PEASHOOTER;
            mode = MODE_NONE;
            wave = 0;
            totalWaves = totalWavesForLevel();
            waveSpawned = 0;
            spawned = 0;
            killed = 0;
            vinhSpawnedThisLevel = 0;
            levelWon = false;
            homeLose = false;
            homeZombie = null;
            paused = false;
            miniGameActive = false;

            for (int r = 0; r < ROWS; r++) Arrays.fill(plants[r], null);
            zombies.clear();
            peas.clear();
            bombs.clear();
            drops.clear();
            resetMowers();

            last = System.currentTimeMillis();
            spawnClock = last;
            waveClock = last;

            if (currentLevel >= 5 && random.nextFloat() < 0.45f) {
                miniGameActive = true;
                miniGameType = 1 + random.nextInt(3);
                miniTimer = 25f;
                miniKills = 0;
                for (int i = 0; i < 3; i++) {
                    int[] pool = {PEASHOOTER, SUNFLOWER, GIGANUT, CHOMPER, REPEATER, MINE};
                    miniPlants[i] = pool[random.nextInt(pool.length)];
                }
                screen = SCREEN_MINIGAME;
            } else {
                screen = SCREEN_LOADING;
            }
            loadStart = System.currentTimeMillis();
        }

        void update() {
            long now = System.currentTimeMillis();
            float dt = (now - last) / 1000f;
            if (dt < 0) dt = 0;
            if (dt > .08f) dt = .08f;
            last = now;

            if (waveSpawned < totalZombiesForWave(wave)) {
                if (now - spawnClock >= Math.max(850, 2200 - currentLevel * 100L)) {
                    spawnZombie();
                    spawnClock = now;
                }
            } else if (zombies.isEmpty()) {
                if (wave < totalWaves - 1) {
                    if (now - waveClock >= 1800) {
                        wave++;
                        waveSpawned = 0;
                        waveClock = now;
                        spawnClock = now;
                    }
                } else if (now - waveClock >= 1200) {
                    finishLevel();
                    return;
                }
            }

            updatePlants(dt);
            updatePeas(dt);
            updateBombs(dt);
            updateZombies(dt);
            updateMowers(dt);
            cleanDrops();
            clean();
        }

        int totalZombiesForWave(int w) {
            return totalZombiesForWaveAt(w);
        }

        void updatePlants(float dt) {
            for (int r = 0; r < ROWS; r++) {
                for (int col = 0; col < COLS; col++) {
                    Plant a = plants[r][col];
                    if (a == null) continue;

                    a.timer -= dt;
                    if (a.pfTime > 0) {
                        a.pfTime -= dt;
                        if (a.pfTime < 0) a.pfTime = 0;
                    }

                    if (a.type == MINE) {
                        if (!a.mineReady) {
                            a.mineTimer -= dt;
                            if (a.mineTimer <= 0) {
                                a.mineReady = true;
                            }
                        } else {
                            for (Zombie z : zombies) {
                                if (z.dead || z.row != a.row) continue;
                                if (Math.abs(z.x - (a.x + a.w / 2f)) < cellW * .42f) {
                                    z.hp -= 1800;
                                    z.mineHit = true;
                                    a.hp = 0;
                                    break;
                                }
                            }
                        }
                        continue;
                    }

                    if (a.type == SUNFLOWER && a.timer <= 0) {
                        sun += a.pfTime > 0 ? 100 : 50;
                        a.timer = a.pfTime > 0 ? .7f : 5f;
                    }

                    if ((a.type == PEASHOOTER || a.type == REPEATER) &&
                            a.timer <= 0 && rowHasZombie(a.row)) {
                        float px = a.x + a.w * .90f;
                        float py = a.y + a.h * .43f;
                        int damage = 20;
                        peas.add(new Pea(px, py, a.row, damage));
                        if (a.type == REPEATER) {
                            peas.add(new Pea(px, py - 14, a.row, damage));
                        }
                        a.timer = a.pfTime > 0 ? .35f : 1.1f;
                    }

                    // Giganut is a wall. It NEVER shoots.
                    if (a.type == GIGANUT && a.pfPermanent) {
                        a.hp = Math.min(a.maxHp, a.hp);
                    }

                    if (a.type == CHOMPER) {
                        if (a.chomperPF) {
                            pullChomper(a, dt);
                        } else if (a.timer > 0) {
                            // 40 second bite cooldown.
                        } else {
                            Zombie z = nearestZombie(a, 1.45f);
                            if (z != null) {
                                z.hp = 0;
                                a.timer = 40f;
                                beep(ToneGenerator.TONE_PROP_BEEP);
                            }
                        }
                    }
                }
            }
        }

        boolean rowHasZombie(int row) {
            for (Zombie z : zombies) {
                if (!z.dead && z.row == row) return true;
            }
            return false;
        }

        Zombie nearestZombie(Plant a, float cells) {
            float mouth = a.x + a.w * .55f;
            float best = Float.MAX_VALUE;
            Zombie out = null;
            for (Zombie z : zombies) {
                if (z.dead || z.row != a.row) continue;
                float d = Math.abs(z.x - mouth);
                if (d <= a.w * cells && z.x >= mouth - 8 && d < best) {
                    best = d;
                    out = z;
                }
            }
            return out;
        }

        void pullChomper(Plant a, float dt) {
            a.chompTime -= dt;
            float mouth = a.x + a.w * .55f;
            boolean any = false;
            float step = 500f * dt;

            for (Zombie z : zombies) {
                if (z.dead || z.row != a.row) continue;
                if (z.x < mouth - 2) continue;
                any = true;
                float dx = mouth - z.x;
                if (Math.abs(dx) <= step) {
                    z.hp = 0;
                } else {
                    z.x += dx > 0 ? step : -step;
                }
            }

            if (!any || a.chompTime <= 0) {
                a.chomperPF = false;
                a.chompTime = 0;
                a.timer = 0; // Plant Food resets the 40 second cooldown.
                beep(ToneGenerator.TONE_PROP_BEEP);
            }
        }

        void updatePeas(float dt) {
            Iterator<Pea> it = peas.iterator();
            while (it.hasNext()) {
                Pea b = it.next();
                b.x += 500f * dt;
                boolean hit = false;
                for (Zombie z : zombies) {
                    if (z.dead || z.row != b.row) continue;
                    if (Math.abs(z.x - b.x) < 28) {
                        z.hp -= b.damage;
                        hit = true;
                        break;
                    }
                }
                if (hit || b.x > getWidth() + 50) it.remove();
            }
        }

        void updateBombs(float dt) {
            Iterator<Bomb> it = bombs.iterator();
            while (it.hasNext()) {
                Bomb b = it.next();
                float dx = b.tx - b.x;
                float dy = b.ty - b.y;
                float d = (float) Math.sqrt(dx * dx + dy * dy);
                float step = 420f * dt;
                if (d <= step || d == 0) {
                    explodeBomb(b);
                    it.remove();
                } else {
                    b.x += dx / d * step;
                    b.y += dy / d * step;
                }
            }
        }

        void explodeBomb(Bomb b) {
            if (b.enemy) {
                int row = (int) ((b.ty - top) / cellH);
                int col = (int) ((b.tx - left) / cellW);
                for (int r = 0; r < ROWS; r++) {
                    for (int c = 0; c < COLS; c++) {
                        Plant a = plants[r][c];
                        if (a != null && Math.abs(r - row) <= 1 && Math.abs(c - col) <= 1) {
                            a.hp -= 250;
                        }
                    }
                }
                // Zombie Vĩnh Hùng has no sound.
            } else {
                for (Zombie z : zombies) {
                    if (z.dead) continue;
                    float dx = z.x - b.tx;
                    float dy = z.y - b.ty;
                    if (Math.sqrt(dx * dx + dy * dy) <= cellW * 1.35f) {
                        z.hp -= 500;
                    }
                }
                beep(ToneGenerator.TONE_PROP_BEEP2);
            }
        }

        void updateZombies(float dt) {
            for (Zombie z : zombies) {
                if (z.dead) continue;
                if (z.slow > 0) z.slow -= dt;

                if (z.vinh) {
                    // Move 5 fixed steps, then stand still. No sound.
                    if (z.stepsMoved < 5) {
                        z.stepTimer += dt;
                        if (z.stepTimer >= .32f) {
                            z.x -= 70f;
                            z.stepsMoved++;
                            z.stepTimer = 0;
                        }
                    } else {
                        z.bombTimer -= dt;
                        if (z.bombTimer <= 0) {
                            Plant target = firstPlant(z.row);
                            if (target != null) {
                                bombs.add(new Bomb(z.x, z.y,
                                        target.x + target.w / 2f,
                                        target.y + target.h / 2f, true));
                            }
                            z.bombTimer = 8f;
                        }
                    }
                    continue;
                }

                Plant target = findPlant(z);
                if (target != null) {
                    z.attackTimer -= dt;
                    if (target.type == MINE && !target.mineReady) {
                        // A buried mine is not triggered until it sprouts.
                    } else if (z.attackTimer <= 0) {
                        z.attackTimer = z.giga ? .65f : .8f;
                        if (target.type != MINE) target.hp -= z.giga ? 35 : 12;
                    }
                } else {
                    float speed = z.speed;
                    if (z.slow > 0) speed *= .45f;
                    z.x -= speed * dt;
                }

                if (z.x <= left - 25) {
                    Mower m = mowers[z.row];
                    if (!m.used) {
                        m.used = true;
                        m.active = true;
                        m.x = left - 45;
                        beep(ToneGenerator.TONE_PROP_ACK);
                    } else {
                        enterHouse(z);
                    }
                    z.dead = true;
                }
            }
        }

        Plant findPlant(Zombie z) {
            for (int col = 0; col < COLS; col++) {
                Plant a = plants[z.row][col];
                if (a == null) continue;
                float px = a.x + a.w / 2f;
                if (Math.abs(z.x - px) < (z.giga || z.zomGiga ? 60 : 48)) return a;
            }
            return null;
        }

        Plant firstPlant(int row) {
            for (int col = COLS - 1; col >= 0; col--) {
                if (plants[row][col] != null) return plants[row][col];
            }
            return null;
                    }
                                         
        void updateMowers(float dt) {
            for (Mower m : mowers) {
                if (!m.active) continue;
                m.x += 620f * dt;
                for (Zombie z : zombies) {
                    if (!z.dead && z.row == m.row && Math.abs(z.x - m.x) < 55) z.hp = 0;
                }
                if (m.x > getWidth() + 80) m.active = false;
            }
        }

        void cleanDrops() {
            Iterator<Drop> it = drops.iterator();
            while (it.hasNext()) {
                Drop d = it.next();
                d.life -= .03f;
                if (d.life <= 0) it.remove();
            }
        }

        void clean() {
            for (int r = 0; r < ROWS; r++) {
                for (int c = 0; c < COLS; c++) {
                    if (plants[r][c] != null && plants[r][c].hp <= 0) plants[r][c] = null;
                }
            }

            Iterator<Zombie> it = zombies.iterator();
            while (it.hasNext()) {
                Zombie z = it.next();
                if (z.hp <= 0 || z.dead) {
                    if (z.hp <= 0) {
                        killed++;
                        sun += 25;
                        float chance = .18f;
                        if (random.nextFloat() < chance) {
                            drops.add(new Drop(z.x, z.y, Drop.COIN));
                        }
                    }
                    it.remove();
                }
            }
        }

        void spawnZombie() {
            int rows = activeRows();
            int row = random.nextInt(Math.max(1, rows));

            boolean vinh = false;
            boolean zomGiga = false;
            boolean giga = false;

            if (currentLevel >= 4 && vinhSpawnedThisLevel < 3 &&
                    wave == totalWaves - 1 &&
                    (waveSpawned == 0 || waveSpawned == totalZombiesForWave(wave) / 2 ||
                     waveSpawned == totalZombiesForWave(wave) - 1)) {
                vinh = true;
            } else if (currentLevel >= 3 && wave >= 1 && waveSpawned % 5 == 0) {
                zomGiga = true;
            } else if (currentLevel >= 2 && waveSpawned % 4 == 0) {
                giga = true;
            }

            Zombie z = new Zombie(getWidth() + 70,
                    top + row * cellH + cellH * .48f,
                    row, giga, zomGiga, vinh,
                    zombieHpFor(giga, vinh, zomGiga));
            zombies.add(z);
            if (vinh) vinhSpawnedThisLevel++;
            spawned++;
            waveSpawned++;
        }

        void plantAt(int row, int col) {
            if (row >= activeRows()) return;
            if (plants[row][col] != null) return;

            if (!isUnlocked(selected)) return;

            int cost;
            int hp;
            if (selected == SUNFLOWER) { cost = 50; hp = 300; }
            else if (selected == PEASHOOTER) { cost = 100; hp = 400; }
            else if (selected == GIGANUT) { cost = 150; hp = 4000; }
            else if (selected == CHOMPER) { cost = 125; hp = 800; }
            else if (selected == REPEATER) { cost = 175; hp = 500; }
            else { cost = 25; hp = 1; }

            if (sun < cost) return;
            sun -= cost;

            Plant a = new Plant(selected, row, col, hp,
                    left + col * cellW, top + row * cellH, cellW, cellH);
            if (selected == MINE) {
                a.mineTimer = 30f;
                a.mineReady = false;
            }
            plants[row][col] = a;
            selected = PEASHOOTER;
            beep(ToneGenerator.TONE_PROP_BEEP);
        }

        boolean isUnlocked(int type) {
            if (type == PEASHOOTER) return true;
            if (type == SUNFLOWER) return sunflowerUnlocked;
            if (type == GIGANUT) return gigaUnlocked;
            if (type == CHOMPER) return chomperUnlocked;
            if (type == REPEATER) return repeaterUnlocked;
            if (type == MINE) return mineUnlocked;
            return false;
        }

        void usePlantFood(int row, int col) {
            if (plantFood <= 0) return;
            Plant a = plants[row][col];
            if (a == null || a.type == MINE) return;

            plantFood--;
            a.pfTime = 8f;
            a.timer = 0;

            if (a.type == GIGANUT) {
                if (!a.pfPermanent) {
                    a.maxHp = 8000;
                    a.hp = 8000;
                    a.pfPermanent = true;
                } else {
                    a.hp = Math.min(a.maxHp, a.hp + 4000);
                }
            } else if (a.type == CHOMPER) {
                a.chomperPF = true;
                a.chompTime = 4f;
                a.timer = 0; // reset 40s bite cooldown
            }
            beep(ToneGenerator.TONE_PROP_BEEP2);
        }

        void supportAttack(int row, int col, int type) {
            int cost = type == MODE_BOMB ? 50 : 90;
            if (coins < cost) return;
            coins -= cost;

            float cx = left + col * cellW + cellW / 2f;
            if (type == MODE_BOMB) {
                bombs.add(new Bomb(getWidth() - 30,
                        top + row * cellH + cellH / 2f,
                        cx, top + row * cellH + cellH / 2f));
            } else {
                for (Zombie z : zombies) {
                    if (z.dead || Math.abs(z.row - row) > 1) continue;
                    if (Math.abs(z.x - cx) <= cellW * 2f) {
                        z.hp -= 800;
                    }
                }
                beep(ToneGenerator.TONE_PROP_BEEP2);
            }
        }

        void shovelAt(int row, int col) {
            if (plants[row][col] != null) {
                plants[row][col] = null;
                mode = MODE_NONE;
                beep(ToneGenerator.TONE_PROP_ACK);
            }
        }

        void enterHouse(Zombie z) {
            homeLose = true;
            homeZombie = z.copyStandingAtHouse();
            failFadeStart = System.currentTimeMillis();
            screen = SCREEN_HOME_LOSE;
        }

        void finishLevel() {
            levelWon = true;
            screen = SCREEN_WIN;

            // Seed reward chance after all zombies are defeated.
            if (random.nextFloat() < .20f) zenSeeds++;

            // Unlock progression.
            if (currentLevel == 1) sunflowerUnlocked = true;
            if (currentLevel == 2) gigaUnlocked = true;
            if (currentLevel == 3) mineUnlocked = true;
            if (currentLevel == 4) chomperUnlocked = true;
            if (currentLevel == 5) repeaterUnlocked = true;

            if (currentLevel < MAX_LEVEL) {
                maxUnlocked = Math.max(maxUnlocked, currentLevel + 1);
            }
        }

        void nextLevel() {
            if (currentLevel >= MAX_LEVEL) {
                screen = SCREEN_WORLD;
                return;
            }
            startLevel(currentLevel + 1);
        }

        void resetMowers() {
            for (int r = 0; r < ROWS; r++) mowers[r] = new Mower(r);
        }

        void restartCurrentLevel() {
            sun = Math.max(500, sun);
            startLevel(currentLevel);
        }

        void exitToWorld() {
            screen = SCREEN_LOADING;
            loadStart = System.currentTimeMillis();
            // World is shown after the short loading screen.
            currentLevel = Math.max(0, Math.min(currentLevel, maxUnlocked));
            postExitWorld = true;
        }

        boolean postExitWorld = false;

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            if (e.getAction() != MotionEvent.ACTION_UP) return true;

            float x = e.getX();
            float y = e.getY();

            if (screen == SCREEN_WORLD) {
                handleWorldTouch(x, y);
                return true;
            }
            if (screen == SCREEN_ZEN) {
                if (x > getWidth() - 140 && y < 85) {
                    screen = SCREEN_WORLD;
                    invalidate();
                    return true;
                }
                if (y > 100) {
                    // Minimal Zen Garden action: tap a plant plot to spend a seed.
                    if (zenSeeds > 0) {
                        zenSeeds--;
                        beep(ToneGenerator.TONE_PROP_ACK);
                    }
                }
                invalidate();
                return true;
            }
            if (screen == SCREEN_LOADING) {
                return true;
            }
            if (screen == SCREEN_HOME_LOSE) {
                if (x > getWidth() / 2f - 110 && x < getWidth() / 2f + 110 &&
                        y > getHeight() - 115) {
                    int retryLevel = currentLevel;
                    startLevel(retryLevel);
                    screen = SCREEN_LOADING;
                    loadStart = System.currentTimeMillis();
                    homeLose = false;
                    postExitWorld = false;
                    startAfterLoading = true;
                    invalidate();
                }
                return true;
            }
            if (screen == SCREEN_MINIGAME) {
                handleMiniTouch(x, y);
                invalidate();
                return true;
            }
            if (screen == SCREEN_WIN) {
                float cy = getHeight() / 2f + 20;
                if (x >= getWidth() / 2f - 160 && x <= getWidth() / 2f - 20 && y >= cy && y <= cy + 60) {
                    nextLevel();
                } else if (x >= getWidth() / 2f + 20 && x <= getWidth() / 2f + 160 && y >= cy && y <= cy + 60) {
                    exitToWorld();
                }
                invalidate();
                return true;
            }
            if (screen == SCREEN_PAUSE) {
                if (x >= getWidth() / 2f - 135 && x <= getWidth() / 2f - 15 &&
                        y >= getHeight() / 2f && y <= getHeight() / 2f + 55) {
                    restartCurrentLevel();
                } else if (x >= getWidth() / 2f + 15 && x <= getWidth() / 2f + 135 &&
                        y >= getHeight() / 2f && y <= getHeight() / 2f + 55) {
                    exitToWorld();
                }
                invalidate();
                return true;
            }

            // Playing screen.
            if (x > getWidth() - 60 && y < 40) {
                screen = SCREEN_PAUSE;
                invalidate();
                return true;
            }

            float w = getWidth() / 7f;
            if (y >= 32 && y <= top - 8) {
                int pick = (int) (x / w);
                if (pick == 0 && sunflowerUnlocked) selected = SUNFLOWER;
                else if (pick == 1) selected = PEASHOOTER;
                else if (pick == 2 && gigaUnlocked) selected = GIGANUT;
                else if (pick == 3 && chomperUnlocked) selected = CHOMPER;
                else if (pick == 4 && repeaterUnlocked) selected = REPEATER;
                else if (pick == 5 && mineUnlocked) selected = MINE;
                else if (pick == 6) {
                    // Utility tray: top half PF, bottom half shovel.
                    if (y < 68) mode = MODE_PLANT_FOOD;
                    else mode = MODE_SHOVEL;
                    selected = PEASHOOTER;
                }
                invalidate();
                return true;
            }

            if (y < top || x < left || x >= left + COLS * cellW || y >= top + ROWS * cellH) {
                return true;
            }

            int col = (int) ((x - left) / cellW);
            int row = (int) ((y - top) / cellH);
            if (row < 0 || row >= activeRows() || col < 0 || col >= COLS) return true;

            // Pick drops first.
            Iterator<Drop> di = drops.iterator();
            while (di.hasNext()) {
                Drop d = di.next();
                if (Math.abs(d.x - x) < 30 && Math.abs(d.y - y) < 30) {
                    if (d.type == Drop.SUN) sun += 25;
                    else if (d.type == Drop.COIN) coins += 25;
                    else zenSeeds++;
                    di.remove();
                    beep(ToneGenerator.TONE_PROP_ACK);
                    invalidate();
                    return true;
                }
            }

            if (mode == MODE_PLANT_FOOD) {
                usePlantFood(row, col);
                mode = MODE_NONE;
            } else if (mode == MODE_SHOVEL) {
                shovelAt(row, col);
            } else if (mode == MODE_BOMB) {
                supportAttack(row, col, MODE_BOMB);
                mode = MODE_NONE;
            } else if (mode == MODE_FIRE) {
                supportAttack(row, col, MODE_FIRE);
                mode = MODE_NONE;
            } else if (selected >= 1 && selected <= 6) {
                plantAt(row, col);
            }

            invalidate();
            return true;
        }

        boolean startAfterLoading = false;

        void handleWorldTouch(float x, float y) {
            int cols = 5;
            for (int i = 0; i <= MAX_LEVEL; i++) {
                int row = i / cols;
                int col = i % cols;
                float bx = 70 + col * ((getWidth() - 140) / 5f);
                float by = 140 + row * 90;
                if (x >= bx && x <= bx + 95 && y >= by && y <= by + 60) {
                    if (i <= maxUnlocked) {
                        startLevel(i);
                    }
                    return;
                }
            }

            if (x > getWidth() - 190 && y > getHeight() - 90) {
                screen = SCREEN_ZEN;
                return;
            }
        }

        void handleMiniTouch(float x, float y) {
            if (miniGameType == 1) {
                if (miniKills < miniNeed) miniKills++;
                coins += 10;
            } else if (miniGameType == 2) {
                miniKills++;
                coins += 20;
            } else {
                miniKills++;
                zenSeeds += 1;
            }
        }

        class Plant {
            int type, row, col, hp, maxHp;
            float x, y, w, h;
            float timer = .1f;
            float pfTime = 0;
            float mineTimer = 30f;
            float chompTime = 0;
            boolean chomperPF = false;
            boolean pfPermanent = false;
            boolean mineReady = false;

            Plant(int type, int row, int col, int hp,
                  float x, float y, float w, float h) {
                this.type = type;
                this.row = row;
                this.col = col;
                this.hp = hp;
                this.maxHp = hp;
                this.x = x;
                this.y = y;
                this.w = w;
                this.h = h;
            }
        }

        class Zombie {
            float x, y, speed;
            float attackTimer = 0;
            float slow = 0;
            float bombTimer = 8;
            float stepTimer = 0;
            int stepsMoved = 0;
            int row, hp, maxHp;
            boolean giga, zomGiga, vinh, dead = false, mineHit = false;

            Zombie(float x, float y, int row, boolean giga,
                   boolean zomGiga, boolean vinh, int hp) {
                this.x = x;
                this.y = y;
                this.row = row;
                this.giga = giga;
                this.zomGiga = zomGiga;
                this.vinh = vinh;
                this.hp = hp;
                this.maxHp = hp;
                if (vinh) speed = 0;
                else if (zomGiga) speed = 20f;
                else if (giga) speed = 20f;
                else speed = 24f;
            }

            Zombie copyStandingAtHouse() {
                Zombie z = new Zombie(0, 0, row, giga, zomGiga, vinh, hp);
                z.x = x;
                z.y = y;
                z.dead = false;
                z.stepsMoved = 5;
                return z;
            }
        }

        class Pea {
            float x, y;
            int row, damage;
            Pea(float x, float y, int row, int damage) {
                this.x = x;
                this.y = y;
                this.row = row;
                this.damage = damage;
            }
        }

        class Bomb {
            float x, y, tx, ty;
            boolean enemy;
            Bomb(float x, float y, float tx, float ty) {
                this(x, y, tx, ty, false);
            }
            Bomb(float x, float y, float tx, float ty, boolean enemy) {
                this.x = x;
                this.y = y;
                this.tx = tx;
                this.ty = ty;
                this.enemy = enemy;
            }
        }

        class Drop {
            static final int SUN = 0;
            static final int COIN = 1;
            static final int SEED = 2;
            float x, y, life = 10f;
            int type;
            Drop(float x, float y, int type) {
                this.x = x;
                this.y = y;
                this.type = type;
            }
        }

        class Mower {
            int row;
            float x;
            boolean used = false;
            boolean active = false;
            Mower(int row) {
                this.row = row;
                this.x = left - 45;
            }
        }
    }
        }
                
