package com.alpvz.gardendefense;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Màn hình trận
        LinearLayout battle = new LinearLayout(this);
        battle.setOrientation(LinearLayout.VERTICAL);
        battle.setGravity(Gravity.CENTER);
        battle.setPadding(20, 20, 20, 20);
        battle.setBackgroundColor(Color.rgb(105, 170, 75));

        // Tiêu đề
        TextView title = new TextView(this);
        title.setText("GARDEN DEFENSE");
        title.setTextColor(Color.WHITE);
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER);

        battle.addView(title, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                80
        ));

        // Khu vực 3 cây
        LinearLayout plants = new LinearLayout(this);
        plants.setOrientation(LinearLayout.HORIZONTAL);
        plants.setGravity(Gravity.CENTER);

        plants.addView(createPlant(R.drawable.sun, "SUNFLOWER"));
        plants.addView(createPlant(R.drawable.peashoot, "PEASHOOTER"));
        plants.addView(createPlant(R.drawable.giganut, "GIGANUT"));

        battle.addView(plants, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                500
        ));

        setContentView(battle);
    }

    private LinearLayout createPlant(int imageId, String name) {

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(15, 15, 15, 15);

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.argb(180, 255, 255, 255));
        background.setCornerRadius(25);
        box.setBackground(background);

        ImageView image = new ImageView(this);
        image.setImageResource(imageId);
        image.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        box.addView(image, new LinearLayout.LayoutParams(
                260,
                300
        ));

        TextView text = new TextView(this);
        text.setText(name);
        text.setTextColor(Color.rgb(30, 80, 30));
        text.setTextSize(20);
        text.setGravity(Gravity.CENTER);

        box.addView(text, new LinearLayout.LayoutParams(
                260,
                60
        ));

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(290, 380);

        params.setMargins(10, 0, 10, 0);

        box.setLayoutParams(params);

        return box;
    }
    }
