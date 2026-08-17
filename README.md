# Hydration-Reminder

Aplicación de escritorio en Java 25 que recuerda tomar agua durante el día,
registra el consumo y muestra progreso hacia una meta diaria configurable.

## Objetivo

Ayudar al usuario a mantener un hábito constante de hidratación mediante
recordatorios no intrusivos (bandeja del sistema), un registro simple de
consumo diario y visualización de progreso, sin depender de servicios en
la nube ni requerir conexión a internet para el uso básico.

## Stack

| Capa         | Tecnología                                                                                                                   |
| ------------ | ---------------------------------------------------------------------------------------------------------------------------- |
| Lenguaje     | Java 25 (LTS)                                                                                                                |
| UI           | JavaFX 24 (+ FXML para las vistas)                                                                                           |
| Bandeja/tray | Dorkbox SystemTray (cross-platform Win/Mac/Linux)                                                                            |
| Persistencia | SQLite embebido (`org.xerial:sqlite-jdbc`)                                                                                   |
| Concurrencia | Virtual threads (`Executors.newVirtualThreadPerTaskExecutor()`) para el scheduler y accesos a DB, sin bloquear el hilo de UI |
| Build        | Maven                                                                                                                        |
| Gráficos     | JavaFX `LineChart` para el historial de consumo                                                                              |
| Tipografía   | Fuentes variables embebidas: **Fraunces** (display) + **Manrope** (cuerpo), licencia OFL                                     |

## Estructura del proyecto

```
hydration-reminder/
├── pom.xml
├── README.md
└── src/main/
    ├── java/com/hydration/
    │   ├── HydrationApp.java          # Entry point, arranca JavaFX + tray + scheduler
    │   ├── model/
    │   │   ├── HydrationEntry.java    # Registro individual (timestamp, ml)
    │   │   └── UserSettings.java      # Meta diaria, intervalo, horario activo/silencioso
    │   ├── service/
    │   │   ├── DatabaseService.java   # CRUD sobre SQLite + cálculo de racha/historial
    │   │   ├── ReminderScheduler.java # Programación de recordatorios (virtual threads)
    │   │   └── NotificationService.java # Envío de notificaciones nativas
    │   ├── controller/
    │   │   ├── MainController.java    # Vista principal (progreso, registro rápido, racha)
    │   │   ├── HistoryController.java # Vista de historial (LineChart + estadísticas)
    │   │   └── SettingsController.java # Vista de configuración (metas, horarios)
    │   └── util/
    │       ├── TrayIconManager.java   # Configuración del ícono de bandeja y menú
    │       └── AppIcons.java          # Carga el set de íconos de ventana (todas las resoluciones)
    └── resources/
        ├── views/
        │   ├── main.fxml               # Vista principal
        │   ├── history.fxml            # Vista de historial
        │   └── settings.fxml           # Vista de configuración
        ├── styles/
        │   ├── main.css                 # Estilos generales ("Marea nocturna")
        │   └── history-chart.css        # Estilos específicos del LineChart
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
| `icons/app-icon.ico`                       | Multi-resolución (16/24/32/48/64/128/256) para empaquetar con `jpackage` en Windows. No se usa en tiempo de ejecución de JavaFX.                                                                                                                                                          |
| `icons/app-icon.icns`                      | Bundle de macOS (16 a 512@2x) para `jpackage` en macOS. Tampoco se usa en tiempo de ejecución.                                                                                                                                                                                            |

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
- Notificación nativa desde la bandeja del sistema al cumplirse el intervalo
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

> La suite usa **JUnit 5.10** y **Mockito 5.23.0**. Los tests de
> `NotificationService` disparan notificaciones nativas reales del SO durante la
> corrida (esto agrega ~8 s y abre notificaciones en pantalla); los demás tests
> son rápidos. Al final se imprime el resumen, por ejemplo:
> `Tests run: 30, Failures: 0, Errors: 0, Skipped: 0`.

## Empaquetar ejecutable (jpackage)

Para distribuir la app como ejecutable nativo de **Windows** (sin que el usuario
tenga Java instalado) se usa `jpackage`, la herramienta que viene incluida en el
JDK. No es multiplataforma: el comando debe correr en el mismo SO del destino,
así que hay que ejecutarlo en Windows para hacer los `.exe`/`.msi`.

### 1. Preparar los insumos (jar + dependencias runtime en una carpeta)

```bash
REM Compila y empaqueta: genera target\hydration-reminder-1.0.0.jar
mvnw.cmd clean package

REM Vuelca las dependencias runtime (incluido JavaFX y sus nativos) a una carpeta
mvnw.cmd -ntp dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target\jpackage-input

REM Copia el jar de la app junto a esas dependencias
copy /y target\hydration-reminder-1.0.0.jar target\jpackage-input\
```

> `target/jpackage-input` queda como el directorio `--input` que usa `jpackage`.
> Incluye los `.dll` de JavaFX para Windows (vienen dentro de los jars
> `javafx-graphics-win`, `javafx-controls-win`, etc.), así que el app corre sin
> que el usuario tenga que instalar nada.

### 2. App-image: carpeta autocontenida (recomendado para probar)

```bash
jpackage ^
  --type app-image ^
  --name Hydration Reminder ^
  --app-version 1.0.0 ^
  --input target\jpackage-input ^
  --main-jar hydration-reminder-1.0.0.jar ^
  --main-class com.hydration.HydrationApp ^
  --dest target\dist ^
  --icon src\main\resources\icons\app-icon.ico
```

Genera la carpeta autocontenida `target/dist/Hydration Reminder/` con su JRE
embebido: ejecutás `Hydration_Reminder.exe` y la app abre sin depender de un Java
instalado. Es la forma más simple de probar/distribuir sin instalar.

### 3. Instalador .exe / .msi (distribución final)

```bash
REM Instalador EXE de Windows (no requiere herramientas adicionales)
jpackage ^
  --type exe ^
  --name Hydration Reminder ^
  --app-version 1.0.0 ^
  --input target\jpackage-input ^
  --main-jar hydration-reminder-1.0.0.jar ^
  --main-class com.hydration.HydrationApp ^
  --dest target\dist ^
  --icon src\main\resources\icons\app-icon.ico ^
  --win-menu ^
  --win-shortcut

REM Instalador MSI (requiere el toolkit WiX 3.0+ instalado en el sistema)
jpackage ^
  --type msi ^
  --name Hydration Reminder ^
  --app-version 1.0.0 ^
  --input target\jpackage-input ^
  --main-jar hydration-reminder-1.0.0.jar ^
  --main-class com.hydration.HydrationApp ^
  --dest target\dist ^
  --icon src\main\resources\icons\app-icon.ico
```

> `exe` produce un instalador `.exe` de Windows sin herramientas extra.
> `msi` produce un `.msi` y necesita el toolkit
> [WiX](https://wixtoolset.org/) (solo para el paso de empaquetado del instalador).
> El ícono se toma de `src/main/resources/icons/app-icon.ico` (multi-resolución,
> el `.ico` se usa en tiempo de empaquetado; no en runtime). En macOS se usaría el
> `app-icon.icns` equivalente.

## Notas de implementación

- JavaFX no tiene soporte nativo de bandeja del sistema; por eso se usa
  `dorkbox.systemTray`, que abstrae las diferencias entre Windows, macOS y Linux
  mejor que `java.awt.SystemTray` puro.
- El `ReminderScheduler` corre en un `ScheduledExecutorService` respaldado por
  virtual threads, así los timers y el acceso a SQLite no compiten con el hilo
  de UI de JavaFX (`Platform.runLater` se usa solo para tocar nodos de la UI).
- Las fuentes Fraunces y Manrope son variables (un solo archivo `.ttf` cubre
  todos los pesos vía ejes OpenType). El soporte de interpolación de peso de
  fuentes variables depende del motor de texto de la plataforma; si el SO no
  lo soporta, JavaFX cae en la instancia por defecto del archivo (esto no
  rompe nada, solo puede notarse una variación de grosor menor a la esperada
  entre `-fx-font-weight` distintos). Las licencias OFL de ambas fuentes están
  incluidas en `resources/fonts/`.
