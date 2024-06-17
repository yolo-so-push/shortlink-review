package com.guolihong.shortlink.project.mq.consumer;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.Week;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.guolihong.shortlink.project.common.convention.exception.ServiceException;
import com.guolihong.shortlink.project.dao.entity.*;
import com.guolihong.shortlink.project.dao.mapper.*;
import com.guolihong.shortlink.project.dto.biz.ShortLinkStatsRecordDTO;
import com.guolihong.shortlink.project.mq.idempotent.MessageQueueIdempotentHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.guolihong.shortlink.project.common.constant.RedisKeyConstant.LOCK_GID_UPDATE_KEY;
import static com.guolihong.shortlink.project.common.constant.ShortLinkConstant.AMAP_REMOTE_URL;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShortLinkStatsConsumer implements StreamListener<String, MapRecord<String, String, String>> {
    private final StringRedisTemplate stringRedisTemplate;
    private final MessageQueueIdempotentHandler messageQueueIdempotentHandler;
    private final RedissonClient redissonClient;
    private final ShortLinkGotoMapper shortLinkGotoMapper;
    private final ShortLinkMapper shortLinkMapper;
    private final LinkAccessStatsMapper linkAccessStatsMapper;
    @Value("${short-link.stats.amqp-key}")
    private String amqpKey;
    private final LinkLocaleStatsMapper linkLocaleStatsMapper;
    private final LinkOsStatsMapper linkOsStatsMapper;
    private final LinkBrowserStatsMapper linkBrowserStatsMapper;
    private final LinkAccessLogsMapper linkAccessLogsMapper;
    private final LinkDeviceStatsMapper linkDeviceStatsMapper;
    private final LinkNetworkStatsMapper linkNetworkStatsMapper;
    private final LinkStatsTodayMapper linkStatsTodayMapper;


    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        String stream = message.getStream();
        RecordId id = message.getId();
        //首先判断消息是否已经被消费过了
        if (!messageQueueIdempotentHandler.isMessageProcessed(id.toString())){
            //判断消费流程是否执行完毕
            if (messageQueueIdempotentHandler.isAccomplish(id.toString())){
                //执行完毕
                return;
            }
            throw new ServiceException("消息消费未完成，需要消息队列重试");
        }
        try {
            Map<String, String> produceMap = message.getValue();
            ShortLinkStatsRecordDTO record = JSON.parseObject(produceMap.get("statsRecord"), ShortLinkStatsRecordDTO.class);
            actualSaveShortLinkStats(record);
            //从消息队列中删除消息
            stringRedisTemplate.opsForStream().delete(Objects.requireNonNull(stream),id.getValue());
        }catch (Throwable ex){
            //消息流程未执行完成,删除消息标识
            messageQueueIdempotentHandler.deleteMessageProcessed(id.toString());
            log.error("记录短链接监控消费异常",ex);
            throw ex;
        }
        //消息消费流程执行结束
        messageQueueIdempotentHandler.setAccomplish(id.toString());
    }

    /**
     * 保存监控数据到数据库
     * @param record
     */
    private void actualSaveShortLinkStats(ShortLinkStatsRecordDTO record) {
        String fullShortUrl = record.getFullShortUrl();
        //通过读写锁进行监控记录插入数据库
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(String.format(LOCK_GID_UPDATE_KEY, fullShortUrl));
        RLock rLock = readWriteLock.readLock();
        rLock.lock();
        try {
            LambdaQueryWrapper<ShortLinkGotoDO> wr = Wrappers.lambdaQuery(ShortLinkGotoDO.class).eq(ShortLinkGotoDO::getFullShortUrl, fullShortUrl);
            ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(wr);
            String gid = shortLinkGotoDO.getGid();
            Week week = DateUtil.dayOfWeekEnum(new Date());
            int hour = DateUtil.hour(new Date(), true);
            int weekValue = week.getIso8601Value();
            LinkAccessStatsDO accessStatsDO = LinkAccessStatsDO.builder()
                    .pv(1)
                    .uv(record.getUvFirstFlag()?1:0)
                    .uip(record.getUipFirstFlag()?1:0)
                    .hour(hour)
                    .weekday(weekValue)
                    .fullShortUrl(fullShortUrl)
                    .date(new Date())
                    .build();
            linkAccessStatsMapper.shortLinkStats(accessStatsDO);
            Map<String,Object> localeParamMap=new HashMap<>();
            localeParamMap.put("key",amqpKey);
            localeParamMap.put("ip",record.getRemoteAddr());
            //通过发送请求获取当前ip所在地区信息
            String localeResultStr = HttpUtil.get(AMAP_REMOTE_URL, localeParamMap);
            JSONObject localeResultObj = JSON.parseObject(localeResultStr);
            String infoCode = localeResultObj.getString("infocode");
            String actualProvince="未知";
            String actualCity="未知";
            if (StrUtil.isNotBlank(infoCode)&&Objects.equals(infoCode,"10000")){
                String province = localeResultObj.getString("province");
                boolean unFlag = StrUtil.equals(province, "[]");
                LinkLocaleStatsDO localeStatsDO = LinkLocaleStatsDO.builder()
                        .province(unFlag?actualProvince:province)
                        .city(unFlag?actualCity:localeResultObj.getString("city"))
                        .adcode(unFlag?"未知":localeResultObj.getString("adcode"))
                        .cnt(1)
                        .fullShortUrl(fullShortUrl)
                        .country("中国")
                        .date(new Date())
                        .build();
                linkLocaleStatsMapper.shortLinkLocaleState(localeStatsDO);

            }
            LinkOsStatsDO linkOsStatsDO = LinkOsStatsDO.builder()
                    .os(record.getOs())
                    .cnt(1)
                    .fullShortUrl(fullShortUrl)
                    .date(new Date())
                    .build();
            linkOsStatsMapper.shortLinkOsState(linkOsStatsDO);
            LinkDeviceStatsDO deviceStatsDO = LinkDeviceStatsDO.builder()
                    .device(record.getDevice())
                    .cnt(1)
                    .fullShortUrl(fullShortUrl)
                    .date(new Date())
                    .build();
            linkDeviceStatsMapper.shortLinkDeviceState(deviceStatsDO);
            LinkBrowserStatsDO linkBrowserStatsDO = LinkBrowserStatsDO.builder()
                    .browser(record.getBrowser())
                    .cnt(1)
                    .fullShortUrl(fullShortUrl)
                    .date(new Date())
                    .build();
            linkBrowserStatsMapper.shortLinkBrowserState(linkBrowserStatsDO);
            LinkNetworkStatsDO linkNetworkStatsDO = LinkNetworkStatsDO.builder()
                    .network(record.getNetwork())
                    .cnt(1)
                    .fullShortUrl(fullShortUrl)
                    .date(new Date())
                    .build();
            linkNetworkStatsMapper.shortLinkNetworkState(linkNetworkStatsDO);
            LinkAccessLogsDO linkAccessLogsDO = LinkAccessLogsDO.builder()
                    .ip(record.getRemoteAddr())
                    .network(record.getNetwork())
                    .browser(record.getBrowser())
                    .fullShortUrl(fullShortUrl)
                    .user(record.getUv())
                    .os(record.getOs())
                    .device(record.getDevice())
                    .locale(StrUtil.join("-","中国",actualProvince,actualCity))
                    .build();
            linkAccessLogsMapper.insert(linkAccessLogsDO);
            shortLinkMapper.incrementStats(gid, fullShortUrl, 1, record.getUvFirstFlag() ? 1 : 0, record.getUipFirstFlag() ? 1 : 0);
            LinkStatsTodayDO linkStatsTodayDO = LinkStatsTodayDO.builder()
                    .todayPv(1)
                    .fullShortUrl(fullShortUrl)
                    .todayUip(record.getUipFirstFlag()?1:0)
                    .todayUv(record.getUvFirstFlag()?1:0)
                    .date(new Date())
                    .build();
            linkStatsTodayMapper.shortLinkTodayState(linkStatsTodayDO);
        }catch (Throwable ex){
            log.error("短链接访问量统计异常",ex);
        }finally {
            rLock.unlock();
        }
    }
}
