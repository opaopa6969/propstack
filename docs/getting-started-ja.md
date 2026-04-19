# PropStack — はじめる

[English](getting-started.md) | [Japanese (日本語)](getting-started-ja.md)

PropStack は複数のソースから設定を読み込み、アプリケーションスコープのコンポーネントを管理するための依存ゼロの Java ライブラリ。このガイドでは 5 分以内に動作する状態にする。

---

## 要件

- Java 21+
- Maven または Gradle

---

## 1. 依存を追加

### Maven

```xml
<dependency>
    <groupId>org.unlaxer</groupId>
    <artifactId>propstack</artifactId>
    <version>0.9.1</version>
</dependency>
```

### Gradle

```groovy
implementation 'org.unlaxer:propstack:0.9.1'
```

最新バージョン: [![Maven Central](https://img.shields.io/maven-central/v/org.unlaxer/propstack)](https://central.sonatype.com/artifact/org.unlaxer/propstack)

> 注意: CI バッジは未設置。[architecture-ja.md — 1.0 残タスク](architecture-ja.md#10-残タスク) を参照。

---

## 2. 基本的な使い方

```java
import org.unlaxer.propstack.PropStack;

PropStack props = new PropStack();

// デフォルト付き String
String host = props.get("DB_HOST", "localhost");

// 型付きプリミティブ
int port     = props.getInt("DB_PORT", 5432);
boolean debug = props.getBoolean("DEBUG", false);
long timeout  = props.getLong("TIMEOUT_MS", 5000L);

// 必須 — 未設定で IllegalStateException
String secret = props.require("JWT_SECRET");
```

以上。アノテーションなし、DI なし、設定クラスなし。

---

## 3. アプリ名を指定する（シークレット用）

```java
// ~/.myapp/application.properties からも読む
PropStack props = new PropStack("myapp");
```

各マシンで `~/.myapp/application.properties` を作成し、その環境のシークレットを書く:

```properties
# ~/.myapp/application.properties  (git にコミットしない)
DB_PASSWORD=s3cr3t
JWT_SECRET=abc123
```

共有デフォルトは `src/main/resources/application.properties` に書く（git にコミット、シークレットなし）。

---

## 4. KeyHolder で型付きキーを定義する

1 回限りのルックアップ以外には、型付き enum でキーを定義する:

```java
import org.unlaxer.propstack.*;

enum Db implements KeyHolder {
    HOST(TypedKey.string("DB_HOST").describedAs("データベースホスト名")),
    PORT(TypedKey.integer("DB_PORT").defaultsTo(5432)),
    NAME(TypedKey.string("DB_NAME").describedAs("データベーススキーマ名")),
    PASSWORD(TypedKey.secret("DB_PASSWORD").describedAs("データベースパスワード"));

    private final TypedKey<?> key;
    Db(TypedKey<?> key) { this.key = key; }
    public TypedKey<?> typedKey() { return key; }
}
```

`PropStack` で enum を使う:

```java
PropStack props = new PropStack("myapp");

// 起動時に一括検証 — 全不足キーをまとめて報告
props.validate(Db.class);

// 型安全なアクセス — キャスト不要
String host  = props.require(Db.HOST);   // デフォルトなし、未設定で例外
int port     = props.get(Db.PORT);       // 未設定なら 5432
String name  = props.require(Db.NAME);
String pw    = props.require(Db.PASSWORD);
```

---

## 5. 設定を診断する

```java
// 全キーの値・デフォルト・シークレット・不足を表示
System.out.print(props.dump(Db.class));
```

出力例:

```
--- Db ---
  DB_HOST                   = prod-db.internal
  DB_PORT                   = 5432 (default)
  DB_NAME                   = myapp
  DB_PASSWORD               = ****** (secret)
```

特定のキーがどこから来たか確認する:

```java
System.out.print(props.trace("DB_HOST"));
```

```
DB_HOST:
  [0] set()               → (empty)
  [1] SystemProperties    → (empty)
  [2] EnvironmentVariables → prod-db.internal  ← MATCH
```

> **注意:** `trace()` は最初の MATCH で停止する。それ以降のソースは表示されない。これは実際の解決動作を反映している。全レイヤーを確認したい場合は `PropStack.defaultSources()` を使って手動で走査する。

---

## 6. 開発者ごとのオーバーライド

チームメンバーのローカル環境はそれぞれ違う。共有ファイルを編集する代わりに、開発者ごとのオーバーライドファイルを作る:

```java
PropStack props = new PropStack("myapp",
    PropertySource.forUser()   // application.user_{ユーザー名}.properties
);
```

Alice が `src/main/resources/application.user_alice.properties` を作成:

```properties
DB_HOST=alice-local-db
DB_PORT=54321
```

このファイルは git にコミットできる — シークレットはなく、チームの助けになる。新メンバーのオンボーディングは「`application.user_{あなたの名前}.properties` を作って、変えたいキーだけ追加すればいい」の一言で済む。

---

## 7. Registry — コンポーネント管理

`Registry` はアプリケーションスコープのオブジェクトを管理する PropStack の仲間:

```java
import org.unlaxer.propstack.Registry;

// 登録
Registry.put(DataSource.class, createDataSource(props));

// 取得（スレッドセーフ）
DataSource ds = Registry.get(DataSource.class);

// 遅延初期化
DataSource ds = Registry.get(DataSource.class, () -> createDataSource(props));
```

`main()` でアプリケーションを組み立てる:

```java
public static void main(String[] args) {
    PropStack props = new PropStack("myapp");
    props.validate(Db.class);

    Registry.put(DataSource.class, createDataSource(props));
    Registry.put(MyService.class, new MyService(Registry.get(DataSource.class)));

    // サービス開始
}
```

テスト分離のために:

```java
@AfterEach
void cleanup() {
    Registry.clear();
}
```

---

## 次のステップ

- [API クックブック](api-cookbook-ja.md) — 全メソッドの使用例
- [アーキテクチャ](architecture-ja.md) — スタック構造の内部解説
- [設計判断](design-decisions.ja.md) — なぜこの設計にしたか
