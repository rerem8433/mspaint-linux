#!/bin/bash
# Installateur universel de Paint (toutes distributions : Arch/Manjaro, Fedora, openSUSE, Debian/Ubuntu…)
#   ./install.sh              -> installe pour l'utilisateur courant (~/.local)
#   sudo ./install.sh         -> installe pour tout le système (/usr/local)
#   ./install.sh --uninstall  -> désinstalle (même logique utilisateur / sudo)
set -e
cd "$(dirname "$0")"
PKG=mspaint-linux

if [ "$(id -u)" -eq 0 ]; then
  PREFIX=/usr/local
  BIN=$PREFIX/bin
  SHARE=$PREFIX/share
else
  BIN=$HOME/.local/bin
  SHARE=${XDG_DATA_HOME:-$HOME/.local/share}
fi
APPDIR=$SHARE/$PKG
APPS=$SHARE/applications
ICONS=$SHARE/icons/hicolor

refresh() {
  command -v update-desktop-database >/dev/null 2>&1 && update-desktop-database -q "$APPS" 2>/dev/null || true
  command -v gtk-update-icon-cache >/dev/null 2>&1 && gtk-update-icon-cache -q -f -t "$ICONS" 2>/dev/null || true
  command -v kbuildsycoca6 >/dev/null 2>&1 && kbuildsycoca6 >/dev/null 2>&1 || true
  command -v kbuildsycoca5 >/dev/null 2>&1 && kbuildsycoca5 >/dev/null 2>&1 || true
}

if [ "$1" = "--uninstall" ]; then
  rm -rf "$APPDIR"
  rm -f "$BIN/mspaint" "$APPS/$PKG.desktop"
  for s in 16 24 32 48 64 128 256 512; do rm -f "$ICONS/${s}x${s}/apps/$PKG.png"; done
  refresh
  echo "Paint a été désinstallé."
  exit 0
fi

if ! command -v java >/dev/null 2>&1; then
  . /etc/os-release 2>/dev/null || true
  echo "ERREUR : Java n'est pas installé. Installe-le puis relance ce script :"
  case " $ID $ID_LIKE " in
    *" arch "*|*" manjaro "*) echo "  sudo pacman -S jre-openjdk" ;;
    *" fedora "*|*" rhel "*)  echo "  sudo dnf install java-21-openjdk" ;;
    *" suse "*|*" opensuse "*) echo "  sudo zypper install java-21-openjdk" ;;
    *" debian "*|*" ubuntu "*) echo "  sudo apt install default-jre" ;;
    *) echo "  (paquet OpenJDK / JRE de ta distribution, version 8 ou plus)" ;;
  esac
  exit 1
fi

# nettoyage de l'ancienne installation par script (versions de test)
if [ "$(id -u)" -ne 0 ]; then
  rm -rf "$HOME/.local/share/mspaint" "$HOME/.local/share/applications/mspaint.desktop"
fi

mkdir -p "$APPDIR" "$BIN" "$APPS"
install -m 644 MSPaint.jar "$APPDIR/MSPaint.jar"
cat > "$BIN/mspaint" <<EOF
#!/bin/sh
exec java -Dawt.useSystemAAFontSettings=on -Dswing.aatext=true -jar "$APPDIR/MSPaint.jar" "\$@"
EOF
chmod 755 "$BIN/mspaint"
sed "s|^Exec=.*|Exec=\"$BIN/mspaint\" %f|" $PKG.desktop > "$APPS/$PKG.desktop"
chmod 644 "$APPS/$PKG.desktop"
for s in 16 24 32 48 64 128 256 512; do
  mkdir -p "$ICONS/${s}x${s}/apps"
  install -m 644 "icons/${PKG}_$s.png" "$ICONS/${s}x${s}/apps/$PKG.png"
done
refresh

echo "OK : Paint est installé."
echo "  - Menu des applications > Graphisme > Paint"
echo "  - Clic droit sur une image > Ouvrir avec > Paint"
echo "  - Commande : $BIN/mspaint [image]"
