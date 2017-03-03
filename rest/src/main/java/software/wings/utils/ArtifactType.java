package software.wings.utils;

import software.wings.beans.command.Command;
import software.wings.beans.command.CommandType;
import software.wings.beans.command.ScpCommandUnit.ScpFileCategory;
import software.wings.common.UUIDGenerator;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static software.wings.beans.Graph.Builder.aGraph;
import static software.wings.beans.Graph.Node.Builder.aNode;
import static software.wings.beans.command.Command.Builder.aCommand;
import static software.wings.beans.command.CommandUnitType.COMMAND;
import static software.wings.beans.command.CommandUnitType.COPY_CONFIGS;
import static software.wings.beans.command.CommandUnitType.EXEC;
import static software.wings.beans.command.CommandUnitType.PROCESS_CHECK_RUNNING;
import static software.wings.beans.command.CommandUnitType.PROCESS_CHECK_STOPPED;
import static software.wings.beans.command.CommandUnitType.RESIZE;
import static software.wings.beans.command.CommandUnitType.SCP;
import static software.wings.beans.command.CommandUnitType.SETUP_ENV;

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
                  .addNodes(aNode()
                                .withOrigin(true)
                                .withX(50)
                                .withY(50)
                                .withId(UUIDGenerator.graphIdGenerator("node"))
                                .withType(EXEC.name())
                                .withName("Start Service")
                                .addProperty("commandPath", "$WINGS_RUNTIME_PATH")
                                .addProperty("commandString", "java -jar \"$WINGS_RUNTIME_PATH/$ARTIFACT_FILE_NAME\"")
                                .build(),
                      aNode()
                          .withX(200)
                          .withY(50)
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
                  .addNodes(aNode()
                                .withOrigin(true)
                                .withX(50)
                                .withY(50)
                                .withId(UUIDGenerator.graphIdGenerator("node"))
                                .withType(EXEC.name())
                                .withName("Stop Service")
                                .addProperty("commandPath", "$WINGS_RUNTIME_PATH")
                                .addProperty("commandString",
                                    "\npgrep -f \"$WINGS_RUNTIME_PATH/$ARTIFACT_FILE_NAME\" | xargs kill  || true")
                                .build(),
                      aNode()
                          .withX(200)
                          .withY(50)
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
          .withGraph(
              aGraph()
                  .withGraphName("Install")
                  .addNodes(
                      aNode()
                          .withOrigin(true)
                          .withX(50)
                          .withY(50)
                          .withId(UUIDGenerator.graphIdGenerator("node"))
                          .withName("Setup Runtime Paths")
                          .withType(SETUP_ENV.name())
                          .addProperty("commandString",
                              "mkdir -p \"$WINGS_RUNTIME_PATH\"\nmkdir -p \"$WINGS_BACKUP_PATH\"\nmkdir -p \"$WINGS_STAGING_PATH\"")
                          .build(),
                      aNode()
                          .withX(200)
                          .withY(50)
                          .withId(UUIDGenerator.graphIdGenerator("node"))
                          .withName("Stop")
                          .withType(COMMAND.name())
                          .addProperty("referenceId", "Stop")
                          .build(),
                      aNode()
                          .withX(350)
                          .withY(50)
                          .withId(UUIDGenerator.graphIdGenerator("node"))
                          .withName("Copy Artifact")
                          .withType(SCP.name())
                          .addProperty("fileCategory", ScpFileCategory.ARTIFACTS)
                          .addProperty("destinationDirectoryPath", "$WINGS_RUNTIME_PATH")
                          .build(),
                      aNode()
                          .withX(500)
                          .withY(50)
                          .withId(UUIDGenerator.graphIdGenerator("node"))
                          .withName("Copy Configs")
                          .withType(COPY_CONFIGS.name())
                          .addProperty("destinationParentPath", "$WINGS_RUNTIME_PATH")
                          .build(),
                      aNode()
                          .withX(650)
                          .withY(50)
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
    public List<Command> getDefaultCommands() {
      return asList(
          aCommand()
              .withCommandType(CommandType.INSTALL)
              .withGraph(
                  aGraph()
                      .withGraphName("Install")
                      .addNodes(
                          aNode()
                              .withOrigin(true)
                              .withX(50)
                              .withY(50)
                              .withId(UUIDGenerator.graphIdGenerator("node"))
                              .withName("Setup Runtime Paths")
                              .withType(SETUP_ENV.name())
                              .addProperty("commandString",
                                  "mkdir -p \"$WINGS_RUNTIME_PATH\"\nmkdir -p \"$WINGS_BACKUP_PATH\"\nmkdir -p \"$WINGS_STAGING_PATH\"")
                              .build(),
                          aNode()
                              .withX(200)
                              .withY(50)
                              .withId(UUIDGenerator.graphIdGenerator("node"))
                              .withName("Copy Artifact")
                              .withType(SCP.name())
                              .addProperty("fileCategory", ScpFileCategory.ARTIFACTS)
                              .addProperty("destinationDirectoryPath", "$WINGS_RUNTIME_PATH")
                              .build(),
                          aNode()
                              .withX(350)
                              .withY(50)
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
    public List<Command> getDefaultCommands() {
      return asList(
          aCommand()
              .withCommandType(CommandType.INSTALL)
              .withGraph(
                  aGraph()
                      .withGraphName("Install")
                      .addNodes(
                          aNode()
                              .withOrigin(true)
                              .withX(50)
                              .withY(50)
                              .withId(UUIDGenerator.graphIdGenerator("node"))
                              .withName("Setup Runtime Paths")
                              .withType(SETUP_ENV.name())
                              .addProperty("commandString",
                                  "mkdir -p \"$WINGS_RUNTIME_PATH\"\nmkdir -p \"$WINGS_BACKUP_PATH\"\nmkdir -p \"$WINGS_STAGING_PATH\"")
                              .build(),
                          aNode()
                              .withX(200)
                              .withY(50)
                              .withId(UUIDGenerator.graphIdGenerator("node"))
                              .withName("Copy Artifact")
                              .withType(SCP.name())
                              .addProperty("fileCategory", ScpFileCategory.ARTIFACTS)
                              .addProperty("destinationDirectoryPath", "$WINGS_RUNTIME_PATH")
                              .build(),
                          aNode()
                              .withX(350)
                              .withY(50)
                              .withId(UUIDGenerator.graphIdGenerator("node"))
                              .withName("Expand Artifact")
                              .withType(EXEC.name())
                              .addProperty("commandPath", "$WINGS_RUNTIME_PATH")
                              .addProperty("commandString", "tar -xvzf \"$ARTIFACT_FILE_NAME\"")
                              .build(),
                          aNode()
                              .withX(500)
                              .withY(50)
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
   * Zip artifact type.
   */
  ZIP {
    private static final long serialVersionUID = 2932493038229748527L;

    @Override
    public List<Command> getDefaultCommands() {
      return asList(
          aCommand()
              .withCommandType(CommandType.START)
              .withGraph(
                  aGraph()
                      .withGraphName("Install")
                      .addNodes(
                          aNode()
                              .withOrigin(true)
                              .withX(50)
                              .withY(50)
                              .withId(UUIDGenerator.graphIdGenerator("node"))
                              .withName("Setup Runtime Paths")
                              .withType(SETUP_ENV.name())
                              .addProperty("commandString",
                                  "mkdir -p \"$WINGS_RUNTIME_PATH\"\nmkdir -p \"$WINGS_BACKUP_PATH\"\nmkdir -p \"$WINGS_STAGING_PATH\"")
                              .build(),
                          aNode()
                              .withX(200)
                              .withY(50)
                              .withId(UUIDGenerator.graphIdGenerator("node"))
                              .withName("Copy Artifact")
                              .withType(SCP.name())
                              .addProperty("fileCategory", ScpFileCategory.ARTIFACTS)
                              .addProperty("destinationDirectoryPath", "$WINGS_RUNTIME_PATH")
                              .build(),
                          aNode()
                              .withX(350)
                              .withY(50)
                              .withId(UUIDGenerator.graphIdGenerator("node"))
                              .withName("Expand Artifact")
                              .withType(EXEC.name())
                              .addProperty("commandPath", "$WINGS_RUNTIME_PATH")
                              .addProperty("commandString", "unzip \"$ARTIFACT_FILE_NAME\"")
                              .build(),
                          aNode()
                              .withX(500)
                              .withY(50)
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
   * Docker artifact type.
   */
  DOCKER {
    private static final long serialVersionUID = 2932493038229748527L;

    @Override
    public List<Command> getDefaultCommands() {
      return asList(aCommand()
                        .withCommandType(CommandType.RESIZE)
                        .withGraph(aGraph()
                                       .withGraphName("Resize Service Cluster")
                                       .addNodes(aNode()
                                                     .withOrigin(true)
                                                     .withX(50)
                                                     .withY(50)
                                                     .withId(UUIDGenerator.graphIdGenerator("node"))
                                                     .withName("Resize ECS Service")
                                                     .withType(RESIZE.name())
                                                     .build())
                                       // TODO(brett): Handle kubernetes
                                       .buildPipeline())
                        .build());
    }
  },
  /**
   * Other artifact type.
   */
  OTHER {
    private static final long serialVersionUID = 2932493038229748527L;

    @Override
    public List<Command> getDefaultCommands() {
      return emptyList();
    }
  };

  /**
   * Gets default commands.
   *
   * @return the default commands
   */
  public abstract List<Command> getDefaultCommands();
}
