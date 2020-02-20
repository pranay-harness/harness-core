package software.wings.sm.states;

import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.rule.OwnerRule.SATYAM;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.AwsAmiInfrastructureMapping.Builder.anAwsAmiInfrastructureMapping;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;

import com.google.common.collect.ImmutableMap;

import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.AmiServiceSetupElement;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.AwsConfig;
import software.wings.beans.Environment;
import software.wings.beans.SettingAttribute;
import software.wings.service.impl.aws.model.AwsAmiSwitchRoutesRequest;
import software.wings.service.impl.aws.model.AwsAmiSwitchRoutesResponse;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;

public class AwsAmiSwitchRoutesStateTest extends WingsBaseTest {
  @Mock private SettingsService mockSettingsService;
  @Mock private InfrastructureMappingService mockInfrastructureMappingService;
  @Mock private ActivityService mockActivityService;
  @Mock private SecretManager mockSecretManager;
  @Mock private DelegateService mockDelegateService;

  @InjectMocks private AwsAmiSwitchRoutesState state = new AwsAmiSwitchRoutesState("stateName");

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testExecute() {
    state.setDownsizeOldAsg(true);
    ExecutionContextImpl mockContext = mock(ExecutionContextImpl.class);
    Application application = anApplication().uuid(APP_ID).name(APP_NAME).accountId(ACCOUNT_ID).build();
    doReturn(application).when(mockContext).fetchRequiredApp();
    Environment environment = anEnvironment().uuid(ENV_ID).name(ENV_NAME).build();
    doReturn(environment).when(mockContext).fetchRequiredEnvironment();
    Activity activity = Activity.builder().uuid(ACTIVITY_ID).appId(APP_ID).build();
    doReturn(activity).when(mockActivityService).save(any());
    AmiServiceSetupElement serviceSetupElement = AmiServiceSetupElement.builder()
                                                     .oldAutoScalingGroupName("foo__1")
                                                     .newAutoScalingGroupName("foo__2")
                                                     .autoScalingSteadyStateTimeout(10)
                                                     .build();
    doReturn(serviceSetupElement).when(mockContext).getContextElement(any());
    String classicLb = "classicLb";
    String stageDlassicLb = "stageClassicLb";
    String targetGroup = "targetGp";
    String stageTargetGroup = "stageTargetGp";
    String baseAsg = "baseAsg";
    AwsAmiInfrastructureMapping infrastructureMapping =
        anAwsAmiInfrastructureMapping()
            .withUuid(INFRA_MAPPING_ID)
            .withRegion("us-east-1")
            .withClassicLoadBalancers(singletonList(classicLb))
            .withStageClassicLoadBalancers(singletonList(stageDlassicLb))
            .withTargetGroupArns(singletonList(targetGroup))
            .withStageTargetGroupArns(singletonList(stageTargetGroup))
            .withAutoScalingGroupName(baseAsg)
            .build();
    doReturn(infrastructureMapping).when(mockInfrastructureMappingService).get(anyString(), anyString());
    SettingAttribute cloudProvider = aSettingAttribute().withValue(AwsConfig.builder().build()).build();
    doReturn(cloudProvider).when(mockSettingsService).get(anyString());
    doReturn(emptyList()).when(mockSecretManager).getEncryptionDetails(any(), anyString(), anyString());
    ExecutionResponse response = state.execute(mockContext);
    ArgumentCaptor<DelegateTask> captor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(mockDelegateService).queueTask(captor.capture());
    DelegateTask delegateTask = captor.getValue();
    assertThat(delegateTask).isNotNull();
    assertThat(delegateTask.getData().getParameters()).isNotNull();
    assertThat(1).isEqualTo(delegateTask.getData().getParameters().length);
    assertThat(delegateTask.getData().getParameters()[0] instanceof AwsAmiSwitchRoutesRequest).isTrue();
    AwsAmiSwitchRoutesRequest params = (AwsAmiSwitchRoutesRequest) delegateTask.getData().getParameters()[0];
    assertThat(params.getOldAsgName()).isEqualTo("foo__1");
    assertThat(params.getNewAsgName()).isEqualTo("foo__2");
    assertThat(params.getPrimaryClassicLBs().size()).isEqualTo(1);
    assertThat(params.getPrimaryClassicLBs().get(0)).isEqualTo(classicLb);
    assertThat(params.getPrimaryTargetGroupARNs().size()).isEqualTo(1);
    assertThat(params.getPrimaryTargetGroupARNs().get(0)).isEqualTo(targetGroup);
    assertThat(params.getStageClassicLBs().size()).isEqualTo(1);
    assertThat(params.getStageClassicLBs().get(0)).isEqualTo(stageDlassicLb);
    assertThat(params.getStageTargetGroupARNs().size()).isEqualTo(1);
    assertThat(params.getStageTargetGroupARNs().get(0)).isEqualTo(stageTargetGroup);
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testHandleAsyncResponse() {
    ExecutionContextImpl mockContext = mock(ExecutionContextImpl.class);
    AwsAmiSwitchRoutesResponse delegateResponse = AwsAmiSwitchRoutesResponse.builder().executionStatus(SUCCESS).build();
    ExecutionResponse response = state.handleAsyncResponse(mockContext, ImmutableMap.of(ACTIVITY_ID, delegateResponse));
    assertThat(response.getExecutionStatus()).isEqualTo(SUCCESS);
  }
}