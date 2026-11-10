# MyGymBro API

Preparación para alojamiento gratuito con PostgreSQL:
[Despliegue en Render y Neon](../../bdd/DESPLIEGUE-GRATUITO.md).

## Cómo funcionará el acceso

El usuario escribe su nombre, correo, contraseña y selecciona CLIENTE, ENTRENADOR o ambos. Recibe un correo con un enlace y pulsa Confirmar correo. Después ya puede entrar con su correo y contraseña.

También puede registrarse o entrar con Google. Una vez dentro, su sesión permanece abierta hasta que la cierre. Las contraseñas del registro normal se guardan con BCrypt. Las cuentas creadas con Google no necesitan una contraseña propia de MyGymBro.

Esta entrega implementa el servidor. Las pantallas de Android, incluido el botón de Google y el almacenamiento de la sesión en el teléfono, se conectarán después. La página para confirmar el correo sí está incluida en la API.

## Qué falta conectar

El código está preparado, pero los correos reales y el acceso real con Google requieren configurar tu cuenta. Todavía no se han conectado ni probado con servicios externos. Consulta [Configuración del acceso](CONFIGURACION-ACCESO.md).

## Arranque

Usar Java 25. Crear una base MySQL vacía y configurar en IntelliJ las variables DB_URL, DB_USERNAME y DB_PASSWORD. Ejecutar MygymbroApiApplication. Flyway crea o actualiza las tablas sin borrar usuarios. Las cuentas de la primera entrega quedan pendientes de confirmar correo; pueden solicitar un nuevo enlace.

La migración conserva las contraseñas anteriores. Al iniciar sesión se pasan a BCrypt cuando son compatibles con su límite de 72 bytes. Una contraseña antigua superior a ese límite conserva su hash anterior para evitar truncarla.

## Rutas para conectar la aplicación

| Método | Ruta | Función |
| --- | --- | --- |
| POST | /api/auth/register | Crear cuenta pendiente y enviar correo |
| POST | /api/auth/confirm-email | Confirmar con el token del correo, desde la aplicación |
| POST | /api/auth/resend-verification | Pedir otro enlace; máximo uno por minuto y destinatario |
| GET | /api/auth/verify-email?token=... | Abrir página de confirmación sin consumir el enlace |
| POST | /api/auth/verify-email | Confirmar mediante el botón de la página |
| POST | /api/auth/login | Entrar con correo y contraseña |
| POST | /api/auth/google | Entrar o crear una cuenta con una identificación de Google verificada |
| POST | /api/auth/google/link | Vincular Google desde una sesión de la misma cuenta |
| GET | /api/auth/me | Consultar el usuario actual |
| POST | /api/auth/logout | Cerrar la sesión actual |

Registro:

```json
{"nombre":"Ana","email":"ana@example.com","contrasena":"UnaClaveDeEjemplo123","roles":["CLIENTE","ENTRENADOR"]}
```

Login:

```json
{"email":"ana@example.com","contrasena":"UnaClaveDeEjemplo123"}
```

Google:

```json
{"idToken":"<identificación obtenida de Google por Android>","roles":["CLIENTE"]}
```

Los roles son obligatorios al crear una cuenta Google y no modifican los roles de una cuenta ya existente. El servidor verifica firma, emisor, destinatario de la identificación, caducidad y correo verificado. Identifica la cuenta Google mediante sub, no por su correo.

Si el correo ya está registrado con contraseña, no se unen las cuentas automáticamente: primero iniciar sesión y llamar a /google/link con idToken. El correo de Google debe coincidir con el de la cuenta confirmada.

Confirmación JSON: {"token":"<token del correo>"}. Reenvío: {"email":"ana@example.com"}. Las rutas privadas requieren Authorization: Bearer <accessToken>. expiresAt es null: la sesión no tiene caducidad automática. Google sí exige que su identificación sea válida cuando se usa para entrar; eso no caduca la sesión de MyGymBro.

Los enlaces de correo duran 24 horas, son de un solo uso y se almacenan como hash. Un nuevo envío invalida el enlace anterior. Las sesiones se almacenan también como hash y cerrar sesión revoca solamente la actual.

## Pruebas

Ejecutar mvnw.cmd verify con Java 25 desde la carpeta de la API. Por defecto se utiliza H2 en memoria, con las migraciones reales. Se comprueban registro, BCrypt, confirmación y reenvío, errores de envío, Google, validación de firmas y sesiones sin caducidad.

Para repetir las pruebas de integración contra MySQL usar una base exclusiva de pruebas y las variables TEST_DB_URL, TEST_DB_USERNAME y TEST_DB_PASSWORD. Las pruebas borran usuarios, roles asignados, verificaciones y sesiones de esa base. Nunca utilizar una base con datos que se quieran conservar. La prueba de actualización de esquema usa siempre una H2 aislada.

Quedan pendientes RFG4 (vinculación entre entrenador y cliente), entrenamiento, nutrición y salud.
