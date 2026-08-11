package org.thuantn.desktopsearch.indexing;

import org.thuantn.desktopsearch.scanner.FileMetadata;

import java.io.IOException;
import java.nio.file.Path;

public interface IndexRepository {

    void upsert(FileMetadata fileMetaData) throws IOException;

    void delete(Path path) throws IOException;

    void commit() throws IOException;

    void close() throws IOException;

}
