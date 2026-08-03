// scripts/get-jre.js
// Downloads and unpacks OpenJDK 17 using standard Node.js built-in modules.
// No npm packages required. No /bin/sh required.

const https = require('https');
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

const PROJECT_ROOT = path.join(__dirname, '..');
const JRE_DIR = path.join(PROJECT_ROOT, 'jre');
const JRE_JAVA = path.join(JRE_DIR, 'bin', 'java');
const JRE_URL = 'https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jre/hotspot/normal/eclipse';

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

// Pure JS POSIX tar parser (Unpacks tar archives without external libraries or system commands)
function extractTar(tarBuffer, targetDir, stripComponents = 1) {
  let offset = 0;

  while (offset + 512 <= tarBuffer.length) {
    const header = tarBuffer.subarray(offset, offset + 512);
    offset += 512;

    // Check for end of archive block
    if (header.every(b => b === 0)) break;

    // Extract filename and ustar prefix
    let rawName = header.toString('utf8', 0, 100).replace(/\0.*/, '');
    const prefix = header.toString('utf8', 345, 500).replace(/\0.*/, '');
    if (prefix) rawName = path.join(prefix, rawName);

    // Read file size (octal) & type flag
    const sizeStr = header.toString('utf8', 124, 136).replace(/\0.*/, '').trim();
    const size = parseInt(sizeStr, 8) || 0;
    const typeflag = String.fromCharCode(header[156]);

    // Strip top-level directory
    const parts = rawName.split('/').filter(Boolean);
    if (parts.length > stripComponents) {
      const relPath = parts.slice(stripComponents).join(path.sep);
      const fullPath = path.join(targetDir, relPath);

      if (typeflag === '5' || rawName.endsWith('/')) {
        // Directory
        fs.mkdirSync(fullPath, { recursive: true });
      } else if (typeflag === '0' || typeflag === '\0' || typeflag === '') {
        // Regular File
        const fileData = tarBuffer.subarray(offset, offset + size);
        fs.mkdirSync(path.dirname(fullPath), { recursive: true });
        fs.writeFileSync(fullPath, fileData);
        
        // Grant execute permissions for binaries
        if (relPath.startsWith('bin' + path.sep) || relPath.endsWith('java')) {
          try { fs.chmodSync(fullPath, 0o755); } catch (_) {}
        }
      }
    }

    // Move offset to next 512-byte block boundary
    offset += Math.ceil(size / 512) * 512;
  }
}

async function ensureJre() {
  if (fs.existsSync(JRE_JAVA)) {
    console.log('JRE already present at', JRE_JAVA);
    return;
  }

  console.log('JRE not found. Downloading OpenJDK 17 in pure Node mode...');
  
  if (!fs.existsSync(JRE_DIR)) {
    fs.mkdirSync(JRE_DIR, { recursive: true });
  }

  try {
    console.log('Downloading JRE archive...');
    const compressedBuffer = await fetchBuffer(JRE_URL);

    console.log('Decompressing gunzip...');
    const decompressedTar = zlib.gunzipSync(compressedBuffer);

    console.log('Unpacking tar entries into ./jre...');
    extractTar(decompressedTar, JRE_DIR, 1);

    console.log('JRE successfully installed at:', JRE_DIR);
  } catch (err) {
    console.error('Failed to download/extract JRE:', err.message);
    throw err;
  }
}

module.exports = { ensureJre };

if (require.main === module) {
  ensureJre().catch(() => process.exit(1));
}