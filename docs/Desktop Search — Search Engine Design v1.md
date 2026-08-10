# Desktop Search — Search Engine Design v1

**Document:** Search Engine Design  
**Version:** 1.0  
**Parent document:** Desktop Search — System Design v1  
**Related document:** Indexing Engine Design v1  
**Status:** Draft  
**Target:** Windows Desktop Application  
**Language:** Java  
**Search Engine:** Apache Lucene

---

# 1. Purpose

Search Engine là component chịu trách nhiệm chuyển input của người dùng thành Lucene query, thực thi query trên local search index, ranking kết quả và trả về model phù hợp cho UI.

Luồng tổng quát:

```text
User Input
    ↓
Search Debounce
    ↓
SearchRequest
    ↓
Query Parser
    ↓
SearchQuery
    ↓
Lucene Query Builder
    ↓
IndexSearcher
    ↓
Ranking / Sorting
    ↓
SearchResult Mapper
    ↓
UI
```

Search Engine không đọc filesystem để tìm file.

Filesystem chỉ được dùng ở một số bước kiểm tra phụ như lazy stale cleanup hoặc open file.

Search thực tế phải dựa trên Lucene index.

---

# 2. Design Goals

Search Engine V1 phải đạt các mục tiêu:

1. Search filename nhanh.
2. Hỗ trợ search-as-you-type.
3. Exact match được ưu tiên cao.
4. Prefix/partial search hoạt động tốt.
5. Search không phân biệt hoa thường.
6. Hỗ trợ Unicode.
7. Hỗ trợ filter.
8. Hỗ trợ sorting.
9. Có pagination hoặc giới hạn result hợp lý.
10. UI không phụ thuộc Lucene API.
11. Search result mới index có thể xuất hiện qua Near Real-Time refresh.
12. Query parser có thể mở rộng.
13. Search không block JavaFX thread.
14. Query cũ không được overwrite query mới.
15. Search latency có thể benchmark rõ ràng.

---

# 3. Non-Goals V1

Search Engine V1 chưa cần:

- semantic search;
- embeddings;
- vector search;
- AI ranking;
- natural language understanding;
- typo correction phức tạp;
- OCR search;
- distributed search;
- remote query API;
- multi-user authorization;
- heavy autocomplete engine;
- machine-learned ranking.

Các phần này có thể bổ sung ở V2+.

---

# 4. High-Level Architecture

```text
┌──────────────────────────────────────────────┐
│                  JavaFX UI                   │
│                                              │
│ SearchBox                                    │
│ Filters                                      │
│ Sort Controls                                │
└──────────────────────┬───────────────────────┘
                       │
                       ▼
              ┌─────────────────┐
              │ SearchController│
              └────────┬────────┘
                       │
                       ▼
              ┌─────────────────┐
              │  SearchService  │
              └────────┬────────┘
                       │
           ┌───────────┴────────────┐
           ▼                        ▼
   ┌─────────────────┐      ┌──────────────────┐
   │   QueryParser   │      │ SearchRepository │
   └────────┬────────┘      └─────────┬────────┘
            │                         │
            ▼                         ▼
   ┌─────────────────┐       ┌──────────────────┐
   │   SearchQuery   │       │  IndexSearcher   │
   └────────┬────────┘       └─────────┬────────┘
            │                          │
            ▼                          │
   ┌─────────────────┐                 │
   │LuceneQueryBuilder│────────────────┘
   └────────┬────────┘
            │
            ▼
      Lucene Query
```

---

# 5. Core Components

Search Engine gồm:

```text
SearchService
SearchRequest
SearchResponse
SearchResult
SearchQuery
QueryParser
LuceneQueryBuilder
SearchRepository
SearchResultMapper
SearchSort
SearchFilter
SearchRankingPolicy
SearchSuggestionService      future
SearchHighlightService       later phase
```

---

# 6. SearchService

Đây là public application API chính của search module.

Interface đề xuất:

```java
public interface SearchService {

    SearchResponse search(SearchRequest request);

}
```

SearchService chịu trách nhiệm orchestration:

```text
SearchRequest
    ↓
parse
    ↓
build query
    ↓
execute
    ↓
map result
    ↓
SearchResponse
```

SearchService không chứa trực tiếp logic Lucene field-level.

---

# 7. SearchRequest

Đề xuất model:

```java
public record SearchRequest(
    String rawQuery,
    int limit,
    int offset,
    SearchSort sort
) {}
```

Ví dụ:

```text
rawQuery = "ext:java transaction"
limit    = 50
offset   = 0
sort     = RELEVANCE
```

---

# 8. SearchRequest Defaults

Nếu UI không truyền:

```text
limit
sort
```

thì application dùng default:

```text
limit = 50
sort  = RELEVANCE
```

Không nên cho query unlimited.

---

# 9. SearchResponse

```java
public record SearchResponse(
    List<SearchResult> results,
    long totalHits,
    long tookMillis,
    boolean hasMore
) {}
```

Có thể thêm:

```text
parsedQuery
warnings
```

sau này.

---

# 10. SearchResult

UI không được nhận Lucene `Document`.

Model đề xuất:

```java
public record SearchResult(
    String name,
    Path path,
    String extension,
    long size,
    Instant createdAt,
    Instant modifiedAt,
    boolean directory,
    float score
) {}
```

Sau này có thể thêm:

```text
highlight
mimeType
matchType
```

---

# 11. Search Query Layers

Phân biệt rõ 3 cấp:

```text
Raw Query
    ↓
Parsed Domain Query
    ↓
Lucene Query
```

Ví dụ:

```text
ext:pdf spring transaction
```

Raw string.

Parser tạo:

```text
keyword = "spring transaction"
extension = "pdf"
```

Lucene builder tạo BooleanQuery.

Không cho UI tự tạo Lucene Query.

---

# 12. SearchQuery Model

Đề xuất:

```java
public record SearchQuery(
    String keyword,
    String extension,
    String path,
    Long minSize,
    Long maxSize,
    Instant modifiedFrom,
    Instant modifiedTo,
    Boolean directoryOnly
) {}
```

V1 có thể chưa dùng tất cả field ngay.

---

# 13. Query Language V1

Query syntax mục tiêu:

```text
transaction
```

```text
ext:pdf transaction
```

```text
ext:java service
```

```text
path:projects spring
```

```text
size:>10MB report
```

```text
modified:today report
```

```text
type:dir project
```

---

# 14. Query Language Principles

Syntax phải:

- dễ đọc;
- dễ parse;
- không quá giống SQL;
- có thể mở rộng;
- raw keyword vẫn là use case chính.

Ví dụ user chỉ cần:

```text
transaction
```

chứ không phải:

```text
name contains transaction
```

---

# 15. QueryParser

Interface:

```java
public interface QueryParser {

    SearchQuery parse(String rawQuery);

}
```

QueryParser không phụ thuộc Lucene.

Điều này giúp unit test dễ.

---

# 16. Query Parsing Strategy

V1 chưa cần parser generator.

Có thể implement tokenization đơn giản:

```text
raw query
    ↓
split respecting quoted strings
    ↓
recognize filter tokens
    ↓
remaining tokens = keyword
```

Ví dụ:

```text
ext:pdf "spring transaction"
```

tokens:

```text
ext:pdf
spring transaction
```

---

# 17. Quoted Phrases

Nên hỗ trợ:

```text
"spring transaction"
```

nghĩa là phrase search.

Có thể represent trong SearchQuery bằng:

```text
keyword
phraseMode
```

Ví dụ:

```java
public record SearchText(
    String value,
    boolean phrase
) {}
```

V1 nếu muốn đơn giản có thể để phrase search ở phase 2.

---

# 18. Unknown Filters

Input:

```text
foo:bar transaction
```

Có hai lựa chọn:

1. Báo invalid query.
2. Coi `foo:bar` là keyword thường.

Đề xuất V1:

**Unknown field được coi là keyword thường.**

Lý do:

- search box không nên quá dễ fail;
- user có thể search filename có dấu `:` logic khác trên platform.

---

# 19. Invalid Filter Values

Ví dụ:

```text
size:abc
```

Không nên crash.

Có thể:

```text
ignore invalid filter
```

hoặc trả warning.

Đề xuất V1:

```text
SearchResponse.warnings
```

future.

MVP có thể fallback raw keyword.

---

# 20. Search Fields

Các field Lucene quan trọng:

```text
name
name_exact
name_prefix
path_search
path_exact
extension
size
created_at
modified_at
is_directory
content
```

`name_prefix` có thể được thêm nếu cần partial/prefix search tốt hơn.

---

# 21. Filename Search Problem

Filename khác text paragraph.

Ví dụ:

```text
TransactionService.java
spring_transaction_manager.md
desktop-search-design.pdf
```

User có thể search:

```text
transaction
```

```text
trans
```

```text
service
```

```text
desktop search
```

Analyzer phải hỗ trợ pattern này.

---

# 22. Filename Tokenization

Mục tiêu token hóa:

```text
TransactionService.java
```

thành:

```text
transaction
service
java
```

```text
spring_transaction_manager.md
```

thành:

```text
spring
transaction
manager
md
```

```text
desktop-search-design.pdf
```

thành:

```text
desktop
search
design
pdf
```

---

# 23. Filename Analyzer V1

Analyzer nên xử lý:

- lowercase;
- camelCase;
- snake_case;
- kebab-case;
- dots;
- whitespace;
- Unicode.

Conceptually:

```text
filename
   ↓
split delimiters
   ↓
split camelCase
   ↓
lowercase
```

Có thể xây bằng Lucene token filters hoặc preprocessing.

---

# 24. Exact Filename Field

Ngoài analyzed `name`, cần:

```text
name_exact
```

Ví dụ user search:

```text
pom.xml
```

Exact name match phải được boost cao.

`name_exact` được lowercase normalize cho lookup.

Display vẫn lấy `name` stored field.

---

# 25. Prefix Search

Search-as-you-type yêu cầu:

```text
tran
```

match:

```text
transaction
TransactionService.java
```

Có nhiều strategy.

---

# 26. PrefixQuery Strategy

Lucene hỗ trợ:

```text
PrefixQuery
```

Ví dụ:

```text
name token prefix "tran"
```

Ưu điểm:

- đơn giản;
- không tăng index size như edge-ngram.

Nhược điểm:

- query expansion có thể nặng với prefix quá ngắn.

Đề xuất V1:

**Dùng PrefixQuery với minimum prefix length.**

---

# 27. Minimum Prefix Length

Không chạy wildcard/prefix quá rộng với:

```text
a
```

hoặc:

```text
s
```

Đề xuất:

```text
prefix matching only when token length >= 2
```

hoặc 3 sau benchmark.

---

# 28. Exact + Token + Prefix Strategy

Với query:

```text
transaction
```

Search builder có thể tạo:

```text
SHOULD exact filename
SHOULD analyzed filename match
SHOULD prefix match
SHOULD path match
SHOULD content match
```

với boost khác nhau.

---

# 29. Initial Ranking Policy

Đề xuất logical boost:

```text
exact full filename      x12
exact analyzed token     x8
filename prefix          x6
filename normal match    x5
path match               x2
content match            x1
```

Đây chỉ là starting point.

Không coi các số là final.

---

# 30. Ranking Example

Query:

```text
transaction
```

Files:

```text
transaction
transaction.pdf
TransactionService.java
spring-transaction.md
notes.pdf
```

Trong đó `notes.pdf` có content chứa transaction.

Expected order gần:

```text
transaction
transaction.pdf
TransactionService.java
spring-transaction.md
notes.pdf
```

---

# 31. Extension Handling

User thường search:

```text
pdf
```

Có ambiguity:

- filename có từ pdf;
- extension = pdf.

Không tự coi bare token `pdf` là extension filter.

Extension filter chỉ khi:

```text
ext:pdf
```

---

# 32. Extension Filter Query

Lucene:

```text
TermQuery(extension, "pdf")
```

đặt trong:

```text
BooleanClause.FILTER
```

không ảnh hưởng relevance score.

---

# 33. Filter vs MUST

Các filter như:

```text
extension
size
modified date
directory flag
```

nên dùng Lucene FILTER clause.

Lợi ích:

- không contribute score;
- rõ semantic;
- có thể optimize caching.

---

# 34. Path Search

User:

```text
path:project spring
```

`path:` filter có thể hoạt động như contains/search chứ không exact.

Ví dụ:

```text
D:\Projects\DesktopSearch
```

match `project`.

---

# 35. Path Fields

Nên có:

```text
path_exact
path_search
```

`path_exact`:

- identity;
- delete/update.

`path_search`:

- tokenized;
- search/filter path.

---

# 36. Size Query

Syntax:

```text
size:>10MB
size:<1GB
```

Parser phải convert thành bytes.

Ví dụ:

```text
10MB -> 10 * 1024 * 1024
```

Có thể hỗ trợ:

```text
KB
MB
GB
TB
```

---

# 37. Size Range Query

Lucene dùng:

```text
LongPoint.newRangeQuery(...)
```

hoặc lower/upper range.

Ví dụ:

```text
size:>10MB
```

→

```text
minSize = 10485761
```

---

# 38. Modified Date Query

Examples:

```text
modified:today
modified:yesterday
modified:week
modified:>2026-08-01
```

V1 có thể chỉ hỗ trợ:

```text
today
yesterday
```

và ISO date.

Không cần tự nhiên quá phức tạp.

---

# 39. Timezone

Date filter phải dùng local timezone của desktop application.

Không interpret `today` theo UTC.

Convert local date range thành epoch millis trước Lucene query.

---

# 40. Directory Filter

Syntax:

```text
type:dir
```

hoặc:

```text
type:file
```

Domain:

```text
directoryOnly = true/false
```

Lucene:

```text
TermQuery(is_directory, "true")
```

---

# 41. Full-text Search

Khi `content` field được enable:

```text
keyword
```

có thể match:

```text
filename
path
content
```

Nhưng filename phải boost cao hơn content.

---

# 42. Metadata-only Mode

Trước khi Tika được implement:

```text
content field absent
```

Search builder vẫn hoạt động.

Không cần branch toàn bộ SearchService.

Chỉ bỏ content clause nếu content indexing chưa enable.

---

# 43. Multi-field Query Strategy

Query:

```text
spring
```

Có thể build:

```text
BooleanQuery SHOULD:
    name exact
    name analyzed
    name prefix
    path_search
    content
```

Minimum should match:

```text
1
```

---

# 44. Multi-word Query

Query:

```text
spring transaction
```

Có hai semantic options:

### OR

match file chứa spring hoặc transaction.

### AND

match cả hai.

Đề xuất V1:

**Default AND giữa user terms.**

Vì desktop search user thường mong kết quả cụ thể hơn.

---

# 45. Multi-word Example

Input:

```text
spring transaction
```

Logical:

```text
term spring
AND
term transaction
```

Mỗi term tự match trên:

```text
name/path/content
```

---

# 46. Why Default AND

Nếu OR:

```text
spring transaction
```

có thể trả hàng nghìn file chỉ có `spring`.

AND giúp:

- giảm noise;
- ranking dễ hiểu;
- desktop search precision tốt hơn.

Có thể thêm OR syntax sau.

---

# 47. Phrase Search

Future V1.1:

```text
"spring transaction"
```

sử dụng `PhraseQuery` trên content/name analyzed fields.

Không bắt buộc MVP.

---

# 48. Fuzzy Search

User typo:

```text
transction
```

vẫn muốn match:

```text
transaction
```

Lucene hỗ trợ `FuzzyQuery`.

Nhưng không nên bật fuzzy cho mọi token mặc định vì:

- query tốn hơn;
- result noise;
- prefix typing có thể conflict.

---

# 49. Fuzzy Strategy V1

Đề xuất:

Fuzzy chỉ dùng khi:

```text
normal exact/token/prefix search có ít kết quả
```

hoặc user bật explicit fuzzy future.

MVP có thể chưa cần automatic fuzzy.

---

# 50. Progressive Search Strategy

Future strategy:

```text
Phase 1:
exact + normal + prefix

if hits < threshold:
    Phase 2:
    fuzzy fallback
```

Threshold ví dụ:

```text
< 10 results
```

Nhưng cần benchmark.

---

# 51. Why Not Wildcard `*query*`

Query:

```text
*ransact*
```

substring wildcard có thể rất expensive.

Không dùng leading wildcard mặc định.

Nếu cần arbitrary substring:

- n-gram index;
- specialized field;
- alternative technique.

V1 tránh.

---

# 52. Partial Search Definition

Trong V1, "partial" chủ yếu nghĩa:

```text
prefix of token
```

Ví dụ:

```text
trans
```

→ transaction.

Không guarantee:

```text
ansac
```

→ transaction.

Đây là intentional scope.

---

# 53. Search-as-you-type

UI không gọi SearchService mỗi keydown ngay lập tức.

Flow:

```text
user types
    ↓
debounce 150–250 ms
    ↓
search
```

Initial proposal:

```text
200 ms
```

---

# 54. Minimum Query Length

Có thể không search khi input rỗng.

Query length:

```text
0
```

→ UI có thể show:

```text
recent files
search history
```

future.

V1 trả empty result.

---

# 55. Single-character Query

Một ký tự như:

```text
a
```

có thể match cực lớn.

Policy:

- allow exact token search;
- disable prefix expansion;
- result limited.

Không cần block hoàn toàn.

---

# 56. Query Cancellation

Search tasks chạy background.

User nhập:

```text
spring
```

rồi ngay:

```text
spring boot
```

Query cũ có thể finish sau.

Không được để result cũ overwrite result mới.

---

# 57. Query Sequence ID

UI/controller duy trì:

```text
AtomicLong querySequence
```

Ví dụ:

```text
#41 spring
#42 spring boot
```

Khi result #41 trả về:

```text
if sequence != latest
    discard
```

---

# 58. Thread Interruption

Không nhất thiết phải hard cancel Lucene query nhỏ.

V1 có thể:

```text
let old query finish
discard stale result
```

đơn giản và đủ hiệu quả nếu latency thấp.

---

# 59. Search Executor

Search không chạy JavaFX thread.

Có thể dùng:

```text
searchExecutor
```

fixed pool.

Initial:

```text
2 threads
```

hoặc:

```text
1–2 threads
```

Vì local user thường chỉ có một active search box.

---

# 60. Search Executor Queue

Không để backlog query từ mỗi keystroke.

Nếu UI debounce tốt thì queue nhỏ.

Có thể dùng executor strategy:

```text
latest query wins
```

future.

MVP:

```text
fixed executor + sequence discard
```

---

# 61. SearchRepository

Interface:

```java
public interface SearchRepository {

    SearchPage search(
        Query query,
        SearchSort sort,
        int limit,
        int offset
    );

}
```

`Query` ở đây là Lucene Query nội bộ module.

Có thể giữ type Lucene ở infrastructure package.

---

# 62. LuceneSearchRepository

Responsibilities:

```text
acquire IndexSearcher
execute search
collect TopDocs
load stored fields
release searcher
```

Không parse raw query.

---

# 63. SearcherManager

Search repository phải lấy searcher từ:

```text
SearcherManager
```

Flow:

```text
IndexSearcher searcher = manager.acquire();

try {
    ...
} finally {
    manager.release(searcher);
}
```

Đảm bảo NRT lifecycle đúng.

---

# 64. NRT Visibility

Indexing Engine refresh:

```text
every ~500 ms
```

Search Engine tự động nhìn thấy snapshot mới qua SearcherManager.

SearchService không gọi:

```text
IndexWriter.commit()
```

để "refresh search".

Commit và search visibility là hai khái niệm khác nhau.

---

# 65. Search Result Retrieval

Lucene query trả:

```text
ScoreDoc[]
```

Sau đó load stored fields cần thiết.

Không retrieve content raw nếu không cần.

---

# 66. Stored Fields

Search result cần:

```text
name
path_display
extension
size
created_at
modified_at
is_directory
```

Không cần load:

```text
content
```

---

# 67. Sorting

Supported sorts:

```java
public enum SearchSort {
    RELEVANCE,
    NAME_ASC,
    NAME_DESC,
    SIZE_ASC,
    SIZE_DESC,
    MODIFIED_NEWEST,
    MODIFIED_OLDEST
}
```

---

# 68. Relevance Sort

Default:

```text
RELEVANCE
```

Lucene score descending.

---

# 69. Name Sort

Cần:

```text
SortedDocValuesField
```

cho name normalized.

Không sort bằng stored string trực tiếp.

---

# 70. Numeric Sort

Size:

```text
NumericDocValuesField
```

Modified timestamp:

```text
NumericDocValuesField
```

---

# 71. Sort and Score

Khi sort theo:

```text
modified newest
```

score không quyết định order.

Có thể vẫn lấy score nếu UI cần, nhưng không bắt buộc.

---

# 72. Tie-breaker

Khi relevance bằng nhau, nên deterministic.

Có thể secondary sort:

```text
name
```

hoặc path.

Điều này tránh result nhảy thứ tự khó hiểu.

---

# 73. Pagination Strategy

Naive:

```text
offset + limit
```

Ví dụ:

```text
offset = 50000
limit = 50
```

Lucene phải collect nhiều result trước đó.

Không tối ưu deep pagination.

---

# 74. MVP Pagination

Desktop Search thường chỉ cần top results.

Đề xuất:

```text
max displayed results = 500
```

UI load incremental:

```text
50
100
150
...
```

Không cần deep pagination hàng chục nghìn result.

---

# 75. SearchAfter Future

Nếu cần pagination lớn:

```text
searchAfter
```

với last `ScoreDoc`.

V1 chưa cần expose cursor abstraction.

---

# 76. Search Limit

Hard safety cap:

```text
maxLimit = 500
```

Nếu request:

```text
limit=100000
```

clamp về max.

---

# 77. Total Hits

Lucene có thể track total hits.

Nhưng exact total với query rất lớn có thể tốn thêm.

V1 có thể dùng threshold.

UI không nhất thiết cần exact:

```text
10,000+ results
```

thay vì exact 153,281.

---

# 78. SearchResponse Example

```text
Query: spring transaction

Results: 73
Time: 24 ms
```

Nếu Lucene chỉ biết lower bound:

```text
1000+ results
```

acceptable.

---

# 79. Result Ranking Factors

V1 ranking chủ yếu dựa trên:

```text
field boost
Lucene BM25
exact match
prefix match
```

Không thêm manual recency score ngay.

---

# 80. Recency Ranking Future

Có thể boost recently modified file.

Nhưng nếu user search filename exact:

```text
pom.xml
```

không nên file mới hơn tự vượt exact logical result.

Do đó recency boost cần rất nhẹ nếu thêm.

V1 bỏ.

---

# 81. Recently Opened Ranking Future

Có thể dùng SQLite:

```text
recent_files
open_count
last_opened
```

rồi rerank top K.

Không nằm V1.

---

# 82. Ranking Architecture Future

```text
Lucene top 100
    ↓
Application reranker
    ↓
top 50
```

Có thể bổ sung sau mà không đổi public API.

---

# 83. Result Highlighting

Metadata filename highlight:

Query:

```text
transaction
```

Display:

```text
SpringTransactionService.java
      ^^^^^^^^^^^
```

Có thể highlight UI bằng simple case-insensitive substring/token mapping.

Không nhất thiết dùng Lucene Highlighter cho filename.

---

# 84. Content Highlighting

Khi full-text search:

```text
... declarative transaction management ...
```

Snippet generation phức tạp hơn.

V1 metadata search không cần.

Phase full-text có thể dùng Lucene unified highlighter.

---

# 85. Search Match Type

Future `SearchResult` có thể chứa:

```java
enum MatchType {
    EXACT_NAME,
    NAME,
    PATH,
    CONTENT
}
```

UI có thể show tại sao file match.

MVP không cần.

---

# 86. Search Empty Query

Input:

```text
""
```

V1 behavior:

```text
return empty result
```

Future có thể show:

```text
recent files
recent searches
pinned files
```

---

# 87. Directory Results

Directory được index.

Search result:

```text
Projects
DesktopSearch
Documents
```

Nếu result là directory:

Double click:

```text
open directory
```

Nếu file:

```text
open default application
```

---

# 88. Stale Search Results

Lucene có thể chứa file đã bị delete nhưng watcher chưa cập nhật.

Khi mapping result:

```java
Files.exists(path)
```

cho mọi result có thể gây extra filesystem I/O.

Không nên check toàn bộ nếu search latency quan trọng.

---

# 89. Lazy Stale Strategy

Đề xuất:

Search chỉ tin Lucene.

Khi user:

```text
open file
```

mà file không tồn tại:

```text
submit DeleteFileTask
show file unavailable
```

Có thể future opportunistic cleanup top result.

Không check `Files.exists` cho mọi search result trong V1.

---

# 90. Search Error Handling

Các error categories:

```text
INVALID_QUERY
INDEX_UNAVAILABLE
INDEX_CLOSED
SEARCH_IO_ERROR
INTERNAL_ERROR
```

SearchService nên trả/throw domain exception.

Không expose raw Lucene exception lên UI.

---

# 91. Search Exception

Ví dụ:

```java
public class SearchException extends RuntimeException {
}
```

hoặc typed errors.

UI hiển thị:

```text
Search index is temporarily unavailable.
```

không show stack trace.

---

# 92. Index Rebuilding State

Khi index đang rebuild:

Có thể search index cũ nếu chưa swap.

V1 đơn giản:

```text
search may temporarily be unavailable during full rebuild
```

Future có thể rebuild index mới rồi atomic swap.

---

# 93. Search During Initial Indexing

Một lợi thế NRT:

User không cần chờ scan hoàn tất.

Flow:

```text
indexing 200k files...
```

User search ngay.

Lucene trả phần đã index.

UI có thể hiển thị:

```text
Indexing in progress
```

---

# 94. Partial Index Semantics

Kết quả trong initial scan chưa guaranteed complete.

UI nên có trạng thái:

```text
Indexing...
128,432 files indexed
```

Search result vẫn usable.

---

# 95. Analyzer Compatibility

Analyzer dùng khi query phải compatible với analyzer dùng lúc index.

Nếu index `name` bằng FilenameAnalyzer:

Query term cũng phải qua cùng analyzer.

Không lowercase thủ công rồi tạo TermQuery bừa cho analyzed field.

---

# 96. Per-field Analyzer

Architecture:

```text
name        → FilenameAnalyzer
path_search → PathAnalyzer
content     → StandardAnalyzer
```

QueryBuilder phải biết analyzer tương ứng.

---

# 97. Exact Field Normalization

Exact fields:

```text
name_exact
extension
path_exact
```

không analyzed.

Application normalize trước:

```text
lowercase
path normalization
```

---

# 98. Filename Query Builder

For one keyword term:

```text
BooleanQuery SHOULD
```

Possible clauses:

```text
name_exact TermQuery boost 12
name analyzed query boost 8
name PrefixQuery boost 6
path_search query boost 2
content query boost 1
```

---

# 99. Whole Filename Exact Match

Input:

```text
pom.xml
```

normalize:

```text
pom.xml
```

check against:

```text
name_exact
```

boost cao.

---

# 100. Extension Suffix Search

Input:

```text
pom.xml
```

Analyzed `name` có thể tokenize:

```text
pom
xml
```

nhưng exact name ensures file này đứng đầu.

---

# 101. Query Term Building

Với keyword:

```text
spring transaction
```

parse thành terms:

```text
spring
transaction
```

Mỗi term build one field-disjunction.

Then combine:

```text
termGroup(spring) MUST
termGroup(transaction) MUST
```

---

# 102. Conceptual Lucene Query

```text
MUST (
    name:spring^8
    OR name:spring*^6
    OR path:spring^2
    OR content:spring
)

MUST (
    name:transaction^8
    OR name:transaction*^6
    OR path:transaction^2
    OR content:transaction
)

FILTER extension:java
```

---

# 103. Boost Configuration

Không hardcode sâu trong builder.

Config model:

```java
public record SearchRankingConfig(
    float exactNameBoost,
    float nameBoost,
    float prefixBoost,
    float pathBoost,
    float contentBoost
) {}
```

Defaults:

```text
exactName = 12
name      = 8
prefix    = 6
path      = 2
content   = 1
```

---

# 104. Query Complexity Limits

User có thể paste query cực dài.

Need caps.

Ví dụ:

```text
max raw query length = 512 or 1024 chars
```

V1 có thể clamp/reject.

Không cho hundreds of wildcard terms.

---

# 105. Too Many Terms

Input có thể có:

```text
1000 words
```

Lucene BooleanQuery có clause limits.

Parser nên giới hạn:

```text
max search terms = 20
```

hoặc tương tự.

Desktop filename search không cần 1000 terms.

---

# 106. Prefix Expansion Safety

Không prefix query:

```text
term length < 2
```

Không build hundreds of prefix clauses.

MultiTermQuery rewrite strategy có thể benchmark.

---

# 107. Fuzzy Safety

Nếu future fuzzy:

```text
minimum term length >= 4
maxEdits = 1 or 2
```

Không fuzzy một-letter token.

---

# 108. Query Cache

Lucene có internal caching mechanisms.

Application V1 không cần tự build:

```text
Map<String, SearchResponse>
```

vì index thay đổi liên tục và invalidation khó.

Không premature cache.

---

# 109. Search History

Search history thuộc persistence/app feature.

Không phải Search Engine core.

Có thể lưu:

```text
rawQuery
searchedAt
```

SQLite.

V1 optional.

---

# 110. Search Suggestions

Future suggestions có thể đến từ:

```text
search history
indexed filenames
popular extensions
```

Không nằm V1.

---

# 111. Search Performance Targets

Initial targets:

```text
100k docs   p95 < 100 ms
500k docs   p95 < 200 ms
1m docs     p95 < 500 ms
```

metadata search.

Các target này cần benchmark trên real hardware.

---

# 112. Search Latency Measurement

Measure riêng:

```text
parse time
query build time
Lucene search time
result mapping time
total time
```

Ví dụ:

```text
parse      0.2ms
build      0.5ms
search    13ms
mapping    3ms
total     17ms
```

---

# 113. Logging

Không log every search ở INFO nếu người dùng search-as-you-type.

Có thể:

```text
DEBUG query="spring" hits=45 took=18ms
```

Slow query:

```text
WARN slow_search queryLength=... took=650ms
```

Không log sensitive query content ở production nếu privacy concern.

---

# 114. Privacy

Search query có thể chứa:

```text
password
salary
private document name
```

Do đó V1 nên cân nhắc:

```text
do not log raw query at INFO
```

DEBUG mode có thể configurable.

---

# 115. Metrics

Metrics:

```text
searchCount
averageLatency
p50
p95
p99
zeroResultCount
queryLength
resultCount
```

Có thể log/benchmark trước, chưa cần metrics backend.

---

# 116. Benchmark Query Set

Need realistic queries:

```text
single exact filename
single prefix
common term
rare term
two-word AND
extension filter
path filter
size filter
modified filter
no result
```

---

# 117. Dataset Shape

Benchmark index:

```text
100k
500k
1m
```

with:

```text
Java source filenames
documents
images
random names
deep directories
Unicode filenames
```

---

# 118. Unicode Queries

Tests:

```text
thiết kế
hệ thống
日本語
設計書
```

Need correct lowercase/token behavior.

---

# 119. Vietnamese Search

V1 should search Vietnamese with diacritics exactly/analyzed.

Example:

```text
thiết kế hệ thống.pdf
```

Query:

```text
thiết kế
```

must match.

Accent-insensitive search:

```text
thiet ke
```

matching `thiết kế`

is a separate optional feature.

---

# 120. Accent Folding

Could use:

```text
ASCIIFoldingFilter
```

but it changes semantics across languages.

If used, index/query analyzer must be consistent.

V1 decision:

**Do not force accent folding initially.**

Benchmark/user testing first.

---

# 121. Case Insensitivity

All textual search should be case-insensitive.

Example:

```text
Transaction
transaction
TRANSACTION
```

same search semantics.

---

# 122. Search Repository Package

Recommended:

```text
com.desktopsearch.search
│
├── api
│   ├── SearchService.java
│   ├── SearchRequest.java
│   ├── SearchResponse.java
│   └── SearchResult.java
│
├── query
│   ├── SearchQuery.java
│   ├── QueryParser.java
│   ├── DefaultQueryParser.java
│   ├── LuceneQueryBuilder.java
│   └── SearchTextTokenizer.java
│
├── ranking
│   ├── SearchRankingConfig.java
│   └── SearchSort.java
│
├── lucene
│   ├── LuceneSearchRepository.java
│   ├── SearchResultMapper.java
│   └── LuceneSortFactory.java
│
└── error
    └── SearchException.java
```

---

# 123. SearchService Implementation Flow

Pseudo-code:

```java
public SearchResponse search(SearchRequest request) {

    validate(request);

    long start = System.nanoTime();

    SearchQuery parsed =
        queryParser.parse(request.rawQuery());

    Query luceneQuery =
        luceneQueryBuilder.build(parsed);

    SearchPage page =
        searchRepository.search(
            luceneQuery,
            request.sort(),
            request.limit(),
            request.offset()
        );

    long took =
        elapsedMillis(start);

    return new SearchResponse(
        page.results(),
        page.totalHits(),
        took,
        page.hasMore()
    );
}
```

---

# 124. Search Validation

Validate:

```text
rawQuery != null
limit > 0
limit <= max
offset >= 0
```

Raw empty query can return empty response instead of exception.

---

# 125. SearchResultMapper

Maps:

```text
Lucene Document
ScoreDoc
```

to:

```text
SearchResult
```

No UI-specific object.

---

# 126. Lucene Field Constants

Do not repeat strings:

```java
"name_exact"
"path_exact"
```

everywhere.

Use:

```java
public final class LuceneFieldNames {

    public static final String NAME = "name";
    public static final String NAME_EXACT = "name_exact";
    ...

}
```

Shared between indexing/search Lucene infrastructure.

---

# 127. Shared Lucene Schema Module

Because Indexing Engine and Search Engine both depend on field names/analyzers, create internal shared package:

```text
com.desktopsearch.lucene
```

containing:

```text
LuceneFieldNames
LuceneAnalyzerFactory
LuceneSchemaVersion
```

Avoid circular dependency:

```text
search → indexing
```

or:

```text
indexing → search
```

Both depend on shared Lucene infrastructure.

---

# 128. Dependency Direction

```text
UI
 ↓
Search API
 ↓
Search Application Logic
 ↓
Lucene Search Infrastructure
 ↓
Shared Lucene Infrastructure
```

Indexing:

```text
Indexing Logic
 ↓
Lucene Index Infrastructure
 ↓
Shared Lucene Infrastructure
```

Search và Indexing không gọi business logic của nhau.

---

# 129. Search During Refresh

`maybeRefresh()` có thể xảy ra concurrent search.

SearcherManager handles safe snapshot switching.

Search query đang chạy tiếp tục trên old valid searcher.

Next query sees newer snapshot.

Đây là desired NRT behavior.

---

# 130. Search During Commit

Commit không nên block application ở logic level.

Lucene handles write internals, nhưng disk contention có thể tăng latency.

Benchmark commit interval với search p95.

---

# 131. Commit/Search Contention Metrics

Benchmark:

```text
search latency while idle
search latency while indexing
search latency during commit
```

Desktop app cần responsive ngay cả khi indexing background.

---

# 132. Indexing Priority

Nếu initial indexing saturates CPU/disk, search latency có thể xấu.

Possible future mitigation:

```text
lower indexing thread count
pause heavy extraction while user active
priority queues
```

V1 trước tiên dùng bounded workers và benchmark.

---

# 133. Search Priority Over Indexing

Product principle:

```text
interactive search > background indexing throughput
```

Nếu phải trade-off:

- search phải responsive;
- indexing có thể chậm hơn.

---

# 134. UI Behavior

Search flow:

```text
TextField change
    ↓
debounce
    ↓
background SearchService
    ↓
result arrives
    ↓
sequence check
    ↓
Platform.runLater(...)
    ↓
update list
```

---

# 135. Loading Indicator

Không nên flash spinner với query 10ms.

UI chỉ show loading nếu query vượt threshold.

Ví dụ future:

```text
> 150 ms
```

MVP có thể không cần spinner.

---

# 136. Result Limit UI

Default:

```text
50 results
```

If more:

```text
Show more
```

or virtualized list.

JavaFX ListView hỗ trợ cell virtualization tương đối tốt.

---

# 137. Sorting UI

Controls:

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

Không cần expose ascending/descending mọi kiểu ngay.

---

# 138. Filter UI vs Query Syntax

Có hai cách cùng tồn tại:

```text
ext:pdf
```

và dropdown extension.

UI filter có thể convert thành SearchRequest structured filters future.

V1 raw syntax trước.

---

# 139. Future Structured SearchRequest

Sau này:

```java
public record SearchRequest(
    String rawQuery,
    SearchFilters filters,
    SearchSort sort,
    int limit
) {}
```

Parser chỉ xử lý inline filters.

UI filters merge với parsed filters.

---

# 140. Conflict Resolution

Ví dụ:

Raw:

```text
ext:pdf spring
```

UI extension dropdown:

```text
java
```

Need rule.

Future recommendation:

**Explicit UI filter overrides inline query** hoặc reject conflict.

Không cần solve trong initial V1 nếu chưa có filter UI.

---

# 141. Query Normalization

Before parsing:

```text
trim
collapse unnecessary whitespace
```

Không lowercase entire raw query blindly nếu quoted/display semantics sau này cần.

Analyzer handles textual normalization.

---

# 142. Special Characters

Lucene parser syntax như:

```text
+
-
:
(
)
```

không được expose trực tiếp nếu không dùng Lucene QueryParser.

Custom QueryParser sẽ tránh việc user input vô tình trở thành Lucene syntax injection.

---

# 143. Do Not Use Raw Lucene QueryParser Initially

Không làm:

```java
new QueryParser("name", analyzer)
    .parse(userInput);
```

trực tiếp với toàn bộ raw input.

Lý do:

- user input có special syntax;
- field control khó;
- ranking khó custom;
- query language của app sẽ phụ thuộc Lucene syntax.

Better:

```text
our parser
    ↓
our model
    ↓
programmatic Lucene Query
```

---

# 144. Search Query Security

Local app nên không có SQL injection problem.

Nhưng raw Lucene query syntax vẫn có thể tạo:

- expensive wildcard;
- too many clauses;
- parse failures.

Custom builder giới hạn query complexity tốt hơn.

---

# 145. Zero Result Behavior

If no results:

```text
0 results
```

Future fuzzy fallback có thể chạy.

MVP chỉ trả empty.

UI có thể suggest:

```text
Try a shorter query
```

nhưng không cần engine-specific.

---

# 146. Result Deduplication

Mỗi filesystem path có unique `path_exact`.

Indexing upsert guarantee một document/path.

Search không cần deduplicate thường xuyên.

Nếu duplicates xuất hiện thì đó là indexing bug cần fix, không mask ở search layer.

---

# 147. Search Correctness Tests

Test:

```text
exact filename
token filename
prefix
case-insensitive
extension filter
path filter
size range
date range
directory filter
multi-word AND
```

---

# 148. Exact Ranking Test

Dataset:

```text
transaction
transaction.pdf
my-transaction-notes.pdf
notes.pdf(content transaction)
```

Assert exact filename/file-name matches rank before content-only.

---

# 149. Prefix Test

Dataset:

```text
TransactionService.java
TransferService.java
```

Query:

```text
tran
```

Expected both if token prefix semantics permit:

```text
TransactionService.java
TransferService.java
```

Ranking may depend on token.

---

# 150. Multi-word Test

Dataset:

```text
spring-transaction.pdf
spring-security.pdf
transaction-notes.pdf
```

Query:

```text
spring transaction
```

Expected:

```text
spring-transaction.pdf
```

only under default AND.

---

# 151. Extension Filter Test

Query:

```text
ext:java service
```

Should not return:

```text
service.pdf
```

---

# 152. Sort Tests

Test:

```text
NAME_ASC
SIZE_DESC
MODIFIED_NEWEST
```

with deterministic expected order.

---

# 153. NRT Test

Flow:

```text
index A
no commit
refresh
search A
```

A must be searchable.

This proves refresh != commit.

---

# 154. Concurrency Search Test

Run:

```text
indexing background
multiple searches
refresh scheduler
```

Assert:

- no closed searcher errors;
- no corrupted results;
- no deadlock.

---

# 155. Performance Test

For each dataset:

```text
100k
500k
1m
```

run:

```text
1000 representative queries
```

capture:

```text
p50
p95
p99
max
```

---

# 156. Search Benchmark Modes

Benchmark separately:

```text
idle index
active metadata indexing
active full-text indexing
```

Because system load differs.

---

# 157. Definition of Done — Search Engine V1

Search Engine V1 hoàn thành khi:

1. Search filename hoạt động.
2. Exact filename được boost cao.
3. Case-insensitive.
4. Multi-word default AND.
5. Prefix search hoạt động.
6. Extension filter hoạt động.
7. Path filter hoạt động.
8. Size filter hoạt động.
9. Modified date filter hoạt động.
10. Directory/file filter hoạt động.
11. Relevance sorting hoạt động.
12. Name sorting hoạt động.
13. Size sorting hoạt động.
14. Modified sorting hoạt động.
15. Result limit hoạt động.
16. UI không phụ thuộc Lucene.
17. Search chạy background.
18. Query cũ không overwrite query mới.
19. SearcherManager được acquire/release đúng.
20. NRT refresh cho phép search document mới chưa commit.
21. Query invalid không crash app.
22. Có unit tests parser/builder.
23. Có integration tests Lucene.
24. Có benchmark cơ bản 100k+ documents.

---

# 158. Implementation Order

## Step 1

Create shared Lucene infrastructure:

```text
LuceneFieldNames
AnalyzerFactory
schema constants
```

---

## Step 2

Implement:

```text
SearchResult
SearchRequest
SearchResponse
SearchSort
```

---

## Step 3

Implement simple:

```text
SearchQuery
QueryParser
```

chỉ raw keyword.

---

## Step 4

Implement:

```text
LuceneQueryBuilder
```

filename exact + token.

---

## Step 5

Implement:

```text
LuceneSearchRepository
SearcherManager acquire/release
```

---

## Step 6

Implement:

```text
SearchService
```

End-to-end keyword search.

---

## Step 7

Add:

```text
prefix query
ranking boosts
```

---

## Step 8

Add filters:

```text
ext
path
size
modified
type
```

---

## Step 9

Add sorting.

---

## Step 10

Add:

```text
search-as-you-type debounce
sequence cancellation
```

ở UI/application layer.

---

## Step 11

Benchmark and tune:

```text
boosts
prefix minimum length
result limit
analyzers
```

---

# 159. Recommended MVP Search Feature Set

MVP search nên chỉ cần:

```text
keyword filename search
exact match boost
prefix match
extension filter
relevance sort
modified sort
50 result limit
search-as-you-type
```

Không cần hoàn thành toàn bộ query syntax trước khi UI usable.

---

# 160. Search V1.1 Candidates

Sau MVP:

```text
path filter
size filter
date filter
directory filter
phrase search
fuzzy fallback
highlight
full-text search
```

---

# 161. Search V2 Candidates

Future:

```text
semantic search
vector index
hybrid keyword + vector
recent-file reranking
search history suggestions
accent-insensitive search
plugin search sources
OCR results
```

---

# 162. Hybrid Search Future

Architecture future:

```text
                  Query
                    │
          ┌─────────┴─────────┐
          ▼                   ▼
      Lucene BM25         Vector Search
          │                   │
          └─────────┬─────────┘
                    ▼
                  Fusion
                    ↓
                  Ranker
                    ↓
                 Results
```

Current SearchService abstraction nên đủ để mở rộng tới strategy này sau.

---

# 163. Architectural Boundary

Public API vẫn:

```text
SearchService.search(SearchRequest)
```

Do đó UI không quan tâm phía dưới là:

```text
Lucene only
```

hay future:

```text
Lucene + vector engine
```

Đây là lý do phải giữ search engine encapsulated.

---

# 164. Important Design Decisions

| Area | Decision |
|---|---|
| Search backend | Apache Lucene |
| Query parsing | Custom parser |
| Raw Lucene syntax | Không expose |
| Default word semantics | AND |
| Exact filename | High boost |
| Prefix search | Có |
| Leading wildcard | Không |
| Fuzzy | Không mặc định |
| Content search | Optional phase |
| Filters | Lucene FILTER clauses |
| Default sort | Relevance |
| Default limit | 50 |
| Deep pagination | Không ưu tiên |
| NRT | SearcherManager |
| Search execution | Background executor |
| UI stale query handling | Sequence ID |
| Query cache | Không custom V1 |
| Case sensitivity | Case-insensitive |
| Unicode | Supported |
| Accent folding | Không mặc định |
| Search source of truth | Lucene index |

---

# 165. Final Search Architecture

```text
                        USER
                         │
                         ▼
                  ┌──────────────┐
                  │ Search Input │
                  └──────┬───────┘
                         │
                    debounce
                         │
                         ▼
                  ┌──────────────┐
                  │SearchRequest │
                  └──────┬───────┘
                         │
                         ▼
                  ┌──────────────┐
                  │ QueryParser  │
                  └──────┬───────┘
                         │
                         ▼
                  ┌──────────────┐
                  │ SearchQuery  │
                  └──────┬───────┘
                         │
                         ▼
               ┌───────────────────┐
               │LuceneQueryBuilder │
               └─────────┬─────────┘
                         │
                         ▼
                 ┌───────────────┐
                 │SearcherManager│
                 └───────┬───────┘
                         │
                         ▼
                  ┌─────────────┐
                  │IndexSearcher│
                  └──────┬──────┘
                         │
                  TopDocs / Score
                         │
                         ▼
                ┌──────────────────┐
                │Result Mapper     │
                └────────┬─────────┘
                         │
                         ▼
                ┌──────────────────┐
                │ SearchResponse   │
                └────────┬─────────┘
                         │
                         ▼
                       UI
```

---

# 166. Final Interaction with Indexing Engine

```text
                   INDEXING SIDE

Filesystem
    ↓
Scanner / Watcher
    ↓
Index Pipeline
    ↓
IndexWriter
    ↓
SearcherManager Refresh
               │
               │
               ▼
          SEARCH SIDE
               │
               ▼
          IndexSearcher
               ↓
          SearchResult
               ↓
               UI
```

Indexing Engine và Search Engine gặp nhau ở:

```text
Lucene IndexWriter
SearcherManager
Shared Lucene Schema
```

nhưng business logic của hai module vẫn tách biệt.

---

# 167. Core Search Principle

Search Engine V1 được thiết kế theo nguyên tắc:

```text
Query fast.
Rank predictably.
Limit expensive operations.
Keep Lucene behind an application boundary.
```

User input không trực tiếp điều khiển Lucene syntax.

Search phải ưu tiên interactive latency hơn việc chạy những query quá thông minh hoặc quá rộng.

---

# 168. Conclusion

Search Engine V1 tập trung vào một search experience đơn giản nhưng mạnh:

```text
type keyword
    ↓
results appear quickly
```

Phía dưới, query được xử lý qua:

```text
custom parser
field-aware matching
exact boost
prefix matching
filters
Lucene BM25
NRT search
```

Architecture này đủ đơn giản để implement sớm nhưng vẫn có đường mở rộng rõ ràng cho:

```text
full-text search
fuzzy search
highlight
recent-file ranking
semantic search
hybrid search
```

mà không cần phá API giữa UI và search core.

Search Engine V1 cùng với Indexing Engine V1 tạo thành hai core module chính của Desktop Search:

```text
Filesystem
    ↓
INDEXING ENGINE
    ↓
Lucene
    ↓
SEARCH ENGINE
    ↓
JavaFX UI
```

Sau hai module này, project đã đủ rõ để bắt đầu implementation thực tế.