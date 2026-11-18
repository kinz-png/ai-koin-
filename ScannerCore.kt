package id.eltiy.micinai

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

private val io = Executors.newSingleThreadExecutor()
private val ui = Handler(Looper.getMainLooper())

data class ScanConfig(val chain: String = "solana", val minLiquidity: Double = 5000.0,
    val minVolume: Double = 1000.0, val maxAgeHours: Int = 168, val minScore: Int = 0,
    val maxResults: Int = 30) {
    fun save(c: Context) { c.getSharedPreferences("cfg",0).edit().putString("chain",chain).putString("liq",minLiquidity.toString()).putString("vol",minVolume.toString()).putInt("age",maxAgeHours).putInt("score",minScore).apply() }
    companion object { fun load(c: Context): ScanConfig { val p=c.getSharedPreferences("cfg",0); return ScanConfig(p.getString("chain","solana")?:"solana",p.getString("liq","5000")!!.toDoubleOrNull()?:5000.0,p.getString("vol","1000")!!.toDoubleOrNull()?:1000.0,p.getInt("age",168),p.getInt("score",0)) } }
}
data class Coin(val symbol:String,val name:String,val address:String,val chain:String,val dex:String,val url:String,
    val liquidity:Double,val volume5m:Double,val volume1h:Double,val volume24h:Double,val buys1h:Int,val sells1h:Int,
    val priceUsd:Double,val change5m:Double,val change1h:Double,val change24h:Double,val fdv:Double?,val ageHours:Double?,
    val score:Int,val risk:String,val reasons:List<String>)
data class Scan(val coins:List<Coin>,val error:String?,val timestamp:Long,val profiles:Int,val pairs:Int)

object Scanner {
 private val busy=AtomicBoolean(false); private var cache:Pair<String,Scan>?=null
 private fun request(url:String):String { val c=URL(url).openConnection() as HttpURLConnection; try { c.connectTimeout=9000;c.readTimeout=9000;c.setRequestProperty("Accept","application/json");c.setRequestProperty("User-Agent","MicinAIScreener/0.3");if(c.responseCode !in 200..299) error("API HTTP ${c.responseCode}");return c.inputStream.bufferedReader().use{it.readText()} } finally { c.disconnect() } }
 fun scan(config:ScanConfig,force:Boolean=false,callback:(Scan)->Unit) {
  val key=config.toString(); val old=cache
  if(!force && old?.first==key && System.currentTimeMillis()-old.second.timestamp<30_000){ui.post{callback(old.second)};return}
  if(!busy.compareAndSet(false,true)){ui.post{callback(Scan(emptyList(),"Pemindaian masih berjalan",System.currentTimeMillis(),0,0))};return}
  io.execute {
   val now=System.currentTimeMillis()
   val result=try {
    // DEX Screener profile feed is not a complete new-pair feed.
    val feed=JSONArray(request("https://api.dexscreener.com/token-profiles/latest/v1"));val addresses=linkedSetOf<String>()
    for(i in 0 until feed.length()){val o=feed.optJSONObject(i)?:continue;if(o.optString("chainId")==config.chain){val a=o.optString("tokenAddress");if(a.isNotBlank())addresses.add(a)};if(addresses.size>=30)break}
    if(addresses.isEmpty()) Scan(emptyList(),"Belum ada profil token ${config.chain} di feed saat ini",now,0,0) else {
     val pairs=mutableListOf<JSONObject>();var failures=0
     addresses.toList().chunked(20).forEach { batch -> try { val path=batch.joinToString(",") { java.net.URLEncoder.encode(it,"UTF-8") };val arr=JSONArray(request("https://api.dexscreener.com/tokens/v1/${config.chain}/$path"));for(j in 0 until arr.length())arr.optJSONObject(j)?.let{pairs.add(it)} } catch(_:Exception){failures++} }
     val best=linkedMapOf<String,JSONObject>();pairs.forEach { p -> val a=p.optJSONObject("baseToken")?.optString("address")?:return@forEach;if(a !in addresses)return@forEach;val prev=best[a];val liq=p.optJSONObject("liquidity")?.optDouble("usd",0.0)?:0.0;val prevL=prev?.optJSONObject("liquidity")?.optDouble("usd",-1.0)?:-1.0;if(liq>prevL)best[a]=p }
     val coins=best.mapNotNull { (addr,p) ->
      val l=p.optJSONObject("liquidity")?.optDouble("usd",0.0)?:0.0;val vol=p.optJSONObject("volume");val v5=vol?.optDouble("m5",0.0)?:0.0;val v1=vol?.optDouble("h1",0.0)?:0.0;val v24=vol?.optDouble("h24",0.0)?:0.0
      val tx=p.optJSONObject("txns")?.optJSONObject("h1");val b=tx?.optInt("buys",0)?:0;val s=tx?.optInt("sells",0)?:0
      val created=p.optLong("pairCreatedAt",0);val age=if(created>0)((now-created).coerceAtLeast(0)/3600000.0)else null
      val ch=p.optJSONObject("priceChange");val c5=ch?.optDouble("m5",0.0)?:0.0;val c1=ch?.optDouble("h1",0.0)?:0.0;val c24=ch?.optDouble("h24",0.0)?:0.0
      val fdv=if(p.has("fdv"))p.optDouble("fdv") else null
      val assess=Scoring.score(l,v1,v24,b,s,age,c5,c1,fdv);val base=p.optJSONObject("baseToken")
      Coin(base?.optString("symbol","?")?:"?",base?.optString("name","")?:"",addr,config.chain,p.optString("dexId"),p.optString("url"),l,v5,v1,v24,b,s,p.optString("priceUsd").toDoubleOrNull()?:0.0,c5,c1,c24,fdv,age,assess.first,assess.second,assess.third)
     }.filter { it.liquidity>=config.minLiquidity && it.volume1h>=config.minVolume && it.ageHours!=null && it.ageHours<=config.maxAgeHours && it.score>=config.minScore }
      .sortedWith(compareByDescending<Coin>{it.score}.thenByDescending{it.volume1h}).take(config.maxResults)
     Scan(coins,if(coins.isEmpty())"Tidak ada token lolos filter. Longgarkan filter." else null,now,addresses.size,pairs.size)
    }
   } catch(e:Exception){Scan(emptyList(),"Gagal mengambil data: ${e.message}",now,0,0)}
   busy.set(false);if(result.error==null)cache=key to result;ui.post{callback(result)}
  }
 }
}
object Scoring {
 fun score(liq:Double,vol1:Double,vol24:Double,buys:Int,sells:Int,age:Double?,c5:Double,c1:Double,fdv:Double?):Triple<Int,String,List<String>> {
  var score=0
  val reasons=mutableListOf<String>()
  score+=when{liq>=100000->25;liq>=25000->19;liq>=10000->13;liq>=5000->7;else->0}
  if(liq<10000) reasons.add("Likuiditas tipis")
  score+=when{vol1>=100000->20;vol1>=20000->15;vol1>=5000->10;vol1>=1000->5;else->0}
  if(vol1<1000) reasons.add("Volume rendah")
  val n=buys+sells
  val buyRatio=if(n>0)buys.toDouble()/n else 0.0
  score+=when{n>=30&&buyRatio>=.65->20;n>=10&&buyRatio>=.55->12;else->0}
  if(n<10) reasons.add("Transaksi sedikit")
  if(n>=10&&buyRatio<.35) { score-=12; reasons.add("Tekanan jual tinggi (${(100*(1-buyRatio)).toInt()}%)") }
  score+=when{age==null->0;age in 1.0..72.0->15;age<1->5;else->10}
  if(age==null) reasons.add("Usia pair tidak diketahui")
  score+=when{c5 in 0.5..8.0 && c1 in 0.0..35.0->10;c1>100->0;else->0}
  if(c1>100) { score-=8; reasons.add("Lonjakan 1j ekstrem") }
  if(c5 < -20 || c1 < -40) { score-=10; reasons.add("Penurunan harga tajam") }
  if(vol1>0 && liq>0 && vol1/liq>5) { score-=8; reasons.add("Volume 1j >5x likuiditas") }
  if(vol24>0 && vol1/vol24>.8) reasons.add("Volume 1j sangat dominan dibanding 24j")
  if(fdv!=null && fdv>0 && liq/fdv<.01) { score-=5; reasons.add("Rasio likuiditas/FDV sangat rendah") }
  val risk=when {
   liq<5000||n<5||age==null -> "DATA TERBATAS"
   (n>=10&&buyRatio<.35)||c1>100||c1 < -40|| (liq>0&&vol1/liq>5) -> "WASPADA TINGGI"
   liq<15000||age<1||(fdv!=null&&fdv>0&&liq/fdv<.01) -> "WASPADA"
   else -> "BELUM DIAUDIT"
  }
  if(reasons.isEmpty()) reasons.add("Indikator pasar dasar terpenuhi")
  return Triple(score.coerceIn(0,100),risk,reasons)
 }
}
