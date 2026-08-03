// scripts/get-jre.js
// Downloads a portable OpenJDK 17 JRE into ./jre at runtime or postinstall
// Uses pure JS streaming with zlib + tar (NO system shell or tar binary needed!)

const https = require('https');
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');
const tar = require('tar');

const PROJECT_ROOT = path.join(__dirname, '..');
const JRE_DIR = path.join(PROJECT_ROOT, 'jre');
const JRE_JAVA = path.join(JRE_DIR, 'bin', 'java');
const JRE_URL = 'https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jre/hotspot/normal/eclipse';

function downloadStream(url, redirects = 0) {
  return new Promise((resolve, reject) => {
    if (redirects > 5) return reject(new Error('Too many redirects while downloading JRE'));
    
    https.get(url, (res) => {
      if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
        return resolve(downloadStream(res.headers.location, redirects + 1));
      }
      if (res.statusCode !== 200) {
        return reject(new Error(`JRE download failed with status code: ${res.statusCode}`));
      }
      resolve(res);
    }).on('error', reject);
  });
}

async function ensureJre() {
  if (fs.existsSync(JRE_JAVA)) {
    console.log('✅ JRE already present at', JRE_JAVA);
    return;
  }

  console.log('🚀 JRE not found! Downloading OpenJDK 17 in pure JS mode (no shell required)...');
  
  if (!fs.existsSync(JRE_DIR)) {
    fs.mkdirSync(JRE_DIR, { recursive: true });
  }

  try {
    const stream = await downloadStream(JRE_URL);

    // Pipe HTTPS response -> Gunzip -> Tar Extractor (Pure JS, no shell or system 'tar' required)
    await new Promise((resolve, reject) => {
      stream
        .pipe(zlib.createGunzip())
        .pipe(
          tar.x({
            cwd: JRE_DIR,
            strip: 1 // Strips top-level directory inside tar
          })
        )
        .on('finish', resolve)
        .on('error', reject);
    });

    // Ensure Java binary has execution permissions if on Unix/Linux
    try {
      fs.chmodSync(JRE_JAVA, 0o755);
    } catch (_) {
      // Ignore if on non-posix filesystems
    }

    console.log('🎉 JRE successfully downloaded and extracted to:', JRE_DIR);
  } catch (err) {
    console.error('❌ Failed to download/extract JRE:', err.message);
    throw err;
  }
}

module.exports = { ensureJre };

// Allow direct execution via CLI (e.g. `node scripts/get-jre.js`)
if (require.main === module) {
  ensureJre().catch(() => process.exit(1));
}