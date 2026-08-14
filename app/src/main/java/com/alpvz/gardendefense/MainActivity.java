package com.alpvz.gardendefense;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.*;
import android.view.*;
import java.util.*;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle b){
        super.onCreate(b);
        setContentView(new GameView());
    }

    class GameView extends View {

        Paint p=new Paint(3);
        Random r=new Random();

        Bitmap sunImg,peaImg,gigaImg,zombieImg,
                bulletImg,chomperImg,bomberImg;

        ArrayList<Plant> plants=new ArrayList<>();
        ArrayList<Zombie> zombies=new ArrayList<>();
        ArrayList<Bullet> bullets=new ArrayList<>();
        ArrayList<BombBullet> bombBullets=new ArrayList<>();
        ArrayList<CoinDrop> coinDrops=new ArrayList<>();

        int selected=0;
        int support=0;

        int sun=500;
        int coins=9999;
        int plantFood=0;

        int spawned=0;
        int killed=0;
        int level=1;

        boolean lose=false;
        boolean win=false;
        boolean chomperUnlocked=false;

        final int ROWS=5;
        final int COLS=9;

        float left=20;
        float top=185;
        float cellW,cellH;

        long last;
        long spawnTime;

        GameView(){
            super(MainActivity.this);

            sunImg=load("sun");
            peaImg=load("peashoot");
            gigaImg=load("giganut");
            zombieImg=load("zomplatz");
            bulletImg=load("gigapea");
            chomperImg=load("chomper");
            bomberImg=load("zomvinhhung");

            last=System.currentTimeMillis();
            spawnTime=last;
        }

        Bitmap load(String n){
            int id=getResources().getIdentifier(
                    n,
                    "drawable",
                    getPackageName()
            );

            return id==0 ? null :
                    BitmapFactory.decodeResource(
                            getResources(),
                            id
                    );
        }

        @Override
        protected void onDraw(Canvas c){

            cellW=(getWidth()-40f)/COLS;
            cellH=(getHeight()-top-20f)/ROWS;

            p.setColor(Color.rgb(95,175,70));
            c.drawRect(
                    0,
                    0,
                    getWidth(),
                    getHeight(),
                    p
            );

            topUI(c);
            drawBoard(c);
            drawPlants(c);
            drawBullets(c);
            drawBombBullets(c);
            drawCoinDrops(c);
            drawZombies(c);

            if(!lose && !win){
                update();
                postInvalidateDelayed(30);
            }else{
                drawEnd(c);
            }
        }

        void topUI(Canvas c){

            p.setColor(Color.rgb(55,120,50));
            c.drawRect(
                    0,
                    0,
                    getWidth(),
                    165,
                    p
            );

            p.setColor(Color.WHITE);
            p.setTextSize(18);

            c.drawText(
                    "MÀN "+level+
                    "  SUN: "+sun+
                    "  XU: "+coins+
                    "  PF: "+plantFood,
                    10,
                    25,
                    p
            );

            card(c,5,45,1,"SUN",sunImg);
            card(c,130,45,2,"PEA",peaImg);
            card(c,255,45,3,"GIGA",gigaImg);

            if(chomperUnlocked){
                card(
                        c,
                        380,
                        45,
                        4,
                        "CHOMP",
                        chomperImg
                );
            }

            skill(c,505,45,1,"SAM",30);
            skill(c,630,45,2,"BANG",60);
            skill(c,755,45,3,"LUA",90);
        }

        void card(
                Canvas c,
                float x,
                float y,
                int t,
                String n,
                Bitmap img){

            p.setColor(
                    selected==t ?
                    Color.YELLOW :
                    Color.WHITE
            );

            c.drawRoundRect(
                    new RectF(
                            x,
                            y,
                            x+115,
                            y+90
                    ),
                    10,
                    10,
                    p
            );

            if(img!=null){

                c.drawBitmap(
                        img,
                        null,
                        new RectF(
                                x+5,
                                y+5,
                                x+55,
                                y+80
                        ),
                        p
                );
            }

            p.setColor(Color.DKGRAY);
            p.setTextSize(12);

            c.drawText(
                    n,
                    x+60,
                    y+48,
                    p
            );
        }

        void skill(
                Canvas c,
                float x,
                float y,
                int t,
                String n,
                int cost){

            if(x>getWidth()-5)
                return;

            p.setColor(
                    support==t ?
                    Color.YELLOW :
                    Color.WHITE
            );

            c.drawRoundRect(
                    new RectF(
                            x,
                            y,
                            x+115,
                            y+90
                    ),
                    10,
                    10,
                    p
            );

            p.setColor(
                    t==1 ?
                    Color.YELLOW :
                    t==2 ?
                    Color.CYAN :
                    Color.RED
            );

            c.drawCircle(
                    x+28,
                    y+34,
                    17,
                    p
            );

            p.setColor(Color.DKGRAY);
            p.setTextSize(11);

            c.drawText(
                    n,
                    x+50,
                    y+35,
                    p
            );

            c.drawText(
                    cost+" XU",
                    x+50,
                    y+58,
                    p
            );
        }

        void drawBoard(Canvas c){

            for(int row=0;row<ROWS;row++){

                for(int col=0;col<COLS;col++){

                    p.setColor(
                            (row+col)%2==0 ?
                            Color.rgb(115,190,75) :
                            Color.rgb(105,180,68)
                    );

                    float x=
                            left+
                            col*cellW;

                    float y=
                            top+
                            row*cellH;

                    c.drawRect(
                            x,
                            y,
                            x+cellW-2,
                            y+cellH-2,
                            p
                    );
                }
            }
        }

        void drawPlants(Canvas c){

            for(Plant a:plants){

                Bitmap img=
                        a.type==1 ?
                        sunImg :
                        a.type==2 ?
                        peaImg :
                        a.type==3 ?
                        gigaImg :
                        chomperImg;

                float x=
                        left+
                        a.col*cellW;

                float y=
                        top+
                        a.row*cellH;

                if(img!=null){

                    c.drawBitmap(
                            img,
                            null,
                            new RectF(
                                    x+5,
                                    y+5,
                                    x+cellW-5,
                                    y+cellH-5
                            ),
                            p
                    );
                }

                hp(
                        c,
                        x+cellW*.3f,
                        y+4,
                        cellW*.4f,
                        a.hp,
                        a.max
                );
            }
        }

        void drawBullets(Canvas c){

            for(Bullet b:bullets){

                if(bulletImg!=null){

                    c.drawBitmap(
                            bulletImg,
                            null,
                            new RectF(
                                    b.x-21,
                                    b.y-21,
                                    b.x+21,
                                    b.y+21
                            ),
                            p
                    );

                }else{

                    p.setColor(Color.GREEN);

                    c.drawCircle(
                            b.x,
                            b.y,
                            18,
                            p
                    );
                }
            }
        }

        void drawBombBullets(Canvas c){

            for(BombBullet b:bombBullets){

                p.setColor(
                        Color.rgb(
                                255,
                                70,
                                20
                        )
                );

                c.drawCircle(
                        b.x,
                        b.y,
                        13,
                        p
                );

                p.setColor(Color.YELLOW);

                c.drawCircle(
                        b.x-4,
                        b.y-4,
                        4,
                        p
                );
            }
        }

        void drawCoinDrops(Canvas c){

            for(CoinDrop d:coinDrops){

                p.setColor(
                        d.type==1 ?
                        Color.LTGRAY :
                        d.type==2 ?
                        Color.YELLOW :
                        Color.CYAN
                );

                c.drawCircle(
                        d.x,
                        d.y,
                        9,
                        p
                );

                p.setColor(Color.DKGRAY);
                p.setTextSize(9);

                c.drawText(
                        ""+d.value,
                        d.x-7,
                        d.y+3,
                        p
                );
            }
        }

        void drawZombies(Canvas c){

            for(Zombie z:zombies){

                float w=
                        z.bomber ?
                        65 :
                        z.big ?
                        105 :
                        82;

                float h=
                        z.bomber ?
                        85 :
                        z.big ?
                        140 :
                        112;

                Bitmap img=
                        z.bomber ?
                        bomberImg :
                        zombieImg;

                if(img!=null){

                    c.drawBitmap(
                            img,
                            null,
                            new RectF(
                                    z.x-w/2,
                                    z.y-h/2,
                                    z.x+w/2,
                                    z.y+h/2
                            ),
                            p
                    );

                }else{

                    p.setColor(
                            z.bomber ?
                            Color.DKGRAY :
                            z.big ?
                            Color.BLACK :
                            Color.GRAY
                    );

                    c.drawRoundRect(
                            new RectF(
                                    z.x-w/2,
                                    z.y-h/2,
                                    z.x+w/2,
                                    z.y+h/2
                            ),
                            12,
                            12,
                            p
                    );
                }

                hp(
                        c,
                        z.x-30,
                        z.y-(z.big?75:60),
                        60,
                        z.hp,
                        z.max
                );
            }
        }

        void hp(
                Canvas c,
                float x,
                float y,
                float w,
                int v,
                int m){

            p.setColor(Color.RED);

            c.drawRect(
                    x,
                    y,
                    x+w,
                    y+5,
                    p
            );

            p.setColor(Color.GREEN);

            float q=
                    Math.max(
                            0,
                            Math.min(
                                    1,
                                    v/(float)m
                            )
                    );

            c.drawRect(
                    x,
                    y,
                    x+w*q,
                    y+5,
                    p
            );
        }

        void update(){

            long now=
                    System.currentTimeMillis();

            float dt=
                    (now-last)/1000f;

            if(dt>.1f)
                dt=.1f;

            last=now;

            int total=
                    level==1 ?
                    12 :
                    level==2 ?
                    15 :
                    18;

            if(
                    spawned<total &&
                    now-spawnTime>=5000
            ){

                spawn();
                spawnTime=now;
            }

            for(Plant a:plants){

                a.timer+=dt;

                if(a.foodTime>0){

                    a.foodTime-=dt;

                    if(a.foodTime<0)
                        a.foodTime=0;
                }

                if(
                        a.type==1 &&
                        a.timer>=
                        (a.foodTime>0 ?
                        .1f :
                        5f)
                ){

                    sun+=50;
                    a.timer=0;
                }

                if(
                        a.type==2 &&
                        a.timer>=
                        (a.foodTime>0 ?
                        .1f :
                        1.2f) &&
                        rowHas(a.row)
                ){

                    bullets.add(
                            new Bullet(
                                    left+
                                    a.col*cellW+
                                    cellW-10,
                                    top+
                                    a.row*cellH+
                                    cellH/2,
                                    a.row
                            )
                    );

                    a.timer=0;
                }

                /*
                 * CHOMPER PLANT FOOD:
                 * hút zombie cùng hàng.
                 */
                if(
                        a.type==4 &&
                        a.chomperFoodActive
                ){

                    float px=
                            left+
                            a.col*cellW+
                            cellW/2;

                    boolean any=false;

                    for(Zombie z:zombies){

                        if(z.row==a.row){

                            any=true;

                            float dist=
                                    Math.abs(
                                            z.x-px
                                    );

                            if(dist>18){

                                z.x+=
                                        (px>z.x?1:-1)*
                                        Math.min(
                                                12,
                                                dist
                                        );
                            }
                        }
                    }

                    a.chomperFoodTimer-=dt;

                    if(
                            !any ||
                            a.chomperFoodTimer<=0
                    ){

                        for(Zombie z:zombies){

                            if(z.row==a.row)
                                z.hp=0;
                        }

                        a.chomperFoodActive=false;

                        /*
                         * Reset như Chomper
                         * vừa mới đặt xuống.
                         */
                        a.chomperReady=true;
                        a.timer=0;
                    }
                }

                /*
                 * Chomper mới đặt:
                 * ăn ngay.
                 */
                if(
                        a.type==4 &&
                        !a.chomperFoodActive &&
                        a.chomperReady
                ){

                    Zombie z=
                            chompTarget(a);

                    if(z!=null){

                        z.hp=0;

                        a.chomperReady=false;
                        a.timer=0;
                    }
                }

                /*
                 * Sau khi ăn thường:
                 * chờ 40 giây.
                 */
                else if(
                        a.type==4 &&
                        !a.chomperFoodActive &&
                        !a.chomperReady &&
                        a.timer>=40f
                ){

                    Zombie z=
                            chompTarget(a);

                    if(z!=null){

                        z.hp=0;
                        a.timer=0;
                    }
                }
            }

            for(Zombie z:zombies){

                if(z.slow>0)
                    z.slow-=dt;
            }

            updateBullets();
            updateBombBullets();
            updateZombies();
            updateCoinDrops(dt);

            clean();

            if(
                    spawned>=total &&
                    zombies.isEmpty() &&
                    killed>=total
            ){

                win=true;
            }
        }

        boolean rowHas(int row){

            for(Zombie z:zombies)
                if(z.row==row)
                    return true;

            return false;
        }

        void updateBullets(){

            Iterator<Bullet>it=
                    bullets.iterator();

            while(it.hasNext()){

                Bullet b=it.next();

                b.x+=8;

                boolean hit=false;

                for(Zombie z:zombies){

                    if(
                            z.row==b.row &&
                            Math.abs(z.x-b.x)<35
                    ){

                        z.hp-=25;
                        hit=true;
                        break;
                    }
                }

                if(
                        hit ||
                        b.x>getWidth()+60
                ){

                    it.remove();
                }
            }
        }

        void updateBombBullets(){

            Iterator<BombBullet>it=
                    bombBullets.iterator();

            while(it.hasNext()){

                BombBullet b=it.next();

                float dx=b.tx-b.x;
                float dy=b.ty-b.y;

                float d=
                        (float)Math.sqrt(
                                dx*dx+
                           void updateZombies(){

            for(Zombie z:zombies){

                if(z.x<-70){

                    lose=true;
                    return;
                }

                /*
                 * Zomvinhhung:
                 * chỉ đứng yên và ném.
                 */
                if(z.bomber){

                    float stop=
                            left+
                            cellW*1.5f;

                    if(z.x>stop){

                        z.x-=.6f;

                    }else{

                        z.x=stop;
                        z.stopped=true;
                    }

                    if(z.stopped){

                        z.throwTimer+=30;

                        if(z.throwTimer>=8000){

                            bombBullets.add(
                                    new BombBullet(
                                            z.x,
                                            z.y,
                                            stop,
                                            z.y,
                                            z.row
                                    )
                            );

                            z.throwTimer=0;
                        }
                    }

                    continue;
                }

                Plant a=findPlant(z);

                if(a!=null){

                    long n=
                            System.currentTimeMillis();

                    if(n-a.lastBite>=500){

                        a.hp-=100;
                        a.lastBite=n;
                    }

                }else{

                    float speed=
                            z.big ?
                            .55f :
                            1f;

                    if(z.slow>0)
                        speed*=.45f;

                    z.x-=speed;
                }
            }
        }

        Plant findPlant(Zombie z){

            for(Plant a:plants){

                if(a.row==z.row){

                    float px=
                            left+
                            a.col*cellW+
                            cellW/2;

                    if(
                            Math.abs(z.x-px)<
                            (z.big?70:55)
                    ){

                        return a;
                    }
                }
            }

            return null;
        }

        Zombie chompTarget(Plant a){

            for(Zombie z:zombies){

                if(z.row==a.row){

                    float px=
                            left+
                            a.col*cellW+
                            cellW/2;

                    if(
                            Math.abs(z.x-px)<
                            cellW*1.5f
                    ){

                        return z;
                    }
                }
            }

            return null;
        }

        void spawn(){

            int row=r.nextInt(ROWS);

            boolean big=false;
            boolean bomber=false;

            if(level==2){

                big=
                        spawned>=4 &&
                        spawned%4==0;
            }

            if(level==3){

                if(
                        spawned>0 &&
                        spawned%4==0
                ){

                    bomber=true;

                }else if(spawned%3==0){

                    big=true;
                }
            }

            zombies.add(
                    new Zombie(
                            getWidth()+70,
                            top+
                            row*cellH+
                            cellH/2,
                            row,
                            big,
                            bomber
                    )
            );

            spawned++;
        }

        void clean(){

            Iterator<Plant>pi=
                    plants.iterator();

            while(pi.hasNext()){

                if(pi.next().hp<=0)
                    pi.remove();
            }

            Iterator<Zombie>zi=
                    zombies.iterator();

            while(zi.hasNext()){

                Zombie z=zi.next();

                if(z.hp<=0){

                    zi.remove();

                    killed++;
                    sun+=25;

                    int q=
                            r.nextInt(100);

                    /*
                     * 1% kim cương = 100
                     * 5% vàng = 50
                     * 10% bạc = 25
                     */
                    if(q<1){

                        coinDrops.add(
                                new CoinDrop(
                                        z.x,
                                        z.y,
                                        3,
                                        100
                                )
                        );

                    }else if(q<6){

                        coinDrops.add(
                                new CoinDrop(
                                        z.x,
                                        z.y,
                                        2,
                                        50
                                )
                        );

                    }else if(q<16){

                        coinDrops.add(
                                new CoinDrop(
                                        z.x,
                                        z.y,
                                        1,
                                        25
                                )
                        );
                    }
                }
            }
        }

        void updateCoinDrops(float dt){

            Iterator<CoinDrop>it=
                    coinDrops.iterator();

            while(it.hasNext()){

                CoinDrop d=it.next();

                d.life-=dt;

                if(d.life<=0)
                    it.remove();
            }
        }

        void usePlantFood(float x,float y){

            if(plantFood<=0){

                support=0;
                return;
            }

            int col=
                    Math.max(
                            0,
                            Math.min(
                                    COLS-1,
                                    (int)((x-left)/cellW)
                            )
                    );

            int row=
                    Math.max(
                            0,
                            Math.min(
                                    ROWS-1,
                                    (int)((y-top)/cellH)
                            )
                    );

            boolean used=false;

            /*
             * Plant Food chỉ dùng cho cây.
             * Không còn cho Zomvinhhung.
             */
            for(Plant a:plants){

                if(
                        a.row==row &&
                        a.col==col
                ){

                    if(a.type==1){

                        a.foodTime=3f;
                        sun+=300;
                        used=true;

                    }else if(a.type==2){

                        a.foodTime=3f;
                        used=true;

                    }else if(
                            a.type==4 &&
                            !a.chomperFoodActive
                    ){

                        /*
                         * Chomper hút toàn bộ zombie
                         * trên hàng rồi nuốt.
                         */
                        a.chomperFoodActive=true;
                        a.chomperFoodTimer=.7f;
                        a.chomperReady=false;

                        used=true;
                    }

                    break;
                }
            }

            if(used)
                plantFood--;

            support=0;
        }

        void useSupport(float x,float y){

            int cost=
                    support==1 ?
                    30 :
                    support==2 ?
                    60 :
                    90;

            if(coins<cost){

                support=0;
                return;
            }

            int row=
                    Math.max(
                            0,
                            Math.min(
                                    ROWS-1,
                                    (int)((y-top)/cellH)
                            )
                    );

            int col=
                    Math.max(
                            0,
                            Math.min(
                                    COLS-1,
                                    (int)((x-left)/cellW)
                            )
                    );

            for(Zombie z:zombies){

                if(
                        z.row>=row-1 &&
                        z.row<=row+1 &&
                        Math.abs(
                                z.x-
                                (
                                    left+
                                    col*cellW+
                                    cellW/2
                                )
                        )<
                        cellW*1.6f
                ){

                    if(support==1){

                        z.hp-=500;

                    }else if(support==2){

                        z.slow=5;

                    }else{

                        z.hp-=800;
                    }
                }
            }

            coins-=cost;
            support=0;
        }

        void drawEnd(Canvas c){

            p.setColor(0xAA000000);

            c.drawRect(
                    0,
                    0,
                    getWidth(),
                    getHeight(),
                    p
            );

            p.setTextAlign(
                    Paint.Align.CENTER
            );

            p.setColor(Color.WHITE);
            p.setTextSize(34);

            if(win){

                if(level==2)
                    chomperUnlocked=true;

                c.drawText(
                        level==2 ?
                        "MỞ KHÓA CHOMPER!" :
                        "THẮNG MÀN "+level,
                        getWidth()/2f,
                        getHeight()/2f-20,
                        p
                );

                p.setTextSize(17);

                c.drawText(
                        level<3 ?
                        "CHẠM ĐỂ SANG MÀN TIẾP" :
                        "CHẠM ĐỂ CHƠI LẠI",
                        getWidth()/2f,
                        getHeight()/2f+35,
                        p
                );

            }else{

                c.drawText(
                        "THUA!",
                        getWidth()/2f,
                        getHeight()/2f-20,
                        p
                );

                p.setTextSize(17);

                c.drawText(
                        "CHẠM ĐỂ CHƠI LẠI",
                        getWidth()/2f,
                        getHeight()/2f+35,
                        p
                );
            }

            p.setTextAlign(
                    Paint.Align.LEFT
            );
        }

        void reset(boolean all){

            plants.clear();
            zombies.clear();
            bullets.clear();
            bombBullets.clear();
            coinDrops.clear();

            selected=0;
            support=0;
            spawned=0;
            killed=0;

            lose=false;
            win=false;

            if(all){

                level=1;
                chomperUnlocked=false;

                sun=500;
                coins=9999;
                plantFood=0;
            }

            last=
                    System.currentTimeMillis();

            spawnTime=last;
        }

        @Override
        public boolean onTouchEvent(
                MotionEvent e
        ){

            if(
                    e.getAction() !=
                    MotionEvent.ACTION_DOWN
            )
                return true;

            float x=e.getX();
            float y=e.getY();

            if(lose){

                reset(true);
                invalidate();
                return true;
            }

            if(win){

                if(level==2)
                    chomperUnlocked=true;

                if(level<3){

                    level++;
                    reset(false);

                }else{

                    reset(true);
                }

                invalidate();
                return true;
            }

            /*
             * Chọn kỹ năng hỗ trợ.
             */
            if(y>=40&&y<=140){

                if(x>=500&&x<620){

                    support=1;
                    selected=0;
                    return true;
                }

                if(x>=625&&x<745){

                    support=2;
                    selected=0;
                    return true;
                }

                if(x>=750&&x<870){

                    support=3;
                    selected=0;
                    return true;
                }
            }

            /*
             * Chọn cây.
             */
            if(y>=40&&y<=140){

                if(x<125){

                    selected=1;

                }else if(x<250){

                    selected=2;

                }else if(x<375){

                    selected=3;

                }else if(
                        x<500 &&
                        chomperUnlocked
                ){

                    selected=4;
                }

                invalidate();
                return true;
            }

            /*
             * Đặt cây / dùng Plant Food /
             * dùng kỹ năng.
             */
            if(
                    x>=left &&
                    x<left+COLS*cellW &&
                    y>=top &&
                    y<top+ROWS*cellH
            ){

                if(support!=0){

                    useSupport(x,y);
                    invalidate();
                    return true;
                }

                if(
                        selected==0 &&
                        plantFood>0
                ){

                    usePlantFood(x,y);
                    invalidate();
                    return true;
                }

                if(selected!=0){

                    int col=
                            (int)((x-left)/cellW);

                    int row=
                            (int)((y-top)/cellH);

                    if(!occupied(row,col)){

                        int cost=
                                selected==1 ?
                                50 :
                                selected==2 ?
                                100 :
                                selected==3 ?
                                150 :
                                150;

                        if(sun>=cost){

                            /*
                             * Giganut = 6000 HP.
                             */
                            int max=
                                    selected==1 ?
                                    300 :
                                    selected==2 ?
                                    400 :
                                    selected==3 ?
                                    6000 :
                                    800;

                            plants.add(
                                    new Plant(
                                            selected,
                                            row,
                                            col,
                                            max
                                    )
                            );

                            sun-=cost;
                            selected=0;
                        }
                    }
                }

                invalidate();
            }

            return true;
        }

        boolean occupied(
                int row,
                int col
        ){

            for(Plant a:plants){

                if(
                        a.row==row &&
                        a.col==col
                ){

                    return true;
                }
            }

            return false;
        }
    }

    class Plant{

        int type,row,col,hp,max;

        float timer;
        long lastBite;

        float foodTime;

        boolean chomperReady;
        boolean chomperFoodActive;
        float chomperFoodTimer;

        Plant(
                int t,
                int r,
                int c,
                int h
        ){

            type=t;
            row=r;
            col=c;

            hp=max=h;

            foodTime=0;

            /*
             * Chomper vừa đặt:
             * được ăn ngay.
             */
            chomperReady=(t==4);

            chomperFoodActive=false;
            chomperFoodTimer=0;
        }
    }

    class Zombie{

        float x,y;

        float damageTimer;
        float throwTimer;
        float slow;

        int row;
        int hp,max;

        boolean big;
        boolean bomber;
        boolean stopped;

        Zombie(
                float xx,
                float yy,
                int rr,
                boolean b,
                boolean bo
        ){

            x=xx;
            y=yy;
            row=rr;

            big=b;
            bomber=bo;

            /*
             * Zomto = 1000 HP
             * Zomvinhhung = 500 HP
             */
            hp=max=
                    bo ?
                    500 :
                    b ?
                    1000 :
                    300;
        }
    }

    class Bullet{

        float x,y;
        int row;

        Bullet(
                float xx,
                float yy,
                int rr
        ){

            x=xx;
            y=yy;
            row=rr;
        }
    }

    class BombBullet{

        float x,y,tx,ty;
        float speed=5;

        int row;

        BombBullet(
                float xx,
                float yy,
                float a,
                float b,
                int r
        ){

            x=xx;
            y=yy;

            tx=a;
            ty=b;

            row=r;
        }
    }

    class CoinDrop{

        float x,y;
        float life=8f;

        int type;
        int value;

        CoinDrop(
                float xx,
                float yy,
                int t,
                int v
        ){

            x=xx;
            y=yy;

            type=t;
            value=v;
        }
    }
                   }
