package org.xiaobai.prettylike.controller;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.xiaobai.prettylike.common.result.BaseResponse;
import org.xiaobai.prettylike.common.result.ResultUtils;
import org.xiaobai.prettylike.model.dto.thumb.DoThumbRequest;
import org.xiaobai.prettylike.service.ThumbService;

/**
 * 点赞Controller
 */
@RestController
@RequestMapping("/thumb")
@Tag(name = "点赞控制层")
public class ThumbController {

    @Resource
    private ThumbService thumbService;

    private final Counter successCounter;
    private final Counter failureCounter;

    public ThumbController(MeterRegistry registry) {
        this.successCounter = Counter.builder("thumb.success.count")
                .description("Total successful thumb")
                .register(registry);
        this.failureCounter = Counter.builder("thumb.failure.count")
                .description("Total failed thumb")
                .register(registry);
    }

    @PostMapping("/do")
    @Operation(description = "点赞")
    public BaseResponse<Boolean> doThumb(@RequestBody DoThumbRequest doThumbRequest, HttpServletRequest request) {
        Boolean success;
        try {
            success = thumbService.doThumb(doThumbRequest, request);
            if (success) {
                successCounter.increment();
            } else {
                failureCounter.increment();
            }
        } catch (Exception e) {
            failureCounter.increment();
            throw e;
        }
        return ResultUtils.success(success);
    }

    @PostMapping("/undo")
    @Operation(description = "取消点赞")
    public BaseResponse<Boolean> undoThumb(@RequestBody DoThumbRequest doThumbRequest, HttpServletRequest request) {
        Boolean success = thumbService.undoThumb(doThumbRequest, request);
        return ResultUtils.success(success);
    }
}
