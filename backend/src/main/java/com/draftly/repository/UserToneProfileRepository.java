package com.draftly.repository;

import com.draftly.entity.UserToneProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface UserToneProfileRepository extends JpaRepository<UserToneProfile, UUID> {
    @Query("""
            select profile
            from UserToneProfile profile
            left join profile.user user
            where profile.userEmail = :userEmail or user.email = :userEmail
            """)
    Optional<UserToneProfile> findByUserEmail(String userEmail);

    @Modifying
    @Query("""
            delete from UserToneProfile profile
            where profile.userEmail = :userEmail
               or profile.user.id in (
                    select user.id
                    from User user
                    where user.email = :userEmail
               )
            """)
    void deleteByUserEmail(String userEmail);
}
