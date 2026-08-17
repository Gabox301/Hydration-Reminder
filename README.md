# Hydration-Reminder

Aplicación de escritorio en Java 25 que recuerda tomar agua durante el día,
registra el consumo y muestra progreso hacia una meta diaria configurable.

## Objetivo

Ayudar al usuario a mantener un hábito constante de hidratación mediante
recordatorios no intrusivos (bandeja del sistema), un registro simple de
consumo diario y visualización de progreso, sin depender de servicios en
la nube ni requerir conexión a internet para el uso básico.

## Stack

| Capa           | Tecnología                                                                                                                   |
| -------------- | ---------------------------------------------------------------------------------------------------------------------------- |
| Lenguaje       | Java 25 (LTS)                                                                                                                |
| UI             | JavaFX 24 (+ FXML para las vistas)                                                                                           |
| Notificaciones | Toasts propias de JavaFX (`ToastManager`) + sonido MP3 (`javafx-media`)                                                      |
| Bandeja/tray   | Dorkbox SystemTray (cross-platform Win/Mac/Linux)                                                                            |
| Persistencia   | SQLite embebido (`org.xerial:sqlite-jdbc`)                                                                                   |
| Concurrencia   | Virtual threads (`Executors.newVirtualThreadPerTaskExecutor()`) para el scheduler y accesos a DB, sin bloquear el hilo de UI |
| Build          | Maven (shade + launch4j para el EXE de Windows)                                                                              |
| Gráficos       | JavaFX `LineChart` para el historial de consumo                                                                              |
| Tipografía     | Fuentes variables embebidas: **Fraunces** (display) + **Manrope** (cuerpo), licencia OFL                                     |

## Estructura del proyecto

```
hydration-reminder/
├── pom.xml
├── README.md
└── src/main/
    ├── java/com/hydration/
    │   ├── HydrationApp.java          # Entry point, arranca JavaFX + tray + scheduler
    │   ├── Launcher.java              # Main del jar empaquetado (arranca HydrationApp)
    │   ├── model/
    │   │   ├── HydrationEntry.java    # Registro individual (timestamp, ml)
    │   │   └── UserSettings.java      # Meta diaria, intervalo, horario activo/silencioso
    │   ├── service/
    │   │   ├── DatabaseService.java   # CRUD sobre SQLite + cálculo de racha/historial
    │   │   ├── ReminderScheduler.java # Programación de recordatorios (virtual threads)
    │   │   └── NotificationService.java # Fachada: construye el contenido y delega en ToastManager
    │   ├── controller/
    │   │   ├── MainController.java    # Vista principal (progreso, registro rápido, racha)
    │   │   ├── HistoryController.java # Vista de historial (LineChart + estadísticas)
    │   │   └── SettingsController.java # Vista de configuración (metas, horarios)
    │   └── util/
    │       ├── TrayIconManager.java   # Configuración del ícono de bandeja y menú
    │       ├── AppIcons.java          # Carga el set de íconos de ventana (todas las resoluciones)
    │       ├── Fonts.java             # Carga las fuentes embebidas (Fraunces + Manrope)
    │       ├── ToastManager.java      # Toasts propias: ventana translúcida, apilado, acciones
    │       └── SoundPlayer.java       # Reproduce el sonido de notificación (MP3)
    └── resources/
        ├── views/
        │   ├── main.fxml               # Vista principal
        │   ├── history.fxml            # Vista de historial
        │   └── settings.fxml           # Vista de configuración
        ├── styles/
        │   ├── main.css                 # Estilos generales ("Marea nocturna")
        │   ├── history-chart.css        # Estilos específicos del LineChart
        │   └── toast.css                # Estilos de las toasts
        ├── sounds/
        │   └── notification.mp3         # Sonido que acompaña cada toast
        ├── fonts/                        # Fraunces y Manrope (variable, embebidas)
        └── icons/                        # Logo/ícono de la app (ver sección Logo e íconos)
```

## Logo e íconos

El logo es una gota de agua con degradé cian (paleta "Marea nocturna"),
generada vectorialmente por geometría (círculo + tangentes) para que el
contorno quede perfectamente suave en cualquier tamaño.

| Archivo                                    | Uso                                                                                                                                                                                                                                                                                       |
| ------------------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `icons/tray-icon.png`                      | Ícono de la bandeja del sistema (gota sola, fondo transparente). Es el que usa `TrayIconManager` — **no renombrar ni mover**, el código lo referencia por ese path exacto.                                                                                                                |
| `icons/tray-icon@32.png`, `@16.png`        | Variantes más chicas del mismo ícono, por si en algún SO hace falta forzar un tamaño específico en vez de dejar que Dorkbox escale `tray-icon.png`.                                                                                                                                       |
| `icons/app-icon-{16..512}.png`             | Gota sobre tile redondeado con resplandor, en 8 resoluciones. `AppIcons.loadAll()` las carga todas y se las pasa a cada `Stage.getIcons()` (ventana principal, Historial, Configuración); JavaFX elige la resolución más apropiada según el contexto (barra de título, taskbar, alt-tab). |
| `icons/app-icon-1024.png` / `app-icon.png` | Versión master en alta resolución, por si hace falta regenerar algo o usarla en un futuro splash screen.                                                                                                                                                                                  |
| `icons/app-icon.ico`                       | Multi-resolución (16/24/32/48/64/128/256). Lo usa launch4j para ponerle ícono al EXE de Windows. No se usa en tiempo de ejecución de JavaFX.                                                                                                                                              |
| `icons/app-icon.icns`                      | Bundle de macOS (16 a 512@2x) por si en el futuro se empaqueta con `jpackage` en macOS. Tampoco se usa en tiempo de ejecución.                                                                                                                                                            |

## Dirección visual: "Marea nocturna"

Tema oscuro tinta-petróleo con acentos cian bioluminiscentes y toques coral
para la racha. Tipografía **Fraunces** (serif con curvas orgánicas, evoca una
gota de agua) para títulos y cifras grandes, y **Manrope** (geométrica,
moderna) para el resto del texto — ninguna de las dos es una fuente de
sistema genérica (nada de Arial/Roboto/Inter). Tarjetas con efecto de vidrio
esmerilado (`glass-card`), degradés sutiles y sombras con resplandor cian en
los elementos interactivos para reforzar la sensación de "app moderna",
evitando el aspecto por defecto de JavaFX.

## Funcionalidades core (MVP)

- Recordatorios periódicos configurables (cada X minutos, o rango horario ej. 9–18hs)
- **Toast propia de JavaFX** al cumplirse el intervalo: ventana translúcida sin decorar
  ("Marea nocturna"), esquinas redondeadas, sonido MP3, apilado vertical, clic en el cuerpo
  para abrir la app y una fila de botones de registro rápido (**200/300/500 ml**)
- Registro de consumo por vaso (cantidades predefinidas: 200/300/500 ml)
- Meta diaria configurable (por defecto 2000 ml) con barra de progreso
- Persistencia local en SQLite: historial de consumo por día
- Modo silencioso configurable (no notificar en un rango horario, ej. mientras dormís)
- Icono en bandeja del sistema con menú rápido (registrar vaso, abrir app, pausar recordatorios, salir)
- **Ventana de configuración**: editar meta diaria, intervalo de recordatorio, horario activo y modo silencioso, con guardado que reinicia el scheduler al instante
- **Ventana de historial**: `LineChart` de los últimos 14 días (consumo vs. meta), más promedio, mejor día y racha actual
- **Racha (streak)**: días consecutivos cumpliendo la meta, visible como badge en la vista principal

## Features a futuro (para elevar la calidad)

- **Detección de inactividad**: no recordar si el usuario no está frente a la compu (evita notificaciones inútiles)
- **Widget flotante mini**: ventana sin decoración, semitransparente, siempre visible con el progreso del día
- **Ajuste dinámico de meta**: integrar una API de clima y sugerir aumentar la meta en días de calor
- **Exportación de historial** a CSV/PDF
- **Perfiles múltiples**: útil si varias personas comparten la misma compu
- **Sonidos personalizables** para las notificaciones
- **Atajo global de teclado** para registrar un vaso sin abrir la ventana principal
- **Sincronización opcional** (Google Drive / archivo compartido) para tener el historial en más de un dispositivo
- **Cantidad libre de ml** además de los tres botones predefinidos

## Cómo correr

El proyecto incluye el **Maven Wrapper** (modo `only-script`), así que no hace falta
instalar Maven: el wrapper lo descarga automáticamente la primera vez.

```bash
# Windows
.\mvnw.cmd clean javafx:run

# macOS / Linux
./mvnw clean javafx:run
```

Si ya tenés Maven instalado y querés usar el binario del sistema, seguís pudiendo
usar `mvn` directamente:

```bash
mvn clean javafx:run
```

También podés compilar (sin abrir la ventana) con `mvnw.cmd clean compile`, y
empaquetar el jar con `mvnw.cmd clean package`.

### Ejecutar los tests

```bash
# Windows
.\mvnw.cmd clean test

# macOS / Linux
./mvnw clean test
```

> La suite usa **JUnit 5.10** y **Mockito 5.23.0**. `NotificationServiceTest`
> verifica los toasts con un `ToastManager` mockeado (no abre ventanas ni emite
> sonido); el resto cubre scheduler, DB, íconos y tray. Al final se imprime el
> resumen, por ejemplo: `Tests run: 29, Failures: 0, Errors: 0, Skipped: 0`.
> La cobertura se valida con **JaCoCo** (mínimo 80 % en `com.hydration.model.*`
> y `com.hydration.service.*`) durante `verify`.

## Empaquetar ejecutable (EXE de Windows)

El build de Maven genera un ejecutable nativo de Windows con **launch4j**:
el plugin **shade** arma un jar con todas las dependencias (JavaFX, sus DLL
nativas y los recursos) y launch4j lo envuelve en un `Hydration_Reminder.exe`.

```bash
# Compila, corre los tests y genera el EXE
.\mvnw.cmd clean package
```

Resultados dentro de `target\`:

| Archivo                            | Descripción                                     |
| ---------------------------------- | ----------------------------------------------- |
| `hydration-reminder-1.0.0.jar`     | Jar de la app (sin dependencias)                |
| `hydration-reminder-1.0.0-all.jar` | Jar sombreado: app + JavaFX + SQLite + recursos |
| `dist\Hydration_Reminder.exe`      | Ejecutable de Windows (launch4j)                |

> El EXE se lanza con el JRE del sistema: el pom exige **Java 25+ instalado**
> en la máquina destino (`<jre><minVersion>25.0.0</minVersion>`). Si en el
> futuro querés una carpeta autocontenida con JRE embebido (sin depender de un
> Java instalado), `jpackage` sobre el jar sombreado sigue siendo la vía.

## Notas de implementación

- Las notificaciones son **toasts propias de JavaFX** (`ToastManager`), no
  notificaciones del SO: una ventana sin decorar y translúcida con la estética
  "Marea nocturna". El dropshadow de la tarjeta se mantiene contenido dentro de
  la ventana (`SHADOW_PADDING`): con el pipeline de software de Prism, un blur
  que excede los bordes de una ventana translúcida se renderiza como fondo
  blanco. Las toasts se apilan usando la altura real de cada una (la más nueva
  abajo) y se cierran a los 8 s (pausa al pasar el mouse). JavaFX no tiene
  soporte nativo de bandeja del sistema; por eso el menú del tray usa
  `dorkbox.systemTray`.
- El sonido de notificación se reproduce con `javafx-media` (`MediaPlayer`).
  El MP3 vive como recurso del classpath y se extrae a un archivo temporal
  antes de reproducirlo, porque `Media` no admite URIs `jar:` (contenido
  dentro del jar sombreado). Si el audio falla, la toast se muestra igual.
- El `ReminderScheduler` corre en un `ScheduledExecutorService` respaldado por
  virtual threads, así los timers y el acceso a SQLite no compiten con el hilo
  de UI de JavaFX (`Platform.runLater` se usa solo para tocar nodos de la UI).
- El pom pasa flags de JVM tanto en `javafx:run` como en el EXE de launch4j:
  `--enable-native-access=javafx.graphics`, `--enable-native-access=javafx.media`,
  `--enable-native-access=ALL-UNNAMED` y `--sun-misc-unsafe-memory-access=allow`.
  Silencian los warnings de native access de JDK 24+ (el módulo de media expone
  natives propios, por eso `javafx.media` está listado explícitamente).
- Las fuentes Fraunces y Manrope son variables (un solo archivo `.ttf` cubre
  todos los pesos vía ejes OpenType). El soporte de interpolación de peso de
  fuentes variables depende del motor de texto de la plataforma; si el SO no
  lo soporta, JavaFX cae en la instancia por defecto del archivo (esto no
  rompe nada, solo puede notarse una variación de grosor menor a la esperada
  entre `-fx-font-weight` distintos). Las licencias OFL de ambas fuentes están
  incluidas en `resources/fonts/`.
