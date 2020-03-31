package software.wings.service.impl.yaml;

import static io.harness.microservice.NotifyEngineTarget.GENERAL;
import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.ROHIT_KUMAR;
import static io.harness.rule.OwnerRule.SATYAM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.trigger.WebhookSource.GITHUB;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.exception.WingsException;
import io.harness.rule.Owner;
import io.harness.waiter.WaitNotifyEngine;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.EntityType;
import software.wings.beans.GitConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.GitFileChange;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.trigger.WebhookEventUtils;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.yaml.YamlChangeSetService;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.yaml.errorhandling.GitSyncError;
import software.wings.yaml.gitSync.GitWebhookRequestAttributes;
import software.wings.yaml.gitSync.YamlChangeSet;
import software.wings.yaml.gitSync.YamlGitConfig;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.ws.rs.core.HttpHeaders;

public class YamlGitServiceImplTest extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;

  @Mock private DelegateService delegateService;
  @Mock private AlertService alertService;
  @Mock private SecretManager secretManager;
  @Mock private AccountService mockAccountService;
  @Mock private YamlDirectoryService mockYamlDirectoryService;
  @Mock private HttpHeaders httpHeaders;
  @Mock private WebhookEventUtils webhookEventUtils;
  @Mock private WaitNotifyEngine waitNotifyEngine;
  @Mock private YamlChangeSetService yamlChangeSetService;

  @InjectMocks @Inject private YamlGitServiceImpl yamlGitService;

  private static final String TEST_GIT_REPO_URL = "https://github.com/rathn/SyncTest";
  private static final String TEST_GIT_REPO_USER = "user";
  private static final String TEST_GIT_REPO_PASSWORD = "password";
  private static final String WEBHOOK_TOKEN = "Webhook_Token";
  private static final String GH_PUSH_REQ_FILE = "software/wings/service/impl/webhook/github_push_request.json";

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testCheckForValidNameSyntax() throws Exception {
    List<GitFileChange> gitFileChanges = new ArrayList<>();
    gitFileChanges.add(GitFileChange.Builder.aGitFileChange().withFilePath("Setup/Defaults.yaml").build());
    gitFileChanges.add(
        GitFileChange.Builder.aGitFileChange().withFilePath("Setup/Applications/App1/Index.yaml").build());
    gitFileChanges.add(
        GitFileChange.Builder.aGitFileChange().withFilePath("Setup/Artifact Servers/jenkins.yaml").build());
    gitFileChanges.add(
        GitFileChange.Builder.aGitFileChange().withFilePath("Setup/Verification Providers/NewRelic.yaml").build());
    gitFileChanges.add(GitFileChange.Builder.aGitFileChange()
                           .withFilePath("Setup/Applications/app1/Services/service1/Index.yaml")
                           .build());
    gitFileChanges.add(GitFileChange.Builder.aGitFileChange().withFilePath("Setup/Applications").build());
    gitFileChanges.add(GitFileChange.Builder.aGitFileChange().withFilePath("Setup/Applications/app1/Services").build());
    gitFileChanges.add(GitFileChange.Builder.aGitFileChange().withFilePath("Setup/Verification Providers").build());
    gitFileChanges.add(GitFileChange.Builder.aGitFileChange().withFilePath("Setup/Verification Providers").build());
    gitFileChanges.add(
        GitFileChange.Builder.aGitFileChange()
            .withFilePath("Setup/Applications/App1/Environments/env1/PCF Overrides/Services/SERVICE_NAME/a.yml")
            .build());
    gitFileChanges.add(
        GitFileChange.Builder.aGitFileChange()
            .withFilePath("Setup/Applications/App1/Environments/env1/PCF Overrides/Services/SERVICE_NAME/a.yml")
            .build());
    gitFileChanges.add(
        GitFileChange.Builder.aGitFileChange()
            .withFilePath("Setup/Applications/App1/Environments/env1/PCF Overrides/Services/SERVICE_NAME/Index.yaml")
            .build());

    yamlGitService.ensureValidNameSyntax(gitFileChanges);
    gitFileChanges.add(GitFileChange.Builder.aGitFileChange()
                           .withFilePath("Setup/Applications/app1/Services/service/1/Index.yaml")
                           .build());
    try {
      yamlGitService.ensureValidNameSyntax(gitFileChanges);
      assertThat(false).isTrue();
    } catch (Exception ex) {
      assertThat(ex instanceof WingsException).isTrue();
      assertThat(ex.getMessage().contains(
                     "Invalid entity name, entity can not contain / in the name. Caused invalid file path:"))
          .isTrue();
    }
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void getAllYamlErrorsForAccount() {
    yamlGitService.getAllYamlErrorsForAccount(ACCOUNT_ID);
    verify(mockYamlDirectoryService)
        .traverseDirectory(anyList(), eq(ACCOUNT_ID), any(), anyString(), eq(false), eq(false), any());
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void getAllYamlErrorsForAllAccounts() {
    List<Account> accounts = Arrays.asList(anAccount().withAccountName("Name1").withUuid("AccId1").build(),
        anAccount().withAccountName("Name2").withUuid("AccId2").build());
    doReturn(accounts).when(mockAccountService).list(any());
    yamlGitService.getAllYamlErrorsForAllAccounts();
    verify(mockYamlDirectoryService, times(2)).getDirectory(anyString(), anyString(), anyBoolean(), any());
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testProcessWebhookPost() throws Exception {
    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withAccountId(ACCOUNT_ID)
            .withUuid(SETTING_ID)
            .withName("GitConnector")
            .withValue(GitConfig.builder().accountId(ACCOUNT_ID).webhookToken(WEBHOOK_TOKEN).build())
            .build();

    YamlGitConfig yamlGitConfig =
        YamlGitConfig.builder().accountId(ACCOUNT_ID).gitConnectorId(SETTING_ID).branchName("master").build();

    when(webhookEventUtils.isGitPingEvent(httpHeaders)).thenReturn(false);
    when(webhookEventUtils.obtainWebhookSource(httpHeaders)).thenReturn(GITHUB);
    doNothing().when(webhookEventUtils).validatePushEvent(GITHUB, httpHeaders);
    when(webhookEventUtils.obtainBranchName(any(), any(), any())).thenReturn("master");
    final YamlChangeSet yamlChangeSet = YamlChangeSet.builder().build();
    yamlChangeSet.setUuid("uuid");
    doReturn(yamlChangeSet).when(yamlChangeSetService).save(any(YamlChangeSet.class));
    wingsPersistence.save(settingAttribute);
    wingsPersistence.save(yamlGitConfig);

    String response = yamlGitService.validateAndQueueWebhookRequest(
        ACCOUNT_ID, WEBHOOK_TOKEN, obtainPayload(GH_PUSH_REQ_FILE), httpHeaders);
    assertThat(response).isEqualTo("Successfully accepted webhook request for processing");
    verify(yamlChangeSetService, times(1)).save(any(YamlChangeSet.class));
  }

  private String obtainPayload(String filePath) throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();

    File file = new File(classLoader.getResource(filePath).getFile());
    return FileUtils.readFileToString(file, Charset.defaultCharset());
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void test_handleGitChangeSet() {
    final GitWebhookRequestAttributes gitWebhookRequestAttributes = GitWebhookRequestAttributes.builder()
                                                                        .gitConnectorId(SETTING_ID)
                                                                        .branchName("branchName")
                                                                        .headCommitId("commitId")
                                                                        .build();
    final YamlChangeSet yamlChangeSet =
        YamlChangeSet.builder().gitWebhookRequestAttributes(gitWebhookRequestAttributes).build();
    yamlChangeSet.setUuid("changesetId");

    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withAccountId(ACCOUNT_ID)
            .withUuid(SETTING_ID)
            .withName("gitconnectorid")
            .withValue(GitConfig.builder().accountId(ACCOUNT_ID).webhookToken(WEBHOOK_TOKEN).build())
            .build();

    YamlGitConfig yamlGitConfig =
        YamlGitConfig.builder().accountId(ACCOUNT_ID).gitConnectorId(SETTING_ID).branchName("branchName").build();
    wingsPersistence.save(settingAttribute);
    wingsPersistence.save(yamlGitConfig);
    yamlGitService.handleGitChangeSet(yamlChangeSet, ACCOUNT_ID);
    verify(delegateService, times(1)).queueTask(any(DelegateTask.class));
    verify(waitNotifyEngine, times(1)).waitForAllOn(eq(GENERAL), any(GitCommandCallback.class), anyString());
    verify(yamlChangeSetService, times(0))
        .updateStatus(eq(ACCOUNT_ID), eq("changesetId"), any(YamlChangeSet.Status.class));
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void test_getActiveGitToHarnessSyncErrors() {
    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withAccountId(ACCOUNT_ID)
            .withUuid(SETTING_ID)
            .withName("gitconnectorid")
            .withValue(GitConfig.builder().accountId(ACCOUNT_ID).webhookToken(WEBHOOK_TOKEN).build())
            .build();
    wingsPersistence.save(settingAttribute);

    YamlGitConfig yamlGitConfig1 = YamlGitConfig.builder()
                                       .entityType(EntityType.APPLICATION)
                                       .entityId("appid")
                                       .accountId(ACCOUNT_ID)
                                       .gitConnectorId(SETTING_ID)
                                       .branchName("branchName")
                                       .enabled(true)
                                       .build();

    YamlGitConfig yamlGitConfig2 = YamlGitConfig.builder()
                                       .entityType(EntityType.ACCOUNT)
                                       .entityId(ACCOUNT_ID)
                                       .accountId(ACCOUNT_ID)
                                       .gitConnectorId(SETTING_ID)
                                       .branchName("branchName")
                                       .enabled(true)
                                       .build();

    YamlGitConfig yamlGitConfig3 = YamlGitConfig.builder()
                                       .entityType(EntityType.APPLICATION)
                                       .entityId("appid1")
                                       .accountId(ACCOUNT_ID)
                                       .gitConnectorId(SETTING_ID)
                                       .branchName("branchName123")
                                       .enabled(true)
                                       .build();
    yamlGitConfig1.setAppId("appid");
    wingsPersistence.save(yamlGitConfig1);
    wingsPersistence.save(yamlGitConfig2);
    wingsPersistence.save(yamlGitConfig3);

    final GitSyncError gitSyncError1 = GitSyncError.builder()
                                           .yamlFilePath("Setup/index123.yaml")
                                           .yamlContent("ds")
                                           .accountId(ACCOUNT_ID)
                                           .changeType("MODIFY")
                                           .fullSyncPath(false)
                                           .gitCommitId("commitid")
                                           .branchName("branchName")
                                           .gitConnectorId(SETTING_ID)
                                           .build();
    gitSyncError1.setAppId("appid");
    gitSyncError1.setStatus(GitSyncErrorStatus.ACTIVE);
    final String savedGitSyncError1 = wingsPersistence.save(gitSyncError1);

    final GitSyncError gitSyncError2 = GitSyncError.builder()
                                           .yamlFilePath("Setup/index456.yaml")
                                           .yamlContent("ds")
                                           .accountId(ACCOUNT_ID)
                                           .changeType("MODIFY")
                                           .fullSyncPath(false)
                                           .gitCommitId("commitid")
                                           .branchName("branchName")
                                           .gitConnectorId(SETTING_ID)
                                           .build();
    gitSyncError2.setAppId(Application.GLOBAL_APP_ID);
    gitSyncError2.setStatus(null);
    final String savedGitSyncError2 = wingsPersistence.save(gitSyncError2);

    final GitSyncError gitSyncError3 = GitSyncError.builder()
                                           .yamlFilePath("Setup/index789.yaml")
                                           .yamlContent("ds")
                                           .accountId(ACCOUNT_ID)
                                           .changeType("MODIFY")
                                           .fullSyncPath(false)
                                           .gitCommitId("commitid")
                                           .branchName("branchName")
                                           .gitConnectorId(SETTING_ID)
                                           .build();
    gitSyncError3.setAppId(Application.GLOBAL_APP_ID);
    final String savedGitSyncError3 = wingsPersistence.save(gitSyncError3);

    final GitSyncError gitSyncError4 = GitSyncError.builder()
                                           .yamlFilePath("Setup/index101112.yaml")
                                           .yamlContent("ds")
                                           .accountId(ACCOUNT_ID)
                                           .changeType("MODIFY")
                                           .fullSyncPath(false)
                                           .gitCommitId("commitid")
                                           .branchName("branchName")
                                           .gitConnectorId(SETTING_ID)
                                           .build();
    gitSyncError4.setAppId("appid1");

    wingsPersistence.save(gitSyncError4);

    final GitSyncError gitSyncError5 = GitSyncError.builder()
                                           .yamlFilePath("Setup/index131415.yaml")
                                           .yamlContent("ds")
                                           .accountId(ACCOUNT_ID)
                                           .changeType("MODIFY")
                                           .fullSyncPath(false)
                                           .gitCommitId("commitid")
                                           .branchName("branchName")
                                           .gitConnectorId(SETTING_ID)
                                           .build();
    gitSyncError5.setStatus(GitSyncErrorStatus.EXPIRED);
    wingsPersistence.save(gitSyncError5);

    final GitSyncError gitSyncError6 = GitSyncError.builder()
                                           .yamlFilePath("Setup/index789456.yaml")
                                           .yamlContent("ds")
                                           .accountId(ACCOUNT_ID)
                                           .changeType("MODIFY")
                                           .gitCommitId("")
                                           .branchName("branchName")
                                           .gitConnectorId(SETTING_ID)
                                           .build();
    gitSyncError6.setAppId(Application.GLOBAL_APP_ID);
    wingsPersistence.save(gitSyncError6);

    final GitSyncError gitSyncError7 = GitSyncError.builder()
                                           .yamlFilePath("Setup/index789456565.yaml")
                                           .yamlContent("ds")
                                           .accountId(ACCOUNT_ID)
                                           .changeType("MODIFY")
                                           .gitCommitId(null)
                                           .branchName("branchName")
                                           .gitConnectorId(SETTING_ID)
                                           .build();
    gitSyncError7.setAppId(Application.GLOBAL_APP_ID);
    wingsPersistence.save(gitSyncError7);

    final long _30_days_millis = System.currentTimeMillis() - Duration.ofDays(30).toMillis();

    final List<GitSyncError> activeGitToHarnessSyncErrors =
        yamlGitService.getActiveGitToHarnessSyncErrors(ACCOUNT_ID, SETTING_ID, "branchName", _30_days_millis);

    assertThat(activeGitToHarnessSyncErrors.stream().map(GitSyncError::getUuid))
        .contains(savedGitSyncError1, savedGitSyncError2, savedGitSyncError3);
    assertThat(activeGitToHarnessSyncErrors).hasSize(3);
  }
}
