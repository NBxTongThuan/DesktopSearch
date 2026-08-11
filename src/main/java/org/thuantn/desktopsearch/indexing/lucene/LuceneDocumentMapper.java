package org.thuantn.desktopsearch.indexing.lucene;

import org.apache.lucene.document.*;
import org.thuantn.desktopsearch.common.AppConstant;
import org.thuantn.desktopsearch.scanner.FileMetadata;

import java.util.Objects;

public class LuceneDocumentMapper {
    
    public Document mapToDocument(FileMetadata fileMetadata) {
        
        Objects.requireNonNull(fileMetadata, AppConstant.FILE_DATA_REQUIRE_NON_NULL);
        
        Document doc = new Document();
        
        doc.add(new StringField(LuceneFieldNames.PATH,
            fileMetadata.path().toString(),
            Field.Store.YES));
        
        doc.add(new TextField(LuceneFieldNames.NAME,
            fileMetadata.fileName(),
            Field.Store.YES));
        
        doc.add(new StringField(LuceneFieldNames.EXTENSION,
            fileMetadata.extension(),
            Field.Store.YES));
        
        doc.add(new StoredField(LuceneFieldNames.SIZE,
            fileMetadata.size()));
        
        doc.add(new StoredField(LuceneFieldNames.CREATED_AT,
            fileMetadata.createdAt().toEpochMilli()));
        
        doc.add(new StoredField(LuceneFieldNames.MODIFIED_AT,
            fileMetadata.modifiedAt().toEpochMilli()));
        
        return doc;
    }
    
}
