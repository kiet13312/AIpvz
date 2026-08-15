package com.alpvz.gardendefense;

import android.app.Activity;
import android.os.Bundle;
import android.content.pm.ActivityInfo;
import android.graphics.*;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.view.*;
import java.util.*;

public class MainActivity extends Activity {
    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(5894);
        setContentView(new GameView());
    }

    class GameView extends View {
        Paint p=new Paint(3);
        Random r=new Random();
        ArrayList<Plant> plants=new ArrayList<>();
        ArrayList<Zombie> zombies=new ArrayList<>();
        ArrayList<Bullet> bullets=new ArrayList<>();
        ArrayList<Bomb> bombs=new ArrayList<>();
        ArrayList<CoinDrop> drops=new ArrayList<>();
        Mower[] mowers=new Mower[5];

        Bitmap sunImg,peaImg,gigaImg,zombieImg,bulletImg,chomperImg,bomberImg;
        ToneGenerator tone;

        int selected=0,support=0,sun=500,coins=9999,food=0;
        int spawned=0,killed=0,level=1;
        boolean win=false,lose=false,chomperUnlocked=false;

        final int ROWS=5,COLS=9;
        float left=18,top=185,cellW,cellH;
        long last,spawnTime;

        GameView(){
            super(MainActivity.this);
            sunImg=load("sun");
            peaImg=load("peashoot");
            gigaImg=load("giganut");
            zombieImg=load("zomplatz");
            bulletImg=load("gigapea");
            chomperImg=load("chomper");
            bomberImg=load("zomvinhhung");
            tone=new ToneGenerator(AudioManager.STREAM_MUSIC,70);
            resetMowers();
            last=spawnTime=System.currentTimeMillis();
        }

        Bitmap load(String n){
            int id=getResources().getIdentifier(n,"drawable",getPackageName());
            return id==0?null:BitmapFactory.decodeResource(getResources(),id);
        }

        void beep(int t){
            try{tone.startTone(t,80);}catch(Exception e){}
        }

        @Override protected void onDraw(Canvas c){
            c.drawColor(Color.rgb(28,75,30));
            cellW=(getWidth()-left*2)/COLS;
            cellH=(getHeight()-top-15)/ROWS;

            topUI(c);
            drawPlants(c);
            drawMowers(c);
            drawBullets(c);
            drawBombs(c);
            drawDrops(c);
            drawZombies(c);

            if(!win&&!lose){
                update();
                postInvalidateDelayed(30);
            }else drawEnd(c);
        }

        void topUI(Canvas c){
            p.setColor(Color.rgb(35,100,42));
            c.drawRect(0,0,getWidth(),165,p);
            p.setColor(Color.WHITE);
            p.setTextSize(17);
            c.drawText("MÀN "+level+"   SUN: "+sun+
                    "   XU: "+coins+"   PF: "+food,10,24,p);

            float w=getWidth()/4f;
            card(c,0,38,w,1,"SUN",sunImg);
            card(c,w,38,w,2,"PEA",peaImg);
            card(c,2*w,38,w,3,"GIGA",gigaImg);
            card(c,3*w,38,w,chomperUnlocked?4:0,
                    chomperUnlocked?"CHOMP":"LOCK",chomperImg);

            skill(c,0,128,w,1,"SAM",30);
            skill(c,w,128,w,2,"BANG",60);
            skill(c,2*w,128,w,3,"LUA",90);
            skill(c,3*w,128,w,9,"PF "+food,100);
        }

        void card(Canvas c,float x,float y,float w,int t,String n,Bitmap b){
            p.setColor(selected==t&&t!=0?Color.YELLOW:Color.WHITE);
            c.drawRect(x+2,y,x+w-2,y+82,p);
            if(b!=null&&t!=0)
                c.drawBitmap(b,null,new RectF(x+8,y+4,x+70,y+78),p);
            p.setColor(Color.DKGRAY);
            p.setTextSize(12);
            c.drawText(n,x+78,y+44,p);
        }

        void skill(Canvas c,float x,float y,float w,int t,String n,int cost){
            p.setColor(support==t?Color.YELLOW:Color.WHITE);
            c.drawRect(x+2,y,x+w-2,y+32,p);
            p.setColor(Color.DKGRAY);
            p.setTextSize(11);
            c.drawText(n+" "+cost+" XU",x+10,y+21,p);
        }

        void drawPlants(Canvas c){
            for(Plant a:plants){
                Bitmap b=a.type==1?sunImg:
                        a.type==2?peaImg:
                        a.type==3?gigaImg:chomperImg;
                float x=left+a.col*cellW;
                float y=top+a.row*cellH;

                if(b!=null)
                    c.drawBitmap(b,null,
                            new RectF(x+5,y+4,x+cellW-5,y+cellH-5),p);

                hp(c,x+8,y+3,cellW-16,a.hp,a.max);

                if(a.foodTime>0){
                    p.setColor(Color.YELLOW);
                    c.drawCircle(x+cellW-18,y+18,6,p);
                }
            }
        }

        void drawZombies(Canvas c){
            for(Zombie z:zombies){
                float w=z.bomber?60:z.big?90:70;
                float h=z.bomber?80:z.big?125:100;
                Bitmap b=z.bomber?bomberImg:zombieImg;

                if(b!=null)
                    c.drawBitmap(b,null,
                            new RectF(z.x-w/2,z.y-h/2,
                                    z.x+w/2,z.y+h/2),p);
                else{
                    p.setColor(z.bomber?Color.DKGRAY:
                            z.big?Color.BLACK:Color.GRAY);
                    c.drawRect(z.x-w/2,z.y-h/2,
                            z.x+w/2,z.y+h/2,p);
                }

                hp(c,z.x-28,z.y-h/2-7,56,z.hp,z.max);
            }
        }

        void drawBullets(Canvas c){
            for(Bullet b:bullets){
                if(b.img!=null)
                    c.drawBitmap(b.img,null,
                            new RectF(b.x-14,b.y-14,b.x+14,b.y+14),p);
                else{
                    p.setColor(b.damage>=100?Color.YELLOW:Color.GREEN);
                    c.drawCircle(b.x,b.y,10,p);
                }
            }
        }

        void drawBombs(Canvas c){
            for(Bomb b:bombs){
                p.setColor(Color.RED);
                c.drawCircle(b.x,b.y,11,p);
                p.setColor(Color.YELLOW);
                c.drawCircle(b.x-3,b.y-3,3,p);
            }
        }

        void drawDrops(Canvas c){
            for(CoinDrop d:drops){
                p.setColor(Color.YELLOW);
                c.drawCircle(d.x,d.y,9,p);
            }
        }

        void drawMowers(Canvas c){
            for(Mower m:mowers){
                if(m.used)continue;
                p.setColor(Color.RED);
                c.drawRect(m.x-25,m.y-14,m.x+25,m.y+14,p);
                p.setColor(Color.BLACK);
                c.drawCircle(m.x-14,m.y+15,7,p);
                c.drawCircle(m.x+14,m.y+15,7,p);
            }
        }

        void hp(Canvas c,float x,float y,float w,int v,int max){
            p.setColor(Color.RED);
            c.drawRect(x,y,x+w,y+5,p);
            p.setColor(Color.GREEN);
            float q=Math.max(0,Math.min(1,v/(float)max));
            c.drawRect(x,y,x+w*q,y+5,p);
        }

        void update(){
            long now=System.currentTimeMillis();
            float dt=Math.min(.1f,(now-last)/1000f);
            last=now;

            int total=10+level*3;
            long delay=Math.max(1200,4200-level*300);

            if(spawned<total&&now-spawnTime>=delay){
                spawn();
                spawnTime=now;
            }

            for(Plant a:plants){
                a.timer+=dt;

                if(a.foodTime>0){
                    a.foodTime-=dt;
                    if(a.foodTime<0)a.foodTime=0;
                }

                if(a.type==1){
                    float cd=a.foodTime>0?.12f:5f;
                    if(a.timer>=cd){
                        sun+=a.foodTime>0?100:50;
                        a.timer=0;
                        beep(ToneGenerator.TONE_PROP_BEEP);
                    }
                }

                if(a.type==2&&a.timer>=(a.foodTime>0?.12f:1.2f)
                        &&rowHas(a.row)){
                    bullets.add(new Bullet(
                            left+a.col*cellW+cellW-10,
                            top+a.row*cellH+cellH/2,
                            a.row,bulletImg,
                            a.foodTime>0?70:25));
                    a.timer=0;
                }

                if(a.type==3&&a.timer>=(a.foodTime>0?.15f:2.2f)
                        &&rowHas(a.row)){
                    bullets.add(new Bullet(
                            left+a.col*cellW+cellW-10,
                            top+a.row*cellH+cellH/2,
                            a.row,bulletImg,
                            a.foodTime>0?180:100));
                    a.timer=0;
                }

                if(a.type==4){
                    if(a.foodTime>0){
                        if(a.timer>=.45f){
                            Zombie z=chompTarget(a);
                            if(z!=null){
                                z.hp=0;
                                a.timer=0;
                                beep(ToneGenerator.TONE_PROP_BEEP2);
                            }
                        }
                    }else if(a.timer>=4f){
                        Zombie z=chompTarget(a);
                        if(z!=null){
                            z.hp=0;
                            a.timer=0;
                            beep(ToneGenerator.TONE_PROP_BEEP);
                        }
                    }
                }
            }

            for(Zombie z:zombies){
                if(z.slow>0)z.slow-=dt;
                if(z.bombCD>0)z.bombCD-=dt;
            }

            updateBullets();
            updateBombs();
            updateZombies();
            updateMowers();
            clean();

            if(spawned>=total&&zombies.isEmpty()&&killed>=total){
                win=true;
                beep(ToneGenerator.TONE_PROP_ACK);
            }
        }

        boolean rowHas(int row){
            for(Zombie z:zombies)
                if(z.row==row)return true;
            return false;
        }

        void updateBullets(){
            Iterator<Bullet>it=bullets.iterator();

            while(it.hasNext()){
                Bullet b=it.next();
                b.x+=9;
                boolean hit=false;

                for(Zombie z:zombies){
                    if(z.row==b.row&&Math.abs(z.x-b.x)<35){
                        z.hp-=b.damage;
                        hit=true;
                        break;
                    }
                }

                if(hit||b.x>getWidth()+50)it.remove();
            }
        }

        void updateBombs(){
            Iterator<Bomb>it=bombs.iterator();

            while(it.hasNext()){
                Bomb b=it.next();
                float dx=b.tx-b.x,dy=b.ty-b.y;
                float d=(float)Math.sqrt(dx*dx+dy*dy);

                if(d<=b.speed){
                    explode(b.tx,b.ty,b.row);
                    it.remove();
                }else{
                    b.x+=dx/d*b.speed;
                    b.y+=dy/d*b.speed;
                }
            }
        }

        void explode(float x,float y,int row){
            for(Zombie z:zombies)
                if(z.row>=row-1&&z.row<=row+1&&
                        Math.abs(z.x-x)<cellW*1.5f)
                    z.hp-=450;

            beep(ToneGenerator.TONE_PROP_BEEP2);
        }

        void updateZombies(){
            for(Zombie z:zombies){
                if(z.x<left-20){
                    if(!mowers[z.row].used&&!mowers[z.row].active){
                        mowers[z.row].active=true;
                        beep(ToneGenerator.TONE_PROP_ACK);
                    }else if(mowers[z.row].used){
                        lose=true;
                        beep(ToneGenerator.TONE_PROP_NACK);
                        return;
                    }
                }

                if(z.bomber){
                    float stop=left+cellW*1.5f;

                    if(z.x>stop)z.x-=.6f;
                    else z.stopped=true;

                    if(z.stopped&&z.bombCD<=0){
                        bombs.add(new Bomb(
                                z.x,z.y,
                                Math.max(left,z.x-cellW*1.5f),
                                z.y,z.row));
                        z.bombCD=8;
                    }

                    continue;
                }

                Plant a=findPlant(z);

                if(a!=null){
                    long n=System.currentTimeMillis();

                    if(n-a.lastBite>500){
                        a.hp-=z.big?180:100;
                        a.lastBite=n;
                    }
                }else{
                    float speed=z.big?.55f:1f;
                    if(z.slow>0)speed*=.45f;
                    z.x-=speed;
                }
            }
        }

        void updateMowers(){
            for(Mower m:mowers){
                if(!m.active||m.used)continue;

                m.x+=13;

                for(Zombie z:zombies)
                    if(z.row==m.row&&Math.abs(z.x-m.x)<45)
                        z.hp=0;

                if(m.x>getWidth()+60)m.used=true;
            }
        }

        Plant findPlant(Zombie z){
            for(Plant a:plants)
                if(a.row==z.row&&
                        Math.abs(z.x-
                        (left+a.col*cellW+cellW/2))<
                        (z.big?65:50))
                    return a;

            return null;
        }

        Zombie chompTarget(Plant a){
            for(Zombie z:zombies)
                if(z.row==a.row&&
                        Math.abs(z.x-
                        (left+a.col*cellW+cellW/2))<
                        cellW*1.4f)
                    return z;

            return null;
        }

        void spawn(){
            int row=r.nextInt(ROWS);
            boolean big=level>=2&&spawned%4==0;
            boolean bomber=level>=3&&spawned%5==0;

            zombies.add(new Zombie(
                    getWidth()+70,
                    top+row*cellH+cellH/2,
                    row,big,bomber));

            spawned++;
        }

        void clean(){
            Iterator<Plant>pi=plants.iterator();
            while(pi.hasNext())
                if(pi.next().hp<=0)pi.remove();

            Iterator<Zombie>zi=zombies.iterator();

            while(zi.hasNext()){
                Zombie z=zi.next();

                if(z.hp<=0){
                    zi.remove();
                    killed++;
                    sun+=25;

                    if(r.nextInt(100)<15)
                        drops.add(new CoinDrop(z.x,z.y));
                }
            }

            Iterator<CoinDrop>di=drops.iterator();

            while(di.hasNext()){
                CoinDrop d=di.next();
                if(d.life--<=0)di.remove();
            }
        }

        boolean occupied(int row,int col){
            for(Plant a:plants)
                if(a.row==row&&a.col==col)return true;
            return false;
        }

        void useFood(float x,float y){
            if(food<=0){
                support=0;
                return;
            }

            int col=Math.max(0,Math.min(COLS-1,
                    (int)((x-left)/cellW)));

            int row=Math.max(0,Math.min(ROWS-1,
                    (int)((y-top)/cellH)));

            for(Plant a:plants){
                if(a.row==row&&a.col==col){
                    food--;

                    if(a.type==3){
                        a.foodTime=6f;
                        a.timer=0;

                        for(int i=0;i<3;i++)
                            bullets.add(new Bullet(
                                    left+a.col*cellW+cellW-10,
                                    top+a.row*cellH+
                                    cellH/2+(i-1)*18,
                                    a.row,bulletImg,180));

                    }else if(a.type==4){
                        a.foodTime=6f;
                        a.timer=0;

                        for(Zombie z:zombies)
                            if(z.row==a.row&&
                                    Math.abs(z.x-
                                    (left+a.col*cellW+cellW/2))<
                                    cellW*2.2f)
                                z.hp=0;

                    }else{
                        a.foodTime=6f;
                        a.timer=0;
                    }

                    support=0;
                    beep(ToneGenerator.TONE_PROP_BEEP2);
                    return;
                }
            }

            support=0;
        }

        void useSupport(float x,float y){
            int cost=support==1?30:support==2?60:90;

            if(coins<cost){
                support=0;
                return;
            }

            int row=Math.max(0,Math.min(ROWS-1,
                    (int)((y-top)/cellH)));

            int col=Math.max(0,Math.min(COLS-1,
                    (int)((x-left)/cellW)));

            for(Zombie z:zombies){
                if(z.row>=row-1&&z.row<=row+1&&
                        Math.abs(z.x-
                        (left+col*cellW+cellW/2))<
                        cellW*1.7f){

                    if(support==1)z.hp-=500;
                    else if(support==2)z.slow=5;
                    else z.hp-=800;
                }
            }

            coins-=cost;
            support=0;
            beep(ToneGenerator.TONE_PROP_BEEP2);
        }

        void resetMowers(){
            for(int i=0;i<ROWS;i++)
                mowers[i]=new Mower(
                        i,left-35,
                        top+i*cellH+cellH/2);
        }

        void reset(boolean all){
            plants.clear();
            zombies.clear();
            bullets.clear();
            bombs.clear();
            drops.clear();

            selected=0;
            support=0;
            spawned=0;
            killed=0;
            win=false;
            lose=false;

            if(all){
                level=1;
                chomperUnlocked=false;
                sun=500;
                coins=9999;
                food=0;
            }

            resetMowers();
            last=spawnTime=System.currentTimeMillis();
        }

        void drawEnd(Canvas c){
            p.setColor(0xAA000000);
            c.drawRect(0,0,getWidth(),getHeight(),p);
            p.setTextAlign(Paint.Align.CENTER);
            p.setColor(Color.WHITE);
            p.setTextSize(32);

            c.drawText(win?
                    (level<9?"THẮNG MÀN "+level:
                    "HOÀN THÀNH 9 MÀN"):
                    "THUA!",
                    getWidth()/2f,getHeight()/2f,p);

            p.setTextSize(17);
            c.drawText("CHẠM ĐỂ TIẾP TỤC",
                    getWidth()/2f,
                    getHeight()/2f+40,p);

            p.setTextAlign(Paint.Align.LEFT);
        }

        @Override public boolean onTouchEvent(MotionEvent e){
            if(e.getAction()!=MotionEvent.ACTION_DOWN)return true;

            float x=e.getX(),y=e.getY();

            if(win){
                if(level==2)chomperUnlocked=true;

                if(level<9){
                    level++;
                    reset(false);
                }else reset(true);

                invalidate();
                return true;
            }

            if(lose){
                reset(true);
                invalidate();
                return true;
            }

            float w=getWidth()/4f;

            if(y>=38&&y<120){
                if(x<w)selected=1;
                else if(x<2*w)selected=2;
                else if(x<3*w)selected=3;
                else if(chomperUnlocked)selected=4;

                invalidate();
                return true;
            }

            if(y>=128&&y<165){
                if(x<w)support=1;
                else if(x<2*w)support=2;
           else if(x<3*w)support=3;
else if(coins>=100){
    coins-=100;
    food++;
    support=9;
}
invalidate();
return true;
}

Iterator<CoinDrop> ci=drops.iterator();
while(ci.hasNext()){
    CoinDrop d=ci.next();
    if(Math.abs(d.x-x)<30&&Math.abs(d.y-y)<30){
        coins+=25;
        ci.remove();
        beep(ToneGenerator.TONE_PROP_BEEP);
        return true;
    }
}

if(x>=left&&x<left+COLS*cellW&&
        y>=top&&y<top+ROWS*cellH){

    if(support==9){
        useFood(x,y);
        invalidate();
        return true;
    }

    if(support>0){
        useSupport(x,y);
        invalidate();
        return true;
    }

    if(selected>0){
        int col=(int)((x-left)/cellW);
        int row=(int)((y-top)/cellH);

        int cost=selected==1?50:
                selected==2?100:
                selected==3?150:150;

        int max=selected==1?300:
                selected==2?400:
                selected==3?3000:800;

        if(!occupied(row,col)&&sun>=cost){
            plants.add(new Plant(selected,row,col,max));
            sun-=cost;
            selected=0;
            beep(ToneGenerator.TONE_PROP_BEEP);
        }
    }
}

invalidate();
return true;
}
                }
