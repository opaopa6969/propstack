# PropStack — アーキテクチャ

[English](architecture.md) | [Japanese (日本語)](architecture-ja.md)

---

## 概要

PropStack は 2 つの直交する関心事を持つ:

| モジュール | 責務 |
|-----------|------|
| `PropStack` | `List<PropertySource>` によるカスケード型プロパティ解決 |
| `Registry` | `ConcurrentHashMap` によるアプリケーションスコープのコンポーネント管理 |

両者は独立している。`PropStack` は文字列を読む。`Registry` はオブジェクトを管理する。アプリケーションが両者を繋ぐ。

---

## PropStack — スタック構造

### List\<PropertySource\>

`PropStack` は順序付きの `List<PropertySource>` を保持する。解決は **first-match-wins**: リストを順に走査し、最初に見つかった非空の値を返す。

```
sources[0]  インメモリ上書き         (set() 呼び出し)          ← 最優先
sources[1]  SystemProperties        (-D フラグ)
sources[2]  EnvironmentVariables    (OS 環境変数)
sources[3]  ~/.appName/app.props    (ユーザーホーム、任意)
sources[n]  classpath app.props     (JAR 内デフォルト)        ← 最低優先
```

リストは plain な `java.util.ArrayList`。挿入用 API は別途設けない — `PropStack.defaultSources(appName)` で取得し、標準の `List` 操作で操作する（DD-006）。

### PropertySource インターフェース

```java
public interface PropertySource {
    Optional<String> getRawValue(String key);
    PropertySource set(String key, String value);
    Set<String> keys();
    default Optional<String> get(String key) { ... }  // VariableExpander を適用
}
```

組み込みソースはすべて静的ファクトリメソッドが返す匿名内部クラス（`systemProperties()`、`environmentVariables()`、`fromPath()`、`fromClasspath()`、`of(Map)`、`fromArgs()`）。

**注意**: `fromPath()` は存在しないファイルを **サイレントに無視する**。これは任意の設定場所には意図的な設計だが、パスのタイポでもエラーが出ない。どのソースが値を提供しているか確認するには `trace()` を使うこと。

### VariableExpander

プロパティ値内の `${VAR}` 参照は `VariableExpander.INSTANCE` によって展開される。`PropertySource.get()` 内で `UnaryOperator<String>` として適用される。展開ソースはシステムプロパティと環境変数（この順）。循環参照は検出されない。

---

## TypedKey — レコード

`TypedKey<T>` は Java の `record`:

```java
public record TypedKey<T>(
    String key,
    Class<T> type,
    T defaultValue,
    String description,
    boolean sensitive
) { ... }
```

`record` であることで構造的等価性、マップキーとしての安全な利用、イミュータブルなビルダーセマンティクスが得られる（`.defaultsTo()` と `.describedAs()` は新しいインスタンスを返す）。

### 型変換

`PropStack` は内部の `convert()` メソッドで生の `String` を `T` に変換する。対応する型:

| ファクトリ | Java 型 |
|-----------|---------|
| `TypedKey.string()` | `String` |
| `TypedKey.integer()` | `Integer` / `int` |
| `TypedKey.bool()` | `Boolean` / `boolean` |
| `TypedKey.longKey()` | `Long` / `long` |
| `TypedKey.doubleKey()` | `Double` / `double` |
| `TypedKey.stringList()` | `List<String>` （カンマ分割） |
| `TypedKey.secret()` | `String` + `sensitive=true` |

任意のオブジェクト構築はしない — `String → DataSource` は変換ではなく構築（DD-004）。

### defaultsTo() vs describedAs()

似て見えるが動作が異なる 2 つの概念（DD-008）:

| メソッド | validate() への影響 | dump() への影響 | セマンティクス |
|---------|-------------------|----------------|--------------|
| `.defaultsTo(v)` | スキップ（値あり扱い） | `587 (default)` と表示 | 本番で使える安全なデフォルト |
| `.describedAs(text)` | 不足として検出 | `[MISSING] — text` と表示 | ドキュメントのみ |

`.defaultsTo()` が設定されたキーは未設定として報告されない。`.describedAs()` のみのキーは値が存在しない場合に必ず検出される。

### KeyHolder パターン

Enum が `KeyHolder` を実装することで、関連するキーをグループ化し、型安全なアクセスを可能にする:

```java
enum Smtp implements KeyHolder {
    HOST(TypedKey.string("SMTP_HOST").describedAs("SMTP サーバーホスト名")),
    PORT(TypedKey.integer("SMTP_PORT").defaultsTo(587)),
    PASSWORD(TypedKey.secret("SMTP_PASSWORD"));

    private final TypedKey<?> key;
    Smtp(TypedKey<?> key) { this.key = key; }
    public TypedKey<?> typedKey() { return key; }
}
```

このパターンは Java enum のジェネリクス制限を解決する: 単一の enum は混在する型（String、Integer、Boolean）に対して `TypedKey<T>` を実装できないが、`TypedKey<?>` フィールドを *保持* することはできる（DD-003）。

---

## Registry — ConcurrentHashMap

```java
private static final Map<String, Object> instances = new ConcurrentHashMap<>();
```

`Registry` は単一の `ConcurrentHashMap<String, Object>` を持つ静的クラス。マップキーはルックアップ識別子から生成される文字列:

| ルックアップ | マップキー形式 |
|------------|--------------|
| `Registry.get(DataSource.class)` | `"com.example.DataSource"` |
| `Registry.get(DB.PROD)` | `"com.example.DataSource#PROD"` |
| `Registry.get("myName")` | `"myName"` |

### スレッド安全性

`ConcurrentHashMap` の操作は単一エントリレベルでアトミック。遅延初期化に使う `computeIfAbsent` はサプライヤーがキーごとに最大 1 回だけ実行されることを保証する。

### テスト分離

`Registry.clear()` は全ステートをリセットする。`@AfterEach` で呼び出してテスト間の汚染を防ぐ。

---

## trace() の挙動

`trace()` はソースを走査し、**最初の MATCH で停止する**。それ以降のソースは表示されない:

```
DB_HOST:
  [0] set()               → (empty)
  [1] SystemProperties    → (empty)
  [2] EnvironmentVariables → prod-db  ← MATCH
```

ソース [3] と [4] はマッチが見つかった後は表示されない。これは解決動作を正確に反映しているが、全レイヤーを同時に確認する用途には使えない。全レイヤーを確認したい場合は `PropStack.defaultSources()` を使ってソースを手動で走査する。

---

## データフロー

```
application.properties  ─┐
ユーザーオーバーライド    ─┤  List<PropertySource>  →  PropStack.get(key)
env vars / -D フラグ     ─┤                             │
set() 上書き             ─┘                             │
                                                        ▼
                                         TypedKey.convert(String → T)
                                                        │
                                                        ▼
                                  T (String / int / boolean / List / ...)

PropStack.validate()  → 全不足 TypedKey を収集（デフォルトなし、値なし）→ まとめて例外
PropStack.dump()      → 全 KeyHolder エントリを値/デフォルト/シークレット/不足で整形
PropStack.trace()     → 1 キーのソースを走査、最初のマッチで停止
```

---

## 1.0 残タスク

> CI バッジは未設置。以下は 1.0 向けに記録。

- [ ] GitHub Actions ワークフロー（push/PR で `mvn verify`）
- [ ] README の CI バッジ
- [ ] Javadoc サイト（GitHub Pages）
- [ ] `PropStack.defaultSources()` を安定 API としてドキュメント化
- [ ] 検討: 全ソースを表示する `trace()` オプション（最初のマッチで停止しない）
- [ ] 検討: enum なしで `validate()` に使える `TypedKey` 登録（匿名キーカタログ）
