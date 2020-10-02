package jpa.autocode.controller;

import com.alibaba.fastjson.JSON;
import jpa.autocode.bean.Parms;
import jpa.autocode.core.CreateCode;
import jpa.autocode.core.JavaCreate;
import jpa.autocode.util.ParmsUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.EntityManager;
import java.util.List;

@RestController
public class CodeController {

    @Autowired
    private EntityManager entityManager;
    @Value("${code-create.database-name}")
    private String dataBaseName;
    @Value("${code-create.bean-package}")
    private String doMainPackage;
    @Value("${code-create.service-package}")
    private String servicePackage;
    @Value("${code-create.service-impl-package}")
    private String serviceImplPackag;
    @Value("${code-create.repository-package}")
    private String repositoryPackage;
    @Value("${code-create.controller-package}")
    private String controllerPackage;
    @Value("${code-create.enable}")
    private String enable;
    @Value("${code-create.database-name}")
    private String dataTableName;
    @Value("${code-create.database-type}")
    private String dataBaseType;

    @PostMapping(value = "/code/create")
    public ResponseEntity createCode(String parmsList) {
        List<Parms> parm = JSON.parseArray(parmsList, Parms.class);
        if (ParmsUtil.getValueByKey(parm, "table").size() == 0 || ParmsUtil.getValueByKey(parm, "table").isEmpty()) {
            return ResponseEntity.ok("请选择表名，类似这样的dev_?");
        }
        CreateCode createCode = new JavaCreate(entityManager, dataBaseName, ParmsUtil.getValueByKey(parm, "table").get(0)
                , doMainPackage, servicePackage, serviceImplPackag, repositoryPackage, controllerPackage, dataBaseType, parm);
        if (!"true".equals(enable)) {
            return ResponseEntity.ok("未启用代码生成");
        }
        try {
            createCode.create();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.ok("代码生成成功！");
    }

    @PostMapping(value = "/loadtable")
    public ResponseEntity loadtable() {
        if (!"true".equals(enable)) {
            return ResponseEntity.ok("未启用代码生成");
        }
        StringBuffer sb = new StringBuffer();
        if ("mysql".equals(dataBaseType)) {
            sb.append("select table_name from information_schema.tables where table_schema=? and table_type='base table'");
            return ResponseEntity.ok(entityManager.createNativeQuery(sb.toString())
                    .setParameter(1, dataTableName).getResultList());
        } else if ("oracle".equals(dataBaseType)) {
            sb.append("select table_name from user_tab_comments");
        }
        return ResponseEntity.ok(entityManager.createNativeQuery(sb.toString()).getResultList());
    }


}
