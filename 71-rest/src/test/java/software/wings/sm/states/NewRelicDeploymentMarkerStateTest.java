package software.wings.sm.states;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.RAGHU;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.exception.WingsException;
import io.harness.rule.OwnerRule.Owner;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.NewRelicConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TemplateExpression;
import software.wings.common.TemplateExpressionProcessor;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.newrelic.NewRelicApplication;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.newrelic.NewRelicService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;

public class NewRelicDeploymentMarkerStateTest extends WingsBaseTest {
  @Inject private SettingsService settingsService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private SecretManager secretManager;
  @Inject private TemplateExpressionProcessor templateExpressionProcessor;
  @Mock private WorkflowExecutionService workflowExecutionService;
  @Mock private ExecutionContext executionContext;
  @Mock private NewRelicService newRelicService;
  @Mock private DelegateService delegateService;
  private String accountId;
  private String analysisServerConfigId;
  private NewRelicDeploymentMarkerState newRelicDeploymentMarkerState;

  @Before
  public void setup() throws Exception {
    accountId = generateUuid();
    analysisServerConfigId = generateUuid();
    when(workflowExecutionService.isTriggerBasedDeployment(any(ExecutionContext.class))).thenReturn(true);
    newRelicDeploymentMarkerState = new NewRelicDeploymentMarkerState("NewRelicDeploymentMarkerState");
    FieldUtils.writeField(newRelicDeploymentMarkerState, "settingsService", settingsService, true);
    FieldUtils.writeField(newRelicDeploymentMarkerState, "workflowExecutionService", workflowExecutionService, true);
    FieldUtils.writeField(newRelicDeploymentMarkerState, "newRelicService", newRelicService, true);
    FieldUtils.writeField(newRelicDeploymentMarkerState, "secretManager", secretManager, true);
    FieldUtils.writeField(newRelicDeploymentMarkerState, "delegateService", delegateService, true);
    FieldUtils.writeField(
        newRelicDeploymentMarkerState, "templateExpressionProcessor", templateExpressionProcessor, true);
    when(executionContext.getApp())
        .thenReturn(Application.Builder.anApplication().appId(generateUuid()).accountId(accountId).build());
    when(delegateService.queueTask(any(DelegateTask.class))).thenReturn(generateUuid());
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void shouldTestTriggered() {
    NewRelicConfig newRelicConfig = NewRelicConfig.builder()
                                        .accountId(accountId)
                                        .newRelicUrl("newrelic-url")
                                        .apiKey(generateUuid().toCharArray())
                                        .build();
    SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                            .withAccountId(accountId)
                                            .withName("relic-config")
                                            .withValue(newRelicConfig)
                                            .build();

    final String settingId = wingsPersistence.save(settingAttribute);
    doThrow(new WingsException("Can not find application by id"))
        .when(newRelicService)
        .resolveApplicationId(anyString(), anyString());

    doThrow(new WingsException("Can not find application by name"))
        .when(newRelicService)
        .resolveApplicationName(anyString(), anyString());

    when(executionContext.renderExpression("${workflow.variables.NewRelic_Server}"))
        .thenReturn(settingAttribute.getUuid());
    when(executionContext.renderExpression("${workflow.variables.NewRelic_App}")).thenReturn("30444");

    final NewRelicDeploymentMarkerState spyRelicDeploymentMarkerState = spy(this.newRelicDeploymentMarkerState);
    spyRelicDeploymentMarkerState.setAnalysisServerConfigId(settingId);
    doReturn(asList(TemplateExpression.builder()
                        .fieldName("analysisServerConfigId")
                        .expression("${NewRelic_Server}")
                        .metadata(ImmutableMap.of("entityType", "NEWRELIC_CONFIGID"))
                        .build(),
                 TemplateExpression.builder()
                     .fieldName("applicationId")
                     .expression("${NewRelic_App}")
                     .metadata(ImmutableMap.of("entityType", "NEWRELIC_APPID"))
                     .build()))
        .when(spyRelicDeploymentMarkerState)
        .getTemplateExpressions();

    try {
      spyRelicDeploymentMarkerState.execute(executionContext);
      fail("Passed with wrong app name");
    } catch (WingsException e) {
      assertThat(e.getMessage()).isEqualTo("Can not find application by name");
    }

    doReturn(NewRelicApplication.builder().id(30444).build())
        .when(newRelicService)
        .resolveApplicationName(anyString(), anyString());
    ExecutionResponse executionResponse = spyRelicDeploymentMarkerState.execute(executionContext);
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.RUNNING);

    doThrow(new RuntimeException("Can not find application by id"))
        .when(newRelicService)
        .resolveApplicationId(anyString(), anyString());

    try {
      spyRelicDeploymentMarkerState.execute(executionContext);
      fail("Passed with wrong app id");
    } catch (RuntimeException e) {
      assertThat(e.getMessage()).isEqualTo("Can not find application by id");
    }
  }
}
