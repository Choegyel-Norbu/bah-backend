package com.attirehub.user.service;

import com.attirehub.shared.exception.ResourceNotFoundException;
import com.attirehub.user.dto.AddressRequest;
import com.attirehub.user.dto.AddressResponse;
import com.attirehub.user.entity.Address;
import com.attirehub.user.entity.User;
import com.attirehub.user.mapper.AddressMapper;
import com.attirehub.user.repository.AddressRepository;
import com.attirehub.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private static final Logger log = LoggerFactory.getLogger(AddressServiceImpl.class);

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final AddressMapper addressMapper;

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getUserAddresses(Long userId) {
        return addressRepository.findByUserId(userId)
                .stream()
                .map(addressMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public AddressResponse createAddress(Long userId, AddressRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Address address = addressMapper.toEntity(request);
        address.setUser(user);

        // If this is the first address or marked as default, handle default logic
        if (request.isDefault()) {
            addressRepository.unsetDefaultAddresses(userId);
        }

        Address savedAddress = addressRepository.save(address);
        log.info("Address created: addressId={}, userId={}", savedAddress.getId(), userId);
        return addressMapper.toResponse(savedAddress);
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(Long userId, Long addressId, AddressRequest request) {
        Address address = findUserAddress(userId, addressId);

        addressMapper.updateEntity(request, address);

        if (request.isDefault()) {
            addressRepository.unsetDefaultAddresses(userId);
            address.setDefault(true);
        }

        Address updatedAddress = addressRepository.save(address);
        log.info("Address updated: addressId={}, userId={}", addressId, userId);
        return addressMapper.toResponse(updatedAddress);
    }

    @Override
    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        Address address = findUserAddress(userId, addressId);
        addressRepository.delete(address);
        log.info("Address deleted: addressId={}, userId={}", addressId, userId);
    }

    @Override
    @Transactional
    public AddressResponse setDefaultAddress(Long userId, Long addressId) {
        Address address = findUserAddress(userId, addressId);

        // Unset all current defaults, then set this one
        addressRepository.unsetDefaultAddresses(userId);
        address.setDefault(true);

        Address updatedAddress = addressRepository.save(address);
        log.info("Default address set: addressId={}, userId={}", addressId, userId);
        return addressMapper.toResponse(updatedAddress);
    }

    private Address findUserAddress(Long userId, Long addressId) {
        return addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));
    }
}
