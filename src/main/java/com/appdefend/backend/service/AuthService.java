package com.appdefend.backend.service;

import com.appdefend.backend.dto.AuthDtos.AuthResponse;
import com.appdefend.backend.dto.AuthDtos.CreateUserRequest;
import com.appdefend.backend.dto.AuthDtos.LoginRequest;
import com.appdefend.backend.dto.AuthDtos.UpdateUserRequest;
import com.appdefend.backend.dto.AuthDtos.UserSummary;
import com.appdefend.backend.exception.ApiException;
import com.appdefend.backend.model.PlatformUser;
import com.appdefend.backend.repository.UserRepository;
import com.appdefend.backend.security.AppUserDetails;
import com.appdefend.backend.security.JwtService;
import com.appdefend.backend.security.TokenStoreService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final TokenStoreService tokenStoreService;
    private final PasswordEncoder passwordEncoder;
    private final LicenseService licenseService;

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       JwtService jwtService,
                       TokenStoreService tokenStoreService,
                       PasswordEncoder passwordEncoder,
                       LicenseService licenseService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.tokenStoreService = tokenStoreService;
        this.passwordEncoder = passwordEncoder;
        this.licenseService = licenseService;
    }

    public AuthResponse login(LoginRequest request) {
        licenseService.assertLoginAllowed();
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        PlatformUser user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        return buildTokens(user);
    }

    public AuthResponse refresh(String refreshToken) {
        if (!tokenStoreService.isRefreshTokenValid(refreshToken)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token is invalid or expired");
        }
        String username = jwtService.extractUsername(refreshToken);
        PlatformUser user = userRepository.findByEmail(username)
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));
        tokenStoreService.revokeRefreshToken(refreshToken);
        return buildTokens(user);
    }

    public void logout(String accessToken, String refreshToken) {
        tokenStoreService.revokeAccessToken(accessToken);
        tokenStoreService.revokeRefreshToken(refreshToken);
    }

    public PlatformUser createUser(CreateUserRequest request) {
        if (request.enabled()) {
            licenseService.assertProvisioningAllowed(userRepository.countEnabledUsers() + 1);
        }
        Long userId = userRepository.create(
            request.fullName(),
            request.email(),
            passwordEncoder.encode(request.password()),
            request.enabled());
        List<Long> roleIds = request.roleIds() == null || request.roleIds().isEmpty() ? List.of(3L) : request.roleIds();
        for (Long roleId : roleIds) {
            userRepository.assignRole(userId, roleId);
        }
        return userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create user"));
    }

    public List<PlatformUser> findAllUsers() {
        return userRepository.findAll();
    }

    public PlatformUser updateUser(Long userId, UpdateUserRequest request) {
        PlatformUser existingUser = userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        int projectedEnabledUsers = userRepository.countEnabledUsers();
        if (!existingUser.enabled() && request.enabled()) {
            projectedEnabledUsers += 1;
        }
        if (existingUser.enabled() && !request.enabled()) {
            projectedEnabledUsers -= 1;
        }
        if (request.enabled()) {
            licenseService.assertProvisioningAllowed(projectedEnabledUsers);
        }
        userRepository.update(userId, request.fullName(), request.enabled());
        userRepository.clearRoles(userId);
        List<Long> roleIds = request.roleIds() == null || request.roleIds().isEmpty() ? List.of(3L) : request.roleIds();
        for (Long roleId : roleIds) {
            userRepository.assignRole(userId, roleId);
        }
        return userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public void deleteUser(Long userId) {
        userRepository.delete(userId);
    }

    private AuthResponse buildTokens(PlatformUser user) {
        AppUserDetails userDetails = new AppUserDetails(user);
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);
        tokenStoreService.storeRefreshToken(refreshToken);
        return new AuthResponse(
            accessToken,
            refreshToken,
            jwtService.getAccessTokenExpirySeconds(),
            toSummary(user)
        );
    }

    public UserSummary toSummary(PlatformUser user) {
        return new UserSummary(user.id(), user.fullName(), user.email(), user.roles(), user.permissions(), user.views());
    }
}
