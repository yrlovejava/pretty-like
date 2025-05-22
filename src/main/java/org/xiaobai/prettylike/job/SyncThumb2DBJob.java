package org.xiaobai.prettylike.job;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.text.StrPool;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.xiaobai.prettylike.common.enums.ThumbTypeEnum;
import org.xiaobai.prettylike.mapper.BlogMapper;
import org.xiaobai.prettylike.model.entity.Thumb;
import org.xiaobai.prettylike.service.ThumbService;
import org.xiaobai.prettylike.utils.RedisKeyUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * 定时将 Redis 中的临时点赞数据同步到数据库
 */
@Slf4j
//@Component
public class SyncThumb2DBJob {

    @Resource
    private ThumbService thumbService;

    @Resource
    private BlogMapper blogMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Scheduled(initialDelay = 10000, fixedDelay = 10000)// 首次延迟10s，间隔10s
    @Transactional(rollbackFor = Exception.class)
    public void run() {
        log.info("开始执行");
        DateTime nowDate = DateUtil.date();
        String date = DateUtil.format(nowDate, "HH:mm:") + (DateUtil.second(nowDate) / 10 - 1) * 10;
        syncThumb2DBByDate(date);
        log.info("临时数据同步完成");
    }

    /**
     * 根据传入时间片将临时点赞数据和取消点赞数据同步到数据库
     * @param date 日期
     */
    public void syncThumb2DBByDate(String date) {
        // 获取到临时点赞和取消点赞数据
        // todo 如果数据量过大，可以分批读取数据
        String tempThumbKey = RedisKeyUtil.getTempThumbKey(date);
        // 这里获取到了临时点赞数(hash结构)
        Map<Object, Object> allTempThumbMap = redisTemplate.opsForHash().entries(tempThumbKey);
        boolean thumbMapEmpty = CollUtil.isEmpty(allTempThumbMap);
        if (thumbMapEmpty) {
            // 如果为空直接返回
            return;
        }

        // 同步点赞到数据库
        Map<Long, Long> blogThumbCountMap = new HashMap<>();
        ArrayList<Thumb> thumbList = new ArrayList<>();
        LambdaQueryWrapper<Thumb> wrapper = new LambdaQueryWrapper<>();
        boolean needRemove = false;
        // 遍历所有点赞记录
        for (Object userIdBlogIdObj : allTempThumbMap.keySet()) {
            // key 为 {userId}:{BlogId} value 为 0:点赞 -1:取消点赞
            String userIdBlogId = (String) userIdBlogIdObj;
            String[] userIdAndBlogId = userIdBlogId.split(StrPool.COLON);
            Long userId = Long.valueOf(userIdAndBlogId[0]);
            Long blogId = Long.valueOf(userIdAndBlogId[1]);
            // -1 取消点赞，1 点赞
            Integer thumbType = Integer.valueOf(allTempThumbMap.get(userIdBlogId).toString());
            if (thumbType == ThumbTypeEnum.INCR.getValue()) {
                Thumb thumb = new Thumb();
                thumb.setUserId(userId);
                thumb.setBlogId(blogId);
                thumbList.add(thumb);
            } else if (thumbType == ThumbTypeEnum.DECR.getValue()) {
                // 拼接查询条件，批量删除
                // todo 数据量过大，可以分批操作
                needRemove = true;
                // or 会拼接条件
                wrapper.or().eq(Thumb::getUserId, userId).eq(Thumb::getBlogId, blogId);
            } else {
                if (thumbType != ThumbTypeEnum.NON.getValue()) {
                    log.warn("数据异常：{}", userId + "," + blogId + "," + thumbType);
                }
                continue;
            }
            // 计算点赞增量
            blogThumbCountMap.put(blogId, blogThumbCountMap.getOrDefault(blogId, 0L) + thumbType);
        }

        // 批量插入
        thumbService.saveBatch(thumbList);
        // 批量删除
        if(needRemove){
            thumbService.remove(wrapper);
        }

        // 批量更新博客点赞量
        if(!blogThumbCountMap.isEmpty()){
            blogMapper.batchUpdateThumbCount(blogThumbCountMap);
        }

        // 异步删除 Redis 中的临时数据
        Thread.startVirtualThread(() -> {
            redisTemplate.delete(tempThumbKey);
        });
    }

}
