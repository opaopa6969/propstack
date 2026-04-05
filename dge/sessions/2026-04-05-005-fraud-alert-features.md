# DGE Session: fraud-alert ApplicationProperties からの機能移植 + スタック途中差し込み

**Date:** 2026-04-05
**Decision:** [DD-005](../../docs/design-decisions.md#dd-005-features-from-fraud-alert-applicationproperties), [DD-006](../../docs/design-decisions.md#dd-006-stack-insertion--defaultsources-over-inserter)
**Cast:** ☕ ヤン + 👤 今泉 + ⚔ リヴァイ + 🏥 ハウス + 🎩 千石
**Context:** 作者の fraud-alert プロジェクトにある本家 ApplicationProperties はかなり高機能。必要な部分だけ propstack に移植したい。また、スタックの途中にカスタムソースを差し込みたいケースの設計。

---

## Part 1: 機能移植の評価

☕ **ヤン**: fraud-alert 版の ApplicationProperties が propstack より優れてる点を整理した。

### 1. コマンドライン引数 `--KEY=value`

👤 **今泉**: そもそも、これって `-D` と何が違うんですか？

☕ **ヤン**: `-DKEY=value` は JVM システムプロパティ。`--KEY=value` はアプリケーション引数。

```bash
# -D は JVM レベル。全ライブラリから見える
java -DDB_HOST=prod-db -jar app.jar

# -- はアプリ引数。main(String[] args) で受け取る
java -jar app.jar --DB_HOST=prod-db
```

⚔ **リヴァイ**: Spring Boot の `--server.port=8080` がこれだ。ユーザーにとって `-D` より直感的。

**判定: 採用。** 実装コスト低い。ユーザー価値高い。

### 2. 環境別プロパティファイル自動解決

```
classpath:
  application.properties                    ← 全環境共通
  application.host_silver-hawk.properties   ← このホスト名の時だけ
  application.user_opa.properties           ← このユーザーの時だけ
  application.os_linux.properties           ← Linux の時だけ
```

👤 **今泉**: これ、Spring の `application-{profile}.properties` と何が違うんですか？

☕ **ヤン**: Spring は **手動で profile を指定** する (`--spring.profiles.active=prod`)。fraud-alert は **自動検出** する。ホスト名・OS・ユーザー名から。

🏥 **ハウス**: ちょっと待て。これは **暗黙の振る舞い** じゃないか。

**作者のコメント:** 自動検出は良い仕組みだと思ったけど黒魔術的なの分かる。自分で作ってるから後輩に「自分の環境では○○が△△になってて...」みたいな話をされた時に「user_name で properties 作って書き換えたい項目だけ追加すればいいよ」って案内してた。便利だけど仕組み知らないとそう思うよね。

🎩 **千石**: **明示的にする方法** があります。profile を手動指定にする:

```java
PropStack props = new PropStack("myapp", "prod");
```

☕ **ヤン**: 折衷案 — 自動検出もオプトインで有効化できるようにする:

```java
// デフォルト: 明示的 profile のみ
PropStack props = new PropStack("myapp", "prod");

// オプトイン: 自動検出も有効化
PropStack props = new PropStack("myapp",
    PropertySource.forHost(),
    PropertySource.forUser(),
    PropertySource.forProfile("prod")
);
```

使う側が **意図的に選択** すれば黒魔術じゃない。

**判定: 明示的 profile 採用。自動検出はオプトイン。**

### 3. `validate()` 一括検証

☕ **ヤン**: 今の propstack だと `require()` を個別に呼ぶ。問題は **最初の1つで止まる**。

⚔ **リヴァイ**: 実用性が高い。起動時に全設定を検証して、足りないのを全部リストで出す:

```java
props.validate(Smtp.class, Db.class);
// → IllegalStateException: Missing required properties: [SMTP_HOST, DB_NAME, DB_PASSWORD]
```

**判定: 採用。**

---

## Part 2: スタック途中差し込み (DD-006)

**作者の提案:** fraud-alert 版でちょっと設計に悩んでいたのはスタックの途中に差し込みたい時どうしようって話。

☕ **ヤン**: 5つの案:

### 案 A: Inserter パターン (fraud-alert 方式)

```java
new ApplicationProperties(args,
    PropertyResolverInserter.after(SystemEnvironmentPropertyResolver.class, vaultSource)
);
```

問題: 内部実装の順番を知ってないと使えない。

### 案 B: 全部コンストラクタで順番指定

問題: デフォルト構成が使えなくなる。毎回全部書く。

### 案 C: インデックス指定

問題: 脆い。内部順番が変わったら壊れる。

### 案 D: 名前付きソース + 位置指定

問題: 過剰設計。

### 案 E: defaultSources() を公開するだけ

🏥 **ハウス**: そもそもこの機能、**本当に必要か？** PropStack のユーザーの何割がスタック途中差し込みを必要とする？

☕ **ヤン**: 正直、1% くらい。

🏥 **ハウス**: じゃあ **案 B を簡単にする** 方がいい。デフォルト構成をコピペで書けるヘルパーを用意する:

```java
var sources = PropStack.defaultSources("myapp");
sources.add(2, vaultSource);  // 普通の List 操作
PropStack props = new PropStack(false, sources.toArray(PropertySource[]::new));
```

☕ **ヤン**: おお、これいい。新しい API を作らなくて済む。`List` 操作で全部できる。

⚔ **リヴァイ**: `ArrayList` だから `add(index, element)` で差し込みも `remove` も自由。新しい API 不要。Java の標準 API で操作する。

🎩 **千石**: これが最も PropStack らしい解決です。新しい概念を作らない。既存の Java の仕組みで解決する。

**→ DD-005: CLI args, profile, validate, auto-detect(opt-in) 採用。DD-006: defaultSources() 採用。新概念ゼロ。**
