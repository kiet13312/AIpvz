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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new BattleView());
    }

    private class BattleView extends View {

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Random random = new Random();

        // ===== IMAGES =====
        private Bitmap sunflower;
        private Bitmap peashooter;
        private Bitmap giganut;
        private Bitmap gigapea;
        private Bitmap zomplatz;

        // ===== GAME =====
        private final ArrayList<Plant> plants = new ArrayList<>();
        private final ArrayList<Zombie> zombies = new ArrayList<>();
        private final ArrayList<Projectile> projectiles = new ArrayList<>();

        private int selectedPlant = 0;

        private int sun = 250;

        private final int rows = 5;
        private final int cols = 8;

        private float gridLeft;
        private float gridTop;
        private float cellWidth;
        private float cellHeight;

        private long lastZombieSpawn = 0;
        private long lastPlantShot = 0;

        private boolean gameOver = false;

        BattleView() {
            super(MainActivity.this);

            setBackgroundColor(Color.rgb(100, 175, 75));

            sunflower = loadBitmap("sunflower");
            peashooter = loadBitmap("peashooter");
            giganut = loadBitmap("giganut");
            gigapea = loadBitmap("gigapea");
            zomplatz = loadBitmap("zomplatz");
        }

        private Bitmap loadBitmap(String name) {
            int resourceId = getResources().getIdentifier(
                    name,
                    "drawable",
                    getPackageName()
            );

            if (resourceId == 0) {
                return null;
            }

            return BitmapFactory.decodeResource(
                    getResources(),
                    resourceId
            );
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            calculateGrid();

            drawBackground(canvas);
            drawTopBar(canvas);
            drawPlantCards(canvas);
            drawGrid(canvas);
            drawPlants(canvas);
            drawProjectiles(canvas);
            drawZombies(canvas);

            if (gameOver) {
                drawGameOver(canvas);
            }

            updateGame();

            postInvalidateDelayed(30);
        }

        private void calculateGrid() {
            gridLeft = 20;
            gridTop = 210;

            cellWidth = (getWidth() - 40f) / cols;

            float availableHeight = getHeight() - gridTop - 20f;
            cellHeight = availableHeight / rows;
        }

        private void drawBackground(Canvas canvas) {
            canvas.drawColor(Color.rgb(105, 180, 75));
        }

        private void drawTopBar(Canvas canvas) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(45, 100, 45));

            canvas.drawRect(
                    0,
                    0,
                    getWidth(),
                    70,
                    paint
            );

            paint.setColor(Color.WHITE);
            paint.setTextSize(28);
            paint.setFakeBoldText(true);

            canvas.drawText(
                    "GARDEN DEFENSE",
                    20,
                    45,
                    paint
            );

            paint.setColor(Color.YELLOW);

            canvas.drawCircle(
                    getWidth() - 120,
                    35,
                    17,
                    paint
            );

            paint.setColor(Color.WHITE);
            paint.setTextSize(22);
            paint.setFakeBoldText(false);

            canvas.drawText(
                    String.valueOf(sun),
                    getWidth() - 90,
                    43,
                    paint
            );
        }

        private void drawPlantCards(Canvas canvas) {

            drawPlantCard(
                    canvas,
                    20,
                    90,
                    sunflower,
                    "SUN",
                    1
            );

            drawPlantCard(
                    canvas,
                    160,
                    90,
                    peashooter,
                    "PEA",
                    2
            );

            drawPlantCard(
                    canvas,
                    300,
                    90,
                    giganut,
                    "GIGA",
                    3
            );
        }

        private void drawPlantCard(
                Canvas canvas,
                float x,
                float y,
                Bitmap bitmap,
                String text,
                int type
        ) {

            if (selectedPlant == type) {
                paint.setColor(Color.YELLOW);
            } else {
                paint.setColor(Color.rgb(235, 240, 220));
            }

            canvas.drawRoundRect(
                    new RectF(
                            x,
                            y,
                            x + 120,
                            y + 95
                    ),
                    15,
                    15,
                    paint
            );

            if (bitmap != null) {

                canvas.drawBitmap(
                        bitmap,
                        null,
                        new RectF(
                                x + 12,
                                y + 8,
                                x + 68,
                                y + 78
                        ),
                        paint
                );
            }

            paint.setColor(Color.rgb(35, 80, 35));
            paint.setTextSize(14);
            paint.setFakeBoldText(true);

            canvas.drawText(
                    text,
                    x + 76,
                    y + 53,
                    paint
            );

            paint.setFakeBoldText(false);
        }

        private void drawGrid(Canvas canvas) {

            paint.setStyle(Paint.Style.FILL);

            for (int row = 0; row < rows; row++) {

                for (int col = 0; col < cols; col++) {

                    float left = gridLeft + col * cellWidth;
                    float top = gridTop + row * cellHeight;

                    if ((row + col) % 2 == 0) {
                        paint.setColor(Color.rgb(125, 195, 85));
                    } else {
                        paint.setColor(Color.rgb(115, 185, 75));
                    }

                    canvas.drawRect(
                            left,
                            top,
                            left + cellWidth - 2,
                            top + cellHeight - 2,
                            paint
                    );
                }
            }
        }

        private void drawPlants(Canvas canvas) {

            for (Plant plant : plants) {

                Bitmap bitmap = null;

                if (plant.type == 1) {
                    bitmap = sunflower;
                } else if (plant.type == 2) {
                    bitmap = peashooter;
                } else if (plant.type == 3) {
                    bitmap = giganut;
                }

                if (bitmap != null) {

                    float left =
                            gridLeft + plant.col * cellWidth + 5;

                    float top =
                            gridTop + plant.row * cellHeight + 5;

                    canvas.drawBitmap(
                            bitmap,
                            null,
                            new RectF(
                                    left,
                                    top,
                                    left + cellWidth - 10,
                                    top + cellHeight - 10
                            ),
                            paint
                    );
                }

                drawHealthBar(
                        canvas,
                        gridLeft + plant.col * cellWidth + 8,
                        gridTop + plant.row * cellHeight + 4,
                        cellWidth - 16,
                        plant.hp,
                        plant.maxHp
                );
            }
        }

        private void drawProjectiles(Canvas canvas) {

            for (Projectile projectile : projectiles) {

                if (gigapea != null) {

                    canvas.drawBitmap(
                            gigapea,
                            null,
                            new RectF(
                                    projectile.x - 12,
                                    projectile.y - 12,
                                    projectile.x + 12,
                                    projectile.y + 12
                            ),
                            paint
                    );

                } else {

                    paint.setColor(Color.YELLOW);

                    canvas.drawCircle(
                            projectile.x,
                            projectile.y,
                            8,
                            paint
                    );
                }
            }
        }

        private void drawZombies(Canvas canvas) {

            for (Zombie zombie : zombies) {

                if (zomplatz != null) {

                    canvas.drawBitmap(
                            zomplatz,
                            null,
                            new RectF(
                                    zombie.x,
                                    zombie.y,
                                    zombie.x + 65,
                                    zombie.y + 95
                            ),
                            paint
                    );

                } else {

                    paint.setColor(Color.GRAY);

                    canvas.drawCircle(
                            zombie.x + 32,
                            zombie.y + 25,
                            25,
                            paint
                    );

                    canvas.drawRect(
                            zombie.x + 10,
                            zombie.y + 45,
                            zombie.x + 55,
                            zombie.y + 95,
                            paint
                    );
                }

                drawHealthBar(
                        canvas,
                        zombie.x,
                        zombie.y - 10,
                        65,
                        zombie.hp,
                        zombie.maxHp
                );
            }
        }

        private void drawHealthBar(
                Canvas canvas,
                float x,
                float y,
                float width,
               
