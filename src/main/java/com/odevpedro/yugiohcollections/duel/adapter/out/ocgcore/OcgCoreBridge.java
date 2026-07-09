package com.odevpedro.yugiohcollections.duel.adapter.out.ocgcore;

/**
 * Declarações dos métodos nativos que mapeiam para o ocgcore C++.
 * A implementação está em native/ocgcore-bridge/ (libocgcore.so).
 *
 * O carregamento da biblioteca é feito pelo OcgCoreLoader no startup.
 *
 * Contrato com o C++:
 *   - processAction  recebe o estado serializado em JSON + ação, devolve novo estado JSON
 *   - advancePhase   recebe o estado serializado, devolve novo estado JSON
 *   - isActionValid  recebe estado + ação, devolve boolean
 */
public class OcgCoreBridge {

    public native String processAction(String stateJson, String actionJson, String playerId);

    public native String advancePhase(String stateJson);

    public native boolean isActionValid(String stateJson, String actionJson, String playerId);
}
