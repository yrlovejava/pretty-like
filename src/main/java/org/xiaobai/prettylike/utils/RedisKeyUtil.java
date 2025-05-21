package org.xiaobai.prettylike.utils;

import org.xiaobai.prettylike.common.constant.ThumbConstant;

/**
 * Redis Key 工具类
 */
public final class RedisKeyUtil {

    public static String getUserThumbKey(Long userId) {
        return ThumbConstant.USER_THUMB_KEY_PREFIX + userId;
    }

    /**
     * 获取 临时点赞记录 key
     */
    public static String getTempThumbKey(String time) {
        return ThumbConstant.TEMP_THUMB_KEY_PREFIX.formatted(time);
    }
}
