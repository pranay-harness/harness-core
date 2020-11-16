package io.harness.engine;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;

import io.harness.adviser.OrchestrationAdviserTypes;
import io.harness.advisers.success.OnSuccessAdviserParameters;
import io.harness.engine.interrupts.steps.SimpleAsyncStep;
import io.harness.engine.interrupts.steps.SimpleStepAsyncParams;
import io.harness.facilitator.FacilitatorObtainment;
import io.harness.facilitator.FacilitatorType;
import io.harness.plan.Plan;
import io.harness.plan.PlanNode;
import io.harness.pms.advisers.AdviserObtainment;
import io.harness.pms.advisers.AdviserType;
import io.harness.serializer.KryoSerializer;
import io.harness.state.StepType;
import io.harness.steps.dummy.DummyStep;
import io.harness.steps.section.SectionStepParameters;

public class PlanRepo {
  @Inject private KryoSerializer kryoSerializer;

  public Plan planWithBigWait() {
    String sectionNodeId = generateUuid();
    String test1Id = generateUuid();
    String test3Id = generateUuid();
    String test2Id = generateUuid();
    String dummyNodeId = generateUuid();

    return Plan.builder()
        .node(
            PlanNode.builder()
                .uuid(test1Id)
                .name("Test1 - No Wait")
                .stepType(SimpleAsyncStep.STEP_TYPE)
                .identifier("test1")
                .stepParameters(SimpleStepAsyncParams.builder().build())
                .facilitatorObtainment(FacilitatorObtainment.builder()
                                           .type(FacilitatorType.builder().type(FacilitatorType.ASYNC).build())
                                           .build())
                .adviserObtainment(
                    AdviserObtainment.newBuilder()
                        .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.ON_SUCCESS.name()).build())
                        .setParameters(ByteString.copyFrom(
                            kryoSerializer.asBytes(OnSuccessAdviserParameters.builder().nextNodeId(test2Id).build())))
                        .build())
                .build())
        .node(
            PlanNode.builder()
                .uuid(test2Id)
                .name("Test2 - BigWait")
                .stepType(SimpleAsyncStep.STEP_TYPE)
                .identifier("test2")
                .stepParameters(SimpleStepAsyncParams.builder().duration(15).build())
                .facilitatorObtainment(FacilitatorObtainment.builder()
                                           .type(FacilitatorType.builder().type(FacilitatorType.ASYNC).build())
                                           .build())
                .adviserObtainment(
                    AdviserObtainment.newBuilder()
                        .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.ON_SUCCESS.name()).build())
                        .setParameters(ByteString.copyFrom(
                            kryoSerializer.asBytes(OnSuccessAdviserParameters.builder().nextNodeId(test3Id).build())))
                        .build())
                .build())
        .node(PlanNode.builder()
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
            PlanNode.builder()
                .uuid(sectionNodeId)
                .name("Section")
                .stepType(StepType.builder().type("SECTION").build())
                .identifier("section_1")
                .stepParameters(SectionStepParameters.builder().childNodeId(test1Id).build())
                .adviserObtainment(
                    AdviserObtainment.newBuilder()
                        .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.ON_SUCCESS.name()).build())
                        .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                            OnSuccessAdviserParameters.builder().nextNodeId(dummyNodeId).build())))
                        .build())
                .facilitatorObtainment(FacilitatorObtainment.builder()
                                           .type(FacilitatorType.builder().type(FacilitatorType.CHILD).build())
                                           .build())
                .build())
        .node(PlanNode.builder()
                  .uuid(dummyNodeId)
                  .name("Dummy Node 1")
                  .identifier("dummy")
                  .stepType(DummyStep.STEP_TYPE)
                  .facilitatorObtainment(FacilitatorObtainment.builder()
                                             .type(FacilitatorType.builder().type(FacilitatorType.SYNC).build())
                                             .build())
                  .build())
        .startingNodeId(sectionNodeId)
        .build();
  }

  public Plan planWithFailure() {
    String test1Id = generateUuid();
    String dummyNodeId = generateUuid();

    return Plan.builder()
        .node(
            PlanNode.builder()
                .uuid(test1Id)
                .name("Test1 - No Wait")
                .stepType(SimpleAsyncStep.STEP_TYPE)
                .identifier("test1")
                .stepParameters(SimpleStepAsyncParams.builder().shouldFail(true).build())
                .facilitatorObtainment(FacilitatorObtainment.builder()
                                           .type(FacilitatorType.builder().type(FacilitatorType.ASYNC).build())
                                           .build())
                .adviserObtainment(
                    AdviserObtainment.newBuilder()
                        .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.ON_SUCCESS.name()).build())
                        .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                            OnSuccessAdviserParameters.builder().nextNodeId(dummyNodeId).build())))
                        .build())
                .adviserObtainment(AdviserObtainment.newBuilder()
                                       .setType(AdviserType.newBuilder()
                                                    .setType(OrchestrationAdviserTypes.MANUAL_INTERVENTION.name())
                                                    .build())
                                       .build())
                .build())
        .node(PlanNode.builder()
                  .uuid(dummyNodeId)
                  .name("Dummy Node 1")
                  .identifier("dummy")
                  .stepType(DummyStep.STEP_TYPE)
                  .facilitatorObtainment(FacilitatorObtainment.builder()
                                             .type(FacilitatorType.builder().type(FacilitatorType.SYNC).build())
                                             .build())
                  .build())
        .startingNodeId(test1Id)
        .build();
  }
}
