/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.utils;

import static software.wings.beans.Graph.Builder.aGraph;
import static software.wings.beans.Graph.graphIdGenerator;
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

import static com.google.common.collect.ImmutableMap.of;
import static java.util.Collections.singletonList;

import software.wings.beans.AppContainer;
import software.wings.beans.Graph;
import software.wings.beans.GraphNode;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandType;
import software.wings.beans.command.ScpCommandUnit.ScpFileCategory;
import software.wings.beans.command.SetupEnvCommandUnit;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
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
              .addNodes(
                  GraphNode.builder()
                      .origin(true)
                      .id(graphIdGenerator("node"))
                      .type(EXEC.name())
                      .name("Start Service")
                      .properties(ImmutableMap.<String, Object>builder()
                                      .put("commandPath", "$WINGS_RUNTIME_PATH/tomcat/bin")
                                      .put("commandString", "./startup.sh")
                                      .put("tailFiles", true)
                                      .put("tailPatterns",
                                          singletonList(of("filePath", "$WINGS_RUNTIME_PATH/tomcat/logs/catalina.out",
                                              "pattern", "Server startup in")))
                                      .put("commandType", CommandType.START)
                                      .build())
                      .build(),
                  GraphNode.builder()
                      .id(graphIdGenerator("node"))
                      .name("Process Running")
                      .type(PROCESS_CHECK_RUNNING.name())
                      .properties(ImmutableMap.<String, Object>builder()
                                      .put("commandString",
                                          "i=0\n"
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
                                      .build())
                      .build(),
                  GraphNode.builder()
                      .id(graphIdGenerator("node"))
                      .type(PORT_CHECK_LISTENING.name())
                      .name("Port Listening")
                      .properties(
                          ImmutableMap.<String, Object>builder()
                              .put("commandString",
                                  "server_xml=\"$WINGS_RUNTIME_PATH/tomcat/conf/server.xml\"\n"
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
                      .build())
              .buildPipeline();
      return aCommand().withCommandType(CommandType.START).withGraph(graph).build();
    }

    @Override
    protected Command getStopCommand(ArtifactType artifactType) {
      Graph graph =
          aGraph()
              .withGraphName("Stop")
              .addNodes(GraphNode.builder()
                            .origin(true)
                            .id(graphIdGenerator("node"))
                            .type(EXEC.name())
                            .name("Stop Service")
                            .properties(ImmutableMap.<String, Object>builder()
                                            .put("commandPath", "$WINGS_RUNTIME_PATH/tomcat/bin")
                                            .put("commandString", "[ -f ./shutdown.sh ] && ./shutdown.sh  || true")
                                            .put("commandType", CommandType.STOP)
                                            .build())
                            .build(),
                  GraphNode.builder()
                      .id(graphIdGenerator("node"))
                      .name("Process Stopped")
                      .type(PROCESS_CHECK_STOPPED.name())
                      .properties(ImmutableMap.<String, Object>builder()
                                      .put("commandString",
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
                                      .build())
                      .build(),
                  GraphNode.builder()
                      .id(graphIdGenerator("node"))
                      .type(PORT_CHECK_CLEARED.name())
                      .name("Port Cleared")
                      .properties(
                          ImmutableMap.<String, Object>builder()
                              .put("commandString",
                                  "server_xml=\"$WINGS_RUNTIME_PATH/tomcat/conf/server.xml\"\n"
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
                      .build())
              .buildPipeline();
      return aCommand().withCommandType(CommandType.STOP).withGraph(graph).build();
    }

    @Override
    protected Command getInstallCommand(ArtifactType artifactType, AppContainer appContainer) {
      Graph graph =
          aGraph()
              .withGraphName("Install")
              .addNodes(getSetupRuntimePathsNode(), getStopNode(), getCopyAppStackNode(),
                  GraphNode.builder()
                      .id(graphIdGenerator("node"))
                      .name("Expand App Stack")
                      .type(EXEC.name())
                      .properties(
                          ImmutableMap.<String, Object>builder()
                              .put("commandPath", "$WINGS_RUNTIME_PATH")
                              .put("commandString",
                                  "rm -rf tomcat\n"
                                      + (".".equals(appContainer.getStackRootDirectory())
                                              ? ""
                                              : "rm -rf " + appContainer.getStackRootDirectory() + "\n")
                                      + appContainer.getFileType().getUnarchiveCommand(
                                          appContainer.getFileName(), appContainer.getStackRootDirectory(), "tomcat")
                                      + "\nchmod +x tomcat/bin/*")
                              .build())
                      .build(),
                  GraphNode.builder()
                      .id(graphIdGenerator("node"))
                      .name("Copy Artifact")
                      .type(SCP.name())
                      .properties(ImmutableMap.<String, Object>builder()
                                      .put("fileCategory", ScpFileCategory.ARTIFACTS)
                                      .put("destinationDirectoryPath", "$WINGS_RUNTIME_PATH/tomcat/webapps")
                                      .build())
                      .build(),
                  getCopyConfigsNode(), getStartNode())
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
              .addNodes(GraphNode.builder()
                            .origin(true)
                            .id(graphIdGenerator("node"))
                            .type(EXEC.name())
                            .name("Start Service")
                            .properties(ImmutableMap.<String, Object>builder()
                                            .put("commandPath", "$WINGS_RUNTIME_PATH/jboss/bin")
                                            .put("commandString", "nohup ./standalone.sh &")
                                            .put("tailFiles", true)
                                            .put("tailPatterns",
                                                singletonList(of("filePath", "nohup.out", "pattern", "started in")))
                                            .build())
                            .build(),
                  GraphNode.builder()
                      .id(graphIdGenerator("node"))
                      .name("Process Running")
                      .type(PROCESS_CHECK_RUNNING.name())
                      .properties(ImmutableMap.<String, Object>builder()
                                      .put("commandString",
                                          "i=0\n"
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
                                      .build())
                      .build(),
                  GraphNode.builder()
                      .id(graphIdGenerator("node"))
                      .type(PORT_CHECK_LISTENING.name())
                      .name("Port Listening")
                      .properties(
                          ImmutableMap.<String, Object>builder()
                              .put("commandString",
                                  "standalone_xml=\"$WINGS_RUNTIME_PATH/jboss/standalone/configuration/standalone.xml\"\n"
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
                      .build())
              .buildPipeline();
      return aCommand().withCommandType(CommandType.START).withGraph(graph).build();
    }

    @Override
    protected Command getStopCommand(ArtifactType artifactType) {
      Graph graph =
          aGraph()
              .withGraphName("Stop")
              .addNodes(
                  GraphNode.builder()
                      .origin(true)
                      .id(graphIdGenerator("node"))
                      .type(EXEC.name())
                      .name("Stop Service")
                      .properties(ImmutableMap.<String, Object>builder()
                                      .put("commandPath", "$WINGS_RUNTIME_PATH/jboss/bin")
                                      .put("commandString",
                                          "pgrep -f \"\\-Djboss.home.dir=$WINGS_RUNTIME_PATH/jboss\" | xargs kill")
                                      .build())
                      .build(),
                  GraphNode.builder()
                      .id(graphIdGenerator("node"))
                      .name("Process Stopped")
                      .type(PROCESS_CHECK_STOPPED.name())
                      .properties(ImmutableMap.<String, Object>builder()
                                      .put("commandString",
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
                                      .build())
                      .build(),
                  GraphNode.builder()
                      .id(graphIdGenerator("node"))
                      .type(PORT_CHECK_CLEARED.name())
                      .name("Port Cleared")
                      .properties(
                          ImmutableMap.<String, Object>builder()
                              .put("commandString",
                                  "standalone_xml=\"$WINGS_RUNTIME_PATH/jboss/standalone/configuration/standalone.xml\"\n"
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
                      .build())
              .buildPipeline();
      return aCommand().withCommandType(CommandType.STOP).withGraph(graph).build();
    }

    @Override
    protected Command getInstallCommand(ArtifactType artifactType, AppContainer appContainer) {
      Graph graph =
          aGraph()
              .withGraphName("Install")
              .addNodes(getSetupRuntimePathsNode(), getStopNode(), getCopyAppStackNode(),
                  GraphNode.builder()
                      .id(graphIdGenerator("node"))
                      .name("Expand App Stack")
                      .type(EXEC.name())
                      .properties(
                          ImmutableMap.<String, Object>builder()
                              .put("commandPath", "$WINGS_RUNTIME_PATH")
                              .put("commandString",
                                  "rm -rf jboss\n"
                                      + (".".equals(appContainer.getStackRootDirectory())
                                              ? ""
                                              : "rm -rf " + appContainer.getStackRootDirectory() + "\n")
                                      + appContainer.getFileType().getUnarchiveCommand(
                                          appContainer.getFileName(), appContainer.getStackRootDirectory(), "jboss")
                                      + "\nchmod +x jboss/bin/*")
                              .build())
                      .build(),
                  GraphNode.builder()
                      .id(graphIdGenerator("node"))
                      .name("Copy Artifact")
                      .type(SCP.name())
                      .properties(ImmutableMap.<String, Object>builder()
                                      .put("fileCategory", ScpFileCategory.ARTIFACTS)
                                      .put("destinationDirectoryPath", "$WINGS_RUNTIME_PATH")
                                      .build())
                      .build(),
                  GraphNode.builder()
                      .id(graphIdGenerator("node"))
                      .name("Expand Artifact")
                      .type(EXEC.name())
                      .properties(ImmutableMap.<String, Object>builder()
                                      .put("commandPath", "$WINGS_RUNTIME_PATH/jboss/standalone/deployments")
                                      .put("commandString",
                                          "mkdir -p $ARTIFACT_FILE_NAME\n"
                                              + "touch ${ARTIFACT_FILE_NAME}.dodeploy\n"
                                              + "cd $ARTIFACT_FILE_NAME\n"
                                              + "jar xvf \"$WINGS_RUNTIME_PATH/$ARTIFACT_FILE_NAME\"")
                                      .build())
                      .build(),
                  getCopyConfigsNode(), getStartNode())
              .buildPipeline();

      return aCommand().withCommandType(CommandType.INSTALL).withGraph(graph).build();
    }
  };

  private static GraphNode getStartNode() {
    return GraphNode.builder()
        .id(graphIdGenerator("node"))
        .name("Start")
        .type(COMMAND.name())
        .properties(ImmutableMap.<String, Object>builder().put("referenceId", "Start").build())
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

  private static GraphNode getCopyAppStackNode() {
    return GraphNode.builder()
        .id(graphIdGenerator("node"))
        .name("Copy App Stack")
        .type(SCP.name())
        .properties(ImmutableMap.<String, Object>builder()
                        .put("destinationDirectoryPath", "$WINGS_RUNTIME_PATH")
                        .put("fileCategory", ScpFileCategory.APPLICATION_STACK)
                        .build())
        .build();
  }

  private static GraphNode getStopNode() {
    return GraphNode.builder()
        .id(graphIdGenerator("node"))
        .name("Stop")
        .type(COMMAND.name())
        .properties(ImmutableMap.<String, Object>builder().put("referenceId", "Stop").build())
        .build();
  }

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
