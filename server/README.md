# MazeSahur Multiplayer Server

Minimal WebSocket server for MazeSahur. It is server-authoritative for player movement and shares the maze seed with clients.

## Build locally

```bash
# Build runnable fat JAR (packages dependencies)
./gradlew :server:jar

# Run locally (PORT defaults to 8080)
PORT=8080 java -jar server/build/libs/server-0.0.1-SNAPSHOT.jar
```

## Protocol (JSON over WebSocket `/ws`)

- Join: `{"type":"join","room":"test","name":"Player1"}`
- Input (repeat): `{"type":"input","seq":1,"moveX":0.0,"moveZ":1.0,"yaw":90.0}`
- Server → joined: `{"type":"joined","playerId":"...","room":"test","seed":123,"players":[...] }`
- Server → state (20 Hz): `{"type":"state","ts":...,"players":[{"id":"...","name":"...","x":...,"y":...,"z":...,"yaw":...}]}`.

## Docker

Prereq: run `./gradlew :server:jar` so `server/build/libs/server-0.0.1-SNAPSHOT.jar` exists.

Build image:
```bash
docker build -t mazesahur-server:latest server
```

Run:
```bash
docker run -e PORT=8080 -p 8080:8080 mazesahur-server:latest
```

For Pterodactyl: use the image above or let Pterodactyl build from this Dockerfile; set `PORT=8080` and map 443→8080 via your reverse proxy with WebSocket upgrades enabled.
