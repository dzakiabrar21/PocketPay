"""Composes portfolio mockups (phones on a soft gradient) from website/public/screens.

Usage: python docs/portfolio/make_mockups.py
"""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parents[2]
SCREENS = ROOT / "website" / "public" / "screens"
OUT = Path(__file__).resolve().parent
S = 2                      # supersampling factor
CW, CH = 2000, 1500        # final canvas
PW = 470                   # screen width inside each phone
BEZEL = 16


def gradient(w, h, c1, c2, c3):
    base = Image.new("RGB", (w, h))
    px = base.load()
    for y in range(h):
        for x in range(w):
            t = (x / w * 0.55 + y / h * 0.45)
            a, b, k = (c1, c2, t / 0.5) if t < 0.5 else (c2, c3, (t - 0.5) / 0.5)
            px[x, y] = tuple(int(a[i] + (b[i] - a[i]) * k) for i in range(3))
    return base


def phone(name, width):
    shot = Image.open(SCREENS / f"{name}.webp").convert("RGB")
    sw = width * S
    sh = round(shot.height * sw / shot.width)
    shot = shot.resize((sw, sh), Image.LANCZOS)
    b = BEZEL * S
    fw, fh = sw + 2 * b, sh + 2 * b
    r_out = int(fw * 0.13)
    frame = Image.new("RGBA", (fw, fh), (0, 0, 0, 0))
    d = ImageDraw.Draw(frame)
    d.rounded_rectangle((0, 0, fw - 1, fh - 1), r_out, fill=(18, 20, 26, 255), outline=(70, 74, 86, 255), width=3 * S)
    mask = Image.new("L", (sw, sh), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, sw - 1, sh - 1), r_out - b, fill=255)
    frame.paste(shot, (b, b), mask)
    # dynamic island
    iw, ih = int(sw * 0.27), int(sw * 0.075)
    ImageDraw.Draw(frame).rounded_rectangle(((fw - iw) // 2, b + int(sw * 0.03), (fw + iw) // 2, b + int(sw * 0.03) + ih), ih // 2, fill=(10, 11, 14, 255))
    return frame


def compose(names, out, palette, offsets, widths=None):
    bg = gradient(CW * S // 4, CH * S // 4, *palette).resize((CW * S, CH * S), Image.BICUBIC)
    canvas = bg.convert("RGBA")
    widths = widths or [PW] * len(names)
    n = len(names)
    frames = [phone(nm, w) for nm, w in zip(names, widths)]
    total = sum(f.width for f in frames)
    gap = (CW * S - total) // (n + 1)
    x = gap
    for f, oy in zip(frames, offsets):
        y = oy * S
        shadow = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
        sh = Image.new("RGBA", f.size, (20, 30, 20, 110))
        shadow.paste(sh, (x + 10 * S, y + 40 * S), f.split()[3])
        canvas.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(38 * S)))
        canvas.alpha_composite(f, (x, y))
        x += f.width + gap
    canvas.convert("RGB").resize((CW, CH), Image.LANCZOS).save(OUT / out, optimize=True)
    print("wrote", out)


SAGE = ((232, 240, 214), (206, 224, 176), (176, 205, 150))
LAV = ((226, 224, 252), (190, 186, 244), (158, 152, 226))
MINT = ((224, 240, 236), (186, 220, 214), (150, 196, 196))
CASCADE = [150, 230, 310]
ARCH = [230, 120, 230]

for lang in ("en", "id"):
    sfx = "" if lang == "en" else "-id"
    compose([f"splash-{lang}-light", f"home-{lang}-dark", f"pockets-{lang}-dark"], f"01-overview{sfx}.png", SAGE, CASCADE)
    compose([f"pay-{lang}-dark", f"pin-{lang}-dark", f"success-{lang}-dark"], f"02-payment{sfx}.png", LAV, ARCH, [450, 500, 450])
    compose([f"stats-{lang}-dark", f"receipt-{lang}-dark", f"profile-{lang}-dark"], f"03-insights{sfx}.png", MINT, ARCH, [450, 500, 450])
    compose([f"home-{lang}-light", f"pockets-{lang}-light", f"stats-{lang}-light"], f"04-light-mode{sfx}.png", SAGE, CASCADE)
