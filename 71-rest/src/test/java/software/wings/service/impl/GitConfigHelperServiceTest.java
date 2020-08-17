package software.wings.service.impl;

import static io.harness.rule.OwnerRule.ARVIND;
import static io.harness.rule.OwnerRule.YOGESH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.yaml.GitCommand.GitCommandType.VALIDATE;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.FeatureName;
import software.wings.beans.GitConfig;
import software.wings.beans.yaml.GitCommandExecutionResponse;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.sm.ExecutionContext;

import java.util.ArrayList;
import java.util.List;

public class GitConfigHelperServiceTest extends WingsBaseTest {
  @Mock ExecutionContext context;
  @Mock DelegateService delegateService;
  @Mock FeatureFlagService featureFlagService;

  @Inject @InjectMocks private GitConfigHelperService gitConfigHelperService;

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testRenderGitConfig() {
    String branchExpression = "${branch}";
    String urlExpression = "${url}";
    String refExpression = "${ref}";
    GitConfig gitConfig = GitConfig.builder().branch(branchExpression).repoUrl(urlExpression).build();
    gitConfig.setReference(refExpression);

    when(context.renderExpression(branchExpression)).thenReturn("master");
    when(context.renderExpression(urlExpression)).thenReturn("github.com");
    when(context.renderExpression(refExpression)).thenReturn("tag-1");

    gitConfigHelperService.renderGitConfig(context, gitConfig);

    assertThat(gitConfig.getBranch()).isEqualTo("master");
    assertThat(gitConfig.getRepoUrl()).isEqualTo("github.com");
    assertThat(gitConfig.getReference()).isEqualTo("tag-1");
  }

  @Test
  @Owner(developers = ARVIND)
  @Category(UnitTests.class)
  public void validateGitConfigTestFeatureOnUrlTypeAccount() throws Exception {
    doReturn(true).when(featureFlagService).isEnabled(FeatureName.GIT_ACCOUNT_SUPPORT, ACCOUNT_ID);
    validateGitConfigUrlTypeAccount();
  }

  @Test
  @Owner(developers = ARVIND)
  @Category(UnitTests.class)
  public void validateGitConfigTestFeatureOffUrlTypeAccount() throws Exception {
    doReturn(false).when(featureFlagService).isEnabled(FeatureName.GIT_ACCOUNT_SUPPORT, ACCOUNT_ID);
    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(this ::validateGitConfigUrlTypeAccount)
        .withMessageContaining("Account level git connector is not enabled");
  }

  @Test
  @Owner(developers = ARVIND)
  @Category(UnitTests.class)
  public void validateGitConfigTestFeatureOnUrlTypeRepo() throws Exception {
    doReturn(true).when(featureFlagService).isEnabled(FeatureName.GIT_ACCOUNT_SUPPORT, ACCOUNT_ID);
    validateGitConfigUrlTypeRepo();
  }

  @Test
  @Owner(developers = ARVIND)
  @Category(UnitTests.class)
  public void validateGitConfigTestFeatureOffUrlTypeRepo() throws Exception {
    doReturn(false).when(featureFlagService).isEnabled(FeatureName.GIT_ACCOUNT_SUPPORT, ACCOUNT_ID);
    validateGitConfigUrlTypeRepo();
  }

  private void validateGitConfigUrlTypeRepo() throws InterruptedException {
    GitConfig gitConfig = GitConfig.builder()
                              .accountId(ACCOUNT_ID)
                              .keyAuth(false)
                              .username("")
                              .password("".toCharArray())
                              .urlType(GitConfig.UrlType.REPO)
                              .accountId("id")
                              .build();
    List<EncryptedDataDetail> encryptionDetails = new ArrayList<>();
    doReturn(GitCommandExecutionResponse.builder().build()).when(delegateService).executeTask(any());
    ArgumentCaptor<DelegateTask> captor = ArgumentCaptor.forClass(DelegateTask.class);

    gitConfigHelperService.validateGitConfig(gitConfig, encryptionDetails);
    verify(delegateService, times(1)).executeTask(captor.capture());
    DelegateTask task = captor.getAllValues().get(0);
    Object[] parameters = task.getData().getParameters();
    assertThat(parameters.length).isEqualTo(3);
    assertThat(parameters[0]).isEqualTo(VALIDATE);
    assertThat(parameters[1]).isEqualTo(gitConfig);
    assertThat(parameters[2]).isEqualTo(encryptionDetails);
  }

  private void validateGitConfigUrlTypeAccount() throws InterruptedException {
    GitConfig gitConfig = GitConfig.builder()
                              .accountId(ACCOUNT_ID)
                              .repoUrl("http://x.y/z")
                              .keyAuth(false)
                              .username("")
                              .password("".toCharArray())
                              .urlType(GitConfig.UrlType.ACCOUNT)
                              .build();

    doReturn(GitCommandExecutionResponse.builder().build()).when(delegateService).executeTask(any());

    List<EncryptedDataDetail> encryptionDetails = new ArrayList<>();
    gitConfigHelperService.validateGitConfig(gitConfig, encryptionDetails);
    verify(delegateService, times(0)).executeTask(any());

    gitConfig.setRepoName("");
    gitConfigHelperService.validateGitConfig(gitConfig, encryptionDetails);
    verify(delegateService, times(0)).executeTask(any());

    gitConfig.setRepoName("repo.git");
    ArgumentCaptor<DelegateTask> captor = ArgumentCaptor.forClass(DelegateTask.class);
    gitConfigHelperService.validateGitConfig(gitConfig, encryptionDetails);
    verify(delegateService, times(1)).executeTask(captor.capture());

    DelegateTask task = captor.getAllValues().get(0);
    Object[] parameters = task.getData().getParameters();
    assertThat(parameters.length).isEqualTo(3);
    assertThat(parameters[0]).isEqualTo(VALIDATE);
    assertThat(parameters[1]).isEqualTo(gitConfig);
    assertThat(parameters[2]).isEqualTo(encryptionDetails);
  }
}