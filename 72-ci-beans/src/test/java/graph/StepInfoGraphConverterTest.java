package graph;

import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.google.inject.Inject;

import io.harness.beans.CIBeansTest;
import io.harness.beans.steps.stepinfo.GitCloneStepInfo;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.yaml.core.Parallel;
import io.harness.yaml.core.auxiliary.intfc.ExecutionSection;
import io.harness.yaml.core.auxiliary.intfc.StepWrapper;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StepInfoGraphConverterTest extends CIBeansTest {
  @Inject private StepInfoGraphConverter stepInfoGraphConverter;

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void testConversion() {
    List<ExecutionSection> executionSectionList = new ArrayList<>();
    executionSectionList.add(GitCloneStepInfo.builder().identifier("git-before-1").build());
    executionSectionList.add(GitCloneStepInfo.builder().identifier("git-before-2").build());

    List<StepWrapper> parallelList = new ArrayList<>();
    parallelList.add(GitCloneStepInfo.builder().identifier("git-parallel-1").build());
    parallelList.add(GitCloneStepInfo.builder().identifier("git-parallel-2").build());
    parallelList.add(GitCloneStepInfo.builder().identifier("git-parallel-3").build());
    executionSectionList.add(Parallel.builder().sections(parallelList).build());

    executionSectionList.add(GitCloneStepInfo.builder().identifier("git-after-1").build());

    final StepInfoGraph stepInfoGraph = stepInfoGraphConverter.convert(executionSectionList);
    assertThat(stepInfoGraph).isNotNull();
    assertThat(stepInfoGraph.getStartNodeUuid())
        .isEqualTo(stepInfoGraph.getNode("git-before-1").getStepMetadata().getUuid());

    assertThat(stepInfoGraph.getNextNodeUuids(stepInfoGraph.getNode("git-before-1")))
        .contains(stepInfoGraph.getNode("git-before-2").getStepMetadata().getUuid());
    assertThat(stepInfoGraph.getNextNodeUuids(stepInfoGraph.getNode("git-before-2")))
        .contains(stepInfoGraph.getNode("git-parallel-1").getStepMetadata().getUuid());
    assertThat(stepInfoGraph.getNextNodeUuids(stepInfoGraph.getNode("git-before-2")))
        .contains(stepInfoGraph.getNode("git-parallel-2").getStepMetadata().getUuid());
    assertThat(stepInfoGraph.getNextNodeUuids(stepInfoGraph.getNode("git-before-2")))
        .contains(stepInfoGraph.getNode("git-parallel-3").getStepMetadata().getUuid());

    assertThat(StepInfoGraph.isNILStepUuId(stepInfoGraph.getNextNodeUuids(stepInfoGraph.getNode("git-after-1")).get(0)))
        .isTrue();
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void testConversion_withGraphSection() {
    List<ExecutionSection> executionSectionList = new ArrayList<>();
    executionSectionList.add(GitCloneStepInfo.builder().identifier("git-before-1").build());

    List<StepWrapper> GraphList = new ArrayList<>();
    GraphList.add(GitCloneStepInfo.builder()
                      .identifier("git-graph-1")
                      .dependencies(Collections.singletonList("git-graph-3"))
                      .build());
    GraphList.add(GitCloneStepInfo.builder()
                      .identifier("git-graph-2")
                      .dependencies(Collections.singletonList("git-graph-3"))
                      .build());
    GraphList.add(GitCloneStepInfo.builder().identifier("git-graph-3").build());
    executionSectionList.add(io.harness.yaml.core.Graph.builder().sections(GraphList).build());

    executionSectionList.add(GitCloneStepInfo.builder().identifier("git-after-1").build());

    final StepInfoGraph stepInfoGraph = stepInfoGraphConverter.convert(executionSectionList);

    assertThat(stepInfoGraph.getNextNodeUuids(stepInfoGraph.getNode("git-graph-3")))
        .contains(stepInfoGraph.getNode("git-graph-1").getStepMetadata().getUuid());
    assertThat(stepInfoGraph.getNextNodeUuids(stepInfoGraph.getNode("git-graph-3")))
        .contains(stepInfoGraph.getNode("git-graph-2").getStepMetadata().getUuid());
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void testConversion_withEmptyList() {
    final StepInfoGraph stepInfoGraph = stepInfoGraphConverter.convert(new ArrayList<>());
    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(stepInfoGraph::getStartNodeUuid);
  }
}