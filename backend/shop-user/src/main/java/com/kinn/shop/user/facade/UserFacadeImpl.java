package com.kinn.shop.user.facade;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.kinn.shop.api.user.UserFacade;
import com.kinn.shop.api.user.dto.AddressDTO;
import com.kinn.shop.api.user.dto.IdentityDTO;
import com.kinn.shop.user.entity.UserAddress;
import com.kinn.shop.user.entity.UserIdentity;
import com.kinn.shop.user.mapper.UserAddressMapper;
import com.kinn.shop.user.mapper.UserIdentityMapper;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

/**
 * 用户服务对内 RPC：订单结算/下单取地址与清关实名快照。
 * 查无一律返回 null，由消费方决定业务语义。
 */
@DubboService
@RequiredArgsConstructor
public class UserFacadeImpl implements UserFacade {

    private final UserAddressMapper addressMapper;
    private final UserIdentityMapper identityMapper;

    @Override
    public AddressDTO getAddress(long userId, long addressId) {
        UserAddress address = addressMapper.selectOne(Wrappers.<UserAddress>lambdaQuery()
                .eq(UserAddress::getId, addressId)
                .eq(UserAddress::getUserId, userId));
        if (address == null) {
            return null;
        }
        AddressDTO dto = new AddressDTO();
        dto.setId(address.getId());
        dto.setReceiverName(address.getReceiverName());
        dto.setPhone(address.getPhone());
        dto.setCountryCode(address.getCountryCode());
        dto.setState(address.getState());
        dto.setCity(address.getCity());
        dto.setAddressLine1(address.getAddressLine1());
        dto.setAddressLine2(address.getAddressLine2());
        dto.setPostcode(address.getPostcode());
        return dto;
    }

    @Override
    public IdentityDTO getIdentity(long userId, Long identityId) {
        UserIdentity identity;
        if (identityId == null) {
            // 默认实名记录优先；没有默认则任取一条
            identity = identityMapper.selectOne(Wrappers.<UserIdentity>lambdaQuery()
                    .eq(UserIdentity::getUserId, userId)
                    .eq(UserIdentity::getIsDefault, 1)
                    .last("LIMIT 1"));
            if (identity == null) {
                identity = identityMapper.selectOne(Wrappers.<UserIdentity>lambdaQuery()
                        .eq(UserIdentity::getUserId, userId)
                        .orderByDesc(UserIdentity::getId)
                        .last("LIMIT 1"));
            }
        } else {
            identity = identityMapper.selectOne(Wrappers.<UserIdentity>lambdaQuery()
                    .eq(UserIdentity::getId, identityId)
                    .eq(UserIdentity::getUserId, userId));
        }
        if (identity == null) {
            return null;
        }
        // 只携带脱敏信息，身份证密文绝不出用户服务
        IdentityDTO dto = new IdentityDTO();
        dto.setId(identity.getId());
        dto.setRealName(identity.getRealName());
        dto.setIdCardMask(identity.getIdCardMask());
        return dto;
    }
}
