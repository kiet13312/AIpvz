package com.alpvz.gardendefense;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.*;
import android.view.*;
import java.util.*;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(new GameView());
    }

    class GameView extends View {

        Paint p = new Paint(3);
        Random r = new Random();

        Bitmap sanImg;
        Bitmap sunImg;
        Bitmap peaImg;
        Bitmap gigaImg;
        Bitmap bulletImg;
        Bitmap zombieImg;
        Bitmap chomperImg;
        Bitmap vinhhungImg;

        ArrayList<Plant> plants = new ArrayList<>();
        ArrayList<Zombie> zombies = new ArrayList<>();
        ArrayList<Bullet> bullets = new ArrayList<>();
        ArrayList<BombBullet> bombBullets = new ArrayList<>();
        ArrayList<SunDrop> sunDrops = new ArrayList<>();
        ArrayList<CoinDrop> coinDrops = new ArrayList<>();

        int selected = 0;

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

        float left = 20;
        float top = 210;

        float cellW;
        float cellH;

        long lastTime;
        long spawnTime;
        long sunTime;

        GameView() {

            super(MainActivity.this);

            /*
             * Ảnh sân:
             *
             * app/src/main/res/drawable/san.png
             *
             * Android resource dùng tên "san".
             */
            sanImg = load("san");

            sunImg = load("sun");
            peaImg = load("peashoot");
            gigaImg = load("giganut");
            bulletImg = load("gigapea");
            zombieImg = load("zomplatz");

            chomperImg = load("chomper");
            vinhhungImg = load("zomvinhhung");

            lastTime = System.currentTimeMillis();
            spawnTime = lastTime;
            sunTime = lastTime;
        }

        Bitmap load(String name) {

            int id = getResources().getIdentifier(
                    name,
                    "drawable",
                    getPackageName()
            );

            if (id == 0)
                return null;

            return BitmapFactory.decodeResource(
                    getResources(),
                    id
            );
        }

        @Override
        protected void onDraw(Canvas c) {

            /*
             * LƯỚI 5 x 9
             */
            cellW =
                    (getWidth() - 40f)
                            / COLS;

            cellH =
                    (getHeight() - top - 20f)
                            / ROWS;

            p.setColor(
                    Color.rgb(
                            90,
                            165,
                            70
                    )
            );

            c.drawRect(
                    0,
                    0,
                    getWidth(),
                    getHeight(),
                    p
            );

            drawTop(c);

            drawBoard(c);

            drawSunDrops(c);
            drawCoinDrops(c);

            drawPlants(c);

            drawBullets(c);
            drawBombBullets(c);

            drawZombies(c);

            if (!lose && !win) {

                updateGame();

                postInvalidateDelayed(30);

            } else {

                drawEnd(c);
            }
        }

        void drawTop(Canvas c) {

            p.setColor(
                    Color.rgb(
                            50,
                            110,
                            50
                    )
            );

            c.drawRect(
                    0,
                    0,
                    getWidth(),
                    200,
                    p
            );

            p.setColor(Color.WHITE);
            p.setTextSize(17);

            c.drawText(
                    "SUN: " + sun,
                    10,
                    24,
                    p
            );

            c.drawText(
                    "XU: " + coins,
                    10,
                    47,
                    p
            );

            c.drawText(
                    "PLANT FOOD: " + plantFood,
                    10,
                    70,
                    p
            );

            c.drawText(
                    "MÀN: " + level,
                    10,
                    93,
                    p
            );

            float bx = 150;

            float bw =
                    getWidth() - 165;

            p.setColor(Color.DKGRAY);

            c.drawRoundRect(
                    new RectF(
                            bx,
                            10,
                            bx + bw,
                            30
                    ),
                    8,
                    8,
                    p
            );

            p.setColor(Color.GREEN);

            float progress =
                    getTarget() == 0
                            ? 0
                            : Math.min(
                                    1f,
                                    killed /
                                            (float)getTarget()
                            );

            c.drawRoundRect(
                    new RectF(
                            bx,
                            10,
                            bx + bw * progress,
                            30
                    ),
                    8,
                    8,
                    p
            );

            p.setColor(Color.WHITE);
            p.setTextSize(12);

            c.drawText(
                    killed +
                            "/" +
                            getTarget(),
                    bx + 7,
                    25,
                    p
            );

            /*
             * MENU CÂY
             */

            float menuY = 105;
            float menuH = 42;

            float menuW =
                    getWidth() / 4f;

            card(
                    c,
                    0,
                    menuY,
                    menuW,
                    menuH,
                    1,
                    "SUN",
                    sunImg
            );

            card(
                    c,
                    menuW,
                    menuY,
                    menuW,
                    menuH,
                    2,
                    "PEA",
                    peaImg
            );

            card(
                    c,
                    menuW * 2,
                    menuY,
                    menuW,
                    menuH,
                    3,
                    "GIGA",
                    gigaImg
            );

            card(
                    c,
                    menuW * 3,
                    menuY,
                    menuW,
                    menuH,
                    4,
                    "CHOMPER",
                    chomperImg
            );

            /*
             * KỸ NĂNG
             */

            skill(
                    c,
                    5,
                    155,
                    menuW - 10,
                    35,
                    "SẤM 30"
            );

            skill(
                    c,
                    menuW + 5,
                    155,
                    menuW - 10,
                    35,
                    "BĂNG 60"
            );

            skill(
                    c,
                    menuW * 2 + 5,
                    155,
                    menuW - 10,
                    35,
                    "LỬA 90"
            );

            skill(
                    c,
                    menuW * 3 + 5,
                    155,
                    menuW - 10,
                    35,
                    "PF 100"
            );
        }

        void card(
                Canvas c,
                float x,
                float y,
                float w,
                float h,
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
                            x + 3,
                            y + 2,
                            x + w - 3,
                            y + h - 2
                    ),
                    7,
                    7,
                    p
            );

            if (img != null) {

                c.drawBitmap(
                        img,
                        null,
                        new RectF(
                                x + 4,
                                y + 4,
                                x + h - 2,
                                y + h - 4
                        ),
                        p
                );
            }

            p.setColor(Color.DKGRAY);
            p.setTextSize(10);

            c.drawText(
                    name,
                    x + h + 2,
                    y + 25,
                    p
            );
        }

        void skill(
                Canvas c,
                float x,
                float y,
                float w,
                float h,
                String text
        ) {

            p.setColor(Color.WHITE);

            c.drawRoundRect(
                    new RectF(
                            x,
                            y,
                            x + w,
                            y + h
                    ),
                    7,
                    7,
                    p
            );

            p.setColor(Color.DKGRAY);
            p.setTextSize(10);

            c.drawText(
                    text,
                    x + 5,
                    y + 22,
                    p
            );
        }

        void drawBoard(Canvas c) {

            /*
             * san.png được kéo vừa đúng vùng 5 x 9.
             *
             * Nếu ảnh sân không tồn tại thì dùng
             * nền ô mặc định.
             */

            if (sanImg != null) {

                c.drawBitmap(
                        sanImg,
                        null,
                        new RectF(
                                left,
                                top,
                                left + COLS * cellW,
                                top + ROWS * cellH
                        ),
                        p
                );

            } else {

                for (int row = 0;
                     row < ROWS;
                     row++) {

                    for (int col = 0;
                         col < COLS;
                         col++) {

                        p.setColor(
                                (row + col) % 2 == 0
                                        ? Color.rgb(
                                                115,
                                                190,
                                                75
                                        )
                                        : Color.rgb(
                                                105,
                                                180,
                                                68
                                        )
                        );

                        float x =
                                left +
                                col * cellW;

                        float y =
                                top +
                                row * cellH;

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
        }

        void drawPlants(Canvas c) {

            for (Plant a : plants) {

                Bitmap img;

                if (a.type == 1)
                    img = sunImg;
                else if (a.type == 2)
                    img = peaImg;
                else if (a.type == 3)
                    img = gigaImg;
                else
                    img = chomperImg;

                float x =
                        left +
                        a.col * cellW;

                float y =
                        top +
                        a.row * cellH;

                if (img != null) {

                    /*
                     * ẢNH PLANT 256x256:
                     * tự co theo kích thước ô.
                     */

                    float padX =
                            cellW * 0.08f;

                    float padY =
                            cellH * 0.05f;

                    float boxW =
                            cellW -
                            padX * 2;

                    float boxH =
                            cellH -
                            padY * 2;

                    float size =
                            Math.min(
                                    boxW,
                                    boxH
                            );

                    float drawX =
                            x +
                            (cellW - size) / 2f;

                    float drawY =
                            y +
                            (cellH - size) / 2f;

                    c.drawBitmap(
                            img,
                            null,
                            new RectF(
                                    drawX,
                                    drawY,
                                    drawX + size,
                                    drawY + size
                            ),
                            p
                    );
                }

                drawHP(
                        c,
                        x + cellW * .18f,
                        y + 3,
                        cellW * .64f,
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

            for (BombBullet b :
                    bombBullets) {

                /*
                 * ĐẠN RIÊNG CỦA ZOMVINHHUNG
                 */

                p.setColor(
                        Color.rgb(
                                180,
                                40,
                                220
                        )
                );

                c.drawCircle(
                        b.x,
                        b.y,
                        12,
                        p
                );
            }
        }

        void drawZombies(Canvas c) {

            for (Zombie z : zombies) {

                float w;
                float h;

                Bitmap img;

                if (z.bomber) {

                    w = 70;
                    h = 100;

                    img = vinhhungImg;

                } else if (z.big) {

                    w = 95;
                    h = 125;

                    img = zombieImg;

                } else {

                    w = 76;
                    h = 105;

                    img = zombieImg;
                }

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
                                    ? Color.rgb(
                                            100,
                                            40,
                                            140
                                    )
                                    : Color.GRAY
                    );

                    c.drawRoundRect(
                            new RectF(
                                    z.x - w / 2,
                                    z.y - h / 2,
                                    z.x + w / 2,
                                    z.y + h / 2
                            ),
                            10,
                            10,
                            p
                    );
                }

                drawHP(
                        c,
                        z.x - 30,
                        z.y - h / 2 - 8,
                        60,
                        z.hp,
                        z.max
                );
            }
        }

        void drawHP(
                Canvas c,
                float x,
                float y,
                float w,
                int hp,
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

            float q =
                    Math.max(
                            0,
                            Math.min(
                                    1,
                                    hp /
                                            (float)max
                            )
                    );

            c.drawRect(
                    x,
                    y,
                    x + w * q,
                    y + 5,
                    p
            );
                }        void updateGame() {

            long now =
                    System.currentTimeMillis();

            float dt =
                    (now - lastTime) / 1000f;

            if (dt > .1f)
                dt = .1f;

            lastTime = now;

            /*
             * MẶT TRỜI RƠI
             */

            if (now - sunTime >= 7000) {

                addSunDrop();

                sunTime = now;
            }

            /*
             * ZOMBIE XUẤT HIỆN
             */

            if (spawned < getTarget()
                    && now - spawnTime >=
                    getSpawnDelay()) {

                spawnZombie();

                spawnTime = now;
            }

            /*
             * CÂY
             */

            for (Plant a : plants) {

                a.timer += dt;

                /*
                 * PLANT FOOD:
                 * cooldown 0.1 giây trong 3 giây.
                 */

                if (a.foodTime > 0)
                    a.foodTime -= dt;

                if (a.type == 1) {

                    float cooldown =
                            a.foodTime > 0
                                    ? .1f
                                    : 5f;

                    if (a.timer >= cooldown) {

                        sun += 50;

                        a.timer = 0;
                    }
                }

                if (a.type == 2) {

                    float cooldown =
                            a.foodTime > 0
                                    ? .1f
                                    : 1.2f;

                    if (a.timer >= cooldown
                            && rowHasZombie(a.row)) {

                        bullets.add(
                                new Bullet(
                                        left +
                                                a.col *
                                                cellW +
                                                cellW -
                                                10,
                                        top +
                                                a.row *
                                                cellH +
                                                cellH / 2,
                                        a.row
                                )
                        );

                        a.timer = 0;
                    }
                }

                /*
                 * CHOMPER
                 * cooldown 60 giây.
                 */

                if (a.type == 4) {

                    if (a.timer >= 60f) {

                        Zombie target =
                                findChomperTarget(a);

                        if (target != null) {

                            target.hp = 0;

                            a.timer = 0;
                        }
                    }
                }
            }

            updateSunDrops();
            updateCoinDrops();

            updateBullets();
            updateBombBullets();

            updateZombies();

            clean();

            if (spawned >= getTarget()
                    && zombies.isEmpty()
                    && killed >= getTarget()) {

                win = true;
            }
        }

        int getTarget() {

            if (level == 1)
                return 15;

            if (level == 2)
                return 18;

            return 22;
        }

        long getSpawnDelay() {

            if (level == 1)
                return 4000;

            if (level == 2)
                return 3300;

            return 2800;
        }

        boolean rowHasZombie(int row) {

            for (Zombie z : zombies) {

                if (z.row == row)
                    return true;
            }

            return false;
        }

        void updateBullets() {

            Iterator<Bullet> it =
                    bullets.iterator();

            while (it.hasNext()) {

                Bullet b = it.next();

                b.x += 8;

                boolean hit = false;

                for (Zombie z :
                        zombies) {

                    if (z.row == b.row
                            && Math.abs(
                                    z.x - b.x
                            ) < 32) {

                        z.hp -= 25;

                        hit = true;

                        break;
                    }
                }

                if (hit
                        || b.x >
                        getWidth() + 50) {

                    it.remove();
                }
            }
        }

        void updateBombBullets() {

            Iterator<BombBullet> it =
                    bombBullets.iterator();

            while (it.hasNext()) {

                BombBullet b = it.next();

                /*
                 * ĐẠN BAY TỪ PHẢI SANG TRÁI
                 */

                b.x -= 5;

                for (Plant a : plants) {

                    float px =
                            left +
                            a.col * cellW +
                            cellW / 2;

                    if (a.row == b.row
                            && Math.abs(
                                    px - b.x
                            ) < 28) {

                        /*
                         * 100 DAMAGE
                         */

                        a.hp -= 100;

                        b.hit = true;

                        break;
                    }
                }

                if (b.hit
                        || b.x < -50) {

                    it.remove();
                }
            }
        }

        void updateZombies() {

            for (Zombie z :
                    zombies) {

                if (z.x < -70) {

                    lose = true;

                    return;
                }

                /*
                 * ZOMVINHHUNG
                 *
                 * ĐỨNG YÊN
                 * KHÔNG ĐI VỀ PHÍA CÂY
                 * NÉM MỖI 8 GIÂY
                 */

                if (z.bomber) {

                    z.throwTimer += 30;

                    if (z.throwTimer >= 8000) {

                        bombBullets.add(
                                new BombBullet(
                                        z.x - 35,
                                        z.y,
                                        z.row
                                )
                        );

                        z.throwTimer = 0;
                    }

                    continue;
                }

                Plant target =
                        findPlant(z);

                if (target != null) {

                    /*
                     * ZOMTO:
                     * CHỈ TRÂU MÁU,
                     * KHÔNG CÓ KỸ NĂNG.
                     */

                    long now =
                            System.currentTimeMillis();

                    if (now -
                            target.lastBite
                            >= 500) {

                        target.hp -= 100;

                        target.lastBite = now;
                    }

                } else {

                    z.x -=
                            z.big
                                    ? .6f
                                    : 1.2f;
                }
            }
        }

        Plant findPlant(Zombie z) {

            for (Plant a :
                    plants) {

                if (a.row != z.row)
                    continue;

                float px =
                        left +
                        a.col * cellW +
                        cellW / 2;

                if (Math.abs(
                        z.x - px
                ) < 55) {

                    return a;
                }
            }

            return null;
        }

        Zombie findChomperTarget(
                Plant a
        ) {

            for (Zombie z :
                    zombies) {

                if (z.row != a.row)
                    continue;

                float px =
                        left +
                        a.col * cellW +
                        cellW / 2;

                if (Math.abs(
                        z.x - px
                ) < cellW * 1.5f) {

                    return z;
                }
            }

            return null;
        }

        void spawnZombie() {

            int row =
                    r.nextInt(ROWS);

            int type =
                    r.nextInt(5);

            /*
             * ZOMTO CHỈ XUẤT HIỆN
             * Ở MÀN 2/3.
             */

            boolean big =
                    level >= 2 &&
                    type == 0;

            /*
             * ZOMVINHHUNG
             * KHÔNG XUẤT HIỆN MÀN 1.
             */

            boolean bomber =
                    level >= 2 &&
                    type == 1;

            zombies.add(
                    new Zombie(
                            getWidth() + 60,
                            top +
                                    row *
                                    cellH +
                                    cellH / 2,
                            row,
                            big,
                            bomber
                    )
            );

            spawned++;
        }

        void addSunDrop() {

            sunDrops.add(
                    new SunDrop(
                            30 +
                                    r.nextInt(
                                            Math.max(
                                                    40,
                                                    getWidth() - 60
                                            )
                                    ),
                            205
                    )
            );
        }

        void updateSunDrops() {

            Iterator<SunDrop> it =
                    sunDrops.iterator();

            while (it.hasNext()) {

                SunDrop s = it.next();

                s.y += 1.5f;

                /*
                 * TỰ BIẾN MẤT SAU KHI
                 * RƠI GẦN CUỐI SÂN.
                 */

                if (s.y >
                        getHeight() - 25) {

                    it.remove();
                }
            }
        }

        void updateCoinDrops() {

            Iterator<CoinDrop> it =
                    coinDrops.iterator();

            while (it.hasNext()) {

                CoinDrop d = it.next();

                d.y += .5f;

                if (d.y >
                        getHeight() - 25) {

                    it.remove();
                }
            }
        }

        void clean() {

            Iterator<Plant> pi =
                    plants.iterator();

            while (pi.hasNext()) {

                if (pi.next().hp <= 0)
                    pi.remove();
            }

            Iterator<Zombie> zi =
                    zombies.iterator();

            while (zi.hasNext()) {

                Zombie z = zi.next();

                if (z.hp <= 0) {

                    zi.remove();

                    killed++;

                    sun += 25;

                    dropCoin(z.x, z.y);
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

        void dropCoin(
                float x,
                float y
        ) {

            /*
             * 10% bạc = 25 xu
             * 5% vàng = 50 xu
             * 1% kim cương = 100 xu
             *
             * Phần còn lại không rơi xu.
             */

            int n =
                    r.nextInt(100);

            if (n < 10) {

                coinDrops.add(
                        new CoinDrop(
                                x,
                                y,
                                1,
                                25
                        )
                );

            } else if (n < 15) {

                coinDrops.add(
                        new CoinDrop(
                                x,
                                y,
                                2,
                                50
                        )
                );

            } else if (n < 16) {

                coinDrops.add(
                        new CoinDrop(
                                x,
                                y,
                                3,
                                100
                        )
                );
            }
        }

        void drawSunDrops(Canvas c) {

            for (SunDrop s :
                    sunDrops) {

                if (sunImg != null) {

                    c.drawBitmap(
                            sunImg,
                            null,
                            new RectF(
                                    s.x - 20,
                                    s.y - 20,
                                    s.x + 20,
                                    s.y + 20
                            ),
                            p
                    );

                } else {

                    p.setColor(Color.YELLOW);

                    c.drawCircle(
                            s.x,
                            s.y,
                            17,
                            p
                    );
                }
            }
        }

        void drawCoinDrops(Canvas c) {

            for (CoinDrop d :
                    coinDrops) {

                if (d.type == 1)
                    p.setColor(
                            Color.LTGRAY
                    );
                else if (d.type == 2)
                    p.setColor(
                            Color.YELLOW
                    );
                else
                    p.setColor(
                            Color.CYAN
                    );

                c.drawCircle(
                        d.x,
                        d.y,
                        14,
                        p
                );

                p.setColor(
                        Color.DKGRAY
                );

                p.setTextSize(9);

                c.drawText(
                        "" + d.value,
                        d.x - 7,
                        d.y + 3,
                        p
                );
            }
        }

        void drawEnd(Canvas c) {

            p.setColor(
                    0xAA000000
            );

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
            p.setTextSize(40);

            c.drawText(
                    win
                            ? "CHIẾN THẮNG!"
                            : "THUA!",
                    getWidth() / 2f,
                    getHeight() / 2f - 20,
                    p
            );

            p.setTextSize(17);

            if (win) {

                c.drawText(
                        level < 3
                                ? "CHẠM ĐỂ SANG MÀN"
                                : "ĐÃ HOÀN THÀNH",
                        getWidth() / 2f,
                        getHeight() / 2f + 20,
                        p
                );

            } else {

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

        void restart() {

            if (win) {

                if (level == 1) {

                    level = 2;

                    /*
                     * THẮNG MÀN 1
                     * CHƯA MỞ CHOMPER.
                     *
                     * THẮNG MÀN 2 MỚI MỞ.
                     */

                } else if (level == 2) {

                    level = 3;

                    chomperUnlocked = true;

                } else {

                    level = 1;
                }
            }

            plants.clear();
            zombies.clear();
            bullets.clear();
            bombBullets.clear();
            sunDrops.clear();
            coinDrops.clear();

            selected = 0;

            sun = 500;

            spawned = 0;
            killed = 0;

            lose = false;
            win = false;

            lastTime =
                    System.currentTimeMillis();

            spawnTime = lastTime;

            sunTime = lastTime;
        }

        boolean occupied(
                int row,
                int col
        ) {

            for (Plant a :
                    plants) {

                if (a.row == row
                        && a.col == col) {

                    return true;
                }
            }

            return false;
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

            /*
             * MÀN KẾT THÚC
             */

            if (lose || win) {

                restart();

                invalidate();

                return true;
            }

            /*
             * MENU CÂY
             */

            float menuW =
                    getWidth() / 4f;

            if (y >= 100 &&
                    y <= 150) {

                if (x < menuW) {

                    selected = 1;

                } else if (x < menuW * 2) {

                    selected = 2;

                } else if (x < menuW * 3) {

                    selected = 3;

                } else if (
                        chomperUnlocked
                        && x < menuW * 4
                ) {

                    selected = 4;
                }

                invalidate();

                return true;
            }

            /*
             * KỸ NĂNG
             */

            if (y >= 150 &&
                    y <= 195) {

                if (x < menuW) {

                    useThunder();

                } else if (x < menuW * 2) {

                    useFreeze();

                } else if (x < menuW * 3) {

                    useFire();

                } else {

                    usePlantFood();
                }

                invalidate();

                return true;
            }

            /*
             * NHẶT SUN
             */

            Iterator<SunDrop> si =
                    sunDrops.iterator();

            while (si.hasNext()) {

                SunDrop s = si.next();

                if (Math.abs(
                        x - s.x
                ) < 35 &&
                        Math.abs(
                                y - s.y
                        ) < 35) {

                    sun += 50;

                    si.remove();

                    invalidate();

                    return true;
                }
            }

            /*
             * NHẶT XU
             */

            Iterator<CoinDrop> ci =
                    coinDrops.iterator();

            while (ci.hasNext()) {

                CoinDrop d = ci.next();

                if (Math.abs(
                        x - d.x
                ) < 35 &&
                        Math.abs(
                                y - d.y
                        ) < 35) {

                    coins += d.value;

           
