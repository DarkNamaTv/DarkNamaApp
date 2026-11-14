# Fonts (bundled Persian, optional English)

The app ships with Vazirmatn for Persian text and also offers the system default. **No optional English TTFs are included in this repository.** To enable an English family, supply genuine, correctly licensed TTF files in `app/src/main/res/font/`. Filenames must match **exactly** (lowercase):

| English family | File names |
| --- | --- |
| Inter | `inter_regular.ttf`, `inter_medium.ttf`, `inter_semibold.ttf`, `inter_bold.ttf` |
| Poppins | `poppins_regular.ttf`, `poppins_medium.ttf`, `poppins_semibold.ttf`, `poppins_bold.ttf` |
| Montserrat | `montserrat_regular.ttf`, `montserrat_medium.ttf`, `montserrat_semibold.ttf`, `montserrat_bold.ttf` |
| Nunito | `nunito_regular.ttf`, `nunito_medium.ttf`, `nunito_semibold.ttf`, `nunito_bold.ttf` |
| Lato | `lato_regular.ttf`, `lato_medium.ttf`, `lato_semibold.ttf`, `lato_bold.ttf` |

Only files actually present in `res/font` are compiled into the APK. **Do not download, generate, rename another font to one of these names, or add dummy resources.** A completely absent English family is hidden from the picker. An absent selected English family falls back safely to the system font while its choice stays saved; missing weights use the nearest present file (lower on a tie). Retired Persian selections are instead reset to the system default and saved as such. Both the app's typography and the player's subtitles follow the active app language without changing text sizes, line heights or spacing. Android supplies fallback glyphs for scripts absent from a bundled font.

SIL Open Font License **1.1** attribution: bundled Persian Vazirmatn by **Saber Rastikerdar**; optional English Inter by **Rasmus Andersson**, Poppins by **Indian Type Foundry and Jonny Pinhorn**, Montserrat by **Julieta Ulanovsky and collaborators**, Nunito by **Vernon Adams and collaborators**, and Lato by **Łukasz Dziedzic**. The upstream OFL notices identify **The Inter Project Authors**, **The Poppins Project Authors**, **The Montserrat.Git Project Authors**, **The Nunito Project Authors**, and **tyPoland Łukasz Dziedzic**, respectively, as copyright holders. Include each supplied font's *actual* original copyright notice and full OFL license with redistributed builds; check the individual font's license before including it.
