# Kinetic WOL

Kinetic WOL es una app Android nativa para guardar equipos de red y enviarles paquetes Wake-on-LAN desde el teléfono. El proyecto también prepara una vía de invocación por voz basada en App Actions, con foco en compatibilidad práctica con Gemini dentro de la infraestructura oficial actual de Android.

## Estado actual

- Base Android migrada por completo desde el prototipo web original
- UI principal en Jetpack Compose
- Persistencia local real con Room
- Envío WOL manual funcional
- Fulfillment headless funcional mediante `WakeDeviceActivity`
- Shortcuts dinámicos publicados para equipos guardados
- Recursos bilingües en español e inglés

Validaciones ya hechas sobre la app:

- creación, edición y borrado de dispositivos
- envío de magic packets contra un listener UDP local
- invocación headless por `adb` resolviendo dispositivos por nombre
- aparición de shortcuts dinámicos al mantener pulsado el icono de la app

## Stack técnico

- Kotlin
- Jetpack Compose
- Material 3
- Room
- Android SDK 36
- `minSdk = 36`
- Java 17

## Arquitectura

```text
app/src/main/java/dev/neikon/kineticwol/
├── actions/           # fulfillment headless y shortcuts dinámicos
├── data/              # Room y repositorio
├── domain/            # modelo y lógica WOL
├── ui/                # Compose, estado y tema
└── util/              # utilidades compartidas
```

Piezas clave:

- [MainActivity.kt](/var/home/neikon/repos/kinetic_wol/app/src/main/java/dev/neikon/kineticwol/MainActivity.kt): entrada principal de la app
- [KineticWolApp.kt](/var/home/neikon/repos/kinetic_wol/app/src/main/java/dev/neikon/kineticwol/ui/KineticWolApp.kt): dashboard, formulario y navegación principal
- [HomeViewModel.kt](/var/home/neikon/repos/kinetic_wol/app/src/main/java/dev/neikon/kineticwol/ui/home/HomeViewModel.kt): estado, validación y acciones de la pantalla
- [WakeOnLanSender.kt](/var/home/neikon/repos/kinetic_wol/app/src/main/java/dev/neikon/kineticwol/domain/wol/WakeOnLanSender.kt): normalización MAC y envío del magic packet
- [WakeDeviceActivity.kt](/var/home/neikon/repos/kinetic_wol/app/src/main/java/dev/neikon/kineticwol/actions/WakeDeviceActivity.kt): ejecución headless por intent
- [DeviceShortcutPublisher.kt](/var/home/neikon/repos/kinetic_wol/app/src/main/java/dev/neikon/kineticwol/actions/DeviceShortcutPublisher.kt): shortcuts dinámicos por dispositivo
- [shortcuts.xml](/var/home/neikon/repos/kinetic_wol/app/src/main/res/xml/shortcuts.xml): capability y query patterns de App Actions

## Requisitos de entorno

Para abrir y compilar el proyecto:

- Android Studio reciente
- Android SDK 36
- JDK 17 o el `Embedded JDK` de Android Studio

Notas:

- el proyecto usa `minSdk 36`, así que solo apunta a Android 16+
- en este repo no se está usando `gradlew`; la compilación se ha validado desde Android Studio

## Cómo ejecutar la app

1. Abre el repo en Android Studio.
2. Asegúrate de que el `Gradle JDK` apunta al JDK embebido o a Java 17+.
3. Sincroniza el proyecto.
4. Ejecuta `:app` sobre un emulador o dispositivo Android 16+.

## Cómo probar Wake-on-LAN en local

El repo incluye un listener Python para verificar que llegan magic packets:

- [listen_wol.py](/var/home/neikon/repos/kinetic_wol/scripts/listen_wol.py)
- [scripts/README.md](/var/home/neikon/repos/kinetic_wol/scripts/README.md)

Ejemplo recomendado:

```bash
python3 scripts/listen_wol.py --port 4009 --once
```

Luego crea un dispositivo en la app apuntando a tu host:

- `Nombre`: el que prefieras
- `MAC`: una MAC válida de prueba
- `Host`: IP de tu equipo o `10.0.2.2` si disparas desde emulador Android
- `Puerto`: `4009`

## Cómo probar el flujo headless

Una vez guardado un dispositivo en la app, puedes disparar el fulfillment sin tocar la UI:

```bash
adb -s emulator-5554 shell am start -W \
  -a dev.neikon.kineticwol.action.WAKE_DEVICE \
  -n dev.neikon.kineticwol/.actions.WakeDeviceActivity \
  --es deviceName "bazzite"
```

Eso debería resolver el dispositivo guardado por nombre y enviar el magic packet.

## App Actions y Gemini

El proyecto ya incluye la base necesaria:

- `shortcuts.xml`
- `meta-data` de shortcuts en el manifest
- activity headless exportada
- shortcuts dinámicos por equipo

Pero hay límites importantes:

- no se ha encontrado un built-in intent oficial específico para Wake-on-LAN
- la solución actual usa un `custom intent`
- la documentación oficial sigue mencionando un `App Actions test tool`, pero en el entorno del proyecto ese plugin no aparece disponible en Android Studio
- la validación real de Assistant/Gemini probablemente requerirá una `internal testing release`

La guía preparada para eso está en:

- [release_internal_testing.md](/var/home/neikon/repos/kinetic_wol/docs/release_internal_testing.md)

## Tests

Tests unitarios incluidos:

- [WakeOnLanSenderTest.kt](/var/home/neikon/repos/kinetic_wol/app/src/test/java/dev/neikon/kineticwol/domain/wol/WakeOnLanSenderTest.kt)
- [DeviceNameNormalizerTest.kt](/var/home/neikon/repos/kinetic_wol/app/src/test/java/dev/neikon/kineticwol/util/DeviceNameNormalizerTest.kt)

## Documentación del proyecto

- [AGENTS.md](/var/home/neikon/repos/kinetic_wol/AGENTS.md): índice y reglas operativas
- [docs/agent_memory.md](/var/home/neikon/repos/kinetic_wol/docs/agent_memory.md): memoria persistente del proyecto
- [docs/app_spec.md](/var/home/neikon/repos/kinetic_wol/docs/app_spec.md): especificación funcional y técnica
- [docs/roadmap.md](/var/home/neikon/repos/kinetic_wol/docs/roadmap.md): histórico, estado y próximos pasos
- [docs/integration_blueprint.md](/var/home/neikon/repos/kinetic_wol/docs/integration_blueprint.md): blueprint inicial de integración
- [docs/release_internal_testing.md](/var/home/neikon/repos/kinetic_wol/docs/release_internal_testing.md): guía para testing interno en Play Console

## Estado del roadmap

Hecho:

- CRUD de dispositivos
- persistencia con Room
- envío WOL manual
- fulfillment headless
- shortcuts dinámicos
- limpieza inicial de build con AGP 9
- mejoras de UX en formulario y navegación

Pendiente principal:

- validar la invocación real desde Assistant/Gemini fuera de `adb`
- endurecer la estrategia final de App Actions
- decidir si se añade historial persistente de eventos
