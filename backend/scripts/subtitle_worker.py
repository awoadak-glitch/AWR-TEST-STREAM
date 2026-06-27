#!/usr/bin/env python3
"""Demand subtitle worker template.
For legal use: provide an owned video/audio file or a licensed transcript.
This demo creates valid AR/EN SRT placeholders and catalog status updates.
"""
import argparse, json, time
from pathlib import Path
ROOT = Path(__file__).resolve().parents[2]
DATA = ROOT / 'data'

def save(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(data, encoding='utf-8')

def srt(lang):
    if lang == 'ar':
        return "1\n00:00:01,000 --> 00:00:04,000\nتم إنشاء طلب الترجمة بنجاح.\n\n2\n00:00:04,500 --> 00:00:08,000\nاربط العامل بملف فيديو قانوني لإنتاج ترجمة كاملة.\n"
    return "1\n00:00:01,000 --> 00:00:04,000\nSubtitle request created successfully.\n\n2\n00:00:04,500 --> 00:00:08,000\nConnect the worker to a legal video file to produce full subtitles.\n"

ap = argparse.ArgumentParser()
ap.add_argument('--media-id', required=True)
ap.add_argument('--source-language', default='auto')
args = ap.parse_args()
out = DATA / 'subtitles' / args.media_id
save(out / 'ar.srt', srt('ar'))
save(out / 'en.srt', srt('en'))
index_path = DATA / 'subtitles_index.json'
try: index = json.loads(index_path.read_text(encoding='utf-8'))
except Exception: index = {}
index[args.media_id] = {'ar': f'data/subtitles/{args.media_id}/ar.srt', 'en': f'data/subtitles/{args.media_id}/en.srt', 'status': 'ready', 'updated_at': time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime())}
index_path.write_text(json.dumps(index, ensure_ascii=False, indent=2), encoding='utf-8')
