package software.wings.sm.states;

import static io.harness.beans.ExecutionStatus.FAILED;
import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.rule.OwnerRule.ANIL;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.AwsAmiInfrastructureMapping.Builder.anAwsAmiInfrastructureMapping;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;

import io.harness.beans.EmbeddedUser;
import io.harness.category.element.UnitTests;
import io.harness.delegate.task.aws.LbDetailsForAlbTrafficShift;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.stubbing.Answer;
import software.wings.WingsBaseTest;
import software.wings.api.AwsAmiSetupExecutionData;
import software.wings.beans.Activity;
import software.wings.beans.AwsConfig;
import software.wings.beans.DeploymentExecutionContext;
import software.wings.beans.Service;
import software.wings.beans.command.AmiCommandUnit;
import software.wings.beans.command.Command;
import software.wings.beans.command.ServiceCommand;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.states.spotinst.SpotInstStateHelper;

import java.util.Map;

public class AwsAmiServiceTrafficShiftAlbSetupTest extends WingsBaseTest {
  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testExecuteSuccess() {
    AwsAmiServiceTrafficShiftAlbSetup state = spy(new AwsAmiServiceTrafficShiftAlbSetup("Setup Test"));
    DeploymentExecutionContext mockContext = mock(DeploymentExecutionContext.class);
    initializeMockSetup(state, mockContext, true);
    verifyTestResult(state.execute(mockContext));
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testExecuteFailure() {
    AwsAmiServiceTrafficShiftAlbSetup state = spy(new AwsAmiServiceTrafficShiftAlbSetup("Setup Test"));
    DeploymentExecutionContext mockContext = mock(DeploymentExecutionContext.class);
    initializeMockSetup(state, mockContext, false);
    ExecutionResponse response = state.execute(mockContext);
    assertThat(response.getExecutionStatus()).isEqualTo(FAILED);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testValidateFields() {
    AwsAmiServiceTrafficShiftAlbSetup state = spy(new AwsAmiServiceTrafficShiftAlbSetup("Setup Test"));
    Map<String, String> fieldsMap = state.validateFields();
    assertThat(fieldsMap).isNotNull();
    assertThat(fieldsMap.size()).isEqualTo(2);
  }

  private void initializeMockSetup(
      AwsAmiServiceTrafficShiftAlbSetup state, DeploymentExecutionContext mockContext, boolean isSuccess) {
    state.setMinInstances("0");
    state.setMaxInstances("1");
    state.setDesiredInstances("1");
    state.setAutoScalingSteadyStateTimeout("10");
    state.setUseCurrentRunningCount(true);
    state.setLbDetails(singletonList(LbDetailsForAlbTrafficShift.builder()
                                         .loadBalancerName("lbName")
                                         .loadBalancerArn("lbArn")
                                         .listenerArn("listArn")
                                         .listenerPort("8080")
                                         .useSpecificRule(true)
                                         .ruleArn("ruleArn")
                                         .build()));

    ActivityService activityService = mock(ActivityService.class);
    ServiceResourceService serviceResourceService = mock(ServiceResourceService.class);
    SpotInstStateHelper spotInstStateHelper = mock(SpotInstStateHelper.class);
    DelegateService delegateService = mock(DelegateService.class);
    AwsAmiServiceStateHelper awsAmiServiceHelper = mock(AwsAmiServiceStateHelper.class);

    on(state).set("activityService", activityService);
    on(state).set("serviceResourceService", serviceResourceService);
    on(state).set("spotinstStateHelper", spotInstStateHelper);
    on(state).set("delegateService", delegateService);
    on(state).set("awsAmiServiceHelper", awsAmiServiceHelper);

    when(mockContext.renderExpression(anyString())).thenAnswer((Answer<String>) invocation -> {
      Object[] args = invocation.getArguments();
      return (String) args[0];
    });

    AwsAmiTrafficShiftAlbData trafficShiftAlbData =
        AwsAmiTrafficShiftAlbData.builder()
            .artifact(anArtifact().withUuid(ARTIFACT_ID).build())
            .app(anApplication().uuid(APP_ID).build())
            .service(Service.builder().build())
            .env(anEnvironment().uuid(ENV_ID).build())
            .awsConfig(AwsConfig.builder().build())
            .infrastructureMapping(
                anAwsAmiInfrastructureMapping().withUuid(INFRA_MAPPING_ID).withSpotinstElastiGroupJson("json").build())
            .awsEncryptedDataDetails(emptyList())
            .region("region")
            .serviceId("serviceId")
            .currentUser(EmbeddedUser.builder().build())
            .build();
    doReturn(trafficShiftAlbData).when(awsAmiServiceHelper).populateAlbTrafficShiftSetupData(mockContext);
    doReturn(Activity.builder().uuid(ACTIVITY_ID).commandUnits(singletonList(new AmiCommandUnit())).build())
        .when(activityService)
        .save(any());
    doReturn(ServiceCommand.Builder.aServiceCommand()
                 .withName("Aws Ami Setup")
                 .withCommand(Command.Builder.aCommand().build())
                 .build())
        .when(serviceResourceService)
        .getCommandByName(any(), any(), any(), any());
    doReturn(emptyList()).when(serviceResourceService).getFlattenCommandUnitList(any(), any(), any(), any());
    if (!isSuccess) {
      doThrow(Exception.class).when(delegateService).queueTask(any());
    }
  }

  private void verifyTestResult(ExecutionResponse response) {
    assertThat(response).isNotNull();
    assertThat(response.getCorrelationIds().size()).isEqualTo(1);
    assertThat(response.getCorrelationIds().get(0)).isEqualTo(ACTIVITY_ID);
    assertThat(response.getExecutionStatus()).isEqualTo(SUCCESS);
    assertThat(response.getStateExecutionData()).isNotNull();
    assertThat(response.getStateExecutionData() instanceof AwsAmiSetupExecutionData).isTrue();
    AwsAmiSetupExecutionData data = (AwsAmiSetupExecutionData) response.getStateExecutionData();
    assertThat(data.getActivityId()).isEqualTo(ACTIVITY_ID);
  }
}
