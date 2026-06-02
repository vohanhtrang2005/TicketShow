package com.waterpark.tickershow.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.waterpark.tickershow.dto.request.SepayWebhookRequest;

import java.util.Map;
@RestController
@RequestMapping("/webhooks/sepay")
public class SepayWebhookController {
     @PostMapping
     
    public ResponseEntity<Map<String, Object>> receiveWebhook(@RequestBody SepayWebhookRequest request) {


        System.out.println("SePay webhook payload: " + request);
    System.out.println("SePay content: " + request.getContent());
    System.out.println("SePay amount: " + request.getTransferAmount());
    System.out.println("SePay type: " + request.getTransferType());
    System.out.println("SePay referenceCode: " + request.getReferenceCode());

    return ResponseEntity.ok(Map.of("success", true));
}
    }

