package io.harness.delegate.task.mixin;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.EncryptionConfig;
import io.harness.security.encryption.EncryptionType;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

@UtilityClass
@Slf4j
public class JiraCapabilityGenerator {
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
}
