# DGE Session: PropStack でオブジェクト構築はしない

**Date:** 2026-04-05
**Decision:** [DD-004](../../docs/design-decisions.md#dd-004-no-object-construction-in-propstack)
**Cast:** ☕ ヤン + 👤 今泉 + ⚔ リヴァイ + 🏥 ハウス
**Context:** 作者から「任意の object を get できる仕組みも考えてみる？FQDN 入れればできるとは思うけど」という提案。

---

☕ **ヤン**: 2つの案を整理しよう。

### 案 A: Converter 付き TypedKey

```java
TypedKey<DataSource> DS = TypedKey.of("DB_URL", 
    url -> new HikariDataSource(url));

DataSource ds = props.get(DS);
// "jdbc:postgresql://localhost:5432/mydb" → DataSource オブジェクト
```

### 案 B: FQDN リフレクション

```java
// application.properties:
// CACHE_IMPL=com.example.RedisCacheImpl

TypedKey<Cache> CACHE = TypedKey.classRef("CACHE_IMPL", Cache.class);
Cache cache = props.get(CACHE);
// "com.example.RedisCacheImpl" → new RedisCacheImpl()
```

👤 **今泉**: そもそも、これって PropStack の責務ですか？設定を読むライブラリがオブジェクトの生成まで担当するのは？

☕ **ヤン**: いい質問。境界線の問題だね:

```
PropStack の責務:    文字列を読む → 型変換（int, boolean, etc.）
Registry の責務:     オブジェクトを管理する（put/get）
??? の責務:          文字列からオブジェクトを構築する
```

⚔ **リヴァイ**: `String → int` は型変換。`String → DataSource` は **構築**。別物だ。

🎩 **千石**: でもお客様の視点で考えましょう。実際のコードで何が起きるか:

```java
// 今のやり方 — PropStack + Registry を別々に使う
PropStack props = new PropStack();
String url = props.require(Db.URL);
DataSource ds = new HikariDataSource(url);  // ← この行が毎回要る
Registry.put(DataSource.class, ds);

// converter があれば
DataSource ds = props.get(Db.DATASOURCE);  // 1行で済む
Registry.put(DataSource.class, ds);
```

☕ **ヤン**: 1行減るだけ。でもその1行のために `TypedKey` に `Function<String, T>` を持たせると...

👤 **今泉**: 「誰が困るの？」— converter が PropStack に入ると:
1. TypedKey が重くなる（Function フィールド追加）
2. converter の例外処理が PropStack の責務に入る
3. `DataSource` の生成ロジックが設定キーの定義に混入する

⚔ **リヴァイ**: やめろ。**PropStack は文字列を読む。オブジェクトを作るのはアプリの仕事。** 混ぜるな。

☕ **ヤン**: 案 B の FQDN はもっと危険だね。リフレクションで任意のクラスをインスタンス化するのはセキュリティリスクもある。

🏥 **ハウス**: 全員嘘をついている。本当に欲しいのは converter じゃない。**PropStack と Registry の橋渡し** だ:

```java
// これが本当に欲しいもの
PropStack props = new PropStack();
Registry.put(DataSource.class, () -> createDataSource(props));  // lazy

// 使う側
DataSource ds = Registry.get(DataSource.class);  // 初回で生成
```

これなら PropStack は文字列を読むだけ、Registry がオブジェクトを管理するだけ。**責務が混ざらない**。

👤 **今泉**: 「前もそうだったっけ」— Spring が `@Bean` メソッドで構築ロジックを書くのと同じ構造ですよね。ただしプロキシなしで。

☕ **ヤン**: そう。`main.java` の10行がそれ:

```java
PropStack props = new PropStack();

// 構築は明示的。converter もリフレクションも不要。
Registry.put(DataSource.class, () -> createDataSource(props));
Registry.put(Cache.class, () -> {
    String impl = props.get(Config.CACHE_IMPL);  // FQDN を読む
    return (Cache) Class.forName(impl).getDeclaredConstructor().newInstance();
});
```

FQDN からのインスタンス化も **アプリ側で明示的にやる**。PropStack に隠さない。

⚔ **リヴァイ**: 機能を追加しないという判断が正しい時もある。今がそれだ。

☕ **ヤン**: ゼロ行の実装が最強の実装。紅茶おかわり。

---

**→ やらない。PropStack は文字列を読む。Registry はオブジェクトを管理する。構築はアプリの責任。**
