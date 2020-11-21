package graph;

import static io.harness.rule.OwnerRule.ALEKSANDAR;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.harness.beans.CIBeansTest;
import io.harness.beans.steps.stepinfo.GitCloneStepInfo;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.yaml.core.ParallelStepElement;
import io.harness.yaml.core.StepElement;
import io.harness.yaml.core.auxiliary.intfc.ExecutionWrapper;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class StepInfoGraphConverterTest extends CIBeansTest {
  @Inject private StepInfoGraphConverter stepInfoGraphConverter;

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void testConversion() {
    List<ExecutionWrapper> executionSectionList = new ArrayList<>();
    executionSectionList.add(
        StepElement.builder().stepSpecType(GitCloneStepInfo.builder().identifier("git-before-1").build()).build());
    executionSectionList.add(
        StepElement.builder().stepSpecType(GitCloneStepInfo.builder().identifier("git-before-2").build()).build());

    List<ExecutionWrapper> parallelList = new ArrayList<>();
    parallelList.add(
        StepElement.builder().stepSpecType(GitCloneStepInfo.builder().identifier("git-parallel-1").build()).build());
    parallelList.add(
        StepElement.builder().stepSpecType(GitCloneStepInfo.builder().identifier("git-parallel-2").build()).build());
    parallelList.add(
        StepElement.builder().stepSpecType(GitCloneStepInfo.builder().identifier("git-parallel-3").build()).build());
    executionSectionList.add(ParallelStepElement.builder().sections(parallelList).build());

    executionSectionList.add(
        StepElement.builder().stepSpecType(GitCloneStepInfo.builder().identifier("git-after-1").build()).build());

    final StepInfoGraph stepInfoGraph = stepInfoGraphConverter.convert(executionSectionList);
    assertThat(stepInfoGraph).isNotNull();
    assertThat(stepInfoGraph.getStartNodeUuid()).isEqualTo(stepInfoGraph.getNode("git-before-1").getIdentifier());

    assertThat(stepInfoGraph.getNextNodeUuids(stepInfoGraph.getNode("git-before-1")))
        .contains(stepInfoGraph.getNode("git-before-2").getIdentifier());
    assertThat(stepInfoGraph.getNextNodeUuids(stepInfoGraph.getNode("git-before-2")))
        .contains(stepInfoGraph.getNode("git-parallel-1").getIdentifier());
    assertThat(stepInfoGraph.getNextNodeUuids(stepInfoGraph.getNode("git-before-2")))
        .contains(stepInfoGraph.getNode("git-parallel-2").getIdentifier());
    assertThat(stepInfoGraph.getNextNodeUuids(stepInfoGraph.getNode("git-before-2")))
        .contains(stepInfoGraph.getNode("git-parallel-3").getIdentifier());

    assertThat(StepInfoGraph.isNILStepUuId(stepInfoGraph.getNextNodeUuids(stepInfoGraph.getNode("git-after-1")).get(0)))
        .isTrue();
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  @Ignore("TODO: Graph section is not yet supported")
  public void testConversion_withGraphSection() {
    List<ExecutionWrapper> executionSectionList = new ArrayList<>();
    executionSectionList.add(
        StepElement.builder().stepSpecType(GitCloneStepInfo.builder().identifier("git-before-1").build()).build());

    List<StepElement> GraphList = new ArrayList<>();
    GraphList.add(
        StepElement.builder().stepSpecType(GitCloneStepInfo.builder().identifier("git-graph-1").build()).build());
    GraphList.add(
        StepElement.builder().stepSpecType(GitCloneStepInfo.builder().identifier("git-graph-2").build()).build());
    GraphList.add(
        StepElement.builder().stepSpecType(GitCloneStepInfo.builder().identifier("git-graph-3").build()).build());
    executionSectionList.add(io.harness.yaml.core.Graph.builder().sections(GraphList).build());

    executionSectionList.add(
        StepElement.builder().stepSpecType(GitCloneStepInfo.builder().identifier("git-after-1").build()).build());

    final StepInfoGraph stepInfoGraph = stepInfoGraphConverter.convert(executionSectionList);

    assertThat(stepInfoGraph.getNextNodeUuids(stepInfoGraph.getNode("git-graph-3")))
        .contains(stepInfoGraph.getNode("git-graph-1").getIdentifier());
    assertThat(stepInfoGraph.getNextNodeUuids(stepInfoGraph.getNode("git-graph-3")))
        .contains(stepInfoGraph.getNode("git-graph-2").getIdentifier());
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void testConversion_withEmptyList() {
    final StepInfoGraph stepInfoGraph = stepInfoGraphConverter.convert(new ArrayList<>());
    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(stepInfoGraph::getStartNodeUuid);
  }
}
