package io.harness.cvng.core.utils;

import io.harness.cvng.client.NextGenService;
import io.harness.ng.core.environment.dto.EnvironmentResponseDTO;
import io.harness.ng.core.service.dto.ServiceResponseDTO;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class EnvironmentServiceCache {
  @Inject private NextGenService nextGenService;

  private LoadingCache<EntityKey, EnvironmentResponseDTO> environmentCache =
      CacheBuilder.newBuilder()
          .maximumSize(10000)
          .expireAfterWrite(1, TimeUnit.HOURS)
          .build(new CacheLoader<EntityKey, EnvironmentResponseDTO>() {
            @Override
            public EnvironmentResponseDTO load(EntityKey entityKey) {
              return nextGenService.getEnvironment(entityKey.getEntityIdentifier(), entityKey.getAccountId(),
                  entityKey.getOrgIdentifier(), entityKey.getProjectIdentifier());
            }
          });

  private LoadingCache<EntityKey, ServiceResponseDTO> serviceCache =
      CacheBuilder.newBuilder()
          .maximumSize(10000)
          .expireAfterWrite(1, TimeUnit.HOURS)
          .build(new CacheLoader<EntityKey, ServiceResponseDTO>() {
            @Override
            public ServiceResponseDTO load(EntityKey entityKey) {
              return nextGenService.getService(entityKey.getEntityIdentifier(), entityKey.getAccountId(),
                  entityKey.getOrgIdentifier(), entityKey.getProjectIdentifier());
            }
          });

  public EnvironmentResponseDTO getEnvironment(
      String accountId, String orgIdentifier, String projectIdentifier, String environmentIdentifier) {
    try {
      return environmentCache.get(EntityKey.builder()
                                      .accountId(accountId)
                                      .orgIdentifier(orgIdentifier)
                                      .projectIdentifier(projectIdentifier)
                                      .entityIdentifier(environmentIdentifier)
                                      .build());
    } catch (ExecutionException ex) {
      log.warn("Exception occurred in fetching environment for account {} org {} project {} env {}", accountId,
          orgIdentifier, projectIdentifier, environmentIdentifier, ex);
    }
    return null;
  }

  public ServiceResponseDTO getService(
      String accountId, String orgIdentifier, String projectIdentifier, String environmentIdentifier) {
    try {
      return serviceCache.get(EntityKey.builder()
                                  .accountId(accountId)
                                  .orgIdentifier(orgIdentifier)
                                  .projectIdentifier(projectIdentifier)
                                  .entityIdentifier(environmentIdentifier)
                                  .build());
    } catch (ExecutionException ex) {
      log.warn("Exception occurred in fetching environment for account {} org {} project {} env {}", accountId,
          orgIdentifier, projectIdentifier, environmentIdentifier, ex);
    }
    return null;
  }

  @Value
  @Builder
  public static class EntityKey {
    private String accountId;
    private String orgIdentifier;
    private String projectIdentifier;
    private String entityIdentifier;
  }
}
