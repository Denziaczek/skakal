#!/usr/bin/env python3
"""Generuje tekstury, modele, blockstate'y, receptury, loot table'e i dźwięki moda Ski Jump.
Uruchom: python3 tools/gen_assets.py   (wymaga Pillow)."""
import json, os, random
from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
A = os.path.join(ROOT, "assets", "skijump")
D = os.path.join(ROOT, "data", "skijump")
MCARMOR = os.path.join(ROOT, "assets", "minecraft", "textures", "models", "armor")
rnd = random.Random(7)


def w(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        if isinstance(data, str):
            f.write(data)
        else:
            json.dump(data, f, indent=2, ensure_ascii=False)


def png(path, img):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)


def noise(base, amp=12, size=16):
    img = Image.new("RGBA", (size, size), base + (255,))
    px = img.load()
    for x in range(size):
        for y in range(size):
            d = rnd.randint(-amp, amp)
            px[x, y] = tuple(max(0, min(255, c + d)) for c in base) + (255,)
    return img


# ------------------------------------------------------------------ tekstury bloków
def tex_block():
    b = os.path.join(A, "textures", "block")
    # tor najazdowy: lodowo-śnieżny z dwoma rowkami
    t = noise((190, 220, 240), 10)
    d = ImageDraw.Draw(t)
    for x in (4, 11):
        d.line([(x, 0), (x, 15)], fill=(120, 160, 195, 255))
        d.line([(x + 1, 0), (x + 1, 15)], fill=(235, 245, 255, 255))
    png(os.path.join(b, "inrun_track.png"), t)
    # próg: biały z pomarańczowym pasem
    t = noise((236, 238, 242), 6)
    d = ImageDraw.Draw(t)
    d.rectangle([0, 6, 15, 9], fill=(235, 120, 30, 255))
    d.line([(0, 5), (15, 5)], fill=(150, 150, 160, 255))
    d.line([(0, 10), (15, 10)], fill=(150, 150, 160, 255))
    png(os.path.join(b, "takeoff_table.png"), t)
    # znaczniki K (czerwony) i HS (niebieski): śnieg z pasem
    for name, col in (("k_marker", (205, 40, 40)), ("hs_marker", (45, 90, 215))):
        t = noise((240, 244, 250), 5)
        d = ImageDraw.Draw(t)
        d.rectangle([0, 5, 15, 10], fill=col + (255,))
        for x in range(0, 16, 4):
            d.rectangle([x, 5, x + 1, 10], fill=(250, 250, 250, 255))
        png(os.path.join(b, name + ".png"), t)
    # belka startowa
    t = noise((150, 105, 60), 10)
    d = ImageDraw.Draw(t)
    d.rectangle([0, 0, 15, 3], fill=(200, 40, 40, 255))
    d.rectangle([0, 12, 15, 15], fill=(200, 40, 40, 255))
    for x in range(0, 16, 4):
        d.rectangle([x, 4, x + 1, 11], fill=(235, 235, 235, 255))
    png(os.path.join(b, "start_gate.png"), t)
    # wiatromierz: 4 stopnie (przezroczysty, model "cross")
    for n in range(4):
        t = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        d = ImageDraw.Draw(t)
        d.rectangle([7, 3, 8, 15], fill=(160, 160, 170, 255))
        d.rectangle([6, 14, 9, 15], fill=(110, 110, 120, 255))
        # wiatrak z miseczkami, zwiększa się z siłą wiatru
        r = 2 + n
        d.line([(8 - r, 3), (8 + r, 3)], fill=(220, 220, 230, 255))
        d.rectangle([8 - r - 1, 2, 8 - r, 4], fill=(230, 60, 60, 255))
        d.rectangle([8 + r, 2, 8 + r + 1, 4], fill=(230, 60, 60, 255))
        # biało-czerwona wiatrówka
        L = 2 + 3 * n
        for i in range(L):
            col = (240, 240, 240, 255) if (i // 2) % 2 == 0 else (225, 50, 50, 255)
            d.rectangle([9 + i, 7 + (i // 3), 9 + i, 8 + (i // 3)], fill=col)
        png(os.path.join(b, "anemometer_%d.png" % n), t)
    # tablica wyników
    t = noise((28, 32, 40), 4)
    d = ImageDraw.Draw(t)
    d.rectangle([1, 1, 14, 14], outline=(80, 90, 110, 255))
    for y in (3, 6, 9, 12):
        d.line([(3, y), (6, y)], fill=(255, 210, 60, 255))
        d.line([(8, y), (13, y)], fill=(90, 230, 120, 255))
    png(os.path.join(b, "scoreboard.png"), t)
    # wieża sędziowska
    t = noise((140, 100, 60), 10)
    d = ImageDraw.Draw(t)
    d.rectangle([2, 3, 13, 9], fill=(120, 170, 210, 255), outline=(60, 40, 20, 255))
    d.line([(8, 3), (8, 9)], fill=(60, 40, 20, 255))
    png(os.path.join(b, "judge_tower.png"), t)
    t = noise((120, 85, 50), 8)
    png(os.path.join(b, "judge_tower_top.png"), t)
    # stacja zawodów
    t = noise((50, 90, 150), 6)
    d = ImageDraw.Draw(t)
    d.rectangle([2, 2, 13, 13], outline=(240, 240, 240, 255))
    d.rectangle([5, 5, 10, 10], fill=(240, 200, 60, 255))
    png(os.path.join(b, "competition_station.png"), t)
    # puchary
    for name, col in (("gold", (240, 200, 50)), ("silver", (205, 210, 220)), ("bronze", (190, 115, 60))):
        t = noise(col, 14)
        png(os.path.join(b, "trophy_%s.png" % name), t)


# ------------------------------------------------------------------ tekstury przedmiotów
def item_img(draw_fn):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw_fn(ImageDraw.Draw(img))
    return img


def tex_items():
    i = os.path.join(A, "textures", "item")

    def ski(col, tip):
        def f(d):
            d.polygon([(1, 14), (2, 15), (14, 3), (13, 2)], fill=col + (255,))
            d.polygon([(12, 1), (15, 4), (14, 3), (13, 2)], fill=tip + (255,))
            d.line([(4, 12), (7, 9)], fill=(40, 40, 40, 255))
            d.line([(5, 8), (8, 11)], fill=(30, 30, 30, 255))
        return f
    png(os.path.join(i, "ski_wood.png"), item_img(ski((190, 140, 80), (120, 80, 40))))
    png(os.path.join(i, "ski_iron.png"), item_img(ski((190, 195, 205), (110, 115, 125))))
    png(os.path.join(i, "ski_composite.png"), item_img(ski((40, 120, 220), (230, 230, 240))))

    def suit(col, accent):
        def f(d):
            d.rectangle([4, 3, 11, 14], fill=col + (255,))
            d.rectangle([1, 4, 4, 9], fill=col + (255,))
            d.rectangle([11, 4, 14, 9], fill=col + (255,))
            d.rectangle([6, 2, 9, 3], fill=(230, 200, 170, 255))
            d.line([(8, 4), (8, 14)], fill=accent + (255,))
            d.line([(4, 12), (11, 12)], fill=accent + (255,))
        return f
    png(os.path.join(i, "suit_standard.png"), item_img(suit((235, 235, 245), (210, 40, 40))))
    png(os.path.join(i, "suit_pro.png"), item_img(suit((30, 40, 90), (255, 200, 40))))

    def helmet(d):
        d.pieslice([2, 2, 13, 14], 180, 360, fill=(235, 235, 240, 255))
        d.rectangle([2, 8, 13, 10], fill=(235, 235, 240, 255))
        d.rectangle([4, 7, 11, 9], fill=(40, 70, 120, 255))
        d.line([(2, 4), (13, 4)], fill=(210, 40, 40, 255))
    png(os.path.join(i, "helmet.png"), item_img(helmet))

    def wand(d):
        d.line([(3, 13), (12, 4)], fill=(120, 80, 40, 255), width=2)
        d.rectangle([11, 1, 14, 4], fill=(240, 200, 60, 255))
        d.point([(12, 2)], fill=(255, 255, 255, 255))
    png(os.path.join(i, "hill_wand.png"), item_img(wand))

    def blueprint(d):
        d.rectangle([1, 2, 14, 13], fill=(40, 90, 170, 255), outline=(220, 235, 255, 255))
        d.line([(3, 11), (6, 5)], fill=(255, 255, 255, 255))
        d.line([(6, 5), (9, 8)], fill=(255, 255, 255, 255))
        d.line([(9, 8), (13, 11)], fill=(255, 255, 255, 255))
        d.line([(3, 11), (13, 11)], fill=(150, 190, 240, 255))
    png(os.path.join(i, "hill_blueprint.png"), item_img(blueprint))

    for name, col in (("gold", (240, 200, 50)), ("silver", (205, 210, 220)), ("bronze", (190, 115, 60))):
        def medal(d, col=col):
            d.polygon([(5, 1), (8, 6), (11, 1)], fill=(210, 40, 40, 255))
            d.ellipse([3, 6, 12, 15], fill=col + (255,), outline=(90, 70, 20, 255))
            d.ellipse([6, 9, 9, 12], outline=(120, 90, 20, 255))
        png(os.path.join(i, "medal_%s.png" % name), item_img(medal))


# ------------------------------------------------------------------ tekstury pancerza (64x32)
def armor_layer(name, head=None, body=None, legs=None):
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    if head:
        base, trim = head
        d.rectangle([0, 0, 31, 15], fill=base + (255,))
        d.rectangle([0, 8, 31, 10], fill=trim + (255,))
        d.rectangle([8, 0, 15, 7], fill=base + (255,))
        d.rectangle([8, 11, 15, 13], fill=(40, 70, 120, 255))      # szybka
    if body:
        base, trim = body
        d.rectangle([16, 16, 39, 31], fill=base + (255,))
        d.rectangle([40, 16, 55, 31], fill=base + (255,))
        d.rectangle([20, 20, 27, 22], fill=trim + (255,))
        d.rectangle([16, 28, 39, 29], fill=trim + (255,))
        d.rectangle([40, 28, 55, 29], fill=trim + (255,))
    if legs:
        base, trim = legs
        d.rectangle([0, 27, 15, 31], fill=base + (255,))
        d.rectangle([0, 29, 15, 29], fill=trim + (255,))
    png(os.path.join(MCARMOR, name + "_layer_1.png"), img)


def tex_armor():
    armor_layer("skijump_ski_wood", legs=((190, 140, 80), (120, 80, 40)))
    armor_layer("skijump_ski_iron", legs=((190, 195, 205), (110, 115, 125)))
    armor_layer("skijump_ski_composite", legs=((40, 120, 220), (235, 235, 245)))
    armor_layer("skijump_suit_standard", body=((235, 235, 245), (210, 40, 40)))
    armor_layer("skijump_suit_pro", body=((30, 40, 90), (255, 200, 40)))
    armor_layer("skijump_helmet", head=((235, 235, 240), (210, 40, 40)))


# ------------------------------------------------------------------ skin Skoczka i drobiazgi
def skin():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    skin_c = (225, 180, 150, 255)
    suit_c = (30, 60, 150, 255)
    trim = (230, 230, 240, 255)
    hair = (70, 45, 25, 255)
    helm = (235, 235, 240, 255)

    def fillr(x0, y0, x1, y1, c):
        d.rectangle([x0, y0, x1 - 1, y1 - 1], fill=c)
    fillr(0, 0, 32, 16, skin_c)
    fillr(8, 0, 16, 8, hair)
    fillr(0, 8, 8, 12, hair); fillr(16, 8, 24, 12, hair); fillr(24, 8, 32, 16, hair); fillr(8, 8, 16, 11, hair)
    fillr(9, 12, 11, 13, (30, 30, 30, 255)); fillr(13, 12, 15, 13, (30, 30, 30, 255))
    # kask (nakładka)
    fillr(32, 0, 64, 16, (0, 0, 0, 0))
    fillr(40, 0, 48, 8, helm); fillr(32, 8, 40, 11, helm); fillr(48, 8, 56, 11, helm); fillr(40, 8, 48, 11, helm); fillr(56, 8, 64, 16, helm)
    fillr(40, 11, 48, 13, (40, 70, 120, 255))
    # tułów i ręce
    fillr(16, 16, 40, 32, suit_c); fillr(40, 16, 56, 32, suit_c); fillr(20, 20, 28, 22, trim)
    fillr(32, 48, 48, 64, suit_c)
    # nogi
    fillr(0, 16, 16, 32, suit_c); fillr(16, 48, 32, 64, suit_c)
    fillr(0, 28, 16, 32, (210, 40, 40, 255)); fillr(16, 60, 32, 64, (210, 40, 40, 255))
    # dłonie
    fillr(44, 28, 48, 32, skin_c); fillr(52, 28, 56, 32, skin_c); fillr(36, 60, 40, 64, skin_c); fillr(44, 60, 48, 64, skin_c)
    png(os.path.join(A, "textures", "entity", "skoczek.png"), img)
    png(os.path.join(A, "textures", "entity", "seat.png"), Image.new("RGBA", (16, 16), (0, 0, 0, 0)))


def icon():
    img = Image.new("RGBA", (128, 128), (200, 225, 245, 255))
    d = ImageDraw.Draw(img)
    for y in range(128):
        c = int(200 - y * 0.35)
        d.line([(0, y), (127, y)], fill=(c, c + 20, 250 - y // 4, 255))
    # skocznia: rozbieg i zeskok
    d.polygon([(0, 128), (0, 40), (46, 78), (52, 82), (128, 118), (128, 128)], fill=(245, 248, 252, 255))
    d.line([(0, 40), (46, 78), (54, 82)], fill=(120, 160, 200, 255), width=3)
    d.line([(54, 82), (128, 118)], fill=(150, 180, 210, 255), width=2)
    # skoczek w locie
    d.polygon([(66, 52), (96, 62), (92, 66), (62, 58)], fill=(30, 60, 150, 255))
    d.ellipse([92, 56, 100, 64], fill=(235, 235, 240, 255))
    d.line([(60, 62), (100, 74)], fill=(210, 40, 40, 255), width=2)
    d.line([(62, 60), (102, 70)], fill=(210, 40, 40, 255), width=2)
    png(os.path.join(A, "icon.png"), img)


# ------------------------------------------------------------------ modele i blockstate'y
TEX = "skijump:block/"


def layered(name, tex):
    for n in range(1, 9):
        h = n * 2
        w(os.path.join(A, "models", "block", "%s_%d.json" % (name, n)), {
            "parent": "minecraft:block/block",
            "textures": {"all": TEX + tex, "particle": TEX + tex},
            "elements": [{
                "from": [0, 0, 0], "to": [16, h, 16],
                "faces": {
                    "down": {"texture": "#all", "cullface": "down"},
                    "up": {"texture": "#all", "uv": [0, 0, 16, 16]},
                    "north": {"texture": "#all", "uv": [0, 16 - h, 16, 16]},
                    "south": {"texture": "#all", "uv": [0, 16 - h, 16, 16]},
                    "west": {"texture": "#all", "uv": [0, 16 - h, 16, 16]},
                    "east": {"texture": "#all", "uv": [0, 16 - h, 16, 16]},
                }}]})
    w(os.path.join(A, "blockstates", name + ".json"),
      {"variants": {"layers=%d" % n: {"model": "skijump:block/%s_%d" % (name, n)} for n in range(1, 9)}})
    w(os.path.join(A, "models", "item", name + ".json"), {"parent": "skijump:block/%s_8" % name})


def cube_all(name, tex=None):
    w(os.path.join(A, "models", "block", name + ".json"),
      {"parent": "minecraft:block/cube_all", "textures": {"all": TEX + (tex or name)}})
    w(os.path.join(A, "blockstates", name + ".json"), {"variants": {"": {"model": "skijump:block/" + name}}})
    w(os.path.join(A, "models", "item", name + ".json"), {"parent": "skijump:block/" + name})


def models():
    for nm in ("inrun_track", "takeoff_table", "k_marker", "hs_marker"):
        layered(nm, nm)
    cube_all("scoreboard")
    cube_all("competition_station")
    # wieża sędziowska: słup z innym wierzchem
    w(os.path.join(A, "models", "block", "judge_tower.json"),
      {"parent": "minecraft:block/cube_column", "textures": {"end": TEX + "judge_tower_top", "side": TEX + "judge_tower"}})
    w(os.path.join(A, "blockstates", "judge_tower.json"), {"variants": {"": {"model": "skijump:block/judge_tower"}}})
    w(os.path.join(A, "models", "item", "judge_tower.json"), {"parent": "skijump:block/judge_tower"})
    # belka startowa (kierunek wzdłuż osi X dla north/south)
    w(os.path.join(A, "models", "block", "start_gate.json"), {
        "textures": {"t": TEX + "start_gate", "particle": TEX + "start_gate"},
        "elements": [
            {"from": [0, 3, 5], "to": [16, 6, 11], "faces": {f: {"texture": "#t"} for f in ("north", "south", "east", "west", "up", "down")}},
            {"from": [1, 0, 6], "to": [3, 3, 10], "faces": {f: {"texture": "#t"} for f in ("north", "south", "east", "west", "up", "down")}},
            {"from": [13, 0, 6], "to": [15, 3, 10], "faces": {f: {"texture": "#t"} for f in ("north", "south", "east", "west", "up", "down")}},
        ]})
    rot = {"north": 0, "south": 180, "east": 90, "west": 270}
    w(os.path.join(A, "blockstates", "start_gate.json"), {"variants": {
        "facing=%s" % k: ({"model": "skijump:block/start_gate", "y": v} if v else {"model": "skijump:block/start_gate"})
        for k, v in rot.items()}})
    w(os.path.join(A, "models", "item", "start_gate.json"), {"parent": "skijump:block/start_gate"})
    # wiatromierz
    for n in range(4):
        w(os.path.join(A, "models", "block", "anemometer_%d.json" % n),
          {"parent": "minecraft:block/cross", "textures": {"cross": TEX + "anemometer_%d" % n}})
    w(os.path.join(A, "blockstates", "anemometer.json"),
      {"variants": {"strength=%d" % n: {"model": "skijump:block/anemometer_%d" % n} for n in range(4)}})
    w(os.path.join(A, "models", "item", "anemometer.json"),
      {"parent": "minecraft:item/generated", "textures": {"layer0": TEX + "anemometer_2"}})
    # puchary
    def face6():
        return {f: {"texture": "#t"} for f in ("north", "south", "east", "west", "up", "down")}
    for nm in ("gold", "silver", "bronze"):
        w(os.path.join(A, "models", "block", "trophy_%s.json" % nm), {
            "textures": {"t": TEX + "trophy_" + nm, "particle": TEX + "trophy_" + nm},
            "elements": [
                {"from": [4, 0, 4], "to": [12, 1.5, 12], "faces": face6()},
                {"from": [7, 1.5, 7], "to": [9, 6, 9], "faces": face6()},
                {"from": [4, 6, 4], "to": [12, 13, 12], "faces": face6()},
                {"from": [2, 8, 7], "to": [4, 11, 9], "faces": face6()},
                {"from": [12, 8, 7], "to": [14, 11, 9], "faces": face6()},
            ]})
        w(os.path.join(A, "blockstates", "trophy_%s.json" % nm), {"variants": {"": {"model": "skijump:block/trophy_" + nm}}})
        w(os.path.join(A, "models", "item", "trophy_%s.json" % nm), {"parent": "skijump:block/trophy_" + nm})
    # przedmioty
    for nm in ("ski_wood", "ski_iron", "ski_composite", "suit_standard", "suit_pro", "helmet", "medal_gold", "medal_silver", "medal_bronze", "hill_blueprint"):
        w(os.path.join(A, "models", "item", nm + ".json"),
          {"parent": "minecraft:item/generated", "textures": {"layer0": "skijump:item/" + nm}})
    w(os.path.join(A, "models", "item", "hill_wand.json"),
      {"parent": "minecraft:item/handheld", "textures": {"layer0": "skijump:item/hill_wand"}})
    w(os.path.join(A, "models", "item", "skoczek_spawn_egg.json"), {"parent": "minecraft:item/template_spawn_egg"})


# ------------------------------------------------------------------ dźwięki
def sounds():
    def ev(name, vol=1.0, pitch=1.0):
        return {"name": name, "type": "event", "volume": vol, "pitch": pitch}
    w(os.path.join(A, "sounds.json"), {
        "wind": {"subtitle": "skijump.subtitle.wind", "sounds": [ev("minecraft:item.elytra.flying", 0.8, 1.0)]},
        "crowd": {"subtitle": "skijump.subtitle.crowd", "sounds": [ev("minecraft:entity.villager.celebrate", 1.0, 0.85), ev("minecraft:entity.villager.yes", 1.0, 0.7)]},
        "land": {"subtitle": "skijump.subtitle.land", "sounds": [ev("minecraft:block.snow.place", 1.0, 0.8), ev("minecraft:block.snow.step", 1.0, 0.7)]},
        "fall": {"subtitle": "skijump.subtitle.fall", "sounds": [ev("minecraft:entity.player.big_fall", 1.0, 0.9), ev("minecraft:block.snow.break", 1.0, 0.8)]},
        "start": {"subtitle": "skijump.subtitle.start", "sounds": [ev("minecraft:block.note_block.bell", 1.0, 1.2)]},
    })


# ------------------------------------------------------------------ dane: receptury, loot, tagi
BLOCKS = ["start_gate", "inrun_track", "takeoff_table", "k_marker", "hs_marker", "anemometer", "scoreboard",
          "judge_tower", "competition_station", "trophy_gold", "trophy_silver", "trophy_bronze"]


def ing(s):
    return {"tag": s[4:]} if s.startswith("tag:") else {"item": s}


def shaped(name, pattern, key, count=1):
    w(os.path.join(D, "recipes", name + ".json"), {
        "type": "minecraft:crafting_shaped",
        "category": "equipment",
        "pattern": pattern,
        "key": {k: ing(v) for k, v in key.items()},
        "result": {"item": "skijump:" + name, "count": count}})


def shapeless(name, items, count=1):
    w(os.path.join(D, "recipes", name + ".json"), {
        "type": "minecraft:crafting_shapeless",
        "category": "misc",
        "ingredients": [ing(i) for i in items],
        "result": {"item": "skijump:" + name, "count": count}})


def data():
    shaped("ski_wood", ["P P", "P P"], {"P": "tag:minecraft:planks"})
    shaped("ski_iron", ["I I", "I I"], {"I": "minecraft:iron_ingot"})
    shaped("ski_composite", ["QIQ", "Q Q"], {"Q": "minecraft:quartz", "I": "minecraft:iron_ingot"})
    shaped("suit_standard", ["W W", "WWW", "WWW"], {"W": "minecraft:white_wool"})
    shaped("suit_pro", ["PWP", "WLW", "PWP"], {"P": "minecraft:phantom_membrane", "W": "minecraft:white_wool", "L": "minecraft:leather"})
    shaped("helmet", ["LLL", "LIL"], {"L": "minecraft:leather", "I": "minecraft:iron_ingot"})
    shaped("hill_wand", ["  Q", " S ", "S  "], {"Q": "minecraft:quartz", "S": "minecraft:stick"})
    shaped("start_gate", ["PPP", "S S"], {"P": "tag:minecraft:planks", "S": "minecraft:stick"})
    shaped("inrun_track", ["PPP", "SSS"], {"P": "minecraft:packed_ice", "S": "minecraft:snow_block"}, 6)
    shaped("takeoff_table", ["QQQ", "PPP"], {"Q": "minecraft:quartz", "P": "minecraft:packed_ice"}, 6)
    shapeless("hill_blueprint", ["minecraft:paper", "minecraft:blue_dye", "minecraft:snow_block"])
    shapeless("k_marker", ["skijump:inrun_track", "minecraft:red_dye"])
    shapeless("hs_marker", ["skijump:inrun_track", "minecraft:blue_dye"])
    shaped("anemometer", ["IWI", " S ", " I "], {"I": "minecraft:iron_ingot", "W": "minecraft:white_wool", "S": "minecraft:stick"})
    shaped("scoreboard", ["III", "IRI", "III"], {"I": "minecraft:iron_ingot", "R": "minecraft:redstone"})
    shaped("judge_tower", ["PPP", "PGP", "PPP"], {"P": "tag:minecraft:planks", "G": "minecraft:glass_pane"})
    shaped("competition_station", ["III", "ICI", "IPI"], {"I": "minecraft:iron_ingot", "C": "minecraft:clock", "P": "minecraft:paper"})
    shaped("trophy_gold", ["G G", " G ", "GGG"], {"G": "minecraft:gold_ingot"})
    shaped("trophy_silver", ["G G", " G ", "GGG"], {"G": "minecraft:iron_ingot"})
    shaped("trophy_bronze", ["G G", " G ", "GGG"], {"G": "minecraft:copper_ingot"})
    for b in BLOCKS:
        w(os.path.join(D, "loot_tables", "blocks", b + ".json"), {
            "type": "minecraft:block",
            "pools": [{"rolls": 1.0, "bonus_rolls": 0.0,
                       "entries": [{"type": "minecraft:item", "name": "skijump:" + b}],
                       "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    w(os.path.join(ROOT, "data", "minecraft", "tags", "blocks", "mineable", "pickaxe.json"),
      {"replace": False, "values": ["skijump:" + b for b in BLOCKS if b not in ("start_gate", "judge_tower")]})
    w(os.path.join(ROOT, "data", "minecraft", "tags", "blocks", "mineable", "axe.json"),
      {"replace": False, "values": ["skijump:start_gate", "skijump:judge_tower"]})


# ------------------------------------------------------------------ ulepszone tekstury sprzętu (v1.1)
def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c[:3]) + (255,)


def rect(d, x0, y0, x1, y1, c):
    d.rectangle([x0, y0, x1 - 1, y1 - 1], fill=c)


def armor_helmet(shell, accent, visor):
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    rect(d, 8, 0, 16, 8, shell)                      # góra
    rect(d, 11, 0, 13, 8, accent)                    # pas na czubku
    for (x0, x1) in ((0, 8), (8, 16), (16, 24), (24, 32)):
        rect(d, x0, 8, x1, 14, shell)                # ściany głowy (dolne 2 rzędy zostają odkryte)
        rect(d, x0, 8, x1, 9, shade(shell, 1.12))    # blask
        rect(d, x0, 9, x1, 10, accent)               # opaska
    rect(d, 8, 10, 16, 13, visor)                    # szybka z przodu
    rect(d, 9, 10, 15, 11, shade(visor, 1.5))        # odbicie
    for (x0, x1) in ((0, 8), (16, 24)):
        rect(d, x0 + 2, 10, x0 + 6, 13, shade(shell, 0.7))   # nauszniki
    rect(d, 24, 12, 32, 14, shade(shell, 0.75))      # tył
    png(os.path.join(MCARMOR, "skijump_helmet_layer_1.png"), img)


def armor_suit(name, base, accent, trim):
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # tułów: góra, dół, bok, przód, bok, tył
    rect(d, 20, 16, 28, 20, shade(base, 1.2)); rect(d, 28, 16, 36, 20, shade(base, 0.7))
    for (x0, x1) in ((16, 20), (20, 28), (28, 32), (32, 40)):
        rect(d, x0, 20, x1, 32, base)
        rect(d, x0, 20, x1, 22, shade(base, 1.25))       # kołnierz
        rect(d, x0, 28, x1, 30, accent)                  # pas
        rect(d, x0, 30, x1, 32, shade(base, 0.8))
    rect(d, 23, 22, 25, 28, accent)                      # pionowy pas z przodu
    rect(d, 24, 22, 25, 28, trim)
    rect(d, 21, 24, 22, 26, trim); rect(d, 26, 24, 27, 26, trim)
    rect(d, 17, 20, 19, 30, accent); rect(d, 29, 20, 31, 30, accent)   # pasy boczne
    rect(d, 34, 23, 38, 27, accent)                      # numer z tyłu
    # ręka: góra, dół, zewnątrz, przód, wewnątrz, tył
    rect(d, 44, 16, 48, 20, shade(base, 1.2)); rect(d, 48, 16, 52, 20, shade(base, 0.7))
    for (x0, x1) in ((40, 44), (44, 48), (48, 52), (52, 56)):
        rect(d, x0, 20, x1, 32, base)
        rect(d, x0, 20, x1, 22, shade(base, 1.2))
        rect(d, x0, 26, x1, 27, accent)
        rect(d, x0, 28, x1, 32, shade(trim, 0.35))       # rękawica
    rect(d, 41, 21, 43, 26, accent)
    png(os.path.join(MCARMOR, name + "_layer_1.png"), img)


def armor_skiboot(name, boot, rim):
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    rect(d, 4, 16, 8, 20, rim); rect(d, 8, 16, 12, 20, shade(boot, 0.6))
    for (x0, x1) in ((0, 4), (4, 8), (8, 12), (12, 16)):
        rect(d, x0, 24, x1, 32, boot)
        rect(d, x0, 24, x1, 26, rim)
        rect(d, x0, 28, x1, 29, shade(boot, 1.3))
        rect(d, x0, 30, x1, 32, shade(boot, 0.55))
    png(os.path.join(MCARMOR, name + "_layer_1.png"), img)


def ski_tex(name, base, accent):
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    rect(d, 24, 0, 28, 24, base)                         # góra narty
    rect(d, 26, 0, 27, 24, accent)
    for y in range(2, 24, 6):
        rect(d, 24, y, 28, y + 1, accent)
    rect(d, 24, 0, 28, 2, shade(base, 0.6))              # czubek
    rect(d, 28, 0, 32, 24, shade(base, 0.45))            # spód
    rect(d, 0, 24, 56, 25, shade(base, 0.7))             # boki
    png(os.path.join(A, "textures", "entity", name + ".png"), img)


def tex_armor_v2():
    armor_helmet((236, 238, 244), (205, 40, 40), (40, 80, 140))
    armor_suit("skijump_suit_standard", (236, 238, 246), (205, 40, 40), (255, 255, 255))
    armor_suit("skijump_suit_pro", (28, 40, 105), (250, 195, 40), (255, 255, 255))
    armor_skiboot("skijump_ski_wood", (70, 52, 38), (190, 140, 80))
    armor_skiboot("skijump_ski_iron", (62, 66, 74), (190, 195, 205))
    armor_skiboot("skijump_ski_composite", (24, 34, 70), (60, 140, 235))
    ski_tex("ski_wood", (200, 150, 85), (120, 80, 40))
    ski_tex("ski_iron", (185, 190, 200), (95, 100, 112))
    ski_tex("ski_composite", (40, 120, 220), (240, 240, 250))


# ------------------------------------------------------------------ v1.2: własne bryły sprzętu + struktura skijump_hill
def region(img, ox, oy, base, kind):
    """Pole 64x32 z subtelnym szumem i wzorem (kind: plain/stripes/glossy)."""
    d = ImageDraw.Draw(img)
    for y in range(32):
        for x in range(64):
            k = 1.0 + (rnd.random() - 0.5) * 0.08 + (0.10 - 0.20 * y / 31.0) * (1.0 if kind != "plain" else 0.5)
            img.putpixel((ox + x, oy + y), shade(base, k))
    if kind == "stripes":
        for x in range(0, 64, 8):
            d.rectangle([ox + x, oy, ox + x + 1, oy + 31], fill=shade(base, 1.35))
    if kind == "glossy":
        for i in range(0, 40, 1):
            d.line([(ox + i, oy + 31), (ox + i + 20, oy)], fill=shade(base, 1.0 + 0.012 * (20 - abs(i - 20))), width=1)


def gear_tex(name, base, accent, dark, light):
    img = Image.new("RGBA", (128, 64), (0, 0, 0, 255))
    region(img, 0, 0, base, "plain")
    region(img, 64, 0, accent, "stripes")
    region(img, 0, 32, dark, "glossy")
    region(img, 64, 32, light, "plain")
    png(os.path.join(A, "textures", "entity", name + ".png"), img)


def tex_gear_v3():
    gear_tex("gear_helmet", (236, 238, 244), (205, 40, 40), (28, 62, 120), (52, 56, 64))
    gear_tex("gear_suit_standard", (236, 238, 246), (205, 40, 40), (40, 44, 52), (250, 250, 250))
    gear_tex("gear_suit_pro", (28, 40, 105), (250, 195, 40), (24, 26, 34), (245, 245, 250))
    # pancerz vanilla jest niewidoczny (rysują go własne bryły)
    for nm in ("skijump_ski_wood", "skijump_ski_iron", "skijump_ski_composite", "skijump_suit_standard", "skijump_suit_pro", "skijump_helmet"):
        png(os.path.join(MCARMOR, nm + "_layer_1.png"), Image.new("RGBA", (64, 32), (0, 0, 0, 0)))


def nbt_template():
    import gzip, struct
    def s16(x): return struct.pack(">H", len(x.encode())) + x.encode()
    def tag(t, name, payload): return bytes([t]) + s16(name) + payload
    def lst(t, items): return bytes([t]) + struct.pack(">i", len(items)) + b"".join(items)
    ints3 = lambda a: lst(3, [struct.pack(">i", v) for v in a])
    palette = lst(10, [tag(8, "Name", s16("minecraft:air")) + b"\x00"])
    block = tag(9, "pos", ints3([0, 0, 0])) + tag(3, "state", struct.pack(">i", 0)) + b"\x00"
    root = (tag(9, "size", ints3([1, 1, 1])) + tag(9, "palette", palette) + tag(9, "blocks", lst(10, [block]))
            + tag(9, "entities", lst(0, [])) + tag(3, "DataVersion", struct.pack(">i", 3465)) + b"\x00")
    data = bytes([10]) + s16("") + root
    path = os.path.join(D, "structures", "hill_marker.nbt")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(gzip.compress(data))


def worldgen():
    w(os.path.join(ROOT, "data", "minecraft", "worldgen", "structure", "skijump_hill.json"), {
        "type": "minecraft:jigsaw", "biomes": "#skijump:has_skijump_hill", "step": "surface_structures",
        "spawn_overrides": {}, "terrain_adaptation": "none", "start_pool": "skijump:hill_marker", "size": 1,
        "start_height": {"absolute": 0}, "project_start_to_heightmap": "WORLD_SURFACE_WG",
        "max_distance_from_center": 80, "use_expansion_hack": False})
    w(os.path.join(D, "worldgen", "template_pool", "hill_marker.json"), {
        "name": "skijump:hill_marker", "fallback": "minecraft:empty",
        "elements": [{"weight": 1, "element": {"element_type": "minecraft:single_pool_element",
                                               "location": "skijump:hill_marker", "processors": "minecraft:empty",
                                               "projection": "rigid"}}]})
    w(os.path.join(D, "worldgen", "structure_set", "skijump_hills.json"), {
        "structures": [{"structure": "minecraft:skijump_hill", "weight": 1}],
        "placement": {"type": "minecraft:random_spread", "spacing": 26, "separation": 10, "salt": 1893451}})
    w(os.path.join(D, "tags", "worldgen", "biome", "has_skijump_hill.json"), {
        "replace": False,
        "values": ["#minecraft:is_mountain", "minecraft:snowy_plains", "minecraft:snowy_taiga", "minecraft:taiga",
                   "minecraft:old_growth_pine_taiga", "minecraft:old_growth_spruce_taiga"]})
    w(os.path.join(D, "tags", "worldgen", "structure", "skijump_hill.json"), {
        "replace": False, "values": ["minecraft:skijump_hill"]})
    nbt_template()


if __name__ == "__main__":
    tex_block()
    tex_items()
    tex_armor()
    tex_armor_v2()
    tex_gear_v3()
    worldgen()
    skin()
    icon()
    models()
    sounds()
    data()
    print("assets generated")
