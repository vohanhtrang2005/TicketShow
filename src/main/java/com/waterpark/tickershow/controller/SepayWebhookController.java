package com.waterpark.tickershow.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.waterpark.tickershow.dto.request.SepayWebhookRequest;
import com.waterpark.tickershow.dto.response.ApiResponse;
import com.waterpark.tickershow.service.SepayWebhookService;


@RestController
@RequestMapping("/webhooks/sepay")
public class SepayWebhookController {
    
     private final SepayWebhookService sepayWebhookService;
    
    public SepayWebhookController(SepayWebhookService sepayWebhookService) {
        this.sepayWebhookService = sepayWebhookService;
    }

@PostMapping
public ResponseEntity<ApiResponse<Void>> receiveWebhook(@RequestBody SepayWebhookRequest request) {
    ApiResponse<Void> result = sepayWebhookService.handleWebhook(request);

    if (result.getStatusCode() == 200) {
        return ResponseEntity.ok(result);
    }

    return ResponseEntity.status(result.getStatusCode()).body(result);
}
    }

