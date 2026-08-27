package com.alpvz.gardendefense;

import android.app.Activity;
import android.os.Bundle;
import android.content.pm.ActivityInfo;
import android.graphics.*;
import android.media.*;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import java.util.*;

public class MainActivity extends Activity {
    GardenGame game;
    FrameLayout root;
    VideoView winVideo;
    Button continueBtn;
    Handler handler=new Handler(Looper.getMainLooper());

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(1024,1024);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        root=new FrameLayout(this);
        game=new GardenGame();
        root.addView(game,new FrameLayout.LayoutParams(-1,-1));
        setContentView(root);
    }

    void winVideo(){
        if(winVideo!=null)return;
        int id=getResources().getIdentifier("win","raw",getPackageName());
        if(id==0)return;
        winVideo=new VideoView(this);
        winVideo.setBackgroundColor(Color.BLACK);
        winVideo.setVideoURI(Uri.parse("android.resource://"+getPackageName()+"/"+id));
        root.addView(winVideo,new FrameLayout.LayoutParams(-1,-1));
        continueBtn=new Button(this);
        continueBtn.setText("CHƠI TIẾP");
        continueBtn.setVisibility(View.GONE);
        FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(260,100,Gravity.RIGHT|Gravity.BOTTOM);
        lp.setMargins(0,0,24,24);
        root.addView(continueBtn,lp);
        winVideo.setOnPreparedListener(m->{m.setLooping(false);winVideo.start();});
        winVideo.setOnCompletionListener(m->{continueBtn.setVisibility(View.VISIBLE);});
        continueBtn.setOnClickListener(v->{
            closeVideo();
            if(game.level<9)game.startLevel(game.level+1);
            else{game.screen=GardenGame.HOME;game.invalidate();}
        });
    }

    void closeVideo(){
        handler.removeCallbacksAndMessages(null);
        if(winVideo!=null){
            try{winVideo.stopPlayback();}catch(Exception e){}
            root.removeView(winVideo);
            winVideo=null;
        }
        if(continueBtn!=null){
            root.removeView(continueBtn);
            continueBtn=null;
        }
    }

    @Override protected void onPause(){
        if(game!=null)game.save();
        super.onPause();
    }

    @Override protected void onDestroy(){
        closeVideo();
        if(game!=null)game.releaseSounds();
        super.onDestroy();
    }

    @Override public void onBackPressed(){
        if(game.screen==GardenGame.PLAY)game.screen=GardenGame.PAUSE;
        else if(game.screen==GardenGame.PAUSE)game.screen=GardenGame.PLAY;
        else if(game.screen!=GardenGame.HOME){
            closeVideo();
            game.screen=GardenGame.HOME;
        }else{super.onBackPressed();return;}
        game.invalidate();
    }

    public class GardenGame extends View {
        static final int ROWS=5,COLS=9;
        static final int SUNFLOWER=1,PEASHOOTER=2,GIGANUT=3,
                CHOMPER=4,REPEATER=5,MINE=6,BINU=7;
        static final int HOME=0,LEVELS=1,PLAY=2,PAUSE=3,WIN=4,LOSE=5;
        static final int NONE=0,SHOVEL=1,FOOD=2;

        Paint p=new Paint(3);
        RectF rr=new RectF();
        Random rnd=new Random();

        Plant[][] plants=new Plant[ROWS][COLS];
        ArrayList<Zombie> zombies=new ArrayList<>();
        ArrayList<Pea> peas=new ArrayList<>();
        ArrayList<SunDrop> suns=new ArrayList<>();
        Mower[] mowers=new Mower[ROWS];

        Bitmap sunImg,peaImg,gigaImg,chomperImg,repeaterImg,mineImg;
        Bitmap peaFoodImg,repeaterFoodImg,gigaFoodImg;
        Bitmap zombieImg,bulletImg;
        Bitmap binuImg,binu1Img,binu2Img,binu3Img,binu4Img;

        MediaPlayer foodSound,binuSound1,binuSound2;

        float left,top,cw,ch;
        int screen=HOME,level=1,unlocked=1;
        int selected=PEASHOOTER,tool=NONE;
        int sun=500,coins=99999,food=10000;
        int killed,total,spawned;
        long last,spawnClock;

        boolean speed2=false;
        boolean binuJump=false;
        int binuFrame=0,binuRow=-1,binuCol=-1;
        long binuClock;

        GardenGame(){
            super(MainActivity.this);
            setFocusable(true);

            android.content.SharedPreferences s=
                    getSharedPreferences("garden_defense",0);
            level=s.getInt("level",1);
            unlocked=s.getInt("unlocked",1);
            coins=s.getInt("coins",99999);
            food=s.getInt("food",10000);

            for(int r=0;r<ROWS;r++)mowers[r]=new Mower(r);
            loadImages();
            initSounds();
            last=spawnClock=System.currentTimeMillis();
        }

        Bitmap img(String n){
            try{
                int id=getResources().getIdentifier(n,"drawable",getPackageName());
                if(id==0)return null;
                BitmapFactory.Options o=new BitmapFactory.Options();
                o.inScaled=false;
                o.inPreferredConfig=Bitmap.Config.ARGB_8888;
                return BitmapFactory.decodeResource(getResources(),id,o);
            }catch(Exception e){return null;}
        }

        void loadImages(){
            sunImg=img("sun");
            peaImg=img("peashoot");
            gigaImg=img("giganut");
            chomperImg=img("chomper");
            repeaterImg=img("repeater");
            mineImg=img("min");
            peaFoodImg=img("peashootplantfood");
            repeaterFoodImg=img("repeaterplantfood");
            gigaFoodImg=img("giganutplantfood");
            zombieImg=img("zomplatz");
            bulletImg=img("gigapea");

            binuImg=img("binu");
            binu1Img=img("binu1");
            binu2Img=img("binu2");
            binu3Img=img("binu3");
            binu4Img=img("binu4");
        }

        void initSounds(){
            foodSound=makeSound("peashootplantfood");
            binuSound1=makeSound("binusound1");
            binuSound2=makeSound("binusound2");
        }

        MediaPlayer makeSound(String n){
            try{
                int id=getResources().getIdentifier(n,"raw",getPackageName());
                return id==0?null:MediaPlayer.create(MainActivity.this,id);
            }catch(Exception e){return null;}
        }

        @Override protected void onSizeChanged(int w,int h,int ow,int oh){
            left=w*.18f;
            top=h*.25f;
            cw=w*.78f/COLS;
            ch=h*.70f/ROWS;
            for(int r=0;r<ROWS;r++)mowers[r].x=left-cw*.4f;
        }

        @Override protected void onDraw(Canvas c){
            if(screen==HOME){drawHome(c);return;}
            if(screen==LEVELS){drawLevels(c);return;}
            drawGame(c);
            if(screen==PAUSE)overlay(c,"TẠM DỪNG","TIẾP TỤC","CHƠI LẠI","THOÁT");
            if(screen==WIN)overlay(c,"CHIẾN THẮNG!","MÀN TIẾP","CHƠI LẠI","VỀ MENU");
            if(screen==LOSE)overlay(c,"ZOMBIE ĐÃ VÀO NHÀ!","CHƠI LẠI","","VỀ MENU");
            if(screen==PLAY){update();postInvalidateDelayed(40);}
        }

        void text(Canvas c,String s,float x,float y,float z,int col,Paint.Align a){
            p.setTextSize(z);p.setColor(col);p.setTextAlign(a);
            c.drawText(s,x,y,p);
        }

        void button(Canvas c,float x1,float y1,float x2,float y2,String s){
            p.setColor(Color.rgb(50,100,55));
            c.drawRoundRect(getWidth()*x1,getHeight()*y1,
                    getWidth()*x2,getHeight()*y2,12,12,p);
            text(c,s,getWidth()*(x1+x2)/2,
                    getHeight()*(y1+y2)/2+8,20,Color.WHITE,Paint.Align.CENTER);
        }

        void drawHome(Canvas c){
            c.drawColor(Color.rgb(25,65,30));
            text(c,"GARDEN DEFENSE",getWidth()/2f,getHeight()*.25f,
                    42,Color.WHITE,Paint.Align.CENTER);
            text(c,"☀ "+sun+"  XU "+coins+"  PF "+food,
                    getWidth()/2f,getHeight()*.34f,22,Color.YELLOW,Paint.Align.CENTER);
            button(c,.30f,.44f,.70f,.55f,"CHƠI");
            button(c,.30f,.60f,.70f,.71f,"CHỌN MÀN");
        }

        void drawLevels(Canvas c){
            c.drawColor(Color.rgb(20,55,25));
            text(c,"CHỌN MÀN",getWidth()/2f,getHeight()*.10f,
                    32,Color.WHITE,Paint.Align.CENTER);

            for(int i=1;i<=9;i++){
                int col=(i-1)%3,row=(i-1)/3;
                float x=.18f+col*.22f,y=.18f+row*.19f;
                p.setColor(i<=unlocked?Color.rgb(65,145,70):Color.DKGRAY);
                c.drawRoundRect(getWidth()*x,getHeight()*y,
                        getWidth()*(x+.17f),getHeight()*(y+.13f),14,14,p);
                text(c,i<=unlocked?"MÀN "+i:"KHÓA",
                        getWidth()*(x+.085f),getHeight()*(y+.082f),
                        19,Color.WHITE,Paint.Align.CENTER);
            }
            button(c,.04f,.84f,.20f,.94f,"QUAY LẠI");
        }

        void drawGame(Canvas c){
            c.drawColor(Color.rgb(92,155,70));
            p.setColor(Color.rgb(38,78,40));
            c.drawRect(0,0,getWidth(),top,p);

            text(c,"☀ "+sun,14,34,22,Color.YELLOW,Paint.Align.LEFT);
            text(c,"MÀN "+level,getWidth()*.25f,34,20,Color.WHITE,Paint.Align.LEFT);
            text(c,"ZOM "+killed+"/"+total,getWidth()*.45f,34,19,Color.WHITE,Paint.Align.LEFT);
            text(c,"XU "+coins,getWidth()*.65f,34,19,Color.YELLOW,Paint.Align.LEFT);
            text(c,"PF "+food,getWidth()*.84f,34,19,Color.WHITE,Paint.Align.LEFT);

            drawCards(c);drawBoard(c);drawPlants(c);
            drawPeas(c);drawZombies(c);drawSuns(c);drawMowers(c);

            button(c,.64f,.075f,.75f,.15f,"MUA PF");
            button(c,.82f,.075f,.90f,.15f,"Ⅱ");
            button(c,.91f,.075f,.99f,.15f,speed2?"×2":"▶");

            p.setColor(Color.DKGRAY);
            c.drawRect(getWidth()*.18f,getHeight()*.215f,
                    getWidth()*.82f,getHeight()*.23f,p);
            p.setColor(Color.GREEN);
            float pr=total==0?0:killed/(float)total;
            c.drawRect(getWidth()*.18f,getHeight()*.215f,
                    getWidth()*(.18f+.64f*Math.min(1,pr)),
                    getHeight()*.23f,p);
    }        void drawCards(Canvas c){
            int[] t={SUNFLOWER,PEASHOOTER,GIGANUT,CHOMPER,REPEATER,MINE,BINU};

            for(int i=0;i<t.length;i++){
                float x=getWidth()*(.005f+i*.061f);
                float y=getHeight()*.075f;
                float w=getWidth()*.055f,h=getHeight()*.09f;

                p.setColor(selected==t[i]&&tool==NONE?
                        Color.YELLOW:Color.rgb(45,85,45));

                c.drawRoundRect(x,y,x+w,y+h,8,8,p);

                if(unlocked(t[i]))
                    drawPlant(c,t[i],x+w/2,y+h/2,
                            Math.min(w,h)*.72f,null);
                else
                    text(c,"LOCK",x+w/2,y+h*.62f,
                            10,Color.LTGRAY,Paint.Align.CENTER);
            }

            p.setColor(tool==SHOVEL?Color.YELLOW:Color.rgb(55,80,55));
            c.drawRoundRect(getWidth()*.435f,getHeight()*.075f,
                    getWidth()*.495f,getHeight()*.15f,8,8,p);
            text(c,"XẺNG",getWidth()*.465f,getHeight()*.122f,
                    11,Color.WHITE,Paint.Align.CENTER);

            p.setColor(tool==FOOD?Color.YELLOW:Color.rgb(55,80,55));
            c.drawRoundRect(getWidth()*.505f,getHeight()*.075f,
                    getWidth()*.62f,getHeight()*.15f,8,8,p);
            text(c,"PF "+food,getWidth()*.562f,getHeight()*.122f,
                    12,Color.WHITE,Paint.Align.CENTER);
        }

        void drawBoard(Canvas c){
            for(int r=0;r<ROWS;r++)
                for(int col=0;col<COLS;col++){
                    p.setColor(!activeRow(r)?Color.rgb(72,110,62):
                            (r+col)%2==0?Color.rgb(103,166,78):
                            Color.rgb(91,153,67));

                    c.drawRect(left+col*cw,top+r*ch,
                            left+(col+1)*cw,top+(r+1)*ch,p);
                }

            p.setColor(Color.rgb(135,95,55));
            c.drawRect(0,top,left,top+ROWS*ch,p);
        }

        void drawPlants(Canvas c){
            for(int r=0;r<ROWS;r++)
                for(int col=0;col<COLS;col++){
                    Plant a=plants[r][col];
                    if(a==null)continue;

                    float x=left+col*cw+cw/2;
                    float y=top+r*ch+ch/2;

                    drawPlant(c,a.type,x,y,
                            Math.min(cw,ch)*.74f,a);

                    hp(c,x-cw*.3f,y+ch*.32f,cw*.6f,
                            a.hp,a.maxHp);
                }

            if(binuJump&&binuRow>=0){
                float x=left+binuCol*cw+cw/2;
                float y=top+binuRow*ch+ch/2;

                drawPlant(c,BINU,x,y,
                        Math.min(cw,ch)*.82f,null);
            }
        }

        void drawPlant(Canvas c,int type,float x,float y,
                       float size,Plant a){

            Bitmap b=null;

            if(a!=null&&a.foodUsed){
                if(type==PEASHOOTER)b=peaFoodImg;
                else if(type==REPEATER)b=repeaterFoodImg;
                else if(type==GIGANUT)b=gigaFoodImg;
            }

            if(b==null){
                if(type==SUNFLOWER)b=sunImg;
                else if(type==PEASHOOTER)b=peaImg;
                else if(type==GIGANUT)b=gigaImg;
                else if(type==CHOMPER)b=chomperImg;
                else if(type==REPEATER)b=repeaterImg;
                else if(type==MINE)b=mineImg;
                else if(type==BINU){
                    if(!binuJump)b=binuImg;
                    else if(binuFrame==1)b=binu1Img;
                    else if(binuFrame==2)b=binu2Img;
                    else if(binuFrame==3)b=binu3Img;
                    else b=binu4Img;
                }
            }

            if(b!=null){
                c.drawBitmap(b,null,
                        new RectF(x-size/2,y-size/2,
                                x+size/2,y+size/2),p);
            }else{
                p.setColor(type==GIGANUT?
                        Color.rgb(145,95,55):
                        Color.rgb(55,175,70));
                c.drawCircle(x,y,size*.35f,p);
            }
        }

        void drawZombies(Canvas c){
            for(Zombie z:zombies){
                if(z.hp<=0)continue;

                float w=z.boss?cw*1.35f:
                        z.giga?cw*.95f:cw*.68f;
                float h=z.boss?ch*1.35f:
                        z.giga?ch*1.08f:ch*.82f;

                if(zombieImg!=null)
                    c.drawBitmap(zombieImg,null,
                            new RectF(z.x-w/2,z.y-h*.55f,
                                    z.x+w/2,z.y+h*.45f),p);
                else{
                    p.setColor(Color.DKGRAY);
                    c.drawOval(new RectF(z.x-w/2,z.y-h/2,
                            z.x+w/2,z.y+h/2),p);
                }

                hp(c,z.x-w/2,z.y-h*.65f,w,z.hp,z.maxHp);
            }
        }

        void drawPeas(Canvas c){
            for(Pea q:peas){
                float s=q.big?18:10;

                if(bulletImg!=null)
                    c.drawBitmap(bulletImg,null,
                            new RectF(q.x-s,q.y-s,q.x+s,q.y+s),p);
                else{
                    p.setColor(Color.GREEN);
                    c.drawCircle(q.x,q.y,s*.7f,p);
                }
            }
        }

        void drawSuns(Canvas c){
            for(SunDrop s:suns){
                if(sunImg!=null)
                    c.drawBitmap(sunImg,null,
                            new RectF(s.x-18,s.y-18,
                                    s.x+18,s.y+18),p);
                else{
                    p.setColor(Color.YELLOW);
                    c.drawCircle(s.x,s.y,16,p);
                }
            }
        }

        void drawMowers(Canvas c){
            for(Mower m:mowers){
                float y=top+m.row*ch+ch*.72f;
                p.setColor(m.used?Color.DKGRAY:
                        Color.rgb(190,70,40));

                c.drawRoundRect(m.x-cw*.3f,y-ch*.18f,
                        m.x+cw*.3f,y,8,8,p);
            }
        }

        void hp(Canvas c,float x,float y,float w,
                float value,float max){
            p.setColor(Color.DKGRAY);
            c.drawRect(x,y,x+w,y+6,p);

            if(max>0){
                p.setColor(Color.GREEN);
                c.drawRect(x,y,x+w*
                        Math.max(0,Math.min(1,value/max)),
                        y+6,p);
            }
        }

        void update(){
            if(screen!=PLAY)return;

            long now=System.currentTimeMillis();
            float dt=Math.min(.08f,(now-last)/1000f)*
                    (speed2?2:1);
            last=now;

            updateBinu(now);
            checkBinu(now);
            updatePlants(now);
            updatePeas(dt);
            updateZombies(now,dt);
            updateMowers(dt);
            removeDead();

            if(spawned<total&&now-spawnClock>=spawnDelay()){
                spawnZombie();
                spawned++;
                spawnClock=now;
            }

            if(spawned>=total&&zombies.isEmpty()&&killed>=total)
                winLevel();
        }

        void updateBinu(long now){
            if(!binuJump)return;

            if(now-binuClock>=85){
                binuClock=now;
                binuFrame++;

                if(binuFrame>=4){
                    smashBinu();
                    binuJump=false;
                    binuFrame=0;
                    binuRow=-1;
                    binuCol=-1;
                }
            }
        }

        void checkBinu(long now){
            if(binuJump)return;

            for(int r=0;r<ROWS;r++)
                for(int col=0;col<COLS;col++){
                    Plant a=plants[r][col];

                    if(a==null||a.type!=BINU)continue;

                    float bx=left+col*cw+cw/2;

                    for(Zombie z:zombies){
                        if(z.hp<=0||z.row!=r)continue;

                        if(z.x>bx&&z.x<=bx+cw*1.05f){
                            binuJump=true;
                            binuFrame=1;
                            binuRow=r;
                            binuCol=col;
                            binuClock=now;

                            plants[r][col]=null;
                            playBinuSound2();
                            return;
                        }
                    }
                }
        }

        void smashBinu(){
            if(binuRow<0)return;

            float cx=left+binuCol*cw+cw/2+cw;

            for(Zombie z:zombies){
                if(z.hp<=0||z.row!=binuRow)continue;

                if(Math.abs(z.x-cx)<=cw*.90f){
                    if(z.boss)
                        z.hp=Math.max(1,z.hp*.5f);
                    else
                        z.hp=0;
                }
            }
        }

        void updatePlants(long now){
            for(int r=0;r<ROWS;r++)
                for(int col=0;col<COLS;col++){
                    Plant a=plants[r][col];
                    if(a==null)continue;

                    if(a.foodUsed&&a.type!=GIGANUT&&
                            now>=a.foodUntil){
                        a.foodUsed=false;
                        a.foodUntil=0;
                    }

                    boolean f=a.foodUsed&&now<a.foodUntil;

                    if(a.type==SUNFLOWER){
                        long cd=f?1800:7000;
                        if(now-a.last>=cd){
                            suns.add(new SunDrop(
                                    left+col*cw+cw/2,
                                    top+r*ch+ch*.2f));
                            a.last=now;
                        }

                    }else if(a.type==PEASHOOTER){
                        long cd=f?700:1500;
                        if(now-a.last>=cd&&rowHasZombie(r)){
                            fire(r,col,f?45:30,f);
                            a.last=now;
                        }

                    }else if(a.type==REPEATER){
                        long cd=f?700:1500;
                        if(now-a.last>=cd&&rowHasZombie(r)){
                            fire(r,col,f?45:30,f);
                            a.secondShot=now+500;
                            a.last=now;
                        }

                    }else if(a.type==CHOMPER){
                        if(now-a.last>=3500){
                            Zombie z=nearest(r,col,cw*1.5f);
                            if(z!=null){
                                z.hp=0;
                                a.last=now;
                            }
                        }

                    }else if(a.type==MINE){
                        if(!a.armed&&now>=a.armAt)a.armed=true;
                        if(a.armed){
                            Zombie z=onCell(r,col);
                            if(z!=null)explodeMine(r,col,a);
                        }
                    }

                    if(a.secondShot>0&&now>=a.secondShot){
                        fire(r,col,f?45:30,f);
                        a.secondShot=0;
                    }
                }
        }

        void explodeMine(int r,int col,Plant mine){
            float cx=left+col*cw+cw/2;

            for(Zombie z:zombies)
                if(z.row>=Math.max(0,r-1)&&
                   z.row<=Math.min(ROWS-1,r+1)&&
                   Math.abs(z.x-cx)<=cw*1.55f)
                    z.hp-=1800;

            mine.hp=0;
        }

        void fire(int r,int col,int dmg,boolean big){
            peas.add(new Pea(
                    left+col*cw+cw*.56f,
                    top+r*ch+ch*.5f,
                    r,dmg,big));
        }        void updatePeas(float dt){
            Iterator<Pea> it=peas.iterator();

            while(it.hasNext()){
                Pea q=it.next();

                q.x+=(q.enemy?-1:1)*cw*8.5f*dt;

                if(q.enemy){
                    int col=(int)((q.x-left)/cw);

                    if(col>=0&&col<COLS){
                        Plant a=plants[q.row][col];

                        if(a!=null){
                            a.hp-=q.damage;
                            it.remove();
                            continue;
                        }
                    }

                    if(q.x<left-cw)it.remove();
                    continue;
                }

                Zombie hit=null;

                for(Zombie z:zombies)
                    if(z.hp>0&&z.row==q.row&&
                            Math.abs(z.x-q.x)<cw*.3f){
                        hit=z;
                        break;
                    }

                if(hit!=null){
                    hit.hp-=q.damage;
                    it.remove();
                }else if(q.x>getWidth()+40)
                    it.remove();
            }
        }

        void updateZombies(long now,float dt){
            for(Zombie z:zombies){
                if(z.hp<=0)continue;

                if(z.boss&&now-z.lastShot>=1800){
                    peas.add(new Pea(
                            z.x-cw*.55f,z.y,z.row,
                            150,true,true));
                    z.lastShot=now;
                }

                Plant a=frontPlant(z);

                if(a!=null){
                    if(now-z.lastAttack>=800){
                        a.hp-=100;
                        z.lastAttack=now;
                    }
                }else{
                    z.x-=z.speed*dt;
                }

                if(z.x<=left-cw*.45f){
                    Mower m=mowers[z.row];

                    if(!m.used){
                        m.used=true;
                        m.active=true;
                        m.x=left-cw*.5f;
                        z.x=left-cw*.45f;
                    }else if(!m.active){
                        screen=LOSE;
                        return;
                    }
                }
            }
        }

        void updateMowers(float dt){
            for(Mower m:mowers){
                if(!m.active)continue;

                m.x+=cw*17f*dt;

                for(Zombie z:zombies)
                    if(z.row==m.row&&
                       Math.abs(z.x-m.x)<cw*.55f)
                        z.hp=0;

                if(m.x>getWidth()+cw)
                    m.active=false;
            }
        }

        void removeDead(){
            Iterator<Zombie> it=zombies.iterator();

            while(it.hasNext()){
                Zombie z=it.next();

                if(z.hp<=0){
                    it.remove();
                    killed++;
                    coins+=z.boss?100:5;
                }
            }

            for(int r=0;r<ROWS;r++)
                for(int col=0;col<COLS;col++)
                    if(plants[r][col]!=null&&
                       plants[r][col].hp<=0)
                        plants[r][col]=null;
        }

        void spawnZombie(){
            int n=activeRows();
            int start=(ROWS-n)/2;
            int r=start+rnd.nextInt(n);

            boolean boss=level==9&&spawned==total-1;
            boolean giga=!boss&&level>=2&&rnd.nextInt(4)==0;

            zombies.add(new Zombie(
                    r,getWidth()+cw,
                    top+r*ch+ch/2,boss,giga));
        }

        long spawnDelay(){
            if(level<=2)return 4200;
            if(level<=4)return 3600;
            if(level<=8)return 3100;
            return 2600;
        }

        boolean rowHasZombie(int r){
            for(Zombie z:zombies)
                if(z.row==r&&z.x>left&&z.hp>0)return true;
            return false;
        }

        Plant frontPlant(Zombie z){
            int col=(int)((z.x-left)/cw);
            return col>=0&&col<COLS?plants[z.row][col]:null;
        }

        Zombie onCell(int r,int col){
            float x=left+col*cw+cw/2;
            for(Zombie z:zombies)
                if(z.row==r&&Math.abs(z.x-x)<cw*.5f)
                    return z;
            return null;
        }

        Zombie nearest(int r,int col,float range){
            float x=left+col*cw+cw/2;
            Zombie best=null;
            float d0=Float.MAX_VALUE;

            for(Zombie z:zombies){
                if(z.row!=r||z.hp<=0)continue;
                float d=Math.abs(z.x-x);

                if(d<=range&&d<d0){
                    d0=d;
                    best=z;
                }
            }
            return best;
        }

        boolean unlocked(int t){
            if(t==PEASHOOTER||t==BINU)return true;
            if(t==SUNFLOWER)return level>=2;
            if(t==GIGANUT)return level>=3;
            if(t==MINE)return level>=4;
            if(t==CHOMPER)return level>=5;
            if(t==REPEATER)return level>=6;
            return false;
        }

        int cost(int t){
            if(t==SUNFLOWER)return 50;
            if(t==PEASHOOTER)return 100;
            if(t==GIGANUT)return 125;
            if(t==CHOMPER)return 150;
            if(t==REPEATER)return 200;
            if(t==MINE)return 50;
            if(t==BINU)return 500;
            return 999999;
        }

        int activeRows(){
            if(level==1)return 1;
            if(level<=3)return 3;
            return 5;
        }

        boolean activeRow(int r){
            int n=activeRows();
            int start=(ROWS-n)/2;
            return r>=start&&r<start+n;
        }

        void startLevel(int lv){
            closeVideo();

            level=Math.max(1,Math.min(9,lv));
            sun=500;
            speed2=false;
            screen=PLAY;
            killed=0;
            spawned=0;

            total=lv<=2?8:lv<=4?10:lv<=8?12:15;

            clear();

            binuJump=false;
            binuFrame=0;
            binuRow=-1;
            binuCol=-1;

            last=spawnClock=System.currentTimeMillis();
        }

        void clear(){
            for(int r=0;r<ROWS;r++){
                for(int col=0;col<COLS;col++)
                    plants[r][col]=null;
                mowers[r]=new Mower(r);
            }

            zombies.clear();
            peas.clear();
            suns.clear();
            tool=NONE;
        }

        void winLevel(){
            screen=WIN;

            if(level<9)
                unlocked=Math.max(unlocked,level+1);

            save();

            if(level==9)
                post(()->MainActivity.this.winVideo());
        }

        void useFood(Plant a){
            if(a==null||a.type==BINU||food<=0||a.foodUsed)
                return;

            food--;
            a.foodUsed=true;

            try{
                if(foodSound!=null){
                    if(foodSound.isPlaying())foodSound.pause();
                    foodSound.seekTo(0);
                    foodSound.start();
                }
            }catch(Exception e){}

            long now=System.currentTimeMillis();

            if(a.type==GIGANUT){
                a.maxHp=8000;
                a.hp=8000;
                a.foodUntil=0;
            }else if(a.type==SUNFLOWER){
                a.foodUntil=now+12000;
                a.last=now-2000;
            }else if(a.type==PEASHOOTER||
                     a.type==REPEATER){
                a.foodUntil=now+12000;
                a.last=now-1000;
            }else if(a.type==CHOMPER){
                Zombie z=nearest(a.row,a.col,cw*2.2f);
                if(z!=null)z.hp=0;
                a.foodUntil=now+3000;
            }else if(a.type==MINE){
                a.armed=true;
                explodeMine(a.row,a.col,a);
            }

            save();
        }

        void buyFood(){
            if(coins>=100){
                coins-=100;
                food++;
                save();
            }
        }

        void playBinuSound2(){
            try{
                if(binuSound2!=null&&!binuSound2.isPlaying()){
                    binuSound2.seekTo(0);
                    binuSound2.start();
                }
            }catch(Exception e){}
        }

        void playBinuSound1(){
            try{
                if(binuSound1!=null){
                    binuSound1.seekTo(0);
                    binuSound1.start();
                }
            }catch(Exception e){}
        }

        void restart(){startLevel(level);}

        void save(){
            getSharedPreferences("garden_defense",0)
                    .edit()
                    .putInt("level",level)
                    .putInt("unlocked",unlocked)
                    .putInt("coins",coins)
                    .putInt("food",food)
                    .apply();
        }

        void releaseSounds(){
            try{if(foodSound!=null)foodSound.release();}catch(Exception e){}
            try{if(binuSound1!=null)binuSound1.release();}catch(Exception e){}
            try{if(binuSound2!=null)binuSound2.release();}catch(Exception e){}
        }

        void overlay(Canvas c,String title,String a,String b,String d){
            p.setColor(0xaa000000);
            c.drawRect(0,0,getWidth(),getHeight(),p);

            text(c,title,getWidth()/2f,getHeight()*.25f,
                    34,Color.WHITE,Paint.Align.CENTER);

            if(!a.isEmpty())button(c,.30f,.38f,.70f,.48f,a);
            if(!b.isEmpty())button(c,.30f,.52f,.70f,.62f,b);
            if(!d.isEmpty())button(c,.30f,.66f,.70f,.76f,d);
        }

        @Override public boolean onTouchEvent(MotionEvent e){
            if(e.getAction()!=MotionEvent.ACTION_DOWN)return true;

            float x=e.getX(),y=e.getY();

            if(screen==HOME){
                if(y>getHeight()*.40f&&y<getHeight()*.57f)
                    startLevel(level);
                else if(y>getHeight()*.58f&&y<getHeight()*.75f)
                    screen=LEVELS;

                invalidate();
                return true;
            }

            if(screen==LEVELS){
                if(y>getHeight()*.82f){
                    screen=HOME;
                    invalidate();
                    return true;
                }

                for(int i=1;i<=9;i++){
                    int col=(i-1)%3,row=(i-1)/3;
                    float x1=getWidth()*(.18f+col*.22f);
                    float y1=getHeight()*(.18f+row*.19f);

                    if(i<=unlocked&&x>=x1&&
                       x<=x1+getWidth()*.17f&&
                       y>=y1&&
                       y<=y1+getHeight()*.13f){
                        startLevel(i);
                        return true;
                    }
                }
                return true;
            }

            if(screen==PAUSE){
                if(y>getHeight()*.35f&&y<getHeight()*.51f)
                    screen=PLAY;
                else if(y>getHeight()*.51f&&y<getHeight()*.65f)
                    restart();
                else if(y>getHeight()*.65f&&y<getHeight()*.80f){
                    save();
                    screen=HOME;
                }

                invalidate();
                return true;
            }

            if(screen==WIN){
                if(y>getHeight()*.35f&&y<getHeight()*.51f){
                    if(level<9)startLevel(level+1);
                }else if(y>getHeight()*.51f&&y<getHeight()*.65f)
                    restart();
                else if(y>getHeight()*.65f&&y<getHeight()*.80f){
                    closeVideo();
                    screen=HOME;
                }

                invalidate();
                return true;
            }

            if(screen==LOSE){
                if(y>getHeight()*.35f&&y<getHeight()*.51f)
                    restart();
                else if(y>getHeight()*.65f&&y<getHeight()*.80f)
                    screen=HOME;

                invalidate();
                return true;
            }

            if(screen==PLAY){

                if(y>=getHeight()*.075f&&
                   y<=getHeight()*.15f&&
                   x>=getWidth()*.64f&&
                   x<=getWidth()*.75f){
                    buyFood();
                    invalidate();
                    return true;
                }

                if(y>=getHeight()*.075f&&
                   y<=getHeight()*.15f&&
                   x>=getWidth()*.435f&&
                   x<=getWidth()*.495f){
                    tool=tool==SHOVEL?NONE:SHOVEL;
                    invalidate();
                    return true;
                }

                if(y>=getHeight()*.075f&&
                   y<=getHeight()*.15f&&
                   x>=getWidth()*.505f&&
                   x<=getWidth()*.62f){
                    tool=tool==FOOD?NONE:FOOD;
                    invalidate();
                    return true;
                }

                if(y>=getHeight()*.075f&&
                   y<=getHeight()*.15f&&
                   x>=getWidth()*.80f&&
                   x<=getWidth()*.90f){
                    screen=PAUSE;
                    invalidate();
                    return true;
                }

                if(y>=getHeight()*.075f&&
                   y<=getHeight()*.15f&&
                   x>getWidth()*.90f){
                    speed2=!speed2;
                    invalidate();
                    return true;
                }

                if(y>=getHeight()*.075f&&
                   y<=getHeight()*.15f&&
                   x<getWidth()*.435f){

                    int i=(int)((x/getWidth()-.005f)/.061f);
                    int[] ts={SUNFLOWER,PEASHOOTER,GIGANUT,
                            CHOMPER,REPEATER,MINE,BINU};

                    if(i>=0&&i<ts.length&&unlocked(ts[i])){
                        selected=ts[i];
                        tool=NONE;
                    }

                    invalidate();
                    return true;
                }

                Iterator<SunDrop> sit=suns.iterator();

                while(sit.hasNext()){
                    SunDrop s=sit.next();

                    if(Math.hypot(x-s.x,y-s.y)<45){
                        sun+=100;
                        sit.remove();
                        save();
                        invalidate();
                        return true;
                    }
                }

                if(x>=left&&x<=left+COLS*cw&&
                   y>=top&&y<=top+ROWS*ch){

                    int col=(int)((x-left)/cw);
                    int r=(int)((y-top)/ch);

                    if(!activeRow(r))return true;

                    Plant a=plants[r][col];

                    if(tool==SHOVEL){
                        plants[r][col]=null;
                        tool=NONE;

                    }else if(tool==FOOD){
                        if(a!=null&&a.type!=BINU)useFood(a);
                        tool=NONE;

                    }else if(a==null&&unlocked(selected)&&
                            sun>=cost(selected)){

                        sun-=cost(selected);
                        plants[r][col]=new Plant(selected,r,col);

                        if(selected==BINU)
                            playBinuSound1();

                        save();
                    }

                    invalidate();
                    return true;
                }
            }

            return true;
        }
    }

    static class Plant{
        int type,row,col,hp,maxHp;
        long last,secondShot,foodUntil,armAt;
        boolean foodUsed,armed;

        Plant(int t,int r,int c){
            type=t;row=r;col=c;
            maxHp=t==GIGANUT?4000:1000;
            hp=maxHp;
            armAt=System.currentTimeMillis()+30000;
        }
    }

    static class Zombie{
        float x,y,speed;
        int row,hp,maxHp,damage;
        boolean boss,giga;
        long lastAttack,lastShot;

        Zombie(int r,float xx,float yy,boolean b,boolean g){
            row=r;x=xx;y=yy;boss=b;giga=g;

            maxHp=b?2500:g?900:300;
            hp=maxHp;

            speed=b?12:g?16:22;
            damage=b?150:100;
        }
    }

    class Pea{
        float x,y;
        int row,damage;
        boolean big,enemy;

        Pea(float xx,float yy,int r,int d,boolean b){
            this(xx,yy,r,d,b,false);
        }

        Pea(float xx,float yy,int r,int d,boolean b,boolean en){
            x=xx;y=yy;row=r;damage=d;big=b;enemy=en;
        }
    }

    class SunDrop{
        float x,y;
        SunDrop(float xx,float yy){x=xx;y=yy;}
    }

    class Mower{
        int row;
        float x;
        boolean used,active;

        Mower(int r){
            row=r;
            x=0;
        }
    }
            }
