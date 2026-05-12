package databreeze.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import databreeze.entity.ExternalIdentity;
import databreeze.enums.AuthProvider;

public interface ExternalIdentityRepository extends JpaRepository<ExternalIdentity, UUID> {
    Optional<ExternalIdentity> findByProviderAndProviderUserId(AuthProvider provider, String providerUserId);

    Optional<ExternalIdentity> findByProviderAndProviderEmail(AuthProvider provider, String providerEmail);
}
