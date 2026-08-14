package com.alpvz.gardendefense;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import java.util.*;

public class MainActivity extends Activity {

    GameView game;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        game = new GameView();
        setContentView(game);
    }

    class GameView extends View {

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        Bitmap sunflower, peashooter, giganut;

        int selected = 0;
        int rows = 5;
        int cols = 9;
        int cellW, cellH;

        ArrayList<Plant> plants = new ArrayList<>();
        ArrayList<Pea> peas = new ArrayList<>();
        ArrayList<Zombie> zombies = new ArrayList<>();

        long lastSpawn = 0;
        long lastShot = 0;

        GameView() {
            super(MainActivity.this);

            sunflower = loadImage("sunflower");
            peashooter = loadImage("peashooter");
            giganut = loadImage("giganut");

            zombies.add(new Zombie(8, 1, 180));
            zombies.add(new Zombie(7, 3, 180));
        }

        Bitmap loadImage(String name) {
            int id = getResources().getIdentifier(
                    name,
                    "drawable",
                    getPackageName()
            );
            return id == 0 ? null : BitmapFactory.decodeResource(
                    getResources(), id
            );
        }

        @Override
        protected void onDraw(Canvas c) {
            super.onDraw(c);

            c.drawColor(Color.rgb(100, 180, 70));

            cellW = getWidth() / cols;
            cellH = (getHeight() - 230) / rows;

            drawMenu(c);
            drawBoard(c);
            drawPlants(c);
            drawPeas(c);
            drawZombies(c);

            invalidate();
        }

        void drawMenu(Canvas c) {
            p.setColor(Color.WHITE);
            p.setTextSize(30);
            p.setTypeface(Typeface.DEFAULT_BOLD);
            c.drawText("GARDEN DEFENSE", 30, 45, p);

            drawCard(c, 20, 65, 150, 190, "SUNFLOW
