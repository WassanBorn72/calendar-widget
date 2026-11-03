# Android の正式版配布

アプリ ID: `io.github.wassanborn72.hitohi`。初回は v1.0.0 / versionCode 1。

## 署名鍵

秘密鍵・パスワードはリポジトリ、GitHub Releases、ブラウザ配信ディレクトリに入れないでください。初回の鍵はチェックアウト外の `/workspace/calendar-widget-signing/` に作成しています。この環境以外でも更新を続けられるよう、keystore とパスワードファイルを安全な保管先へバックアップしてください。鍵を失うと同じアプリへの更新を配布できません。キーのエイリアスは `hitohi`、形式は JKS です。

## ビルドと検証

JDK 17以上、Gradle 8.11.1、SDK Platform 35、Build Tools 35.0.0 を使用します。パスワードそのものをコマンドに書かず、権限600のパスワードファイルを使います。

```sh
export HITOHI_KEYSTORE=/secure/path/calendar-widget-signing/hitohi_release.jks
export HITOHI_PASSWORD_FILE=/secure/path/calendar-widget-signing/hitohi_stone_password.txt
export ANDROID_HOME=/path/to/android-sdk
sh scripts/build-release.sh
```

更新時は versionCode を増やし、versionName を更新します。旧リリースと署名証明書の SHA-256 が同じことを確認してください。公開前に APK の署名検証、Lint、時間配置テストとブラウザテストを実施します。端末固有の検証結果は、実行済みと未実行を分けてリリースノートに記載します。

## GitHub Releases

署名済み APK と hitohi_SHA256SUMS、署名証明書情報と `hitohi_release.json` を `release-assets/v<version>/` に置きます。このフォルダーの APK は直接配布用の公開成果物で、秘密鍵は含みません。ソース変更と同じリポジトリで配布対象を固定するために保存します。

バージョンと同じタグを作成して push すると `Release Android` が動きます。ワークフローはソースのビルド・Lint・時間配置テスト、APK のチェックサム・署名・アプリ ID・バージョン・非デバッグ設定を確認してから GitHub Release を作成します。実行失敗時は既存リリースを書き換えず停止します。GitHub Actions とリポジトリ内の Releases 作成権限が必要です。

公開された Release ページと添付 APK のダウンロードを確認して初めて、外部公開完了と扱ってください。タグを push しただけでは公開成功を意味しません。


署名フォルダーには `hitohi_release.jks`、`hitohi_key_alias.txt`、`hitohi_key_password.txt`、`hitohi_keystone_base64.txt`、`hitohi_stone_password.txt`、`hitohi_PUBLIC_CERTIFICATE_SHA256.txt` を保存しています。`hitohi_keystone_base64.txt` は JKS 本体の Base64、`hitohi_stone_password.txt` は keystore のパスワードです。Base64 は暗号化ではありません。公開してよいのは証明書フィンガープリントだけです。ファイル名は指定に合わせた表記です。
