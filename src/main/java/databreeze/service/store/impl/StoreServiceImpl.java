package databreeze.service.store.impl;

import databreeze.dto.billing.SubscriptionResponse;
import databreeze.dto.store.CreateStoreRequest;
import databreeze.dto.store.StoreResponse;
import databreeze.dto.store.UpdateStoreRequest;
import databreeze.entity.Store;
import databreeze.enums.CommercePlatform;
import databreeze.enums.WorkspacePermission;
import databreeze.enums.WorkspaceStatus;
import databreeze.repository.StoreRepository;
import databreeze.service.billing.UsageMeterService;
import databreeze.service.store.StoreService;
import databreeze.service.workspace.WorkspaceAccessService;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StoreServiceImpl implements StoreService {
    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private WorkspaceAccessService workspaceAccessService;

    @Autowired
    private UsageMeterService usageMeterService;

    @Override
    @Transactional(readOnly = true)
    public List<StoreResponse> listStores(UUID workspaceId, UUID actorUserId) {
        workspaceAccessService.requireReadAccess(workspaceId, actorUserId);
        return storeRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public StoreResponse createStore(UUID workspaceId, UUID actorUserId, CreateStoreRequest request) {
        workspaceAccessService.requirePermission(workspaceId, actorUserId, WorkspacePermission.MANAGE_STORES);
        enforceStoreLimit(workspaceId, actorUserId);

        Store store = Store.builder()
                .workspaceId(workspaceId)
                .name(normalizeRequired(request.getName(), "Tên shop/store không được để trống."))
                .platform(request.getPlatform() == null ? CommercePlatform.SHOPEE : request.getPlatform())
                .externalStoreId(normalizeOptional(request.getExternalStoreId()))
                .countryCode(defaultIfBlank(request.getCountryCode(), "VN"))
                .currencyCode(defaultIfBlank(request.getCurrencyCode(), "VND"))
                .status(WorkspaceStatus.ACTIVE)
                .build();
        return toResponse(storeRepository.save(store));
    }

    @Override
    @Transactional(readOnly = true)
    public StoreResponse getStore(UUID workspaceId, UUID actorUserId, UUID storeId) {
        workspaceAccessService.requireReadAccess(workspaceId, actorUserId);
        return toResponse(requireStore(workspaceId, storeId));
    }

    @Override
    @Transactional
    public StoreResponse updateStore(UUID workspaceId, UUID actorUserId, UUID storeId, UpdateStoreRequest request) {
        workspaceAccessService.requirePermission(workspaceId, actorUserId, WorkspacePermission.MANAGE_STORES);
        Store store = requireStore(workspaceId, storeId);

        if (request.getName() != null) {
            store.setName(normalizeRequired(request.getName(), "Tên shop/store không được để trống."));
        }
        if (request.getPlatform() != null) {
            store.setPlatform(request.getPlatform());
        }
        if (request.getExternalStoreId() != null) {
            store.setExternalStoreId(normalizeOptional(request.getExternalStoreId()));
        }
        if (request.getCountryCode() != null) {
            store.setCountryCode(defaultIfBlank(request.getCountryCode(), "VN"));
        }
        if (request.getCurrencyCode() != null) {
            store.setCurrencyCode(defaultIfBlank(request.getCurrencyCode(), "VND"));
        }
        if (request.getStatus() != null) {
            store.setStatus(request.getStatus());
        }
        return toResponse(storeRepository.save(store));
    }

    @Override
    @Transactional
    public StoreResponse archiveStore(UUID workspaceId, UUID actorUserId, UUID storeId) {
        workspaceAccessService.requirePermission(workspaceId, actorUserId, WorkspacePermission.MANAGE_STORES);
        Store store = requireStore(workspaceId, storeId);
        store.setStatus(WorkspaceStatus.ARCHIVED);
        return toResponse(storeRepository.save(store));
    }

    private void enforceStoreLimit(UUID workspaceId, UUID actorUserId) {
        SubscriptionResponse subscription = usageMeterService.getSubscription(workspaceId, actorUserId);
        Integer maxStores = subscription.getPlan() == null ? null : subscription.getPlan().getMaxStores();
        if (maxStores == null) {
            return;
        }
        long activeStores = storeRepository.countByWorkspaceIdAndStatus(workspaceId, WorkspaceStatus.ACTIVE);
        if (activeStores >= maxStores) {
            throw new IllegalStateException("Workspace đã đạt giới hạn " + maxStores + " store của gói hiện tại.");
        }
    }

    private Store requireStore(UUID workspaceId, UUID storeId) {
        return storeRepository.findByIdAndWorkspaceId(storeId, workspaceId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy shop/store trong workspace hiện tại."));
    }

    private StoreResponse toResponse(Store store) {
        return StoreResponse.builder()
                .id(store.getId())
                .workspaceId(store.getWorkspaceId())
                .name(store.getName())
                .platform(store.getPlatform())
                .externalStoreId(store.getExternalStoreId())
                .countryCode(store.getCountryCode())
                .currencyCode(store.getCurrencyCode())
                .status(store.getStatus())
                .createdAt(store.getCreatedAt())
                .updatedAt(store.getUpdatedAt())
                .build();
    }

    private String normalizeRequired(String value, String message) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private String defaultIfBlank(String value, String fallback) {
        String normalized = normalizeOptional(value);
        return normalized == null ? fallback : normalized.toUpperCase();
    }
}
