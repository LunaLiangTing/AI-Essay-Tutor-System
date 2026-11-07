package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.TClass;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ClassMapper extends BaseMapper<TClass> {
    // 继承 BaseMapper 后，MyBatis-Plus 会自动帮你生成：
    // insert, deleteById, updateById, selectById, selectList, selectCount 等所有单表方法
}