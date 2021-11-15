package io.harness.stateutils.buildstate;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.beans.connector.ConnectorType.BITBUCKET;
import static io.harness.delegate.beans.connector.ConnectorType.CODECOMMIT;
import static io.harness.delegate.beans.connector.ConnectorType.GIT;
import static io.harness.delegate.beans.connector.ConnectorType.GITHUB;
import static io.harness.delegate.beans.connector.ConnectorType.GITLAB;
import static io.harness.exception.WingsException.USER;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.beans.environment.K8BuildJobEnvInfo;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorResourceClient;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.delegate.beans.ci.pod.ConnectorDetails.ConnectorDetailsBuilder;
import io.harness.delegate.beans.ci.pod.SSHKeyDetails;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryAuthType;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryConnectorDTO;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryUsernamePasswordAuthDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialType;
import io.harness.delegate.beans.connector.awsconnector.AwsManualConfigSpecDTO;
import io.harness.delegate.beans.connector.docker.DockerAuthType;
import io.harness.delegate.beans.connector.docker.DockerConnectorDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorCredentialDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpCredentialType;
import io.harness.delegate.beans.connector.gcpconnector.GcpManualDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthCredentialDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitConnector;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketApiAccessType;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketConnector;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketHttpCredentials;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketSshCredentials;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketUsernameTokenApiAccess;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitAuthentication;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfig;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitSSHAuthentication;
import io.harness.delegate.beans.connector.scm.github.GithubConnector;
import io.harness.delegate.beans.connector.scm.github.GithubHttpAuthenticationType;
import io.harness.delegate.beans.connector.scm.github.GithubHttpCredentials;
import io.harness.delegate.beans.connector.scm.github.GithubSshCredentials;
import io.harness.delegate.beans.connector.scm.github.GithubUsernamePassword;
import io.harness.delegate.beans.connector.scm.github.GithubUsernameToken;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabConnector;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabHttpAuthenticationType;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabHttpCredentials;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabSshCredentials;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabUsernamePassword;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabUsernameToken;
import io.harness.eraro.ErrorCode;
import io.harness.exception.ConnectorNotFoundException;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.ngexception.CIStageExecutionException;
import io.harness.ng.core.NGAccess;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.JsonUtils;
import io.harness.utils.IdentifierRefHelper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import retrofit2.Response;

@Singleton
@Slf4j
@OwnedBy(HarnessTeam.CI)
public class ConnectorUtils {
  private final ConnectorResourceClient connectorResourceClient;
  private final SecretManagerClientService secretManagerClientService;
  private final SecretUtils secretUtils;

  private final Duration RETRY_SLEEP_DURATION = Duration.ofSeconds(2);
  private final int MAX_ATTEMPTS = 6;

  @Inject
  public ConnectorUtils(ConnectorResourceClient connectorResourceClient, SecretUtils secretUtils,
      @Named("PRIVILEGED") SecretManagerClientService secretManagerClientService) {
    this.connectorResourceClient = connectorResourceClient;
    this.secretUtils = secretUtils;
    this.secretManagerClientService = secretManagerClientService;
  }

  public Map<String, ConnectorDetails> getConnectorDetailsMap(NGAccess ngAccess, Set<String> connectorNameSet) {
    Map<String, ConnectorDetails> connectorDetailsMap = new HashMap<>();
    if (isNotEmpty(connectorNameSet)) {
      for (String connectorIdentifier : connectorNameSet) {
        ConnectorDetails connectorDetails = getConnectorDetails(ngAccess, connectorIdentifier);
        connectorDetailsMap.put(connectorDetails.getIdentifier(), connectorDetails);
      }
    }

    return connectorDetailsMap;
  }

  public ConnectorDetails getConnectorDetailsWithConversionInfo(
      NGAccess ngAccess, K8BuildJobEnvInfo.ConnectorConversionInfo connectorConversionInfo) {
    ConnectorDetails connectorDetails = getConnectorDetails(ngAccess, connectorConversionInfo.getConnectorRef());
    connectorDetails.setEnvToSecretsMap(connectorConversionInfo.getEnvToSecretsMap());
    return connectorDetails;
  }

  public ConnectorDetails getConnectorDetails(NGAccess ngAccess, String connectorIdentifier) {
    RetryPolicy<Object> retryPolicy =
        getRetryPolicy(format("[Retrying failed call to fetch connector: [%s], attempt: {}", connectorIdentifier),
            format("Failed to fetch connector: [%s] after retrying {} times", connectorIdentifier));

    return Failsafe.with(retryPolicy).get(() -> { return getConnectorDetailsInternal(ngAccess, connectorIdentifier); });
  }

  private ConnectorDetails getConnectorDetailsInternal(NGAccess ngAccess, String connectorIdentifier)
      throws IOException {
    log.info("Getting connector details for connector ref [{}]", connectorIdentifier);
    IdentifierRef connectorRef = IdentifierRefHelper.getIdentifierRef(connectorIdentifier,
        ngAccess.getAccountIdentifier(), ngAccess.getOrgIdentifier(), ngAccess.getProjectIdentifier());

    ConnectorDTO connectorDTO = getConnector(connectorRef);
    ConnectorType connectorType = connectorDTO.getConnectorInfo().getConnectorType();

    ConnectorDetailsBuilder connectorDetailsBuilder =
        ConnectorDetails.builder()
            .connectorType(connectorType)
            .connectorConfig(connectorDTO.getConnectorInfo().getConnectorConfig())
            .identifier(connectorDTO.getConnectorInfo().getIdentifier())
            .orgIdentifier(connectorDTO.getConnectorInfo().getOrgIdentifier())
            .projectIdentifier(connectorDTO.getConnectorInfo().getProjectIdentifier());

    log.info("Fetching encryption details for connector details for connector id:[{}] type:[{}]", connectorIdentifier,
        connectorType);
    ConnectorDetails connectorDetails;

    switch (connectorType) {
      case DOCKER:
        connectorDetails = getDockerConnectorDetails(ngAccess, connectorDTO, connectorDetailsBuilder);
        break;
      case KUBERNETES_CLUSTER:
        connectorDetails = getK8sConnectorDetails(ngAccess, connectorDTO, connectorDetailsBuilder);
        break;
      case GIT:
      case GITHUB:
      case GITLAB:
      case BITBUCKET:
      case CODECOMMIT:
        connectorDetails = getGitConnectorDetails(ngAccess, connectorDTO, connectorDetailsBuilder);
        break;
      case GCP:
        connectorDetails = getGcpConnectorDetails(ngAccess, connectorDTO, connectorDetailsBuilder);
        break;
      case AWS:
        connectorDetails = getAwsConnectorDetails(ngAccess, connectorDTO, connectorDetailsBuilder);
        break;
      case ARTIFACTORY:
        connectorDetails = getArtifactoryConnectorDetails(ngAccess, connectorDTO, connectorDetailsBuilder);
        break;
      default:
        throw new InvalidArgumentsException(format("Unexpected connector type:[%s]", connectorType));
    }
    log.info(
        "Successfully fetched encryption details for  connector id:[{}] type:[{}]", connectorIdentifier, connectorType);
    return connectorDetails;
  }

  public String fetchUserName(ConnectorDetails gitConnector) {
    if (gitConnector.getConnectorType() == GITHUB) {
      GithubConnector gitConfigDTO = (GithubConnector) gitConnector.getConnectorConfig();
      return fetchUserNameFromGithubConnector(gitConfigDTO);
    } else if (gitConnector.getConnectorType() == GITLAB) {
      GitlabConnector gitConfigDTO = (GitlabConnector) gitConnector.getConnectorConfig();
      return fetchUserNameFromGitlabConnector(gitConfigDTO);
    } else if (gitConnector.getConnectorType() == BITBUCKET) {
      BitbucketConnector gitConfigDTO = (BitbucketConnector) gitConnector.getConnectorConfig();
      return fetchUserNameFromBitbucketConnector(gitConfigDTO, gitConnector.getIdentifier());
    } else {
      throw new CIStageExecutionException("Unsupported git connector " + gitConnector.getConnectorType());
    }
  }

  public String retrieveURL(ConnectorDetails gitConnector) {
    if (gitConnector.getConnectorType() == GITHUB) {
      GithubConnector gitConfigDTO = (GithubConnector) gitConnector.getConnectorConfig();
      return gitConfigDTO.getUrl();
    } else if (gitConnector.getConnectorType() == BITBUCKET) {
      BitbucketConnector gitConfigDTO = (BitbucketConnector) gitConnector.getConnectorConfig();
      return gitConfigDTO.getUrl();
    } else if (gitConnector.getConnectorType() == GIT) {
      GitConfig gitConfig = (GitConfig) gitConnector.getConnectorConfig();
      return gitConfig.getUrl();
    } else if (gitConnector.getConnectorType() == GITLAB) {
      GitlabConnector gitConfigDTO = (GitlabConnector) gitConnector.getConnectorConfig();
      return gitConfigDTO.getUrl();
    } else if (gitConnector.getConnectorType() == CODECOMMIT) {
      AwsCodeCommitConnector gitConfigDTO = (AwsCodeCommitConnector) gitConnector.getConnectorConfig();
      return gitConfigDTO.getUrl();
    } else {
      throw new CIStageExecutionException("scmType " + gitConnector.getConnectorType() + "is not supported.");
    }
  }

  public boolean hasApiAccess(ConnectorDetails gitConnector) {
    if (gitConnector.getConnectorType() == GITHUB) {
      GithubConnector gitConfigDTO = (GithubConnector) gitConnector.getConnectorConfig();
      return gitConfigDTO.getApiAccess() != null;
    } else if (gitConnector.getConnectorType() == BITBUCKET) {
      BitbucketConnector gitConfigDTO = (BitbucketConnector) gitConnector.getConnectorConfig();
      return gitConfigDTO.getApiAccess() != null;
    } else if (gitConnector.getConnectorType() == GITLAB) {
      GitlabConnector gitConfigDTO = (GitlabConnector) gitConnector.getConnectorConfig();
      return gitConfigDTO.getApiAccess() != null;
    } else if (gitConnector.getConnectorType() == GIT || gitConnector.getConnectorType() == CODECOMMIT) {
      return false;
    } else {
      throw new CIStageExecutionException("scmType " + gitConnector.getConnectorType() + "is not supported");
    }
  }

  private ConnectorDetails getArtifactoryConnectorDetails(
      NGAccess ngAccess, ConnectorDTO connectorDTO, ConnectorDetailsBuilder connectorDetailsBuilder) {
    List<EncryptedDataDetail> encryptedDataDetails;

    ArtifactoryConnectorDTO artifactoryConnectorDTO =
        (ArtifactoryConnectorDTO) connectorDTO.getConnectorInfo().getConnectorConfig();
    if (artifactoryConnectorDTO.getAuth().getAuthType() == ArtifactoryAuthType.USER_PASSWORD) {
      ArtifactoryUsernamePasswordAuthDTO auth =
          (ArtifactoryUsernamePasswordAuthDTO) artifactoryConnectorDTO.getAuth().getCredentials();
      encryptedDataDetails = secretManagerClientService.getEncryptionDetails(ngAccess, auth);
      return connectorDetailsBuilder.encryptedDataDetails(encryptedDataDetails).build();
    }
    throw new InvalidArgumentsException(format("Unsupported artifactory auth type:[%s] on connector:[%s]",
        artifactoryConnectorDTO.getAuth().getAuthType(), artifactoryConnectorDTO));
  }

  private ConnectorDetails getAwsConnectorDetails(
      NGAccess ngAccess, ConnectorDTO connectorDTO, ConnectorDetailsBuilder connectorDetailsBuilder) {
    List<EncryptedDataDetail> encryptedDataDetails;
    AwsConnectorDTO awsConnectorDTO = (AwsConnectorDTO) connectorDTO.getConnectorInfo().getConnectorConfig();
    AwsCredentialDTO awsCredentialDTO = awsConnectorDTO.getCredential();
    if (awsCredentialDTO.getAwsCredentialType() == AwsCredentialType.MANUAL_CREDENTIALS) {
      AwsManualConfigSpecDTO awsManualConfigSpecDTO = (AwsManualConfigSpecDTO) awsCredentialDTO.getConfig();
      encryptedDataDetails = secretManagerClientService.getEncryptionDetails(ngAccess, awsManualConfigSpecDTO);
      return connectorDetailsBuilder.encryptedDataDetails(encryptedDataDetails).build();
    } else if (awsCredentialDTO.getAwsCredentialType() == AwsCredentialType.INHERIT_FROM_DELEGATE) {
      return connectorDetailsBuilder.build();
    } else if (awsCredentialDTO.getAwsCredentialType() == AwsCredentialType.IRSA) {
      return connectorDetailsBuilder.build();
    }
    throw new InvalidArgumentsException(format("Unsupported aws credential type:[%s] on connector:[%s]",
        awsCredentialDTO.getAwsCredentialType(), awsConnectorDTO));
  }

  private ConnectorDetails getGcpConnectorDetails(
      NGAccess ngAccess, ConnectorDTO connectorDTO, ConnectorDetailsBuilder connectorDetailsBuilder) {
    List<EncryptedDataDetail> encryptedDataDetails;
    GcpConnectorDTO gcpConnectorDTO = (GcpConnectorDTO) connectorDTO.getConnectorInfo().getConnectorConfig();
    GcpConnectorCredentialDTO credential = gcpConnectorDTO.getCredential();
    if (credential.getGcpCredentialType() == GcpCredentialType.MANUAL_CREDENTIALS) {
      GcpManualDetailsDTO credentialConfig = (GcpManualDetailsDTO) credential.getConfig();
      encryptedDataDetails = secretManagerClientService.getEncryptionDetails(ngAccess, credentialConfig);
      return connectorDetailsBuilder.encryptedDataDetails(encryptedDataDetails).build();
    } else if (credential.getGcpCredentialType() == GcpCredentialType.INHERIT_FROM_DELEGATE) {
      return connectorDetailsBuilder.build();
    }
    throw new InvalidArgumentsException(format("Unsupported gcp credential type:[%s] on connector:[%s]",
        gcpConnectorDTO.getCredential().getGcpCredentialType(), gcpConnectorDTO));
  }

  private ConnectorDetails getGitConnectorDetails(
      NGAccess ngAccess, ConnectorDTO connectorDTO, ConnectorDetailsBuilder connectorDetailsBuilder) {
    if (connectorDTO.getConnectorInfo().getConnectorType() == GITHUB) {
      return buildGithubConnectorDetails(ngAccess, connectorDTO, connectorDetailsBuilder);
    } else if (connectorDTO.getConnectorInfo().getConnectorType() == GITLAB) {
      return buildGitlabConnectorDetails(ngAccess, connectorDTO, connectorDetailsBuilder);
    } else if (connectorDTO.getConnectorInfo().getConnectorType() == BITBUCKET) {
      return buildBitBucketConnectorDetails(ngAccess, connectorDTO, connectorDetailsBuilder);
    } else if (connectorDTO.getConnectorInfo().getConnectorType() == CODECOMMIT) {
      return buildAwsCodeCommitConnectorDetails(ngAccess, connectorDTO, connectorDetailsBuilder);
    } else if (connectorDTO.getConnectorInfo().getConnectorType() == GIT) {
      return buildGitConnectorDetails(ngAccess, connectorDTO, connectorDetailsBuilder);
    } else {
      throw new CIStageExecutionException(
          "Unsupported git connector " + connectorDTO.getConnectorInfo().getConnectorType());
    }
  }

  private ConnectorDetails buildGitlabConnectorDetails(
      NGAccess ngAccess, ConnectorDTO connectorDTO, ConnectorDetailsBuilder connectorDetailsBuilder) {
    List<EncryptedDataDetail> encryptedDataDetails;
    GitlabConnector gitConfigDTO = (GitlabConnector) connectorDTO.getConnectorInfo().getConnectorConfig();
    if (gitConfigDTO.getAuthentication().getAuthType() == GitAuthType.HTTP) {
      GitlabHttpCredentials gitlabHttpCredentials =
          (GitlabHttpCredentials) gitConfigDTO.getAuthentication().getCredentials();
      encryptedDataDetails =
          secretManagerClientService.getEncryptionDetails(ngAccess, gitlabHttpCredentials.getHttpCredentialsSpec());
      if (gitConfigDTO.getApiAccess() != null && gitConfigDTO.getApiAccess().getSpec() != null) {
        encryptedDataDetails.addAll(
            secretManagerClientService.getEncryptionDetails(ngAccess, gitConfigDTO.getApiAccess().getSpec()));
      }
      return connectorDetailsBuilder.encryptedDataDetails(encryptedDataDetails).build();
    } else if (gitConfigDTO.getAuthentication().getAuthType() == GitAuthType.SSH) {
      GitlabSshCredentials gitlabSshCredentialsDTO =
          (GitlabSshCredentials) gitConfigDTO.getAuthentication().getCredentials();
      SSHKeyDetails sshKey = secretUtils.getSshKey(ngAccess, gitlabSshCredentialsDTO.getSshKeyRef());
      if (sshKey.getSshKeyReference().getEncryptedPassphrase() != null) {
        throw new CIStageExecutionException("Unsupported ssh key format, passphrase is unsupported in git connector: "
            + gitConfigDTO.getAuthentication().getAuthType());
      }
      connectorDetailsBuilder.sshKeyDetails(sshKey);
      if (gitConfigDTO.getApiAccess() != null && gitConfigDTO.getApiAccess().getSpec() != null) {
        encryptedDataDetails =
            secretManagerClientService.getEncryptionDetails(ngAccess, gitConfigDTO.getApiAccess().getSpec());
        connectorDetailsBuilder.encryptedDataDetails(encryptedDataDetails);
      }
      return connectorDetailsBuilder.build();
    } else {
      throw new CIStageExecutionException(
          "Unsupported git connector auth" + gitConfigDTO.getAuthentication().getAuthType());
    }
  }

  private ConnectorDetails buildGithubConnectorDetails(
      NGAccess ngAccess, ConnectorDTO connectorDTO, ConnectorDetailsBuilder connectorDetailsBuilder) {
    List<EncryptedDataDetail> encryptedDataDetails;
    GithubConnector gitConfigDTO = (GithubConnector) connectorDTO.getConnectorInfo().getConnectorConfig();

    if (gitConfigDTO.getAuthentication().getAuthType() == GitAuthType.HTTP) {
      GithubHttpCredentials githubHttpCredentialsDTO =
          (GithubHttpCredentials) gitConfigDTO.getAuthentication().getCredentials();
      encryptedDataDetails =
          secretManagerClientService.getEncryptionDetails(ngAccess, githubHttpCredentialsDTO.getHttpCredentialsSpec());
      if (gitConfigDTO.getApiAccess() != null && gitConfigDTO.getApiAccess().getSpec() != null) {
        encryptedDataDetails.addAll(
            secretManagerClientService.getEncryptionDetails(ngAccess, gitConfigDTO.getApiAccess().getSpec()));
      }
      return connectorDetailsBuilder.encryptedDataDetails(encryptedDataDetails).build();
    } else if (gitConfigDTO.getAuthentication().getAuthType() == GitAuthType.SSH) {
      GithubSshCredentials githubSshCredentialsDTO =
          (GithubSshCredentials) gitConfigDTO.getAuthentication().getCredentials();
      SSHKeyDetails sshKey = secretUtils.getSshKey(ngAccess, githubSshCredentialsDTO.getSshKeyRef());
      if (sshKey.getSshKeyReference().getEncryptedPassphrase() != null) {
        throw new CIStageExecutionException("Unsupported ssh key format, passphrase is unsupported in git connector: "
            + gitConfigDTO.getAuthentication().getAuthType());
      }
      connectorDetailsBuilder.sshKeyDetails(sshKey);
      if (gitConfigDTO.getApiAccess() != null && gitConfigDTO.getApiAccess().getSpec() != null) {
        encryptedDataDetails =
            secretManagerClientService.getEncryptionDetails(ngAccess, gitConfigDTO.getApiAccess().getSpec());
        connectorDetailsBuilder.encryptedDataDetails(encryptedDataDetails);
      }
      return connectorDetailsBuilder.build();
    } else {
      throw new CIStageExecutionException(
          "Unsupported git connector auth" + gitConfigDTO.getAuthentication().getAuthType());
    }
  }

  private ConnectorDetails buildBitBucketConnectorDetails(
      NGAccess ngAccess, ConnectorDTO connectorDTO, ConnectorDetailsBuilder connectorDetailsBuilder) {
    List<EncryptedDataDetail> encryptedDataDetails;
    BitbucketConnector gitConfigDTO = (BitbucketConnector) connectorDTO.getConnectorInfo().getConnectorConfig();
    if (gitConfigDTO.getAuthentication().getAuthType() == GitAuthType.HTTP) {
      BitbucketHttpCredentials bitbucketHttpCredentialsDTO =
          (BitbucketHttpCredentials) gitConfigDTO.getAuthentication().getCredentials();
      encryptedDataDetails = secretManagerClientService.getEncryptionDetails(
          ngAccess, bitbucketHttpCredentialsDTO.getHttpCredentialsSpec());
      if (gitConfigDTO.getApiAccess() != null && gitConfigDTO.getApiAccess().getSpec() != null) {
        encryptedDataDetails.addAll(
            secretManagerClientService.getEncryptionDetails(ngAccess, gitConfigDTO.getApiAccess().getSpec()));
      }
      return connectorDetailsBuilder.encryptedDataDetails(encryptedDataDetails).build();
    } else if (gitConfigDTO.getAuthentication().getAuthType() == GitAuthType.SSH) {
      BitbucketSshCredentials bitbucketSshCredentials =
          (BitbucketSshCredentials) gitConfigDTO.getAuthentication().getCredentials();
      SSHKeyDetails sshKey = secretUtils.getSshKey(ngAccess, bitbucketSshCredentials.getSshKeyRef());
      connectorDetailsBuilder.sshKeyDetails(sshKey);
      if (sshKey.getSshKeyReference().getEncryptedPassphrase() != null) {
        throw new CIStageExecutionException("Unsupported ssh key format, passphrase is unsupported in git connector: "
            + gitConfigDTO.getAuthentication().getAuthType());
      }
      if (gitConfigDTO.getApiAccess() != null && gitConfigDTO.getApiAccess().getSpec() != null) {
        encryptedDataDetails =
            secretManagerClientService.getEncryptionDetails(ngAccess, gitConfigDTO.getApiAccess().getSpec());
        connectorDetailsBuilder.encryptedDataDetails(encryptedDataDetails);
      }
      return connectorDetailsBuilder.build();
    } else {
      throw new CIStageExecutionException(
          "Unsupported git connector auth" + gitConfigDTO.getAuthentication().getAuthType());
    }
  }

  private ConnectorDetails buildAwsCodeCommitConnectorDetails(
      NGAccess ngAccess, ConnectorDTO connectorDTO, ConnectorDetailsBuilder connectorDetailsBuilder) {
    AwsCodeCommitConnector gitConfigDTO = (AwsCodeCommitConnector) connectorDTO.getConnectorInfo().getConnectorConfig();
    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();
    gitConfigDTO.getDecryptableEntities().forEach(decryptableEntity
        -> encryptedDataDetails.addAll(secretManagerClientService.getEncryptionDetails(ngAccess, decryptableEntity)));
    connectorDetailsBuilder.encryptedDataDetails(encryptedDataDetails);
    return connectorDetailsBuilder.build();
  }

  private ConnectorDetails buildGitConnectorDetails(
      NGAccess ngAccess, ConnectorDTO connectorDTO, ConnectorDetailsBuilder connectorDetailsBuilder) {
    List<EncryptedDataDetail> encryptedDataDetails;
    GitConfig gitConfig = (GitConfig) connectorDTO.getConnectorInfo().getConnectorConfig();
    GitAuthentication gitAuth = gitConfig.getGitAuth();
    if (gitConfig.getGitAuthType() == GitAuthType.HTTP) {
      encryptedDataDetails = secretManagerClientService.getEncryptionDetails(ngAccess, gitAuth);
      return connectorDetailsBuilder.encryptedDataDetails(encryptedDataDetails).build();
    } else if (gitConfig.getGitAuthType() == GitAuthType.SSH) {
      GitSSHAuthentication gitSSHAuthentication = (GitSSHAuthentication) gitAuth;
      SSHKeyDetails sshKey = secretUtils.getSshKey(ngAccess, gitSSHAuthentication.getEncryptedSshKey());
      connectorDetailsBuilder.sshKeyDetails(sshKey);
      if (sshKey.getSshKeyReference().getEncryptedPassphrase() != null) {
        throw new CIStageExecutionException(
            "Unsupported ssh key format, passphrase is unsupported in git connector: " + gitConfig.getGitAuthType());
      }
      return connectorDetailsBuilder.build();
    } else {
      throw new CIStageExecutionException("Unsupported git connector auth" + gitConfig.getGitAuthType());
    }
  }

  private ConnectorDetails getDockerConnectorDetails(
      NGAccess ngAccess, ConnectorDTO connectorDTO, ConnectorDetailsBuilder connectorDetailsBuilder) {
    List<EncryptedDataDetail> encryptedDataDetails;
    DockerConnectorDTO dockerConnectorDTO = (DockerConnectorDTO) connectorDTO.getConnectorInfo().getConnectorConfig();
    DockerAuthType dockerAuthType = dockerConnectorDTO.getAuth().getAuthType();
    if (dockerAuthType == DockerAuthType.USER_PASSWORD) {
      encryptedDataDetails =
          secretManagerClientService.getEncryptionDetails(ngAccess, dockerConnectorDTO.getAuth().getCredentials());
      return connectorDetailsBuilder.encryptedDataDetails(encryptedDataDetails).build();
    } else if (dockerAuthType == DockerAuthType.ANONYMOUS) {
      return connectorDetailsBuilder.build();
    } else {
      throw new InvalidArgumentsException(
          format("Unsupported docker credential type:[%s] on connector:[%s]", dockerAuthType, dockerConnectorDTO));
    }
  }

  private ConnectorDetails getK8sConnectorDetails(
      NGAccess ngAccess, ConnectorDTO connectorDTO, ConnectorDetailsBuilder connectorDetailsBuilder) {
    List<EncryptedDataDetail> encryptedDataDetails;
    KubernetesClusterConfigDTO kubernetesClusterConfigDTO =
        (KubernetesClusterConfigDTO) connectorDTO.getConnectorInfo().getConnectorConfig();
    KubernetesCredentialDTO config = kubernetesClusterConfigDTO.getCredential();
    if (config.getKubernetesCredentialType() == KubernetesCredentialType.MANUAL_CREDENTIALS) {
      KubernetesClusterDetailsDTO kubernetesCredentialSpecDTO = (KubernetesClusterDetailsDTO) config.getConfig();
      KubernetesAuthCredentialDTO kubernetesAuthCredentialDTO = kubernetesCredentialSpecDTO.getAuth().getCredentials();
      encryptedDataDetails = secretManagerClientService.getEncryptionDetails(ngAccess, kubernetesAuthCredentialDTO);
      return connectorDetailsBuilder.encryptedDataDetails(encryptedDataDetails).build();
    } else if (config.getKubernetesCredentialType() == KubernetesCredentialType.INHERIT_FROM_DELEGATE) {
      return connectorDetailsBuilder.build();
    }
    throw new InvalidArgumentsException(format("Unsupported gcp credential type:[%s] on connector:[%s]",
        kubernetesClusterConfigDTO.getCredential().getKubernetesCredentialType(), kubernetesClusterConfigDTO));
  }

  private ConnectorDTO getConnector(IdentifierRef connectorRef) throws IOException {
    log.info("Fetching connector details for connector id:[{}] acc:[{}] project:[{}] org:[{}]",
        connectorRef.getIdentifier(), connectorRef.getAccountIdentifier(), connectorRef.getProjectIdentifier(),
        connectorRef.getOrgIdentifier());

    Response<ResponseDTO<Optional<ConnectorDTO>>> response =
        connectorResourceClient
            .get(connectorRef.getIdentifier(), connectorRef.getAccountIdentifier(), connectorRef.getOrgIdentifier(),
                connectorRef.getProjectIdentifier())
            .execute();
    if (response.isSuccessful()) {
      Optional<ConnectorDTO> connectorDTO = response.body().getData();
      if (!connectorDTO.isPresent()) {
        throw new CIStageExecutionException(format("Connector not present for identifier : [%s] with scope: [%s]",
            connectorRef.getIdentifier(), connectorRef.getScope()));
      }
      return connectorDTO.get();
    } else {
      ErrorCode errorCode = getResponseErrorCode(response);
      if (errorCode == ErrorCode.RESOURCE_NOT_FOUND_EXCEPTION) {
        throw new ConnectorNotFoundException(format("Connector not found for identifier : [%s] with scope: [%s]",
                                                 connectorRef.getIdentifier(), connectorRef.getScope()),
            USER);
      } else {
        throw new CIStageExecutionException(
            format("Failed to find connector for identifier: [%s] with scope: [%s] with error: %s",
                connectorRef.getIdentifier(), connectorRef.getScope(), errorCode));
      }
    }
  }

  private <T> ErrorCode getResponseErrorCode(Response<ResponseDTO<T>> response) throws IOException {
    try {
      FailureDTO failureResponse =
          JsonUtils.asObject(response.errorBody().string(), new TypeReference<FailureDTO>() {});
      return failureResponse.getCode();
    } catch (Exception e) {
      ErrorDTO errResponse = JsonUtils.asObject(response.errorBody().string(), new TypeReference<ErrorDTO>() {});
      return errResponse.getCode();
    }
  }

  private String fetchUserNameFromGitlabConnector(GitlabConnector gitConfigDTO) {
    if (gitConfigDTO.getAuthentication().getAuthType() == GitAuthType.HTTP) {
      GitlabHttpCredentials gitlabHttpCredentials =
          (GitlabHttpCredentials) gitConfigDTO.getAuthentication().getCredentials();
      if (gitlabHttpCredentials.getType() == GitlabHttpAuthenticationType.USERNAME_AND_PASSWORD) {
        GitlabUsernamePassword GitlabHttpCredentialsSpecDTO =
            (GitlabUsernamePassword) gitlabHttpCredentials.getHttpCredentialsSpec();
        return GitlabHttpCredentialsSpecDTO.getUsername();
      } else if (gitlabHttpCredentials.getType() == GitlabHttpAuthenticationType.USERNAME_AND_TOKEN) {
        GitlabUsernameToken GitlabHttpCredentialsSpecDTO =
            (GitlabUsernameToken) gitlabHttpCredentials.getHttpCredentialsSpec();
        return GitlabHttpCredentialsSpecDTO.getUsername();
      }
    }

    return null;
  }

  private String fetchUserNameFromBitbucketConnector(BitbucketConnector gitConfigDTO, String identifier) {
    try {
      if (gitConfigDTO.getApiAccess().getType() == BitbucketApiAccessType.USERNAME_AND_TOKEN) {
        return ((BitbucketUsernameTokenApiAccess) gitConfigDTO.getApiAccess().getSpec()).getUsername();
      }
    } catch (Exception ex) {
      log.error(format("Unable to get username information from api access for identifier %s", identifier), ex);
      throw new CIStageExecutionException(
          format("Unable to get username information from api access for identifier %s", identifier));
    }
    throw new CIStageExecutionException(
        format("Unable to get username information from api access for identifier %s", identifier));
  }

  private String fetchUserNameFromGithubConnector(GithubConnector gitConfigDTO) {
    if (gitConfigDTO.getAuthentication().getAuthType() == GitAuthType.HTTP) {
      GithubHttpCredentials githubHttpCredentialsDTO =
          (GithubHttpCredentials) gitConfigDTO.getAuthentication().getCredentials();
      if (githubHttpCredentialsDTO.getType() == GithubHttpAuthenticationType.USERNAME_AND_PASSWORD) {
        GithubUsernamePassword githubHttpCredentialsSpecDTO =
            (GithubUsernamePassword) githubHttpCredentialsDTO.getHttpCredentialsSpec();
        return githubHttpCredentialsSpecDTO.getUsername();
      } else if (githubHttpCredentialsDTO.getType() == GithubHttpAuthenticationType.USERNAME_AND_TOKEN) {
        GithubUsernameToken githubHttpCredentialsSpecDTO =
            (GithubUsernameToken) githubHttpCredentialsDTO.getHttpCredentialsSpec();
        return githubHttpCredentialsSpecDTO.getUsername();
      }
    }
    return null;
  }

  private RetryPolicy<Object> getRetryPolicy(String failedAttemptMessage, String failureMessage) {
    return new RetryPolicy<>()
        .handle(Exception.class)
        .abortOn(ConnectorNotFoundException.class)
        .withDelay(RETRY_SLEEP_DURATION)
        .withMaxAttempts(MAX_ATTEMPTS)
        .onFailedAttempt(event -> log.info(failedAttemptMessage, event.getAttemptCount(), event.getLastFailure()))
        .onFailure(event -> log.error(failureMessage, event.getAttemptCount(), event.getFailure()));
  }
}
