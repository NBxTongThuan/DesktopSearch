# Desktop Search — UI/UX Design v1

**Document:** UI/UX Design  
**Version:** 1.0  
**Status:** Draft for Implementation  
**Project:** Desktop Search  
**Target Platform:** Windows Desktop  
**UI Technology:** JavaFX  
**Primary Interaction:** Keyboard-first Search

---

# 1. Purpose

Tài liệu này định nghĩa trải nghiệm người dùng và kiến trúc giao diện của Desktop Search.

Phạm vi bao gồm:

- main window;
- search bar;
- search result list;
- filters;
- sorting;
- context actions;
- indexing status;
- settings;
- indexed locations;
- exclusions;
- index management;
- loading/error/empty states;
- keyboard navigation;
- background task interaction;
- UI state;
- responsiveness;
- JavaFX architecture;
- accessibility;
- visual guidelines.

Tài liệu không nhằm mô tả pixel-perfect UI.

Mục tiêu chính là trả lời:

```text
User sees what?
User can do what?
When an event happens, UI changes how?
Which application service is invoked?
Which operation is background?
Which state is displayed?
```

---

# 2. UX Goals

Desktop Search hướng tới trải nghiệm:

```text
Fast
Minimal
Keyboard-friendly
Predictable
Low distraction
Local-first
```

Trọng tâm không phải tạo một file manager đầy đủ.

Trọng tâm là:

```text
Open App
   ↓
Type
   ↓
Find
   ↓
Open File
```

Toàn bộ core flow nên thực hiện được trong vài giây.

---

# 3. Primary User Flow

Flow chính:

```text
Launch Desktop Search
        │
        ▼
Search field focused
        │
        ▼
User types query
        │
        ▼
Debounce
        │
        ▼
Search runs in background
        │
        ▼
Results displayed
        │
        ▼
User navigates with keyboard/mouse
        │
        ▼
Open file / directory
```

Target UX:

```text
Launch → type → Enter
```

không cần nhiều interaction phụ.

---

# 4. Main Window

Main Window gồm:

```text
┌──────────────────────────────────────────────────────────────┐
│ Desktop Search                                  ⚙   ─ □ X   │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│ 🔍  Search files...                                      ✕   │
│                                                              │
├──────────────────────────────────────────────────────────────┤
│ Type: All ▼      Modified: Any ▼       Sort: Relevance ▼    │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│                   SEARCH RESULTS                             │
│                                                              │
│                                                              │
├──────────────────────────────────────────────────────────────┤
│ Ready                                      128,542 indexed   │
└──────────────────────────────────────────────────────────────┘
```

Main Window chia thành:

```text
Title Area
Search Area
Filter Bar
Result Area
Status Bar
```

---

# 5. Initial Window Size

Recommended initial size:

```text
Width:  900–1100 px
Height: 600–750 px
```

Minimum size:

```text
Width:  ~700 px
Height: ~450 px
```

UI phải resize được.

Không hard-code layout dựa trên fixed pixels ngoài spacing/minimum sensible size.

---

# 6. Startup Behavior

Khi application mở:

```text
Application start
      ↓
Window displayed
      ↓
Search box focused
```

User có thể bắt đầu gõ ngay.

Không yêu cầu click vào search box.

---

# 7. Search Bar

Search Bar là component quan trọng nhất.

Concept:

```text
┌─────────────────────────────────────────────┐
│ 🔍 spring transaction                   ✕  │
└─────────────────────────────────────────────┘
```

Components:

```text
Search icon
TextField
Clear button
```

---

# 8. Search Placeholder

Default placeholder:

```text
Search files...
```

Future có thể:

```text
Search files, folders and content...
```

nếu full-text indexing enable.

V1 ưu tiên đơn giản.

---

# 9. Search Behavior

Text changes:

```text
User types
    ↓
reset debounce timer
    ↓
wait 200 ms
    ↓
create SearchRequest
    ↓
background SearchService
```

Default:

```text
search debounce = 200 ms
```

Không search ngay từng key event.

---

# 10. Empty Query

Nếu query:

```text
""
```

V1 hiển thị empty/home state.

Không query Lucene toàn index.

UI:

```text
Search your files

Start typing to search indexed files and folders.
```

Future có thể show:

```text
Recent files
Recent searches
Pinned items
```

nhưng không nằm MVP.

---

# 11. Clear Search

Clear button xuất hiện khi:

```text
query is not empty
```

Click:

```text
clear query
clear result
focus search box
```

Keyboard:

```text
Esc
```

khi search box đang có text:

```text
clear query
```

---

# 12. Search-As-You-Type States

State flow:

```text
EMPTY
  │
  │ typing
  ▼
DEBOUNCING
  │
  ▼
SEARCHING
  │
  ├──────────► RESULTS
  │
  ├──────────► NO_RESULTS
  │
  └──────────► SEARCH_ERROR
```

`DEBOUNCING` không nhất thiết hiển thị ra UI.

---

# 13. Search Loading UX

Vì target latency rất thấp, không show large spinner ngay khi query bắt đầu.

Nếu query hoàn thành < ~150 ms:

```text
results update directly
```

Nếu > threshold:

```text
small subtle progress indicator
```

có thể xuất hiện gần status area.

Không overlay toàn màn hình.

Search vẫn là lightweight interaction.

---

# 14. Stale Query Handling

Example:

```text
query #21 = spring
query #22 = spring boot
```

Nếu #21 trả về sau #22:

```text
discard #21
```

UI chỉ update nếu:

```text
response.sequenceId == latestSequence
```

User không được thấy kết quả nhảy ngược.

---

# 15. Search Result Area

Result area sử dụng vertical list.

Concept:

```text
☕ TransactionService.java
   D:\Projects\desktop-search\src\service
   Java • 12 KB • Modified 10 Aug 2026

📄 Spring Transaction Notes.pdf
   D:\Documents\Java
   PDF • 2.4 MB • Modified 8 Aug 2026
```

---

# 16. Result Row

Mỗi row chứa ba level thông tin.

### Primary

```text
filename
```

### Secondary

```text
parent path
```

### Metadata

```text
type
size
modified date
```

Example:

```text
TransactionService.java
D:\Projects\Sphere\src\main\java\service
JAVA • 14.2 KB • Modified 10 Aug 2026 11:24
```

---

# 17. Result Information Priority

Priority:

```text
1. Filename
2. Path
3. Type
4. Modified date
5. Size
```

Không show quá nhiều metadata làm row rối.

Full path có thể truncate visual.

Tooltip hoặc context action có thể show/copy full path.

---

# 18. Result Row Height

Recommended:

```text
56–72 px
```

cho metadata layout ba dòng.

Nếu muốn compact mode future:

```text
40–48 px
```

V1 dùng comfortable layout.

---

# 19. Result Icons

Result có icon theo loại:

```text
Folder
PDF
Word
Excel
Text
Java/source
Image
Unknown file
```

V1 không cần extract Windows native icon cho mọi file.

Có thể dùng generic application icons theo extension.

Native icons là future enhancement.

---

# 20. Directory Result

Directory result:

```text
📁 desktop-search
   D:\Projects
   Folder • Modified ...
```

Double-click / Enter:

```text
open directory in Windows Explorer
```

---

# 21. File Result

File result:

```text
📄 document.pdf
```

Double-click / Enter:

```text
open using OS default application
```

Desktop Search không tự implement editor/viewer trong V1.

---

# 22. Result Selection

Result list có single-selection.

Mouse:

```text
single click → select
double click → open
```

Keyboard:

```text
↑ / ↓ → move selection
Enter → open selected item
```

---

# 23. Default Result Selection

Khi search result mới xuất hiện:

Option A:

```text
auto-select first result
```

Option B:

```text
no result selected
```

Recommended:

**Auto-select first result.**

Lý do:

keyboard flow nhanh:

```text
type
 ↓
Enter
```

mở result đầu tiên.

---

# 24. Selection While Typing

Khi query thay đổi và new result set arrives:

```text
selection resets to first result
```

Không giữ index cũ vì item semantics đã thay đổi.

---

# 25. Keyboard Navigation

Primary shortcuts:

```text
Ctrl + F
Ctrl + L
```

→ focus search box.

```text
Arrow Down
```

khi focus search box:

→ move focus/selection xuống result đầu tiên.

```text
Arrow Up / Down
```

→ navigate results.

```text
Enter
```

→ open selected item.

```text
Ctrl + Enter
```

→ open containing folder.

```text
Esc
```

→ clear query hoặc close active popup/dialog.

---

# 26. Optional Keyboard Shortcuts

Future:

```text
Ctrl + C
```

→ copy selected path.

```text
Alt + Enter
```

→ file properties.

```text
Ctrl + ,
```

→ Settings.

Không cần toàn bộ trong MVP.

---

# 27. Context Menu

Right-click result:

```text
Open
Open containing folder
────────────────────
Copy path
Copy file name
────────────────────
Properties
```

Potential future:

```text
Pin
Exclude this folder
Re-index file
```

Không đưa quá nhiều option V1.

---

# 28. Open Containing Folder

Action:

```text
Open containing folder
```

Expected Windows behavior:

```text
Explorer opens parent directory
selected file highlighted if possible
```

Platform implementation nằm:

```text
WindowsExplorerService
```

không nằm trực tiếp trong JavaFX cell.

---

# 29. Copy Path

Action:

```text
Copy path
```

copy absolute display path.

Example:

```text
D:\Projects\desktop-search\README.md
```

Feedback:

small transient status:

```text
Path copied
```

Không cần dialog.

---

# 30. Search Result Highlight

Filename keyword có thể được highlight.

Example query:

```text
transaction
```

Result:

```text
SpringTransactionService.java
      ^^^^^^^^^^^
```

V1 có thể highlight bằng style spans nếu implementation đơn giản.

Nếu gây complexity lớn, có thể đưa vào V1.1.

Không ảnh hưởng core usability.

---

# 31. Result Count

Status:

```text
73 results • 18 ms
```

Nếu index engine chỉ track approximate total:

```text
1,000+ results
```

acceptable.

Không cần exact total nếu expensive.

---

# 32. Result Limit

Default backend request:

```text
50 results
```

UI load ban đầu 50.

Nếu còn:

```text
Show more
```

hoặc load additional batch khi scroll.

V1 recommended:

```text
Load More
```

đơn giản hơn automatic infinite scroll.

---

# 33. Maximum UI Results

Safety limit:

```text
500
```

Không render 50,000 result rows.

User nên refine query thay vì browse toàn index.

---

# 34. JavaFX Result Component

Recommended:

```text
ListView<SearchResultViewModel>
```

với custom:

```text
ListCell<SearchResultViewModel>
```

Reasons:

- virtualized;
- selection model;
- keyboard navigation;
- reusable cells.

Không dùng VBox chứa hàng nghìn node.

---

# 35. Filter Bar

Filter Bar nằm dưới search input.

Initial:

```text
Type: All ▼
Modified: Any ▼
Sort: Relevance ▼
```

V1 không cần expose mọi search query feature bằng UI.

---

# 36. Type Filter

Options:

```text
All
Documents
Folders
Images
Source Code
Other
```

Alternative technical filter:

```text
Extension
```

Nhưng UX category thường dễ dùng hơn.

Inline query vẫn hỗ trợ:

```text
ext:pdf
```

---

# 37. Type Category Mapping

Possible:

```text
Documents
  pdf doc docx txt md rtf ppt pptx xls xlsx

Images
  jpg jpeg png gif bmp webp svg

Source Code
  java kt js ts py c cpp h cs go rs html css xml json
```

Mapping nên nằm application configuration, không hard-code trong JavaFX cell.

---

# 38. Modified Filter

Simple options:

```text
Any time
Today
Yesterday
Last 7 days
Last 30 days
```

Custom date range future.

---

# 39. Sort Control

Options:

```text
Relevance
Name
Modified
Size
```

Default:

```text
Relevance
```

Direction:

```text
Name → ascending default
Modified → newest first
Size → largest first
```

Future có direction toggle.

---

# 40. Filter Behavior

Filter change:

```text
change filter
    ↓
increment query sequence
    ↓
execute search immediately
```

Không cần debounce filter click.

---

# 41. Inline Query vs Filter UI

Search box:

```text
ext:pdf transaction
```

và UI type filter có thể coexist.

V1 recommendation:

UI filters compile vào structured `SearchRequest`.

Inline filters được parser xử lý.

Nếu conflict:

```text
UI explicit filter wins
```

Rule phải deterministic.

---

# 42. Status Bar

Bottom status bar:

```text
┌────────────────────────────────────────────────────────┐
│ 73 results • 18 ms                 128,542 files       │
└────────────────────────────────────────────────────────┘
```

Left:

```text
search status
```

Right:

```text
index status
```

---

# 43. Index Ready State

Example:

```text
128,542 files indexed
```

or shorter:

```text
128,542 indexed
```

---

# 44. Indexing State

During scan:

```text
Indexing D:\Projects...
124,521 files indexed
```

Do not show:

```text
67%
```

unless total count is known reliably.

---

# 45. Why No Fake Percentage

Scanner streams filesystem.

Before full traversal:

```text
total files unknown
```

Computing total first would require extra scan.

UX should display factual progress:

```text
files discovered
files indexed
failures
```

instead of misleading percentage.

---

# 46. Indexing Detail Popover

Click indexing status can open small panel:

```text
Indexing

Root:
D:\Projects

Discovered: 128,912
Indexed:    128,542
Skipped:    338
Failed:     32

[Stop indexing]
```

This can be future/mid-V1.

Main status remains compact.

---

# 47. Search During Indexing

Search remains enabled.

UI can show message:

```text
Indexing in progress — results may be incomplete.
```

Do not block user until initial indexing finishes.

This is important UX feature enabled by Lucene NRT.

---

# 48. Initial Setup State

First launch with no indexed roots:

```text
┌────────────────────────────────────────┐
│                                        │
│        No folders indexed yet          │
│                                        │
│ Choose folders you want Desktop Search │
│ to index.                              │
│                                        │
│        [ Add folder ]                  │
│                                        │
└────────────────────────────────────────┘
```

Search box can be disabled or remain inactive with clear message.

Recommended:

Search box exists but empty state guides configuration.

---

# 49. Add Folder Flow

User click:

```text
Add folder
```

→ JavaFX:

```text
DirectoryChooser
```

Select folder.

Then:

```text
persist root
    ↓
start background scan
    ↓
UI shows indexing state
```

No application restart.

---

# 50. Duplicate Root Handling

If user adds same root twice:

```text
D:\Projects
```

UI should not create duplicate.

Display informational message:

```text
This folder is already indexed.
```

---

# 51. Nested Root Handling

Example existing root:

```text
D:\Projects
```

User adds:

```text
D:\Projects\DesktopSearch
```

This is redundant.

Recommended UI:

```text
This folder is already included by D:\Projects.
```

Don't add nested root unless future config explicitly supports overrides.

---

# 52. Settings Screen

Settings accessible via:

```text
⚙
```

Top/right area.

Can open:

- separate Scene/Dialog;
- overlay;
- main content navigation.

Recommended V1:

**Dedicated settings view within same window or modal dialog.**

Avoid creating multiple application windows unnecessarily.

---

# 53. Settings Sections

```text
Settings
│
├── Indexed Locations
├── Exclusions
├── Search
├── Index
├── Appearance
└── About
```

Some sections can be future.

---

# 54. Indexed Locations Screen

Concept:

```text
Indexed Locations

D:\Projects
128,542 items                          [Remove]

D:\Documents
32,114 items                           [Remove]

                                     [+ Add folder]
```

Optional status:

```text
Indexing
Ready
Unavailable
```

---

# 55. Root Status

Possible status:

```text
READY
INDEXING
UNAVAILABLE
ERROR
```

Example:

```text
D:\ExternalDrive
Drive unavailable
```

Don't automatically remove root if removable drive disappears.

---

# 56. Remove Root

Click:

```text
Remove
```

Requires confirmation because index entries will be removed.

Dialog:

```text
Remove indexed location?

D:\Projects

Files on disk will not be deleted.
Only Desktop Search index entries will be removed.

[Cancel] [Remove]
```

Critical clarification:

**Never imply actual user files will be deleted.**

---

# 57. Exclusions Screen

Concept:

```text
Excluded Locations

D:\Projects\node_modules
D:\Projects\.git

[+ Add exclusion]
```

Can support:

```text
exact paths
directory names
patterns future
```

V1 recommended focus on paths.

---

# 58. Default Exclusions UX

System-level unsafe/unreadable paths can be skipped internally.

Developer folder exclusions like:

```text
node_modules
.git
target
```

should not necessarily be hidden magic defaults.

If defaults exist, settings should expose them.

Predictability > invisible behavior.

---

# 59. Search Settings

Potential:

```text
Search

[ ] Search file contents

Maximum content extraction file size:
50 MB
```

Content indexing disabled in initial metadata MVP.

When feature available, enabling it triggers additional indexing.

---

# 60. Content Indexing Enable Flow

User enables:

```text
Search file contents
```

UI warning:

```text
Desktop Search needs to process supported documents.
Indexing may take additional time and disk space.
```

Then:

```text
enable setting
   ↓
background content indexing
```

No blocking dialog.

---

# 61. Index Settings

Concept:

```text
Index

Indexed files:       128,542
Index size:          412 MB
Last indexed:        Today 13:42

[Rebuild Index]
```

Possible:

```text
Open index location
```

developer/debug only, not necessary user-facing.

---

# 62. Rebuild Index

This is destructive only to derived index data.

Confirmation:

```text
Rebuild search index?

Desktop Search will recreate its local search index.

Your files will not be modified or deleted.

Search results may be incomplete while rebuilding.

[Cancel] [Rebuild]
```

---

# 63. Rebuild State

During rebuild:

```text
Rebuilding index...
42,512 files indexed
```

Search behavior V1 can be:

```text
partial new index available as it builds
```

or temporary unavailable depending implementation.

Preferred if possible:

search partial new index.

But UX must reflect:

```text
Results may be incomplete while rebuilding.
```

---

# 64. Error States

UI must distinguish different failures.

Search:

```text
Search failed
```

Index:

```text
Index unavailable
```

Filesystem:

```text
Folder unavailable
```

Permission:

```text
Some files could not be indexed
```

---

# 65. Search Error

Example:

```text
Search is temporarily unavailable.

[Retry]
```

Do not show Java exception/stack trace to user.

Details go logs.

---

# 66. Index Fatal Error

Example:

```text
Search index could not be opened.

Desktop Search can rebuild the index from your files.

[Rebuild Index]
```

Avoid scary data-loss wording because index is derived.

---

# 67. Partial Index Errors

If:

```text
32 files failed
```

do not interrupt user with modal dialog.

Status:

```text
Index complete • 32 files skipped
```

Click can show details.

---

# 68. Permission Failure UX

Example:

```text
Some files could not be accessed due to Windows permissions.
```

No request to bypass permissions.

No elevation automatically.

---

# 69. No Results State

Query:

```text
spring transaction
```

No hits:

```text
No results for “spring transaction”

Try:
• a shorter search term
• removing filters
```

Don't show huge error styling.

Zero result is normal.

---

# 70. Filter No Results

If filters active:

```text
No results with the current filters.

[Clear filters]
```

Useful because query may otherwise be valid.

---

# 71. Window State Persistence

Application can remember:

```text
window width
window height
window position
last sort
```

Future/optional V1.

Should not necessarily remember search query across app restart.

---

# 72. Minimize Behavior

Standard Windows window behavior.

No tray app required V1.

Closing window:

```text
shutdown application
```

Graceful shutdown invokes indexing engine lifecycle.

Future tray mode can keep watcher running.

---

# 73. System Tray

**Not V1 requirement.**

Potential future:

```text
background indexing
global shortcut
quick search popup
```

Would change application lifecycle significantly.

Do not design core around tray yet.

---

# 74. Global Shortcut

Future feature:

```text
Alt + Space
```

or custom shortcut to summon search.

Not V1.

Would require OS-level integration.

---

# 75. Search Window Philosophy

Main UI should remain minimal.

Avoid:

```text
large sidebar
dashboard cards
analytics charts
file explorer tree
```

Desktop Search is a search utility.

User's eye should go directly to:

```text
search bar
results
```

---

# 76. Visual Hierarchy

Priority:

```text
1. Search input
2. Filename
3. Path
4. Filters
5. Metadata
6. Status information
```

Settings/index metrics must not compete visually with search.

---

# 77. Color Strategy

Use neutral desktop palette.

Light theme initial recommendation:

```text
Background       #F7F8FA
Surface          #FFFFFF
Primary text     #1F2328
Secondary text   #6B7280
Border           #E5E7EB
Accent           blue
Selection        subtle accent background
Error            red
Warning          amber
Success/status   green
```

Exact color palette should live in CSS constants/design tokens.

---

# 78. Dark Theme

Can be supported later with alternative CSS.

Architecture should avoid hard-coded colors in Java code.

Use style classes.

V1 can ship light theme only if desired.

---

# 79. Typography

Use Windows-friendly system font.

JavaFX default can be used initially.

Possible preference:

```text
Segoe UI
```

when available.

Suggested hierarchy:

```text
Search input        16–18
Filename            14–15 semibold
Path                12–13
Metadata            11–12
Status              11–12
```

---

# 80. Spacing

Use consistent spacing scale.

Example:

```text
4
8
12
16
24
32
```

Avoid arbitrary:

```text
13
17
21
```

across components.

---

# 81. Hover State

Result row hover:

```text
subtle background
```

Selected state must be visually stronger than hover.

Never rely only on color difference too subtle to notice.

---

# 82. Focus State

Keyboard focus must be visible.

Search input:

```text
accent border
```

Results:

```text
visible selected row
```

Settings controls:

standard focus ring.

Do not remove focus outline purely for aesthetics.

---

# 83. Accessibility

V1 should ensure:

- keyboard navigation;
- readable contrast;
- scalable UI;
- logical focus order;
- text labels for icon-only controls where possible;
- tooltip/accessibility text.

Main search must be fully usable without mouse.

---

# 84. Tooltip Policy

Use tooltip for ambiguous icons:

```text
Settings
Clear search
Add folder
```

Do not require tooltip to understand core result information.

---

# 85. JavaFX UI Architecture

Recommended architecture:

```text
View
  ↓
ViewModel / Controller
  ↓
Application Service
  ↓
Core API
```

Example:

```text
MainSearchView
      ↓
MainSearchViewModel
      ↓
SearchService
```

---

# 86. UI Must Not Access Lucene

Forbidden:

```java
searchField.setOnKeyReleased(event -> {
    IndexSearcher searcher = ...
});
```

Correct:

```text
UI
 ↓
SearchViewModel
 ↓
SearchService
```

Lucene stays behind search module.

---

# 87. UI Must Not Scan Filesystem

Forbidden:

```text
JavaFX Controller
   ↓
Files.walkFileTree(...)
```

Correct:

```text
Controller
   ↓
IndexCoordinator / IndexedLocationService
```

---

# 88. ViewModel Responsibilities

`MainSearchViewModel` owns presentation state:

```text
query
results
selectedResult
searching
resultCount
searchTime
activeFilters
sort
errorMessage
```

It coordinates SearchService asynchronously.

It does not implement Lucene query logic.

---

# 89. Suggested MainSearchViewModel

Conceptual properties:

```java
StringProperty query;

ObservableList<SearchResultViewModel> results;

ObjectProperty<SearchResultViewModel> selectedResult;

BooleanProperty searching;

LongProperty resultCount;

LongProperty searchDurationMillis;

ObjectProperty<SearchSort> sort;

ObjectProperty<SearchState> state;
```

JavaFX-specific properties should remain inside UI layer.

Core models remain plain Java records.

---

# 90. SearchResultViewModel

Wraps core:

```text
SearchResult
```

for UI needs.

Possible fields:

```text
displayName
displayPath
formattedSize
formattedModifiedTime
iconType
directory
```

Formatting does not belong in SearchResult domain record.

---

# 91. UI Search State Enum

Recommended:

```java
enum SearchUiState {
    EMPTY,
    SEARCHING,
    RESULTS,
    NO_RESULTS,
    ERROR
}
```

Could derive state instead of mutable enum.

Either approach acceptable.

---

# 92. Index UI State

Recommended:

```java
enum IndexUiState {
    NOT_CONFIGURED,
    READY,
    INDEXING,
    REBUILDING,
    ERROR
}
```

Maps from indexing engine status.

---

# 93. Main Layout JavaFX

Possible layout:

```text
BorderPane
│
├── top
│   └── VBox
│       ├── SearchBar
│       └── FilterBar
│
├── center
│   └── StackPane
│       ├── ResultList
│       ├── EmptyState
│       ├── NoResultState
│       └── ErrorState
│
└── bottom
    └── StatusBar
```

This maps naturally to state switching.

---

# 94. SearchBar Components

Possible:

```text
HBox
├── search icon
├── TextField
└── clear Button
```

Alternative custom control future.

---

# 95. FilterBar Components

```text
HBox
├── ComboBox<FileTypeFilter>
├── ComboBox<ModifiedFilter>
└── ComboBox<SearchSort>
```

Use strongly typed enum/models, not raw strings where possible.

---

# 96. StatusBar Components

```text
HBox
├── search status Label
├── spacer
└── index status Hyperlink/Button/Label
```

Index status can be clickable for details.

---

# 97. Settings Layout

Recommended:

```text
BorderPane
│
├── left
│   └── navigation list
│
└── center
    └── selected settings panel
```

For V1 small settings, simpler:

```text
TabPane
```

could work.

However custom navigation gives cleaner styling.

---

# 98. FXML vs Programmatic JavaFX

Two choices.

### FXML

Pros:

- layout separated from controller;
- visual structure readable.

Cons:

- runtime bindings/string identifiers;
- controller injection complexity.

### Programmatic Java

Pros:

- compile-time structure;
- easier refactoring;
- no FXML lifecycle confusion.

Recommended V1:

**Programmatic JavaFX or minimal FXML.**

Because UI size is moderate and project prioritizes Java design learning.

Final decision can be taken in Bootstrap Plan.

---

# 99. Recommended UI Implementation Style

Suggested:

```text
Programmatic JavaFX
+
small reusable custom controls
+
CSS
```

Example:

```text
MainView
SearchBar
ResultListView
ResultListCell
FilterBar
StatusBar
SettingsView
```

Avoid one giant `MainController.java`.

---

# 100. UI Package Structure

Recommended:

```text
com.desktopsearch.ui
│
├── app
│   └── DesktopSearchApplication.java
│
├── main
│   ├── MainView.java
│   ├── MainViewModel.java
│   └── MainWindowController.java
│
├── search
│   ├── SearchBar.java
│   ├── SearchResultList.java
│   ├── SearchResultCell.java
│   └── SearchResultViewModel.java
│
├── filter
│   └── FilterBar.java
│
├── status
│   └── StatusBar.java
│
├── settings
│   ├── SettingsView.java
│   ├── IndexedLocationsView.java
│   ├── ExclusionsView.java
│   └── IndexSettingsView.java
│
├── dialog
│   ├── RemoveRootDialog.java
│   └── RebuildIndexDialog.java
│
├── style
│   └── desktop-search.css
│
└── util
    ├── FileSizeFormatter.java
    └── DateTimeFormatter.java
```

---

# 101. View → Service Dependencies

Allowed:

```text
MainViewModel
   ↓
SearchService

IndexedLocationsViewModel
   ↓
IndexedLocationService

IndexStatusViewModel
   ↓
IndexCoordinator / IndexStatusService
```

Forbidden:

```text
SearchResultCell → Lucene
SettingsView → SQLite JDBC
MainView → WatchService
```

---

# 102. Threading Rules

Hard rule:

**Never block JavaFX Application Thread with IO or Lucene operations.**

Background:

```text
search
filesystem scan
indexing
content extraction
SQLite heavy operations
rebuild
```

UI thread:

```text
control events
state updates
rendering
```

---

# 103. Search Thread Flow

```text
JavaFX Thread
     │
     │ query changed
     ▼
Debouncer
     │
     ▼
Search Executor
     │
     ▼
SearchService
     │
     ▼
SearchResponse
     │
     ▼
Platform.runLater
     │
     ▼
Update ViewModel/UI
```

---

# 104. Index Progress Flow

```text
Indexing Engine
      │
      ▼
Progress Event
      │
      ▼
UI Progress Adapter
      │
 throttle 250–500 ms
      ▼
JavaFX Thread
      │
      ▼
StatusBar
```

Never emit UI update for every file.

---

# 105. Progress Update Frequency

Recommended:

```text
250–500 ms
```

Fast enough to feel live.

Slow enough not to overload JavaFX event queue.

---

# 106. Search Performance UX Targets

User perceived goals:

```text
typing remains smooth
result update feels instant
scroll stays smooth
indexing doesn't freeze UI
```

Initial technical goals:

```text
Debounce                 200 ms
Search p95               <200–500 ms depending index
Progress UI update       250–500 ms
Initial result batch     50
Maximum UI results       500
```

---

# 107. Result Virtualization

Use JavaFX virtualized list.

Do not create all cells upfront.

Even with 500 visible-loaded results:

```text
only visible cells need active Node structures
```

---

# 108. Image/Thumbnail Policy

V1 does not render file thumbnails.

Why:

- filesystem IO;
- memory;
- image decoding;
- latency;
- caching complexity.

Use lightweight type icons.

Thumbnail preview can be future feature.

---

# 109. Preview Pane

Not V1.

Future could show:

```text
file metadata
text snippet
image preview
```

but this reduces minimal search UI simplicity.

Do not reserve large permanent pane in V1.

---

# 110. Drag and Drop

Not core V1 requirement.

Potential:

drag search result into another application.

JavaFX may support it future.

---

# 111. File Properties

Context menu:

```text
Properties
```

Could use Windows native properties future.

MVP can omit if platform integration difficult.

Do not build custom full properties window unnecessarily.

---

# 112. Notifications

Use inline status for common events:

```text
Path copied
Indexing started
Index rebuilt
```

Avoid modal dialogs.

Modal dialogs reserved for:

```text
Remove indexed root
Rebuild index
destructive-ish configuration actions
fatal errors requiring choice
```

---

# 113. Toasts

JavaFX has no native toast requirement.

A small transient status label is enough.

Do not introduce third-party notification library solely for toasts.

---

# 114. Confirmation Dialog Principles

Use confirmation only when action:

- changes persistent configuration significantly;
- causes large expensive operation;
- could surprise user.

Examples:

```text
Remove indexed location
Rebuild index
```

Do not confirm:

```text
Open file
Copy path
Change sort
```

---

# 115. Destructive Language

Because index is derived data:

Say:

```text
Remove from search index
```

not:

```text
Delete files
```

when actual filesystem files remain untouched.

UX language must clearly distinguish:

```text
user file
```

from:

```text
search index entry
```

---

# 116. Indexed Root Unavailable

Example external drive removed.

Settings:

```text
E:\Documents
Unavailable
```

Search may still contain stale entries temporarily.

If user opens result:

```text
File is currently unavailable.
```

Do not remove configured root automatically.

---

# 117. Stale Result Open

If file missing:

```text
This file no longer exists at the indexed location.
```

Then:

```text
schedule index cleanup
```

UI result may disappear after refresh.

No technical exception shown.

---

# 118. Rename Behavior

Watcher sees delete/create.

UI search results update naturally after NRT refresh.

No special rename animation needed.

---

# 119. Result Refresh While User Navigates

Potential issue:

Watcher/index refresh changes result list while keyboard selecting.

Recommendation:

Only search results refresh automatically when:

```text
current query re-executed
```

Do not continuously rerun active query on every file watcher event in V1.

New file appears on next user query/filter change.

Future can auto-refresh carefully.

This avoids selection jumping.

---

# 120. Search Query Syntax UX

Advanced users can type:

```text
ext:pdf transaction
```

Normal users need not know syntax.

Future help:

```text
?
```

or Settings/Search Syntax.

Not necessary to show syntax cheat sheet permanently.

---

# 121. Query Syntax Help

Potential popup:

```text
Search tips

ext:pdf report
path:projects spring
size:>10MB
modified:today
```

Accessible through small help icon or documentation.

V1.1 candidate.

---

# 122. Search Invalid Filter UX

Input:

```text
size:banana report
```

Search should not throw modal error.

Possible status:

```text
Invalid size filter ignored.
```

Results still based on remaining query if parser supports fallback.

Search box should remain forgiving.

---

# 123. Index Setup Onboarding

First launch can use one-screen onboarding:

```text
Welcome to Desktop Search

Choose which folders Desktop Search should index.

Your files stay on this computer.

[Choose folders]
```

No multi-page onboarding wizard.

---

# 124. Privacy Messaging

Since content/index local:

Settings/About can state:

```text
Search index is stored locally on this computer.
```

If content indexing enable:

```text
Document text is processed locally.
```

No cloud claims beyond actual implementation.

---

# 125. Application Name & Title

Window title:

```text
Desktop Search
```

Future product name can replace.

Keep technical codename out of UI if renamed.

---

# 126. App Icon

V1 needs simple recognizable icon.

Concept:

```text
magnifying glass + file/folder
```

Not important to engine implementation.

Needed before Windows packaging/release.

---

# 127. Main Screen State — No Configuration

```text
┌──────────────────────────────────────────────────────────────┐
│ Desktop Search                                      ⚙       │
├──────────────────────────────────────────────────────────────┤
│ 🔍 Search files...                                          │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│                   No folders indexed                         │
│                                                              │
│          Choose folders to start searching.                  │
│                                                              │
│                    [ Add folder ]                            │
│                                                              │
├──────────────────────────────────────────────────────────────┤
│ Not configured                                               │
└──────────────────────────────────────────────────────────────┘
```

---

# 128. Main Screen State — Indexing

```text
┌──────────────────────────────────────────────────────────────┐
│ Desktop Search                                      ⚙       │
├──────────────────────────────────────────────────────────────┤
│ 🔍 transaction                                              │
├──────────────────────────────────────────────────────────────┤
│ Type: All ▼      Modified: Any ▼      Sort: Relevance ▼     │
├──────────────────────────────────────────────────────────────┤
│ TransactionService.java                                     │
│ D:\Projects\app\service                                     │
│ JAVA • 14 KB • Today                                        │
│                                                              │
│ transaction-notes.pdf                                       │
│ D:\Documents                                                │
│ PDF • 1.2 MB • Yesterday                                    │
├──────────────────────────────────────────────────────────────┤
│ 2 results • 17 ms        Indexing... 124,521 files          │
└──────────────────────────────────────────────────────────────┘
```

---

# 129. Main Screen State — Ready

```text
┌──────────────────────────────────────────────────────────────┐
│ Desktop Search                                      ⚙       │
├──────────────────────────────────────────────────────────────┤
│ 🔍 spring transaction                                      │
├──────────────────────────────────────────────────────────────┤
│ Type: All ▼      Modified: Any ▼      Sort: Relevance ▼     │
├──────────────────────────────────────────────────────────────┤
│ TransactionService.java                                     │
│ D:\Projects\app\src                                         │
│ JAVA • 14 KB • Today                                        │
│                                                              │
│ Spring Transaction.pdf                                      │
│ D:\Documents\Java                                           │
│ PDF • 2.5 MB • 8 Aug                                        │
├──────────────────────────────────────────────────────────────┤
│ 73 results • 18 ms                       128,542 indexed     │
└──────────────────────────────────────────────────────────────┘
```

---

# 130. Main Screen State — No Results

```text
┌──────────────────────────────────────────────────────────────┐
│ 🔍 abcdefghxyz                                              │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│                      No results                              │
│                                                              │
│             No files match “abcdefghxyz”.                    │
│                                                              │
│            Try a shorter search term.                        │
│                                                              │
├──────────────────────────────────────────────────────────────┤
│ 0 results                                 128,542 indexed    │
└──────────────────────────────────────────────────────────────┘
```

---

# 131. Main Screen State — Search Error

```text
Search is temporarily unavailable.

[Retry]
```

Index status can remain visible separately.

---

# 132. Settings Wireframe

```text
┌──────────────────────────────────────────────────────────────┐
│ Settings                                                     │
├───────────────┬──────────────────────────────────────────────┤
│ Locations     │ Indexed Locations                            │
│ Exclusions    │                                              │
│ Search        │ D:\Projects                        [Remove]   │
│ Index         │ D:\Documents                       [Remove]   │
│ Appearance    │                                              │
│ About         │ [+ Add folder]                               │
│               │                                              │
└───────────────┴──────────────────────────────────────────────┘
```

---

# 133. Settings Navigation

MVP can simplify sections to:

```text
Locations
Search
Index
```

Exclusions can live inside Locations.

Appearance/About added later.

Do not implement empty settings categories.

---

# 134. Dialog Style

Dialogs should use:

```text
title
short explanation
primary action
cancel
```

Avoid paragraphs of legal-like text.

---

# 135. Search Result Formatting

File size formatter:

```text
1024        → 1 KB
1048576     → 1 MB
```

User-friendly display.

Core stores bytes.

---

# 136. Date Formatting

Recent files:

```text
Today 13:42
Yesterday 18:15
```

Older:

```text
8 Aug 2026
```

Exact time can be tooltip.

Do not clutter row with milliseconds/timezones.

---

# 137. Path Formatting

Display original Windows path:

```text
D:\Projects\DesktopSearch
```

not normalized lowercase identity:

```text
d:\projects\desktopsearch
```

Index normalized path and UI display path must remain separate.

---

# 138. Long Paths

If parent path too long:

```text
D:\Projects\...\service
```

using middle ellipsis is preferable because root and final directory are useful.

Full path available via:

```text
tooltip
copy path
```

---

# 139. Localization

V1 can use one UI language.

Architecture should centralize strings if localization is expected future.

Do not hard-code important labels across many classes.

Possible:

```text
ResourceBundle
```

future.

Not mandatory initial MVP.

---

# 140. Event Handling

Main UI events:

```text
QueryChanged
ResultSelected
ResultOpened
OpenContainingFolder
CopyPath
FilterChanged
SortChanged
AddRoot
RemoveRoot
RebuildIndex
```

ViewModel/controller maps these to application services.

---

# 141. Event-to-Service Mapping

```text
QueryChanged
   ↓
SearchService.search()

OpenResult
   ↓
FileLauncher.open()

OpenContainingFolder
   ↓
WindowsExplorerService.reveal()

AddRoot
   ↓
IndexedLocationService.addRoot()
   ↓
IndexCoordinator.indexRoot()

RemoveRoot
   ↓
IndexedLocationService.removeRoot()

Rebuild
   ↓
IndexMaintenanceService.rebuild()
```

---

# 142. Search UI Boundary

UI creates:

```text
SearchRequest
```

and consumes:

```text
SearchResponse
```

It must not know:

```text
Query
TopDocs
ScoreDoc
Document
SearcherManager
```

---

# 143. Index UI Boundary

UI consumes presentation-safe index status:

```text
IndexStatus
IndexProgress
```

It must not know:

```text
BlockingQueue
IndexWriter
worker threads
```

---

# 144. Persistence UI Boundary

Settings uses repository/application service.

UI must not directly execute:

```sql
SELECT *
```

against SQLite.

---

# 145. UI Error Boundary

Core exception:

```text
SearchException
IndexingException
```

mapped into user messages centrally.

Do not display:

```text
java.nio.file.AccessDeniedException...
```

directly.

---

# 146. Accessibility Keyboard Flow

Typical flow:

```text
App opens
    ↓
Search focus
    ↓
type query
    ↓
Down
    ↓
result selected
    ↓
Enter
```

No mouse necessary.

---

# 147. Focus Return

After closing settings/dialog:

```text
focus returns to previous logical component
```

Usually search box/main window.

After opening external file, app retains current search.

---

# 148. Search Persistence During Settings

If user opens Settings and closes:

```text
current query/results preserved
```

No need rerun unless configuration changed.

If indexed roots change, results can refresh.

---

# 149. Root Add While Search Active

User adds root.

Indexing begins.

Current results remain.

New root content becomes available after refresh/new query.

No full UI reset.

---

# 150. Root Remove While Search Active

After remove completes:

active query should be re-run.

Results from removed root disappear.

---

# 151. Rebuild While Search Active

Display status:

```text
Rebuilding index — results may be incomplete.
```

Query can be rerun as index grows depending engine behavior.

Do not clear search text.

---

# 152. UI Logging

UI logs:

```text
window lifecycle
unexpected controller errors
service failures
```

Do not log every key typed or raw search query at INFO.

---

# 153. CSS Structure

Possible:

```text
desktop-search.css

.root
.search-bar
.filter-bar
.search-result-cell
.search-result-cell:selected
.result-name
.result-path
.result-metadata
.status-bar
.empty-state
.error-state
.settings
```

Avoid inline style strings:

```java
node.setStyle("-fx-background-color: ...");
```

except temporary debugging.

---

# 154. Design Tokens

CSS variables are limited compared to web CSS, so organize constants/document them.

Suggested token concepts:

```text
color-bg
color-surface
color-text-primary
color-text-secondary
color-border
color-accent
spacing-xs
spacing-sm
spacing-md
```

Actual implementation may use repeated CSS values or preprocessing later.

---

# 155. Animation

V1 uses minimal animation.

Possible:

- subtle progress indicator;
- small fade for status.

Avoid animated result list or heavy transitions.

Search utility should feel immediate.

---

# 156. Responsive Layout

Window resizing behavior:

- search bar stretches;
- result area consumes available space;
- path truncates;
- metadata remains readable;
- filter controls may compact.

Below minimum width, window prevents further shrink.

No mobile responsive layout required.

---

# 157. Multiple Monitors

Normal JavaFX window behavior.

Persisted position must be validated on startup so window does not reopen off-screen after monitor removal.

Future implementation detail.

---

# 158. High DPI

JavaFX should handle DPI scaling.

Avoid assumptions based on physical pixels.

Test Windows scaling:

```text
100%
125%
150%
```

before release.

---

# 159. Empty State Design Principle

Empty state should guide action.

Bad:

```text
No data
```

Better:

```text
No folders indexed yet.
Add a folder to start searching.
```

---

# 160. Error State Design Principle

Tell user:

```text
what failed
what they can do
```

not technical cause unless actionable.

Example:

```text
Search index could not be opened.
Rebuild the index to continue.
```

---

# 161. Performance State Design Principle

Background work should be visible but non-blocking.

```text
Indexing...
```

not:

```text
Please wait until indexing completes.
```

Search remains usable.

---

# 162. Privacy UX Principle

If content indexing is added, make local behavior explicit.

No network/cloud indicator required unless future cloud features exist.

---

# 163. MVP UI Scope

MVP must include:

```text
Main window
Search bar
Search-as-you-type
Result list
File/folder icons
Keyboard selection
Enter open
Double click open
Context menu
Open containing folder
Copy path
Basic sort
Extension/type filter
Index status
Initial no-root state
Add indexed folder
Remove indexed folder
Basic settings
Rebuild index action
No result state
Search error state
Index error state
```

---

# 164. Not Required for MVP

Exclude:

```text
System tray
Global hotkey
File preview
Thumbnails
Drag-and-drop
Animations
Cloud sync
Recent-file dashboard
Pinned files
Complex search builder
Custom date range picker
Native Windows file icons
Multiple themes
Localization
```

---

# 165. UI V1.1 Candidates

After MVP:

```text
Dark theme
Query syntax help
Search highlighting
Search history
Recent files
More filters
Detailed indexing popup
Native file icons
Content snippets
```

---

# 166. UI V2 Candidates

Potential:

```text
Global hotkey
Quick-search popup
System tray background service
Preview pane
Semantic search mode
Hybrid result explanation
Pinned files
Plugin results
```

---

# 167. UI Testing Strategy

Unit test:

```text
formatters
ViewModel logic
filter mapping
state transitions
```

Integration/UI tests:

```text
search → results
empty state
filter change
settings root add/remove
```

Full JavaFX automation can be introduced only where useful.

Don't spend excessive effort screenshot-testing every pixel.

---

# 168. ViewModel Test Example

Given:

```text
query changed rapidly:
spring
spring boot
```

Response spring returns later.

Expected:

```text
ViewModel displays only spring boot response.
```

This is important UI concurrency test.

---

# 169. Index Progress Test

Engine sends thousands of progress events.

UI adapter must throttle and not enqueue thousands of JavaFX updates.

---

# 170. UI Performance Tests

Manual/profiling checks:

```text
rapid typing
fast scrolling
indexing while searching
resize window
500 result rows
frequent progress events
```

Look for:

```text
JavaFX thread stalls
excess Node creation
memory leak
listener leak
```

---

# 171. Definition of Done — UI/UX V1

UI/UX implementation is complete when:

1. Application opens to search-focused main window.
2. Search input is immediately usable.
3. Search runs asynchronously.
4. Typing does not freeze UI.
5. Debounce is implemented.
6. Stale result race is handled.
7. Results display filename/path/metadata.
8. First result auto-selects.
9. Keyboard Up/Down navigation works.
10. Enter opens selected item.
11. Double click opens result.
12. Open containing folder works.
13. Copy path works.
14. Basic filters work.
15. Sort works.
16. Result count/search latency displayed.
17. Indexing state displayed.
18. Search works while indexing.
19. First-launch no-root state works.
20. Folder can be added through UI.
21. Indexed location can be removed.
22. Settings screen works.
23. Rebuild index flow works.
24. Confirmation wording clearly says user files are safe.
25. No-result state works.
26. Search error state works.
27. Index error state works.
28. UI does not directly access Lucene.
29. UI does not directly perform filesystem scan.
30. Core operations never block JavaFX Application Thread.

---

# 172. Recommended Implementation Order

## Step 1 — Static Main Window

Build:

```text
MainView
SearchBar
FilterBar
ResultList
StatusBar
```

with dummy data.

Goal:

validate layout only.

---

## Step 2 — Search ViewModel

Implement:

```text
query state
debounce
search executor
sequence ID
SearchService integration
```

---

## Step 3 — Result Interaction

Implement:

```text
selection
Enter
double-click
context menu
copy path
open folder
```

---

## Step 4 — Filters and Sort

Connect UI controls to `SearchRequest`.

---

## Step 5 — Index Status

Connect:

```text
IndexProgress
```

to StatusBar using throttled updates.

---

## Step 6 — Indexed Locations Settings

Implement:

```text
list roots
add root
remove root
```

---

## Step 7 — Index Maintenance UX

Implement:

```text
rebuild
error states
```

---

## Step 8 — Styling

Apply:

```text
desktop-search.css
```

after interaction works.

Do not start project by spending days on CSS.

---

# 173. Primary UX Principle

Desktop Search should optimize:

```text
time from intent
to opening the desired file.
```

Every UI element should be evaluated against that goal.

If a component does not make search, selection, configuration, or problem recovery easier, it probably does not belong on the main screen.

---

# 174. Final UI Architecture

```text
                         USER
                          │
                          ▼
                  ┌───────────────┐
                  │  JavaFX View  │
                  └───────┬───────┘
                          │
                          ▼
                 ┌────────────────┐
                 │   ViewModel    │
                 └───────┬────────┘
                         │
          ┌──────────────┼───────────────┐
          ▼              ▼               ▼
    SearchService   Index Service   Platform Service
          │              │               │
          ▼              ▼               ▼
       Lucene       Indexing Engine    Windows
```

JavaFX remains presentation layer.

It does not become application core.

---

# 175. Final UX Flow

```text
Launch
  ↓
Search focused
  ↓
Type query
  ↓
200ms debounce
  ↓
Background search
  ↓
Results
  ↓
↑ / ↓
  ↓
Enter
  ↓
File opens
```

Configuration flow:

```text
Settings
  ↓
Add indexed location
  ↓
Background indexing
  ↓
Status updates
  ↓
Search available during indexing
```

Recovery flow:

```text
Index error
   ↓
Clear explanation
   ↓
Rebuild Index
   ↓
Background recovery
```

---

# 176. Conclusion

Desktop Search V1 uses a deliberately minimal search-oriented UI.

Main screen contains only the information required to:

```text
Search
Filter
Select
Open
Understand indexing status
```

Advanced configuration is placed in Settings.

The UI is designed as a thin presentation layer over the application APIs:

```text
JavaFX
    ↓
ViewModel
    ↓
Application Services
    ↓
Indexing/Search Core
```

The most important UX requirements are:

```text
search box always ready
keyboard-first navigation
fast asynchronous results
no JavaFX blocking
clear index state
non-blocking indexing
predictable file actions
clear distinction between index data and user files
```

Desktop Search should feel like a utility rather than a complex file-management application.