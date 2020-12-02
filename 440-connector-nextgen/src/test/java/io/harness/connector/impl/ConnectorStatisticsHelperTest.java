package io.harness.connector.impl;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.connector.ConnectorsTestBase;
import io.harness.connector.apis.dto.stats.ConnectorStatistics;
import io.harness.connector.apis.dto.stats.ConnectorStatusStats;
import io.harness.connector.apis.dto.stats.ConnectorTypeStats;
import io.harness.connector.entities.ConnectivityStatus;
import io.harness.connector.entities.Connector;
import io.harness.connector.entities.ConnectorConnectivityDetails;
import io.harness.connector.utils.AWSConnectorTestHelper;
import io.harness.connector.utils.DockerConnectorTestHelper;
import io.harness.connector.utils.KubernetesConnectorTestHelper;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.encryption.Scope;
import io.harness.repositories.ConnectorRepository;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import com.google.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ConnectorStatisticsHelperTest extends ConnectorsTestBase {
  @Inject ConnectorRepository connectorRepository;
  @Inject ConnectorStatisticsHelper connectorStatisticsHelper;
  private static final String accountIdentifier = "accountIdentifier";
  private static final String orgIdentifier = "orgIdentifier";
  private static final String projectIdentifier = "projectIdentifier";
  private static final String identifier = "identifier";

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void getStats() {
    for (int i = 0; i < 5; i++) {
      Connector connector = KubernetesConnectorTestHelper.createK8sConnector(
          accountIdentifier, orgIdentifier, projectIdentifier, identifier, Scope.ACCOUNT);
      setStatus(connector, ConnectivityStatus.SUCCESS);
      connectorRepository.save(connector);
    }

    for (int i = 0; i < 3; i++) {
      Connector connector = AWSConnectorTestHelper.createAWSConnector(
          accountIdentifier, orgIdentifier, projectIdentifier, identifier, Scope.ACCOUNT);
      setStatus(connector, ConnectivityStatus.FAILURE);
      connectorRepository.save(connector);
    }

    for (int i = 0; i < 4; i++) {
      Connector connector = DockerConnectorTestHelper.createDockerConnector(
          accountIdentifier, orgIdentifier, projectIdentifier, identifier, Scope.ACCOUNT);
      setStatus(connector, ConnectivityStatus.FAILURE);
      connectorRepository.save(connector);
    }

    for (int i = 0; i < 6; i++) {
      Connector connector = DockerConnectorTestHelper.createDockerConnector(
          accountIdentifier, orgIdentifier, projectIdentifier, identifier, Scope.ACCOUNT);
      setStatus(connector, ConnectivityStatus.SUCCESS);
      connectorRepository.save(connector);
    }

    ConnectorStatistics connectorStatistics =
        connectorStatisticsHelper.getStats(accountIdentifier, orgIdentifier, projectIdentifier, Scope.ACCOUNT);
    assertThat(connectorStatistics).isNotNull();
    List<ConnectorTypeStats> connectorTypeStatsList = connectorStatistics.getTypeStats();
    assertThat(connectorTypeStatsList.size()).isEqualTo(3);
    Map<ConnectorType, Integer> typeCount = connectorTypeStatsList.stream().collect(
        Collectors.toMap(ConnectorTypeStats::getType, ConnectorTypeStats::getCount));
    assertThat(typeCount.get(ConnectorType.AWS)).isEqualTo(3);
    assertThat(typeCount.get(ConnectorType.DOCKER)).isEqualTo(10);
    assertThat(typeCount.get(ConnectorType.KUBERNETES_CLUSTER)).isEqualTo(5);

    List<ConnectorStatusStats> connectorStatusStatsList = connectorStatistics.getStatusStats();
    assertThat(connectorStatusStatsList.size()).isEqualTo(2);
    Map<ConnectivityStatus, Integer> statusCount = connectorStatusStatsList.stream().collect(
        Collectors.toMap(ConnectorStatusStats::getStatus, ConnectorStatusStats::getCount));
    assertThat(statusCount.get(ConnectivityStatus.SUCCESS)).isEqualTo(11);
    assertThat(statusCount.get(ConnectivityStatus.FAILURE)).isEqualTo(7);
  }

  private void setStatus(Connector connector, ConnectivityStatus status) {
    connector.setStatus(ConnectorConnectivityDetails.builder().status(status).build());
  }
}