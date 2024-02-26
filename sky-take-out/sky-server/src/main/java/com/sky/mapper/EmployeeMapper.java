package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface EmployeeMapper {

    /**
     * 根据用户名查询员工
     * @param username
     * @return
     */
    @Select("select * from employee where username = #{username}")
    Employee getByUsername(String username);


    void insert(Employee employee);

    Page pageQuery(EmployeePageQueryDTO employeePageQueryDTO);

    /**
     * 修改（通用）
     * @param employee
     */
    void update(Employee employee);

    /**
     *
     * @return
     */
    @Select("select * from employee where id = #{id}")
    Employee getByid(Long id);
}
