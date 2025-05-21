package org.xiaobai.prettylike.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.xiaobai.prettylike.mapper.BlogMapper;
import org.xiaobai.prettylike.model.entity.Blog;
import org.xiaobai.prettylike.service.BlogService;

/**
 * 内容表 Service 实现类
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements BlogService {
}
