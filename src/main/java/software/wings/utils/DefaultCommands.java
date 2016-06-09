package software.wings.utils;

import static software.wings.beans.Graph.Builder.aGraph;
import static software.wings.beans.Graph.Link.Builder.aLink;
import static software.wings.beans.Graph.Node.Builder.aNode;
import static software.wings.beans.Graph.ORIGIN_STATE;
import static software.wings.sm.TransitionType.SUCCESS;

import software.wings.beans.Graph;

/**
 * Created by anubhaw on 6/9/16.
 */
public class DefaultCommands {
  public static final Graph START_COMMAND_GRAPH =
      aGraph()
          .withGraphName("START")
          .addNodes(aNode().withId(ORIGIN_STATE).withType(ORIGIN_STATE).build(),
              aNode()
                  .withId("n0")
                  .withType("EXEC")
                  .addProperty("commandPath", "/bin/")
                  .addProperty("commandString", "sh startup.sh")
                  .build())
          .addLinks(aLink().withFrom(ORIGIN_STATE).withTo("n0").withType(SUCCESS.name()).withId("l0").build())
          .build();

  public static final Graph STOP_COMMAND_GRAPH =
      aGraph()
          .withGraphName("STOP")
          .addNodes(aNode().withId(ORIGIN_STATE).withType(ORIGIN_STATE).build(),
              aNode()
                  .withId("n0")
                  .withType("EXEC")
                  .addProperty("commandPath", "/bin/")
                  .addProperty("commandString", "sh stop.sh")
                  .build())
          .addLinks(aLink().withFrom(ORIGIN_STATE).withTo("n0").withType(SUCCESS.name()).withId("l0").build())
          .build();

  public static final Graph INSTALL_COMMAND_GRAPH =
      aGraph()
          .withGraphName("INSTALL")
          .addNodes(aNode().withId(ORIGIN_STATE).withType(ORIGIN_STATE).build(),
              aNode().withId("n0").withType("COMMAND").addProperty("referenceId", "START").build(),
              aNode().withId("n1").withType("COPY_ARTIFACT").build(),
              aNode().withId("n2").withType("COMMAND").addProperty("referenceId", "STOP").build())
          .addLinks(aLink().withFrom(ORIGIN_STATE).withTo("n0").withType(SUCCESS.name()).withId("l0").build(),
              aLink().withFrom("n0").withTo("n1").withType(SUCCESS.name()).withId("l1").build(),
              aLink().withFrom("n1").withTo("n2").withType(SUCCESS.name()).withId("l2").build())
          .build();
}
