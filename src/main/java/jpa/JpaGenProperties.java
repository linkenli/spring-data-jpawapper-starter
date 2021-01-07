package jpa;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties("code-create")
public class JpaGenProperties {

    private String databaseType;

    private String databaseName;

    private String repositoryPackage;
    private String beanPackage;
    private String servicePackage;
    private String serviceImplPackage;
    private String controllerPackage;

    private String baseDomainClass;

    private boolean enable;

}
