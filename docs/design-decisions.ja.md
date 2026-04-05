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

## DD-005: fraud-alert ApplicationProperties からの機能移植

**背景:** 元の ApplicationProperties (fraud-alert) には PropStack にない追加機能がある。DGE セッションで各機能を評価した。

**採用:**

| 機能 | 設計 |
|------|------|
| コマンドライン引数 `--KEY=value` | `new PropStack(args)` — args を最優先ソースとして追加する新コンストラクタ |
| 明示的 profile | `new PropStack("myapp", "prod")` — ベースに加えて `application.prod.properties` を読む |
| `validate()` 一括検証 | `props.validate(Smtp.class, Db.class)` — 最初の1つじゃなく全ての不足キーを一度に報告 |

**オプトインで採用:**

| 機能 | 設計 |
|------|------|
| 自動検出 profile (user/host/os) | `new PropStack("myapp", PropStack.autoProfile())` — オプトインのみ、デフォルト無効。`application.user_opa.properties` 等を読む |

**却下:**

| 機能 | 理由 |
|------|------|
| `getInstance()` FQDN リフレクション | DD-004: 責務違反 |
| `Populator` 自動バインド | DI フレームワーク化する |
| `getEnum()` 型ベース探索 | ニッチ。必要になったら追加 |
| 自動検出をデフォルトに | 暗黙の振る舞い。新メンバーにはデバッグ困難。オプトインなら OK |

**DGE での議論ポイント:**

> 自動検出は作った人には便利だが、知らない人には魔法に見える。実際に fraud-alert では「自分の環境で application.user_{name}.properties を作って、変えたい項目だけ書けばいい」と案内していた。便利だが仕組みを知らない人にとっては暗黙の振る舞い。オプトインにすることで「意図的に選択した黒魔術」になる。

## DD-006: スタック途中差し込み — Inserter より defaultSources()

**問題:** ユーザーがカスタム PropertySource（Vault, Consul 等）をスタックの特定位置に差し込みたい場合がある。

**候補:**

| アプローチ | 判定 | 理由 |
|----------|------|------|
| A: Inserter パターン (predicate) | 却下 | 内部実装への依存 |
| B: 全部手書き | 既にある | デフォルト構成を毎回書くのが面倒 |
| C: インデックス指定 insert | 却下 | 脆い。内部順番変更で壊れる |
| D: 名前付きソース + insertAfter | 却下 | 過剰設計 |
| **E: `defaultSources()` メソッド** | **採用** | 新概念ゼロ。標準の List 操作 |

**解決策:** デフォルトソースリストを `ArrayList` として公開:

```java
// 99% のユーザー
PropStack props = new PropStack("myapp");

// 1% のカスタム差し込み
var sources = PropStack.defaultSources("myapp");
sources.add(2, new VaultPropertySource(client));  // 普通の List.add
PropStack props = new PropStack(false, sources.toArray(PropertySource[]::new));
```

新しい API 概念なし。Java の `List` が差し込みメカニズム。
