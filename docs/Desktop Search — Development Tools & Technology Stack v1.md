# Desktop Search — Development Tools & Technology Stack v1

**Document:** Development Tools & Technology Stack  
**Version:** 1.0  
**Status:** Approved for Initial Implementation  
**Project:** Desktop Search  
**Target Platform:** Windows Desktop  
**Primary Language:** Java  
**Architecture:** Local Desktop Application / Modular Monolith

---

# 1. Purpose

Tài liệu này định nghĩa toàn bộ:

- development environment;
- IDE;
- programming language;
- runtime;
- build system;
- UI framework;
- indexing/search technology;
- filesystem APIs;
- persistence;
- content extraction;
- concurrency;
- logging;
- testing;
- benchmarking;
- profiling;
- packaging;
- version control;

được sử dụng trong Desktop Search.

Tài liệu đồng thời giải thích:

```text
Technology
    ↓
Purpose
    ↓
Why Selected
    ↓
Alternatives
    ↓
Final Decision
```

Mục tiêu là tránh việc lựa chọn công nghệ tùy ý trong quá trình implementation.

---

# 2. Technology Philosophy

Desktop Search là ứng dụng:

```text
local-first
desktop-native
single-user
performance-sensitive
filesystem-heavy
search-heavy
```

Do đó stack được ưu tiên theo:

1. Low runtime overhead.
2. Fast startup.
3. Low memory usage.
4. Native filesystem access.
5. Embedded components.
6. Không yêu cầu server riêng.
7. Dễ đóng gói thành Windows application.
8. Dễ debug và profiling.
9. Architecture đơn giản.
10. Có khả năng xử lý hàng trăm nghìn tới hàng triệu file.

Không lựa chọn công nghệ chỉ vì phổ biến trong web backend.

---

# 3. Final Stack Overview

```text
Desktop Search
│
├── Development
│   ├── Windows 11
│   ├── IntelliJ IDEA
│   ├── Git
│   └── GitHub
│
├── Runtime
│   ├── Java
│   └── OpenJDK
│
├── Build
│   └── Maven
│
├── Desktop UI
│   ├── JavaFX
│   └── CSS
│
├── Search
│   └── Apache Lucene
│
├── Filesystem
│   ├── Java NIO.2
│   ├── Files
│   ├── Path
│   └── WatchService
│
├── Content Extraction
│   └── Apache Tika
│
├── Persistence
│   ├── Apache Lucene Index
│   └── SQLite
│
├── Concurrency
│   └── Java Concurrency API
│
├── Logging
│   ├── SLF4J
│   └── Logback
│
├── Testing
│   ├── JUnit 5
│   └── Mockito
│
├── Benchmark
│   └── JMH
│
├── Profiling
│   ├── Java Flight Recorder
│   ├── Java Mission Control
│   └── IntelliJ Profiler
│
└── Distribution
    ├── jlink
    └── jpackage
```

---

# 4. Operating System

## Selected

**Windows 11**

## Purpose

Primary:

- development platform;
- runtime platform;
- filesystem integration target;
- packaging target;
- performance testing environment.

Desktop Search V1 được thiết kế trước tiên cho Windows.

## Why Selected

Ứng dụng hướng tới Windows filesystem và Windows desktop usage.

Một số hành vi cần test trực tiếp trên Windows:

- drive letters;
- NTFS;
- case-insensitive paths;
- hidden/system files;
- locked files;
- filesystem permissions;
- WatchService behavior;
- opening files/directories;
- application packaging.

## Alternatives

- Linux
- macOS

## Decision

```text
Primary target: Windows
```

Cross-platform support không phải requirement của V1.

Tuy nhiên core Java code nên tránh phụ thuộc Windows nếu không thực sự cần thiết.

---

# 5. Programming Language

## Selected

**Java**

## Recommended Version

**Java 21 LTS**

## Purpose

Java được sử dụng cho toàn bộ:

```text
UI
Indexing
Search
Filesystem
Concurrency
Persistence integration
Content extraction
Application lifecycle
```

## Why Java

Project được xây dựng nhằm khai thác Java ecosystem.

Java có sẵn:

- NIO filesystem API;
- mature concurrency primitives;
- Lucene native ecosystem;
- Apache Tika;
- JavaFX;
- JFR;
- jlink;
- jpackage.

Lucene bản thân cũng là Java library.

Do đó Java cho phép toàn bộ application chạy trong một JVM mà không cần IPC hoặc external service.

## Why Java 21

Java 21 là LTS release và cung cấp nền tảng hiện đại, ổn định cho desktop application.

Có thể sử dụng:

- records;
- sealed interfaces;
- pattern matching;
- modern switch;
- improved JVM/runtime;
- mature tooling.

## Alternatives

### Java 17

Ổn định nhưng cũ hơn.

### Kotlin

Syntax ngắn hơn nhưng thêm language/tooling layer không cần thiết.

### C#

Rất phù hợp Windows desktop nhưng project này định hướng Java.

### C++

Performance mạnh nhưng complexity và memory-safety cost cao hơn nhiều.

## Decision

```text
Language: Java
Baseline: Java 21 LTS
```

---

# 6. Java Distribution

## Selected

**OpenJDK-compatible JDK**

Recommended development distribution:

**Eclipse Temurin JDK 21**

## Purpose

Cung cấp:

```text
javac
java
jlink
jpackage
JFR
standard Java runtime
```

## Why Selected

Temurin:

- miễn phí;
- OpenJDK compatible;
- phổ biến;
- phù hợp development và redistribution scenarios;
- không tạo dependency vào Oracle commercial licensing.

## Alternatives

- Oracle JDK
- Amazon Corretto
- Microsoft Build of OpenJDK
- Azul Zulu

## Decision

Không phụ thuộc vendor-specific API.

Project target:

```text
Java 21 compatible JDK
```

Development default:

```text
Eclipse Temurin 21
```

---

# 7. IDE

## Selected

**IntelliJ IDEA**

Community hoặc Ultimate đều có thể sử dụng.

## Purpose

Primary development environment cho:

```text
Java coding
debugging
Maven
JUnit
Git
profiling
refactoring
dependency inspection
```

## Why Selected

IntelliJ IDEA có Java tooling mạnh:

- code completion;
- navigation;
- refactoring;
- debugger;
- Maven integration;
- JUnit integration;
- Git integration;
- profiler support;
- JavaFX support.

Project có nhiều module logic như:

```text
indexing
search
filesystem
lucene
watcher
```

nên khả năng navigation/refactoring tốt rất hữu ích.

## Alternatives

### Eclipse

Java IDE rất mạnh và mature.

Không có technical blocker nếu dùng Eclipse.

### Visual Studio Code

Nhẹ hơn nhưng Java desktop development experience không đồng nhất bằng full IDE.

### NetBeans

Java support tốt nhưng ecosystem/tooling ít phổ biến hơn IntelliJ hiện nay.

## Decision

```text
Primary IDE: IntelliJ IDEA
```

IDE không được tạo dependency vào source code.

Developer khác vẫn có thể build project hoàn toàn bằng Maven CLI.

---

# 8. Build Tool

## Selected

**Apache Maven**

## Purpose

Quản lý:

```text
dependencies
compilation
tests
plugins
packaging
build lifecycle
```

## Why Maven

Project dependency graph không quá phức tạp.

Maven cung cấp:

- predictable lifecycle;
- dependency management rõ ràng;
- Java ecosystem support mạnh;
- IntelliJ integration tốt;
- cấu hình dễ đọc;
- reproducible command-line builds.

Với project này, sự rõ ràng quan trọng hơn khả năng scripting build cực kỳ linh hoạt.

## Alternatives

### Gradle

Rất mạnh và flexible.

Ưu điểm:

- build scripts linh hoạt;
- incremental build mạnh;
- Kotlin DSL.

Nhưng flexibility này chưa tạo giá trị đáng kể cho Desktop Search V1.

### Maven Wrapper

Không phải alternative mà nên bổ sung để cố định Maven version.

## Decision

```text
Build Tool: Maven
```

Project nên sử dụng:

```text
mvnw
mvnw.cmd
```

để developer không cần cài đúng Maven version thủ công.

---

# 9. Dependency Management

Dependencies được khai báo duy nhất thông qua Maven.

Không manually copy:

```text
.jar
```

vào project.

Core dependencies dự kiến:

```text
JavaFX
Apache Lucene
Apache Tika
SQLite JDBC
SLF4J
Logback
JUnit
Mockito
JMH
```

Version cụ thể được khóa trong `pom.xml`.

Technology document không giữ patch version cụ thể để tránh nhanh outdated.

---

# 10. Application Architecture Technology

## Selected

**Plain Java Modular Monolith**

Không sử dụng application framework lớn cho V1.

Architecture:

```text
JavaFX UI
    ↓
Application Services
    ↓
Core Components
    ↓
Infrastructure
```

Tất cả chạy trong:

```text
one JVM process
```

---

# 11. Spring Framework / Spring Boot

## Decision

**Không sử dụng trong V1.**

## Why

Desktop Search không phải web server.

Không cần:

- HTTP server;
- REST controller;
- Spring MVC;
- Spring Security;
- cloud configuration;
- service discovery;
- web application context.

Spring Boot sẽ thêm:

- startup cost;
- memory overhead;
- dependency complexity;
- abstraction không cần thiết.

## Dependency Injection

V1 sử dụng:

```text
constructor injection manually
```

Ví dụ:

```java
var repository = new LuceneIndexRepository(...);

var coordinator =
    new DefaultIndexCoordinator(
        repository,
        metadataReader,
        fileFilter
    );
```

Nếu dependency graph trở nên phức tạp mới đánh giá DI framework.

---

# 12. Internal Communication

## Selected

**Direct Java method calls**

Không dùng:

```text
REST
HTTP
gRPC
RPC
```

giữa UI và indexing/search engine.

Architecture:

```text
JavaFX
  ↓
SearchService
  ↓
LuceneSearchRepository
```

không phải:

```text
JavaFX
  ↓ HTTP
Backend Server
  ↓
Lucene
```

## Why

Các component chạy cùng process.

REST chỉ tạo:

- serialization;
- HTTP overhead;
- port management;
- lifecycle complexity;
- error handling complexity.

Không có lợi ích đủ lớn cho V1.

---

# 13. UI Framework

## Selected

**JavaFX**

## Purpose

Desktop UI:

```text
Search window
Search box
Results
Filters
Settings
Index status
Indexed directories
```

## Why Selected

JavaFX:

- native Java ecosystem;
- chạy cùng JVM;
- CSS styling;
- observable UI model;
- background task integration;
- ListView/TableView virtualization;
- hỗ trợ packaging bằng jlink/jpackage.

Không cần embedded browser/runtime riêng.

## Alternatives

### Swing

Stable và mature nhưng UI model cũ hơn.

### Electron

UI ecosystem mạnh nhưng yêu cầu Chromium + Java backend integration hoặc viết phần lớn app bằng JS/TS.

Runtime footprint lớn hơn.

### Compose Desktop

Khá hấp dẫn nhưng thêm Kotlin-centric ecosystem.

### SWT

Native widgets tốt nhưng dependency/platform complexity cao hơn.

## Decision

```text
UI Framework: JavaFX
```

---

# 14. UI Styling

## Selected

**JavaFX CSS**

V1 không cần third-party UI framework.

Purpose:

```text
colors
spacing
fonts
hover states
result row styling
dark/light themes future
```

## Decision

Bắt đầu bằng standard JavaFX CSS.

Chỉ thêm library UI nếu thực sự cần component mà JavaFX thiếu.

---

# 15. Search Engine

## Selected

**Apache Lucene**

Đây là technology quan trọng nhất của project.

## Purpose

Lucene chịu trách nhiệm:

```text
indexing
text analysis
search
ranking
filtering
sorting
near-real-time search
```

## Why Selected

Lucene là embedded Java search library.

Application có thể:

```text
Java Process
   ↓
Lucene API
   ↓
Local Index
```

không cần server.

Lucene hỗ trợ:

- inverted index;
- BM25;
- TermQuery;
- PrefixQuery;
- FuzzyQuery;
- PhraseQuery;
- BooleanQuery;
- range queries;
- filters;
- sorting;
- analyzers;
- SearcherManager;
- Near Real-Time search.

## Alternatives

### Elasticsearch

Xây trên Lucene nhưng là distributed search server.

Desktop Search V1 không cần:

```text
cluster
REST API
distributed shards
network service
```

Running Elasticsearch cùng desktop application là quá nặng.

### OpenSearch

Tương tự Elasticsearch.

### SQLite FTS

Đơn giản nhưng search/ranking/analyzer flexibility thấp hơn Lucene cho mục tiêu project.

### Custom Search Engine

Không hợp lý.

Implement inverted index riêng là một project hoàn toàn khác.

## Decision

```text
Search Engine: Apache Lucene
```

Lucene được embedded trực tiếp vào application.

---

# 16. Lucene Storage

## Selected

**FSDirectory**

Lucene index được lưu local disk.

Conceptually:

```text
%LOCALAPPDATA%
    ↓
DesktopSearch
    ↓
index
```

## Why

Index phải tồn tại sau restart.

Không sử dụng RAMDirectory-like approach cho production index.

---

# 17. Lucene Search Model

Core Lucene APIs dự kiến:

```text
IndexWriter
Directory
Document
Analyzer
Query
IndexSearcher
SearcherManager
TopDocs
ScoreDoc
```

Indexing và search cùng chia sẻ:

```text
field definitions
analyzers
schema version
```

---

# 18. Filesystem API

## Selected

**Java NIO.2**

Primary APIs:

```text
java.nio.file.Path
java.nio.file.Files
java.nio.file.FileVisitor
java.nio.file.WatchService
java.nio.file.attribute.BasicFileAttributes
```

---

# 19. File Traversal

## Selected

```java
Files.walkFileTree(...)
```

## Purpose

Recursive directory traversal.

## Why

Cho phép streaming traversal.

Không cần giữ toàn bộ filesystem tree trong RAM.

Preferred:

```text
discover
   ↓
submit
   ↓
continue
```

thay vì:

```text
discover everything
   ↓
List<Path>
   ↓
process
```

---

# 20. File Change Monitoring

## Selected

**Java WatchService**

## Purpose

Detect:

```text
CREATE
MODIFY
DELETE
```

filesystem events.

## Why Selected

- built into JDK;
- no native dependency;
- đủ cho V1;
- integrates directly with indexing pipeline.

## Limitations

WatchService không được coi là absolute source of truth.

Có thể:

- overflow;
- miss events;
- cần recursive registration;
- behavior phụ thuộc OS.

Do đó application vẫn cần reconciliation strategy.

## Alternatives

### Windows native filesystem APIs

Có thể cung cấp khả năng sâu hơn nhưng yêu cầu JNI/JNA/native code.

### USN Journal

Rất hấp dẫn cho Windows-specific high-performance indexing nhưng complexity cao hơn đáng kể.

## Decision

```text
V1: Java WatchService
```

Windows USN Journal là candidate cho V2 nếu benchmark chứng minh WatchService không đủ.

---

# 21. Content Extraction

## Selected

**Apache Tika**

## Purpose

Extract text/metadata từ:

```text
PDF
DOC
DOCX
PPT
PPTX
XLS
XLSX
TXT
HTML
RTF
...
```

## Why Selected

Tika cung cấp abstraction thống nhất:

```text
file
  ↓
Tika
  ↓
text
```

thay vì application tự tích hợp:

```text
PDFBox
POI
HTML parser
RTF parser
...
```

riêng biệt.

## Alternatives

### Apache PDFBox

Tốt cho PDF nhưng không giải quyết các format khác.

### Apache POI

Tốt cho Microsoft Office nhưng không phải generic extraction framework.

### Custom extractors

Maintenance cost cao.

## Decision

```text
Content Extraction: Apache Tika
```

Tuy nhiên:

**Tika không nằm trong metadata-only MVP đầu tiên.**

Implementation order:

```text
metadata indexing
    ↓
stable
    ↓
Tika integration
```

---

# 22. Metadata Persistence

Search index không nên chứa mọi application configuration.

Cần persistence riêng cho:

```text
indexed roots
excluded paths
application settings
search history future
recent files future
index metadata
```

---

# 23. Application Database

## Selected

**SQLite**

## Purpose

Lưu application metadata nhỏ và structured.

Example tables future:

```text
indexed_root
excluded_path
app_setting
search_history
recent_file
```

## Why Selected

SQLite:

- embedded;
- serverless;
- single file;
- mature;
- low operational complexity;
- rất phù hợp desktop application.

## Alternatives

### H2

Java-native và embedded tốt.

Nhưng SQLite phổ biến hơn cho desktop local persistence và file database.

### PostgreSQL / MySQL

Không phù hợp vì yêu cầu database server.

### JSON files

Đủ cho config cực nhỏ nhưng khó query/evolve khi data model tăng.

## Decision

```text
Application Metadata DB: SQLite
```

---

# 24. SQLite Driver

Java sử dụng JDBC abstraction.

Architecture:

```text
Application
    ↓
JDBC
    ↓
SQLite JDBC Driver
    ↓
SQLite database
```

Driver cụ thể sẽ được khóa trong Maven dependency khi bootstrap.

Không sử dụng ORM trong V1.

---

# 25. Why No Hibernate/JPA

Data model SQLite của Desktop Search nhỏ.

Hibernate sẽ thêm complexity không cần thiết:

```text
entity lifecycle
persistence context
ORM mapping
proxy
configuration
```

V1 sử dụng:

```text
plain JDBC
repository classes
```

Ví dụ:

```text
IndexedRootRepository
SettingsRepository
```

---

# 26. Search Data vs Application Data

Hai storage có trách nhiệm khác nhau.

```text
Lucene
│
└── searchable filesystem representation

SQLite
│
└── application metadata/configuration
```

Không dùng SQLite để thay Lucene.

Không dùng Lucene để thay SQLite.

---

# 27. Concurrency

## Selected

**Java Concurrency API**

Primary components:

```text
ExecutorService
ScheduledExecutorService
BlockingQueue
ArrayBlockingQueue
AtomicLong
AtomicBoolean
ConcurrentHashMap
```

## Why

JDK đã cung cấp đủ primitive cần cho indexing/search pipeline.

Không cần reactive framework.

---

# 28. Indexing Concurrency

Architecture:

```text
Scanner
   ↓
BlockingQueue
   ↓
Fixed Worker Pool
   ↓
Lucene
```

Selected APIs:

```text
ArrayBlockingQueue
ExecutorService
```

---

# 29. Search Concurrency

Search chạy background executor.

Không chạy Lucene search trực tiếp trên:

```text
JavaFX Application Thread
```

Architecture:

```text
JavaFX Thread
    ↓
Search Executor
    ↓
Lucene
    ↓
JavaFX Thread
```

---

# 30. Virtual Threads

Java 21 có virtual threads.

Tuy nhiên V1 **không mặc định dùng virtual thread cho indexing engine**.

Lý do:

bottleneck chủ yếu là:

```text
disk IO
content parsing
Lucene indexing
```

và cần bounded concurrency để tránh saturate disk/CPU.

Virtual thread không thay thế backpressure.

Có thể benchmark sau.

---

# 31. Reactive Programming

## Decision

Không sử dụng:

```text
Project Reactor
RxJava
```

trong V1.

Producer-consumer model hiện tại đủ đơn giản với JDK concurrency primitives.

Reactive framework sẽ làm debugging/lifecycle phức tạp hơn mà chưa có lợi ích rõ.

---

# 32. Logging Abstraction

## Selected

**SLF4J**

## Purpose

Application code gọi logging abstraction thay vì implementation trực tiếp.

Example:

```java
private static final Logger log =
    LoggerFactory.getLogger(IndexWorker.class);
```

---

# 33. Logging Implementation

## Selected

**Logback**

## Purpose

- console logging during development;
- rolling file logs;
- configurable log level;
- troubleshooting production desktop builds.

## Why

SLF4J + Logback là combination mature và đơn giản.

## Alternatives

- java.util.logging
- Log4j2

## Decision

```text
Logging API: SLF4J
Logging backend: Logback
```

---

# 34. Logging Strategy

Levels:

```text
ERROR
WARN
INFO
DEBUG
TRACE
```

Examples:

```text
INFO
Indexing started

INFO
Indexing completed files=...

WARN
File access denied

ERROR
Lucene commit failed

DEBUG
File skipped by filter
```

Không log mỗi indexed file ở INFO.

---

# 35. Log Storage

Production logs dự kiến:

```text
%LOCALAPPDATA%
└── DesktopSearch
    └── logs
```

Dùng rolling policy để tránh log tăng vô hạn.

---

# 36. Unit Testing Framework

## Selected

**JUnit 5**

## Purpose

Test:

```text
PathNormalizer
FileMetadataReader
FileFilter
QueryParser
LuceneQueryBuilder
SearchRanking
configuration
```

## Why

Standard modern Java testing ecosystem.

---

# 37. Mocking Framework

## Selected

**Mockito**

## Purpose

Mock boundary dependencies khi unit test cần.

Ví dụ:

```text
SearchService
    ↓
mock SearchRepository
```

## Rule

Không mock mọi thứ.

Lucene repository nên có integration test với temporary real index thay vì mock toàn bộ Lucene API.

---

# 38. Integration Testing

Integration tests sử dụng:

```text
temporary directories
real filesystem
real Lucene index
real SQLite database where useful
```

Test important flows:

```text
create file
    ↓
index
    ↓
search
    ↓
modify
    ↓
reindex
    ↓
delete
```

---

# 39. Temporary Test Files

Use JUnit temporary directory support.

Ví dụ:

```java
@TempDir
Path tempDir;
```

Điều này đặc biệt quan trọng cho filesystem-heavy application.

---

# 40. Benchmark Framework

## Selected

**JMH**

Java Microbenchmark Harness.

## Purpose

Benchmark low-level operations:

```text
query building
analyzer performance
Lucene search
document mapping
selected indexing operations
```

## Why

Naive:

```java
long start = System.nanoTime();
```

loop benchmark có thể bị JVM warmup/JIT optimization làm sai.

JMH xử lý:

- warmup;
- forks;
- iterations;
- measurement.

---

# 41. Macro Benchmarking

JMH không đủ cho toàn application.

Cần custom benchmark runner cho:

```text
100k files
500k files
1m files
```

để đo:

```text
initial indexing duration
files/sec
heap usage
index size
search latency
```

Do đó:

```text
JMH = micro benchmarks

Custom benchmark suite = end-to-end performance
```

---

# 42. JVM Profiling

## Selected

**Java Flight Recorder — JFR**

## Purpose

Observe:

```text
CPU
allocations
GC
threads
locks
IO
exceptions
```

## Why

JFR được tích hợp sâu vào JVM và có overhead thấp.

Đây là tool quan trọng khi benchmark indexing engine.

---

# 43. Java Mission Control

## Selected

**Java Mission Control — JMC**

## Purpose

Phân tích JFR recordings.

Useful scenarios:

```text
Why indexing is slow?
Why heap grows?
Which method allocates most?
Are workers blocked?
Is GC excessive?
```

---

# 44. IntelliJ Profiler

Có thể sử dụng trong development cho quick profiling.

Nhưng performance conclusions quan trọng nên xác nhận bằng:

```text
JFR
JMC
JMH
repeatable benchmark
```

không chỉ nhìn IDE profiler một lần.

---

# 45. Memory Analysis

Khi cần:

```text
heap dump
```

có thể sử dụng:

- IntelliJ profiler;
- VisualVM;
- Eclipse MAT;

tùy tình huống.

Không cần dependency runtime.

---

# 46. Version Control

## Selected

**Git**

## Purpose

- source history;
- branches;
- code review;
- rollback;
- releases;
- tags.

---

# 47. Git Repository Hosting

## Selected

**GitHub**

## Purpose

Remote source repository.

Có thể sử dụng:

```text
Issues
Pull Requests
Actions future
Releases
```

## Alternatives

- GitLab
- Bitbucket

Không có architecture dependency vào GitHub.

---

# 48. Git Strategy

Với solo/small project:

```text
main
  ↑
feature/*
```

Không cần Git Flow phức tạp.

Examples:

```text
feature/indexing-core
feature/lucene-search
feature/file-watcher
feature/javafx-ui
```

---

# 49. Commit Strategy

Commits nên theo logical implementation steps.

Example:

```text
feat: initialize Maven JavaFX project

feat: add file metadata reader

feat: implement recursive file scanner

feat: add Lucene index repository

feat: implement filename search

feat: add bounded indexing queue
```

Tránh một commit chứa toàn bộ project.

---

# 50. Code Formatting

Primary formatting dựa trên IntelliJ Java formatter hoặc agreed Java style.

Quan trọng:

```text
formatting phải deterministic
```

Future có thể thêm:

- Spotless;
- Checkstyle;

nếu project cần automated enforcement.

Không bắt buộc ngay milestone đầu.

---

# 51. Static Analysis

Initial:

```text
IntelliJ inspections
javac warnings
```

Future candidate:

```text
SpotBugs
Checkstyle
PMD
```

Không add tất cả ngay từ đầu.

Rule:

```text
tool phải giải quyết vấn đề thực tế
```

không phải càng nhiều plugin càng tốt.

---

# 52. Lombok

## Decision

**Không sử dụng mặc định.**

## Why

Java hiện đại có:

```text
records
```

cho immutable data models.

Các core classes nên explicit constructor/dependency.

Lombok giảm boilerplate nhưng tạo compile-time magic không thực sự cần trong project này.

Nếu sau này có lý do đủ mạnh mới reconsider.

---

# 53. JSON Processing

V1 chưa cần heavy JSON usage.

Nếu cần:

```text
index-meta.json
```

hoặc import/export settings, có thể sử dụng Jackson.

Không thêm dependency cho đến khi feature cần.

SQLite nên lưu application config chính.

---

# 54. Configuration

V1 configuration có thể được load từ:

```text
SQLite
+
application defaults
```

Không cần Spring configuration system.

Runtime constants:

```text
queue capacity
worker count
commit interval
refresh interval
max extraction size
```

được represent bằng typed Java configuration objects.

---

# 55. Packaging

## Selected

**jlink + jpackage**

## Purpose

Biến Java application thành Windows desktop distribution.

---

# 56. jlink

`jlink` tạo custom Java runtime chỉ chứa module cần thiết.

Thay vì yêu cầu user:

```text
Install Java first
```

application có thể ship cùng runtime.

Benefits:

- predictable runtime;
- user không cần configure JAVA_HOME;
- tránh incompatible JDK;
- giảm runtime size so với full JDK.

---

# 57. jpackage

`jpackage` tạo native application package.

Target future:

```text
DesktopSearch.exe
```

và installer/package phù hợp Windows.

Có thể cấu hình:

```text
application icon
name
version
vendor
runtime image
```

---

# 58. Distribution Model

User experience mục tiêu:

```text
Download installer
      ↓
Install
      ↓
Desktop Search
      ↓
Run
```

không phải:

```text
install JDK
set JAVA_HOME
download jar
java -jar ...
```

---

# 59. Application Data Directory

Use:

```text
%LOCALAPPDATA%\DesktopSearch
```

Conceptual structure:

```text
DesktopSearch
│
├── index
│
├── database
│   └── desktop-search.db
│
├── logs
│
├── cache
│
└── runtime metadata
```

Exact structure được chốt khi bootstrap.

---

# 60. CI/CD

V1 local implementation chưa cần complex CI/CD.

Recommended future:

**GitHub Actions**

Pipeline:

```text
checkout
    ↓
setup Java
    ↓
mvn test
    ↓
package
```

Release pipeline future có thể build Windows package.

---

# 61. Containers

## Decision

**Docker không được sử dụng cho runtime Desktop Search V1.**

## Why

Application là:

```text
native local desktop app
```

Container làm filesystem/UI integration khó hơn.

Lucene và SQLite đều embedded.

Không có infrastructure service cần Docker.

Docker chỉ có thể dùng future cho auxiliary tooling nếu cần.

---

# 62. Network Dependencies

Core Desktop Search phải hoạt động:

```text
offline
```

Không yêu cầu:

- backend server;
- cloud database;
- remote search API;
- authentication service;
- internet connection.

Đây là deliberate architectural property.

---

# 63. Privacy Model

Filesystem index ở local machine.

```text
User files
    ↓
local extraction
    ↓
local Lucene index
```

Không upload content lên cloud trong V1.

Điều này giảm:

- privacy risk;
- network dependency;
- latency;
- infrastructure cost.

---

# 64. Technology Not Selected

V1 intentionally không sử dụng:

```text
Spring Boot
Spring MVC
REST internal API
Microservices
Docker runtime
Kubernetes
Elasticsearch
OpenSearch
PostgreSQL
MySQL
Redis
Kafka
RabbitMQ
Hibernate
JPA
Project Reactor
RxJava
Electron
Lombok
```

Không phải vì các công nghệ trên xấu.

Chúng chỉ không giải quyết requirement hiện tại tốt hơn stack đơn giản.

---

# 65. Why No Kafka

Indexing task queue nằm trong cùng JVM.

Need:

```text
BlockingQueue<IndexTask>
```

không cần distributed event broker.

Kafka sẽ tạo operational complexity cực lớn so với requirement.

---

# 66. Why No Redis

Application single process và local-first.

Không cần:

```text
distributed cache
distributed lock
remote key-value store
```

Lucene/search state nằm local.

---

# 67. Why No Microservices

Không có distributed deployment requirement.

Tách:

```text
indexing-service
search-service
ui-service
```

thành process/network services sẽ làm:

- startup phức tạp;
- deployment khó;
- IPC overhead;
- debugging khó;
- failure modes tăng.

Thay vào đó dùng modular monolith:

```text
one process
many clear modules
```

---

# 68. Why Modular Monolith

Architecture:

```text
Desktop Search JVM
│
├── UI Module
├── Indexing Module
├── Search Module
├── Scanner Module
├── Watcher Module
├── Extraction Module
├── Persistence Module
└── Shared Lucene Infrastructure
```

Boundaries vẫn rõ nhưng không cần network.

---

# 69. IDE Plugins

Không yêu cầu plugin proprietary bắt buộc.

Useful optional IntelliJ support:

```text
Maven
Git
JavaFX/CSS support
Database tools if available
```

Project phải build được dù không có optional IDE plugin.

---

# 70. Database Inspection Tool

SQLite có thể inspect bằng:

- IntelliJ database tooling nếu edition hỗ trợ;
- DBeaver;
- SQLite CLI;
- DB Browser for SQLite.

## Recommended

**DBeaver hoặc DB Browser for SQLite**

Tool này chỉ phục vụ development/debug.

Không phải runtime dependency.

---

# 71. Git GUI

Optional:

```text
IntelliJ Git integration
GitHub Desktop
Git CLI
```

Không có requirement bắt buộc.

Git CLI vẫn là canonical capability.

---

# 72. API Documentation

Core code documentation:

```text
Javadoc
README
architecture documents
```

Không cần Swagger/OpenAPI vì application không expose REST API.

---

# 73. Diagram Tool

Architecture diagrams có thể maintain bằng text-based format.

Recommended future:

```text
Mermaid
PlantUML
```

nếu repository cần diagrams-as-code.

Không bắt buộc runtime.

---

# 74. Documentation Format

Recommended:

```text
Markdown
```

Repository:

```text
docs/
```

Example:

```text
docs/
├── 01-system-design.md
├── 02-development-tools-and-technology-stack.md
├── 03-indexing-engine-design.md
├── 04-search-engine-design.md
├── 05-ui-ux-design.md
├── 06-implementation-plan.md
└── 07-testing-benchmark-plan.md
```

---

# 75. Dependency Direction

Technology choices phải tuân thủ architecture:

```text
UI
 ↓
Application
 ↓
Domain/Core
 ↓
Infrastructure
```

Core logic không nên phụ thuộc JavaFX.

Ví dụ:

```text
SearchService
```

không return:

```text
ObservableList
TableView
JavaFX Property
```

Nó return plain Java model.

---

# 76. Framework Isolation

External technology nên nằm ở boundary.

Examples:

```text
Lucene
  → lucene infrastructure package

SQLite
  → persistence package

Tika
  → extraction package

JavaFX
  → ui package
```

Không để Lucene `Document` chạy xuyên toàn application.

Không để SQLite `ResultSet` ra ngoài repository.

Không để Tika object xuất hiện trong search domain.

---

# 77. Version Policy

Dependency versions được quản lý trong:

```text
pom.xml
```

Rules:

1. Không dùng dynamic versions.
2. Không dùng snapshot dependency cho stable branch.
3. Major upgrade phải test index compatibility.
4. Lucene version change phải đặc biệt chú ý schema/index compatibility.
5. JavaFX version phải compatible Java baseline.
6. Tika security updates cần theo dõi.

---

# 78. Dependency Upgrade Strategy

Không update dependency chỉ vì version mới xuất hiện.

Upgrade khi:

```text
security fix
bug fix relevant
performance improvement relevant
required feature
supported runtime change
```

Mỗi major upgrade cần chạy:

```text
unit tests
integration tests
benchmark smoke test
```

---

# 79. Lucene Version Upgrade

Lucene đặc biệt quan trọng.

Major Lucene upgrade có thể ảnh hưởng:

```text
index format
analyzers
query behavior
ranking
API
```

Do đó index metadata phải có:

```text
schemaVersion
```

Nếu incompatible:

```text
rebuild index
```

Filesystem là source of truth nên rebuild luôn khả thi.

---

# 80. Security Considerations

Tika parse untrusted local documents.

Do đó:

- maintain supported Tika version;
- giới hạn file size;
- catch parser failures;
- không execute embedded content;
- avoid uncontrolled memory usage.

Search/index code không chạy file contents như code.

---

# 81. Resource Limits

Technology configuration phải hỗ trợ explicit limits:

```text
Index Queue
    → bounded

Worker Pool
    → bounded

Search Results
    → bounded

Extraction Size
    → bounded

Log Files
    → rolling / bounded
```

Đây là cross-cutting design rule.

---

# 82. Initial Development Configuration

Recommended starting configuration:

```text
Java                       21 LTS
Build                      Maven
IDE                        IntelliJ IDEA
UI                         JavaFX
Search                     Apache Lucene
Filesystem                 Java NIO.2
Filesystem Watch           WatchService
Content Extraction         Apache Tika
Application DB             SQLite
Concurrency                Java Executor API
Index Queue                ArrayBlockingQueue
Logging API                SLF4J
Logging Backend            Logback
Unit Test                  JUnit 5
Mocking                    Mockito
Microbenchmark             JMH
Profiler                   JFR + JMC
Packaging                  jlink + jpackage
Version Control            Git
Repository                 GitHub
```

---

# 83. Technology Introduction Order

Không add toàn bộ dependencies ngày đầu.

## Phase 1 — Foundation

```text
Java
Maven
JUnit
SLF4J
Logback
```

---

## Phase 2 — Filesystem

```text
Java NIO
```

Không cần external dependency.

---

## Phase 3 — Search Core

```text
Apache Lucene
```

---

## Phase 4 — Desktop UI

```text
JavaFX
```

Có thể làm sau console vertical slice.

---

## Phase 5 — Persistence

```text
SQLite JDBC
```

Khi indexed roots/settings cần persist.

---

## Phase 6 — Content Search

```text
Apache Tika
```

Sau metadata search stable.

---

## Phase 7 — Benchmark

```text
JMH
JFR
JMC
```

JFR có thể dùng sớm khi cần.

---

# 84. First Vertical Slice Technology Set

Feature đầu tiên:

```text
Given directory
    ↓
scan files
    ↓
index filename
    ↓
search keyword
    ↓
print results
```

chỉ cần:

```text
Java
Maven
Lucene
JUnit
SLF4J
Logback
```

Không cần:

```text
JavaFX
SQLite
Tika
WatchService
```

ngay lập tức.

Điều này giữ debugging scope nhỏ.

---

# 85. Second Vertical Slice

Sau core search:

```text
JavaFX
    ↓
SearchBox
    ↓
SearchService
    ↓
Lucene
    ↓
Result List
```

Lúc này mới có UI usable.

---

# 86. Third Vertical Slice

```text
WatchService
    ↓
CREATE / MODIFY / DELETE
    ↓
IndexTask
    ↓
Lucene
    ↓
NRT search update
```

---

# 87. Fourth Vertical Slice

```text
SQLite
    ↓
persist indexed roots/settings
```

Application restart nhớ directories cần index.

---

# 88. Fifth Vertical Slice

```text
Apache Tika
    ↓
document text
    ↓
Lucene content field
    ↓
full-text search
```

---

# 89. Technology Risk Matrix

| Technology | Risk | Notes |
|---|---|---|
| Java | Low | Mature runtime |
| Maven | Low | Mature build ecosystem |
| JavaFX | Low–Medium | Desktop UI ecosystem nhỏ hơn web |
| Lucene | Medium | Powerful API, cần hiểu analyzer/index lifecycle |
| WatchService | Medium–High | Windows event/reconciliation behavior cần test kỹ |
| Tika | Medium | File parsing có thể CPU/RAM heavy |
| SQLite | Low | Simple embedded persistence |
| JDK Concurrency | Medium | Race/backpressure/shutdown cần thiết kế cẩn thận |
| JFR/JMC | Low | Development tooling |
| jpackage | Medium | Windows packaging cần test riêng |

---

# 90. Highest Technical Risks

Ba technology area cần proof-of-concept sớm nhất:

```text
1. Lucene filename indexing/search quality

2. WatchService reliability at large directory scale

3. Tika performance/resource usage
```

Không cần POC cho:

```text
JUnit
SLF4J
SQLite basic CRUD
```

vì rủi ro thấp.

---

# 91. Technology Decision — Lucene vs Elasticsearch

Final:

```text
Lucene
```

Reason:

```text
Embedded
No server
Low operational overhead
Direct Java integration
NRT search
Strong query/ranking features
```

Elasticsearch giải quyết bài toán lớn hơn requirement của Desktop Search.

---

# 92. Technology Decision — JavaFX vs Electron

Final:

```text
JavaFX
```

Reason:

```text
One language
One JVM
Lower architecture complexity
Direct service invocation
No browser runtime
Better fit for Java-focused project
```

Electron có thể tạo UI ecosystem phong phú hơn nhưng không cần thiết cho V1.

---

# 93. Technology Decision — Maven vs Gradle

Final:

```text
Maven
```

Reason:

```text
Simple
Predictable
Explicit
Strong Java support
Enough for project requirements
```

Gradle không bị loại vì technical weakness; chỉ không cần flexibility của nó.

---

# 94. Technology Decision — SQLite vs H2

Final:

```text
SQLite
```

Reason:

```text
Embedded
File based
Widely used for desktop applications
Simple inspection/debugging
No server
```

---

# 95. Technology Decision — WatchService vs Native Windows API

Final V1:

```text
WatchService
```

Reason:

```text
Built into Java
No native bridge
Fast implementation
Enough to validate architecture
```

Future:

```text
USN Journal / Windows native watcher
```

nếu performance hoặc reliability yêu cầu.

---

# 96. Technology Decision — Plain Java vs Spring Boot

Final:

```text
Plain Java
```

Reason:

```text
Desktop local application
No HTTP server
No distributed services
Small dependency graph
Fast startup desirable
Low memory desirable
```

Constructor-based composition đủ cho V1.

---

# 97. Technology Decision — Platform Threads vs Virtual Threads

Final V1:

```text
bounded platform thread pools
```

cho indexing.

Reason:

```text
Need explicit concurrency limits
Disk and CPU must not be saturated
Worker count itself is part of performance tuning
```

Virtual threads vẫn có thể được thử nghiệm ở các IO task khác sau.

---

# 98. Technology Decision — JDBC vs ORM

Final:

```text
JDBC
```

Reason:

SQLite schema nhỏ và CRUD đơn giản.

ORM chưa mang lại đủ giá trị.

---

# 99. Toolchain Flow

Development flow:

```text
IntelliJ IDEA
     │
     ▼
   Java
     │
     ▼
   Maven
     │
     ├──────────────► JUnit / Mockito
     │
     ▼
 Application
     │
     ├── JavaFX
     ├── Lucene
     ├── Tika
     ├── SQLite
     └── Java NIO
     │
     ▼
   JFR/JMC
     │
     ▼
jlink / jpackage
     │
     ▼
Windows Application
```

---

# 100. Final Technology Stack

| Category | Selected Technology |
|---|---|
| Target OS | Windows 11 |
| Language | Java |
| Java Baseline | Java 21 LTS |
| JDK | OpenJDK / Eclipse Temurin |
| IDE | IntelliJ IDEA |
| Build | Maven + Maven Wrapper |
| Architecture | Modular Monolith |
| Application Framework | Plain Java |
| Dependency Injection | Manual constructor injection |
| UI | JavaFX |
| UI Styling | JavaFX CSS |
| Search Engine | Apache Lucene |
| Lucene Storage | FSDirectory |
| Filesystem | Java NIO.2 |
| File Traversal | Files.walkFileTree |
| Filesystem Monitoring | WatchService |
| Content Extraction | Apache Tika |
| Application DB | SQLite |
| Database Access | JDBC |
| ORM | None |
| Concurrency | Java Concurrency API |
| Index Queue | ArrayBlockingQueue |
| Logging | SLF4J + Logback |
| Unit Testing | JUnit 5 |
| Mocking | Mockito |
| Integration Testing | Real temporary FS/Lucene/SQLite |
| Microbenchmark | JMH |
| JVM Profiling | JFR |
| Profiling Analysis | JMC |
| Version Control | Git |
| Repository | GitHub |
| Packaging | jlink + jpackage |
| Runtime Container | None |
| Internal REST API | None |
| Web Server | None |

---

# 101. Final Stack Architecture

```text
┌────────────────────────────────────────────┐
│                 Windows                    │
│                                            │
│  ┌──────────────────────────────────────┐  │
│  │           Desktop Search             │  │
│  │                                      │  │
│  │              Java 21                 │  │
│  │                 │                    │  │
│  │        ┌────────┴────────┐           │  │
│  │        │                 │           │  │
│  │     JavaFX          Application      │  │
│  │        │              Services       │  │
│  │        │                 │           │  │
│  │        └────────┬────────┘           │  │
│  │                 │                    │  │
│  │      ┌──────────┼───────────┐        │  │
│  │      │          │           │        │  │
│  │    Lucene    Java NIO     SQLite     │  │
│  │      │          │           │        │  │
│  │      │      WatchService    │        │  │
│  │      │          │           │        │  │
│  │      └──── Apache Tika ─────┘        │  │
│  │                                      │  │
│  │       SLF4J + Logback                │  │
│  └──────────────────────────────────────┘  │
│                                            │
│            Local Filesystem                │
└────────────────────────────────────────────┘
```

---

# 102. Final Principles

Technology selection của Desktop Search V1 tuân theo:

```text
Embedded over Server

Local over Network

Standard JDK over Framework

Bounded over Unlimited

Simple over Distributed

Measured over Assumed

Modular over Microservice

Native Desktop over Internal Web Architecture
```

Mục tiêu không phải sử dụng nhiều công nghệ.

Mục tiêu là sử dụng **ít công nghệ nhất có thể nhưng đủ để giải quyết đúng bài toán**.

---

# 103. Conclusion

Desktop Search V1 sử dụng stack:

```text
Java 21
+
JavaFX
+
Apache Lucene
+
Java NIO / WatchService
+
SQLite
+
Apache Tika
```

với development toolchain:

```text
IntelliJ IDEA
+
Maven
+
Git / GitHub
+
JUnit / Mockito
+
JMH
+
JFR / JMC
+
jlink / jpackage
```

Không sử dụng Spring Boot, REST internal API, microservices hoặc external search/database server.

Toàn bộ application chạy local trong một JVM theo kiến trúc modular monolith.

Điều này giữ Desktop Search:

```text
fast
lightweight
offline-capable
debuggable
packageable
maintainable
```

đồng thời vẫn đủ nền tảng để xử lý indexing/search ở quy mô lớn.

Technology stack chỉ được mở rộng khi benchmark hoặc feature requirement chứng minh stack hiện tại chưa đủ.