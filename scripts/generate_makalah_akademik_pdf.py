import os
import sys
from reportlab.lib.pagesizes import A4
from reportlab.lib import colors
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak, KeepTogether, HRFlowable
)
from reportlab.pdfgen import canvas

class AcademicNumberedCanvas(canvas.Canvas):
    """Canvas untuk dokumen akademik formal dengan running header dan page number X dari Y."""
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
            self.drawString(54, 804, "Kajian Ilmiah & Rekayasa Sistem • Rancang Bangun Platform Rekomendasi Rute Multimodal NaikApa")
            self.setStrokeColor(colors.HexColor("#CBD5E1"))
            self.setLineWidth(0.75)
            self.line(54, 796, 541, 796)
        
        # Footer di semua halaman
        self.setStrokeColor(colors.HexColor("#CBD5E1"))
        self.setLineWidth(0.75)
        self.line(54, 42, 541, 42)
        
        self.drawString(54, 30, "NaikApa: Multi-Modal Intelligent Transit & Routing Recommendation Engine (Jabodetabek)")
        page_text = f"Halaman {self._pageNumber} dari {page_count}"
        self.drawRightString(541, 30, page_text)
        self.restoreState()

def build_academic_pdf(filename):
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
    
    # Academic Color Palette
    C_PRIMARY = colors.HexColor("#0F172A")    # Slate 900
    C_NAVY = colors.HexColor("#1E3A8A")       # Blue 900
    C_ACCENT = colors.HexColor("#0284C7")     # Sky 600
    C_TEXT = colors.HexColor("#1E293B")       # Slate 800
    C_MUTED = colors.HexColor("#64748B")      # Slate 500
    C_BG_LIGHT = colors.HexColor("#F8FAFC")   # Slate 50
    C_BG_BOX = colors.HexColor("#F1F5F9")     # Slate 100
    C_BORDER = colors.HexColor("#CBD5E1")     # Slate 300

    # Typography Styles
    paper_title_style = ParagraphStyle(
        'PaperTitle',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=16,
        leading=21,
        textColor=C_PRIMARY,
        alignment=1, # Center
        spaceAfter=5
    )
    
    paper_subtitle_style = ParagraphStyle(
        'PaperSubTitle',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=9,
        leading=13,
        textColor=C_MUTED,
        alignment=1,
        spaceAfter=10
    )

    h1_style = ParagraphStyle(
        'Heading1_Academic',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=11,
        leading=14.5,
        textColor=C_NAVY,
        spaceBefore=9,
        spaceAfter=3.5,
        keepWithNext=True
    )

    h2_style = ParagraphStyle(
        'Heading2_Academic',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=9.5,
        leading=12.5,
        textColor=C_ACCENT,
        spaceBefore=6,
        spaceAfter=2.5,
        keepWithNext=True
    )

    body_style = ParagraphStyle(
        'Body_Academic',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=8.2,
        leading=12,
        textColor=C_TEXT,
        alignment=4, # Justified
        spaceAfter=3.5
    )

    abstract_style = ParagraphStyle(
        'Abstract_Text',
        parent=styles['Normal'],
        fontName='Helvetica-Oblique',
        fontSize=7.8,
        leading=11.2,
        textColor=C_TEXT,
        alignment=4
    )

    bullet_style = ParagraphStyle(
        'Bullet_Academic',
        parent=body_style,
        leftIndent=12,
        firstLineIndent=-8,
        alignment=4,
        spaceAfter=2.5
    )

    table_header_style = ParagraphStyle(
        'TableHeader_Academic',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=7.8,
        leading=10,
        textColor=colors.white,
        alignment=1
    )

    table_cell_style = ParagraphStyle(
        'TableCell_Academic',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=7.5,
        leading=9.8,
        textColor=C_TEXT
    )

    table_cell_center = ParagraphStyle(
        'TableCellCenter_Academic',
        parent=table_cell_style,
        alignment=1
    )

    table_cell_bold = ParagraphStyle(
        'TableCellBold_Academic',
        parent=table_cell_style,
        fontName='Helvetica-Bold'
    )

    formula_style = ParagraphStyle(
        'Formula_Academic',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=8,
        leading=11.5,
        textColor=C_NAVY
    )

    story = []

    # ── JUDUL ARTIKEL ILMIAH ──────────────────────────────────────────────────
    story.append(Paragraph("Rancang Bangun Sistem Rekomendasi Rute Multimodal Cerdas Terintegrasi Berbasis Multi-Criteria Decision Making (MCDM) dan Crowdsourced Sensing untuk Mobilitas Perkotaan Jabodetabek", paper_title_style))
    story.append(Paragraph("<b>Kajian Akademik:</b> Analisis Latar Belakang, Identifikasi Masalah, Urgensi Sosial-Ekonomi, Tinjauan Teoretis, Arsitektur Solusi, dan Dampak Keberlanjutan Transportasi", paper_subtitle_style))
    story.append(HRFlowable(width="100%", thickness=1.5, color=C_NAVY, spaceBefore=0, spaceAfter=6))

    # ── ABSTRAK ──────────────────────────────────────────────────────────────
    abstract_content = """
    <b>ABSTRAK:</b> Kawasan megapolitan Jakarta, Bogor, Depok, Tangerang, dan Bekasi (Jabodetabek) menghadapi tantangan kemacetan struktural dengan kerugian ekonomi mencapai puluhan triliun rupiah per tahun dan kontribusi polusi udara yang masif. Kendati jaringan transportasi massal (TransJakarta, KRL Commuter Line, MRT Jakarta, dan LRT) terus berkembang, tingkat peralihan masyarakat dari kendaraan pribadi masih terhambat oleh masalah klasik <i>First-Mile / Last-Mile connectivity gap</i>, fragmentasi informasi jadwal dan tarif antarmoda, serta ketiadaan sistem pendukung keputusan yang mampu membandingkan opsi multimodal secara holistik. Dokumen ini menyajikan kajian akademis dan arsitektur teknis <b>NaikApa</b>, sebuah platform rekomendasi perjalanan cerdas berbasis Android yang mengintegrasikan algoritma graf <b>Dijkstra Multimodal</b> (di atas data standar <i>General Transit Feed Specification</i> / GTFS), perutean dinamis kendaraan pribadi (TomTom Routing API), model penilaian <b>Multi-Criteria Decision Making (MCDM)</b> dengan teknik normalisasi terbalik (<i>inverse normalization</i>), serta pelaporan gangguan real-time berbasis partisipasi komunitas (<i>crowdsourced sensing</i>). Sistem ini secara transparan mengevaluasi parameter waktu, biaya bahan bakar/tiket, jarak jalan kaki, dan frekuensi transfer, menghasilkan rekomendasi rute utama dan alternatif yang terpersonalisasi serta mampu memitigasi disrupsi jaringan transportasi secara adaptif.
    <br/><br/>
    <b>Kata Kunci:</b> <i>Multimodal Transportation, Route Recommendation Engine, Multi-Criteria Decision Making (MCDM), GTFS, Dijkstra Algorithm, First-Mile Last-Mile, Crowdsourced Sensing, Jabodetabek.</i>
    """
    t_abstract = Table([[Paragraph(abstract_content, abstract_style)]], colWidths=[487])
    t_abstract.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, -1), C_BG_LIGHT),
        ('BOX', (0, 0), (-1, -1), 1, C_BORDER),
        ('PADDING', (0, 0), (-1, -1), 5),
        ('ROUNDEDCORNERS', [3, 3, 3, 3]),
    ]))
    story.append(t_abstract)
    story.append(Spacer(1, 6))

    # ── BAB I: PENDAHULUAN ───────────────────────────────────────────────────
    story.append(Paragraph("1. Pendahuluan & Latar Belakang Masalah", h1_style))
    story.append(Paragraph(
        "Kawasan metropolitan Jabodetabek dihuni oleh lebih dari 30 juta jiwa dengan mobilitas komuter harian melintasi batas administratif mencapai jutaan pergerakan per hari. Berdasarkan data Badan Pusat Statistik (BPS) dan laporan transportasi perkotaan, rasio ketergantungan masyarakat terhadap kendaraan pribadi (sepeda motor dan mobil) masih berada di atas 80%. Tingginya volume kendaraan pribadi ini memicu kemacetan kronis, pemborosan konsumsi energi bahan bakar fosil, peningkatan emisi gas rumah kaca (CO<sub>2</sub>, PM<sub>2.5</sub>), serta kerugian waktu produktif masyarakat.",
        body_style
    ))
    story.append(Paragraph(
        "Di sisi lain, Pemerintah Republik Indonesia dan Pemerintah Provinsi DKI Jakarta telah menginvestasikan sumber daya signifikan dalam membangun infrastruktur angkutan umum massal, seperti Bus Rapid Transit (TransJakarta), Kereta Rel Listrik (KRL Commuter Line), Moda Raya Terpadu (MRT Jakarta), LRT Jakarta, dan LRT Jabodebek. Namun, pemanfaatan sistem angkutan massal tersebut belum optimal karena informasi antarmoda masih terfragmentasi dan belum ada platform cerdas yang mampu mengombinasikan kendaraan pribadi sebagai pengumpan (<i>feeder</i>) menuju stasiun transit terdekat secara otomatis.",
        body_style
    ))
    story.append(Spacer(1, 3))

    # ── BAB II: IDENTIFIKASI MASALAH ─────────────────────────────────────────
    story.append(Paragraph("2. Identifikasi Masalah & Analisis Kesenjangan (Gap Analysis)", h1_style))
    story.append(Paragraph(
        "Berdasarkan observasi empiris perilaku komuter perkotaan dan tinjauan literatur sistem transportasi cerdas (<i>Intelligent Transportation Systems</i>), teridentifikasi lima permasalahan fundamental:",
        body_style
    ))

    problems_data = [
        [
            Paragraph("<b>No</b>", table_header_style),
            Paragraph("<b>Dimensi Masalah</b>", table_header_style),
            Paragraph("<b>Fakta & Fenomena Lapangan</b>", table_header_style),
            Paragraph("<b>Implikasi terhadap Keputusan Komuter</b>", table_header_style)
        ],
        [
            Paragraph("<b>1</b>", table_cell_center),
            Paragraph("<b>First-Mile & Last-Mile Connectivity Gap</b>", table_cell_bold),
            Paragraph("Stasiun/halte transit tidak berada tepat di depan pintu rumah atau kantor komuter. Jarak jangkau pejalan kaki (>1 km) membuat transit massal dianggap tidak praktis.", table_cell_style),
            Paragraph("Masyarakat memilih berkendara pribadi dari titik awal hingga tujuan akhir (<i>end-to-end</i>), menambah beban kemacetan koridor utama.", table_cell_style)
        ],
        [
            Paragraph("<b>2</b>", table_cell_center),
            Paragraph("<b>Fragmentasi Informasi Rute & Tarif (Information Silos)</b>", table_cell_bold),
            Paragraph("Setiap operator (KAI Commuter, MRTJ, TransJakarta, LRT) memiliki skema tarif dan aplikasi terpisah. Tidak ada komparasi transparan antara ongkos BBM vs tarif transit.", table_cell_style),
            Paragraph("Komuter mengalami kesulitan mengestimasi total biaya riil perjalanan gabungan (BBM + Parkir + Tiket Transit).", table_cell_style)
        ],
        [
            Paragraph("<b>3</b>", table_cell_center),
            Paragraph("<b>Ketiadaan Integrasi Rute Kombinasi (Park & Ride)</b>", table_cell_bold),
            Paragraph("Aplikasi navigasi komersial arus utama (seperti Google Maps / Waze) cenderung memisahkan mode berkendara dan mode transit dalam silo terpisah.", table_cell_style),
            Paragraph("Tidak ada rekomendasi otomatis: 'Bawa motor ke Stasiun A, parkir, lanjut KRL ke Stasiun B, jalan kaki ke kantor'.", table_cell_style)
        ],
        [
            Paragraph("<b>4</b>", table_cell_center),
            Paragraph("<b>Disrupsi Dinamis & Keterlambatan Real-Time</b>", table_cell_bold),
            Paragraph("Kondisi kepadatan luar biasa, antrean stasiun, kerusakan sinyal, atau penutupan jalur sering terjadi mendadak dan tidak tercermin pada jadwal statis.", table_cell_style),
            Paragraph("Komuter terjebak di simpul transit yang lumpuh tanpa alternatif pengalihan rute adaptif.", table_cell_style)
        ],
        [
            Paragraph("<b>5</b>", table_cell_center),
            Paragraph("<b>Kelumpuhan Keputusan (Decision Paralysis Multi-Kriteria)</b>", table_cell_bold),
            Paragraph("Setiap individu memiliki batasan preferensi yang berbeda: ada yang mementingkan kecepatan (pekerja), biaya murah (mahasiswa), kenyamanan jalan, atau kemudahan transit.", table_cell_style),
            Paragraph("Sulit menentukan rute optimal tanpa algoritma skoring terbobot yang transparan dan terukur.", table_cell_style)
        ]
    ]

    t_prob = Table(problems_data, colWidths=[24, 114, 175, 174])
    t_prob.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), C_NAVY),
        ('GRID', (0, 0), (-1, -1), 0.5, C_BORDER),
        ('VALIGN', (0, 0), (-1, -1), 'TOP'),
        ('ROWBACKGROUNDS', (0, 1), (-1, -1), [colors.white, C_BG_LIGHT]),
        ('PADDING', (0, 0), (-1, -1), 2.5),
    ]))
    story.append(t_prob)
    story.append(Spacer(1, 5))

    # ── BAB III: URGENSI & SIGNIFIKANSI ───────────────────────────────────────
    story.append(Paragraph("3. Urgensi & Signifikansi Proyek / Penelitian", h1_style))
    story.append(Paragraph("Pengembangan platform <b>NaikApa</b> memiliki signifikansi strategis multidimensi:", body_style))
    story.append(Paragraph("• <b>Urgensi Transportasi Berkelanjutan (SDG 11 & SDG 13):</b> Mendorong peralihan modal (<i>modal shift</i>) dari kendaraan pribadi ke transportasi massal dengan mempermudah adopsi strategi <i>Park and Ride</i>.", bullet_style))
    story.append(Paragraph("• <b>Efisiensi Ekonomi Rumah Tangga Komuter:</b> Memberikan transparansi komparasi pengeluaran harian antara biaya bahan bakar kendaraan pribadi versus tiket angkutan umum.", bullet_style))
    story.append(Paragraph("• <b>Kecerdasan Komputasi Mobilitas (MaaS - Mobility-as-a-Service):</b> Mengimplementasikan integrasi data terbuka (Open Data GTFS) dan algoritma MCDM adaptif dalam perangkat bergerak pengguna akhir (Android native).", bullet_style))
    story.append(Paragraph("• <b>Kekuatan Data Berbasis Komunitas (Crowdsourced Intelligence):</b> Memberdayakan komuter untuk saling berbagi laporan gangguan nyata demi resiliensi jaringan mobilitas kota.", bullet_style))
    story.append(Spacer(1, 5))

    # ── BAB IV: SOLUSI YANG DIAJUKAN ─────────────────────────────────────────
    story.append(Paragraph("4. Solusi yang Diajukan: Arsitektur Sistem Rekomendasi NaikApa", h1_style))
    story.append(Paragraph(
        "Untuk menjawab kesenjangan di atas, <b>NaikApa</b> dirancang sebagai sistem rekomendasi rute multimodal komprehensif yang memadukan 4 pilar arsitektur teknologi:",
        body_style
    ))

    sol_data = [
        [
            Paragraph("<b>Pilar Solusi</b>", table_header_style),
            Paragraph("<b>Landasan Teknologi & Algoritma</b>", table_header_style),
            Paragraph("<b>Peran & Fungsi dalam Sistem</b>", table_header_style)
        ],
        [
            Paragraph("<b>1. Multimodal Transit Graph Engine</b>", table_cell_bold),
            Paragraph("Standardisasi <b>GTFS</b> (TransJakarta, KRL, MRT, LRT) + Graf In-Memory + <b>Dijkstra Algorithm</b> dengan penalti transfer.", table_cell_style),
            Paragraph("Memetakan seluruh jaringan transit Jabodetabek, mengidentifikasi rute tercepat dan transit transfer antarmoda (radius &le; 350 m).", table_cell_style)
        ],
        [
            Paragraph("<b>2. Real-Time Road Routing & Traffic</b>", table_cell_bold),
            Paragraph("<b>TomTom REST API</b> (Search, Geocoding, Dynamic Matrix Routing) untuk Sepeda Motor dan Mobil.", table_cell_style),
            Paragraph("Menghitung rute kendaraan pribadi dengan data kemacetan langsung, opsi hindari tol, serta estimasi konsumsi BBM riil.", table_cell_style)
        ],
        [
            Paragraph("<b>3. First-Mile / Last-Mile Combiner</b>", table_cell_bold),
            Paragraph("Spatial Nearest Neighbor Search (Radius Asal &le; 8 km, Radius Tujuan &le; 2.5 km) + Multi-segment Orchestrator.", table_cell_style),
            Paragraph("Membangun hingga 9 kombinasi rute <i>Park & Ride</i> (Motor/Mobil ke Stasiun &rarr; Transit Antarmoda &rarr; Jalan Kaki ke Destinasi).", table_cell_style)
        ],
        [
            Paragraph("<b>4. Multi-Criteria Scoring & Reason Engine</b>", table_cell_bold),
            Paragraph("<b>MCDM Inverse Normalization (0–100)</b> + Preference Weighting + Natural Language Generation.", table_cell_style),
            Paragraph("Menghasilkan peringkat objektif (Rekomendasi Utama & 2 Alternatif) beserta kalimat penjelasan rasional dan peringatan gangguan aktif.", table_cell_style)
        ]
    ]

    t_sol = Table(sol_data, colWidths=[125, 165, 197])
    t_sol.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), C_NAVY),
        ('GRID', (0, 0), (-1, -1), 0.5, C_BORDER),
        ('VALIGN', (0, 0), (-1, -1), 'TOP'),
        ('ROWBACKGROUNDS', (0, 1), (-1, -1), [colors.white, C_BG_LIGHT]),
        ('PADDING', (0, 0), (-1, -1), 3),
    ]))
    story.append(t_sol)
    story.append(Spacer(1, 5))

    # ── BAB V: TINJAUAN FORMULASI MATEMATIS ───────────────────────────────────
    story.append(Paragraph("5. Landasan Teoretis & Formulasi Matematis Sistem", h1_style))
    story.append(Paragraph("Sistem rekomendasi bekerja berdasarkan formulasi deterministik multi-parameter:", body_style))

    # Box Rumus MCDM
    f_content = """
    <b>A. Model Normalisasi Terbalik Multi-Kriteria (Inverse Normalization Model):</b><br/>
    Untuk setiap dimensi <i>i</i> &isin; {<i>T</i> (Waktu), <i>C</i> (Biaya), <i>W</i> (Jalan Kaki), <i>K</i> (Transit)}, nilai performa ternormalisasi <b>S<sub>i</sub> &isin; [0.0, 1.0]</b> dihitung sebagai:<br/>
    &nbsp;&nbsp;&nbsp;&nbsp;<b>S<sub>i</sub> = max(0.0, 1.0 &minus; (Nilai_Aktual<sub>i</sub> / Maksimum_Referensi<sub>i</sub>))</b><br/>
    <i>Konstanta Batas: T<sub>max</sub> = 7.200 s (2 Jam), C<sub>max</sub> = Rp 50.000, W<sub>max</sub> = 3.000 m (3 km), K<sub>max</sub> = 5 kali.</i>
    <br/><br/>
    <b>B. Formulasi Skor Mentah (Weighted Utility Function):</b><br/>
    &nbsp;&nbsp;&nbsp;&nbsp;<b>Skor_Mentah = round(&Sigma; (W<sub>i</sub> &times; S<sub>i</sub>)) = round( W<sub>T</sub>&middot;S<sub>T</sub> + W<sub>C</sub>&middot;S<sub>C</sub> + W<sub>W</sub>&middot;S<sub>W</sub> + W<sub>K</sub>&middot;S<sub>K</sub> )</b><br/>
    <i>dengan kendala &Sigma; W<sub>i</sub> = 100%. Vektor bobot (W<sub>T</sub>, W<sub>C</sub>, W<sub>W</sub>, W<sub>K</sub>) disesuaikan menurut preferensi:</i><br/>
    • <b>Tercepat:</b> (45, 15, 15, 15) &nbsp;|&nbsp; • <b>Terhemat:</b> (20, 45, 15, 10) &nbsp;|&nbsp; • <b>Min Jalan:</b> (20, 15, 45, 10) &nbsp;|&nbsp; • <b>Min Transit:</b> (20, 15, 10, 45).
    <br/><br/>
    <b>C. Model Penalti Gangguan Adaptif Spatio-Temporal:</b><br/>
    &nbsp;&nbsp;&nbsp;&nbsp;<b>Skor_Akhir = clamp( Skor_Mentah &minus; P<sub>gangguan</sub>, 0, 100 )</b><br/>
    <i>di mana <b>P<sub>gangguan</sub> = 15 poin</b> apabila terdapat laporan gangguan aktif (t<sub>lapor</sub> &le; 1 jam) pada simpul halte/stasiun atau rute transit yang dilalui.</i>
    """
    t_formula = Table([[Paragraph(f_content, formula_style)]], colWidths=[487])
    t_formula.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, -1), C_BG_BOX),
        ('BOX', (0, 0), (-1, -1), 1, C_ACCENT),
        ('PADDING', (0, 0), (-1, -1), 5),
        ('ROUNDEDCORNERS', [3, 3, 3, 3]),
    ]))
    story.append(t_formula)
    story.append(Spacer(1, 6))

    # ── BAB VI: METODOLOGI & ARSITEKTUR REKAYASA ─────────────────────────────
    story.append(Paragraph("6. Metodologi Rekayasa Perangkat Lunak & Keandalan", h1_style))
    story.append(Paragraph(
        "Pengembangan aplikasi NaikApa mengadopsi prinsip <b>Clean Architecture</b> dan <b>Offline-First Resilience</b>:",
        body_style
    ))
    story.append(Paragraph("• <b>Presentation Layer (Android MVVM):</b> Menggunakan Kotlin Coroutines, StateFlow, Navigation Component, dan ViewBinding untuk antarmuka yang responsif dan reaktif.", bullet_style))
    story.append(Paragraph("• <b>Domain Layer (Pure Business Logic):</b> Berisi algoritma Dijkstra, kalkulator tarif berjenjang, dan scoring engine independen dari framework UI.", bullet_style))
    story.append(Paragraph("• <b>Data Layer & Cloud Synchronization:</b> SQLite lokal untuk data GTFS dan sesi offline, dipadukan dengan Cloud Backend (Neon PostgreSQL) untuk sinkronisasi laporan gangguan komunitas real-time.", bullet_style))
    story.append(Paragraph("• <b>Anti Happy-Path Engineering:</b> Menerapkan mekanisme fallback bertingkat (misal: jika API rute jalan kaki TomTom gagal, sistem otomatis beralih ke estimasi jarak Geodesik Haversine; jika server backend offline, autentikasi dan rute dialihkan ke database SQLite lokal).", bullet_style))
    story.append(Spacer(1, 5))

    # ── BAB VII: KESIMPULAN & ARAH PENGEMBANGAN ──────────────────────────────
    concl_wrapper = []
    concl_wrapper.append(Paragraph("7. Kesimpulan & Rekomendasi Pengembangan Lanjutan", h1_style))
    concl_wrapper.append(Paragraph(
        "<b>Kesimpulan:</b> Platform <b>NaikApa</b> membuktikan bahwa integrasi data terbuka transportasi massal (GTFS), perutean jalan raya dinamis, dan model pendukung keputusan multi-kriteria (MCDM) mampu memecahkan masalah fragmentasi informasi serta mengatasi hambatan <i>First-Mile/Last-Mile</i> di kawasan metropolitan Jabodetabek. Sistem ini menyajikan rekomendasi perjalanan yang objektif, terukur (skor 0–100), adaptif terhadap gangguan, dan berorientasi pada kebutuhan riil komuter.",
        body_style
    ))
    concl_wrapper.append(Paragraph("<b>Arah Pengembangan Lanjutan (Future Roadmap):</b>", h2_style))
    concl_wrapper.append(Paragraph("1. <b>Integrasi Pembayaran Terpadu (Single Ticketing):</b> Integrasi API pembayaran digital (QRIS / e-Wallet) untuk reservasi parkir dan tiket transit dalam satu klik.", bullet_style))
    concl_wrapper.append(Paragraph("2. <b>Prediksi Kepadatan Berbasis Machine Learning:</b> Memanfaatkan data historis pelaporan gangguan untuk memprediksi probabilitas keterlambatan armada pada jam sibuk secara proaktif.", bullet_style))
    concl_wrapper.append(Paragraph("3. <b>Ekspansi Moda Mikro (Micro-Mobility):</b> Memasukkan opsi sepeda sewa (bike-sharing) dan skuter listrik sebagai alternatif moda pengumpan first-mile ramah lingkungan.", bullet_style))
    concl_wrapper.append(Spacer(1, 3))

    # ── REFERENSI AKADEMIK ───────────────────────────────────────────────────
    concl_wrapper.append(Paragraph("Referensi Akademik & Standar Teknis", h1_style))
    refs = [
        "1. Google Developers. (2024). <i>General Transit Feed Specification (GTFS) Reference</i>. MobilityData IO.",
        "2. Dijkstra, E. W. (1959). A note on two problems in connexion with graphs. <i>Numerische Mathematik</i>, 1(1), 269-271.",
        "3. Saaty, T. L. (1990). How to make a decision: The Analytic Hierarchy Process. <i>European Journal of Operational Research</i>, 48(1), 9-26.",
        "4. ITDP Indonesia. (2022). <i>Laporan Integrasi Transportasi Publik Jabodetabek dan Tantangan First-Mile Last-Mile</i>. Institute for Transportation and Development Policy.",
        "5. World Bank. (2019). <i>Enhancing Urban Mobility in Greater Jakarta: Issues and Policy Options</i>. World Bank Group, Washington D.C.",
        "6. TomTom Developer Portal. (2024). <i>Routing API & Search API Technical Documentation</i>. TomTom International B.V."
    ]
    for r in refs:
        concl_wrapper.append(Paragraph(r, ParagraphStyle('RefStyle', parent=body_style, fontSize=7.2, leading=9.5, spaceAfter=1.5)))

    story.append(KeepTogether(concl_wrapper))

    doc.build(story, canvasmaker=AcademicNumberedCanvas)
    print(f"Academic PDF berhasil dibuat di: {filename}")

if __name__ == '__main__':
    target_path = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "konsep", "Kajian_Akademik_Masalah_Urgensi_dan_Solusi_NaikApa.pdf"))
    build_academic_pdf(target_path)
