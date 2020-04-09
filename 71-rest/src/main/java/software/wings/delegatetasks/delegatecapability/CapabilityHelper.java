package software.wings.delegatetasks.delegatecapability;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.artifact.ArtifactStreamType.GCR;

import com.google.inject.Singleton;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.delegate.task.mixin.ProcessExecutorCapabilityGenerator;
import io.harness.security.encryption.EncryptableSettingWithEncryptionDetails;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.EncryptionConfig;
import io.harness.security.encryption.EncryptionType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.settings.SettingValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
      task.getExecutionCapabilities().addAll(fetchExecutionCapabilitiesForSecretManagers(encryptionConfigs));
    }
  }

  private static List<ExecutionCapability> fetchExecutionCapabilitiesForSecretManagers(
      Collection<EncryptionConfig> encryptionConfigs) {
    List<ExecutionCapability> executionCapabilities = new ArrayList<>();
    encryptionConfigs.forEach(encryptionConfig -> {
      List<ExecutionCapability> encryptionConfigExecutionCapabilities =
          fetchExecutionCapabilityForSecretManager(encryptionConfig);
      executionCapabilities.addAll(encryptionConfigExecutionCapabilities);
    });

    return executionCapabilities;
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

    executionCapabilities.addAll(fetchExecutionCapabilitiesForEncryptedDataDetails(encryptedDataDetails));
    return executionCapabilities;
  }

  public static List<ExecutionCapability> fetchExecutionCapabilitiesForEncryptedDataDetails(
      List<EncryptedDataDetail> encryptedDataDetails) {
    List<ExecutionCapability> executionCapabilities = new ArrayList<>();

    if (isEmpty(encryptedDataDetails)) {
      return executionCapabilities;
    }
    return fetchExecutionCapabilitiesForSecretManagers(
        fetchEncryptionConfigsMapFromEncryptedDataDetails(encryptedDataDetails).values());
  }

  public static List<ExecutionCapability> fetchExecutionCapabilityForSecretManager(
      @NotNull EncryptionConfig encryptionConfig) {
    if (encryptionConfig instanceof ExecutionCapabilityDemander) {
      return ((ExecutionCapabilityDemander) encryptionConfig).fetchRequiredExecutionCapabilities();
    } else if (isNotEmpty(encryptionConfig.getEncryptionServiceUrl())) {
      return new ArrayList<>(
          Collections.singleton(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
              encryptionConfig.getEncryptionServiceUrl())));
    }
    return new ArrayList<>();
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
        return fetchEncryptionConfigsMapFromEncryptedDataDetails(encryptedDataDetails);
      }
      // TODO: For Task "SECRET_DECRYPT_REF", is argument is only EncryptedDataDetail.
      // Actually it should be later changed to List to match all other apis
      // can be done later
      argument = Arrays.stream(taskData.getParameters())
                     .filter(parameter -> parameter instanceof EncryptedDataDetail)
                     .findFirst()
                     .orElse(null);

      if (argument != null) {
        EncryptedDataDetail encryptedDataDetail = (EncryptedDataDetail) argument;
        return fetchEncryptionConfigsMapFromEncryptedDataDetails(Collections.singletonList(encryptedDataDetail));
      }

      // BATCH_SECRET_DECRYPT
      argument = Arrays.stream(taskData.getParameters())
                     .filter(CapabilityHelper::isEncryptableSettingWithEncryptionDetailsList)
                     .findFirst()
                     .orElse(null);

      if (argument != null) {
        List<EncryptableSettingWithEncryptionDetails> encryptableSettingWithEncryptionDetails =
            (List<EncryptableSettingWithEncryptionDetails>) argument;
        return fetchEncryptionConfigsMapFromEncryptableSettings(encryptableSettingWithEncryptionDetails);
      }

    } catch (Exception e) {
      logger.warn("Failed while generating Encryption Configs from EncryptionDataDetails: " + e);
    }
    return encryptionConfigsMap;
  }

  private static Map<String, EncryptionConfig> fetchEncryptionConfigsMapFromEncryptedDataDetails(
      List<EncryptedDataDetail> encryptedDataDetails) {
    Map<String, EncryptionConfig> encryptionConfigsMap = new HashMap<>();
    if (isEmpty(encryptedDataDetails)) {
      return encryptionConfigsMap;
    }
    List<EncryptedDataDetail> nonLocalEncryptedDetails =
        encryptedDataDetails.stream()
            .filter(encryptedDataDetail
                -> encryptedDataDetail.getEncryptedData().getEncryptionType() != EncryptionType.LOCAL)
            .collect(Collectors.toList());
    if (isNotEmpty(nonLocalEncryptedDetails)) {
      nonLocalEncryptedDetails.forEach(nonLocalEncryptedDetail
          -> encryptionConfigsMap.put(
              nonLocalEncryptedDetail.getEncryptionConfig().getUuid(), nonLocalEncryptedDetail.getEncryptionConfig()));
    }
    return encryptionConfigsMap;
  }

  private static Map<String, EncryptionConfig> fetchEncryptionConfigsMapFromEncryptableSettings(
      List<EncryptableSettingWithEncryptionDetails> encryptableSettingWithEncryptionDetails) {
    Map<String, EncryptionConfig> encryptionConfigsMap = new HashMap<>();
    encryptableSettingWithEncryptionDetails.forEach(encryptableSettingWithEncryptionDetail
        -> encryptionConfigsMap.putAll(fetchEncryptionConfigsMapFromEncryptedDataDetails(
            encryptableSettingWithEncryptionDetail.getEncryptedDataDetails())));

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

  private static boolean isEncryptableSettingWithEncryptionDetailsList(Object argument) {
    try {
      if (!(argument instanceof List)) {
        return false;
      }

      List list = (List) argument;
      if (isNotEmpty(list) && list.get(0) instanceof EncryptableSettingWithEncryptionDetails) {
        return true;
      }
    } catch (Exception e) {
      logger.warn("Failed in determining if instance of EncryptableSettingWithEncryptionDetails");
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
      List<ExecutionCapability> capabilitiesForEncryption =
          fetchExecutionCapabilitiesForEncryptedDataDetails(encryptedDataDetails);
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
}
