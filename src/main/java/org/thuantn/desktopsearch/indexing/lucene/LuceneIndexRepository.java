package org.thuantn.desktopsearch.indexing.lucene;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.thuantn.desktopsearch.indexing.IndexRepository;
import org.thuantn.desktopsearch.scanner.FileMetadata;

import java.io.IOException;
import java.nio.file.Path;

public class LuceneIndexRepository implements IndexRepository {

    private final Directory directory;
    private final IndexWriter writer;
    private final LuceneDocumentMapper luceneDocumentMapper;

    public LuceneIndexRepository(Path indexPath, LuceneDocumentMapper luceneDocumentMapper) throws IOException {
        this.directory = FSDirectory.open(indexPath);
        StandardAnalyzer standardAnalyzer = new StandardAnalyzer();
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(standardAnalyzer);
        this.writer = new IndexWriter(directory, indexWriterConfig);
        this.luceneDocumentMapper = luceneDocumentMapper;
    }

    @Override
    public void upsert(FileMetadata fileMetaData) throws IOException {
        Document document = luceneDocumentMapper.mapToDocument(fileMetaData);
        writer.updateDocument(
            new Term(
                LuceneFieldNames.PATH,
                fileMetaData.path().toString()
            ), document
        );
    }

    @Override
    public void delete(Path path) throws IOException {
        writer.deleteDocuments(
            new Term(LuceneFieldNames.PATH,
                path.toString()
            ));
    }

    @Override
    public void commit() throws IOException {
        writer.commit();
    }

    @Override
    public void close() throws IOException {
        writer.close();
        directory.close();
    }

}
