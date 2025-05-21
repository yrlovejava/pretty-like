package org.xiaobai.prettylike.service;

import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletRequest;
import org.xiaobai.prettylike.model.entity.Blog;
import org.xiaobai.prettylike.model.entity.User;
import org.xiaobai.prettylike.model.vo.BlogVO;

import java.util.List;

/**
 * 内容表 Service 接口
 */
public interface BlogService extends IService<Blog> {

    BlogVO getBlogVOById(long blogId, HttpServletRequest request);

    BlogVO getBlogVO(Blog blog, User loginUser);

    List<BlogVO> getBlogVOList(List<Blog> blogList, HttpServletRequest request);
}
