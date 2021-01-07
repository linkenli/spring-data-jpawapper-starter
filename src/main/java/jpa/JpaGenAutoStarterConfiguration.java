package jpa;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import jpa.autocode.controller.CodeController;
import jpa.autocode.core.JavaCreate;


@Configuration
@EnableConfigurationProperties(JpaGenProperties.class)
@Import({CodeController.class, JavaCreate.class})
public class JpaGenAutoStarterConfiguration {


}
