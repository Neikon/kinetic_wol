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
- [x] Extender la quick tile para ofrecer apagado remoto cuando el dispositivo lo soporte
- [x] Añadir un icono adaptive propio para la app siguiendo el modelo de Android
- [x] Añadir pipeline de GitHub Actions para compilar APK y publicar una GitHub Release en cada push a `main`
- [x] Implementar apagado remoto por SSH con clave privada, fingerprint y comando configurable
- [x] Añadir generación de claves SSH en la app y captura automática del fingerprint del host en la primera prueba
- [x] Validar end-to-end la integración con el agente Linux HTTP del otro repositorio
- [x] Validar end-to-end el apagado remoto por SSH en un PC Linux real con sudoers preparado
- [x] Validar end-to-end el apagado remoto por SSH en TrueNAS SCALE
- [x] Ajustar el pipeline de GitHub Actions para usar Gradle `9.4.1` tras actualizar AGP a `9.2.0`
- [x] Optar el workflow de GitHub Actions a Node.js 24 para eliminar el warning de deprecación de Node.js 20
- [x] Actualizar las actions del pipeline a variantes Node.js 24 y sustituir `android-actions/setup-android` por `sdkmanager` directo
- [x] Confirmar que el pipeline vuelve a compilar correctamente y sin warnings de Node.js 20
- [x] Evitar que el pipeline de build/release se ejecute en pushes que solo cambian documentación Markdown
- [x] Actualizar el README para reflejar apagado remoto, estado pendiente de voz y enlace rápido a releases
- [x] Añadir al README el enlace al agente Linux `Neikon/kinetic_sol` y aclarar las dos vías de apagado remoto
- [x] Añadir guía detallada para configurar apagado remoto por SSH en equipos Linux

## Checklist en curso

- [ ] Revisar y endurecer App Actions sobre la base ya validada
- [ ] Reducir warnings y complejidad de Gradle sin perder compatibilidad de build

## Checklist pendiente

- [ ] Validar envío WOL en dispositivo real y red local
- [ ] Comprobar invocación real desde Assistant/Gemini mediante surfaces disponibles o `internal testing release`
- [ ] Seguir endureciendo shortcuts dinámicos y flujo de fulfillment por voz
- [ ] Generar y subir una `internal testing release`
- [ ] Decidir si se añade historial persistente de eventos

## Riesgos abiertos

- [ ] Falta de toolchain local en esta sesión para compilar desde terminal
- [ ] El token del agente se persiste por ahora en `Room`; si la feature madura, habrá que moverlo a almacenamiento más seguro
- [ ] La clave privada SSH se persiste por ahora en `Room`; si la feature madura, habrá que moverla a almacenamiento más seguro
- [ ] El `App Actions test tool` no está disponible en el Android Studio del usuario pese a que la documentación oficial todavía lo menciona
- [ ] Posible limitación de locale para voz en español
