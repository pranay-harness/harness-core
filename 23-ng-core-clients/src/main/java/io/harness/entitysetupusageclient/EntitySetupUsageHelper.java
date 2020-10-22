package io.harness.entitysetupusageclient;

import com.google.inject.Singleton;

import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.entitysetupusage.dto.EntitySetupUsageDTO;

@Singleton
public class EntitySetupUsageHelper {
  public EntitySetupUsageDTO createEntityReference(
      String accountIdentifier, EntityDetail referredEntity, EntityDetail referredByEntity) {
    return EntitySetupUsageDTO.builder()
        .accountIdentifier(accountIdentifier)
        .referredEntity(referredEntity)
        .referredByEntity(referredByEntity)
        .build();
  }
}
