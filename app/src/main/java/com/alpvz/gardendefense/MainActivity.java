package com.alpvz.gardendefense;

import android.app.Activity;
import android.os.Bundle;
import android.content.pm.ActivityInfo;
import android.graphics.*;
import android.view.*;
import android.media.*;

import java.util.*;

public class MainActivity extends Activity {
    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN|
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        setContentView(new GameView());
    }

    class GameView extends View {
        Paint p=new Paint(3);
        Random rnd=new Random();
        ToneGenerator sound=new ToneGenerator(AudioManager.STREAM_MUSIC,60);

        final int R=5,C=9;
        Plant[][] plants=new Plant[R][C];
        ArrayList<Zombie> zombies=new ArrayList<>();
        ArrayList<Pea> peas=new ArrayList<>();
        ArrayList<Bomb> bombs=new ArrayList<>();
        Mower[] mowers=new Mower[R];

        float bx,by,cw,ch;
        int sun=400,pf=3,level=1,selected=1;
        long last,spawnTime,levelTime,sunTime;
        boolean win=false,lose=false;

        GameView(){
            super(MainActivity.this);
            last=System.currentTimeMillis();
            levelTime=last;
            for(int r=0;r<R;r++)mowers[r]=new Mower(r);
        }

        void beep(){
            try{sound.startTone(ToneGenerator.TONE_DTMF_1,70);}
            catch(Exception ignored){}
        }

        void layout(){
            bx=getWidth()*.03f;
            by=getHeight()*.23f;
            cw=getWidth()*.94f/C;
            ch=getHeight()*.70f/R;
        }

        @Override protected void onDraw(Canvas c){
            layout();
            long now=System.currentTimeMillis();
            float dt=Math.min(.05f,(now-last)/1000f);
            last=now;
            if(!win&&!lose)update(dt,now);
            drawTop(c);
            drawBoard(c);
            invalidate();
        }

        void update(float dt,long now){
            if(now-sunTime>3500){
                sun+=25;
                sunTime=now;
            }

            if(now-spawnTime>Math.max(900,3000-level*180)){
                spawnZombie();
                spawnTime=now;
            }

            if(now-levelTime>30000&&level<9){
                level++;
                levelTime=now;
                beep();
            }

            for(int r=0;r<R;r++){
                Mower m=mowers[r];
                if(m.active){
                    m.x+=520*dt;
                    for(Zombie z:zombies)
                        if(z.row==r&&Math.abs(z.x-m.x)<75)z.hp=0;
                    if(m.x>getWidth()+100)m.active=false;
                }
            }

            for(int r=0;r<R;r++)for(int col=0;col<C;col++){
                Plant q=plants[r][col];
                if(q==null)continue;

                q.cool-=dt;
                q.pfTime=Math.max(0,q.pfTime-dt);

                if(q.type==1&&q.cool<=0){
                    sun+=25;
                    q.cool=5;
                }

                if(q.type==2&&q.cool<=0){
                    peas.add(new Pea(
                            q.x+q.w*.86f,
                            q.y+q.h*.43f,
                            q.row,
                            q.pfTime>0?50:25));
                    q.cool=q.pfTime>0?.60f:1.15f;
                }

                if(q.type==3&&q.cool<=0){
                    Zombie z=nearest(q,85);
                    if(z!=null){
                        z.hp-=q.pfTime>0?90:45;
                        q.cool=q.pfTime>0?1f:1.8f;
                        beep();
                    }
                }

                if(q.type==4&&q.pfTime>0&&q.cool<=0){
                    Zombie z=nearest(q,180);
                    if(z!=null){
                        z.hp-=40;
                        q.cool=.55f;
                    }
                }
            }

            for(Pea b:peas){
                b.x+=430*dt;
                for(Zombie z:zombies){
                    if(z.row==b.row&&!z.dead&&Math.abs(z.x-b.x)<34){
                        z.hp-=b.damage;
                        b.dead=true;
                        break;
                    }
                }
            }

            for(Bomb b:bombs){
                b.t-=dt;
                if(b.t<=0){
                    for(Zombie z:zombies)
                        if(z.row==b.row&&Math.abs(z.x-b.x)<190)
                            z.hp-=180;
                    b.dead=true;
                    beep();
                }
            }

            for(Zombie z:zombies){
                Plant q=plantAt(z.row,z.x);

                if(q!=null&&z.x<q.x+q.w+8){
                    z.att-=dt;
                    if(z.att<=0){
                        q.hp-=z.damage;
                        z.att=.8f;
                    }
                    z.x-=5*dt;
                }else{
                    z.x-=z.speed*dt;
                }

                if(z.x<bx-55&&!mowers[z.row].used&&!mowers[z.row].active){
                    mowers[z.row].used=true;
                    mowers[z.row].active=true;
                    mowers[z.row].x=bx-90;
                    beep();
                }

                if(z.x<bx-110&&!mowers[z.row].active){
                    lose=true;
                }
            }

            clean();
        }

        Plant plantAt(int row,float x){
            for(int c=0;c<C;c++){
                Plant q=plants[row][c];
                if(q!=null&&x>q.x-25&&x<q.x+q.w+25)return q;
            }
            return null;
        }

        Zombie nearest(Plant q,float range){
            Zombie best=null;
            float d=99999;
            for(Zombie z:zombies){
                if(z.row!=q.row||z.dead)continue;
                float x=Math.abs(z.x-q.x);
                if(x<range&&x<d){
                    d=x;
                    best=z;
                }
            }
            return best;
        }

        void spawnZombie(){
            int row=rnd.nextInt(R);
            int kind=0;

            if(level>=5&&rnd.nextInt(5)==0)kind=2;
            else if(level>=3&&rnd.nextInt(4)==0)kind=1;

            zombies.add(new Zombie(row,kind,getWidth()+70));
        }

        void clean(){
            for(int r=0;r<R;r++)for(int c=0;c<C;c++){
                if(plants[r][c]!=null&&plants[r][c].hp<=0)
                    plants[r][c]=null;
            }

            Iterator<Pea> ip=peas.iterator();
            while(ip.hasNext()){
                Pea q=ip.next();
                if(q.dead||q.x>getWidth()+50)ip.remove();
            }

            Iterator<Bomb> ib=bombs.iterator();
            while(ib.hasNext()){
                if(ib.next().dead)ib.remove();
            }

            Iterator<Zombie> iz=zombies.iterator();
            while(iz.hasNext()){
                if(iz.next().hp<=0)iz.remove();
            }

            if(level==9&&zombies.isEmpty()&&
                    System.currentTimeMillis()-levelTime>8000){
                win=true;
            }
        }

        void drawTop(Canvas c){
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.rgb(35,105,45));
            c.drawRect(0,0,getWidth(),getHeight()*.20f,p);

            text(c,"M"+level+"  SUN:"+sun+"  PF:"+pf,
                    12,20,Color.WHITE,18);

            float w=getWidth()/7f;
            String[] names={"SUN","PEA","GIGA","CHOMP","PF","BOMB","NEXT"};

            for(int i=0;i<7;i++){
                p.setColor(selected==i+1?Color.YELLOW:Color.WHITE);
                c.drawRect(i*w+3,34,(i+1)*w-3,100,p);
                text(c,names[i],i*w+15,72,Color.DKGRAY,17);
            }
        }

        void drawBoard(Canvas c){
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.rgb(100,180,60));
            c.drawRect(bx,by,getWidth()-bx,getHeight()-10,p);

            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(1);

            for(int r=0;r<R;r++)for(int col=0;col<C;col++){
                p.setColor(Color.rgb(85,155,50));
                float x=bx+col*cw,y=by+r*ch;
                c.drawRect(x,y,x+cw,y+ch,p);
            }

            p.setStyle(Paint.Style.FILL);

            for(Mower m:mowers)m.draw(c);
            for(int r=0;r<R;r++)for(int col=0;col<C;col++)
                if(plants[r][col]!=null)plants[r][col].draw(c);

            for(Pea q:peas)q.draw(c);
            for(Bomb q:bombs)q.draw(c);
            for(Zombie q:zombies)q.draw(c);

            if(win||lose){
                p.setColor(0xaa000000);
                c.drawRect(0,0,getWidth(),getHeight(),p);
                text(c,win?"YOU WIN":"GAME OVER",
                        getWidth()/2-110,getHeight()/2,
                        Color.WHITE,42);
            }
        }

        void text(Canvas c,String s,float x,float y,int color,float size){
            p.setStyle(Paint.Style.FILL);
            p.setColor(color);
            p.setTextSize(size);
            c.drawText(s,x,y,p);
        }

        @Override public boolean onTouchEvent(MotionEvent e){
            if(e.getAction()!=MotionEvent.ACTION_UP)return true;

            float x=e.getX(),y=e.getY();

            if(win||lose){
                if(y<120)resetGame();
                return true;
            }

            float w=getWidth()/7f;

            if(y<110){
                if(x<7*w){
                    selected=(int)(x/w)+1;
                    beep();
                    return true;
                }
            }

            if(y<by||y>getHeight()-5)return true;

            int col=(int)((x-bx)/cw);
            int row=(int)((y-by)/ch);

            if(row<0||row>=R||col<0||col>=C)return true;

            if(selected==5){
                usePlantFood(row,col);
                return true;
            }

            if(selected==6){
                if(sun>=50){
                    sun-=50;
                    bombs.add(new Bomb(
                            row,
                            bx+col*cw+cw/2,
                            by+row*ch+ch/2));
                    beep();
                }
                return true;
            }

            if(selected==7){
                if(level<9){
                    level++;
                    levelTime=System.currentTimeMillis();
                    beep();
                }
                return true;
            }

            if(plants[row][col]==null)
                plant(selected,row,col);

            return true;
        }

        void plant(int type,int row,int col){
            int cost;

            if(type==1)cost=50;
            else if(type==2)cost=100;
            else if(type==3)cost=150;
            else if(type==4)cost=125;
            else return;

            if(sun<cost)return;

            sun-=cost;
            plants[row][col]=new Plant(type,row,col);
            beep();
        }

        void usePlantFood(int row,int col){
            Plant q=plants[row][col];

            if(q!=null&&(q.type==3||q.type==4)&&pf>0){
                pf--;
                q.pfTime=7;
                q.cool=0;
                beep();
            }
        }

        void resetGame(){
            sun=400;
            pf=3;
            level=1;
            selected=1;
            win=false;
            lose=false;

            zombies.clear();
            peas.clear();
            bombs.clear();

            for(int r=0;r<R;r++){
                mowers[r]=new Mower(r);
                for(int c=0;c<C;c++)
                    plants[r][c]=null;
            }

            levelTime=System.currentTimeMillis();
        }

        class Plant{
            int type,row,col;
            float x,y,w,h,hp,cool=0,pfTime=0;

            Plant(int t,int r,int c){
                type=t;
                row=r;
                col=c;
                x=bx+c*cw;
                y=by+r*ch;
                w=cw;
                h=ch;

                if(type==4)hp=700;
                else if(type==3)hp=220;
                else hp=180;
            }

            void draw(Canvas c){
                int color;

                if(type==1)color=Color.YELLOW;
                else if(type==2)color=Color.rgb(70,150,50);
                else if(type==3)color=Color.rgb(110,40,150);
                else color=Color.rgb(70,100,90);

                p.setColor(color);
                c.drawCircle(x+w/2,y+h/2,
                        Math.min(w,h)*.30f,p);

                p.setColor(Color.DKGRAY);
                c.drawRect(x+w*.25f,y+h*.72f,
                        x+w*.75f,y+h*.80f,p);

                String s;
                if(type==1)s="S";
                else if(type==2)s="P";
                else if(type==3)s="C";
                else s="G";

                text(c,s,x+w*.42f,y+h*.58f,Color.WHITE,28);

                float max;
                if(type==4)max=700;
                else if(type==3)max=220;
                else max=180;

                p.setColor(Color.GREEN);
                c.drawRect(x+8,y+5,
                        x+8+(w-16)*Math.max(0,hp/max),
                        y+9,p);

                if(pfTime>0)
                    text(c,"PF",x+w*.36f,y+22,
                            Color.MAGENTA,14);
            }
        }

        class Pea{
            float x,y;
            int row,damage;
            boolean dead=false;

            Pea(float a,float b,int r,int d){
                x=a;
                y=b;
                row=r;
                damage=d;
            }

            void draw(Canvas c){
                p.setColor(Color.rgb(70,220,70));
                c.drawCircle(x,y,8,p);
            }
        }

        class Bomb{
            float x,y,t=1;
            int row;
            boolean dead=false;

            Bomb(int r,float a,float b){
                row=r;
                x=a;
                y=b;
            }

            void draw(Canvas c){
                p.setColor(Color.RED);
                c.drawCircle(x,y,18,p);
                text(c,"!",x-5,y+8,Color.WHITE,22);
            }
        }

        class Zombie{
            int row,hp,kind,damage;
            float x,speed,att=0;
            boolean dead=false;

            Zombie(int r,int k,float start){
                row=r;
                kind=k;
                x=start;

                if(k==2){
                    hp=600;
                    damage=25;
                    speed=12;
                }else if(k==1){
                    hp=300;
                    damage=18;
                    speed=18;
                }else{
                    hp=180;
                    damage=10;
                    speed=24;
                }
            }

            void draw(Canvas c){
                p.setColor(
                        kind==2?Color.rgb(80,50,80):
                        kind==1?Color.rgb(70,70,70):
                        Color.GRAY);

                float cy=by+row*ch+ch*.38f;

                c.drawCircle(x,cy,20,p);
                c.drawRect(x-14,
                        by+row*ch+ch*.48f,
                        x+14,
                        by+row*ch+ch*.86f,p);

                float max=kind==2?600:kind==1?300:180;

                p.setColor(Color.GREEN);
                c.drawRect(x-25,
                        by+row*ch+5,
                        x-25+50*Math.max(0,hp/max),
                        by+row*ch+9,p);
            }
        }

        class Mower{
            int row;
            float x;
            boolean used=false,active=false;

            Mower(int r){
                row=r;
                x=bx-90;
            }

            void draw(Canvas c){
                if(used&&!active)return;

                float y=by+row*ch+ch*.65f;

                p.setColor(Color.RED);
                c.drawRect(x,y,x+60,y+ch*.23f,p);

                p.setColor(Color.DKGRAY);
                c.drawCircle(x+12,y+ch*.25f,8,p);
                c.drawCircle(x+48,y+ch*.25f,8,p);
            }
        }
    }

    @Override protected void onDestroy(){
        try{sound.release();}catch(Exception ignored){}
        super.onDestroy();
    }
        }
