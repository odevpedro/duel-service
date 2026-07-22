#include <cstdint>
#include <cstring>
#include <string>
#include <vector>
#include <cstdio>
#include <mutex>
#include <unordered_map>
#include <unistd.h>
#include <fcntl.h>
#include <sqlite3.h>

#define LOG_FILE "/tmp/ocgcore_bridge.log"
#define DEBUG(...) do { \
    int fd = open(LOG_FILE, O_WRONLY | O_CREAT | O_APPEND, 0644); \
    if (fd >= 0) { \
        char buf[4096]; int n = snprintf(buf, sizeof(buf), __VA_ARGS__); \
        if (n > 0) write(fd, buf, n < (int)sizeof(buf) ? n : (int)sizeof(buf)); \
        close(fd); \
    } \
} while(0)
#include <jni.h>
#include <nlohmann/json.hpp>

#include "ocgapi.h"
#include "ocgapi_types.h"
#include "ocgapi_constants.h"

using json = nlohmann::json;

static std::mutex g_mutex;
static std::unordered_map<std::string, OCG_Duel> g_active_duels;
static std::unordered_map<std::string, void*> g_card_dbs;

struct CardInfo {
    uint32_t code;
    uint32_t type;
    uint32_t level;
    uint64_t race;
    uint32_t attribute;
    int32_t attack;
    int32_t defense;
};

static sqlite3* g_card_db = nullptr;
static std::unordered_map<uint32_t, CardInfo> g_card_cache;
static std::mutex g_cache_mutex;

static void init_card_db(const char* db_path) {
    std::lock_guard<std::mutex> lock(g_cache_mutex);
    if (g_card_db) return;
    if (sqlite3_open(db_path ? db_path : "cards.cdb", &g_card_db) != SQLITE_OK) {
        DEBUG("[ocgcore] Failed to open cards.cdb: %s\n", sqlite3_errmsg(g_card_db));
        g_card_db = nullptr;
        return;
    }
    DEBUG("[ocgcore] Loaded cards.cdb from %s\n", db_path ? db_path : "cards.cdb");
}

static CardInfo* lookup_card(uint32_t code) {
    std::lock_guard<std::mutex> lock(g_cache_mutex);
    auto it = g_card_cache.find(code);
    if (it != g_card_cache.end()) return &it->second;
    if (!g_card_db) return nullptr;

    sqlite3_stmt* stmt = nullptr;
    const char* sql = "SELECT type, level, race, attribute, atk, def FROM datas WHERE id = ?";
    if (sqlite3_prepare_v2(g_card_db, sql, -1, &stmt, nullptr) != SQLITE_OK) return nullptr;
    sqlite3_bind_int(stmt, 1, (int)code);
    if (sqlite3_step(stmt) == SQLITE_ROW) {
        CardInfo info{};
        info.code = code;
        info.type = (uint32_t)sqlite3_column_int(stmt, 0);
        info.level = (uint32_t)sqlite3_column_int(stmt, 1);
        info.race = (uint64_t)sqlite3_column_int64(stmt, 2);
        info.attribute = (uint32_t)sqlite3_column_int(stmt, 3);
        info.attack = sqlite3_column_int(stmt, 4);
        info.defense = sqlite3_column_int(stmt, 5);
        sqlite3_finalize(stmt);
        auto result = g_card_cache.emplace(code, info);
        return &result.first->second;
    }
    sqlite3_finalize(stmt);
    return nullptr;
}

struct EngineResult {
    uint32_t turn;
    uint32_t phase;
    uint8_t turn_player;
    int32_t lp[2];
    bool game_over;
    uint8_t winner_player;
    uint8_t win_reason;
    bool init_success;
    std::string error_msg;
    json field_data;
    json card_data;
};

static void card_reader(void* payload, uint32_t code, OCG_CardData* data) {
    CardInfo* ci = lookup_card(code);
    if (ci) {
        std::memset(data, 0, sizeof(*data));
        data->code = code;
        data->type = ci->type;
        data->level = ci->level;
        data->race = ci->race;
        data->attribute = ci->attribute;
        data->attack = ci->attack;
        data->defense = ci->defense;
        return;
    }
    auto& db = *static_cast<std::unordered_map<uint32_t, OCG_CardData>*>(payload);
    auto it = db.find(code);
    if (it != db.end()) {
        *data = it->second;
    } else {
        std::memset(data, 0, sizeof(*data));
        data->code = code;
        data->type = TYPE_MONSTER | TYPE_NORMAL;
        data->level = 4;
        data->race = RACE_WARRIOR;
        data->attribute = ATTRIBUTE_EARTH;
    }
}

static int script_reader(void*, OCG_Duel, const char*) { return 0; }
static void log_handler(void*, const char* str, int) { DEBUG("[ocgcore] %s\n", str); }

static void card_reader_done(void*, OCG_CardData*) {}

static uint32_t rd32(const uint8_t*& p) {
    uint32_t v; std::memcpy(&v, p, 4); p += 4; return v;
}
static uint8_t rd8(const uint8_t*& p) { return *p++; }
static uint64_t rd64(const uint8_t*& p) {
    uint64_t v; std::memcpy(&v, p, 8); p += 8; return v;
}

static uint16_t rd16(const uint8_t*& p) {
    uint16_t v; std::memcpy(&v, p, 2); p += 2; return v;
}

static json query_location_codes(OCG_Duel duel, uint8_t con, uint32_t loc, uint32_t flags) {
    OCG_QueryInfo info{};
    info.flags = flags;
    info.con = con;
    info.loc = loc;

    uint32_t len = 0;
    void* data = OCG_DuelQueryLocation(duel, &len, &info);
    if (!data || len == 0) return json::array();

    const uint8_t* p = static_cast<const uint8_t*>(data);
    const uint8_t* end = p + len;

    if (p + 4 > end) return json::array();
    uint32_t total_size;
    std::memcpy(&total_size, p, 4);
    p += 4;

    json result = json::array();
    while (p < end) {
        uint32_t code_val = 0;
        uint32_t type_val = 0;
        uint32_t pos_val = 0;
        uint32_t lv_val = 0;
        int32_t atk_val = 0;
        int32_t def_val = 0;
        bool have_code = false;
        bool have_type = false;
        bool have_pos = false;

        bool is_null_entry = false;
        if (p + 2 <= end) {
            uint16_t maybe_null;
            std::memcpy(&maybe_null, p, 2);
            if (maybe_null == 0) {
                is_null_entry = true;
            }
        }

        if (is_null_entry) {
            result.push_back(nullptr);
            p += 2;
            continue;
        }

        while (p < end) {
            if (p + 2 > end) break;
            uint16_t esize = rd16(p);
            if (p + esize > end) break;

            if (esize >= 4) {
                uint32_t etype;
                std::memcpy(&etype, p, 4);

                if (etype == QUERY_CODE && esize >= 8) {
                    std::memcpy(&code_val, p + 4, 4);
                    have_code = true;
                } else if (etype == QUERY_TYPE && esize >= 8) {
                    std::memcpy(&type_val, p + 4, 4);
                    have_type = true;
                } else if (etype == QUERY_POSITION && esize >= 8) {
                    std::memcpy(&pos_val, p + 4, 4);
                    have_pos = true;
                } else if (etype == QUERY_LEVEL && esize >= 8) {
                    std::memcpy(&lv_val, p + 4, 4);
                } else if (etype == QUERY_ATTACK && esize >= 8) {
                    std::memcpy(&atk_val, p + 4, 4);
                } else if (etype == QUERY_DEFENSE && esize >= 8) {
                    std::memcpy(&def_val, p + 4, 4);
                } else if (etype == QUERY_END) {
                    p += esize;
                    break;
                }
            }
            p += esize;
        }

        if (have_code) {
            json card_json;
            card_json["code"] = code_val;
            if (have_type) card_json["type"] = type_val;
            if (have_pos) card_json["position"] = pos_val;
            card_json["level"] = lv_val;
            card_json["atk"] = atk_val;
            card_json["def"] = def_val;
            result.push_back(card_json);
        } else {
            result.push_back(nullptr);
        }
    }

    return result;
}

static json query_zone_codes(OCG_Duel duel, uint8_t con, uint32_t loc, int zone_count) {
    json result = json::array();
    for (int z = 0; z < zone_count; ++z) {
        OCG_QueryInfo info{};
        info.flags = QUERY_CODE | QUERY_TYPE | QUERY_POSITION;
        info.con = con;
        info.loc = loc;
        info.seq = z;

        uint32_t len = 0;
        void* data = OCG_DuelQuery(duel, &len, &info);
        if (!data || len == 0) {
            result.push_back(nullptr);
            continue;
        }

        const uint8_t* p = static_cast<const uint8_t*>(data);
        const uint8_t* end = p + len;

        uint32_t code_val = 0;
        uint32_t type_val = 0;
        uint32_t pos_val = 0;
        uint32_t lv_val = 0;
        int32_t atk_val = 0;
        int32_t def_val = 0;
        bool have_code = false;
        bool have_type = false;
        bool have_pos = false;

        while (p < end) {
            if (p + 2 > end) break;
            uint16_t esize = rd16(p);
            if (p + esize > end) break;

            if (esize >= 4) {
                uint32_t etype;
                std::memcpy(&etype, p, 4);

                if (etype == QUERY_CODE && esize >= 8) {
                    std::memcpy(&code_val, p + 4, 4);
                    have_code = true;
                } else if (etype == QUERY_TYPE && esize >= 8) {
                    std::memcpy(&type_val, p + 4, 4);
                    have_type = true;
                } else if (etype == QUERY_POSITION && esize >= 8) {
                    std::memcpy(&pos_val, p + 4, 4);
                    have_pos = true;
                } else if (etype == QUERY_LEVEL && esize >= 8) {
                    std::memcpy(&lv_val, p + 4, 4);
                } else if (etype == QUERY_ATTACK && esize >= 8) {
                    std::memcpy(&atk_val, p + 4, 4);
                } else if (etype == QUERY_DEFENSE && esize >= 8) {
                    std::memcpy(&def_val, p + 4, 4);
                } else if (etype == QUERY_END) {
                    p += esize;
                    break;
                }
            }
            p += esize;
        }

        if (have_code) {
            json card_json;
            card_json["code"] = code_val;
            if (have_type) card_json["type"] = type_val;
            if (have_pos) card_json["position"] = pos_val;
            card_json["level"] = lv_val;
            card_json["atk"] = atk_val;
            card_json["def"] = def_val;
            result.push_back(card_json);
        } else {
            result.push_back(nullptr);
        }
    }
    return result;
}

static std::vector<uint8_t> build_response(const uint8_t* buf, size_t len) {
    const uint8_t* p = buf;
    uint8_t mt = rd8(p);

    switch (mt) {
        case MSG_SELECT_IDLECMD: {
            rd8(p);
            uint32_t sc = rd32(p);
            for (uint32_t i = 0; i < sc; ++i) { rd32(p); rd8(p); rd8(p); rd32(p); }
            uint32_t spc = rd32(p);
            for (uint32_t i = 0; i < spc; ++i) { rd32(p); rd8(p); rd8(p); rd32(p); }
            uint32_t rpc = rd32(p);
            for (uint32_t i = 0; i < rpc; ++i) { rd32(p); rd8(p); rd8(p); rd8(p); }
            uint32_t mc = rd32(p);
            for (uint32_t i = 0; i < mc; ++i) { rd32(p); rd8(p); rd8(p); rd32(p); }
            uint32_t sc2 = rd32(p);
            for (uint32_t i = 0; i < sc2; ++i) { rd32(p); rd8(p); rd8(p); rd32(p); }
            uint32_t ac = rd32(p);
            for (uint32_t i = 0; i < ac; ++i) { rd32(p); rd8(p); rd8(p); rd32(p); rd64(p); rd8(p); }

            const uint8_t* end = buf + len;
            uint8_t can_bp = p < end ? rd8(p) : 0;
            uint8_t can_ep = p < end ? rd8(p) : 0;

            // Priority: activate effects > summon > set monsters > set spells/traps > battle phase > end phase
            if (ac > 0) {
                uint32_t v = (0u << 16) | 5;
                return {uint8_t(v & 0xff), uint8_t((v>>8)&0xff), uint8_t((v>>16)&0xff), uint8_t((v>>24)&0xff)};
            }
            if (sc > 0) {
                uint32_t v = 0;
                return {uint8_t(v & 0xff), uint8_t((v>>8)&0xff), uint8_t((v>>16)&0xff), uint8_t((v>>24)&0xff)};
            }
            if (mc > 0) {
                uint32_t v = (0u << 16) | 3;
                return {uint8_t(v & 0xff), uint8_t((v>>8)&0xff), uint8_t((v>>16)&0xff), uint8_t((v>>24)&0xff)};
            }
            if (sc2 > 0) {
                uint32_t v = (0u << 16) | 4;
                return {uint8_t(v & 0xff), uint8_t((v>>8)&0xff), uint8_t((v>>16)&0xff), uint8_t((v>>24)&0xff)};
            }
            if (can_bp) {
                uint32_t v = (0u << 16) | 6;
                return {uint8_t(v & 0xff), uint8_t((v>>8)&0xff), uint8_t((v>>16)&0xff), uint8_t((v>>24)&0xff)};
            }
            uint32_t v = (0u << 16) | 7;
            return {uint8_t(v & 0xff), uint8_t((v>>8)&0xff), uint8_t((v>>16)&0xff), uint8_t((v>>24)&0xff)};
        }

        case MSG_SELECT_BATTLECMD: {
            uint8_t playerid = rd8(p);
            uint32_t ac = rd32(p);
            for (uint32_t i = 0; i < ac; ++i) { rd32(p); rd8(p); rd8(p); rd32(p); rd64(p); rd8(p); }
            uint32_t atkc = rd32(p);
            for (uint32_t i = 0; i < atkc; ++i) { rd32(p); rd8(p); rd8(p); rd8(p); rd8(p); }
            const uint8_t* end = buf + len;
            uint8_t can_m2 = p < end ? rd8(p) : 0;
            uint8_t can_ep = p < end ? rd8(p) : 0;

            // Priority: activate > attack > main phase 2 > end phase
            if (ac > 0) {
                int32_t val = 0;
                std::vector<uint8_t> r(4); std::memcpy(r.data(), &val, 4);
                return r;
            }
            if (atkc > 0) {
                int32_t val = 1;
                std::vector<uint8_t> r(4); std::memcpy(r.data(), &val, 4);
                return r;
            }
            if (can_m2) {
                int32_t val = 2;
                std::vector<uint8_t> r(4); std::memcpy(r.data(), &val, 4);
                return r;
            }
            int32_t val = 3;
            std::vector<uint8_t> r(4); std::memcpy(r.data(), &val, 4);
            return r;
        }

        case MSG_SELECT_YESNO:
        case MSG_SELECT_EFFECTYN: {
            int32_t val = 1;
            std::vector<uint8_t> r(4); std::memcpy(r.data(), &val, 4);
            return r;
        }

        case MSG_SELECT_OPTION: {
            int32_t val = 0;
            std::vector<uint8_t> r(4); std::memcpy(r.data(), &val, 4);
            return r;
        }

        case MSG_SELECT_CARD:
        case MSG_SELECT_UNSELECT_CARD:
        case MSG_SELECT_TRIBUTE: {
            int32_t val = -1;
            std::vector<uint8_t> r(4); std::memcpy(r.data(), &val, 4);
            return r;
        }

        case MSG_SELECT_CHAIN: {
            rd8(p); rd8(p); uint32_t chains = rd32(p);
            bool can_activate = false;
            for (uint32_t i = 0; i < chains; ++i) {
                rd32(p); rd8(p); rd8(p); rd32(p); rd32(p); rd8(p); rd8(p); rd32(p); rd64(p);
                rd8(p); uint8_t forced = rd8(p);
                if (forced > 0) can_activate = true;
            }
            if (can_activate && chains > 0) {
                int32_t val = 0;
                std::vector<uint8_t> r(4); std::memcpy(r.data(), &val, 4);
                return r;
            }
            int32_t val = -1;
            std::vector<uint8_t> r(4); std::memcpy(r.data(), &val, 4);
            return r;
        }

        case MSG_SELECT_PLACE:
        case MSG_SELECT_DISFIELD: {
            uint8_t playerid = rd8(p); (void)playerid;
            uint32_t count = rd32(p);
            if (count > 0) {
                rd8(p); rd8(p); uint8_t loc = rd8(p); uint8_t seq = rd8(p);
                return {0, loc, seq};
            }
            return {0, LOCATION_MZONE, 0};
        }

        case MSG_SELECT_POSITION: {
            rd8(p); uint32_t count = rd32(p);
            if (count > 0) {
                uint32_t positions = rd32(p);
                if (positions & POS_FACEUP_ATTACK) { int32_t v = POS_FACEUP_ATTACK; std::vector<uint8_t> r(4); std::memcpy(r.data(), &v, 4); return r; }
                if (positions & POS_FACEUP_DEFENSE) { int32_t v = POS_FACEUP_DEFENSE; std::vector<uint8_t> r(4); std::memcpy(r.data(), &v, 4); return r; }
                if (positions & POS_FACEDOWN_DEFENSE) { int32_t v = POS_FACEDOWN_DEFENSE; std::vector<uint8_t> r(4); std::memcpy(r.data(), &v, 4); return r; }
            }
            int32_t val = POS_FACEUP_ATTACK;
            std::vector<uint8_t> r(4); std::memcpy(r.data(), &val, 4);
            return r;
        }

        case MSG_SELECT_SUM:
        case MSG_SELECT_COUNTER: {
            int32_t val = -1;
            std::vector<uint8_t> r(4); std::memcpy(r.data(), &val, 4);
            return r;
        }

        default:
            return {};
    }
}

static EngineResult run_engine(OCG_Duel duel, bool auto_play = false, const std::string& duelId = "") {
    EngineResult r{};
    r.init_success = true;
    r.turn = 0;
    r.phase = PHASE_DRAW;
    r.turn_player = 0;
    r.lp[0] = 8000; r.lp[1] = 8000;

    int responded = 0;
    bool saw_human_select = false;
    int safety = 0;
    while (!r.game_over && safety < 500) {
        ++safety;
        int st = OCG_DuelProcess(duel);
        DEBUG("[ocgcore-debug] DuelProcess call %d returned %d (END=%d, AWAIT=%d, CONT=%d)\n",
                safety, st, OCG_DUEL_STATUS_END, OCG_DUEL_STATUS_AWAITING, OCG_DUEL_STATUS_CONTINUE);
        if (st == OCG_DUEL_STATUS_END) { r.game_over = true; break; }

        uint32_t len = 0;
        const uint8_t* ptr = static_cast<const uint8_t*>(OCG_DuelGetMessage(duel, &len));
        if (!ptr || len == 0) { DEBUG("[ocgcore-debug] No messages, continuing\n"); continue; }

        DEBUG("[ocgcore-debug] Got %u bytes of messages\n", len);

        const uint8_t* end = ptr + len;
        bool saw_phase = false;
        while (ptr < end) {
            if (ptr + 4 > end) break;
            uint32_t msg_size;
            std::memcpy(&msg_size, ptr, 4); ptr += 4;
            if (ptr + msg_size > end) break;

            const uint8_t* b = ptr;
            uint8_t mt = b[0];
            DEBUG("[ocgcore-debug]   msg_type=%d\n", (int)mt);

            switch (mt) {
                case MSG_WIN:
                    if (msg_size >= 3) { r.winner_player = b[1]; r.win_reason = b[2]; }
                    r.game_over = true; return r;

                case MSG_NEW_TURN:
                    if (msg_size >= 2) { r.turn_player = b[1]; r.turn++; }
                    break;

                case MSG_NEW_PHASE:
                    if (msg_size >= 3) {
                        uint16_t ph;
                        std::memcpy(&ph, b + 1, 2);
                        r.phase = ph;
                    }
                    if (responded > 0)
                        saw_phase = true;
                    break;

                case MSG_LPUPDATE:
                    if (msg_size >= 6) {
                        uint8_t pl = b[1]; uint32_t lp;
                        std::memcpy(&lp, b + 2, 4);
                        if (pl < 2) r.lp[pl] = (int32_t)lp;
                    }
                    break;

                case MSG_DRAW: case MSG_MOVE: case MSG_POS_CHANGE:
                case MSG_DAMAGE: case MSG_RECOVER:
                case MSG_SUMMONING: case MSG_SUMMONED:
                case MSG_SPSUMMONING: case MSG_SPSUMMONED:
                case MSG_FLIPSUMMONING: case MSG_FLIPSUMMONED:
                case MSG_SET: case MSG_ATTACK: case MSG_BATTLE:
                case MSG_SHUFFLE_DECK: case MSG_SHUFFLE_HAND:
                case MSG_HINT: case MSG_CONFIRM_DECKTOP:
                case MSG_CONFIRM_CARDS: case MSG_REFRESH_DECK:
                case MSG_START:
                    break;

                case MSG_SELECT_IDLECMD:
                case MSG_SELECT_BATTLECMD:
                case MSG_SELECT_YESNO:
                case MSG_SELECT_EFFECTYN:
                case MSG_SELECT_CARD:
                case MSG_SELECT_OPTION:
                case MSG_SELECT_PLACE:
                case MSG_SELECT_DISFIELD:
                case MSG_SELECT_POSITION:
                case MSG_SELECT_TRIBUTE:
                case MSG_SELECT_CHAIN:
                case MSG_SELECT_SUM:
                case MSG_SELECT_UNSELECT_CARD:
                case MSG_SELECT_COUNTER: {
                    uint8_t msg_playerid = (msg_size >= 2) ? b[1] : 0;
                    if (auto_play && msg_playerid == 1) {
                        auto resp = build_response(b, msg_size);
                        if (!resp.empty())
                            OCG_DuelSetResponse(duel, resp.data(), (uint32_t)resp.size());
                        responded++;
                    } else {
                        saw_human_select = true;
                    }
                    break;
                }

                case MSG_RETRY:
                    r.game_over = true;
                    return r;

                default:
                    break;
            }

            ptr += msg_size;
        }

        if (saw_human_select) {
            DEBUG("[ocgcore-debug] BREAK at human SELECT: auto_play=%d responded=%d\n", auto_play, responded);
            break;
        }
        if (responded > 0 && saw_phase) {
            DEBUG("[ocgcore-debug] BREAK: responded=%d saw_phase=%d phase=0x%x turn=%d turnPlayer=%d\n",
                    responded, (int)saw_phase, r.phase, r.turn, r.turn_player);
            break;
        }
    }

    DEBUG("[ocgcore-debug] run_engine result: phase=0x%x turn=%d turnPlayer=%d lp=[%d,%d] gameOver=%d\n",
            r.phase, r.turn, r.turn_player, r.lp[0], r.lp[1], (int)r.game_over);

    uint32_t flen = 0;
    void* fraw = OCG_DuelQueryField(duel, &flen);
    if (fraw && flen > 0) {
        const uint8_t* p = static_cast<const uint8_t*>(fraw);
        const uint8_t* end = p + flen;

        json field;
        field["duelOptions"] = rd32(p);
        json players = json::array();

        for (int pl = 0; pl < 2; ++pl) {
            json player;
            player["lp"] = (int32_t)rd32(p);

            json mz = json::array();
            for (int z = 0; z < 7; ++z) {
                json zj;
                if (p >= end) break;
                uint8_t present = rd8(p);
                zj["present"] = (bool)present;
                if (present) { zj["position"] = rd8(p); zj["xyzCount"] = rd32(p); }
                mz.push_back(zj);
            }
            player["monsterZones"] = mz;

            json sz = json::array();
            for (int z = 0; z < 8; ++z) {
                json zj;
                if (p >= end) break;
                uint8_t present = rd8(p);
                zj["present"] = (bool)present;
                if (present) { zj["position"] = rd8(p); zj["xyzCount"] = rd32(p); }
                sz.push_back(zj);
            }
            player["spellTrapZones"] = sz;

            player["deckCount"]   = (uint32_t)OCG_DuelQueryCount(duel, (uint8_t)pl, LOCATION_DECK);
            player["handCount"]   = (uint32_t)OCG_DuelQueryCount(duel, (uint8_t)pl, LOCATION_HAND);
            player["graveCount"]  = (uint32_t)OCG_DuelQueryCount(duel, (uint8_t)pl, LOCATION_GRAVE);
            player["removedCount"] = (uint32_t)OCG_DuelQueryCount(duel, (uint8_t)pl, LOCATION_REMOVED);
            player["extraCount"]   = (uint32_t)OCG_DuelQueryCount(duel, (uint8_t)pl, LOCATION_EXTRA);
            player["extraPCount"] = 0;

            players.push_back(player);
        }

        field["players"] = players;

        if (p < end) {
            uint32_t cc = rd32(p);
            json chains = json::array();
            for (uint32_t i = 0; i < cc && p < end; ++i) {
                json ch;
                ch["code"] = rd32(p);
                ch["controler"] = rd8(p);
                ch["location"] = rd8(p);
                ch["sequence"] = rd32(p);
                ch["position"] = rd32(p);
                ch["triggerControler"] = rd8(p);
                ch["triggerLocation"] = rd8(p);
                ch["triggerSequence"] = rd32(p);
                ch["description"] = rd64(p);
                chains.push_back(ch);
            }
            field["chain"] = chains;
        }

        r.field_data = field;
    }

    uint32_t query_flags = QUERY_CODE | QUERY_TYPE | QUERY_POSITION | QUERY_LEVEL | QUERY_ATTACK | QUERY_DEFENSE;
    json card_data;
    card_data["queryFlags"] = (uint32_t)query_flags;
    json card_players = json::array();
    for (int pl = 0; pl < 2; ++pl) {
        json cp;
        cp["hand"] = query_location_codes(duel, (uint8_t)pl, LOCATION_HAND, query_flags);
        cp["grave"] = query_location_codes(duel, (uint8_t)pl, LOCATION_GRAVE, query_flags);
        cp["removed"] = query_location_codes(duel, (uint8_t)pl, LOCATION_REMOVED, query_flags);
        cp["deck"] = query_location_codes(duel, (uint8_t)pl, LOCATION_DECK, query_flags);
        cp["extra"] = query_location_codes(duel, (uint8_t)pl, LOCATION_EXTRA, query_flags);
        cp["monsterZones"] = query_zone_codes(duel, (uint8_t)pl, LOCATION_MZONE, 7);
        cp["spellTrapZones"] = query_zone_codes(duel, (uint8_t)pl, LOCATION_SZONE, 8);
        card_players.push_back(cp);
    }
    card_data["players"] = card_players;
    r.card_data = card_data;

    return r;
}

extern "C" {

JNIEXPORT jstring JNICALL Java_com_odevpedro_yugiohcollections_duel_adapter_out_ocgcore_OcgCoreBridge_processAction
  (JNIEnv* env, jobject, jstring stateJson, jstring actionJson, jstring)
{
    const char* state_cstr = env->GetStringUTFChars(stateJson, nullptr);
    const char* action_cstr = env->GetStringUTFChars(actionJson, nullptr);

    json result;
    OCG_Duel duel = nullptr;
    bool is_new = false;
    bool was_removed = false;
    std::string duelId;

    do {
        json state, action;
        try {
            state = json::parse(state_cstr);
            action = json::parse(action_cstr);
        } catch (...) {
            result["error"] = "JSON parse error";
            break;
        }

        duelId = state.value("duelId", "");
        if (duelId.empty()) {
            result["error"] = "missing duelId";
            break;
        }

        {
            std::lock_guard<std::mutex> lock(g_mutex);
            auto it = g_active_duels.find(duelId);
            if (it != g_active_duels.end()) {
                duel = it->second;
            }
        }

        if (duel) {
            is_new = false;

            // Read the pending engine message and build a response
            uint32_t msg_len = 0;
            const uint8_t* msg = static_cast<const uint8_t*>(OCG_DuelGetMessage(duel, &msg_len));
            if (msg && msg_len > 0) {
                auto resp = build_response(msg, msg_len);
                if (!resp.empty()) {
                    OCG_DuelSetResponse(duel, resp.data(), (uint32_t)resp.size());
                }
            }
        } else {
            is_new = true;

            static std::once_flag db_init_flag;
            std::call_once(db_init_flag, []() {
                const char* paths[] = {
                    "cards.cdb",
                    "../cards.cdb",
                    "/usr/share/ygopro/cards.cdb",
                    "/usr/local/share/ygopro/cards.cdb",
                    getenv("YGOPRO_CARDS_DB")
                };
                for (auto p : paths) {
                    if (p && access(p, R_OK) == 0) {
                        init_card_db(p);
                        break;
                    }
                }
            });

            OCG_DuelOptions opts{};
            opts.seed[0] = 1; opts.seed[1] = 2; opts.seed[2] = 3; opts.seed[3] = 4;
            opts.flags = DUEL_TEST_MODE;
            opts.team1.startingLP = 8000;
            opts.team1.startingDrawCount = 5;
            opts.team1.drawCountPerTurn = 1;
            opts.team2.startingLP = 8000;
            opts.team2.startingDrawCount = 5;
            opts.team2.drawCountPerTurn = 1;

            auto* card_db = new std::unordered_map<uint32_t, OCG_CardData>();

            opts.cardReader = card_reader;
            opts.payload1 = card_db;
            opts.scriptReader = script_reader;
            opts.payload2 = nullptr;
            opts.logHandler = log_handler;
            opts.payload3 = nullptr;
            opts.cardReaderDone = card_reader_done;
            opts.payload4 = nullptr;

            int rc = OCG_CreateDuel(&duel, &opts);
            if (rc != OCG_DUEL_CREATION_SUCCESS || !duel) {
                delete card_db;
                result["error"] = "OCG_CreateDuel failed: " + std::to_string(rc);
                break;
            }

            auto add_cards = [&](const json& pj, uint8_t team, uint8_t duelist) {
                static const std::pair<const char*, uint32_t> locs[] = {
                    {"deck", LOCATION_DECK},
                    {"extraDeck", LOCATION_EXTRA}
                };
                for (auto& [key, loc] : locs) {
                    if (!pj.contains(key) || !pj[key].is_array()) continue;
                    for (const auto& card : pj[key]) {
                        uint32_t code = (uint32_t)card.value("code", 0LL);
                        if (code == 0 && card.contains("cardId")) {
                            if (card["cardId"].is_number())
                                code = card["cardId"].get<uint32_t>();
                            else
                                code = (uint32_t)std::hash<std::string>{}(card["cardId"].get<std::string>());
                        }
                        if (code == 0) continue;

                        OCG_CardData cd{};
                        cd.code = code;
                        cd.level = (uint32_t)card.value("level", 4);
                        cd.attack = card.value("atk", 0);
                        cd.defense = card.value("def", 0);
                        std::string t = card.value("type", "MONSTER");
                        if (t == "SPELL") {
                            cd.type = TYPE_SPELL;
                        } else if (t == "TRAP") {
                            cd.type = TYPE_TRAP;
                        } else {
                            cd.type = TYPE_MONSTER | TYPE_NORMAL;
                            cd.race = RACE_WARRIOR;
                            cd.attribute = ATTRIBUTE_EARTH;
                        }
                        (*card_db)[code] = cd;

                        OCG_NewCardInfo ci{};
                        ci.team = team;
                        ci.duelist = 0;
                        ci.code = code;
                        ci.con = team;
                        ci.loc = loc;
                        ci.seq = 0;
                        ci.pos = POS_FACEDOWN_DEFENSE;
                        OCG_DuelNewCard(duel, &ci);
                    }
                }
            };

            if (state.contains("playerA")) add_cards(state["playerA"], 0, 0);
            if (state.contains("playerB")) add_cards(state["playerB"], 1, 1);

            OCG_StartDuel(duel);

            {
                std::lock_guard<std::mutex> lock(g_mutex);
                g_active_duels[duelId] = duel;
                g_card_dbs[duelId] = card_db;
            }
        }

        EngineResult er = run_engine(duel, !is_new, duelId);

        result["engine"] = {
            {"turn", er.turn},
            {"phase", er.phase},
            {"turnPlayer", er.turn_player},
            {"lp0", er.lp[0]},
            {"lp1", er.lp[1]},
            {"gameOver", er.game_over},
            {"winnerPlayer", er.winner_player},
            {"winReason", er.win_reason},
            {"field", er.field_data}
        };
        if (!er.card_data.is_null()) {
            result["cardData"] = er.card_data;
        }
        result["duelId"] = duelId;
        result["turnNumber"] = (int)er.turn;
        result["currentPhase"] = er.phase == PHASE_DRAW ? "DRAW"
            : er.phase == PHASE_STANDBY ? "STANDBY"
            : er.phase == PHASE_MAIN1 ? "MAIN_1"
            : er.phase == PHASE_BATTLE ? "BATTLE"
            : er.phase == PHASE_MAIN2 ? "MAIN_2"
            : er.phase == PHASE_END ? "END" : "UNKNOWN";
        result["status"] = er.game_over ? "FINISHED" : "IN_PROGRESS";

        if (er.game_over && duel) {
            std::lock_guard<std::mutex> lock(g_mutex);
            auto cd_it = g_card_dbs.find(duelId);
            if (cd_it != g_card_dbs.end()) {
                delete static_cast<std::unordered_map<uint32_t, OCG_CardData>*>(cd_it->second);
                g_card_dbs.erase(cd_it);
            }
            OCG_DestroyDuel(duel);
            g_active_duels.erase(duelId);
            duel = nullptr;
            was_removed = true;
        }

    } while (false);

    if (duel && !was_removed) {
        // Duel is still alive and stored in map, leave it there
    } else if (duel) {
        OCG_DestroyDuel(duel);
    }

    env->ReleaseStringUTFChars(stateJson, state_cstr);
    env->ReleaseStringUTFChars(actionJson, action_cstr);

    std::string out = result.dump();
    return env->NewStringUTF(out.c_str());
}

JNIEXPORT jstring JNICALL Java_com_odevpedro_yugiohcollections_duel_adapter_out_ocgcore_OcgCoreBridge_advancePhase
  (JNIEnv* env, jobject self, jstring stateJson) {
    return Java_com_odevpedro_yugiohcollections_duel_adapter_out_ocgcore_OcgCoreBridge_processAction(
        env, self, stateJson,
        env->NewStringUTF(R"({"actionType":"NONE"})"),
        nullptr);
}

JNIEXPORT jboolean JNICALL Java_com_odevpedro_yugiohcollections_duel_adapter_out_ocgcore_OcgCoreBridge_isActionValid
  (JNIEnv*, jobject, jstring, jstring, jstring) {
    return JNI_TRUE;
}

} // extern "C"
