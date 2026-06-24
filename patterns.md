Mikroservis mimarisinde tasarım desenleri, dağıtık sistemlerin beraberinde getirdiği karmaşıklığı, ağ gecikmelerini ve veri tutarlılığı sorunlarını yönetmek için kullanılır
. Bu desenler, sistemin ölçeklenebilir, dayanıklı ve bakımı kolay bir yapıda olmasını sağlar
.
1. Database per Service & API Gateway
Neden Kullanıyoruz? Geleneksel monolitik yapılarda tüm servisler tek bir veritabanını paylaşırken, mikroservislerde servislerin birbirine sıkı sıkıya bağlanmasını önlemek ve bağımsız ölçeklenebilirlik sağlamak gerekir
.
Faydaları: Her servisin kendi veritabanına sahip olması (Database per Service), servis otonomisini artırır, hata izolasyonu sağlar ve her servisin ihtiyacına uygun veritabanı teknolojisini (Polyglot Persistence) seçmesine olanak tanır
. API Gateway ise istemcilerin onlarca mikroservisle doğrudan konuşması yerine tek bir giriş noktası üzerinden iletişim kurmasını sağlayarak istemci tarafındaki karmaşıklığı gizler
.
Nasıl Çalışır? Database per Service deseninde veriler servis içinde kapsüllenir ve dışarıya sadece API üzerinden açılır
. API Gateway, istemciden gelen istekleri karşılar; kimlik doğrulama, yönlendirme ve istek birleştirme (aggregator) gibi görevleri üstlenerek isteği uygun mikroservise iletir
.
2. Cache (Önbellek) & Rate Limiting (Hız Sınırlandırması)
Neden Kullanıyoruz? Dağıtık sistemlerde performans kayıplarını önlemek ve aşırı yük altında sistemin çökmesini engellemek için bu desenler kritiktir
.
Faydaları: Cache, sık erişilen verileri geçici bir alanda (örneğin Redis) saklayarak veritabanı yükünü azaltır ve yanıt sürelerini hızlandırır
. Rate Limiting ise sistemin kapasitesinden fazla istek almasını engelleyerek servis stabilitesini korur ve kaynak tükenmesini önler
.
Nasıl Çalışır? Cache deseni, gelen isteği önce önbellekte arar; veri orada varsa hemen döner, yoksa ana kaynaktan alıp önbelleğe yazar
. Rate Limiting, belirli bir zaman diliminde kullanıcı veya IP başına izin verilen maksimum istek eşiğini belirler; bu eşik aşıldığında istekler reddedilir veya kuyruğa alınır
.
3. Saga Pattern (Orchestrator) & CQRS
Neden Kullanıyoruz? Mikroservislerde her servisin kendi veritabanı olduğu için klasik ACID işlemleri yapılamaz; dağıtık işlemleri yönetmek için Saga, okuma ve yazma işlemlerini optimize etmek için CQRS kullanılır
.
Faydaları: Saga, dağıtık sistemlerde veri tutarlılığını sağlamak için yerel işlemler dizisi kullanır ve bir hata anında sistemi eski haline döndürebilir
. CQRS ise yazma (Command) ve okuma (Query) modellerini ayırarak karmaşık sorguların performansını artırır
.
Nasıl Çalışır? Orchestrator tabanlı Saga'da merkezi bir kontrolcü, her servise hangi işlemi yapacağını söyler ve hata durumunda "telafi edici işlemleri" (compensating transactions) tetikler
. CQRS'te ise veriyi güncelleyen model ile veriyi sorgulayan model farklıdır; yazma modeli durum değişikliklerini kaydederken, okuma modeli sorgular için optimize edilmiş ayrı bir görünüm sunar
.
4. Outbox & Inbox Patterns
Neden Kullanıyoruz? Bir servisin hem veritabanını güncelleyip hem de bir mesaj yayınlaması gerektiği durumlarda ortaya çıkan "çifte yazma" (dual write) problemini ve mesajların mükerrer işlenmesi sorununu çözmek için kullanılır
.
Faydaları: Outbox, veritabanı güncellemesi ile mesaj yayınlamanın atomik (ya hep ya hiç) gerçekleşmesini garanti eder
. Inbox ise alıcı tarafta mesajların sadece bir kez işlenmesini (idempotency) sağlar
.
Nasıl Çalışır? Outbox deseninde servis, iş verisini ve gönderilecek mesajı aynı yerel işlem içinde bir "outbox" tablosuna yazar
. Ayrı bir süreç (Relay veya CDC) bu tabloyu izleyerek mesajları mesaj kuyruğuna iletir
. Inbox deseninde ise tüketici servis, işlediği mesajların ID'lerini bir "inbox" tablosunda saklar ve yeni gelen mesajın daha önce işlenip işlenmediğini kontrol eder
.
5. Circuit Breaker (Devre Kesici - Resilience4j)
Neden Kullanıyoruz? Dağıtık bir sistemde bir servisteki hatanın tüm sisteme yayılmasını (cascading failure) önlemek için "fail-fast" (hızlı hata verme) stratejisi uygulanmalıdır
.
Faydaları: Sürekli hata veren bir servise yapılan çağrıları durdurarak sistem kaynaklarının sonsuza kadar meşgul edilmesini önler ve hatalı servisin toparlanması için zaman tanır
. Resilience4j, Java ekosisteminde bu desenin uygulanması için standartlaşmış bir kütüphanedir
.
Nasıl Çalışır? Üç durumda çalışır:
Closed (Kapalı): Her şey yolunda, istekler normal iletilir
.
Open (Açık): Hata eşiği aşılırsa devre açılır; istekler servise gitmeden anında hata döner veya bir yedek yanıta (fallback) yönlendirilir
.
Half-Open (Yarı-Açık): Belirli bir süre sonra devre, servisin düzelip düzelmediğini test etmek için sınırlı sayıda isteğe izin verir; başarı sağlanırsa tekrar kapalı duruma geçer