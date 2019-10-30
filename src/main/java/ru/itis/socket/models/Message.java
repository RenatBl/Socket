package ru.itis.socket.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Message {
    private Long id;
    private String text;
    private LocalDateTime dateTime;
    private Long ownerId;
    private Long receiverId;

    public Message(String text, LocalDateTime dateTime, Long ownerId) {
        this.text = text;
        this.dateTime = dateTime;
        this.ownerId = ownerId;
    }

    public Message(String text, LocalDateTime dateTime, Long ownerId, Long receiverId) {
        this.text = text;
        this.dateTime = dateTime;
        this.ownerId = ownerId;
        this.receiverId = receiverId;
    }
}
