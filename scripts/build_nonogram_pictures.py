#!/usr/bin/env python3
"""Converts black-and-white PNGs into the nonogram picture data file.

Usage: python3 -I scripts/build_nonogram_pictures.py <image-root> <output-file>
<image-root> holds {category}/{NN}_{id}.png. Add a picture by adding it to PICKS.
The order of the output is the play order, and ids are stored in player saves:
never rename an id. Adding a picture reshuffles the order, which is safe:
progress is stored by id.
"""
import glob
import os
import random
import struct
import sys
import zlib

SIZE = 20
ORDER_SEED = 20261031

PICKS = {
    "animals": {"rabbit": "うさぎ", "bear": "くま", "koala": "コアラ", "fox": "きつね", "cow": "うし", "owl": "ふくろう",
                "penguin": "ペンギン", "duck": "あひる", "swan": "はくちょう", "rooster": "にわとり", "whale": "くじら",
                "fish": "さかな", "octopus": "たこ", "squid": "いか", "crab": "かに", "jellyfish": "くらげ",
                "starfish": "ひとで", "turtle": "かめ"},
    "food": {"apple": "りんご", "banana": "バナナ", "cherries": "さくらんぼ", "strawberry": "いちご", "grapes": "ぶどう",
             "watermelon": "すいか", "pineapple": "パイナップル", "carrot": "にんじん", "broccoli": "ブロッコリー",
             "rice_ball": "おにぎり", "taiyaki": "たいやき", "hamburger": "ハンバーガー", "pizza_slice": "ピザ",
             "popsicle": "アイスキャンディ", "candy": "キャンディ", "coffee_cup": "コーヒー"},
    "buildings": {"house": "いえ", "apartment": "マンション", "castle": "おしろ", "japanese_castle": "天守閣",
                  "torii_gate": "鳥居", "five_story_pagoda": "五重塔", "church": "教会", "cathedral": "大聖堂",
                  "parthenon": "神殿", "pyramid": "ピラミッド", "lighthouse": "灯台", "windmill": "風車", "factory": "工場",
                  "suspension_bridge": "つり橋", "circus_tent": "サーカス", "igloo": "かまくら"},
    "plants": {"tulip": "チューリップ", "sunflower": "ひまわり", "plum_blossom": "うめ", "lotus": "はす", "daisy": "デイジー",
               "three_leaf_clover": "クローバー", "maple_leaf": "もみじ", "fern_frond": "シダ", "potted_sprout": "ふたば",
               "pine_tree": "もみの木", "broadleaf_tree": "木", "palm_tree": "やしの木", "saguaro_cactus": "サボテン",
               "potted_flower": "はちうえ", "bonsai": "ぼんさい", "mushroom": "きのこ"},
    "tools": {"hammer": "ハンマー", "saw": "のこぎり", "scissors": "はさみ", "key": "かぎ", "padlock": "南京錠", "umbrella": "かさ",
              "eyeglasses": "めがね", "crown": "おうかん", "top_hat": "シルクハット", "anchor": "いかり", "trumpet": "トランペット",
              "hourglass": "砂時計", "alarm_clock": "目覚まし時計", "camera": "カメラ", "headphones": "ヘッドホン",
              "hanger": "ハンガー", "magnet": "じしゃく"},
    "vehicles": {"suv": "くるま", "bus": "バス", "double_decker_bus": "2階建てバス", "tractor": "トラクター",
                 "excavator": "ショベルカー", "bicycle": "じてんしゃ", "scooter": "スクーター", "steam_locomotive": "機関車",
                 "cable_car": "ロープウェイ", "sailboat": "ヨット", "cruise_ship": "客船", "submarine": "せんすいかん",
                 "jet_airplane": "ひこうき", "hot_air_balloon": "気球", "blimp": "飛行船", "rocket": "ロケット", "ufo": "UFO"},
}


def read_png(path):
    d=open(path,'rb').read(); p=8; idat=b''; plte=None
    while p<len(d):
        n,t=struct.unpack('>I4s',d[p:p+8]); c=d[p+8:p+8+n]; p+=12+n
        if t==b'IHDR': w,h,bd,ct,_,_,il=struct.unpack('>IIBBBBB',c)
        elif t==b'PLTE': plte=c
        elif t==b'IDAT': idat+=c
    assert bd==8 and il==0,(bd,il)
    ch={0:1,2:3,3:1,4:2,6:4}[ct]; raw=zlib.decompress(idat); st=w*ch
    prev=bytearray(st); rows=[]; i=0
    for y in range(h):
        f=raw[i]; line=bytearray(raw[i+1:i+1+st]); i+=1+st
        for x in range(st):
            a=line[x-ch] if x>=ch else 0; b=prev[x]; c=prev[x-ch] if x>=ch else 0
            if f==1: line[x]=(line[x]+a)&255
            elif f==2: line[x]=(line[x]+b)&255
            elif f==3: line[x]=(line[x]+(a+b)//2)&255
            elif f==4:
                q=a+b-c; pa,pb,pc=abs(q-a),abs(q-b),abs(q-c)
                line[x]=(line[x]+(a if pa<=pb and pa<=pc else b if pb<=pc else c))&255
        rows.append(line); prev=line
    gray=[]
    for r in rows:
        if ct in(0,4): gray.append([r[x*ch] for x in range(w)])
        elif ct==3: gray.append([plte[r[x]*3+1] for x in range(w)])
        else: gray.append([(r[x*ch]+r[x*ch+1]+r[x*ch+2])//3 for x in range(w)])
    return w,h,gray
def bbox(w,h,g):
    ys=[y for y in range(h) if any(v<128 for v in g[y])]; xs=[x for x in range(w) if any(g[y][x]<128 for y in range(h))]
    x0,x1,y0,y1=min(xs),max(xs)+1,min(ys),max(ys)+1
    s=max(x1-x0,y1-y0); cx=(x0+x1)//2; cy=(y0+y1)//2
    return cx-s//2,cy-s//2,s
def down(g,w,h,box,n,th=.5):
    x0,y0,s=box; out=[]
    for r in range(n):
        for c in range(n):
            ya,yb=y0+r*s//n,y0+(r+1)*s//n; xa,xb=x0+c*s//n,x0+(c+1)*s//n
            t=k=0
            for y in range(max(ya,0),min(yb,h)):
                for x in range(max(xa,0),min(xb,w)):
                    t+=1;k+=g[y][x]<128
            out.append(t>0 and k/t>=th)
    return out


def main(root, output):
    entries = []
    for category, names in PICKS.items():
        for name, label in names.items():
            path = glob.glob(f"{root}/{category}/[0-9][0-9]_{name}.png")
            assert len(path) == 1, name
            width, height, gray = read_png(path[0])
            bits = down(gray, width, height, bbox(width, height, gray), SIZE)
            entries.append((name, label, "".join("1" if bit else "0" for bit in bits)))
    random.Random(ORDER_SEED).shuffle(entries)
    with open(output, "w", encoding="utf-8") as out:
        for entry in entries:
            out.write("\t".join(entry) + "\n")
    print(len(entries), "pictures")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
