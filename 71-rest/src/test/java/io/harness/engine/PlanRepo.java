package io.harness.engine;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.adviser.AdviserObtainment;
import io.harness.adviser.AdviserType;
import io.harness.adviser.impl.success.OnSuccessAdviserParameters;
import io.harness.engine.interrupts.steps.SimpleAsyncStep;
import io.harness.engine.interrupts.steps.SimpleStepAsyncParams;
import io.harness.facilitator.FacilitatorObtainment;
import io.harness.facilitator.FacilitatorType;
import io.harness.plan.ExecutionNode;
import io.harness.plan.Plan;
import io.harness.state.StepType;
import io.harness.state.core.dummy.DummyStep;
import io.harness.state.core.section.SectionStepParameters;

public class PlanRepo {
  public static Plan planWithBigWait() {
    String planId = generateUuid();
    String sectionNodeId = generateUuid();
    String test1Id = generateUuid();
    String test3Id = generateUuid();
    String test2Id = generateUuid();
    String dummyNodeId = generateUuid();

    return Plan.builder()
        .node(ExecutionNode.builder()
                  .uuid(test1Id)
                  .name("Test1 - No Wait")
                  .stepType(SimpleAsyncStep.STEP_TYPE)
                  .identifier("test1")
                  .stepParameters(SimpleStepAsyncParams.builder().build())
                  .facilitatorObtainment(FacilitatorObtainment.builder()
                                             .type(FacilitatorType.builder().type(FacilitatorType.ASYNC).build())
                                             .build())
                  .adviserObtainment(AdviserObtainment.builder()
                                         .type(AdviserType.builder().type(AdviserType.ON_SUCCESS).build())
                                         .parameters(OnSuccessAdviserParameters.builder().nextNodeId(test2Id).build())
                                         .build())
                  .build())
        .node(ExecutionNode.builder()
                  .uuid(test2Id)
                  .name("Test2 - BigWait")
                  .stepType(SimpleAsyncStep.STEP_TYPE)
                  .identifier("test2")
                  .stepParameters(SimpleStepAsyncParams.builder().duration(15).build())
                  .facilitatorObtainment(FacilitatorObtainment.builder()
                                             .type(FacilitatorType.builder().type(FacilitatorType.ASYNC).build())
                                             .build())
                  .adviserObtainment(AdviserObtainment.builder()
                                         .type(AdviserType.builder().type(AdviserType.ON_SUCCESS).build())
                                         .parameters(OnSuccessAdviserParameters.builder().nextNodeId(test3Id).build())
                                         .build())
                  .build())
        .node(ExecutionNode.builder()
                  .uuid(test3Id)
                  .name("Test 3")
                  .stepType(SimpleAsyncStep.STEP_TYPE)
                  .identifier("test3")
                  .stepParameters(SimpleStepAsyncParams.builder().duration(2).build())
                  .facilitatorObtainment(FacilitatorObtainment.builder()
                                             .type(FacilitatorType.builder().type(FacilitatorType.ASYNC).build())
                                             .build())
                  .build())
        .node(
            ExecutionNode.builder()
                .uuid(sectionNodeId)
                .name("Section")
                .stepType(StepType.builder().type("SECTION").build())
                .identifier("section_1")
                .stepParameters(SectionStepParameters.builder().childNodeId(test1Id).build())
                .adviserObtainment(AdviserObtainment.builder()
                                       .type(AdviserType.builder().type(AdviserType.ON_SUCCESS).build())
                                       .parameters(OnSuccessAdviserParameters.builder().nextNodeId(dummyNodeId).build())
                                       .build())
                .facilitatorObtainment(FacilitatorObtainment.builder()
                                           .type(FacilitatorType.builder().type(FacilitatorType.CHILD).build())
                                           .build())
                .build())
        .node(ExecutionNode.builder()
                  .uuid(dummyNodeId)
                  .name("Dummy Node 1")
                  .identifier("dummy")
                  .stepType(DummyStep.STATE_TYPE)
                  .facilitatorObtainment(FacilitatorObtainment.builder()
                                             .type(FacilitatorType.builder().type(FacilitatorType.SYNC).build())
                                             .build())
                  .build())
        .startingNodeId(sectionNodeId)
        .uuid(planId)
        .build();
  }
}
