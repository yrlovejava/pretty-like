package org.xiaobai.prettylike.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.PulsarClientException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.pulsar.core.PulsarTemplate;
import org.springframework.stereotype.Service;
import org.xiaobai.prettylike.common.constant.RedisLuaScriptConstant;
import org.xiaobai.prettylike.common.enums.LuaStatusEnum;
import org.xiaobai.prettylike.listener.thumb.msg.ThumbEvent;
import org.xiaobai.prettylike.mapper.ThumbMapper;
import org.xiaobai.prettylike.model.dto.thumb.DoThumbRequest;
import org.xiaobai.prettylike.model.entity.Thumb;
import org.xiaobai.prettylike.model.entity.User;
import org.xiaobai.prettylike.service.ThumbService;
import org.xiaobai.prettylike.service.UserService;
import org.xiaobai.prettylike.utils.RedisKeyUtil;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 点赞表 Service 实现类 (基于MQ实现)
 */
@Slf4j
@Service("thumbService")
@RequiredArgsConstructor
public class ThumbServiceMQImpl extends ServiceImpl<ThumbMapper, Thumb>
        implements ThumbService {

    private final UserService userService;

    private final RedisTemplate<String, Object> redisTemplate;

    private final PulsarTemplate<ThumbEvent> pulsarTemplate;

    @Override
    public Boolean doThumb(DoThumbRequest doThumbRequest, HttpServletRequest request) {
        if (doThumbRequest == null || doThumbRequest.getBlogId() == null) {
            throw new RuntimeException("参数错误");
        }
        User loginUser = userService.getLoginUser(request);
        Long loginUserId = loginUser.getId();
        Long blogId = doThumbRequest.getBlogId();
        String userThumbKey = RedisKeyUtil.getUserThumbKey(loginUserId);
        // 执行 Lua 脚本，点赞存入 Redis
        Long result = redisTemplate.execute(
                RedisLuaScriptConstant.THUMB_SCRIPT_MQ,
                List.of(userThumbKey),
                blogId
        );
        if(result == null){
            log.error("执行点赞Lua脚本失败");
            return false;
        }
        if(LuaStatusEnum.FAIL.getValue() == result){
            throw new RuntimeException("用户已点赞");
        }

        ThumbEvent thumbEvent = ThumbEvent.builder()
                .blogId(blogId)
                .userId(loginUserId)
                .type(ThumbEvent.EventType.INCR)
                .eventTime(LocalDateTime.now())
                .build();

        try {
            pulsarTemplate.sendAsync("thumb-topic", thumbEvent).exceptionally(ex -> {
                redisTemplate.opsForHash().delete(userThumbKey, blogId.toString(), true);
                log.error("点赞事件发送失败: userId={}, blogId={}", loginUserId, blogId, ex);
                return null;
            });
        } catch (PulsarClientException e) {
            throw new RuntimeException("点赞消息发送失败");
        }

        return true;
    }

    @Override
    public Boolean undoThumb(DoThumbRequest doThumbRequest, HttpServletRequest request) {
        if (doThumbRequest == null || doThumbRequest.getBlogId() == null) {
            throw new RuntimeException("参数错误");
        }
        User loginUser = userService.getLoginUser(request);
        Long loginUserId = loginUser.getId();
        Long blogId = doThumbRequest.getBlogId();
        String userThumbKey = RedisKeyUtil.getUserThumbKey(loginUserId);
        // 执行 Lua 脚本，点赞记录从 Redis 删除
        Long result = redisTemplate.execute(
                RedisLuaScriptConstant.UNTHUMB_SCRIPT_MQ,
                List.of(userThumbKey),
                blogId
        );
        if(result == null){
            log.error("执行取消点赞Lua脚本失败");
            return false;
        }

        if (LuaStatusEnum.FAIL.getValue() == result) {
            throw new RuntimeException("用户未点赞");
        }

        ThumbEvent thumbEvent = ThumbEvent.builder()
                .blogId(blogId)
                .userId(loginUserId)
                .type(ThumbEvent.EventType.DECR)
                .eventTime(LocalDateTime.now())
                .build();
        try {
            pulsarTemplate.sendAsync("thumb-topic", thumbEvent).exceptionally(ex -> {
                redisTemplate.opsForHash().put(userThumbKey, blogId.toString(), true);
                log.error("取消点赞事件发送失败: userId={}, blogId={}", loginUserId, blogId, ex);
                return null;
            });
        } catch (PulsarClientException e) {
            throw new RuntimeException("取消点赞事件发送失败");
        }

        return true;
    }

    @Override
    public Boolean hasThumb(Long blogId, Long userId) {
        return redisTemplate.opsForHash().hasKey(RedisKeyUtil.getUserThumbKey(userId), blogId.toString());
    }
}
