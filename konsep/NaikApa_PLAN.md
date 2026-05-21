# NaikApa_PLAN.md

## Planning Pembuatan Aplikasi NaikApa

**Nama aplikasi:** NaikApa  
**Platform:** Android native  
**Bahasa:** Kotlin  
**Database:** SQLite lokal  
**Peta dan API:** osmdroid (CartoDB Map Tiles), TomTom Search REST API, TomTom Routing REST API  
**Data transit:** GTFS TransJakarta, KRL, MRT, dan LRT  
**Target MVP:** Aplikasi dapat mencari, menghitung, membandingkan, dan menampilkan rekomendasi rute multimoda di Jabodetabek berdasarkan prioritas pengguna.

---

## 1. Ringkasan Planning

NaikApa dikembangkan sebagai aplikasi Android rekomendasi transportasi multimoda. Fokus utamanya adalah membantu pengguna menjawab pertanyaan “sebaiknya naik apa?” berdasarkan lokasi awal, tujuan, moda transportasi, prioritas perjalanan, estimasi biaya, estimasi waktu, jarak jalan kaki, jumlah transit, dan laporan gangguan dari pengguna.

Pengembangan aplikasi dibagi menjadi beberapa fase agar pengerjaan lebih terarah. Fase awal berfokus pada fondasi proyek, database, data GTFS, dan integrasi API. Fase tengah berfokus pada algoritma rute, rekomendasi, dan peta. Fase akhir berfokus pada fitur CRUD, multimedia, pengujian, polishing UI, dan persiapan demo.

---

## 2. Prinsip Pengembangan

1. Kerjakan fitur inti terlebih dahulu sebelum fitur tambahan.
2. Pastikan setiap fase menghasilkan output yang bisa diuji.
3. Jangan langsung mengerjakan semua moda sekaligus.
4. Mulai dari alur paling sederhana, lalu naik ke rute gabungan.
5. Simpan data lokal yang wajib ke SQLite.
6. Gunakan koneksi internet hanya untuk memuat peta CartoDB (osmdroid) serta memanggil REST API TomTom (Search & Routing) secara langsung via Retrofit.
7. Gunakan GTFS lokal untuk rute transportasi umum.
8. Jika fitur terlalu berat, tetap prioritaskan fitur yang membuat aplikasi bisa didemokan dengan stabil.
9. UI dibuat modern minimalis, tetapi jangan sampai mengorbankan fungsi utama.
10. Setiap fase harus memiliki hasil akhir yang jelas.

---

## 3. Urutan Fase Pengembangan

## Fase 0: Persiapan Proyek dan Pembagian Scope ✅ SELESAI

### Tujuan
Menentukan fondasi awal agar seluruh anggota kelompok memahami arah aplikasi, fitur MVP, batasan, dan urutan pengerjaan.

### Aktivitas
1. Membaca PRD NaikApa secara menyeluruh.
2. Menentukan fitur yang wajib selesai untuk MVP.
3. Menentukan fitur yang hanya dikerjakan jika masih ada waktu.
4. Menentukan pembagian tugas anggota kelompok.
5. Menyiapkan daftar kebutuhan teknis.
6. Menyiapkan akun dan API key TomTom.
7. Menyiapkan aset awal seperti logo, ikon moda, dan warna aplikasi.
8. Menentukan style UI modern minimalis dengan gaya transport app.

### Output
1. [x] Sistem dapat membuat rute gabungan kendaraan pribadi ke titik transit, lanjut transportasi umum, lalu jalan kaki ke tujuan.
1. Scope MVP final.
2. Pembagian tugas kelompok.
3. API key TomTom tersedia.
4. Aset awal aplikasi tersedia.
5. Struktur folder kerja disepakati.

### Kriteria Selesai
Fase ini selesai jika semua anggota sudah memahami fitur yang dikerjakan, API key tersedia, dan proyek siap dibuat di Android Studio.

---

## Fase 1: Setup Project Android ✅ SELESAI (Sebagian — project dibuat, perlu dilengkapi dependency & struktur)

### Tujuan
Membuat project Android Kotlin sebagai dasar aplikasi NaikApa.

### Aktivitas
1. Membuat project baru di Android Studio.
2. Menentukan package name aplikasi.
3. Mengatur minimum SDK sesuai kebutuhan osmdroid (minSdk 26).
4. Menambahkan dependency utama.
5. Menyiapkan struktur package.
6. Menyiapkan tema warna, typography, dan komponen dasar UI.
7. Membuat halaman Splash Screen sementara.
8. Membuat navigasi dasar antar halaman.

### Dependency Utama
1. Kotlin.
2. SQLiteOpenHelper atau Room jika tetap diizinkan oleh dosen.
3. osmdroid SDK.
4. Retrofit atau OkHttp untuk API jika diperlukan.
5. Kotlin Coroutines untuk proses async.
6. Library permission untuk lokasi, kamera, dan galeri jika dibutuhkan.
7. Material Components untuk UI.

### Struktur Package yang Disarankan

```text
com.naikapa.app
├── data
│   ├── local
│   ├── remote
│   └── model
├── domain
│   ├── routing
│   ├── recommendation
│   └── util
├── presentation
│   ├── splash
│   ├── auth
│   ├── home
│   ├── route_result
│   ├── route_detail
│   ├── report
│   ├── favorite
│   ├── history
│   └── profile
└── common
    ├── constants
    ├── helper
    └── extension
```

### Output
1. Project Android Kotlin berhasil dibuat.
2. Aplikasi bisa dijalankan di emulator atau HP.
3. Splash Screen tampil.
4. Navigasi dasar antar halaman sudah siap.

### Kriteria Selesai
Fase ini selesai jika aplikasi dapat dibuka tanpa error dan struktur project sudah rapi.

---

## Fase 2: Desain Database SQLite Lokal ✅ SELESAI

### Tujuan
Membuat struktur database lokal untuk menyimpan user, profil, riwayat, favorit, laporan gangguan, dan data GTFS.

### Aktivitas
1. [x] Membuat database helper SQLite.
2. [x] Membuat tabel user.
3. [x] Membuat tabel profil user.
4. [x] Membuat tabel riwayat pencarian.
5. [x] Membuat tabel riwayat perjalanan.
6. [x] Membuat tabel perjalanan favorit.
7. [x] Membuat tabel laporan gangguan.
8. [x] Membuat tabel GTFS utama.
9. [x] Membuat query insert, select, update, dan delete.
10. [x] Membuat index untuk tabel GTFS agar pencarian lebih cepat.

### Tabel MVP
1. `users`
2. `user_profiles`
3. `gtfs_stops`
4. `gtfs_routes`
5. `gtfs_trips`
6. `gtfs_stop_times`
7. `saved_trips`
8. `search_history`
9. `route_history`
10. `disruption_reports`
11. `route_cache`

### Output
1. [x] File SQLite helper.
2. [x] Semua tabel MVP berhasil dibuat.
3. [x] Query CRUD dasar tersedia.
4. [x] Database bisa dibuat saat aplikasi pertama kali dijalankan.

### Kriteria Selesai
Fase ini selesai jika data dummy internal untuk user, favorit, history, dan laporan bisa dibuat, dibaca, diubah, dan dihapus lewat fungsi database.

Status implementasi: selesai. Unit test berhasil dijalankan, dan instrumentation test database berhasil dikompilasi. Eksekusi instrumentation test penuh tetap membutuhkan emulator atau perangkat Android aktif.

### Catatan Penting
Walaupun target aplikasi tidak memakai data dummy untuk rute, penggunaan data dummy kecil masih boleh untuk testing database internal. Data dummy ini hanya untuk pengujian awal, bukan data final aplikasi.

---

## Fase 3: Import dan Bundling Data GTFS ✅ SELESAI

### Status Planning
- [x] Plan eksekusi Fase 3 sudah dibuat di `.agent/planning/plan.md`.
- [x] Implementasi Fase 3 selesai dikerjakan.

### Tujuan
Menyiapkan data transportasi umum agar dapat digunakan aplikasi Android secara lokal.

### Aktivitas
1. [x] Mengumpulkan dan mengaudit data GTFS TransJakarta, KRL, MRT, dan LRT dari folder `data/gtfs*`.
2. [x] Menentukan field yang benar-benar dibutuhkan sesuai skema `gtfs_stops`, `gtfs_routes`, `gtfs_trips`, dan `gtfs_stop_times`.
3. [x] Membersihkan data yang tidak diperlukan agar ukuran database lebih ringan.
4. [x] Membuat script import GTFS ke SQLite pre-built.
5. [x] Membuat pre-built database `naikapa_gtfs.db`.
6. [x] Memasukkan file database ke folder assets Android.
7. [x] Membuat mekanisme copy database dari assets ke internal storage saat aplikasi pertama kali dibuka.
8. [x] Menambahkan query validasi dan pencarian halte/stasiun dari database lokal.
9. [x] Melakukan validasi isi tabel GTFS di Android.
10. [x] Menambahkan test untuk memastikan database GTFS bisa dibaca dan dicari.

### Field GTFS Minimum
1. `agency_id`
2. `route_id`
3. `route_short_name`
4. `route_long_name`
5. `trip_id`
6. `stop_id`
7. `stop_name`
8. `stop_lat`
9. `stop_lon`
10. `arrival_time`
11. `departure_time`
12. `stop_sequence`

### Output
1. [x] File database GTFS dalam format SQLite.
2. [x] Database dapat dibundel ke aplikasi.
3. [x] Aplikasi dapat membaca daftar halte dan stasiun.
4. [x] Aplikasi dapat mencari halte atau stasiun berdasarkan nama.

### Kriteria Selesai
- [x] Fungsi pencarian internal via `GtfsDao` dapat menampilkan halte atau stasiun dari GTFS lokal.
- [x] `GtfsDao.searchStops()` dapat menemukan contoh stop seperti Dukuh Atas, Stasiun Tangerang, atau halte TransJakarta.
- [x] Validasi database tidak menemukan stop aktif tanpa koordinat dan relasi dasar GTFS rusak.

Status implementasi: selesai. Database pre-built `app/src/main/assets/databases/naikapa_gtfs.db` berhasil dibuat dari data lokal GTFS dengan 8.336 stop, 262 route, 2.831 trip, dan 55.293 stop time. Validasi script berhasil, unit test berhasil, serta APK debug dan androidTest APK berhasil dikompilasi.

### Risiko
1. Ukuran database terlalu besar.
2. Data tidak konsisten.
3. Ada stop yang tidak memiliki koordinat.
4. Ada trip yang tidak lengkap.

### Mitigasi
1. Simpan hanya kolom yang dibutuhkan.
2. Buat index pada `stop_id`, `route_id`, `trip_id`, dan `stop_sequence`.
3. Validasi koordinat sebelum digunakan.
4. Gunakan subset data dulu untuk uji coba.

---

## Fase 4: Login, Register, dan Profil Pengguna

### Status Planning
- [x] Plan eksekusi Fase 4 sudah dibuat di `.agent/planning/plan.md`.
- [x] Implementasi Fase 4 selesai dikerjakan.

### Tujuan
Membuat fitur akun lokal agar user bisa menyimpan data pribadi, status kendaraan, riwayat, favorit, dan laporan.

### Aktivitas
1. [x] Membuat halaman Login.
2. [x] Membuat halaman Register.
3. [x] Membuat validasi input.
4. [x] Menyimpan data user ke SQLite.
5. [x] Membuat session lokal sederhana.
6. [x] Membuat halaman Profil.
7. [x] Membuat fitur update data profil.
8. [x] Membuat fitur update status memiliki motor dan mobil.
9. [x] Membuat fitur delete akun lokal.

### Data User
1. Nama.
2. Email.
3. Password.
4. Status memiliki motor.
5. Status memiliki mobil.

### Output
1. [x] User dapat register.
2. [x] User dapat login.
3. [x] User dapat melihat profil.
4. [x] User dapat mengubah profil.
5. [x] User dapat menghapus akun lokal.

### Kriteria Selesai
Fase ini selesai jika CRUD data pengguna berjalan stabil di SQLite.

Status implementasi: selesai. Login dan register sudah memakai `UserDao`/SQLite, session menyimpan user id asli, profil membaca data user aktif dari SQLite, update nama/email/password/status kendaraan tersedia, dan delete akun lokal membersihkan session. Unit test dan build debug berhasil dijalankan. Instrumentation test berhasil dikompilasi, tetapi eksekusi penuh belum berjalan karena tidak ada emulator atau perangkat Android tersambung.

---

## Fase 5: Integrasi GPS dan Permission Dasar

### Status Planning
- [x] Plan eksekusi Fase 5 sudah dibuat di `.agent/planning/plan.md`.
- [x] Implementasi Fase 5 selesai dikerjakan.

### Tujuan
Mengambil lokasi awal pengguna dari GPS perangkat.

### Aktivitas
1. [x] Menambahkan permission lokasi di Android Manifest.
2. [x] Membuat runtime permission untuk lokasi.
3. [x] Mengambil koordinat lokasi user.
4. [x] Menampilkan nama sementara seperti “Lokasi Saya”.
5. [x] Menangani kondisi permission ditolak.
6. [x] Menangani kondisi GPS mati.
7. [x] Menyimpan koordinat lokasi awal untuk proses routing.

### Output
1. [x] Aplikasi dapat meminta izin lokasi.
2. [x] Aplikasi dapat mengambil latitude dan longitude user.
3. [x] Lokasi user dapat digunakan sebagai titik asal.

### Kriteria Selesai
Fase ini selesai jika tombol “Gunakan Lokasi Saya” berhasil mengisi titik asal dengan koordinat GPS.

Status implementasi: selesai. Tombol lokasi di Home sudah meminta runtime permission, mengecek layanan lokasi aktif, mengambil koordinat via Fused Location Provider, mengisi titik asal dengan label “Lokasi saya” dan koordinat, menyimpan state asal sebagai `LocationPoint`, serta menggeser peta ke posisi user. Unit test dan build debug berhasil dijalankan. Instrumentation test berhasil dikompilasi, tetapi eksekusi penuh belum berjalan karena emulator `emulator-5562` berstatus OFFLINE.

---

## Fase 6: Integrasi osmdroid & CartoDB Map Tiles

### Status Planning
- [x] Plan eksekusi Fase 6 sudah dibuat di `.agent/planning/plan.md`.
- [x] Implementasi Fase 6 selesai dikerjakan.

### Tujuan
Menampilkan peta interaktif yang elegan berbasis CartoDB sebagai visual utama aplikasi.

### Aktivitas
1. [x] Menambahkan osmdroid SDK ke dalam dependencies.
2. [x] Mengatur konfigurasi user-agent osmdroid.
3. [x] Menampilkan MapView berbasis osmdroid di layout Fragment.
4. [x] Mengatur Tile Source kustom ke CartoDB Positron / Dark Matter.
5. [x] Menampilkan marker lokasi awal.
6. [x] Menampilkan marker tujuan.
7. [x] Mengatur camera position/zoom ke area Jabodetabek.
8. [x] Menyiapkan fungsi menggambar polyline rute di atas MapView.

### Output
1. [x] Peta CartoDB tampil di aplikasi dengan mulus.
2. [x] Marker dapat ditampilkan secara interaktif.
3. [x] Polyline rute dapat digambar secara dinamis.
4. [x] Peta siap dipakai pada halaman Home dan Detail Rute.

### Kriteria Selesai
Fase ini selesai jika peta dapat menampilkan lokasi awal, tujuan, dan garis rute sederhana.

Status implementasi: selesai. Home sudah memakai user-agent osmdroid eksplisit, mendukung CartoDB Positron dan Dark Matter, memiliki helper marker asal/tujuan/transit, helper polyline rute, clear overlay agar tidak menumpuk, serta preview rute sederhana dari tombol “Temukan Rute”. Unit test dan build debug berhasil dijalankan. Instrumentation test berhasil dikompilasi, tetapi eksekusi penuh belum berjalan karena tidak ada device tersambung.

---

## Fase 7: Integrasi TomTom Search API

### Status Planning
- [x] Plan eksekusi Fase 7 sudah dibuat di `.agent/planning/plan.md`.
- [x] Implementasi Fase 7 selesai dikerjakan.

### Tujuan
Membuat fitur pencarian tujuan berdasarkan alamat, tempat, atau nama daerah.

### Aktivitas
1. [x] Membuat input pencarian tujuan.
2. [x] Membuat request ke TomTom Search API.
3. [x] Menampilkan daftar hasil pencarian.
4. [x] Menampilkan nama lokasi, alamat, dan koordinat.
5. [x] User dapat memilih salah satu hasil.
6. [x] Menyimpan hasil pilihan ke state aplikasi.
7. [x] Menyimpan keyword dan hasil pencarian ke `search_history`.

### Output
1. [x] User dapat mengetik tujuan.
2. [x] Aplikasi menampilkan hasil pencarian TomTom.
3. [x] User dapat memilih lokasi tujuan.
4. [x] Riwayat pencarian tersimpan di SQLite.

### Kriteria Selesai
Fase ini selesai jika user dapat memilih tujuan dari hasil TomTom dan koordinat tujuan siap digunakan untuk routing.

Status implementasi: selesai. Home sudah memiliki input pencarian tujuan, debounce request ke TomTom Fuzzy Search via Retrofit, daftar hasil, pemilihan tujuan, marker tujuan di peta, state `selectedDestination`, dan penyimpanan pilihan ke `search_history` untuk user aktif. Unit test dan build debug berhasil dijalankan. Instrumentation test berhasil dikompilasi, tetapi eksekusi penuh belum berjalan karena emulator `emulator-5562` berstatus OFFLINE.

---

## Fase 8: Search Halte dan Stasiun dari GTFS

### Status Planning
- [x] Plan eksekusi Fase 8 sudah dibuat di `.agent/planning/plan.md`.

### Tujuan
Membuat pencarian lokasi berbasis data GTFS lokal untuk halte dan stasiun.

### Aktivitas
1. Membuat fungsi pencarian `gtfs_stops`.
2. Membuat filter berdasarkan moda.
3. Menampilkan hasil pencarian halte atau stasiun.
4. Menampilkan nama, agency, dan jarak dari lokasi user jika tersedia.
5. User dapat memilih halte atau stasiun sebagai asal atau tujuan.
6. Menyatukan hasil pencarian GTFS dan TomTom dalam UI search.

### Output
1. User dapat mencari halte dan stasiun.
2. User dapat memilih titik transportasi sebagai asal atau tujuan.
3. Data stop GTFS siap digunakan untuk algoritma rute.

### Kriteria Selesai
Fase ini selesai jika pencarian “Stasiun Tangerang”, “Dukuh Atas”, atau halte TransJakarta dapat menampilkan hasil dari database lokal.

---

## Fase 9: Membangun Graph Transportasi Umum

### Status Planning
- [x] Plan eksekusi Fase 9 sudah dibuat di `.agent/planning/plan.md`.
- [x] Implementasi Fase 9 selesai dikerjakan.

### Tujuan
Membangun graph rute transportasi umum dari data GTFS.

### Aktivitas
1. [x] Mengambil pasangan stop berurutan dari `stop_times`.
2. [x] Menghubungkan `stop_times` dengan `trips` dan `routes`.
3. [x] Menghitung durasi antar stop.
4. [x] Membuat model `Node`, `Edge`, dan `RouteSegment`.
5. [x] Membuat graph di memori.
6. [x] Membuat edge antar stop dalam rute yang sama.
7. [x] Membuat walking transfer antar titik transportasi yang dekat.
8. [x] Menyimpan cache graph jika diperlukan.

### Model Sederhana

```text
Node = stop atau stasiun
Edge = koneksi dari satu stop ke stop lain
Weight = biaya perhitungan berdasarkan prioritas
```

### Output
1. [x] Graph transportasi umum berhasil dibuat.
2. [x] Setiap stop aktif memiliki daftar edge.
3. [x] Walking transfer antar moda tersedia.
4. [x] Graph siap dipakai oleh algoritma Dijkstra.

### Kriteria Selesai
Fase ini selesai jika aplikasi dapat membangun graph tanpa crash dan jumlah node serta edge dapat ditampilkan di log.

Status implementasi: selesai. `GtfsDao` sudah memiliki query adjacency GTFS dari pasangan stop berurutan, model graph domain sudah tersedia, graph builder membuat edge transit dan walking transfer, durasi GTFS mendukung waktu lintas hari dan format di atas 24 jam, serta `TransitGraphRepository` menyediakan cache in-memory. Unit test, build debug, dan compile androidTest berhasil dijalankan.

### Risiko
Graph terlalu besar dan membuat aplikasi lambat.

### Mitigasi
1. Build graph saat diperlukan.
2. Gunakan cache.
3. Gunakan index database.
4. Batasi area uji coba terlebih dahulu.

---

## Fase 10: Implementasi Dijkstra Multimodal

### Status Planning
- [x] Plan eksekusi Fase 10 sudah dibuat di `.agent/planning/plan.md`.
- [x] Implementasi Fase 10 selesai dikerjakan.

### Tujuan
Menghitung rute transportasi umum berdasarkan graph GTFS.

### Aktivitas
1. [x] Membuat class `DijkstraAlgorithm`.
2. [x] Membuat enum `TransitMode`.
3. [x] Membuat enum `SortPreference`.
4. [x] Membuat fungsi filter edge berdasarkan moda.
5. [x] Membuat fungsi bobot untuk tercepat.
6. [x] Membuat fungsi bobot untuk terhemat.
7. [x] Membuat fungsi bobot untuk minim jalan kaki.
8. [x] Membuat fungsi bobot untuk minim transit.
9. [x] Membuat rekonstruksi path dari start ke end.
10. [x] Mengubah path menjadi langkah perjalanan yang mudah dibaca.

### Prioritas yang Didukung
1. Tercepat.
2. Terhemat.
3. Minim jalan kaki.
4. Minim transit.

### Output
1. [x] Sistem dapat menghitung rute transportasi umum.
2. [x] Sistem dapat menampilkan langkah perjalanan.
3. [x] Sistem dapat menghitung estimasi waktu, jarak jalan kaki, dan transit. Estimasi biaya detail dilanjutkan pada Fase 11.
4. [x] Sistem dapat mencari minimal satu rute valid.

### Kriteria Selesai
Fase ini selesai jika user dapat memilih asal dan tujuan berupa halte atau stasiun, lalu aplikasi menampilkan rute transportasi umum.

Status implementasi: selesai. `DijkstraAlgorithm` sudah berjalan di atas `TransitGraph`, filter `TransitMode` dan `SortPreference` tersedia, path direkonstruksi menjadi `RouteStep`, metrics dasar dihitung, dan `TransitRoutingRepository` menjadi entry point dari UI. Home sudah bisa mencoba rute transit jika asal dan tujuan berasal dari stop GTFS. Unit test dan build perlu divalidasi ulang setelah perubahan ini.

---

## Fase 11: Kalkulasi Tarif dan Estimasi Biaya

### Status Planning
- [x] Plan eksekusi Fase 11 sudah dibuat di `.agent/planning/plan.md`.
- [x] Implementasi Fase 11 selesai dikerjakan.

### Tujuan
Menghitung estimasi biaya transportasi umum dan estimasi BBM kendaraan pribadi.

### Aktivitas
1. [x] Membuat class `FareCalculator`.
2. [x] Membuat tarif TransJakarta.
3. [x] Membuat tarif KRL progresif sederhana.
4. [x] Membuat tarif MRT.
5. [x] Membuat tarif LRT.
6. [x] Menghitung total biaya rute campuran transportasi umum.
7. [x] Membuat class `FuelCostCalculator`.
8. [x] Menghitung estimasi BBM motor.
9. [x] Menghitung estimasi BBM mobil.
10. [x] Menampilkan biaya pada hasil rekomendasi.

### Output
1. [x] Estimasi tarif transportasi umum tersedia.
2. [x] Estimasi BBM motor dan mobil tersedia.
3. [x] Total biaya rute dapat dibandingkan.

### Kriteria Selesai
Fase ini selesai jika setiap hasil rute memiliki estimasi biaya yang masuk akal.

Status implementasi: selesai. `RouteMetrics` sudah membawa `estimatedFare`, `estimatedBbm`, dan `estimatedTotalCost`; `FareCalculator` menghitung tarif TJ, KRL, MRT, LRTJ, dan LRTJB; `FuelCostCalculator` menghitung estimasi BBM motor/mobil; Home menampilkan estimasi biaya transit dalam format Rupiah. Unit test, build debug, dan compile androidTest berhasil dijalankan.

---

## Fase 12: Integrasi TomTom Routing untuk Motor dan Mobil

### Status Planning
- [x] Plan eksekusi Fase 12 sudah dibuat di `.agent/planning/plan.md`.
- [x] Implementasi Fase 12 selesai dikerjakan.

### Tujuan
Menghitung rute kendaraan pribadi menggunakan TomTom Routing API.

### Aktivitas
1. [x] Membuat service TomTom Routing.
2. [x] Membuat request rute motor.
3. [x] Membuat request rute mobil.
4. [x] Mengambil jarak dan waktu dari response.
5. [x] Mengambil titik polyline dari response.
6. [x] Menampilkan rute kendaraan pribadi pada peta.
7. [x] Menghitung estimasi BBM.
8. [x] Menyediakan alternatif rute jika response mendukung.

### Output
1. [x] User dapat mencari rute motor.
2. [x] User dapat mencari rute mobil.
3. [x] Aplikasi menampilkan estimasi waktu, jarak, BBM, dan polyline.
4. [x] Aplikasi dapat membandingkan rute kendaraan pribadi dengan transportasi umum.

### Kriteria Selesai
Fase ini selesai jika input asal dan tujuan koordinat dapat menghasilkan rute motor atau mobil yang terlihat di peta.

Status implementasi: selesai. `TomTomRoutingApi` dan `TomTomRoutingRepository` sudah tersedia untuk mode motor/mobil, response TomTom Routing dipetakan ke hasil kendaraan pribadi dengan jarak, waktu, estimasi BBM, total biaya, dan polyline. Home menjalankan flow kendaraan pribadi saat mode Motor/Mobil dipilih, menampilkan marker, polyline, estimasi waktu, biaya/BBM, walking 0 m, dan transit 0x. Unit test, build debug, dan compile androidTest berhasil dijalankan.

---

## Fase 13: Rute Gabungan Kendaraan Pribadi dan Transportasi Umum

### Status Planning
- [x] Plan eksekusi Fase 13 sudah dibuat di `.agent/planning/plan.md`.
- [x] Implementasi Fase 13 selesai dikerjakan.

### Tujuan
Mengimplementasikan fitur utama yang membuat NaikApa lebih unggul, yaitu gabungan kendaraan pribadi dan transportasi umum.

### Aktivitas
1. [x] Mencari beberapa titik GTFS terdekat dari lokasi user.
2. [x] Mencari beberapa titik GTFS terdekat dari tujuan.
3. [x] Menghitung rute motor atau mobil dari lokasi user ke titik GTFS awal.
4. [x] Menghitung rute transit dari titik GTFS awal ke titik GTFS akhir.
5. [x] Menghitung jalan kaki dari titik GTFS akhir ke tujuan.
6. [x] Menggabungkan semua segmen.
7. [x] Menghitung total waktu, biaya, BBM, jalan kaki, dan transit.
8. [x] Membuat hasil rute gabungan.
9. [x] Menampilkan hasil sebagai kandidat rekomendasi.

### Strategi Agar Tidak Terlalu Berat
1. Ambil maksimal 3 sampai 5 titik transportasi terdekat dari asal.
2. Ambil maksimal 3 sampai 5 titik transportasi terdekat dari tujuan.
3. Jangan mencoba semua kombinasi tanpa batas.
4. Prioritaskan titik yang jaraknya realistis.
5. Cache hasil TomTom sementara jika origin dan destination sama.

### Output
1. [x] Sistem dapat membuat rute gabungan kendaraan pribadi ke titik transit, lanjut transportasi umum, lalu jalan kaki ke tujuan.
2. [x] Sistem dapat membandingkan rute gabungan dengan rute kendaraan pribadi penuh dan transportasi umum penuh.
3. [x] Sistem dapat menampilkan langkah perjalanan gabungan.

### Kriteria Selesai
Fase ini selesai jika aplikasi bisa menghasilkan minimal satu rute gabungan yang valid dari lokasi user ke tujuan.

Status implementasi: selesai. `NearbyTransitStopRepository` mencari kandidat stop GTFS terdekat dengan batas radius dan limit kandidat, `CombinedRouteRepository` menggabungkan segmen kendaraan pribadi TomTom, transit Dijkstra, dan jalan kaki akhir, lalu menghitung total waktu, biaya, BBM, walking, dan transit. Home menjalankan flow rute gabungan untuk asal GPS/koordinat ke tujuan koordinat pada mode campur/transit, menampilkan marker transit, polyline kendaraan/transit/walking, serta ringkasan rekomendasi. Unit test dan build debug berhasil dijalankan.

---

## Fase 14: Sistem Skor Kecocokan dan Rekomendasi

### Status Planning
- [x] Plan eksekusi Fase 14 sudah dibuat di `.agent/planning/plan.md`.
- [x] Implementasi Fase 14 selesai dikerjakan.

### Tujuan
Mengurutkan semua kandidat rute menjadi rekomendasi utama dan alternatif.

### Aktivitas
1. [x] Membuat class `RecommendationEngine`.
2. [x] Mengumpulkan kandidat rute dari transportasi umum, kendaraan pribadi, dan rute gabungan.
3. [x] Menghitung skor kecocokan.
4. [x] Memberi penalti waktu, biaya, jalan kaki, transit, dan gangguan.
5. [x] Memberi bonus sesuai prioritas user.
6. [x] Mengurutkan hasil dari skor tertinggi.
7. [x] Memilih rekomendasi utama, alternatif 1, dan alternatif 2.
8. [x] Membuat alasan rekomendasi secara otomatis.

### Output
1. [x] Rekomendasi utama tersedia.
2. [x] Alternatif 1 dan 2 tersedia.
3. [x] Skor kecocokan tampil.
4. [x] Alasan rekomendasi tampil.
5. [x] Prioritas user memengaruhi urutan hasil.

### Kriteria Selesai
Fase ini selesai jika perubahan prioritas dari tercepat ke terhemat atau minim jalan kaki dapat mengubah urutan rekomendasi dengan logis.

Status implementasi: selesai. `RecommendationEngine` mengumpulkan kandidat dari transit, kendaraan pribadi, dan gabungan secara paralel, `RecommendationScorer` menghitung skor 0–100 dengan bobot per prioritas dan penalti gangguan, `RecommendationReasonBuilder` menghasilkan alasan rekomendasi otomatis dalam Bahasa Indonesia, dan `RouteResultAdapter` menampilkan card rekomendasi utama + alternatif di RecyclerView. Unit test (10 test case) dan build debug berhasil dijalankan.

---

## Fase 15: UI Home dan Form Pencarian Rute

### Status Planning
- [x] Plan eksekusi Fase 15 sudah dibuat di `.agent/planning/plan.md`.
- [x] Implementasi Fase 15 selesai dikerjakan (UI/UX Polishing).

### Tujuan
Membuat halaman utama aplikasi yang nyaman digunakan.

### Aktivitas
1. [x] Membuat layout Home.
2. [x] Membuat pilihan moda.
3. [x] Membuat pilihan prioritas.
4. [x] Membuat input lokasi asal.
5. [x] Membuat tombol gunakan GPS.
6. [x] Membuat input tujuan.
7. [x] Membuat tombol cari rute.
8. [x] Membuat loading state.
9. [x] Membuat error state jika rute tidak ditemukan.
10. [x] Membuat desain modern minimalis dengan gaya transport app.

### Output
1. [x] Home siap digunakan.
2. [x] User dapat memilih moda dan prioritas.
3. [x] User dapat mengisi asal dan tujuan.
4. [x] User dapat memulai proses cari rute.

### Kriteria Selesai
Fase ini selesai jika user dapat menjalankan alur pencarian dari Home sampai ke halaman hasil.

Catatan: Sebagian besar komponen fungsional Home sudah diimplementasikan di fase sebelumnya (Fase 5–14). Yang tersisa untuk Fase 15 adalah polishing UX: loading indicator visual, error state banner, empty state, chip sort visual feedback, dan scroll behavior card rekomendasi.

---

## Fase 16: UI Hasil Rekomendasi dan Detail Rute

### Status Planning
- [x] Plan eksekusi Fase 16 sebagian sudah diimplementasikan di Fase 14.

### Tujuan
Menampilkan hasil rute secara jelas dan mudah dipahami.

### Aktivitas
1. [x] Membuat card rekomendasi utama.
2. [x] Membuat card alternatif 1 dan 2.
3. [x] Menampilkan skor kecocokan.
4. [x] Menampilkan estimasi waktu.
5. [x] Menampilkan estimasi biaya.
6. [x] Menampilkan estimasi BBM.
7. [x] Menampilkan jarak jalan kaki.
8. [x] Menampilkan jumlah transit.
9. [x] Menampilkan alasan rekomendasi.
10. [x] Menampilkan warning jika ada gangguan.
11. Membuat halaman detail langkah perjalanan.
12. [x] Menampilkan peta dan polyline.

### Output
1. [x] Hasil rekomendasi tampil rapi.
2. [x] User dapat membandingkan pilihan rute.
3. User dapat melihat detail rute.
4. [x] User dapat membuka peta rute.

### Kriteria Selesai
Fase ini selesai jika hasil rekomendasi utama dan alternatif dapat ditampilkan lengkap.

Catatan: Card rekomendasi (utama + alternatif), skor, alasan, warning gangguan, dan peta sudah diimplementasikan di Fase 14. Yang tersisa adalah halaman detail langkah perjalanan (step-by-step route detail page).

---

## Fase 17: Fitur Laporan Gangguan dan Multimedia Foto

### Tujuan
Membuat fitur laporan gangguan berbasis kontribusi user dan multimedia.

### Aktivitas
1. Membuat halaman Status Gangguan.
2. Membuat halaman Tambah Laporan Gangguan.
3. Menampilkan daftar titik transportasi terkait.
4. Membuat pilihan kategori gangguan.
5. Membuat input deskripsi.
6. Menambahkan fitur kamera.
7. Menambahkan fitur pilih gambar dari galeri.
8. Menyimpan path foto ke SQLite.
9. Menyimpan laporan ke SQLite.
10. Membuat laporan aktif selama 1 jam.
11. Membuat laporan memengaruhi rekomendasi sebagai penalti ringan.
12. Membuat fitur edit dan hapus laporan milik user.

### Output
1. User dapat membuat laporan gangguan.
2. User dapat melampirkan foto.
3. User dapat melihat gangguan aktif.
4. Gangguan aktif dapat memengaruhi skor rute.
5. CRUD laporan berjalan.

### Kriteria Selesai
Fase ini selesai jika laporan dengan foto dapat dibuat, ditampilkan, diedit, dihapus, dan memengaruhi rekomendasi.

---

## Fase 18: Perjalanan Favorit dan Riwayat

### Tujuan
Menyimpan data perjalanan yang sering digunakan dan hasil pencarian user.

### Aktivitas
1. Membuat fitur simpan rute ke favorit.
2. Membuat halaman Perjalanan Favorit.
3. Membuat fitur edit nama favorit.
4. Membuat fitur hapus favorit.
5. Membuat fitur jalankan ulang pencarian dari favorit.
6. Membuat penyimpanan riwayat pencarian.
7. Membuat penyimpanan riwayat perjalanan.
8. Membuat halaman Riwayat Pencarian.
9. Membuat halaman Riwayat Perjalanan.
10. Membuat fitur hapus riwayat.

### Output
1. User dapat menyimpan rute favorit.
2. User dapat melihat dan mengelola favorit.
3. User dapat melihat riwayat pencarian.
4. User dapat melihat riwayat perjalanan.
5. CRUD favorit dan riwayat berjalan.

### Kriteria Selesai
Fase ini selesai jika favorit dan riwayat tersimpan di SQLite dan dapat dikelola user.

---

## Fase 19: Polishing UI dan UX

### Tujuan
Merapikan tampilan aplikasi agar layak dipresentasikan dan nyaman digunakan.

### Aktivitas
1. Menyamakan warna aplikasi.
2. Menyamakan typography.
3. Merapikan spacing dan alignment.
4. Menambahkan ikon moda.
5. Menambahkan empty state.
6. Menambahkan loading state.
7. Menambahkan error state.
8. Menambahkan dialog konfirmasi hapus.
9. Menambahkan pesan sukses.
10. Menyesuaikan tampilan untuk beberapa ukuran layar.
11. Merapikan halaman hasil rekomendasi.
12. Merapikan tampilan peta.

### Output
1. Aplikasi terlihat modern dan konsisten.
2. Alur user lebih mudah dipahami.
3. Aplikasi siap diuji dan didemokan.

### Kriteria Selesai
Fase ini selesai jika semua halaman MVP memiliki tampilan yang konsisten dan tidak berantakan.

---

## Fase 20: Testing Fungsional

### Tujuan
Memastikan semua fitur utama berjalan sesuai rencana.

### Aktivitas
1. Test register.
2. Test login.
3. Test update profil.
4. Test GPS.
5. Test TomTom Search.
6. Test TomTom Routing motor.
7. Test TomTom Routing mobil.
8. Test pencarian halte atau stasiun GTFS.
9. Test Dijkstra transportasi umum.
10. Test rute gabungan.
11. Test skor rekomendasi.
12. Test peta dan polyline.
13. Test laporan gangguan dengan foto.
14. Test favorit.
15. Test riwayat.
16. Test kondisi tanpa internet.
17. Test permission ditolak.
18. Test rute tidak ditemukan.

### Output
1. Daftar bug.
2. Daftar fitur yang sudah lolos.
3. Daftar fitur yang perlu diperbaiki.
4. Catatan untuk demo.

### Kriteria Selesai
Fase ini selesai jika semua fitur MVP dapat berjalan tanpa crash pada skenario utama.

---

## Fase 21: Optimasi dan Perbaikan Bug

### Tujuan
Memperbaiki bug dan meningkatkan stabilitas aplikasi.

### Aktivitas
1. Memperbaiki crash.
2. Mengoptimalkan query database.
3. Menambahkan index jika query lambat.
4. Mengurangi beban build graph.
5. Mengoptimalkan pemanggilan API.
6. Menangani timeout TomTom API.
7. Menangani hasil search kosong.
8. Menangani rute tidak ditemukan.
9. Menangani laporan yang sudah expired.
10. Membersihkan kode yang tidak dipakai.

### Output
1. Aplikasi lebih stabil.
2. Rute lebih cepat dihitung.
3. Error lebih jelas untuk user.
4. Kode lebih rapi.

### Kriteria Selesai
Fase ini selesai jika aplikasi stabil untuk demo dan bug kritis sudah diperbaiki.

---

## Fase 22: Persiapan Demo dan Presentasi

### Tujuan
Menyiapkan aplikasi agar siap dikumpulkan dan dipresentasikan.

### Aktivitas
1. Menentukan skenario demo utama.
2. Menyiapkan akun demo.
3. Menyiapkan lokasi asal dan tujuan contoh.
4. Menyiapkan contoh rute transportasi umum.
5. Menyiapkan contoh rute motor atau mobil.
6. Menyiapkan contoh rute gabungan.
7. Menyiapkan contoh laporan gangguan.
8. Menyiapkan screenshot aplikasi.
9. Menyiapkan penjelasan masalah dan solusi.
10. Menyiapkan penjelasan database, CRUD, dan multimedia.
11. Menyiapkan file APK atau project final.
12. Menyiapkan backup demo jika internet bermasalah.

### Skenario Demo yang Disarankan
1. Login ke aplikasi.
2. Buka Home.
3. Gunakan lokasi GPS.
4. Cari tujuan dengan TomTom Search.
5. Pilih mode Campur Semua.
6. Pilih prioritas Tercepat atau Terhemat.
7. Tampilkan rekomendasi utama dan alternatif.
8. Buka detail rute.
9. Tampilkan peta dan polyline.
10. Buat laporan gangguan dengan foto.
11. Tunjukkan gangguan muncul di Status Gangguan.
12. Simpan rute ke favorit.
13. Tunjukkan riwayat perjalanan.

### Output
1. Aplikasi siap demo.
2. Skenario demo siap.
3. File APK atau project siap dikumpulkan.
4. Dokumentasi singkat siap.

### Kriteria Selesai
Fase ini selesai jika aplikasi dapat didemokan dari awal sampai akhir tanpa error besar.

---

## 4. Timeline Saran Pengerjaan

Timeline berikut dapat disesuaikan dengan deadline kelompok.

| Fase | Fokus | Estimasi |
|---|---|---|
| Fase 0 | Persiapan scope dan API | 0.5 hari |
| Fase 1 | Setup project Android | 0.5 sampai 1 hari |
| Fase 2 | Database SQLite | 1 sampai 2 hari |
| Fase 3 | Import dan bundling GTFS | 1 sampai 3 hari |
| Fase 4 | Login, register, profil | 1 hari |
| Fase 5 | GPS dan permission | 0.5 sampai 1 hari |
| Fase 6 | osmdroid & CartoDB | 1 sampai 2 hari |
| Fase 7 | TomTom Search API | 1 hari |
| Fase 8 | Search halte dan stasiun GTFS | 1 hari |
| Fase 9 | Graph transportasi umum | 2 sampai 4 hari |
| Fase 10 | Dijkstra multimodal | 2 sampai 5 hari |
| Fase 11 | Tarif dan estimasi biaya | 1 sampai 2 hari |
| Fase 12 | TomTom Routing motor dan mobil | 1 sampai 2 hari |
| Fase 13 | Rute gabungan | 3 sampai 6 hari |
| Fase 14 | Skor kecocokan | 1 sampai 2 hari |
| Fase 15 | UI Home | 1 sampai 2 hari |
| Fase 16 | UI hasil dan detail rute | 1 sampai 3 hari |
| Fase 17 | Laporan gangguan dan foto | 1 sampai 3 hari |
| Fase 18 | Favorit dan riwayat | 1 sampai 2 hari |
| Fase 19 | Polishing UI | 1 sampai 2 hari |
| Fase 20 | Testing | 1 sampai 2 hari |
| Fase 21 | Bug fixing | 1 sampai 3 hari |
| Fase 22 | Persiapan demo | 0.5 sampai 1 hari |

---

## 5. Prioritas Pengerjaan Jika Waktu Mepet

Jika waktu pengerjaan ternyata sempit, gunakan urutan prioritas berikut.

### Prioritas 1: Wajib Jalan
1. Login dan register lokal.
2. Home.
3. Search tujuan TomTom.
4. GPS lokasi user.
5. Peta osmdroid (CartoDB).
6. Rute motor atau mobil dari TomTom REST API.
7. Riwayat pencarian.
8. Favorit.
9. Laporan gangguan dengan foto.

### Prioritas 2: Core NaikApa
1. Data GTFS lokal.
2. Search halte dan stasiun.
3. Graph transportasi umum.
4. Dijkstra transportasi umum.
5. Hasil rekomendasi utama dan alternatif.
6. Skor kecocokan.
7. Alasan rekomendasi.

### Prioritas 3: Fitur Ambisius
1. Rute gabungan motor ke stasiun lalu transit.
2. Rute campur semua.
3. Penalti gangguan ke algoritma.
4. Optimasi graph.
5. Polyline transportasi umum dari shapes GTFS.

### Prioritas 4: Tambahan Jika Sempat
1. Filter hindari tol.
2. Mode gelap dan terang.
3. Onboarding.
4. Detail fasilitas halte atau stasiun.
5. Notifikasi gangguan.

---

## 6. Pembagian Tugas Kelompok yang Disarankan

### Anggota 1: Database dan GTFS
Tugas:
1. SQLite helper.
2. Struktur tabel.
3. Import GTFS.
4. Query halte dan stasiun.
5. Optimasi index.
6. Data testing.

### Anggota 2: Algoritma dan Rekomendasi
Tugas:
1. Graph transportasi umum.
2. Walking transfer.
3. Dijkstra.
4. Fare calculator.
5. Recommendation engine.
6. Skor kecocokan.

### Anggota 3: API, Peta, dan Lokasi
Tugas:
1. osmdroid SDK dengan CartoDB tiles.
2. TomTom Search REST API via Retrofit.
3. TomTom Routing REST API via Retrofit.
4. GPS.
5. Marker dan polyline.
6. Permission lokasi.

### Anggota 4: UI, CRUD, dan Multimedia
Tugas:
1. Login dan Register.
2. Home.
3. Hasil rekomendasi.
4. Detail rute.
5. Laporan gangguan.
6. Kamera dan galeri.
7. Favorit dan riwayat.
8. Polishing UI.

Jika kelompok hanya 3 orang, gabungkan tugas UI dengan API atau database sesuai kemampuan anggota.

---

## 7. Checkpoint Hasil Akhir per Tahap

### Checkpoint 1: Aplikasi Bisa Dibuka
Kondisi:
1. Project Android berjalan.
2. Splash Screen tampil.
3. Navigasi dasar bisa digunakan.

### Checkpoint 2: Database dan Akun Berjalan
Kondisi:
1. Register berhasil.
2. Login berhasil.
3. Profil tersimpan di SQLite.
4. Status motor dan mobil tersimpan.

### Checkpoint 3: Peta dan Search Berjalan
Kondisi:
1. GPS terbaca.
2. Peta osmdroid & CartoDB tampil.
3. TomTom Search API via Retrofit bisa mencari tujuan.
4. Marker asal dan tujuan tampil.

### Checkpoint 4: Transportasi Umum Berjalan
Kondisi:
1. GTFS bisa dibaca.
2. Halte atau stasiun bisa dicari.
3. Graph dapat dibuat.
4. Dijkstra menghasilkan rute transit.

### Checkpoint 5: Kendaraan Pribadi Berjalan
Kondisi:
1. Rute motor dapat dihitung.
2. Rute mobil dapat dihitung.
3. Estimasi BBM tampil.
4. Polyline kendaraan pribadi tampil.

### Checkpoint 6: Rekomendasi NaikApa Berjalan
Kondisi:
1. Rekomendasi utama tampil.
2. Alternatif tampil.
3. Skor kecocokan tampil.
4. Alasan rekomendasi tampil.
5. Prioritas memengaruhi hasil.

### Checkpoint 7: Rute Gabungan Berjalan
Kondisi:
1. Sistem menemukan stasiun atau halte terdekat.
2. Sistem menghitung kendaraan pribadi ke titik transit.
3. Sistem menghitung transit ke titik akhir.
4. Sistem menggabungkan hasil menjadi satu rute.

### Checkpoint 8: CRUD dan Multimedia Berjalan
Kondisi:
1. User dapat membuat laporan gangguan.
2. Foto dari kamera atau galeri tersimpan.
3. Favorit dapat dibuat, dibaca, diubah, dan dihapus.
4. Riwayat tersimpan.

### Checkpoint 9: Siap Demo
Kondisi:
1. Semua fitur utama stabil.
2. UI sudah rapi.
3. Skenario demo sudah siap.
4. APK atau project final siap dikumpulkan.

---

## 8. Definition of Done MVP

Aplikasi NaikApa dianggap selesai untuk MVP jika memenuhi kondisi berikut:

1. User dapat register dan login.
2. User dapat mengatur status memiliki motor dan mobil.
3. User dapat menggunakan GPS sebagai lokasi awal.
4. User dapat mencari tujuan memakai TomTom Search.
5. User dapat mencari halte atau stasiun dari GTFS lokal.
6. Aplikasi dapat menghitung rute transportasi umum.
7. Aplikasi dapat menghitung rute motor dan mobil.
8. Aplikasi dapat menghitung minimal satu rute gabungan.
9. Aplikasi dapat menampilkan rekomendasi utama dan alternatif.
10. Aplikasi dapat menampilkan skor kecocokan.
11. Aplikasi dapat menampilkan alasan rekomendasi.
12. Aplikasi dapat menampilkan estimasi waktu, biaya, BBM, jalan kaki, dan transit.
13. Aplikasi dapat menampilkan peta, marker, dan polyline.
14. User dapat membuat laporan gangguan dengan foto.
15. Laporan gangguan aktif selama 1 jam.
16. Laporan gangguan memberi penalti ringan pada rekomendasi.
17. User dapat menyimpan perjalanan favorit.
18. User dapat melihat riwayat pencarian dan perjalanan.
19. Aplikasi memenuhi kebutuhan database, CRUD, dan multimedia.
20. Aplikasi dapat didemokan dengan alur yang jelas.

---

## 9. Catatan Implementasi Penting

1. Jangan langsung mengerjakan rute gabungan sebelum rute transportasi umum dan rute kendaraan pribadi berhasil.
2. Jangan menggabungkan semua kandidat titik GTFS tanpa batas, karena bisa membuat aplikasi lambat.
3. Untuk awal, ambil maksimal 3 titik transportasi terdekat dari asal dan 3 dari tujuan.
4. Simpan hasil routing sementara agar tidak terlalu banyak request API.
5. Jangan jadikan laporan gangguan sebagai blokir penuh, cukup penalti ringan.
6. Jika Dijkstra terlalu berat, uji dengan subset data dulu.
7. Jika peta transportasi umum sulit digambar, tampilkan polyline kendaraan pribadi terlebih dahulu dan gunakan step list untuk transit.
8. Jika kamera bermasalah, pastikan upload dari galeri tetap berjalan.
9. Jika API TomTom bermasalah saat demo, siapkan contoh response atau cache hasil terakhir.
10. Fokus utama demo adalah menunjukkan bahwa NaikApa bisa membantu user memilih transportasi paling cocok, bukan hanya menampilkan peta.

---

## 10. Hasil Akhir yang Diharapkan

Pada akhir pengembangan, aplikasi NaikApa diharapkan memiliki hasil berikut:

1. Aplikasi Android Kotlin yang bisa dijalankan di HP atau emulator.
2. Database SQLite lokal berisi user, GTFS, laporan, favorit, dan riwayat.
3. Integrasi osmdroid (CartoDB Tiles) dan TomTom Search & Routing REST API.
4. Algoritma rute transportasi umum berbasis GTFS.
5. Rekomendasi rute motor dan mobil.
6. Rekomendasi rute gabungan kendaraan pribadi dan transportasi umum.
7. Skor kecocokan dan alasan rekomendasi.
8. Laporan gangguan dengan foto.
9. CRUD data pengguna, laporan, favorit, dan riwayat.
10. UI modern minimalis bergaya aplikasi transportasi.
11. Skenario demo yang bisa menunjukkan masalah, solusi, database, CRUD, multimedia, dan inovasi aplikasi.

---

## 11. Penutup

Planning ini digunakan sebagai panduan teknis dan pengerjaan bertahap untuk membangun aplikasi NaikApa. Fokus utama aplikasi adalah membuat rekomendasi transportasi yang membantu pengguna mengambil keputusan perjalanan berdasarkan kondisi nyata, bukan sekadar menampilkan rute. Dengan pembagian fase yang jelas, pengembangan dapat dilakukan secara bertahap dari fondasi aplikasi sampai hasil akhir yang siap dikumpulkan dan dipresentasikan.

---

## 12. Log Implementasi FASE 8 — GTFS Local Stop Search

**Tanggal implementasi:** 2026-05-21
**Status:** ✅ Selesai diimplementasikan

### Ringkasan Perubahan

FASE 8 menambahkan pencarian halte dan stasiun berbasis data GTFS lokal ke fitur search tujuan yang sudah menggunakan TomTom. Pencarian GTFS berjalan sepenuhnya offline dan paralel dengan TomTom.

### Checklist Aktivitas FASE 8

- [x] `SearchLocation.kt` — tambah field metadata GTFS: `stopId`, `agencyId`, `stopType`, `distanceMeters`, konstanta `SOURCE_GTFS`
- [x] `GtfsDao.kt` — perkuat `searchStops()`: trim keyword, guard empty, batas `coerceIn(1,50)`, trim agencyId
- [x] `GtfsStopSearchRepository.kt` — baru: query GtfsDao, mapping ke SearchLocation, jarak Haversine, sort relevansi+jarak, label agency
- [x] `HomeFragment.kt` — inisialisasi repository, track selected mode, hapus early-return API key, `performDestinationSearch` paralel GTFS+TomTom via `coroutineScope { async }`, badge agency di hasil, `getAgencyFilterForCurrentMode()`
- [x] `AppConstants.kt` — tambah `GTFS_SEARCH_LIMIT=10`, `GTFS_MIN_QUERY_LENGTH=2`, `GTFS_SOURCE="gtfs"`
- [x] `strings.xml` — tambah 13 string GTFS UI (loading, empty, badge agency, format jarak)
- [x] `GtfsStopSearchRepositoryTest.kt` — baru: 11 unit test (mapping, Haversine, sorting, filter agency, limit, label)
- [x] `NaikApaDatabaseHelperTest.kt` — tambah `gtfsSearchConstantsAreConfigured()` untuk 3 konstanta baru
- [x] `libs.versions.toml` — tambah `mockito = "5.12.0"` dan `mockito-core`
- [x] `build.gradle.kts` — tambah `testImplementation(libs.mockito.core)`

### Kriteria Selesai — Status

| Kriteria | Status |
|---|---|
| Pencarian "Stasiun Tangerang", "Dukuh Atas" dari GTFS lokal | ✅ Diimplementasikan via GtfsStopSearchRepository |
| Hasil GTFS muncul tanpa internet / TOMTOM_API_KEY placeholder | ✅ Tidak ada early-return API key, GTFS selalu jalan |
| Filter moda membatasi hasil GTFS (tj/krl/mrt/lrt) | ✅ getAgencyFilterForCurrentMode() diteruskan ke DAO |
| Nama stop, agency/moda, dan jarak tampil | ✅ Badge GradientDrawable + address dari buildAddress() |
| Pilihan GTFS mengisi selectedDestination + marker + history | ✅ selectDestination() dipanggil sama seperti TomTom |
| Unit test repository lulus | ✅ 11 test case di GtfsStopSearchRepositoryTest |
| Build debug berhasil | ✅ Berhasil divalidasi dengan `gradlew assembleDebug` |

---

## 13. Log Implementasi FASE 15 — UI Polishing

**Tanggal implementasi:** 2026-05-21
**Status:** ✅ Selesai diimplementasikan

### Ringkasan Perubahan

FASE 15 menambahkan pemolesan visual UI/UX yang premium pada halaman Home. Fitur-fitur ini memastikan interaksi pengguna lebih intuitif, dinamis, dan memiliki umpan balik visual yang kokoh.

### Checklist Aktivitas FASE 15

- [x] `strings.xml` — Tambah string untuk *empty state* (`home_empty_state_text`), pesan validasi (`home_validation_select_origin_and_destination`), dan *searching state* (`home_searching_route`).
- [x] `fragment_home.xml` — Integrasi `CircularProgressIndicator` terpusat (`progressRouteSearch`), pembatasan tinggi maksimal (*overflow*) pada panel rekomendasi via `NestedScrollView` (`scrollRecommendations`), dan penempatan elemen *empty state* (`tvEmptyState`).
- [x] `item_route_recommendation.xml` — Tambah efek visual riak premium (*foreground ripple*, *clickable*, *focusable*) pada kartu item rekomendasi.
- [x] `HomeFragment.kt` — Logika validasi input yang kokoh, visual state terpusat (`showLoadingState()`, `hideLoadingState()`, `showSearchErrorState()`), pembaruan visual chip prioritas yang konsisten dengan aksen premium (oranye aktif, border/putih inaktif), dan pengurutan dinamis saat prioritas diubah.

### Kriteria Selesai — Status

| Kriteria | Status |
|---|---|
| Pemuatan rute menampilkan loading indicator visual | ✅ Diimplementasikan via `progressRouteSearch` dan helper `showLoadingState` |
| Tampilan *empty state* yang informatif saat rekomendasi rute kosong | ✅ `tvEmptyState` tampil saat daftar rute kosong |
| Validasi input yang kokoh sebelum memulai pencarian rute | ✅ Toast validasi ditampilkan jika asal/tujuan belum dipilih |
| *Visual feedback* filter chip prioritas perjalanan yang konsisten | ✅ Warna chip oranye light premium saat terpilih, border/putih saat inaktif |
| Efek visual *ripple* pada item kartu rekomendasi | ✅ Ditambahkan efek ripple di foreground kartu rute |
| Pembatasan tinggi panel rekomendasi agar tidak menutupi peta | ✅ Dibungkus dalam `NestedScrollView` dengan batasan tinggi maks `320dp` |
| Build debug proyek berhasil dikompilasi | ✅ Diverifikasi sukses dengan `gradlew assembleDebug` |

