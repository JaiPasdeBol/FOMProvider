# Design Documentation — FOM Provider

*Internal technical document: architecture, data model and design decisions of the application. Intended audience: a developer picking up or extending the code.*

## 1. Introduction and goals

**FOM Provider** is a desktop application (Java / Swing, Maven project) built to:

- load and merge one or more FOM files (Federation Object Model, OMT format, IEEE 1516 standard / RPR-FOM profile);
- browse their hierarchy of object and interaction classes, inheritance included;
- manage **federates** (the participants of an HLA federation) and decide, for each one, which object attributes and which interactions it should subscribe to;
- produce XML files conforming to the `federatesFOMProvider.xsd` schema, to be used directly in the federates' own code.

The application never modifies the FOM files it loads (read-only): only federate files are created/modified, since producing them is the whole point of the tool.

## 2. Architecture overview

The code is organized into three packages under `com.jpbergeaud.fomprovider`, following a classic model / parsing-and-IO / UI separation, with no third-party framework (plain Swing, no dependency injection):

- **`model`** — pure data classes (POJOs), with no dependency on Swing or I/O: `FomModel`, `ObjectClassNode`, `AttributeInfo`, `InteractionClassNode`, `ParameterInfo`, `Federate`.
- **`parser`** — reading and writing XML files: `FomFileParser`, `FomMerger`, `FederateFileIO`, `FomParseException`.
- **`ui`** — the window and its Swing components: `MainFrame`, `DetailPanel`, `FomTreeFactory`, `FomTreeCellRenderer`, `CategoryNode`, `AppIcon`, `MascotIcon`, `SearchDialog`, `SearchIndex`, `SearchEntry`.

`Main` is the entry point: it builds and shows `MainFrame` on the Event Dispatch Thread, following standard Swing practice.

The project follows the standard Maven layout (`src/main/java`, `src/main/resources`), with the icon/mascot (`icon.png`) placed under resources so it ends up at the root of the classpath once compiled. The application is launched via Maven (`mvn exec:java`) from the project folder.

## 3. Data model (`model`)

### FomModel

The root of the **merged** model: it groups the top-level object and interaction classes (`HLAobjectRoot`, `HLAinteractionRoot`, etc.) coming from every FOM file loaded in the session. Exposes `getObjectClasses()` / `getInteractionClasses()` and `clear()` (used by *File → Reset*).

### ObjectClassNode / InteractionClassNode

Represent an `objectClass` and an `interactionClass` element of the FOM respectively, following the same design: a name, scalar fields (`sharing`, `semantics`, and for interactions `transportation`/`order`), a map of sub-elements (`attributes` or `parameters`, indexed by name), and a map of **direct subclasses** (`children`), which allows the whole HLA inheritance hierarchy to be represented recursively (`HLAobjectRoot` → `PhysicalEntity` → `Platform` → `Aircraft`, for instance).

The same instances serve both as the parsing result for a single file and as a node in the merged model: `mergeFrom(other)` fills in fields that are still empty without ever overwriting a value that's already set, which lets several complementary FOM files be loaded (a base FOM enriched by a separate file, for example) without losing information or depending on load order.

### AttributeInfo / ParameterInfo

Carry the textual fields of an `attribute` or a `parameter` as read from the XML. `AttributeInfo` is richer (`dataType`, `updateType`, `updateCondition`, `valueRequired`, `ownership`, `sharing`, `transportation`, `order`, `semantics`) than `ParameterInfo` (`dataType`, `semantics`), which faithfully mirrors the OMT schema: an HLA interaction has no notion of *update*/*ownership* at the parameter level, since those only make sense for an object attribute whose state is replicated over time.

### Federate

Models a federate as described by a file conforming to `federatesFOMProvider.xsd`: a name, a color (see below), an optional associated file, and above all two selection structures:

- `objectAttributes : Map<String, Set<String>>`, indexed by the **full, qualified name** of the object class (e.g. `HLAobjectRoot.PhysicalEntity.Aircraft`), giving for each one the set of selected attributes (a class with no selection simply has no entry);
- `interactions : Set<String>`, the full names of the interaction classes subscribed to in their entirety.

`isAttributeSelected`/`setAttributeSelected` and `isInteractionSelected`/`setInteractionSelected` encapsulate these structures; `hasAnyAttributeSelected` is used to compute the colored dots in the tree. The unmodifiable views `getObjectAttributesView()`/`getInteractionsView()` are the ones used by `FederateFileIO` to write the XML.

The **color** associated with a federate exists only for display purposes (column headers, tree dots): it isn't defined by the XML schema and is therefore never written to the file — it is reassigned cyclically on each load, drawn from a fixed palette of ten visually distinct colors (`MainFrame.FEDERATE_PALETTE`). Beyond ten simultaneously loaded federates, colors repeat (see §8).

## 4. Reading and writing XML (`parser`)

### FomFileParser / FomMerger

`FomFileParser.parse(File)` reads a FOM file (OMT format) and produces a `FomModel`. The parser is **namespace-tolerant** (a FOM file may or may not explicitly declare its XML namespace) and **hardened against XXE attacks**: document-type declarations are disallowed (`disallow-doctype-decl`) and external entity resolution is disabled, as a precaution since FOM files may come from third parties.

`FomMerger.mergeInto(FomModel target, FomModel source)` recursively merges a newly parsed model into the current one, class by class, relying on `mergeFrom` (see §3). This is the mechanism that lets *File → Load FOM files...* be called several times in a row without losing files already loaded.

### FederateFileIO

`parse(File)` / `write(Federate, File)`: the counterpart of `FomFileParser` for federate files, with the same XXE hardening. The namespace used is `http://tempuri.org/FederateFOMProvider.xsd`, the one declared by `federatesFOMProvider.xsd`.

### FomParseException

A single checked exception used throughout the `parser` package to signal a read or write failure; the UI always catches it to display it in a dialog box (`JOptionPane`) rather than letting an unhandled exception propagate.

## 5. The `federatesFOMProvider.xsd` schema

This schema defines the **exchange format** between the UI and the federates' own code: each `FOMObject` element references an object class by its full, qualified name and carries the list of attributes selected for that federate; each `FOMInteraction` element references an interaction class in its entirety, in line with the HLA standard, which doesn't allow parameter-by-parameter subscription (see §6, `DetailPanel`).

The schema was fixed once during the project: two type references (`FederateFOMObject`, `FederateFOMInteraction`) used in the definition of the root type `FederateFOMProvider` were missing the `tns:` namespace prefix, which caused strict XSD validation to fail (`src-resolve.4.1` error). The schema, kept as a claude.ai project document rather than in the code repository, was corrected accordingly.

## 6. User interface (`ui`)

### MainFrame

The main window and top-level controller. Builds:

- the menu bar (**File**: load FOMs, reset, quit; **Edit**: search — see below; **Federates**: create, load, rename, save / save as, update all federates with an associated file, unload);
- the tree (`JTree`), whose model is rebuilt by `FomTreeFactory` on every change (FOM load, federate creation/loading/renaming/unloading);
- the `DetailPanel` and the status bar.

All file open/save dialogs (`JFileChooser`) open by default in the **project folder**, defined as the application's current working directory at launch (`PROJECT_DIRECTORY = new File(System.getProperty("user.dir"))`) — which matches the project folder when the application is launched via Maven from it.

One notable Swing detail: refreshing the colored dots in the tree after a checkbox is toggled in the `DetailPanel` is **not** done with a plain `tree.repaint()` (which doesn't force `JTree` to recompute a row's already-cached size when its rendered content changes size), but with `DefaultTreeModel.nodeChanged(node)` on the currently selected node — see §7.

### FomTreeFactory / CategoryNode

`FomTreeFactory.buildTreeModel(FomModel, List<Federate>)` builds the displayed `DefaultTreeModel`, with three root categories (**Federates**, **Objects**, **Interactions**, in that order), each carried by a simple `CategoryNode` (a label holder with no logic of its own).

Two static methods, `fullObjectName` and `fullInteractionName`, walk up the tree from a given node to recompute its **full, qualified name** (e.g. `HLAobjectRoot.PhysicalEntity.Aircraft`) by concatenating its ancestors' names. This logic is shared by the tree's cell renderer and, independently (via explicitly passed ancestor lists), by `MainFrame`/`DetailPanel` — this string is the key used throughout `Federate`'s selection structures.

### FomTreeCellRenderer

Custom rendering of tree nodes: for each federate with a selection on the corresponding object or interaction (`Federate.hasAnyAttributeSelected` / `isInteractionSelected`), it adds a **colored HTML dot** (`<font color='#rrggbb'>●</font>`) in front of the node's label, in that federate's color. A federate node likewise carries a single dot of its own color. The three cases (object, interaction, federate) are each handled by a dedicated branch of this renderer.

### DetailPanel

The central panel, the functional core of the UI: a `CardLayout` with three states (empty / object-or-interaction content / federate) switches the display according to the tree selection.

For an object class, the displayed attributes include those inherited from parent classes (a **Class** column shows which class actually declares each row), and a checkbox column is added per loaded federate to flag its access to each attribute. The attribute's **Name** column is **frozen** in the first position of the table: it stays visible no matter where the horizontal scrollbar is (which only scrolls the federate columns and the other details). Technically, this relies on **two `JTable`s sharing the same `DefaultTableModel`** — one (`table`) showing every column except the first, the other (`fixedColumnTable`) showing only that first one, placed as the *row header* of the `JScrollPane` holding the former. See §7 for the two pitfalls encountered with this technique.

For an interaction class, since the HLA standard requires subscribing to the interaction as a whole (no parameter-by-parameter selection), the parameter table itself is left untouched: a separate table, shown above it, lists one row per federate with a checkbox for the whole-interaction subscription.

### AppIcon / MascotIcon

Two classes deliberately kept independent despite sharing the same source (`icon.png`):

- **`AppIcon`** loads the image once and provides the sizes needed for the window's system icon (taskbar, title bar...) via `windowIcons()` (a list of sizes, for `Window.setIconImages`) and `scaled(int)`.
- **`MascotIcon`** reloads the image independently, for the decorative illustration shown in the detail panel when nothing is selected — a deliberate separation of concerns (system icon vs. decorative UI illustration) rather than a direct dependency on `AppIcon`.

Both rely on the same quality-scaling algorithm (progressive halving plus bicubic interpolation), since `Image.getScaledInstance()` produces a blurry result at large reduction factors. `MascotIcon.scaled(int)` goes further and returns a **multi-resolution** image (`BaseMultiResolutionImage`, several variants from x1 to x3 of the requested size): on Windows, when the display is scaled (125%, 150%, 200%...), Swing paints icons at a physical resolution higher than the logical size requested, and a single bitmap image ends up stretched by the system — hence blurry — no matter how good the original scaling was; providing several resolutions lets Swing pick the variant that matches the screen's actual scale factor.

## 7. Notable design decisions and Swing pitfalls encountered

A few choices are worth documenting for whoever picks up this code, since the corresponding mistakes were actually made and fixed during the project:

- **Frozen column via two `JTable`s** (§6, `DetailPanel`): a `DefaultTableModel.setDataVector()` call fully rebuilds the columns of **every** `JTable` observing that model (so both of them), which means the columns have to be re-split (`splitFrozenColumn()`) after every such call, before any width adjustment. In addition, sorting by clicking a header only affects the `RowSorter` of the table it belongs to: the two `JTable`s must therefore explicitly **share the same `RowSorter` instance** (`fixedColumnTable.setRowSorter(table.getRowSorter())`) to stay row-aligned when sorting.
- **Width of the frozen area**: `JViewport.getPreferredSize()` delegates, for a `Scrollable` view (which a `JTable` is), to `getPreferredScrollableViewportSize()` — which defaults to 450×400 px for any `JTable`, regardless of its columns' actual width. Without explicitly setting this size to the real width of the "Name" column (`fixedColumnTable.setPreferredScrollableViewportSize(...)`), the frozen area displayed at 450 px wide no matter its content, leaving a large empty gap before the first scrolling column.
- **Refreshing the tree's dots** (§6, `MainFrame.onFederateSelectionChanged`): `JTree` caches each row's size; a plain `repaint()` alone doesn't force it to recompute that size when the rendered content changes size without a model structure change. `DefaultTreeModel.nodeChanged(node)` forces that recomputation.
- **Read-only FOM files vs. writable federate files**: a deliberate architectural choice — the tool's purpose is to *produce* federate files from a FOM that is only ever *consulted*, never to modify the FOM itself.

## 8. Known limitations and possible future work

- No undo/redo on federate selections.
- No consistency check between a loaded federate and the FOM(s) actually loaded in the session: a federate can reference an object or attribute that doesn't (or no longer) exist in the current FOM without any warning being raised.
- The federate color palette cycles over ten colors; beyond ten simultaneously loaded federates, two federates may end up sharing the same display color.
- No version control or diffing on federate files (every save fully rewrites the file).
