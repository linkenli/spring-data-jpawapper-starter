package jpa.autocode.core;

import com.squareup.javapoet.*;
import jpa.autocode.bean.CodeModel;
import jpa.autocode.bean.Parms;
import jpa.autocode.bean.Table;
import jpa.autocode.util.DateUtils;
import jpa.autocode.util.ParmsUtil;
import jpa.autocode.util.StringUtil;
import jpa.autocode.util.UUIDUtils;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.lang.model.element.Modifier;
import javax.persistence.Column;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.criteria.Predicate;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;

@Data
public class JavaCreate implements CreateCode {
    private final static Logger LOGGER = LoggerFactory.getLogger(JavaCreate.class);

    private EntityManager entityManager;
    protected String dataBaseName;
    protected CodeModel codeModel = new CodeModel();
    protected String tableName;// 表名
    protected String version = "V 1.0.5";// 版本
    protected String doMainPackage = "com.liubx.bean";// 实体类路径
    protected String servicePackage = "com.liubx.web.service";// service路径
    protected String serviceImplPackage = "com.liubx.web.service.impl";// service实现类路径
    protected String repositoryPackage = "com.liubx.web.repository";// repository类路径
    protected String controllerPackage = "com.liubx.web.controller";// controller类路径
    protected String dataBaseType;// 数据库类型
    protected String basePath;// 绝对路径前缀
    protected List<Parms> parm;// 参数
    protected List<String> createInstance;// 创建实例

    public JavaCreate(EntityManager entityManager, String dataBaseName, String tableName, String doMainPackage,
                      String servicePackage, String serviceImplPackage, String repositoryPackage, String controllerPackage,
                      String dataBaseType, List<Parms> parm) {
        Assert.notNull(dataBaseName, "数据库名不能为空！");
        Assert.notNull(tableName, "表不能为空！");
        Assert.notNull(doMainPackage, "实体类路径不能为空！");
        Assert.notNull(servicePackage, "service 路径不能为空！");
        Assert.notNull(serviceImplPackage, "service 实现类路径不能为空！");
        Assert.notNull(repositoryPackage, "repository 包路径不能为空！");
        Assert.notNull(controllerPackage, "controller 包路径不能为空！");
        Assert.notNull(dataBaseType, "数据库类型不能为空！");
        this.entityManager = entityManager;
        this.dataBaseName = dataBaseName;
        this.tableName = tableName;
        this.doMainPackage = doMainPackage;
        this.servicePackage = servicePackage;
        this.serviceImplPackage = serviceImplPackage;
        this.repositoryPackage = repositoryPackage;
        this.controllerPackage = controllerPackage;
        this.dataBaseType = dataBaseType;
        this.parm = parm;
        this.createInstance = ParmsUtil.getValueByKey(this.parm, "type_c");
        this.initBasePath();
    }

    public JavaCreate(EntityManager entityManager, String tableName, String dataBaseName) {
        this.entityManager = entityManager;
        this.tableName = tableName;
        this.dataBaseName = dataBaseName;
        this.initBasePath();
    }

    @Override
    public void create() {
        String sql = this.getSql();
        List<Object[]> resultList = entityManager.createNativeQuery(sql).getResultList();

        // 查询数据库
        List<Table> tableList = new ArrayList<>();
        resultList.forEach(t -> {
            Table table = new Table();
            table.setName(StringUtil.objToStr(t[0]));
            table.setComment(StringUtil.objToStr(t[1]));
            table.setDataType(StringUtil.objToStr(t[2]));
            table.setIsPri(StringUtil.objToStr(t[3]));
            tableList.add(table);
        });

        // 准备相关名
        codeModel.setBeanName(getEntityName(tableName));
        codeModel.setRepositoryName(codeModel.getBeanName() + "Repository");
        codeModel.setServerName(codeModel.getBeanName() + "Service");
        codeModel.setServerImplName(codeModel.getServerName() + "Impl");
        codeModel.setControllerName(codeModel.getBeanName() + "Controller");

        // 生成代码
        try {
            this.newThreadCreateCode(tableName, tableList);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void newThreadCreateCode(String tableName, List<Table> tableList) throws InterruptedException, ClassNotFoundException, NoSuchFieldException, SecurityException {
        // 生成domain
        this.createDomainClass(tableName, tableList);
        Thread.sleep(1000);

        if (createInstance.contains("repository")) {
            // 生成repository
            this.createRepository();
            Thread.sleep(1000);
        }
       
        if (createInstance.contains("service")) {
            // 生成service接口
            this.createServiceClass();
            Thread.sleep(1000);
        }

        if (createInstance.contains("serviceImpl")) {
            // 生成service接口实现类
            this.createServiceClassImpl();
            Thread.sleep(1000);
        }
        if (createInstance.contains("controller")) {
            // 生成controller
            this.createController();
        }
    }

    public boolean createDomainClass(String tableName, List<Table> tableList) {
        /** 读取mysql转Java类型配置 **/
        InputStream in = null;
        if ("mysql".equals(dataBaseType)) {
            in = this.getClass().getClassLoader().getResourceAsStream("mysqlToJava.properties");
        } else {
            in = this.getClass().getClassLoader().getResourceAsStream("oracleToJava.properties");
        }
        ResourceBundle resourceBundle = null;
        try {
            resourceBundle = new PropertyResourceBundle(in);
        } catch (IOException e) {
            e.printStackTrace();
        }

        
        TypeSpec.Builder builder = TypeSpec.classBuilder(codeModel.getBeanName());
        ResourceBundle finalResourceBundle = resourceBundle;
        tableList.forEach(t -> {
        	
        	List<AnnotationSpec> list = new ArrayList<AnnotationSpec>();
        	
            /** 属性上面的注解 **/
            AnnotationSpec annotationSpecColumn;
            
            if (t.getIsPri().equals("true")) {
                annotationSpecColumn = AnnotationSpec.builder(Id.class).build();
                list.add(annotationSpecColumn);
                annotationSpecColumn = AnnotationSpec.builder(GeneratedValue.class)
                		.addMember("strategy", "$T.IDENTITY", GenerationType.class)
                		.build();
                list.add(annotationSpecColumn);
                AnnotationSpec.builder(GenerationType.class).build();
            } else {
            	annotationSpecColumn = AnnotationSpec.builder(Column.class)
                        .addMember("name", "$S", t.getName().toLowerCase())
                        .build();
            	list.add(annotationSpecColumn);
            }

            Class clazz = String.class;
            if (finalResourceBundle != null) {
                try {
                    String dataType = t.getDataType().toLowerCase();
                    dataType = dataType.lastIndexOf("(") != -1 ? dataType.substring(0, dataType.lastIndexOf("(")) : dataType;
                    clazz = Class.forName(finalResourceBundle.getString(dataType));
                    if (clazz == Date.class) {
                        // 处理日期格式化
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
            /** 添加属性 **/
            FieldSpec fieldSpec = FieldSpec.builder(clazz, getCamelName(t.getName()), Modifier.PRIVATE)
                    .addJavadoc(t.getComment())// 字段注释
                    .addAnnotations(list)
                    .build();
            builder.addField(fieldSpec);
            LOGGER.info("bean生成成功！");
        });

        /** 生成注解 **/
        AnnotationSpec annotationSpecTable = AnnotationSpec.builder(javax.persistence.Table.class)
                .addMember("name", "$S", tableName)
                .build();
        AnnotationSpec annotationSpecEntity = AnnotationSpec.builder(javax.persistence.Entity.class).build();
        AnnotationSpec annotationSpecData = AnnotationSpec.builder(Data.class).build();

        TypeSpec typeSpec = builder
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(annotationSpecData)
//                .superclass(ClassName.bestGuess("com.disney.wdpro.wechat.model.BaseEntity"))
                .addAnnotation(annotationSpecEntity)
                .addAnnotation(annotationSpecTable)
                .addJavadoc(" @Author: Linken Li\n" +
                        " @Description: \n" +
                        " @Date: " + DateUtils.formateDate("yyyy/MM/dd") + ".\n" +
                        " @Modified by\n")
                .build();
        JavaFile javaFile = JavaFile.builder(doMainPackage, typeSpec).build();

        outFile(javaFile);
        return true;
    }

    private void createRepository() throws ClassNotFoundException, NoSuchFieldException, SecurityException {
        ClassName superClass = ClassName.bestGuess(repositoryPackage + ".BaseDao");

        ClassName paramOne = ClassName.bestGuess(doMainPackage + "." + codeModel.getBeanName());// 泛型第一个参数
        Class<?> beanClz = Class.forName(doMainPackage + "." + codeModel.getBeanName());
        String name = beanClz.getDeclaredField("id").getType().getName();
        ClassName paramTwo = ClassName.bestGuess(name);// 泛型第二个参数
        ParameterizedTypeName parameterizedTypeName = ParameterizedTypeName.get(superClass, paramOne, paramTwo);

        TypeSpec typeSpec = TypeSpec.interfaceBuilder(codeModel.getRepositoryName())
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(parameterizedTypeName)
                .addJavadoc("@Author:Linken Li\n@Date: " + DateUtils.formateDate("yyyy/MM/dd") + "\n")
//                .addAnnotation(Repository.class)
                .build();

        JavaFile javaFile = JavaFile.builder(repositoryPackage, typeSpec).build();
        outFile(javaFile);
        LOGGER.info("repository create success！");
    }

    public boolean createServiceClass() throws ClassNotFoundException, NoSuchFieldException, SecurityException {
        ClassName beanClass = ClassName.bestGuess(doMainPackage + "." + codeModel.getBeanName());

        ClassName superClass = ClassName.bestGuess(servicePackage + ".IService");
        
        ClassName paramOne = ClassName.bestGuess(doMainPackage + "." + codeModel.getBeanName());// 泛型第一个参数
        Class<?> beanClz = Class.forName(doMainPackage + "." + codeModel.getBeanName());
        String name = beanClz.getDeclaredField("id").getType().getName();
        ClassName paramTwo = ClassName.bestGuess(name);// 泛型第二个参数
        
        ParameterizedTypeName parameterizedTypeName = ParameterizedTypeName.get(superClass, paramOne, paramTwo);

        TypeSpec typeSpec = TypeSpec.interfaceBuilder(codeModel.getServerName())
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("@Author:Linken Li\n@Date: " + DateUtils.formateDate("yyyy/MM/dd") + "\n")
                .addSuperinterface(parameterizedTypeName)
                .build();

        JavaFile javaFile = JavaFile.builder(servicePackage, typeSpec).build();
        outFile(javaFile);
        LOGGER.info("service create success！");
        return true;
    }

    private void createServiceClassImpl() throws ClassNotFoundException, NoSuchFieldException, SecurityException {
        ClassName repositoryClass = ClassName.bestGuess(repositoryPackage + "." + codeModel.getRepositoryName());
        ClassName superClass = ClassName.bestGuess(servicePackage + "." + codeModel.getServerName());

        ClassName paramOne = ClassName.bestGuess(doMainPackage + "." + codeModel.getBeanName());// 泛型第一个参数
        Class<?> beanClz = Class.forName(doMainPackage + "." + codeModel.getBeanName());
        String name = beanClz.getDeclaredField("id").getType().getName();
        ClassName paramTwo = ClassName.bestGuess(name);// 泛型第二个参数
        ParameterizedTypeName parameterizedTypeName = ParameterizedTypeName.get(superClass, paramOne, paramTwo);
        
        FieldSpec fieldSpec = FieldSpec.builder(repositoryClass, StringUtil.firstLetterLowerCase(codeModel.getRepositoryName()), Modifier.PRIVATE)
                .addAnnotation(Autowired.class)
                .build();

        ClassName baseDao = ClassName.bestGuess(repositoryPackage + ".BaseDao");
        ParameterizedTypeName returnTypeName =ParameterizedTypeName.get(baseDao, paramOne, paramTwo);
        		
        MethodSpec repoMethod = MethodSpec.methodBuilder("getDao")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addCode("return " + StringUtil.firstLetterLowerCase(codeModel.getRepositoryName()) + ";\n")
                .returns(returnTypeName)
                .build();
        
        String beanParm = StringUtil.firstLetterLowerCase(codeModel.getBeanName());
        String repositoryName = StringUtil.firstLetterLowerCase(codeModel.getRepositoryName());


        TypeSpec typeSpec = TypeSpec.classBuilder(codeModel.getServerImplName())
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("@Author:Linken Li\n@Date: " + DateUtils.formateDate("yyyy/MM/dd") + "\n")
                .addAnnotation(Service.class)
                .addAnnotation(Transactional.class)
                .addSuperinterface(superClass)
                .addField(fieldSpec)
                .addMethod(repoMethod)
                .build();

        JavaFile javaFile = JavaFile.builder(serviceImplPackage, typeSpec).build();
        outFile(javaFile);
        LOGGER.info("serviceImpl create success！");
    }

    private void createController() throws ClassNotFoundException {
        ClassName serverClassName = ClassName.bestGuess(servicePackage + "." + codeModel.getServerName());
        ClassName domainClassName = ClassName.bestGuess(doMainPackage + "." + codeModel.getBeanName());
        Class saveReturnClass = Class.forName("com.disney.wdpro.wechat.dto.RR");

        String serverName = StringUtil.firstLetterLowerCase(codeModel.getServerName());
        String domainName = StringUtil.firstLetterLowerCase(codeModel.getBeanName());

        AnnotationSpec rootmapping = AnnotationSpec
                .builder(RequestMapping.class)
                .addMember("value", "$S", "/" + domainName)
                .build();
        
        AnnotationSpec saveAnnotation = AnnotationSpec
                .builder(PostMapping.class)
                .addMember("value", "$S", "/save")
                .build();

        AnnotationSpec deleteAnnotation = AnnotationSpec
                .builder(PostMapping.class)
                .addMember("value", "$S", "/delete")
                .build();

        AnnotationSpec infoAnnotation = AnnotationSpec
                .builder(PostMapping.class)
                .addMember("value", "$S", "/get/{id}")
                .build();

        AnnotationSpec pageListAnnotation = AnnotationSpec
                .builder(PostMapping.class)
                .addMember("value", "$S", "/list")
                .build();

        FieldSpec fieldSpec = FieldSpec.builder(serverClassName, serverName, Modifier.PUBLIC)
                .addAnnotation(Autowired.class)
                .build();

        ParameterSpec infoParm = ParameterSpec.builder(String.class, "id")
                .addAnnotation(PathVariable.class)
                .build();

        MethodSpec saveMethod = MethodSpec.methodBuilder("save")
                .addAnnotation(saveAnnotation)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(domainClassName, domainName)
                .addCode("return null;\n")
                .returns(saveReturnClass)
                .build();

        MethodSpec deleteMethod = MethodSpec.methodBuilder("delete")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(deleteAnnotation)
                .addParameter(String.class, "ids")
                .addCode("return null;\n")
                .returns(saveReturnClass)
                .build();

        MethodSpec infoMethod = MethodSpec.methodBuilder("get")
                .addAnnotation(infoAnnotation)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(infoParm)
                .addCode("return null;\n")
                .returns(saveReturnClass)
                .build();

        MethodSpec pageListMethod = MethodSpec.methodBuilder("list")
                .addAnnotation(pageListAnnotation)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(domainClassName, domainName)
                .addParameter(int.class, "page")
                .addParameter(int.class, "pageSize")
                .addCode("return null;\n")
                .returns(Page.class)
                .build();

        TypeSpec className = TypeSpec.classBuilder(codeModel.getControllerName())
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("@Author:Linken Li\n@Date: " + DateUtils.formateDate("yyyy/MM/dd") + "\n")
                .addAnnotation(RestController.class)
                .addAnnotation(rootmapping)
                .addField(fieldSpec)
                .addMethod(saveMethod)
                .addMethod(deleteMethod)
                .addMethod(infoMethod)
                .addMethod(pageListMethod)
                .build();

        JavaFile javaFile = JavaFile.builder(controllerPackage, className).build();
        outFile(javaFile);
    }

    private void outFile(JavaFile javaFile) {
        try {
            File file = new File((basePath + File.separator + "src" + File.separator + "main" + File.separator + "java"));
            javaFile.writeTo(System.out);
            javaFile.writeTo(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getSql() {
        StringBuffer sb = new StringBuffer();
        if ("mysql".equals(dataBaseType)) {
            sb.append("select COLUMN_NAME as name,column_comment as comment, data_type as dataType, if(column_key='PRI','true','false') from INFORMATION_SCHEMA.Columns\n" +
                    " where table_name='" + tableName + "' and table_schema= '" + dataBaseName + "'");
        } else if ("oracle".equals(dataBaseType)) {
            sb.append("select utc.column_name as 字段名,\n" +
                    "       ucc.comments 注释,\n" +
                    "       utc.data_type 数据类型,\n" +
                    "       CASE UTC.COLUMN_NAME\n" +
                    "         WHEN (select col.column_name\n" +
                    "             from user_constraints con, user_cons_columns col\n" +
                    "            where con.constraint_name = col.constraint_name\n" +
                    "              and con.constraint_type = 'P'\n" +
                    "              and col.table_name = '"+ tableName.toUpperCase() +"') THEN\n" +
                    "          'true'\n" +
                    "         ELSE\n" +
                    "          'false'\n" +
                    "       END AS 主键    \n" +
                    "  from user_tab_columns utc, user_col_comments ucc\n" +
                    " where utc.table_name = ucc.table_name\n" +
                    "   and utc.column_name = ucc.column_name\n" +
                    "   and utc.table_name = '"+ tableName.toUpperCase() +"'\n" +
                    " order by column_id");
        }
        return sb.toString();
    }

    private void initBasePath() {
        this.basePath = this.getClass().getClassLoader().getResource("").getPath();
        try {
            basePath = URLDecoder.decode(basePath, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        basePath = basePath.substring(1, basePath.indexOf("/target"));
    }

    private String getEntityName(String tableName) {
        String[] arr = tableName.split("_");
        String entityName = "";
        for (String str : arr) {
            entityName += StringUtil.firstLetterUppercase(str);
        }

        return entityName;
    }
    
    private String getCamelName(String columnName) {
    	String[] arr = columnName.toLowerCase().split("_");
    	StringBuffer sb = new StringBuffer();
		for (int i = 0; i < arr.length; i++) {
			if (i == 0) {
				sb.append(arr[i]);
			}
			else {
				sb.append(StringUtil.firstLetterUppercase(arr[i]));
			}
		}
		
		return sb.toString();
    }
}
