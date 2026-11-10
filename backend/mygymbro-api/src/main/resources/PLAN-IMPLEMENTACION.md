# Plan de implementación de MyGymBro

Fuente: C:\Users\Cristian\Downloads\Memoria Proyecto.docx, texto y diagrama de autenticación revisados el 30 de octubre de 2026.

## Estado inspeccionado

Proyecto original: C:\Users\Cristian\Desktop\MyGymBro\backend\MyGymBroAPI.
El pom.xml declara Spring Boot 4.1.2, Java 25, Web MVC, JPA, Security, Validation y MySQL. Solo se localizaron la clase de arranque y su prueba inicial. application.properties solo declara spring.application.name.
La carpeta de trabajo actual es AppGym; el proyecto original está fuera de las rutas habilitadas para edición. No se han modificado sus archivos.

## Arquitectura propuesta

Una API y una base MySQL, con paquetes por funcionalidad: autenticacion, entrenamiento, nutricion y salud. Dentro de cada módulo, separar API, lógica de aplicación y persistencia cuando corresponda. Esta organización es una propuesta técnica, no un requisito adicional. No crear proyectos independientes ni microservicios para esta primera entrega.
Los módulos consultarán la identidad y los vínculos aceptados antes de acceder a datos de clientes. Un vínculo pendiente o rechazado no concederá acceso.

## Trazabilidad y entregas

| Entrega | Requisito | Comportamiento de la memoria |
| --- | --- | --- |
| Autenticación | RFG1 | Registro con datos básicos y selección de rol cliente o entrenador |
| Autenticación | RFG2 | Inicio de sesión con credenciales |
| Autenticación | RFG3 | Cierre de la sesión activa |
| Vinculación | RFG4 | Solicitud del entrenador y aceptación del cliente |
| Entrenamiento | RFE1 | Crear rutina con nombre |
| Entrenamiento | RFE2 | Añadir ejercicios a los días de una rutina |
| Entrenamiento | RFE3 | Editar y eliminar rutinas propias |
| Entrenamiento | RFE4 | Duplicar rutina |
| Entrenamiento | RFE5 | Registrar series, repeticiones y carga de una sesión |
| Entrenamiento | RFE6 | Entrenador corrige registros de un cliente vinculado, según CUE-5 |
| Entrenamiento | RFE7 | Consultar historial por período |
| Entrenamiento | RFE8 | Comparar un ejercicio entre dos sesiones |
| Entrenamiento | RFE9 | Crear o asignar rutina a un cliente vinculado |
| Nutrición | RFN1 | Crear dieta con nombre |
| Nutrición | RFN2 | Añadir alimentos existentes a dieta |
| Nutrición | RFN3 | Añadir alimentos no registrados |
| Nutrición | RFN4 | Asignar dieta a cliente vinculado |
| Nutrición | RFN5 | Crear comidas como conjuntos de alimentos |
| Nutrición | RFN6 | Consultar registro propio de alimentación |
| Nutrición | RFN7 | Entrenador consulta registro de clientes vinculados |
| Salud | RFSB1 | Registrar peso y consultar media y tendencia |
| Salud | RFSB2 | Registrar sueño y consultar media y regularidad |
| Salud | RFSB3 | Registrar moral y consultar media |
| Salud | RFSB4 | Entrenador consulta salud de un cliente vinculado |

## Secuencia de trabajo

1. Resolver las decisiones de autenticación que afectan al modelo de datos. Leer instrucciones locales del proyecto antes de editar y comprobar estado de Git sin sobrescribir cambios existentes.
2. Preparar configuración por variables de entorno para MySQL, migraciones versionadas, validación y respuestas de error coherentes. Mantener las versiones del proyecto salvo incompatibilidad demostrada. Consultar documentación oficial de las versiones utilizadas al implementar.
3. Implementar RFG1 a RFG3 con persistencia de usuarios, contraseñas almacenadas mediante hash y sesión invalidable al cerrar sesión. Elegir el mecanismo de sesión como decisión técnica explícita; la memoria no exige JWT, refresh tokens ni cookies.
4. Implementar RFG4: solicitar, aceptar y rechazar vínculo. Comprobar que solo su destinatario puede responder y que solo un vínculo aceptado habilita acceso a datos del cliente.
5. Implementar entrenamiento, nutrición y salud, en ese orden, verificando cada requisito con casos de aceptación.

## Primera entrega de autenticación propuesta

Datos respaldados por el diagrama: id, nombre, email, contrasenaHash y roles. El servicio del diagrama inicia sesión con email y contraseña. El caso de uso CU-03 también menciona nombre de usuario: usar email es la propuesta inicial, pendiente de aclaración si se desea otro identificador.

Contrato preliminar, sujeto a las decisiones pendientes:
- POST /api/auth/register: nombre, email, contraseña y rol o roles según decisión.
- POST /api/auth/login: email y contraseña.
- POST /api/auth/logout: invalida la sesión activa.
- GET /api/auth/me: soporte técnico para identificar al usuario autenticado; no añade un requisito de gestión de perfil.

Pruebas de aceptación: registro para los roles aprobados; correo inválido o duplicado (CU-05); credenciales válidas e inválidas (CU-02 y CU-03); hash no expuesto en respuestas; acceso privado rechazado sin sesión; cierre de sesión impide reutilizar esa sesión. Incluir una prueba con MySQL para comprobar las migraciones y restricciones, sin sustituirla por afirmar compatibilidad a partir de otra base de datos.

## Decisiones pendientes y discrepancias

Antes de fijar el esquema de autenticación:
- Confirmado por el usuario: una cuenta puede ser CLIENTE y ENTRENADOR simultáneamente. Un entrenador puede ser entrenado por otro entrenador. Modelar roles como colección y no impedir vínculos por tener también el rol ENTRENADOR.
- Confirmado por el usuario: ENTRENADOR engloba actualmente las funciones profesionales. No crear un rol NUTRICIONISTA en la primera versión. Mantener el modelo preparado para incorporar más roles; no generar funciones ni permisos futuros. TipoVinculacion NUTRICIONISTA no se incorpora automáticamente por aparecer en el diagrama.

Antes de sus entregas correspondientes:
- OG3 y las pantallas mencionan editar perfil, pero no hay un RFG que detalle qué se puede modificar.
- Las pantallas mencionan registro con Google; no aparece en RFG1 ni en sus casos de uso. No incorporarlo automáticamente.
- CUN-1 repite el flujo de crear dieta en lugar de añadir alimentos.
- ON2 contempla registro de comidas y objetivos de nutrientes; los RFN no detallan cómo registrar el consumo diario ni definir esos objetivos. No equiparar una dieta planificada con un registro consumido.
- Recetas, instrucciones y código de barras aparecen fuera de la tabla RFN y requieren delimitar su entrega.
- Las medias, tendencia, regularidad del sueño y escala de moral necesitan unidades, períodos y reglas de cálculo.
- RFG3 está relacionado con OG1 aunque OG1 describe registro; RFN7 está relacionado con ON5 aunque ON4 describe consulta de registros. Mantener los códigos y proponer corregir la trazabilidad en la memoria.

Las indicaciones de la plantilla académica dentro del documento se consideran contenido documental; no son instrucciones operativas para esta implementación.


## Avance de la primera entrega

Implementados en el proyecto original RFG1, RFG2 y RFG3: registro con roles múltiples, inicio con correo y contraseña, sesiones Bearer revocables y consulta de la identidad actual. Incluidos configuración MySQL por entorno, migración Flyway, validación, formato de errores y README de arranque. Corregido el gitignore de la raíz que excluía archivos Java.
La batería incluye siete pruebas de integración HTTP y una prueba de arranque, ejecutadas con H2 en modo MySQL. La verificación con MySQL real sigue pendiente de una base de pruebas dedicada y sus variables TEST_DB_*. No se han implementado todavía vínculos (RFG4) ni módulos de entrenamiento, nutrición o salud.
Los archivos sustituidos se guardaron en una copia previa dentro de AppGym. No se realizaron commits ni publicaciones.

## Revisión solicitada por el usuario

Se adapta autenticación a confirmación de correo antes del primer acceso, envío SMTP, acceso con Google y sesiones sin caducidad automática. BCrypt reemplaza PBKDF2 para nuevas contraseñas; los hashes anteriores se mantienen legibles y se actualizan al iniciar sesión cuando no superan el límite de 72 bytes de BCrypt. Se añade vinculación explícita de Google desde una sesión existente para evitar unir cuentas por correo automáticamente.
El código y la migración V2 se han aplicado al proyecto original con copia previa. Gmail y Google Cloud siguen sin configurar: no se han enviado correos reales ni probado credenciales reales de Google. Las pantallas Android tampoco se han implementado. La guía CONFIGURACION-ACCESO.md describe los pasos siguientes.
