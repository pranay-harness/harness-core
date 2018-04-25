package software.wings.helpers.ext.container;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.helpers.ext.helm.HelmConstants.KUBE_CONFIG_TEMPLATE;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import groovy.lang.Singleton;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerStatus;
import io.fabric8.kubernetes.api.model.extensions.DaemonSet;
import io.fabric8.kubernetes.api.model.extensions.DaemonSetStatus;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.api.model.extensions.DeploymentStatus;
import io.fabric8.kubernetes.api.model.extensions.ReplicaSet;
import io.fabric8.kubernetes.api.model.extensions.ReplicaSetStatus;
import io.fabric8.kubernetes.api.model.extensions.StatefulSet;
import io.fabric8.kubernetes.api.model.extensions.StatefulSetStatus;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AzureConfig;
import software.wings.beans.ErrorCode;
import software.wings.beans.GcpConfig;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.cloudprovider.gke.GkeClusterService;
import software.wings.cloudprovider.gke.KubernetesContainerService;
import software.wings.exception.InvalidRequestException;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.azure.AzureHelperService;
import software.wings.helpers.ext.helm.HelmDeployServiceImpl.KubeControllerStatus;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.service.intfc.security.EncryptionService;

import java.io.File;
import java.io.FileWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by anubhaw on 4/20/18.
 */
@Singleton
public class ContainerDeploymentDelegateHelper {
  @Inject private AzureHelperService azureHelperService;
  @Inject private GkeClusterService gkeClusterService;
  @Inject private KubernetesContainerService kubernetesContainerService;
  @Inject private EncryptionService encryptionService;
  private static final Logger logger = LoggerFactory.getLogger(ContainerDeploymentDelegateHelper.class);
  private static final String KUBE_CONFIG_DIR = "./repository/helm/.kube/";

  public static final LoadingCache<String, Object> lockObjects =
      CacheBuilder.newBuilder().expireAfterAccess(30, TimeUnit.MINUTES).build(CacheLoader.from(Object::new));

  public String createAndGetKubeConfigLocation(ContainerServiceParams containerServiceParam) {
    try {
      KubernetesConfig kubernetesConfig = getKubernetesConfig(containerServiceParam);
      encryptionService.decrypt(kubernetesConfig, containerServiceParam.getEncryptionDetails());

      SettingAttribute settingAttribute = containerServiceParam.getSettingAttribute();
      if (settingAttribute.getValue() instanceof KubernetesConfig
          || settingAttribute.getValue() instanceof KubernetesClusterConfig) {
        kubernetesConfig.setCaCert(getEncodedChars(kubernetesConfig.getCaCert()));
        kubernetesConfig.setClientCert(getEncodedChars(kubernetesConfig.getClientCert()));
        kubernetesConfig.setClientKey(getEncodedChars(kubernetesConfig.getClientKey()));
      }

      String configFileContent = getConfigFileContent(kubernetesConfig);
      String md5Hash = DigestUtils.md5Hex(configFileContent);

      synchronized (lockObjects.get(md5Hash)) {
        String configFilePath = KUBE_CONFIG_DIR + md5Hash;
        File file = new File(configFilePath);
        if (!file.exists()) {
          file.getParentFile().mkdirs();
          try (FileWriter writer = new FileWriter(file)) {
            writer.write(configFileContent);
          }
        }
        return file.getAbsolutePath();
      }
    } catch (Exception e) {
      logger.error("Error occurred in creating config file", e);
      throw new WingsException(ErrorCode.INVALID_REQUEST).addParam("message", e.getMessage());
    }
  }

  protected char[] getEncodedChars(char[] chars) throws UnsupportedEncodingException {
    if (isEmpty(chars) || !(new String(chars).startsWith("-----BEGIN "))) {
      return chars;
    }

    byte[] encode = Base64.getEncoder().encode(new String(chars).getBytes("UTF-8"));
    return new String(encode, "UTF-8").toCharArray();
  }

  private String getConfigFileContent(KubernetesConfig config) {
    String clientCertData =
        isNotEmpty(config.getClientCert()) ? "client-certificate-data: " + new String(config.getClientCert()) : "";
    String clientKeyData =
        isNotEmpty(config.getClientKey()) ? "client-key-data: " + new String(config.getClientKey()) : "";
    String password = isNotEmpty(config.getPassword()) ? "password: " + new String(config.getPassword()) : "";
    String username = isNotEmpty(config.getUsername()) ? "username: " + new String(config.getUsername()) : "";

    return KUBE_CONFIG_TEMPLATE.replace("${MASTER_URL}", config.getMasterUrl())
        .replace("${USER_NAME}", username)
        .replace("${CLIENT_CERT_DATA}", clientCertData)
        .replace("${CLIENT_KEY_DATA}", clientKeyData)
        .replace("${PASSWORD}", password);
  }

  public String getKubeConfigFileContent(ContainerServiceParams containerServiceParam) {
    KubernetesConfig kubernetesConfig = getKubernetesConfig(containerServiceParam);
    encryptionService.decrypt(kubernetesConfig, containerServiceParam.getEncryptionDetails());
    return getConfigFileContent(kubernetesConfig);
  }

  public KubernetesConfig getKubernetesConfig(ContainerServiceParams containerServiceParam) {
    SettingAttribute settingAttribute = containerServiceParam.getSettingAttribute();
    List<EncryptedDataDetail> encryptedDataDetails = containerServiceParam.getEncryptionDetails();
    String clusterName = containerServiceParam.getClusterName();
    String namespace = containerServiceParam.getNamespace();

    KubernetesConfig kubernetesConfig;
    if (settingAttribute.getValue() instanceof KubernetesConfig) {
      kubernetesConfig = (KubernetesConfig) settingAttribute.getValue();
    } else if (settingAttribute.getValue() instanceof KubernetesClusterConfig) {
      kubernetesConfig = ((KubernetesClusterConfig) settingAttribute.getValue()).createKubernetesConfig(namespace);
    } else if (settingAttribute.getValue() instanceof GcpConfig) {
      kubernetesConfig = gkeClusterService.getCluster(settingAttribute, encryptedDataDetails, clusterName, namespace);
      kubernetesConfig.setDecrypted(true);
    } else if (settingAttribute.getValue() instanceof AzureConfig) {
      AzureConfig azureConfig = (AzureConfig) settingAttribute.getValue();
      kubernetesConfig = azureHelperService.getKubernetesClusterConfig(azureConfig, encryptedDataDetails,
          containerServiceParam.getSubscriptionId(), containerServiceParam.getResourceGroup(), clusterName, namespace);
      kubernetesConfig.setDecrypted(true);
    } else {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT)
          .addParam(
              "args", "Unknown kubernetes cloud provider setting value: " + settingAttribute.getValue().getType());
    }
    return kubernetesConfig;
  }

  public KubeControllerStatus getControllerStatus(HasMetadata hasMetadata) {
    if ("Deployment".equals(hasMetadata.getKind())) {
      DeploymentStatus status = ((Deployment) hasMetadata).getStatus();
      return KubeControllerStatus.builder()
          .name(hasMetadata.getMetadata().getName())
          .kind(hasMetadata.getKind())
          .runningCount(status.getReadyReplicas() == null ? 0 : status.getReadyReplicas())
          .desiredCount(status.getReplicas() == null ? 0 : status.getReplicas())
          .build();
    } else if ("StatefulSet".equals(hasMetadata.getKind())) {
      StatefulSetStatus status = ((StatefulSet) hasMetadata).getStatus();
      return KubeControllerStatus.builder()
          .name(hasMetadata.getMetadata().getName())
          .kind(hasMetadata.getKind())
          .runningCount(status.getReadyReplicas() == null ? 0 : status.getReadyReplicas())
          .desiredCount(status.getReplicas() == null ? 0 : status.getReplicas())
          .build();
    } else if ("ReplicaSet".equals(hasMetadata.getKind())) {
      ReplicaSetStatus status = ((ReplicaSet) hasMetadata).getStatus();
      return KubeControllerStatus.builder()
          .name(hasMetadata.getMetadata().getName())
          .kind(hasMetadata.getKind())
          .runningCount(status.getReadyReplicas() == null ? 0 : status.getReadyReplicas())
          .desiredCount(status.getReplicas() == null ? 0 : status.getReplicas())
          .build();
    } else if ("ReplicationController".equals(hasMetadata.getKind())) {
      ReplicationControllerStatus status = ((ReplicationController) hasMetadata).getStatus();
      return KubeControllerStatus.builder()
          .name(hasMetadata.getMetadata().getName())
          .kind(hasMetadata.getKind())
          .runningCount(status.getReadyReplicas() == null ? 0 : status.getReadyReplicas())
          .desiredCount(status.getReplicas() == null ? 0 : status.getReplicas())
          .build();
    } else if ("DaemonSet".equals(hasMetadata.getKind())) {
      DaemonSetStatus status = ((DaemonSet) hasMetadata).getStatus();
      return KubeControllerStatus.builder()
          .name(hasMetadata.getMetadata().getName())
          .kind(hasMetadata.getKind())
          .runningCount(status.getNumberReady() == null ? 0 : status.getNumberReady())
          .desiredCount(status.getDesiredNumberScheduled() == null ? 0 : status.getDesiredNumberScheduled())
          .build();
    } else {
      throw new InvalidRequestException("Unhandled resource type" + hasMetadata.getKind());
    }
  }

  public List<ContainerInfo> getContainerInfosWhenReadyByLabels(ContainerServiceParams containerServiceParams,
      KubernetesConfig kubernetesConfig, ExecutionLogCallback executionLogCallback, Map<String, String> labels) {
    List<? extends HasMetadata> controllers = kubernetesContainerService.getControllers(
        kubernetesConfig, containerServiceParams.getEncryptionDetails(), labels);

    List<KubeControllerStatus> controllerStatuses = controllers.stream()
                                                        .map(this ::getControllerStatus)
                                                        .filter(this ::steadyStateCheckRequired)
                                                        .collect(Collectors.toList());

    executionLogCallback.saveExecutionLog(String.format("Deployed Controllers [%s]:", controllerStatuses.size()));
    controllerStatuses.forEach(kubeControllerStatus
        -> executionLogCallback.saveExecutionLog(String.format("Kind:%s, Name:%s (desired: %s)",
            kubeControllerStatus.getKind(), kubeControllerStatus.getName(), kubeControllerStatus.getDesiredCount())));

    List<ContainerInfo> containerInfoList = new ArrayList<>();
    if (controllerStatuses.size() > 0) {
      containerInfoList =
          controllerStatuses.stream()
              .flatMap(controllerStatus
                  -> kubernetesContainerService
                         .getContainerInfosWhenReady(kubernetesConfig, containerServiceParams.getEncryptionDetails(),
                             controllerStatus.getName(), 0, controllerStatus.getDesiredCount(),
                             (int) TimeUnit.MINUTES.toMinutes(30), new ArrayList<>(),
                             controllerStatus.getKind().equals("DaemonSet"), executionLogCallback, true, 0)
                         .stream())
              .collect(Collectors.toList());
    }
    return containerInfoList;
  }

  public List<ContainerInfo> getContainerInfosWhenReadyByLabel(String labelName, String labelValue,
      ContainerServiceParams containerServiceParams, KubernetesConfig kubernetesConfig,
      ExecutionLogCallback executionLogCallback) {
    return getContainerInfosWhenReadyByLabels(
        containerServiceParams, kubernetesConfig, executionLogCallback, ImmutableMap.of(labelName, labelValue));
  }

  private boolean steadyStateCheckRequired(KubeControllerStatus controllerStatus) {
    boolean noSteadyCheckRequire = (controllerStatus.getDesiredCount() == 0 && controllerStatus.getRunningCount() == 0)
        || "Deployment".equals(controllerStatus.getKind());

    if (noSteadyCheckRequire) {
      logger.info("Controller doesn't need steady state check. [{}]", controllerStatus);
    }
    return !noSteadyCheckRequire;
  }
}
