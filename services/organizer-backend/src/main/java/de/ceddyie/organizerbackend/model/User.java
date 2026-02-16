package de.ceddyie.organizerbackend.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String discordId;

    private String username;
    private String avatar;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
