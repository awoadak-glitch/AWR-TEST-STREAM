import express from 'express';
import cors from 'cors';
import fs from 'fs';
import path from 'path';
import { nanoid } from 'nanoid';

const app = express();
const root = path.resolve(process.cwd(), '..');
const dataDir = path.join(root, 'data');
app.use(cors());
app.use(express.json({ limit: '1mb' }));

function readJson(file, fallback) {
  try { return JSON.parse(fs.readFileSync(path.join(dataDir, file), 'utf8')); }
  catch { return fallback; }
}
function writeJson(file, value) {
  const full = path.join(dataDir, file);
  fs.mkdirSync(path.dirname(full), { recursive: true });
  fs.writeFileSync(full, JSON.stringify(value, null, 2));
}

app.get('/health', (_, res) => res.json({ ok: true, name: 'AWR Stream Hub API' }));
app.get('/catalog/:kind', (req, res) => {
  const kind = req.params.kind;
  res.json(readJson(`catalog/${kind}.json`, []));
});
app.get('/search', (req, res) => {
  const q = String(req.query.q || '').toLowerCase();
  const all = ['movies', 'kdrama', 'anime'].flatMap(k => readJson(`catalog/${k}.json`, []));
  res.json(all.filter(x => JSON.stringify(x).toLowerCase().includes(q)).slice(0, 80));
});
app.post('/request', (req, res) => {
  const title = String(req.body.title || '').trim();
  const kind = String(req.body.kind || 'movie');
  if (!title) return res.status(400).json({ error: 'title_required' });
  const requests = readJson('requests.json', []);
  const item = { id: nanoid(10), title, kind, status: 'queued', priority: 'instant', created_at: new Date().toISOString() };
  requests.unshift(item);
  writeJson('requests.json', requests.slice(0, 1000));
  res.json(item);
});
app.post('/subtitle/request', (req, res) => {
  const media_id = String(req.body.media_id || '').trim();
  if (!media_id) return res.status(400).json({ error: 'media_id_required' });
  const jobs = readJson('subtitle_jobs.json', []);
  const item = { id: nanoid(10), media_id, status: 'queued', outputs: ['ar.srt', 'en.srt'], created_at: new Date().toISOString() };
  jobs.unshift(item);
  writeJson('subtitle_jobs.json', jobs.slice(0, 1000));
  res.json(item);
});

const port = process.env.PORT || 8787;
app.listen(port, () => console.log(`AWR API running on ${port}`));
