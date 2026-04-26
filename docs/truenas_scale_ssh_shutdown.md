# Configurar apagado remoto por SSH en TrueNAS SCALE desde la Web UI

Esta guía explica cómo preparar TrueNAS SCALE para que Kinetic WOL pueda apagar el servidor por SSH usando una clave generada en la app.

La configuración se hace desde la Web UI de TrueNAS, sin editar archivos manualmente.

## 1. Requisitos

- TrueNAS SCALE accesible desde el teléfono por IP o DNS.
- Acceso de administrador a la Web UI de TrueNAS.
- Kinetic WOL instalado en Android.
- Un usuario de TrueNAS que pueda acceder por SSH.

Comando validado para TrueNAS SCALE:

```bash
sudo -n /usr/sbin/shutdown -h now
```

En la Web UI de TrueNAS se autoriza el ejecutable:

```text
/usr/sbin/shutdown
```

La app añade los argumentos `-h now` en el comando remoto.

## 2. Generar la clave SSH en Kinetic WOL

En la app:

1. Crea o edita el dispositivo de TrueNAS.
2. Activa `Apagado remoto`.
3. Selecciona el método `SSH`.
4. Pulsa `Generar nueva clave SSH`.
5. Copia la clave pública.

La clave pública debe empezar por algo parecido a:

```text
ssh-rsa AAAA... kinetic-wol@android
```

No copies la clave privada a TrueNAS. La clave privada se queda guardada en Kinetic WOL.

## 3. Preparar el usuario en TrueNAS

En la Web UI de TrueNAS SCALE:

1. Entra en `Credentials > Users`.
2. Edita un usuario existente o crea uno nuevo con `Add`.
3. Usa un usuario no-root.
4. Activa `SSH Access`.
5. Pega la clave pública de Kinetic WOL en el campo `Public SSH Key`.

En versiones o layouts antiguos de SCALE, la pantalla puede aparecer como `Credentials > Local Users`.

TrueNAS selecciona `Shell Access` cuando activas `SSH Access`. Si la UI muestra una opción de shell, usa una shell válida como `sh` o `bash`, no `nologin`.

## 4. Permitir solo el comando de apagado sin contraseña

En la misma pantalla del usuario, busca las opciones de `sudo`.

Configura:

- `Allowed sudo commands with no password`: añade `/usr/sbin/shutdown`

No actives `Allow all sudo commands with no password` salvo que realmente quieras dar permisos mucho más amplios. Para Kinetic WOL basta con permitir el ejecutable de apagado.

Guarda los cambios del usuario.

## 5. Activar el servicio SSH

En la Web UI de TrueNAS SCALE:

1. Ve a `System > Services`.
2. Busca `SSH`.
3. Entra en la configuración del servicio si quieres revisar el puerto.
4. Usa el puerto `22`, salvo que tengas otro puerto definido.
5. Guarda.
6. Activa el servicio SSH.
7. Activa `Start Automatically` si quieres que SSH vuelva a arrancar tras reiniciar TrueNAS.

Si solo vas a usar autenticación por clave, no necesitas habilitar login por contraseña.

## 6. Configurar el dispositivo en Kinetic WOL

En Kinetic WOL, dentro del dispositivo de TrueNAS:

- Método: `SSH`
- Host SSH: IP o DNS de TrueNAS
- Puerto SSH: `22`, o el puerto configurado en TrueNAS
- Usuario SSH: el usuario configurado en `Credentials > Users`
- Clave privada: la generada por Kinetic WOL
- Fingerprint del host: déjalo vacío para la primera prueba
- Comando:

```bash
sudo -n /usr/sbin/shutdown -h now
```

Pulsa `Probar conexión`.

Si la prueba funciona, Kinetic WOL guardará el fingerprint del host automáticamente.

## 7. Probar el apagado

Después de una prueba SSH correcta:

1. Guarda el dispositivo.
2. Vuelve al dashboard.
3. Pulsa `Apagar`.

TrueNAS debería iniciar el apagado.

## 8. Problemas frecuentes

### La app no conecta por SSH

- Revisa que el servicio `SSH` está activo en `System > Services`.
- Revisa el puerto configurado.
- Revisa que el teléfono puede llegar a la IP de TrueNAS.
- Revisa que el usuario tiene `SSH Access`.

### Autenticación fallida

- Revisa que pegaste la clave pública completa en `Public SSH Key`.
- Revisa que en Kinetic WOL usas el mismo usuario de TrueNAS.
- Si regeneraste la clave en Kinetic WOL, vuelve a copiar la nueva clave pública a TrueNAS.

### La prueba conecta, pero apagar falla

Revisa la configuración de `sudo` del usuario.

La Web UI debe permitir este ejecutable sin contraseña:

```text
/usr/sbin/shutdown
```

Y Kinetic WOL debe ejecutar:

```bash
sudo -n /usr/sbin/shutdown -h now
```

### Fingerprint del host no coincide

Puede pasar si reinstalaste TrueNAS, cambiaste la configuración SSH o estás apuntando a otra máquina.

Si el cambio es esperado:

1. Borra el fingerprint guardado en Kinetic WOL.
2. Pulsa `Probar conexión` otra vez.
3. Guarda el dispositivo.

## 9. Nota de seguridad

SSH en un NAS es una capacidad sensible. TrueNAS recomienda no habilitar SSH salvo que sea necesario.

Para reducir riesgo:

- usa clave pública en vez de contraseña
- usa un usuario no-root
- permite solo `/usr/sbin/shutdown` sin contraseña
- no expongas SSH a Internet si no es imprescindible
- desactiva SSH si ya no lo necesitas

## Referencias

- [TrueNAS SCALE: SSH service](https://www.truenas.com/docs/scale/25.04/scaletutorials/systemsettings/services/sshservicescale/)
- [TrueNAS SCALE: Managing users](https://www.truenas.com/docs/scale/credentials/users/manageusers/)
