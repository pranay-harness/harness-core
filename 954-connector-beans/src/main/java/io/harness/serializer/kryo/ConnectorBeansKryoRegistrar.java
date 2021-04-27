package io.harness.serializer.kryo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsAuthType;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectorDTO;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryAuthCredentialsDTO;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryAuthType;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryAuthenticationDTO;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryConnectorDTO;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryConstants;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryUsernamePasswordAuthDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsConstants;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialSpecDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialType;
import io.harness.delegate.beans.connector.awsconnector.AwsInheritFromDelegateSpecDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsManualConfigSpecDTO;
import io.harness.delegate.beans.connector.awsconnector.CrossAccountAccessDTO;
import io.harness.delegate.beans.connector.awskmsconnector.AwsKmsConnectorCredentialDTO;
import io.harness.delegate.beans.connector.awskmsconnector.AwsKmsConnectorDTO;
import io.harness.delegate.beans.connector.awskmsconnector.AwsKmsConstants;
import io.harness.delegate.beans.connector.awskmsconnector.AwsKmsCredentialSpecAssumeIAMDTO;
import io.harness.delegate.beans.connector.awskmsconnector.AwsKmsCredentialSpecAssumeSTSDTO;
import io.harness.delegate.beans.connector.awskmsconnector.AwsKmsCredentialSpecDTO;
import io.harness.delegate.beans.connector.awskmsconnector.AwsKmsCredentialSpecManualConfigDTO;
import io.harness.delegate.beans.connector.awskmsconnector.AwsKmsCredentialType;
import io.harness.delegate.beans.connector.ceawsconnector.AwsCurAttributesDTO;
import io.harness.delegate.beans.connector.ceawsconnector.CEAwsConnectorDTO;
import io.harness.delegate.beans.connector.ceawsconnector.CEAwsFeatures;
import io.harness.delegate.beans.connector.ceazure.BillingExportSpecDTO;
import io.harness.delegate.beans.connector.ceazure.CEAzureConnectorDTO;
import io.harness.delegate.beans.connector.ceazure.CEAzureFeatures;
import io.harness.delegate.beans.connector.cek8s.CEKubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.docker.DockerAuthCredentialsDTO;
import io.harness.delegate.beans.connector.docker.DockerAuthType;
import io.harness.delegate.beans.connector.docker.DockerAuthenticationDTO;
import io.harness.delegate.beans.connector.docker.DockerConnectorDTO;
import io.harness.delegate.beans.connector.docker.DockerRegistryProviderType;
import io.harness.delegate.beans.connector.docker.DockerUserNamePasswordDTO;
import io.harness.delegate.beans.connector.gcpccm.GcpCloudCostConnectorDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorCredentialDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpConstants;
import io.harness.delegate.beans.connector.gcpconnector.GcpCredentialType;
import io.harness.delegate.beans.connector.gcpconnector.GcpDelegateDetailsDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpManualDetailsDTO;
import io.harness.delegate.beans.connector.gcpkmsconnector.GcpKmsConnectorDTO;
import io.harness.delegate.beans.connector.helm.HttpHelmAuthCredentialsDTO;
import io.harness.delegate.beans.connector.helm.HttpHelmAuthType;
import io.harness.delegate.beans.connector.helm.HttpHelmAuthenticationDTO;
import io.harness.delegate.beans.connector.helm.HttpHelmConnectorDTO;
import io.harness.delegate.beans.connector.helm.HttpHelmUsernamePasswordDTO;
import io.harness.delegate.beans.connector.jira.JiraConnectorDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthCredentialDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthType;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClientKeyCertDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialSpecDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType;
import io.harness.delegate.beans.connector.k8Connector.KubernetesDelegateDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesOpenIdConnectDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesServiceAccountDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesUserNamePasswordDTO;
import io.harness.delegate.beans.connector.localconnector.LocalConnectorDTO;
import io.harness.delegate.beans.connector.newrelic.NewRelicConnectorDTO;
import io.harness.delegate.beans.connector.nexusconnector.NexusAuthCredentialsDTO;
import io.harness.delegate.beans.connector.nexusconnector.NexusAuthType;
import io.harness.delegate.beans.connector.nexusconnector.NexusAuthenticationDTO;
import io.harness.delegate.beans.connector.nexusconnector.NexusConnectorDTO;
import io.harness.delegate.beans.connector.nexusconnector.NexusConstants;
import io.harness.delegate.beans.connector.nexusconnector.NexusUsernamePasswordAuthDTO;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitAuthType;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitConnectorDTO;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitHttpsAuthType;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitHttpsCredentialsDTO;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitSecretKeyAccessKeyDTO;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitUrlType;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketApiAccessDTO;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketApiAccessSpecDTO;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketApiAccessType;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketConnectorDTO;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketHttpAuthenticationType;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketHttpCredentialsDTO;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketSshCredentialsDTO;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketUsernamePasswordDTO;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketUsernameTokenApiAccessDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.CustomCommitAttributes;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitHTTPAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitSSHAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitSyncConfig;
import io.harness.delegate.beans.connector.scm.github.GithubApiAccessDTO;
import io.harness.delegate.beans.connector.scm.github.GithubApiAccessSpecDTO;
import io.harness.delegate.beans.connector.scm.github.GithubApiAccessType;
import io.harness.delegate.beans.connector.scm.github.GithubAppSpecDTO;
import io.harness.delegate.beans.connector.scm.github.GithubAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.github.GithubConnectorDTO;
import io.harness.delegate.beans.connector.scm.github.GithubCredentialsDTO;
import io.harness.delegate.beans.connector.scm.github.GithubHttpAuthenticationType;
import io.harness.delegate.beans.connector.scm.github.GithubHttpCredentialsDTO;
import io.harness.delegate.beans.connector.scm.github.GithubHttpCredentialsSpecDTO;
import io.harness.delegate.beans.connector.scm.github.GithubSshCredentialsDTO;
import io.harness.delegate.beans.connector.scm.github.GithubTokenSpecDTO;
import io.harness.delegate.beans.connector.scm.github.GithubUsernamePasswordDTO;
import io.harness.delegate.beans.connector.scm.github.GithubUsernameTokenDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabApiAccessDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabApiAccessSpecDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabApiAccessType;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabConnectorDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabHttpAuthenticationType;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabHttpCredentialsDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabKerberosDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabSshCredentialsDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabTokenSpecDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabUsernamePasswordDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabUsernameTokenDTO;
import io.harness.delegate.beans.connector.splunkconnector.SplunkConnectorDTO;
import io.harness.delegate.beans.connector.vaultconnector.VaultConnectorDTO;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;
import java.util.LinkedHashSet;
@OwnedBy(HarnessTeam.PL)
public class ConnectorBeansKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(ConnectorDTO.class, 26001);
    kryo.register(ConnectorInfoDTO.class, 26002);
    kryo.register(AppDynamicsConnectorDTO.class, 19105);
    kryo.register(CustomCommitAttributes.class, 19070);
    kryo.register(GitAuthenticationDTO.class, 19063);
    kryo.register(GitAuthType.class, 19066);
    kryo.register(GitConfigDTO.class, 19060);
    kryo.register(GitConnectionType.class, 19068);
    kryo.register(GitHTTPAuthenticationDTO.class, 19064);
    kryo.register(GitSSHAuthenticationDTO.class, 19065);
    kryo.register(GitSyncConfig.class, 19069);
    kryo.register(KubernetesAuthCredentialDTO.class, 19058);
    kryo.register(KubernetesAuthDTO.class, 19050);
    kryo.register(KubernetesAuthType.class, 19051);
    kryo.register(KubernetesClientKeyCertDTO.class, 19053);
    kryo.register(KubernetesClusterConfigDTO.class, 19045);
    kryo.register(KubernetesClusterDetailsDTO.class, 19049);
    kryo.register(KubernetesCredentialSpecDTO.class, 19047);
    kryo.register(KubernetesCredentialType.class, 19046);
    kryo.register(KubernetesDelegateDetailsDTO.class, 19048);
    kryo.register(KubernetesOpenIdConnectDTO.class, 19055);
    kryo.register(KubernetesServiceAccountDTO.class, 19054);
    kryo.register(KubernetesUserNamePasswordDTO.class, 19052);
    kryo.register(SplunkConnectorDTO.class, 19111);
    kryo.register(DockerAuthCredentialsDTO.class, 19112);
    kryo.register(DockerAuthenticationDTO.class, 19113);
    kryo.register(DockerAuthType.class, 19114);
    kryo.register(DockerConnectorDTO.class, 19115);
    kryo.register(DockerUserNamePasswordDTO.class, 19116);
    kryo.register(KubernetesCredentialDTO.class, 19342);
    kryo.register(JiraConnectorDTO.class, 19344);
    kryo.register(GcpConnectorDTO.class, 19345);
    kryo.register(GcpConnectorCredentialDTO.class, 19346);
    kryo.register(GcpCredentialType.class, 19347);
    kryo.register(GcpConstants.class, 19348);
    kryo.register(GcpDelegateDetailsDTO.class, 19349);
    kryo.register(GcpManualDetailsDTO.class, 19350);
    kryo.register(AwsConnectorDTO.class, 19351);
    kryo.register(AwsConstants.class, 19352);
    kryo.register(AwsCredentialDTO.class, 19353);
    kryo.register(AwsCredentialSpecDTO.class, 19354);
    kryo.register(AwsCredentialType.class, 19355);
    kryo.register(AwsInheritFromDelegateSpecDTO.class, 19357);
    kryo.register(AwsManualConfigSpecDTO.class, 19358);
    kryo.register(CrossAccountAccessDTO.class, 19362);
    kryo.register(ConnectorType.class, 19372);
    kryo.register(DockerRegistryProviderType.class, 19434);
    kryo.register(GithubHttpCredentialsDTO.class, 19440);
    kryo.register(GithubHttpAuthenticationType.class, 19441);
    kryo.register(GithubUsernamePasswordDTO.class, 19442);
    kryo.register(GitlabConnectorDTO.class, 19443);
    kryo.register(GithubConnectorDTO.class, 19444);
    kryo.register(GithubApiAccessDTO.class, 19445);
    kryo.register(GithubSshCredentialsDTO.class, 19446);
    kryo.register(GithubApiAccessSpecDTO.class, 19447);
    kryo.register(GithubAppSpecDTO.class, 19449);
    kryo.register(GithubTokenSpecDTO.class, 19450);
    kryo.register(GithubApiAccessType.class, 19451);
    kryo.register(GithubAuthenticationDTO.class, 19452);
    kryo.register(GithubCredentialsDTO.class, 19453);
    kryo.register(CEAwsConnectorDTO.class, 19454);
    kryo.register(AwsCurAttributesDTO.class, 19455);
    kryo.register(CEAwsFeatures.class, 19456);
    kryo.register(ArtifactoryConnectorDTO.class, 19487);
    kryo.register(ArtifactoryAuthCredentialsDTO.class, 19488);
    kryo.register(ArtifactoryAuthenticationDTO.class, 19489);
    kryo.register(ArtifactoryAuthType.class, 19490);
    kryo.register(ArtifactoryConstants.class, 19491);
    kryo.register(ArtifactoryUsernamePasswordAuthDTO.class, 19492);
    kryo.register(NexusConnectorDTO.class, 19498);
    kryo.register(NexusAuthenticationDTO.class, 19499);
    kryo.register(NexusAuthType.class, 19500);
    kryo.register(NexusUsernamePasswordAuthDTO.class, 19501);
    kryo.register(NexusAuthCredentialsDTO.class, 19502);
    kryo.register(NexusConstants.class, 19503);
    kryo.register(VaultConnectorDTO.class, 19506);
    kryo.register(GithubUsernameTokenDTO.class, 19511);
    kryo.register(GitlabUsernameTokenDTO.class, 19512);
    kryo.register(GitlabAuthenticationDTO.class, 19520);
    kryo.register(BitbucketConnectorDTO.class, 19521);
    kryo.register(GithubHttpCredentialsSpecDTO.class, 19522);
    kryo.register(GitlabHttpCredentialsDTO.class, 19523);
    kryo.register(BitbucketAuthenticationDTO.class, 19524);
    kryo.register(GitlabUsernamePasswordDTO.class, 19525);
    kryo.register(BitbucketHttpCredentialsDTO.class, 19526);
    kryo.register(GitlabKerberosDTO.class, 19527);
    kryo.register(GitlabHttpAuthenticationType.class, 19528);
    kryo.register(BitbucketHttpAuthenticationType.class, 19529);
    kryo.register(BitbucketUsernamePasswordDTO.class, 19530);
    kryo.register(LocalConnectorDTO.class, 543237);
    kryo.register(GcpKmsConnectorDTO.class, 543238);

    kryo.register(AwsKmsConnectorDTO.class, 543286);
    kryo.register(AwsKmsConnectorCredentialDTO.class, 543288);
    kryo.register(AwsKmsCredentialType.class, 543289);
    kryo.register(AwsKmsCredentialSpecDTO.class, 543290);
    kryo.register(AwsKmsConstants.class, 543291);
    kryo.register(AwsKmsCredentialSpecManualConfigDTO.class, 543292);
    kryo.register(AwsKmsCredentialSpecAssumeIAMDTO.class, 543293);
    kryo.register(AwsKmsCredentialSpecAssumeSTSDTO.class, 543294);

    kryo.register(CEAzureConnectorDTO.class, 19540);
    kryo.register(CEAzureFeatures.class, 19541);
    kryo.register(BillingExportSpecDTO.class, 19542);
    kryo.register(LinkedHashSet.class, 100030);
    kryo.register(CEKubernetesClusterConfigDTO.class, 19543);
    kryo.register(GitlabSshCredentialsDTO.class, 19643);
    kryo.register(GitlabTokenSpecDTO.class, 19644);
    kryo.register(GitlabApiAccessDTO.class, 19645);
    kryo.register(GitlabApiAccessType.class, 19646);
    kryo.register(GitlabApiAccessSpecDTO.class, 19647);
    kryo.register(AwsCodeCommitConnectorDTO.class, 19648);
    kryo.register(AwsCodeCommitAuthenticationDTO.class, 19649);
    kryo.register(AwsCodeCommitHttpsCredentialsDTO.class, 19650);
    kryo.register(AwsCodeCommitSecretKeyAccessKeyDTO.class, 19651);
    kryo.register(AwsCodeCommitUrlType.class, 19652);
    kryo.register(AwsCodeCommitHttpsAuthType.class, 19653);
    kryo.register(AwsCodeCommitAuthType.class, 19654);
    kryo.register(HttpHelmAuthCredentialsDTO.class, 19655);
    kryo.register(HttpHelmAuthenticationDTO.class, 19656);
    kryo.register(HttpHelmAuthType.class, 19657);
    kryo.register(HttpHelmConnectorDTO.class, 19658);
    kryo.register(HttpHelmUsernamePasswordDTO.class, 19659);
    kryo.register(BitbucketApiAccessDTO.class, 19660);
    kryo.register(BitbucketUsernameTokenApiAccessDTO.class, 19661);
    kryo.register(BitbucketApiAccessType.class, 19662);
    kryo.register(BitbucketApiAccessSpecDTO.class, 19663);
    kryo.register(NewRelicConnectorDTO.class, 19664);
    kryo.register(AppDynamicsAuthType.class, 19665);
    kryo.register(GcpCloudCostConnectorDTO.class, 19666);
    kryo.register(BitbucketSshCredentialsDTO.class, 19667);
  }
}