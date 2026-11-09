package org.example.controller;

import org.example.service.IngestionService;
import org.example.service.RagService;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

        import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final IngestionService ingestionService;
    private final RagService ragService;
    private final VectorStore vectorStore;
    private final JdbcTemplate jdbc;

    public RagController(IngestionService ingestionService, RagService ragService,
                         VectorStore vectorStore, JdbcTemplate jdbc) {
        this.ingestionService = ingestionService;
        this.ragService = ragService;
        this.vectorStore = vectorStore;
        this.jdbc = jdbc;
    }

    // Load all files from src/main/resources/docs
    @PostMapping("/ingest")
    public Map<String, Integer> ingest() throws IOException {
        return ingestionService.ingestAll();
    }

    // Full RAG: retrieve + LLM answer + sources
    @GetMapping("/ask")
    public RagService.RagAnswer ask(@RequestParam String q,
                                    @RequestParam(defaultValue = "4") int topK) {
        return ragService.ask(q, topK);
    }

    // Retrieval only, no LLM (for debugging)
    @GetMapping("/search")
    public List<Map<String, Object>> search(@RequestParam String q,
                                            @RequestParam(defaultValue = "4") int topK) {
        return vectorStore.similaritySearch(SearchRequest.builder().query(q).topK(topK).build())
                .stream()
                .map(d -> Map.<String, Object>of(
                        "text", d.getText(),
                        "metadata", d.getMetadata()))
                .toList();
    }

    // Wipe all stored chunks
//    @DeleteMapping("/clear")
//    public String clear() {
//        jdbc.execute("TRUNCATE vector_store");
//        return "vector_store cleared";
//    }
}