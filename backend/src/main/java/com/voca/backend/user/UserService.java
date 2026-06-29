package com.voca.backend.user;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public User currentUser(Authentication authentication) {
        return userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }

    @Transactional
    public User updateProfile(Authentication authentication, UpdateProfileRequest request) {
        User user = currentUser(authentication);
        user.setDisplayName(request.displayName().trim());
        user.setEnglishLevel(request.englishLevel());
        user.setLearningGoal(request.learningGoal() == null ? null : request.learningGoal().trim());
        user.setDailyGoal(request.dailyGoal());
        return user;
    }
}
