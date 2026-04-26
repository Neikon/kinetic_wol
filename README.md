# Kinetic WOL

Kinetic WOL es una app Android nativa para guardar equipos de red, despertarlos con Wake-on-LAN y apagarlos de forma remota cuando el dispositivo tiene una ruta de apagado configurada.

El apagado remoto puede hacerse por dos vías:

- SSH directo al equipo remoto
- agente Linux HTTP, disponible en [Neikon/kinetic_sol](https://github.com/Neikon/kinetic_sol)

La descarga rápida del APK está en las releases del repositorio:

- [GitHub Releases](https://github.com/Neikon/kinetic_wol/releases)

El control por voz con Assistant/Gemini todavía está pendiente. El proyecto incluye base técnica para App Actions y fulfillment headless, pero la invocación real desde voz aún no está cerrada ni validada como funcional para usuarios finales.

## Estado actual

- App Android nativa funcional
- UI principal en Jetpack Compose
- Persistencia local real con Room
- Envío WOL manual funcional
- Apagado remoto manual funcional por agente Linux HTTP
- Apagado remoto manual funcional por SSH con clave privada, fingerprint de host y comando configurable
- Quick tile de Android con selector para despertar y apagar dispositivos compatibles
- Fulfillment headless funcional mediante `WakeDeviceActivity`
- Shortcuts dinámicos publicados para equipos guardados
- Recursos bilingües en español e inglés
- Pipeline de GitHub Actions que compila APK debug y publica una prerelease en cada push relevante a `main`

Validaciones ya hechas sobre la app:

- creación, edición y borrado de dispositivos
- envío de magic packets contra un listener UDP local
- apagado remoto contra el agente Linux HTTP
- apagado remoto por SSH en un PC Linux con `sudoers` preparado
- apagado remoto por SSH en TrueNAS SCALE
- invocación headless por `adb` resolviendo dispositivos por nombre
- aparición de shortcuts dinámicos al mantener pulsado el icono de la app

Pendiente importante:

- el control por voz real con Assistant/Gemini todavía está por hacer
- falta generar y validar una `internal testing release` para cerrar el flujo real de App Actions

## Stack técnico

- Kotlin
- Jetpack Compose
- Material 3
- Room
- sshj
- Android SDK 36
- `minSdk = 36`
- Java 17

## Arquitectura

```text
app/src/main/java/dev/neikon/kineticwol/
├── actions/           # fulfillment headless y shortcuts dinámicos
├── data/              # Room y repositorio
├── domain/            # modelo y lógica WOL/apagado remoto
├── ui/                # Compose, estado y tema
└── util/              # utilidades compartidas
```

Piezas clave:

- [MainActivity.kt](app/src/main/java/dev/neikon/kineticwol/MainActivity.kt): entrada principal de la app
- [KineticWolApp.kt](app/src/main/java/dev/neikon/kineticwol/ui/KineticWolApp.kt): dashboard, formulario y navegación principal
- [HomeViewModel.kt](app/src/main/java/dev/neikon/kineticwol/ui/home/HomeViewModel.kt): estado, validación y acciones de la pantalla
- [WakeOnLanSender.kt](app/src/main/java/dev/neikon/kineticwol/domain/wol/WakeOnLanSender.kt): normalización MAC y envío del magic packet
- [AgentShutdownSender.kt](app/src/main/java/dev/neikon/kineticwol/domain/shutdown/AgentShutdownSender.kt): apagado remoto por agente Linux HTTP
- [SshShutdownSender.kt](app/src/main/java/dev/neikon/kineticwol/domain/shutdown/SshShutdownSender.kt): apagado remoto por SSH
- [SshKeyMaterialGenerator.kt](app/src/main/java/dev/neikon/kineticwol/domain/shutdown/SshKeyMaterialGenerator.kt): generación de claves SSH dentro de la app
- [WakeDeviceActivity.kt](app/src/main/java/dev/neikon/kineticwol/actions/WakeDeviceActivity.kt): ejecución headless por intent
- [WakeQuickTileService.kt](app/src/main/java/dev/neikon/kineticwol/actions/WakeQuickTileService.kt): tile rápida de Android
- [DeviceShortcutPublisher.kt](app/src/main/java/dev/neikon/kineticwol/actions/DeviceShortcutPublisher.kt): shortcuts dinámicos por dispositivo
- [shortcuts.xml](app/src/main/res/xml/shortcuts.xml): capability y query patterns de App Actions

## Requisitos de entorno

Para abrir y compilar el proyecto:

- Android Studio reciente
- Android SDK 36
- JDK 17 o el `Embedded JDK` de Android Studio

Notas:

- el proyecto usa `minSdk 36`, así que solo apunta a Android 16+
- en este repo no se está usando `gradlew`; la compilación se ha validado desde Android Studio

## Descargar la app

El workflow de GitHub Actions publica una prerelease por cada push relevante a `main`.

Para descargar el APK:

1. Abre [GitHub Releases](https://github.com/Neikon/kinetic_wol/releases).
2. Entra en la release más reciente `main-...`.
3. Descarga el archivo `kinetic-wol-main-....apk`.

## Cómo ejecutar la app

1. Abre el repo en Android Studio.
2. Asegúrate de que el `Gradle JDK` apunta al JDK embebido o a Java 17+.
3. Sincroniza el proyecto.
4. Ejecuta `:app` sobre un emulador o dispositivo Android 16+.

## Cómo probar Wake-on-LAN en local

El repo incluye un listener Python para verificar que llegan magic packets:

- [listen_wol.py](scripts/listen_wol.py)
- [scripts/README.md](scripts/README.md)

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

## Cómo configurar apagado remoto

Kinetic WOL permite apagar un dispositivo guardado si se activa la sección de apagado remoto en el formulario del dispositivo.

Métodos soportados:

- `Agente`: agente Linux HTTP autenticado por Bearer token
- `SSH`: conexión SSH por clave privada, fingerprint de host y comando remoto configurable

### Agente Linux HTTP

El agente Linux compatible con esta ruta está en:

- [Neikon/kinetic_sol](https://github.com/Neikon/kinetic_sol)

La app espera este contrato:

- `GET /api/v1/status`
- `POST /api/v1/poweroff`
- `Authorization: Bearer <token>`

En el editor del dispositivo configura:

- URL base del agente
- token
- método `Agente`

Usa `Probar conexión` antes de guardar como ruta de apagado habitual.

### SSH

En el editor del dispositivo configura:

- host SSH
- puerto, normalmente `22`
- usuario
- clave privada
- fingerprint del host
- comando remoto

La app puede generar un par de claves SSH. Copia la clave pública que muestra la app al `authorized_keys` del usuario remoto.

El comando por defecto es:

```bash
sudo -n systemctl poweroff
```

Para TrueNAS SCALE se ha validado usando:

```bash
sudo -n /usr/sbin/shutdown -h now
```

El usuario remoto debe poder ejecutar el comando configurado sin prompt interactivo. En Linux normalmente hay que preparar una regla específica de `sudoers` o, en TrueNAS SCALE, añadir el ejecutable permitido en los comandos `sudo` sin contraseña del usuario.

Guía detallada para Linux:

- [Configurar apagado remoto por SSH en Linux](docs/ssh_linux_shutdown.md)

Guía detallada para TrueNAS SCALE desde la Web UI:

- [Configurar apagado remoto por SSH en TrueNAS SCALE](docs/truenas_scale_ssh_shutdown.md)

## App Actions y Gemini

Estado: pendiente de cerrar y validar como experiencia real de usuario.

El proyecto incluye base técnica:

- `shortcuts.xml`
- `meta-data` de shortcuts en el manifest
- activity headless exportada
- shortcuts dinámicos por equipo

Pero el control por voz todavía no está terminado:

- no se ha encontrado un built-in intent oficial específico para Wake-on-LAN
- la solución actual usa un `custom intent`
- la documentación oficial sigue mencionando un `App Actions test tool`, pero en el entorno del proyecto ese plugin no aparece disponible en Android Studio
- la validación real de Assistant/Gemini probablemente requerirá una `internal testing release`

La guía preparada para eso está en:

- [release_internal_testing.md](docs/release_internal_testing.md)

## Tests

Tests unitarios incluidos:

- [WakeOnLanSenderTest.kt](app/src/test/java/dev/neikon/kineticwol/domain/wol/WakeOnLanSenderTest.kt)
- [DeviceNameNormalizerTest.kt](app/src/test/java/dev/neikon/kineticwol/util/DeviceNameNormalizerTest.kt)
- [AgentShutdownSenderTest.kt](app/src/test/java/dev/neikon/kineticwol/domain/shutdown/AgentShutdownSenderTest.kt)
- [SshShutdownSenderTest.kt](app/src/test/java/dev/neikon/kineticwol/domain/shutdown/SshShutdownSenderTest.kt)

## Documentación del proyecto

- [AGENTS.md](AGENTS.md): índice y reglas operativas
- [docs/agent_memory.md](docs/agent_memory.md): memoria persistente del proyecto
- [docs/app_spec.md](docs/app_spec.md): especificación funcional y técnica
- [docs/roadmap.md](docs/roadmap.md): histórico, estado y próximos pasos
- [docs/release_internal_testing.md](docs/release_internal_testing.md): guía para testing interno en Play Console

## Estado del roadmap

- [x] CRUD de dispositivos
- [x] Persistencia con Room
- [x] Envío WOL manual
- [x] Apagado remoto por agente Linux HTTP
- [x] Apagado remoto por SSH
- [x] Fulfillment headless
- [x] Shortcuts dinámicos
- [x] Quick tile de Android
- [x] Publicación automática de APK en GitHub Releases
- [x] Limpieza inicial de build con AGP 9
- [x] Mejoras de UX en formulario y navegación
- [ ] Implementar/cerrar la invocación real desde Assistant/Gemini fuera de `adb`
- [ ] Endurecer la estrategia final de App Actions y validar una `internal testing release`
- [ ] Decidir si se añade historial persistente de eventos
