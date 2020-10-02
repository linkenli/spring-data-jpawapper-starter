package c.b.x.l.test;

import c.b.x.l.test.bean.User;
import c.b.x.l.test.impl.UserServer;
import c.b.x.l.test.repository.UserRepository;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ApplicationTests {

    private final static Logger LOGGER = LoggerFactory.getLogger(ApplicationTests.class);

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserServer userServer;

    @org.junit.Test
    public void save() {
//        System.out.println(userRepository.countHql("select count(1) from User where id = ?1", new Object[]{"1"}));
        Page page = userServer.pageList(new User(), 0, 10);
        System.out.println(page);
    }

}

