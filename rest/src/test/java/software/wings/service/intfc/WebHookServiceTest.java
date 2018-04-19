package software.wings.service.intfc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder.aWorkflowExecution;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.PORTAL_URL;
import static software.wings.utils.WingsTestConstants.WORKFLOW_EXECUTION_ID;

import com.google.inject.Inject;

import org.junit.Test;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.app.MainConfiguration;
import software.wings.beans.Application;
import software.wings.beans.WebHookRequest;
import software.wings.beans.WebHookResponse;
import software.wings.beans.WorkflowExecution;
import software.wings.sm.ExecutionStatus;
import software.wings.utils.CryptoUtil;

public class WebHookServiceTest extends WingsBaseTest {
  @Mock private ArtifactStreamService artifactStreamService;
  @Mock private ArtifactService artifactService;
  @Mock private TriggerService triggerService;
  @Mock private AppService appService;
  @Mock private WorkflowExecutionService workflowExecutionService;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS) private MainConfiguration configuration;
  @Inject @InjectMocks private WebHookService webHookService;

  @Test
  public void testPopulateUrlFieldsWhenTriggering() {
    final Application application =
        anApplication().withUuid(APP_ID).withAppId(APP_ID).withAccountId(ACCOUNT_ID).build();
    doReturn(application).when(appService).get(APP_ID);
    final WorkflowExecution execution = aWorkflowExecution()
                                            .withAppId(APP_ID)
                                            .withEnvId(ENV_ID)
                                            .withUuid(WORKFLOW_EXECUTION_ID)
                                            .withStatus(ExecutionStatus.RUNNING)
                                            .build();
    final String token = CryptoUtil.secureRandAlphaNumString(40);
    doReturn(execution).when(triggerService).triggerExecutionByWebHook(eq(APP_ID), eq(token), anyMap(), anyMap());
    when(configuration.getPortal().getUrl()).thenReturn(PORTAL_URL);
    WebHookRequest request = WebHookRequest.builder().application(APP_ID).build();
    WebHookResponse response = webHookService.execute(token, request);
    assertThat(response).isNotNull();
    assertThat(response.getRequestId()).isEqualTo(WORKFLOW_EXECUTION_ID);
    assertThat(response.getStatus()).isEqualTo(ExecutionStatus.RUNNING.name());
    assertThat(response.getApiUrl())
        .isEqualTo(String.format("%s/api/external/v1/executions/%s/status?accountId=%s&appId=%s", PORTAL_URL,
            WORKFLOW_EXECUTION_ID, ACCOUNT_ID, APP_ID));
    assertThat(response.getUiUrl())
        .isEqualTo(String.format("%s/#/account/%s/app/%s/env/%s/executions/%s/details", PORTAL_URL, ACCOUNT_ID, APP_ID,
            ENV_ID, WORKFLOW_EXECUTION_ID));
  }
}
