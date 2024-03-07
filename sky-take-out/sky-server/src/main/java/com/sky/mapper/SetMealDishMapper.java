package com.sky.mapper;

import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetMealDishMapper {


    List<Long> getSetMealByDishId(List<Long> ids);

    void insertBatch(List<SetmealDish> list);

    @Delete("delete from setmeal_dish where setmeal_id = #{setmealId}")
    void deleteBySetMealId(Long setmealId);

    @Select("select * from setmeal_dish where setmeal_id = #{setmealId}")
    List<SetmealDish> getById(Long setmealId);

}
