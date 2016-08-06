package software.wings.utils;

import static com.google.common.collect.ImmutableMap.of;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static software.wings.beans.Graph.Builder.aGraph;
import static software.wings.beans.Graph.Link.Builder.aLink;
import static software.wings.beans.Graph.Node.Builder.aNode;
import static software.wings.beans.command.CommandUnitType.COMMAND;
import static software.wings.beans.command.CommandUnitType.COPY_CONFIGS;
import static software.wings.beans.command.CommandUnitType.EXEC;
import static software.wings.beans.command.CommandUnitType.PORT_CHECK_CLEARED;
import static software.wings.beans.command.CommandUnitType.PORT_CHECK_LISTENING;
import static software.wings.beans.command.CommandUnitType.PROCESS_CHECK_RUNNING;
import static software.wings.beans.command.CommandUnitType.PROCESS_CHECK_STOPPED;
import static software.wings.beans.command.CommandUnitType.SCP;
import static software.wings.beans.command.CommandUnitType.SETUP_ENV;
import static software.wings.sm.TransitionType.SUCCESS;

import software.wings.beans.Graph;
import software.wings.beans.command.ScpCommandUnit.ScpFileCategory;

import java.util.List;
import java.util.UUID;

/**
 * Created by anubhaw on 6/9/16.
 */
public class DefaultCommands {
  /**
   * Graph id generator string.
   *
   * @param prefix the prefix
   * @return the string
   */
  public static String graphIdGenerator(String prefix) {
    return prefix + "_" + UUID.randomUUID().toString();
  }

  /**
   * Gets start command graph.
   *
   * @return the start command graph
   */
  public static Graph getStartCommandGraph() {
    List<String> nodes = asList(graphIdGenerator("node"), graphIdGenerator("node"), graphIdGenerator("node"));

    return aGraph()
        .withGraphName("Start")
        .addNodes(aNode()
                      .withOrigin(true)
                      .withX(50)
                      .withY(50)
                      .withId(nodes.get(0))
                      .withType(EXEC.name())
                      .withName("Start Service")
                      .addProperty("commandPath", "$WINGS_RUNTIME_PATH/tomcat/bin")
                      .addProperty("commandString", "./startup.sh")
                      .addProperty("tailFiles", true)
                      .addProperty("tailPatterns",
                          singletonList(of("filePath", "$WINGS_RUNTIME_PATH/tomcat/logs/catalina.out", "pattern",
                              "Server startup in")))
                      .build(),
            aNode()
                .withX(200)
                .withY(50)
                .withId(nodes.get(1))
                .withName("Process Check Running")
                .withType(PROCESS_CHECK_RUNNING.name())
                .addProperty("commandString", "set -x\npgrep -f \"\\-Dcatalina.home=$WINGS_RUNTIME_PATH/tomcat\"")
                .build(),
            aNode()
                .withX(350)
                .withY(50)
                .withId(nodes.get(2))
                .withType(PORT_CHECK_LISTENING.name())
                .withName("Port Check Listening")
                .addProperty("commandString", "set -x\nnc -v -z -w 5 localhost 8080")
                .build())
        .addLinks(aLink()
                      .withFrom(nodes.get(0))
                      .withTo(nodes.get(1))
                      .withType(SUCCESS.name())
                      .withId(graphIdGenerator("link"))
                      .build(),
            aLink()
                .withFrom(nodes.get(1))
                .withTo(nodes.get(2))
                .withType(SUCCESS.name())
                .withId(graphIdGenerator("link"))
                .build())
        .build();
  }

  /**
   * Gets stop command graph.
   *
   * @return the stop command graph
   */
  public static Graph getStopCommandGraph() {
    List<String> nodes = asList(graphIdGenerator("node"), graphIdGenerator("node"), graphIdGenerator("node"));
    return aGraph()
        .withGraphName("Stop")
        .addNodes(aNode()
                      .withOrigin(true)
                      .withX(50)
                      .withY(50)
                      .withId(nodes.get(0))
                      .withType(EXEC.name())
                      .withName("Stop Service")
                      .addProperty("commandPath", "$WINGS_RUNTIME_PATH/tomcat/bin")
                      .addProperty("commandString", "[[ -f ./shutdown.sh ]] && ./shutdown.sh  || true")
                      .build(),
            aNode()
                .withX(200)
                .withY(50)
                .withId(nodes.get(1))
                .withName("Process Check Stopped")
                .withType(PROCESS_CHECK_STOPPED.name())
                .addProperty("commandString",
                    "set -x\npgrep -f \"\\-Dcatalina.home=$WINGS_RUNTIME_PATH/tomcat\"\nrc=$?\nif [ \"$rc\" -eq 0 ]\nthen\nexit 1\nfi")
                .build(),
            aNode()
                .withX(350)
                .withY(50)
                .withId(nodes.get(2))
                .withType(PORT_CHECK_CLEARED.name())
                .withName("Port Check Cleared")
                .addProperty("commandString",
                    "set -x\nnc -v -z -w 5 localhost 8080\nrc=$?\nif [ \"$rc\" -eq 0 ]\nthen\nexit 1\nfi")
                .build())
        .addLinks(aLink()
                      .withFrom(nodes.get(0))
                      .withTo(nodes.get(1))
                      .withType(SUCCESS.name())
                      .withId(graphIdGenerator("link"))
                      .build(),
            aLink()
                .withFrom(nodes.get(1))
                .withTo(nodes.get(2))
                .withType(SUCCESS.name())
                .withId(graphIdGenerator("link"))
                .build())
        .build();
  }

  /**
   * Gets install command graph.
   *
   * @return the install command graph
   */
  public static Graph getInstallCommandGraph() {
    List<String> nodes = asList(graphIdGenerator("node"), graphIdGenerator("node"), graphIdGenerator("node"),
        graphIdGenerator("node"), graphIdGenerator("node"), graphIdGenerator("node"), graphIdGenerator("node"));

    return aGraph()
        .withGraphName("Install")
        .addNodes(
            aNode()
                .withOrigin(true)
                .withX(50)
                .withY(50)
                .withId(nodes.get(0))
                .withName("Setup Runtime Paths")
                .withType(SETUP_ENV.name())
                .addProperty("commandString",
                    "mkdir -p \"$WINGS_RUNTIME_PATH\"\nmkdir -p \"$WINGS_BACKUP_PATH\"\nmkdir -p \"$WINGS_STAGING_PATH\"")
                .build(),
            aNode()
                .withX(200)
                .withY(50)
                .withId(nodes.get(1))
                .withName("Stop")
                .withType(COMMAND.name())
                .addProperty("referenceId", "Stop")
                .build(),
            aNode()
                .withX(350)
                .withY(50)
                .withId(nodes.get(2))
                .withName("Copy App Stack")
                .withType(SCP.name())
                .addProperty("destinationDirectoryPath", "$WINGS_RUNTIME_PATH")
                .addProperty("fileCategory", ScpFileCategory.APPLICATION_STACK)
                .build(),
            aNode()
                .withX(500)
                .withY(50)
                .withId(nodes.get(3))
                .withName("Expand App Server")
                .withType(EXEC.name())
                .addProperty("commandPath", "$WINGS_RUNTIME_PATH")
                .addProperty("commandString",
                    "rm -rf tomcat\ntar -xvzf apache-tomcat-7.0.70.tar.gz\nmv apache-tomcat-7.0.70 tomcat")
                .build(),
            aNode()
                .withX(650)
                .withY(50)
                .withId(nodes.get(4))
                .withName("Copy Artifact")
                .withType(SCP.name())
                .addProperty("fileCategory", ScpFileCategory.ARTIFACTS)
                .addProperty("destinationDirectoryPath", "$WINGS_RUNTIME_PATH/tomcat/webapps")
                .build(),
            aNode()
                .withX(800)
                .withY(50)
                .withId(nodes.get(5))
                .withName("Copy Configs")
                .withType(COPY_CONFIGS.name())
                .addProperty("destinationParentPath", "$WINGS_RUNTIME_PATH")
                .build(),
            aNode()
                .withX(950)
                .withY(50)
                .withId(nodes.get(6))
                .withName("Start")
                .withType(COMMAND.name())
                .addProperty("referenceId", "Start")
                .build())
        .addLinks(aLink()
                      .withFrom(nodes.get(0))
                      .withTo(nodes.get(1))
                      .withType(SUCCESS.name())
                      .withId(graphIdGenerator("link"))
                      .build(),
            aLink()
                .withFrom(nodes.get(1))
                .withTo(nodes.get(2))
                .withType(SUCCESS.name())
                .withId(graphIdGenerator("link"))
                .build(),
            aLink()
                .withFrom(nodes.get(2))
                .withTo(nodes.get(3))
                .withType(SUCCESS.name())
                .withId(graphIdGenerator("link"))
                .build(),
            aLink()
                .withFrom(nodes.get(3))
                .withTo(nodes.get(4))
                .withType(SUCCESS.name())
                .withId(graphIdGenerator("link"))
                .build(),
            aLink()
                .withFrom(nodes.get(4))
                .withTo(nodes.get(5))
                .withType(SUCCESS.name())
                .withId(graphIdGenerator("link"))
                .build(),
            aLink()
                .withFrom(nodes.get(5))
                .withTo(nodes.get(6))
                .withType(SUCCESS.name())
                .withId(graphIdGenerator("link"))
                .build())
        .build();
  }
}
