package com.example.demo.controller;

import com.example.demo.entity.Event;
import com.example.demo.entity.Participant;
import com.example.demo.service.EventService;
import com.example.demo.service.ParticipantService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Controller
public class ParticipantController {

    private final EventService eventService;
    private final ParticipantService participantService;

    public ParticipantController(EventService eventService,
                                 ParticipantService participantService) {
        this.eventService = eventService;
        this.participantService = participantService;
    }

    @GetMapping("/register/{eventId}")
    public String showRegisterForm(@PathVariable Long eventId, Model model) {
        Event event = eventService.getEvent(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found."));

        if (event.getRegistered() >= event.getCapacity()) {
            List<Event> events = eventService.getAllEvents();
            model.addAttribute("events", events);
            model.addAttribute("error", "No spots available for this class!");
            return "participants";
        }

        model.addAttribute("event", event);
        model.addAttribute("participant", new Participant());
        return "register";
    }

    @PostMapping("/register/{eventId}")
    public String register(@PathVariable Long eventId,
                           @ModelAttribute Participant participant,
                           @RequestParam(required = false) boolean accepted,
                           Model model) {

        Event event = eventService.getEvent(eventId).orElse(null);
        if (event == null) return "redirect:/participants";

        try {
            participantService.registerParticipant(eventId, participant, accepted);
            return "redirect:/participants";
        } catch (IllegalStateException | IllegalArgumentException ex) {
            model.addAttribute("event", event);
            model.addAttribute("participant", participant);
            model.addAttribute("error", ex.getMessage());
            return "register";
        }
    }

    @GetMapping("/unregister/{eventId}")
    public String showUnregisterForm(@PathVariable Long eventId, Model model) {
        Event event = eventService.getEvent(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found."));

        model.addAttribute("event", event);
        model.addAttribute("participant", new Participant());
        return "unregister";
    }

    @PostMapping("/unregister/{eventId}")
    public String unregister(@PathVariable Long eventId,
                             @ModelAttribute Participant participant,
                             Model model) {
        try {
            participantService.unregisterParticipant(eventId, participant.getEmail());
            return "redirect:/participants";
        } catch (IllegalArgumentException ex) {
            Event event = eventService.getEvent(eventId).orElse(null);
            model.addAttribute("event", event);
            model.addAttribute("participant", participant);
            model.addAttribute("error", ex.getMessage());
            return "unregister";
        }
    }

    @PostMapping("/participants/delete-multiple")
    public String deleteMultipleParticipants(@RequestParam List<Long> ids) {
        if (ids == null || ids.isEmpty()) return "redirect:/participants";

        Long eventId = null;
        for (Long id : ids) {
            eventId = participantService.getEventIdForParticipant(id);
            participantService.deleteParticipant(id);
        }

        if (eventId == null) return "redirect:/participants";
        return "redirect:/details/" + eventId;
    }


    @GetMapping("/participants/delete/{id}")
    public String deleteParticipant(@PathVariable Long id) {
        Long eventId = participantService.getEventIdForParticipant(id);
        participantService.deleteParticipant(id);
        return "redirect:/details/" + eventId;
    }

    @GetMapping("/participants")
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

        return "participants";
    }

    @GetMapping("/details/{id}/export")
    public void exportParticipantsCsv(@PathVariable Long id, HttpServletResponse response) {
        Event event = eventService.getEvent(id).orElse(null);
        participantService.exportParticipantsCsv(event, response);
    }

}
