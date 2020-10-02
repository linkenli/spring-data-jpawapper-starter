# framework
[GitHub地址](https://github.com/LiuBingXu18/framework) [maven地址](https://mvnrepository.com/artifact/com.github.liubingxu18/spring-data-jpawapper)
## spring-data-jpawapper是什么?
一个封装了jpa操作的工具集，且支持bean, repository, server, serverImpl, controller生成

## 有哪些功能？

* crud
    *  方便使用hibernate hql sql语句,支持参数传递
* 代码生成
    *  根据表生成代码

## 怎么使用？

* spring boot 引入

```xml
    <parent>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-parent</artifactId>
	<version>2.1.1.RELEASE</version>
	<relativePath/> <!-- lookup parent from repository -->
    </parent>
    <dependency>
    	<groupId>com.github.liubingxu18</groupId>
	<artifactId>spring-data-jpawapper</artifactId>
	<version>1.0.8</version>
    </dependency>
```

* application.yml 引入

```xml
spring:
  jpa:
    show-sql: true
    database: mysql
    generate-ddl: true
    hibernate:
      ddl-auto: update
  datasource:
    url: jdbc:mysql://localhost:3306/dataBaseName?useUnicode=true&characterEncoding=utf-8&useSSL=true&serverTimezone=UTC
    username: root
    password: pwd
    type: com.alibaba.druid.pool.DruidDataSource
    driver-class-name: com.mysql.cj.jdbc.Driver
code-create: 
 #需要生成代码的数据库
   database-name: toolkit
 #生成bean的包
   bean-package: com.liubx.bean
 #生成service的包
   service-package: com.liubx.web.service
 #生成serviceImpl的包
   service-impl-package: com.liubx.web.service.impl
 #生成repository的包
   repository-package: com.liubx.web.repoDsitory
 #生成controller的包
   controller-package: com.liubx.web.controller
 #是否生成代码
   enable: true
 #数据库类型 mysql或者oracle
   database-type: mysql
```
* 主函数引入注解

```xml
    @ComponentScan(basePackages = {"jpa.autocode", "you code package"})  
    @EnableJpaRepositories(repositoryFactoryBeanClass = BaseRepositoryFactoryBean.class)
```
* 代码生成
    访问localhost:8080/code.html  
    ![在这里插入图片描述](https://img-blog.csdnimg.cn/20190201210506170.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzI3NDc0ODUx,size_16,color_FFFFFF,t_70)
