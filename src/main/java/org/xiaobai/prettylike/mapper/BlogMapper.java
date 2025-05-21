package org.xiaobai.prettylike.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.xiaobai.prettylike.model.entity.Blog;

import java.util.Map;

/**
 * 内容 Mapper
 */
public interface BlogMapper extends BaseMapper<Blog> {

    void batchUpdateThumbCount(@Param("countMap") Map<Long, Long> countMap);
}
