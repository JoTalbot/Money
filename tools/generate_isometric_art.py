#!/usr/bin/env python3
"""Генерирует исходные 2D-ассеты DeadRig в единой изометрической палитре."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter
import random

OUT = Path(__file__).resolve().parents[1] / "Assets" / "Resources" / "Isometric"
S = 3  # supersampling
random.seed(7331)


def canvas(w, h):
    return Image.new("RGBA", (w*S, h*S), (0, 0, 0, 0)), ImageDraw.Draw(Image.new("RGBA", (1, 1)))


def poly(d, pts, fill, outline=None, width=1):
    pts = [(int(x*S), int(y*S)) for x, y in pts]
    d.polygon(pts, fill=fill)
    if outline:
        d.line(pts + [pts[0]], fill=outline, width=width*S, joint="curve")


def ellipse(d, box, fill, outline=None, width=1):
    d.ellipse(tuple(int(v*S) for v in box), fill=fill, outline=outline, width=width*S)


def rect(d, box, fill, outline=None, width=1, radius=0):
    box = tuple(int(v*S) for v in box)
    d.rounded_rectangle(box, radius=radius*S, fill=fill, outline=outline, width=width*S)


def save(img, name):
    img.resize((img.width//S, img.height//S), Image.Resampling.LANCZOS).save(OUT / name, optimize=True)


def ground():
    img = Image.new("RGBA", (256*S, 128*S), (0,0,0,0)); d = ImageDraw.Draw(img)
    poly(d, [(128,4),(252,64),(128,124),(4,64)], "#202b2b", "#344343", 2)
    poly(d, [(128,9),(245,64),(128,119),(11,64)], "#263333")
    # subtle worn panels and seams
    d.line([(128*S,9*S),(128*S,119*S)], fill="#1a2426", width=1*S)
    d.line([(11*S,64*S),(245*S,64*S)], fill="#314143", width=1*S)
    for _ in range(16):
        x=random.randint(42,214); y=random.randint(28,98)
        # retain only points inside diamond
        if abs(x-128)/124 + abs(y-64)/58 < .86:
            ellipse(d,(x-1,y-1,x+1,y+1),"#405052")
    # hazard corner strokes
    poly(d,[(22,59),(34,53),(39,56),(27,62)],"#d78a2b")
    poly(d,[(217,72),(229,66),(234,69),(222,75)],"#d78a2b")
    save(img,"ground_tile.png")


def base():
    img=Image.new("RGBA",(512*S,512*S),(0,0,0,0)); d=ImageDraw.Draw(img)
    # soft contact shadow
    ellipse(d,(70,410,442,480),(0,0,0,90))
    # armored platform
    poly(d,[(256,306),(432,390),(256,478),(80,390)],"#172124","#0b1012",5)
    poly(d,[(80,390),(256,478),(256,438),(80,351)],"#101719")
    poly(d,[(432,390),(256,478),(256,438),(432,351)],"#0c1315")
    poly(d,[(256,270),(422,350),(256,432),(90,350)],"#344448","#0e1517",5)
    # central bunker
    poly(d,[(256,112),(371,171),(256,230),(141,171)],"#596a6d","#101719",5)
    poly(d,[(141,171),(256,230),(256,380),(141,321)],"#2c3b3f","#101719",5)
    poly(d,[(371,171),(256,230),(256,380),(371,321)],"#1f2c30","#101719",5)
    # orange armor braces
    poly(d,[(141,218),(166,230),(166,319),(141,307)],"#df8428")
    poly(d,[(371,218),(346,230),(346,319),(371,307)],"#b85e1d")
    # glowing reactor door
    poly(d,[(205,259),(256,285),(256,366),(205,341)],"#10191c","#0a0f10",3)
    poly(d,[(307,259),(256,285),(256,366),(307,341)],"#0b1316","#0a0f10",3)
    poly(d,[(220,279),(256,297),(256,345),(220,327)],"#27cdd0")
    poly(d,[(292,279),(256,297),(256,345),(292,327)],"#168a99")
    # roof reactor rings
    ellipse(d,(198,50,314,157),"#152124","#0a1012",5)
    ellipse(d,(214,65,298,142),"#dd7d25","#ffb54a",4)
    ellipse(d,(229,80,283,130),"#162b31","#39e7e4",5)
    ellipse(d,(243,93,269,118),"#a8ffff")
    # lamps
    for x,y in [(126,350),(386,350),(178,397),(334,397)]:
        ellipse(d,(x-8,y-8,x+8,y+8),"#ffb036","#502d12",2)
    save(img,"base_core.png")


def turret():
    img=Image.new("RGBA",(384*S,384*S),(0,0,0,0)); d=ImageDraw.Draw(img)
    ellipse(d,(54,292,330,350),(0,0,0,85))
    poly(d,[(192,205),(300,258),(192,314),(84,258)],"#4b5c60","#11191b",5)
    poly(d,[(84,258),(192,314),(192,345),(84,289)],"#263438")
    poly(d,[(300,258),(192,314),(192,345),(300,289)],"#1d292d")
    # turret head
    poly(d,[(192,130),(270,169),(192,210),(114,169)],"#68777a","#101719",5)
    poly(d,[(114,169),(192,210),(192,269),(114,228)],"#344448")
    poly(d,[(270,169),(192,210),(192,269),(270,228)],"#26363a")
    # angled twin barrels
    poly(d,[(183,149),(205,138),(313,190),(289,203)],"#263337","#0c1214",4)
    poly(d,[(171,170),(191,160),(295,211),(273,224)],"#39494d","#0c1214",4)
    ellipse(d,(302,181,327,204),"#28e3e0","#c4ffff",3)
    ellipse(d,(286,202,310,225),"#28e3e0","#c4ffff",3)
    # orange armor cap
    poly(d,[(192,103),(245,130),(192,157),(139,130)],"#df8328","#22180e",4)
    ellipse(d,(174,93,210,127),"#74ffff","#e4ffff",3)
    save(img,"turret.png")


def zombie():
    img=Image.new("RGBA",(256*S,384*S),(0,0,0,0)); d=ImageDraw.Draw(img)
    ellipse(d,(50,326,206,365),(0,0,0,80))
    # legs
    poly(d,[(91,245),(126,254),(119,334),(76,343)],"#26343a","#101617",4)
    poly(d,[(133,252),(166,238),(185,326),(145,337)],"#202d32","#101617",4)
    # coat/body
    poly(d,[(77,127),(129,104),(182,137),(169,261),(125,283),(72,249)],"#4e6858","#111915",5)
    poly(d,[(129,104),(182,137),(169,261),(126,282),(127,155)],"#354c43")
    # torn orange work vest
    poly(d,[(82,150),(126,171),(124,225),(76,202)],"#b96022")
    poly(d,[(130,171),(173,149),(170,205),(132,226)],"#d27827")
    # arms
    poly(d,[(74,151),(45,188),(76,240),(98,224)],"#607c63","#111915",4)
    poly(d,[(177,151),(213,184),(182,231),(159,217)],"#516e59","#111915",4)
    # head
    poly(d,[(128,48),(171,69),(164,125),(128,145),(87,122),(84,74)],"#71936c","#111915",5)
    poly(d,[(128,48),(171,69),(164,125),(128,145)],"#587b5d")
    # face / implant
    ellipse(d,(101,83,116,98),"#d9ff73")
    ellipse(d,(140,83,156,99),"#ff573d")
    d.line([(146*S,91*S),(181*S,75*S)],fill="#ff604a",width=3*S)
    # exposed brain edge
    poly(d,[(93,67),(107,46),(128,48),(117,71)],"#bd5f79","#401e2a",3)
    save(img,"zombie.png")


def rift():
    img=Image.new("RGBA",(256*S,256*S),(0,0,0,0)); d=ImageDraw.Draw(img)
    ellipse(d,(24,141,232,207),(0,0,0,75))
    poly(d,[(128,94),(224,142),(128,191),(32,142)],"#421c2e","#170d13",4)
    poly(d,[(128,109),(195,143),(128,177),(61,143)],"#f0445c")
    poly(d,[(128,119),(177,143),(128,168),(79,143)],"#45192e")
    for x,y in [(77,112),(179,118),(105,78),(154,86)]:
        poly(d,[(x,y-14),(x+8,y+8),(x,y+16),(x-7,y+7)],"#ff694f")
    save(img,"spawn_rift.png")


def projectile():
    img=Image.new("RGBA",(128*S,128*S),(0,0,0,0)); d=ImageDraw.Draw(img)
    for r,a in [(50,30),(34,65),(21,150)]:
        ellipse(d,(64-r,64-r,64+r,64+r),(54,236,239,a))
    ellipse(d,(48,48,80,80),"#bfffff")
    save(img,"projectile.png")


def main():
    OUT.mkdir(parents=True,exist_ok=True)
    ground(); base(); turret(); zombie(); rift(); projectile()
    print(f"Созданы ассеты: {OUT}")

if __name__ == '__main__': main()
