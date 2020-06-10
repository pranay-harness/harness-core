package io.harness.redesign.services;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import io.harness.adviser.AdviserObtainment;
import io.harness.adviser.AdviserType;
import io.harness.adviser.impl.fail.OnFailAdviserParameters;
import io.harness.adviser.impl.retry.RetryAdviserParameters;
import io.harness.adviser.impl.success.OnSuccessAdviser;
import io.harness.adviser.impl.success.OnSuccessAdviserParameters;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.artifact.bean.ArtifactConfigWrapper;
import io.harness.cdng.artifact.bean.yaml.DockerHubArtifactConfig;
import io.harness.cdng.artifact.steps.ArtifactStep;
import io.harness.cdng.artifact.steps.ArtifactStepParameters;
import io.harness.cdng.infra.beans.K8sDirectInfraDefinition;
import io.harness.cdng.infra.steps.InfrastructureSectionStep;
import io.harness.cdng.infra.steps.InfrastructureStep;
import io.harness.cdng.pipeline.CDPipeline;
import io.harness.cdng.pipeline.DeploymentStage;
import io.harness.cdng.pipeline.PipelineInfrastructure;
import io.harness.cdng.pipeline.beans.CDPipelineSetupParameters;
import io.harness.cdng.pipeline.steps.PipelineSetupStep;
import io.harness.cdng.service.Service;
import io.harness.delegate.task.shell.ScriptType;
import io.harness.facilitator.FacilitatorObtainment;
import io.harness.facilitator.FacilitatorType;
import io.harness.interrupts.RepairActionCode;
import io.harness.plan.Plan;
import io.harness.plan.PlanNode;
import io.harness.redesign.advisers.HttpResponseCodeSwitchAdviser;
import io.harness.redesign.advisers.HttpResponseCodeSwitchAdviserParameters;
import io.harness.redesign.states.email.EmailStep;
import io.harness.redesign.states.email.EmailStepParameters;
import io.harness.redesign.states.http.BasicHttpStep;
import io.harness.redesign.states.http.BasicHttpStepParameters;
import io.harness.redesign.states.http.chain.BasicHttpChainStep;
import io.harness.redesign.states.http.chain.BasicHttpChainStepParameters;
import io.harness.redesign.states.shell.ShellScriptStep;
import io.harness.redesign.states.shell.ShellScriptStepParameters;
import io.harness.redesign.states.wait.WaitStep;
import io.harness.redesign.states.wait.WaitStepParameters;
import io.harness.references.OutcomeRefObject;
import io.harness.state.StepType;
import io.harness.state.core.dummy.DummySectionStep;
import io.harness.state.core.dummy.DummySectionStepParameters;
import io.harness.state.core.dummy.DummySectionStepTransput;
import io.harness.state.core.fork.ForkStep;
import io.harness.state.core.fork.ForkStepParameters;
import io.harness.state.core.section.SectionStep;
import io.harness.state.core.section.SectionStepParameters;
import io.harness.state.core.section.chain.SectionChainStep;
import io.harness.state.core.section.chain.SectionChainStepParameters;
import io.harness.state.io.StepParameters;
import lombok.experimental.UtilityClass;
import software.wings.sm.states.ShellScriptState;

@OwnedBy(CDC)
@Redesign
@UtilityClass
public class CustomExecutionUtils {
  private static final String BASIC_HTTP_STATE_URL_404 = "http://httpstat.us/404";
  private static final String BASIC_HTTP_STATE_URL_200 = "http://httpstat.us/200";
  private static final String BASIC_HTTP_STATE_URL_500 = "http://httpstat.us/500";
  private static final StepType DUMMY_STEP_TYPE = StepType.builder().type("DUMMY").build();
  private static final StepType BASIC_HTTP_STEP_TYPE = StepType.builder().type("BASIC_HTTP").build();

  public static Plan provideHttpSwitchPlan() {
    String planId = generateUuid();
    String httpNodeId = generateUuid();
    String dummyNode1Id = generateUuid();
    String dummyNode2Id = generateUuid();
    String dummyNode3Id = generateUuid();
    String waitNodeId = generateUuid();

    BasicHttpStepParameters basicHttpStateParameters =
        BasicHttpStepParameters.builder().url(BASIC_HTTP_STATE_URL_200).method("GET").build();
    return Plan.builder()
        .node(PlanNode.builder()
                  .uuid(httpNodeId)
                  .name("Basic Http")
                  .stepType(BASIC_HTTP_STEP_TYPE)
                  .stepParameters(basicHttpStateParameters)
                  .identifier("http")
                  .adviserObtainment(AdviserObtainment.builder()
                                         .type(AdviserType.builder().type("HTTP_RESPONSE_CODE_SWITCH").build())
                                         .parameters(HttpResponseCodeSwitchAdviserParameters.builder()
                                                         .responseCodeNodeIdMapping(200, dummyNode1Id)
                                                         .responseCodeNodeIdMapping(404, dummyNode2Id)
                                                         .responseCodeNodeIdMapping(500, dummyNode3Id)
                                                         .build())
                                         .build())
                  .facilitatorObtainment(FacilitatorObtainment.builder()
                                             .type(FacilitatorType.builder().type(FacilitatorType.TASK).build())
                                             .build())
                  .build())
        .node(
            PlanNode.builder()
                .uuid(dummyNode1Id)
                .name("Dummy Node 1")
                .stepType(DUMMY_STEP_TYPE)
                .identifier("dummy1")
                .adviserObtainment(AdviserObtainment.builder()
                                       .type(AdviserType.builder().type(AdviserType.ON_SUCCESS).build())
                                       .parameters(OnSuccessAdviserParameters.builder().nextNodeId(waitNodeId).build())
                                       .build())
                .facilitatorObtainment(FacilitatorObtainment.builder()
                                           .type(FacilitatorType.builder().type(FacilitatorType.SYNC).build())
                                           .build())
                .refObject(OutcomeRefObject.builder().name("http").producerId(httpNodeId).build())
                .build())
        .node(
            PlanNode.builder()
                .uuid(dummyNode2Id)
                .name("Dummy Node 2")
                .stepType(DUMMY_STEP_TYPE)
                .identifier("dummy2")
                .adviserObtainment(AdviserObtainment.builder()
                                       .type(AdviserType.builder().type(AdviserType.ON_SUCCESS).build())
                                       .parameters(OnSuccessAdviserParameters.builder().nextNodeId(waitNodeId).build())
                                       .build())
                .facilitatorObtainment(FacilitatorObtainment.builder()
                                           .type(FacilitatorType.builder().type(FacilitatorType.SYNC).build())
                                           .build())
                .build())
        .node(
            PlanNode.builder()
                .uuid(dummyNode3Id)
                .name("Dummy Node 3")
                .stepType(DUMMY_STEP_TYPE)
                .identifier("dummy3")
                .adviserObtainment(AdviserObtainment.builder()
                                       .type(AdviserType.builder().type(AdviserType.ON_SUCCESS).build())
                                       .parameters(OnSuccessAdviserParameters.builder().nextNodeId(waitNodeId).build())
                                       .build())
                .facilitatorObtainment(FacilitatorObtainment.builder()
                                           .type(FacilitatorType.builder().type(FacilitatorType.SYNC).build())
                                           .build())
                .build())
        .node(PlanNode.builder()
                  .uuid(waitNodeId)
                  .name("Wait Node")
                  .identifier("wait")
                  .stepType(StepType.builder().type("WAIT_STATE").build())
                  .stepParameters(WaitStepParameters.builder().waitDurationSeconds(5).build())
                  .facilitatorObtainment(FacilitatorObtainment.builder()
                                             .type(FacilitatorType.builder().type(FacilitatorType.ASYNC).build())
                                             .build())
                  .build())
        .startingNodeId(httpNodeId)
        .uuid(planId)
        .build();
  }

  public static Plan provideHttpForkPlan() {
    String planId = generateUuid();
    String httpNodeId1 = generateUuid();
    String httpNodeId2 = generateUuid();
    String forkNodeId = generateUuid();
    String dummyNodeId = generateUuid();
    String emailNodeId = generateUuid();

    BasicHttpStepParameters basicHttpStateParameters1 =
        BasicHttpStepParameters.builder().url(BASIC_HTTP_STATE_URL_200).method("GET").build();

    BasicHttpStepParameters basicHttpStateParameters2 =
        BasicHttpStepParameters.builder().url(BASIC_HTTP_STATE_URL_404).method("GET").build();
    return Plan.builder()
        .node(PlanNode.builder()
                  .uuid(httpNodeId1)
                  .name("Basic Http 1")
                  .stepType(BASIC_HTTP_STEP_TYPE)
                  .identifier("http_parallel_1")
                  .stepParameters(basicHttpStateParameters1)
                  .facilitatorObtainment(FacilitatorObtainment.builder()
                                             .type(FacilitatorType.builder().type(FacilitatorType.TASK).build())
                                             .build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(httpNodeId2)
                  .name("Basic Http 2")
                  .stepType(BASIC_HTTP_STEP_TYPE)
                  .identifier("http_parallel_2")
                  .stepParameters(basicHttpStateParameters2)
                  .facilitatorObtainment(FacilitatorObtainment.builder()
                                             .type(FacilitatorType.builder().type(FacilitatorType.TASK).build())
                                             .build())
                  .build())
        .node(
            PlanNode.builder()
                .uuid(forkNodeId)
                .name("FORK")
                .stepType(StepType.builder().type("FORK").build())
                .identifier("fork")
                .stepParameters(
                    ForkStepParameters.builder().parallelNodeId(httpNodeId1).parallelNodeId(httpNodeId2).build())
                .adviserObtainment(AdviserObtainment.builder()
                                       .type(AdviserType.builder().type(AdviserType.ON_SUCCESS).build())
                                       .parameters(OnSuccessAdviserParameters.builder().nextNodeId(dummyNodeId).build())
                                       .build())
                .facilitatorObtainment(FacilitatorObtainment.builder()
                                           .type(FacilitatorType.builder().type(FacilitatorType.CHILDREN).build())
                                           .build())
                .build())
        .node(
            PlanNode.builder()
                .uuid(dummyNodeId)
                .name("Dummy Node 1")
                .identifier("dummy1")
                .stepType(DUMMY_STEP_TYPE)
                .adviserObtainment(AdviserObtainment.builder()
                                       .type(AdviserType.builder().type(AdviserType.ON_SUCCESS).build())
                                       .parameters(OnSuccessAdviserParameters.builder().nextNodeId(emailNodeId).build())
                                       .build())
                .facilitatorObtainment(FacilitatorObtainment.builder()
                                           .type(FacilitatorType.builder().type(FacilitatorType.SYNC).build())
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
                  .facilitatorObtainment(FacilitatorObtainment.builder()
                                             .type(FacilitatorType.builder().type(FacilitatorType.SYNC).build())
                                             .build())
                  .build())
        .startingNodeId(forkNodeId)
        .uuid(planId)
        .build();
  }

  public static Plan provideHttpSectionPlan() {
    String planId = generateUuid();
    String sectionNodeId = generateUuid();
    String httpNodeId1 = generateUuid();
    String httpNodeId2 = generateUuid();
    String waitNodeId1 = generateUuid();
    String dummyNodeId = generateUuid();

    BasicHttpStepParameters basicHttpStateParameters1 =
        BasicHttpStepParameters.builder().url(BASIC_HTTP_STATE_URL_200).method("GET").build();

    BasicHttpStepParameters basicHttpStateParameters2 =
        BasicHttpStepParameters.builder().url(BASIC_HTTP_STATE_URL_404).method("GET").build();
    return Plan.builder()
        .node(
            PlanNode.builder()
                .uuid(httpNodeId1)
                .name("Basic Http 1")
                .stepType(BASIC_HTTP_STEP_TYPE)
                .identifier("http_1")
                .stepParameters(basicHttpStateParameters1)
                .facilitatorObtainment(FacilitatorObtainment.builder()
                                           .type(FacilitatorType.builder().type(FacilitatorType.TASK).build())
                                           .build())
                .adviserObtainment(AdviserObtainment.builder()
                                       .type(AdviserType.builder().type(AdviserType.ON_SUCCESS).build())
                                       .parameters(OnSuccessAdviserParameters.builder().nextNodeId(waitNodeId1).build())
                                       .build())
                .build())
        .node(
            PlanNode.builder()
                .uuid(waitNodeId1)
                .name("Wait Step")
                .stepType(WaitStep.STEP_TYPE)
                .identifier("wait_1")
                .stepParameters(WaitStepParameters.builder().waitDurationSeconds(5).build())
                .facilitatorObtainment(FacilitatorObtainment.builder()
                                           .type(FacilitatorType.builder().type(FacilitatorType.ASYNC).build())
                                           .build())
                .adviserObtainment(AdviserObtainment.builder()
                                       .type(AdviserType.builder().type(AdviserType.ON_SUCCESS).build())
                                       .parameters(OnSuccessAdviserParameters.builder().nextNodeId(httpNodeId2).build())
                                       .build())
                .build())
        .node(PlanNode.builder()
                  .uuid(httpNodeId2)
                  .name("Basic Http 2")
                  .stepType(BASIC_HTTP_STEP_TYPE)
                  .identifier("http_2")
                  .stepParameters(basicHttpStateParameters2)
                  .facilitatorObtainment(FacilitatorObtainment.builder()
                                             .type(FacilitatorType.builder().type(FacilitatorType.TASK).build())
                                             .build())
                  .build())
        .node(
            PlanNode.builder()
                .uuid(sectionNodeId)
                .name("Section")
                .stepType(StepType.builder().type("SECTION").build())
                .identifier("section_1")
                .stepParameters(SectionStepParameters.builder().childNodeId(httpNodeId1).build())
                .adviserObtainment(AdviserObtainment.builder()
                                       .type(AdviserType.builder().type(AdviserType.ON_SUCCESS).build())
                                       .parameters(OnSuccessAdviserParameters.builder().nextNodeId(dummyNodeId).build())
                                       .build())
                .facilitatorObtainment(FacilitatorObtainment.builder()
                                           .type(FacilitatorType.builder().type(FacilitatorType.CHILD).build())
                                           .build())
                .build())
        .node(PlanNode.builder()
                  .uuid(dummyNodeId)
                  .name("Dummy Node 1")
                  .identifier("dummy")
                  .stepType(DUMMY_STEP_TYPE)
                  .facilitatorObtainment(FacilitatorObtainment.builder()
                                             .type(FacilitatorType.builder().type(FacilitatorType.SYNC).build())
                                             .build())
                  .build())
        .startingNodeId(sectionNodeId)
        .uuid(planId)
        .build();
  }

  public static Plan provideHttpRetryIgnorePlan() {
    String httpNodeId = generateUuid();
    String dummyNodeId = generateUuid();
    BasicHttpStepParameters basicHttpStateParameters =
        BasicHttpStepParameters.builder().url(BASIC_HTTP_STATE_URL_500).method("GET").build();
    return Plan.builder()
        .uuid(generateUuid())
        .startingNodeId(httpNodeId)
        .node(PlanNode.builder()
                  .uuid(httpNodeId)
                  .name("Basic Http 1")
                  .stepType(BasicHttpStep.STEP_TYPE)
                  .identifier("dummy")
                  .stepParameters(basicHttpStateParameters)
                  .facilitatorObtainment(FacilitatorObtainment.builder()
                                             .type(FacilitatorType.builder().type(FacilitatorType.TASK).build())
                                             .build())
                  .adviserObtainment(AdviserObtainment.builder()
                                         .type(AdviserType.builder().type(AdviserType.RETRY).build())
                                         .parameters(RetryAdviserParameters.builder()
                                                         .retryCount(2)
                                                         .waitIntervalList(ImmutableList.of(2, 5))
                                                         .repairActionCodeAfterRetry(RepairActionCode.IGNORE)
                                                         .nextNodeId(dummyNodeId)
                                                         .build())
                                         .build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(dummyNodeId)
                  .name("Dummy Node 1")
                  .identifier("dummy")
                  .stepType(DUMMY_STEP_TYPE)
                  .facilitatorObtainment(FacilitatorObtainment.builder()
                                             .type(FacilitatorType.builder().type(FacilitatorType.SYNC).build())
                                             .build())
                  .build())
        .build();
  }

  public static Plan provideHttpRetryAbortPlan() {
    String httpNodeId = generateUuid();
    String dummyNodeId = generateUuid();
    BasicHttpStepParameters basicHttpStateParameters =
        BasicHttpStepParameters.builder().url(BASIC_HTTP_STATE_URL_500).method("GET").build();
    return Plan.builder()
        .uuid(generateUuid())
        .startingNodeId(httpNodeId)
        .node(PlanNode.builder()
                  .uuid(httpNodeId)
                  .name("Basic Http 1")
                  .stepType(BasicHttpStep.STEP_TYPE)
                  .identifier("dummy")
                  .stepParameters(basicHttpStateParameters)
                  .facilitatorObtainment(FacilitatorObtainment.builder()
                                             .type(FacilitatorType.builder().type(FacilitatorType.TASK).build())
                                             .build())
                  .adviserObtainment(AdviserObtainment.builder()
                                         .type(AdviserType.builder().type(AdviserType.RETRY).build())
                                         .parameters(RetryAdviserParameters.builder()
                                                         .retryCount(2)
                                                         .waitIntervalList(ImmutableList.of(2, 5))
                                                         .repairActionCodeAfterRetry(RepairActionCode.END_EXECUTION)
                                                         .nextNodeId(dummyNodeId)
                                                         .build())
                                         .build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(dummyNodeId)
                  .name("Dummy Node 1")
                  .identifier("dummy")
                  .stepType(DUMMY_STEP_TYPE)
                  .facilitatorObtainment(FacilitatorObtainment.builder()
                                             .type(FacilitatorType.builder().type(FacilitatorType.SYNC).build())
                                             .build())
                  .build())
        .build();
  }

  public static Plan provideHttpRollbackPlan() {
    String planId = generateUuid();
    String sectionNodeId = generateUuid();
    String rollbackSectionNodeId = generateUuid();
    String rollbackHttpNodeId1 = generateUuid();
    String httpNodeId1 = generateUuid();
    String httpNodeId2 = generateUuid();
    String dummyNodeId = generateUuid();

    BasicHttpStepParameters basicHttpStateParameters1 =
        BasicHttpStepParameters.builder().url(BASIC_HTTP_STATE_URL_200).method("GET").build();

    BasicHttpStepParameters basicHttpStateParameters2 =
        BasicHttpStepParameters.builder().url(BASIC_HTTP_STATE_URL_404).method("GET").build();
    return Plan.builder()
        .node(
            PlanNode.builder()
                .uuid(httpNodeId1)
                .name("Basic Http 1")
                .stepType(BASIC_HTTP_STEP_TYPE)
                .identifier("http-1")
                .stepParameters(basicHttpStateParameters1)
                .facilitatorObtainment(FacilitatorObtainment.builder()
                                           .type(FacilitatorType.builder().type(FacilitatorType.TASK).build())
                                           .build())
                .adviserObtainment(AdviserObtainment.builder()
                                       .type(AdviserType.builder().type(AdviserType.ON_SUCCESS).build())
                                       .parameters(OnSuccessAdviserParameters.builder().nextNodeId(httpNodeId2).build())
                                       .build())
                .build())
        .node(PlanNode.builder()
                  .uuid(httpNodeId2)
                  .name("Basic Http 2")
                  .stepType(BASIC_HTTP_STEP_TYPE)
                  .identifier("http-2")
                  .stepParameters(basicHttpStateParameters2)
                  .facilitatorObtainment(FacilitatorObtainment.builder()
                                             .type(FacilitatorType.builder().type(FacilitatorType.TASK).build())
                                             .build())
                  .build())
        .node(
            PlanNode.builder()
                .uuid(sectionNodeId)
                .name("Section")
                .stepType(StepType.builder().type("SECTION").build())
                .identifier("section-1")
                .stepParameters(SectionStepParameters.builder().childNodeId(httpNodeId1).build())
                .adviserObtainment(
                    AdviserObtainment.builder()
                        .type(AdviserType.builder().type(AdviserType.ON_FAIL).build())
                        .parameters(OnFailAdviserParameters.builder().nextNodeId(rollbackSectionNodeId).build())
                        .build())
                .adviserObtainment(AdviserObtainment.builder()
                                       .type(AdviserType.builder().type(AdviserType.ON_SUCCESS).build())
                                       .parameters(OnSuccessAdviserParameters.builder().nextNodeId(dummyNodeId).build())
                                       .build())
                .facilitatorObtainment(FacilitatorObtainment.builder()
                                           .type(FacilitatorType.builder().type(FacilitatorType.CHILD).build())
                                           .build())
                .build())
        .node(PlanNode.builder()
                  .uuid(rollbackSectionNodeId)
                  .name("Section")
                  .stepType(StepType.builder().type("SECTION").build())
                  .identifier("section-1")
                  .stepParameters(SectionStepParameters.builder().childNodeId(rollbackHttpNodeId1).build())
                  .facilitatorObtainment(FacilitatorObtainment.builder()
                                             .type(FacilitatorType.builder().type(FacilitatorType.CHILD).build())
                                             .build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(rollbackHttpNodeId1)
                  .name("Rollback Http 1")
                  .stepType(BASIC_HTTP_STEP_TYPE)
                  .identifier("rollback-http-1")
                  .stepParameters(basicHttpStateParameters1)
                  .facilitatorObtainment(FacilitatorObtainment.builder()
                                             .type(FacilitatorType.builder().type(FacilitatorType.TASK).build())
                                             .build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(dummyNodeId)
                  .name("Dummy Node 1")
                  .identifier("dummy")
                  .stepType(DUMMY_STEP_TYPE)
                  .facilitatorObtainment(FacilitatorObtainment.builder()
                                             .type(FacilitatorType.builder().type(FacilitatorType.SYNC).build())
                                             .build())
                  .build())
        .startingNodeId(sectionNodeId)
        .uuid(planId)
        .build();
  }

  /**
   * Execution plan hierarchy:
   *
   * - section1
   *   - sectionChild
   *     - shell0 (stores to outcome and sweeping output with name shell1 in section scope)
   *     - shell  (stores to outcome and sweeping output with name shell2 in global scope)
   * - section2
   *   - sectionChild
   *     - shell
   *     - dummy
   */
  public static Plan provideSimpleShellScriptPlan() {
    String section1NodeId = generateUuid();
    String section11NodeId = generateUuid();
    String section2NodeId = generateUuid();
    String section21NodeId = generateUuid();
    String shellScript11NodeId = generateUuid();
    String shellScript12NodeId = generateUuid();
    String shellScript2NodeId = generateUuid();
    String dummyNodeId = generateUuid();
    ShellScriptStepParameters shellScript11StepParameters =
        ShellScriptStepParameters.builder()
            .executeOnDelegate(true)
            .connectionType(ShellScriptState.ConnectionType.SSH)
            .scriptType(ScriptType.BASH)
            .scriptString("echo 'Hello, world, from script 11!'\n"
                + "export HELLO='hello1!'\n"
                + "export HI='hi1!'\n"
                + "echo \"scriptType = ${scriptType}\"\n"
                + "echo \"sweepingOutputScope = ${sweepingOutputScope}\"\n")
            .outputVars("HELLO,HI")
            .sweepingOutputName("shell1")
            .sweepingOutputScope("SECTION")
            .build();
    ShellScriptStepParameters shellScript12StepParameters =
        ShellScriptStepParameters.builder()
            .executeOnDelegate(true)
            .connectionType(ShellScriptState.ConnectionType.SSH)
            .scriptType(ScriptType.BASH)
            .scriptString("echo 'Hello, world, from script 12!'\n"
                + "export HELLO='hello2!'\n"
                + "export HI='hi2!'\n"
                + "echo \"shell1.HELLO = ${sectionChild.shell1.variables.HELLO}\"\n" // ancestor output
                + "echo \"shell1.HI = ${sectionChild.shell1.variables.HI}\"\n" // ancestor output
                + "echo \"shell1.HELLO = ${sectionChild.shellOutcome.sweepingOutputEnvVariables.HELLO}\"\n" // ancestor
                                                                                                            // output
                + "echo \"shell1.HI = ${sectionChild.shellOutcome.sweepingOutputEnvVariables.HI}\"\n" // ancestor
                                                                                                      // outcome
                + "echo \"shell1.HELLO = ${section1.sectionChild.shell1.variables.HELLO}\"\n" // qualified output
                + "echo \"shell1.HI = ${section1.sectionChild.shell1.variables.HI}\"\n" // qualified output
                + "echo \"shell1.HELLO = ${section1.sectionChild.shellOutcome.sweepingOutputEnvVariables.HELLO}\"\n" // qualified output
                + "echo \"shell1.HI = ${section1.sectionChild.shellOutcome.sweepingOutputEnvVariables.HI}\"\n" // qualified
                // outcome
                + "echo \"scriptType = ${scriptType}\"\n" // child
                + "echo \"section1.f1 = ${section1.data.map.f1}\"\n" // qualified
                + "echo \"section1.f2 = ${section1.data.map.f2}\"\n" // qualified
                + "echo \"sectionChild.f1 = ${sectionChild.data.map.f1}\"\n" // ancestor
                + "echo \"sectionChild.f1 = ${section1.sectionChild.data.map.f1}\"\n" // qualified
                + "echo \"sectionChild.f2 = ${sectionChild.data.map.f2}\"\n" // ancestor
                + "echo \"sectionChild.f2 = ${section1.sectionChild.data.map.f2}\"\n" // qualified
                + "echo \"shell1.HELLO = ${ancestor.sectionChild.shell1.variables.HELLO}\"\n"
                + "echo \"shell1.HI = ${ancestor.sectionChild.shell1.variables.HI}\"\n"
                + "echo \"shell1.HELLO = ${ancestor.sectionChild.shellOutcome.sweepingOutputEnvVariables.HELLO}\"\n"
                + "echo \"shell1.HI = ${ancestor.sectionChild.shellOutcome.sweepingOutputEnvVariables.HI}\"\n"
                + "echo \"shell1.HELLO = ${qualified.section1.sectionChild.shell1.variables.HELLO}\"\n"
                + "echo \"shell1.HI = ${qualified.section1.sectionChild.shell1.variables.HI}\"\n"
                + "echo \"shell1.HELLO = ${qualified.section1.sectionChild.shellOutcome.sweepingOutputEnvVariables.HELLO}\"\n"
                + "echo \"shell1.HI = ${qualified.section1.sectionChild.shellOutcome.sweepingOutputEnvVariables.HI}\"\n"
                + "echo \"scriptType = ${child.scriptType}\"\n"
                + "echo \"section1.f1 = ${qualified.section1.data.map.f1}\"\n"
                + "echo \"section1.f2 = ${qualified.section1.data.map.f2}\"\n"
                + "echo \"sectionChild.f1 = ${ancestor.sectionChild.data.map.f1}\"\n"
                + "echo \"sectionChild.f1 = ${qualified.section1.sectionChild.data.map.f1}\"\n"
                + "echo \"sectionChild.f2 = ${ancestor.sectionChild.data.map.f2}\"\n"
                + "echo \"sectionChild.f2 = ${qualified.section1.sectionChild.data.map.f2}\"\n")
            .outputVars("HELLO,HI")
            .sweepingOutputName("shell2")
            .build();
    ShellScriptStepParameters shellScript2StepParameters =
        ShellScriptStepParameters.builder()
            .executeOnDelegate(true)
            .connectionType(ShellScriptState.ConnectionType.SSH)
            .scriptType(ScriptType.BASH)
            .scriptString("echo 'Hello, world, from script 2!'\n"
                + "echo \"shell1.HELLO = ${section1.sectionChild.shell1.variables.HELLO}\"\n" // qualified output
                + "echo \"shell1.HI = ${section1.sectionChild.shell1.variables.HI}\"\n" // qualified output
                + "echo \"shell1.HELLO = ${section1.sectionChild.shellOutcome.sweepingOutputEnvVariables.HELLO}\"\n" // qualified output
                + "echo \"shell1.HI = ${section1.sectionChild.shellOutcome.sweepingOutputEnvVariables.HI}\"\n" // qualified
                                                                                                               // outcome
                + "echo \"shell2.HELLO = ${shell2.variables.HELLO}\"\n" // output
                + "echo \"shell2.HI = ${shell2.variables.HI}\"\n" // output
                + "echo \"section1.f1 = ${section1.data.map.f1}\"\n" // qualified
                + "echo \"section1.f1 = ${section1.outcomeData.map.f1}\"\n" // qualified
                + "echo \"section11.f1 = ${section1.sectionChild.data.map.f1}\"\n" // qualified
                + "echo \"section11.f1 = ${section1.sectionChild.outcomeData.map.f1}\"\n" // qualified
                + "echo \"shell2.scriptType = ${section1.sectionChild.shell.scriptType}\"\n" // qualified
                + "echo \"shell2.HELLO = ${section1.sectionChild.shell.data.sweepingOutputEnvVariables.HELLO}\"\n" // qualified
                + "echo \"shell2.HI = ${section1.sectionChild.shell.data.sweepingOutputEnvVariables.HI}\"\n" // qualified
                + "echo \"scriptType = ${scriptType}\"\n" // child
                + "echo \"section2.f1 = ${section2.data.map.f1}\"\n" // qualified
                + "echo \"section2.f2 = ${section2.data.map.f2}\"\n" // qualified
                + "echo \"sectionChild.f1 = ${sectionChild.data.map.f1}\"\n" // ancestor
                + "echo \"sectionChild.f1 = ${section2.sectionChild.data.map.f1}\"\n" // qualified
                + "echo \"sectionChild.f2 = ${sectionChild.data.map.f2}\"\n" // ancestor
                + "echo \"sectionChild.f2 = ${section2.sectionChild.data.map.f2}\"\n" // qualified
                + "echo \"shell1.HELLO = ${qualified.section1.sectionChild.shell1.variables.HELLO}\"\n"
                + "echo \"shell1.HI = ${qualified.section1.sectionChild.shell1.variables.HI}\"\n"
                + "echo \"shell1.HELLO = ${qualified.section1.sectionChild.shellOutcome.sweepingOutputEnvVariables.HELLO}\"\n"
                + "echo \"shell1.HI = ${qualified.section1.sectionChild.shellOutcome.sweepingOutputEnvVariables.HI}\"\n"
                + "echo \"shell2.HELLO = ${output.shell2.variables.HELLO}\"\n"
                + "echo \"shell2.HI = ${output.shell2.variables.HI}\"\n"
                + "echo \"section1.f1 = ${qualified.section1.data.map.f1}\"\n"
                + "echo \"section1.f1 = ${qualified.section1.outcomeData.map.f1}\"\n"
                + "echo \"section11.f1 = ${qualified.section1.sectionChild.data.map.f1}\"\n"
                + "echo \"section11.f1 = ${qualified.section1.sectionChild.outcomeData.map.f1}\"\n"
                + "echo \"shell2.scriptType = ${qualified.section1.sectionChild.shell.scriptType}\"\n"
                + "echo \"shell2.HELLO = ${qualified.section1.sectionChild.shell.data.sweepingOutputEnvVariables.HELLO}\"\n"
                + "echo \"shell2.HI = ${qualified.section1.sectionChild.shell.data.sweepingOutputEnvVariables.HI}\"\n"
                + "echo \"scriptType = ${child.scriptType}\"\n"
                + "echo \"section2.f1 = ${qualified.section2.data.map.f1}\"\n"
                + "echo \"section2.f2 = ${qualified.section2.data.map.f2}\"\n"
                + "echo \"sectionChild.f1 = ${ancestor.sectionChild.data.map.f1}\"\n"
                + "echo \"sectionChild.f1 = ${qualified.section2.sectionChild.data.map.f1}\"\n"
                + "echo \"sectionChild.f2 = ${ancestor.sectionChild.data.map.f2}\"\n"
                + "echo \"sectionChild.f2 = ${qualified.section2.sectionChild.data.map.f2}\"\n")
            .build();

    return Plan.builder()
        .uuid(generateUuid())
        .startingNodeId(section1NodeId)
        .node(
            PlanNode.builder()
                .uuid(section1NodeId)
                .name("Section 1")
                .identifier("section1")
                .stepType(DummySectionStep.STEP_TYPE)
                .group("SECTION")
                .stepParameters(
                    DummySectionStepParameters.builder()
                        .childNodeId(section11NodeId)
                        .data(DummySectionStepTransput.builder().map(ImmutableMap.of("f1", "v11", "f2", "v12")).build())
                        .build())
                .adviserObtainment(
                    AdviserObtainment.builder()
                        .type(AdviserType.builder().type(AdviserType.ON_SUCCESS).build())
                        .parameters(OnSuccessAdviserParameters.builder().nextNodeId(section2NodeId).build())
                        .build())
                .facilitatorObtainment(FacilitatorObtainment.builder()
                                           .type(FacilitatorType.builder().type(FacilitatorType.CHILD).build())
                                           .build())
                .build())
        .node(
            PlanNode.builder()
                .uuid(section11NodeId)
                .name("Section 11")
                .identifier("sectionChild")
                .stepType(DummySectionStep.STEP_TYPE)
                .group("SECTION")
                .stepParameters(
                    DummySectionStepParameters.builder()
                        .childNodeId(shellScript11NodeId)
                        .data(
                            DummySectionStepTransput.builder().map(ImmutableMap.of("f1", "v111", "f2", "v112")).build())
                        .build())
                .facilitatorObtainment(FacilitatorObtainment.builder()
                                           .type(FacilitatorType.builder().type(FacilitatorType.CHILD).build())
                                           .build())
                .build())
        .node(
            PlanNode.builder()
                .uuid(section2NodeId)
                .name("Section 2")
                .identifier("section2")
                .stepType(DummySectionStep.STEP_TYPE)
                .group("SECTION")
                .stepParameters(
                    DummySectionStepParameters.builder()
                        .childNodeId(section21NodeId)
                        .data(DummySectionStepTransput.builder().map(ImmutableMap.of("f1", "v21", "f2", "v22")).build())
                        .build())
                .facilitatorObtainment(FacilitatorObtainment.builder()
                                           .type(FacilitatorType.builder().type(FacilitatorType.CHILD).build())
                                           .build())
                .build())
        .node(
            PlanNode.builder()
                .uuid(section21NodeId)
                .name("Section 21")
                .identifier("sectionChild")
                .stepType(DummySectionStep.STEP_TYPE)
                .group("SECTION")
                .stepParameters(
                    DummySectionStepParameters.builder()
                        .childNodeId(shellScript2NodeId)
                        .data(
                            DummySectionStepTransput.builder().map(ImmutableMap.of("f1", "v211", "f2", "v212")).build())
                        .build())
                .facilitatorObtainment(FacilitatorObtainment.builder()
                                           .type(FacilitatorType.builder().type(FacilitatorType.CHILD).build())
                                           .build())
                .build())
        .node(PlanNode.builder()
                  .uuid(shellScript11NodeId)
                  .name("shell11")
                  .identifier("shell0")
                  .stepType(ShellScriptStep.STEP_TYPE)
                  .stepParameters(shellScript11StepParameters)
                  .facilitatorObtainment(FacilitatorObtainment.builder()
                                             .type(FacilitatorType.builder().type(FacilitatorType.TASK).build())
                                             .build())
                  .adviserObtainment(
                      AdviserObtainment.builder()
                          .type(AdviserType.builder().type(AdviserType.ON_SUCCESS).build())
                          .parameters(OnSuccessAdviserParameters.builder().nextNodeId(shellScript12NodeId).build())
                          .build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(shellScript12NodeId)
                  .name("shell12")
                  .identifier("shell")
                  .stepType(ShellScriptStep.STEP_TYPE)
                  .stepParameters(shellScript12StepParameters)
                  .facilitatorObtainment(FacilitatorObtainment.builder()
                                             .type(FacilitatorType.builder().type(FacilitatorType.TASK).build())
                                             .build())
                  .build())
        .node(
            PlanNode.builder()
                .uuid(shellScript2NodeId)
                .name("shell2")
                .identifier("shell")
                .stepType(ShellScriptStep.STEP_TYPE)
                .stepParameters(shellScript2StepParameters)
                .facilitatorObtainment(FacilitatorObtainment.builder()
                                           .type(FacilitatorType.builder().type(FacilitatorType.TASK).build())
                                           .build())
                .adviserObtainment(AdviserObtainment.builder()
                                       .type(AdviserType.builder().type(AdviserType.ON_SUCCESS).build())
                                       .parameters(OnSuccessAdviserParameters.builder().nextNodeId(dummyNodeId).build())
                                       .build())
                .build())
        .node(PlanNode.builder()
                  .uuid(dummyNodeId)
                  .name("Dummy Node 1")
                  .identifier("dummy")
                  .stepType(DUMMY_STEP_TYPE)
                  .facilitatorObtainment(FacilitatorObtainment.builder()
                                             .type(FacilitatorType.builder().type(FacilitatorType.SYNC).build())
                                             .build())
                  .build())
        .build();
  }

  public static Plan provideTaskChainPlan() {
    String sectionNodeId = generateUuid();
    String httpChainId = generateUuid();
    String dummyNodeId = generateUuid();

    BasicHttpStepParameters basicHttpStateParameters1 =
        BasicHttpStepParameters.builder().url(BASIC_HTTP_STATE_URL_200).method("GET").build();

    BasicHttpStepParameters basicHttpStateParameters2 =
        BasicHttpStepParameters.builder().url(BASIC_HTTP_STATE_URL_404).method("GET").build();
    return Plan.builder()
        .node(
            PlanNode.builder()
                .uuid(sectionNodeId)
                .name("Section")
                .stepType(SectionStep.STEP_TYPE)
                .identifier("section_1")
                .stepParameters(SectionStepParameters.builder().childNodeId(httpChainId).build())
                .adviserObtainment(AdviserObtainment.builder()
                                       .type(AdviserType.builder().type(AdviserType.ON_SUCCESS).build())
                                       .parameters(OnSuccessAdviserParameters.builder().nextNodeId(dummyNodeId).build())
                                       .build())
                .facilitatorObtainment(FacilitatorObtainment.builder()
                                           .type(FacilitatorType.builder().type(FacilitatorType.CHILD).build())
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
                  .facilitatorObtainment(FacilitatorObtainment.builder()
                                             .type(FacilitatorType.builder().type(FacilitatorType.TASK_CHAIN).build())
                                             .build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(dummyNodeId)
                  .name("Dummy Node 1")
                  .identifier("dummy")
                  .stepType(DUMMY_STEP_TYPE)
                  .facilitatorObtainment(FacilitatorObtainment.builder()
                                             .type(FacilitatorType.builder().type(FacilitatorType.SYNC).build())
                                             .build())
                  .build())
        .startingNodeId(sectionNodeId)
        .uuid(generateUuid())
        .build();
  }

  public static Plan provideSectionChainPlan() {
    String sectionChainNodeId = generateUuid();
    String httpNode1Id = generateUuid();
    String httpNode2Id = generateUuid();
    String dummyNode1Id = generateUuid();
    String dummyNode2Id = generateUuid();

    BasicHttpStepParameters basicHttpStateParameters1 =
        BasicHttpStepParameters.builder().url(BASIC_HTTP_STATE_URL_200).method("GET").build();

    BasicHttpStepParameters basicHttpStateParameters2 =
        BasicHttpStepParameters.builder().url(BASIC_HTTP_STATE_URL_404).method("GET").build();
    return Plan.builder()
        .node(PlanNode.builder()
                  .uuid(sectionChainNodeId)
                  .name("Section Chain")
                  .stepType(SectionChainStep.STEP_TYPE)
                  .identifier("section_chain")
                  .stepParameters(
                      SectionChainStepParameters.builder().childNodeId(httpNode1Id).childNodeId(httpNode2Id).build())
                  .facilitatorObtainment(FacilitatorObtainment.builder()
                                             .type(FacilitatorType.builder().type(FacilitatorType.CHILD_CHAIN).build())
                                             .build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(httpNode1Id)
                  .name("HTTP 1")
                  .stepType(BasicHttpStep.STEP_TYPE)
                  .identifier("http1")
                  .stepParameters(basicHttpStateParameters1)
                  .adviserObtainment(
                      AdviserObtainment.builder()
                          .type(OnSuccessAdviser.ADVISER_TYPE)
                          .parameters(OnSuccessAdviserParameters.builder().nextNodeId(dummyNode1Id).build())
                          .build())
                  .facilitatorObtainment(FacilitatorObtainment.builder()
                                             .type(FacilitatorType.builder().type(FacilitatorType.TASK).build())
                                             .build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(httpNode2Id)
                  .name("HTTP 2")
                  .stepType(BasicHttpStep.STEP_TYPE)
                  .identifier("http2")
                  .stepParameters(basicHttpStateParameters2)
                  .adviserObtainment(
                      AdviserObtainment.builder()
                          .type(OnSuccessAdviser.ADVISER_TYPE)
                          .parameters(OnSuccessAdviserParameters.builder().nextNodeId(dummyNode2Id).build())
                          .build())
                  .facilitatorObtainment(FacilitatorObtainment.builder()
                                             .type(FacilitatorType.builder().type(FacilitatorType.TASK).build())
                                             .build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(dummyNode1Id)
                  .name("Dummy Node 1")
                  .stepType(DUMMY_STEP_TYPE)
                  .identifier("dummy1")
                  .facilitatorObtainment(FacilitatorObtainment.builder()
                                             .type(FacilitatorType.builder().type(FacilitatorType.SYNC).build())
                                             .build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(dummyNode2Id)
                  .name("Dummy Node 2")
                  .stepType(DUMMY_STEP_TYPE)
                  .identifier("dummy2")
                  .facilitatorObtainment(FacilitatorObtainment.builder()
                                             .type(FacilitatorType.builder().type(FacilitatorType.SYNC).build())
                                             .build())
                  .build())
        .startingNodeId(sectionChainNodeId)
        .uuid(generateUuid())
        .build();
  }

  /*
  pipelineSetup
  infra section
    infra state
    shell script
   */

  public static Plan provideInfraStateTestPlan() {
    String pipelineSetupNodeId = generateUuid();
    String infraSectionNodeId = generateUuid();
    String infraStateNodeId = generateUuid();
    String scriptNodeId = generateUuid();

    Service service = Service.builder().identifier("my service").build();

    K8sDirectInfraDefinition k8sDirectInfraDefinition =
        K8sDirectInfraDefinition.builder()
            .name("k8s direct")
            .spec(K8sDirectInfraDefinition.Spec.builder().namespace("namespace").build())
            .build();

    PipelineInfrastructure pipelineInfrastructure =
        PipelineInfrastructure.builder().infraDefinition(k8sDirectInfraDefinition).build();

    CDPipeline cdPipeline =
        CDPipeline.builder()
            .stage(DeploymentStage.builder().service(service).infrastructure(pipelineInfrastructure).build())
            .build();

    ShellScriptStepParameters shellScriptStepParameters =
        ShellScriptStepParameters.builder()
            .executeOnDelegate(true)
            .connectionType(ShellScriptState.ConnectionType.SSH)
            .scriptType(ScriptType.BASH)
            .scriptString("echo 'hey' ${infrastructureMapping.namespace} ")
            .build();

    String PIPELINE_SETUP = "PIPELINE SETUP";
    String INFRASTRUCTURE_SECTION = "INFRASTRUCTURE SECTION";
    String INFRASTRUCTURE = "INFRASTRUCTURE";
    String SHELL_SCRIPT = "SHELL SCRIPT";

    return Plan.builder()
        .uuid(generateUuid())
        .startingNodeId(pipelineSetupNodeId)
        .node(PlanNode.builder()
                  .uuid(pipelineSetupNodeId)
                  .name(PIPELINE_SETUP)
                  .identifier(PIPELINE_SETUP)
                  .stepType(PipelineSetupStep.STEP_TYPE)
                  .stepParameters(CDPipelineSetupParameters.builder().cdPipeline(cdPipeline).build())
                  .adviserObtainment(
                      AdviserObtainment.builder()
                          .type(AdviserType.builder().type(AdviserType.ON_SUCCESS).build())
                          .parameters(OnSuccessAdviserParameters.builder().nextNodeId(infraSectionNodeId).build())
                          .build())
                  .facilitatorObtainment(FacilitatorObtainment.builder()
                                             .type(FacilitatorType.builder().type(FacilitatorType.SYNC).build())
                                             .build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(infraSectionNodeId)
                  .name(INFRASTRUCTURE_SECTION)
                  .identifier(INFRASTRUCTURE_SECTION)
                  .stepParameters(SectionStepParameters.builder().childNodeId(infraStateNodeId).build())
                  .stepType(InfrastructureSectionStep.STEP_TYPE)
                  .facilitatorObtainment(FacilitatorObtainment.builder()
                                             .type(FacilitatorType.builder().type(FacilitatorType.CHILD).build())
                                             .build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(infraStateNodeId)
                  .name(INFRASTRUCTURE)
                  .identifier(INFRASTRUCTURE)
                  .stepType(InfrastructureStep.STEP_TYPE)
                  .facilitatorObtainment(FacilitatorObtainment.builder()
                                             .type(FacilitatorType.builder().type(FacilitatorType.SYNC).build())
                                             .build())
                  .refObject(OutcomeRefObject.builder().name("service").producerId(pipelineSetupNodeId).build())
                  .refObject(OutcomeRefObject.builder().name("infraDefinition").producerId(pipelineSetupNodeId).build())
                  .adviserObtainment(
                      AdviserObtainment.builder()
                          .type(AdviserType.builder().type(AdviserType.ON_SUCCESS).build())
                          .parameters(OnSuccessAdviserParameters.builder().nextNodeId(scriptNodeId).build())
                          .build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(scriptNodeId)
                  .name(SHELL_SCRIPT)
                  .identifier(SHELL_SCRIPT)
                  .stepType(ShellScriptStep.STEP_TYPE)
                  .stepParameters(shellScriptStepParameters)
                  .facilitatorObtainment(FacilitatorObtainment.builder()
                                             .type(FacilitatorType.builder().type(FacilitatorType.TASK).build())
                                             .build())
                  .build())
        .build();
  }

  public Plan provideGraphTestPlan() {
    String planId = generateUuid();
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
        BasicHttpStepParameters.builder().url(BASIC_HTTP_STATE_URL_200).method("GET").build();
    return Plan.builder()
        .uuid(planId)
        .startingNodeId(dummyStartNode)
        .node(PlanNode.builder()
                  .uuid(dummyStartNode)
                  .identifier("dummy-start")
                  .name("dummy-start")
                  .stepType(DUMMY_STEP_TYPE)
                  .adviserObtainment(AdviserObtainment.builder()
                                         .type(AdviserType.builder().type(AdviserType.ON_SUCCESS).build())
                                         .parameters(OnSuccessAdviserParameters.builder().nextNodeId(forkId).build())
                                         .build())
                  .facilitatorObtainment(FacilitatorObtainment.builder()
                                             .type(FacilitatorType.builder().type(FacilitatorType.SYNC).build())
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
                .adviserObtainment(AdviserObtainment.builder()
                                       .type(AdviserType.builder().type(AdviserType.ON_SUCCESS).build())
                                       .parameters(OnSuccessAdviserParameters.builder().nextNodeId(dummuNodeId).build())
                                       .build())
                .facilitatorObtainment(FacilitatorObtainment.builder()
                                           .type(FacilitatorType.builder().type(FacilitatorType.CHILDREN).build())
                                           .build())
                .build())
        .node(PlanNode.builder()
                  .uuid(section1Id)
                  .identifier("section1")
                  .name("section1")
                  .stepType(SectionStep.STEP_TYPE)
                  .stepParameters(SectionStepParameters.builder().childNodeId(forkId2).build())
                  .facilitatorObtainment(FacilitatorObtainment.builder()
                                             .type(FacilitatorType.builder().type(FacilitatorType.CHILD).build())
                                             .build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(section2Id)
                  .identifier("section2")
                  .name("section2")
                  .stepType(SectionStep.STEP_TYPE)
                  .stepParameters(SectionStepParameters.builder().childNodeId(httpSwitchId).build())
                  .facilitatorObtainment(FacilitatorObtainment.builder()
                                             .type(FacilitatorType.builder().type(FacilitatorType.CHILD).build())
                                             .build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(forkId2)
                  .identifier("fork2")
                  .name("fork2")
                  .stepType(ForkStep.STEP_TYPE)
                  .stepParameters(ForkStepParameters.builder().parallelNodeId(http1Id).parallelNodeId(http2Id).build())
                  .facilitatorObtainment(FacilitatorObtainment.builder()
                                             .type(FacilitatorType.builder().type(FacilitatorType.CHILDREN).build())
                                             .build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(httpSwitchId)
                  .identifier("http-switch")
                  .name("http-switch")
                  .stepType(BASIC_HTTP_STEP_TYPE)
                  .stepParameters(basicHttpStepParameters1)
                  .adviserObtainment(AdviserObtainment.builder()
                                         .type(HttpResponseCodeSwitchAdviser.ADVISER_TYPE)
                                         .parameters(HttpResponseCodeSwitchAdviserParameters.builder()
                                                         .responseCodeNodeIdMapping(200, dummyNode1Id)
                                                         .responseCodeNodeIdMapping(404, dummyNode2Id)
                                                         .build())
                                         .build())
                  .facilitatorObtainment(FacilitatorObtainment.builder()
                                             .type(FacilitatorType.builder().type(FacilitatorType.TASK).build())
                                             .build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(http1Id)
                  .identifier("http1")
                  .name("http1")
                  .stepType(BASIC_HTTP_STEP_TYPE)
                  .stepParameters(basicHttpStepParameters1)
                  .facilitatorObtainment(FacilitatorObtainment.builder()
                                             .type(FacilitatorType.builder().type(FacilitatorType.TASK).build())
                                             .build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(http2Id)
                  .identifier("http2")
                  .name("http2")
                  .stepType(BASIC_HTTP_STEP_TYPE)
                  .stepParameters(basicHttpStepParameters1)
                  .facilitatorObtainment(FacilitatorObtainment.builder()
                                             .type(FacilitatorType.builder().type(FacilitatorType.TASK).build())
                                             .build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(dummyNode1Id)
                  .identifier("dummy1")
                  .name("dummy1")
                  .stepType(DUMMY_STEP_TYPE)
                  .facilitatorObtainment(FacilitatorObtainment.builder()
                                             .type(FacilitatorType.builder().type(FacilitatorType.SYNC).build())
                                             .build())
                  .refObject(OutcomeRefObject.builder().name("http").producerId(httpSwitchId).build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(dummyNode2Id)
                  .identifier("dummy2")
                  .name("dummy2")
                  .stepType(DUMMY_STEP_TYPE)
                  .facilitatorObtainment(FacilitatorObtainment.builder()
                                             .type(FacilitatorType.builder().type(FacilitatorType.SYNC).build())
                                             .build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(dummuNodeId)
                  .identifier("dummy-final")
                  .name("dummy-final")
                  .stepType(DUMMY_STEP_TYPE)
                  .facilitatorObtainment(FacilitatorObtainment.builder()
                                             .type(FacilitatorType.builder().type(FacilitatorType.SYNC).build())
                                             .build())
                  .build())
        .build();
  }

  public Plan provideArtifactStateTestPlan() {
    String planId = generateUuid();
    String sectionNodeId = generateUuid();
    String dummyNodeId = generateUuid();
    String artifactNodeId = generateUuid();
    ArtifactConfigWrapper artifactConfigWrapper = DockerHubArtifactConfig.builder()
                                                      .dockerhubConnector("https://registry.hub.docker.com/")
                                                      .imagePath("library/ubuntu")
                                                      .tag("latest")
                                                      .build();
    ArtifactStepParameters stepParameters = ArtifactStepParameters.builder().artifact(artifactConfigWrapper).build();
    return Plan.builder()
        .startingNodeId(sectionNodeId)
        .uuid(planId)
        .node(PlanNode.builder()
                  .uuid(artifactNodeId)
                  .name("ARTIFACT_STEP")
                  .identifier("ARTIFACT_STEP1")
                  .stepType(ArtifactStep.STEP_TYPE)
                  .stepParameters(stepParameters)
                  .facilitatorObtainment(FacilitatorObtainment.builder()
                                             .type(FacilitatorType.builder().type(FacilitatorType.TASK).build())
                                             .build())
                  .build())
        .node(
            PlanNode.builder()
                .uuid(sectionNodeId)
                .name("Section")
                .stepType(StepType.builder().type("SECTION").build())
                .identifier("section_1")
                .stepParameters(SectionStepParameters.builder().childNodeId(artifactNodeId).build())
                .adviserObtainment(AdviserObtainment.builder()
                                       .type(AdviserType.builder().type(AdviserType.ON_SUCCESS).build())
                                       .parameters(OnSuccessAdviserParameters.builder().nextNodeId(dummyNodeId).build())
                                       .build())
                .facilitatorObtainment(FacilitatorObtainment.builder()
                                           .type(FacilitatorType.builder().type(FacilitatorType.CHILD).build())
                                           .build())
                .build())
        .node(PlanNode.builder()
                  .uuid(dummyNodeId)
                  .name("Dummy Node 1")
                  .identifier("dummy")
                  .stepType(DUMMY_STEP_TYPE)
                  .facilitatorObtainment(FacilitatorObtainment.builder()
                                             .type(FacilitatorType.builder().type(FacilitatorType.SYNC).build())
                                             .build())
                  .build())
        .build();
  }
}
