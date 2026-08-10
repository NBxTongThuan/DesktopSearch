# Desktop Search — Indexing Engine Design v1

**Document:** Indexing Engine Design  
**Version:** 1.0  
**Parent document:** Desktop Search — System Design v1  
**Status:** Draft  
**Target:** Windows Desktop Application  
**Language:** Java  
**Search Engine:** Apache Lucene

---

# 1. Purpose

Indexing Engine là core component chịu trách nhiệm chuyển trạng thái của Windows File System thành một search index có thể query nhanh.

Nhiệm vụ chính:

```text
Filesystem
    ↓
Discover Files
    ↓
Read Metadata
    ↓
Filter
    ↓
Optional Content Extraction
    ↓
Build Lucene Document
    ↓
Write Index
```

Indexing Engine phải xử lý được:

- initial full scan;
- incremental file update;
- file creation;
- file modification;
- file deletion;
- large directory trees;
- concurrent processing;
- filesystem errors;
- backpressure;
- Lucene commit;
- Lucene refresh;
- application restart;
- crash recovery.

---

# 2. Design Goals

Indexing Engine V1 phải đạt các mục tiêu sau.

## 2.1 Performance

Có khả năng xử lý:

```text
100,000+
500,000+
1,000,000+
```

file mà không yêu cầu giữ toàn bộ danh sách file trong RAM.

---

## 2.2 Memory Safety

Memory usage phải tương đối ổn định.

Không cho phép architecture dạng:

```java
List<Path> files = Files.walk(root).toList();
```

với filesystem lớn.

Thay vào đó sử dụng streaming pipeline.

---

## 2.3 Fault Isolation

Một file lỗi:

```text
AccessDenied
FileNotFound
Corrupted PDF
Locked file
```

không được làm toàn bộ indexing process dừng lại.

---

## 2.4 Near Real-Time Search

File mới index cần có thể xuất hiện trong search result trong thời gian ngắn mà không cần Lucene `commit()` sau từng file.

---

## 2.5 Incremental Updates

Không rebuild toàn bộ index sau mỗi thay đổi filesystem.

---

## 2.6 Testability

Scanner, queue coordinator, document mapper, filtering và Lucene layer phải có thể test độc lập.

---

# 3. Indexing Engine Architecture

High-level architecture:

```text
                      ┌──────────────────┐
                      │    File Root     │
                      └────────┬─────────┘
                               │
                               ▼
                      ┌──────────────────┐
                      │   File Scanner   │
                      └────────┬─────────┘
                               │
                               ▼
                      ┌──────────────────┐
                      │   File Filter    │
                      └────────┬─────────┘
                               │
                               ▼
                      ┌──────────────────┐
                      │ Metadata Reader  │
                      └────────┬─────────┘
                               │
                               ▼
                     ┌────────────────────┐
                     │ IndexTask Producer │
                     └─────────┬──────────┘
                               │
                               ▼
                      ┌──────────────────┐
                      │  Bounded Queue   │
                      └────────┬─────────┘
                               │
                 ┌─────────────┼─────────────┐
                 ▼             ▼             ▼
             Worker 1      Worker 2      Worker N
                 │             │             │
                 └─────────────┼─────────────┘
                               ▼
                     ┌─────────────────────┐
                     │ Content Extraction  │
                     │      optional       │
                     └──────────┬──────────┘
                                ▼
                     ┌─────────────────────┐
                     │   Document Mapper   │
                     └──────────┬──────────┘
                                ▼
                     ┌─────────────────────┐
                     │ Lucene IndexWriter  │
                     └──────────┬──────────┘
                                │
                 ┌──────────────┴───────────────┐
                 ▼                              ▼
              Refresh                        Commit
                 │                              │
                 ▼                              ▼
         SearcherManager                  Persistent Index
```

---

# 4. Core Components

Indexing Engine gồm các component chính:

```text
FileScanner
FileMetadataReader
FileFilter
IndexCoordinator
IndexTaskQueue
IndexWorker
ContentExtractor
LuceneDocumentMapper
IndexRepository
IndexCommitManager
IndexRefreshManager
IndexProgressTracker
IndexErrorHandler
```

---

# 5. FileMetadata Model

`FileMetadata` đại diện trạng thái filesystem của một file tại thời điểm scan.

Đề xuất:

```java
public record FileMetadata(
    String id,
    String name,
    Path path,
    Path parentPath,
    String extension,
    long size,
    Instant createdAt,
    Instant modifiedAt,
    boolean directory,
    boolean hidden
) {}
```

---

# 6. File ID Strategy

V1 sử dụng:

```text
normalized absolute path
```

làm identity.

Ví dụ:

```text
D:\Projects\desktop-search\README.md
```

normalize thành dạng thống nhất.

Logical ID có thể:

```text
id = normalizedPath
```

hoặc hash của normalized path.

Ví dụ:

```text
SHA-256(normalizedPath)
```

Tuy nhiên V1 nên ưu tiên đơn giản:

```text
path_exact
```

làm unique identifier trực tiếp.

---

# 7. Path Normalization

Path phải được normalize trước khi index.

Pipeline:

```text
Path
 ↓
toAbsolutePath()
 ↓
normalize()
 ↓
String representation
```

Trên Windows cần lưu ý:

```text
C:\Projects\Test.java

c:\projects\Test.java
```

filesystem thường case-insensitive.

V1 có thể normalize identity về lowercase.

Ví dụ:

```java
String normalized =
    path.toAbsolutePath()
        .normalize()
        .toString()
        .toLowerCase(Locale.ROOT);
```

Nhưng display path vẫn phải lưu bản gốc.

Do đó Lucene có:

```text
path_exact
path_display
```

---

# 8. FileMetadataReader

Scanner chỉ discover `Path`.

MetadataReader chịu trách nhiệm đọc attributes.

Interface:

```java
public interface FileMetadataReader {

    FileMetadata read(Path path) throws IOException;

}
```

Implementation sử dụng:

```java
Files.readAttributes(
    path,
    BasicFileAttributes.class
);
```

Flow:

```text
Path
 │
 ▼
BasicFileAttributes
 │
 ▼
FileMetadata
```

---

# 9. FileScanner

Scanner chỉ chịu trách nhiệm traverse directory.

Interface:

```java
public interface FileScanner {

    ScanResult scan(
        Path root,
        FileScanListener listener
    );

}
```

Có thể dùng listener streaming:

```java
public interface FileScanListener {

    void onFile(Path file);

    void onError(Path path, Exception exception);

}
```

---

# 10. Scanner Implementation

V1 sử dụng:

```java
Files.walkFileTree(...)
```

và:

```java
SimpleFileVisitor<Path>
```

Pseudo flow:

```text
visitFile
    ↓
filter
    ↓
metadata reader
    ↓
submit task
```

Không sử dụng:

```java
Files.walk(root).toList()
```

vì nó dễ dẫn tới memory pressure.

---

# 11. Directory Handling

Search V1 chủ yếu search file.

Có hai lựa chọn:

### Option A

Chỉ index file.

### Option B

Index cả directory.

Đề xuất:

**Index cả file và directory.**

Như vậy user có thể search:

```text
Projects
Documents
desktop-search
```

và mở directory trực tiếp.

`FileMetadata.directory` dùng để phân biệt.

---

# 12. File Filtering

Trước khi submit indexing task cần kiểm tra filter.

Interface:

```java
public interface FileFilter {

    boolean accept(Path path, FileMetadata metadata);

}
```

Có thể tổ hợp:

```text
ExcludedPathFilter
HiddenFileFilter
FileSizeFilter
ExtensionFilter
SystemDirectoryFilter
```

---

# 13. Excluded Paths

Ví dụ default excluded candidates:

```text
$RECYCLE.BIN
System Volume Information
```

Các thư mục developer có thể cho user exclude:

```text
.git
node_modules
target
build
.gradle
.idea
```

Nhưng không nên hard-code quá nhiều.

User config quyết định phần lớn exclude rule.

---

# 14. Filter Pipeline

```text
FileMetadata
     │
     ▼
CompositeFileFilter
     │
     ├── excluded path?
     ├── unsupported?
     ├── too large?
     ├── ignored?
     └── allowed?
           │
           ▼
       IndexTask
```

---

# 15. IndexTask Model

Không đưa raw `Path` vào queue nếu sau đó worker phải đọc lại toàn bộ metadata.

Tạo model:

```java
public sealed interface IndexTask
    permits UpsertFileTask, DeleteFileTask {
}
```

---

# 16. UpsertFileTask

```java
public record UpsertFileTask(
    FileMetadata metadata
) implements IndexTask {}
```

`UPSERT` sử dụng cho:

```text
CREATE
MODIFY
initial scan
```

Worker không cần phân biệt create/update.

Lucene layer thực hiện:

```text
updateDocument()
```

với unique path.

---

# 17. DeleteFileTask

```java
public record DeleteFileTask(
    String normalizedPath
) implements IndexTask {}
```

Dùng cho watcher event:

```text
DELETE
```

---

# 18. Why Upsert

Nếu dùng:

```text
CREATE -> addDocument
MODIFY -> updateDocument
```

application phải biết chắc file có tồn tại trong index hay không.

Không cần thiết.

Thay vào đó:

```text
CREATE
MODIFY
RECONCILE
```

đều trở thành:

```text
UPSERT
```

Index layer xử lý idempotently.

---

# 19. Index Coordinator

`IndexCoordinator` điều phối toàn bộ indexing pipeline.

Responsibilities:

```text
start scan
submit tasks
manage queues
start workers
track progress
cancel indexing
shutdown
```

Interface:

```java
public interface IndexCoordinator {

    void startInitialScan(Collection<Path> roots);

    void submit(IndexTask task);

    void cancel();

    IndexingStatus status();

}
```

---

# 20. Queue Design

Dùng:

```java
BlockingQueue<IndexTask>
```

Implementation ban đầu:

```java
ArrayBlockingQueue
```

hoặc:

```java
LinkedBlockingQueue
```

với explicit capacity.

Đề xuất V1:

```text
ArrayBlockingQueue
```

vì:

- fixed memory footprint;
- predictable;
- built-in backpressure.

---

# 21. Queue Capacity

Initial value:

```text
5,000 tasks
```

Ví dụ:

```java
new ArrayBlockingQueue<>(5000);
```

Không coi `5000` là magic optimal value.

Sau này benchmark.

Config:

```text
index.queue.capacity=5000
```

---

# 22. Backpressure

Ví dụ:

```text
Scanner: 50,000 files/sec
Worker:  10,000 files/sec
```

Nếu unlimited queue:

```text
40,000 tasks/sec accumulate
```

RAM liên tục tăng.

Bounded queue:

```text
queue full
   ↓
producer blocks
   ↓
workers consume
   ↓
producer continues
```

Đây là intentional behavior.

---

# 23. Producer

Initial scan đóng vai trò Producer.

```text
FileScanner
    ↓
IndexCoordinator.submit()
    ↓
BlockingQueue.put()
```

Dùng:

```java
queue.put(task);
```

thay vì:

```java
queue.add(task);
```

vì `put()` block khi queue full.

---

# 24. Consumer Workers

Worker loop:

```text
while running:
    task = queue.take()

    try:
        process(task)
    catch:
        error handler
```

Không terminate worker nếu một task fail.

---

# 25. Worker Count

Initial recommendation:

```text
metadata-only indexing:
2–4 worker threads
```

Full-text extraction:

```text
2–N extraction workers
```

Thread count nên configurable.

Ví dụ:

```text
index.worker.count=4
```

---

# 26. Why Not One Thread per File

Không bao giờ làm:

```java
new Thread(() -> index(file)).start();
```

cho mỗi file.

Với 100k file sẽ phá hệ thống.

Phải dùng bounded concurrency.

---

# 27. Executor Design

Có thể dùng:

```text
scannerExecutor
indexWorkerExecutor
maintenanceExecutor
watcherExecutor
```

Ví dụ:

```text
scannerExecutor
    = single thread

indexWorkerExecutor
    = fixed thread pool

maintenanceExecutor
    = scheduled single thread

watcherExecutor
    = single thread
```

---

# 28. Separate Scanner and Worker Pools

Không dùng chung worker pool giữa scanner và indexer.

Nếu dùng chung:

```text
scanner tasks
content extraction
Lucene indexing
```

có thể starve nhau.

V1 tách workload rõ ràng.

---

# 29. Index Worker Pipeline

Mỗi `UpsertFileTask` đi qua:

```text
IndexTask
    │
    ▼
Validate Current File State
    │
    ▼
Content Extraction
    │
    ▼
LuceneDocumentMapper
    │
    ▼
IndexRepository.upsert()
```

Delete:

```text
DeleteFileTask
    │
    ▼
IndexRepository.delete()
```

---

# 30. Revalidate Before Index

Filesystem có thể thay đổi sau khi scanner đọc metadata.

Ví dụ:

```text
Scanner
  ↓
A.pdf exists
  ↓
task queued
  ↓
user deletes A.pdf
  ↓
worker receives task
```

Worker cần kiểm tra:

```java
Files.exists(path)
```

trước content extraction.

Nếu không tồn tại:

```text
delete index entry
```

hoặc skip.

---

# 31. Stale Metadata

Một file có thể modified sau khi metadata được đọc nhưng trước khi index.

V1 chấp nhận eventual consistency.

Watcher sau đó phát:

```text
MODIFY
```

và index lại.

Không cần lock filesystem.

---

# 32. Content Extraction Strategy

Content extraction **không bắt buộc cho MVP metadata indexing**.

Worker:

```text
if content indexing disabled:
    skip extraction
```

Khi enable:

```text
supports extension?
file size acceptable?
not directory?
    ↓
extract
```

---

# 33. ContentExtractor Interface

```java
public interface ContentExtractor {

    ExtractionResult extract(Path path);

}
```

Result:

```java
public record ExtractionResult(
    String content,
    String mimeType,
    boolean successful,
    String error
) {}
```

---

# 34. Extraction Failure

Nếu Tika fail:

```text
filename metadata vẫn được index
```

Ví dụ:

```text
report.pdf
```

corrupt.

Lucene document vẫn có:

```text
name
path
size
timestamps
extension
```

chỉ thiếu:

```text
content
```

---

# 35. Extraction Limits

Default proposal:

```text
maxExtractSize = 50 MB
```

Có thể config:

```properties
extract.enabled=true
extract.max-file-size-mb=50
```

File lớn hơn threshold:

```text
metadata-only index
```

---

# 36. Lucene Document Schema

V1 schema:

```text
FIELD                 PURPOSE
------------------------------------------------
id                    unique identity
name                  filename search
name_exact            exact match / sort
path                   path search
path_exact             identity / update/delete
path_display           UI display
parent_path            parent filtering
extension              filtering
size                   numeric filtering/sorting
created_at             filtering/sorting
modified_at            filtering/sorting
is_directory           filtering
content                full text search
```

---

# 37. Lucene Field Design

## id

```text
StringField
stored = YES
```

---

## name

```text
TextField
stored = YES
```

Analyzer applied.

---

## name_exact

```text
StringField
stored = YES
```

Có thể thêm:

```text
SortedDocValuesField
```

để sort.

---

## path_exact

```text
StringField
stored = YES
```

Không analyze.

---

## path_display

```text
StoredField
```

hoặc `StringField` nếu cần retrieval/filter.

---

## extension

```text
StringField
```

normalize lowercase.

---

## size

Index:

```text
LongPoint
```

Stored:

```text
StoredField
```

Sorting:

```text
NumericDocValuesField
```

Có thể cần nhiều Lucene Field cùng đại diện logical field `size`.

---

# 38. Timestamp Fields

`created_at` và `modified_at` lưu epoch millis.

Ví dụ:

```text
modified_at = 1786330800000
```

Index:

```text
LongPoint
```

Sort:

```text
NumericDocValuesField
```

Display:

```text
StoredField
```

---

# 39. Content Field

```text
TextField
stored = NO
```

Không nhất thiết lưu raw content trong Lucene.

Mục tiêu:

```text
search
```

không phải retrieval toàn bộ document.

Sau này highlight có thể cần strategy riêng.

---

# 40. DocumentMapper

Interface:

```java
public interface LuceneDocumentMapper {

    Document map(
        FileMetadata metadata,
        ExtractionResult extraction
    );

}
```

Mapper không tự extract file.

Nó chỉ map input thành Lucene document.

---

# 41. Analyzer Strategy

Filename có đặc điểm khác paragraph text.

Ví dụ:

```text
TransactionService.java
spring_transaction_note.pdf
user-profile-controller.ts
```

Nếu dùng analyzer quá đơn giản:

```text
TransactionService
```

có thể không split tốt thành:

```text
transaction
service
```

---

# 42. Filename Analyzer V1

Ban đầu có thể sử dụng một custom analyzer gồm:

```text
Tokenizer
    ↓
LowerCaseFilter
```

và cân nhắc split:

```text
camelCase
snake_case
kebab-case
dot
```

Ví dụ:

```text
TransactionService.java
```

tokens:

```text
transaction
service
java
```

---

# 43. Content Analyzer

Content có thể dùng analyzer khác filename.

V1:

```text
StandardAnalyzer
```

là lựa chọn hợp lý để bắt đầu.

Không cần custom linguistic analyzer quá sớm.

---

# 44. Multiple Analyzers

Có thể dùng:

```text
PerFieldAnalyzerWrapper
```

conceptually:

```text
name     → FilenameAnalyzer
path     → PathAnalyzer
content  → StandardAnalyzer
```

---

# 45. Unicode Support

Filename cần hỗ trợ:

```text
Vietnamese
Japanese
Unicode symbols
```

Ví dụ:

```text
Thiết kế hệ thống.pdf
設計書.docx
```

Không được normalize bằng cách xóa Unicode.

Case normalization phải Unicode-safe.

---

# 46. Lucene IndexRepository

Không expose `IndexWriter` ra ngoài.

Interface:

```java
public interface IndexRepository {

    void upsert(
        FileMetadata metadata,
        ExtractionResult extraction
    );

    void delete(String normalizedPath);

    void commit();

}
```

---

# 47. Upsert Implementation

Conceptually:

```java
writer.updateDocument(
    new Term("path_exact", normalizedPath),
    document
);
```

Nếu document chưa tồn tại:

```text
insert
```

Nếu tồn tại:

```text
replace
```

Đây chính là behavior cần cho indexing pipeline.

---

# 48. Delete Implementation

Conceptually:

```java
writer.deleteDocuments(
    new Term("path_exact", normalizedPath)
);
```

Delete không cần biết document có thực sự tồn tại hay không.

Operation idempotent.

---

# 49. IndexWriter Ownership

Chỉ có một `IndexWriter` instance cho index chính trong application.

Không tạo:

```text
one IndexWriter per worker
```

Sai architecture:

```text
Worker 1 → Writer 1
Worker 2 → Writer 2
Worker 3 → Writer 3
```

Đúng:

```text
Worker 1 ─┐
Worker 2 ─┼──► shared IndexWriter
Worker 3 ─┘
```

---

# 50. IndexWriter Thread Safety

Application có thể cho multiple worker gọi indexing repository.

`IndexWriter` được thiết kế để hỗ trợ concurrent indexing.

Tuy nhiên application vẫn giới hạn worker để kiểm soát:

```text
CPU
IO
content extraction
memory
```

---

# 51. Commit vs Refresh

Hai khái niệm phải tách biệt.

## Refresh

Làm document mới index có thể search được.

## Commit

Persist durable index state xuống disk.

Không phải mỗi lần search thấy document mới đều cần commit.

---

# 52. Bad Commit Strategy

Không:

```text
file 1
commit

file 2
commit

file 3
commit
```

Vì sẽ làm throughput giảm mạnh.

---

# 53. Commit Strategy V1

Đề xuất:

```text
periodic commit
```

Ví dụ:

```text
every 5–10 seconds
```

và:

```text
on graceful shutdown
```

Có thể thêm threshold:

```text
after N changed documents
```

nhưng V1 chưa cần.

---

# 54. Commit Manager

Component:

```java
public interface IndexCommitManager {

    void start();

    void requestCommit();

    void shutdown();

}
```

Có thể dùng:

```text
ScheduledExecutorService
```

chạy:

```text
writer.commit()
```

periodically.

---

# 55. Commit Lock

Không được có nhiều scheduled commit chạy overlap.

Ví dụ:

```text
commit A takes 8 seconds
timer fires commit B after 5 seconds
```

Cần đảm bảo:

```text
one active commit at a time
```

---

# 56. Near Real-Time Refresh

Search nên sử dụng mô hình near-real-time.

Architecture:

```text
IndexWriter
    │
    ▼
SearcherManager
    │
    ▼
IndexSearcher
```

Index refresh:

```text
SearcherManager.maybeRefresh()
```

theo periodic interval.

---

# 57. Refresh Interval

Initial proposal:

```text
500 ms – 1 second
```

Ví dụ:

```text
refresh every 500 ms
```

Không cần refresh mỗi document.

---

# 58. Searcher Lifecycle

Search service:

```text
acquire searcher
    ↓
perform query
    ↓
release searcher
```

Không giữ một `IndexSearcher` cũ vĩnh viễn.

SearcherManager chịu trách nhiệm lifecycle.

---

# 59. Refresh Manager

```java
public interface IndexRefreshManager {

    void start();

    void refresh();

    void shutdown();

}
```

Scheduler:

```text
every 500 ms:
    maybeRefresh()
```

---

# 60. Full Initial Scan

Initial scan flow:

```text
User adds:
D:\Projects
        │
        ▼
IndexCoordinator
        │
        ▼
ScannerExecutor
        │
        ▼
Files.walkFileTree
        │
        ▼
FileMetadataReader
        │
        ▼
FileFilter
        │
        ▼
UpsertFileTask
        │
        ▼
BlockingQueue
        │
        ▼
Index Workers
        │
        ▼
Lucene
```

---

# 61. Multi-root Scan

User có thể index:

```text
D:\Projects
D:\Documents
C:\Users\Thuan\Desktop
```

Có thể scan sequentially trong V1.

```text
root 1
 ↓
root 2
 ↓
root 3
```

Không cần concurrent scan nhiều root ngay.

Filesystem traversal thường IO-bound nhưng concurrent root scanning có thể làm disk seek tệ hơn.

Benchmark trước khi optimize.

---

# 62. Initial Scan Progress

Need metrics:

```text
filesDiscovered
filesQueued
filesIndexed
filesSkipped
filesFailed
bytesProcessed
```

Không nhất thiết biết total file trước.

Nếu muốn progress percentage chính xác:

```text
phải scan hai lần
```

không đáng.

V1 hiển thị:

```text
Indexed 128,542 files
```

thay vì:

```text
72%
```

---

# 63. Indexing State

Possible state:

```java
enum IndexingState {
    IDLE,
    SCANNING,
    PAUSED,
    CANCELLING,
    FAILED,
    COMPLETED
}
```

PAUSED có thể để future nếu chưa implement.

---

# 64. IndexProgress

```java
public record IndexProgress(
    IndexingState state,
    long discovered,
    long queued,
    long indexed,
    long skipped,
    long failed
) {}
```

---

# 65. Progress Updates

Không publish event mỗi file.

Nếu index:

```text
100,000 files/sec
```

mà UI nhận 100k update thì UI chết.

Progress nên throttle.

Ví dụ:

```text
update UI every 250 ms
```

hoặc:

```text
every 500 files
```

---

# 66. Cancellation

User có thể stop indexing.

Cancellation không nên:

```text
Thread.stop()
```

Flow:

```text
cancel requested
     │
     ▼
scanner checks flag
     │
     ▼
stop producing new tasks
     │
     ▼
worker shutdown strategy
```

---

# 67. Cancellation Policy

Có hai option.

### Immediate

Discard queued tasks.

### Graceful

Stop scanning nhưng xử lý hết queue.

Đề xuất V1:

```text
Stop scanning
Drain currently queued tasks
Commit
Return IDLE
```

Ổn định hơn.

---

# 68. Poison Pill

Có thể dùng poison task để stop worker:

```text
STOP_TASK
```

nhưng với ExecutorService không bắt buộc.

Có thể:

```text
running = false
workers interrupted
```

Thiết kế cần tránh worker kẹt mãi tại `queue.take()`.

---

# 69. Watcher Integration

Initial scan và watcher phải dùng chung IndexCoordinator.

Không xây hai indexing code path khác nhau.

```text
Initial Scanner ───┐
                   │
                   ▼
                IndexTask
                   ▲
                   │
File Watcher ──────┘
```

---

# 70. Filesystem Event Model

```java
public record FileChangeEvent(
    FileChangeType type,
    Path path,
    Instant detectedAt
) {}
```

Types:

```java
CREATE,
MODIFY,
DELETE
```

Không cần explicit RENAME trong V1.

Rename =

```text
DELETE oldPath
CREATE newPath
```

---

# 71. Watcher Event Translation

CREATE:

```text
path
 ↓
read metadata
 ↓
UpsertFileTask
```

MODIFY:

```text
path
 ↓
read latest metadata
 ↓
UpsertFileTask
```

DELETE:

```text
path
 ↓
normalize path
 ↓
DeleteFileTask
```

---

# 72. Event Debounce

File save có thể tạo:

```text
MODIFY
MODIFY
MODIFY
```

trong thời gian ngắn.

Do đó:

```text
Watcher
   ↓
Debounce Buffer
   ↓
IndexTask
```

---

# 73. Debounce Key

Key:

```text
normalizedPath
```

Data:

```text
latest event type
last event timestamp
```

Default debounce:

```text
300 ms
```

---

# 74. Event Coalescing

Ví dụ:

```text
CREATE
MODIFY
MODIFY
```

coalesce thành:

```text
UPSERT
```

---

# 75. DELETE + CREATE

Một rename hoặc atomic save có thể sinh:

```text
DELETE
CREATE
```

V1 xử lý độc lập.

Nếu cuối cùng file tồn tại:

```text
UPSERT
```

Nếu không:

```text
DELETE
```

Eventual index state vẫn đúng.

---

# 76. Temporary Files

Applications thường tạo temporary file:

```text
~$document.docx
file.tmp
.swap
```

Có thể gây noisy events.

V1 nên có configurable ignore pattern.

Ví dụ:

```text
*.tmp
~$*
*.swp
```

Nhưng tránh hard-code aggressive rules làm mất file user muốn search.

---

# 77. New Directory Event

WatchService cần register directory mới.

Flow:

```text
CREATE directory
       │
       ▼
register watcher
       │
       ▼
scan directory contents
       │
       ▼
index files
```

Cần xử lý trường hợp directory được tạo với nhiều file đã có sẵn rất nhanh.

---

# 78. Recursive Watch Registration

Initial root:

```text
D:\Projects
```

cần register:

```text
D:\Projects
D:\Projects\App1
D:\Projects\App1\src
D:\Projects\App2
...
```

Với rất nhiều directory có thể tốn resource.

Đây là limitation quan trọng của Java WatchService.

---

# 79. Watcher Overflow

WatchService có thể phát:

```text
OVERFLOW
```

nghĩa là event bị mất.

Không được ignore.

Policy:

```text
OVERFLOW
    ↓
mark root dirty
    ↓
schedule reconciliation scan
```

---

# 80. Reconciliation

Watcher không phải guaranteed perfect source of truth.

Do đó architecture cần reconciliation.

```text
Watcher = fast incremental updates

Periodic Scan = correctness safety net
```

---

# 81. Reconciliation Strategy V1

Có thể chạy:

```text
on startup
```

và future:

```text
periodically
```

Ví dụ:

```text
every several hours
```

Không cần implement scheduled full scan ngay MVP.

Nhưng startup reconciliation rất đáng có.

---

# 82. Startup Reconciliation

Option đơn giản V1:

```text
Application starts
    ↓
load roots
    ↓
start background scan
    ↓
UPSERT every current file
```

Nhưng vấn đề:

```text
deleted files
```

không tự biến mất.

---

# 83. Deleted File Reconciliation

Có nhiều strategy.

### Strategy A

Clear index root rồi rebuild.

Simple nhưng expensive.

### Strategy B

Mark-and-sweep.

Recommended future.

---

# 84. Mark-and-Sweep Concept

Mỗi reconciliation session có:

```text
scan_generation
```

Ví dụ:

```text
generation = 42
```

Mỗi file seen:

```text
last_seen_generation = 42
```

Sau scan:

```text
documents belonging to root
where generation < 42
```

là deleted files.

Tuy nhiên implement với Lucene sẽ tăng complexity.

Không bắt buộc V1 MVP.

---

# 85. V1 Recovery Strategy

Đề xuất:

```text
Normal runtime:
Watcher incremental updates

Application startup:
Background rescan roots

Periodic manual "Rebuild Index":
Clear and re-index
```

Deleted stale records có thể xử lý bằng:

```text
when result opened/search result mapped
```

check existence và schedule delete.

Sau đó V2 implement reconciliation chuẩn.

---

# 86. Lazy Stale Cleanup

Search result:

```text
A.pdf
```

nhưng:

```java
Files.notExists(path)
```

SearchService có thể:

```text
exclude result
submit DeleteFileTask
```

Đây là secondary safety net.

Không thay thế watcher.

---

# 87. Index Creation

Startup:

```text
index path exists?
```

Nếu không:

```text
create Lucene index
```

Nếu có:

```text
open existing index
```

---

# 88. Index Versioning

Application schema sau này thay đổi.

Ví dụ V1 document:

```text
name
path
```

V2:

```text
name
path
content
mime
```

Cần `indexVersion`.

Có thể lưu file:

```text
index-meta.json
```

Ví dụ:

```json
{
  "schemaVersion": 1
}
```

Nếu incompatible:

```text
rebuild index
```

---

# 89. Corrupted Lucene Index

Startup có thể fail khi mở index.

V1 policy:

```text
detect failure
    ↓
notify user
    ↓
move corrupted index
    ↓
create new index
    ↓
rebuild
```

Không nên silently delete ngay.

---

# 90. Index Directory

Proposed location:

```text
%LOCALAPPDATA%\DesktopSearch\index
```

Ví dụ:

```text
C:\Users\<User>\AppData\Local\DesktopSearch\index
```

---

# 91. Separate Data from User Files

Không bao giờ tạo Lucene index bên trong user indexed directory.

Sai:

```text
D:\Projects\.desktop-search-index
```

vì watcher có thể index chính index files.

Correct:

```text
AppData\Local\DesktopSearch\index
```

---

# 92. Index Directory Exclusion

Application data path bắt buộc auto-exclude khỏi scanner.

Tránh recursive self-indexing:

```text
Lucene writes file
     ↓
Watcher sees file
     ↓
Indexer indexes Lucene file
     ↓
Lucene writes more files
```

---

# 93. Duplicate Tasks

Một file có thể bị submit nhiều lần:

```text
initial scan UPSERT
watcher MODIFY
watcher MODIFY
```

Correctness không sao vì upsert idempotent.

Nhưng performance có thể tệ.

---

# 94. Task Deduplication

V1 có thể chỉ debounce watcher.

Không cần global queue dedup phức tạp.

Nếu cần V2:

```text
pendingPaths ConcurrentHashMap
```

để tránh nhiều task cùng path.

---

# 95. Task Ordering Problem

Ví dụ:

```text
UPSERT A
DELETE A
```

nếu workers process out-of-order:

```text
DELETE executed first
UPSERT executes second
```

file bị resurrect trong index dù thực tế đã delete.

Đây là một vấn đề quan trọng.

---

# 96. Path-level Ordering

V1 cần xử lý ordering cho event updates.

Có vài lựa chọn.

### Option 1

Single writer worker.

Ordering dễ nhưng giảm throughput.

### Option 2

Partition tasks theo path hash.

### Option 3

Revalidate filesystem trước UPSERT.

Đề xuất V1:

**Revalidate before UPSERT + use upsert/delete semantics.**

Nếu UPSERT A chạy sau DELETE nhưng file thực tế không còn:

```text
worker sees Files.notExists(A)
    ↓
delete A
```

do đó state cuối vẫn đúng.

---

# 97. Modify Ordering

Hai UPSERT:

```text
version 1
version 2
```

process reverse order.

Nếu worker dùng metadata cũ sẽ có nguy cơ index version cũ.

Giải pháp:

Worker không hoàn toàn tin task metadata đối với live watcher.

Trước actual index có thể reread current attributes.

V1 có thể tạo:

```text
FileSnapshot
```

ngay khi worker process.

---

# 98. Fresh Snapshot Strategy

Task chỉ cần chứa:

```text
operation
path
```

và worker đọc metadata mới nhất.

So với storing FileMetadata trong task.

Initial design đã dùng metadata trong task.

Đề xuất refine:

```text
Initial scan:
metadata can be carried

Live events:
worker refresh metadata
```

Hoặc đơn giản hơn:

```text
worker always re-read metadata
```

Tradeoff:

thêm filesystem calls nhưng correctness tốt hơn.

---

# 99. Recommended V1 Task Model

Đề xuất cuối cùng:

```java
public record UpsertFileTask(
    Path path
) implements IndexTask {}
```

Thay vì chứa `FileMetadata`.

Worker:

```text
Path
 ↓
Files.exists
 ↓
MetadataReader
 ↓
Filter
 ↓
Extractor
 ↓
Mapper
 ↓
Lucene
```

Ưu điểm:

- fresh metadata;
- queue nhẹ hơn;
- event ordering an toàn hơn;
- scanner không phải tạo object lớn.

---

# 100. Revised Pipeline

```text
Scanner
  │
  ▼
Path
  │
  ▼
UpsertFileTask
  │
  ▼
Bounded Queue
  │
  ▼
Worker
  │
  ├── check exists
  ├── read metadata
  ├── filter
  ├── extract
  └── map
       │
       ▼
    Lucene
```

Đây là architecture được khuyến nghị cho V1.

---

# 101. Directory Delete

Watcher có thể nhận:

```text
DELETE directory
```

nhưng directory đã biến mất nên không thể traverse children để biết file nào bị xóa.

Need index API:

```java
deleteByPathPrefix(directoryPath)
```

---

# 102. Path Prefix Delete

Nếu:

```text
D:\Projects\App1
```

bị xóa.

Phải remove:

```text
D:\Projects\App1
D:\Projects\App1\a.java
D:\Projects\App1\src\b.java
...
```

Do đó Lucene schema cần hỗ trợ directory/root relationship.

---

# 103. Indexed Root Field

Nên thêm field:

```text
root_path
```

Ví dụ:

```text
root_path = D:\Projects
```

giúp:

- rebuild one root;
- delete one root;
- root-scoped reconciliation.

---

# 104. Parent Hierarchy

Prefix deletion với raw StringField không tối ưu.

V1 có thể:

```text
query documents whose path begins with normalizedDirectory + separator
```

bằng suitable indexed path field.

Alternative đơn giản:

khi root configuration thay đổi:

```text
rebuild root
```

Đối với normal directory delete, watcher có thể scan Lucene path field.

---

# 105. Path Search Field

Nên phân biệt:

```text
path_exact
path_search
```

`path_search` tokenized theo separator.

Ví dụ:

```text
D:\Projects\DesktopSearch\src
```

tokens:

```text
projects
desktopsearch
src
```

---

# 106. Root Removal

Nếu user remove:

```text
D:\Projects
```

khỏi indexed roots.

Flow:

```text
stop watching root
    ↓
delete documents root_path = root
    ↓
commit
```

Không cần scan filesystem.

---

# 107. File Permission Error

Worker:

```text
readAttributes
```

có thể throw:

```text
AccessDeniedException
```

Policy:

```text
increment skipped/failed metric
log DEBUG/WARN
continue
```

---

# 108. File Locks

Windows application có thể lock file.

Metadata thường vẫn đọc được nhưng content extraction có thể fail.

Policy:

```text
metadata index succeeds
content extraction skipped
```

Có thể future retry.

---

# 109. Retry Strategy

V1 không retry vô hạn.

Possible transient errors:

```text
file temporarily locked
file being replaced
```

V1 có thể retry:

```text
1–2 times
```

với small delay.

Không dùng exponential retry phức tạp ban đầu.

---

# 110. Error Categories

Define:

```text
SKIPPED
TRANSIENT_ERROR
PERMANENT_ERROR
INDEX_ERROR
EXTRACTION_ERROR
```

để log/metric dễ hiểu.

---

# 111. Index Failure

Lucene write failure nghiêm trọng hơn file read failure.

Ví dụ:

```text
disk full
index corruption
IO failure
```

Nếu IndexWriter fail:

```text
indexing state -> FAILED
stop producing tasks
notify UI
```

Không tiếp tục spam errors.

---

# 112. Disk Full Handling

Nếu disk chứa Lucene index hết dung lượng:

```text
commit/write fails
```

UI cần hiển thị lỗi rõ:

```text
Indexing stopped: insufficient disk space
```

Không crash vô nghĩa.

---

# 113. Logging Levels

Examples:

```text
INFO
Indexing started root=D:\Projects

INFO
Initial scan completed discovered=225104 indexed=224998 failed=12

WARN
Content extraction failed path=...

DEBUG
Skipping excluded file path=...

ERROR
Lucene commit failed
```

Không log mỗi successful file ở INFO.

---

# 114. Metrics

Core counters:

```text
discovered
queued
processed
indexed
deleted
skipped
failed
queueSize
activeWorkers
```

Performance metrics:

```text
files/sec
average processing time
content extraction time
Lucene write time
commit duration
refresh duration
```

---

# 115. Index Throughput

Measure:

```text
indexed documents / second
```

separately for:

```text
metadata-only
full-text
```

Không benchmark hai mode chung vì performance rất khác.

---

# 116. Benchmark Datasets

Create synthetic datasets:

```text
10k files
100k files
500k files
1m files
```

File distribution:

```text
small text
source code
empty files
directories
mixed extensions
```

Full-text benchmark riêng với PDF/DOCX.

---

# 117. Worker Bottleneck Analysis

Pipeline:

```text
Metadata Read
    ↓
Extraction
    ↓
Mapping
    ↓
Lucene Write
```

Đo từng stage.

Không giả định Lucene là bottleneck.

Thực tế full-text extraction có thể là phần chậm nhất.

---

# 118. Content Extraction Pool Future

Nếu Tika extraction rất slow, có thể tách:

```text
IndexTask
   ↓
Metadata Workers
   ↓
Extraction Queue
   ↓
Extraction Workers
   ↓
Lucene Queue
   ↓
Index Writer
```

Nhưng V1 chưa cần.

Bắt đầu bằng một worker pipeline đơn giản.

Chỉ split khi benchmark chứng minh cần.

---

# 119. Initial V1 Concurrency Model

Recommended:

```text
Scanner Thread:       1

Index Worker Threads: 4

Watcher Thread:       1

Maintenance Thread:   1
```

Maintenance xử lý:

```text
refresh
commit
metrics
```

Có thể tách commit/refresh scheduler sau.

---

# 120. Shutdown Sequence

Graceful shutdown:

```text
Application shutdown requested
          │
          ▼
Stop watcher
          │
          ▼
Stop scanners
          │
          ▼
Stop accepting new tasks
          │
          ▼
Drain index queue
          │
          ▼
Stop workers
          │
          ▼
Refresh
          │
          ▼
Commit
          │
          ▼
Close SearcherManager
          │
          ▼
Close IndexWriter
          │
          ▼
Close Lucene Directory
```

---

# 121. Forced Shutdown

Nếu OS/process kill:

```text
latest uncommitted changes
```

có thể mất.

Nhưng previously committed Lucene index vẫn phải remain valid.

Startup rescan sẽ recover missing changes.

Đây là acceptable V1 tradeoff.

---

# 122. Crash Recovery Principle

Index là:

```text
derived data
```

Filesystem mới là source of truth.

Do đó nếu cần:

```text
index can always be rebuilt
```

Architecture không được coi Lucene là nơi duy nhất chứa critical user data.

---

# 123. Rebuild Index

Application cần command:

```text
Rebuild Index
```

Flow:

```text
stop indexing
    ↓
close Lucene resources
    ↓
delete/move index directory
    ↓
create new index
    ↓
scan configured roots
```

---

# 124. Rebuild One Root Future

V2:

```text
Reindex D:\Projects
```

không rebuild các root khác.

`root_path` field hỗ trợ feature này.

---

# 125. Configuration Model

Example:

```java
public record IndexConfiguration(
    int queueCapacity,
    int workerCount,
    Duration refreshInterval,
    Duration commitInterval,
    long maxExtractionBytes,
    boolean contentExtractionEnabled
) {}
```

---

# 126. Initial Defaults

```text
queueCapacity             = 5000
workerCount               = 4
refreshInterval           = 500 ms
commitInterval            = 10 sec
contentExtractionEnabled  = false initially
maxExtractionSize         = 50 MB
watchDebounce             = 300 ms
```

Các giá trị này là starting point, không phải production truth.

---

# 127. Package Structure

Recommended:

```text
com.desktopsearch.indexing
│
├── api
│   ├── IndexService.java
│   ├── IndexCoordinator.java
│   └── IndexStatus.java
│
├── task
│   ├── IndexTask.java
│   ├── UpsertFileTask.java
│   └── DeleteFileTask.java
│
├── worker
│   ├── IndexWorker.java
│   └── IndexWorkerPool.java
│
├── metadata
│   ├── FileMetadata.java
│   └── FileMetadataReader.java
│
├── filter
│   ├── FileFilter.java
│   ├── CompositeFileFilter.java
│   ├── ExcludedPathFilter.java
│   └── ExtractionEligibilityFilter.java
│
├── lucene
│   ├── LuceneIndexRepository.java
│   ├── LuceneDocumentMapper.java
│   ├── LuceneFieldNames.java
│   ├── LuceneIndexFactory.java
│   ├── IndexCommitManager.java
│   └── IndexRefreshManager.java
│
├── progress
│   ├── IndexProgress.java
│   └── IndexProgressTracker.java
│
└── error
    ├── IndexErrorHandler.java
    └── IndexingException.java
```

Scanner nằm module riêng:

```text
com.desktopsearch.scanner
```

Watcher:

```text
com.desktopsearch.watcher
```

Extractor:

```text
com.desktopsearch.extractor
```

---

# 128. Main Interfaces

## FileScanner

```java
public interface FileScanner {

    void scan(
        Path root,
        Consumer<Path> consumer
    );

}
```

---

## FileMetadataReader

```java
public interface FileMetadataReader {

    FileMetadata read(Path path)
        throws IOException;

}
```

---

## IndexCoordinator

```java
public interface IndexCoordinator {

    void indexRoots(Collection<Path> roots);

    void submit(IndexTask task);

    void shutdown();

}
```

---

## IndexRepository

```java
public interface IndexRepository {

    void upsert(
        FileMetadata metadata,
        ExtractionResult extraction
    );

    void delete(String normalizedPath);

    void commit();

}
```

---

## ContentExtractor

```java
public interface ContentExtractor {

    ExtractionResult extract(Path path);

}
```

---

# 129. Recommended Task Processing Algorithm

Pseudo-code:

```java
process(task) {

    switch (task) {

        case UpsertFileTask upsert -> {

            Path path = upsert.path();

            if (Files.notExists(path)) {
                repository.delete(normalize(path));
                return;
            }

            FileMetadata metadata =
                metadataReader.read(path);

            if (!filter.accept(path, metadata)) {
                repository.delete(metadata.id());
                return;
            }

            ExtractionResult extraction =
                extractionPolicy.shouldExtract(metadata)
                    ? extractor.extract(path)
                    : ExtractionResult.empty();

            repository.upsert(
                metadata,
                extraction
            );
        }

        case DeleteFileTask delete -> {
            repository.delete(
                normalize(delete.path())
            );
        }
    }
}
```

---

# 130. Important Property: Idempotency

Các operation phải cố gắng idempotent.

```text
UPSERT A
UPSERT A
UPSERT A
```

final result vẫn chỉ có:

```text
one A
```

Similarly:

```text
DELETE A
DELETE A
```

không lỗi.

Điều này cực kỳ quan trọng vì filesystem event có thể duplicate.

---

# 131. Eventual Consistency

Desktop Search không đảm bảo transaction đồng bộ tuyệt đối với filesystem.

Ví dụ file được tạo:

```text
T0: file created
T1: WatchService detects
T2: task queued
T3: Lucene updated
T4: search refresh
```

Trong khoảng `T0 → T4`, file có thể chưa searchable.

Target:

```text
sub-second to few seconds
```

là acceptable.

---

# 132. Index Consistency Contract

System guarantees:

```text
Eventually:
Lucene index reflects configured filesystem roots.
```

Không guarantee:

```text
every filesystem change instantly visible.
```

---

# 133. Testing Plan

## MetadataReader Test

Test:

```text
filename
extension
size
created time
modified time
directory
Unicode path
```

---

## FileFilter Test

Cases:

```text
excluded path
allowed path
large file
directory
hidden file
```

---

## DocumentMapper Test

Input:

```text
FileMetadata
```

Output:

Lucene document fields đúng.

---

## Repository Test

Test:

```text
upsert A
search A

upsert A modified
only one A exists

delete A
A unavailable
```

---

# 134. Queue Test

Queue capacity:

```text
10
```

Producer pushes 100 tasks.

Slow consumer.

Assert:

```text
producer blocks
memory remains bounded
```

---

# 135. Concurrency Test

Multiple workers:

```text
100 threads submit same file path
```

Final index:

```text
one document
```

---

# 136. Ordering Test

Scenario:

```text
UPSERT A
DELETE A
```

A is deleted from filesystem.

Even if UPSERT worker executes last:

```text
Files.notExists(A)
```

causes delete.

Final index correct.

---

# 137. Watcher Test

Temporary directory:

```text
create file
modify file
delete file
```

Assert eventual index state.

---

# 138. Restart Test

Flow:

```text
start
index 100 files
commit
shutdown
restart
```

Assert:

```text
100 files searchable
```

without initial clear/rebuild.

---

# 139. Crash Simulation

Index documents.

Do not graceful shutdown.

Restart repository.

Verify last committed index opens successfully.

Then rescan filesystem.

---

# 140. Large Dataset Test

Generate:

```text
1,000,000 dummy files
```

hoặc synthetic task objects nếu filesystem generation quá nặng.

Measure:

```text
throughput
heap
GC
queue depth
commit latency
```

---

# 141. Definition of Done — Indexing Engine V1

Indexing Engine được coi là hoàn thành khi:

1. Có thể recursive scan configured directory.
2. Không load toàn bộ path vào memory.
3. Sử dụng bounded task queue.
4. Có fixed-size workers.
5. File metadata được map thành Lucene document.
6. File được upsert theo normalized path.
7. File được delete khỏi Lucene.
8. Duplicate UPSERT không tạo duplicate document.
9. Searcher thấy update qua NRT refresh.
10. Không commit mỗi document.
11. Có periodic commit.
12. Graceful shutdown commit index.
13. Permission error không kill indexing.
14. File biến mất giữa lúc xử lý không gây crash.
15. Watcher event sử dụng cùng indexing pipeline.
16. MODIFY event được debounce.
17. Queue có backpressure.
18. Có progress metrics.
19. Application restart có thể mở index cũ.
20. Có manual rebuild mechanism.

---

# 142. Implementation Order

Không implement tất cả cùng lúc.

## Step 1

```text
FileMetadata
FileMetadataReader
PathNormalizer
```

---

## Step 2

```text
FileScanner
NioFileScanner
FileFilter
```

Test scan 100k file.

---

## Step 3

```text
LuceneIndexFactory
LuceneDocumentMapper
LuceneIndexRepository
```

Index metadata single-thread trước.

---

## Step 4

```text
Search simple filename
```

Chứng minh Lucene schema đúng trước khi thêm concurrency.

---

## Step 5

```text
IndexTask
BlockingQueue
IndexWorker
IndexCoordinator
```

Thêm producer-consumer.

---

## Step 6

```text
ProgressTracker
Cancellation
Graceful shutdown
```

---

## Step 7

```text
SearcherManager
RefreshManager
CommitManager
```

Near real-time indexing.

---

## Step 8

```text
WatchService
FileChangeEvent
Debouncer
```

Incremental indexing.

---

## Step 9

```text
Tika ContentExtractor
```

Sau khi metadata-only engine ổn định.

---

# 143. Critical Rule During Implementation

Không bắt đầu từ Tika.

Không bắt đầu từ JavaFX.

Không bắt đầu từ WatchService.

Core đầu tiên phải chứng minh được:

```text
Path
 ↓
Metadata
 ↓
Lucene Document
 ↓
IndexWriter
 ↓
Search
```

Sau đó mới thêm:

```text
Concurrency
 ↓
Watcher
 ↓
Content extraction
```

Như vậy khi có bug, phạm vi debug nhỏ hơn rất nhiều.

---

# 144. Final Indexing Architecture

Architecture mục tiêu của Indexing Engine V1:

```text
                     INITIAL INDEXING
                           │
                           ▼
                    ┌─────────────┐
                    │ FileScanner │
                    └──────┬──────┘
                           │ Path
                           ▼
                 ┌───────────────────┐
                 │   IndexTaskQueue  │
                 │     bounded       │
                 └─────────┬─────────┘
                           │
                ┌──────────┼───────────┐
                ▼          ▼           ▼
             Worker      Worker      Worker
                │          │           │
                └──────────┼───────────┘
                           │
                           ▼
                 ┌───────────────────┐
                 │ Metadata Reader   │
                 └─────────┬─────────┘
                           │
                           ▼
                 ┌───────────────────┐
                 │      Filter       │
                 └─────────┬─────────┘
                           │
                           ▼
                 ┌───────────────────┐
                 │ Content Extractor │
                 │     optional      │
                 └─────────┬─────────┘
                           │
                           ▼
                 ┌───────────────────┐
                 │  Document Mapper  │
                 └─────────┬─────────┘
                           │
                           ▼
                 ┌───────────────────┐
                 │   IndexWriter     │
                 └──────┬──────┬─────┘
                        │      │
                 refresh│      │commit
                        │      │
                        ▼      ▼
                     Search   Disk


                    LIVE INDEXING

Windows Filesystem
        │
        ▼
   WatchService
        │
        ▼
 Event Debouncer
        │
        ▼
    IndexTask
        │
        └────────────► same bounded indexing pipeline
```

---

# 145. Final Design Decisions

| Area | Decision |
|---|---|
| File traversal | `Files.walkFileTree` |
| Scan strategy | Streaming |
| Queue | Bounded `BlockingQueue` |
| Default capacity | 5000 |
| Producer | Scanner / Watcher |
| Consumers | Fixed worker pool |
| Worker count | Start with 4 |
| Unique identity | Normalized absolute path |
| Write strategy | UPSERT |
| Delete strategy | Delete by path |
| Index writer | Single shared `IndexWriter` |
| Search visibility | Near Real-Time refresh |
| Refresh | ~500 ms initial |
| Commit | Periodic ~10 sec |
| Shutdown | Graceful + final commit |
| Content extraction | Optional |
| Extractor | Apache Tika |
| Watch changes | Java `WatchService` |
| Rename | DELETE + CREATE |
| Event duplicate | Debounce + idempotent upsert |
| Queue overload | Backpressure |
| Filesystem race | Revalidate path in worker |
| Index recovery | Rescan/rebuild from filesystem |
| Source of truth | Windows filesystem |
| Lucene | Derived search state |

---

# 146. Conclusion

Indexing Engine V1 được thiết kế theo mô hình:

```text
Producer
    ↓
Bounded Queue
    ↓
Worker Pool
    ↓
Lucene
```

Hai producer chính:

```text
Initial File Scanner

File System Watcher
```

đều đẩy task vào cùng một indexing pipeline.

Điểm quan trọng nhất của thiết kế là:

```text
Filesystem is the source of truth.

Lucene is a searchable derived representation.
```

Indexing engine không cần đảm bảo mọi thay đổi filesystem được phản ánh tức thời.

Thay vào đó hệ thống hướng tới:

```text
eventual consistency
fast incremental updates
bounded resource usage
fault isolation
recoverability
```

V1 cố ý giữ pipeline tương đối đơn giản.

Chỉ khi benchmark cho thấy bottleneck mới tách thêm:

```text
metadata queue
extraction queue
writer queue
```

hoặc đưa indexing engine sang process riêng.

Với thiết kế hiện tại, Desktop Search có thể bắt đầu từ metadata-only indexing rất đơn giản, nhưng vẫn có nền kiến trúc đủ tốt để mở rộng tới full-text indexing và hàng triệu file mà không phải viết lại toàn bộ core.