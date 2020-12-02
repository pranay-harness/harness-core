package io.harness.connector.utils;

import io.harness.connector.apis.dto.ConnectorFilterDTO;
import io.harness.connector.entities.ConnectivityStatus;
import io.harness.delegate.beans.connector.ConnectorCategory;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.encryption.Scope;

import java.util.Arrays;
import java.util.Collections;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ConnectorFilterTestHelper {
  public ConnectorFilterDTO createConnectorFilterForTest(
      String orgIdentifier, String projectIdentifier, String identifier) {
    return ConnectorFilterDTO.builder()
        .name("name")
        .orgIdentifier(orgIdentifier)
        .projectIdentifier(projectIdentifier)
        .identifier(identifier)
        .categories(Collections.singletonList(ConnectorCategory.CLOUD_PROVIDER))
        .connectorNames(Arrays.asList("Connector 1", "Connector 2"))
        .connectivityStatuses(Collections.singletonList(ConnectivityStatus.SUCCESS))
        .connectorIdentifiers(Arrays.asList("Connector identifier 1", "Connector identifier 2"))
        .descriptions(Arrays.asList("Connector description 1", "Connector description 2"))
        .inheritingCredentialsFromDelegate(true)
        .scopes(Collections.singletonList(Scope.ACCOUNT))
        .searchTerm("searchTerm")
        .types(Collections.singletonList(ConnectorType.KUBERNETES_CLUSTER))
        .build();
  }
}
