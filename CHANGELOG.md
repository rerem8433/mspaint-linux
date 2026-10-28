# Changelog

Toutes les évolutions notables de ce projet sont listées ici.
Format inspiré de [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/), versions selon [SemVer](https://semver.org/lang/fr/).

## [1.0.0] – 2026-10-28

Première version publiée : le portage Java de 2022 est réparé, complété et traduit.

### Ajouté
- **Annuler / Rétablir** : `Ctrl+Z`, `Ctrl+Y` (ou `Ctrl+Maj+Z`) et boutons ↶ ↷ dans la barre d'onglets. Ça fonctionne pour tous les outils (pinceaux, formes, texte, pot, rogner, pivoter…).
- **Outil Texte** : zone de saisie sur le canevas, police, taille, gras, italique, souligné, barré, fond transparent ou opaque. `Échap` ou un clic à côté valide le texte.
- **Pot de peinture** : clic gauche = couleur 1, clic droit = couleur 2.
- **Toutes les formes** (21) : triangle, losange, pentagone, hexagone, flèches, étoiles à 4/5/6 branches, légendes, cœur, éclair…
- **Polygone** (un clic par sommet, double-clic pour fermer) et **Courbe** (tracer la ligne, puis glisser pour la courber).
- **Rogner, Redimensionner** (pourcentage ou pixels, proportions conservées), **Faire pivoter** (90° gauche/droite, 180°) et **Retourner** (horizontal/vertical).
- **Mode sombre** avec bouton ☀ en haut à droite. Le choix est mémorisé. Le dessin et la palette ne sont jamais altérés.
- **Interface entièrement en français**, y compris les boîtes de dialogue Ouvrir/Enregistrer.
- **Images récentes** réelles dans le menu Fichier (4 derniers fichiers).
- **Raccourcis** : `Ctrl+N/O/S/Maj+S/P/A/C/X/V`, `Suppr`, `Échap`, `Entrée`.
- Ouverture d'une image passée en argument (`mspaint image.png`, « Ouvrir avec »).
- Icône d'application, entrée de menu, paquet `.deb` et archive universelle.

### Corrigé
- Interface illisible (texte blanc sur fond blanc) avec un thème GTK sombre.
- **Ouvrir** ne faisait rien (fonction jamais codée).
- Une **ligne tracée vers la gauche ou vers le haut** était perdue ou réduite à un point ailleurs. Après chaque forme, l'outil basculait sur Sélection.
- La **gomme** laissait des trous quand on bougeait vite et ralentissait sur les grandes images.
- L'**enregistrement en JPEG/BMP** échouait sans aucun message (image avec transparence). L'enregistrement ne prenait que le premier calque.
- **Imprimer** ne fonctionnait pas.
- **Sélectionner tout** ne marchait que si l'outil Sélection était déjà actif.
- **Couleur 1 / Couleur 2** pouvaient rester sélectionnés ensemble, ce qui rendait la couleur 2 impossible à changer.
- « Définir comme arrière-plan du Bureau » et « À propos » ne faisaient rien.
- La liste « Images récentes » était fausse et figée.
