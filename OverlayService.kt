package id.eltiy.micinai

import android.app.*
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.*
import android.view.*
import android.widget.*
import java.util.*
import kotlin.math.roundToInt

class OverlayService:Service(){
 private lateinit var wm:WindowManager;private var root:LinearLayout?=null;private var content:LinearLayout?=null;private lateinit var lp:WindowManager.LayoutParams;private var collapsed=false
 private val handler=Handler(Looper.getMainLooper());private val poll=object:Runnable{override fun run(){if(root!=null){refresh(false);handler.postDelayed(this,90_000)}}}
 override fun onBind(i:Intent?):IBinder?=null
 override fun onCreate(){super.onCreate();val channel="micin_float";getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(channel,"MICIN AI Scanner",NotificationManager.IMPORTANCE_LOW));val pi=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE);val n=Notification.Builder(this,channel).setSmallIcon(android.R.drawable.ic_menu_search).setContentTitle("MICIN AI aktif").setContentText("Floating scanner sedang berjalan").setContentIntent(pi).build();if(Build.VERSION.SDK_INT>=34)startForeground(7,n,android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)else startForeground(7,n);wm=getSystemService(WINDOW_SERVICE) as WindowManager;show();refresh(true);handler.postDelayed(poll,90_000)}
 private fun dp(x:Int)=(x*resources.displayMetrics.density).roundToInt()
 private fun label(s:String,size:Float=11f)=TextView(this).apply{text=s;textSize=size;setTextColor(Color.WHITE);setPadding(dp(7),dp(6),dp(7),dp(6))}
 private fun show(){if(!android.provider.Settings.canDrawOverlays(this)){stopSelf();return};val box=LinearLayout(this).apply{orientation=1;background=GradientDrawable().apply{setColor(Color.rgb(12,22,39));cornerRadius=dp(16).toFloat();setStroke(dp(1),Color.rgb(49,201,173))};elevation=dp(8).toFloat()};val head=LinearLayout(this).apply{orientation=0;setBackgroundColor(Color.rgb(24,55,69))};val title=label("≡ MICIN AI",14f).apply{setTextColor(Color.rgb(75,231,187))};head.addView(title,LinearLayout.LayoutParams(0,dp(46),1f));val mini=label("−",20f);val ref=label("↻",20f);val close=label("×",20f);head.addView(mini);head.addView(ref);head.addView(close);box.addView(head);content=LinearLayout(this).apply{orientation=1;setPadding(dp(7),dp(4),dp(7),dp(7))};box.addView(content);mini.setOnClickListener{collapsed=!collapsed;content?.visibility=if(collapsed)View.GONE else View.VISIBLE;mini.text=if(collapsed)"+" else "−"};ref.setOnClickListener{refresh(true)};close.setOnClickListener{stopSelf()}
 lp=WindowManager.LayoutParams(dp(300),WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT).apply{gravity=Gravity.TOP or Gravity.START;x=dp(12);y=dp(110)}
 title.setOnTouchListener(object:View.OnTouchListener{var x=0;var y=0;var rx=0f;var ry=0f;override fun onTouch(v:View?,e:MotionEvent):Boolean{when(e.action){MotionEvent.ACTION_DOWN->{x=lp.x;y=lp.y;rx=e.rawX;ry=e.rawY;return true};MotionEvent.ACTION_MOVE->{lp.x=x+(e.rawX-rx).roundToInt();lp.y=y+(e.rawY-ry).roundToInt();try{wm.updateViewLayout(box,lp)}catch(_:Exception){};return true};MotionEvent.ACTION_UP->return true};return false}});root=box;wm.addView(box,lp)
 }
 private fun refresh(force:Boolean){content?.removeAllViews();content?.addView(label("Scanning ${ScanConfig.load(this).chain.uppercase()}…"));Scanner.scan(ScanConfig.load(this),force){r->if(root==null)return@scan;content?.removeAllViews();content?.addView(label("${r.coins.size} HASIL • ${r.error?:"LIVE DATA"}"));r.coins.take(5).forEach{c->val item=label("${c.symbol.take(12)}  ${c.score}/100 • ${"%+.1f".format(Locale.US,c.change1h)}%\nLiq $${"%,.0f".format(Locale.US,c.liquidity)} • Vol1j $${"%,.0f".format(Locale.US,c.volume1h)}\n${c.buys1h}/${c.sells1h} buy/sell • ${c.risk}",11f);item.setOnClickListener{if(c.url.startsWith("https://dexscreener.com/"))startActivity(Intent(Intent.ACTION_VIEW,android.net.Uri.parse(c.url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))};content?.addView(item)};content?.addView(label("⚠ Bukan audit kontrak / sinyal beli",9f))}}
 override fun onDestroy(){handler.removeCallbacks(poll);root?.let{try{wm.removeView(it)}catch(_:Exception){}};root=null;super.onDestroy()}
}
