package org.xiaobai.prettylike.job;

import com.google.common.collect.Sets;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.pulsar.core.PulsarTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.xiaobai.prettylike.common.constant.ThumbConstant;
import org.xiaobai.prettylike.listener.thumb.msg.ThumbEvent;
import org.xiaobai.prettylike.model.entity.Thumb;
import org.xiaobai.prettylike.service.ThumbService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 点赞消息对账定时任务
 */
@Slf4j
@Component
public class ThumbReconcileJob {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private ThumbService thumbService;

    @Resource
    private PulsarTemplate<ThumbEvent> pulsarTemplate;

    /**
     * 定时任务（每天凌晨2点执行）
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void run() {
        long startTime = System.currentTimeMillis();

        // 1.获取该分片下所有用户ID
        Set<Long> userIds = new HashSet<>();
        String pattern = ThumbConstant.USER_THUMB_KEY_PREFIX + "*";
        // 使用游标做全量key扫描的优化，避免一次获取所有key
        try (Cursor<String> cursor = redisTemplate.scan(ScanOptions.scanOptions().match(pattern).count(1000).build())) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                Long userId = Long.valueOf(key.replace(ThumbConstant.USER_THUMB_KEY_PREFIX, ""));
                userIds.add(userId);
            }
        }

        // 2.逐用户比对
        userIds.forEach(userId -> {
            // 从redis中获取用户点赞的所有blogId
            Set<Long> redisBlogIds = redisTemplate.opsForHash().keys(ThumbConstant.USER_THUMB_KEY_PREFIX + userId).stream()
                    .map(each -> Long.valueOf(each.toString()))
                    .collect(Collectors.toSet());

            // 从数据库中查询用户点赞的所有blogId
            List<Thumb> thumbList = thumbService.lambdaQuery()
                    .eq(Thumb::getUserId, userId)
                    .list();
            Set<Long> mysqlBlogIds = Optional.ofNullable(thumbList)
                    .orElse(new ArrayList<>())
                    .stream()
                    .map(Thumb::getBlogId)
                    .collect(Collectors.toSet());

            // 计算差异
            Set<Long> diffBlogIds = Sets.difference(redisBlogIds,mysqlBlogIds);

            // 4.发送补偿事件

        });

        log.info("对账任务完成，耗时：{}ms", System.currentTimeMillis() - startTime);
    }

    /**
     * 发送补偿事件到Pulsar
     * @param userId 用户ID
     * @param blogIds blogId集合
     */
    private void sendCompensationEvents(Long userId,Set<Long> blogIds) {
        blogIds.forEach(blogId -> {
            ThumbEvent thumbEvent = new ThumbEvent(userId, blogId, ThumbEvent.EventType.INCR, LocalDateTime.now());
            try {
                pulsarTemplate.sendAsync("thumb-topic", thumbEvent)
                        .exceptionally(ex -> {
                            log.error("补偿事件发送失败: userId={}, blogId={}", userId, blogId, ex);
                            return null;
                        });
            } catch (Exception e) {
                log.error("补偿事件发送失败: userId={}, blogId={}", userId, blogId, e);
            }
        });
    }
}
