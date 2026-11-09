package org.example.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RagService {

    public record RagAnswer(String answer, List<String> sources) {}

    private static final String SYSTEM_TEMPLATE = """
        You are an assistant that answers questions using the provided context.

        Rules:
        - Use only the information in the context. Do not use outside knowledge.
        - If the context contains the answer, answer clearly and briefly.
        - If the context does not contain the answer, reply exactly: I don't know.
        - Do not mention the word "context" in your answer.

        Context:
        {context}
        """;

    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    public RagService(ChatClient.Builder builder, VectorStore vectorStore) {
        this.vectorStore = vectorStore;
        this.chatClient = builder.build();
    }

    public RagAnswer ask(String question, int topK) {
        // 1. RETRIEVE: embed the question, find closest chunks in pgvector
        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(question)
                        .topK(topK)
                        .similarityThreshold(0.4)
                        .build());

        System.out.println("Docs fetched chunks from pg_vector having size : "+docs.size());
        if (docs.isEmpty()) {
            return new RagAnswer("I don't know (no relevant content found).", List.of());
        }

        // 2. AUGMENT: join the chunks into one context string
        String context = docs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n---\n"));

        // 3. GENERATE: llama3.2 answers using only that context
        String answer = chatClient.prompt()
                .system(s -> s.text(SYSTEM_TEMPLATE).param("context", context))
                .user(question)
                .call()
                .content();

        List<String> sources = docs.stream()
                .map(d -> String.valueOf(d.getMetadata().getOrDefault("filename", "unknown")))
                .distinct()
                .toList();

        return new RagAnswer(answer, sources);
    }
}