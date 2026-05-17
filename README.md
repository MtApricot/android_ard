# android_ard

Android スマートフォンの音声認識で詐欺関連キーワードを検知し、Bluetooth 経由で Arduino ボードの LED を点灯させるシステムです。通話や会話の内容をリアルタイムで監視し、疑わしい発言が検出された場合にユーザーへ警告表示と物理的なアラート（LED）を行います。

---

## 概要

| 項目 | 内容 |
|------|------|
| 目的 | 音声から詐欺の可能性があるキーワードを検出し、警告と Arduino LED による通知 |
| 構成 | Android アプリ + Arduino ファームウェア |
| 通信方式 | Bluetooth Classic（SPP: Serial Port Profile） |
| パッケージ名 | `com.example.myapplication` |

### 動作フロー

```
[ユーザー] ボタンタップ → 音声認識開始
       ↓
[MainActivity] 認識テキストを解析
       ↓ キーワード検出時（金・銀行・カード・誰・返済）
  「詐欺の可能性があります N回目」を表示
  sw = 1 に設定
       ↓
[MyService] sw==1 を監視 → Bluetooth で "1" を送信
       ↓
[Arduino] シリアル受信 → LED（ピン13）ON
```

---

## ディレクトリ構造

リポジトリに含まれるファイル・ディレクトリを、実際の構成に合わせて記載しています（`.git/`、`build/`、`.gradle/` などビルド生成物は除く）。

```
android_ard/
├── .gitignore
├── README.md
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradlew
├── gradlew.bat
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── .idea/                             # Android Studio 設定（一部のみリポジトリに含まれる）
│   ├── .gitignore
│   ├── .name
│   ├── compiler.xml
│   ├── gradle.xml
│   └── misc.xml
├── arduino/
│   └── arduino.ino
└── app/
    ├── .gitignore
    ├── build.gradle
    ├── proguard-rules.pro
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── java/com/example/myapplication/
        │   │   ├── MainActivity.java
        │   │   ├── BluetoothChatFragment.java
        │   │   ├── BluetoothConnection.java
        │   │   ├── DeviceListActivity.java
        │   │   └── MyService.java
        │   └── res/
        │       ├── layout/
        │       │   ├── activity_main.xml
        │       │   ├── fragment_bluetooth_chat.xml
        │       │   ├── activity_device_list.xml
        │       │   └── device_name.xml
        │       ├── menu/
        │       │   └── bluetooth_menu.xml
        │       ├── values/
        │       │   ├── strings.xml
        │       │   ├── colors.xml
        │       │   ├── themes.xml
        │       │   └── ids.xml
        │       ├── values-night/
        │       │   └── themes.xml
        │       ├── drawable/
        │       │   └── ic_launcher_background.xml
        │       ├── drawable-v24/
        │       │   └── ic_launcher_foreground.xml
        │       ├── mipmap-anydpi-v26/
        │       │   ├── ic_launcher.xml
        │       │   └── ic_launcher_round.xml
        │       ├── mipmap-hdpi/
        │       │   ├── ic_launcher.webp
        │       │   └── ic_launcher_round.webp
        │       ├── mipmap-mdpi/
        │       │   ├── ic_launcher.webp
        │       │   └── ic_launcher_round.webp
        │       ├── mipmap-xhdpi/
        │       │   ├── ic_launcher.webp
        │       │   └── ic_launcher_round.webp
        │       ├── mipmap-xxhdpi/
        │       │   ├── ic_launcher.webp
        │       │   └── ic_launcher_round.webp
        │       ├── mipmap-xxxhdpi/
        │       │   ├── ic_launcher.webp
        │       │   └── ic_launcher_round.webp
        │       └── xml/
        │           ├── backup_rules.xml
        │           └── data_extraction_rules.xml
        ├── test/java/com/example/myapplication/
        │   └── ExampleUnitTest.java
        └── androidTest/java/com/example/myapplication/
            └── ExampleInstrumentedTest.java
```

---

## 使用技術

### Android アプリ

| カテゴリ | 技術・バージョン |
|----------|------------------|
| 言語 | Java 8 |
| ビルド | Gradle 7.4 / Android Gradle Plugin 7.3.1 |
| compileSdk | 33 |
| minSdk / targetSdk | 32 |
| UI | AndroidX AppCompat, Material Components, ConstraintLayout |
| アーキテクチャ | Activity + Fragment + Foreground Service |
| Bluetooth | Bluetooth Classic / RFCOMM（SPP UUID） |
| 音声 | `RecognizerIntent`（Android 標準音声認識） |
| 権限 | `BLUETOOTH`, `BLUETOOTH_ADMIN`, `BLUETOOTH_CONNECT`, 位置情報, `FOREGROUND_SERVICE` |

#### 主要ライブラリ（`app/build.gradle`）

```gradle
implementation 'androidx.appcompat:appcompat:1.6.1'
implementation 'com.google.android.material:material:1.9.0'
implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
```

#### Bluetooth SPP UUID

```
00001101-0000-1000-8000-00805F9B34FB
```

標準的なシリアルポートプロファイル（HC-05 / HC-06 等のモジュールで一般的に使用）。

### Arduino

| 項目 | 内容 |
|------|------|
| 言語 | Arduino C++（`.ino`） |
| シリアル速度 | 9600 bps |
| LED ピン | 13（内蔵 LED または外部 LED） |
| 受信コマンド | `'1'` → LED ON / `'0'` → LED OFF |

### ハードウェア想定

- **Android 端末**: API 32（Android 12L）以上
- **Arduino ボード**: Uno 等
- **Bluetooth モジュール**: HC-05 / HC-06 等（Arduino とシリアル接続、Android と SPP ペアリング）

---

## 主要コンポーネント

### MainActivity

- `BluetoothChatFragment` を `main_layout` にホスト
- ボタンタップで `RecognizerIntent` による音声認識を連続実行
- 認識結果に以下のキーワードが含まれると詐欺警告:
  - `金` / `銀行` / `カード` / `誰` / `返済`
- 検出時に `sw = 1` を設定し、検出回数を画面に表示
- Android 12+ 向けに `BLUETOOTH_CONNECT` ランタイム権限を要求

### BluetoothChatFragment

- Bluetooth の有効化確認
- オプションメニューから `DeviceListActivity` を起動し接続先 MAC アドレスを取得
- **Start**: `MyService` をフォアグラウンドで起動（`LED_STATUS=1` を付与）
- **Stop**: `MyService` を停止

### BluetoothConnection

- 指定 MAC アドレスのデバイスへ RFCOMM ソケットで接続
- `connectionInterface` コールバックで接続成功/失敗を通知
- `InputStream` / `OutputStream` を提供

### MyService

- フォアグラウンドサービスとして通知チャネルを表示（Android 8.0+）
- Bluetooth 接続後、`ConnectedThread` が 1 秒間隔で `MainActivity.getSw()` を監視
- `sw == 1` のとき Arduino へ `"1"`（バイト）を送信してループ終了
- サービス停止時（`cancel`）は `"0"` を送信して LED を消灯

### DeviceListActivity

- ペアリング済み Bluetooth デバイスをリスト表示
- 選択したデバイスの MAC アドレス（末尾 17 文字）を Intent で返却

### arduino.ino

- シリアルで `'1'` / `'0'` を受信し、ピン 13 の LED を HIGH / LOW に制御
- 状態を `Serial.println("ON")` / `"OFF"` でデバッグ出力

---

## セットアップ

### 前提条件

- [Android Studio](https://developer.android.com/studio)（推奨: Hedgehog 以降でも可）
- JDK 8 以上
- Arduino IDE（スケッチ書き込み用）
- Bluetooth モジュール付き Arduino

### Arduino

1. `arduino/arduino.ino` を Arduino IDE で開く
2. Bluetooth モジュールを Arduino のシリアルピン（通常 TX/RX）に接続
3. LED をピン 13 に接続（内蔵 LED のみでも可）
4. ボード・ポートを選択して書き込み
5. Android 端末と Bluetooth モジュールをペアリング（PIN はモジュールの仕様に従う）

### Android

1. リポジトリをクローン
2. Android Studio でプロジェクトを開く
3. Gradle Sync を実行
4. API 32 以上の実機またはエミュレータにインストール
5. アプリ起動後:
   - Bluetooth および位置情報の権限を許可
   - メニュー「Connect a device」で Arduino の Bluetooth モジュールを選択
   - **Start** でサービス開始
   - 画面のボタンで音声認識を開始

---

## ビルド・実行

```bash
# デバッグ APK のビルド
./gradlew assembleDebug

# 接続済みデバイスへインストール
./gradlew installDebug
```

Android Studio からは **Run**（▶）で `app` モジュールを実行できます。

---

## 権限一覧

`AndroidManifest.xml` で宣言されている権限:

| 権限 | 用途 |
|------|------|
| `BLUETOOTH` / `BLUETOOTH_ADMIN` | Bluetooth 通信（レガシー） |
| `BLUETOOTH_CONNECT` | Android 12+ での接続・スキャン |
| `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` | Bluetooth デバイス検出（OS 要件） |
| `FOREGROUND_SERVICE` | バックグラウンドでの Bluetooth 維持 |

---

## 制限事項・注意

- **minSdk 32** のため、Android 12L 未満の端末ではインストールできません。
- 音声認識は端末の Google 音声認識等に依存します。未インストールの場合はエラー表示になります。
- キーワード検出は単純な部分文字列一致（`String.contains`）です。誤検知・見逃しがあり得ます。
- 詐欺防止の補助ツールであり、法的・セキュリティ上の保証はありません。
- Bluetooth 接続中は Arduino 側モジュールの電源とペアリング状態を維持してください。

---

## テスト

```bash
# ユニットテスト
./gradlew test

# 計装テスト（エミュレータ/実機が必要）
./gradlew connectedAndroidTest
```

現状、テストクラスは Android Studio 生成時のテンプレートのみです。

---

## ライセンス

リポジトリに LICENSE ファイルが含まれていない場合、利用・再配布前に作者へ確認してください。

---

## 関連ファイル早見表

| ファイル | 役割 |
|----------|------|
| `MainActivity.java` | 音声認識・キーワード判定・UI |
| `BluetoothChatFragment.java` | Bluetooth 操作 UI |
| `BluetoothConnection.java` | 低レベル Bluetooth 接続 |
| `MyService.java` | バックグラウンド送信 |
| `DeviceListActivity.java` | デバイス選択 |
| `arduino.ino` | LED 制御ファームウェア |
