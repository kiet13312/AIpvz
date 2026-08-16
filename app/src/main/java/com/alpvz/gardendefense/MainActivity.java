package com.alpvz.gardendefense;

import android.app.*;
import android.os.*;
import android.content.pm.ActivityInfo;
import android.graphics.*;
import android.view.*;
import java.util.*;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(5894);
        setContentView(new GameView());
    }

    class GameView extends View {
        Paint p=new Paint(3);
        Random r=new Random();

        final int R=5,C=9;
        final int SUN=1,PEA=2,GIGA=3,CHOMP=4;
        final int MAX=9;

        Bitmap sunImg,peaImg,gigaImg,chompImg,zomImg,bulletImg;

        Plant[][] plants=new Plant[R][C];
        ArrayList<Zombie> zombies=new ArrayList<>();
        ArrayList<Shot> shots=new ArrayList<>();

        float left,top,cw,ch;
        int sun=500;
        int level=1;
        int selected=PEA;
        int killed=0;
        int spawned=0;
        int total=10;

        long last,spawnTime;
        boolean win=false,lose=false;

        GameView(){
            super(MainActivity.this);
            setFocusable(true);
            sunImg=img("sun");
            peaImg=img("peashoot");
            gigaImg=img("giganut");
            chompImg=img("chomper");
            zomImg=img("zomplatz");
            bulletImg=img("gigapea");
            last=spawnTime=System.currentTimeMillis();
        }

        Bitmap img(String n){
            int id=getResources().getIdentifier(
                    n,"drawable",getPackageName());
            return id==0?null:BitmapFactory.decodeResource(
                    getResources(),id);
        }

        @Override protected void onDraw(Canvas c){
            layout();
            drawGame(c);
            if(!win&&!lose){
                update();
                postInvalidateDelayed(30);
            }
        }

        void layout(){
            left=10;
            top=75;
            cw=(getWidth()-20)/9f;
            ch=(getHeight()-top-10)/5f;
        }

        void drawGame(Canvas c){
            c.drawColor(Color.rgb(75,145,65));

            p.setColor(0xff17451f);
            c.drawRect(0,0,getWidth(),top-3,p);

            text(c,"MÀN "+level+"   ☀ "+sun+
                    "   "+killed+"/"+total,
                    10,22,18,Color.WHITE);

            card(c,0,"SUN",SUN,sunImg);
            card(c,1,"PEA",PEA,peaImg);
            card(c,2,"GIGA",GIGA,gigaImg);
            card(c,3,"CHOMP",CHOMP,chompImg);

            for(int y=0;y<R;y++)
                for(int x=0;x<C;x++){
                    float xx=left+x*cw;
                    float yy=top+y*ch;
                    p.setColor((x+y)%2==0?
                            0xff79bd4c:0xff69ad43);
                    c.drawRect(xx,yy,
                            xx+cw-2,yy+ch-2,p);
                }

            drawPlants(c);
            drawShots(c);
            drawZombies(c);

            if(win) end(c,"THẮNG!");
            if(lose) end(c,"THUA!");
        }

        void card(Canvas c,int i,String name,int type,Bitmap b){
            float w=getWidth()/4f;
            float x=i*w;

            p.setColor(type==selected?
                    Color.YELLOW:Color.WHITE);
            c.drawRoundRect(
                    new RectF(x+2,28,x+w-2,68),
                    6,6,p);

            if(b!=null)
                c.drawBitmap(b,null,
                        new RectF(x+5,30,x+38,66),p);

            p.setColor(Color.BLACK);
            p.setTextSize(10);
            c.drawText(name,x+43,54,p);
        }

        void drawPlants(Canvas c){
            for(int y=0;y<R;y++)
                for(int x=0;x<C;x++){
                    Plant a=plants[y][x];
                    if(a==null)continue;

                    Bitmap b=a.type==SUN?sunImg:
                            a.type==GIGA?gigaImg:
                            a.type==CHOMP?chompImg:peaImg;

                    if(b!=null)
                        c.drawBitmap(b,null,
                            new RectF(
                                a.x+5,a.y+5,
                                a.x+cw-5,a.y+ch-5),p);

                    hp(c,a.x+8,a.y+4,
                            cw-16,a.life,a.max);
                }
        }

        void drawShots(Canvas c){
            for(Shot s:shots){
                if(bulletImg!=null)
                    c.drawBitmap(bulletImg,null,
                        new RectF(
                            s.x-12,s.y-12,
                            s.x+12,s.y+12),p);
                else{
                    p.setColor(Color.GREEN);
                    c.drawCircle(s.x,s.y,7,p);
                }
            }
        }

        void drawZombies(Canvas c){
            for(Zombie z:zombies){
                if(z.dead)continue;

                float w=z.boss?cw*1.15f:60;
                float h=z.boss?ch*1.15f:90;

                if(zomImg!=null)
                    c.drawBitmap(zomImg,null,
                        new RectF(
                            z.x-w/2,z.y-h/2,
                            z.x+w/2,z.y+h/2),p);
                else{
                    p.setColor(Color.GRAY);
                    c.drawRect(
                        z.x-w/2,z.y-h/2,
                        z.x+w/2,z.y+h/2,p);
                }

                hp(c,z.x-w/2,z.y-h/2-7,
                        w,z.life,z.max);
            }
        }

        void hp(Canvas c,float x,float y,float w,
                int v,int m){
            p.setColor(Color.RED);
            c.drawRect(x,y,x+w,y+5,p);
            p.setColor(Color.GREEN);
            float q=Math.max(0,
                    Math.min(1,v/(float)Math.max(1,m)));
            c.drawRect(x,y,x+w*q,y+5,p);
        }

        void text(Canvas c,String s,float x,float y,
                  float size,int color){
            p.setColor(color);
            p.setTextSize(size);
            c.drawText(s,x,y,p);
          }        void update(){
            long now=System.currentTimeMillis();
            float dt=Math.min(.08f,
                    (now-last)/1000f);
            last=now;

            if(now-spawnTime>1800 &&
                    spawned<total){
                spawn();
                spawned++;
                spawnTime=now;
            }

            for(int y=0;y<R;y++)
                for(int x=0;x<C;x++){
                    Plant a=plants[y][x];
                    if(a==null)continue;

                    a.timer+=dt;

                    if(a.type==SUN && a.timer>5){
                        sun+=50;
                        a.timer=0;
                    }

                    if((a.type==PEA ||
                        a.type==GIGA) && a.timer>.9f){
                        shoot(a);
                        a.timer=0;
                    }
                }

            for(Shot s:shots){
                s.x+=500*dt;

                for(Zombie z:zombies){
                    if(z.dead)continue;
                    if(z.row!=s.row)continue;

                    if(Math.abs(s.x-z.x)<35){
                        z.life-=s.damage;
                        s.dead=true;
                        break;
                    }
                }
            }

            for(Zombie z:zombies){
                if(z.dead)continue;

                z.attack-=dt;

                if(z.boss){
                    z.x+=z.dir*60*dt;

                    if(z.x<left+cw){
                        z.x=left+cw;
                        z.dir=1;
                    }

                    if(z.x>left+cw*8){
                        z.x=left+cw*8;
                        z.dir=-1;
                    }
                }else{
                    boolean block=false;

                    for(int x=0;x<C;x++){
                        Plant a=plants[z.row][x];
                        if(a!=null &&
                           z.x>a.x &&
                           z.x<a.x+cw){
                            block=true;

                            if(z.attack<=0){
                                a.life-=20;
                                z.attack=1;
                            }

                            break;
                        }
                    }

                    if(!block)
                        z.x-=z.speed*dt;

                    if(z.x<left-40)
                        lose=true;
                }
            }

            clean();

            if(level==9 &&
               spawned>=total &&
               zombies.size()==0)
                win=true;

            if(level<9 &&
               spawned>=total &&
               zombies.size()==0)
                win=true;
        }

        void spawn(){
            int row=r.nextInt(R);
            boolean boss=level==9 &&
                    spawned==total-1;

            Zombie z=new Zombie();
            z.row=row;
            z.x=getWidth()+60;
            z.y=top+row*ch+ch/2;
            z.boss=boss;

            if(boss){
                z.life=9000;
                z.max=9000;
                z.speed=0;
                z.dir=-1;
            }else{
                z.life=100+level*20;
                z.max=z.life;
                z.speed=18+level*2;
            }

            zombies.add(z);
        }

        void shoot(Plant a){
            shots.add(new Shot(
                a.x+cw-5,
                a.y+ch/2,
                a.row,
                a.type==GIGA?45:20));
        }

        void clean(){
            Iterator<Shot> si=shots.iterator();

            while(si.hasNext()){
                Shot s=si.next();

                if(s.dead || s.x>getWidth()+50)
                    si.remove();
            }

            Iterator<Zombie> zi=zombies.iterator();

            while(zi.hasNext()){
                Zombie z=zi.next();

                if(z.life<=0){
                    z.dead=true;
                    killed++;
                    sun+=25;
                    zi.remove();
                }
            }
        }

        @Override public boolean onTouchEvent(
                MotionEvent e){

            if(e.getAction()!=MotionEvent.ACTION_DOWN)
                return true;

            float x=e.getX();
            float y=e.getY();

            if(win){
                if(level<9){
                    level++;
                    total=10+level;
                    spawned=0;
                    killed=0;
                    win=false;
                    lose=false;
                    zombies.clear();
                    shots.clear();
                    clearPlants();
                    last=spawnTime=
                        System.currentTimeMillis();
                }
                return true;
            }

            if(lose){
                level=1;
                total=10;
                spawned=0;
                killed=0;
                sun=500;
                win=false;
                lose=false;
                zombies.clear();
                shots.clear();
                clearPlants();
                last=spawnTime=
                    System.currentTimeMillis();
                return true;
            }

            if(y<72){
                int i=(int)(x/(getWidth()/4f));

                if(i==0)selected=SUN;
                if(i==1)selected=PEA;
                if(i==2)selected=GIGA;
                if(i==3)selected=CHOMP;

                return true;
            }

            if(y>=top && y<top+ch*R){
                int col=(int)((x-left)/cw);
                int row=(int)((y-top)/ch);

                if(col>=0&&col<C&&
                   row>=0&&row<R&&
                   plants[row][col]==null){

                    int cost=selected==SUN?50:
                             selected==PEA?100:
                             selected==GIGA?200:150;

                    if(sun>=cost){
                        sun-=cost;
                        plants[row][col]=
                            new Plant(selected,row,col);
                    }
                }
            }

            return true;
        }

        void clearPlants(){
            for(int y=0;y<R;y++)
                Arrays.fill(plants[y],null);
        }

        void end(Canvas c,String s){
            p.setColor(0xdd000000);
            c.drawRect(0,0,getWidth(),
                    getHeight(),p);

            p.setTextAlign(Paint.Align.CENTER);
            p.setColor(Color.WHITE);
            p.setTextSize(38);
            c.drawText(s,getWidth()/2,
                    getHeight()/2,p);

            p.setTextSize(18);

            if(win)
                c.drawText(level<9?
                    "CHẠM ĐỂ SANG MÀN":
                    "HOÀN THÀNH 9 MÀN!",
                    getWidth()/2,
                    getHeight()/2+45,p);
            else
                c.drawText("CHẠM ĐỂ CHƠI LẠI",
                    getWidth()/2,
                    getHeight()/2+45,p);

            p.setTextAlign(Paint.Align.LEFT);
        }

        class Plant{
            int type,row,col;
            int life,max=500;
            float x,y,timer;

            Plant(int t,int r,int c){
                type=t;
                row=r;
                col=c;
                life=t==GIGA?1200:
                     t==CHOMP?800:500;
                max=life;
                x=left+c*cw;
                y=top+r*ch;
            }
        }

        class Zombie{
            int row,life=100,max=100;
            float x,y,speed=20;
            float attack=0;
            float dir=1;
            boolean dead=false;
            boolean boss=false;
        }

        class Shot{
            float x,y;
            int row,damage;
            boolean dead=false;

            Shot(float X,float Y,int R,int D){
                x=X;
                y=Y;
                row=R;
                damage=D;
            }
        }
    }
                      }
