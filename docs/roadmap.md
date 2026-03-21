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
- Añadido script Python para escuchar magic packets WOL en local
- Integrada publicación de shortcuts dinámicos para equipos guardados
- Añadida guía de release interna para validar App Actions con Play Console
- Iniciada limpieza de build migrando a `built-in Kotlin` de AGP 9
- Mejorada la robustez del formulario con validación de nombres duplicados y mejor entrada de teclado

## En curso

- Revisar y endurecer App Actions sobre la base ya validada
- Reducir warnings y complejidad de Gradle sin perder compatibilidad de build

## Pendiente inmediato

- validar envío WOL en dispositivo real y red local
- comprobar invocación real desde Assistant/Gemini mediante surfaces disponibles o `internal testing release`
- seguir endureciendo shortcuts dinámicos y flujo de fulfillment por voz
- generar y subir una `internal testing release`
- decidir si se añade historial persistente de eventos

## Riesgos abiertos

- falta de toolchain local en esta sesión para compilar
- el `App Actions test tool` no está disponible en el Android Studio del usuario pese a que la documentación oficial todavía lo menciona
- posible limitación de locale para voz en español
