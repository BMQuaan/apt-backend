package com.ptithcm.apt.controller;

import com.ptithcm.apt.dto.response.ApiResponse;
import com.ptithcm.apt.entity.Role;
import com.ptithcm.apt.entity.User;
import com.ptithcm.apt.exception.NotFoundException;
import com.ptithcm.apt.repository.RoleRepository;
import com.ptithcm.apt.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TestController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/public/register")
    public ResponseEntity<ApiResponse<Void>> registerUser(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Tên đăng nhập (username) đã tồn tại!");
        }

        Role defaultRole = roleRepository.findByRoleName("USER")
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Role: RESIDENT trong hệ thống. Vui lòng chạy script SQL."));

        User newUser = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(defaultRole)
                .isActive(true)
                .build();

        userRepository.save(newUser);

        return ResponseEntity.ok(ApiResponse.success(null, "Đăng ký tài khoản thành công!"));
    }

    @GetMapping("/hello")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> admin(){
        return ResponseEntity.ok(ApiResponse.success("admin", null));
    }

    /**
     */
    @Data
    public static class RegisterRequest {
        @NotBlank(message = "Username không được để trống")
        private String username;

        @NotBlank(message = "Password không được để trống")
        private String password;
    }

    /**
     */


}