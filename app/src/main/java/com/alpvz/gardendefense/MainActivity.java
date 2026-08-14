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

        Bitmap sunImg, peaImg, gigaImg;
        Bitmap bulletImg, zombieImg;
        Bitmap chomperImg, bomberImg;

        ArrayList<Plant> plants = new ArrayList<>();
        ArrayList<Zombie> zombies = new ArrayList<>();
        ArrayList<Bullet> bullets = new ArrayList<>();
        ArrayList<BombBullet> bombBullets = new ArrayList<>();

        int selected = 0;
        int sun = 500;
        int spawned = 0;
        int killed = 0;
        int level = 1;

        boolean chomperUnlocked = false;
        boolean lose = false;
        boolean win = false;

        final int ROWS = 5;
        final int COLS = 9;

        float left = 20;
        float top = 185;
        float cellW, cellH;

        long lastTime;
        long spawnTime;

        GameView() {
            super(MainActivity.this);

            sunImg = load("sun");
            peaImg = load("peashoot");
            gigaImg = load("giganut");
            bulletImg = load("gigapea");
            zombieImg = load("zomplatz");

            chomperImg = load("chomper");
            bomberImg = load("zomvinhhung");

            lastTime = System.currentTimeMillis();
            spawnTime = lastTime;
        }

        Bitmap load(String name) {
            int id = getResources().getIdentifier(
                    name,
                    "drawable",
                    getPackageName()
            );

            if (id == 0) return null;

            return BitmapFactory.decodeResource(
                    getResources(),
                    id
            );
        }

        @Override
        protected void onDraw(Canvas c) {

            cellW = (getWidth() - 40f) / COLS;
            cellH = (getHeight() - top - 20f) / ROWS;

            p.setColor(Color.rgb(95,175,70));
            c.drawRect(
                    0,0,
                    getWidth(),
                    getHeight(),
                    p
            );

            drawTop(c);
            drawBoard(c);
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

            p.setColor(Color.rgb(55,120,50));
            c.drawRect(
                    0,0,
                    getWidth(),
                    165,
                    p
            );

            p.setColor(Color.WHITE);
            p.setTextSize(22);

            c.drawText(
                    "MÀN " + level +
                    "   SUN: " + sun,
                    15,32,p
            );

            card(c,10,55,1,"SUN",sunImg);
            card(c,140,55,2,"PEA",peaImg);
            card(c,270,55,3,"GIGA",gigaImg);

            if (chomperUnlocked) {
                card(
                        c,400,55,
                        4,"CHOMPER",
                        chomperImg
                );
            }
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
                            x,y,
                            x+120,y+90
                    ),
                    12,12,p
            );

            if (img != null) {
                c.drawBitmap(
                        img,
                        null,
                        new RectF(
                                x+5,y+5,
                                x+60,y+80
                        ),
                        p
                );
            }

            p.setColor(Color.DKGRAY);
            p.setTextSize(
                    name.equals("CHOMPER")
                            ? 10 : 14
            );

            c.drawText(
                    name,
                    x+65,
                    y+50,
                    p
            );
        }

        void drawBoard(Canvas c) {

            for (int row=0;row<ROWS;row++) {
                for (int col=0;col<COLS;col++) {

                    p.setColor(
                            (row+col)%2==0
                                    ? Color.rgb(115,190,75)
                                    : Color.rgb(105,180,68)
                    );

                    float x =
                            left+col*cellW;

                    float y =
                            top+row*cellH;

                    c.drawRect(
                            x,y,
                            x+cellW-2,
                            y+cellH-2,
                            p
                    );
                }
            }
        }

        void drawPlants(Canvas c) {

            for (Plant a:plants) {

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
                        left+a.col*cellW;

                float y =
                        top+a.row*cellH;

                if (img != null) {

                    c.drawBitmap(
                            img,
                            null,
                            new RectF(
                                    x+5,y+5,
                                    x+cellW-5,
                                    y+cellH-5
                            ),
                            p
                    );
                }

                drawHP(
                        c,
                        x+cellW*.3f,
                        y+4,
                        cellW*.4f,
                        a.hp,
                        a.max
                );
            }
        }

        void drawBullets(Canvas c) {

            for (Bullet b:bullets) {

                if (bulletImg != null) {

                    c.drawBitmap(
                            bulletImg,
                            null,
                            new RectF(
                                    b.x-21,b.y-21,
                                    b.x+21,b.y+21
                            ),
                            p
                    );
                } else {

                    p.setColor(Color.GREEN);

                    c.drawCircle(
                            b.x,b.y,18,p
                    );
                }
            }
        }

        void drawBombBullets(Canvas c) {

            for (BombBullet b:bombBullets) {

                // ĐẠN ZOMVINHHUNG MÀU ĐỎ/CAM
                p.setColor(Color.rgb(255,70,20));

                c.drawCircle(
                        b.x,
                        b.y,
                        13,
                        p
                );

                p.setColor(Color.YELLOW);

                c.drawCircle(
                        b.x-4,
                        b.y-4,
                        4,
                        p
                );
            }
        }

        void drawZombies(Canvas c) {

            for (Zombie z:zombies) {

                float w;
                float h;
                Bitmap img;

                if (z.bomber) {

                    // NHỎ HƠN ĐỂ ĐỠ LAG
                    w = 65;
                    h = 85;
                    img = bomberImg;

                } else if (z.big) {

                    w = 110;
                    h = 150;
                    img = zombieImg;

                } else {

                    w = 82;
                    h = 112;
                    img = zombieImg;
                }

                if (img != null) {

                    c.drawBitmap(
                            img,
                            null,
                            new RectF(
                                    z.x-w/2,
                                    z.y-h/2,
                                    z.x+w/2,
                                    z.y+h/2
                            ),
                            p
                    );
                }

                drawHP(
                        c,
                        z.x-30,
                        z.y-(z.big?80:55),
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
                    x,y,
                    x+w,y+5,
                    p
            );

            p.setColor(Color.GREEN);

            float q = Math.max(
                    0,
                    Math.min(
                            1,
                            hp/(float)max
                    )
            );

            c.drawRect(
                    x,y,
                    x+w*q,y+5,
                    p
            );
        }

        void updateGame() {

            long now =
                    System.currentTimeMillis();

            float dt =
                    (now-lastTime)/1000f;

            if (dt > 0.1f)
                dt = 0.1f;

            lastTime = now;

            int total =
                    level == 1 ? 10 :
                    level == 2 ? 15 :
                    22;

            int delay =
                    level == 1 ? 3500 :
                    level == 2 ? 2800 :
                    2200;

            if (
                    spawned < total &&
                    now-spawnTime >= delay
            ) {

                spawnZombie();
                spawnTime = now;
            }

            for (Plant a:plants) {

                a.timer += dt;

                if (
                        a.type == 1 &&
                        a.timer >= 5
                ) {

                    sun += 100;
                    a.timer = 0;
                }

                if (
                        a.type == 2 &&
                        a.timer >= 1.2f &&
                        rowHasZombie(a.row)
                ) {

                    bullets.add(
                            new Bullet(
                                    left+a.col*cellW+
                                            cellW-10,
                                    top+a.row*cellH+
                                            cellH/2,
                                    a.row
                            )
                    );

                    a.timer = 0;
                }

                if (
                        a.type == 4 &&
                        a.timer >= 2
                ) {

                    Zombie target =
                            chomperTarget(a);

                    if (target != null) {

                        target.hp = 0;
                        a.timer = 0;
                    }
                }
            }

            for (Zombie z:zombies) {

                if (z.big) {

                    z.damageTimer += dt;

                    if (z.damageTimer >= 1) {

                        for (Plant a:plants)
                            a.hp -= 10;

                        z.damageTimer = 0;
                    }
                }
            }

            updateBullets();
            updateBombBullets();
            updateZombies();

            clean();

            if (
                    spawned >= total &&
                    zombies.isEmpty()
            ) {

                win = true;
            }
        }

        boolean rowHasZombie(int row) {

            for (Zombie z:zombies) {

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

                for (Zombie z:zombies) {

                    if (
                            z.row == b.row &&
                            Math.abs(z.x-b.x)<35
                    ) {

                        z.hp -= 25;
                        hit = true;
                        break;
                    }
                }

                if (
                        hit ||
                        b.x > getWidth()+60
                ) {

                    it.remove();
                }
            }
                }        void updateBombBullets() {

            Iterator<BombBullet> it =
                    bombBullets.iterator();

            while (it.hasNext()) {

                BombBullet b = it.next();

                b.x += b.speed;

                if (b.x >= b.targetX) {

                    bombAttackAt(
                            b.targetX,
                            b.targetY
                    );

                    it.remove();
                }
            }
        }

        void bombAttackAt(
                float x,
                float y
        ) {

            int centerCol = 1;

            for (Plant a:plants) {

                if (
                        Math.abs(
                                a.col-centerCol
                        ) <= 1 &&
                        Math.abs(
                                a.row-
                                (int)((y-top)/cellH)
                        ) <= 1
                ) {

                    a.hp -= 200;
                }
            }
        }

        void updateZombies() {

            for (Zombie z:zombies) {

                if (z.x < -70) {

                    lose = true;
                    return;
                }

                if (z.bomber) {

                    float stopX =
                            left+cellW*1.5f;

                    if (!z.stopped) {

                        if (z.x > stopX) {

                            z.x -= 0.6f;

                        } else {

                            z.x = stopX;
                            z.stopped = true;
                        }
                    }

                    if (z.stopped) {

                        z.throwTimer += 30;

                        if (
                                z.throwTimer >= 8000
                        ) {

                            throwBomb(z);
                            z.throwTimer = 0;
                        }
                    }

                    continue;
                }

                Plant target =
                        findPlant(z);

                if (target != null) {

                    long now =
                            System.currentTimeMillis();

                    if (
                            now-target.lastBite >= 500
                    ) {

                        target.hp -= 100;
                        target.lastBite = now;
                    }

                } else {

                    z.x -=
                            z.big
                                    ? 0.55f
                                    : 1.0f;
                }
            }
        }

        void throwBomb(Zombie z) {

            float targetX =
                    left+cellW*1.5f;

            float targetY =
                    top+
                    z.row*cellH+
                    cellH/2;

            bombBullets.add(
                    new BombBullet(
                            z.x,
                            z.y,
                            targetX,
                            targetY
                    )
            );
        }

        Plant findPlant(Zombie z) {

            for (Plant a:plants) {

                if (a.row != z.row)
                    continue;

                float px =
                        left+
                        a.col*cellW+
                        cellW/2;

                if (
                        Math.abs(z.x-px) <
                        (z.big?70:55)
                ) {

                    return a;
                }
            }

            return null;
        }

        Zombie chomperTarget(Plant a) {

            for (Zombie z:zombies) {

                if (z.row != a.row)
                    continue;

                float px =
                        left+
                        a.col*cellW+
                        cellW/2;

                if (
                        z.x > px-20 &&
                        z.x < px+cellW*1.5f
                ) {

                    return z;
                }
            }

            return null;
        }

        void spawnZombie() {

            int row =
                    r.nextInt(ROWS);

            boolean bomber = false;
            boolean big = false;

            if (level == 1) {

                bomber = false;
                big = false;

            } else if (level == 2) {

                if (
                        spawned > 0 &&
                        spawned % 5 == 0
                ) {

                    big = true;
                }

            } else {

                if (
                        spawned > 0 &&
                        spawned % 4 == 0
                ) {

                    bomber = true;

                } else if (
                        spawned % 5 == 0
                ) {

                    big = true;
                }
            }

            zombies.add(
                    new Zombie(
                            getWidth()+70,
                            top+
                            row*cellH+
                            cellH/2,
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

                if (
                        pi.next().hp <= 0
                ) {

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
        }

        boolean occupied(
                int row,
                int col
        ) {

            for (Plant a:plants) {

                if (
                        a.row == row &&
                        a.col == col
                ) {

                    return true;
                }
            }

            return false;
        }

        void drawEnd(Canvas c) {

            p.setColor(0xAA000000);

            c.drawRect(
                    0,0,
                    getWidth(),
                    getHeight(),
                    p
            );

            p.setTextAlign(
                    Paint.Align.CENTER
            );

            p.setColor(Color.WHITE);
            p.setTextSize(38);

            if (win) {

                if (
                        level == 2 &&
                        !chomperUnlocked
                ) {

                    chomperUnlocked = true;

                    c.drawText(
                            "MỞ KHÓA CHOMPER!",
                            getWidth()/2f,
                            getHeight()/2f-25,
                            p
                    );

                } else {

                    c.drawText(
                            "THẮNG MÀN "+level,
                            getWidth()/2f,
                            getHeight()/2f-25,
                            p
                    );
                }

            } else {

                c.drawText(
                        "THUA!",
                        getWidth()/2f,
                        getHeight()/2f-25,
                        p
                );
            }

            p.setTextSize(18);

            c.drawText(
                    win && level < 3
                            ? "CHẠM ĐỂ SANG MÀN TIẾP"
                            : "CHẠM ĐỂ CHƠI LẠI",
                    getWidth()/2f,
                    getHeight()/2f+30,
                    p
            );

            p.setTextAlign(
                    Paint.Align.LEFT
            );
        }

        void nextLevel() {

            plants.clear();
            zombies.clear();
            bullets.clear();
            bombBullets.clear();

            spawned = 0;
            killed = 0;
            selected = 0;

            lose = false;
            win = false;

            level++;

            lastTime =
                    System.currentTimeMillis();

            spawnTime = lastTime;
        }

        void restart() {

            level = 1;
            chomperUnlocked = false;
            sun = 500;

            plants.clear();
            zombies.clear();
            bullets.clear();
            bombBullets.clear();

            spawned = 0;
            killed = 0;
            selected = 0;

            lose = false;
            win = false;

            lastTime =
                    System.currentTimeMillis();

            spawnTime = lastTime;
        }

        @Override
        public boolean onTouchEvent(
                MotionEvent e
        ) {

            if (
                    e.getAction() !=
                    MotionEvent.ACTION_DOWN
            )
                return true;

            float x = e.getX();
            float y = e.getY();

            if (lose) {

                restart();
                invalidate();
                return true;
            }

            if (win) {

                if (level == 2)
                    chomperUnlocked = true;

                if (level < 3) {

                    nextLevel();

                } else {

                    restart();
                }

                invalidate();
                return true;
            }

            if (
                    y >= 45 &&
                    y <= 155
            ) {

                if (x < 140) {

                    selected = 1;

                } else if (x < 275) {

                    selected = 2;

                } else if (x < 410) {

                    selected = 3;

                } else if (
                        x < 550 &&
                        chomperUnlocked
                ) {

                    selected = 4;
                }

                invalidate();
                return true;
            }

            if (
                    selected != 0 &&
                    x >= left &&
                    x < left+COLS*cellW &&
                    y >= top &&
                    y < top+ROWS*cellH
            ) {

                int col =
                        (int)((x-left)/cellW);

                int row =
                        (int)((y-top)/cellH);

                if (!occupied(row,col)) {

                    int cost =
                            selected == 1 ? 50 :
                            selected == 2 ? 100 :
                            selected == 3 ? 150 :
                            150;

                    if (sun >= cost) {

                        int max =
                                selected == 1 ? 300 :
                                selected == 2 ? 400 :
                                selected == 3 ? 3000 :
                                800;

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
        }
    }

    class Zombie {

        float x;
        float y;

        float damageTimer;
        float throwTimer;

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

            if (bomber) {

                hp = 150;
                max = 150;

            } else if (big) {

                hp = 1000;
                max = 1000;

            } else {

                hp = 300;
                max = 300;
            }
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

        float targetX;
        float targetY;

        float speed = 5;

        BombBullet(
                float xx,
                float yy,
                float tx,
                float ty
        ) {

            x = xx;
            y = yy;
            targetX = tx;
            targetY = ty;
        }
    }
                    }
