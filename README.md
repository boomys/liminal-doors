# Liminal Doors

Minecraft **1.21.5 Fabric** için yazılmış, sıfırdan geliştirilmiş bir liminal-korku modu.

Bir kapıdan geçiyorsun ve arkasında başka bir kapı, onun arkasında başka bir boş oda var.
Odalar bitmiyor, ışıklar güvenilir değil ve ilk odadan sonra yalnız olmadığını fark ediyorsun.

> **Not:** Bu mod videolarda görülen `.2doors.jar` modundan **bağımsızdır** ve ondan hiçbir
> kod, model, doku veya ses dosyası içermez. O mod kapalı kaynak ve "All Rights Reserved"
> lisanslı olduğu için içeriği kopyalanamaz. Buradaki mod aynı türden (liminal mekân + takip
> eden varlık) **özgün** bir yapımdır: görsel olarak vanilla iskelet modeli/dokusu, sesler ise
> vanilla ses olayları kullanılır.

## Gereksinimler

| | |
|---|---|
| Minecraft | 1.21.5 |
| Mod loader | Fabric Loader 0.16.0 veya üzeri |
| Bağımlılık | Fabric API (1.21.5 sürümü) |
| Java | 21+ |

## Kurulum

1. Fabric Loader'ı Minecraft 1.21.5 profiline kur.
2. `mods` klasörüne bu modun jar dosyasını ve **Fabric API** jar'ını at.
3. Oyunu başlat.

## Nasıl oynanır

- **Eğil (sneak) + herhangi bir kapıya sağ tık** → o kapıdan liminal odalara geçersin.
- Alternatif olarak `/liminal enter` yazabilirsin.
- Her odada **ilerideki kapı** bir sonraki odaya götürür. Arkanda kalan kapı kilitlidir.
- Toplam **7 oda**. Son kapıyı kullandığında başladığın yere dönersin.
- `/liminal leave` → yarıda bırakıp geri dönersin.
- `/liminal status` → kaçıncı odada olduğunu gösterir.

### Takipçi (The Follower)

- 2. odadan itibaren, her iki odadan birinde odaya seninle birlikte girer.
- Seni fark ettiğinde peşine düşer; yaklaştığında kalp atışı sesini duyarsın.
- Sen ona bakmıyorken bazen arkana ışınlanır.
- Yakınındayken görüşün kararır (darkness etkisi).

## Kaynaktan derleme

```bash
# Loom 1.19, Gradle'ı çalıştıran JVM için Java 25 ister; üretilen bayt kodu Java 21'dir.
export JAVA_HOME=/yol/jdk-25
gradle build
# çıktı: build/libs/liminal-doors-1.0.0.jar
```

Gradle kurulu değilse: `gradle wrapper` ile sarmalayıcıyı üretip `./gradlew build` kullanabilirsin.

Geliştirme sırasında hızlı test için:

```bash
gradle runClient   # istemciyi modla açar
gradle runServer   # adanmış sunucu
```

## Teknik notlar

- Odalar çalışma zamanında, oyuncuya özel ayrılmış uzak bir bölgede (y≈180) blok blok inşa
  edilir; hiçbir yapı/dimension JSON'u gerekmez.
- Oturum bilgisi bellekte tutulur: sunucu yeniden başlarsa oda içinde kalırsan
  `/liminal leave` seni dünya doğuş noktasına ışınlar.
- Mod hem istemci hem sunucu tarafında çalışır (`environment: "*"`).

## Lisans

MIT — bkz. [LICENSE](LICENSE).
