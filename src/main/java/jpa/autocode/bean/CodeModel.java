package jpa.autocode.bean;

import lombok.Data;

@Data
public class CodeModel {
    private String beanName;// 实体类名
    private String repositoryName;// repository名
    private String serviceName;// server名
    private String serviceImplName;// server实现类名
    private String controllerName;// 控制器名
}
