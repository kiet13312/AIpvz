package com.alpvz.gardendefense;

import android.app.Activity;
import android.os.Bundle;
import android.content.pm.ActivityInfo;
import android.graphics.*;
import android.view.*;
import android.view.WindowManager;
import java.util.*;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        setRequestedOrientation(
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        );

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

        setContentView(new GameView());
    }

    class GameView extends View {

        Paint p = new Paint(3);
        Random r = new Random();

        Bitmap sunImg, peaImg, gigaImg, zombieImg,
               bulletImg, chomperImg, bomberImg;

        ArrayList<Plant> plants = new ArrayList<>();
        ArrayList<Zombie> zombies = new ArrayList<>();
        ArrayList<Bullet> bullets = new ArrayList<>();
        ArrayList<BombBullet> bombBullets = new ArrayList<>();
        ArrayList<CoinDrop> coinDrops = new ArrayList<>();

        int selected = 0;
        int support = 0;

        int sun = 500;
        int coins = 9999;
        int plantFood = 0;

        int spawned = 0;
        int killed = 0;
        int level = 1;

        boolean lose = false;
        boolean win = false;
        boolean chomperUnlocked = false;

        final int ROWS = 5;
        final int COLS = 9;

        float left, top, cellW, cellH;
        long last, spawnTime;

        GameView() {
            super(MainActivity.this);

            sunImg = load("sun");
            peaImg = load("peashoot");
            gigaImg = load("giganut");
            zombieImg = load("zomplatz");
            bulletImg = load("gigapea");
            chomperImg = load("chomper");
            bomberImg = load("zomvinhhung");

            last = spawnTime =
                System.currentTimeMillis();
        }

        Bitmap load(String n) {
            int id = getResources().getIdentifier(
                n,
                "drawable",
                getPackageName()
            );

            return id == 0
                ? null
                : BitmapFactory.decodeResource(
                    getResources(), id
                );
        }

        @Override
        protected void onDraw(Canvas c) {

            float sw = getWidth();
            float sh = getHeight();

            left = sw * 0.18f;
            top = sh * 0.17f;

            float boardW = sw * 0.80f;
            float boardH = sh * 0.76f;

            cellW = boardW / COLS;
            cellH = boardH / ROWS;

            p.setColor(
                Color.rgb(95, 175, 70)
            );

            c.drawRect(
                0, 0,
                sw, sh,
                p
            );

            topUI(c);
            drawBoard(c);
            drawPlants(c);
            drawBullets(c);
            drawBombBullets(c);
            drawCoinDrops(c);
            drawZombies(c);

            if (!lose && !win) {
                update();
                postInvalidateDelayed(30);
            } else {
                drawEnd(c);
            }
        }

        void topUI(Canvas c) {

            p.setColor(
                Color.rgb(55, 120, 50)
            );

            c.drawRect(
                0, 0,
                getWidth(),
                top - 8,
                p
            );

            p.setColor(Color.WHITE);
            p.setTextSize(17);

            c.drawText(
                "MÀN " + level +
                "  SUN: " + sun +
                "  XU: " + coins +
                "  PF: " + plantFood,
                10, 25, p
            );

            float w = getWidth() / 4f;

            card(c, 5, 45, 1, "SUN", sunImg, w);
            card(c, w, 45, 2, "PEA", peaImg, w);
            card(c, w * 2, 45, 3, "GIGA", gigaImg, w);

            if (chomperUnlocked)
                card(
                    c, w * 3, 45,
                    4, "CHOMP",
                    chomperImg, w
                );

            skill(c, w * 4, 45, 1, "SAM", 30, w);
            skill(c, w * 5, 45, 2, "BANG", 60, w);
            skill(c, w * 6, 45, 3, "LUA", 90, w);

            foodCard(c, w * 7, 45, w);
        }

        void card(
            Canvas c,
            float x,
            float y,
            int t,
            String n,
            Bitmap img,
            float w
        ) {
            p.setColor(
                selected == t
                    ? Color.YELLOW
                    : Color.WHITE
            );

            c.drawRoundRect(
                new RectF(
                    x + 2, y,
                    x + w - 2, y + 85
                ),
                10, 10, p
            );

            if (img != null) {
                c.drawBitmap(
                    img,
                    null,
                    new RectF(
                        x + 5, y + 5,
                        x + w * .48f,
                        y + 78
                    ),
                    p
                );
            }

            p.setColor(Color.DKGRAY);
            p.setTextSize(11);

            c.drawText(
                n,
                x + w * .52f,
                y + 45,
                p
            );
        }

        void skill(
            Canvas c,
            float x,
            float y,
            int t,
            String n,
            int cost,
            float w
        ) {
            if (x >= getWidth())
                return;

            p.setColor(
                support == t
                    ? Color.YELLOW
                    : Color.WHITE
            );

            c.drawRoundRect(
                new RectF(
                    x + 2, y,
                    Math.min(
                        x + w - 2,
                        getWidth() - 2
                    ),
                    y + 85
                ),
                10, 10, p
            );

            p.setColor(
                t == 1
                    ? Color.YELLOW
                    : t == 2
                    ? Color.CYAN
                    : Color.RED
            );

            c.drawCircle(
                x + 25,
                y + 32,
                15,
                p
            );

            p.setColor(Color.DKGRAY);
            p.setTextSize(10);

            c.drawText(
                n,
                x + 48,
                y + 34,
                p
            );

            c.drawText(
                cost + " XU",
                x + 48,
                y + 55,
                p
            );
        }

        void foodCard(
            Canvas c,
            float x,
            float y,
            float w
        ) {
            if (x >= getWidth())
                return;

            p.setColor(
                support == 9
                    ? Color.YELLOW
                    : Color.WHITE
            );

            c.drawRoundRect(
                new RectF(
                    x + 2, y,
                    Math.min(
                        x + w - 2,
                        getWidth() - 2
                    ),
                    y + 85
                ),
                10, 10, p
            );

            p.setColor(
                Color.rgb(60, 210, 80)
            );

            c.drawCircle(
                x + 25,
                y + 32,
                15,
                p
            );

            p.setColor(Color.DKGRAY);
            p.setTextSize(10);

            c.drawText(
                "PF",
                x + 17,
                y + 35,
                p
            );

            c.drawText(
                "100 XU",
                x + 47,
                y + 32,
                p
            );

            c.drawText(
                "SL:" + plantFood,
                x + 47,
                y + 52,
                p
            );
        }

        void drawBoard(Canvas c) {

            for (int row = 0; row < ROWS; row++) {
                for (int col = 0; col < COLS; col++) {

                    p.setColor(
                        (row + col) % 2 == 0
                            ? Color.rgb(115, 190, 75)
                            : Color.rgb(105, 180, 68)
                    );

                    float x =
                        left + col * cellW;

                    float y =
                        top + row * cellH;

                    c.drawRect(
                        x,
                        y,
                        x + cellW - 2,
                        y + cellH - 2,
                        p
                    );
                }
            }
        }

        void drawPlants(Canvas c) {

            for (Plant a : plants) {

                Bitmap img =
                    a.type == 1
                        ? sunImg
                        : a.type == 2
                        ? peaImg
                        : a.type == 3
                        ? gigaImg
                        : chomperImg;

                float x =
                    left + a.col * cellW;

                float y =
                    top + a.row * cellH;

                if (img != null) {
                    c.drawBitmap(
                        img,
                        null,
                        new RectF(
                            x + 5,
                            y + 5,
                            x + cellW - 5,
                            y + cellH - 5
                        ),
                        p
                    );
                }

                hp(
                    c,
                    x + cellW * .2f,
                    y + 3,
                    cellW * .6f,
                    a.hp,
                    a.max
                );
            }
        }

        void drawBullets(Canvas c) {

            for (Bullet b : bullets) {

                if (bulletImg != null) {
                    c.drawBitmap(
                        bulletImg,
                        null,
                        new RectF(
                            b.x - 18,
                            b.y - 18,
                            b.x + 18,
                            b.y + 18
                        ),
                        p
                    );
                } else {

                    p.setColor(Color.GREEN);

                    c.drawCircle(
                        b.x,
                        b.y,
                        14,
                        p
                    );
                }
            }
        }

        void drawBombBullets(Canvas c) {

            for (BombBullet b : bombBullets) {

                p.setColor(
                    Color.rgb(255, 70, 20)
                );

                c.drawCircle(
                    b.x,
                    b.y,
                    13,
                    p
                );

                p.setColor(Color.YELLOW);

                c.drawCircle(
                    b.x - 4,
                    b.y - 4,
                    4,
                    p
                );
            }
        }

        void drawCoinDrops(Canvas c) {

            for (CoinDrop d : coinDrops) {

                p.setColor(
                    d.type == 1
                        ? Color.LTGRAY
                        : d.type == 2
                        ? Color.YELLOW
                        : Color.CYAN
                );

                c.drawCircle(
                    d.x,
                    d.y,
                    9,
                    p
                );
            }
        }

        void drawZombies(Canvas c) {

            for (Zombie z : zombies) {

                float w =
                    z.bomber
                        ? 65
                        : z.big
                        ? 105
                        : 82;

                float h =
                    z.bomber
                        ? 85
                        : z.big
                        ? 140
                        : 112;

                Bitmap img =
                    z.bomber
                        ? bomberImg
                        : zombieImg;

                if (img != null) {

                    c.drawBitmap(
                        img,
                        null,
                        new RectF(
                            z.x - w / 2,
                            z.y - h / 2,
                            z.x + w / 2,
                            z.y + h / 2
                        ),
                        p
                    );

                } else {

                    p.setColor(
                        z.bomber
                            ? Color.DKGRAY
                            : z.big
                            ? Color.BLACK
                            : Color.GRAY
                    );

                    c.drawRoundRect(
                        new RectF(
                            z.x - w / 2,
                            z.y - h / 2,
                            z.x + w / 2,
                            z.y + h / 2
                        ),
                        12, 12, p
                    );
                }

                hp(
                    c,
                    z.x - 30,
                    z.y -
                        (z.big ? 75 : 60),
                    60,
                    z.hp,
                    z.max
                );
            }
        }

        void hp(
            Canvas c,
            float x,
            float y,
            float w,
            int v,
            int m
        ) {
            p.setColor(Color.RED);

            c.drawRect(
                x, y,
                x + w, y + 5,
                p
            );

            p.setColor(Color.GREEN);

            float q =
                Math.max(
                    0,
                    Math.min(
                        1,
                        v / (float)Math.max(1, m)
                    )
                );

            c.drawRect(
                x, y,
                x + w * q,
                y + 5,
                p
            );
        }

        void update() {

            long now =
                System.currentTimeMillis();

            float dt =
                (now - last) / 1000f;

            if (dt > .1f)
                dt = .1f;

            last = now;

            int total =
                12 + (level - 1) * 3;

            if (
                spawned < total &&
                now - spawnTime >= 5000
            ) {
                spawn();
                spawnTime = now;
            }

            for (Plant a : plants) {

                a.timer += dt;

                if (a.foodTime > 0) {
                    a.foodTime -= dt;

                    if (a.foodTime < 0)
                        a.foodTime = 0;
                }

                if (
                    a.type == 1 &&
                    a.timer >=
                        (a.foodTime > 0
                            ? .1f
                            : 5f)
                ) {
                    sun += 50;
                    a.timer = 0;
                }

                if (
                    a.type == 2 &&
                    a.timer >=
                        (a.foodTime > 0
                            ? .1f
                            : 1.2f) &&
                    rowHas(a.row)
                ) {
                    bullets.add(
                        new Bullet(
                            left +
                                a.col * cellW +
                                cellW - 10,
                            top +
                                a.row * cellH +
                                cellH / 2,
                            a.row
                        )
                    );

                    a.timer = 0;
                }

                if (a.type == 4) {

                    if (a.chompCooldown > 0)
                        a.chompCooldown -= dt;

                    if (
                        !a.food &&
                        a.chompCooldown <= 0
                    ) {
                        Zombie z =
                            chompTarget(a);

                        if (z != null) {
                            z.hp = 0;
                            a.chompCooldown = 40f;
                        }
                    }

                    if (a.food) {

                        a.foodTimer -= dt;

                        float mx =
                            left +
                            a.col * cellW +
                            cellW / 2;

                        boolean any = false;

                        for (Zombie z : zombies) {

                            if (z.row != a.row)
                                continue;

                            any = true;

                            float dx = mx - z.x;

                            if (Math.abs(dx) > 6) {
                                z.x +=
                                    dx > 0
                                        ? Math.min(
                                            18,
                                            Math.abs(dx)
                                        )
                                        : -Math.min(
                                            18,
                                            Math.abs(dx)
                                        );
                            }
                        }

                        if (
                            !any ||
                            a.foodTimer <= 0
                        ) {

                            for (Zombie z : zombies) {
                                if (z.row == a.row)
                                    z.hp = 0;
                            }

                            a.food = false;
                            a.foodTimer = 0;
                            a.chompCooldown = 0;
                        }
                    }
                }
            }

            for (Zombie z : zombies) {

                if (z.slow > 0)
                    z.slow -= dt;
            }

            updateBullets();
            updateBombBullets();
            updateZombies();
            updateCoinDrops(dt);
            clean();

            if (
                spawned >= total &&
                zombies.isEmpty() &&
                killed >= total
            ) {
                win = true;
            }
        }

        boolean rowHas(int row) {

            for (Zombie z : zombies)
                if (z.row == row)
                    return true;

            return false;
        }

        void updateBullets() {

            Iterator<Bullet> it =
                bullets.iterator();

            while (it.hasNext()) {

                Bullet b = it.next();

                b.x += 8;

                boolean hit = false;

                for (Zombie z : zombies) {

                    if (
                        z.row == b.row &&
                        Math.abs(z.x - b.x) < 35
                    ) {
                        z.hp -= 25;
                        hit = true;
                        break;
                    }
                }

                if (
                    hit ||
                    b.x > getWidth() + 60
                )
                    it.remove();
            }
        }

        void updateBombBullets() {

          
