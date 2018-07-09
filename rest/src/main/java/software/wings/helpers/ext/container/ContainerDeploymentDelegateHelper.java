package software.wings.helpers.ext.container;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.helpers.ext.helm.HelmConstants.KUBE_CONFIG_TEMPLATE;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import groovy.lang.Singleton;
import io.fabric8.kubernetes.api.model.HasMetadata;
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
import software.wings.beans.command.LogCallback;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.cloudprovider.gke.GkeClusterService;
import software.wings.cloudprovider.gke.KubernetesContainerService;
import software.wings.exception.InvalidRequestException;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.azure.AzureHelperService;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.utils.Misc;

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

  @SuppressFBWarnings({"DM_DEFAULT_ENCODING", "RV_RETURN_VALUE_IGNORED_BAD_PRACTICE"})
  public String createAndGetKubeConfigLocation(ContainerServiceParams containerServiceParam) {
    try {
      KubernetesConfig kubernetesConfig = getKubernetesConfig(containerServiceParam);
      encryptionService.decrypt(kubernetesConfig, containerServiceParam.getEncryptionDetails());

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
      throw new InvalidRequestException(Misc.getMessage(e), e);
    }
  }

  private char[] getEncodedChars(char[] chars) throws UnsupportedEncodingException {
    if (isEmpty(chars) || !(new String(chars).startsWith("-----BEGIN "))) {
      return chars;
    }

    byte[] encode = Base64.getEncoder().encode(new String(chars).getBytes("UTF-8"));
    return new String(encode, "UTF-8").toCharArray();
  }

  private void encodeCharsIfNeeded(KubernetesConfig config) throws UnsupportedEncodingException {
    config.setCaCert(getEncodedChars(config.getCaCert()));
    config.setClientCert(getEncodedChars(config.getClientCert()));
    config.setClientKey(getEncodedChars(config.getClientKey()));
  }

  private String getConfigFileContent(KubernetesConfig config) throws UnsupportedEncodingException {
    encodeCharsIfNeeded(config);

    if (isBlank(config.getMasterUrl())) {
      return "";
    }

    String clientCertData =
        isNotEmpty(config.getClientCert()) ? "client-certificate-data: " + new String(config.getClientCert()) : "";
    String clientKeyData =
        isNotEmpty(config.getClientKey()) ? "client-key-data: " + new String(config.getClientKey()) : "";
    String password = isNotEmpty(config.getPassword()) ? "password: " + new String(config.getPassword()) : "";
    String username = isNotEmpty(config.getUsername()) ? "username: " + config.getUsername() : "";

    return KUBE_CONFIG_TEMPLATE.replace("${MASTER_URL}", config.getMasterUrl())
        .replace("${USER_NAME}", username)
        .replace("${CLIENT_CERT_DATA}", clientCertData)
        .replace("${CLIENT_KEY_DATA}", clientKeyData)
        .replace("${PASSWORD}", password);
  }

  public String getKubeConfigFileContent(ContainerServiceParams containerServiceParam) {
    try {
      KubernetesConfig kubernetesConfig = getKubernetesConfig(containerServiceParam);
      encryptionService.decrypt(kubernetesConfig, containerServiceParam.getEncryptionDetails());
      return getConfigFileContent(kubernetesConfig);
    } catch (Exception e) {
      throw new InvalidRequestException(Misc.getMessage(e), e);
    }
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

  public int getControllerCountByLabels(
      ContainerServiceParams containerServiceParams, KubernetesConfig kubernetesConfig, Map<String, String> labels) {
    List<? extends HasMetadata> controllers = kubernetesContainerService.getControllers(
        kubernetesConfig, containerServiceParams.getEncryptionDetails(), labels);

    return controllers.size();
  }

  public List<ContainerInfo> getContainerInfosWhenReadyByLabels(ContainerServiceParams containerServiceParams,
      KubernetesConfig kubernetesConfig, ExecutionLogCallback executionLogCallback, Map<String, String> labels) {
    List<? extends HasMetadata> controllers = kubernetesContainerService.getControllers(
        kubernetesConfig, containerServiceParams.getEncryptionDetails(), labels);

    executionLogCallback.saveExecutionLog(format("Deployed Controllers [%s]:", controllers.size()));
    controllers.forEach(controller
        -> executionLogCallback.saveExecutionLog(format("Kind:%s, Name:%s (desired: %s)", controller.getKind(),
            controller.getMetadata().getName(), kubernetesContainerService.getControllerPodCount(controller))));

    List<ContainerInfo> containerInfoList = new ArrayList<>();
    if (controllers.size() > 0) {
      containerInfoList =
          controllers.stream()
              .filter(controller
                  -> !(controller.getKind().equals("ReplicaSet")
                      && controller.getMetadata().getOwnerReferences() != null))
              .flatMap(controller -> {
                boolean isNotVersioned =
                    controller.getKind().equals("DaemonSet") || controller.getKind().equals("StatefulSet");
                return kubernetesContainerService
                    .getContainerInfosWhenReady(kubernetesConfig, containerServiceParams.getEncryptionDetails(),
                        controller.getMetadata().getName(), 0, (int) TimeUnit.MINUTES.toMinutes(30), new ArrayList<>(),
                        isNotVersioned, executionLogCallback, true, 0)
                    .stream();
              })
              .collect(Collectors.toList());
    }
    return containerInfoList;
  }

  public List<ContainerInfo> getContainerInfosWhenReadyByLabel(String labelName, String labelValue,
      ContainerServiceParams containerServiceParams, KubernetesConfig kubernetesConfig,
      LogCallback executionLogCallback) {
    return getContainerInfosWhenReadyByLabels(containerServiceParams, kubernetesConfig,
        (ExecutionLogCallback) executionLogCallback, ImmutableMap.of(labelName, labelValue));
  }
}
