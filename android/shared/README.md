# Código compartido de MyGymBro

Los módulos androidApp, iosApp, desktopApp y webApp arrancan la aplicación.
La interfaz y el acceso a los datos comunes se implementan aquí.

## Organización actual

- src/App.kt: raíz de Compose y punto de composición de las dependencias del ejemplo.
- src/presentation/home/HomeScreen.kt: pantalla que recibe los datos que muestra.
- src/domain/repository/GreetingRepository.kt: contrato para obtener el saludo.
- src/data/repository/PlatformGreetingRepository.kt: implementación local del contrato.
- src/Greeting.kt: utiliza el repositorio; permite recibir otra implementación por constructor.
- src/Platform.kt: contrato expect de plataforma.
- src@android, src@ios, src@jvm y src@wasmJs: implementaciones actual por plataforma.

Los paquetes Kotlin utilizan el prefijo com.example.mygymbroapp.
Los directorios presentation, domain y data están dentro de src, por lo que se compilan
para todas las plataformas configuradas en module.yaml.

## Flujo del ejemplo

App → Greeting → GreetingRepository → PlatformGreetingRepository → getPlatform()

App entrega el saludo a HomeScreen. HomeScreen controla únicamente la visibilidad
del contenido. El ejemplo todavía no necesita un ViewModel ni operaciones asíncronas.

## Siguiente paso: conexión con la API

La conexión de red está pendiente: no hay cliente HTTP, URL predeterminada,
endpoints ni credenciales configurados.

Al integrar el primer caso de uso real:
1. Confirmar el contrato de la API y la URL de desarrollo accesible desde cada destino.
2. Definir el modelo y el contrato del repositorio en domain.
3. Implementar el cliente remoto y sus DTO en data, con operaciones asíncronas.
4. Proporcionar la URL desde la configuración de arranque, sin duplicar los repositorios
   en cada aplicación y sin incrustar contraseñas o tokens en el código.
5. Conectar la pantalla mediante un ViewModel que exponga carga, resultado y error.

Cada entorno puede necesitar una dirección diferente para el mismo servidor:
localhost en un dispositivo apunta al propio dispositivo. El origen web
también debe estar permitido por la configuración CORS del servidor.

El almacenamiento seguro, los permisos y otras APIs nativas se implementarán en
src@<plataforma> cuando el caso de uso lo requiera.

## Verificación en Windows

Desde la raíz del proyecto:
- .\kotlin.bat build -m androidApp
- .\kotlin.bat build -m desktopApp
- .\kotlin.bat build -m webApp
- .\kotlin.bat test -m shared -p jvm

Consultar .\kotlin.bat build --help y .\kotlin.bat test --help para filtrar plataformas.
Los destinos iOS requieren macOS y Xcode.
