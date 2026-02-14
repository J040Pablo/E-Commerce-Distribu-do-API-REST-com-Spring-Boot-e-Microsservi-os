package com.ecommerce.order_service.dto;

import lombok.Data;

@Data
public class CustomerResponse {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String address;
}
