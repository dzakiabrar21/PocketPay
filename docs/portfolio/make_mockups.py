"""Composes portfolio mockups from website/public/screens.

Two layouts: `cover` (two overlapping phones, cropped at the bottom) and `feature`
(one phone next to a short explanation). Run: python docs/portfolio/make_mockups.py
"""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parents[2]
SCREENS = ROOT / "website" / "public" / "screens"
OUT = Path(__file__).resolve().parent
FONTS = Path("C:/Windows/Fonts")
S = 2
BEZEL = 16

SAGE = ((236, 244, 222), (214, 230, 188), (186, 212, 160))
LAV = ((232, 230, 254), (204, 200, 248), (176, 170, 232))
MINT = ((230, 244, 240), (196, 226, 220), (162, 204, 202))
SAND = ((250, 244, 228), (240, 232, 200), (222, 214, 170))
INK = (16, 28, 52)
MUTED = (62, 76, 98)
LIME = (84, 140, 12)


def font(name, size):
    return ImageFont.truetype(str(FONTS / name), size * S)


def gradient(w, h, c1, c2, c3):
    small = Image.new("RGB", (w // 8, h // 8))
    px = small.load()
    for y in range(small.height):
        for x in range(small.width):
            t = x / small.width * 0.55 + y / small.height * 0.45
            a, b, k = (c1, c2, t / 0.5) if t < 0.5 else (c2, c3, (t - 0.5) / 0.5)
            px[x, y] = tuple(int(a[i] + (b[i] - a[i]) * k) for i in range(3))
    return small.resize((w, h), Image.BICUBIC)


def phone(name, width):
    shot = Image.open(SCREENS / f"{name}.webp").convert("RGB")
    sw = width * S
    sh = round(shot.height * sw / shot.width)
    shot = shot.resize((sw, sh), Image.LANCZOS)
    b = BEZEL * S
    fw, fh = sw + 2 * b, sh + 2 * b
    r = int(fw * 0.13)
    frame = Image.new("RGBA", (fw, fh), (0, 0, 0, 0))
    d = ImageDraw.Draw(frame)
    d.rounded_rectangle((0, 0, fw - 1, fh - 1), r, fill=(18, 20, 26, 255), outline=(78, 82, 94, 255), width=3 * S)
    mask = Image.new("L", (sw, sh), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, sw - 1, sh - 1), r - b, fill=255)
    frame.paste(shot, (b, b), mask)
    iw, ih = int(sw * 0.27), int(sw * 0.075)
    top = b + int(sw * 0.03)
    d.rounded_rectangle(((fw - iw) // 2, top, (fw + iw) // 2, top + ih), ih // 2, fill=(10, 11, 14, 255))
    return frame


def place(canvas, frame, x, y):
    shadow = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    shadow.paste(Image.new("RGBA", frame.size, (20, 34, 30, 120)), (x + 12 * S, y + 36 * S), frame.split()[3])
    canvas.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(36 * S)))
    canvas.paste(frame, (x, y), frame)


def wrap(draw, text, fnt, max_w):
    lines, cur = [], ""
    for word in text.split():
        trial = f"{cur} {word}".strip()
        if draw.textlength(trial, font=fnt) <= max_w:
            cur = trial
        else:
            lines.append(cur)
            cur = word
    return lines + [cur]


def logo(canvas, x, y):
    d = ImageDraw.Draw(canvas)
    d.rounded_rectangle((x, y, x + 46 * S, y + 46 * S), 12 * S, fill=(150, 230, 60))
    d.rounded_rectangle((x + 12 * S, y + 14 * S, x + 34 * S, y + 32 * S), 5 * S, fill=INK)
    d.text((x + 60 * S, y + 2 * S), "PocketPay", font=font("segoeuib.ttf", 32), fill=INK)


def finish(canvas, out, size):
    canvas.convert("RGB").resize(size, Image.LANCZOS).save(OUT / out, optimize=True)
    print("wrote", out)


def cover(names, out, palette, tagline):
    cw, ch = 2000, 1500
    canvas = gradient(cw * S, ch * S, *palette).convert("RGBA")
    place(canvas, phone(names[0], 600), 640 * S, 250 * S)
    place(canvas, phone(names[1], 600), 1170 * S, 420 * S)
    logo(canvas, 110 * S, 100 * S)
    d = ImageDraw.Draw(canvas)
    f = font("segoeuib.ttf", 72)
    y = 500 * S
    for line in wrap(d, tagline, f, 480 * S):
        d.text((110 * S, y), line, font=f, fill=INK)
        y += 88 * S
    finish(canvas, out, (cw, ch))


def feature(name, out, palette, eyebrow, title, body, bullets, phone_left=True):
    cw, ch = 2000, 1250
    canvas = gradient(cw * S, ch * S, *palette).convert("RGBA")
    pf = phone(name, 470)
    px = 300 * S if phone_left else (cw - 300) * S - pf.width
    place(canvas, pf, px, (ch * S - pf.height) // 2)
    d = ImageDraw.Draw(canvas)
    tx = 860 * S if phone_left else 190 * S
    maxw = 940 * S
    y = 300 * S
    d.text((tx, y), eyebrow.upper(), font=font("segoeuib.ttf", 28), fill=LIME)
    y += 70 * S
    ft = font("segoeuib.ttf", 84)
    for line in wrap(d, title, ft, maxw):
        d.text((tx, y), line, font=ft, fill=INK)
        y += 102 * S
    y += 40 * S
    fb = font("segoeui.ttf", 34)
    for line in wrap(d, body, fb, maxw):
        d.text((tx, y), line, font=fb, fill=MUTED)
        y += 50 * S
    y += 40 * S
    fl = font("seguisb.ttf", 32)
    for text in bullets:
        d.ellipse((tx, y + 10 * S, tx + 22 * S, y + 32 * S), fill=(150, 230, 60), outline=INK, width=2 * S)
        d.text((tx + 46 * S, y), text, font=fl, fill=INK)
        y += 62 * S
    logo(canvas, (cw - 330) * S if phone_left else 190 * S, 90 * S)
    finish(canvas, out, (cw, ch))


COPY = {
    "en": {
        "tag": "A digital wallet with round-up savings.",
        "roundup": ("Round-up savings", "Every payment saves a little", "Pay with QRIS and PocketPay rounds the amount up to the next Rp 5,000. The difference lands in the Spare Change Jar, and you can pay from the main account or any pocket.",
                    ["Round-up on every QRIS payment", "Choose the source of funds", "Atomic: all steps succeed or none do"]),
        "pockets": ("Pockets", "Savings that grow", "Split money into pockets for daily spending, goals or locked deposits. Interest is paid every day by a background job, even when the app is closed.",
                    ["3.5% p.a. flexible, 6% p.a. locked", "Savings goals with progress", "Early withdrawal blocked until maturity"]),
        "security": ("Security", "Locked by default", "The app starts locked and locks again after 60 seconds in the background. The PIN is hashed with PBKDF2 and the database is encrypted with SQLCipher.",
                     ["6-digit PIN, weak PINs rejected", "30 s lockout after 5 wrong tries", "Crypto-bound biometrics"]),
        "stats": ("Statistics", "See where money goes", "Weekly and monthly charts of income and expenses, with the latest expenses listed underneath. Refunds and internal moves are excluded from the totals.",
                  ["Weekly / monthly toggle", "Income vs. expenses", "Recent expenses at a glance"]),
        "receipt": ("Receipts and refunds", "A receipt for everything", "Every transaction has a digital receipt that can be shared. A payment can be refunded once within 24 hours, and the money goes back to where it came from.",
                    ["Shareable receipt and PDF statement", "One refund per payment, 24 h window", "Idempotency key against duplicates"]),
    },
    "id": {
        "tag": "Dompet digital dengan tabungan pembulatan.",
        "roundup": ("Tabungan pembulatan", "Setiap bayar, menabung", "Bayar dengan QRIS dan PocketPay membulatkan nominal ke Rp 5.000 berikutnya. Selisihnya masuk ke Celengan Receh, dan sumber dana bisa rekening utama atau kantong mana pun.",
                    ["Pembulatan di setiap pembayaran QRIS", "Pilih sumber dana", "Atomik: semua langkah berhasil atau tidak sama sekali"]),
        "pockets": ("Kantong", "Tabungan yang bertumbuh", "Pisahkan uang ke kantong untuk jajan, target, atau deposito terkunci. Bunga dibayar tiap hari oleh job latar belakang, bahkan saat aplikasi tertutup.",
                    ["3,5% p.a. fleksibel, 6% p.a. terkunci", "Target tabungan dengan progres", "Penarikan dini diblokir sampai jatuh tempo"]),
        "security": ("Keamanan", "Terkunci sejak awal", "Aplikasi selalu mulai terkunci dan mengunci lagi setelah 60 detik di latar. PIN di-hash dengan PBKDF2 dan database dienkripsi dengan SQLCipher.",
                     ["PIN 6 digit, PIN lemah ditolak", "Kuncian 30 detik setelah 5 kali salah", "Biometrik terikat kripto"]),
        "stats": ("Statistik", "Lihat uang ke mana", "Grafik mingguan dan bulanan untuk pemasukan dan pengeluaran, dengan pengeluaran terbaru di bawahnya. Refund dan pemindahan internal tidak dihitung.",
                  ["Pilihan mingguan / bulanan", "Pemasukan vs. pengeluaran", "Pengeluaran terbaru sekilas"]),
        "receipt": ("Struk dan refund", "Struk untuk semua transaksi", "Setiap transaksi punya struk digital yang bisa dibagikan. Pembayaran bisa di-refund satu kali dalam 24 jam, dan uang kembali ke sumber asalnya.",
                    ["Struk dan rekening koran PDF", "Satu refund per pembayaran, jendela 24 jam", "Kunci idempotensi anti duplikat"]),
    },
}

for lang in ("en", "id"):
    sfx = "" if lang == "en" else "-id"
    c = COPY[lang]
    cover([f"home-{lang}-dark", f"pockets-{lang}-dark"], f"01-cover{sfx}.png", SAGE, c["tag"])
    feature(f"pay-{lang}-dark", f"02-roundup{sfx}.png", LAV, *c["roundup"], phone_left=True)
    feature(f"pockets-{lang}-dark", f"03-pockets{sfx}.png", SAGE, *c["pockets"], phone_left=False)
    feature(f"pin-{lang}-dark", f"04-security{sfx}.png", MINT, *c["security"], phone_left=True)
    feature(f"stats-{lang}-light", f"05-statistics{sfx}.png", SAND, *c["stats"], phone_left=False)
    feature(f"receipt-{lang}-dark", f"06-receipts{sfx}.png", LAV, *c["receipt"], phone_left=True)
