package org.xiaobai.prettylike.listener.thumb.msg;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 点赞事件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThumbEvent {

    private Long userId;
    private Long blogId;
    // INCR/DECR
    private EventType type;
    private LocalDateTime eventTime;

    public enum EventType {
        INCR,
        DECR
    }
}
