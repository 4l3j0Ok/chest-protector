<p align="center">
  <img src="docs/assets/logo.png" alt="Chest Protector logo" width="320">
</p>

# Chest Protector — Fabric 1.21.1

Fabric 1.21.1 mod that adds PIN-protected vanilla chests.

## Features

- Password Protector item with a key-like item texture
- Crafting recipe for the Password Protector
- Creative inventory registration so recipe viewers such as JEI can discover the item
- 1-6 digit numeric PIN GUI
- English and Spanish translations
- Chest and trapped chest support
- Double chest support: protecting either half locks the whole inventory
- Server-side PIN validation
- Owner-only breaking for protected chests
- Hopper insertion and extraction blocked while a chest is protected
- Explosion protection for locked chests, preventing TNT/creeper bypasses
- Client sync for the locked state and owner name without sending the PIN

## Credits

Chest Protector uses [Ward & Watch](https://github.com/JuanSebLopez/Ward-Watch) by JuanSebLopez as its base. Credit goes to the original Ward & Watch project and author for the initial concept and implementation this backport builds on.

## Build

Requires Java 21.

```bash
./gradlew build
```

The build uses the included Gradle wrapper and Java 21. Source API targets were adapted against Yarn 1.21.1+build.3.
