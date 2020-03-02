package software.wings.delegatetasks.validation;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.HarnessStringUtils.join;
import static io.harness.network.Http.connectableHttpUrl;
import static java.lang.String.format;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.filesystem.FileIo;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.annotations.Transient;
import org.zeroturnaround.exec.ProcessExecutor;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.AzureConfig;
import software.wings.beans.GcpConfig;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.KubernetesConfig;
import software.wings.cloudprovider.gke.GkeClusterService;
import software.wings.helpers.ext.azure.AzureHelperService;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.request.ManifestAwareTaskParams;
import software.wings.helpers.ext.kustomize.KustomizeConfig;
import software.wings.helpers.ext.kustomize.KustomizeConstants;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.settings.SettingValue;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Singleton
@Slf4j
public class K8sValidationHelper {
  @Inject @Transient private transient GkeClusterService gkeClusterService;
  @Inject @Transient private transient AzureHelperService azureHelperService;
  @Inject @Transient private transient EncryptionService encryptionService;

  boolean validateContainerServiceParams(K8sClusterConfig k8sClusterConfig) {
    SettingValue value = k8sClusterConfig.getCloudProvider();

    // see if we can decrypt from this delegate
    if (!value.isDecrypted() && isNotEmpty(k8sClusterConfig.getCloudProviderEncryptionDetails())) {
      try {
        encryptionService.decrypt((EncryptableSetting) value, k8sClusterConfig.getCloudProviderEncryptionDetails());
      } catch (Exception e) {
        logger.info("failed to decrypt " + value, e);
        return false;
      }
    }

    boolean validated;
    if (value instanceof KubernetesClusterConfig && ((KubernetesClusterConfig) value).isUseKubernetesDelegate()) {
      validated = ((KubernetesClusterConfig) value).getDelegateName().equals(System.getenv().get("DELEGATE_NAME"));
    } else {
      String url;
      url = getKubernetesMasterUrl(k8sClusterConfig);
      validated = connectableHttpUrl(url);
    }

    return validated;
  }

  public String getCriteria(K8sClusterConfig k8sClusterConfig) {
    SettingValue value = k8sClusterConfig.getCloudProvider();
    if (value instanceof KubernetesClusterConfig) {
      KubernetesClusterConfig kubernetesClusterConfig = (KubernetesClusterConfig) value;
      if (kubernetesClusterConfig.isUseKubernetesDelegate()) {
        return "delegate-name: " + kubernetesClusterConfig.getDelegateName();
      }
      return kubernetesClusterConfig.getMasterUrl();
    } else if (value instanceof KubernetesConfig) {
      return ((KubernetesConfig) value).getMasterUrl();
    } else if (value instanceof GcpConfig) {
      return "GCP:" + k8sClusterConfig.getGcpKubernetesCluster().getClusterName();
    } else if (value instanceof AzureConfig) {
      String subscriptionId = k8sClusterConfig.getAzureKubernetesCluster().getSubscriptionId();
      String resourceGroup = k8sClusterConfig.getAzureKubernetesCluster().getResourceGroup();
      return "Azure:" + subscriptionId + resourceGroup + k8sClusterConfig.getAzureKubernetesCluster().getName();
    } else {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT)
          .addParam("args", "Unknown kubernetes cloud provider setting value: " + value.getType());
    }
  }

  @Nullable
  public String getKustomizeCriteria(@Nonnull KustomizeConfig kustomizeConfig) {
    if (isNotEmpty(kustomizeConfig.getPluginRootDir())) {
      return join(":", "KustomizePluginDir", kustomizeConfig.getPluginRootDir());
    }
    return null;
  }

  public boolean kustomizeValidationNeeded(K8sTaskParameters k8sTaskParameters) {
    if (k8sTaskParameters instanceof ManifestAwareTaskParams) {
      return fetchKustomizeConfig((ManifestAwareTaskParams) k8sTaskParameters) != null;
    }
    return false;
  }

  @Nullable
  public KustomizeConfig fetchKustomizeConfig(ManifestAwareTaskParams taskParams) {
    return taskParams.getK8sDelegateManifestConfig() != null
        ? taskParams.getK8sDelegateManifestConfig().getKustomizeConfig()
        : null;
  }

  /**
   * Tests whether kustomize plugin path exists on the machine
   *
   * @param config
   * @return {@code true} if the plugin path field is null/empty or the
   * plugin path actually exists on the machine; {@code false} otherwise
   */
  public boolean doesKustomizePluginDirExist(@Nonnull KustomizeConfig config) {
    String kustomizePluginPath = renderPathUsingEnvVariables(config.getPluginRootDir());
    if (isNotEmpty(kustomizePluginPath)) {
      try {
        kustomizePluginPath = join("/", kustomizePluginPath, KustomizeConstants.KUSTOMIZE_PLUGIN_DIR_SUFFIX);
        return FileIo.checkIfFileExist(kustomizePluginPath);
      } catch (IOException e) {
        return false;
      }
    }
    return true;
  }

  private String renderPathUsingEnvVariables(String kustomizePluginPath) {
    if (isNotEmpty(kustomizePluginPath)) {
      try {
        return executeShellCommand(format("echo %s", kustomizePluginPath));
      } catch (Exception ex) {
        logger.error(format("Could not echo kustomizePluginPath %s", kustomizePluginPath));
      }
    }
    return kustomizePluginPath;
  }

  private String getKubernetesMasterUrl(K8sClusterConfig k8sClusterConfig) {
    SettingValue value = k8sClusterConfig.getCloudProvider();
    KubernetesConfig kubernetesConfig;
    if (value instanceof KubernetesConfig) {
      kubernetesConfig = (KubernetesConfig) value;
    } else {
      String namespace = k8sClusterConfig.getNamespace();
      List<EncryptedDataDetail> edd = k8sClusterConfig.getCloudProviderEncryptionDetails();
      if (value instanceof GcpConfig) {
        kubernetesConfig = gkeClusterService.getCluster(
            (GcpConfig) value, edd, k8sClusterConfig.getGcpKubernetesCluster().getClusterName(), namespace);
      } else if (value instanceof AzureConfig) {
        AzureConfig azureConfig = (AzureConfig) value;
        kubernetesConfig = azureHelperService.getKubernetesClusterConfig(azureConfig, edd,
            k8sClusterConfig.getAzureKubernetesCluster().getSubscriptionId(),
            k8sClusterConfig.getAzureKubernetesCluster().getResourceGroup(),
            k8sClusterConfig.getAzureKubernetesCluster().getName(), namespace);
      } else if (value instanceof KubernetesClusterConfig) {
        return ((KubernetesClusterConfig) value).getMasterUrl();
      } else {
        throw new WingsException(ErrorCode.INVALID_ARGUMENT)
            .addParam("args", "Unknown kubernetes cloud provider setting value: " + value.getType());
      }
    }
    return kubernetesConfig.getMasterUrl();
  }

  private String executeShellCommand(String cmd) throws InterruptedException, TimeoutException, IOException {
    return new ProcessExecutor()
        .command("/bin/sh", "-c", cmd)
        .readOutput(true)
        .timeout(5, TimeUnit.SECONDS)
        .execute()
        .outputUTF8()
        .trim();
  }
}
