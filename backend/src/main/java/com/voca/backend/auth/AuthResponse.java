package com.voca.backend.auth;

import com.voca.backend.user.UserResponse;

public record AuthResponse(
        String token,
        String tokenType,
        UserResponse user
) {
}
