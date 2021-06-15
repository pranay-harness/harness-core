package io.harness.ng.serviceaccounts.service.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.SOWMYA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.NgManagerTestBase;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.category.element.UnitTests;
import io.harness.ng.serviceaccounts.dto.ServiceAccountRequestDTO;
import io.harness.ng.serviceaccounts.entities.ServiceAccount;
import io.harness.ng.serviceaccounts.service.api.ServiceAccountService;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import io.harness.serviceaccount.ServiceAccountDTO;

import java.util.List;
import javax.inject.Inject;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(PL)
public class ServiceAccountServiceImplTest extends NgManagerTestBase {
  @Inject private HPersistence hPersistence;
  @Inject private ServiceAccountService serviceAccountService;

  private String accountIdentifier;
  private String orgIdentifier;
  private String projectIdentifier;
  private String identifier;
  private String name;
  private String description;
  private ServiceAccountRequestDTO serviceAccountRequestDTO;

  @Before
  public void setup() throws IllegalAccessException {
    accountIdentifier = generateUuid();
    orgIdentifier = generateUuid();
    projectIdentifier = generateUuid();
    identifier = generateUuid();
    name = generateUuid();
    description = generateUuid();

    serviceAccountRequestDTO = new ServiceAccountRequestDTO(identifier, name, description);
    FieldUtils.writeField(serviceAccountService, "hPersistence", hPersistence, true);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testCreateServiceAccount() {
    serviceAccountService.createServiceAccount(
        accountIdentifier, orgIdentifier, projectIdentifier, serviceAccountRequestDTO);
    ServiceAccount serviceAccount = hPersistence.createQuery(ServiceAccount.class).get();

    assertThat(serviceAccount.getAccountIdentifier()).isEqualTo(accountIdentifier);
    assertThat(serviceAccount.getOrgIdentifier()).isEqualTo(orgIdentifier);
    assertThat(serviceAccount.getProjectIdentifier()).isEqualTo(projectIdentifier);
    assertThat(serviceAccount.getIdentifier()).isEqualTo(identifier);
    assertThat(serviceAccount.getName()).isEqualTo(name);
    assertThat(serviceAccount.getDescription()).isEqualTo(description);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testCreateServiceAccount_duplicateIdentifier() {
    serviceAccountService.createServiceAccount(
        accountIdentifier, orgIdentifier, projectIdentifier, serviceAccountRequestDTO);
    ServiceAccount serviceAccount = hPersistence.createQuery(ServiceAccount.class).get();
    assertThat(serviceAccount).isNotNull();

    assertThatThrownBy(()
                           -> serviceAccountService.createServiceAccount(
                               accountIdentifier, orgIdentifier, projectIdentifier, serviceAccountRequestDTO))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Duplicate service account with identifier " + identifier + " in scope");
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testUpdateServiceAccount() {
    serviceAccountService.createServiceAccount(
        accountIdentifier, orgIdentifier, projectIdentifier, serviceAccountRequestDTO);
    serviceAccountRequestDTO = new ServiceAccountRequestDTO(identifier, name + "_new", description);
    serviceAccountService.updateServiceAccount(
        accountIdentifier, orgIdentifier, projectIdentifier, identifier, serviceAccountRequestDTO);

    ServiceAccount serviceAccount = hPersistence.createQuery(ServiceAccount.class).get();

    assertThat(serviceAccount.getAccountIdentifier()).isEqualTo(accountIdentifier);
    assertThat(serviceAccount.getOrgIdentifier()).isEqualTo(orgIdentifier);
    assertThat(serviceAccount.getProjectIdentifier()).isEqualTo(projectIdentifier);
    assertThat(serviceAccount.getIdentifier()).isEqualTo(identifier);
    assertThat(serviceAccount.getName()).isEqualTo(name + "_new");
    assertThat(serviceAccount.getDescription()).isEqualTo(description);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testDeleteServiceAccount() {
    serviceAccountService.createServiceAccount(
        accountIdentifier, orgIdentifier, projectIdentifier, serviceAccountRequestDTO);
    ServiceAccount serviceAccount = hPersistence.createQuery(ServiceAccount.class).get();
    assertThat(serviceAccount).isNotNull();
    serviceAccountService.deleteServiceAccount(accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    serviceAccount = hPersistence.createQuery(ServiceAccount.class).get();
    assertThat(serviceAccount).isNull();
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testListServiceAccounts() {
    serviceAccountService.createServiceAccount(
        accountIdentifier, orgIdentifier, projectIdentifier, serviceAccountRequestDTO);
    List<ServiceAccountRequestDTO> accounts =
        serviceAccountService.listServiceAccounts(accountIdentifier, orgIdentifier, projectIdentifier);
    assertThat(accounts.size()).isEqualTo(1);
    accounts = serviceAccountService.listServiceAccounts(accountIdentifier, null, null);
    assertThat(accounts.size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testGetServiceAccountDTO() {
    serviceAccountService.createServiceAccount(
        accountIdentifier, orgIdentifier, projectIdentifier, serviceAccountRequestDTO);
    ServiceAccountDTO account =
        serviceAccountService.getServiceAccountDTO(accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    assertThat(account).isNotNull();
    assertThat(account.getName()).isEqualTo(name);
    assertThat(account.getScope()).isEqualTo(Scope.of(accountIdentifier, orgIdentifier, projectIdentifier));
  }
}
