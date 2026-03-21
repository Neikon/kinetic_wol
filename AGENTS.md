# Agents Index

Este repositorio usa estos documentos como memoria operativa persistente. Antes de continuar el desarrollo, léelos en este orden:

1. [docs/agent_memory.md](docs/agent_memory.md)
2. [docs/app_spec.md](docs/app_spec.md)
3. [docs/roadmap.md](docs/roadmap.md)

Reglas de uso:

- `agent_memory.md`: contexto acordado con el usuario y decisiones activas.
- `app_spec.md`: definición funcional y técnica detallada de Kinetic WOL.
- `roadmap.md`: histórico de trabajo, estado actual y próximos pasos.

Cuando se cierre una sesión:

- actualiza `agent_memory.md` con nuevas decisiones o riesgos
- actualiza `roadmap.md` con lo terminado y lo pendiente
- ajusta `app_spec.md` si cambia el alcance del producto

Directrices de versionado:

- crea un commit en cada progreso reseñable, no solo al final
- haz `push` tras cada commit relevante para dejar el estado respaldado en remoto
- puedes crear ramas nuevas, fusionarlas o reorganizar el trabajo si eso mejora la limpieza del historial
- prioriza un historial claro y reversible para poder rastrear cambios y revertir decisiones si hace falta
