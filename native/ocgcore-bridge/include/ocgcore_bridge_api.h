#ifndef OCG_CORE_BRIDGE_API_H
#define OCG_CORE_BRIDGE_API_H

#include <stdbool.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

/*
 * API esperada da biblioteca ygopro-core (edo9300/ygopro-core).
 *
 * O JNI bridge (ocgcore_bridge.cpp) chama estas funcoes para delegar
 * o processamento do jogo ao motor C++ real.
 *
 * Cada funcao recebe JSON e retorna JSON.
 * Strings retornadas devem ser liberadas com ocgcore_bridge_free_string().
 */

typedef struct ocgcore_duel ocgcore_duel_t;

/*
 * Cria uma instancia de duelo a partir do estado JSON.
 * Retorna NULL em caso de erro.
 */
ocgcore_duel_t* ocgcore_bridge_create(const char* state_json);

/*
 * Processa uma acao no duelo.
 * state_json: estado completo do duelo em JSON
 * action_json: acao a ser processada em JSON
 * player_id: ID do jogador que executa a acao
 * Retorna JSON com o estado atualizado.
 * Chamador deve liberar com ocgcore_bridge_free_string().
 */
char* ocgcore_bridge_process_action(
    ocgcore_duel_t* duel,
    const char* state_json,
    const char* action_json,
    const char* player_id
);

/*
 * Avanca para a proxima fase.
 * state_json: estado completo do duelo em JSON
 * Retorna JSON com o estado atualizado.
 * Chamador deve liberar com ocgcore_bridge_free_string().
 */
char* ocgcore_bridge_advance_phase(
    ocgcore_duel_t* duel,
    const char* state_json
);

/*
 * Verifica se uma acao e valida no estado atual.
 * state_json: estado completo do duelo em JSON
 * action_json: acao a ser validada em JSON
 * player_id: ID do jogador
 * Retorna true se a acao for valida.
 */
bool ocgcore_bridge_is_action_valid(
    ocgcore_duel_t* duel,
    const char* state_json,
    const char* action_json,
    const char* player_id
);

/*
 * Libera uma string retornada por ocgcore_bridge_*.
 */
void ocgcore_bridge_free_string(char* str);

/*
 * Destroi uma instancia de duelo.
 */
void ocgcore_bridge_destroy(ocgcore_duel_t* duel);

#ifdef __cplusplus
}
#endif

#endif /* OCG_CORE_BRIDGE_API_H */
