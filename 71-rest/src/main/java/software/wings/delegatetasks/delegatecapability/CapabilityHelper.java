package software.wings.delegatetasks.delegatecapability;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.artifact.ArtifactStreamType.GCR;

import com.google.inject.Singleton;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.executioncapability.ChartMuseumCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.beans.executioncapability.HelmCapability;
import io.harness.delegate.beans.executioncapability.HttpConnectionExecutionCapability;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.delegate.task.mixin.ProcessExecutorCapabilityGenerator;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.EncryptionConfig;
import io.harness.security.encryption.EncryptionType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.KmsConfig;
import software.wings.beans.VaultConfig;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.settings.SettingValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Singleton
@Slf4j
public class CapabilityHelper {
  public static final String TERRAFORM = "terraform";
  public static final String HELM = "helm";
  public static final String CHART_MUSEUM = "chart-museum";

  private static final String HELM_VERSION_COMMAND = "${HELM_PATH} version -c";

  private static final String CHART_MUSEUM_VERSION_COMMAND = "${CHART_MUSEUM_PATH} -v";

  public static void embedCapabilitiesInDelegateTask(
      DelegateTask task, Collection<EncryptionConfig> encryptionConfigs) {
    if (isEmpty(task.getData().getParameters()) || isNotEmpty(task.getExecutionCapabilities())) {
      return;
    }

    task.setExecutionCapabilities(new ArrayList<>());
    task.getExecutionCapabilities().addAll(
        Arrays.stream(task.getData().getParameters())
            .filter(param -> param instanceof ExecutionCapabilityDemander)
            .flatMap(param -> ((ExecutionCapabilityDemander) param).fetchRequiredExecutionCapabilities().stream())
            .collect(toList()));

    if (isNotEmpty(encryptionConfigs)) {
      encryptionConfigs.forEach(encryptionConfig -> {
        ExecutionCapability vaultCapability = getHttpCapabilityForDecryption(encryptionConfig);
        if (vaultCapability != null) {
          task.getExecutionCapabilities().add(vaultCapability);
        }
      });
    }
  }

  public static List<ExecutionCapability> generateDelegateCapabilities(
      ExecutionCapabilityDemander capabilityDemander, List<EncryptedDataDetail> encryptedDataDetails) {
    List<ExecutionCapability> executionCapabilities = new ArrayList<>();

    if (capabilityDemander != null) {
      executionCapabilities.addAll(capabilityDemander.fetchRequiredExecutionCapabilities());
    }
    if (isEmpty(encryptedDataDetails)) {
      return executionCapabilities;
    }

    executionCapabilities.addAll(generateKmsHttpCapabilities(encryptedDataDetails));
    return executionCapabilities;
  }

  public static List<ExecutionCapability> generateKmsHttpCapabilities(List<EncryptedDataDetail> encryptedDataDetails) {
    List<ExecutionCapability> executionCapabilities = new ArrayList<>();

    if (isEmpty(encryptedDataDetails)) {
      return executionCapabilities;
    }

    EncryptedDataDetail encryptedDataDetail = encryptedDataDetails.get(0);
    ExecutionCapability vaultCapability = getHttpCapabilityForDecryption(encryptedDataDetail.getEncryptionConfig());
    if (vaultCapability != null) {
      executionCapabilities.add(vaultCapability);
    }

    return executionCapabilities;
  }

  public static HttpConnectionExecutionCapability getHttpCapabilityForDecryption(
      @NotNull EncryptionConfig encryptionConfig) {
    if (encryptionConfig instanceof KmsConfig) {
      return HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapabilityForKms(
          ((KmsConfig) encryptionConfig).getRegion());
    } else if (encryptionConfig instanceof VaultConfig) {
      return HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
          ((VaultConfig) encryptionConfig).getVaultUrl());
    } else if (isNotEmpty(encryptionConfig.getEncryptionServiceUrl())) {
      return HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
          encryptionConfig.getEncryptionServiceUrl());
    }
    return null;
  }

  public static Map<String, EncryptionConfig> fetchEncryptionDetailsListFromParameters(TaskData taskData) {
    Map<String, EncryptionConfig> encryptionConfigsMap = new HashMap<>();
    // TODO: Remove this when jenkins/bambooConfig etc are integrated with new framework
    try {
      Object argument = Arrays.stream(taskData.getParameters())
                            .filter(CapabilityHelper::isEncryptionDetailsList)
                            .findFirst()
                            .orElse(null);

      if (argument != null) {
        List<EncryptedDataDetail> encryptedDataDetails = (List<EncryptedDataDetail>) argument;

        List<EncryptedDataDetail> nonLocalEncryptedDetails =
            encryptedDataDetails.stream()
                .filter(encryptedDataDetail
                    -> encryptedDataDetail.getEncryptedData().getEncryptionType() != EncryptionType.LOCAL)
                .collect(Collectors.toList());

        // There can be more than 1 non-Local encryptedDataDetails.
        // e.g. in case of JenkinConfig, it has token / username-password. User will select 1
        // of the auth mechanism. In this case, it will have 2 encryptedDataDetails (same entry twice)
        if (isNotEmpty(nonLocalEncryptedDetails)) {
          EncryptedDataDetail encryptedDataDetail = nonLocalEncryptedDetails.get(0);
          encryptionConfigsMap.put(
              encryptedDataDetail.getEncryptionConfig().getUuid(), encryptedDataDetail.getEncryptionConfig());
        }
      } else {
        // TODO: For Task "SECRET_DECRYPT_REF", is argument is only EncryptedDataDetail.
        // Actually it should be later changed to List to match all other apis
        // can be done later
        argument = Arrays.stream(taskData.getParameters())
                       .filter(parameter -> parameter instanceof EncryptedDataDetail)
                       .findFirst()
                       .orElse(null);

        if (argument != null) {
          EncryptedDataDetail encryptedDataDetail = (EncryptedDataDetail) argument;
          encryptionConfigsMap.put(
              encryptedDataDetail.getEncryptionConfig().getUuid(), encryptedDataDetail.getEncryptionConfig());
        }
      }
    } catch (Exception e) {
      logger.warn("Failed while generating Encryption Configs from EncryptionDataDetails: " + e);
    }
    return encryptionConfigsMap;
  }

  public static boolean isTaskParameterType(TaskData taskData) {
    return taskData.getParameters().length == 1 && taskData.getParameters()[0] instanceof TaskParameters;
  }

  private static boolean isEncryptionDetailsList(Object argument) {
    // TODO: Remove this when jenkins/bamoConfig etc are integrated with new framework
    try {
      if (!(argument instanceof List)) {
        return false;
      }

      List list = (List) argument;
      if (isNotEmpty(list) && list.get(0) instanceof EncryptedDataDetail) {
        return true;
      }
    } catch (Exception e) {
      logger.warn("Failed in determining if instance of EncryptionDetails");
    }

    return false;
  }

  public static String generateLogStringWithCapabilitiesGenerated(DelegateTask task) {
    StringBuilder builder = new StringBuilder(128)
                                .append("Capabilities Generate for Task: ")
                                .append(task.getData().getTaskType())
                                .append(" are: ");

    task.getExecutionCapabilities().forEach(capability -> builder.append('\n').append(capability.toString()));
    return builder.toString();
  }

  /**
   * This is Used by BuildSourceParameters
   */
  public static List<ExecutionCapability> generateCapabilities(
      SettingValue settingValue, ArtifactStreamAttributes artifactStreamAttributes) {
    String artifactStreamType = artifactStreamAttributes.getArtifactStreamType();

    List<ExecutionCapability> executionCapabilities = new ArrayList<>();
    if (artifactStreamType.equals(GCR.name())) {
      String hostName = artifactStreamAttributes.getRegistryHostName();
      executionCapabilities.add(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
          new StringBuilder(128)
              .append("https://")
              .append(hostName)
              .append(hostName.endsWith("/") ? "" : "/")
              .toString()));

    } else {
      executionCapabilities.addAll(settingValue.fetchRequiredExecutionCapabilities());
    }

    return executionCapabilities;
  }

  public static List<ExecutionCapability> generateExecutionCapabilitiesForProcessExecutor(
      String category, List<String> processExecutorArguments, List<EncryptedDataDetail> encryptedDataDetails) {
    List<ExecutionCapability> executionCapabilities = new ArrayList<>();
    executionCapabilities.add(
        ProcessExecutorCapabilityGenerator.buildProcessExecutorCapability(category, processExecutorArguments));

    if (isNotEmpty(encryptedDataDetails)) {
      List<ExecutionCapability> capabilitiesForEncryption = generateKmsHttpCapabilities(encryptedDataDetails);
      if (isNotEmpty(capabilitiesForEncryption)) {
        executionCapabilities.addAll(capabilitiesForEncryption);
      }
    }
    return executionCapabilities;
  }

  public static List<ExecutionCapability> generateExecutionCapabilitiesForTerraform(
      List<EncryptedDataDetail> encryptedDataDetails) {
    List<String> processExecutorArguments = new ArrayList<>();
    processExecutorArguments.add("/bin/sh");
    processExecutorArguments.add("-c");
    processExecutorArguments.add("terraform --version");

    return generateExecutionCapabilitiesForProcessExecutor(TERRAFORM, processExecutorArguments, encryptedDataDetails);
  }

  public static List<ExecutionCapability> generateExecutionCapabilitiesForHelm(
      List<EncryptedDataDetail> encryptedDataDetails) {
    List<ExecutionCapability> executionCapabilities = new ArrayList<>();
    executionCapabilities.add(HelmCapability.builder().helmCommand(HELM_VERSION_COMMAND).build());

    if (isNotEmpty(encryptedDataDetails)) {
      List<ExecutionCapability> capabilitiesForEncryption = generateKmsHttpCapabilities(encryptedDataDetails);
      if (isNotEmpty(capabilitiesForEncryption)) {
        executionCapabilities.addAll(capabilitiesForEncryption);
      }
    }
    return executionCapabilities;
  }

  public static List<ExecutionCapability> generateExecutionCapabilitiesForChartMuseum(
      List<EncryptedDataDetail> encryptedDataDetails) {
    List<ExecutionCapability> executionCapabilities = new ArrayList<>();
    executionCapabilities.add(ChartMuseumCapability.builder().chartMuseumCommand(CHART_MUSEUM_VERSION_COMMAND).build());
    if (isNotEmpty(encryptedDataDetails)) {
      List<ExecutionCapability> capabilitiesForEncryption = generateKmsHttpCapabilities(encryptedDataDetails);
      if (isNotEmpty(capabilitiesForEncryption)) {
        executionCapabilities.addAll(capabilitiesForEncryption);
      }
    }
    return executionCapabilities;
  }
}
