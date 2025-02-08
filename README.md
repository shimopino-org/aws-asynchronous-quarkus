# AWS Asynchronous Processing with Quarkus

このプロジェクトは、Quarkusフレームワークを使用してAWS Lambda関数を実装するためのモノリポジトリです。

## プロジェクト構造

```
.
├── build.gradle          # ルートプロジェクトのビルド設定
├── settings.gradle       # Gradleプロジェクトの設定
└── lambda-functions/    # Lambda関数のディレクトリ
```

## 必要条件

- Java 17以上
- Gradle 8.x
- AWS CLI（設定済み）

## ビルド方法

```bash
./gradlew build
```

## テスト実行

```bash
./gradlew test
```

## デプロイ方法

各Lambda関数のディレクトリで以下のコマンドを実行：

```bash
./gradlew build
``` 