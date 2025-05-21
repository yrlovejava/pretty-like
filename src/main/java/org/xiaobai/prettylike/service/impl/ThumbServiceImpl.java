package org.xiaobai.prettylike.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.xiaobai.prettylike.mapper.ThumbMapper;
import org.xiaobai.prettylike.model.entity.Thumb;
import org.xiaobai.prettylike.service.ThumbService;

/**
 * 点赞表 Service 实现类
 */
@Service
public class ThumbServiceImpl extends ServiceImpl<ThumbMapper, Thumb> implements ThumbService {
}
