/**
 * GOOGLE APPS SCRIPT - KEUANGAN KELUARGA & TAGIHAN BULANAN
 * Pasang script ini di Extensions > Apps Script pada Google Sheets Anda.
 */

const SHEET_TRANSAKSI = "Transaksi";
const SHEET_TAGIHAN = "Tagihan";

function getSheetTransaksi() {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  let sheet = ss.getSheetByName(SHEET_TRANSAKSI);
  if (!sheet) {
    sheet = ss.insertSheet(SHEET_TRANSAKSI);
    sheet.appendRow(["id", "tanggal", "tipe", "kategori", "nominal", "akun", "catatan", "pencatat"]);
  }
  return sheet;
}

function getSheetTagihan() {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  let sheet = ss.getSheetByName(SHEET_TAGIHAN);
  if (!sheet) {
    sheet = ss.insertSheet(SHEET_TAGIHAN);
    sheet.appendRow(["id", "nama", "nominal", "jatuhTempo", "kategori", "bulanLunas"]);
  }
  return sheet;
}

// ==========================================
// ENDPOINT GET (Ambil Data)
// ==========================================
function doGet(e) {
  try {
    const view = (e && e.parameter && e.parameter.view) ? e.parameter.view : "transactions";

    // VIEW: TAGIHAN BULANAN
    if (view === "bills") {
      const sheet = getSheetTagihan();
      const rows = sheet.getDataRange().getValues();
      const bills = [];

      for (let i = 1; i < rows.length; i++) {
        const row = rows[i];
        if (!row[0]) continue;
        bills.push({
          id: String(row[0]),
          nama: String(row[1]),
          nominal: Number(row[2]) || 0,
          jatuhTempo: Number(row[3]) || 1,
          kategori: String(row[4] || "Tagihan & Listrik"),
          bulanLunas: String(row[5] || "")
        });
      }

      return ContentService.createTextOutput(JSON.stringify({
        status: "success",
        data: bills
      })).setMimeType(ContentService.MimeType.JSON);
    }

    // DEFAULT VIEW: TRANSAKSI & SUMMARY
    const sheet = getSheetTransaksi();
    const rows = sheet.getDataRange().getValues();
    const data = [];

    for (let i = 1; i < rows.length; i++) {
      const row = rows[i];
      if (!row[0]) continue;
      data.push({
        id: String(row[0]),
        tanggal: String(row[1]),
        tipe: String(row[2]), // 'EXPENSE' atau 'INCOME'
        kategori: String(row[3]),
        nominal: Number(row[4]) || 0,
        akun: String(row[5]),
        catatan: String(row[6]),
        pencatat: String(row[7])
      });
    }

    data.reverse(); // Terbaru di atas

    let totalIncome = 0;
    let totalExpense = 0;
    data.forEach(item => {
      if (item.tipe === "INCOME") totalIncome += item.nominal;
      else if (item.tipe === "EXPENSE") totalExpense += item.nominal;
    });

    return ContentService.createTextOutput(JSON.stringify({
      status: "success",
      summary: {
        totalIncome: totalIncome,
        totalExpense: totalExpense,
        balance: totalIncome - totalExpense,
        totalTransactions: data.length
      },
      data: data
    })).setMimeType(ContentService.MimeType.JSON);

  } catch (error) {
    return ContentService.createTextOutput(JSON.stringify({
      status: "error",
      message: error.toString()
    })).setMimeType(ContentService.MimeType.JSON);
  }
}

// ==========================================
// ENDPOINT POST (Tambah, Edit, Hapus)
// ==========================================
function doPost(e) {
  try {
    const postData = JSON.parse(e.postData.contents);
    const action = postData.action || "add";

    // 1. TAMBAH TRANSAKSI
    if (action === "add") {
      const sheet = getSheetTransaksi();
      const id = postData.id || Utilities.getUuid();
      const tanggal = postData.tanggal || Utilities.formatDate(new Date(), "Asia/Jakarta", "yyyy-MM-dd HH:mm:ss");
      const tipe = postData.tipe || "EXPENSE";
      const kategori = postData.kategori || "Umum";
      const nominal = Number(postData.nominal) || 0;
      const akun = postData.akun || "Cash";
      const catatan = postData.catatan || "";
      const pencatat = postData.pencatat || "Anonim";

      sheet.appendRow([id, tanggal, tipe, kategori, nominal, akun, catatan, pencatat]);

      return ContentService.createTextOutput(JSON.stringify({
        status: "success",
        message: "Transaksi berhasil dicatat",
        id: id
      })).setMimeType(ContentService.MimeType.JSON);
    }

    // 2. HAPUS TRANSAKSI
    else if (action === "delete") {
      const sheet = getSheetTransaksi();
      const targetId = String(postData.id);
      const rows = sheet.getDataRange().getValues();
      let found = false;

      for (let i = 1; i < rows.length; i++) {
        if (String(rows[i][0]) === targetId) {
          sheet.deleteRow(i + 1);
          found = true;
          break;
        }
      }

      return ContentService.createTextOutput(JSON.stringify({
        status: found ? "success" : "not_found",
        message: found ? "Transaksi berhasil dihapus" : "ID tidak ditemukan"
      })).setMimeType(ContentService.MimeType.JSON);
    }

    // 3. TAMBAH TAGIHAN BULANAN
    else if (action === "add_bill") {
      const sheet = getSheetTagihan();
      const id = postData.id || Utilities.getUuid();
      const nama = postData.nama || "Tagihan";
      const nominal = Number(postData.nominal) || 0;
      const jatuhTempo = Number(postData.jatuhTempo) || 1;
      const kategori = postData.kategori || "Tagihan & Listrik";
      const bulanLunas = postData.bulanLunas || "";

      sheet.appendRow([id, nama, nominal, jatuhTempo, kategori, bulanLunas]);

      return ContentService.createTextOutput(JSON.stringify({
        status: "success",
        message: "Tagihan bulanan berhasil ditambahkan",
        id: id
      })).setMimeType(ContentService.MimeType.JSON);
    }

    // 4. CENTANG / BATAL CENTANG TAGIHAN LUNAS
    else if (action === "toggle_bill") {
      const sheet = getSheetTagihan();
      const targetId = String(postData.id);
      const rows = sheet.getDataRange().getValues();
      let found = false;

      for (let i = 1; i < rows.length; i++) {
        if (String(rows[i][0]) === targetId) {
          const newBulanLunas = postData.bulanLunas || "";
          sheet.getRange(i + 1, 6).setValue(newBulanLunas); // Kolom F: bulanLunas
          found = true;
          break;
        }
      }

      // Jika user memilih untuk langsung catat ke riwayat pengeluaran
      if (postData.recordTransaction === true) {
        const sheetTx = getSheetTransaksi();
        const txId = Utilities.getUuid();
        const tanggal = Utilities.formatDate(new Date(), "Asia/Jakarta", "yyyy-MM-dd HH:mm:ss");
        sheetTx.appendRow([
          txId,
          tanggal,
          "EXPENSE",
          postData.kategori || "Tagihan & Listrik",
          Number(postData.nominal) || 0,
          postData.akun || "BCA",
          "Pembayaran " + (postData.nama || "Tagihan"),
          postData.pencatat || "Suami"
        ]);
      }

      return ContentService.createTextOutput(JSON.stringify({
        status: found ? "success" : "not_found",
        message: found ? "Status tagihan berhasil diperbarui" : "ID tagihan tidak ditemukan"
      })).setMimeType(ContentService.MimeType.JSON);
    }

    // 5. HAPUS TAGIHAN BULANAN
    else if (action === "delete_bill") {
      const sheet = getSheetTagihan();
      const targetId = String(postData.id);
      const rows = sheet.getDataRange().getValues();
      let found = false;

      for (let i = 1; i < rows.length; i++) {
        if (String(rows[i][0]) === targetId) {
          sheet.deleteRow(i + 1);
          found = true;
          break;
        }
      }

      return ContentService.createTextOutput(JSON.stringify({
        status: found ? "success" : "not_found",
        message: found ? "Tagihan berhasil dihapus" : "ID tagihan tidak ditemukan"
      })).setMimeType(ContentService.MimeType.JSON);
    }

  } catch (error) {
    return ContentService.createTextOutput(JSON.stringify({
      status: "error",
      message: error.toString()
    })).setMimeType(ContentService.MimeType.JSON);
  }
}
