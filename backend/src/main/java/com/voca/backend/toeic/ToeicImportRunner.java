package com.voca.backend.toeic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class ToeicImportRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ToeicImportRunner.class);

    private final ToeicImportService importService;
    private final boolean importOnStartup;
    private final String importDir;

    public ToeicImportRunner(
            ToeicImportService importService,
            @Value("${app.toeic.import-on-startup:true}") boolean importOnStartup,
            @Value("${app.toeic.import-dir:../imports/chamtoeic}") String importDir
    ) {
        this.importService = importService;
        this.importOnStartup = importOnStartup;
        this.importDir = importDir;
    }

    @Override
    public void run(String... args) {
        if (!importOnStartup) {
            log.info("TOEIC import on startup disabled");
            return;
        }
        importService.importDirectory(Path.of(importDir));
    }
}
