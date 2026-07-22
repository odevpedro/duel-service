package com.odevpedro.yugiohcollections.duel.adapter.out.ocgcore;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Slf4j
@Component
public class OcgCoreLoader {

    private static volatile boolean loaded;
    private static final String LIB_NAME = resolveLibName();
    private static final String RESOURCE_PATH = "/native/" + LIB_NAME;

    @PostConstruct
    public void load() {
        log.info("Loading native library: {}", LIB_NAME);

        try (InputStream in = getClass().getResourceAsStream(RESOURCE_PATH)) {
            if (in == null) {
                loaded = false;
                throw new IllegalStateException(
                        "Native ocgcore library not found at " + RESOURCE_PATH
                                + ". Run ./gradlew fullBuildNative before starting the service."
                );
            }

            Path temp = Files.createTempFile("ocgcore-", "-" + LIB_NAME);
            temp.toFile().deleteOnExit();
            Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);

            System.load(temp.toAbsolutePath().toString());
            log.info("ocgcore loaded from: {}", temp);
            loaded = true;
        } catch (Throwable t) {
            loaded = false;
            log.error("Failed to load native ocgcore; no stub or fallback is available", t);
            throw new IllegalStateException("Native ocgcore is required", t);
        }
    }

    public static boolean isLoaded() {
        return loaded;
    }

    private static String resolveLibName() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win"))   return "ocgcore.dll";
        if (os.contains("mac"))   return "libocgcore.dylib";
        return "libocgcore.so";
    }
}
