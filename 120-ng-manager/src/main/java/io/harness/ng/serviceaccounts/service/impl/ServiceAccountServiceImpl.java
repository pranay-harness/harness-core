package io.harness.ng.serviceaccounts.service.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.datacollection.utils.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.ng.serviceaccounts.dto.ServiceAccountRequestDTO;
import io.harness.ng.serviceaccounts.entities.ServiceAccount;
import io.harness.ng.serviceaccounts.entities.ServiceAccount.ServiceAccountKeys;
import io.harness.ng.serviceaccounts.service.api.ServiceAccountService;
import io.harness.persistence.HPersistence;
import io.harness.serviceaccount.ServiceAccountDTO;

import com.hazelcast.util.Preconditions;
import groovy.util.logging.Slf4j;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.mongodb.morphia.query.Query;

@Slf4j
@OwnedBy(PL)
public class ServiceAccountServiceImpl implements ServiceAccountService {
  @Inject private HPersistence hPersistence;

  @Override
  public void createServiceAccount(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, ServiceAccountRequestDTO requestDTO) {
    ServiceAccount existingAccount = hPersistence.createQuery(ServiceAccount.class)
                                         .filter(ServiceAccountKeys.accountIdentifier, accountIdentifier)
                                         .filter(ServiceAccountKeys.orgIdentifier, orgIdentifier)
                                         .filter(ServiceAccountKeys.projectIdentifier, projectIdentifier)
                                         .filter(ServiceAccountKeys.identifier, requestDTO.getIdentifier())
                                         .get();
    Preconditions.checkState(existingAccount == null,
        "Duplicate service account with identifier " + requestDTO.getIdentifier() + " in scope");
    ServiceAccount serviceAccount = ServiceAccount.builder()
                                        .accountIdentifier(accountIdentifier)
                                        .orgIdentifier(orgIdentifier)
                                        .projectIdentifier(projectIdentifier)
                                        .name(requestDTO.getName())
                                        .identifier(requestDTO.getIdentifier())
                                        .description(requestDTO.getDescription())
                                        .build();
    hPersistence.save(serviceAccount);
  }

  @Override
  public void updateServiceAccount(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String identifier, ServiceAccountRequestDTO requestDTO) {
    ServiceAccount serviceAccount = hPersistence.createQuery(ServiceAccount.class)
                                        .filter(ServiceAccountKeys.accountIdentifier, accountIdentifier)
                                        .filter(ServiceAccountKeys.orgIdentifier, orgIdentifier)
                                        .filter(ServiceAccountKeys.projectIdentifier, projectIdentifier)
                                        .filter(ServiceAccountKeys.identifier, identifier)
                                        .get();
    Preconditions.checkNotNull(serviceAccount, "Service account with identifier" + identifier + " doesn't exist");
    serviceAccount.setName(requestDTO.getName());
    serviceAccount.setDescription(requestDTO.getDescription());
    hPersistence.save(serviceAccount);
  }

  @Override
  public void deleteServiceAccount(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    Query<ServiceAccount> query = hPersistence.createQuery(ServiceAccount.class)
                                      .filter(ServiceAccountKeys.accountIdentifier, accountIdentifier)
                                      .filter(ServiceAccountKeys.orgIdentifier, orgIdentifier)
                                      .filter(ServiceAccountKeys.projectIdentifier, projectIdentifier)
                                      .filter(ServiceAccountKeys.identifier, identifier);
    hPersistence.delete(query);
  }

  @Override
  public ServiceAccountDTO getServiceAccountDTO(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    ServiceAccount serviceAccount = hPersistence.createQuery(ServiceAccount.class)
                                        .filter(ServiceAccountKeys.accountIdentifier, accountIdentifier)
                                        .filter(ServiceAccountKeys.orgIdentifier, orgIdentifier)
                                        .filter(ServiceAccountKeys.projectIdentifier, projectIdentifier)
                                        .filter(ServiceAccountKeys.identifier, identifier)
                                        .get();
    Preconditions.checkNotNull(serviceAccount, "Service account with identifier" + identifier + " doesn't exist");
    return ServiceAccountDTO.builder()
        .name(serviceAccount.getName())
        .scope(Scope.of(accountIdentifier, orgIdentifier, projectIdentifier))
        .build();
  }

  @Override
  public List<ServiceAccountRequestDTO> listServiceAccounts(
      String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    List<ServiceAccount> serviceAccounts = hPersistence.createQuery(ServiceAccount.class)
                                               .filter(ServiceAccountKeys.accountIdentifier, accountIdentifier)
                                               .filter(ServiceAccountKeys.orgIdentifier, orgIdentifier)
                                               .filter(ServiceAccountKeys.projectIdentifier, projectIdentifier)
                                               .asList();
    List<ServiceAccountRequestDTO> serviceAccountRequestDTOS = new ArrayList<>();
    if (isNotEmpty(serviceAccounts)) {
      serviceAccounts.forEach(serviceAccount
          -> serviceAccountRequestDTOS.add(new ServiceAccountRequestDTO(
              serviceAccount.getIdentifier(), serviceAccount.getName(), serviceAccount.getDescription())));
    }
    return serviceAccountRequestDTOS;
  }
}
