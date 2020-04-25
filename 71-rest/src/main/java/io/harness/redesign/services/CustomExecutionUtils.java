package io.harness.redesign.services;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import com.google.common.collect.ImmutableMap;

import io.harness.adviser.AdviserObtainment;
import io.harness.adviser.AdviserType;
import io.harness.adviser.impl.success.OnSuccessAdviserParameters;
import io.harness.annotations.Redesign;
import io.harness.facilitate.FacilitatorObtainment;
import io.harness.facilitate.FacilitatorType;
import io.harness.plan.ExecutionNode;
import io.harness.plan.ExecutionPlan;
import io.harness.redesign.advisers.HttpResponseCodeSwitchAdviserParameters;
import io.harness.redesign.states.http.BasicHttpStateParameters;
import io.harness.redesign.states.wait.WaitStateParameters;
import io.harness.state.core.fork.ForkStateParameters;
import lombok.experimental.UtilityClass;
import software.wings.sm.StateType;

@Redesign
@UtilityClass
public class CustomExecutionUtils {
  private static final String BASIC_HTTP_STATE_URL_500 = "http://httpstat.us/500";
  private static final String BASIC_HTTP_STATE_URL_200 = "http://httpstat.us/200";

  public static ExecutionPlan provideHttpSwitchPlan() {
    String planId = generateUuid();
    String httpNodeId = generateUuid();
    String dummyNode1Id = generateUuid();
    String dummyNode2Id = generateUuid();
    String dummyNode3Id = generateUuid();
    String waitNodeId = generateUuid();

    String DUMMY_STATE_TYPE = "DUMMY";

    BasicHttpStateParameters basicHttpStateParameters =
        BasicHttpStateParameters.builder().url(BASIC_HTTP_STATE_URL_200).method("GET").build();
    return ExecutionPlan.builder()
        .node(ExecutionNode.builder()
                  .uuid(httpNodeId)
                  .name("Basic Http")
                  .stateType("BASIC_HTTP")
                  .stateParameters(basicHttpStateParameters)
                  .levelName("STEP")
                  .adviserObtainment(AdviserObtainment.builder()
                                         .type(AdviserType.builder().type("HTTP_RESPONSE_CODE_SWITCH").build())
                                         .parameters(HttpResponseCodeSwitchAdviserParameters.builder()
                                                         .responseCodeNodeIdMapping(200, dummyNode1Id)
                                                         .responseCodeNodeIdMapping(404, dummyNode2Id)
                                                         .responseCodeNodeIdMapping(500, dummyNode3Id)
                                                         .build())
                                         .build())
                  .facilitatorObtainment(FacilitatorObtainment.builder()
                                             .type(FacilitatorType.builder().type(FacilitatorType.ASYNC).build())
                                             .build())
                  .build())
        .node(
            ExecutionNode.builder()
                .uuid(dummyNode1Id)
                .name("Dummy Node 1")
                .stateType(DUMMY_STATE_TYPE)
                .levelName("STEP")
                .adviserObtainment(AdviserObtainment.builder()
                                       .type(AdviserType.builder().type(AdviserType.ON_SUCCESS).build())
                                       .parameters(OnSuccessAdviserParameters.builder().nextNodeId(waitNodeId).build())
                                       .build())
                .facilitatorObtainment(FacilitatorObtainment.builder()
                                           .type(FacilitatorType.builder().type(FacilitatorType.SYNC).build())
                                           .build())
                .build())
        .node(
            ExecutionNode.builder()
                .uuid(dummyNode2Id)
                .stateType(DUMMY_STATE_TYPE)
                .levelName("STEP")
                .adviserObtainment(AdviserObtainment.builder()
                                       .type(AdviserType.builder().type(AdviserType.ON_SUCCESS).build())
                                       .parameters(OnSuccessAdviserParameters.builder().nextNodeId(waitNodeId).build())
                                       .build())
                .facilitatorObtainment(FacilitatorObtainment.builder()
                                           .type(FacilitatorType.builder().type(FacilitatorType.SYNC).build())
                                           .build())
                .build())
        .node(
            ExecutionNode.builder()
                .uuid(dummyNode3Id)
                .name("Dummy Node 3")
                .levelName("STEP")
                .stateType(DUMMY_STATE_TYPE)
                .adviserObtainment(AdviserObtainment.builder()
                                       .type(AdviserType.builder().type(AdviserType.ON_SUCCESS).build())
                                       .parameters(OnSuccessAdviserParameters.builder().nextNodeId(waitNodeId).build())
                                       .build())
                .facilitatorObtainment(FacilitatorObtainment.builder()
                                           .type(FacilitatorType.builder().type(FacilitatorType.SYNC).build())
                                           .build())
                .build())
        .node(ExecutionNode.builder()
                  .uuid(waitNodeId)
                  .name("Wait Node")
                  .levelName("STEP")
                  .stateType(StateType.WAIT.name())
                  .stateParameters(WaitStateParameters.builder().waitDurationSeconds(5).build())
                  .facilitatorObtainment(FacilitatorObtainment.builder()
                                             .type(FacilitatorType.builder().type(FacilitatorType.ASYNC).build())
                                             .build())
                  .build())
        .startingNodeId(httpNodeId)
        .setupAbstractions(ImmutableMap.<String, String>builder()
                               .put("accountId", "kmpySmUISimoRrJL6NL73w")
                               .put("appId", "XEsfW6D_RJm1IaGpDidD3g")
                               .build())
        .uuid(planId)
        .build();
  }

  public static ExecutionPlan provideForkPlan() {
    String planId = generateUuid();
    String httpNodeId1 = generateUuid();
    String httpNodeId2 = generateUuid();
    String forkNodeId = generateUuid();
    String dummyNodeId = generateUuid();

    String DUMMY_STATE_TYPE = "DUMMY";

    BasicHttpStateParameters basicHttpStateParameters1 =
        BasicHttpStateParameters.builder().url(BASIC_HTTP_STATE_URL_200).method("GET").build();

    BasicHttpStateParameters basicHttpStateParameters2 =
        BasicHttpStateParameters.builder().url(BASIC_HTTP_STATE_URL_500).method("GET").build();
    return ExecutionPlan.builder()
        .node(ExecutionNode.builder()
                  .uuid(httpNodeId1)
                  .name("Basic Http")
                  .stateType("BASIC_HTTP")
                  .levelName("STEP")
                  .stateParameters(basicHttpStateParameters1)
                  .facilitatorObtainment(FacilitatorObtainment.builder()
                                             .type(FacilitatorType.builder().type(FacilitatorType.ASYNC).build())
                                             .build())
                  .build())
        .node(ExecutionNode.builder()
                  .uuid(httpNodeId2)
                  .name("Basic Http")
                  .stateType("BASIC_HTTP")
                  .levelName("STEP")
                  .stateParameters(basicHttpStateParameters2)
                  .facilitatorObtainment(FacilitatorObtainment.builder()
                                             .type(FacilitatorType.builder().type(FacilitatorType.ASYNC).build())
                                             .build())
                  .build())
        .node(
            ExecutionNode.builder()
                .uuid(forkNodeId)
                .name("FORK")
                .stateType("FORK")
                .levelName("FORK")
                .stateParameters(
                    ForkStateParameters.builder().parallelNodeId(httpNodeId1).parallelNodeId(httpNodeId2).build())
                .adviserObtainment(AdviserObtainment.builder()
                                       .type(AdviserType.builder().type(AdviserType.ON_SUCCESS).build())
                                       .parameters(OnSuccessAdviserParameters.builder().nextNodeId(dummyNodeId).build())
                                       .build())
                .facilitatorObtainment(FacilitatorObtainment.builder()
                                           .type(FacilitatorType.builder().type(FacilitatorType.CHILDREN).build())
                                           .build())
                .build())
        .node(ExecutionNode.builder()
                  .uuid(dummyNodeId)
                  .name("Dummy Node 1")
                  .levelName("STEP")
                  .stateType(DUMMY_STATE_TYPE)
                  .facilitatorObtainment(FacilitatorObtainment.builder()
                                             .type(FacilitatorType.builder().type(FacilitatorType.SYNC).build())
                                             .build())
                  .build())
        .startingNodeId(forkNodeId)
        .setupAbstractions(ImmutableMap.<String, String>builder()
                               .put("accountId", "kmpySmUISimoRrJL6NL73w")
                               .put("appId", "XEsfW6D_RJm1IaGpDidD3g")
                               .build())
        .uuid(planId)
        .build();
  }
}
