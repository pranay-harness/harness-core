package io.harness.connector.impl;

import static io.harness.connector.entities.ConnectivityStatus.SUCCESS;
import static io.harness.delegate.beans.connector.ConnectorType.GIT;
import static io.harness.delegate.beans.connector.ConnectorType.KUBERNETES_CLUSTER;
import static io.harness.rule.OwnerRule.DEEPAK;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.connector.ConnectorsTestBase;
import io.harness.connector.apis.dto.ConnectorFilterDTO;
import io.harness.connector.apis.dto.ConnectorListFilter;
import io.harness.connector.entities.ConnectivityStatus;
import io.harness.connector.utils.ConnectorFilterTestHelper;
import io.harness.delegate.beans.connector.ConnectorCategory;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.encryption.Scope;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.bson.Document;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.data.mongodb.core.query.Criteria;

public class ConnectorFilterServiceImplTest extends ConnectorsTestBase {
  @Inject ConnectorFilterServiceImpl connectorFilterService;
  private static final String accountIdentifier = "accountIdentifier";
  private static final String orgIdentifier = "orgIdentifier";
  private static final String projectIdentifier = "projectIdentifier";
  private static final String filterIdentifier = "filterIdentifier";

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void createCriteriaFromConnectorFilter() {
    String accountId = "accountId";
    String name = "name";
    String orgId = "orgId";
    String projectId = "projectId";
    String description = "description";
    String identifier = "identifier";
    String searchTerm = "searchTerm";
    ConnectorListFilter connectorListFilter = ConnectorListFilter.builder()
                                                  .category(Collections.singletonList(ConnectorCategory.CLOUD_PROVIDER))
                                                  .connectivityStatus(Collections.singletonList(SUCCESS))
                                                  .name(Collections.singletonList(name))
                                                  .connectorIdentifier(Collections.singletonList(identifier))
                                                  .description(Collections.singletonList(description))
                                                  .orgIdentifier(Collections.singletonList(orgId))
                                                  .searchTerm(searchTerm)
                                                  .type(Arrays.asList(KUBERNETES_CLUSTER, GIT))
                                                  .build();
    Criteria criteria =
        connectorFilterService.createCriteriaFromConnectorListQueryParams(accountId, connectorListFilter);
    Document criteriaObject = criteria.getCriteriaObject();
    assertThat(criteriaObject.size()).isEqualTo(9);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testGet() {
    ConnectorFilterDTO connectorFilterInput =
        ConnectorFilterTestHelper.createConnectorFilterForTest(orgIdentifier, projectIdentifier, filterIdentifier);
    connectorFilterService.create(accountIdentifier, connectorFilterInput);
    ConnectorFilterDTO connectorFilterOutput =
        connectorFilterService.get(accountIdentifier, orgIdentifier, projectIdentifier, filterIdentifier);
    assertThat(connectorFilterOutput).isNotNull();
    verifyTheValuesAreCorrect(connectorFilterInput, connectorFilterOutput);
  }

  private void verifyTheValuesAreCorrect(
      ConnectorFilterDTO connectorFilterInput, ConnectorFilterDTO connectorFilterOutput) {
    assertThat(connectorFilterOutput.getOrgIdentifier()).isEqualTo(orgIdentifier);
    assertThat(connectorFilterOutput.getProjectIdentifier()).isEqualTo(projectIdentifier);
    assertThat(connectorFilterOutput.getIdentifier()).isEqualTo(filterIdentifier);
    assertThat(connectorFilterOutput.getInheritingCredentialsFromDelegate())
        .isEqualTo(connectorFilterOutput.getInheritingCredentialsFromDelegate());
    assertThat(connectorFilterOutput.getName()).isEqualTo(connectorFilterInput.getName());
    assertThat(connectorFilterOutput.getSearchTerm()).isEqualTo(connectorFilterInput.getSearchTerm());
    assertThat(connectorFilterOutput.getScopes()).isEqualTo(connectorFilterInput.getScopes());
    assertThat(connectorFilterOutput.getConnectivityStatuses())
        .isEqualTo(connectorFilterInput.getConnectivityStatuses());
    assertThat(connectorFilterOutput.getTypes()).isEqualTo(connectorFilterInput.getTypes());
    assertThat(connectorFilterOutput.getConnectorNames()).isEqualTo(connectorFilterInput.getConnectorNames());
    assertThat(connectorFilterOutput.getCategories()).isEqualTo(connectorFilterInput.getCategories());
    assertThat(connectorFilterOutput.getConnectorIdentifiers())
        .isEqualTo(connectorFilterInput.getConnectorIdentifiers());
    assertThat(connectorFilterOutput.getDescriptions()).isEqualTo(connectorFilterInput.getDescriptions());
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testListForAccountLevelFilter() {
    for (int i = 0; i < 5; i++) {
      ConnectorFilterDTO connectorFilterDTO =
          ConnectorFilterTestHelper.createConnectorFilterForTest(null, null, filterIdentifier + i);
      connectorFilterService.create(accountIdentifier, connectorFilterDTO);
    }
    ConnectorFilterDTO connectorFilterDTO =
        ConnectorFilterTestHelper.createConnectorFilterForTest(orgIdentifier, null, filterIdentifier + "i");
    connectorFilterService.create(accountIdentifier, connectorFilterDTO);
    List<ConnectorFilterDTO> filtersList =
        connectorFilterService.list(0, 100, accountIdentifier, null, null, null).getContent();
    assertThat(filtersList.size()).isEqualTo(5);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testListForOrgLevelFilter() {
    for (int i = 0; i < 5; i++) {
      ConnectorFilterDTO connectorFilterDTO =
          ConnectorFilterTestHelper.createConnectorFilterForTest(orgIdentifier, null, filterIdentifier + i);
      connectorFilterService.create(accountIdentifier, connectorFilterDTO);
    }
    ConnectorFilterDTO connectorFilterDTO = ConnectorFilterTestHelper.createConnectorFilterForTest(
        orgIdentifier, projectIdentifier, filterIdentifier + "i");
    connectorFilterService.create(accountIdentifier, connectorFilterDTO);
    List<ConnectorFilterDTO> filtersList =
        connectorFilterService.list(0, 100, accountIdentifier, orgIdentifier, null, null).getContent();
    assertThat(filtersList.size()).isEqualTo(5);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testListForProjectLevelFilter() {
    for (int i = 0; i < 5; i++) {
      ConnectorFilterDTO connectorFilterDTO = ConnectorFilterTestHelper.createConnectorFilterForTest(
          orgIdentifier, projectIdentifier, filterIdentifier + i);
      connectorFilterService.create(accountIdentifier, connectorFilterDTO);
    }
    ConnectorFilterDTO connectorFilterDTO =
        ConnectorFilterTestHelper.createConnectorFilterForTest(null, null, filterIdentifier + "i");
    connectorFilterService.create(accountIdentifier, connectorFilterDTO);
    List<ConnectorFilterDTO> filtersList =
        connectorFilterService.list(0, 100, accountIdentifier, orgIdentifier, projectIdentifier, null).getContent();
    assertThat(filtersList.size()).isEqualTo(5);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testCreate() {
    ConnectorFilterDTO connectorFilterInput =
        ConnectorFilterTestHelper.createConnectorFilterForTest(orgIdentifier, projectIdentifier, filterIdentifier);
    ConnectorFilterDTO createdOne = connectorFilterService.create(accountIdentifier, connectorFilterInput);
    assertThat(createdOne).isNotNull();
    verifyTheValuesAreCorrect(connectorFilterInput, createdOne);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testUpdate() {
    ConnectorFilterDTO connectorFilterInput =
        ConnectorFilterTestHelper.createConnectorFilterForTest(orgIdentifier, projectIdentifier, filterIdentifier);
    ConnectorFilterDTO createdOne = connectorFilterService.create(accountIdentifier, connectorFilterInput);
    assertThat(createdOne).isNotNull();
    ConnectorFilterDTO updatedInput = getUpdatedConnectorFilter();
    ConnectorFilterDTO updatedFilterOutput = connectorFilterService.update(accountIdentifier, updatedInput);
    verifyTheValuesAreCorrect(updatedInput, updatedFilterOutput);
  }

  private ConnectorFilterDTO getUpdatedConnectorFilter() {
    return ConnectorFilterDTO.builder()
        .name("name"
            + "updated")
        .orgIdentifier(orgIdentifier)
        .projectIdentifier(projectIdentifier)
        .identifier(filterIdentifier)
        .categories(Arrays.asList(ConnectorCategory.CLOUD_PROVIDER, ConnectorCategory.ARTIFACTORY))
        .connectorNames(Arrays.asList("Connector 1"))
        .connectivityStatuses(Collections.singletonList(ConnectivityStatus.FAILURE))
        .connectorIdentifiers(Arrays.asList("Connector identifier 1"))
        .descriptions(Arrays.asList("Connector description 1"))
        .inheritingCredentialsFromDelegate(false)
        .scopes(Collections.singletonList(Scope.PROJECT))
        .searchTerm("searchTerm 1")
        .types(Collections.singletonList(ConnectorType.DOCKER))
        .build();
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testDelete() {
    ConnectorFilterDTO connectorFilterInput =
        ConnectorFilterTestHelper.createConnectorFilterForTest(orgIdentifier, projectIdentifier, filterIdentifier);
    connectorFilterService.create(accountIdentifier, connectorFilterInput);
    Boolean deleteResult =
        connectorFilterService.delete(accountIdentifier, orgIdentifier, projectIdentifier, filterIdentifier);
    assertThat(deleteResult).isTrue();
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testDeleteWhenFilterDoesnotExists() {
    connectorFilterService.delete(accountIdentifier, orgIdentifier, projectIdentifier, filterIdentifier);
  }
}
