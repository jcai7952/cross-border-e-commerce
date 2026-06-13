package com.kinn.shop.product.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.kinn.shop.common.core.BizException;
import com.kinn.shop.common.core.ErrorCode;
import com.kinn.shop.product.dto.FlashSaleSaveDTO;
import com.kinn.shop.product.entity.FlashSale;
import com.kinn.shop.product.entity.FlashSaleItem;
import com.kinn.shop.product.entity.Product;
import com.kinn.shop.product.mapper.FlashSaleItemMapper;
import com.kinn.shop.product.mapper.FlashSaleMapper;
import com.kinn.shop.product.mapper.ProductMapper;
import com.kinn.shop.product.vo.AdminFlashSaleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理端闪购维护：活动列表（含商品明细）/ 创建 / 全量更新（items 删后插）/ 启停。
 * FlashSaleService.current() 直查 DB 无缓存，无需失效处理。
 */
@Service
@RequiredArgsConstructor
public class AdminFlashSaleService {

    private final FlashSaleMapper flashSaleMapper;
    private final FlashSaleItemMapper flashSaleItemMapper;
    private final ProductMapper productMapper;
    private final ProductAssembler productAssembler;

    public List<AdminFlashSaleVO> list() {
        List<FlashSale> sales = flashSaleMapper.selectList(Wrappers.<FlashSale>lambdaQuery()
                .orderByDesc(FlashSale::getId));
        if (sales.isEmpty()) {
            return List.of();
        }
        List<FlashSaleItem> items = flashSaleItemMapper.selectList(Wrappers.<FlashSaleItem>lambdaQuery()
                .in(FlashSaleItem::getSaleId, sales.stream().map(FlashSale::getId).toList())
                .orderByAsc(FlashSaleItem::getId));
        Map<Long, List<FlashSaleItem>> itemsBySale = items.stream()
                .collect(Collectors.groupingBy(FlashSaleItem::getSaleId));
        List<Long> productIds = items.stream().map(FlashSaleItem::getProductId).distinct().toList();
        Map<Long, String> namesEn = productIds.isEmpty() ? Map.of()
                : productAssembler.resolveNames(productIds, Locales.EN);
        return sales.stream().map(sale -> {
            AdminFlashSaleVO vo = new AdminFlashSaleVO();
            vo.setId(sale.getId());
            vo.setTitle(sale.getTitle());
            vo.setStartTime(sale.getStartTime());
            vo.setEndTime(sale.getEndTime());
            vo.setStatus(sale.getStatus());
            vo.setCreateTime(sale.getCreateTime());
            vo.setUpdateTime(sale.getUpdateTime());
            vo.setItems(itemsBySale.getOrDefault(sale.getId(), List.of()).stream().map(item -> {
                AdminFlashSaleVO.ItemVO iv = new AdminFlashSaleVO.ItemVO();
                iv.setId(item.getId());
                iv.setProductId(item.getProductId());
                iv.setProductNameEn(namesEn.get(item.getProductId()));
                iv.setDiscountPercent(item.getDiscountPercent());
                iv.setQuota(item.getQuota());
                iv.setSold(item.getSold());
                return iv;
            }).toList());
            return vo;
        }).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(FlashSaleSaveDTO dto) {
        validate(dto);
        FlashSale sale = new FlashSale();
        sale.setTitle(dto.getTitle().trim());
        sale.setStartTime(dto.getStartTime());
        sale.setEndTime(dto.getEndTime());
        sale.setStatus(1);
        flashSaleMapper.insert(sale);
        insertItems(sale.getId(), dto);
        return sale.getId();
    }

    /** 全量更新：基本信息覆盖，items 删后插（sold 归零）。 */
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, FlashSaleSaveDTO dto) {
        FlashSale sale = requireSale(id);
        validate(dto);
        sale.setTitle(dto.getTitle().trim());
        sale.setStartTime(dto.getStartTime());
        sale.setEndTime(dto.getEndTime());
        flashSaleMapper.updateById(sale);
        flashSaleItemMapper.delete(Wrappers.<FlashSaleItem>lambdaQuery()
                .eq(FlashSaleItem::getSaleId, id));
        insertItems(id, dto);
    }

    public void updateStatus(Long id, Integer status) {
        FlashSale sale = requireSale(id);
        sale.setStatus(status);
        flashSaleMapper.updateById(sale);
    }

    private FlashSale requireSale(Long id) {
        FlashSale sale = flashSaleMapper.selectById(id);
        if (sale == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "闪购活动不存在");
        }
        return sale;
    }

    private void validate(FlashSaleSaveDTO dto) {
        if (!dto.getStartTime().isBefore(dto.getEndTime())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "开始时间必须早于结束时间");
        }
        List<Long> productIds = dto.getItems().stream()
                .map(FlashSaleSaveDTO.ItemDTO::getProductId).toList();
        if (new HashSet<>(productIds).size() != productIds.size()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "闪购商品重复");
        }
        Set<Long> existing = productMapper.selectBatchIds(productIds).stream()
                .map(Product::getId).collect(Collectors.toSet());
        for (Long productId : productIds) {
            if (!existing.contains(productId)) {
                throw new BizException(ErrorCode.PARAM_ERROR, "商品不存在: " + productId);
            }
        }
    }

    private void insertItems(Long saleId, FlashSaleSaveDTO dto) {
        for (FlashSaleSaveDTO.ItemDTO item : dto.getItems()) {
            FlashSaleItem row = new FlashSaleItem();
            row.setSaleId(saleId);
            row.setProductId(item.getProductId());
            row.setDiscountPercent(item.getDiscountPercent());
            row.setQuota(item.getQuota() == null ? 0 : item.getQuota());
            row.setSold(0);
            flashSaleItemMapper.insert(row);
        }
    }
}
