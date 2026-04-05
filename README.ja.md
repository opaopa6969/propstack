# PropStack

[English](README.md) | [Japanese (日本語)](README.ja.md)

**Java 用スタック型プロパティリゾルバ + コンポーネントレジストリ。DI 不要。アノテーション不要。プロキシ不要。**

```java
enum Config implements PropertyKey {
    DB_HOST, DB_PORT, DB_NAME;
    public String key() { return name(); }
}

PropStack props = new PropStack();
String host = props.require(Config.DB_HOST);
int port = props.getInt(Config.DB_PORT);
```

型安全。スタック可能。依存ゼロ。

## なぜ PropStack か？

既存の設定ライブラリは全て「フレームワークに乗れ」と言ってくる:
- Spring Boot → `@Value` + `@Configuration` + DI コンテナ + プロキシの黒魔術
- MicroProfile Config → CDI + `@Inject` + `@ConfigProperty`
- Typesafe Config → HOCON 形式 + Scala エコシステム
- owner → インターフェース + アノテーション + マジックプロキシ

**PropStack には主張がない。** プロパティを読む。複数ソースから。最初に見つかった値を返す。

`Registry` も同梱 — DI フレームワークを使いたくないけどアプリケーションスコープのコンポーネント管理は必要な人のための、最小限のコンポーネントレジストリ。

## 同梱クラス

| クラス | 機能 |
|-------|------|
| `PropStack` | スタック型プロパティリゾルバ |
| `Registry` | 名前付き + 型安全なコンポーネントレジストリ |
| `RegistryKey<T>` | 型安全カタログ enum 用インターフェース |
| `PropertySource` | プラガブルなプロパティソースインターフェース |
| `ApplicationProperties` | PropStack の後方互換エイリアス |
| `Singletons` | Registry の後方互換エイリアス |

***

## PropStack — 設定

### 解決順序

```
1. props.set("KEY", "value")       ← プログラム上書き（最優先）
2. -DKEY=value                      ← JVM システムプロパティ
3. KEY=value (環境変数)             ← OS 環境変数
4. ~/.volta/application.properties  ← ユーザーホームファイル
5. classpath application.properties ← JAR 内デフォルト（最低優先）
```

### Maven

```xml
<dependency>
    <groupId>org.unlaxer</groupId>
    <artifactId>propstack</artifactId>
    <version>0.2.0</version>
</dependency>
```

### 基本

```java
PropStack props = new PropStack();

String host = props.get("DB_HOST", "localhost");
int port = props.getInt("PORT", 8080);
boolean debug = props.getBoolean("DEBUG", false);
long timeout = props.getLong("TIMEOUT_MS", 5000L);

// 必須（未設定で例外）
String secret = props.require("JWT_SECRET");
```

### カスタムアプリ名

```java
// ~/.volta/ ではなく ~/.myapp/application.properties から読む
PropStack props = new PropStack("myapp");
```

### カスタムソース

```java
PropStack props = new PropStack(true,
    PropertySource.fromPath(Path.of("/etc/myapp/config.properties")),
    PropertySource.fromClasspath("defaults.properties")
);
```

### 変数展開

```properties
# application.properties
GREETING=hello ${USER}
DB_URL=jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
```

`${VAR}` はシステムプロパティと環境変数から解決される。

### 型付きキー（オプション）

```java
enum Config implements PropertyKey {
    DB_HOST, DB_PORT, DB_NAME;
    public String key() { return name(); }
}

String host = props.get(Config.DB_HOST).orElse("localhost");
```

***

## Registry — コンポーネント管理

### クラスで取得（1 型 1 インスタンス）

```java
Registry.put(DataSource.class, dataSource);
DataSource ds = Registry.get(DataSource.class);
```

### 名前付きキーで取得（1 型 複数インスタンス）

```java
enum DB implements RegistryKey<DataSource> {
    PROD(DataSource.class),
    DEV(DataSource.class);

    private final Class<DataSource> type;
    DB(Class<DataSource> type) { this.type = type; }
    public Class<DataSource> type() { return type; }
}

Registry.put(DB.PROD, prodDataSource);
Registry.put(DB.DEV, devDataSource);

DataSource prod = Registry.get(DB.PROD);  // 型安全
DataSource dev = Registry.get(DB.DEV);    // 同じ型、別インスタンス
```

### 遅延初期化

```java
// 初回 get() で生成、以後キャッシュ
DataSource ds = Registry.get(DataSource.class, () -> createDataSource(props));
```

### テストサポート

```java
@AfterEach
void cleanup() {
    Registry.clear();  // 全リセット
}

@Test
void test() {
    Registry.put(DataSource.class, mockDataSource);  // モック差し替え
    // ...
}
```

***

## なぜ DI ではないのか？ — 会話劇

> 以下は PropStack が DI フレームワークではなく Registry を選んだ理由を探る設計会話劇です。
> これが我々の設計判断の方法 — キャラクター駆動の討論（[DGE 手法](https://github.com/opaopa6969/DGE-toolkit)）。

---

☕ **ヤン** *(怠惰な戦略家)*: まず認めよう。**DI の原則は正しい**。依存はハードコードじゃなく注入すべきだ。問題は、そのために *フレームワーク* が必要かってこと。

👤 **今泉** *(質問する人)*: そもそも、Service Locator が「アンチパターン」になったのっていつからですか？

🎩 **千石** *(品質の番人)*: Mark Seemann が 2010 年に「Service Locator is an Anti-Pattern」を書いてからです。理由は 3 つ:
1. API が嘘をつく — 依存がコンストラクタに出てこない
2. テストが困難
3. エラーが実行時に起きる（コンパイル時に検出できない）

☕ **ヤン**: でもさ、**Spring の `@Autowired` も全く同じ問題** じゃない？
- フィールドインジェクションはコンストラクタに依存が見えない → Seemann 自身の批判そのもの
- Bean 未定義で `NoUniqueBeanDefinitionException` → 実行時エラー
- CGLIB プロキシが **デバッガが追えないコールスタック** を生む
- `@Conditional` で **同じコードが環境によって違う振る舞い** をする

🏥 **Dr. ハウス** *(隠れた問題の診断医)*: 全員嘘をついている。Spring DI は **Service Locator の問題を全部持った上で、さらにプロキシの黒魔術が乗る**。

| 症状 | Service Locator | Spring DI | PropStack Registry |
|------|----------------|-----------|-------------------|
| 隠れた依存 | メソッド内の `get(X.class)` | フィールドの `@Autowired` | 同じ |
| テスト困難 | `put()` で差し替え | `@MockBean` で差し替え | `put()` で差し替え |
| 実行時エラー | 未登録 → 例外 | 未定義 → 起動失敗 | 未登録 → 例外 |
| **デバッグ** | **直接呼び出し** | **プロキシ地獄** | **直接呼び出し** |

⚔ **リヴァイ** *(実装の強制者)*: 差は **ほぼゼロ** だ。だが Spring DI はプロキシ + AOP + 暗黙スキャンという追加の複雑さを持つ。

☕ **ヤン**: そしてここが核心。**DI の原則 ≠ DI フレームワーク**。

```java
// これが依存性注入。フレームワーク不要。
class MyService {
    private final DataSource ds;
    MyService(DataSource ds) { this.ds = ds; }  // コンストラクタインジェクション
}

// 誰が注入するかは自由。
new MyService(Registry.get(DataSource.class));  // Registry でも
new MyService(prodDs);                          // 手動でも
new MyService(mockDs);                          // テストでも
```

👤 **今泉**: 「要するに」、DI と Service Locator は **対立軸じゃなくて補完関係** ってことですか？

🎩 **千石**: そうです。Martin Fowler の 2004 年の原論文では **対等な選択肢** として紹介されています。「アンチパターン」のレッテルは後からブログ記事で貼られたもので、パターンコミュニティの総意ではありません。

🏥 **Dr. ハウス**: 診断結果。**「Service Locator はアンチパターン」は誤診**。正確な診断は「テストサポートなしにグローバルな可変状態で依存を隠すのがアンチパターン」。`put()`、`get()`、`clear()` を持つ Registry にはその問題がない。Vicodin くれ。

---

### PropStack の立場

> **DI の原則は正しい。DI フレームワークはほとんどのアプリには過剰。**
>
> PropStack が提供するもの:
> - `PropStack` — どこからでも設定を読む。フレームワーク不要
> - `Registry` — 名前と型でコンポーネント管理。フレームワーク不要
> - コンストラクタインジェクション — **自分で書け。1 行だ**
>
> **組み立ては自分の責任。それがポイント。**

```java
// main.java — これが「DI コンテナ」。10 行。読める。
PropStack props = new PropStack();
DataSource ds = createDataSource(props);
Registry.put(DataSource.class, ds);

MyService service = new MyService(ds);     // コンストラクタインジェクション
Registry.put(MyService.class, service);    // Registry にも登録

app.start();
```

スキャンなし。プロキシなし。30 秒の起動待ちなし。黒魔術なし。
**自分で書いたから、自分でデバッグできる。**

***

## 後方互換性

```java
// これらはそのまま動く:
ApplicationProperties props = new ApplicationProperties();
Singletons.get(MyClass.class);
Singletons.put(MyClass.class, instance);
```

## 仕組み

```
PropStack
  ├── [0] メモリ上の上書き (set() 呼び出し)
  ├── [1] System.getProperty()     ← -D フラグ
  ├── [2] System.getenv()          ← 環境変数
  ├── [3] ~/.volta/app.properties  ← ユーザーファイル
  └── [4] classpath app.properties ← デフォルト

Registry
  └── ConcurrentHashMap<String, Object>
      ├── "com.example.DataSource"              → クラスで取得
      ├── "com.example.DataSource#PROD"         → RegistryKey で取得
      └── "myCustomName"                        → 文字列で取得
```

## 要件

- Java 21+
- 依存ゼロ（テスト: JUnit 5）

## ライセンス

MIT
