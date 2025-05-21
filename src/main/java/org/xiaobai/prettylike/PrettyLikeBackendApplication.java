package org.xiaobai.prettylike;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 启动类
 */
@SpringBootApplication
@MapperScan("org.xiaobai.prettylike.mapper")
public class PrettyLikeBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(PrettyLikeBackendApplication.class, args);
    }
}
