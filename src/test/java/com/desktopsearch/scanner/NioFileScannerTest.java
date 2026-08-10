package com.desktopsearch.scanner;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.thuantn.desktopsearch.scanner.FileScanner;
import org.thuantn.desktopsearch.scanner.NioFileScanner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class NioFileScannerTest {

    private final FileScanner scanner = new NioFileScanner();

    @TempDir
    Path tempDir;

    @Test
    void shouldScanFilesRecursively() throws IOException {

        // Arrange
        Path fileA = Files.createFile(
                tempDir.resolve("a.txt")
        );

        Path fileB = Files.createFile(
                tempDir.resolve("b.pdf")
        );

        Path srcDir = Files.createDirectory(
                tempDir.resolve("src")
        );

        Path fileC = Files.createFile(
                srcDir.resolve("Main.java")
        );

        List<Path> scannedFiles = new ArrayList<>();

        // Act
        scanner.scan(
                tempDir,
                scannedFiles::add
        );

        // Assert
        assertEquals(3, scannedFiles.size());

        assertTrue(scannedFiles.contains(fileA));
        assertTrue(scannedFiles.contains(fileB));
        assertTrue(scannedFiles.contains(fileC));
    }

    @Test
    void shouldThrowExceptionWhenRootDoesNotExist() {

        // Arrange
        Path notExistingPath =
                tempDir.resolve("not-exist");

        // Act + Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> scanner.scan(
                        notExistingPath,
                        path -> {
                        }
                )
        );
    }

    @Test
    void shouldThrowExceptionWhenRootIsNotDirectory()
            throws IOException {

        // Arrange
        Path file =
                Files.createFile(
                        tempDir.resolve("test.txt")
                );

        // Act + Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> scanner.scan(
                        file,
                        path -> {
                        }
                )
        );
    }

}
