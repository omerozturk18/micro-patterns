$baseDir = "."
$version = "1.0.0-SNAPSHOT"
$springBootVersion = "3.3.0"
$springCloudVersion = "2023.0.1"

$services = @(
    "service-registry",
    "api-gateway",
    "product-service",
    "order-service",
    "payment-service",
    "customer-service",
    "notification-service"
)

$rootPom = @"
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.ecommerce</groupId>
    <artifactId>real-ecommerce-microservices</artifactId>
    <version>$version</version>
    <packaging>pom</packaging>
    
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>$springBootVersion</version>
    </parent>

    <properties>
        <java.version>17</java.version>
        <spring-cloud.version>$springCloudVersion</spring-cloud.version>
    </properties>

    <modules>
"@
foreach ($svc in $services) {
    $rootPom += "`n        <module>$svc</module>"
}
$rootPom += @"

    </modules>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>`${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
"@
[System.IO.File]::WriteAllText(".\pom.xml", $rootPom, [System.Text.Encoding]::UTF8)

foreach ($svc in $services) {
    New-Item -ItemType Directory -Force -Path ".\$svc\src\main\java\com\ecommerce\$($svc.Replace('-',''))" | Out-Null
    New-Item -ItemType Directory -Force -Path ".\$svc\src\main\resources" | Out-Null
    
    $deps = ""
    $webDep = "<dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>"
    $swaggerDep = "<dependency><groupId>org.springdoc</groupId><artifactId>springdoc-openapi-starter-webmvc-ui</artifactId><version>2.5.0</version></dependency>"
    
    if ($svc -eq "service-registry") {
        $deps = @"
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
        </dependency>
"@
        $webDep = ""
        $swaggerDep = ""
    } elseif ($svc -eq "api-gateway") {
        $webDep = ""
        $swaggerDep = "<dependency><groupId>org.springdoc</groupId><artifactId>springdoc-openapi-starter-webflux-ui</artifactId><version>2.5.0</version></dependency>"
        $deps = @"
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-gateway</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
"@
    } else {
        $deps = @"
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
"@
        if ($svc -eq "product-service" -or $svc -eq "customer-service") {
            $deps += @"
`n        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
        </dependency>
"@
        }
        
        $pkgPath = ".\$svc\src\main\java\com\ecommerce\$($svc.Replace('-',''))"
        New-Item -ItemType Directory -Force -Path "$pkgPath\controller" | Out-Null
        New-Item -ItemType Directory -Force -Path "$pkgPath\service" | Out-Null
        New-Item -ItemType Directory -Force -Path "$pkgPath\repository" | Out-Null
        New-Item -ItemType Directory -Force -Path "$pkgPath\entity" | Out-Null
        New-Item -ItemType Directory -Force -Path "$pkgPath\dto" | Out-Null
        New-Item -ItemType Directory -Force -Path "$pkgPath\event" | Out-Null
        New-Item -ItemType Directory -Force -Path "$pkgPath\config" | Out-Null
    }

    $svcPom = @"
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.ecommerce</groupId>
        <artifactId>real-ecommerce-microservices</artifactId>
        <version>$version</version>
    </parent>
    <artifactId>$svc</artifactId>

    <dependencies>
        $webDep
        $swaggerDep
        $deps
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
"@
    [System.IO.File]::WriteAllText(".\$svc\pom.xml", $svcPom, [System.Text.Encoding]::UTF8)
    
    $port = 0
    $appName = $svc
    $yml = "spring:`n  application:`n    name: $appName`n"
    if ($svc -eq "service-registry") {
        $port = 8761
        $yml += "server:`n  port: $port`neureka:`n  client:`n    register-with-eureka: false`n    fetch-registry: false`n"
    } elseif ($svc -eq "api-gateway") {
        $port = 8080
        $yml += "server:`n  port: $port`neureka:`n  client:`n    service-url:`n      defaultZone: http://localhost:8761/eureka/`n"
        $yml += "springdoc:`n  swagger-ui:`n    urls:`n      - name: product-service`n        url: /product-service/v3/api-docs`n      - name: order-service`n        url: /order-service/v3/api-docs`n      - name: payment-service`n        url: /payment-service/v3/api-docs`n      - name: customer-service`n        url: /customer-service/v3/api-docs`n      - name: notification-service`n        url: /notification-service/v3/api-docs`n"
    } else {
        $yml += "server:`n  port: 0`n"
        $yml += "eureka:`n  client:`n    service-url:`n      defaultZone: http://localhost:8761/eureka/`n"
        $yml += "spring:`n  datasource:`n    url: jdbc:h2:mem:${appName}_db`n    driverClassName: org.h2.Driver`n    username: sa`n    password:`n  jpa:`n    hibernate:`n      ddl-auto: update`n  h2:`n    console:`n      enabled: true`n      path: /h2-console`n"
    }
    
    if ($svc -eq "product-service") { $yml = $yml.Replace("port: 0", "port: 8081") }
    if ($svc -eq "order-service") { $yml = $yml.Replace("port: 0", "port: 8082") }
    if ($svc -eq "payment-service") { $yml = $yml.Replace("port: 0", "port: 8083") }
    if ($svc -eq "customer-service") { $yml = $yml.Replace("port: 0", "port: 8084") }
    if ($svc -eq "notification-service") { $yml = $yml.Replace("port: 0", "port: 8085") }

    [System.IO.File]::WriteAllText(".\$svc\src\main\resources\application.yml", $yml, [System.Text.Encoding]::UTF8)
    
    $pkgName = $svc.Replace("-","")
    $classNameParts = $svc.Split("-")
    $className = ""
    foreach ($part in $classNameParts) {
        $className += $part.Substring(0,1).ToUpper() + $part.Substring(1)
    }
    $className += "Application"
    
    $annotations = "@SpringBootApplication`n"
    $imports = "import org.springframework.boot.SpringApplication;`nimport org.springframework.boot.autoconfigure.SpringBootApplication;`n"
    
    if ($svc -eq "service-registry") {
        $annotations += "@org.springframework.cloud.netflix.eureka.server.EnableEurekaServer`n"
    } elseif ($svc -ne "api-gateway") {
        $annotations += "@org.springframework.cloud.openfeign.EnableFeignClients`n"
    }

    $appJava = @"
package com.ecommerce.$pkgName;

$imports

$annotations
public class $className {
    public static void main(String[] args) {
        SpringApplication.run(${className}.class, args);
    }
}
"@
    [System.IO.File]::WriteAllText(".\$svc\src\main\java\com\ecommerce\$pkgName\$($className).java", $appJava, [System.Text.Encoding]::UTF8)
}

Write-Output "Realistic Microservices scaffolding completed."
