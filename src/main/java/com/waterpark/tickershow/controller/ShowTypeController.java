package com.waterpark.tickershow.controller;

import com.waterpark.tickershow.dto.response.ApiResponse;
import com.waterpark.tickershow.entity.ShowType;
import com.waterpark.tickershow.repository.ShowTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/show-types")
@RequiredArgsConstructor
public class ShowTypeController {

    private final ShowTypeRepository showTypeRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ShowType>>> getShowTypes() {
        return ResponseEntity.ok(ApiResponse.success(showTypeRepository.findAll()));
    }
}
