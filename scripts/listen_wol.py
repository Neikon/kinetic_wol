#!/usr/bin/env python3
"""Listen for Wake-on-LAN magic packets on UDP."""

from __future__ import annotations

import argparse
import binascii
import socket
import sys
from dataclasses import dataclass
from datetime import datetime


MAC_BYTES = 6
PREAMBLE = b"\xff" * MAC_BYTES
EXPECTED_PACKET_SIZE = 6 + (16 * MAC_BYTES)


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


def main() -> int:
    args = build_parser().parse_args()

    if not (1 <= args.port <= 65535):
        print("error: el puerto debe estar entre 1 y 65535", file=sys.stderr)
        return 2

    expected_mac = args.mac
    if expected_mac:
        print(f"Filtrando por MAC objetivo: {format_mac(expected_mac)}")

    with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as sock:
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        sock.bind((args.host, args.port))
        if args.timeout is not None:
            sock.settimeout(args.timeout)

        print(f"Escuchando UDP en {args.host}:{args.port}")

        while True:
            try:
                data, sender = sock.recvfrom(4096)
            except TimeoutError:
                print("Tiempo de espera agotado sin recibir magic packets validos.")
                return 1
            except KeyboardInterrupt:
                print("\nInterrumpido por el usuario.")
                return 130

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
