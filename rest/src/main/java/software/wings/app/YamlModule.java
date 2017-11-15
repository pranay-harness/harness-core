package software.wings.app;

import static software.wings.beans.InfrastructureMappingType.*;
import static software.wings.beans.OrchestrationWorkflowType.*;
import static software.wings.beans.artifact.ArtifactStreamType.*;
import static software.wings.beans.command.CommandUnitType.*;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;

import software.wings.service.impl.yaml.AppYamlResourceServiceImpl;
import software.wings.service.impl.yaml.YamlArtifactStreamServiceImpl;
import software.wings.service.impl.yaml.YamlDirectoryServiceImpl;
import software.wings.service.impl.yaml.YamlGitServiceImpl;
import software.wings.service.impl.yaml.YamlHistoryServiceImpl;
import software.wings.service.impl.yaml.YamlResourceServiceImpl;
import software.wings.service.impl.yaml.handler.artifactstream.AmazonS3ArtifactStreamYamlHandler;
import software.wings.service.impl.yaml.handler.artifactstream.ArtifactStreamYamlHandler;
import software.wings.service.impl.yaml.handler.artifactstream.ArtifactoryArtifactStreamYamlHandler;
import software.wings.service.impl.yaml.handler.artifactstream.ArtifactoryDockerArtifactStreamYamlHandler;
import software.wings.service.impl.yaml.handler.artifactstream.BambooArtifactStreamYamlHandler;
import software.wings.service.impl.yaml.handler.artifactstream.DockerArtifactStreamYamlHandler;
import software.wings.service.impl.yaml.handler.artifactstream.EcrArtifactStreamYamlHandler;
import software.wings.service.impl.yaml.handler.artifactstream.GcrArtifactStreamYamlHandler;
import software.wings.service.impl.yaml.handler.artifactstream.JenkinsArtifactStreamYamlHandler;
import software.wings.service.impl.yaml.handler.artifactstream.NexusArtifactStreamYamlHandler;
import software.wings.service.impl.yaml.handler.command.AwsLambdaCommandUnitYamlHandler;
import software.wings.service.impl.yaml.handler.command.CodeDeployCommandUnitYamlHandler;
import software.wings.service.impl.yaml.handler.command.CommandUnitYamlHandler;
import software.wings.service.impl.yaml.handler.command.CopyConfigCommandUnitYamlHandler;
import software.wings.service.impl.yaml.handler.command.DockerStartCommandUnitYamlHandler;
import software.wings.service.impl.yaml.handler.command.DockerStopCommandUnitYamlHandler;
import software.wings.service.impl.yaml.handler.command.ExecCommandUnitYamlHandler;
import software.wings.service.impl.yaml.handler.command.KubernetesResizeCommandUnitYamlHandler;
import software.wings.service.impl.yaml.handler.command.PortCheckClearedCommandUnitYamlHandler;
import software.wings.service.impl.yaml.handler.command.PortCheckListeningCommandUnitYamlHandler;
import software.wings.service.impl.yaml.handler.command.ProcessCheckRunningCommandUnitYamlHandler;
import software.wings.service.impl.yaml.handler.command.ProcessCheckStoppedCommandUnitYamlHandler;
import software.wings.service.impl.yaml.handler.command.ResizeCommandUnitYamlHandler;
import software.wings.service.impl.yaml.handler.command.ScpCommandUnitYamlHandler;
import software.wings.service.impl.yaml.handler.command.SetupEnvCommandUnitYamlHandler;
import software.wings.service.impl.yaml.handler.inframapping.AwsInfraMappingYamlHandler;
import software.wings.service.impl.yaml.handler.inframapping.AwsLambdaInfraMappingYamlHandler;
import software.wings.service.impl.yaml.handler.inframapping.CodeDeployInfraMappingYamlHandler;
import software.wings.service.impl.yaml.handler.inframapping.DirectKubernetesInfraMappingYamlHandler;
import software.wings.service.impl.yaml.handler.inframapping.EcsInfraMappingYamlHandler;
import software.wings.service.impl.yaml.handler.inframapping.GcpKubernetesInfraMappingYamlHandler;
import software.wings.service.impl.yaml.handler.inframapping.InfraMappingYamlHandler;
import software.wings.service.impl.yaml.handler.inframapping.PhysicalInfraMappingYamlHandler;
import software.wings.service.impl.yaml.handler.workflow.BasicWorkflowYamlHandler;
import software.wings.service.impl.yaml.handler.workflow.CanaryWorkflowYamlHandler;
import software.wings.service.impl.yaml.handler.workflow.MultiServiceWorkflowYamlHandler;
import software.wings.service.impl.yaml.handler.workflow.WorkflowYamlHandler;
import software.wings.service.impl.yaml.sync.YamlSyncServiceImpl;
import software.wings.service.intfc.yaml.AppYamlResourceService;
import software.wings.service.intfc.yaml.YamlArtifactStreamService;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.service.intfc.yaml.YamlHistoryService;
import software.wings.service.intfc.yaml.YamlResourceService;
import software.wings.service.intfc.yaml.sync.YamlSyncService;

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
    bind(YamlSyncService.class).to(YamlSyncServiceImpl.class);

    MapBinder<String, ArtifactStreamYamlHandler> artifactStreamYamlHelperMapBinder =
        MapBinder.newMapBinder(binder(), String.class, ArtifactStreamYamlHandler.class);
    artifactStreamYamlHelperMapBinder.addBinding(AMAZON_S3.name()).to(AmazonS3ArtifactStreamYamlHandler.class);
    artifactStreamYamlHelperMapBinder.addBinding(ARTIFACTORY.name()).to(ArtifactoryArtifactStreamYamlHandler.class);
    artifactStreamYamlHelperMapBinder.addBinding(ARTIFACTORYDOCKER.name())
        .to(ArtifactoryDockerArtifactStreamYamlHandler.class);
    artifactStreamYamlHelperMapBinder.addBinding(BAMBOO.name()).to(BambooArtifactStreamYamlHandler.class);
    artifactStreamYamlHelperMapBinder.addBinding(DOCKER.name()).to(DockerArtifactStreamYamlHandler.class);
    artifactStreamYamlHelperMapBinder.addBinding(ECR.name()).to(EcrArtifactStreamYamlHandler.class);
    artifactStreamYamlHelperMapBinder.addBinding(GCR.name()).to(GcrArtifactStreamYamlHandler.class);
    artifactStreamYamlHelperMapBinder.addBinding(JENKINS.name()).to(JenkinsArtifactStreamYamlHandler.class);
    artifactStreamYamlHelperMapBinder.addBinding(NEXUS.name()).to(NexusArtifactStreamYamlHandler.class);

    MapBinder<String, InfraMappingYamlHandler> infraMappingYamlHelperMapBinder =
        MapBinder.newMapBinder(binder(), String.class, InfraMappingYamlHandler.class);
    infraMappingYamlHelperMapBinder.addBinding(AWS_SSH.name()).to(AwsInfraMappingYamlHandler.class);
    infraMappingYamlHelperMapBinder.addBinding(AWS_AWS_CODEDEPLOY.name()).to(CodeDeployInfraMappingYamlHandler.class);
    infraMappingYamlHelperMapBinder.addBinding(AWS_AWS_LAMBDA.name()).to(AwsLambdaInfraMappingYamlHandler.class);
    infraMappingYamlHelperMapBinder.addBinding(DIRECT_KUBERNETES.name())
        .to(DirectKubernetesInfraMappingYamlHandler.class);
    infraMappingYamlHelperMapBinder.addBinding(AWS_ECS.name()).to(EcsInfraMappingYamlHandler.class);
    infraMappingYamlHelperMapBinder.addBinding(GCP_KUBERNETES.name()).to(GcpKubernetesInfraMappingYamlHandler.class);
    infraMappingYamlHelperMapBinder.addBinding(PHYSICAL_DATA_CENTER_SSH.name())
        .to(PhysicalInfraMappingYamlHandler.class);

    MapBinder<String, WorkflowYamlHandler> workflowYamlHelperMapBinder =
        MapBinder.newMapBinder(binder(), String.class, WorkflowYamlHandler.class);
    workflowYamlHelperMapBinder.addBinding(BASIC.name()).to(BasicWorkflowYamlHandler.class);
    workflowYamlHelperMapBinder.addBinding(CANARY.name()).to(CanaryWorkflowYamlHandler.class);
    workflowYamlHelperMapBinder.addBinding(MULTI_SERVICE.name()).to(MultiServiceWorkflowYamlHandler.class);

    MapBinder<String, CommandUnitYamlHandler> commandUnitYamlHandlerMapBinder =
        MapBinder.newMapBinder(binder(), String.class, CommandUnitYamlHandler.class);
    commandUnitYamlHandlerMapBinder.addBinding(EXEC.name()).to(ExecCommandUnitYamlHandler.class);
    commandUnitYamlHandlerMapBinder.addBinding(SCP.name()).to(ScpCommandUnitYamlHandler.class);
    commandUnitYamlHandlerMapBinder.addBinding(COPY_CONFIGS.name()).to(CopyConfigCommandUnitYamlHandler.class);
    //    commandUnitYamlHandlerMapBinder.addBinding(COMMAND.name()).to(CommandYamlHandler.class);
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
    commandUnitYamlHandlerMapBinder.addBinding(RESIZE.name()).to(ResizeCommandUnitYamlHandler.class);
    commandUnitYamlHandlerMapBinder.addBinding(CODE_DEPLOY.name()).to(CodeDeployCommandUnitYamlHandler.class);
    commandUnitYamlHandlerMapBinder.addBinding(AWS_LAMBDA.name()).to(AwsLambdaCommandUnitYamlHandler.class);
    commandUnitYamlHandlerMapBinder.addBinding(RESIZE_KUBERNETES.name())
        .to(KubernetesResizeCommandUnitYamlHandler.class);
  }
}
