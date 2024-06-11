package com.guolihong.shortlink.project.service.impl;

import cn.hutool.core.text.StrBuilder;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guolihong.shortlink.project.common.convention.exception.ClientException;
import com.guolihong.shortlink.project.common.convention.exception.ServiceException;
import com.guolihong.shortlink.project.config.GotoDomainWhiteListConfiguration;
import com.guolihong.shortlink.project.dao.entity.ShortLinkDO;
import com.guolihong.shortlink.project.dao.entity.ShortLinkGotoDO;
import com.guolihong.shortlink.project.dao.mapper.ShortLinkGotoMapper;
import com.guolihong.shortlink.project.dao.mapper.ShortLinkMapper;
import com.guolihong.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.guolihong.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import com.guolihong.shortlink.project.service.ShortLinkService;
import com.guolihong.shortlink.project.toolkit.HashUtil;
import com.guolihong.shortlink.project.toolkit.LinkUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.guolihong.shortlink.project.common.constant.RedisKeyConstant.GOTO_SHORT_LINK_KEY;
import static com.guolihong.shortlink.project.common.constant.RedisKeyConstant.SHORT_LINK_CREATE_LOCK_KEY;

@Service
@RequiredArgsConstructor
public class ShortLinkServiceImpl extends ServiceImpl<ShortLinkMapper, ShortLinkDO> implements ShortLinkService {
    private final RBloomFilter<String> shortLinkCachePenetrationBloomFilter;
    @Value("${short-link.domain.default}")
    private String createShortLinkDefaultDomain;
    private final ShortLinkGotoMapper shortLinkGotoMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final GotoDomainWhiteListConfiguration gotoDomainWhiteListConfiguration;
    private final RedissonClient redissonClient;
    /**
     * 创建短链接
     * @param requestParam
     * @return
     */
    @Override
    public ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO requestParam) {
        // 检查原始连接是否在白名单内
        verificationWhitelist(requestParam.getOriginUrl());
        String suffix=generateSuffix(requestParam);
        String fullShortUrl=requestParam.getDomain()+"/"+suffix;
        ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                .domain(createShortLinkDefaultDomain)
                .originUrl(requestParam.getOriginUrl())
                .gid(requestParam.getGid())
                .createdType(requestParam.getCreatedType())
                .validDateType(requestParam.getValidDateType())
                .validDate(requestParam.getValidDate())
                .describe(requestParam.getDescribe())
                .shortUri(suffix)
                .enableStatus(0)
                .totalPv(0)
                .totalUv(0)
                .totalUip(0)
                .delTime(0L)
                .fullShortUrl(fullShortUrl)
                .favicon(getFavicon(requestParam.getOriginUrl()))
                .build();
        ShortLinkGotoDO shortLinkGotoDO=ShortLinkGotoDO.builder()
                .fullShortUrl(fullShortUrl)
                .gid(requestParam.getGid())
                .build();
        try {
            baseMapper.insert(shortLinkDO);
            shortLinkGotoMapper.insert(shortLinkGotoDO);
        }catch (DuplicateKeyException e){
            if (!shortLinkCachePenetrationBloomFilter.contains(fullShortUrl)){
                //如果布隆过滤器中没有，需要添加进去
                shortLinkCachePenetrationBloomFilter.add(fullShortUrl);
            }
        }
        //将创建的短链接放入到redis中,这里如果短链接为永久有效则使用默认有效期，负责使用用户提供的有效期
        stringRedisTemplate.opsForValue().set(String.format(GOTO_SHORT_LINK_KEY,fullShortUrl)
                ,requestParam.getOriginUrl(), LinkUtil.getLinkCacheValidTime(requestParam.getValidDate()), TimeUnit.MILLISECONDS);
        return ShortLinkCreateRespDTO.builder()
                .fullShortUrl("http://"+fullShortUrl)
                .gid(requestParam.getGid())
                .originUrl(requestParam.getOriginUrl())
                .build();
    }

    /**
     * 基于分布式锁的方式创建短链接
     * @param requestParam
     * @return
     */
    @Override
    public ShortLinkCreateRespDTO createShortLinkByLock(ShortLinkCreateReqDTO requestParam) {
        verificationWhitelist(requestParam.getOriginUrl());
        String fullShortUrl;
        // 为什么说布隆过滤器性能远胜于分布式锁？这里对字符串加锁，所有的请求只有一个线程可以获取锁，性能太差
        RLock lock = redissonClient.getLock(SHORT_LINK_CREATE_LOCK_KEY);
        lock.lock();
        try {
            String shortLinkSuffix = generateSuffixByLock(requestParam);
            fullShortUrl = StrBuilder.create(createShortLinkDefaultDomain)
                    .append("/")
                    .append(shortLinkSuffix)
                    .toString();
            ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                    .domain(createShortLinkDefaultDomain)
                    .originUrl(requestParam.getOriginUrl())
                    .gid(requestParam.getGid())
                    .createdType(requestParam.getCreatedType())
                    .validDateType(requestParam.getValidDateType())
                    .validDate(requestParam.getValidDate())
                    .describe(requestParam.getDescribe())
                    .shortUri(shortLinkSuffix)
                    .enableStatus(0)
                    .totalPv(0)
                    .totalUv(0)
                    .totalUip(0)
                    .delTime(0L)
                    .fullShortUrl(fullShortUrl)
                    .favicon(getFavicon(requestParam.getOriginUrl()))
                    .build();
            ShortLinkGotoDO linkGotoDO = ShortLinkGotoDO.builder()
                    .fullShortUrl(fullShortUrl)
                    .gid(requestParam.getGid())
                    .build();
            try {
                baseMapper.insert(shortLinkDO);
                shortLinkGotoMapper.insert(linkGotoDO);
            } catch (DuplicateKeyException ex) {
                throw new ServiceException(String.format("短链接：%s 生成重复", fullShortUrl));
            }
            stringRedisTemplate.opsForValue().set(
                    String.format(GOTO_SHORT_LINK_KEY, fullShortUrl),
                    requestParam.getOriginUrl(),
                    LinkUtil.getLinkCacheValidTime(requestParam.getValidDate()), TimeUnit.MILLISECONDS
            );
        } finally {
            lock.unlock();
        }
        return ShortLinkCreateRespDTO.builder()
                .fullShortUrl("http://" + fullShortUrl)
                .originUrl(requestParam.getOriginUrl())
                .gid(requestParam.getGid())
                .build();
    }

    private String generateSuffixByLock(ShortLinkCreateReqDTO requestParam) {
        String suffix;
        int customGenCount=0;
        while (true){
            if (customGenCount>10){
                throw new ServiceException("短链接重复创建，请稍后再试");
            }
            suffix= HashUtil.hashToBase62(requestParam.getOriginUrl()+ UUID.randomUUID());
            LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                    .eq(ShortLinkDO::getGid, requestParam.getGid())
                    .eq(ShortLinkDO::getFullShortUrl, createShortLinkDefaultDomain + "/" + suffix)
                    .eq(ShortLinkDO::getDelFlag, 0);
            ShortLinkDO shortLinkDO = baseMapper.selectOne(queryWrapper);
            if (shortLinkDO==null){
                break;
            }
            customGenCount++;
        }
        return suffix;
    }

    private void verificationWhitelist(String originUrl) {
        Boolean enable = gotoDomainWhiteListConfiguration.getEnable();
        if (enable==null|| !enable){
            return;
        }
        //通过原始链接获取网站域名
        String domain=LinkUtil.extractDomain(originUrl);
        if (StrUtil.isBlank(domain)){
            throw new ClientException("短链接填写有误");
        }
        List<String> details=gotoDomainWhiteListConfiguration.getDetails();
        if (!details.contains(domain)){
            throw new ClientException("演示环境为避免恶意攻击，请生成以下网站连接："+gotoDomainWhiteListConfiguration.getNames());
        }
    }

    /**
     * 获取网站图标
     * @param url
     * @return
     */
    @SneakyThrows
    private String getFavicon(String url) {
        URL targetUrl = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) targetUrl.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();
        int responseCode = connection.getResponseCode();
        if (HttpURLConnection.HTTP_OK == responseCode) {
            Document document = Jsoup.connect(url).get();
            Element faviconLink = document.select("link[rel~=(?i)^(shortcut )?icon]").first();
            if (faviconLink != null) {
                return faviconLink.attr("abs:href");
            }
        }
        return null;
    }

    private String generateSuffix(ShortLinkCreateReqDTO requestParam) {
        //避免原始连接相同造成hash冲突
        String suffix;
        int customGenCount=0;
        while (true){
            if (customGenCount>10){
                throw new ServiceException("短链接重复创建，请稍后再试");
            }
            suffix= HashUtil.hashToBase62(requestParam.getOriginUrl()+ UUID.randomUUID());
            if (!shortLinkCachePenetrationBloomFilter.contains(suffix)){
                break;
            }
            customGenCount++;
        }
        return suffix;
    }
}
