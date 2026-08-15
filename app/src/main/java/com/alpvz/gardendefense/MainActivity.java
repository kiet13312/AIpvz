package com.alpvz.gardendefense;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(new GameView());
    }

    class GameView extends View {

        Paint p = new Paint(3);
        Random r = new Random();

        Bitmap sunImg, peaImg, gigaImg;
        Bitmap chomperImg, zombieImg, bomberImg, bulletImg;

        ArrayList<Plant> plants = new ArrayList<>();
        ArrayList<Zombie> zombies = new ArrayList<>();
        ArrayList<Bullet> bullets = new ArrayList<>();
        ArrayList<BombBullet> bombBullets = new ArrayList<>();

        int selected = 0;
        int sun = 500;
        int plantFood = 0;
        int spawned = 0;
        int killed = 0;
        int level = 1;

        final int ROWS = 5;
        final int COLS = 9;

        float left = 20f;
        float top = 185f;
        float cellW;
        float cellH;

        long last;
        long spawnTime;

        boolean lose = false;
        boolean win = false;
        boolean chomperUnlocked = true;

        GameView() {
            super(MainActivity.this);

            sunImg = load("sun");
            peaImg = load("peashoot");
            gigaImg = load("giganut");
            chomperImg = load("chomper");

            zombieImg = load("zomplatz");
            bomberImg = load("zomvinhhung");
            bulletImg = load("gigapea");

            last = System.currentTimeMillis();
            spawnTime = last;
        }

        Bitmap load(String name) {
            int id = getResources().getIdentifier(
                    name,
                    "drawable",
                    getPackageName()
            );

            if (id == 0) {
                return null;
            }

            return BitmapFactory.decodeResource(
                    getResources(),
                    id
            );
        }

        @Override
        protected void onDraw(Canvas c) {

            cellW = (getWidth() - 40f) / COLS;
            cellH = (getHeight() - top - 20f) / ROWS;

            p.setColor(Color.rgb(95, 175, 70));
            c.drawRect(
                    0,
                    0,
                    getWidth(),
                    getHeight(),
                    p
            );

            topUI(c);
            drawBoard(c);
            drawPlants(c);
            drawBullets(c);
            drawBombBullets(c);
            drawZombies(c);

            if (!lose && !win) {
                update();
                postInvalidateDelayed(30);
            } else {
                drawEnd(c);
            }
        }

        void topUI(Canvas c) {

            p.setColor(Color.rgb(55, 120, 50));
            c.drawRect(
                    0,
                    0,
                    getWidth(),
                    165,
                    p
            );

            p.setColor(Color.WHITE);
            p.setTextSize(18);

            c.drawText(
                    "MÀN " + level +
                    "  SUN: " + sun +
                    "  PF: " + plantFood,
                    10,
                    25,
                    p
            );

            card(c, 5, 45, 1, "SUN", sunImg);
            card(c, 130, 45, 2, "PEA", peaImg);
            card(c, 255, 45, 3, "GIGA", gigaImg);

            if (chomperUnlocked) {
                card(c, 380, 45, 4, "CHOMP", chomperImg);
            }

            p.setColor(Color.WHITE);
            p.setTextSize(12);

            c.drawText(
                    "Plant Food",
                    505,
                    70,
                    p
            );

            c.drawText(
                    "chạm ô PF rồi chọn cây",
                    505,
                    90,
                    p
            );
        }

        void card(
                Canvas c,
                float x,
                float y,
                int type,
                String name,
                Bitmap img
        ) {

            p.setColor(
                    selected == type
                            ? Color.YELLOW
                            : Color.WHITE
            );

            c.drawRoundRect(
                    new RectF(
                            x,
                            y,
                            x + 115,
                            y + 90
                    ),
                    10,
                    10,
                    p
            );

            if (img != null) {
                c.drawBitmap(
                        img,
                        null,
                        new RectF(
                                x + 5,
                                y + 5,
                                x + 55,
                                y + 80
                        ),
                        p
                );
            }

            p.setColor(Color.DKGRAY);
            p.setTextSize(12);

            c.drawText(
                    name,
                    x + 60,
                    y + 48,
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

                    float x = left + col * cellW;
                    float y = top + row * cellH;

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

                Bitmap img;

                if (a.type == 1) {
                    img = sunImg;
                } else if (a.type == 2) {
                    img = peaImg;
                } else if (a.type == 3) {
                    img = gigaImg;
                } else {
                    img = chomperImg;
                }

                float x = left + a.col * cellW;
                float y = top + a.row * cellH;

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
                        x + cellW * 0.3f,
                        y + 4,
                        cellW * 0.4f,
                        a.hp,
                        a.max
                );

                if (a.type == 4 &&
                        a.chompCooldown > 0f) {

                    p.setColor(Color.WHITE);
                    p.setTextSize(10);

                    c.drawText(
                            String.format(
                                    "%.0fs",
                                    a.chompCooldown
                            ),
                            x + 5,
                            y + cellH - 5,
                            p
                    );
                }
            }
        }

        void drawBullets(Canvas c) {

            for (Bullet b : bullets) {

                if (bulletImg != null) {

                    c.drawBitmap(
                            bulletImg,
                            null,
                            new RectF(
                                    b.x - 21,
                                    b.y - 21,
                                    b.x + 21,
                                    b.y + 21
                            ),
                            p
                    );

                } else {

                    p.setColor(Color.GREEN);

                    c.drawCircle(
                            b.x,
                            b.y,
                            18,
                            p
                    );
                }
            }
        }

        void drawBombBullets(Canvas c) {

            for (BombBullet b : bombBullets) {

                p.setColor(Color.rgb(255, 70, 20));

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

        void drawZombies(Canvas c) {

            for (Zombie z : zombies) {

                float w;
                float h;

                if (z.bomber) {
                    w = 65;
                    h = 85;
                } else if (z.big) {
                    w = 110;
                    h = 150;
                } else {
                    w = 82;
                    h = 112;
                }

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
                            12,
                            12,
                            p
                    );
                }

                hp(
                        c,
                        z.x - 35,
                        z.y - 70,
                        70,
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
                int value,
                int max
        ) {

            p.setColor(Color.RED);

            c.drawRect(
                    x,
                    y,
                    x + w,
                    y + 5,
                    p
            );

            p.setColor(Color.GREEN);

            float q = Math.max(
                    0f,
                    Math.min(
                            1f,
                            value / (float) max
                    )
            );

            c.drawRect(
                    x,
                    y,
                    x + w * q,
                    y + 5,
                    p
            );
        }

        void update() {

            long now = System.currentTimeMillis();

            float dt =
                    (now - last) / 1000f;

            if (dt > 0.1f) {
                dt = 0.1f;
            }

            last = now;

            int total =
                    level == 1
                            ? 12
                            : level == 2
                                ? 15
                                : 18;

            if (spawned < total &&
                    now - spawnTime >= 4000) {

                spawn();

                spawnTime = now;
            }

            for (Plant a : plants) {

                a.timer += dt;

                if (a.foodTime > 0f) {

                    a.foodTime -= dt;

                    if (a.foodTime < 0f) {
                        a.foodTime = 0f;
                    }
                }

                if (a.chompCooldown > 0f) {

                    a.chompCooldown -= dt;

                    if (a.chompCooldown < 0f) {
                        a.chompCooldown = 0f;
                    }
                }

                if (a.type == 1) {

                    float interval =
                            a.foodTime > 0f
                                    ? 0.1f
                                    : 5f;

                    if (a.timer >= interval) {

                        sun += 100;
                        a.timer = 0f;
                    }
                }

                if (a.type == 2) {

                    float interval =
                            a.foodTime > 0f
                                    ? 0.1f
                                    : 1.2f;

                    if (a.timer >= interval &&
                            rowHasZombie(a.row)) {

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

                        a.timer = 0f;
                    }
                }

                /*
                 * CHOMPER
                 *
                 * Khi vừa đặt:
                 * chompCooldown = 0
                 * => ăn được ngay.
                 *
                 * Ăn xong:
                 * chompCooldown = 40 giây.
                 */
                if (a.type == 4 &&
                        a.chompCooldown <= 0f) {

                    Zombie z =
                            chompTarget(a);

                    if (z != null) {

                        z.hp = 0;

                        a.chompCooldown = 40f;
                    }
                }
            }

            for (Zombie z : zombies) {

                if (z.slow > 0f) {

                    z.slow -= dt;

                    if (z.slow < 0f) {
                        z.slow = 0f;
                    }
                }
            }

            updateBullets();
            updateBombBullets();
            updateZombies();
            clean();

            if (spawned >= total &&
                    zombies.isEmpty() &&
                    killed >= total) {

                win = true;
            }
        }

        boolean rowHasZombie(int row) {

            for (Zombie z : zombies) {

                if (z.row == row) {
                    return true;
                }
            }

            return false;
        }

        void updateBullets() {

            Iterator<Bullet> it =
                    bullets.iterator();

            while (it.hasNext()) {

                Bullet b = it.next();

                b.x += 8f;

                boolean hit = false;

                for (Zombie z : zombies) {

                    if (z.row == b.row &&
                            Math.abs(z.x - b.x) < 35f) {

                        z.hp -= 25;

                        hit = true;

                        break;
                    }
                }

                if (hit ||
                        b.x > getWidth() + 60) {

                    it.remove();
                }
            }
        }

        void updateBombBullets() {

            Iterator<BombBullet> it =
                    bombBullets.iterator();

            while (it.hasNext()) {

                BombBullet b = it.next();

                float dx = b.tx - b.x;
                float dy = b.ty - b.y;

                float d =
                        (float) Math.sqrt(
                                dx * dx + dy * dy
                        );

                if (d <= b.speed ||
                        d == 0f) {

                    bombExplode(
                            b.tx,
                            b.ty,
                            b.row
                    );

                    it.remove();

                } else {

                    b.x +=
                            dx / d *
                            b.speed;

                    b.y +=
                            dy / d *
                            b.speed;
                }
            }
        }

        void bombExplode(
                float x,
                float y,
                int row
        ) {

            int col = Math.max(
                    0,
                    Math.min(
                            COLS - 1,
                            (int) ((x - left) / cellW)
                    )
            );

            for (Plant a : plants) {

                if (Math.abs(a.col - col) <= 1 &&
                        Math.abs(a.row - row) <= 1) {

                    a.hp -= 100;
                }
            }
                }        void updateZombies() {

            for (Zombie z : zombies) {

                if (z.x < -70) {
                    lose = true;
                    return;
                }

                /*
                 * ZOMVINHHUNG
                 *
                 * Chỉ xuất hiện ở màn 3.
                 * Đi tới ô thứ 2 rồi đứng lại.
                 * Cứ 8 giây ném một quả bom.
                 */
                if (z.bomber) {

                    float stop =
                            left + cellW * 1.5f;

                    if (z.x > stop) {

                        z.x -= 0.6f;

                    } else {

                        z.x = stop;
                        z.stopped = true;
                    }

                    if (z.stopped) {

                        z.throwTimer += 30f;

                        if (z.throwTimer >= 8000f) {

                            bombBullets.add(
                                    new BombBullet(
                                            z.x,
                                            z.y,
                                            stop,
                                            z.y,
                                            z.row
                                    )
                            );

                            z.throwTimer = 0f;
                        }
                    }

                    continue;
                }

                Plant a = findPlant(z);

                if (a != null) {

                    long n =
                            System.currentTimeMillis();

                    if (n - a.lastBite >= 500) {

                        a.hp -=
                                z.big
                                        ? 150
                                        : 100;

                        a.lastBite = n;
                    }

                } else {

                    float speed =
                            z.big
                                    ? 0.55f
                                    : 1f;

                    if (z.slow > 0f) {
                        speed *= 0.45f;
                    }

                    z.x -= speed;
                }
            }
        }

        Plant findPlant(Zombie z) {

            for (Plant a : plants) {

                if (a.row != z.row) {
                    continue;
                }

                float px =
                        left +
                        a.col * cellW +
                        cellW / 2;

                if (Math.abs(z.x - px) <
                        (z.big ? 70f : 55f)) {

                    return a;
                }
            }

            return null;
        }

        Zombie chompTarget(Plant a) {

            for (Zombie z : zombies) {

                if (z.row != a.row) {
                    continue;
                }

                float px =
                        left +
                        a.col * cellW +
                        cellW / 2;

                if (Math.abs(z.x - px) <
                        cellW * 1.5f) {

                    return z;
                }
            }

            return null;
        }

        void spawn() {

            int row = r.nextInt(ROWS);

            boolean big = false;
            boolean bomber = false;

            if (level == 2) {

                big =
                        spawned >= 4 &&
                        spawned % 4 == 0;
            }

            if (level == 3) {

                if (spawned > 0 &&
                        spawned % 4 == 0) {

                    bomber = true;

                } else if (spawned % 3 == 0) {

                    big = true;
                }
            }

            zombies.add(
                    new Zombie(
                            getWidth() + 70,
                            top +
                            row * cellH +
                            cellH / 2,
                            row,
                            big,
                            bomber
                    )
            );

            spawned++;
        }

        void clean() {

            Iterator<Plant> pi =
                    plants.iterator();

            while (pi.hasNext()) {

                if (pi.next().hp <= 0) {
                    pi.remove();
                }
            }

            Iterator<Zombie> zi =
                    zombies.iterator();

            while (zi.hasNext()) {

                Zombie z = zi.next();

                if (z.hp <= 0) {

                    zi.remove();

                    killed++;

                    sun += 25;
                }
            }

            Iterator<Bullet> bi =
                    bullets.iterator();

            while (bi.hasNext()) {

                if (bi.next().x >
                        getWidth() + 60) {

                    bi.remove();
                }
            }
        }

        boolean occupied(
                int row,
                int col
        ) {

            for (Plant a : plants) {

                if (a.row == row &&
                        a.col == col) {

                    return true;
                }
            }

            return false;
        }

        /*
         * PLANT FOOD
         *
         * Chomper:
         * - hút toàn bộ zombie trên cùng hàng
         * - nuốt hết
         * - cooldown trở về 0 ngay
         * - có thể ăn tiếp ngay sau Plant Food
         */
        void usePlantFood(
                float x,
                float y
        ) {

            if (plantFood <= 0) {

                selected = 0;
                return;
            }

            int col = Math.max(
                    0,
                    Math.min(
                            COLS - 1,
                            (int) ((x - left) / cellW)
                    )
            );

            int row = Math.max(
                    0,
                    Math.min(
                            ROWS - 1,
                            (int) ((y - top) / cellH)
                    )
            );

            for (Plant a : plants) {

                if (a.row != row ||
                        a.col != col) {

                    continue;
                }

                if (a.type == 4) {

                    for (Zombie z : zombies) {

                        if (z.row == a.row) {
                            z.hp = 0;
                        }
                    }

                    a.chompCooldown = 0f;
                    a.timer = 0f;

                    plantFood--;

                    selected = 0;

                    return;
                }

                /*
                 * Plant Food thường cho Sunflower
                 * hoặc Peashooter vẫn được giữ.
                 */
                if (a.type == 1 ||
                        a.type == 2) {

                    a.foodTime = 3f;

                    plantFood--;

                    selected = 0;

                    return;
                }

                break;
            }

            selected = 0;
        }

        void drawEnd(Canvas c) {

            p.setColor(0xAA000000);

            c.drawRect(
                    0,
                    0,
                    getWidth(),
                    getHeight(),
                    p
            );

            p.setTextAlign(
                    Paint.Align.CENTER
            );

            p.setColor(Color.WHITE);
            p.setTextSize(36);

            if (win) {

                c.drawText(
                        "THẮNG MÀN " + level,
                        getWidth() / 2f,
                        getHeight() / 2f - 20,
                        p
                );

                p.setTextSize(18);

                c.drawText(
                        level < 3
                                ? "CHẠM ĐỂ SANG MÀN TIẾP"
                                : "CHẠM ĐỂ CHƠI LẠI",
                        getWidth() / 2f,
                        getHeight() / 2f + 35,
                        p
                );

            } else {

                c.drawText(
                        "THUA!",
                        getWidth() / 2f,
                        getHeight() / 2f - 20,
                        p
                );

                p.setTextSize(18);

                c.drawText(
                        "CHẠM ĐỂ CHƠI LẠI",
                        getWidth() / 2f,
                        getHeight() / 2f + 35,
                        p
                );
            }

            p.setTextAlign(
                    Paint.Align.LEFT
            );
        }

        void reset(boolean all) {

            plants.clear();
            zombies.clear();
            bullets.clear();
            bombBullets.clear();

            selected = 0;
            spawned = 0;
            killed = 0;

            lose = false;
            win = false;

            if (all) {

                level = 1;
                sun = 500;
                plantFood = 0;
            }

            last =
                    System.currentTimeMillis();

            spawnTime = last;
        }

        @Override
        public boolean onTouchEvent(
                MotionEvent e
        ) {

            if (e.getAction() !=
                    MotionEvent.ACTION_DOWN) {

                return true;
            }

            float x = e.getX();
            float y = e.getY();

            if (lose) {

                reset(true);
                invalidate();

                return true;
            }

            if (win) {

                if (level < 3) {

                    level++;
                    reset(false);

                } else {

                    reset(true);
                }

                invalidate();

                return true;
            }

            /*
             * CHỌN CÂY
             */
            if (y >= 45 &&
                    y <= 135) {

                if (x < 125) {

                    selected = 1;
                    invalidate();
                    return true;
                }

                if (x < 250) {

                    selected = 2;
                    invalidate();
                    return true;
                }

                if (x < 375) {

                    selected = 3;
                    invalidate();
                    return true;
                }

                if (x < 500 &&
                        chomperUnlocked) {

                    selected = 4;
                    invalidate();
                    return true;
                }

                /*
                 * Nút Plant Food.
                 */
                if (x >= 500) {

                    if (plantFood > 0) {
                        selected = 9;
                    }

                    invalidate();

                    return true;
                }
            }

            /*
             * DÙNG PLANT FOOD
             */
            if (selected == 9 &&
                    x >= left &&
                    x < left + COLS * cellW &&
                    y >= top &&
                    y < top + ROWS * cellH) {

                usePlantFood(x, y);

                invalidate();

                return true;
            }

            /*
             * ĐẶT CÂY
             */
            if (selected >= 1 &&
                    selected <= 4 &&
                    x >= left &&
                    x < left + COLS * cellW &&
                    y >= top &&
                    y < top + ROWS * cellH) {

                int col =
                        (int) ((x - left) / cellW);

                int row =
                        (int) ((y - top) / cellH);

                if (!occupied(row, col)) {

                    int cost;

                    if (selected == 1) {
                        cost = 50;
                    } else if (selected == 2) {
                        cost = 100;
                    } else {
                        cost = 150;
                    }

                    if (sun >= cost) {

                        int max;

                        if (selected == 1) {

                            max = 300;

                        } else if (selected == 2) {

                            max = 400;

                        } else if (selected == 3) {

                            /*
                             * GIGANUT:
                             * 3000 -> 6000 HP
                             * = x2 máu.
                             */
                            max = 6000;

                        } else {

                            /*
                             * CHOMPER
                             */
                            max = 800;
                        }

                        plants.add(
                                new Plant(
                                        selected,
                                        row,
                                        col,
                                        max
                                )
                        );

                        sun -= cost;

                        selected = 0;
                    }
                }

                invalidate();
            }

            return true;
        }
    }

    class Plant {

        int type;
        int row;
        int col;

        int hp;
        int max;

        float timer;
        float foodTime;

        /*
         * Chomper cooldown.
         * 0 = ăn được ngay.
         * 40 = vừa ăn xong, chờ 40 giây.
         */
        float chompCooldown;

        long lastBite;

        Plant(
                int t,
                int r,
                int c,
                int h
        ) {

            type = t;
            row = r;
            col = c;

            hp = h;
            max = h;

            timer = 0f;
            foodTime = 0f;
            chompCooldown = 0f;

            lastBite = 0L;
        }
    }

    class Zombie {

        float x;
        float y;

        float throwTimer;
        float slow;

        int row;
        int hp;
        int max;

        boolean big;
        boolean bomber;
        boolean stopped;

        Zombie(
                float xx,
                float yy,
                int rr,
                boolean b,
                boolean bo
        ) {

            x = xx;
            y = yy;
            row = rr;

            big = b;
            bomber = bo;
            stopped = false;

            throwTimer = 0f;
            slow = 0f;

            /*
             * Zomto = 1000 HP.
             *
             * Zomvinhhung = 500 HP.
             * => đúng bằng 1/2 Zomto.
             */
            if (bo) {

                hp = 500;

            } else if (b) {

                hp = 1000;

            } else {

                hp = 300;
            }

            max = hp;
        }
    }

    class Bullet {

        float x;
        float y;
        int row;

        Bullet(
                float xx,
                float yy,
                int rr
        ) {

            x = xx;
            y = yy;
            row = rr;
        }
    }

    class BombBullet {

        float x;
        float y;

        float tx;
        float ty;

        float speed = 5f;

        int row;

        BombBullet(
                float xx,
                float yy,
                float targetX,
                float targetY,
                int rr
        ) {

            x = xx;
            y = yy;

            tx = targetX;
            ty = targetY;

            row = rr;
        }
    }
                        }
