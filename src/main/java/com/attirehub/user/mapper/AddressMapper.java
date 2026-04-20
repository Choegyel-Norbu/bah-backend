package com.attirehub.user.mapper;

import com.attirehub.user.dto.AddressRequest;
import com.attirehub.user.dto.AddressResponse;
import com.attirehub.user.entity.Address;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface AddressMapper {

    @Mapping(target = "isDefault", source = "default")
    AddressResponse toResponse(Address address);

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "isDefault", source = "default")
    Address toEntity(AddressRequest request);

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(AddressRequest request, @MappingTarget Address address);
}
