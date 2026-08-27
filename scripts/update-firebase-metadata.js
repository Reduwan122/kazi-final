/**
 * Updates Firebase Realtime Database "appUpdate" node using Google Service Account OAuth2 JWT.
 * Zero external dependencies (uses native Node.js crypto and https modules).
 */

const fs = require('fs');
const https = require('https');
const crypto = require('crypto');

async function main() {
  const serviceAccountJson = process.env.FIREBASE_SERVICE_ACCOUNT;
  const versionCode = parseInt(process.env.VERSION_CODE, 10);
  const versionName = process.env.VERSION_NAME;
  const apkUrl = process.env.APK_URL;
  const sha256 = process.env.SHA256_HASH;
  const fileSize = parseInt(process.env.FILE_SIZE || '0', 10);
  const releaseNotes = process.env.RELEASE_NOTES || 'নতুন ফিচার ও পারফরম্যান্স উন্নতি।';
  const forceUpdate = process.env.FORCE_UPDATE === 'true';
  const releaseDate = new Date().toISOString().split('T')[0];

  if (!serviceAccountJson) {
    console.warn('⚠️ FIREBASE_SERVICE_ACCOUNT secret is not set. Skipping automated Firebase Realtime Database update.');
    console.warn('To enable automatic in-app updates, please add FIREBASE_SERVICE_ACCOUNT to GitHub Repository Secrets.');
    return;
  }

  let serviceAccount;
  try {
    serviceAccount = JSON.parse(serviceAccountJson);
  } catch (e) {
    console.error('❌ Failed to parse FIREBASE_SERVICE_ACCOUNT JSON:', e.message);
    process.exit(1);
  }

  const projectId = serviceAccount.project_id || 'test-114d2';
  const databaseUrl = `https://${projectId}-default-rtdb.firebaseio.com`;

  console.log(`📡 Authenticating with Firebase using Service Account (${serviceAccount.client_email})...`);

  const accessToken = await getAccessToken(serviceAccount);
  console.log('✅ Google OAuth2 Access Token successfully acquired.');

  const payload = {
    versionCode: versionCode,
    versionName: versionName,
    apkUrl: apkUrl,
    forceUpdate: forceUpdate,
    title: 'নতুন আপডেট পাওয়া গেছে',
    message: `কাজী এগ্রোটেক সংস্করণ v${versionName} ব্যবহারের জন্য প্রস্তুত।`,
    releaseNotes: releaseNotes,
    fileSize: fileSize,
    sha256: sha256,
    releaseDate: releaseDate
  };

  console.log(`📤 Updating Firebase Realtime Database at ${databaseUrl}/appUpdate.json...`);
  console.log('Payload:', JSON.stringify(payload, null, 2));

  await updateFirebaseDatabase(databaseUrl, accessToken, payload);
  console.log('🎉 Firebase Realtime Database "appUpdate" node updated successfully!');
}

function getAccessToken(serviceAccount) {
  return new Promise((resolve, reject) => {
    const now = Math.floor(Date.now() / 1000);
    const header = { alg: 'RS256', typ: 'JWT' };
    const claimSet = {
      iss: serviceAccount.client_email,
      scope: 'https://www.googleapis.com/auth/userinfo.email https://www.googleapis.com/auth/firebase.database',
      aud: 'https://oauth2.googleapis.com/token',
      exp: now + 3600,
      iat: now
    };

    const encodedHeader = base64UrlEncode(JSON.stringify(header));
    const encodedClaimSet = base64UrlEncode(JSON.stringify(claimSet));
    const stringToSign = `${encodedHeader}.${encodedClaimSet}`;

    const signer = crypto.createSign('RSA-SHA256');
    signer.update(stringToSign);
    const signature = signer.sign(serviceAccount.private_key, 'base64');
    const encodedSignature = signature.replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
    const jwt = `${stringToSign}.${encodedSignature}`;

    const postData = `grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&assertion=${jwt}`;

    const req = https.request(
      'https://oauth2.googleapis.com/token',
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded',
          'Content-Length': Buffer.byteLength(postData)
        }
      },
      (res) => {
        let body = '';
        res.on('data', (chunk) => (body += chunk));
        res.on('end', () => {
          if (res.statusCode >= 200 && res.statusCode < 300) {
            try {
              const data = JSON.parse(body);
              resolve(data.access_token);
            } catch (err) {
              reject(err);
            }
          } else {
            reject(new Error(`OAuth2 Token error HTTP ${res.statusCode}: ${body}`));
          }
        });
      }
    );

    req.on('error', reject);
    req.write(postData);
    req.end();
  });
}

function updateFirebaseDatabase(databaseUrl, accessToken, payload) {
  return new Promise((resolve, reject) => {
    const url = new URL(`${databaseUrl}/appUpdate.json`);
    const postData = JSON.stringify(payload);

    const req = https.request(
      url,
      {
        method: 'PUT',
        headers: {
          'Authorization': `Bearer ${accessToken}`,
          'Content-Type': 'application/json',
          'Content-Length': Buffer.byteLength(postData)
        }
      },
      (res) => {
        let body = '';
        res.on('data', (chunk) => (body += chunk));
        res.on('end', () => {
          if (res.statusCode >= 200 && res.statusCode < 300) {
            resolve(body);
          } else {
            reject(new Error(`Firebase Database update error HTTP ${res.statusCode}: ${body}`));
          }
        });
      }
    );

    req.on('error', reject);
    req.write(postData);
    req.end();
  });
}

function base64UrlEncode(str) {
  return Buffer.from(str)
    .toString('base64')
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');
}

main().catch((err) => {
  console.error('❌ Error updating Firebase:', err);
  process.exit(1);
});

