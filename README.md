# Spring AI RAG Demo

RAG with Spring Boot, Spring AI, Ollama and pgvector. Reads PDFs from the project folder and answers questions using only their content.

## Stack

Java 17, Spring Boot 4.1, Spring AI 2.0, Ollama (`llama3.2`, `nomic-embed-text`), PostgreSQL + pgvector

## Setup

1. Pull the models:
   ```bash
   ollama pull llama3.2
   ollama pull nomic-embed-text
   ```
2. Start PostgreSQL with pgvector on port `5433`, database `springai_rag`, and run `CREATE EXTENSION IF NOT EXISTS vector;`
3. Set the datasource username and password in `src/main/resources/application.properties`
4. Put a text-based PDF in `src/main/resources/docs/`
5. Run the app (port `8089`)

## API

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/rag/ingest` | Load PDFs from `docs/` into pgvector |
| `GET` | `/api/rag/ask?q=...` | Answer with source file names |
| `GET` | `/api/rag/search?q=...` | Show retrieved chunks (no LLM) |
| `DELETE` | `/api/rag/clear` | Delete all stored chunks |

## Example

```bash
curl -X POST http://localhost:8089/api/rag/ingest
curl "http://localhost:8089/api/rag/ask?q=what%20is%20company%20leave%20policy"
```

## Notes

- `dimensions=768` must match the embedding model. If you change the model, drop `vector_store` and ingest again.
- After adding a file to `docs/`, restart the app and call `/ingest`.
