package io.harness.seeddata;

import static io.harness.seeddata.SampleDataProviderConstants.K8S_INFRA_NAME;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.api.CloudProviderType;
import software.wings.api.DeploymentType;
import software.wings.infra.DirectKubernetesInfrastructure;
import software.wings.infra.InfrastructureDefinition;
import software.wings.service.intfc.InfrastructureDefinitionService;

@Singleton
public class InfraDefinitionSampleDataProvider {
  @Inject private InfrastructureDefinitionService infrastructureDefinitionService;

  public InfrastructureDefinition createInfraStructure(
      String appId, String envId, String cloudProviderId, String namespace) {
    InfrastructureDefinition infrastructureDefinition =
        InfrastructureDefinition.builder()
            .appId(appId)
            .cloudProviderType(CloudProviderType.KUBERNETES_CLUSTER)
            .deploymentType(DeploymentType.KUBERNETES)
            .name(K8S_INFRA_NAME)
            .envId(envId)
            .infrastructure(
                DirectKubernetesInfrastructure.builder().cloudProviderId(cloudProviderId).namespace(namespace).build())
            .build();
    return infrastructureDefinitionService.save(infrastructureDefinition, true);
  }
}
