package com.kinn.shop.user.vo;

import com.kinn.shop.user.entity.UserAddress;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "收货地址")
public class AddressVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String receiverName;
    private String phone;
    private String countryCode;
    private String state;
    private String city;
    private String addressLine1;
    private String addressLine2;
    private String postcode;
    private Integer isDefault;
    private LocalDateTime createTime;

    public static AddressVO from(UserAddress address) {
        AddressVO vo = new AddressVO();
        vo.setId(address.getId());
        vo.setReceiverName(address.getReceiverName());
        vo.setPhone(address.getPhone());
        vo.setCountryCode(address.getCountryCode());
        vo.setState(address.getState());
        vo.setCity(address.getCity());
        vo.setAddressLine1(address.getAddressLine1());
        vo.setAddressLine2(address.getAddressLine2());
        vo.setPostcode(address.getPostcode());
        vo.setIsDefault(address.getIsDefault());
        vo.setCreateTime(address.getCreateTime());
        return vo;
    }
}
