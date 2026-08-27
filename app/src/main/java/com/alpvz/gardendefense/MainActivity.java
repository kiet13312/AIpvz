package com.alpvz.gardendefense;

import android.app.Activity;
import android.os.Bundle;
import android.content.pm.ActivityInfo;
import android.graphics.*;
import android.media.*;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.widget.VideoView;
import android.widget.Button;
import android.widget.FrameLayout;
import java.util.*;

public class MainActivity extends Activity {

    private GardenGame game;
    private FrameLayout root;
    private VideoView winVideo;
    private Button continueBtn;

    private final Handler videoHandler =
            new Handler(Looper.getMainLooper());

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setRequestedOrientation(
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        root = new FrameLayout(this);
        game = new GardenGame();

        root.addView(
                game,
                new FrameLayout.LayoutParams(-1, -1));

        setContentView(root);
    }

    @Override
    protected void onPause() {
        if (game != null)
            game.save();

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        closeWinVideo();

        if (game != null)
            game.releaseSounds();

        super.onDestroy();
    }

    void showWinVideo() {
        if (root == null || winVideo != null)
            return;

        int id = getResources().getIdentifier(
                "win",
                "raw",
                getPackageName());

        if (id == 0)
            return;

        winVideo = new VideoView(this);
        winVideo.setBackgroundColor(Color.BLACK);

        winVideo.setVideoURI(
                Uri.parse(
                        "android.resource://" +
                        getPackageName() +
                        "/" + id));

        root.addView(
                winVideo,
                new FrameLayout.LayoutParams(-1, -1));

        continueBtn = new Button(this);
        continueBtn.setText("CHƠI TIẾP");
        continueBtn.setTextSize(16);
        continueBtn.setVisibility(View.GONE);

        FrameLayout.LayoutParams bp =
                new FrameLayout.LayoutParams(
                        260,
                        100,
                        Gravity.RIGHT | Gravity.BOTTOM);

        bp.setMargins(0, 0, 24, 24);
        root.addView(continueBtn, bp);

        winVideo.setOnPreparedListener(mp -> {
            mp.setLooping(false);
            winVideo.start();
        });

        winVideo.setOnCompletionListener(mp -> {
            if (continueBtn != null)
                continueBtn.setVisibility(View.VISIBLE);
        });

        videoHandler.postDelayed(() -> {
            if (continueBtn != null)
                continueBtn.setVisibility(View.VISIBLE);
        }, 3000);

        continueBtn.setOnClickListener(v -> {
            closeWinVideo();

            if (game.level < 9) {
                game.startLevel(game.level + 1);
            } else {
                game.screen = GardenGame.HOME;
                game.invalidate();
            }
        });
    }

    void closeWinVideo() {
        videoHandler.removeCallbacksAndMessages(null);

        if (winVideo != null) {
            try {
                winVideo.stopPlayback();
            } catch (Exception ignored) {}

            root.removeView(winVideo);
            winVideo = null;
        }

        if (continueBtn != null) {
            root.removeView(continueBtn);
            continueBtn = null;
        }
    }

    @Override
    public void onBackPressed() {

        if (game == null) {
            super.onBackPressed();
            return;
        }

        if (game.screen == GardenGame.PLAY) {
            game.screen = GardenGame.PAUSE;

        } else if (game.screen == GardenGame.PAUSE) {
            game.screen = GardenGame.PLAY;

        } else if (game.screen != GardenGame.HOME) {
            closeWinVideo();
            game.screen = GardenGame.HOME;

        } else {
            super.onBackPressed();
            return;
        }

        game.invalidate();
    }


    public class GardenGame extends View {

        static final int ROWS = 5;
        static final int COLS = 9;

        static final int SUNFLOWER = 1;
        static final int PEASHOOTER = 2;
        static final int GIGANUT = 3;
        static final int CHOMPER = 4;
        static final int REPEATER = 5;
        static final int MINE = 6;
        static final int BINU = 7;

        static final int HOME = 0;
        static final int LEVELS = 1;
        static final int PLAY = 2;
        static final int PAUSE = 3;
        static final int WIN = 4;
        static final int LOSE = 5;

        static final int NONE = 0;
        static final int SHOVEL = 1;
        static final int FOOD = 2;

        final Paint p =
                new Paint(Paint.ANTI_ALIAS_FLAG);

        final RectF drawRect = new RectF();
        final Random rnd = new Random();

        final Plant[][] plants =
                new Plant[ROWS][COLS];

        final ArrayList<Zombie> zombies =
                new ArrayList<>();

        final ArrayList<Pea> peas =
                new ArrayList<>();

        final ArrayList<SunDrop> suns =
                new ArrayList<>();

        final Mower[] mowers =
                new Mower[ROWS];


        Bitmap sunImg;
        Bitmap peaImg;
        Bitmap gigaImg;
        Bitmap chomperImg;
        Bitmap repeaterImg;
        Bitmap mineImg;

        Bitmap peaFoodImg;
        Bitmap repeaterFoodImg;
        Bitmap gigaFoodImg;

        Bitmap zombieImg;
        Bitmap bulletImg;

        Bitmap binuImg;
        Bitmap binu1Img;
        Bitmap binu2Img;
        Bitmap binu3Img;
        Bitmap binu4Img;


        MediaPlayer foodSound;
        MediaPlayer binuSound1;
        MediaPlayer binuSound2;


        float left;
        float top;
        float cw;
        float ch;


        int screen = HOME;
        int level = 1;
        int unlocked = 1;

        int selected = PEASHOOTER;
        int tool = NONE;

        int sun = 500;
        int coins = 99999;
        int food = 10000;

        int killed;
        int total;
        int spawned;


        long last;
        long spawnClock;


        boolean speed2 = false;


        // BINU
        boolean binuJump = false;
        int binuFrame = 0;
        int binuRow = -1;
        int binuCol = -1;
        long binuClock = 0;


        GardenGame() {

            super(MainActivity.this);

            setFocusable(true);


            android.content.SharedPreferences sp =
                    getSharedPreferences(
                            "garden_defense",
                            MODE_PRIVATE);


            level =
                    sp.getInt("level", 1);

            unlocked =
                    sp.getInt("unlocked", 1);

            coins =
                    sp.getInt("coins", 99999);

            food =
                    sp.getInt("food", 10000);


            for (int r = 0;
                 r < ROWS;
                 r++) {

                mowers[r] =
                        new Mower(r);
            }


            loadImages();
            initSounds();


            last =
                    System.currentTimeMillis();

            spawnClock = last;
        }


        Bitmap img(String n) {

            try {

                int id =
                        getResources().getIdentifier(
                                n,
                                "drawable",
                                getPackageName());

                if (id == 0)
                    return null;


                BitmapFactory.Options o =
                        new BitmapFactory.Options();

                o.inScaled = false;

                o.inPreferredConfig =
                        Bitmap.Config.ARGB_8888;


                return BitmapFactory.decodeResource(
                        getResources(),
                        id,
                        o);

            } catch (Throwable e) {
                return null;
            }
        }


        void loadImages() {

            sunImg = img("sun");
            peaImg = img("peashoot");
            gigaImg = img("giganut");
            chomperImg = img("chomper");
            repeaterImg = img("repeater");
            mineImg = img("min");

            peaFoodImg =
                    img("peashootplantfood");

            repeaterFoodImg =
                    img("repeaterplantfood");

            gigaFoodImg =
                    img("giganutplantfood");

            zombieImg =
                    img("zomplatz");

            bulletImg =
                    img("gigapea");


            binuImg =
                    img("binu");

            binu1Img =
                    img("binu1");

            binu2Img =
                    img("binu2");

            binu3Img =
                    img("binu3");

            binu4Img =
                    img("binu4");
        }


        MediaPlayer makeSound(String n) {

            try {

                int id =
                        getResources().getIdentifier(
                                n,
                                "raw",
                                getPackageName());

                if (id == 0)
                    return null;

                return MediaPlayer.create(
                        MainActivity.this,
                        id);

            } catch (Exception e) {
                return null;
            }
        }


        void initSounds() {

            foodSound =
                    makeSound(
                            "peashootplantfood");

            binuSound1 =
                    makeSound(
                            "binusound1");

            binuSound2 =
                    makeSound(
                            "binusound2");
        }


        void playBinuSound1() {

            try {

                if (binuSound1 != null) {

                    binuSound1.seekTo(0);
                    binuSound1.start();
                }

            } catch (Exception ignored) {}
        }


        void playBinuSound2() {

            try {

                if (binuSound2 != null &&
                    !binuSound2.isPlaying()) {

                    binuSound2.seekTo(0);
                    binuSound2.start();
                }

            } catch (Exception ignored) {}
        }


        @Override
        protected void onSizeChanged(
                int w,
                int h,
                int ow,
                int oh) {

            left = w * .18f;
            top = h * .25f;

            cw = w * .78f / COLS;
            ch = h * .70f / ROWS;


            for (int r = 0;
                 r < ROWS;
                 r++) {

                if (mowers[r] != null)
                    mowers[r].x =
                            left - cw * .4f;
            }
        }


        @Override
        protected void onDraw(Canvas c) {

            if (screen == HOME) {
                drawHome(c);
                return;
            }

            if (screen == LEVELS) {
                drawLevels(c);
                return;
            }


            drawGame(c);


            if (screen == PAUSE)
                overlay(
                        c,
                        "TẠM DỪNG",
                        "TIẾP TỤC",
                        "CHƠI LẠI",
                        "THOÁT");


            if (screen == WIN)
                overlay(
                        c,
                        "CHIẾN THẮNG!",
                        "MÀN TIẾP",
                        "CHƠI LẠI",
                        "VỀ MENU");


            if (screen == LOSE)
                overlay(
                        c,
                        "ZOMBIE ĐÃ VÀO NHÀ!",
                        "CHƠI LẠI",
                        "",
                        "VỀ MENU");


            if (screen == PLAY) {
                update();
                postInvalidateDelayed(40);
            }
        }


        RectF dr(
                float l,
                float t,
                float r,
                float b) {

            drawRect.set(
                    l,t,r,b);

            return drawRect;
        }


        void text(
                Canvas c,
                String s,
                float x,
                float y,
                float size,
                int color,
                Paint.Align a) {

            p.setTextSize(size);
            p.setColor(color);
            p.setTextAlign(a);

            c.drawText(
                    s,
                    x,
                    y,
                    p);
        }


        void button(
                Canvas c,
                float x1,
                float y1,
                float x2,
                float y2,
                String s) {

            p.setColor(
                    Color.rgb(
                            50,100,55));

            c.drawRoundRect(
                    getWidth()*x1,
                    getHeight()*y1,
                    getWidth()*x2,
                    getHeight()*y2,
                    12,12,p);

            text(
                    c,
                    s,
                    getWidth()*(x1+x2)/2,
                    getHeight()*(y1+y2)/2+8,
                    20,
                    Color.WHITE,
                    Paint.Align.CENTER);
        }


        void drawHome(Canvas c) {

            c.drawColor(
                    Color.rgb(25,65,30));

            text(
                    c,
                    "GARDEN DEFENSE",
                    getWidth()/2f,
                    getHeight()*.25f,
                    42,
                    Color.WHITE,
                    Paint.Align.CENTER);

            text(
                    c,
                    "☀ "+sun+
                    "  XU "+coins+
                    "  PF "+food,
                    getWidth()/2f,
                    getHeight()*.34f,
                    22,
                    Color.YELLOW,
                    Paint.Align.CENTER);

            button(
                    c,.30f,.44f,.70f,.55f,
                    "CHƠI");

            button(
                    c,.30f,.60f,.70f,.71f,
                    "CHỌN MÀN");
        }


        void drawLevels(Canvas c) {

            c.drawColor(
                    Color.rgb(20,55,25));

            text(
                    c,
                    "CHỌN MÀN",
                    getWidth()/2f,
                    getHeight()*.10f,
                    32,
                    Color.WHITE,
                    Paint.Align.CENTER);


            for(int i=1;i<=9;i++){

                int col=(i-1)%3;
                int row=(i-1)/3;

                float x=.18f+col*.22f;
                float y=.18f+row*.19f;

                p.setColor(
                        i<=unlocked
                                ? Color.rgb(65,145,70)
                                : Color.DKGRAY);

                c.drawRoundRect(
                        getWidth()*x,
                        getHeight()*y,
                        getWidth()*(x+.17f),
                        getHeight()*(y+.13f),
                        14,14,p);

                text(
                        c,
                        i<=unlocked
                                ? "MÀN "+i
                                : "KHÓA",
                        getWidth()*(x+.085f),
                        getHeight()*(y+.082f),
                        19,
                        Color.WHITE,
                        Paint.Align.CENTER);
            }


            button(
                    c,
                    .04f,.84f,.20f,.94f,
                    "QUAY LẠI");
        }


        void drawGame(Canvas c) {

            c.drawColor(
                    Color.rgb(92,155,70));

            p.setColor(
                    Color.rgb(38,78,40));

            c.drawRect(
                    0,0,
                    getWidth(),
                    top,p);


            text(
                    c,
                    "☀ "+sun,
                    14,34,22,
                    Color.YELLOW,
                    Paint.Align.LEFT);

            text(
                    c,
                    "MÀN "+level,
                    getWidth()*.25f,
                    34,20,
                    Color.WHITE,
                    Paint.Align.LEFT);

            text(
                    c,
                    "ZOM "+killed+"/"+total,
                    getWidth()*.45f,
                    34,19,
                    Color.WHITE,
                    Paint.Align.LEFT);

            text(
                    c,
                    "XU "+coins,
                    getWidth()*.65f,
                    34,19,
                    Color.YELLOW,
                    Paint.Align.LEFT);

            text(
                    c,
                    "PF "+food,
                    getWidth()*.84f,
                    34,19,
                    Color.WHITE,
                    Paint.Align.LEFT);


            drawCards(c);
            drawBoard(c);
            drawPlants(c);
            drawPeas(c);
            drawZombies(c);
            drawSuns(c);
            drawMowers(c);


            button(
                    c,.64f,.075f,.75f,.15f,
                    "MUA PF");

            button(
                    c,.82f,.075f,.90f,.15f,
                    "Ⅱ");

            button(
                    c,.91f,.075f,.99f,.15f,
                    speed2?"×2":"▶");
        }


        void drawCards(Canvas c) {

            int[] t={
                    SUNFLOWER,
                    PEASHOOTER,
                    GIGANUT,
                    CHOMPER,
                    REPEATER,
                    MINE,
                    BINU
            };


            for(int i=0;i<t.length;i++){

                float x=
                        getWidth()*
                        (.005f+i*.061f);

                float y=
                        getHeight()*.075f;

                float w=
                        getWidth()*.055f;

                float h=
                        getHeight()*.09f;


                p.setColor(
                        selected==t[i]&&
                        tool==NONE
                                ?Color.YELLOW
                                :Color.rgb(
                                        45,85,45));


                c.drawRoundRect(
                        x,y,
                        x+w,y+h,
                        8,8,p);


                if(unlocked(t[i])){

                    drawPlant(
                            c,
                            t[i],
                            x+w/2,
                            y+h/2,
                            Math.min(w,h)*.72f,
                            null);

                }else{

                    text(
                            c,
                            "LOCK",
                            x+w/2,
                            y+h*.62f,
                            10,
                            Color.LTGRAY,
                            Paint.Align.CENTER);
                }
            }


            p.setColor(
                    tool==SHOVEL
                            ?Color.YELLOW
                            :Color.rgb(55,80,55));

            c.drawRoundRect(
                    getWidth()*.435f,
                    getHeight()*.075f,
                    getWidth()*.495f,
                    getHeight()*.15f,
                    8,8,p);

            text(
                    c,
                    "XẺNG",
                    getWidth()*.465f,
                    getHeight()*.122f,
                    void drawBoard(Canvas c){

            for(int r=0;r<ROWS;r++)
                for(int col=0;col<COLS;col++){

                    p.setColor(
                            activeRow(r)
                                    ?((r+col)%2==0
                                        ?Color.rgb(103,166,78)
                                        :Color.rgb(91,153,67))
                                    :Color.rgb(72,110,62));

                    c.drawRect(
                            left+col*cw,
                            top+r*ch,
                            left+(col+1)*cw,
                            top+(r+1)*ch,
                            p);
                }


            p.setColor(
                    Color.rgb(135,95,55));

            c.drawRect(
                    0,
                    top,
                    left,
                    top+ROWS*ch,
                    p);
        }


        void drawPlants(Canvas c){

            for(int r=0;r<ROWS;r++)
                for(int col=0;col<COLS;col++){

                    Plant a=plants[r][col];

                    if(a==null)
                        continue;


                    float x=
                            left+col*cw+cw/2;

                    float y=
                            top+r*ch+ch/2;


                    drawPlant(
                            c,
                            a.type,
                            x,
                            y,
                            Math.min(cw,ch)*.74f,
                            a);


                    hp(
                            c,
                            x-cw*.3f,
                            y+ch*.32f,
                            cw*.6f,
                            a.hp,
                            a.maxHp);
                }


            // Binu vẫn hiện trong lúc nhảy.
            if(binuJump&&
               binuRow>=0&&
               binuCol>=0){

                float x=
                        left+
                        binuCol*cw+
                        cw/2;

                float y=
                        top+
                        binuRow*ch+
                        ch/2;


                drawPlant(
                        c,
                        BINU,
                        x,
                        y,
                        Math.min(cw,ch)*.82f,
                        null);
            }
        }


        void drawPlant(
                Canvas c,
                int type,
                float x,
                float y,
                float size,
                Plant a){

            Bitmap b=null;


            // PF chỉ dành cho các cây được phép.
            if(a!=null&&a.foodUsed){

                if(type==PEASHOOTER)
                    b=peaFoodImg;

                else if(type==REPEATER)
                    b=repeaterFoodImg;

                else if(type==GIGANUT)
                    b=gigaFoodImg;
            }


            if(b==null){

                if(type==SUNFLOWER)
                    b=sunImg;

                else if(type==PEASHOOTER)
                    b=peaImg;

                else if(type==GIGANUT)
                    b=gigaImg;

                else if(type==CHOMPER)
                    b=chomperImg;

                else if(type==REPEATER)
                    b=repeaterImg;

                else if(type==MINE)
                    b=mineImg;

                else if(type==BINU){

                    if(!binuJump)
                        b=binuImg;

                    else if(binuFrame==1)
                        b=binu1Img;

                    else if(binuFrame==2)
                        b=binu2Img;

                    else if(binuFrame==3)
                        b=binu3Img;

                    else
                        b=binu4Img;
                }
            }


            if(b!=null){

                c.drawBitmap(
                        b,
                        null,
                        dr(
                                x-size/2,
                                y-size/2,
                                x+size/2,
                                y+size/2),
                        p);

                return;
            }


            p.setColor(
                    type==GIGANUT
                            ?Color.rgb(145,95,55)
                            :Color.rgb(55,175,70));

            c.drawCircle(
                    x,y,size*.35f,p);
        }


        void drawZombies(Canvas c){

            for(Zombie z:zombies){

                if(z.hp<=0)
                    continue;


                float w=
                        z.boss
                                ?cw*1.35f
                                :z.giga
                                ?cw*.95f
                                :cw*.68f;


                float h=
                        z.boss
                                ?ch*1.35f
                                :z.giga
                                ?ch*1.08f
                                :ch*.82f;


                if(zombieImg!=null){

                    c.drawBitmap(
                            zombieImg,
                            null,
                            dr(
                                    z.x-w/2,
                                    z.y-h*.55f,
                                    z.x+w/2,
                                    z.y+h*.45f),
                            p);

                }else{

                    p.setColor(
                            Color.DKGRAY);

                    c.drawOval(
                            dr(
                                    z.x-w/2,
                                    z.y-h/2,
                                    z.x+w/2,
                                    z.y+h/2),
                            p);
                }


                hp(
                        c,
                        z.x-w/2,
                        z.y-h*.65f,
                        w,
                        z.hp,
                        z.maxHp);
            }
        }


        void drawPeas(Canvas c){

            for(Pea q:peas){

                float s=
                        q.big?18:10;


                if(bulletImg!=null){

                    c.drawBitmap(
                            bulletImg,
                            null,
                            dr(
                                    q.x-s,
                                    q.y-s,
                                    q.x+s,
                                    q.y+s),
                            p);

                }else{

                    p.setColor(
                            Color.GREEN);

                    c.drawCircle(
                            q.x,
                            q.y,
                            s*.7f,
                            p);
                }
            }
        }


        void drawSuns(Canvas c){

            for(SunDrop s:suns){

                if(sunImg!=null){

                    c.drawBitmap(
                            sunImg,
                            null,
                            dr(
                                    s.x-18,
                                    s.y-18,
                                    s.x+18,
                                    s.y+18),
                            p);

                }else{

                    p.setColor(
                            Color.YELLOW);

                    c.drawCircle(
                            s.x,
                            s.y,
                            16,
                            p);
                }
            }
        }


        void drawMowers(Canvas c){

            for(Mower m:mowers){

                float y=
                        top+
                        m.row*ch+
                        ch*.72f;


                p.setColor(
                        m.used
                                ?Color.DKGRAY
                                :Color.rgb(
                                        190,70,40));


                c.drawRoundRect(
                        m.x-cw*.3f,
                        y-ch*.18f,
                        m.x+cw*.3f,
                        y,
                        8,8,p);
            }
        }


        void hp(
                Canvas c,
                float x,
                float y,
                float w,
                float value,
                float max){

            p.setColor(
                    Color.DKGRAY);

            c.drawRect(
                    x,y,
                    x+w,y+6,
                    p);


            if(max>0){

                p.setColor(
                        Color.GREEN);

                c.drawRect(
                        x,y,
                        x+w*
                                Math.max(
                                        0,
                                        Math.min(
                                                1,
                                                value/max)),
                        y+6,
                        p);
            }
        }


        // =========================
        // UPDATE
        // =========================

        void update(){

            if(screen!=PLAY)
                return;


            long now=
                    System.currentTimeMillis();


            float dt=
                    Math.min(
                            .08f,
                            (now-last)/1000f)
                    *(speed2?2:1);


            last=now;


            updateBinu(now);
            checkBinu(now);

            updatePlants(now);
            updatePeas(dt);
            updateZombies(now,dt);
            updateMowers(dt);

            removeDead();


            if(spawned<total&&
               now-spawnClock>=
                       spawnDelay()){

                spawnZombie();

                spawned++;

                spawnClock=now;
            }


            if(spawned>=total&&
               zombies.isEmpty()&&
               killed>=total){

                winLevel();
            }
        }


        // =========================
        // BINU
        // =========================

        void updateBinu(long now){

            if(!binuJump)
                return;


            if(now-binuClock>=85){

                binuClock=now;

                binuFrame++;


                // Đến frame 4 thì đè.
                if(binuFrame>=4){

                    smashBinu();


                    binuJump=false;

                    binuFrame=0;

                    binuRow=-1;
                    binuCol=-1;

                    // Không stop sound2.
                    // Sound chạy đến hết file.
                }
            }
        }


        void checkBinu(long now){

            if(binuJump)
                return;


            for(int r=0;r<ROWS;r++){

                for(int col=0;
                    col<COLS;
                    col++){

                    Plant a=
                            plants[r][col];


                    if(a==null||
                       a.type!=BINU)
                        continue;


                    float bx=
                            left+
                            col*cw+
                            cw/2f;


                    for(Zombie z:zombies){

                        if(z.hp<=0||
                           z.row!=r)
                            continue;


                        // Zombie đến 1 ô trước Binu.
                        if(z.x>bx&&
                           z.x<=bx+cw*1.05f){

                            binuJump=true;

                            binuFrame=1;

                            binuRow=r;
                            binuCol=col;

                            binuClock=now;


                            // Sau khi nhảy Binu biến mất.
                            plants[r][col]=null;


                            playBinuSound2();

                            return;
                        }
                    }
                }
            }
        }


        void smashBinu(){

            if(binuRow<0||
               binuCol<0)
                return;


            // Tâm vùng Binu đáp xuống.
            float cx=
                    left+
                    binuCol*cw+
                    cw/2f+
                    cw;


            float range=
                    cw*.90f;


            for(Zombie z:zombies){

                if(z.hp<=0||
                   z.row!=binuRow)
                    continue;


                if(Math.abs(
                        z.x-cx)
                        <=range){

                    // Boss: mất 50% HP hiện tại.
                    if(z.boss){

                        z.hp=
                                Math.max(
                                        1,
                                        (int)
                                        (z.hp*.5f));

                    }else{

                        // Zombie thường chết.
                        z.hp=0;
                    }
                }
            }
        }


        // =========================
        // PLANTS
        // =========================

        void updatePlants(long now){

            for(int r=0;r<ROWS;r++){

                for(int col=0;
                    col<COLS;
                    col++){

                    Plant a=
                            plants[r][col];

                    if(a==null)
                        continue;


                    if(a.foodUsed&&
                       a.type!=GIGANUT&&
                       now>=a.foodUntil){

                        a.foodUsed=false;
                        a.foodUntil=0;
                    }


                    boolean f=
                            a.foodUsed&&
                            now<a.foodUntil;


                    if(a.type==SUNFLOWER){

                        long cd=
                                f?1800:7000;


                        if(now-a.last>=cd){

                            suns.add(
                                    new SunDrop(
                                            left+
                                            col*cw+
                                            cw/2,
                                            top+
                                            r*ch+
                                            ch*.2f));

                            a.last=now;
                        }


                    }else if(
                            a.type==PEASHOOTER){

                        long cd=
                                f?700:1500;


                        if(now-a.last>=cd&&
                           rowHasZombie(r)){

                            fire(
                                    r,
                                    col,
                                    f?45:30,
                                    f);

                            a.last=now;
                        }


                    }else if(
                            a.type==REPEATER){

                        long cd=
                                f?700:1500;


                        if(now-a.last>=cd&&
                           rowHasZombie(r)){

                            fire(
                                    r,
                                    col,
                                    f?45:30,
                                    f);

                            a.secondShot=
                                    now+500;

                            a.last=now;
                        }


                    }else if(
                            a.type==CHOMPER){

                        if(now-a.last>=3500){

                            Zombie z=
                                    nearest(
                                            r,
                                            col,
                                            cw*1.5f);

                            if(z!=null){

                                z.hp=0;
                                a.last=now;
                            }
                        }


                    }else if(
                            a.type==MINE){

                        if(!a.armed&&
                           now>=a.armAt)

                            a.armed=true;


                        if(a.armed){

                            Zombie z=
                                    onCell(
                                            r,
                                            col);

                            if(z!=null)
                                explodeMine(
                                        r,col,a);
                        }
                    }


                    if(a.secondShot>0&&
                       now>=a.secondShot){

                        fire(
                                r,
                                col,
                                f?45:30,
                                f);

                        a.secondShot=0;
                    }
                }
            }
        }


        void explodeMine(
                int r,
                int col,
                Plant mine){

            float cx=
                    left+
                    col*cw+
                    cw/2f;


            for(Zombie z:zombies){

                if(z.row>=
                        Math.max(0,r-1)&&
                   z.row<=
                        Math.min(
                                ROWS-1,
                                r+1)&&
                   Math.abs(
                           z.x-cx)
                        <=cw*1.55f){

                    z.hp-=1800;
                }
            }


            mine.hp=0;
        }


        void fire(
                int r,
                int col,
                int dmg,
                boolean big){

            peas.add(
                    new Pea(
                            left+
                            col*cw+
                            cw*.56f,
                            top+
                            r*ch+
                            ch*.5f,
                            r,
                            dmg,
                            big));
        }


        // =========================
        // PEAS
        // =========================

        void updatePeas(float dt){

            Iterator<Pea> it=
                    peas.iterator();


            while(it.hasNext()){

                Pea q=it.next();


                q.x+=
                        (q.enemy?-1:1)*
                        cw*8.5f*
                        dt;


                if(q.enemy){

                    int col=
                            (int)
                            ((q.x-left)/cw);


                    if(col>=0&&
                       col<COLS){

                        Plant a=
                                plants[
                                        q.row
                                ][col];


                        if(a!=null){

                            a.hp-=
                                    q.damage;

                            it.remove();

                            continue;
                        }
                    }


                    if(q.x<
                       left-cw)

                        it.remove();

                    continue;
                }


                Zombie hit=null;


                for(Zombie z:zombies){

                    if(z.hp>0&&
                       z.row==q.row&&
                       Math.abs(
                               z.x-q.x)
                           <cw*.3f){

                        hit=z;
                        break;
                    }
                }


                if(hit!=null){

                    hit.hp-=q.damage;

                    it.remove();

                }else if(
                        q.x>
                         boolean unlocked(int t){

            if(t==PEASHOOTER)
                return true;

            if(t==BINU)
                return true;

            if(t==SUNFLOWER)
                return level>=2;

            if(t==GIGANUT)
                return level>=3;

            if(t==MINE)
                return level>=4;

            if(t==CHOMPER)
                return level>=5;

            if(t==REPEATER)
                return level>=6;

            return false;
        }


        int cost(int t){

            if(t==SUNFLOWER)
                return 50;

            if(t==PEASHOOTER)
                return 100;

            if(t==GIGANUT)
                return 125;

            if(t==CHOMPER)
                return 150;

            if(t==REPEATER)
                return 200;

            if(t==MINE)
                return 50;

            if(t==BINU)
                return 500;

            return 999999;
        }


        int activeRows(){

            if(level==1)
                return 1;

            if(level<=3)
                return 3;

            return 5;
        }


        boolean activeRow(int r){

            int n=
                    activeRows();

            int start=
                    (ROWS-n)/2;

            return r>=start&&
                   r<start+n;
        }


        void startLevel(int lv){

            closeWinVideo();

            level=
                    Math.max(
                            1,
                            Math.min(
                                    9,
                                    lv));

            sun=500;
            speed2=false;
            screen=PLAY;

            killed=0;
            spawned=0;


            total=
                    lv<=2
                            ?8
                            :lv<=4
                            ?10
                            :lv<=8
                            ?12
                            :15;


            clear();


            binuJump=false;
            binuFrame=0;
            binuRow=-1;
            binuCol=-1;


            last=
                    System.currentTimeMillis();

            spawnClock=last;
        }


        void clear(){

            for(int r=0;r<ROWS;r++){

                for(int col=0;
                    col<COLS;
                    col++){

                    plants[r][col]=null;
                }

                mowers[r]=
                        new Mower(r);
            }


            zombies.clear();
            peas.clear();
            suns.clear();

            tool=NONE;
        }


        void winLevel(){

            screen=WIN;


            if(level<9){

                unlocked=
                        Math.max(
                                unlocked,
                                level+1);
            }


            save();


            if(level==9){

                post(
                        MainActivity.this::
                                showWinVideo);
            }
        }


        void useFood(Plant a){

            // Binu không nhận Plant Food.
            if(a==null||
               a.type==BINU||
               food<=0||
               a.foodUsed)
                return;


            food--;

            a.foodUsed=true;

            playFoodSound();


            long now=
                    System.currentTimeMillis();


            if(a.type==GIGANUT){

                a.maxHp=8000;
                a.hp=8000;
                a.foodUntil=0;

            }else if(
                    a.type==SUNFLOWER){

                a.foodUntil=
                        now+12000;

                a.last=
                        now-2000;

            }else if(
                    a.type==PEASHOOTER||
                    a.type==REPEATER){

                a.foodUntil=
                        now+12000;

                a.last=
                        now-1000;

            }else if(
                    a.type==CHOMPER){

                Zombie z=
                        nearest(
                                a.row,
                                a.col,
                                cw*2.2f);

                if(z!=null)
                    z.hp=0;

                a.foodUntil=
                        now+3000;

            }else if(
                    a.type==MINE){

                a.armed=true;

                explodeMine(
                        a.row,
                        a.col,
                        a);
            }


            save();
        }


        void playFoodSound(){

            try{

                if(foodSound!=null){

                    if(foodSound.isPlaying())
                        foodSound.pause();

                    foodSound.seekTo(0);
                    foodSound.start();
                }

            }catch(Exception ignored){}
        }


        void buyFood(){

            if(coins>=100){

                coins-=100;
                food++;

                save();
            }
        }


        void restart(){

            startLevel(level);
        }


        void save(){

            getSharedPreferences(
                    "garden_defense",
                    MODE_PRIVATE)
                    .edit()
                    .putInt("level",level)
                    .putInt("unlocked",unlocked)
                    .putInt("coins",coins)
                    .putInt("food",food)
                    .apply();
        }


        void releaseSounds(){

            try{
                if(foodSound!=null)
                    foodSound.release();
            }catch(Exception ignored){}


            try{
                if(binuSound1!=null)
                    binuSound1.release();
            }catch(Exception ignored){}


            try{
                if(binuSound2!=null)
                    binuSound2.release();
            }catch(Exception ignored){}


            foodSound=null;
            binuSound1=null;
            binuSound2=null;
        }


        void overlay(
                Canvas c,
                String title,
                String a,
                String b,
                String d){

            p.setColor(0xaa000000);

            c.drawRect(
                    0,0,
                    getWidth(),
                    getHeight(),
                    p);


            text(
                    c,
                    title,
                    getWidth()/2f,
                    getHeight()*.25f,
                    34,
                    Color.WHITE,
                    Paint.Align.CENTER);


            if(!a.isEmpty())
                button(
                        c,
                        .30f,.38f,
                        .70f,.48f,
                        a);

            if(!b.isEmpty())
                button(
                        c,
                        .30f,.52f,
                        .70f,.62f,
                        b);

            if(!d.isEmpty())
                button(
                        c,
                        .30f,.66f,
                        .70f,.76f,
                        d);
        }


        @Override
        public boolean onTouchEvent(
                MotionEvent e){

            if(e.getAction() !=
                    MotionEvent.ACTION_DOWN)
                return true;


            float x=e.getX();
            float y=e.getY();


            if(screen==HOME){

                if(y>getHeight()*.40f&&
                   y<getHeight()*.57f)

                    startLevel(level);

                else if(
                        y>getHeight()*.58f&&
                        y<getHeight()*.75f)

                    screen=LEVELS;


                invalidate();
                return true;
            }


            if(screen==LEVELS){

                if(y>getHeight()*.82f){

                    screen=HOME;
                    invalidate();

                    return true;
                }


                for(int i=1;i<=9;i++){

                    int col=(i-1)%3;
                    int row=(i-1)/3;

                    float x1=
                            getWidth()*
                            (.18f+col*.22f);

                    float y1=
                            getHeight()*
                            (.18f+row*.19f);


                    if(i<=unlocked&&
                       x>=x1&&
                       x<=x1+
                           getWidth()*.17f&&
                       y>=y1&&
                       y<=y1+
                           getHeight()*.13f){

                        startLevel(i);
                        return true;
                    }
                }

                return true;
            }


            if(screen==PAUSE){

                if(y>getHeight()*.35f&&
                   y<getHeight()*.51f)

                    screen=PLAY;

                else if(
                        y>getHeight()*.51f&&
                        y<getHeight()*.65f)

                    restart();

                else if(
                        y>getHeight()*.65f&&
                        y<getHeight()*.80f){

                    save();
                    screen=HOME;
                }


                invalidate();
                return true;
            }


            if(screen==WIN){

                if(y>getHeight()*.35f&&
                   y<getHeight()*.51f){

                    if(level<9)
                        startLevel(level+1);

                }else if(
                        y>getHeight()*.51f&&
                        y<getHeight()*.65f){

                    restart();

                }else if(
                        y>getHeight()*.65f&&
                        y<getHeight()*.80f){

                    closeWinVideo();
                    screen=HOME;
                }


                invalidate();
                return true;
            }


            if(screen==LOSE){

                if(y>getHeight()*.35f&&
                   y<getHeight()*.51f)

                    restart();

                else if(
                        y>getHeight()*.65f&&
                        y<getHeight()*.80f)

                    screen=HOME;


                invalidate();
                return true;
            }


            if(screen==PLAY){

                // MUA PF
                if(y>=getHeight()*.075f&&
                   y<=getHeight()*.15f&&
                   x>=getWidth()*.64f&&
                   x<=getWidth()*.75f){

                    buyFood();
                    invalidate();
                    return true;
                }


                // XẺNG
                if(y>=getHeight()*.075f&&
                   y<=getHeight()*.15f&&
                   x>=getWidth()*.435f&&
                   x<=getWidth()*.495f){

                    tool=
                            tool==SHOVEL
                                    ?NONE
                                    :SHOVEL;

                    invalidate();
                    return true;
                }


                // PLANT FOOD
                if(y>=getHeight()*.075f&&
                   y<=getHeight()*.15f&&
                   x>=getWidth()*.505f&&
                   x<=getWidth()*.62f){

                    tool=
                            tool==FOOD
                                    ?NONE
                                    :FOOD;

                    invalidate();
                    return true;
                }


                // PAUSE
                if(y>=getHeight()*.075f&&
                   y<=getHeight()*.15f&&
                   x>=getWidth()*.80f&&
                   x<=getWidth()*.90f){

                    screen=PAUSE;

                    invalidate();
                    return true;
                }


                // SPEED
                if(y>=getHeight()*.075f&&
                   y<=getHeight()*.15f&&
                   x>getWidth()*.90f){

                    speed2=!speed2;

                    invalidate();
                    return true;
                }


                // CHỌN CÂY
                if(y>=getHeight()*.075f&&
                   y<=getHeight()*.15f&&
                   x<getWidth()*.435f){

                    int i=
                            (int)
                            ((x/getWidth()-.005f)
                                    /.061f);


                    int[] ts={
                            SUNFLOWER,
                            PEASHOOTER,
                            GIGANUT,
                            CHOMPER,
                            REPEATER,
                            MINE,
                            BINU
                    };


                    if(i>=0&&
                       i<ts.length&&
                       unlocked(ts[i])){

                        selected=ts[i];
                        tool=NONE;
                    }


                    invalidate();
                    return true;
                }


                // NHẶT SUN
                Iterator<SunDrop> sit=
                        suns.iterator();


                while(sit.hasNext()){

                    SunDrop s=sit.next();


                    if(Math.hypot(
                            x-s.x,
                            y-s.y)<45){

                        sun+=100;

                        sit.remove();

                        save();

                        invalidate();

                        return true;
                    }
                }


                // BOARD
                if(x>=left&&
                   x<=left+COLS*cw&&
                   y>=top&&
                   y<=top+ROWS*ch){

                    int col=
                            (int)
                            ((x-left)/cw);

                    int r=
                            (int)
                            ((y-top)/ch);


                    if(!activeRow(r))
                        return true;


                    Plant a=
                            plants[r][col];


                    if(tool==SHOVEL){

                        plants[r][col]=null;
                        tool=NONE;


                    }else if(tool==FOOD){

                        // Binu không dùng PF.
                        if(a!=null&&
                           a.type!=BINU){

                            useFood(a);
                        }

                        tool=NONE;


                    }else if(
                            a==null&&
                            unlocked(selected)&&
                            sun>=cost(selected)){

                        sun-=cost(selected);


                        plants[r][col]=
                                new Plant(
                                        selected,
                                        r,
                                        col);


                        // Lúc đặt Binu phát sound1.
                        if(selected==BINU)
                            playBinuSound1();


                        save();
                    }


                    invalidate();
                    return true;
                }
            }


            return true;
        }
    }


    // =========================
    // PLANT
    // =========================

    static class Plant {

        int type;
        int row;
        int col;

        int hp;
        int maxHp;

        long last;
        long secondShot;
        long foodUntil;
        long armAt;

        boolean foodUsed;
        boolean armed;


        Plant(
                int type,
                int row,
                int col){

            this.type=type;
            this.row=row;
            this.col=col;


            maxHp =
                    type==
                            MainActivity
                            .GardenGame
                            .GIGANUT
                            ?4000
                            :1000;


            hp=maxHp;


            armAt=
                    System.currentTimeMillis()
                    +30000;
        }
    }


    // =========================
    // ZOMBIE
    // =========================

    static class Zombie {

        float x;
        float y;
        float speed;

        int row;
        int hp;
        int maxHp;
        int damage;

        boolean boss;
        boolean giga;

        long lastAttack;
        long lastShot;


        Zombie(
                int row,
                float x,
                float y,
                boolean boss,
                boolean giga){

            this.row=row;
            this.x=x;
            this.y=y;
            this.boss=boss;
            this.giga=giga;


            maxHp =
                    boss
                            ?2500
                            :giga
                            ?900
                            :300;


            hp=maxHp;


            speed =
                    boss
                            ?12
                            :giga
                            ?16
                            :22;


            damage =
                    boss
                            ?150
                            :100;
        }
    }


    // =========================
    // PEA
    // =========================

    class Pea {

        float x;
        float y;

        int row;
        int damage;

        boolean big;
        boolean enemy;


        Pea(
                float x,
                float y,
                int row,
                int damage,
                boolean big){

            this(
                    x,
                    y,
                    row,
                    damage,
                    big,
                    false);
        }


        Pea(
                float x,
                float y,
                int row,
                int damage,
                boolean big,
                boolean enemy){

            this.x=x;
            this.y=y;
            this.row=row;
            this.damage=damage;
            this.big=big;
            this.enemy=enemy;
        }
    }


    // =========================
    // SUN
    // =========================

    class SunDrop {

        float x;
        float y;


        SunDrop(
                float x,
                float y){

            this.x=x;
            this.y=y;
        }
    }


    // =========================
    // MOWER
    // =========================

    class Mower {

        int row;
        float x;

        boolean used;
        boolean active;


        Mower(int row){

            this.row=row;
            this.x=0;
        }
    }
                }
