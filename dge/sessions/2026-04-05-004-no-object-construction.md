# DGE Session: PropStack でオブジェクト構築はしない

**Date:** 2026-04-05
**Decision:** [DD-004](../../docs/design-decisions.md#dd-004-no-object-construction-in-propstack)
**Cast:** ☕ ヤン + 👤 今泉 + ⚔ リヴァイ + 🏥 ハウス

## テーマ

`TypedKey<DataSource>` で任意オブジェクトを設定から取得する仕組みを入れるべきか。

## 却下された案

- **Converter 付き TypedKey** — 構築ロジックがキー定義に漏れる。責務違反。
- **FQDN リフレクション** — セキュリティリスク。黒魔術。

## 核心の議論

⚔ リヴァイ: `String → int` は型変換。`String → DataSource` は **構築**。別物だ。混ぜるな。

🏥 ハウス: 本当に欲しいのは PropStack と Registry の橋渡し。その橋は `main()` メソッド。

☕ ヤン: ゼロ行の実装が最強の実装。

## 結論

**→ やらない。PropStack は文字列を読む。Registry はオブジェクトを管理する。構築はアプリの責任。**
