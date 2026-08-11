package org.thuantn.desktopsearch.indexing;

import org.thuantn.desktopsearch.scanner.FileMetadata;
import org.thuantn.desktopsearch.scanner.FileMetadataReader;
import org.thuantn.desktopsearch.scanner.FileScanner;

import java.io.IOException;
import java.nio.file.Path;

public class IndexingService {
    
    private final FileScanner fileScanner;
    private final FileMetadataReader fileMetadataReader;
    private final IndexRepository indexRepository;
    
    public IndexingService(FileScanner fileScanner, FileMetadataReader fileMetadataReader, IndexRepository indexRepository) {
        this.fileScanner = fileScanner;
        this.fileMetadataReader = fileMetadataReader;
        this.indexRepository = indexRepository;
    }
    
    public void index(Path root) throws IOException {
        
        fileScanner.scan(root,
            path -> {
                try {
                    indexRepository.upsert(
                        fileMetadataReader.read(path)
                    );
                } catch (IOException e) {
//                    throw new RuntimeException(e);
                }
            }
        );
        
        indexRepository.commit();
        indexRepository.close();
    }
    
}
