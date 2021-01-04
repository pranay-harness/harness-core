package io.harness.callgraph;

import static io.harness.rule.OwnerRule.SHIVAKUMAR;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.callgraph.util.StackNode;
import io.harness.callgraph.util.Target;
import io.harness.callgraph.util.config.Config;
import io.harness.callgraph.util.config.ConfigUtils;
import io.harness.callgraph.writer.GraphWriter;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;

public class CallGraph2Test {
  private StackNode node;
  private GraphWriter writer;
  private CallGraph graph;

  @Before
  public void before() throws IOException {
    Config c = Mockito.mock(Config.class);
    Mockito.when(c.writeTo()).thenReturn(new Target[] {Target.COVERAGE_JSON});
    node = Mockito.mock(StackNode.class);
    Mockito.when(node.toString()).thenReturn("method()");
    writer = Mockito.mock(GraphWriter.class);
    ConfigUtils.inject(c);
    graph = Mockito.spy(new CallGraph(123));
    graph.writers.set(0, writer);
  }

  @Test
  @Owner(developers = SHIVAKUMAR)
  @Category(UnitTests.class)
  public void testCalledNodeNotTestMethod() throws IOException {
    // first method is not test, so nothing should be pushed to the stack
    graph.called(node);
    assertThat(graph.calls.isEmpty()).isEqualTo(true);
  }

  @Test
  @Owner(developers = SHIVAKUMAR)
  @Category(UnitTests.class)
  public void testCalledNode() throws IOException {
    // first method is test, so the node should be pushed to the stack
    Mockito.when(node.isTestMethod()).thenReturn(true);

    graph.called(node);
    assertThat(graph.calls.peek()).isEqualTo(node);
  }

  @Test
  @Owner(developers = SHIVAKUMAR)
  @Category(UnitTests.class)
  public void testCalledEdge() throws IOException {
    StackNode item2 = Mockito.mock(StackNode.class);
    graph.calls.push(item2);
    graph.called(node);
    Mockito.verify(writer).edge(item2, node);
    assertThat(graph.calls.peek()).isEqualTo(node);
  }
}
