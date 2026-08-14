package com.alpvz.gardendefense;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.*;
import android.view.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new GameView());
    }

    class GameView extends View {

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        Random random = new Random();

        Bitmap sunImg, peaImg, gigaImg, bulletImg, zombieImg;

        ArrayList<Plant> plants = new ArrayList<>();
        ArrayList<Zombie> zombies = new ArrayList<>();
        ArrayList<Bullet> bullets = new ArrayList<>();

        int selected = 0;
        int sun = 500;

        int rows = 5;
        int cols = 9;

        float left;
        float top;
        float cellW;
        float cellH;

        long lastSpawn = 0;
        long lastUpdate = 0;

        GameView() {
            super(MainActivity.this);

            sunImg = load("sun");
            peaImg = load("peashoot");
            gigaImg = load("giganut");
            bulletImg = load("gigapea");
            zombieImg = load("zomplatz");

            lastUpdate = System.currentTimeMillis();
            lastSpawn = lastUpdate;
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
            super.onDraw(c);

            left = 20;
            top = 180;

            cellW = (getWidth() - 40f) / cols;
            cellH = (getHeight() - top - 20f) / rows;

            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.rgb(95, 175, 70));
            c.drawRect(0, 0, getWidth(), getHeight(), p);

            drawTop(c);
            drawBoard(c);
            drawPlants(c);
            drawBullets(c);
            drawZombies(c);

            updateGame();

            postInvalidateDelayed(30);
        }

        void drawTop(Canvas c) {

            p.setColor(Color.rgb(55, 120, 50));
            c.drawRect(0, 0, getWidth(), 160, p);

            p.setColor(Color.WHITE);
            p.setTextSize(24);
            c.drawText("SUN: " + sun, 20, 35, p);

            drawButton(c, 20, 60, "SUN", 1, sunImg);
            drawButton(c, 155, 60, "PEA", 2, peaImg);
            drawButton(c, 290, 60, "GIGA", 3, gigaImg);
        }

        void drawButton(
                Canvas c,
                float x,
                float y,
                String name,
                int type,
                Bitmap img
        ) {
            p.setColor(
                    selected == type
                            ? Color.YELLOW
                            : Color.WHITE
            );

            c.drawRoundRect(
                    x,
                    y,
                    x + 120,
                    y + 90,
                    12,
                    12,
                    p
            );

            if (img != null) {
                c.drawBitmap(
                        img,
                        null,
                        new RectF(
                                x + 5,
                                y + 5,
                                x + 60,
                                y + 80
                        ),
                        p
                );
            }

            p.setColor(Color.DKGRAY);
            p.setTextSize(14);
            c.drawText(name, x + 68, y + 48, p);
        }

        void drawBoard(Canvas c) {

            for (int r = 0; r < rows; r++) {

                for (int col = 0; col < cols; col++) {

                    float x = left + col * cellW;
                    float y = top + r * cellH;

                    p.setColor(
                            (r + col) % 2 == 0
                                    ? Color.rgb(115, 190, 75)
                                    : Color.rgb(105, 180, 68)
                    );

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

            for (Plant plant : plants) {

                Bitmap img = null;

                if (plant.type == 1) img = sunImg;
                if (plant.type == 2) img = peaImg;
                if (plant.type == 3) img = gigaImg;

                float x = left + plant.col * cellW;
                float y = top + plant.row * cellH;

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
                } else {

                    if (plant.type == 1) {
                        p.setColor(Color.YELLOW);
                    } else if (plant.type == 2) {
                        p.setColor(Color.GREEN);
                    } else {
                        p.setColor(Color.rgb(230, 190, 30));
                    }

                    c.drawCircle(
                            x + cellW / 2,
                            y + cellH / 2,
                            28,
                            p
                    );
                }

                drawHealth(
                        c,
                        x + 8,
                        y + 5,
                        cellW - 16,
                        plant.hp,
                        plant.maxHp
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
                                    b.x - 12,
                                    b.y - 12,
                                    b.x + 12,
                                    b.y + 12
                            ),
                            p
                    );

                } else {

                    p.setColor(Color.GREEN);

                    c.drawCircle(
                            b.x,
                            b.y,
                            8,
                            p
                    );
                }
            }
        }

        void drawZombies(Canvas c) {

            for (Zombie z : zombies) {

                if (zombieImg != null) {

                    c.drawBitmap(
                            zombieImg,
                            null,
                            new RectF(
                                    z.x - 30,
                                    z.y - 45,
                                    z.x + 30,
                                    z.y + 45
                            ),
                            p
                    );

                } else {

                    p.setColor(Color.GRAY);

                    c.drawRect(
                            z.x - 25,
                            z.y - 40,
                            z.x + 25,
                            z.y + 40,
                            p
                    );
                }

                drawHealth(
                        c,
                        z.x - 30,
                        z.y - 55,
                        60,
                        z.hp,
                        z.maxHp
                );
            }
        }

        void drawHealth(
                Canvas c,
                float x,
                float y,
                float width,
                int hp,
                int maxHp
        ) {

            p.setColor(Color.RED);

            c.drawRect(
                    x,
                    y,
                    x + width,
                    y + 6,
                    p
            );

            p.setColor(Color.GREEN);

            float percent =
                    Math.max(
                            0f,
                            Math.min(
                                    1f,
                                    hp / (float) maxHp
                            )
                    );

            c.drawRect(
                    x,
                    y,
                    x + width * percent,
                    y + 6,
                    p
            );
        }

        void updateGame() {

            long now = System.currentTimeMillis();

            float dt =
                    (now - lastUpdate) / 1000f;

            if (dt > 0.1f) {
                dt = 0.1f;
            }

            lastUpdate = now;

            if (now - lastSpawn > 4000) {
                spawnZombie();
                lastSpawn = now;
            }

            for (Plant plant : plants) {

                plant.timer += dt;

                if (plant.type == 1 &&
                        plant.timer >= 5f) {

                    sun += 25;
                    plant.timer = 0;
                }

                if (plant.type == 2 &&
                        plant.timer >= 1.2f) {

                    if (hasZombieInRow(plant.row)) {

                        bullets.add(
                                new Bullet(
                                        left +
                                                plant.col * cellW +
                                                cellW - 10,
                                        top +
                                                plant.row * cellH +
                                                cellH / 2,
                                        plant.row
                                )
                        );

                        plant.timer = 0;
                    }
                }
            }

            updateBullets();
            updateZombies();
            removeDead();
        }

        boolean hasZombieInRow(int row) {

            for (Zombie z : zombies) {

                if (z.row == row) {
                    return true;
                }
            }

            return false;
        }

        void updateBullets() {

            Iterator<Bullet> bi =
                    bullets.iterator();

            while (bi.hasNext()) {

                Bullet b = bi.next();

                b.x += 8;

                boolean hit = false;

                for (Zombie z : zombies) {

                    if (z.row == b.row &&
                            Math.abs(z.x - b.x) < 30) {

                        z.hp -= 25;
                        hit = true;
                        break;
                    }
                }

                if (hit ||
                        b.x > getWidth() + 30) {

                    bi.remove();
                }
            }
        }

        void updateZombies() {

            for (Zombie z : zombies) {

                Plant target = findPlant(z);

                if (target != null) {

                    long now =
                            System.currentTimeMillis();

                    if (now - target.lastHit > 700) {

                        target.hp -= 15;
                        target.lastHit = now;
                    }

                } else {

                    z.x -= 1.2f;
                }
            }
        }

        Plant findPlant(Zombie z) {

            for (Plant plant : plants) {

                if (plant.row != z.row) {
                    continue;
                }

                float px =
                        left +
                                plant.col * cellW +
                                cellW / 2;

                if (Math.abs(z.x - px) < 55) {
                    return plant;
                }
            }

            return null;
        }

        void removeDead() {

            Iterator<Plant> pi =
                    plants.iterator();

            while (pi.hasNext()) {

                Plant plant = pi.next();

                if (plant.hp <= 0) {
                    pi.remove();
                }
            }

            Iterator<Zombie> zi =
                    zombies.iterator();

            while (zi.hasNext()) {

                Zombie z = zi.next();

                if (z.hp <= 0) {
                    zi.remove();
                    sun += 25;
                }
            }

            Iterator<Bullet> bi =
                    bullets.iterator();

            while (bi.hasNext()) {

                Bullet b = bi.next();

                if (b.x > getWidth() + 50) {
                    bi.remove();
                }
            }
        }

        void spawnZombie() {

            int row =
                    random.nextInt(rows);

            zombies.add(
                    new Zombie(
                            getWidth() + 40,
                            top +
                                    row * cellH +
                                    cellH / 2,
                            row
                    )
            );
        }

        @Override
        public boolean onTouchEvent(
                MotionEvent event
        ) {

            if (event.getAction() !=
                    MotionEvent.ACTION_DOWN) {

                return true;
            }

            float x = event.getX();
            float y = event.getY();

            /*
             * Chọn cây.
             * Vùng bấm rộng hơn hình nút để cả 3 cây
             * đều chọn được ổn định.
             */

            if (y >= 50 && y <= 160) {

                if (x >= 10 && x < 145) {
                    selected = 1;
                    return true;
                }

                if (x >= 145 && x < 285) {
                    selected = 2;
                    return true;
                }

                if (x >= 285 && x < 440) {
                    selected = 3;
                    return true;
                }
            }

            /*
             * Đặt cây.
             * Tất cả 3 loại đều chỉ chiếm đúng 1 ô.
             */

            if (selected != 0 &&
                    x >= left &&
                    x < left + cols * cellW &&
                    y >= top &&
                    y < top + rows * cellH) {

                int col =
                        (int)((x - left) / cellW);

                int row =
                        (int)((y - top) / cellH);

                if (row >= 0 &&
                        row < rows &&
                        col >= 0 &&
                        col < cols) {

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

                            int hp;

                            if (selected == 3) {
                                hp = 1500;
                            } else if (selected == 1) {
                                hp = 300;
                            } else {
                                hp = 400;
                            }

                            plants.add(
                                    new Plant(
                                            selected,
                                            row,
                                            col,
                                            hp
                                    )
                            );

                            sun -= cost;
                            selected = 0;
                        }
                    }
                }

                return true;
            }

            return true;
        }

        boolean occupied(
                int row,
                int col
        ) {

            for (Plant plant : plants) {

                if (plant.row == row &&
                        plant.col == col) {

                    return true;
                }
            }

            return false;
        }
    }

    class Plant {

        int type;
        int row;
        int col;

        int hp;
        int maxHp;

        float timer = 0;
        long lastHit = 0;

        Plant(
                int type,
                int row,
                int col,
                int hp
        ) {

            this.type = type;
            this.row = row;
            this.col = col;

            this.hp = hp;
            this.maxHp = hp;
        }
    }

    class Zombie {

        float x;
        float y;

        int row;

        int hp = 200;
        int maxHp = 200;

        Zombie(
                float x,
                float y,
                int row
        ) {

            this.x = x;
            this.y = y;
            this.row = row;
        }
    }

    class Bullet {

        float x;
        float y;

        int row;

        Bullet(
                float x,
                float y,
                int row
        ) {

            this.x = x;
            this.y = y;
            this.row = row;
        }
    }
        }
