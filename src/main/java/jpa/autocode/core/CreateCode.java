package jpa.autocode.core;

import java.util.List;

import javax.persistence.EntityManager;

import jpa.autocode.bean.Parms;

public interface CreateCode {
    void create(EntityManager entityManager, String tableName, List<Parms> parm);
}
