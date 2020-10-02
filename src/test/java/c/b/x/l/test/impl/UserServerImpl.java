package c.b.x.l.test.impl;

import c.b.x.l.test.bean.User;
import c.b.x.l.test.repository.UserRepository;
import jpa.autocode.util.UUIDUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @Author:LiuBingXu
 * @Date: 2019/01/28
 */
@Service
@Transactional
public class UserServerImpl implements UserServer {
  @Autowired
  private UserRepository userRepository;

  @Override
  public User saveOrUpdate(User user) {
      if (StringUtils.isEmpty(user.getId())) {
      user.setId(UUIDUtils.getUUID());
      }
    return userRepository.save(user);
  }

  @Override
  public User getUserById(String id) {
      return userRepository.findById(id ).get();
  }

  @Override
  public boolean deleteUserByIds(String ids) {
      String[] idArr = ids.split(",");
      userRepository.batchDelete(Arrays.asList(idArr));
      return true;
  }

  public Specification toPredicate(User user) {
     return (Specification<User>) (root, criteriaQuery, criteriaBuilder) -> {
         List<Predicate> predicate = new ArrayList<>();
         if (org.apache.commons.lang3.StringUtils.isNotBlank(user.getId())) {
             predicate.add(criteriaBuilder.equal(root.get("id"), user.getId()));
         }
         return criteriaQuery.where(predicate.toArray(new Predicate[predicate.size()])).getRestriction();
     };
  }

  @Override
  public Page pageList(User user, int page, int pageSize) {
      Sort sort = Sort.by(Sort.Direction.DESC, "id");
      Pageable pageable = PageRequest.of(page, pageSize, sort);
      return userRepository.pageList(pageable, toPredicate(user));
  }
}
