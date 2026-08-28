/**
 * ==============================================================================
 * KAZI AGROTECH — GOOGLE APPS SCRIPT BACKUP WEB APP
 * ==============================================================================
 * 
 * Instructions for Setup:
 * 1. Open Google Sheets (https://sheets.new) and create a new Spreadsheet.
 *    Name it e.g. "Kazi Agrotech - Cloud Database Backup".
 * 2. Click on "Extensions" > "Apps Script".
 * 3. Delete any code in the editor and paste this entire file content.
 * 4. (Optional) Set your API_TOKEN below to secure your endpoint.
 * 5. Click "Deploy" > "New deployment".
 * 6. Select type: "Web app".
 *    - Description: "Kazi Agrotech Backup Endpoint"
 *    - Execute as: "Me" (your Google account)
 *    - Who has access: "Anyone" (allows the Android app to POST data without OAuth login)
 * 7. Click "Deploy" and Authorize access when prompted.
 * 8. Copy the generated "Web App URL" (starts with https://script.google.com/macros/s/...)
 * 9. Paste this URL into the Kazi Agrotech app under Settings > Cloud Backup.
 * ==============================================================================
 */

// Optional secret token for extra security. Leave empty ("") if you don't want token check.
var API_TOKEN = "";

function doPost(e) {
  var lock = LockService.getScriptLock();
  // Wait up to 30 seconds for other executions to finish
  if (!lock.tryLock(30000)) {
    return createJsonResponse(false, "Server busy. Please try again later.", 0);
  }

  try {
    if (!e || !e.postData || !e.postData.contents) {
      return createJsonResponse(false, "No payload received in request", 0);
    }

    var payload;
    try {
      payload = JSON.parse(e.postData.contents);
    } catch (parseError) {
      return createJsonResponse(false, "Invalid JSON payload: " + parseError.message, 0);
    }

    // Token check if configured
    if (API_TOKEN && API_TOKEN.trim() !== "") {
      var reqToken = payload.api_token || (e.parameter ? e.parameter.token : "");
      if (reqToken !== API_TOKEN) {
        return createJsonResponse(false, "Unauthorized: Invalid API Token", 0);
      }
    }

    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var data = payload.data || {};
    var processedCount = 0;

    // 1. Sync Farm Profile
    if (data.farm_profile) {
      syncFarmProfile(ss, data.farm_profile);
      processedCount++;
    }

    // 2. Sync Daily Reports (Upsert)
    if (data.daily_reports && Array.isArray(data.daily_reports)) {
      processedCount += syncDailyReports(ss, data.daily_reports);
    }

    // 3. Sync Monthly Expenses (Upsert)
    if (data.monthly_expenses && Array.isArray(data.monthly_expenses)) {
      processedCount += syncMonthlyExpenses(ss, data.monthly_expenses);
    }

    // 4. Sync Users (Upsert)
    if (data.users && Array.isArray(data.users)) {
      processedCount += syncUsers(ss, data.users);
    }

    // 5. Sync Role Permissions (Upsert)
    if (data.role_permissions && typeof data.role_permissions === 'object') {
      processedCount += syncRolePermissions(ss, data.role_permissions);
    }

    // 6. Log Backup Activity
    logBackupActivity(ss, payload, processedCount, "SUCCESS", "Backup completed successfully");

    return createJsonResponse(true, "ক্লাউড ব্যাকআপ সফল হয়েছে", processedCount);

  } catch (error) {
    try {
      var ssErr = SpreadsheetApp.getActiveSpreadsheet();
      logBackupActivity(ssErr, payload || {}, 0, "ERROR", error.toString());
    } catch (logErr) {}
    return createJsonResponse(false, "ব্যাকআপ সম্পন্ন হয়নি: " + error.message, 0);
  } finally {
    lock.releaseLock();
  }
}

function doGet(e) {
  return createJsonResponse(true, "Kazi Agrotech Google Sheets Backup Web App is online and healthy.", 0);
}

function createJsonResponse(success, message, count) {
  var output = {
    success: success,
    message: message,
    records_processed: count,
    timestamp: new Date().toISOString()
  };
  return ContentService
    .createTextOutput(JSON.stringify(output))
    .setMimeType(ContentService.MimeType.JSON);
}

// -------------------------------------------------------------
// SYNC FUNCTIONS
// -------------------------------------------------------------

function syncFarmProfile(ss, profile) {
  var sheet = getOrCreateSheet(ss, "Farm Profile (ফার্ম প্রোফাইল)", [
    "Field (বিবরণ)", "Value (তথ্য)", "Last Updated (আপডেট সময়)"
  ]);

  var rows = [
    ["ফার্মের নাম (Farm Name)", profile.farmName || "", new Date()],
    ["মালিকের নাম (Owner Name)", profile.ownerName || "", new Date()],
    ["মোবাইল নম্বর (Mobile)", profile.mobileNumber || "", new Date()],
    ["ঠিকানা (Address)", profile.address || "", new Date()],
    ["লোগো ইমোজি (Logo Emoji)", profile.logoEmoji || "🐔", new Date()],
    ["প্রারম্ভিক স্টক (Initial Stock)", profile.initialOpeningStock || 0, new Date()],
    ["প্রারম্ভিক তারিখ (Initial Date)", profile.initialOpeningDate || "", new Date()],
    ["ডার্ক মোড (Dark Mode)", profile.isDarkMode ? "ON" : "OFF", new Date()],
    ["স্বয়ংক্রিয় ব্যাকআপ (Auto Backup)", profile.autoBackup ? "ON" : "OFF", new Date()]
  ];

  sheet.getRange(2, 1, sheet.getLastRow() > 1 ? sheet.getLastRow() - 1 : 1, 3).clearContent();
  sheet.getRange(2, 1, rows.length, 3).setValues(rows);
}

function syncDailyReports(ss, reports) {
  var headers = [
    "Record ID", "তারিখ (Date)", "বর্তমান মুরগী", "মৃত মুরগী", "ডিম উৎপাদন",
    "ডিম বিক্রয়", "ডিমের দর (৳)", "মোট বিক্রয় (৳)", "ঔষধ খরচ (৳)",
    "সমাপনী স্টক", "অন্যান্য স্টক বৃদ্ধি", "নষ্ট / ঘাটতি", "স্টক সমন্বয়",
    "সমন্বয়ের কারণ", "মন্তব্য (Remarks)", "Sync Timestamp"
  ];
  var sheet = getOrCreateSheet(ss, "Daily Reports (দৈনিক রিপোর্ট)", headers);
  if (reports.length === 0) return 0;

  var idColIndex = 1; // 1-indexed
  var existingMap = getRowIndexMap(sheet, idColIndex);
  var appendRows = [];
  var now = new Date();

  reports.forEach(function(r) {
    var idStr = String(r.id || r.date);
    var rowValues = [
      idStr,
      r.date || "",
      r.currentBirds || 0,
      r.deadBirds || 0,
      r.eggProduction || 0,
      r.eggSold || 0,
      r.eggPrice || 0,
      r.totalSale || 0,
      r.medicineCost || 0,
      r.currentStock || 0,
      r.otherStockIn || 0,
      r.otherStockOut || 0,
      r.stockAdjustment || 0,
      r.adjustmentReason || "",
      r.remarks || "",
      now
    ];

    if (existingMap[idStr]) {
      sheet.getRange(existingMap[idStr], 1, 1, rowValues.length).setValues([rowValues]);
    } else {
      appendRows.push(rowValues);
    }
  });

  if (appendRows.length > 0) {
    sheet.getRange(sheet.getLastRow() + 1, 1, appendRows.length, appendRows[0].length).setValues(appendRows);
  }
  return reports.length;
}

function syncMonthlyExpenses(ss, expenses) {
  var headers = [
    "Record ID", "তারিখ (Date)", "খাদ্য / ফিড (৳)", "ঔষধ ও ভ্যাকসিন (৳)",
    "স্টাফ বাজার (৳)", "স্টাফ বেতন (৳)", "গাড়ি মেরামত (৳)", "সম্পদ ক্রয় (৳)",
    "বিদ্যুৎ বিল (৳)", "অন্যান্য খরচ (৳)", "মোট ব্যয় (৳)", "মন্তব্য (Remarks)",
    "Sync Timestamp"
  ];
  var sheet = getOrCreateSheet(ss, "Monthly Expenses (মাসিক খরচ)", headers);
  if (expenses.length === 0) return 0;

  var idColIndex = 1;
  var existingMap = getRowIndexMap(sheet, idColIndex);
  var appendRows = [];
  var now = new Date();

  expenses.forEach(function(e) {
    var idStr = String(e.id || e.date);
    var rowValues = [
      idStr,
      e.date || "",
      e.feedCost || 0,
      e.medicineCost || 0,
      e.staffMarket || 0,
      e.staffSalary || 0,
      e.vehicleRepair || 0,
      e.assets || 0,
      e.electricityBill || 0,
      e.otherExpense || 0,
      e.totalExpense || 0,
      e.remarks || "",
      now
    ];

    if (existingMap[idStr]) {
      sheet.getRange(existingMap[idStr], 1, 1, rowValues.length).setValues([rowValues]);
    } else {
      appendRows.push(rowValues);
    }
  });

  if (appendRows.length > 0) {
    sheet.getRange(sheet.getLastRow() + 1, 1, appendRows.length, appendRows[0].length).setValues(appendRows);
  }
  return expenses.length;
}

function syncUsers(ss, users) {
  var headers = [
    "User ID (UID)", "ইউজারনেম (Name)", "ইমেইল (Email)", "মোবাইল (Phone)",
    "রোল (Role)", "অনুমোদিত (Approved)", "রেজিস্ট্রেশন তারিখ", "Sync Timestamp"
  ];
  var sheet = getOrCreateSheet(ss, "Users (ইউজার তালিকা)", headers);
  if (users.length === 0) return 0;

  var idColIndex = 1;
  var existingMap = getRowIndexMap(sheet, idColIndex);
  var appendRows = [];
  var now = new Date();

  users.forEach(function(u) {
    var idStr = String(u.id || u.email);
    var regDateStr = u.registeredDate ? new Date(u.registeredDate).toLocaleString() : "";
    var rowValues = [
      idStr,
      u.username || "",
      u.email || "",
      u.phone || "",
      u.role || "WORKER",
      u.isApproved ? "YES (অনুমোদিত)" : "NO (পেন্ডিং)",
      regDateStr,
      now
    ];

    if (existingMap[idStr]) {
      sheet.getRange(existingMap[idStr], 1, 1, rowValues.length).setValues([rowValues]);
    } else {
      appendRows.push(rowValues);
    }
  });

  if (appendRows.length > 0) {
    sheet.getRange(sheet.getLastRow() + 1, 1, appendRows.length, appendRows[0].length).setValues(appendRows);
  }
  return users.length;
}

function syncRolePermissions(ss, rolePermsMap) {
  var headers = [
    "Role Key", "রোল নাম", "Daily View", "Daily Add", "User View",
    "Expense View", "Expense Add", "Expense Delete", "Report View", "Report Download", "Sync Timestamp"
  ];
  var sheet = getOrCreateSheet(ss, "Role Permissions (রোল পারমিশন)", headers);
  var keys = Object.keys(rolePermsMap);
  if (keys.length === 0) return 0;

  var idColIndex = 1;
  var existingMap = getRowIndexMap(sheet, idColIndex);
  var appendRows = [];
  var now = new Date();

  keys.forEach(function(k) {
    var p = rolePermsMap[k];
    var idStr = String(p.roleKey || k).toUpperCase();
    var rowValues = [
      idStr,
      p.roleDisplayName || idStr,
      p.dailyReportView ? "TRUE" : "FALSE",
      p.dailyReportAdd ? "TRUE" : "FALSE",
      p.userManagementView ? "TRUE" : "FALSE",
      p.expenseView ? "TRUE" : "FALSE",
      p.expenseAdd ? "TRUE" : "FALSE",
      p.expenseDelete ? "TRUE" : "FALSE",
      p.reportAnalyticsView ? "TRUE" : "FALSE",
      p.reportAnalyticsDownload ? "TRUE" : "FALSE",
      now
    ];

    if (existingMap[idStr]) {
      sheet.getRange(existingMap[idStr], 1, 1, rowValues.length).setValues([rowValues]);
    } else {
      appendRows.push(rowValues);
    }
  });

  if (appendRows.length > 0) {
    sheet.getRange(sheet.getLastRow() + 1, 1, appendRows.length, appendRows[0].length).setValues(appendRows);
  }
  return keys.length;
}

function logBackupActivity(ss, payload, recordCount, status, message) {
  var headers = [
    "Timestamp (সময়)", "Status (স্ট্যাটাস)", "Records Synced", "App Version",
    "Schema Version", "Triggered By", "Message / Note"
  ];
  var sheet = getOrCreateSheet(ss, "Backup Log (লগ)", headers);
  var now = new Date();
  var row = [
    now,
    status,
    recordCount,
    payload.app_version || "1.0.0",
    payload.backup_schema_version || 1,
    payload.user_email || "System/Auto",
    message
  ];
  sheet.appendRow(row);
}

// -------------------------------------------------------------
// HELPER FUNCTIONS
// -------------------------------------------------------------

function getOrCreateSheet(ss, sheetName, headers) {
  var sheet = ss.getSheetByName(sheetName);
  if (!sheet) {
    sheet = ss.insertSheet(sheetName);
    sheet.appendRow(headers);
    var headerRange = sheet.getRange(1, 1, 1, headers.length);
    headerRange.setBackground("#1B5E20");
    headerRange.setFontColor("#FFFFFF");
    headerRange.setFontWeight("bold");
    sheet.setFrozenRows(1);
  }
  return sheet;
}

function getRowIndexMap(sheet, idColIndex) {
  var map = {};
  var lastRow = sheet.getLastRow();
  if (lastRow <= 1) return map;

  var ids = sheet.getRange(2, idColIndex, lastRow - 1, 1).getValues();
  for (var i = 0; i < ids.length; i++) {
    var val = String(ids[i][0]);
    if (val !== "") {
      map[val] = i + 2; // Row numbers are 1-based and row 1 is header
    }
  }
  return map;
}
