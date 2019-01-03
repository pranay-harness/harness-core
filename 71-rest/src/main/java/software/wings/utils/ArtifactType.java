package software.wings.utils;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static software.wings.api.ScriptType.POWERSHELL;
import static software.wings.beans.Graph.Builder.aGraph;
import static software.wings.beans.GraphNode.GraphNodeBuilder.aGraphNode;
import static software.wings.beans.command.Command.Builder.aCommand;
import static software.wings.beans.command.CommandUnitType.AWS_AMI;
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
import static software.wings.common.Constants.AMI_SETUP_COMMAND_NAME;
import static software.wings.common.Constants.ASG_COMMAND_NAME;
import static software.wings.common.Constants.PCF_RESIZE;
import static software.wings.common.Constants.PCF_SETUP;
import static software.wings.utils.PowerShellScriptsLoader.psScriptMap;

import io.harness.data.structure.UUIDGenerator;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandType;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.ScpCommandUnit.ScpFileCategory;
import software.wings.beans.command.SetupEnvCommandUnit;
import software.wings.common.Constants;
import software.wings.utils.PowerShellScriptsLoader.PsScript;

import java.util.List;

/**
 * The Enum ArtifactType.
 */
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
          .withGraph(
              aGraph()
                  .withGraphName("Start")
                  .addNodes(aGraphNode()
                                .origin(true)
                                .id(UUIDGenerator.graphIdGenerator("node"))
                                .type(EXEC.name())
                                .name("Start Service")
                                .addProperty("commandPath", "$WINGS_RUNTIME_PATH")
                                .addProperty("commandString", "java -jar \"$WINGS_RUNTIME_PATH/$ARTIFACT_FILE_NAME\"")
                                .build(),
                      aGraphNode()
                          .id(UUIDGenerator.graphIdGenerator("node"))
                          .name("Process Running")
                          .type(PROCESS_CHECK_RUNNING.name())
                          .addProperty("commandString",
                              "set -x\n"
                                  + "i=0\n"
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
                  .addNodes(aGraphNode()
                                .origin(true)
                                .id(UUIDGenerator.graphIdGenerator("node"))
                                .type(EXEC.name())
                                .name("Stop Service")
                                .addProperty("commandPath", "$WINGS_RUNTIME_PATH")
                                .addProperty("commandString",
                                    "\npgrep -f \"$WINGS_RUNTIME_PATH/$ARTIFACT_FILE_NAME\" | xargs kill  || true")
                                .build(),
                      aGraphNode()
                          .id(UUIDGenerator.graphIdGenerator("node"))
                          .name("Process Stopped")
                          .type(PROCESS_CHECK_STOPPED.name())
                          .addProperty("commandString",
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
                         .addNodes(aGraphNode()
                                       .origin(true)
                                       .id(UUIDGenerator.graphIdGenerator("node"))
                                       .name("Setup Runtime Paths")
                                       .type(SETUP_ENV.name())
                                       .addProperty("commandString", SetupEnvCommandUnit.SETUP_ENV_COMMAND_STRING)
                                       .build(),
                             aGraphNode()
                                 .id(UUIDGenerator.graphIdGenerator("node"))
                                 .name("Stop")
                                 .type(COMMAND.name())
                                 .addProperty("referenceId", "Stop")
                                 .build(),
                             aGraphNode()
                                 .id(UUIDGenerator.graphIdGenerator("node"))
                                 .name("Copy Artifact")
                                 .type(SCP.name())
                                 .addProperty("fileCategory", ScpFileCategory.ARTIFACTS)
                                 .addProperty("destinationDirectoryPath", "$WINGS_RUNTIME_PATH")
                                 .build(),
                             aGraphNode()
                                 .id(UUIDGenerator.graphIdGenerator("node"))
                                 .name("Copy Configs")
                                 .type(COPY_CONFIGS.name())
                                 .addProperty("destinationParentPath", "$WINGS_RUNTIME_PATH")
                                 .build(),
                             aGraphNode()
                                 .id(UUIDGenerator.graphIdGenerator("node"))
                                 .name("Start")
                                 .type(COMMAND.name())
                                 .addProperty("referenceId", "Start")
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
                             .addNodes(aGraphNode()
                                           .origin(true)
                                           .id(UUIDGenerator.graphIdGenerator("node"))
                                           .name("Setup Runtime Paths")
                                           .type(SETUP_ENV.name())
                                           .addProperty("commandString", SetupEnvCommandUnit.SETUP_ENV_COMMAND_STRING)
                                           .build(),
                                 aGraphNode()
                                     .id(UUIDGenerator.graphIdGenerator("node"))
                                     .name("Copy Artifact")
                                     .type(SCP.name())
                                     .addProperty("fileCategory", ScpFileCategory.ARTIFACTS)
                                     .addProperty("destinationDirectoryPath", "$WINGS_RUNTIME_PATH")
                                     .build(),
                                 aGraphNode()
                                     .id(UUIDGenerator.graphIdGenerator("node"))
                                     .name("Copy Configs")
                                     .type(COPY_CONFIGS.name())
                                     .addProperty("destinationParentPath", "$WINGS_RUNTIME_PATH")
                                     .build())
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
          .withGraph(
              aGraph()
                  .withGraphName("Start")
                  .addNodes(aGraphNode()
                                .origin(true)
                                .id(UUIDGenerator.graphIdGenerator("node"))
                                .type(EXEC.name())
                                .name("Start Service")
                                .addProperty("commandPath", "$WINGS_RUNTIME_PATH")
                                .addProperty("commandString", "echo \"service start script should be added here\"")
                                .build(),
                      aGraphNode()
                          .id(UUIDGenerator.graphIdGenerator("node"))
                          .name("Service Running")
                          .type(PROCESS_CHECK_RUNNING.name())
                          .addProperty("commandString", "echo \"service running check should be added here\"")
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
          .withGraph(
              aGraph()
                  .withGraphName("Stop")
                  .addNodes(aGraphNode()
                                .origin(true)
                                .id(UUIDGenerator.graphIdGenerator("node"))
                                .type(EXEC.name())
                                .name("Stop Service")
                                .addProperty("commandPath", "$WINGS_RUNTIME_PATH")
                                .addProperty("commandString", "echo \"service stop script should be added here\"")
                                .build(),
                      aGraphNode()
                          .id(UUIDGenerator.graphIdGenerator("node"))
                          .name("Service Stopped")
                          .type(PROCESS_CHECK_STOPPED.name())
                          .addProperty("commandString", "echo \"service stopped check should be added here\"")
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
                         .addNodes(aGraphNode()
                                       .origin(true)
                                       .id(UUIDGenerator.graphIdGenerator("node"))
                                       .name("Setup Runtime Paths")
                                       .type(SETUP_ENV.name())
                                       .addProperty("commandString", SetupEnvCommandUnit.SETUP_ENV_COMMAND_STRING)
                                       .build(),
                             aGraphNode()
                                 .id(UUIDGenerator.graphIdGenerator("node"))
                                 .name("Copy Artifact")
                                 .type(SCP.name())
                                 .addProperty("fileCategory", ScpFileCategory.ARTIFACTS)
                                 .addProperty("destinationDirectoryPath", "$WINGS_RUNTIME_PATH")
                                 .build(),
                             aGraphNode()
                                 .id(UUIDGenerator.graphIdGenerator("node"))
                                 .name("Expand Artifact")
                                 .type(EXEC.name())
                                 .addProperty("commandPath", "$WINGS_RUNTIME_PATH")
                                 .addProperty("commandString", "tar -xvzf \"$ARTIFACT_FILE_NAME\"")
                                 .build(),
                             aGraphNode()
                                 .id(UUIDGenerator.graphIdGenerator("node"))
                                 .name("Copy Configs")
                                 .type(COPY_CONFIGS.name())
                                 .addProperty("destinationParentPath", "$WINGS_RUNTIME_PATH")
                                 .build())
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
          .withGraph(
              aGraph()
                  .withGraphName("Start")
                  .addNodes(aGraphNode()
                                .origin(true)
                                .id(UUIDGenerator.graphIdGenerator("node"))
                                .type(EXEC.name())
                                .name("Start Service")
                                .addProperty("commandPath", "$WINGS_RUNTIME_PATH")
                                .addProperty("commandString", "echo \"service start script should be added here\"")
                                .build(),
                      aGraphNode()
                          .id(UUIDGenerator.graphIdGenerator("node"))
                          .name("Service Running")
                          .type(PROCESS_CHECK_RUNNING.name())
                          .addProperty("commandString", "echo \"service running check should be added here\"")
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
          .withGraph(
              aGraph()
                  .withGraphName("Stop")
                  .addNodes(aGraphNode()
                                .origin(true)
                                .id(UUIDGenerator.graphIdGenerator("node"))
                                .type(EXEC.name())
                                .name("Stop Service")
                                .addProperty("commandPath", "$WINGS_RUNTIME_PATH")
                                .addProperty("commandString", "echo \"service stop script should be added here\"")
                                .build(),
                      aGraphNode()
                          .id(UUIDGenerator.graphIdGenerator("node"))
                          .name("Service Stopped")
                          .type(PROCESS_CHECK_STOPPED.name())
                          .addProperty("commandString", "echo \"service stopped check should be added here\"")
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
          .withCommandType(CommandType.START)
          .withGraph(aGraph()
                         .withGraphName("Install")
                         .addNodes(aGraphNode()
                                       .origin(true)
                                       .id(UUIDGenerator.graphIdGenerator("node"))
                                       .name("Setup Runtime Paths")
                                       .type(SETUP_ENV.name())
                                       .addProperty("commandString", SetupEnvCommandUnit.SETUP_ENV_COMMAND_STRING)
                                       .build(),
                             aGraphNode()
                                 .id(UUIDGenerator.graphIdGenerator("node"))
                                 .name("Copy Artifact")
                                 .type(SCP.name())
                                 .addProperty("fileCategory", ScpFileCategory.ARTIFACTS)
                                 .addProperty("destinationDirectoryPath", "$WINGS_RUNTIME_PATH")
                                 .build(),
                             aGraphNode()
                                 .id(UUIDGenerator.graphIdGenerator("node"))
                                 .name("Expand Artifact")
                                 .type(EXEC.name())
                                 .addProperty("commandPath", "$WINGS_RUNTIME_PATH")
                                 .addProperty("commandString", "unzip \"$ARTIFACT_FILE_NAME\"")
                                 .build(),
                             aGraphNode()
                                 .id(UUIDGenerator.graphIdGenerator("node"))
                                 .name("Copy Configs")
                                 .type(COPY_CONFIGS.name())
                                 .addProperty("destinationParentPath", "$WINGS_RUNTIME_PATH")
                                 .build())
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
                                       .addNodes(aGraphNode()
                                                     .origin(true)
                                                     .id(UUIDGenerator.graphIdGenerator("node"))
                                                     .name("Setup ECS Service")
                                                     .type(ECS_SETUP.name())
                                                     .build())
                                       .buildPipeline())
                        .build(),
          aCommand()
              .withCommandType(CommandType.SETUP)
              .withGraph(aGraph()
                             .withGraphName("Setup Replication Controller")
                             .addNodes(aGraphNode()
                                           .origin(true)
                                           .id(UUIDGenerator.graphIdGenerator("node"))
                                           .name("Setup Kubernetes Replication Controller")
                                           .type(KUBERNETES_SETUP.name())
                                           .build())
                             .buildPipeline())
              .build(),
          aCommand()
              .withCommandType(CommandType.RESIZE)
              .withGraph(aGraph()
                             .withGraphName("Resize Service Cluster")
                             .addNodes(aGraphNode()
                                           .origin(true)
                                           .id(UUIDGenerator.graphIdGenerator("node"))
                                           .name("Resize ECS Service")
                                           .type(RESIZE.name())
                                           .build())
                             .buildPipeline())
              .build(),
          aCommand()
              .withCommandType(CommandType.RESIZE)
              .withGraph(aGraph()
                             .withGraphName("Resize Replication Controller")
                             .addNodes(aGraphNode()
                                           .origin(true)
                                           .id(UUIDGenerator.graphIdGenerator("node"))
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
          .withGraph(
              aGraph()
                  .withGraphName("Start")
                  .addNodes(aGraphNode()
                                .origin(true)
                                .id(UUIDGenerator.graphIdGenerator("node"))
                                .type(EXEC.name())
                                .name("Start Service")
                                .addProperty("commandPath", "$WINGS_RUNTIME_PATH")
                                .addProperty("commandString", "echo \"service start script should be added here\"")
                                .build(),
                      aGraphNode()
                          .id(UUIDGenerator.graphIdGenerator("node"))
                          .name("Service Running")
                          .type(PROCESS_CHECK_RUNNING.name())
                          .addProperty("commandString", "echo \"service running check should be added here\"")
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
          .withGraph(
              aGraph()
                  .withGraphName("Stop")
                  .addNodes(aGraphNode()
                                .origin(true)
                                .id(UUIDGenerator.graphIdGenerator("node"))
                                .type(EXEC.name())
                                .name("Stop Service")
                                .addProperty("commandPath", "$WINGS_RUNTIME_PATH")
                                .addProperty("commandString", "echo \"service stop script should be added here\"")
                                .build(),
                      aGraphNode()
                          .id(UUIDGenerator.graphIdGenerator("node"))
                          .name("Service Stopped")
                          .type(PROCESS_CHECK_STOPPED.name())
                          .addProperty("commandString", "echo \"service stopped check should be added here\"")
                          .build())
                  .buildPipeline())
          .build();
    }

    private Command getInstallCommand() {
      return aCommand()
          .withCommandType(CommandType.INSTALL)
          .withGraph(aGraph()
                         .withGraphName("Install")
                         .addNodes(aGraphNode()
                                       .origin(true)
                                       .id(UUIDGenerator.graphIdGenerator("node"))
                                       .name("Setup Runtime Paths")
                                       .type(SETUP_ENV.name())
                                       .addProperty("commandString", SetupEnvCommandUnit.SETUP_ENV_COMMAND_STRING)
                                       .build(),
                             aGraphNode()
                                 .id(UUIDGenerator.graphIdGenerator("node"))
                                 .name("Copy Artifact")
                                 .type(SCP.name())
                                 .addProperty("fileCategory", ScpFileCategory.ARTIFACTS)
                                 .addProperty("destinationDirectoryPath", "$WINGS_RUNTIME_PATH")
                                 .build(),
                             aGraphNode()
                                 .id(UUIDGenerator.graphIdGenerator("node"))
                                 .name("Copy Configs")
                                 .type(COPY_CONFIGS.name())
                                 .addProperty("destinationParentPath", "$WINGS_RUNTIME_PATH")
                                 .build(),
                             aGraphNode()
                                 .id(UUIDGenerator.graphIdGenerator("node"))
                                 .name("Install")
                                 .type(EXEC.name())
                                 .addProperty("commandPath", "$WINGS_RUNTIME_PATH")
                                 .addProperty("commandString", "sudo yum install -y \"$ARTIFACT_FILE_NAME\"")
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
                         .withGraphName(Constants.AWS_LAMBDA_COMMAND_NAME)
                         .addNodes(aGraphNode()
                                       .origin(true)
                                       .id(UUIDGenerator.graphIdGenerator("node"))
                                       .name(Constants.AWS_LAMBDA_COMMAND_NAME)
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
                         .addNodes(aGraphNode()
                                       .origin(true)
                                       .id(UUIDGenerator.graphIdGenerator("node"))
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
                         .addNodes(aGraphNode()
                                       .origin(true)
                                       .id(UUIDGenerator.graphIdGenerator("node"))
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
                         .addNodes(aGraphNode()
                                       .origin(true)
                                       .id(UUIDGenerator.graphIdGenerator("node"))
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
                         .addNodes(aGraphNode()
                                       .origin(true)
                                       .id(UUIDGenerator.graphIdGenerator("node"))
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
                         .addNodes(aGraphNode()
                                       .origin(true)
                                       .id(UUIDGenerator.graphIdGenerator("node"))
                                       .name(ASG_COMMAND_NAME)
                                       .type(AWS_AMI.name())
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

  /**
   * Gets default commands.
   *
   * @return the default commands
   */
  public abstract List<Command> getDefaultCommands();

  public abstract boolean isInternal();
}
