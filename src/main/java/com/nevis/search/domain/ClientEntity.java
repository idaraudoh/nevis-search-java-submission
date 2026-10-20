package com.nevis.search.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "clients")
public class ClientEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "first_name", nullable = false, length = 200)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 200)
    private String lastName;

    /** Stored lowercased so uniqueness is case-insensitive. */
    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(columnDefinition = "text")
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "client_social_links", joinColumns = @JoinColumn(name = "client_id"))
    @OrderColumn(name = "link_order")
    @Column(name = "link", length = 1000)
    private List<String> socialLinks = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ClientEntity() {}

    public ClientEntity(String firstName, String lastName, String email, String description, List<String> socialLinks) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.description = description;
        this.socialLinks = socialLinks == null ? new ArrayList<>() : new ArrayList<>(socialLinks);
    }

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID().toString();
        if (createdAt == null) createdAt = Instant.now();
    }

    public String getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getDescription() { return description; }
    public List<String> getSocialLinks() { return socialLinks; }
    public Instant getCreatedAt() { return createdAt; }
}
