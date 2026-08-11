package com.desktopsearch.scanner;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.store.FSDirectory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.thuantn.desktopsearch.indexing.lucene.LuceneDocumentMapper;
import org.thuantn.desktopsearch.indexing.lucene.LuceneFieldNames;
import org.thuantn.desktopsearch.indexing.lucene.LuceneIndexRepository;
import org.thuantn.desktopsearch.scanner.FileMetadata;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IndexTest {
    private final LuceneDocumentMapper luceneDocumentMapper = new LuceneDocumentMapper();



    @TempDir
    Path tempDir;

    private LuceneIndexRepository luceneIndexRepository;

    public IndexTest() throws IOException {
    }

    @Test
    void mapToLuceneDocument() {
        FileMetadata metadata = new FileMetadata(
            Path.of("C:\\Users\\TongThuan\\Downloads\\_DETAILED_FLOW_FOR_TEAM 2.xlsx"),
            "_DETAILED_FLOW_FOR_TEAM 2.xlsx",
            "xlsx",
            1024L,
            Instant.parse("2026-08-10T10:00:00Z"),
            Instant.parse("2026-08-10T11:00:00Z")
        );

        Document document = luceneDocumentMapper.mapToDocument(metadata);

        assertEquals(1024L,document.getField(LuceneFieldNames.SIZE)
            .numericValue()
            .longValue()
        );

    }

    @Test
    void indexDocument() throws IOException {

        luceneIndexRepository = new LuceneIndexRepository(tempDir, luceneDocumentMapper);

        FileMetadata metadata = new FileMetadata(
            tempDir,
            "_DETAILED_FLOW_FOR_TEAM 2.xlsx",
            "xlsx",
            1024L,
            Instant.parse("2026-08-10T10:00:00Z"),
            Instant.parse("2026-08-10T11:00:00Z")
        );

        luceneIndexRepository.upsert(metadata);

        luceneIndexRepository.commit();

        luceneIndexRepository.delete(tempDir);

        luceneIndexRepository.commit();

        try (DirectoryReader reader =
                 DirectoryReader.open(
                     FSDirectory.open(tempDir)
                 )) {

            assertEquals(0, reader.numDocs());
        }
        
        luceneIndexRepository.close();

    }

}
