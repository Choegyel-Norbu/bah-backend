package com.attirehub.user.repository;

import com.attirehub.shared.enums.Role;
import com.attirehub.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /**
     * Returns all users with the given role.
     */
    List<User> findByRole(Role role);
}
