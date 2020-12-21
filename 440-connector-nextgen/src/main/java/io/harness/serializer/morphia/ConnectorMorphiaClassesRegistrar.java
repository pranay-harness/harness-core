package io.harness.serializer.morphia;

import io.harness.connector.entities.Connector;
import io.harness.connector.entities.ConnectorFilter;
import io.harness.connector.entities.embedded.appdynamicsconnector.AppDynamicsConnector;
import io.harness.connector.entities.embedded.artifactoryconnector.ArtifactoryConnector;
import io.harness.connector.entities.embedded.artifactoryconnector.ArtifactoryUserNamePasswordAuthentication;
import io.harness.connector.entities.embedded.awsconnector.AwsAccessKeyCredential;
import io.harness.connector.entities.embedded.awsconnector.AwsConfig;
import io.harness.connector.entities.embedded.awsconnector.AwsIamCredential;
import io.harness.connector.entities.embedded.docker.DockerConnector;
import io.harness.connector.entities.embedded.docker.DockerUserNamePasswordAuthentication;
import io.harness.connector.entities.embedded.gcpconnector.GcpConfig;
import io.harness.connector.entities.embedded.gcpconnector.GcpDelegateDetails;
import io.harness.connector.entities.embedded.gcpconnector.GcpServiceAccountKey;
import io.harness.connector.entities.embedded.gcpkmsconnector.GcpKmsConnector;
import io.harness.connector.entities.embedded.gitconnector.GitConfig;
import io.harness.connector.entities.embedded.gitconnector.GitSSHAuthentication;
import io.harness.connector.entities.embedded.gitconnector.GitUserNamePasswordAuthentication;
import io.harness.connector.entities.embedded.githubconnector.GithubApiAccess;
import io.harness.connector.entities.embedded.githubconnector.GithubConnector;
import io.harness.connector.entities.embedded.githubconnector.GithubHttpAuthentication;
import io.harness.connector.entities.embedded.githubconnector.GithubSshAuthentication;
import io.harness.connector.entities.embedded.githubconnector.GithubTokenApiAccess;
import io.harness.connector.entities.embedded.githubconnector.GithubUsernamePassword;
import io.harness.connector.entities.embedded.githubconnector.GithubUsernameToken;
import io.harness.connector.entities.embedded.gitlabconnector.GitlabConnector;
import io.harness.connector.entities.embedded.gitlabconnector.GitlabHttpAuthentication;
import io.harness.connector.entities.embedded.gitlabconnector.GitlabSshAuthentication;
import io.harness.connector.entities.embedded.gitlabconnector.GitlabTokenApiAccess;
import io.harness.connector.entities.embedded.gitlabconnector.GitlabUsernamePassword;
import io.harness.connector.entities.embedded.gitlabconnector.GitlabUsernameToken;
import io.harness.connector.entities.embedded.jira.JiraConnector;
import io.harness.connector.entities.embedded.kubernetescluster.K8sClientKeyCert;
import io.harness.connector.entities.embedded.kubernetescluster.K8sOpenIdConnect;
import io.harness.connector.entities.embedded.kubernetescluster.K8sServiceAccount;
import io.harness.connector.entities.embedded.kubernetescluster.K8sUserNamePassword;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesClusterConfig;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesClusterDetails;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesDelegateDetails;
import io.harness.connector.entities.embedded.localconnector.LocalConnector;
import io.harness.connector.entities.embedded.nexusconnector.NexusConnector;
import io.harness.connector.entities.embedded.nexusconnector.NexusUserNamePasswordAuthentication;
import io.harness.connector.entities.embedded.splunkconnector.SplunkConnector;
import io.harness.connector.entities.embedded.vaultconnector.VaultConnector;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;

import java.util.Set;

public class ConnectorMorphiaClassesRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(Connector.class);
    set.add(KubernetesClusterConfig.class);
    set.add(GitConfig.class);
    set.add(VaultConnector.class);
    set.add(GcpKmsConnector.class);
    set.add(LocalConnector.class);
    set.add(AppDynamicsConnector.class);
    set.add(SplunkConnector.class);
    set.add(DockerConnector.class);
    set.add(GcpConfig.class);
    set.add(AwsConfig.class);
    set.add(ArtifactoryConnector.class);
    set.add(JiraConnector.class);
    set.add(NexusConnector.class);
    set.add(ConnectorFilter.class);
    set.add(GithubConnector.class);
    set.add(GitlabConnector.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    h.put("connector.entities.embedded.kubernetescluster.KubernetesDelegateDetails", KubernetesDelegateDetails.class);
    h.put("connector.entities.embedded.kubernetescluster.KubernetesClusterDetails", KubernetesClusterDetails.class);
    h.put("connector.entities.embedded.kubernetescluster.K8sClientKeyCert", K8sClientKeyCert.class);
    h.put("connector.entities.embedded.kubernetescluster.K8sOpenIdConnect", K8sOpenIdConnect.class);
    h.put("connector.entities.embedded.kubernetescluster.K8sServiceAccount", K8sServiceAccount.class);
    h.put("connector.entities.embedded.kubernetescluster.K8sUserNamePassword", K8sUserNamePassword.class);
    h.put("connector.entities.embedded.gitconnector.GitSSHAuthentication", GitSSHAuthentication.class);
    h.put("connector.entities.embedded.gitconnector.GitUserNamePasswordAuthentication",
        GitUserNamePasswordAuthentication.class);
    h.put("connector.entities.embedded.gcpconnector.GcpDelegateDetails", GcpDelegateDetails.class);
    h.put("connector.entities.embedded.gcpconnector.GcpServiceAccountKey", GcpServiceAccountKey.class);
    h.put("connector.entities.embedded.awsconnector.AwsIamCredential", AwsIamCredential.class);
    h.put("connector.entities.embedded.awsconnector.AwsAccessKeyCredential", AwsAccessKeyCredential.class);
    h.put("connector.entities.embedded.docker.DockerUserNamePasswordAuthentication",
        DockerUserNamePasswordAuthentication.class);
    h.put("connector.entities.embedded.artifactoryconnector.ArtifactoryUserNamePasswordAuthentication",
        ArtifactoryUserNamePasswordAuthentication.class);
    h.put("connector.entities.embedded.nexusconnector.NexusUserNamePasswordAuthentication",
        NexusUserNamePasswordAuthentication.class);

    h.put("connector.entities.embedded.githubconnector.GithubAppApiAccess", GithubApiAccess.class);
    h.put("connector.entities.embedded.githubconnector.GithubTokenApiAccess", GithubTokenApiAccess.class);
    h.put("connector.entities.embedded.githubconnector.GithubSshAuthentication", GithubSshAuthentication.class);
    h.put("connector.entities.embedded.githubconnector.GithubHttpAuthentication", GithubHttpAuthentication.class);
    h.put("connector.entities.embedded.githubconnector.GithubUsernamePassword", GithubUsernamePassword.class);
    h.put("connector.entities.embedded.githubconnector.GithubUsernameToken", GithubUsernameToken.class);

    h.put("connector.entities.embedded.gitlabconnector.GitlabTokenApiAccess", GitlabTokenApiAccess.class);
    h.put("connector.entities.embedded.gitlabconnector.GitlabSshAuthentication", GitlabSshAuthentication.class);
    h.put("connector.entities.embedded.gitlabconnector.GitlabHttpAuthentication", GitlabHttpAuthentication.class);
    h.put("connector.entities.embedded.gitlabconnector.GitlabUsernamePassword", GitlabUsernamePassword.class);
    h.put("connector.entities.embedded.gitlabconnector.GitlabUsernameToken", GitlabUsernameToken.class);
  }
}
