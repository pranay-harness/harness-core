package software.wings.service.impl.yaml;

import static io.harness.rule.OwnerRule.ARVIND;
import static io.harness.rule.OwnerRule.DEEPAK;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.exception.GitConnectionDelegateException;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.TransportException;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import software.wings.WingsBaseTest;
import software.wings.beans.GitConfig;
import software.wings.beans.GitOperationContext;

import java.util.Arrays;
import java.util.List;

public class GitClientHelperTest extends WingsBaseTest {
  @Inject @InjectMocks GitClientHelper gitClientHelper;

  @Test(expected = GitConnectionDelegateException.class)
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void test_checkIfGitConnectivityIssueInCaseOfTransportException() {
    gitClientHelper.checkIfGitConnectivityIssue(
        new GitAPIException("Git Exception", new TransportException("Transport Exception")) {});
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void test_checkIfGitConnectivityIssueIsNotTrownInCaseOfOtherExceptions() {
    gitClientHelper.checkIfGitConnectivityIssue(new GitAPIException("newTransportException") {});
  }

  @Test
  @Owner(developers = ARVIND)
  @Category(UnitTests.class)
  public void fetchCompleteUrlRepo() {
    String repoUrl1 = "https://abc@a.org/account/name";
    String repoUrl2 = "https://abc@a.org/account/name.git";
    String random = "random";
    GitConfig config = GitConfig.builder().build();

    // UrlType: Repo
    config.setUrlType(GitConfig.UrlType.REPO);
    config.setRepoUrl(repoUrl1);
    config.setRepoName(random);
    assertThat(gitClientHelper.fetchCompleteUrl(config, null)).isEqualTo(repoUrl1);
    assertThat(gitClientHelper.fetchCompleteUrl(config, random)).isEqualTo(repoUrl1);
    config.setRepoUrl(repoUrl2);
    assertThat(gitClientHelper.fetchCompleteUrl(config, null)).isEqualTo(repoUrl2);
    assertThat(gitClientHelper.fetchCompleteUrl(config, random)).isEqualTo(repoUrl2);
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void testGetRepoDirectory() {
    final String repoDirectory =
        gitClientHelper.getRepoDirectory(GitOperationContext.builder()
                                             .gitConnectorId("id")
                                             .gitConfig(GitConfig.builder()
                                                            .accountId("accountId")
                                                            .gitRepoType(GitConfig.GitRepositoryType.HELM)
                                                            .repoUrl("http://github.com/my-repo")
                                                            .build())
                                             .build());
    assertThat(repoDirectory)
        .isEqualTo("./repository/helm/accountId/id/my-repo/9d0502fc8d289f365a3fdcb24607c878b68fad36");
  }

  @Test
  @Owner(developers = ARVIND)
  @Category(UnitTests.class)
  public void fetchCompleteUrlAccount() {
    List<String> urls =
        Arrays.asList("https://abc@a.org/account/", "https://abc@a.org/account///", "https://abc@a.org/account");
    List<String> repoNames = Arrays.asList("/repo", "///repo", "repo");
    List<String> extendedRepoNames = Arrays.asList("/xyz/repo", "///xyz/repo", "xyz/repo");

    String repoUrl1 = "https://abc@a.org/account/repo";
    String repoUrl2 = "https://abc@a.org/account/xyz/repo";
    GitConfig config = GitConfig.builder().build();

    // UrlType: Account
    config.setUrlType(GitConfig.UrlType.ACCOUNT);

    for (String url : urls) {
      config.setRepoUrl(url);
      for (String repoName : repoNames) {
        assertThat(gitClientHelper.fetchCompleteUrl(config, repoName)).isEqualTo(repoUrl1);
      }
      for (String repoName : extendedRepoNames) {
        assertThat(gitClientHelper.fetchCompleteUrl(config, repoName)).isEqualTo(repoUrl2);
      }
    }
  }
}