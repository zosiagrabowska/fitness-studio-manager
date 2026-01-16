package com.example.demo;

import com.example.demo.entity.Event;
import com.example.demo.entity.Participant;
import com.example.demo.exception.RegistrationFullException;
import com.example.demo.repository.EventRepository;
import com.example.demo.repository.ParticipantRepository;
import com.example.demo.service.EmailService;
import com.example.demo.service.ParticipantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import jakarta.servlet.http.HttpServletResponse;

import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ParticipantServiceUnitTest {

    @Mock
    private ParticipantRepository participantRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private ParticipantService service;

    private Event e;
    private Participant p;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        e = new Event("Yoga", "1.1", 2, LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        e.setId(1L);

        p = new Participant("email@gmail.com", "John", "Doe", 25, null);
        p.setId(1L);
    }

    @Test
    void isRegisteredTest() {
        when(participantRepository.existsByEmailAndEventId("a@test.com", 1L)).thenReturn(true);
        assertTrue(service.isRegistered("a@test.com", 1L));

        when(participantRepository.existsByEmailAndEventId("a@test.com", 1L)).thenReturn(false);
        assertFalse(service.isRegistered("a@test.com", 1L));
    }


    @Test
    void registerSuccessfulTest() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(e));
        when(participantRepository.existsByEmailAndEventId(p.getEmail(), 1L)).thenReturn(false);

        service.registerParticipant(1L, p, true);

        verify(participantRepository).save(p);
        verify(eventRepository).save(e);
        verify(emailService).sendRegistrationEmail(p, e);
        assertEquals(1, e.getRegistered());
    }

    @Test
    void registerNoSpotsTest() {
        e.setRegistered(2);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(e));

        RegistrationFullException ex = assertThrows(RegistrationFullException.class,
                () -> service.registerParticipant(1L, p, true));
        assertEquals("No available spots. Try again later!", ex.getMessage());
    }

    @Test
    void registerNotAcceptedTest() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(e));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.registerParticipant(1L, p, false));
        assertEquals("You must accept the terms!", ex.getMessage());
    }

    @Test
    void registerAlreadyRegisteredTest() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(e));
        when(participantRepository.existsByEmailAndEventId(p.getEmail(), 1L)).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.registerParticipant(1L, p, true));
        assertEquals("You are already registered for this class!", ex.getMessage());
    }

    @Test
    void unregisterTest() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(e));
        when(participantRepository.existsByEmailAndEventId("john@example.com", 1L)).thenReturn(true);
        when(participantRepository.findByEmailAndEventId("john@example.com", 1L)).thenReturn(Optional.of(p));

        e.setRegistered(1);

        service.unregisterParticipant(1L, "john@example.com");

        verify(participantRepository).delete(p);
        verify(eventRepository).save(e);
        verify(emailService).sendUnregistrationEmail(p, e);
        assertEquals(0, e.getRegistered());
    }

    @Test
    void unregisterNotRegisteredTest() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(e));
        when(participantRepository.existsByEmailAndEventId("john@example.com", 1L)).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.unregisterParticipant(1L, "john@example.com"));
        assertEquals("You are not registered for this class!", ex.getMessage());
    }

    @Test
    void deleteSuccessfulTest() {
        p.setEvent(e);
        e.setRegistered(1);
        when(participantRepository.findById(1L)).thenReturn(Optional.of(p));

        service.deleteParticipant(1L);

        verify(participantRepository).delete(p);
        verify(eventRepository).save(e);
        verify(emailService).sendParticipantDeletedEmail(p, e);
        assertEquals(0, e.getRegistered());
    }

    @Test
    void exportCsvTest() throws Exception {
        p.setId(1L);
        e.getParticipants().add(p);
        e.setStartTime(LocalDateTime.of(2025, 9, 11, 12, 0));

        PrintWriter writer = mock(PrintWriter.class);
        when(response.getWriter()).thenReturn(writer);

        service.exportParticipantsCsv(e, response);

        verify(writer).println("Id,First Name,Last Name,Age,Email Address");
        verify(writer).printf("%d,%s,%s,%d,%s%n",
                p.getId(),
                p.getFirstName(),
                p.getLastName(),
                p.getAge(),
                p.getEmail());
    }

}
