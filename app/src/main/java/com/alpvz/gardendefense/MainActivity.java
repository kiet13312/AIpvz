package com.alpvz.gardendefense;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.*;
import android.content.Context;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new GameView(this));
    }

    public static class GameView extends View {

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ArrayList<Plant> plants = new ArrayList<>();
        ArrayList<Zombie> zombies = new ArrayList<>();
        ArrayList<Pea> peas = new ArrayList<>();

        Bitmap sunflowerImg;
        Bitmap peashootImg;
        Bitmap giganutImg;

        Random random = new Random();

        int selectedPlant = 0;
        int money = 500;

        long lastTime;
        long zombieTimer = 0;
        long sunTimer = 0;

        int rows = 5;
        int cols = 9;

        float boardLeft;
        float boardTop;
        float cellW;
        float cellH;

        public GameView(Context context) {
            super(context);

            paint.setTypeface(Typeface.create("sans", Typeface.BOLD));

            sunflowerImg = loadImage("sunflower");
            peashootImg = loadImage("peashoot");
            giganutImg = loadImage("giganut");

            lastTime = System.currentTimeMillis();
        }

        Bitmap loadImage(String name) {
            int id = getResources().getIdentifier(
                    name,
                    "drawable",
                    getContext().getPackageName()
            );

            if (id == 0) {
                return null;
            }

            return BitmapFactory.decodeResource(getResources(), id);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            float w = getWidth();
            float h = getHeight();

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(102, 180, 70));
            canvas.drawRect(0, 0, w, h, paint);

            drawTopBar(canvas);

            boardLeft = 25;
            boardTop = 190;
            cellW = (w - 50) / cols;
            cellH = (h - boardTop - 40) / rows;

            drawBoard(canvas);

            long now = System.currentTimeMillis();
            float dt = (now - lastTime) / 1000f;

            if (dt > 0.1f) {
                dt = 0.1f;
            }

            lastTime = now;

            updateGame(dt);

            drawObjects(canvas);

            invalidate();
        }

        void drawTopBar(Canvas canvas) {
            paint.setColor(Color.rgb(72, 145, 45));
            canvas.drawRect(0, 0, getWidth(), 170, paint);

            paint.setColor(Color.WHITE);
            paint.setTextSize(30);
            canvas.drawText("GARDEN DEFENSE", 25, 45, paint);

            paint.setTextSize(18);
            canvas.drawText("SUN: " + money, 25, 75, paint);

            drawCard(canvas, 20, 90, 150, 155, "SUNFLOWER", 1);
            drawCard(canvas, 165, 90, 295, 155, "PEASHOOT", 2);
            drawCard(canvas, 310, 90, 440, 155, "GIGANUT", 3);
        }

        void drawCard(
                Canvas canvas,
                float left,
                float top,
                float right,
                float bottom,
                String name,
                int type
        ) {
            paint.setColor(selectedPlant == type
                    ? Color.rgb(255, 230, 120)
                    : Color.rgb(220, 235, 205));

            canvas.drawRoundRect(
                    left,
                    top,
                    right,
                    bottom,
                    15,
                    15,
                    paint
            );

            Bitmap img = null;

            if (type == 1) img = sunflowerImg;
            if (type == 2) img = peashootImg;
            if (type == 3) img = giganutImg;

            if (img != null) {
                RectF dst = new RectF(
                        left + 25,
                        top + 3,
                        right - 25,
                        bottom - 25
                );

                canvas.drawBitmap(img, null, dst, paint);
            } else {
                drawFallbackPlant(
                        canvas,
                        (left + right) / 2,
                        top + 25,
                        type,
                        35
                );
            }

            paint.setColor(Color.DKGRAY);
            paint.setTextSize(12);
            canvas.drawText(name, left + 12, bottom - 7, paint);
        }

        void drawBoard(Canvas canvas) {
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {

                    paint.setStyle(Paint.Style.FILL);

                    if ((r + c) % 2 == 0) {
                        paint.setColor(Color.rgb(116, 190, 75));
                    } else {
                        paint.setColor(Color.rgb(108, 181, 68));
                    }

                    float l = boardLeft + c * cellW;
                    float t = boardTop + r * cellH;

                    canvas.drawRect(
                            l,
                            t,
                            l + cellW,
                            t + cellH,
                            paint
                    );

                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(2);
                    paint.setColor(Color.rgb(75, 145, 55));

                    canvas.drawRect(
                            l,
                            t,
                            l + cellW,
                            t + cellH,
                            paint
                    );

                    paint.setStyle(Paint.Style.FILL);
                }
            }

            paint.setColor(Color.WHITE);
            paint.setTextSize(15);
            canvas.drawText(
                    "Chọn cây rồi chạm vào ô sân",
                    25,
                    getHeight() - 15,
                    paint
            );
        }

        void updateGame(float dt) {

            zombieTimer += (long)(dt * 1000);
            sunTimer += (long)(dt * 1000);

            if (sunTimer >= 5000) {
                money += 25;
                sunTimer = 0;
            }

            if (zombieTimer >= 3500) {
                spawnZombie();
                zombieTimer = 0;
            }

            for (Plant p : plants) {

                p.timer += dt;

                if (p.type == 1) {
                    if (p.timer >= 6) {
                        money += 25;
                        p.timer = 0;
                    }
                }

                if (p.type == 2) {
                    if (p.timer >= 1.2f) {

                        Zombie target = findZombie(p.row);

                        if (target != null) {
                            peas.add(
                                    new Pea(
                                            p.x + 35,
                                            p.y,
                                            p.row
                                    )
                            );
                        }

                        p.timer = 0;
                    }
                }
            }

            for (Pea pea : peas) {

                pea.x += 430 * dt;

                Iterator<Zombie> zi = zombies.iterator();

                while (zi.hasNext()) {

                    Zombie z = zi.next();

                    if (z.row == pea.row &&
                            Math.abs(pea.x - z.x) < 35) {

                        z.hp -= 25;
                        pea.dead = true;

                        if (z.hp <= 0) {
                            zi.remove();
                            money += 50;
                        }

                        break;
                    }
                }

                if (pea.x > getWidth()) {
                    pea.dead = true;
                }
            }

            Iterator<Pea> pi = peas.iterator();

            while (pi.hasNext()) {
                if (pi.next().dead) {
                    pi.remove();
                }
            }

            for (Zombie z : zombies) {

                Plant blocker = findBlockingPlant(z);

                if (blocker != null) {

                    if (blocker.type == 3) {
                        blocker.hp -= 8 * dt;

                        if (blocker.hp <= 0) {
                            plants.remove(blocker);
                        }
                    }

                } else {
                    z.x -= 35 * dt;
                }

                if (z.x < boardLeft - 50) {
                    z.x = boardLeft - 50;
                }
            }
        }

        Zombie findZombie(int row) {
            Zombie closest = null;

            for (Zombie z : zombies) {

                if (z.row == row && z.x > 0) {

                    if (closest == null || z.x < closest.x) {
                        closest = z;
                    }
                }
            }

            return closest;
        }

        Plant findBlockingPlant(Zombie z) {

            for (Plant p : plants) {

                if (p.row == z.row &&
                        Math.abs(p.x - z.x) < 55) {

                    return p;
                }
            }

            return null;
        }

        void spawnZombie() {

            int row =
