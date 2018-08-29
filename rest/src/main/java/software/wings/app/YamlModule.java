package software.wings.app;

import static software.wings.beans.InfrastructureMappingType.AWS_AMI;
import static software.wings.beans.InfrastructureMappingType.AWS_AWS_CODEDEPLOY;
import static software.wings.beans.InfrastructureMappingType.AWS_AWS_LAMBDA;
import static software.wings.beans.InfrastructureMappingType.AWS_ECS;
import static software.wings.beans.InfrastructureMappingType.AWS_SSH;
import static software.wings.beans.InfrastructureMappingType.AZURE_KUBERNETES;
import static software.wings.beans.InfrastructureMappingType.DIRECT_KUBERNETES;
import static software.wings.beans.InfrastructureMappingType.GCP_KUBERNETES;
import static software.wings.beans.InfrastructureMappingType.PCF_PCF;
import static software.wings.beans.InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH;
import static software.wings.beans.InfrastructureMappingType.PHYSICAL_DATA_CENTER_WINRM;
import static software.wings.beans.OrchestrationWorkflowType.BASIC;
import static software.wings.beans.OrchestrationWorkflowType.BLUE_GREEN;
import static software.wings.beans.OrchestrationWorkflowType.BUILD;
import static software.wings.beans.OrchestrationWorkflowType.CANARY;
import static software.wings.beans.OrchestrationWorkflowType.MULTI_SERVICE;
import static software.wings.beans.OrchestrationWorkflowType.ROLLING;
import static software.wings.beans.artifact.ArtifactStreamType.ACR;
import static software.wings.beans.artifact.ArtifactStreamType.AMAZON_S3;
import static software.wings.beans.artifact.ArtifactStreamType.AMI;
import static software.wings.beans.artifact.ArtifactStreamType.ARTIFACTORY;
import static software.wings.beans.artifact.ArtifactStreamType.BAMBOO;
import static software.wings.beans.artifact.ArtifactStreamType.DOCKER;
import static software.wings.beans.artifact.ArtifactStreamType.ECR;
import static software.wings.beans.artifact.ArtifactStreamType.GCR;
import static software.wings.beans.artifact.ArtifactStreamType.GCS;
import static software.wings.beans.artifact.ArtifactStreamType.JENKINS;
import static software.wings.beans.artifact.ArtifactStreamType.NEXUS;
import static software.wings.beans.command.CommandUnitType.AWS_LAMBDA;
import static software.wings.beans.command.CommandUnitType.CODE_DEPLOY;
import static software.wings.beans.command.CommandUnitType.COMMAND;
import static software.wings.beans.command.CommandUnitType.COPY_CONFIGS;
import static software.wings.beans.command.CommandUnitType.DOCKER_START;
import static software.wings.beans.command.CommandUnitType.DOCKER_STOP;
import static software.wings.beans.command.CommandUnitType.ECS_SETUP;
import static software.wings.beans.command.CommandUnitType.EXEC;
import static software.wings.beans.command.CommandUnitType.KUBERNETES_SETUP;
import static software.wings.beans.command.CommandUnitType.PORT_CHECK_CLEARED;
import static software.wings.beans.command.CommandUnitType.PORT_CHECK_LISTENING;
import static software.wings.beans.command.CommandUnitType.PROCESS_CHECK_RUNNING;
import static software.wings.beans.command.CommandUnitType.PROCESS_CHECK_STOPPED;
import static software.wings.beans.command.CommandUnitType.RESIZE;
import static software.wings.beans.command.CommandUnitType.RESIZE_KUBERNETES;
import static software.wings.beans.command.CommandUnitType.SCP;
import static software.wings.beans.command.CommandUnitType.SETUP_ENV;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;

import software.wings.api.DeploymentType;
import software.wings.beans.InfrastructureProvisionerType;
import software.wings.service.impl.yaml.AppYamlResourceServiceImpl;
import software.wings.service.impl.yaml.YamlArtifactStreamServiceImpl;
import software.wings.service.impl.yaml.YamlDirectoryServiceImpl;
import software.wings.service.impl.yaml.YamlGitServiceImpl;
import software.wings.service.impl.yaml.YamlHistoryServiceImpl;
import software.wings.service.impl.yaml.YamlPushServiceImpl;
import software.wings.service.impl.yaml.YamlResourceServiceImpl;
import software.wings.service.impl.yaml.handler.artifactstream.AcrArtifactStreamYamlHandler;
import software.wings.service.impl.yaml.handler.artifactstream.AmazonS3ArtifactStreamYamlHandler;
import software.wings.service.impl.yaml.handler.artifactstream.AmiArtifactStreamYamlHandler;
import software.wings.service.impl.yaml.handler.artifactstream.ArtifactStreamYamlHandler;
import software.wings.service.impl.yaml.handler.artifactstream.ArtifactoryArtifactStreamYamlHandler;
import software.wings.service.impl.yaml.handler.artifactstream.BambooArtifactStreamYamlHandler;
import software.wings.service.impl.yaml.handler.artifactstream.DockerArtifactStreamYamlHandler;
import software.wings.service.impl.yaml.handler.artifactstream.EcrArtifactStreamYamlHandler;
import software.wings.service.impl.yaml.handler.artifactstream.GcrArtifactStreamYamlHandler;
import software.wings.service.impl.yaml.handler.artifactstream.GcsArtifactStreamYamlHandler;
import software.wings.service.impl.yaml.handler.artifactstream.JenkinsArtifactStreamYamlHandler;
import software.wings.service.impl.yaml.handler.artifactstream.NexusArtifactStreamYamlHandler;
import software.wings.service.impl.yaml.handler.command.AmiCommandUnitYamlHandler;
import software.wings.service.impl.yaml.handler.command.AwsLambdaCommandUnitYamlHandler;
import software.wings.service.impl.yaml.handler.command.CodeDeployCommandUnitYamlHandler;
import software.wings.service.impl.yaml.handler.command.CommandRefCommandUnitYamlHandler;
import software.wings.service.impl.yaml.handler.command.CommandUnitYamlHandler;
import software.wings.service.impl.yaml.handler.command.CopyConfigCommandUnitYamlHandler;
import software.wings.service.impl.yaml.handler.command.DockerStartCommandUnitYamlHandler;
import software.wings.service.impl.yaml.handler.command.DockerStopCommandUnitYamlHandler;
import software.wings.service.impl.yaml.handler.command.EcsSetupCommandUnitYamlHandler;
import software.wings.service.impl.yaml.handler.command.ExecCommandUnitYamlHandler;
import software.wings.service.impl.yaml.handler.command.KubernetesResizeCommandUnitYamlHandler;
import software.wings.service.impl.yaml.handler.command.KubernetesSetupCommandUnitYamlHandler;
import software.wings.service.impl.yaml.handler.command.PortCheckClearedCommandUnitYamlHandler;
import software.wings.service.impl.yaml.handler.command.PortCheckListeningCommandUnitYamlHandler;
import software.wings.service.impl.yaml.handler.command.ProcessCheckRunningCommandUnitYamlHandler;
import software.wings.service.impl.yaml.handler.command.ProcessCheckStoppedCommandUnitYamlHandler;
import software.wings.service.impl.yaml.handler.command.ResizeCommandUnitYamlHandler;
import software.wings.service.impl.yaml.handler.command.ScpCommandUnitYamlHandler;
import software.wings.service.impl.yaml.handler.command.SetupEnvCommandUnitYamlHandler;
import software.wings.service.impl.yaml.handler.deploymentspec.DeploymentSpecificationYamlHandler;
import software.wings.service.impl.yaml.handler.deploymentspec.container.EcsContainerTaskYamlHandler;
import software.wings.service.impl.yaml.handler.deploymentspec.container.HelmChartSpecificationYamlHandler;
import software.wings.service.impl.yaml.handler.deploymentspec.container.KubernetesContainerTaskYamlHandler;
import software.wings.service.impl.yaml.handler.deploymentspec.container.PcfServiceSpecificationYamlHandler;
import software.wings.service.impl.yaml.handler.deploymentspec.lambda.LambdaSpecificationYamlHandler;
import software.wings.service.impl.yaml.handler.deploymentspec.userdata.UserDataSpecificationYamlHandler;
import software.wings.service.impl.yaml.handler.inframapping.AwsAmiInfraMappingYamlHandler;
import software.wings.service.impl.yaml.handler.inframapping.AwsInfraMappingYamlHandler;
import software.wings.service.impl.yaml.handler.inframapping.AwsLambdaInfraMappingYamlHandler;
import software.wings.service.impl.yaml.handler.inframapping.AzureKubernetesInfraMappingYamlHandler;
import software.wings.service.impl.yaml.handler.inframapping.CodeDeployInfraMappingYamlHandler;
import software.wings.service.impl.yaml.handler.inframapping.DirectKubernetesInfraMappingYamlHandler;
import software.wings.service.impl.yaml.handler.inframapping.EcsInfraMappingYamlHandler;
import software.wings.service.impl.yaml.handler.inframapping.GcpKubernetesInfraMappingYamlHandler;
import software.wings.service.impl.yaml.handler.inframapping.InfraMappingYamlHandler;
import software.wings.service.impl.yaml.handler.inframapping.PcfInfraMappingYamlHandler;
import software.wings.service.impl.yaml.handler.inframapping.PhysicalInfraMappingWinRmYamlHandler;
import software.wings.service.impl.yaml.handler.inframapping.PhysicalInfraMappingYamlHandler;
import software.wings.service.impl.yaml.handler.infraprovisioner.CloudFormationInfrastructureProvisionerYamlHandler;
import software.wings.service.impl.yaml.handler.infraprovisioner.InfrastructureProvisionerYamlHandler;
import software.wings.service.impl.yaml.handler.infraprovisioner.TerraformInfrastructureProvisionerYamlHandler;
import software.wings.service.impl.yaml.handler.setting.artifactserver.ArtifactServerYamlHandler;
import software.wings.service.impl.yaml.handler.setting.artifactserver.ArtifactoryConfigYamlHandler;
import software.wings.service.impl.yaml.handler.setting.artifactserver.BambooConfigYamlHandler;
import software.wings.service.impl.yaml.handler.setting.artifactserver.DockerRegistryConfigYamlHandler;
import software.wings.service.impl.yaml.handler.setting.artifactserver.GitConfigYamlHandler;
import software.wings.service.impl.yaml.handler.setting.artifactserver.JenkinsConfigYamlHandler;
import software.wings.service.impl.yaml.handler.setting.artifactserver.NexusConfigYamlHandler;
import software.wings.service.impl.yaml.handler.setting.cloudprovider.AwsConfigYamlHandler;
import software.wings.service.impl.yaml.handler.setting.cloudprovider.AzureConfigYamlHandler;
import software.wings.service.impl.yaml.handler.setting.cloudprovider.CloudProviderYamlHandler;
import software.wings.service.impl.yaml.handler.setting.cloudprovider.GcpConfigYamlHandler;
import software.wings.service.impl.yaml.handler.setting.cloudprovider.KubernetesClusterConfigYamlHandler;
import software.wings.service.impl.yaml.handler.setting.cloudprovider.PcfConfigYamlHandler;
import software.wings.service.impl.yaml.handler.setting.cloudprovider.PhysicalDataCenterConfigYamlHandler;
import software.wings.service.impl.yaml.handler.setting.collaborationprovider.CollaborationProviderYamlHandler;
import software.wings.service.impl.yaml.handler.setting.collaborationprovider.SlackConfigYamlHandler;
import software.wings.service.impl.yaml.handler.setting.collaborationprovider.SmtpConfigYamlHandler;
import software.wings.service.impl.yaml.handler.setting.verificationprovider.AppDynamicsConfigYamlHandler;
import software.wings.service.impl.yaml.handler.setting.verificationprovider.DynaTraceConfigYamlHandler;
import software.wings.service.impl.yaml.handler.setting.verificationprovider.ElkConfigYamlHandler;
import software.wings.service.impl.yaml.handler.setting.verificationprovider.JenkinsConfigVerificationYamlHandler;
import software.wings.service.impl.yaml.handler.setting.verificationprovider.LogzConfigYamlHandler;
import software.wings.service.impl.yaml.handler.setting.verificationprovider.NewRelicConfigYamlHandler;
import software.wings.service.impl.yaml.handler.setting.verificationprovider.PrometheusConfigYamlHandler;
import software.wings.service.impl.yaml.handler.setting.verificationprovider.SplunkConfigYamlHandler;
import software.wings.service.impl.yaml.handler.setting.verificationprovider.SumoConfigYamlHandler;
import software.wings.service.impl.yaml.handler.setting.verificationprovider.VerificationProviderYamlHandler;
import software.wings.service.impl.yaml.handler.workflow.BasicWorkflowYamlHandler;
import software.wings.service.impl.yaml.handler.workflow.BlueGreenWorkflowYamlHandler;
import software.wings.service.impl.yaml.handler.workflow.BuildWorkflowYamlHandler;
import software.wings.service.impl.yaml.handler.workflow.CanaryWorkflowYamlHandler;
import software.wings.service.impl.yaml.handler.workflow.MultiServiceWorkflowYamlHandler;
import software.wings.service.impl.yaml.handler.workflow.RollingWorkflowYamlHandler;
import software.wings.service.impl.yaml.handler.workflow.WorkflowYamlHandler;
import software.wings.service.impl.yaml.service.YamlServiceImpl;
import software.wings.service.intfc.yaml.AppYamlResourceService;
import software.wings.service.intfc.yaml.YamlArtifactStreamService;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.service.intfc.yaml.YamlHistoryService;
import software.wings.service.intfc.yaml.YamlPushService;
import software.wings.service.intfc.yaml.YamlResourceService;
import software.wings.service.intfc.yaml.sync.YamlService;
import software.wings.settings.SettingValue.SettingVariableTypes;

/**
 * Guice Module for initializing all yaml classes.
 *
 * @author rktummala on 10/17/17
 */
public class YamlModule extends AbstractModule {
  /* (non-Javadoc)
   * @see com.google.inject.AbstractModule#configure()
   */
  @Override
  protected void configure() {
    bind(YamlHistoryService.class).to(YamlHistoryServiceImpl.class);
    bind(YamlDirectoryService.class).to(YamlDirectoryServiceImpl.class);
    bind(YamlResourceService.class).to(YamlResourceServiceImpl.class);
    bind(AppYamlResourceService.class).to(AppYamlResourceServiceImpl.class);
    bind(YamlGitService.class).to(YamlGitServiceImpl.class);
    bind(YamlArtifactStreamService.class).to(YamlArtifactStreamServiceImpl.class);
    bind(YamlService.class).to(YamlServiceImpl.class);
    bind(YamlPushService.class).to(YamlPushServiceImpl.class);

    MapBinder<String, ArtifactStreamYamlHandler> artifactStreamYamlHelperMapBinder =
        MapBinder.newMapBinder(binder(), String.class, ArtifactStreamYamlHandler.class);
    artifactStreamYamlHelperMapBinder.addBinding(AMAZON_S3.name()).to(AmazonS3ArtifactStreamYamlHandler.class);
    artifactStreamYamlHelperMapBinder.addBinding(AMI.name()).to(AmiArtifactStreamYamlHandler.class);
    artifactStreamYamlHelperMapBinder.addBinding(ARTIFACTORY.name()).to(ArtifactoryArtifactStreamYamlHandler.class);
    artifactStreamYamlHelperMapBinder.addBinding(BAMBOO.name()).to(BambooArtifactStreamYamlHandler.class);
    artifactStreamYamlHelperMapBinder.addBinding(DOCKER.name()).to(DockerArtifactStreamYamlHandler.class);
    artifactStreamYamlHelperMapBinder.addBinding(ECR.name()).to(EcrArtifactStreamYamlHandler.class);
    artifactStreamYamlHelperMapBinder.addBinding(GCR.name()).to(GcrArtifactStreamYamlHandler.class);
    artifactStreamYamlHelperMapBinder.addBinding(ACR.name()).to(AcrArtifactStreamYamlHandler.class);
    artifactStreamYamlHelperMapBinder.addBinding(JENKINS.name()).to(JenkinsArtifactStreamYamlHandler.class);
    artifactStreamYamlHelperMapBinder.addBinding(NEXUS.name()).to(NexusArtifactStreamYamlHandler.class);
    artifactStreamYamlHelperMapBinder.addBinding(GCS.name()).to(GcsArtifactStreamYamlHandler.class);

    MapBinder<String, InfraMappingYamlHandler> infraMappingYamlHelperMapBinder =
        MapBinder.newMapBinder(binder(), String.class, InfraMappingYamlHandler.class);
    infraMappingYamlHelperMapBinder.addBinding(AWS_SSH.name()).to(AwsInfraMappingYamlHandler.class);
    infraMappingYamlHelperMapBinder.addBinding(AWS_AWS_CODEDEPLOY.name()).to(CodeDeployInfraMappingYamlHandler.class);
    infraMappingYamlHelperMapBinder.addBinding(AWS_AWS_LAMBDA.name()).to(AwsLambdaInfraMappingYamlHandler.class);
    infraMappingYamlHelperMapBinder.addBinding(AWS_AMI.name()).to(AwsAmiInfraMappingYamlHandler.class);
    infraMappingYamlHelperMapBinder.addBinding(DIRECT_KUBERNETES.name())
        .to(DirectKubernetesInfraMappingYamlHandler.class);
    infraMappingYamlHelperMapBinder.addBinding(AWS_ECS.name()).to(EcsInfraMappingYamlHandler.class);
    infraMappingYamlHelperMapBinder.addBinding(GCP_KUBERNETES.name()).to(GcpKubernetesInfraMappingYamlHandler.class);
    infraMappingYamlHelperMapBinder.addBinding(AZURE_KUBERNETES.name())
        .to(AzureKubernetesInfraMappingYamlHandler.class);
    infraMappingYamlHelperMapBinder.addBinding(PHYSICAL_DATA_CENTER_SSH.name())
        .to(PhysicalInfraMappingYamlHandler.class);
    infraMappingYamlHelperMapBinder.addBinding(PHYSICAL_DATA_CENTER_WINRM.name())
        .to(PhysicalInfraMappingWinRmYamlHandler.class);
    infraMappingYamlHelperMapBinder.addBinding(PCF_PCF.name()).to(PcfInfraMappingYamlHandler.class);

    MapBinder<String, DeploymentSpecificationYamlHandler> deploymentSpecYamlHelperMapBinder =
        MapBinder.newMapBinder(binder(), String.class, DeploymentSpecificationYamlHandler.class);
    deploymentSpecYamlHelperMapBinder.addBinding(DeploymentType.ECS.name()).to(EcsContainerTaskYamlHandler.class);
    deploymentSpecYamlHelperMapBinder.addBinding(DeploymentType.KUBERNETES.name())
        .to(KubernetesContainerTaskYamlHandler.class);
    deploymentSpecYamlHelperMapBinder.addBinding(DeploymentType.HELM.name())
        .to(HelmChartSpecificationYamlHandler.class);
    deploymentSpecYamlHelperMapBinder.addBinding(DeploymentType.PCF.name())
        .to(PcfServiceSpecificationYamlHandler.class);
    deploymentSpecYamlHelperMapBinder.addBinding(DeploymentType.AWS_LAMBDA.name())
        .to(LambdaSpecificationYamlHandler.class);
    deploymentSpecYamlHelperMapBinder.addBinding(DeploymentType.AMI.name()).to(UserDataSpecificationYamlHandler.class);

    MapBinder<String, ArtifactServerYamlHandler> artifactServerYamlHelperMapBinder =
        MapBinder.newMapBinder(binder(), String.class, ArtifactServerYamlHandler.class);
    artifactServerYamlHelperMapBinder.addBinding(SettingVariableTypes.ARTIFACTORY.name())
        .to(ArtifactoryConfigYamlHandler.class);
    artifactServerYamlHelperMapBinder.addBinding(SettingVariableTypes.BAMBOO.name()).to(BambooConfigYamlHandler.class);
    artifactServerYamlHelperMapBinder.addBinding(SettingVariableTypes.DOCKER.name())
        .to(DockerRegistryConfigYamlHandler.class);
    artifactServerYamlHelperMapBinder.addBinding(SettingVariableTypes.GIT.name()).to(GitConfigYamlHandler.class);
    artifactServerYamlHelperMapBinder.addBinding(SettingVariableTypes.JENKINS.name())
        .to(JenkinsConfigYamlHandler.class);
    artifactServerYamlHelperMapBinder.addBinding(SettingVariableTypes.NEXUS.name()).to(NexusConfigYamlHandler.class);

    MapBinder<String, VerificationProviderYamlHandler> verificationProviderYamlHelperMapBinder =
        MapBinder.newMapBinder(binder(), String.class, VerificationProviderYamlHandler.class);
    verificationProviderYamlHelperMapBinder.addBinding(SettingVariableTypes.JENKINS.name())
        .to(JenkinsConfigVerificationYamlHandler.class);
    verificationProviderYamlHelperMapBinder.addBinding(SettingVariableTypes.APP_DYNAMICS.name())
        .to(AppDynamicsConfigYamlHandler.class);
    verificationProviderYamlHelperMapBinder.addBinding(SettingVariableTypes.ELK.name()).to(ElkConfigYamlHandler.class);
    verificationProviderYamlHelperMapBinder.addBinding(SettingVariableTypes.LOGZ.name())
        .to(LogzConfigYamlHandler.class);
    verificationProviderYamlHelperMapBinder.addBinding(SettingVariableTypes.NEW_RELIC.name())
        .to(NewRelicConfigYamlHandler.class);
    verificationProviderYamlHelperMapBinder.addBinding(SettingVariableTypes.DYNA_TRACE.name())
        .to(DynaTraceConfigYamlHandler.class);
    verificationProviderYamlHelperMapBinder.addBinding(SettingVariableTypes.PROMETHEUS.name())
        .to(PrometheusConfigYamlHandler.class);
    verificationProviderYamlHelperMapBinder.addBinding(SettingVariableTypes.SPLUNK.name())
        .to(SplunkConfigYamlHandler.class);
    verificationProviderYamlHelperMapBinder.addBinding(SettingVariableTypes.SUMO.name())
        .to(SumoConfigYamlHandler.class);

    MapBinder<String, CollaborationProviderYamlHandler> collaborationProviderYamlHelperMapBinder =
        MapBinder.newMapBinder(binder(), String.class, CollaborationProviderYamlHandler.class);
    collaborationProviderYamlHelperMapBinder.addBinding(SettingVariableTypes.SMTP.name())
        .to(SmtpConfigYamlHandler.class);
    collaborationProviderYamlHelperMapBinder.addBinding(SettingVariableTypes.SLACK.name())
        .to(SlackConfigYamlHandler.class);

    MapBinder<String, CloudProviderYamlHandler> cloudProviderYamlHelperMapBinder =
        MapBinder.newMapBinder(binder(), String.class, CloudProviderYamlHandler.class);
    cloudProviderYamlHelperMapBinder.addBinding(SettingVariableTypes.AWS.name()).to(AwsConfigYamlHandler.class);
    cloudProviderYamlHelperMapBinder.addBinding(SettingVariableTypes.GCP.name()).to(GcpConfigYamlHandler.class);
    cloudProviderYamlHelperMapBinder.addBinding(SettingVariableTypes.AZURE.name()).to(AzureConfigYamlHandler.class);
    cloudProviderYamlHelperMapBinder.addBinding(SettingVariableTypes.KUBERNETES_CLUSTER.name())
        .to(KubernetesClusterConfigYamlHandler.class);
    cloudProviderYamlHelperMapBinder.addBinding(SettingVariableTypes.PHYSICAL_DATA_CENTER.name())
        .to(PhysicalDataCenterConfigYamlHandler.class);
    cloudProviderYamlHelperMapBinder.addBinding(SettingVariableTypes.PCF.name()).to(PcfConfigYamlHandler.class);

    MapBinder<String, WorkflowYamlHandler> workflowYamlHelperMapBinder =
        MapBinder.newMapBinder(binder(), String.class, WorkflowYamlHandler.class);
    workflowYamlHelperMapBinder.addBinding(BASIC.name()).to(BasicWorkflowYamlHandler.class);
    workflowYamlHelperMapBinder.addBinding(ROLLING.name()).to(RollingWorkflowYamlHandler.class);
    workflowYamlHelperMapBinder.addBinding(BLUE_GREEN.name()).to(BlueGreenWorkflowYamlHandler.class);
    workflowYamlHelperMapBinder.addBinding(BUILD.name()).to(BuildWorkflowYamlHandler.class);
    workflowYamlHelperMapBinder.addBinding(CANARY.name()).to(CanaryWorkflowYamlHandler.class);
    workflowYamlHelperMapBinder.addBinding(MULTI_SERVICE.name()).to(MultiServiceWorkflowYamlHandler.class);

    MapBinder<String, CommandUnitYamlHandler> commandUnitYamlHandlerMapBinder =
        MapBinder.newMapBinder(binder(), String.class, CommandUnitYamlHandler.class);
    commandUnitYamlHandlerMapBinder.addBinding(EXEC.name()).to(ExecCommandUnitYamlHandler.class);
    commandUnitYamlHandlerMapBinder.addBinding(SCP.name()).to(ScpCommandUnitYamlHandler.class);
    commandUnitYamlHandlerMapBinder.addBinding(COPY_CONFIGS.name()).to(CopyConfigCommandUnitYamlHandler.class);
    commandUnitYamlHandlerMapBinder.addBinding(COMMAND.name()).to(CommandRefCommandUnitYamlHandler.class);
    commandUnitYamlHandlerMapBinder.addBinding(SETUP_ENV.name()).to(SetupEnvCommandUnitYamlHandler.class);
    commandUnitYamlHandlerMapBinder.addBinding(DOCKER_START.name()).to(DockerStartCommandUnitYamlHandler.class);
    commandUnitYamlHandlerMapBinder.addBinding(DOCKER_STOP.name()).to(DockerStopCommandUnitYamlHandler.class);
    commandUnitYamlHandlerMapBinder.addBinding(PROCESS_CHECK_RUNNING.name())
        .to(ProcessCheckRunningCommandUnitYamlHandler.class);
    commandUnitYamlHandlerMapBinder.addBinding(PROCESS_CHECK_STOPPED.name())
        .to(ProcessCheckStoppedCommandUnitYamlHandler.class);
    commandUnitYamlHandlerMapBinder.addBinding(PORT_CHECK_CLEARED.name())
        .to(PortCheckClearedCommandUnitYamlHandler.class);
    commandUnitYamlHandlerMapBinder.addBinding(PORT_CHECK_LISTENING.name())
        .to(PortCheckListeningCommandUnitYamlHandler.class);
    commandUnitYamlHandlerMapBinder.addBinding(CODE_DEPLOY.name()).to(CodeDeployCommandUnitYamlHandler.class);
    commandUnitYamlHandlerMapBinder.addBinding(AWS_LAMBDA.name()).to(AwsLambdaCommandUnitYamlHandler.class);
    commandUnitYamlHandlerMapBinder.addBinding(AWS_AMI.name()).to(AmiCommandUnitYamlHandler.class);
    commandUnitYamlHandlerMapBinder.addBinding(RESIZE.name()).to(ResizeCommandUnitYamlHandler.class);
    commandUnitYamlHandlerMapBinder.addBinding(RESIZE_KUBERNETES.name())
        .to(KubernetesResizeCommandUnitYamlHandler.class);
    commandUnitYamlHandlerMapBinder.addBinding(ECS_SETUP.name()).to(EcsSetupCommandUnitYamlHandler.class);
    commandUnitYamlHandlerMapBinder.addBinding(KUBERNETES_SETUP.name()).to(KubernetesSetupCommandUnitYamlHandler.class);

    MapBinder<String, InfrastructureProvisionerYamlHandler> infrastructureProvisionerYamlHandlerMapBinder =
        MapBinder.newMapBinder(binder(), String.class, InfrastructureProvisionerYamlHandler.class);
    infrastructureProvisionerYamlHandlerMapBinder.addBinding(InfrastructureProvisionerType.TERRAFORM.name())
        .to(TerraformInfrastructureProvisionerYamlHandler.class);
    infrastructureProvisionerYamlHandlerMapBinder.addBinding(InfrastructureProvisionerType.CLOUD_FORMATION.name())
        .to(CloudFormationInfrastructureProvisionerYamlHandler.class);
  }
}
