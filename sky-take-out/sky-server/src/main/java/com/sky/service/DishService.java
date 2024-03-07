package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;

import java.util.List;

public interface DishService {
    void saveWithFlavor(DishDTO dishDTO);

    PageResult pageQuery(DishPageQueryDTO dto);

    void delete(List<Long> ids);

    void update(DishDTO dishDTO);


    DishVO getByIdWithFlavors(Long id);

    List<Dish> getByCategoryId(Long categoryId);
}
