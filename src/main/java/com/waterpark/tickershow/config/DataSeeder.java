package com.waterpark.tickershow.config;

import com.waterpark.tickershow.entity.*;
import com.waterpark.tickershow.enums.RoleName;
import com.waterpark.tickershow.enums.ShowTypeName;
import com.waterpark.tickershow.enums.UserStatus;
import com.waterpark.tickershow.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final ShowTypeRepository showTypeRepository;
    private final VenueRepository venueRepository;
    private final ZoneRepository zoneRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        seedPermissions();
        seedRoles();
        seedAdminUser();
        seedDemoUsers();
        seedShowTypes();
        seedVenuesAndZones();
        log.info("✅ DataSeeder completed.");
    }

    // ─── Permissions ──────────────────────────────────────────────────────────

    private void seedPermissions() {
        List<String[]> permissions = Arrays.asList(
                new String[]{"CREATE_SHOW",             "Tạo show mới"},
                new String[]{"UPDATE_SHOW",             "Cập nhật show"},
                new String[]{"APPROVE_SHOW",            "Phê duyệt show"},
                new String[]{"PUBLISH_SHOW",            "Xuất bản show"},
                new String[]{"DELETE_SHOW",             "Xóa show"},
                new String[]{"CREATE_SCHEDULE",         "Tạo lịch trình"},
                new String[]{"UPDATE_SCHEDULE",         "Cập nhật lịch trình"},
                new String[]{"APPROVE_SCHEDULE",        "Phê duyệt lịch trình"},
                new String[]{"MANAGE_VENUE",            "Quản lý địa điểm"},
                new String[]{"MANAGE_ZONE",             "Quản lý khu vực"},
                new String[]{"CREATE_BOOKING",          "Tạo đặt vé"},
                new String[]{"VIEW_BOOKING",            "Xem đặt vé"},
                new String[]{"MANAGE_PAYMENT",          "Quản lý thanh toán"},
                new String[]{"ASSIGN_STAFF",            "Phân công nhân viên"},
                new String[]{"APPROVE_ASSIGNMENT",      "Phê duyệt phân công"},
                new String[]{"VIEW_ASSIGNED_SCHEDULE",  "Xem lịch được phân công"},
                new String[]{"SCAN_QR",                 "Quét mã QR check-in"},
                new String[]{"VIEW_REPORT",             "Xem báo cáo"},
                new String[]{"MANAGE_USER",             "Quản lý người dùng"}
        );

        for (String[] p : permissions) {
            if (!permissionRepository.existsByName(p[0])) {
                permissionRepository.save(Permission.builder()
                        .name(p[0]).description(p[1]).build());
                log.info("  Seeded permission: {}", p[0]);
            }
        }
    }

    // ─── Roles ────────────────────────────────────────────────────────────────

    private void seedRoles() {
        seedRole(RoleName.CUSTOMER, "Khách hàng",
                Set.of("CREATE_BOOKING", "VIEW_BOOKING"));

        seedRole(RoleName.STAFF, "Nhân viên vận hành",
                Set.of("VIEW_ASSIGNED_SCHEDULE", "SCAN_QR", "VIEW_BOOKING"));

        seedRole(RoleName.OPERATOR, "Điều hành viên",
                Set.of("CREATE_SHOW", "UPDATE_SHOW", "CREATE_SCHEDULE", "UPDATE_SCHEDULE",
                        "ASSIGN_STAFF", "VIEW_BOOKING"));

        seedRole(RoleName.MANAGER, "Quản lý",
                Set.of("APPROVE_SHOW", "PUBLISH_SHOW", "DELETE_SHOW",
                        "APPROVE_SCHEDULE", "MANAGE_VENUE", "MANAGE_ZONE",
                        "APPROVE_ASSIGNMENT", "MANAGE_PAYMENT",
                        "VIEW_REPORT", "MANAGE_USER", "VIEW_BOOKING"));

        seedRole(RoleName.ADMIN, "Quản trị hệ thống",
                Set.of("CREATE_SHOW", "UPDATE_SHOW", "APPROVE_SHOW", "PUBLISH_SHOW", "DELETE_SHOW",
                        "CREATE_SCHEDULE", "UPDATE_SCHEDULE", "APPROVE_SCHEDULE",
                        "MANAGE_VENUE", "MANAGE_ZONE", "ASSIGN_STAFF", "APPROVE_ASSIGNMENT",
                        "MANAGE_PAYMENT", "VIEW_REPORT", "MANAGE_USER", "SCAN_QR",
                        "VIEW_ASSIGNED_SCHEDULE", "CREATE_BOOKING", "VIEW_BOOKING"));
    }

    private void seedRole(RoleName roleName, String description, Set<String> permissionNames) {
        if (roleRepository.existsByName(roleName)) return;

        Set<Permission> permissions = new HashSet<>();
        for (String name : permissionNames) {
            permissionRepository.findByName(name).ifPresent(permissions::add);
        }

        roleRepository.save(Role.builder()
                .name(roleName).description(description).permissions(permissions).build());
        log.info("  Seeded role: {} with {} permissions", roleName, permissions.size());
    }

    // ─── Users ────────────────────────────────────────────────────────────────

    private void seedAdminUser() {
        seedUser("admin@waterpark.com", "System Admin", "0900000000", "admin123", RoleName.ADMIN);
    }

    private void seedDemoUsers() {
        seedUser("manager@waterpark.com",  "Demo Manager",  "0911111111", "manager123",  RoleName.MANAGER);
        seedUser("operator@waterpark.com", "Demo Operator", "0922222222", "operator123", RoleName.OPERATOR);
        seedUser("staff@waterpark.com",    "Demo Staff",    "0933333333", "staff123",    RoleName.STAFF);
        seedUser("customer@waterpark.com", "Demo Customer", "0944444444", "customer123", RoleName.CUSTOMER);
    }

    private void seedUser(String email, String fullName, String phone, String password, RoleName roleName) {
        if (userRepository.existsByEmail(email)) return;

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException(roleName + " role not found"));

        userRepository.save(User.builder()
                .fullName(fullName).email(email)
                .password(passwordEncoder.encode(password))
                .phone(phone).role(role).status(UserStatus.ACTIVE).build());
        log.info("  Seeded user: {} ({})", email, roleName);
    }

    // ─── Show Types ───────────────────────────────────────────────────────────

    private void seedShowTypes() {
        for (ShowTypeName typeName : ShowTypeName.values()) {
            if (showTypeRepository.findByName(typeName).isEmpty()) {
                String description = switch (typeName) {
                    case FREE_NO_REGISTRATION ->
                            "Miễn phí - Không cần đăng ký (thường ở ngoài trời, sức chứa không giới hạn)";
                    case FREE_WITH_REGISTRATION ->
                            "Miễn phí - Cần đăng ký trước (địa điểm giới hạn số lượng)";
                    case PAID_WITH_REGISTRATION ->
                            "Trả phí - Cần đặt vé và thanh toán";
                };
                showTypeRepository.save(ShowType.builder()
                        .name(typeName).description(description).build());
                log.info("  Seeded show type: {}", typeName);
            }
        }
    }

    // ─── Venues & Zones ───────────────────────────────────────────────────────

    private void seedVenuesAndZones() {
        if (!venueRepository.findAll().isEmpty()) return;

        // Venue 1: Sân khấu ngoài trời (unlimited-style)
        Venue outdoor = venueRepository.save(Venue.builder()
                .name("Sân khấu ngoài trời")
                .location("Khu vực trung tâm công viên")
                .description("Sân khấu lộ thiên rộng rãi, phù hợp show miễn phí không cần đăng ký")
                .capacity(2000)
                .active(true)
                .build());
        seedZones(outdoor,
                new String[]{"Khu A",   "1000",  "0"},
                new String[]{"Khu B",   "700",   "0"},
                new String[]{"Khu C",   "300",   "0"}
        );
        log.info("  Seeded venue: {}", outdoor.getName());

        // Venue 2: Nhà hát bể bơi
        Venue poolTheater = venueRepository.save(Venue.builder()
                .name("Nhà hát bể bơi")
                .location("Khu B - gần hồ sóng")
                .description("Nhà hát với bể bơi trung tâm, phù hợp show biểu diễn dưới nước")
                .capacity(800)
                .active(true)
                .build());
        seedZones(poolTheater,
                new String[]{"Khu A",   "200",  "150000"},
                new String[]{"Khu B",   "300",  "100000"},
                new String[]{"Khu C",   "200",  "80000"},
                new String[]{"Khu VIP", "100",  "350000"}
        );
        log.info("  Seeded venue: {}", poolTheater.getName());

        // Venue 3: Rạp chiếu ngoài trời
        Venue amphitheater = venueRepository.save(Venue.builder()
                .name("Khán đài vòng cung")
                .location("Khu C - khu giải trí gia đình")
                .description("Khán đài kiểu vòng cung có mái che một phần, phù hợp mọi loại show")
                .capacity(500)
                .active(true)
                .build());
        seedZones(amphitheater,
                new String[]{"Khu A",   "150",  "120000"},
                new String[]{"Khu B",   "200",  "80000"},
                new String[]{"Khu C",   "100",  "60000"},
                new String[]{"Khu VIP", "50",   "300000"}
        );
        log.info("  Seeded venue: {}", amphitheater.getName());

        // Venue 4: Studio nội thất
        Venue studio = venueRepository.save(Venue.builder()
                .name("Studio biểu diễn")
                .location("Khu D - trung tâm giải trí")
                .description("Phòng studio nội thất cao cấp cho show nhỏ và chuyên nghiệp")
                .capacity(200)
                .active(true)
                .build());
        seedZones(studio,
                new String[]{"Khu A",   "50",   "200000"},
                new String[]{"Khu B",   "80",   "150000"},
                new String[]{"Khu VIP", "30",   "500000"},
                new String[]{"Khu C",   "40",   "100000"}
        );
        log.info("  Seeded venue: {}", studio.getName());
    }

    private void seedZones(Venue venue, String[]... zoneDefs) {
        for (String[] def : zoneDefs) {
            String name = def[0];
            int capacity = Integer.parseInt(def[1]);
            BigDecimal price = new BigDecimal(def[2]);

            Zone zone = Zone.builder()
                    .venue(venue)
                    .name(name)
                    .capacity(capacity)
                    .defaultPrice(price)
                    .description(name + " - " + venue.getName())
                    .build();
            zoneRepository.save(zone);
        }
    }
}
