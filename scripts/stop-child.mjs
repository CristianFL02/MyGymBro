import { spawnSync } from 'node:child_process';

// Cierra únicamente el proceso creado por la prueba, incluidos sus descendientes.
export function stopChild(child) {
  if (child.exitCode !== null || !child.pid) return;
  if (process.platform === 'win32') {
    spawnSync('taskkill.exe', ['/PID', String(child.pid), '/T', '/F'], { stdio: 'ignore' });
  } else child.kill('SIGTERM');
}
