package com.alpvz.gardendefense;

import android.app.*;
import android.os.*;
import android.content.pm.ActivityInfo;
import android.graphics.*;
import android.media.*;
import android.view.*;
import java.util.*;

public class MainActivity extends Activity{
 GameView g; SoundPool sp; int shoot; MediaPlayer pf;
 boolean pfPlaying=false;

 @Override public void onCreate(Bundle b){
  super.onCreate(b);
  requestWindowFeature(Window.FEATURE_NO_TITLE);
  getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
  getWindow().getDecorView().setSystemUiVisibility(5894);
  setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
  AudioAttributes a=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
  sp=new SoundPool.Builder().setAudioAttributes(a).setMaxStreams(12).build();
  shoot=sp.load(this,R.raw.peashoot,1);
  g=new GameView();
  setContentView(g);
 }

 void peaSound(){
  if(!pfPlaying&&sp!=null)sp.play(shoot,1,1,1,0,1);
 }

 void pfSound(){
  if(pfPlaying)return;
  pfPlaying=true;
  if(pf!=null)pf.release();
  pf=MediaPlayer.create(this,R.raw.peashootplantfood);
  if(pf==null){pfPlaying=false;return;}
  pf.setOnCompletionListener(x->{pfPlaying=false;pf.release();pf=null;});
  pf.start();
 }

 @Override protected void onDestroy(){
  if(pf!=null)pf.release();
  if(sp!=null)sp.release();
  super.onDestroy();
 }

 @Override public void onBackPressed(){
  if(g.screen==2)g.screen=3;
  else if(g.screen!=0)g.screen=0;
  else super.onBackPressed();
  g.invalidate();
 }

 class GameView extends View{
  static final int R=5,C=9;
  static final int SUN=1,PEA=2,GIGA=3,CHOMP=4,REP=5,MINE=6;
  static final int HOME=0,LEVEL=1,PLAY=2,PAUSE=3,WIN=4,LOSE=5;
  Paint p=new Paint(3),t=new Paint(3);
  Random rd=new Random();
  Plant[][] pl=new Plant[R][C];
  ArrayList<Zombie> zs=new ArrayList<>();
  ArrayList<Pea> bs=new ArrayList<>();
  ArrayList<Sun> ss=new ArrayList<>();
  Mower[] mw=new Mower[R];

  Bitmap sunImg,peaImg,gigaImg,chompImg,repImg,mineImg,zomImg,bulletImg;
  int screen=HOME,level=1,unlocked=1,selected=PEA,tool=0;
  int sun=500,coins=99999,food=3,wave=0,total=5,spawned=0,target=5;
  long last=System.currentTimeMillis(),spawn,lastSun;
  float left,top,cw,ch;

  GameView(){
   super(MainActivity.this);
   setFocusable(true);
   load();
   for(int i=0;i<R;i++)mw[i]=new Mower(i);
  }

  void load(){
   sunImg=img("sun");peaImg=img("peashoot");gigaImg=img("giganut");
   chompImg=img("chomper");repImg=img("repeater");mineImg=img("min");
   zomImg=img("zomplatz");bulletImg=img("gigapea");
  }

  Bitmap img(String n){
   int id=getResources().getIdentifier(n,"drawable",getPackageName());
   return id==0?null:BitmapFactory.decodeResource(getResources(),id);
  }

  @Override protected void onSizeChanged(int w,int h,int ow,int oh){
   left=w*.18f;top=h*.19f;cw=w*.073f;ch=h*.125f;
  }

  @Override protected void onDraw(Canvas c){
   if(screen==HOME){home(c);return;}
   if(screen==LEVEL){levels(c);return;}
   game(c);
   if(screen==PAUSE)pause(c);
   if(screen==WIN)win(c);
   if(screen==LOSE)lose(c);
   if(screen==PLAY){update();postInvalidateDelayed(30);}
  }

  void home(Canvas c){
   c.drawColor(Color.rgb(25,55,30));
   txt(c,"GARDEN DEFENSE",getWidth()/2f,getHeight()*.25f,getHeight()*.07f,Color.WHITE);
   btn(c,.32f,.38f,.68f,.50f,"CHƠI");
   btn(c,.32f,.55f,.68f,.67f,"CHỌN MÀN");
  }

  void levels(Canvas c){
   c.drawColor(Color.rgb(20,50,25));
   txt(c,"CHỌN MÀN",getWidth()/2f,getHeight()*.10f,getHeight()*.06f,Color.WHITE);
   for(int i=1;i<=9;i++){
    int cc=(i-1)%3,rr=(i-1)/3;
    float x=.18f+cc*.22f,y=.18f+rr*.20f;
    p.setColor(i<=unlocked?Color.rgb(65,145,70):Color.DKGRAY);
    c.drawRoundRect(getWidth()*x,getHeight()*y,getWidth()*(x+.17f),getHeight()*(y+.13f),18,18,p);
    txt(c,i<=unlocked?"MÀN "+i:"KHÓA",getWidth()*(x+.085f),getHeight()*(y+.083f),getHeight()*.035f,Color.WHITE);
   }
   btn(c,.04f,.83f,.20f,.94f,"QUAY LẠI");
  }

  void game(Canvas c){
   c.drawColor(Color.rgb(92,155,70));
   p.setColor(Color.rgb(38,78,40));c.drawRect(0,0,getWidth(),top,p);

   txtL(c,"☀ "+sun,12,getHeight()*.045f,Color.YELLOW);
   txtL(c,"Màn "+level,getWidth()*.34f,getHeight()*.045f,Color.WHITE);
   txtL(c,"Sóng "+wave+"/"+total,getWidth()*.46f,getHeight()*.045f,Color.WHITE);
   txtL(c,"Xu "+coins,getWidth()*.70f,getHeight()*.045f,Color.YELLOW);
   txtL(c,"PF "+food,getWidth()*.87f,getHeight()*.045f,Color.WHITE);

   btn(c,.76f,.075f,.91f,.145f,"MUA PF 100");
   cards(c);board(c);mowers(c);plants(c);zombies(c);peas(c);suns(c);
  }

  void cards(Canvas c){
   int[] a={SUN,PEA,GIGA,CHOMP,REP,MINE};
   for(int i=0;i<6;i++){
    float x=getWidth()*.01f+i*getWidth()*.075f,y=getHeight()*.075f,w=getWidth()*.065f,h=getHeight()*.09f;
    p.setColor(selected==a[i]&&tool==0?Color.YELLOW:Color.rgb(45,80,45));
    c.drawRoundRect(x,y,x+w,y+h,10,10,p);
    if(open(a[i]))plant(c,a[i],x+w/2,y+h/2,Math.min(w,h)*.62f);
    else txt(c,"🔒",x+w/2,y+h*.67f,h*.35f,Color.GRAY);
   }
   p.setColor(tool==100?Color.YELLOW:Color.DKGRAY);
   c.drawRoundRect(getWidth()*.465f,y0(),getWidth()*.53f,getHeight()*.165f,10,10,p);
   txt(c,"X",getWidth()*.497f,getHeight()*.135f,getHeight()*.035f,Color.WHITE);

   p.setColor(tool==101?Color.YELLOW:Color.rgb(90,45,125));
   c.drawRoundRect(getWidth()*.54f,y0(),getWidth()*.63f,getHeight()*.165f,10,10,p);
   txt(c,"PF "+food,getWidth()*.585f,getHeight()*.135f,getHeight()*.025f,Color.WHITE);
  }

  float y0(){return getHeight()*.075f;}

  void board(Canvas c){
   for(int r=0;r<R;r++)for(int q=0;q<C;q++){
    p.setColor((r+q)%2==0?Color.rgb(103,166,78):Color.rgb(91,153,67));
    c.drawRect(left+q*cw,top+r*ch,left+(q+1)*cw,top+(r+1)*ch,p);
   }
   p.setColor(Color.rgb(130,92,52));c.drawRect(0,top,left,top+R*ch,p);
  }

  void plants(Canvas c){
   for(int r=0;r<R;r++)for(int q=0;q<C;q++){
    Plant a=pl[r][q];if(a==null)continue;
    float x=left+q*cw+cw/2,y=top+r*ch+ch/2;
    plant(c,a.type,x,y,Math.min(cw,ch)*.72f);
    hp(c,x-cw*.3f,y+ch*.33f,cw*.6f,a.hp,a.max);
   }
  }

  void plant(Canvas c,int type,float x,float y,float s){
   Bitmap b=type==SUN?sunImg:type==PEA?peaImg:type==GIGA?gigaImg:type==CHOMP?chompImg:type==REP?repImg:mineImg;
   if(b!=null){c.drawBitmap(b,null,new RectF(x-s*.48f,y-s*.48f,x+s*.48f,y+s*.48f),p);return;}
   p.setColor(type==SUN?Color.YELLOW:type==GIGA?Color.rgb(155,105,55):type==CHOMP?Color.rgb(145,80,185):Color.rgb(55,175,70));
   c.drawCircle(x,y,s*.38f,p);
  }

  void zombies(Canvas c){
   for(Zombie z:zs){
    float w=z.boss?cw*1.18f:cw*.60f,h=z.boss?ch*1.15f:ch*.78f;
    if(zomImg!=null)c.drawBitmap(zomImg,null,new RectF(z.x-w/2,z.y-h*.1f,z.x+w/2,z.y+h*.9f),p);
    else{p.setColor(z.boss?Color.rgb(95,40,110):Color.GRAY);c.drawOval(new RectF(z.x-w/2,z.y-h*.1f,z.x+w/2,z.y+h*.9f),p);}
    hp(c,z.x-w/2,z.y-h*.2f,w,z.hp,z.max);
   }
  }

  void peas(Canvas c){
   for(Pea b:bs){
    float s=b.big?22:10;
    if(bulletImg!=null)c.drawBitmap(bulletImg,null,new RectF(b.x-s,b.y-s,b.x+s,b.y+s),p);
    else{p.setColor(b.big?Color.rgb(80,255,80):Color.GREEN);c.drawCircle(b.x,b.y,b.big?15:7,p);}
   }
  }

  void suns(Canvas c){
   for(Sun s:ss){
    p.setColor(Color.YELLOW);c.drawCircle(s.x,s.y,16,p);
    txt(c,"+",s.x,s.y+5,15,Color.rgb(120,80,0));
   }
  }

  void mowers(Canvas c){
   for(Mower m:mw){
    float y=top+m.row*ch+ch*.7f;
    p.setColor(m.used?Color.DKGRAY:Color.rgb(180,70,40));
    c.drawRoundRect(m.x-cw*.3f,y-ch*.23f,m.x+cw*.3f,y+ch*.02f,7,7,p);
    p.setColor(Color.BLACK);c.drawCircle(m.x-cw*.18f,y+5,7,p);c.drawCircle(m.x+cw*.18f,y+5,7,p);
   }
  }

  void update(){
   long n=System.currentTimeMillis();
   float dt=Math.min(.05f,(n-last)/1000f);last=n;

   if(n-lastSun>8000){lastSun=n;ss.add(new Sun(left+rd.nextInt(C)*cw+cw/2,top*.8f));}
   updatePlants(n);updatePeas(dt);updateZombies(n,dt);updateMowers(dt);dead();

   if(spawned<target&&n-spawn>=delay()){spawnZombie();spawned++;spawn=n;}

   if(spawned>=target&&zs.isEmpty()){
    wave++;
    if(wave>=total)win();
    else{spawned=0;target=waveTarget();spawn=n;}
   }
  }

  void updatePlants(long n){
   for(int r=0;r<R;r++)for(int q=0;q<C;q++){
    Plant a=pl[r][q];if(a==null)continue;
    boolean pf=n<a.pfUntil;
    long cd=pf?100:a.type==SUN?7000:a.type==REP?1800:1500;

    if(a.type==SUN&&n-a.last>=cd){
     ss.add(new Sun(left+q*cw+cw/2,top+r*ch+ch*.25f));a.last=n;
    }

    if((a.type==PEA||a.type==REP)&&n-a.last>=cd&&has(r)){
     fire(r,q,a.type==REP?28:30,false);a.last=n;
     if(a.type==REP)a.second=n+500;
    }

    if(a.type==REP&&a.second>0&&n>=a.second){
     fire(r,q,28,false);a.second=0;
     fire(r,q,1200,true);
    }

    if(a.type==CHOMP&&n-a.last>=3500){
     Zombie z=near(r,q);if(z!=null){z.hp=0;a.last=n;}
    }

    if(a.type==MINE&&n>=a.armed){
     Zombie z=cell(r,q);if(z!=null){z.hp=0;a.hp=0;}
    }
   }
  }

  void fire(int r,int q,int dmg,boolean big){
   bs.add(new Pea(left+q*cw+cw*.55f,top+r*ch+ch*.5f,r,dmg,big,false,1));
   peaSound();
  }

  void updatePeas(float dt){
   ArrayList<Pea> add=new ArrayList<>();
   Iterator<Pea> it=bs.iterator();

   while(it.hasNext()){
    Pea b=it.next();
    b.x+=b.dir*cw*9f*dt;

    if(b.fromZombie){
     Plant hit=null;
     for(int q=0;q<C;q++)if(pl[b.row][q]!=null&&Math.abs(left+q*cw+cw/2-b.x)<cw*.3f){hit=pl[b.row][q];break;}
     if(hit!=null){hit.hp-=b.damage;it.remove();}
     else if(b.x<left-cw)it.remove();
     continue;
    }

    Zombie caught=null;
    long n=System.currentTimeMillis();

    for(Zombie z:zs)if(z.boss&&z.row==b.row&&Math.abs(z.x-b.x)<(b.big?cw*.42f:cw*.28f)&&n>=z.catchReady){
     caught=z;break;
    }

    if(caught!=null){
     caught.catchReady=n+5000;
     it.remove();
     add.add(new Pea(caught.x-cw*.35f,caught.y+ch*.35f,caught.row,500,false,true,-1));
     continue;
    }

    Zombie hit=null;
    for(Zombie z:zs)if(z.row==b.row&&Math.abs(z.x-b.x)<(b.big?cw*.42f:cw*.28f)){hit=z;break;}
    if(hit!=null){hit.hp-=b.damage;it.remove();}
    else if(b.x>getWidth()+30)it.remove();
   }
   bs.addAll(add);
  }

  void updateZombies(long n,float dt){
   for(Zombie z:zs){
    if(z.hp<=0)continue;

    if(z.boss){
     z.x+=z.dir*cw*1.4f*dt;
     float mn=left+cw*2,mx=getWidth()-cw;
     if(z.x<mn){z.x=mn;z.dir=1;}
     if(z.x>mx){z.x=mx;z.dir=-1;}
    }else{
     Plant a=at(z);
     if(a!=null){
      if(n-z.lastAttack>=800){a.hp-=z.damage;z.lastAttack=n;}
     }else z.x-=Math.min(z.speed,getWidth()*.035f)*dt;
    }

    if(!z.boss&&z.x<left-cw*.4f){
     Mower m=mw[z.row];
     if(!m.used){m.used=true;m.active=true;m.x=left-cw*.5f;}
     else{screen=LOSE;return;}
    }
   }
  }

  void updateMowers(float dt){
   for(Mower m:mw)if(m.active){
    m.x+=cw*18f*dt;
    for(Zombie z:zs)if(z.row==m.row&&Math.abs(z.x-m.x)<cw*.55f)z.hp=0;
    if(m.x>getWidth()+cw)m.active=false;
   }
  }

  void spawnZombie(){
   int r=rd.nextInt(R);
   boolean boss=level==9&&wave==total-1&&spawned==target-1;
   zs.add(new Zombie(r,rd.nextInt(3),boss));
  }

  void dead(){
   Iterator<Zombie>it=zs.iterator();
   while(it.hasNext()){
    Zombie z=it.next();
    if(z.hp<=0){it.remove();coins+=z.boss?100:5;}
   }
   for(int r=0;r<R;r++)for(int q=0;q<C;q++)if(pl[r][q]!=null&&pl[r][q].hp<=0)pl[r][q]=null;
  }

  boolean has(int r){for(Zombie z:zs)if(z.row==r&&z.x>left)return true;return false;}

  Plant at(Zombie z){
   int q=(int)((z.x-left)/cw);
   return q<0||q>=C?null:pl[z.row][q];
  }

  Zombie cell(int r,int q){
   float x=left+q*cw+cw/2;
   for(Zombie z:zs)if(z.row==r&&Math.abs(z.x-x)<cw*.45f)return z;
   return null;
  }

  Zombie near(int r,int q){
   float x=left+q*cw+cw/2,d0=99999;Zombie best=null;
   for(Zombie z:zs)if(z.row==r){float d=Math.abs(z.x-x);if(d<cw*1.4f&&d<d0){d0=d;best=z;}}
   return best;
  }

  void start(int lv){
   level=lv;screen=PLAY;wave=0;spawned=0;target=waveTarget();
   total=level<=2?5:level<=4?6:level<=8?5:7;
   clear();spawn=lastSun=System.currentTimeMillis();
  }

  void clear(){
   for(int r=0;r<R;r++){for(int q=0;q<C;q++)pl[r][q]=null;mw[r]=new Mower(r);}
   zs.clear();bs.clear();ss.clear();selected=PEA;tool=0;
  }

  int waveTarget(){return 3+Math.min(5,level/2+wave);}
  long delay(){return level<=2?4300:level<=4?3800:level<=8?3200:2600;}

  boolean open(int x){
   if(x==PEA||x==SUN)return level>=1;
   if(x==GIGA)return level>=2;
   if(x==CHOMP)return level>=3;
   if(x==REP)return level>=4;
   return level>=5;
  }

  int cost(int x){
   return x==SUN?50:x==PEA?100:x==GIGA?125:x==CHOMP?150:x==REP?200:50;
  }

  void usePF(Plant a){
   long n=System.currentTimeMillis();
   if(a.type==SUN||a.type==PEA||a.type==REP){
    a.pfUntil=n+3000;a.last=n-100;
    if(a.type==PEA||a.type==REP)pfSound();
   }else if(a.type==GIGA){
    a.max=8000;a.hp=8000;
   }else if(a.type==CHOMP){
    Zombie z=near(a.row,a.col);if(z!=null)z.hp=0;
   }else{
    for(Zombie z:zs)if(z.row==a.row&&Math.abs(z.x-(left+a.col*cw+cw/2))<cw*2)z.hp-=1800;
    a.hp=0;
   }
  }

  void win(){screen=WIN;if(level<9)unlocked=Math.max(unlocked,level+1);}

  void pause(Canvas c){shade(c);txt(c,"TẠM DỪNG",getWidth()/2f,getHeight()*.34f,getHeight()*.07f,Color.WHITE);btn(c,.32f,.50f,.68f,.63f,"TIẾP TỤC");}
  void win(Canvas c){shade(c);txt(c,"CHIẾN THẮNG!",getWidth()/2f,getHeight()*.34f,getHeight()*.07f,Color.WHITE);btn(c,.30f,.55f,.70f,.68f,level<9?"MÀN TIẾP":"MENU");}
  void lose(Canvas c){shade(c);txt(c,"THUA!",getWidth()/2f,getHeight()*.34f,getHeight()*.07f,Color.WHITE);btn(c,.30f,.50f,.70f,.63f,"CHƠI LẠI");btn(c,.30f,.68f,.70f,.81f,"MENU");}

  void shade(Canvas c){p.setColor(Color.argb(210,0,0,0));c.drawRect(0,0,getWidth(),getHeight(),p);}

  void hp(Canvas c,float x,float y,float w,float v,float m){
   p.setColor(Color.RED);c.drawRect(x,y,x+w,y+5,p);
   p.setColor(Color.GREEN);c.drawRect(x,y,x+w*Math.max(0,Math.min(1,v/m)),y+5,p);
  }

  void btn(Canvas c,float a,float b,float d,float e,String s){
   p.setColor(Color.rgb(65,135,70));c.drawRoundRect(getWidth()*a,getHeight()*b,getWidth()*d,getHeight()*e,16,16,p);
   txt(c,s,getWidth()*(a+d)/2,getHeight()*(b+e)/2+getHeight()*.02f,getHeight()*.038f,Color.WHITE);
  }

  void txt(Canvas c,String s,float x,float y,float z,int color){
   t.setTextAlign(Paint.Align.CENTER);t.setTextSize(z);t.setColor(color);c.drawText(s,x,y,t);
  }

  void txtL(Canvas c,String s,float x,float y,int color){
   t.setTextAlign(Paint.Align.LEFT);t.setTextSize(getHeight()*.033f);t.setColor(color);c.drawText(s,x,y,t);
  }

  @Override public boolean onTouchEvent(MotionEvent e){
   if(e.getAction()!=MotionEvent.ACTION_UP)return true;
   float x=e.getX(),y=e.getY();

   if(screen==HOME){
    if(in(x,y,.32f,.38f,.68f,.50f))start(level);
    else if(in(x,y,.32f,.55f,.68f,.67f))screen=LEVEL;
    invalidate();return true;
   }

   if(screen==LEVEL){
    if(in(x,y,.04f,.83f,.20f,.94f)){screen=HOME;invalidate();return true;}
    for(int i=1;i<=9;i++){
     int cc=(i-1)%3,rr=(i-1)/3;
     float a=.18f+cc*.22f,b=.18f+rr*.20f;
     if(in(x,y,a,b,a+.17f,b+.13f)&&i<=unlocked){start(i);return true;}
    }
    return true;
   }

   if(screen==WIN){
    if(in(x,y,.30f,.55f,.70f,.68f)){if(level<9)start(level+1);else screen=HOME;invalidate();}
    return true;
   }

   if(screen==LOSE){
    if(in(x,y,.30f,.50f,.70f,.63f))start(level);
    else if(in(x,y,.30f,.68f,.70f,.81f))screen=HOME;
    invalidate();return true;
   }

   if(screen==PAUSE){
    if(in(x,y,.32f,.50f,.68f,.63f))screen=PLAY;
    invalidate();return true;
   }

   Iterator<Sun> si=ss.iterator();
   while(si.hasNext()){
    Sun s=si.next();
    if(Math.hypot(x-s.x,y-s.y)<Math.max(55,getHeight()*.065f)){sun+=25;si.remove();invalidate();return true;}
   }

   float cy=getHeight()*.075f;
   if(y>=cy&&y<=cy+getHeight()*.09f){
    for(int i=0;i<6;i++){
     float xx=getWidth()*.01f+i*getWidth()*.075f;
     if(x>=xx&&x<=xx+getWidth()*.065f){
      int[] a={SUN,PEA,GIGA,CHOMP,REP,MINE};
      if(open(a[i])){selected=a[i];tool=0;}
      invalidate();return true;
     }
    }

    if(x>=getWidth()*.465f&&x<=getWidth()*.53f){tool=100;invalidate();return true;}

    if(x>=getWidth()*.54f&&x<=getWidth()*.66f&&food>0){tool=101;invalidate();return true;}
   }

   if(y>=getHeight()*.075f&&y<=getHeight()*.145f&&x>=getWidth()*.76f&&x<=getWidth()*.91f){
    if(coins>=100){coins-=100;food++;}
    invalidate();return true;
   }

   if(x>=left&&x<left+C*cw&&y>=top&&y<top+R*ch){
    int q=(int)((x-left)/cw),r=(int)((y-top)/ch);

    if(tool==100){pl[r][q]=null;tool=0;invalidate();return true;}

    if(tool==101){
     if(pl[r][q]!=null&&food>0){usePF(pl[r][q]);food--;}
     tool=0;invalidate();return true;
    }

    if(pl[r][q]==null&&open(selected)&&sun>=cost(selected)){
     sun-=cost(selected);pl[r][q]=new Plant(selected,r,q);
    }
    invalidate();return true;
   }
   return true;
  }

  boolean in(float x,float y,float a,float b,float d,float e){
   return x>=getWidth()*a&&x<=getWidth()*d&&y>=getHeight()*b&&y<=getHeight()*e;
  }

  class Plant{
   int type,row,col,hp,max;
   long last,second,armed,pfUntil;
   Plant(int t,int r,int c){
    type=t;row=r;col=c;
    max=t==GIGA?4000:t==MINE?120:500;
    hp=max;last=System.currentTimeMillis();
    armed=t==MINE?last+3000:Long.MAX_VALUE;
   }
  }

  class Zombie{
   float x,y,speed,dir=-1,hp,max;int row,type,damage;boolean boss;long lastAttack,catchReady;
   Zombie(int r,int ty,boolean b){
    row=r;type=ty;boss=b;x=left+C*cw+cw;y=top+r*ch+ch*.16f;
    if(b){max=5000;speed=cw*.10f;damage=45;}
    else if(ty==1){max=1000+level*100;speed=cw*.13f;damage=32;}
    else if(ty==2){max=450+level*60;speed=cw*.32f;damage=18;}
    else{max=650+level*75;speed=cw*.20f;damage=24;}
    hp=max;catchReady=System.currentTimeMillis()+5000;
   }
  }

  class Pea{
   float x,y;int row,damage,dir;boolean big,fromZombie;
   Pea(float X,float Y,int R,int D,boolean B,boolean F,int I){
    x=X;y=Y;row=R;damage=D;big=B;fromZombie=F;dir=I;
   }
  }

  class Sun{float x,y;Sun(float X,float Y){x=X;y=Y;}}
  class Mower{int row;float x;boolean used,active;Mower(int r){row=r;x=left-cw*.5f;}}
 }
      }
