package org.example.service;


import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class IngestionService {

    private final VectorStore vectorStore;
    private final ResourcePatternResolver resolver;

    public IngestionService(VectorStore vectorStore, ResourcePatternResolver resolver) {
        this.vectorStore = vectorStore;
        this.resolver = resolver;
    }

    // Reads every file in src/main/resources/docs and returns chunks stored per file
    public Map<String, Integer> ingestAll() throws IOException {
        Map<String, Integer> result = new LinkedHashMap<>();

        for (Resource resource : resolver.getResources("classpath:documents/acme-handbook.pdf")) {
            String filename = resource.getFilename();
            if (filename == null || filename.startsWith(".") || !resource.isReadable()) {
                continue; // skip hidden files like .DS_Store
            }
            result.put(filename, ingestOne(resource, filename));
        }
        return result;
    }

    private int ingestOne(Resource resource, String filename) {
        // Remove old chunks of this file first, so re-running never creates duplicates
        vectorStore.delete(new FilterExpressionBuilder().eq("filename", filename).build());

        // 1. READ: Tika extracts text from pdf/docx/xlsx/txt
        List<Document> docs = new TikaDocumentReader(resource).get();
        System.out.println("Document list generated during ingestion : "+docs);

        // 2. CHUNK
//        List<Document> chunks = new TokenTextSplitter().apply(docs);
        List<Document> chunks = TokenTextSplitter.builder()
                .withChunkSize(500)
                .build()
                .apply(docs);

        // 3. TAG each chunk with its file name
        chunks.forEach(c -> c.getMetadata().put("filename", filename));

        // 4. EMBED + STORE
        System.out.println("List of chunks generated : "+chunks);
        vectorStore.add(chunks);
        return chunks.size();
    }
}