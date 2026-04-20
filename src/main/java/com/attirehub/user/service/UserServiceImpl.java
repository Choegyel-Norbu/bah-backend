package com.attirehub.user.service;

import com.attirehub.shared.exception.ResourceNotFoundException;
import com.attirehub.user.dto.UpdateProfileRequest;
import com.attirehub.user.dto.UserProfileResponse;
import com.attirehub.user.entity.User;
import com.attirehub.user.mapper.UserMapper;
import com.attirehub.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        User user = findUserById(userId);
        return userMapper.toProfileResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findUserById(userId);

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }

        User updatedUser = userRepository.save(user);
        log.info("User profile updated: userId={}", userId);
        return userMapper.toProfileResponse(updatedUser);
    }

    @Override
    @Transactional
    public void deleteAccount(Long userId) {
        User user = findUserById(userId);
        // Soft delete — deactivate account instead of removing data
        user.setActive(false);
        userRepository.save(user);
        log.info("User account deactivated (soft delete): userId={}", userId);
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }
}
