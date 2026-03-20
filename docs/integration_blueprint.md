# Android Wake-on-LAN con voz

Documento de referencia para portar la funcionalidad Wake-on-LAN con App Actions a otro repositorio Android que ya tenga una GUI base.

## 1. Objetivo

Construir una app Android que permita:

- gestionar uno o varios equipos
- enviar paquetes Wake-on-LAN reales por UDP
- ser invocada por voz mediante Google Assistant / Gemini usando App Actions
- ejecutar el envío sin interacción visual obligatoria

Resultado esperado:

```text
"Hey Google, despierta mi PC con MiApp"
```

Y que ocurra:

1. Assistant/Gemini resuelva la App Action
2. la app reciba el intent con el parámetro del dispositivo
3. la app busque el dispositivo en almacenamiento local
4. la app envíe el magic packet
5. el PC objetivo reciba el paquete Wake-on-LAN

## 2. Restricciones

- lenguaje: Kotlin
- compatibilidad mínima: Android 8.0 (API 26)
- UI: puede mantenerse la existente del repo destino
- evitar librerías innecesarias
- usar sockets UDP nativos
- la lógica de Wake-on-LAN debe quedar desacoplada de la UI
- la ejecución por voz no debe depender de navegación manual en pantalla

## 3. Módulos lógicos a integrar

En el repo destino deben existir, como mínimo, estas piezas conceptuales:

### 3.1 Modelo de dominio

Entidad recomendada:

```kotlin
data class WakeDevice(
    val id: String,
    val name: String,
    val macAddress: String,
    val host: String,
    val port: Int = 9,
)
```

Responsabilidades:

- identificar de forma estable el dispositivo
- mostrarlo en la UI
- resolverlo por nombre desde Assistant
- contener la información necesaria para emitir el paquete WOL

### 3.2 Servicio de Wake-on-LAN

Nombre recomendado:

```kotlin
WakeOnLanSender
```

Responsabilidades:

- validar y normalizar la MAC
- construir el magic packet
- enviarlo por UDP al host/broadcast y puerto configurados

Contrato recomendado:

```kotlin
class WakeOnLanSender {
    suspend fun send(device: WakeDevice)
    fun buildMagicPacket(macAddress: String): ByteArray
}
```

### 3.3 Persistencia

Puede implementarse con:

- DataStore si el repo aún no tiene almacenamiento estructurado
- Room si el repo ya trabaja con base de datos

Responsabilidades:

- guardar lista de dispositivos
- añadir, editar y eliminar
- buscar por nombre para fulfillment de Assistant

Contrato mínimo:

```kotlin
interface DeviceRepository {
    suspend fun upsert(device: WakeDevice)
    suspend fun delete(deviceId: String)
    suspend fun findByName(name: String): WakeDevice?
    fun observeDevices(): Flow<List<WakeDevice>>
}
```

### 3.4 Fulfillment headless

Componente recomendado:

- `Activity` ligera / headless

Nombre recomendado:

```kotlin
WakeDeviceActivity
```

Responsabilidades:

- recibir el intent desde App Actions
- extraer `device.name`
- resolver el dispositivo en persistencia
- ejecutar `WakeOnLanSender.send`
- finalizar sin depender de UI adicional

## 4. Implementación Wake-on-LAN

## 4.1 Formato del magic packet

Debe seguir este formato exacto:

- 6 bytes `FF FF FF FF FF FF`
- 16 repeticiones consecutivas de la MAC address en binario

Longitud total esperada:

```text
6 + (16 * 6) = 102 bytes
```

## 4.2 Reglas de MAC

La app debe aceptar, como mínimo:

- `AA:BB:CC:DD:EE:FF`
- `AA-BB-CC-DD-EE-FF`
- `AABBCCDDEEFF`

Se recomienda normalizar eliminando `:` y `-`, validar que queden 12 dígitos hexadecimales y convertir de dos en dos.

## 4.3 Envío UDP

El envío debe hacerse con `DatagramSocket`.

Requisitos:

- resolver `host` o broadcast con `InetAddress.getByName`
- usar el puerto configurado del dispositivo
- activar `socket.broadcast = true`
- ejecutar en `Dispatchers.IO`

Ejemplo de comportamiento:

- destino típico: `192.168.1.255`
- puerto típico: `9`

## 5. Integración con GUI existente

No conviene reemplazar la GUI del repo destino si ya existe una base útil. La integración correcta es:

1. conservar la estructura visual existente
2. sustituir mocks por persistencia real
3. conectar botones o acciones a `WakeOnLanSender`
4. añadir formularios o pantallas necesarias solo donde falten

## 5.1 Capacidades mínimas de la UI

La GUI final debe permitir:

- listar dispositivos
- crear dispositivo
- editar dispositivo
- eliminar dispositivo
- lanzar manualmente Wake-on-LAN

Campos mínimos en la UI:

- nombre
- MAC
- IP o broadcast
- puerto

## 5.2 Validaciones de UI

La UI debe impedir guardar si:

- el nombre está vacío
- la MAC está vacía o claramente mal formada
- el host está vacío
- el puerto no está entre `1` y `65535`

## 6. App Actions

## 6.1 Intent a soportar

Intent solicitado:

```text
actions.intent.WAKE_DEVICE
```

Parámetro:

```text
device.name
```

## 6.2 Query patterns

Deben incluir frases como:

- `despierta mi pc con MiApp`
- `enciende $device.name con MiApp`
- `wake $device.name with MiApp`

Nota importante:

- la marca de invocación `con MiApp` debe alinearse con el nombre real visible de la aplicación
- si el nombre del producto cambia, deben cambiar también las frases de entrenamiento

## 6.3 shortcuts.xml

Debe existir un archivo:

```text
app/src/main/res/xml/shortcuts.xml
```

Estructura esperada:

- `<capability android:name="actions.intent.WAKE_DEVICE">`
- `<intent>` apuntando al componente Android que manejará la acción
- `<parameter>` que conecte `device.name` con una extra o key local
- `<shortcut>` opcional con `capability-binding`

Puntos clave:

- `android:targetPackage` debe coincidir con el package real de la app
- `android:targetClass` debe apuntar a la actividad/receiver correctos
- la key local usada en `<parameter>` debe coincidir con la key que leerá el fulfillment

## 6.4 AndroidManifest

Debe incluir:

- `meta-data` con `android.app.shortcuts`
- declaración del componente de fulfillment exportado
- `intent-filter` con la action interna elegida
- permiso de red si la app no lo tiene ya

Ejemplo conceptual:

```xml
<meta-data
    android:name="android.app.shortcuts"
    android:resource="@xml/shortcuts" />
```

Y un componente tipo:

```xml
<activity
    android:name=".actions.WakeDeviceActivity"
    android:exported="true"
    android:noHistory="true" />
```

## 7. Fulfillment sin UI

La forma más simple y robusta para este caso es una `Activity` headless o casi invisible.

Flujo recomendado:

1. Assistant lanza la activity
2. la activity extrae `device.name`
3. consulta repositorio
4. llama a `WakeOnLanSender`
5. devuelve `RESULT_OK` o `RESULT_CANCELED`
6. finaliza

Ventajas:

- implementación simple
- buena compatibilidad
- fácil de probar con `adb`

## 7.1 Extracción del parámetro

La extracción debe ser tolerante, porque según la integración puede variar la key entregada. Conviene revisar:

- `deviceName`
- `device.name`
- cualquier extra equivalente que defina `shortcuts.xml`

## 7.2 Resolución del dispositivo

La estrategia base debe ser:

- coincidencia exacta normalizada por minúsculas y trim

Normalización recomendada:

```text
trim + lowercase
```

Opcional futuro:

- alias por dispositivo
- coincidencia flexible
- desambiguación por varios resultados

## 8. Arquitectura recomendada en el repo destino

Si el repo destino ya tiene arquitectura, adaptarse a ella. Si no la tiene clara, usar esta:

```text
ui/
  screens/
  components/
data/
  repository/
  storage/
domain/
  model/
  wol/
actions/
  WakeDeviceActivity.kt
```

Mapeo sugerido:

- `domain/model/WakeDevice.kt`
- `domain/wol/WakeOnLanSender.kt`
- `data/repository/DeviceRepository.kt`
- `data/storage/...`
- `actions/WakeDeviceActivity.kt`

## 9. Plan de migración desde el prototipo visual

Este es el orden recomendado para integrar sobre una app ya existente.

### Fase 1. Auditoría del repo destino

Antes de tocar nada:

- identificar package name
- identificar `minSdk`, `targetSdk`, Compose/XML, navegación y arquitectura
- localizar pantalla de lista o dashboard
- localizar formulario existente si ya lo hay
- detectar si ya existe DataStore, Room o repositorios

Entregable:

- mapa breve del proyecto y puntos de integración

### Fase 2. Integrar modelo y persistencia

Tareas:

- crear o adaptar `WakeDevice`
- crear repositorio o store real
- reemplazar mocks de dispositivos por datos persistidos

Criterio de éxito:

- la lista de dispositivos se guarda y se recupera tras reiniciar la app

### Fase 3. Integrar Wake-on-LAN real

Tareas:

- crear `WakeOnLanSender`
- conectar acción manual en UI
- mostrar feedback básico de éxito o error

Criterio de éxito:

- desde la app se envía el magic packet real

### Fase 4. Integrar App Actions

Tareas:

- crear `shortcuts.xml`
- actualizar `AndroidManifest.xml`
- crear activity headless
- implementar búsqueda por nombre y envío

Criterio de éxito:

- Assistant invoca la app con un nombre de dispositivo válido y se envía el paquete

### Fase 5. Validación y endurecimiento

Tareas:

- probar casos de error
- revisar normalización de nombres
- revisar logs
- validar comportamiento con app abierta y cerrada

## 10. Casos de error que deben contemplarse

La implementación final debe manejar claramente:

- falta `device.name` en el intent
- dispositivo no encontrado
- MAC inválida
- host inválido o no resoluble
- error de socket / red
- puerto fuera de rango

Comportamiento mínimo esperado:

- no crashear
- registrar error
- devolver resultado cancelado o mensaje de error si aplica

## 11. Logs y depuración

Conviene añadir logs en:

- recepción del intent
- nombre de dispositivo extraído
- resultado de búsqueda en repositorio
- intento de envío
- éxito o excepción

Sin exponer datos innecesarios en producción.

## 12. Testing recomendado

## 12.1 Unit tests

Como mínimo:

- construcción del magic packet
- validación de MAC
- búsqueda de dispositivo por nombre normalizado

## 12.2 Pruebas manuales con adb

Debe existir una forma de probar el fulfillment sin Assistant:

```bash
adb shell am start \
  -a com.tuapp.action.WAKE_DEVICE \
  -n com.tuapp/.actions.WakeDeviceActivity \
  --es deviceName "Gaming PC"
```

Objetivo:

- desacoplar la validación del intent handling respecto a Assistant

## 12.3 Pruebas con script Python

Para no apagar el PC cada vez, usar un listener UDP local como verificador de llegada del magic packet.

Script de referencia:

```text
scripts/listen_wol.py
```

Uso típico:

```bash
python3 scripts/listen_wol.py --port 9 --mac AA:BB:CC:DD:EE:FF --once
```

Objetivo:

- confirmar que la app está emitiendo correctamente el paquete
- aislar el problema de BIOS/NIC/configuración WOL del problema de la app

## 12.4 Pruebas con App Actions Test Tool

Flujo recomendado:

1. abrir Android Studio
2. abrir App Actions Test Tool
3. crear preview de `actions.intent.WAKE_DEVICE`
4. lanzar invocation con `device.name`
5. verificar que la activity headless se ejecuta
6. verificar recepción del paquete con el script Python

## 13. Criterios de aceptación

La funcionalidad se considera terminada cuando:

- se pueden crear, editar y eliminar dispositivos
- la lista persiste entre ejecuciones
- pulsar el botón de despertar envía un magic packet válido
- el intent interno vía `adb` despierta o al menos emite el paquete
- Assistant/Gemini puede invocar la App Action
- el parámetro `device.name` resuelve el dispositivo correcto
- no hay dependencia de UI manual para el flujo por voz

## 14. Decisiones no negociables

Estas decisiones deben mantenerse salvo cambio explícito:

- Wake-on-LAN real vía UDP, no simulación
- lógica WOL encapsulada en `WakeOnLanSender`
- nombre del dispositivo almacenado localmente
- fulfillment sin dependencia de interacción manual
- soporte mínimo Android 8+
- Kotlin como lenguaje principal

## 15. Código de referencia existente

En este workspace existe una implementación base que sirve como fuente de verdad funcional:

- [WakeOnLanSender.kt](/var/home/neikon/repos/wok_hf/app/src/main/java/com/neikon/miappwol/wol/WakeOnLanSender.kt)
- [WakeDevice.kt](/var/home/neikon/repos/wok_hf/app/src/main/java/com/neikon/miappwol/model/WakeDevice.kt)
- [DeviceStore.kt](/var/home/neikon/repos/wok_hf/app/src/main/java/com/neikon/miappwol/data/DeviceStore.kt)
- [WakeDeviceActivity.kt](/var/home/neikon/repos/wok_hf/app/src/main/java/com/neikon/miappwol/actions/WakeDeviceActivity.kt)
- [shortcuts.xml](/var/home/neikon/repos/wok_hf/app/src/main/res/xml/shortcuts.xml)
- [AndroidManifest.xml](/var/home/neikon/repos/wok_hf/app/src/main/AndroidManifest.xml)
- [listen_wol.py](/var/home/neikon/repos/wok_hf/scripts/listen_wol.py)

Nota:

- esta implementación es una base funcional de referencia
- al portarla al nuevo repo habrá que adaptar package names, estructura, dependencias y arquitectura existente

## 16. Instrucción para la siguiente conversación

Cuando abras el repo nuevo en otro workspace, el objetivo debe plantearse así:

```text
Lee docs/integration_blueprint.md y usa ese documento como especificación de implementación.
Analiza la arquitectura actual del repo y adapta la solución Wake-on-LAN + App Actions a este proyecto existente sin rehacer la GUI desde cero.
```

Y, si quieres maximizar continuidad, añade también:

```text
Si hay conflictos entre la especificación y la arquitectura actual del repo, prioriza mantener la arquitectura del repo y adaptar la implementación.
```

## 17. Siguiente paso recomendado

Proceso ideal:

1. clonar el repo real en un nuevo workspace
2. copiar este documento `docs/integration_blueprint.md` al repo nuevo
3. abrir una nueva conversación en ese workspace
4. pedir la integración completa usando este documento como contrato técnico

