package id.eltiy.micinai

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup
import android.widget.*
import android.text.InputType
import java.text.SimpleDateFormat
import java.util.*

class MainActivity:Activity(){
 private lateinit var chain:Spinner;private lateinit var liq:EditText;private lateinit var vol:EditText;private lateinit var age:Spinner;private lateinit var score:Spinner;private lateinit var status:TextView;private lateinit var list:LinearLayout
 private val chains=arrayOf("Solana","Base","BNB Chain","Ethereum");private val ids=arrayOf("solana","base","bsc","ethereum");private val ages=arrayOf("24 jam","72 jam","7 hari","30 hari");private val ageVals=intArrayOf(24,72,168,720);private val scores=arrayOf("Semua","40+","60+","80+");private val scoreVals=intArrayOf(0,40,60,80)
 private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
 private fun txt(s:String,size:Float=13f)=TextView(this).apply{text=s;textSize=size;setTextColor(Color.WHITE);setPadding(0,dp(5),0,dp(5))}
 override fun onCreate(saved:Bundle?){super.onCreate(saved);window.statusBarColor=Color.rgb(9,15,28)
  val root=LinearLayout(this).apply{orientation=1;setPadding(dp(18),dp(22),dp(18),dp(12));setBackgroundColor(Color.rgb(10,18,32))}
  root.addView(txt("◈ MICIN AI SCREENER",23f).apply{setTextColor(Color.rgb(67,225,184))});root.addView(txt("DEX market scanner • v0.4 • risk flags • tanpa auto-trading",11f))
  root.addView(txt("JARINGAN",11f));chain=Spinner(this).apply{adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,chains)};root.addView(chain)
  val cfg=ScanConfig.load(this);chain.setSelection(ids.indexOf(cfg.chain).coerceAtLeast(0))
  root.addView(txt("LIKUIDITAS MINIMUM (USD)",11f));liq=field(cfg.minLiquidity.toLong().toString());root.addView(liq)
  root.addView(txt("VOLUME 1 JAM MINIMUM (USD)",11f));vol=field(cfg.minVolume.toLong().toString());root.addView(vol)
  val row=LinearLayout(this).apply{orientation=0};val col1=LinearLayout(this).apply{orientation=1};col1.addView(txt("USIA PAIR MAKS.",11f));age=Spinner(this).apply{adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,ages)};col1.addView(age);row.addView(col1,LinearLayout.LayoutParams(0,-2,1f));val col2=LinearLayout(this).apply{orientation=1};col2.addView(txt("SKOR MINIMUM",11f));score=Spinner(this).apply{adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,scores)};col2.addView(score);row.addView(col2,LinearLayout.LayoutParams(0,-2,1f));root.addView(row)
  age.setSelection(ageVals.indexOf(cfg.maxAgeHours).coerceAtLeast(0));score.setSelection(scoreVals.indexOf(cfg.minScore).coerceAtLeast(0))
  val actions=LinearLayout(this).apply{orientation=0};actions.addView(btn("SCAN") { saveCfg();scan(true) },LinearLayout.LayoutParams(0,-2,1f));actions.addView(btn("FLOATING") { saveCfg();openOverlay() },LinearLayout.LayoutParams(0,-2,1f));root.addView(actions);root.addView(btn("TUTUP FLOATING"){stopService(Intent(this,OverlayService::class.java))})
  status=txt("Menghubungkan ke DEX Screener…",12f);root.addView(status);val scroll=ScrollView(this);list=LinearLayout(this).apply{orientation=1};scroll.addView(list);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f));root.addView(txt("Peringatan: skor adalah heuristik aktivitas pasar, bukan AI prediktif, audit kontrak, atau jaminan token aman.",10f));setContentView(root)
  if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),9)
  scan(false)
 }
 private fun field(value:String)=EditText(this).apply{setText(value);setTextColor(Color.WHITE);setHintTextColor(Color.GRAY);inputType=InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL;setSingleLine(true)}
 private fun btn(s:String,act:()->Unit)=Button(this).apply{text=s;isAllCaps=false;textSize=12f;setOnClickListener{act()}}
 private fun saveCfg(){ScanConfig(ids[chain.selectedItemPosition],liq.text.toString().toDoubleOrNull()?:5000.0,vol.text.toString().toDoubleOrNull()?:1000.0,ageVals[age.selectedItemPosition],scoreVals[score.selectedItemPosition]).also{it.save(this)}}
 private fun openOverlay(){if(!Settings.canDrawOverlays(this)){startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:$packageName")));Toast.makeText(this,"Izinkan tampil di atas aplikasi lain, lalu tekan FLOATING lagi",Toast.LENGTH_LONG).show();return};try{startForegroundService(Intent(this,OverlayService::class.java))}catch(e:Exception){Toast.makeText(this,e.message,Toast.LENGTH_LONG).show()}}
 private fun scan(force:Boolean){status.text="Memindai…";Scanner.scan(ScanConfig.load(this),force){r->list.removeAllViews();status.text="${r.coins.size} token lolos • ${r.profiles} profil • ${r.pairs} pair • ${SimpleDateFormat("HH:mm:ss",Locale.getDefault()).format(Date(r.timestamp))}\n${r.error?:"Urut berdasarkan skor"}";r.coins.forEach{coin->val card=TextView(this).apply{text="${coin.symbol}  ${coin.score}/100\n${coin.name}\nLiq $${"%,.0f".format(Locale.US,coin.liquidity)} • Vol 1j $${"%,.0f".format(Locale.US,coin.volume1h)} • Vol 24j $${"%,.0f".format(Locale.US,coin.volume24h)}\nBuy/Sell ${coin.buys1h}/${coin.sells1h} • 5m ${"%+.1f".format(coin.change5m)}% • 1j ${"%+.1f".format(coin.change1h)}%\nFDV ${coin.fdv?.let{"$"+"%,.0f".format(Locale.US,it)}?:"-"} • Usia ${coin.ageHours?.let{"%.1f jam".format(Locale.US,it)}?:"?"}\n${coin.risk}\n${coin.reasons.joinToString(" • ")}\nTekan lama untuk salin alamat";textSize=12f;setTextColor(Color.WHITE);setPadding(dp(12),dp(10),dp(12),dp(10));background=GradientDrawable().apply{setColor(Color.rgb(28,42,62));cornerRadius=dp(12).toFloat()};setOnClickListener{if(coin.url.startsWith("https://dexscreener.com/"))startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(coin.url)))};setOnLongClickListener{val clip=getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager;clip.setPrimaryClip(android.content.ClipData.newPlainText("Token address",coin.address));Toast.makeText(this@MainActivity,"Alamat token disalin",Toast.LENGTH_SHORT).show();true}};list.addView(card,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(8)})}}
 }
}
