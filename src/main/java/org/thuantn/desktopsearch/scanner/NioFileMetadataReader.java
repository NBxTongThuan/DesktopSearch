package org.thuantn.desktopsearch.scanner;

import org.thuantn.desktopsearch.common.AppConstant;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Locale;
import java.util.Objects;

public class NioFileMetadataReader implements FileMetadataReader {

    @Override
    public FileMetadata read(Path path) throws IOException {

        Objects.requireNonNull(path, AppConstant.PATH_MUST_NOT_BE_NULL);

        BasicFileAttributes attributes = Files.readAttributes(
                path, BasicFileAttributes.class
        );

        return new FileMetadata(
            path,
            path.getFileName().toString(),
            extractExtension(path.getFileName().toString()),
            attributes.size(),
            attributes.creationTime().toInstant(),
            attributes.lastModifiedTime().toInstant()
        );
    }

    private String extractExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');

        if (lastDot < 0 || lastDot == fileName.length() - 1) {
            return "";
        }

        return fileName.substring(lastDot + 1)
                .toLowerCase(Locale.ROOT);
    }
}
