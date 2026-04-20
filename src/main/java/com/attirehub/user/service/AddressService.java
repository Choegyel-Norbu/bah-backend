package com.attirehub.user.service;

import com.attirehub.user.dto.AddressRequest;
import com.attirehub.user.dto.AddressResponse;

import java.util.List;

public interface AddressService {

    List<AddressResponse> getUserAddresses(Long userId);

    AddressResponse createAddress(Long userId, AddressRequest request);

    AddressResponse updateAddress(Long userId, Long addressId, AddressRequest request);

    void deleteAddress(Long userId, Long addressId);

    AddressResponse setDefaultAddress(Long userId, Long addressId);
}
