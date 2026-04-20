package com.attirehub.user.mapper;

import com.attirehub.user.dto.UserProfileResponse;
import com.attirehub.user.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserProfileResponse toProfileResponse(User user);
}
