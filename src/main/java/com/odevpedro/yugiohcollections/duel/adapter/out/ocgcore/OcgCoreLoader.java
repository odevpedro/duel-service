package com.odevpedro.yugiohcollections.duel.adapter.out.ocgcore;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Extrai a lib nativa de dentro do JAR para um diretório temporário
 * e a carrega via System.load — torna o JAR autocontido.
 */
@Slf4j
@Component
public class OcgCoreLoader {

    private static final String LIB_NAME = resolveLibName();
    private static final String RESOURCE_PATH = "/native/" + LIB_NAME;

    @PostConstruct
    public void load() throws IOException {
        log.info("Loading native library: {}", LIB_NAME);

        try (InputStream in = getClass().getResourceAsStream(RESOURCE_PATH)) {
            if (in == null) {
                throw new IllegalStateException(
                    "Native library not found in JAR: " + RESOURCE_PATH +
                    "\nCompile the ocgcore and place it in src/main/resources/native/"
                );
            }

            Path temp = Files.createTempFile("ocgcore-", "-" + LIB_NAME);
            temp.toFile().deleteOnExit();
            Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);

            System.load(temp.toAbsolutePath().toString());
            log.info("ocgcore loaded from: {}", temp);
        }
    }

    private static String resolveLibName() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win"))   return "ocgcore.dll";
        if (os.contains("mac"))   return "libocgcore.dylib";
        return "libocgcore.so";
    }
}
