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
- La integración de App Actions para este caso no dispone de un BII oficial específico para Wake-on-LAN.
- La implementación actual se apoyará en un fulfillment headless y en un custom intent para la parte de Assistant/App Actions.
- Según la documentación oficial actual, los custom intents de App Actions tienen limitación de locale `en-US`; esto es un riesgo conocido para el objetivo de voz en español.

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
- Tests unitarios básicos para WOL y normalización
