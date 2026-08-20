# Ansible for Android

Android client for the [Ansible](https://ansible.su) messaging service.

> **This project is a fork of [Telegram for Android](https://github.com/DrKLO/Telegram).**
> It is licensed under [GPL v2 (or later)](LICENSE), the same terms as
> the upstream project. We are grateful to the Telegram for Android
> Authors for their work — without it this fork would not exist.
>
> Ansible for Android is **not affiliated with, endorsed by, or
> sponsored by Telegram FZ-LLC**. It connects to Ansible servers, not
> Telegram servers, and cannot be used to access Telegram accounts.

[![License: GPL v2](https://img.shields.io/badge/License-GPLv2-blue.svg)](LICENSE)
[![Upstream](https://img.shields.io/badge/forked%20from-Telegram--Android-orange.svg)](https://github.com/DrKLO/Telegram)

---

## What this is

Ansible for Android is the official Android client for the Ansible
messenger. It is built on top of the Telegram for Android codebase
under GPL v2, with the following high-level modifications:

- Networking layer rewritten to use the **MVSy 1.0** protocol and
  connect to Ansible backend servers (instead of MTProto 2.0 / Telegram
  DCs).
- Branding, visual identity, and product naming replaced throughout.
- Telegram-specific features removed where not applicable to Ansible
  (e.g., Telegram Premium subscriptions, Telegram Stars, Fragment
  integration, sponsored messages).
- Additional features added that are unique to Ansible.

The complete list of changes from upstream is tracked in
[`CHANGELOG.md`](CHANGELOG.md).

## Relationship to upstream

| | Telegram for Android | Ansible for Android |
|---|---|---|
| License | GPL v2 (or later) | GPL v2 (or later) — same |
| Backend | Telegram DCs | Ansible servers (`mvsy.ansible.su`) |
| Protocol | MTProto 2.0 | MVSy 1.0 |
| Trademarks | Telegram | Ansible |
| Account compatibility | Telegram accounts | Ansible accounts (separate system) |
| Source repository | [DrKLO/Telegram](https://github.com/DrKLO/Telegram) | [behappy-android/Telegram](https://github.com/behappy-android/Telegram) |
| Package id | `org.telegram.messenger` | `rest.behappy.android` |

We do **not** merge updates from upstream automatically. The fork is
independently maintained.

## Compliance with upstream README requirements

The upstream Telegram-Android README requests the following:

| Upstream request | Status in this fork |
|---|---|
| Obtain your own api_id | N/A — Ansible uses MVSy 1.0, not MTProto |
| Don't use the name "Telegram" | ✅ Rebranded to Ansible |
| Don't use Telegram's logo | ✅ Independent Ansible logo |
| Follow security guidelines | ✅ Adopted from upstream |
| Publish your code | ✅ This repository is public |

## Downloads

The latest Ansible for Android builds are available at:

- Direct APK: <https://ansible.su/android>
- Source release archives: <https://github.com/behappy-android/Telegram/releases>

For Telegram for Android (the upstream project) please visit
[the upstream repository](https://github.com/DrKLO/Telegram).

## Building from source

Build instructions are inherited from upstream. The only change is
configuration:

1. Use the Ansible backend endpoint instead of Telegram DCs (configured
   in the resources).
2. Bundle the Ansible app icon and resources in place of Telegram's.

See the upstream repository for the original build instructions.

## License

Ansible for Android is free software: you can redistribute it and/or
modify it under the terms of the **GNU General Public License v2 (or
any later version)** as published by the Free Software Foundation.

- Full license text: [LICENSE](LICENSE)
- Attribution and trademark notice: [NOTICE](NOTICE)

By contributing to this repository, you agree that your contributions
will be licensed under the same terms.

## Trademarks

"Telegram" is a trademark of Telegram FZ-LLC. It is used in this README
and in source code copyright headers solely to identify the upstream
project from which this fork is derived, as required by GPL §1. It is
**not** used as a trademark of this product.

"Ansible" is a trademark of the Ansible Android Authors.

## Contact

- General: <https://ansible.su>
- Source code questions: open an issue on this repository
- License compliance / DMCA: <legal@ansible.su>
