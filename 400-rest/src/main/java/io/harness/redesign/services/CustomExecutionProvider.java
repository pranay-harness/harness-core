package io.harness.redesign.services;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.annotations.dev.OwnedBy;
import io.harness.config.MockServerConfig;
import io.harness.plan.Plan;
import io.harness.pms.contracts.advisers.AdviserObtainment;
import io.harness.pms.contracts.advisers.AdviserType;
import io.harness.pms.contracts.commons.RepairActionCode;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.contracts.steps.SkipType;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.adviser.OrchestrationAdviserTypes;
import io.harness.pms.sdk.core.adviser.fail.OnFailAdviser;
import io.harness.pms.sdk.core.adviser.fail.OnFailAdviserParameters;
import io.harness.pms.sdk.core.adviser.marksuccess.OnMarkSuccessAdviserParameters;
import io.harness.pms.sdk.core.adviser.retry.RetryAdviserParameters;
import io.harness.pms.sdk.core.adviser.success.OnSuccessAdviser;
import io.harness.pms.sdk.core.adviser.success.OnSuccessAdviserParameters;
import io.harness.pms.sdk.core.facilitator.OrchestrationFacilitatorType;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.redesign.advisers.HttpResponseCodeSwitchAdviser;
import io.harness.redesign.advisers.HttpResponseCodeSwitchAdviserParameters;
import io.harness.redesign.states.email.EmailStep;
import io.harness.redesign.states.email.EmailStepParameters;
import io.harness.redesign.states.http.BasicHttpStep;
import io.harness.redesign.states.http.BasicHttpStepParameters;
import io.harness.redesign.states.http.chain.BasicHttpChainStep;
import io.harness.redesign.states.http.chain.BasicHttpChainStepParameters;
import io.harness.redesign.states.wait.WaitStep;
import io.harness.redesign.states.wait.WaitStepParameters;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.barriers.BarrierStep;
import io.harness.steps.barriers.BarrierStepParameters;
import io.harness.steps.fork.ForkStep;
import io.harness.steps.fork.ForkStepParameters;
import io.harness.steps.resourcerestraint.ResourceRestraintStep;
import io.harness.steps.resourcerestraint.ResourceRestraintStepParameters;
import io.harness.steps.resourcerestraint.beans.AcquireMode;
import io.harness.steps.resourcerestraint.beans.HoldingScope.HoldingScopeBuilder;
import io.harness.steps.section.SectionStep;
import io.harness.steps.section.SectionStepParameters;
import io.harness.steps.section.chain.SectionChainStep;
import io.harness.steps.section.chain.SectionChainStepParameters;
import io.harness.timeout.contracts.TimeoutObtainment;
import io.harness.timeout.trackers.absolute.AbsoluteTimeoutParameters;
import io.harness.timeout.trackers.absolute.AbsoluteTimeoutTrackerFactory;

import software.wings.app.MainConfiguration;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import java.time.Duration;

@OwnedBy(CDC)
@Singleton
public class CustomExecutionProvider {
  @Inject private MainConfiguration configuration;
  @Inject private KryoSerializer kryoSerializer;

  private static final String BASIC_HTTP_STATE_URL_404 = "404";
  private static final String BASIC_HTTP_STATE_URL_200 = "200";
  private static final String BASIC_HTTP_STATE_URL_500 = "500";
  private static final StepType DUMMY_STEP_TYPE = StepType.newBuilder().setType("DUMMY").build();
  private static final StepType BASIC_HTTP_STEP_TYPE = StepType.newBuilder().setType("BASIC_HTTP").build();
  private static final String RESOURCE_UNIT = generateUuid();

  public Plan provideHttpSwitchPlan() {
    String httpNodeId = generateUuid();
    String dummyNode1Id = generateUuid();
    String dummyNode2Id = generateUuid();
    String dummyNode3Id = generateUuid();
    String waitNodeId = generateUuid();

    BasicHttpStepParameters basicHttpStateParameters =
        BasicHttpStepParameters.builder().url(getMockServerUrl() + BASIC_HTTP_STATE_URL_200).method("GET").build();
    return Plan.builder()
        .node(PlanNode.builder()
                  .uuid(httpNodeId)
                  .name("Basic Http")
                  .stepType(BASIC_HTTP_STEP_TYPE)
                  .stepParameters(basicHttpStateParameters)
                  .identifier("http")
                  .adviserObtainment(AdviserObtainment.newBuilder()
                                         .setType(AdviserType.newBuilder().setType("HTTP_RESPONSE_CODE_SWITCH").build())
                                         .setParameters(ByteString.copyFrom(
                                             kryoSerializer.asBytes(HttpResponseCodeSwitchAdviserParameters.builder()
                                                                        .responseCodeNodeIdMapping(200, dummyNode1Id)
                                                                        .responseCodeNodeIdMapping(404, dummyNode2Id)
                                                                        .responseCodeNodeIdMapping(500, dummyNode3Id)
                                                                        .build())))
                                         .build())
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.TASK).build())
                          .build())
                  .build())
        .node(
            PlanNode.builder()
                .uuid(dummyNode1Id)
                .name("Dummy Node 1")
                .stepType(DUMMY_STEP_TYPE)
                .identifier("dummy1")
                .adviserObtainment(
                    AdviserObtainment.newBuilder()
                        .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.ON_SUCCESS.name()).build())
                        .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                            OnSuccessAdviserParameters.builder().nextNodeId(waitNodeId).build())))
                        .build())
                .facilitatorObtainment(
                    FacilitatorObtainment.newBuilder()
                        .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.SYNC).build())
                        .build())
                .refObject(RefObjectUtils.getOutcomeRefObject("http", httpNodeId, null))
                .build())
        .node(
            PlanNode.builder()
                .uuid(dummyNode2Id)
                .name("Dummy Node 2")
                .stepType(DUMMY_STEP_TYPE)
                .identifier("dummy2")
                .adviserObtainment(
                    AdviserObtainment.newBuilder()
                        .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.ON_SUCCESS.name()).build())
                        .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                            OnSuccessAdviserParameters.builder().nextNodeId(waitNodeId).build())))
                        .build())
                .facilitatorObtainment(
                    FacilitatorObtainment.newBuilder()
                        .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.SYNC).build())
                        .build())
                .build())
        .node(
            PlanNode.builder()
                .uuid(dummyNode3Id)
                .name("Dummy Node 3")
                .stepType(DUMMY_STEP_TYPE)
                .identifier("dummy3")
                .adviserObtainment(
                    AdviserObtainment.newBuilder()
                        .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.ON_SUCCESS.name()).build())
                        .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                            OnSuccessAdviserParameters.builder().nextNodeId(waitNodeId).build())))
                        .build())
                .facilitatorObtainment(
                    FacilitatorObtainment.newBuilder()
                        .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.SYNC).build())
                        .build())
                .build())
        .node(PlanNode.builder()
                  .uuid(waitNodeId)
                  .name("Wait Node")
                  .identifier("wait")
                  .stepType(StepType.newBuilder().setType("WAIT_STATE").build())
                  .stepParameters(WaitStepParameters.builder().waitDurationSeconds(5).build())
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.ASYNC).build())
                          .build())
                  .build())
        .startingNodeId(httpNodeId)
        .build();
  }

  public Plan provideHttpSwitchPlanV2() {
    String httpNodeId = generateUuid();

    BasicHttpStepParameters basicHttpStateParameters =
        BasicHttpStepParameters.builder().url("http://httpstat.us/" + BASIC_HTTP_STATE_URL_200).method("GET").build();
    return Plan.builder()
        .node(PlanNode.builder()
                  .uuid(httpNodeId)
                  .name("Basic Http 1")
                  .stepType(BASIC_HTTP_STEP_TYPE)
                  .identifier("http_1")
                  .stepParameters(basicHttpStateParameters)
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.TASK).build())
                          .build())
                  .build())
        .startingNodeId(httpNodeId)
        .build();
  }

  public Plan provideHttpSwitchPlanV3() {
    String httpNodeId = generateUuid();

    BasicHttpStepParameters basicHttpStateParameters =
        BasicHttpStepParameters.builder().url("http://httpstat.us/" + BASIC_HTTP_STATE_URL_200).method("GET").build();
    return Plan.builder()
        .node(PlanNode.builder()
                  .uuid(httpNodeId)
                  .name("Basic Http 1")
                  .stepType(BASIC_HTTP_STEP_TYPE)
                  .identifier("http")
                  .stepParameters(basicHttpStateParameters)
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.TASK).build())
                          .build())
                  .build())
        .startingNodeId(httpNodeId)
        .build();
  }

  public Plan provideHttpForkPlan() {
    String httpNodeId1 = generateUuid();
    String httpNodeId2 = generateUuid();
    String forkNodeId = generateUuid();
    String dummyNodeId = generateUuid();
    String emailNodeId = generateUuid();

    BasicHttpStepParameters basicHttpStateParameters1 =
        BasicHttpStepParameters.builder().url(getMockServerUrl() + BASIC_HTTP_STATE_URL_200).method("GET").build();

    BasicHttpStepParameters basicHttpStateParameters2 =
        BasicHttpStepParameters.builder().url(getMockServerUrl() + BASIC_HTTP_STATE_URL_404).method("GET").build();
    return Plan.builder()
        .node(PlanNode.builder()
                  .uuid(httpNodeId1)
                  .name("Basic Http 1")
                  .stepType(BASIC_HTTP_STEP_TYPE)
                  .identifier("http_parallel_1")
                  .stepParameters(basicHttpStateParameters1)
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.TASK).build())
                          .build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(httpNodeId2)
                  .name("Basic Http 2")
                  .stepType(BASIC_HTTP_STEP_TYPE)
                  .identifier("http_parallel_2")
                  .stepParameters(basicHttpStateParameters2)
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.TASK).build())
                          .build())
                  .build())
        .node(
            PlanNode.builder()
                .uuid(forkNodeId)
                .name("FORK")
                .stepType(StepType.newBuilder().setType("FORK").build())
                .identifier("fork")
                .stepParameters(
                    ForkStepParameters.builder().parallelNodeId(httpNodeId1).parallelNodeId(httpNodeId2).build())
                .adviserObtainment(
                    AdviserObtainment.newBuilder()
                        .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.ON_SUCCESS.name()).build())
                        .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                            OnSuccessAdviserParameters.builder().nextNodeId(dummyNodeId).build())))
                        .build())
                .facilitatorObtainment(
                    FacilitatorObtainment.newBuilder()
                        .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILDREN).build())
                        .build())
                .build())
        .node(
            PlanNode.builder()
                .uuid(dummyNodeId)
                .name("Dummy Node 1")
                .identifier("dummy1")
                .stepType(DUMMY_STEP_TYPE)
                .adviserObtainment(
                    AdviserObtainment.newBuilder()
                        .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.ON_SUCCESS.name()).build())
                        .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                            OnSuccessAdviserParameters.builder().nextNodeId(emailNodeId).build())))
                        .build())
                .facilitatorObtainment(
                    FacilitatorObtainment.newBuilder()
                        .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.SYNC).build())
                        .build())
                .build())
        .node(PlanNode.builder()
                  .uuid(emailNodeId)
                  .name("Email Node")
                  .identifier("email")
                  .stepType(EmailStep.STEP_TYPE)
                  .stepParameters(EmailStepParameters.builder()
                                      .subject("subject")
                                      .body("body")
                                      .toAddress("to1@harness.io, to2@harness.io")
                                      .build())
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.SYNC).build())
                          .build())
                  .build())
        .startingNodeId(forkNodeId)
        .build();
  }

  public Plan provideHttpSectionPlan() {
    String sectionNodeId = generateUuid();
    String httpNodeId1 = generateUuid();
    String httpNodeId2 = generateUuid();
    String waitNodeId1 = generateUuid();
    String dummyNodeId = generateUuid();

    BasicHttpStepParameters basicHttpStateParameters1 =
        BasicHttpStepParameters.builder().url(getMockServerUrl() + BASIC_HTTP_STATE_URL_500).method("GET").build();

    BasicHttpStepParameters basicHttpStateParameters2 =
        BasicHttpStepParameters.builder().url(getMockServerUrl() + BASIC_HTTP_STATE_URL_404).method("GET").build();
    return Plan.builder()
        .node(PlanNode.builder()
                  .uuid(httpNodeId1)
                  .name("Basic Http 1")
                  .stepType(BASIC_HTTP_STEP_TYPE)
                  .identifier("http_1")
                  .stepParameters(basicHttpStateParameters1)
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.TASK).build())
                          .build())
                  .adviserObtainment(
                      AdviserObtainment.newBuilder()
                          .setType(
                              AdviserType.newBuilder().setType(OrchestrationAdviserTypes.MARK_SUCCESS.name()).build())
                          .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                              OnMarkSuccessAdviserParameters.builder().nextNodeId(waitNodeId1).build())))
                          .build())
                  .build())
        .node(
            PlanNode.builder()
                .uuid(waitNodeId1)
                .name("Wait Step")
                .stepType(WaitStep.STEP_TYPE)
                .identifier("wait_1")
                .stepParameters(WaitStepParameters.builder().waitDurationSeconds(5).build())
                .facilitatorObtainment(
                    FacilitatorObtainment.newBuilder()
                        .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.ASYNC).build())
                        .build())
                .adviserObtainment(
                    AdviserObtainment.newBuilder()
                        .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.ON_SUCCESS.name()).build())
                        .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                            OnSuccessAdviserParameters.builder().nextNodeId(httpNodeId2).build())))
                        .build())
                .build())
        .node(PlanNode.builder()
                  .uuid(httpNodeId2)
                  .name("Basic Http 2")
                  .stepType(BASIC_HTTP_STEP_TYPE)
                  .identifier("http_2")
                  .stepParameters(basicHttpStateParameters2)
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.TASK).build())
                          .build())
                  .build())
        .node(
            PlanNode.builder()
                .uuid(sectionNodeId)
                .name("Section")
                .stepType(StepType.newBuilder().setType("SECTION").build())
                .identifier("section_1")
                .stepParameters(SectionStepParameters.builder().childNodeId(httpNodeId1).build())
                .adviserObtainment(
                    AdviserObtainment.newBuilder()
                        .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.ON_SUCCESS.name()).build())
                        .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                            OnSuccessAdviserParameters.builder().nextNodeId(dummyNodeId).build())))
                        .build())
                .facilitatorObtainment(
                    FacilitatorObtainment.newBuilder()
                        .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILD).build())
                        .build())
                .build())
        .node(PlanNode.builder()
                  .uuid(dummyNodeId)
                  .name("Dummy Node 1")
                  .identifier("dummy")
                  .stepType(DUMMY_STEP_TYPE)
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.SYNC).build())
                          .build())
                  .build())
        .startingNodeId(sectionNodeId)
        .build();
  }

  public Plan provideHttpRetryIgnorePlan() {
    String httpNodeId = generateUuid();
    String dummyNodeId = generateUuid();
    BasicHttpStepParameters basicHttpStateParameters =
        BasicHttpStepParameters.builder().url(getMockServerUrl() + BASIC_HTTP_STATE_URL_500).method("GET").build();
    return Plan.builder()
        .startingNodeId(httpNodeId)
        .node(PlanNode.builder()
                  .uuid(httpNodeId)
                  .name("Basic Http 1")
                  .stepType(BasicHttpStep.STEP_TYPE)
                  .identifier("dummy")
                  .stepParameters(basicHttpStateParameters)
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.TASK).build())
                          .build())
                  .adviserObtainment(
                      AdviserObtainment.newBuilder()
                          .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.RETRY.name()).build())
                          .setParameters(ByteString.copyFrom(
                              kryoSerializer.asBytes(RetryAdviserParameters.builder()
                                                         .retryCount(2)
                                                         .waitIntervalList(ImmutableList.of(2, 5))
                                                         .repairActionCodeAfterRetry(RepairActionCode.IGNORE)
                                                         .nextNodeId(dummyNodeId)
                                                         .build())))
                          .build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(dummyNodeId)
                  .name("Dummy Node 1")
                  .identifier("dummy")
                  .stepType(DUMMY_STEP_TYPE)
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.SYNC).build())
                          .build())
                  .build())
        .build();
  }

  public Plan provideHttpRetryAbortPlan() {
    String httpNodeId = generateUuid();
    String dummyNodeId = generateUuid();
    BasicHttpStepParameters basicHttpStateParameters =
        BasicHttpStepParameters.builder().url(getMockServerUrl() + BASIC_HTTP_STATE_URL_500).method("GET").build();
    return Plan.builder()
        .startingNodeId(httpNodeId)
        .node(PlanNode.builder()
                  .uuid(httpNodeId)
                  .name("Basic Http 1")
                  .stepType(BasicHttpStep.STEP_TYPE)
                  .identifier("dummy")
                  .stepParameters(basicHttpStateParameters)
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.TASK).build())
                          .build())
                  .adviserObtainment(
                      AdviserObtainment.newBuilder()
                          .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.RETRY.name()).build())
                          .setParameters(ByteString.copyFrom(
                              kryoSerializer.asBytes(RetryAdviserParameters.builder()
                                                         .retryCount(2)
                                                         .waitIntervalList(ImmutableList.of(2, 5))
                                                         .repairActionCodeAfterRetry(RepairActionCode.END_EXECUTION)
                                                         .nextNodeId(dummyNodeId)
                                                         .build())))
                          .build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(dummyNodeId)
                  .name("Dummy Node 1")
                  .identifier("dummy")
                  .stepType(DUMMY_STEP_TYPE)
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.SYNC).build())
                          .build())
                  .build())
        .build();
  }

  public Plan provideHttpInterventionPlan() {
    String httpNodeId = generateUuid();
    String dummyNodeId = generateUuid();
    BasicHttpStepParameters basicHttpStateParameters =
        BasicHttpStepParameters.builder().url("http://httpstat.us/" + BASIC_HTTP_STATE_URL_500).method("GET").build();
    return Plan.builder()
        .startingNodeId(httpNodeId)
        .node(
            PlanNode.builder()
                .uuid(httpNodeId)
                .name("Basic Http 1")
                .stepType(BasicHttpStep.STEP_TYPE)
                .identifier("dummy")
                .stepParameters(basicHttpStateParameters)
                .facilitatorObtainment(
                    FacilitatorObtainment.newBuilder()
                        .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.TASK).build())
                        .build())
                .adviserObtainment(AdviserObtainment.newBuilder()
                                       .setType(AdviserType.newBuilder()
                                                    .setType(OrchestrationAdviserTypes.MANUAL_INTERVENTION.name())
                                                    .build())
                                       .build())
                .adviserObtainment(
                    AdviserObtainment.newBuilder()
                        .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.ON_SUCCESS.name()).build())
                        .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                            OnSuccessAdviserParameters.builder().nextNodeId(dummyNodeId).build())))
                        .build())
                .build())
        .node(PlanNode.builder()
                  .uuid(dummyNodeId)
                  .name("Dummy Node 1")
                  .identifier("dummy")
                  .stepType(DUMMY_STEP_TYPE)
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.SYNC).build())
                          .build())
                  .build())
        .build();
  }

  public Plan provideHttpRollbackPlan() {
    String sectionNodeId = generateUuid();
    String rollbackSectionNodeId = generateUuid();
    String rollbackHttpNodeId1 = generateUuid();
    String httpNodeId1 = generateUuid();
    String httpNodeId2 = generateUuid();
    String dummyNodeId = generateUuid();

    BasicHttpStepParameters basicHttpStateParameters1 =
        BasicHttpStepParameters.builder().url(getMockServerUrl() + BASIC_HTTP_STATE_URL_200).method("GET").build();

    BasicHttpStepParameters basicHttpStateParameters2 =
        BasicHttpStepParameters.builder().url(getMockServerUrl() + BASIC_HTTP_STATE_URL_404).method("GET").build();
    return Plan.builder()
        .node(
            PlanNode.builder()
                .uuid(httpNodeId1)
                .name("Basic Http 1")
                .stepType(BASIC_HTTP_STEP_TYPE)
                .identifier("http-1")
                .stepParameters(basicHttpStateParameters1)
                .facilitatorObtainment(
                    FacilitatorObtainment.newBuilder()
                        .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.TASK).build())
                        .build())
                .adviserObtainment(
                    AdviserObtainment.newBuilder()
                        .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.ON_SUCCESS.name()).build())
                        .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                            OnSuccessAdviserParameters.builder().nextNodeId(httpNodeId2).build())))
                        .build())
                .build())
        .node(PlanNode.builder()
                  .uuid(httpNodeId2)
                  .name("Basic Http 2")
                  .stepType(BASIC_HTTP_STEP_TYPE)
                  .identifier("http-2")
                  .stepParameters(basicHttpStateParameters2)
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.TASK).build())
                          .build())
                  .build())
        .node(
            PlanNode.builder()
                .uuid(sectionNodeId)
                .name("Section")
                .stepType(StepType.newBuilder().setType("SECTION").build())
                .identifier("section-1")
                .stepParameters(SectionStepParameters.builder().childNodeId(httpNodeId1).build())
                .adviserObtainment(
                    AdviserObtainment.newBuilder()
                        .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.ON_FAIL.name()).build())
                        .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                            OnFailAdviserParameters.builder().nextNodeId(rollbackSectionNodeId).build())))
                        .build())
                .adviserObtainment(
                    AdviserObtainment.newBuilder()
                        .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.ON_SUCCESS.name()).build())
                        .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                            OnSuccessAdviserParameters.builder().nextNodeId(dummyNodeId).build())))
                        .build())
                .facilitatorObtainment(
                    FacilitatorObtainment.newBuilder()
                        .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILD).build())
                        .build())
                .build())
        .node(PlanNode.builder()
                  .uuid(rollbackSectionNodeId)
                  .name("Section")
                  .stepType(StepType.newBuilder().setType("SECTION").build())
                  .identifier("section-1")
                  .stepParameters(SectionStepParameters.builder().childNodeId(rollbackHttpNodeId1).build())
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILD).build())
                          .build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(rollbackHttpNodeId1)
                  .name("Rollback Http 1")
                  .stepType(BASIC_HTTP_STEP_TYPE)
                  .identifier("rollback-http-1")
                  .stepParameters(basicHttpStateParameters1)
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.TASK).build())
                          .build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(dummyNodeId)
                  .name("Dummy Node 1")
                  .identifier("dummy")
                  .stepType(DUMMY_STEP_TYPE)
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.SYNC).build())
                          .build())
                  .build())
        .startingNodeId(sectionNodeId)
        .build();
  }

  public Plan provideTaskChainPlan(String facilitatorType) {
    String sectionNodeId = generateUuid();
    String httpChainId = generateUuid();
    String dummyNodeId = generateUuid();

    BasicHttpStepParameters basicHttpStateParameters1 =
        BasicHttpStepParameters.builder().url("http://httpstat.us/" + BASIC_HTTP_STATE_URL_200).method("GET").build();

    BasicHttpStepParameters basicHttpStateParameters2 =
        BasicHttpStepParameters.builder().url("http://httpstat.us/" + BASIC_HTTP_STATE_URL_404).method("GET").build();
    return Plan.builder()
        .node(
            PlanNode.builder()
                .uuid(sectionNodeId)
                .name("Section")
                .stepType(SectionStep.STEP_TYPE)
                .identifier("section_1")
                .stepParameters(SectionStepParameters.builder().childNodeId(httpChainId).build())
                .adviserObtainment(
                    AdviserObtainment.newBuilder()
                        .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.ON_SUCCESS.name()).build())
                        .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                            OnSuccessAdviserParameters.builder().nextNodeId(dummyNodeId).build())))
                        .build())
                .facilitatorObtainment(
                    FacilitatorObtainment.newBuilder()
                        .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILD).build())
                        .build())
                .build())
        .node(PlanNode.builder()
                  .uuid(httpChainId)
                  .name("HTTP Chain")
                  .stepType(BasicHttpChainStep.STEP_TYPE)
                  .identifier("http_chain")
                  .stepParameters(BasicHttpChainStepParameters.builder()
                                      .linkParameter(basicHttpStateParameters1)
                                      .linkParameter(basicHttpStateParameters2)
                                      .build())
                  .facilitatorObtainment(FacilitatorObtainment.newBuilder()
                                             .setType(FacilitatorType.newBuilder().setType(facilitatorType).build())
                                             .build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(dummyNodeId)
                  .name("Dummy Node 1")
                  .identifier("dummy")
                  .stepType(DUMMY_STEP_TYPE)
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.SYNC).build())
                          .build())
                  .build())
        .startingNodeId(sectionNodeId)
        .build();
  }

  public Plan provideSectionChainPlan() {
    String sectionChainNodeId = generateUuid();
    String httpNode1Id = generateUuid();
    String httpNode2Id = generateUuid();
    String dummyNode1Id = generateUuid();
    String dummyNode2Id = generateUuid();

    BasicHttpStepParameters basicHttpStateParameters1 =
        BasicHttpStepParameters.builder().url(getMockServerUrl() + BASIC_HTTP_STATE_URL_200).method("GET").build();

    BasicHttpStepParameters basicHttpStateParameters2 =
        BasicHttpStepParameters.builder().url(getMockServerUrl() + BASIC_HTTP_STATE_URL_404).method("GET").build();
    return Plan.builder()
        .node(
            PlanNode.builder()
                .uuid(sectionChainNodeId)
                .name("Section Chain")
                .stepType(SectionChainStep.STEP_TYPE)
                .identifier("section_chain")
                .stepParameters(
                    SectionChainStepParameters.builder().childNodeId(httpNode1Id).childNodeId(httpNode2Id).build())
                .facilitatorObtainment(
                    FacilitatorObtainment.newBuilder()
                        .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILD_CHAIN).build())
                        .build())
                .build())
        .node(PlanNode.builder()
                  .uuid(httpNode1Id)
                  .name("HTTP 1")
                  .stepType(BasicHttpStep.STEP_TYPE)
                  .identifier("http1")
                  .stepParameters(basicHttpStateParameters1)
                  .adviserObtainment(AdviserObtainment.newBuilder()
                                         .setType(OnSuccessAdviser.ADVISER_TYPE)
                                         .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                                             OnSuccessAdviserParameters.builder().nextNodeId(dummyNode1Id).build())))
                                         .build())
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.TASK).build())
                          .build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(httpNode2Id)
                  .name("HTTP 2")
                  .stepType(BasicHttpStep.STEP_TYPE)
                  .identifier("http2")
                  .stepParameters(basicHttpStateParameters2)
                  .adviserObtainment(AdviserObtainment.newBuilder()
                                         .setType(OnSuccessAdviser.ADVISER_TYPE)
                                         .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                                             OnSuccessAdviserParameters.builder().nextNodeId(dummyNode2Id).build())))
                                         .build())
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.TASK).build())
                          .build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(dummyNode1Id)
                  .name("Dummy Node 1")
                  .stepType(DUMMY_STEP_TYPE)
                  .identifier("dummy1")
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.SYNC).build())
                          .build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(dummyNode2Id)
                  .name("Dummy Node 2")
                  .stepType(DUMMY_STEP_TYPE)
                  .identifier("dummy2")
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.SYNC).build())
                          .build())
                  .build())
        .startingNodeId(sectionChainNodeId)
        .build();
  }

  public Plan provideSectionChainPlanWithFailure() {
    String sectionChainNodeId = generateUuid();
    String httpNode1Id = generateUuid();
    String httpNode2Id = generateUuid();
    String httpNode3Id = generateUuid();
    String dummyNode1Id = generateUuid();
    String dummyNode2Id = generateUuid();

    BasicHttpStepParameters basicHttpStateParameters1 =
        BasicHttpStepParameters.builder().url(getMockServerUrl() + BASIC_HTTP_STATE_URL_200).method("GET").build();

    BasicHttpStepParameters basicHttpStateParameters2 =
        BasicHttpStepParameters.builder().url(getMockServerUrl() + BASIC_HTTP_STATE_URL_404).method("GET").build();

    BasicHttpStepParameters basicHttpStateParameters3 =
        BasicHttpStepParameters.builder().url(getMockServerUrl() + BASIC_HTTP_STATE_URL_500).method("GET").build();
    return Plan.builder()
        .node(
            PlanNode.builder()
                .uuid(sectionChainNodeId)
                .name("Section Chain")
                .stepType(SectionChainStep.STEP_TYPE)
                .identifier("section_chain")
                .stepParameters(
                    SectionChainStepParameters.builder().childNodeId(httpNode1Id).childNodeId(httpNode2Id).build())
                .facilitatorObtainment(
                    FacilitatorObtainment.newBuilder()
                        .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILD_CHAIN).build())
                        .build())
                .build())
        .node(PlanNode.builder()
                  .uuid(httpNode1Id)
                  .name("HTTP 1")
                  .stepType(BasicHttpStep.STEP_TYPE)
                  .identifier("http1")
                  .stepParameters(basicHttpStateParameters1)
                  .adviserObtainment(AdviserObtainment.newBuilder()
                                         .setType(OnSuccessAdviser.ADVISER_TYPE)
                                         .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                                             OnSuccessAdviserParameters.builder().nextNodeId(httpNode3Id).build())))
                                         .build())
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.TASK).build())
                          .build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(httpNode3Id)
                  .name("HTTP 3")
                  .stepType(BasicHttpStep.STEP_TYPE)
                  .identifier("http3")
                  .stepParameters(basicHttpStateParameters3)
                  .adviserObtainment(AdviserObtainment.newBuilder()
                                         .setType(OnSuccessAdviser.ADVISER_TYPE)
                                         .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                                             OnSuccessAdviserParameters.builder().nextNodeId(dummyNode1Id).build())))
                                         .build())
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.TASK).build())
                          .build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(httpNode2Id)
                  .name("HTTP 2")
                  .stepType(BasicHttpStep.STEP_TYPE)
                  .identifier("http2")
                  .stepParameters(basicHttpStateParameters2)
                  .adviserObtainment(AdviserObtainment.newBuilder()
                                         .setType(OnSuccessAdviser.ADVISER_TYPE)
                                         .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                                             OnSuccessAdviserParameters.builder().nextNodeId(dummyNode2Id).build())))
                                         .build())
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.TASK).build())
                          .build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(dummyNode1Id)
                  .name("Dummy Node 1")
                  .stepType(DUMMY_STEP_TYPE)
                  .identifier("dummy1")
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.SYNC).build())
                          .build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(dummyNode2Id)
                  .name("Dummy Node 2")
                  .stepType(DUMMY_STEP_TYPE)
                  .identifier("dummy2")
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.SYNC).build())
                          .build())
                  .build())
        .startingNodeId(sectionChainNodeId)
        .build();
  }

  public Plan provideSectionChainPlanWithNoChildren() {
    String sectionChainNodeId = generateUuid();
    String dummyNode1Id = generateUuid();

    return Plan.builder()
        .node(
            PlanNode.builder()
                .uuid(sectionChainNodeId)
                .name("Section Chain")
                .stepType(SectionChainStep.STEP_TYPE)
                .identifier("section_chain")
                .stepParameters(SectionChainStepParameters.builder().build())
                .facilitatorObtainment(
                    FacilitatorObtainment.newBuilder()
                        .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILD_CHAIN).build())
                        .build())
                .adviserObtainment(AdviserObtainment.newBuilder()
                                       .setType(OnSuccessAdviser.ADVISER_TYPE)
                                       .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                                           OnSuccessAdviserParameters.builder().nextNodeId(dummyNode1Id).build())))
                                       .build())
                .build())
        .node(PlanNode.builder()
                  .uuid(dummyNode1Id)
                  .name("Dummy Node 1")
                  .stepType(DUMMY_STEP_TYPE)
                  .identifier("dummy1")
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.SYNC).build())
                          .build())
                  .build())
        .startingNodeId(sectionChainNodeId)
        .build();
  }

  public Plan provideSectionChainRollbackPlan() {
    String sectionChainNodeId = generateUuid();
    String httpNode1Id = generateUuid();
    String httpNode2Id = generateUuid();
    String httpNode3Id = generateUuid();
    String dummyNode1Id = generateUuid();
    String dummyNode2Id = generateUuid();

    BasicHttpStepParameters basicHttpStateParameters1 =
        BasicHttpStepParameters.builder().url(getMockServerUrl() + BASIC_HTTP_STATE_URL_200).method("GET").build();

    BasicHttpStepParameters basicHttpStateParameters2 =
        BasicHttpStepParameters.builder().url(getMockServerUrl() + BASIC_HTTP_STATE_URL_500).method("GET").build();

    BasicHttpStepParameters basicHttpStateParameters3 =
        BasicHttpStepParameters.builder().url(getMockServerUrl() + BASIC_HTTP_STATE_URL_404).method("GET").build();
    return Plan.builder()
        .node(
            PlanNode.builder()
                .uuid(sectionChainNodeId)
                .name("Section Chain")
                .stepType(SectionChainStep.STEP_TYPE)
                .identifier("section_chain")
                .adviserObtainment(AdviserObtainment.newBuilder()
                                       .setType(OnSuccessAdviser.ADVISER_TYPE)
                                       .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                                           OnSuccessAdviserParameters.builder().nextNodeId(dummyNode1Id).build())))
                                       .build())
                .adviserObtainment(AdviserObtainment.newBuilder()
                                       .setType(OnFailAdviser.ADVISER_TYPE)
                                       .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                                           OnFailAdviserParameters.builder().nextNodeId(dummyNode2Id).build())))
                                       .build())
                .stepParameters(SectionChainStepParameters.builder()
                                    .childNodeId(httpNode1Id)
                                    .childNodeId(httpNode2Id)
                                    .childNodeId(httpNode3Id)
                                    .build())
                .facilitatorObtainment(
                    FacilitatorObtainment.newBuilder()
                        .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILD_CHAIN).build())
                        .build())
                .build())
        .node(PlanNode.builder()
                  .uuid(httpNode1Id)
                  .name("HTTP 1")
                  .stepType(BasicHttpStep.STEP_TYPE)
                  .identifier("http1")
                  .stepParameters(basicHttpStateParameters1)
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.TASK).build())
                          .build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(httpNode2Id)
                  .name("HTTP 2")
                  .stepType(BasicHttpStep.STEP_TYPE)
                  .identifier("http2")
                  .stepParameters(basicHttpStateParameters2)
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.TASK).build())
                          .build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(httpNode3Id)
                  .name("HTTP 3")
                  .stepType(BasicHttpStep.STEP_TYPE)
                  .identifier("http3")
                  .stepParameters(basicHttpStateParameters3)
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.TASK).build())
                          .build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(dummyNode1Id)
                  .name("Dummy Node 1")
                  .stepType(DUMMY_STEP_TYPE)
                  .identifier("dummy1")
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.SYNC).build())
                          .build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(dummyNode2Id)
                  .name("Dummy Node 2")
                  .stepType(DUMMY_STEP_TYPE)
                  .identifier("dummy2")
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.SYNC).build())
                          .build())
                  .build())
        .startingNodeId(sectionChainNodeId)
        .build();
  }

  /*
  infra section
    env state
    infra state
    shell script
  shell script
   */

  public Plan provideGraphTestPlan() {
    String dummyStartNode = generateUuid();
    String forkId = generateUuid();
    String section1Id = generateUuid();
    String section2Id = generateUuid();
    String dummuNodeId = generateUuid();
    String forkId2 = generateUuid();
    String httpSwitchId = generateUuid();
    String http1Id = generateUuid();
    String http2Id = generateUuid();
    String dummyNode1Id = generateUuid();
    String dummyNode2Id = generateUuid();
    StepParameters basicHttpStepParameters1 =
        BasicHttpStepParameters.builder().url(getMockServerUrl() + BASIC_HTTP_STATE_URL_200).method("GET").build();
    return Plan.builder()
        .startingNodeId(dummyStartNode)
        .node(
            PlanNode.builder()
                .uuid(dummyStartNode)
                .identifier("dummy-start")
                .name("dummy-start")
                .stepType(DUMMY_STEP_TYPE)
                .adviserObtainment(
                    AdviserObtainment.newBuilder()
                        .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.ON_SUCCESS.name()).build())
                        .setParameters(ByteString.copyFrom(
                            kryoSerializer.asBytes(OnSuccessAdviserParameters.builder().nextNodeId(forkId).build())))
                        .build())
                .facilitatorObtainment(
                    FacilitatorObtainment.newBuilder()
                        .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.SYNC).build())
                        .build())
                .build())
        .node(
            PlanNode.builder()
                .uuid(forkId)
                .identifier("fork1")
                .name("fork1")
                .stepType(ForkStep.STEP_TYPE)
                .stepParameters(
                    ForkStepParameters.builder().parallelNodeId(section1Id).parallelNodeId(section2Id).build())
                .adviserObtainment(
                    AdviserObtainment.newBuilder()
                        .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.ON_SUCCESS.name()).build())
                        .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                            OnSuccessAdviserParameters.builder().nextNodeId(dummuNodeId).build())))
                        .build())
                .facilitatorObtainment(
                    FacilitatorObtainment.newBuilder()
                        .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILDREN).build())
                        .build())
                .build())
        .node(PlanNode.builder()
                  .uuid(section1Id)
                  .identifier("section1")
                  .name("section1")
                  .stepType(SectionStep.STEP_TYPE)
                  .stepParameters(SectionStepParameters.builder().childNodeId(forkId2).build())
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILD).build())
                          .build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(section2Id)
                  .identifier("section2")
                  .name("section2")
                  .stepType(SectionStep.STEP_TYPE)
                  .stepParameters(SectionStepParameters.builder().childNodeId(httpSwitchId).build())
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILD).build())
                          .build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(forkId2)
                  .identifier("fork2")
                  .name("fork2")
                  .stepType(ForkStep.STEP_TYPE)
                  .stepParameters(ForkStepParameters.builder().parallelNodeId(http1Id).parallelNodeId(http2Id).build())
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILDREN).build())
                          .build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(httpSwitchId)
                  .identifier("http-switch")
                  .name("http-switch")
                  .stepType(BASIC_HTTP_STEP_TYPE)
                  .stepParameters(basicHttpStepParameters1)
                  .adviserObtainment(AdviserObtainment.newBuilder()
                                         .setType(HttpResponseCodeSwitchAdviser.ADVISER_TYPE)
                                         .setParameters(ByteString.copyFrom(
                                             kryoSerializer.asBytes(HttpResponseCodeSwitchAdviserParameters.builder()
                                                                        .responseCodeNodeIdMapping(200, dummyNode1Id)
                                                                        .responseCodeNodeIdMapping(404, dummyNode2Id)
                                                                        .build())))
                                         .build())
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.TASK).build())
                          .build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(http1Id)
                  .identifier("http1")
                  .name("http1")
                  .stepType(BASIC_HTTP_STEP_TYPE)
                  .stepParameters(basicHttpStepParameters1)
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.TASK).build())
                          .build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(http2Id)
                  .identifier("http2")
                  .name("http2")
                  .stepType(BASIC_HTTP_STEP_TYPE)
                  .stepParameters(basicHttpStepParameters1)
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.TASK).build())
                          .build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(dummyNode1Id)
                  .identifier("dummy1")
                  .name("dummy1")
                  .stepType(DUMMY_STEP_TYPE)
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.SYNC).build())
                          .build())
                  .refObject(RefObjectUtils.getOutcomeRefObject("http", httpSwitchId, null))
                  .build())
        .node(PlanNode.builder()
                  .uuid(dummyNode2Id)
                  .identifier("dummy2")
                  .name("dummy2")
                  .stepType(DUMMY_STEP_TYPE)
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.SYNC).build())
                          .build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(dummuNodeId)
                  .identifier("dummy-final")
                  .name("dummy-final")
                  .stepType(DUMMY_STEP_TYPE)
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.SYNC).build())
                          .build())
                  .build())
        .build();
  }

  public Plan providePlanWithSingleBarrier() {
    String forkNodeId = generateUuid();
    String dummyNode1Id = generateUuid();
    String dummyNode2Id = generateUuid();
    String barrierNodeId = generateUuid();
    return Plan.builder()
        .startingNodeId(forkNodeId)
        .node(PlanNode.builder()
                  .uuid(forkNodeId)
                  .name("Fork Node")
                  .stepType(ForkStep.STEP_TYPE)
                  .identifier("fork1")
                  .stepParameters(
                      ForkStepParameters.builder().parallelNodeId(dummyNode1Id).parallelNodeId(dummyNode2Id).build())
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILDREN).build())
                          .build())
                  .build())
        .node(
            PlanNode.builder()
                .uuid(dummyNode1Id)
                .name("Dummy Node 1")
                .stepType(DUMMY_STEP_TYPE)
                .identifier("dummy1")
                .adviserObtainment(
                    AdviserObtainment.newBuilder()
                        .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.ON_SUCCESS.name()).build())
                        .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                            OnSuccessAdviserParameters.builder().nextNodeId(barrierNodeId).build())))
                        .build())
                .facilitatorObtainment(
                    FacilitatorObtainment.newBuilder()
                        .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.SYNC).build())
                        .build())
                .build())
        .node(PlanNode.builder()
                  .uuid(dummyNode2Id)
                  .name("Dummy Node 2")
                  .stepType(DUMMY_STEP_TYPE)
                  .identifier("dummy2")
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.SYNC).build())
                          .build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(barrierNodeId)
                  .identifier("barrier1")
                  .name("barrier1")
                  .stepType(BarrierStep.STEP_TYPE)
                  .stepParameters(BarrierStepParameters.builder().identifier("BAR1").build())
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.BARRIER).build())
                          .build())
                  .timeoutObtainment(TimeoutObtainment.newBuilder()
                                         .setDimension(AbsoluteTimeoutTrackerFactory.DIMENSION)
                                         .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                                             AbsoluteTimeoutParameters.builder().timeoutMillis(100000).build())))
                                         .build())
                  .build())
        .build();
  }

  public Plan providePlanWithMultipleBarriers() {
    String forkNodeId = generateUuid();
    String dummyNodeId1 = generateUuid();
    String dummyNodeId2 = generateUuid();
    String dummyNodeId3 = generateUuid();
    String dummyNodeId4 = generateUuid();
    String barrierNodeId1 = generateUuid();
    String barrierNodeId2 = generateUuid();
    String barrierNodeId3 = generateUuid();
    String waitNodeId = generateUuid();
    return Plan.builder()
        .startingNodeId(forkNodeId)
        .node(PlanNode.builder()
                  .uuid(forkNodeId)
                  .name("Fork Node")
                  .stepType(ForkStep.STEP_TYPE)
                  .identifier("fork1")
                  .stepParameters(
                      ForkStepParameters.builder().parallelNodeId(dummyNodeId1).parallelNodeId(dummyNodeId2).build())
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILDREN).build())
                          .build())
                  .build())
        .node(
            PlanNode.builder()
                .uuid(dummyNodeId1)
                .name("Dummy Node 1")
                .stepType(DUMMY_STEP_TYPE)
                .identifier("dummy1")
                .adviserObtainment(
                    AdviserObtainment.newBuilder()
                        .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.ON_SUCCESS.name()).build())
                        .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                            OnSuccessAdviserParameters.builder().nextNodeId(barrierNodeId1).build())))
                        .build())
                .facilitatorObtainment(
                    FacilitatorObtainment.newBuilder()
                        .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.SYNC).build())
                        .build())
                .build())
        .node(
            PlanNode.builder()
                .uuid(dummyNodeId2)
                .name("Dummy Node 2")
                .stepType(DUMMY_STEP_TYPE)
                .identifier("dummy2")
                .facilitatorObtainment(
                    FacilitatorObtainment.newBuilder()
                        .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.SYNC).build())
                        .build())
                .adviserObtainment(
                    AdviserObtainment.newBuilder()
                        .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.ON_SUCCESS.name()).build())
                        .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                            OnSuccessAdviserParameters.builder().nextNodeId(waitNodeId).build())))
                        .build())
                .build())
        .node(
            PlanNode.builder()
                .uuid(waitNodeId)
                .name("Wait Node")
                .identifier("wait")
                .stepType(StepType.newBuilder().setType("WAIT_STATE").build())
                .stepParameters(WaitStepParameters.builder().waitDurationSeconds(5).build())
                .facilitatorObtainment(
                    FacilitatorObtainment.newBuilder()
                        .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.ASYNC).build())
                        .build())
                .adviserObtainment(
                    AdviserObtainment.newBuilder()
                        .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.ON_SUCCESS.name()).build())
                        .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                            OnSuccessAdviserParameters.builder().nextNodeId(barrierNodeId2).build())))
                        .build())
                .build())
        .node(
            PlanNode.builder()
                .uuid(barrierNodeId1)
                .identifier("barrier1")
                .name("barrier1")
                .stepType(BarrierStep.STEP_TYPE)
                .stepParameters(BarrierStepParameters.builder().identifier("BAR1").build())
                .facilitatorObtainment(
                    FacilitatorObtainment.newBuilder()
                        .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.BARRIER).build())
                        .build())
                .adviserObtainment(
                    AdviserObtainment.newBuilder()
                        .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.ON_SUCCESS.name()).build())
                        .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                            OnSuccessAdviserParameters.builder().nextNodeId(dummyNodeId3).build())))
                        .build())
                .timeoutObtainment(TimeoutObtainment.newBuilder()
                                       .setDimension(AbsoluteTimeoutTrackerFactory.DIMENSION)
                                       .setParameters(ByteString.copyFrom(
                                           kryoSerializer.asBytes(AbsoluteTimeoutParameters.builder()
                                                                      .timeoutMillis(Duration.ofMinutes(10).toMillis())
                                                                      .build())))
                                       .build())
                .build())
        .node(
            PlanNode.builder()
                .uuid(barrierNodeId2)
                .identifier("barrier2")
                .name("barrier2")
                .stepType(BarrierStep.STEP_TYPE)
                .stepParameters(BarrierStepParameters.builder().identifier("BAR1").build())
                .facilitatorObtainment(
                    FacilitatorObtainment.newBuilder()
                        .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.BARRIER).build())
                        .build())
                .adviserObtainment(
                    AdviserObtainment.newBuilder()
                        .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.ON_SUCCESS.name()).build())
                        .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                            OnSuccessAdviserParameters.builder().nextNodeId(barrierNodeId3).build())))
                        .build())
                .timeoutObtainment(TimeoutObtainment.newBuilder()
                                       .setDimension(AbsoluteTimeoutTrackerFactory.DIMENSION)
                                       .setParameters(ByteString.copyFrom(
                                           kryoSerializer.asBytes(AbsoluteTimeoutParameters.builder()
                                                                      .timeoutMillis(Duration.ofMinutes(10).toMillis())
                                                                      .build())))
                                       .build())
                .build())
        .node(
            PlanNode.builder()
                .uuid(barrierNodeId3)
                .identifier("barrier3")
                .name("barrier3")
                .stepType(BarrierStep.STEP_TYPE)
                .stepParameters(BarrierStepParameters.builder().identifier("BAR2").build())
                .facilitatorObtainment(
                    FacilitatorObtainment.newBuilder()
                        .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.BARRIER).build())
                        .build())
                .adviserObtainment(
                    AdviserObtainment.newBuilder()
                        .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.ON_SUCCESS.name()).build())
                        .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                            OnSuccessAdviserParameters.builder().nextNodeId(dummyNodeId4).build())))
                        .build())
                .timeoutObtainment(TimeoutObtainment.newBuilder()
                                       .setDimension(AbsoluteTimeoutTrackerFactory.DIMENSION)
                                       .setParameters(ByteString.copyFrom(
                                           kryoSerializer.asBytes(AbsoluteTimeoutParameters.builder()
                                                                      .timeoutMillis(Duration.ofMinutes(10).toMillis())
                                                                      .build())))
                                       .build())
                .build())
        .node(PlanNode.builder()
                  .uuid(dummyNodeId3)
                  .name("Dummy Node 3")
                  .stepType(DUMMY_STEP_TYPE)
                  .identifier("dummy3")
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.SYNC).build())
                          .build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(dummyNodeId4)
                  .name("Dummy Node 4")
                  .stepType(DUMMY_STEP_TYPE)
                  .identifier("dummy4")
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.SYNC).build())
                          .build())
                  .build())
        .build();
  }

  public Plan provideResourceRestraintPlan() {
    String dummyNode1Id = generateUuid();
    String dummyNode2Id = generateUuid();
    String resourceRestraintInstanceId = generateUuid();
    String complaintId = "kmpySmUISimoRrJL6NL73w";
    String resourceRestraintId = "TIIa8fwtQD2G6hbE2PzWBQ";
    return Plan.builder()
        .startingNodeId(dummyNode1Id)
        .node(
            PlanNode.builder()
                .uuid(dummyNode1Id)
                .name("Dummy Node 1")
                .stepType(DUMMY_STEP_TYPE)
                .identifier("dummy1")
                .adviserObtainment(
                    AdviserObtainment.newBuilder()
                        .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.ON_SUCCESS.name()).build())
                        .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                            OnSuccessAdviserParameters.builder().nextNodeId(resourceRestraintInstanceId).build())))
                        .build())
                .facilitatorObtainment(
                    FacilitatorObtainment.newBuilder()
                        .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.SYNC).build())
                        .build())
                .build())
        .node(
            PlanNode.builder()
                .uuid(resourceRestraintInstanceId)
                .identifier("resourceRestraint1")
                .name("resourceRestraint1")
                .stepType(ResourceRestraintStep.STEP_TYPE)
                .stepParameters(ResourceRestraintStepParameters.builder()
                                    .claimantId(complaintId)
                                    .permits(1)
                                    .resourceUnit(RESOURCE_UNIT)
                                    .resourceRestraintId(resourceRestraintId)
                                    .acquireMode(AcquireMode.ACCUMULATE)
                                    .holdingScope(HoldingScopeBuilder.aPlan().build())
                                    .build())
                .adviserObtainment(
                    AdviserObtainment.newBuilder()
                        .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.ON_SUCCESS.name()).build())
                        .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                            OnSuccessAdviserParameters.builder().nextNodeId(dummyNode2Id).build())))
                        .build())
                .facilitatorObtainment(FacilitatorObtainment.newBuilder()
                                           .setType(FacilitatorType.newBuilder()
                                                        .setType(OrchestrationFacilitatorType.RESOURCE_RESTRAINT)
                                                        .build())
                                           .build())
                .build())
        .node(PlanNode.builder()
                  .uuid(dummyNode2Id)
                  .name("Dummy Node 2")
                  .stepType(DUMMY_STEP_TYPE)
                  .identifier("dummy2")
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.SYNC).build())
                          .build())
                  .build())
        .build();
  }

  public Plan provideResourceRestraintWithWaitPlan() {
    String dummyNode1Id = generateUuid();
    String dummyNode2Id = generateUuid();
    String resourceRestraintInstanceId = generateUuid();
    String waitNodeId = generateUuid();
    String complaintId = "kmpySmUISimoRrJL6NL73w";
    String resourceRestraintId = "TIIa8fwtQD2G6hbE2PzWBQ";
    return Plan.builder()
        .startingNodeId(dummyNode1Id)
        .node(
            PlanNode.builder()
                .uuid(dummyNode1Id)
                .name("Dummy Node 1")
                .stepType(DUMMY_STEP_TYPE)
                .identifier("dummy1")
                .adviserObtainment(
                    AdviserObtainment.newBuilder()
                        .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.ON_SUCCESS.name()).build())
                        .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                            OnSuccessAdviserParameters.builder().nextNodeId(resourceRestraintInstanceId).build())))
                        .build())
                .facilitatorObtainment(
                    FacilitatorObtainment.newBuilder()
                        .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.SYNC).build())
                        .build())
                .build())
        .node(
            PlanNode.builder()
                .uuid(resourceRestraintInstanceId)
                .identifier("resourceRestraint2")
                .name("resourceRestraint2")
                .stepType(ResourceRestraintStep.STEP_TYPE)
                .stepParameters(ResourceRestraintStepParameters.builder()
                                    .claimantId(complaintId)
                                    .permits(1)
                                    .resourceUnit(RESOURCE_UNIT)
                                    .resourceRestraintId(resourceRestraintId)
                                    .acquireMode(AcquireMode.ACCUMULATE)
                                    .holdingScope(HoldingScopeBuilder.aPlan().build())
                                    .build())
                .adviserObtainment(
                    AdviserObtainment.newBuilder()
                        .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.ON_SUCCESS.name()).build())
                        .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                            OnSuccessAdviserParameters.builder().nextNodeId(waitNodeId).build())))
                        .build())
                .facilitatorObtainment(FacilitatorObtainment.newBuilder()
                                           .setType(FacilitatorType.newBuilder()
                                                        .setType(OrchestrationFacilitatorType.RESOURCE_RESTRAINT)
                                                        .build())
                                           .build())
                .build())
        .node(
            PlanNode.builder()
                .uuid(waitNodeId)
                .name("Wait Node")
                .identifier("wait")
                .stepType(StepType.newBuilder().setType("WAIT_STATE").build())
                .stepParameters(WaitStepParameters.builder().waitDurationSeconds(50).build())
                .facilitatorObtainment(
                    FacilitatorObtainment.newBuilder()
                        .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.ASYNC).build())
                        .build())
                .adviserObtainment(
                    AdviserObtainment.newBuilder()
                        .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.ON_SUCCESS.name()).build())
                        .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                            OnSuccessAdviserParameters.builder().nextNodeId(dummyNode2Id).build())))
                        .build())
                .build())
        .node(PlanNode.builder()
                  .uuid(dummyNode2Id)
                  .name("Dummy Node 2")
                  .stepType(DUMMY_STEP_TYPE)
                  .identifier("dummy2")
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.SYNC).build())
                          .build())
                  .build())
        .build();
  }

  public Plan provideGraphTestPlanWithSkippedNodes() {
    String dummyStartNode = generateUuid();
    String forkId = generateUuid();
    String section1Id = generateUuid();
    String section2Id = generateUuid();
    String dummuNodeId = generateUuid();
    String forkId2 = generateUuid();
    String forkId3 = generateUuid();
    String dummyNode1Id = generateUuid();
    String dummyNode2Id = generateUuid();
    String dummyNode3Id = generateUuid();
    String dummyNode4Id = generateUuid();
    return Plan.builder()
        .startingNodeId(dummyStartNode)
        .node(
            PlanNode.builder()
                .uuid(dummyStartNode)
                .identifier("dummy-start")
                .name("dummy-start")
                .stepType(DUMMY_STEP_TYPE)
                .adviserObtainment(
                    AdviserObtainment.newBuilder()
                        .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.ON_SUCCESS.name()).build())
                        .setParameters(ByteString.copyFrom(
                            kryoSerializer.asBytes(OnSuccessAdviserParameters.builder().nextNodeId(forkId).build())))
                        .build())
                .facilitatorObtainment(
                    FacilitatorObtainment.newBuilder()
                        .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.SYNC).build())
                        .build())
                .build())
        .node(
            PlanNode.builder()
                .uuid(forkId)
                .identifier("fork1")
                .name("fork1")
                .stepType(ForkStep.STEP_TYPE)
                .stepParameters(
                    ForkStepParameters.builder().parallelNodeId(section1Id).parallelNodeId(section2Id).build())
                .adviserObtainment(
                    AdviserObtainment.newBuilder()
                        .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.ON_SUCCESS.name()).build())
                        .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                            OnSuccessAdviserParameters.builder().nextNodeId(dummuNodeId).build())))
                        .build())
                .facilitatorObtainment(
                    FacilitatorObtainment.newBuilder()
                        .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILDREN).build())
                        .build())
                .build())
        .node(PlanNode.builder()
                  .uuid(section1Id)
                  .identifier("section1")
                  .name("section1")
                  .stepType(SectionStep.STEP_TYPE)
                  .stepParameters(SectionStepParameters.builder().childNodeId(forkId2).build())
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILD).build())
                          .build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(section2Id)
                  .identifier("section2")
                  .name("section2")
                  .stepType(SectionStep.STEP_TYPE)
                  .stepParameters(SectionStepParameters.builder().childNodeId(forkId3).build())
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILD).build())
                          .build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(forkId2)
                  .identifier("fork2")
                  .name("fork2")
                  .stepType(ForkStep.STEP_TYPE)
                  .skipGraphType(SkipType.SKIP_TREE)
                  .stepParameters(
                      ForkStepParameters.builder().parallelNodeId(dummyNode3Id).parallelNodeId(dummyNode4Id).build())
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILDREN).build())
                          .build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(forkId3)
                  .identifier("fork3")
                  .name("fork3")
                  .stepType(ForkStep.STEP_TYPE)
                  .skipGraphType(SkipType.SKIP_NODE)
                  .stepParameters(
                      ForkStepParameters.builder().parallelNodeId(dummyNode1Id).parallelNodeId(dummyNode2Id).build())
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILDREN).build())
                          .build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(dummyNode3Id)
                  .identifier("dummy3")
                  .name("dummy3")
                  .stepType(DUMMY_STEP_TYPE)
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.SYNC).build())
                          .build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(dummyNode4Id)
                  .identifier("dummy4")
                  .name("dummy4")
                  .stepType(DUMMY_STEP_TYPE)
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.SYNC).build())
                          .build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(dummyNode1Id)
                  .identifier("dummy1")
                  .name("dummy1")
                  .stepType(DUMMY_STEP_TYPE)
                  .skipGraphType(SkipType.SKIP_NODE)
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.SYNC).build())
                          .build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(dummyNode2Id)
                  .identifier("dummy2")
                  .name("dummy2")
                  .stepType(DUMMY_STEP_TYPE)
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.SYNC).build())
                          .build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(dummuNodeId)
                  .identifier("dummy-final")
                  .name("dummy-final")
                  .stepType(DUMMY_STEP_TYPE)
                  .facilitatorObtainment(
                      FacilitatorObtainment.newBuilder()
                          .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.SYNC).build())
                          .build())
                  .build())
        .build();
  }

  private String getMockServerUrl() {
    MockServerConfig mockServerConfig = configuration.getMockServerConfig();
    return mockServerConfig.getBaseUrl() + ":" + mockServerConfig.getPort() + '/';
  }

  public Plan getSkipChildrenPlan() {
    PlanNode httpAdvisor =
        PlanNode.builder()
            .uuid(generateUuid())
            .name("http4")
            .identifier("http4")
            .stepType(BasicHttpStep.STEP_TYPE)
            .stepParameters(BasicHttpStepParameters.builder().url("http://httpstat.us/201").method("GET").build())
            .facilitatorObtainment(
                FacilitatorObtainment.newBuilder()
                    .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.TASK).build())
                    .build())
            .build();

    PlanNode http1 =
        PlanNode.builder()
            .uuid(generateUuid())
            .name("Http1")
            .identifier("http1")
            .stepType(BasicHttpStep.STEP_TYPE)
            .stepParameters(BasicHttpStepParameters.builder().url("http://httpstat.us/200").method("GET").build())
            .facilitatorObtainment(
                FacilitatorObtainment.newBuilder()
                    .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.TASK).build())
                    .build())
            .build();

    PlanNode http2 =
        PlanNode.builder()
            .uuid(generateUuid())
            .name("http2")
            .identifier("http2")
            .stepType(BasicHttpStep.STEP_TYPE)
            .stepParameters(BasicHttpStepParameters.builder().url("wrong").method("GET").build())
            .facilitatorObtainment(
                FacilitatorObtainment.newBuilder()
                    .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.TASK).build())
                    .build())
            .adviserObtainment(
                AdviserObtainment.newBuilder()
                    .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.ON_FAIL.name()).build())
                    .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                        OnFailAdviserParameters.builder().nextNodeId(httpAdvisor.getUuid()).build())))
                    .build())
            .build();

    PlanNode http3 =
        PlanNode.builder()
            .uuid(generateUuid())
            .name("http3")
            .identifier("http3")
            .stepType(BasicHttpStep.STEP_TYPE)
            .stepParameters(BasicHttpStepParameters.builder().url("http://httpstat.us/202").method("GET").build())
            .facilitatorObtainment(
                FacilitatorObtainment.newBuilder()
                    .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.TASK).build())
                    .build())
            .adviserObtainment(
                AdviserObtainment.newBuilder()
                    .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.ON_FAIL.name()).build())
                    .setParameters(ByteString.copyFrom(
                        kryoSerializer.asBytes(OnFailAdviserParameters.builder().nextNodeId(http2.getUuid()).build())))
                    .build())
            .build();

    SectionChainStepParameters sectionChainStepParameters = SectionChainStepParameters.builder()
                                                                .childNodeId(http1.getUuid())
                                                                .childNodeId(http2.getUuid())
                                                                .childNodeId(http3.getUuid())
                                                                .build();

    PlanNode optionalSectionNode =
        PlanNode.builder()
            .uuid(generateUuid())
            .name("Parent Section Node")
            .identifier("parentSectionNode")
            .stepType(SectionChainStep.STEP_TYPE)
            .stepParameters(sectionChainStepParameters)
            .facilitatorObtainment(
                FacilitatorObtainment.newBuilder()
                    .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILD_CHAIN).build())
                    .build())
            .build();

    return Plan.builder()
        .node(http1)
        .node(http2)
        .node(http3)
        .node(httpAdvisor)
        .node(optionalSectionNode)
        .startingNodeId(optionalSectionNode.getUuid())
        .build();
  }
}
