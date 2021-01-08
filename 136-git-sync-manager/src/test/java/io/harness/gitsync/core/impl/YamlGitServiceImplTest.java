package io.harness.gitsync.core.impl;

import static io.harness.gitsync.common.YamlProcessingLogContext.WEBHOOK_TOKEN;
import static io.harness.gitsync.core.impl.YamlGitServiceImpl.WEBHOOK_SUCCESS_MSG;
import static io.harness.rule.OwnerRule.ABHINAV;
import static io.harness.waiter.NgOrchestrationNotifyEventListener.NG_ORCHESTRATION;

import static software.wings.beans.trigger.WebhookSource.GITHUB;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.apis.dto.ConnectorInfoDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.encryption.Scope;
import io.harness.git.model.ChangeType;
import io.harness.git.model.GitFileChange;
import io.harness.gitsync.common.beans.YamlChangeSet;
import io.harness.gitsync.common.service.YamlGitConfigService;
import io.harness.gitsync.core.beans.GitCommit;
import io.harness.gitsync.core.beans.GitWebhookRequestAttributes;
import io.harness.gitsync.core.callback.GitCommandCallback;
import io.harness.gitsync.core.service.GitCommitService;
import io.harness.gitsync.core.service.YamlChangeSetService;
import io.harness.gitsync.gitsyncerror.service.GitSyncErrorService;
import io.harness.repositories.repositories.yamlGitConfig.YamlGitConfigRepository;
import io.harness.repositories.repositories.yamlGitFolderConfig.YamlGitFolderConfigRepository;
import io.harness.rule.Owner;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.waiter.WaitNotifyEngine;

import software.wings.beans.SettingAttribute;
import software.wings.service.impl.trigger.WebhookEventUtils;

import com.google.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Optional;
import javax.ws.rs.core.HttpHeaders;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

public class YamlGitServiceImplTest extends CategoryTest {
  public static final String ACCOUNTID = "ACCOUNTID";
  static final String GH_PUSH_REQ_FILE = "software/wings/service/impl/webhook/github_push_request.json";

  @Inject YamlGitConfigRepository yamlGitConfigRepository;
  @Inject YamlGitFolderConfigRepository yamlGitFolderConfigRepository;
  @Mock YamlGitConfigService yamlGitConfigService;
  @Mock SecretManagerClientService ngSecretService;
  @Mock GitSyncErrorService gitSyncErrorService;
  @Mock WaitNotifyEngine waitNotifyEngine;
  @Mock private HttpHeaders httpHeaders;
  @Mock YamlChangeSetService yamlChangeSetService;
  @Mock GitCommitService gitCommitService;
  @Mock WebhookEventUtils webhookEventUtils;

  @InjectMocks @Inject @Spy public YamlGitServiceImpl yamlGitService;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testHandleHarnessChangeSet() {
    GitFileChange gitFileChange = GitFileChange.builder()
                                      .rootPath("abc")
                                      .changeType(ChangeType.ADD)
                                      .fileContent("abc")
                                      .filePath("path")
                                      .syncFromGit(false)
                                      .build();
    YamlChangeSet yamlChangeSet = YamlChangeSet.builder()
                                      .status(YamlChangeSet.Status.QUEUED)
                                      .scope(Scope.ACCOUNT)
                                      .accountId(ACCOUNTID)
                                      .gitToHarness(false)
                                      .gitFileChanges(Collections.singletonList(gitFileChange))
                                      .build();

    doReturn(null).when(ngSecretService).getEncryptionDetails(any(), any());
    doReturn(YamlGitConfigDTO.builder().build())
        .when(yamlGitConfigService)
        .getByFolderIdentifierAndIsEnabled(null, null, ACCOUNTID, null);
    doReturn(Optional.of(ConnectorInfoDTO.builder()
                             .connectorType(ConnectorType.GIT)
                             .connectorConfig(GitConfigDTO.builder().build())
                             .build()))
        .when(yamlGitConfigService)
        .getGitConnector(any(), any(), any(), any());
    yamlGitService.handleHarnessChangeSet(yamlChangeSet, ACCOUNTID);
    verify(waitNotifyEngine, times(1)).waitForAllOn(any(), any(), any());
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testHandleHarnessChangeSetError() {
    GitFileChange gitFileChange = GitFileChange.builder()
                                      .rootPath("abc")
                                      .changeType(ChangeType.ADD)
                                      .fileContent("abc")
                                      .filePath("path")
                                      .syncFromGit(false)
                                      .build();
    YamlChangeSet yamlChangeSet = YamlChangeSet.builder()
                                      .status(YamlChangeSet.Status.QUEUED)
                                      .scope(Scope.ACCOUNT)
                                      .accountId(ACCOUNTID)
                                      .gitToHarness(false)
                                      .gitFileChanges(Collections.singletonList(gitFileChange))
                                      .build();

    yamlGitService.handleHarnessChangeSet(yamlChangeSet, ACCOUNTID);
    verify(yamlGitConfigService, times(0)).getGitConnector(any(), any(), any(), any());
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testRemoveGitSyncError() {
    GitFileChange gitFileChange = GitFileChange.builder()
                                      .rootPath("abc")
                                      .changeType(ChangeType.ADD)
                                      .fileContent("abc")
                                      .filePath("path")
                                      .syncFromGit(false)
                                      .build();
    yamlGitService.removeGitSyncErrors(ACCOUNTID, null, null, Collections.singletonList(gitFileChange), false);
    verify(gitSyncErrorService, times(1))
        .deleteByAccountIdOrgIdProjectIdAndFilePath(ACCOUNTID, null, null, Collections.singletonList("path"));
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testProcessWebhookPost() throws Exception {
    when(webhookEventUtils.isGitPingEvent(httpHeaders)).thenReturn(false);
    when(webhookEventUtils.obtainWebhookSource(httpHeaders)).thenReturn(GITHUB);
    doNothing().when(webhookEventUtils).validatePushEvent(GITHUB, httpHeaders);

    doReturn(Collections.singletonList(SettingAttribute.Builder.aSettingAttribute().build()))
        .when(yamlGitService)
        .getGitConnectors(anyString());
    doReturn("abc").when(yamlGitService).getGitConnectorIdByWebhookToken(any(), anyString());
    doReturn(Collections.singletonList(YamlGitConfigDTO.builder().build()))
        .when(yamlGitConfigService)
        .getByConnectorRepoAndBranch(any(), any(), any(), any());

    when(webhookEventUtils.obtainBranchName(any(), any(), any())).thenReturn("master");
    when(webhookEventUtils.obtainRepositoryName(any(), any(), any())).thenReturn(Optional.of("test-repo"));

    final YamlChangeSet yamlChangeSet = YamlChangeSet.builder().build();
    yamlChangeSet.setUuid("uuid");
    doReturn(yamlChangeSet).when(yamlChangeSetService).save(any(YamlChangeSet.class));

    String response = yamlGitService.validateAndQueueWebhookRequest(
        ACCOUNT_ID, WEBHOOK_TOKEN, obtainPayload(GH_PUSH_REQ_FILE), httpHeaders);
    assertThat(response).isEqualTo(WEBHOOK_SUCCESS_MSG);
    verify(yamlChangeSetService, times(1)).save(any(YamlChangeSet.class));
  }

  private String obtainPayload(String filePath) throws IOException {
    return IOUtils.toString(
        Thread.currentThread().getContextClassLoader().getResourceAsStream(filePath), StandardCharsets.UTF_8);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void test_handleGitChangeSet() {
    final GitWebhookRequestAttributes gitWebhookRequestAttributes = GitWebhookRequestAttributes.builder()
                                                                        .gitConnectorId(SETTING_ID)
                                                                        .branchName("branchName")
                                                                        .headCommitId("commitId")
                                                                        .repo("repo")
                                                                        .build();
    final YamlChangeSet yamlChangeSet =
        YamlChangeSet.builder().accountId(ACCOUNT_ID).gitWebhookRequestAttributes(gitWebhookRequestAttributes).build();
    yamlChangeSet.setUuid("changesetId");

    doReturn(Optional.empty()).when(gitCommitService).findGitCommitWithProcessedStatus(any(), any(), any(), any());
    doReturn(Collections.singletonList(YamlGitConfigDTO.builder().build()))
        .when(yamlGitConfigService)
        .getByConnectorRepoAndBranch(any(), any(), any(), any());
    doReturn(Optional.of(GitCommit.builder().build()))
        .when(gitCommitService)
        .findLastProcessedGitCommit(any(), any(), any());
    doReturn(null).when(ngSecretService).getEncryptionDetails(any(), any());
    doReturn(Optional.of(ConnectorInfoDTO.builder()
                             .connectorType(ConnectorType.GIT)
                             .connectorConfig(GitConfigDTO.builder().build())
                             .build()))
        .when(yamlGitConfigService)
        .getGitConnector(any(), any(), any(), any());

    yamlGitService.handleGitChangeSet(yamlChangeSet, ACCOUNT_ID);
    verify(waitNotifyEngine, times(1)).waitForAllOn(eq(NG_ORCHESTRATION), any(GitCommandCallback.class), anyString());
    verify(yamlChangeSetService, times(0))
        .updateStatus(eq(ACCOUNT_ID), eq("changesetId"), any(YamlChangeSet.Status.class));
  }
}
