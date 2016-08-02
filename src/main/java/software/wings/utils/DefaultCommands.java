package software.wings.utils;

import static java.util.Arrays.asList;
import static software.wings.beans.Graph.Builder.aGraph;
import static software.wings.beans.Graph.Link.Builder.aLink;
import static software.wings.beans.Graph.Node.Builder.aNode;
import static software.wings.beans.command.CommandUnitType.COMMAND;
import static software.wings.beans.command.CommandUnitType.EXEC;
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
    List<String> nodes = asList(graphIdGenerator("node"), graphIdGenerator("node"));

    return aGraph()
        .withGraphName("Start")
        .addNodes(aNode()
                      .withOrigin(true)
                      .withX(400)
                      .withY(200)
                      .withId(nodes.get(1))
                      .withType(EXEC.name())
                      .withName("Start Service")
                      .addProperty("commandPath", "tomcat/bin")
                      .addProperty("commandString", "./startup.sh")
                      .build())
        .build();
  }

  /**
   * Gets stop command graph.
   *
   * @return the stop command graph
   */
  public static Graph getStopCommandGraph() {
    List<String> nodes = asList(graphIdGenerator("node"), graphIdGenerator("node"));

    return aGraph()
        .withGraphName("Stop")
        .addNodes(aNode()
                      .withOrigin(true)
                      .withX(400)
                      .withY(200)
                      .withId(nodes.get(1))
                      .withType(EXEC.name())
                      .withName("Stop Service")
                      .addProperty("commandPath", "tomcat/bin")
                      .addProperty("commandString", "[[ -f ./shutdown.sh ]] && ./shutdown.sh  || true")
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
    List<String> linkes = asList(graphIdGenerator("link"), graphIdGenerator("link"), graphIdGenerator("link"),
        graphIdGenerator("link"), graphIdGenerator("link"), graphIdGenerator("link"), graphIdGenerator("link"));

    return aGraph()
        .withGraphName("Install")
        .addNodes(aNode()
                      .withOrigin(true)
                      .withX(200)
                      .withY(200)
                      .withId(nodes.get(1))
                      .withName("Setup Runtime Paths")
                      .withType(SETUP_ENV.name())
                      .build(),
            aNode()
                .withX(350)
                .withY(200)
                .withId(nodes.get(2))
                .withName("Stop")
                .withType(COMMAND.name())
                .addProperty("referenceId", "Stop")
                .build(),
            aNode()
                .withX(500)
                .withY(200)
                .withId(nodes.get(3))
                .withName("Copy App Stack")
                .withType(SCP.name())
                .addProperty("fileCategory", ScpFileCategory.APPLICATION_STACK)
                .build(),
            aNode()
                .withX(650)
                .withY(200)
                .withId(nodes.get(4))
                .withName("Expand App Server")
                .withType(EXEC.name())
                .addProperty("commandPath", "")
                .addProperty("commandString",
                    "rm -rf tomcat && tar -xvzf apache-tomcat-7.0.70.tar.gz && mv apache-tomcat-7.0.70 tomcat")
                .build(),
            aNode()
                .withX(800)
                .withY(200)
                .withId(nodes.get(5))
                .withName("Copy Artifact")
                .withType(SCP.name())
                .addProperty("fileCategory", ScpFileCategory.ARTIFACTS)
                .addProperty("relativeFilePath", "tomcat/webapps")
                .build(),
            aNode()
                .withX(950)
                .withY(200)
                .withId(nodes.get(6))
                .withName("Start")
                .withType(COMMAND.name())
                .addProperty("referenceId", "Start")
                .build())
        .addLinks(
            aLink().withFrom(nodes.get(1)).withTo(nodes.get(2)).withType(SUCCESS.name()).withId(linkes.get(1)).build(),
            aLink().withFrom(nodes.get(2)).withTo(nodes.get(3)).withType(SUCCESS.name()).withId(linkes.get(2)).build(),
            aLink().withFrom(nodes.get(3)).withTo(nodes.get(4)).withType(SUCCESS.name()).withId(linkes.get(3)).build(),
            aLink().withFrom(nodes.get(4)).withTo(nodes.get(5)).withType(SUCCESS.name()).withId(linkes.get(4)).build(),
            aLink().withFrom(nodes.get(5)).withTo(nodes.get(6)).withType(SUCCESS.name()).withId(linkes.get(5)).build())
        .build();
  }
}
