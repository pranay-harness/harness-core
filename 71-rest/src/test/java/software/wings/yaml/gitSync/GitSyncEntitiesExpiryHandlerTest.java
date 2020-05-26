package software.wings.yaml.gitSync;

import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static io.harness.rule.OwnerRule.ABHINAV;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.inject.Inject;

import io.harness.beans.PageResponse;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.GitCommit;
import software.wings.beans.GitFileActivitySummary;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.yaml.sync.GitSyncErrorService;
import software.wings.service.intfc.yaml.sync.GitSyncService;
import software.wings.yaml.errorhandling.GitSyncError;

import java.util.Arrays;
import java.util.List;

public class GitSyncEntitiesExpiryHandlerTest extends WingsBaseTest {
  @Mock GitSyncService gitSyncService;
  @Mock AppService appService;
  @Mock GitSyncErrorService gitSyncErrorService;
  @InjectMocks @Inject GitSyncEntitiesExpiryHandler gitSyncEntitiesExpiryHandler;

  private final Long ONE_MONTH_IN_MILLS = 2592000000L;
  private final Long TWELVE_MONTH_IN_MILLS = 31104000000L;
  Account account;

  @Before
  public void Setup() {
    MockitoAnnotations.initMocks(this);
    account = getAccount("PAID");
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testGitSyncError() {
    GitSyncError gitSyncError =
        GitSyncError.builder().gitCommitId("commitId").accountId(account.getUuid()).fullSyncPath(false).build();
    wingsPersistence.save(gitSyncError);
    GitSyncError gitSyncError_1 =
        GitSyncError.builder().gitCommitId("commitId1").accountId(account.getUuid()).fullSyncPath(false).build();
    wingsPersistence.save(gitSyncError_1);
    gitSyncError_1.setCreatedAt(gitSyncError_1.getCreatedAt() + 2);
    wingsPersistence.save(gitSyncError_1);

    gitSyncEntitiesExpiryHandler.handleGitError(account, gitSyncError_1.getCreatedAt() - 1);
    ArgumentCaptor<List> argumentCaptor = ArgumentCaptor.forClass(List.class);
    verify(gitSyncErrorService, times(1)).deleteGitSyncErrors(argumentCaptor.capture(), any());
    assert argumentCaptor.getValue().size() == 1;
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testGitCommits() {
    final String connectorId = "gitConnectorId";
    final String branchName = "branchName";
    final String accountId = account.getUuid();
    GitCommit gitCommit = buildGitCommit(connectorId, branchName, GitCommit.Status.COMPLETED, accountId);
    GitCommit gitCommit_1 = buildGitCommit(connectorId, branchName, GitCommit.Status.FAILED, accountId);
    wingsPersistence.save(gitCommit);
    wingsPersistence.save(gitCommit_1);

    // When we have failed commit followed by success one we dont delete any.
    testGitCommitsForExpiredCommits(account, 2, System.currentTimeMillis() + 2 * TWELVE_MONTH_IN_MILLS);

    GitCommit gitCommit_2 = buildGitCommit(connectorId, branchName, GitCommit.Status.COMPLETED_WITH_ERRORS, accountId);
    wingsPersistence.save(gitCommit_2);
    GitCommit gitCommit_3 = buildGitCommit(connectorId, branchName, GitCommit.Status.COMPLETED, accountId);
    wingsPersistence.save(gitCommit_3);

    testGitCommitsForExpiredCommits(account, 1, System.currentTimeMillis() + 2 * TWELVE_MONTH_IN_MILLS);

    wingsPersistence.save(gitCommit);
    wingsPersistence.save(gitCommit_1);
    wingsPersistence.save(gitCommit_2);
    wingsPersistence.save(gitCommit_3);

    testGitCommitsForExpiredCommits(account, 4, gitCommit_2.getCreatedAt() - 1);

    wingsPersistence.save(gitCommit);
    wingsPersistence.save(gitCommit_1);
    wingsPersistence.save(gitCommit_2);
    wingsPersistence.save(gitCommit_3);
    gitCommit_3.setCreatedAt(gitCommit_3.getCreatedAt() + 2);
    wingsPersistence.save(gitCommit_3);
    gitCommit_2.setCreatedAt(gitCommit_2.getCreatedAt() + 1);
    wingsPersistence.save(gitCommit_2);

    testGitCommitsForExpiredCommits(account, 2, gitCommit_2.getCreatedAt() + 1);
  }

  private GitCommit buildGitCommit(String connectorId, String branchName, GitCommit.Status status, String accountId) {
    return GitCommit.builder()
        .gitConnectorId(connectorId)
        .branchName(branchName)
        .status(status)
        .accountId(accountId)
        .build();
  }

  private void testGitCommitsForExpiredCommits(Account account, int expectedNotDeletedCount, long expiryTime) {
    gitSyncEntitiesExpiryHandler.handleGitCommits(account, expiryTime);
    List<GitCommit> postDeletedCommits =
        wingsPersistence.createQuery(GitCommit.class).filter(GitCommit.ACCOUNT_ID_KEY, account.getUuid()).asList();
    assertThat(postDeletedCommits.size()).isEqualTo(expectedNotDeletedCount);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testGitFileActivity() {
    doReturn(Arrays.asList("appId")).when(appService).getAppIdsByAccountId(account.getUuid());
    GitFileActivity gitFileActivity = GitFileActivity.builder().accountId(account.getUuid()).build();
    gitFileActivity.setUuid("uuid");
    PageResponse pageResponse = aPageResponse().withResponse(Arrays.asList(gitFileActivity)).build();
    doReturn(pageResponse).when(gitSyncService).fetchGitSyncActivity(any(), anyString(), anyString(), anyBoolean());
    gitSyncEntitiesExpiryHandler.handleGitFileActivity(account, 0L);
    ArgumentCaptor<List> argumentCaptor = ArgumentCaptor.forClass(List.class);
    verify(gitSyncService, times(1)).deleteGitActivity(argumentCaptor.capture(), any());
    assert argumentCaptor.getValue().size() == 1;
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testGitFileActivitySummary() {
    doReturn(Arrays.asList("appId")).when(appService).getAppIdsByAccountId(account.getUuid());
    GitFileActivitySummary gitFileActivity = GitFileActivitySummary.builder().accountId(account.getUuid()).build();
    gitFileActivity.setUuid("uuid");
    PageResponse pageResponse = aPageResponse().withResponse(Arrays.asList(gitFileActivity)).build();
    doReturn(pageResponse).when(gitSyncService).fetchGitCommits(any(), anyBoolean(), anyString(), anyString());
    gitSyncEntitiesExpiryHandler.handleGitCommitInGitFileActivitySummary(account, 0L);
    ArgumentCaptor<List> argumentCaptor = ArgumentCaptor.forClass(List.class);
    verify(gitSyncService, times(1)).deleteGitCommits(argumentCaptor.capture(), any());
    assert argumentCaptor.getValue().size() == 1;
  }
}