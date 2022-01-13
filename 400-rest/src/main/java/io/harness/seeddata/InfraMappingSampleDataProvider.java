/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.seeddata;

import static io.harness.seeddata.SampleDataProviderConstants.K8S_SERVICE_INFRA_NAME;
import static io.harness.validation.Validator.notNullCheck;

import static software.wings.beans.DirectKubernetesInfrastructureMapping.Builder.aDirectKubernetesInfrastructureMapping;

import software.wings.api.DeploymentType;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.ServiceTemplate;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.settings.SettingVariableTypes;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class InfraMappingSampleDataProvider {
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private ServiceTemplateService serviceTemplateService;

  public InfrastructureMapping createKubeServiceInfraStructure(
      String accountId, String appId, String envId, String serviceId, String cloudProviderId, String namespace) {
    ServiceTemplate serviceTemplate = serviceTemplateService.get(appId, serviceId, envId);
    notNullCheck("Service template does not exist", serviceTemplate);

    DirectKubernetesInfrastructureMapping directKubeInfra =
        aDirectKubernetesInfrastructureMapping()
            .withInfraMappingType(InfrastructureMappingType.DIRECT_KUBERNETES.name())
            .withAccountId(accountId)
            .withAppId(appId)
            .withServiceId(serviceId)
            .withComputeProviderSettingId(cloudProviderId)
            .withComputeProviderType(SettingVariableTypes.KUBERNETES_CLUSTER.name())
            .withDeploymentType(DeploymentType.KUBERNETES.name())
            .withName(K8S_SERVICE_INFRA_NAME)
            .withNamespace(namespace)
            .withAutoPopulate(false)
            .withServiceTemplateId(serviceTemplate.getUuid())
            .withEnvId(envId)
            .withSample(true)
            .build();
    return infrastructureMappingService.save(directKubeInfra, true, null);
  }
}
