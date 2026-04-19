# Especificación de Kinetic WOL

## 1. Propósito

Kinetic WOL es una app Android que permite registrar dispositivos de red y enviarles paquetes Wake-on-LAN reales. Además, prepara un flujo de invocación por voz con App Actions para que el envío pueda ejecutarse sin interacción visual manual.

## 2. Público objetivo

- uso personal del autor en Android 16+
- posible distribución futura a otros usuarios

## 3. Requisitos funcionales

### 3.1 Gestión de dispositivos

La app debe permitir:

- listar dispositivos guardados
- crear un dispositivo
- editar un dispositivo
- eliminar un dispositivo

Cada dispositivo tendrá como mínimo:

- nombre
- MAC address
- host o broadcast
- puerto

Además, podrá incluir capacidades opcionales de control remoto:

- apagado remoto por agente Linux HTTP autenticado por token
- apagado remoto por SSH en una fase posterior

### 3.2 Wake-on-LAN

La app debe:

- validar y normalizar la MAC
- construir un magic packet de 102 bytes
- enviarlo por UDP usando `DatagramSocket`
- permitir envío manual desde la UI
- permitir envío headless desde un intent de voz

### 3.3 Apagado remoto

La app debe preparar un modelo extensible para acciones remotas adicionales por dispositivo.

Primera fase:

- activar o desactivar apagado remoto por dispositivo
- configurar un agente Linux mediante URL base y token
- verificar conectividad y disponibilidad del backend con `GET /api/v1/status`
- enviar una petición HTTP autenticada al endpoint de apagado del agente
- mostrar la acción `Apagar` solo cuando la configuración esté lista
- configurar apagado remoto por SSH con:
  - host
  - puerto
  - usuario
  - clave privada
  - fingerprint del host
  - passphrase opcional
  - comando remoto configurable
- verificar conectividad SSH y autenticación antes del apagado real
- ejecutar un comando de apagado remoto no interactivo, con valor por defecto `sudo -n systemctl poweroff`

Fases posteriores previstas:

- otras acciones de energía si aportan valor real

Contrato mínimo actual del agente Linux:

- `GET /api/v1/status`
- `POST /api/v1/poweroff`
- `Authorization: Bearer <token>`
- diferenciación de errores útil en UI para token inválido, ruta incorrecta, backend no disponible y problemas de red

Contrato mínimo previsto para SSH:

- autenticación por clave privada, no por password
- verificación explícita del fingerprint del host
- comando remoto configurable por dispositivo
- para Linux de escritorio y TrueNAS SCALE, la ruta soportada por la app asume que el host remoto ya está preparado para ejecutar el comando sin prompt interactivo

### 3.4 App Actions

La app debe incluir:

- `shortcuts.xml`
- `meta-data` en manifest para shortcuts
- una `WakeDeviceActivity` headless exportada
- extracción tolerante de parámetro de dispositivo
- publicación de shortcuts dinámicos para equipos guardados

Estado deseado:

- soporte práctico para invocación desde Gemini en Android mediante la infraestructura oficial disponible para App Actions

### 3.5 Internacionalización

- idioma base: español
- idioma secundario: inglés

## 4. Arquitectura

```text
app/
  actions/
  data/local/
  data/repository/
  domain/model/
  domain/repository/
  domain/wol/
  ui/
  util/
```

### 4.1 Capas

- `domain`: modelos y lógica WOL
- `domain`: modelos y lógica WOL y de apagado remoto
- `data`: Room y repositorio
- `ui`: Compose, estado de pantalla y tema
- `actions`: fulfillment headless para App Actions

## 5. Diseño de experiencia

- estructura inspirada en el prototipo anterior
- visual adaptada a Material 3 moderno
- dashboard prioritario
- feedback claro de éxito y error
- formularios con validación inmediata

## 6. Riesgos conocidos

- La funcionalidad de App Actions para este caso depende de límites actuales de Assistant/App Actions.
- No existe documentación oficial encontrada para un built-in intent específico `WAKE_DEVICE`.
- Los custom intents documentados oficialmente están limitados a `en-US`.

## 7. Criterios de aceptación

- CRUD de dispositivos operativo
- persistencia local real
- envío WOL real desde UI
- fulfillment interno operativo por intent
- base App Actions integrada
- recursos bilingües definidos
