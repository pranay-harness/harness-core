/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.app;

import static software.wings.beans.InfrastructureMappingType.AWS_AMI;
import static software.wings.beans.command.CommandUnitType.AWS_LAMBDA;
import static software.wings.beans.command.CommandUnitType.CODE_DEPLOY;
import static software.wings.beans.command.CommandUnitType.COMMAND;
import static software.wings.beans.command.CommandUnitType.COPY_CONFIGS;
import static software.wings.beans.command.CommandUnitType.DOCKER_START;
import static software.wings.beans.command.CommandUnitType.DOCKER_STOP;
import static software.wings.beans.command.CommandUnitType.DOWNLOAD_ARTIFACT;
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

import software.wings.common.TemplateConstants;
import software.wings.service.impl.yaml.handler.command.AmiCommandUnitYamlHandler;
import software.wings.service.impl.yaml.handler.command.AwsLambdaCommandUnitYamlHandler;
import software.wings.service.impl.yaml.handler.command.CodeDeployCommandUnitYamlHandler;
import software.wings.service.impl.yaml.handler.command.CommandRefCommandUnitYamlHandler;
import software.wings.service.impl.yaml.handler.command.CommandUnitYamlHandler;
import software.wings.service.impl.yaml.handler.command.CopyConfigCommandUnitYamlHandler;
import software.wings.service.impl.yaml.handler.command.DockerStartCommandUnitYamlHandler;
import software.wings.service.impl.yaml.handler.command.DockerStopCommandUnitYamlHandler;
import software.wings.service.impl.yaml.handler.command.DownloadArtifactCommandUnitYamlHandler;
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
import software.wings.service.impl.yaml.handler.templatelibrary.CommandTemplateRefYamlHandler;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;

public class CommandLibrarySharedModule extends AbstractModule {
  private final boolean refCommandUnitSupported;

  public CommandLibrarySharedModule(boolean refCommandUnitSupported) {
    this.refCommandUnitSupported = refCommandUnitSupported;
  }

  @Override
  protected void configure() {
    MapBinder<String, CommandUnitYamlHandler> commandUnitYamlHandlerMapBinder =
        MapBinder.newMapBinder(binder(), String.class, CommandUnitYamlHandler.class);
    commandUnitYamlHandlerMapBinder.addBinding(EXEC.name()).to(ExecCommandUnitYamlHandler.class);
    commandUnitYamlHandlerMapBinder.addBinding(SCP.name()).to(ScpCommandUnitYamlHandler.class);
    commandUnitYamlHandlerMapBinder.addBinding(COPY_CONFIGS.name()).to(CopyConfigCommandUnitYamlHandler.class);
    if (refCommandUnitSupported) {
      commandUnitYamlHandlerMapBinder.addBinding(COMMAND.name()).to(CommandRefCommandUnitYamlHandler.class);
      commandUnitYamlHandlerMapBinder.addBinding(TemplateConstants.TEMPLATE_REF_COMMAND)
          .to(CommandTemplateRefYamlHandler.class);
    }
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
    commandUnitYamlHandlerMapBinder.addBinding(DOWNLOAD_ARTIFACT.name())
        .to(DownloadArtifactCommandUnitYamlHandler.class);
  }
}
