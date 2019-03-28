package software.wings.delegatetasks.delegatecapability;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.stream.Collectors.toList;

import com.google.inject.Singleton;

import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.beans.executioncapability.HttpConnectionExecutionCapability;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.security.encryption.EncryptionConfig;
import io.harness.security.encryption.EncryptionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.KmsConfig;
import software.wings.beans.VaultConfig;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Singleton
public class CapabilityHelper {
  private static final Logger logger = LoggerFactory.getLogger(CapabilityHelper.class);

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

    executionCapabilities.addAll(generateVaultHttpCapabilities(encryptedDataDetails));
    return executionCapabilities;
  }

  public static List<ExecutionCapability> generateVaultHttpCapabilities(
      List<EncryptedDataDetail> encryptedDataDetails) {
    List<ExecutionCapability> executionCapabilities = new ArrayList<>();

    if (isEmpty(encryptedDataDetails)) {
      return executionCapabilities;
    }

    EncryptedDataDetail encryptedDataDetail = encryptedDataDetails.get(0);
    return Arrays.asList(getHttpCapabilityForDecryption(encryptedDataDetail.getEncryptionConfig()));
  }

  public static HttpConnectionExecutionCapability getHttpCapabilityForDecryption(EncryptionConfig encryptionConfig) {
    if (encryptionConfig instanceof KmsConfig) {
      return HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapabilityForKms(
          ((KmsConfig) encryptionConfig).getRegion());
    } else if (encryptionConfig instanceof VaultConfig) {
      return HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
          ((VaultConfig) encryptionConfig).getVaultUrl());
    }

    return null;
  }

  public static Map<String, EncryptionConfig> fetchEncryptionDetailsListFromParameters(TaskData taskData) {
    Map<String, EncryptionConfig> encryptionConfigsMap = new HashMap<>();
    // TODO: Remove this when jenkins/bambooConfig etc are integrated with new framework
    try {
      Object argument = Arrays.stream(taskData.getParameters())
                            .filter(parameter -> isEncryptionDetailsList(parameter))
                            .findFirst()
                            .orElse(null);

      if (argument != null) {
        List<EncryptedDataDetail> encryptedDataDetails = (List<EncryptedDataDetail>) argument;

        List<EncryptedDataDetail> nonLocalEncryptedDetails =
            encryptedDataDetails.stream()
                .filter(encryptedDataDetail -> encryptedDataDetail.getEncryptionType() != EncryptionType.LOCAL)
                .collect(Collectors.toList());

        // There can be more than 1 non-Local encryptedDataDetails.
        // e.g. in case of JenkinConfig, it has token / username-password. User will select 1
        // of the auth mechanism. In this case, it will have 2 encryptedDataDetails (same entry twice)
        if (isNotEmpty(nonLocalEncryptedDetails)) {
          EncryptedDataDetail encryptedDataDetail = nonLocalEncryptedDetails.get(0);
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
    StringBuilder builder =
        new StringBuilder(128).append("Capabilities Generate for Task: ").append(task.getUuid()).append(" are: ");

    task.getExecutionCapabilities().forEach(capability -> builder.append('\n').append(capability.toString()));
    return builder.toString();
  }
}
