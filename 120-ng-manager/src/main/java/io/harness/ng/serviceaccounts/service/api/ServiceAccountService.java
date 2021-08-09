package io.harness.ng.serviceaccounts.service.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ServiceAccountFilterDTO;
import io.harness.ng.serviceaccounts.dto.ServiceAccountAggregateDTO;
import io.harness.serviceaccount.ServiceAccountDTO;

import java.util.List;
import org.springframework.data.domain.Pageable;

@OwnedBy(PL)
public interface ServiceAccountService {
  ServiceAccountDTO createServiceAccount(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, ServiceAccountDTO requestDTO);
  List<ServiceAccountDTO> listServiceAccounts(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, List<String> identifiers);
  ServiceAccountDTO updateServiceAccount(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String identifier, ServiceAccountDTO requestDTO);
  boolean deleteServiceAccount(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);
  ServiceAccountDTO getServiceAccountDTO(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);
  PageResponse<ServiceAccountAggregateDTO> listAggregateServiceAccounts(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, List<String> identifiers, Pageable pageable, ServiceAccountFilterDTO filterDTO);

  ServiceAccountAggregateDTO getServiceAccountAggregateDTO(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);
}
