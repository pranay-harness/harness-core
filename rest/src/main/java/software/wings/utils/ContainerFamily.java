package software.wings.utils;

import static com.google.common.collect.ImmutableMap.of;
import static io.harness.data.structure.UUIDGenerator.graphIdGenerator;
import static java.util.Collections.singletonList;
import static software.wings.beans.Graph.Builder.aGraph;
import static software.wings.beans.GraphNode.GraphNodeBuilder.aGraphNode;
import static software.wings.beans.command.Command.Builder.aCommand;
import static software.wings.beans.command.CommandUnitType.COMMAND;
import static software.wings.beans.command.CommandUnitType.COPY_CONFIGS;
import static software.wings.beans.command.CommandUnitType.EXEC;
import static software.wings.beans.command.CommandUnitType.PORT_CHECK_CLEARED;
import static software.wings.beans.command.CommandUnitType.PORT_CHECK_LISTENING;
import static software.wings.beans.command.CommandUnitType.PROCESS_CHECK_RUNNING;
import static software.wings.beans.command.CommandUnitType.PROCESS_CHECK_STOPPED;
import static software.wings.beans.command.CommandUnitType.SCP;
import static software.wings.beans.command.CommandUnitType.SETUP_ENV;

import com.google.common.collect.Lists;

import software.wings.beans.AppContainer;
import software.wings.beans.Graph;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandType;
import software.wings.beans.command.ScpCommandUnit.ScpFileCategory;
import software.wings.beans.command.SetupEnvCommandUnit;

import java.util.List;

/**
 * Created by peeyushaggarwal on 8/31/16.
 */
public enum ContainerFamily {
  /**
   * The constant TOMCAT.
   */
  TOMCAT {
    private static final long serialVersionUID = 2932493038229748527L;

    @Override
    public boolean isInternal() {
      return false;
    }

    @Override
    protected Command getStartCommand(ArtifactType artifactType) {
      Graph graph =
          aGraph()
              .withGraphName("Start")
              .addNodes(aGraphNode()
                            .withOrigin(true)
                            .withId(graphIdGenerator("node"))
                            .withType(EXEC.name())
                            .withName("Start Service")
                            .addProperty("commandPath", "$WINGS_RUNTIME_PATH/tomcat/bin")
                            .addProperty("commandString", "./startup.sh")
                            .addProperty("tailFiles", true)
                            .addProperty("tailPatterns",
                                singletonList(of("filePath", "$WINGS_RUNTIME_PATH/tomcat/logs/catalina.out", "pattern",
                                    "Server startup in")))
                            .addProperty("commandType", CommandType.START)
                            .build(),
                  aGraphNode()
                      .withId(graphIdGenerator("node"))
                      .withName("Process Running")
                      .withType(PROCESS_CHECK_RUNNING.name())
                      .addProperty("commandString",
                          "set -x\n"
                              + "i=0\n"
                              + "while [ \"$i\" -lt 30 ]\n"
                              + "do\n"
                              + "  pgrep -f \"\\-Dcatalina.home=$WINGS_RUNTIME_PATH/tomcat\"\n"
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
                      .build(),
                  aGraphNode()
                      .withId(graphIdGenerator("node"))
                      .withType(PORT_CHECK_LISTENING.name())
                      .withName("Port Listening")
                      .addProperty("commandString",
                          "set -x\n"
                              + "server_xml=\"$WINGS_RUNTIME_PATH/tomcat/conf/server.xml\"\n"
                              + "\n"
                              + "if [ -f \"$server_xml\" ]\n"
                              + "then\n"
                              + "port=$(grep \"<Connector[ ]*port=\\\"[0-9]*\\\"[ ]*protocol=\\\"HTTP/1.1\\\"\" \"$server_xml\" |cut -d '\"' -f2)\n"
                              + "nc -v -z -w 5 localhost $port\n"
                              + "else\n"
                              + " echo \"Tomcat config file(\"$server_xml\") does not exist.. port check failed.\"\n"
                              + " exit 1\n"
                              + "fi")
                      .build())
              .buildPipeline();
      return aCommand().withCommandType(CommandType.START).withGraph(graph).build();
    }

    @Override
    protected Command getStopCommand(ArtifactType artifactType) {
      Graph graph =
          aGraph()
              .withGraphName("Stop")
              .addNodes(aGraphNode()
                            .withOrigin(true)
                            .withId(graphIdGenerator("node"))
                            .withType(EXEC.name())
                            .withName("Stop Service")
                            .addProperty("commandPath", "$WINGS_RUNTIME_PATH/tomcat/bin")
                            .addProperty("commandString", "[ -f ./shutdown.sh ] && ./shutdown.sh  || true")
                            .addProperty("commandType", CommandType.STOP)
                            .build(),
                  aGraphNode()
                      .withId(graphIdGenerator("node"))
                      .withName("Process Stopped")
                      .withType(PROCESS_CHECK_STOPPED.name())
                      .addProperty("commandString",
                          "i=0\n"
                              + "while [ \"$i\" -lt 30 ]\n"
                              + "do\n"
                              + "  pgrep -f \"\\-Dcatalina.home=$WINGS_RUNTIME_PATH/tomcat\"\n"
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
                      .build(),
                  aGraphNode()
                      .withId(graphIdGenerator("node"))
                      .withType(PORT_CHECK_CLEARED.name())
                      .withName("Port Cleared")
                      .addProperty("commandString",
                          "set -x\n"
                              + "server_xml=\"$WINGS_RUNTIME_PATH/tomcat/conf/server.xml\"\n"
                              + "if [ -f \"$server_xml\" ]\n"
                              + "then\n"
                              + "port=$(grep \"<Connector[ ]*port=\\\"[0-9]*\\\"[ ]*protocol=\\\"HTTP/1.1\\\"\" \"$server_xml\" |cut -d '\"' -f2)\n"
                              + "nc -v -z -w 5 localhost $port\n"
                              + "rc=$?\n"
                              + "if [ \"$rc\" -eq 0 ]\n"
                              + "then\n"
                              + "exit 1\n"
                              + "fi\n"
                              + "else\n"
                              + " echo \"Tomcat config file(\"$server_xml\") does not exist.. skipping port check.\"\n"
                              + "fi")
                      .build())
              .buildPipeline();
      return aCommand().withCommandType(CommandType.STOP).withGraph(graph).build();
    }

    @Override
    protected Command getInstallCommand(ArtifactType artifactType, AppContainer appContainer) {
      Graph graph = aGraph()
                        .withGraphName("Install")
                        .addNodes(aGraphNode()
                                      .withOrigin(true)
                                      .withId(graphIdGenerator("node"))
                                      .withName("Setup Runtime Paths")
                                      .withType(SETUP_ENV.name())
                                      .addProperty("commandString", SetupEnvCommandUnit.setupEnvCommandString)
                                      .build(),
                            aGraphNode()
                                .withId(graphIdGenerator("node"))
                                .withName("Stop")
                                .withType(COMMAND.name())
                                .addProperty("referenceId", "Stop")
                                .build(),
                            aGraphNode()
                                .withId(graphIdGenerator("node"))
                                .withName("Copy App Stack")
                                .withType(SCP.name())
                                .addProperty("destinationDirectoryPath", "$WINGS_RUNTIME_PATH")
                                .addProperty("fileCategory", ScpFileCategory.APPLICATION_STACK)
                                .build(),
                            aGraphNode()
                                .withId(graphIdGenerator("node"))
                                .withName("Expand App Stack")
                                .withType(EXEC.name())
                                .addProperty("commandPath", "$WINGS_RUNTIME_PATH")
                                .addProperty("commandString",
                                    "rm -rf tomcat\n"
                                        + (".".equals(appContainer.getStackRootDirectory())
                                                  ? ""
                                                  : "rm -rf " + appContainer.getStackRootDirectory() + "\n")
                                        + appContainer.getFileType().getUnarchiveCommand(appContainer.getFileName(),
                                              appContainer.getStackRootDirectory(), "tomcat")
                                        + "\nchmod +x tomcat/bin/*")
                                .build(),
                            aGraphNode()
                                .withId(graphIdGenerator("node"))
                                .withName("Copy Artifact")
                                .withType(SCP.name())
                                .addProperty("fileCategory", ScpFileCategory.ARTIFACTS)
                                .addProperty("destinationDirectoryPath", "$WINGS_RUNTIME_PATH/tomcat/webapps")
                                .build(),
                            aGraphNode()
                                .withId(graphIdGenerator("node"))
                                .withName("Copy Configs")
                                .withType(COPY_CONFIGS.name())
                                .addProperty("destinationParentPath", "$WINGS_RUNTIME_PATH")
                                .build(),
                            aGraphNode()
                                .withId(graphIdGenerator("node"))
                                .withName("Start")
                                .withType(COMMAND.name())
                                .addProperty("referenceId", "Start")
                                .build())
                        .buildPipeline();
      return aCommand().withCommandType(CommandType.INSTALL).withGraph(graph).build();
    }

  },
  /**
   * The constant JBOSS.
   */
  JBOSS {
    private static final long serialVersionUID = 2932493038229748527L;

    @Override
    public boolean isInternal() {
      return false;
    }

    @Override
    protected Command getStartCommand(ArtifactType artifactType) {
      Graph graph =
          aGraph()
              .withGraphName("Start")
              .addNodes(
                  aGraphNode()
                      .withOrigin(true)
                      .withId(graphIdGenerator("node"))
                      .withType(EXEC.name())
                      .withName("Start Service")
                      .addProperty("commandPath", "$WINGS_RUNTIME_PATH/jboss/bin")
                      .addProperty("commandString", "nohup ./standalone.sh &")
                      .addProperty("tailFiles", true)
                      .addProperty("tailPatterns", singletonList(of("filePath", "nohup.out", "pattern", "started in")))
                      .build(),
                  aGraphNode()
                      .withId(graphIdGenerator("node"))
                      .withName("Process Running")
                      .withType(PROCESS_CHECK_RUNNING.name())
                      .addProperty("commandString",
                          "set -x\n"
                              + "i=0\n"
                              + "while [ \"$i\" -lt 30 ]\n"
                              + "do\n"
                              + "  pgrep -f \"\\-Djboss.home.dir=$WINGS_RUNTIME_PATH/jboss\"\n"
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
                      .build(),
                  aGraphNode()
                      .withId(graphIdGenerator("node"))
                      .withType(PORT_CHECK_LISTENING.name())
                      .withName("Port Listening")
                      .addProperty("commandString",
                          "set -x\n"
                              + "standalone_xml=\"$WINGS_RUNTIME_PATH/jboss/standalone/configuration/standalone.xml\"\n"
                              + "\n"
                              + "if [ -f \"$standalone_xml\" ]\n"
                              + "then\n"
                              + "port=$(grep \"<socket-binding name=\\\"http\\\" port=\\\"\\${jboss.http.port\" \"$standalone_xml\" | cut -d \":\" -f2 | cut -d \"}\" -f1)\n"
                              + "nc -v -z -w 5 localhost $port\n"
                              + "else\n"
                              + " echo \"JBoss config file(\"$standalone_xml\") does not exist.. port check failed.\"\n"
                              + " exit 1\n"
                              + "fi")
                      .build())
              .buildPipeline();
      return aCommand().withCommandType(CommandType.START).withGraph(graph).build();
    }

    @Override
    protected Command getStopCommand(ArtifactType artifactType) {
      Graph graph =
          aGraph()
              .withGraphName("Stop")
              .addNodes(aGraphNode()
                            .withOrigin(true)
                            .withId(graphIdGenerator("node"))
                            .withType(EXEC.name())
                            .withName("Stop Service")
                            .addProperty("commandPath", "$WINGS_RUNTIME_PATH/jboss/bin")
                            .addProperty("commandString",
                                "pgrep -f \"\\-Djboss.home.dir=$WINGS_RUNTIME_PATH/jboss\" | xargs kill")
                            .build(),
                  aGraphNode()
                      .withId(graphIdGenerator("node"))
                      .withName("Process Stopped")
                      .withType(PROCESS_CHECK_STOPPED.name())
                      .addProperty("commandString",
                          "i=0\n"
                              + "while [ \"$i\" -lt 30 ]\n"
                              + "do\n"
                              + "  pgrep -f \"\\-Djboss.home.dir=$WINGS_RUNTIME_PATH/jboss\"\n"
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
                      .build(),
                  aGraphNode()
                      .withId(graphIdGenerator("node"))
                      .withType(PORT_CHECK_CLEARED.name())
                      .withName("Port Cleared")
                      .addProperty("commandString",
                          "set -x\n"
                              + "standalone_xml=\"$WINGS_RUNTIME_PATH/jboss/standalone/configuration/standalone.xml\"\n"
                              + "if [ -f \"$standalone_xml\" ]\n"
                              + "then\n"
                              + "port=$(grep \"<socket-binding name=\\\"http\\\" port=\\\"\\${jboss.http.port\" \"$standalone_xml\" | cut -d \":\" -f2 | cut -d \"}\" -f1)\n"
                              + "nc -v -z -w 5 localhost $port\n"
                              + "rc=$?\n"
                              + "if [ \"$rc\" -eq 0 ]\n"
                              + "then\n"
                              + "exit 1\n"
                              + "fi\n"
                              + "else\n"
                              + " echo \"JBoss config file(\"$standalone_xml\") does not exist.. skipping port check.\"\n"
                              + "fi")
                      .build())
              .buildPipeline();
      return aCommand().withCommandType(CommandType.STOP).withGraph(graph).build();
    }

    @Override
    protected Command getInstallCommand(ArtifactType artifactType, AppContainer appContainer) {
      Graph graph = aGraph()
                        .withGraphName("Install")
                        .addNodes(aGraphNode()
                                      .withOrigin(true)
                                      .withId(graphIdGenerator("node"))
                                      .withName("Setup Runtime Paths")
                                      .withType(SETUP_ENV.name())
                                      .addProperty("commandString", SetupEnvCommandUnit.setupEnvCommandString)
                                      .build(),
                            aGraphNode()
                                .withId(graphIdGenerator("node"))
                                .withName("Stop")
                                .withType(COMMAND.name())
                                .addProperty("referenceId", "Stop")
                                .build(),
                            aGraphNode()
                                .withId(graphIdGenerator("node"))
                                .withName("Copy App Stack")
                                .withType(SCP.name())
                                .addProperty("destinationDirectoryPath", "$WINGS_RUNTIME_PATH")
                                .addProperty("fileCategory", ScpFileCategory.APPLICATION_STACK)
                                .build(),
                            aGraphNode()
                                .withId(graphIdGenerator("node"))
                                .withName("Expand App Stack")
                                .withType(EXEC.name())
                                .addProperty("commandPath", "$WINGS_RUNTIME_PATH")
                                .addProperty("commandString",
                                    "rm -rf jboss\n"
                                        + (".".equals(appContainer.getStackRootDirectory())
                                                  ? ""
                                                  : "rm -rf " + appContainer.getStackRootDirectory() + "\n")
                                        + appContainer.getFileType().getUnarchiveCommand(
                                              appContainer.getFileName(), appContainer.getStackRootDirectory(), "jboss")
                                        + "\nchmod +x jboss/bin/*")
                                .build(),
                            aGraphNode()
                                .withId(graphIdGenerator("node"))
                                .withName("Copy Artifact")
                                .withType(SCP.name())
                                .addProperty("fileCategory", ScpFileCategory.ARTIFACTS)
                                .addProperty("destinationDirectoryPath", "$WINGS_RUNTIME_PATH")
                                .build(),
                            aGraphNode()
                                .withId(graphIdGenerator("node"))
                                .withName("Expand Artifact")
                                .withType(EXEC.name())
                                .addProperty("commandPath", "$WINGS_RUNTIME_PATH/jboss/standalone/deployments")
                                .addProperty("commandString",
                                    "mkdir -p $ARTIFACT_FILE_NAME\n"
                                        + "touch ${ARTIFACT_FILE_NAME}.dodeploy\n"
                                        + "cd $ARTIFACT_FILE_NAME\n"
                                        + "jar xvf \"$WINGS_RUNTIME_PATH/$ARTIFACT_FILE_NAME\"")
                                .build(),
                            aGraphNode()
                                .withId(graphIdGenerator("node"))
                                .withName("Copy Configs")
                                .withType(COPY_CONFIGS.name())
                                .addProperty("destinationParentPath", "$WINGS_RUNTIME_PATH")
                                .build(),
                            aGraphNode()
                                .withId(graphIdGenerator("node"))
                                .withName("Start")
                                .withType(COMMAND.name())
                                .addProperty("referenceId", "Start")
                                .build())
                        .buildPipeline();
      return aCommand().withCommandType(CommandType.INSTALL).withGraph(graph).build();
    }
  };

  /**
   * Gets default commands.
   *
   * @param artifactType the artifact type
   * @param appContainer the app container
   * @return the default commands
   */
  public List<Command> getDefaultCommands(ArtifactType artifactType, AppContainer appContainer) {
    return Lists.newArrayList(
        getStartCommand(artifactType), getInstallCommand(artifactType, appContainer), getStopCommand(artifactType));
  }

  /**
   * Gets stop command graph.
   *
   * @param artifactType the artifact type
   * @return the stop command graph
   */
  protected abstract Command getStopCommand(ArtifactType artifactType);

  /**
   * Gets start command graph.
   *
   * @param artifactType the artifact type
   * @return the start command graph
   */
  protected abstract Command getStartCommand(ArtifactType artifactType);

  /**
   * Gets install command graph.
   *
   * @param artifactType the artifact type
   * @param appContainer the app container
   * @return the install command graph
   */
  protected abstract Command getInstallCommand(ArtifactType artifactType, AppContainer appContainer);

  public abstract boolean isInternal();
}
