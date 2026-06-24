package com.ecommerce.orderservice.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableJpaRepositories(
    basePackages = "com.ecommerce.orderservice.repository.read",
    entityManagerFactoryRef = "readEntityManagerFactory",
    transactionManagerRef = "readTransactionManager"
)
public class ReadDbConfig {

    @Bean(name = "readDataSourceProperties")
    @ConfigurationProperties("spring.datasource-read")
    public DataSourceProperties readDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "readDataSource")
    public DataSource readDataSource(@Qualifier("readDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Bean(name = "readEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean readEntityManagerFactory(
            @Qualifier("readDataSource") DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.ecommerce.orderservice.entity.read");
        em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.ddl-auto", "update");
        properties.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        em.setJpaPropertyMap(properties);

        return em;
    }

    @Bean(name = "readTransactionManager")
    public PlatformTransactionManager readTransactionManager(
            @Qualifier("readEntityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory.getObject());
    }
}
