package io.harness.seeddata;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Workflow;
import software.wings.utils.Validator;

@Singleton
public class SeedDataProviderService {
  @Inject private CloudProviderSeedDataProvider cloudProviderSeedDataProvider;
  @Inject private ConnectorSeedDataProvider connectorGenerator;
  @Inject private ApplicationSeedDataProvider applicationSeedDataProvider;
  @Inject private ServiceSeedDataProvider serviceSeedDataProvider;
  @Inject private ArtifactStreamSeedDataProvider artifactStreamSeedDataProvider;
  @Inject private EnvironmentSeedDataProvider environmentSeedDataProvider;
  @Inject private InfraMappingSeedDataProvider infraMappingSeedDataProvider;
  @Inject private WorkflowSeedDataProvider workflowSeedDataProvider;
  @Inject private PipelineSeedDataProvider pipelineSeedDataProvider;

  public Application createKubernetesApp(Account account) {
    // The following steps to achieve end to end seed data generation
    // 1. Create Cloud Provider ->
    SettingAttribute kubernetesClusterConfig =
        cloudProviderSeedDataProvider.createKubernetesClusterConfig(account.getUuid());

    // 2. Create Docker connector
    SettingAttribute dockerConnector = connectorGenerator.createDockerConnector(account.getUuid());

    // 3. Create App
    Application kubernetesApp = applicationSeedDataProvider.createKubernetesApp(account.getUuid());
    Validator.notNullCheck("Kubernetes App not saved", kubernetesApp);

    // 4. Create Service
    Service kubeService = serviceSeedDataProvider.createKubeService(kubernetesApp.getUuid());

    // 5. Create Artifact Stream
    artifactStreamSeedDataProvider.createDockerArtifactStream(
        kubernetesApp.getAppId(), kubeService.getUuid(), dockerConnector.getUuid());
    // 6. Create QA Environment
    Environment qaEnv = environmentSeedDataProvider.createQAEnvironment(kubernetesApp.getUuid());

    // 7. Create QA Service Infrastructure
    InfrastructureMapping qaInfraMapping =
        infraMappingSeedDataProvider.createKubeServiceInfraStructure(account.getUuid(), kubernetesApp.getUuid(),
            qaEnv.getUuid(), kubeService.getUuid(), kubernetesClusterConfig.getUuid());

    // 8. Create Prod Environment
    Environment prodEnv = environmentSeedDataProvider.createProdEnvironment(kubernetesApp.getUuid());

    // 9. Create Prod Service Infrastructure

    InfrastructureMapping prodInfraMapping =
        infraMappingSeedDataProvider.createKubeServiceInfraStructure(account.getUuid(), kubernetesApp.getUuid(),
            prodEnv.getUuid(), kubeService.getUuid(), kubernetesClusterConfig.getUuid());

    // 8. Create Workflow
    Workflow workflow = workflowSeedDataProvider.createKubeWorkflow(
        kubernetesApp.getUuid(), qaEnv.getUuid(), kubeService.getUuid(), qaInfraMapping.getUuid());

    // 9 Templatize Workflow
    workflowSeedDataProvider.templatizeEnvAndServiceInfra(workflow);

    // 9. Create a Pipeline
    pipelineSeedDataProvider.createPipeline(kubernetesApp.getAccountId(), kubernetesApp.getUuid(), workflow,
        qaEnv.getUuid(), qaInfraMapping.getUuid(), prodEnv.getUuid(), prodInfraMapping.getUuid());

    return kubernetesApp;
  }
}
