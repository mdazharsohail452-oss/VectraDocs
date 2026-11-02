import { useEffect, useState } from "react";
import {
  LayoutDashboard, Files, MessageSquare, FileText,
  Sparkles, Database, Activity, Plus, Send, Bot,
  User, Loader2, Trash2
} from "lucide-react";
import {
  getDocuments,
  uploadDocument,
  askQuestion,
  deleteDocument
} from "./services/api";

export default function App() {
  const [documents, setDocuments] = useState([]);
  const [page, setPage] = useState("dashboard");
  const [selectedDoc, setSelectedDoc] = useState("");
  const [question, setQuestion] = useState("");
  const [messages, setMessages] = useState([]);
  const [loading, setLoading] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [deletingId, setDeletingId] = useState("");
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  const loadDocuments = async () => {
    try {
      const data = await getDocuments();
      setDocuments(data);
      setSelectedDoc(current => {
        if (current && data.some(doc => doc.id === current)) return current;
        return data.length ? data[0].id : "";
      });
      setError("");
      return data;
    } catch {
      setError("Could not load documents. Check Spring Boot.");
      return [];
    }
  };

  useEffect(() => {
    loadDocuments();
  }, []);

  const handleUpload = async (e) => {
    const file = e.target.files[0];
    if (!file) return;
    if (!file.name.toLowerCase().endsWith(".pdf")) {
      setError("Please select a PDF file.");
      e.target.value = "";
      return;
    }

    setUploading(true);
    setError("");
    setNotice("");
    try {
      const result = await uploadDocument(file);
      const updatedDocuments = await getDocuments();
      setDocuments(updatedDocuments);
      const uploadedId = result.document_id || result.documentId;
      const uploadedDocument = updatedDocuments.find(doc => doc.id === uploadedId)
        || updatedDocuments.find(doc => doc.filename === file.name);
      if (uploadedDocument) setSelectedDoc(uploadedDocument.id);
      setMessages([]);
      setPage("chat");
      setNotice("PDF uploaded and indexed successfully.");
    } catch (err) {
      setError(err.response?.data?.message || err.response?.data?.detail || "PDF upload failed.");
    } finally {
      setUploading(false);
      e.target.value = "";
    }
  };

  const handleDelete = async (doc) => {
    const confirmed = window.confirm(
      `Delete "${doc.filename}"? This removes its document record and FAISS vectors. This action cannot be undone.`
    );
    if (!confirmed) return;

    setDeletingId(doc.id);
    setError("");
    setNotice("");
    try {
      await deleteDocument(doc.id);
      const updatedDocuments = await getDocuments();
      setDocuments(updatedDocuments);

      if (selectedDoc === doc.id) {
        setSelectedDoc(updatedDocuments.length ? updatedDocuments[0].id : "");
        setMessages([]);
      }
      setNotice(`Deleted ${doc.filename} successfully.`);
    } catch (err) {
      setError(
        err.response?.data?.message
        || err.response?.data?.detail
        || "Document deletion failed. Check that the Python delete endpoint is running."
      );
    } finally {
      setDeletingId("");
    }
  };

  const handleAsk = async (e) => {
    e.preventDefault();
    if (!question.trim() || !selectedDoc || loading) return;

    const currentQuestion = question.trim();
    setMessages(prev => [...prev, { role: "user", text: currentQuestion }]);
    setQuestion("");
    setLoading(true);
    setError("");

    try {
      const result = await askQuestion(currentQuestion, selectedDoc);
      setMessages(prev => [...prev, {
        role: "assistant",
        text: result.answer || "No answer returned.",
        sources: result.sources || []
      }]);
    } catch (err) {
      setError(err.response?.data?.message || err.response?.data?.detail || "Could not get an answer.");
    } finally {
      setLoading(false);
    }
  };

  const selectedDocument = documents.find(d => d.id === selectedDoc);

  return (
    <div className="min-h-screen bg-[#09090b] text-zinc-100 flex">
      <aside className="w-64 border-r border-zinc-800 p-6 flex flex-col">
        <div className="text-xl font-bold mb-2">
          <span className="text-teal-400">✦</span> VectraDocs
        </div>
        <p className="text-xs text-zinc-500 mb-10">AI-Powered Document Intelligence</p>

        <p className="text-xs text-zinc-600 mb-3">WORKSPACE</p>

        <button onClick={() => setPage("dashboard")}
          className={`flex gap-3 p-3 rounded-xl mb-2 ${page === "dashboard" ? "bg-teal-500/15 text-teal-300" : "text-zinc-400 hover:bg-zinc-800"}`}>
          <LayoutDashboard size={18}/> Dashboard
        </button>

        <button onClick={() => setPage("documents")}
          className={`flex gap-3 p-3 rounded-xl mb-2 ${page === "documents" ? "bg-teal-500/15 text-teal-300" : "text-zinc-400 hover:bg-zinc-800"}`}>
          <Files size={18}/> Documents
        </button>

        <button onClick={() => setPage("chat")}
          className={`flex gap-3 p-3 rounded-xl ${page === "chat" ? "bg-teal-500/15 text-teal-300" : "text-zinc-400 hover:bg-zinc-800"}`}>
          <MessageSquare size={18}/> AI Chat
        </button>

        <div className="mt-auto text-xs text-zinc-600">Document Intelligence Workspace</div>
      </aside>

      <main className="flex-1 p-8 md:p-10 overflow-y-auto">
        <header className="flex justify-between items-center mb-10">
          <div>
            <h1 className="text-2xl font-semibold">
              {page === "chat" ? "AI Document Chat" : page === "documents" ? "Documents" : "Dashboard"}
            </h1>
            <p className="text-sm text-zinc-500 mt-1">Your intelligent document workspace</p>
          </div>
          <label className="bg-teal-600 hover:bg-teal-500 px-4 py-2.5 rounded-xl text-sm cursor-pointer flex items-center gap-2">
            {uploading ? <Loader2 size={16} className="animate-spin"/> : <Plus size={16}/>}
            {uploading ? "Uploading..." : "Upload PDF"}
            <input type="file" accept=".pdf,application/pdf" className="hidden" onChange={handleUpload} disabled={uploading}/>
          </label>
        </header>

        {error && <div className="mb-6 rounded-xl border border-red-500/30 bg-red-500/10 p-3 text-sm text-red-300">{error}</div>}
        {notice && <div className="mb-6 rounded-xl border border-emerald-500/30 bg-emerald-500/10 p-3 text-sm text-emerald-300">{notice}</div>}

        {page === "dashboard" && (
          <>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-5 mb-8">
                <Stat icon={<Files className="text-teal-400"/>} label="Total Documents" value={documents.length}/>
                <Stat icon={<Database className="text-teal-400"/>} label="Vector Database" value="FAISS"/>
                <Stat icon={<Activity className="text-teal-400"/>} label="AI Pipeline" value="Active"/>
            </div>

            <section className="bg-teal border border-zinc-800 rounded-2xl p-6">
              <h2 className="font-semibold mb-5">Recent Documents</h2>
              <DocumentList documents={documents} onDelete={handleDelete} deletingId={deletingId}/>
            </section>

            <button onClick={() => setPage("chat")} className="mt-6 w-full text-left bg-teal-600/10 border border-teal-500/20 rounded-2xl p-6 hover:bg-teal-600/15">
              <Sparkles className="text-teal-400 mb-3"/>
              <h3 className="font-semibold">Chat with your documents</h3>
              <p className="text-sm text-zinc-400 mt-1">Ask questions and get AI-powered answers from your PDFs.</p>
            </button>
          </>
        )}

        {page === "documents" && (
          <section className="bg-teal border border-zinc-800 rounded-2xl p-6">
            <h2 className="font-semibold mb-5">Your Documents</h2>
            <DocumentList documents={documents} onDelete={handleDelete} deletingId={deletingId}/>
          </section>
        )}

        {page === "chat" && (
          <div className="max-w-4xl mx-auto">
            <section className="bg-[#121215] border border-zinc-800 rounded-2xl p-5 mb-5">
              <label className="text-sm text-zinc-400 block mb-2">Select a document</label>
              <select value={selectedDoc} onChange={e => { setSelectedDoc(e.target.value); setMessages([]); }}
                className="w-full bg-[#09090b] border border-zinc-700 rounded-xl p-3 text-sm outline-none">
                <option value="">Choose a PDF</option>
                {documents.map(doc => <option key={doc.id} value={doc.id}>{doc.filename}</option>)}
              </select>
              {selectedDocument && <p className="text-xs text-zinc-500 mt-2">{selectedDocument.chunksStored} chunks indexed</p>}
              {!documents.length && <p className="text-xs text-zinc-500 mt-2">Upload a PDF to start chatting.</p>}
            </section>

            <section className="bg-[#121215] border border-zinc-800 rounded-2xl min-h-[420px] p-6 flex flex-col">
              <div className="flex-1 space-y-5">
                {messages.length === 0 && (
                  <div className="text-center py-24">
                    <Sparkles size={32} className="text-teal-400 mx-auto mb-4"/>
                    <h2 className="font-semibold">Ask your document anything</h2>
                    <p className="text-sm text-zinc-500 mt-2">Select a PDF and enter your question below.</p>
                  </div>
                )}

                {messages.map((msg, i) => (
                  <div key={i} className="flex gap-3">
                    <div className={`w-8 h-8 shrink-0 rounded-lg flex items-center justify-center ${msg.role === "assistant" ? "bg-teal-500/20 text-teal-300" : "bg-zinc-800 text-zinc-300"}`}>
                      {msg.role === "assistant" ? <Bot size={17}/> : <User size={17}/>}
                    </div>
                    <div className="flex-1">
                      <p className="text-xs text-zinc-500 mb-2">{msg.role === "assistant" ? "VectraDocs AI" : "You"}</p>
                      <p className="text-sm leading-7 whitespace-pre-wrap text-zinc-200">{msg.text}</p>
                      {msg.sources?.length > 0 && (
                        <div className="mt-3 text-xs text-zinc-500">
                          Sources: {msg.sources.map((s, j) => (
                            <span key={j} className="inline-block bg-zinc-800 rounded-md px-2 py-1 mr-2 mb-2">
                              {typeof s === "string" ? s : s.filename || s.source || `Source ${j + 1}`}
                            </span>
                          ))}
                        </div>
                      )}
                    </div>
                  </div>
                ))}

                {loading && <div className="text-sm text-teal-300 flex gap-2 items-center"><Loader2 size={16} className="animate-spin"/> Searching document and generating answer...</div>}
              </div>

              <form onSubmit={handleAsk} className="flex gap-3 mt-6 border-t border-zinc-800 pt-5">
                <input value={question} onChange={e => setQuestion(e.target.value)}
                  placeholder="Ask a question about your PDF..."
                  className="flex-1 bg-[#09090b] border border-zinc-700 rounded-xl px-4 py-3 text-sm outline-none focus:border-teal-500"
                />
                <button disabled={loading || !selectedDoc || !question.trim()}
                  className="bg-teal-600 hover:bg-teal-500 disabled:opacity-40 rounded-xl px-5">
                  <Send size={18}/>
                </button>
              </form>
            </section>
          </div>
        )}
      </main>
    </div>
  );
}

function Stat({ icon, label, value }) {
  return (
    <div className="bg-[#121215] border border-zinc-800 rounded-2xl p-5">
      <div className="text-violet-400 mb-4">{icon}</div>
      <p className="text-sm text-zinc-500">{label}</p>
      <p className="text-2xl font-semibold mt-1">{value}</p>
    </div>
  );
}

function DocumentList({ documents, onDelete, deletingId }) {
  if (!documents.length) return <p className="text-sm text-zinc-500">No documents uploaded yet.</p>;

  return (
    <div className="space-y-3">
      {documents.map(doc => (
        <div key={doc.id} className="flex items-center gap-4 border-b border-zinc-800 last:border-0 pb-3">
          <FileText className="text-violet-400" size={20}/>
          <div className="flex-1 min-w-0">
            <p className="text-sm truncate">{doc.filename}</p>
            <p className="text-xs text-zinc-500 mt-1">{doc.pagesProcessed} pages · {doc.chunksStored} chunks</p>
          </div>
          <span className="text-xs text-emerald-400">Indexed</span>
          <button
            type="button"
            onClick={() => onDelete(doc)}
            disabled={Boolean(deletingId)}
            title={`Delete ${doc.filename}`}
            aria-label={`Delete ${doc.filename}`}
            className="p-2 rounded-lg text-zinc-500 hover:text-red-400 hover:bg-red-500/10 disabled:opacity-40"
          >
            {deletingId === doc.id
              ? <Loader2 size={17} className="animate-spin"/>
              : <Trash2 size={17}/>}
          </button>
        </div>
      ))}
    </div>
  );
}
