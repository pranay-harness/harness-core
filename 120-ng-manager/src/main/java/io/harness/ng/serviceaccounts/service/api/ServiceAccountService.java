package io.harness.ng.serviceaccounts.service.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.serviceaccounts.dto.ServiceAccountRequestDTO;
import io.harness.serviceaccount.ServiceAccountDTO;

import java.util.List;

@OwnedBy(PL)
public interface ServiceAccountService {
  void createServiceAccount(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, ServiceAccountRequestDTO requestDTO);
  List<ServiceAccountRequestDTO> listServiceAccounts(
      String accountIdentifier, String orgIdentifier, String projectIdentifier);
  void updateServiceAccount(String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier,
      ServiceAccountRequestDTO requestDTO);
  void deleteServiceAccount(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);
  ServiceAccountDTO getServiceAccountDTO(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);
}
