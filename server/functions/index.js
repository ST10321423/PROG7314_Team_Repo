const functions = require("firebase-functions");
const admin = require("firebase-admin");
const express = require("express");
const cors = require("cors");

admin.initializeApp();
const db = admin.firestore();

const app = express();
app.use(cors({origin:true}));
app.use(express.json());

// Verify Firebase ID token from "Authorization: Bearer <token>"
async function auth(req, res, next) {
  try {
    const hdr = req.headers.authorization || "";
    const m = hdr.match(/^Bearer\s+(.+)$/i);
    if (!m) return res.status(401).json({error:"Missing token"});
    const decoded = await admin.auth().verifyIdToken(m[1]);
    req.uid = decoded.uid;
    next();
  } catch (e) {
    return res.status(401).json({error:"Invalid token"});
  }
}

const itemsCol = (uid) => db.collection("tasks").doc(uid).collection("items");
const toTask = (doc) => ({id: doc.id, ...doc.data()});

// GET /tasks  -> list tasks
app.get("/tasks", auth, async (req, res) => {
  const snap = await itemsCol(req.uid).orderBy("createdAt","desc").get();
  res.json(snap.docs.map(toTask));
});

// POST /tasks -> create task
app.post("/tasks", auth, async (req, res) => {
  const b = req.body || {};
  const title = b.title;
  const description = b.description || "";
  const dueDate = b.dueDate ? new Date(b.dueDate) : null;
  const completed = !!b.completed;

  if (!title || typeof title !== "string") {
    return res.status(400).json({error:"title is required"});
  }

  const now = admin.firestore.FieldValue.serverTimestamp();
  const data = {
    title,
    description,
    dueDate,
    completed,
    createdAt: now,
    updatedAt: now
  };

  const ref = await itemsCol(req.uid).add(data);
  const doc = await ref.get();
  res.status(201).json(toTask(doc));
});

// PUT /tasks/:id -> partial update
app.put("/tasks/:id", auth, async (req, res) => {
  const id = req.params.id;
  const b = req.body || {};

  const patch = {updatedAt: admin.firestore.FieldValue.serverTimestamp()};
  if (b.title !== undefined) patch.title = b.title;
  if (b.description !== undefined) patch.description = b.description;
  if (b.dueDate !== undefined) {
    patch.dueDate = b.dueDate ? new Date(b.dueDate) : null;
  }
  if (b.completed !== undefined) patch.completed = !!b.completed;

  const ref = itemsCol(req.uid).doc(id);
  const snap = await ref.get();
  if (!snap.exists) return res.status(404).json({error:"Not found"});

  await ref.update(patch);
  const updated = await ref.get();
  res.json(toTask(updated));
});

// DELETE /tasks/:id -> delete task
app.delete("/tasks/:id", auth, async (req, res) => {
  const id = req.params.id;
  const ref = itemsCol(req.uid).doc(id);
  const snap = await ref.get();
  if (!snap.exists) return res.status(404).json({error:"Not found"});
  await ref.delete();
  res.status(204).send();
});

// Export HTTPS function
exports.api = functions.https.onRequest(app);
