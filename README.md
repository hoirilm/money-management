# Keuangan Keluarga - Android Native (Jetpack Compose)

Aplikasi pencatatan keuangan modern, minimalis, dan kekinian yang terhubung langsung dengan **Google Sheets di Google Drive** sebagai database utamanya via **Google Apps Script**. Mendukung kolaborasi suami dan istri serta checklist tagihan bulanan berulang.

---

## ✨ Fitur Utama

1. **Sinkronisasi Langsung ke Google Sheets**:
   - Semua pencatatan tersimpan di Google Sheets secara *real-time*.
   - Sheet `Transaksi` untuk riwayat pemasukan/pengeluaran.
   - Sheet `Tagihan` untuk checklist komitmen tagihan bulanan.
2. **Kolaborasi Suami & Istri**:
   - Setiap ponsel dapat memilih profil aktif (`Suami` atau `Istri`).
   - Filter transaksi fleksibel: **[Semua]**, **[Suami]**, atau **[Istri]**.
   - Saldo dan grafik menyesuaikan profil yang dipilih.
3. **Checklist Tagihan Bulanan (Smart Auto-Reset)**:
   - Input manual tagihan berulang (Nama tagihan, Estimasi nominal, Tanggal jatuh tempo, Kategori).
   - Indikator kemajuan tagihan bulanan (*progress bar* & sisa tagihan belum dibayar).
   - **Auto-Reset Tiap Bulan**: Status lunas tersimpan per bulan (misal `2026-10`). Saat kalender berganti ke bulan berikutnya, status otomatis kembali menjadi belum bayar tanpa perlu reset manual!
   - Saat dicentang lunas, tersedia opsi untuk langsung mencatat otomatis ke riwayat pengeluaran.
4. **Desain UI Minimalis Modern (Material 3)**:
   - Navigasi elegan antara tab **Transaksi** dan **Tagihan**.
   - *Hero Summary Card* dengan gradien dan pill indikator.
   - *Interactive Modal Bottom Sheet* untuk input cepat.

---

## ⚡ Cara Menjalankan & Build Tanpa Android Studio

Karena Anda **tidak menginstall Android Studio**, cara termudah dan tercepat untuk mendapatkan aplikasi adalah menggunakan **GitHub Actions**:

### Metode: Build APK di Cloud via GitHub Actions (100% Gratis & Otomatis)

1. Buat repository baru di akun GitHub Anda (misal: `keuangan-keluarga`).
2. Masukkan dan push semua file dari folder ini ke GitHub:
   ```bash
   git init
   git add .
   git commit -m "feat: setup keuangan keluarga app with monthly bills checklist"
   git branch -M main
   git remote add origin https://github.com/<username-anda>/<repo-anda>.git
   git push -u origin main
   ```
3. Buka tab **Actions** di repositori GitHub Anda.
4. Skrip `.github/workflows/build-apk.yml` akan otomatis berjalan dan meng-compile aplikasi dalam 2–3 menit.
5. Klik proses workflow yang selesai, lalu unduh file **`Keuangan-Keluarga-Debug-APK`**.
6. Kirim file `.apk` tersebut ke HP Anda dan HP istri via WhatsApp/Telegram atau buka langsung dari browser HP, lalu install!

---

## 📝 Setup Google Sheets & Backend

1. Buka Google Sheets Anda di Google Drive.
2. Klik menu **Extensions (Ekstensi)** > **Apps Script**.
3. Buka file [`google_apps_script.js`](./google_apps_script.js) di proyek ini, copy seluruh kodenya, dan paste ke Apps Script.
4. Klik **Deploy** > **Manage deployments** (atau **New deployment**).
   - Tipe: **Web app**
   - **Execute as**: `Me (email Anda)`
   - **Who has access**: `Anyone`
5. Salin URL Web App yang berakhiran `/exec`.
6. Buka aplikasi di HP, masuk ke **Pengaturan** (ikon gear di kanan atas), lalu tempelkan URL tersebut dan pilih profil (`Suami` / `Istri`). Selesai!
