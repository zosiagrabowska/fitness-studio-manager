// http://localhost:8080
// http://localhost:8080/participants


package com.example.demo.controller;

import com.example.demo.entity.Event;
import com.example.demo.service.EventService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Controller
public class EventController {

    private final EventService eventService;


    public EventController(EventService service) {
        this.eventService = service;
    }

    @GetMapping("/new-event")
    public String newEvent(Model model) {
        model.addAttribute("event", new Event());
        return "new-event";
    }

    @PostMapping("/new-event")
    public String createEvent(
            @RequestParam String title,
            @RequestParam String location,
            @RequestParam int capacity,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime startTime,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime endTime,
            Model model) {

        try {
            LocalDateTime start = date.atTime(startTime);
            LocalDateTime end = date.atTime(endTime);

            eventService.saveOrUpdateEvent(null, title, location, capacity, start, end);
            return "redirect:/";

        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("event", new Event(title, location, capacity, date.atTime(startTime), date.atTime(endTime)));
            return "new-event";
        } catch (Exception ex) {
            model.addAttribute("error", "Unexpected error: " + ex.getMessage());
            model.addAttribute("event", new Event(title, location, capacity, date.atTime(startTime), date.atTime(endTime)));
            return "new-event";
        }
    }


    @GetMapping("/edit-event/{id}")
    public String editEvent(@PathVariable Long id, Model model) {
        Event event = eventService.getEvent(id).orElse(null);
        if (event == null) return "redirect:/";
        model.addAttribute("event", event);
        return "edit-event";
    }

    @PostMapping("/edit-event/{id}")
    public String updateEvent(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam String location,
            @RequestParam int capacity,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime startTime,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime endTime,
            Model model) {

        try {
            LocalDateTime start = date.atTime(startTime);
            LocalDateTime end = date.atTime(endTime);

            eventService.saveOrUpdateEvent(id, title, location, capacity, start, end);
            return "redirect:/";

        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("event", new Event(title, location, capacity, date.atTime(startTime), date.atTime(endTime)));
            return "edit-event";
        } catch (Exception ex) {
            model.addAttribute("error", "Unexpected error: " + ex.getMessage());
            model.addAttribute("event", new Event(title, location, capacity, date.atTime(startTime), date.atTime(endTime)));
            return "edit-event";
        }
    }

    @GetMapping("/delete-event/{id}")
    private String deleteEvent(@PathVariable Long id) {
        eventService.deleteEvent(id);
        return "redirect:/";
    }

    @PostMapping("/delete-multiple")
    private String deleteMultipleEvents(@RequestParam List<Long> ids) {
        for (Long id : ids) {
            deleteEvent(id);
        }
        return "redirect:/";
    }

    @GetMapping("/details/{id}")
    public String eventDetails(@PathVariable Long id, Model model) {
        Event event = eventService.getEvent(id).orElse(null);
        if (event == null) return "redirect:/";

        model.addAttribute("event", event);
        model.addAttribute("participants", event.getParticipants());
        return "details";
    }

    @GetMapping("/")
    public String filter(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @RequestParam(required = false) String available,
            Model model) {

        List<Event> events = eventService.filterEvents(title, location, date, available);

        model.addAttribute("events", events);
        model.addAttribute("filterTitle", title);
        model.addAttribute("filterLocation", location);
        model.addAttribute("filterDate", date);
        model.addAttribute("filterAvailable", available);

        boolean panelOpen = title != null || location != null || date != null || (available != null && !available.isBlank());
        model.addAttribute("panelOpen", panelOpen);

        return "calendar";
    }

}
