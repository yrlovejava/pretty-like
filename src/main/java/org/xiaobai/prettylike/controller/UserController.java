package org.xiaobai.prettylike.controller;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.xiaobai.prettylike.common.constant.UserConstant;
import org.xiaobai.prettylike.common.result.BaseResponse;
import org.xiaobai.prettylike.common.result.ResultUtils;
import org.xiaobai.prettylike.model.entity.User;
import org.xiaobai.prettylike.service.UserService;

/**
 * 用户Controller
 */
@RestController
@RequestMapping("/thumb")
public class UserController {

    @Resource
    private UserService userService;

    @GetMapping("/login")
    public BaseResponse<User> login(long userId, HttpServletRequest request) {
        User user = userService.getById(userId);
        request.getSession().setAttribute(UserConstant.LOGIN_USER, user);
        return ResultUtils.success(user);
    }

    @GetMapping("/get/login")
    public BaseResponse<User> getLoginUser(HttpServletRequest request) {
        User loginUser = (User) request.getSession().getAttribute(UserConstant.LOGIN_USER);
        return ResultUtils.success(loginUser);
    }
}
