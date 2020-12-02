package io.harness.connector.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.apis.dto.ConnectorFilterDTO;
import io.harness.connector.entities.ConnectivityStatus;
import io.harness.connector.entities.ConnectorFilter;
import io.harness.connector.utils.ConnectorFilterTestHelper;
import io.harness.delegate.beans.connector.ConnectorCategory;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.encryption.Scope;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import io.harness.utils.FullyQualifiedIdentifierHelper;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class ConnectorFilterMapperTest extends CategoryTest {
  @InjectMocks ConnectorFilterMapper connectorFilterMapper;
  private static final String accountIdentifier = "accountIdentifier";
  private static final String orgIdentifier = "orgIdentifier";
  private static final String projectIdentifier = "projectIdentifier";
  private static final String filterIdentifier = "filterIdentifier";

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void toConnectorFilter() {
    ConnectorFilterDTO connectorFilterDTO =
        ConnectorFilterTestHelper.createConnectorFilterForTest(orgIdentifier, projectIdentifier, filterIdentifier);
    ConnectorFilter filterEntity = connectorFilterMapper.toConnectorFilter(connectorFilterDTO, accountIdentifier);
    assertThat(filterEntity).isNotNull();
    assertThat(filterEntity.getAccountIdentifier()).isEqualTo(accountIdentifier);
    assertThat(filterEntity.getOrgIdentifier()).isEqualTo(orgIdentifier);
    assertThat(filterEntity.getProjectIdentifier()).isEqualTo(projectIdentifier);
    assertThat(filterEntity.getFullyQualifiedIdentifier())
        .isEqualTo(FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
            accountIdentifier, orgIdentifier, projectIdentifier, filterIdentifier));
    assertThat(filterEntity.getIdentifier()).isEqualTo(filterIdentifier);
    assertThat(filterEntity.getInheritingCredentialsFromDelegate())
        .isEqualTo(connectorFilterDTO.getInheritingCredentialsFromDelegate());
    assertThat(filterEntity.getName()).isEqualTo(connectorFilterDTO.getName());
    assertThat(filterEntity.getSearchTerm()).isEqualTo(connectorFilterDTO.getSearchTerm());
    assertThat(filterEntity.getScopes()).isEqualTo(connectorFilterDTO.getScopes());
    assertThat(filterEntity.getConnectivityStatuses()).isEqualTo(connectorFilterDTO.getConnectivityStatuses());
    assertThat(filterEntity.getTypes()).isEqualTo(connectorFilterDTO.getTypes());
    assertThat(filterEntity.getConnectorNames()).isEqualTo(connectorFilterDTO.getConnectorNames());
    assertThat(filterEntity.getCategories()).isEqualTo(connectorFilterDTO.getCategories());
    assertThat(filterEntity.getConnectorIdentifier()).isEqualTo(connectorFilterDTO.getConnectorIdentifiers());
    assertThat(filterEntity.getDescriptions()).isEqualTo(connectorFilterDTO.getDescriptions());
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void writeDTO() {
    ConnectorFilter filterEntity =
        createConnectorFilterEntityForTest(orgIdentifier, projectIdentifier, filterIdentifier);
    ConnectorFilterDTO filterDTO = connectorFilterMapper.writeDTO(filterEntity);
    assertThat(filterDTO).isNotNull();
    assertThat(filterDTO.getOrgIdentifier()).isEqualTo(orgIdentifier);
    assertThat(filterDTO.getProjectIdentifier()).isEqualTo(projectIdentifier);
    assertThat(filterDTO.getIdentifier()).isEqualTo(filterIdentifier);
    assertThat(filterDTO.getInheritingCredentialsFromDelegate())
        .isEqualTo(filterEntity.getInheritingCredentialsFromDelegate());
    assertThat(filterDTO.getName()).isEqualTo(filterEntity.getName());
    assertThat(filterDTO.getSearchTerm()).isEqualTo(filterEntity.getSearchTerm());
    assertThat(filterDTO.getScopes()).isEqualTo(filterEntity.getScopes());
    assertThat(filterDTO.getConnectivityStatuses()).isEqualTo(filterEntity.getConnectivityStatuses());
    assertThat(filterDTO.getTypes()).isEqualTo(filterEntity.getTypes());
    assertThat(filterDTO.getConnectorNames()).isEqualTo(filterEntity.getConnectorNames());
    assertThat(filterDTO.getCategories()).isEqualTo(filterEntity.getCategories());
    assertThat(filterDTO.getConnectorIdentifiers()).isEqualTo(filterEntity.getConnectorIdentifier());
    assertThat(filterDTO.getDescriptions()).isEqualTo(filterEntity.getDescriptions());
  }

  private ConnectorFilter createConnectorFilterEntityForTest(
      String orgIdentifier, String projectIdentifier, String identifier) {
    return ConnectorFilter.builder()
        .name("name")
        .orgIdentifier(orgIdentifier)
        .projectIdentifier(projectIdentifier)
        .identifier(identifier)
        .categories(Collections.singletonList(ConnectorCategory.CLOUD_PROVIDER))
        .connectorNames(Arrays.asList("Connector 1", "Connector 2"))
        .connectivityStatuses(Collections.singletonList(ConnectivityStatus.SUCCESS))
        .connectorIdentifier(Arrays.asList("Connector identifier 1", "Connector identifier 2"))
        .descriptions(Arrays.asList("Connector description 1", "Connector description 2"))
        .inheritingCredentialsFromDelegate(true)
        .scopes(Collections.singletonList(Scope.ACCOUNT))
        .searchTerm("searchTerm")
        .types(Collections.singletonList(ConnectorType.KUBERNETES_CLUSTER))
        .build();
  }
}