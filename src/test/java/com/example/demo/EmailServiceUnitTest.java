package com.example.demo;

import com.example.demo.entity.Event;
import com.example.demo.entity.Participant;
import com.example.demo.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceUnitTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    private Event event;
    private Participant participant;

    private Event createEvent() {
        Event event = new Event();
        event.setId(1L);
        event.setTitle("Yoga");
        event.setLocation("1.1");
        event.setStartTime(LocalDateTime.of(2025, 9, 12, 10, 0));
        event.setEndTime(LocalDateTime.of(2025, 9, 12, 11, 0));
        return event;
    }

    private Participant createParticipant(Event event) {
        Participant participant = new Participant();
        participant.setId(1L);
        participant.setFirstName("Jane");
        participant.setLastName("Doe");
        participant.setEmail("a@testt.com");
        participant.setAge(25);
        participant.setEvent(event);
        return participant;
    }

    @BeforeEach
    public void setup() {
        event = createEvent();
        participant = createParticipant(event);
    }

    @Test
    void sendRegistrationEmailTest() {
        emailService.sendRegistrationEmail(participant, event);
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendUnregistrationEmailTest() {
        emailService.sendUnregistrationEmail(participant, event);
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendEventDeletedEmailTest() {
        event.setParticipants(List.of(participant));
        emailService.sendEventDeletedEmail(event);
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendEventUpdatedEmailTest() {
        event.setParticipants(List.of(participant));
        emailService.sendEventUpdatedEmail(event);
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendParticipantDeletedEmailTest() {
        emailService.sendParticipantDeletedEmail(participant, event);
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}
