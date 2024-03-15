package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishOverViewVO;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@Slf4j
@RequestMapping("/admin/dish")
@Api(tags = "菜品相关接口")
public class DishController {
    private static final String DISH_ID = "dish_id ";
    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 新增菜品
     * @param dishDTO
     * @return
     */
    @PostMapping
    @ApiOperation("新增菜品")
    public Result save(@RequestBody DishDTO dishDTO){
        log.info("新增菜品",dishDTO);
        dishService.saveWithFlavor(dishDTO);


        //清理缓存数据
        Long l = redisCacheDel(DISH_ID + dishDTO.getCategoryId());
        log.info("redis del:{}",l);
        return Result.success();
    }

    /**
     * 菜品分页查询
     * @param dto
     * @return
     */
    @GetMapping("/page")
    @ApiOperation("菜品分页查询")
    public Result<PageResult> page(DishPageQueryDTO dto){
        log.info("菜品分页查询{}",dto);
        PageResult pageResult = dishService.pageQuery(dto);
        return Result.success(pageResult);
    }

    /**
     * 批量删除菜品
     * @param ids
     * @return
     */
    @DeleteMapping
    @ApiOperation("批量删除菜品")
    public Result delete(@RequestParam List<Long> ids){
        log.info("批量删除菜品");
        dishService.delete(ids);
        Long l = redisCacheDel(DISH_ID + "*");
        log.info("redis del:{}",l);
        return Result.success();
    }

    @GetMapping("{id}")
    @ApiOperation("根据id查询菜品")
    public Result<DishVO> getById(@PathVariable Long id){
        log.info("根据id查询菜品{}",id);
        DishVO byId = dishService.getByIdWithFlavors(id);
        return Result.success(byId);
    }

    @PutMapping
    @ApiOperation("修改菜品")
    public Result<DishVO> update(@RequestBody DishDTO dishDTO){
        log.info("修改菜品{}",dishDTO);

        dishService.update(dishDTO);
        //cacheDel
        Long l = redisCacheDel(DISH_ID + dishDTO.getCategoryId());
        log.info("redis del:{}",l);
        return Result.success();
    }

    /**
     * 根据分类ID查询
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类ID查询")
    public Result<List<Dish>> getByCategoryId(Long categoryId){
        log.info("根据分类ID查询{}", categoryId);
        List<Dish> byCategoryId = dishService.getByCategoryId(categoryId);
        return Result.success(byCategoryId);
    }
    @PostMapping("/status/{status}")
    public Result setStatus(@PathVariable Integer status, Long id){
        log.info("set status{},id{}",status,id);
        dishService.setStatus(status,id);
        return Result.success();
    }
    private Long redisCacheDel(String keys){
        Set setKeys = redisTemplate.keys(keys);
        Long delete = redisTemplate.delete(setKeys);
        log.info("---------------------redis delete:{}",delete);
        return delete;
    }
}
