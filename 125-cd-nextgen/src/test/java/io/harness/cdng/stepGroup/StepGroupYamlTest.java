package io.harness.cdng.stepGroup;

import static io.harness.rule.OwnerRule.ARCHIT;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cdng.pipeline.CDPipeline;
import io.harness.cdng.pipeline.DeploymentStage;
import io.harness.rule.Owner;
import io.harness.yaml.core.ParallelStepElement;
import io.harness.yaml.core.StageElement;
import io.harness.yaml.core.StepGroupElement;
import io.harness.yaml.core.auxiliary.intfc.ExecutionWrapper;
import io.harness.yaml.core.auxiliary.intfc.StageElementWrapper;
import io.harness.yaml.utils.YamlPipelineUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.net.URL;
import java.util.List;

public class StepGroupYamlTest extends CategoryTest {
  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testYamlParseForStepGroupAndParallel() throws IOException {
    ClassLoader classLoader = this.getClass().getClassLoader();
    final URL testFile = classLoader.getResource("cdng/stepGroup.yml");
    CDPipeline cdPipeline = YamlPipelineUtils.read(testFile, CDPipeline.class);
    assertThat(cdPipeline.getStages().size()).isEqualTo(2);

    // First Stage
    StageElementWrapper stageWrapper = cdPipeline.getStages().get(0);
    assertThat(stageWrapper).isInstanceOf(StageElement.class);
    assertThat(((StageElement) stageWrapper).getStageType()).isInstanceOf(DeploymentStage.class);
    DeploymentStage deploymentStage = (DeploymentStage) ((StageElement) stageWrapper).getStageType();
    List<ExecutionWrapper> steps = deploymentStage.getExecution().getSteps();
    assertThat(steps.size()).isEqualTo(1);
    assertThat(steps.get(0)).isInstanceOf(ParallelStepElement.class);

    ParallelStepElement parallelStepElement = (ParallelStepElement) steps.get(0);
    List<ExecutionWrapper> sections = parallelStepElement.getSections();
    assertThat(sections.size()).isEqualTo(3);
    assertThat(sections.get(1)).isInstanceOf(StepGroupElement.class);

    StepGroupElement stepGroupElement = (StepGroupElement) sections.get(1);
    assertThat(stepGroupElement.getIdentifier()).isEqualTo("StepGroup1");
    assertThat(stepGroupElement.getName()).isEqualTo("StepGroup1");
    assertThat(stepGroupElement.getSteps().size()).isEqualTo(1);
  }
}
