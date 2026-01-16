package com.example.demo.service;

import com.example.demo.entity.Event;
import com.example.demo.entity.Participant;
import com.example.demo.exception.RegistrationFullException;
import com.example.demo.repository.EventRepository;
import com.example.demo.repository.ParticipantRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.io.PrintWriter;
import java.io.IOException;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class ParticipantService {

    private final ParticipantRepository participantRepository;
    private final EventRepository eventRepository;
    private final EmailService emailService;

    public ParticipantService(ParticipantRepository participantRepository,
                              EventRepository eventRepository,
                              EmailService emailService) {
        this.participantRepository = participantRepository;
        this.eventRepository = eventRepository;
        this.emailService = emailService;
    }

    public boolean isRegistered(String email, Long eventId) {
        return participantRepository.existsByEmailAndEventId(email, eventId);
    }

    public void registerParticipant(Long eventId, Participant participant, boolean accepted) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));

        if (event.getRegistered() >= event.getCapacity())
            throw new RegistrationFullException("No available spots. Try again later!");

        if (!accepted)
            throw new IllegalStateException("You must accept the terms!");

        if (participantRepository.existsByEmailAndEventId(participant.getEmail(), eventId))
            throw new IllegalArgumentException("You are already registered for this class!");

        participant.setEvent(event);
        participantRepository.save(participant);

        event.setRegistered(event.getRegistered() + 1);
        eventRepository.save(event);

        emailService.sendRegistrationEmail(participant, event);
    }

    public void unregisterParticipant(Long eventId, String email) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));

        if (!isRegistered(email, eventId)) {
            throw new IllegalArgumentException("You are not registered for this class!");
        }

        Participant participant = participantRepository.findByEmailAndEventId(email, eventId)
                .orElseThrow(() -> new IllegalArgumentException("Participant not found"));

        participantRepository.delete(participant);

        event.setRegistered(Math.max(0, event.getRegistered() - 1));
        eventRepository.save(event);

        emailService.sendUnregistrationEmail(participant, event);
    }

    @Transactional
    public void deleteParticipant(Long id) {
        participantRepository.findById(id).ifPresent(p -> {
            Event event = p.getEvent();
            participantRepository.delete(p);
            if (event != null) {
                event.setRegistered(Math.max(0, event.getRegistered() - 1));
                eventRepository.save(event);
                emailService.sendParticipantDeletedEmail(p, event);
            }
        });
    }

    public Long getEventIdForParticipant(Long id){
        return participantRepository.findById(id)
                .map(p -> p.getEvent() != null ? p.getEvent().getId() : null)
                .orElse(null);
    }

    public void exportParticipantsCsv(Event event, HttpServletResponse response) {
        if (event == null) return;

        String eventId = event.getId().toString();
        String safeTitle = event.getTitle().replaceAll("[^a-zA-Z0-9\\-]", "_");
        String date = event.getStartTime().toLocalDate().toString();
        String time = event.getStartTime().toLocalTime().toString().replace(":", "h");
        String filename = String.format("%s_%s_%s_%s.csv", eventId, safeTitle, date, time);

        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        try (PrintWriter writer = response.getWriter()) {
            writer.println("Id,First Name,Last Name,Age,Email Address");

            for (Participant p : event.getParticipants()) {
                writer.printf("%d,%s,%s,%d,%s%n",
                        p.getId(),
                        p.getFirstName(),
                        p.getLastName(),
                        p.getAge(),
                        p.getEmail()
                );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
