package com.waterpark.tickershow.controller;

import com.waterpark.tickershow.dto.request.LoginRequest;
import com.waterpark.tickershow.dto.request.RegisterRequest;
import com.waterpark.tickershow.dto.response.ApiResponse;
import com.waterpark.tickershow.dto.response.LoginResponse;
import com.waterpark.tickershow.dto.response.UserResponse;
import com.waterpark.tickershow.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", response));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        // JWT is stateless — client simply discards token
        return ResponseEntity.ok(ApiResponse.success("Đăng xuất thành công", null));
    }
}
