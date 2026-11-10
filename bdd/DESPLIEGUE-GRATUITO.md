# Preparación para Render Free y Neon Free

Estado: proyecto Neon enlazado y PostgreSQL conectado mediante JDBC; la API aún
no está publicada en Internet. El envío de
correo todavía requiere adaptación: Render Free bloquea SMTP en los puertos
25, 465 y 587. No desplegar el registro como operativo hasta resolverlo con un
proveedor de correo por HTTPS y probar la confirmación real.

## Base de datos

Crear un proyecto Neon en el plan Free, sin tarjeta. Elegir una región europea
cercana a la de la API. Usar una base nueva, no una copia del esquema MySQL.

El perfil `render` usa migraciones PostgreSQL independientes en `db/postgresql`.
Las migraciones MySQL originales no se modifican. Esto crea las tablas, pero no
transfiere datos existentes: si hay usuarios locales, hay que planificar su
migración antes de cambiar la aplicación.

Las variables oficiales de Neon se guardan en `bdd/.env`, ignorado por Git.
El adaptador Java las consume con los perfiles `neon` y `render`:

- `DATABASE_URL`: conexión de aplicación, normalmente con `-pooler`.
- `DATABASE_URL_UNPOOLED`: conexión directa utilizada exclusivamente por Flyway.
- `DATABASE_URL_POOLED`: alias opcional que se conserva si ya existía.

Se mantienen JDBC, JPA y Flyway. El adaptador extrae las credenciales de la URL
para que no aparezcan en los registros JDBC, traduce `channel_binding` y exige
`sslmode=verify-full` usando el almacén de confianza de Java.

Configurar en Render como variables privadas, nunca en Git:

- `SPRING_PROFILES_ACTIVE=render`
- `DATABASE_URL`: valor con pool obtenido de Neon
- `DATABASE_URL_UNPOOLED`: valor directo obtenido de Neon
- `PUBLIC_BASE_URL`: URL HTTPS pública de la API una vez conocida
- `GOOGLE_CLIENT_ID`: identificador de cliente autorizado, si se usa Google

Las variables oficiales usan formato `postgresql://`: el adaptador las convierte
a JDBC. Se conserva también la alternativa anterior `DB_URL` JDBC y
`DB_USERNAME`/`DB_PASSWORD` cuando no hay variables `DATABASE_URL*`.
Las credenciales limitan el acceso a la
base; no se trata de una subred privada compartida como la prevista en Oracle.

## API

Desde la raíz, con Java 25 y Node 24 o posterior:

```text
npm run build
npm start
```

`build` ejecuta Maven `verify` y sus pruebas H2. `start` carga `bdd/.env.local` o
`bdd/.env` y activa por defecto el perfil `neon`, accesible en `127.0.0.1:8080`.
El perfil `render` escucha en todas las interfaces y usa el puerto del proveedor.
Maven Wrapper conserva su versión; se corrige una comprobación nula de enlaces
de directorio para que funcione en Windows.

La herramienta npm se instaló de forma portable en `.tools`, ya que no estaba
disponible en PATH. En este equipo se puede invocar `.tools\bin\npm.cmd` en
lugar de `npm`. El CLI Neon está en `.tools\neon-global\neon.cmd`.

Las skills `neon` y `neon-postgres` están en `.agents/skills`. El MCP para Codex
está en `.codex/config.toml`, usa OAuth sin claves dentro del repositorio y está
limitado al proyecto enlazado. La autenticación CLI y MCP son independientes:
Codex puede solicitar autorización del MCP en su primer uso.

Publicar el repositorio solo tras revisar que no incluya credenciales, archivos
del IDE con secretos ni datos personales. La carpeta backend todavía debe
añadirse al control de versiones; no se ha realizado ningún commit ni push.

En Render crear un Web Service conectado al repositorio:

- Root Directory: `backend/mygymbro-api`
- Runtime: Docker (el Dockerfile utiliza Java 25)
- Instance Type: Free
- Región: la europea más próxima a Neon
- Variables privadas: las indicadas arriba y las del proveedor de correo cuando
  esté adaptado; revisar `CONFIGURACION-ACCESO.md` para Google y correo.

No añadir tarjeta ni seleccionar un servicio de pago. No crear Render Postgres
Free: caduca a los 30 días. El Dockerfile usa la compilación Maven; las pruebas
deben ejecutarse previamente en el ordenador o en integración continua.

El puerto se obtiene de `PORT`. El pool de base de datos tiene un máximo de
tres conexiones. Java limita su heap al 50 % de la memoria del contenedor;
hay que observar el consumo total real tras desplegar.

## Comprobación antes de compartir la app

1. Verificar Flyway y arranque contra PostgreSQL real (H2 no lo sustituye).
2. Probar registro, envío y confirmación de correo, login y cierre de sesión.
3. Probar Google si está configurado.
4. Configurar en Android la URL HTTPS de Render y probar desde fuera de casa.
5. Comprobar recuperación tras suspensión por inactividad y consumo de memoria.
6. Exportar copias de la base periódicamente y revisar cuotas de ambos servicios.

`npm run test:neon` prueba registro, confirmación, login, sesión, logout y
revocación contra una rama desechable cuyo entorno está en
`.tools/.env.verification`. Utiliza un receptor SMTP local, no envía correos
externos ni comprueba Google. Crea datos exclusivamente en esa rama; nunca
apuntar ese archivo a production. La rama temporal debe eliminarse al terminar.

Para repetir la prueba, crear una rama temporal de production y descargar
exclusivamente sus credenciales (el `bdd/.env` principal se conserva).
Los comandos Neon siguientes se ejecutan desde `bdd`; la prueba npm, desde la raíz:

```text
cd bdd
neon branches create --name codex-gymbro-verification --parent production --cu 0.25 --no-secrets
neon env pull --branch codex-gymbro-verification --file ../.tools/.env.verification --service postgres
cd ..
npm run test:neon
cd bdd
neon branches delete codex-gymbro-verification
cd ..
```

Validación realizada el 4 de noviembre de 2026: Maven `verify` con 29 pruebas
correctas, flujo HTTP completo contra PostgreSQL en una rama aislada y arranque
de production con Flyway V1/V2, roles y escritura/lectura revertida. La rama de
prueba se elimina al terminar; los servidores de prueba se cierran.

Render Free se suspende tras 15 minutos sin tráfico y puede tardar alrededor de
un minuto en despertar. No guardar archivos subidos en su disco efímero. Neon
Free tiene cuotas de almacenamiento y cómputo; no es disponibilidad garantizada
ni una promesa de gratuidad perpetua de los proveedores.

Referencias consultadas el 4 de noviembre de 2026:
- https://render.com/docs/free
- https://render.com/docs/docker
- https://neon.com/pricing
