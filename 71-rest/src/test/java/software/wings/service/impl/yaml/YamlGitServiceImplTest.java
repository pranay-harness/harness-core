package software.wings.service.impl.yaml;

import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.ANSHUL;
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
import software.wings.beans.GitConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.GitFileChange;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.trigger.WebhookEventUtils;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.yaml.gitSync.YamlGitConfig;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
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

    yamlGitService.checkForValidNameSyntax(gitFileChanges);
    gitFileChanges.add(GitFileChange.Builder.aGitFileChange()
                           .withFilePath("Setup/Applications/app1/Services/service/1/Index.yaml")
                           .build());
    try {
      yamlGitService.checkForValidNameSyntax(gitFileChanges);
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

    wingsPersistence.save(settingAttribute);
    wingsPersistence.save(yamlGitConfig);

    String response =
        yamlGitService.processWebhookPost(ACCOUNT_ID, WEBHOOK_TOKEN, obtainPayload(GH_PUSH_REQ_FILE), httpHeaders);
    assertThat(response).isEqualTo("Successfully queued webhook request for processing");
    verify(waitNotifyEngine, times(1)).waitForAllOn(any(), any(), any());
    verify(delegateService, times(1)).queueTask(any(DelegateTask.class));
  }

  private String obtainPayload(String filePath) throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();

    File file = new File(classLoader.getResource(filePath).getFile());
    return FileUtils.readFileToString(file, Charset.defaultCharset());
  }
}
