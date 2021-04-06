package io.harness.pms.rbac;

import io.harness.EntityType;
import io.harness.accesscontrol.clients.ResourceScope;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.data.structure.EmptyPredicate;

import java.util.Map;
import lombok.experimental.UtilityClass;
import org.apache.groovy.util.Maps;

@UtilityClass
@OwnedBy(HarnessTeam.PIPELINE)
public class PipelineReferredEntityPermissionHelper {
  private final String PERMISSION_PLACE_HOLDER = "core_%s_%s";

  public final Map<EntityType, String> coreEntityTypeToPermissionEntityName =
      Maps.of(EntityType.CONNECTORS, NGResourceType.CONNECTOR, EntityType.SECRETS, NGResourceType.SECRETS,
          EntityType.SERVICE, NGResourceType.SERVICE, EntityType.ENVIRONMENT, NGResourceType.ENVIRONMENT);

  public String getPermissionForGivenType(EntityType entityType, boolean isNew) {
    String permission = "runtimeAccess";
    if (isNew) {
      permission = "create";
    }
    if (coreEntityTypeToPermissionEntityName.containsKey(entityType)) {
      return String.format(
          PERMISSION_PLACE_HOLDER, coreEntityTypeToPermissionEntityName.get(entityType).toLowerCase(), permission);
    }
    throw new UnsupportedOperationException();
  }

  /**
   * For create since the identifier does not exist in DB, special handling is required.
   * @param identifierRef
   * @return
   */
  public String getParentResourceIdentifierForCreate(IdentifierRef identifierRef) {
    if (EmptyPredicate.isNotEmpty(identifierRef.getProjectIdentifier())) {
      return identifierRef.getProjectIdentifier();
    }
    if (EmptyPredicate.isNotEmpty(identifierRef.getOrgIdentifier())) {
      return identifierRef.getOrgIdentifier();
    }
    if (EmptyPredicate.isNotEmpty(identifierRef.getAccountIdentifier())) {
      return identifierRef.getAccountIdentifier();
    }
    throw new UnsupportedOperationException();
  }

  /**
   * For create since the identifier does not exist in DB, special handling is required.
   * @param identifierRef
   * @return
   */
  public String getEntityTypeForCreate(IdentifierRef identifierRef) {
    if (EmptyPredicate.isNotEmpty(identifierRef.getProjectIdentifier())) {
      return "PROJECT";
    }
    if (EmptyPredicate.isNotEmpty(identifierRef.getOrgIdentifier())) {
      return "ORGANIZATION";
    }
    if (EmptyPredicate.isNotEmpty(identifierRef.getAccountIdentifier())) {
      return "ACCOUNT";
    }
    throw new UnsupportedOperationException();
  }

  /**
   * For create since the identifier does not exist in DB, special handling is required.
   * @param identifierRef
   * @return
   */
  public ResourceScope getResourceScopeForCreate(IdentifierRef identifierRef) {
    if (EmptyPredicate.isNotEmpty(identifierRef.getProjectIdentifier())) {
      return ResourceScope.builder()
          .accountIdentifier(identifierRef.getAccountIdentifier())
          .orgIdentifier(identifierRef.getOrgIdentifier())
          .build();
    }
    if (EmptyPredicate.isNotEmpty(identifierRef.getOrgIdentifier())) {
      return ResourceScope.builder().accountIdentifier(identifierRef.getAccountIdentifier()).build();
    }
    return ResourceScope.builder().build();
  }

  public String getEntityName(EntityType entityType) {
    return coreEntityTypeToPermissionEntityName.get(entityType);
  }
}
