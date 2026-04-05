# DGE Session: fraud-alert ApplicationProperties からの機能移植

**Date:** 2026-04-05
**Decision:** [DD-005](../../docs/design-decisions.md#dd-005-features-from-fraud-alert-applicationproperties), [DD-006](../../docs/design-decisions.md#dd-006-stack-insertion--defaultsources-over-inserter)
**Cast:** ☕ ヤン + 👤 今泉 + ⚔ リヴァイ + 🏥 ハウス + 🎩 千石

## テーマ

fraud-alert の本家 ApplicationProperties にあって PropStack にない機能を評価。

## 評価結果

| 機能 | 判定 | 理由 |
|------|------|------|
| CLI args `--KEY=value` | **採用** | 実装コスト低、ユーザー価値高 |
| 明示的 profile | **採用** | transparent、Spring 互換的 |
| 自動検出 (user/host/os) | **オプトイン採用** | 便利だが暗黙的。作者は fraud-alert で活用していたが、新メンバーには魔法に見える |
| `validate()` 一括検証 | **採用** | Spring にない。全エラーを一度に報告 |
| `Populator` 自動バインド | 却下 | DI フレームワーク化 |

## スタック途中差し込み (DD-006)

5 案を評価:
- A: Inserter (predicate) → 内部実装依存
- B: 全手書き → 面倒
- C: インデックス → 脆い
- D: 名前付きソース → 過剰設計
- **E: `defaultSources()` を公開** → **採用**。新概念ゼロ。List 操作で全部できる。

☕ ヤン: ゼロ行の新 API が最強。

**→ DD-005: CLI args, profile, validate, auto-detect(opt-in) 採用。DD-006: defaultSources() 採用。**
