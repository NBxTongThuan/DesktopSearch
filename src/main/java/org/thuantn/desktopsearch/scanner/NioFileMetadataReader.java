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
    public FileMetaData read(Path path) throws IOException {

        Objects.requireNonNull(path, AppConstant.PATH_MUST_NOT_BE_NULL);

        BasicFileAttributes attributes = Files.readAttributes(
                path, BasicFileAttributes.class
        );

        return FileMetaData.builder()
                .size(attributes.size())
                .fileName(path.getFileName().toString())
                .createdAt(attributes.creationTime().toInstant())
                .path(path)
                .extension(extractExtension(path.getFileName().toString()))
                .modifiedAt(attributes.lastModifiedTime().toInstant())
                .build();
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
