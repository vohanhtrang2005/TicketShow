package com.waterpark.tickershow.service;

import com.waterpark.tickershow.dto.request.CreatePermissionRequest;
import com.waterpark.tickershow.dto.response.PermissionResponse;
import com.waterpark.tickershow.entity.Permission;
import com.waterpark.tickershow.exception.BusinessRuleException;
import com.waterpark.tickershow.exception.ConflictException;
import com.waterpark.tickershow.exception.ResourceNotFoundException;
import com.waterpark.tickershow.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;

    public List<PermissionResponse> getAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public PermissionResponse createPermission(CreatePermissionRequest request) {
        if (permissionRepository.existsByName(request.getName())) {
            throw new ConflictException("Permission đã tồn tại: " + request.getName());
        }

        Permission permission = Permission.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();

        return toResponse(permissionRepository.save(permission));
    }

    @Transactional
    public void deletePermission(Long id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", id));

        if (!permission.getRoles().isEmpty()) {
            throw new BusinessRuleException("Không thể xóa permission đang được role sử dụng");
        }

        permissionRepository.delete(permission);
    }

    private PermissionResponse toResponse(Permission permission) {
        return PermissionResponse.builder()
                .id(permission.getId())
                .name(permission.getName())
                .description(permission.getDescription())
                .build();
    }
}
