package com.alpvz.gardendefense;

import android.app.Activity;
import android.os.Bundle;
import android.content.pm.ActivityInfo;
import android.graphics.*;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.view.*;
import java.util.*;

/*
 * NEW MainActivity.java
 * Viết lại độc lập, không ghép từ các bản MainActivity cũ.
 *
 * Giữ:
 * - Màn hình ngang/fullscreen
 * - Bàn 5 x 9
 * - 9 màn
 * - Màn 1-4 bình thường
 * - Màn 2 KHÔNG phải băng chuyền
 * - Màn 5-8: chế độ thường như các màn khác
 * - Máy cắt cỏ 5 hàng, mỗi hàng dùng 1 lần
 * - Sunflower, Peashooter, Giganut, Chomper, Repeater, Mine
 * - Plant Food
 * - Xẻng
 * - Repeater bắn 2 viên, viên thứ hai trễ 0.5 giây
 * - Boss lớn ở màn 9 bắn pea
 *
 * Không có:
 * - Zen Garden
 * - loading screen
 * - zombie Vĩnh Hùng
 * - băng chuyền cây ở màn 2
 */
public class MainActivity extends Activity {

    private ToneGenerator tone;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
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

        try {
            tone = new ToneGenerator(AudioManager.STREAM_MUSIC, 45);
        } catch (Exception ignored) {
        }

        setContentView(new GameView());
    }

    private void beep(int t) {
        try {
            if (tone != null) tone.startTone(t, 50);
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onDestroy() {
        try {
            if (tone != null) tone.release();
        } catch (Exception ignored) {
        }
        super.onDestroy();
    }

    private class GameView extends View {

        private static final int ROWS = 5;
        private static final int COLS = 9;
        private static final int MAX_LEVEL = 9;

        private static final int SUNFLOWER = 1;
        private static final int PEASHOOTER = 2;
        private static final int GIGANUT = 3;
        private static final int CHOMPER = 4;
        private static final int REPEATER = 5;
        private static final int MINE = 6;

        private static final int TOOL_NONE = 0;
        private static final int TOOL_SHOVEL = 100;
        private static final int TOOL_PLANT_FOOD = 101;

        private static final int SCREEN_HOME = 0;
        private static final int SCREEN_LEVELS = 1;
        private static final int SCREEN_PLAY = 2;
        private static final int SCREEN_PAUSE = 3;
        private static final int SCREEN_WIN = 4;
        private static final int SCREEN_LOSE = 5;

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Random random = new Random();

        private final Plant[][] plants = new Plant[ROWS][COLS];
        private final ArrayList<Zombie> zombies = new ArrayList<>();
        private final ArrayList<Pea> peas = new ArrayList<>();
        private final ArrayList<SunDrop> suns = new ArrayList<>();
        private final Mower[] mowers = new Mower[ROWS];

        private int screen = SCREEN_HOME;
        private int level = 1;
        private int maxUnlocked = 1;

        private int sun = 500;
        private int coins = 99999;
        private int plantFood = 3;

        private int selectedPlant = PEASHOOTER;
        private int tool = TOOL_NONE;

        private int wave = 0;
        private int totalWaves = 5;
        private int spawnTarget = 0;
        private int spawned = 0;


        private long lastFrame;
        private long lastSpawn;
        private long naturalSunTimer;

        private boolean running = true;

        private float left;
        private float top;
        private float cellW;
        private float cellH;

        GameView() {
            super(MainActivity.this);
            setFocusable(true);

            textPaint.setTypeface(Typeface.create(
                    Typeface.DEFAULT,
                    Typeface.BOLD
            ));

            lastFrame = System.currentTimeMillis();

            for (int r = 0; r < ROWS; r++) {
                mowers[r] = new Mower(r);
            }

            post(gameLoop);
        }

        private final Runnable gameLoop = new Runnable() {
            @Override
            public void run() {
                if (!running) return;

                update();
                invalidate();
                postDelayed(this, 30);
            }
        };

        @Override
        protected void onSizeChanged(
                int w,
                int h,
                int oldW,
                int oldH
        ) {
            left = w * 0.18f;
            top = h * 0.20f;

            cellW = w * 0.072f;
            cellH = h * 0.125f;

        }

        @Override
        protected void onDraw(Canvas c) {
            super.onDraw(c);

            if (screen == SCREEN_HOME) {
                drawHome(c);
            } else if (screen == SCREEN_LEVELS) {
                drawLevels(c);
            } else {
                drawGame(c);

                if (screen == SCREEN_PAUSE) {
                    drawPause(c);
                } else if (screen == SCREEN_WIN) {
                    drawWin(c);
                } else if (screen == SCREEN_LOSE) {
                    drawLose(c);
                }
            }
        }

        private void drawHome(Canvas c) {
            c.drawColor(Color.rgb(25, 55, 30));

            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(getHeight() * 0.10f);

            c.drawText(
                    "GARDEN DEFENSE",
                    getWidth() / 2f,
                    getHeight() * 0.22f,
                    textPaint
            );

            button(
                    c,
                    getWidth() * 0.32f,
                    getHeight() * 0.38f,
                    getWidth() * 0.68f,
                    getHeight() * 0.50f,
                    "CHƠI"
            );

            button(
                    c,
                    getWidth() * 0.32f,
                    getHeight() * 0.55f,
                    getWidth() * 0.68f,
                    getHeight() * 0.67f,
                    "CHỌN MÀN"
            );

            textPaint.setTextSize(getHeight() * 0.035f);
            textPaint.setColor(Color.LTGRAY);

            c.drawText(
                    "5×9 • 9 MÀN • MÁY CẮT CỎ",
                    getWidth() / 2f,
                    getHeight() * 0.82f,
                    textPaint
            );
        }

        private void drawLevels(Canvas c) {
            c.drawColor(Color.rgb(20, 50, 25));

            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(getHeight() * 0.065f);

            c.drawText(
                    "CHỌN MÀN",
                    getWidth() / 2f,
                    getHeight() * 0.11f,
                    textPaint
            );

            for (int i = 1; i <= MAX_LEVEL; i++) {
                int col = (i - 1) % 3;
                int row = (i - 1) / 3;

                float x1 = getWidth() * 0.18f
                        + col * getWidth() * 0.22f;
                float y1 = getHeight() * 0.18f
                        + row * getHeight() * 0.20f;
                float x2 = x1 + getWidth() * 0.17f;
                float y2 = y1 + getHeight() * 0.13f;

                paint.setColor(
                        isLevelUnlocked(i)
                                ? Color.rgb(65, 145, 70)
                                : Color.rgb(70, 70, 70)
                );

                c.drawRoundRect(
                        x1, y1, x2, y2,
                        18, 18, paint
                );

                textPaint.setTextSize(getHeight() * 0.038f);
                textPaint.setColor(Color.WHITE);

                c.drawText(
                        isLevelUnlocked(i)
                                ? "MÀN " + i
                                : "KHÓA",
                        (x1 + x2) / 2f,
                        y1 + getHeight() * 0.083f,
                        textPaint
                );
            }

            button(
                    c,
                    getWidth() * 0.04f,
                    getHeight() * 0.83f,
                    getWidth() * 0.20f,
                    getHeight() * 0.94f,
                    "QUAY LẠI"
            );
        }

        private void drawGame(Canvas c) {
            paint.setColor(Color.rgb(92, 155, 70));
            c.drawRect(0, 0, getWidth(), getHeight(), paint);

            drawTop(c);
            drawBoard(c);
            drawPlants(c);
            drawZombies(c);
            drawPeas(c);
            drawSuns(c);
            drawMowers(c);
        }

        private void drawTop(Canvas c) {
            paint.setColor(Color.rgb(38, 78, 40));
            c.drawRect(0, 0, getWidth(), top, paint);

            textPaint.setTextAlign(Paint.Align.LEFT);
            textPaint.setColor(Color.YELLOW);
            textPaint.setTextSize(getHeight() * 0.033f);

            c.drawText(
                    "☀ " + sun,
                    12,
                    getHeight() * 0.045f,
                    textPaint
            );

            textPaint.setColor(Color.WHITE);
            c.drawText(
                    "Màn " + level,
                    getWidth() * 0.34f,
                    getHeight() * 0.045f,
                    textPaint
            );

            c.drawText(
                    "Sóng " + wave + "/" + totalWaves,
                    getWidth() * 0.47f,
                    getHeight() * 0.045f,
                    textPaint
            );

            textPaint.setColor(Color.YELLOW);
            c.drawText(
                    "Xu " + coins,
                    getWidth() * 0.70f,
                    getHeight() * 0.045f,
                    textPaint
            );

            textPaint.setColor(Color.WHITE);
            c.drawText(
                    "PF " + plantFood,
                    getWidth() * 0.87f,
                    getHeight() * 0.045f,
                    textPaint
            );

            drawCard(c, SUNFLOWER, 0);
            drawCard(c, PEASHOOTER, 1);
            drawCard(c, GIGANUT, 2);
            drawCard(c, CHOMPER, 3);
            drawCard(c, REPEATER, 4);
            drawCard(c, MINE, 5);

            float sx = getWidth() * 0.465f;
            float sy = getHeight() * 0.075f;

            paint.setColor(
                    tool == TOOL_SHOVEL
                            ? Color.YELLOW
                            : Color.DKGRAY
            );

            c.drawRoundRect(
                    sx, sy,
                    sx + getWidth() * 0.065f,
                    sy + getHeight() * 0.09f,
                    10, 10, paint
            );

            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTextSize(getHeight() * 0.035f);
            textPaint.setColor(Color.WHITE);

            c.drawText(
                    "X",
                    sx + getWidth() * 0.0325f,
                    sy + getHeight() * 0.060f,
                    textPaint
            );
        }

        private void drawCard(Canvas c, int type, int index) {
            float x = getWidth() * 0.01f
                    + index * getWidth() * 0.075f;
            float y = getHeight() * 0.075f;
            float w = getWidth() * 0.065f;
            float h = getHeight() * 0.09f;

            boolean unlocked = plantUnlocked(type);

            paint.setColor(
                    selectedPlant == type && tool == TOOL_NONE
                            ? Color.YELLOW
                            : Color.rgb(45, 80, 45)
            );

            c.drawRoundRect(
                    x, y, x + w, y + h,
                    10, 10, paint
            );

            if (unlocked) {
                drawPlantIcon(
                        c,
                        type,
                        x + w / 2f,
                        y + h / 2f,
                        Math.min(w, h) * 0.62f
                );
            } else {
                textPaint.setTextAlign(Paint.Align.CENTER);
                textPaint.setColor(Color.GRAY);
                textPaint.setTextSize(h * 0.35f);
                c.drawText(
                        "🔒",
                        x + w / 2f,
                        y + h * 0.63f,
                        textPaint
                );
            }
        }

        private void drawBoard(Canvas c) {
            for (int r = 0; r < ROWS; r++) {
                for (int col = 0; col < COLS; col++) {
                    float x1 = left + col * cellW;
                    float y1 = top + r * cellH;

                    paint.setColor(
                            (r + col) % 2 == 0
                                    ? Color.rgb(103, 166, 78)
                                    : Color.rgb(91, 153, 67)
                    );

                    c.drawRect(
                            x1,
                            y1,
                            x1 + cellW,
                            y1 + cellH,
                            paint
                    );
                }
            }

            paint.setColor(Color.rgb(130, 92, 52));
            c.drawRect(
                    0,
                    top,
                    left,
                    top + ROWS * cellH,
                    paint
            );
        }

        private void drawPlants(Canvas c) {
            for (int r = 0; r < ROWS; r++) {
                for (int col = 0; col < COLS; col++) {
                    Plant pl = plants[r][col];
                    if (pl == null) continue;

                    float x = left + col * cellW + cellW / 2f;
                    float y = top + r * cellH + cellH / 2f;

                    drawPlantIcon(
                            c,
                            pl.type,
                            x,
                            y,
                            Math.min(cellW, cellH) * 0.72f
                    );

                    drawHp(
                            c,
                            x - cellW * 0.30f,
                            y + cellH * 0.33f,
                            cellW * 0.60f,
                            5,
                            pl.hp,
                            pl.maxHp
                    );
                }
            }
        }

        private void drawPlantIcon(
                Canvas c,
                int type,
                float x,
                float y,
                float size
        ) {
            if (type == SUNFLOWER) {
                paint.setColor(Color.YELLOW);
                for (int i = 0; i < 8; i++) {
                    double a = i * Math.PI / 4;
                    c.drawCircle(
                            x + (float)Math.cos(a) * size * 0.30f,
                            y + (float)Math.sin(a) * size * 0.30f,
                            size * 0.17f,
                            paint
                    );
                }

                paint.setColor(Color.rgb(110, 65, 25));
                c.drawCircle(x, y, size * 0.25f, paint);

            } else if (type == PEASHOOTER) {
                paint.setColor(Color.rgb(55, 180, 70));
                c.drawCircle(x, y, size * 0.36f, paint);

                paint.setColor(Color.rgb(45, 145, 55));
                c.drawCircle(
                        x + size * 0.27f,
                        y - size * 0.04f,
                        size * 0.22f,
                        paint
                );

                paint.setColor(Color.BLACK);
                c.drawCircle(
                        x + size * 0.31f,
                        y - size * 0.04f,
                        size * 0.06f,
                        paint
                );

            } else if (type == GIGANUT) {
                paint.setColor(Color.rgb(155, 105, 55));
                c.drawOval(
                        x - size * 0.36f,
                        y - size * 0.45f,
                        x + size * 0.36f,
                        y + size * 0.45f,
                        paint
                );

                paint.setColor(Color.BLACK);
                c.drawCircle(
                        x - size * 0.13f,
                        y - size * 0.07f,
                        size * 0.05f,
                        paint
                );
                c.drawCircle(
                        x + size * 0.13f,
                        y - size * 0.07f,
                        size * 0.05f,
                        paint
                );

            } else if (type == CHOMPER) {
                paint.setColor(Color.rgb(145, 80, 185));
                c.drawCircle(x, y, size * 0.38f, paint);

                paint.setColor(Color.WHITE);
                Path teeth = new Path();

                teeth.moveTo(x - size * 0.28f, y);
                teeth.lineTo(x - size * 0.16f, y + size * 0.22f);
                teeth.lineTo(x - size * 0.05f, y);
                teeth.lineTo(x + size * 0.06f, y + size * 0.22f);
                teeth.lineTo(x + size * 0.18f, y);
                teeth.close();

                c.drawPath(teeth, paint);

            } else if (type == REPEATER) {
                paint.setColor(Color.rgb(55, 175, 70));
                c.drawCircle(x, y, size * 0.35f, paint);

                paint.setColor(Color.rgb(45, 135, 55));
                c.drawCircle(
                        x + size * 0.24f,
                        y - size * 0.10f,
                        size * 0.18f,
                        paint
                );
                c.drawCircle(
                        x + size * 0.38f,
                        y - size * 0.02f,
                        size * 0.12f,
                        paint
                );

            } else if (type == MINE) {
                paint.setColor(Color.rgb(65, 65, 65));
                c.drawCircle(x, y, size * 0.36f, paint);

                paint.setColor(Color.RED);
                c.drawCircle(x, y, size * 0.12f, paint);
            }
        }

        private void drawZombies(Canvas c) {
            for (Zombie z : zombies) {
                float w = z.boss ? cellW * 1.15f : cellW * 0.55f;
                float h = z.boss ? cellH * 1.10f : cellH * 0.72f;

                      paint.setColor(
                        z.boss
                                ? Color.rgb(90, 45, 110)
                                : z.type == 1
                                ? Color.rgb(105, 90, 110)
                                : Color.rgb(80, 110, 80)
                );

                c.drawRect(
                        z.x - w / 2f,
                        z.y,
                        z.x + w / 2f,
                        z.y + h,
                        paint
                );

                paint.setColor(Color.rgb(155, 180, 145));
                c.drawCircle(
                        z.x,
                        z.y - h * 0.06f,
                        w * 0.40f,
                        paint
                );

                paint.setColor(Color.BLACK);

                c.drawCircle(
                        z.x - w * 0.13f,
                        z.y - h * 0.08f,
                        w * 0.045f,
                        paint
                );
                c.drawCircle(
                        z.x + w * 0.13f,
                        z.y - h * 0.08f,
                        w * 0.045f,
                        paint
                );

                drawHp(
                        c,
                        z.x - w * 0.50f,
                        z.y - h * 0.26f,
                        w,
                        5,
                        z.hp,
                        z.maxHp
                );
            }
        }

        private void drawPeas(Canvas c) {
            for (Pea pea : peas) {
                paint.setColor(
                        pea.ice
                                ? Color.CYAN
                                : Color.rgb(70, 220, 70)
                );

                c.drawCircle(
                        pea.x,
                        pea.y,
                        pea.ice ? 8 : 7,
                        paint
                );
            }
        }

        private void drawSuns(Canvas c) {
            for (SunDrop s : suns) {
                paint.setColor(Color.YELLOW);
                c.drawCircle(s.x, s.y, 15, paint);

                textPaint.setTextAlign(Paint.Align.CENTER);
                textPaint.setColor(Color.rgb(120, 80, 0));
                textPaint.setTextSize(14);

                c.drawText(
                        "+",
                        s.x,
                        s.y + 5,
                        textPaint
                );
            }
        }

        private void drawMowers(Canvas c) {
            for (Mower m : mowers) {
                float x = m.x;
                float y = top + m.row * cellH + cellH * 0.50f;

                paint.setColor(
                        m.used
                                ? Color.DKGRAY
                                : Color.rgb(180, 70, 40)
                );

                c.drawRoundRect(
                        x - cellW * 0.30f,
                        y - cellH * 0.25f,
                        x + cellW * 0.30f,
                        y + cellH * 0.25f,
                        8, 8, paint
                );

                paint.setColor(Color.BLACK);
                c.drawCircle(
                        x - cellW * 0.18f,
                        y + cellH * 0.25f,
                        7,
                        paint
                );
                c.drawCircle(
                        x + cellW * 0.18f,
                        y + cellH * 0.25f,
                        7,
                        paint
                );
            }
        }

        private void drawPause(Canvas c) {
            overlay(c, Color.argb(190, 0, 0, 0));

            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(getHeight() * 0.09f);

            c.drawText(
                    "TẠM DỪNG",
                    getWidth() / 2f,
                    getHeight() * 0.35f,
                    textPaint
            );

            button(
                    c,
                    getWidth() * 0.32f,
                    getHeight() * 0.50f,
                    getWidth() * 0.68f,
                    getHeight() * 0.63f,
                    "TIẾP TỤC"
            );
        }

        private void drawWin(Canvas c) {
            overlay(c, Color.argb(215, 0, 80, 0));

            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setColor(Color.YELLOW);
            textPaint.setTextSize(getHeight() * 0.09f);

            c.drawText(
                    "CHIẾN THẮNG!",
                    getWidth() / 2f,
                    getHeight() * 0.32f,
                    textPaint
            );

            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(getHeight() * 0.04f);

            c.drawText(
                    "+" + mowerRewardForDisplay() + " XU",
                    getWidth() / 2f,
                    getHeight() * 0.41f,
                    textPaint
            );

            button(
                    c,
                    getWidth() * 0.30f,
                    getHeight() * 0.55f,
                    getWidth() * 0.70f,
                    getHeight() * 0.68f,
                    level < MAX_LEVEL ? "MÀN TIẾP" : "MENU"
            );
        }

        private void drawLose(Canvas c) {
            overlay(c, Color.argb(215, 100, 0, 0));

            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(getHeight() * 0.09f);

            c.drawText(
                    "THUA!",
                    getWidth() / 2f,
                    getHeight() * 0.32f,
                    textPaint
            );

            button(
                    c,
                    getWidth() * 0.30f,
                    getHeight() * 0.50f,
                    getWidth() * 0.70f,
                    getHeight() * 0.63f,
                    "CHƠI LẠI"
            );

            button(
                    c,
                    getWidth() * 0.30f,
                    getHeight() * 0.68f,
                    getWidth() * 0.70f,
                    getHeight() * 0.81f,
                    "MENU"
            );
        }

        

        private void update() {
            long now = System.currentTimeMillis();

            if (screen == SCREEN_PLAY) {
                updateGame(now);
            }

            lastFrame = now;
        }

        private void updateGame(long now) {
            if (now - naturalSunTimer > 8000) {
                naturalSunTimer = now;
                suns.add(new SunDrop(
                        left + random.nextInt(COLS) * cellW + cellW / 2f,
                        top * 0.80f
                ));
            }

            updatePlants(now);
            updatePeas();
            updateZombies(now);
            updateMowers();

            if (spawned < spawnTarget &&
                    now - lastSpawn >= spawnDelay()) {

                spawnZombie();
                lastSpawn = now;
                spawned++;
            }

            if (spawned >= spawnTarget &&
                    zombies.isEmpty()) {

                wave++;

                if (wave >= totalWaves) {
                    finishWin();
                } else {
                    spawned = 0;
                    spawnTarget = waveTarget();
                    lastSpawn = now;
                }
            }

            collectAutomaticSunDrops();
        }

        private void updatePlants(long now) {
            for (int r = 0; r < ROWS; r++) {
                for (int col = 0; col < COLS; col++) {
                    Plant pl = plants[r][col];
                    if (pl == null) continue;

                    if (pl.type == SUNFLOWER &&
                            now - pl.lastAction >= 7000) {

                        suns.add(new SunDrop(
                                left + col * cellW + cellW / 2f,
                                top + r * cellH + cellH * 0.25f
                        ));

                        pl.lastAction = now;
                    }

                    if (pl.type == PEASHOOTER &&
                            now - pl.lastAction >= 1500 &&
                            rowHasZombie(r)) {

                        firePea(
                                r,
                                col,
                                30,
                                false
                        );

                        pl.lastAction = now;
                    }

                    if (pl.type == REPEATER &&
                            now - pl.lastAction >= 1800 &&
                            rowHasZombie(r)) {

                        firePea(r, col, 28, false);

                        // Viên thứ hai được đánh dấu để bắn sau 0.5s.
                        pl.secondShotAt = now + 500;
                        pl.lastAction = now;
                    }

                    if (pl.type == REPEATER &&
                            pl.secondShotAt > 0 &&
                            now >= pl.secondShotAt) {

                        firePea(r, col, 28, false);
                        pl.secondShotAt = 0;
                    }

                    if (pl.type == MINE &&
                            now >= pl.armedAt) {

                        Zombie hit = findZombieAtCell(r, col);

                        if (hit != null) {
                            hit.hp = 0;
                            pl.hp = 0;
                        }
                    }

                    if (pl.type == CHOMPER &&
                            now - pl.lastAction >= 3500) {

                        Zombie hit = nearestZombieInRange(r, col);

                        if (hit != null) {
                            hit.hp = 0;
                            pl.lastAction = now;
                        }
                    }
                }
            }

            removeDeadPlants();
            removeDeadZombies();
        }

        private void firePea(
                int row,
                int col,
                int damage,
                boolean ice
        ) {
            peas.add(new Pea(
                    left + col * cellW + cellW * 0.55f,
                    top + row * cellH + cellH * 0.50f,
                    row,
                    damage,
                    ice
            ));
        }

        private void updatePeas() {
            Iterator<Pea> it = peas.iterator();

            while (it.hasNext()) {
                Pea pea = it.next();

                pea.x += cellW * 0.045f;

                Zombie hit = null;

                for (Zombie z : zombies) {
                    if (z.row != pea.row) continue;

                    if (Math.abs(z.x - pea.x) <
                            cellW * 0.28f) {
                        hit = z;
                        break;
                    }
                }

                if (hit != null) {
                    hit.hp -= pea.damage;

                    if (pea.ice) {
                        hit.slowUntil =
                                System.currentTimeMillis() + 2500;
                    }

                    it.remove();
                } else if (pea.x > getWidth() + 20) {
                    it.remove();
                }
            }

            removeDeadZombies();
        }

        private void updateZombies(long now) {
            Iterator<Zombie> it = zombies.iterator();

            while (it.hasNext()) {
                Zombie z = it.next();

                if (z.hp <= 0) {
                    it.remove();
                    coins += 5;
                    continue;
                }

                float speed = z.speed;

                if (now < z.slowUntil) {
                    speed *= 0.55f;
                }

                Plant target = plantAtZombie(z);

                if (target != null) {
                    if (now - z.lastAttack >= 850) {
                        target.hp -= z.damage;
                        z.lastAttack = now;
                    }
                } else {
                    z.x -= speed;
                }

                if (z.boss &&
                        now - z.bossShotAt >= 2200) {

                    fireBossPea(z);
                    z.bossShotAt = now;
                }

                if (z.x < left - cellW * 0.55f) {
                    Mower mower = mowers[z.row];

                    if (!mower.used) {
                        mower.active = true;
                        mower.x = left + cellW * 0.45f;
                        mower.used = true;
                    } else {
                        screen = SCREEN_LOSE;
                        beep(ToneGenerator.TONE_PROP_NACK);
                    }

                    it.remove();
                }
            }

            removeDeadPlants();
        }

        private void fireBossPea(Zombie boss) {
            peas.add(new Pea(
                    boss.x - cellW * 0.35f,
                    boss.y + cellH * 0.35f,
                    boss.row,
                    40,
                    false
            ));
        }

        private void updateMowers() {
            for (Mower m : mowers) {
                if (!m.active) continue;

                m.x += cellW * 0.08f;

                for (Zombie z : zombies) {
                    if (z.row != m.row) continue;

                    if (Math.abs(z.x - m.x) <
                            cellW * 0.55f) {
                        z.hp = 0;
                    }
                }

                if (m.x > getWidth() + cellW) {
                    m.active = false;
                }
            }

            removeDeadZombies();
        }

        

        private void collectAutomaticSunDrops() {
            Iterator<SunDrop> it = suns.iterator();

            while (it.hasNext()) {
                SunDrop s = it.next();

                if (s.collected) {
                    it.remove();
                }
            }
        }

        private void spawnZombie() {
            int row = random.nextInt(ROWS);

            boolean boss =
                    level == MAX_LEVEL &&
                    spawned == spawnTarget - 1 &&
                    wave == totalWaves - 1;

            int type = random.nextInt(3);

            zombies.add(
                    new Zombie(
                            row,
                            type,
                            boss
                    )
            );
        }

        private void startLevel(int chosen) {
            if (chosen < 1) chosen = 1;
            if (chosen > MAX_LEVEL) chosen = MAX_LEVEL;

            level = chosen;
            screen = SCREEN_PLAY;

            clearBoardState();

            totalWaves = wavesForLevel();
            wave = 0;
            spawned = 0;
            spawnTarget = waveTarget();


            lastSpawn = System.currentTimeMillis();
            naturalSunTimer = lastSpawn;

        }

        

        

        

        private int mowerRewardForDisplay() {
            int reward = 0;
            for (Mower m : mowers) {
                if (!m.used) reward += 50;
            }
            return reward;
        }

        private void finishWin() {
            screen = SCREEN_WIN;
            coins += mowerRewardForDisplay();

            if (level < MAX_LEVEL) {
                maxUnlocked =
                        Math.max(maxUnlocked, level + 1);
            }
        }

        private void clearBoardState() {
            for (int r = 0; r < ROWS; r++) {
                for (int col = 0; col < COLS; col++) {
                    plants[r][col] = null;
                }
                mowers[r] = new Mower(r);
            }

            zombies.clear();
            peas.clear();
            suns.clear();
        }

        private int wavesForLevel() {
            if (level <= 2) return 5;
            if (level <= 4) return 6;
            if (level <= 8) return 5;
            return 7;
        }

        private int waveTarget() {
            return 3 + Math.min(
                    5,
                    level / 2 + wave
            );
        }

        private long spawnDelay() {
            if (level <= 2) return 4300;
            if (level <= 4) return 3800;
            if (level <= 8) return 3200;
            return 2600;
        }

        private boolean isLevelUnlocked(int n) {
            return n <= maxUnlocked;
        }

        private boolean plantUnlocked(int type) {
            if (type == SUNFLOWER) return level >= 1;
            if (type == PEASHOOTER) return level >= 1;
            if (type == GIGANUT) return level >= 2;
            if (type == CHOMPER) return level >= 3;
            if (type == REPEATER) return level >= 4;
            if (type == MINE) return level >= 5;
            return false;
        }

        private int plantCost(int type) {
            if (type == SUNFLOWER) return 50;
            if (type == PEASHOOTER) return 100;

            if (type == GIGANUT) return 125;
            if (type == CHOMPER) return 150;
            if (type == REPEATER) return 200;
            if (type == MINE) return 50;
            return 0;
        }

        private boolean rowHasZombie(int row) {
            for (Zombie z : zombies) {
                if (z.row == row && z.x > left) {
                    return true;
                }
            }
            return false;
                          }

        private Plant plantAtZombie(Zombie z) {
            int col =
                    (int)((z.x - left) / cellW);

            if (col < 0 || col >= COLS) {
                return null;
            }

            return plants[z.row][col];
        }

        private Zombie findZombieAtCell(
                int row,
                int col
        ) {
            float center =
                    left + col * cellW + cellW / 2f;

            for (Zombie z : zombies) {
                if (z.row == row &&
                        Math.abs(z.x - center) <
                                cellW * 0.45f) {
                    return z;
                }
            }

            return null;
        }

        private Zombie nearestZombieInRange(
                int row,
                int col
        ) {
            float center =
                    left + col * cellW + cellW / 2f;

            Zombie best = null;
            float bestD = Float.MAX_VALUE;

            for (Zombie z : zombies) {
                if (z.row != row) continue;

                float d = Math.abs(z.x - center);

                if (d < cellW * 1.35f &&
                        d < bestD) {
                    best = z;
                    bestD = d;
                }
            }

            return best;
        }

        private void removeDeadPlants() {
            for (int r = 0; r < ROWS; r++) {
                for (int col = 0; col < COLS; col++) {
                    Plant p = plants[r][col];

                    if (p != null && p.hp <= 0) {
                        plants[r][col] = null;
                    }
                }
            }
        }

        private void removeDeadZombies() {
            Iterator<Zombie> it = zombies.iterator();

            while (it.hasNext()) {
                Zombie z = it.next();

                if (z.hp <= 0) {
                    it.remove();
                    coins += z.boss ? 100 : 5;
                }
            }
        }

        private void drawHp(
                Canvas c,
                float x,
                float y,
                float w,
                float h,
                float hp,
                float maxHp
        ) {
            paint.setColor(Color.RED);
            c.drawRect(x, y, x + w, y + h, paint);

            float ratio =
                    Math.max(0f,
                            Math.min(1f, hp / maxHp));

            paint.setColor(Color.GREEN);
            c.drawRect(
                    x,
                    y,
                    x + w * ratio,
                    y + h,
                    paint
            );
        }

        private void overlay(Canvas c, int color) {
            paint.setColor(color);
            c.drawRect(
                    0,
                    0,
                    getWidth(),
                    getHeight(),
                    paint
            );
        }

        private void button(
                Canvas c,
                float x1,
                float y1,
                float x2,
                float y2,
                String label
        ) {
            paint.setColor(Color.rgb(65, 135, 70));

            c.drawRoundRect(
                    x1, y1, x2, y2,
                    18, 18, paint
            );

            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(
                    (y2 - y1) * 0.36f
            );

            c.drawText(
                    label,
                    (x1 + x2) / 2f,
                    y1 + (y2 - y1) * 0.66f,
                    textPaint
            );
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            if (e.getAction() != MotionEvent.ACTION_UP) {
                return true;
            }

            float x = e.getX();
            float y = e.getY();

            if (screen == SCREEN_HOME) {
                if (inside(
                        x, y,
                        getWidth() * 0.32f,
                        getHeight() * 0.38f,
                        getWidth() * 0.68f,
                        getHeight() * 0.50f
                )) {
                    startLevel(level);
                    return true;
                }

                if (inside(
                        x, y,
                        getWidth() * 0.32f,
                        getHeight() * 0.55f,
                        getWidth() * 0.68f,
                        getHeight() * 0.67f
                )) {
                    screen = SCREEN_LEVELS;
                    return true;
                }

                return true;
            }

            if (screen == SCREEN_LEVELS) {
                if (inside(
                        x, y,
                        getWidth() * 0.04f,
                        getHeight() * 0.83f,
                        getWidth() * 0.20f,
                        getHeight() * 0.94f
                )) {
                    screen = SCREEN_HOME;
                    return true;
                }

                for (int i = 1; i <= MAX_LEVEL; i++) {
                    int col = (i - 1) % 3;
                    int row = (i - 1) / 3;

                    float x1 =
                            getWidth() * 0.18f +
                            col * getWidth() * 0.22f;

                    float y1 =
                            getHeight() * 0.18f +
                            row * getHeight() * 0.20f;

                    float x2 =
                            x1 + getWidth() * 0.17f;

                    float y2 =
                            y1 + getHeight() * 0.13f;

                    if (inside(x, y, x1, y1, x2, y2)) {
                        if (isLevelUnlocked(i)) {
                            startLevel(i);
                        }
                        return true;
                    }
                }

                return true;
            }

            if (screen == SCREEN_WIN) {
                if (inside(
                        x, y,
                        getWidth() * 0.30f,
                        getHeight() * 0.55f,
                        getWidth() * 0.70f,
                        getHeight() * 0.68f
                )) {
                    if (level < MAX_LEVEL) {
                        startLevel(level + 1);
                    } else {
                        screen = SCREEN_HOME;
                    }
                }

                return true;
            }

            if (screen == SCREEN_LOSE) {
                if (inside(
                        x, y,
                        getWidth() * 0.30f,
                        getHeight() * 0.50f,
                        getWidth() * 0.70f,
                        getHeight() * 0.63f
                )) {
                    startLevel(level);
                    return true;
                }

                if (inside(
                        x, y,
                        getWidth() * 0.30f,
                        getHeight() * 0.68f,
                        getWidth() * 0.70f,
                        getHeight() * 0.81f
                )) {
                    screen = SCREEN_HOME;
                    return true;
                }

                return true;
            }

            if (screen == SCREEN_PAUSE) {
                if (inside(
                        x, y,
                        getWidth() * 0.32f,
                        getHeight() * 0.50f,
                        getWidth() * 0.68f,
                        getHeight() * 0.63f
                )) {
                    screen = SCREEN_PLAY;
                }

                return true;
            }



            if (screen != SCREEN_PLAY) {
                return true;
            }

            // Plant cards.
            if (y >= getHeight() * 0.07f &&
                    y <= getHeight() * 0.17f) {

                for (int i = 0; i < 6; i++) {
                    float x1 =
                            getWidth() * 0.01f +
                            i * getWidth() * 0.075f;

                    float x2 =
                            x1 + getWidth() * 0.065f;

                    if (x >= x1 && x <= x2) {
                        int[] ids = {
                                SUNFLOWER,
                                PEASHOOTER,
                                GIGANUT,
                                CHOMPER,
                                REPEATER,
                                MINE
                        };

                        if (plantUnlocked(ids[i])) {
                            selectedPlant = ids[i];
                            tool = TOOL_NONE;
                        }

                        return true;
                    }
                }

                float sx = getWidth() * 0.465f;

                if (x >= sx &&
                        x <= sx + getWidth() * 0.065f) {
                    tool = TOOL_SHOVEL;
                    return true;
                }
            }

            // Plant food button: top-right PF.
            if (x > getWidth() * 0.84f &&
                    y < getHeight() * 0.08f) {
                if (plantFood > 0) {
                    tool = TOOL_PLANT_FOOD;
                }
                return true;
            }

            // Board.
            if (x >= left &&
                    x < left + COLS * cellW &&
                    y >= top &&
                    y < top + ROWS * cellH) {

                int col =
                        (int)((x - left) / cellW);

                int row =
                        (int)((y - top) / cellH);

                if (row < 0 || row >= ROWS ||
                        col < 0 || col >= COLS) {
                    return true;
                }

                if (tool == TOOL_SHOVEL) {
                    plants[row][col] = null;
                    tool = TOOL_NONE;
                    return true;
                }

                if (tool == TOOL_PLANT_FOOD) {
                    if (plants[row][col] != null &&
                            plantFood > 0) {

                        activatePlantFood(
                                plants[row][col]
                        );

                        plantFood--;
                    }

                    tool = TOOL_NONE;
                    return true;
                }

                if (plantUnlocked(selectedPlant) &&
                        plants[row][col] == null) {

                    int cost =
                            plantCost(selectedPlant);

                    if (sun >= cost) {
                        sun -= cost;

                        plants[row][col] =
                                new Plant(
                                        selectedPlant,
                                        row,
                                        col
                                );

                        beep(ToneGenerator.TONE_PROP_BEEP);
                    }
                }
            }

            return true;
        }

        private void activatePlantFood(Plant pl) {
            if (pl.type == SUNFLOWER) {
                sun += 125;
            } else if (pl.type == PEASHOOTER) {
                for (int i = 0; i < 3; i++) {
                    firePea(
                            pl.row,
                            pl.col,
                            70,
                            false
                    );
                }
            } else if (pl.type == GIGANUT) {
                pl.hp = pl.maxHp;
            } else if (pl.type == CHOMPER) {
                Zombie z =
                        nearestZombieInRange(
                                pl.row,
                                pl.col
                        );

                if (z != null) {
                    z.hp = 0;
                }
            } else if (pl.type == REPEATER) {
                for (int i = 0; i < 6; i++) {
                    firePea(
                            pl.row,
                            pl.col,
                            55,
                            false
                    );
                }
            } else if (pl.type == MINE) {
                for (Zombie z : zombies) {
                    if (z.row == pl.row) {
                        z.hp = 0;
                    }
                }
            }
        }

        private boolean inside(
                float x,
                float y,
                float x1,
                float y1,
                float x2,
                float y2
        ) {
            return x >= x1 &&
                    x <= x2 &&
                    y >= y1 &&
                    y <= y2;
        }

        @Override
        public boolean dispatchKeyEventPreIme(KeyEvent event) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_BACK &&
                    event.getAction() == KeyEvent.ACTION_UP) {

                if (screen == SCREEN_PLAY) {
                    screen = SCREEN_PAUSE;
                    return true;
                }

                if (screen == SCREEN_PAUSE ||
                        screen == SCREEN_LEVELS ||
                        screen == SCREEN_WIN ||
                        screen == SCREEN_LOSE) {

                    screen = SCREEN_HOME;
                    return true;
                }
            }

            return super.dispatchKeyEventPreIme(event);
        }

        private class Plant {
            final int type;
            final int row;
            final int col;

            int hp;
            final int maxHp;

            long lastAction;
            long secondShotAt;
            long armedAt;

            Plant(int type, int row, int col) {
                this.type = type;
                this.row = row;
                this.col = col;

                if (type == GIGANUT) {
                    maxHp = 1800;
                } else if (type == MINE) {
                    maxHp = 120;
                } else {
                    maxHp = 500;
                }

                hp = maxHp;

                long now = System.currentTimeMillis();

                lastAction = now;
                secondShotAt = 0;

                armedAt =
                        type == MINE
                                ? now + 3000
                                : Long.MAX_VALUE;
            }
        }

        private class Zombie {
            float x;
            float y;

            final int row;
            final int type;
            final boolean boss;

            float hp;
            final float maxHp;

            float speed;
            int damage;

            long lastAttack;
            long slowUntil;
            long bossShotAt;

            Zombie(int row, int type, boolean boss) {
                this.row = row;
                this.type = type;
                this.boss = boss;

                x =
                        left +
                        COLS * cellW +
                        cellW;

                y =
                        top +
                        row * cellH +
                        cellH * 0.16f;

                if (boss) {
                    maxHp = 5000;
                    speed = cellW * 0.008f;
                    damage = 45;
                } else if (type == 1) {
                    maxHp = 1000 + level * 100;
                    speed = cellW * 0.010f;
                    damage = 32;
                } else if (type == 2) {
                    maxHp = 450 + level * 60;
                    speed = cellW * 0.024f;
                    damage = 18;
                } else {
                    maxHp = 650 + level * 75;
                    speed = cellW * 0.014f;
                    damage = 24;
                }

                hp = maxHp;

                long now = System.currentTimeMillis();
                bossShotAt = now;
            }
        }

        private class Pea {
            float x;
            final float y;

            final int row;
            final int damage;
            final boolean ice;

            Pea(
                    float x,
                    float y,
                    int row,
                    int damage,
                    boolean ice
            ) {
                this.x = x;
                this.y = y;
                this.row = row;
                this.damage = damage;
                this.ice = ice;
            }
        }

        private class SunDrop {
            final float x;
            final float y;
            boolean collected;

            SunDrop(float x, float y) {
                this.x = x;
                this.y = y;
            }
        }

        private class Mower {
            final int row;

            float x;
            boolean used;
            boolean active;

            Mower(int row) {
                this.row = row;
                this.x =
                        left +
                        cellW * 0.45f;
            }
        }
    }
                    }
              
