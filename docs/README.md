# Desktop Search

A fast, local-first desktop file search application built with **Java, JavaFX, and Apache Lucene**, designed for efficient filesystem indexing and near real-time search.

> **Project Status:** Design & Initial Implementation  
> **Target Platform:** Windows  
> **Architecture:** Modular Monolith  
> **Java:** Java 21 LTS

---

## 1. Overview

Desktop Search là ứng dụng desktop cho phép tìm kiếm nhanh file và thư mục trên máy tính.

Thay vì duyệt filesystem mỗi lần người dùng tìm kiếm, application xây dựng một **local search index** và sử dụng index này để trả về kết quả với độ trễ thấp.

High-level flow:

```text
Windows Filesystem
        │
        ▼
   File Scanner
        │
        ▼
 Indexing Engine
        │
        ▼
 Apache Lucene
        │
        ▼
  Search Engine
        │
        ▼
    JavaFX UI
```

Project được thiết kế theo các nguyên tắc:

- Local-first
- Offline-capable
- Fast search
- Background indexing
- Bounded resource usage
- Clear module boundaries
- No internal REST API
- No external search server
- No unnecessary distributed infrastructure

---

# 2. Documentation

Tài liệu thiết kế nằm trong:

```text
docs/
```

Recommended structure:

```text
docs/
├── 01-system-design.md
├── 02-development-tools-and-technology-stack.md
├── 03-indexing-engine-design.md
├── 04-search-engine-design.md
├── 05-ui-ux-design.md
├── 06-project-bootstrap-and-implementation-plan.md
└── 07-testing-and-benchmark-plan.md
```

> Tên file thực tế có thể thay đổi, nhưng nên giữ prefix số để thể hiện thứ tự đọc.

---

# 3. Recommended Reading Order

Không nên đọc các tài liệu độc lập theo thứ tự ngẫu nhiên.

Recommended order:

```text
01. System Design
        │
        ▼
02. Development Tools & Technology Stack
        │
        ▼
03. Indexing Engine Design
        │
        ▼
04. Search Engine Design
        │
        ▼
05. UI/UX Design
        │
        ▼
06. Project Bootstrap & Implementation Plan
        │
        ▼
07. Testing & Benchmark Plan
        │
        ▼
    Implementation
```

Mỗi tài liệu được xây dựng dựa trên các quyết định từ tài liệu trước đó.

---

# 4. 01 — System Design

**Read this first.**

System Design cung cấp cái nhìn tổng thể về Desktop Search.

Đọc tài liệu này để hiểu:

- Project giải quyết vấn đề gì.
- Scope của V1.
- Kiến trúc tổng thể.
- Các module chính.
- Dependency direction.
- Data flow.
- Application lifecycle.
- Index lifecycle.
- Search lifecycle.
- Concurrency model ở mức high-level.
- Persistence strategy.
- Error handling strategy.
- Performance goals.

High-level architecture:

```text
┌──────────────────────┐
│      JavaFX UI       │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│ Application Services │
└──────────┬───────────┘
           │
     ┌─────┴─────┐
     ▼           ▼
 Search       Indexing
 Engine        Engine
     │           │
     └─────┬─────┘
           ▼
     Apache Lucene
           │
           ▼
      Local Index
```

Sau khi đọc xong, người đọc phải trả lời được:

> Desktop Search được chia thành những thành phần nào và chúng tương tác với nhau như thế nào?

Không cần hiểu chi tiết Lucene hoặc indexing worker ở bước này.

---

# 5. 02 — Development Tools & Technology Stack

**Read after System Design.**

Tài liệu này giải thích các công cụ và công nghệ được lựa chọn để implement architecture.

Main stack:

| Area | Technology |
|---|---|
| Language | Java 21 |
| JDK | OpenJDK / Eclipse Temurin |
| IDE | IntelliJ IDEA |
| Build | Maven |
| UI | JavaFX |
| Search | Apache Lucene |
| Filesystem | Java NIO.2 |
| File Monitoring | WatchService |
| Content Extraction | Apache Tika |
| Application DB | SQLite |
| Logging | SLF4J + Logback |
| Testing | JUnit 5 + Mockito |
| Benchmark | JMH |
| Profiling | JFR + JMC |
| Packaging | jlink + jpackage |
| Version Control | Git |
| Repository | GitHub |

Tài liệu không chỉ liệt kê technology mà còn giải thích:

```text
Technology
    ↓
Purpose
    ↓
Why Selected
    ↓
Alternatives
    ↓
Decision
```

Ví dụ:

```text
Lucene
    vs
Elasticsearch / OpenSearch / SQLite FTS
```

hoặc:

```text
JavaFX
    vs
Swing / Electron / Compose Desktop
```

Sau khi đọc xong, người đọc phải hiểu:

> Project sử dụng những công nghệ nào và tại sao chúng được chọn?

---

# 6. 03 — Indexing Engine Design

Đây là tài liệu đầu tiên đi sâu vào một core engine.

Indexing Engine chịu trách nhiệm biến filesystem thành searchable index.

Conceptual flow:

```text
Filesystem
    │
    ▼
File Scanner
    │
    ▼
File Discovery
    │
    ▼
Index Task
    │
    ▼
Bounded Queue
    │
    ▼
Index Workers
    │
    ├── Metadata Extraction
    │
    ├── Content Extraction
    │
    └── Document Mapping
    │
    ▼
Apache Lucene
```

Đọc tài liệu này để hiểu:

- Recursive filesystem scanning.
- File metadata.
- File filtering.
- Index task model.
- Producer-consumer architecture.
- Bounded queue.
- Backpressure.
- Worker pool.
- Lucene document mapping.
- IndexWriter lifecycle.
- Batch/commit strategy.
- Near Real-Time refresh.
- File CREATE/MODIFY/DELETE.
- WatchService.
- Reconciliation.
- Error handling.
- Graceful shutdown.

Sau khi đọc xong, người đọc phải trả lời được:

> Một file từ filesystem đi qua những bước nào trước khi có thể search được?

---

# 7. 04 — Search Engine Design

Search Engine Design mô tả chiều ngược lại của Indexing Engine.

Indexing:

```text
File
  ↓
Lucene Index
```

Search:

```text
User Query
    ↓
Lucene Index
    ↓
Search Results
```

Search flow:

```text
Raw Query
    │
    ▼
Query Parser
    │
    ▼
Search Request
    │
    ▼
Lucene Query Builder
    │
    ▼
IndexSearcher
    │
    ▼
Ranking
    │
    ▼
Search Result Mapping
    │
    ▼
SearchResponse
```

Đọc tài liệu này để hiểu:

- SearchRequest.
- Query parsing.
- Query normalization.
- Filename search.
- Prefix search.
- Fuzzy search.
- Full-text search.
- Filtering.
- Sorting.
- BM25/relevance.
- Result limits.
- SearcherManager.
- Near Real-Time search.
- Search concurrency.
- Search performance.
- Error handling.

Sau khi đọc xong, người đọc phải trả lời được:

> Một chuỗi người dùng nhập vào được biến thành kết quả search như thế nào?

---

# 8. 05 — UI/UX Design

Sau khi hiểu cả Indexing và Search Engine mới nên đọc UI.

UI không trực tiếp thao tác với Lucene.

Architecture:

```text
JavaFX View
     │
     ▼
ViewModel / Controller
     │
     ▼
Application Service
     │
     ├── SearchService
     ├── IndexedLocationService
     └── IndexService
```

Tài liệu mô tả:

- Main search window.
- Search bar.
- Search-as-you-type.
- Debounce.
- Result list.
- Result selection.
- Keyboard navigation.
- Filters.
- Sorting.
- Context menu.
- Indexed location settings.
- Index progress.
- Error states.
- Empty states.
- Rebuild flow.
- JavaFX threading rules.
- UI performance.
- Accessibility.

Primary UX:

```text
Launch
  ↓
Type
  ↓
Search
  ↓
↑ / ↓
  ↓
Enter
  ↓
Open File
```

Sau khi đọc xong, người đọc phải hiểu:

> User tương tác với Desktop Search như thế nào và UI giao tiếp với application core qua boundary nào?

---

# 9. 06 — Project Bootstrap & Implementation Plan

Tài liệu này là cầu nối giữa:

```text
DESIGN
   ↓
CODE
```

Không nên bắt đầu implementation lớn trước khi đọc tài liệu này.

Nó định nghĩa:

- Maven project bootstrap.
- Package structure.
- Initial dependencies.
- Module boundaries.
- Implementation milestones.
- Vertical slices.
- Development order.
- Git strategy.
- Definition of Done cho từng milestone.

Implementation không bắt đầu bằng việc tạo toàn bộ class trong architecture diagram.

Thay vào đó project được xây dựng incrementally.

First vertical slice:

```text
Directory
    ↓
File Scanner
    ↓
File Metadata
    ↓
Lucene Index
    ↓
Basic Search
    ↓
Console Result
```

Sau khi core hoạt động:

```text
Core Search
    ↓
JavaFX
    ↓
WatchService
    ↓
SQLite
    ↓
Content Search / Tika
```

---

# 10. 07 — Testing & Benchmark Plan

Desktop Search là performance-sensitive application.

Correctness thôi chưa đủ.

Project cần đo:

```text
Indexing throughput
Search latency
Memory usage
Index size
Concurrency behavior
Recovery behavior
```

Dataset targets:

```text
10K files
    ↓
100K files
    ↓
500K files
    ↓
1M files
```

Tài liệu này định nghĩa:

- Unit testing.
- Integration testing.
- Filesystem testing.
- Lucene integration testing.
- WatchService testing.
- Concurrency testing.
- Failure/recovery testing.
- JMH benchmarks.
- End-to-end benchmarks.
- JFR profiling.
- Performance acceptance criteria.

Sau khi đọc xong, người đọc phải hiểu:

> Làm thế nào để chứng minh Desktop Search vừa đúng vừa đủ nhanh?

---

# 11. Documentation Dependency Map

Các tài liệu không độc lập hoàn toàn.

```text
                    ┌─────────────────────┐
                    │    System Design    │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │ Tools & Tech Stack  │
                    └──────────┬──────────┘
                               │
                    ┌──────────┴──────────┐
                    ▼                     ▼
          ┌──────────────────┐   ┌──────────────────┐
          │ Indexing Engine  │   │  Search Engine   │
          └────────┬─────────┘   └────────┬─────────┘
                   │                      │
                   └──────────┬───────────┘
                              ▼
                     ┌────────────────┐
                     │  UI/UX Design  │
                     └───────┬────────┘
                             ▼
                  ┌─────────────────────┐
                  │ Implementation Plan │
                  └──────────┬──────────┘
                             ▼
                  ┌─────────────────────┐
                  │ Testing & Benchmark │
                  └──────────┬──────────┘
                             ▼
                       IMPLEMENTATION
```

---

# 12. Reading Paths

Không phải lúc nào cũng cần đọc toàn bộ documentation.

## New Developer

Đọc toàn bộ:

```text
System Design
    ↓
Technology Stack
    ↓
Indexing Engine
    ↓
Search Engine
    ↓
UI/UX
    ↓
Implementation Plan
    ↓
Testing Plan
```

---

## Working on Indexing

Recommended:

```text
System Design
    ↓
Technology Stack
    ↓
Indexing Engine Design
    ↓
Implementation Plan
    ↓
Testing & Benchmark Plan
```

---

## Working on Search

Recommended:

```text
System Design
    ↓
Indexing Engine Design
    ↓
Search Engine Design
    ↓
Implementation Plan
    ↓
Testing & Benchmark Plan
```

Indexing Design vẫn cần đọc vì Search Engine phụ thuộc vào Lucene schema/index structure.

---

## Working on UI

Recommended:

```text
System Design
    ↓
Search Engine Design
    ↓
UI/UX Design
    ↓
Implementation Plan
```

Không nên implement JavaFX trước khi hiểu `SearchRequest`, `SearchResponse` và indexing states.

---

## Working on Performance

Recommended:

```text
System Design
    ↓
Indexing Engine Design
    ↓
Search Engine Design
    ↓
Testing & Benchmark Plan
```

---

# 13. Important Architectural Rules

Trước khi contribute code, cần nắm các rule sau.

### Rule 1 — UI does not access Lucene directly

Bad:

```text
JavaFX
  ↓
IndexSearcher
```

Correct:

```text
JavaFX
  ↓
SearchService
  ↓
Search Engine
  ↓
Lucene
```

---

### Rule 2 — UI does not scan the filesystem

Bad:

```text
JavaFX Controller
    ↓
Files.walkFileTree()
```

Correct:

```text
JavaFX
    ↓
Indexing Service
    ↓
File Scanner
```

---

### Rule 3 — No internal REST API

Desktop Search V1 runs inside one JVM.

Use:

```text
Java method calls
```

not:

```text
HTTP / REST
```

between modules.

---

### Rule 4 — Never block JavaFX Application Thread

The following must run in background:

```text
Filesystem scan
Lucene search
Indexing
Tika extraction
Index rebuild
Heavy persistence operations
```

---

### Rule 5 — Do not load the entire filesystem into memory

Prefer streaming:

```text
scan
 ↓
emit
 ↓
process
```

instead of:

```text
scan everything
 ↓
List<Path>
 ↓
process
```

---

### Rule 6 — Resource usage must be bounded

Examples:

```text
Bounded queue
Bounded worker pool
Bounded search results
Bounded extraction size
Bounded logs
```

---

### Rule 7 — Filesystem is the source of truth

Lucene index is derived data.

Therefore:

```text
Corrupted Index
      ↓
Delete/Rebuild
      ↓
Filesystem
      ↓
New Index
```

User files must never depend on Lucene index survival.

---

# 14. Implementation Philosophy

This project intentionally avoids implementing everything at once.

Development follows:

```text
Understand
   ↓
Design
   ↓
Implement small slice
   ↓
Test
   ↓
Measure
   ↓
Review
   ↓
Expand
```

The first goal is **not** to build the final UI.

The first goal is to prove:

```text
Filesystem
    ↓
Lucene
    ↓
Search
```

works correctly.

---

# 15. Initial Implementation Roadmap

Recommended learning/implementation sequence:

```text
01. FileScanner
        ↓
02. FileMetadata
        ↓
03. MetadataReader
        ↓
04. Lucene Document Mapping
        ↓
05. Lucene Index Repository
        ↓
06. Basic Search
        ↓
07. Index Queue
        ↓
08. Index Workers
        ↓
09. JavaFX Search UI
        ↓
10. WatchService
        ↓
11. SQLite Settings
        ↓
12. Apache Tika Content Search
        ↓
13. Benchmark & Optimization
        ↓
14. Windows Packaging
```

Each step should be understood and tested before moving to the next.

---

# 16. Project Structure

Target repository structure:

```text
desktop-search/
│
├── docs/
│   ├── 01-system-design.md
│   ├── 02-development-tools-and-technology-stack.md
│   ├── 03-indexing-engine-design.md
│   ├── 04-search-engine-design.md
│   ├── 05-ui-ux-design.md
│   ├── 06-project-bootstrap-and-implementation-plan.md
│   └── 07-testing-and-benchmark-plan.md
│
├── src/
│   ├── main/
│   │   └── java/
│   │
│   └── test/
│       └── java/
│
├── pom.xml
├── mvnw
├── mvnw.cmd
├── .gitignore
└── README.md
```

The exact source package structure is defined by the implementation plan and evolves incrementally.

---

# 17. Current Project Status

```text
System Design                         ✅
Development Tools & Technology Stack  ✅
Indexing Engine Design                ✅
Search Engine Design                  ✅
UI/UX Design                          ✅

Project Bootstrap & Implementation    ⏳
Testing & Benchmark Plan              ⏳

Implementation                        🚧
```

---

# 18. Development Principle

> Do not implement a component before understanding why it exists.

Desktop Search is also a learning project.

The objective is not only to produce working code, but to understand the engineering decisions behind:

- filesystem traversal;
- indexing;
- search;
- concurrency;
- backpressure;
- persistence;
- desktop UI;
- profiling;
- performance optimization.

Code should therefore be implemented incrementally, reviewed, tested, and benchmarked rather than generated as one large block.

---

# 19. Quick Start for Contributors

Before writing code:

```text
1. Read System Design
2. Read Technology Stack
3. Read the design document for the component
4. Check the Implementation Plan
5. Implement one small unit
6. Write tests
7. Run and inspect behavior
8. Review the design assumptions
9. Commit
10. Continue to the next unit
```

For the first implementation milestone:

```text
Start with FileScanner.
```

Do not start with JavaFX, Tika, SQLite, or WatchService.

---

# 20. Summary

If you are new to the project, start here:

```text
README
   ↓
System Design
   ↓
Technology Stack
   ↓
Indexing Engine
   ↓
Search Engine
   ↓
UI/UX
   ↓
Implementation Plan
   ↓
Testing & Benchmark
   ↓
CODE
```

The documentation explains **why and how the system should work**.

The source code is the implementation of those decisions.