package com.odevpedro.yugiohcollections.duel.adapter.out.ocgcore;

/**
 * Declarações dos métodos nativos que mapeiam para o ocgcore C++.
 * A implementação real está em ocgcore.dll (Windows) / ocgcore.so (Linux).
 *
 * Contrato com o C++:
 *   - processAction  recebe o estado serializado em JSON + ação, devolve novo estado JSON
 *   - advancePhase   recebe o estado serializado, devolve novo estado JSON
 *   - isActionValid  recebe estado + ação, devolve boolean
 */
public class OcgCoreBridge {

    static {
        // carregado pelo OcgCoreLoader no startup — não chamar System.load aqui
    }

    public native String processAction(String stateJson, String actionJson, String playerId);

    public native String advancePhase(String stateJson);

    public native boolean isActionValid(String stateJson, String actionJson, String playerId);
}
