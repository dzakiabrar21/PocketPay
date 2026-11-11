import type { Dict } from "./types";

const repo = "https://github.com/dzakiabrar21/PocketPay";

export const id: Dict = {
  lang: "id",
  meta: {
    title: "PocketPay — Receh kecil, tabungan besar (dompet digital Android)",
    description:
      "PocketPay: aplikasi dompet digital Android yang menabung receh otomatis. Bayar QRIS, kantong berbunga, refund 24 jam, statistik, dan keamanan berlapis. Proyek portofolio dengan data simulasi.",
    ogTitle: "PocketPay — Dompet digital yang menabung receh untukmu",
    ogDescription:
      "Pembayaran QRIS, tabungan pembulatan, kantong berbunga, refund, dan aplikasi terenkripsi berkunci PIN. Proyek portofolio Kotlin + Jetpack Compose.",
  },
  skip: "Lewati ke konten",
  nav: {
    story: "Cerita",
    features: "Fitur",
    screens: "Tampilan",
    rules: "Aturan uang",
    security: "Keamanan",
    docs: "Dokumen",
    langAria: "Bahasa",
  },
  hero: {
    chip: "Proyek portofolio · data simulasi",
    titleBefore: "Receh kecil, ",
    titleEm: "tabungan",
    titleAfter: " besar.",
    lead: "PocketPay adalah dompet digital Android yang membulatkan setiap pembayaran QRIS dan menabung selisihnya untukmu. Ada kantong berbunga, refund 24 jam, statistik, dan database terenkripsi.",
    ctaStory: "Lihat ceritanya",
    ctaSource: "Kode sumber",
    facts: [
      { value: "6.300+", label: "baris Kotlin" },
      { value: "99", label: "pengujian" },
      { value: "2", label: "bahasa" },
      { value: "0", label: "server — semua di perangkat" },
    ],
    notes: [
      { title: "+Rp1.500", sub: "masuk Celengan Receh" },
      { title: "Refund 24 jam", sub: "uang kembali atomik" },
      { title: "AES-256", sub: "kunci di Keystore" },
    ],
  },
  story: {
    eyebrow: "Cerita aplikasi",
    title: "Satu pembayaran, lima hal yang terjadi di baliknya",
    sub: "Gulir untuk mengikuti perjalanan satu transaksi. Layar di kanan berubah mengikuti setiap bab.",
    warn: "**Catatan:** uangnya simulasi, dan QRIS-nya hanya valid secara format karena PocketPay bukan penerbit QRIS terdaftar. Jangan dipakai untuk dana sungguhan.",
    chapters: [
      {
        no: "01",
        shot: "pay",
        title: "Bayar tanpa mikir",
        text: "Pindai QRIS lewat kamera atau galeri. Hanya QRIS valid (CRC-16, mata uang IDR) yang diterima; kode lain langsung ditolak.",
        bullets: [
          "Pilih sumber dana: Rekening Utama atau kantong.",
          "Kantong kurang? Selisihnya otomatis diambil dari Rekening Utama dalam satu transaksi atomik.",
        ],
      },
      {
        no: "02",
        shot: "pockets",
        title: "Receh jadi tabungan",
        text: "Nominal dibulatkan ke atas ke kelipatan Rp 5.000 dan selisihnya masuk ke Celengan Receh. Kantong lain menumbuhkan uangmu dengan bunga.",
        bullets: [
          "Kantong belanja 3,5% p.a. dengan target dan progres.",
          "Deposito 6% p.a. terkunci sampai jatuh tempo.",
          "Bunga harian masuk lewat WorkManager, walau aplikasi tertutup.",
        ],
      },
      {
        no: "03",
        shot: "stats",
        title: "Lihat ke mana uangmu pergi",
        text: "Grafik mingguan dan bulanan dibangun dari transaksi nyata. Ketuk satu batang untuk melihat pemasukan dan pengeluaran hari itu.",
        bullets: [
          "Refund, pembayaran yang dibatalkan, dan perpindahan antar-kantong tidak dihitung ganda.",
          "Riwayat bisa dicari dan difilter menurut tipe dan periode.",
        ],
      },
      {
        no: "04",
        shot: "pin",
        title: "Aman sejak dibuka",
        text: "Aplikasi selalu mulai terkunci. PIN 6 digit atau biometrik diminta lagi setelah 60 detik di latar dan sebelum setiap pembayaran.",
        bullets: [
          "Lima kali salah mengunci kolom PIN selama 30 detik.",
          "Database SQLCipher; kuncinya dibungkus Android Keystore.",
        ],
      },
      {
        no: "05",
        shot: "receipt",
        title: "Ada bukti, bisa dibatalkan",
        text: "Setiap transaksi punya struk digital dengan nomor referensi dan kunci idempotensi, sehingga tidak pernah tercatat dua kali.",
        bullets: [
          "Refund dalam 24 jam: dana kembali, transaksi asli ditandai dibatalkan.",
          "Rekening koran PDF multi-halaman untuk dibuka atau dibagikan.",
        ],
      },
    ],
  },
  features: {
    eyebrow: "Semua fitur",
    title: "Yang dibutuhkan dompet modern, sudah jalan",
    interestBig: { value: "6%", label: "per tahun, deposito terkunci" },
    tiles: {
      qris: {
        title: "Bayar QRIS dengan sumber dana berlapis",
        text: "Pemindai kamera + galeri, validasi EMVCo, dan transaksi atomik. Kekurangan kantong ditutup dari Rekening Utama.",
      },
      roundup: { title: "Round-up", text: "Dibulatkan ke Rp 5.000, bisa dimatikan per pembayaran." },
      interest: {
        title: "Bunga harian otomatis",
        text: "Dihitung dengan BigDecimal dan tidak pernah dibayar dua kali. Notifikasi saat deposito jatuh tempo atau target tercapai.",
      },
      refund: { title: "Refund", text: "24 jam, sekali saja, atomik." },
      stats: { title: "Statistik", text: "Batang mingguan & bulanan yang bisa diketuk." },
      scan: {
        title: "Scan me & e-statement PDF",
        text: "QR terima yang bisa dipindai dan rekening koran multi-halaman dengan kop dan watermark.",
      },
      notify: {
        title: "Notifikasi pribadi",
        text: "Di layar kunci hanya tampil kalimat umum; rincian muncul setelah dibuka.",
      },
      security: {
        title: "PIN + biometrik terikat kripto",
        text: "Hash PBKDF2 yang dienkripsi lagi dengan Keystore; kunci biometrik gugur bila ada sidik jari baru.",
      },
      i18n: {
        title: "Dwibahasa, terang & gelap",
        text: "Indonesia dan Inggris lengkap; tema mengikuti sistem dari satu set token warna.",
      },
    },
  },
  gallery: {
    eyebrow: "Tampilan",
    title: "Hangat, bersih, dan konsisten",
    sub: "Geser ke samping. Gambar dirender langsung dari aplikasi dengan data contoh.",
    light: "Terang",
    dark: "Gelap",
    themeAria: "Tema",
    items: [
      { shot: "splash", title: "Splash", text: "Gerbang masuk bermerek." },
      { shot: "pin", title: "Kunci PIN", text: "Keypad dan sidik jari." },
      { shot: "home", title: "Beranda", text: "Saldo, kantong, riwayat." },
      { shot: "pay", title: "Bayar QRIS", text: "Sumber dana & round-up." },
      { shot: "success", title: "Berhasil", text: "Ringkasan bergaya struk." },
      { shot: "stats", title: "Statistik", text: "Grafik yang bisa diketuk." },
      { shot: "pockets", title: "Kantong", text: "Belanja, deposito, target." },
      { shot: "manage", title: "Kelola kantong", text: "Top-up, tarik, tutup." },
      { shot: "receipt", title: "Struk & refund", text: "Detail transaksi." },
      { shot: "receive", title: "Scan me", text: "QR terima." },
      { shot: "history", title: "Riwayat", text: "Cari dan filter." },
      { shot: "profile", title: "Profil", text: "Status keamanan." },
      { shot: "statement", title: "E-statement", text: "Ekspor PDF." },
    ],
  },
  rules: {
    eyebrow: "Aturan uang",
    title: "Hitungan yang bisa kamu cek sendiri",
    sub: "Contoh nyata: membayar Rp 23.500 dari kantong Jajan Harian. Semua aturan ada di `BankingDao` dan diuji.",
    receipt: {
      caption: "CONTOH PEMBAYARAN QRIS",
      ariaLabel: "Contoh struk pembayaran",
      lines: [
        { label: "Merchant", value: "Kantin Mahasiswa" },
        { label: "Sumber dana", value: "Jajan Harian" },
        { label: "Dibayar", value: "Rp23.500" },
        "rule",
        { label: "Round-up → Rp25.000", value: "Rp1.500" },
        { label: "Dari", value: "Rekening Utama" },
        { label: "Masuk ke", value: "Celengan Receh" },
      ],
      totalLabel: "Total keluar",
      totalValue: "Rp25.000",
      badge: "✓ ATOMIK · KUNCI IDEMPOTENSI 76367D01",
    },
    defs: [
      {
        term: "Round-up",
        text: "Dibulatkan ke atas ke kelipatan Rp 5.000. Selisih dipindahkan dari Rekening Utama ke Celengan Receh.",
      },
      {
        term: "Cascading",
        text: "Kantong kurang? Selisihnya diambil dari Rekening Utama. Ditolak bila gabungan saldo tak cukup untuk pembayaran + round-up.",
      },
      {
        term: "Deposito",
        text: "Terkunci sampai jatuh tempo: tidak bisa membayar, ditransfer, ditarik, atau ditutup.",
      },
      {
        term: "Bunga",
        text: "`saldo × rate ÷ 100 ÷ 365 × hari penuh` — dibulatkan ke bawah, maks. 365 hari per proses; sisa hari terbawa.",
      },
      {
        term: "Refund",
        text: "24 jam. Dana kembali ke Rekening Utama, transaksi asli menjadi `REVERSED`, round-up tetap tersimpan. Refund kedua ditolak.",
      },
      {
        term: "Integritas",
        text: "Saldo `Long`, nominal harus > 0, dan saldo dibaca ulang di dalam transaksi sehingga layar basi tidak bisa boros.",
      },
    ],
  },
  security: {
    eyebrow: "Keamanan",
    title: "Lima lapis, dan jujur soal batasnya",
    layers: [
      {
        title: "Layar",
        text: "`FLAG_SECURE` menutup screenshot dan Recents. Aplikasi mulai terkunci dan terkunci lagi setelah 60 detik di latar.",
      },
      {
        title: "Akses",
        text: "PIN 6 digit (PBKDF2, 120 rb iterasi; PIN lemah ditolak), kuncian 30 detik setelah 5 kali salah, dan biometrik terikat `CryptoObject`.",
      },
      {
        title: "Data diam",
        text: "SQLCipher AES-256 dengan kata sandi acak yang dibungkus kunci Android Keystore. Database lama yang belum terenkripsi dikonversi otomatis.",
      },
      {
        title: "Transaksi",
        text: "Atomik, saldo dibaca ulang, kunci idempotensi, dan error bertipe yang tidak membocorkan detail internal.",
      },
      {
        title: "Perangkat",
        text: "Tanpa backup, FileProvider hanya untuk folder rekening koran, R8 aktif, dan notifikasi privat di layar kunci.",
      },
    ],
    limitsTitle: "Yang belum ada",
    limits: [
      "Tidak ada backend atau akun; perangkat adalah satu-satunya sumber data.",
      "Tidak ada deteksi root/emulator (Play Integrity).",
      "Penghitung salah-PIN ada di preferensi biasa, jadi bisa direset di perangkat root.",
      "Jalur biometrik belum diuji di HP dengan sidik jari terdaftar.",
      "QRIS valid secara format, tetapi PocketPay bukan penerbit terdaftar.",
    ],
  },
  architecture: {
    eyebrow: "Arsitektur",
    title: "Satu arah, mudah diuji",
    sub: "Clean Architecture + MVI: UI mengirim intent, ViewModel mengelola `StateFlow`, repository memanggil DAO yang menjalankan aturan uang secara atomik.",
    diagramAria: "Diagram arsitektur",
    decisions: [
      { title: "Satu transaksi Room", text: "Setiap operasi uang atomik; saldo dibaca ulang di dalamnya." },
      { title: "Error bertipe", text: "`BankingError` dipetakan ke pesan dwibahasa." },
      { title: "Overlay yang aman", text: "Menelan sentuhan dan menyembunyikan layar bawah dari TalkBack." },
      { title: "Warna semantik", text: "Terang/gelap tanpa hex di layar." },
    ],
    stackNote: "· target 36",
  },
  testing: {
    eyebrow: "Pengujian",
    title: "99 pengujian, termasuk di perangkat sungguhan",
    sub: "Aturan uang, PIN, QRIS, statistik, migrasi database terenkripsi, dan paginasi PDF. Setiap layar juga dirender ke gambar untuk tinjauan desain.",
    bars: [
      { label: "Unit (JVM)", value: 76 },
      { label: "Fungsional di HP", value: 8 },
      { label: "Render layar", value: 15 },
    ],
    commentJvm: "# JVM",
    commentDevice: "# Keystore, SQLCipher, PDF, worker, screenshot",
  },
  start: {
    eyebrow: "Mulai cepat",
    title: "Jalankan dalam beberapa menit",
    tips: [
      "Butuh Android Studio (JDK 17+) dan perangkat atau emulator Android 8.0+ (API 26).",
      "Saat pertama dibuka, buat PIN 6 digit (tidak boleh berulang atau berurutan).",
      "Di emulator, pakai **Input manual** di pemindai karena tidak ada kamera.",
      "Data awal: Rekening Utama Rp 5.000.000 plus kantong Jajan Harian dan Deposito Darurat.",
    ],
  },
  docs: {
    eyebrow: "Dokumentasi",
    title: "Baca lebih dalam",
    items: [
      {
        file: "ARCHITECTURE.md",
        title: "Arsitektur",
        text: "Lapisan, paket, aliran MVI, migrasi, pekerjaan latar.",
        href: `${repo}/blob/main/docs/ARCHITECTURE.md`,
      },
      {
        file: "FEATURES.md",
        title: "Fitur & aturan",
        text: "Setiap fitur dengan rumus dan batasannya.",
        href: `${repo}/blob/main/docs/FEATURES.md`,
      },
      {
        file: "SECURITY.md",
        title: "Catatan keamanan",
        text: "Apa yang dilindungi, bagaimana, dan apa yang belum.",
        href: `${repo}/blob/main/docs/SECURITY.md`,
      },
      {
        file: "TESTING.md",
        title: "Pengujian",
        text: "Daftar test, cara menjalankan, dan harness screenshot.",
        href: `${repo}/blob/main/docs/TESTING.md`,
      },
      { file: "README.md", title: "README", text: "Ringkasan proyek dan langkah instalasi.", href: `${repo}#readme` },
    ],
    notBuilt: {
      title: "Belum dibuat",
      items: [
        "Transfer ke pengguna lain, tagihan, dan isi saldo dari luar.",
        "Akun/login dan backend sinkronisasi.",
        "Kategori, anggaran, dan pembayaran terjadwal.",
      ],
    },
    next: {
      title: "Berikutnya",
      items: [
        "Transfer ke kontak simulasi dan alur `PENDING` → berhasil/gagal.",
        "Use case terpisah, test ViewModel, dan CI GitHub Actions.",
        "Uji biometrik di perangkat fisik.",
      ],
    },
  },
  footer: { text: "Proyek portofolio dengan data simulasi. MIT License." },
  copy: { idle: "Salin", done: "✓" },
  alts: {
    splash: "Layar splash",
    pin: "Layar kunci PIN",
    home: "Beranda dengan kartu saldo dan kantong",
    pay: "Form pembayaran QRIS",
    success: "Halaman pembayaran berhasil",
    stats: "Statistik pemasukan dan pengeluaran",
    pockets: "Daftar kantong",
    manage: "Sheet kelola kantong",
    receipt: "Struk transaksi dan tombol refund",
    receive: "Kode QR terima",
    history: "Riwayat transaksi",
    profile: "Profil dan status keamanan",
    statement: "Sheet ekspor e-statement",
  },
};
