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

        Bitmap sunImg;
        Bitmap peaImg;
        Bitmap gigaImg;
        Bitmap bulletImg;
        Bitmap zombieImg;
        Bitmap bigZombieImg;

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

        int totalZombies = 10;
        int spawnedZombies = 0;
        int killedZombies = 0;

        boolean gameOver = false;
        boolean victory = false;

        GameView() {
            super(MainActivity.this);

            sunImg = load("sun");
            peaImg = load("peashoot");
            gigaImg = load("giganut");
            bulletImg = load("gigapea");
            zombieImg = load("zomplatz");
            bigZombieImg = load("zomto");

            lastUpdate = System.currentTimeMillis();
            lastSpawn = lastUpdate;
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
            super.onDraw(c);

            left = 20;
            top = 180;

            cellW = (getWidth() - 40f) / cols;
            cellH = (getHeight() - top - 20f) / rows;

            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.rgb(95, 175, 70));

            c.drawRect(
                    0,
                    0,
                    getWidth(),
                    getHeight(),
                    p
            );

            drawTop(c);
            drawBoard(c);
            drawPlants(c);
            drawBullets(c);
            drawZombies(c);

            if (!gameOver && !victory) {
                updateGame();
                postInvalidateDelayed(30);
            }

            if (gameOver) {
                drawEndScreen(
                        c,
                        "THUA!",
                        "QUÁI ĐÃ VÀO NHÀ"
                );
            }

            if (victory) {
                drawEndScreen(
                        c,
                        "CHIẾN THẮNG!",
                        "ĐÃ HẠ GỤC TẤT CẢ QUÁI"
                );
            }
        }

        void drawTop(Canvas c) {

            p.setColor(Color.rgb(55, 120, 50));

            c.drawRect(
                    0,
                    0,
                    getWidth(),
                    160,
                    p
            );

            p.setColor(Color.WHITE);
            p.setTextSize(24);

            c.drawText(
                    "SUN: " + sun,
                    20,
                    32,
                    p
            );

            drawProgress(c);

            drawButton(
                    c,
                    20,
                    60,
                    "SUN",
                    1,
                    sunImg
            );

            drawButton(
                    c,
                    155,
                    60,
                    "PEA",
                    2,
                    peaImg
            );

            drawButton(
                    c,
                    290,
                    60,
                    "GIGA",
                    3,
                    gigaImg
            );
        }

        void drawProgress(Canvas c) {

            float x = 470;
            float y = 18;
            float width = getWidth() - 490;
            float height = 22;

            p.setColor(Color.DKGRAY);

            c.drawRoundRect(
                    x,
                    y,
                    x + width,
                    y + height,
                    10,
                    10,
                    p
            );

            float progress =
                    killedZombies / (float) totalZombies;

            if (progress > 1f) {
                progress = 1f;
            }

            p.setColor(Color.rgb(60, 220, 70));

            c.drawRoundRect(
                    x,
                    y,
                    x + width * progress,
                    y + height,
                    10,
                    10,
                    p
            );

            p.setColor(Color.WHITE);
            p.setTextSize(15);

            c.drawText(
                    killedZombies + "/" + totalZombies,
                    x + 8,
                    y + 17,
                    p
            );
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

            c.drawText(
                    name,
                    x + 68,
                    y + 48,
                    p
            );
        }

        void drawBoard(Canvas c) {

            for (int r = 0; r < rows; r++) {

                for (int col = 0; col < cols; col++) {

                    float x =
                            left + col * cellW;

                    float y =
                            top + r * cellH;

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

                if (plant.type == 1) {
                    img = sunImg;
                } else if (plant.type == 2) {
                    img = peaImg;
                } else if (plant.type == 3) {
                    img = gigaImg;
                }

                float x =
                        left + plant.col * cellW;

                float y =
                        top + plant.row * cellH;

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

                // Thanh máu cây ngắn hơn.
                drawHealth(
                        c,
                        x + cellW * 0.30f,
                        y + 4,
                        cellW * 0.40f,
                        plant.hp,
                        plant.maxHp
                );
            }
        }

        void drawBullets(Canvas c) {

            for (Bullet b : bullets) {

                if (bulletImg != null) {

                    // Gigapea lớn.
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
                            16,
                            p
                    );
                }
            }
        }

        void drawZombies(Canvas c) {

            for (Zombie z : zombies) {

                if (z.big) {
                    drawBigZombie(c, z);
                } else {
                    drawNormalZombie(c, z);
                }

                // Thanh máu zombie cũng ngắn hơn.
                drawHealth(
                        c,
                        z.x - (z.big ? 35 : 30),
                        z.y - (z.big ? 70 : 60),
                        z.big ? 70 : 60,
                        z.hp,
                        z.maxHp
                );
            }
        }

        void drawNormalZombie(
                Canvas c,
                Zombie z
        ) {

            if (zombieImg != null) {

                c.drawBitmap(
                        zombieImg,
                        null,
                        new RectF(
                                z.x - 38,
                                z.y - 55,
                                z.x + 38,
                                z.y + 55
                        ),
                        p
                );

            } else {

                p.setColor(Color.GRAY);

                c.drawRect(
                        z.x - 32,
                        z.y - 50,
                        z.x + 32,
                        z.y + 50,
                        p
                );
            }
        }

        void drawBigZombie(
                Canvas c,
                Zombie z
        ) {

            if (bigZombieImg != null) {

                c.drawBitmap(
                        bigZombieImg,
                        null,
                        new RectF(
                                z.x - 55,
                                z.y - 75,
                                z.x + 55,
                                z.y + 75
                        ),
                        p
                );

            } else {

                p.setColor(
                        Color.rgb(
                                70,
                                70,
                                75
                        )
                );

                c.drawRoundRect(
                        z.x - 50,
                        z.y - 70,
                        z.x + 50,
                        z.y + 70,
                        15,
                        15,
                        p
                );
            }

            if (z.jumping) {

                p.setColor(Color.WHITE);
                p.setTextSize(14);

                c.drawText(
                        "JUMP!",
                        z.x - 25,
                        z.y - 85,
                        p
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
                    y + 5,
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
                    y + 5,
                    p
            );
        }

        void drawEndScreen(
                Canvas c,
                String title,
                String subtitle
        ) {

            p.setColor(0xaa000000);

            c.drawRect(
                    0,
                    0,
                    getWidth(),
                    getHeight(),
                    p
            );

            p.setColor(Color.WHITE);
            p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(42);

            c.drawText(
                    title,
                    getWidth() / 2f,
                    getHeight() / 2f - 20,
                    p
            );

            p.setTextSize(20);

            c.drawText(
                    subtitle,
                    getWidth() / 2f,
                    getHeight() / 2f + 25,
                    p
            );

            p.setTextSize(16);

            c.drawText(
                    "Nhấn để chơi lại",
                    getWidth() / 2f,
                    getHeight() / 2f + 65,
                    p
            );

            p.setTextAlign(Paint.Align.LEFT);
        }

        void updateGame() {

            long now =
                    System.currentTimeMillis();

            float dt =
                    (now - lastUpdate)
                            / 1000f;

            if (dt > 0.1f) {
                dt = 0.1f;
            }

            lastUpdate = now;

            /*
             * Chỉ sinh zombie cho tới đủ 10 con.
             * Khi cả 10 con đều chết => thắng.
             */

            if (spawnedZombies < totalZombies &&
                    now - lastSpawn >= 4000) {

                spawnZombie();

                lastSpawn = now;
            }

            for (Plant plant : plants) {

                plant.timer += dt;

                // Sunflower: tăng 100 sun.
                if (plant.type == 1 &&
                        plant.timer >= 5f) {

                    sun += 100;
                    plant.timer = 0;
                }

                // Peashooter.
                if (plant.type == 2 &&
                        plant.timer >= 1.2f) {

                    if (hasZombieInRow(
                            plant.row
                    )) {

                        bullets.add(
                                new Bullet(
                                        left +
                                                plant.col *
                                                        cellW +
                                                cellW - 10,
                                        top +
                                                plant.row *
                                                        cellH +
                                                cellH / 2,
                                        plant.row
                                )
                        );

                        plant.timer = 0;
                    }
                }
            }

            /*
             * Zombie to gây 10 damage/giây
             * lên toàn bộ cây.
             */

            for (Zombie z : zombies) {

                if (z.big) {

                    z.bigDamageTimer += dt;

                    if (z.bigDamageTimer >= 1f) {

                        for (Plant plant : plants) {
                            plant.hp -= 10;
                        }

                        z.bigDamageTimer = 0;
                    }
                }
            }

            updateBullets();
            updateZombies();
            removeDead();

            /*
             * Thắng khi:
             * - đã sinh đủ 10 zombie
             * - không còn zombie nào sống
             */

            if (spawnedZombies >= totalZombies &&
                    zombies.isEmpty() &&
                    killedZombies >= totalZombies) {

                victory = true;
            }
        }

        boolean hasZombieInRow(
                int row
        ) {

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
                            Math.abs(
                                    z.x - b.x
                            ) < 35) {

                        z.hp -= 25;

                        hit = true;

                        break;
                    }
                }

                if (hit ||
                        b.x > getWidth() + 50) {

                    bi.remove();
                }
            }
        }

        void updateZombies() {

            for (Zombie z : zombies) {

                /*
                 * Zombie lọt sang mép trái = thua.
                 */

                if (z.x < -60) {
                    gameOver = true;
                    return;
                }

                Plant target =
                        findPlant(z);

                if (target != null) {

                    long now =
                            System.currentTimeMillis();

                    /*
                     * Zombie thường:
                     * 100 damage mỗi 0,5 giây.
                     */

                    if (!z.big) {

                        if (now -
                                target.lastHit >= 500) {

                            target.hp -= 100;

                            target.lastHit =
                                    now;
                        }

                    } else {

                        /*
                         * Zombie to:
                         * không cắn từng cây,
                         * gây 10 damage/giây toàn sân.
                         */

                        z.jumping = true;

                        z.jumpTimer += 30;

                        if (z.jumpTimer >= 1200) {

                            z.x -=
                                    cellW * 1.5f;

                            z.jumpTimer = 0;
                            z.jumping = false;
                        }
                    }

                } else {

                    /*
                     * Zombie thường nhanh.
                     * Zombie to chậm 2 lần.
                     */

                    float speed =
                            z.big
                                    ? 0.6f
                                    : 1.2f;

                    z.x -= speed;
    
