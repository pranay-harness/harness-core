/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.utils;

import static io.harness.annotations.dev.HarnessModule._930_DELEGATE_TASKS;
import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.shell.ScriptType.POWERSHELL;

import static software.wings.beans.Graph.Builder.aGraph;
import static software.wings.beans.Graph.graphIdGenerator;
import static software.wings.beans.command.Command.Builder.aCommand;
import static software.wings.beans.command.CommandUnitType.AWS_AMI;
import static software.wings.beans.command.CommandUnitType.AZURE_VMSS_DUMMY;
import static software.wings.beans.command.CommandUnitType.CODE_DEPLOY;
import static software.wings.beans.command.CommandUnitType.COMMAND;
import static software.wings.beans.command.CommandUnitType.COPY_CONFIGS;
import static software.wings.beans.command.CommandUnitType.ECS_SETUP;
import static software.wings.beans.command.CommandUnitType.EXEC;
import static software.wings.beans.command.CommandUnitType.KUBERNETES_SETUP;
import static software.wings.beans.command.CommandUnitType.PROCESS_CHECK_RUNNING;
import static software.wings.beans.command.CommandUnitType.PROCESS_CHECK_STOPPED;
import static software.wings.beans.command.CommandUnitType.RESIZE;
import static software.wings.beans.command.CommandUnitType.RESIZE_KUBERNETES;
import static software.wings.beans.command.CommandUnitType.SCP;
import static software.wings.beans.command.CommandUnitType.SETUP_ENV;
import static software.wings.beans.command.DownloadArtifactCommandUnit.Builder.aDownloadArtifactCommandUnit;
import static software.wings.beans.command.ExecCommandUnit.Builder.anExecCommandUnit;
import static software.wings.service.impl.aws.model.AwsConstants.AMI_SETUP_COMMAND_NAME;
import static software.wings.service.impl.workflow.WorkflowServiceHelper.AZURE_VMSS_DEPLOY;
import static software.wings.service.impl.workflow.WorkflowServiceHelper.AZURE_VMSS_SETUP;
import static software.wings.service.impl.workflow.WorkflowServiceHelper.AZURE_WEBAPP_SLOT_SETUP;
import static software.wings.service.impl.workflow.WorkflowServiceHelper.AZURE_WEBAPP_SLOT_SWAP;
import static software.wings.service.impl.workflow.WorkflowServiceHelper.PCF_RESIZE;
import static software.wings.service.impl.workflow.WorkflowServiceHelper.PCF_SETUP;
import static software.wings.sm.states.AwsAmiServiceDeployState.ASG_COMMAND_NAME;
import static software.wings.sm.states.AwsLambdaState.AWS_LAMBDA_COMMAND_NAME;
import static software.wings.utils.PowerShellScriptsLoader.psScriptMap;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.GraphNode;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandType;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.CommandUnitType;
import software.wings.beans.command.ScpCommandUnit.ScpFileCategory;
import software.wings.beans.command.SetupEnvCommandUnit;
import software.wings.utils.PowerShellScriptsLoader.PsScript;

import com.google.common.collect.ImmutableMap;
import java.util.List;

/**
 * The Enum ArtifactType.
 */
@OwnedBy(CDC)
@TargetModule(_930_DELEGATE_TASKS)
public enum ArtifactType {
  /**
   * Jar artifact type.
   */
  JAR {
    private static final long serialVersionUID = 2932493038229748527L;

    @Override
    public boolean isInternal() {
      return false;
    }

    @Override
    public List<Command> getDefaultCommands() {
      return asList(getStartCommand(), getStopCommand(), getInstallCommand());
    }

    /**
     * Gets start command graph.
     *
     * @return the start command graph
     */
    private Command getStartCommand() {
      return aCommand()
          .withCommandType(CommandType.START)
          .withGraph(aGraph()
                         .withGraphName("Start")
                         .addNodes(getStartServiceNode("java -jar \"$WINGS_RUNTIME_PATH/$ARTIFACT_FILE_NAME\""),
                             GraphNode.builder()
                                 .id(graphIdGenerator("node"))
                                 .name("Process Running")
                                 .type(PROCESS_CHECK_RUNNING.name())
                                 .properties(ImmutableMap.<String, Object>builder()
                                                 .put("commandString",
                                                     "i=0\n"
                                                         + "while [ \"$i\" -lt 30 ]\n"
                                                         + "do\n"
                                                         + "  pgrep -f \"$WINGS_RUNTIME_PATH/$ARTIFACT_FILE_NAME\"\n"
                                                         + "  rc=$?\n"
                                                         + "  if [ \"$rc\" -eq 0 ]\n"
                                                         + "  then\n"
                                                         + "    exit 0\n"
                                                         + "    sleep 1\n"
                                                         + "    i=$((i+1))\n"
                                                         + "  else\n"
                                                         + "    sleep 1\n"
                                                         + "    i=$((i+1))\n"
                                                         + "  fi\n"
                                                         + "done\n"
                                                         + "exit 1")
                                                 .build())

                                 .build())
                         .buildPipeline())
          .build();
    }

    /**
     * Gets stop command graph.
     *
     * @return the stop command graph
     */
    private Command getStopCommand() {
      return aCommand()
          .withCommandType(CommandType.STOP)
          .withGraph(aGraph()
                         .withGraphName("Stop")
                         .addNodes(getStopServiceNode(
                                       "\npgrep -f \"$WINGS_RUNTIME_PATH/$ARTIFACT_FILE_NAME\" | xargs kill  || true"),
                             GraphNode.builder()
                                 .id(graphIdGenerator("node"))
                                 .name("Process Stopped")
                                 .type(PROCESS_CHECK_STOPPED.name())
                                 .properties(ImmutableMap.<String, Object>builder()
                                                 .put("commandString",
                                                     "i=0\n"
                                                         + "while [ \"$i\" -lt 30 ]\n"
                                                         + "do\n"
                                                         + "  pgrep -f \"$WINGS_RUNTIME_PATH/$ARTIFACT_FILE_NAME\"\n"
                                                         + "  rc=$?\n"
                                                         + "  if [ \"$rc\" -eq 0 ]\n"
                                                         + "  then\n"
                                                         + "    sleep 1\n"
                                                         + "    i=$((i+1))\n"
                                                         + "  else\n"
                                                         + "    exit 0\n"
                                                         + "  fi\n"
                                                         + "done\n"
                                                         + "exit 1")
                                                 .build())
                                 .build())
                         .buildPipeline())
          .build();
    }

    /**
     * Gets install command graph.
     *
     * @return the install command graph
     */
    private Command getInstallCommand() {
      return aCommand()
          .withCommandType(CommandType.INSTALL)
          .withGraph(aGraph()
                         .withGraphName("Install")
                         .addNodes(getSetupRuntimePathsNode(),
                             GraphNode.builder()
                                 .id(graphIdGenerator("node"))
                                 .name("Stop")
                                 .type(COMMAND.name())
                                 .properties(ImmutableMap.<String, Object>builder().put("referenceId", "Stop").build())
                                 .build(),
                             getCopyArtifactNode(), getCopyConfigsNode(),
                             GraphNode.builder()
                                 .id(graphIdGenerator("node"))
                                 .name("Start")
                                 .type(COMMAND.name())
                                 .properties(ImmutableMap.<String, Object>builder().put("referenceId", "Start").build())
                                 .build())
                         .buildPipeline())
          .build();
    }
  },
  /**
   * War artifact type.
   */
  WAR {
    public static final long serialVersionUID = 2932493038229748527L;

    @Override
    public boolean isInternal() {
      return false;
    }

    @Override
    public List<Command> getDefaultCommands() {
      return asList(
          aCommand()
              .withCommandType(CommandType.INSTALL)
              .withGraph(aGraph()
                             .withGraphName("Install")
                             .addNodes(getSetupRuntimePathsNode(), getCopyArtifactNode(), getCopyConfigsNode())
                             .buildPipeline())
              .build());
    }
  },
  /**
   * Tar artifact type.
   */
  TAR {
    private static final long serialVersionUID = 2932493038229748527L;

    @Override
    public boolean isInternal() {
      return false;
    }

    @Override
    public List<Command> getDefaultCommands() {
      return asList(getStartCommand(), getInstallCommand(), getStopCommand());
    }

    /**
     * Gets start command graph.
     *
     * @return the start command graph
     */
    private Command getStartCommand() {
      return aCommand()
          .withCommandType(CommandType.START)
          .withGraph(aGraph()
                         .withGraphName("Start")
                         .addNodes(getStartServiceNode("echo \"service start script should be added here\""),
                             getServiceRunningNode())
                         .buildPipeline())
          .build();
    }

    /**
     * Gets stop command graph.
     *
     * @return the stop command graph
     */
    private Command getStopCommand() {
      return aCommand()
          .withCommandType(CommandType.STOP)
          .withGraph(
              aGraph()
                  .withGraphName("Stop")
                  .addNodes(getStopServiceNode("echo \"service stop script should be added here\""),
                      GraphNode.builder()
                          .id(graphIdGenerator("node"))
                          .name("Service Stopped")
                          .type(PROCESS_CHECK_STOPPED.name())
                          .properties(ImmutableMap.<String, Object>builder()
                                          .put("commandString", "echo \"service stopped check should be added here\"")
                                          .build())
                          .build())
                  .buildPipeline())
          .build();
    }

    /**
     * Get Install Command
     * @return the install command graph
     */
    private Command getInstallCommand() {
      return aCommand()
          .withCommandType(CommandType.INSTALL)
          .withGraph(aGraph()
                         .withGraphName("Install")
                         .addNodes(getSetupRuntimePathsNode(), getCopyArtifactNode(),
                             GraphNode.builder()
                                 .id(graphIdGenerator("node"))
                                 .name("Expand Artifact")
                                 .type(EXEC.name())
                                 .properties(ImmutableMap.<String, Object>builder()
                                                 .put("commandPath", "$WINGS_RUNTIME_PATH")
                                                 .put("commandString", "tar -xvzf \"$ARTIFACT_FILE_NAME\"")
                                                 .build())
                                 .build(),
                             getCopyConfigsNode())
                         .buildPipeline())
          .build();
    }
  },
  /**
   * Zip artifact type.
   */
  ZIP {
    private static final long serialVersionUID = 2932493038229748527L;

    @Override
    public boolean isInternal() {
      return false;
    }

    @Override
    public List<Command> getDefaultCommands() {
      return asList(getStartCommand(), getInstallCommand(), getStopCommand());
    }

    /**
     * Gets start command graph.
     *
     * @return the start command graph
     */
    private Command getStartCommand() {
      return aCommand()
          .withCommandType(CommandType.START)
          .withGraph(aGraph()
                         .withGraphName("Start")
                         .addNodes(getStartServiceNode("echo \"service start script should be added here\""),
                             getServiceRunningNode())
                         .buildPipeline())
          .build();
    }

    /**
     * Gets stop command graph.
     *
     * @return the stop command graph
     */
    private Command getStopCommand() {
      return aCommand()
          .withCommandType(CommandType.STOP)
          .withGraph(
              aGraph()
                  .withGraphName("Stop")
                  .addNodes(getStopServiceNode("echo \"service stop script should be added here\""),
                      GraphNode.builder()
                          .id(graphIdGenerator("node"))
                          .name("Service Stopped")
                          .type(PROCESS_CHECK_STOPPED.name())
                          .properties(ImmutableMap.<String, Object>builder()
                                          .put("commandString", "echo \"service stopped check should be added here\"")
                                          .build())
                          .build())
                  .buildPipeline())
          .build();
    }

    /**
     * Get Install Command
     * @return the install command graph
     */
    private Command getInstallCommand() {
      return aCommand()
          .withCommandType(CommandType.INSTALL)
          .withGraph(aGraph()
                         .withGraphName("Install")
                         .addNodes(getSetupRuntimePathsNode(), getCopyArtifactNode(),
                             GraphNode.builder()
                                 .id(graphIdGenerator("node"))
                                 .name("Expand Artifact")
                                 .type(EXEC.name())
                                 .properties(ImmutableMap.<String, Object>builder()
                                                 .put("commandPath", "$WINGS_RUNTIME_PATH")
                                                 .put("commandString", "unzip \"$ARTIFACT_FILE_NAME\"")
                                                 .build())
                                 .build(),
                             getCopyConfigsNode())
                         .buildPipeline())
          .build();
    }
  },

  /**
   * NuGET artifact type.
   */
  NUGET {
    private static final long serialVersionUID = 2932493038229748527L;

    @Override
    public boolean isInternal() {
      return false;
    }

    @Override
    public List<Command> getDefaultCommands() {
      return asList(getStartCommand(), getInstallCommand(), getStopCommand());
    }

    /**
     * Gets start command graph.
     * @return the start command graph
     */
    private Command getStartCommand() {
      return aCommand()
          .withCommandType(CommandType.START)
          .withGraph(aGraph()
                         .withGraphName("Start")
                         .addNodes(getStartServiceNode("echo \"service start script should be added here\""),
                             getServiceRunningNode())
                         .buildPipeline())
          .build();
    }

    /**
     * Gets stop command graph.
     * @return the stop command graph
     */
    private Command getStopCommand() {
      return aCommand()
          .withCommandType(CommandType.STOP)
          .withGraph(
              aGraph()
                  .withGraphName("Stop")
                  .addNodes(getStopServiceNode("echo \"service stop script should be added here\""),
                      GraphNode.builder()
                          .id(graphIdGenerator("node"))
                          .name("Service Stopped")
                          .type(PROCESS_CHECK_STOPPED.name())
                          .properties(ImmutableMap.<String, Object>builder()
                                          .put("commandString", "echo \"service stopped check should be added here\"")
                                          .build())
                          .build())
                  .buildPipeline())
          .build();
    }

    /**
     * Get Install Command
     * @return the install command graph
     */
    private Command getInstallCommand() {
      return aCommand()
          .withCommandType(CommandType.INSTALL)
          .withGraph(aGraph()
                         .withGraphName("Install")
                         .addNodes(getSetupRuntimePathsNode(), getCopyArtifactNode(),
                             GraphNode.builder()
                                 .id(graphIdGenerator("node"))
                                 .name("Expand Artifact")
                                 .type(EXEC.name())
                                 .properties(ImmutableMap.<String, Object>builder()
                                                 .put("commandPath", "$WINGS_RUNTIME_PATH")
                                                 .put("commandString", "nuget install \"$ARTIFACT_FILE_NAME\"")
                                                 .build())
                                 .build(),
                             getCopyConfigsNode())
                         .buildPipeline())
          .build();
    }
  },
  /**
   * Docker artifact type.
   */
  DOCKER {
    private static final long serialVersionUID = 2932493038229748527L;

    @Override
    public boolean isInternal() {
      return true;
    }

    @Override
    public List<Command> getDefaultCommands() {
      return asList(aCommand()
                        .withCommandType(CommandType.SETUP)
                        .withGraph(aGraph()
                                       .withGraphName("Setup Service Cluster")
                                       .addNodes(GraphNode.builder()
                                                     .origin(true)
                                                     .id(graphIdGenerator("node"))
                                                     .name("Setup ECS Service")
                                                     .type(ECS_SETUP.name())
                                                     .build())
                                       .buildPipeline())
                        .build(),
          aCommand()
              .withCommandType(CommandType.SETUP)
              .withGraph(aGraph()
                             .withGraphName("Setup Replication Controller")
                             .addNodes(GraphNode.builder()
                                           .origin(true)
                                           .id(graphIdGenerator("node"))
                                           .name("Setup Kubernetes Replication Controller")
                                           .type(KUBERNETES_SETUP.name())
                                           .build())
                             .buildPipeline())
              .build(),
          aCommand()
              .withCommandType(CommandType.RESIZE)
              .withGraph(aGraph()
                             .withGraphName("Resize Service Cluster")
                             .addNodes(GraphNode.builder()
                                           .origin(true)
                                           .id(graphIdGenerator("node"))
                                           .name("Resize ECS Service")
                                           .type(RESIZE.name())
                                           .build())
                             .buildPipeline())
              .build(),
          aCommand()
              .withCommandType(CommandType.RESIZE)
              .withGraph(aGraph()
                             .withGraphName("Resize Replication Controller")
                             .addNodes(GraphNode.builder()
                                           .origin(true)
                                           .id(graphIdGenerator("node"))
                                           .name("Resize Kubernetes Replication Controller")
                                           .type(RESIZE_KUBERNETES.name())
                                           .build())
                             .buildPipeline())
              .build());
    }
  },
  /**
   * RPM artifact type
   */
  RPM {
    private static final long serialVersionUID = 2932493038229748527L;

    @Override
    public boolean isInternal() {
      return false;
    }

    @Override
    public List<Command> getDefaultCommands() {
      return asList(getStartCommand(), getInstallCommand(), getStopCommand());
    }

    /**
     * Gets start command graph.
     *
     * @return the start command graph
     */
    private Command getStartCommand() {
      return aCommand()
          .withCommandType(CommandType.START)
          .withGraph(aGraph()
                         .withGraphName("Start")
                         .addNodes(getStartServiceNode("echo \"service start script should be added here\""),
                             getServiceRunningNode())
                         .buildPipeline())
          .build();
    }

    /**
     * Gets stop command graph.
     *
     * @return the stop command graph
     */
    private Command getStopCommand() {
      return aCommand()
          .withCommandType(CommandType.STOP)
          .withGraph(
              aGraph()
                  .withGraphName("Stop")
                  .addNodes(getStopServiceNode("echo \"service stop script should be added here\""),
                      GraphNode.builder()
                          .id(graphIdGenerator("node"))
                          .name("Service Stopped")
                          .type(PROCESS_CHECK_STOPPED.name())
                          .properties(ImmutableMap.<String, Object>builder()
                                          .put("commandString", "echo \"service stopped check should be added here\"")
                                          .build())
                          .build())
                  .buildPipeline())
          .build();
    }

    private Command getInstallCommand() {
      return aCommand()
          .withCommandType(CommandType.INSTALL)
          .withGraph(aGraph()
                         .withGraphName("Install")
                         .addNodes(getSetupRuntimePathsNode(), getCopyArtifactNode(), getCopyConfigsNode(),
                             GraphNode.builder()
                                 .id(graphIdGenerator("node"))
                                 .name("Install")
                                 .type(EXEC.name())
                                 .properties(ImmutableMap.<String, Object>builder()
                                                 .put("commandPath", "$WINGS_RUNTIME_PATH")
                                                 .put("commandString", "sudo yum install -y \"$ARTIFACT_FILE_NAME\"")
                                                 .build())
                                 .build())
                         .buildPipeline())
          .build();
    }
  },

  /**
   * The constant AWS_LAMBDA.
   */
  AWS_LAMBDA {
    private static final long serialVersionUID = 2932493038229748527L;

    @Override
    public boolean isInternal() {
      return true;
    }

    @Override
    public List<Command> getDefaultCommands() {
      return asList(getAwsLambdaCommand());
    }

    private Command getAwsLambdaCommand() {
      return aCommand()
          .withCommandType(CommandType.INSTALL)
          .withGraph(aGraph()
                         .withGraphName(AWS_LAMBDA_COMMAND_NAME)
                         .addNodes(GraphNode.builder()
                                       .origin(true)
                                       .id(graphIdGenerator("node"))
                                       .name(AWS_LAMBDA_COMMAND_NAME)
                                       .type(AWS_LAMBDA.name())
                                       .build())
                         .buildPipeline())
          .build();
    }
  },

  /**
   * The constant AWS_CODEDEPLOY.
   */
  AWS_CODEDEPLOY {
    private static final long serialVersionUID = 2932493038229748527L;

    @Override
    public boolean isInternal() {
      return true;
    }

    @Override
    public List<Command> getDefaultCommands() {
      return asList(getCodeDeployCommand());
    }

    /**
     * Get Code Deploy Command
     * @return
     */
    private Command getCodeDeployCommand() {
      return aCommand()
          .withCommandType(CommandType.INSTALL)
          .withGraph(aGraph()
                         .withGraphName("Amazon Code Deploy")
                         .addNodes(GraphNode.builder()
                                       .origin(true)
                                       .id(graphIdGenerator("node"))
                                       .name("Amazon Code Deploy")
                                       .type(CODE_DEPLOY.name())
                                       .build())
                         .buildPipeline())
          .build();
    }
  },

  /**
   * The constant AWS_CODEDEPLOY.
   */
  PCF {
    private static final long serialVersionUID = 2932493038229748527L;

    @Override
    public boolean isInternal() {
      return true;
    }

    @Override
    public List<Command> getDefaultCommands() {
      return asList(getPcfSetupCommand());
    }

    /**
     * Get Code Deploy Command
     * @return
     */
    private Command getPcfSetupCommand() {
      return aCommand()
          .withCommandType(CommandType.SETUP)
          .withGraph(aGraph()
                         .withGraphName(PCF_SETUP)
                         .addNodes(GraphNode.builder()
                                       .origin(true)
                                       .id(graphIdGenerator("node"))
                                       .name(PCF_SETUP)
                                       .type(PCF.name())
                                       .build())
                         .buildPipeline())
          .build();
    }

    private Command getPcfDeployCommand() {
      return aCommand()
          .withCommandType(CommandType.RESIZE)
          .withGraph(aGraph()
                         .withGraphName(PCF_RESIZE)
                         .addNodes(GraphNode.builder()
                                       .origin(true)
                                       .id(graphIdGenerator("node"))
                                       .name(PCF_RESIZE)
                                       .type(PCF.name())
                                       .build())
                         .buildPipeline())
          .build();
    }
  },

  /**
   * The constant AWS_CODEDEPLOY.
   */
  AMI {
    private static final long serialVersionUID = 2932493038229748527L;

    @Override
    public boolean isInternal() {
      return true;
    }

    @Override
    public List<Command> getDefaultCommands() {
      return asList(getAmiSetupCommandUnit(), getAmiDeployCommandUnit());
    }

    private Command getAmiSetupCommandUnit() {
      return aCommand()
          .withCommandType(CommandType.SETUP)
          .withGraph(aGraph()
                         .withGraphName(AMI_SETUP_COMMAND_NAME)
                         .addNodes(GraphNode.builder()
                                       .origin(true)
                                       .id(graphIdGenerator("node"))
                                       .name(AMI_SETUP_COMMAND_NAME)
                                       .type(AWS_AMI.name())
                                       .build())
                         .buildPipeline())
          .build();
    }

    /**
     * Get Code Deploy Command
     * @return
     */
    private Command getAmiDeployCommandUnit() {
      return aCommand()
          .withCommandType(CommandType.INSTALL)
          .withGraph(aGraph()
                         .withGraphName(ASG_COMMAND_NAME)
                         .addNodes(GraphNode.builder()
                                       .origin(true)
                                       .id(graphIdGenerator("node"))
                                       .name(ASG_COMMAND_NAME)
                                       .type(AWS_AMI.name())
                                       .build())
                         .buildPipeline())
          .build();
    }
  },

  /**
   * The constant AZURE_MACHINE_IMAGE.
   */
  AZURE_MACHINE_IMAGE {
    private static final long serialVersionUID = 2932493038229748527L;

    @Override
    public boolean isInternal() {
      return true;
    }

    @Override
    public List<Command> getDefaultCommands() {
      return asList(getAzureMachineImageSetupCommandUnit(), getAzureMachineImageDeployCommandUnit());
    }

    private Command getAzureMachineImageSetupCommandUnit() {
      return aCommand()
          .withCommandType(CommandType.SETUP)
          .withGraph(aGraph()
                         .withGraphName(AZURE_VMSS_SETUP)
                         .addNodes(GraphNode.builder()
                                       .origin(true)
                                       .id(graphIdGenerator("node"))
                                       .name(AZURE_VMSS_SETUP)
                                       .type(AZURE_VMSS_DUMMY.name())
                                       .build())
                         .buildPipeline())
          .build();
    }

    /**
     * Get Code Deploy Command
     * @return
     */
    private Command getAzureMachineImageDeployCommandUnit() {
      return aCommand()
          .withCommandType(CommandType.INSTALL)
          .withGraph(aGraph()
                         .withGraphName(AZURE_VMSS_DEPLOY)
                         .addNodes(GraphNode.builder()
                                       .origin(true)
                                       .id(graphIdGenerator("node"))
                                       .name(AZURE_VMSS_DEPLOY)
                                       .type(AZURE_VMSS_DUMMY.name())
                                       .build())
                         .buildPipeline())
          .build();
    }
  },

  /**
   * The constant AZURE_WEBAPP.
   */
  AZURE_WEBAPP {
    private static final long serialVersionUID = 2932493038229748527L;

    @Override
    public boolean isInternal() {
      return true;
    }

    @Override
    public List<Command> getDefaultCommands() {
      return asList(getAzureWebAppSlotSetupCommand(), getAzureWebAppSlotSwapCommand());
    }

    private Command getAzureWebAppSlotSetupCommand() {
      return aCommand()
          .withCommandType(CommandType.SETUP)
          .withGraph(aGraph()
                         .withGraphName(AZURE_WEBAPP_SLOT_SETUP)
                         .addNodes(GraphNode.builder()
                                       .origin(true)
                                       .id(graphIdGenerator("node"))
                                       .name(AZURE_WEBAPP_SLOT_SETUP)
                                       .type(CommandUnitType.AZURE_WEBAPP.name())
                                       .build())
                         .buildPipeline())
          .build();
    }

    /**
     * Get Code Deploy Command
     * @return
     */
    private Command getAzureWebAppSlotSwapCommand() {
      return aCommand()
          .withCommandType(CommandType.INSTALL)
          .withGraph(aGraph()
                         .withGraphName(AZURE_WEBAPP_SLOT_SWAP)
                         .addNodes(GraphNode.builder()
                                       .origin(true)
                                       .id(graphIdGenerator("node"))
                                       .name(AZURE_WEBAPP_SLOT_SWAP)
                                       .type(CommandUnitType.AZURE_WEBAPP.name())
                                       .build())
                         .buildPipeline())
          .build();
    }
  },

  IIS {
    private static final long serialVersionUID = 2932493038229748527L;

    @Override
    public boolean isInternal() {
      return false;
    }

    @Override
    public List<Command> getDefaultCommands() {
      return asList(getInstallCommand());
    }

    private Command getInstallCommand() {
      return aCommand()
          .withCommandType(CommandType.INSTALL)
          .withName("Install IIS Website")
          .withCommandUnits(asList(new CommandUnit[] {aDownloadArtifactCommandUnit()
                                                          .withName(PsScript.DownloadArtifact.getDisplayName())
                                                          .withScriptType(POWERSHELL)
                                                          .withCommandPath("$env:TEMP")
                                                          .build(),
              anExecCommandUnit()
                  .withName(PsScript.ExpandArtifacts.getDisplayName())
                  .withScriptType(POWERSHELL)
                  .withCommandString(psScriptMap.get(PsScript.ExpandArtifacts))
                  .build(),
              anExecCommandUnit()
                  .withName(PsScript.CreateIISAppPool.getDisplayName())
                  .withScriptType(POWERSHELL)
                  .withCommandString(psScriptMap.get(PsScript.CreateIISAppPool))
                  .build(),
              anExecCommandUnit()
                  .withName(PsScript.CreateIISWebsite.getDisplayName())
                  .withScriptType(POWERSHELL)
                  .withCommandString(psScriptMap.get(PsScript.CreateIISWebsite))
                  .build()}))
          .build();
    }
  },

  /**
   * Other artifact type.
   */
  OTHER {
    private static final long serialVersionUID = 2932493038229748527L;

    @Override
    public boolean isInternal() {
      return false;
    }

    @Override
    public List<Command> getDefaultCommands() {
      return emptyList();
    }
  },

  IIS_APP {
    private static final long serialVersionUID = 2932493038229748527L;

    @Override
    public boolean isInternal() {
      return false;
    }

    @Override
    public List<Command> getDefaultCommands() {
      return asList(getInstallCommand());
    }

    private Command getInstallCommand() {
      return aCommand()
          .withCommandType(CommandType.INSTALL)
          .withName("Install IIS Application")
          .withCommandUnits(asList(new CommandUnit[] {aDownloadArtifactCommandUnit()
                                                          .withName(PsScript.DownloadArtifact.getDisplayName())
                                                          .withScriptType(POWERSHELL)
                                                          .withCommandPath("$env:TEMP")
                                                          .build(),
              anExecCommandUnit()
                  .withName(PsScript.ExpandArtifacts.getDisplayName())
                  .withScriptType(POWERSHELL)
                  .withCommandString(psScriptMap.get(PsScript.ExpandArtifacts))
                  .build(),
              anExecCommandUnit()
                  .withName(PsScript.CreateIISVirtualDirectory.getDisplayName())
                  .withScriptType(POWERSHELL)
                  .withCommandString(psScriptMap.get(PsScript.CreateIISVirtualDirectory))
                  .build()}))
          .build();
    }
  },

  IIS_VirtualDirectory {
    private static final long serialVersionUID = 2932493038229748527L;

    @Override
    public boolean isInternal() {
      return false;
    }

    @Override
    public List<Command> getDefaultCommands() {
      return asList(getInstallCommand());
    }

    private Command getInstallCommand() {
      return aCommand()
          .withCommandType(CommandType.INSTALL)
          .withName("Install IIS Application")
          .withCommandUnits(asList(new CommandUnit[] {aDownloadArtifactCommandUnit()
                                                          .withName(PsScript.DownloadArtifact.getDisplayName())
                                                          .withScriptType(POWERSHELL)
                                                          .withCommandPath("$env:TEMP")
                                                          .build(),
              anExecCommandUnit()
                  .withName(PsScript.ExpandArtifacts.getDisplayName())
                  .withScriptType(POWERSHELL)
                  .withCommandString(psScriptMap.get(PsScript.ExpandArtifacts))
                  .build(),
              anExecCommandUnit()
                  .withName(PsScript.CreateIISVirtualDirectory.getDisplayName())
                  .withScriptType(POWERSHELL)
                  .withCommandString(psScriptMap.get(PsScript.CreateIISVirtualDirectory))
                  .build()}))
          .build();
    }
  };

  private static GraphNode getSetupRuntimePathsNode() {
    return GraphNode.builder()
        .origin(true)
        .id(graphIdGenerator("node"))
        .name("Setup Runtime Paths")
        .type(SETUP_ENV.name())
        .properties(ImmutableMap.<String, Object>builder()
                        .put("commandString", SetupEnvCommandUnit.SETUP_ENV_COMMAND_STRING)
                        .build())
        .build();
  }

  private static GraphNode getCopyConfigsNode() {
    return GraphNode.builder()
        .id(graphIdGenerator("node"))
        .name("Copy Configs")
        .type(COPY_CONFIGS.name())
        .properties(ImmutableMap.<String, Object>builder().put("destinationParentPath", "$WINGS_RUNTIME_PATH").build())
        .build();
  }

  private static GraphNode getCopyArtifactNode() {
    return GraphNode.builder()
        .id(graphIdGenerator("node"))
        .name("Copy Artifact")
        .type(SCP.name())
        .properties(ImmutableMap.<String, Object>builder()
                        .put("fileCategory", ScpFileCategory.ARTIFACTS)
                        .put("destinationDirectoryPath", "$WINGS_RUNTIME_PATH")
                        .build())
        .build();
  }

  private static GraphNode getStartServiceNode(String commandString) {
    return GraphNode.builder()
        .origin(true)
        .id(graphIdGenerator("node"))
        .type(EXEC.name())
        .name("Start Service")
        .properties(ImmutableMap.<String, Object>builder()
                        .put("commandPath", "$WINGS_RUNTIME_PATH")
                        .put("commandString", "java -jar \"$WINGS_RUNTIME_PATH/$ARTIFACT_FILE_NAME\"")
                        .build())
        .build();
  }

  private static GraphNode getStopServiceNode(String commandString) {
    return GraphNode.builder()
        .origin(true)
        .id(graphIdGenerator("node"))
        .type(EXEC.name())
        .name("Stop Service")
        .properties(ImmutableMap.<String, Object>builder()
                        .put("commandPath", "$WINGS_RUNTIME_PATH")
                        .put("commandString", commandString)
                        .build())
        .build();
  }

  private static GraphNode getServiceRunningNode() {
    return GraphNode.builder()
        .id(graphIdGenerator("node"))
        .name("Service Running")
        .type(PROCESS_CHECK_RUNNING.name())
        .properties(ImmutableMap.<String, Object>builder()
                        .put("commandString", "echo \"service running check should be added here\"")
                        .build())
        .build();
  }

  /**
   * Gets default commands.
   *
   * @return the default commands
   */
  public abstract List<Command> getDefaultCommands();

  public abstract boolean isInternal();
}
