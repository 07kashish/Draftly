package com.draftly.repository;

import com.draftly.entity.Draft;
import com.draftly.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DraftRepository extends JpaRepository<Draft, UUID> {
    List<Draft> findByUserOrderByCreatedAtDesc(User user);

    List<Draft> findByUserEmailOrderByCreatedAtDesc(String email);
}
