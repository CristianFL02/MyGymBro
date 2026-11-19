# API de entrenamiento de MyGymBro

Actualizado: **18 de noviembre de 2026**. Android Compose todavía no está implementado. Este documento describe únicamente la API.

## Mapa rápido: dónde está cada función

Los controllers reciben la petición HTTP; los servicios comprueban y ejecutan la operación;
los repositorios consultan o guardan entidades. Una entidad puede tener métodos internos
sin que exista todavía un endpoint para esa funcionalidad.

| Acción del usuario | Endpoint implementado | Método del controller | Método del servicio |
| --- | --- | --- | --- |
| Ver nombres de mis rutinas | `GET /api/rutina` | `RutinaController.listar` | `RutinaService.listarRutinas` |
| Crear rutina con nombre | `POST /api/rutina/create` | `RutinaController.crearRutina` | `RutinaService.crearRutina` |
| Abrir una rutina y sus días | `GET /api/rutina/{id}` | `RutinaController.mostrarRutinaEntera` | `RutinaService.listarRutinaEntera` |
| Cambiar solo su nombre | `PATCH /api/rutina/{id}` | `RutinaController.editarNombre` | `RutinaService.editarNombre` |
| Guardar días, ejercicios, objetivos y alternativas | `PUT /api/rutina/{id}` | `RutinaController.guardarCompleta` | `RutinaService.guardarCompleta` |
| Duplicar rutina | `POST /api/rutina/{id}/duplicar` | `RutinaController.duplicar` | `RutinaService.duplicarRutina` |
| Eliminar rutina | `DELETE /api/rutina/{id}` | `RutinaController.eliminar` | `RutinaService.eliminarRutina` |
| Buscar ejercicios para elegir | `GET /api/ejercicios` | `EjercicioController.listar` | `AccesoEjercicio.listar` |
| Empezar a entrenar un día | `POST /api/sesiones` | `SesionController.iniciar` | `SesionService.iniciar` |
| Ver entrenamientos pendientes | `GET /api/sesiones/en-curso` | `SesionController.enCurso` | `SesionService.enCurso` |
| Abrir un entrenamiento guardado | `GET /api/sesiones/{id}` | `SesionController.consultar` | `SesionService.consultar` |
| Guardar progreso o terminar entrenamiento | `PUT /api/sesiones/{id}` | `SesionController.guardar` | `SesionService.guardar` |

**Dos guardados diferentes:** el de rutina cambia la planificación; el de sesión cambia
únicamente ese entrenamiento. En sesiones, `finalizar: false` guarda un borrador y
`finalizar: true` solicita su finalización. No hay un endpoint separado para finalizar.

**Todavía sin endpoint:** `HistorialService.obtenerPorSemanas` consulta el historial interno y
`HistorialService.compararEjercicio` calcula diferencias. No se pueden llamar por HTTP hoy.
Tampoco hay aún operaciones HTTP de asignaciones ni correcciones del entrenador.

### Por dónde leer el código

1. Empezar por el método del controller de la acción que interesa.
2. Seguir la llamada al servicio; su Javadoc describe comprobaciones y cambios.
3. Consultar el repositorio si se quiere entender la búsqueda en la base de datos.
4. Consultar el DTO de entrada y salida para ver el JSON esperado.

Los Javadoc están en los paquetes `entrenamiento.controller`, `entrenamiento.service` y
`entrenamiento.repository`. En el IDE se pueden consultar desde la declaración o mediante
la documentación rápida del método. No se ha incorporado Swagger/OpenAPI ni se han añadido rutas nuevas.

También se ha generado el [índice HTML de Javadoc](target/javadoc-entrenamiento/index.html)
para controllers, servicios y repositorios. Es documentación local, no un endpoint de la API.
Se guarda en `target`, por lo que habrá que regenerarla si se limpia esa carpeta;
los comentarios Javadoc permanecen en los archivos Java.

## 1. Comportamiento acordado

- La lista muestra el nombre de las rutinas; devuelve también su ID para abrirlas.
- Una rutina contiene días de entrenamiento, sin necesidad de asociarlos a lunes, martes, etc.
- Cada día contiene ejercicios con series efectivas objetivo, rango de repeticiones, descanso previsto y esfuerzo RIR o RPE.
- Cada entrada del día puede tener una lista ordenada de ejercicios alternativos propuestos. No obliga a elegir exclusivamente entre esas opciones.
- Las aproximaciones se planifican por ejercicio del día y no cuentan como series efectivas.
- El ejercicio del catálogo tiene una técnica original; la entrada de la rutina puede tener una técnica personalizada, sin sobrescribir la original.
- Una sesión iniciada copia las indicaciones del día. Permite cambiar ejercicios, objetivos y aproximaciones sin modificar la rutina.
- Se puede guardar una sesión sin terminar, recuperarla y finalizarla después.
- Cada serie realizada indica tipo (`APROXIMACION` o `EFECTIVA`), resultados y RIR o RPE opcional.
- Cada ejercicio realizado admite valoraciones y comentarios opcionales.
- La animación del ejercicio queda para el futuro; no se ha añadido almacenamiento de archivos ni un campo de imagen provisional.

### Valoraciones por ejercicio realizado

| Campo | Significado de 1 a 10 |
| --- | --- |
| `comodidad` | Muy incómodo → muy cómodo |
| `capacidadFuerza` | Muy limitada → excelente |
| `bombeo` | Ninguno → máximo |
| `molestias` | Ninguna → máximas |
| `calidadTecnica` | Muy mala → excelente |
| `comentariosUsuario` | Texto opcional, hasta 2000 caracteres |

Las puntuaciones admiten `null` o enteros de 1 a 10. No rellenar un campo no equivale a puntuar cero. Son valoraciones subjetivas, no un diagnóstico automático de eficacia.

### Permisos: actual y pendiente

**Implementado:** los endpoints de rutinas y sesiones comprueban el propietario. Un entrenador no puede editar una rutina propia de otro usuario. El catálogo expone ejercicios públicos y privados del usuario autenticado; las escrituras también validan ese acceso.

**Acordado, pendiente del flujo de asignaciones:** el cliente podrá iniciar y adaptar una sesión de una rutina asignada, pero no editar la rutina del entrenador. Hoy solo se pueden iniciar sesiones desde rutinas propias; no se presenta la asignación como implementada.

**Sin aprobar:** compartir voluntariamente rutinas propias con entrenadores mediante permisos de lectura. No se concede ese acceso automáticamente por estar vinculados.

## 2. Datos y relaciones

```text
Rutina → DiaRutina → EjercicioRutina → Ejercicio del catálogo
                         └─ Aproximaciones planificadas

SesionEntrenamiento → RegistroEjercicio → SerieRegistrada
                         ├─ Copia de las indicaciones y aproximaciones
                         └─ Valoraciones y comentarios del usuario
```

- `EjercicioRutina.id` identifica la aparición concreta en un día; `ejercicioId` identifica la definición del catálogo.
- `RegistroEjercicio` identifica el ejercicio realizado dentro de una sesión. Su ejercicio puede sustituirse durante esa sesión.
- Las series objetivo cuentan solo el trabajo efectivo. Las aproximaciones realizadas se guardan como series diferenciadas.
- `PlanRegistro` conserva series, rango, descanso, esfuerzo y textos técnicos al iniciar la sesión. No depende de futuras ediciones de la rutina.
- `diaRutinaOrigenId` y `nombreDiaOrigen` conservan el origen como referencia histórica, sin una FK que impida quitar ese día de la planificación.
- `AsignacionRutina` existe como entidad; todavía no hay un flujo HTTP de asignación.

## 3. Endpoints implementados

Todos requieren `Authorization: Bearer <token_de_sesion>`. Las entradas JSON requieren `Content-Type: application/json`.

| Método y ruta | Función | Éxito |
| --- | --- | --- |
| `POST /api/rutina/create` | Crear la cabecera con nombre | 201 |
| `GET /api/rutina` | Listar rutinas propias activas: ID y nombre | 200 |
| `GET /api/rutina/{id}` | Consultar días, ejercicios, objetivos, técnicas y aproximaciones | 200 |
| `PATCH /api/rutina/{id}` | Cambiar solo el nombre | 200 |
| `PUT /api/rutina/{id}` | Guardar el contenido completo de una rutina propia | 200 |
| `POST /api/rutina/{id}/duplicar` | Copiar días, entradas, objetivos, técnicas y aproximaciones | 201 |
| `DELETE /api/rutina/{id}` | Eliminación lógica | 204, sin cuerpo |
| `GET /api/ejercicios` | Listar ejercicios accesibles con técnica original y tipo de medición | 200 |
| `POST /api/sesiones` | Iniciar un borrador copiando un día de una rutina propia | 201 |
| `GET /api/sesiones/en-curso` | Recuperar sesiones propias sin finalizar | 200 |
| `GET /api/sesiones/{id}` | Consultar el detalle de una sesión propia | 200 |
| `PUT /api/sesiones/{id}` | Guardar un borrador completo o finalizarlo | 200 |

El catálogo no incluye todavía endpoints de creación de ejercicios ni datos iniciales precargados. Si la base no contiene ejercicios accesibles, devuelve `[]`.

### Crear o renombrar

`POST /api/rutina/create` y `PATCH /api/rutina/{id}` reciben:

```json
{ "nombre": "Torso/Pierna" }
```

Devuelven `RutinaDTO`: `nombre` e `id`. El nombre es obligatorio, de hasta 30 caracteres. Crear la cabecera no crea días automáticamente.

### Guardar una rutina completa

Obtener primero el detalle y su `version`. Ejemplo de `PUT /api/rutina/5`:

```json
{
  "version": 0,
  "nombre": "Torso/Pierna",
  "dias": [
    {
      "id": null,
      "nombre": "Torso A",
      "ejercicios": [
        {
          "id": null,
          "datos": {
            "ejercicioId": 7,
            "seriesObjetivo": 3,
            "repeticionesMin": 8,
            "repeticionesMax": 12,
            "descansoSeg": 120,
            "esfuerzoObjetivo": { "rir": 2, "rpe": null },
            "tecnicaPersonalizada": "Bajada controlada",
            "aproximaciones": [
              { "repeticiones": 10, "carga": 20, "descansoSeg": 60 }
            ]
          }
        }
      ]
    }
  ]
}
```

Reglas del reemplazo completo:

- ID `null`: crear. ID existente: actualizar, comprobando pertenencia a rutina y día.
- Días y entradas existentes omitidos: eliminar de la planificación.
- El orden de las listas determina el orden de días y ejercicios. Se conservan los IDs al reordenar.
- Las listas pueden estar vacías; no pueden ser `null`. Se admiten hasta 100 días, 100 ejercicios por día y 100 aproximaciones por ejercicio en una petición.
- El nombre del día admite hasta 15 caracteres. Las técnicas admiten hasta 2000.
- Las aproximaciones tienen repeticiones positivas; carga y descanso pueden ser `null` si no se prescriben. La carga no se calcula automáticamente.
- El rango cumple `0 <= mínimo <= máximo`; en `REPS_CARGA`, el mínimo es positivo. Las series objetivo son positivas.
- `descansoSeg` es opcional y no negativo.
- `esfuerzoObjetivo` es opcional. Se informa RIR entero no negativo **o** RPE numérico entre 0 y 10; no ambos. RPE admite decimales. El cero de RPE sigue admitiéndose por compatibilidad con el modelo anterior.
- Devuelve `RutinaDetalleDTO` con la nueva `version`, IDs definitivos y detalles de los días. Cada entrada incluye técnica original, personalizada y aproximaciones.
- La operación es transaccional. Una versión antigua se rechaza; los errores no deben guardar cambios parciales.

### Iniciar y recuperar un entrenamiento

`POST /api/sesiones`:

```json
{ "rutinaId": 5, "diaId": 12 }
```

Crea una sesión con `finalizada: false`, copia los objetivos, técnicas y aproximaciones y devuelve su detalle con ID y versión. Las series realizadas empiezan vacías: no se inventan resultados por copiar la planificación.

`GET /api/sesiones/en-curso` recupera los borradores del usuario. `GET /api/sesiones/{id}` devuelve ejercicios, plan, aproximaciones, valoraciones y series, tanto en curso como finalizadas.

### Guardar el entrenamiento sin terminar o finalizarlo

Ejemplo de `PUT /api/sesiones/21`:

```json
{
  "version": 0,
  "duracionMin": 20,
  "finalizar": false,
  "ejercicios": [
    {
      "id": 41,
      "ejercicioId": 9,
      "plan": {
        "seriesObjetivo": 3,
        "repeticionesMin": 8,
        "repeticionesMax": 12,
        "descansoSeg": 90,
        "esfuerzoObjetivo": { "rir": null, "rpe": 8.5 },
        "tecnicaOriginal": "Indicaciones copiadas al iniciar",
        "tecnicaPersonalizada": "Adaptación para esta sesión"
      },
      "aproximaciones": [{ "repeticiones": 10, "carga": 10, "descansoSeg": 45 }],
      "valoracion": {
        "comodidad": 8,
        "capacidadFuerza": 9,
        "bombeo": 7,
        "molestias": 1,
        "calidadTecnica": 8,
        "comentariosUsuario": "Máquina ocupada: uso mancuernas"
      },
      "series": [
        {
          "tipo": "APROXIMACION",
          "repeticiones": 10,
          "carga": 10,
          "tiempoSeg": 0,
          "distanciaM": 0,
          "esfuerzo": null,
          "descansoSeg": 45,
          "notas": null
        },
        {
          "tipo": "EFECTIVA",
          "repeticiones": 8,
          "carga": 25,
          "tiempoSeg": 0,
          "distanciaM": 0,
          "esfuerzo": { "rir": 2, "rpe": null },
          "descansoSeg": 90,
          "notas": null
        }
      ]
    }
  ]
}
```

- Es un guardado completo: IDs `null` crean registros, IDs propios actualizan y registros omitidos se eliminan de esa sesión. No se admiten IDs ajenos o duplicados.
- Se pueden sustituir ejercicios, añadir o quitar registros y editar objetivos, aproximaciones y series de la sesión sin cambiar la rutina.
- La posición de cada serie determina su número; no se recibe un número arbitrario. Un registro admite hasta 200 series y una sesión hasta 100 ejercicios por petición.
- `plan`, `valoracion` y los esfuerzos pueden ser `null`. Las listas deben enviarse, aunque estén vacías. Las puntuaciones internas también pueden ser `null`.
- Una sesión en curso admite registros sin series y una lista de ejercicios vacía. Las series que se envíen deben tener mediciones válidas.
- `REPS_CARGA` exige repeticiones positivas; `TIEMPO`, tiempo positivo; `DISTANCIA_TIEMPO`, distancia y tiempo positivos. Carga, distancia y tiempos no admiten negativos; los números decimales deben ser finitos.
- `finalizar: true` exige al menos un ejercicio y al menos una serie realizada en cada ejercicio. No exige completar todas las series prescritas.
- Una sesión finalizada se puede consultar, pero no editar mediante este endpoint. La futura corrección autorizada del entrenador será otro flujo.
- Hay que enviar la última `version` recibida. Un conflicto exige volver a cargar el recurso antes de decidir qué cambios conservar.
- Las unidades de carga y la política de fechas deben concretarse antes de integrar clientes; la API no convierte unidades implícitamente. Distancia se almacena en metros; tiempos y descansos en segundos.

### Ejercicios alternativos

En `datos` de cada entrada enviada a `PUT /api/rutina/{id}` se puede incluir:

```json
{
  "ejercicioId": 7,
  "seriesObjetivo": 3,
  "repeticionesMin": 8,
  "repeticionesMax": 12,
  "descansoSeg": 120,
  "esfuerzoObjetivo": null,
  "tecnicaPersonalizada": null,
  "aproximaciones": [],
  "alternativasIds": [9, 12]
}
```

- Se admiten hasta 20 alternativas por entrada, sin duplicados ni el propio ejercicio principal. Todos los IDs deben existir y ser accesibles al usuario.
- Las alternativas son referencias a ejercicios del catálogo, sin duplicar sus definiciones. Se guardan en `alternativas_rutina` mediante una relación `@ManyToMany` ordenada, sin cascada de borrado sobre los ejercicios.
- El detalle de la rutina devuelve `alternativas`, una lista de objetos con `ejercicioId`, `nombre` y `tipoMedicion`, en el orden prescrito.
- Como el guardado es completo, enviar `[]`, `null` u omitir `alternativasIds` elimina las propuestas de esa entrada. El contrato anterior sin ese campo sigue siendo válido.
- Al duplicar una rutina se copia la lista. Al iniciar una sesión se conserva una lista independiente en `alternativas_sesion` y una referencia `ejercicioPropuesto` al principal original.
- Cambiar posteriormente las propuestas de la rutina no modifica las de una sesión ya iniciada. Se conserva la composición de la lista; nombres y tipo siguen siendo los del catálogo, no una copia histórica de sus metadatos.
- Para elegir una alternativa se utiliza el mismo `PUT /api/sesiones/{id}`, manteniendo el ID del registro y cambiando `ejercicioId`. También se puede escoger otro ejercicio accesible del catálogo, aunque no esté propuesto. Los objetivos y resultados se envían explícitamente; no se convierte una medición a otra automáticamente.
- `ejercicioPropuesto` permite identificar el principal inicial aunque se haya sustituido. Es `null` en registros previos a V7 o añadidos manualmente a la sesión, porque no se inventa una propuesta histórica.
- Las lecturas omiten propuestas cuyo ejercicio ya no sea accesible al usuario. Las escrituras de la rutina rechazan ejercicios privados ajenos e inexistentes con 404; el principal repetido o las alternativas duplicadas se rechazan con 400.

No hace falta un endpoint adicional: las propuestas se guardan con la rutina y se consultan en el detalle de rutina o sesión.

## 4. Errores y seguridad

| Estado | Caso |
| --- | --- |
| 400 | Datos inválidos, escala fuera de rango, RIR y RPE simultáneos, JSON no válido o IDs de hijos incompatibles. |
| 401 | Sesión de autenticación ausente o inválida. |
| 404 | Recurso inexistente o no accesible al usuario, incluidas rutinas eliminadas. |
| 409 | Versión antigua, modificación de una sesión finalizada o conflicto de integridad/estado. |

Formato habitual:

```json
{ "status": 404, "mensaje": "La sesión no existe", "campos": {} }
```

Nunca se elige el propietario mediante un `usuarioId` del cuerpo. Se toma de la autenticación. Los hijos se comprueban contra el recurso padre. Un vínculo de entrenador no habilita estos endpoints sobre cuentas ajenas.

## 5. Persistencia y pruebas

- `V4`: tablas iniciales de entrenamiento.
- `V5`: conversión del objetivo fijo al rango mínimo/máximo.
- `V6`: técnica original y personalizada, aproximaciones, esfuerzo alternativo, valoraciones, orden y versiones de rutinas y sesiones.
- `V7`: alternativas propuestas de rutina y sesión, y referencia al ejercicio principal propuesto al iniciar una sesión. Los datos previos mantienen sus listas vacías.
- Los registros previos de series se mantienen como `EFECTIVA` y conservan su RPE. Los datos nuevos opcionales quedan sin rellenar; no se inventan puntuaciones históricas.
- Las migraciones se mantienen en `db/migration` y `db/postgresql`. No se han aplicado a datos reales durante este trabajo.
- Las comparaciones internas excluyen series de aproximación. No hay todavía endpoint de comparación.

Pruebas: ejecutar desde `backend/mygymbro-api` con Java 25:

```powershell
.\mvnw.cmd test
```

Las pruebas de integración usan MockMvc y H2. Deben verificar guardado/lectura, modificaciones del borrador sin alterar la rutina, tipos de serie, RIR/RPE, puntuaciones opcionales, permisos, versiones y conservación de datos al migrar. No equivalen a probar el despliegue con PostgreSQL o MySQL reales.

Última verificación de esta implementación: **75 pruebas, 0 fallos, 0 errores y 0 omitidas**, con Maven en modo sin conexión usando la caché local. Incluye una petición inválida ejecutada sin transacción envolvente de prueba para comprobar que se revierten realmente nombre, días y versión en la base temporal. También se comprueban guardado, reordenación, duplicación, copia y elección de alternativas, acceso a ejercicios privados propios y rechazo de propuestas duplicadas, iguales al principal, privadas ajenas o inexistentes. La migración conserva los registros anteriores sin inventar propuestas.

## 6. Qué queda por hacer en la API

| Bloque | Pendiente concreto |
| --- | --- |
| Catálogo | Carga de datos iniciales; concretar si se necesita gestión HTTP de ejercicios personalizados y consulta individual. |
| Asignaciones (RFE9) | Flujo del entrenador, propiedad y permisos, vínculo aceptado, consulta del cliente e inicio de sesiones desde rutinas asignadas. |
| Correcciones (RFE6) | Campos corregibles, DTO, endpoint, autorización del entrenador vinculado y pruebas. |
| Historial (RFE7) | Endpoint, DTO de resumen, contrato de período y límites. Existe consulta interna por semanas de sesiones finalizadas. |
| Comparaciones (RFE8) | Endpoint, DTO sin entidades JPA, acceso a ambas sesiones y métricas para tiempo/distancia. Existe cálculo interno orientado a carga y repeticiones. |
| Unidades y fechas | Cerrar unidades de carga y política temporal para los contratos de registro. |
| Verificación | Migraciones y flujos en el motor real del despliegue; revisar límites del catálogo y de la recuperación de borradores al crecer los datos. |
| Animación | Futuro: decidir formato, almacenamiento, permisos y contrato. |
| Lectura por entrenador | Opcional pendiente de aprobación; no concede edición sobre rutinas propias del cliente. |

RFE1 y RFE4 disponen de endpoints. RFE2 y RFE3 cuentan con guardado de contenido propio; RFE5 cuenta con borradores y finalización desde días propios. Esto no declara terminados RFE6–RFE9 ni la parte de asignaciones.

## 7. Organización del código

- `controller`: HTTP y usuario autenticado.
- `service`: autorización, guardado transaccional y conversión de respuestas.
- `repository`: consultas de rutinas, ejercicios y sesiones.
- `dto/EntrenamientoDtos.java`: contratos de esfuerzos, aproximaciones, valoraciones y sesiones, y entradas del guardado completo.
- Entidades: planificación, registro y reglas de dominio.

Para defender el diseño: **la rutina prescribe; la sesión copia esa prescripción y registra lo realizado**. Por eso una sustitución durante el entrenamiento no debe editar la rutina del entrenador. Los DTO evitan enviar relaciones JPA y la versión evita sobrescribir silenciosamente cambios de otra edición.
