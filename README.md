# VectraDocs – AI-Powered Document Intelligence Platform

> Transform documents into searchable knowledge with AI-powered summarization and question answering.

VectraDocs is a full-stack AI-powered document intelligence application that enables users to upload PDF documents, generate concise summaries, and ask questions based on their document content.

The platform combines a React frontend, Java Spring Boot backend, and Python FastAPI AI service to integrate document processing, transformer-based summarization, semantic retrieval, and Retrieval-Augmented Generation (RAG).

## Demo Video

▶️ [Watch VectraDocs in Action](https://drive.google.com/file/d/1rFvDozp4C_fFWE-cTSMT9a5_9UFDw_C1/view?usp=drivesdk)

See how VectraDocs uploads a PDF, generates an AI-powered summary, and answers questions using semantic search and Retrieval-Augmented Generation (RAG).

## Key Features

- **PDF Document Upload** — Upload and process PDF documents through an interactive web interface.
- **Document Text Extraction** — Extract textual content from PDF files using PyMuPDF.
- **Intelligent Text Chunking** — Split extracted content into manageable chunks for processing and retrieval.
- **AI-Powered Summarization** — Generate document summaries using the DistilBART transformer model.
- **Semantic Search** — Use text embeddings and FAISS-based vector similarity search to retrieve relevant document content.
- **RAG-Based Question Answering** — Ask questions about uploaded documents and retrieve relevant context to generate answers.
- **Document Management** — View uploaded documents and delete documents through the application.
- **AI Service Integration** — Connect Python-based AI processing with the Java backend through REST APIs.

## Technology Stack

| Component | Technologies |
|---|---|
| Frontend | React 18, Vite, Tailwind CSS |
| Backend | Java, Spring Boot, REST APIs |
| AI Service | Python, FastAPI |
| NLP / Transformers | Hugging Face Transformers, DistilBART |
| PDF Processing | PyMuPDF |
| Embeddings | Sentence Transformers |
| Vector Search | FAISS |
| Database | MySQL |
| API Communication | Axios, HTTP REST APIs |

## System Architecture

```text
             USER
               |
               v
       React Frontend
          (Vite)
               |
               v
      Spring Boot Backend
          (REST APIs)
               |
               v
       Python FastAPI
          AI Service
               |
      +--------+--------+
      |                 |
      v                 v
 PDF Processing    AI Pipeline
  (PyMuPDF)        (DistilBART)
      |                 |
      v                 v
 Text Chunking     Summarization
      |
      v
 Text Embeddings
      |
      v
 FAISS Vector Search
      |
      v
 Relevant Document Chunks
      |
      v
 RAG-Based Question Answering
      |
      v
 Response to React Frontend
```

## Application Workflow

1. The user uploads a PDF through the React interface.
2. The Spring Boot backend receives the document and coordinates AI processing.
3. The FastAPI service extracts text from the PDF.
4. The extracted content is divided into smaller chunks.
5. The AI pipeline generates document summaries and processes semantic representations.
6. FAISS performs vector similarity search to retrieve relevant document chunks.
7. The RAG pipeline uses retrieved context to support question answering.
8. The response is returned to the frontend for display.

## Project Structure

```text
VectraDocs/
│
├── ai-service/
│   └── main.py
|   └── requirements.txt
│
├── frontend/
│   ├── public/
│   ├── src/
│   │   ├── services/
│   │   │   └── api.js
│   │   ├── App.jsx
│   │   ├── App.css
│   │   ├── index.css
│   │   └── main.jsx
│   └── package.json
│
├── src/
│   ├── main/
│   │   ├── java/com/az/backed/
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   ├── entity/
│   │   │   └── service/
│   │   └── resources/
│   │       └── application.yaml
│   │
│   └── test/
│
├── pom.xml
├── mvnw
├── mvnw.cmd
└── .gitignore
```

## API Endpoints

The frontend communicates with the Spring Boot backend through REST APIs.

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/api/documents` | Retrieve uploaded documents |
| `POST` | `/api/documents/upload` | Upload a PDF document |
| `POST` | `/api/chat/ask` | Ask a question about a document |
| `DELETE` | `/api/documents/{id}` | Delete a document |

## Running the Project Locally

### Prerequisites

- Java JDK
- Maven
- Python
- Node.js and npm
- MySQL

### 1. Clone the Repository

```bash
git clone [https://github.com/mdazharsohail452-oss/VectraDocs.git]
cd VectraDocs
```

### 2. Start the Python AI Service

```bash
cd ai-service
python -m venv .venv
```

Activate the virtual environment on Windows:

```bash
.venv\Scripts\activate
```

Install the Python dependencies:

```bash
pip install -r requirements.txt
```

Start FastAPI:

```bash
uvicorn main:app --host 127.0.0.1 --port 8000 --reload
```

AI service documentation:

```text
http://localhost:8000/docs
```

### 3. Start the Spring Boot Backend

Open a new terminal at the project root.

Configure your MySQL database and required environment variables.

Run:

```bash
mvn spring-boot:run
```

Backend:

```text
http://localhost:8080
```

### 4. Start the React Frontend

Open another terminal:

```bash
cd frontend
npm install
npm run dev
```

Frontend:

```text
http://localhost:5173
```

## Learning and Engineering Focus

This project brings together several areas of software engineering and applied AI:

- Full-stack application development
- Java Spring Boot REST API development
- Python-based AI service integration
- PDF text extraction and document processing
- Transformer-based natural language processing
- Text embeddings and vector similarity search
- Retrieval-Augmented Generation (RAG)
- Communication between independently running services

## Future Improvements

- Production cloud deployment and scalable AI hosting
- Document-level access control and authentication
- Improved retrieval quality through cross-encoder reranking
- Redis-based caching
- Persistent vector storage and optimized document indexing
- Automated testing and monitoring

---

**VectraDocs – Turning static documents into accessible, searchable knowledge.**
