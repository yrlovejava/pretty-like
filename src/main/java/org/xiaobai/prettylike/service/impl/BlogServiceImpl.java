package org.xiaobai.prettylike.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.xiaobai.prettylike.common.constant.ThumbConstant;
import org.xiaobai.prettylike.mapper.BlogMapper;
import org.xiaobai.prettylike.model.entity.Blog;
import org.xiaobai.prettylike.model.entity.User;
import org.xiaobai.prettylike.model.vo.BlogVO;
import org.xiaobai.prettylike.service.BlogService;
import org.xiaobai.prettylike.service.ThumbService;
import org.xiaobai.prettylike.service.UserService;
import org.xiaobai.prettylike.utils.RedisKeyUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 内容表 Service 实现类
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements BlogService {

    @Resource
    private UserService userService;

    @Resource
    @Lazy
    private ThumbService thumbService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public BlogVO getBlogVOById(long blogId, HttpServletRequest request) {
        Blog blog = this.getById(blogId);
        User loginUser = userService.getLoginUser(request);
        return this.getBlogVO(blog, loginUser);
    }

    @Override
    public BlogVO getBlogVO(Blog blog, User loginUser) {
        BlogVO blogVO = new BlogVO();
        BeanUtil.copyProperties(blog, blogVO);

        Boolean exist = thumbService.hasThumb(blog.getId(), loginUser.getId());
        blogVO.setHasThumb(exist);

        return blogVO;
    }

    @Override
    public List<BlogVO> getBlogVOList(List<Blog> blogList, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Map<Long, Boolean> blogIdHasThumbMap = new HashMap<>();
        if (ObjUtil.isNotEmpty(loginUser)) {
            List<Object> blogIdList = blogList.stream().map(blog -> blog.getId().toString()).collect(Collectors.toList());

            // 获取点赞
            List<Object> thumbList = redisTemplate.opsForHash()
                    .multiGet(RedisKeyUtil.getUserThumbKey(loginUser.getId()), blogIdList);
            for (int i = 0; i < thumbList.size(); i++) {
                if (thumbList.get(i) == null) {
                    continue;
                }
                blogIdHasThumbMap.put(Long.valueOf(blogIdList.get(i).toString()), true);
            }
        }
        return blogList.stream()
                .map(blog -> {
                    BlogVO blogVO = new BlogVO();
                    BeanUtil.copyProperties(blog, blogVO);
                    blogVO.setHasThumb(blogIdHasThumbMap.get(blog.getId()));
                    return blogVO;
                })
                .toList();
    }
}
