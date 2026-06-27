#!/usr/bin/env python3
import argparse, json, time, re
from pathlib import Path
ROOT = Path(__file__).resolve().parents[2]
DATA = ROOT / 'data'
CAT = DATA / 'catalog'
CAT.mkdir(parents=True, exist_ok=True)

def load(path, fallback):
    try: return json.loads(path.read_text(encoding='utf-8'))
    except Exception: return fallback

def save(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding='utf-8')

def slug(s):
    return re.sub(r'[^a-z0-9]+', '-', s.lower()).strip('-')[:80]

ap = argparse.ArgumentParser()
ap.add_argument('--title', required=True)
ap.add_argument('--kind', default='movie')
args = ap.parse_args()
kind_map = {'movie':'movies', 'movies':'movies', 'kdrama':'kdrama', 'anime':'anime', 'subtitle':'movies'}
kind = kind_map.get(args.kind, 'movies')
path = CAT / f'{kind}.json'
items = load(path, [])
media_id = f'{kind}-{slug(args.title)}'
if not any(x.get('id') == media_id for x in items):
    items.insert(0, {
        'id': media_id,
        'title': args.title,
        'kind': kind,
        'language': 'auto',
        'year': None,
        'rating': None,
        'poster': '',
        'backdrop': '',
        'overview': 'Priority user request. Metadata worker can enrich this record later.',
        'genres': ['Requested'],
        'subtitle_status': 'requested',
        'priority': 'instant',
        'created_at': time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime())
    })
save(path, items[:5000])
requests = load(DATA / 'requests.json', [])
requests.insert(0, {'title': args.title, 'kind': kind, 'media_id': media_id, 'status': 'added_to_catalog', 'created_at': time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime())})
save(DATA / 'requests.json', requests[:1000])
