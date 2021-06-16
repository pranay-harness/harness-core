package io.harness.ng.serviceaccounts.service;

import io.harness.beans.Scope;
import io.harness.ng.core.mapper.ResourceScopeMapper;
import io.harness.ng.serviceaccounts.entities.ServiceAccount;
import io.harness.serviceaccount.ServiceAccountDTO;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ServiceAccountDTOMapper {
  public ServiceAccountDTO getDTOFromServiceAccount(ServiceAccount serviceAccount) {
    return ServiceAccountDTO.builder()
        .identifier(serviceAccount.getIdentifier())
        .name(serviceAccount.getName())
        .description(serviceAccount.getDescription())
        .resourceScope(ResourceScopeMapper.getResourceScope(Scope.of(serviceAccount.getAccountIdentifier(),
            serviceAccount.getOrgIdentifier(), serviceAccount.getProjectIdentifier())))
        .build();
  }
}
