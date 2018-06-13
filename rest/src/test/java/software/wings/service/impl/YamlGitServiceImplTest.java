package software.wings.service.impl;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.beans.Account;
import software.wings.beans.GitConfig;
import software.wings.beans.yaml.GitCommandExecutionResponse;
import software.wings.beans.yaml.GitCommandExecutionResponse.GitCommandStatus;
import software.wings.beans.yaml.GitFileChange;
import software.wings.exception.WingsException;
import software.wings.service.impl.yaml.YamlGitServiceImpl;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.yaml.YamlDirectoryService;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class YamlGitServiceImplTest {
  private @InjectMocks YamlGitServiceImpl yamlGitService = spy(YamlGitServiceImpl.class);
  private @Mock DelegateService delegateService;
  private @Mock AlertService alertService;
  private @Mock SecretManager secretManager;
  private @Mock AccountService mockAccountService;
  private @Mock YamlDirectoryService mockYamlDirectoryService;

  private static final String TEST_GIT_REPO_URL = "https://github.com/rathn/SyncTest";
  private static final String TEST_GIT_REPO_USER = "user";
  private static final String TEST_GIT_REPO_PASSWORD = "password";

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
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

    MethodUtils.invokeMethod(yamlGitService, true, "checkForValidNameSyntax", gitFileChanges);

    gitFileChanges.add(GitFileChange.Builder.aGitFileChange()
                           .withFilePath("Setup/Applications/app1/Services/service/1/Index.yaml")
                           .build());
    try {
      MethodUtils.invokeMethod(yamlGitService, true, "checkForValidNameSyntax", gitFileChanges);
      assertTrue(false);
    } catch (InvocationTargetException ex) {
      assertTrue(ex.getTargetException() instanceof WingsException);
      assertTrue(ex.getTargetException().getMessage().contains(
          "Invalid entity name, entity can not contain / in the name. Caused invalid file path:"));
    }
  }

  @Test
  public void testValidateGit() throws Exception {
    doReturn(GitCommandExecutionResponse.builder()
                 .gitCommandStatus(GitCommandStatus.FAILURE)
                 .errorMessage("Invalid Repo")
                 .build())
        .doReturn(GitCommandExecutionResponse.builder()
                      .gitCommandStatus(GitCommandStatus.SUCCESS)
                      .errorMessage(StringUtils.EMPTY)
                      .build())
        .when(delegateService)
        .executeTask(any());

    doReturn(null).when(alertService).openAlert(any(), any(), any(), any());
    doNothing().when(alertService).closeAlert(any(), any(), any(), any());
    doReturn(null).when(secretManager).getEncryptionDetails(any(), any(), any());

    try {
      MethodUtils.invokeMethod(yamlGitService, true, "validateGit",
          new Object[] {GitConfig.builder()
                            .accountId("ACCOUNT_ID")
                            .repoUrl(TEST_GIT_REPO_URL)
                            .username(TEST_GIT_REPO_USER)
                            .password(TEST_GIT_REPO_PASSWORD.toCharArray())
                            .build()});
      fail("Was Expected to fail");
    } catch (Exception e) {
      assertTrue((((InvocationTargetException) e).getTargetException()) instanceof WingsException);
    }

    verify(alertService).openAlert(any(), any(), any(), any());
    verify(alertService, times(0)).closeAlert(any(), any(), any(), any());
    MethodUtils.invokeMethod(yamlGitService, true, "validateGit",
        new Object[] {GitConfig.builder()
                          .accountId("ACCOUNT_ID")
                          .repoUrl(TEST_GIT_REPO_URL)
                          .username(TEST_GIT_REPO_USER)
                          .password(TEST_GIT_REPO_PASSWORD.toCharArray())
                          .build()});
    verify(alertService).closeAlert(any(), any(), any(), any());
  }

  @Test
  public void getAllYamlErrorsForAccount() {
    yamlGitService.getAllYamlErrorsForAccount(ACCOUNT_ID);
    verify(mockYamlDirectoryService)
        .traverseDirectory(anyList(), eq(ACCOUNT_ID), any(), anyString(), eq(false), eq(false), any());
  }

  @Test
  public void getAllYamlErrorsForAllAccounts() {
    List<Account> accounts = Arrays.asList(anAccount().withAccountName("Name1").withUuid("AccId1").build(),
        anAccount().withAccountName("Name2").withUuid("AccId2").build());
    doReturn(accounts).when(mockAccountService).list(any());
    yamlGitService.getAllYamlErrorsForAllAccounts();
    verify(yamlGitService, times(2)).getAllYamlErrorsForAccount(anyString());
  }
}
