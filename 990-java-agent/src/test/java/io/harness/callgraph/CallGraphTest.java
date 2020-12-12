package io.harness.callgraph;

import static io.harness.rule.OwnerRule.SHIVAKUMAR;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.callgraph.util.StackNode;
import io.harness.callgraph.util.Target;
import io.harness.callgraph.util.config.Config;
import io.harness.callgraph.util.config.ConfigUtils;
import io.harness.callgraph.writer.GraphDBCSVFileWriter;
import io.harness.callgraph.writer.GraphWriter;
import io.harness.callgraph.writer.JSONCoverageFileWriter;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;

@Slf4j
public class CallGraphTest {
  @Test
  @Owner(developers = SHIVAKUMAR)
  @Category(UnitTests.class)
  public void testCreateWriter() throws IOException {
    CallGraph cg = new CallGraph(1);

    GraphWriter w = cg.createWriter(Target.COVERAGE_JSON);
    assertThat(w).isInstanceOf(JSONCoverageFileWriter.class);

    w = cg.createWriter(Target.GRAPH_DB_CSV);
    assertThat(w).isInstanceOf(GraphDBCSVFileWriter.class);
  }
  @Test
  @Owner(developers = SHIVAKUMAR)
  @Category(UnitTests.class)
  public void testFinish() throws IOException {
    CallGraph graph = new CallGraph(1);
    GraphWriter writer = Mockito.mock(GraphWriter.class);
    StackNode node = Mockito.mock(StackNode.class);
    graph.writers.set(0, writer);

    graph.finish();
    Mockito.verify(writer, Mockito.never()).end();
    graph.calls.push(node);
    graph.finish();
    Mockito.verify(writer, Mockito.never()).end();
    Mockito.verify(writer, Mockito.times(2)).close();
  }

  @Test
  @Owner(developers = SHIVAKUMAR)
  @Category(UnitTests.class)
  public void testReturned() throws IOException {
    Config c = Mockito.mock(Config.class);
    Mockito.when(c.writeTo()).thenReturn(new Target[] {Target.COVERAGE_JSON});
    ConfigUtils.inject(c);

    CallGraph graph = new CallGraph(1);
    GraphWriter writer = Mockito.mock(GraphWriter.class);
    StackNode[] nodes = new StackNode[] {Mockito.mock(StackNode.class), Mockito.mock(StackNode.class),
        Mockito.mock(StackNode.class), Mockito.mock(StackNode.class)};

    // Calling with unknown item should clear the whole stack
    graph.writers.set(0, writer);
    for (StackNode node : nodes) {
      graph.calls.push(node);
    }
    graph.returned(Mockito.mock(StackNode.class));
    assertThat(graph.calls.isEmpty()).isEqualTo(true);
    Mockito.verify(writer).end();

    // It should stop removing when the equal item was found
    graph.writers.set(0, writer);
    for (StackNode node : nodes) {
      graph.calls.push(node);
    }
    graph.returned(nodes[1]);
    assertThat(graph.calls.size()).isEqualTo(1);
    Mockito.verifyNoMoreInteractions(writer);

    // It should call writer.end when the stack is empty
    graph.writers.set(0, writer);
    graph.calls.clear();
    for (StackNode node : nodes) {
      graph.calls.push(node);
    }
    graph.returned(nodes[0]);
    assertThat(graph.calls.isEmpty()).isEqualTo(true);
    Mockito.verify(writer, Mockito.times(2)).end();
  }
}