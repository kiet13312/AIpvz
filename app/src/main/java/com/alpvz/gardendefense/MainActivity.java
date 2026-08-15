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
    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        setContentView(new Game());
    }

    class Game extends View {
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        Random rnd=new Random();
        ToneGenerator sound;

        ArrayList<Plant> plants=new ArrayList<>();
        ArrayList<Zombie> zombies=new ArrayList<>();
        ArrayList<Pea> peas=new ArrayList<>();
        ArrayList<Bomb> bombs=new ArrayList<>();
        Mower[] mowers=new Mower[5];

        Bitmap sunImg,peaImg,gigaImg,chompImg,zombieImg,bombImg;
        int sun=500,coins=9999,food=0,level=1;
        int selected=0,action=0,spawned=0,kills=0;
        boolean win=false,lose=false;

        final int ROWS=5,COLS=9;
        float L=20,T=175,CW,CH;
        long last,spawnClock;

        Game(){
            super(MainActivity.this);
            sunImg=img("sun");
            peaImg=img("peashoot");
            gigaImg=img("giganut");
            chompImg=img("chomper");
            zombieImg=img("zomplatz");
            bombImg=img("zomvinhhung");
            try{
                sound=new ToneGenerator(AudioManager.STREAM_MUSIC,70);
            }catch(Exception e){}
            resetMowers();
            last=spawnClock=System.currentTimeMillis();
        }

        Bitmap img(String n){
            int id=getResources().getIdentifier(n,"drawable",getPackageName());
            return id==0?null:BitmapFactory.decodeResource(getResources(),id);
        }

        void beep(int type){
            if(sound!=null)try{sound.startTone(type,70);}catch(Exception e){}
        }

        @Override protected void onDraw(Canvas c){
            super.onDraw(c);
            CW=(getWidth()-L*2)/COLS;
            CH=(getHeight()-T-10)/ROWS;

            p.setColor(Color.rgb(35,100,40));
            c.drawRect(0,0,getWidth(),getHeight(),p);

            drawTop(c);
            drawBoard(c);
            drawMowers(c);
            drawPlants(c);
            drawPeas(c);
            drawBombs(c);
            drawZombies(c);

            if(win||lose)end(c);
            else{
                tick();
                postInvalidateDelayed(30);
            }
        }

        void drawTop(Canvas c){
            p.setColor(Color.rgb(28,82,32));
            c.drawRect(0,0,getWidth(),165,p);

            p.setColor(Color.WHITE);
            p.setTextSize(16);
            c.drawText("MÀN "+level+"   SUN:"+sun+
                    "   XU:"+coins+"   PF:"+food,10,22,p);

            float w=getWidth()/4f;
            card(c,0,"SUN",sunImg,1);
            card(c,w,"PEA",peaImg,2);
            card(c,w*2,"GIGA",gigaImg,3);
            card(c,w*3,"CHOMP",chompImg,4);

            button(c,0,"BOM 60",1);
            button(c,w,"LỬA 90",2);
            button(c,w*2,"PF 100",3);
            button(c,w*3,"RESET",4);
        }

        void card(Canvas c,float x,String s,Bitmap b,int type){
            float w=getWidth()/4f;
            p.setColor(selected==type?Color.YELLOW:Color.WHITE);
            c.drawRect(x+2,35,x+w-2,112,p);
            if(b!=null)c.drawBitmap(b,null,
                    new RectF(x+8,40,x+72,107),p);
            p.setColor(Color.DKGRAY);
            p.setTextSize(12);
            c.drawText(s,x+82,78,p);
        }

        void button(Canvas c,float x,String s,int type){
            float w=getWidth()/4f;
            p.setColor(action==type?Color.YELLOW:Color.WHITE);
            c.drawRect(x+2,118,x+w-2,158,p);
            p.setColor(Color.DKGRAY);
            p.setTextSize(12);
            c.drawText(s,x+10,143,p);
        }

        void drawBoard(Canvas c){
            p.setStyle(Paint.Style.FILL);
            for(int r=0;r<ROWS;r++)
                for(int col=0;col<COLS;col++){
                    p.setColor((r+col)%2==0?
                            Color.rgb(105,185,65):
                            Color.rgb(98,175,60));
                    float x=L+col*CW,y=T+r*CH;
                    c.drawRect(x,y,x+CW-1,y+CH-1,p);
                }
        }

        void drawPlants(Canvas c){
            for(Plant a:plants){
                float x=L+a.col*CW,y=T+a.row*CH;
                Bitmap b=a.type==1?sunImg:
                        a.type==2?peaImg:
                        a.type==3?gigaImg:chompImg;

                if(b!=null)c.drawBitmap(b,null,
                        new RectF(x+5,y+4,x+CW-5,y+CH-4),p);

                bar(c,x+8,y+4,CW-16,a.hp,a.max);

                if(a.food>0){
                    p.setColor(Color.YELLOW);
                    c.drawCircle(x+CW-15,y+17,7,p);
                }
            }
        }

        void drawZombies(Canvas c){
            for(Zombie z:zombies){
                float w=z.giga?78:62,h=z.giga?118:92;

                if(zombieImg!=null)
                    c.drawBitmap(zombieImg,null,
                            new RectF(z.x-w/2,z.y-h/2,
                                    z.x+w/2,z.y+h/2),p);
                else{
                    p.setColor(z.giga?Color.DKGRAY:Color.GRAY);
                    c.drawRect(z.x-w/2,z.y-h/2,
                            z.x+w/2,z.y+h/2,p);
                }

                bar(c,z.x-28,z.y-h/2-8,56,z.hp,z.max);
            }
        }

        void drawPeas(Canvas c){
            for(Pea b:peas){
                if(b.img!=null)
                    c.drawBitmap(b.img,null,
                            new RectF(b.x-12,b.y-12,b.x+12,b.y+12),p);
                else{
                    p.setColor(b.damage>=100?Color.YELLOW:Color.GREEN);
                    c.drawCircle(b.x,b.y,9,p);
                }
            }
        }

        void drawBombs(Canvas c){
            for(Bomb b:bombs){
                if(b.img!=null)
                    c.drawBitmap(b.img,null,
                            new RectF(b.x-18,b.y-18,b.x+18,b.y+18),p);
                else{
                    p.setColor(Color.RED);
                    c.drawCircle(b.x,b.y,13,p);
                }
            }
        }

        void drawMowers(Canvas c){
            for(Mower m:mowers){
                if(m.used)continue;
                p.setColor(Color.RED);
                c.drawRect(m.x-25,m.y-13,m.x+25,m.y+13,p);
                p.setColor(Color.BLACK);
                c.drawCircle(m.x-15,m.y+15,7,p);
                c.drawCircle(m.x+15,m.y+15,7,p);
            }
        }

        void bar(Canvas c,float x,float y,float w,int hp,int max){
            p.setColor(Color.RED);
            c.drawRect(x,y,x+w,y+5,p);
            p.setColor(Color.GREEN);
            float q=Math.max(0,Math.min(1,hp/(float)max));
            c.drawRect(x,y,x+w*q,y+5,p);
        }

        void tick(){
            long now=System.currentTimeMillis();
            float dt=Math.min(.08f,(now-last)/1000f);
            last=now;

            int total=10+level*3;
            long delay=Math.max(1100,3800-level*250);

            if(spawned<total&&now-spawnClock>=delay){
                spawn();
                spawnClock=now;
            }

            for(Plant a:plants){
                a.timer+=dt;

                if(a.food>0){
                    a.food-=dt;
                    if(a.food<0)a.food=0;
                }

                if(a.type==1){
                    if(a.timer>=(a.food>0?.2f:4f)){
                        sun+=a.food>0?100:50;
                        a.timer=0;
                    }
                }

                if(a.type==2&&a.timer>=(a.food>0?.25f:1.1f)
                        &&hasZombie(a.row)){
                    /*
                     * Đạn PEASHOOT xuất phát ở phía trước nòng:
                     * x + CW - 10, y ở khoảng 42% chiều cao cây.
                     */
                    peas.add(new Pea(
                            L+a.col*CW+CW-10,
                            T+a.row*CH+CH*.42f,
                            a.row,peaImg,a.food>0?70:25));
                    a.timer=0;
                }

                if(a.type==3&&a.timer>=(a.food>0?.25f:2f)
                        &&hasZombie(a.row)){
                    peas.add(new Pea(
                            L+a.col*CW+CW-10,
                            T+a.row*CH+CH*.42f,
                            a.row,peaImg,a.food>0?180:100));
                    a.timer=0;
                }

                if(a.type==4&&a.timer>=(a.food>0?.35f:2.8f)){
                    Zombie z=nearZombie(a);
                    if(z!=null){
                        z.hp=0;
                        a.timer=0;
                        beep(ToneGenerator.TONE_PROP_BEEP);
                    }
                }
            }

            updatePeas();
            updateBombs();
            updateZombies();
            updateMowers();
            clean();

            if(spawned>=total&&kills>=total&&zombies.isEmpty()){
                win=true;
                beep(ToneGenerator.TONE_PROP_ACK);
            }
        }

        boolean hasZombie(int row){
            for(Zombie z:zombies)if(z.row==row)return true;
            return false;
        }

        Zombie nearZombie(Plant a){
            float px=L+a.col*CW+CW/2;
            for(Zombie z:zombies)
                if(z.row==a.row&&Math.abs(z.x-px)<CW*1.4f)
                    return z;
            return null;
        }

        void updatePeas(){
            Iterator<Pea>it=peas.iterator();
            while(it.hasNext()){
                Pea b=it.next();
                b.x+=9;

                boolean hit=false;
                for(Zombie z:zombies)
                    if(z.row==b.row&&Math.abs(z.x-b.x)<30){
                        z.hp-=b.damage;
                        hit=true;
                        break;
                    }

                if(hit||b.x>getWidth()+30)it.remove();
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
                if(Math.abs(z.x-x)<CW*1.5f&&
                        Math.abs(z.row-row)<=1)
                    z.hp-=500;
            beep(ToneGenerator.TONE_PROP_BEEP);
        }

        void updateZombies(){
            for(Zombie z:zombies){
                if(z.slow>0)z.slow-=.03f;

                Plant a=plantAt(z);

                if(a!=null){
                    if(System.currentTimeMillis()-z.bite>500){
                        a.hp-=z.giga?160:80;
                        z.bite=System.currentTimeMillis();
                    }
                }else{
                    float speed=z.giga?.55f:1f;
                    if(z.slow>0)speed*=.45f;
                    z.x-=speed;
                }

                if(z.x<L-25){
                    Mower m=mowers[z.row];

                    if(!m.active&&!m.used){
                        m.active=true;
                        beep(ToneGenerator.TONE_PROP_ACK);
                    }else if(m.used){
                        lose=true;
                        beep(ToneGenerator.TONE_PROP_NACK);
                        return;
                    }
                }
            }
        }

        Plant plantAt(Zombie z){
            for(Plant a:plants){
                float px=L+a.col*CW+CW/2;
                if(a.row==z.row&&Math.abs(z.x-px)<CW*.55f)
                    return a;
            }
            return null;
        }

        void updateMowers(){
            for(Mower m:mowers){
                if(!m.active||m.used)continue;

                m.x+=14;

                for(Zombie z:zombies)
                    if(z.row==m.row&&Math.abs(z.x-m.x)<45)
                        z.hp=0;

                if(m.x>getWidth()+50)m.used=true;
            }
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
                    kills++;
                    coins+=25;
                }
            }
        }

        void spawn(){
            int row=rnd.nextInt(ROWS);
            boolean giga=level>=2&&spawned%4==0;
            zombies.add(new Zombie(
                    getWidth()+70,
                    T+row*CH+CH/2,
                    row,giga));
            spawned++;
        }

        boolean occupied(int row,int col){
            for(Plant a:plants)
                if(a.row==row&&a.col==col)return true;
            return false;
        }

        void resetMowers(){
            for(int i=0;i<ROWS;i++)
                mowers[i]=new Mower(i,L-35,
                        T+i*CH+CH/2);
        }

        void reset(){
            plants.clear();
            zombies.clear();
            peas.clear();
            bombs.clear();
            selected=0;
            action=0;
            spawned=0;
            kills=0;
            win=false;
            lose=false;
            sun=500;
            coins=9999;
            food=0;
            resetMowers();
            last=spawnClock=System.currentTimeMillis();
        }

        void plantFood(float x,float y){
            if(food<=0)return;

            int col=(int)((x-L)/CW);
            int row=(int)((y-T)/CH);

            if(row<0||row>=ROWS||col<0||col>=COLS)return;

            for(Plant a:plants)
                if(a.row==row&&a.col==col){
                    food--;
                    a.food=6;

                    if(a.type==3){
                        for(int i=-1;i<=1;i++)
                            peas.add(new Pea(
                                    L+a.col*CW+CW-10,
                                    T+a.row*CH+CH*.42f+i*18,
                                    a.row,peaImg,180));
                    }

                    if(a.type==4){
                        for(Zombie z:zombies)
                            if(z.row==a.row&&
                                    Math.abs(z.x-
                                    (L+a.col*CW+CW/2))<CW*2)
                                z.hp=0;
                    }

                    beep(ToneGenerator.TONE_PROP_BEEP);
                    action=0;
                    return;
                }
        }

        void bomb(float x,float y){
            if(coins<60)return;

            int col=(int)((x-L)/CW);
            int row=(int)((y-T)/CH);

            if(row<0||row>=ROWS||col<0||col>=COLS)return;

            coins-=60;

            float tx=L+col*CW+CW/2;
            float ty=T+row*CH+CH/2;

            bombs.add(new Bomb(
                    getWidth()-50,0,tx,ty,row));

            action=0;
        }

        void fire(float x,float y){
            if(coins<90)return;

            int col=(int)((x-L)/CW);
            int row=(int)((y-T)/CH);

            if(row<0||row>=ROWS||col<0||col>=COLS)return;

            coins-=90;

            float tx=L+col*CW+CW/2;
            float ty=T+row*CH+CH/2;

            for(Zombie z:zombies)
                if(Math.abs(z.x-tx)<CW*1.5f&&
                        Math.abs(z.row-row)<=1)
                    z.hp-=800;

            action=0;
            beep(ToneGenerator.TONE_PROP_BEEP);
        }

        void place(int type,float x,float y){
            int col=(int)((x-L)/CW);
            int row=(int)((y-T)/CH);

            if(row<0||row>=ROWS||col<0||col>=COLS)return;
            if(occupied(row,col))return;

            int cost=type==1?50:
                    type==2?100:
                    type==3?150:150;

            int hp=type==1?300:
                    type==2?400:
                    type==3?3000:800;

            if(sun<cost)return;

            plants.add(new Plant(type,row,col,hp));
            sun-=cost;
            selected=0;
            beep(ToneGenerator.TONE_PROP_BEEP);
        }

        void end(Canvas c){
            p.setColor(0xAA000000);
            c.drawRect(0,0,getWidth(),getHeight(),p);

            p.setColor(Color.WHITE);
            p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(32);
            c.drawText(win?"THẮNG MÀN "+level:"THUA!",
                    getWidth()/2f,getHeight()/2f,p);

            p.setTextSize(17);
            c.drawText("CHẠM ĐỂ TIẾP TỤC",
                    getWidth()/2f,getHeight()/2f+40,p);

            p.setTextAlign(Paint.Align.LEFT);
        }

        @Override public boolean onTouchEvent(MotionEvent e){
            if(e.getAction()!=MotionEvent.ACTION_DOWN)return true;

            float x=e.getX(),y=e.getY();

            if(win){
                if(level<9){
                    level++;
                    plants.clear();
                    zombies.clear();
                    peas.clear();
                    bombs.clear();
                    selected=0;
                    action=0;
                    spawned=0;
                    kills=0;
                    win=false;
                    resetMowers();
                    last=spawnClock=System.currentTimeMillis();
                }else reset();

                invalidate();
                return true;
            }

            if(lose){
                reset();
                invalidate();
                return true;
            }

            float w=getWidth()/4f;

            if(y>=35&&y<112){
                if(x<w)selected=1;
                else if(x<2*w)selected=2;
                else if(x<3*w)selected=3;
                else selected=4;

                action=0;
                invalidate();
                return true;
            }

            if(y>=118&&y<158){
                if(x<w){
                    action=1;
                }else if(x<2*w){
                    action=2;
                }else if(x<3*w){
                    if(coins>=100){
                        coins-=100;
                        food++;
                    }
                    action=3;
                }else{
                    reset();
                }

                selected=0;
                invalidate();
                return true;
            }

            if(y>=T&&y<T+ROWS*CH&&
                    x>=L&&x<L+COLS*CW){

                if(action==3){
                    plantFood(x,y);
                }else if(action==1){
                    bomb(x,y);
                }else if(action==2){
                    fire(x,y);
                }else if(selected>0){
                    place(selected,x,y);
                }

                invalidate();
                return true;
            }

            return true;
        }
    }

    class Plant{
        int type,row,col,hp,max;
        float timer=0,food=0;
        long bite=0;

        Plant(int t,int r,int c,int
