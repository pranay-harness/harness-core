package software.wings.delegatetasks.delegatecapability;

import static io.harness.annotations.dev.HarnessTeam.DEL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static software.wings.beans.artifact.ArtifactStreamType.GCR;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.capability.EncryptedDataDetailsCapabilityHelper;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.expression.ExpressionEvaluator;
import io.harness.security.encryption.EncryptableSettingWithEncryptionDetails;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.EncryptionConfig;

import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.settings.SettingValue;

import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(DEL)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Singleton
@Slf4j
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public class CapabilityHelper {
  public static List<ExecutionCapability> generateDelegateCapabilities(ExecutionCapabilityDemander capabilityDemander,
      List<EncryptedDataDetail> encryptedDataDetails, ExpressionEvaluator maskingEvaluator) {
    List<ExecutionCapability> executionCapabilities = new ArrayList<>();

    if (capabilityDemander != null) {
      executionCapabilities.addAll(capabilityDemander.fetchRequiredExecutionCapabilities(maskingEvaluator));
    }
    if (isEmpty(encryptedDataDetails)) {
      return executionCapabilities;
    }

    executionCapabilities.addAll(EncryptedDataDetailsCapabilityHelper.fetchExecutionCapabilitiesForEncryptedDataDetails(
        encryptedDataDetails, maskingEvaluator));
    return executionCapabilities;
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
        return EncryptedDataDetailsCapabilityHelper.fetchEncryptionConfigsMapFromEncryptedDataDetails(
            encryptedDataDetails);
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
        return EncryptedDataDetailsCapabilityHelper.fetchEncryptionConfigsMapFromEncryptedDataDetails(
            Collections.singletonList(encryptedDataDetail));
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
      log.warn("Failed while generating Encryption Configs from EncryptionDataDetails: " + e);
    }
    return encryptionConfigsMap;
  }

  private static Map<String, EncryptionConfig> fetchEncryptionConfigsMapFromEncryptableSettings(
      List<EncryptableSettingWithEncryptionDetails> encryptableSettingWithEncryptionDetails) {
    Map<String, EncryptionConfig> encryptionConfigsMap = new HashMap<>();
    encryptableSettingWithEncryptionDetails.forEach(encryptableSettingWithEncryptionDetail
        -> encryptionConfigsMap.putAll(
            EncryptedDataDetailsCapabilityHelper.fetchEncryptionConfigsMapFromEncryptedDataDetails(
                encryptableSettingWithEncryptionDetail.getEncryptedDataDetails())));

    return encryptionConfigsMap;
  }

  public static boolean isTaskParameterType(TaskData taskData) {
    return taskData.getParameters() == null
        || taskData.getParameters().length == 1 && taskData.getParameters()[0] instanceof TaskParameters;
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
      log.warn("Failed in determining if instance of EncryptionDetails");
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
      log.warn("Failed in determining if instance of EncryptableSettingWithEncryptionDetails");
    }

    return false;
  }

  public static String generateLogStringWithCapabilitiesGenerated(
      String taskType, List<ExecutionCapability> executionCapabilities) {
    StringBuilder builder =
        new StringBuilder(128).append("Capabilities Generate for Task: ").append(taskType).append(" are: ");

    executionCapabilities.forEach(capability -> builder.append('\n').append(capability.toString()));
    return builder.toString();
  }

  /**
   * This is Used by BuildSourceParameters
   */
  public static List<ExecutionCapability> generateCapabilities(SettingValue settingValue,
      ArtifactStreamAttributes artifactStreamAttributes, ExpressionEvaluator maskingEvaluator) {
    String artifactStreamType = artifactStreamAttributes.getArtifactStreamType();

    List<ExecutionCapability> executionCapabilities = new ArrayList<>();
    if (artifactStreamType.equals(GCR.name())) {
      String hostName = artifactStreamAttributes.getRegistryHostName();
      executionCapabilities.add(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
          new StringBuilder(128)
              .append("https://")
              .append(hostName)
              .append(hostName.endsWith("/") ? "" : "/")
              .toString(),
          maskingEvaluator));

    } else {
      executionCapabilities.addAll(settingValue.fetchRequiredExecutionCapabilities(maskingEvaluator));
    }

    return executionCapabilities;
  }
}
