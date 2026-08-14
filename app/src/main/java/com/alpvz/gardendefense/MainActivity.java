package com.alpvz.gardendefense;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.*;
import android.view.*;
import java.util.*;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(new GameView());
    }

    class GameView extends View {
        Paint p = new Paint(3);
        Random r = new Random();

        Bitmap sunImg, peaImg, gigaImg, bulletImg, zombieImg, bombImg;

        ArrayList<Plant> plants = new ArrayList<>();
        ArrayList<Zombie> zombies = new ArrayList<>();
        ArrayList<Bullet> bullets = new ArrayList<>();

        int selected = 0;
        int sun = 500;
        int spawned = 0;
        int killed = 0;

        final int ROWS = 5;
        final int COLS = 9;
        final int TOTAL = 15;

        float left = 20;
        float top = 185;
        float cellW, cellH;

        long lastTime;
        long spawnTime;

        boolean lose = false;
        boolean win = false;

        GameView() {
            super(MainActivity.this);

            sunImg = load("sun");
            peaImg = load("peashoot");
            gigaImg = load("giganut");
            bulletImg = load("gigapea");
            zombieImg = load("zomplatz");
            bombImg = load("zombom");

            lastTime = System.currentTimeMillis();
            spawnTime = lastTime;
        }

        Bitmap load(String name) {
            int id = getResources().getIdentifier(
                    name, "drawable", getPackageName()
            );
            return id == 0 ? null :
                    BitmapFactory.decodeResource(getResources(), id);
        }

        @Override
        protected void onDraw(Canvas c) {
            cellW = (getWidth() - 40f) / COLS;
            cellH = (getHeight() - top - 20f) / ROWS;

            p.setColor(Color.rgb(95,175,70));
            c.drawRect(0,0,getWidth(),getHeight(),p);

            drawTop(c);
            drawBoard(c);
            drawPlants(c);
            drawBullets(c);
            drawZombies(c);

            if (!lose && !win) {
                updateGame();
                postInvalidateDelayed(30);
            } else {
                drawEnd(c);
            }
        }

        void drawTop(Canvas c) {
            p.setColor(Color.rgb(55,120,50));
            c.drawRect(0,0,getWidth(),165,p);

            p.setColor(Color.WHITE);
            p.setTextSize(24);
            c.drawText("SUN: " + sun,18,32,p);

            float bx = 220;
            float bw = getWidth() - 240;

            p.setColor(Color.DKGRAY);
            c.drawRoundRect(
                    new RectF(bx,10,bx+bw,32),
                    10,10,p
            );

            p.setColor(Color.GREEN);
            float progress = Math.min(
                    1f, killed / (float)TOTAL
            );

            c.drawRoundRect(
                    new RectF(
                            bx,10,
                            bx+bw*progress,32
                    ),
                    10,10,p
            );

            p.setColor(Color.WHITE);
            p.setTextSize(13);
            c.drawText(
                    killed + "/" + TOTAL,
                    bx+8,26,p
            );

            card(c,10,55,1,"SUN",sunImg);
            card(c,145,55,2,"PEA",peaImg);
            card(c,280,55,3,"GIGA",gigaImg);
        }

        void card(Canvas c,float x,float y,
                  int type,String name,Bitmap img) {

            p.setColor(
                    selected == type
                            ? Color.YELLOW
                            : Color.WHITE
            );

            c.drawRoundRect(
                    new RectF(x,y,x+120,y+90),
                    12,12,p
            );

            if (img != null) {
                c.drawBitmap(
                        img,null,
                        new RectF(
                                x+5,y+5,
                                x+60,y+80
                        ),p
                );
            }

            p.setColor(Color.DKGRAY);
            p.setTextSize(14);
            c.drawText(name,x+67,y+50,p);
        }

        void drawBoard(Canvas c) {
            for(int row=0;row<ROWS;row++) {
                for(int col=0;col<COLS;col++) {

                    p.setColor(
                            (row+col)%2==0
                                    ? Color.rgb(115,190,75)
                                    : Color.rgb(105,180,68)
                    );

                    float x = left + col*cellW;
                    float y = top + row*cellH;

                    c.drawRect(
                            x,y,
                            x+cellW-2,
                            y+cellH-2,
                            p
                    );
                }
            }
        }

        void drawPlants(Canvas c) {
            for(Plant a:plants) {
                Bitmap img =
                        a.type==1 ? sunImg :
                        a.type==2 ? peaImg :
                        gigaImg;

                float x = left+a.col*cellW;
                float y = top+a.row*cellH;

                if(img!=null) {
                    c.drawBitmap(
                            img,null,
                            new RectF(
                                    x+5,y+5,
                                    x+cellW-5,
                                    y+cellH-5
                            ),p
                    );
                }

                drawHP(
                        c,
                        x+cellW*.3f,
                        y+4,
                        cellW*.4f,
                        a.hp,a.max
                );
            }
        }

        void drawBullets(Canvas c) {
            for(Bullet b:bullets) {
                if(bulletImg!=null) {
                    c.drawBitmap(
                            bulletImg,null,
                            new RectF(
                                    b.x-21,b.y-21,
                                    b.x+21,b.y+21
                            ),p
                    );
                } else {
                    p.setColor(Color.GREEN);
                    c.drawCircle(
                            b.x,b.y,18,p
                    );
                }
            }
        }        void drawZombies(Canvas c) {
            for(Zombie z:zombies) {

                float w = z.big ? 110 :
                        z.bomber ? 90 : 82;

                float h = z.big ? 150 :
                        z.bomber ? 120 : 112;

                Bitmap img =
                        z.bomber ? bombImg : zombieImg;

                if(img!=null) {
                    c.drawBitmap(
                            img,null,
                            new RectF(
                                    z.x-w/2,
                                    z.y-h/2,
                                    z.x+w/2,
                                    z.y+h/2
                            ),p
                    );
                } else {
                    p.setColor(
                            z.bomber
                                    ? Color.rgb(80,60,60)
                                    : z.big
                                        ? Color.DKGRAY
                                        : Color.GRAY
                    );

                    c.drawRoundRect(
                            new RectF(
                                    z.x-w/2,
                                    z.y-h/2,
                                    z.x+w/2,
                                    z.y+h/2
                            ),
                            12,12,p
                    );
                }

                drawHP(
                        c,
                        z.x-30,
                        z.y-(z.big?80:62),
                        60,
                        z.hp,z.max
                );
            }
        }

        void drawHP(
                Canvas c,float x,float y,
                float w,int hp,int max) {

            p.setColor(Color.RED);
            c.drawRect(x,y,x+w,y+5,p);

            p.setColor(Color.GREEN);

            float q = Math.max(
                    0,
                    Math.min(1,hp/(float)max)
            );

            c.drawRect(
                    x,y,x+w*q,y+5,p
            );
        }

        void updateGame() {
            long now = System.currentTimeMillis();

            float dt =
                    (now-lastTime)/1000f;

            if(dt>0.1f) dt=0.1f;

            lastTime=now;

            if(spawned<TOTAL &&
                    now-spawnTime>=4000) {

                spawnZombie();
                spawnTime=now;
            }

            for(Plant a:plants) {
                a.timer+=dt;

                if(a.type==1 &&
                        a.timer>=5) {

                    sun+=100;
                    a.timer=0;
                }

                if(a.type==2 &&
                        a.timer>=1.2f &&
                        rowHasZombie(a.row)) {

                    bullets.add(
                            new Bullet(
                                    left+a.col*cellW+
                                            cellW-10,
                                    top+a.row*cellH+
                                            cellH/2,
                                    a.row
                            )
                    );

                    a.timer=0;
                }
            }

            for(Zombie z:zombies) {
                if(z.big) {
                    z.damageTimer+=dt;

                    if(z.damageTimer>=1) {
                        for(Plant a:plants) {
                            a.hp-=10;
                        }

                        z.damageTimer=0;
                    }
                }
            }

            updateBullets();
            updateZombies();
            clean();

            if(spawned>=TOTAL &&
                    zombies.isEmpty() &&
                    killed>=TOTAL) {

                win=true;
            }
        }

        boolean rowHasZombie(int row) {
            for(Zombie z:zombies)
                if(z.row==row)
                    return true;

            return false;
        }

        void updateBullets() {
            Iterator<Bullet> it =
                    bullets.iterator();

            while(it.hasNext()) {
                Bullet b=it.next();

                b.x+=8;

                boolean hit=false;

                for(Zombie z:zombies) {

                    if(z.row==b.row &&
                            Math.abs(z.x-b.x)<35) {

                        z.hp-=25;
                        hit=true;
                        break;
                    }
                }

                if(hit ||
                        b.x>getWidth()+60) {

                    it.remove();
                }
            }
        }

        void updateZombies() {

            for(Zombie z:zombies) {

                if(z.x<-70) {
                    lose=true;
                    return;
                }

                // ZOMBIE NÉM BOM
                if(z.bomber) {

                    float stopX =
                            left+cellW*1.5f;

                    if(!z.stopped) {

                        if(z.x>stopX) {
                            z.x-=0.75f;
                        } else {
                            z.x=stopX;
                            z.stopped=true;
                        }
                    }

                    z.throwTimer+=30;

                    if(z.stopped &&
                            z.throwTimer>=8000) {

                        bombAttack(z);
                        z.throwTimer=0;
                    }

                    continue;
                }

                Plant target=findPlant(z);

                if(target!=null) {

                    // ZOMBIE TO ĐỤNG CÂY = THUA
                    if(z.big) {
                        z.x-=cellW*2;
                        lose=true;
                        return;
                    }

                    // ZOMBIE THƯỜNG CẮN 100 DAMAGE
                    if(System.currentTimeMillis()-
                            target.lastBite>=500) {

                        target.hp-=100;

                        target.lastBite=
                                System.currentTimeMillis();
                    }

                } else {

                    // Zombie thường chậm hơn
                    z.x-=z.big ? 0.5f : 0.8f;
                }
            }
        }

        void bombAttack(Zombie z) {

            // Vùng nổ 3x3 quanh ô thứ 2
            int centerCol=1;

            for(Plant a:plants) {

                int dc=Math.abs(
                        a.col-centerCol
                );

                int dr=Math.abs(
                        a.row-z.row
                );

                if(dc<=1 && dr<=1) {
                    a.hp-=200;
                }
            }
        }

        Plant findPlant(Zombie z) {

            for(Plant a:plants) {

                if(a.row!=z.row)
                    continue;

                float px =
                        left+
                        a.col*cellW+
                        cellW/2;

                if(Math.abs(z.x-px)<
                        (z.big?70:55)) {

                    return a;
                }
            }

            return null;
        }

        void spawnZombie() {

            int row=r.nextInt(ROWS);

            int type=r.nextInt(5);

            boolean big=type==0;
            boolean bomber=type==1;

            zombies.add(
                    new Zombie(
                            getWidth()+60,
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

        void clean() {

            Iterator<Plant> pi=
                    plants.iterator();

            while(pi.hasNext()) {
                if(pi.next().hp<=0)
                    pi.remove();
            }

            Iterator<Zombie> zi=
                    zombies.iterator();

            while(zi.hasNext()) {

                Zombie z=zi.next();

                if(z.hp<=0) {
                    zi.remove();
                    killed++;
                    sun+=25;
                }
            }

            Iterator<Bullet> bi=
                    bullets.iterator();

            while(bi.hasNext()) {

                if(bi.next().x>
                        getWidth()+60) {

                    bi.remove();
                }
            }
        }

        boolean occupied(int row,int col) {

            for(Plant a:plants) {
                if(a.row==row &&
                        a.col==col)
                    return true;
            }

            return false;
                }        void drawEnd(Canvas c) {

            p.setColor(0xAA000000);

            c.drawRect(
                    0,0,
                    getWidth(),
                    getHeight(),
                    p
            );

            p.setTextAlign(
                    Paint.Align.CENTER
            );

            p.setColor(Color.WHITE);
            p.setTextSize(42);

            c.drawText(
                    win ? "CHIẾN THẮNG!" : "THUA!",
                    getWidth()/2f,
                    getHeight()/2f-20,
                    p
            );

            p.setTextSize(18);

            c.drawText(
                    "CHẠM ĐỂ CHƠI LẠI",
                    getWidth()/2f,
                    getHeight()/2f+45,
                    p
            );

            p.setTextAlign(
                    Paint.Align.LEFT
            );
        }

        void restart() {

            plants.clear();
            zombies.clear();
            bullets.clear();

            selected=0;
            sun=500;
            spawned=0;
            killed=0;

            lose=false;
            win=false;

            lastTime=
                    System.currentTimeMillis();

            spawnTime=lastTime;
        }

        @Override
        public boolean onTouchEvent(
                MotionEvent e) {

            if(e.getAction()!=
                    MotionEvent.ACTION_DOWN)
                return true;

            float x=e.getX();
            float y=e.getY();

            if(lose || win) {
                restart();
                invalidate();
                return true;
            }

            // NÚT CHỌN CÂY
            if(y>=45 && y<=155) {

                if(x<140)
                    selected=1;
                else if(x<275)
                    selected=2;
                else if(x<430)
                    selected=3;

                invalidate();
                return true;
            }

            // ĐẶT CÂY
            if(selected!=0 &&
                    x>=left &&
                    x<left+COLS*cellW &&
                    y>=top &&
                    y<top+ROWS*cellH) {

                int col=(int)
                        ((x-left)/cellW);

                int row=(int)
                        ((y-top)/cellH);

                if(!occupied(row,col)) {

                    int cost =
                            selected==1 ? 50 :
                            selected==2 ? 100 :
                            150;

                    if(sun>=cost) {

                        int max =
                                selected==1 ? 300 :
                                selected==2 ? 400 :
                                3000;

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

                invalidate();
            }

            return true;
        }
    }

    class Plant {

        int type,row,col;
        int hp,max;
        float timer;
        long lastBite;

        Plant(
                int t,
                int r,
                int c,
                int h) {

            type=t;
            row=r;
            col=c;
            hp=h;
            max=h;
        }
    }

    class Zombie {

        float x,y;
        float damageTimer;
        float jumpTimer;
        float throwTimer;

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
                boolean bo) {

            x=xx;
            y=yy;
            row=rr;

            big=b;
            bomber=bo;

            stopped=false;

            if(big) {
                hp=1000;
                max=1000;
            } else if(bomber) {
                // Zombie ném bom còn 50% máu.
                hp=150;
                max=150;
            } else {
                hp=300;
                max=300;
            }
        }
    }

    class Bullet {

        float x,y;
        int row;

        Bullet(
                float xx,
                float yy,
                int rr) {

            x=xx;
            y=yy;
            row=rr;
        }
    }
                                      }
