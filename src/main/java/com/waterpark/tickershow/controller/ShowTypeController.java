package com.waterpark.tickershow.controller;

import com.waterpark.tickershow.dto.response.ApiResponse;
import com.waterpark.tickershow.entity.ShowType;
import com.waterpark.tickershow.repository.ShowTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/show-types")
@RequiredArgsConstructor
public class ShowTypeController {

    private final ShowTypeRepository showTypeRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ShowType>>> getAllShowTypes() {
        return ResponseEntity.ok(ApiResponse.success(showTypeRepository.findAll()));
    }
}
