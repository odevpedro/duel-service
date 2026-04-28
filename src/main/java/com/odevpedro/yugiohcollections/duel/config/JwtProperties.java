package com.odevpedro.yugiohcollections.duel.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret;
    private long expirationMs;
    private long refreshExpirationMs;
    private boolean skipBlacklistCheck = true;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public void setExpirationMs(long expirationMs) {
        this.expirationMs = expirationMs;
    }

    public long getRefreshExpirationMs() {
        return refreshExpirationMs;
    }

    public void setRefreshExpirationMs(long refreshExpirationMs) {
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public boolean isSkipBlacklistCheck() {
        return skipBlacklistCheck;
    }

    public void setSkipBlacklistCheck(boolean skipBlacklistCheck) {
        this.skipBlacklistCheck = skipBlacklistCheck;
    }
}