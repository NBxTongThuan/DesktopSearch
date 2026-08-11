package org.thuantn.desktopsearch;

import org.thuantn.desktopsearch.config.AppConfig;
import org.thuantn.desktopsearch.indexing.IndexingService;
import org.thuantn.desktopsearch.indexing.lucene.LuceneDocumentMapper;
import org.thuantn.desktopsearch.indexing.lucene.LuceneIndexRepository;
import org.thuantn.desktopsearch.scanner.FileMetadataReader;
import org.thuantn.desktopsearch.scanner.FileScanner;
import org.thuantn.desktopsearch.scanner.NioFileMetadataReader;
import org.thuantn.desktopsearch.scanner.NioFileScanner;

import java.nio.file.Path;

public class DesktopSearchApplication {
    
    public static void main(String[] args) throws Exception {
        
        LuceneDocumentMapper luceneDocumentMapper = new LuceneDocumentMapper();
        
        FileScanner fileScanner = new NioFileScanner();
        
        FileMetadataReader fileMetadataReader = new NioFileMetadataReader();
        
        LuceneIndexRepository indexRepository = new LuceneIndexRepository(Path.of(AppConfig.get("app.index.path")), luceneDocumentMapper);
        
        IndexingService indexingService = new IndexingService(
            fileScanner, fileMetadataReader, indexRepository
        );
        
        indexingService.index(Path.of("/home/thuantn/IdeaProjects/cobol/cobol-rules-pack/pack/rules"));
        
    }
    
}
