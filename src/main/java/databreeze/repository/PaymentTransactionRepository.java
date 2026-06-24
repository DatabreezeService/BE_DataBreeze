package databreeze.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import databreeze.entity.PaymentTransaction;
import databreeze.enums.PaymentProvider;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

    Optional<PaymentTransaction> findByProviderAndProviderOrderCode(
            PaymentProvider provider, String providerOrderCode);

    Optional<PaymentTransaction> findByProviderAndProviderPaymentId(
            PaymentProvider provider, String providerPaymentId);

    List<PaymentTransaction> findByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);
}
