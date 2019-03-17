package software.wings.service.impl.yaml;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import io.harness.category.element.UnitTests;
import io.harness.exception.WingsException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.beans.Account;
import software.wings.beans.yaml.GitFileChange;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.yaml.YamlDirectoryService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class YamlGitServiceImplTest {
  @InjectMocks private YamlGitServiceImpl yamlGitService = spy(YamlGitServiceImpl.class);
  @Mock private DelegateService delegateService;
  @Mock private AlertService alertService;
  @Mock private SecretManager secretManager;
  @Mock private AccountService mockAccountService;
  @Mock private YamlDirectoryService mockYamlDirectoryService;

  private static final String TEST_GIT_REPO_URL = "https://github.com/rathn/SyncTest";
  private static final String TEST_GIT_REPO_USER = "user";
  private static final String TEST_GIT_REPO_PASSWORD = "password";

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
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

    yamlGitService.checkForValidNameSyntax(gitFileChanges);
    gitFileChanges.add(GitFileChange.Builder.aGitFileChange()
                           .withFilePath("Setup/Applications/app1/Services/service/1/Index.yaml")
                           .build());
    try {
      yamlGitService.checkForValidNameSyntax(gitFileChanges);
      assertTrue(false);
    } catch (Exception ex) {
      assertTrue(ex instanceof WingsException);
      assertTrue(ex.getMessage().contains(
          "Invalid entity name, entity can not contain / in the name. Caused invalid file path:"));
    }
  }

  @Test
  @Category(UnitTests.class)
  public void getAllYamlErrorsForAccount() {
    yamlGitService.getAllYamlErrorsForAccount(ACCOUNT_ID);
    verify(mockYamlDirectoryService)
        .traverseDirectory(anyList(), eq(ACCOUNT_ID), any(), anyString(), eq(false), eq(false), any());
  }

  @Test
  @Category(UnitTests.class)
  public void getAllYamlErrorsForAllAccounts() {
    List<Account> accounts = Arrays.asList(anAccount().withAccountName("Name1").withUuid("AccId1").build(),
        anAccount().withAccountName("Name2").withUuid("AccId2").build());
    doReturn(accounts).when(mockAccountService).list(any());
    yamlGitService.getAllYamlErrorsForAllAccounts();
    verify(yamlGitService, times(2)).getAllYamlErrorsForAccount(anyString());
  }
}
