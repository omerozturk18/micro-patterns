# Gerçekçi E-Ticaret Mikroservisleri Kullanım Rehberi

Artık tamamen işlevsel, kurumsal seviyede bir E-Ticaret mikroservis mimarisine sahipsiniz.

## 🏗️ Mimari Genel Bakış

Tüm servisler kendilerini `http://localhost:8761` adresinde çalışan **Service Registry (Eureka)** üzerine kaydeder.
`http://localhost:8080` adresinde çalışan **API Gateway**, gelen trafiği Eureka servis keşfi mekanizmasını (service discovery) kullanarak ilgili mikroservislere yönlendirir (örn. `lb://order-service`).

## 🚀 Nasıl Çalıştırılır?
1. Favori IDE'nizde (IntelliJ, Eclipse vb.) ana klasördeki `pom.xml` dosyasını açın.
2. Projeyi derleyin: Terminalde proje dizinindeyken `mvn clean install` komutunu çalıştırın.
3. Uygulamaları sırasıyla başlatın:
   - Önce `ServiceRegistryApplication` sınıfını çalıştırın (tamamen ayağa kalkmasını bekleyin).
   - Sonra `ApiGatewayApplication` sınıfını çalıştırın.
   - Ardından kalan servisleri dilediğiniz sırayla çalıştırın: `ProductServiceApplication`, `OrderServiceApplication`, `PaymentServiceApplication`, `CustomerServiceApplication`, `NotificationServiceApplication`.
4. Tüm servisler ayağa kalktığında **http://localhost:8080/swagger-ui.html** adresine gidin. Sağ üst köşede bir açılır menü (dropdown) göreceksiniz. Bu menüden *istediğiniz* servisi seçebilir ve doğrudan API Gateway üzerinden test edebilirsiniz!

---

## 🧩 Tasarım Kalıpları (Design Patterns) Nasıl Test Edilir?

### 1. Database per Service (Servis Başına Veritabanı) & API Gateway
- **Nerede**: `product-service`, `order-service`, `customer-service` ve `api-gateway`
- **Nasıl Test Edilir**: Swagger arayüzünde açılır menüden `product-service`'i seçin ve `POST /api/products` endpoint'ini kullanarak yeni bir ürün oluşturun. Bu ürün sadece o servisin kendi H2 veritabanına (`jdbc:h2:mem:product-service_db`) kaydedilir. Ayrıca tüm bu isteklerin `8080` portundaki Gateway üzerinden yapıldığına dikkat edin.

### 2. Cache (Önbellek) Pattern & Rate Limiting (Hız Sınırlandırması)
- **Nerede**: `product-service`
- **Nasıl Test Edilir**: 
  - `GET /api/products` isteği gönderin. İlk seferinde konsolda "Fetching products from DB" (Veritabanından çekiliyor) yazdığını göreceksiniz. Sonraki istekleriniz anında dönecek ve bu yazı çıkmayacaktır (Cache).
  - Yine `GET /api/products` endpoint'ine 10 saniye içinde ardı ardına 5'ten fazla istek gönderin. **Rate Limiter** devreye girecek ve size bir hata fırlatmak yerine Fallback (yedek) mesajı dönecektir.

### 3. Saga Pattern (Orchestrator) & CQRS
- **Nerede**: `order-service` ve `payment-service`
- **Nasıl Test Edilir**: 
  - Swagger arayüzünde `order-service`'i seçin. 
  - `/api/orders/commands` endpoint'ine `POST` isteği atın (Gövde örneği: `{"productId": 1, "amount": 500}`).
  - **Saga Başarılı**: Orchestrator, OpenFeign aracılığıyla `payment-service`'i çağırır. Tutar 1000'den küçük olduğu için ödeme başarılı olur ve siparişin durumu `COMPLETED` olarak güncellenir.
  - **Saga Başarısız / Telafi (Compensation)**: Aynı endpoint'e `1500` tutarında bir sipariş gönderin. Ödeme reddedilecek ve Orchestrator siparişin durumunu `CANCELLED` olarak güncelleyerek telafi işlemini yapacaktır.
  - **CQRS**: Kod yapısına baktığınızda yazma (POST) işlemlerini `OrderCommandController`'ın, okuma (GET) işlemlerini ise `OrderQueryController`'ın üstlendiğini görebilirsiniz.

### 4. Outbox & Inbox Patterns
- **Nerede**: `order-service` (Outbox) ve `notification-service` (Inbox)
- **Nasıl Test Edilir**: 
  - Yeni bir sipariş oluşturduğunuzda, `order-service` veritabanına sadece siparişi değil, aynı zamanda bir `OutboxEvent` kaydeder.
  - Zamanlanmış bir görev (`OutboxPoller.java`) her 5 saniyede bir çalışır. Bu eventi alır ve `notification-service`'e gönderir.
  - `notification-service` konsol loglarını kontrol edin. "Processing Notification for Event: ORDER_CREATED" şeklinde bir log göreceksiniz.
  - **Inbox Idempotency (Aynı işlemin tekrarını önleme)**: Eğer poller aynı eventi ikinci kez gönderirse, `NotificationController` kendi içindeki `InboxEventRepository` veritabanını kontrol eder ve eventin daha önce işlendiğini anlayıp (Idempotency) atlar.

### 5. Circuit Breaker (Devre Kesici - Resilience4j)
- **Nerede**: `customer-service`, `order-service`'i çağırırken kullanır.
- **Nasıl Test Edilir**:
  - Swagger arayüzünde `customer-service`'i seçin.
  - `GET /api/customers/orders` endpoint'ine istek atın. OpenFeign aracılığıyla siparişleri başarıyla getirecektir.
  - Şimdi, IDE'niz üzerinden **`order-service` uygulamasını durdurun** (Stop).
  - Tekrar `GET /api/customers/orders` endpoint'ine istek atın. Uygulama hata verip çökmek yerine, **Circuit Breaker** (Devre Kesici) hatayı yakalar ve konsola uyarı yazdırırken size güvenli bir yedek senaryo (boş bir liste) döndürür.

---
> [!TIP]
> **H2 Veritabanı Arayüzleri**: Her bir servisin veritabanındaki tabloları ve verileri görmek için tarayıcınızda `http://localhost:<SERVIS_PORTU>/h2-console` adresine gidebilirsiniz. Giriş yaparken JDBC URL kısmına `jdbc:h2:mem:<SERVIS_ADI>_db` (Örn: `jdbc:h2:mem:order-service_db`) yazdığınızdan emin olun.
