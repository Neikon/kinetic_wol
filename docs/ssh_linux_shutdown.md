# Configurar apagado remoto por SSH en Linux

Esta guía explica cómo preparar un equipo Linux para que Kinetic WOL pueda apagarlo por SSH usando clave privada y un comando `sudo` no interactivo.

## 1. Requisitos

- El equipo Linux debe estar en la misma red o ser accesible por IP/DNS desde el teléfono.
- El servicio SSH debe estar instalado y activo.
- Debes tener un usuario Linux para la conexión SSH.
- Debes poder ejecutar comandos con `sudo` en ese equipo para preparar `sudoers`.

En esta guía se usa este comando de apagado:

```bash
sudo -n /usr/bin/systemctl poweroff
```

El `-n` es importante: hace que `sudo` falle si necesita contraseña, en vez de quedarse esperando un prompt que la app no puede responder.

## 2. Comprobar o instalar SSH

En el equipo Linux, comprueba si SSH está activo:

```bash
systemctl status ssh
```

En algunas distribuciones el servicio se llama `sshd`:

```bash
systemctl status sshd
```

Si no está instalado, en Debian/Ubuntu:

```bash
sudo apt update
sudo apt install openssh-server
sudo systemctl enable --now ssh
```

En Fedora/Bazzite:

```bash
sudo systemctl enable --now sshd
```

Comprueba la IP del equipo:

```bash
hostname -I
```

Apunta la IP que usarás como `Host SSH` en Kinetic WOL.

## 3. Generar la clave en Kinetic WOL

En la app:

1. Abre o crea el dispositivo Linux.
2. Activa `Apagado remoto`.
3. Selecciona el método `SSH`.
4. Pulsa `Generar nueva clave SSH`.
5. Copia la clave pública que muestra la app.

La clave pública debe parecerse a esto:

```text
ssh-rsa AAAA... kinetic-wol@android
```

No copies la clave privada al equipo Linux. La clave privada se queda en la app.

## 4. Autorizar la clave pública en Linux

En el equipo Linux, entra con el usuario que vas a configurar en Kinetic WOL.

Prepara el directorio SSH:

```bash
mkdir -p ~/.ssh
chmod 700 ~/.ssh
```

Edita `authorized_keys`. Si prefieres `nano`:

```bash
nano ~/.ssh/authorized_keys
```

Pega la clave pública completa en una línea nueva. Guarda con:

```text
Ctrl+O
Enter
Ctrl+X
```

Aplica permisos:

```bash
chmod 600 ~/.ssh/authorized_keys
```

## 5. Configurar sudoers para apagado sin contraseña

Kinetic WOL ejecuta un comando remoto por SSH. Para apagar sin interacción, el usuario remoto debe poder ejecutar el comando exacto sin contraseña.

Primero localiza `systemctl`:

```bash
command -v systemctl
```

Normalmente devolverá:

```text
/usr/bin/systemctl
```

Edita `sudoers` con `visudo` usando `nano`:

```bash
sudo EDITOR=nano visudo
```

Añade al final una línea como esta, cambiando `TU_USUARIO` por el usuario Linux real:

```text
TU_USUARIO ALL=(root) NOPASSWD: /usr/bin/systemctl poweroff
```

Guarda con:

```text
Ctrl+O
Enter
Ctrl+X
```

No edites `/etc/sudoers` directamente con un editor normal. `visudo` valida la sintaxis antes de guardar y evita dejar sudo roto.

## 6. Configurar Kinetic WOL

En el dispositivo dentro de Kinetic WOL:

- Método: `SSH`
- Host SSH: IP o nombre DNS del equipo Linux
- Puerto SSH: `22`, salvo que hayas cambiado el puerto del servidor
- Usuario SSH: el usuario donde pegaste la clave pública
- Clave privada: la clave privada generada por la app
- Fingerprint del host: déjalo vacío en la primera prueba
- Comando:

```bash
sudo -n /usr/bin/systemctl poweroff
```

Pulsa `Probar conexión`.

Si todo está correcto, la app conectará por SSH, ejecutará un comando de prueba inocuo y guardará automáticamente el fingerprint del host en la primera prueba.

## 7. Probar el apagado

Después de una prueba SSH correcta:

1. Guarda el dispositivo.
2. Vuelve al dashboard.
3. Pulsa `Apagar` en el dispositivo.

El equipo debería apagarse.

## 8. Problemas frecuentes

### Host no resoluble o no accesible

- Revisa la IP configurada.
- Comprueba que el teléfono y el equipo están en la misma red.
- Comprueba que SSH está activo:

```bash
systemctl status ssh
systemctl status sshd
```

### Puerto cerrado o conexión rechazada

- Revisa el puerto SSH.
- Revisa el firewall del equipo Linux.
- En Ubuntu/Debian con UFW:

```bash
sudo ufw allow ssh
```

### Autenticación fallida

- Revisa que la clave pública está en `~/.ssh/authorized_keys` del usuario correcto.
- Revisa permisos:

```bash
chmod 700 ~/.ssh
chmod 600 ~/.ssh/authorized_keys
```

- Revisa que en Kinetic WOL estás usando el mismo usuario.

### Fingerprint del host no coincide

Esto puede pasar si reinstalaste el sistema, cambiaste el servidor SSH o estás apuntando a otra máquina con la misma IP.

Si el cambio es esperado, borra el fingerprint guardado en Kinetic WOL y vuelve a pulsar `Probar conexión` para capturarlo de nuevo.

### La prueba SSH funciona, pero apagar falla

El problema suele estar en `sudoers`.

Revisa que el comando configurado en la app coincide exactamente con el permitido en `sudoers`.

Para este comando en la app:

```bash
sudo -n /usr/bin/systemctl poweroff
```

La línea de `sudoers` debe permitir:

```text
TU_USUARIO ALL=(root) NOPASSWD: /usr/bin/systemctl poweroff
```

## 9. Nota de seguridad

La regla anterior permite a ese usuario apagar el equipo sin contraseña. Es una concesión limitada si solo permites el comando exacto de apagado, pero sigue siendo una capacidad sensible.

Usa un usuario dedicado si quieres aislar mejor este acceso.
