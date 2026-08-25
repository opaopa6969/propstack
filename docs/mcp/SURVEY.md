# PropStack — MCP 化調査 (Phase 1)

## 概要

PropStack は Java 21+ 向けの**ゼロ依存プロパティスタックライブラリ**。`PropStack`（カスケーディングプロパティ解決）と `Registry`（サービスロケーター）の2モジュールからなる。Maven Central に `org.unlaxer:propstack` として公開済み（v0.9.2）。インメモリのみで永続化を持たず、起動1ms未満で完了する。常駐プロセス・HTTP API・CLI はいずれも持たない。

## 判定と理由

**判定: `skip`（対応しない）**

- 純粋なJavaライブラリであり、エージェントが外からHTTP/stdioで呼ぶべき常駐能力を持たない
- 全機能はJVMプロセス内で完結し、起動1ms未満のため常駐サーバ化する価値がない
- プロパティ解決・バリデーション・dump/trace はアプリケーション組み込み用途であり、MCP toolとして公開してもエージェントが他サービスと組み合わせる絵が描けない
- spec/guideドキュメントは既にMarkdown群（SPEC.md 68KB, api-cookbook.md, getting-started.md, architecture.md, design-decisions.md）として充実しており、MCP resource化しても追加価値が薄い
- `.mcp.json` は issue-broker（別リポジトリ）の stdio クライアント設定であり、PropStack自体のMCPサーバではない
- Java 21+ が必要で、volta基盤（Node/Python中心）にJVMランタイムを追加するコストが見合わない

## 公開候補

| kind | name | io | 副作用 | 長時間 | 備考 |
|------|------|----|--------|--------|------|
| tool | resolve | `{key, sources[]} → Optional<String>` | read | No | プロパティ解決。JVM内で完結、外から呼ぶ意義薄 |
| tool | validate | `KeyHolder enum classes → missing keys[]` | read | No | 一括バリデーション。アプリ起動時用 |
| tool | dump | `KeyHolder enum classes → formatted string` | read | No | 診断出力（シークレットマスク付き）|
| tool | trace | `key → source-by-source report` | read | No | 値の由来追跡 |
| tool | registry_get_put | `class/name → instance` | write | No | コンポーネント登録・取得。JVM内オブジェクト |
| resource | spec | `propstack://spec` | — | — | SPEC.md が既存。resource化の追加価値薄 |
| resource | guide | `propstack://guide` | — | — | api-cookbook.md が既存 |

> 全候補は「技術的には定義できるが、エージェントが他サービスと組み合わせて呼ぶ絵が描けない」ため採用しない。

## 組み合わせ例

該当なし。PropStackの能力はJVMプロセス内でのみ意味を持ち、他のMCPサービスと有機的に組み合わせるユースケースが存在しない。

## 依存と協調

| repo | direction | capability | exists_now | note |
|------|-----------|------------|------------|------|
| issue-broker | depends_on | issue-broker MCP server (stdio) | Yes | `.mcp.json` が issue-broker の stdio クライアント設定を含む。プロパティ管理とは無関係 |

PropStack 自体は他のMCP入口に依存しないし、他に提供する入口もない。

## ライブラリのサーバ化

該当なし（`needed: false`）。起動1ms未満・インメモリのみ・JVM内完結のライブラリを常駐サーバ化するコスト（JVMランタイム追加、ポート管理、状態管理）に見合う価値がない。

## リスク

- プロパティ値にシークレットが含まれる可能性があり、リモートMCP経由で公開すると漏洩リスク
- Java 21+ が必要で、volta基盤にJVMランタイムを追加する運用コスト
- インメモリのみでセッションをまたぐ状態管理がないため、常駐プロセスにする意味がない

## 持ち主への質問

- PropStackを使ったアプリケーションの設定検証をエージェント経由で行いたいユースケースがあるか？（あれば `skill-only` で配布する手もある）
- spec/SPEC.md（68KB）は既に機械可読性が高いが、MCP resource として配布することに価値を見出すか
