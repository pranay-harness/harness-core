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
import software.wings.beans.SettingAttribute;
import software.wings.beans.SumoConfig;
import software.wings.service.impl.sumo.SumoDataCollectionInfo;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.verification.VerificationStateAnalysisExecutionData;

import java.util.HashSet;
import java.util.Set;

public class SumoLogicAnalysisStateTest extends APMStateVerificationTestBase {
  @Mock private SettingsService settingsService;
  @Mock private AppService appService;
  @Mock private WaitNotifyEngine waitNotifyEngine;
  @Mock private DelegateService delegateService;
  @Mock private SecretManager secretManager;

  private SumoLogicAnalysisState sumoLogicAnalysisState;
  private SumoConfig sumoConfig;
  private String configId;
  private VerificationStateAnalysisExecutionData executionData;
  private Set<String> hosts;

  @Before
  public void setUp() throws IllegalAccessException {
    MockitoAnnotations.initMocks(this);

    String accountId = generateUuid();
    String appId = generateUuid();
    configId = generateUuid();
    sumoLogicAnalysisState = Mockito.spy(new SumoLogicAnalysisState("sumoLogicState"));
    sumoConfig = SumoConfig.builder().accountId(accountId).build();
    SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                            .withAccountId(accountId)
                                            .withAppId(appId)
                                            .withValue(sumoConfig)
                                            .build();
    executionData = VerificationStateAnalysisExecutionData.builder().build();
    hosts = new HashSet<>();
    hosts.add("host1");

    Application app = new Application();
    app.setName("name");

    FieldUtils.writeField(sumoLogicAnalysisState, "settingsService", settingsService, true);
    FieldUtils.writeField(sumoLogicAnalysisState, "appService", appService, true);
    FieldUtils.writeField(sumoLogicAnalysisState, "waitNotifyEngine", waitNotifyEngine, true);
    FieldUtils.writeField(sumoLogicAnalysisState, "delegateService", delegateService, true);
    FieldUtils.writeField(sumoLogicAnalysisState, "secretManager", secretManager, true);

    when(settingsService.get(configId)).thenReturn(settingAttribute);
    when(appService.get(any())).thenReturn(app);

    doReturn(configId).when(sumoLogicAnalysisState).getResolvedConnectorId(any(), any(), any());
    doReturn(workflowId).when(sumoLogicAnalysisState).getWorkflowId(any());
    doReturn(serviceId).when(sumoLogicAnalysisState).getPhaseServiceId(any());
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testTriggerAnalysisDataCollection_whenConnectorIdIsValid() {
    sumoLogicAnalysisState.triggerAnalysisDataCollection(executionContext, executionData, hosts);

    ArgumentCaptor<DelegateTask> delegateTaskArgumentCaptor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService, times(1)).queueTask(delegateTaskArgumentCaptor.capture());

    DelegateTask task = delegateTaskArgumentCaptor.getValue();
    SumoDataCollectionInfo dataCollectionInfo = (SumoDataCollectionInfo) task.getData().getParameters()[0];

    assertThat(dataCollectionInfo.getSumoConfig()).isEqualTo(sumoConfig);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testTriggerAnalysisDataCollection_whenConnectorIdIsInValid() {
    when(settingsService.get(configId)).thenReturn(null);
    assertThatThrownBy(
        () -> sumoLogicAnalysisState.triggerAnalysisDataCollection(executionContext, executionData, hosts))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("No connector found with id " + configId);
  }
}