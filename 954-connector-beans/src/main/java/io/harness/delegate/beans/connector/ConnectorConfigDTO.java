package io.harness.delegate.beans.connector;

import io.harness.beans.DecryptableEntity;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectorDTO;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryConnectorDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.ceawsconnector.CEAwsConnectorDTO;
import io.harness.delegate.beans.connector.ceazure.CEAzureConnectorDTO;
import io.harness.delegate.beans.connector.cek8s.CEKubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.docker.DockerConnectorDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorDTO;
import io.harness.delegate.beans.connector.gcpkmsconnector.GcpKmsConnectorDTO;
import io.harness.delegate.beans.connector.helm.HttpHelmConnectorDTO;
import io.harness.delegate.beans.connector.jira.JiraConnectorDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.localconnector.LocalConnectorDTO;
import io.harness.delegate.beans.connector.newrelic.NewRelicConnectorDTO;
import io.harness.delegate.beans.connector.nexusconnector.NexusConnectorDTO;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitConnectorDTO;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketConnectorDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.scm.github.GithubConnectorDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabConnectorDTO;
import io.harness.delegate.beans.connector.splunkconnector.SplunkConnectorDTO;
import io.harness.delegate.beans.connector.vaultconnector.VaultConnectorDTO;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = KubernetesClusterConfigDTO.class, name = "K8sCluster")
  , @JsonSubTypes.Type(value = GitConfigDTO.class, name = "Git"),
      @JsonSubTypes.Type(value = SplunkConnectorDTO.class, name = "Splunk"),
      @JsonSubTypes.Type(value = AppDynamicsConnectorDTO.class, name = "AppDynamics"),
      @JsonSubTypes.Type(value = NewRelicConnectorDTO.class, name = "NewRelic"),
      @JsonSubTypes.Type(value = VaultConnectorDTO.class, name = "Vault"),
      @JsonSubTypes.Type(value = DockerConnectorDTO.class, name = "DockerRegistry"),
      @JsonSubTypes.Type(value = LocalConnectorDTO.class, name = "Local"),
      @JsonSubTypes.Type(value = GcpKmsConnectorDTO.class, name = "GcpKms"),
      @JsonSubTypes.Type(value = GcpConnectorDTO.class, name = "Gcp"),
      @JsonSubTypes.Type(value = AwsConnectorDTO.class, name = "Aws"),
      @JsonSubTypes.Type(value = CEAwsConnectorDTO.class, name = "CEAws"),
      @JsonSubTypes.Type(value = ArtifactoryConnectorDTO.class, name = "Artifactory"),
      @JsonSubTypes.Type(value = JiraConnectorDTO.class, name = "Jira"),
      @JsonSubTypes.Type(value = NexusConnectorDTO.class, name = "Nexus"),
      @JsonSubTypes.Type(value = GithubConnectorDTO.class, name = "Github"),
      @JsonSubTypes.Type(value = GitlabConnectorDTO.class, name = "Gitlab"),
      @JsonSubTypes.Type(value = BitbucketConnectorDTO.class, name = "Bitbucket"),
      @JsonSubTypes.Type(value = AwsCodeCommitConnectorDTO.class, name = "Codecommit"),
      @JsonSubTypes.Type(value = CEAzureConnectorDTO.class, name = "CEAzure"),
      @JsonSubTypes.Type(value = CEKubernetesClusterConfigDTO.class, name = "CEK8sCluster"),
      @JsonSubTypes.Type(value = HttpHelmConnectorDTO.class, name = "HttpHelmRepo")
})
public abstract class ConnectorConfigDTO implements DecryptableEntity {
  @JsonIgnore public abstract List<DecryptableEntity> getDecryptableEntities();

  public void validate() {
    // no op implementation which base classes can override
  }
}
