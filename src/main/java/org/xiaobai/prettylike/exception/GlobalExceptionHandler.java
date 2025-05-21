package org.xiaobai.prettylike.exception;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.xiaobai.prettylike.common.result.BaseResponse;
import org.xiaobai.prettylike.common.result.ErrorCode;
import org.xiaobai.prettylike.common.result.ResultUtils;

/**
 * 全局异常处理器
 */
@Slf4j
@Hidden
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e) {
        log.error(e.getMessage(), e);
        return ResultUtils.error(ErrorCode.OPERATION_ERROR, e.getMessage());
    }
}
