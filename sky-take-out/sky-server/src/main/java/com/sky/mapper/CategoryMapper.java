package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.enumeration.OperationType;
import com.sky.result.PageResult;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CategoryMapper {


    @AutoFill(value = OperationType.INSERT)
    void save(Category category);

    Page<Category> pageQuery(CategoryPageQueryDTO categoryPageQueryDTO);

    @AutoFill(value = OperationType.UPDATE)
    void update(Category category);

    @Delete("delete from sky_take_out.category where id = #{id}")
    void delete(Long id);


    List<Category> listResult(Integer type);
}
