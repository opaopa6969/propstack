# DGE Session: 競合分析 + defaultsTo vs describedAs

**Date:** 2026-04-05
**Decision:** [DD-007](../../docs/design-decisions.md#dd-007-competitive-analysis--features-adopted-from-dge-review), [DD-008](../../docs/design-decisions.md#dd-008-defaultsto-vs-describedas--doc-as-code)
**Cast:** ☕ ヤン + 👤 今泉 + ⚔ リヴァイ + 🎩 千石 + 😈 Red Team
**Context:** PropStack v0.5.0 時点で他の設定ライブラリと比較。良い点は取り入れる。

---

## Part 1: 競合分析 (DD-007)

☕ **ヤン**: 主要な競合を全部並べて、正直に評価しよう。PropStack が劣ってる点は取り入れる。

😈 **Red Team**: 敵の長所を徹底的に洗い出す。

| 機能 | Spring Boot | MicroProfile | Typesafe Config | owner | dotenv | **PropStack** |
|------|------------|-------------|----------------|-------|--------|--------------|
| 型安全キー | `@ConfigurationProperties` | `@ConfigProperty` | パス式 | interface proxy | ❌ | ✅ TypedKey enum |
| profile/環境切替 | ✅ | ✅ | ✅ | ❌ | ❌ | ✅ v0.5.0 |
| CLI args | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ v0.5.0 |
| 一括検証 | ❌ 1つずつ失敗 | ❌ | ❌ | ❌ | ❌ | ✅ `validate()` |
| YAML | ✅ | ✅ | ✅ (HOCON) | ❌ | ❌ | ❌ |
| ネスト構造 `db.host` | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ |
| リスト/配列 | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ |
| Secret マスク | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| ホットリロード | ❌ | ❌ | ✅ | ❌ | ❌ | ❌ |
| 0 依存 | ❌ (20+) | ❌ (CDI) | ❌ (Scala) | ❌ | ✅ | ✅ |
| 起動時間 | ❌ (数秒) | ❌ | ✅ | ✅ | ✅ | ✅ (<1ms) |
| デバッグ透明性 | ❌ (proxy) | ❌ (CDI) | ✅ | ❌ (proxy) | ✅ | ✅ |

### Gap 1: リスト / 配列

☕ **ヤン**: これは便利。カンマ区切りを `List<String>` にする機能。

```java
TypedKey<List<String>> ORIGINS = TypedKey.stringList("ALLOWED_ORIGINS");
List<String> origins = props.get(ORIGINS);
```

**判定: 採用。**

### Gap 2: Secret マスク

🎩 **千石**: 設定値をログに出す時にパスワードが平文で出るのは品質の問題です。

```java
TypedKey<String> PASSWORD = TypedKey.secret("DB_PASSWORD");
props.dump();  // DB_PASSWORD = ******
```

**判定: 採用。**

### Gap 3: 設定ダンプ / dump()

☕ **ヤン**: **全キーの一覧ダンプ** は欲しい:

```
SMTP_HOST = smtp.gmail.com
SMTP_PORT = 587 (default)
SMTP_PASSWORD = ****** (secret)
DB_NAME = [MISSING] ← 未設定
```

起動時にこれを出せば、どの設定がどこから来てるか一目瞭然。

**判定: 採用。**

### Gap 4: ソース追跡 / trace()

👤 **今泉**: 「この値、どこから来てるの？」って聞かれた時に答えられない。

```java
props.trace("DB_HOST");
// DB_HOST:
//   [0] set()               → (empty)
//   [1] SystemProperties    → (empty)
//   [2] EnvironmentVariables → prod-db  ← MATCH
```

**判定: 採用。** デバッグ時に最強。

### 却下

- **YAML** → 依存増。ドット区切り `.properties` で十分
- **ネスト構造** → ドット記法で既に動く
- **ホットリロード** → バグの温床。再起動が安全

### Red Team 最終レポート

😈 **Red Team**: PropStack が競合に **絶対勝ってる** ところ:

1. **`validate()` 一括検証** — Spring にはない。1 つずつ死ぬ
2. **TypedKey enum グルーピング** — 機能別カタログ。Spring にはない
3. **`trace()` でソース追跡** — Spring は `@Value` のソースが追えない
4. **`dump()` でシークレットマスク付き診断** — Spring は actuator が必要
5. **74 テスト、0 依存、<1ms 起動** — Spring は 20+ 依存、数秒起動

☕ **ヤン**: Spring にないものを 4 つ持ってる。これは「Spring の代替」じゃなくて **「Spring より良い部分がある独自のライブラリ」** だ。

---

## Part 2: defaultsTo vs describedAs (DD-008)

**作者の問題提起:** `TypedKey.string("DB_HOST", "localhost")` の localhost ってどういう使われ方？documentation? それとも default 値？ ... default 値としてはここで指定すべきなのか？下手に default 値を key に入れちゃうと設定不備が露呈しない。

👤 **今泉**: そもそも、TypedKey の `defaultValue` って2つの意味で使われてませんか？

```java
// A: 本当のデフォルト（これで動く）
PORT(TypedKey.integer("SMTP_PORT", 587))  // SMTP は 587 が標準。これで正しい。

// B: ドキュメント的な例示（これで動いたらまずい）
HOST(TypedKey.string("DB_HOST", "localhost"))  // 本番で localhost はまずい
```

🏥 **ハウス**: 全員嘘をついている。「デフォルト値」という概念自体が2つの別物を混同してる:

| | 安全なデフォルト | 危険なデフォルト |
|---|---|---|
| 例 | `SMTP_PORT=587` | `DB_HOST=localhost` |
| 本番で使える？ | YES | NO |
| `validate()` で検出？ | 不要 | **検出すべき** |
| 意図 | 仕様上の正解 | 開発の便宜 |

⚔ **リヴァイ**: **明示的なファクトリメソッド名で分ける**:

```java
// 安全なデフォルト（本番で使える値）
PORT(TypedKey.integer("SMTP_PORT").defaultsTo(587)),

// ドキュメントのみ（validate で検出される）
HOST(TypedKey.string("DB_HOST").describedAs("database hostname")),
```

🎩 **千石**: `dump()` の出力:

```
SMTP_PORT     = 587 (default)
DB_HOST       = [MISSING] — database hostname
DB_PASSWORD   = [MISSING] — app password (secret)
```

`DB_HOST` は description 付きで MISSING。localhost がデフォルトとして忍び込まない。

**作者のコメント:** Doc as code!

**→ DD-007: List, secret, dump, trace 採用。DD-008: defaultsTo/describedAs で意図を型で表現。Doc as code。**
