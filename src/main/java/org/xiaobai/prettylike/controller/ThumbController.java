package org.xiaobai.prettylike.controller;

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
public class ThumbController {

    @Resource
    private ThumbService thumbService;

    @PostMapping("/do")
    public BaseResponse<Boolean> doThumb(@RequestBody DoThumbRequest doThumbRequest, HttpServletRequest request) {
        Boolean success = thumbService.doThumb(doThumbRequest, request);
        return ResultUtils.success(success);
    }

    @PostMapping("/undo")
    public BaseResponse<Boolean> undoThumb(@RequestBody DoThumbRequest doThumbRequest, HttpServletRequest request) {
        Boolean success = thumbService.undoThumb(doThumbRequest, request);
        return ResultUtils.success(success);
    }
}
