package com.alpvz.gardendefense;

import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;

public class MainActivity extends Activity {
    GameView game;
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        game = new GameView(this);
        setContentView(game);
    }

    static class GameView extends View {
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Random rnd = new Random();
        final int ROWS=5,COLS=9;
        final int[][] plants=new int[ROWS][COLS];
        final boolean[] mowerReady={true,true,true,true,true};
        final ArrayList<Zombie> zombies=new ArrayList<>();
        final ArrayList<Shot> shots=new ArrayList<>();
        final ArrayList<Bomb> bombs=new ArrayList<>();
        final ArrayList<Mower> mowers=new ArrayList<>();
        int sun=400,pf=0,level=1,selected=1,kills=0,tick=0;
        boolean gameOver;
        float ox,oy,cw,ch;
        long last=System.currentTimeMillis();
        ToneGenerator tone;

        GameView(Context c){
            super(c);
            setFocusable(true);
            tone=new ToneGenerator(AudioManager.STREAM_MUSIC,60);
        }

        void snd(int t){
            try{tone.startTone(t,70);}catch(Exception ignored){}
        }

        @Override protected void onDraw(Canvas c){
            int w=getWidth(),h=getHeight();
            c.drawColor(Color.rgb(45,125,50));

            p.setColor(Color.rgb(30,85,35));
            c.drawRect(0,0,w,48,p);
            p.setColor(Color.WHITE);
            p.setTextSize(15);
            c.drawText("M"+level+"  SUN:"+sun+"  PF:"+pf+"  K:"+kills,10,30,p);

            int top=55,cardH=58,cardW=w/5;
            String[] names={"SUN 50","PEA 100","GIGA 175","CHOMP 150","BOMB 75"};

            for(int i=0;i<5;i++){
                p.setColor(selected==i+1?Color.YELLOW:Color.WHITE);
                c.drawRect(i*cardW,top,(i+1)*cardW-3,top+cardH,p);
                p.setColor(Color.DKGRAY);
                p.setTextSize(14);
                c.drawText(names[i],i*cardW+10,top+35,p);
            }

            p.setColor(Color.WHITE);
            p.setTextSize(12);
            c.drawText("Chomper Plant Food: tap CHOMP with PF to eat nearby zombies",10,135,p);

            ox=10;
            oy=160;
            cw=(w-20f)/COLS;
            ch=(h-oy-10f)/ROWS;

            for(int r=0;r<ROWS;r++)
                for(int col=0;col<COLS;col++){
                    p.setColor((r+col)%2==0?
                            Color.rgb(105,185,65):Color.rgb(95,175,58));
                    c.drawRect(ox+col*cw,oy+r*ch,
                            ox+(col+1)*cw,oy+(r+1)*ch,p);
                    drawPlant(c,r,col,plants[r][col]);
                }

            for(Zombie z:zombies)drawZombie(c,z);

            for(Shot s:shots){
                p.setColor(Color.YELLOW);
                c.drawCircle(s.x,s.y,5,p);
            }

            for(Bomb b:bombs){
                p.setColor(Color.RED);
                c.drawCircle(b.x,b.y,9,p);
            }

            for(Mower m:mowers){
                p.setColor(Color.DKGRAY);
                c.drawRect(m.x,m.y-12,m.x+42,m.y+12,p);
                p.setColor(Color.BLACK);
                c.drawCircle(m.x+8,m.y+12,6,p);
                c.drawCircle(m.x+34,m.y+12,6,p);
            }

            if(gameOver){
                p.setColor(Color.argb(190,0,0,0));
                c.drawRect(0,0,w,h,p);
                p.setColor(Color.WHITE);
                p.setTextSize(30);
                c.drawText("GAME OVER - TAP TO RESTART",
                        w/2-220,h/2,p);
            }
        }

        void drawPlant(Canvas c,int r,int col,int t){
            if(t==0)return;
            float x=ox+(col+.5f)*cw;
            float y=oy+(r+.5f)*ch;

            p.setColor(Color.rgb(35,150,45));
            c.drawRect(x-3,y+5,x+3,y+25,p);

            if(t==1){
                p.setColor(Color.YELLOW);
                c.drawCircle(x,y,18,p);
            }else if(t==2){
                p.setColor(Color.rgb(100,190,80));
                c.drawOval(x-22,y-11,x+22,y+11,p);
            }else if(t==3){
                p.setColor(Color.rgb(70,70,70));
                c.drawCircle(x,y,22,p);
                p.setColor(Color.LTGRAY);
                c.drawCircle(x,y,14,p);
            }else{
                p.setColor(Color.MAGENTA);
                c.drawCircle(x,y,20,p);
                p.setColor(Color.WHITE);
                c.drawCircle(x+8,y-2,5,p);
            }
        }

        void drawZombie(Canvas c,Zombie z){
            p.setColor(Color.rgb(125,125,125));
            c.drawOval(z.x-14,z.y-25,z.x+14,z.y+25,p);
            p.setColor(Color.GREEN);
            c.drawRect(z.x-20,z.y-33,z.x+20,z.y-28,p);
            p.setColor(Color.BLACK);
            p.setTextSize(9);
            c.drawText(z.kind,z.x-24,z.y+39,p);
        }

        @Override public boolean onTouchEvent(MotionEvent e){
            if(e.getAction()!=MotionEvent.ACTION_DOWN)return true;

            float x=e.getX(),y=e.getY();

            if(gameOver){
                restart();
                return true;
            }

            if(y>=55&&y<113){
                selected=(int)(x/(getWidth()/5f))+1;
                return true;
            }

            if(y>=oy){
                int col=(int)((x-ox)/cw);
                int r=(int)((y-oy)/ch);
                if(r>=0&&r<ROWS&&col>=0&&col<COLS)
                    place(r,col);
            }
            return true;
        }

        void place(int r,int col){
            if(selected==5){
                throwBomb(r,col);
                return;
            }

            if(plants[r][col]!=0)return;

            int cost=selected==1?50:
                    selected==2?100:
                    selected==3?175:150;

            if(sun<cost)return;

            sun-=cost;
            plants[r][col]=selected;
            snd(ToneGenerator.TONE_PROP_BEEP);

            if(selected==4&&pf>0){
                pf--;
                chomperPF(r,col);
            }
        }

        void chomperPF(int r,int col){
            float cx=ox+(col+.5f)*cw;

            for(Iterator<Zombie> it=zombies.iterator();it.hasNext();){
                Zombie z=it.next();
                if(z.row==r&&Math.abs(z.x-cx)<cw*2){
                    it.remove();
                    kills++;
                }
            }
            snd(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD);
        }

        void throwBomb(int r,int col){
            if(sun<75)return;

            sun-=75;
            bombs.add(new Bomb(
                    ox+(col+.5f)*cw,
                    oy+(r+.5f)*ch,r));
            snd(ToneGenerator.TONE_CDMA_CALLDROP_LITE);
        }

        void spawn(){
            int r=rnd.nextInt(ROWS);
            boolean boss=rnd.nextInt(6)==0;

            zombies.add(new Zombie(
                    r,
                    getWidth()+35,
                    oy+(r+.5f)*ch,
                    boss?260:100,
                    boss?"Zomvinhhung":"ZOMBIE"));
        }

        void update(){
            long now=System.currentTimeMillis();
            if(now-last<35)return;
            last=now;
            tick++;

            if(tick%100==0)spawn();

            if(tick%90==0)
                for(int r=0;r<ROWS;r++)
                    for(int col=0;col<COLS;col++)
                        if(plants[r][col]==1)sun+=25;

            for(int r=0;r<ROWS;r++)
                for(int col=0;col<COLS;col++){
                    int t=plants[r][col];

                    if(t==2&&tick%28==0)
                        shots.add(new Shot(
                                ox+(col+.7f)*cw,
                                oy+(r+.5f)*ch,r));

                    if(t==3&&tick%45==0)
                        for(Zombie z:zombies)
                            if(z.row==r&&z.x>ox+(col+.5f)*cw){
                                z.hp-=35;
                                break;
                            }

                    if(t==4&&tick%70==0)
                        for(Zombie z:zombies)
                            if(z.row==r&&
                               Math.abs(z.x-(ox+(col+.5f)*cw))<cw*1.5f){
                                z.hp=0;
                                break;
                            }
                }

            for(Iterator<Shot> it=shots.iterator();it.hasNext();){
                Shot s=it.next();
                s.x+=9;
                boolean hit=false;

                for(Zombie z:zombies)
                    if(z.row==s.row&&Math.abs(z.x-s.x)<22){
                        z.hp-=20;
                        hit=true;
                        break;
                    }

                if(hit||s.x>getWidth()+20)it.remove();
            }

            for(Iterator<Bomb> it=bombs.iterator();it.hasNext();){
                Bomb b=it.next();
                b.x+=10;

                if(b.x>getWidth()-90){
                    for(Zombie z:zombies)
                        if(z.row==b.row&&Math.abs(z.x-b.x)<95)
                            z.hp-=180;

                    it.remove();
                    snd(ToneGenerator.TONE_CDMA_HIGH_SRR);
                }
            }

            for(Iterator<Zombie> it=zombies.iterator();it.hasNext();){
                Zombie z=it.next();

                if(z.hp<=0){
                    it.remove();
                    kills++;
                    sun+=25;
                    continue;
                }

                int col=(int)((z.x-ox)/cw);

                if(col>=0&&col<COLS&&plants[z.row][col]!=0){
                    if(tick%35==0)plants[z.row][col]=0;
                }else{
                    z.x-=z.kind.equals("Zomvinhhung")?.7f:1.1f;
                }

                if(z.x<ox-30){
                    if(mowerReady[z.row]){
                        mowerReady[z.row]=false;
                        mowers.add(new Mower(
                                z.row,
                                ox-60,
                                oy+(z.row+.5f)*ch));
                        it.remove();
                        snd(ToneGenerator.TONE_CDMA_NETWORK_BUSY);
                    }else{
                        gameOver=true;
                        it.remove();
                        break;
                    }
                }
            }

            for(Iterator<Mower> it=mowers.iterator();it.hasNext();){
                Mower m=it.next();
                m.x+=15;

                for(Iterator<Zombie> iz=zombies.iterator();iz.hasNext();){
                    Zombie z=iz.next();
                    if(z.row==m.row&&Math.abs(z.x-m.x)<48){
                        iz.remove();
                        kills++;
                        sun+=25;
                    }
                }

                if(m.x>getWidth()+60)it.remove();
            }

            if(tick%600==0&&level<9){
                level++;
                sun+=100;
                pf++;
                Arrays.fill(mowerReady,true);
                snd(ToneGenerator.TONE_PROP_ACK);
            }

            invalidate();
        }

        void restart(){
            for(int[] a:plants)Arrays.fill(a,0);
            zombies.clear();
            shots.clear();
            bombs.clear();
            mowers.clear();
            Arrays.fill(mowerReady,true);

            sun=400;
            pf=0;
            level=1;
            kills=0;
            tick=0;
            gameOver=false;
            invalidate();
        }

        @Override protected void onAttachedToWindow(){
            super.onAttachedToWindow();
            post(new Runnable(){
                @Override public void run(){
                    update();
                    postDelayed(this,16);
                }
            });
        }

        static class Zombie{
            int row;
            float x,y,hp;
            String kind;

            Zombie(int r,float x,float y,float hp,String k){
                row=r;
                this.x=x;
                this.y=y;
                this.hp=hp;
                kind=k;
            }
        }

        static class Shot{
            float x,y;
            int row;

            Shot(float x,float y,int r){
                this.x=x;
                this.y=y;
                row=r;
            }
        }

        static class Bomb{
            float x,y;
            int row;

            Bomb(float x,float y,int r){
                this.x=x;
                this.y=y;
                row=r;
            }
        }

        static class Mower{
            int row;
            float x,y;

            Mower(int r,float x,float y){
                row=r;
                this.x=x;
                this.y=y;
            }
        }
    }
                             }
