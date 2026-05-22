package com.app.wordlearn.presentation.analytics

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.app.wordlearn.domain.model.AnalyticsData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Analiz raporunu Android Print Framework ile kağıda basar veya PDF olarak kaydeder.
 *
 * Akış:
 *  1. AnalyticsData → HTML string (basit, tablo + progress bar)
 *  2. Headless WebView'a yükle
 *  3. Sayfa hazır olunca PrintManager'a PrintDocumentAdapter olarak ver
 *  4. Kullanıcı sistemin "Print" diyaloğunda "Save as PDF" veya gerçek yazıcı seçebilir
 *
 * Spec gereksinimi: "Bu rapor istendiğinde kağıt üzerinden çıktı alınabilsin"
 */
object AnalyticsPrintHelper {

    fun print(context: Context, data: AnalyticsData) {
        val html = buildHtml(data)
        // WebView'i context bağlayarak oluştur — referansı tut, GC süpürmesin diye field'da.
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                createPrintJob(context, view)
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null)
    }

    private fun createPrintJob(context: Context, webView: WebView) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val jobName = "WordLearn Analiz Raporu - ${todayLabel()}"
        val adapter = webView.createPrintDocumentAdapter(jobName)
        printManager.print(
            jobName,
            adapter,
            PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .build()
        )
    }

    private fun buildHtml(data: AnalyticsData): String {
        val date = todayLabel()
        val avgSuccess = "%.1f".format(data.averageSuccess)
        val catRows = data.categorySuccessRates.entries
            .sortedByDescending { it.value }
            .joinToString("") { (cat, rate) -> progressRow(cat, rate) }
        val lvlRows = data.levelSuccessRates.entries
            .sortedByDescending { it.value }
            .joinToString("") { (lvl, rate) -> progressRow(lvl, rate) }

        return """
            <html>
            <head><meta charset="utf-8"><style>
              body { font-family: sans-serif; padding: 24px; color: #222; }
              h1 { color: #4A90E2; margin-bottom: 4px; }
              .subtitle { color: #888; font-size: 12px; margin-bottom: 24px; }
              .stat-grid { display: flex; gap: 16px; margin-bottom: 24px; flex-wrap: wrap; }
              .stat-card { flex: 1; min-width: 140px; padding: 12px; border: 1px solid #ddd;
                            border-radius: 8px; text-align: center; }
              .stat-value { font-size: 22px; font-weight: bold; color: #4A90E2; }
              .stat-label { font-size: 11px; color: #777; margin-top: 4px; }
              h2 { color: #333; border-bottom: 2px solid #4A90E2; padding-bottom: 4px; margin-top: 24px; }
              .row { margin: 8px 0; }
              .row-header { display: flex; justify-content: space-between; font-size: 13px; }
              .bar-bg { background: #eee; height: 8px; border-radius: 4px; margin-top: 4px; }
              .bar-fill { background: #4A90E2; height: 8px; border-radius: 4px; }
              .empty { color: #999; font-style: italic; }
              .footer { margin-top: 32px; font-size: 10px; color: #aaa; text-align: center; }
            </style></head>
            <body>
              <h1>📊 WordLearn Analiz Raporu</h1>
              <div class="subtitle">Oluşturuldu: $date</div>

              <div class="stat-grid">
                <div class="stat-card">
                  <div class="stat-value">${data.totalLearnedWords}</div>
                  <div class="stat-label">Öğrenilen Kelime</div>
                </div>
                <div class="stat-card">
                  <div class="stat-value">${data.totalQuestions}</div>
                  <div class="stat-label">Toplam Soru</div>
                </div>
                <div class="stat-card">
                  <div class="stat-value">$avgSuccess%</div>
                  <div class="stat-label">Ortalama Başarı</div>
                </div>
                <div class="stat-card">
                  <div class="stat-value">${data.longestStreak}</div>
                  <div class="stat-label">En Uzun Seri</div>
                </div>
              </div>

              <h2>Kategori Bazlı Başarı</h2>
              ${if (data.categorySuccessRates.isEmpty()) "<div class='empty'>Henüz veri yok.</div>" else catRows}

              <h2>Seviye Bazlı Başarı</h2>
              ${if (data.levelSuccessRates.isEmpty()) "<div class='empty'>Henüz veri yok.</div>" else lvlRows}

              <div class="footer">WordLearn — kendi tempon, kendi kelimelerin, kendi cihazın.</div>
            </body>
            </html>
        """.trimIndent()
    }

    private fun progressRow(label: String, rate: Float): String {
        val pct = rate.coerceIn(0f, 100f)
        return """
            <div class="row">
              <div class="row-header"><span>$label</span><span><b>${"%.0f".format(pct)}%</b></span></div>
              <div class="bar-bg"><div class="bar-fill" style="width:${pct}%"></div></div>
            </div>
        """.trimIndent()
    }

    private fun todayLabel(): String =
        SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("tr", "TR")).format(Date())
}
