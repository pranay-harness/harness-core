package io.harness.seeddata;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.utils.Misc;
import software.wings.utils.Validator;

@Singleton
public class SampleDataProviderService {
  private static final Logger logger = LoggerFactory.getLogger(SampleDataProviderService.class);

  @Inject private CloudProviderSampleDataProvider cloudProviderSeedDataProvider;
  @Inject private ConnectorSampleDataProvider connectorGenerator;
  @Inject private ApplicationSampleDataProvider applicationSampleDataProvider;
  @Inject private ServiceSampleDataProvider serviceSampleDataProvider;
  @Inject private ArtifactStreamSampleDataProvider artifactStreamSampleDataProvider;
  @Inject private EnvironmentSampleDataProvider environmentSampleDataProvider;
  @Inject private InfraMappingSampleDataProvider infraMappingSampleDataProvider;
  @Inject private WorkflowSampleDataProvider workflowSampleDataProvider;
  @Inject private PipelineSampleDataProvider pipelineSampleDataProvider;

  public void createHarnessSampleApp(Account account) {
    try {
      // The following steps to achieve end to end seed data generation
      // 1. Create Cloud Provider ->
      SettingAttribute kubernetesClusterConfig =
          cloudProviderSeedDataProvider.createKubernetesClusterConfig(account.getUuid());

      // 2. Create Docker connector
      SettingAttribute dockerConnector = connectorGenerator.createDockerConnector(account.getUuid());

      // 3. Create App
      Application kubernetesApp = applicationSampleDataProvider.createKubernetesApp(account.getUuid());
      Validator.notNullCheck("Kubernetes App not saved", kubernetesApp);

      // 4. Create Service
      Service kubeService = serviceSampleDataProvider.createKubeService(kubernetesApp.getUuid());

      // 5. Create Artifact Stream
      artifactStreamSampleDataProvider.createDockerArtifactStream(
          kubernetesApp.getAppId(), kubeService.getUuid(), dockerConnector.getUuid());
      // 6. Create QA Environment
      Environment qaEnv = environmentSampleDataProvider.createQAEnvironment(kubernetesApp.getUuid());

      // 7. Create QA Service Infrastructure
      InfrastructureMapping qaInfraMapping =
          infraMappingSampleDataProvider.createKubeServiceInfraStructure(account.getUuid(), kubernetesApp.getUuid(),
              qaEnv.getUuid(), kubeService.getUuid(), kubernetesClusterConfig.getUuid());

      // 8. Create Prod Environment
      Environment prodEnv = environmentSampleDataProvider.createProdEnvironment(kubernetesApp.getUuid());

      // 9. Create Prod Service Infrastructure

      InfrastructureMapping prodInfraMapping =
          infraMappingSampleDataProvider.createKubeServiceInfraStructure(account.getUuid(), kubernetesApp.getUuid(),
              prodEnv.getUuid(), kubeService.getUuid(), kubernetesClusterConfig.getUuid());

      // 8. Create Workflow
      String basicWorkflowId = workflowSampleDataProvider.createK8sBasicWorkflow(
          kubernetesApp.getUuid(), qaEnv.getUuid(), kubeService.getUuid(), qaInfraMapping.getUuid());

      // 9 Create Canary Workflow
      String canaryWorkflowId = workflowSampleDataProvider.createK8sCanaryWorkflow(
          kubernetesApp.getUuid(), prodEnv.getUuid(), kubeService.getUuid(), prodInfraMapping.getUuid());

      // 10. Create a Pipeline
      pipelineSampleDataProvider.createPipeline(kubernetesApp.getAccountId(), kubernetesApp.getUuid(), basicWorkflowId,
          qaEnv.getUuid(), canaryWorkflowId, prodEnv.getUuid());

    } catch (Exception ex) {
      logger.error("Failed to create Sample Application for the account [" + account.getUuid()
              + "]. Reason: " + Misc.getMessage(ex),
          ex);
    }
  }
}
