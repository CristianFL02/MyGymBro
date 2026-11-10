# Base de datos

Esta carpeta reúne la configuración de PostgreSQL en Neon:

- `neon.ts`: política de los servicios y ramas de Neon.
- `.neon`: enlace privado al proyecto y rama, ignorado por Git.
- `.env` / `.env.local`: credenciales privadas, ignoradas por Git.
- `.env.example`: ejemplo sin credenciales reales.
- [Configuración y comprobaciones](NEON-SETUP.md).
- [Despliegue gratuito](DESPLIEGUE-GRATUITO.md).

Desde la raíz de MyGymBro:

```text
npm run build
npm start
```

El arranque lee `bdd/.env.local` o `bdd/.env`. La API y las migraciones Flyway
permanecen en `backend/mygymbro-api`. Las skills y el MCP permanecen en
`.agents` y `.codex` para que Codex los encuentre.

Ejecutar los comandos Neon desde esta carpeta para que encuentren `.neon`
y `neon.ts`. Si se usa la instalación portable de este equipo, desde `bdd`:

```text
..\.tools\neon-global\neon.cmd status
..\.tools\neon-global\neon.cmd deploy
```

No es necesario volver a desplegar para reorganizar estos archivos.
