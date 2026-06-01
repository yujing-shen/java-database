# Java Relational Database Engine

## 项目亮点（简历 / 面试速览）

纯 Java 实现的**轻量级关系型数据库服务端**（无 JDBC/SQLite），适合作为**解决方案工程师 / AI 产品经理**类岗位的可演示个人项目：

- **端到端交付**：TCP 客户端–服务端（8888）+ 命令解析 + 磁盘持久化（TSV）+ JUnit 自动化测试
- **需求与契约清晰**：统一 `[OK]` / `[ERROR]` 响应；强制分号、引号/括号校验；须先 `USE` 再操作表
- **可讲的设计取舍**：自定义分词器、`WHERE` 递归条件求值、嵌套循环 JOIN、自动 `id` 主键；明确不做事务/索引（见下文 Limitations）
- **质量保障**：单元测试（Tokenizer）+ 多步场景测试（建库 → 插数 → 改表结构 → 复杂查询）

---

A **client–server database engine** built from scratch in Java (no JDBC/SQLite). Users send SQL-style commands over TCP; the server parses them, executes DDL/DML, and persists data as TSV files on disk.

> **Resume angle (Solution Engineer / AI PM):** End-to-end product slice—protocol, validation, core logic, persistence, and automated regression tests—useful for discussing requirements, trade-offs, demos, and quality gates without claiming production DB scale.

---

## Highlights (what to say in an interview)

| Theme | What this project shows |
|--------|-------------------------|
| **End-to-end delivery** | Runnable server + CLI client + file persistence + JUnit suite |
| **Requirements → behaviour** | Clear `[OK]` / `[ERROR]` contract; commands must end with `;` |
| **Defensive design** | Input checks (quotes, brackets, reserved names), `USE` before table ops, auto `id` column |
| **Test-driven quality** | Unit tests (`Tokenizer`) + scenario harness (`DB-tests`) for full workflows |
| **Trade-off awareness** | Simple nested-loop join, no transactions/indexes—documented under [Limitations](#limitations) |

---

## Architecture

```text
DBClient  ──TCP:8888──►  DBServer.handleCommand()
                              │
                              ├─ syntax guards (;, quotes, brackets)
                              ├─ Tokenizer  →  List<String> tokens
                              └─ DatabaseEngine.executeCommand()
                                      │
                                      ├─ ConditionEvaluator  (WHERE: AND/OR, parentheses)
                                      └─ StorageManager      (load/save *.tab)
                                              │
                                              ▼
                                    databases/<db>/<table>.tab
```

| Component | Role |
|-----------|------|
| `DBServer` | Networking, `handleCommand` entry point |
| `Tokenizer` | Splits commands into tokens; protects quoted strings |
| `DatabaseEngine` | Dispatches CREATE / USE / INSERT / SELECT / … |
| `Table` / `Row` | In-memory schema and rows |
| `StorageManager` | Reads/writes tab-separated `.tab` files |
| `ConditionEvaluator` | Recursively evaluates `WHERE` conditions |

**Request flow (example):** `USE demo` → set `currentDatabase` in memory → `INSERT` loads `demo/marks.tab`, appends row, saves file.

---

## Features (accurate to implementation)

- **Custom command language** — SQL-like syntax tailored to coursework spec (not full ANSI SQL).
- **Tokenizer** — Handles glued tokens (`WHERE(age>20)`), keeps string literals intact (`'Steve Jobs'`).
- **WHERE evaluation** — Recursive split on top-level `AND` / `OR` outside parentheses (`ConditionEvaluator`).
- **Schema** — `CREATE TABLE` auto-adds an `id` column; users cannot declare `id` manually.
- **Persistence** — Database = folder; table = `.tab` file (header row + data rows, tab-separated).
- **Inner join** — Nested-loop join; output columns prefixed as `table.column`.
- **ALTER TABLE** — `ADD` pads existing rows with `NULL`; `DROP` removes columns.

---

## Supported commands

| Category | Commands |
|----------|----------|
| Database | `CREATE DATABASE`, `USE`, `DROP DATABASE` |
| Table | `CREATE TABLE`, `DROP TABLE`, `ALTER TABLE … ADD`, `ALTER TABLE … DROP` |
| Data | `INSERT INTO … VALUES`, `SELECT … FROM … [WHERE …]`, `UPDATE … SET … [WHERE …]`, `DELETE FROM … [WHERE …]` |
| Relational | `JOIN table1 AND table2 ON col1 AND col2` |

**Response format:** `[OK]` or `[ERROR] …`; `SELECT` / `JOIN` also return tab-separated result tables.

**Rules worth knowing for demos:**

- Every command must end with `;` (checked in `DBServer`).
- `USE <database>` must run before table operations (except `CREATE DATABASE`).
- `INSERT` values do not include `id` — the engine assigns incrementing ids.

---

## Quick demo

```bash
chmod +x mvnw    # once, on macOS/Linux
./mvnw exec:java@server   # terminal 1
./mvnw exec:java@client   # terminal 2
```

```sql
CREATE DATABASE demo;
USE demo;
CREATE TABLE marks (name, mark, pass);
INSERT INTO marks VALUES ('Ann', 70, TRUE);
INSERT INTO marks VALUES ('Bob', 55, FALSE);
SELECT * FROM marks;
SELECT name FROM marks WHERE (mark > 60) AND (pass == TRUE);
```

On disk: `databases/demo/marks.tab` (runtime data; not committed to git).

---

## Project layout

```text
src/main/java/edu/uob/   DBServer, DatabaseEngine, Tokenizer, Table, Row, StorageManager, …
src/test/java/edu/uob/   JUnit tests + DB-tests scenario harness
databases/               Runtime storage (gitignored)
```

---

## Prerequisites & build

- Java 17+
- Maven (wrapper included: `./mvnw`)

```bash
./mvnw test              # run all tests
./mvnw exec:java@server  # start server on port 8888
./mvnw exec:java@client  # interactive client
```

Tests can also call `DBServer.handleCommand(String)` directly (no socket)—see `ExampleDBTests`.

---

## Testing

JUnit 5 coverage includes:

- **Tokenizer** — spacing, quotes, operators (`TokenizerTests`)
- **Engine** — CRUD, schema changes, joins (`DBServerTests`, `DB-tests/*`)
- **Scenarios** — multi-step flows via `DBServerHarness` (create DB → insert → query → alter)

```bash
./mvnw test
```

---

## Limitations (intentional scope)

- No JDBC, no SQL standard compliance, no query planner
- Single-threaded blocking server (one client connection handled sequentially in the sample design)
- No indexes, transactions, `COMMIT`/`ROLLBACK`, or concurrent write safety
- String literals use simple single-quote rules (no escape sequences)
- Join is **O(n×m)** nested loop

Stating these clearly is stronger for Solution / PM interviews than overselling “production-grade.”

---

## Possible extensions (product thinking)

- [ ] Indexes (B-tree) for faster `SELECT` on large tables
- [ ] WAL + transactions for crash-safe multi-step operations
- [ ] Connection pooling / thread-per-request for concurrent clients
- [ ] Structured logging and metrics for operability demos

---

## Tech stack

Java 17 · Maven · JUnit 5 · `java.net` sockets · flat-file (TSV) storage

---

## Course context

Developed as a university coursework project (University of Bristol) implementing a specified DB server protocol and test suite. The codebase demonstrates parsing, state management, and file I/O without external database libraries.
