FROM node:24.11.0-bullseye-slim AS core-builder

RUN apt-get update && apt-get install -y --no-install-recommends \
    g++ make cmake pkg-config git ca-certificates \
    libboost-system-dev libsqlite3-dev libjsoncpp-dev \
    nlohmann-json3-dev libcurl4-openssl-dev \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /build/core
COPY .local-runtime/evolution-server/core/ ./
RUN cmake -B build -S . -DCMAKE_BUILD_TYPE=Release \
    && cmake --build build --parallel

FROM node:24.11.0-bullseye AS server-builder

ARG EVOLUTION_TYPES_COMMIT
WORKDIR /build/server

COPY .local-runtime/evolution-server/package.json .local-runtime/evolution-server/package-lock.json ./
RUN HUSKY=0 npm ci

COPY .local-runtime/evolution-server/ ./
COPY runtime/evolution/patches/ /tmp/evolution-patches/
RUN git apply --unidiff-zero /tmp/evolution-patches/*.patch
RUN git clone --no-checkout --filter=blob:none \
      https://github.com/diangogav/evolution-types.git src/evolution-types \
    && git -C src/evolution-types fetch --depth 1 origin "$EVOLUTION_TYPES_COMMIT" \
    && git -C src/evolution-types checkout --detach "$EVOLUTION_TYPES_COMMIT" \
    && test "$(git -C src/evolution-types rev-parse HEAD)" = "$EVOLUTION_TYPES_COMMIT" \
    && npm run build \
    && npm prune --omit=dev

FROM node:24.11.0-bullseye-slim

RUN apt-get update && apt-get install -y --no-install-recommends \
    dumb-init liblua5.3-0 libsqlite3-0 libevent-2.1-7 \
    && rm -rf /var/lib/apt/lists/*
RUN apt-get update && apt-get install -y --no-install-recommends libboost-system1.74.0 libjsoncpp24 libcurl4 && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=server-builder /build/server/dist ./
COPY --from=server-builder /build/server/package.json ./package.json
COPY --from=server-builder /build/server/node_modules ./node_modules
COPY --from=core-builder /build/core/libocgcore.so ./core/libocgcore.so
COPY --from=core-builder /build/core/CoreIntegrator ./core/CoreIntegrator

ENTRYPOINT ["dumb-init", "--"]
CMD ["node", "./src/index.js"]
