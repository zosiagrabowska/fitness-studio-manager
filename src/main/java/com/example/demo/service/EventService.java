package com.example.demo.service;

import com.example.demo.entity.Event;
import com.example.demo.repository.EventRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.time.format.DateTimeFormatter;


@Service
public class EventService {

    private final EventRepository repository;
    private final EmailService emailService;

    public EventService(EventRepository repository, EmailService emailService) {
        this.repository = repository;
        this.emailService = emailService;
    }

    public List<Event> getAllEvents() {
        List<Event> list = repository.findAll();
        list.sort(Comparator.comparing(Event::getStartTime));
        return list;
    }

    public Optional<Event> getEvent(Long id) {
        return repository.findById(id);
    }

    public void deleteEvent(Long id) {
        Event event = repository.findById(id).orElse(null);
        if (event != null) {
            emailService.sendEventDeletedEmail(event);
            repository.deleteById(id);
        }
    }


    public List<Event> findConflicts(String location, LocalDateTime end, LocalDateTime start, Long Id) {
        if (Id == null) {
            return repository.findByLocationAndStartTimeLessThanAndEndTimeGreaterThan(location, end, start);
        } else {
            return repository.findByLocationAndStartTimeLessThanAndEndTimeGreaterThanAndIdNot(location, end, start, Id);
        }
    }


    public List<Event> getAllEventsInRoom(String location, LocalDateTime date) {
        LocalDateTime dayStart = date.toLocalDate().atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);

        return repository.findByLocationAndStartTimeBetween(location, dayStart, dayEnd).stream()
                .sorted(Comparator.comparing(Event::getStartTime))
                .toList();
    }

    public void saveOrUpdateEvent(Long id, String title, String location, int capacity,
                                  LocalDateTime start, LocalDateTime end) {

        if (end.isBefore(start) || end.equals(start))
            throw new IllegalArgumentException("End time must be after start time.");

        List<Event> conflicts = findConflicts(location, end, start, id);

        if (!conflicts.isEmpty()) {
            List<Event> allOnDay = getAllEventsInRoom(location, start);
            throw new IllegalStateException("Chosen room is occupied at these times:\n" +
                    mergeEventTimes(allOnDay) +
                    "\nChoose an available time slot.");
        }

        Event event;
        if (id == null) {
            event = new Event(title, location, capacity, start, end);
        } else {
            event = getEvent(id).orElseThrow(() -> new IllegalArgumentException("Event not found."));
            event.setTitle(title);
            event.setLocation(location);
            event.setCapacity(capacity);
            event.setStartTime(start);
            event.setEndTime(end);
            emailService.sendEventUpdatedEmail(event);
        }

        repository.save(event);
    }

    public List<Event> filterEvents(String title, String location, LocalDate date, String available) {
        List<Event> events = getAllEvents();

        if (title != null && !title.isBlank()) {
            events = events.stream().filter(e -> e.getTitle().toLowerCase().contains(title.toLowerCase())).toList();
        }
        if (location != null && !location.isBlank()) {
            events = events.stream().filter(e -> e.getLocation().equals(location)).toList();
        }
        if (date != null) {
            events = events.stream().filter(e -> e.getStartTime().toLocalDate().equals(date)).toList();
        }
        if (available != null && !available.isBlank()) {
            if (available.equals("true")) {
                events = events.stream().filter(e -> e.getRegistered() < e.getCapacity()).toList();
            } else if (available.equals("false")) {
                events = events.stream().filter(e -> e.getRegistered() >= e.getCapacity()).toList();
            }
        }

        return events;
    }

    public String mergeEventTimes(List<Event> events) {
        if (events.isEmpty()) return "";

        List<Event> sortedEvents = events.stream().sorted(Comparator.comparing(Event::getStartTime)).toList();
        List<String> mergedTimes = new ArrayList<>();

        LocalDateTime currentStart = sortedEvents.getFirst().getStartTime();
        LocalDateTime currentEnd = sortedEvents.getFirst().getEndTime();

        for (int i = 1; i < sortedEvents.size(); i++) {
            Event e = sortedEvents.get(i);
            if (!e.getStartTime().isAfter(currentEnd)) {
                currentEnd = e.getEndTime();
            } else {
                mergedTimes.add(formatTime(currentStart) + " - " + formatTime(currentEnd));
                currentStart = e.getStartTime();
                currentEnd = e.getEndTime();
            }
        }
        mergedTimes.add(formatTime(currentStart) + " - " + formatTime(currentEnd));

        return String.join("\n", mergedTimes);
    }

    private String formatTime(LocalDateTime time) {
        return time.format(DateTimeFormatter.ofPattern("HH:mm"));
    }


}
