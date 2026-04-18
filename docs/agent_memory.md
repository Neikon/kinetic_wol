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
- SSH queda planificado para una segunda fase, pero la arquitectura de `remoteShutdown` ya contempla ambos métodos

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

## Notas de build actuales

- La rama de limpieza migra la build a `built-in Kotlin` de AGP 9
- Los flags heredados de compatibilidad añadidos por Android Studio se han eliminado para reducir warnings y deuda técnica
- La build `assembleDebug` compila correctamente en el entorno Android Studio del usuario tras la limpieza inicial
- El repositorio sigue sin incluir `gradlew`, así que desde esta sesión no se puede ejecutar la build localmente aunque haya tests unitarios añadidos

## Preparacion de release

- La siguiente validación real recomendada es una `internal testing release` en Google Play
- Se ha añadido una guía operativa en `docs/release_internal_testing.md`
