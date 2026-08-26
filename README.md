# assets/launcher-icons

Launcher icon assets consumed by `.github/workflows/build-apk-debug.yml`.

Real Bin-Box branding (replaced the initial placeholder set). Structure and
filenames are dictated by the workflow's fetch/verify steps — keep them
exact if regenerating:

```
res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher.png
res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher_round.png
res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher_adaptive_back.png
res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher_adaptive_fore.png
res/mipmap-anydpi-v26/ic_launcher.xml
res/mipmap-anydpi-v26/ic_launcher_round.xml
play_store_512.png
1024.png
```

`ic_launcher_round.png` (legacy raster round icon, API < 26) and
`ic_launcher_round.xml` (adaptive-icon round variant) are derived from the
square source: round PNGs are a circular crop of `ic_launcher.png` per
density, and `ic_launcher_round.xml` duplicates `ic_launcher.xml` — both
standard practice since the adaptive-icon system applies its own mask.
