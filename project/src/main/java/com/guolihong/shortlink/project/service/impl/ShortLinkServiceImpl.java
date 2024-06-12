package com.guolihong.shortlink.project.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.text.StrBuilder;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guolihong.shortlink.project.common.constant.RedisKeyConstant;
import com.guolihong.shortlink.project.common.convention.exception.ClientException;
import com.guolihong.shortlink.project.common.convention.exception.ServiceException;
import com.guolihong.shortlink.project.common.enums.VailDateTypeEnum;
import com.guolihong.shortlink.project.config.GotoDomainWhiteListConfiguration;
import com.guolihong.shortlink.project.dao.entity.ShortLinkDO;
import com.guolihong.shortlink.project.dao.entity.ShortLinkGotoDO;
import com.guolihong.shortlink.project.dao.mapper.ShortLinkGotoMapper;
import com.guolihong.shortlink.project.dao.mapper.ShortLinkMapper;
import com.guolihong.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.guolihong.shortlink.project.dto.req.ShortLinkPageReqDTO;
import com.guolihong.shortlink.project.dto.req.ShortLinkUpdateReqDTO;
import com.guolihong.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import com.guolihong.shortlink.project.dto.resp.ShortLinkPageRespDTO;
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
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Objects;
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
    @Transactional
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
    @Transactional
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

    @Override
    public IPage<ShortLinkPageRespDTO> pageQuery(ShortLinkPageReqDTO requestParam) {
        IPage<ShortLinkDO> shortLinkDOIPage = baseMapper.pageLink(requestParam);
        return shortLinkDOIPage.convert(e->{
            ShortLinkPageRespDTO shortLinkPageRespDTO = BeanUtil.toBean(e, ShortLinkPageRespDTO.class);
            shortLinkPageRespDTO.setDomain("http://"+e.getDomain());
            return shortLinkPageRespDTO;
        });
    }
    @Transactional
    @Override
    public void updateShortLink(ShortLinkUpdateReqDTO requestParam) {
        // 检查原始连接是否在白名单内
        verificationWhitelist(requestParam.getOriginUrl());
        LambdaQueryWrapper<ShortLinkDO> eq = Wrappers.lambdaQuery(ShortLinkDO.class)
                .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                .eq(ShortLinkDO::getGid, requestParam.getOriginGid())
                .eq(ShortLinkDO::getEnableStatus, 0)
                .eq(ShortLinkDO::getDelFlag, 0);
        ShortLinkDO shortLinkDO = baseMapper.selectOne(eq);
        if (shortLinkDO==null){
            throw new ClientException("短链接记录不存在");
        }
        //这里需要判断用户是否将当前短链接移动到了其他分组
        if (requestParam.getGid().equals(shortLinkDO.getGid())){
            LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                    .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                    .eq(ShortLinkDO::getGid, requestParam.getGid())
                    .eq(ShortLinkDO::getDelFlag, 0)
                    .eq(ShortLinkDO::getEnableStatus, 0)
                    .set(Objects.equals(requestParam.getValidDateType(), VailDateTypeEnum.PERMANENT), ShortLinkDO::getValidDate, null);
            ShortLinkDO linkDO = ShortLinkDO.builder()
                    .domain(shortLinkDO.getDomain())
                    .shortUri(shortLinkDO.getShortUri())
                    .favicon(getFavicon(requestParam.getOriginUrl()))
                    .createdType(shortLinkDO.getCreatedType())
                    .gid(requestParam.getGid())
                    .originUrl(requestParam.getOriginUrl())
                    .describe(requestParam.getDescribe())
                    .validDateType(requestParam.getValidDateType())
                    .validDate(requestParam.getValidDate())
                    .build();
            baseMapper.update(linkDO,updateWrapper);
        }else{
            //当前短链接分组改变,这里使用先删除后新增
            RReadWriteLock lock= redissonClient.getReadWriteLock(RedisKeyConstant.LOCK_GID_UPDATE_KEY + requestParam.getFullShortUrl());
            RLock rLock = lock.writeLock();
            rLock.lock();
            try {
                LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                        .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(ShortLinkDO::getGid, shortLinkDO.getGid())
                        .eq(ShortLinkDO::getDelFlag, 0)
                        .eq(ShortLinkDO::getEnableStatus, 0)
                        .eq(ShortLinkDO::getDelTime,0l);
                ShortLinkDO linkDO = ShortLinkDO.builder()
                        .delTime(System.currentTimeMillis())
                        .build();
                linkDO.setDelFlag(1);
                baseMapper.update(linkDO,updateWrapper);
                ShortLinkDO build = ShortLinkDO.builder().domain(createShortLinkDefaultDomain)
                        .originUrl(requestParam.getOriginUrl())
                        .gid(requestParam.getGid())
                        .createdType(shortLinkDO.getCreatedType())
                        .validDateType(requestParam.getValidDateType())
                        .validDate(requestParam.getValidDate())
                        .describe(requestParam.getDescribe())
                        .shortUri(shortLinkDO.getShortUri())
                        .enableStatus(shortLinkDO.getEnableStatus())
                        .totalPv(shortLinkDO.getTotalPv())
                        .totalUv(shortLinkDO.getTotalUv())
                        .totalUip(shortLinkDO.getTotalUip())
                        .delTime(0L)
                        .fullShortUrl(shortLinkDO.getFullShortUrl())
                        .favicon(getFavicon(requestParam.getOriginUrl()))
                        .build();
                baseMapper.insert(build);
                LambdaQueryWrapper<ShortLinkGotoDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                        .eq(ShortLinkGotoDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(ShortLinkGotoDO::getGid, shortLinkDO.getGid());
                shortLinkGotoMapper.delete(queryWrapper);
                ShortLinkGotoDO linkGotoDO = ShortLinkGotoDO.builder()
                        .gid(requestParam.getGid())
                        .fullShortUrl(requestParam.getFullShortUrl())
                        .build();
                shortLinkGotoMapper.insert(linkGotoDO);
            }finally {
                rLock.unlock();
            }
        }
        //TODO 还需要保持缓存中数据和短链接数据一致

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
