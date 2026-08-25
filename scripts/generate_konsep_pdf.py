import os
import sys
from reportlab.lib.pagesizes import A4
from reportlab.lib import colors
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak, KeepTogether, HRFlowable
)
from reportlab.pdfgen import canvas

class NumberedCanvas(canvas.Canvas):
    """Canvas yang mencatat total halaman untuk membuat 'Halaman X dari Y' dan header/footer rapi."""
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self._saved_page_states = []

    def showPage(self):
        self._saved_page_states.append(dict(self.__dict__))
        self._startPage()

    def save(self):
        num_pages = len(self._saved_page_states)
        for state in self._saved_page_states:
            self.__dict__.update(state)
            self.draw_page_decorations(num_pages)
            super().showPage()
        super().save()

    def draw_page_decorations(self, page_count):
        self.saveState()
        self.setFont("Helvetica", 8)
        self.setFillColor(colors.HexColor("#64748B"))
        
        # Header di halaman 2 dst
        if self._pageNumber > 1:
            self.drawString(54, 804, "NaikApa — Sistem Rekomendasi & Algoritma Skoring Rute Multimodal")
            self.setStrokeColor(colors.HexColor("#CBD5E1"))
            self.setLineWidth(0.75)
            self.line(54, 796, 541, 796)
        
        # Footer di semua halaman
        self.setStrokeColor(colors.HexColor("#CBD5E1"))
        self.setLineWidth(0.75)
        self.line(54, 42, 541, 42)
        
        self.drawString(54, 30, "Dokumentasi Konsep Teknis & Keputusan Rekomendasi • NaikApa Android")
        page_text = f"Halaman {self._pageNumber} dari {page_count}"
        self.drawRightString(541, 30, page_text)
        self.restoreState()

def build_pdf(filename):
    os.makedirs(os.path.dirname(filename), exist_ok=True)
    doc = SimpleDocTemplate(
        filename,
        pagesize=A4,
        leftMargin=54,
        rightMargin=54,
        topMargin=48,
        bottomMargin=48
    )

    styles = getSampleStyleSheet()
    
    # Custom Palette
    C_PRIMARY = colors.HexColor("#1E3A8A")    # Deep Blue
    C_SECONDARY = colors.HexColor("#0284C7")  # Sky/Cyan
    C_TEXT = colors.HexColor("#1E293B")       # Slate Dark
    C_MUTED = colors.HexColor("#64748B")      # Slate Gray
    C_BG_LIGHT = colors.HexColor("#F8FAFC")   # Soft Off-White
    C_BG_BOX = colors.HexColor("#EFF6FF")     # Soft Blue Light
    C_BORDER = colors.HexColor("#CBD5E1")     # Border Gray
    C_GREEN = colors.HexColor("#059669")
    C_AMBER = colors.HexColor("#D97706")

    # Typography Styles
    title_style = ParagraphStyle(
        'DocTitle',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=20,
        leading=24,
        textColor=C_PRIMARY,
        spaceAfter=4
    )
    
    subtitle_style = ParagraphStyle(
        'DocSubTitle',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=10.5,
        leading=14.5,
        textColor=C_MUTED,
        spaceAfter=10
    )

    h1_style = ParagraphStyle(
        'Heading1_Custom',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=12.5,
        leading=16,
        textColor=C_PRIMARY,
        spaceBefore=10,
        spaceAfter=5,
        keepWithNext=True
    )

    h2_style = ParagraphStyle(
        'Heading2_Custom',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=10,
        leading=13.5,
        textColor=C_SECONDARY,
        spaceBefore=7,
        spaceAfter=3,
        keepWithNext=True
    )

    body_style = ParagraphStyle(
        'Body_Custom',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=8.5,
        leading=12.5,
        textColor=C_TEXT,
        spaceAfter=4
    )

    bullet_style = ParagraphStyle(
        'Bullet_Custom',
        parent=body_style,
        leftIndent=12,
        firstLineIndent=-8,
        spaceAfter=3
    )

    formula_box_style = ParagraphStyle(
        'Formula_Box',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=9,
        leading=13,
        textColor=C_PRIMARY
    )

    formula_desc_style = ParagraphStyle(
        'Formula_Desc',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=8,
        leading=11.5,
        textColor=C_MUTED
    )

    table_header_style = ParagraphStyle(
        'TableHeader',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=8.5,
        leading=10.5,
        textColor=colors.white,
        alignment=1
    )

    table_cell_style = ParagraphStyle(
        'TableCell',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=8,
        leading=10.5,
        textColor=C_TEXT
    )

    table_cell_center = ParagraphStyle(
        'TableCellCenter',
        parent=table_cell_style,
        alignment=1
    )

    table_cell_bold = ParagraphStyle(
        'TableCellBold',
        parent=table_cell_style,
        fontName='Helvetica-Bold'
    )

    callout_text = ParagraphStyle(
        'CalloutText',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=8.5,
        leading=12.5,
        textColor=C_TEXT
    )

    story = []

    # ── HEADER ───────────────────────────────────────────────────────────────
    story.append(Paragraph("Sistem Rekomendasi & Algoritma Skoring Rute", title_style))
    story.append(Paragraph("Panduan Lengkap Alur Pengambilan Keputusan, Perhitungan Biaya, Waktu, dan Formula Skoring Multi-Kriteria (0–100) pada Aplikasi <b>NaikApa</b>", subtitle_style))
    story.append(HRFlowable(width="100%", thickness=1.5, color=C_SECONDARY, spaceBefore=0, spaceAfter=8))

    # ── RINGKASAN EKSEKUTIF BOX ──────────────────────────────────────────────
    summary_html = """
    <b>🎯 Inti Konsep:</b> NaikApa menerapkan <b>Multi-Criteria Decision Making (MCDM)</b> yang memadukan <b>Dijkstra Multimodal</b>, <b>TomTom Routing API</b>, dan <b>Dynamic Normalization Scoring Engine</b>. Sistem secara objektif mengkalkulasi dan membandingkan semua kemungkinan perjalanan (Kendaraan Pribadi, Angkutan Umum, dan Park & Ride) ke dalam skor <b>0–100</b> berdasarkan prioritas pengguna serta laporan gangguan lalu lintas real-time.
    """
    summary_table = Table([[Paragraph(summary_html, callout_text)]], colWidths=[487])
    summary_table.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, -1), C_BG_BOX),
        ('BOX', (0, 0), (-1, -1), 1, C_SECONDARY),
        ('PADDING', (0, 0), (-1, -1), 6),
        ('ROUNDEDCORNERS', [4, 4, 4, 4]),
    ]))
    story.append(summary_table)
    story.append(Spacer(1, 6))

    # ── 1. ALUR 5 TAHAPAN ────────────────────────────────────────────────────
    story.append(Paragraph("1. Alur 5 Tahap Pengambilan Keputusan Rekomendasi", h1_style))
    story.append(Paragraph(
        "Ketika pengguna menekan tombol <b>'Temukan Rekomendasi'</b> di Home, kelas <code>RecommendationEngine</code> menjalankan 5 tahapan berurutan:",
        body_style
    ))

    flow_data = [
        [
            Paragraph("<b>No</b>", table_header_style),
            Paragraph("<b>Nama Proses</b>", table_header_style),
            Paragraph("<b>Komponen Sistem</b>", table_header_style),
            Paragraph("<b>Keluaran & Fungsi (Output)</b>", table_header_style)
        ],
        [
            Paragraph("<b>1</b>", table_cell_center),
            Paragraph("<b>Pencarian Kandidat Rute Paralel</b>", table_cell_bold),
            Paragraph("<code>RecommendationEngine</code><br/>(Kotlin Coroutines <code>async</code>)", table_cell_style),
            Paragraph("Kandidat: Transit Murni, Kendaraan Pribadi (Motor/Mobil), dan Rute Gabungan (Park & Ride).", table_cell_style)
        ],
        [
            Paragraph("<b>2</b>", table_cell_center),
            Paragraph("<b>Kalkulasi Metrik Riil</b>", table_cell_bold),
            Paragraph("<code>FareCalculator</code>, <code>FuelCostCalculator</code>, <code>TomTomRouting</code>", table_cell_style),
            Paragraph("Total Durasi (detik), Jarak Tempuh (meter), Jarak Jalan Kaki (meter), Biaya (Rp), Jumlah Transit.", table_cell_style)
        ],
        [
            Paragraph("<b>3</b>", table_cell_center),
            Paragraph("<b>Normalisasi & Pembobotan</b>", table_cell_bold),
            Paragraph("<code>RecommendationScorer</code>", table_cell_style),
            Paragraph("Skor Mentah (0–100) per rute berdasarkan matriks bobot preferensi (Tercepat, Terhemat, Min Jalan, Min Transit).", table_cell_style)
        ],
        [
            Paragraph("<b>4</b>", table_cell_center),
            Paragraph("<b>Evaluasi Gangguan Real-Time</b>", table_cell_bold),
            Paragraph("<code>DisruptionReportDao</code> / Backend NeonDB", table_cell_style),
            Paragraph("Pengurangan penalti (-15 poin) jika rute transit melewati titik halte/stasiun yang sedang bermasalah.", table_cell_style)
        ],
        [
            Paragraph("<b>5</b>", table_cell_center),
            Paragraph("<b>Perangkingan & Pembuatan Alasan</b>", table_cell_bold),
            Paragraph("<code>RecommendationReasonBuilder</code>", table_cell_style),
            Paragraph("1 <b>Rekomendasi Utama</b> + maks. 2 <b>Alternatif</b> lengkap dengan kalimat alasan dalam Bahasa Indonesia.", table_cell_style)
        ]
    ]

    t_flow = Table(flow_data, colWidths=[25, 125, 140, 197])
    t_flow.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), C_PRIMARY),
        ('GRID', (0, 0), (-1, -1), 0.5, C_BORDER),
        ('VALIGN', (0, 0), (-1, -1), 'TOP'),
        ('ROWBACKGROUNDS', (0, 1), (-1, -1), [colors.white, C_BG_LIGHT]),
        ('PADDING', (0, 0), (-1, -1), 3.5),
    ]))
    story.append(t_flow)
    story.append(Spacer(1, 6))

    # ── 2. PEMBENTUKAN KANDIDAT RUTE MULTIMODAL ──────────────────────────────
    story.append(Paragraph("2. Pembentukan Kandidat Rute & Strategi Multimodal", h1_style))
    story.append(Paragraph("• <b>Transit Murni (Dijkstra):</b> Menghitung rute terpendek di graf GTFS (TransJakarta, KRL, MRT, LRT) termasuk transfer jalan kaki antarmoda (&le; 350 m).", bullet_style))
    story.append(Paragraph("• <b>Kendaraan Pribadi (TomTom Routing):</b> Menghitung rute Motor dan Mobil dengan lalu lintas langsung (traffic live) dan opsi tanpa jalan tol.", bullet_style))
    story.append(Paragraph("• <b>Rute Gabungan Park & Ride (Combined):</b> Menemukan hingga 3 halte asal terdekat (radius &le; 8 km) dan 3 halte tujuan terdekat (radius &le; 2.5 km), lalu menguji hingga <b>9 kombinasi rute</b> (First-Mile Kendaraan &rarr; Segmen Transit Antarmoda &rarr; Last-Mile Jalan Kaki).", bullet_style))
    story.append(Spacer(1, 6))

    # ── 3. FORMULA BIAYA & WAKTU ─────────────────────────────────────────────
    story.append(Paragraph("3. Formula Perhitungan Biaya & Waktu Tempuh Riil", h1_style))
    story.append(Paragraph("Setiap kandidat rute dihitung biaya dan waktu riilnya menggunakan formula pasti:", body_style))

    cost_data = [
        [
            Paragraph("<b>Moda Transportasi</b>", table_header_style),
            Paragraph("<b>Rumus / Logika Perhitungan Tarif</b>", table_header_style),
            Paragraph("<b>Contoh Nilai Riil</b>", table_header_style)
        ],
        [
            Paragraph("<b>TransJakarta (BRT)</b>", table_cell_bold),
            Paragraph("Flat (tetap) <b>Rp 3.500</b> tanpa memandang jarak atau jumlah transit koridor.", table_cell_style),
            Paragraph("Rp 3.500", table_cell_center)
        ],
        [
            Paragraph("<b>KRL Commuter Line</b>", table_cell_bold),
            Paragraph("<b>Rp 3.000</b> (&le; 25 km) + <b>Rp 1.000</b> per 10 km tambahan:<br/><code>Tarif = 3000 + ceil((jarak_km - 25) / 10) * 1000</code>", table_cell_style),
            Paragraph("32 km &rarr; Rp 4.000<br/>48 km &rarr; Rp 6.000", table_cell_center)
        ],
        [
            Paragraph("<b>MRT Jakarta</b>", table_cell_bold),
            Paragraph("<b>Rp 3.000</b> base + <b>Rp 1.000</b> per stasiun dilewati (Maks. Rp 14.000):<br/><code>Tarif = min(3000 + (jumlah_stasiun - 1) * 1000, 14000)</code>", table_cell_style),
            Paragraph("5 stasiun &rarr; Rp 7.000<br/>13 stasiun &rarr; Rp 14.000", table_cell_center)
        ],
        [
            Paragraph("<b>LRT Jakarta</b>", table_cell_bold),
            Paragraph("Flat <b>Rp 5.000</b> (Rute Kelapa Gading - Velodrome).", table_cell_style),
            Paragraph("Rp 5.000", table_cell_center)
        ],
        [
            Paragraph("<b>LRT Jabodebek</b>", table_cell_bold),
            Paragraph("<b>Rp 5.000</b> base (1 km) + <b>Rp 700/km</b> tambahan (Maks. Rp 20.000):<br/><code>Tarif = min(5000 + round((jarak_km - 1) * 700), 20000)</code>", table_cell_style),
            Paragraph("15 km &rarr; Rp 14.800<br/>30 km &rarr; Rp 20.000", table_cell_center)
        ],
        [
            Paragraph("<b>BBM Sepeda Motor</b>", table_cell_bold),
            Paragraph("Konsumsi: <b>40 km / Liter</b> | Harga BBM: <b>Rp 10.000 / Liter</b><br/><code>Biaya BBM = (Jarak_km / 40.0) * 10.000</code>", table_cell_style),
            Paragraph("12 km &rarr; Rp 3.000<br/>24 km &rarr; Rp 6.000", table_cell_center)
        ],
        [
            Paragraph("<b>BBM Mobil Pribadi</b>", table_cell_bold),
            Paragraph("Konsumsi: <b>12 km / Liter</b> | Harga BBM: <b>Rp 10.000 / Liter</b><br/><code>Biaya BBM = (Jarak_km / 12.0) * 10.000</code>", table_cell_style),
            Paragraph("12 km &rarr; Rp 10.000<br/>24 km &rarr; Rp 20.000", table_cell_center)
        ],
        [
            Paragraph("<b>Jalan Kaki (Waktu)</b>", table_cell_bold),
            Paragraph("Kecepatan <b>4.0 km/jam</b> atau <b>0.9 detik per meter</b>:<br/><code>Durasi_detik = Jarak_jalan_kaki_meter * 0.9</code>", table_cell_style),
            Paragraph("500 m &rarr; 450 detik (7.5 mnt)", table_cell_center)
        ]
    ]

    t_cost = Table(cost_data, colWidths=[105, 277, 105])
    t_cost.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), C_PRIMARY),
        ('GRID', (0, 0), (-1, -1), 0.5, C_BORDER),
        ('VALIGN', (0, 0), (-1, -1), 'TOP'),
        ('ROWBACKGROUNDS', (0, 1), (-1, -1), [colors.white, C_BG_LIGHT]),
        ('PADDING', (0, 0), (-1, -1), 3.5),
    ]))
    story.append(t_cost)
    story.append(Spacer(1, 8))

    # ── 4. SISTEM SKORING MULTIKRITERIA ──────────────────────────────────────
    story.append(Paragraph("4. Algoritma Skoring Multi-Kriteria (0–100)", h1_style))
    story.append(Paragraph(
        "Skoring menggunakan <b>Normalisasi Terbalik (Inverse Normalization)</b>. Nilai aktual yang makin kecil (waktu makin singkat, biaya makin murah, jalan kaki makin sedikit) menghasilkan nilai performa makin tinggi mendekati <b>1.0</b>:",
        body_style
    ))

    # Formula Box
    f_box = [
        [
            Paragraph("<b>📐 Rumus Normalisasi Terbalik Dimensi (Skala 0.0 – 1.0):</b><br/>"
                      "<code>Nilai_Dimensi = max(0.0, 1.0 - (Nilai_Aktual / Maksimum_Referensi))</code>", formula_box_style)
        ],
        [
            Paragraph("<b>Konstanta Maksimum Referensi:</b> "
                      "• Waktu Max (T<sub>max</sub>) = <b>7.200 dtk (2 Jam)</b> | "
                      "• Biaya Max (C<sub>max</sub>) = <b>Rp 50.000</b> | "
                      "• Jalan Kaki Max (W<sub>max</sub>) = <b>3.000 m (3 km)</b> | "
                      "• Transit Max (K<sub>max</sub>) = <b>5 kali</b>", formula_desc_style)
        ]
    ]
    t_fbox = Table(f_box, colWidths=[487])
    t_fbox.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, -1), C_BG_BOX),
        ('BOX', (0, 0), (-1, -1), 1, C_SECONDARY),
        ('PADDING', (0, 0), (-1, -1), 5),
    ]))
    story.append(t_fbox)
    story.append(Spacer(1, 6))

    story.append(Paragraph("<b>Matriks Bobot Berdasarkan Prioritas Pengguna:</b>", h2_style))
    weights_data = [
        [
            Paragraph("<b>Preferensi Pengguna</b>", table_header_style),
            Paragraph("<b>Bobot Waktu (W<sub>T</sub>)</b>", table_header_style),
            Paragraph("<b>Bobot Biaya (W<sub>C</sub>)</b>", table_header_style),
            Paragraph("<b>Bobot Jalan (W<sub>W</sub>)</b>", table_header_style),
            Paragraph("<b>Bobot Transit (W<sub>K</sub>)</b>", table_header_style),
            Paragraph("<b>Fokus Utama Rekomendasi</b>", table_header_style)
        ],
        [
            Paragraph("⚡ <b>Tercepat (Fastest)</b>", table_cell_bold),
            Paragraph("<b>45%</b>", table_cell_center),
            Paragraph("15%", table_cell_center),
            Paragraph("15%", table_cell_center),
            Paragraph("15%", table_cell_center),
            Paragraph("Waktu tempuh paling singkat", table_cell_style)
        ],
        [
            Paragraph("💰 <b>Terhemat (Cheapest)</b>", table_cell_bold),
            Paragraph("20%", table_cell_center),
            Paragraph("<b>45%</b>", table_cell_center),
            Paragraph("15%", table_cell_center),
            Paragraph("10%", table_cell_center),
            Paragraph("Total ongkos paling murah", table_cell_style)
        ],
        [
            Paragraph("🚶 <b>Jalan Kaki Minimal</b>", table_cell_bold),
            Paragraph("20%", table_cell_center),
            Paragraph("15%", table_cell_center),
            Paragraph("<b>45%</b>", table_cell_center),
            Paragraph("10%", table_cell_center),
            Paragraph("Kenyamanan / kurangi jalan", table_cell_style)
        ],
        [
            Paragraph("🔄 <b>Transit Minimal</b>", table_cell_bold),
            Paragraph("20%", table_cell_center),
            Paragraph("15%", table_cell_center),
            Paragraph("10%", table_cell_center),
            Paragraph("<b>45%</b>", table_cell_center),
            Paragraph("Hindari sering ganti moda", table_cell_style)
        ]
    ]

    t_weights = Table(weights_data, colWidths=[120, 68, 68, 68, 68, 95])
    t_weights.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), C_PRIMARY),
        ('GRID', (0, 0), (-1, -1), 0.5, C_BORDER),
        ('VALIGN', (0, 0), (-1, -1), 'MIDDLE'),
        ('ROWBACKGROUNDS', (0, 1), (-1, -1), [colors.white, C_BG_LIGHT]),
        ('PADDING', (0, 0), (-1, -1), 3.5),
    ]))
    story.append(t_weights)
    story.append(Spacer(1, 6))

    # Formula Skor Akhir & Penalti
    score_calc_box = [
        [
            Paragraph("<b>1. Skor Mentah (Raw Score):</b> "
                      "<code>Skor_Mentah = round( (W_T * Nilai_Waktu) + (W_C * Nilai_Biaya) + (W_W * Nilai_Jalan) + (W_K * Nilai_Transit) )</code><br/>"
                      "<b>2. Penalti Gangguan Aktif:</b> Jika titik rute memiliki laporan gangguan aktif (&le; 1 jam terakhir), penalti = <b>15 Poin</b>.<br/>"
                      "<b>3. Skor Akhir (Clamped):</b> <code>Skor_Akhir = clamp( Skor_Mentah - Penalti_Gangguan, 0, 100 )</code>",
                      body_style)
        ]
    ]
    t_scalc = Table(score_calc_box, colWidths=[487])
    t_scalc.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, -1), C_BG_LIGHT),
        ('BOX', (0, 0), (-1, -1), 1, C_BORDER),
        ('PADDING', (0, 0), (-1, -1), 5),
    ]))
    story.append(t_scalc)
    story.append(Spacer(1, 8))

    # ── 5. STUDI KASUS NYATA ─────────────────────────────────────────────────
    story.append(Paragraph("5. Studi Kasus Nyata: Simulasi Perhitungan Skor", h1_style))
    story.append(Paragraph(
        "<b>Skenario:</b> Perjalanan dari <b>Tebet</b> menuju <b>Bundaran HI</b> pada jam sibuk pagi. Pengguna memilih prioritas <b>⚡ Tercepat (Fastest)</b>.",
        body_style
    ))

    case_data = [
        [
            Paragraph("<b>Parameter Metrik</b>", table_header_style),
            Paragraph("<b>Opsi A: Motor Langsung</b>", table_header_style),
            Paragraph("<b>Opsi B: KRL + MRT</b>", table_header_style),
            Paragraph("<b>Opsi C: Motor ke Stasiun + KRL</b>", table_header_style)
        ],
        [
            Paragraph("<b>Deskripsi Rute</b>", table_cell_bold),
            Paragraph("Motor lewat jalan arteri", table_cell_style),
            Paragraph("Jalan &rarr; KRL Tebet &rarr; Sudirman &rarr; MRT Dukuh Atas &rarr; HI", table_cell_style),
            Paragraph("Motor ke St. Tebet (parkir) &rarr; KRL Sudirman &rarr; Jalan ke HI", table_cell_style)
        ],
        [
            Paragraph("<b>Total Waktu (T)</b>", table_cell_bold),
            Paragraph("25 Menit (1.500 s)", table_cell_center),
            Paragraph("35 Menit (2.100 s)", table_cell_center),
            Paragraph("28 Menit (1.680 s)", table_cell_center)
        ],
        [
            Paragraph("<b>Total Biaya (C)</b>", table_cell_bold),
            Paragraph("Rp 3.000 (BBM 12 km)", table_cell_center),
            Paragraph("Rp 7.000 (KRL 3rb + MRT 4rb)", table_cell_center),
            Paragraph("Rp 5.000 (BBM 2rb + KRL 3rb)", table_cell_center)
        ],
        [
            Paragraph("<b>Jarak Jalan Kaki (W)</b>", table_cell_bold),
            Paragraph("0 meter", table_cell_center),
            Paragraph("450 meter", table_cell_center),
            Paragraph("300 meter", table_cell_center)
        ],
        [
            Paragraph("<b>Jumlah Transit (K)</b>", table_cell_bold),
            Paragraph("0 kali", table_cell_center),
            Paragraph("2 kali (KRL, MRT)", table_cell_center),
            Paragraph("1 kali (KRL)", table_cell_center)
        ],
        [
            Paragraph("<b>Nilai Waktu (S_T)</b>", table_cell_bold),
            Paragraph("1 - (1500/7200) = <b>0.792</b>", table_cell_center),
            Paragraph("1 - (2100/7200) = <b>0.708</b>", table_cell_center),
            Paragraph("1 - (1680/7200) = <b>0.767</b>", table_cell_center)
        ],
        [
            Paragraph("<b>Nilai Biaya (S_C)</b>", table_cell_bold),
            Paragraph("1 - (3000/50000) = <b>0.940</b>", table_cell_center),
            Paragraph("1 - (7000/50000) = <b>0.860</b>", table_cell_center),
            Paragraph("1 - (5000/50000) = <b>0.900</b>", table_cell_center)
        ],
        [
            Paragraph("<b>Nilai Jalan (S_W)</b>", table_cell_bold),
            Paragraph("1 - (0/3000) = <b>1.000</b>", table_cell_center),
            Paragraph("1 - (450/3000) = <b>0.850</b>", table_cell_center),
            Paragraph("1 - (300/3000) = <b>0.900</b>", table_cell_center)
        ],
        [
            Paragraph("<b>Nilai Transit (S_K)</b>", table_cell_bold),
            Paragraph("1 - (0/5) = <b>1.000</b>", table_cell_center),
            Paragraph("1 - (2/5) = <b>0.600</b>", table_cell_center),
            Paragraph("1 - (1/5) = <b>0.800</b>", table_cell_center)
        ],
        [
            Paragraph("<b>Perhitungan Skor Mentah</b><br/><i>(45% T + 15% C + 15% W + 15% K)</i>", table_cell_bold),
            Paragraph("45(0.792) + 15(0.940) + 15(1.0) + 15(1.0)<br/>= 35.64 + 14.10 + 15 + 15<br/>= <b>79.74 &rarr; 80</b>", table_cell_center),
            Paragraph("45(0.708) + 15(0.860) + 15(0.85) + 15(0.6)<br/>= 31.86 + 12.90 + 12.75 + 9.0<br/>= <b>66.51 &rarr; 67</b>", table_cell_center),
            Paragraph("45(0.767) + 15(0.900) + 15(0.90) + 15(0.8)<br/>= 34.52 + 13.50 + 13.50 + 12.0<br/>= <b>73.52 &rarr; 74</b>", table_cell_center)
        ],
        [
            Paragraph("<b>Penalti Gangguan</b>", table_cell_bold),
            Paragraph("0 poin (Lancar)", table_cell_center),
            Paragraph("-15 poin (St. Tebet Padat)", table_cell_center),
            Paragraph("-15 poin (St. Tebet Padat)", table_cell_center)
        ],
        [
            Paragraph("<b>Skor Akhir & Peringkat</b>", table_cell_bold),
            Paragraph("<font color='#059669'><b>80 (Peringkat 1)</b></font><br/><b>Rekomendasi Utama</b>", table_cell_center),
            Paragraph("<font color='#D97706'><b>52 (Peringkat 3)</b></font><br/><b>Alternatif 2</b>", table_cell_center),
            Paragraph("<font color='#0284C7'><b>59 (Peringkat 2)</b></font><br/><b>Alternatif 1</b>", table_cell_center)
        ]
    ]

    t_case = Table(case_data, colWidths=[105, 127, 127, 128])
    t_case.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), C_PRIMARY),
        ('GRID', (0, 0), (-1, -1), 0.5, C_BORDER),
        ('VALIGN', (0, 0), (-1, -1), 'MIDDLE'),
        ('ROWBACKGROUNDS', (0, 1), (-1, -1), [colors.white, C_BG_LIGHT]),
        ('BACKGROUND', (0, -1), (-1, -1), C_BG_BOX),
        ('PADDING', (0, 0), (-1, -1), 3),
    ]))
    story.append(t_case)
    story.append(Spacer(1, 8))

    # ── 6. REASON BUILDER ────────────────────────────────────────────────────
    reasons_wrapper = []
    reasons_wrapper.append(Paragraph("6. Engine Alasan Rekomendasi (Reason Builder)", h1_style))
    reasons_wrapper.append(Paragraph(
        "Modul <code>RecommendationReasonBuilder</code> membandingkan karakteristik setiap opsi secara relatif dan menghasilkan kalimat penjelasan natural:",
        body_style
    ))

    reasons_data = [
        [
            Paragraph("<b>Status Kartu Rute</b>", table_header_style),
            Paragraph("<b>Kondisi Logika Program</b>", table_header_style),
            Paragraph("<b>Contoh Teks Alasan yang Ditampilkan ke User</b>", table_header_style)
        ],
        [
            Paragraph("<b>Rekomendasi Utama</b><br/>(Tercepat)", table_cell_bold),
            Paragraph("Waktu tempuh paling singkat dibanding semua kandidat.", table_cell_style),
            Paragraph("<i>'Rute ini dipilih karena memiliki waktu tempuh paling singkat, sekitar 25 menit.'</i>", table_cell_style)
        ],
        [
            Paragraph("<b>Rekomendasi Utama</b><br/>(Terhemat)", table_cell_bold),
            Paragraph("Total biaya paling rendah dibanding semua kandidat.", table_cell_style),
            Paragraph("<i>'Rute ini dipilih karena memiliki total biaya paling rendah, sekitar Rp 3.500.'</i>", table_cell_style)
        ],
        [
            Paragraph("<b>Alternatif 1 / 2</b><br/>(Lebih Hemat)", table_cell_bold),
            Paragraph("Bukan tercepat, tapi ongkos lebih murah dari rekomendasi utama.", table_cell_style),
            Paragraph("<i>'Alternatif ini lebih hemat biaya dibanding rekomendasi utama, meski sedikit lebih lama.'</i>", table_cell_style)
        ],
        [
            Paragraph("<b>Alternatif 1 / 2</b><br/>(Lebih Cepat)", table_cell_bold),
            Paragraph("Bukan termurah, tapi waktu tempuh lebih cepat dari rekomendasi utama.", table_cell_style),
            Paragraph("<i>'Alternatif ini lebih cepat dibanding rekomendasi utama, meski biayanya sedikit lebih tinggi.'</i>", table_cell_style)
        ],
        [
            Paragraph("<b>Peringatan Gangguan</b><br/>(Warning Badge)", table_cell_bold),
            Paragraph("Rute melewati simpul transit dengan laporan gangguan aktif.", table_cell_style),
            Paragraph("<font color='#B45309'><i>'⚠ Ada laporan gangguan aktif pada salah satu titik perjalanan ini. Rute tetap ditampilkan dengan penalti skor.'</i></font>", table_cell_style)
        ]
    ]

    t_reasons = Table(reasons_data, colWidths=[105, 172, 210])
    t_reasons.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), C_PRIMARY),
        ('GRID', (0, 0), (-1, -1), 0.5, C_BORDER),
        ('VALIGN', (0, 0), (-1, -1), 'TOP'),
        ('ROWBACKGROUNDS', (0, 1), (-1, -1), [colors.white, C_BG_LIGHT]),
        ('PADDING', (0, 0), (-1, -1), 3.5),
    ]))
    reasons_wrapper.append(t_reasons)
    reasons_wrapper.append(Spacer(1, 8))

    # ── 7. KESIMPULAN ────────────────────────────────────────────────────────
    reasons_wrapper.append(Paragraph("7. Keunggulan Arsitektur Rekomendasi NaikApa", h1_style))
    reasons_wrapper.append(Paragraph("1. <b>Objektif & Terukur:</b> Skor 0–100 murni dihasilkan dari normalisasi matematis yang adil antar moda.", bullet_style))
    reasons_wrapper.append(Paragraph("2. <b>Personalisasi Otomatis:</b> Menyesuaikan opsi kendaraan yang dimiliki user (Motor, Mobil, atau Transportasi Umum saja).", bullet_style))
    reasons_wrapper.append(Paragraph("3. <b>Respon Gangguan Real-Time:</b> Otomatis mendemosi rute yang terkena kendala kepadatan atau gangguan teknis.", bullet_style))
    reasons_wrapper.append(Paragraph("4. <b>Transparan & Human-Friendly:</b> Selalu dilengkapi alasan eksplisit dalam Bahasa Indonesia yang mudah dipahami.", bullet_style))

    story.append(KeepTogether(reasons_wrapper))

    doc.build(story, canvasmaker=NumberedCanvas)
    print(f"PDF berhasil diperbarui di: {filename}")

if __name__ == '__main__':
    target_path = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "konsep", "Konsep_Perhitungan_dan_Rekomendasi_Rute_NaikApa.pdf"))
    build_pdf(target_path)
