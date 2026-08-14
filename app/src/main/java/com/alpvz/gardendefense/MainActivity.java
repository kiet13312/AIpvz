package com.alpvz.gardendefense;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ImageView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout battle = new LinearLayout(this);
        battle.setOrientation(LinearLayout.HORIZONTAL);
        battle.setGravity(Gravity.CENTER);
        battle.setBackgroundColor(Color.rgb(30, 100, 30));
        battle.setPadding(20, 20, 20, 20);

        ImageView sunflower = new ImageView(this);
        sunflower.setImageResource(R.drawable.sunflower);
        sunflower.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        ImageView peashooter = new ImageView(this);
        peashooter.setImageResource(R.drawable.peashooter);
        peashooter.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        ImageView wallnut = new ImageView(this);
        wallnut.setImageResource(R.drawable.wallnut);
        wallnut.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        int size = 300;

        battle.addView(sunflower, new LinearLayout.LayoutParams(size, size));
        battle.addView(peashooter, new LinearLayout.LayoutParams(size, size));
        battle.addView(wallnut, new LinearLayout.LayoutParams(size, size));

        setContentView(battle);
    }
}
