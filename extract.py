import json
import os

transcript_path = r'C:\Users\matsu\.gemini\antigravity\brain\cc377c0b-6fe0-4942-92e2-5d237540c6f8\.system_generated\logs\transcript.jsonl'
output_path = r'C:\Users\matsu\template-mod-template-1.20.1\transcript_search.txt'

with open(transcript_path, 'r', encoding='utf-8') as f_in, open(output_path, 'w', encoding='utf-8') as f_out:
    for line in f_in:
        if '"type":"USER_INPUT"' in line:
            data = json.loads(line)
            content = data.get('content', '')
            if 'パーセント' in content or '%' in content or '倍' in content or '確率' in content or 'action' in content:
                f_out.write(content.replace('\n', ' ') + '\n')
