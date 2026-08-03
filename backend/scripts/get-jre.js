// downloads a portable OpenJDK 17 JRE into ./jre at install time
// runs as npm postinstall, no root/apt needed since it just unpacks a tarball
const https = require('https');
const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const JRE_DIR = path.join(__dirname, '..', 'jre');
const URL = 'https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jre/hotspot/normal/eclipse';

if (fs.existsSync(path.join(JRE_DIR, 'bin', 'java'))) {
  console.log('JRE already present, skipping download');
  process.exit(0);
}

fs.mkdirSync(JRE_DIR, { recursive: true });
const tarPath = path.join(__dirname, '..', 'jre.tar.gz');

function download(url, dest, redirects = 0) {
  return new Promise((resolve, reject) => {
    if (redirects > 5) return reject(new Error('too many redirects'));
    https.get(url, (res) => {
      if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
        return resolve(download(res.headers.location, dest, redirects + 1));
      }
      if (res.statusCode !== 200) return reject(new Error('download failed: ' + res.statusCode));
      const file = fs.createWriteStream(dest);
      res.pipe(file);
      file.on('finish', () => file.close(resolve));
    }).on('error', reject);
  });
}

download(URL, tarPath)
  .then(() => {
    execSync(`tar -xzf ${tarPath} -C ${JRE_DIR} --strip-components=1`);
    fs.unlinkSync(tarPath);
    console.log('JRE installed at', JRE_DIR);
  })
  .catch((err) => {
    console.error('JRE download failed:', err.message);
    process.exit(1);
  });