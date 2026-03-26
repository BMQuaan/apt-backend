package com.ptithcm.apt.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notification_recipients", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"notification_id", "apartment_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRecipient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "apartment_id", nullable = false)
    private Apartment apartment;

    @Column(name = "is_read")
    @Builder.Default
    private Boolean isRead = false;
}
