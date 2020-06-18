package software.wings.graphql.datafetcher.trigger;

import static io.harness.rule.OwnerRule.MILAN;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import software.wings.app.MainConfiguration;
import software.wings.app.PortalConfig;
import software.wings.beans.WebHookToken;
import software.wings.beans.trigger.ArtifactTriggerCondition;
import software.wings.beans.trigger.GithubAction;
import software.wings.beans.trigger.PipelineTriggerCondition;
import software.wings.beans.trigger.ReleaseAction;
import software.wings.beans.trigger.ScheduledTriggerCondition;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.trigger.TriggerConditionType;
import software.wings.beans.trigger.WebHookTriggerCondition;
import software.wings.beans.trigger.WebhookEventType;
import software.wings.beans.trigger.WebhookSource;
import software.wings.graphql.schema.type.trigger.QLConditionType;
import software.wings.graphql.schema.type.trigger.QLCreateTriggerInput;
import software.wings.graphql.schema.type.trigger.QLOnNewArtifact;
import software.wings.graphql.schema.type.trigger.QLOnPipelineCompletion;
import software.wings.graphql.schema.type.trigger.QLOnSchedule;
import software.wings.graphql.schema.type.trigger.QLOnWebhook;
import software.wings.graphql.schema.type.trigger.QLTriggerConditionInput;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.PipelineService;

import java.util.Arrays;
import java.util.Collections;

@PrepareForTest(TriggerConditionController.class)
@RunWith(PowerMockRunner.class)
public class TriggerConditionControllerTest extends CategoryTest {
  @Test
  @Owner(developers = MILAN)
  @Category(UnitTests.class)
  public void populateTriggerConditionShouldReturnArtifactTriggerCondition() {
    ArtifactTriggerCondition artifactTriggerCondition = ArtifactTriggerCondition.builder()
                                                            .artifactStreamId("sourceId")
                                                            .artifactSourceName("sourceName")
                                                            .artifactFilter("artifactFilter")
                                                            .regex(true)
                                                            .build();
    artifactTriggerCondition.setConditionType(TriggerConditionType.NEW_ARTIFACT);
    Trigger trigger = Trigger.builder().condition(artifactTriggerCondition).build();

    QLOnNewArtifact qlOnNewArtifact =
        (QLOnNewArtifact) TriggerConditionController.populateTriggerCondition(trigger, null, null);

    assertThat(qlOnNewArtifact).isNotNull();
    assertThat(qlOnNewArtifact.getArtifactFilter()).isEqualTo(artifactTriggerCondition.getArtifactFilter());
    assertThat(qlOnNewArtifact.getArtifactSourceId()).isEqualTo(artifactTriggerCondition.getArtifactStreamId());
    assertThat(qlOnNewArtifact.getArtifactSourceName()).isEqualTo(artifactTriggerCondition.getArtifactSourceName());
    assertThat(qlOnNewArtifact.getRegex()).isEqualTo(artifactTriggerCondition.isRegex());
    assertThat(qlOnNewArtifact.getTriggerConditionType().name())
        .isEqualTo(artifactTriggerCondition.getConditionType().name());
  }

  @Test
  @Owner(developers = MILAN)
  @Category(UnitTests.class)
  public void populateTriggerConditionShouldReturnPipelineTriggerCondition() {
    PipelineTriggerCondition pipelineTrigerCondition =
        PipelineTriggerCondition.builder().pipelineId("pipelineId").pipelineName("pipelineName").build();
    pipelineTrigerCondition.setConditionType(TriggerConditionType.PIPELINE_COMPLETION);
    Trigger trigger = Trigger.builder().condition(pipelineTrigerCondition).build();

    QLOnPipelineCompletion qlOnPipelineCompletion =
        (QLOnPipelineCompletion) TriggerConditionController.populateTriggerCondition(trigger, null, null);

    assertThat(qlOnPipelineCompletion).isNotNull();
    assertThat(qlOnPipelineCompletion.getPipelineId()).isEqualTo(pipelineTrigerCondition.getPipelineId());
    assertThat(qlOnPipelineCompletion.getPipelineName()).isEqualTo(pipelineTrigerCondition.getPipelineName());
    assertThat(qlOnPipelineCompletion.getTriggerConditionType().name())
        .isEqualTo(pipelineTrigerCondition.getConditionType().name());
  }

  @Test
  @Owner(developers = MILAN)
  @Category(UnitTests.class)
  public void populateTriggerConditionShouldReturnScheduledTriggerCondition() {
    ScheduledTriggerCondition scheduledTriggerCondition = ScheduledTriggerCondition.builder()
                                                              .cronExpression("cronExpression")
                                                              .cronDescription("cronDescription")
                                                              .onNewArtifactOnly(true)
                                                              .build();
    scheduledTriggerCondition.setConditionType(TriggerConditionType.SCHEDULED);
    Trigger trigger = Trigger.builder().condition(scheduledTriggerCondition).build();

    QLOnSchedule qlOnSchedule = (QLOnSchedule) TriggerConditionController.populateTriggerCondition(trigger, null, null);

    assertThat(qlOnSchedule).isNotNull();
    assertThat(qlOnSchedule.getCronDescription()).isEqualTo(scheduledTriggerCondition.getCronDescription());
    assertThat(qlOnSchedule.getCronExpression()).isEqualTo(scheduledTriggerCondition.getCronExpression());
    assertThat(qlOnSchedule.getOnNewArtifactOnly()).isEqualTo(scheduledTriggerCondition.isOnNewArtifactOnly());
    assertThat(qlOnSchedule.getTriggerConditionType().name())
        .isEqualTo(scheduledTriggerCondition.getConditionType().name());
  }

  @Test
  @Owner(developers = MILAN)
  @Category(UnitTests.class)
  public void
  populateTriggerConditionShouldReturnWebhookTriggerConditionWithGithubWebhookSourceWithPullRequestEventAndAction() {
    String accountId = "1234";
    MainConfiguration mainConfiguration = Mockito.mock(MainConfiguration.class);
    PortalConfig portalConfig = Mockito.mock(PortalConfig.class);

    Mockito.when(portalConfig.getUrl()).thenReturn("URL");
    mainConfiguration.setPortal(portalConfig);
    Mockito.when(mainConfiguration.getPortal()).thenReturn(portalConfig);

    WebHookToken webHookToken =
        WebHookToken.builder().webHookToken("webhookToken").httpMethod("POST").payload("payload").build();
    StringBuilder webhookURL = new StringBuilder(mainConfiguration.getPortal().getUrl());
    webhookURL.append("/api/webhooks/").append(webHookToken.getWebHookToken()).append("?accountId=").append(accountId);

    WebHookTriggerCondition webhookTriggerCondition = WebHookTriggerCondition.builder()
                                                          .webHookToken(webHookToken)
                                                          .webhookSource(WebhookSource.GITHUB)
                                                          .eventTypes(Arrays.asList(WebhookEventType.PULL_REQUEST))
                                                          .actions(Arrays.asList(GithubAction.OPENED))
                                                          .branchRegex("regex")
                                                          .build();
    Trigger trigger = Trigger.builder().condition(webhookTriggerCondition).build();

    QLOnWebhook qlOnWebhook =
        (QLOnWebhook) TriggerConditionController.populateTriggerCondition(trigger, mainConfiguration, accountId);

    assertThat(qlOnWebhook).isNotNull();
    assertThat(qlOnWebhook.getWebhookSource().name()).isEqualTo(webhookTriggerCondition.getWebhookSource().name());
    assertThat(qlOnWebhook.getBranchRegex()).isEqualTo(webhookTriggerCondition.getBranchRegex());
    assertThat(qlOnWebhook.getTriggerConditionType().name())
        .isEqualTo(webhookTriggerCondition.getConditionType().name());
    assertThat(qlOnWebhook.getWebhookDetails()).isNotNull();
    assertThat(qlOnWebhook.getWebhookDetails().getHeader()).isEqualTo("content-type: application/json");
    assertThat(qlOnWebhook.getWebhookDetails().getMethod()).isEqualTo(webHookToken.getHttpMethod());
    assertThat(qlOnWebhook.getWebhookDetails().getPayload()).isEqualTo(webHookToken.getPayload());
    assertThat(qlOnWebhook.getWebhookDetails().getWebhookURL()).isEqualTo(webhookURL.toString());
    assertThat(qlOnWebhook.getWebhookEvent()).isNotNull();
    assertThat(qlOnWebhook.getWebhookEvent().getEvent()).isEqualTo(WebhookEventType.PULL_REQUEST.getValue());
    assertThat(qlOnWebhook.getWebhookEvent().getAction()).isEqualTo(GithubAction.OPENED.getValue());
  }

  @Test
  @Owner(developers = MILAN)
  @Category(UnitTests.class)
  public void
  populateTriggerConditionShouldReturnWebhookTriggerConditionWithGithubWebhookSourceWithPackageEventAndAction() {
    String accountId = "1234";
    MainConfiguration mainConfiguration = Mockito.mock(MainConfiguration.class);
    PortalConfig portalConfig = Mockito.mock(PortalConfig.class);

    Mockito.when(portalConfig.getUrl()).thenReturn("URL");
    mainConfiguration.setPortal(portalConfig);
    Mockito.when(mainConfiguration.getPortal()).thenReturn(portalConfig);

    WebHookToken webHookToken =
        WebHookToken.builder().webHookToken("webhookToken").httpMethod("POST").payload("payload").build();
    StringBuilder webhookURL = new StringBuilder(mainConfiguration.getPortal().getUrl());
    webhookURL.append("/api/webhooks/").append(webHookToken.getWebHookToken()).append("?accountId=").append(accountId);

    WebHookTriggerCondition webhookTriggerCondition = WebHookTriggerCondition.builder()
                                                          .webHookToken(webHookToken)
                                                          .webhookSource(WebhookSource.GITHUB)
                                                          .eventTypes(Arrays.asList(WebhookEventType.PACKAGE))
                                                          .actions(Arrays.asList(GithubAction.OPENED))
                                                          .branchRegex("regex")
                                                          .build();
    Trigger trigger = Trigger.builder().condition(webhookTriggerCondition).build();

    QLOnWebhook qlOnWebhook =
        (QLOnWebhook) TriggerConditionController.populateTriggerCondition(trigger, mainConfiguration, accountId);

    assertThat(qlOnWebhook).isNotNull();
    assertThat(qlOnWebhook.getWebhookSource().name()).isEqualTo(webhookTriggerCondition.getWebhookSource().name());
    assertThat(qlOnWebhook.getBranchRegex()).isEqualTo(webhookTriggerCondition.getBranchRegex());
    assertThat(qlOnWebhook.getTriggerConditionType().name())
        .isEqualTo(webhookTriggerCondition.getConditionType().name());
    assertThat(qlOnWebhook.getWebhookDetails()).isNotNull();
    assertThat(qlOnWebhook.getWebhookDetails().getHeader()).isEqualTo("content-type: application/json");
    assertThat(qlOnWebhook.getWebhookDetails().getMethod()).isEqualTo(webHookToken.getHttpMethod());
    assertThat(qlOnWebhook.getWebhookDetails().getPayload()).isEqualTo(webHookToken.getPayload());
    assertThat(qlOnWebhook.getWebhookDetails().getWebhookURL()).isEqualTo(webhookURL.toString());
    assertThat(qlOnWebhook.getWebhookEvent()).isNotNull();
    assertThat(qlOnWebhook.getWebhookEvent().getEvent()).isEqualTo(WebhookEventType.PACKAGE.getValue());
    assertThat(qlOnWebhook.getWebhookEvent().getAction()).isEqualTo(GithubAction.OPENED.getValue());
  }

  @Test
  @Owner(developers = MILAN)
  @Category(UnitTests.class)
  public void populateTriggerConditionShouldReturnWebhookTriggerConditionWithGithubWebhookSourceWithReleaseAction() {
    String accountId = "1234";
    MainConfiguration mainConfiguration = Mockito.mock(MainConfiguration.class);
    PortalConfig portalConfig = Mockito.mock(PortalConfig.class);

    Mockito.when(portalConfig.getUrl()).thenReturn("URL");
    mainConfiguration.setPortal(portalConfig);
    Mockito.when(mainConfiguration.getPortal()).thenReturn(portalConfig);

    WebHookToken webHookToken =
        WebHookToken.builder().webHookToken("webhookToken").httpMethod("POST").payload("payload").build();
    StringBuilder webhookURL = new StringBuilder(mainConfiguration.getPortal().getUrl());
    webhookURL.append("/api/webhooks/").append(webHookToken.getWebHookToken()).append("?accountId=").append(accountId);

    WebHookTriggerCondition webhookTriggerCondition = WebHookTriggerCondition.builder()
                                                          .webHookToken(webHookToken)
                                                          .webhookSource(WebhookSource.GITHUB)
                                                          .eventTypes(Arrays.asList(WebhookEventType.RELEASE))
                                                          .releaseActions(Arrays.asList(ReleaseAction.CREATED))
                                                          .branchRegex("regex")
                                                          .build();
    Trigger trigger = Trigger.builder().condition(webhookTriggerCondition).build();

    QLOnWebhook qlOnWebhook =
        (QLOnWebhook) TriggerConditionController.populateTriggerCondition(trigger, mainConfiguration, accountId);

    assertThat(qlOnWebhook).isNotNull();
    assertThat(qlOnWebhook.getWebhookSource().name()).isEqualTo(webhookTriggerCondition.getWebhookSource().name());
    assertThat(qlOnWebhook.getBranchRegex()).isEqualTo(webhookTriggerCondition.getBranchRegex());
    assertThat(qlOnWebhook.getTriggerConditionType().name())
        .isEqualTo(webhookTriggerCondition.getConditionType().name());
    assertThat(qlOnWebhook.getWebhookDetails()).isNotNull();
    assertThat(qlOnWebhook.getWebhookDetails().getHeader()).isEqualTo("content-type: application/json");
    assertThat(qlOnWebhook.getWebhookDetails().getMethod()).isEqualTo(webHookToken.getHttpMethod());
    assertThat(qlOnWebhook.getWebhookDetails().getPayload()).isEqualTo(webHookToken.getPayload());
    assertThat(qlOnWebhook.getWebhookDetails().getWebhookURL()).isEqualTo(webhookURL.toString());
    assertThat(qlOnWebhook.getWebhookEvent()).isNotNull();
    assertThat(qlOnWebhook.getWebhookEvent().getEvent()).isEqualTo(WebhookEventType.RELEASE.getValue());
    assertThat(qlOnWebhook.getWebhookEvent().getAction()).isEqualTo(ReleaseAction.CREATED.getValue());
  }

  @Test
  @Owner(developers = MILAN)
  @Category(UnitTests.class)
  public void populateTriggerConditionShouldReturnWebhookTriggerConditionWithGitlabWebhookSource() {
    String accountId = "1234";
    MainConfiguration mainConfiguration = Mockito.mock(MainConfiguration.class);
    PortalConfig portalConfig = Mockito.mock(PortalConfig.class);

    Mockito.when(portalConfig.getUrl()).thenReturn("URL");
    mainConfiguration.setPortal(portalConfig);
    Mockito.when(mainConfiguration.getPortal()).thenReturn(portalConfig);

    WebHookToken webHookToken =
        WebHookToken.builder().webHookToken("webhookToken").httpMethod("POST").payload("payload").build();
    StringBuilder webhookURL = new StringBuilder(mainConfiguration.getPortal().getUrl());
    webhookURL.append("/api/webhooks/").append(webHookToken.getWebHookToken()).append("?accountId=").append(accountId);

    WebHookTriggerCondition webhookTriggerCondition = WebHookTriggerCondition.builder()
                                                          .webHookToken(webHookToken)
                                                          .webhookSource(WebhookSource.GITLAB)
                                                          .eventTypes(Arrays.asList(WebhookEventType.PUSH))
                                                          .build();
    Trigger trigger = Trigger.builder().condition(webhookTriggerCondition).build();

    QLOnWebhook qlOnWebhook =
        (QLOnWebhook) TriggerConditionController.populateTriggerCondition(trigger, mainConfiguration, accountId);

    assertThat(qlOnWebhook).isNotNull();
    assertThat(qlOnWebhook.getWebhookSource().name()).isEqualTo(webhookTriggerCondition.getWebhookSource().name());
    assertThat(qlOnWebhook.getBranchRegex()).isEqualTo(webhookTriggerCondition.getBranchRegex());
    assertThat(qlOnWebhook.getTriggerConditionType().name())
        .isEqualTo(webhookTriggerCondition.getConditionType().name());
    assertThat(qlOnWebhook.getWebhookDetails()).isNotNull();
    assertThat(qlOnWebhook.getWebhookDetails().getHeader()).isEqualTo("content-type: application/json");
    assertThat(qlOnWebhook.getWebhookDetails().getMethod()).isEqualTo(webHookToken.getHttpMethod());
    assertThat(qlOnWebhook.getWebhookDetails().getPayload()).isEqualTo(webHookToken.getPayload());
    assertThat(qlOnWebhook.getWebhookDetails().getWebhookURL()).isEqualTo(webhookURL.toString());
    assertThat(qlOnWebhook.getWebhookEvent()).isNotNull();
    assertThat(qlOnWebhook.getWebhookEvent().getEvent()).isEqualTo(WebhookEventType.PUSH.getValue());
  }

  @Test
  @Owner(developers = MILAN)
  @Category(UnitTests.class)
  public void populateTriggerConditionShouldReturnWebhookTriggerConditionWithBitBucketWebhookSource() {
    String accountId = "1234";
    MainConfiguration mainConfiguration = Mockito.mock(MainConfiguration.class);
    PortalConfig portalConfig = Mockito.mock(PortalConfig.class);

    Mockito.when(portalConfig.getUrl()).thenReturn("URL");
    mainConfiguration.setPortal(portalConfig);
    Mockito.when(mainConfiguration.getPortal()).thenReturn(portalConfig);

    WebHookToken webHookToken =
        WebHookToken.builder().webHookToken("webhookToken").httpMethod("POST").payload("payload").build();
    StringBuilder webhookURL = new StringBuilder(mainConfiguration.getPortal().getUrl());
    webhookURL.append("/api/webhooks/").append(webHookToken.getWebHookToken()).append("?accountId=").append(accountId);

    WebHookTriggerCondition webhookTriggerCondition =
        WebHookTriggerCondition.builder()
            .webHookToken(webHookToken)
            .webhookSource(WebhookSource.BITBUCKET)
            .bitBucketEvents(Arrays.asList(WebhookSource.BitBucketEventType.PULL_REQUEST_CREATED))
            .build();
    Trigger trigger = Trigger.builder().condition(webhookTriggerCondition).build();

    QLOnWebhook qlOnWebhook =
        (QLOnWebhook) TriggerConditionController.populateTriggerCondition(trigger, mainConfiguration, accountId);

    assertThat(qlOnWebhook).isNotNull();
    assertThat(qlOnWebhook.getWebhookSource().name()).isEqualTo(webhookTriggerCondition.getWebhookSource().name());
    assertThat(qlOnWebhook.getBranchRegex()).isEqualTo(webhookTriggerCondition.getBranchRegex());
    assertThat(qlOnWebhook.getTriggerConditionType().name())
        .isEqualTo(webhookTriggerCondition.getConditionType().name());
    assertThat(qlOnWebhook.getWebhookDetails()).isNotNull();
    assertThat(qlOnWebhook.getWebhookDetails().getHeader()).isEqualTo("content-type: application/json");
    assertThat(qlOnWebhook.getWebhookDetails().getMethod()).isEqualTo(webHookToken.getHttpMethod());
    assertThat(qlOnWebhook.getWebhookDetails().getPayload()).isEqualTo(webHookToken.getPayload());
    assertThat(qlOnWebhook.getWebhookDetails().getWebhookURL()).isEqualTo(webhookURL.toString());
    assertThat(qlOnWebhook.getWebhookEvent()).isNotNull();
    assertThat(qlOnWebhook.getWebhookEvent().getEvent()).isEqualTo("pullrequest");
    assertThat(qlOnWebhook.getWebhookEvent().getAction()).isEqualTo("created");
  }

  @Mock PipelineService pipelineService;

  @Mock ArtifactStreamService artifactStreamService;

  @Test
  @Owner(developers = MILAN)
  @Category(UnitTests.class)
  public void resolveTriggerConditionShouldReturnOnNewArtifactConditionType() throws Exception {
    QLTriggerConditionInput qlTriggerConditionInput =
        QLTriggerConditionInput.builder().conditionType(QLConditionType.ON_NEW_ARTIFACT).build();
    QLCreateTriggerInput qlCreateTriggerInput =
        QLCreateTriggerInput.builder().condition(qlTriggerConditionInput).build();

    ArtifactTriggerCondition artifactTriggerCondition = ArtifactTriggerCondition.builder()
                                                            .artifactSourceName("name")
                                                            .artifactStreamId("id")
                                                            .artifactFilter("filter")
                                                            .regex(true)
                                                            .build();

    PowerMockito.spy(TriggerConditionController.class);
    PowerMockito.doReturn(artifactTriggerCondition)
        .when(TriggerConditionController.class, "validateAndResolveOnNewArtifactConditionType", qlCreateTriggerInput,
            artifactStreamService);

    ArtifactTriggerCondition retreivedArtifactTriggerCondition =
        (ArtifactTriggerCondition) TriggerConditionController.resolveTriggerCondition(
            qlCreateTriggerInput, pipelineService, artifactStreamService);

    assertThat(retreivedArtifactTriggerCondition.getArtifactStreamId())
        .isEqualTo(artifactTriggerCondition.getArtifactStreamId());
    assertThat(retreivedArtifactTriggerCondition.getArtifactSourceName())
        .isEqualTo(artifactTriggerCondition.getArtifactSourceName());
    assertThat(retreivedArtifactTriggerCondition.getArtifactFilter())
        .isEqualTo(artifactTriggerCondition.getArtifactFilter());
    assertThat(retreivedArtifactTriggerCondition.isRegex()).isEqualTo(artifactTriggerCondition.isRegex());
  }

  @Test
  @Owner(developers = MILAN)
  @Category(UnitTests.class)
  public void resolveTriggerConditionShouldReturnOnPipelineCompletionConditionType() throws Exception {
    QLTriggerConditionInput qlTriggerConditionInput =
        QLTriggerConditionInput.builder().conditionType(QLConditionType.ON_PIPELINE_COMPLETION).build();
    QLCreateTriggerInput qlCreateTriggerInput =
        QLCreateTriggerInput.builder().condition(qlTriggerConditionInput).build();

    PipelineTriggerCondition pipelineTriggerCondition =
        PipelineTriggerCondition.builder().pipelineId("id").pipelineName("name").build();

    PowerMockito.spy(TriggerConditionController.class);
    PowerMockito.doReturn(pipelineTriggerCondition)
        .when(TriggerConditionController.class, "validateAndResolveOnPipelineCompletionConditionType",
            qlCreateTriggerInput, pipelineService);

    PipelineTriggerCondition retreivedPipelineTriggerCondition =
        (PipelineTriggerCondition) TriggerConditionController.resolveTriggerCondition(
            qlCreateTriggerInput, pipelineService, artifactStreamService);

    assertThat(pipelineTriggerCondition.getPipelineId()).isEqualTo(retreivedPipelineTriggerCondition.getPipelineId());
    assertThat(pipelineTriggerCondition.getPipelineName())
        .isEqualTo(retreivedPipelineTriggerCondition.getPipelineName());
  }

  @Test
  @Owner(developers = MILAN)
  @Category(UnitTests.class)
  public void resolveTriggerConditionShouldReturnOnScheduleConditionType() throws Exception {
    QLTriggerConditionInput qlTriggerConditionInput =
        QLTriggerConditionInput.builder().conditionType(QLConditionType.ON_SCHEDULE).build();
    QLCreateTriggerInput qlCreateTriggerInput =
        QLCreateTriggerInput.builder().condition(qlTriggerConditionInput).build();

    ScheduledTriggerCondition scheduledTriggerCondition = ScheduledTriggerCondition.builder()
                                                              .cronExpression("expression")
                                                              .cronDescription("description")
                                                              .onNewArtifactOnly(true)
                                                              .build();

    PowerMockito.spy(TriggerConditionController.class);
    PowerMockito.doReturn(scheduledTriggerCondition)
        .when(TriggerConditionController.class, "validateAndResolveOnScheduleConditionType", qlCreateTriggerInput);

    ScheduledTriggerCondition retreivedScheduledTriggerCondition =
        (ScheduledTriggerCondition) TriggerConditionController.resolveTriggerCondition(
            qlCreateTriggerInput, pipelineService, artifactStreamService);

    assertThat(retreivedScheduledTriggerCondition.getCronExpression())
        .isEqualTo(scheduledTriggerCondition.getCronExpression());
    assertThat(retreivedScheduledTriggerCondition.getCronDescription())
        .isEqualTo(scheduledTriggerCondition.getCronDescription());
    assertThat(retreivedScheduledTriggerCondition.isOnNewArtifactOnly())
        .isEqualTo(scheduledTriggerCondition.isOnNewArtifactOnly());
  }

  @Test
  @Owner(developers = MILAN)
  @Category(UnitTests.class)
  public void resolveTriggerConditionShouldReturnOnWebhookConditionTypeWithGithubEvent() throws Exception {
    QLTriggerConditionInput qlTriggerConditionInput =
        QLTriggerConditionInput.builder().conditionType(QLConditionType.ON_WEBHOOK).build();
    QLCreateTriggerInput qlCreateTriggerInput =
        QLCreateTriggerInput.builder().condition(qlTriggerConditionInput).build();

    WebHookToken webHookToken =
        WebHookToken.builder().payload("payload").httpMethod("POST").webHookToken("token").build();

    WebHookTriggerCondition webHookTriggerCondition =
        WebHookTriggerCondition.builder()
            .webHookToken(webHookToken)
            .webhookSource(WebhookSource.GITHUB)
            .eventTypes(Collections.singletonList(WebhookEventType.PULL_REQUEST))
            .actions(Collections.singletonList(GithubAction.OPENED))
            .branchRegex("regex")
            .build();

    PowerMockito.spy(TriggerConditionController.class);
    PowerMockito.doReturn(webHookTriggerCondition)
        .when(TriggerConditionController.class, "validateAndResolveOnWebhookConditionType", qlCreateTriggerInput);

    WebHookTriggerCondition retrievedWebHookTriggerCondition =
        (WebHookTriggerCondition) TriggerConditionController.resolveTriggerCondition(
            qlCreateTriggerInput, pipelineService, artifactStreamService);

    assertThat(retrievedWebHookTriggerCondition.getWebHookToken().getPayload())
        .isEqualTo(webHookTriggerCondition.getWebHookToken().getPayload());
    assertThat(retrievedWebHookTriggerCondition.getWebHookToken().getHttpMethod())
        .isEqualTo(webHookTriggerCondition.getWebHookToken().getHttpMethod());
    assertThat(retrievedWebHookTriggerCondition.getWebHookToken().getPayload())
        .isEqualTo(webHookTriggerCondition.getWebHookToken().getPayload());
    assertThat(retrievedWebHookTriggerCondition.getWebhookSource())
        .isEqualTo(webHookTriggerCondition.getWebhookSource());
    assertThat(retrievedWebHookTriggerCondition.getEventTypes().get(0))
        .isEqualTo(webHookTriggerCondition.getEventTypes().get(0));
    assertThat(retrievedWebHookTriggerCondition.getActions().get(0))
        .isEqualTo(webHookTriggerCondition.getActions().get(0));
    assertThat(retrievedWebHookTriggerCondition.getBranchRegex()).isEqualTo(webHookTriggerCondition.getBranchRegex());
  }
}
