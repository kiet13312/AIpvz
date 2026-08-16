package com.alpvz.gardendefense;

import android.app.Activity;
import android.os.Bundle;
import android.content.SharedPreferences;
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

        setRequestedOrientation(
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

        sound = new ToneGenerator(
                AudioManager.STREAM_MUSIC, 45);

        setContentView(new GameView());
    }

    void beep(int tone) {
        try {
            if (sound != null)
                sound.startTone(tone, 55);
        } catch (Exception ignored) {}
    }

    @Override
    protected void onDestroy() {
        try {
            if (sound != null)
                sound.release();
        } catch (Exception ignored) {}
        super.onDestroy();
    }

    class GameView extends View {

        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Random rnd = new Random();

        final int ROWS = 5;
        final int COLS = 9;
        final int MAX_LEVEL = 9;

        final int SUN = 1;
        final int PEA = 2;
        final int GIGA = 3;
        final int CHOMP = 4;
        final int REPEAT = 5;
        final int MINE = 6;

        final int NONE = 0;
        final int PF = 10;
        final int SHOVEL = 11;

        final int WORLD = 0;
        final int PLAY = 1;
        final int PAUSE = 2;
        final int WIN = 3;
        final int LOSE = 4;

        Bitmap sunImg;
        Bitmap peaImg;
        Bitmap gigaImg;
        Bitmap chompImg;
        Bitmap repeatImg;
        Bitmap mineImg;
        Bitmap zombieImg;
        Bitmap bossImg;
        Bitmap peaBulletImg;

        Plant[][] plants = new Plant[ROWS][COLS];
        ArrayList<Zombie> zombies = new ArrayList<>();
        ArrayList<PeaShot> shots = new ArrayList<>();
        Mower[] mowers = new Mower[ROWS];

        float left;
        float top;
        float cellW;
        float cellH;

        int screen = WORLD;

        int level = 0;
        int unlocked = 0;

        int sun = 600;
        int coins = 9999;
        int plantFood = 3;

        int selected = PEA;
        int mode = NONE;

        int wave = 0;
        int waves = 3;
        int waveSpawned = 0;
        int killed = 0;

        boolean paused = false;

        long lastTime;
        long spawnTime;
        long waveTime;

        SharedPreferences save;

        boolean sunflowerOpen = false;
        boolean gigaOpen = false;
        boolean chompOpen = false;
        boolean repeatOpen = false;
        boolean mineOpen = false;

        GameView() {
            super(MainActivity.this);

            setFocusable(true);

            save = getSharedPreferences(
                    "pvz_save", MODE_PRIVATE);

            loadSave();

            sunImg = load("sun");
            peaImg = load("peashoot");
            gigaImg = load("giganut");
            chompImg = load("chomper");
            repeatImg = load("repeater");
            mineImg = load("min");

            zombieImg = load("zomplatz");
            bossImg = load("zomvinhhung");
            peaBulletImg = load("gigapea");

            resetMowers();

            lastTime = System.currentTimeMillis();
            spawnTime = lastTime;
            waveTime = lastTime;
        }

        Bitmap load(String name) {
            int id = getResources().getIdentifier(
                    name,
                    "drawable",
                    getPackageName());

            if (id == 0)
                return null;

            return BitmapFactory.decodeResource(
                    getResources(), id);
        }

        void loadSave() {
            unlocked = save.getInt("unlocked", 0);

            coins = save.getInt("coins", 9999);
            plantFood = save.getInt("pf", 3);

            sunflowerOpen =
                    save.getBoolean("sun", false);

            gigaOpen =
                    save.getBoolean("giga", false);

            mineOpen =
                    save.getBoolean("mine", false);

            chompOpen =
                    save.getBoolean("chomp", false);

            repeatOpen =
                    save.getBoolean("repeat", false);
        }

        void saveGame() {
            save.edit()
                    .putInt("unlocked", unlocked)
                    .putInt("coins", coins)
                    .putInt("pf", plantFood)
                    .putBoolean("sun", sunflowerOpen)
                    .putBoolean("giga", gigaOpen)
                    .putBoolean("mine", mineOpen)
                    .putBoolean("chomp", chompOpen)
                    .putBoolean("repeat", repeatOpen)
                    .apply();
        }

        @Override
        protected void onDraw(Canvas c) {
            layout();

            if (screen == WORLD) {
                drawWorld(c);
                return;
            }

            if (screen == PLAY) {
                drawGame(c);
                updateGame();
                postInvalidateDelayed(30);
                return;
            }

            if (screen == PAUSE) {
                drawGame(c);
                drawPause(c);
                return;
            }

            if (screen == WIN) {
                drawGame(c);
                drawWin(c);
                return;
            }

            if (screen == LOSE) {
                drawLose(c);
            }
        }

        void layout() {
            left = 48;
            top = getHeight() * .24f;

            cellW =
                    (getWidth() - left - 10) / COLS;

            cellH =
                    (getHeight() - top - 8) / ROWS;
        }

        int rows() {
            if (level <= 1)
                return 1;

            if (level <= 3)
                return 3;

            return 5;
        }

        void drawWorld(Canvas c) {
            c.drawColor(Color.rgb(20, 40, 25));

            text(c,
                    "GARDEN DEFENSE",
                    getWidth() / 2f,
                    55,
                    Color.WHITE,
                    30,
                    Paint.Align.CENTER);

            text(c,
                    "CHỌN MÀN",
                    getWidth() / 2f,
                    85,
                    Color.LTGRAY,
                    16,
                    Paint.Align.CENTER);

            for (int i = 0; i <= MAX_LEVEL; i++) {

                int r = i / 5;
                int col = i % 5;

                float x =
                        60 + col *
                        ((getWidth() - 120) / 5f);

                float y =
                        120 + r * 78;

                boolean open = i <= unlocked;

                p.setColor(
                        open
                        ? Color.rgb(50, 150, 65)
                        : Color.DKGRAY);

                c.drawRoundRect(
                        new RectF(
                                x,
                                y,
                                x + 90,
                                y + 55),
                        10,
                        10,
                        p);

                text(c,
                        i == 0
                        ? "HƯỚNG DẪN"
                        : "MÀN " + i,
                        x + 45,
                        y + 34,
                        Color.WHITE,
                        i == 0 ? 10 : 16,
                        Paint.Align.CENTER);
            }

            text(c,
                    "🪙 " + coins +
                    "    PF " + plantFood,
                    20,
                    getHeight() - 25,
                    Color.WHITE,
                    15,
                    Paint.Align.LEFT);
        }

        void drawGame(Canvas c) {

            c.drawColor(
                    Color.rgb(95, 170, 70));

            drawTop(c);
            drawBoard(c);
            drawMowers(c);
            drawPlants(c);
            drawShots(c);
            drawZombies(c);
        }

        void drawTop(Canvas c) {

            p.setColor(
                    Color.rgb(25, 65, 30));

            c.drawRect(
                    0,
                    0,
                    getWidth(),
                    top - 5,
                    p);

            text(c,
                    "MÀN " + level +
                    "  WAVE " +
                    (wave + 1) +
                    "/" + waves,
                    8,
                    21,
                    Color.WHITE,
                    14,
                    Paint.Align.LEFT);

            text(c,
                    "☀ " + sun +
                    "   🪙 " + coins +
                    "   PF " + plantFood,
                    getWidth() / 2f,
                    21,
                    Color.WHITE,
                    14,
                    Paint.Align.CENTER);

            button(
                    c,
                    getWidth() - 52,
                    5,
                    getWidth() - 8,
                    34,
                    "Ⅱ",
                    Color.DKGRAY);

            float w =
                    getWidth() / 7f;

            card(c, 0, "SUN",
                    SUN, sunImg,
                    w, sunflowerOpen);

            card(c, w, "PEA",
                    PEA, peaImg,
                    w, true);

            card(c, w * 2, "GIGA",
                    GIGA, gigaImg,
                    w, gigaOpen);

            card(c, w * 3, "CHOMP",
                    CHOMP, chompImg,
                    w, chompOpen);

            card(c, w * 4, "REPEAT",
                    REPEAT, repeatImg,
                    w, repeatOpen);

            card(c, w * 5, "MINE",
                    MINE, mineImg,
                    w, mineOpen);

            card(c, w * 6,
                    "UTIL",
                    NONE,
                    null,
                    w,
                    true);
        }

        void card(Canvas c,
                  float x,
                  String name,
                  int type,
                  Bitmap img,
                  float w,
                  boolean open) {

            boolean active =
                    selected == type &&
                    mode == NONE;

            p.setColor(
                    !open
                    ? Color.GRAY
                    : active
                    ? Color.YELLOW
                    : Color.WHITE);

            c.drawRoundRect(
                    new RectF(
                            x + 2,
                            34,
                            x + w - 2,
                            top - 9),
                    8,
                    8,
                    p);

            if (open && img != null) {
                c.drawBitmap(
                        img,
                        null,
                        new RectF(
                                x + 5,
                                39,
                                x + 50,
                                top - 15),
                        p);
            }

            text(c,
                    open ? name : "LOCK",
                    x + w / 2f + 14,
                    63,
                    open
                    ? Color.DKGRAY
                    : Color.LTGRAY,
                    9,
                    Paint.Align.CENTER);
        }

        void drawBoard(Canvas c) {

            int activeRows = rows();

            for (int r = 0; r < ROWS; r++) {

                for (int col = 0;
                     col < COLS;
                     col++) {

                    float x =
                            left + col * cellW;

                    float y =
                            top + r * cellH;

                    if (r >= activeRows) {
                        p.setColor(
                                Color.rgb(35, 70, 40));
                    } else {
                        p.setColor(
                                (r + col) % 2 == 0
                                ? Color.rgb(110, 185, 65)
                                : Color.rgb(100, 175, 60));
                    }

                    c.drawRect(
                            x,
                            y,
                            x + cellW - 2,
                            y + cellH - 2,
                            p);
                }
            }
        }

        void drawPlants(Canvas c) {

            for (int r = 0; r < ROWS; r++) {

                for (int col = 0;
                     col < COLS;
                     col++) {

                    Plant a =
                            plants[r][col];

                    if (a == null)
                        continue;

                    Bitmap img = plantImage(a.type);

                    if (img != null) {

                        c.drawBitmap(
                                img,
                                null,
                                new RectF(
                                        a.x + 3,
                                        a.y + 3,
                                        a.x + a.w - 3,
                                        a.y + a.h - 3),
                                p);

                    } else {

                        p.setColor(
                                Color.rgb(60, 180, 70));

                        c.drawCircle(
                                a.x + a.w / 2,
                                a.y + a.h / 2,
                                24,
                                p);
                    }

                    bar(
                            c,
                            a.x + 6,
                            a.y + 4,
                            a.w - 12,
                            a.hp,
                            a.maxHp);

                    if (a.pf > 0) {
                        text(c,
                                "PF",
                                a.x + 5,
                                a.y + 20,
                                Color.MAGENTA,
                                11,
                                Paint.Align.LEFT);
                    }

                    if (a.type == MINE &&
                        !a.ready) {

                        text(c,
                                "" +
                                (int)Math.ceil(
                                        a.timer),
                                a.x + a.w / 2,
                                a.y + a.h * .6f,
                                Color.WHITE,
                                13,
                                Paint.Align.CENTER);
                    }
                }
            }
        }

        Bitmap plantImage(int type) {
            if (type == SUN) return sunImg;
            if (type == PEA) return peaImg;
            if (type == GIGA) return gigaImg;
            if (type == CHOMP) return chompImg;
            if (type == REPEAT) return repeatImg;
            if (type == MINE) return mineImg;
            return null;
    }        void drawMowers(Canvas c) {
            for (Mower m : mowers) {
                float y = top + m.row * cellH + cellH * .70f;

                if (m.used && !m.active)
                    continue;

                p.setColor(Color.RED);
                c.drawRect(
                        m.x - 25, y - 13,
                        m.x + 25, y + 13, p);

                p.setColor(Color.DKGRAY);
                c.drawCircle(m.x - 15, y + 15, 8, p);
                c.drawCircle(m.x + 15, y + 15, 8, p);

                if (m.active) {
                    text(c, "⚡",
                            m.x,
                            y - 18,
                            Color.YELLOW,
                            16,
                            Paint.Align.CENTER);
                }
            }
        }

        void drawShots(Canvas c) {
            for (PeaShot s : shots) {
                p.setColor(
                        s.damage >= 50
                        ? Color.rgb(255, 190, 40)
                        : Color.rgb(50, 220, 70));

                c.drawCircle(
                        s.x,
                        s.y,
                        s.damage >= 50 ? 9 : 6,
                        p);
            }
        }

        void drawZombies(Canvas c) {
            for (Zombie z : zombies) {
                if (z.dead) continue;

                float w = z.boss ? 82 : 62;
                float h = z.boss ? 110 : 90;

                p.setColor(
                        z.boss
                        ? Color.rgb(80, 60, 90)
                        : Color.rgb(95, 95, 95));

                c.drawRoundRect(
                        new RectF(
                                z.x - w / 2,
                                z.y - h / 2,
                                z.x + w / 2,
                                z.y + h / 2),
                        12,
                        12,
                        p);

                p.setColor(Color.WHITE);
                c.drawCircle(
                        z.x - 13,
                        z.y - 18,
                        7,
                        p);

                c.drawCircle(
                        z.x + 13,
                        z.y - 18,
                        7,
                        p);

                p.setColor(Color.BLACK);
                c.drawCircle(
                        z.x - 13,
                        z.y - 18,
                        3,
                        p);

                c.drawCircle(
                        z.x + 13,
                        z.y - 18,
                        3,
                        p);

                if (z.boss) {
                    text(c,
                            "BOSS",
                            z.x,
                            z.y - h / 2 - 10,
                            Color.YELLOW,
                            11,
                            Paint.Align.CENTER);
                }

                bar(
                        c,
                        z.x - 30,
                        z.y - h / 2 - 7,
                        60,
                        z.hp,
                        z.maxHp);
            }
        }

        void drawPause(Canvas c) {
            p.setColor(0xaa000000);
            c.drawRect(
                    0,
                    0,
                    getWidth(),
                    getHeight(),
                    p);

            text(
                    c,
                    "TẠM DỪNG",
                    getWidth() / 2f,
                    getHeight() / 2f - 50,
                    Color.WHITE,
                    30,
                    Paint.Align.CENTER);

            button(
                    c,
                    getWidth() / 2f - 150,
                    getHeight() / 2f,
                    getWidth() / 2f - 20,
                    getHeight() / 2f + 55,
                    "CHƠI LẠI",
                    Color.rgb(45, 145, 65));

            button(
                    c,
                    getWidth() / 2f + 20,
                    getHeight() / 2f,
                    getWidth() / 2f + 150,
                    getHeight() / 2f + 55,
                    "THOÁT",
                    Color.rgb(110, 75, 70));
        }

        void drawWin(Canvas c) {
            p.setColor(0xdd000000);

            c.drawRect(
                    0,
                    0,
                    getWidth(),
                    getHeight(),
                    p);

            text(
                    c,
                    "CHIẾN THẮNG!",
                    getWidth() / 2f,
                    getHeight() / 2f - 65,
                    Color.WHITE,
                    30,
                    Paint.Align.CENTER);

            text(
                    c,
                    "Màn " + level + " hoàn thành",
                    getWidth() / 2f,
                    getHeight() / 2f - 25,
                    Color.YELLOW,
                    18,
                    Paint.Align.CENTER);

            if (level < MAX_LEVEL) {
                button(
                        c,
                        getWidth() / 2f - 160,
                        getHeight() / 2f + 15,
                        getWidth() / 2f - 20,
                        getHeight() / 2f + 70,
                        "MÀN TIẾP",
                        Color.rgb(45, 145, 65));
            }

            button(
                    c,
                    getWidth() / 2f + 20,
                    getHeight() / 2f + 15,
                    getWidth() / 2f + 160,
                    getHeight() / 2f + 70,
                    "THOÁT",
                    Color.rgb(110, 75, 70));
        }

        void drawLose(Canvas c) {
            p.setColor(0xdd000000);

            c.drawRect(
                    0,
                    0,
                    getWidth(),
                    getHeight(),
                    p);

            text(
                    c,
                    "ZOMBIE ĐÃ VÀO NHÀ!",
                    getWidth() / 2f,
                    getHeight() / 2f - 45,
                    Color.WHITE,
                    25,
                    Paint.Align.CENTER);

            text(
                    c,
                    "Máy cắt cỏ hàng đó đã được sử dụng.",
                    getWidth() / 2f,
                    getHeight() / 2f - 10,
                    Color.LTGRAY,
                    14,
                    Paint.Align.CENTER);

            button(
                    c,
                    getWidth() / 2f - 145,
                    getHeight() / 2f + 25,
                    getWidth() / 2f - 10,
                    getHeight() / 2f + 80,
                    "CHƠI LẠI",
                    Color.rgb(45, 145, 65));

            button(
                    c,
                    getWidth() / 2f + 10,
                    getHeight() / 2f + 25,
                    getWidth() / 2f + 145,
                    getHeight() / 2f + 80,
                    "THOÁT",
                    Color.rgb(110, 75, 70));
        }

        void updateGame() {
            long now = System.currentTimeMillis();

            float dt =
                    (now - lastTime) / 1000f;

            if (dt < 0)
                dt = 0;

            if (dt > .08f)
                dt = .08f;

            lastTime = now;

            if (waveSpawned < zombiesNeeded()) {

                if (now - spawnTime >
                        Math.max(
                                800,
                                2100 - level * 100)) {

                    spawnZombie();
                    spawnTime = now;
                }

            } else if (zombies.isEmpty()) {

                if (wave < waves - 1) {

                    if (now - waveTime > 1800) {
                        wave++;
                        waveSpawned = 0;
                        waveTime = now;
                        spawnTime = now;
                    }

                } else {
                    levelWin = true;
                    saveGame();
                    screen = WIN;
                    return;
                }
            }

            updatePlants(dt);
            updateShots(dt);
            updateZombies(dt);
            updateMowers(dt);

            cleanDead();
        }

        int zombiesNeeded() {
            return 5 + level * 2 + wave * 2;
        }

        void updatePlants(float dt) {

            for (int r = 0; r < ROWS; r++) {

                for (int col = 0;
                     col < COLS;
                     col++) {

                    Plant a = plants[r][col];

                    if (a == null)
                        continue;

                    a.timer -= dt;

                    if (a.pfTimer > 0)
                        a.pfTimer -= dt;

                    if (a.type == MINE) {

                        if (!a.ready) {
                            a.mineTimer -= dt;

                            if (a.mineTimer <= 0)
                                a.ready = true;

                        } else {

                            for (Zombie z : zombies) {

                                if (z.dead ||
                                    z.row != r)
                                    continue;

                                if (Math.abs(
                                        z.x -
                                        (a.x + a.w / 2))
                                        < cellW * .45f) {

                                    z.hp -= 2000;
                                    a.hp = 0;
                                    break;
                                }
                            }
                        }

                        continue;
                    }

                    if (a.type == SUN &&
                        a.timer <= 0) {

                        sun +=
                                a.pfTimer > 0
                                ? 100
                                : 50;

                        a.timer =
                                a.pfTimer > 0
                                ? .8f
                                : 5f;
                    }

                    if ((a.type == PEA ||
                         a.type == REPEAT) &&
                        a.timer <= 0 &&
                        rowHasZombie(r)) {

                        float bx =
                                a.x + a.w * .85f;

                        float by =
                                a.y + a.h * .45f;

                        shots.add(
                                new PeaShot(
                                        bx,
                                        by,
                                        r,
                                        a.type == REPEAT
                                        ? 35
                                        : 25));

                        if (a.type == REPEAT) {
                            shots.add(
                                    new PeaShot(
                                            bx,
                                            by - 15,
                                            r,
                                            35));
                        }

                        a.timer =
                                a.pfTimer > 0
                                ? .3f
                                : 1.1f;
                    }

                    if (a.type == CHOMP &&
                        a.timer <= 0) {

                        Zombie z =
                                nearestZombie(a);

                        if (z != null) {
                            z.hp = 0;
                            a.timer = 40f;
                        }
                    }
                }
            }
        }

        boolean rowHasZombie(int row) {

            for (Zombie z : zombies) {

                if (!z.dead &&
                    z.row == row)
                    return true;
            }

            return false;
        }

        Zombie nearestZombie(Plant a) {

            Zombie best = null;
            float dist = Float.MAX_VALUE;

            for (Zombie z : zombies) {

                if (z.dead ||
                    z.row != a.row)
                    continue;

                float d =
                        Math.abs(
                                z.x -
                                (a.x + a.w));

                if (d < cellW * 1.5f &&
                    d < dist) {

                    dist = d;
                    best = z;
                }
            }

            return best;
        }

        void updateShots(float dt) {

            Iterator<PeaShot> it =
                    shots.iterator();

            while (it.hasNext()) {

                PeaShot s = it.next();

                s.x += 520f * dt;

                boolean hit = false;

                for (Zombie z : zombies) {

                    if (z.dead ||
                        z.row != s.row)
                        continue;

                    if (Math.abs(
                            z.x - s.x) < 28) {

                        z.hp -= s.damage;
                        hit = true;
                        break;
                    }
                }

                if (hit ||
                    s.x > getWidth() + 40) {

                    it.remove();
                }
            }
        }

        void updateZombies(float dt) {

            for (Zombie z : zombies) {

                if (z.dead)
                    continue;

                Plant target =
                        findPlant(z);

                if (target != null) {

                    z.attack -= dt;

                    if (z.attack <= 0) {

                        z.attack =
                                z.boss
                                ? .55f
                                : .8f;

                        target.hp -=
                                z.boss
                                ? 30
                                : 12;
                    }

                } else {

                    z.x -= z.speed * dt;
                }

                if (z.x <= left - 25) {

                    Mower m =
                            mowers[z.row];

                    if (!m.used) {

                        m.used = true;
                        m.active = true;
                        m.x = left - 45;

                        beep(
                                ToneGenerator
                                .TONE_PROP_ACK);

                    } else {

                        screen = LOSE;

                        beep(
                                ToneGenerator
                                .TONE_PROP_NACK);

                        return;
                    }

                    z.dead = true;
                }
            }
        }

        Plant findPlant(Zombie z) {

            for (int col = 0;
                 col < COLS;
                 col++) {

                Plant a =
                        plants[z.row][col];

                if (a == null)
                    continue;

                float px =
                        a.x + a.w / 2;

                if (Math.abs(
                        z.x - px) <
                        (z.boss ? 65 : 48)) {

                    return a;
                }
            }

            return null;
        }        void updateMowers(float dt) {

            for (Mower m : mowers) {

                if (!m.active)
                    continue;

                m.x += 700f * dt;

                for (Zombie z : zombies) {

                    if (z.dead ||
                        z.row != m.row)
                        continue;

                    if (Math.abs(
                            z.x - m.x) < 65) {

                        z.hp = 0;
                    }
                }

                if (m.x >
                        getWidth() + 100) {

                    m.active = false;
                }
            }
        }

        void cleanDead() {

            for (int r = 0;
                 r < ROWS;
                 r++) {

                for (int c = 0;
                     c < COLS;
                     c++) {

                    Plant a = plants[r][c];

                    if (a != null &&
                        a.hp <= 0) {

                        plants[r][c] = null;
                    }
                }
            }

            Iterator<Zombie> it =
                    zombies.iterator();

            while (it.hasNext()) {

                Zombie z = it.next();

                if (z.hp <= 0 ||
                    z.dead) {

                    if (z.hp <= 0) {
                        coins += 10;
                    }

                    it.remove();
                }
            }
        }

        void spawnZombie() {

            int maxRows =
                    Math.max(1, rows());

            int row =
                    rnd.nextInt(maxRows);

            boolean boss =
                    level == MAX_LEVEL &&
                    wave == waves - 1 &&
                    waveSpawned == zombiesNeeded() - 1;

            int hp =
                    boss
                    ? 3500
                    : 500 + level * 100;

            float speed =
                    boss ? 10f : 23f;

            zombies.add(
                    new Zombie(
                            getWidth() + 70,
                            top +
                            row * cellH +
                            cellH * .5f,
                            row,
                            hp,
                            speed,
                            boss));

            waveSpawned++;
        }

        void plantAt(int row, int col) {

            if (row < 0 ||
                row >= rows())
                return;

            if (col < 0 ||
                col >= COLS)
                return;

            if (plants[row][col] != null)
                return;

            int cost;
            int hp;

            switch (selected) {

                case SUN:
                    cost = 50;
                    hp = 300;
                    break;

                case PEA:
                    cost = 100;
                    hp = 400;
                    break;

                case GIGA:
                    cost = 150;
                    hp = 4000;
                    break;

                case CHOMP:
                    cost = 125;
                    hp = 800;
                    break;

                case REPEAT:
                    cost = 175;
                    hp = 500;
                    break;

                case MINE:
                    cost = 25;
                    hp = 100;
                    break;

                default:
                    return;
            }

            if (sun < cost)
                return;

            sun -= cost;

            Plant a =
                    new Plant(
                            selected,
                            row,
                            col,
                            hp,
                            left + col * cellW,
                            top + row * cellH,
                            cellW,
                            cellH);

            if (selected == MINE) {
                a.mineTimer = 30f;
                a.ready = false;
            }

            plants[row][col] = a;

            selected = PEA;

            beep(
                    ToneGenerator
                    .TONE_PROP_BEEP);
        }

        void usePlantFood(int row, int col) {

            if (plantFood <= 0)
                return;

            Plant a =
                    plants[row][col];

            if (a == null)
                return;

            plantFood--;

            a.pfTimer = 8f;
            a.timer = 0;

            if (a.type == GIGA) {
                a.maxHp = 8000;
                a.hp = 8000;
            }

            if (a.type == CHOMP) {
                Zombie z =
                        nearestZombie(a);

                if (z != null)
                    z.hp = 0;
            }

            if (a.type == MINE) {
                a.ready = true;
            }

            saveGame();

            beep(
                    ToneGenerator
                    .TONE_PROP_BEEP2);
        }

        void shovel(int row, int col) {

            if (plants[row][col] != null) {
                plants[row][col] = null;
                mode = NONE;

                beep(
                        ToneGenerator
                        .TONE_PROP_ACK);
            }
        }

        boolean unlocked(int type) {

            if (type == PEA)
                return true;

            if (type == SUN)
                return sunflowerOpen;

            if (type == GIGA)
                return gigaOpen;

            if (type == CHOMP)
                return chompOpen;

            if (type == REPEAT)
                return repeatOpen;

            if (type == MINE)
                return mineOpen;

            return false;
        }

        void finishLevel() {

            levelWin = true;

            if (level < MAX_LEVEL) {
                unlocked = Math.max(
                        unlocked,
                        level);
            }

            if (level == 1)
                sunflowerOpen = true;

            if (level == 2)
                gigaOpen = true;

            if (level == 3)
                mineOpen = true;

            if (level == 4)
                chompOpen = true;

            if (level == 5)
                repeatOpen = true;

            saveGame();

            screen = WIN;
        }

        void startLevel(int n) {

            level =
                    Math.max(
                            1,
                            Math.min(
                                    MAX_LEVEL,
                                    n));

            wave = 0;
            waveSpawned = 0;
            killed = 0;

            levelWin = false;

            selected = PEA;
            mode = NONE;

            zombies.clear();
            shots.clear();

            for (int r = 0;
                 r < ROWS;
                 r++) {

                Arrays.fill(
                        plants[r],
                        null);
            }

            resetMowers();

            lastTime =
                    System.currentTimeMillis();

            spawnTime = lastTime;
            waveTime = lastTime;

            screen = PLAY;
        }

        void resetMowers() {

            for (int r = 0;
                 r < ROWS;
                 r++) {

                mowers[r] =
                        new Mower(r);

                mowers[r].x =
                        left - 45;
            }
        }

        @Override
        public boolean onTouchEvent(
                MotionEvent e) {

            if (e.getAction() !=
                    MotionEvent.ACTION_UP)
                return true;

            float x = e.getX();
            float y = e.getY();

            if (screen == WORLD) {

                int cols = 5;

                for (int i = 0;
                     i <= MAX_LEVEL;
                     i++) {

                    int rr = i / cols;
                    int cc = i % cols;

                    float bx =
                            60 +
                            cc *
                            ((getWidth() - 120)
                                    / 5f);

                    float by =
                            120 +
                            rr * 78;

                    if (x >= bx &&
                        x <= bx + 90 &&
                        y >= by &&
                        y <= by + 55) {

                        if (i == 0 ||
                            i <= unlocked + 1) {

                            startLevel(
                                    Math.max(1, i));
                        }

                        return true;
                    }
                }

                return true;
            }

            if (screen == WIN) {

                if (level < MAX_LEVEL &&
                    x >= getWidth()/2f-160 &&
                    x <= getWidth()/2f-20) {

                    startLevel(level + 1);

                } else if (
                        x >= getWidth()/2f+20) {

                    screen = WORLD;
                }

                invalidate();
                return true;
            }

            if (screen == LOSE) {

                if (x >= getWidth()/2f-145 &&
                    x <= getWidth()/2f+145) {

                    startLevel(level);
                }

                invalidate();
                return true;
            }

            if (screen == PAUSE) {

                if (x >= getWidth()/2f-150 &&
                    x <= getWidth()/2f-20) {

                    startLevel(level);

                } else if (
                        x >= getWidth()/2f+20 &&
                        x <= getWidth()/2f+150) {

                    screen = WORLD;
                }

                invalidate();
                return true;
            }

            if (screen != PLAY)
                return true;

            if (x > getWidth() - 60 &&
                y < 40) {

                screen = PAUSE;
                invalidate();
                return true;
            }

            float w =
                    getWidth() / 7f;

            if (y >= 32 &&
                y <= top - 8) {

                int pick =
                        (int)(x / w);

                if (pick == 0 &&
                    sunflowerOpen)
                    selected = SUN;

                else if (pick == 1)
                    selected = PEA;

                else if (pick == 2 &&
                         gigaOpen)
                    selected = GIGA;

                else if (pick == 3 &&
                         chompOpen)
                    selected = CHOMP;

                else if (pick == 4 &&
                         repeatOpen)
                    selected = REPEAT;

                else if (pick == 5 &&
                         mineOpen)
                    selected = MINE;

                else if (pick == 6) {

                    if (x > w * 6 &&
                        y < 70) {

                        mode = PF;

                    } else {

                        mode = SHOVEL;
                    }
                }

                invalidate();
                return true;
            }

            if (x < left ||
                x >= left + COLS * cellW ||
                y < top ||
                y >= top + ROWS * cellH) {

                return true;
            }

            int col =
                    (int)((x - left) / cellW);

            int row =
                    (int)((y - top) / cellH);

            if (row < 0 ||
                row >= rows() ||
                col < 0 ||
                col >= COLS)
                return true;

            if (mode == PF) {

                usePlantFood(row, col);
                mode = NONE;

            } else if (mode == SHOVEL) {

                shovel(row, col);

            } else {

                plantAt(row, col);
            }

            invalidate();
            return true;
        }

        void bar(Canvas c,
                 float x,
                 float y,
                 float w,
                 int hp,
                 int max) {

            p.setColor(Color.RED);

            c.drawRect(
                    x,
                    y,
                    x + w,
                    y + 5,
                    p);

            float q =
                    Math.max(
                            0,
                            Math.min(
                                    1,
                                    hp /
                                    (float)Math.max(
                                            1,
                                            max)));

            p.setColor(Color.GREEN);

            c.drawRect(
                    x,
                    y,
                    x + w * q,
                    y + 5,
                    p);
        }

        void text(Canvas c,
                  String s,
                  float x,
                  float y,
                  int color,
                  float size,
                  Paint.Align align) {

            p.setColor(color);
            p.setTextSize(size);
            p.setTextAlign(align);

            c.drawText(
                    s,
                    x,
                    y,
                    p);
        }

        void button(Canvas c,
                    float l,
                    float t,
                    float r,
                    float b,
                    String s,
                    int color) {

            p.setColor(color);

            c.drawRoundRect(
                    new RectF(
                            l,
                            t,
                            r,
                            b),
                    9,
                    9,
                    p);

            text(
                    c,
                    s,
                    (l + r) / 2,
                    (t + b) / 2 + 6,
                    Color.WHITE,
                    14,
                    Paint.Align.CENTER);
        }

        class Plant {

            int type;
            int row;
            int col;

            int hp;
            int maxHp;

            float x;
            float y;
            float w;
            float h;

            float timer = .2f;
            float pfTimer = 0;
            float mineTimer = 30f;

            boolean ready = false;

            Plant(int type,
                  int row,
                  int col,
                  int hp,
                  float x,
                  float y,
                  float w,
                  float h) {

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

            float x;
            float y;
            float speed;
            float attack = .5f;

            int row;
            int hp;
            int maxHp;

            boolean boss;
            boolean dead;

            Zombie(float x,
                   float y,
                   int row,
                   int hp,
                   float speed,
                   boolean boss) {

                this.x = x;
                this.y = y;
                this.row = row;
                this.hp = hp;
                this.maxHp = hp;
                this.speed = speed;
                this.boss = boss;
            }
        }

        class PeaShot {

            float x;
            float y;

            int row;
            int damage;

            PeaShot(float x,
                    float y,
                    int row,
                    int damage) {

                this.x = x;
                this.y = y;
                this.row = row;
                this.damage = damage;
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
