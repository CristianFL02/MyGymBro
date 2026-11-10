import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { createServer } from 'node:net';
import { createWriteStream, existsSync, readFileSync } from 'node:fs';
import { parseEnv } from 'node:util';
import { resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { setTimeout as delay } from 'node:timers/promises';
import { stopChild } from './stop-child.mjs';

// Solo una rama desechable, nunca la base production ni TEST_DB_* de la suite.
const root = fileURLToPath(new URL('../', import.meta.url));
const envFile = resolve(root, '.tools/.env.verification');
assert.ok(existsSync(envFile), 'Primero descarga el entorno de la rama temporal');
const smtpSockets = new Set();
const smtp = createServer(socket => {
  smtpSockets.add(socket);
  socket.on('close', () => smtpSockets.delete(socket));
  socket.on('error', () => {});
  socket.setEncoding('utf8');
  socket.write('220 localhost ESMTP test\r\n');
  let buffer = '', data = false, message = '';
  socket.on('data', chunk => {
    buffer += chunk;
    while (buffer.includes('\r\n')) {
      const end = buffer.indexOf('\r\n');
      const line = buffer.slice(0, end); buffer = buffer.slice(end + 2);
      if (data) {
        if (line === '.') {
          data = false;
          const decoded = message.replace(/=\r\n/g, '').replace(/=3D/g, '=');
          verificationToken = decoded.match(/token=([A-Za-z0-9_-]{43})/)?.[1];
          socket.write('250 Accepted for local test\r\n');
        } else message += line + '\r\n';
      } else if (/^EHLO/i.test(line)) socket.write('250-localhost\r\n250 SIZE 100000\r\n');
      else if (/^DATA/i.test(line)) { data = true; message = ''; socket.write('354 End with .\r\n'); }
      else if (/^QUIT/i.test(line)) socket.end('221 Bye\r\n');
      else socket.write('250 OK\r\n');
    }
  });
});
let verificationToken;
await new Promise(resolve => smtp.listen(0, '127.0.0.1', resolve));
const port = 18087;
const base = `http://127.0.0.1:${port}`;
const log = createWriteStream(resolve(root, '.tools/neon-e2e-api.log'));
const env = {
  ...process.env, ...parseEnv(readFileSync(envFile, 'utf8')), SPRING_PROFILES_ACTIVE: 'neon',
  PORT: String(port), SERVER_ADDRESS: '127.0.0.1', PUBLIC_BASE_URL: 'http://localhost:' + port,
  MAIL_HOST: '127.0.0.1', MAIL_PORT: String(smtp.address().port), MAIL_FROM: 'test@example.com',
  SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH: 'false',
  SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE: 'false',
  SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_REQUIRED: 'false',
};
const java = process.env.JAVA_HOME
  ? resolve(process.env.JAVA_HOME, 'bin', process.platform === 'win32' ? 'java.exe' : 'java') : 'java';
const app = spawn(java, ['-XX:MaxRAMPercentage=50', '-jar', resolve(root, 'backend/mygymbro-api/target/mygymbro-api-0.0.1-SNAPSHOT.jar')], { cwd: root, env, stdio: ['ignore', 'pipe', 'pipe'] });
app.stdout.pipe(log); app.stderr.pipe(log);
let exited = false; app.on('exit', () => { exited = true; });
async function request(path, body, token) {
  return fetch(base + path, {
    method: body === undefined ? 'GET' : 'POST',
    headers: { ...(body === undefined ? {} : { 'Content-Type': 'application/json' }), ...(token ? { Authorization: 'Bearer ' + token } : {}) },
    ...(body === undefined ? {} : { body: JSON.stringify(body) }),
    signal: AbortSignal.timeout(20000),
  });
}
try {
  let ready = false;
  for (let i = 0; i < 90; i++) {
    if (exited) throw new Error('La API terminó antes de arrancar; consulta el registro privado');
    try { if ((await request('/api/auth/me')).status === 401) { ready = true; break; } } catch {}
    await delay(1000);
  }
  assert.ok(ready, 'La API no arrancó a tiempo');
  console.log('PASS: Spring Boot arranca con las migraciones PostgreSQL y JPA validate');
  const email = `neon-e2e-${Date.now()}@example.com`;
  const password = 'PruebaNeon-' + crypto.randomUUID();
  let response = await request('/api/auth/register', { nombre: 'Prueba Neon', email, contrasena: password, roles: ['CLIENTE'] });
  assert.equal(response.status, 201, 'Registro');
  assert.ok(verificationToken, 'No se recibió el correo en el receptor SMTP local');
  console.log('PASS: registro persistido y confirmación recibida en SMTP local de prueba');
  response = await request('/api/auth/login', { email, contrasena: password });
  assert.equal(response.status, 403, 'No permite acceso sin confirmar correo');
  response = await request('/api/auth/confirm-email', { token: verificationToken });
  assert.equal(response.status, 200, 'Confirmación');
  response = await request('/api/auth/login', { email, contrasena: password });
  assert.equal(response.status, 200, 'Login');
  const session = await response.json();
  assert.ok(session.accessToken);
  response = await request('/api/auth/me', undefined, session.accessToken);
  assert.equal(response.status, 200, 'Sesión leída de Neon');
  assert.equal((await response.json()).email, email);
  response = await request('/api/auth/logout', {}, session.accessToken);
  assert.equal(response.status, 204, 'Logout');
  response = await request('/api/auth/me', undefined, session.accessToken);
  assert.equal(response.status, 401, 'Token revocado');
  console.log('PASS: confirmación, login, consulta de sesión, logout y revocación contra Neon real');
  console.log('Nota: prueba aislada; correo externo y acceso Google no se han probado');
} finally {
  stopChild(app);
  for (const socket of smtpSockets) socket.destroy();
  smtp.close();
  await Promise.race([new Promise(resolve => app.once('exit', resolve)), delay(5000)]);
  log.end();
}
