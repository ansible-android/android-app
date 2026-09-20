# Changelog

All notable changes to Ansible for Android will be documented in this
file.

Changes inherited from upstream Telegram for Android are not repeated
here. This changelog covers only modifications made by the Ansible
Android Authors.

The format is based on [Keep a Changelog](https://keepachangelog.com/),
and this project adheres to [Semantic Versioning](https://semver.org/).

## [0.1.4] - 2026-08-21

### Fixed (works in Russia without VPN)
- **Removed the Telegram DPI-evasion backup cascade** that got the app blocked
  by RU DPI. The primary obfuscated2 stream to 85.193.80.91:10443 is identical
  to the desktop client (which is not blocked), so the differentiator was the
  Android-only auxiliary traffic: `onRequestNewServerIpAndPort` fired
  `FirebaseTask` (ipconfig from Telegram's Firebase project), `GoogleDnsLoadTask`
  / `MozillaDnsLoadTask` (DoH), and a `ResolveHostByNameTask` that fronted DNS
  over `https://www.google.com/resolve` with a forged `Host: dns.google.com`
  header. That mix (Google/Mozilla DoH + SNI≠Host fronting), which fires
  aggressively once `clientBlocked` drops the retry threshold to 5s, is a
  TSPU-visible circumvention fingerprint. `onRequestNewServerIpAndPort` is now a
  no-op (the seed DC is a fixed IP; `help.getConfig` supplies the real
  dc_options after handshake), and `ResolveHostByNameTask` uses plain system DNS
  — matching what the desktop fork already did.
- **Stopped port-hopping to 443 / 5222.** `Datacenter.h defaultPorts` was
  `{-1,443,5222,-1}`, so on connection trouble the client presented high-entropy
  obfuscated2 on 443 (where DPI expects TLS) and on the legacy Telegram port
  5222 — a second block trigger. Now `{-1,-1,-1,-1}` (always the configured
  10443, like desktop).
- Version 0.1.4 (`versionName 0.1.4`, `versionCode 3001004`).

### Known follow-up
- `google-services.json` is still Telegram's Firebase project (`tmessages2`);
  startup FCM registration binds the device to Telegram's project (a data leak,
  not the DPI block, and push is already broken because the backend can't push
  to those tokens). Replace with the Ansible Firebase project.

## [0.1.3] - 2026-08-21

### Changed
- **Only two languages (English + Russian).** The language settings listed ~10
  because `LocaleController` hard-codes a built-in `LocaleInfo` list independent
  of the server's `langpack.getLanguages` (which already returns just en/ru).
  Removed the eight non-en/ru built-in entries (it, es, de, nl, ar, pt-BR, ko,
  uk) and deleted their bundled `values-*` string resources; the config-qualifier
  dirs (night, sw600dp, v21…) and `values-ru` stay. A device set to a dropped
  locale now falls back to English (both supported languages come from the server
  langpack: full English, ~73% Russian).
- Version 0.1.3 (`versionName 0.1.3`, `versionCode 3001003`).

## [0.1.2] - 2026-08-21

### Changed
- **Animated stickers use the brand format only.** The MIME
  `application/x-tgsticker` and the `.tgs` extension are gone; the client
  now recognises and produces `application/x-ansible-sticker` / `.ass`
  exclusively (matching the backend `me`-side output and the desktop
  client, which accept only the brand MIME). Legacy `tgs` is no longer
  accepted. The internal Lottie ext hint `tgs` → `ass` across all
  producers and consumers; the local invalid-marker `x-bad-tgsticker`
  was rebranded too.
- **Dice** used a Telegram-internal placeholder MIME
  `application/x-tgsdice`; rebranded to `application/x-ansible-dice`
  (a client-only synthetic marker — the dice document is a local
  placeholder with no server round-trip, so this never touches the
  wire). Dice-face animations themselves are ordinary
  `x-ansible-sticker` Lottie, as the backend serves them.
- Version 0.1.2 (`versionName 0.1.2`, `versionCode 3001002`).

### Notes
- Left untouched (separate features, backend does not emit them):
  `x-tgwallpattern` (wallpaper pattern), `x-tgstoryboard(map)` (video
  seek preview). The `"tgs":1` field inside bundled `res/raw/*.json`
  Lottie files is a Lottie-format flag, not the sticker MIME, and is
  intentionally kept.
- `google-services.json` is still Telegram's Firebase project
  (`tmessages2`) — to be replaced with the Ansible Firebase config in a
  follow-up release once that project's config is available.

## [0.1.1] - 2026-08-21

### Changed
- **Datacenter bootstrap** now points at the live production LB
  `85.193.80.91:10443` (SPB). The dead seeds `144.31.238.115` /
  `144.31.221.5` are removed. Host/port matches the desktop client and
  the `help.getConfig` the cluster serves (`this_dc=1`, dc1..5 →
  `85.193.80.91:10443`); the CDN datacenter (`195.133.31.208:5222`) is
  discovered at runtime via `help.getConfig`.
- **Deep-link domain** unified to `asme.su` (the messenger short-link
  domain). Link recognition (`t.me`, `behappy.me`, `behappy.dog`),
  generation (usernames, invites `+`, `joinchat`, `addstickers`,
  `addemoji`, `giftcode`, `folder`, `boost`, `call`, `c/`, `m/`,
  `BotFather`, `spambot`, `premiumbot`, `proxy`, `socks`) and the
  `PREFIX` handle-subdomain pattern all resolve to `asme.su`. Website
  links (`blog`, `tour`, `iv`, `faq`, `privacy`, `embed`, `dl`,
  `instant-view.`, `core.`, `messenger.`) stay on `ansible.su`.
- **URI scheme** changed from `tg://` to `as://` (and `tgb` → `asb`),
  matching the desktop client and backend (`mvsy_link_page_handler`
  emits `as://…`). Manifest `BROWSABLE` filters register `asme.su`,
  `*.asme.su`, and the `as` / `asb` schemes.
- **App name** is now "Ansible" (was "Telegram" in the built-in string
  fallbacks; server language packs remain authoritative at runtime).
- **App icon** replaced with the Ansible brand mark across the default,
  standalone (`_sa`) and alternate icon sets (legacy, adaptive
  foreground, and round). Telegram-blue adaptive backgrounds and the
  Telegram paper-plane monochrome layer are removed.
- Version bumped to `0.1.1` (`versionName 0.1.1`, `versionCode 3001001`,
  matching the cross-client scheme `3000000 + minor*1000 + patch`).

### Fixed
- Deep-link parser was dead after the scheme migration: the `switch
  (scheme)` cases in `LaunchActivity` and `LinkManager.isWebAppLink`
  were still labelled `case "tg"`, so every `as://` link (usernames,
  invites, stickers, QR login, bot buttons, in-app settings links) was
  silently dropped. Relabelled to `case "as"`.
- `Browser.extractUsername` used the old Telegram `substring` offsets
  after the host string was rebranded, corrupting extracted usernames;
  offsets now match `asme.su/` (8 / 15 / 16).
- Story, boost, premium/star invoice and NFT-gift links were still built
  on `ansible.su` (opened the website in a browser instead of the in-app
  flow); now `asme.su`.
- `Browser.isInternalUri` had a dead duplicated host branch and forced
  usernames starting with `blog`/`faq` to the external browser; instant-
  view (`/iv`) recognition disagreed between call sites. Cleaned up.
- Google Assistant App Actions (`shortcuts.xml`) and localized app-name
  overrides still emitted `tg://` / "Telegram"; both migrated.

### Rebrand (icon & name)
- App name is "Ansible" in the default and all nine localized string
  fallbacks (previously the launcher label showed "Telegram" on ru/de/
  es/it/nl/pt/uk/ar/ko devices).
- Replaced the Telegram wordmark shown on the chats screen and stories
  header (`telegram_logo_2`) and the first-run intro (`telegram_logo`,
  `intro_tg_plane`) with the Ansible mark, and every notification's
  status-bar icon (`notification.png`) with a white Ansible glyph.
- Rebranded gallery/download folder names, system-contacts action
  labels, service-notification user names and support e-mail addresses
  (`*@stel.com` → `*@ansible.su`).

### Security / MTProto
- Neutralised the Telegram ipconfig fallback that could poison the DC
  list: the `help.configSimple` trusted key in `Datacenter.cpp` is now
  an Ansible-owned key (Telegram-signed fallback payloads from the
  Firebase remote-config / DoH path are rejected fail-closed), and the
  `dcDomainName` DoH defaults point at `apv3.ansible.su` instead of
  `apv3.stel.com`.
- Payment-webview `BLACKLISTED_PROTOCOLS` now blocks `as` (and `tg`), so
  a third-party payment page cannot launch app deep links mid-payment.
- Internal-URL host check tightened from `endsWith("ansible.su")` to an
  exact/subdomain match (no `evilansible.su` false positive).
- Built-in server RSA public key stays the rotated key
  (`MIIBCgKCAQEAvqCL9IFB…`, fingerprint `0xDDCF36F8466E4286`), the only
  key the live cluster holds the private half for. No legacy key/
  fingerprint is shipped, so `selectPublicKey` cannot pick a stale key.

### Known follow-ups (not blocking 0.1.1)
- `google-services.json` still carries Telegram's Firebase project; its
  remote-config payload is now rejected (above), but installs still phone
  home to Telegram's Firebase until an Ansible project config is dropped in.
- The round-video watermark Lottie (`raw/plane_logo_plain.json`) is still
  the Telegram plane; replace with an Ansible logo Lottie or disable it.
- Stars-transaction detail links still point at `fragment.com`.

[0.1.4]: https://github.com/behappy-android/Telegram/compare/v0.1.3...v0.1.4
[0.1.3]: https://github.com/behappy-android/Telegram/compare/v0.1.2...v0.1.3
[0.1.2]: https://github.com/behappy-android/Telegram/compare/v0.1.1...v0.1.2
[0.1.1]: https://github.com/behappy-android/Telegram/compare/v0.1.0...v0.1.1
[0.1.0]: https://github.com/behappy-android/Telegram/releases/tag/v0.1.0
