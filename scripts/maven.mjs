import { spawn } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { resolve } from 'node:path';

const backend = fileURLToPath(new URL('../backend/mygymbro-api/', import.meta.url));
const args = ['-B', '-ntp', `-Dmaven.repo.local=${resolve(backend, 'target/maven-repository')}`, 'verify'];
// La compilación habitual siempre usa H2; las pruebas remotas se ejecutan aparte.
const env = { ...process.env };
delete env.TEST_DB_URL;
delete env.TEST_DB_USERNAME;
delete env.TEST_DB_PASSWORD;
const child = process.platform === 'win32'
  ? spawn('cmd.exe', ['/d', '/s', '/c', `mvnw.cmd ${args.map(arg => `"${arg}"`).join(' ')}`], { cwd: backend, env, stdio: 'inherit', windowsVerbatimArguments: true })
  : spawn('sh', ['./mvnw', ...args], { cwd: backend, env, stdio: 'inherit' });
child.on('error', () => { console.error('No se pudo ejecutar Maven. Revisa Java 25.'); process.exitCode = 1; });
child.on('exit', code => { process.exitCode = code ?? 1; });
