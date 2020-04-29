package software.wings.sm.states.spotinst;

import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.rule.OwnerRule.SATYAM;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
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
import static software.wings.utils.WingsTestConstants.SERVICE_ID;

import com.google.common.collect.ImmutableMap;

import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.command.CommandExecutionResult;
import io.harness.delegate.task.aws.LbDetailsForAlbTrafficShift;
import io.harness.delegate.task.spotinst.response.SpotInstTaskExecutionResponse;
import io.harness.delegate.task.spotinst.response.SpotinstTrafficShiftAlbSetupResponse;
import io.harness.rule.Owner;
import io.harness.spotinst.model.ElastiGroup;
import io.harness.spotinst.model.ElastiGroupCapacity;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import software.wings.WingsBaseTest;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.beans.Activity;
import software.wings.beans.AwsConfig;
import software.wings.beans.DeploymentExecutionContext;
import software.wings.beans.SpotInstConfig;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;

import java.util.List;
import java.util.Map;

public class SpotinstTrafficShiftAlbSetupStateTest extends WingsBaseTest {
  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testExecute() {
    SpotinstTrafficShiftAlbSetupState state = spy(SpotinstTrafficShiftAlbSetupState.class);
    on(state).set("minInstancesExpr", "0");
    on(state).set("maxInstancesExpr", "1");
    on(state).set("targetInstancesExpr", "1");
    on(state).set("elastigroupNamePrefix", "foo");
    on(state).set("timeoutIntervalInMinExpr", "10");
    on(state).set("useCurrentRunningCount", true);
    on(state).set("lbDetails",
        singletonList(LbDetailsForAlbTrafficShift.builder()
                          .loadBalancerName("lbName")
                          .loadBalancerArn("lbArn")
                          .listenerArn("listArn")
                          .listenerPort("8080")
                          .useSpecificRule(true)
                          .ruleArn("ruleArn")
                          .build()));
    ActivityService mockActivityService = mock(ActivityService.class);
    on(state).set("activityService", mockActivityService);
    DelegateService mockDelegateService = mock(DelegateService.class);
    on(state).set("delegateService", mockDelegateService);
    SpotInstStateHelper mockSpotinstStateHelper = mock(SpotInstStateHelper.class);
    on(state).set("spotinstStateHelper", mockSpotinstStateHelper);
    DeploymentExecutionContext mockContext = mock(DeploymentExecutionContext.class);
    when(mockContext.renderExpression(anyString())).thenAnswer(new Answer<String>() {
      @Override
      public String answer(InvocationOnMock invocation) throws Throwable {
        Object[] args = invocation.getArguments();
        return (String) args[0];
      }
    });
    SpotinstTrafficShiftDataBag dataBag =
        SpotinstTrafficShiftDataBag.builder()
            .app(anApplication().uuid(APP_ID).build())
            .env(anEnvironment().uuid(ENV_ID).build())
            .infrastructureMapping(
                anAwsAmiInfrastructureMapping().withUuid(INFRA_MAPPING_ID).withSpotinstElastiGroupJson("json").build())
            .awsConfig(AwsConfig.builder().build())
            .awsEncryptedDataDetails(emptyList())
            .spotinstConfig(SpotInstConfig.builder().build())
            .spotinstEncryptedDataDetails(emptyList())
            .build();
    doReturn(dataBag).when(mockSpotinstStateHelper).getDataBag(any());
    PhaseElement mockPhaseElement = mock(PhaseElement.class);
    doReturn(mockPhaseElement).when(mockContext).getContextElement(any(), anyString());
    doReturn(ServiceElement.builder().uuid(SERVICE_ID).build()).when(mockPhaseElement).getServiceElement();
    doReturn(anArtifact().withUuid(ARTIFACT_ID).build()).when(mockContext).getDefaultArtifactForService(anyString());
    doReturn(Activity.builder().uuid(ACTIVITY_ID).build())
        .when(mockSpotinstStateHelper)
        .createActivity(any(), any(), anyString(), anyString(), any(), anyList());
    doReturn(ElastiGroup.builder()
                 .id("elastId")
                 .name("elastName")
                 .capacity(ElastiGroupCapacity.builder().minimum(0).maximum(1).target(1).build())
                 .build())
        .when(mockSpotinstStateHelper)
        .generateConfigFromJson(anyString());
    ExecutionResponse response = state.execute(mockContext);
    assertThat(response).isNotNull();
    assertThat(response.getCorrelationIds().size()).isEqualTo(1);
    assertThat(response.getCorrelationIds().get(0)).isEqualTo(ACTIVITY_ID);
    assertThat(response.getExecutionStatus()).isEqualTo(SUCCESS);
    assertThat(response.getStateExecutionData()).isNotNull();
    assertThat(response.getStateExecutionData() instanceof SpotinstTrafficShiftAlbSetupExecutionData).isTrue();
    SpotinstTrafficShiftAlbSetupExecutionData data =
        (SpotinstTrafficShiftAlbSetupExecutionData) response.getStateExecutionData();
    assertThat(data.getServiceId()).isEqualTo(SERVICE_ID);
    assertThat(data.getEnvId()).isEqualTo(ENV_ID);
    assertThat(data.getElastigroupOriginalConfig().getId()).isEqualTo("elastId");
    assertThat(data.getElastigroupOriginalConfig().getName()).isEqualTo("elastName");
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testHandleAsyncResponse() {
    SpotinstTrafficShiftAlbSetupState state = spy(SpotinstTrafficShiftAlbSetupState.class);
    on(state).set("useCurrentRunningCount", true);
    on(state).set("elastigroupNamePrefix", "foo");
    ActivityService mockActivityService = mock(ActivityService.class);
    on(state).set("activityService", mockActivityService);
    SpotInstStateHelper mockSpotinstStateHelper = mock(SpotInstStateHelper.class);
    on(state).set("spotinstStateHelper", mockSpotinstStateHelper);
    doReturn(10).when(mockSpotinstStateHelper).renderCount(anyString(), any(), anyInt());
    SpotInstTaskExecutionResponse delegateResponse =
        SpotInstTaskExecutionResponse.builder()
            .delegateMetaInfo(DelegateMetaInfo.builder().build())
            .commandExecutionStatus(CommandExecutionResult.CommandExecutionStatus.SUCCESS)
            .spotInstTaskResponse(
                SpotinstTrafficShiftAlbSetupResponse.builder()
                    .newElastigroup(ElastiGroup.builder()
                                        .name("newName")
                                        .id("newId")
                                        .capacity(ElastiGroupCapacity.builder().minimum(0).maximum(0).target(0).build())
                                        .build())
                    .elastiGroupsToBeDownsized(singletonList(
                        ElastiGroup.builder()
                            .id("oldId")
                            .name("oldName")
                            .capacity(ElastiGroupCapacity.builder().minimum(0).maximum(1).target(1).build())
                            .build()))
                    .lbDetailsWithTargetGroups(singletonList(LbDetailsForAlbTrafficShift.builder()
                                                                 .loadBalancerName("lbName")
                                                                 .loadBalancerArn("lbArn")
                                                                 .listenerArn("listArn")
                                                                 .listenerPort("8080")
                                                                 .useSpecificRule(true)
                                                                 .ruleArn("ruleArn")
                                                                 .prodTargetGroupName("prodTgtName")
                                                                 .prodTargetGroupArn("prodTgtArn")
                                                                 .stageTargetGroupName("stageTgtName")
                                                                 .stageTargetGroupArn("stageTgtArn")
                                                                 .build()))
                    .build())
            .build();
    SpotinstTrafficShiftAlbSetupExecutionData stateExecutionData =
        SpotinstTrafficShiftAlbSetupExecutionData.builder()
            .appId(APP_ID)
            .envId(ENV_ID)
            .infraMappingId(INFRA_MAPPING_ID)
            .serviceId(SERVICE_ID)
            .elastigroupOriginalConfig(
                ElastiGroup.builder()
                    .id("foo")
                    .name("nameFoo")
                    .capacity(ElastiGroupCapacity.builder().minimum(1).maximum(2).target(1).build())
                    .build())
            .build();
    ExecutionContext mockContext = mock(ExecutionContext.class);
    when(mockContext.renderExpression(anyString())).thenAnswer(new Answer<String>() {
      @Override
      public String answer(InvocationOnMock invocation) throws Throwable {
        Object[] args = invocation.getArguments();
        return (String) args[0];
      }
    });
    doReturn(stateExecutionData).when(mockContext).getStateExecutionData();
    ExecutionResponse response = state.handleAsyncResponse(mockContext, ImmutableMap.of(ACTIVITY_ID, delegateResponse));
    assertThat(stateExecutionData.getNewElastigroupId()).isEqualTo("newId");
    assertThat(stateExecutionData.getNewElastigroupName()).isEqualTo("newName");
    assertThat(stateExecutionData.getOldElastigroupId()).isEqualTo("oldId");
    assertThat(stateExecutionData.getOldElastigroupName()).isEqualTo("oldName");
    assertThat(response).isNotNull();
    assertThat(response.getExecutionStatus()).isEqualTo(SUCCESS);
    List<ContextElement> contextElements = response.getContextElements();
    assertThat(contextElements).isNotNull();
    assertThat(contextElements.size()).isEqualTo(1);
    ContextElement contextElement = contextElements.get(0);
    assertThat(contextElement).isNotNull();
    assertThat(contextElement instanceof SpotinstTrafficShiftAlbSetupElement).isTrue();
    SpotinstTrafficShiftAlbSetupElement setupElement = (SpotinstTrafficShiftAlbSetupElement) contextElement;
    assertThat(setupElement.getOldElastiGroupOriginalConfig().getCapacity().getMinimum()).isEqualTo(0);
    assertThat(setupElement.getOldElastiGroupOriginalConfig().getCapacity().getMaximum()).isEqualTo(1);
    assertThat(setupElement.getOldElastiGroupOriginalConfig().getCapacity().getTarget()).isEqualTo(1);
    assertThat(setupElement.getNewElastiGroupOriginalConfig().getCapacity().getMinimum()).isEqualTo(0);
    assertThat(setupElement.getNewElastiGroupOriginalConfig().getCapacity().getMaximum()).isEqualTo(1);
    assertThat(setupElement.getNewElastiGroupOriginalConfig().getCapacity().getTarget()).isEqualTo(1);
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testValidateFields() {
    SpotinstTrafficShiftAlbSetupState state = spy(SpotinstTrafficShiftAlbSetupState.class);
    Map<String, String> fieldsMap = state.validateFields();
    assertThat(fieldsMap).isNotNull();
    assertThat(fieldsMap.size()).isEqualTo(2);
  }
}