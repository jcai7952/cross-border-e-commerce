package com.kinn.shop.api.user.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class AddressDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String receiverName;
    private String phone;
    /** ISO 3166-1 alpha-2 */
    private String countryCode;
    private String state;
    private String city;
    private String addressLine1;
    private String addressLine2;
    private String postcode;
}
