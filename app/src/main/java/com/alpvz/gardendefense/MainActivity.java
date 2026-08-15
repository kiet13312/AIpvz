package com.alpvz.gardendefense;

import android.app.Activity;
import android.os.Bundle;
import android.content.pm.ActivityInfo;
import android.graphics.*;
import android.view.*;
import java.util.*;

public class MainActivity extends Activity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(new GameView());
    }

    class GameView extends View {
        Paint p=new Paint(3); Random r=new Random();
        Bitmap sunImg,peaImg,gigaImg,zombieImg,bulletImg,chomperImg,bomberImg;
        ArrayList<Plant> plants=new ArrayList<>();
        ArrayList<Zombie> zombies=new ArrayList<>();
        ArrayList<Bullet> bullets=new ArrayList<>();
        ArrayList<BombBullet> bombs=new ArrayList<>();
        ArrayList<SunDrop> suns=new ArrayList<>();
        ArrayList<CoinDrop> coinsDrop=new ArrayList<>();
        ArrayList<Mower> mowers=new ArrayList<>();

        final int ROWS=5,COLS=9,MAX_LEVEL=9;
        float left=10,top=145,cellW,cellH;
        int selected=0,support=0,sun=500,coins=9999,plantFood=0;
        int level=1,spawned=0,killed=0,total;
        boolean lose=false,win=false,chomperUnlocked=false;
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
            last=spawnTime=System.currentTimeMillis();
        }

        Bitmap load(String n){
            int id=getResources().getIdentifier(n,"drawable",getPackageName());
            return id==0?null:BitmapFactory.decodeResource(getResources(),id);
        }

        @Override protected void onDraw(Canvas c){
            cellW=(getWidth()-20f)/COLS;
            cellH=Math.max(50,(getHeight()-top-10f)/ROWS);
            p.setColor(Color.rgb(92,170,70));
            c.drawRect(0,0,getWidth(),getHeight(),p);

            topUI(c);
            drawBoard(c);
            drawMowers(c);
            drawPlants(c);
            drawSuns(c);
            drawBullets(c);
            drawBombs(c);
            drawCoins(c);
            drawZombies(c);

            if(!lose&&!win){
                update();
                postInvalidateDelayed(30);
            }else drawEnd(c);
        }

        void topUI(Canvas c){
            p.setColor(Color.rgb(40,105,45));
            c.drawRect(0,0,getWidth(),135,p);
            p.setColor(Color.WHITE);
            p.setTextSize(17);
            c.drawText("MÀN "+level+"/9   SUN:"+sun+"   XU:"+coins+"   PF:"+plantFood,10,22,p);

            card(c,5,35,1,"SUN",sunImg);
            card(c,125,35,2,"PEA",peaImg);
            card(c,245,35,3,"GIGA",gigaImg);
            if(chomperUnlocked)card(c,365,35,4,"CHOMP",chomperImg);

            skill(c,500,35,1,"SAM",30);
            skill(c,620,35,2,"BOM",60);
            skill(c,740,35,3,"LUA",90);
            foodCard(c,860,35);
        }

        void card(Canvas c,float x,float y,int t,String s,Bitmap b){
            p.setColor(selected==t?Color.YELLOW:Color.WHITE);
            c.drawRect(x,y,x+110,y+90,p);
            if(b!=null)c.drawBitmap(b,null,
                    new RectF(x+5,y+5,x+55,y+82),p);
            p.setColor(Color.DKGRAY);
            p.setTextSize(12);
            c.drawText(s,x+62,y+48,p);
        }

        void skill(Canvas c,float x,float y,int t,String s,int cost){
            if(x>=getWidth())return;
            p.setColor(support==t?Color.YELLOW:Color.WHITE);
            c.drawRect(x,y,x+110,y+90,p);
            p.setColor(t==1?Color.YELLOW:t==2?Color.RED:Color.CYAN);
            c.drawCircle(x+25,y+30,16,p);
            p.setColor(Color.DKGRAY);
            p.setTextSize(11);
            c.drawText(s,x+47,y+34,p);
            c.drawText(cost+" XU",x+47,y+55,p);
        }

        void foodCard(Canvas c,float x,float y){
            if(x>=getWidth())return;
            p.setColor(support==9?Color.YELLOW:Color.WHITE);
            c.drawRect(x,y,x+105,y+90,p);
            p.setColor(Color.GREEN);
            c.drawCircle(x+22,y+30,15,p);
            p.setColor(Color.DKGRAY);
            p.setTextSize(10);
            c.drawText("PF",x+15,y+33,p);
            c.drawText("100 XU",x+42,y+33,p);
            c.drawText("SL:"+plantFood,x+42,y+53,p);
        }

        void drawBoard(Canvas c){
            for(int rr=0;rr<ROWS;rr++)
                for(int cc=0;cc<COLS;cc++){
                    float x=left+cc*cellW,y=top+rr*cellH;
                    p.setColor((rr+cc)%2==0
                            ?Color.rgb(115,190,75)
                            :Color.rgb(105,180,68));
                    c.drawRect(x,y,x+cellW-2,y+cellH-2,p);
                }
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
                            new RectF(x+4,y+4,x+cellW-4,y+cellH-4),p);
                hp(c,x+cellW*.25f,y+3,cellW*.5f,a.hp,a.max);
            }
        }

        void drawZombies(Canvas c){
            for(Zombie z:zombies){
                float w=z.bomber?62:z.big?100:78;
                float h=z.bomber?82:z.big?135:105;
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

                hp(c,z.x-30,z.y-h/2-7,60,z.hp,z.max);
            }
        }

        void drawBullets(Canvas c){
            for(Bullet b:bullets){
                if(bulletImg!=null)
                    c.drawBitmap(bulletImg,null,
                            new RectF(b.x-18,b.y-18,b.x+18,b.y+18),p);
                else{
                    p.setColor(Color.GREEN);
                    c.drawCircle(b.x,b.y,10,p);
                }
            }
        }

        void drawBombs(Canvas c){
            for(BombBullet b:bombs){
                p.setColor(Color.RED);
                c.drawCircle(b.x,b.y,12,p);
                p.setColor(Color.YELLOW);
                c.drawCircle(b.x-4,b.y-4,4,p);
            }
        }

        void drawSuns(Canvas c){
            for(SunDrop d:suns){
                p.setColor(Color.YELLOW);
                c.drawCircle(d.x,d.y,14,p);
            }
        }

        void drawCoins(Canvas c){
            for(CoinDrop d:coinsDrop){
                p.setColor(d.type==3?Color.CYAN:
                        d.type==2?Color.YELLOW:Color.LTGRAY);
                c.drawCircle(d.x,d.y,9,p);
            }
        }

        void drawMowers(Canvas c){
            for(Mower m:mowers){
                p.setColor(Color.DKGRAY);
                c.drawRect(m.x-24,m.y-13,m.x+24,m.y+13,p);
                p.setColor(Color.RED);
                c.drawCircle(m.x,m.y,8,p);
                p.setColor(Color.WHITE);
                c.drawCircle(m.x-15,m.y+14,7,p);
                c.drawCircle(m.x+15,m.y+14,7,p);
            }
        }

        void hp(Canvas c,float x,float y,float w,int v,int max){
            p.setColor(Color.RED);
            c.drawRect(x,y,x+w,y+5,p);
            p.setColor(Color.GREEN);
            float q=Math.max(0,
                    Math.min(1,v/(float)Math.max(1,max)));
            c.drawRect(x,y,x+w*q,y+5,p);
        }

        void update(){
            long now=System.currentTimeMillis();
            float dt=Math.min(.1f,(now-last)/1000f);
            last=now;

            total=8+level*3;

            if(spawned<total &&
                    now-spawnTime>=Math.max(1200,4200-level*300)){
                spawn();
                spawnTime=now;
            }

            if(r.nextInt(180)==0)
                suns.add(new SunDrop(getWidth()/2f,top+20));

            for(Plant a:plants){
                a.timer+=dt;

                if(a.foodTime>0){
                    a.foodTime-=dt;
                    if(a.foodTime<0)a.foodTime=0;
                }

                if(a.type==1 &&
                        a.timer>=(a.foodTime>0?.15f:5f)){
                    suns.add(new SunDrop(
                            left+a.col*cellW+cellW/2,
                            top+a.row*cellH+cellH/2));
                    a.timer=0;
                }

                if(a.type==2 &&
                        a.timer>=(a.foodTime>0?.15f:.9f) &&
                        rowHas(a.row)){
                    bullets.add(new Bullet(
                            left+a.col*cellW+cellW-8,
                            top+a.row*cellH+cellH/2,
                            a.row,25));
                    a.timer=0;
                }

                if(a.type==3 &&
                        a.timer>=(a.foodTime>0?.2f:2.2f) &&
                        rowHas(a.row)){
                    bullets.add(new Bullet(
                            left+a.col*cellW+cellW-8,
                            top+a.row*cellH+cellH/2,
                            a.row,70));
                    a.timer=0;
                }

                if(a.type==4 &&
                        a.timer>=(a.foodTime>0?8f:25f)){
                    Zombie z=chompTarget(a);
                    if(z!=null){
                        z.hp-=Math.min(z.hp,600);
                        a.timer=0;
                    }
                }
            }

            for(Zombie z:zombies){
                if(z.slow>0)z.slow-=dt;
                if(z.foodBombTimer>0)z.foodBombTimer-=dt;
            }

            updateBullets();
            updateBombs();
            updateZombies();
            updateMowers(dt);
            updateDrops(dt);
            clean();

            if(spawned>=total &&
                    zombies.isEmpty() &&
                    killed>=total)
                win=true;
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
                b.x+=b.speed;
                boolean hit=false;

                for(Zombie z:zombies){
                    if(z.row==b.row &&
                            Math.abs(z.x-b.x)<35){
                        z.hp-=b.damage;
                        hit=true;
                        break;
                    }
                }

                if(hit||b.x>getWidth()+50)
                    it.remove();
            }
        }

        void updateBombs(){
            Iterator<BombBullet>it=bombs.iterator();

            while(it.hasNext()){
                BombBullet b=it.next();
                float dx=b.tx-b.x;
                float dy=b.ty-b.y;
                float d=(float)Math.sqrt(dx*dx+dy*dy);

                if(d<=b.speed||d==0){
                    explode(b.tx,b.ty,b.row,b.enemy);
                    it.remove();
                }else{
                    b.x+=dx/d*b.speed;
                    b.y+=dy/d*b.speed;
                }
            }
        }

        void explode(float x,float y,int row,boolean enemy){
            if(enemy){
                int col=(int)((x-left)/cellW);

                for(Plant a:plants){
                    if(Math.abs(a.row-row)<=1 &&
                            Math.abs(a.col-col)<=1)
                        a.hp-=250;
                }
            }else{
                for(Zombie z:zombies){
                    if(z.row==row &&
                            Math.abs(z.x-x)<cellW*1.6f)
                        z.hp-=700;
                }
            }
        }

        void updateZombies(){
            for(Zombie z:zombies){

                if(z.bomber){
                    float stop=left+cellW*1.4f;

                    if(z.x>stop){
                        z.x-=.6f;
                    }else{
                        z.x=stop;
                        z.stopped=true;
                        z.throwTimer+=30;

                        if(z.stopped &&
                                z.throwTimer>=8000 &&
                                z.foodBombTimer<=0){

                            Plant target=findPlant(z);
                            float tx=z.x-cellW;

                            if(target!=null)
                                tx=left+target.col*cellW+cellW/2;

                            bombs.add(new BombBullet(
                                    z.x,z.y,tx,z.y,z.row,true));

                            z.throwTimer=0;
                        }
                    }

                    continue;
                }

                Plant a=findPlant(z);

                if(a!=null){
                    long n=System.currentTimeMillis();

                    if(n-a.lastBite>=500){
                        a.hp-=z.big?140:100;
                        a.lastBite=n;
                    }
                }else{
                    float speed=z.big?.55f:1f;

                    if(z.slow>0)
                        speed*=.45f;

                    z.x-=speed;
                }

                if(z.x<=left+cellW*.25f &&
                        !hasMower(z.row))
                    activateMower(z.row);

                if(z.x<-90 &&
                        !hasActiveMower(z.row))
                    lose=true;
            }
        }

        Plant findPlant(Zombie z){
            Plant best=null;
            float dist=Float.MAX_VALUE;

            for(Plant a:plants){
                if(a.row==z.row){
                    float px=left+a.col*cellW+cellW/2;
                    float d=z.x-px;

                    if(d>=-20&&d<dist){
                        dist=d;
                        best=a;
                    }
                }
            }

            return best;
        }

        Zombie chompTarget(Plant a){
            Zombie best=null;
            float dist=Float.MAX_VALUE;
            float px=left+a.col*cellW+cellW/2;

            for(Zombie z:zombies){
                if(z.row==a.row &&
                        z.x>=px-cellW*1.5f &&
                        z.x<dist){
                    dist=z.x;
                    best=z;
                }
            }

            return best;
        }

        void activateMower(int row){
            if(!hasMower(row))
                mowers.add(new Mower(
                        left+20,
                        top+row*cellH+cellH/2,
                        row));
        }

        boolean hasMower(int row){
            for(Mower m:mowers)
                if(m.row==row)return true;
            return false;
        }

        boolean hasActiveMower(int row){
            for(Mower m:mowers)
                if(m.row==row&&m.active)return true;
            return false;
        }

        void updateMowers(float dt){
            Iterator<Mower>it=mowers.iterator();

            while(it.hasNext()){
                Mower m=it.next();

                if(!m.active)continue;

                m.x+=11;

                for(Zombie z:zombies){
                    if(z.row==m.row &&
                            Math.abs(z.x-m.x)<55)
                        z.hp=0;
                }

                if(m.x>getWidth()+60)
                    it.remove();
            }
        }

        void spawn(){
            int row=r.nextInt(ROWS);
            boolean big=false,bomber=false;

            if(level>=2&&spawned%4==0)
                big=true;

            if(level>=3&&spawned>1&&spawned%5==0)
                bomber=true;

            zombies.add(new Zombie(
                    getWidth()+80,
                    top+row*cellH+cellH/2,
                    row,big,bomber));

            spawned++;
        }

        void clean(){
            Iterator<Plant>pi=plants.iterator();

            while(pi.hasNext())
                if(pi.next().hp<=0)
                    pi.remove();

            Iterator<Zombie>zi=zombies.iterator();

            while(zi.hasNext()){
                Zombie z=zi.next();

                if(z.hp<=0){
                    float x=z.x,y=z.y;
                    zi.remove();
                    killed++;
                    sun+=25;

                    int q=r.nextInt(100);

                    if(q<2){
                        coins+=100;
                        coinsDrop.add(new CoinDrop(x,y,3));
                    }else if(q<8){
                        coins+=50;
                        coinsDrop.add(new CoinDrop(x,y,2));
                    }else if(q<20){
                        coins+=25;
                        coinsDrop.add(new CoinDrop(x,y,1));
                    }
                }
            }
        }

        void updateDrops(float dt){
            Iterator<SunDrop>si=suns.iterator();

            while(si.hasNext()){
                SunDrop d=si.next();
                d.life-=dt;
                if(d.life<=0)si.remove();
            }

            Iterator<CoinDrop>ci=coinsDrop.iterator();

            while(ci.hasNext()){
                CoinDrop d=ci.next();
                d.life-=dt;
                if(d.life<=0)ci.remove();
            }
        }

        void usePlantFood(float x,float y){
            if(plantFood<=0){
                support=0;
                return;
            }

            int col=(int)((x-left)/cellW);
            int row=(int)((y-top)/cellH);
            boolean used=false;

            if(row>=0&&row<ROWS&&col>=0&&col<COLS){
                for(Plant a:plants){
                    if(a.row==row&&a.col==col){
                        a.foodTime=3f;
                        used=true;

                        if(a.type==4){
                            a.hp=Math.min(a.max,a.hp+400);
                            a.timer=0;
                        }

                        break;
                    }
                }
            }

            if(used)plantFood--;
            support=0;
        }

        void useSupport(float x,float y){
            int row=Math.max(0,
                    Math.min(ROWS-1,
                    (int)((y-top)/cellH)));

            int col=Math.max(0,
                    Math.min(COLS-1,
                    (int)((x-left)/cellW)));

            if(support==2){
                if(coins<60){
                    support=0;
                    return;
                }

                coins-=60;

                float tx=left+col*cellW+cellW/2;
                float ty=top+row*cellH+cellH/2;

                bombs.add(new BombBullet(
                        tx,ty,tx,ty,row,false));

            }else{
                int cost=support==1?30:90;

                if(coins<cost){
                    support=0;
                    return;
                }

                coins-=cost;

                for(Zombie z:zombies){
                    if(z.row>=row-1&&
                            z.row<=row+1&&
                            Math.abs(z.x-
                            (left+col*cellW+cellW/2))
                            <cellW*1.7f){

                        z.hp-=support==1?500:800;
                    }
                }
            }

            support=0;
        }

        boolean occupied(int row,int col){
            for(Plant a:plants)
           
