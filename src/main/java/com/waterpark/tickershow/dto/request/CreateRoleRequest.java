package com.waterpark.tickershow.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoleRequest {

    @NotBlank(message = "Tên role không được để trống")
    @Size(max = 50, message = "Tên role tối đa 50 ký tự")
    private String name;

    private String description;

    private Set<Long> permissionIds;
}
