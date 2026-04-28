package com.odevpedro.yugiohcollections.duel.adapter.out.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.odevpedro.yugiohcollections.duel.domain.model.DuelState;
import com.odevpedro.yugiohcollections.duel.domain.port.DuelRepositoryPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Repository
public class RedisDuelRepository implements DuelRepositoryPort {

    private static final String KEY_PREFIX = "duel:";
    private static final long TTL_HOURS = 24;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisDuelRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public DuelState save(DuelState duelState) {
        try {
            String key = KEY_PREFIX + duelState.getDuelId();
            String value = objectMapper.writeValueAsString(duelState);
            redisTemplate.opsForValue().set(key, value, TTL_HOURS, TimeUnit.HOURS);
            log.debug("Saved duel {} to Redis", duelState.getDuelId());
            return duelState;
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize duel state: {}", duelState.getDuelId(), e);
            throw new RuntimeException("Failed to save duel state", e);
        }
    }

    @Override
    public Optional<DuelState> findById(String duelId) {
        String key = KEY_PREFIX + duelId;
        String value = redisTemplate.opsForValue().get(key);
        
        if (value == null) {
            return Optional.empty();
        }
        
        try {
            DuelState state = objectMapper.readValue(value, DuelState.class);
            return Optional.of(state);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize duel state: {}", duelId, e);
            return Optional.empty();
        }
    }

    @Override
    public void delete(String duelId) {
        String key = KEY_PREFIX + duelId;
        redisTemplate.delete(key);
        log.debug("Deleted duel {} from Redis", duelId);
    }

    public void extendTtl(String duelId) {
        String key = KEY_PREFIX + duelId;
        redisTemplate.expire(key, TTL_HOURS, TimeUnit.HOURS);
    }
}