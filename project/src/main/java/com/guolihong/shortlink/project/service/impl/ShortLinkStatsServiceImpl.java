package com.guolihong.shortlink.project.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.guolihong.shortlink.project.dao.entity.*;
import com.guolihong.shortlink.project.dao.mapper.*;
import com.guolihong.shortlink.project.dto.req.ShortLinkGroupStatsAccessRecordReqDTO;
import com.guolihong.shortlink.project.dto.req.ShortLinkGroupStatsReqDTO;
import com.guolihong.shortlink.project.dto.req.ShortLinkStatsAccessRecordReqDTO;
import com.guolihong.shortlink.project.dto.req.ShortLinkStatsReqDTO;
import com.guolihong.shortlink.project.dto.resp.*;
import com.guolihong.shortlink.project.service.ShortLinkStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class ShortLinkStatsServiceImpl implements ShortLinkStatsService {
    private final LinkAccessStatsMapper linkAccessStatsMapper;
    private final LinkAccessLogsMapper linkAccessLogsMapper;
    private final LinkLocaleStatsMapper linkLocaleStatsMapper;
    private final LinkBrowserStatsMapper linkBrowserStatsMapper;
    private final LinkOsStatsMapper linkOsStatsMapper;
    private final LinkDeviceStatsMapper linkDeviceStatsMapper;
    private final LinkNetworkStatsMapper linkNetworkStatsMapper;
    @Override
    public ShortLinkStatsRespDTO oneShortLinkStats(ShortLinkStatsReqDTO requestParam) {
        //短链接基础监控数据 t_link_access_stats
        List<LinkAccessStatsDO> linkAccessStatsDOS = linkAccessStatsMapper.listStatsByShortLink(requestParam);
        if (CollUtil.isEmpty(linkAccessStatsDOS)){
            return null;
        }
        //log表每访问一次产生一条记录（查询当前短链接指定时间的所有访问数据集合）
        ShortLinkStatsRespDTO pvUvUidStatsByShortLink=linkAccessLogsMapper.findPvUvUidStatsByShortLink(requestParam);
        //基础访问详情(每日访问详情pv,uv,uip)
        List<ShortLinkStatsAccessDailyRespDTO> daily=new ArrayList<>();
        List<String> rangeDate = DateUtil.rangeToList(DateUtil.parse(requestParam.getStartDate()), DateUtil.parse(requestParam.getStartDate()), DateField.DAY_OF_MONTH)
                .stream()
                .map(DateUtil::formatDate)
                .toList();
        rangeDate.forEach(e->linkAccessStatsDOS.stream().filter(item-> Objects.equals(e,item.getDate()))
                .findFirst().ifPresentOrElse(item->{
                    ShortLinkStatsAccessDailyRespDTO dailyRespDTO = ShortLinkStatsAccessDailyRespDTO.builder()
                            .date(e)
                            .pv(item.getPv())
                            .uip(item.getUip())
                            .uv(item.getUv())
                            .build();
                    daily.add(dailyRespDTO);
                },()->{
                    ShortLinkStatsAccessDailyRespDTO dailyRespDTO = ShortLinkStatsAccessDailyRespDTO.builder()
                            .date(e)
                            .pv(0)
                            .uip(0)
                            .uv(0)
                            .build();
                    daily.add(dailyRespDTO);
                }));
        //地区访问详情
        List<ShortLinkStatsLocaleCNRespDTO> localeCNRespDTOS=new ArrayList<>();
        List<LinkLocaleStatsDO> linkLocaleStatsDOS = linkLocaleStatsMapper.listLocaleByShortLink(requestParam);
        //当前短链接在全国的访问次数
        int sum = linkLocaleStatsDOS.stream().mapToInt(LinkLocaleStatsDO::getCnt).sum();
        linkLocaleStatsDOS.forEach(e->{
            double ration = (double) e.getCnt() / sum;
            double actualRation=Math.round(ration*100.0)/100.0;
            ShortLinkStatsLocaleCNRespDTO localeCNRespDTO = ShortLinkStatsLocaleCNRespDTO.builder()
                    .cnt(e.getCnt())
                    .ratio(actualRation)
                    .locale(e.getProvince())
                    .build();
            localeCNRespDTOS.add(localeCNRespDTO);
        });
        //小时访问详情(查询当前短链接都在哪个时间段访问较多)
        List<Integer> hourStats=new ArrayList<>();
        List<LinkAccessStatsDO> listHourStatsByShortLink = linkAccessStatsMapper.listHourStatsByShortLink(requestParam);
        for (int i = 0; i < 24; i++) {
            AtomicInteger hour = new AtomicInteger(i);
            int hourCnt = listHourStatsByShortLink.stream()
                    .filter(e -> Objects.equals(e.getHour(), hour.get()))
                    .findFirst()
                    .map(LinkAccessStatsDO::getPv)
                    .orElse(0);
            hourStats.add(hourCnt);
        }
        //高频访问ip
        List<ShortLinkStatsTopIpRespDTO> topIpRespDTOS=new ArrayList<>();
        List<HashMap<String, Object>> topIpByShortLink = linkAccessLogsMapper.listTopIpByShortLink(requestParam);
        topIpByShortLink.forEach(e->{
            ShortLinkStatsTopIpRespDTO topIpRespDTO = ShortLinkStatsTopIpRespDTO.builder()
                    .ip(e.get("ip").toString())
                    .cnt(Integer.valueOf(e.get("count").toString()))
                    .build();
            topIpRespDTOS.add(topIpRespDTO);
        });
        //一周访问详情
        List<Integer> weekdayStats=new ArrayList<>();
        List<LinkAccessStatsDO> weekdayStatsByShortLink = linkAccessStatsMapper.listWeekdayStatsByShortLink(requestParam);
        for (int i = 1; i < 8; i++) {
            AtomicInteger weekday=new AtomicInteger(i);
            int weekdayCnt = weekdayStatsByShortLink.stream()
                    .filter(e -> Objects.equals(e.getWeekday(), weekday))
                    .findFirst()
                    .map(LinkAccessStatsDO::getPv)
                    .orElse(0);
            weekdayStats.add(weekdayCnt);
        }
        //浏览器访问详情
        List<ShortLinkStatsBrowserRespDTO> browserRespDTOS=new ArrayList<>();
        List<HashMap<String, Object>> browserStatsByShortLink = linkBrowserStatsMapper.listBrowserStatsByShortLink(requestParam);
        int browserSum = browserStatsByShortLink.stream().mapToInt(e -> Integer.valueOf(e.get("count").toString()))
                .sum();
        browserStatsByShortLink.forEach(e->{
            double ration = (double) Integer.valueOf(e.get("count").toString()) / browserSum;
            double actualRation=Math.round(ration*100.0)/100.0;
            ShortLinkStatsBrowserRespDTO browserRespDTO = ShortLinkStatsBrowserRespDTO.builder()
                    .cnt(Integer.parseInt(e.get("count").toString()))
                    .browser(e.get("browser").toString())
                    .ratio(actualRation)
                    .build();
            browserRespDTOS.add(browserRespDTO);
        });
        //操作系统访问详情
        List<ShortLinkStatsOsRespDTO> osRespDTOS=new ArrayList<>();
        List<HashMap<String, Object>> osStatsByShortLink = linkOsStatsMapper.listOsStatsByShortLink(requestParam);
        int osSum = osStatsByShortLink.stream()
                .mapToInt(e -> Integer.parseInt(e.get("count").toString()))
                .sum();
        osStatsByShortLink.forEach(e->{
            double ration = (double) Integer.parseInt(e.get("count").toString()) / osSum;
            double actualRation = Math.round(ration * 100.0) / 100.0;
            ShortLinkStatsOsRespDTO osRespDTO = ShortLinkStatsOsRespDTO.builder()
                    .cnt(Integer.parseInt(e.get("count").toString()))
                    .os(e.get("os").toString())
                    .ratio(actualRation)
                    .build();
            osRespDTOS.add(osRespDTO);
        });
        //访客访问类型详情
        List<ShortLinkStatsUvRespDTO> uvTypeStats=new ArrayList<>();
        HashMap<String, Object> typeCntByShortLink = linkAccessLogsMapper.findUvTypeCntByShortLink(requestParam);
        int oldUserCnt = Integer.parseInt(
                Optional.ofNullable(typeCntByShortLink)
                .map(e -> e.get("oldUserCnt"))
                .map(Object::toString)
                .orElse("0"));
        int newUserCnt = Integer.parseInt(Optional.ofNullable(typeCntByShortLink)
                .map(e -> e.get("newUserCnt"))
                .map(Object::toString)
                .orElse("0"));
        int uvSum=oldUserCnt+newUserCnt;
        double oldRation = (double) oldUserCnt / uvSum;
        double actualOldRation = Math.round(oldRation * 100.0) / 100.0;
        double newRation = (double) newUserCnt / uvSum;
        double actualNewRation = Math.round(newRation * 100.0) / 100.0;
        ShortLinkStatsUvRespDTO newUser = ShortLinkStatsUvRespDTO.builder()
                .uvType("newUser")
                .ratio(actualNewRation)
                .cnt(newUserCnt)
                .build();
        uvTypeStats.add(newUser);
        ShortLinkStatsUvRespDTO oldUser = ShortLinkStatsUvRespDTO.builder()
                .uvType("oldUser")
                .ratio(actualOldRation)
                .cnt(oldUserCnt)
                .build();
        uvTypeStats.add(oldUser);
        //访问设备类型详情
        List<ShortLinkStatsDeviceRespDTO> deviceRespDTOS=new ArrayList<>();
        List<LinkDeviceStatsDO> deviceStatsDOS = linkDeviceStatsMapper.listDeviceStatsByShortLink(requestParam);
        int deviceSum = deviceStatsDOS.stream().mapToInt(LinkDeviceStatsDO::getCnt)
                .sum();
        deviceStatsDOS.forEach(e->{
            double ration = (double) e.getCnt() / deviceSum;
            double actualRation = Math.round(ration * 100.0) / 100.0;
            ShortLinkStatsDeviceRespDTO deviceRespDTO = ShortLinkStatsDeviceRespDTO.builder()
                    .device(e.getDevice())
                    .ratio(actualRation)
                    .cnt(e.getCnt())
                    .build();
            deviceRespDTOS.add(deviceRespDTO);
        });
        //访问网络类型详情
        List<ShortLinkStatsNetworkRespDTO> networkRespDTOS=new ArrayList<>();
        List<LinkNetworkStatsDO> networkStatsDOS = linkNetworkStatsMapper.listNetworkStatsByShortLink(requestParam);
        int netWorkSum = networkStatsDOS.stream().mapToInt(LinkNetworkStatsDO::getCnt)
                .sum();
        networkStatsDOS.forEach(e->{
            double ration = (double) e.getCnt() / netWorkSum;
            double actualRation=Math.round(ration*100.0)/100.0;
            ShortLinkStatsNetworkRespDTO networkRespDTO = ShortLinkStatsNetworkRespDTO.builder()
                    .cnt(e.getCnt())
                    .ratio(actualRation)
                    .network(e.getNetwork())
                    .build();
            networkRespDTOS.add(networkRespDTO);
        });
        return ShortLinkStatsRespDTO.builder()
                .pv(pvUvUidStatsByShortLink.getPv())
                .uip(pvUvUidStatsByShortLink.getUip())
                .uv(pvUvUidStatsByShortLink.getUv())
                .daily(daily)
                .localeCnStats(localeCNRespDTOS)
                .networkStats(networkRespDTOS)
                .osStats(osRespDTOS)
                .deviceStats(deviceRespDTOS)
                .topIpStats(topIpRespDTOS)
                .browserStats(browserRespDTOS)
                .weekdayStats(weekdayStats)
                .hourStats(hourStats)
                .uvTypeStats(uvTypeStats)
                .build();
    }

    @Override
    public ShortLinkGroupStatsRespDTO groupShortLinkStats(ShortLinkGroupStatsReqDTO requestParam) {
        List<LinkAccessStatsDO> listStatsByGroup = linkAccessStatsMapper.listStatsByGroup(requestParam);
        if (CollUtil.isEmpty(listStatsByGroup)){
            return null;
        }
        //基础访问数据
        LinkAccessStatsDO pvUvUidStatsByGroup = linkAccessLogsMapper.findPvUvUidStatsByGroup(requestParam);
        //基础访问详情
        List<ShortLinkStatsAccessDailyRespDTO> daily = new ArrayList<>();
        List<String> rangeDates = DateUtil.rangeToList(DateUtil.parse(requestParam.getStartDate()), DateUtil.parse(requestParam.getEndDate()), DateField.DAY_OF_MONTH).stream()
                .map(DateUtil::formatDate)
                .toList();
        rangeDates.forEach(each -> listStatsByGroup.stream()
                .filter(item -> Objects.equals(each, DateUtil.formatDate(item.getDate())))
                .findFirst()
                .ifPresentOrElse(item -> {
                    ShortLinkStatsAccessDailyRespDTO accessDailyRespDTO = ShortLinkStatsAccessDailyRespDTO.builder()
                            .date(each)
                            .pv(item.getPv())
                            .uv(item.getUv())
                            .uip(item.getUip())
                            .build();
                    daily.add(accessDailyRespDTO);
                }, () -> {
                    ShortLinkStatsAccessDailyRespDTO accessDailyRespDTO = ShortLinkStatsAccessDailyRespDTO.builder()
                            .date(each)
                            .pv(0)
                            .uv(0)
                            .uip(0)
                            .build();
                    daily.add(accessDailyRespDTO);
                }));
        //地区访问详情
        List<ShortLinkStatsLocaleCNRespDTO> localeCNRespDTOS=new ArrayList<>();
        List<LinkLocaleStatsDO> listLocaleByGroup=linkLocaleStatsMapper.listLocaleByGroup(requestParam);
        int localeSum = listLocaleByGroup.stream()
                .mapToInt(LinkLocaleStatsDO::getCnt)
                .sum();
        listLocaleByGroup.forEach(e->{
            double ration = (double) e.getCnt() / localeSum;
            double actualRation = Math.round(ration * 100.0) / 100.0;
            ShortLinkStatsLocaleCNRespDTO localeCNRespDTO = ShortLinkStatsLocaleCNRespDTO.builder()
                    .cnt(e.getCnt())
                    .locale(e.getProvince())
                    .ratio(actualRation)
                    .build();
            localeCNRespDTOS.add(localeCNRespDTO);
        });
        //小时访问详情
        List<Integer> hourStats=new ArrayList<>();
        List<LinkAccessStatsDO> hourStatsByGroup = linkAccessStatsMapper.listHourStatsByGroup(requestParam);
        for (int i = 0; i < 24; i++) {
            AtomicInteger hour=new AtomicInteger(i);
            int hourCnt = hourStatsByGroup.stream().filter(e -> Objects.equals(e.getHour(), hour.get()))
                    .findFirst()
                    .map(LinkAccessStatsDO::getPv)
                    .orElse(0);
            hourStats.add(hourCnt);
        }
        //高频访问ip
        List<ShortLinkStatsTopIpRespDTO> topIpStats=new ArrayList<>();
        List<HashMap<String, Object>> listTopIpByGroup = linkAccessLogsMapper.listTopIpByGroup(requestParam);
        listTopIpByGroup.forEach(e->{
            ShortLinkStatsTopIpRespDTO topIpRespDTO = ShortLinkStatsTopIpRespDTO.builder()
                    .ip(e.get("ip").toString())
                    .cnt(Integer.parseInt(e.get("count").toString()))
                    .build();
            topIpStats.add(topIpRespDTO);
        });
        //一周访问详情
        List<Integer> weekdayStats=new ArrayList<>();
        List<LinkAccessStatsDO> statsByGroup = linkOsStatsMapper.listWeekdayStatsByGroup(requestParam);
        for (int i = 1; i < 8; i++) {
            AtomicInteger weekday=new AtomicInteger(i);
            Integer weekdayCnt = statsByGroup.stream()
                    .filter(e -> Objects.equals(e.getWeekday(), weekday.get()))
                    .findFirst()
                    .map(LinkAccessStatsDO::getPv)
                    .orElse(0);
            weekdayStats.add(weekdayCnt);
        }
        //浏览器访问详情
        List<ShortLinkStatsBrowserRespDTO> browserStats=new ArrayList<>();
        List<HashMap<String, Object>> listBrowserStatsByGroup = linkBrowserStatsMapper.listBrowserStatsByGroup(requestParam);
        int browserSum = listBrowserStatsByGroup.stream()
                .mapToInt(e -> Integer.parseInt(e.get("count").toString()))
                .sum();
        listBrowserStatsByGroup.forEach(e->{
            double ration = (double) Integer.parseInt(e.get("count").toString()) / browserSum;
            double actualRation=Math.round(ration*100.0)/100.0;
            ShortLinkStatsBrowserRespDTO browserRespDTO = ShortLinkStatsBrowserRespDTO.builder()
                    .cnt(Integer.parseInt(e.get("count").toString()))
                    .browser(e.get("browser").toString())
                    .ratio(actualRation)
                    .build();
            browserStats.add(browserRespDTO);
        });
        //操作系统访问详情
        List<ShortLinkStatsOsRespDTO> osStats=new ArrayList<>();
        List<HashMap<String, Object>> osStatsByGroup = linkOsStatsMapper.listOsStatsByGroup(requestParam);
        int osSum = osStatsByGroup.stream()
                .mapToInt(e -> Integer.parseInt(e.get("count").toString()))
                .sum();
        osStatsByGroup.forEach(e->{
            double ration = (double) Integer.parseInt(e.get("count").toString()) / osSum;
            double actualRation = Math.round(ration * 100.0) / 100.0;
            ShortLinkStatsOsRespDTO statsOsRespDTO = ShortLinkStatsOsRespDTO.builder()
                    .cnt(Integer.parseInt(e.get("count").toString()))
                    .os(e.get("os").toString())
                    .ratio(actualRation)
                    .build();
            osStats.add(statsOsRespDTO);
        });
        // 访问设备类型详情
        List<ShortLinkStatsDeviceRespDTO> deviceStats = new ArrayList<>();
        List<LinkDeviceStatsDO> listDeviceStatsByGroup = linkDeviceStatsMapper.listDeviceStatsByGroup(requestParam);
        int deviceSum = listDeviceStatsByGroup.stream()
                .mapToInt(LinkDeviceStatsDO::getCnt)
                .sum();
        listDeviceStatsByGroup.forEach(each -> {
            double ratio = (double) each.getCnt() / deviceSum;
            double actualRatio = Math.round(ratio * 100.0) / 100.0;
            ShortLinkStatsDeviceRespDTO deviceRespDTO = ShortLinkStatsDeviceRespDTO.builder()
                    .cnt(each.getCnt())
                    .device(each.getDevice())
                    .ratio(actualRatio)
                    .build();
            deviceStats.add(deviceRespDTO);
        });
        //访问网络类型
        List<ShortLinkStatsNetworkRespDTO> networkRespDTOS=new ArrayList<>();
        List<LinkNetworkStatsDO> networkStatsDOS=linkNetworkStatsMapper.listNetworkStatsByGroup(requestParam);
        int networkSum = networkStatsDOS.stream().mapToInt(LinkNetworkStatsDO::getCnt)
                .sum();
        networkStatsDOS.forEach(e->{
            double ration = (double) e.getCnt() / networkSum;
            double actualRation = Math.round(ration * 100.0) / 100.0;
            ShortLinkStatsNetworkRespDTO networkRespDTO = ShortLinkStatsNetworkRespDTO.builder()
                    .network(e.getNetwork())
                    .ratio(actualRation)
                    .cnt(e.getCnt())
                    .build();
            networkRespDTOS.add(networkRespDTO);
        });
        //访客访问类型
        List<ShortLinkGroupStatsUvRespDTO> uvTypeStats=new ArrayList<>();
        HashMap<String, Object> findUvTypeByShortLink=linkAccessLogsMapper.findUvTypeCntByGroup(requestParam);
        int oldUserCnt = Integer.parseInt(
                Optional.ofNullable(findUvTypeByShortLink)
                        .map(each -> each.get("oldUserCnt"))
                        .map(Object::toString)
                        .orElse("0")
        );
        int newUserCnt = Integer.parseInt(
                Optional.ofNullable(findUvTypeByShortLink)
                        .map(each -> each.get("newUserCnt"))
                        .map(Object::toString)
                        .orElse("0")
        );
        int uvSum = oldUserCnt + newUserCnt;
        double oldRatio = (double) oldUserCnt / uvSum;
        double actualOldRatio = Math.round(oldRatio * 100.0) / 100.0;
        double newRatio = (double) newUserCnt / uvSum;
        double actualNewRatio = Math.round(newRatio * 100.0) / 100.0;
        ShortLinkGroupStatsUvRespDTO newUvRespDTO = ShortLinkGroupStatsUvRespDTO.builder()
                .uvType("newUser")
                .cnt(newUserCnt)
                .ratio(actualNewRatio)
                .build();
        uvTypeStats.add(newUvRespDTO);
        ShortLinkGroupStatsUvRespDTO oldUvRespDTO = ShortLinkGroupStatsUvRespDTO.builder()
                .uvType("oldUser")
                .cnt(oldUserCnt)
                .ratio(actualOldRatio)
                .build();
        uvTypeStats.add(oldUvRespDTO);
        return ShortLinkGroupStatsRespDTO.builder()
                .hourStats(hourStats)
                .deviceStats(deviceStats)
                .weekdayStats(weekdayStats)
                .browserStats(browserStats)
                .topIpStats(topIpStats)
                .localeCnStats(localeCNRespDTOS)
                .osStats(osStats)
                .networkStats(networkRespDTOS)
                .daily(daily)
                .uv(pvUvUidStatsByGroup.getUv())
                .pv(pvUvUidStatsByGroup.getPv())
                .uip(pvUvUidStatsByGroup.getUip())
                .uvTypeStats(uvTypeStats)
                .build();
    }

    @Override
    public IPage<ShortLinkStatsAccessRecordRespDTO> shortLinkStatsAccessRecord(ShortLinkStatsAccessRecordReqDTO requestParam) {
        LambdaQueryWrapper<LinkAccessLogsDO> wrapper = Wrappers.lambdaQuery(LinkAccessLogsDO.class)
                .eq(LinkAccessLogsDO::getFullShortUrl, requestParam.getFullShortUrl())
                .between(LinkAccessLogsDO::getCreateTime, requestParam.getStartDate(), requestParam.getEndDate())
                .eq(LinkAccessLogsDO::getDelFlag, 0)
                .orderByDesc(LinkAccessLogsDO::getCreateTime);
        IPage<LinkAccessLogsDO> linkAccessLogsDOIPage = linkAccessLogsMapper.selectPage(requestParam, wrapper);
        IPage<ShortLinkStatsAccessRecordRespDTO> recordRespDTOIPage = linkAccessLogsDOIPage.convert(e -> BeanUtil.toBean(e, ShortLinkStatsAccessRecordRespDTO.class));
        List<String> userList = recordRespDTOIPage.getRecords().stream()
                .map(ShortLinkStatsAccessRecordRespDTO::getUser)
                .toList();
        List<Map<String,Object>> uvTypeList=linkAccessLogsMapper.selectUvTypeByUsers(requestParam.getGid(),requestParam.getFullShortUrl(),requestParam.getEnableStatus(),requestParam.getStartDate(),requestParam.getEndDate(),userList);
        recordRespDTOIPage.getRecords().forEach(e->{
            String uvType = uvTypeList.stream().filter(item -> Objects.equals(e.getUser(), item.get("user")))
                    .findFirst()
                    .map(item -> item.get("uvType"))
                    .map(Object::toString)
                    .orElse("旧访客");
            e.setUvType(uvType);
        });
        return recordRespDTOIPage;
    }

    @Override
    public IPage<ShortLinkStatsAccessRecordRespDTO> groupShortLinkStatsAccessRecord(ShortLinkGroupStatsAccessRecordReqDTO requestParam) {
        IPage<LinkAccessLogsDO> linkAccessLogsDOIPage = linkAccessLogsMapper.selectGroupPage(requestParam);
        IPage<ShortLinkStatsAccessRecordRespDTO> result = linkAccessLogsDOIPage.convert(e -> BeanUtil.toBean(e, ShortLinkStatsAccessRecordRespDTO.class));
        List<String> userList = result.getRecords().stream().map(ShortLinkStatsAccessRecordRespDTO::getUser)
                .toList();
        List<Map<String, Object>> uvTypeList = linkAccessLogsMapper.selectGroupUvTypeByUsers(requestParam.getGid(), requestParam.getStartDate(), requestParam.getEndDate(), userList);
        result.getRecords().forEach(e->{
            String uvType = uvTypeList.stream().filter(item -> Objects.equals(item, e.getUser()))
                    .findFirst().map(item -> item.get("uvType"))
                    .map(Object::toString)
                    .orElse("旧访客");
            e.setUvType(uvType);
        });
        return result;
    }
}
