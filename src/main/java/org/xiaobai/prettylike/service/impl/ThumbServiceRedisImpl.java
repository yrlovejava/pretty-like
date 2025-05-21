package org.xiaobai.prettylike.service.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.xiaobai.prettylike.common.constant.RedisLuaScriptConstant;
import org.xiaobai.prettylike.common.enums.LuaStatusEnum;
import org.xiaobai.prettylike.mapper.ThumbMapper;
import org.xiaobai.prettylike.model.dto.thumb.DoThumbRequest;
import org.xiaobai.prettylike.model.entity.Thumb;
import org.xiaobai.prettylike.model.entity.User;
import org.xiaobai.prettylike.service.ThumbService;
import org.xiaobai.prettylike.service.UserService;
import org.xiaobai.prettylike.utils.RedisKeyUtil;

import java.util.Arrays;
import java.util.Objects;

/**
 * 点赞 Redis 实现层
 */
@Slf4j
@Service("thumbService")
@RequiredArgsConstructor
public class ThumbServiceRedisImpl extends ServiceImpl<ThumbMapper, Thumb> implements ThumbService {

    private final UserService userService;

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 点赞 认为两部分，点赞数和点赞记录，点赞数按照时间分片（hash结构），点赞记录是每个用户一个hash
     * 通过 LUA 脚本，对点赞数增加，插入点赞记录（会先判断是否已经点赞）
     * @param doThumbRequest 点赞请求
     * @param request http请求
     * @return 是否成功
     */
    @Override
    public Boolean doThumb(DoThumbRequest doThumbRequest, HttpServletRequest request) {
        if (doThumbRequest == null || doThumbRequest.getBlogId() == null) {
            throw new RuntimeException("参数错误");
        }
        User loginUser = userService.getLoginUser(request);
        Long blogId = doThumbRequest.getBlogId();

        String timeSlice = getTimeSlice();
        // Redis Key
        String tempThumbKey = RedisKeyUtil.getTempThumbKey(timeSlice);
        String userThumbKey = RedisKeyUtil.getUserThumbKey(loginUser.getId());

        // 执行 Lua 脚本
        // TODO 点赞记录会有大 Key 问题
        Long result = redisTemplate.execute(
                RedisLuaScriptConstant.THUMB_SCRIPT,
                Arrays.asList(tempThumbKey, userThumbKey),
                loginUser.getId(),
                blogId
        );
        if(Objects.isNull(result)){
            throw new RuntimeException("用户点赞出错");
        }

        if (LuaStatusEnum.FAIL.getValue() == result) {
            throw new RuntimeException("用户已点赞");
        }

        // 更新成功才执行
        return LuaStatusEnum.SUCCESS.getValue() == result;
    }

    @Override
    public Boolean undoThumb(DoThumbRequest doThumbRequest, HttpServletRequest request) {
        if (doThumbRequest == null || doThumbRequest.getBlogId() == null) {
            throw new RuntimeException("参数错误");
        }
        User loginUser = userService.getLoginUser(request);

        Long blogId = doThumbRequest.getBlogId();
        // 计算时间片
        String timeSlice = getTimeSlice();
        // Redis Key
        String tempThumbKey = RedisKeyUtil.getTempThumbKey(timeSlice);
        String userThumbKey = RedisKeyUtil.getUserThumbKey(loginUser.getId());

        // 执行 Lua 脚本
        Long result = redisTemplate.execute(
                RedisLuaScriptConstant.UNTHUMB_SCRIPT,
                Arrays.asList(tempThumbKey, userThumbKey),
                loginUser.getId(),
                blogId
        );
        if(!Objects.isNull(result)){
            throw new RuntimeException("用户取消点赞失败");
        }


        // 根据返回值处理结果
        if (result == LuaStatusEnum.FAIL.getValue()) {
            throw new RuntimeException("用户未点赞");
        }
        return LuaStatusEnum.SUCCESS.getValue() == result;
    }

    private String getTimeSlice() {
        DateTime nowDate = DateUtil.date();
        // 获取到当前时间前最近的整数秒，比如当前 11:20:23 ，获取到 11:20:20
        return DateUtil.format(nowDate, "HH:mm:") + (DateUtil.second(nowDate) / 10) * 10;
    }

    @Override
    public Boolean hasThumb(Long blogId, Long userId) {
        return redisTemplate.opsForHash()
                .hasKey(RedisKeyUtil.getUserThumbKey(userId), blogId.toString());
    }
}
