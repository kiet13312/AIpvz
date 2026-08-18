package com.alpvz.gardendefense;

import android.app.Activity;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.*;
import android.media.*;
import android.view.*;
import java.util.*;

public class MainActivity extends Activity{
 GameView g; SoundPool sp; int mineSound; MediaPlayer pfPlayer; boolean pfOn; SharedPreferences save;
 @Override public void onCreate(Bundle b){super.onCreate(b);requestWindowFeature(Window.FEATURE_NO_TITLE);getWindow().setFlags(1024,1024);getWindow().getDecorView().setSystemUiVisibility(5894);setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);save=getSharedPreferences("pvz_save",0);soundInit();g=new GameView();setContentView(g);}
 void soundInit(){AudioAttributes a=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();sp=new SoundPool.Builder().setAudioAttributes(a).setMaxStreams(8).build();mineSound=sp.load(this,R.raw.peashoot,1);}
 void mineSound(){if(!pfOn&&sp!=null)sp.play(mineSound,1,1,1,0,1);}
 void pfSound(){if(pfOn)return;pfOn=true;if(pfPlayer!=null)pfPlayer.release();pfPlayer=MediaPlayer.create(this,R.raw.peashootplantfood);if(pfPlayer==null){pfOn=false;return;}pfPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener(){public void onCompletion(MediaPlayer m){pfOn=false;m.release();pfPlayer=null;}});pfPlayer.start();}
 void saveGame(){save.edit().putInt("level",g.level).putInt("un",g.un).putInt("sun",g.sun).putInt("coins",g.coins).putInt("food",g.food).apply();}
 @Override protected void onPause(){saveGame();super.onPause();}
 @Override protected void onDestroy(){if(pfPlayer!=null)pfPlayer.release();if(sp!=null)sp.release();super.onDestroy();}
 @Override public void onBackPressed(){if(g.screen==2)g.screen=3;else if(g.screen!=0)g.screen=0;else{super.onBackPressed();return;}g.invalidate();}

 class GameView extends View{
  static final int R=5,C=9,SUN=1,PEA=2,GIGA=3,CHOMP=4,REP=5,MINE=6,HOME=0,LEVEL=1,PLAY=2,PAUSE=3,WIN=4,LOSE=5;
  final Paint p=new Paint(1),t=new Paint(1);final Random rnd=new Random();
  Plant[][] plant=new Plant[R][C];ArrayList<Zombie> zombies=new ArrayList<>();ArrayList<Pea> peas=new ArrayList<>();ArrayList<SunDrop> suns=new ArrayList<>();Mower[] mowers=new Mower[R];
  Bitmap[] img=new Bitmap[8];int screen=HOME,level=1,un=1,sel=PEA,tool=0,sun=500,coins=99999,food=3,wave=0,total=5,spawned,target,speed=1;long last=System.currentTimeMillis(),spawn,lastSun;float L,T,cw,ch;
  GameView(){super(MainActivity.this);setFocusable(true);load();for(int i=0;i<R;i++)mowers[i]=new Mower(i);level=save.getInt("level",1);un=save.getInt("un",1);sun=save.getInt("sun",500);coins=save.getInt("coins",99999);food=save.getInt("food",3);}
  void load(){String[] a={"zomplatz","sun","peashoot","giganut","chomper","repeater","min","gigapea"};for(int i=0;i<a.length;i++){int id=getResources().getIdentifier(a[i],"drawable",getPackageName());img[i]=id==0?null:BitmapFactory.decodeResource(getResources(),id);}}
  @Override protected void onSizeChanged(int w,int h,int ow,int oh){L=w*.18f;T=h*.19f;cw=w*.073f;ch=h*.125f;}
  @Override protected void onDraw(Canvas c){if(screen==HOME){home(c);return;}if(screen==LEVEL){levels(c);return;}drawGame(c);if(screen==PAUSE)shadeText(c,"TẠM DỪNG");if(screen==WIN)shadeText(c,"CHIẾN THẮNG");if(screen==LOSE)shadeText(c,"THUA");if(screen==PLAY){update();postInvalidateDelayed(30);}}
  void home(Canvas c){c.drawColor(Color.rgb(25,55,30));tx(c,"GARDEN DEFENSE",getWidth()/2f,getHeight()*.22f,.07f,Color.WHITE);btn(c,.28f,.36f,.72f,.48f,"CHƠI");btn(c,.28f,.52f,.72f,.64f,"CHỌN MÀN");}
  void levels(Canvas c){c.drawColor(Color.rgb(20,50,25));tx(c,"CHỌN MÀN",getWidth()/2f,getHeight()*.1f,.06f,Color.WHITE);for(int i=1;i<=9;i++){int cc=(i-1)%3,rr=(i-1)/3;float x=.18f+cc*.22f,y=.18f+rr*.20f;p.setColor(i<=un?Color.rgb(65,145,70):Color.DKGRAY);c.drawRoundRect(getWidth()*x,getHeight()*y,getWidth()*(x+.17f),getHeight()*(y+.13f),16,16,p);tx(c,i<=un?"MÀN "+i:"KHÓA",getWidth()*(x+.085f),getHeight()*(y+.08f),.035f,Color.WHITE);}btn(c,.04f,.84f,.20f,.94f,"QUAY LẠI");}
  void drawGame(Canvas c){c.drawColor(Color.rgb(92,155,70));p.setColor(Color.rgb(38,78,40));c.drawRect(0,0,getWidth(),T,p);tl(c,"☀ "+sun,10,30,Color.YELLOW);tl(c,"M"+level,getWidth()*.30f,30,Color.WHITE);tl(c,"S"+wave+"/"+total,getWidth()*.42f,30,Color.WHITE);tl(c,"$"+coins,getWidth()*.60f,30,Color.YELLOW);tl(c,"PF"+food,getWidth()*.78f,30,Color.WHITE);btn(c,.86f,.075f,.99f,.145f,speed==2?"×2":"×1");cards(c);board(c);drawPlants(c);drawZombies(c);drawPeas(c);drawSuns(c);drawMowers(c);}
  void cards(Canvas c){int[] a={SUN,PEA,GIGA,CHOMP,REP,MINE};for(int i=0;i<6;i++){float x=getWidth()*.01f+i*getWidth()*.075f,y=getHeight()*.075f,w=getWidth()*.065f,h=getHeight()*.09f;p.setColor(sel==a[i]&&tool==0?Color.YELLOW:Color.rgb(45,80,45));c.drawRoundRect(x,y,x+w,y+h,8,8,p);if(open(a[i]))plant(c,a[i],x+w/2,y+h/2,Math.min(w,h)*.6f);} }
  void board(Canvas c){for(int r=0;r<R;r++)for(int q=0;q<C;q++){p.setColor((r+q)%2==0?Color.rgb(103,166,78):Color.rgb(91,153,67));c.drawRect(L+q*cw,T+r*ch,L+(q+1)*cw,T+(r+1)*ch,p);}p.setColor(Color.rgb(130,92,52));c.drawRect(0,T,L,T+R*ch,p);}
  void drawPlants(Canvas c){for(int r=0;r<R;r++)for(int q=0;q<C;q++){Plant a=plant[r][q];if(a==null)continue;float x=L+q*cw+cw/2,y=T+r*ch+ch/2;plant(c,a.type,x,y,Math.min(cw,ch)*.7f);hp(c,x-cw*.3f,y+ch*.33f,cw*.6f,a.hp,a.max);}}
  void plant(Canvas c,int type,float x,float y,float s){Bitmap b=img[type];if(b!=null){c.drawBitmap(b,null,new RectF(x-s*.48f,y-s*.48f,x+s*.48f,y+s*.48f),p);return;}p.setColor(type==SUN?Color.YELLOW:type==GIGA?Color.rgb(150,100,55):type==CHOMP?Color.rgb(145,80,185):Color.rgb(55,175,70));c.drawCircle(x,y,s*.38f,p);}
  void drawZombies(Canvas c){for(Zombie z:zombies){float w=z.boss?cw*1.1f:cw*.58f,h=z.boss?ch*1.1f:ch*.75f;if(img[0]!=null)c.drawBitmap(img[0],null,new RectF(z.x-w/2,z.y-h*.1f,z.x+w/2,z.y+h*.9f),p);hp(c,z.x-w/2,z.y-h*.2f,w,z.hp,z.max);}}
  void drawPeas(Canvas c){for(Pea q:peas){float s=q.big?20:9;Bitmap b=img[7];if(b!=null)c.drawBitmap(b,null,new RectF(q.x-s,q.y-s,q.x+s,q.y+s),p);else{p.setColor(Color.GREEN);c.drawCircle(q.x,q.y,s*.7f,p);}}}
  void drawSuns(Canvas c){for(SunDrop s:suns){p.setColor(Color.YELLOW);c.drawCircle(s.x,s.y,15,p);}}
  void drawMowers(Canvas c){for(Mower m:mowers){float y=T+m.row*ch+ch*.7f;p.setColor(m.used?Color.DKGRAY:Color.rgb(180,70,40));c.drawRect(m.x-cw*.3f,y-ch*.2f,m.x+cw*.3f,y,p);}}

  void update(){long n=System.currentTimeMillis();float dt=Math.min(.08f,(n-last)/1000f)*speed;last=n;if(n-lastSun>8000){lastSun=n;suns.add(new SunDrop(L+rnd.nextInt(C)*cw+cw/2,T*.8f));}updatePlants(n);updatePeas(dt);updateZombies(n,dt);updateMowers(dt);dead();if(spawned<target&&n-spawn>=delay()){spawnZombie();spawned++;spawn=n;}if(spawned>=target&&zombies.isEmpty()){wave++;if(wave>=total){winLevel();}else{spawned=0;target=waveTarget();spawn=n;}}}
  void updatePlants(long n){for(int r=0;r<R;r++)for(int q=0;q<C;q++){Plant a=plant[r][q];if(a==null)continue;boolean pf0=a.pfUntil>n;long cd=pf0?100:(a.type==SUN?7000:a.type==REP?1500:1500);if(a.type==SUN&&n-a.last>=cd){suns.add(new SunDrop(L+q*cw+cw/2,T+r*ch+ch*.25f));a.last=n;}if(a.type==PEA&&n-a.last>=cd&&has(r)){fire(r,q,30,false);a.last=n;}if(a.type==REP&&n-a.last>=cd&&has(r)){fire(r,q,30,false);a.last=n;a.second=n+200;}if(a.type==REP&&a.second>0&&n>=a.second){fire(r,q,30,false);a.second=0;}if(a.type==CHOMP&&n-a.last>=3500){Zombie zz=near(r,q);if(zz!=null){zz.hp=0;a.last=n;}}if(a.type==MINE&&n>=a.armed){Zombie zz=cell(r,q);if(zz!=null){mineSound();zz.hp=0;a.hp=0;}}}}
  void fire(int r,int q,int dmg,boolean big){peas.add(new Pea(L+q*cw+cw*.55f,T+r*ch+ch*.5f,r,dmg,big,false,1));}
  void updatePeas(float dt){ArrayList<Pea> add=new ArrayList<>();Iterator<Pea>it=peas.iterator();long n=System.currentTimeMillis();while(it.hasNext()){Pea q=it.next();q.x+=q.dir*cw*9f*dt;if(q.fromZombie){Plant hit=null;for(int j=0;j<C;j++){Plant a=plant[q.row][j];if(a!=null&&Math.abs(L+j*cw+cw/2-q.x)<cw*.3f){hit=a;break;}}if(hit!=null){hit.hp-=q.damage;it.remove();}else if(q.x<L-cw)it.remove();continue;}Zombie boss=null;for(Zombie zz:zombies)if(zz.boss&&zz.row==q.row&&Math.abs(zz.x-q.x)<cw*.3f&&n>=zz.catchReady){boss=zz;break;}if(boss!=null){boss.catchReady=n+5000;it.remove();add.add(new Pea(boss.x-cw*.3f,boss.y+ch*.3f,boss.row,500,false,true,-1));continue;}Zombie hit=null;for(Zombie zz:zombies)if(zz.row==q.row&&Math.abs(zz.x-q.x)<cw*.28f){hit=zz;break;}if(hit!=null){hit.hp-=q.damage;it.remove();}else if(q.x>getWidth()+30)it.remove();}peas.addAll(add);}
  void updateZombies(long n,float dt){for(Zombie q:zombies){if(q.hp<=0)continue;if(q.boss){q.x+=q.dir*cw*1.1f*dt;if(q.x<L+cw*2){q.x=L+cw*2;q.dir=1;}if(q.x>getWidth()-cw){q.x=getWidth()-cw;q.dir=-1;}}else{Plant a=at(q);if(a!=null){if(n-q.lastAttack>=850){a.hp-=q.damage;q.lastAttack=n;}}else q.x-=q.speed*dt;if(q.x<L-cw*.4f){Mower mm=mowers[q.row];if(!mm.used){mm.used=true;mm.active=true;mm.x=L-cw*.5f;}else{screen=LOSE;return;}}}}}
  void updateMowers(float dt){for(Mower q:mowers)if(q.active){q.x+=cw*18*dt;for(Zombie zz:zombies)if(zz.row==q.row&&Math.abs(zz.x-q.x)<cw*.55f)zz.hp=0;if(q.x>getWidth()+cw)q.active=false;}}
  void spawnZombie(){int r=rnd.nextInt(R);boolean boss=level==9&&wave==total-1&&spawned==target-1;zombies.add(new Zombie(r,rnd.nextInt(3),boss));}
  void dead(){Iterator<Zombie>it=zombies.iterator();while(it.hasNext()){Zombie q=it.next();if(q.hp<=0){it.remove();coins+=q.boss?100:5;}}for(int r=0;r<R;r++)for(int q=0;q<C;q++)if(plant[r][q]!=null&&plant[r][q].hp<=0)plant[r][q]=null;}
  boolean has(int r){for(Zombie q:zombies)if(q.row==r&&q.x>L)return true;return false;}
  Plant at(Zombie q){int j=(int)((q.x-L)/cw);return j<0||j>=C?null:plant[q.row][j];}
  Zombie cell(int r,int q){float x=L+q*cw+cw/2;for(Zombie z:zombies)if(z.row==r&&Math.abs(z.x-x)<cw*.45f)return z;return null;}
  Zombie near(int r,int q){float x=L+q*cw+cw/2,d0=99999;Zombie best=null;for(Zombie z:zombies)if(z.row==r){float d=Math.abs(z.x-x);if(d<cw*1.4f&&d<d0){d0=d;best=z;}}return best;}

  void start(int lv){level=lv;screen=PLAY;wave=0;spawned=0;total=lv<=2?5:lv<=4?6:lv<=8?5:7;target=waveTarget();clear();spawn=lastSun=last=System.currentTimeMillis();}
  void clear(){for(int r=0;r<R;r++){for(int q=0;q<C;q++)plant[r][q]=null;mowers[r]=new Mower(r);}zombies.clear();peas.clear();suns.clear();tool=0;sel=PEA;}
  int waveTarget(){return 3+Math.min(5,level/2+wave);}
  long delay(){return level<=2?4300:level<=4?3800:level<=8?3300:3000;}
  boolean open(int x){if(x==PEA||x==SUN)return level>=1;if(x==GIGA)return level>=2;if(x==MINE)return level>=3;if(x==CHOMP)return level>=4;if(x==REP)return level>=5;return false;}
  int cost(int x){return x==SUN?50:x==PEA?100:x==GIGA?125:x==CHOMP?150:x==REP?200:50;}
  int mowerReward(){int n=0;for(Mower x:mowers)if(!x.used)n+=50;return n;}
  void winLevel(){screen=WIN;coins+=mowerReward();if(level<9)un=Math.max(un,level+1);saveGame();}
  void usePF(Plant a){long n=System.currentTimeMillis();if(a.type==SUN||a.type==PEA||a.type==REP){a.pfUntil=n+3000;a.last=n-100;pfSound();}else if(a.type==GIGA){a.max=8000;a.hp=8000;}else if(a.type==CHOMP){Zombie q=near(a.row,a.col);if(q!=null)q.hp=0;}else{for(Zombie q:zombies)if(q.row==a.row&&Math.abs(q.x-(L+a.col*cw+cw/2))<cw*2)q.hp-=1800;a.hp=0;}}
  void save(){saveGame();}
  void pause(Canvas c){overlay(c,"TẠM DỪNG");}
  void win(Canvas c){overlay(c,"CHIẾN THẮNG");}
  void lose(Canvas c){overlay(c,"THUA");}
  void overlay(Canvas c,String s){p.setColor(Color.argb(210,0,0,0));c.drawRect(0,0,getWidth(),getHeight(),p);tx(c,s,getWidth()/2f,getHeight()*.35f,.07f,Color.WHITE);}
  void btn(Canvas c,float a,float b,float d,float e,String s){p.setColor(Color.rgb(65,135,70));c.drawRoundRect(getWidth()*a,getHeight()*b,getWidth()*d,getHeight()*e,14,14,p);tx(c,s,getWidth()*(a+d)/2,getHeight()*(b+e)/2+15,.035f,Color.WHITE);}
  void tx(Canvas c,String s,float x,float y,float z,int col){t.setTextAlign(Paint.Align.CENTER);t.setTextSize(getHeight()*z);t.setColor(col);c.drawText(s,x,y,t);}
  void tl(Canvas c,String s,float x,float y,int col){t.setTextAlign(Paint.Align.LEFT);t.setTextSize(getHeight()*.03f);t.setColor(col);c.drawText(s,x,y,t);}

  @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()!=MotionEvent.ACTION_UP)return true;float x=e.getX(),y=e.getY();
   if(screen==HOME){if(in(x,y,.28f,.36f,.72f,.48f))start(level);else if(in(x,y,.28f,.52f,.72f,.64f))screen=LEVEL;invalidate();return true;}
   if(screen==LEVEL){if(in(x,y,.04f,.84f,.20f,.94f)){screen=HOME;invalidate();return true;}for(int i=1;i<=9;i++){int cc=(i-1)%3,rr=(i-1)/3;float a=.18f+cc*.22f,b=.18f+rr*.20f;if(in(x,y,a,b,a+.17f,b+.13f)&&i<=un){start(i);return true;}}return true;}
   if(screen==WIN){if(level<9)start(level+1);else screen=HOME;invalidate();return true;}
   if(screen==LOSE){if(in(x,y,.30f,.50f,.70f,.63f))start(level);else screen=HOME;invalidate();return true;}
   if(screen==PAUSE){screen=PLAY;invalidate();return true;}
   if(in(x,y,.86f,.075f,.99f,.145f)){speed=speed==1?2:1;invalidate();return true;}
   Iterator<SunDrop>si=suns.iterator();while(si.hasNext()){SunDrop s=si.next();if(Math.hypot(x-s.x,y-s.y)<Math.max(55,getHeight()*.06f)){sun+=25;si.remove();saveGame();invalidate();return true;}}
   float cy=getHeight()*.075f;if(y>=cy&&y<=cy+getHeight()*.09f){int[] a={SUN,PEA,GIGA,CHOMP,REP,MINE};for(int i=0;i<6;i++){float xx=getWidth()*.01f+i*getWidth()*.075f;if(x>=xx&&x<=xx+getWidth()*.065f){if(open(a[i])){sel=a[i];tool=0;}invalidate();return true;}}if(x>=getWidth()*.465f&&x<=getWidth()*.525f){tool=100;return true;}if(x>=getWidth()*.54f&&x<=getWidth()*.63f&&food>0){tool=101;return true;}}
   if(y>=getHeight()*.075f&&y<=getHeight()*.145f&&x>=getWidth()*.76f&&x<=getWidth()*.91f){if(coins>=100){coins-=100;food++;}saveGame();invalidate();return true;}
   if(x>=L&&x<L+C*cw&&y>=T&&y<T+R*ch){int q=(int)((x-L)/cw),r=(int)((y-T)/ch);if(tool==100){plant[r][q]=null;tool=0;}else if(tool==101){if(plant[r][q]!=null&&food>0){usePF(plant[r][q]);food--;}tool=0;}else if(plant[r][q]==null&&open(sel)&&sun>=cost(sel)){sun-=cost(sel);plant[r][q]=new Plant(sel,r,q);}saveGame();invalidate();return true;}
   return true;
  }
  boolean in(float x,float y,float a,float b,float d,float e){return x>=getWidth()*a&&x<=getWidth()*d&&y>=getHeight()*b&&y<=getHeight()*e;}
  class Plant{int type,row,col,hp,max;long last,second,armed,pfUntil;Plant(int t,int r,int q){type=t;row=r;col=q;max=t==GIGA?4000:t==MINE?120:500;hp=max;last=System.currentTimeMillis();armed=t==MINE?last+3000:Long.MAX_VALUE;}}
  class Zombie{float x,y,speed,dir=-1,hp,max;int row,type,damage;boolean boss;long lastAttack,catchReady;Zombie(int r,int ty,boolean bo){row=r;type=ty;boss=bo;x=L+C*cw+cw;y=T+r*ch+ch*.16f;if(bo){max=5000;speed=cw*.10f;damage=45;}else if(ty==1){max=1000+level*100;speed=cw*.13f;damage=32;}else if(ty==2){max=450+level*60;speed=cw*.32f;damage=18;}else{max=650+level*75;speed=cw*.20f;damage=24;}hp=max;catchReady=System.currentTimeMillis()+5000;}}
  class Pea{float x,y;int row,damage,dir;boolean big,fromZombie;Pea(float X,float Y,int r,int d,boolean bg,boolean f,int di){x=X;y=Y;row=r;damage=d;big=bg;fromZombie=f;dir=di;}}
  class SunDrop{float x,y;SunDrop(float X,float Y){x=X;y=Y;}}
  class Mower{int row;float x;boolean used,active;Mower(int r){row=r;x=L-cw*.5f;}}
 }
  }
  
