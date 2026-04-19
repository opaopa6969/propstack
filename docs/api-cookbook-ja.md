# PropStack API クックブック

[English](api-cookbook.md) | [Japanese (日本語)](api-cookbook-ja.md)

全 PropStack API の実践的な使用例。各レシピは**いつ使うか**と**どう使うか**を示す。

---

## TypedKey — キーの定義

### `TypedKey.string(key)`

いつ: 安全なデフォルトがない必須文字列プロパティ。

```java
HOST(TypedKey.string("DB_HOST"))
// props.require(Db.HOST) → 未設定で例外
// props.validate(Db.class) → 不足として検出
```

### `TypedKey.string(key).describedAs(text)`

いつ: `dump()` と `validate()` の出力にドキュメントを表示する必須文字列。

```java
HOST(TypedKey.string("DB_HOST").describedAs("データベースホスト名"))
// dump() → DB_HOST = [MISSING] — データベースホスト名
// validate() → Missing: [DB_HOST]
```

### `TypedKey.integer(key).defaultsTo(n)`

いつ: `validate()` でスキップされる本番で安全なデフォルト付き整数。

```java
PORT(TypedKey.integer("DB_PORT").defaultsTo(5432))
// props.get(Db.PORT) → 未設定なら 5432
// validate() → 不足として報告されない
// dump() → DB_PORT = 5432 (default)
```

### `TypedKey.secret(key)`

いつ: ログに絶対出てはいけない機密文字列。

```java
PASSWORD(TypedKey.secret("DB_PASSWORD"))
// dump() → DB_PASSWORD = ****** (secret)
// require() は未設定で例外 — secret はオプションを意味しない
```

`.describedAs()` と組み合わせる:

```java
PASSWORD(TypedKey.secret("DB_PASSWORD").describedAs("サービスアカウントのパスワード"))
// dump() → DB_PASSWORD = [MISSING] — サービスアカウントのパスワード (secret)
```

### `TypedKey.bool(key).defaultsTo(false)`

いつ: 安全なデフォルト付きのフィーチャーフラグ。

```java
enum Feature implements KeyHolder {
    DARK_MODE(TypedKey.bool("FEATURE_DARK_MODE").defaultsTo(false)),
    BETA_UI(TypedKey.bool("FEATURE_BETA_UI").defaultsTo(false));
    // ...
}

boolean dark = props.get(Feature.DARK_MODE);  // 未設定なら false
```

### `TypedKey.stringList(key)`

いつ: カンマ区切りのリスト。

```java
ORIGINS(TypedKey.stringList("ALLOWED_ORIGINS"))

// application.properties:
// ALLOWED_ORIGINS=https://example.com,https://api.example.com

List<String> origins = props.get(Smtp.ORIGINS);
// → ["https://example.com", "https://api.example.com"]
```

カンマ周りの空白はトリムされる。空エントリは除去される。

### `TypedKey.longKey(key)` / `TypedKey.doubleKey(key)`

```java
TIMEOUT(TypedKey.longKey("TIMEOUT_MS").defaultsTo(5000L))
RATE(TypedKey.doubleKey("SAMPLE_RATE").defaultsTo(0.1))
```

---

## PropStack — 値の読み取り

### `props.get(KeyHolder)`

いつ: デフォルトがある値を読む。未設定ならデフォルトを返す。

```java
int port = props.get(Smtp.PORT);   // 未設定なら 587
```

### `props.require(KeyHolder)`

いつ: 必ず存在する必要がある値を読む。未設定でデフォルトもない場合は `IllegalStateException`。

```java
String host = props.require(Smtp.HOST);
// → IllegalStateException: Required property missing: SMTP_HOST
```

### `props.get(String, String)`

いつ: TypedKey 定義なしのアドホックなルックアップ。

```java
String base = props.get("API_BASE_URL", "https://api.example.com");
```

### `props.getInt(String, int)` / `getBoolean` / `getLong` / `getDouble`

インラインデフォルト付きの型付きプリミティブ。完全な KeyHolder enum が不要な場合に使う。

```java
int workers  = props.getInt("THREAD_POOL_SIZE", 4);
boolean mock = props.getBoolean("USE_MOCK_PAYMENT", false);
```

### `props.set(String, String)`

いつ: プログラム上書き — 最高優先度、全てを上書きする。

```java
props.set("LOG_LEVEL", "DEBUG");
// 環境変数、-D フラグ、ファイルの値を全て上書き
```

---

## PropStack — 検証と診断

### `props.validate(Class<KeyHolder>...)`

いつ: アプリケーション起動時、サービス生成前。

```java
props.validate(Db.class, Smtp.class, Auth.class);
// → IllegalStateException: Missing required properties: [DB_HOST, SMTP_HOST, AUTH_SECRET]
```

全不足キーをまとめて報告する。`.defaultsTo()` のあるキーはスキップ。`.describedAs()` のあるキーは値がなければ報告。`Registry.put(...)` の前に呼んでフェイルファストにする。

### `props.dump(Class<KeyHolder>...)`

いつ: 設定ミスの環境をデバッグする、または起動設定をログに記録する。

```java
System.out.print(props.dump(Db.class, Smtp.class));
```

```
--- Db ---
  DB_HOST                   = prod-db.internal
  DB_PORT                   = 5432 (default)
  DB_NAME                   = myapp
  DB_PASSWORD               = ****** (secret)
--- Smtp ---
  SMTP_HOST                 = smtp.gmail.com
  SMTP_PORT                 = 587 (default)
  SMTP_USER                 = alerts@example.com
  SMTP_PASSWORD             = ****** (secret)
  ALLOWED_ORIGINS           = [MISSING] — カンマ区切りの許可オリジンリスト
```

ログに安全: シークレットはマスク済み、不足キーは可視化。

### `props.trace(String)` / `trace(KeyHolder)` / `trace(PropertyKey)`

いつ: 値が予期しないソースから来ている、または全く設定されていない。

```java
System.out.print(props.trace("DB_HOST"));
System.out.print(props.trace(Db.HOST));  // 同じ結果
```

```
DB_HOST:
  [0] set()               → (empty)
  [1] SystemProperties    → (empty)
  [2] EnvironmentVariables → prod-db.internal  ← MATCH
```

> **注意:** `trace()` は最初の `MATCH` 以降の表示を止める。これは意図的 — first-match-wins の解決動作を反映している。マッチ後のソース（ホームファイル、classpath）はキーを持っていても表示されない。全ソースを確認したい場合は `PropStack.defaultSources(appName)` を手動で走査する。

---

## PropStack — カスタムソース

### `PropertySource.fromPath(Path)`

いつ: ディスク上の絶対パスから設定を読む。

```java
PropStack props = new PropStack(true,
    PropertySource.fromPath(Path.of("/etc/myapp/config.properties"))
);
```

> **警告:** `fromPath()` は存在しないファイル、読み取れないファイル、パースエラーのあるファイルを **サイレントに無視する**。パスのタイポでもエラーが出ない。パスソースが読み込まれているか確認するには `trace()` を使う。

### `PropertySource.fromClasspath(String)`

いつ: JAR に同梱された名前付きプロパティファイルを読む。

```java
PropertySource.fromClasspath("defaults.properties")
```

### `PropertySource.of(Map)`

いつ: テスト値やプログラム上書きをソースとして注入する。

```java
PropertySource overrides = PropertySource.of(Map.of(
    "DB_HOST", "test-db",
    "DB_PORT", "5433"
));
PropStack props = new PropStack(false, overrides);
```

### `PropertySource.fromArgs(String[])`

いつ: コマンドラインから `--KEY=value` フラグを受け付ける。

```java
public static void main(String[] args) {
    PropStack props = new PropStack("myapp", args);
    // java -jar app.jar --DB_HOST=custom-host
    // → DB_HOST が "custom-host" として解決（最優先）
}
```

### `PropertySource.forUser()` / `forHost()` / `forOs()`

いつ: 開発者ごと、ホストごと、OS ごとのオーバーライドを読む。

```java
PropStack props = new PropStack("myapp",
    PropertySource.forUser(),   // application.user_alice.properties
    PropertySource.forHost(),   // application.host_prod-01.properties
    PropertySource.forOs()      // application.os_linux.properties
);
```

存在しないファイルはサイレントにスキップ。オーバーライドファイルを作らない開発者は共有デフォルトをそのまま使う。

### `PropStack.defaultSources(String)` — スタックカスタマイズ

いつ: カスタムソース（Vault、Consul など）を特定の位置に挿入する（DD-006）。

```java
var sources = PropStack.defaultSources("myapp");
// sources = [SystemProperties, EnvironmentVariables, ~/.myapp/app.props, classpath app.props]

sources.add(2, new VaultPropertySource(vaultClient));  // env の後、ホームファイルの前

PropStack props = new PropStack(false, sources.toArray(PropertySource[]::new));
```

標準の `List.add(index, element)` — 新しいコンセプト不要。

---

## Registry — コンポーネント管理

### `Registry.put(Class, T)` / `Registry.get(Class)`

いつ: 1 型 1 インスタンス。

```java
Registry.put(DataSource.class, HikariDataSource.createPool(config));
DataSource ds = Registry.get(DataSource.class);
```

### `Registry.get(Class, Supplier<T>)`

いつ: 遅延初期化 — 最初のアクセス時に生成。

```java
// 最初の get() で 1 回だけ生成、以後キャッシュ
DataSource ds = Registry.get(DataSource.class, () -> createDataSource(props));
```

### `Registry.put(RegistryKey<T>, T)` / `Registry.get(RegistryKey<T>)`

いつ: 同じ型の複数インスタンス。

```java
enum DB implements RegistryKey<DataSource> {
    PRIMARY(DataSource.class),
    REPLICA(DataSource.class);

    private final Class<DataSource> type;
    DB(Class<DataSource> type) { this.type = type; }
    public Class<DataSource> type() { return type; }
}

Registry.put(DB.PRIMARY, primaryPool);
Registry.put(DB.REPLICA, replicaPool);

DataSource primary = Registry.get(DB.PRIMARY);
DataSource replica  = Registry.get(DB.REPLICA);
```

### `Registry.put(String, Object)` / `Registry.get(String)`

いつ: 型付き enum なしの名前付きルックアップ。

```java
Registry.put("httpClient", HttpClient.newHttpClient());
HttpClient client = Registry.get("httpClient");
```

### `Registry.contains(Class<?>)` / `Registry.size()`

いつ: 登録状態の確認（例: 条件付き初期化）。

```java
if (!Registry.contains(DataSource.class)) {
    Registry.put(DataSource.class, createDataSource(props));
}
```

### `Registry.remove(Class<?>)` / `Registry.clear()`

いつ: テストクリーンアップまたはグレースフルシャットダウン。

```java
@AfterEach
void cleanup() {
    Registry.clear();  // テスト間で全リセット
}

// 個別削除
Registry.remove(DataSource.class);
```

---

## 変数展開

プロパティ値内の `${VAR}` は自動展開される。

```properties
# application.properties
GREETING=hello ${USER}
DB_URL=jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
```

```java
String url = props.get("DB_URL", "");
// → "jdbc:postgresql://prod-db:5432/myapp"  (DB_HOST, DB_PORT, DB_NAME が設定されていれば)
```

展開はシステムプロパティを先に参照し、次に環境変数を参照する。循環参照（`A=${B}`, `B=${A}`）は検出されず、予期しない結果になる。

---

## 完全な例 — 起動時の組み立て

```java
public class App {

    enum Db implements KeyHolder {
        HOST(TypedKey.string("DB_HOST").describedAs("データベースホスト名")),
        PORT(TypedKey.integer("DB_PORT").defaultsTo(5432)),
        NAME(TypedKey.string("DB_NAME").describedAs("データベーススキーマ")),
        PASSWORD(TypedKey.secret("DB_PASSWORD").describedAs("データベースパスワード"));

        private final TypedKey<?> key;
        Db(TypedKey<?> key) { this.key = key; }
        public TypedKey<?> typedKey() { return key; }
    }

    public static void main(String[] args) {
        PropStack props = new PropStack("myapp", args);

        // フェイルファスト: 全不足キーをまとめて報告
        props.validate(Db.class);

        // 任意: 設定サマリをログ出力（シークレットはマスク済み）
        System.out.print(props.dump(Db.class));

        // コンポーネントの組み立て
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl("jdbc:postgresql://"
            + props.require(Db.HOST) + ":" + props.get(Db.PORT) + "/"
            + props.require(Db.NAME));
        cfg.setPassword(props.require(Db.PASSWORD));
        Registry.put(DataSource.class, new HikariDataSource(cfg));

        Registry.put(UserRepository.class,
            new UserRepository(Registry.get(DataSource.class)));

        // 起動
        new AppServer(Registry.get(UserRepository.class)).start();
    }
}
```
