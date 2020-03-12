package software.wings.service.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.ANKIT;
import static io.harness.rule.OwnerRule.UJJAWAL;
import static java.util.Arrays.asList;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.beans.BasicOrchestrationWorkflow.BasicOrchestrationWorkflowBuilder.aBasicOrchestrationWorkflow;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.DEPLOY_SERVICE;
import static software.wings.beans.PhaseStepType.POST_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.service.impl.PipelinePreDeploymentValidator.APPROVAL_ERROR_MSG;
import static software.wings.service.impl.WorkflowPreDeploymentValidator.getWorkflowRestrictedFeatureErrorMsg;
import static software.wings.sm.states.ApprovalState.APPROVAL_STATE_TYPE_VARIABLE;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.PIPELINE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_NAME;

import com.google.inject.Inject;

import io.harness.beans.WorkflowType;
import io.harness.category.element.UnitTests;
import io.harness.configuration.DeployMode;
import io.harness.exception.WingsException;
import io.harness.limits.LimitCheckerFactory;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.WingsBaseTest;
import software.wings.app.MainConfiguration;
import software.wings.beans.AccountType;
import software.wings.beans.GraphNode;
import software.wings.beans.LicenseInfo;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.deployment.PreDeploymentChecker;
import software.wings.service.intfc.deployment.RateLimitCheck;
import software.wings.sm.StateType;
import software.wings.sm.states.ApprovalState.ApprovalStateType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nonnull;

public class PreDeploymentCheckerTest extends WingsBaseTest {
  private static final String TEST_ACCOUNT_ID = "ACCOUNT_ID";
  private static final String TEST_ACCOUNT_NAME = "ACCOUNT_NAME";

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Mock private AccountService accountService;

  @Mock private LimitCheckerFactory limitCheckerFactory;

  @InjectMocks @Inject private PreDeploymentChecks preDeploymentChecker;
  @InjectMocks @Inject @RateLimitCheck private PreDeploymentChecker rateLimitChecker;
  @Mock private MainConfiguration mainConfiguration;

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void checkDeploymentRateLimit() {
    when(mainConfiguration.getDeployMode()).thenReturn(DeployMode.ONPREM);

    String accountId = "some-account-id";
    rateLimitChecker.check(accountId);

    verifyZeroInteractions(limitCheckerFactory);
  }

  @Before
  public void setup() {
    when(accountService.get(Mockito.any(String.class)))
        .thenReturn(anAccount()
                        .withCompanyName(TEST_ACCOUNT_NAME)
                        .withAccountName(TEST_ACCOUNT_NAME)
                        .withAccountKey(TEST_ACCOUNT_NAME)
                        .withLicenseInfo(getLicenseInfoForType(AccountType.COMMUNITY))
                        .build());
  }

  private LicenseInfo getLicenseInfoForType(@Nonnull String accountType) {
    LicenseInfo licenseInfo = getLicenseInfo();
    licenseInfo.setAccountType(accountType);
    return licenseInfo;
  }

  @Test()
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  public void testWorkflowPreDeploymentCheckerViolation() {
    Workflow workflow = getWorkflow(true);
    thrown.expect(WingsException.class);
    thrown.expectMessage(getWorkflowRestrictedFeatureErrorMsg(workflow.getName()));
    when(accountService.getAccountType(ACCOUNT_ID)).thenReturn(Optional.of(AccountType.COMMUNITY));
    preDeploymentChecker.checkIfWorkflowUsingRestrictedFeatures(workflow);
  }

  @Test()
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  public void testPipelinePreDeploymentCheckerViolation() {
    Pipeline pipeline = getPipeline();
    thrown.expect(WingsException.class);
    thrown.expectMessage(APPROVAL_ERROR_MSG);
    when(accountService.getAccountType(ACCOUNT_ID)).thenReturn(Optional.of(AccountType.COMMUNITY));
    preDeploymentChecker.checkIfPipelineUsingRestrictedFeatures(pipeline);
  }

  public static final Workflow getWorkflow(boolean addFlowControl) {
    return aWorkflow()
        .name(WORKFLOW_NAME)
        .uuid(generateUuid())
        .appId(APP_ID)
        .accountId(TEST_ACCOUNT_ID)
        .serviceId(SERVICE_ID)
        .infraMappingId(INFRA_MAPPING_ID)
        .workflowType(WorkflowType.ORCHESTRATION)
        .envId(ENV_ID)
        .orchestrationWorkflow(
            aBasicOrchestrationWorkflow()
                .withPreDeploymentSteps(
                    aPhaseStep(PRE_DEPLOYMENT)
                        .addStep(GraphNode.builder()
                                     .type(addFlowControl ? StateType.BARRIER.name() : StateType.SUB_WORKFLOW.name())
                                     .build())
                        .build())
                .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT).build())
                .withWorkflowPhases(Collections.singletonList(
                    WorkflowPhaseBuilder.aWorkflowPhase()
                        .phaseSteps(Collections.singletonList(

                            aPhaseStep(DEPLOY_SERVICE)
                                .addStep(GraphNode.builder()
                                             .type(addFlowControl ? StateType.RESOURCE_CONSTRAINT.name()
                                                                  : StateType.SUB_WORKFLOW.name())
                                             .build()

                                        )
                                .build()

                                ))
                        .build()

                        ))
                .build())
        .build();
  }

  private Pipeline getPipeline() {
    return Pipeline.builder()
        .name("pipeline1")
        .accountId(ACCOUNT_ID)
        .appId(APP_ID)
        .uuid(PIPELINE_ID)
        .pipelineStages(asList(prepareStage()))
        .build();
  }

  private PipelineStage prepareStage() {
    Map<String, Object> envStateproperties = new HashMap<>();
    envStateproperties.put("envId", ENV_ID);
    envStateproperties.put("workflowId", WORKFLOW_ID);

    Map<String, Object> approvalStateProperties = new HashMap<>();
    approvalStateProperties.put(APPROVAL_STATE_TYPE_VARIABLE, ApprovalStateType.SHELL_SCRIPT);

    return PipelineStage.builder()
        .pipelineStageElements(asList(PipelineStageElement.builder()
                                          .name("STAGE1")
                                          .type(StateType.ENV_STATE.name())
                                          .properties(envStateproperties)
                                          .build(),
            PipelineStageElement.builder()
                .name("STAGE2")
                .type(StateType.APPROVAL.name())
                .properties(approvalStateProperties)
                .build()))
        .build();
  }
}
