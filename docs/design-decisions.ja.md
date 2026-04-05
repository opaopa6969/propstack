# 設計判断

[English](design-decisions.md) | [Japanese (日本語)](design-decisions.ja.md)

[DGE (Design-Gap Exploration)](https://github.com/opaopa6969/DGE-toolkit) — キャラクター駆動の対話による設計判断の記録。

## DD-001: なぜ DI ではないのか

[README — なぜ DI ではないのか？](../README.ja.md#なぜ-di-ではないのか--会話劇) を参照。

**判断:** PropStack は DI フレームワークではなく Registry パターンを採用。
**理由:** DI の原則は正しい。DI フレームワークは過剰。Spring の `@Autowired` は Service Locator と同じ問題を持つ上に、プロキシの黒魔術が乗る。

## DD-002: 命名 — PropStack

**判断:** 「PropStack」を採用。
**候補:** stackable-properties, unlaxer-config, propstack, cascading-config, konfig, simplestack
**理由:** 短い。被りなし。`new PropStack()` が綺麗。名は体を表す必要はない — DI 嫌いが作った小さなライブラリ。

## DD-003: TypedKey — Enum + TypedKey フィールド（案 D）

**判断:** Enum が `KeyHolder` を実装し、`TypedKey<?>` をフィールドとして保持する。

**問題:** Java の enum は1つのジェネリクスパラメータしか持てない。設定グループ（例: SMTP）には String, Integer, Boolean のキーが混在する。

**候補:**

| | 型安全 | enum 維持 | 機能グルーピング | デフォルト値 | 一覧取得 |
|---|---|---|---|---|---|
| A: enum に `TypedKey<T>` インターフェース | ✅ | ❌ (1型制限) | ❌ | ✅ | ❌ |
| B: static 定数 | ✅ | ❌ | ❌ | ✅ | ❌ |
| C: enum + converter | ❌ (Object) | ✅ | ✅ | ✅ | ✅ |
| **D: enum + TypedKey フィールド** | **✅** | **✅** | **✅** | **✅** | **✅** |

**解決策:** 各 enum 定数が `TypedKey<?>` フィールドを保持。型情報はファクトリメソッド（`TypedKey.string()`, `TypedKey.integer()` 等）で定義時に確定。PropStack が内部で変換する。

```java
enum Smtp implements KeyHolder {
    HOST(TypedKey.string("SMTP_HOST")),
    PORT(TypedKey.integer("SMTP_PORT", 587));
    
    private final TypedKey<?> key;
    Smtp(TypedKey<?> key) { this.key = key; }
    public TypedKey<?> typedKey() { return key; }
}

String host = props.get(Smtp.HOST);  // コンパイル時に String
int port = props.get(Smtp.PORT);     // コンパイル時に int
```

## DD-004: PropStack でオブジェクト構築はしない

**判断:** PropStack は文字列から任意のオブジェクト（DataSource, Cache 実装等）への変換を**サポートしない**。

**却下されたアプローチ:**

| アプローチ | 却下理由 |
|----------|---------|
| Converter 付き TypedKey (`TypedKey.of("DB_URL", url -> new HikariDataSource(url))`) | 構築ロジックがキー定義に漏れる。責務違反 |
| FQDN リフレクション (`com.example.RedisCacheImpl` → `Class.forName().newInstance()`) | セキュリティリスク。黒魔術 |

**理由（DGE 会話劇要約）:**

> ⚔ **リヴァイ**: `String → int` は型変換。`String → DataSource` は **構築**。別物だ。混ぜるな。
>
> 🏥 **Dr. ハウス**: 本当に欲しいのは PropStack と Registry の橋渡しだ。その橋は `main()` メソッド。10行。明示的。デバッグ可能。
>
> ☕ **ヤン**: ゼロ行の実装が最強の実装。PropStack は文字列を読む。Registry はオブジェクトを管理する。アプリがそれを繋ぐ。

**正しいやり方:**

```java
// main.java — 君が DI コンテナだ
PropStack props = new PropStack();

Registry.put(DataSource.class, () -> createDataSource(props));
Registry.put(Cache.class, () -> {
    String impl = props.require(Config.CACHE_IMPL);  // FQDN を文字列として読む
    return (Cache) Class.forName(impl).getDeclaredConstructor().newInstance();  // アプリの判断
});

app.start();
```

**境界:** PropStack は文字列を読む。Registry はオブジェクトを管理する。構築はアプリの責任。
