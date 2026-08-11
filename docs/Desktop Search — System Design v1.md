# Desktop Search — System Design v1

**Version:** 1.0  
**Status:** Draft  
**Target platform:** Windows  
**Primary language:** Java  
**Architecture:** Modular Monolith Desktop Application

---

# 1. Introduction

## 1.1. Problem Statement

Khi số lượng file trên máy tính tăng lên hàng chục nghìn hoặc hàng trăm nghìn file, việc tìm kiếm bằng cách duyệt thủ công qua thư mục trở nên chậm và khó quản lý.

Windows Search hỗ trợ tìm kiếm file nhưng trong nhiều trường hợp:

- tốc độ tìm kiếm chưa ổn định;
- khả năng filter hạn chế;
- khó kiểm soát phạm vi indexing;
- full-text search không đồng nhất giữa các loại file;
- không hỗ trợ tốt các query nâng cao;
- người dùng không kiểm soát được cơ chế ranking;
- khó mở rộng thêm semantic search hoặc các cơ chế indexing tùy chỉnh.

Desktop Search được xây dựng nhằm cung cấp một công cụ tìm kiếm file local có tốc độ cao, có thể mở rộng và cho phép người dùng kiểm soát cách dữ liệu được index.

---

# 2. Project Goals

Desktop Search hướng tới các mục tiêu chính:

1. Search file theo tên với tốc độ rất nhanh.
2. Search không cần scan filesystem tại thời điểm truy vấn.
3. Theo dõi thay đổi filesystem và cập nhật index tự động.
4. Search nội dung bên trong các loại tài liệu phổ biến.
5. Hỗ trợ filter và query nâng cao.
6. Hoạt động hoàn toàn local.
7. Có khả năng xử lý hàng trăm nghìn đến hàng triệu file.
8. Không yêu cầu chạy backend server riêng.
9. Architecture đủ sạch để mở rộng trong tương lai.

Mục tiêu dài hạn có thể bao gồm:

- semantic search;
- hybrid keyword/vector search;
- duplicate file detection;
- file preview;
- plugin system;
- ranking theo hành vi người dùng;
- Windows Explorer integration.

---

# 3. Non-Goals — Version 1

Version đầu tiên **không** tập trung vào:

- cloud search;
- multi-user;
- sync dữ liệu giữa nhiều máy;
- REST API;
- microservices;
- mobile client;
- distributed search;
- AI chatbot;
- OCR;
- network drive indexing;
- semantic/vector search.

Những chức năng này có thể được xem xét sau khi search engine cơ bản đã ổn định.

---

# 4. Functional Requirements

## FR-01 — Configure Search Location

Người dùng có thể chọn các thư mục được phép index.

Ví dụ:

```text
C:\Users\Thuan\Documents
D:\Projects
D:\Books
```

Người dùng cũng có thể loại trừ các thư mục.

Ví dụ:

```text
node_modules
.git
target
build
AppData
```

---

## FR-02 — Initial File Scan

Hệ thống phải có khả năng recursive scan toàn bộ file trong các thư mục được cấu hình.

Thông tin cơ bản cần thu thập:

```text
file name
absolute path
extension
file size
created time
modified time
file type
```

---

## FR-03 — File Indexing

Metadata của file phải được lưu vào search index.

Search engine không được duyệt filesystem mỗi khi người dùng search.

---

## FR-04 — Search by Filename

Người dùng có thể search:

```text
transaction
```

và nhận được:

```text
transaction-note.pdf
spring-transaction.md
TransactionService.java
database_transaction.docx
```

---

## FR-05 — Partial Search

Có thể tìm bằng một phần filename.

Ví dụ:

```text
trans
```

có thể match:

```text
transaction.pdf
transaction-service.java
```

---

## FR-06 — Search Filters

Hỗ trợ filter:

```text
extension
file size
modified time
directory
file type
```

Ví dụ:

```text
ext:pdf transaction
```

hoặc:

```text
ext:java service
```

---

## FR-07 — Sorting

Search result có thể sort theo:

```text
relevance
name
size
modified time
created time
```

---

## FR-08 — Open File

Double click search result sẽ mở file bằng application mặc định của Windows.

---

## FR-09 — Open Containing Folder

Người dùng có thể mở thư mục chứa file và chọn file tương ứng.

---

## FR-10 — Copy File Path

Cho phép copy:

```text
D:\Projects\desktop-search\README.md
```

vào clipboard.

---

## FR-11 — Incremental Indexing

Sau initial scan, hệ thống phải theo dõi filesystem.

Các event quan trọng:

```text
CREATE
MODIFY
DELETE
RENAME
```

phải được phản ánh vào index.

---

## FR-12 — Full-text Search

Ở phase sau, hệ thống có thể extract text từ:

```text
TXT
MD
PDF
DOCX
PPTX
XLSX
HTML
source code
```

và đưa text vào Lucene.

---

# 5. Non-Functional Requirements

## NFR-01 — Search Performance

Target ban đầu:

```text
100,000 files  -> < 100 ms
500,000 files  -> < 200 ms
1,000,000 files -> < 500 ms
```

Đây là target kỹ thuật ban đầu chứ chưa phải SLA.

Performance thực tế phải được benchmark.

---

## NFR-02 — UI Responsiveness

Không được chạy:

```text
filesystem scanning
content extraction
index writing
```

trên JavaFX UI Thread.

UI phải responsive trong lúc indexing.

---

## NFR-03 — Memory

Không được load toàn bộ danh sách file vào RAM.

Sai:

```java
List<Path> files = scanEntireDisk();
```

Đúng hơn:

```text
Scanner
   ↓
bounded queue
   ↓
Indexer
```

File được xử lý theo streaming pipeline.

---

## NFR-04 — Fault Tolerance

Một file lỗi không được làm toàn bộ indexing job fail.

Ví dụ:

```text
PermissionDeniedException
Corrupted PDF
File deleted during scan
File locked
Invalid path
```

phải được xử lý độc lập.

---

## NFR-05 — Index Persistence

Search index phải tồn tại sau khi application đóng.

Khi mở application lần tiếp theo không cần full re-index nếu filesystem không thay đổi đáng kể.

---

# 6. High-Level Architecture

Desktop Search sử dụng kiến trúc:

**Modular Monolith**

Toàn bộ application chạy trong một JVM process.

```text
┌──────────────────────────────────────────────┐
│              Desktop Search App              │
│                                              │
│  ┌────────────────────────────────────────┐  │
│  │              JavaFX UI                 │  │
│  └──────────────────┬─────────────────────┘  │
│                     │                        │
│                     ▼                        │
│  ┌────────────────────────────────────────┐  │
│  │          Application Services          │  │
│  └─────────────┬──────────────────────────┘  │
│                │                             │
│      ┌─────────┼──────────┐                  │
│      ▼         ▼          ▼                  │
│   Search     Index      Settings             │
│   Module     Module      Module               │
│      │         ▲                             │
│      │         │                             │
│      │    ┌────┴───────┐                     │
│      │    │            │                     │
│      ▼    ▼            ▼                     │
│   Lucene Scanner    File Watcher              │
│          │                                  │
│          ▼                                  │
│      File System                             │
└──────────────────────────────────────────────┘
```

---

# 7. Why No REST API

Desktop Search là local desktop application.

Architecture dạng:

```text
JavaFX
   ↓ HTTP
Spring Boot
   ↓
Lucene
```

sẽ tạo thêm:

- HTTP serialization;
- DTO mapping;
- server lifecycle;
- port management;
- network error handling;
- duplicate application layer;
- deployment complexity.

Trong khi toàn bộ component chạy trên cùng máy.

Do đó Version 1 sử dụng:

```text
JavaFX
   ↓
Java Service
   ↓
Lucene
```

Giao tiếp thông qua method call và interface.

REST API chỉ đáng cân nhắc nếu sau này có:

```text
mobile client
browser extension
remote search
multiple applications using search engine
separate indexing daemon
```

---

# 8. Module Design

Application dự kiến chia thành các module logic sau.

```text
desktop-search
│
├── application
├── ui
├── search
├── indexing
├── scanner
├── watcher
├── extractor
├── persistence
├── platform
└── common
```

---

# 9. Application Module

Application module chịu trách nhiệm bootstrap hệ thống.

Ví dụ:

```text
Application startup

    ↓

Load configuration

    ↓

Initialize Lucene

    ↓

Initialize repositories

    ↓

Start indexing service

    ↓

Start filesystem watcher

    ↓

Start JavaFX UI
```

Application module không chứa business logic trực tiếp.

---

# 10. Scanner Module

Scanner chịu trách nhiệm duyệt filesystem.

Interface dự kiến:

```java
public interface FileScanner {

    void scan(
        Path root,
        FileScanHandler handler
    );

}
```

Scanner sử dụng:

```java
Files.walkFileTree(...)
```

thay vì load toàn bộ path vào collection.

Pipeline:

```text
Directory
   │
   ▼
FileVisitor
   │
   ├── file
   ├── file
   ├── directory
   ├── file
   ▼
FileMetadata
```

Scanner không chịu trách nhiệm ghi Lucene.

Scanner chỉ discover file.

---

# 11. File Metadata Model

Model trung tâm:

```java
FileMetadata
```

Có thể chứa:

```text
id
name
absolutePath
parentPath
extension
size
createdAt
modifiedAt
isDirectory
mimeType
```

Ví dụ:

```text
name        = TransactionService.java
path        = D:\Project\src\TransactionService.java
parent      = D:\Project\src
extension   = java
size        = 10452
modifiedAt  = 2026-08-10T10:15
```

`FileMetadata` nên là immutable object.

Ví dụ Java record:

```java
public record FileMetadata(
    String name,
    Path path,
    String extension,
    long size,
    Instant createdAt,
    Instant modifiedAt
) {}
```

---

# 12. Indexing Module

Indexing module chịu trách nhiệm chuyển:

```text
FileMetadata
```

thành:

```text
Lucene Document
```

Luồng:

```text
FileMetadata
      │
      ▼
DocumentMapper
      │
      ▼
Lucene Document
      │
      ▼
IndexWriter
```

Index module cung cấp abstraction:

```java
public interface IndexService {

    void index(FileMetadata file);

    void update(FileMetadata file);

    void delete(Path path);

}
```

UI và Scanner không được trực tiếp thao tác Lucene `IndexWriter`.

---

# 13. Lucene Document Schema

Một file tương ứng với một Lucene Document.

Schema ban đầu:

```text
id
name
name_exact
path
path_exact
parent_path
extension
size
created_at
modified_at
content
```

Ý nghĩa:

### `name`

Analyzed field để search filename.

### `name_exact`

Không analyze.

Phục vụ:

```text
sorting
exact match
```

### `path`

Có thể analyzed để search theo directory/path.

### `path_exact`

Lưu absolute path nguyên bản.

Dùng như identity của file.

### `extension`

Keyword field.

Ví dụ:

```text
pdf
java
docx
```

### `size`

Numeric field.

Phục vụ range query.

### `modified_at`

Numeric/date field.

### `content`

Full-text content.

Field này chưa bắt buộc trong MVP.

---

# 14. File Identity

Version đầu có thể dùng:

```text
normalized absolute path
```

làm unique key.

Ví dụ:

```text
D:\Projects\App.java
```

Nhược điểm:

rename file sẽ thay đổi identity.

Watcher có thể xử lý rename như:

```text
DELETE oldPath
CREATE newPath
```

Cách này đủ đơn giản cho V1.

---

# 15. Search Module

Search module nhận query từ UI.

```text
UI
 │
 ▼
SearchService
 │
 ▼
QueryParser
 │
 ▼
SearchQuery
 │
 ▼
LuceneQueryBuilder
 │
 ▼
Lucene IndexSearcher
 │
 ▼
SearchResult
```

Interface:

```java
public interface SearchService {

    SearchResponse search(SearchRequest request);

}
```

---

# 16. SearchRequest

Search request không nên truyền raw UI state trực tiếp xuống Lucene.

Ví dụ:

```java
public record SearchRequest(
    String query,
    int limit,
    int offset,
    SearchSort sort
) {}
```

Sau này QueryParser convert query string thành model.

---

# 17. SearchQuery Model

Ví dụ:

```java
public record SearchQuery(
    String keyword,
    String extension,
    Long minSize,
    Long maxSize,
    Instant modifiedFrom,
    Instant modifiedTo,
    String path
) {}
```

Input:

```text
ext:java transaction
```

được parse thành:

```text
keyword = transaction
extension = java
```

---

# 18. Query Language

Version nâng cao hỗ trợ:

```text
transaction
ext:pdf transaction
ext:java service
path:project spring
size:>100MB
modified:today
```

Có thể mở rộng:

```text
size:<10MB
modified:>2026-01-01
name:"spring transaction"
```

QueryParser không phụ thuộc JavaFX.

Do đó có thể unit test độc lập.

---

# 19. Search Ranking

Lucene relevance score là ranking cơ bản.

Sau này có thể custom boost.

Ví dụ:

```text
exact filename       x10
filename contains    x5
path match           x2
content match        x1
```

Ví dụ query:

```text
transaction
```

Kết quả:

```text
transaction.pdf                    score 15
transaction-service.java           score 12
spring-note.pdf                    score 5
```

Trong đó `spring-note.pdf` chỉ match content.

---

# 20. Search Result Model

UI không nên nhận Lucene `Document`.

Thay vào đó:

```java
public record SearchResult(
    String name,
    Path path,
    String extension,
    long size,
    Instant modifiedAt,
    float score
) {}
```

Điều này ngăn UI phụ thuộc trực tiếp vào Lucene.

---

# 21. Concurrency Architecture

Đây là một trong những phần quan trọng nhất của project.

Indexing không nên làm theo:

```text
Scan file
   ↓
index
   ↓
scan next file
```

vì extraction/indexing có thể làm scanner bị block.

Thay vào đó dùng producer-consumer.

```text
                ┌──────────────┐
                │ File Scanner │
                └──────┬───────┘
                       │
                       ▼
                ┌──────────────┐
                │ Bounded Queue│
                └──────┬───────┘
                       │
         ┌─────────────┼─────────────┐
         ▼             ▼             ▼
     Worker 1       Worker 2      Worker N
         │             │             │
         └─────────────┼─────────────┘
                       ▼
                  IndexService
                       │
                       ▼
                   Lucene
```

---

# 22. Why Bounded Queue

Không được dùng queue unlimited.

Ví dụ scanner có thể tìm:

```text
1,000,000 files
```

trong khi indexer chỉ xử lý:

```text
5,000 files/sec
```

Nếu producer nhanh hơn consumer, memory sẽ tăng liên tục.

Do đó:

```java
BlockingQueue<FileMetadata>
```

có capacity giới hạn.

Ví dụ:

```text
capacity = 5,000
```

Khi queue đầy:

```text
scanner blocks
```

đây chính là cơ chế backpressure.

---

# 23. Thread Pools

Dự kiến có các executor riêng.

```text
scanExecutor
indexExecutor
contentExtractionExecutor
watcherExecutor
```

Không nên dùng:

```java
Executors.newCachedThreadPool()
```

bừa bãi.

Thread count cần benchmark.

Ví dụ khởi đầu:

```text
scanner       1 thread
indexer       2–4 threads
extractor     CPU dependent
watcher       1 thread
```

Lucene IndexWriter vốn hỗ trợ concurrent document addition, nhưng application vẫn cần kiểm soát workload.

---

# 24. UI Thread

JavaFX có:

```text
JavaFX Application Thread
```

Thread này chỉ làm:

```text
render UI
handle input
update view state
```

Không chạy:

```text
Files.walkFileTree
PDF extraction
Lucene indexing
filesystem IO
```

Background service cập nhật progress thông qua event.

Ví dụ:

```text
IndexProgressEvent

scanned = 120,000
indexed = 118,500
failed = 32
```

UI chỉ subscribe event.

---

# 25. Filesystem Watcher

Sau initial indexing:

```text
File Watcher
```

theo dõi filesystem.

Java hỗ trợ:

```java
WatchService
```

Event cơ bản:

```text
ENTRY_CREATE
ENTRY_MODIFY
ENTRY_DELETE
```

Luồng:

```text
Filesystem Event
      │
      ▼
FileChangeEvent
      │
      ▼
IndexUpdateQueue
      │
      ▼
IndexService
```

---

# 26. WatchService Limitation

WatchService thường hoạt động ở mức directory.

Nếu cần recursive monitoring, application phải register các directory cần theo dõi.

Khi directory mới được tạo:

```text
CREATE directory
        ↓
register directory
        ↓
scan directory
```

Đây là một phần cần test kỹ trên Windows.

---

# 27. Event Debouncing

Một thao tác save file có thể sinh nhiều filesystem event.

Ví dụ:

```text
MODIFY
MODIFY
MODIFY
```

trong vài milliseconds.

Nếu index mỗi event:

```text
file parsed 3 lần
index updated 3 lần
```

gây lãng phí.

Do đó cần debounce.

Ví dụ:

```text
MODIFY A
MODIFY A
MODIFY A
     │
     ▼
wait 300 ms
     │
     ▼
one IndexUpdate
```

---

# 28. Full-text Extraction

Phase sau thêm `ContentExtractor`.

Interface:

```java
public interface ContentExtractor {

    ExtractionResult extract(Path file);

}
```

Implementation ban đầu:

```text
TikaContentExtractor
```

Pipeline:

```text
File
 │
 ▼
Apache Tika
 │
 ▼
Text
 │
 ▼
Text Normalizer
 │
 ▼
Lucene Document.content
```

---

# 29. Extraction Limits

Không nên extract mọi file vô điều kiện.

Ví dụ:

```text
10 GB log file
4 GB ISO
video file
binary executable
```

Có thể gây performance problem.

Config:

```text
maxExtractFileSize = 50 MB
```

Hoặc theo file type.

Ví dụ:

```text
pdf      enabled
docx     enabled
txt      enabled
java     enabled
exe      disabled
zip      disabled initially
video    disabled
```

---

# 30. Persistence

Lucene chịu trách nhiệm lưu search index.

SQLite có thể dùng cho application metadata.

Ví dụ SQLite tables:

```text
settings
indexed_roots
excluded_paths
search_history
recent_files
application_state
```

Không lưu toàn bộ file metadata vào SQLite nếu Lucene đã chứa thông tin cần thiết.

Tránh duplicate source of truth không cần thiết.

---

# 31. Configuration

Ví dụ:

```text
~/.desktop-search/
```

hoặc trên Windows:

```text
%LOCALAPPDATA%\DesktopSearch
```

Cấu trúc:

```text
DesktopSearch/
│
├── index/
├── database/
│   └── app.db
├── logs/
│   └── desktop-search.log
└── config/
```

---

# 32. Logging

Application cần structured logging tối thiểu.

Ví dụ:

```text
INFO  indexing started root=D:\Projects
INFO  indexing completed files=125402 duration=32s

WARN  cannot read file path=...
ERROR extraction failed path=...
```

Không log toàn bộ mỗi file ở INFO vì có thể sinh hàng triệu dòng.

Per-file log nên ở DEBUG hoặc TRACE.

---

# 33. Error Handling

Các lỗi filesystem phổ biến:

```text
AccessDeniedException
NoSuchFileException
FileSystemException
IOException
SecurityException
```

Một file lỗi:

```text
skip file
record failure
continue indexing
```

không terminate toàn bộ scan.

---

# 34. Race Conditions

Filesystem luôn có thể thay đổi trong lúc scan.

Ví dụ:

```text
Scanner discovers A.pdf

           ↓

User deletes A.pdf

           ↓

Indexer tries reading A.pdf
```

Indexer phải chấp nhận:

```text
NoSuchFileException
```

và bỏ qua.

Filesystem không được coi là immutable.

---

# 35. Index Consistency

Một vấn đề khác:

```text
Lucene says file exists
```

nhưng file đã bị xóa trong lúc app tắt.

Có thể giải quyết bằng startup reconciliation.

Ví dụ:

```text
Application Start
       │
       ▼
Load existing index
       │
       ▼
Quick filesystem reconciliation
       │
       ├── deleted file → remove index
       ├── modified file → re-index
       └── new file → index
```

Version đầu có thể đơn giản hơn bằng periodic rescan.

---

# 36. Search Pagination

Không nên trả toàn bộ result.

Ví dụ query:

```text
java
```

có thể match:

```text
50,000 files
```

UI chỉ cần:

```text
top 50
```

hoặc:

```text
top 100
```

Search API cần limit.

---

# 37. Search-as-you-type

UI có thể search mỗi khi user nhập.

Nhưng không nên query Lucene từng keystroke tức thời:

```text
t
tr
tra
tran
trans
```

trong vài milliseconds.

Sử dụng debounce:

```text
user typing
    │
    ▼
wait ~150–250 ms
    │
    ▼
SearchService
```

---

# 38. Cancellation

Nếu user search:

```text
spring
```

sau đó lập tức nhập:

```text
spring transaction
```

kết quả query cũ không được overwrite query mới.

Có thể sử dụng:

```text
query sequence id
```

Ví dụ:

```text
query #15 spring

query #16 spring transaction
```

UI chỉ chấp nhận result của query mới nhất.

---

# 39. Initial Package Structure

```text
com.desktopsearch
│
├── DesktopSearchApplication.java
│
├── application
│   ├── ApplicationContext.java
│   └── ApplicationLifecycle.java
│
├── ui
│   ├── controller
│   ├── view
│   ├── model
│   └── component
│
├── search
│   ├── SearchService.java
│   ├── DefaultSearchService.java
│   ├── SearchRequest.java
│   ├── SearchResult.java
│   ├── query
│   │   ├── SearchQuery.java
│   │   ├── QueryParser.java
│   │   └── LuceneQueryBuilder.java
│   └── ranking
│
├── indexing
│   ├── IndexService.java
│   ├── LuceneIndexService.java
│   ├── DocumentMapper.java
│   └── IndexCoordinator.java
│
├── scanner
│   ├── FileScanner.java
│   ├── NioFileScanner.java
│   ├── FileMetadata.java
│   └── ScanConfiguration.java
│
├── watcher
│   ├── FileWatcher.java
│   ├── NioFileWatcher.java
│   ├── FileChangeEvent.java
│   └── FileChangeType.java
│
├── extractor
│   ├── ContentExtractor.java
│   ├── TikaContentExtractor.java
│   └── ExtractionResult.java
│
├── persistence
│   ├── settings
│   └── history
│
├── platform
│   └── windows
│       ├── WindowsFileLauncher.java
│       └── WindowsExplorerService.java
│
└── common
    ├── concurrent
    ├── event
    └── exception
```

---

# 40. Dependency Direction

Phải tránh việc module gọi lung tung.

Dependency mong muốn:

```text
UI
 │
 ▼
Application Services
 │
 ├──────────────► Search
 │
 ├──────────────► Index
 │
 └──────────────► Settings

Scanner ────────► Index

Watcher ────────► Index

Index ──────────► Extractor

Search ─────────► Lucene
```

Không nên:

```text
Lucene → UI
Scanner → JavaFX Controller
Tika → UI
Watcher → SearchView
```

---

# 41. Event Model

Các background service có thể publish application events.

Ví dụ:

```text
IndexingStarted
IndexingProgress
IndexingCompleted
IndexingFailed

FileIndexed
FileRemoved

SearchCompleted
```

Không cần triển khai một event bus phức tạp ngay lập tức.

Có thể bắt đầu bằng Java interface/listener.

---

# 42. Startup Lifecycle

Application startup dự kiến:

```text
START
 │
 ▼
Load config
 │
 ▼
Open SQLite
 │
 ▼
Open Lucene Directory
 │
 ▼
Create IndexWriter
 │
 ▼
Create SearcherManager
 │
 ▼
Start Watcher
 │
 ▼
Perform reconciliation if required
 │
 ▼
Show Main Window
```

UI có thể được show sớm hơn và hiển thị trạng thái:

```text
Preparing search index...
```

---

# 43. Shutdown Lifecycle

Không được đóng process trực tiếp.

Shutdown:

```text
Stop accepting indexing jobs
       │
       ▼
Stop watcher
       │
       ▼
Drain/stop worker queues
       │
       ▼
Commit Lucene
       │
       ▼
Close IndexWriter
       │
       ▼
Close SQLite
       │
       ▼
Shutdown executors
       │
       ▼
EXIT
```

---

# 44. Index Commit Strategy

Không nên Lucene commit sau từng file.

Sai:

```text
index file
commit

index file
commit

index file
commit
```

Commit là operation tương đối expensive.

Có thể:

```text
commit after batch
```

hoặc:

```text
commit periodically
```

Ví dụ:

```text
every 5 seconds
```

hoặc:

```text
every N documents
```

Lucene Near Real-Time Search có thể cho phép search thấy document mới mà chưa cần full commit.

---

# 45. Initial Indexing Pipeline

Pipeline đầy đủ:

```text
Indexed Root
     │
     ▼
NioFileScanner
     │
     ▼
FileMetadata
     │
     ▼
Filter
     │
     ├── excluded path
     ├── unsupported file
     └── ignored file
     │
     ▼
Index Queue
     │
     ▼
Index Worker
     │
     ├── metadata mapping
     │
     └── optional content extraction
     │
     ▼
Lucene Document
     │
     ▼
IndexWriter
```

---

# 46. Search Pipeline

```text
Keyboard Input
      │
      ▼
UI Debounce
      │
      ▼
SearchRequest
      │
      ▼
QueryParser
      │
      ▼
SearchQuery
      │
      ▼
LuceneQueryBuilder
      │
      ▼
IndexSearcher
      │
      ▼
TopDocs
      │
      ▼
SearchResult Mapper
      │
      ▼
UI Result List
```

---

# 47. Incremental Update Pipeline

```text
Windows Filesystem
       │
       ▼
WatchService
       │
       ▼
FileChangeEvent
       │
       ▼
Debouncer
       │
       ▼
Index Update Queue
       │
       ├── CREATE
       ├── MODIFY
       └── DELETE
       │
       ▼
IndexService
       │
       ▼
Lucene
```

---

# 48. Technology Stack

Proposed stack:

```text
Java 21+
JavaFX
Apache Lucene
Apache Tika
SQLite
SLF4J
Logback
JUnit 5
Mockito
JMH
Maven hoặc Gradle
```

V1 không sử dụng Spring Boot.

Lý do:

Desktop Search hiện không cần:

```text
web server
MVC
REST
Spring Security
Spring Data
microservice infrastructure
```

Dependency Injection nếu cần có thể làm bằng constructor injection thuần Java.

---

# 49. Testing Strategy

## Unit Test

Test các component:

```text
QueryParser
DocumentMapper
FileFilter
SearchQueryBuilder
SearchResultMapper
```

Ví dụ:

```text
input:

ext:pdf transaction

expected:

keyword = transaction
extension = pdf
```

---

## Integration Test

Tạo temporary directory:

```text
temp/
├── hello.txt
├── java.pdf
└── src/
    └── TransactionService.java
```

Sau đó:

```text
scan
index
search
assert result
```

---

## Concurrency Test

Test:

```text
10,000
100,000
1,000,000
```

synthetic file records.

Kiểm tra:

```text
memory
queue size
throughput
thread count
```

---

## Benchmark

Sử dụng JMH cho các component phù hợp.

Các metric:

```text
files indexed / second
search latency p50
search latency p95
index size
memory usage
startup time
```

---

# 50. Performance Metrics

Application nên có internal metrics đơn giản.

Ví dụ:

```text
Total indexed files
Indexing rate
Queue length
Failed files
Average extraction time
Search latency
Index size
```

Ban đầu chỉ log metrics, chưa cần dashboard.

---

# 51. Security and Privacy

Desktop Search chạy local.

Không upload:

```text
filename
file content
search query
```

lên external server.

Các file không đọc được do permission sẽ được skip.

Application không cố bypass Windows filesystem permission.

---

# 52. Edge Cases

Phải xem xét:

```text
very long paths
Unicode filenames
Vietnamese filenames
Japanese filenames
hidden files
read-only files
permission denied
symbolic links
junctions
network paths
file deleted during indexing
file renamed during indexing
very large files
corrupted documents
millions of small files
```

---

# 53. Symbolic Links

Recursive scan cần tránh cycle.

Ví dụ:

```text
A → B
B → A
```

V1 có thể:

```text
do not follow symbolic links
```

để giảm complexity.

Sau này có thể configuration hóa.

---

# 54. Large Files

Content extraction cần giới hạn.

Ví dụ:

```text
file size > 50 MB
```

chỉ index metadata.

Không extract content.

File vẫn tìm được bằng filename/path.

---

# 55. Architecture Evolution

V1:

```text
Desktop Monolith
```

Sau này nếu indexing cần chạy độc lập UI:

```text
Desktop UI
    │
    ▼
Local Search Engine Process
```

có thể tách thành:

```text
desktop-search-ui.exe

desktop-search-engine.exe
```

và giao tiếp IPC.

Nhưng không nên thiết kế quá sớm cho requirement chưa tồn tại.

---

# 56. Development Milestones

## Milestone 1 — Project Skeleton

Implement:

```text
Java project
JavaFX window
module/package structure
logging
configuration
```

Output:

App chạy được.

---

## Milestone 2 — Scanner

Implement:

```text
FileScanner
NioFileScanner
FileMetadata
ScanConfiguration
```

Có thể scan:

```text
100k+ files
```

mà không load tất cả vào RAM.

---

## Milestone 3 — Lucene Index

Implement:

```text
Lucene initialization
DocumentMapper
IndexService
```

Metadata được persist.

---

## Milestone 4 — Basic Search

Implement:

```text
SearchService
SearchRequest
SearchResult
Lucene query
```

Search filename.

---

## Milestone 5 — JavaFX Search UI

Implement:

```text
search input
search result list
double click
open folder
copy path
```

---

## Milestone 6 — Background Indexing

Thêm:

```text
thread pool
bounded queue
progress
backpressure
cancellation
```

---

## Milestone 7 — File Watcher

Thêm:

```text
WatchService
recursive registration
CREATE
MODIFY
DELETE
debounce
```

---

## Milestone 8 — Full-text Search

Thêm:

```text
Apache Tika
content extraction
content field
highlight
```

---

## Milestone 9 — Query Language

Thêm:

```text
ext:
size:
path:
modified:
```

---

## Milestone 10 — Optimization

Benchmark:

```text
100k files
500k files
1m files
```

Optimize:

```text
thread pool
batching
Lucene analyzer
commit interval
memory
queue size
```

---

# 57. MVP Definition

Desktop Search được coi là hoàn thành MVP khi:

1. User có thể chọn directory.
2. Application scan directory.
3. Metadata được index bằng Lucene.
4. Restart app không mất index.
5. Search filename nhanh.
6. Search-as-you-type hoạt động.
7. Có filter extension.
8. Có sort.
9. Có open file.
10. Có open containing folder.
11. UI không freeze trong lúc indexing.
12. File mới được tự động index.
13. File bị xóa được remove khỏi index.
14. Có indexing progress.
15. Có xử lý basic filesystem error.

Full-text search có thể nằm ngay sau MVP.

---

# 58. Important Design Principles

Project tuân theo một số nguyên tắc.

### Principle 1

**Filesystem không phải search database.**

Không scan disk khi user search.

---

### Principle 2

**Index là read-optimized representation của filesystem.**

```text
Filesystem
    ↓
Index
    ↓
Search
```

---

### Principle 3

**Scanner discover file, Indexer index file.**

Không trộn responsibility.

---

### Principle 4

**UI không biết Lucene.**

UI chỉ biết:

```text
SearchService
SearchResult
```

---

### Principle 5

**Background workload không được block UI.**

---

### Principle 6

**Không load dataset lớn vào RAM nếu có thể stream.**

---

### Principle 7

**Không thêm distributed architecture khi chưa có distributed problem.**

---

### Principle 8

**Measure before optimize.**

Performance decision phải dựa trên benchmark.

---

# 59. Main Technical Challenges

Các bài toán kỹ thuật đáng chú ý nhất của project:

### 1. Large filesystem traversal

Scan hàng trăm nghìn file hiệu quả.

### 2. Concurrent indexing

Thiết kế producer-consumer + backpressure.

### 3. Lucene indexing strategy

Analyzer, schema, commit và refresh strategy.

### 4. Filesystem consistency

Filesystem thay đổi liên tục trong khi index.

### 5. File watcher

Theo dõi recursive filesystem.

### 6. Search latency

Giữ latency thấp khi index lớn.

### 7. Content extraction

Xử lý nhiều format và corrupted file.

### 8. Memory control

Không để indexing pipeline ăn hết RAM.

### 9. UI responsiveness

JavaFX không bị freeze.

### 10. Crash recovery

Index không corrupt khi application bị kill.

---

# 60. Final Architecture — Version 1

```text
                        ┌──────────────────────┐
                        │      JavaFX UI       │
                        └──────────┬───────────┘
                                   │
                       SearchRequest / Commands
                                   │
               ┌───────────────────┴──────────────────┐
               ▼                                      ▼
        ┌─────────────┐                       ┌─────────────────┐
        │SearchService│                       │ApplicationService│
        └──────┬──────┘                       └─────────────────┘
               │
               ▼
        ┌─────────────┐
        │   Lucene    │◄──────────────────────────────┐
        │Search Index │                               │
        └─────────────┘                               │
                                                      │
                                             ┌────────┴────────┐
                                             │  IndexService   │
                                             └────────┬────────┘
                                                      │
                        ┌─────────────────────────────┼────────────────┐
                        │                             │                │
                        ▼                             ▼                ▼
                 ┌────────────┐               ┌────────────┐   ┌────────────┐
                 │FileScanner │               │FileWatcher │   │ Extractor  │
                 └─────┬──────┘               └─────┬──────┘   └────────────┘
                       │                            │
                       └─────────────┬──────────────┘
                                     ▼
                              Windows File System
```

---

# 61. Architecture Decision Summary

| Decision | Choice |
|---|---|
| Application type | Desktop |
| Architecture | Modular Monolith |
| Backend server | Không |
| Communication | In-process method calls |
| Language | Java |
| Desktop UI | JavaFX |
| Search engine | Apache Lucene |
| Filesystem API | Java NIO |
| File watcher | WatchService |
| Document extraction | Apache Tika |
| Configuration metadata | SQLite |
| Concurrency | ExecutorService + BlockingQueue |
| Search strategy V1 | Keyword |
| Semantic search | Future |
| OS target đầu tiên | Windows |

---

# 62. Next Design Step

Sau System Design v1, phần tiếp theo cần thiết kế sâu hơn là:

```text
Indexing Engine Design
```

Bao gồm:

```text
FileMetadata model
Lucene field schema
Analyzer strategy
File scanner implementation
Index coordinator
Blocking queue
worker lifecycle
batch indexing
commit strategy
refresh strategy
duplicate events
filesystem watcher
index recovery
```

Đây sẽ là module core quan trọng nhất của Desktop Search.

Sau khi Indexing Engine Design ổn định mới bắt đầu implementation code.

---

# 63. Current Design Conclusion

Desktop Search Version 1 sẽ được xây dựng như một **local modular monolith**.

Core architecture:

```text
Filesystem
    ↓
Scanner / Watcher
    ↓
Indexing Pipeline
    ↓
Lucene
    ↓
Search Engine
    ↓
JavaFX
```

Không REST.

Không Spring Boot.

Không microservice.

Không AI ở giai đoạn đầu.

Ưu tiên của Version 1 là xây được một search engine local:

```text
fast
stable
incremental
memory-efficient
testable
extensible
```

Khi foundation này hoạt động tốt, semantic search và những feature nâng cao có thể được thêm vào mà không cần phá vỡ core architecture.