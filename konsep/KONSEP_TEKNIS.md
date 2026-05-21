# 📱 Tije Transit Planner — Dokumentasi Konsep & Arsitektur Teknis

> **Versi:** 1.0 (Web Prototype)  
> **Target Platform Berikutnya:** Android (Kotlin)  
> **Dibuat:** Mei 2026  
> **Tujuan Dokumen:** Blueprint teknis untuk rekonstruksi ulang sebagai aplikasi mobile.

---

## 📋 Daftar Isi

1. [Gambaran Umum Proyek](#1-gambaran-umum-proyek)
2. [Arsitektur Sistem](#2-arsitektur-sistem)
3. [Sumber Data GTFS](#3-sumber-data-gtfs)
4. [Struktur File GTFS](#4-struktur-file-gtfs)
5. [Proses Import & Database](#5-proses-import--database)
6. [Graf di Memori (In-Memory Graph)](#6-graf-di-memori-in-memory-graph)
7. [Algoritma Dijkstra Multimodal](#7-algoritma-dijkstra-multimodal)
8. [Sistem Kalkulasi Tarif](#8-sistem-kalkulasi-tarif)
9. [Walking Transfer Geospasial](#9-walking-transfer-geospasial)
10. [Integrasi TomTom API (Mode Berkendara)](#10-integrasi-tomtom-api-mode-berkendara)
11. [REST API Backend](#11-rest-api-backend)
12. [Identitas Moda (Agency ID)](#12-identitas-moda-agency-id)
13. [Catatan Penting & Keterbatasan Data](#13-catatan-penting--keterbatasan-data)
14. [Panduan Adaptasi ke Android/Kotlin](#14-panduan-adaptasi-ke-androidkotlin)

---

## 1. Gambaran Umum Proyek

**Tije Transit Planner** adalah aplikasi pencari rute transit dan berkendara di wilayah **Jakarta dan Jabodetabek**. Aplikasi ini mendukung **7 moda transportasi**:

| Moda | Kode | Deskripsi |
|---|---|---|
| Campur | `all` | Gabungan semua moda transit |
| TransJakarta | `tj` | Bus Rapid Transit (BRT) Jakarta |
| KRL | `krl` | Commuter Line Jabodetabek (PT KAI Commuter) |
| MRT | `mrt` | Mass Rapid Transit Jakarta (MRTJ) |
| LRT | `lrt` | LRT Jakarta & LRT Jabodebek |
| Motor | `motor` | Berkendara sepeda motor via TomTom |
| Mobil | `mobil` | Berkendara mobil via TomTom |

### Fitur Utama
- 🔍 **Autocomplete halte/stasiun** (offline, berbasis data GTFS lokal)
- 🔍 **Autocomplete alamat bebas** (untuk motor/mobil via TomTom Fuzzy Search API)
- 🗺️ **Visualisasi rute di peta** (Leaflet + beberapa style tile layer)
- ⚡ **3 preferensi sortir**: Tercepat, Terhemat, Transit Minimal
- 🚗 **Rute alternatif berkendara**: Hingga 3 rute sekaligus dalam 1 API request
- 🛣️ **Opsi hindari tol** (khusus mode Mobil)
- ⛽ **Estimasi biaya BBM** berdasarkan jarak

---

## 2. Arsitektur Sistem

```
┌─────────────────────────────────────────────────────────┐
│                   FRONTEND (Browser)                     │
│  HTML + Vanilla CSS + Vanilla JS + Leaflet.js            │
│                                                          │
│  ┌──────────────┐  ┌────────────────┐  ┌─────────────┐  │
│  │ Mode Selector│  │  Autocomplete  │  │  Peta Rute  │  │
│  │  (7 Moda)   │  │  Halte/Alamat  │  │  (Leaflet)  │  │
│  └──────────────┘  └────────────────┘  └─────────────┘  │
└───────────────────────▼──────────────────────▼───────────┘
                        │                      │
                  REST API                TomTom API
              (Backend lokal)          (Langsung dari browser)
                        │
┌───────────────────────▼──────────────────────────────────┐
│                  BACKEND (Node.js + Express)              │
│                                                          │
│  GET /api/config   → Kirim TomTom API Key ke frontend   │
│  GET /api/stops    → Daftar halte/stasiun per moda      │
│  GET /api/route    → Hitung rute transit (Dijkstra)     │
│                                                          │
│  ┌─────────────────────────────────────────────────────┐ │
│  │            routing.js (Core Logic)                  │ │
│  │                                                     │ │
│  │  1. initRouting()     → Build in-memory graph       │ │
│  │  2. findShortestPath()→ Dijkstra multimodal         │ │
│  └─────────────────────────────────────────────────────┘ │
│                          │                               │
│                          ▼                               │
│  ┌─────────────────────────────────────────────────────┐ │
│  │         SQLite Database (gtfs.db ~29MB)             │ │
│  │  Tabel: stops, stop_times, trips, routes, agency,   │ │
│  │         fare_attributes, fare_rules, shapes, ...    │ │
│  └─────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────┘
                          ▲
                          │ (import sekali saja)
                          │
┌─────────────────────────┴────────────────────────────────┐
│                   DATA SUMBER (Offline)                   │
│                                                          │
│  data/gtfs/          → TransJakarta (TJ)                 │
│  data/gtfs-krl/      → KRL Commuter Line Jabodetabek     │
│  data/gtfs-mrt/      → MRT Jakarta                       │
│  data/gtfs-lrt/      → LRT Jakarta & LRT Jabodebek       │
└──────────────────────────────────────────────────────────┘
```

> **Untuk Android:** Backend Node.js tidak diperlukan. Logika Dijkstra dan data GTFS bisa dibundel langsung ke dalam aplikasi menggunakan **Room Database** atau **SQLite** native.

---

## 3. Sumber Data GTFS

Semua data transit bersifat **offline** — diimpor sekali ke SQLite dan digunakan tanpa internet.

### 3.1 TransJakarta (TJ)
- **Agency ID:** `Tije`
- **Sumber:** Feed GTFS resmi TransJakarta (tersedia di situs resmi / portal data publik)
- **Cakupan:** Seluruh koridor BRT TransJakarta di wilayah DKI Jakarta
- **File utama:** `agency.txt`, `routes.txt`, `stops.txt`, `stop_times.txt`, `trips.txt`, `fare_attributes.txt`, `fare_rules.txt`, `shapes.txt`, `frequencies.txt`
- **Catatan Tarif:** Tarif per rute disimpan di `fare_rules.txt` dan `fare_attributes.txt`. Rute reguler Rp 3.500, beberapa rute premium lebih tinggi.

### 3.2 KRL Commuter Line Jabodetabek
- **Agency ID:** `KAIC` (KAI Commuter)
- **Sumber:** Data custom berdasarkan jadwal resmi KAI Commuter (GAPEKA 2025, Update Mei 2026) + Peta Rute resmi
- **Cakupan:** Jalur Bogor, Cikarang, Rangkasbitung, Tangerang, Tanjung Priok
- **Catatan:** Data GTFS ini **bukan feed resmi KAI** — dibuat manual dengan referensi dokumen resmi. `stop_times.txt` menggunakan model berbasis **headway** (bukan jadwal per trip eksak).
- **Skema Tarif Progresif:** Rp 3.000 untuk 25 km pertama + Rp 1.000 per 10 km berikutnya (diimplementasikan di kode, bukan di GTFS fare_rules).

### 3.3 MRT Jakarta
- **Agency ID:** `MRTJ`
- **Sumber:** Data custom berdasarkan informasi resmi MRT Jakarta
- **Cakupan:** Jalur Lebak Bulus–Bundaran HI (Koridor Selatan-Utara)
- **Skema Tarif:** Rp 3.000 base + Rp 1.000 per stasiun dilewati, maksimal Rp 14.000

### 3.4 LRT Jakarta
- **Agency ID:** `LRTJ`
- **Sumber:** Data custom
- **Cakupan:** Jalur Kelapa Gading–Velodrome
- **Skema Tarif:** Flat Rp 5.000

### 3.5 LRT Jabodebek
- **Agency ID:** `LRTJB`
- **Sumber:** Data custom
- **Cakupan:** Jalur Dukuh Atas–Bekasi Timur & Dukuh Atas–Jatimulya
- **Skema Tarif:** Rp 5.000 base + Rp 700/km, maksimal Rp 20.000

---

## 4. Struktur File GTFS

Format GTFS (General Transit Feed Specification) adalah standar terbuka oleh Google untuk data transit publik. Setiap dataset terdiri dari file-file `.txt` (CSV).

### File Wajib yang Digunakan

| File | Deskripsi | Kolom Kunci |
|---|---|---|
| `agency.txt` | Identitas operator transit | `agency_id`, `agency_name` |
| `routes.txt` | Daftar rute/koridor | `route_id`, `agency_id`, `route_short_name`, `route_long_name`, `route_color`, `route_text_color` |
| `trips.txt` | Daftar perjalanan (trip) per rute | `trip_id`, `route_id`, `direction_id` |
| `stops.txt` | Daftar halte/stasiun | `stop_id`, `stop_name`, `stop_lat`, `stop_lon` |
| `stop_times.txt` | Jadwal kedatangan/keberangkatan per trip | `trip_id`, `stop_id`, `arrival_time`, `departure_time`, `stop_sequence` |
| `fare_attributes.txt` | Definisi tarif | `fare_id`, `price`, `currency_type` |
| `fare_rules.txt` | Aturan tarif per rute | `fare_id`, `route_id` |
| `shapes.txt` | Koordinat geometri jalur rute | `shape_id`, `shape_pt_lat`, `shape_pt_lon`, `shape_pt_sequence` |
| `calendar.txt` | Jadwal layanan mingguan | `service_id`, `monday`–`sunday` |

### Struktur Kritis: `stop_times.txt`

Ini adalah file terbesar dan terpenting. Format kolomnya:

```
trip_id, arrival_time, departure_time, stop_id, stop_sequence
T1_PAGI, 06:00:00, 06:00:00, BLOK-M, 1
T1_PAGI, 06:05:00, 06:05:00, HALIMUN, 2
T1_PAGI, 06:10:00, 06:10:00, DUKUH-ATAS, 3
...
```

**Cara sistem menggunakannya:** Sistem melakukan JOIN `stop_times` dengan dirinya sendiri untuk menemukan pasangan halte berurutan (`stop_sequence + 1`) dalam trip yang sama. Selisih waktu antara `departure_time` halte A dan `arrival_time` halte B adalah **durasi segmen**.

> **Catatan waktu > 24 jam:** Format GTFS mengizinkan waktu seperti `25:30:00` untuk perjalanan yang melewati tengah malam. Sistem menangani ini dengan konversi ke detik dan koreksi `+ 86400` jika hasilnya negatif.

---

## 5. Proses Import & Database

### Alur Import (Dilakukan Sekali)

```
File GTFS (.txt) ──► node server/import.js ──► gtfs.db (SQLite ~29MB)
```

Library **`gtfs` (npm)** digunakan untuk mengimpor semua file GTFS ke dalam satu database SQLite secara otomatis menggunakan `config.json`:

```json
{
  "agencies": [
    { "agencyKey": "transjakarta", "path": "./data/gtfs" },
    { "agencyKey": "krl",          "path": "./data/gtfs-krl" },
    { "agencyKey": "mrt",          "path": "./data/gtfs-mrt" },
    { "agencyKey": "lrt",          "path": "./data/gtfs-lrt" }
  ],
  "sqlitePath": "./gtfs.db"
}
```

### Query Utama untuk Membangun Graf

```sql
SELECT
  st1.stop_id AS from_stop_id,
  st2.stop_id AS to_stop_id,
  r.route_id, r.route_short_name, r.route_long_name,
  r.route_color, r.route_text_color, r.agency_id,
  st1.departure_time, st2.arrival_time
FROM stop_times st1
JOIN stop_times st2
  ON  st1.trip_id = st2.trip_id
  AND st2.stop_sequence = st1.stop_sequence + 1
JOIN trips t ON st1.trip_id = t.trip_id
JOIN routes r ON t.route_id = r.route_id
```

**Hasil query ini adalah daftar semua koneksi langsung antar halte berurutan.** Karena satu pasang halte bisa dilewati ratusan trip dalam sehari, sistem mengagregasi durasi dengan menghitung **rata-rata durasi** dari semua trip yang melewati pasangan halte tersebut.

---

## 6. Graf di Memori (In-Memory Graph)

Setelah data dimuat dari SQLite, sistem membangun **graf berarah berbobot** (directed weighted graph) di RAM menggunakan JavaScript object (Map/Dictionary).

### Struktur Graf

```javascript
graph = {
  "STOP_A": [
    {
      to: "STOP_B",
      routeId: "1A",
      routeShortName: "1A",
      routeLongName: "Blok M - Kota",
      routeColor: "E31E24",
      routeTextColor: "FFFFFF",
      distance: 1.23,      // km (Haversine)
      duration: 180,       // detik (rata-rata dari stop_times)
      agencyId: "Tije"
    },
    {
      to: "STOP_C",
      routeId: "WALKING",
      routeShortName: "Jalan Kaki",
      routeLongName: "Jalan Kaki ke Stasiun X",
      distance: 0.28,      // km
      duration: 252,       // detik (jarak × 900 detik/km = ~4 km/jam)
      agencyId: "WALKING"
    }
  ],
  "STOP_B": [ ... ],
  ...
}
```

### Statistik Graf (Web Prototype)
- **Halte/Stasiun Aktif:** ~8.045 node
- **Koneksi Rute:** ~14.658 edge berdasarkan jadwal
- **Walking Transfer:** ~910 koneksi jalan kaki antarmoda (radius ≤ 350 meter)

---

## 7. Algoritma Dijkstra Multimodal

Sistem menggunakan **algoritma Dijkstra** yang dimodifikasi untuk mendukung:
1. **Multi-moda** (bisa melewati TJ, KRL, MRT, LRT, dan jalan kaki dalam satu rute)
2. **Filter moda** (hanya satu moda jika dipilih)
3. **3 mode sortir** dengan bobot yang berbeda

### Pseudocode Dijkstra

```
fungsi findShortestPath(startId, endId, mode, sortBy):
  inisialisasi distances[semua_stop] = ∞
  distances[startId] = 0
  
  priorityQueue.push(startId, bobot=0)
  
  selama priorityQueue tidak kosong:
    currStop = ambil stop dengan bobot terkecil dari queue
    
    jika currStop == endId: BERHENTI
    
    untuk setiap tetangga (edge) dari currStop:
      // FILTER MODA
      jika mode == 'tj' dan edge.agencyId != 'Tije': LEWATI
      jika mode == 'krl' dan edge.agencyId != 'KAIC': LEWATI
      jika mode == 'mrt' dan edge.agencyId != 'MRTJ': LEWATI
      jika mode == 'lrt' dan edge.agencyId != 'LRTJ/LRTJB': LEWATI
      
      // HITUNG BOBOT (tergantung sortBy)
      edgeCost = hitungBobot(edge, edgeSebelumnya, sortBy)
      
      newDist = distances[currStop] + edgeCost
      jika newDist < distances[edge.to]:
        distances[edge.to] = newDist
        previous[edge.to] = currStop + info_edge
        queue.update(edge.to, newDist)
  
  // REKONSTRUKSI JALUR
  jalur = []
  curr = endId
  selama curr != startId:
    jalur.prepend(previous[curr])
    curr = previous[curr].from
  
  kembalikan jalur
```

### Sistem Bobot Berdasarkan `sortBy`

#### Mode `fastest` (Tercepat)
```
edgeCost = edge.duration (detik)
         + TRANSFER_PENALTY (300 detik) jika ganti rute berbeda
         + 60 detik jika WALKING → naik kendaraan
```

**Alasan penalti 300 detik (5 menit):** Pada kenyataannya, saat pindah bus/kereta, ada waktu tunggu (headway), waktu berjalan ke peron, dll. Tanpa penalti ini, algoritma bisa merekomendasikan rute dengan banyak transit yang secara teori lebih cepat tapi tidak realistis.

#### Mode `cheapest` (Terhemat)
```
edgeCost = fare (Rupiah virtual, berdasarkan entry pertama ke agensi)
         + transferPenalty (Rp 500 virtual jika ganti armada)
         + (edge.duration × 0.00001)  ← tiebreaker durasi sangat kecil
```

**Logika tarif per agensi:**
- Jika **masuk pertama kali** ke agensi baru → charge tarif dasar
- Jika **masih di agensi yang sama** → charge biaya progresif per km/stasiun
- Transfer antara halte TJ reguler → **GRATIS** (tidak ada double-charge jika tarif sama)

**Catatan tiebreaker:** Jika dua rute sama-sama biayanya Rp 3.500, sistem memilih yang lebih cepat karena bobot kecil `× 0.00001` dari durasi berfungsi sebagai tiebreaker deterministik.

#### Mode `fewest_transfers` (Transit Minimal)
```
edgeCost = 1.0 jika ganti rute (berbeda routeShortName)
         + 0   jika masih di rute yang sama
         + (edge.duration × 0.00001)  ← tiebreaker durasi
```

Sistem meminimalkan **jumlah perpindahan armada**, bukan jumlah moda. Tiebreaker durasi memastikan jika jumlah transfer sama, rute tercepat yang dipilih.

### Rekonstruksi Jalur ke "Steps" (Langkah Perjalanan)

Setelah Dijkstra selesai, sistem merekonstruksi jalur dari `endId` kembali ke `startId` dengan menelusuri `previous[]`. Hasilnya adalah array segmen halte-ke-halte.

Segmen ini kemudian **dikelompokkan** menjadi langkah perjalanan (`steps`) yang lebih readable:
- Segmen berurutan dengan `routeShortName` dan `agencyId` yang **sama** → digabung menjadi satu step
- Segmen dengan `routeId === 'WALKING'` selalu berdiri sendiri (tidak digabung)

**Contoh Output Step:**
```json
{
  "routeShortName": "1A",
  "routeLongName": "Blok M - Kota",
  "agencyId": "Tije",
  "boardStop": { "id": "BLK", "name": "Blok M", "lat": -6.244, "lon": 106.798 },
  "alightStop": { "id": "DKA", "name": "Dukuh Atas BNI", "lat": -6.201, "lon": 106.823 },
  "stopsCount": 5,
  "distance": 4.2,
  "duration": 900
}
```

---

## 8. Sistem Kalkulasi Tarif

Tarif dihitung setelah rekonstruksi jalur selesai, berdasarkan steps yang sudah terkelompok.

### Skema Tarif per Moda

#### TransJakarta (Tije)
- **Tarif reguler:** Flat **Rp 3.500** per perjalanan (tidak ada double-charge antar koridor TJ biasa)
- **Tarif premium:** Beberapa rute punya tarif lebih tinggi (disimpan di `fare_rules.txt`)
- **Rute gratis:** Beberapa rute penghubung berkode `GR` tidak dikenai tarif
- **Logika gabungan:** Jika ada rute premium, tarif = jumlah premium + Rp 3.500 (jika ada rute reguler)

```
fare_rules.txt & fare_attributes.txt
  ↓
routeFares = { "route_id": price_in_rupiah, ... }
```

#### KRL Commuter Line (KAIC)
```
jika jarak <= 25 km: tarif = Rp 3.000
jika jarak > 25 km:  tarif = Rp 3.000 + ⌈(jarak - 25) / 10⌉ × Rp 1.000
```

Contoh:
- 15 km → Rp 3.000
- 30 km → Rp 3.000 + (1 × Rp 1.000) = Rp 4.000
- 55 km → Rp 3.000 + (3 × Rp 1.000) = Rp 6.000

#### MRT Jakarta (MRTJ)
```
tarif = Rp 3.000 + (stopsCount × Rp 1.000)
tarif = min(tarif, Rp 14.000)
```

#### LRT Jakarta (LRTJ)
```
tarif = Rp 5.000 (flat, tidak bergantung jarak)
```

#### LRT Jabodebek (LRTJB)
```
tarif = Rp 5.000 + max(0, (jarak - 1) × Rp 700)
tarif = min(tarif, Rp 20.000)
```

#### Estimasi BBM (Motor/Mobil — bukan tarif transit)
```
Motor:  konsumsi 40 km/liter × Rp 10.000/liter → Rp 250 per km
Mobil:  konsumsi 12 km/liter × Rp 10.000/liter → Rp 833 per km

estimasi_bbm = jarak_km × rate_per_km
```

---

## 9. Walking Transfer Geospasial

Ini adalah salah satu fitur paling kritis untuk membuat rute multimoda yang realistis.

### Masalah
Data GTFS tiap operator (TJ, KRL, MRT, LRT) hanya berisi koneksi **dalam jaringannya sendiri**. Tidak ada informasi tentang bagaimana penumpang berpindah antar jaringan (misalnya dari bus TJ ke KRL).

### Solusi: Auto-Generate Walking Transfer

Saat inisialisasi, sistem secara otomatis menghitung koneksi jalan kaki antara halte **dari agensi yang berbeda** yang jaraknya ≤ 350 meter:

```javascript
// Algoritma O(n²) — dijalankan sekali saat startup
for (setiap pasang halte s1, s2 dari berbeda agensi):
  jarak = Haversine(s1.lat, s1.lon, s2.lat, s2.lon)
  
  jika jarak <= 0.35 km:
    walkDuration = jarak × 900  // detik (asumsi kecepatan jalan 4 km/jam)
    
    tambahkan edge: s1 → s2 (WALKING)
    tambahkan edge: s2 → s1 (WALKING)
```

### Rumus Haversine (Jarak Bola Bumi)

```
R = 6371 km (jari-jari bumi)
dLat = (lat2 - lat1) × π/180
dLon = (lon2 - lon1) × π/180

a = sin²(dLat/2) + cos(lat1 × π/180) × cos(lat2 × π/180) × sin²(dLon/2)
c = 2 × atan2(√a, √(1-a))
jarak = R × c
```

### Contoh Transfer yang Dihasilkan
- **Dukuh Atas BNI** (TJ) ↔ **Stasiun Sudirman/BNI** (KRL) — ±250m
- **Tanah Abang** (TJ) ↔ **Stasiun Tanah Abang** (KRL)
- **Lebak Bulus** (MRT) ↔ halte sekitar (TJ)

### Catatan Penting
- Threshold **350 meter** dipilih secara pragmatis — bisa diubah
- Kecepatan jalan kaki diasumsikan **4 km/jam** (900 detik/km)
- Koneksi WALKING hanya dibuat **antar agensi berbeda** untuk mencegah walking transfer yang tidak masuk akal dalam jaringan yang sama

---

## 10. Integrasi TomTom API (Mode Berkendara)

Mode Motor dan Mobil tidak menggunakan data GTFS sama sekali — seluruhnya mengandalkan **TomTom API** secara langsung dari browser/klien.

### API Key
API Key TomTom disimpan di `.env` pada backend:
```
API_KEY=your_tomtom_api_key_here
```
Frontend mengambil API Key dari endpoint `/api/config` agar key tidak ter-hardcode di JavaScript.

### 10.1 TomTom Fuzzy Search API (Autocomplete Alamat)

**Endpoint:**
```
GET https://api.tomtom.com/search/2/search/{query}.json
  ?key={apiKey}
  &countrySet=ID
  &typeahead=true
  &limit=8
  &lat=-6.2088
  &lon=106.8456
```

**Parameter penting:**
| Parameter | Nilai | Keterangan |
|---|---|---|
| `query` | String pencarian | Diambil dari input pengguna |
| `countrySet=ID` | Indonesia | Filter hasil hanya Indonesia |
| `typeahead=true` | — | Aktifkan mode autocomplete real-time |
| `limit=8` | — | Maksimal 8 saran ditampilkan |
| `lat`, `lon` | Pusat Jakarta | Bias pencarian ke sekitar Jakarta |

**Response yang digunakan:**
```json
{
  "results": [
    {
      "address": { "freeformAddress": "Jl. Sudirman No.1, Jakarta" },
      "position": { "lat": -6.2100, "lon": 106.8230 },
      "poi": { "name": "Menara BCA" }
    }
  ]
}
```

**Debounce:** Request hanya dikirim setelah pengguna berhenti mengetik **300ms** (untuk menghindari spam request).

### 10.2 TomTom Routing API (Kalkulasi Rute)

**Endpoint:**
```
GET https://api.tomtom.com/routing/1/calculateRoute/{fromLat},{fromLon}:{toLat},{toLon}/json
  ?key={apiKey}
  &travelMode={motorcycle|car}
  &routeType=fastest
  &maxAlternatives=2
  [&avoid=tollRoads]
```

**Parameter penting:**
| Parameter | Nilai | Keterangan |
|---|---|---|
| `travelMode` | `motorcycle` atau `car` | Mode berkendara |
| `routeType=fastest` | — | Optimasi untuk rute tercepat |
| `maxAlternatives=2` | — | Minta **1 rute utama + 2 alternatif** |
| `avoid=tollRoads` | Opsional | Hindari jalan tol (khusus mobil) |

**1 Request → 3 Rute:** Ini efisien dari sisi penggunaan kuota API. Semua rute diterima dalam satu respons JSON.

**Response yang digunakan:**
```json
{
  "routes": [
    {
      "summary": {
        "lengthInMeters": 12500,
        "travelTimeInSeconds": 2100
      },
      "legs": [
        {
          "points": [
            { "latitude": -6.2110, "longitude": 106.8230 },
            { "latitude": -6.2115, "longitude": 106.8235 },
            ...
          ]
        }
      ]
    },
    { /* rute alternatif 1 */ },
    { /* rute alternatif 2 */ }
  ]
}
```

**Catatan:** `points` dalam `legs[0].points` adalah array koordinat jalur rute yang sangat detail (bisa ratusan titik). Koordinat ini digunakan langsung untuk menggambar `Polyline` di peta.

### 10.3 TomTom Map Tiles (Peta)

| Style | URL |
|---|---|
| Basic (Light) | `https://api.tomtom.com/map/1/tile/basic/main/{z}/{x}/{y}.png?key={key}` |
| Hybrid/Satellite | `https://api.tomtom.com/map/1/tile/hybrid/main/{z}/{x}/{y}.png?key={key}` |

Alternatif gratis (tidak butuh API key):
| Style | URL |
|---|---|
| CartoDB Positron | `https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png` |
| CartoDB Dark Matter | `https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png` |

---

## 11. REST API Backend

### `GET /api/config`
Mengembalikan konfigurasi publik termasuk TomTom API Key.
```json
{ "tomtomApiKey": "xxxxxxxxxxxxxxxxxxxx" }
```

### `GET /api/stops?mode={mode}`
Mengembalikan daftar halte/stasiun berdasarkan moda.

**Query Parameter:**
- `mode`: `all` / `tj` / `krl` / `mrt` / `lrt`

**Response:**
```json
[
  { "id": "BLK", "name": "Blok M", "lat": -6.244, "lon": 106.798, "agencyId": "Tije" },
  ...
]
```

### `GET /api/route?from={stopId}&to={stopId}&mode={mode}&sortBy={sort}`
Menghitung rute tercepat/terhemat antar dua halte.

**Query Parameters:**
| Parameter | Contoh | Keterangan |
|---|---|---|
| `from` | `BLK` | Stop ID asal |
| `to` | `HBK` | Stop ID tujuan |
| `mode` | `all` / `tj` / `krl` / `mrt` / `lrt` | Filter moda |
| `sortBy` | `fastest` / `cheapest` / `fewest_transfers` | Preferensi sortir |

**Response:**
```json
{
  "startStop": { "id": "BLK", "name": "Blok M", "lat": -6.244, "lon": 106.798 },
  "endStop":   { "id": "HBK", "name": "Harmoni", "lat": -6.167, "lon": 106.814 },
  "totalDistance": 8.45,
  "totalDuration": 2400,
  "totalFare": 3500,
  "steps": [
    {
      "routeId": "1A",
      "routeShortName": "1A",
      "routeLongName": "Blok M - Kota",
      "routeColor": "E31E24",
      "agencyId": "Tije",
      "boardStop": { "id": "BLK", "name": "Blok M", ... },
      "alightStop": { "id": "DKA", "name": "Dukuh Atas", ... },
      "stopsCount": 5,
      "distance": 4.2,
      "duration": 900
    },
    {
      "routeId": "WALKING",
      "routeShortName": "Jalan Kaki",
      "agencyId": "WALKING",
      ...
    },
    ...
  ],
  "fullPath": [ { "id": "BLK", ... }, { "id": "STOP_X", ... }, ... ]
}
```

---

## 12. Identitas Moda (Agency ID)

| Agency ID | Moda | Nama Lengkap |
|---|---|---|
| `Tije` | Bus TJ | TransJakarta |
| `KAIC` | KRL | KAI Commuter (KRL Jabodetabek) |
| `MRTJ` | MRT | MRT Jakarta |
| `LRTJ` | LRT | LRT Jakarta (Kelapa Gading–Velodrome) |
| `LRTJB` | LRT | LRT Jabodebek (Dukuh Atas–Bekasi/Jatimulya) |
| `WALKING` | Jalan Kaki | Auto-generated walking transfer |

**Catatan:** Agency ID `WALKING` tidak berasal dari GTFS — ini adalah konvensi internal yang dibuat sistem untuk merepresentasikan segmen jalan kaki antar halte.

---

## 13. Catatan Penting & Keterbatasan Data

### ⚠️ Data KRL Bukan Jadwal Eksak
Data `stop_times.txt` KRL menggunakan **model headway** (frekuensi keberangkatan), bukan jadwal eksak per trip. Durasi antar stasiun dihitung berdasarkan rata-rata dari semua trip dalam dataset.

**Implikasi untuk Kotlin:**
- Untuk produksi, ganti dengan jadwal eksak dari PDF resmi KAI Commuter GAPEKA 2025 atau scraping dari API resmi.
- Pertimbangkan integrasi dengan **Real-time GTFS** jika tersedia.

### ⚠️ Tidak Ada Ketersediaan Real-Time
Aplikasi ini **tidak mempertimbangkan** jadwal aktual (jam berapa sekarang, apakah kendaraan sedang beroperasi). Sistem mengasumsikan semua layanan aktif dan menggunakan durasi rata-rata.

**Implikasi untuk Kotlin:**
- Tambahkan filter waktu berdasarkan `calendar.txt` dan `calendar_dates.txt` (hari layanan aktif).
- Idealnya integrasikan GTFS-RT (Realtime) feed jika operator menyediakannya.

### ⚠️ Koordinat Beberapa Stasiun Estimasi
Koordinat beberapa stasiun KRL adalah hasil interpolasi/estimasi berdasarkan peta. Untuk navigasi presisi, lakukan validasi koordinat dengan Google Maps atau OpenStreetMap.

### ⚠️ Threshold Walking Transfer 350m Pragmatis
Ambang batas 350 meter untuk walking transfer dipilih pragmatis. Dalam kondisi nyata, beberapa transfer yang "dekat secara jarak" mungkin sulit ditempuh (jalan buntu, pagar, dll).

---

## 14. Panduan Adaptasi ke Android/Kotlin

Ini adalah panduan tingkat tinggi untuk merekonstruksi aplikasi sebagai **native Android app**.

### Arsitektur yang Direkomendasikan

```
Android App (Kotlin)
│
├── Data Layer
│   ├── Room Database (SQLite) ← ganti Node.js + better-sqlite3
│   │   ├── StopEntity
│   │   ├── RouteEntity
│   │   ├── TripEntity
│   │   ├── StopTimeEntity
│   │   ├── FareEntity
│   │   └── FareRuleEntity
│   │
│   └── TomTom SDK / Retrofit API Client
│       ├── TomTomSearchApi
│       └── TomTomRoutingApi
│
├── Domain Layer
│   ├── RoutingGraph (In-Memory Graph, sama dengan routing.js)
│   ├── DijkstraAlgorithm
│   ├── WalkingTransferBuilder (Haversine)
│   └── FareCalculator
│
└── Presentation Layer
    ├── Map Fragment (TomTom Maps SDK atau Mapbox)
    ├── SearchFragment
    └── RouteResultFragment
```

### Rekomendasi Library

| Kebutuhan | Library Kotlin |
|---|---|
| Database lokal | **Room** (Android Jetpack) |
| HTTP Client | **Retrofit** + **OkHttp** |
| Maps | **TomTom Maps SDK for Android** atau **Mapbox** |
| DI (Dependency Injection) | **Hilt** |
| Async/Coroutines | **Kotlin Coroutines + Flow** |
| Parsing JSON | **Kotlinx Serialization** atau **Gson** |
| UI Components | **Jetpack Compose** (Modern) |

### Strategi Import GTFS ke Room

```kotlin
// Gunakan library: com.github.OneBusAway:gtfs-realtime-bindings
// Atau buat CSV parser manual karena GTFS = CSV

// Contoh alur:
// 1. Bundel file .db yang sudah diisi sebagai asset
// 2. ATAU import CSV saat pertama kali app dibuka
// 3. ATAU unduh dari server dan simpan lokal

@Entity(tableName = "stops")
data class StopEntity(
    @PrimaryKey val stopId: String,
    val stopName: String,
    val stopLat: Double,
    val stopLon: Double,
    val agencyId: String
)
```

### Implementasi Graf & Dijkstra di Kotlin

```kotlin
// Graph representation
val graph: Map<String, List<Edge>> = HashMap()

data class Edge(
    val to: String,
    val routeId: String,
    val routeShortName: String,
    val agencyId: String,
    val distance: Double,  // km
    val duration: Int      // detik
)

// Priority Queue Dijkstra
fun findShortestPath(
    startId: String, 
    endId: String, 
    mode: TransitMode,
    sortBy: SortPreference
): RouteResult? {
    val distances = mutableMapOf<String, Double>().withDefault { Double.MAX_VALUE }
    val previous = mutableMapOf<String, EdgeTrace>()
    val pq = PriorityQueue<Pair<String, Double>>(compareBy { it.second })
    
    distances[startId] = 0.0
    pq.add(Pair(startId, 0.0))
    
    while (pq.isNotEmpty()) {
        val (currId, currDist) = pq.poll()
        if (currDist > distances.getValue(currId)) continue
        if (currId == endId) break
        
        graph[currId]?.forEach { edge ->
            if (!isEdgeAllowed(edge, mode)) return@forEach
            
            val cost = calculateEdgeCost(edge, previous[currId], sortBy)
            val newDist = distances.getValue(currId) + cost
            
            if (newDist < distances.getValue(edge.to)) {
                distances[edge.to] = newDist
                previous[edge.to] = EdgeTrace(from = currId, edge = edge)
                pq.add(Pair(edge.to, newDist))
            }
        }
    }
    
    return reconstructPath(startId, endId, previous)
}
```

### Walking Transfer Builder

```kotlin
fun buildWalkingTransfers(stops: List<StopEntity>): List<Edge> {
    val walkingEdges = mutableListOf<Edge>()
    val WALK_THRESHOLD_KM = 0.35  // 350 meter
    val WALK_SPEED_SECONDS_PER_KM = 900.0  // 4 km/jam
    
    for (i in stops.indices) {
        for (j in i + 1 until stops.size) {
            val s1 = stops[i]
            val s2 = stops[j]
            
            // Hanya buat walking transfer antar agensi berbeda
            if (s1.agencyId == s2.agencyId) continue
            
            val dist = haversine(s1.stopLat, s1.stopLon, s2.stopLat, s2.stopLon)
            if (dist <= WALK_THRESHOLD_KM) {
                val walkDuration = (dist * WALK_SPEED_SECONDS_PER_KM).roundToInt()
                // Tambahkan edge dua arah
                walkingEdges.add(Edge(s2.stopId, "WALKING", "Jalan Kaki", "WALKING", dist, walkDuration))
                // dan kebalikannya...
            }
        }
    }
    return walkingEdges
}
```

### Integrasi TomTom untuk Mode Berkendara

Untuk Android, gunakan **TomTom Maps SDK for Android** yang menyediakan:
- Native Kotlin API untuk Routing
- Native Kotlin API untuk Search (Fuzzy Search)
- Mapview component untuk menampilkan peta dan polyline

```kotlin
// Contoh menggunakan TomTom SDK
val routingApi = OnlineRoutingApi.create(context, apiKey)

val routeOptions = RoutePlanningOptions(
    itinerary = Itinerary(
        origin = Place(GeoPoint(fromLat, fromLon)),
        destination = Place(GeoPoint(toLat, toLon))
    ),
    routeType = RouteType.FASTEST,
    travelMode = TravelMode.CAR,
    maxAlternatives = 2,
    avoid = if (avoidToll) listOf(Avoid.TOLL_ROADS) else emptyList()
)

routingApi.planRoute(routeOptions, object : RoutePlanningCallback {
    override fun onSuccess(result: RoutePlanningResult) {
        val routes = result.routes  // List<Route> berisi hingga 3 rute
    }
})
```

### Strategi Bundling Database

**Opsi 1 (Direkomendasikan untuk Produksi):** Pre-built SQLite  
1. Jalankan import GTFS menggunakan script Node.js/Python sekali saja
2. Bundel file `.db` sebagai Android Asset
3. Copy ke internal storage saat pertama kali app dibuka

**Opsi 2:** Import online saat pertama kali  
1. Server menyediakan file GTFS terbaru
2. App mengunduh dan mengimpor saat install pertama

**Opsi 3:** Gunakan backend API  
1. Pertahankan backend Node.js sebagai API server
2. Android app hanya sebagai client REST API
3. Cocok jika ingin update data tanpa update app

---

## Penutup

Proyek web prototype ini membuktikan bahwa konsep **multimodal transit routing** dengan data GTFS lokal dan integrasi TomTom API dapat bekerja dengan baik untuk kasus Jakarta. Semua logika inti (Dijkstra, walking transfer, kalkulasi tarif) dapat diadaptasi langsung ke Kotlin dengan perubahan minimal pada algoritmanya.

Tantangan terbesar pada versi Android:
1. **Performa Dijkstra** pada ~8.000 node harus dioptimasi (pertimbangkan A* atau pra-komputasi)
2. **Ukuran database** (~29MB) perlu dikelola dengan baik agar tidak memberatkan ukuran APK
3. **Keakuratan data** terutama KRL — perlu validasi dan update periodik

Semoga sukses! 🚀
