from PIL import Image, ImageDraw, ImageFilter
S=1024
im=Image.new('RGBA',(S,S),(0,0,0,0)); d=ImageDraw.Draw(im)
# ombre
sh=Image.new('RGBA',(S,S),(0,0,0,0)); sd=ImageDraw.Draw(sh)
sd.ellipse((110,190,930,880),fill=(0,0,0,90)); sh=sh.filter(ImageFilter.GaussianBlur(30)); im.alpha_composite(sh)
# palette
d.ellipse((90,150,910,840),fill=(245,222,179,255),outline=(176,132,80,255),width=18)
d.ellipse((560,560,720,700),fill=(0,0,0,0))  # trou
hole=Image.new('L',(S,S),0); ImageDraw.Draw(hole).ellipse((560,560,720,700),fill=255)
a=im.getchannel('A'); a.paste(0,mask=hole); im.putalpha(a)
d=ImageDraw.Draw(im); d.ellipse((560,560,720,700),outline=(176,132,80,255),width=14)
for (x,y,c) in [(250,330,(230,40,40)),(420,250,(255,190,20)),(610,270,(40,170,60)),(760,400,(30,110,230)),(270,560,(150,60,200)),(420,690,(20,20,20))]:
    d.ellipse((x-70,y-70,x+70,y+70),fill=c+(255,),outline=(0,0,0,60),width=6)
# pinceau
br=Image.new('RGBA',(S,S),(0,0,0,0)); bd=ImageDraw.Draw(br)
bd.rounded_rectangle((470,40,560,560),radius=40,fill=(150,85,35,255))
bd.rectangle((462,520,568,640),fill=(190,190,200,255))
bd.polygon([(462,640),(568,640),(540,800),(515,860),(490,800)],fill=(40,40,40,255))
bd.polygon([(490,800),(540,800),(515,880)],fill=(30,110,230,255))
br=br.rotate(-35,center=(515,460),resample=Image.BICUBIC)
im.alpha_composite(br)
for s in (16,24,32,48,64,128,256,512):
    im.resize((s,s),Image.LANCZOS).save(f'mspaint-linux_{s}.png')
