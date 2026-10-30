package com.certverify.repository;

import com.certverify.entity.Role;
import com.certverify.entity.User;
import com.certverify.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByRole(Role role);

    long countByRole(Role role);

    long countByRoleAndStatus(Role role, UserStatus status);

    @Query("""
            select u from User u
            where u.role = :role
              and (:status is null or u.status = :status)
            order by u.createdAt desc
            """)
    Page<User> findByRoleAndOptionalStatus(@Param("role") Role role,
                                           @Param("status") UserStatus status,
                                           Pageable pageable);
}
