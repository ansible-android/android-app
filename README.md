# Ansible для Android

<img src="Ansible/src/main/res/mipmap-xhdpi/ic_launcher.png" width="80" alt="Значок Ansible для Android">

Приложение мессенджера Ansible для телефонов и планшетов на Android. Это независимый **форк Telegram для Android**. Для обмена сообщениями приложение подключается к серверам Ansible и использует учётную запись Ansible. [Исходный проект Telegram](https://github.com/DrKLO/Telegram).

**[Скачать Ansible для Android](https://ansible.su/android)** · [Выпуски на GitHub](https://github.com/ansible-android/android-app/releases)

## Сборка APK

В репозитории используется Gradle Wrapper. Для сборки варианта Standalone подготовьте JDK 17, Android SDK Platform 36, Build Tools 36.0.0, NDK 27.2.12479018 и CMake 3.22.1. Эти версии также использует [сценарий сборки Android](.github/workflows/build-debug.yml).

```sh
git clone --recursive https://github.com/ansible-android/android-app.git
cd android-app
./gradlew :Ansible_AppStandalone:assembleAfatStandalone
```

Основной код клиента находится в [модуле Ansible](Ansible/). Варианты приложения и подключённые модули перечислены в [настройках Gradle](settings.gradle).

## Лицензия и авторство

Клиент распространяется по GNU GPL версии 2 или более поздней. Исходный код Telegram для Android сохраняет уведомления о правах его авторов. [Лицензия](LICENSE) · [Сведения об авторах](NOTICE).

[Сайт Ansible](https://ansible.su) · [История изменений](CHANGELOG.md) · [Сообщить о проблеме](https://github.com/ansible-android/android-app/issues)
