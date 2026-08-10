package org.thuantn.desktopsearch.scanner;


import org.thuantn.desktopsearch.common.AppConstant;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.function.Consumer;

public class NioFileScanner implements FileScanner {
    @Override
    public void scan(Path root, Consumer<Path> fileConsumer) throws IOException {

        Objects.requireNonNull(root, AppConstant.ROOT_REQUIRE_NON_NULL);
        Objects.requireNonNull(fileConsumer, AppConstant.FILE_CONSUMER_REQUIRE_NON_NULL);

        if (Files.notExists(root)) {
            throw new IllegalArgumentException(AppConstant.ROOT_PATH_IS_NOT_EXISTS + root);
        }

        if (!Files.isDirectory(root)) {
            throw new IllegalArgumentException(AppConstant.ROOT_PATH_IS_NOT_A_DIRECTORY + root);
        }

        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        fileConsumer.accept(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) {
//                        log.warn("Failed to access path: {}", file, exc);
                        return FileVisitResult.CONTINUE;
                    }
                }
        );
    }
}
