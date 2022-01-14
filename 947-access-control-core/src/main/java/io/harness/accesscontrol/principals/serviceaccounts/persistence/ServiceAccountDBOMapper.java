package io.harness.accesscontrol.principals.serviceaccounts.persistence;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.principals.serviceaccounts.ServiceAccount;
import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
public class ServiceAccountDBOMapper {
  public static ServiceAccountDBO toDBO(ServiceAccount object) {
    return ServiceAccountDBO.builder()
        .identifier(object.getIdentifier())
        .scopeIdentifier(object.getScopeIdentifier())
        .createdAt(object.getCreatedAt())
        .lastModifiedAt(object.getLastModifiedAt())
        .version(object.getVersion())
        .build();
  }

  public static ServiceAccount fromDBO(ServiceAccountDBO object) {
    return ServiceAccount.builder()
        .identifier(object.getIdentifier())
        .scopeIdentifier(object.getScopeIdentifier())
        .createdAt(object.getCreatedAt())
        .lastModifiedAt(object.getLastModifiedAt())
        .version(object.getVersion())
        .build();
  }
}
