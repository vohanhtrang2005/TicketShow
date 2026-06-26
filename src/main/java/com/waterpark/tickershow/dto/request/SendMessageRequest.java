package com.waterpark.tickershow.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendMessageRequest {

    @NotBlank(message = "Tin nhan khong duoc de trong")
    @Size(max = 2000, message = "Tin nhan qua dai")
    private String content;
}