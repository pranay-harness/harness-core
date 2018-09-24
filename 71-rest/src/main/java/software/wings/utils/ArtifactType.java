package software.wings.utils;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
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
import static software.wings.common.Constants.AMI_SETUP_COMMAND_NAME;
import static software.wings.common.Constants.ASG_COMMAND_NAME;
import static software.wings.common.Constants.PCF_RESIZE;
import static software.wings.common.Constants.PCF_SETUP;
import static software.wings.utils.PowerShellScriptsLoader.psScriptMap;

import io.harness.data.structure.UUIDGenerator;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandType;
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
                                .withOrigin(true)
                                .withId(UUIDGenerator.graphIdGenerator("node"))
                                .withType(EXEC.name())
                                .withName("Start Service")
                                .addProperty("commandPath", "$WINGS_RUNTIME_PATH")
                                .addProperty("commandString", "java -jar \"$WINGS_RUNTIME_PATH/$ARTIFACT_FILE_NAME\"")
                                .build(),
                      aGraphNode()
                          .withId(UUIDGenerator.graphIdGenerator("node"))
                          .withName("Process Running")
                          .withType(PROCESS_CHECK_RUNNING.name())
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
                                .withOrigin(true)
                                .withId(UUIDGenerator.graphIdGenerator("node"))
                                .withType(EXEC.name())
                                .withName("Stop Service")
                                .addProperty("commandPath", "$WINGS_RUNTIME_PATH")
                                .addProperty("commandString",
                                    "\npgrep -f \"$WINGS_RUNTIME_PATH/$ARTIFACT_FILE_NAME\" | xargs kill  || true")
                                .build(),
                      aGraphNode()
                          .withId(UUIDGenerator.graphIdGenerator("node"))
                          .withName("Process Stopped")
                          .withType(PROCESS_CHECK_STOPPED.name())
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
                                       .withOrigin(true)
                                       .withId(UUIDGenerator.graphIdGenerator("node"))
                                       .withName("Setup Runtime Paths")
                                       .withType(SETUP_ENV.name())
                                       .addProperty("commandString", SetupEnvCommandUnit.setupEnvCommandString)
                                       .build(),
                             aGraphNode()
                                 .withId(UUIDGenerator.graphIdGenerator("node"))
                                 .withName("Stop")
                                 .withType(COMMAND.name())
                                 .addProperty("referenceId", "Stop")
                                 .build(),
                             aGraphNode()
                                 .withId(UUIDGenerator.graphIdGenerator("node"))
                                 .withName("Copy Artifact")
                                 .withType(SCP.name())
                                 .addProperty("fileCategory", ScpFileCategory.ARTIFACTS)
                                 .addProperty("destinationDirectoryPath", "$WINGS_RUNTIME_PATH")
                                 .build(),
                             aGraphNode()
                                 .withId(UUIDGenerator.graphIdGenerator("node"))
                                 .withName("Copy Configs")
                                 .withType(COPY_CONFIGS.name())
                                 .addProperty("destinationParentPath", "$WINGS_RUNTIME_PATH")
                                 .build(),
                             aGraphNode()
                                 .withId(UUIDGenerator.graphIdGenerator("node"))
                                 .withName("Start")
                                 .withType(COMMAND.name())
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
                                           .withOrigin(true)
                                           .withId(UUIDGenerator.graphIdGenerator("node"))
                                           .withName("Setup Runtime Paths")
                                           .withType(SETUP_ENV.name())
                                           .addProperty("commandString", SetupEnvCommandUnit.setupEnvCommandString)
                                           .build(),
                                 aGraphNode()
                                     .withId(UUIDGenerator.graphIdGenerator("node"))
                                     .withName("Copy Artifact")
                                     .withType(SCP.name())
                                     .addProperty("fileCategory", ScpFileCategory.ARTIFACTS)
                                     .addProperty("destinationDirectoryPath", "$WINGS_RUNTIME_PATH")
                                     .build(),
                                 aGraphNode()
                                     .withId(UUIDGenerator.graphIdGenerator("node"))
                                     .withName("Copy Configs")
                                     .withType(COPY_CONFIGS.name())
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
                                .withOrigin(true)
                                .withId(UUIDGenerator.graphIdGenerator("node"))
                                .withType(EXEC.name())
                                .withName("Start Service")
                                .addProperty("commandPath", "$WINGS_RUNTIME_PATH")
                                .addProperty("commandString", "echo \"service start script should be added here\"")
                                .build(),
                      aGraphNode()
                          .withId(UUIDGenerator.graphIdGenerator("node"))
                          .withName("Service Running")
                          .withType(PROCESS_CHECK_RUNNING.name())
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
                                .withOrigin(true)
                                .withId(UUIDGenerator.graphIdGenerator("node"))
                                .withType(EXEC.name())
                                .withName("Stop Service")
                                .addProperty("commandPath", "$WINGS_RUNTIME_PATH")
                                .addProperty("commandString", "echo \"service stop script should be added here\"")
                                .build(),
                      aGraphNode()
                          .withId(UUIDGenerator.graphIdGenerator("node"))
                          .withName("Service Stopped")
                          .withType(PROCESS_CHECK_STOPPED.name())
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
                                       .withOrigin(true)
                                       .withId(UUIDGenerator.graphIdGenerator("node"))
                                       .withName("Setup Runtime Paths")
                                       .withType(SETUP_ENV.name())
                                       .addProperty("commandString", SetupEnvCommandUnit.setupEnvCommandString)
                                       .build(),
                             aGraphNode()
                                 .withId(UUIDGenerator.graphIdGenerator("node"))
                                 .withName("Copy Artifact")
                                 .withType(SCP.name())
                                 .addProperty("fileCategory", ScpFileCategory.ARTIFACTS)
                                 .addProperty("destinationDirectoryPath", "$WINGS_RUNTIME_PATH")
                                 .build(),
                             aGraphNode()
                                 .withId(UUIDGenerator.graphIdGenerator("node"))
                                 .withName("Expand Artifact")
                                 .withType(EXEC.name())
                                 .addProperty("commandPath", "$WINGS_RUNTIME_PATH")
                                 .addProperty("commandString", "tar -xvzf \"$ARTIFACT_FILE_NAME\"")
                                 .build(),
                             aGraphNode()
                                 .withId(UUIDGenerator.graphIdGenerator("node"))
                                 .withName("Copy Configs")
                                 .withType(COPY_CONFIGS.name())
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
                                .withOrigin(true)
                                .withId(UUIDGenerator.graphIdGenerator("node"))
                                .withType(EXEC.name())
                                .withName("Start Service")
                                .addProperty("commandPath", "$WINGS_RUNTIME_PATH")
                                .addProperty("commandString", "echo \"service start script should be added here\"")
                                .build(),
                      aGraphNode()
                          .withId(UUIDGenerator.graphIdGenerator("node"))
                          .withName("Service Running")
                          .withType(PROCESS_CHECK_RUNNING.name())
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
                                .withOrigin(true)
                                .withId(UUIDGenerator.graphIdGenerator("node"))
                                .withType(EXEC.name())
                                .withName("Stop Service")
                                .addProperty("commandPath", "$WINGS_RUNTIME_PATH")
                                .addProperty("commandString", "echo \"service stop script should be added here\"")
                                .build(),
                      aGraphNode()
                          .withId(UUIDGenerator.graphIdGenerator("node"))
                          .withName("Service Stopped")
                          .withType(PROCESS_CHECK_STOPPED.name())
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
                                       .withOrigin(true)
                                       .withId(UUIDGenerator.graphIdGenerator("node"))
                                       .withName("Setup Runtime Paths")
                                       .withType(SETUP_ENV.name())
                                       .addProperty("commandString", SetupEnvCommandUnit.setupEnvCommandString)
                                       .build(),
                             aGraphNode()
                                 .withId(UUIDGenerator.graphIdGenerator("node"))
                                 .withName("Copy Artifact")
                                 .withType(SCP.name())
                                 .addProperty("fileCategory", ScpFileCategory.ARTIFACTS)
                                 .addProperty("destinationDirectoryPath", "$WINGS_RUNTIME_PATH")
                                 .build(),
                             aGraphNode()
                                 .withId(UUIDGenerator.graphIdGenerator("node"))
                                 .withName("Expand Artifact")
                                 .withType(EXEC.name())
                                 .addProperty("commandPath", "$WINGS_RUNTIME_PATH")
                                 .addProperty("commandString", "unzip \"$ARTIFACT_FILE_NAME\"")
                                 .build(),
                             aGraphNode()
                                 .withId(UUIDGenerator.graphIdGenerator("node"))
                                 .withName("Copy Configs")
                                 .withType(COPY_CONFIGS.name())
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
                                                     .withOrigin(true)
                                                     .withId(UUIDGenerator.graphIdGenerator("node"))
                                                     .withName("Setup ECS Service")
                                                     .withType(ECS_SETUP.name())
                                                     .build())
                                       .buildPipeline())
                        .build(),
          aCommand()
              .withCommandType(CommandType.SETUP)
              .withGraph(aGraph()
                             .withGraphName("Setup Replication Controller")
                             .addNodes(aGraphNode()
                                           .withOrigin(true)
                                           .withId(UUIDGenerator.graphIdGenerator("node"))
                                           .withName("Setup Kubernetes Replication Controller")
                                           .withType(KUBERNETES_SETUP.name())
                                           .build())
                             .buildPipeline())
              .build(),
          aCommand()
              .withCommandType(CommandType.RESIZE)
              .withGraph(aGraph()
                             .withGraphName("Resize Service Cluster")
                             .addNodes(aGraphNode()
                                           .withOrigin(true)
                                           .withId(UUIDGenerator.graphIdGenerator("node"))
                                           .withName("Resize ECS Service")
                                           .withType(RESIZE.name())
                                           .build())
                             .buildPipeline())
              .build(),
          aCommand()
              .withCommandType(CommandType.RESIZE)
              .withGraph(aGraph()
                             .withGraphName("Resize Replication Controller")
                             .addNodes(aGraphNode()
                                           .withOrigin(true)
                                           .withId(UUIDGenerator.graphIdGenerator("node"))
                                           .withName("Resize Kubernetes Replication Controller")
                                           .withType(RESIZE_KUBERNETES.name())
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
                                .withOrigin(true)
                                .withId(UUIDGenerator.graphIdGenerator("node"))
                                .withType(EXEC.name())
                                .withName("Start Service")
                                .addProperty("commandPath", "$WINGS_RUNTIME_PATH")
                                .addProperty("commandString", "echo \"service start script should be added here\"")
                                .build(),
                      aGraphNode()
                          .withId(UUIDGenerator.graphIdGenerator("node"))
                          .withName("Service Running")
                          .withType(PROCESS_CHECK_RUNNING.name())
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
                                .withOrigin(true)
                                .withId(UUIDGenerator.graphIdGenerator("node"))
                                .withType(EXEC.name())
                                .withName("Stop Service")
                                .addProperty("commandPath", "$WINGS_RUNTIME_PATH")
                                .addProperty("commandString", "echo \"service stop script should be added here\"")
                                .build(),
                      aGraphNode()
                          .withId(UUIDGenerator.graphIdGenerator("node"))
                          .withName("Service Stopped")
                          .withType(PROCESS_CHECK_STOPPED.name())
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
                                       .withOrigin(true)
                                       .withId(UUIDGenerator.graphIdGenerator("node"))
                                       .withName("Setup Runtime Paths")
                                       .withType(SETUP_ENV.name())
                                       .addProperty("commandString", SetupEnvCommandUnit.setupEnvCommandString)
                                       .build(),
                             aGraphNode()
                                 .withId(UUIDGenerator.graphIdGenerator("node"))
                                 .withName("Copy Artifact")
                                 .withType(SCP.name())
                                 .addProperty("fileCategory", ScpFileCategory.ARTIFACTS)
                                 .addProperty("destinationDirectoryPath", "$WINGS_RUNTIME_PATH")
                                 .build(),
                             aGraphNode()
                                 .withId(UUIDGenerator.graphIdGenerator("node"))
                                 .withName("Copy Configs")
                                 .withType(COPY_CONFIGS.name())
                                 .addProperty("destinationParentPath", "$WINGS_RUNTIME_PATH")
                                 .build(),
                             aGraphNode()
                                 .withId(UUIDGenerator.graphIdGenerator("node"))
                                 .withName("Install")
                                 .withType(EXEC.name())
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
                                       .withOrigin(true)
                                       .withId(UUIDGenerator.graphIdGenerator("node"))
                                       .withName(Constants.AWS_LAMBDA_COMMAND_NAME)
                                       .withType(AWS_LAMBDA.name())
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
                                       .withOrigin(true)
                                       .withId(UUIDGenerator.graphIdGenerator("node"))
                                       .withName("Amazon Code Deploy")
                                       .withType(CODE_DEPLOY.name())
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
                                       .withOrigin(true)
                                       .withId(UUIDGenerator.graphIdGenerator("node"))
                                       .withName(PCF_SETUP)
                                       .withType(PCF.name())
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
                                       .withOrigin(true)
                                       .withId(UUIDGenerator.graphIdGenerator("node"))
                                       .withName(PCF_RESIZE)
                                       .withType(PCF.name())
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
                                       .withOrigin(true)
                                       .withId(UUIDGenerator.graphIdGenerator("node"))
                                       .withName(AMI_SETUP_COMMAND_NAME)
                                       .withType(AWS_AMI.name())
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
                                       .withOrigin(true)
                                       .withId(UUIDGenerator.graphIdGenerator("node"))
                                       .withName(ASG_COMMAND_NAME)
                                       .withType(AWS_AMI.name())
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
          .withGraph(aGraph()
                         .withGraphName("Install")
                         .addNodes(aGraphNode()
                                       .withOrigin(true)
                                       .withId(UUIDGenerator.graphIdGenerator("node"))
                                       .withType(EXEC.name())
                                       .withName(PsScript.DownloadArtifacts.getDisplayName())
                                       .addProperty("scriptType", "POWERSHELL")
                                       .addProperty("commandString", psScriptMap.get(PsScript.DownloadArtifacts))
                                       .build(),
                             aGraphNode()
                                 .withOrigin(true)
                                 .withId(UUIDGenerator.graphIdGenerator("node"))
                                 .withType(EXEC.name())
                                 .withName(PsScript.ExpandArtifacts.getDisplayName())
                                 .addProperty("scriptType", "POWERSHELL")
                                 .addProperty("commandString", psScriptMap.get(PsScript.ExpandArtifacts))
                                 .build(),
                             aGraphNode()
                                 .withOrigin(true)
                                 .withId(UUIDGenerator.graphIdGenerator("node"))
                                 .withType(EXEC.name())
                                 .withName(PsScript.CreateIISAppPool.getDisplayName())
                                 .addProperty("scriptType", "POWERSHELL")
                                 .addProperty("commandString", psScriptMap.get(PsScript.CreateIISAppPool))
                                 .build(),
                             aGraphNode()
                                 .withOrigin(true)
                                 .withId(UUIDGenerator.graphIdGenerator("node"))
                                 .withType(EXEC.name())
                                 .withName(PsScript.CreateIISWebsite.getDisplayName())
                                 .addProperty("scriptType", "POWERSHELL")
                                 .addProperty("commandString", psScriptMap.get(PsScript.CreateIISWebsite))
                                 .build())
                         .buildPipeline())
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
          .withGraph(aGraph()
                         .withGraphName("Install")
                         .addNodes(aGraphNode()
                                       .withOrigin(true)
                                       .withId(UUIDGenerator.graphIdGenerator("node"))
                                       .withType(EXEC.name())
                                       .withName(PsScript.DownloadArtifacts.getDisplayName())
                                       .addProperty("scriptType", "POWERSHELL")
                                       .addProperty("commandString", psScriptMap.get(PsScript.DownloadArtifacts))
                                       .build(),
                             aGraphNode()
                                 .withOrigin(true)
                                 .withId(UUIDGenerator.graphIdGenerator("node"))
                                 .withType(EXEC.name())
                                 .withName(PsScript.ExpandArtifacts.getDisplayName())
                                 .addProperty("scriptType", "POWERSHELL")
                                 .addProperty("commandString", psScriptMap.get(PsScript.ExpandArtifacts))
                                 .build(),
                             aGraphNode()
                                 .withOrigin(true)
                                 .withId(UUIDGenerator.graphIdGenerator("node"))
                                 .withType(EXEC.name())
                                 .withName(PsScript.CreateIISApplication.getDisplayName())
                                 .addProperty("scriptType", "POWERSHELL")
                                 .addProperty("commandString", psScriptMap.get(PsScript.CreateIISApplication))
                                 .build())
                         .buildPipeline())
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
          .withGraph(aGraph()
                         .withGraphName("Install")
                         .addNodes(aGraphNode()
                                       .withOrigin(true)
                                       .withId(UUIDGenerator.graphIdGenerator("node"))
                                       .withType(EXEC.name())
                                       .withName(PsScript.DownloadArtifacts.getDisplayName())
                                       .addProperty("scriptType", "POWERSHELL")
                                       .addProperty("commandString", psScriptMap.get(PsScript.DownloadArtifacts))
                                       .build(),
                             aGraphNode()
                                 .withOrigin(true)
                                 .withId(UUIDGenerator.graphIdGenerator("node"))
                                 .withType(EXEC.name())
                                 .withName(PsScript.ExpandArtifacts.getDisplayName())
                                 .addProperty("scriptType", "POWERSHELL")
                                 .addProperty("commandString", psScriptMap.get(PsScript.ExpandArtifacts))
                                 .build(),
                             aGraphNode()
                                 .withOrigin(true)
                                 .withId(UUIDGenerator.graphIdGenerator("node"))
                                 .withType(EXEC.name())
                                 .withName(PsScript.CreateIISVirtualDirectory.getDisplayName())
                                 .addProperty("scriptType", "POWERSHELL")
                                 .addProperty("commandString", psScriptMap.get(PsScript.CreateIISVirtualDirectory))
                                 .build())
                         .buildPipeline())
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
