package com.alpvz.gardendefense;
import android.app.Activity;
import android.os.Bundle;
import android.content.pm.ActivityInfo;
import android.graphics.*;
import android.media.*;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import java.util.*;
public class MainActivity extends Activity {
private GameView game;
private FrameLayout root;private VideoView winVideo;private Button continueBtn;private Handler h=new Handler(Looper.getMainLooper());private SoundPool sp;private int mineSound;private MediaPlayer pfPlayer;private boolean pfPlaying=false;
@Override public void onCreate(Bundle b) {
super.onCreate(b);
requestWindowFeature(Window.FEATURE_NO_TITLE);
getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
getWindow().getDecorView().setSystemUiVisibility(5894);
setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
initSound();game=new GameView();root=new FrameLayout(this);root.addView(game,new FrameLayout.LayoutParams(-1,-1));setContentView(root);
}
void initSound(){AudioAttributes a=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();sp=new SoundPool.Builder().setAudioAttributes(a).setMaxStreams(8).build();mineSound=sp.load(this,R.raw.peashoot,1);}
void playMineSound(){if(pfPlaying||sp==null)return;sp.play(mineSound,1,1,1,0,1);}
void playPFSound(){if(pfPlaying)return;pfPlaying=true;if(pfPlayer!=null)pfPlayer.release();pfPlayer=MediaPlayer.create(this,R.raw.peashootplantfood);if(pfPlayer==null){pfPlaying=false;return;}pfPlayer.setOnCompletionListener(m->{pfPlaying=false;m.release();pfPlayer=null;});pfPlayer.start();}
void showWinVideo(){if(root==null||winVideo!=null)return;winVideo=new VideoView(this);winVideo.setVideoURI(Uri.parse("android.resource://"+getPackageName()+"/"+R.raw.win));winVideo.setBackgroundColor(Color.BLACK);winVideo.setAlpha(0f);root.addView(winVideo,new FrameLayout.LayoutParams(-1,-1));continueBtn=new Button(this);continueBtn.setText("CHƠI TIẾP");continueBtn.setTextSize(16);continueBtn.setVisibility(View.GONE);FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(250,100,Gravity.RIGHT|Gravity.BOTTOM);bp.setMargins(0,0,25,25);root.addView(continueBtn,bp);winVideo.setOnPreparedListener(m->{winVideo.start();winVideo.animate().alpha(1f).setDuration(700).start();h.postDelayed(()->{if(continueBtn!=null)continueBtn.setVisibility(View.VISIBLE);},3000);});continueBtn.setOnClickListener(v->{closeWinVideo();if(game.level<game.LEVELS)game.startLevel(game.level+1);else{game.screen=game.HOME;game.invalidate();}});}
void closeWinVideo(){if(h!=null)h.removeCallbacksAndMessages(null);if(winVideo!=null){winVideo.stopPlayback();root.removeView(winVideo);winVideo=null;}if(continueBtn!=null){root.removeView(continueBtn);continueBtn=null;}}
@Override protected void onDestroy(){closeWinVideo();if(pfPlayer!=null)pfPlayer.release();if(sp!=null)sp.release();super.onDestroy();}
@Override public void onBackPressed() {
if (game == null) { super.onBackPressed(); return; }
if (game.screen == GameView.PLAY || game.screen == GameView.PAUSE) game.screen = GameView.PAUSE;
else if (game.screen != GameView.HOME) game.screen = GameView.HOME;
else super.onBackPressed();
game.invalidate();
}
private class GameView extends View {
static final int ROWS=5, COLS=9, LEVELS=9;
static final int SUNFLOWER=1, PEASHOOTER=2, GIGANUT=3, CHOMPER=4, REPEATER=5, MINE=6;
static final int NONE=0, SHOVEL=100, PF_TOOL=101;
static final int HOME=0, LEVELS_SCREEN=1, PLAY=2, PAUSE=3, WIN=4, LOSE=5;
final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); final Paint textPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
final Random rnd=new Random();
final Plant[][] plants=new Plant[ROWS][COLS];
final ArrayList<Zombie> zombies=new ArrayList<>();
final ArrayList<Pea> peas=new ArrayList<>();
final ArrayList<SunDrop> sunDrops=new ArrayList<>();
final Mower[] mowers=new Mower[ROWS];
Bitmap sunImg, peaImg, gigaImg, chompImg, repeatImg, mineImg, zombieImg, bulletImg;
int screen=HOME, level=1, unlocked=1, selected=PEASHOOTER, tool=NONE;
int sun=500, coins=99999, plantFood=3, wave=0, totalWaves=5, spawned=0, target=5;
long lastFrame=System.currentTimeMillis(), lastSpawn=0, lastSun=0;
float left, top, cw, ch;
boolean levelStarted=false; String pfMsg=""; long pfMsgUntil=0;
GameView(){ super(MainActivity.this); setFocusable(true); loadImages(); for(int r=0;r<ROWS;r++)mowers[r]=new Mower(r); }
void loadImages(){
sunImg=load("sun"); peaImg=load("peashoot"); gigaImg=load("giganut"); chompImg=load("chomper");
repeatImg=load("repeater"); mineImg=load("min"); zombieImg=load("zomplatz"); bulletImg=load("gigapea");
}
Bitmap load(String n){ int id=getResources().getIdentifier(n,"drawable",getPackageName()); return id==0?null:BitmapFactory.decodeResource(getResources(),id); }
@Override protected void onSizeChanged(int w,int h,int ow,int oh){ left=w*0.18f; top=h*0.19f; cw=w*0.073f; ch=h*0.125f; }
@Override protected void onDraw(Canvas c){
if(screen==HOME){drawHome(c);return;} if(screen==LEVELS_SCREEN){drawLevels(c);return;}
drawGame(c); if(screen==PAUSE)drawPause(c); else if(screen==WIN)drawWin(c); else if(screen==LOSE)drawLose(c);
if(screen==PLAY) { update(); postInvalidateDelayed(30); }
}
void drawHome(Canvas c){
c.drawColor(Color.rgb(25,55,30)); title(c,"GARDEN DEFENSE",0.20f,32);
button(c,0.32f,0.38f,0.68f,0.50f,"CHƠI"); button(c,0.32f,0.55f,0.68f,0.67f,"CHỌN MÀN");
small(c,"5×9 • 9 MÀN • MÁY CẮT CỎ",0.82f,Color.LTGRAY);
}
void drawLevels(Canvas c){
c.drawColor(Color.rgb(20,50,25)); title(c,"CHỌN MÀN",0.10f,25);
for(int i=1;i<=LEVELS;i++){int cc=(i-1)%3,rr=(i-1)/3;float x=.18f+cc*.22f,y=.18f+rr*.20f;
p.setColor(i<=unlocked?Color.rgb(65,145,70):Color.DKGRAY); c.drawRoundRect(getWidth()*x,getHeight()*y,getWidth()*(x+.17f),getHeight()*(y+.13f),18,18,p);
center(c,i<=unlocked?"MÀN "+i:"KHÓA",getWidth()*(x+.085f),getHeight()*(y+.083f),getHeight()*.038f,Color.WHITE);
}
button(c,.04f,.83f,.20f,.94f,"QUAY LẠI");
}
void drawGame(Canvas c){
c.drawColor(Color.rgb(92,155,70));
p.setColor(Color.rgb(38,78,40)); c.drawRect(0,0,getWidth(),top,p);
smallAt(c,"☀ "+sun,12,getHeight()*.045f,Color.YELLOW); smallAt(c,"Màn "+level,getWidth()*.34f,getHeight()*.045f,Color.WHITE);
smallAt(c,"Sóng "+wave+"/"+totalWaves,getWidth()*.46f,getHeight()*.045f,Color.WHITE); smallAt(c,"Xu "+coins,getWidth()*.70f,getHeight()*.045f,Color.YELLOW);
smallAt(c,"PF "+plantFood,getWidth()*.87f,getHeight()*.045f,Color.WHITE);button(c,.76f,.075f,.91f,.145f,"MUA PF 100"); if(System.currentTimeMillis()<pfMsgUntil)center(c,pfMsg,getWidth()*.72f,getHeight()*.16f,getHeight()*.035f,Color.YELLOW);
drawCards(c); drawBoard(c); drawMowers(c); drawPlants(c); drawZombies(c); drawPeas(c); drawSunDrops(c);
}
void drawCards(Canvas c){int[] ids={SUNFLOWER,PEASHOOTER,GIGANUT,CHOMPER,REPEATER,MINE}; for(int i=0;i<ids.length;i++){float x=getWidth()*.01f+i*getWidth()*.075f,y=getHeight()*.075f,w=getWidth()*.065f,h=getHeight()*.09f;p.setColor(selected==ids[i]&&tool==NONE?Color.YELLOW:Color.rgb(45,80,45));c.drawRoundRect(x,y,x+w,y+h,10,10,p);if(plantUnlocked(ids[i]))drawPlant(c,ids[i],x+w/2,y+h/2,Math.min(w,h)*.62f);else center(c,"🔒",x+w/2,y+h*.63f,h*.35f,Color.GRAY);}
float sx=getWidth()*.465f,sy=getHeight()*.075f;p.setColor(tool==SHOVEL?Color.YELLOW:Color.DKGRAY);c.drawRoundRect(sx,sy,sx+getWidth()*.065f,sy+getHeight()*.09f,10,10,p);center(c,"X",sx+getWidth()*.0325f,sy+getHeight()*.06f,getHeight()*.035f,Color.WHITE);
float fx=getWidth()*.54f;p.setColor(tool==PF_TOOL?Color.YELLOW:Color.rgb(90,45,125));c.drawRoundRect(fx,sy,fx+getWidth()*.09f,sy+getHeight()*.09f,10,10,p);center(c,"PF "+plantFood,fx+getWidth()*.045f,sy+getHeight()*.06f,getHeight()*.025f,Color.WHITE);
}
void drawBoard(Canvas c){for(int r=0;r<ROWS;r++)for(int col=0;col<COLS;col++){float x=left+col*cw,y=top+r*ch;p.setColor((r+col)%2==0?Color.rgb(103,166,78):Color.rgb(91,153,67));c.drawRect(x,y,x+cw,y+ch,p);}p.setColor(Color.rgb(130,92,52));c.drawRect(0,top,left,top+ROWS*ch,p);}
void drawPlants(Canvas c){for(int r=0;r<ROWS;r++)for(int col=0;col<COLS;col++){Plant a=plants[r][col];if(a==null)continue;float x=left+col*cw+cw/2,y=top+r*ch+ch/2;drawPlant(c,a.type,x,y,Math.min(cw,ch)*.72f);hp(c,x-cw*.30f,y+ch*.33f,cw*.60f,5,a.hp,a.maxHp);}}
void drawPlant(Canvas c,int type,float x,float y,float size){Bitmap b=img(type);if(b!=null){c.drawBitmap(b,null,new RectF(x-size*.48f,y-size*.48f,x+size*.48f,y+size*.48f),p);return;}p.setColor(type==SUNFLOWER?Color.YELLOW:type==GIGANUT?Color.rgb(155,105,55):type==CHOMPER?Color.rgb(145,80,185):Color.rgb(55,175,70));c.drawCircle(x,y,size*.38f,p);}
Bitmap img(int type){if(type==SUNFLOWER)return sunImg;if(type==PEASHOOTER)return peaImg;if(type==GIGANUT)return gigaImg;if(type==CHOMPER)return chompImg;if(type==REPEATER)return repeatImg;return mineImg;}
void drawZombies(Canvas c){for(Zombie z:zombies){float w=z.boss?cw*1.18f:cw*.60f,h=z.boss?ch*1.15f:ch*.78f;Bitmap b=zombieImg;if(b!=null)c.drawBitmap(b,null,new RectF(z.x-w/2,z.y-h*.1f,z.x+w/2,z.y+h*.9f),p);else{p.setColor(z.boss?Color.rgb(95,40,110):Color.GRAY);c.drawOval(new RectF(z.x-w/2,z.y-h*.1f,z.x+w/2,z.y+h*.9f),p);}hp(c,z.x-w/2,z.y-h*.2f,w,5,z.hp,z.maxHp);}}
void drawPeas(Canvas c){for(Pea q:peas){float s=q.big?22:10;if(bulletImg!=null)c.drawBitmap(bulletImg,null,new RectF(q.x-s,q.y-s,q.x+s,q.y+s),p);else{p.setColor(q.big?Color.rgb(80,255,80):Color.GREEN);c.drawCircle(q.x,q.y,q.big?15:7,p);}}}
void drawSunDrops(Canvas c){for(SunDrop s:sunDrops){p.setColor(Color.YELLOW);c.drawCircle(s.x,s.y,16,p);center(c,"+",s.x,s.y+5,15,Color.rgb(120,80,0));}}
void drawMowers(Canvas c){for(Mower m:mowers){float y=top+m.row*ch+ch*.70f;p.setColor(m.used?Color.DKGRAY:Color.rgb(180,70,40));c.drawRoundRect(m.x-cw*.30f,y-ch*.23f,m.x+cw*.30f,y+ch*.02f,7,7,p);p.setColor(Color.BLACK);c.drawCircle(m.x-cw*.18f,y+5,7,p);c.drawCircle(m.x+cw*.18f,y+5,7,p);}}
void update(){long now=System.currentTimeMillis();float dt=Math.min(.05f,(now-lastFrame)/1000f);lastFrame=now;if(now-lastSun>8000){lastSun=now;sunDrops.add(new SunDrop(left+rnd.nextInt(COLS)*cw+cw/2,top*.80f));}updatePlants(now);updatePeas(dt);updateZombies(now,dt);updateMowers(dt);removeDead();if(spawned<target&&now-lastSpawn>=spawnDelay()){spawnZombie();spawned++;lastSpawn=now;}if(spawned>=target&&zombies.isEmpty()){wave++;if(wave>=totalWaves)winLevel();else{spawned=0;target=waveTarget();lastSpawn=now;}}}
void updatePlants(long now){for(int r=0;r<ROWS;r++)for(int col=0;col<COLS;col++){Plant a=plants[r][col];if(a==null)continue;boolean pf=a.pfUntil>now;long sunCd=pf?100:7000;long peaCd=pf?100:1500;long repCd=pf?100:1800;if(a.type==SUNFLOWER&&now-a.lastAction>=sunCd){sunDrops.add(new SunDrop(left+col*cw+cw/2,top+r*ch+ch*.25f));a.lastAction=now;}if(a.type==PEASHOOTER&&now-a.lastAction>=peaCd&&rowHasZombie(r)){fire(r,col,30,false);a.lastAction=now;}if(a.type==REPEATER&&now-a.lastAction>=repCd&&rowHasZombie(r)){fire(r,col,28,false);a.secondShotAt=now+200;a.lastAction=now;}if(a.type==REPEATER&&a.secondShotAt>0&&now>=a.secondShotAt){fire(r,col,28,false);a.secondShotAt=0;}if(a.type==CHOMPER&&now-a.lastAction>=3500){Zombie z=near(r,col);if(z!=null){z.hp=0;a.lastAction=now;}}if(a.type==MINE&&now>=a.armedAt){Zombie z=cellZombie(r,col);if(z!=null){z.hp=0;a.hp=0;}}}}
void fire(int row,int col,int damage,boolean big){peas.add(new Pea(left+col*cw+cw*.55f,top+row*ch+ch*.50f,row,damage,big,false,1));}
void updatePeas(float dt){ArrayList<Pea> returned=new ArrayList<>();Iterator<Pea>it=peas.iterator();while(it.hasNext()){Pea q=it.next();q.x+=q.dir*cw*9f*dt;if(q.fromZombie){Plant hitPlant=null;for(int col=0;col<COLS;col++){Plant a=plants[q.row][col];if(a!=null&&Math.abs((left+col*cw+cw/2)-q.x)<cw*.30f){hitPlant=a;break;}}if(hitPlant!=null){hitPlant.hp-=q.damage;it.remove();}else if(q.x<left-cw)it.remove();continue;}Zombie caught=null;long now=System.currentTimeMillis();for(Zombie z:zombies)if(z.boss&&z.row==q.row&&Math.abs(z.x-q.x)<(q.big?cw*.42f:cw*.28f)&&now>=z.catchReadyAt){caught=z;break;}if(caught!=null){caught.catchReadyAt=now+5000;it.remove();returned.add(new Pea(caught.x-cw*.35f,caught.y+ch*.35f,caught.row,500,false,true,-1));}else{Zombie hit=null;for(Zombie z:zombies)if(z.row==q.row&&Math.abs(z.x-q.x)<(q.big?cw*.42f:cw*.28f)){hit=z;break;}if(hit!=null){hit.hp-=q.damage;it.remove();}else if(q.x>getWidth()+30)it.remove();}}peas.addAll(returned);}void updateZombies(long now,float dt){for(Zombie z:zombies){if(z.hp<=0)continue;if(z.boss){z.x+=z.dir*cw*1.0f*dt;float min=left+cw*2,max=getWidth()-cw;if(z.x<min){z.x=min;z.dir=1;}if(z.x>max){z.x=max;z.dir=-1;}}else{Plant targetPlant=plantAt(z);if(targetPlant!=null){if(now-z.lastAttack>=800){targetPlant.hp-=z.damage;z.lastAttack=now;}}else z.x-=Math.min(z.speed, getWidth()*.035f)*dt;}if(!z.boss&&z.x<left-cw*.4f){Mower m=mowers[z.row];if(!m.used){m.used=true;m.active=true;m.x=left-cw*.5f;}else{screen=LOSE;return;}}}}
void updateMowers(float dt){for(Mower m:mowers)if(m.active){m.x+=cw*18f*dt;for(Zombie z:zombies)if(z.row==m.row&&Math.abs(z.x-m.x)<cw*.55f)z.hp=0;if(m.x>getWidth()+cw)m.active=false;}}
void spawnZombie(){int row=rnd.nextInt(ROWS);boolean boss=level==9&&wave==totalWaves-1&&spawned==target-1;zombies.add(new Zombie(row,rnd.nextInt(3),boss));}
void removeDead(){Iterator<Zombie>it=zombies.iterator();while(it.hasNext()){Zombie z=it.next();if(z.hp<=0){it.remove();coins+=z.boss?100:5;}}for(int r=0;r<ROWS;r++)for(int c=0;c<COLS;c++)if(plants[r][c]!=null&&plants[r][c].hp<=0)plants[r][c]=null;}
boolean rowHasZombie(int r){for(Zombie z:zombies)if(z.row==r&&z.x>left)return true;return false;}
Plant plantAt(Zombie z){int col=(int)((z.x-left)/cw);return col<0||col>=COLS?null:plants[z.row][col];}
Zombie cellZombie(int r,int col){float x=left+col*cw+cw/2;for(Zombie z:zombies)if(z.row==r&&Math.abs(z.x-x)<cw*.45f)return z;return null;}
Zombie near(int r,int col){float x=left+col*cw+cw/2;Zombie best=null;float bd=Float.MAX_VALUE;for(Zombie z:zombies)if(z.row==r){float d=Math.abs(z.x-x);if(d<cw*1.4f&&d<bd){best=z;bd=d;}}return best;}
void startLevel(int lv){level=Math.max(1,Math.min(LEVELS,lv));screen=PLAY;clear();wave=0;spawned=0;target=waveTarget();totalWaves=wavesForLevel();lastSpawn=System.currentTimeMillis();lastSun=lastSpawn;}
void clear(){for(int r=0;r<ROWS;r++){for(int c=0;c<COLS;c++)plants[r][c]=null;mowers[r]=new Mower(r);}zombies.clear();peas.clear();sunDrops.clear();selected=PEASHOOTER;tool=NONE;}
int wavesForLevel(){return level<=2?5:level<=4?6:level<=8?5:7;}
int waveTarget(){return 3+Math.min(5,level/2+wave);}
long spawnDelay(){return level<=2?4300:level<=4?3800:level<=8?3200:2600;}
boolean levelOpen(int n){return n<=unlocked;}
boolean plantUnlocked(int type){return type==SUNFLOWER||type==PEASHOOTER?level>=1:type==GIGANUT?level>=2:type==CHOMPER?level>=3:type==REPEATER?level>=4:type==MINE&&level>=5;}
int cost(int type){return type==SUNFLOWER?50:type==PEASHOOTER?100:type==GIGANUT?125:type==CHOMPER?150:type==REPEATER?200:50;}
int mowerReward(){int r=0;for(Mower m:mowers)if(!m.used)r+=50;return r;}
void winLevel(){screen=WIN;coins+=mowerReward();if(level<LEVELS)unlocked=Math.max(unlocked,level+1);post(MainActivity.this::showWinVideo);}
void drawPause(Canvas c){overlay(c,Color.argb(190,0,0,0));title(c,"TẠM DỪNG",0.34f,30);button(c,.32f,.50f,.68f,.63f,"TIẾP TỤC");}
void drawWin(Canvas c){overlay(c,Color.argb(215,0,80,0));title(c,"CHIẾN THẮNG!",0.32f,30);center(c,"+"+mowerReward()+" XU TỪ XE CẮT CỎ",getWidth()/2f,getHeight()*.42f,getHeight()*.04f,Color.WHITE);button(c,.30f,.55f,.70f,.68f,level<LEVELS?"MÀN TIẾP":"MENU");}
void drawLose(Canvas c){overlay(c,Color.argb(220,100,0,0));title(c,"THUA!",0.32f,30);button(c,.30f,.50f,.70f,.63f,"CHƠI LẠI");button(c,.30f,.68f,.70f,.81f,"MENU");}
void hp(Canvas c,float x,float y,float w,float h,float value,float max){p.setColor(Color.RED);c.drawRect(x,y,x+w,y+h,p);p.setColor(Color.GREEN);c.drawRect(x,y,x+w*Math.max(0,Math.min(1,value/max)),y+h,p);}
void overlay(Canvas c,int color){p.setColor(color);c.drawRect(0,0,getWidth(),getHeight(),p);}
void title(Canvas c,String s,float y,float size){center(c,s,getWidth()/2f,getHeight()*y,getHeight()*.07f,Color.WHITE);}
void small(Canvas c,String s,float y,int color){center(c,s,getWidth()/2f,getHeight()*y,getHeight()*.035f,color);}
void smallAt(Canvas c,String s,float x,float y,int color){textPaint.setTextAlign(Paint.Align.LEFT);textPaint.setTextSize(getHeight()*.033f);textPaint.setColor(color);c.drawText(s,x,y,textPaint);}
void center(Canvas c,String s,float x,float y,float size,int color){textPaint.setTextAlign(Paint.Align.CENTER);textPaint.setTextSize(size);textPaint.setColor(color);c.drawText(s,x,y,textPaint);}
void button(Canvas c,float x1,float y1,float x2,float y2,String s){p.setColor(Color.rgb(65,135,70));c.drawRoundRect(getWidth()*x1,getHeight()*y1,getWidth()*x2,getHeight()*y2,16,16,p);center(c,s,getWidth()*(x1+x2)/2,getHeight()*(y1+y2)/2+getHeight()*.02f,getHeight()*.038f,Color.WHITE);}
@Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()!=MotionEvent.ACTION_UP)return true;float x=e.getX(),y=e.getY();
if(screen==HOME){if(inside(x,y,.32f,.38f,.68f,.50f)){startLevel(level);}else if(inside(x,y,.32f,.55f,.68f,.67f)){screen=LEVELS_SCREEN;}invalidate();return true;}
if(screen==LEVELS_SCREEN){if(inside(x,y,.04f,.83f,.20f,.94f)){screen=HOME;invalidate();return true;}for(int i=1;i<=LEVELS;i++){int cc=(i-1)%3,rr=(i-1)/3;float x1=.18f+cc*.22f,y1=.18f+rr*.20f;if(insidePx(x,y,getWidth()*x1,getHeight()*y1,getWidth()*(x1+.17f),getHeight()*(y1+.13f))&&levelOpen(i)){startLevel(i);return true;}}return true;}
if(screen==WIN){if(inside(x,y,.30f,.55f,.70f,.68f)){if(level<LEVELS)startLevel(level+1);else screen=HOME;invalidate();}return true;}
if(screen==LOSE){if(inside(x,y,.30f,.50f,.70f,.63f)){startLevel(level);}else if(inside(x,y,.30f,.68f,.70f,.81f)){screen=HOME;}invalidate();return true;}
if(screen==PAUSE){if(inside(x,y,.32f,.50f,.68f,.63f)){screen=PLAY;invalidate();}return true;}
// Nhặt sun.
Iterator<SunDrop> si=sunDrops.iterator();while(si.hasNext()){SunDrop sd=si.next();if(Math.hypot(x-sd.x,y-sd.y)<Math.max(55,getHeight()*.065f)){sun+=25;si.remove();invalidate();return true;}}
// Plant cards + shovel + plant food.
float cardY=getHeight()*.075f,cardH=getHeight()*.09f;
if(y>=cardY&&y<=cardY+cardH){for(int i=0;i<6;i++){float x1=getWidth()*.01f+i*getWidth()*.075f;if(x>=x1&&x<=x1+getWidth()*.065f){int[] ids={SUNFLOWER,PEASHOOTER,GIGANUT,CHOMPER,REPEATER,MINE};if(plantUnlocked(ids[i])){selected=ids[i];tool=NONE;}return true;}}
float sx=getWidth()*.465f;if(x>=sx&&x<=sx+getWidth()*.065f){tool=SHOVEL;return true;}
if(x>=getWidth()*.76f&&x<=getWidth()*.91f&&y>=getHeight()*.075f&&y<=getHeight()*.145f){if(coins>=100){coins-=100;plantFood++;pfMsg="MUA PF -100 XU";pfMsgUntil=System.currentTimeMillis()+1000;}else{pfMsg="KHONG DU 100 XU";pfMsgUntil=System.currentTimeMillis()+1000;}invalidate();return true;}
float fx=getWidth()*.54f;if(x>=fx&&x<=fx+getWidth()*.12f&&plantFood>0){tool=PF_TOOL;invalidate();return true;}}
if(x>=left&&x<left+COLS*cw&&y>=top&&y<top+ROWS*ch){int col=(int)((x-left)/cw),row=(int)((y-top)/ch);if(tool==SHOVEL){plants[row][col]=null;tool=NONE;return true;}if(tool==PF_TOOL){Plant pfPlant=plants[row][col];if(pfPlant!=null&&plantFood>0){usePF(pfPlant);plantFood--;pfMsg="PLANT FOOD!";pfMsgUntil=System.currentTimeMillis()+900;}tool=NONE;invalidate();return true;}if(plants[row][col]==null&&plantUnlocked(selected)){int co=cost(selected);if(sun>=co){sun-=co;plants[row][col]=new Plant(selected,row,col);}}invalidate();return true;}return true;}
void usePF(Plant a){long now=System.currentTimeMillis();if(a.type==SUNFLOWER||a.type==PEASHOOTER||a.type==REPEATER){a.pfUntil=now+3000;a.lastAction=now-100;if(a.type!=SUNFLOWER)playPFSound();}else if(a.type==GIGANUT){a.maxHp=8000;a.hp=8000;}else if(a.type==CHOMPER){Zombie z=near(a.row,a.col);if(z!=null)z.hp=0;}else if(a.type==MINE){for(Zombie z:zombies)if(z.row==a.row&&Math.abs(z.x-(left+a.col*cw+cw/2))<cw*2.0f)z.hp-=1800;a.hp=0;playMineSound();}}
boolean inside(float x,float y,float x1,float y1,float x2,float y2){return insidePx(x,y,getWidth()*x1,getHeight()*y1,getWidth()*x2,getHeight()*y2);}
boolean insidePx(float x,float y,float x1,float y1,float x2,float y2){return x>=x1&&x<=x2&&y>=y1&&y<=y2;}
class Plant{final int type,row,col; int maxHp;int hp;long lastAction,secondShotAt,armedAt,pfUntil;Plant(int type,int row,int col){this.type=type;this.row=row;this.col=col;maxHp=type==GIGANUT?4000:type==MINE?120:500;hp=maxHp;long n=System.currentTimeMillis();lastAction=n;armedAt=type==MINE?n+3000:Long.MAX_VALUE;pfUntil=0;}}
class Zombie{float x,y,speed,dir=-1;final int row,type;final boolean boss;float hp,maxHp;int damage;long lastAttack,catchReadyAt;Zombie(int row,int type,boolean boss){this.row=row;this.type=type;this.boss=boss;x=left+COLS*cw+cw;y=top+row*ch+ch*.16f;if(boss){maxHp=5000;speed=cw*.10f;damage=45;}
else if(type==1){maxHp=1000+level*100;speed=cw*.13f;damage=32;}
else if(type==2){maxHp=450+level*60;speed=cw*.32f;damage=18;}
else{maxHp=650+level*75;speed=cw*.20f;damage=24;}
hp=maxHp;catchReadyAt=System.currentTimeMillis()+5000;}
class Pea{float x;final float y;final int row,damage;final boolean big,fromZombie;final int dir;Pea(float x,float y,int row,int damage,boolean big,boolean fromZombie,int dir){this.x=x;this.y=y;this.row=row;this.damage=damage;this.big=big;this.fromZombie=fromZombie;this.dir=dir;}}
class SunDrop{final float x,y;SunDrop(float x,float y){this.x=x;this.y=y;}}
class Mower{final int row;float x;boolean used,active;Mower(int row){this.row=row;this.x=left-cw*.5f;}}
}
}
 }
                                                                                                                                                                                                                                                                                                                                                  
