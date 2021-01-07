package c.b.x.l.test.impl;

import c.b.x.l.test.bean.User;
import org.springframework.data.domain.Page;

/**
 * @Author:LiuBingXu
 * @Date: 2019/01/28
 */
public interface UserServer {
    User saveOrUpdate(User user);

    User getUserById(String id);

    boolean deleteUserByIds(String ids);

    Page pageList(User user, int page, int pageSize);
}
