package com.voca.backend.user;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PutMapping("/me")
    public UserResponse updateMe(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return UserResponse.from(userService.updateProfile(authentication, request));
    }
}
