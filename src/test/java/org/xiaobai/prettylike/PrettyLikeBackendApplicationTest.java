package org.xiaobai.prettylike;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.xiaobai.prettylike.service.BlogService;
import org.xiaobai.prettylike.service.ThumbService;
import org.xiaobai.prettylike.service.UserService;

@SpringBootTest
public class PrettyLikeBackendApplicationTest {

    @Resource
    private ThumbService thumbService;

    @Resource
    private UserService userService;

    @Resource
    private BlogService blogService;

    @Test
    void contextLoads() {
        System.out.println(thumbService.list());
        System.out.println(userService.list());
        System.out.println(blogService.list());
    }
}
