package io.harness.seeddata;

import static io.harness.seeddata.SampleDataProviderConstants.HARNESS_SAMPLE_APP;
import static io.harness.seeddata.SampleDataProviderConstants.K8S_SERVICE_INFRA_DEFAULT_NAMESPACE;
import static io.harness.seeddata.SampleDataProviderConstants.K8S_SERVICE_INFRA_PROD_NAMESPACE;
import static io.harness.seeddata.SampleDataProviderConstants.K8S_SERVICE_INFRA_QA_NAMESPACE;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
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
      // Create Cloud Provider ->
      SettingAttribute kubernetesClusterConfig =
          cloudProviderSeedDataProvider.createKubernetesClusterConfig(account.getUuid());

      // Create Docker connector
      SettingAttribute dockerConnector = connectorGenerator.createDockerConnector(account.getUuid());

      createK8sSampleApp(account, kubernetesClusterConfig, dockerConnector);
    } catch (Exception ex) {
      logger.error("Failed to create Sample Application for the account [" + account.getUuid()
              + "]. Reason: " + ExceptionUtils.getMessage(ex),
          ex);
    }
  }

  private void createK8sSampleApp(
      Account account, SettingAttribute kubernetesClusterConfig, SettingAttribute dockerConnector) {
    // Create App
    Application kubernetesApp = applicationSampleDataProvider.createKubernetesApp(account.getUuid());
    Validator.notNullCheck("Kubernetes App not saved", kubernetesApp);

    // Create Service
    Service kubeService = serviceSampleDataProvider.createKubeService(kubernetesApp.getUuid());

    // Create Artifact Stream
    artifactStreamSampleDataProvider.createDockerArtifactStream(
        kubernetesApp.getAppId(), kubeService.getUuid(), dockerConnector.getUuid());
    // Create QA Environment
    Environment qaEnv = environmentSampleDataProvider.createQAEnvironment(kubernetesApp.getUuid());

    // Create QA Service Infrastructure
    InfrastructureMapping qaInfraMapping = infraMappingSampleDataProvider.createKubeServiceInfraStructure(
        account.getUuid(), kubernetesApp.getUuid(), qaEnv.getUuid(), kubeService.getUuid(),
        kubernetesClusterConfig.getUuid(), K8S_SERVICE_INFRA_DEFAULT_NAMESPACE);

    // Create Prod Environment
    Environment prodEnv = environmentSampleDataProvider.createProdEnvironment(kubernetesApp.getUuid());

    // Create Prod Service Infrastructure
    InfrastructureMapping prodInfraMapping = infraMappingSampleDataProvider.createKubeServiceInfraStructure(
        account.getUuid(), kubernetesApp.getUuid(), prodEnv.getUuid(), kubeService.getUuid(),
        kubernetesClusterConfig.getUuid(), K8S_SERVICE_INFRA_DEFAULT_NAMESPACE);

    // Create Workflow
    String basicWorkflowId = workflowSampleDataProvider.createK8sBasicWorkflow(
        kubernetesApp.getUuid(), qaEnv.getUuid(), kubeService.getUuid(), qaInfraMapping.getUuid());

    // Create Canary Workflow
    String canaryWorkflowId = workflowSampleDataProvider.createK8sCanaryWorkflow(
        kubernetesApp.getUuid(), prodEnv.getUuid(), kubeService.getUuid(), prodInfraMapping.getUuid());

    // Create a Pipeline
    pipelineSampleDataProvider.createPipeline(kubernetesApp.getAccountId(), kubernetesApp.getUuid(), basicWorkflowId,
        qaEnv.getUuid(), canaryWorkflowId, prodEnv.getUuid());
  }

  public void createK8sV2SampleApp(Account account) {
    try {
      // The following steps to achieve end to end seed data generation
      // Create Cloud Provider ->
      SettingAttribute kubernetesClusterConfig =
          cloudProviderSeedDataProvider.createKubernetesClusterConfig(account.getUuid());

      // Create Docker connector
      SettingAttribute dockerConnector = connectorGenerator.createDockerConnector(account.getUuid());

      createK8sV2SampleApp(account, kubernetesClusterConfig, dockerConnector, HARNESS_SAMPLE_APP);

    } catch (Exception ex) {
      throw new WingsException(ErrorCode.GENERAL_ERROR, WingsException.USER)
          .addParam("Failed to create Sample Application for the account [" + account.getUuid()
                  + "]. Reason: " + ExceptionUtils.getMessage(ex),
              ex);
    }
  }

  private void createK8sV2SampleApp(
      Account account, SettingAttribute kubernetesClusterConfig, SettingAttribute dockerConnector, String appName) {
    Application kubernetesApp = applicationSampleDataProvider.createApp(account.getUuid(), appName, appName);
    Validator.notNullCheck("Kubernetes App not saved", kubernetesApp);

    Service kubeService = serviceSampleDataProvider.createK8sV2Service(kubernetesApp.getUuid());

    artifactStreamSampleDataProvider.createDockerArtifactStream(
        kubernetesApp.getAppId(), kubeService.getUuid(), dockerConnector.getUuid());

    Environment qaEnv = environmentSampleDataProvider.createQAEnvironment(kubernetesApp.getUuid());

    InfrastructureMapping qaInfraMapping =
        infraMappingSampleDataProvider.createKubeServiceInfraStructure(account.getUuid(), kubernetesApp.getUuid(),
            qaEnv.getUuid(), kubeService.getUuid(), kubernetesClusterConfig.getUuid(), K8S_SERVICE_INFRA_QA_NAMESPACE);

    Environment prodEnv = environmentSampleDataProvider.createProdEnvironment(kubernetesApp.getUuid());

    InfrastructureMapping prodInfraMapping = infraMappingSampleDataProvider.createKubeServiceInfraStructure(
        account.getUuid(), kubernetesApp.getUuid(), prodEnv.getUuid(), kubeService.getUuid(),
        kubernetesClusterConfig.getUuid(), K8S_SERVICE_INFRA_PROD_NAMESPACE);

    String basicWorkflowId = workflowSampleDataProvider.createK8sV2RollingWorkflow(
        kubernetesApp.getUuid(), qaEnv.getUuid(), kubeService.getUuid(), qaInfraMapping.getUuid());

    String canaryWorkflowId = workflowSampleDataProvider.createK8sV2CanaryWorkflow(
        kubernetesApp.getUuid(), prodEnv.getUuid(), kubeService.getUuid(), prodInfraMapping.getUuid());

    pipelineSampleDataProvider.createPipeline(kubernetesApp.getAccountId(), kubernetesApp.getUuid(), basicWorkflowId,
        qaEnv.getUuid(), canaryWorkflowId, prodEnv.getUuid());
  }
}
