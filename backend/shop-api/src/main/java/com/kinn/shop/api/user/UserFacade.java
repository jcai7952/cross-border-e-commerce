package com.kinn.shop.api.user;

import com.kinn.shop.api.user.dto.AddressDTO;
import com.kinn.shop.api.user.dto.IdentityDTO;

/**
 * 用户服务对内 RPC：订单结算/下单时取地址与清关实名快照。
 * 查无返回 null，由消费方决定业务语义。
 */
public interface UserFacade {

    AddressDTO getAddress(long userId, long addressId);

    /** identityId 为 null 时取该用户默认实名记录。 */
    IdentityDTO getIdentity(long userId, Long identityId);
}
