/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.helpers.ext.container;

import static io.harness.exception.WingsException.USER;

import io.harness.delegate.beans.TaskData;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;

import software.wings.beans.AzureKubernetesInfrastructureMapping;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SyncTaskContext;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.service.impl.MasterUrlFetchTaskParameter;
import software.wings.service.intfc.ContainerService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class ContainerMasterUrlHelper {
  @Inject private DelegateProxyFactory delegateProxyFactory;
  @Inject InfrastructureMappingService infrastructureMappingService;
  @Inject EnvironmentService environmentService;
  @Inject ContainerDeploymentManagerHelper containerDeploymentManagerHelper;

  public boolean fetchMasterUrlAndUpdateInfraMapping(ContainerInfrastructureMapping containerInfraMapping,
      ContainerServiceParams containerServiceParams, SyncTaskContext syncTaskContext, String workflowExecutionId) {
    String masterUrl = fetchMasterUrl(containerServiceParams, syncTaskContext);
    if (containerInfraMapping instanceof GcpKubernetesInfrastructureMapping) {
      ((GcpKubernetesInfrastructureMapping) containerInfraMapping).setMasterUrl(masterUrl);
    } else {
      ((AzureKubernetesInfrastructureMapping) containerInfraMapping).setMasterUrl(masterUrl);
    }
    InfrastructureMapping infrastructureMapping =
        infrastructureMappingService.save(containerInfraMapping, workflowExecutionId);
    return infrastructureMapping != null;
  }

  public String fetchMasterUrl(ContainerServiceParams containerServiceParams, SyncTaskContext syncTaskContext) {
    try {
      return delegateProxyFactory.get(ContainerService.class, syncTaskContext)
          .fetchMasterUrl(MasterUrlFetchTaskParameter.builder().containerServiceParams(containerServiceParams).build());
    } catch (Exception e) {
      log.warn(ExceptionUtils.getMessage(e), e);
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), USER);
    }
  }

  public String fetchMasterUrl(
      ContainerServiceParams containerServiceParams, ContainerInfrastructureMapping infraMapping) {
    if (infraMapping instanceof DirectKubernetesInfrastructureMapping) {
      final SettingAttribute settingAttribute = containerServiceParams.getSettingAttribute();
      if (settingAttribute.getValue() instanceof KubernetesClusterConfig) {
        return ((KubernetesClusterConfig) settingAttribute.getValue()).getMasterUrl();
      }
    }

    Environment environment = environmentService.get(infraMapping.getAppId(), infraMapping.getEnvId());
    return fetchMasterUrl(containerServiceParams,
        SyncTaskContext.builder()
            .accountId(infraMapping.getAccountId())
            .appId(infraMapping.getAppId())
            .envId(infraMapping.getEnvId())
            .envType(environment.getEnvironmentType())
            .infrastructureMappingId(infraMapping.getUuid())
            .serviceId(infraMapping.getServiceId())
            .infraStructureDefinitionId(infraMapping.getInfrastructureDefinitionId())
            .timeout(TaskData.DEFAULT_SYNC_CALL_TIMEOUT)
            .build());
  }

  public boolean masterUrlRequired(ContainerInfrastructureMapping containerInfraMapping) {
    if (containerInfraMapping instanceof GcpKubernetesInfrastructureMapping) {
      return ((GcpKubernetesInfrastructureMapping) containerInfraMapping).getMasterUrl() == null;
    } else if (containerInfraMapping instanceof AzureKubernetesInfrastructureMapping) {
      return ((AzureKubernetesInfrastructureMapping) containerInfraMapping).getMasterUrl() == null;
    } else {
      return false;
    }
  }

  public boolean masterUrlRequiredWithProvisioner(ContainerInfrastructureMapping containerInfraMapping) {
    boolean required = masterUrlRequired(containerInfraMapping);
    if (!required) {
      return false;
    } else {
      return containerInfraMapping.getProvisionerId() == null;
    }
  }
}
