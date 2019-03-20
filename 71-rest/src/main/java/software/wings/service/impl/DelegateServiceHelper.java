package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static java.util.stream.Collectors.toList;

import com.google.inject.Singleton;

import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.beans.executioncapability.HttpConnectionExecutionCapability;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.exception.InvalidRequestException;
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
public class DelegateServiceHelper {
  private static final Logger logger = LoggerFactory.getLogger(DelegateServiceHelper.class);

  public void embedCapabilitiesInDelegateTask(DelegateTask task, Collection<EncryptionConfig> encryptionConfigs) {
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

  public HttpConnectionExecutionCapability getHttpCapabilityForDecryption(EncryptionConfig encryptionConfig) {
    if (encryptionConfig instanceof KmsConfig) {
      return HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapabilityForKms(
          ((KmsConfig) encryptionConfig).getRegion());
    } else if (encryptionConfig instanceof VaultConfig) {
      return HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
          ((VaultConfig) encryptionConfig).getVaultUrl());
    }

    return null;
  }

  public Map<String, EncryptionConfig> fetchEncryptionDetailsListFromParameters(TaskData taskData) {
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

        if (nonLocalEncryptedDetails.size() > 1) {
          throw new InvalidRequestException("More than one encrypted records associated", USER);
        }

        EncryptedDataDetail encryptedDataDetail = nonLocalEncryptedDetails.get(0);

        final String encryptionConfigUuid = encryptedDataDetail.getEncryptionConfig().getUuid();

        encryptionConfigsMap.put(encryptionConfigUuid, encryptedDataDetail.getEncryptionConfig());
      }
    } catch (Exception e) {
      logger.warn("Failed while generating Encryption Configs from EncryptionDataDetails");
    }
    return encryptionConfigsMap;
  }

  public boolean isTaskParameterType(TaskData taskData) {
    return taskData.getParameters().length == 1 && taskData.getParameters()[0] instanceof TaskParameters;
  }

  private boolean isEncryptionDetailsList(Object argument) {
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
}
