package io.harness.cdng.connectornextgen;

import static io.harness.data.encoding.EncodingUtils.encodeBase64;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.fabric8.kubernetes.api.model.DoneableReplicationController;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerList;
import io.fabric8.kubernetes.api.model.extensions.DaemonSet;
import io.fabric8.kubernetes.api.model.extensions.DaemonSetList;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.api.model.extensions.DeploymentList;
import io.fabric8.kubernetes.api.model.extensions.DoneableDaemonSet;
import io.fabric8.kubernetes.api.model.extensions.DoneableDeployment;
import io.fabric8.kubernetes.api.model.extensions.DoneableReplicaSet;
import io.fabric8.kubernetes.api.model.extensions.DoneableStatefulSet;
import io.fabric8.kubernetes.api.model.extensions.ReplicaSet;
import io.fabric8.kubernetes.api.model.extensions.ReplicaSetList;
import io.fabric8.kubernetes.api.model.extensions.StatefulSet;
import io.fabric8.kubernetes.api.model.extensions.StatefulSetList;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import io.fabric8.kubernetes.client.dsl.ScalableResource;
import io.harness.delegate.beans.connector.k8Connector.ClientKeyCertDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.ServiceAccountDTO;
import io.harness.delegate.beans.connector.k8Connector.UserNamePasswordDTO;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import software.wings.service.impl.KubernetesHelperService;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Singleton
public class KubernetesValidationHelper {
  private static final String DEFULT = "default";
  @Inject KubernetesHelperService kubernetesHelperService;

  private KubernetesClient getKubernetesClient(KubernetesClusterConfigDTO kubernetesClusterConfigDTO) {
    Config config = getConfig(kubernetesClusterConfigDTO);

    String namespace = "default";
    if (isNotBlank(config.getNamespace())) {
      namespace = config.getNamespace();
    }

    OkHttpClient okHttpClient = kubernetesHelperService.createHttpClientWithProxySetting(config);
    try (DefaultKubernetesClient client = new DefaultKubernetesClient(okHttpClient, config)) {
      return client.inNamespace(namespace);
    }
  }

  private NonNamespaceOperation<ReplicationController, ReplicationControllerList, DoneableReplicationController,
      RollableScalableResource<ReplicationController, DoneableReplicationController>>
  rcOperations(KubernetesClusterConfigDTO kubernetesConfig, String namespace) {
    return getKubernetesClient(kubernetesConfig).replicationControllers().inNamespace(namespace);
  }

  private NonNamespaceOperation<Deployment, DeploymentList, DoneableDeployment,
      ScalableResource<Deployment, DoneableDeployment>>
  deploymentOperations(KubernetesClusterConfigDTO kubernetesConfig, String namespace) {
    return getKubernetesClient(kubernetesConfig).extensions().deployments().inNamespace(namespace);
  }

  private NonNamespaceOperation<ReplicaSet, ReplicaSetList, DoneableReplicaSet,
      RollableScalableResource<ReplicaSet, DoneableReplicaSet>>
  replicaOperations(KubernetesClusterConfigDTO kubernetesConfig, String namespace) {
    return getKubernetesClient(kubernetesConfig).extensions().replicaSets().inNamespace(namespace);
  }

  private NonNamespaceOperation<DaemonSet, DaemonSetList, DoneableDaemonSet, Resource<DaemonSet, DoneableDaemonSet>>
  daemonOperations(KubernetesClusterConfigDTO kubernetesConfig, String namespace) {
    return getKubernetesClient(kubernetesConfig).extensions().daemonSets().inNamespace(namespace);
  }

  private NonNamespaceOperation<StatefulSet, StatefulSetList, DoneableStatefulSet,
      RollableScalableResource<StatefulSet, DoneableStatefulSet>>
  statefulOperations(KubernetesClusterConfigDTO kubernetesConfig, String namespace) {
    return getKubernetesClient(kubernetesConfig).apps().statefulSets().inNamespace(namespace);
  }

  @VisibleForTesting
  Config getConfig(KubernetesClusterConfigDTO kubernetesConfig) {
    ConfigBuilder configBuilder = new ConfigBuilder().withTrustCerts(true);
    configBuilder.withNamespace("default");
    populateManualCredentialsInConfig(kubernetesConfig, configBuilder);
    return configBuilder.build();
  }

  private void populateManualCredentialsInConfig(
      KubernetesClusterConfigDTO kubernetesConfig, ConfigBuilder configBuilder) {
    KubernetesClusterDetailsDTO kubernetesClusterDetailsDTO =
        (KubernetesClusterDetailsDTO) kubernetesConfig.getConfig();
    if (isNotBlank(kubernetesClusterDetailsDTO.getMasterUrl())) {
      configBuilder.withMasterUrl(kubernetesClusterDetailsDTO.getMasterUrl().trim());
    }
    KubernetesAuthDTO kubernetesCredentialDTO = kubernetesClusterDetailsDTO.getAuth();
    switch (kubernetesCredentialDTO.getAuthType()) {
      case USER_PASSWORD:
        populateUserNamePasswordInConfig((UserNamePasswordDTO) kubernetesCredentialDTO.getCredentials(), configBuilder);
        break;
      case SERVICE_ACCOUNT:
        populateServiceAccountInConfig((ServiceAccountDTO) kubernetesCredentialDTO.getCredentials(), configBuilder);
        break;
      case CLIENT_KEY_CERT:
        populateClientKeyCertInConfig((ClientKeyCertDTO) kubernetesCredentialDTO.getCredentials(), configBuilder);
        break;
      default:
        break;
    }
  }

  private void populateUserNamePasswordInConfig(UserNamePasswordDTO userNamePassword, ConfigBuilder configBuilder) {
    if (isNotBlank(userNamePassword.getUsername())) {
      configBuilder.withUsername(userNamePassword.getUsername().trim());
    }
    if (isNotBlank(userNamePassword.getPassword())) {
      configBuilder.withPassword(userNamePassword.getPassword());
    }
    if (isNotBlank(userNamePassword.getCacert())) {
      configBuilder.withCaCertData(userNamePassword.getCacert());
    }
  }

  private void populateServiceAccountInConfig(ServiceAccountDTO serviceAccount, ConfigBuilder configBuilder) {
    if (serviceAccount.getServiceAccountToken() != null) {
      configBuilder.withOauthToken(serviceAccount.getServiceAccountToken());
    }
  }

  private void populateClientKeyCertInConfig(ClientKeyCertDTO clientKey, ConfigBuilder configBuilder) {
    if (clientKey.getClientCert() != null) {
      configBuilder.withClientCertData(encode(clientKey.getClientCert().toCharArray()));
    }
    if (clientKey.getClientKey() != null) {
      configBuilder.withClientKeyData(encode(clientKey.getClientKey().toCharArray()));
    }
    if (clientKey.getClientKeyPassphrase() != null) {
      configBuilder.withClientKeyPassphrase(clientKey.getClientKeyPassphrase());
    }
    if (clientKey.getClientKeyAlgo() != null) {
      configBuilder.withClientKeyAlgo(clientKey.getClientKeyAlgo().trim());
    }
  }

  private static String encode(char[] value) {
    String encodedValue = new String(value).trim();
    if (isNotBlank(encodedValue) && encodedValue.startsWith("-----BEGIN ")) {
      encodedValue = encodeBase64(encodedValue);
    }
    return encodedValue;
  }

  public List<? extends HasMetadata> listControllers(KubernetesClusterConfigDTO kubernetesConfig) {
    List<? extends HasMetadata> controllers = new ArrayList<>();
    // todo @deepak: Change the return type from generics
    boolean allFailed = true;
    try {
      controllers.addAll((List) rcOperations(kubernetesConfig, DEFULT).list().getItems());
      allFailed = false;
    } catch (RuntimeException e) {
      logger.info("KUBERNETES VALIDATION TASK: Exception in rc Operation", e);
      // Ignore
    }
    try {
      controllers.addAll((List) deploymentOperations(kubernetesConfig, DEFULT).list().getItems());
      allFailed = false;
    } catch (RuntimeException e) {
      logger.info("KUBERNETES VALIDATION TASK: Exception in deployment Operation", e);
      // Ignore
    }
    try {
      controllers.addAll((List) replicaOperations(kubernetesConfig, DEFULT).list().getItems());
      allFailed = false;
    } catch (RuntimeException e) {
      logger.info("KUBERNETES VALIDATION TASK: Exception in replica Operation", e);
      // Ignore
    }
    try {
      controllers.addAll((List) statefulOperations(kubernetesConfig, DEFULT).list().getItems());
      allFailed = false;
    } catch (RuntimeException e) {
      logger.info("KUBERNETES VALIDATION TASK: Exception in stateful Operation", e);
      // Ignore
    }
    try {
      controllers.addAll((List) daemonOperations(kubernetesConfig, DEFULT).list().getItems());
      allFailed = false;
    } catch (RuntimeException e) {
      logger.info("KUBERNETES VALIDATION TASK: Exception in daemon Operation", e);
      // Ignore
    }
    if (allFailed) {
      controllers.addAll((List) deploymentOperations(kubernetesConfig, DEFULT).list().getItems());
    }
    return controllers;
  }
}
