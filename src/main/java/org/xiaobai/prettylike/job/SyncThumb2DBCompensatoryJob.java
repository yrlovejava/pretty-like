package org.xiaobai.prettylike.job;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.xiaobai.prettylike.common.constant.ThumbConstant;
import org.xiaobai.prettylike.utils.RedisKeyUtil;

import java.util.HashSet;
import java.util.Set;

/**
 * 定时将 Redis 中的临时点赞数据同步到数据库的补偿措施
 * 当数据在 Redis 中，由于不可控因素导致没有成功同步到数据库时，通过该任务补偿
 */
@Slf4j
@Component
public class SyncThumb2DBCompensatoryJob {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private SyncThumb2DBJob syncThumb2DBJob;

    @Scheduled(cron = "0 0 2 * * *")
    public void run(){
        log.info("开始执行补偿任务");
        Set<String> thumbKeys = redisTemplate.keys(RedisKeyUtil.getTempThumbKey("") + "*");
        Set<String> needHandleDataSet = new HashSet<>();
        thumbKeys.stream()
                .filter(ObjUtil::isNotNull)
                .forEach(thumbKey -> needHandleDataSet.add(
                        thumbKey.replace(ThumbConstant.TEMP_THUMB_KEY_PREFIX.formatted(""), "")
                ));

        if (CollUtil.isEmpty(needHandleDataSet)) {
            log.info("没有需要处理的数据");
            return;
        }

        // 补偿数据
        for (String dataSet : needHandleDataSet) {
            syncThumb2DBJob.syncThumb2DBByDate(dataSet);
        }

        log.info("补偿任务执行完成");
    }
}
