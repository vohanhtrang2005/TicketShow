package com.waterpark.tickershow.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateZoneRequest {

    @NotBlank(message = "Tên khu vực không được để trống")
    @Size(max = 100, message = "Tên khu vực tối đa 100 ký tự")
    private String name;  // "Khu A", "Khu B", "Khu VIP", ...

    @Min(value = 1, message = "Sức chứa phải lớn hơn 0")
    private Integer capacity;

    // Giá mặc định của zone (có thể override per-schedule)
    @DecimalMin(value = "0.0", message = "Giá không được âm")
    private BigDecimal defaultPrice;

    private String description;
}
