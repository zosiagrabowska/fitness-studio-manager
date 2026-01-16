package com.example.demo;

import com.example.demo.entity.Event;
import com.example.demo.repository.EventRepository;
import com.example.demo.service.EmailService;
import com.example.demo.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EventServiceUnitTest {

    @Mock
    private EventRepository repository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private EventService service;

    private Event e1;
    private Event e2;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        e1 = new Event("Yoga", "1.1", 10,
                LocalDateTime.of(2025, 9, 12, 10, 0),
                LocalDateTime.of(2025, 9, 12, 11, 0));

        e2 = new Event("Pilates", "1.1", 15,
                LocalDateTime.of(2025, 9, 12, 11, 0),
                LocalDateTime.of(2025, 9, 12, 12, 0));
    }

    @Test
    void getAllEventsTest() {
        when(repository.findAll()).thenReturn(Arrays.asList(e2, e1));

        List<Event> result = service.getAllEvents();

        assertEquals(2, result.size());
        assertEquals("Yoga", result.getFirst().getTitle());
        verify(repository).findAll();
    }

    @Test
    void getEventTest() {
        when(repository.findById(1L)).thenReturn(Optional.of(e1));

        Optional<Event> result = service.getEvent(1L);

        assertTrue(result.isPresent());
        assertEquals("Yoga", result.get().getTitle());
    }

    @Test
    void deleteEventTest() {
        when(repository.findById(1L)).thenReturn(Optional.of(e1));

        service.deleteEvent(1L);

        verify(emailService).sendEventDeletedEmail(e1);
        verify(repository).deleteById(1L);
    }

    @Test
    void findConflictsTest() {
        when(repository.findByLocationAndStartTimeLessThanAndEndTimeGreaterThan(
                eq("1.1"), any(), any())).thenReturn(Collections.singletonList(e1));

        List<Event> conflicts = service.findConflicts("1.1", e1.getEndTime(), e1.getStartTime(), null);

        assertEquals(1, conflicts.size());
    }

    @Test
    void getAllEventsInRoomTest() {
        when(repository.findByLocationAndStartTimeBetween(eq("1.1"), any(), any()))
                .thenReturn(Arrays.asList(e2, e1));

        List<Event> result = service.getAllEventsInRoom("1.1", e1.getStartTime());

        assertEquals(2, result.size());
        assertEquals("Yoga", result.getFirst().getTitle());
    }

    @Test
    void saveOrUpdateEventCreateTest() {
        LocalDateTime start = LocalDateTime.of(2025, 9, 12, 9, 0);
        LocalDateTime end = LocalDateTime.of(2025, 9, 12, 10, 0);

        service.saveOrUpdateEvent(null, "HIIT", "1.2", 20, start, end);

        verify(repository).save(any(Event.class));
    }

    @Test
    void saveOrUpdateEventUpdateTest() {
        when(repository.findById(1L)).thenReturn(Optional.of(e1));

        LocalDateTime newStart = LocalDateTime.of(2025, 9, 12, 12, 0);
        LocalDateTime newEnd = LocalDateTime.of(2025, 9, 12, 13, 0);

        service.saveOrUpdateEvent(1L, "Yoga Advanced", "1.1", 12, newStart, newEnd);

        verify(emailService).sendEventUpdatedEmail(e1);
        verify(repository).save(e1);
        assertEquals("Yoga Advanced", e1.getTitle());
        assertEquals(12, e1.getCapacity());
    }

    @Test
    void saveOrUpdateEventWringTimeTest() {
        LocalDateTime start = LocalDateTime.of(2025, 9, 12, 12, 0);
        LocalDateTime end = LocalDateTime.of(2025, 9, 12, 11, 0);

        Exception ex = assertThrows(IllegalArgumentException.class,
                () -> service.saveOrUpdateEvent(null, "Yoga", "1.1", 10, start, end));

        assertEquals("End time must be after start time.", ex.getMessage());
    }

    @Test
    void filterEventsTest() {
        when(repository.findAll()).thenReturn(Arrays.asList(e1, e2));

        List<Event> filtered = service.filterEvents("Yoga", null, null, null);
        assertEquals(1, filtered.size());
        assertEquals("Yoga", filtered.getFirst().getTitle());
    }

    @Test
    void mergeEventTimesTest() {
        List<Event> events = Arrays.asList(e1, e2);

        String result = service.mergeEventTimes(events);
        assertTrue(result.contains("10:00 - 12:00"));
    }

}
