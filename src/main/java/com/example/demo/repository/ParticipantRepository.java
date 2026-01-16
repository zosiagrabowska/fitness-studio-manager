package com.example.demo.repository;

import com.example.demo.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ParticipantRepository extends JpaRepository<Participant, Long> {

    boolean existsByEmailAndEventId(String email, Long eventId);
    Optional<Participant> findByEmailAndEventId(String email, Long eventId);
}
