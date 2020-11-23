package io.harness.delegate.beans.connector;

import io.harness.beans.DecryptableEntity;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectorDTO;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryConnectorDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureContainerRegistryConnectorDTO;
import io.harness.delegate.beans.connector.docker.DockerConnectorDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorDTO;
import io.harness.delegate.beans.connector.gcpkmsconnector.GcpKmsConnectorDTO;
import io.harness.delegate.beans.connector.gitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.jira.JiraConnectorDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.localconnector.LocalConnectorDTO;
import io.harness.delegate.beans.connector.nexusconnector.NexusConnectorDTO;
import io.harness.delegate.beans.connector.splunkconnector.SplunkConnectorDTO;
import io.harness.delegate.beans.connector.vaultconnector.VaultConnectorDTO;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;

@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(value = "ConnectorConfigDTO",
    subTypes = {KubernetesClusterConfigDTO.class, GitConfigDTO.class, DockerConnectorDTO.class,
        SplunkConnectorDTO.class, AppDynamicsConnectorDTO.class, VaultConnectorDTO.class, LocalConnectorDTO.class,
        GcpKmsConnectorDTO.class, GcpConnectorDTO.class, AwsConnectorDTO.class, ArtifactoryConnectorDTO.class,
        JiraConnectorDTO.class, NexusConnectorDTO.class, AzureContainerRegistryConnectorDTO.class},
    discriminator = "type")
public abstract class ConnectorConfigDTO {
  @JsonIgnore public abstract DecryptableEntity getDecryptableEntity();
}
