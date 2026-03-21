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

### 3.2 Wake-on-LAN

La app debe:

- validar y normalizar la MAC
- construir un magic packet de 102 bytes
- enviarlo por UDP usando `DatagramSocket`
- permitir envío manual desde la UI
- permitir envío headless desde un intent de voz

### 3.3 App Actions

La app debe incluir:

- `shortcuts.xml`
- `meta-data` en manifest para shortcuts
- una `WakeDeviceActivity` headless exportada
- extracción tolerante de parámetro de dispositivo
- publicación de shortcuts dinámicos para equipos guardados

Estado deseado:

- soporte práctico para invocación desde Gemini en Android mediante la infraestructura oficial disponible para App Actions

### 3.4 Internacionalización

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
