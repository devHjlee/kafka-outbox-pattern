package com.hj.kafkaoutboxpattern.outbox.enity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_outbox")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String aggregateType;
    private Long aggregateId;

    private String type;

    @Lob
    private String payload;

    private LocalDateTime createdAt;

    private boolean sent;

    public void completeSent() {
        this.sent = true;
    }
}
