package com.kinn.shop.product.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.kinn.shop.common.core.BizException;
import com.kinn.shop.common.core.ErrorCode;
import com.kinn.shop.product.dto.CategorySaveDTO;
import com.kinn.shop.product.entity.Category;
import com.kinn.shop.product.entity.Product;
import com.kinn.shop.product.mapper.CategoryMapper;
import com.kinn.shop.product.mapper.ProductMapper;
import com.kinn.shop.product.vo.AdminCategoryVO;
import com.kinn.shop.product.vo.CategoryTreeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 类目：前台两级树 + 管理端 CRUD。
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;

    /** 前台两级树（仅 status=1），名称按 locale。 */
    public List<CategoryTreeVO> tree(String locale) {
        boolean zh = Locales.isZh(locale);
        List<Category> all = categoryMapper.selectList(Wrappers.<Category>lambdaQuery()
                .eq(Category::getStatus, 1)
                .orderByAsc(Category::getSort)
                .orderByAsc(Category::getId));
        Map<Long, List<Category>> childrenByParent = all.stream()
                .filter(c -> c.getParentId() != null && c.getParentId() != 0)
                .collect(Collectors.groupingBy(Category::getParentId));
        return all.stream()
                .filter(c -> c.getParentId() == null || c.getParentId() == 0)
                .map(c -> {
                    CategoryTreeVO vo = new CategoryTreeVO();
                    vo.setId(c.getId());
                    vo.setName(zh ? c.getNameZh() : c.getNameEn());
                    vo.setIcon(c.getIcon());
                    vo.setChildren(childrenByParent.getOrDefault(c.getId(), List.of()).stream()
                            .map(ch -> {
                                CategoryTreeVO cv = new CategoryTreeVO();
                                cv.setId(ch.getId());
                                cv.setName(zh ? ch.getNameZh() : ch.getNameEn());
                                return cv;
                            }).toList());
                    return vo;
                }).toList();
    }

    /** 一级类目展开为 [自身 + 子类目]，其余返回 [自身]。 */
    public List<Long> expandCategoryIds(Long categoryId) {
        Category category = categoryMapper.selectById(categoryId);
        if (category == null || category.getLevel() == null || category.getLevel() != 1) {
            return List.of(categoryId);
        }
        List<Long> ids = new ArrayList<>();
        ids.add(categoryId);
        categoryMapper.selectList(Wrappers.<Category>lambdaQuery().eq(Category::getParentId, categoryId))
                .forEach(c -> ids.add(c.getId()));
        return ids;
    }

    /** 管理端全量树（含停用、税率、排序）。 */
    public List<AdminCategoryVO> adminTree() {
        List<Category> all = categoryMapper.selectList(Wrappers.<Category>lambdaQuery()
                .orderByAsc(Category::getSort)
                .orderByAsc(Category::getId));
        Map<Long, List<Category>> childrenByParent = all.stream()
                .filter(c -> c.getParentId() != null && c.getParentId() != 0)
                .collect(Collectors.groupingBy(Category::getParentId));
        return all.stream()
                .filter(c -> c.getParentId() == null || c.getParentId() == 0)
                .map(c -> {
                    AdminCategoryVO vo = toAdminVO(c);
                    vo.setChildren(childrenByParent.getOrDefault(c.getId(), List.of()).stream()
                            .map(this::toAdminVO).toList());
                    return vo;
                }).toList();
    }

    private AdminCategoryVO toAdminVO(Category c) {
        AdminCategoryVO vo = new AdminCategoryVO();
        vo.setId(c.getId());
        vo.setParentId(c.getParentId());
        vo.setLevel(c.getLevel());
        vo.setNameZh(c.getNameZh());
        vo.setNameEn(c.getNameEn());
        vo.setIcon(c.getIcon());
        vo.setSort(c.getSort());
        vo.setPostalTaxRate(c.getPostalTaxRate());
        vo.setStatus(c.getStatus());
        return vo;
    }

    public Long create(CategorySaveDTO dto) {
        long parentId = checkParent(dto.getParentId(), null);
        Category category = new Category();
        apply(dto, category, parentId);
        categoryMapper.insert(category);
        return category.getId();
    }

    public void update(Long id, CategorySaveDTO dto) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "类目不存在");
        }
        long parentId = checkParent(dto.getParentId(), id);
        apply(dto, category, parentId);
        categoryMapper.updateById(category);
    }

    public void delete(Long id) {
        Long children = categoryMapper.selectCount(Wrappers.<Category>lambdaQuery().eq(Category::getParentId, id));
        Long products = productMapper.selectCount(Wrappers.<Product>lambdaQuery().eq(Product::getCategoryId, id));
        if (children > 0 || products > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "存在子类目或商品，不可删除");
        }
        categoryMapper.deleteById(id);
    }

    private long checkParent(Long parentIdParam, Long selfId) {
        long parentId = parentIdParam == null ? 0 : parentIdParam;
        if (parentId == 0) {
            return 0;
        }
        if (selfId != null && parentId == selfId) {
            throw new BizException(ErrorCode.PARAM_ERROR, "父类目不能是自身");
        }
        Category parent = categoryMapper.selectById(parentId);
        if (parent == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "父类目不存在");
        }
        if (parent.getLevel() != null && parent.getLevel() >= 2) {
            throw new BizException(ErrorCode.PARAM_ERROR, "仅支持两级类目");
        }
        return parentId;
    }

    private void apply(CategorySaveDTO dto, Category category, long parentId) {
        category.setParentId(parentId);
        category.setLevel(parentId == 0 ? 1 : 2);
        category.setNameZh(dto.getNameZh());
        category.setNameEn(dto.getNameEn());
        category.setIcon(dto.getIcon());
        category.setSort(dto.getSort() == null ? 0 : dto.getSort());
        category.setPostalTaxRate(dto.getPostalTaxRate() == null ? 20 : dto.getPostalTaxRate());
        category.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
    }
}
