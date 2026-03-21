# Scripts

## `listen_wol.py`

Escucha paquetes Wake-on-LAN en UDP y muestra la MAC objetivo cuando detecta un magic packet valido.

Ejemplos:

```bash
python3 scripts/listen_wol.py --port 9
python3 scripts/listen_wol.py --port 9 --once
python3 scripts/listen_wol.py --port 9 --mac AA:BB:CC:DD:EE:FF --once
python3 scripts/listen_wol.py --port 9 --timeout 15
```

Nota:

- en Linux, el puerto `9` requiere privilegios por ser menor que `1024`
- para pruebas normales es mas practico usar un puerto alto, por ejemplo `4009`
- si usas `4009`, configura tambien `4009` como puerto del dispositivo en la app

Ejemplo recomendado de prueba local:

```bash
python3 scripts/listen_wol.py --port 4009 --once
```

Para escucha continua, sal con `Ctrl+C`.

Si tu terminal escribe secuencias raras como `^[[99;5u` al pulsar `Ctrl+C`, se ha quedado activo un modo de teclado extendido. Puedes restaurarlo con:

```bash
printf '\e[<u'
```
