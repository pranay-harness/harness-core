package software.wings.delegatetasks.citasks.cik8handler;

/**
 * Helper class to create spec for image registry and GIT secrets. Generated spec can be used for creation of secrets on
 * a K8 cluster.
 */

import static io.harness.data.encoding.EncodingUtils.encodeBase64;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.utils.KubernetesConvention.getKubernetesGitSecretName;
import static software.wings.utils.KubernetesConvention.getKubernetesRegistrySecretName;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.GitConfig;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.container.ImageDetails;
import software.wings.service.intfc.security.EncryptionService;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Singleton
public class SecretSpecBuilder {
  private static final String DOCKER_REGISTRY_CREDENTIAL_TEMPLATE =
      "{\"%s\":{\"username\":\"%s\",\"password\":\"%s\"}}";
  private static final String DOCKER_REGISTRY_SECRET_TYPE = "kubernetes.io/dockercfg";

  public static final String GIT_SECRET_USERNAME_KEY = "username";
  public static final String GIT_SECRET_PWD_KEY = "password";
  public static final String GIT_SECRET_SSH_KEY = "ssh_key";
  private static final String GIT_SECRETE_TYPE = "opaque";
  private static final String DOCKER_CONFIG_KEY = ".dockercfg";

  @Inject private EncryptionService encryptionService;

  public Secret getRegistrySecretSpec(ImageDetails imageDetails, String namespace) {
    if (!(isNotBlank(imageDetails.getRegistryUrl()) && isNotBlank(imageDetails.getUsername())
            && isNotBlank(imageDetails.getPassword()))) {
      return null;
    }

    String registrySecretName = getKubernetesRegistrySecretName(imageDetails);
    String credentialData = format(DOCKER_REGISTRY_CREDENTIAL_TEMPLATE, imageDetails.getRegistryUrl(),
        imageDetails.getUsername(), imageDetails.getPassword());
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

  public Secret getGitSecretSpec(GitConfig gitConfig, List<EncryptedDataDetail> gitEncryptedDataDetails,
      String namespace) throws UnsupportedEncodingException {
    if (gitConfig == null) {
      return null;
    }

    encryptionService.decrypt(gitConfig, gitEncryptedDataDetails);
    Map<String, String> data = new HashMap<>();
    if (gitConfig.getAuthenticationScheme() == HostConnectionAttributes.AuthenticationScheme.HTTP_PASSWORD) {
      String urlEncodedPwd = URLEncoder.encode(new String(gitConfig.getPassword()), "UTF-8");
      data.put(GIT_SECRET_USERNAME_KEY, encodeBase64(gitConfig.getUsername()));
      data.put(GIT_SECRET_PWD_KEY, encodeBase64(urlEncodedPwd));
    } else {
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
        .withType(GIT_SECRETE_TYPE)
        .withData(data)
        .build();
  }
}
