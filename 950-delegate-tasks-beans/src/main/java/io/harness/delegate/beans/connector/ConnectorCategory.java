package io.harness.delegate.beans.connector;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;

public enum ConnectorCategory {
  CLOUD_PROVIDER(new HashSet(Arrays.asList(ConnectorType.KUBERNETES_CLUSTER, ConnectorType.GCP, ConnectorType.AWS))),
  SECRET_MANAGER(new HashSet(
      Arrays.asList(ConnectorType.VAULT, ConnectorType.KMS, ConnectorType.GCP_KMS, ConnectorType.AWS_SECRETS_MANAGER,
          ConnectorType.AZURE_VAULT, ConnectorType.CYBERARK, ConnectorType.CUSTOM, ConnectorType.LOCAL))),
  CONNECTOR(new HashSet(Arrays.asList())),
  ARTIFACTORY(new HashSet(Arrays.asList(ConnectorType.DOCKER, ConnectorType.ARTIFACTORY, ConnectorType.NEXUS))),
  CODE_REPO(
      new HashSet(Arrays.asList(ConnectorType.GITHUB, ConnectorType.GITLAB))), // removed ConnectorType.GIT for now.
  MONITORING(new HashSet(Arrays.asList(ConnectorType.SPLUNK, ConnectorType.APP_DYNAMICS))),
  TICKETING(new HashSet(Arrays.asList(ConnectorType.JIRA)));

  @Getter private Set<ConnectorType> connectors;

  ConnectorCategory(Set<ConnectorType> connectors) {
    this.connectors = connectors;
  }
}
