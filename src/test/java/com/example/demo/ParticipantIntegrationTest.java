package com.example.demo;

import com.example.demo.entity.Event;
import com.example.demo.entity.Participant;
import com.example.demo.repository.EventRepository;
import com.example.demo.repository.ParticipantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})

@AutoConfigureMockMvc
public class ParticipantIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    private Event e1;
    private Event e2;

    @BeforeEach
    void setup() {
        eventRepository.deleteAll();

        e1 = new Event("Event 1", "Room A", 2,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(2));
        e2 = new Event("Event 2", "Room B", 1,
                LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(2).plusHours(3));

        eventRepository.save(e1);
        eventRepository.save(e2);
    }

    @Test
    void registerSuccessfulTest() throws Exception {
        mockMvc.perform(post("/register/" + e1.getId())
                        .param("firstName", "John")
                        .param("lastName", "Doe")
                        .param("age", "25")
                        .param("email", "a@test.com")
                        .param("accepted", "true")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/participants"));

        assertThat(participantRepository.existsByEmailAndEventId("a@test.com", e1.getId())).isTrue();
    }

    @Test
    void registerFullTest() throws Exception {
        Participant p = new Participant("a@test.com", "A", "B", 20, e2);
        participantRepository.save(p);
        e2.setRegistered(1);
        eventRepository.save(e2);

        mockMvc.perform(get("/register/" + e2.getId()))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("error"))
                .andExpect(model().attribute("error", "No spots available for this class!"));
    }

    @Test
    void registerNotAcceptedTest() throws Exception {
        mockMvc.perform(post("/register/" + e1.getId())
                        .param("firstName", "Jane")
                        .param("lastName", "Doe")
                        .param("age", "22")
                        .param("email", "a@test.com")
                        .param("accepted", "false")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("error"))
                .andExpect(model().attribute("error", "You must accept the terms!"));
    }

    @Test
    void unregisterTest() throws Exception {
        Participant p = new Participant("a@test.com", "Mike", "Smith", 30, e1);
        participantRepository.save(p);
        e1.setRegistered(1);
        eventRepository.save(e1);

        mockMvc.perform(post("/unregister/" + e1.getId())
                        .param("email", "a@test.com")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/participants"));

        assertThat(participantRepository.existsByEmailAndEventId("a@test.com", e1.getId())).isFalse();
    }

    @Test
    void deleteTest() throws Exception {
        Participant p = new Participant("a@test.com", "Alice", "Wonder", 28, e1);
        participantRepository.save(p);
        e1.setRegistered(1);
        eventRepository.save(e1);

        mockMvc.perform(get("/participants/delete/" + p.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/details/" + e1.getId()));

        assertThat(participantRepository.existsByEmailAndEventId("a@test.com", e1.getId())).isFalse();
    }

    @Test
    void deleteMultipleTest() throws Exception {
        Participant p1 = new Participant("a@test.com", "A", "B", 20, e1);
        Participant p2 = new Participant("c@test.com", "C", "D", 22, e1);
        participantRepository.save(p1);
        participantRepository.save(p2);
        e1.setRegistered(2);
        eventRepository.save(e1);

        mockMvc.perform(post("/participants/delete-multiple")
                        .param("ids", String.valueOf(p1.getId()), String.valueOf(p2.getId()))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/details/" + e1.getId()));

        assertThat(participantRepository.count()).isZero();
    }

    @Test
    void isRegisteredTest() throws Exception {
        Participant p = new Participant("a@test.com", "F", "G", 24, e1);
        participantRepository.save(p);

        assertThat(participantRepository.existsByEmailAndEventId("a@test.com", e1.getId())).isTrue();
        assertThat(participantRepository.existsByEmailAndEventId("b@test.com", e1.getId())).isFalse();
    }
}
