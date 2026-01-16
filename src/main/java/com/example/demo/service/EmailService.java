package com.example.demo.service;

import com.example.demo.entity.Event;
import com.example.demo.entity.Participant;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    private String buildEmailBody(Participant participant, Event event, String firstLine, String ending) {
        return "Hi " + participant.getFirstName() + ",\n\n" +
                firstLine + "\n\n" +
                "Details:\n" +
                "Title: " + event.getTitle() + "\n" +
                "Room: " + event.getLocation() + "\n" +
                "Date: " + event.getStartTime().toLocalDate() + "\n" +
                "Time: " + event.getStartTime().toLocalTime() + "-" + event.getEndTime().toLocalTime() + "\n\n" +
                ending;
    }

    public void sendRegistrationEmail(Participant participant, Event event) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(participant.getEmail());
        message.setFrom("Company XYZ <mail@gmail.com>");
        message.setSubject("Confirmation of registration for: " + event.getTitle());
        message.setText(buildEmailBody(
                participant,
                event,
                "Thank you for registering!",
                "Please arrive 10 minutes before the class starts.\n\nSee you soon!\n\nTeam XYZ"
        ));
        mailSender.send(message);
    }

    public void sendUnregistrationEmail(Participant participant, Event event) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(participant.getEmail());
        message.setFrom("Company XYZ <twoj.email@gmail.com>");
        message.setSubject("Confirmation of unregistration from: " + event.getTitle());
        message.setText(buildEmailBody(
                participant,
                event,
                "You have been successfully unregistered from the class.",
                "We hope to see you at another class soon!\n\nTeam XYZ"
        ));
        mailSender.send(message);
    }

    public void sendParticipantDeletedEmail(Participant participant, Event event) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(participant.getEmail());
        message.setFrom("Company XYZ <twoj.email@gmail.com>");
        message.setSubject("Removed from the list for: " + event.getTitle());
        message.setText(buildEmailBody(
                participant,
                event,
                "We're sorry to inform you that you have been removed from the list of attendees for the class.",
                "We apologize for the inconvenience.\n\nTeam XYZ"
        ));
        mailSender.send(message);
    }

    public void sendEventUpdatedEmail(Event event) {
        if (event.getParticipants() == null) return;

        for (Participant participant : event.getParticipants()) {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(participant.getEmail());
            message.setFrom("Company XYZ <twoj.email@gmail.com>");
            message.setSubject("Class updated: " + event.getTitle());
            message.setText(buildEmailBody(
                    participant,
                    event,
                    "The class you are registered for has been updated.\n\nPlease check the updated schedule below.",
                    "See you soon!\n\nTeam XYZ"
            ));
            mailSender.send(message);
        }
    }

    public void sendEventDeletedEmail(Event event) {
        if (event.getParticipants() == null) return;

        for (Participant participant : event.getParticipants()) {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(participant.getEmail());
            message.setFrom("Company XYZ <twoj.email@gmail.com>");
            message.setSubject("Class cancelled: " + event.getTitle());
            message.setText(buildEmailBody(
                    participant,
                    event,
                    "The following class has been cancelled:",
                    "We apologize for the inconvenience.\n\nTeam XYZ"
            ));
            mailSender.send(message);
        }
    }
}
