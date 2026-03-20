# Roadmap

## Hecho

- Creado branch de trabajo `android-actions-wol`
- Explorado el prototipo web original
- Leído `docs/integration_blueprint.md`
- Definidas decisiones de producto y tecnología con el usuario
- Decidido migrar completamente a Android nativo
- Añadida memoria persistente del proyecto
- Añadida especificación detallada del producto
- Añadido índice `AGENTS.md`
- Eliminado el scaffold web original del árbol de trabajo
- Creado proyecto Android con `compileSdk/minSdk/targetSdk 36`
- Configurada base Compose + Material 3
- Implementado modelo `WakeDevice`
- Implementada persistencia con `Room`
- Implementado `WakeOnLanSender`
- Implementado dashboard Compose y pantalla de edición
- Implementada `WakeDeviceActivity` headless
- Añadido `shortcuts.xml`
- Añadidos recursos bilingües base
- Añadidos tests unitarios iniciales
- Rama publicada en remoto y pull request abierto
- Android Studio actualizó AGP, Gradle wrapper y configuración de daemon JVM para la build local
 
## En curso

- Revisar y endurecer la base Android hasta primera build real

## Pendiente inmediato

- abrir el proyecto en Android Studio con toolchain Android moderna
- generar o completar el Gradle wrapper real si hiciera falta
- compilar y corregir errores de integración que solo salgan en build
- validar envío WOL en dispositivo real y red local
- validar App Actions desde Android Studio App Actions Test Tool
- decidir si se añade historial persistente de eventos

## Riesgos abiertos

- falta de toolchain local en esta sesión para compilar
- validación real de App Actions pendiente en Android Studio
- posible limitación de locale para voz en español
