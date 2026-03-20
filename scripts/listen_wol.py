#!/usr/bin/env python3
"""Listen for Wake-on-LAN magic packets on UDP."""

from __future__ import annotations

import argparse
import atexit
import binascii
import signal
import socket
import sys
from dataclasses import dataclass
from datetime import datetime
from time import monotonic


MAC_BYTES = 6
PREAMBLE = b"\xff" * MAC_BYTES
EXPECTED_PACKET_SIZE = 6 + (16 * MAC_BYTES)
POLL_INTERVAL_SECONDS = 0.5
DISABLE_KITTY_KEYBOARD_PROTOCOL = "\x1b[<u"


@dataclass(frozen=True)
class MagicPacket:
    sender_host: str
    sender_port: int
    target_mac: str
    raw_size: int


def normalize_mac(mac: str) -> str:
    normalized = mac.replace(":", "").replace("-", "").strip().upper()
    if len(normalized) != MAC_BYTES * 2:
        raise argparse.ArgumentTypeError(
            "la MAC debe tener exactamente 12 digitos hexadecimales",
        )
    try:
        int(normalized, 16)
    except ValueError as exc:
        raise argparse.ArgumentTypeError("la MAC contiene caracteres no hexadecimales") from exc
    return normalized


def format_mac(mac: str) -> str:
    return ":".join(mac[index : index + 2] for index in range(0, len(mac), 2))


def extract_magic_packet(data: bytes, sender: tuple[str, int]) -> MagicPacket | None:
    if len(data) < EXPECTED_PACKET_SIZE:
        return None
    if data[:MAC_BYTES] != PREAMBLE:
        return None

    target = data[MAC_BYTES : MAC_BYTES + MAC_BYTES]
    repeated_target = target * 16
    if data[MAC_BYTES : MAC_BYTES + len(repeated_target)] != repeated_target:
        return None

    target_mac = binascii.hexlify(target).decode("ascii").upper()
    return MagicPacket(
        sender_host=sender[0],
        sender_port=sender[1],
        target_mac=target_mac,
        raw_size=len(data),
    )


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Escucha paquetes Wake-on-LAN por UDP y muestra la MAC objetivo.",
    )
    parser.add_argument(
        "--host",
        default="0.0.0.0",
        help="interfaz de escucha. Por defecto: 0.0.0.0",
    )
    parser.add_argument(
        "--port",
        type=int,
        default=9,
        help="puerto UDP de escucha. Por defecto: 9",
    )
    parser.add_argument(
        "--mac",
        type=normalize_mac,
        help="si se indica, solo informa de paquetes dirigidos a esa MAC",
    )
    parser.add_argument(
        "--timeout",
        type=float,
        default=None,
        help="segundos maximos de espera antes de salir",
    )
    parser.add_argument(
        "--once",
        action="store_true",
        help="salir tras detectar el primer magic packet valido",
    )
    return parser


def disable_extended_keyboard_protocol() -> None:
    if not sys.stdout.isatty():
        return
    sys.stdout.write(DISABLE_KITTY_KEYBOARD_PROTOCOL)
    sys.stdout.flush()


def main() -> int:
    disable_extended_keyboard_protocol()
    atexit.register(disable_extended_keyboard_protocol)

    args = build_parser().parse_args()

    if not (1 <= args.port <= 65535):
        print("error: el puerto debe estar entre 1 y 65535", file=sys.stderr)
        return 2

    expected_mac = args.mac
    if expected_mac:
        print(f"Filtrando por MAC objetivo: {format_mac(expected_mac)}")

    stop_requested = False

    def handle_stop(_signum: int, _frame: object) -> None:
        nonlocal stop_requested
        stop_requested = True

    signal.signal(signal.SIGINT, handle_stop)
    signal.signal(signal.SIGTERM, handle_stop)

    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    except PermissionError:
        print(
            "error: el sistema ha denegado la creacion del socket UDP.",
            file=sys.stderr,
        )
        print(
            "Prueba a ejecutar fuera de un sandbox restrictivo o usa tu terminal normal.",
            file=sys.stderr,
        )
        return 13

    with sock:
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        try:
            sock.bind((args.host, args.port))
        except PermissionError:
            print(
                "error: no tienes permisos para escuchar en ese puerto.",
                file=sys.stderr,
            )
            print(
                "En Linux, los puertos inferiores a 1024 requieren privilegios.",
                file=sys.stderr,
            )
            print(
                "Opciones recomendadas:",
                file=sys.stderr,
            )
            print(
                f"- usar un puerto alto, por ejemplo: python3 scripts/listen_wol.py --port 4009",
                file=sys.stderr,
            )
            print(
                "- configurar ese mismo puerto en el dispositivo dentro de la app",
                file=sys.stderr,
            )
            print(
                "- o ejecutar el script con privilegios si de verdad necesitas el puerto 9",
                file=sys.stderr,
            )
            return 13
        if args.timeout is not None:
            deadline = monotonic() + args.timeout
        else:
            deadline = None

        print(f"Escuchando UDP en {args.host}:{args.port}")

        while True:
            if stop_requested:
                print("\nInterrumpido por el usuario.")
                return 130

            if deadline is not None:
                remaining = deadline - monotonic()
                if remaining <= 0:
                    print("Tiempo de espera agotado sin recibir magic packets validos.")
                    return 1
                sock.settimeout(min(POLL_INTERVAL_SECONDS, remaining))
            else:
                sock.settimeout(POLL_INTERVAL_SECONDS)

            try:
                data, sender = sock.recvfrom(4096)
            except socket.timeout:
                continue

            packet = extract_magic_packet(data, sender)
            if packet is None:
                continue
            if expected_mac and packet.target_mac != expected_mac:
                continue

            timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
            print(
                f"[{timestamp}] magic packet valido desde "
                f"{packet.sender_host}:{packet.sender_port} -> "
                f"{format_mac(packet.target_mac)} ({packet.raw_size} bytes)",
            )

            if args.once:
                return 0


if __name__ == "__main__":
    raise SystemExit(main())
