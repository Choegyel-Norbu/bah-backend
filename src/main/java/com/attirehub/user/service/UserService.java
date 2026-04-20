package com.attirehub.user.service;

import com.attirehub.user.dto.UpdateProfileRequest;
import com.attirehub.user.dto.UserProfileResponse;

public interface UserService {

    UserProfileResponse getProfile(Long userId);

    UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request);

    void deleteAccount(Long userId);
}
