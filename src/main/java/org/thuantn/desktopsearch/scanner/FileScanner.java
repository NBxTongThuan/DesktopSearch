package org.thuantn.desktopsearch.scanner;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

public interface FileScanner {
    
    void scan(Path root, Consumer<Path> fileConsumer) throws IOException;
    
}
