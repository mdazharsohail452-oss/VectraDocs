import axios from "axios";

const api = axios.create({
  baseURL: "http://localhost:8080",
});

export const getDocuments = async () => {
  const response = await api.get("/api/documents");
  return response.data;
};

export const uploadDocument = async (file) => {
  const formData = new FormData();
  formData.append("file", file);
  const response = await api.post("/api/documents/upload", formData);
  return response.data;
};

export const askQuestion = async (question, documentId) => {
  const response = await api.post("/api/chat/ask", {
    question,
    document_id: documentId,
    top_k: 3,
  });
  return response.data;
};

export const deleteDocument = async (documentId) => {
  const response = await api.delete(
    `/api/documents/${encodeURIComponent(documentId)}`
  );
  return response.data;
};

export default api;
