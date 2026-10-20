package com.nevis.search.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<ClientEntity, String> {
    boolean existsByEmail(String email);
}
