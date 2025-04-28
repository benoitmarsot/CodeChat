## 🧠 Tokenization?

---

### 🔄 Vector Flow Overview

    [Raw text or code] 
        ↓
    ✅ Tokenizer (e.g., OpenAI's tiktoken, Hugging Face, etc.)
        Use for counting token
        We implemented tiktoken
        ↓
    ✅ Embedding model (e.g., OpenAI, BAAI, HuggingFace)
        we implemented: OpenAI, HuggingFace
        ↓
    ✅ Vector (e.g., float[1536])
        ↓
    ✅ Stored in PostgreSQL using pgvector

> **pgvector only stores and indexes vectors.** It has no idea how the vectors were created or tokenized.

---

### 🧰 What About PostgreSQL's Full-Text Search?

PostgreSQL does include full-text search features like `to_tsvector()` and stemming, but these are for **keyword-based search**, not embeddings.

Example hybrid search (optional):

    SELECT *
    FROM chunks
    WHERE to_tsvector('english', content) @@ plainto_tsquery('java')
    ORDER BY embedding <-> '[0.1, 0.2, ...]'
    LIMIT 10;

> Here, `to_tsvector` does tokenization, but it’s unrelated to vector search.

---

### 🧰 Embedded search overview

**Embedded (vector) search** is a process where natural language or code is converted into high-dimensional vectors (embeddings) using a machine learning model. These vectors capture the semantic meaning of the input, allowing for similarity search based on meaning rather than exact keywords.

**How it works:**

1. **Input:** User provides a query (sentence, code, etc.).
2. **Embedding:** The query is converted into a vector using the same embedding model used for your stored data.
3. **Similarity Search:** The vector is compared (using cosine similarity or Euclidean distance) to vectors stored in the database (e.g., with pgvector).
4. **Result:** The most similar chunks (content) are returned, ranked by vector similarity.

**Key points:**

- Embedded search finds semantically similar content, even if the exact words don’t match.
- It complements or can be combined with keyword search (e.g., PostgreSQL full-text search) for hybrid approaches.
- All tokenization and embedding logic is handled by your application code, not by PostgreSQL.

---

### ✅ Summary

Task                      | Who Handles It?
--------------------------|----------------------------
Tokenization              | ✅ **Your code** (e.g. `tiktoken`)
Embedding generation      | ✅ **Your code, or Ai vendor** (e.g. HugginFace (local), or OpenAI API)
Vector storage/search     | ✅ **pgvector** in PostgreSQL