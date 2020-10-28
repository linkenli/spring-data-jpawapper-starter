package jpa.autocode.controller;

import com.alibaba.fastjson.JSON;

import jpa.JpaGenProperties;
import jpa.autocode.bean.Parms;
import jpa.autocode.core.CreateCode;
import jpa.autocode.core.JavaCreate;
import jpa.autocode.util.ParmsUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.EntityManager;
import java.util.List;

@RestController
public class CodeController {

	@Autowired
    private EntityManager entityManager;
	@Autowired
	@Qualifier("javaCreate")
	private CreateCode javaCreate;
	@Autowired
	private JpaGenProperties jpaGenProperties;

    @PostMapping(value = "/code/create")
    public ResponseEntity createCode(String parmsList) {
    	if (!jpaGenProperties.isEnable()) {
            return ResponseEntity.ok("未启用代码生成");
        }
        List<Parms> parm = JSON.parseArray(parmsList, Parms.class);
        if (ParmsUtil.getValueByKey(parm, "table").size() == 0 || ParmsUtil.getValueByKey(parm, "table").isEmpty()) {
            return ResponseEntity.ok("请选择表名，类似这样的dev_?");
        }
        
        try {
        	javaCreate.create(entityManager, ParmsUtil.getValueByKey(parm, "table").get(0), parm);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok("代码生成失败！");
        }
        return ResponseEntity.ok("代码生成成功！");
    }

    @RequestMapping("/show")
    public JpaGenProperties showProperties() {
    	return jpaGenProperties;
    }
    
    @PostMapping(value = "/loadtable")
    public ResponseEntity loadtable() {
        if (!jpaGenProperties.isEnable()) {
            return ResponseEntity.ok("未启用代码生成");
        }
        StringBuffer sb = new StringBuffer();
        if ("mysql".equals(jpaGenProperties.getDatabaseType())) {
            sb.append("select table_name from information_schema.tables where table_schema=? and table_type='base table'");
            return ResponseEntity.ok(entityManager.createNativeQuery(sb.toString())
                    .setParameter(1, jpaGenProperties.getDatabaseName()).getResultList());
        } else if ("oracle".equals(jpaGenProperties.getDatabaseType())) {
            sb.append("select table_name from user_tab_comments");
        }
        return ResponseEntity.ok(entityManager.createNativeQuery(sb.toString()).getResultList());
    }


}
