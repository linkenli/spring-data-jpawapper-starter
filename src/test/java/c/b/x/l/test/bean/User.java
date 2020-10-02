package c.b.x.l.test.bean;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Date;

/**
 * @Author:LiuBingXu
 * @Description: 用户实体类
 * @Date: 2018/6/24.
 * @Modified by
 */
@Data
@Entity
@Table(name="dev_user")
public class User implements Serializable {

    @Id
    private String id;
    @Column(name="username")
    private String userName;
    @Column(name="sex")
    private String sex;
    @Column(name="city")
    private String city;
    @Column(name="sign")
    private String sign;
    @Column(name="experience")
    private String experience;
    @Column(name="score")
    private String score;
    @Column(name="classify")
    private String classify;
    @Column(name="wealth")
    private String wealth;
    @Column(name="createtime")
    private Date createtime;
}
