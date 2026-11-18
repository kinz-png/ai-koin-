# MICIN AI Screener v0.4.0

Aplikasi Android untuk memantau token DEX berbasis data publik DEX Screener. Tidak meminta wallet, seed phrase, atau private key dan tidak melakukan transaksi.

## Fitur
- Jaringan: Solana, Base, BNB Chain, Ethereum.
- Filter likuiditas minimum, volume 1 jam minimum, usia pair maksimum, dan skor minimum.
- Ringkasan volume 5m/1h/24h, perubahan harga, buy/sell, FDV, usia pair, serta pair terbaik per token berdasarkan likuiditas.
- Skor heuristik dengan pengurang nilai untuk tekanan jual, lonjakan/penurunan harga ekstrem, volume 1 jam yang terlalu besar dibanding likuiditas, dan rasio likuiditas/FDV yang rendah.
- Label risiko: DATA TERBATAS, WASPADA TINGGI, WASPADA, atau BELUM DIAUDIT.
- Tekan kartu token untuk membuka pair di DEX Screener; tekan lama untuk menyalin alamat token.
- Floating overlay dapat digeser, minimize/expand, refresh, dan menampilkan 5 token teratas.

## Batasan penting
- Feed `token-profiles/latest/v1` dari DEX Screener berisi profil token terbaru/terbarui, bukan daftar lengkap semua pair atau semua token baru.
- Skor dan label risiko adalah heuristik dari aktivitas pasar, bukan AI prediktif, bukan audit kontrak, dan bukan rekomendasi beli/jual.
- Aplikasi belum memeriksa honeypot, fungsi mint/freeze, pajak transfer, LP lock/burn, distribusi holder, riwayat deployer, wallet cluster, atau transaksi smart money.
- Volume/likuiditas dapat berubah cepat dan data API bisa terlambat atau tidak lengkap. DYOR; jangan gunakan skor sebagai satu-satunya dasar keputusan.

## Build APK
Buka repository ini di Android Studio atau unggah ke GitHub. Workflow `.github/workflows/build-apk.yml` akan membangun debug APK saat dijalankan manual dari tab Actions atau saat push ke `main`/`master`. Artefak bernama `MICIN-AI-Scanner-0.4.0`.

Persyaratan build: JDK 17, Android SDK Platform 35, Build Tools 35.0.0, Gradle 8.7, Android Gradle Plugin 8.6.1.

Debug APK bukan rilis Play Store. Untuk distribusi publik, buat build release yang ditandatangani dengan keystore milik sendiri dan jangan pernah memasukkan keystore/password ke repository publik.
