# Configuración de Neon — 4 de noviembre de 2026

## Organización actual

La configuración se trasladó a `bdd`: `.env`, `.env.example`, `.neon`,
`neon.ts` y las dos guías. Los comandos Neon se ejecutan desde `bdd`.
Los comandos npm se ejecutan desde la raíz de MyGymBro y el arranque lee
`bdd/.env.local` o `bdd/.env`. El historial siguiente describe la instalación
original, anterior al traslado; las salidas históricas se conservan.

La base PostgreSQL del proyecto existente `mygymbro-bdd` está conectada a la API
Java. No se ha publicado la API en Internet ni contratado un plan de pago.

## Cambios de esta configuración

- Instalación portable de npm y Neon CLI 8.18.1 en `.tools`, ignorada por Git.
  Las dependencias adicionales se instalaron con scripts de instalación
  desactivados. La autenticación CLI se completó en el navegador del usuario.
- `.neon`: enlace local al proyecto indicado, organización indicada y rama
  `production`; está ignorado por Git. No se creó un segundo proyecto.
- `neon.ts`: configuración de Postgres únicamente. Se conservó la política de
  ramas generada, retirando el `auth: false` explícito para no gestionar ni
  desactivar servicios de identidad existentes.
- `package.json` y `package-lock.json`: dependencias de configuración de Neon y
  comandos `build`, `start` y `test:neon`. La API sigue siendo Java/Maven.
- `.agents/skills/neon`, `.agents/skills/neon-postgres` y `skills-lock.json`:
  skills oficiales instaladas y comprobadas como actualizadas. Se consultó
  también la dirección `.well-known/agent-skills/neon/SKILL.md` indicada.
- `.codex/config.toml`: servidor MCP Neon con OAuth, sin clave en el archivo,
  limitado al proyecto `weathered-flower-73965268`. No se modificó el archivo
  global de configuración de Codex. El servidor está configurado; su conexión
  desde un nuevo chat todavía puede requerir autorización OAuth independiente.
- El primer `neon init` recomendado detectó otros editores y añadió también
  configuración MCP OAuth en `C:/Users/Cristian/.copilot/mcp-config.json`,
  `C:/Users/Cristian/.junie/mcp/mcp.json` y
  `C:/Users/Cristian/.codeium/windsurf/mcp_config.json`. No se usan para la API.
  Los intentos de instalar sus plugins y skills fallaron; después se instalaron
  específicamente las skills y el MCP de Codex en este proyecto.
- `.gitignore`, `.env` y `.env.example`: credenciales fuera de Git y ejemplo
  sin secretos. Neon actualizó sus propias variables; se conservó el alias
  `DATABASE_URL_POOLED` suministrado. Nunca copiar estas credenciales a Android.
- `NeonEnvironmentPostProcessor.java` y `META-INF/spring.factories`: adaptación
  de las variables oficiales a JDBC/JPA y Flyway. Pool para aplicación, conexión
  directa para migraciones, credenciales fuera de la URL registrada y TLS con
  validación de certificado y hostname mediante el almacén de Java.
- `application-neon.properties` y `application-render.properties`: perfiles
  locales y de alojamiento. Se conserva la alternativa MySQL local sin activar
  esos perfiles. No se cambian las migraciones MySQL.
- `mvnw.cmd`: corrección de la lectura de un valor nulo de `Target` en Windows.
- `scripts/maven.mjs`, `start.mjs`, `stop-child.mjs`, `verify-neon.mjs`:
  compilación, arranque, cierre de procesos y prueba HTTP real con correo local.
- `NeonEnvironmentPostProcessorTests.java`: cinco pruebas del adaptador,
  selección de conexiones, errores sin secretos y conservación del modo local.
- Guía `backend/mygymbro-api/DESPLIEGUE-GRATUITO.md` actualizada.

Los cambios anteriores de preparación ya habían añadido el controlador JDBC,
el módulo Flyway PostgreSQL, migraciones PostgreSQL separadas, Dockerfile y
prueba de migraciones; ahora se han comprobado contra Neon real.

## Comprobaciones

Salida de `neon deploy`:

```text
No changes — branch production already matches the policy.
Utilized services: Postgres
Pulled 3 Neon variables into .env
```

Salida de `npm run build` (ejecuta Maven verify):

```text
Tests run: 29, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Prueba HTTP completa en una rama temporal clonada de production:

```text
PASS: Spring Boot arranca con las migraciones PostgreSQL y JPA validate
PASS: registro persistido y confirmación recibida en SMTP local de prueba
PASS: confirmación, login, consulta de sesión, logout y revocación contra Neon real
```

Comprobación en production, inicialmente vacía:

```text
PASS: API iniciada en local con production de Neon; ruta privada responde HTTP 401 sin token
PASS: PostgreSQL conectado con sslmode=verify-full y certificados verificados por Java
Tablas public: 6
PASS: Flyway V1
PASS: Flyway V2
PASS: roles iniciales disponibles
PASS: escritura/lectura transaccional; revertida sin datos persistentes
```

Las tablas son las cinco de autenticación y el historial de Flyway. No se
ejecutó la suite que borra usuarios contra production. La prueba completa usó
una rama desechable y un receptor SMTP local: no acredita envío a buzones
externos ni Google. Los registros privados de ejecución están en `.tools` y
no deben publicarse sin revisión.

## Incidencias y límites

- npm no estaba en PATH: se descargó una copia portable del registro oficial.
  El primer `init` falló al invocar esa copia; se corrigieron sus lanzadores y
  se completó la configuración con comandos dirigidos a Codex.
- La revisión automática bloqueó una instalación con scripts mientras había
  credenciales en `.env`. Se resolvió instalando con `--ignore-scripts`.
- La cuenta gratuita rechazó configurar explícitamente el intervalo de
  suspensión de la rama temporal. Se creó conservando el valor del servicio.
- La consulta inicial de `pg_stat_ssl` no acreditó TLS desde el cliente. Se
  sustituyó por una conexión JDBC con `sslmode=verify-full`, que pasó.
- Se corrigió el cierre de procesos de prueba en Windows para terminar los
  procesos Java y sockets locales creados por la prueba.
- La API no está alojada públicamente. El SMTP real debe adaptarse antes de
  desplegar en Render Free. Google y la aplicación Android no se han probado.
- La contraseña de base de datos fue compartida en el chat: conviene rotarla
  desde Neon y volver a descargar las variables antes del uso real.

## Uso local

Desde la raíz de MyGymBro, con Java 25 y Node 24 o posterior:

```text
npm run build
npm start
```

Si npm no está disponible, usar `.tools\bin\npm.cmd` en su lugar. El arranque
local escucha en `127.0.0.1:8080`, usa Neon y no publica un puerto de la base de
datos en el ordenador. Para Render, usar el perfil `render` y las variables
descritas en la guía de despliegue.
