package com.kinn.shop.product.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.kinn.shop.common.core.BizException;
import com.kinn.shop.common.core.ErrorCode;
import com.kinn.shop.product.entity.Currency;
import com.kinn.shop.product.mapper.CurrencyMapper;
import com.kinn.shop.product.vo.CurrencyVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 币种：列表 + 校验取用。
 */
@Service
@RequiredArgsConstructor
public class CurrencyService {

    private final CurrencyMapper currencyMapper;

    /** enabled=1 按 sort 升序。 */
    public List<CurrencyVO> listEnabled() {
        return currencyMapper.selectList(Wrappers.<Currency>lambdaQuery()
                        .eq(Currency::getEnabled, 1)
                        .orderByAsc(Currency::getSort))
                .stream().map(c -> {
                    CurrencyVO vo = new CurrencyVO();
                    vo.setCode(c.getCode());
                    vo.setSymbol(c.getSymbol());
                    vo.setNameZh(c.getNameZh());
                    vo.setNameEn(c.getNameEn());
                    vo.setDecimalDigits(c.getDecimalDigits());
                    return vo;
                }).toList();
    }

    /** 取启用币种，不存在或停用抛 CURRENCY_NOT_SUPPORTED。 */
    public Currency getRequired(String code) {
        Currency currency = currencyMapper.selectById(code);
        if (currency == null || currency.getEnabled() == null || currency.getEnabled() != 1) {
            throw new BizException(ErrorCode.CURRENCY_NOT_SUPPORTED);
        }
        return currency;
    }
}
