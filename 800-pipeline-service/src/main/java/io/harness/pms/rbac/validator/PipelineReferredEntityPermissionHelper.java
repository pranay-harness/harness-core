package io.harness.pms.rbac.validator;

import io.harness.EntityType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.Map;
import lombok.experimental.UtilityClass;
import org.apache.groovy.util.Maps;

@UtilityClass
@OwnedBy(HarnessTeam.PIPELINE)
public class PipelineReferredEntityPermissionHelper {
  public final Map<EntityType, String> coreEntityTypeToPermissionEntityName = Maps.of(EntityType.CONNECTORS,
      "connector", EntityType.SECRETS, "secret", EntityType.SERVICE, "service", EntityType.ENVIRONMENT, "environment");

  public String getPermissionForGivenType(EntityType entityType, boolean isNew) {
    String permission = "runtimeAccess";
    if (isNew) {
      permission = "create";
    }
    if (coreEntityTypeToPermissionEntityName.containsKey(entityType)) {
      return String.format("core_%s_%s", coreEntityTypeToPermissionEntityName.get(entityType), permission);
    }
    throw new UnsupportedOperationException();
  }

  public String getEntityName(EntityType entityType) {
    return coreEntityTypeToPermissionEntityName.get(entityType);
  }
}
