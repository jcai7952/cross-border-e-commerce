package com.kinn.shop.product.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kinn.shop.common.constant.RedisKeys;
import com.kinn.shop.common.core.BizException;
import com.kinn.shop.common.core.ErrorCode;
import com.kinn.shop.product.entity.ExchangeRate;
import com.kinn.shop.product.mapper.ExchangeRateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 汇率：base=USD。DB 为准，Redis hash 缓存 10 分钟；
 * 每日 06:00 调 frankfurter API 刷新，失败保留旧值；后台可手工覆盖。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private static final String BASE = "USD";
    private static final String API_SYMBOLS = "CNY,EUR,GBP,JPY";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private final ExchangeRateMapper exchangeRateMapper;
    private final StringRedisTemplate stringRedisTemplate;

    private final RestClient restClient = RestClient.builder()
            .baseUrl("https://api.frankfurter.app")
            .requestFactory(timeoutFactory())
            .build();

    private static SimpleClientHttpRequestFactory timeoutFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        return factory;
    }

    /** 1 USD = ? quote。USD 直接返回 1；不支持的币种抛 CURRENCY_NOT_SUPPORTED。 */
    public BigDecimal getRate(String quote) {
        if (quote == null || BASE.equalsIgnoreCase(quote)) {
            return BigDecimal.ONE;
        }
        String q = quote.trim().toUpperCase();
        Object cached = stringRedisTemplate.opsForHash().get(RedisKeys.EXCHANGE_RATES, q);
        if (cached != null) {
            return new BigDecimal(cached.toString());
        }
        BigDecimal rate = loadAndCache().get(q);
        if (rate == null) {
            throw new BizException(ErrorCode.CURRENCY_NOT_SUPPORTED);
        }
        return rate;
    }

    private Map<String, BigDecimal> loadAndCache() {
        List<ExchangeRate> rows = exchangeRateMapper.selectList(Wrappers.<ExchangeRate>lambdaQuery()
                .eq(ExchangeRate::getBaseCurrency, BASE));
        Map<String, BigDecimal> rates = new HashMap<>();
        Map<String, String> hash = new HashMap<>();
        for (ExchangeRate row : rows) {
            rates.put(row.getQuoteCurrency(), row.getRate());
            hash.put(row.getQuoteCurrency(), row.getRate().toPlainString());
        }
        if (!hash.isEmpty()) {
            stringRedisTemplate.opsForHash().putAll(RedisKeys.EXCHANGE_RATES, hash);
            stringRedisTemplate.expire(RedisKeys.EXCHANGE_RATES, CACHE_TTL);
        }
        return rates;
    }

    public void clearCache() {
        stringRedisTemplate.delete(RedisKeys.EXCHANGE_RATES);
    }

    /** 管理端全量列表。 */
    public List<ExchangeRate> listAll() {
        return exchangeRateMapper.selectList(Wrappers.<ExchangeRate>lambdaQuery()
                .orderByAsc(ExchangeRate::getQuoteCurrency));
    }

    /** 手工覆盖，source=MANUAL，清缓存。 */
    @Transactional(rollbackFor = Exception.class)
    public void manualOverride(String quote, BigDecimal rate) {
        String q = quote.trim().toUpperCase();
        if (BASE.equals(q)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "基准币 USD 无需设置汇率");
        }
        upsert(q, rate, "MANUAL");
        clearCache();
    }

    /** 每日 06:00 自动刷新。 */
    @Scheduled(cron = "0 0 6 * * ?")
    public void scheduledRefresh() {
        refreshFromApi();
    }

    /** 调 frankfurter 刷新（source=API），失败 log.warn 保留旧值；返回最新列表。 */
    public List<ExchangeRate> refreshFromApi() {
        try {
            FrankfurterResp resp = restClient.get()
                    .uri("/latest?base={base}&symbols={symbols}", BASE, API_SYMBOLS)
                    .retrieve()
                    .body(FrankfurterResp.class);
            if (resp == null || resp.rates() == null || resp.rates().isEmpty()) {
                log.warn("[rate] frankfurter returned empty rates, keep old values");
                return listAll();
            }
            resp.rates().forEach((quote, rate) -> upsert(quote.toUpperCase(), rate, "API"));
            clearCache();
            log.info("[rate] refreshed {} rates from frankfurter (date={})", resp.rates().size(), resp.date());
        } catch (Exception e) {
            log.warn("[rate] frankfurter refresh failed, keep old values: {}", e.getMessage());
        }
        return listAll();
    }

    private void upsert(String quote, BigDecimal rate, String source) {
        ExchangeRate row = exchangeRateMapper.selectOne(Wrappers.<ExchangeRate>lambdaQuery()
                .eq(ExchangeRate::getBaseCurrency, BASE)
                .eq(ExchangeRate::getQuoteCurrency, quote));
        if (row == null) {
            row = new ExchangeRate();
            row.setBaseCurrency(BASE);
            row.setQuoteCurrency(quote);
            row.setRate(rate);
            row.setSource(source);
            exchangeRateMapper.insert(row);
        } else {
            row.setRate(rate);
            row.setSource(source);
            exchangeRateMapper.updateById(row);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FrankfurterResp(BigDecimal amount, String base, String date, Map<String, BigDecimal> rates) {
    }
}
