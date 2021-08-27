package io.harness.ng.cv.listener;

import static io.harness.rule.OwnerRule.ABHIJITH;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.service.steps.ServiceStepOutcome;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.cvng.beans.change.event.ChangeEventDTO;
import io.harness.cvng.beans.change.event.metadata.HarnessCDEventMetaData;
import io.harness.cvng.client.CVNGClient;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.plan.execution.SetupAbstractionKeys;
import io.harness.pms.sdk.core.data.OptionalOutcome;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.rule.Owner;
import io.harness.steps.environment.EnvironmentOutcome;

import java.time.Instant;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import retrofit2.Call;

@OwnedBy(HarnessTeam.CV)
public class HarnessCDChangeEventListenerTest extends CategoryTest {
  @Mock OutcomeService outcomeService;
  @Mock CVNGClient cvngClient;
  @InjectMocks @Spy HarnessCDChangeEventListener harnessCDChangeEventListener;

  OrchestrationEvent orchestrationEvent;

  String accountId;
  String orgIdentifier;
  String projectIdentifier;
  String serviceIdentifier;
  String environmentIdentifier;
  long deploymentStartTime;
  long deploymentEndTime;
  String executionId;
  String stageId;

  @Before
  public void before() {
    accountId = "accountId";
    orgIdentifier = "orgIdentifier";
    projectIdentifier = "projectIdentifier";
    serviceIdentifier = "serviceIdentifier";
    environmentIdentifier = "environmentIdentifier";
    deploymentStartTime = Instant.now().getEpochSecond();
    deploymentEndTime = Instant.now().getEpochSecond();
    executionId = "executionId";
    stageId = "stageId";
    orchestrationEvent =
        OrchestrationEvent.builder()
            .ambiance(Ambiance.newBuilder()
                          .setPlanExecutionId(executionId)
                          .addLevels(Level.newBuilder()
                                         .setStepType(StepType.newBuilder()
                                                          .setType("DEPLOYMENT_STAGE_STEP")
                                                          .setStepCategory(StepCategory.STAGE)
                                                          .build())
                                         .setStartTs(deploymentStartTime)
                                         .setIdentifier(stageId)
                                         .build())
                          .putSetupAbstractions(SetupAbstractionKeys.accountId, accountId)
                          .putSetupAbstractions(SetupAbstractionKeys.orgIdentifier, orgIdentifier)
                          .putSetupAbstractions(SetupAbstractionKeys.projectIdentifier, projectIdentifier)
                          .build())
            .status(Status.SUCCEEDED)
            .build();
    MockitoAnnotations.initMocks(this);
    Mockito
        .when(outcomeService.resolveOptional(
            Mockito.any(), Mockito.eq(RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.SERVICE))))
        .thenReturn(OptionalOutcome.builder()
                        .found(true)
                        .outcome(ServiceStepOutcome.builder().identifier(serviceIdentifier).build())
                        .build());
    InfrastructureOutcome infrastructureOutcome = Mockito.mock(InfrastructureOutcome.class);
    Mockito.when(infrastructureOutcome.getEnvironment())
        .thenReturn(EnvironmentOutcome.builder().identifier(environmentIdentifier).build());
    Mockito
        .when(outcomeService.resolveOptional(Mockito.any(),
            Mockito.eq(RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.INFRASTRUCTURE_OUTCOME))))
        .thenReturn(OptionalOutcome.builder().found(true).outcome(infrastructureOutcome).build());
    Mockito.when(cvngClient.registerChangeEvent(Mockito.any(), Mockito.any())).thenReturn(Mockito.mock(Call.class));
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void handleEvent_success() {
    final ArgumentCaptor<ChangeEventDTO> captor = ArgumentCaptor.forClass(ChangeEventDTO.class);
    harnessCDChangeEventListener.handleEvent(orchestrationEvent);
    Mockito.verify(cvngClient).registerChangeEvent(captor.capture(), Mockito.anyString());

    ChangeEventDTO changeEventDTO = captor.getValue();
    Assertions.assertThat(changeEventDTO.getAccountId()).isEqualTo(accountId);
    Assertions.assertThat(changeEventDTO.getOrgIdentifier()).isEqualTo(orgIdentifier);
    Assertions.assertThat(changeEventDTO.getProjectIdentifier()).isEqualTo(projectIdentifier);
    Assertions.assertThat(changeEventDTO.getServiceIdentifier()).isEqualTo(serviceIdentifier);
    Assertions.assertThat(changeEventDTO.getEnvIdentifier()).isEqualTo(environmentIdentifier);
    Assertions.assertThat(((HarnessCDEventMetaData) changeEventDTO.getChangeEventMetaData()).getStatus())
        .isEqualTo(Status.SUCCEEDED);
    Assertions.assertThat(((HarnessCDEventMetaData) changeEventDTO.getChangeEventMetaData()).getDeploymentEndTime())
        .isEqualTo(deploymentEndTime);
    Assertions.assertThat(((HarnessCDEventMetaData) changeEventDTO.getChangeEventMetaData()).getExecutionId())
        .isEqualTo(executionId);
    Assertions.assertThat(((HarnessCDEventMetaData) changeEventDTO.getChangeEventMetaData()).getStageId())
        .isEqualTo(stageId);
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void handleEvent_noServiceOutcome() {
    Mockito
        .when(outcomeService.resolveOptional(
            Mockito.any(), Mockito.eq(RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.SERVICE))))
        .thenReturn(OptionalOutcome.builder().found(false).build());
    harnessCDChangeEventListener.handleEvent(orchestrationEvent);
    Mockito.verifyZeroInteractions(cvngClient);
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void handleEvent_noInfrastructureOutcome() {
    Mockito
        .when(outcomeService.resolveOptional(Mockito.any(),
            Mockito.eq(RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.INFRASTRUCTURE_OUTCOME))))
        .thenReturn(OptionalOutcome.builder().found(false).build());
    harnessCDChangeEventListener.handleEvent(orchestrationEvent);
    Mockito.verifyZeroInteractions(cvngClient);
  }
}