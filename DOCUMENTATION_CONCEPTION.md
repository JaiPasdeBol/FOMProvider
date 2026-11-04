# Documentation de conception — FOM Provider

*Document technique interne : architecture, modèle de données et décisions de conception de l'application. Public visé : développeur reprenant ou faisant évoluer le code.*

## 1. Introduction et objectifs

**FOM Provider** est une application de bureau (Java / Swing, projet Maven) destinée à :

- charger et fusionner un ou plusieurs fichiers FOM (Federation Object Model, format OMT, norme IEEE 1516 / profil RPR-FOM) ;
- en explorer la hiérarchie de classes d'objets et d'interactions, héritage compris ;
- gérer des **fédérés** (participants d'une fédération HLA) et décider, pour chacun, des attributs d'objet et des interactions auxquels il doit souscrire ;
- produire des fichiers XML conformes au schéma `federatesFOMProvider.xsd`, exploitables ensuite dans le code des fédérés eux-mêmes.

L'application ne modifie jamais les fichiers FOM chargés (lecture seule) : seuls les fichiers de fédéré sont créés/modifiés, puisque c'est leur production qui constitue l'objectif de l'outil.

## 2. Vue d'ensemble de l'architecture

Le code est organisé en trois packages sous `com.jpbergeaud.fomprovider`, selon une séparation classique modèle / E-S / IHM, sans framework tiers (Swing pur, pas d'injection de dépendances) :

- **`model`** — classes de données pures (POJO), sans dépendance à Swing ni aux E/S : `FomModel`, `ObjectClassNode`, `AttributeInfo`, `InteractionClassNode`, `ParameterInfo`, `Federate`.
- **`parser`** — lecture et écriture des fichiers XML : `FomFileParser`, `FomMerger`, `FederateFileIO`, `FomParseException`.
- **`ui`** — la fenêtre et ses composants Swing : `MainFrame`, `DetailPanel`, `FomTreeFactory`, `FomTreeCellRenderer`, `CategoryNode`, `AppIcon`, `MascotIcon`, `SearchDialog`, `SearchIndex`, `SearchEntry`.

`Main` est le point d'entrée : il construit et affiche `MainFrame` sur l'Event Dispatch Thread, selon l'usage standard Swing.

Le projet suit la disposition Maven standard (`src/main/java`, `src/main/resources`), l'icône/mascotte (`icon.png`) étant placée dans les ressources pour se retrouver à la racine du classpath une fois compilée. L'application se lance via Maven (`mvn exec:java`) depuis le dossier du projet.

## 3. Modèle de données (`model`)

### FomModel

Racine du modèle **fusionné** : regroupe les classes d'objets et d'interactions de premier niveau (`HLAobjectRoot`, `HLAinteractionRoot`, etc.) issues de l'ensemble des fichiers FOM chargés dans la session. Expose `getObjectClasses()` / `getInteractionClasses()` et `clear()` (utilisé par *Fichier → Réinitialiser*).

### ObjectClassNode / InteractionClassNode

Représentent respectivement un élément `objectClass` et `interactionClass` de la FOM, avec le même principe de conception : nom, champs scalaires (`sharing`, `semantics`, et pour les interactions `transportation`/`order`), une map de sous-éléments (`attributes` ou `parameters`, indexée par nom) et une map de **sous-classes directes** (`children`), permettant de représenter récursivement toute la hiérarchie d'héritage HLA (`HLAobjectRoot` → `PhysicalEntity` → `Platform` → `Aircraft`, par exemple).

Ces mêmes instances servent à la fois de résultat de parsing pour un fichier unique et de nœud dans le modèle fusionné : `mergeFrom(other)` complète les champs encore vides sans jamais écraser une valeur déjà renseignée, ce qui permet de charger plusieurs fichiers FOM qui se complètent (cas d'une FOM de base enrichie par un fichier séparé) sans perdre d'information ni dépendre de l'ordre de chargement.

### AttributeInfo / ParameterInfo

Portent les champs textuels d'un `attribute` ou d'un `parameter` tels que lus dans le XML. `AttributeInfo` est plus riche (`dataType`, `updateType`, `updateCondition`, `valueRequired`, `ownership`, `sharing`, `transportation`, `order`, `semantics`) que `ParameterInfo` (`dataType`, `semantics`), ce qui reflète fidèlement le schéma OMT : une interaction HLA n'a pas de notion de *update*/*ownership* au niveau du paramètre, ces notions n'ayant de sens que pour un attribut d'objet dont l'état est répliqué dans le temps.

### Federate

Modélise un fédéré tel que décrit par un fichier conforme à `federatesFOMProvider.xsd` : un nom, une couleur (voir plus bas), un fichier associé éventuel, et surtout deux structures de sélection :

- `objectAttributes : Map<String, Set<String>>`, indexée par **nom complet et qualifié** de la classe d'objet (ex. `HLAobjectRoot.PhysicalEntity.Aircraft`), donnant pour chacune l'ensemble des attributs sélectionnés (une classe sans sélection n'a simplement pas d'entrée) ;
- `interactions : Set<String>`, les noms complets des classes d'interaction souscrites dans leur intégralité.

`isAttributeSelected`/`setAttributeSelected` et `isInteractionSelected`/`setInteractionSelected` encapsulent ces structures ; `hasAnyAttributeSelected` sert au calcul des pastilles de couleur dans l'arbre. Les vues `getObjectAttributesView()`/`getInteractionsView()` (non modifiables) sont celles utilisées par `FederateFileIO` pour l'écriture XML.

La **couleur** associée à un fédéré n'existe que pour l'affichage (en-têtes de colonne, pastilles de l'arbre) : elle n'est pas définie par le schéma XML et n'est donc jamais écrite dans le fichier — elle est réassignée cycliquement à chaque chargement, à partir d'une palette fixe de dix couleurs décorrélées (`MainFrame.FEDERATE_PALETTE`). Au-delà de dix fédérés simultanés, les couleurs se répètent (voir §8).

## 4. Lecture et écriture XML (`parser`)

### FomFileParser / FomMerger

`FomFileParser.parse(File)` lit un fichier FOM (format OMT) et produit un `FomModel`. Le parseur est **tolérant aux espaces de noms** (un fichier FOM peut ou non déclarer explicitement son espace de noms XML) et **durci contre les attaques XXE** : déclaration de type de document interdite (`disallow-doctype-decl`) et résolution d'entités externes désactivée, par précaution puisque les fichiers FOM proviennent potentiellement de tiers.

`FomMerger.mergeInto(FomModel cible, FomModel source)` fusionne récursivement un modèle nouvellement parsé dans le modèle courant, classe par classe, en s'appuyant sur `mergeFrom` (voir §3). C'est ce mécanisme qui permet à *Fichier → Charger des fichiers FOM...* d'être appelé plusieurs fois de suite sans perdre les fichiers déjà chargés.

### FederateFileIO

`parse(File)` / `write(Federate, File)` : symétrique de `FomFileParser` pour les fichiers de fédéré, avec le même durcissement XXE. L'espace de noms utilisé est `http://tempuri.org/FederateFOMProvider.xsd`, celui déclaré par `federatesFOMProvider.xsd`.

### FomParseException

Exception vérifiée unique utilisée par l'ensemble du package `parser` pour signaler un échec de lecture ou d'écriture ; l'IHM l'intercepte systématiquement pour l'afficher dans une boîte de dialogue (`JOptionPane`) plutôt que de laisser remonter une exception non gérée.

## 5. Le schéma `federatesFOMProvider.xsd`

Ce schéma définit le **format d'échange** entre l'IHM et le code des fédérés : chaque élément `FOMObject` référence une classe d'objet par son nom complet et qualifié et porte la liste des attributs sélectionnés pour ce fédéré ; chaque élément `FOMInteraction` référence une classe d'interaction dans son intégralité, conformément à la norme HLA qui ne permet pas de souscription paramètre par paramètre (voir §6, `DetailPanel`).

Le schéma a fait l'objet d'une correction en cours de projet : deux références de type (`FederateFOMObject`, `FederateFOMInteraction`) utilisées dans la définition du type racine `FederateFOMProvider` étaient dépourvues du préfixe d'espace de noms `tns:`, ce qui faisait échouer une validation XSD stricte (erreur `src-resolve.4.1`). Le schéma, maintenu comme document du projet claude.ai plutôt que dans le dépôt de code, a été corrigé en conséquence.

## 6. Interface graphique (`ui`)

### MainFrame

Fenêtre principale et contrôleur de haut niveau. Construit :

- la barre de menus (**Fichier** : charger des FOM, réinitialiser, quitter ; **Édition** : rechercher — voir plus bas ; **Fédérés** : créer, charger, renommer, enregistrer / enregistrer sous, mettre à jour tous les fédérés associés à un fichier, décharger) ;
- l'arborescence (`JTree`), dont le modèle est reconstruit par `FomTreeFactory` à chaque modification (chargement de FOM, création/chargement/renommage/déchargement de fédéré) ;
- le `DetailPanel` et la barre de statut.

Toutes les boîtes de dialogue d'ouverture/enregistrement de fichier (`JFileChooser`) s'ouvrent par défaut dans le **dossier du projet**, défini comme le répertoire de travail courant de l'application au lancement (`PROJECT_DIRECTORY = new File(System.getProperty("user.dir"))`) — ce qui correspond au dossier du projet lorsque l'application est lancée via Maven depuis celui-ci.

Un détail Swing notable : la mise à jour des pastilles de couleur dans l'arbre, suite à une case cochée dans le `DetailPanel`, n'est **pas** obtenue par un simple `tree.repaint()` (qui ne force pas `JTree` à recalculer la taille déjà mise en cache d'une ligne dont le contenu affiché change), mais par `DefaultTreeModel.nodeChanged(node)` sur le nœud actuellement sélectionné — voir §7.

### FomTreeFactory / CategoryNode

`FomTreeFactory.buildTreeModel(FomModel, List<Federate>)` construit le `DefaultTreeModel` affiché, avec trois catégories racines (**Fédérés**, **Objets**, **Interactions**, dans cet ordre), chacune portée par un simple nœud `CategoryNode` (porteur de libellé, sans logique propre).

Deux méthodes statiques, `fullObjectName` et `fullInteractionName`, remontent l'arbre depuis un nœud donné pour recalculer son **nom complet et qualifié** (ex. `HLAobjectRoot.PhysicalEntity.Aircraft`) en concaténant les noms des ancêtres. Cette logique est partagée par le renderer de l'arbre et, indépendamment (via les listes d'ancêtres passées explicitement), par `MainFrame`/`DetailPanel` — c'est cette chaîne qui sert de clé dans les structures de sélection de `Federate`.

### FomTreeCellRenderer

Rendu personnalisé des nœuds de l'arbre : ajoute, par fédéré ayant une sélection sur l'objet ou l'interaction correspondant (`Federate.hasAnyAttributeSelected` / `isInteractionSelected`), une **pastille HTML colorée** (`<font color='#rrggbb'>●</font>`) devant le libellé du nœud, dans la couleur du fédéré. Un nœud de fédéré porte, de la même façon, une unique pastille de sa propre couleur. Les trois cas (objet, interaction, fédéré) sont traités par des branches dédiées de ce renderer.

### DetailPanel

Panneau central, cœur fonctionnel de l'IHM : un `CardLayout` à trois états (vide / contenu objet-ou-interaction / fédéré) commute l'affichage selon la sélection dans l'arbre.

Pour une classe d'objet, les attributs affichés incluent ceux hérités des classes parentes (une colonne **Classe** indique la classe qui déclare effectivement chaque ligne), et une colonne à cocher est ajoutée par fédéré chargé pour signaler son accès à chaque attribut. La colonne **Nom** de l'attribut est **figée** en première position du tableau : elle reste visible quelle que soit la position de la barre de défilement horizontale (qui ne fait défiler que les colonnes de fédéré et les autres détails). Techniquement, ceci repose sur **deux `JTable` partageant le même `DefaultTableModel`** — l'une (`table`) affichant toutes les colonnes sauf la première, l'autre (`fixedColumnTable`) n'affichant que celle-ci, posée comme *row header* de la `JScrollPane` contenant la première. Voir §7 pour les deux pièges rencontrés avec cette technique.

Pour une classe d'interaction, la norme HLA imposant une souscription à l'interaction dans son ensemble (pas de sélection paramètre par paramètre), le tableau des paramètres n'est pas modifié : une table séparée, affichée au-dessus, liste une ligne par fédéré avec une case à cocher pour la souscription globale.

### AppIcon / MascotIcon

Deux classes volontairement indépendantes malgré leur source commune (`icon.png`) :

- **`AppIcon`** charge l'image une fois et fournit les tailles destinées à l'icône système de la fenêtre (barre des tâches, barre de titre...) via `windowIcons()` (liste de tailles, pour `Window.setIconImages`) et `scaled(int)`.
- **`MascotIcon`** recharge l'image indépendamment, pour l'illustration décorative affichée dans le panneau de détail lorsqu'aucun élément n'est sélectionné — séparation délibérée des responsabilités (icône système vs illustration d'IHM) plutôt qu'une dépendance directe à `AppIcon`.

Les deux s'appuient sur le même algorithme de redimensionnement de qualité (réduction progressive par paliers de moitié, interpolation bicubique), `Image.getScaledInstance()` donnant un résultat flou sur de forts facteurs de réduction. `MascotIcon.scaled(int)` va plus loin en renvoyant une image **multi-résolution** (`BaseMultiResolutionImage`, plusieurs variantes de x1 à x3 de la taille demandée) : sous Windows, lorsque l'affichage est mis à l'échelle (125 %, 150 %, 200 %...), Swing dessine les icônes à une résolution physique supérieure à la taille logique demandée, et une image bitmap unique s'y retrouve étirée par le système — donc floue — quelle que soit la qualité du redimensionnement d'origine ; fournir plusieurs résolutions laisse Swing choisir la variante adaptée à l'échelle réelle de l'écran.

### SearchDialog / SearchIndex / SearchEntry

Implémentent la fonction de recherche accessible via **Édition → Rechercher...** : une fenêtre modale (`SearchDialog`) propose un champ de texte et une liste filtrée en direct, pour sélectionner rapidement une classe d'objet, une classe d'interaction ou un attribut par son nom, sans avoir à parcourir manuellement l'arborescence.

- **`SearchEntry`** est une entrée immuable de l'index : sa nature (`Kind` — `OBJECT_CLASS`, `INTERACTION_CLASS` ou `ATTRIBUTE`), son nom simple (utilisé pour le filtrage), le nom complet et qualifié de la classe à sélectionner dans l'arborescence, et un indicateur précisant si ce nom complet désigne une classe d'interaction ou une classe d'objet. Pour une entrée `ATTRIBUTE`, le nom complet porté n'est jamais celui de l'attribut mais celui de **la classe d'objet qui le déclare** : c'est elle qui doit être sélectionnée, l'attribut lui-même n'étant pas un nœud de l'arbre.
- **`SearchIndex.build(FomModel)`** reconstruit, à partir du modèle fusionné, la liste exhaustive des entrées possibles : parcours récursif de `model.getObjectClasses()` (une entrée `OBJECT_CLASS` par classe, une entrée `ATTRIBUTE` par attribut qu'elle déclare) et de `model.getInteractionClasses()` (une entrée `INTERACTION_CLASS` par classe). Les paramètres d'interaction ne sont volontairement pas indexés : la norme HLA n'autorisant de s'abonner qu'à une interaction entière, un paramètre isolé n'aurait pas de cible de sélection pertinente dans l'arborescence (voir §6, `DetailPanel`). L'index est reconstruit à chaque ouverture de la fenêtre plutôt que maintenu à jour en continu, pour rester trivialement cohérent avec le modèle même après un chargement ou une réinitialisation.
- **`SearchDialog`** filtre la liste des `SearchEntry` sur leur nom simple (jamais sur leur nom complet) à chaque modification du champ de texte (`DocumentListener`), insensible à la casse, et plafonne l'affichage à 200 résultats pour rester réactif sur une FOM volumineuse (le compteur en bas de fenêtre indique alors le nombre réel de correspondances). La validation d'une ligne (double-clic, ou Entrée) transmet l'entrée choisie à un `Consumer<SearchEntry>` fourni par `MainFrame`, sans que `SearchDialog` ait besoin de connaître l'arborescence Swing.
- Côté `MainFrame`, la sélection effective est faite par deux méthodes de recherche en profondeur dans l'arbre (`findObjectClassNode`/`findInteractionClassNode`), qui comparent le nom complet reconstruit à chaque nœud (via `FomTreeFactory.fullObjectName`/`fullInteractionName`, voir §6) au nom complet porté par l'entrée choisie ; le nœud trouvé est ensuite sélectionné (`tree.setSelectionPath`) et rendu visible (`tree.scrollPathToVisible`), ce qui déclenche l'affichage de son détail via le `TreeSelectionListener` déjà en place — aucune logique d'affichage dédiée n'a donc été nécessaire côté `DetailPanel`.

## 7. Décisions de conception et pièges Swing rencontrés

Quelques choix méritent d'être documentés pour qui reprendrait ce code, les erreurs correspondantes ayant déjà été rencontrées et corrigées en cours de projet :

- **Colonne figée via deux `JTable`** (§6, `DetailPanel`) : un `DefaultTableModel.setDataVector()` reconstruit entièrement les colonnes de **chaque** `JTable` qui observe ce modèle (donc des deux), ce qui oblige à re-répartir les colonnes (`splitFrozenColumn()`) après chaque appel, avant tout ajustement de largeur. Par ailleurs, le tri par clic sur un en-tête n'affecte que le `RowSorter` du tableau concerné : les deux `JTable` doivent donc explicitement **partager la même instance de `RowSorter`** (`fixedColumnTable.setRowSorter(table.getRowSorter())`) pour rester alignés ligne à ligne lors d'un tri.
- **Largeur de la zone figée** : `JViewport.getPreferredSize()` délègue, pour une vue `Scrollable` (ce qu'est un `JTable`), à `getPreferredScrollableViewportSize()` — qui vaut par défaut 450×400 px pour n'importe quel `JTable`, indépendamment de la largeur réelle de ses colonnes. Sans fixer explicitement cette taille à la largeur réelle de la colonne "Nom" (`fixedColumnTable.setPreferredScrollableViewportSize(...)`), la zone figée s'affichait 450 px de large quel que soit son contenu, laissant un grand vide avant la première colonne défilante.
- **Rafraîchissement des pastilles de l'arbre** (§6, `MainFrame.onFederateSelectionChanged`) : `JTree` met en cache la taille de chaque ligne ; un `repaint()` seul ne force pas son recalcul lorsque le contenu rendu change de taille sans changement de structure du modèle. `DefaultTreeModel.nodeChanged(node)` force ce recalcul.
- **Lecture seule des fichiers FOM vs écriture des fichiers de fédéré** : choix architectural délibéré — l'outil a pour but de *produire* des fichiers de fédéré à partir d'une FOM *consultée*, jamais de modifier la FOM elle-même.
- **Résolution d'une entrée de recherche par parcours plutôt que par index persistant** : plutôt que de maintenir une table associant chaque nom complet à son nœud dans l'arbre (à invalider à chaque reconstruction de l'arbre), `MainFrame` refait un simple parcours en profondeur au moment où l'utilisateur choisit un résultat de recherche. Ce choix évite toute désynchronisation entre un index mis en cache et l'arbre effectivement affiché, au prix d'un parcours O(n) déclenché uniquement sur une action utilisateur explicite — négligeable face à la taille d'une FOM typique.

## 8. Limites connues et pistes d'évolution

- Pas de fonction d'annulation/rétablissement (undo/redo) sur les sélections de fédéré.
- Aucune vérification de cohérence entre un fédéré chargé et la ou les FOM effectivement chargées dans la session : un fédéré peut référencer un objet ou un attribut qui n'existe pas (ou plus) dans la FOM courante sans qu'un avertissement ne soit levé.
- La palette de couleurs de fédéré est cyclique sur dix couleurs ; au-delà de dix fédérés simultanément chargés, deux fédérés peuvent partager la même couleur à l'affichage.
- Pas de gestion de versions ni de différentiel sur les fichiers de fédéré (chaque enregistrement réécrit intégralement le fichier).
