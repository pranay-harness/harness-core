package software.wings.delegatetasks.citasks.cik8handler;

import static io.harness.data.encoding.EncodingUtils.encodeBase64;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.govern.Switch.unhandled;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.ci.pod.SecretParams.Type.FILE;
import static software.wings.beans.ci.pod.SecretParams.Type.TEXT;
import static software.wings.utils.KubernetesConvention.getKubernetesGitSecretName;
import static software.wings.utils.KubernetesConvention.getKubernetesRegistrySecretName;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptableSettingWithEncryptionDetails;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.AwsConfig;
import software.wings.beans.DockerConfig;
import software.wings.beans.GcpConfig;
import software.wings.beans.GitConfig;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.SettingAttribute;
import software.wings.beans.ci.pod.EncryptedVariableWithType;
import software.wings.beans.ci.pod.ImageDetailsWithConnector;
import software.wings.beans.ci.pod.SecretParams;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.beans.container.ImageDetails;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingVariableTypes;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
  private static final String USERNAME_PREFIX = "USERNAME_";
  private static final String PASSWORD_PREFIX = "PASSWORD_";
  private static final String ENDPOINT_PREFIX = "ENDPOINT_";
  private static final String ACCESS_KEY_PREFIX = "ACCESS_KEY_";
  private static final String SECRET_KEY_PREFIX = "SECRET_KEY_";
  private static final String SECRET_PATH_PREFIX = "SECRET_PATH_";

  @Inject private EncryptionService encryptionService;

  public Secret getRegistrySecretSpec(ImageDetailsWithConnector imageDetailsWithConnector, String namespace) {
    EncryptableSetting encryptableSetting = imageDetailsWithConnector.getEncryptableSetting();
    String registryUrl = null;
    String username = null;
    String password = null;
    encryptionService.decrypt(
        imageDetailsWithConnector.getEncryptableSetting(), imageDetailsWithConnector.getEncryptedDataDetails());
    if (encryptableSetting != null) {
      SettingVariableTypes settingType = encryptableSetting.getSettingType();
      switch (settingType) {
        case DOCKER:
          DockerConfig dockerConfig = (DockerConfig) encryptableSetting;
          registryUrl = dockerConfig.getDockerRegistryUrl();
          username = dockerConfig.getUsername();
          password = String.valueOf(dockerConfig.getPassword());
          break;
        default:
          unhandled(settingType);
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

  public Map<String, SecretParams> decryptCustomSecretVariables(
      Map<String, EncryptedVariableWithType> encryptedSecrets) {
    Map<String, SecretParams> data = new HashMap<>();
    if (isNotEmpty(encryptedSecrets)) {
      for (Map.Entry<String, EncryptedVariableWithType> encryptedVariable : encryptedSecrets.entrySet()) {
        try {
          String value = String.valueOf(
              encryptionService.getDecryptedValue(encryptedVariable.getValue().getEncryptedDataDetail()));
          switch (encryptedVariable.getValue().getType()) {
            case FILE:
              data.put(encryptedVariable.getKey(),
                  SecretParams.builder()
                      .secretKey(SECRET_KEY + encryptedVariable.getKey())
                      .type(FILE)
                      .value(encodeBase64(value))
                      .build());
              break;
            case TEXT:
              data.put(encryptedVariable.getKey(),
                  SecretParams.builder()
                      .secretKey(SECRET_KEY + encryptedVariable.getKey())
                      .type(TEXT)
                      .value(encodeBase64(value))
                      .build());
              break;
            default:
              unhandled(encryptedVariable.getValue().getType());
          }
        } catch (IOException e) {
          throw new WingsException("Error occurred while decrypting encrypted variables", e);
        }
      }
    }

    return data;
  }

  public Map<String, SecretParams> decryptPublishArtifactSecretVariables(
      Map<String, EncryptableSettingWithEncryptionDetails> publishArtifactEncryptedValues) {
    Map<String, SecretParams> secretData = new HashMap<>();
    if (isNotEmpty(publishArtifactEncryptedValues)) {
      for (Map.Entry<String, EncryptableSettingWithEncryptionDetails> encryptedVariable :
          publishArtifactEncryptedValues.entrySet()) {
        List<EncryptableSettingWithEncryptionDetails> detailsList =
            encryptionService.decrypt(Collections.singletonList(encryptedVariable.getValue()));

        EncryptableSettingWithEncryptionDetails encryptableSettingWithEncryptionDetails =
            detailsList.stream().findFirst().orElse(null);

        if (encryptableSettingWithEncryptionDetails != null) {
          EncryptableSetting encryptableSetting = encryptableSettingWithEncryptionDetails.getEncryptableSetting();

          if (encryptableSetting != null) {
            SettingVariableTypes settingType = encryptableSetting.getSettingType();
            switch (settingType) {
              case DOCKER:
                DockerConfig dockerConfig = (DockerConfig) encryptableSetting;
                String registryUrl = dockerConfig.getDockerRegistryUrl();
                String username = dockerConfig.getUsername();
                String password = String.valueOf(dockerConfig.getPassword());

                secretData.put(USERNAME_PREFIX + encryptedVariable.getKey(),
                    getVariableSecret(USERNAME_PREFIX + encryptedVariable.getKey(), username));
                secretData.put(PASSWORD_PREFIX + encryptedVariable.getKey(),
                    getVariableSecret(PASSWORD_PREFIX + encryptedVariable.getKey(), password));
                secretData.put(ENDPOINT_PREFIX + encryptedVariable.getKey(),
                    getVariableSecret(ENDPOINT_PREFIX + encryptedVariable.getKey(), registryUrl));

                break;
              case AWS:
                AwsConfig awsConfig = (AwsConfig) encryptableSetting;
                String accessKey = awsConfig.getAccessKey();
                String secretKey = String.valueOf(awsConfig.getSecretKey());

                secretData.put(ACCESS_KEY_PREFIX + encryptedVariable.getKey(),
                    getVariableSecret(ACCESS_KEY_PREFIX + encryptedVariable.getKey(), accessKey));
                secretData.put(SECRET_KEY_PREFIX + encryptedVariable.getKey(),
                    getVariableSecret(SECRET_KEY_PREFIX + encryptedVariable.getKey(), secretKey));
                break;
              case ARTIFACTORY:
                ArtifactoryConfig artifactoryConfig = (ArtifactoryConfig) encryptableSetting;
                String artifactoryConfigUsername = artifactoryConfig.getUsername();
                String artifactoryConfigPassword = String.valueOf(artifactoryConfig.getPassword());

                secretData.put(USERNAME_PREFIX + encryptedVariable.getKey(),
                    getVariableSecret(USERNAME_PREFIX + encryptedVariable.getKey(), artifactoryConfigUsername));
                secretData.put(PASSWORD_PREFIX + encryptedVariable.getKey(),
                    getVariableSecret(PASSWORD_PREFIX + encryptedVariable.getKey(), artifactoryConfigPassword));
                break;
              case GCP:
                GcpConfig gcpConfig = (GcpConfig) encryptableSetting;
                String serviceAccountKeyFileContent = String.valueOf(gcpConfig.getServiceAccountKeyFileContent());
                secretData.put(SECRET_PATH_PREFIX + encryptedVariable.getKey(),
                    getFileSecret(SECRET_PATH_PREFIX + encryptedVariable.getKey(), serviceAccountKeyFileContent));
                break;
              default:
                unhandled(settingType);
            }
          }
        }
      }
    }

    return secretData;
  }
  private SecretParams getVariableSecret(String key, String secret) {
    return SecretParams.builder().secretKey(key).value(encodeBase64(secret)).type(TEXT).build();
  }

  private SecretParams getFileSecret(String key, String secret) {
    return SecretParams.builder().secretKey(key).value(encodeBase64(secret)).type(FILE).build();
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

  public Secret getGitSecretSpec(GitConfig gitConfig, List<EncryptedDataDetail> gitEncryptedDataDetails,
      String namespace) throws UnsupportedEncodingException {
    if (gitConfig == null) {
      return null;
    }

    encryptionService.decrypt(gitConfig, gitEncryptedDataDetails);
    Map<String, String> data = new HashMap<>();
    if (isNotEmpty(gitConfig.getUsername()) && isNotEmpty(gitConfig.getPassword())) {
      String urlEncodedPwd = URLEncoder.encode(new String(gitConfig.getPassword()), "UTF-8");
      data.put(GIT_SECRET_USERNAME_KEY, encodeBase64(gitConfig.getUsername()));
      data.put(GIT_SECRET_PWD_KEY, encodeBase64(urlEncodedPwd));
    } else if (isNotEmpty(gitConfig.getSshSettingId()) && gitConfig.getSshSettingAttribute() != null) {
      SettingAttribute sshSettingAttribute = gitConfig.getSshSettingAttribute();
      SettingValue settingValue = sshSettingAttribute.getValue();
      if (!(settingValue instanceof HostConnectionAttributes)) {
        String errMsg = "Type mismatch: Git config SSH setting value not a type of HostConnectionAttributes";
        logger.error(errMsg);
        throw new InvalidRequestException(errMsg);
      }

      HostConnectionAttributes hostConnectionAttributes = (HostConnectionAttributes) settingValue;
      if (isNotEmpty(hostConnectionAttributes.getKey())) {
        String sshKey = new String(hostConnectionAttributes.getKey());
        data.put(GIT_SECRET_SSH_KEY, encodeBase64(sshKey));
      }
    }

    if (data.isEmpty()) {
      String errMsg = format("Invalid GIT Authentication scheme %s for repository %s",
          gitConfig.getAuthenticationScheme().toString(), gitConfig.getRepoUrl());
      logger.error(errMsg);
      throw new InvalidArgumentsException(errMsg, WingsException.USER);
    }

    String secretName = getKubernetesGitSecretName(gitConfig.getRepoUrl());
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
