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
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(new GameView());
    }

    class GameView extends View {
        Paint p = new Paint(3);
        Random r = new Random();
        Bitmap sunImg, peaImg, gigaImg, bulletImg, zombieImg;
        ArrayList<Plant> plants = new ArrayList<>();
        ArrayList<Zombie> zombies = new ArrayList<>();
        ArrayList<Bullet> bullets = new ArrayList<>();

        int selected = 0, sun = 500, spawned = 0, killed = 0;
        final int ROWS = 5, COLS = 9, TOTAL = 10;
        float left = 20, top = 185, cellW, cellH;
        long last, spawnTime;
        boolean lose = false, win = false;

        GameView() {
            super(MainActivity.this);
            sunImg = load("sun");
            peaImg = load("peashoot");
            gigaImg = load("giganut");
            bulletImg = load("gigapea");
            zombieImg = load("zomplatz");
            last = spawnTime = System.currentTimeMillis();
        }

        Bitmap load(String n) {
            int id = getResources().getIdentifier(n, "drawable", getPackageName());
            return id == 0 ? null : BitmapFactory.decodeResource(getResources(), id);
        }

        protected void onDraw(Canvas c) {
            cellW = (getWidth() - 40f) / COLS;
            cellH = (getHeight() - top - 20f) / ROWS;

            p.setColor(Color.rgb(95,175,70));
            c.drawRect(0,0,getWidth(),getHeight(),p);
            topUI(c);
            board(c);
            drawPlants(c);
            drawBullets(c);
            drawZombies(c);

            if (!lose && !win) {
                update();
                postInvalidateDelayed(30);
            } else end(c);
        }

        void topUI(Canvas c) {
            p.setColor(Color.rgb(55,120,50));
            c.drawRect(0,0,getWidth(),165,p);

            p.setColor(Color.WHITE);
            p.setTextSize(24);
            c.drawText("SUN: " + sun,18,32,p);

            float bx = 220, bw = getWidth()-240;
            p.setColor(Color.DKGRAY);
            c.drawRoundRect(new RectF(bx,10,bx+bw,32),10,10,p);
            p.setColor(Color.GREEN);
            float prog = Math.min(1f,killed/(float)TOTAL);
            c.drawRoundRect(new RectF(bx,10,bx+bw*prog,32),10,10,p);

            p.setColor(Color.WHITE);
            p.setTextSize(13);
            c.drawText(killed+"/"+TOTAL,bx+8,26,p);

            card(c,10,55,1,"SUN",sunImg);
            card(c,145,55,2,"PEA",peaImg);
            card(c,280,55,3,"GIGA",gigaImg);
        }

        void card(Canvas c,float x,float y,int type,String name,Bitmap img) {
            p.setColor(selected==type ? Color.YELLOW : Color.WHITE);
            c.drawRoundRect(new RectF(x,y,x+120,y+90),12,12,p);
            if(img!=null)c.drawBitmap(img,null,new RectF(x+5,y+5,x+60,y+80),p);
            p.setColor(Color.DKGRAY);
            p.setTextSize(14);
            c.drawText(name,x+67,y+50,p);
        }

        void board(Canvas c) {
            for(int row=0;row<ROWS;row++)
                for(int col=0;col<COLS;col++) {
                    p.setColor((row+col)%2==0
                            ? Color.rgb(115,190,75)
                            : Color.rgb(105,180,68));
                    float x=left+col*cellW, y=top+row*cellH;
                    c.drawRect(x,y,x+cellW-2,y+cellH-2,p);
                }
        }

        void drawPlants(Canvas c) {
            for(Plant a:plants) {
                Bitmap img=a.type==1?sunImg:a.type==2?peaImg:gigaImg;
                float x=left+a.col*cellW,y=top+a.row*cellH;
                if(img!=null)c.drawBitmap(img,null,
                        new RectF(x+5,y+5,x+cellW-5,y+cellH-5),p);
                hp(c,x+cellW*.3f,y+4,cellW*.4f,a.hp,a.max);
            }
        }

        void drawBullets(Canvas c) {
            for(Bullet b:bullets) {
                if(bulletImg!=null)c.drawBitmap(bulletImg,null,
                        new RectF(b.x-21,b.y-21,b.x+21,b.y+21),p);
                else { p.setColor(Color.GREEN); c.drawCircle(b.x,b.y,18,p); }
            }
        }

        void drawZombies(Canvas c) {
            for(Zombie z:zombies) {
                float w=z.big?110:82,h=z.big?150:112;
                if(zombieImg!=null)c.drawBitmap(zombieImg,null,
                        new RectF(z.x-w/2,z.y-h/2,z.x+w/2,z.y+h/2),p);
                else { p.setColor(Color.GRAY);
                    c.drawRoundRect(new RectF(z.x-w/2,z.y-h/2,z.x+w/2,z.y+h/2),12,12,p); }
                float hw=z.big?70:55;
                hp(c,z.x-hw/2,z.y-(z.big?80:62),hw,z.hp,z.max);
            }
        }

        void hp(Canvas c,float x,float y,float w,int value,int max) {
            p.setColor(Color.RED);
            c.drawRect(x,y,x+w,y+5,p);
            p.setColor(Color.GREEN);
            float q=Math.max(0,Math.min(1,value/(float)max));
            c.drawRect(x,y,x+w*q,y+5,p);
        }

        void update() {
            long now=System.currentTimeMillis();
            float dt=(now-last)/1000f;
            if(dt>.1f)dt=.1f;
            last=now;

            if(spawned<TOTAL && now-spawnTime>=4000) {
                spawn();
                spawnTime=now;
            }

            for(Plant a:plants) {
                a.timer+=dt;
                if(a.type==1 && a.timer>=5) {
                    sun+=100;
                    a.timer=0;
                }
                if(a.type==2 && a.timer>=1.2f && rowHasZombie(a.row)) {
                    bullets.add(new Bullet(
                            left+a.col*cellW+cellW-10,
                            top+a.row*cellH+cellH/2,
                            a.row));
                    a.timer=0;
                }
            }

            for(Zombie z:zombies) if(z.big) {
                z.damageTimer+=dt;
                if(z.damageTimer>=1) {
                    for(Plant a:plants)a.hp-=10;
                    z.damageTimer=0;
                }
            }

            bullets();
            zombies();
            clean();

            if(spawned>=TOTAL && zombies.isEmpty() && killed>=TOTAL)win=true;
        }

        boolean rowHasZombie(int row) {
            for(Zombie z:zombies)if(z.row==row)return true;
            return false;
        }

        void bullets() {
            Iterator<Bullet> it=bullets.iterator();
            while(it.hasNext()) {
                Bullet b=it.next();
                b.x+=8;
                boolean hit=false;
                for(Zombie z:zombies)
                    if(z.row==b.row && Math.abs(z.x-b.x)<35) {
                        z.hp-=25;
                        hit=true;
                        break;
                    }
                if(hit||b.x>getWidth()+60)it.remove();
            }
        }

        void zombies() {
            for(Zombie z:zombies) {
                if(z.x<-70){lose=true;return;}
                Plant a=findPlant(z);
                if(a!=null) {
                    if(!z.big) {
                        long now=System.currentTimeMillis();
                        if(now-a.lastBite>=500) {
                            a.hp-=100;
                            a.lastBite=now;
                        }
                    } else {
                        z.jumpTimer+=30;
                        if(z.jumpTimer>=1200) {
                            z.x-=cellW*1.5f;
                            z.jumpTimer=0;
                        }
                    }
                } else z.x -= z.big ? 0.6f : 1.2f;
            }
        }

        Plant findPlant(Zombie z) {
            for(Plant a:plants) {
                if(a.row!=z.row)continue;
                float px=left+a.col*cellW+cellW/2;
                if(Math.abs(z.x-px)<(z.big?70:55))return a;
            }
            return null;
        }

        void clean() {
            Iterator<Plant> pi=plants.iterator();
            while(pi.hasNext())if(pi.next().hp<=0)pi.remove();

            Iterator<Zombie> zi=zombies.iterator();
            while(zi.hasNext())if(zi.next().hp<=0) {
                zi.remove();
                killed++;
                sun+=25;
            }

            Iterator<Bullet> bi=bullets.iterator();
            while(bi.hasNext())if(bi.next().x>getWidth()+60)bi.remove();
        }

        void spawn() {
            int row=r.nextInt(ROWS);
            boolean big=r.nextInt(5)==0;
            zombies.add(new Zombie(getWidth()+60,
                    top+row*cellH+cellH/2,row,big));
            spawned++;
        }

        boolean occupied(int row,int col) {
            for(Plant a:plants)
                if(a.row==row&&a.col==col)return true;
            return false;
        }

        void end(Canvas c) {
            p.setColor(0xAA000000);
            c.drawRect(0,0,getWidth(),getHeight(),p);
            p.setTextAlign(Paint.Align.CENTER);
            p.setColor(Color.WHITE);
            p.setTextSize(42);
            c.drawText(win?"CHIẾN THẮNG!":"THUA!",
                    getWidth()/2f,getHeight()/2f-20,p);
            p.setTextSize(18);
            c.drawText("CHẠM ĐỂ CHƠI LẠI",
                    getWidth()/2f,getHeight()/2f+45,p);
            p.setTextAlign(Paint.Align.LEFT);
        }

        void restart() {
            plants.clear(); zombies.clear(); bullets.clear();
            selected=0; sun=500; spawned=0; killed=0;
            lose=false;win=false;
            last=spawnTime=System.currentTimeMillis();
        }

        public boolean onTouchEvent(MotionEvent e) {
            if(e.getAction()!=MotionEvent.ACTION_DOWN)return true;
            float x=e.getX(),y=e.getY();

            if(lose||win){restart();invalidate();return true;}

            if(y>=45&&y<=155) {
                if(x<140)selected=1;
                else if(x<275)selected=2;
                else if(x<430)selected=3;
                invalidate();
                return true;
            }

            if(selected!=0&&x>=left&&x<left+COLS*cellW&&
                    y>=top&&y<top+ROWS*cellH) {
                int col=(int)((x-left)/cellW);
                int row=(int)((y-top)/cellH);

                if(!occupied(row,col)) {
                    int cost=selected==1?50:selected==2?100:150;
                    if(sun>=cost) {
                        int max=selected==1?300:selected==2?400:3000;
                        plants.add(new Plant(selected,row,col,max));
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
        int type,row,col,hp,max;
        float timer;
        long lastBite;
        Plant(int t,int r,int c,int h) {
            type=t;row=r;col=c;hp=h;max=h;
        }
    }

    class Zombie {
        float x,y,damageTimer,jumpTimer;
        int row,hp,max;
        boolean big;
        Zombie(float xx,float yy,int rr,boolean b) {
            x=xx;y=yy;row=rr;big=b;
            hp=max=b?1000:300;
        }
    }

    class Bullet {
        float x,y;
        int row;
        Bullet(float xx,float yy,int rr) {
            x=xx;y=yy;row=rr;
        }
    }
                }
                                  
