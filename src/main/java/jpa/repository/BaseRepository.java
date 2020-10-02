package jpa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.io.Serializable;
import java.util.List;

@NoRepositoryBean
public interface BaseRepository<T, ID extends Serializable> extends JpaRepository<T, ID>
        , JpaSpecificationExecutor<T> {

    boolean support(String modelType);

    /**
     * 查询集合
     *
     * @param hql hql语句
     * @return 集合
     */
    List<T> listHql(String hql);

    /**
     * 查询集合
     *
     * @param sql sql语句
     * @return 集合
     */
    List<T> listSql(String sql);

    /**
     * 分页查询
     *
     * @param hql  hql语句
     * @param page 页码从0开始
     * @param size 一页大小
     * @return 集合
     */
    List<T> listPageHql(String hql, int page, int size);

    /**
     * 分页查询
     *
     * @param hql    hql语句
     * @param page   页码从0开始
     * @param size   一页大小
     * @param params 参数
     * @return 集合
     */
    List<T> listPageHql(String hql, int page, int size, List<Object> params);

    /**
     * 分页查询
     *
     * @param hql    hql语句
     * @param page   页码从0开始
     * @param size   一页大小
     * @param params 参数
     * @return 集合
     */
    List<T> listPageHql(String hql, int page, int size, Object[] params);

    /**
     * 分页查询
     *
     * @param sql  sql
     * @param page 页码从0开始
     * @param size 一页大小
     * @return 集合
     */
    List<T> listPageSql(String sql, int page, int size);

    /**
     * 分页查询
     *
     * @param sql    sql
     * @param page   页码从0开始
     * @param size   一页大小
     * @param params 参数
     * @return 集合
     */
    List<T> listPageSql(String sql, int page, int size, List<Object> params);

    /**
     * 分页查询
     *
     * @param sql    sql
     * @param page   页码从0开始
     * @param size   一页大小
     * @param params 参数
     * @return 集合
     */
    List<T> listPageSql(String sql, int page, int size, Object[] params);

    /**
     * 查询对象
     *
     * @param sql    sql语句
     * @param params 参数
     * @return 对象
     */
    T findSql(String sql, Object[] params);

    /**
     * 查询对象
     *
     * @param sql    sql语句
     * @param params 参数
     * @return 对象
     */
    T findSql(String sql, List<Object> params);

    /**
     * 查询对象
     *
     * @param hql    hql语句
     * @param params 参数
     * @return 对象
     */
    T findHql(String hql, Object[] params);

    /**
     * 查询对象
     *
     * @param hql    hql语句
     * @param params 参数
     * @return 对象
     */
    T findHql(String hql, List<Object> params);

    /**
     * 查询集合对象
     *
     * @param sql    sql语句
     * @param params 数组参数
     * @return 集合对象
     */
    List<T> findListSql(String sql, Object[] params);

    /**
     * 查询集合对象
     *
     * @param sql    sql语句
     * @param params 集合参数
     * @return 集合对象
     */
    List<T> findListSql(String sql, List<Object> params);

    /**
     * 查询集合对象
     *
     * @param sql    sql语句
     * @param params 数组参数
     * @return 集合对象
     */
    List<Object[]> getListSql(String sql, Object[] params);

    /**
     * 查询集合对象
     *
     * @param sql    sql语句
     * @param params 集合参数
     * @return 集合对象
     */
    List<Object[]> getListSql(String sql, List<Object> params);

    /**
     * 分页查询
     *
     * @param pageable 参数
     * @return 分页结果
     */
    Page<T> pageList(Pageable pageable);

    /**
     * 分页查询
     *
     * @param pageable       参数
     * @param specifications 参数条件
     * @return 分页结果
     */
    Page<T> pageList(Pageable pageable, Specification specifications);

    /**
     * 执行sql语句
     *
     * @param sql sql语句
     * @return 返回1成功，0失败
     */
    int executeUpdateSql(String sql);

    /**
     * 执行sql语句
     *
     * @param sql    sql语句
     * @param params 集合参数
     * @return 返回1成功，0失败
     */
    int executeUpdateSql(String sql, List<Object> params);

    /**
     * 执行sql语句
     *
     * @param sql    sql语句
     * @param params 数组参数
     * @return 返回1成功，0失败
     */
    int executeUpdateSql(String sql, Object[] params);

    /**
     * 执行hql语句
     *
     * @param hql hql语句
     * @return 返回1成功，0失败
     */
    int executeUpdateHql(String hql);

    /**
     * 执行hql语句
     *
     * @param hql    hql语句
     * @param params 集合参数
     * @return 返回1成功，0失败
     */
    int executeUpdateHql(String hql, List<Object> params);

    /**
     * 执行hql语句
     *
     * @param hql    hql语句
     * @param params 数组参数
     * @return 返回1成功，0失败
     */
    int executeUpdateHql(String hql, Object[] params);

    /**
     * 批量删除
     *
     * @param ids 主键集合
     */
    void batchDelete(List<ID> ids);

    /**
     * 求总数
     *
     * @param hql hql语句
     * @return 总数
     */
    Long countHql(String hql);

    /**
     * 求总数
     *
     * @param hql    hql语句
     * @param params 集合参数
     * @return 总数
     */
    Long countHql(String hql, List<Object> params);

    /**
     * 求总数
     *
     * @param hql    hql语句
     * @param params 数组参数
     * @return 总数
     */
    Long countHql(String hql, Object[] params);

    /**
     * 求总数
     *
     * @param sql sql语句
     * @return 总数
     */
    Long countSql(String sql);

    /**
     * 求总数
     *
     * @param sql    sql语句
     * @param params 集合参数
     * @return 总数
     */
    Long countSql(String sql, List<Object> params);

    /**
     * 求总数
     *
     * @param sql    sql语句
     * @param params 数组参数
     * @return 总数
     */
    Long countSql(String sql, Object[] params);

    /**
     * 查询一个字段值
     *
     * @param sql sql语句
     * @return 返回一个Object值
     */
    Object findObjSql(String sql);

    /**
     * 查询一个字段值
     *
     * @param sql    sql语句
     * @param params 集合参数
     * @return 返回一个Object值
     */
    Object findObjSql(String sql, List<Object> params);

    /**
     * 查询一个字段值
     *
     * @param sql    sql语句
     * @param params 数组参数
     * @return 返回一个Object值
     */
    Object findObjSql(String sql, Object[] params);

    /**
     * 查询一个字段值
     *
     * @param hql hql语句
     * @return 返回一个Object值
     */
    Object findObjHql(String hql);

    /**
     * 查询一个字段值
     *
     * @param hql    hql语句
     * @param params 集合参数
     * @return 返回一个Object值
     */
    Object findObjHql(String hql, List<Object> params);

    /**
     * 查询一个字段值
     *
     * @param hql    shql语句
     * @param params 数组参数
     * @return 返回一个Object值
     */
    Object findObjHql(String hql, Object[] params);
}