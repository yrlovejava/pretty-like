package org.xiaobai.prettylike.service;

import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletRequest;
import org.xiaobai.prettylike.model.entity.User;

/**
 * 用户表 Service 接口
 */
public interface UserService extends IService<User> {
    User getLoginUser(HttpServletRequest request);
}
