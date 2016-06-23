package software.wings.beans;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import software.wings.beans.Graph.Link;
import software.wings.beans.Graph.Node;
import software.wings.common.UUIDGenerator;

/**
 * @author Rishi
 */
public class GraphTest {
  @Test
  public void shouldRepaintSimpleGraph() {
    Node node1 = Node.Builder.aNode().withId("node1").build();
    Node node2 = Node.Builder.aNode().withId("node2").build();
    Node node3 = Node.Builder.aNode().withId("node3").build();

    Link link1 =
        Link.Builder.aLink().withId("link1").withFrom(node1.getId()).withTo(node2.getId()).withType("success").build();
    Link link2 =
        Link.Builder.aLink().withId("link2").withFrom(node2.getId()).withTo(node3.getId()).withType("success").build();
    Graph graph = Graph.Builder.aGraph().addNodes(node1, node2, node3).addLinks(link1, link2).build();

    graph.repaint(node1.getId());
    assertThat(graph).isNotNull();
    assertThat(graph.getNodes()).isNotNull();
    assertThat(graph.getNodes().get(0).getX()).isEqualTo(Graph.DEFAULT_INITIAL_X);
    assertThat(graph.getNodes().get(0).getY()).isEqualTo(Graph.DEFAULT_INITIAL_Y);
    assertThat(graph.getNodes().get(1).getX())
        .isEqualTo(graph.getNodes().get(0).getX() + Graph.DEFAULT_NODE_WIDTH + Graph.DEFAULT_ARROW_WIDTH);
    assertThat(graph.getNodes().get(1).getY()).isEqualTo(Graph.DEFAULT_INITIAL_Y);
    assertThat(graph.getNodes().get(2).getX())
        .isEqualTo(graph.getNodes().get(1).getX() + Graph.DEFAULT_NODE_WIDTH + Graph.DEFAULT_ARROW_WIDTH);
    assertThat(graph.getNodes().get(2).getY()).isEqualTo(Graph.DEFAULT_INITIAL_Y);
  }

  @Test
  public void shouldRepaintNestedGraph() {
    Graph graph =
        Graph.Builder.aGraph()
            .addNodes(Node.Builder.aNode().withId("node1").build(), Node.Builder.aNode().withId("node2").build(),
                Node.Builder.aNode().withId("node2_1").build(), Node.Builder.aNode().withId("node2_1_1").build(),
                Node.Builder.aNode().withId("node2_1_2").build(), Node.Builder.aNode().withId("node2_1_3").build(),
                Node.Builder.aNode().withId("node2_1_4").build(), Node.Builder.aNode().withId("node2_2").build(),
                Node.Builder.aNode().withId("node2_3").build(), Node.Builder.aNode().withId("node2_3_1").build(),
                Node.Builder.aNode().withId("node2_3_2").build(), Node.Builder.aNode().withId("node2_3_3").build(),
                Node.Builder.aNode().withId("node3").build(), Node.Builder.aNode().withId("node3_1").build(),
                Node.Builder.aNode().withId("node3_1_1").build(), Node.Builder.aNode().withId("node3_1_1_1").build())
            .addLinks(Link.Builder.aLink()
                          .withId(UUIDGenerator.getUuid())
                          .withFrom("node1")
                          .withTo("node2")
                          .withType("success")
                          .build())
            .addLinks(Link.Builder.aLink()
                          .withId(UUIDGenerator.getUuid())
                          .withFrom("node2")
                          .withTo("node3")
                          .withType("success")
                          .build())
            .addLinks(Link.Builder.aLink()
                          .withId(UUIDGenerator.getUuid())
                          .withFrom("node2_1")
                          .withTo("node2_2")
                          .withType("success")
                          .build())
            .addLinks(Link.Builder.aLink()
                          .withId(UUIDGenerator.getUuid())
                          .withFrom("node2_2")
                          .withTo("node2_3")
                          .withType("success")
                          .build())
            .addLinks(Link.Builder.aLink()
                          .withId(UUIDGenerator.getUuid())
                          .withFrom("node2_3_1")
                          .withTo("node2_3_2")
                          .withType("success")
                          .build())
            .addLinks(Link.Builder.aLink()
                          .withId(UUIDGenerator.getUuid())
                          .withFrom("node2_3_2")
                          .withTo("node2_3_3")
                          .withType("success")
                          .build())
            .addLinks(Link.Builder.aLink()
                          .withId(UUIDGenerator.getUuid())
                          .withFrom("node2_1_1")
                          .withTo("node2_1_2")
                          .withType("success")
                          .build())
            .addLinks(Link.Builder.aLink()
                          .withId(UUIDGenerator.getUuid())
                          .withFrom("node2_1_2")
                          .withTo("node2_1_3")
                          .withType("success")
                          .build())
            .addLinks(Link.Builder.aLink()
                          .withId(UUIDGenerator.getUuid())
                          .withFrom("node2_1_3")
                          .withTo("node2_1_4")
                          .withType("success")
                          .build())
            .addLinks(Link.Builder.aLink()
                          .withId(UUIDGenerator.getUuid())
                          .withFrom("node2")
                          .withTo("node2_1")
                          .withType("repeat")
                          .build())
            .addLinks(Link.Builder.aLink()
                          .withId(UUIDGenerator.getUuid())
                          .withFrom("node2")
                          .withTo("node2_1_1")
                          .withType("repeat")
                          .build())
            .addLinks(Link.Builder.aLink()
                          .withId(UUIDGenerator.getUuid())
                          .withFrom("node2_3")
                          .withTo("node2_3_1")
                          .withType("repeat")
                          .build())
            .addLinks(Link.Builder.aLink()
                          .withId(UUIDGenerator.getUuid())
                          .withFrom("node3")
                          .withTo("node3_1")
                          .withType("repeat")
                          .build())
            .addLinks(Link.Builder.aLink()
                          .withId(UUIDGenerator.getUuid())
                          .withFrom("node3")
                          .withTo("node3_1_1")
                          .withType("repeat")
                          .build())
            .addLinks(Link.Builder.aLink()
                          .withId(UUIDGenerator.getUuid())
                          .withFrom("node3")
                          .withTo("node3_1_1_1")
                          .withType("repeat")
                          .build())
            .build();

    graph.repaint("node1");
    assertThat(graph).isNotNull();
    assertThat(graph.getNodes()).isNotNull();
  }
}
