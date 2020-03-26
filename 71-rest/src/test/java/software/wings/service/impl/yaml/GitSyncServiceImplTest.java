package software.wings.service.impl.yaml;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.DEEPAK;
import static io.harness.rule.OwnerRule.VARDAN_BANSAL;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.EntityType;
import software.wings.beans.GitCommit;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.GitFileChange;
import software.wings.dl.WingsPersistence;
import software.wings.exception.YamlProcessingException;
import software.wings.yaml.errorhandling.GitSyncError;
import software.wings.yaml.gitSync.GitFileActivity;
import software.wings.yaml.gitSync.GitFileProcessingSummary;
import software.wings.yaml.gitSync.YamlChangeSet;
import software.wings.yaml.gitSync.YamlGitConfig;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class GitSyncServiceImplTest extends WingsBaseTest {
  @InjectMocks @Inject private GitSyncServiceImpl gitSyncService;
  @Inject private WingsPersistence wingsPersistence;
  private String accountId = generateUuid();
  private String uuid = generateUuid();

  @Test
  @Owner(developers = VARDAN_BANSAL)
  @Category(UnitTests.class)
  public void test_shouldListErrors() {
    final GitSyncError gitSyncError1 = GitSyncError.builder()
                                           .gitCommitId("gitCommitId1")
                                           .yamlFilePath("yamlFilePath1")
                                           .yamlContent("yamlContent1")
                                           .accountId(accountId)
                                           .build();

    final GitSyncError gitSyncError2 = GitSyncError.builder()
                                           .gitCommitId("gitCommitId2")
                                           .yamlFilePath("yamlFilePath2")
                                           .yamlContent("yamlContent2")
                                           .accountId(accountId)
                                           .build();

    wingsPersistence.save(Arrays.asList(gitSyncError1, gitSyncError2));

    final PageRequest pageRequest = aPageRequest().withOffset("0").withLimit("2").build();

    final PageResponse<GitSyncError> errorList = gitSyncService.fetchErrors(pageRequest);
    assertThat(errorList.size()).isEqualTo(2);
  }

  @Test
  @Owner(developers = VARDAN_BANSAL)
  @Category(UnitTests.class)
  public void test_discardGitSyncErrorsForGivenIds() {
    final GitSyncError gitSyncError = GitSyncError.builder()
                                          .gitCommitId("gitCommitId")
                                          .yamlFilePath("yamlFilePath")
                                          .yamlContent("yamlContent")
                                          .accountId(accountId)
                                          .build();

    wingsPersistence.save(gitSyncError);
    gitSyncService.updateGitSyncErrorStatus(Arrays.asList(gitSyncError), GitFileActivity.Status.DISCARDED, accountId);
    assertThat(wingsPersistence.get(GitSyncError.class, gitSyncError.getUuid())).isEqualTo(null);
  }

  @Test
  @Owner(developers = VARDAN_BANSAL)
  @Category(UnitTests.class)
  public void test_fetchRepositories() {
    final YamlGitConfig expectedYamlGitConfig = YamlGitConfig.builder()
                                                    .accountId(accountId)
                                                    .branchName("branchName")
                                                    .enabled(true)
                                                    .entityId(uuid)
                                                    .entityType(EntityType.APPLICATION)
                                                    .build();

    wingsPersistence.save(expectedYamlGitConfig);
    final Application application = Application.Builder.anApplication()
                                        .yamlGitConfig(expectedYamlGitConfig)
                                        .accountId(accountId)
                                        .appId(uuid)
                                        .uuid(uuid)
                                        .build();
    wingsPersistence.save(application);
    final List<Application> applications = gitSyncService.fetchRepositories(accountId);
    assertThat(applications.size()).isEqualTo(1);
    final YamlGitConfig actualYamlGitConfig = applications.get(0).getYamlGitConfig();
    assertThat(actualYamlGitConfig.isEnabled()).isEqualTo(true);
    assertThat(actualYamlGitConfig.getBranchName()).isEqualTo("branchName");
  }

  @Test
  @Owner(developers = VARDAN_BANSAL)
  @Category(UnitTests.class)
  public void test_fetchGitCommits() {
    final GitCommit gitCommit =
        GitCommit.builder()
            .commitId("commitId")
            .yamlGitConfigId("yamlGitConfigId")
            .fileProcessingSummary(
                GitFileProcessingSummary.builder().totalCount(10).successCount(5).failureCount(5).build())
            .accountId(accountId)
            .status(GitCommit.Status.COMPLETED)
            .yamlChangeSet(YamlChangeSet.builder()
                               .gitFileChanges(Arrays.asList(GitFileChange.Builder.aGitFileChange()
                                                                 .withAccountId(accountId)
                                                                 .withChangeType(ChangeType.ADD)
                                                                 .build()))
                               .build())
            .build();

    wingsPersistence.save(gitCommit);
    final PageResponse pageResponse = gitSyncService.fetchGitCommits(aPageRequest().withLimit("1").build(), accountId);
    assertThat(pageResponse).isNotNull();
    final List<GitCommit> responseList = pageResponse.getResponse();
    assertThat(responseList.size()).isEqualTo(1);
    assertThat(responseList.get(0).getAccountId()).isEqualTo(accountId);
  }

  @Test
  @Owner(developers = VARDAN_BANSAL)
  @Category(UnitTests.class)
  public void test_logFileActivityAndGenerateProcessingSummary() {
    final List<Change> changes = Arrays.asList(Change.Builder.aFileChange()
                                                   .withAccountId(accountId)
                                                   .withCommitId("commitId")
                                                   .withChangeType(ChangeType.ADD)
                                                   .withFileContent("someContent")
                                                   .build());
    final HashMap<String, YamlProcessingException.ChangeWithErrorMsg> failedYamlFileChangeMap = new HashMap<>();
    GitFileProcessingSummary fileProcessingSummary = gitSyncService.logFileActivityAndGenerateProcessingSummary(
        changes, failedYamlFileChangeMap, GitFileActivity.Status.SKIPPED, "successfully proccessed");
    assertThat(fileProcessingSummary).isNotNull();
    assertThat(fileProcessingSummary.getFailureCount()).isEqualTo(1);
    failedYamlFileChangeMap.put("errorMssg",
        YamlProcessingException.ChangeWithErrorMsg.builder()
            .change(Change.Builder.aFileChange().withAccountId(accountId).withChangeType(ChangeType.ADD).build())
            .build());
    fileProcessingSummary = gitSyncService.logFileActivityAndGenerateProcessingSummary(
        changes, failedYamlFileChangeMap, GitFileActivity.Status.SUCCESS, "successfully proccessed");
    assertThat(fileProcessingSummary).isNotNull();
    assertThat(fileProcessingSummary.getSuccessCount()).isEqualTo(1);
  }

  @Test
  @Owner(developers = VARDAN_BANSAL)
  @Category(UnitTests.class)
  public void test_fetchGitSyncActivity() {
    final GitFileActivity fileActivity = GitFileActivity.builder()
                                             .commitId("commitId")
                                             .accountId(accountId)
                                             .fileContent("some file content")
                                             .triggeredBy(GitFileActivity.TriggeredBy.USER)
                                             .status(GitFileActivity.Status.SUCCESS)
                                             .build();

    wingsPersistence.save(fileActivity);
    final PageResponse pageResponse = gitSyncService.fetchGitSyncActivity(aPageRequest().withLimit("1").build());
    assertThat(pageResponse).isNotNull();
    final List<GitFileActivity> responseList = pageResponse.getResponse();
    assertThat(responseList.size()).isEqualTo(1);
    assertThat(responseList.get(0).getAccountId()).isEqualTo(accountId);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void test_fetchGitToHarnessErrors() {
    final String commitId = "gitCommitId";
    // Saving GitSyncError
    final GitSyncError gitSyncError =
        GitSyncError.builder().accountId(accountId).fullSyncPath(false).gitCommitId(commitId).build();
    gitSyncError.setCreatedAt(System.currentTimeMillis());
    String id = wingsPersistence.save(gitSyncError);

    PageRequest<GitToHarnessErrorCommitStats> req = aPageRequest().withLimit("2").withOffset("0").build();
    List<GitToHarnessErrorCommitStats> errorsList =
        gitSyncService.fetchGitToHarnessErrors(req, accountId, null).getResponse();
    assertThat(errorsList.size()).isEqualTo(1);
    GitToHarnessErrorCommitStats error = errorsList.get(0);
    assertThat(error.getFailedCount()).isEqualTo(1);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void test_fetchGitToHarnessErrorsCommitWise() {
    final String commitId = "gitCommitId";
    // Saving GitSyncError
    final GitSyncError gitSyncError =
        GitSyncError.builder().accountId(accountId).fullSyncPath(false).gitCommitId(commitId).build();
    gitSyncError.setCreatedAt(System.currentTimeMillis());
    String id = wingsPersistence.save(gitSyncError);

    PageRequest<GitSyncError> req = aPageRequest().withLimit("2").withOffset("0").build();
    List<GitSyncError> errorsList = gitSyncService.fetchErrorsInEachCommits(req, commitId, accountId).getResponse();
    assertThat(errorsList.size()).isEqualTo(1);
    GitSyncError error = errorsList.get(0);
    assertThat(error.equals(gitSyncError)).isTrue();
  }
}
