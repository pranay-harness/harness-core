package io.harness.delegate.task.citasks.cik8handler;

import static io.harness.data.encoding.EncodingUtils.encodeBase64;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.beans.ci.pod.SecretParams.Type.FILE;
import static io.harness.delegate.beans.ci.pod.SecretParams.Type.TEXT;
import static io.harness.govern.Switch.unhandled;
import static io.harness.k8s.KubernetesConvention.getKubernetesGitSecretName;
import static io.harness.k8s.KubernetesConvention.getKubernetesRegistrySecretName;

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
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitHTTPAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitSSHAuthenticationDTO;
import io.harness.delegate.task.citasks.cik8handler.helper.ConnectorEnvVariablesHelper;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.WingsException;
import io.harness.k8s.model.ImageDetails;
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

/**
 * Helper class to create spec for image registry and GIT secrets. Generated spec can be used for creation of secrets on
 * a K8 cluster.
 */

@Slf4j
@Singleton
public class SecretSpecBuilder {
  private static final String DOCKER_REGISTRY_CREDENTIAL_TEMPLATE =
      "{\"%s\":{\"username\":\"%s\",\"password\":\"%s\"}}";
  private static final String DOCKER_REGISTRY_SECRET_TYPE = "kubernetes.io/dockercfg";

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

  public Secret getRegistrySecretSpec(ImageDetailsWithConnector imageDetailsWithConnector, String namespace) {
    ConnectorDetails connectorDetails = imageDetailsWithConnector.getImageConnectorDetails();
    String registryUrl = null;
    String username = null;
    String password = null;
    if (connectorDetails != null) {
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
      }
    }

    if (!(isNotBlank(registryUrl) && isNotBlank(username) && isNotBlank(password))) {
      return null;
    }
    imageDetailsWithConnector.getImageDetails().setUsername(username);
    imageDetailsWithConnector.getImageDetails().setPassword(password);
    imageDetailsWithConnector.getImageDetails().setRegistryUrl(registryUrl);

    String registrySecretName =
        getKubernetesRegistrySecretName(ImageDetails.builder().registryUrl(registryUrl).username(username).build());
    String credentialData = format(DOCKER_REGISTRY_CREDENTIAL_TEMPLATE, registryUrl, username, password);
    Map<String, String> data = ImmutableMap.of(DOCKER_CONFIG_KEY, encodeBase64(credentialData));
    return new SecretBuilder()
        .withNewMetadata()
        .withName(registrySecretName)
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
    Map<String, SecretParams> secretData = new HashMap<>();
    if (gitConnector == null) {
      return secretData;
    }

    GitConfigDTO gitConfigDTO = (GitConfigDTO) gitConnector.getConnectorConfig();
    log.info(
        "Decrypting git connector id:[{}], type:[{}]", gitConnector.getIdentifier(), gitConnector.getConnectorType());
    secretDecryptionService.decrypt(gitConfigDTO.getGitAuth(), gitConnector.getEncryptedDataDetails());

    GitAuthType gitAuthType = gitConfigDTO.getGitAuthType();
    if (gitAuthType == GitAuthType.HTTP) {
      GitHTTPAuthenticationDTO gitHTTPAuthenticationDTO = (GitHTTPAuthenticationDTO) gitConfigDTO.getGitAuth();

      String key = DRONE_NETRC_PASSWORD;
      secretData.put(key,
          SecretParams.builder()
              .secretKey(key)
              .value(encodeBase64(new String(gitHTTPAuthenticationDTO.getPasswordRef().getDecryptedValue())))
              .type(TEXT)
              .build());
    } else if (gitAuthType == GitAuthType.SSH) {
      GitSSHAuthenticationDTO gitHTTPAuthenticationDTO = (GitSSHAuthenticationDTO) gitConfigDTO.getGitAuth();

      String key = SSH_KEY;
      secretData.put(key,
          SecretParams.builder()
              .secretKey(key)
              .value(encodeBase64(gitHTTPAuthenticationDTO.getEncryptedSshKey()))
              .type(TEXT)
              .build());
    }
    return secretData;
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
    GitConfigDTO gitConfigDTO = (GitConfigDTO) gitConnector.getConnectorConfig();
    log.info(
        "Decrypting git connector id:[{}], type:[{}]", gitConnector.getIdentifier(), gitConnector.getConnectorType());
    secretDecryptionService.decrypt(gitConfigDTO.getGitAuth(), gitConnector.getEncryptedDataDetails());
    Map<String, String> data = new HashMap<>();

    GitAuthType gitAuthType = gitConfigDTO.getGitAuthType();
    if (gitAuthType == GitAuthType.HTTP) {
      GitHTTPAuthenticationDTO gitHTTPAuthenticationDTO = (GitHTTPAuthenticationDTO) gitConfigDTO.getGitAuth();

      String urlEncodedPwd =
          URLEncoder.encode(new String(gitHTTPAuthenticationDTO.getPasswordRef().getDecryptedValue()), "UTF-8");
      data.put(GIT_SECRET_USERNAME_KEY, encodeBase64(gitHTTPAuthenticationDTO.getUsername()));
      data.put(GIT_SECRET_PWD_KEY, encodeBase64(urlEncodedPwd));
    } else if (gitAuthType == GitAuthType.SSH) {
      GitSSHAuthenticationDTO gitHTTPAuthenticationDTO = (GitSSHAuthenticationDTO) gitConfigDTO.getGitAuth();
      data.put(GIT_SECRET_SSH_KEY, encodeBase64(gitHTTPAuthenticationDTO.getEncryptedSshKey()));
    }

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
}
