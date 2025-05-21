package org.xiaobai.prettylike.controller;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.xiaobai.prettylike.common.result.BaseResponse;
import org.xiaobai.prettylike.common.result.ResultUtils;
import org.xiaobai.prettylike.model.entity.Blog;
import org.xiaobai.prettylike.model.vo.BlogVO;
import org.xiaobai.prettylike.service.BlogService;

import java.util.List;

/**
 * 内容Controller
 */
@RestController
@RequestMapping("/blog")
public class BlogController {

    @Resource
    private BlogService blogService;

    @GetMapping("/get")
    public BaseResponse<BlogVO> get(long blogId, HttpServletRequest request) {
        BlogVO blogVO = blogService.getBlogVOById(blogId, request);
        return ResultUtils.success(blogVO);
    }

    @GetMapping("/list")
    public BaseResponse<List<BlogVO>> list(HttpServletRequest request) {
        List<Blog> blogList = blogService.list();
        List<BlogVO> blogVOList = blogService.getBlogVOList(blogList, request);
        return ResultUtils.success(blogVOList);
    }
}
