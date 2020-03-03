package software.wings.sm.states;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.SOWMYA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.waiter.WaitNotifyEngine;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import software.wings.beans.Application;
import software.wings.beans.GcpConfig;
import software.wings.beans.SettingAttribute;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.stackdriver.StackDriverDataCollectionInfo;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.stackdriver.StackDriverService;
import software.wings.verification.VerificationStateAnalysisExecutionData;

import java.util.HashMap;
import java.util.Map;

public class StackDriverStateTest extends APMStateVerificationTestBase {
  @Mock private MetricDataAnalysisService metricAnalysisService;
  @Mock private DelegateService delegateService;
  @Mock private SettingsService settingsService;
  @Mock private StackDriverService stackDriverService;
  @Mock private SecretManager secretManager;
  @Mock private AppService appService;
  @Mock private WaitNotifyEngine waitNotifyEngine;

  private StackDriverState stackDriverState;
  private AnalysisContext analysisContext;
  private VerificationStateAnalysisExecutionData executionData;
  private Map<String, String> hosts;
  private String configId;
  private SettingAttribute settingAttribute;
  private GcpConfig gcpConfig;

  @Before
  public void setUp() throws IllegalAccessException {
    setupCommon();

    MockitoAnnotations.initMocks(this);

    analysisContext = AnalysisContext.builder().build();
    executionData = VerificationStateAnalysisExecutionData.builder().build();
    hosts = new HashMap<>();
    configId = generateUuid();

    stackDriverState = Mockito.spy(new StackDriverState("stackDriverState"));
    stackDriverState.setTimeDuration("10");
    gcpConfig = GcpConfig.builder().accountId(accountId).build();
    settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                           .withAccountId(accountId)
                           .withAppId(appId)
                           .withValue(gcpConfig)
                           .build();

    Application app = new Application();
    app.setName("name");

    FieldUtils.writeField(stackDriverState, "metricAnalysisService", metricAnalysisService, true);
    FieldUtils.writeField(stackDriverState, "delegateService", delegateService, true);
    FieldUtils.writeField(stackDriverState, "settingsService", settingsService, true);
    FieldUtils.writeField(stackDriverState, "stackDriverService", stackDriverService, true);
    FieldUtils.writeField(stackDriverState, "secretManager", secretManager, true);
    FieldUtils.writeField(stackDriverState, "appService", appService, true);
    FieldUtils.writeField(stackDriverState, "waitNotifyEngine", waitNotifyEngine, true);

    when(settingsService.get(configId)).thenReturn(settingAttribute);
    when(appService.get(any())).thenReturn(app);

    doReturn(configId).when(stackDriverState).getResolvedConnectorId(any(), any(), any());
    doReturn(workflowId).when(stackDriverState).getWorkflowId(any());
    doReturn(serviceId).when(stackDriverState).getPhaseServiceId(any());
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testTriggerAnalysisDataCollection_whenConnectorIdIsValid() {
    stackDriverState.triggerAnalysisDataCollection(executionContext, analysisContext, executionData, hosts);
    ArgumentCaptor<DelegateTask> delegateTaskArgumentCaptor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService, times(1)).queueTask(delegateTaskArgumentCaptor.capture());

    DelegateTask task = delegateTaskArgumentCaptor.getValue();
    StackDriverDataCollectionInfo dataCollectionInfo =
        (StackDriverDataCollectionInfo) task.getData().getParameters()[0];

    assertThat(dataCollectionInfo.getGcpConfig()).isEqualTo(gcpConfig);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testTriggerAnalysisDataCollection_whenConnectorIdIsInValid() {
    when(settingsService.get(configId)).thenReturn(null);
    assertThatThrownBy(
        () -> stackDriverState.triggerAnalysisDataCollection(executionContext, analysisContext, executionData, hosts))
        .isInstanceOf(NullPointerException.class);
  }
}