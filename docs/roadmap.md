# Roadmap

## Checklist completado

- [x] Crear branch inicial de trabajo `android-actions-wol`
- [x] Explorar el repo y revisar el blueprint inicial de integración
- [x] Definir decisiones de producto y tecnología con el usuario
- [x] Migrar completamente el proyecto a Android nativo
- [x] Añadir memoria persistente del proyecto
- [x] Añadir especificación detallada del producto
- [x] Añadir índice operativo en [AGENTS.md](../AGENTS.md)
- [x] Eliminar el scaffold anterior del árbol de trabajo
- [x] Crear proyecto Android con `compileSdk/minSdk/targetSdk 36`
- [x] Configurar base Compose + Material 3
- [x] Implementar modelo `WakeDevice`
- [x] Implementar persistencia con `Room`
- [x] Implementar `WakeOnLanSender`
- [x] Implementar dashboard Compose y pantalla de edición
- [x] Implementar `WakeDeviceActivity` headless
- [x] Añadir `shortcuts.xml`
- [x] Añadir recursos bilingües base
- [x] Añadir tests unitarios iniciales
- [x] Publicar ramas de trabajo y consolidar merges en `main`
- [x] Actualizar la toolchain local del proyecto con Android Studio
- [x] Añadir script Python para escuchar magic packets WOL en local
- [x] Integrar publicación de shortcuts dinámicos para equipos guardados
- [x] Añadir guía de release interna para validar App Actions con Play Console
- [x] Limpiar la build migrando a `built-in Kotlin` de AGP 9
- [x] Mejorar la robustez del formulario con validación de nombres duplicados
- [x] Mejorar la entrada de teclado del formulario por tipo de campo
- [x] Añadir placeholders y copy localizado en la UI principal
- [x] Ajustar la navegación del editor para volver al dashboard con el botón atrás
- [x] Limpiar la cabecera del editor y el badge inferior del título
- [x] Validar el envío WOL manual contra un listener UDP local
- [x] Validar el fulfillment headless mediante `adb`
- [x] Confirmar que los shortcuts dinámicos aparecen en Android
- [x] Mejorar el [README.md](../README.md)
- [x] Añadir una quick tile de Android para despertar equipos guardados desde el panel rápido
- [x] Permitir descartar de forma persistente el bloque hero del dashboard
- [x] Extender el modelo de dispositivo para soportar apagado remoto por agente Linux
- [x] Añadir migración Room para persistir configuración del agente de apagado remoto
- [x] Añadir sección de apagado remoto en el editor de dispositivos
- [x] Añadir acción manual `Apagar` para dispositivos con agente configurado
- [x] Alinear Android con las rutas canónicas `/api/v1/status` y `/api/v1/poweroff` de KineticSOL
- [x] Añadir flujo `Probar conexión` para el agente Linux
- [x] Diferenciar errores HTTP y de red del agente en la UI Android
- [x] Añadir diagnóstico de conectividad Android para KineticSOL con logging y validación de `baseUrl`
- [x] Habilitar cleartext HTTP temporalmente en Android para pruebas LAN del agente Linux

## Checklist en curso

- [ ] Validar end-to-end la integración con el agente Linux del otro repositorio
- [ ] Revisar y endurecer App Actions sobre la base ya validada
- [ ] Reducir warnings y complejidad de Gradle sin perder compatibilidad de build

## Checklist pendiente

- [ ] Implementar apagado remoto por SSH
- [ ] Validar envío WOL en dispositivo real y red local
- [ ] Comprobar invocación real desde Assistant/Gemini mediante surfaces disponibles o `internal testing release`
- [ ] Seguir endureciendo shortcuts dinámicos y flujo de fulfillment por voz
- [ ] Generar y subir una `internal testing release`
- [ ] Decidir si se añade historial persistente de eventos

## Riesgos abiertos

- [ ] Falta de toolchain local en esta sesión para compilar desde terminal
- [ ] El token del agente se persiste por ahora en `Room`; si la feature madura, habrá que moverlo a almacenamiento más seguro
- [ ] El `App Actions test tool` no está disponible en el Android Studio del usuario pese a que la documentación oficial todavía lo menciona
- [ ] Posible limitación de locale para voz en español
