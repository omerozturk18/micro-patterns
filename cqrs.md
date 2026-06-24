Neden Okuma ve Yazma DB'leri Ayrılmalı? (Fiziksel CQRS)
Farklı İhtiyaçlar: Yazma işlemleri genellikle veri bütünlüğü (ACID) gerektirdiği için İlişkisel Veritabanlarına (PostgreSQL, MySQL vb.) yapılır. Okuma işlemleri ise çok hızlı çalışması gerektiği için genellikle NoSQL (MongoDB, Elasticsearch, Redis) gibi veritabanlarına yapılır.
Performans (Ölçeklenebilirlik): Bir e-ticaret sitesinde okuma işlemleri (Query) %95, yazma işlemleri (Command) %5 oranındadır. Sadece Okuma veritabanını ayrı bir sunucuda çoğaltarak (scale) performansı uçurabilirsiniz.
Veri Modelleri: Okuma DB'sine veriler, arayüzün (UI) tam olarak ihtiyaç duyduğu formatta (hiçbir tabloyu joinlemeye gerek kalmadan) kaydedilir.







Mikroservis mimarilerinde veri tutarlılığını sağlamak ve sistem dayanıklılığını artırmak için kullanılan temel tasarım kalıpları şunlardır:
Veri Tutarlılığı ve Dağıtık İşlem Kalıpları
Saga Pattern: Mikroservislerde her servisin kendi veritabanı olduğu için geleneksel ACID işlemleri yapılamaz; Saga, dağıtık işlemleri bir dizi yerel işlem olarak yönetir
. Bir adım başarısız olursa, Saga daha önce tamamlanmış adımları geri almak için telafi edici işlemler (compensating transactions) yürütür
. İki ana uygulama yöntemi vardır: Koreografi (servislerin merkezi koordinatör olmadan olaylarla haberleşmesi) ve Orkestrasyon (merkezi bir kontrolcünün süreci yönetmesi)
.
CQRS (Command Query Responsibility Segregation): Veri depolama alanındaki yazma (Command) ve okuma (Query) operasyonlarını birbirinden ayırır
. Komutlar durumu güncellerken, sorgular okuma için optimize edilmiş ayrı bir modelden veri çeker; bu da özellikle karmaşık sorgularda performansı artırır
.
Event Sourcing: Bir nesnenin sadece güncel durumunu saklamak yerine, o nesne üzerinde gerçekleşen tüm durum değişikliklerini sıralı, değiştirilemez bir olay dizisi (append-only store) olarak saklar
. Sistemin durumu, bu olayların en baştan itibaren tekrar oynatılmasıyla (replay) herhangi bir andaki haline geri getirilebilir
.
Transactional Outbox: Veritabanı güncellemesi ile mesaj yayınlamanın atomik olarak gerçekleşmesini sağlayarak "dual write" problemini çözer
. Servis, iş verisini ve dışarı gönderilecek olayı aynı yerel veritabanı işlemi içinde bir "outbox" tablosuna yazar
. Ayrı bir süreç (relay), bu tabloyu izleyerek mesajları mesaj kuyruğuna güvenilir bir şekilde iletir
.
Inbox Pattern: Outbox ile "en az bir kez" (at-least-once) teslimat garantisi verildiği için mesajlar mükerrer gidebilir; Inbox, işlenen mesajların ID'lerini saklayarak aynı mesajın tekrar işlenmesini engeller (idempotency)
.
Resilience (Dayanıklılık) Kalıpları
Bu kalıplar, dağıtık sistemlerde kaçınılmaz olan hataların tüm sistemi çökertmesini engellemek için tasarlanmıştır
.
Circuit Breaker (Devre Kesici): Sürekli hata veren bir servise yapılan çağrıları belirli bir eşik değerinden sonra durdurarak kademeli çökmeleri (cascading failures) önler
. Devre üç durumda çalışır: Kapalı (normal akış), Açık (çağrılar hemen hata döndürür) ve Yarı-Açık (servisin düzelip düzelmediğini test etmek için sınırlı çağrıya izin verir)
.
Bulkhead (Bölme): Gemi gövdelerindeki sızdırmaz bölmelerden esinlenilmiştir; sistem kaynaklarını (iş parçacığı havuzları, bağlantılar) izole edilmiş parçalara ayırır
. Böylece bir servisteki aşırı yüklenme veya hata, diğer servislerin kaynaklarını tüketip tüm sistemi felç etmez
.
Retry (Yeniden Deneme): Geçici ağ kesintileri gibi kısa süreli hatalarda işlemi otomatik olarak tekrar dener
. Hatalı servisi daha fazla yük altına sokmamak için genellikle üstel geri çekilme (exponential backoff) ve rastgelelik (jitter) ile uygulanır
.
Timeout (Zaman Aşımı): Bir servisin yanıt vermesi için beklenen maksimum süreyi belirler
. Yanıt geciktiğinde çağrıyı sonlandırarak kaynakların sonsuza kadar meşgul edilmesini önler ve sistemin hızlı hata vermesini (fail-fast) sağlar
.
Cache (Önbellek): Sık erişilen verileri geçici bir alanda saklayarak backend servisleri üzerindeki yükü azaltır ve performansı artırır
. Servis kesintisi anında önbellekteki eski (stale) veri, sistemin çalışmaya devam etmesini sağlayabilir
.
Fallback (Yedek Yanıt): Bir servis hatası veya zaman aşımı durumunda kullanıcıya sunulan varsayılan veya alternatif yanıt mekanizmasıdır
. Örneğin, kişiselleştirilmiş öneri listesi alınamazsa genel "en çok satanlar" listesinin gösterilmesi bir fallback örneğidir
.
Bu kalıpların birlikte nasıl uygulanabileceğine dair somut kod örnekleri veya bir e-ticaret senaryosu üzerinden detaylı bir çalışma raporu oluşturmamı ister misiniz?
Daha detaylı anlat

Mikroservis mimarilerinde veri tutarlılığını ve sistem dayanıklılığını (resilience) sağlamak için kullanılan bu kalıpları, kaynaklar ışığında daha derinlemesine inceleyelim:
1. Veri Tutarlılığı ve Dağıtık İşlem Kalıpları
Saga Pattern: Mikroservislerde her servisin kendi veritabanı olduğu için geleneksel ACID işlemleri yapılamaz; Saga, dağıtık işlemleri bir dizi yerel işlem olarak yönetir
. Her yerel işlem kendi veritabanını günceller ve bir sonraki adımı tetikleyen bir mesaj yayınlar
. Bir adım başarısız olursa, Saga daha önce tamamlanmış adımları geri almak için telafi edici işlemler (compensating transactions) yürütür
. İki ana türü vardır:
Koreografi (Choreography): Servisler merkezi bir koordinatör olmadan olaylar aracılığıyla haberleşir; basit iş akışları için uygundur ancak servis sayısı arttıkça karmaşıklaşabilir
.
Orkestrasyon (Orchestration): Merkezi bir "orkestratör" tüm işlemleri yönetir ve katılımcılara hangi adımı atacaklarını söyler; karmaşık iş akışları için daha uygundur
.
Event Sourcing: Bir nesnenin sadece güncel durumunu saklamak yerine, o nesne üzerinde gerçekleşen tüm durum değişikliklerini sıralı ve değiştirilemez bir olay dizisi (append-only store) olarak saklar
. Sistemin durumu, bu olayların en baştan oynatılmasıyla (replay) herhangi bir andaki haline geri getirilebilir
. Bu yöntem, mükemmel bir denetim izi (audit trail) sağlar
.
CQRS (Command Query Responsibility Segregation): Veri yazma (Command) ve veri okuma (Query) operasyonlarını birbirinden ayırır
. Yazma modeli veri tutarlılığına odaklanırken, okuma modeli sorgu performansı için optimize edilmiş ayrı bir veritabanı veya görünüm (materialized view) kullanabilir
. Genellikle Event Sourcing ile birlikte kullanılır
.
2. Güvenilir Mesajlaşma Kalıpları
Transactional Outbox: Veritabanı güncellemesi ile olay yayınlamanın atomik olmamasından kaynaklanan "dual write" problemini çözer
. Uygulama, iş verisini ve gönderilecek olayı aynı yerel veritabanı işlemi içinde bir "outbox" tablosuna yazar
. Ayrı bir süreç (Relay), bu tabloyu izleyerek mesajları mesaj kuyruğuna (Kafka, RabbitMQ vb.) iletir
. Relay iki şekilde çalışabilir:
Polling Publisher: Outbox tablosunu belirli aralıklarla sorgular
.
Change Data Capture (CDC): Veritabanı işlem günlüklerini (WAL, binlog) izleyerek değişiklikleri anında yakalar ve iletir
.
Inbox Pattern (ve Idempotency): Outbox kalıbı "en az bir kez" (at-least-once) teslimat garantisi verdiği için mesajlar mükerrer iletilebilir
. Alıcı servisler, işlenen mesajların ID'lerini bir "inbox" tablosunda saklayarak aynı mesajın tekrar işlenmesini engellemelidir (idempotency)
.
3. Resilience (Dayanıklılık) Kalıpları
Bu kalıplar, dağıtık sistemlerdeki hataların tüm sistemi felç etmesini (cascading failure) engellemek için tasarlanmıştır
.
Circuit Breaker (Devre Kesici): Sürekli hata veren bir servise yapılan çağrıları durdurarak sistemin nefes almasını sağlar
. Üç durumu vardır:
Kapalı (Closed): İstekler normal şekilde iletilir
.
Açık (Open): Hata eşiği aşılırsa devre açılır, istekler hemen hata döndürür veya fallback'e yönlendirilir
.
Yarı-Açık (Half-Open): Bir süre sonra sistemin düzelip düzelmediğini anlamak için sınırlı sayıda deneme isteğine izin verilir
.
Bulkhead (Bölme): Sistem kaynaklarını (iş parçacığı havuzları, bağlantılar) izole edilmiş parçalara ayırır
. Bir servisteki aşırı yüklenme veya hata, diğer servislerin kaynaklarını tüketip tüm sistemi çökertemez
.
Retry (Yeniden Deneme): Geçici ağ kesintileri gibi kısa süreli hatalarda işlemi tekrar dener
. Hatalı servisi daha fazla yük altında bırakmamak için üstel geri çekilme (exponential backoff) ve denemeler arasına rastgelelik katan jitter ile uygulanmalıdır
.
Timeout (Zaman Aşımı): Bir servisin yanıt vermesi için beklenen maksimum süreyi belirler
. Zaman aşımı olduğunda istek sonlandırılır, böylece kaynakların sonsuza kadar meşgul edilmesi önlenir
.
Cache: Sık erişilen verileri saklayarak backend yükünü azaltır
. Ana servis devre dışı kaldığında, önbellekteki eski (stale) veriler bir fallback olarak kullanılabilir
.
Fallback (Yedek Yanıt): Bir servis başarısız olduğunda kullanıcıya sunulan alternatif veya varsayılan yanıttır
. Örneğin, öneri servisi çalışmıyorsa genel bir "popüler ürünler" listesinin gösterilmesi bir fallback stratejisidir
.
Bu kalıpların bir e-ticaret sisteminde (sipariş, ödeme, stok yönetimi gibi) nasıl uçtan uca kurgulanabileceğine dair bir mimari rapor hazırlamamı ister misiniz?









# Implementation Plan: Tam Fiziksel CQRS (İki Veritabanı)

`order-service` içerisindeki mevcut Mantıksal (Logical) CQRS yapısını, Okuma (Read) ve Yazma (Write) veritabanlarının **fiziksel olarak ayrıldığı** tam bir CQRS mimarisine dönüştüreceğiz.

## 1. Veritabanı Yapılandırması (Çift DB)
Şu anda `order-service` tek bir H2 veritabanına bağlı. Spring Boot içerisinde iki farklı veritabanı bağlantısı yapılandıracağız:
- **Write DB:** `jdbc:h2:mem:order_write_db` (İlişkisel / Kayıtların atıldığı ana yer)
- **Read DB:** `jdbc:h2:mem:order_read_db` (Okuma işlemlerinin çok hızlı yapılabilmesi için özelleştirilmiş görünüm)

Bunu yapabilmek için `application.yml`'da her iki bağlantıyı tanımlayıp, Spring'e özel `@Configuration` sınıfları (Örn: `WriteDbConfig` ve `ReadDbConfig`) yazacağız.

## 2. Klasör ve Kod Ayrımı
`order-service` içerisindeki paketleri şu şekilde ayıracağız:
- `repository/write/`: Write DB'ye bağlanan JPA Repository'ler (Sipariş Kaydı, Outbox).
- `repository/read/`: Read DB'ye bağlanan JPA Repository'ler.
- `entity/write/`: `Order` (Yazma modeli)
- `entity/read/`: `OrderReadModel` (Okuma modeli - UI'ın tam beklediği formatta denormalize edilmiş veri)

## 3. Eventual Consistency (Nihai Tutarlılık) Mekanizması
CQRS'in kalbi olan senkronizasyon:
1. Kullanıcı `OrderCommandController` üzerinden sipariş verdiğinde, sipariş sadece **Write DB**'ye kaydedilecek.
2. İşlem tamamlandığında Spring `ApplicationEvent` (Uygulama İçi Olay) fırlatılacak.
3. `OrderProjector` (veya Synchronizer) adında bir dinleyici bu olayı yakalayacak. Veriyi okuma formatına (DTO gibi) dönüştürüp **Read DB**'ye asenkron olarak kaydedecek.
4. `OrderQueryController` verileri sadece **Read DB** üzerinden getirecek.

## 4. Test Stratejisi
Swagger üzerinden `POST /api/orders/commands` ile sipariş oluşturacağız.
H2 Console arayüzüne gidip `order_write_db`'de yazma modelini, `order_read_db`'de okuma modelini ayrı ayrı göreceğiz. `GET /api/orders/queries` sadece Read DB'yi sorgulayacak.

---
**Bu planı onaylıyor musunuz? Onaylarsanız `order-service` üzerinde çift veritabanlı Fiziksel CQRS entegrasyonuna hemen başlıyorum!**
