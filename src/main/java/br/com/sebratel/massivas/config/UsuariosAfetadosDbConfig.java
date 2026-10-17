package br.com.sebratel.massivas.config;


import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = {"br.com.sebratel.massivas.repository.afetados"},
        entityManagerFactoryRef = "afetadosEntityManagerFactory",
        transactionManagerRef = "afetadosTransactionManager"
)
public class UsuariosAfetadosDbConfig {
    @Bean
    @ConfigurationProperties("spring.datasource.afetados")
    public DataSourceProperties afetadosDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "afetadosDataSource")
    public DataSource afetadosDataSource() {
        // O DataSourceProperties mapeará o .url do properties automaticamente
        return afetadosDataSourceProperties()
                .initializeDataSourceBuilder()
                .type(com.zaxxer.hikari.HikariDataSource.class)
                .build();
    }

    @Bean(name = "afetadosEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean afetadosEntityManagerFactory(
            EntityManagerFactoryBuilder builder, @Qualifier("afetadosDataSource") DataSource dataSource) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.dialect", "org.hibernate.dialect.MariaDBDialect");
        return builder
                .dataSource(dataSource)
                .packages("br.com.sebratel.massivas.model.entity")
                .persistenceUnit("afetados")
                .properties(properties)
                .build();
    }

    @Bean(name = "afetadosTransactionManager")
    public PlatformTransactionManager afetadosTransactionManager(
            @Qualifier("afetadosEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}