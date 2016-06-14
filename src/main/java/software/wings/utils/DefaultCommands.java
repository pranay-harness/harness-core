package software.wings.utils;

import static java.util.Arrays.asList;
import static software.wings.beans.Graph.Builder.aGraph;
import static software.wings.beans.Graph.Link.Builder.aLink;
import static software.wings.beans.Graph.Node.Builder.aNode;
import static software.wings.beans.Graph.ORIGIN_STATE;
import static software.wings.sm.TransitionType.SUCCESS;

import software.wings.beans.Graph;

import java.util.List;
import java.util.UUID;

/**
 * Created by anubhaw on 6/9/16.
 */
public class DefaultCommands {
  public static String graphIdGenerator(String prefix) {
    return prefix + "_" + UUID.randomUUID().toString();
  }

  public static Graph getStartCommandGraph() {
    List<String> nodes = asList(graphIdGenerator("node"), graphIdGenerator("node"));

    return aGraph()
        .withGraphName("START")
        .addNodes(aNode().withId(nodes.get(0)).withType(ORIGIN_STATE).build(),
            aNode()
                .withId(nodes.get(1))
                .withType("EXEC")
                .withName("Start Service")
                .addProperty("commandPath", "")
                .addProperty("commandString", "bash start.sh")
                .build())
        .addLinks(aLink()
                      .withFrom(nodes.get(0))
                      .withTo(nodes.get(1))
                      .withType(SUCCESS.name())
                      .withId(graphIdGenerator("link"))
                      .build())
        .build();
  }

  public static Graph getStopCommandGraph() {
    List<String> nodes = asList(graphIdGenerator("node"), graphIdGenerator("node"));

    return aGraph()
        .withGraphName("STOP")
        .addNodes(aNode().withId(nodes.get(0)).withType(ORIGIN_STATE).build(),
            aNode()
                .withId(nodes.get(1))
                .withType("EXEC")
                .withName("Stop Service")
                .addProperty("commandPath", "")
                .addProperty("commandString", "bash stop.sh")
                .build())
        .addLinks(aLink()
                      .withFrom(nodes.get(0))
                      .withTo(nodes.get(1))
                      .withType(SUCCESS.name())
                      .withId(graphIdGenerator("link"))
                      .build())
        .build();
  }

  public static Graph getInstallCommandGraph() {
    List<String> nodes = asList(graphIdGenerator("node"), graphIdGenerator("node"), graphIdGenerator("node"),
        graphIdGenerator("node"), graphIdGenerator("node"));
    List<String> linkes =
        asList(graphIdGenerator("link"), graphIdGenerator("link"), graphIdGenerator("link"), graphIdGenerator("link"));

    return aGraph()
        .withGraphName("INSTALL")
        .addNodes(aNode().withId(nodes.get(0)).withType(ORIGIN_STATE).build(),
            aNode()
                .withId(nodes.get(1))
                .withName("STOP")
                .withType("COMMAND")
                .addProperty("referenceId", "STOP")
                .build(),
            aNode().withId(nodes.get(2)).withName("Copy Artifact").withType("COPY_ARTIFACT").build(),
            aNode()
                .withId(nodes.get(3))
                .withName("Expand App Server")
                .withType("EXEC")
                .addProperty("commandPath", "")
                .addProperty("commandString", "bash install.sh")
                .build(),
            aNode()
                .withId(nodes.get(4))
                .withName("START")
                .withType("COMMAND")
                .addProperty("referenceId", "START")
                .build())
        .addLinks(
            aLink().withFrom(nodes.get(0)).withTo(nodes.get(1)).withType(SUCCESS.name()).withId(linkes.get(0)).build(),
            aLink().withFrom(nodes.get(1)).withTo(nodes.get(2)).withType(SUCCESS.name()).withId(linkes.get(1)).build(),
            aLink().withFrom(nodes.get(2)).withTo(nodes.get(3)).withType(SUCCESS.name()).withId(linkes.get(2)).build(),
            aLink().withFrom(nodes.get(3)).withTo(nodes.get(4)).withType(SUCCESS.name()).withId(linkes.get(3)).build())
        .build();
  }
}
