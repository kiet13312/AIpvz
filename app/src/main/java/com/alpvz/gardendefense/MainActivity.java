package com.alpvz.gardendefense;

import android.app.Activity;
import android.os.Bundle;
import android.content.pm.ActivityInfo;
import android.graphics.*;
import android.view.*;
import android.media.AudioManager;
import android.media.ToneGenerator;
import java.util.*;

public class MainActivity extends Activity {
    ToneGenerator sound;
    @Override protected void onCreate(Bundle b){
        super.onCreate(b); setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(5894);
        sound=new ToneGenerator(AudioManager.STREAM_MUSIC,55); setContentView(new GameView());
    }
    void beep(int t){try{if(sound!=null)sound.startTone(t,55);}catch(Exception ignored){}}
    @Override protected void onDestroy(){try{if(sound!=null)sound.release();}catch(Exception ignored){} super.onDestroy();}

    class GameView extends View {
        final Paint p=new Paint(3); final Random rnd=new Random();
        final int R=5,C=9,MAX=9, SUN=1,PEA=2,GIGA=3,CHOMP=4,REP=5,MINE=6;
        Bitmap sunImg,peaImg,gigaImg,chompImg,repImg,mineImg,zomImg,vinhImg,peaBullet;
        Bitmap sunScaled,peaScaled,gigaScaled,chompScaled,repScaled,mineScaled,bulletScaled,zomScaled,vinhScaled;
        Bitmap sunCard,peaCard,gigaCard,chompCard,repCard,mineCard;
        int cacheW=-1,cacheH=-1,tool=0;
        Plant[][] plants=new Plant[R][C]; ArrayList<Zombie> zs=new ArrayList<>(); ArrayList<Shot> shots=new ArrayList<>(); ArrayList<Coin> coins=new ArrayList<>(); ArrayList<Mine> mines=new ArrayList<>(); Mower[] mowers=new Mower[R];
        float left,top,cw,ch; int screen=0,level=1,unlocked=1,selected=0,skill=0; int sun=500,coin=0,food=3,killed=0,spawned=0; long last,spawnAt,levelStart; boolean paused=false,win=false,lose=false; Zombie houseZombie;
        GameView(){super(MainActivity.this);setFocusable(true);loadImgs();resetMowers();last=System.currentTimeMillis();}
        void loadImgs(){sunImg=load("sun");peaImg=load("peashoot");gigaImg=load("giganut");chompImg=load("chomper");repImg=load("repeater");mineImg=load("min");zomImg=load("zomplatz");vinhImg=load("zomvinhhung");peaBullet=load("gigapea");}
        Bitmap load(String n){int id=getResources().getIdentifier(n,"drawable",getPackageName());return id==0?null:BitmapFactory.decodeResource(getResources(),id);}
        @Override protected void onDraw(Canvas c){layout(); if(screen==0)drawWorld(c);else drawGame(c); if(screen==1&&!paused&&!win&&!lose){update();postInvalidateDelayed(40);}}
        void layout(){
            left=12;top=78;cw=(getWidth()-24)/9f;ch=(getHeight()-top-72)/5f;
            cacheBitmaps();
        }
        void cacheBitmaps(){
            // Keep original bitmaps and scale only at draw time; this avoids creating bitmaps every frame.
            sunScaled=sunImg; peaScaled=peaImg; gigaScaled=gigaImg; chompScaled=chompImg;
            repScaled=repImg; mineScaled=mineImg; bulletScaled=peaBullet; zomScaled=zomImg; vinhScaled=vinhImg;
            sunCard=sunImg; peaCard=peaImg; gigaCard=gigaImg; chompCard=chompImg; repCard=repImg; mineCard=mineImg;
        }
        void bg(Canvas c,int color){c.drawColor(color);}
        void txt(Canvas c,String s,float x,float y,float size,int color){p.setColor(color);p.setTextSize(size);p.setTextAlign(Paint.Align.LEFT);c.drawText(s,x,y,p);}
        void center(Canvas c,String s,float x,float y,float size,int color){p.setColor(color);p.setTextSize(size);p.setTextAlign(Paint.Align.CENTER);c.drawText(s,x,y,p);p.setTextAlign(Paint.Align.LEFT);}
        void drawWorld(Canvas c){bg(c,Color.rgb(35,110,48));center(c,"GARDEN DEFENSE",getWidth()/2,55,30,Color.WHITE);center(c,"CHỌN MÀN",getWidth()/2,88,18,Color.YELLOW);
            int cols=3; float w=getWidth()/3f,h=82; for(int i=1;i<=MAX;i++){int q=i-1,col=q%cols,row=q/cols;float x=col*w+18,y=110+row*(h+14);p.setColor(i<=unlocked?Color.rgb(80,170,75):Color.rgb(75,75,75));c.drawRoundRect(new RectF(x,y,x+w-36,y+h),14,14,p);center(c,"MÀN "+i,x+(w-36)/2,y+35,22,Color.WHITE);center(c,i<=unlocked?"CHƠI":"KHÓA",x+(w-36)/2,y+62,13,i<=unlocked?Color.YELLOW:Color.LTGRAY);}
            txt(c,"Xu: "+coin,18,getHeight()-18,18,Color.YELLOW);txt(c,"Màn đã mở: "+unlocked+"/"+MAX,getWidth()-170,getHeight()-18,15,Color.WHITE);
        }
        void drawGame(Canvas c){int[] colors={0,0xff477b35,0xff6d8b32,0xff4b7890,0xff8a6238,0xff6b4a87,0xff39756f,0xff806b3d,0xff465c88,0xff805047};bg(c,colors[level]);
            drawTop(c); for(int r=0;r<R;r++)for(int col=0;col<C;col++){float x=left+col*cw,y=top+r*ch;p.setColor((r+col)%2==0?0xff79bd4c:0xff69ad43);c.drawRect(x,y,x+cw-2,y+ch-2,p);} drawMowers(c);drawMines(c);drawPlants(c);drawShots(c);drawZombies(c);drawCoins(c);
            if(paused)drawPause(c); if(win)drawResult(c,true); if(lose)drawLose(c);
        }
        void drawTop(Canvas c){
            p.setColor(0xdd183b20);c.drawRect(0,0,getWidth(),top-5,p);
            txt(c,"MÀN "+level+"   ☀ "+sun+"   XU "+coin+"   PF "+food+"   WAVE "+Math.min(spawned/3+1,totalWaves())+"/"+totalWaves(),8,18,12,Color.WHITE);
            float w=getWidth()/9f;
            card(c,0,"SUN",SUN,sunCard,w,unlocked>=1);
            card(c,1,"PEA",PEA,peaCard,w,true);
            card(c,2,"GIGA",GIGA,gigaCard,w,unlocked>=2);
            card(c,3,"CHOMP",CHOMP,chompCard,w,unlocked>=4);
            card(c,4,"REP",REP,repCard,w,unlocked>=5);
            card(c,5,"MINE",MINE,mineCard,w,unlocked>=3);
            if(selected!=0){int idx=selected==SUN?0:selected==PEA?1:selected==GIGA?2:selected==CHOMP?3:selected==REP?4:5;outline(c,idx*w,w);}
            drawBottomControls(c);
        }
        void drawBottomControls(Canvas c){
            float y=getHeight()-58;
            // Các nút chức năng nằm dưới đáy để khay cây phía trên thoáng hơn.
                        circleButton(c,getWidth()-150,y,28,"PF",0xffffc107,Color.BLACK,tool==1);
            circleButton(c,getWidth()-82,y,28,"II",0xff455a64,Color.WHITE,false);
            circleButton(c,getWidth()-32,y,28,"X",0xff8d6e63,Color.WHITE,tool==2);
            center(c,""+food,getWidth()-150,y+42,12,Color.WHITE);
            // Xẻng nằm sát góc phải dưới màn hình.
        }
        void circleButton(Canvas c,float x,float y,float radius,String label,int color,int textColor,boolean active){
            p.setColor(0x55000000);c.drawCircle(x+2,y+3,radius+2,p);
            p.setColor(color);c.drawCircle(x,y,radius,p);
            if(active){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(4);p.setColor(Color.YELLOW);c.drawCircle(x,y,radius+3,p);p.setStyle(Paint.Style.FILL);}
            center(c,label,x,y+6,12,textColor);
        }
        void outline(Canvas c,float x,float w){p.setColor(Color.YELLOW);c.drawRoundRect(new RectF(x+1,23,x+w-1,76),7,7,p);p.setColor(0xdd183b20);c.drawRoundRect(new RectF(x+4,26,x+w-4,73),6,6,p);}
        void card(Canvas c,int i,String name,int type,Bitmap img,float w,boolean enabled){
            float x=i*w;
            p.setColor(enabled?Color.WHITE:0xff666666);
            c.drawRoundRect(new RectF(x+2,24,x+w-2,76),6,6,p);
            if(img!=null&&enabled)c.drawBitmap(img,null,new RectF(x+5,29,x+38,71),p);
            txt(c,name,x+40,56,7,enabled?Color.DKGRAY:Color.LTGRAY);
        }
        void card(Canvas c,int i,String name,int type,Bitmap img,float w){float x=i*w;p.setColor(Color.WHITE);c.drawRoundRect(new RectF(x+2,24,x+w-2,76),6,6,p);if(img!=null)c.drawBitmap(img,null,new RectF(x+5,29,x+38,71),p);txt(c,name,x+40,56,7,Color.DKGRAY);}
        void drawPlants(Canvas c){for(int r=0;r<R;r++)for(int col=0;col<C;col++){Plant a=plants[r][col];if(a==null)continue;Bitmap b=a.type==SUN?sunScaled:a.type==PEA?peaScaled:a.type==GIGA?gigaScaled:a.type==CHOMP?chompScaled:a.type==REP?repScaled:mineScaled;
                if(b!=null)c.drawBitmap(b,null,new RectF(a.x+4,a.y+4,a.x+a.w-4,a.y+a.h-4),p);bar(c,a.x+6,a.y+4,a.w-12,a.hp,a.max);if(a.type==MINE&&a.arm<30)center(c,""+Math.ceil(30-a.arm),a.x+a.w/2,a.y+20,11,Color.WHITE);}}
        void drawMines(Canvas c){for(Mine m:mines){p.setColor(m.armed?Color.RED:Color.rgb(90,60,25));c.drawCircle(m.x,m.y,12,p);}}
        void drawShots(Canvas c){for(Shot s:shots){if(bulletScaled!=null)c.drawBitmap(bulletScaled,null,new RectF(s.x-23,s.y-23,s.x+23,s.y+23),p);else{p.setColor(Color.GREEN);c.drawCircle(s.x,s.y,7,p);}}}
        void drawZombies(Canvas c){for(Zombie z:zs){if(z.dead)continue;float w=z.vinh?65:z.giga?100:z.to?82:70,h=z.vinh?86:z.giga?135:z.to?112:100;Bitmap b=z.vinh?vinhScaled:zomScaled;if(b!=null)c.drawBitmap(b,null,new RectF(z.x-w/2,z.y-h/2,z.x+w/2,z.y+h/2),p);else{p.setColor(Color.GRAY);c.drawRect(z.x-w/2,z.y-h/2,z.x+w/2,z.y+h/2,p);}bar(c,z.x-30,z.y-h/2-7,60,z.hp,z.max);}}
        void drawCoins(Canvas c){for(Coin q:coins){p.setColor(Color.YELLOW);c.drawCircle(q.x,q.y,11,p);}}
        void drawMowers(Canvas c){for(Mower m:mowers)if(m.active){p.setColor(Color.RED);c.drawRect(m.x-25,top+m.row*ch+ch*.55f-12,m.x+25,top+m.row*ch+ch*.55f+12,p);}}
        void bar(Canvas c,float x,float y,float w,int hp,int max){p.setColor(Color.RED);c.drawRect(x,y,x+w,y+5,p);p.setColor(Color.GREEN);c.drawRect(x,y,x+w*Math.max(0,Math.min(1,hp/(float)Math.max(1,max))),y+5,p);}
        void drawPause(Canvas c){p.setColor(0xbb000000);c.drawRect(0,0,getWidth(),getHeight(),p);center(c,"TẠM DỪNG",getWidth()/2,150,34,Color.WHITE);button(c,"CHƠI TIẾP",getWidth()/2-90,185,180,45);button(c,"CHƠI LẠI",getWidth()/2-90,240,180,45);button(c,"THOÁT",getWidth()/2-90,295,180,45);}
        void drawResult(Canvas c,boolean ok){p.setColor(0xbb000000);c.drawRect(0,0,getWidth(),getHeight(),p);center(c,"CHIẾN THẮNG!",getWidth()/2,155,32,Color.WHITE);button(c,"NEXT LEVEL",getWidth()/2-100,190,200,48);button(c,"THOÁT",getWidth()/2-100,250,200,48);}
        void drawLose(Canvas c){p.setColor(Color.BLACK);c.drawRect(0,0,getWidth(),getHeight(),p);if(houseZombie!=null){float w=houseZombie.vinh?65:houseZombie.giga?100:82,h=houseZombie.vinh?86:houseZombie.giga?135:112;Bitmap b=houseZombie.vinh?vinhImg:zomImg;if(b!=null)c.drawBitmap(b,null,new RectF(getWidth()/2-w/2,getHeight()/2-h/2,getWidth()/2+w/2,getHeight()/2+h/2),p);}button(c,"CHƠI LẠI",getWidth()/2-100,getHeight()-105,200,50);}
        void button(Canvas c,String s,float x,float y,float w,float h){p.setColor(0xff4c8c4f);c.drawRoundRect(new RectF(x,y,x+w,y+h),10,10,p);center(c,s,x+w/2,y+h/2+7,17,Color.WHITE);}
        int totalWaves(){return 3+level/3;} int totalZ(){return 8+level*2;}
        void update(){long now=System.currentTimeMillis();float dt=Math.min(.08f,(now-last)/1000f);last=now;if(now-spawnAt>Math.max(900,2600-level*140L)&&spawned<totalZ()){spawn();spawnAt=now;} updatePlants(dt);updateShots(dt);updateMines(dt);updateZombies(dt);updateMowers(dt);clean();if(spawned>=totalZ()&&zs.isEmpty()){win=true;unlocked=Math.max(unlocked,Math.min(MAX,level+1));} }
        void spawn(){int row=rnd.nextInt(R);boolean v=level>=4&&spawned%5==0;boolean to=!v&&level>=2&&spawned%4==0;boolean giga=!v&&level>=3&&spawned%7==0;zs.add(new Zombie(getWidth()+60,top+row*ch+ch*.5f,row,to,giga,v));spawned++;}
        void updatePlants(float dt){for(int r=0;r<R;r++)for(int c=0;c<C;c++){Plant a=plants[r][c];if(a==null)continue;a.timer-=dt;if(a.pf>0)a.pf-=dt;if(a.type==SUN&&a.timer<=0){sun+=50;a.timer=a.pf>0?.7f:5f;}if((a.type==PEA||a.type==REP)&&a.timer<=0&&rowHas(a.row)){float x=a.x+a.w*.88f,y=a.y+a.h*.43f;if(a.type==PEA)shots.add(new Shot(x,y,a.row,25));else{shots.add(new Shot(x,y,a.row,25));shots.add(new Shot(x,y,a.row,25));}a.timer=a.pf>0?.45f:(a.type==REP?.75f:1.1f);}if(a.type==CHOMP&&!a.pfMode&&a.timer<=0){Zombie z=near(a);if(z!=null){z.hp=0;a.timer=40;}}if(a.type==CHOMP&&a.pfMode){pull(a,dt);}if(a.type==GIGA){a.hp=Math.min(a.max,a.hp);}}}
        boolean rowHas(int r){for(Zombie z:zs)if(!z.dead&&z.row==r)return true;return false;}
        Zombie near(Plant a){Zombie out=null;float best=Float.MAX_VALUE;for(Zombie z:zs)if(!z.dead&&z.row==a.row&&z.x>a.x-10&&z.x<a.x+cw*1.6f&&z.x<best){best=z.x;out=z;}return out;}
        void pull(Plant a,float dt){a.pull-=dt;float mouth=a.x+a.w*.6f;boolean any=false;for(Zombie z:zs)if(!z.dead&&z.row==a.row){any=true;float d=mouth-z.x,step=450*dt;if(Math.abs(d)<=step)z.hp=0;else z.x+=d>0?step:-step;}if(!any||a.pull<=0){a.pfMode=false;a.pull=0;a.timer=0;}}
        void updateShots(float dt){Iterator<Shot> it=shots.iterator();while(it.hasNext()){Shot s=it.next();s.x+=520*dt;boolean hit=false;for(Zombie z:zs)if(!z.dead&&z.row==s.row&&Math.abs(z.x-s.x)<30){z.hp-=s.damage;hit=true;break;}if(hit||s.x>getWidth()+50)it.remove();}}
        void updateMines(float dt){Iterator<Mine> it=mines.iterator();while(it.hasNext()){Mine m=it.next();m.arm+=dt;if(!m.armed&&m.arm>=30)m.armed=true;if(m.armed){for(Zombie z:zs)if(!z.dead&&z.row==m.row&&Math.abs(z.x-m.x)<38){z.hp-=1800;m.dead=true;break;}}if(m.dead)it.remove();}}
        void updateZombies(float dt){for(Zombie z:zs){if(z.dead)continue;if(z.vinh){if(z.steps<5){z.x-=24*dt;z.steps+=24*dt;}else{z.still=true;z.bombTimer-=dt;if(z.bombTimer<=0){Plant a=firstPlant(z.row);if(a!=null){a.hp-=300;z.bombTimer=8;}}}continue;}Plant a=findPlant(z);if(a!=null){z.attack-=dt;if(z.attack<=0){a.hp-=z.giga?35:12;z.attack=z.giga?.55f:.8f;}}else z.x-=z.speed*dt;if(z.x<left-25){Mower m=mowers[z.row];if(!m.used){m.used=true;m.active=true;m.x=left-45;}else{houseZombie=cloneZombie(z);lose=true;}z.dead=true;}}}
        Zombie cloneZombie(Zombie z){return new Zombie(z.x,z.y,z.row,z.to,z.giga,z.vinh);}
        Plant findPlant(Zombie z){for(int c=0;c<C;c++){Plant a=plants[z.row][c];if(a!=null&&Math.abs(z.x-(a.x+a.w/2))<(z.giga?60:48))return a;}return null;}
        Plant firstPlant(int r){for(int c=C-1;c>=0;c--)if(plants[r][c]!=null)return plants[r][c];return null;}
        void updateMowers(float dt){for(Mower m:mowers)if(m.active){m.x+=620*dt;for(Zombie z:zs)if(!z.dead&&z.row==m.row&&Math.abs(z.x-m.x)<55)z.hp=0;if(m.x>getWidth()+80)m.active=false;}}
        void clean(){for(int r=0;r<R;r++)for(int c=0;c<C;c++)if(plants[r][c]!=null&&plants[r][c].hp<=0)plants[r][c]=null;Iterator<Zombie> it=zs.iterator();while(it.hasNext()){Zombie z=it.next();if(z.hp<=0||z.dead){if(z.hp<=0){killed++;if(rnd.nextFloat()<.2f)coins.add(new Coin(z.x,z.y));}it.remove();}}Iterator<Coin> ci=coins.iterator();while(ci.hasNext()){Coin q=ci.next();q.life-=.03f;if(q.life<=0)ci.remove();}}
        void resetMowers(){for(int r=0;r<R;r++)mowers[r]=new Mower(r);}
        void startLevel(int l){level=Math.max(1,Math.min(MAX,l));screen=1;paused=false;win=false;lose=false;houseZombie=null;selected=PEA;skill=0;tool=0;sun=500;food=3;spawned=0;killed=0;zs.clear();shots.clear();mines.clear();coins.clear();for(int r=0;r<R;r++)for(int c=0;c<C;c++)plants[r][c]=null;resetMowers();last=spawnAt=levelStart=System.currentTimeMillis();invalidate();}
        void next(){if(level<MAX){unlocked=Math.max(unlocked,level+1);startLevel(level+1);}else{screen=0;invalidate();}}
        void exitWorld(){screen=0;paused=false;win=false;lose=false;invalidate();}
        void restart(){startLevel(level);}
        void plant(int r,int c){if(selected==0)return;if(selected==REP&&!repUnlocked())return;if(selected==MINE&&!mineUnlocked())return;int cost=selected==SUN?50:selected==PEA?100:selected==GIGA?175:selected==CHOMP?150:selected==REP?200:25;if(sun<cost||plants[r][c]!=null)return;sun-=cost;int hp=selected==SUN?300:selected==PEA?300:selected==GIGA?4000:selected==CHOMP?800:selected==REP?350:1;Plant a=new Plant(selected,r,c,hp,left+c*cw,top+r*ch,cw,ch);plants[r][c]=a;if(selected==MINE)mines.add(new Mine(a.x+cw/2,a.y+ch*.55f,r));selected=0;}
        boolean repUnlocked(){return unlocked>=5;} boolean mineUnlocked(){return unlocked>=3;}
        void useFood(int r,int c){if(food<=0||plants[r][c]==null)return;Plant a=plants[r][c];food--;a.pf=8;if(a.type==GIGA){a.max=8000;a.hp=8000;}if(a.type==CHOMP){a.pfMode=true;a.pull=4;a.timer=0;}else if(a.type==PEA||a.type==REP){a.timer=0;}else if(a.type==SUN){a.timer=0;}}
        void skill(int type,int r,int c){if(type==1)useFood(r,c);else if(type==2){if(sun>=50){sun-=50;for(Zombie z:zs)if(z.row==r&&Math.abs(z.x-(left+c*cw+cw/2))<cw*2)z.hp-=800;}}else if(type==3){if(coin>=30){coin-=30;for(Zombie z:zs)if(Math.abs(z.row-r)<=1)z.speed*=.5f;}}}
        @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()!=MotionEvent.ACTION_UP)return true;float x=e.getX(),y=e.getY();
            if(screen==0){int cols=3;float w=getWidth()/3f,h=82;for(int i=1;i<=MAX;i++){int q=i-1,col=q%cols,row=q/cols;float bx=col*w+18,by=110+row*(h+14);if(x>=bx&&x<=bx+w-36&&y>=by&&y<=by+h&&i<=unlocked){startLevel(i);return true;}}return true;}
            if(lose){if(y>getHeight()-150){restart();}return true;} if(win){if(y>=180&&y<=255){next();}else if(y>=255&&y<=330)exitWorld();return true;} if(paused){if(y>=180&&y<=230){paused=false;}else if(y>=230&&y<=285)restart();else if(y>=285&&y<=345)exitWorld();invalidate();return true;}
            float w=getWidth()/9f;
            if(y>=22&&y<=79){int pick=(int)(x/w);
                tool=0;
                if(pick==0&&unlocked>=1)selected=SUN;
                else if(pick==1)selected=PEA;
                else if(pick==2&&unlocked>=2)selected=GIGA;
                else if(pick==3&&unlocked>=4)selected=CHOMP;
                else if(pick==4&&unlocked>=5)selected=REP;
                else if(pick==5&&unlocked>=3)selected=MINE;
                invalidate();return true;}
            float by=getHeight()-58;
            if(Math.hypot(x-(getWidth()-150),y-by)<=34){selected=0;tool=1;invalidate();return true;}
            if(Math.hypot(x-(getWidth()-82),y-by)<=34){paused=true;invalidate();return true;}
            if(Math.hypot(x-(getWidth()-32),y-by)<=34){selected=0;tool=2;invalidate();return true;}
            if(x<left||x>=left+C*cw||y<top||y>=top+R*ch)return true;int col=(int)((x-left)/cw),row=(int)((y-top)/ch);
            Iterator<Coin> ci=coins.iterator();while(ci.hasNext()){Coin q=ci.next();if(Math.abs(q.x-x)<30&&Math.abs(q.y-y)<30){coin+=25;ci.remove();invalidate();return true;}}
            if(tool==1){useFood(row,col);tool=0;}else if(tool==2){
                if(plants[row][col]!=null){
                    if(plants[row][col].type==MINE){Iterator<Mine> mi=mines.iterator();while(mi.hasNext()){Mine m=mi.next();if(m.row==row&&Math.abs(m.x-(plants[row][col].x+cw/2))<4)mi.remove();}}
                    plants[row][col]=null;
                }
                tool=0;
            }else if(selected!=0){plant(row,col);}invalidate();return true;
        }
        class Plant{int type,row,col,hp,max;float x,y,w,h,timer=.1f,pf=0,pull=0,arm=0;boolean pfMode=false;Plant(int t,int r,int c,int hp,float x,float y,float w,float h){type=t;row=r;col=c;this.hp=this.max=hp;this.x=x;this.y=y;this.w=w;this.h=h;}}
        class Zombie{float x,y,speed,attack=.8f,bombTimer=8,steps=0;int row,hp,max;boolean to,giga,vinh,dead,still;Zombie(float x,float y,int row,boolean to,boolean giga,boolean vinh){this.x=x;this.y=y;this.row=row;this.to=to;this.giga=giga;this.vinh=vinh;if(vinh){hp=max=3000;speed=24;}else if(giga){hp=max=1600;speed=18;}else if(to){hp=max=700;speed=20;}else{hp=max=300;speed=24;}}}
        class Shot{float x,y;int row,damage;Shot(float x,float y,int r,int d){this.x=x;this.y=y;row=r;damage=d;}}
        class Mine{float x,y,arm=0;int row;boolean armed,dead;Mine(float x,float y,int r){this.x=x;this.y=y;row=r;}}
        class Coin{float x,y,life=10;Coin(float x,float y){this.x=x;this.y=y;}}
        class Mower{int row;float x;boolean used,active;Mower(int r){row=r;x=left-45;}}
    }
                    }
                
