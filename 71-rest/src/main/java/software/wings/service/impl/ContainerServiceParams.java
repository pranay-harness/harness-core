package software.wings.service.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.mixin.AwsRegionCapabilityGenerator;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import lombok.Builder;
import lombok.Data;
import software.wings.beans.AwsConfig;
import software.wings.beans.AzureConfig;
import software.wings.beans.GcpConfig;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.SettingAttribute;
import software.wings.delegatetasks.delegatecapability.CapabilityHelper;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.settings.SettingValue;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
public class ContainerServiceParams implements ExecutionCapabilityDemander {
  private SettingAttribute settingAttribute;
  private List<EncryptedDataDetail> encryptionDetails;
  private String containerServiceName;
  private String clusterName;
  private String namespace;
  private String region;
  private String subscriptionId;
  private String resourceGroup;
  private Set<String> containerServiceNames;

  public boolean isKubernetesClusterConfig() {
    if (settingAttribute == null) {
      return false;
    }

    SettingValue value = settingAttribute.getValue();

    return value instanceof AzureConfig || value instanceof GcpConfig || value instanceof KubernetesConfig
        || value instanceof KubernetesClusterConfig;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    if (settingAttribute == null) {
      return CapabilityHelper.generateKmsHttpCapabilities(encryptionDetails);
    }
    SettingValue value = settingAttribute.getValue();

    if (value instanceof AwsConfig) {
      return Collections.singletonList(AwsRegionCapabilityGenerator.buildAwsRegionCapability(region));
    } else if (value instanceof KubernetesClusterConfig) {
      return CapabilityHelper.generateDelegateCapabilities(value, encryptionDetails);
    } else if ("None".equals(clusterName)) {
      return Collections.singletonList(
          HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
              "https://container.googleapis.com/"));
    } else {
      return CapabilityHelper.generateDelegateCapabilities(value, encryptionDetails);
    }
  }
}
