package jpa.autocode.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;

import javax.lang.model.element.Modifier;
import javax.persistence.Column;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import jpa.JpaGenProperties;
import jpa.autocode.bean.CodeModel;
import jpa.autocode.bean.Parms;
import jpa.autocode.bean.Table;
import jpa.autocode.util.DateUtils;
import jpa.autocode.util.ParmsUtil;
import jpa.autocode.util.StringUtil;
import lombok.Data;

@Data
@Service("javaCreate")
public class JavaCreate implements CreateCode {
    private final static Logger LOGGER = LoggerFactory.getLogger(JavaCreate.class);

    @Autowired
    private JpaGenProperties jpaGenProperties;

    private EntityManager entityManager;
    protected CodeModel codeModel = new CodeModel();
    protected String tableName;// 表名
    protected String version = "V 1.0.5";// 版本
    protected String basePath;// 绝对路径前缀
    protected List<Parms> parm;// 参数
    protected List<String> createInstance;// 创建实例

    private Class idType;

    @Override
    public void create(EntityManager entityManager, String tableName, List<Parms> parm) {
        LOGGER.info("tableName={}", tableName);
        LOGGER.info("parm={}", parm);

        checkProperties(tableName);

        this.entityManager = entityManager;
        this.tableName = tableName;
        this.parm = parm;
        this.createInstance = ParmsUtil.getValueByKey(this.parm, "type_c");

        this.initBasePath();

        String sql = this.getSql();
        List<Object[]> resultList = entityManager.createNativeQuery(sql).getResultList();

        // 查询数据库
        List<Table> tableList = new ArrayList<>();
        resultList.forEach(t -> {
            Table table = new Table();
            table.setName(StringUtil.objToStr(t[0]));
            table.setComment(StringUtil.objToStr(t[1]));
            table.setDataType(StringUtil.objToStr(t[2]));
            table.setColumnType(StringUtil.objToStr(t[3]));
            table.setIsPri(StringUtil.objToStr(t[4]));
            tableList.add(table);
        });

        // 准备相关名
        codeModel.setBeanName(getEntityName(tableName));
        codeModel.setRepositoryName(codeModel.getBeanName() + "Repository");
        codeModel.setServiceName(codeModel.getBeanName() + "Service");
        codeModel.setServiceImplName(codeModel.getServiceName() + "Impl");
        codeModel.setControllerName(codeModel.getBeanName() + "Controller");

        // 生成代码
        try {
            this.newThreadCreateCode(tableName, tableList);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkProperties(String tableName) {
        Assert.notNull(jpaGenProperties.getDatabaseName(), "数据库名不能为空！");
        Assert.notNull(jpaGenProperties.getDatabaseType(), "数据库类型不能为空！");
        Assert.notNull(tableName, "表不能为空！");
        Assert.notNull(jpaGenProperties.getBeanPackage(), "实体类路径不能为空！");
        Assert.notNull(jpaGenProperties.getServicePackage(), "service 路径不能为空！");
        Assert.notNull(jpaGenProperties.getServiceImplPackage(), "service 实现类路径不能为空！");
        Assert.notNull(jpaGenProperties.getRepositoryPackage(), "repository 包路径不能为空！");
        Assert.notNull(jpaGenProperties.getControllerPackage(), "controller 包路径不能为空！");
    }

    void newThreadCreateCode(String tableName, List<Table> tableList) throws InterruptedException, ClassNotFoundException, NoSuchFieldException, SecurityException {
        // 生成domain
        this.createDomainClass(tableName, tableList);
//        Thread.sleep(1000);

        if (createInstance.contains("repository")) {
            // 生成repository
            this.createRepository();
//            Thread.sleep(1000);
        }

        if (createInstance.contains("service")) {
            // 生成service接口
            this.createServiceClass();
//            Thread.sleep(1000);
        }

        if (createInstance.contains("serviceImpl")) {
            // 生成service接口实现类
            this.createServiceClassImpl();
//            Thread.sleep(1000);
        }
        if (createInstance.contains("controller")) {
            // 生成controller
            this.createController();
        }
    }

    public boolean createDomainClass(String tableName, List<Table> tableList) throws ClassNotFoundException {
        /** 读取mysql转Java类型配置 **/
        InputStream in = null;
        if ("mysql".equals(jpaGenProperties.getDatabaseType())) {
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


        String baseEntity = jpaGenProperties.getBeanPackage() + ".BaseEntity";
        Class<?> clz = Class.forName(baseEntity);
        Set<String> allFields = getAllFields(clz);

        TypeSpec.Builder builder = TypeSpec.classBuilder(codeModel.getBeanName());
        ResourceBundle finalResourceBundle = resourceBundle;
        tableList.forEach(t -> {

            if (clz == null || allFields.contains(getCamelName(t.getName()))) {
                return;
            }
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
                if (",created_time,updated_time,".contains("," + t.getName().toLowerCase() + ",")) {
                    annotationSpecColumn = AnnotationSpec.builder(Column.class)
                            .addMember("name", "$S", t.getName().toLowerCase())
                            .addMember("insertable", "$L", false)
                            .addMember("updatable", "$L", false)
                            .build();
                    list.add(annotationSpecColumn);
                } else {
                    annotationSpecColumn = AnnotationSpec.builder(Column.class)
                            .addMember("name", "$S", t.getName().toLowerCase())
                            .addMember("columnDefinition", "$S", t.getDataType())
                            .build();
                    list.add(annotationSpecColumn);
                }
            }

            String columnName = t.getName();
            Class clazz = String.class;
            if (finalResourceBundle != null) {
                try {
                    String dataType = t.getDataType().toLowerCase();
                    dataType = dataType.lastIndexOf("(") != -1 ? dataType.substring(0, dataType.lastIndexOf("(")) : dataType;
                    clazz = Class.forName(finalResourceBundle.getString(dataType));
                    if (clazz == Date.class) {
                        // 处理日期格式化
                    }
                    if (t.getIsPri().equals("true")) {
                        idType = clazz;
                    }  else if (t.getColumnType().equals("tinyint(1)")) {
                        clazz = boolean.class;
                        if (columnName.startsWith("is_")) {
                            columnName = StringUtils.substringAfter(columnName, "is_");
                        }
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }

            String fieldName = getCamelName(columnName);
            /** 添加属性 **/
            FieldSpec fieldSpec = FieldSpec.builder(clazz, fieldName, Modifier.PRIVATE)
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
                .superclass(ClassName.bestGuess(baseEntity))
                .addAnnotation(annotationSpecEntity)
                .addAnnotation(annotationSpecTable)
                .addJavadoc(" @Author: Linken Li\n" +
                        " @Description: \n" +
                        " @Date: " + DateUtils.formateDate("yyyy/MM/dd") + ".\n" +
                        " @Modified by\n")
                .build();
        JavaFile javaFile = JavaFile.builder(jpaGenProperties.getBeanPackage(), typeSpec).build();

        outFile(javaFile);
        return true;
    }

    private Set<String> getAllFields(Class<?> clz) {
        Field[] declaredFields = clz.getDeclaredFields();
        Set<String> set = new HashSet<String>();
        for (Field f : declaredFields) {
            set.add(f.getName());
        }

        return set;
    }

    private void createRepository() throws ClassNotFoundException, NoSuchFieldException, SecurityException {
        ClassName superClass = ClassName.bestGuess(jpaGenProperties.getRepositoryPackage() + ".BaseRepository");

        ClassName paramOne = ClassName.bestGuess(jpaGenProperties.getBeanPackage() + "." + codeModel.getBeanName());// 泛型第一个参数

        ClassName paramTwo = ClassName.bestGuess(idType.getName());// 泛型第二个参数
        ParameterizedTypeName parameterizedTypeName = ParameterizedTypeName.get(superClass, paramOne, paramTwo);

        TypeSpec typeSpec = TypeSpec.interfaceBuilder(codeModel.getRepositoryName())
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(parameterizedTypeName)
                .addJavadoc("@Author:Linken Li\n@Date: " + DateUtils.formateDate("yyyy/MM/dd") + "\n")
//                .addAnnotation(Repository.class)
                .build();

        JavaFile javaFile = JavaFile.builder(jpaGenProperties.getRepositoryPackage(), typeSpec).build();
        outFile(javaFile);
        LOGGER.info("repository create success！");
    }

    public boolean createServiceClass() throws ClassNotFoundException, NoSuchFieldException, SecurityException {
        ClassName beanClass = ClassName.bestGuess(jpaGenProperties.getBeanPackage() + "." + codeModel.getBeanName());

        ClassName superClass = ClassName.bestGuess(jpaGenProperties.getServicePackage() + ".IService");

        ClassName paramOne = ClassName.bestGuess(jpaGenProperties.getBeanPackage() + "." + codeModel.getBeanName());// 泛型第一个参数
        ClassName paramTwo = ClassName.bestGuess(idType.getName());// 泛型第二个参数

        ParameterizedTypeName parameterizedTypeName = ParameterizedTypeName.get(superClass, paramOne, paramTwo);

        TypeSpec typeSpec = TypeSpec.interfaceBuilder(codeModel.getServiceName())
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("@Author:Linken Li\n@Date: " + DateUtils.formateDate("yyyy/MM/dd") + "\n")
                .addSuperinterface(parameterizedTypeName)
                .build();

        JavaFile javaFile = JavaFile.builder(jpaGenProperties.getServicePackage(), typeSpec).build();
        outFile(javaFile);
        LOGGER.info("service create success！");
        return true;
    }

    private void createServiceClassImpl() throws ClassNotFoundException, NoSuchFieldException, SecurityException {
        ClassName repositoryClass = ClassName.bestGuess(jpaGenProperties.getRepositoryPackage() + "." + codeModel.getRepositoryName());
        ClassName superClass = ClassName.bestGuess(jpaGenProperties.getServicePackage() + "." + codeModel.getServiceName());

        ClassName paramOne = ClassName.bestGuess(jpaGenProperties.getBeanPackage() + "." + codeModel.getBeanName());// 泛型第一个参数
        ClassName paramTwo = ClassName.bestGuess(idType.getName());// 泛型第二个参数
        ParameterizedTypeName parameterizedTypeName = ParameterizedTypeName.get(superClass, paramOne, paramTwo);

        FieldSpec fieldSpec = FieldSpec.builder(repositoryClass, StringUtil.firstLetterLowerCase(codeModel.getRepositoryName()), Modifier.PRIVATE)
                .addAnnotation(Autowired.class)
                .build();

        ClassName baseRepository = ClassName.bestGuess(jpaGenProperties.getRepositoryPackage() + ".BaseRepository");
        ParameterizedTypeName returnTypeName = ParameterizedTypeName.get(baseRepository, paramOne, paramTwo);

        MethodSpec repoMethod = MethodSpec.methodBuilder("getDao")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addCode("return " + StringUtil.firstLetterLowerCase(codeModel.getRepositoryName()) + ";\n")
                .returns(returnTypeName)
                .build();

        String beanParm = StringUtil.firstLetterLowerCase(codeModel.getBeanName());
        String repositoryName = StringUtil.firstLetterLowerCase(codeModel.getRepositoryName());


        TypeSpec typeSpec = TypeSpec.classBuilder(codeModel.getServiceImplName())
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("@Author:Linken Li\n@Date: " + DateUtils.formateDate("yyyy/MM/dd") + "\n")
                .addAnnotation(Service.class)
                .addAnnotation(Transactional.class)
                .addSuperinterface(superClass)
                .addField(fieldSpec)
                .addMethod(repoMethod)
                .build();

        JavaFile javaFile = JavaFile.builder(jpaGenProperties.getServiceImplPackage(), typeSpec).build();
        outFile(javaFile);
        LOGGER.info("serviceImpl create success！");
    }

    private void createController() throws ClassNotFoundException, NoSuchFieldException, SecurityException {
        ClassName serviceClassName = ClassName.bestGuess(jpaGenProperties.getServicePackage() + "." + codeModel.getServiceName());
        ClassName domainClassName = ClassName.bestGuess(jpaGenProperties.getBeanPackage() + "." + codeModel.getBeanName());
        ClassName defaultReturnClass = ClassName.bestGuess("com.disney.wdpro.was.common.model.CommonReturnModel");
        ClassName list = ClassName.get("java.util", "List");
        ParameterizedTypeName parameterizedList = ParameterizedTypeName.get(list, domainClassName);
//        Class saveReturnClass = Class.forName("com.disney.wdpro.wechat.dto.RR");

        String serviceName = StringUtil.firstLetterLowerCase(codeModel.getServiceName());
        String domainName = StringUtil.firstLetterLowerCase(codeModel.getBeanName());

        ClassName paramClz = ClassName.bestGuess(idType.getName());// 泛型第二个参数

        AnnotationSpec rootmapping = AnnotationSpec
                .builder(RequestMapping.class)
                .addMember("value", "$S", "/api/" + domainName)
                .build();

        AnnotationSpec saveAnnotation = AnnotationSpec
                .builder(PostMapping.class)
                .addMember("value", "$S", "/save")
                .build();

        AnnotationSpec deleteAnnotation = AnnotationSpec
                .builder(DeleteMapping.class)
                .addMember("value", "$S", "/delete/{id}")
                .build();

        AnnotationSpec infoAnnotation = AnnotationSpec
                .builder(GetMapping.class)
                .addMember("value", "$S", "/get/{id}")
                .build();

        AnnotationSpec pageListAnnotation = AnnotationSpec
                .builder(PostMapping.class)
                .addMember("value", "$S", "/list")
                .build();

        FieldSpec fieldSpec = FieldSpec.builder(serviceClassName, serviceName, Modifier.PRIVATE)
                .addAnnotation(Autowired.class)
                .build();

        ParameterSpec idParm = ParameterSpec.builder(paramClz, "id")
                .addAnnotation(PathVariable.class)
                .build();


        ParameterSpec domainParm = ParameterSpec.builder(domainClassName, domainName)
                .addAnnotation(RequestBody.class)
                .build();

        MethodSpec saveMethod = MethodSpec.methodBuilder("save")
                .addAnnotation(saveAnnotation)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(domainParm)
                .addCode(String.format("\nreturn %s.save(%s);\n", serviceName, domainName))
                .returns(domainClassName)
                .build();

        MethodSpec deleteMethod = MethodSpec.methodBuilder("delete")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(deleteAnnotation)
                .addParameter(idParm)
                .addCode(String.format("\n %s.deleteById(id);\n return CommonReturnModel.successResponse();\n", serviceName))
                .returns(defaultReturnClass)
                .build();

        MethodSpec infoMethod = MethodSpec.methodBuilder("get")
                .addAnnotation(infoAnnotation)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(idParm)
                .addCode(String.format("\nreturn %s.findById(id);\n", serviceName))
                .returns(domainClassName)
                .build();

        MethodSpec pageListMethod = MethodSpec.methodBuilder("list")
                .addAnnotation(pageListAnnotation)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(domainParm)
                .addCode(String.format("\nreturn %s.findAll();\n", serviceName))
                .returns(parameterizedList)
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

        JavaFile javaFile = JavaFile.builder(jpaGenProperties.getControllerPackage(), className).build();
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
        if ("mysql".equals(jpaGenProperties.getDatabaseType())) {
            sb.append("select COLUMN_NAME as name,column_comment as comment, data_type as dataType, column_type, if(column_key='PRI','true','false') from INFORMATION_SCHEMA.Columns\n" +
                    " where table_name='" + tableName + "' and table_schema= '" + jpaGenProperties.getDatabaseName() + "'");
        } else if ("oracle".equals(jpaGenProperties.getDatabaseType())) {
            sb.append("select utc.column_name as 字段名,\n" +
                    "       ucc.comments 注释,\n" +
                    "       utc.data_type 数据类型,\n" +
                    "       null 数据定义,\n" +
                    "       CASE UTC.COLUMN_NAME\n" +
                    "         WHEN (select col.column_name\n" +
                    "             from user_constraints con, user_cons_columns col\n" +
                    "            where con.constraint_name = col.constraint_name\n" +
                    "              and con.constraint_type = 'P'\n" +
                    "              and col.table_name = '" + tableName.toUpperCase() + "') THEN\n" +
                    "          'true'\n" +
                    "         ELSE\n" +
                    "          'false'\n" +
                    "       END AS 主键    \n" +
                    "  from user_tab_columns utc, user_col_comments ucc\n" +
                    " where utc.table_name = ucc.table_name\n" +
                    "   and utc.column_name = ucc.column_name\n" +
                    "   and utc.table_name = '" + tableName.toUpperCase() + "'\n" +
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
        String os = System.getProperties().getProperty("os.name");
        if (os.startsWith("Windows")) {
            basePath = basePath.substring(1, basePath.indexOf("/target"));
        } else {
            basePath = "/" + basePath.substring(1, basePath.indexOf("/target"));
        }
        LOGGER.info("basePath={}", basePath);
    }

    private String getEntityName(String tableName) {
        if (tableName.toLowerCase().startsWith("tb_")) {
            tableName = tableName.substring(3);
        }
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
            } else {
                sb.append(StringUtil.firstLetterUppercase(arr[i]));
            }
        }

        return sb.toString();
    }
}
