package org.thuantn.desktopsearch.scanner;


import lombok.Builder;

import java.nio.file.Path;
import java.time.Instant;

@Builder
public record FileMetaData(

        Path path,
        String fileName,
        String extension,
        long size,
        Instant createdAt,
        Instant modifiedAt

) {}
