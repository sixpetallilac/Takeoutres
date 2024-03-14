package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {
    /**
     * openid查询用户
     * @param openid
     * @return
     */
    @Select("select * from sky_take_out.user where id = #{openid}")
    User getByOpenId(String openid);

    void insert(User user);

}
