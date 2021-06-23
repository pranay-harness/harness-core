package io.harness.ng.serviceaccounts.service;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.serviceaccounts.entities.ServiceAccount;
import io.harness.serviceaccount.ServiceAccountDTO;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(PL)
public class ServiceAccountDTOMapper {
  public ServiceAccountDTO getDTOFromServiceAccount(ServiceAccount serviceAccount) {
    return ServiceAccountDTO.builder()
        .identifier(serviceAccount.getIdentifier())
        .name(serviceAccount.getName())
        .description(serviceAccount.getDescription())
        .accountIdentifier(serviceAccount.getAccountIdentifier())
        .orgIdentifier(serviceAccount.getOrgIdentifier())
        .projectIdentifier(serviceAccount.getProjectIdentifier())
        .build();
  }
}
