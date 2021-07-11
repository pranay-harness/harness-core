package io.harness.ng.serviceaccounts.service;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.mapper.TagMapper;
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
        .email(serviceAccount.getEmail())
        .description(serviceAccount.getDescription())
        .tags(TagMapper.convertToMap(serviceAccount.getTags()))
        .accountIdentifier(serviceAccount.getAccountIdentifier())
        .orgIdentifier(serviceAccount.getOrgIdentifier())
        .projectIdentifier(serviceAccount.getProjectIdentifier())
        .build();
  }

  public ServiceAccount getServiceAccountFromDTO(ServiceAccountDTO serviceAccountDTO) {
    return ServiceAccount.builder()
        .accountIdentifier(serviceAccountDTO.getAccountIdentifier())
        .orgIdentifier(serviceAccountDTO.getOrgIdentifier())
        .projectIdentifier(serviceAccountDTO.getProjectIdentifier())
        .name(serviceAccountDTO.getName())
        .identifier(serviceAccountDTO.getIdentifier())
        .description(serviceAccountDTO.getDescription())
        .email(serviceAccountDTO.getEmail().toLowerCase())
        .tags(TagMapper.convertToList(serviceAccountDTO.getTags()))
        .build();
  }
}
