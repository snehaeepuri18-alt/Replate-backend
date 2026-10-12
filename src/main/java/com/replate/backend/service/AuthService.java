package com.replate.backend.service;

import com.replate.backend.dto.AuthResponse;
import com.replate.backend.dto.LoginRequest;
import com.replate.backend.dto.RegisterRequest;
import com.replate.backend.dto.UserResponse;
import com.replate.backend.entity.Restaurant;
import com.replate.backend.entity.User;
import com.replate.backend.enums.Role;
import com.replate.backend.exception.ApiException;
import com.replate.backend.repository.RestaurantRepository;
import com.replate.backend.repository.UserRepository;
import com.replate.backend.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       RestaurantRepository restaurantRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.restaurantRepository = restaurantRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "This email is already registered");
        }

        if (req.role() == Role.RESTAURANT) {
            if (isBlank(req.restaurantName())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Enter your restaurant name");
            }
            if (req.phone() == null || !req.phone().matches("\\d{10}")) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Enter a 10-digit phone number");
            }
            if (isBlank(req.address())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Enter your restaurant address");
            }
        }

        User user = new User();
        user.setName(req.name().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(req.password()));   // hashed here
        user.setRole(req.role());
        userRepository.save(user);

        if (req.role() == Role.RESTAURANT) {
            Restaurant restaurant = new Restaurant();
            restaurant.setUser(user);
            restaurant.setRestaurantName(req.restaurantName().trim());
            restaurant.setPhone(req.phone());
            restaurant.setAddress(req.address().trim());
            restaurantRepository.save(restaurant);
        }

        return new AuthResponse(jwtService.generateToken(user), toUserResponse(user));
    }

    public AuthResponse login(LoginRequest req) {
        String email = req.email().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Wrong email or password"));

        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Wrong email or password");
        }

        return new AuthResponse(jwtService.generateToken(user), toUserResponse(user));
    }

    public UserResponse toUserResponse(User user) {
        String restaurantName = null;
        if (user.getRole() == Role.RESTAURANT) {
            restaurantName = restaurantRepository.findByUserId(user.getId())
                    .map(Restaurant::getRestaurantName)
                    .orElse(null);
        }
        return new UserResponse(user.getId(), user.getName(), user.getEmail(),
                user.getRole(), restaurantName);
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}