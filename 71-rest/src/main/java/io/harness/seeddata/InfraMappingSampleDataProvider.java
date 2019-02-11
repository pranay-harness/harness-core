package io.harness.seeddata;

import static io.harness.seeddata.SampleDataProviderConstants.K8S_SERVICE_INFRA_NAME;
import static software.wings.beans.DirectKubernetesInfrastructureMapping.Builder.aDirectKubernetesInfrastructureMapping;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.api.DeploymentType;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.ServiceTemplate;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.utils.Validator;

@Singleton
public class InfraMappingSampleDataProvider {
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private ServiceTemplateService serviceTemplateService;

  public InfrastructureMapping createKubeServiceInfraStructure(
      String accountId, String appId, String envId, String serviceId, String cloudProviderId, String namespace) {
    ServiceTemplate serviceTemplate = serviceTemplateService.get(appId, serviceId, envId);
    Validator.notNullCheck("Service template does not exist", serviceTemplate);

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
            .build();
    return infrastructureMappingService.save(directKubeInfra, true);
  }
}
