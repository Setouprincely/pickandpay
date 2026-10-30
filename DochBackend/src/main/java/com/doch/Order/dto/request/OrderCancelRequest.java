package com.doch.Order.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OrderCancelRequest {
    @Size(max = 500)
    private String reason;
}