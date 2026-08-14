package com.alpvz.gardendefense;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.content.*;
import java.util.*;

public class MainActivity extends Activity {

    GameView game;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        game = new GameView(this);
        setContentView(game);
    }

    class GameView extends View {

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        Bitmap sunflower;
        Bitmap peashooter;
        Bitmap giganut;

        int selected = 0;

        ArrayList<Plant> plants = new ArrayList<>();
        ArrayList<Zombie> zombies = new ArrayList<>();
        ArrayList<Pea> peas = new ArrayList<>();

        long lastSpawn = 0;

        int rows = 4;
        int cols = 7;

        int gridLeft = 30;
        int gridTop = 220;
        int cellW = 130;
        int cellH = 130;

        public GameView(Context c) {
            super(c);

            sunflower = loadImage("sunflower");
            peashooter = loadImage("peashooter");
            giganut = loadImage("giganut");

            setBackgroundColor(Color.rgb(91, 170, 65));
        }

        Bitmap loadImage(String name) {
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

            drawGame(c);
            updateGame();

            postInvalidateDelayed(30);
        }

        void drawGame(Canvas c) {

            // Tiêu đề
            p.setColor(Color.WHITE);
            p.setTextSize(32);
            p.setTypeface(Typeface.DEFAULT_BOLD);
            c.drawText("GARDEN DEFENSE", 30, 50, p);

            // Thanh chọn cây
            drawCard(c, sunflower, "SUNFLOWER", 20);
            drawCard(c, peashooter, "PEASHOOTER", 210);
            drawCard(c, giganut, "GIGANUT", 400);

            // Sân
            p.setColor(Color.rgb(120, 190, 80));

            for (int r = 0; r < rows; r++) {
                for (int col = 0; col < cols; col++) {

                    float l = gridLeft + col * cellW;
                    float t = gridTop + r * cellH;

                    c.drawRect(
                            l,
                            t,
                            l + cellW - 3,
                            t + cellH - 3,
                            p
                    );

                    p.setStyle(Paint.Style.STROKE);
                    p.setColor(Color.rgb(80, 140, 55));
                    c.drawRect(
                            l,
                            t,
                            l + cellW - 3,
                            t + cellH - 3,
                            p
                    );
                    p.setStyle(Paint.Style.FILL);
                    p.setColor(Color.rgb(120, 190, 80));
                }
            }

            // Cây
            for (Plant plant : plants) {
                Bitmap b = plant.image;

                if (b != null) {
                    Rect dst = new Rect(
                            plant.x + 15,
                            plant.y + 10,
                            plant.x + cellW - 15,
                            plant.y + cellH - 10
                    );

                    c.drawBitmap(b, null, dst, p);
                }

                // máu giganut
                if (plant.type == 3) {
                    drawHealth(
                            c,
                            plant.x + 15,
                            plant.y + 5,
                            cellW - 30,
                            plant.hp,
                            300
                    );
                }
            }

            // Zombie
            for (Zombie z : zombies) {
                drawZombie(c, z);

                drawHealth(
                        c,
                        z.x,
                        z.y - 12,
                        70,
                        z.hp,
                        z.maxHp
                );
            }

            // Đạn
            p.setColor(Color.YELLOW);

            for (Pea pea : peas) {
                c.drawCircle(
                        pea.x,
                        pea.y,
                        10,
                        p
                );
            }

            // Chỉ dẫn
            p.setColor(Color.WHITE);
            p.setTextSize(20);

            String text;

            if (selected == 1)
                text = "Đang chọn SUNFLOWER";
            else if (selected == 2)
                text = "Đang chọn PEASHOOTER";
            else if (selected == 3)
                text = "Đang chọn GIGANUT";
            else
                text = "Chọn cây rồi chạm vào ô sân";

            c.drawText(text, 20, getHeight() - 25, p);
        }

        void drawCard(Canvas c, Bitmap image, String name, int x) {

            p.setColor(Color.rgb(220, 235, 205));

            c.drawRoundRect(
                    x,
                    70,
                    x + 170,
                    195,
                    20,
                    20,
                    p
            );

            if (image != null) {

                Rect dst = new Rect(
                        x + 35,
                        78,
                        x + 135,
                        165
                );

                c.drawBitmap(
                        image,
                        null,
                        dst,
                        p
                );
            }

            p.setColor(Color.rgb(35, 80, 35));
            p.setTextSize(15);
            p.setTypeface(Typeface.DEFAULT_BOLD);

            c.drawText(
                    name,
                    x + 20,
                    187,
                    p
            );

            if ((name.equals("SUNFLOWER") && selected == 1) ||
                (name.equals("PEASHOOTER") && selected == 2) ||
                (name.equals("GIGANUT") && selected == 3)) {

                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(5);
                p.setColor(Color.YELLOW);

                c.drawRoundRect(
                        x,
                        70,
                        x + 170,
                        195,
                        20,
                        20,
                        p
                );

                p.setStyle(Paint.Style.FILL);
            }
        }

        void drawZombie(Canvas c, Zombie z) {

            p.setColor(Color.rgb(105, 120, 105));

            c.drawCircle(
                    z.x + 35,
                    z.y + 25,
                    25,
                    p
            );

            c.drawRect(
                    z.x + 15,
                    z.y + 45,
                    z.x + 55,
                    z.y + 105,
                    p
            );

            p.setColor(Color.BLACK);

            c.drawCircle(
                    z.x + 27,
                    z.y + 20,
                    4,
                    p
            );

            c.drawCircle(
                    z.x + 43,
                    z.y + 20,
                    4,
                    p
            );

            c.drawLine(
                    z.x + 20,
                    z.y + 35,
                    z.x + 47,
                    z.y + 35,
                    p
            );
        }

        void drawHealth(
                Canvas c,
                int x,
                int y,
                int width,
                int hp,
                int max
        ) {

            p.setColor(Color.RED);

            c.drawRect(
                    x,
                    y,
                    x + width,
                    y + 7,
                    p
            );

            p.setColor(Color.GREEN);

            float percent = Math.max(
                    0,
                    Math.min(1f, (float) hp / max)
            );

            c.drawRect(
                    x,
                    y,
                    x + width * percent,
                    y + 7,
                    p
            );
        }

        void updateGame() {

            long now = System.currentTimeMillis();

            // sinh zombie
            if (now - lastSpawn > 5000) {

                zombies.add(
                        new Zombie(
                                gridLeft + cols * cellW - 100,
                                gridTop + 40,
                                150
                        )
                );

                lastSpawn = now;
            }

            // Peashooter tự bắn
            for (Plant plant : plants) {

                if (plant.type == 2) {

                    if (now - plant.lastShot > 1200) {

                        peas.add(
                                new Pea(
                                        plant.x + cellW - 10,
                                        plant.y + cellH / 2
                                )
                        );

                        plant.lastShot = now;
                    }
                }
            }

            // Đạn bay
            Iterator<Pea> peaIterator = peas.iterator();

            while (peaIterator.hasNext()) {

                Pea pea = peaIterator.next();

                pea.x += 10;

                boolean hit = false;

                Iterator<Zombie> zombieIterator =
                        zombies.iterator();

                while (zombieIterator.hasNext()) {

                    Zombie z = zombieIterator.next();

                    if (Math.abs(pea.x - z.x) < 30 &&
                        Math.abs(pea.y - (z.y + 50)) < 50) {

                        z.hp -= 25;
                        hit = true;

                        if (z.hp <= 0) {
                            zombieIterator.remove();
                        }

                        break;
                    }
                }

                if (hit || pea.x > getWidth()) {
                    peaIterator.remove();
                }
            }

            // Zombie tiến về bên trái
            for (Zombie z : zombies) {

                boolean blocked = false;

                for (Plant plant : plants) {

                    if (plant.type == 3) {

                        if (z.x < plant.x + cellW &&
                            z.x + 55 > plant.x &&
                            z.y < plant.y + cellH &&
                            z.y + 100 > plant.y) {

                            blocked = true;

                            // Zombie đánh Giganut
                            if (now - z.lastAttack > 800) {

                                plant.hp -= 10;
                                z.lastAttack = now;

                                if (plant.hp <= 0) {
                                    plants.remove(plant);
                                }
                            }

                            break;
                        }
                    }
                }

                if (!blocked) {
                    z.x -= 1;
                }
            }
        }

        @Override
        public boolean onTouchEvent(
                android.view.MotionEvent event
        ) {

            if (event.getAction() != MotionEvent.ACTION_DOWN)
                return true;

            float x = event.getX();
            float y = event.getY();

            // Chọn Sunflower
            if (x >= 20 && x <= 190 &&
                y >= 70 && y <= 195) {

                selected = 1;
                return true;
            }

            // Chọn Peashooter
            if (x >= 210 && x <= 380 &&
                y >= 70 && y <= 195) {

                selected = 2;
                return true;
            }

            // Chọn Giganut
            if (x >= 400 && x <= 570 &&
                y >= 70 && y <= 195) {

                selected = 3;
                return true;
            }

            // Đặt cây
            if (selected != 0 &&
                y >= gridTop) {

                int col =
                        (int)((x - gridLeft) / cellW);

                int row =
                        (int)((y - gridTop) / cellH);

                if (row >= 0 &&
                    row < rows &&
                    col >= 0 &&
                    col < cols) {

                    int px =
                            gridLeft + col * cellW;

                    int py =
                            gridTop + row * cellH;

                    // Không cho đặt chồng cây
                    for (Plant plant : plants) {

                        if (plant.x == px &&
                            plant.y == py) {

                            return true;
                        }
                    }

                    Bitmap img = null;

                    if (selected == 1)
                        img = sunflower;

                    if (selected == 2)
                        img = peashooter;

                    if (selected == 3)
                        img = giganut;

                    plants.add(
                            new Plant(
                                    selected,
                                    px,
                                    py,
                                    img
                            )
                    );

                    selected = 0;
                }
            }

            return true;
        }
    }

    class Plant {

        int type;
        int x;
        int y;

        int hp = 300;

        long lastShot = 0;

        Bitmap image;

        Plant(
                int type,
                int x,
                int y,
                Bitmap image
        ) {

            this.type = type;
            this.x = x;
            this.y = y;
            this.image = image;

            if (type == 3)
                hp = 300;
        }
    }

    class Zombie {

        int x;
        int y;

        int hp;
        int maxHp;

        long lastAttack = 0;

        Zombie(int x, int y, int hp) {

            this.x = x;
            this.y = y;

            this.hp = hp;
            this.maxHp = hp;
        }
    }

    class Pea {

        float x;
        float y;

        Pea(float x, float y) {

            this.x = x;
            this.y = y;
        }
    }
        }
