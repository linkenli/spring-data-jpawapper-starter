package c.b.x.l.test.repository;

import c.b.x.l.test.bean.User;
import jpa.repository.BaseRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import javax.transaction.Transactional;

public interface UserRepository extends BaseRepository<User, String> {
    User findByUserName(String userName);

    @Transactional
    @Modifying
    @Query("update User set userName = ?1 where id = ?2")
    void updateName(String name, String id);
}
