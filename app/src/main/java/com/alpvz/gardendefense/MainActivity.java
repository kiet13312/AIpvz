package com.alpvz.gardendefense;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.*;
import android.view.*;
import java.util.*;

public class MainActivity extends Activity {
    @Override protected void onCreate(Bundle b){ super.onCreate(b); setContentView(new GameView()); }

    class GameView extends View {
        Paint p=new Paint(3); Random r=new Random();
        Bitmap sunImg,peaImg,gigaImg,zombieImg,bulletImg,chomperImg,bomberImg;
        ArrayList<Plant> plants=new ArrayList<>();
        ArrayList<Zombie> zombies=new ArrayList<>();
        ArrayList<Bullet> bullets=new ArrayList<>();
        ArrayList<BombBullet> bombBullets=new ArrayList<>();
        ArrayList<CoinDrop> coinDrops=new ArrayList<>();
        ArrayList<SunDrop> sunDrops=new ArrayList<>();
        int selected=0,support=0,sun=500,coins=9999,plantFood=0,spawned=0,killed=0,level=1;
        boolean lose=false,win=false,chomperUnlocked=false;
        final int ROWS=5,COLS=9; float left=20,top=210,cellW,cellH;
        long last,spawnTime;

        GameView(){ super(MainActivity.this); sunImg=load("sun"); peaImg=load("peashoot"); gigaImg=load("giganut"); zombieImg=load("zomplatz"); bulletImg=load("gigapea"); chomperImg=load("chomper"); bomberImg=load("zomvinhhung"); last=spawnTime=System.currentTimeMillis(); }
        Bitmap load(String n){int id=getResources().getIdentifier(n,"drawable",getPackageName());return id==0?null:BitmapFactory.decodeResource(getResources(),id);}

        @Override protected void onDraw(Canvas c){
            cellW=(getWidth()-40f)/COLS; cellH=(getHeight()-top-20f)/ROWS;
            p.setColor(Color.rgb(95,175,70)); c.drawRect(0,0,getWidth(),getHeight(),p);
            topUI(c); drawBoard(c); drawPlants(c); drawSunDrops(c); drawBullets(c); drawBombBullets(c); drawCoinDrops(c); drawZombies(c);
            if(!lose&&!win){update();postInvalidateDelayed(30);}else drawEnd(c);
        }

        void topUI(Canvas c){
            p.setColor(Color.rgb(55,120,50));
            c.drawRect(0,0,getWidth(),195,p);

            p.setColor(Color.WHITE);
            p.setTextSize(17);
            c.drawText("MÀN "+level+"   SUN: "+sun+"   XU: "+coins,10,25,p);

            float w=getWidth()/4f;

            card(c,0,40,1,"SUN",sunImg,w);
            card(c,w,40,2,"PEA",peaImg,w);
            card(c,2*w,40,3,"GIGA",gigaImg,w);
            if(chomperUnlocked) card(c,3*w,40,4,"CHOMP",chomperImg,w);

            skill(c,0,135,1,"SAM",30,w);
            skill(c,w,135,2,"BANG",60,w);
            skill(c,2*w,135,3,"LUA",90,w);
            foodCard(c,3*w,135,w);
        }

        void card(Canvas c,float x,float y,int t,String n,Bitmap img,float w){
            p.setColor(selected==t?Color.YELLOW:Color.WHITE);
            c.drawRoundRect(new RectF(x+2,y,x+w-2,y+85),10,10,p);

            if(img!=null)
                c.drawBitmap(img,null,new RectF(x+5,y+5,x+55,y+78),p);

            p.setColor(Color.DKGRAY);
            p.setTextSize(11);
            c.drawText(n,x+58,y+45,p);
        }

        void skill(Canvas c,float x,float y,int t,String n,int cost,float w){
            p.setColor(support==t?Color.YELLOW:Color.WHITE);
            c.drawRoundRect(new RectF(x+2,y,x+w-2,y+55),10,10,p);

            p.setColor(t==1?Color.YELLOW:t==2?Color.CYAN:Color.RED);
            c.drawCircle(x+18,y+27,11,p);

            p.setColor(Color.DKGRAY);
            p.setTextSize(10);
            c.drawText(n,x+35,y+23,p);
            c.drawText(cost+" XU",x+35,y+40,p);
        }

        void foodCard(Canvas c,float x,float y,float w){
            p.setColor(support==9?Color.YELLOW:Color.WHITE);
            c.drawRoundRect(new RectF(x+2,y,x+w-2,y+55),10,10,p);

            p.setColor(Color.rgb(70,210,90));
            c.drawCircle(x+18,y+27,11,p);

            p.setColor(Color.DKGRAY);
            p.setTextSize(10);
            c.drawText("PF",x+10,y+30,p);
            c.drawText("100 XU",x+35,y+23,p);
            c.drawText("SL:"+plantFood,x+35,y+40,p);
        }

        void drawBoard(Canvas c){for(int row=0;row<ROWS;row++)for(int col=0;col<COLS;col++){p.setColor((row+col)%2==0?Color.rgb(115,190,75):Color.rgb(105,180,68));float x=left+col*cellW,y=top+row*cellH;c.drawRect(x,y,x+cellW-2,y+cellH-2,p);}}
        void drawPlants(Canvas c){for(Plant a:plants){Bitmap img=a.type==1?sunImg:a.type==2?peaImg:a.type==3?gigaImg:chomperImg;float x=left+a.col*cellW,y=top+a.row*cellH;if(img!=null)c.drawBitmap(img,null,new RectF(x+5,y+5,x+cellW-5,y+cellH-5),p);hp(c,x+cellW*.3f,y+4,cellW*.4f,a.hp,a.max);}}
        void drawBullets(Canvas c){for(Bullet b:bullets){if(bulletImg!=null)c.drawBitmap(bulletImg,null,new RectF(b.x-21,b.y-21,b.x+21,b.y+21),p);else{p.setColor(Color.GREEN);c.drawCircle(b.x,b.y,18,p);}}}
        void drawBombBullets(Canvas c){for(BombBullet b:bombBullets){p.setColor(Color.rgb(255,70,20));c.drawCircle(b.x,b.y,13,p);p.setColor(Color.YELLOW);c.drawCircle(b.x-4,b.y-4,4,p);}}
        void drawSunDrops(Canvas c){
            for(SunDrop d:sunDrops){
                p.setColor(Color.YELLOW);
                c.drawCircle(d.x,d.y,14,p);
                p.setColor(Color.WHITE);
                c.drawCircle(d.x-4,d.y-4,4,p);
            }
        }
        void drawCoinDrops(Canvas c){
            for(CoinDrop d:coinDrops){
                p.setColor(d.type==1?Color.LTGRAY:d.type==2?Color.YELLOW:Color.CYAN);
                c.drawCircle(d.x,d.y,8,p);
            }
        }

        void drawZombies(Canvas c){for(Zombie z:zombies){float w=z.bomber?65:(z.big?105:82),h=z.bomber?85:(z.big?140:112);Bitmap img=z.bomber?bomberImg:zombieImg;if(img!=null)c.drawBitmap(img,null,new RectF(z.x-w/2,z.y-h/2,z.x+w/2,z.y+h/2),p);else{p.setColor(z.bomber?Color.DKGRAY:z.big?Color.BLACK:Color.GRAY);c.drawRoundRect(new RectF(z.x-w/2,z.y-h/2,z.x+w/2,z.y+h/2),12,12,p);}hp(c,z.x-30,z.y-(z.big?75:60),60,z.hp,z.max);}}
        void hp(Canvas c,float x,float y,float w,int v,int m){p.setColor(Color.RED);c.drawRect(x,y,x+w,y+5,p);p.setColor(Color.GREEN);float q=Math.max(0,Math.min(1,v/(float)m));c.drawRect(x,y,x+w*q,y+5,p);}

        void updateSunDrops(float dt){
            Iterator<SunDrop> it=sunDrops.iterator();
            while(it.hasNext()){
                SunDrop d=it.next();
                d.life-=dt;
                if(d.life<=0)it.remove();
            }
        }
        void addSunDrop(float x,float y){sunDrops.add(new SunDrop(x,y));}

        void updateCoinDrops(float dt){
            Iterator<CoinDrop> it=coinDrops.iterator();
            while(it.hasNext()){
                CoinDrop d=it.next();
                d.life-=dt;
                if(d.life<=0)it.remove();
            }
        }

        void update(){
            long now=System.currentTimeMillis();float dt=(now-last)/1000f;if(dt>.1f)dt=.1f;last=now;
            int total=level==1?12:level==2?15:18;
            if(spawned<total&&now-spawnTime>=5000){spawn();spawnTime=now;}
            if(r.nextInt(180)==0)addSunDrop(getWidth()/2f,top+30);
            for(Plant a:plants){
                a.timer+=dt;
                if(a.foodTime>0){ a.foodTime-=dt; if(a.foodTime<0)a.foodTime=0; }
                if(a.type==1&&a.timer>=(a.foodTime>0?0.1f:5f)){addSunDrop(left+a.col*cellW+cellW/2,top+a.row*cellH+cellH/2);a.timer=0;}
                if(a.type==2&&a.timer>=(a.foodTime>0?0.1f:1.2f)&&rowHas(a.row)){bullets.add(new Bullet(left+a.col*cellW+cellW-10,top+a.row*cellH+cellH/2,a.row));a.timer=0;}
                if(a.type==4&&a.timer>=60f){Zombie z=chompTarget(a);if(z!=null){z.hp=0;a.timer=0;}}
            }
            for(Zombie z:zombies) if(z.slow>0) z.slow-=dt;
            updateBullets();updateBombBullets();updateZombies();updateCoinDrops(dt);updateSunDrops(dt);clean();if(spawned>=total&&zombies.isEmpty()&&killed>=total)win=true;
        }
        boolean rowHas(int row){for(Zombie z:zombies)if(z.row==row)return true;return false;}
        void updateBullets(){Iterator<Bullet>it=bullets.iterator();while(it.hasNext()){Bullet b=it.next();b.x+=8;boolean hit=false;for(Zombie z:zombies)if(z.row==b.row&&Math.abs(z.x-b.x)<35){z.hp-=25;hit=true;break;}if(hit||b.x>getWidth()+60)it.remove();}}
        void updateBombBullets(){Iterator<BombBullet>it=bombBullets.iterator();while(it.hasNext()){BombBullet b=it.next();float dx=b.tx-b.x,dy=b.ty-b.y,d=(float)Math.sqrt(dx*dx+dy*dy);if(d<=b.speed||d==0){bombExplode(b.tx,b.ty,b.row);it.remove();}else{b.x+=dx/d*b.speed;b.y+=dy/d*b.speed;}}}
        void bombExplode(float x,float y,int row){int col=Math.max(0,Math.min(COLS-1,(int)((x-left)/cellW)));for(Plant a:plants)if(Math.abs(a.col-col)<=1&&Math.abs(a.row-row)<=1)a.hp-=100;}

        void updateZombies(){for(Zombie z:zombies){if(z.x<-70){lose=true;return;}
            // Zomvinhhung CHỈ CÓ Ở MÀN 3: đi tới ô 2 rồi đứng im, 8 giây ném 1 lần.
            if(z.bomber){float stop=left+cellW*1.5f;if(z.x>stop)z.x-=.6f;else{z.x=stop;z.stopped=true;}if(z.stopped){z.throwTimer+=30;if(z.throwTimer>=8000){bombBullets.add(new BombBullet(z.x,z.y,stop,z.y,z.row));z.throwTimer=0;}}continue;}
            Plant a=findPlant(z);if(a!=null){long n=System.currentTimeMillis();if(n-a.lastBite>=500){a.hp-=100;a.lastBite=n;}}
            else {float speed=z.big?0.55f:1f;if(z.slow>0)speed*=0.45f;z.x-=speed;}
        }}
        Plant findPlant(Zombie z){for(Plant a:plants)if(a.row==z.row){float px=left+a.col*cellW+cellW/2;if(Math.abs(z.x-px)<(z.big?70:55))return a;}return null;}
        Zombie chompTarget(Plant a){for(Zombie z:zombies)if(z.row==a.row){float px=left+a.col*cellW+cellW/2;if(Math.abs(z.x-px)<cellW*1.5f)return z;}return null;}

        void spawn(){int row=r.nextInt(ROWS);boolean big=false,bomber=false;if(level==2)big=spawned>=4&&spawned%4==0;if(level==3){if(spawned>0&&spawned%4==0)bomber=true;else if(spawned%3==0)big=true;}zombies.add(new Zombie(getWidth()+70,top+row*cellH+cellH/2,row,big,bomber));spawned++;}
        void clean(){Iterator<Plant>pi=plants.iterator();while(pi.hasNext())if(pi.next().hp<=0)pi.remove();Iterator<Zombie>zi=zombies.iterator();while(zi.hasNext())if(zi.next().hp<=0){zi.remove();killed++;sun+=25;int q=r.nextInt(100);if(q<1)coins+=100;else if(q<6)coins+=50;else if(q<16)coins+=25;}}
        boolean occupied(int row,int col){for(Plant a:plants)if(a.row==row&&a.col==col)return true;return false;}

        void usePlantFood(float x,float y){
            if(plantFood<=0){support=0;return;}
            int col=Math.max(0,Math.min(COLS-1,(int)((x-left)/cellW)));
            int row=Math.max(0,Math.min(ROWS-1,(int)((y-top)/cellH)));

            boolean used=false;

            // Plant Food on plant in selected tile.
            for(Plant a:plants){
                if(a.row==row&&a.col==col){
                    if(a.type==1||a.type==2){
                        a.foodTime=3f;
                        used=true;
                    }
                    break;
                }
            }
            if(used){
                plantFood--;
            }
            support=0;
        }

        void useSupport(float x,float y){int cost=support==1?30:support==2?60:90;if(coins<cost){support=0;return;}int row=Math.max(0,Math.min(ROWS-1,(int)((y-top)/cellH))),col=Math.max(0,Math.min(COLS-1,(int)((x-left)/cellW)));for(Zombie z:zombies)if(z.row>=row-1&&z.row<=row+1&&Math.abs(z.x-(left+col*cellW+cellW/2))<cellW*1.6f){if(support==1)z.hp-=500;else if(support==2)z.slow=5;else z.hp-=800;}coins-=cost;support=0;}

        void drawEnd(Canvas c){p.setColor(0xAA000000);c.drawRect(0,0,getWidth(),getHeight(),p);p.setTextAlign(Paint.Align.CENTER);p.setColor(Color.WHITE);p.setTextSize(34);if(win){if(level==2)chomperUnlocked=true;c.drawText(level==2?"MỞ KHÓA CHOMPER!":"THẮNG MÀN "+level,getWidth()/2f,getHeight()/2f-20,p);p.setTextSize(17);c.drawText(level<3?"CHẠM ĐỂ SANG MÀN TIẾP":"CHẠM ĐỂ CHƠI LẠI",getWidth()/2f,getHeight()/2f+35,p);}else{c.drawText("THUA!",getWidth()/2f,getHeight()/2f-20,p);p.setTextSize(17);c.drawText("CHẠM ĐỂ CHƠI LẠI",getWidth()/2f,getHeight()/2f+35,p);}p.setTextAlign(Paint.Align.LEFT);}
        void reset(boolean all){plants.clear();zombies.clear();bullets.clear();bombBullets.clear();coinDrops.clear();sunDrops.clear();selected=0;support=0;spawned=0;killed=0;lose=false;win=false;if(all){level=1;chomperUnlocked=false;sun=500;coins=9999;plantFood=0;}last=spawnTime=System.currentTimeMillis();addSunDrop(getWidth()/2f,top+40);}

        @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()!=MotionEvent.ACTION_DOWN)return true;float x=e.getX(),y=e.getY();if(lose){reset(true);invalidate();return true;}if(win){if(level==2)chomperUnlocked=true;if(level<3){level++;reset(false);}else reset(true);invalidate();return true;}
            float menuW=getWidth()/4f;

            // Hàng cây.
            if(y>=40&&y<=125){
                if(x<menuW)selected=1;
                else if(x<2*menuW)selected=2;
                else if(x<3*menuW)selected=3;
                else if(chomperUnlocked)selected=4;
                invalidate();
                return true;
            }

            // Hàng kỹ năng.
            if(y>=130&&y<=200){
                if(x<menuW){support=1;selected=0;return true;}
                else if(x<2*menuW){support=2;selected=0;return true;}
                else if(x<3*menuW){support=3;selected=0;return true;}
                else{
                    if(coins>=100){coins-=100;plantFood++;support=9;selected=0;}
                    return true;
                }
            }
            Iterator<SunDrop> si=sunDrops.iterator();
            while(si.hasNext()){
                SunDrop d=si.next();
                if(Math.abs(d.x-x)<35&&Math.abs(d.y-y)<35){
                    sun+=50;si.remove();invalidate();return true;
                }
            }
            Iterator<CoinDrop> ci=coinDrops.iterator();
            while(ci.hasNext()){
                CoinDrop d=ci.next();
                if(Math.abs(d.x-x)<30&&Math.abs(d.y-y)<30){
                    coins+=d.type==3?100:(d.type==2?50:25);
                    ci.remove();invalidate();return true;
                }
            }
            if(x>=left&&x<left+COLS*cellW&&y>=top&&y<top+ROWS*cellH){if(support==9){usePlantFood(x,y);invalidate();return true;}if(support!=0){useSupport(x,y);invalidate();return true;}if(selected!=0){int col=(int)((x-left)/cellW),row=(int)((y-top)/cellH);if(!occupied(row,col)){int cost=selected==1?50:selected==2?100:selected==3?150:150;if(sun>=cost){int max=selected==1?300:selected==2?400:selected==3?3000:800;plants.add(new Plant(selected,row,col,max));sun-=cost;selected=0;}}}invalidate();}return true;}
    }

    class Plant{int type,row,col,hp,max;float timer;long lastBite;float foodTime;Plant(int t,int r,int c,int h){type=t;row=r;col=c;hp=max=h;foodTime=0;}}
    class Zombie{float x,y,damageTimer,throwTimer,slow;int row,hp,max;boolean big,bomber,stopped;Zombie(float xx,float yy,int rr,boolean b,boolean bo){x=xx;y=yy;row=rr;big=b;bomber=bo;hp=max=bo?150:(b?1000:300);}}
    class Bullet{float x,y;int row;Bullet(float xx,float yy,int rr){x=xx;y=yy;row=rr;}}
    class SunDrop{float x,y,life=12f;SunDrop(float xx,float yy){x=xx;y=yy;}}
    class CoinDrop{float x,y,life=2f;int type;CoinDrop(float xx,float yy,int t){x=xx;y=yy;type=t;}}
    class BombBullet{float x,y,tx,ty,speed=5;int row;BombBullet(float xx,float yy,float a,float b,int r){x=xx;y=yy;tx=a;ty=b;row=r;}}
                }
            
