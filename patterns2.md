TEMEL VERİ VE TUTARLILIK KALIPLARI (CORE DATA & CONSISTENCY PATTERNS)
1. Saga Kalıbu (Saga Pattern)
Tanım: Dağıtık işlemleri, her biri bir servis içinde gerçekleşen bir dizi yerel işlem olarak yöneten ve hata durumunda telafi edici işlemler (compensating transactions) başlatan bir desendir
.
Çözdüğü Problem: Mikroservislerde her servisin kendi veritabanı olduğu için geleneksel ACID işlemlerinin (özellikle 2-Phase Commit) yavaş kalması veya desteklenmemesi durumunda veri tutarlılığını sağlar
.
Nasıl Çalışır:
İşlem bir yerel transaction ile başlar ve bir olay (event) yayınlar
.
Bir sonraki servis olayı dinler, kendi yerel işlemini yapar ve yeni bir olay yayınlar
.
Eğer bir adım başarısız olursa, önceki başarılı adımları geri almak için "telafi edici işlemler" tersine çalıştırılır
.
Örnek: Bir seyahat rezervasyon sistemi; uçak bileti rezerve edilir, otel rezerve edilir, araç kiralama başarısız olursa uçak ve otel rezervasyonları iptal edilir
.
Mimari Diyagram:
graph LR


    A[Sipariş Servisi] -- Başarı --> B[Ödeme Servisi]
    B -- Başarı --> C[Envanter Servisi]
    C -- Hata --> D((Telafi Adımları))
    D -.-> B
    D -.-> A

    
2. CQRS Kalıbı (Command Query Responsibility Segregation)
Tanım: Veri yazma işlemlerini (komutlar) ve veri okuma işlemlerini (sorgular) farklı modeller veya veritabanları kullanarak birbirinden ayıran desendir
.
Çözdüğü Problem: Okuma ve yazma gereksinimleri farklı olan yüksek trafikli sistemlerde tek bir modelin performans darboğazı yaratmasını ve kod karmaşıklığını çözer
.
Nasıl Çalışır:
Uygulama, veri güncelleme (yazma) ve veri çekme (okuma) yollarına bölünür
.
Yazma tarafı ana veritabanını günceller ve bir olay yayınlar
.
Okuma tarafı bu olayı tüketerek kendi optimize edilmiş görünümünü (read-optimized view) günceller
.
Örnek: Bir bankacılık platformu; para transferi (komut) ilişkisel veritabanında yapılırken, işlem geçmişi (sorgu) Elasticsearch gibi hızlı arama motorlarından okunur
.
Mimari Diyagram:
graph TD
    Client -->|Komut| WS[Yazma Servisi]
    WS --> WDB[(Yazma DB)]
    WDB -.->|Olaylar| RS[Okuma Servisi]
    RS --> RDB[(Okuma DB)]
    Client -->|Sorgu| RS
3. Servis Başına Veritabanı Kalıbı (Database per Service Pattern)
Tanım: Her mikroservisin kendi özel veri deposuna sahip olduğu ve bu veriye yalnızca ilgili servisin API'si üzerinden erişilebildiği temel bir desendir
.
Çözdüğü Problem: Merkezi bir veritabanının yarattığı sıkı bağımlılığı ve bir servisteki şema değişikliğinin diğerlerini etkilemesi riskini ortadan kaldırır
.
Nasıl Çalışır:
Her servis kendi verisine sahip olur ve başka bir servisin verisine ihtiyaç duyarsa onun API'sini çağırır
.
Servisler, iş yüklerine göre farklı veritabanı teknolojilerini (Polyglot Persistence) seçebilir
.
Örnek: Bir e-ticaret platformunda Sipariş Servisi'nin PostgreSQL, Ürün Servisi'nin ise esnek şemalar için MongoDB kullanması
.
Mimari Diyagram:
graph TD
    S1[Sipariş Servisi] --- DB1[(SQL DB)]
    S2[Ürün Servisi] --- DB2[(NoSQL DB)]
    S3[Müşteri Servisi] --- DB3[(Graph DB)]
4. Olay Kaynaklama Kalıbı (Event Sourcing Pattern)
Tanım: Sistemin mevcut durumunu kaydetmek yerine, veri üzerinde yapılan tüm değişiklikleri (olayları) değişmez bir günlük (log) olarak saklayan desendir
.
Çözdüğü Problem: Verinin geçmişte hangi aşamalardan geçtiğinin takibini (audit log) yapamama ve geçmiş bir andaki durumu geri getirememe (time-travel) sorunlarını çözer
.
Nasıl Çalışır:
Her durum değişikliği bir "Olay Mağazası"na (Event Store) eklenir
.
Mevcut durumu elde etmek için bu olaylar sırayla yeniden oynatılır
.
Örnek: Bir banka hesap dökümü; sistem bakiyeyi saklamak yerine tüm yatırma ve çekme işlemlerini saklar ve bakiyeyi bu işlemlerden hesaplar
.
Mimari Diyagram:
graph LR
    S[Servis] -->|Ekle| ES[(Olay Mağazası)]
    ES -->|Yeniden Oynat| CS[Mevcut Durum]
MESAJLAŞMA KALIPLARI (MESSAGING PATTERNS)
5. Olay Güdümlü Mimari (Event Driven Architecture - EDA)
Tanım: Servislerin birbirini doğrudan çağırmak yerine, önemli durum değişikliklerini olay olarak yayınlayıp abone oldukları olaylara tepki vererek haberleştiği mimari yaklaşımdır
.
Çözdüğü Problem: Servisler arasındaki zamansal bağımlılığı (tight coupling) azaltarak sistemin ölçeklenebilirliğini ve dayanıklılığını artırır
.
Nasıl Çalışır:
Üretici servis bir olay yayınlar
.
Olay bir mesaj kuyruğuna veya broker'a (Kafka, RabbitMQ) gider
.
Alıcılar (Consumers) broker'dan olayları dinler ve kendi işlemlerini başlatır
.
Örnek: Bir lojistik sistemi; sipariş kargoya verildiğinde ödeme, bildirim ve analitik servislerinin aynı anda tetiklenmesi
.
Mimari Diyagram:
graph LR
    P[Üretici Servis] -->|Olay| MB[Mesaj Broker]
    MB --> C1[Fatura Servisi]
    MB --> C2[Analitik Servisi]
6. Outbox Kalıbı (Outbox Pattern)
Tanım: Bir servisin hem veritabanını güncellemesini hem de ilgili mesajı bir mesaj broker'ına yayınlamasını tek bir atomik işlemde garanti eden desendir
.
Çözdüğü Problem: Veritabanı güncellenip mesajın gönderilemediği "çift yazma" (dual write) kaynaklı veri tutarsızlığını çözer
.
Nasıl Çalışır:
Servis, iş verisini ve gönderilecek mesajı aynı yerel transaction içinde bir "Outbox" tablosuna yazar
.
Ayrı bir süreç (Relay) bu tabloyu tarar ve mesajları broker'a iletir
.
Örnek: Bir kullanıcı servisinin yeni profil oluştururken "Kullanıcı Oluşturuldu" olayını outbox tablosuna kaydederek bildirim servisine garantili ulaşmasını sağlaması
.
Mimari Diyagram:
graph LR
    S[Servis] -->|Tek İşlem| DB[(Yerel DB)]
    subgraph DB
        T1[İş Tablosu]
        T2[Outbox Tablosu]
    end
    T2 --> R[Mesaj Rölesi]
    R --> MB[Mesaj Broker]
7. Inbox Kalıbı (Inbox Pattern)
Tanım: Alıcı tarafta her mesajın tam olarak bir kez (exactly-once) işlenmesini sağlayan mesaj tekilleştirme (deduplication) desenidir
.
Çözdüğü Problem: Mesaj broker'larının mesajı birden fazla kez teslim edebilmesi (at-least-once delivery) sonucu oluşan mükerrer işlem riskini çözer
.
Nasıl Çalışır:
Gelen mesajın benzersiz ID'si "Inbox" tablosunda kontrol edilir
.
Eğer ID varsa mesaj atlanır; yoksa ID kaydedilir ve işlem tek bir transaction'da tamamlanır
.
Örnek: Bir ödeme sistemi; aynı ödeme mesajı iki kez gelirse, müşterinin iki kez ücretlendirilmesini önlemek için ikinci mesajı reddeder
.
Mimari Diyagram:
graph LR
    MB[Mesaj Broker] --> C[Tüketici Servis]
    C -->|ID Kontrol| IT[(Inbox Tablosu)]
    IT -- Yeni --> BL[İş Mantığı]
8. Ölü Mesaj Kuyruğu Kalıbı (Dead Letter Queue - DLQ)
Tanım: İşlenemeyen veya hatalı (poison) mesajların ana kuyruğu tıkamaması için ayrı bir kuyruğa aktarılmasıdır
.
Çözdüğü Problem: Hatalı mesajların sistemi sonsuz döngüye sokmasını veya diğer geçerli mesajların işlenmesini engellemesini önler
.
Nasıl Çalışır:
Mesaj işlenirken hata oluşursa retry mekanizması devreye girer
.
Maksimum deneme sayısına ulaşılırsa broker mesajı DLQ'ya taşır
.
Örnek: Bir kredi kartı başvuru sistemi; hatalı formatlı bir başvurunun sistemi tıkamaması için DLQ'ya alınması
.
Mimari Diyagram:
graph LR
    MQ[Ana Kuyruk] --> C[Tüketici]
    C -- Maksimum Deneme --> DLQ[Dead Letter Queue]
    DLQ --- Op[Operatör İncelemesi]
9. Olay Bildirim Kalıbı (Event Notification Pattern)
Tanım: Bir sistemin, detay içermeyen sadece bir değişikliğin olduğunu bildiren minimal mesajlar (ID ve link gibi) yayınlamasıdır
.
Çözdüğü Problem: Mesaj broker'ı üzerinden büyük veri paketleri göndermenin bant genişliği ve güvenlik maliyetini azaltır
.
Nasıl Çalışır:
Üretici sadece bir sinyal (örneğin: {"siparisId": 123}) yayınlar
.
Alıcı detay gerekliyse üreticinin API'sini çağırarak veriyi çeker
.
Örnek: Bir adres servisinin "Kullanıcı 456 adresini değiştirdi" demesi; nakliye servisinin gidip yeni adresi API'den öğrenmesi
.
Mimari Diyagram:
graph LR
    P[Üretici] -->|Küçük Mesaj| MB[Broker]
    MB --> C[Tüketici]
    C -->|Detay Çek| P
10. Olay Taşıyan Durum Transferi Kalıbı (Event-Carried State Transfer - ECST)
Tanım: Olay mesajlarının, alıcının işini yapması için gereken tüm veriyi (snapshot) içerdiği ve alıcının kaynağa geri dönmesine gerek kalmadığı desendir
.
Çözdüğü Problem: Alıcının kaynağa geri çağrı yapma zorunluluğunu kaldırarak gecikmeyi azaltır ve kaynak servis kapalıyken bile alıcının çalışabilmesini sağlar
.
Nasıl Çalışır:
Üretici, verinin o anki tam halini mesaja ekler
.
Alıcı bu veriyi kendi yerel kopyasında (replica) saklar ve kullanır
.
Örnek: Bir fatura servisinin ödeme olayındaki zengin veriyi kullanarak, müşteri servisi kapalı olsa dahi faturayı kesebilmesi
.
Mimari Diyagram:
graph LR
    P[Üretici] -->|Zengin Mesaj| MB[Broker]
    MB --> C[Tüketici]
    C --> LC[(Yerel Önbellek)]
DAYANIKLILIK KALIPLARI (RESILIENCE PATTERNS)
11. Devre Kesici Kalıbı (Circuit Breaker Pattern)
Tanım: Bir servisin çağrı yaptığı bağımlılık sürekli hata veriyorsa, sistemi korumak için çağrıları bir süreliğine tamamen durduran koruma mekanizmasıdır
.
Çözdüğü Problem: Hatalı bir servisi sürekli beklemekten kaynaklanan kaynak tükenmesini ve zincirleme sistem çöküşlerini (cascading failures) önler
.
Nasıl Çalışır:
Closed: Çağrılar geçer, hatalar sayılır
.
Open: Hata eşiği aşılırsa çağrılar hemen reddedilir (fast-fail)
.
Half-Open: Bir süre sonra test çağrısı yapılır; başarılıysa normale döner
.
Örnek: Bir e-ticaret sitesinin ödeme geçidi yavaşsa, müşteriyi 30 saniye bekletmek yerine "Ödeme servisimiz şu an kapalı" diyerek kaynağı serbest bırakması
.
Mimari Diyagram:
stateDiagram-v2
    Closed --> Open: Hata Eşiği Aşıldı
    Open --> HalfOpen: Bekleme Süresi Doldu
    HalfOpen --> Closed: Test Başarılı
    HalfOpen --> Open: Test Başarısız
12. Yeniden Deneme Kalıbı (Retry Pattern)
Tanım: Başarısız olan bir işlemin, hatanın geçici (transient) olduğu varsayımıyla otomatik olarak tekrar denenmesidir
.
Çözdüğü Problem: Ağ dalgalanmaları veya anlık aşırı yüklenme gibi kısa süreli hataları kullanıcıya yansıtmadan çözer
.
Nasıl Çalışır:
Hata alındığında bir süre beklenir ve işlem tekrar edilir
.
Üstel Geri Çekilme (Exponential Backoff): Bekleme süresi her denemede artırılır
.
Örnek: Bir mobil bankacılık uygulamasının internet koparsa bakiye bilgisini 3 kez arka planda tekrar denemesi
.
Mimari Diyagram:
graph LR
    App -->|İstek| S[Servis]
    S -- Hata --> App
    App -->|Bekle ve Tekrar Dene| S
13. Zaman Aşımı Kalıbı (Timeout Pattern)
Tanım: Bir servisin, diğer bir servisten yanıt bekleyebileceği maksimum süreyi belirleyen sınırlandırmadır
.
Çözdüğü Problem: Yanıt vermeyen servislerin, çağıran servisin thread'lerini ve kaynaklarını sonsuza kadar meşgul etmesini önler
.
Nasıl Çalışır:
Her ağ çağrısı için bir limit (örneğin 500ms) belirlenir
.
Süre dolduğunda cevap gelmese bile bağlantı kesilir ve hata döner
.
Örnek: Bir arama motoru reklam servisinin reklamlar 200ms içinde gelmezse, kullanıcıyı bekletmemek için sayfa reklamsız gösterilir
.
Mimari Diyagram:
graph LR
    A[Servis A] -- Max 500ms --> B[Servis B]
    B -.->|Geç Yanıt| X((Kesinti))
14. Bulkhead Kalıbı (Bulkhead Pattern)
Tanım: Sistem kaynaklarını (thread havuzları, bağlantılar) bölümlere ayırarak, bir bölümdeki hatanın diğer bölümleri etkilemesini önleyen izolasyon desenidir
.
Çözdüğü Problem: Bir mikroservis yavaşladığında tüm ortak kaynakları tüketip diğer sağlıklı servislerin de çalışamaz hale gelmesini önler
.
Nasıl Çalışır:
Farklı bağımlılıklar için ayrı kaynak havuzları oluşturulur
.
Eğer bir havuz dolarsa sadece o bölüm etkilenir, diğerleri çalışmaya devam eder
.
Örnek: Bir film izleme uygulamasında yorumlar servisi çökerse yorum havuzu dolsa bile film oynatma havuzu ayrı olduğu için etkilenmez
.
Mimari Diyagram:
graph TD
    subgraph Ana_Servis
        P1[Ödeme Havuzu]
        P2[Yorum Havuzu]
    end
    P1 --> S1[Ödeme Servisi]
    P2 --> S2[Yorum Servisi]
15. Fallback Kalıbı (Fallback Pattern)
Tanım: Bir servis çağrısı başarısız olduğunda sunulan alternatif yol veya varsayılan veri mekanizmasıdır
.
Çözdüğü Problem: Sistemdeki kısmi arızalar sırasında kullanıcıya hata sayfası yerine kısıtlı ama anlamlı bir içerik sunulmasını sağlar
.
Nasıl Çalışır:
Ana çağrı hata verdiğinde önceden tanımlanmış "fallback" metodu çalışır
.
Genellikle önbellekteki veri veya statik bir varsayılan değer döndürülür
.
Örnek: Bir ürün sayfasındaki tavsiyeler servisinin hata vermesi durumunda, kişiye özel liste yerine "Çok Satanlar" listesinin gösterilmesi
.
Mimari Diyagram:
graph LR
    Client --> S[Birincil Servis]
    S -- Hata --> F[Fallback: Önbelleği Yükle]
    F --> Client
16. Önbellek Kalıbı (Cache Pattern)
Tanım: Sık erişilen verilerin, daha hızlı erişim sağlamak için ana veritabanı yerine geçici bir bellek (RAM) üzerinde saklanmasıdır
.
Çözdüğü Problem: Veritabanı üzerindeki yükü azaltır ve veri erişim hızını (latency) önemli ölçüde iyileştirir
.
Nasıl Çalışır:
Uygulama önce önbelleğe (Redis vb.) bakar
.
Veri oradaysa (Hit) döner; yoksa (Miss) DB'den çeker ve önbelleği günceller
.
Örnek: Bir perakende sitesinin ürün açıklamalarını Redis üzerinde tutarak Black Friday gibi yoğun dönemlerde DB çöküşünü önlemesi
.
Mimari Diyagram:
graph LR
    App -->|1. Kontrol| Cache[(Redis)]
    Cache -- Miss --> DB[(SQL DB)]
    DB -->|2. Güncelle| Cache
17. Idempotency Kalıbı (Idempotency Pattern)
Tanım: Aynı işlemin defalarca tekrarlanması durumunda bile sonucun değişmemesini ve yan etkilerin oluşmamasını garanti eden tasarım ilkesidir
.
Çözdüğü Problem: Ağ hataları nedeniyle tekrarlanan isteklerin, mükerrer sipariş veya çift ödeme gibi kritik iş hatalarına yol açmasını önler
.
Nasıl Çalışır:
İstemci her istek için benzersiz bir "Idempotency Key" gönderir
.
Sunucu bu anahtarı kontrol eder; işlenmişse eski sonucu döner, işlenmemişse işlemi yapar
.
Örnek: Stripe ödeme API'sinin aynı istek anahtarı ile tekrar denendiğinde müşteriden ikinci kez para çekmemesi
.
Mimari Diyagram:
graph LR
    C[İstemci] -->|İstek + Anahtar: XYZ| S[Sunucu]
    S -->|Yeni mi?| P[İşle ve Kaydet]
    S -->|Tekrar mı?| R[Eski Sonucu Dön]
18. Hız Sınırlama Kalıbı (Rate Limiting Pattern)
Tanım: Bir kullanıcının veya istemcinin belirli bir süre içinde yapabileceği maksimum istek sayısını sınırlayan kontrol mekanizmasıdır
.
Çözdüğü Problem: Servislerin aşırı trafik veya kötü niyetli saldırılar (DoS) tarafından çökertilmesini engeller ve adil kaynak kullanımını sağlar
.
Nasıl Çalışır:
İstekler bir "Hız Sınırlayıcı" (Gatekeeper) tarafından karşılanır
.
Limit (Token Bucket vb. algoritmalar) aşılırsa istekler "429 Too Many Requests" ile reddedilir
.
Örnek: Twitter API'sinin bir uygulamanın saatte atabileceği tweet sayısını sınırlayarak platformun kararlılığını koruması
.
Mimari Diyagram:
graph LR
    Client -->|İstekler| RL[Rate Limiter]
    RL -->|Token Var| S[Servis]
    RL -- Token Yok --> E[429 Hatası]
İLETİŞİM KALIPLARI (COMMUNICATION PATTERNS)
19. API Gateway Kalıbı (API Gateway Pattern)
Tanım: Dış istemcilerin tüm mikroservislere erişimi için tek bir giriş noktası sağlayan merkezi bir proxy servisidir
.
Çözdüğü Problem: İstemcilerin onlarca servisin adresini yönetme zorunluluğunu ve güvenlik/loglama gibi işlemlerin her serviste tekrarlanmasını önler
.
Nasıl Çalışır:
İstemci Gateway'e istek gönderir; Gateway isteği doğrular ve doğru servise yönlendirir
.
Gateway, kimlik doğrulama ve hız sınırlama gibi ortak görevleri üstlenir
.
Örnek: Netflix Zuul; tüm cihaz isteklerini karşılayıp arka plandaki yüzlerce servise dağıtır
.
Mimari Diyagram:
graph LR
    App[Mobil/Web] --> AG[API Gateway]
    AG --> S1[Kullanıcı Servisi]
    AG --> S2[Sipariş Servisi]
    AG --> S3[Ödeme Servisi]
20. Backend for Frontend (BFF) Kalıbı
Tanım: Her istemci tipi (mobil, web vb.) için onların özel ihtiyaçlarına göre optimize edilmiş ayrı API Gateway'ler kullanılmasıdır
.
Çözdüğü Problem: Tek bir genel API'nin mobil cihazlar için çok fazla veri gönderip yavaş kalması sorununu çözer
.
Nasıl Çalışır:
Mobil uygulama için ayrı, web için ayrı bir "BFF" servisi oluşturulur
.
Her BFF, kendi istemcisinin UI'sı için gereken verileri toplar ve formatlar
.
Örnek: Spotify'ın mobil uygulama, masaüstü ve web oynatıcı için her birine özel backend servisleri sunması
.
Mimari Diyagram:
graph TD
    M[Mobil Uygulama] --> MB[Mobil BFF]
    W[Web Uygulama] --> WB[Web BFF]
    MB & WB --> Services[Arka Plan Servisleri]
21. API Bileşimi Kalıbı (API Composition Pattern)
Tanım: Birden fazla mikroservisten gelen verileri sorgulayarak tek bir sonuç setinde birleştiren veri toplama desenidir
.
Çözdüğü Problem: Verinin servisler arasında dağıldığı yapılarda, tek bir sayfa için gereken verilerin (JOIN işlemi gibi) nasıl çekileceği sorununu çözer
.
Nasıl Çalışır:
Bir "Bileştirici" (Composer) istemci isteğini alır ve ilgili servislere paralel çağrı yapar
.
Gelen cevapları bellekte birleştirerek tek bir yanıt olarak istemciye döner
.
Örnek: Bir sipariş detay sayfası; ürün açıklaması, fiyatı ve kargo durumunu üç ayrı servisten çekip birleştirir
.
Mimari Diyagram:
graph LR
    C[İstemci] --> AG[API Composer]
    AG --> S1[Servis A]
    AG --> S2[Servis B]
    AG --> S3[Servis C]
    S1 & S2 & S3 -->|Birleştir| AG
22. Toplayıcı Kalıbı (Aggregator Pattern)
Tanım: Birden fazla servisten gelen verileri toplayıp üzerinde iş mantığı uygulayarak birleşik bir görünüm hazırlayan uzmanlaşmış bir servis desenidir
.
Çözdüğü Problem: İstemci tarafındaki karmaşıklığı ve ağ üzerindeki "gevezeliği" (chattiness) azaltarak performansı artırır
.
Nasıl Çalışır:
API Bileşimi'ne benzer şekilde verileri toplar ancak genellikle daha fazla iş mantığı veya orkestrasyon içerir
.
Verileri istemcinin kullanacağı son formata dönüştürür
.
Örnek: Bir kullanıcı profili toplayıcısı; kişisel veriler, sadakat puanları ve son siparişleri toplayıp 360 derece müşteri görünümü sunar
.
Mimari Diyagram:
graph TD
    Client --> Agg[Aggregator Servisi]
    Agg --> S1[Kimlik Servisi]
    Agg --> S2[Puan Servisi]
    Agg --> S3[Geçmiş Servisi]
ALTYAPI KALIPLARI (INFRASTRUCTURE PATTERNS)
23. Servis Keşfi Kalıbı (Service Discovery Pattern)
Tanım: Mikroservis örneklerinin IP ve port bilgilerinin dinamik olarak kaydedildiği ve diğer servisler tarafından bulunduğu mekanizmadır
.
Çözdüğü Problem: Konteynerların (Kubernetes gibi) sürekli IP değiştirdiği ortamlarda servislerin birbirini bulabilmesini sağlar
.
Nasıl Çalışır:
Her servis başladığında kendini merkezi bir "Servis Kayıt Defteri"ne (Registry) kaydeder
.
Çağıran servis registry'ye sorarak hedef servisin adresini alır
.
Örnek: Netflix Eureka; tüm servislerin birbirine "odeme-servisi" gibi isimlerle ulaşmasını sağlar
.
Mimari Diyagram:
graph LR
    S1[Servis A] -->|1. Kaydol| R[Registry]
    S2[Servis B] -->|2. Sorgula| R
    R -->|3. Adres| S2
    S2 -->|4. Çağrı| S1
24. Sidecar Kalıbı (Sidecar Pattern)
Tanım: Ana uygulama konteynerinin yanına, ona loglama veya proxy gibi yardımcı özellikler sağlayan ayrı bir konteyner yerleştirilmesi desenidir
.
Çözdüğü Problem: İş mantığı kodunu loglama, güvenlik veya ağ yapılandırması gibi yardımcı kodlardan arındırarak "ayrıştırma" sağlar
.
Nasıl Çalışır:
Sidecar ana uygulama ile aynı "Pod" içinde çalışır ve aynı ağı paylaşır
.
Uygulamanın tüm dış trafiği genellikle bu sidecar üzerinden geçer
.
Örnek: Bir loglama sidecar'ı; uygulamanın yazdığı logları okuyup merkezi ELK stack'ine gönderir
.
Mimari Diyagram:
graph LR
    subgraph Pod
        App[Ana Konteyner]
        Side[Sidecar Proxy]
    end
    Side <--> App
    Side <--> External[Ağ]
25. Servis Ağı Kalıbı (Service Mesh Pattern)
Tanım: Mikroservisler arası iletişimi yöneten, sidecar proxy'ler ve merkezi bir kontrol panelinden oluşan altyapı katmanıdır
.
Çözdüğü Problem: Yüzlerce servisin olduğu sistemlerde güvenlik (mTLS), gözlemlenebilirlik ve trafik yönetimi kurallarını merkezi olarak yönetir
.
Nasıl Çalışır:
Veri Düzlemi (Data Plane): Her servisin yanında bir sidecar proxy çalışır
.
Kontrol Düzlemi (Control Plane): Tüm proxy'leri merkezi olarak yapılandırır ve yönetir
.
Örnek: Istio; sistemdeki tüm iletişimi otomatik olarak şifrelemek (mTLS) için kullanılır
.
Mimari Diyagram:
graph TD
    CP[Control Plane] -.-> P1 & P2
    subgraph S1
        App1[Uygulama] --- P1[Proxy]
    end
    subgraph S2
        App2[Uygulama] --- P2[Proxy]
    end
    P1 <--> P2
TAŞIMA KALIPLARI (MIGRATION PATTERNS)
26. Strangler Fig Kalıbı (Strangler Fig Pattern)
Tanım: Monolitik bir uygulamayı, özelliklerini parça parça mikroservislere taşıyarak zamanla tamamen devre dışı bırakan kademeli dönüşüm desenidir
.
Çözdüğü Problem: Monolitik sistemlerin "büyük patlama" (big bang) yöntemiyle sıfırdan yazılmasının getirdiği yüksek riskli ve uzun süreli başarısızlık ihtimalini önler
.
Nasıl Çalışır:
Monolitin önüne bir proxy/gateway konur
.
Taşınan özellik mikroservis olarak yazılır ve trafik oraya yönlendirilir
.
Monolit küçülür, sonunda tamamen devreden çıkarılır
.
Örnek: Amazon'un devasa "Obidos" monolitini bu yöntemi kullanarak yıllar içinde binlerce mikroservise bölmesi
.
Mimari Diyagram:
graph LR
    Client --> P[Proxy]
    P --> NM[Yeni Mikroservis]
    P --> OM[Eski Monolit]
    style OM stroke-dasharray: 5 5
Bu içerikler, sunumunuzda hem teknik derinlik sağlayacak hem de mühendislik ekipleri için anlaşılır olacaktır. Başka bir detay veya farklı bir format isterseniz yardımcı olabilirim.