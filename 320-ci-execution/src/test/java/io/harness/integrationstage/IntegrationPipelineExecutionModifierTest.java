package io.harness.integrationstage;

import static io.harness.rule.OwnerRule.ALEKSANDAR;

import static java.util.Arrays.asList;

import io.harness.beans.stages.IntegrationStage;
import io.harness.beans.yaml.extended.infrastrucutre.K8sDirectInfraYaml;
import io.harness.beans.yaml.extended.infrastrucutre.UseFromStageInfraYaml;
import io.harness.category.element.UnitTests;
import io.harness.ci.integrationstage.IntegrationPipelineExecutionModifier;
import io.harness.executionplan.CIExecutionTest;
import io.harness.executionplan.core.impl.ExecutionPlanCreationContextImpl;
import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;
import io.harness.rule.Owner;
import io.harness.yaml.core.ParallelStageElement;
import io.harness.yaml.core.StageElement;
import io.harness.yaml.core.auxiliary.intfc.StageElementWrapper;

import com.google.inject.Inject;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class IntegrationPipelineExecutionModifierTest extends CIExecutionTest {
  @Inject IntegrationPipelineExecutionModifier integrationPipelineExecutionModifier;
  private ExecutionPlanCreationContextImpl executionPlanCreationContext =
      ExecutionPlanCreationContextImpl.builder().build();

  private List<StageElementWrapper> getInputStages() {
    IntegrationStage stage1 =
        IntegrationStage.builder().identifier("s1").infrastructure(K8sDirectInfraYaml.builder().build()).build();
    IntegrationStage stage2 = IntegrationStage.builder()
                                  .identifier("s2")
                                  .infrastructure(UseFromStageInfraYaml.builder().useFromStage("s1").build())
                                  .build();

    IntegrationStage stage3 =
        IntegrationStage.builder().identifier("s3").infrastructure(K8sDirectInfraYaml.builder().build()).build();
    IntegrationStage stage4 = IntegrationStage.builder()
                                  .identifier("s4")
                                  .infrastructure(UseFromStageInfraYaml.builder().useFromStage("s1").build())
                                  .build();

    IntegrationStage stage5 =
        IntegrationStage.builder().identifier("s5").infrastructure(K8sDirectInfraYaml.builder().build()).build();
    IntegrationStage stage6 = IntegrationStage.builder()
                                  .identifier("s6")
                                  .infrastructure(UseFromStageInfraYaml.builder().useFromStage("s3").build())
                                  .build();

    StageElement stageElement1 = StageElement.builder().identifier("s1").stageType(stage1).build();
    StageElement stageElement2 = StageElement.builder().identifier("s2").stageType(stage2).build();
    StageElement stageElement3 = StageElement.builder().identifier("s3").stageType(stage3).build();
    StageElement stageElement4 = StageElement.builder().identifier("s4").stageType(stage4).build();
    StageElement stageElement5 = StageElement.builder().identifier("s5").stageType(stage5).build();
    StageElement stageElement6 = StageElement.builder().identifier("s6").stageType(stage6).build();

    return asList(stageElement1, stageElement2,
        ParallelStageElement.builder().sections(asList(stageElement3, stageElement4)).build(), stageElement5,
        stageElement6);
  }

  private NgPipeline getInputPipeline() {
    return NgPipeline.builder().identifier("test").stages(getInputStages()).build();
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldModifyPipelineExecution() {
    //    NgPipeline pipeline =
    //        integrationPipelineExecutionModifier.modifyExecutionPlan(getInputPipeline(),
    //        executionPlanCreationContext);
    //    assertThat(
    //        ((IntegrationStage) ((StageElement)
    //        pipeline.getStages().get(1)).getStageType()).getInfrastructure().getType()) .isEqualTo(KUBERNETES_DIRECT);
    //
    //    List<StageElementWrapper> sections = ((ParallelStageElement) pipeline.getStages().get(2)).getSections();
    //    assertThat(((IntegrationStage) ((StageElement) sections.get(1)).getStageType()).getInfrastructure().getType())
    //        .isEqualTo(KUBERNETES_DIRECT);
    //
    //    assertThat(
    //        ((IntegrationStage) ((StageElement)
    //        pipeline.getStages().get(4)).getStageType()).getInfrastructure().getType()) .isEqualTo(KUBERNETES_DIRECT);
  }
}
