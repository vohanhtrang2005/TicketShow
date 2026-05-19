package com.waterpark.tickershow.service;

import com.waterpark.tickershow.dto.request.CreateRoleRequest;
import com.waterpark.tickershow.dto.response.PermissionResponse;
import com.waterpark.tickershow.dto.response.RoleResponse;
import com.waterpark.tickershow.entity.Permission;
import com.waterpark.tickershow.entity.Role;
import com.waterpark.tickershow.enums.RoleName;
import com.waterpark.tickershow.exception.BusinessRuleException;
import com.waterpark.tickershow.exception.ConflictException;
import com.waterpark.tickershow.exception.ResourceNotFoundException;
import com.waterpark.tickershow.repository.PermissionRepository;
import com.waterpark.tickershow.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public RoleResponse getRoleById(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", id));
        return toResponse(role);
    }

    @Transactional
    public RoleResponse createRole(CreateRoleRequest request) {
        RoleName roleName;
        try {
            roleName = RoleName.valueOf(request.getName().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("Tên role không hợp lệ: " + request.getName());
        }

        if (roleRepository.existsByName(roleName)) {
            throw new ConflictException("Role đã tồn tại: " + roleName);
        }

        Set<Permission> permissions = new HashSet<>();
        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            permissions = new HashSet<>(permissionRepository.findAllById(request.getPermissionIds()));
            if (permissions.size() != request.getPermissionIds().size()) {
                throw new ResourceNotFoundException("Một hoặc nhiều Permission ID không tồn tại");
            }
        }

        Role role = Role.builder()
                .name(roleName)
                .description(request.getDescription())
                .permissions(permissions)
                .build();

        return toResponse(roleRepository.save(role));
    }

    @Transactional
    public RoleResponse updateRole(Long id, CreateRoleRequest request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", id));

        if (request.getDescription() != null) {
            role.setDescription(request.getDescription());
        }

        if (request.getPermissionIds() != null) {
            Set<Permission> permissions = new HashSet<>(permissionRepository.findAllById(request.getPermissionIds()));
            role.setPermissions(permissions);
        }

        return toResponse(roleRepository.save(role));
    }

    @Transactional
    public void deleteRole(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", id));

        if (!role.getUsers().isEmpty()) {
            throw new BusinessRuleException("Không thể xóa role đang có user sử dụng");
        }

        roleRepository.delete(role);
    }

    private RoleResponse toResponse(Role role) {
        List<PermissionResponse> permissions = role.getPermissions().stream()
                .map(p -> PermissionResponse.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .description(p.getDescription())
                        .build())
                .collect(Collectors.toList());

        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName().name())
                .description(role.getDescription())
                .permissions(permissions)
                .build();
    }
}
