package software.wings.graphql.datafetcher.trigger;

import static io.harness.rule.OwnerRule.DEEPAK_PUTHRAYA;
import static io.harness.rule.OwnerRule.MILAN;
import static io.harness.rule.OwnerRule.ROHITKARELIA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.configuration.DeployMode;
import io.harness.rule.Owner;

import software.wings.app.MainConfiguration;
import software.wings.app.PortalConfig;
import software.wings.beans.SettingAttribute;
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
import software.wings.graphql.schema.type.trigger.QLCreateOrUpdateTriggerInput;
import software.wings.graphql.schema.type.trigger.QLOnNewArtifact;
import software.wings.graphql.schema.type.trigger.QLOnPipelineCompletion;
import software.wings.graphql.schema.type.trigger.QLOnSchedule;
import software.wings.graphql.schema.type.trigger.QLOnWebhook;
import software.wings.graphql.schema.type.trigger.QLTriggerConditionInput;
import software.wings.service.intfc.SettingsService;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class TriggerConditionControllerTest extends CategoryTest {
  @Mock MainConfiguration mainConfiguration;
  @Mock SettingsService settingsService;

  @InjectMocks TriggerConditionController triggerConditionController = Mockito.spy(new TriggerConditionController());

  @Before
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
  }

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
        (QLOnNewArtifact) triggerConditionController.populateTriggerCondition(trigger, null);

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
        (QLOnPipelineCompletion) triggerConditionController.populateTriggerCondition(trigger, null);

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

    QLOnSchedule qlOnSchedule = (QLOnSchedule) triggerConditionController.populateTriggerCondition(trigger, null);

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
    PortalConfig portalConfig = Mockito.mock(PortalConfig.class);

    Mockito.when(portalConfig.getUrl()).thenReturn("URL");
    mainConfiguration.setPortal(portalConfig);
    Mockito.when(mainConfiguration.getPortal()).thenReturn(portalConfig);
    when(mainConfiguration.getDeployMode()).thenReturn(DeployMode.KUBERNETES);

    WebHookToken webHookToken =
        WebHookToken.builder().webHookToken("webhookToken").httpMethod("POST").payload("payload").build();
    StringBuilder webhookURL = new StringBuilder(mainConfiguration.getPortal().getUrl());
    webhookURL.append("/gateway/api/webhooks/")
        .append(webHookToken.getWebHookToken())
        .append("?accountId=")
        .append(accountId);

    WebHookTriggerCondition webhookTriggerCondition = WebHookTriggerCondition.builder()
                                                          .webHookToken(webHookToken)
                                                          .webhookSource(WebhookSource.GITHUB)
                                                          .eventTypes(Arrays.asList(WebhookEventType.PULL_REQUEST))
                                                          .actions(Arrays.asList(GithubAction.OPENED))
                                                          .branchRegex("regex")
                                                          .branchName("branchName")
                                                          .gitConnectorId("gitConnectorId")
                                                          .filePaths(Arrays.asList("filePath1", "filePath2"))
                                                          .checkFileContentChanged(true)
                                                          .build();
    Trigger trigger = Trigger.builder().condition(webhookTriggerCondition).build();

    SettingAttribute gitConfig = new SettingAttribute();
    gitConfig.setName("gitConnectorName");
    Mockito.when(settingsService.get(Matchers.anyString())).thenReturn(gitConfig);

    QLOnWebhook qlOnWebhook = (QLOnWebhook) triggerConditionController.populateTriggerCondition(trigger, accountId);

    assertThat(qlOnWebhook).isNotNull();
    assertThat(qlOnWebhook.getWebhookSource().name()).isEqualTo(webhookTriggerCondition.getWebhookSource().name());
    assertThat(qlOnWebhook.getBranchRegex()).isEqualTo(webhookTriggerCondition.getBranchRegex());
    assertThat(qlOnWebhook.getBranchName()).isEqualTo(webhookTriggerCondition.getBranchName());
    assertThat(qlOnWebhook.getGitConnectorId()).isEqualTo(webhookTriggerCondition.getGitConnectorId());
    assertThat(qlOnWebhook.getGitConnectorName()).isEqualTo(gitConfig.getName());
    assertThat(qlOnWebhook.getFilePaths()).isEqualTo(webhookTriggerCondition.getFilePaths());
    assertThat(qlOnWebhook.getDeployOnlyIfFilesChanged())
        .isEqualTo(webhookTriggerCondition.isCheckFileContentChanged());
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
    PortalConfig portalConfig = Mockito.mock(PortalConfig.class);

    Mockito.when(portalConfig.getUrl()).thenReturn("URL");
    mainConfiguration.setPortal(portalConfig);
    Mockito.when(mainConfiguration.getPortal()).thenReturn(portalConfig);
    when(mainConfiguration.getDeployMode()).thenReturn(DeployMode.KUBERNETES);

    WebHookToken webHookToken =
        WebHookToken.builder().webHookToken("webhookToken").httpMethod("POST").payload("payload").build();
    StringBuilder webhookURL = new StringBuilder(mainConfiguration.getPortal().getUrl());
    webhookURL.append("/gateway/api/webhooks/")
        .append(webHookToken.getWebHookToken())
        .append("?accountId=")
        .append(accountId);

    WebHookTriggerCondition webhookTriggerCondition = WebHookTriggerCondition.builder()
                                                          .webHookToken(webHookToken)
                                                          .webhookSource(WebhookSource.GITHUB)
                                                          .eventTypes(Arrays.asList(WebhookEventType.PACKAGE))
                                                          .actions(Arrays.asList(GithubAction.OPENED))
                                                          .branchRegex("regex")
                                                          .branchName("branchName")
                                                          .gitConnectorId("gitConnectorId")
                                                          .filePaths(Arrays.asList("filePath1", "filePath2"))
                                                          .checkFileContentChanged(true)
                                                          .build();
    Trigger trigger = Trigger.builder().condition(webhookTriggerCondition).build();

    SettingAttribute gitConfig = new SettingAttribute();
    gitConfig.setName("gitConnectorName");
    Mockito.when(settingsService.get(Matchers.anyString())).thenReturn(gitConfig);

    QLOnWebhook qlOnWebhook = (QLOnWebhook) triggerConditionController.populateTriggerCondition(trigger, accountId);

    assertThat(qlOnWebhook).isNotNull();
    assertThat(qlOnWebhook.getWebhookSource().name()).isEqualTo(webhookTriggerCondition.getWebhookSource().name());
    assertThat(qlOnWebhook.getBranchRegex()).isEqualTo(webhookTriggerCondition.getBranchRegex());
    assertThat(qlOnWebhook.getBranchName()).isEqualTo(webhookTriggerCondition.getBranchName());
    assertThat(qlOnWebhook.getGitConnectorId()).isEqualTo(webhookTriggerCondition.getGitConnectorId());
    assertThat(qlOnWebhook.getGitConnectorName()).isEqualTo(gitConfig.getName());
    assertThat(qlOnWebhook.getFilePaths()).isEqualTo(webhookTriggerCondition.getFilePaths());
    assertThat(qlOnWebhook.getDeployOnlyIfFilesChanged())
        .isEqualTo(webhookTriggerCondition.isCheckFileContentChanged());
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
    PortalConfig portalConfig = Mockito.mock(PortalConfig.class);

    Mockito.when(portalConfig.getUrl()).thenReturn("URL");
    mainConfiguration.setPortal(portalConfig);
    Mockito.when(mainConfiguration.getPortal()).thenReturn(portalConfig);
    when(mainConfiguration.getDeployMode()).thenReturn(DeployMode.KUBERNETES);

    WebHookToken webHookToken =
        WebHookToken.builder().webHookToken("webhookToken").httpMethod("POST").payload("payload").build();
    StringBuilder webhookURL = new StringBuilder(mainConfiguration.getPortal().getUrl());
    webhookURL.append("/gateway/api/webhooks/")
        .append(webHookToken.getWebHookToken())
        .append("?accountId=")
        .append(accountId);

    WebHookTriggerCondition webhookTriggerCondition = WebHookTriggerCondition.builder()
                                                          .webHookToken(webHookToken)
                                                          .webhookSource(WebhookSource.GITHUB)
                                                          .eventTypes(Arrays.asList(WebhookEventType.RELEASE))
                                                          .releaseActions(Arrays.asList(ReleaseAction.CREATED))
                                                          .branchRegex("regex")
                                                          .branchName("branchName")
                                                          .gitConnectorId("gitConnectorId")
                                                          .filePaths(Arrays.asList("filePath1", "filePath2"))
                                                          .checkFileContentChanged(true)
                                                          .build();
    Trigger trigger = Trigger.builder().condition(webhookTriggerCondition).build();

    SettingAttribute gitConfig = new SettingAttribute();
    gitConfig.setName("gitConnectorName");
    Mockito.when(settingsService.get(Matchers.anyString())).thenReturn(gitConfig);

    QLOnWebhook qlOnWebhook = (QLOnWebhook) triggerConditionController.populateTriggerCondition(trigger, accountId);

    assertThat(qlOnWebhook).isNotNull();
    assertThat(qlOnWebhook.getWebhookSource().name()).isEqualTo(webhookTriggerCondition.getWebhookSource().name());
    assertThat(qlOnWebhook.getBranchRegex()).isEqualTo(webhookTriggerCondition.getBranchRegex());
    assertThat(qlOnWebhook.getBranchName()).isEqualTo(webhookTriggerCondition.getBranchName());
    assertThat(qlOnWebhook.getGitConnectorId()).isEqualTo(webhookTriggerCondition.getGitConnectorId());
    assertThat(qlOnWebhook.getGitConnectorName()).isEqualTo(gitConfig.getName());
    assertThat(qlOnWebhook.getFilePaths()).isEqualTo(webhookTriggerCondition.getFilePaths());
    assertThat(qlOnWebhook.getDeployOnlyIfFilesChanged())
        .isEqualTo(webhookTriggerCondition.isCheckFileContentChanged());
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
    PortalConfig portalConfig = Mockito.mock(PortalConfig.class);

    Mockito.when(portalConfig.getUrl()).thenReturn("URL");
    mainConfiguration.setPortal(portalConfig);
    Mockito.when(mainConfiguration.getPortal()).thenReturn(portalConfig);
    when(mainConfiguration.getDeployMode()).thenReturn(DeployMode.KUBERNETES);

    WebHookToken webHookToken =
        WebHookToken.builder().webHookToken("webhookToken").httpMethod("POST").payload("payload").build();
    StringBuilder webhookURL = new StringBuilder(mainConfiguration.getPortal().getUrl());
    webhookURL.append("/gateway/api/webhooks/")
        .append(webHookToken.getWebHookToken())
        .append("?accountId=")
        .append(accountId);

    WebHookTriggerCondition webhookTriggerCondition = WebHookTriggerCondition.builder()
                                                          .webHookToken(webHookToken)
                                                          .webhookSource(WebhookSource.GITLAB)
                                                          .eventTypes(Arrays.asList(WebhookEventType.PUSH))
                                                          .branchName("branchName")
                                                          .gitConnectorId("gitConnectorId")
                                                          .filePaths(Arrays.asList("filePath1", "filePath2"))
                                                          .checkFileContentChanged(true)
                                                          .build();
    Trigger trigger = Trigger.builder().condition(webhookTriggerCondition).build();

    SettingAttribute gitConfig = new SettingAttribute();
    gitConfig.setName("gitConnectorName");
    Mockito.when(settingsService.get(Matchers.anyString())).thenReturn(gitConfig);

    QLOnWebhook qlOnWebhook = (QLOnWebhook) triggerConditionController.populateTriggerCondition(trigger, accountId);

    assertThat(qlOnWebhook).isNotNull();
    assertThat(qlOnWebhook.getWebhookSource().name()).isEqualTo(webhookTriggerCondition.getWebhookSource().name());
    assertThat(qlOnWebhook.getBranchRegex()).isEqualTo(webhookTriggerCondition.getBranchRegex());
    assertThat(qlOnWebhook.getBranchName()).isEqualTo(webhookTriggerCondition.getBranchName());
    assertThat(qlOnWebhook.getGitConnectorId()).isEqualTo(webhookTriggerCondition.getGitConnectorId());
    assertThat(qlOnWebhook.getGitConnectorName()).isEqualTo(gitConfig.getName());
    assertThat(qlOnWebhook.getFilePaths()).isEqualTo(webhookTriggerCondition.getFilePaths());
    assertThat(qlOnWebhook.getDeployOnlyIfFilesChanged())
        .isEqualTo(webhookTriggerCondition.isCheckFileContentChanged());
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
  @Owner(developers = DEEPAK_PUTHRAYA)
  @Category(UnitTests.class)
  public void populateTriggerConditionShouldReturnWebhookTriggerConditionWithGitlabWebhookSourceWithEventType() {
    String accountId = "1234";
    PortalConfig portalConfig = Mockito.mock(PortalConfig.class);

    Mockito.when(portalConfig.getUrl()).thenReturn("URL");
    mainConfiguration.setPortal(portalConfig);
    Mockito.when(mainConfiguration.getPortal()).thenReturn(portalConfig);
    when(mainConfiguration.getDeployMode()).thenReturn(DeployMode.KUBERNETES);

    WebHookToken webHookToken =
        WebHookToken.builder().webHookToken("webhookToken").httpMethod("POST").payload("payload").build();
    StringBuilder webhookURL = new StringBuilder(mainConfiguration.getPortal().getUrl());
    webhookURL.append("/gateway/api/webhooks/")
        .append(webHookToken.getWebHookToken())
        .append("?accountId=")
        .append(accountId);

    WebHookTriggerCondition webhookTriggerCondition = WebHookTriggerCondition.builder()
                                                          .webHookToken(webHookToken)
                                                          .webhookSource(WebhookSource.GITLAB)
                                                          .eventTypes(null)
                                                          .branchName("branchName")
                                                          .gitConnectorId("gitConnectorId")
                                                          .filePaths(Arrays.asList("filePath1", "filePath2"))
                                                          .checkFileContentChanged(true)
                                                          .build();
    Trigger trigger = Trigger.builder().condition(webhookTriggerCondition).build();

    SettingAttribute gitConfig = new SettingAttribute();
    gitConfig.setName("gitConnectorName");
    Mockito.when(settingsService.get(Matchers.anyString())).thenReturn(gitConfig);

    QLOnWebhook qlOnWebhook = (QLOnWebhook) triggerConditionController.populateTriggerCondition(trigger, accountId);

    assertThat(qlOnWebhook).isNotNull();
    assertThat(qlOnWebhook.getWebhookSource().name()).isEqualTo(webhookTriggerCondition.getWebhookSource().name());
    assertThat(qlOnWebhook.getBranchRegex()).isEqualTo(webhookTriggerCondition.getBranchRegex());
    assertThat(qlOnWebhook.getBranchName()).isEqualTo(webhookTriggerCondition.getBranchName());
    assertThat(qlOnWebhook.getGitConnectorId()).isEqualTo(webhookTriggerCondition.getGitConnectorId());
    assertThat(qlOnWebhook.getGitConnectorName()).isEqualTo(gitConfig.getName());
    assertThat(qlOnWebhook.getFilePaths()).isEqualTo(webhookTriggerCondition.getFilePaths());
    assertThat(qlOnWebhook.getDeployOnlyIfFilesChanged())
        .isEqualTo(webhookTriggerCondition.isCheckFileContentChanged());
    assertThat(qlOnWebhook.getTriggerConditionType().name())
        .isEqualTo(webhookTriggerCondition.getConditionType().name());
    assertThat(qlOnWebhook.getWebhookDetails()).isNotNull();
    assertThat(qlOnWebhook.getWebhookDetails().getHeader()).isEqualTo("content-type: application/json");
    assertThat(qlOnWebhook.getWebhookDetails().getMethod()).isEqualTo(webHookToken.getHttpMethod());
    assertThat(qlOnWebhook.getWebhookDetails().getPayload()).isEqualTo(webHookToken.getPayload());
    assertThat(qlOnWebhook.getWebhookDetails().getWebhookURL()).isEqualTo(webhookURL.toString());
    assertThat(qlOnWebhook.getWebhookEvent()).isNotNull();
    assertThat(qlOnWebhook.getWebhookEvent().getEvent()).isNull();
  }

  @Test
  @Owner(developers = DEEPAK_PUTHRAYA)
  @Category(UnitTests.class)
  public void populateTriggerConditionShouldReturnWebhookTriggerConditionWithBitBucketWebhookSourceEmptyEventType() {
    String accountId = "1234";
    PortalConfig portalConfig = Mockito.mock(PortalConfig.class);

    Mockito.when(portalConfig.getUrl()).thenReturn("URL");
    mainConfiguration.setPortal(portalConfig);
    Mockito.when(mainConfiguration.getPortal()).thenReturn(portalConfig);

    when(mainConfiguration.getDeployMode()).thenReturn(DeployMode.KUBERNETES);

    WebHookToken webHookToken =
        WebHookToken.builder().webHookToken("webhookToken").httpMethod("POST").payload("payload").build();
    StringBuilder webhookURL = new StringBuilder(mainConfiguration.getPortal().getUrl());
    webhookURL.append("/gateway/api/webhooks/")
        .append(webHookToken.getWebHookToken())
        .append("?accountId=")
        .append(accountId);

    WebHookTriggerCondition webhookTriggerCondition = WebHookTriggerCondition.builder()
                                                          .webHookToken(webHookToken)
                                                          .webhookSource(WebhookSource.BITBUCKET)
                                                          .bitBucketEvents(null)
                                                          .branchName("branchName")
                                                          .gitConnectorId("gitConnectorId")
                                                          .filePaths(Arrays.asList("filePath1", "filePath2"))
                                                          .checkFileContentChanged(true)
                                                          .build();
    Trigger trigger = Trigger.builder().condition(webhookTriggerCondition).build();

    SettingAttribute gitConfig = new SettingAttribute();
    gitConfig.setName("gitConnectorName");
    Mockito.when(settingsService.get(Matchers.anyString())).thenReturn(gitConfig);

    QLOnWebhook qlOnWebhook = (QLOnWebhook) triggerConditionController.populateTriggerCondition(trigger, accountId);

    assertThat(qlOnWebhook).isNotNull();
    assertThat(qlOnWebhook.getWebhookSource().name()).isEqualTo(webhookTriggerCondition.getWebhookSource().name());
    assertThat(qlOnWebhook.getBranchRegex()).isEqualTo(webhookTriggerCondition.getBranchRegex());
    assertThat(qlOnWebhook.getBranchName()).isEqualTo(webhookTriggerCondition.getBranchName());
    assertThat(qlOnWebhook.getGitConnectorId()).isEqualTo(webhookTriggerCondition.getGitConnectorId());
    assertThat(qlOnWebhook.getGitConnectorName()).isEqualTo(gitConfig.getName());
    assertThat(qlOnWebhook.getFilePaths()).isEqualTo(webhookTriggerCondition.getFilePaths());
    assertThat(qlOnWebhook.getDeployOnlyIfFilesChanged())
        .isEqualTo(webhookTriggerCondition.isCheckFileContentChanged());
    assertThat(qlOnWebhook.getTriggerConditionType().name())
        .isEqualTo(webhookTriggerCondition.getConditionType().name());
    assertThat(qlOnWebhook.getWebhookDetails()).isNotNull();
    assertThat(qlOnWebhook.getWebhookDetails().getHeader()).isEqualTo("content-type: application/json");
    assertThat(qlOnWebhook.getWebhookDetails().getMethod()).isEqualTo(webHookToken.getHttpMethod());
    assertThat(qlOnWebhook.getWebhookDetails().getPayload()).isEqualTo(webHookToken.getPayload());
    assertThat(qlOnWebhook.getWebhookDetails().getWebhookURL()).isEqualTo(webhookURL.toString());
    assertThat(qlOnWebhook.getWebhookEvent()).isNotNull();
    assertThat(qlOnWebhook.getWebhookEvent().getEvent()).isNull();
    assertThat(qlOnWebhook.getWebhookEvent().getAction()).isNull();
  }

  @Test
  @Owner(developers = MILAN)
  @Category(UnitTests.class)
  public void populateTriggerConditionShouldReturnWebhookTriggerConditionWithBitBucketWebhookSource() {
    String accountId = "1234";
    PortalConfig portalConfig = Mockito.mock(PortalConfig.class);

    Mockito.when(portalConfig.getUrl()).thenReturn("URL");
    mainConfiguration.setPortal(portalConfig);
    Mockito.when(mainConfiguration.getPortal()).thenReturn(portalConfig);

    when(mainConfiguration.getDeployMode()).thenReturn(DeployMode.KUBERNETES);

    WebHookToken webHookToken =
        WebHookToken.builder().webHookToken("webhookToken").httpMethod("POST").payload("payload").build();
    StringBuilder webhookURL = new StringBuilder(mainConfiguration.getPortal().getUrl());
    webhookURL.append("/gateway/api/webhooks/")
        .append(webHookToken.getWebHookToken())
        .append("?accountId=")
        .append(accountId);

    WebHookTriggerCondition webhookTriggerCondition =
        WebHookTriggerCondition.builder()
            .webHookToken(webHookToken)
            .webhookSource(WebhookSource.BITBUCKET)
            .bitBucketEvents(Arrays.asList(WebhookSource.BitBucketEventType.PULL_REQUEST_CREATED))
            .branchName("branchName")
            .gitConnectorId("gitConnectorId")
            .filePaths(Arrays.asList("filePath1", "filePath2"))
            .checkFileContentChanged(true)
            .build();
    Trigger trigger = Trigger.builder().condition(webhookTriggerCondition).build();

    SettingAttribute gitConfig = new SettingAttribute();
    gitConfig.setName("gitConnectorName");
    Mockito.when(settingsService.get(Matchers.anyString())).thenReturn(gitConfig);

    QLOnWebhook qlOnWebhook = (QLOnWebhook) triggerConditionController.populateTriggerCondition(trigger, accountId);

    assertThat(qlOnWebhook).isNotNull();
    assertThat(qlOnWebhook.getWebhookSource().name()).isEqualTo(webhookTriggerCondition.getWebhookSource().name());
    assertThat(qlOnWebhook.getBranchRegex()).isEqualTo(webhookTriggerCondition.getBranchRegex());
    assertThat(qlOnWebhook.getBranchName()).isEqualTo(webhookTriggerCondition.getBranchName());
    assertThat(qlOnWebhook.getGitConnectorId()).isEqualTo(webhookTriggerCondition.getGitConnectorId());
    assertThat(qlOnWebhook.getGitConnectorName()).isEqualTo(gitConfig.getName());
    assertThat(qlOnWebhook.getFilePaths()).isEqualTo(webhookTriggerCondition.getFilePaths());
    assertThat(qlOnWebhook.getDeployOnlyIfFilesChanged())
        .isEqualTo(webhookTriggerCondition.isCheckFileContentChanged());
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

  @Test
  @Owner(developers = MILAN)
  @Category(UnitTests.class)
  public void resolveTriggerConditionShouldReturnOnNewArtifactConditionType() throws Exception {
    QLTriggerConditionInput qlTriggerConditionInput =
        QLTriggerConditionInput.builder().conditionType(QLConditionType.ON_NEW_ARTIFACT).build();
    QLCreateOrUpdateTriggerInput qlCreateOrUpdateTriggerInput =
        QLCreateOrUpdateTriggerInput.builder().condition(qlTriggerConditionInput).build();

    ArtifactTriggerCondition artifactTriggerCondition = ArtifactTriggerCondition.builder()
                                                            .artifactSourceName("name")
                                                            .artifactStreamId("id")
                                                            .artifactFilter("filter")
                                                            .regex(true)
                                                            .build();

    Mockito.doReturn(artifactTriggerCondition)
        .when(triggerConditionController)
        .validateAndResolveOnNewArtifactConditionType(Matchers.any(QLCreateOrUpdateTriggerInput.class));

    ArtifactTriggerCondition retrievedArtifactTriggerCondition =
        (ArtifactTriggerCondition) triggerConditionController.resolveTriggerCondition(qlCreateOrUpdateTriggerInput);

    assertThat(retrievedArtifactTriggerCondition.getArtifactStreamId())
        .isEqualTo(artifactTriggerCondition.getArtifactStreamId());
    assertThat(retrievedArtifactTriggerCondition.getArtifactSourceName())
        .isEqualTo(artifactTriggerCondition.getArtifactSourceName());
    assertThat(retrievedArtifactTriggerCondition.getArtifactFilter())
        .isEqualTo(artifactTriggerCondition.getArtifactFilter());
    assertThat(retrievedArtifactTriggerCondition.isRegex()).isEqualTo(artifactTriggerCondition.isRegex());
  }

  @Test
  @Owner(developers = MILAN)
  @Category(UnitTests.class)
  public void resolveTriggerConditionShouldReturnOnPipelineCompletionConditionType() throws Exception {
    QLTriggerConditionInput qlTriggerConditionInput =
        QLTriggerConditionInput.builder().conditionType(QLConditionType.ON_PIPELINE_COMPLETION).build();
    QLCreateOrUpdateTriggerInput qlCreateOrUpdateTriggerInput =
        QLCreateOrUpdateTriggerInput.builder().condition(qlTriggerConditionInput).build();

    PipelineTriggerCondition pipelineTriggerCondition =
        PipelineTriggerCondition.builder().pipelineId("id").pipelineName("name").build();

    Mockito.doReturn(pipelineTriggerCondition)
        .when(triggerConditionController)
        .validateAndResolveOnPipelineCompletionConditionType(Matchers.any(QLCreateOrUpdateTriggerInput.class));

    PipelineTriggerCondition retrievedPipelineTriggerCondition =
        (PipelineTriggerCondition) triggerConditionController.resolveTriggerCondition(qlCreateOrUpdateTriggerInput);

    assertThat(pipelineTriggerCondition.getPipelineId()).isEqualTo(retrievedPipelineTriggerCondition.getPipelineId());
    assertThat(pipelineTriggerCondition.getPipelineName())
        .isEqualTo(retrievedPipelineTriggerCondition.getPipelineName());
  }

  @Test
  @Owner(developers = MILAN)
  @Category(UnitTests.class)
  public void resolveTriggerConditionShouldReturnOnScheduleConditionType() throws Exception {
    QLTriggerConditionInput qlTriggerConditionInput =
        QLTriggerConditionInput.builder().conditionType(QLConditionType.ON_SCHEDULE).build();
    QLCreateOrUpdateTriggerInput qlCreateTriggerInput =
        QLCreateOrUpdateTriggerInput.builder().condition(qlTriggerConditionInput).build();

    ScheduledTriggerCondition scheduledTriggerCondition = ScheduledTriggerCondition.builder()
                                                              .cronExpression("expression")
                                                              .cronDescription("description")
                                                              .onNewArtifactOnly(true)
                                                              .build();

    Mockito.doReturn(scheduledTriggerCondition)
        .when(triggerConditionController)
        .validateAndResolveOnScheduleConditionType(Matchers.any(QLCreateOrUpdateTriggerInput.class));

    ScheduledTriggerCondition retrievedScheduledTriggerCondition =
        (ScheduledTriggerCondition) triggerConditionController.resolveTriggerCondition(qlCreateTriggerInput);

    assertThat(retrievedScheduledTriggerCondition.getCronExpression())
        .isEqualTo(scheduledTriggerCondition.getCronExpression());
    assertThat(retrievedScheduledTriggerCondition.getCronDescription())
        .isEqualTo(scheduledTriggerCondition.getCronDescription());
    assertThat(retrievedScheduledTriggerCondition.isOnNewArtifactOnly())
        .isEqualTo(scheduledTriggerCondition.isOnNewArtifactOnly());
  }

  @Test
  @Owner(developers = MILAN)
  @Category(UnitTests.class)
  public void resolveTriggerConditionShouldReturnOnWebhookConditionTypeWithGithubEvent() throws Exception {
    QLTriggerConditionInput qlTriggerConditionInput =
        QLTriggerConditionInput.builder().conditionType(QLConditionType.ON_WEBHOOK).build();
    QLCreateOrUpdateTriggerInput qlCreateTriggerInput =
        QLCreateOrUpdateTriggerInput.builder().condition(qlTriggerConditionInput).build();

    WebHookToken webHookToken =
        WebHookToken.builder().payload("payload").httpMethod("POST").webHookToken("token").build();

    WebHookTriggerCondition webHookTriggerCondition =
        WebHookTriggerCondition.builder()
            .webHookToken(webHookToken)
            .webhookSource(WebhookSource.GITHUB)
            .eventTypes(Collections.singletonList(WebhookEventType.PULL_REQUEST))
            .actions(Collections.singletonList(GithubAction.OPENED))
            .branchRegex("regex")
            .checkFileContentChanged(true)
            .filePaths(Arrays.asList("filePath1", "filePath2"))
            .gitConnectorId("gitConnectorId")
            .build();

    Mockito.doReturn(webHookTriggerCondition)
        .when(triggerConditionController)
        .validateAndResolveOnWebhookConditionType(Matchers.any(QLCreateOrUpdateTriggerInput.class));

    WebHookTriggerCondition retrievedWebHookTriggerCondition =
        (WebHookTriggerCondition) triggerConditionController.resolveTriggerCondition(qlCreateTriggerInput);

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
    assertThat(retrievedWebHookTriggerCondition.getBranchName()).isEqualTo(webHookTriggerCondition.getBranchName());
    assertThat(retrievedWebHookTriggerCondition.isCheckFileContentChanged())
        .isEqualTo(webHookTriggerCondition.isCheckFileContentChanged());
    assertThat(retrievedWebHookTriggerCondition.getGitConnectorId())
        .isEqualTo(webHookTriggerCondition.getGitConnectorId());
    assertThat(retrievedWebHookTriggerCondition.getFilePaths()).isEqualTo(webHookTriggerCondition.getFilePaths());
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void populateTriggerConditionShouldReturnWebhookTriggerForOnPrem() {
    String accountId = "1234";
    PortalConfig portalConfig = Mockito.mock(PortalConfig.class);

    Mockito.when(portalConfig.getUrl()).thenReturn("URL");
    mainConfiguration.setPortal(portalConfig);
    Mockito.when(mainConfiguration.getPortal()).thenReturn(portalConfig);

    when(mainConfiguration.getDeployMode()).thenReturn(DeployMode.ONPREM);

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
                                                          .branchName("branchName")
                                                          .gitConnectorId("gitConnectorId")
                                                          .filePaths(Arrays.asList("filePath1", "filePath2"))
                                                          .checkFileContentChanged(true)
                                                          .build();
    Trigger trigger = Trigger.builder().condition(webhookTriggerCondition).build();

    SettingAttribute gitConfig = new SettingAttribute();
    gitConfig.setName("gitConnectorName");
    Mockito.when(settingsService.get(Matchers.anyString())).thenReturn(gitConfig);

    QLOnWebhook qlOnWebhook = (QLOnWebhook) triggerConditionController.populateTriggerCondition(trigger, accountId);

    assertThat(qlOnWebhook).isNotNull();
    assertThat(qlOnWebhook.getWebhookSource().name()).isEqualTo(webhookTriggerCondition.getWebhookSource().name());
    assertThat(qlOnWebhook.getBranchRegex()).isEqualTo(webhookTriggerCondition.getBranchRegex());
    assertThat(qlOnWebhook.getBranchName()).isEqualTo(webhookTriggerCondition.getBranchName());
    assertThat(qlOnWebhook.getGitConnectorId()).isEqualTo(webhookTriggerCondition.getGitConnectorId());
    assertThat(qlOnWebhook.getGitConnectorName()).isEqualTo(gitConfig.getName());
    assertThat(qlOnWebhook.getFilePaths()).isEqualTo(webhookTriggerCondition.getFilePaths());
    assertThat(qlOnWebhook.getDeployOnlyIfFilesChanged())
        .isEqualTo(webhookTriggerCondition.isCheckFileContentChanged());
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
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void populateTriggerConditionShouldReturnWebhookTriggerForK8sOnPrem() {
    String accountId = "1234";
    PortalConfig portalConfig = Mockito.mock(PortalConfig.class);

    Mockito.when(portalConfig.getUrl()).thenReturn("URL");
    mainConfiguration.setPortal(portalConfig);
    Mockito.when(mainConfiguration.getPortal()).thenReturn(portalConfig);

    when(mainConfiguration.getDeployMode()).thenReturn(DeployMode.KUBERNETES_ONPREM);

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
                                                          .branchName("branchName")
                                                          .gitConnectorId("gitConnectorId")
                                                          .filePaths(Arrays.asList("filePath1", "filePath2"))
                                                          .checkFileContentChanged(true)
                                                          .build();
    Trigger trigger = Trigger.builder().condition(webhookTriggerCondition).build();

    SettingAttribute gitConfig = new SettingAttribute();
    gitConfig.setName("gitConnectorName");
    Mockito.when(settingsService.get(Matchers.anyString())).thenReturn(gitConfig);

    QLOnWebhook qlOnWebhook = (QLOnWebhook) triggerConditionController.populateTriggerCondition(trigger, accountId);

    assertThat(qlOnWebhook).isNotNull();
    assertThat(qlOnWebhook.getWebhookSource().name()).isEqualTo(webhookTriggerCondition.getWebhookSource().name());
    assertThat(qlOnWebhook.getBranchRegex()).isEqualTo(webhookTriggerCondition.getBranchRegex());
    assertThat(qlOnWebhook.getBranchName()).isEqualTo(webhookTriggerCondition.getBranchName());
    assertThat(qlOnWebhook.getGitConnectorId()).isEqualTo(webhookTriggerCondition.getGitConnectorId());
    assertThat(qlOnWebhook.getGitConnectorName()).isEqualTo(gitConfig.getName());
    assertThat(qlOnWebhook.getFilePaths()).isEqualTo(webhookTriggerCondition.getFilePaths());
    assertThat(qlOnWebhook.getDeployOnlyIfFilesChanged())
        .isEqualTo(webhookTriggerCondition.isCheckFileContentChanged());
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
  @Owner(developers = DEEPAK_PUTHRAYA)
  @Category(UnitTests.class)
  public void populateTriggerConditionShouldReturnWebhookTriggerWhenGithubActionIsNullForPullRequests() {
    String accountId = "1234";
    PortalConfig portalConfig = Mockito.mock(PortalConfig.class);

    Mockito.when(portalConfig.getUrl()).thenReturn("URL");
    mainConfiguration.setPortal(portalConfig);
    Mockito.when(mainConfiguration.getPortal()).thenReturn(portalConfig);

    when(mainConfiguration.getDeployMode()).thenReturn(DeployMode.KUBERNETES_ONPREM);

    WebHookToken webHookToken =
        WebHookToken.builder().webHookToken("webhookToken").httpMethod("POST").payload("payload").build();
    StringBuilder webhookURL = new StringBuilder(mainConfiguration.getPortal().getUrl());
    webhookURL.append("/api/webhooks/").append(webHookToken.getWebHookToken()).append("?accountId=").append(accountId);

    WebHookTriggerCondition webhookTriggerCondition = WebHookTriggerCondition.builder()
                                                          .webHookToken(webHookToken)
                                                          .webhookSource(WebhookSource.GITHUB)
                                                          .eventTypes(Arrays.asList(WebhookEventType.PULL_REQUEST))
                                                          .actions(null)
                                                          .branchRegex("regex")
                                                          .branchName("branchName")
                                                          .gitConnectorId("gitConnectorId")
                                                          .filePaths(Arrays.asList("filePath1", "filePath2"))
                                                          .checkFileContentChanged(true)
                                                          .build();
    Trigger trigger = Trigger.builder().condition(webhookTriggerCondition).build();

    SettingAttribute gitConfig = new SettingAttribute();
    gitConfig.setName("gitConnectorName");
    Mockito.when(settingsService.get(Matchers.anyString())).thenReturn(gitConfig);

    QLOnWebhook qlOnWebhook = (QLOnWebhook) triggerConditionController.populateTriggerCondition(trigger, accountId);

    assertThat(qlOnWebhook).isNotNull();
    assertThat(qlOnWebhook.getWebhookSource().name()).isEqualTo(webhookTriggerCondition.getWebhookSource().name());
    assertThat(qlOnWebhook.getBranchRegex()).isEqualTo(webhookTriggerCondition.getBranchRegex());
    assertThat(qlOnWebhook.getBranchName()).isEqualTo(webhookTriggerCondition.getBranchName());
    assertThat(qlOnWebhook.getGitConnectorId()).isEqualTo(webhookTriggerCondition.getGitConnectorId());
    assertThat(qlOnWebhook.getGitConnectorName()).isEqualTo(gitConfig.getName());
    assertThat(qlOnWebhook.getFilePaths()).isEqualTo(webhookTriggerCondition.getFilePaths());
    assertThat(qlOnWebhook.getDeployOnlyIfFilesChanged())
        .isEqualTo(webhookTriggerCondition.isCheckFileContentChanged());
    assertThat(qlOnWebhook.getTriggerConditionType().name())
        .isEqualTo(webhookTriggerCondition.getConditionType().name());
    assertThat(qlOnWebhook.getWebhookDetails()).isNotNull();
    assertThat(qlOnWebhook.getWebhookDetails().getHeader()).isEqualTo("content-type: application/json");
    assertThat(qlOnWebhook.getWebhookDetails().getMethod()).isEqualTo(webHookToken.getHttpMethod());
    assertThat(qlOnWebhook.getWebhookDetails().getPayload()).isEqualTo(webHookToken.getPayload());
    assertThat(qlOnWebhook.getWebhookDetails().getWebhookURL()).isEqualTo(webhookURL.toString());
    assertThat(qlOnWebhook.getWebhookEvent()).isNotNull();
    assertThat(qlOnWebhook.getWebhookEvent().getEvent()).isEqualTo(WebhookEventType.PULL_REQUEST.getValue());
    assertThat(qlOnWebhook.getWebhookEvent().getAction()).isNull();
  }

  @Test
  @Owner(developers = DEEPAK_PUTHRAYA)
  @Category(UnitTests.class)
  public void populateTriggerConditionShouldReturnWebhookTriggerWhenGithubActionIsNullForReleases() {
    String accountId = "1234";
    PortalConfig portalConfig = Mockito.mock(PortalConfig.class);

    Mockito.when(portalConfig.getUrl()).thenReturn("URL");
    mainConfiguration.setPortal(portalConfig);
    Mockito.when(mainConfiguration.getPortal()).thenReturn(portalConfig);

    when(mainConfiguration.getDeployMode()).thenReturn(DeployMode.KUBERNETES_ONPREM);

    WebHookToken webHookToken =
        WebHookToken.builder().webHookToken("webhookToken").httpMethod("POST").payload("payload").build();
    StringBuilder webhookURL = new StringBuilder(mainConfiguration.getPortal().getUrl());
    webhookURL.append("/api/webhooks/").append(webHookToken.getWebHookToken()).append("?accountId=").append(accountId);

    WebHookTriggerCondition webhookTriggerCondition = WebHookTriggerCondition.builder()
                                                          .webHookToken(webHookToken)
                                                          .webhookSource(WebhookSource.GITHUB)
                                                          .eventTypes(Arrays.asList(WebhookEventType.RELEASE))
                                                          .actions(null)
                                                          .branchRegex("regex")
                                                          .branchName("branchName")
                                                          .gitConnectorId("gitConnectorId")
                                                          .filePaths(Arrays.asList("filePath1", "filePath2"))
                                                          .checkFileContentChanged(true)
                                                          .build();
    Trigger trigger = Trigger.builder().condition(webhookTriggerCondition).build();

    SettingAttribute gitConfig = new SettingAttribute();
    gitConfig.setName("gitConnectorName");
    Mockito.when(settingsService.get(Matchers.anyString())).thenReturn(gitConfig);

    QLOnWebhook qlOnWebhook = (QLOnWebhook) triggerConditionController.populateTriggerCondition(trigger, accountId);

    assertThat(qlOnWebhook).isNotNull();
    assertThat(qlOnWebhook.getWebhookSource().name()).isEqualTo(webhookTriggerCondition.getWebhookSource().name());
    assertThat(qlOnWebhook.getBranchRegex()).isEqualTo(webhookTriggerCondition.getBranchRegex());
    assertThat(qlOnWebhook.getBranchName()).isEqualTo(webhookTriggerCondition.getBranchName());
    assertThat(qlOnWebhook.getGitConnectorId()).isEqualTo(webhookTriggerCondition.getGitConnectorId());
    assertThat(qlOnWebhook.getGitConnectorName()).isEqualTo(gitConfig.getName());
    assertThat(qlOnWebhook.getFilePaths()).isEqualTo(webhookTriggerCondition.getFilePaths());
    assertThat(qlOnWebhook.getDeployOnlyIfFilesChanged())
        .isEqualTo(webhookTriggerCondition.isCheckFileContentChanged());
    assertThat(qlOnWebhook.getTriggerConditionType().name())
        .isEqualTo(webhookTriggerCondition.getConditionType().name());
    assertThat(qlOnWebhook.getWebhookDetails()).isNotNull();
    assertThat(qlOnWebhook.getWebhookDetails().getHeader()).isEqualTo("content-type: application/json");
    assertThat(qlOnWebhook.getWebhookDetails().getMethod()).isEqualTo(webHookToken.getHttpMethod());
    assertThat(qlOnWebhook.getWebhookDetails().getPayload()).isEqualTo(webHookToken.getPayload());
    assertThat(qlOnWebhook.getWebhookDetails().getWebhookURL()).isEqualTo(webhookURL.toString());
    assertThat(qlOnWebhook.getWebhookEvent()).isNotNull();
    assertThat(qlOnWebhook.getWebhookEvent().getEvent()).isEqualTo(WebhookEventType.RELEASE.getValue());
    assertThat(qlOnWebhook.getWebhookEvent().getAction()).isNull();
  }
}
