package software.wings.service.intfc;

import static com.google.common.collect.ImmutableMap.of;
import static io.harness.beans.ExecutionStatus.RUNNING;
import static io.harness.rule.OwnerRule.SRINIVAS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.service.impl.WebHookServiceImpl.X_BIT_BUCKET_EVENT;
import static software.wings.service.impl.WebHookServiceImpl.X_GIT_HUB_EVENT;
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

import com.fasterxml.jackson.core.type.TypeReference;
import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import io.harness.serializer.JsonUtils;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
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
import software.wings.beans.trigger.PrAction;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.trigger.WebHookTriggerCondition;
import software.wings.beans.trigger.WebhookEventType;
import software.wings.beans.trigger.WebhookSource;
import software.wings.beans.trigger.WebhookSource.BitBucketEventType;
import software.wings.dl.WingsPersistence;
import software.wings.expression.ManagerExpressionEvaluator;
import software.wings.utils.CryptoUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.HttpHeaders;

public class WebHookServiceTest extends WingsBaseTest {
  @Mock private TriggerService triggerService;
  @Mock private AppService appService;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS) private MainConfiguration configuration;
  @Mock HttpHeaders httpHeaders;
  @Mock private FeatureFlagService featureFlagService;
  @Inject ManagerExpressionEvaluator expressionEvaluator;

  @Inject @InjectMocks private WebHookService webHookService;
  @Inject WingsPersistence wingsPersistence;

  final String token = CryptoUtils.secureRandAlphaNumString(40);

  WorkflowExecution execution =
      WorkflowExecution.builder().appId(APP_ID).envId(ENV_ID).uuid(WORKFLOW_EXECUTION_ID).status(RUNNING).build();

  Trigger trigger = Trigger.builder()
                        .workflowId(PIPELINE_ID)
                        .uuid(TRIGGER_ID)
                        .appId(APP_ID)
                        .name(TRIGGER_NAME)
                        .webHookToken(token)
                        .condition(WebHookTriggerCondition.builder()
                                       .webhookSource(WebhookSource.BITBUCKET)
                                       .parameters(of("PullRequestId", "${pullrequest.id}"))
                                       .webHookToken(WebHookToken.builder().webHookToken(token).build())
                                       .build())
                        .build();

  WebHookTriggerCondition webHookTriggerCondition = (WebHookTriggerCondition) trigger.getCondition();

  @Before
  public void setUp() {
    final Application application = anApplication().uuid(APP_ID).appId(APP_ID).accountId(ACCOUNT_ID).build();
    doReturn(application).when(appService).get(APP_ID);
    doReturn(execution)
        .when(triggerService)
        .triggerExecutionByWebHook(anyString(), anyString(), anyMap(), anyMap(), any());
    when(configuration.getPortal().getUrl()).thenReturn(PORTAL_URL);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldExecuteNoService() {
    List<Map<String, String>> artifacts =
        Collections.singletonList(of("service", SERVICE_NAME, "buildNumber", BUILD_NO));
    WebHookRequest request = WebHookRequest.builder().artifacts(artifacts).application(APP_ID).build();
    WebHookResponse response = (WebHookResponse) webHookService.execute(token, request).getEntity();
    assertThat(response).isNotNull();
    assertThat(response.getError()).isNotEmpty();
  }

  @Test
  @Category(UnitTests.class)
  public void shouldExecuteWithService() {
    wingsPersistence.save(Service.builder().name(SERVICE_NAME).appId(APP_ID).build());
    List<Map<String, String>> artifacts =
        Collections.singletonList(of("service", SERVICE_NAME, "buildNumber", BUILD_NO));
    WebHookRequest request = WebHookRequest.builder().artifacts(artifacts).application(APP_ID).build();
    WebHookResponse response = (WebHookResponse) webHookService.execute(token, request).getEntity();
    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(RUNNING.name());
  }

  @Test
  @Category(UnitTests.class)
  public void shouldExecuteByEventNoTrigger() {
    String payLoad = "Some payload";
    WebHookResponse response = (WebHookResponse) webHookService.executeByEvent(token, payLoad, null).getEntity();
    assertThat(response).isNotNull();
    assertThat(response.getError()).isNotEmpty();
  }

  @Test
  @Owner(emails = SRINIVAS)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldExecuteByEventTriggerInvalidJson() {
    when(triggerService.getTriggerByWebhookToken(token)).thenReturn(trigger);
    String payLoad = "Some payload";
    WebHookResponse response = (WebHookResponse) webHookService.executeByEvent(token, payLoad, null).getEntity();
    assertThat(response).isNotNull();
    assertThat(response.getError()).isNotEmpty();
  }

  @Test
  @Category(UnitTests.class)
  public void shouldExecuteByEventTriggerBitBucket() throws IOException {
    webHookTriggerCondition.setEventTypes(Arrays.asList(WebhookEventType.PULL_REQUEST));
    webHookTriggerCondition.setBitBucketEvents(Arrays.asList(BitBucketEventType.PULL_REQUEST_APPROVED));
    when(triggerService.getTriggerByWebhookToken(token)).thenReturn(trigger);

    ClassLoader classLoader = getClass().getClassLoader();
    File file =
        new File(classLoader.getResource("software/wings/service/impl/webhook/bitbucket_pull_request.json").getFile());
    String payLoad = FileUtils.readFileToString(file, Charset.defaultCharset());

    doReturn("pullrequest:approved").when(httpHeaders).getHeaderString(X_BIT_BUCKET_EVENT);
    doReturn(execution).when(triggerService).triggerExecutionByWebHook(any(), anyMap(), any());
    WebHookResponse response = (WebHookResponse) webHookService.executeByEvent(token, payLoad, httpHeaders).getEntity();
    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(RUNNING.name());
  }

  @Test
  @Category(UnitTests.class)
  public void shouldTestJsonParsing() throws IOException {
    when(triggerService.getTriggerByWebhookToken(token)).thenReturn(trigger);

    ClassLoader classLoader = getClass().getClassLoader();
    File file =
        new File(classLoader.getResource("software/wings/service/impl/webhook/bitbucket_pull_request.json").getFile());
    String payLoad = FileUtils.readFileToString(file, Charset.defaultCharset());

    // convert JSON string to Map
    Map<String, Object> map = JsonUtils.asObject(payLoad, new TypeReference<Map<String, Object>>() {});

    final String value = expressionEvaluator.substitute("${pullrequest.id} - MyVal", map);
    assertThat(value).isNotEmpty().isEqualTo("23 - MyVal");
    assertThat(expressionEvaluator.substitute("${pullrequest.id} - MyVal - ${pullrequest.id}", map))
        .isNotEmpty()
        .isEqualTo("23 - MyVal - 23");
    assertThat(expressionEvaluator.substitute("${pullrequest.id} - MyVal - ${app.name}", map))
        .isNotEmpty()
        .isEqualTo("23 - MyVal - ${app.name}");
  }
  @Test
  @Category(UnitTests.class)
  public void shouldTestJsonGitHubPushParsing() throws IOException {
    when(triggerService.getTriggerByWebhookToken(token)).thenReturn(trigger);

    ClassLoader classLoader = getClass().getClassLoader();
    File file =
        new File(classLoader.getResource("software/wings/service/impl/webhook/github_push_request.json").getFile());
    String payLoad = FileUtils.readFileToString(file, Charset.defaultCharset());

    // convert JSON string to Map
    Map<String, Object> map = JsonUtils.asObject(payLoad, new TypeReference<Map<String, Object>>() {
    }); // mapper.readValue(payLoad, new TypeReference<Map<String, Object>>(){});
    final String value = expressionEvaluator.substitute("${commits[0].id}", map);
    assertThat(value).isNotEmpty().isEqualTo("4ebc6e9e489979a29ca17b8da0c29d9f6803a5b9");
    final String ref = expressionEvaluator.substitute("${ref.substring(${ref.indexOf('/')+1})}", map);
    assertThat(ref).isNotEmpty().isEqualTo("heads/master");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldTestJsonBitBucketPushParsing() throws IOException {
    when(triggerService.getTriggerByWebhookToken(token)).thenReturn(trigger);

    ClassLoader classLoader = getClass().getClassLoader();
    File file =
        new File(classLoader.getResource("software/wings/service/impl/webhook/bitbucket_push_request.json").getFile());
    String payLoad = FileUtils.readFileToString(file, Charset.defaultCharset());

    // convert JSON string to Map
    Map<String, Object> map = JsonUtils.asObject(payLoad, new TypeReference<Map<String, Object>>() {
    }); // mapper.readValue(payLoad, new TypeReference<Map<String, Object>>(){});
    final String value = expressionEvaluator.substitute("${push.changes[0].'new'.name}", map);
    assertThat(value).isNotEmpty().isEqualTo("master");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldTestJsonBitBucketPull() throws IOException {
    webHookTriggerCondition.setEventTypes(Arrays.asList(WebhookEventType.PULL_REQUEST));
    webHookTriggerCondition.setBitBucketEvents(Arrays.asList(BitBucketEventType.PULL_REQUEST_APPROVED));

    when(triggerService.getTriggerByWebhookToken(token)).thenReturn(trigger);

    ClassLoader classLoader = getClass().getClassLoader();
    File file =
        new File(classLoader.getResource("software/wings/service/impl/webhook/bitbucket_pull_request.json").getFile());
    String payLoad = FileUtils.readFileToString(file, Charset.defaultCharset());

    doReturn("pullrequest:approved").when(httpHeaders).getHeaderString(X_BIT_BUCKET_EVENT);
    doReturn(execution).when(triggerService).triggerExecutionByWebHook(any(), anyMap(), any());

    WebHookResponse response = (WebHookResponse) webHookService.executeByEvent(token, payLoad, httpHeaders).getEntity();
    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(RUNNING.name());
  }

  @Test
  @Category(UnitTests.class)
  public void shouldTestJsonBitBucketFork() throws IOException {
    webHookTriggerCondition.setEventTypes(Arrays.asList(WebhookEventType.REPO));
    webHookTriggerCondition.setBitBucketEvents(Arrays.asList(BitBucketEventType.FORK));
    when(triggerService.getTriggerByWebhookToken(token)).thenReturn(trigger);

    ClassLoader classLoader = getClass().getClassLoader();
    File file =
        new File(classLoader.getResource("software/wings/service/impl/webhook/bitbucket_fork_request.json").getFile());
    String payLoad = FileUtils.readFileToString(file, Charset.defaultCharset());

    doReturn("repo:fork").when(httpHeaders).getHeaderString(X_BIT_BUCKET_EVENT);
    doReturn(execution).when(triggerService).triggerExecutionByWebHook(any(), anyMap(), any());

    WebHookResponse response = (WebHookResponse) webHookService.executeByEvent(token, payLoad, httpHeaders).getEntity();
    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(RUNNING.name());
  }

  @Test
  @Category(UnitTests.class)
  public void shouldTestJsonBitBucketForkWithPullRequest() throws IOException {
    webHookTriggerCondition.setEventTypes(Arrays.asList(WebhookEventType.PULL_REQUEST));
    webHookTriggerCondition.setBitBucketEvents(Arrays.asList(BitBucketEventType.ALL));

    when(triggerService.getTriggerByWebhookToken(token)).thenReturn(trigger);

    ClassLoader classLoader = getClass().getClassLoader();
    File file =
        new File(classLoader.getResource("software/wings/service/impl/webhook/bitbucket_fork_request.json").getFile());
    String payLoad = FileUtils.readFileToString(file, Charset.defaultCharset());

    doReturn("repo:fork").when(httpHeaders).getHeaderString(X_BIT_BUCKET_EVENT);
    doReturn(execution).when(triggerService).triggerExecutionByWebHook(any(), anyMap(), any());

    WebHookResponse response = (WebHookResponse) webHookService.executeByEvent(token, payLoad, httpHeaders).getEntity();
    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(RUNNING.name());
  }

  @Test
  @Category(UnitTests.class)
  public void shouldThrowErrorJsonBitBucketFork() throws IOException {
    webHookTriggerCondition.setEventTypes(Arrays.asList(WebhookEventType.REPO));
    webHookTriggerCondition.setBitBucketEvents(Arrays.asList(BitBucketEventType.COMMIT_COMMENT_CREATED));
    when(triggerService.getTriggerByWebhookToken(token)).thenReturn(trigger);

    ClassLoader classLoader = getClass().getClassLoader();
    File file =
        new File(classLoader.getResource("software/wings/service/impl/webhook/bitbucket_fork_request.json").getFile());
    String payLoad = FileUtils.readFileToString(file, Charset.defaultCharset());

    doReturn("repo:fork").when(httpHeaders).getHeaderString(X_BIT_BUCKET_EVENT);
    doReturn(execution).when(triggerService).triggerExecutionByWebHook(any(), anyMap(), any());

    WebHookResponse response = (WebHookResponse) webHookService.executeByEvent(token, payLoad, httpHeaders).getEntity();
    assertThat(response).isNotNull();
    assertThat(response.getError()).isNotEmpty();
  }

  @Test
  @Category(UnitTests.class)
  public void shouldTriggerGitHubPRWithoutActions() throws IOException {
    Trigger webhookTrigger = Trigger.builder()
                                 .workflowId(PIPELINE_ID)
                                 .uuid(TRIGGER_ID)
                                 .appId(APP_ID)
                                 .name(TRIGGER_NAME)
                                 .webHookToken(token)
                                 .condition(WebHookTriggerCondition.builder()
                                                .webhookSource(WebhookSource.GITHUB)
                                                .webHookToken(WebHookToken.builder().webHookToken(token).build())
                                                .build())
                                 .build();
    when(triggerService.getTriggerByWebhookToken(token)).thenReturn(webhookTrigger);

    ClassLoader classLoader = getClass().getClassLoader();
    File file =
        new File(classLoader.getResource("software/wings/service/impl/webhook/github_pull_request.json").getFile());
    String payLoad = FileUtils.readFileToString(file, Charset.defaultCharset());

    doReturn("pull_request").when(httpHeaders).getHeaderString(X_GIT_HUB_EVENT);
    doReturn(execution).when(triggerService).triggerExecutionByWebHook(any(), anyMap(), any());

    WebHookResponse response = (WebHookResponse) webHookService.executeByEvent(token, payLoad, httpHeaders).getEntity();
    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(RUNNING.name());
  }

  @Test
  @Category(UnitTests.class)
  public void shouldTriggerGitHubPRWithClosedAction() throws IOException {
    Trigger webhookTrigger = Trigger.builder()
                                 .workflowId(PIPELINE_ID)
                                 .uuid(TRIGGER_ID)
                                 .appId(APP_ID)
                                 .name(TRIGGER_NAME)
                                 .webHookToken(token)
                                 .condition(WebHookTriggerCondition.builder()
                                                .webhookSource(WebhookSource.GITHUB)
                                                .eventTypes(Collections.singletonList(WebhookEventType.PULL_REQUEST))
                                                .actions(Collections.singletonList(PrAction.CLOSED))
                                                .webHookToken(WebHookToken.builder().webHookToken(token).build())
                                                .build())
                                 .build();
    when(triggerService.getTriggerByWebhookToken(token)).thenReturn(webhookTrigger);

    ClassLoader classLoader = getClass().getClassLoader();
    File file =
        new File(classLoader.getResource("software/wings/service/impl/webhook/github_pull_request.json").getFile());
    String payLoad = FileUtils.readFileToString(file, Charset.defaultCharset());

    doReturn("pull_request").when(httpHeaders).getHeaderString(X_GIT_HUB_EVENT);
    doReturn(execution).when(triggerService).triggerExecutionByWebHook(any(), anyMap(), any());

    WebHookResponse response = (WebHookResponse) webHookService.executeByEvent(token, payLoad, httpHeaders).getEntity();
    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(RUNNING.name());
  }

  @Test
  @Owner(emails = SRINIVAS, intermittent = true)
  @Category(UnitTests.class)
  public void shouldTriggerGitHubPRWithDifferentEvent() throws IOException {
    Trigger webhookTrigger = Trigger.builder()
                                 .workflowId(PIPELINE_ID)
                                 .uuid(TRIGGER_ID)
                                 .appId(APP_ID)
                                 .name(TRIGGER_NAME)
                                 .webHookToken(token)
                                 .condition(WebHookTriggerCondition.builder()
                                                .webhookSource(WebhookSource.GITHUB)
                                                .eventTypes(Collections.singletonList(WebhookEventType.PUSH))
                                                .actions(Collections.singletonList(PrAction.OPENED))
                                                .webHookToken(WebHookToken.builder().webHookToken(token).build())
                                                .build())
                                 .build();
    when(triggerService.getTriggerByWebhookToken(token)).thenReturn(webhookTrigger);

    ClassLoader classLoader = getClass().getClassLoader();
    File file =
        new File(classLoader.getResource("software/wings/service/impl/webhook/github_pull_request.json").getFile());
    String payLoad = FileUtils.readFileToString(file, Charset.defaultCharset());

    doReturn("pull_request").when(httpHeaders).getHeaderString(X_GIT_HUB_EVENT);

    WebHookResponse response = (WebHookResponse) webHookService.executeByEvent(token, payLoad, httpHeaders).getEntity();
    assertThat(response).isNotNull();
    assertThat(response.getError()).isNotEmpty();
  }

  @Test
  @Owner(emails = SRINIVAS, intermittent = true)
  @Category(UnitTests.class)
  public void shouldTriggerGitHubPRWithDifferentAction() throws IOException {
    Trigger webhookTrigger = Trigger.builder()
                                 .workflowId(PIPELINE_ID)
                                 .uuid(TRIGGER_ID)
                                 .appId(APP_ID)
                                 .name(TRIGGER_NAME)
                                 .webHookToken(token)
                                 .condition(WebHookTriggerCondition.builder()
                                                .webhookSource(WebhookSource.GITHUB)
                                                .eventTypes(Collections.singletonList(WebhookEventType.PULL_REQUEST))
                                                .actions(Collections.singletonList(PrAction.OPENED))
                                                .webHookToken(WebHookToken.builder().webHookToken(token).build())
                                                .build())
                                 .build();
    when(triggerService.getTriggerByWebhookToken(token)).thenReturn(webhookTrigger);

    ClassLoader classLoader = getClass().getClassLoader();
    File file =
        new File(classLoader.getResource("software/wings/service/impl/webhook/github_pull_request.json").getFile());
    String payLoad = FileUtils.readFileToString(file, Charset.defaultCharset());

    doReturn("pull_request").when(httpHeaders).getHeaderString(X_GIT_HUB_EVENT);
    doReturn(execution).when(triggerService).triggerExecutionByWebHook(any(), anyMap(), any());

    WebHookResponse response = (WebHookResponse) webHookService.executeByEvent(token, payLoad, httpHeaders).getEntity();
    assertThat(response).isNotNull();
    assertThat(response.getError()).isNotEmpty();
  }

  @Test
  @Owner(emails = SRINIVAS, intermittent = true)
  @Category(UnitTests.class)
  public void shouldTriggerGitHubPushRequest() throws IOException {
    Trigger webhookTrigger = Trigger.builder()
                                 .workflowId(PIPELINE_ID)
                                 .uuid(TRIGGER_ID)
                                 .appId(APP_ID)
                                 .name(TRIGGER_NAME)
                                 .webHookToken(token)
                                 .condition(WebHookTriggerCondition.builder()
                                                .webhookSource(WebhookSource.GITHUB)
                                                .eventTypes(Collections.singletonList(WebhookEventType.PUSH))
                                                .webHookToken(WebHookToken.builder().webHookToken(token).build())
                                                .build())
                                 .build();
    when(triggerService.getTriggerByWebhookToken(token)).thenReturn(webhookTrigger);

    ClassLoader classLoader = getClass().getClassLoader();
    File file =
        new File(classLoader.getResource("software/wings/service/impl/webhook/github_push_request.json").getFile());
    String payLoad = FileUtils.readFileToString(file, Charset.defaultCharset());

    doReturn("push").when(httpHeaders).getHeaderString(X_GIT_HUB_EVENT);
    doReturn(execution).when(triggerService).triggerExecutionByWebHook(any(), anyMap(), any());

    WebHookResponse response = (WebHookResponse) webHookService.executeByEvent(token, payLoad, httpHeaders).getEntity();
    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(RUNNING.name());
  }

  @Test
  @Category(UnitTests.class)
  public void testPopulateUrlFieldsWhenTriggering() {
    final Application application = anApplication().uuid(APP_ID).appId(APP_ID).accountId(ACCOUNT_ID).build();
    doReturn(application).when(appService).get(APP_ID);
    final WorkflowExecution execution =
        WorkflowExecution.builder().appId(APP_ID).envId(ENV_ID).uuid(WORKFLOW_EXECUTION_ID).status(RUNNING).build();
    final String token = CryptoUtils.secureRandAlphaNumString(40);
    doReturn(execution)
        .when(triggerService)
        .triggerExecutionByWebHook(eq(APP_ID), eq(token), anyMap(), anyMap(), any());

    WebHookRequest request = WebHookRequest.builder().application(APP_ID).build();
    WebHookResponse response = (WebHookResponse) webHookService.execute(token, request).getEntity();
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
