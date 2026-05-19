package com.waterpark.tickershow.config;

import com.waterpark.tickershow.entity.Permission;
import com.waterpark.tickershow.entity.Role;
import com.waterpark.tickershow.entity.ShowType;
import com.waterpark.tickershow.entity.User;
import com.waterpark.tickershow.enums.RoleName;
import com.waterpark.tickershow.enums.ShowTypeName;
import com.waterpark.tickershow.enums.UserStatus;
import com.waterpark.tickershow.repository.PermissionRepository;
import com.waterpark.tickershow.repository.RoleRepository;
import com.waterpark.tickershow.repository.ShowTypeRepository;
import com.waterpark.tickershow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final ShowTypeRepository showTypeRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        seedPermissions();
        seedRoles();
        seedAdminUser();
        seedShowTypes();
        log.info("✅ DataSeeder completed.");
    }

    private void seedPermissions() {
        List<String[]> permissions = Arrays.asList(
                new String[]{"CREATE_SHOW", "Tạo show mới"},
                new String[]{"UPDATE_SHOW", "Cập nhật show"},
                new String[]{"APPROVE_SHOW", "Phê duyệt show"},
                new String[]{"PUBLISH_SHOW", "Xuất bản show"},
                new String[]{"DELETE_SHOW", "Xóa show"},
                new String[]{"CREATE_SCHEDULE", "Tạo lịch trình"},
                new String[]{"UPDATE_SCHEDULE", "Cập nhật lịch trình"},
                new String[]{"APPROVE_SCHEDULE", "Phê duyệt lịch trình"},
                new String[]{"MANAGE_VENUE", "Quản lý địa điểm"},
                new String[]{"MANAGE_ZONE", "Quản lý khu vực"},
                new String[]{"CREATE_BOOKING", "Tạo đặt vé"},
                new String[]{"VIEW_BOOKING", "Xem đặt vé"},
                new String[]{"MANAGE_PAYMENT", "Quản lý thanh toán"},
                new String[]{"ASSIGN_STAFF", "Phân công nhân viên"},
                new String[]{"APPROVE_ASSIGNMENT", "Phê duyệt phân công"},
                new String[]{"VIEW_ASSIGNED_SCHEDULE", "Xem lịch được phân công"},
                new String[]{"SCAN_QR", "Quét mã QR check-in"},
                new String[]{"VIEW_REPORT", "Xem báo cáo"},
                new String[]{"MANAGE_USER", "Quản lý người dùng"}
        );

        for (String[] p : permissions) {
            if (!permissionRepository.existsByName(p[0])) {
                permissionRepository.save(Permission.builder()
                        .name(p[0])
                        .description(p[1])
                        .build());
                log.info("  Seeded permission: {}", p[0]);
            }
        }
    }

    private void seedRoles() {
        // CUSTOMER
        seedRole(RoleName.CUSTOMER, "Khách hàng",
                Set.of("CREATE_BOOKING", "VIEW_BOOKING"));

        // STAFF
        seedRole(RoleName.STAFF, "Nhân viên vận hành",
                Set.of("VIEW_ASSIGNED_SCHEDULE", "SCAN_QR", "VIEW_BOOKING"));

        // OPERATOR
        seedRole(RoleName.OPERATOR, "Điều hành viên",
                Set.of("CREATE_SHOW", "UPDATE_SHOW", "CREATE_SCHEDULE", "UPDATE_SCHEDULE",
                        "ASSIGN_STAFF", "VIEW_BOOKING"));

        // MANAGER
        seedRole(RoleName.MANAGER, "Quản lý",
                Set.of("APPROVE_SHOW", "PUBLISH_SHOW", "DELETE_SHOW",
                        "APPROVE_SCHEDULE", "MANAGE_VENUE", "MANAGE_ZONE",
                        "APPROVE_ASSIGNMENT", "MANAGE_PAYMENT",
                        "VIEW_REPORT", "MANAGE_USER", "VIEW_BOOKING"));

        // ADMIN
        seedRole(RoleName.ADMIN, "Quản trị hệ thống",
                Set.of("CREATE_SHOW", "UPDATE_SHOW", "APPROVE_SHOW", "PUBLISH_SHOW", "DELETE_SHOW",
                        "CREATE_SCHEDULE", "UPDATE_SCHEDULE", "APPROVE_SCHEDULE",
                        "MANAGE_VENUE", "MANAGE_ZONE", "ASSIGN_STAFF", "APPROVE_ASSIGNMENT",
                        "MANAGE_PAYMENT", "VIEW_REPORT", "MANAGE_USER", "SCAN_QR",
                        "VIEW_ASSIGNED_SCHEDULE", "CREATE_BOOKING", "VIEW_BOOKING"));
    }

    private void seedRole(RoleName roleName, String description, Set<String> permissionNames) {
        if (roleRepository.existsByName(roleName)) {
            return;
        }

        Set<Permission> permissions = new HashSet<>();
        for (String name : permissionNames) {
            permissionRepository.findByName(name).ifPresent(permissions::add);
        }

        roleRepository.save(Role.builder()
                .name(roleName)
                .description(description)
                .permissions(permissions)
                .build());
        log.info("  Seeded role: {} with {} permissions", roleName, permissions.size());
    }

    private void seedAdminUser() {
        String adminEmail = "admin@waterpark.com";
        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }

        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseThrow(() -> new RuntimeException("ADMIN role not found after seeding"));

        userRepository.save(User.builder()
                .fullName("System Admin")
                .email(adminEmail)
                .password(passwordEncoder.encode("admin123"))
                .phone("0900000000")
                .role(adminRole)
                .status(UserStatus.ACTIVE)
                .build());
        log.info("  Seeded admin user: {}", adminEmail);
    }

    private void seedShowTypes() {
        for (ShowTypeName typeName : ShowTypeName.values()) {
            if (showTypeRepository.findByName(typeName).isEmpty()) {
                String description = switch (typeName) {
                    case FREE_NO_REGISTRATION -> "Miễn phí - Không cần đăng ký";
                    case FREE_WITH_REGISTRATION -> "Miễn phí - Cần đăng ký";
                    case PAID_WITH_REGISTRATION -> "Trả phí - Cần đặt vé và thanh toán";
                };
                showTypeRepository.save(ShowType.builder()
                        .name(typeName)
                        .description(description)
                        .build());
                log.info("  Seeded show type: {}", typeName);
            }
        }
    }
}
