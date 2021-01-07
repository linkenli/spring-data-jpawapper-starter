package jpa.autocode.bean;

import lombok.Data;

@Data
public class Table {
    private String name;// 字段
    private String comment;// 注释
    private String dataType;// 字段类型
    private String columnType;//字段定义
    private String isPri;// 是否主键
}