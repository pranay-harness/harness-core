package software.wings.utils;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static software.wings.beans.Graph.Builder.aGraph;
import static software.wings.beans.Graph.Node.Builder.aNode;
import static software.wings.beans.command.CommandUnitType.COMMAND;
import static software.wings.beans.command.CommandUnitType.COPY_CONFIGS;
import static software.wings.beans.command.CommandUnitType.EXEC;
import static software.wings.beans.command.CommandUnitType.PROCESS_CHECK_RUNNING;
import static software.wings.beans.command.CommandUnitType.PROCESS_CHECK_STOPPED;
import static software.wings.beans.command.CommandUnitType.SCP;
import static software.wings.beans.command.CommandUnitType.SETUP_ENV;

import software.wings.beans.Graph;
import software.wings.beans.command.ScpCommandUnit.ScpFileCategory;
import software.wings.common.UUIDGenerator;

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
    public List<Graph> getDefaultCommands() {
      return asList(getStartCommandGraph(), getStopCommandGraph(), getInstallCommandGraph());
    }

    /**
     * Gets start command graph.
     *
     * @return the start command graph
     */
    private Graph getStartCommandGraph() {
      return aGraph()
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
                  .addProperty("commandString", "set -x\npgrep -f \"$WINGS_RUNTIME_PATH/$ARTIFACT_FILE_NAME\"")
                  .build())
          .buildPipeline();
    }

    /**
     * Gets stop command graph.
     *
     * @return the stop command graph
     */
    private Graph getStopCommandGraph() {
      return aGraph()
          .withGraphName("Stop")
          .addNodes(aNode()
                        .withOrigin(true)
                        .withX(50)
                        .withY(50)
                        .withId(UUIDGenerator.graphIdGenerator("node"))
                        .withType(EXEC.name())
                        .withName("Stop Service")
                        .addProperty("commandPath", "$WINGS_RUNTIME_PATH/tomcat/bin")
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
                      "set -x\npgrep -f \"$WINGS_RUNTIME_PATH/$ARTIFACT_FILE_NAME\"\nrc=$?\nif [ \"$rc\" -eq 0 ]\nthen\nexit 1\nfi")
                  .build())
          .buildPipeline();
    }

    /**
     * Gets install command graph.
     *
     * @return the install command graph
     */
    private Graph getInstallCommandGraph() {
      return aGraph()
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
          .buildPipeline();
    }
  }, /**
      * War artifact type.
      */
  WAR {
    public static final long serialVersionUID = 2932493038229748527L;

    @Override
    public List<Graph> getDefaultCommands() {
      return asList(
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
              .buildPipeline());
    }
  }, /**
      * Tar artifact type.
      */
  TAR {
    private static final long serialVersionUID = 2932493038229748527L;

    @Override
    public List<Graph> getDefaultCommands() {
      return asList(
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
              .buildPipeline());
    }
  }, /**
      * Zip artifact type.
      */
  ZIP {
    private static final long serialVersionUID = 2932493038229748527L;

    @Override
    public List<Graph> getDefaultCommands() {
      return asList(
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
              .buildPipeline());
    }
  }, /**
      * Other artifact type.
      */
  OTHER {
    private static final long serialVersionUID = 2932493038229748527L;

    @Override
    public List<Graph> getDefaultCommands() {
      return emptyList();
    }
  };

  /**
   * Gets default commands.
   *
   * @return the default commands
   */
  public abstract List<Graph> getDefaultCommands();
}
