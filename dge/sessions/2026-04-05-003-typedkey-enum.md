# DGE Session: TypedKey の enum 設計

**Date:** 2026-04-05
**Decision:** [DD-003](../../docs/design-decisions.md#dd-003-typedkey--enum-with-typedkey-field-案-d)
**Cast:** ☕ ヤン + 👤 今泉 + ⚔ リヴァイ + 🎩 千石

## テーマ

PropertyKey に型情報を持たせて `getInt()` / `getString()` の呼び分けを不要にしたい。Java enum のジェネリクス制限をどう回避するか。

## 候補 4 案

| | 型安全 | enum 維持 | グルーピング |
|---|---|---|---|
| A: `TypedKey<T>` interface on enum | ✅ | ❌ (1型制限) | ❌ |
| B: static 定数 | ✅ | ❌ | ❌ |
| C: enum + converter | ❌ (Object) | ✅ | ✅ |
| **D: enum + TypedKey field** | **✅** | **✅** | **✅** |

## 核心の議論

⚔ リヴァイ: 案 A はダメ。Java の enum は 1 つのジェネリクスパラメータしか持てない。

☕ ヤン: 案 D — enum がフィールドとして `TypedKey<?>` を持てばいい。型情報は TypedKey の中に閉じ込める。

👤 今泉: enum の `values()` で一覧取得、機能別グルーピング、全部残る。

## 結論

```java
enum Smtp implements KeyHolder {
    HOST(TypedKey.string("SMTP_HOST")),
    PORT(TypedKey.integer("SMTP_PORT").defaultsTo(587));
    private final TypedKey<?> key;
    Smtp(TypedKey<?> key) { this.key = key; }
    public TypedKey<?> typedKey() { return key; }
}
```

**→ 案 D 採用。enum の網羅性 + 型安全 + 機能グルーピング全取り。**
