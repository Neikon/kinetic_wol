# Memoria del agente

## Producto

- Nombre final: `Kinetic WOL`
- App Android nativa
- Objetivo principal: despertar equipos por Wake-on-LAN
- Invocación por asistente: App Actions con foco en compatibilidad con Gemini en Android

## Decisiones activas

- UI con `Jetpack Compose`
- Persistencia con `Room`
- Arquitectura simple sin librerías innecesarias
- Compatibilidad solo con `Android 16+`
- Idiomas: español base, inglés secundario
- `applicationId`: `dev.neikon.kineticwol`
- El modelo de dispositivo se amplía para soportar capacidades adicionales además de Wake-on-LAN
- La primera ruta de apagado remoto será un agente Linux HTTP autenticado por token
- La app ya implementa dos rutas de apagado remoto:
  - agente Linux HTTP autenticado por token
  - SSH con clave privada, fingerprint de host y comando configurable
- La ruta SSH se apoya en una precondición explícita: el usuario remoto debe poder ejecutar `sudo -n systemctl poweroff` o un comando equivalente sin prompt interactivo

## Contexto funcional

- La estructura visual del prototipo original se conserva conceptualmente:
  - dashboard con resumen
  - lista de dispositivos
  - acción manual de despertar
  - formulario de alta/edición
  - sección informativa de voz
  - registro reciente de eventos

## Restricciones y notas

- El prototipo web original se ha eliminado del árbol de trabajo.
- El `integration_blueprint.md` inicial se retiró del repositorio al quedar obsoleto y contener referencias locales que no debían publicarse.
- La integración de App Actions para este caso no dispone de un BII oficial específico para Wake-on-LAN.
- La implementación actual se apoyará en un fulfillment headless y en un custom intent para la parte de Assistant/App Actions.
- Según la documentación oficial actual, los custom intents de App Actions tienen limitación de locale `en-US`; esto es un riesgo conocido para el objetivo de voz en español.
- La documentación oficial sigue mencionando el `App Actions test tool`, pero en el Android Studio del usuario no aparece el plugin correspondiente; la validación práctica queda apoyada en `adb`, shortcuts dinámicos y, si se desea cerrar el flujo real, una `internal testing release`.

## Estado del entorno de esta sesión

- No había `java` instalado
- No había `gradle` instalado
- No se ha podido compilar ni ejecutar tests instrumentados desde terminal

## Implementado en esta sesión

- Proyecto Android creado desde cero en este repositorio
- Base Compose con dashboard y editor de dispositivos
- Persistencia local con `Room`
- `WakeOnLanSender` con normalización MAC y magic packet
- `WakeDeviceActivity` headless para fulfillment
- `shortcuts.xml` con capacidad de App Actions basada en custom intent
- publicación de shortcuts dinámicos por dispositivo para Assistant/Gemini
- quick tile de Android con selector rápido para despertar dispositivos guardados
- bloque hero de la pantalla principal descartable y persistido localmente
- Tests unitarios básicos para WOL y normalización
- Android Studio actualizó la toolchain del proyecto a AGP `9.1.0`, Gradle `9.3.1` y daemon JVM `21`
- Validado el envío manual de WOL contra un listener Python local
- Validado el fulfillment headless mediante `adb` resolviendo dispositivos guardados por nombre
- Mejorada la UX del formulario con validación de nombres duplicados, limpieza selectiva de errores y teclados específicos por campo
- Primera implementación de apagado remoto por agente Linux en la app Android
- Migración de `Room` a versión 2 para persistir configuración del agente de apagado remoto
- UI de edición extendida con sección de apagado remoto por agente y acción `Apagar` en tarjetas compatibles
- Integración Android alineada con el contrato HTTP actual de KineticSOL:
  - `GET /api/v1/status`
  - `POST /api/v1/poweroff`
  - Bearer token obligatorio
  - flujo de `Probar conexión`
  - mapeo específico de `401`, `404`, `503`, timeout y error de red
- Ajustado el cliente Android para diagnosticar mejor fallos de conectividad con KineticSOL:
  - logging explícito de URL, método, código HTTP y excepción real
  - validación previa de `baseUrl`
  - separación de host no resoluble, conexión rechazada, timeout, SSL y cleartext
  - `usesCleartextTraffic=true` activado temporalmente para pruebas LAN con `http://`
- La quick tile de Android ahora abre el picker si un único dispositivo también soporta apagado remoto, y el picker muestra `Apagar` cuando esa capacidad está lista
- Implementada la primera versión de apagado remoto por SSH:
  - persistencia Room ampliada para host, puerto, usuario, clave privada, fingerprint, passphrase opcional y comando
  - selector de método `Agente` / `SSH` en el editor del dispositivo
  - generación de par de claves SSH dentro de la app y visualización de la clave pública para copiarla al host
  - prueba de conexión SSH con autenticación real y comando remoto inocuo
  - captura automática del fingerprint del host en la primera prueba SSH si aún no estaba guardado
  - apagado SSH usando el comando configurado, por defecto `sudo -n systemctl poweroff`
  - quick tile y picker actualizados para apagar también por SSH
- La app ya define un icono adaptive propio con foreground, background y variante monochrome para themed icons
- Se añadió un workflow de GitHub Actions para compilar `:app:assembleDebug`, adjuntar el APK como artifact y publicar una GitHub Release en cada push a `main`
- El usuario confirmó que la integración end-to-end con el agente Linux HTTP del otro repositorio funciona correctamente
- El usuario confirmó que el apagado remoto por SSH funciona end-to-end en un PC Linux con sudoers preparado
- El usuario confirmó que el apagado remoto por SSH funciona end-to-end en TrueNAS SCALE
- El README se actualizó para presentar Kinetic WOL como app de despertar y apagar dispositivos, enlazar GitHub Releases y dejar claro que el control por voz todavía está pendiente

## Notas de build actuales

- La rama de limpieza migra la build a `built-in Kotlin` de AGP 9
- Los flags heredados de compatibilidad añadidos por Android Studio se han eliminado para reducir warnings y deuda técnica
- La build `assembleDebug` compila correctamente en el entorno Android Studio del usuario tras la limpieza inicial
- Android Studio actualizó la toolchain del proyecto a AGP `9.2.0` y Gradle `9.4.1`
- El pipeline de GitHub Actions debe usar Gradle `9.4.1`; falló al quedarse fijado en `9.3.1` tras la actualización de AGP
- El workflow se ajustó para usar actions que ya declaran Node.js 24 y para configurar Android SDK con `sdkmanager` directo, evitando `android-actions/setup-android@v3`
- El usuario confirmó que el pipeline resultante funciona y ya no muestra warnings de Node.js 20
- El workflow de build/release ignora pushes que solo cambian documentación Markdown (`**/*.md` y `docs/**`)
- El repositorio sigue sin incluir `gradlew`, así que desde esta sesión no se puede ejecutar la build localmente aunque haya tests unitarios añadidos

## Preparacion de release

- La siguiente validación real recomendada es una `internal testing release` en Google Play
- Se ha añadido una guía operativa en `docs/release_internal_testing.md`

## Riesgos activos de SSH

- La clave privada SSH se persiste por ahora en `Room`, igual que el token del agente; si la funcionalidad madura, habrá que mover ambos secretos a almacenamiento más seguro
- La carga de ciertos formatos de clave puede depender de la compatibilidad real de `sshj` en Android con el formato pegado por el usuario
- El test de conexión SSH valida red, fingerprint, clave y ejecución remota, pero no garantiza por sí mismo que `sudoers` esté correctamente preparado para el comando final de apagado
