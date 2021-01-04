package io.harness.delegate.task.citasks.cik8handler;

import static io.harness.data.encoding.EncodingUtils.encodeBase64;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.beans.ci.pod.SecretParams.Type.FILE;
import static io.harness.delegate.beans.ci.pod.SecretParams.Type.TEXT;
import static io.harness.delegate.beans.connector.ConnectorType.BITBUCKET;
import static io.harness.delegate.beans.connector.ConnectorType.GITLAB;
import static io.harness.govern.Switch.unhandled;
import static io.harness.k8s.KubernetesConvention.getKubernetesGitSecretName;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.delegate.beans.ci.pod.ImageDetailsWithConnector;
import io.harness.delegate.beans.ci.pod.SecretParams;
import io.harness.delegate.beans.ci.pod.SecretVariableDTO;
import io.harness.delegate.beans.ci.pod.SecretVariableDetails;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.docker.DockerAuthType;
import io.harness.delegate.beans.connector.docker.DockerConnectorDTO;
import io.harness.delegate.beans.connector.docker.DockerUserNamePasswordDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpCredentialType;
import io.harness.delegate.beans.connector.gcpconnector.GcpManualDetailsDTO;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketConnectorDTO;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketHttpAuthenticationType;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketHttpCredentialsDTO;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketUsernamePasswordDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitSSHAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.github.GithubConnectorDTO;
import io.harness.delegate.beans.connector.scm.github.GithubHttpAuthenticationType;
import io.harness.delegate.beans.connector.scm.github.GithubHttpCredentialsDTO;
import io.harness.delegate.beans.connector.scm.github.GithubUsernamePasswordDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabConnectorDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabHttpAuthenticationType;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabHttpCredentialsDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabUsernamePasswordDTO;
import io.harness.delegate.task.citasks.cik8handler.helper.ConnectorEnvVariablesHelper;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.WingsException;
import io.harness.exception.ngexception.CIStageExecutionException;
import io.harness.security.encryption.SecretDecryptionService;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

/**
 * Helper class to create spec for image registry and GIT secrets. Generated spec can be used for creation of secrets on
 * a K8 cluster.
 */

@Slf4j
@Singleton
public class SecretSpecBuilder {
  private static final String DOCKER_REGISTRY_SECRET_TYPE = "kubernetes.io/dockercfg";
  private static final String BASE_GCR_HOSTNAME = "gcr.io";
  private static final String GCR_USERNAME = "_json_key";
  private static final String PATH_SEPARATOR = "/";
  private static final String USERNAME = "username";
  private static final String PASSWORD = "password";

  public static final String GIT_SECRET_USERNAME_KEY = "username";
  public static final String GIT_SECRET_PWD_KEY = "password";
  public static final String GIT_SECRET_SSH_KEY = "ssh_key";
  public static final String SECRET_KEY = "secret_key";
  public static final String SECRET = "secret";
  private static final String OPAQUE_SECRET_TYPE = "opaque";
  private static final String DOCKER_CONFIG_KEY = ".dockercfg";
  private static final String SSH_KEY = "SSH_KEY";
  private static final String DRONE_NETRC_PASSWORD = "DRONE_NETRC_PASSWORD";

  @Inject private ConnectorEnvVariablesHelper connectorEnvVariablesHelper;
  @Inject private SecretDecryptionService secretDecryptionService;

  public Secret getRegistrySecretSpec(
      String secretName, ImageDetailsWithConnector imageDetailsWithConnector, String namespace) {
    ConnectorDetails connectorDetails = imageDetailsWithConnector.getImageConnectorDetails();
    if (connectorDetails == null) {
      return null;
    }

    String registryUrl = null;
    String username = null;
    String password = null;

    log.info("Decrypting image registry connector details of id:[{}], type:[{}]", connectorDetails.getIdentifier(),
        connectorDetails.getConnectorType());
    if (connectorDetails.getConnectorType() == ConnectorType.DOCKER) {
      DockerConnectorDTO dockerConfig = (DockerConnectorDTO) connectorDetails.getConnectorConfig();
      registryUrl = dockerConfig.getDockerRegistryUrl();

      if (dockerConfig.getAuth().getAuthType() == DockerAuthType.USER_PASSWORD) {
        DockerUserNamePasswordDTO dockerUserNamePasswordDTO =
            (DockerUserNamePasswordDTO) secretDecryptionService.decrypt(
                dockerConfig.getAuth().getCredentials(), connectorDetails.getEncryptedDataDetails());
        username = dockerUserNamePasswordDTO.getUsername();
        password = String.valueOf(dockerUserNamePasswordDTO.getPasswordRef().getDecryptedValue());
      }
    } else if (connectorDetails.getConnectorType() == ConnectorType.GCP) {
      // Image name is of format: HOST-NAME/PROJECT-ID/IMAGE. HOST-NAME is registry url.
      String imageName = imageDetailsWithConnector.getImageDetails().getName();
      String[] imageParts = imageName.split(PATH_SEPARATOR);
      if (imageParts.length >= 1 && imageParts[0].endsWith(BASE_GCR_HOSTNAME)) {
        registryUrl = imageParts[0];
      } else {
        throw new InvalidArgumentsException(
            format("Invalid image: %s for GCR connector", imageName), WingsException.USER);
      }

      GcpConnectorDTO gcpConnectorConfig = (GcpConnectorDTO) connectorDetails.getConnectorConfig();
      if (gcpConnectorConfig.getCredential().getGcpCredentialType() == GcpCredentialType.MANUAL_CREDENTIALS) {
        GcpManualDetailsDTO credentialConfig = (GcpManualDetailsDTO) secretDecryptionService.decrypt(
            (GcpManualDetailsDTO) gcpConnectorConfig.getCredential().getConfig(),
            connectorDetails.getEncryptedDataDetails());
        password = String.valueOf(credentialConfig.getSecretKeyRef().getDecryptedValue());
        username = GCR_USERNAME;
      }
    }

    if (!(isNotBlank(registryUrl) && isNotBlank(username) && isNotBlank(password))) {
      return null;
    }

    String credentialData =
        new JSONObject().put(registryUrl, new JSONObject().put(USERNAME, username).put(PASSWORD, password)).toString();
    Map<String, String> data = ImmutableMap.of(DOCKER_CONFIG_KEY, encodeBase64(credentialData));
    return new SecretBuilder()
        .withNewMetadata()
        .withName(secretName)
        .withNamespace(namespace)
        .endMetadata()
        .withType(DOCKER_REGISTRY_SECRET_TYPE)
        .withData(data)
        .build();
  }

  public Map<String, SecretParams> decryptCustomSecretVariables(List<SecretVariableDetails> secretVariableDetails) {
    Map<String, SecretParams> data = new HashMap<>();
    if (isNotEmpty(secretVariableDetails)) {
      for (SecretVariableDetails secretVariableDetail : secretVariableDetails) {
        log.info("Decrypting custom variable name:[{}], type:[{}], secretRef:[{}]",
            secretVariableDetail.getSecretVariableDTO().getName(),
            secretVariableDetail.getSecretVariableDTO().getType(),
            secretVariableDetail.getSecretVariableDTO().getSecret().toSecretRefStringValue());
        SecretVariableDTO secretVariableDTO = (SecretVariableDTO) secretDecryptionService.decrypt(
            secretVariableDetail.getSecretVariableDTO(), secretVariableDetail.getEncryptedDataDetailList());
        switch (secretVariableDTO.getType()) {
          case FILE:
            data.put(secretVariableDTO.getName(),
                SecretParams.builder()
                    .secretKey(SECRET_KEY + secretVariableDTO.getName())
                    .type(FILE)
                    .value(encodeBase64(secretVariableDTO.getSecret().getDecryptedValue()))
                    .build());
            break;
          case TEXT:
            data.put(secretVariableDTO.getName(),
                SecretParams.builder()
                    .secretKey(SECRET_KEY + secretVariableDTO.getName())
                    .type(TEXT)
                    .value(encodeBase64(secretVariableDTO.getSecret().getDecryptedValue()))
                    .build());
            break;
          default:
            unhandled(secretVariableDTO.getType());
        }
      }
    }

    return data;
  }

  public Map<String, SecretParams> decryptConnectorSecretVariables(Map<String, ConnectorDetails> connectorDetailsMap) {
    Map<String, SecretParams> secretData = new HashMap<>();
    if (isEmpty(connectorDetailsMap)) {
      return secretData;
    }

    for (Map.Entry<String, ConnectorDetails> connectorDetailsEntry : connectorDetailsMap.entrySet()) {
      ConnectorDetails connectorDetails = connectorDetailsEntry.getValue();

      log.info("Decrypting connector id:[{}], type:[{}]", connectorDetails.getIdentifier(),
          connectorDetails.getConnectorType());
      if (connectorDetails.getConnectorType() == ConnectorType.DOCKER) {
        secretData.putAll(connectorEnvVariablesHelper.getDockerSecretVariables(connectorDetails));
      } else if (connectorDetails.getConnectorType() == ConnectorType.AWS) {
        secretData.putAll(connectorEnvVariablesHelper.getAwsSecretVariables(connectorDetails));
      } else if (connectorDetails.getConnectorType() == ConnectorType.GCP) {
        secretData.putAll(connectorEnvVariablesHelper.getGcpSecretVariables(connectorDetails));
      } else if (connectorDetails.getConnectorType() == ConnectorType.ARTIFACTORY) {
        secretData.putAll(connectorEnvVariablesHelper.getArtifactorySecretVariables(connectorDetails));
      }
      log.info("Decrypted connector id:[{}], type:[{}]", connectorDetails.getIdentifier(),
          connectorDetails.getConnectorType());
    }
    return secretData;
  }

  public Map<String, SecretParams> decryptGitSecretVariables(ConnectorDetails gitConnector) {
    if (gitConnector == null) {
      return new HashMap<>();
    }

    if (gitConnector.getConnectorType() == ConnectorType.GITHUB) {
      GithubConnectorDTO gitConfigDTO = (GithubConnectorDTO) gitConnector.getConnectorConfig();
      return retrieveGitHubSecretParams(gitConfigDTO, gitConnector);
    } else if (gitConnector.getConnectorType() == GITLAB) {
      GitlabConnectorDTO gitConfigDTO = (GitlabConnectorDTO) gitConnector.getConnectorConfig();
      return retrieveGitlabSecretParams(gitConfigDTO, gitConnector);
    } else if (gitConnector.getConnectorType() == BITBUCKET) {
      BitbucketConnectorDTO gitConfigDTO = (BitbucketConnectorDTO) gitConnector.getConnectorConfig();
      return retrieveBitbucketSecretParams(gitConfigDTO, gitConnector);
    } else {
      throw new CIStageExecutionException("Unsupported git connector type" + gitConnector.getConnectorType());
    }
  }

  public Secret createSecret(String secretName, String namespace, Map<String, String> data) {
    return new SecretBuilder()
        .withNewMetadata()
        .withName(secretName)
        .withNamespace(namespace)
        .endMetadata()
        .withType(OPAQUE_SECRET_TYPE)
        .withData(data)
        .build();
  }

  public Secret getGitSecretSpec(ConnectorDetails gitConnector, String namespace) throws UnsupportedEncodingException {
    if (gitConnector == null) {
      return null;
    }

    Map<String, String> data = new HashMap<>();

    if (gitConnector.getConnectorType() == ConnectorType.GITHUB) {
      GithubConnectorDTO gitConfigDTO = (GithubConnectorDTO) gitConnector.getConnectorConfig();
      data = retrieveGitHubSecretData(gitConfigDTO, gitConnector);
    } else if (gitConnector.getConnectorType() == GITLAB) {
      GitlabConnectorDTO gitConfigDTO = (GitlabConnectorDTO) gitConnector.getConnectorConfig();
      data = retrieveGitLabSecretData(gitConfigDTO, gitConnector);
    } else if (gitConnector.getConnectorType() == BITBUCKET) {
      BitbucketConnectorDTO gitConfigDTO = (BitbucketConnectorDTO) gitConnector.getConnectorConfig();
      data = retrieveBitbucketSecretData(gitConfigDTO, gitConnector);
    } else {
      throw new CIStageExecutionException("Unsupported git connector type" + gitConnector.getConnectorType());
    }

    GitConfigDTO gitConfigDTO = (GitConfigDTO) gitConnector.getConnectorConfig();
    log.info(
        "Decrypting git connector id:[{}], type:[{}]", gitConnector.getIdentifier(), gitConnector.getConnectorType());
    secretDecryptionService.decrypt(gitConfigDTO.getGitAuth(), gitConnector.getEncryptedDataDetails());

    if (data.isEmpty()) {
      String errMsg = format("Invalid GIT Authentication scheme %s for repository %s", gitConfigDTO.getGitAuthType(),
          gitConfigDTO.getUrl());
      log.error(errMsg);
      throw new InvalidArgumentsException(errMsg, WingsException.USER);
    }

    String secretName = getKubernetesGitSecretName(gitConfigDTO.getUrl());
    return new SecretBuilder()
        .withNewMetadata()
        .withName(secretName)
        .withNamespace(namespace)
        .endMetadata()
        .withType(OPAQUE_SECRET_TYPE)
        .withData(data)
        .build();
  }

  private Map<String, SecretParams> retrieveGitHubSecretParams(
      GithubConnectorDTO gitConfigDTO, ConnectorDetails gitConnector) {
    Map<String, SecretParams> secretData = new HashMap<>();

    if (gitConfigDTO.getAuthentication().getAuthType() == GitAuthType.HTTP) {
      String key = DRONE_NETRC_PASSWORD;

      GithubHttpCredentialsDTO gitHTTPAuthenticationDTO =
          (GithubHttpCredentialsDTO) gitConfigDTO.getAuthentication().getCredentials();

      secretDecryptionService.decrypt(
          gitHTTPAuthenticationDTO.getHttpCredentialsSpec(), gitConnector.getEncryptedDataDetails());

      if (gitHTTPAuthenticationDTO.getType() == GithubHttpAuthenticationType.USERNAME_AND_PASSWORD) {
        GithubUsernamePasswordDTO githubHttpCredentialsSpecDTO =
            (GithubUsernamePasswordDTO) gitHTTPAuthenticationDTO.getHttpCredentialsSpec();

        secretData.put(key,
            SecretParams.builder()
                .secretKey(key)
                .value(encodeBase64(new String(githubHttpCredentialsSpecDTO.getPasswordRef().getDecryptedValue())))
                .type(TEXT)
                .build());
      } else {
        throw new CIStageExecutionException("Unsupported git connector auth type" + gitHTTPAuthenticationDTO.getType());
      }
    } else if (gitConfigDTO.getAuthentication().getAuthType() == GitAuthType.SSH) {
      //        String key = SSH_KEY;
      //        secretData.put(key,
      //            SecretParams.builder()
      //                .secretKey(key)
      //                .value(encodeBase64(gitHTTPAuthenticationDTO.getEncryptedSshKey()))
      //                .type(TEXT)
      //                .build());

      throw new CIStageExecutionException(
          "Unsupported git connector auth" + gitConfigDTO.getAuthentication().getAuthType());
    } else {
      throw new CIStageExecutionException(
          "Unsupported git connector auth" + gitConfigDTO.getAuthentication().getAuthType());
    }

    return secretData;
  }

  private Map<String, String> retrieveGitHubSecretData(GithubConnectorDTO gitConfigDTO, ConnectorDetails gitConnector) {
    Map<String, String> data = new HashMap<>();

    if (gitConfigDTO.getAuthentication().getAuthType() == GitAuthType.HTTP) {
      GithubHttpCredentialsDTO gitHTTPAuthenticationDTO =
          (GithubHttpCredentialsDTO) gitConfigDTO.getAuthentication().getCredentials();

      secretDecryptionService.decrypt(
          gitHTTPAuthenticationDTO.getHttpCredentialsSpec(), gitConnector.getEncryptedDataDetails());

      if (gitHTTPAuthenticationDTO.getType() == GithubHttpAuthenticationType.USERNAME_AND_PASSWORD) {
        GithubUsernamePasswordDTO githubHttpCredentialsSpecDTO =
            (GithubUsernamePasswordDTO) gitHTTPAuthenticationDTO.getHttpCredentialsSpec();

        try {
          String urlEncodedPwd =
              URLEncoder.encode(new String(githubHttpCredentialsSpecDTO.getPasswordRef().getDecryptedValue()), "UTF-8");
          data.put(GIT_SECRET_USERNAME_KEY, encodeBase64(githubHttpCredentialsSpecDTO.getUsername()));
          data.put(GIT_SECRET_PWD_KEY, encodeBase64(urlEncodedPwd));
        } catch (Exception ex) {
          throw new CIStageExecutionException("Failed to encode password");
        }

      } else {
        throw new CIStageExecutionException("Unsupported git connector auth type" + gitHTTPAuthenticationDTO.getType());
      }
    } else if (gitConfigDTO.getAuthentication().getAuthType() == GitAuthType.SSH) {
      //      GitSSHAuthenticationDTO gitHTTPAuthenticationDTO = (GitSSHAuthenticationDTO) gitConfigDTO.getGitAuth();
      //      data.put(GIT_SECRET_SSH_KEY, encodeBase64(gitHTTPAuthenticationDTO.getEncryptedSshKey()));

      throw new CIStageExecutionException(
          "Unsupported git connector auth" + gitConfigDTO.getAuthentication().getAuthType());
    } else {
      throw new CIStageExecutionException(
          "Unsupported git connector auth" + gitConfigDTO.getAuthentication().getAuthType());
    }

    return data;
  }

  private Map<String, SecretParams> retrieveGitlabSecretParams(
      GitlabConnectorDTO gitConfigDTO, ConnectorDetails gitConnector) {
    Map<String, SecretParams> secretData = new HashMap<>();

    if (gitConfigDTO.getAuthentication().getAuthType() == GitAuthType.HTTP) {
      String key = DRONE_NETRC_PASSWORD;

      GitlabHttpCredentialsDTO gitHTTPAuthenticationDTO =
          (GitlabHttpCredentialsDTO) gitConfigDTO.getAuthentication().getCredentials();

      secretDecryptionService.decrypt(
          gitHTTPAuthenticationDTO.getHttpCredentialsSpec(), gitConnector.getEncryptedDataDetails());

      if (gitHTTPAuthenticationDTO.getType() == GitlabHttpAuthenticationType.USERNAME_AND_PASSWORD) {
        GitlabUsernamePasswordDTO gitlabHttpCredentialsSpecDTO =
            (GitlabUsernamePasswordDTO) gitHTTPAuthenticationDTO.getHttpCredentialsSpec();

        secretData.put(key,
            SecretParams.builder()
                .secretKey(key)
                .value(encodeBase64(new String(gitlabHttpCredentialsSpecDTO.getPasswordRef().getDecryptedValue())))
                .type(TEXT)
                .build());
      } else {
        throw new CIStageExecutionException("Unsupported git connector auth type" + gitHTTPAuthenticationDTO.getType());
      }
    } else if (gitConfigDTO.getAuthentication().getAuthType() == GitAuthType.SSH) {
      //        String key = SSH_KEY;
      //        secretData.put(key,
      //            SecretParams.builder()
      //                .secretKey(key)
      //                .value(encodeBase64(gitHTTPAuthenticationDTO.getEncryptedSshKey()))
      //                .type(TEXT)
      //                .build());

      throw new CIStageExecutionException(
          "Unsupported git connector auth" + gitConfigDTO.getAuthentication().getAuthType());
    } else {
      throw new CIStageExecutionException(
          "Unsupported git connector auth" + gitConfigDTO.getAuthentication().getAuthType());
    }

    return secretData;
  }

  private Map<String, String> retrieveGitLabSecretData(GitlabConnectorDTO gitConfigDTO, ConnectorDetails gitConnector) {
    Map<String, String> data = new HashMap<>();

    if (gitConfigDTO.getAuthentication().getAuthType() == GitAuthType.HTTP) {
      GitlabHttpCredentialsDTO gitHTTPAuthenticationDTO =
          (GitlabHttpCredentialsDTO) gitConfigDTO.getAuthentication().getCredentials();

      secretDecryptionService.decrypt(
          gitHTTPAuthenticationDTO.getHttpCredentialsSpec(), gitConnector.getEncryptedDataDetails());

      if (gitHTTPAuthenticationDTO.getType() == GitlabHttpAuthenticationType.USERNAME_AND_PASSWORD) {
        GitlabUsernamePasswordDTO gitlabHttpCredentialsSpecDTO =
            (GitlabUsernamePasswordDTO) gitHTTPAuthenticationDTO.getHttpCredentialsSpec();

        try {
          String urlEncodedPwd =
              URLEncoder.encode(new String(gitlabHttpCredentialsSpecDTO.getPasswordRef().getDecryptedValue()), "UTF-8");
          data.put(GIT_SECRET_USERNAME_KEY, encodeBase64(gitlabHttpCredentialsSpecDTO.getUsername()));
          data.put(GIT_SECRET_PWD_KEY, encodeBase64(urlEncodedPwd));
        } catch (Exception ex) {
          throw new CIStageExecutionException("Failed to encode password");
        }

      } else {
        throw new CIStageExecutionException("Unsupported git connector auth type" + gitHTTPAuthenticationDTO.getType());
      }
    } else if (gitConfigDTO.getAuthentication().getAuthType() == GitAuthType.SSH) {
      //      GitSSHAuthenticationDTO gitHTTPAuthenticationDTO = (GitSSHAuthenticationDTO) gitConfigDTO.getGitAuth();
      //      data.put(GIT_SECRET_SSH_KEY, encodeBase64(gitHTTPAuthenticationDTO.getEncryptedSshKey()));

      throw new CIStageExecutionException(
          "Unsupported git connector auth" + gitConfigDTO.getAuthentication().getAuthType());
    } else {
      throw new CIStageExecutionException(
          "Unsupported git connector auth" + gitConfigDTO.getAuthentication().getAuthType());
    }

    return data;
  }

  private Map<String, SecretParams> retrieveBitbucketSecretParams(
      BitbucketConnectorDTO gitConfigDTO, ConnectorDetails gitConnector) {
    Map<String, SecretParams> secretData = new HashMap<>();

    if (gitConfigDTO.getAuthentication().getAuthType() == GitAuthType.HTTP) {
      String key = DRONE_NETRC_PASSWORD;

      BitbucketHttpCredentialsDTO gitHTTPAuthenticationDTO =
          (BitbucketHttpCredentialsDTO) gitConfigDTO.getAuthentication().getCredentials();

      secretDecryptionService.decrypt(
          gitHTTPAuthenticationDTO.getHttpCredentialsSpec(), gitConnector.getEncryptedDataDetails());

      if (gitHTTPAuthenticationDTO.getType() == BitbucketHttpAuthenticationType.USERNAME_AND_PASSWORD) {
        BitbucketUsernamePasswordDTO bitbucketHttpCredentialsSpecDTO =
            (BitbucketUsernamePasswordDTO) gitHTTPAuthenticationDTO.getHttpCredentialsSpec();

        secretData.put(key,
            SecretParams.builder()
                .secretKey(key)
                .value(encodeBase64(new String(bitbucketHttpCredentialsSpecDTO.getPasswordRef().getDecryptedValue())))
                .type(TEXT)
                .build());
      } else {
        throw new CIStageExecutionException("Unsupported git connector auth type" + gitHTTPAuthenticationDTO.getType());
      }
    } else if (gitConfigDTO.getAuthentication().getAuthType() == GitAuthType.SSH) {
      //        String key = SSH_KEY;
      //        secretData.put(key,
      //            SecretParams.builder()
      //                .secretKey(key)
      //                .value(encodeBase64(gitHTTPAuthenticationDTO.getEncryptedSshKey()))
      //                .type(TEXT)
      //                .build());

      throw new CIStageExecutionException(
          "Unsupported git connector auth" + gitConfigDTO.getAuthentication().getAuthType());
    } else {
      throw new CIStageExecutionException(
          "Unsupported git connector auth" + gitConfigDTO.getAuthentication().getAuthType());
    }

    return secretData;
  }

  private Map<String, String> retrieveBitbucketSecretData(
      BitbucketConnectorDTO gitConfigDTO, ConnectorDetails gitConnector) {
    Map<String, String> data = new HashMap<>();

    if (gitConfigDTO.getAuthentication().getAuthType() == GitAuthType.HTTP) {
      BitbucketHttpCredentialsDTO gitHTTPAuthenticationDTO =
          (BitbucketHttpCredentialsDTO) gitConfigDTO.getAuthentication().getCredentials();

      secretDecryptionService.decrypt(
          gitHTTPAuthenticationDTO.getHttpCredentialsSpec(), gitConnector.getEncryptedDataDetails());

      if (gitHTTPAuthenticationDTO.getType() == BitbucketHttpAuthenticationType.USERNAME_AND_PASSWORD) {
        BitbucketUsernamePasswordDTO bitbucketHttpCredentialsSpecDTO =
            (BitbucketUsernamePasswordDTO) gitHTTPAuthenticationDTO.getHttpCredentialsSpec();

        try {
          String urlEncodedPwd = URLEncoder.encode(
              new String(bitbucketHttpCredentialsSpecDTO.getPasswordRef().getDecryptedValue()), "UTF-8");
          data.put(GIT_SECRET_USERNAME_KEY, encodeBase64(bitbucketHttpCredentialsSpecDTO.getUsername()));
          data.put(GIT_SECRET_PWD_KEY, encodeBase64(urlEncodedPwd));
        } catch (Exception ex) {
          throw new CIStageExecutionException("Failed to encode password");
        }

      } else {
        throw new CIStageExecutionException("Unsupported git connector auth type" + gitHTTPAuthenticationDTO.getType());
      }
    } else if (gitConfigDTO.getAuthentication().getAuthType() == GitAuthType.SSH) {
      //      GitSSHAuthenticationDTO gitHTTPAuthenticationDTO = (GitSSHAuthenticationDTO) gitConfigDTO.getGitAuth();
      //      data.put(GIT_SECRET_SSH_KEY, encodeBase64(gitHTTPAuthenticationDTO.getEncryptedSshKey()));

      throw new CIStageExecutionException(
          "Unsupported git connector auth" + gitConfigDTO.getAuthentication().getAuthType());
    } else {
      throw new CIStageExecutionException(
          "Unsupported git connector auth" + gitConfigDTO.getAuthentication().getAuthType());
    }

    return data;
  }
}
