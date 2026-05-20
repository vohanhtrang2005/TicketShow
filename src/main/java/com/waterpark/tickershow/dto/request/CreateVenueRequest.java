package com.waterpark.tickershow.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateVenueRequest {

    @NotBlank(message = "Tên địa điểm không được để trống")
    @Size(max = 200, message = "Tên địa điểm tối đa 200 ký tự")
    private String name;

    @NotBlank(message = "Vị trí không được để trống")
    private String location;

    @Min(value = 1, message = "Sức chứa phải lớn hơn 0")
    private Integer capacity;

    private String description;
}
