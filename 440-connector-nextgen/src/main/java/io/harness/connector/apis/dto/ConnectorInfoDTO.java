package io.harness.connector.apis.dto;

import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.ExecutionCapabilityDemanderWithScope;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectorDTO;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryConnectorDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.docker.DockerConnectorDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorDTO;
import io.harness.delegate.beans.connector.gitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.jira.JiraConnectorDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.localconnector.LocalConnectorDTO;
import io.harness.delegate.beans.connector.nexusconnector.NexusConnectorDTO;
import io.harness.delegate.beans.connector.splunkconnector.SplunkConnectorDTO;
import io.harness.delegate.beans.connector.vaultconnector.VaultConnectorDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.secretmanagerclient.dto.GcpKmsConfigDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConnectorInfoDTO {
  @NotNull String name;
  @NotNull String identifier;
  String description;
  String orgIdentifier;
  String projectIdentifier;
  List<String> tags;

  @NotNull @JsonProperty("type") ConnectorType connectorType;

  @JsonProperty("spec")
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
  @JsonSubTypes({
    @JsonSubTypes.Type(value = KubernetesClusterConfigDTO.class, name = "K8sCluster")
    , @JsonSubTypes.Type(value = GitConfigDTO.class, name = "Git"),
        @JsonSubTypes.Type(value = SplunkConnectorDTO.class, name = "Splunk"),
        @JsonSubTypes.Type(value = AppDynamicsConnectorDTO.class, name = "AppDynamics"),
        @JsonSubTypes.Type(value = VaultConnectorDTO.class, name = "Vault"),
        @JsonSubTypes.Type(value = DockerConnectorDTO.class, name = "DockerRegistry"),
        @JsonSubTypes.Type(value = LocalConnectorDTO.class, name = "Local"),
        @JsonSubTypes.Type(value = GcpKmsConfigDTO.class, name = "GcpKms"),
        @JsonSubTypes.Type(value = GcpConnectorDTO.class, name = "Gcp"),
        @JsonSubTypes.Type(value = AwsConnectorDTO.class, name = "Aws"),
        @JsonSubTypes.Type(value = ArtifactoryConnectorDTO.class, name = "Artifactory"),
        @JsonSubTypes.Type(value = JiraConnectorDTO.class, name = "Jira"),
        @JsonSubTypes.Type(value = NexusConnectorDTO.class, name = "Nexus")
  })
  @Valid
  @NotNull
  ConnectorConfigDTO connectorConfig;

  @Builder
  public ConnectorInfoDTO(String name, String identifier, String description, String orgIdentifier,
      String projectIdentifier, List<String> tags, ConnectorType connectorType, ConnectorConfigDTO connectorConfig) {
    this.name = name;
    this.identifier = identifier;
    this.description = description;
    this.orgIdentifier = orgIdentifier;
    this.projectIdentifier = projectIdentifier;
    this.tags = tags;
    this.connectorType = connectorType;
    this.connectorConfig = connectorConfig;
  }

  public List<ExecutionCapability> fetchRequiredExecutionCapabilitiesForConnector(String accountIdentifier) {
    if (this.connectorConfig instanceof ExecutionCapabilityDemanderWithScope) {
      return ((ExecutionCapabilityDemanderWithScope) this.connectorConfig)
          .fetchRequiredExecutionCapabilities(accountIdentifier, orgIdentifier, projectIdentifier);
    } else if (this.connectorConfig instanceof ExecutionCapabilityDemander) {
      return ((ExecutionCapabilityDemander) this.connectorConfig).fetchRequiredExecutionCapabilities();
    }
    throw new UnsupportedOperationException(
        "The execution capability is not defined for the connector type " + connectorType);
  }
}
