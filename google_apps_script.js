/**
 * GOOGLE APPS SCRIPT - KEUANGAN KELUARGA & TAGIHAN BULANAN
 * Pasang script ini di Extensions > Apps Script pada Google Sheets Anda,
 * lalu lakukan Deploy > Manage deployments > Edit (ikon pensil) > Version: New version > Deploy.
 */

const SHEET_TRANSAKSI = "Transaksi";
const SHEET_TAGIHAN = "Tagihan";
const SHEET_KATEGORI_TAGIHAN = "KategoriTagihan";

const DEFAULT_BILL_CATEGORIES = [
  ["Tagihan & Listrik", "⚡"],
  ["Wi-Fi & Internet", "🌐"],
  ["Cicilan & Pinjaman", "💳"],
  ["Air PDAM", "💧"],
  ["Pendidikan / SPP", "📚"],
  ["Hiburan / Langganan", "🎬"],
  ["Asuransi & BPJS", "🛡️"],
  ["Lainnya", "📦"]
];

function getSpreadsheetTz() {
  try {
    const tz = SpreadsheetApp.getActiveSpreadsheet().getSpreadsheetTimeZone();
    return tz || "Asia/Jakarta";
  } catch (e) {
    return "Asia/Jakarta";
  }
}

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
    sheet.appendRow(["id", "nama", "nominal", "jatuhTempo", "kategori", "bulanLunas", "autoTxMap"]);
  } else {
    // Pastikan kolom G (autoTxMap) tersedia untuk spreadsheet versi lama
    if (sheet.getLastRow() >= 1) {
      const headerCol7 = String(sheet.getRange(1, 7).getValue() || "").trim();
      if (!headerCol7) {
        sheet.getRange(1, 7).setValue("autoTxMap");
      }
    }
  }
  return sheet;
}

function getSheetKategoriTagihan() {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  let sheet = ss.getSheetByName(SHEET_KATEGORI_TAGIHAN);
  if (!sheet) {
    sheet = ss.insertSheet(SHEET_KATEGORI_TAGIHAN);
    sheet.appendRow(["nama", "icon"]);
    sheet.getRange(2, 1, DEFAULT_BILL_CATEGORIES.length, 2).setValues(DEFAULT_BILL_CATEGORIES);
  }
  return sheet;
}

function readBillCategories() {
  const sheet = getSheetKategoriTagihan();
  const rows = sheet.getDataRange().getValues();
  const categories = [];
  for (let i = 1; i < rows.length; i++) {
    const name = String(rows[i][0] || "").trim();
    if (!name) continue;
    const icon = String(rows[i][1] || "🏷️").trim() || "🏷️";
    categories.push({
      name: name,
      icon: icon
    });
  }
  return categories;
}

function normalizeMonthToken(val) {
  if (!val) return "";
  if (val instanceof Date) {
    return Utilities.formatDate(val, getSpreadsheetTz(), "yyyy-MM");
  }
  const str = String(val).trim();
  if (!str) return "";

  // Format YYYY-MM, YYYY/MM, YYYY-MM-DD, YYYY/MM/DD
  const ymdMatch = str.match(/(\d{4})[-/](\d{1,2})(?:[-/]\d{1,2})?/);
  if (ymdMatch) {
    const year = ymdMatch[1];
    const monthNum = parseInt(ymdMatch[2], 10);
    if (monthNum >= 1 && monthNum <= 12) {
      return year + "-" + ("0" + monthNum).slice(-2);
    }
  }

  // Fallback jika tersimpan sebagai teks Date seperti "Thu Oct 01 2026" atau "Okt 2026"
  const yearMatch = str.match(/\b(19\d{2}|20\d{2})\b/);
  if (yearMatch) {
    const lower = str.toLowerCase();
    const monthMap = [
      { keys: ["jan"], mm: "01" },
      { keys: ["feb"], mm: "02" },
      { keys: ["mar"], mm: "03" },
      { keys: ["apr"], mm: "04" },
      { keys: ["may", "mei"], mm: "05" },
      { keys: ["jun"], mm: "06" },
      { keys: ["jul"], mm: "07" },
      { keys: ["aug", "agu"], mm: "08" },
      { keys: ["sep"], mm: "09" },
      { keys: ["oct", "okt"], mm: "10" },
      { keys: ["nov"], mm: "11" },
      { keys: ["dec", "des"], mm: "12" }
    ];
    for (let i = 0; i < monthMap.length; i++) {
      for (let k = 0; k < monthMap[i].keys.length; k++) {
        if (lower.indexOf(monthMap[i].keys[k]) !== -1) {
          return yearMatch[1] + "-" + monthMap[i].mm;
        }
      }
    }
  }

  return "";
}

function parseBulanLunasList(val) {
  if (!val) return [];
  if (val instanceof Date) {
    const m = normalizeMonthToken(val);
    return m ? [m] : [];
  }
  const parts = String(val).split(",");
  const result = [];
  for (let i = 0; i < parts.length; i++) {
    const norm = normalizeMonthToken(parts[i]);
    if (norm && result.indexOf(norm) === -1) {
      result.push(norm);
    }
  }
  return result;
}

function normalizeBulanLunas(val) {
  return parseBulanLunasList(val).join(",");
}

function parseAutoTxMap(val) {
  const map = {};
  if (!val) return map;
  const parts = String(val).split(",");
  for (let i = 0; i < parts.length; i++) {
    const entry = parts[i].trim();
    if (!entry) continue;
    const colonIdx = entry.indexOf(":");
    if (colonIdx > 0 && colonIdx < entry.length - 1) {
      const rawMonth = entry.substring(0, colonIdx).trim();
      const txId = entry.substring(colonIdx + 1).trim();
      const normMonth = normalizeMonthToken(rawMonth) || rawMonth;
      if (normMonth && txId) {
        map[normMonth] = txId;
      }
    }
  }
  return map;
}

function serializeAutoTxMap(mapObj) {
  if (!mapObj) return "";
  const keys = Object.keys(mapObj);
  const entries = [];
  for (let i = 0; i < keys.length; i++) {
    const k = keys[i];
    const v = mapObj[k];
    if (k && v) {
      entries.push(k + ":" + v);
    }
  }
  return entries.join(",");
}

function normalizeTanggal(val) {
  if (!val) return "";
  if (val instanceof Date) {
    return Utilities.formatDate(val, getSpreadsheetTz(), "yyyy-MM-dd HH:mm:ss");
  }
  return String(val).trim();
}

function writePlainTextCell(cell, textValue) {
  const clean = String(textValue !== undefined && textValue !== null ? textValue : "").trim();
  cell.clearContent();
  cell.setNumberFormat("@");
  if (clean !== "") {
    cell.setValue(clean);
  }
}

function isTruthy(val) {
  return val === true || String(val).toLowerCase() === "true";
}

function jsonResponse(payload) {
  return ContentService
    .createTextOutput(JSON.stringify(payload))
    .setMimeType(ContentService.MimeType.JSON);
}

// ==========================================
// ENDPOINT GET (Ambil Data)
// ==========================================
function doGet(e) {
  try {
    const view = (e && e.parameter && e.parameter.view) ? e.parameter.view : "transactions";

    // VIEW: TAGIHAN BULANAN & KATEGORI TAGIHAN
    if (view === "bills") {
      const sheet = getSheetTagihan();
      const rows = sheet.getDataRange().getValues();
      const bills = [];

      for (let i = 1; i < rows.length; i++) {
        const row = rows[i];
        if (!row[0] && !row[1]) continue;
        bills.push({
          id: String(row[0] || ""),
          nama: String(row[1] || ""),
          nominal: Number(row[2]) || 0,
          jatuhTempo: Number(row[3]) || 1,
          kategori: String(row[4] || "Tagihan & Listrik"),
          bulanLunas: normalizeBulanLunas(row[5]),
          autoTxMap: serializeAutoTxMap(parseAutoTxMap(row[6]))
        });
      }

      return jsonResponse({
        status: "success",
        data: bills,
        categories: readBillCategories()
      });
    }

    // VIEW: HANYA KATEGORI TAGIHAN
    if (view === "bill_categories") {
      return jsonResponse({
        status: "success",
        categories: readBillCategories()
      });
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
        tanggal: normalizeTanggal(row[1]),
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
    data.forEach(function(item) {
      if (item.tipe === "INCOME") totalIncome += item.nominal;
      else if (item.tipe === "EXPENSE") totalExpense += item.nominal;
    });

    const ss = SpreadsheetApp.getActiveSpreadsheet();
    const spreadsheetId = ss.getId();
    const excelExportUrl = "https://docs.google.com/spreadsheets/d/" + spreadsheetId + "/export?format=xlsx";
    const spreadsheetUrl = ss.getUrl();

    return jsonResponse({
      status: "success",
      spreadsheetId: spreadsheetId,
      excelExportUrl: excelExportUrl,
      spreadsheetUrl: spreadsheetUrl,
      summary: {
        totalIncome: totalIncome,
        totalExpense: totalExpense,
        balance: totalIncome - totalExpense,
        totalTransactions: data.length
      },
      data: data
    });

  } catch (error) {
    return jsonResponse({
      status: "error",
      message: error.toString()
    });
  }
}

// ==========================================
// ENDPOINT POST (Tambah, Edit, Hapus)
// ==========================================
function doPost(e) {
  const lock = LockService.getScriptLock();
  lock.waitLock(15000);

  try {
    let postData = {};
    if (e && e.postData && e.postData.contents) {
      try {
        postData = JSON.parse(e.postData.contents);
      } catch (parseErr) {
        postData = e.parameter || {};
      }
    } else if (e && e.parameter) {
      postData = e.parameter;
    }

    const action = postData.action || "add";

    // 1. TAMBAH TRANSAKSI
    if (action === "add") {
      const sheet = getSheetTransaksi();
      const id = postData.id || Utilities.getUuid();
      const tanggal = postData.tanggal || Utilities.formatDate(new Date(), getSpreadsheetTz(), "yyyy-MM-dd HH:mm:ss");
      const tipe = postData.tipe || "EXPENSE";
      const kategori = postData.kategori || "Umum";
      const nominal = Number(postData.nominal) || 0;
      const akun = postData.akun || "Cash";
      const catatan = postData.catatan || "";
      const pencatat = postData.pencatat || "Anonim";

      sheet.appendRow([id, tanggal, tipe, kategori, nominal, akun, catatan, pencatat]);
      try {
        const lastRow = sheet.getLastRow();
        sheet.getRange(lastRow, 2).setNumberFormat("@");
      } catch (err) {}
      SpreadsheetApp.flush();

      return jsonResponse({
        status: "success",
        message: "Transaksi berhasil dicatat",
        id: id
      });
    }

    // 2. HAPUS TRANSAKSI
    else if (action === "delete") {
      const sheet = getSheetTransaksi();
      const targetId = String(postData.id || "").trim();
      const rows = sheet.getDataRange().getValues();
      let found = false;

      for (let i = rows.length - 1; i >= 1; i--) {
        if (String(rows[i][0] || "").trim() === targetId) {
          sheet.deleteRow(i + 1);
          found = true;
          break;
        }
      }
      if (found) SpreadsheetApp.flush();

      return jsonResponse({
        status: found ? "success" : "not_found",
        message: found ? "Transaksi berhasil dihapus" : "ID tidak ditemukan"
      });
    }

    // 3. TAMBAH TAGIHAN BULANAN
    else if (action === "add_bill") {
      const sheet = getSheetTagihan();
      const id = postData.id || Utilities.getUuid();
      const nama = postData.nama || "Tagihan";
      const nominal = Number(postData.nominal) || 0;
      const jatuhTempo = Number(postData.jatuhTempo) || 1;
      const kategori = postData.kategori || "Tagihan & Listrik";
      const bulanLunas = normalizeBulanLunas(postData.bulanLunas || "");

      sheet.appendRow([id, nama, nominal, jatuhTempo, kategori, "", ""]);
      const lastRow = sheet.getLastRow();
      writePlainTextCell(sheet.getRange(lastRow, 6), bulanLunas);
      writePlainTextCell(sheet.getRange(lastRow, 7), "");
      SpreadsheetApp.flush();

      return jsonResponse({
        status: "success",
        message: "Tagihan bulanan berhasil ditambahkan",
        id: id
      });
    }

    // 4. CENTANG / BATAL CENTANG TAGIHAN LUNAS (+ AUTO RECORD / REVERSE SPESIFIK)
    else if (action === "toggle_bill") {
      const sheet = getSheetTagihan();
      const targetId = String(postData.id || "").trim();
      const targetName = String(postData.nama || "").trim().toLowerCase();
      const rows = sheet.getDataRange().getValues();

      // Prioritaskan pencarian berdasarkan ID unik terlebih dahulu, lalu fallback ke Nama Tagihan
      let matchedIndex = -1;
      if (targetId) {
        for (let i = 1; i < rows.length; i++) {
          if (String(rows[i][0] || "").trim() === targetId) {
            matchedIndex = i;
            break;
          }
        }
      }
      if (matchedIndex === -1 && targetName) {
        for (let i = 1; i < rows.length; i++) {
          if (String(rows[i][1] || "").trim().toLowerCase() === targetName) {
            matchedIndex = i;
            break;
          }
        }
      }

      if (matchedIndex === -1) {
        return jsonResponse({
          status: "not_found",
          message: "ID/Nama tagihan tidak ditemukan"
        });
      }

      const matchedRowNumber = matchedIndex + 1;
      const actualBillId = String(rows[matchedIndex][0] || targetId).trim();
      const actualBillName = String(rows[matchedIndex][1] || postData.nama || "Tagihan").trim();
      const actualBillKategori = String(postData.kategori || rows[matchedIndex][4] || "Tagihan & Listrik").trim();
      const actualBillNominal = (postData.nominal !== undefined && postData.nominal !== null && postData.nominal !== "")
        ? (Number(postData.nominal) || 0)
        : (Number(rows[matchedIndex][2]) || 0);

      const oldPaidMonths = parseBulanLunasList(rows[matchedIndex][5]);
      const txMap = parseAutoTxMap(rows[matchedIndex][6]);

      // Tentukan bulan (monthKey) yang sedang di-check / uncheck serta target statusnya (targetIsPaid)
      let monthKey = "";
      let targetIsPaid = false;

      if (postData.monthKey) {
        monthKey = normalizeMonthToken(postData.monthKey) || String(postData.monthKey).trim();
        if (postData.isPaid !== undefined && postData.isPaid !== null) {
          targetIsPaid = isTruthy(postData.isPaid);
        } else {
          const incomingList = parseBulanLunasList(postData.bulanLunas);
          targetIsPaid = incomingList.indexOf(monthKey) !== -1;
        }
      } else {
        // Fallback kompatibilitas payload lama (tanpa monthKey eksplisit): bandingkan bulanLunas lama vs baru
        const incomingList = parseBulanLunasList(postData.bulanLunas !== undefined ? postData.bulanLunas : "");
        let added = "";
        let removed = "";

        for (let a = 0; a < incomingList.length; a++) {
          if (oldPaidMonths.indexOf(incomingList[a]) === -1) {
            added = incomingList[a];
            break;
          }
        }
        for (let r = 0; r < oldPaidMonths.length; r++) {
          if (incomingList.indexOf(oldPaidMonths[r]) === -1) {
            removed = oldPaidMonths[r];
            break;
          }
        }

        if (added) {
          monthKey = added;
          targetIsPaid = true;
        } else if (removed) {
          monthKey = removed;
          targetIsPaid = false;
        } else if (incomingList.length === 0) {
          monthKey = oldPaidMonths.length > 0
            ? oldPaidMonths[oldPaidMonths.length - 1]
            : Utilities.formatDate(new Date(), getSpreadsheetTz(), "yyyy-MM");
          targetIsPaid = false;
        } else {
          monthKey = incomingList[incomingList.length - 1];
          targetIsPaid = true;
        }
      }

      // Perbarui daftar bulanLunas untuk tagihan ini
      let updatedPaidMonths = oldPaidMonths.slice();
      if (targetIsPaid) {
        if (monthKey && updatedPaidMonths.indexOf(monthKey) === -1) {
          updatedPaidMonths.push(monthKey);
        }
      } else {
        if (monthKey) {
          updatedPaidMonths = updatedPaidMonths.filter(function(m) {
            return m !== monthKey;
          });
        } else {
          updatedPaidMonths = [];
        }
      }

      const shouldRecordTx = isTruthy(postData.recordTransaction);
      let createdTxId = null;
      let reversedTxId = null;

      // KONDISI 1: Tagihan DI-CHECKLIST (Tandai Lunas)
      if (targetIsPaid) {
        if (shouldRecordTx) {
          const sheetTx = getSheetTransaksi();
          const txRows = sheetTx.getDataRange().getValues();
          const existingMappedTxId = (monthKey && txMap[monthKey] && txMap[monthKey] !== "NONE") ? txMap[monthKey] : "";
          let existingTxRowNumber = -1;

          if (existingMappedTxId) {
            for (let j = txRows.length - 1; j >= 1; j--) {
              if (String(txRows[j][0] || "").trim() === existingMappedTxId) {
                existingTxRowNumber = j + 1;
                break;
              }
            }
          }

          const tanggal = String(postData.tanggal || "").trim() ||
            Utilities.formatDate(new Date(), getSpreadsheetTz(), "yyyy-MM-dd HH:mm:ss");
          const akun = String(postData.akun || "Cash").trim();
          const catatan = String(postData.catatan || "").trim() ||
            ("Pembayaran " + actualBillName + (monthKey ? " (" + monthKey + ")" : ""));
          const pencatat = String(postData.pencatat || "Suami").trim();

          if (existingTxRowNumber !== -1) {
            // Hindari duplikasi jika request terkirim ulang: update baris transaksi spesifik tersebut
            createdTxId = existingMappedTxId;
            sheetTx.getRange(existingTxRowNumber, 2, 1, 7).setValues([[
              tanggal,
              "EXPENSE",
              actualBillKategori,
              actualBillNominal,
              akun,
              catatan,
              pencatat
            ]]);
            sheetTx.getRange(existingTxRowNumber, 2).setNumberFormat("@");
          } else {
            createdTxId = String(postData.transactionId || "").trim() || ("billtx_" + Utilities.getUuid());
            sheetTx.appendRow([
              createdTxId,
              tanggal,
              "EXPENSE",
              actualBillKategori,
              actualBillNominal,
              akun,
              catatan,
              pencatat
            ]);
            try {
              sheetTx.getRange(sheetTx.getLastRow(), 2).setNumberFormat("@");
            } catch (err) {}
          }

          if (monthKey) {
            txMap[monthKey] = createdTxId;
          }
        } else {
          // Dicentang lunas TANPA opsi catat otomatis ke riwayat pengeluaran
          if (monthKey) {
            txMap[monthKey] = "NONE";
          }
        }
      }
      // KONDISI 2: Tagihan DI-UNCHECK (Batal Lunas -> Reverse spesifik transaksi otomatisnya & uncheck checklist)
      else {
        const mappedTxId = (monthKey && txMap[monthKey]) ? String(txMap[monthKey]).trim() : "";
        const payloadTxId = String(postData.transactionId || "").trim();
        const effectiveTxId = mappedTxId || payloadTxId;

        if (effectiveTxId === "NONE") {
          // Tagihan ini sebelumnya dicentang TANPA catat otomatis -> jangan hapus transaksi apa pun
        } else if (effectiveTxId !== "") {
          // Hapus SPESIFIK hanya transaksi yang dibuat otomatis saat checklist tagihan & periode ini
          const sheetTx = getSheetTransaksi();
          const txRows = sheetTx.getDataRange().getValues();
          for (let j = txRows.length - 1; j >= 1; j--) {
            if (String(txRows[j][0] || "").trim() === effectiveTxId) {
              sheetTx.deleteRow(j + 1);
              reversedTxId = effectiveTxId;
              break;
            }
          }
        } else {
          // Fallback data lama (sebelum kolom autoTxMap ada): cocokkan secara ketat pada catatan pembayaran tagihan ini
          const sheetTx = getSheetTransaksi();
          const txRows = sheetTx.getDataRange().getValues();
          const exactNote = "pembayaran " + actualBillName.toLowerCase();
          const prefixWithPeriod = exactNote + " (";
          let fallbackRowToDelete = -1;

          for (let j = txRows.length - 1; j >= 1; j--) {
            const rowType = String(txRows[j][2] || "").trim().toUpperCase();
            const rowNote = String(txRows[j][6] || "").trim().toLowerCase();
            const rowDate = normalizeTanggal(txRows[j][1]);

            if (rowType === "EXPENSE" && (rowNote === exactNote || rowNote.indexOf(prefixWithPeriod) === 0)) {
              if (!monthKey || rowDate.indexOf(monthKey) === 0 || rowNote.indexOf(monthKey.toLowerCase()) !== -1) {
                fallbackRowToDelete = j + 1;
                reversedTxId = String(txRows[j][0] || "").trim();
                break;
              } else if (fallbackRowToDelete === -1 && rowNote === exactNote) {
                fallbackRowToDelete = j + 1;
                reversedTxId = String(txRows[j][0] || "").trim();
              }
            }
          }

          if (fallbackRowToDelete !== -1) {
            sheetTx.deleteRow(fallbackRowToDelete);
          }
        }

        if (monthKey && txMap[monthKey] !== undefined) {
          delete txMap[monthKey];
        }
      }

      // Simpan perubahan checklist (bulanLunas), mapping transaksi otomatis (autoTxMap), & nominal default
      const finalBulanLunas = updatedPaidMonths.join(",");
      const finalAutoTxMap = serializeAutoTxMap(txMap);

      writePlainTextCell(sheet.getRange(matchedRowNumber, 6), finalBulanLunas);
      writePlainTextCell(sheet.getRange(matchedRowNumber, 7), finalAutoTxMap);

      if (isTruthy(postData.updateDefaultNominal) && postData.nominal !== undefined) {
        sheet.getRange(matchedRowNumber, 3).setValue(actualBillNominal);
      }

      SpreadsheetApp.flush();

      return jsonResponse({
        status: "success",
        message: targetIsPaid ? "Status tagihan ditandai lunas" : "Status lunas tagihan dibatalkan",
        id: actualBillId,
        bulanLunas: finalBulanLunas,
        autoTxMap: finalAutoTxMap,
        transactionId: createdTxId,
        reversedTransactionId: reversedTxId
      });
    }

    // 5. HAPUS TAGIHAN BULANAN
    else if (action === "delete_bill") {
      const sheet = getSheetTagihan();
      const targetId = String(postData.id || "").trim();
      const targetName = String(postData.nama || "").trim().toLowerCase();
      const rows = sheet.getDataRange().getValues();
      let found = false;

      for (let i = rows.length - 1; i >= 1; i--) {
        const rowId = String(rows[i][0] || "").trim();
        const rowName = String(rows[i][1] || "").trim().toLowerCase();
        if ((targetId && rowId === targetId) || (!targetId && targetName && rowName === targetName)) {
          sheet.deleteRow(i + 1);
          found = true;
          break;
        }
      }
      if (found) SpreadsheetApp.flush();

      return jsonResponse({
        status: found ? "success" : "not_found",
        message: found ? "Tagihan berhasil dihapus" : "ID tagihan tidak ditemukan"
      });
    }

    // 6. TAMBAH KATEGORI TAGIHAN (CUSTOM)
    else if (action === "add_bill_category") {
      const sheet = getSheetKategoriTagihan();
      const rawName = String(postData.nama || postData.name || "").trim();
      const rawIcon = String(postData.icon || "🏷️").trim() || "🏷️";

      if (!rawName) {
        return jsonResponse({
          status: "error",
          message: "Nama kategori tagihan tidak boleh kosong"
        });
      }

      const rows = sheet.getDataRange().getValues();
      let exists = false;
      for (let i = 1; i < rows.length; i++) {
        if (String(rows[i][0] || "").trim().toLowerCase() === rawName.toLowerCase()) {
          exists = true;
          break;
        }
      }

      if (!exists) {
        sheet.appendRow([rawName, rawIcon]);
        SpreadsheetApp.flush();
      }

      return jsonResponse({
        status: "success",
        message: exists ? "Kategori tagihan sudah ada" : "Kategori tagihan berhasil ditambahkan",
        categories: readBillCategories()
      });
    }

    // 7. HAPUS KATEGORI TAGIHAN (CUSTOM)
    else if (action === "delete_bill_category") {
      const sheet = getSheetKategoriTagihan();
      const targetName = String(postData.nama || postData.name || "").trim().toLowerCase();

      if (!targetName) {
        return jsonResponse({
          status: "error",
          message: "Nama kategori tagihan tidak boleh kosong"
        });
      }

      const rows = sheet.getDataRange().getValues();
      let deleted = false;
      for (let i = rows.length - 1; i >= 1; i--) {
        if (String(rows[i][0] || "").trim().toLowerCase() === targetName) {
          sheet.deleteRow(i + 1);
          deleted = true;
        }
      }
      if (deleted) SpreadsheetApp.flush();

      return jsonResponse({
        status: deleted ? "success" : "not_found",
        message: deleted ? "Kategori tagihan berhasil dihapus" : "Kategori tagihan tidak ditemukan",
        categories: readBillCategories()
      });
    }

    return jsonResponse({
      status: "error",
      message: "Action tidak dikenali: " + action
    });

  } catch (error) {
    return jsonResponse({
      status: "error",
      message: error.toString()
    });
  } finally {
    try {
      lock.releaseLock();
    } catch (e) {}
  }
}
