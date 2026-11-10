import { spawn } from 'node:child_process';
import { existsSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { resolve } from 'node:path';
import { stopChild } from './stop-child.mjs';

const root = fileURLToPath(new URL('../', import.meta.url));
for (const name of (process.env.NEON_ENV_FILE ? [process.env.NEON_ENV_FILE] : ['bdd/.env.local', 'bdd/.env'])) {
  const file = resolve(root, name);
  if (existsSync(file)) { process.loadEnvFile(file); break; }
}
if (!process.env.DATABASE_URL && !process.env.DATABASE_URL_POOLED) {
  console.error('Configura DATABASE_URL y DATABASE_URL_UNPOOLED en bdd/.env o bdd/.env.local.');
  process.exit(1);
}
const jar = resolve(root, 'backend/mygymbro-api/target/mygymbro-api-0.0.1-SNAPSHOT.jar');
if (!existsSync(jar)) { console.error('Ejecuta npm run build antes de iniciar.'); process.exit(1); }
const java = process.env.JAVA_HOME
  ? resolve(process.env.JAVA_HOME, 'bin', process.platform === 'win32' ? 'java.exe' : 'java') : 'java';
const child = spawn(java, ['-XX:MaxRAMPercentage=50', '-jar', jar], {
  cwd: root,
  env: { ...process.env, SPRING_PROFILES_ACTIVE: process.env.SPRING_PROFILES_ACTIVE || 'neon' },
  stdio: 'inherit',
});
child.on('error', () => { console.error('No se pudo iniciar Java 25.'); process.exitCode = 1; });
child.on('exit', code => { process.exitCode = code ?? 1; });
for (const signal of ['SIGINT', 'SIGTERM']) process.on(signal, () => stopChild(child));
