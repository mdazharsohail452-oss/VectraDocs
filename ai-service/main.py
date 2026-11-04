import re
import torch
import pymupdf
import faiss
import numpy as np

from fastapi import FastAPI, UploadFile, File, HTTPException, Form
from pydantic import BaseModel, Field
from transformers import AutoTokenizer, AutoModelForSeq2SeqLM
from sentence_transformers import SentenceTransformer
from sklearn.feature_extraction.text import TfidfVectorizer
from fastapi import HTTPException

app = FastAPI(title="AI-Powered Document Intelligence API")

device = torch.device("cpu")


SUMMARY_MODEL = "sshleifer/distilbart-cnn-6-6"

EMBEDDING_MODEL = "sentence-transformers/all-MiniLM-L6-v2"

GENERATION_MODEL = "google/flan-t5-base"



# DistilBART model
summary_tokenizer = AutoTokenizer.from_pretrained(SUMMARY_MODEL)
summary_model = AutoModelForSeq2SeqLM.from_pretrained(SUMMARY_MODEL)
summary_model.to(device)
summary_model.eval()


embedding_model = SentenceTransformer(
    EMBEDDING_MODEL,
    device="cpu"
)

# Flan-T5 using 
generation_tokenizer = AutoTokenizer.from_pretrained(GENERATION_MODEL)
generation_model = AutoModelForSeq2SeqLM.from_pretrained(GENERATION_MODEL)
generation_model.to(device)
generation_model.eval()




class SummarizeRequest(BaseModel):
    text: str = Field(min_length=1)


class QuestionRequest(BaseModel):
    question: str = Field(min_length=3)
    document_id: str = Field(min_length=1)
    top_k: int = Field(default=3, ge=1, le=10)


# FAISS index stores document embeddings.
vector_index = None

# Storing original text and source information.
document_chunks = []


def extract_pdf_pages(pdf_bytes):

    doc = pymupdf.open(
        stream=pdf_bytes,
        filetype="pdf"
    )

    pages = []

    try:
        for page_number, page in enumerate(doc, start=1):

            text = page.get_text()

            text = re.sub(
                r"\s+",
                " ",
                text
            ).strip()

            if text:
                pages.append({
                    "page": page_number,
                    "text": text
                })

        return pages

    finally:
        doc.close()


def split_summary_chunks(text, chunk_size=500, overlap=50):

    token_ids = summary_tokenizer.encode(
        text,
        add_special_tokens=False
    )

    chunks = []

    step = chunk_size - overlap

    for start in range(0, len(token_ids), step):

        chunk_tokens = token_ids[start:start + chunk_size]

        if not chunk_tokens:
            break

        chunk_text = summary_tokenizer.decode(
            chunk_tokens,
            skip_special_tokens=True
        )

        chunks.append(chunk_text)

    return chunks


def split_rag_chunks(text, chunk_size=180, overlap=30):

    words = text.split()

    chunks = []

    step = chunk_size - overlap

    for start in range(0, len(words), step):

        chunk = " ".join(
            words[start:start + chunk_size]
        )

        if chunk.strip():
            chunks.append(chunk)

    return chunks


def abstractive_summarize(text):

    inputs = summary_tokenizer(
        text,
        return_tensors="pt",
        max_length=1024,
        truncation=True
    ).to(device)

    with torch.inference_mode():

        summary_ids = summary_model.generate(
            **inputs,
            num_beams=2,
            max_length=160,
            min_length=30,
            no_repeat_ngram_size=3,
            early_stopping=True
        )

    return summary_tokenizer.decode(
        summary_ids[0],
        skip_special_tokens=True
    )


def extractive_summarize(text, top_n=8):

    sentences = re.split(
        r'(?<=[.!?])\s+',
        text
    )

    sentences = [
        sentence.strip()
        for sentence in sentences
        if len(sentence.split()) >= 6
    ]

    if not sentences:
        return text

    if len(sentences) <= top_n:
        return " ".join(sentences)

    try:

        vectorizer = TfidfVectorizer(
            stop_words="english"
        )

        matrix = vectorizer.fit_transform(sentences)

        totals = matrix.sum(axis=1)
        counts = (matrix > 0).sum(axis=1)

        scores = (
            totals.A1 /
            counts.A1.clip(min=1)
        )

        ranked_indices = scores.argsort()[::-1]

        selected_indices = sorted(
            ranked_indices[:top_n]
        )

        return " ".join(
            sentences[i]
            for i in selected_indices
        )

    except ValueError:

        return text


def process_document(text):

    if not text.strip():
        raise ValueError("Document contains no readable text.")

    chunks = split_summary_chunks(text)

    # Maping stage
    chunk_summaries = [
        abstractive_summarize(chunk)
        for chunk in chunks
    ]

    # Reduce stage
    combined_text = " ".join(chunk_summaries)

    final_abstractive = abstractive_summarize(
        combined_text
    )

    # Extractive alternative.
    extractive = extractive_summarize(text)

    original_words = len(text.split())

    summary_words = len(final_abstractive.split())

    compression_ratio = (
        round(summary_words / original_words, 3)
        if original_words else 0
    )

    return {
        "summary": final_abstractive,
        "extractive_summary": extractive,
        "method": "Map-Reduce Abstractive",
        "original_words": original_words,
        "summary_words": summary_words,
        "compression_ratio": compression_ratio,
        "chunks_processed": len(chunks)
    }

def generate_embeddings(texts):

    embeddings = embedding_model.encode(
        texts,
        convert_to_numpy=True,
        normalize_embeddings=True
    )

    return np.asarray(
        embeddings,
        dtype="float32"
    )



# 11. STORING DOCUMENT CHUNKS IN FAISS

def store_document_chunks(pages, filename, document_id):

    global vector_index
    global document_chunks

    new_chunks = []

    for page_data in pages:

        page_number = page_data["page"]
        page_text = page_data["text"]

        chunks = split_rag_chunks(page_text)

        for chunk_number, chunk_text in enumerate(chunks, start=1):

            new_chunks.append({
                "document_id": document_id,
                "text": chunk_text,
                "filename": filename,
                "page": page_number,
                "chunk": chunk_number
            })

    if not new_chunks:
        raise ValueError("No readable chunks found.")

    texts = [
        item["text"]
        for item in new_chunks
    ]

    embeddings = generate_embeddings(texts)

    dimension = embeddings.shape[1]

    # Create FAISS index if it doesn't exist.
    if vector_index is None:

        vector_index = faiss.IndexFlatIP(dimension)

    vector_index.add(embeddings)

    document_chunks.extend(new_chunks)

    return len(new_chunks)


def retrieve_relevant_chunks(question, document_id, top_k=3):

    if vector_index is None or vector_index.ntotal == 0:
        raise ValueError("No documents have been uploaded yet.")

    # DEBUG: Compare requested ID with IDs stored in FAISS metadata.
    requested_id = str(document_id).strip()

    stored_ids = set(
        str(item["document_id"]).strip()
        for item in document_chunks
    )

    query_embedding = generate_embeddings([question])

    scores, indices = vector_index.search(
        query_embedding,
        vector_index.ntotal
    )

    results = []

    for score, index in zip(scores[0], indices[0]):

        if index < 0:
            continue

        chunk = document_chunks[int(index)]

        stored_id = str(chunk["document_id"]).strip()

        if stored_id != requested_id:
            continue

        results.append({
            "text": chunk["text"],
            "filename": chunk["filename"],
            "page": chunk["page"],
            "chunk": chunk["chunk"],
            "similarity_score": round(float(score), 4)
        })

        if len(results) >= top_k:
            break

    if not results:
        raise ValueError(
            f"No chunks found for document ID: {requested_id}. "
            f"Available IDs: {stored_ids}"
        )

    return results

def generate_rag_answer(question, retrieved_chunks):

    context = "\n\n".join(
        [
            f"Source: {item['filename']}, Page: {item['page']}\n"
            f"{item['text']}"
            for item in retrieved_chunks
        ]
    )

    prompt = f"""
Answer the question using only the provided context.

If the context does not contain the answer,
say that the information is not available in the document.

Context:
{context}

Question:
{question}

Answer:
"""

    inputs = generation_tokenizer(
        prompt,
        return_tensors="pt",
        max_length=512,
        truncation=True
    ).to(device)

    with torch.inference_mode():

        output_ids = generation_model.generate(
            **inputs,
            max_new_tokens=180,
            num_beams=2
        )

    answer = generation_tokenizer.decode(
        output_ids[0],
        skip_special_tokens=True
    )

    return answer



@app.get("/health")
def health_check():

    return {
        "status": "UP",
        "service": "Python AI Service",
        "summary_model": SUMMARY_MODEL,
        "embedding_model": EMBEDDING_MODEL,
        "generation_model": GENERATION_MODEL,
        "device": str(device)
    }


#text summarization api

@app.post("/summarize")
async def summarize(request: SummarizeRequest):

    try:

        if len(request.text.strip().split()) < 30:
            raise HTTPException(
                status_code=400,
                detail="Please provide at least 30 words."
            )

        return process_document(request.text)

    except HTTPException:
        raise

    except Exception as e:

        raise HTTPException(
            status_code=500,
            detail=str(e)
        )

@app.post("/summarize-pdf")
async def summarize_pdf(file: UploadFile = File(...)):

    if not file.filename.lower().endswith(".pdf"):
        raise HTTPException(
            status_code=400,
            detail="Please upload a PDF file."
        )

    try:

        pdf_bytes = await file.read()

        if not pdf_bytes:
            raise HTTPException(
                status_code=400,
                detail="Uploaded PDF is empty."
            )

        pages = extract_pdf_pages(pdf_bytes)

        text = " ".join(
            page["text"]
            for page in pages
        )

        if len(text.split()) < 30:
            raise HTTPException(
                status_code=400,
                detail="PDF contains too little readable text."
            )

        return process_document(text)

    except HTTPException:
        raise

    except Exception as e:

        raise HTTPException(
            status_code=500,
            detail=str(e)
        )

    finally:
        await file.close()



@app.post("/rag/upload")
async def upload_document(
    file: UploadFile = File(...),
    document_id: str = Form(...)
):

    if not file.filename.lower().endswith(".pdf"):
        raise HTTPException(
            status_code=400,
            detail="Please upload a PDF file."
        )

    try:

        pdf_bytes = await file.read()

        if not pdf_bytes:
            raise HTTPException(
                status_code=400,
                detail="Uploaded PDF is empty."
            )

        pages = extract_pdf_pages(pdf_bytes)

        if not pages:
            raise HTTPException(
                status_code=400,
                detail="No readable text found. Scanned PDFs require OCR."
            )

        total_words = sum(
            len(page["text"].split())
            for page in pages
        )

        if total_words < 30:
            raise HTTPException(
                status_code=400,
                detail="PDF contains too little readable text."
            )

        chunks_count = store_document_chunks(
            pages,
            file.filename,
            document_id
        )

        return {
            "message": "Document processed successfully.",
            "document_id": document_id,
            "filename": file.filename,
            "pages_processed": len(pages),
            "chunks_stored": chunks_count,
            "vector_database": "FAISS"
        }

    except HTTPException:
        raise

    except Exception as e:

        raise HTTPException(
            status_code=500,
            detail=str(e)
        )

    finally:
        await file.close()

@app.post("/rag/ask")
async def ask_question(request: QuestionRequest):

    try:

        retrieved_chunks = retrieve_relevant_chunks(
            request.question,
            request.document_id,
            request.top_k
        )
        print("\n===== RETRIEVED TEXT =====")

        for item in retrieved_chunks:
            print("Filename:", item["filename"])
            print("Page:", item["page"])
            print("Chunk:", item["chunk"])
            print("Text:", item["text"])
            print("--------------------------")

        answer = generate_rag_answer(
            request.question,
            retrieved_chunks
        )

        sources = []

        for item in retrieved_chunks:

            sources.append({
                "filename": item["filename"],
                "page": item["page"],
                "chunk": item["chunk"],
                "similarity_score": item["similarity_score"],
                "text": item["text"]
            })

        return {
            "question": request.question,
            "document_id": request.document_id,
            "answer": answer,
            "retrieved_chunks": len(retrieved_chunks),
            "sources": sources
        }

    except HTTPException:
        raise

    except Exception as e:
       import traceback

       traceback.print_exc()

       raise HTTPException(
           status_code=500,
           detail=str(e)
       )


@app.get("/rag/status")
def rag_status():

    return {
        "vector_database": "FAISS",
        "documents_indexed": len(set(
            item["document_id"] for item in document_chunks
        )),
        "total_chunks": len(document_chunks),
        "vectors_stored": (
            vector_index.ntotal
            if vector_index is not None
            else 0
        )
    }


@app.delete("/rag/documents/{document_id}")
def delete_rag_document(document_id: str):
    global vector_index
    global document_chunks

    requested_id = str(document_id).strip()

    # Keep chunks belonging to other documents
    remaining_chunks = [
        chunk for chunk in document_chunks
        if str(chunk["document_id"]).strip() != requested_id
    ]

    deleted_count = len(document_chunks) - len(remaining_chunks)

    # Rebuild FAISS only if chunks were actually removed
    if deleted_count > 0:
        if remaining_chunks:
            texts = [chunk["text"] for chunk in remaining_chunks]
            embeddings = generate_embeddings(texts)

            new_index = faiss.IndexFlatIP(embeddings.shape[1])
            new_index.add(embeddings)

            vector_index = new_index
        else:
            vector_index = None

        document_chunks = remaining_chunks

    return {
        "message": "Document deletion processed",
        "document_id": requested_id,
        "chunks_deleted": deleted_count,
        "remaining_chunks": len(document_chunks),
        "vectors_stored": 0 if vector_index is None else vector_index.ntotal
    }


