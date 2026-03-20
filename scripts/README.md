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
