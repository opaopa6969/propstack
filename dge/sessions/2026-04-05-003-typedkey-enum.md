# DGE Session: TypedKey の enum 設計

**Date:** 2026-04-05
**Decision:** [DD-003](../../docs/design-decisions.md#dd-003-typedkey--enum-with-typedkey-field-案-d)
**Cast:** ☕ ヤン + 👤 今泉 + ⚔ リヴァイ + 🎩 千石
**Context:** `props.getInt(Config.DB_PORT)` の返り値の型がキー定義から分からない問題。PropertyKey を拡張して型情報を持たせたい。ただし作者は enum の網羅性を捨てたくない。

---

☕ **ヤン**: 現状の問題を整理しよう:

```java
// 今 — 呼び出し側がメソッドを選ぶ。キーから型がわからない
String host = props.require(Config.DB_HOST);    // OK, でも String なの？
int port = props.getInt(Config.DB_PORT);        // getInt? getInt(key, default)?
boolean debug = props.getBoolean(Config.DEBUG); // 間違えて getString したら？
```

型がキーに紐づいてないから **間違えてもコンパイル時に検出できない**。

👤 **今泉**: そもそも、他のライブラリはこの問題をどう解決してるんですか？

☕ **ヤン**:
- **Typesafe Config** → `config.getInt("port")` — 文字列キー + メソッド選択。同じ問題
- **MicroProfile Config** → `@ConfigProperty(name="port") int port` — アノテーション + DI。型はフィールドで決まる
- **owner** → `@Key("port") int port()` — インターフェースプロキシ。黒魔術
- **kotlinx-serialization** → データクラスにマッピング。型はクラスで決まる

👤 **今泉**: 「他にないの？」— enum に型情報を持たせるアプローチは？

☕ **ヤン**: 3つの設計案がある:

### 案 A: `TypedKey<T>` 新インターフェース

```java
interface TypedKey<T> extends PropertyKey {
    Class<T> type();
    default T defaultValue() { return null; }
}

enum Config implements TypedKey<String> {
    DB_HOST(String.class),
    DB_PORT(Integer.class),   // ← コンパイルエラー！enum は1つの型しか持てない
}
```

⚔ **リヴァイ**: ダメだ。Java の enum は **1つのジェネリクスパラメータしか持てない**。DB_HOST が String で DB_PORT が Integer なら同じ enum に入れられない。

### 案 B: enum をやめて定数に

```java
class Config {
    static final TypedKey<String> DB_HOST = TypedKey.of("DB_HOST", String.class);
    static final TypedKey<Integer> DB_PORT = TypedKey.of("DB_PORT", Integer.class, 5432);
}

String host = props.get(Config.DB_HOST);   // コンパイル時に String 確定
int port = props.get(Config.DB_PORT);      // コンパイル時に int 確定
```

☕ **ヤン**: これいいね。enum の制約を回避して、型安全も得られる。

👤 **今泉**: 「誰が困るの？」— enum の良さ（`values()` で一覧取得、switch で網羅チェック）を失うけど。

### 案 C: enum + converter をキーに持たせる

```java
enum Config implements PropertyKey {
    DB_HOST("DB_HOST"),
    DB_PORT("DB_PORT", Integer::parseInt, 5432);
}
<T> T get(PropertyKey key);  // converter で変換して返す
```

⚔ **リヴァイ**: 案 C は enum を維持できるが、戻り値の型が `Object` になって **キャストが必要** になる。コンパイル時の型安全がない。

🎩 **千石**: 案 B が最もクリーンです。

---

**作者の介入:** enum の網羅性は捨てがたいんだよな。例えば email 関係の propertyKey は enum によって固められている。機能によって自動的に分けられるっていうのが利点なんだ。何か抜け道がありそう。

---

☕ **ヤン**: ちょっと待って。enum をやめる必要はない。**enum がフィールドとして TypedKey を持てばいい**。

### 案 D: enum + TypedKey フィールド

```java
enum Smtp implements KeyHolder {
    HOST(TypedKey.string("SMTP_HOST")),
    PORT(TypedKey.integer("SMTP_PORT").defaultsTo(587));
    
    private final TypedKey<?> key;
    Smtp(TypedKey<?> key) { this.key = key; }
    public TypedKey<?> typedKey() { return key; }
}

String host = props.get(Smtp.HOST);  // compile-time String
int port = props.get(Smtp.PORT);     // compile-time int
```

👤 **今泉**: これだと enum の良さが全部残りますよね？

☕ **ヤン**: そう:
- `Smtp.values()` で SMTP 設定を一覧
- `Db.values()` で DB 設定を一覧
- 機能ごとに enum が分かれてカタログになる
- 型はコンパイル時に `TypedKey` の構築で固定される

⚔ **リヴァイ**: きれいだ。enum ごとのボイラープレート（field + constructor + typedKey()）が気になるが、Java だからしょうがない。

☕ **ヤン**: record にしたいけど enum だからね。Java の限界。でも 5 行のボイラープレートで型安全 + 網羅性 + 機能グルーピングが全部得られる。

---

**スコアカード:**

| | 型安全 | enum 維持 | 機能グルーピング | デフォルト値 | 一覧 |
|---|---|---|---|---|---|
| A: TypedKey interface on enum | ✅ | ❌ | ❌ | ✅ | ❌ |
| B: static 定数 | ✅ | ❌ | ❌ | ✅ | ❌ |
| C: enum + converter | ❌ | ✅ | ✅ | ✅ | ✅ |
| **D: enum + TypedKey field** | **✅** | **✅** | **✅** | **✅** | **✅** |

**→ 案 D 採用。enum の網羅性 + 型安全 + 機能グルーピング全取り。**
