// scripts/get-jre.js
// Auto-detects glibc vs musl (Alpine) containers to fetch the correct JRE architecture.

const https = require('https');
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

const JRE_DIR = path.join(__dirname, '..', 'jre');
const JRE_JAVA = path.join(JRE_DIR, 'bin', 'java');

function getAdoptiumUrl() {
  // Check if running in Alpine/musl container environment
  const isAlpine = fs.existsSync('/etc/alpine-release') || 
                   fs.existsSync('/lib/ld-musl-x86_64.so.1') || 
                   fs.existsSync('/lib/ld-musl-aarch64.so.1');

  if (isAlpine) {
    console.log('Detected Alpine/musl container environment.');
    return 'https://api.adoptium.net/v3/binary/latest/17/ga/alpine-linux/x64/jre/hotspot/normal/eclipse';
  }

  console.log('Detected standard glibc Linux container environment.');
  return 'https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jre/hotspot/normal/eclipse';
}

function fetchBuffer(url, redirects = 0) {
  return new Promise((resolve, reject) => {
    if (redirects > 5) return reject(new Error('Too many redirects while downloading JRE'));
    
    https.get(url, (res) => {
      if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
        return resolve(fetchBuffer(res.headers.location, redirects + 1));
      }
      if (res.statusCode !== 200) {
        return reject(new Error(`Download failed with status code: ${res.statusCode}`));
      }

      const chunks = [];
      res.on('data', chunk => chunks.push(chunk));
      res.on('end', () => resolve(Buffer.concat(chunks)));
      res.on('error', reject);
    }).on('error', reject);
  });
}

function extractTar(tarBuffer, targetDir, stripComponents = 1) {
  let offset = 0;

  while (offset + 512 <= tarBuffer.length) {
    const header = tarBuffer.subarray(offset, offset + 512);
    offset += 512;

    if (header.every(b => b === 0)) break;

    let rawName = header.toString('utf8', 0, 100).replace(/\0.*/, '');
    const prefix = header.toString('utf8', 345, 500).replace(/\0.*/, '');
    if (prefix) rawName = path.join(prefix, rawName);

    const sizeStr = header.toString('utf8', 124, 136).replace(/\0.*/, '').trim();
    const size = parseInt(sizeStr, 8) || 0;
    const typeflag = String.fromCharCode(header[156]);

    const parts = rawName.split('/').filter(Boolean);
    if (parts.length > stripComponents) {
      const relPath = parts.slice(stripComponents).join(path.sep);
      const fullPath = path.join(targetDir, relPath);

      if (typeflag === '5' || rawName.endsWith('/')) {
        fs.mkdirSync(fullPath, { recursive: true });
      } else if (typeflag === '0' || typeflag === '\0' || typeflag === '') {
        const fileData = tarBuffer.subarray(offset, offset + size);
        fs.mkdirSync(path.dirname(fullPath), { recursive: true });
        fs.writeFileSync(fullPath, fileData);
        
        // Ensure execution permissions on binary executables
        if (relPath.startsWith('bin' + path.sep) || relPath.includes('/bin/')) {
          try { fs.chmodSync(fullPath, 0o755); } catch (_) {}
        }
      }
    }

    offset += Math.ceil(size / 512) * 512;
  }
}

async function ensureJre() {
  if (fs.existsSync(JRE_JAVA)) {
    try {
      fs.chmodSync(JRE_JAVA, 0o755);
    } catch (_) {}
    console.log('JRE already present at', JRE_JAVA);
    return;
  }

  // If a previous incompatible JRE installation existed, clean it up
  if (fs.existsSync(JRE_DIR)) {
    fs.rmSync(JRE_DIR, { recursive: true, force: true });
  }
  fs.mkdirSync(JRE_DIR, { recursive: true });

  const downloadUrl = getAdoptiumUrl();
  console.log('Downloading JRE from:', downloadUrl);

  try {
    const compressedBuffer = await fetchBuffer(downloadUrl);
    console.log('Decompressing archive...');
    const decompressedTar = zlib.gunzipSync(compressedBuffer);

    console.log('Extracting JRE into ./jre...');
    extractTar(decompressedTar, JRE_DIR, 1);

    if (fs.existsSync(JRE_JAVA)) {
      fs.chmodSync(JRE_JAVA, 0o755);
    }

    console.log('JRE installation complete at:', JRE_DIR);
  } catch (err) {
    console.error('Failed to prepare JRE:', err.message);
    throw err;
  }
}

module.exports = { ensureJre, JRE_JAVA };

if (require.main === module) {
  ensureJre().catch(() => process.exit(1));
}