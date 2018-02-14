package software.wings.sm.states;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static software.wings.beans.TemplateExpression.Builder.aTemplateExpression;
import static software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder.aWorkflowExecution;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.WingsBaseTest;
import software.wings.app.MainConfiguration;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.Application;
import software.wings.beans.SettingAttribute;
import software.wings.common.TemplateExpressionProcessor;
import software.wings.dl.WingsPersistence;
import software.wings.scheduler.QuartzScheduler;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateType;
import software.wings.waitnotify.WaitNotifyEngine;

import java.util.Collections;
import java.util.Date;
import java.util.UUID;

/**
 * author Srinivas
 */
public class AppDynamicsStateTest extends WingsBaseTest {
  private String accountId;
  private String appId;
  private String stateExecutionId;
  private String workflowId;
  private String workflowExecutionId;
  private String serviceId;
  @Mock private ExecutionContext executionContext;

  @Mock private BroadcasterFactory broadcasterFactory;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private AppService appService;
  @Inject private SettingsService settingsService;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private DelegateService delegateService;
  @Inject private MainConfiguration configuration;
  @Inject private SecretManager secretManager;
  @Inject private TemplateExpressionProcessor templateExpressionProcessor;
  @Mock private MetricDataAnalysisService metricAnalysisService;
  @Mock private QuartzScheduler jobScheduler;

  private AppDynamicsState appDynamicsState;

  @Before
  public void setup() {
    accountId = UUID.randomUUID().toString();
    appId = UUID.randomUUID().toString();
    stateExecutionId = UUID.randomUUID().toString();
    workflowId = UUID.randomUUID().toString();
    workflowExecutionId = UUID.randomUUID().toString();
    serviceId = UUID.randomUUID().toString();

    wingsPersistence.save(Application.Builder.anApplication().withUuid(appId).withAccountId(accountId).build());
    wingsPersistence.save(aWorkflowExecution().withAppId(appId).withWorkflowId(workflowId).build());
    configuration.getPortal().setJwtExternalServiceSecret(accountId);
    MockitoAnnotations.initMocks(this);

    when(executionContext.getAppId()).thenReturn(appId);
    when(executionContext.getWorkflowExecutionId()).thenReturn(workflowExecutionId);
    when(executionContext.getStateExecutionInstanceId()).thenReturn(stateExecutionId);

    Broadcaster broadcaster = mock(Broadcaster.class);
    when(broadcaster.broadcast(anyObject())).thenReturn(null);
    when(broadcasterFactory.lookup(anyObject(), anyBoolean())).thenReturn(broadcaster);
    setInternalState(delegateService, "broadcasterFactory", broadcasterFactory);

    when(jobScheduler.scheduleJob(anyObject(), anyObject())).thenReturn(new Date());

    appDynamicsState = new AppDynamicsState("AppDynamicsState");
    appDynamicsState.setApplicationId("30444");
    appDynamicsState.setTierId("456");
    appDynamicsState.setTimeDuration("6000");

    setInternalState(appDynamicsState, "appService", appService);
    setInternalState(appDynamicsState, "configuration", configuration);
    setInternalState(appDynamicsState, "settingsService", settingsService);
    setInternalState(appDynamicsState, "waitNotifyEngine", waitNotifyEngine);
    setInternalState(appDynamicsState, "delegateService", delegateService);
    setInternalState(appDynamicsState, "jobScheduler", jobScheduler);
    setInternalState(appDynamicsState, "secretManager", secretManager);
    setInternalState(appDynamicsState, "metricAnalysisService", metricAnalysisService);
    setInternalState(appDynamicsState, "templateExpressionProcessor", templateExpressionProcessor);
  }

  @Test
  public void shouldTestNonTemplatized() {
    AppDynamicsConfig appDynamicsConfig = AppDynamicsConfig.builder()
                                              .accountId(accountId)
                                              .controllerUrl("appd-url")
                                              .username("appd-user")
                                              .password("appd-pwd".toCharArray())
                                              .build();
    SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                            .withAccountId(accountId)
                                            .withName("appd-config")
                                            .withValue(appDynamicsConfig)
                                            .build();
    wingsPersistence.save(settingAttribute);
    appDynamicsState.setAnalysisServerConfigId(settingAttribute.getUuid());

    AppDynamicsState spyAppDynamicsState = spy(appDynamicsState);
    doReturn(Collections.singleton("test")).when(spyAppDynamicsState).getCanaryNewHostNames(executionContext);
    doReturn(Collections.singleton("control")).when(spyAppDynamicsState).getLastExecutionNodes(executionContext);
    doReturn(workflowId).when(spyAppDynamicsState).getWorkflowId(executionContext);
    doReturn(serviceId).when(spyAppDynamicsState).getPhaseServiceId(executionContext);

    when(metricAnalysisService.getLastSuccessfulWorkflowExecutionIdWithData(
             StateType.APP_DYNAMICS, workflowId, serviceId))
        .thenReturn(workflowExecutionId);
    ExecutionResponse executionResponse = spyAppDynamicsState.execute(executionContext);
    assertEquals(ExecutionStatus.RUNNING, executionResponse.getExecutionStatus());
  }

  @Test
  public void shouldTestAllTemplatized() {
    AppDynamicsConfig appDynamicsConfig = AppDynamicsConfig.builder()
                                              .accountId(accountId)
                                              .controllerUrl("appd-url")
                                              .username("appd-user")
                                              .password("appd-pwd".toCharArray())
                                              .build();
    SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                            .withAccountId(accountId)
                                            .withName("appd-config")
                                            .withValue(appDynamicsConfig)
                                            .build();
    wingsPersistence.save(settingAttribute);
    appDynamicsState.setAnalysisServerConfigId(settingAttribute.getUuid());

    appDynamicsState.setTemplateExpressions(
        asList(aTemplateExpression()
                   .withFieldName("analysisServerConfigId")
                   .withExpression("${AppDynamics_Server}")
                   .withMetadata(ImmutableMap.of("entityType", "APPDYNAMICS_CONFIGID"))
                   .build(),
            aTemplateExpression()
                .withFieldName("applicationId")
                .withExpression("${AppDynamics_App}")
                .withMetadata(ImmutableMap.of("entityType", "APPDYNAMICS_APPID"))
                .build(),
            aTemplateExpression()
                .withFieldName("tierId")
                .withExpression("${AppDynamics_Tier}")
                .withMetadata(ImmutableMap.of("entityType", "APPDYNAMICS_TIERID"))
                .build()));

    AppDynamicsState spyAppDynamicsState = spy(appDynamicsState);
    doReturn(Collections.singleton("test")).when(spyAppDynamicsState).getCanaryNewHostNames(executionContext);
    doReturn(Collections.singleton("control")).when(spyAppDynamicsState).getLastExecutionNodes(executionContext);
    doReturn(workflowId).when(spyAppDynamicsState).getWorkflowId(executionContext);
    doReturn(serviceId).when(spyAppDynamicsState).getPhaseServiceId(executionContext);

    when(metricAnalysisService.getLastSuccessfulWorkflowExecutionIdWithData(
             StateType.APP_DYNAMICS, workflowId, serviceId))
        .thenReturn(workflowExecutionId);
    when(executionContext.renderExpression("${workflow.variables.AppDynamics_Server}"))
        .thenReturn(settingAttribute.getUuid());
    when(executionContext.renderExpression("${workflow.variables.AppDynamics_App}")).thenReturn("30444");
    when(executionContext.renderExpression("${workflow.variables.AppDynamics_Tier}")).thenReturn("30889");

    ExecutionResponse executionResponse = spyAppDynamicsState.execute(executionContext);
    assertEquals(ExecutionStatus.RUNNING, executionResponse.getExecutionStatus());
  }
}
