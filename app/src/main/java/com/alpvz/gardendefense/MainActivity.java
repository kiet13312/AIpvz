package com.alpvz.gardendefense;

import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.*;
import android.view.*;
import java.util.*;

public class MainActivity extends Activity {
    private GameView game;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        game = new GameView(this);
        setContentView(game);
    }

    @Override protected void onPause() {
        super.onPause();
        if (game != null) game.save();
    }

    class GameView extends View {
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Random rnd = new Random();

        final int ROWS=5, COLS=9, MAX_LEVEL=9;
        final int SUN=1, PEA=2, GIGA=3, CHOMP=4, REPEAT=5, MINE=6;
        final int TOOL_NONE=0, TOOL_PF=1, TOOL_SHOVEL=2;

        Bitmap sunImg, peaImg, gigaImg, chompImg, repeatImg, mineImg, zombieImg, bulletImg;
        final Plant[][] plants = new Plant[ROWS][COLS];
        final ArrayList<Zombie> zombies = new ArrayList<>();
        final ArrayList<PeaShot> shots = new ArrayList<>();
        final ArrayList<DelayedShot> delayed = new ArrayList<>();
        final ArrayList<EnemyShot> enemyShots = new ArrayList<>();
        final Mower[] mowers = new Mower[ROWS];

        float left, top, cellW, cellH;
        int screen=0; // world/play/pause/win/lose
        int level=1, unlocked=1;
        int sun=500, coins=99999, plantFood=3;
        int selected=PEA, tool=TOOL_NONE;
        int wave=0, waves=3, waveSpawned=0, totalKilled=0;
        long lastTime, spawnTime, waveTime;
        boolean bossSpawned=false;
        int randomModeLevel=0, miniType=0, miniScore=0, miniGoal=8;
        boolean miniActive=false;
        float miniTimer=0f;
        SharedPreferences prefs;

        GameView(Context c) {
            super(c);
            setFocusable(true);
            prefs=getSharedPreferences("pvz_save",MODE_PRIVATE);
            unlocked=Math.max(1,prefs.getInt("unlocked",1));
            plantFood=Math.max(3,prefs.getInt("pf",3));

            sunImg=load("sun");
            peaImg=load("peashoot");
            gigaImg=load("giganut");
            chompImg=load("chomper");
            repeatImg=load("repeater");
            mineImg=load("min");
            zombieImg=load("zomplatz");
            bulletImg=load("gigapea");

            resetMowers();
            lastTime=System.currentTimeMillis();
        }

        Bitmap load(String n) {
            int id=getResources().getIdentifier(n,"drawable",getPackageName());
            return id==0?null:BitmapFactory.decodeResource(getResources(),id);
        }

        void save() {
            prefs.edit().putInt("unlocked",unlocked).putInt("pf",plantFood).apply();
        }

        void layout() {
            left=54f;
            top=74f;
            cellW=(getWidth()-left-8f)/COLS;
            cellH=(getHeight()-top-10f)/ROWS;
        }

        @Override protected void onDraw(Canvas c) {
            layout();
            if(screen==0){drawWorld(c);return;}
            if(screen==1){updateGame();drawGame(c);postInvalidateDelayed(30);return;}
            if(screen==2){drawGame(c);drawPause(c);return;}
            if(screen==3){drawGame(c);drawWin(c);return;}
            drawLose(c);
        }

        void drawWorld(Canvas c) {
            c.drawColor(Color.rgb(24,46,28));
            text(c,"GARDEN DEFENSE",getWidth()/2f,55,Color.WHITE,30,Paint.Align.CENTER);
            text(c,"CHỌN MÀN",getWidth()/2f,84,Color.LTGRAY,17,Paint.Align.CENTER);
            for(int i=1;i<=MAX_LEVEL;i++){
                int r=(i-1)/3,col=(i-1)%3;
                float bw=(getWidth()-90f)/3f,x=20+col*bw,y=110+r*68;
                boolean open=i<=unlocked;
                p.setColor(open?Color.rgb(55,150,70):Color.DKGRAY);
                c.drawRoundRect(new RectF(x,y,x+bw-12,y+52),10,10,p);
                text(c,"MÀN "+i,x+(bw-12)/2f,y+32,Color.WHITE,17,Paint.Align.CENTER);
            }
            text(c,"XU "+coins+"   PF "+plantFood,14,getHeight()-18,Color.YELLOW,15,Paint.Align.LEFT);
        }

        void drawGame(Canvas c) {
            c.drawColor(Color.rgb(97,171,67));
            drawTop(c); drawBoard(c); drawMowers(c); drawPlants(c);
            drawShots(c); drawEnemyShots(c); drawZombies(c); drawBottom(c); drawMiniOverlay(c);
        }

        void drawTop(Canvas c) {
            p.setColor(Color.rgb(25,62,28));
            c.drawRect(0,0,getWidth(),top-5,p);
            text(c,"MÀN "+level+"  WAVE "+(wave+1)+"/"+waves,8,20,Color.WHITE,13,Paint.Align.LEFT);
            text(c,"☀ "+sun+"   XU "+coins+"   PF "+plantFood,getWidth()/2f,20,Color.WHITE,13,Paint.Align.CENTER);
            circle(c,getWidth()-27,18,16,Color.DKGRAY);
            text(c,"II",getWidth()-27,23,Color.WHITE,11,Paint.Align.CENTER);

            { 
                float sw=getWidth()/6f;
                card(c,0,"SUN",SUN,sunImg,sw);
                card(c,1,"PEA",PEA,peaImg,sw);
                card(c,2,"GIGA",GIGA,gigaImg,sw);
                card(c,3,"CHOMP",CHOMP,chompImg,sw);
                card(c,4,"REPEAT",REPEAT,repeatImg,sw);
                card(c,5,"MINE",MINE,mineImg,sw);
            }
        }

        void card(Canvas c,int i,String name,int type,Bitmap b,float w){
            boolean open=unlocked(type), active=selected==type&&tool==TOOL_NONE;
            float x=i*w;
            p.setColor(!open?Color.GRAY:(active?Color.YELLOW:Color.WHITE));
            c.drawRoundRect(new RectF(x+2,29,x+w-2,top-6),7,7,p);
            if(open&&b!=null)c.drawBitmap(b,null,new RectF(x+5,32,x+42,top-10),p);
            text(c,open?name:"LOCK",x+w/2f+10,top-13,open?Color.DKGRAY:Color.LTGRAY,8,Paint.Align.CENTER);
        }

        void drawBoard(Canvas c){
            for(int r=0;r<ROWS;r++)for(int col=0;col<COLS;col++){
                float x=left+col*cellW,y=top+r*cellH;
                p.setColor((r+col)%2==0?Color.rgb(119,190,72):Color.rgb(107,178,62));
                c.drawRect(x,y,x+cellW-2,y+cellH-2,p);
            }
        }

        void drawPlants(Canvas c){
            for(int r=0;r<ROWS;r++)for(int col=0;col<COLS;col++){
                Plant a=plants[r][col];
                if(a==null)continue;
                Bitmap b=plantImage(a.type);
                if(b!=null)c.drawBitmap(b,null,new RectF(a.x+4,a.y+4,a.x+a.w-4,a.y+a.h-4),p);
                else{p.setColor(Color.rgb(60,170,70));c.drawCircle(a.x+a.w/2,a.y+a.h/2,22,p);}
                if(a.type!=MINE)bar(c,a.x+6,a.y+4,a.w-12,a.hp,a.maxHp);
                if(a.pfTimer>0)text(c,"PF",a.x+7,a.y+18,Color.MAGENTA,10,Paint.Align.LEFT);
                if(a.type==MINE&&!a.ready)text(c,""+(int)Math.ceil(a.mineTimer),a.x+a.w/2,a.y+a.h*.60f,Color.WHITE,12,Paint.Align.CENTER);
            }
        }

        void drawMowers(Canvas c){
            for(Mower m:mowers){
                if(m.used&&!m.active)continue;
                float y=top+m.row*cellH+cellH*.72f;
                p.setColor(Color.RED);c.drawRect(m.x-26,y-14,m.x+26,y+14,p);
                p.setColor(Color.DKGRAY);c.drawCircle(m.x-15,y+16,8,p);c.drawCircle(m.x+15,y+16,8,p);
            }
        }

        void drawShots(Canvas c){
            for(PeaShot s:shots){
                if(bulletImg!=null)c.drawBitmap(bulletImg,null,new RectF(s.x-11,s.y-11,s.x+11,s.y+11),p);
                else{p.setColor(Color.GREEN);c.drawCircle(s.x,s.y,7,p);}
            }
        }

        void drawEnemyShots(Canvas c){
            for(EnemyShot s:enemyShots){p.setColor(Color.rgb(70,220,70));c.drawCircle(s.x,s.y,8,p);}
        }
        
        void drawZombies(Canvas c){
            for(Zombie z:zombies){
                if(z.dead)continue;
                float w=z.boss?cellW*1.18f:(z.tough?76:62);
                float h=z.boss?cellH*1.18f:(z.tough?106:88);
                if(zombieImg!=null)c.drawBitmap(zombieImg,null,new RectF(z.x-w/2,z.y-h/2,z.x+w/2,z.y+h/2),p);
                else{p.setColor(z.boss?Color.rgb(70,30,70):Color.GRAY);c.drawOval(new RectF(z.x-w/2,z.y-h/2,z.x+w/2,z.y+h/2),p);}
                bar(c,z.x-w/2,z.y-h/2-7,w,z.hp,z.maxHp);
                if(z.boss)text(c,"BOSS",z.x,z.y-h/2-12,Color.YELLOW,11,Paint.Align.CENTER);
            }
        }

        void drawBottom(Canvas c){
            float y=getHeight()-42;
            circle(c,getWidth()-145,y,26,tool==TOOL_PF?Color.YELLOW:Color.rgb(55,130,65));
            circle(c,getWidth()-75,y,26,tool==TOOL_SHOVEL?Color.YELLOW:Color.rgb(100,85,70));
            text(c,"PF",getWidth()-145,y+5,Color.WHITE,11,Paint.Align.CENTER);
            text(c,"X",getWidth()-75,y+5,Color.WHITE,13,Paint.Align.CENTER);
        }

        void circle(Canvas c,float x,float y,float r,int color){p.setColor(color);c.drawCircle(x,y,r,p);}

        void drawMiniOverlay(Canvas c){
            if(!miniActive)return;
            p.setColor(0xee17351b); c.drawRect(0,0,getWidth(),getHeight(),p);
            String title=miniType==1?"MINIGAME: ĐẬP ZOMBIE":(miniType==2?"MINIGAME: THU MẦM":"MINIGAME: CHỌN Ô");
            text(c,title,getWidth()/2f,52,Color.WHITE,24,Paint.Align.CENTER);
            text(c,"Điểm "+miniScore+"/"+miniGoal,getWidth()/2f,82,Color.YELLOW,17,Paint.Align.CENTER);
            text(c,"Thời gian "+(int)Math.ceil(miniTimer),getWidth()-18,28,Color.WHITE,13,Paint.Align.RIGHT);
            if(miniType==1){for(int i=0;i<6;i++){float x=90+(i%3)*220,y=145+(i/3)*125;p.setColor(Color.DKGRAY);c.drawCircle(x,y,32,p);text(c,"Z",x,y+9,Color.WHITE,24,Paint.Align.CENTER);}}
            else if(miniType==2){for(int i=0;i<8;i++){float x=75+(i%4)*155,y=145+(i/4)*120;p.setColor(Color.rgb(65,155,70));c.drawRoundRect(new RectF(x-38,y-28,x+38,y+28),10,10,p);text(c,"MẦM",x,y+5,Color.WHITE,12,Paint.Align.CENTER);}}
            else{for(int i=0;i<9;i++){float x=85+(i%3)*190,y=135+(i/3)*100;p.setColor(Color.rgb(82,145,72));c.drawRect(x-58,y-32,x+58,y+32,p);text(c,"Ô "+(i+1),x,y+5,Color.WHITE,14,Paint.Align.CENTER);}}
            text(c,"Chạm màn hình để chơi",getWidth()/2f,getHeight()-32,Color.LTGRAY,15,Paint.Align.CENTER);
        }

        void updateMiniGame(float dt){
            miniTimer-=dt;
            if(miniScore>=miniGoal || miniTimer<=0){
                miniActive=false;
                level=randomModeLevel; waves=wavesForLevel(); wave=0; waveSpawned=0; bossSpawned=false;
                zombies.clear(); shots.clear(); delayed.clear(); enemyShots.clear();
                for(int r=0;r<ROWS;r++)Arrays.fill(plants[r],null); resetMowers();
                lastTime=spawnTime=waveTime=System.currentTimeMillis();
            }
        }

        void updateGame(){
            long now=System.currentTimeMillis();
            float frameDt=Math.max(0f,Math.min(.08f,(now-lastTime)/1000f));
            if(miniActive){updateMiniGame(frameDt);return;}
            float dt=Math.max(0f,Math.min(.08f,(now-lastTime)/1000f));
            lastTime=now;

            if(waveSpawned<waveTotal()){
                if(now-spawnTime>Math.max(800,2100-level*100)){spawnZombie();spawnTime=now;}
            }else if(zombies.isEmpty()){
                if(wave<waves-1){
                    if(now-waveTime>1600){wave++;waveSpawned=0;waveTime=now;spawnTime=now;}
                }else if(!bossRequired()||bossSpawned){
                    screen=3;
                    if(level<MAX_LEVEL)unlocked=Math.max(unlocked,level+1);
                    save();return;
                }
            }

            updatePlants(dt);updateDelayed(dt);updateShots(dt);updateEnemyShots(dt);
            updateZombies(dt);updateMowers(dt);cleanDead();

            if(level==MAX_LEVEL&&!bossSpawned&&wave>=waves-1&&zombies.isEmpty()&&waveSpawned>=waveTotal())
                spawnBoss();
        }

        int wavesForLevel(){return 3+Math.min(2,(level-1)/4);}
        int waveTotal(){return 4+level+wave*2;}
        boolean bossRequired(){return level==MAX_LEVEL;}

        void spawnZombie(){
            int row=rnd.nextInt(ROWS);
            boolean tough=level>=2&&waveSpawned%4==0;
            int hp=tough?900+level*100:320+level*40;
            float speed=tough?19f:24f;
            zombies.add(new Zombie(getWidth()+70,top+row*cellH+cellH*.5f,row,hp,speed,false,tough));
            waveSpawned++;
        }

        void spawnBoss(){
            if(bossSpawned)return;
            int row=1+rnd.nextInt(3);
            Zombie z=new Zombie(left+cellW*7.5f,top+row*cellH+cellH*.5f,row,9000,0,true,false);
            z.bossDir=-1;z.bossShoot=1.5f;z.bossW=cellW*1.18f;z.bossH=cellH*1.18f;
            zombies.add(z);bossSpawned=true;
        }

        void updatePlants(float dt){
            for(int r=0;r<ROWS;r++)for(int col=0;col<COLS;col++){
                Plant a=plants[r][col];if(a==null)continue;
                a.timer-=dt;if(a.pfTimer>0)a.pfTimer-=dt;

                if(a.type==MINE){
                    if(!a.ready){a.mineTimer-=dt;if(a.mineTimer<=0)a.ready=true;}
                    else for(Zombie z:zombies)if(!z.dead&&!z.boss&&z.row==r&&Math.abs(z.x-(a.x+a.w/2))<cellW*.45f){
                        z.hp-=1800;a.hp=0;break;
                    }
                    continue;
                }

                if(a.type==SUN&&a.timer<=0){sun+=a.pfTimer>0?100:50;a.timer=a.pfTimer>0?.7f:5f;}
                if(a.type==PEA&&a.timer<=0&&rowHas(r)){addPea(a,25);a.timer=a.pfTimer>0?.35f:1.1f;}
                if(a.type==REPEAT&&a.timer<=0&&rowHas(r)){
                    addPea(a,28);
                    delayed.add(new DelayedShot(a.x+a.w*.88f,a.y+a.h*.43f,r,28,.5f));
                    a.timer=a.pfTimer>0?.4f:1.15f;
                }
                if(a.type==CHOMP&&a.timer<=0){Zombie z=nearest(a);if(z!=null){z.hp=0;a.timer=40f;}}
                if(a.type==GIGA)a.hp=Math.min(a.maxHp,a.hp);
            }
        }

        void addPea(Plant a,int damage){shots.add(new PeaShot(a.x+a.w*.88f,a.y+a.h*.43f,a.row,damage));}

        void updateDelayed(float dt){
            Iterator<DelayedShot> it=delayed.iterator();
            while(it.hasNext()){
                DelayedShot d=it.next();d.delay-=dt;
                if(d.delay<=0){shots.add(new PeaShot(d.x,d.y,d.row,d.damage));it.remove();}
            }
        }

        void updateShots(float dt){
            Iterator<PeaShot> it=shots.iterator();
            while(it.hasNext()){
                PeaShot s=it.next();s.x+=540f*dt;boolean hit=false;
                for(Zombie z:zombies)if(!z.dead&&z.row==s.row&&Math.abs(z.x-s.x)<28){z.hp-=s.damage;hit=true;break;}
                if(hit||s.x>getWidth()+60)it.remove();
            }
        }

        void updateEnemyShots(float dt){
            Iterator<EnemyShot> it=enemyShots.iterator();
            while(it.hasNext()){
                EnemyShot s=it.next();s.x-=420f*dt;
                for(int c=COLS-1;c>=0;c--){
                    Plant a=plants[s.row][c];
                    if(a!=null&&Math.abs((a.x+a.w/2)-s.x)<28){a.hp-=180;s.dead=true;break;}
                }
                if(s.x<left-30||s.dead)it.remove();
            }
        }

        void updateZombies(float dt){
            for(Zombie z:zombies){
                if(z.dead)continue;
                if(z.boss){
                    z.x+=z.bossDir*70f*dt;
                    float min=left+cellW*.7f,max=getWidth()-cellW*.8f;
                    if(z.x<=min){z.x=min;z.bossDir=1;}
                    if(z.x>=max){z.x=max;z.bossDir=-1;}
                    z.bossShoot-=dt;
                    if(z.bossShoot<=0){enemyShots.add(new EnemyShot(z.x-z.bossW*.45f,z.y,z.row));z.bossShoot=2f;}
                    continue;
                }

                Plant target=findPlant(z);
                if(target!=null){
                    z.attack-=dt;
                    if(z.attack<=0){target.hp-=z.tough?24:12;z.attack=z.tough?.65f:.8f;}
                }else z.x-=z.speed*dt;

                if(z.x<=left-25){
                    Mower m=mowers[z.row];
                    if(!m.used){m.used=true;m.active=true;m.x=left-45;}
                    else{screen=4;z.dead=true;return;}
                    z.dead=true;
                }
            }
        }

        void updateMowers(float dt){
            for(Mower m:mowers){
                if(!m.active)continue;
                m.x+=700f*dt;
                for(Zombie z:zombies)if(!z.dead&&!z.boss&&z.row==m.row&&Math.abs(z.x-m.x)<62)z.hp=0;
                if(m.x>getWidth()+90)m.active=false;
            }
        }

        void cleanDead(){
            for(int r=0;r<ROWS;r++)for(int c=0;c<COLS;c++)
                if(plants[r][c]!=null&&plants[r][c].hp<=0)plants[r][c]=null;

            Iterator<Zombie> it=zombies.iterator();
            while(it.hasNext()){
                Zombie z=it.next();
                if(z.hp<=0||z.dead){if(z.hp<=0&&!z.boss)coins=99999;it.remove();}
            }
        }

        Zombie nearest(Plant a){
                                Zombie best=null;float bd=Float.MAX_VALUE;
            for(Zombie z:zombies)if(!z.dead&&z.row==a.row){
                float d=Math.abs(z.x-(a.x+a.w));
                if(d<cellW*1.5f&&d<bd){bd=d;best=z;}
            }
            return best;
        }

        Plant findPlant(Zombie z){
            for(int c=0;c<COLS;c++){
                Plant a=plants[z.row][c];
                if(a!=null&&Math.abs(z.x-(a.x+a.w/2))<(z.boss?65:48))return a;
            }
            return null;
        }

        boolean rowHas(int row){for(Zombie z:zombies)if(!z.dead&&z.row==row)return true;return false;}

        void useFood(int row,int col){
            if(plantFood<=0)return;
            Plant a=plants[row][col];if(a==null)return;
            plantFood--;a.pfTimer=8f;a.timer=0;
            if(a.type==GIGA){a.maxHp=8000;a.hp=8000;}
            if(a.type==CHOMP)for(Zombie z:zombies)if(z.row==row&&!z.dead&&!z.boss)z.hp=0;
            if(a.type==MINE)a.ready=true;
            save();
        }

        void plant(int row,int col,int type){
            if(row<0||row>=ROWS||col<0||col>=COLS||plants[row][col]!=null||!unlocked(type))return;
            int cost=type==SUN?50:type==PEA?100:type==GIGA?175:type==CHOMP?150:type==REPEAT?200:25;
            int hp=type==GIGA?4000:type==CHOMP?800:350;
            if(sun<cost)return;
            sun-=cost;
            Plant a=new Plant(type,row,col,hp,left+col*cellW,top+row*cellH,cellW,cellH);
            if(type==MINE){a.mineTimer=30f;a.ready=false;}
            plants[row][col]=a;
            selected=PEA;
        }

        void startLevel(int lv){
            level=Math.max(1,Math.min(MAX_LEVEL,lv));
            waves=wavesForLevel(); wave=0; waveSpawned=0; bossSpawned=false;
            selected=PEA; tool=TOOL_NONE; miniActive=false; miniType=0; miniTimer=0f; miniScore=0;
            randomModeLevel=level;
            zombies.clear(); shots.clear(); delayed.clear(); enemyShots.clear();
            for(int r=0;r<ROWS;r++)Arrays.fill(plants[r],null);
            resetMowers();
            lastTime=spawnTime=waveTime=System.currentTimeMillis();
            if(level>=5 && level<=8){
                randomModeLevel=5+rnd.nextInt(4);
                miniType=1+rnd.nextInt(3);
                miniTimer=20f; miniGoal=8; miniScore=0; miniActive=true;
            }
            screen=1;
        }

        void resetMowers(){
            for(int r=0;r<ROWS;r++)mowers[r]=new Mower(r,left-45);
        }


        Bitmap plantImage(int type){
            if(type==SUN)return sunImg;
            if(type==PEA)return peaImg;
            if(type==GIGA)return gigaImg;
            if(type==CHOMP)return chompImg;
            if(type==REPEAT)return repeatImg;
            if(type==MINE)return mineImg;
            return null;
        }

        String plantName(int type){
            if(type==SUN)return"SUN";
            if(type==PEA)return"PEA";
            if(type==GIGA)return"GIGA";
            if(type==CHOMP)return"CHOMP";
            if(type==REPEAT)return"REPEAT";
            if(type==MINE)return"MINE";
            return"";
        }

        void drawPause(Canvas c){
            p.setColor(0xaa000000);c.drawRect(0,0,getWidth(),getHeight(),p);
            text(c,"TẠM DỪNG",getWidth()/2f,getHeight()/2f-40,Color.WHITE,30,Paint.Align.CENTER);
            button(c,getWidth()/2f-150,getHeight()/2f,getWidth()/2f-20,getHeight()/2f+55,"CHƠI LẠI",Color.rgb(45,140,65));
            button(c,getWidth()/2f+20,getHeight()/2f,getWidth()/2f+150,getHeight()/2f+55,"THOÁT",Color.rgb(110,75,70));
        }

        void drawWin(Canvas c){
            p.setColor(0xcc000000);c.drawRect(0,0,getWidth(),getHeight(),p);
            text(c,"CHIẾN THẮNG!",getWidth()/2f,getHeight()/2f-45,Color.WHITE,30,Paint.Align.CENTER);
            button(c,getWidth()/2f-150,getHeight()/2f,getWidth()/2f-20,getHeight()/2f+55,level<MAX_LEVEL?"MÀN TIẾP":"VỀ MENU",Color.rgb(45,140,65));
            button(c,getWidth()/2f+20,getHeight()/2f,getWidth()/2f+150,getHeight()/2f+55,"THOÁT",Color.rgb(110,75,70));
        }

        void drawLose(Canvas c){
            c.drawColor(Color.rgb(18,18,18));
            text(c,"GAME OVER",getWidth()/2f,getHeight()/2f-35,Color.WHITE,32,Paint.Align.CENTER);
            text(c,"Máy cắt cỏ của hàng này đã dùng rồi.",getWidth()/2f,getHeight()/2f+2,Color.LTGRAY,14,Paint.Align.CENTER);
            button(c,getWidth()/2f-130,getHeight()/2f+30,getWidth()/2f+130,getHeight()/2f+85,"CHƠI LẠI",Color.rgb(45,140,65));
        }

        void bar(Canvas c,float x,float y,float w,int hp,int max){
            p.setColor(Color.RED);c.drawRect(x,y,x+w,y+5,p);
            p.setColor(Color.GREEN);
            float q=Math.max(0,Math.min(1,hp/(float)Math.max(1,max)));
            c.drawRect(x,y,x+w*q,y+5,p);
        }

        void text(Canvas c,String s,float x,float y,int color,float size,Paint.Align a){
            p.setColor(color);p.setTextSize(size);p.setTextAlign(a);c.drawText(s,x,y,p);
        }

        void button(Canvas c,float l,float t,float r,float b,String s,int color){
            p.setColor(color);c.drawRoundRect(new RectF(l,t,r,b),9,9,p);
            text(c,s,(l+r)/2,(t+b)/2+6,Color.WHITE,14,Paint.Align.CENTER);
        }

        @Override public boolean onTouchEvent(MotionEvent e){
            if(e.getAction()!=MotionEvent.ACTION_UP)return true;
            float x=e.getX(),y=e.getY();

            if(screen==1 && miniActive){
                miniScore++;
                if(miniScore>=miniGoal){miniActive=false; updateMiniGame(0f);}
                invalidate(); return true;
            }

            if(screen==0){
                for(int i=1;i<=MAX_LEVEL;i++){
                    int r=(i-1)/3,c=(i-1)%3;
                    float bw=(getWidth()-90f)/3f,bx=20+c*bw,by=110+r*68;
                    if(x>=bx&&x<=bx+bw-12&&y>=by&&y<=by+52&&i<=unlocked){
                        startLevel(i);return true;
                    }
                }
                return true;
            }

            if(screen==2){
                if(y>getHeight()/2f&&y<getHeight()/2f+60&&x<getWidth()/2f)startLevel(level);
                else if(y>getHeight()/2f&&y<getHeight()/2f+60)screen=0;
                invalidate();return true;
            }

            if(screen==3){
                if(x<getWidth()/2f&&level<MAX_LEVEL)startLevel(level+1);
                else screen=0;
                invalidate();return true;
            }

            if(screen==4){startLevel(level);return true;}

            if(x>getWidth()-55&&y<42){screen=2;return true;}

            float by=getHeight()-42;
            if(Math.hypot(x-(getWidth()-145),y-by)<31){tool=TOOL_PF;selected=PEA;return true;}
            if(Math.hypot(x-(getWidth()-75),y-by)<31){tool=TOOL_SHOVEL;selected=PEA;return true;}
            if(y>=28&&y<top){
                float sw=getWidth()/6f;
                int slot=(int)(x/sw);
                selected=slot==0?SUN:slot==1?PEA:slot==2?GIGA:slot==3?CHOMP:slot==4?REPEAT:MINE;
                if(!unlocked(selected))selected=PEA;
                tool=TOOL_NONE;return true;
            }

            if(x<left||y<top||x>=left+COLS*cellW||y>=top+ROWS*cellH)return true;
            int col=(int)((x-left)/cellW),row=(int)((y-top)/cellH);

            if(tool==TOOL_PF){useFood(row,col);tool=TOOL_NONE;}
            else if(tool==TOOL_SHOVEL){plants[row][col]=null;tool=TOOL_NONE;}
            else plant(row,col,selected);

            invalidate();
            return true;
        }

        class Plant{
            int type,row,col,hp,maxHp;float x,y,w,h,timer=.2f,pfTimer,mineTimer=30f;boolean ready;
            Plant(int t,int r,int c,int hp,float x,float y,float w,float h){
                type=t;row=r;col=c;this.hp=this.maxHp=hp;this.x=x;this.y=y;this.w=w;this.h=h;
            }
        }

        class Zombie{
            float x,y,speed,attack=.8f,bossDir=1,bossShoot=2,bossW,bossH;
            int row,hp,maxHp;boolean boss,dead,tough;
            Zombie(float x,float y,int row,int hp,float speed,boolean boss,boolean tough){
                this.x=x;this.y=y;this.row=row;this.hp=this.maxHp=hp;this.speed=speed;this.boss=boss;this.tough=tough;
            }
        }

        class PeaShot{float x,y;int row,damage;PeaShot(float x,float y,int row,int damage){this.x=x;this.y=y;this.row=row;this.damage=damage;}}
        class DelayedShot{float x,y,delay;int row,damage;DelayedShot(float x,float y,int row,int damage,float delay){this.x=x;this.y=y;this.row=row;this.damage=damage;this.delay=delay;}}
        class EnemyShot{float x,y;int row;boolean dead;EnemyShot(float x,float y,int row){this.x=x;this.y=y;this.row=row;}}
        class Mower{int row;float x;boolean used,active;Mower(int row,float x){this.row=row;this.x=x;}}
    }
                }
            
