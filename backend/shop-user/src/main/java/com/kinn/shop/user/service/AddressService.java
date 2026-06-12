package com.kinn.shop.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.kinn.shop.common.core.BizException;
import com.kinn.shop.common.core.ErrorCode;
import com.kinn.shop.user.dto.AddressDTO;
import com.kinn.shop.user.entity.UserAddress;
import com.kinn.shop.user.mapper.UserAddressMapper;
import com.kinn.shop.user.vo.AddressVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 收货地址：所有操作均校验记录归属（user_id = 当前登录用户），防越权。
 */
@Service
@RequiredArgsConstructor
public class AddressService {

    private final UserAddressMapper addressMapper;

    public List<AddressVO> list(long userId) {
        return addressMapper.selectList(new LambdaQueryWrapper<UserAddress>()
                        .eq(UserAddress::getUserId, userId)
                        .orderByDesc(UserAddress::getIsDefault)
                        .orderByDesc(UserAddress::getId))
                .stream().map(AddressVO::from).toList();
    }

    @Transactional
    public AddressVO add(long userId, AddressDTO dto) {
        UserAddress address = new UserAddress();
        address.setUserId(userId);
        applyDto(address, dto);
        boolean asDefault = Boolean.TRUE.equals(dto.getIsDefault());
        if (asDefault) {
            clearDefault(userId);
        }
        address.setIsDefault(asDefault ? 1 : 0);
        addressMapper.insert(address);
        return AddressVO.from(addressMapper.selectById(address.getId()));
    }

    /** PUT 全量更新（可空字段传 null 即清空） */
    @Transactional
    public AddressVO update(long userId, long id, AddressDTO dto) {
        requireOwned(userId, id);
        if (Boolean.TRUE.equals(dto.getIsDefault())) {
            clearDefault(userId);
        }
        LambdaUpdateWrapper<UserAddress> wrapper = new LambdaUpdateWrapper<UserAddress>()
                .eq(UserAddress::getId, id)
                .eq(UserAddress::getUserId, userId)
                .set(UserAddress::getReceiverName, dto.getReceiverName())
                .set(UserAddress::getPhone, dto.getPhone())
                .set(UserAddress::getCountryCode, dto.getCountryCode().toUpperCase())
                .set(UserAddress::getState, dto.getState())
                .set(UserAddress::getCity, dto.getCity())
                .set(UserAddress::getAddressLine1, dto.getAddressLine1())
                .set(UserAddress::getAddressLine2, dto.getAddressLine2())
                .set(UserAddress::getPostcode, dto.getPostcode());
        if (dto.getIsDefault() != null) {
            wrapper.set(UserAddress::getIsDefault, Boolean.TRUE.equals(dto.getIsDefault()) ? 1 : 0);
        }
        addressMapper.update(null, wrapper);
        return AddressVO.from(addressMapper.selectById(id));
    }

    public void delete(long userId, long id) {
        requireOwned(userId, id);
        addressMapper.deleteById(id);
    }

    /** 同一用户互斥置默认 */
    @Transactional
    public void setDefault(long userId, long id) {
        requireOwned(userId, id);
        clearDefault(userId);
        UserAddress update = new UserAddress();
        update.setId(id);
        update.setIsDefault(1);
        addressMapper.updateById(update);
    }

    private void clearDefault(long userId) {
        addressMapper.update(null, new LambdaUpdateWrapper<UserAddress>()
                .eq(UserAddress::getUserId, userId)
                .eq(UserAddress::getIsDefault, 1)
                .set(UserAddress::getIsDefault, 0));
    }

    private UserAddress requireOwned(long userId, long id) {
        UserAddress address = addressMapper.selectById(id);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        return address;
    }

    private void applyDto(UserAddress address, AddressDTO dto) {
        address.setReceiverName(dto.getReceiverName());
        address.setPhone(dto.getPhone());
        address.setCountryCode(dto.getCountryCode().toUpperCase());
        address.setState(dto.getState());
        address.setCity(dto.getCity());
        address.setAddressLine1(dto.getAddressLine1());
        address.setAddressLine2(dto.getAddressLine2());
        address.setPostcode(dto.getPostcode());
    }
}
