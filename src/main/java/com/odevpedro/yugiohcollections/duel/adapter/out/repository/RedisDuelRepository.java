package com.odevpedro.yugiohcollections.duel.adapter.out.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.odevpedro.yugiohcollections.duel.domain.model.DuelState;
import com.odevpedro.yugiohcollections.duel.domain.port.DuelRepositoryPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Repository
@Profile("!dev")
public class RedisDuelRepository implements DuelRepositoryPort {

    private static final String KEY_PREFIX = "duel:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final long ttlHours;
    private final java.util.Map<String, DuelState> fallbackStore = new ConcurrentHashMap<>();
    private volatile boolean redisAvailable = true;

    public RedisDuelRepository(StringRedisTemplate redisTemplate,
                               @Value("${duel.redis.ttl-hours:24}") long ttlHours) {
        this.redisTemplate = redisTemplate;
        this.ttlHours = ttlHours;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public DuelState save(DuelState duelState) {
        try {
            duelState.setVersion(duelState.getVersion() + 1);
            if (!redisAvailable) {
                fallbackStore.put(duelState.getDuelId(), duelState);
                return duelState;
            }
            String key = KEY_PREFIX + duelState.getDuelId();
            String value = objectMapper.writeValueAsString(duelState);
            redisTemplate.opsForValue().set(key, value, ttlHours, TimeUnit.HOURS);
            log.debug("Saved duel {} to Redis", duelState.getDuelId());
            return duelState;
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize duel state: {}", duelState.getDuelId(), e);
            throw new RuntimeException("Failed to save duel state", e);
        } catch (Exception e) {
            redisAvailable = false;
            fallbackStore.put(duelState.getDuelId(), duelState);
            log.warn("Redis unavailable, using in-memory fallback for duel {}: {}", duelState.getDuelId(), e.getMessage());
            return duelState;
        }
    }

    @Override
    public Optional<DuelState> findById(String duelId) {
        if (!redisAvailable) {
            return Optional.ofNullable(fallbackStore.get(duelId));
        }
        String key = KEY_PREFIX + duelId;
        try {
            String value = redisTemplate.opsForValue().get(key);
        
            if (value == null) {
                return Optional.empty();
            }
        
            DuelState state = objectMapper.readValue(value, DuelState.class);
            return Optional.of(state);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize duel state: {}", duelId, e);
            return Optional.empty();
        } catch (Exception e) {
            redisAvailable = false;
            log.warn("Redis unavailable, reading duel {} from in-memory fallback: {}", duelId, e.getMessage());
            return Optional.ofNullable(fallbackStore.get(duelId));
        }
    }

    @Override
    public void delete(String duelId) {
        if (!redisAvailable) {
            fallbackStore.remove(duelId);
            return;
        }
        String key = KEY_PREFIX + duelId;
        try {
            redisTemplate.delete(key);
            log.debug("Deleted duel {} from Redis", duelId);
        } catch (Exception e) {
            redisAvailable = false;
            fallbackStore.remove(duelId);
            log.warn("Redis unavailable, deleting duel {} from in-memory fallback: {}", duelId, e.getMessage());
        }
    }

    public void extendTtl(String duelId) {
        if (!redisAvailable) {
            return;
        }
        String key = KEY_PREFIX + duelId;
        try {
            redisTemplate.expire(key, ttlHours, TimeUnit.HOURS);
        } catch (Exception e) {
            redisAvailable = false;
            log.warn("Redis unavailable, skipping TTL extension for duel {}: {}", duelId, e.getMessage());
        }
    }
}
