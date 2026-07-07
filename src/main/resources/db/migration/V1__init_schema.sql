CREATE TABLE IF NOT EXISTS duel_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    duel_id VARCHAR(255) NOT NULL UNIQUE,
    player_a_id VARCHAR(255) NOT NULL,
    player_b_id VARCHAR(255) NOT NULL,
    winner_id VARCHAR(255),
    loser_id VARCHAR(255),
    player_a_final_lp INTEGER,
    player_b_final_lp INTEGER,
    turn_count INTEGER,
    duel_type VARCHAR(50),
    result VARCHAR(50),
    started_at TIMESTAMP NOT NULL,
    finished_at TIMESTAMP,
    duration_seconds BIGINT
);
