package com.waterpark.tickershow.config;

import com.waterpark.tickershow.entity.User;
import com.waterpark.tickershow.enums.UserStatus;
import com.waterpark.tickershow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User không tồn tại với email: " + email));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UsernameNotFoundException("Tài khoản đã bị khóa hoặc vô hiệu hóa");
        }

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        // Role-based authority: ROLE_MANAGER, ROLE_STAFF, etc.
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().getName().name()));

        // Permission-based authorities
        user.getRole().getPermissions().forEach(permission ->
                authorities.add(new SimpleGrantedAuthority(permission.getName()))
        );

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                authorities
        );
    }
}
