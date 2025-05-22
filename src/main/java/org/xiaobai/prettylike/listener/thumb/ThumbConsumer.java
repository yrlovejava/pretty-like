package org.xiaobai.prettylike.listener.thumb;

import cn.hutool.core.lang.Pair;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.SubscriptionType;
import org.apache.pulsar.common.schema.SchemaType;
import org.springframework.pulsar.annotation.PulsarListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xiaobai.prettylike.listener.thumb.msg.ThumbEvent;
import org.xiaobai.prettylike.mapper.BlogMapper;
import org.xiaobai.prettylike.model.entity.Thumb;
import org.xiaobai.prettylike.service.ThumbService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * 点赞消费者
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ThumbConsumer {

    private final BlogMapper blogMapper;
    private final ThumbService thumbService;

    /**
     * 监听死信队列
     *
     * @param message 消息
     */
    @PulsarListener(topics = "thumb-dlq-topic")
    public void consumerDlq(Message<ThumbEvent> message) {
        MessageId messageId = message.getMessageId();
        log.info("dlq message = {}", messageId);
        log.info("消息 {} 已入库", messageId);
        log.info("已通知相关人员 {} 处理消息 {}", "坤哥", messageId);
    }

    /**
     * 批量处理消息
     *
     * @param messageList 消息列表
     */
    @PulsarListener(
            subscriptionName = "thumb-subscription",
            topics = "thumb-topic",
            schemaType = SchemaType.JSON,
            batch = true,
            subscriptionType = SubscriptionType.Shared,
            // consumerCustomizer = "thumbConsumerConfig",
            // 引用 NACK 重试策略
            negativeAckRedeliveryBackoff = "negativeAckRedeliveryBackoff",
            // 引用 ACK 超时重试策略
            ackTimeoutRedeliveryBackoff = "ackTimeoutRedeliveryBackoff",
            // 引用死信队列策略
            deadLetterPolicy = "deadLetterPolicy"
    )
    @Transactional(rollbackFor = Exception.class)
    public void processBatch(List<Message<ThumbEvent>> messageList) {
        log.info("ThumbConsumer processBatch: {}", messageList.size());

        Map<Long, Long> countMap = new ConcurrentHashMap<>();
        List<Thumb> thumbList = new ArrayList<>();

        LambdaQueryWrapper<Thumb> wrapper = new LambdaQueryWrapper<>();
        AtomicReference<Boolean> needRemove = new AtomicReference<>(false);

        // 提取事件并过滤无效消息
        List<ThumbEvent> eventList = messageList.stream()
                .map(Message::getValue)
                .filter(Objects::nonNull)
                .toList();

        // 按（userId,blogId）分组，并获取每个分组的最新事件
        Map<Pair<Long, Long>, ThumbEvent> latestEvents = eventList.stream()
                .collect(Collectors.groupingBy(
                        event -> Pair.of(event.getUserId(), event.getBlogId()),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> {
                                    // 按事件升序排序，取最后一个作为最新事件
                                    list.sort(Comparator.comparing(ThumbEvent::getEventTime));
                                    if (list.size() % 2 == 0) {
                                        return null;
                                    }
                                    return list.getLast();
                                }
                        )
                ));

        // 分别对点赞和取消点赞事件进行处理
        latestEvents.forEach((userBlogPair, event) -> {
                    if (event == null) {
                        return;
                    }
                    ThumbEvent.EventType finalAction = event.getType();

                    if (finalAction == ThumbEvent.EventType.INCR) {
                        countMap.merge(event.getBlogId(), 1L, Long::sum);
                        Thumb thumb = new Thumb();
                        thumb.setUserId(event.getUserId());
                        thumb.setBlogId(event.getBlogId());
                        thumbList.add(thumb);
                    } else {
                        needRemove.set(true);
                        wrapper.or().eq(Thumb::getUserId, event.getUserId()).eq(Thumb::getBlogId, event.getBlogId());
                        countMap.merge(event.getBlogId(), -1L, Long::sum);
                    }
                }
        );

        // 批量更新数据库
        if(needRemove.get()){
            thumbService.remove(wrapper);
        }
        batchUpdateBlogs(countMap);
        batchInsertThumbs(thumbList);
    }

    public void batchUpdateBlogs(Map<Long, Long> countMap) {
        if (!countMap.isEmpty()) {
            blogMapper.batchUpdateThumbCount(countMap);
        }
    }

    public void batchInsertThumbs(List<Thumb> thumbs) {
        if (!thumbs.isEmpty()) {
            // 分批次插入
            thumbService.saveBatch(thumbs, 500);
        }
    }
}
