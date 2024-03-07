package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetMealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetMealService;
import com.sky.vo.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class SetMealServiceImpl implements SetMealService {
    @Autowired
    SetMealDishMapper setMealDishMapper;
    @Autowired
    SetmealMapper setmealMapper;
    @Autowired
    DishMapper dishMapper;
    /**
     * 新增套餐
     * @param setmealDTO
     */
    @Override
    public void save(SetmealDTO setmealDTO) {
        //SetmealDish(list) + Setmeal
        //类型合并，Setmeal mapper insert
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.insert(setmeal);

        //主键返回 SetmealDish add
        Long setmealId = setmeal.getId();
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
            setmealDishes.forEach(setmealDish -> {
                //完成Setmeal mapper insert后主键返回
                setmealDish.setSetmealId(setmealId);
            });
        setMealDishMapper.insertBatch(setmealDishes);

    }

    /**
     * 分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    public PageResult page(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(),setmealPageQueryDTO.getPageSize());
        Page<SetmealVO> page = setmealMapper.pageQuery(setmealPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 批量删除
     * @param ids
     */
    @Transactional
    public void delete(List<Long> ids) {
        //在售 -> exception
        ids.forEach(id ->{
            Setmeal byId = setmealMapper.getById(id);
            if (StatusConstant.ENABLE.equals(byId.getStatus())){
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        });
        //del 关联 mapper
        ids.forEach(id ->{
           setmealMapper.delete(id);
           setMealDishMapper.deleteBySetMealId(id);
        });
    }



    /**
     * 修改套餐
     * @param setmealDTO
     */
    @Override
    public void update(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);
        setmealMapper.update(setmeal);

        //套餐对应多菜品，foreach -> setmealId 统合入list
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        if (setmealDTO.getId() != null){
            setMealDishMapper.deleteBySetMealId(setmealDTO.getId());
            setmealDishes.forEach(setmealDish -> {
                setmealDish.setDishId(setmealDTO.getId());
            });
        }
        setMealDishMapper.insertBatch(setmealDishes);

    }

    /**
     * getById 回显 查找合并 -> SetmealVO
     * @param id
     * @return
     */
    @Override
    public SetmealVO getById(Long id) {
//        Setmeal setmeal = setmealMapper.getById(id);
//        SetmealVO setmealVO = new SetmealVO();
//        List<SetmealDish> list = setMealDishMapper.getById(id);
//        BeanUtils.copyProperties(setmeal,setmealVO);
//        setmealVO.setSetmealDishes(list);
        SetmealVO byId = setmealMapper.getByIdWithDishes(id);
        return byId;
    }

    @Override
    public void setStatus(Integer status, Long id) {
        if (status.equals(StatusConstant.ENABLE)){
            List<Dish> list = dishMapper.getByDishIdFromSetmealDish(id);
            if (list != null && list.size() > 0){
                list.forEach(dish -> {
                    if (StatusConstant.DISABLE == dish.getStatus()){
                        throw new SetmealEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                    }
                });
            }
        }
        Setmeal setmeal = Setmeal.builder()
                .id(id)
                .status(status)
                .build();
        setmealMapper.update(setmeal);
    }
}
