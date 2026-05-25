package databreeze.service.store;

import databreeze.dto.store.CreateStoreRequest;
import databreeze.dto.store.StoreResponse;
import databreeze.dto.store.UpdateStoreRequest;
import java.util.List;
import java.util.UUID;

public interface StoreService {
    List<StoreResponse> listStores(UUID workspaceId, UUID actorUserId);

    StoreResponse createStore(UUID workspaceId, UUID actorUserId, CreateStoreRequest request);

    StoreResponse getStore(UUID workspaceId, UUID actorUserId, UUID storeId);

    StoreResponse updateStore(UUID workspaceId, UUID actorUserId, UUID storeId, UpdateStoreRequest request);

    StoreResponse archiveStore(UUID workspaceId, UUID actorUserId, UUID storeId);
}
