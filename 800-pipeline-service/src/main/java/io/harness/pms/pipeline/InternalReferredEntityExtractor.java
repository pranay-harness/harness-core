package io.harness.pms.pipeline;

import static io.harness.utils.RestCallToNGManagerClientUtils.execute;

import io.harness.EntityType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.entitysetupusageclient.remote.EntitySetupUsageClient;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.entitysetupusage.dto.EntityReferencesDTO;
import io.harness.ng.core.entitysetupusage.dto.EntitySetupUsageBatchDTO;
import io.harness.ng.core.entitysetupusage.dto.EntitySetupUsageDTO;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Singleton
@OwnedBy(HarnessTeam.PIPELINE)
public class InternalReferredEntityExtractor {
  private static final int MAX_PAGE_SIZE = 50;
  @Inject EntitySetupUsageClient entitySetupUsageClient;

  public List<EntityDetail> extractInternalEntities(String accountIdentifier, List<EntityDetail> entityDetails) {
    List<EntityDetail> referredEntitiesContainingInternalEntities =
        entityDetails.stream()
            .filter(entityDetail -> hasInternalReferredEntities(entityDetail.getType()))
            .collect(Collectors.toList());
    List<EntityDetail> internalReferredEntities = new ArrayList<>();
    Map<EntityType, List<String>> entityTypeEntityDetailMap = new HashMap<>();
    for (EntityDetail entityDetail : referredEntitiesContainingInternalEntities) {
      List<String> entities = entityTypeEntityDetailMap.getOrDefault(entityDetail.getType(), new ArrayList<>());
      entities.add(entityDetail.getEntityRef().getFullyQualifiedName());
      entityTypeEntityDetailMap.put(entityDetail.getType(), entities);
    }
    for (Map.Entry<EntityType, List<String>> entry : entityTypeEntityDetailMap.entrySet()) {
      List<List<String>> partitionedList = Lists.partition(entry.getValue(), MAX_PAGE_SIZE);
      for (List<String> entityDetail : partitionedList) {
        EntityReferencesDTO entityReferencesDTO = execute(entitySetupUsageClient.listAllReferredUsagesBatch(
            accountIdentifier, entityDetail, entry.getKey(), EntityType.SECRETS));
        for (EntitySetupUsageBatchDTO entitySetupUsageBatchDTO : entityReferencesDTO.getEntitySetupUsageBatchList()) {
          internalReferredEntities.addAll(entitySetupUsageBatchDTO.getReferredEntities()
                                              .stream()
                                              .map(EntitySetupUsageDTO::getReferredEntity)
                                              .collect(Collectors.toList()));
        }
      }
    }
    return internalReferredEntities;
  }

  private boolean hasInternalReferredEntities(EntityType entityType) {
    return entityType == EntityType.CONNECTORS;
  }
}
