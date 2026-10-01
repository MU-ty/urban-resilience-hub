package com.example.resilience.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "app_user")
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true) private String username;
    @Column(name = "password_hash", nullable = false) private String passwordHash;
    @Column(nullable = false) private String role;
    @Column(nullable = false) private boolean enabled = true;
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false) private Instant createdAt;

    protected AppUser() {}
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getRole() { return role; }
    public boolean isEnabled() { return enabled; }
}
