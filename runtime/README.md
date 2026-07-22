# Local duel runtime

This runtime replaces the fake local engine with the real EDOPro stack:

- Evolution Server v2.13.2;
- native ocgcore loaded by its CoreIntegrator process;
- Project Ignis CardScripts and BabelCDB;
- Project Ignis WindBot with its Blue-Eyes executor and 40-card deck.

Versions and checksums are pinned in versions.env. Downloaded sources and
binaries live under .local-runtime/ and are not committed.

## Commands

    ./dev.sh runtime-setup
    ./dev.sh runtime-up
    ./dev.sh runtime-status
    ./dev.sh runtime-down

To start the complete lightweight flow in the original frontend:

    ./dev.sh local-play

Open `http://localhost:5173/duel/local`. The route creates the native room,
uploads the pinned Blue-Eyes deck and launches WindBot without login.

The local runtime does not start Spring Boot, PostgreSQL, Redis, Kafka or any
authentication service.

Ports:

- 7911: EDOPro TCP protocol;
- 4001: EDOPro protocol over WebSocket for the browser adapter;
- 7922: Evolution HTTP API;
- 2399: WindBot launcher API.

The original frontend renders the native field state and shows only actions
provided by ocgcore. Card metadata and smoke-deck images are cached locally
during `runtime-setup`, so a duel does not call card-service or an image REST
endpoint.
