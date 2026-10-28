#!/bin/bash
# Construit build/MSPaint.jar et build/mspaint-linux_<version>_all.deb
# Prérequis : JDK >= 8 (javac, jar) et dpkg-deb.   Usage : ./build.sh
set -e
cd "$(dirname "$0")"

VERSION="1.0.0"
PKG="mspaint-linux"
MAINTAINER="Jérémy LAVET <jaylavet@gmail.com>"

rm -rf build
mkdir -p build/classes/icons

echo "==> Compilation des correctifs"
javac --release 8 -nowarn -encoding UTF-8 \
      -cp upstream/MSPaint-original.jar \
      -d build/classes $(find src -name '*.java')
cp icons/${PKG}_{16,24,32,48,64,128,256}.png build/classes/icons/

echo "==> Assemblage du jar"
cp upstream/MSPaint-original.jar build/MSPaint.jar
jar uf build/MSPaint.jar -C build/classes .

echo "==> Construction du paquet .deb"
D=build/deb
mkdir -p $D/DEBIAN $D/usr/bin $D/usr/share/$PKG $D/usr/share/applications $D/usr/share/doc/$PKG
cp build/MSPaint.jar $D/usr/share/$PKG/MSPaint.jar
install -m 755 packaging/mspaint $D/usr/bin/mspaint
install -m 644 packaging/$PKG.desktop $D/usr/share/applications/$PKG.desktop
for s in 16 24 32 48 64 128 256 512; do
  mkdir -p $D/usr/share/icons/hicolor/${s}x${s}/apps
  install -m 644 icons/${PKG}_$s.png $D/usr/share/icons/hicolor/${s}x${s}/apps/$PKG.png
done
install -m 644 README.md $D/usr/share/doc/$PKG/README.md

SIZE=$(du -sk $D/usr | cut -f1)
sed -e "s/@VERSION@/$VERSION/" -e "s/@SIZE@/$SIZE/" -e "s/@MAINTAINER@/$MAINTAINER/" \
    packaging/control.in > $D/DEBIAN/control
install -m 755 packaging/postinst $D/DEBIAN/postinst
install -m 755 packaging/postrm $D/DEBIAN/postrm

dpkg-deb --build --root-owner-group $D build/${PKG}_${VERSION}_all.deb

echo "==> Archive universelle (Arch/Manjaro, Fedora, openSUSE…)"
T=build/${PKG}-${VERSION}
mkdir -p $T/icons
cp build/MSPaint.jar README.md CHANGELOG.md $T/
cp packaging/$PKG.desktop $T/
install -m 755 packaging/install.sh $T/install.sh
cp icons/${PKG}_*.png $T/icons/
tar -C build -czf build/${PKG}-${VERSION}.tar.gz ${PKG}-${VERSION}

echo
echo "==> OK :"
echo "    build/${PKG}_${VERSION}_all.deb"
echo "    build/${PKG}-${VERSION}.tar.gz"
echo "    build/MSPaint.jar"
