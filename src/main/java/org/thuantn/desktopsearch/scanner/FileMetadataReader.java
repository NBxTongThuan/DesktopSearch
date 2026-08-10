package org.thuantn.desktopsearch.scanner;

import java.io.IOException;
import java.nio.file.Path;

public interface FileMetadataReader {

    FileMetaData read(Path path) throws IOException;

}
