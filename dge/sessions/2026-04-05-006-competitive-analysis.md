# DGE Session: 競合分析 + defaultsTo vs describedAs

**Date:** 2026-04-05
**Decision:** [DD-007](../../docs/design-decisions.md#dd-007-competitive-analysis--features-adopted-from-dge-review), [DD-008](../../docs/design-decisions.md#dd-008-defaultsto-vs-describedas--doc-as-code)
**Cast:** ☕ ヤン + 👤 今泉 + ⚔ リヴァイ + 🎩 千石 + 😈 Red Team

## テーマ 1: 競合との比較 (DD-007)

Spring Boot, MicroProfile, Typesafe Config, owner, dotenv と比較。Red Team がギャップを特定。

### 採用した機能

| 機能 | 根拠 |
|------|------|
| `TypedKey.stringList()` | Spring にある。origins, tags に便利 |
| `TypedKey.secret()` | パスワードのログ漏洩防止 |
| `dump()` | Spring にない一行診断。secret マスク付き |
| `trace()` | Spring にできない。どのソースから値が来たか表示 |

### 却下した機能

- YAML → 依存増。ドット区切り `.properties` で十分
- ホットリロード → バグの温床。再起動が安全
- IDE メタデータ JSON → 過剰

### PropStack が Spring に勝つ点（Red Team 確認）

1. `validate()` — 全不足キー一括報告
2. TypedKey enum グルーピング — 機能別カタログ
3. `trace()` — ソース追跡
4. `dump()` — シークレットマスク付き診断
5. 74 テスト、0 依存、<1ms 起動

## テーマ 2: defaultsTo vs describedAs (DD-008)

### 問題

`TypedKey.string("DB_HOST", "localhost")` は曖昧。localhost は安全なデフォルト？開発の仮値？

👤 今泉: デフォルト値が設定不備を隠す。

🏥 ハウス: 「デフォルト値」が2つの別物を混同してる:
- 安全なデフォルト (`SMTP_PORT=587`) — RFC で決まってる
- 危険なデフォルト (`DB_HOST=localhost`) — 本番で事故る

### 結論

```java
PORT(TypedKey.integer("SMTP_PORT").defaultsTo(587))       // 安全。validate() スキップ
HOST(TypedKey.string("DB_HOST").describedAs("hostname"))   // doc のみ。validate() で検出
PASSWORD(TypedKey.secret("DB_PASSWORD"))                    // 必須。dump() でマスク
```

**→ DD-007: List, secret, dump, trace 採用。DD-008: defaultsTo/describedAs で意図を型で表現。Doc as code。**
