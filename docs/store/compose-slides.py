# Play Store slides (1080x1920) in an axio-like style with Zelu's colours. Crops are given in the 909-px-wide
# preview coordinates and scaled to the 1440-px originals.
import base64, io, subprocess, os
from PIL import Image
K = 1440 / 909
def crop(name, x0, y0, x1, y1):
    im = Image.open(f'raw/{name}.jpg').convert('RGB').crop((int(x0*K), int(y0*K), int(x1*K), int(y1*K)))
    b = io.BytesIO(); im.save(b, 'PNG'); return im.size, 'data:image/png;base64,' + base64.b64encode(b.getvalue()).decode()

SLIDES = [  # file, headline, theme, screen crop (y0,y1), pop-out (file, x0,y0,x1,y1, top px on slide) or None
    ('home', 'Every rupee, tracked<br>from your bank SMS', 'dark', (60, 1180), ('home', 50, 70, 860, 700, 600)),
    ('spend', 'See where your<br>money goes', 'sand', (290, 1700), None),
    ('categories', 'Know what changed<br>since last month', 'dark', (290, 1700), ('categories', 70, 430, 840, 650, 820)),
    ('card', 'Every credit card,<br>cycle by cycle', 'sand', (100, 1760), ('homecards', 88, 940, 822, 1408, 1240)),
    ('dues', 'Bills and EMIs,<br>before they\'re due', 'dark', (140, 1530), None),
    ('trends', 'Your money,<br>month by month', 'sand', (90, 1450), None),
    ('privacy', 'No internet. No ads.<br>Stays on your phone.', 'dark', (200, 1860), None),
]
W = 800
for i, (f, title, theme, (y0, y1), pop) in enumerate(SLIDES, 1):
    dark = theme == 'dark'
    bg, ink, line, dots = ('#1E1B18', '#F9F8F6', '#A8845A', '#3A342E') if dark else ('#A8845A', '#1E1B18', '#1E1B18', '#B99A70')
    x0, x1 = (50, 859) if f == 'privacy' else (0, 909)
    (cw, ch), src = crop(f, x0, y0, x1, y1)
    h = int(W * ch / cw)
    popdiv = ''
    if pop:
        pf, px0, py0, px1, py1, ptop = pop
        (pw, ph), psrc = crop(pf, px0, py0, px1, py1)
        pw2 = 900; ph2 = int(pw2 * ph / pw)
        popdiv = f'<img class="pop" src="{psrc}" style="width:{pw2}px;height:{ph2}px;left:{(1080-pw2)//2}px;top:{ptop}px">'
    scr_bg = '#EFE9E3' if f == 'privacy' else '#F9F8F6'
    html = f'''<!doctype html><html><head><meta charset="utf-8"><style>
html,body{{margin:0;width:1080px;height:1920px;overflow:hidden}}
body{{background:{bg};font-family:Inter,sans-serif;position:relative}}
h1{{position:absolute;top:150px;left:60px;right:60px;margin:0;text-align:center;color:{ink};font-size:80px;line-height:1.1;font-weight:800;letter-spacing:-2.5px}}
.phone{{position:absolute;left:{(1080-W)//2-20}px;top:520px;width:{W}px;padding:20px 20px 0;border-radius:76px 76px 0 0;background:#0E0C0B;
  box-shadow:0 0 0 3px #4A433C,0 40px 90px rgba(0,0,0,.4)}}
.notch{{height:56px;background:{scr_bg};border-radius:56px 56px 0 0;position:relative}}
.notch:after{{content:"";position:absolute;left:50%;top:18px;width:26px;height:26px;margin-left:-13px;border-radius:50%;background:#0E0C0B}}
.scr{{display:block;width:{W}px;height:{h}px;background:{scr_bg}}}
.pop{{position:absolute;border-radius:34px;box-shadow:0 0 0 5px {line},0 0 60px {line}80,0 30px 70px rgba(0,0,0,.45)}}
svg{{position:absolute;left:0;top:0}}
</style></head><body>
<svg width="1080" height="1920"><g fill="{dots}">{''.join(f'<circle cx="{820+c*48}" cy="{1580+r*48}" r="8"/>' for r in range(6) for c in range(6))}</g>
<path d="M-60,1500 C120,1330 60,1060 250,960" stroke="{line}" stroke-width="8" fill="none" stroke-linecap="round"/>
<path d="M250,960 l-52,4 M250,960 l-10,50" stroke="{line}" stroke-width="18" stroke-linecap="round"/>
<path d="M1140,640 C980,700 1000,860 900,930" stroke="{line}" stroke-width="8" fill="none" stroke-linecap="round" opacity=".7"/></svg>
<h1>{title}</h1>
<div class="phone"><div class="notch"></div><img class="scr" src="{src}"></div>
{popdiv}
</body></html>'''
    name = f'slide-{i}-{f}'
    open(name + '.html', 'w').write(html)
    subprocess.run(['/opt/pw-browsers/chromium-1194/chrome-linux/chrome', '--headless', '--no-sandbox', '--disable-gpu', '--hide-scrollbars',
        '--force-device-scale-factor=1', '--window-size=1080,2120', f'--screenshot={name}-tall.png', name + '.html'], capture_output=True)
    Image.open(name + '-tall.png').crop((0, 0, 1080, 1920)).convert('RGB').save(name + '.png')
    os.remove(name + '-tall.png'); print(name + '.png')
