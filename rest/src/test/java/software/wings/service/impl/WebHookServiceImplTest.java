package software.wings.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder.aWorkflowExecution;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.PORTAL_URL;
import static software.wings.utils.WingsTestConstants.WORKFLOW_EXECUTION_ID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.app.MainConfiguration;
import software.wings.beans.Application;
import software.wings.beans.WebHookRequest;
import software.wings.beans.WebHookResponse;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecutionStatusResponse;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.ExecutionStatus;
import software.wings.utils.CryptoUtil;

public class WebHookServiceImplTest {
  @Mock private ArtifactStreamService artifactStreamService;
  @Mock private ArtifactService artifactService;
  @Mock private TriggerService triggerService;
  @Mock private AppService appService;
  @Mock private WorkflowExecutionService workflowExecutionService;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS) private MainConfiguration configuration;
  @InjectMocks private WebHookServiceImpl webHookService = spy(WebHookServiceImpl.class);

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testGetWorkflowExecutionStatus() {
    final WorkflowExecution execution = aWorkflowExecution()
                                            .withAppId(APP_ID)
                                            .withUuid(WORKFLOW_EXECUTION_ID)
                                            .withStatus(ExecutionStatus.RUNNING)
                                            .build();
    final String statusToken = CryptoUtil.secureRandAlphaNumString(40);
    doReturn(execution).when(workflowExecutionService).getWorkflowExecution(eq(APP_ID), eq(WORKFLOW_EXECUTION_ID));
    final WorkflowExecutionStatusResponse response =
        webHookService.getWorkflowExecutionStatus(statusToken, APP_ID, WORKFLOW_EXECUTION_ID);
    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(ExecutionStatus.RUNNING.name());
  }

  @Test
  public void testPopulateNewFieldsWhenTriggering() {
    final Application application =
        anApplication().withUuid(APP_ID).withAppId(APP_ID).withAccountId(ACCOUNT_ID).build();
    doReturn(application).when(appService).get(APP_ID);
    final WorkflowExecution execution = aWorkflowExecution()
                                            .withAppId(APP_ID)
                                            .withEnvId(ENV_ID)
                                            .withUuid(WORKFLOW_EXECUTION_ID)
                                            .withStatus(ExecutionStatus.RUNNING)
                                            .build();
    final String statusToken = CryptoUtil.secureRandAlphaNumString(40);
    doReturn(execution).when(triggerService).triggerExecutionByWebHook(eq(APP_ID), eq(statusToken), anyMap(), anyMap());
    when(configuration.getPortal().getUrl()).thenReturn(PORTAL_URL);
    WebHookRequest request = WebHookRequest.builder().application(APP_ID).build();
    WebHookResponse response = webHookService.execute(statusToken, request);
    assertThat(response).isNotNull();
    assertThat(response.getRequestId()).isEqualTo(WORKFLOW_EXECUTION_ID);
    assertThat(response.getStatus()).isEqualTo(ExecutionStatus.RUNNING.name());
    assertThat(response.getRestUrl())
        .endsWith(String.format("/status?appId=%s&workflowExecutionId=%s", APP_ID, WORKFLOW_EXECUTION_ID));
    assertThat(response.getRestUrl()).startsWith(String.format("%s/api/webhooks/", PORTAL_URL));
    assertThat(response.getUiUrl())
        .isEqualTo(String.format("%s/#/account/%s/app/%s/env/%s/executions/%s/details", PORTAL_URL, ACCOUNT_ID, APP_ID,
            ENV_ID, WORKFLOW_EXECUTION_ID));
  }
}
