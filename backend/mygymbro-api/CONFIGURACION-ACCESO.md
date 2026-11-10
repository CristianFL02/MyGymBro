# Configurar el acceso de MyGymBro

El código ya está preparado. Estas conexiones requieren tu cuenta de Google. No hace falta compartir contraseñas en el chat.

## Primero configurar el envío desde Gmail

1. Abrir la seguridad de tu cuenta Google y activar la verificación en dos pasos, si no está activada.
2. Crear una contraseña de aplicación para MyGymBro. Google puede no ofrecer esta opción en algunas cuentas; si no aparece, consultar la guía oficial antes de continuar.
3. En IntelliJ, abrir la configuración con la que ejecutas MygymbroApiApplication y añadir estas variables de entorno, sustituyendo los valores entre corchetes:

```text
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=[tu dirección Gmail]
MAIL_PASSWORD=[contraseña de aplicación, no la contraseña habitual de Google]
MAIL_FROM=[tu dirección Gmail]
PUBLIC_BASE_URL=http://localhost:8080
```

PUBLIC_BASE_URL debe apuntar a donde esté accesible la API. localhost sirve para abrir el enlace en el mismo ordenador. Para abrirlo desde el teléfono o para otros usuarios, hace falta una dirección HTTPS accesible desde ese dispositivo. El remitente y esa dirección se configuran en el servidor; no se aceptan desde el registro del usuario.

El programa conecta a Gmail con STARTTLS. Si falta la configuración o el envío falla, devuelve un error y no anuncia que el correo se ha enviado. No imprime enlaces de confirmación como alternativa al correo.

Guías de Google: [Verificación en dos pasos](https://support.google.com/accounts/answer/185839?hl=es) y [Contraseñas de aplicación](https://support.google.com/accounts/answer/185833?hl=es).

## Después configurar el acceso con Google

1. Crear o elegir un proyecto en Google Cloud y configurar Google Auth Platform con los datos de MyGymBro y sus usuarios de prueba, si la aplicación está en pruebas.
2. Crear un cliente OAuth de tipo Aplicación web para identificar a este servidor. Guardar su ID en la variable GOOGLE_CLIENT_ID de IntelliJ. Para verificar identificaciones de Google en este flujo no se necesita guardar un secreto de cliente en Android ni en esta API.
3. Cuando se implemente Android, crear su cliente Android con el nombre de paquete y la huella de firma correspondientes. Usar Credential Manager con el ID del cliente web como serverClientId y enviar el idToken a POST /api/auth/google.

La API no confía en un correo o identificador escrito por el teléfono: verifica la identificación firmada por Google. Sin GOOGLE_CLIENT_ID, el acceso Google permanece sin configurar y responde 503.

Guías oficiales: [Acceso con Google en Android](https://developer.android.com/identity/sign-in/credential-manager-siwg) y [Verificación en el servidor](https://developers.google.com/identity/sign-in/android/backend-auth).

## Base de datos

```text
DB_URL=jdbc:mysql://localhost:3306/mygymbro
DB_USERNAME=[usuario MySQL]
DB_PASSWORD=[contraseña MySQL]
```

La base debe existir antes de arrancar. Las tablas las gestiona Flyway. No escribir estas contraseñas en el código ni subirlas a Git.
