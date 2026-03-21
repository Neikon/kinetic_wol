# Internal Testing Release

Guía mínima para preparar una release interna de `Kinetic WOL` en Google Play sin publicar la app en producción.

## Objetivo

Usar una `internal testing release` para validar la integración real de App Actions y los shortcuts del sistema en un dispositivo asociado a tu cuenta.

## 1. Requisitos

- Android Studio con build correcta de la app
- cuenta de Google Play Console
- clave de firma de release o aceptación de App Signing de Google Play
- una cuenta Google añadida como tester interno

## 2. Preparar la app localmente

Comprueba en Android Studio:

- `applicationId`: `dev.neikon.kineticwol`
- nombre visible: `Kinetic WOL`
- la build `release` compila sin errores

## 3. Generar el App Bundle

En Android Studio:

1. `Build > Generate Signed Bundle / APK`
2. elige `Android App Bundle`
3. crea o selecciona el `keystore`
4. usa la variante `release`
5. genera el `.aab`

Resultado esperado:

- un fichero tipo `app-release.aab`

## 4. Crear la app en Play Console

En Play Console:

1. crea una aplicación nueva
2. nombre: `Kinetic WOL`
3. idioma predeterminado: español
4. completa lo mínimo exigido por Play Console para permitir un track interno

## 5. Crear el track interno

En Play Console:

1. entra en `Testing > Internal testing`
2. crea una release nueva
3. sube el `.aab`
4. añade tu cuenta como tester
5. publica la release interna

## 6. Instalar la release de testing

Desde el enlace de tester:

1. acepta participar en el test
2. instala la app desde Google Play en el móvil

## 7. Validar App Actions

Pruebas recomendadas:

- comprobar que al mantener pulsado el icono aparecen shortcuts del sistema
- probar frases de invocación con el nombre real `Kinetic WOL`
- si no se resuelve por voz, seguir usando validación por `adb` mientras ajustamos la integración

## 8. Notas importantes

- No hace falta publicar en producción.
- `Internal testing` es suficiente para validar la integración real con Play.
- La lógica headless ya está validada localmente por `adb`.
- El punto de riesgo restante es la disponibilidad real de App Actions/Gemini en tu cuenta/dispositivo.

## 9. Checklist

- [ ] generar `AAB` firmado
- [ ] crear app en Play Console
- [ ] crear `internal testing release`
- [ ] instalar desde Play
- [ ] comprobar shortcuts
- [ ] probar invocación real
