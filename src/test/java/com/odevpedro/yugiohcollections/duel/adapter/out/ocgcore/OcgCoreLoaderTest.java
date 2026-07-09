package com.odevpedro.yugiohcollections.duel.adapter.out.ocgcore;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class OcgCoreLoaderTest {

    @Test
    void isLoadedShouldDefaultToFalse() {
        ReflectionTestUtils.setField(OcgCoreLoader.class, "loaded", false);
        assertThat(OcgCoreLoader.isLoaded()).isFalse();
    }

    @Test
    void isLoadedShouldReflectStateAfterSetting() {
        ReflectionTestUtils.setField(OcgCoreLoader.class, "loaded", true);
        assertThat(OcgCoreLoader.isLoaded()).isTrue();

        ReflectionTestUtils.setField(OcgCoreLoader.class, "loaded", false);
        assertThat(OcgCoreLoader.isLoaded()).isFalse();
    }

    @Test
    void adapterShouldUseFallbackWhenLoaderIsNotLoaded() {
        ReflectionTestUtils.setField(OcgCoreLoader.class, "loaded", false);
        assertThat(OcgCoreLoader.isLoaded()).isFalse();
    }
}
