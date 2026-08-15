package com.alpvz.gardendefense;

import android.app.*;
import android.os.*;
import android.graphics.*;
import android.view.*;
import java.util.*;

public class MainActivity extends Activity{
 @Override public void onCreate(Bundle b){
  super.onCreate(b);
  setContentView(new Game(this));
 }

 class Game extends View{
  Paint p=new Paint(3);
  Random rnd=new Random();

  Bitmap san,sunImg,peaImg,gigaImg,chompImg,zomImg,vinhImg,peaShot;

  ArrayList<Plant> plants=new ArrayList<>();
  ArrayList<Zom> zoms=new ArrayList<>();
  ArrayList<Shot> shots=new ArrayList<>();
  ArrayList<Bomb> bombs=new ArrayList<>();
  ArrayList<Drop> suns=new ArrayList<>();
  ArrayList<Drop> coins=new ArrayList<>();

  final int ROWS=5,COLS=9,MAX_LEVEL=9;

  float left=15,top=195,cw,ch;

  int level=1,spawned=0,killed=0;
  int sun=500,coin=9999,pf=0;
  int selected=0,skill=0;

  boolean win=false,lose=false,chomperUnlocked=false;

  long last,spawnAt,levelStart;

  Game(Activity a){
   super(a);

   last=spawnAt=levelStart=System.currentTimeMillis();

   san=img("san");
   sunImg=img("sun");
   peaImg=img("peashoot");
   gigaImg=img("giganut");
   chompImg=img("chomper");
   zomImg=img("zomplatz");
   vinhImg=img("zomvinhhung");
   peaShot=img("gigapea");
  }

  Bitmap img(String n){
   int id=getResources().getIdentifier(
     n,"drawable",getPackageName()
   );

   return id==0
     ?null
     :BitmapFactory.decodeResource(
        getResources(),id
      );
  }

  @Override protected void onDraw(Canvas c){
   cw=(getWidth()-2*left)/COLS;
   ch=(getHeight()-top-10)/ROWS;

   p.setColor(Color.rgb(55,125,60));
   c.drawRect(
     0,0,getWidth(),getHeight(),p
   );

   ui(c);
   board(c);
   drawDrops(c);
   drawPlants(c);
   drawShots(c);
   drawZoms(c);

   if(win||lose)
    end(c);
   else{
    update();
    postInvalidateDelayed(30);
   }
  }

  void ui(Canvas c){
   p.setColor(Color.rgb(35,85,40));
   c.drawRect(
     0,0,getWidth(),180,p
   );

   p.setColor(Color.WHITE);
   p.setTextSize(15);

   c.drawText(
     "M"+level+
     "  SUN:"+sun+
     "  XU:"+coin+
     "  PF:"+pf,
     8,20,p
   );

   float w=getWidth()/4f;

   card(c,0,1,"SUN",w);
   card(c,w,2,"PEA",w);
   card(c,2*w,3,"GIGA",w);

   if(chomperUnlocked)
    card(c,3*w,4,"CHOMP",w);

   btn(c,0,"SẤM 30",1,w);
   btn(c,w,"BĂNG 60",2,w);
   btn(c,2*w,"LỬA 90",3,w);
   btn(c,3*w,"PF 100",9,w);
  }

  void card(
   Canvas c,
   float x,
   int type,
   String name,
   float w
  ){
   p.setColor(
     selected==type
       ?Color.YELLOW
       :Color.WHITE
   );

   c.drawRect(
     x+2,30,
     x+w-2,100,
     p
   );

   Bitmap b=
     type==1?sunImg:
     type==2?peaImg:
     type==3?gigaImg:
     chompImg;

   if(b!=null){
    c.drawBitmap(
      b,null,
      new RectF(
       x+5,35,
       x+55,95
      ),
      p
    );
   }

   p.setColor(Color.DKGRAY);
   p.setTextSize(11);

   c.drawText(
     name,
     x+60,
     68,
     p
   );
  }

  void btn(
   Canvas c,
   float x,
   String s,
   int type,
   float w
  ){
   p.setColor(
     skill==type
       ?Color.YELLOW
       :Color.WHITE
   );

   c.drawRect(
     x+2,110,
     x+w-2,175,
     p
   );

   p.setColor(Color.DKGRAY);
   p.setTextSize(10);

   c.drawText(
     s,
     x+8,
     146,
     p
   );
  }

  void board(Canvas c){
   if(san!=null){
    c.drawBitmap(
      san,null,
      new RectF(
       left,
       top,
       left+COLS*cw,
       top+ROWS*ch
      ),
      p
    );
    return;
   }

   for(int r=0;r<ROWS;r++){
    for(int q=0;q<COLS;q++){

     p.setColor(
      (r+q)%2==0
       ?Color.rgb(115,190,75)
       :Color.rgb(105,180,68)
     );

     c.drawRect(
      left+q*cw,
      top+r*ch,
      left+(q+1)*cw-2,
      top+(r+1)*ch-2,
      p
     );
    }
   }
  }

  void drawPlants(Canvas c){
   for(Plant a:plants){

    Bitmap b=
     a.t==1?sunImg:
     a.t==2?peaImg:
     a.t==3?gigaImg:
     chompImg;

    float x=left+a.c*cw;
    float y=top+a.r*ch;

    if(b!=null){
     c.drawBitmap(
      b,null,
      new RectF(
       x+4,y+4,
       x+cw-4,y+ch-4
      ),
      p
     );
    }

    hp(
     c,
     x+cw*.2f,
     y+3,
     cw*.6f,
     a.hp,
     a.max
    );
   }
  }

  void drawShots(Canvas c){
   for(Shot s:shots){

    if(peaShot!=null){

     c.drawBitmap(
      peaShot,null,
      new RectF(
       s.x-15,
       s.y-15,
       s.x+15,
       s.y+15
      ),
      p
     );

    }else{

     p.setColor(Color.GREEN);

     c.drawCircle(
      s.x,s.y,
      12,p
     );
    }
   }
  }

  void drawZoms(Canvas c){

   for(Zom z:zoms){

    float w=
     z.v?60:
     z.big?100:
     75;

    float h=
     z.v?80:
     z.big?135:
     105;

    Bitmap b=
     z.v?vinhImg:zomImg;

    if(b!=null){

     c.drawBitmap(
      b,null,
      new RectF(
       z.x-w/2,
       z.y-h/2,
       z.x+w/2,
       z.y+h/2
      ),
      p
     );
    }

    hp(
     c,
     z.x-30,
     z.y-h/2-6,
     60,
     z.hp,
     z.max
    );
   }

   for(Bomb b:bombs){

    p.setColor(Color.rgb(200,40,40));

    c.drawCircle(
     b.x,b.y,
     10,p
    );
   }
  }

  void drawDrops(Canvas c){

   for(Drop d:suns){

    p.setColor(Color.YELLOW);

    c.drawCircle(
     d.x,d.y,
     14,p
    );
   }

   for(Drop d:coins){

    p.setColor(
     d.type==1
      ?Color.LTGRAY
      :d.type==2
      ?Color.YELLOW
      :Color.CYAN
    );

    c.drawCircle(
     d.x,d.y,
     10,p
    );
   }
  }

  void hp(
   Canvas c,
   float x,
   float y,
   float w,
   int v,
   int m
  ){
   p.setColor(Color.RED);

   c.drawRect(
    x,y,
    x+w,y+5,
    p
   );

   p.setColor(Color.GREEN);

   float q=
    Math.max(
     0,
     Math.min(
      1,
      v/(float)Math.max(1,m)
     )
    );

   c.drawRect(
    x,y,
    x+w*q,
    y+5,
    p
   );
  }

  void update(){

   long now=
    System.currentTimeMillis();

   float dt=
    Math.min(
     .1f,
     (now-last)/1000f
    );

   last=now;

   int targetCount=
    12+level*2;

   /*
    * Zombie spawn
    */
   if(
    spawned<targetCount &&
    now-spawnAt>=
     Math.max(
      1800,
      5000-level*250
     )
   ){

    spawn();
    spawnAt=now;
   }

   /*
    * Sun rơi từ trên xuống.
    */
   if(rnd.nextInt(240)==0){

    dropSun(
     20+rnd.nextInt(
      Math.max(
       1,
       getWidth()-40
      )
     ),
     top+10
    );
   }

   /*
    * Plant update
    */
   for(Plant a:plants){

    a.timer+=dt;

    if(a.foodTime>0){

     a.foodTime-=dt;

     if(a.foodTime<=0)
      a.foodTime=0;
    }

    if(a.cd>0){

     a.cd-=dt;

     if(a.cd<0)
      a.cd=0;
    }

    /*
     * Sunflower
     */
    if(
     a.t==1 &&
     a.timer>=
      (
       a.foodTime>0
        ?0.1f
        :5f
      )
    ){

     dropSun(
      left+a.c*cw+cw/2,
      top+a.r*ch+ch/2
     );

     a.timer=0;
    }

    /*
     * Peashooter
     */
    if(
     a.t==2 &&
     a.timer>=
      (
       a.foodTime>0
        ?0.1f
        :1.2f
      ) &&
     hasRow(a.r)
    ){

     shots.add(
      new Shot(
       left+a.c*cw+cw-5,
       top+a.r*ch+ch/2,
       a.r
      )
     );

     a.timer=0;
    }

    /*
     * Chomper thường:
     * mới đặt = ăn ngay.
     * ăn xong = 40 giây.
     */
    if(
     a.t==4 &&
     !a.food &&
     a.cd<=0
    ){

     Zom z=target(a);

     if(z!=null){

      z.hp=0;
      a.cd=40;
     }
    }

    /*
     * Chomper Plant Food:
     * hút zombie vào miệng.
     */
    if(
     a.t==4 &&
     a.food
    ){

     float mouth=
      left+a.c*cw+cw/2;

     boolean any=false;

     for(Zom z:zoms){

      if(z.r==a.r){

       any=true;

       float dx=
        mouth-z.x;

       if(Math.abs(dx)>8){

        z.x+=
         dx>0
          ?Math.min(
            18,
            Math.abs(dx)
           )
          :-Math.min(
            18,
            Math.abs(dx)
           );
       }
      }
     }

     a.foodTimer-=dt;

     if(
      !any ||
      a.foodTimer<=0
     ){

      for(Zom z:zoms)
       if(z.r==a.r)
        z.hp=0;

      a.food=false;
      a.cd=0;
     }
    }
   }

   /*
    * Zombie update
    */
   for(Zom z:zoms){

    if(z.x<-70){

     lose=true;
     continue;
    }

    /*
     * Zomvinhhung:
     * 500 HP
     * đứng ở ô 2
     * ném bom mỗi 2 giây
     */
    if(z.v){

     float stop=
      left+1.5f*cw;

     if(z.x>stop)
      z.x-=.6f;
     else{
      z.x=stop;
      z.stopped=true;
     }

     if(z.stopped){

      z.throwTimer+=dt;

      if(z.throwTimer>=2){

       bombs.add(
        new Bomb(
         z.x,
         z.y,
         left+.5f*cw,
         top+z.r*ch+ch/2,
         z.r
        )
       );

       z.throwTimer=0;
      }
     }

    }else{

     Plant a=findPlant(z);

     if(a!=null){

      if(now-a.bite>=500){

       a.hp-=100;
       a.bite=now;
      }

     }else{

      float speed=
       z.big?.55f:1f;

      if(z.slow>0)
       speed*=.45f;

      z.x-=speed;
     }
    }

    if(z.slow>0)
     z.slow-=dt;
   }

   /*
    * Pea đạn
    */
   for(Shot s:shots){

    s.x+=8;

    for(Zom z:zoms){

     if(
      z.r==s.r &&
      Math.abs(z.x-s.x)<30
     ){

      z.hp-=25;
      s.x=getWidth()+100;
      break;
     }
    }
   }

   /*
    * Bom bay tới mục tiêu
    */
   for(Bomb b:bombs){

    float dx=b.tx-b.x;
    float dy=b.ty-b.y;

    float d=
     (float)Math.sqrt(
      dx*dx+dy*dy
     );

    if(d<=b.sp || d==0){

     explode(b);
     b.dead=true;

    }else{

     b.x+=dx/d*b.sp;
     b.y+=dy/d*b.sp;
    }
   }

   clean();

   /*
    * Mỗi màn >= 60 giây.
    */
   if(
    now-levelStart>=60000 &&
    spawned>=targetCount &&
    zoms.isEmpty()
   ){

    win=true;
   }

   updateDrops(dt);
  }

  void updateDrops(float dt){

   for(Iterator<Drop>i=suns.iterator();
       i.hasNext();){

    Drop d=i.next();

    d.y+=25*dt;
    d.life-=dt;

    if(
     d.life<=0 ||
     d.y>getHeight()-20
    )
     i.remove();
   }

   for(Iterator<Drop>i=coins.iterator();
       i.hasNext();){

    Drop d=i.next();

    d.life-=dt;

    if(d.life<=0)
     i.remove();
   }
  }

  void ZomThrow(Zom z){
   bombs.add(
    new Bomb(
     z.x,
     z.y,
     left+.5f*cw,
     top+z.r*ch+ch/2,
     z.r
    )
   );
  }

  boolean hasRow(int r){
   for(Zom z:zoms)
    if(z.r==r)
     return true;
   return false;
  }

  Zom target(Plant a){

   float x=
    left+a.c*cw+cw/2;

   Zom best=null;
   float bestD=
    Float.MAX_VALUE;

   for(Zom z:zoms){

    if(z.r!=a.r)
     continue;

    float d=
     Math.abs(z.x-x);

    if(
     d<cw*1.5f &&
     d<bestD
    ){

     best=z;
     bestD=d;
    }
   }

   return best;
  }

  Plant findPlant(Zom z){

   for(Plant a:plants){

    if(
     a.r==z.r &&
     Math.abs(
      z.x-
      (
       left+
       a.c*cw+
       cw/2
      )
     )<
     (z.big?70:55)
    )
     return a;
   }

   return null;
  }

  void explode(Bomb b){

   int col=
    (int)(
     (b.tx-left)/cw
    );

   for(Plant a:plants){

    if(
     Math.abs(a.c-col)<=1 &&
     Math.abs(a.r-b.r)<=1
    ){

     a.hp-=100;
    }
   }
  }

  void spawn(){

   int r=
    rnd.nextInt(ROWS);

   boolean v=
    level>=3 &&
    spawned%5==0;

   boolean big=
    level>=2 &&
    spawned%3==0 &&
    !v;

   zoms.add(
    new Zom(
     getWidth()+60,
     top+r*ch+ch/2,
     r,
     big,
     v
    )
   );

   spawned++;
  }

  void clean(){

   for(
    Iterator<Plant>i=plants.iterator();
    i.hasNext();
   ){

    if(i.next().hp<=0)
     i.remove();
   }

   for(
    Iterator<Zom>i=zoms.iterator();
    i.hasNext();
   ){

    Zom z=i.next();

    if(z.hp<=0){

     i.remove();
     killed++;

     sun+=25;

     int q=
      rnd.nextInt(100);

     /*
      * Bạc 10% = 25
      * Vàng 5% = 50
      * Kim cương 1% = 100
      */
     if(q<1)
      dropCoin(z.x,z.y,3);
     else if(q<6)
      dropCoin(z.x,z.y,2);
     else if(q<16)
      dropCoin(z.x,z.y,1);
    }
   }

   for(
    Iterator<Shot>i=shots.iterator();
    i.hasNext();
   ){

    if(
     i.next().x>
     getWidth()+60
    )
     i.remove();
   }

   for(
    Iterator<Bomb>i=bombs.iterator();
    i.hasNext();
   ){

    if(i.next().dead)
     i.remove();
   }
  }

  void dropSun(
   float x,
   float y
  ){

   suns.add(
    new Drop(
     x,y,0
    )
   );
  }

  void dropCoin(
   float x,
   float y,
   int type
  ){

   coins.add(
    new Drop(
     x,y,type
    )
   );
  }

  void usePF(
   int r,
   int c
  ){

   if(pf<=0)
    return;

   for(Plant a:plants){

    if(
     a.r!=r ||
     a.c!=c
    )
     continue;

    /*
     * Sunflower
     */
    if(a.t==1){

     a.foodTime=3;
     sun+=300;
    }

    /*
     * Peashooter
     */
    else if(a.t==2){

     a.foodTime=3;
    }

    /*
     * Chomper
     */
    else if(a.t==4){

     a.food=true;
     a.foodTimer=.7f;
     a.cd=0;
    }

    else
     return;

    pf--;
    return;
   }
  }

  void useSkill(
   int r,
   int c,
   int s
  ){

   int cost=
    s==1
     ?30
     :s==2
     ?60
     :90;

   if(coin<cost)
    return;

   float x=
    left+c*cw+cw/2;

   for(Zom z:zoms){

    if(
     Math.abs(
      z.x-x
     )<cw*1.6f &&
     Math.abs(
      z.r-r
     )<=1
    ){

     if(s==1)
      z.hp-=500;
     else if(s==2)
      z.slow=5;
     else
      z.hp-=800;
    }
   }

   coin-=cost;
  }

  boolean occupied(
   int r,
   int c
  ){

   for(Plant a:plants)
    if(
     a.r==r &&
     a.c==c
    )
     return true;

   return false;
  }

  @Override public boolean onTouchEvent(
   MotionEvent e
  ){

   if(
    e.getAction() !=
    MotionEvent.ACTION_DOWN
   )
    return true;

   float x=e.getX();
   float y=e.getY();
   float w=getWidth()/4f;

   if(win){

    if(level==2)
     chomperUnlocked=true;

    if(level<MAX_LEVEL){

     level++;
     reset(false);

    }else{

     reset(true);
    }

    invalidate();
    return true;
   }

   if(lose){

    reset(true);
    invalidate();
    return true;
   }

   /*
    * Chọn cây
    */
   if(y>=25&&y<=105){

    if(x<w)
     selected=1;
    else if(x<2*w)
     selected=2;
    else if(x<3*w)
     selected=3;
    else if(chomperUnlocked)
     selected=4;

    skill=0;
    invalidate();

    return true;
   }

   /*
    * Kỹ năng + mua PF
    */
   if(y>=105&&y<=180){

    if(x<w){

     skill=1;

    }else if(x<2*w){

     skill=2;

    }else if(x<3*w){

     skill=3;

    }else{

     if(coin>=100){

      coin-=100;
      pf++;
     }

     skill=9;
    }

    invalidate();
    return true;
   }

   /*
    * Sân
    */
   if(
    x>=left &&
    x<left+COLS*cw &&
    y>=top &&
    y<top+ROWS*ch
   ){

    int c=
     (int)(
      (x-left)/cw
     );

    int r=
     (int)(
      (y-top)/ch
     );

    /*
     * Nhặt Sun
     */
    for(
     Iterator<Drop>i=suns.iterator();
     i.hasNext();
    ){

     Drop d=i.next();

     if(
      Math.abs(d.x-x)<30 &&
      Math.abs(d.y-y)<30
     ){

      i.remove();
      sun+=50;

      invalidate();
      return true;
     }
    }

    /*
     * Nhặt xu
     */
    for(
     Iterator<Drop>i=coins.iterator();
     i.hasNext();
    ){

     Drop d=i.next();

     if(
      Math.abs(d.x-x)<25 &&
      Math.abs(d.y-y)<25
     ){

      coin+=
       d.type==3
        ?100
        :d.type==2
        ?50
        :25;

      i.remove();

      invalidate();
      return true;
     }
    }

    /*
     * Plant Food
     */
    if(skill==9){

     usePF(r,c);
     skill=0;

    }else if(skill>0){

     useSkill(
      r,c,skill
     );

     skill=0;

    }else if(
     selected>0 &&
     !occupied(r,c)
    ){

     int cost=
      selected==1
       ?50
       :selected==2
       ?100
       :150;

     int max=
      selected==1
       ?300
       :selected==2
       ?400
       :selected==3
       ?6000
       :800;

     if(sun>=cost){

      plants.add(
       new Plant(
        selected,
        r,
        c,
        max
       )
      );

      sun-=cost;
      selected=0;
     }
    }

    invalidate();
   }

   return true;
  }

  void reset(boolean all){

   plants.clear();
   zoms.clear();
   shots.clear();
   bombs.clear();
   suns.clear();
   coins.clear();

   selected=0;
   skill=0;
   spawned=0;
   killed=0;

   win=false;
   lose=false;

   last=
    spawnAt=
    levelStart=
    System.currentTimeMillis();

   if(all){

    level=1;
    chomperUnlocked=false;

    sun=500;
    coin=9999;
    pf=0;
   }
  }

  void end(Canvas c){

   p.setColor(
    0xAA000000
   );

   c.drawRect(
    0,0,
    getWidth(),
    getHeight(),
    p
   );

   p.setColor(Color.WHITE);
   p.setTextAlign(
    Paint.Align.CENTER
   );

   p.setTextSize(28);

   c.drawText(
    lose
     ?"THUA!"
     :level==2
     ?"MỞ KHÓA CHOMPER!"
     :"THẮNG MÀN "+level,
    getWidth()/2f,
    getHeight()/2f,
    p
   );

   p.setTextSize(15);

   c.drawText(
    "CHẠM ĐỂ TIẾP TỤC",
    getWidth()/2f,
    getHeight()/2f+35,
    p
   );

   p.setTextAlign(
    Paint.Align.LEFT
   );
  }

  class Plant{
   int t,r,c,hp,max;
   float timer,foodTime,cd,foodTimer;
   boolean food;
   long bite;

   Plant(
    int t,
    int r,
    int c,
    int h
   ){

    this.t=t;
    this.r=r;
    this.c=c;

    hp=max=h;
   }
  }

  class Zom{
   float x,y,slow,throwTimer;
   int r,hp,max;

   boolean big;
   boolean v;
   boolean stopped;

   Zom(
    float x,
    float y,
    int r,
    boolean b,
    boolean v
   ){

    this.x=x;
    this.y=y;
    this.r=r;

    big=b;
    this.v=v;

    hp=max=
     v
      ?500
      :b
      ?1000
      :300;
   }
  }

  class Shot{
   float x,y;
   int r;

   Shot(
    float x,
    float y,
    int r
   ){

    this.x=x;
    this.y=y;
    this.r=r;
   }
  }

  class Bomb{
   float x,y,tx,ty;
   float sp=5;
   int r;
   boolean dead;

   Bomb(
    float x,
    float y,
    float tx,
    float ty,
    int r
   ){

    this.x=x;
    this.y=y;

    this.tx=tx;
    this.ty=ty;

    this.r=r;
   }
  }

  class Drop{
   float x,y;
   float life=12;
   int type;

   Drop(
    float x,
    float y,
    int type
   ){

    this.x=x;
    this.y=y;
    this.type=type;
   }
  }
 }
       }
