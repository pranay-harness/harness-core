package software.wings.service.impl;

import static java.util.Collections.emptyList;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.beans.executioncapability.SystemEnvCheckerCapability;
import io.harness.delegate.task.mixin.AwsRegionCapabilityGenerator;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.AwsConfig;
import software.wings.beans.AzureConfig;
import software.wings.beans.GcpConfig;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.SettingAttribute;
import software.wings.delegatetasks.validation.capabilities.ClusterMasterUrlValidationCapability;
import software.wings.settings.SettingValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@Slf4j
public class ContainerServiceParams implements ExecutionCapabilityDemander {
  private SettingAttribute settingAttribute;
  private List<EncryptedDataDetail> encryptionDetails;
  private String containerServiceName;
  private String clusterName;
  private String namespace;
  private String region;
  private String subscriptionId;
  private String resourceGroup;
  private String masterUrl;
  private Set<String> containerServiceNames;
  private boolean cloudCostEnabled;

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
      return emptyList();
    }
    SettingValue value = settingAttribute.getValue();

    List<ExecutionCapability> executionCapabilities = new ArrayList<>();
    if (value instanceof AwsConfig) {
      executionCapabilities.add(AwsRegionCapabilityGenerator.buildAwsRegionCapability(region));
    } else if (value instanceof KubernetesClusterConfig
        && ((KubernetesClusterConfig) value).isUseKubernetesDelegate()) {
      executionCapabilities.add(SystemEnvCheckerCapability.builder()
                                    .systemPropertyName("DELEGATE_NAME")
                                    .comparate(((KubernetesClusterConfig) value).getDelegateName())
                                    .build());
    } else {
      if ("None".equals(clusterName)) {
        executionCapabilities.add(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
            "https://container.googleapis.com/"));
      } else {
        if (masterUrl == null) {
          executionCapabilities.add(
              ClusterMasterUrlValidationCapability.builder().containerServiceParams(this).build());
        } else {
          executionCapabilities.add(
              HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(masterUrl));
        }
      }
    }
    return executionCapabilities;
  }
}
