package software.wings.service.intfc;

import static com.google.common.collect.ImmutableMap.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder.aWorkflowExecution;
import static software.wings.sm.ExecutionStatus.RUNNING;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.BUILD_NO;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.PIPELINE_ID;
import static software.wings.utils.WingsTestConstants.PORTAL_URL;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.TRIGGER_ID;
import static software.wings.utils.WingsTestConstants.TRIGGER_NAME;
import static software.wings.utils.WingsTestConstants.WORKFLOW_EXECUTION_ID;

import com.google.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.app.MainConfiguration;
import software.wings.beans.Application;
import software.wings.beans.Service;
import software.wings.beans.WebHookRequest;
import software.wings.beans.WebHookResponse;
import software.wings.beans.WebHookToken;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.trigger.WebHookTriggerCondition;
import software.wings.dl.WingsPersistence;
import software.wings.utils.CryptoUtil;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class WebHookServiceTest extends WingsBaseTest {
  @Mock private TriggerService triggerService;
  @Mock private AppService appService;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS) private MainConfiguration configuration;
  @Inject @InjectMocks private WebHookService webHookService;
  final String token = CryptoUtil.secureRandAlphaNumString(40);
  @Inject WingsPersistence wingsPersistence;

  WorkflowExecution execution = aWorkflowExecution()
                                    .withAppId(APP_ID)
                                    .withEnvId(ENV_ID)
                                    .withUuid(WORKFLOW_EXECUTION_ID)
                                    .withStatus(RUNNING)
                                    .build();

  Trigger trigger = Trigger.builder()
                        .workflowId(PIPELINE_ID)
                        .uuid(TRIGGER_ID)
                        .appId(APP_ID)
                        .name(TRIGGER_NAME)
                        .webHookToken(token)
                        .condition(WebHookTriggerCondition.builder()
                                       .parameters(of("PullReuestId", "${pullrequest.id}"))
                                       .webHookToken(WebHookToken.builder().webHookToken(token).build())
                                       .build())
                        .build();

  @Before
  public void setUp() {
    final Application application =
        anApplication().withUuid(APP_ID).withAppId(APP_ID).withAccountId(ACCOUNT_ID).build();
    doReturn(application).when(appService).get(APP_ID);
    doReturn(execution).when(triggerService).triggerExecutionByWebHook(eq(APP_ID), eq(token), anyMap(), anyMap());
    when(configuration.getPortal().getUrl()).thenReturn(PORTAL_URL);
  }

  @Test
  public void shouldExecuteNoService() {
    List<Map<String, String>> artifacts =
        Collections.singletonList(of("service", SERVICE_NAME, "buildNumber", BUILD_NO));
    WebHookRequest request = WebHookRequest.builder().artifacts(artifacts).application(APP_ID).build();
    WebHookResponse response = webHookService.execute(token, request);
    assertThat(response).isNotNull();
    assertThat(response.getError()).isNotEmpty();
  }

  @Test
  public void shouldExecuteWithService() {
    wingsPersistence.save(Service.builder().name(SERVICE_NAME).appId(APP_ID).build());
    List<Map<String, String>> artifacts =
        Collections.singletonList(of("service", SERVICE_NAME, "buildNumber", BUILD_NO));
    WebHookRequest request = WebHookRequest.builder().artifacts(artifacts).application(APP_ID).build();
    WebHookResponse response = webHookService.execute(token, request);
    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(RUNNING.name());
  }

  @Test
  public void shouldExecuteByEventNoTrigger() {
    String payLoad = "Some payload";
    WebHookResponse response = webHookService.executeByEvent(token, payLoad);
    assertThat(response).isNotNull();
    assertThat(response.getError()).isNotEmpty();
  }

  @Test
  public void shouldExecuteByEventTriggerInvalidJson() {
    when(triggerService.getTriggerByWebhookToken(token)).thenReturn(trigger);
    String payLoad = "Some payload";
    WebHookResponse response = webHookService.executeByEvent(token, payLoad);
    assertThat(response).isNotNull();
    assertThat(response.getError()).isNotEmpty();
  }

  @Test
  public void shouldExecuteByEventTriggerBitBucket() throws IOException {
    when(triggerService.getTriggerByWebhookToken(token)).thenReturn(trigger);

    ClassLoader classLoader = getClass().getClassLoader();
    File file =
        new File(classLoader.getResource("software/wings/service/impl/webhook/bitbucket_pull_request.json").getFile());
    String payLoad = FileUtils.readFileToString(file, Charset.defaultCharset());

    doReturn(execution).when(triggerService).triggerExecutionByWebHook(any(Trigger.class), anyMap());
    WebHookResponse response = webHookService.executeByEvent(token, payLoad);
    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(RUNNING.name());
  }

  @Test
  public void testPopulateUrlFieldsWhenTriggering() {
    final Application application =
        anApplication().withUuid(APP_ID).withAppId(APP_ID).withAccountId(ACCOUNT_ID).build();
    doReturn(application).when(appService).get(APP_ID);
    final WorkflowExecution execution = aWorkflowExecution()
                                            .withAppId(APP_ID)
                                            .withEnvId(ENV_ID)
                                            .withUuid(WORKFLOW_EXECUTION_ID)
                                            .withStatus(RUNNING)
                                            .build();
    final String token = CryptoUtil.secureRandAlphaNumString(40);
    doReturn(execution).when(triggerService).triggerExecutionByWebHook(eq(APP_ID), eq(token), anyMap(), anyMap());

    WebHookRequest request = WebHookRequest.builder().application(APP_ID).build();
    WebHookResponse response = webHookService.execute(token, request);
    assertThat(response).isNotNull();
    assertThat(response.getRequestId()).isEqualTo(WORKFLOW_EXECUTION_ID);
    assertThat(response.getStatus()).isEqualTo(RUNNING.name());
    assertThat(response.getApiUrl())
        .isEqualTo(String.format("%s/api/external/v1/executions/%s/status?accountId=%s&appId=%s", PORTAL_URL,
            WORKFLOW_EXECUTION_ID, ACCOUNT_ID, APP_ID));
    assertThat(response.getUiUrl())
        .isEqualTo(String.format("%s/#/account/%s/app/%s/env/%s/executions/%s/details", PORTAL_URL, ACCOUNT_ID, APP_ID,
            ENV_ID, WORKFLOW_EXECUTION_ID));
  }
}
