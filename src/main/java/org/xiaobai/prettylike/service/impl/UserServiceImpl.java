package org.xiaobai.prettylike.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.xiaobai.prettylike.common.constant.UserConstant;
import org.xiaobai.prettylike.mapper.UserMapper;
import org.xiaobai.prettylike.model.entity.User;
import org.xiaobai.prettylike.service.UserService;

/**
 * 用户表(User)表服务实现类
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper,User> implements UserService {

    @Override
    public User getLoginUser(HttpServletRequest request) {
        return (User) request.getSession().getAttribute(UserConstant.LOGIN_USER);
    }
}
