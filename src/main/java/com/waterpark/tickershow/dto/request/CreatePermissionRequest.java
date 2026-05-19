package com.waterpark.tickershow.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreatePermissionRequest {

    @NotBlank(message = "Tên permission không được để trống")
    @Size(max = 100, message = "Tên permission tối đa 100 ký tự")
    private String name;

    private String description;
}
