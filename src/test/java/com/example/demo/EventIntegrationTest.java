package com.example.demo;

import com.example.demo.entity.Event;
import com.example.demo.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.hamcrest.Matchers.containsString;
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
public class EventIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    private Event e1;
    private Event e2;

    @BeforeEach
    public void setup() {
        eventRepository.deleteAll();

        e1 = new Event("Yoga", "1.1", 10,
                LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.of(10, 0)),
                LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.of(11, 0)));
        e2 = new Event("Pilates", "1.2", 8,
                LocalDateTime.of(LocalDate.now().plusDays(2), LocalTime.of(14, 0)),
                LocalDateTime.of(LocalDate.now().plusDays(2), LocalTime.of(15, 0)));

        eventRepository.save(e1);
        eventRepository.save(e2);
    }

    @Test
    public void createEventTest() throws Exception {
        mockMvc.perform(post("/new-event")
                        .param("title", "Zumba")
                        .param("location", "2.1")
                        .param("capacity", "12")
                        .param("date", LocalDate.now().plusDays(3).toString())
                        .param("startTime", "09:00")
                        .param("endTime", "10:00")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        Event created = eventRepository.findAll().stream()
                .filter(e -> e.getTitle().equals("Zumba"))
                .findFirst()
                .orElse(null);
        assert created != null;
        assert created.getLocation().equals("2.1");
        assert created.getCapacity() == 12;
    }

    @Test
    public void editEventTest() throws Exception {
        mockMvc.perform(post("/edit-event/" + e1.getId())
                        .param("title", "Yoga Advanced")
                        .param("location", e1.getLocation())
                        .param("capacity", String.valueOf(e1.getCapacity()))
                        .param("date", e1.getStartTime().toLocalDate().toString())
                        .param("startTime", e1.getStartTime().toLocalTime().toString())
                        .param("endTime", e1.getEndTime().toLocalTime().toString())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        Event updated = eventRepository.findById(e1.getId()).orElse(null);
        assert updated != null;
        assert updated.getTitle().equals("Yoga Advanced");
    }

    @Test
    public void deleteEventTest() throws Exception {
        mockMvc.perform(get("/delete-event/" + e1.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        assert eventRepository.findById(e1.getId()).isEmpty();
    }

    @Test
    public void deleteMultipleEventsTest() throws Exception {
        mockMvc.perform(post("/delete-multiple")
                        .param("ids", String.valueOf(e1.getId()), String.valueOf(e2.getId()))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        assert eventRepository.count() == 0;
    }

    @Test
    public void filterTest() throws Exception {
        mockMvc.perform(get("/").param("title", "Yoga"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Yoga")))
                .andExpect(content().string(containsString(e1.getLocation())));

        mockMvc.perform(get("/").param("location", "1.2"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Pilates")))
                .andExpect(content().string(containsString("1.2")));

        mockMvc.perform(get("/").param("date", e1.getStartTime().toLocalDate().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Yoga")))
                .andExpect(content().string(containsString(e1.getLocation())));

        mockMvc.perform(get("/").param("available", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Yoga")))
                .andExpect(content().string(containsString("Pilates")));

        mockMvc.perform(get("/").param("available", "false"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Yoga"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Pilates"))));
    }

}
