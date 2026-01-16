package com.example.demo.repository;

import com.example.demo.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {


    List<Event> findByLocationAndStartTimeLessThanAndEndTimeGreaterThan(
            String location, LocalDateTime end, LocalDateTime start);

    List<Event> findByLocationAndStartTimeLessThanAndEndTimeGreaterThanAndIdNot(
            String location, LocalDateTime end, LocalDateTime start, Long id);

    List<Event> findByLocationAndStartTimeBetween(
            String location, LocalDateTime dayStart, LocalDateTime dayEnd);

}


